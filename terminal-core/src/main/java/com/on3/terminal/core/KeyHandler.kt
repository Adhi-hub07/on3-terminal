package com.on3.terminal.core

import android.view.KeyEvent

object KeyHandler {
    const val KEYMOD_SHIFT = 1
    const val KEYMOD_ALT = 1 shl 1
    const val KEYMOD_CTRL = 1 shl 2
    const val KEYMOD_NUM_LOCK = 1 shl 3

    private val KEY_MAP = mapOf(
        (KeyEvent.KEYCODE_DPAD_UP to 0) to "\u001B[A",
        (KeyEvent.KEYCODE_DPAD_DOWN to 0) to "\u001B[B",
        (KeyEvent.KEYCODE_DPAD_RIGHT to 0) to "\u001B[C",
        (KeyEvent.KEYCODE_DPAD_LEFT to 0) to "\u001B[D",
        (KeyEvent.KEYCODE_DPAD_UP to KEYMOD_SHIFT) to "\u001B[1;2A",
        (KeyEvent.KEYCODE_DPAD_DOWN to KEYMOD_SHIFT) to "\u001B[1;2B",
        (KeyEvent.KEYCODE_DPAD_RIGHT to KEYMOD_SHIFT) to "\u001B[1;2C",
        (KeyEvent.KEYCODE_DPAD_LEFT to KEYMOD_SHIFT) to "\u001B[1;2D",
        (KeyEvent.KEYCODE_DPAD_UP to KEYMOD_ALT) to "\u001B[1;3A",
        (KeyEvent.KEYCODE_DPAD_DOWN to KEYMOD_ALT) to "\u001B[1;3B",
        (KeyEvent.KEYCODE_DPAD_RIGHT to KEYMOD_ALT) to "\u001B[1;3C",
        (KeyEvent.KEYCODE_DPAD_LEFT to KEYMOD_ALT) to "\u001B[1;3D",
        (KeyEvent.KEYCODE_DPAD_UP to KEYMOD_CTRL) to "\u001B[1;5A",
        (KeyEvent.KEYCODE_DPAD_DOWN to KEYMOD_CTRL) to "\u001B[1;5B",
        (KeyEvent.KEYCODE_DPAD_RIGHT to KEYMOD_CTRL) to "\u001B[1;5C",
        (KeyEvent.KEYCODE_DPAD_LEFT to KEYMOD_CTRL) to "\u001B[1;5D",
        (KeyEvent.KEYCODE_F1 to 0) to "\u001BOP",
        (KeyEvent.KEYCODE_F2 to 0) to "\u001BOQ",
        (KeyEvent.KEYCODE_F3 to 0) to "\u001BOR",
        (KeyEvent.KEYCODE_F4 to 0) to "\u001BOS",
        (KeyEvent.KEYCODE_F5 to 0) to "\u001B[15~",
        (KeyEvent.KEYCODE_F6 to 0) to "\u001B[17~",
        (KeyEvent.KEYCODE_F7 to 0) to "\u001B[18~",
        (KeyEvent.KEYCODE_F8 to 0) to "\u001B[19~",
        (KeyEvent.KEYCODE_F9 to 0) to "\u001B[20~",
        (KeyEvent.KEYCODE_F10 to 0) to "\u001B[21~",
        (KeyEvent.KEYCODE_F11 to 0) to "\u001B[23~",
        (KeyEvent.KEYCODE_F12 to 0) to "\u001B[24~",
        (KeyEvent.KEYCODE_DEL to 0) to "\u001B[3~",
        (KeyEvent.KEYCODE_PAGE_UP to 0) to "\u001B[5~",
        (KeyEvent.KEYCODE_PAGE_DOWN to 0) to "\u001B[6~",
        (KeyEvent.KEYCODE_HOME to 0) to "\u001B[H",
        (KeyEvent.KEYCODE_END to 0) to "\u001B[F",
        (KeyEvent.KEYCODE_INSERT to 0) to "\u001B[2~",
        (KeyEvent.KEYCODE_FORWARD_DEL to 0) to "\u001B[3~",
        (KeyEvent.KEYCODE_TAB to 0) to "\t",
        (KeyEvent.KEYCODE_ENTER to 0) to "\r",
        (KeyEvent.KEYCODE_BACK to 0) to "\u001B",
    )

    private val KEY_MAP_APPLICATION = mapOf(
        (KeyEvent.KEYCODE_DPAD_UP to 0) to "\u001BOA",
        (KeyEvent.KEYCODE_DPAD_DOWN to 0) to "\u001BOB",
        (KeyEvent.KEYCODE_DPAD_RIGHT to 0) to "\u001BOC",
        (KeyEvent.KEYCODE_DPAD_LEFT to 0) to "\u001BOD",
    )

    fun getCode(keyCode: Int, keyMod: Int, cursorKeysAppMode: Boolean, keypadAppMode: Boolean): String? {
        if (keypadAppMode) {
            val app = KEY_MAP_APPLICATION[Pair(keyCode, 0)]
            if (app != null) return app
        }
        if (cursorKeysAppMode) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return "\u001BOA"
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return "\u001BOB"
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return "\u001BOC"
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return "\u001BOD"
        }
        val key = Pair(keyCode, keyMod and (KEYMOD_SHIFT or KEYMOD_ALT or KEYMOD_CTRL))
        return KEY_MAP[key]
    }
}
