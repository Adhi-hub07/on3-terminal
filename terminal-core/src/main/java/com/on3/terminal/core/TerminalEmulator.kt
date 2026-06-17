package com.on3.terminal.core

class TerminalEmulator(
    var mColumns: Int,
    var mRows: Int,
    private var mCellWidthPixels: Int = 0,
    private var mCellHeightPixels: Int = 0,
    transcriptRows: Int = DEFAULT_TERMINAL_TRANSCRIPT_ROWS
) {
    companion object {
        const val UNICODE_REPLACEMENT_CHAR = 0xFFFD
        const val DEFAULT_TERMINAL_TRANSCRIPT_ROWS = 2000
        const val TERMINAL_TRANSCRIPT_ROWS_MIN = 100
        const val TERMINAL_TRANSCRIPT_ROWS_MAX = 50000

        const val MOUSE_LEFT_BUTTON = 0
        const val MOUSE_LEFT_BUTTON_MOVED = 32
        const val MOUSE_WHEELUP_BUTTON = 64
        const val MOUSE_WHEELDOWN_BUTTON = 65

        const val TERMINAL_CURSOR_STYLE_BLOCK = 0
        const val TERMINAL_CURSOR_STYLE_UNDERLINE = 1
        const val TERMINAL_CURSOR_STYLE_BAR = 2
    }

    private val mTranscriptRows = transcriptRows.coerceIn(TERMINAL_TRANSCRIPT_ROWS_MIN, TERMINAL_TRANSCRIPT_ROWS_MAX)
    val mMainBuffer = TerminalBuffer(mColumns, mTranscriptRows, mRows)
    val mAltBuffer = TerminalBuffer(mColumns, mRows, mRows)
    private var mScreen: TerminalBuffer = mMainBuffer
    val mColors = TerminalColors()

    private var mCursorRow = 0
    private var mCursorCol = 0
    private var mCursorStyle = TERMINAL_CURSOR_STYLE_BLOCK
    private var mCursorBlinkingEnabled = false
    private var mCursorBlinkState = true

    private var mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
    private var mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
    private var mEffect = 0

    private var mTopMargin = 0
    private var mBottomMargin = mRows
    private var mLeftMargin = 0
    private var mRightMargin = mColumns
    private var mTabStop = BooleanArray(mColumns)
    private var mInsertMode = false
    private var mAboutToAutoWrap = false
    private var mEscapeState = ESC_NONE
    private var mArgIndex = 0
    private val mArgs = IntArray(32)
    private val mOSCArgs = StringBuilder()
    private var mContinueSequence = false
    private var mScrollCounter = 0
    private var mUseLineDrawingG0 = false
    private var mUseLineDrawingG1 = false
    private var mUseLineDrawingUsesG0 = true
    private var mAutoScrollDisabled = false

    private var mUtf8ToFollow = 0
    private var mUtf8Index = 0
    private val mUtf8InputBuffer = ByteArray(4)

    var mTitle: String? = null
    private val mTitleStack = mutableListOf<String>()

    var onTitleChanged: ((String?) -> Unit)? = null
    var onBell: (() -> Unit)? = null
    var onCopyTextToClipboard: ((String) -> Unit)? = null
    var onPasteTextFromClipboard: (() -> Unit)? = null
    var onSessionFinished: (() -> Unit)? = null
    var onColorsChanged: (() -> Unit)? = null
    var onTextChanged: (() -> Unit)? = null

    private var mCurrentDecSetFlags = 0
    private val DECSET_BIT_APPLICATION_CURSOR_KEYS = 1
    private val DECSET_BIT_REVERSE_VIDEO = 1 shl 1
    private val DECSET_BIT_ORIGIN_MODE = 1 shl 2
    private val DECSET_BIT_AUTOWRAP = 1 shl 3
    private val DECSET_BIT_CURSOR_ENABLED = 1 shl 4
    private val DECSET_BIT_APPLICATION_KEYPAD = 1 shl 5
    private val DECSET_BIT_MOUSE_PRESS_RELEASE = 1 shl 6
    private val DECSET_BIT_MOUSE_BUTTON_EVENT = 1 shl 7
    private val DECSET_BIT_MOUSE_PROTOCOL_SGR = 1 shl 9
    private val DECSET_BIT_BRACKETED_PASTE = 1 shl 10
    private val DECSET_BIT_LEFTRIGHT_MARGIN = 1 shl 11

    private val ESC_NONE = 0
    private val ESC = 1
    private val ESC_POUND = 2
    private val ESC_LEFT_PAREN = 3
    private val ESC_RIGHT_PAREN = 4
    private val ESC_CSI = 6
    private val ESC_CSI_QUESTION = 7
    private val ESC_PERCENT = 9
    private val ESC_OSC = 10
    private val ESC_OSC_ESC = 11
    private val ESC_CSI_BIGGER = 12
    private val ESC_CSI_DOLLAR = 14
    private val ESC_CSI_SPACE = 15
    private val ESC_CSI_ASTERIX = 16
    private val ESC_CSI_DQUOTE = 17

    init {
        reset()
    }

    val mStyle: Long get() = TextStyle.encode(mForeColor, mBackColor, mEffect)

    fun reset() {
        mCursorRow = 0
        mCursorCol = 0
        mCursorStyle = TERMINAL_CURSOR_STYLE_BLOCK
        mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
        mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
        mEffect = 0
        mCurrentDecSetFlags = (1 shl 4) or DECSET_BIT_AUTOWRAP
        mInsertMode = false
        mAboutToAutoWrap = false
        mEscapeState = ESC_NONE
        mScrollCounter = 0
        mTopMargin = 0
        mBottomMargin = mRows
        mLeftMargin = 0
        mRightMargin = mColumns
        mTabStop = BooleanArray(mColumns)
        setDefaultTabStops()
        mScreen = mMainBuffer
        mColors.reset()
        onColorsChanged?.invoke()
    }

    private fun setDefaultTabStops() {
        for (i in 0 until mColumns) {
            mTabStop[i] = (i and 7) == 0 && i != 0
        }
    }

    fun getScreen(): TerminalBuffer = mScreen

    fun isAlternateBufferActive(): Boolean = mScreen === mAltBuffer

    fun isReverseVideo(): Boolean = (mCurrentDecSetFlags and DECSET_BIT_REVERSE_VIDEO) != 0

    fun isCursorEnabled(): Boolean = (mCurrentDecSetFlags and DECSET_BIT_CURSOR_ENABLED) != 0

    fun isKeypadApplicationMode(): Boolean = (mCurrentDecSetFlags and DECSET_BIT_APPLICATION_KEYPAD) != 0

    fun isCursorKeysApplicationMode(): Boolean = (mCurrentDecSetFlags and DECSET_BIT_APPLICATION_CURSOR_KEYS) != 0

    fun isMouseTrackingActive(): Boolean =
        (mCurrentDecSetFlags and (DECSET_BIT_MOUSE_PRESS_RELEASE or DECSET_BIT_MOUSE_BUTTON_EVENT)) != 0

    fun isAutoScrollDisabled(): Boolean = mAutoScrollDisabled

    fun getCursorRow(): Int = mCursorRow
    fun getCursorCol(): Int = mCursorCol
    fun getCursorStyle(): Int = mCursorStyle
    fun shouldCursorBeVisible(): Boolean {
        if (!isCursorEnabled()) return false
        return if (mCursorBlinkingEnabled) mCursorBlinkState else true
    }

    fun setCursorBlinkState(state: Boolean) { mCursorBlinkState = state }
    fun clearScrollCounter() { mScrollCounter = 0 }
    fun getScrollCounter(): Int = mScrollCounter

    fun sendMouseEvent(mouseButton: Int, column: Int, row: Int, pressed: Boolean) {
        // Mouse events are sent through the session write mechanism
    }

    fun resize(columns: Int, rows: Int, cellWidth: Int, cellHeight: Int) {
        mCellWidthPixels = cellWidth
        mCellHeightPixels = cellHeight
        if (mRows == rows && mColumns == columns) return
        if (columns < 2 || rows < 2) return

        if (mRows != rows) {
            mRows = rows
            mTopMargin = 0
            mBottomMargin = mRows
        }
        if (mColumns != columns) {
            val oldColumns = mColumns
            mColumns = columns
            val oldTabStop = mTabStop
            mTabStop = BooleanArray(mColumns)
            setDefaultTabStops()
            val toTransfer = minOf(oldColumns, columns)
            oldTabStop.copyInto(mTabStop, 0, 0, toTransfer)
            mLeftMargin = 0
            mRightMargin = mColumns
        }
    }

    fun append(buffer: ByteArray, length: Int) {
        for (i in 0 until length) processByte(buffer[i].toInt() and 0xFF)
    }

    fun write(data: ByteArray, offset: Int, count: Int) {
        // Override in subclass to write to process
    }

    fun write(data: String) {
        write(data.toByteArray(Charsets.UTF_8), 0, data.length)
    }

    fun writeCodePoint(prependEscape: Boolean, codePoint: Int) {
        // Override in subclass
    }

    fun paste(text: String) {
        val bracketed = (mCurrentDecSetFlags and DECSET_BIT_BRACKETED_PASTE) != 0
        if (bracketed) write("\u001B[200~")
        write(text)
        if (bracketed) write("\u001B[201~")
    }

    private fun processByte(b: Int) {
        if (mUtf8ToFollow > 0) {
            handleUtf8Continuation(b)
            return
        }
        when {
            b and 0x80 == 0 -> processCodePoint(b)
            b and 0xE0 == 0xC0 -> { mUtf8ToFollow = 1; mUtf8InputBuffer[0] = b.toByte(); mUtf8Index = 1 }
            b and 0xF0 == 0xE0 -> { mUtf8ToFollow = 2; mUtf8InputBuffer[0] = b.toByte(); mUtf8Index = 1 }
            b and 0xF8 == 0xF0 -> { mUtf8ToFollow = 3; mUtf8InputBuffer[0] = b.toByte(); mUtf8Index = 1 }
            else -> processCodePoint(UNICODE_REPLACEMENT_CHAR)
        }
    }

    private fun handleUtf8Continuation(b: Int) {
        if (b and 0xC0 == 0x80) {
            mUtf8InputBuffer[mUtf8Index++] = b.toByte()
            if (--mUtf8ToFollow == 0) {
                val firstByteMask = when (mUtf8Index) {
                    2 -> 0x1F; 3 -> 0x0F; else -> 0x07
                }
                var codePoint = (mUtf8InputBuffer[0].toInt() and firstByteMask)
                for (i in 1 until mUtf8Index)
                    codePoint = (codePoint shl 6) or (mUtf8InputBuffer[i].toInt() and 0x3F)
                mUtf8Index = 0; mUtf8ToFollow = 0
                if (codePoint in 0x80..0x9F) return
                processCodePoint(codePoint)
            }
        } else {
            mUtf8Index = 0; mUtf8ToFollow = 0
            processCodePoint(UNICODE_REPLACEMENT_CHAR)
            processByte(b)
        }
    }

    fun processCodePoint(codePoint: Int) {
        if (mEscapeState == ESC_NONE) {
            when (codePoint) {
                0 -> return
                7 -> { onBell?.invoke() }
                8 -> { if (mCursorCol > mLeftMargin) mCursorCol--; mAboutToAutoWrap = false }
                9 -> { mCursorCol = nextTabStop(1) }
                10, 11, 12 -> doLinefeed()
                13 -> { mCursorCol = mLeftMargin; mAboutToAutoWrap = false }
                14 -> mUseLineDrawingUsesG0 = false
                15 -> mUseLineDrawingUsesG0 = true
                24, 26 -> { mEscapeState = ESC_NONE; emitCodePoint(127) }
                27 -> startEscapeSequence()
                else -> { if (codePoint >= 32) emitCodePoint(codePoint) }
            }
        } else {
            handleEscapeState(codePoint)
        }
    }

    private fun startEscapeSequence() {
        mEscapeState = ESC
        mContinueSequence = true
        mArgIndex = -1
    }

    private fun handleEscapeState(b: Int) {
        mContinueSequence = false
        when (mEscapeState) {
            ESC -> doEsc(b)
            ESC_POUND -> { if (b == '8') mScreen.getLine(mCursorRow).clear(mStyle); mEscapeState = ESC_NONE }
            ESC_LEFT_PAREN -> { mUseLineDrawingG0 = (b == '0'); mEscapeState = ESC_NONE }
            ESC_RIGHT_PAREN -> { mUseLineDrawingG1 = (b == '0'); mEscapeState = ESC_NONE }
            ESC_CSI -> doCSI(b)
            ESC_CSI_QUESTION -> doCSIQuestion(b)
            ESC_CSI_BIGGER -> doCSIBigger(b)
            ESC_OSC -> doOSC(b)
            ESC_OSC_ESC -> { if (b == '\\') doOSC(-1) else { doOSC(b); mEscapeState = ESC_OSC } }
            ESC_PERCENT -> { mEscapeState = ESC_NONE }
            ESC_CSI_SPACE -> {
                val arg = getArg(0, 0)
                when (b) {
                    'q' -> mCursorStyle = when {
                        arg in 0..2 -> TERMINAL_CURSOR_STYLE_BLOCK
                        arg in 3..4 -> TERMINAL_CURSOR_STYLE_UNDERLINE
                        arg in 5..6 -> TERMINAL_CURSOR_STYLE_BAR
                        else -> TERMINAL_CURSOR_STYLE_BLOCK
                    }
                    else -> {}
                }
                mEscapeState = ESC_NONE
            }
            ESC_CSI_ASTERIX -> mEscapeState = ESC_NONE
            ESC_CSI_DQUOTE -> {
                if (b == 'q') {
                    val arg = getArg(0, 0)
                    if (arg == 0 || arg == 2) mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_PROTECTED.inv()
                    else if (arg == 1) mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_PROTECTED
                }
                mEscapeState = ESC_NONE
            }
            ESC_CSI_DOLLAR -> mEscapeState = ESC_NONE
            else -> mEscapeState = ESC_NONE
        }
    }

    private fun doEsc(b: Int) {
        when (b) {
            '7' -> { /* save cursor */ }
            '8' -> { /* restore cursor */ }
            'D' -> doLinefeed()
            'E' -> { doLinefeed(); mCursorCol = 0 }
            'H' -> { mCursorCol = 0; mCursorRow = 0; mAboutToAutoWrap = false }
            'M' -> reverseIndex()
            'Z' -> write("\u001B[?1;2c")
            'c' -> reset()
            '>' -> { /* normal keypad */ mCurrentDecSetFlags = mCurrentDecSetFlags and DECSET_BIT_APPLICATION_KEYPAD.inv() }
            '=' -> { mCurrentDecSetFlags = mCurrentDecSetFlags or DECSET_BIT_APPLICATION_KEYPAD }
            '#' -> mContinueSequence = true; mEscapeState = ESC_POUND
            '(' -> mContinueSequence = true; mEscapeState = ESC_LEFT_PAREN
            ')' -> mContinueSequence = true; mEscapeState = ESC_RIGHT_PAREN
            '%' -> mEscapeState = ESC_PERCENT
            '[' -> mContinueSequence = true; mArgIndex = -1; mEscapeState = ESC_CSI
            ']' -> mContinueSequence = true; mOSCArgs.setLength(0); mEscapeState = ESC_OSC
            '_' -> mEscapeState = ESC_NONE
            '^' -> mEscapeState = ESC_NONE
            'P' -> mEscapeState = ESC_NONE
            else -> { /* unknown */ }
        }
        if (mEscapeState != ESC) mContinueSequence = false
    }

    private fun setArg(b: Int) {
        if (b in '0'..'9') {
            val index = if (mArgIndex < 0) { mArgIndex = 0; 0 } else mArgIndex
            if (index < mArgs.size) {
                mArgs[index] = mArgs[index] * 10 + (b - '0')
            }
        } else if (b == ';') {
            if (mArgIndex < 0) mArgIndex = 0
            mArgIndex++
            if (mArgIndex < mArgs.size) mArgs[mArgIndex] = 0
        } else {
            // Parameter byte but not digit/semicolon - might be ':' or other
            if (b == ':') { /* sub-parameter, skip for now */ }
            mContinueSequence = true
        }
    }

    private fun doCSI(b: Int) {
        when {
            b in 0x30..0x3F -> setArg(b)
            b in 0x20..0x2F -> {
                if (b == ' ') { mEscapeState = ESC_CSI_SPACE; mContinueSequence = true }
                else if (b == '*') { mEscapeState = ESC_CSI_ASTERIX; mContinueSequence = true }
                else if (b == '"') { mEscapeState = ESC_CSI_DQUOTE; mContinueSequence = true }
                else if (b == '$') { mEscapeState = ESC_CSI_DOLLAR; mContinueSequence = true }
                else mEscapeState = ESC_NONE
            }
            else -> handleCSICommand(b)
        }
    }

    private fun doCSIQuestion(b: Int) {
        when {
            b in 0x30..0x3F -> setArg(b)
            b == '$' -> { mEscapeState = ESC_CSI_DOLLAR; mContinueSequence = true }
            else -> handleCSIQuestionCommand(b)
        }
    }

    private fun doCSIBigger(b: Int) {
        when {
            b in 0x30..0x3F -> setArg(b)
            else -> mEscapeState = ESC_NONE
        }
    }

    private fun getArg(index: Int, default: Int): Int {
        return if (index < mArgIndex + 1 && index < mArgs.size) mArgs[index] else default
    }

    private fun handleCSICommand(b: Int) {
        when (b) {
            '@' -> insertChars(getArg(0, 1))
            'A' -> cursorUp(getArg(0, 1))
            'B' -> cursorDown(getArg(0, 1))
            'C' -> cursorRight(getArg(0, 1))
            'D' -> cursorLeft(getArg(0, 1))
            'E' -> cursorDown(getArg(0, 1)); mCursorCol = mLeftMargin
            'F' -> cursorUp(getArg(0, 1)); mCursorCol = mLeftMargin
            'G' -> mCursorCol = (getArg(0, 1) - 1).coerceIn(mLeftMargin, mRightMargin - 1)
            'H', 'f' -> {
                val row = (getArg(0, 1) - 1).coerceIn(0, mRows - 1)
                val col = (getArg(1, 1) - 1).coerceIn(mLeftMargin, mRightMargin - 1)
                mCursorRow = row; mCursorCol = col; mAboutToAutoWrap = false
            }
            'J' -> eraseDisplay(getArg(0, 0))
            'K' -> eraseLine(getArg(0, 0))
            'L' -> insertLines(getArg(0, 1))
            'M' -> deleteLines(getArg(0, 1))
            'P' -> deleteChars(getArg(0, 1))
            'S' -> scrollUp(getArg(0, 1))
            'T' -> scrollDown(getArg(0, 1))
            'X' -> eraseChars(getArg(0, 1))
            'Z' -> mCursorCol = prevTabStop(getArg(0, 1))
            '`' -> mCursorCol = (getArg(0, 1) - 1).coerceIn(mLeftMargin, mRightMargin - 1)
            'a' -> cursorRight(getArg(0, 1))
            'b' -> repeatChar(getArg(0, 1))
            'c' -> write("\u001B[?1;2c")
            'd' -> mCursorRow = (getArg(0, 1) - 1).coerceIn(0, mRows - 1)
            'e' -> cursorDown(getArg(0, 1))
            'g' -> {
                val arg = getArg(0, 0)
                if (arg == 3) { mTabStop.fill(false) }
                else if (arg == 0 && mCursorCol < mColumns) mTabStop[mCursorCol] = false
            }
            'h' -> setDecMode(getArg(0, 0), true)
            'l' -> setDecMode(getArg(0, 0), false)
            'm' -> setCharacterAttributes()
            'n' -> {
                when (getArg(0, 0)) {
                    5 -> write("\u001B[0n")
                    6 -> write("\u001B[${mCursorRow + 1};${mCursorCol + 1}R")
                }
            }
            'q' -> { /* LED - ignore */ }
            'r' -> setScrollingRegion()
            's' -> { /* save cursor */ }
            'u' -> { /* restore cursor */ }
            'x' -> { /* DECREQTPARM - ignore */ }
            else -> { /* unknown */ }
        }
    }

    private fun handleCSIQuestionCommand(b: Int) {
        when (b) {
            'h' -> setDecPrivateMode(getArg(0, 0), true)
            'l' -> setDecPrivateMode(getArg(0, 0), false)
            'c' -> write("\u001B[?1;2c")
            'n' -> { if (getArg(0, 0) == 15) write("\u001B[?13n") }
            's' -> { /* save DEC mode */ }
            'r' -> { /* restore DEC mode */ }
            't' -> { /* xterm window ops */ }
            else -> { /* unknown */ }
        }
    }

    private fun setDecMode(mode: Int, set: Boolean) {
        when (mode) {
            2 -> { if (set) mCurrentDecSetFlags = mCurrentDecSetFlags or DECSET_BIT_REVERSE_VIDEO
                else mCurrentDecSetFlags = mCurrentDecSetFlags and DECSET_BIT_REVERSE_VIDEO.inv() }
            4 -> { if (set) mInsertMode = true else mInsertMode = false }
            12 -> mCursorBlinkingEnabled = set
            20 -> { /* line feed mode */ }
        }
    }

    private fun setDecPrivateMode(mode: Int, set: Boolean) {
        val bit = when (mode) {
            1 -> DECSET_BIT_APPLICATION_CURSOR_KEYS
            5 -> DECSET_BIT_REVERSE_VIDEO
            6 -> DECSET_BIT_ORIGIN_MODE
            7 -> DECSET_BIT_AUTOWRAP
            25 -> DECSET_BIT_CURSOR_ENABLED
            66 -> DECSET_BIT_APPLICATION_KEYPAD
            69 -> DECSET_BIT_LEFTRIGHT_MARGIN
            1000 -> DECSET_BIT_MOUSE_PRESS_RELEASE
            1002 -> DECSET_BIT_MOUSE_BUTTON_EVENT
            1006 -> DECSET_BIT_MOUSE_PROTOCOL_SGR
            2004 -> DECSET_BIT_BRACKETED_PASTE
            47, 1047, 1049 -> -2 // Alternate screen
            else -> -1
        }
        when {
            bit == -2 -> toggleAlternateScreen(set, mode)
            bit >= 0 -> {
                if (set) mCurrentDecSetFlags = mCurrentDecSetFlags or bit
                else {
                    mCurrentDecSetFlags = mCurrentDecSetFlags and bit.inv()
                    if (mode == 1000) mCurrentDecSetFlags = mCurrentDecSetFlags and (DECSET_BIT_MOUSE_BUTTON_EVENT or DECSET_BIT_MOUSE_PROTOCOL_SGR).inv()
                }
            }
        }
    }

    private fun toggleAlternateScreen(enterAlt: Boolean, mode: Int) {
        if (enterAlt) {
            if (mScreen !== mAltBuffer) {
                mScreen = mAltBuffer
            }
        } else {
            if (mScreen !== mMainBuffer) {
                mScreen = mMainBuffer
            }
        }
    }

    private fun setScrollingRegion() {
        val top = (getArg(0, 1) - 1).coerceIn(0, mRows - 1)
        val bottom = (getArg(1, mRows) - 1).coerceIn(top + 1, mRows - 1)
        mTopMargin = top; mBottomMargin = bottom + 1
        mCursorRow = 0; mCursorCol = 0
    }

    private fun setCharacterAttributes() {
        val count = minOf(mArgIndex + 1, mArgs.size)
        for (i in 0 until count) {
            val attr = mArgs[i]
            when (attr) {
                0 -> { mForeColor = TextStyle.COLOR_INDEX_FOREGROUND; mBackColor = TextStyle.COLOR_INDEX_BACKGROUND; mEffect = 0 }
                1 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_BOLD
                2 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_DIM
                3 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_ITALIC
                4 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE
                5, 6 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_BLINK
                7 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_INVERSE
                8 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE
                9 -> mEffect = mEffect or TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH
                10, 11, 12 -> { /* font selection */ }
                21, 22 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_BOLD.inv()
                23 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_ITALIC.inv()
                24 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE.inv()
                25 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_BLINK.inv()
                27 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE.inv()
                28 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE.inv()
                29 -> mEffect = mEffect and TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH.inv()
                in 30..37 -> mForeColor = attr - 30
                38 -> { if (i + 1 < count) { val next = mArgs[i + 1]; if (next == 5 && i + 2 < count) { mForeColor = mArgs[i + 2]; i += 2 } else if (next == 2 && i + 4 < count) { mForeColor = 0xFF000000 or (mArgs[i + 2] shl 16) or (mArgs[i + 3] shl 8) or mArgs[i + 4]; i += 4 } } }
                39 -> mForeColor = TextStyle.COLOR_INDEX_FOREGROUND
                in 40..47 -> mBackColor = attr - 40
                48 -> { if (i + 1 < count) { val next = mArgs[i + 1]; if (next == 5 && i + 2 < count) { mBackColor = mArgs[i + 2]; i += 2 } else if (next == 2 && i + 4 < count) { mBackColor = 0xFF000000 or (mArgs[i + 2] shl 16) or (mArgs[i + 3] shl 8) or mArgs[i + 4]; i += 4 } } }
                49 -> mBackColor = TextStyle.COLOR_INDEX_BACKGROUND
                in 90..97 -> mForeColor = attr - 90 + 8
                in 100..107 -> mBackColor = attr - 100 + 8
            }
        }
    }

    private fun doOSC(b: Int) {
        if (b == 7 || b == -1) { // BEL or ST
            processOSCString()
            mEscapeState = ESC_NONE
        } else if (b == 27) {
            mEscapeState = ESC_OSC_ESC
        } else if (b >= 32) {
            if (mOSCArgs.length < 8192) mOSCArgs.append(b.toChar())
        }
    }

    private fun processOSCString() {
        val s = mOSCArgs.toString()
        val semicolonIndex = s.indexOf(';')
        val command = if (semicolonIndex >= 0) {
            try { s.substring(0, semicolonIndex).toInt() } catch (e: NumberFormatException) { -1 }
        } else {
            try { s.toInt() } catch (e: NumberFormatException) { -1 }
        }
        val data = if (semicolonIndex >= 0) s.substring(semicolonIndex + 1) else ""

        when (command) {
            0, 1, 2 -> { mTitle = if (command == 2 || command == 0) data else mTitle; onTitleChanged?.invoke(mTitle) }
            8 -> { /* hyperlink - ignore */ }
            10 -> parseColor(data, TextStyle.COLOR_INDEX_FOREGROUND)
            11 -> parseColor(data, TextStyle.COLOR_INDEX_BACKGROUND)
            12 -> parseColor(data, TextStyle.COLOR_INDEX_CURSOR)
            52 -> { /* clipboard */ onCopyTextToClipboard?.invoke(data) }
            104, 105, 106, 110, 111, 112 -> mColors.reset()
        }
    }

    private fun parseColor(data: String, colorIndex: Int) {
        if (data.startsWith("#") && data.length == 7) {
            try {
                val rgb = data.substring(1).toInt(16)
                mColors.mCurrentColors[colorIndex] = 0xFF000000.toInt() or rgb
                onColorsChanged?.invoke()
            } catch (_: NumberFormatException) {}
        }
    }

    private fun emitCodePoint(codePoint: Int) {
        if (codePoint < 32) return

        if (mAboutToAutoWrap) {
            if ((mCurrentDecSetFlags and DECSET_BIT_AUTOWRAP) != 0) {
                doLinefeed()
                mCursorCol = mLeftMargin
            }
            mAboutToAutoWrap = false
        }

        val wcWidth = WcWidth.width(codePoint)
        if (mCursorCol + wcWidth > mRightMargin) {
            mAboutToAutoWrap = true
            mScreen.setLineWrap(mCursorRow, true)
            return
        }

        val drawingChar = mUseLineDrawingUsesG0 && mUseLineDrawingG0 || !mUseLineDrawingUsesG0 && mUseLineDrawingG1
        val actualCodePoint = if (drawingChar) mapLineDrawing(codePoint) else codePoint

        val line = mScreen.getLine(mCursorRow)
        val style = mStyle
        if (mInsertMode) {
            val spaces = mRightMargin - mCursorCol - wcWidth
            if (spaces > 0) {
                val col = mRightMargin - spaces
                for (c in col - 1 downTo mCursorCol) {
                    line.mText[c + 1] = line.mText[c]
                }
            }
        }
        for (c in 0 until wcWidth) {
            val col = mCursorCol + c
            if (col < mColumns) {
                line.mText[col] = if (c == 0) actualCodePoint.toChar() else 0.toChar()
            }
        }
        mCursorCol += wcWidth
    }

    private fun mapLineDrawing(codePoint: Int): Int {
        return when (codePoint) {
            0x6A -> 0x2518; 0x6B -> 0x2510; 0x6C -> 0x250C; 0x6D -> 0x2514
            0x6E -> 0x253C; 0x6F -> 0x23BA; 0x70 -> 0x23BB; 0x71 -> 0x2500
            0x72 -> 0x23BC; 0x73 -> 0x23BD; 0x74 -> 0x251C; 0x75 -> 0x2524
            0x76 -> 0x2534; 0x77 -> 0x252C; 0x78 -> 0x2502; 0x79 -> 0x2264
            0x7A -> 0x2265; 0x7B -> 0x03C0; 0x7C -> 0x2260; 0x7D -> 0x00A3
            0x7E -> 0x00B7; else -> codePoint
        }
    }

    private fun doLinefeed() {
        if (mCursorRow >= mBottomMargin - 1) {
            scrollDown(1)
        }
        mCursorRow = (mCursorRow + 1).coerceAtMost(mRows - 1)
        if (mAboutToAutoWrap) mAboutToAutoWrap = false
    }

    private fun scrollDown(amount: Int) {
        val count = amount.coerceAtMost(mBottomMargin - mTopMargin)
        for (i in 0 until count) {
            val firstLine = mScreen.getLine(mTopMargin)
            if (mScreen === mMainBuffer) {
                val newRow = (mTopMargin + mScreen.mTotalRows - 1) % mScreen.mTotalRows
                val newLine = mScreen.getLine(mTopMargin + mScreen.mScreenRows)
                newLine.clear(mStyle)
                mScreen.mActiveTranscriptRows = minOf(mScreen.mActiveTranscriptRows + 1, mScreen.mTotalRows - mScreen.mScreenRows)
            }
            for (row in mTopMargin until mBottomMargin - 1) {
                val src = mScreen.getLine(row + 1)
                val dst = mScreen.getLine(row)
                src.mText.copyInto(dst.mText)
                dst.mSpaceUsed = src.mSpaceUsed
                dst.mIsLineWrap = src.mIsLineWrap
            }
            val lastLine = mScreen.getLine(mBottomMargin - 1)
            lastLine.clear(mStyle)
        }
        mScrollCounter += count
    }

    private fun scrollUp(amount: Int) {
        val count = amount.coerceAtMost(mBottomMargin - mTopMargin)
        for (i in 0 until count) {
            for (row in mBottomMargin - 1 downTo mTopMargin + 1) {
                val src = mScreen.getLine(row - 1)
                val dst = mScreen.getLine(row)
                src.mText.copyInto(dst.mText)
                dst.mSpaceUsed = src.mSpaceUsed
                dst.mIsLineWrap = src.mIsLineWrap
            }
            val firstLine = mScreen.getLine(mTopMargin)
            firstLine.clear(mStyle)
        }
    }

    private fun reverseIndex() {
        if (mCursorRow > mTopMargin) {
            mCursorRow--
        } else if (mCursorRow == mTopMargin) {
            scrollUp(1)
        }
    }

    private fun cursorUp(count: Int) {
        mCursorRow = (mCursorRow - count).coerceAtLeast(0)
        mAboutToAutoWrap = false
    }

    private fun cursorDown(count: Int) {
        mCursorRow = (mCursorRow + count).coerceAtMost(mRows - 1)
        mAboutToAutoWrap = false
    }

    private fun cursorLeft(count: Int) {
        mCursorCol = (mCursorCol - count).coerceAtLeast(mLeftMargin)
        mAboutToAutoWrap = false
    }

    private fun cursorRight(count: Int) {
        mCursorCol = (mCursorCol + count).coerceAtMost(mRightMargin - 1)
        mAboutToAutoWrap = false
    }

    private fun eraseDisplay(param: Int) {
        when (param) {
            0 -> eraseBelow()
            1 -> eraseAbove()
            2, 3 -> eraseAll()
        }
    }

    private fun eraseBelow() {
        eraseLine(0)
        val style = mStyle
        for (row in mCursorRow + 1 until mRows) {
            mScreen.getLine(row).clear(style)
        }
    }

    private fun eraseAbove() {
        eraseLine(0)
        val style = mStyle
        for (row in 0 until mCursorRow) {
            mScreen.getLine(row).clear(style)
        }
        mCursorCol = mLeftMargin; mCursorRow = 0
    }

    private fun eraseAll() {
        val style = mStyle
        for (row in 0 until mRows) {
            mScreen.getLine(row).clear(style)
        }
        mCursorRow = 0; mCursorCol = mLeftMargin
    }

    private fun eraseLine(param: Int) {
        val style = mStyle
        when (param) {
            0 -> mScreen.getLine(mCursorRow).clear(style, mCursorCol, mColumns)
            1 -> mScreen.getLine(mCursorRow).clear(style, 0, mCursorCol + 1)
            2 -> mScreen.getLine(mCursorRow).clear(style, 0, mColumns)
        }
    }

    private fun deleteChars(count: Int) {
        val line = mScreen.getLine(mCursorRow)
        val moved = (mColumns - mCursorCol - count).coerceAtLeast(0)
        for (col in mCursorCol until mCursorCol + moved) {
            if (col + count < mColumns) {
                line.mText[col] = line.mText[col + count]
            }
        }
        for (col in (mCursorCol + moved) until mColumns) {
            line.mText[col] = ' '
        }
    }

    private fun eraseChars(count: Int) {
        val line = mScreen.getLine(mCursorRow)
        val end = (mCursorCol + count).coerceAtMost(mColumns)
        for (col in mCursorCol until end) {
            line.mText[col] = ' '
        }
    }

    private fun insertLines(count: Int) {
        scrollUp(count)
    }

    private fun deleteLines(count: Int) {
        val c = count.coerceAtMost(mBottomMargin - mCursorRow)
        for (i in 0 until c) {
            for (row in mCursorRow until mBottomMargin - 1) {
                val src = mScreen.getLine(row + 1)
                val dst = mScreen.getLine(row)
                src.mText.copyInto(dst.mText)
                dst.mSpaceUsed = src.mSpaceUsed
                dst.mIsLineWrap = src.mIsLineWrap
            }
            mScreen.getLine(mBottomMargin - 1).clear(mStyle)
        }
    }

    private fun insertChars(count: Int) {
        val line = mScreen.getLine(mCursorRow)
        val moved = (mColumns - mCursorCol - count).coerceAtLeast(0)
        for (col in (mColumns - moved - 1) downTo mCursorCol) {
            line.mText[col + count] = line.mText[col]
        }
        for (col in mCursorCol until (mCursorCol + count).coerceAtMost(mColumns)) {
            line.mText[col] = ' '
        }
    }

    private fun repeatChar(count: Int) {
        // Repeat last emitted character - simplified
    }

    private fun nextTabStop(count: Int): Int {
        var col = mCursorCol
        for (i in 0 until count) {
            col++
            while (col < mColumns && !mTabStop[col]) col++
        }
        return col.coerceAtMost(mRightMargin - 1)
    }

    private fun prevTabStop(count: Int): Int {
        var col = mCursorCol
        for (i in 0 until count) {
            col--
            while (col > 0 && !mTabStop[col]) col--
        }
        return col.coerceAtLeast(mLeftMargin)
    }
}
