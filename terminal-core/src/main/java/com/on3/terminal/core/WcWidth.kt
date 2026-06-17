package com.on3.terminal.core

object WcWidth {
    private const val MAX_CODE_POINT = 0x10FFFF
    private val WIDTHS = IntArray(MAX_CODE_POINT + 1) { 1 }

    init {
        for (cp in 0..MAX_CODE_POINT) {
            when {
                cp < 0x20 || (cp in 0x7F..0x9F) -> WIDTHS[cp] = 0
                cp == 0x00AD || cp in 0x0600..0x0605 ||
                    cp in 0x061C..0x061C || cp in 0x06DD..0x06DD ||
                    cp in 0x070F..0x070F || cp in 0x0890..0x0891 ||
                    cp in 0x08E2..0x08E2 || cp in 0x180B..0x180E ||
                    cp in 0x200B..0x200F || cp in 0x2028..0x202E ||
                    cp in 0x2060..0x2069 || cp in 0xFE00..0xFE0F ||
                    cp in 0xFEFF..0xFEFF || cp in 0xFFF9..0xFFFB ||
                    cp in 0x1D173..0x1D17A || cp in 0xE0001..0xE0001 ||
                    cp in 0xE0020..0xE007F || cp in 0xE0100..0xE01EF ->
                    WIDTHS[cp] = 0
                (cp in 0x1100..0x115F) || (cp in 0x2329..0x232A) ||
                    (cp in 0x2E80..0x303E) || (cp in 0x3040..0xA4CF) ||
                    (cp in 0xAC00..0xD7A3) || (cp in 0xF900..0xFAFF) ||
                    (cp in 0xFE10..0xFE19) || (cp in 0xFE30..0xFE6F) ||
                    (cp in 0xFF01..0xFF60) || (cp in 0xFFE0..0xFFE6) ||
                    (cp in 0x1B000..0x1B0FF) || (cp in 0x1B100..0x1B12F) ||
                    (cp in 0x1F004..0x1F004) || (cp in 0x1F0CF..0x1F0CF) ||
                    (cp in 0x1F18E..0x1F18E) || (cp in 0x1F191..0x1F19A) ||
                    (cp in 0x20000..0x2FFFD) || (cp in 0x30000..0x3FFFD) ->
                    WIDTHS[cp] = 2
            }
        }
    }

    fun width(codePoint: Int): Int {
        return if (codePoint < 0 || codePoint > MAX_CODE_POINT) 1
        else WIDTHS[codePoint]
    }

    fun width(line: CharArray, index: Int): Int {
        if (index >= line.size) return 1
        val c = line[index]
        val cp = if (Character.isHighSurrogate(c) && index + 1 < line.size)
            Character.toCodePoint(c, line[index + 1])
        else c.code
        return width(cp)
    }
}
