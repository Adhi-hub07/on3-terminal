package com.on3.terminal.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.on3.terminal.core.TerminalEmulator
import com.on3.terminal.core.TextStyle
import com.on3.terminal.core.WcWidth

class TerminalRenderer(textSize: Int, typeface: Typeface = Typeface.MONOSPACE) {
    val mTextSize = textSize
    val mTypeface = typeface
    private val mTextPaint = Paint()

    val mFontWidth: Float
    val mFontLineSpacing: Int
    private val mFontAscent: Int
    val mFontLineSpacingAndAscent: Int

    private val asciiMeasures = FloatArray(127)

    init {
        mTextPaint.typeface = typeface
        mTextPaint.isAntiAlias = true
        mTextPaint.textSize = textSize.toFloat()

        mFontLineSpacing = mTextPaint.fontSpacing.toInt()
        mFontAscent = mTextPaint.ascent().toInt()
        mFontLineSpacingAndAscent = mFontLineSpacing + mFontAscent
        mFontWidth = mTextPaint.measureText("X")

        val sb = StringBuilder(" ")
        for (i in asciiMeasures.indices) {
            sb[0] = i.toChar()
            asciiMeasures[i] = mTextPaint.measureText(sb, 0, 1)
        }
    }

    fun render(
        emulator: TerminalEmulator, canvas: Canvas, topRow: Int,
        selectionY1: Int, selectionX1: Int, selectionY2: Int, selectionX2: Int
    ) {
        val reverseVideo = emulator.isReverseVideo()
        val endRow = topRow + emulator.mRows
        val columns = emulator.mColumns
        val cursorCol = emulator.getCursorCol()
        val cursorRow = emulator.getCursorRow()
        val cursorVisible = emulator.shouldCursorBeVisible()
        val screen = emulator.getScreen()
        val palette = emulator.mColors.mCurrentColors
        val cursorStyle = emulator.getCursorStyle()

        if (reverseVideo) {
            canvas.drawColor(palette[TextStyle.COLOR_INDEX_FOREGROUND])
        }

        var yOffset = mFontLineSpacingAndAscent.toFloat()
        for (row in topRow until endRow) {
            yOffset += mFontLineSpacing

            val cursorX = if (row == cursorRow && cursorVisible) cursorCol else -1

            var selx1 = -1; var selx2 = -1
            if (row >= selectionY1 && row <= selectionY2) {
                selx1 = if (row == selectionY1) selectionX1 else 0
                selx2 = if (row == selectionY2) selectionX2 else emulator.mColumns
            }

            val line = screen.getLine(row)
            val lineText = line.mText
            val charsUsed = line.getSpaceUsed()

            var lastRunStyle = 0L
            var lastRunInsideCursor = false
            var lastRunInsideSelection = false
            var lastRunStartCol = -1
            var lastRunStartIdx = 0
            var lastRunFontWidthMismatch = false
            var charIdx = 0
            var measuredWidth = 0f

            var col = 0
            while (col < columns) {
                val ch = lineText[charIdx]
                val isSurrogate = Character.isHighSurrogate(ch)
                val charsForCp = if (isSurrogate) 2 else 1
                val codePoint = if (isSurrogate) Character.toCodePoint(ch, lineText[charIdx + 1]) else ch.code
                val wcWidth = WcWidth.width(codePoint)
                val insideCursor = (cursorX == col || (wcWidth == 2 && cursorX == col + 1))
                val insideSelection = (col >= selx1 && col <= selx2)
                val style = line.getStyle(col)

                val cpWidth = if (codePoint < asciiMeasures.size) asciiMeasures[codePoint]
                    else mTextPaint.measureText(lineText, charIdx, charsForCp)
                val fontWidthMismatch = kotlin.math.abs(cpWidth / mFontWidth - wcWidth) > 0.01f

                if (style != lastRunStyle || insideCursor != lastRunInsideCursor ||
                    insideSelection != lastRunInsideSelection || fontWidthMismatch != lastRunFontWidthMismatch) {
                    if (col > 0) {
                        val runWidth = col - lastRunStartCol
                        val runChars = charIdx - lastRunStartIdx
                        val cursorColor = if (lastRunInsideCursor)
                            emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] else 0
                        val invertCursor = lastRunInsideCursor && cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK
                        drawTextRun(canvas, lineText, palette, yOffset, lastRunStartCol, runWidth,
                            lastRunStartIdx, runChars, measuredWidth, cursorColor, cursorStyle,
                            lastRunStyle, reverseVideo || invertCursor || lastRunInsideSelection)
                    }
                    measuredWidth = 0f
                    lastRunStyle = style
                    lastRunInsideCursor = insideCursor
                    lastRunInsideSelection = insideSelection
                    lastRunStartCol = col
                    lastRunStartIdx = charIdx
                    lastRunFontWidthMismatch = fontWidthMismatch
                }
                measuredWidth += cpWidth
                col += wcWidth
                charIdx += charsForCp
                while (charIdx < charsUsed && WcWidth.width(lineText, charIdx) <= 0) {
                    charIdx += if (Character.isHighSurrogate(lineText[charIdx])) 2 else 1
                }
            }

            val runWidth = columns - lastRunStartCol
            val runChars = charIdx - lastRunStartIdx
            val cursorColor2 = if (lastRunInsideCursor)
                emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] else 0
            val invertCursor2 = lastRunInsideCursor && cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK
            drawTextRun(canvas, lineText, palette, yOffset, lastRunStartCol, runWidth,
                lastRunStartIdx, runChars, measuredWidth, cursorColor2, cursorStyle,
                lastRunStyle, reverseVideo || invertCursor2 || lastRunInsideSelection)
        }
    }

    private fun drawTextRun(
        canvas: Canvas, text: CharArray, palette: IntArray, y: Float,
        startCol: Int, runWidthColumns: Int, startCharIdx: Int, runWidthChars: Int,
        measuredWidth: Float, cursorColor: Int, cursorStyle: Int,
        textStyle: Long, reverseVideo: Boolean
    ) {
        var foreColor = TextStyle.decodeForeColor(textStyle)
        val effect = TextStyle.decodeEffect(textStyle)
        var backColor = TextStyle.decodeBackColor(textStyle)
        val bold = (effect and TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0
        val underline = (effect and TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE) != 0
        val italic = (effect and TextStyle.CHARACTER_ATTRIBUTE_ITALIC) != 0
        val strike = (effect and TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0
        val dim = (effect and TextStyle.CHARACTER_ATTRIBUTE_DIM) != 0

        if (foreColor and 0xFF000000.toInt() != 0xFF000000.toInt()) {
            if (bold && foreColor in 0..7) foreColor += 8
            foreColor = palette[foreColor]
        }
        if (backColor and 0xFF000000.toInt() != 0xFF000000.toInt()) {
            backColor = palette[backColor]
        }

        val reverseHere = reverseVideo xor ((effect and TextStyle.CHARACTER_ATTRIBUTE_INVERSE) != 0)
        var fg = foreColor; var bg = backColor
        if (reverseHere) { fg = backColor; bg = foreColor }

        val left = startCol * mFontWidth
        val right = left + runWidthColumns * mFontWidth

        var saved = false
        var scaleX = 1f
        var scaledLeft = left; var scaledRight = right
        val mes = measuredWidth / mFontWidth
        if (kotlin.math.abs(mes - runWidthColumns) > 0.01f) {
            canvas.save()
            scaleX = runWidthColumns / mes
            canvas.scale(scaleX, 1f, left, y)
            saved = true
            scaledRight = left + (right - left) * (1f / scaleX)
        }

        if (bg != palette[TextStyle.COLOR_INDEX_BACKGROUND]) {
            mTextPaint.color = bg
            canvas.drawRect(scaledLeft, y - mFontLineSpacingAndAscent + mFontAscent, scaledRight, y, mTextPaint)
        }

        if (cursorColor != 0) {
            mTextPaint.color = cursorColor
            var cursorHeight = (mFontLineSpacingAndAscent - mFontAscent).toFloat()
            if (cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE) cursorHeight /= 4f
            var cr = scaledRight
            if (cursorStyle == TerminalEmulator.TERMINAL_CURSOR_STYLE_BAR) {
                cr = scaledLeft + (cr - scaledLeft) / 4f
            }
            canvas.drawRect(scaledLeft, y - cursorHeight, cr, y, mTextPaint)
        }

        if ((effect and TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE) == 0) {
            if (dim) {
                val r = (fg shr 16 and 0xFF) * 2 / 3
                val g = (fg shr 8 and 0xFF) * 2 / 3
                val b = (fg and 0xFF) * 2 / 3
                fg = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
            mTextPaint.isFakeBoldText = bold
            mTextPaint.isUnderlineText = underline
            mTextPaint.textSkewX = if (italic) -0.35f else 0f
            mTextPaint.isStrikeThruText = strike
            mTextPaint.color = fg

            canvas.drawTextRun(text, startCharIdx, runWidthChars, startCharIdx, runWidthChars,
                scaledLeft, y - mFontLineSpacingAndAscent, false, mTextPaint)
        }

        if (saved) canvas.restore()
    }
}
