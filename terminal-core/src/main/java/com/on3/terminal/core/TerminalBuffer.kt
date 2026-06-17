package com.on3.terminal.core

class TerminalBuffer(
    val mColumns: Int,
    val mTotalRows: Int,
    val mScreenRows: Int
) {
    private val mLines = Array(mTotalRows) { TerminalRow(mColumns) }
    private val mLineStyle = LongArray(mTotalRows)
    private val mLineWrap = BooleanArray(mTotalRows)
    var mActiveTranscriptRows = 0

    private fun normalizeRow(row: Int): Int {
        return ((row % mTotalRows) + mTotalRows) % mTotalRows
    }

    fun getLine(row: Int): TerminalRow = mLines[normalizeRow(row)]

    fun getLineWrap(row: Int): Boolean = mLineWrap[normalizeRow(row)]

    fun setLineWrap(row: Int, wrap: Boolean) {
        mLineWrap[normalizeRow(row)] = wrap
    }

    fun clearLineWrap(row: Int) {
        mLineWrap[normalizeRow(row)] = false
    }

    fun getActiveRows(): Int = mScreenRows + mActiveTranscriptRows

    fun getActiveTranscriptRows(): Int = mActiveTranscriptRows

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): CharSequence {
        val sb = StringBuilder()
        for (r in startRow until endRow) {
            val line = getLine(r)
            val cols = line.getSpaceUsed()
            for (c in 0 until mColumns) {
                if (r == startRow && c < startCol) continue
                if (r == endRow - 1 && c >= endCol) break
                if (c < cols && line.mText[c] != 0.toChar()) {
                    sb.append(line.mText[c])
                } else {
                    sb.append(' ')
                }
            }
            if (r < endRow - 1) sb.append('\n')
        }
        return sb
    }

    fun resize(newColumns: Int, newScreenRows: Int, newTotalRows: Int, cursor: IntArray, style: Long, altBuffer: Boolean) {
        TODO("Not implemented yet")
    }

    fun blockCopy(srcLeft: Int, srcTop: Int, width: Int, height: Int, dstLeft: Int, dstTop: Int) {
        for (row in 0 until height) {
            val srcRow = getLine(srcTop + row)
            val dstRow = getLine(dstTop + row)
            for (col in 0 until width) {
                val srcCol = srcLeft + col
                val dstCol = dstLeft + col
                if (srcCol < mColumns && dstCol < mColumns) {
                    dstRow.mText[dstCol] = srcRow.mText[srcCol]
                }
            }
        }
    }

    fun setChar(col: Int, row: Int, char: Int, style: Long) {
        val line = getLine(row)
        val wcWidth = WcWidth.width(char)
        for (c in 0 until wcWidth) {
            if (col + c < mColumns) {
                line.setChar(col + c, if (c == 0) char else 0, style, wcWidth)
            }
        }
    }

    fun setOrClearEffect(bits: Int, setOrClear: Boolean, reverse: Boolean, rectangularChangeAttribute: Boolean,
                         leftMargin: Int, rightMargin: Int, top: Int, left: Int, bottom: Int, right: Int) {
        for (row in top..bottom) {
            val line = getLine(row)
            for (col in left..right) {
                if (col < mColumns) {
                    var style = line.getStyle(col)
                    var effect = TextStyle.decodeEffect(style)
                    if (reverse) {
                        effect = effect xor bits
                    } else if (setOrClear) {
                        effect = effect or bits
                    } else {
                        effect = effect and bits.inv()
                    }
                    val fore = TextStyle.decodeForeColor(style)
                    val back = TextStyle.decodeBackColor(style)
                    line.mStyles[col] = TextStyle.encode(fore, back, effect)
                }
            }
        }
    }
}
