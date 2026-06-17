package com.on3.terminal.core

class TerminalRow(private val columns: Int) {
    val mText = CharArray(columns * 2)
    private val mStyles = LongArray(columns * 2)
    var mSpaceUsed = 0
    var mIsLineWrap = false

    fun getSpaceUsed(): Int = mSpaceUsed

    fun getStyle(column: Int): Long = mStyles[column]

    fun setChar(column: Int, codePoint: Int, style: Long, wcWidth: Int) {
        val index = if (column < mSpaceUsed) column else { mSpaceUsed = column; column }
        if (codePoint < 0x10000) {
            mText[index] = codePoint.toChar()
            mStyles[index] = style
            if (wcWidth == 2 && index + 1 < columns) {
                mText[index + 1] = 0.toChar()
                mStyles[index + 1] = style
            }
        } else {
            mText[index] = Character.highSurrogate(codePoint)
            mStyles[index] = style
            if (index + 1 < columns) {
                mText[index + 1] = Character.lowSurrogate(codePoint)
                mStyles[index + 1] = style
            }
        }
    }

    fun clear(style: Long) {
        for (i in 0 until columns) {
            mText[i] = ' '
            mStyles[i] = style
        }
        mSpaceUsed = columns
        mIsLineWrap = false
    }

    fun clear(style: Long, left: Int, right: Int) {
        for (i in left until right) {
            mText[i] = ' '
            mStyles[i] = style
        }
    }
}
