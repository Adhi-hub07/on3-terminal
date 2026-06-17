package com.on3.terminal.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Scroller
import com.on3.terminal.core.KeyHandler
import com.on3.terminal.core.TerminalEmulator
import com.on3.terminal.core.TerminalSession

class TerminalView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var session: TerminalSession? = null
        private set
    private var emulator: TerminalEmulator? = null
    private var renderer = TerminalRenderer(12, Typeface.MONOSPACE)
    private var topRow = 0
    private var scrollRemainder = 0f
    private var scaleFactor = 1f
    private var combiningAccent = 0

    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector
    private val scroller = Scroller(context)

    private val cursorBlinkerHandler = Handler(Looper.getMainLooper())
    private val cursorBlinkerRunnable = Runnable { invalidate() }

    var onSingleTapUp: ((MotionEvent) -> Unit)? = null
    var onLongPress: ((MotionEvent) -> Boolean)? = null
    var onScale: ((Float) -> Float)? = null
    var onKeyDown: ((Int, KeyEvent, TerminalSession?) -> Boolean)? = null
    var onCodePoint: ((Int, Boolean, TerminalSession?) -> Boolean)? = null
    var readControlKey: (() -> Boolean)? = null
    var readAltKey: (() -> Boolean)? = null
    var readShiftKey: (() -> Boolean)? = null
    var readFnKey: (() -> Boolean)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                requestFocus()
                onSingleTapUp?.invoke(e)
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (emulator == null) return true
                if (emulator!!.isMouseTrackingActive() && e2.isFromSource(InputDevice.SOURCE_MOUSE)) {
                    emulator!!.sendMouseEvent(TerminalEmulator.MOUSE_LEFT_BUTTON_MOVED,
                        (e2.x / renderer.mFontWidth).toInt() + 1,
                        ((e2.y - renderer.mFontLineSpacingAndAscent) / renderer.mFontLineSpacing).toInt() + 1, true)
                } else {
                    scrollRemainder += distanceY
                    val delta = (scrollRemainder / renderer.mFontLineSpacing).toInt()
                    scrollRemainder -= delta * renderer.mFontLineSpacing
                    doScroll(e2, delta)
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (onLongPress?.invoke(e) != true) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                }
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (emulator == null) return true
                if (!scroller.isFinished) return true
                val SCALE = 0.25f
                scroller.fling(0, topRow, 0, -(velocityY * SCALE).toInt(), 0, 0,
                    -emulator!!.getScreen().getActiveTranscriptRows(), 0)
                post(object : Runnable {
                    override fun run() {
                        if (scroller.isFinished) return
                        val more = scroller.computeScrollOffset()
                        val newY = scroller.currY
                        doScroll(e2, newY - topRow)
                        if (more) post(this)
                    }
                })
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (emulator == null) return true
                scaleFactor *= detector.scaleFactor
                scaleFactor = onScale?.invoke(scaleFactor) ?: scaleFactor.coerceIn(0.5f, 3f)
                renderer = TerminalRenderer((12 * scaleFactor).toInt(), renderer.mTypeface)
                updateSize()
                invalidate()
                return true
            }
        })
    }

    fun attachSession(newSession: TerminalSession) {
        session = newSession
        emulator = newSession.emulator
        topRow = 0
        combiningAccent = 0
        updateSize()
        isVerticalScrollBarEnabled = true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_NULL
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        return object : BaseInputConnection(this, true) {
            override fun finishComposingText(): Boolean {
                super.finishComposingText()
                editable?.let { sendTextToTerminal(it); it.clear() }
                return true
            }

            override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
                super.commitText(text, newCursorPosition)
                if (emulator == null) return true
                editable?.let { sendTextToTerminal(it); it.clear() }
                return true
            }

            override fun deleteSurroundingText(leftLength: Int, rightLength: Int): Boolean {
                val deleteKey = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                for (i in 0 until leftLength) sendKeyEvent(deleteKey)
                return super.deleteSurroundingText(leftLength, rightLength)
            }

            private fun sendTextToTerminal(text: Editable) {
                val len = text.length
                var i = 0
                while (i < len) {
                    val c = text[i]
                    val cp = if (Character.isHighSurrogate(c) && i + 1 < len)
                        Character.toCodePoint(c, text[++i]) else c.code
                    val shift = readShiftKey?.invoke() == true
                    val ctrl = cp <= 31 && cp != 27
                    val codePoint = if (ctrl) {
                        when (cp) {
                            31 -> 95; 30 -> 94; 29 -> 93; 28 -> 92
                            else -> cp + 96
                        }
                    } else {
                        if (shift) Character.toUpperCase(cp) else cp
                    }
                    inputCodePoint(KeyCharacterMap.VIRTUAL_KEYBOARD, if (ctrl) codePoint else cp, ctrl, false)
                    i++
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (emulator == null) return true
        if (onKeyDown?.invoke(keyCode, event, session) == true) { invalidate(); return true }
        if (event.isSystem && keyCode != KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)

        val metaState = event.metaState
        val controlDown = event.isCtrlPressed || readControlKey?.invoke() == true
        val altDown = (metaState and KeyEvent.META_ALT_LEFT_ON) != 0 || readAltKey?.invoke() == true
        val shiftDown = event.isShiftPressed || readShiftKey?.invoke() == true

        var keyMod = 0
        if (controlDown) keyMod = keyMod or KeyHandler.KEYMOD_CTRL
        if (altDown) keyMod = keyMod or KeyHandler.KEYMOD_ALT
        if (shiftDown) keyMod = keyMod or KeyHandler.KEYMOD_SHIFT

        if (handleKeyCode(keyCode, keyMod)) return true

        val bitsToClear = KeyEvent.META_CTRL_MASK or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        val effectiveMeta = event.metaState and bitsToClear.inv()
        val result = event.getUnicodeChar(effectiveMeta)
        if (result == 0) return false

        if ((result and KeyCharacterMap.COMBINING_ACCENT) != 0) {
            if (combiningAccent != 0) inputCodePoint(event.deviceId, combiningAccent, controlDown, altDown)
            combiningAccent = result and KeyCharacterMap.COMBINING_ACCENT_MASK
        } else {
            if (combiningAccent != 0) {
                val combined = KeyCharacterMap.getDeadChar(combiningAccent, result)
                if (combined > 0) combiningAccent = 0; inputCodePoint(event.deviceId, if (combined > 0) combined else result, controlDown, altDown)
            } else {
                inputCodePoint(event.deviceId, result, controlDown, altDown)
            }
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (emulator == null && keyCode != KeyEvent.KEYCODE_BACK) return true
        if (event.isSystem) return super.onKeyUp(keyCode, event)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (emulator != null && event.isFromSource(InputDevice.SOURCE_MOUSE) &&
            event.action == MotionEvent.ACTION_SCROLL) {
            val up = event.getAxisValue(MotionEvent.AXIS_VSCROLL) > 0f
            doScroll(event, if (up) -3 else 3)
            return true
        }
        return false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateSize()
    }

    override fun onDraw(canvas: Canvas) {
        if (emulator == null) {
            canvas.drawColor(0xFF1A1A2E.toInt())
        } else {
            renderer.render(emulator!!, canvas, topRow, -1, -1, -1, -1)
        }
    }

    fun onScreenUpdated() {
        if (emulator == null) return
        val rowsInHistory = emulator!!.getScreen().getActiveTranscriptRows()
        if (topRow < -rowsInHistory) topRow = -rowsInHistory
        if (topRow != 0) {
            if (topRow < -3) awakenScrollBars()
            topRow = 0
        }
        emulator!!.clearScrollCounter()
        invalidate()
    }

    fun updateSize() {
        if (width == 0 || height == 0 || session == null) return
        val newColumns = maxOf(4, (width / renderer.mFontWidth).toInt())
        val newRows = maxOf(4, ((height - renderer.mFontLineSpacingAndAscent) / renderer.mFontLineSpacing))
        session?.updateSize(newColumns, newRows, renderer.mFontWidth.toInt(), renderer.mFontLineSpacing)
        emulator = session?.emulator
        topRow = 0
        scrollTo(0, 0)
        invalidate()
    }

    private fun doScroll(event: MotionEvent, rowsDown: Int) {
        if (emulator == null) return
        val up = rowsDown < 0
        val amount = kotlin.math.abs(rowsDown)
        for (i in 0 until amount) {
            if (emulator!!.isMouseTrackingActive()) {
                emulator!!.sendMouseEvent(
                    if (up) TerminalEmulator.MOUSE_WHEELUP_BUTTON else TerminalEmulator.MOUSE_WHEELDOWN_BUTTON,
                    1, 1, true)
            } else {
                topRow = (topRow + (if (up) -1 else 1))
                    .coerceIn(-emulator!!.getScreen().getActiveTranscriptRows(), 0)
                if (!awakenScrollBars()) invalidate()
            }
        }
    }

    private fun handleKeyCode(keyCode: Int, keyMod: Int): Boolean {
        if (emulator == null) return false
        emulator!!.setCursorBlinkState(true)
        val term = emulator!!
        val code = KeyHandler.getCode(keyCode, keyMod,
            term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode())
        if (code == null) return false
        session?.write(code!!)
        return true
    }

    fun inputCodePoint(eventSource: Int, codePoint: Int, controlDown: Boolean, altDown: Boolean) {
        if (session == null || emulator == null) return
        emulator!!.setCursorBlinkState(true)
        val ctrl = controlDown || readControlKey?.invoke() == true
        val alt = altDown || readAltKey?.invoke() == true

        if (onCodePoint?.invoke(codePoint, ctrl, session) == true) return

        var cp = codePoint
        if (ctrl) {
            cp = when {
                cp in 'a'.code..'z'.code -> cp - 'a'.code + 1
                cp in 'A'.code..'Z'.code -> cp - 'A'.code + 1
                cp == ' '.code || cp == '2'.code -> 0
                cp == '['.code || cp == '3'.code -> 27
                cp == '\\'.code || cp == '4'.code -> 28
                cp == ']'.code || cp == '5'.code -> 29
                cp == '^'.code || cp == '6'.code -> 30
                cp == '_'.code || cp == '7'.code || cp == '/'.code -> 31
                cp == '8'.code -> 127
                else -> cp
            }
        }
        if (cp > -1) session!!.writeCodePoint(alt, cp)
    }

    companion object {
        private const val KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD = KeyCharacterMap.VIRTUAL_KEYBOARD
        private const val KEY_EVENT_SOURCE_SOFT_KEYBOARD = 0
    }
}
