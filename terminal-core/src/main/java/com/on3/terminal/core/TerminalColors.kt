package com.on3.terminal.core

class TerminalColors {
    val mCurrentColors = IntArray(259) { 0xFF000000.toInt() or it }

    companion object {
        private val DEFAULT_ANSI_COLORS = intArrayOf(
            0xFF000000.toInt(), 0xFFCC0000.toInt(), 0xFF4E9A06.toInt(), 0xFFC4A000.toInt(),
            0xFF3465A4.toInt(), 0xFF75507B.toInt(), 0xFF06989A.toInt(), 0xFFD3D7CF.toInt(),
            0xFF555753.toInt(), 0xFFEF2929.toInt(), 0xFF8AE234.toInt(), 0xFFFCE94F.toInt(),
            0xFF729FCF.toInt(), 0xFFAD7FA8.toInt(), 0xFF34E2E2.toInt(), 0xFFEEEEEC.toInt()
        )
    }

    init {
        reset()
    }

    fun reset() {
        DEFAULT_ANSI_COLORS.forEachIndexed { i, c -> mCurrentColors[i] = c }
        mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = 0xFFD3D7CF.toInt()
        mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = 0xFF1A1A2E.toInt()
        mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] = 0xFFFFFFFF.toInt()
    }
}
