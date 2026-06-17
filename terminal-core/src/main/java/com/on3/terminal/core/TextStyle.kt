package com.on3.terminal.core

object TextStyle {
    const val COLOR_INDEX_FOREGROUND = 256
    const val COLOR_INDEX_BACKGROUND = 257
    const val COLOR_INDEX_CURSOR = 258

    const val CHARACTER_ATTRIBUTE_BOLD = 1
    const val CHARACTER_ATTRIBUTE_UNDERLINE = 1 shl 1
    const val CHARACTER_ATTRIBUTE_BLINK = 1 shl 2
    const val CHARACTER_ATTRIBUTE_INVERSE = 1 shl 3
    const val CHARACTER_ATTRIBUTE_INVISIBLE = 1 shl 4
    const val CHARACTER_ATTRIBUTE_PROTECTED = 1 shl 5
    const val CHARACTER_ATTRIBUTE_DIM = 1 shl 6
    const val CHARACTER_ATTRIBUTE_ITALIC = 1 shl 7
    const val CHARACTER_ATTRIBUTE_STRIKETHROUGH = 1 shl 8

    private const val FOREGROUND_MASK = 0x1FF
    private const val BACKGROUND_MASK = 0x3FE00
    private const val EFFECT_MASK = 0x7FC0000
    private const val FOREGROUND_SHIFT = 0
    private const val BACKGROUND_SHIFT = 9
    private const val EFFECT_SHIFT = 18

    fun encode(foregroundColor: Int, backgroundColor: Int, effect: Int): Long {
        return (foregroundColor.toLong() shl FOREGROUND_SHIFT) or
                (backgroundColor.toLong() shl BACKGROUND_SHIFT) or
                (effect.toLong() shl EFFECT_SHIFT)
    }

    fun decodeForeColor(style: Long): Int {
        return ((style shr FOREGROUND_SHIFT).toInt() and FOREGROUND_MASK)
    }

    fun decodeBackColor(style: Long): Int {
        return ((style shr BACKGROUND_SHIFT).toInt() and FOREGROUND_MASK)
    }

    fun decodeEffect(style: Long): Int {
        return ((style shr EFFECT_SHIFT).toInt()) and 0x1FF
    }
}
