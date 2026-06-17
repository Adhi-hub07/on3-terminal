package com.on3.terminal.core

object TerminalJNI {
    init {
        System.loadLibrary("termite-core")
    }

    external fun createSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>?,
        envVars: Array<String>?,
        processId: IntArray,
        rows: Int,
        columns: Int,
        cellWidth: Int,
        cellHeight: Int
    ): Int

    external fun setPtyWindowSize(
        fd: Int,
        rows: Int,
        cols: Int,
        cellWidth: Int,
        cellHeight: Int
    )

    external fun waitFor(processId: Int): Int

    external fun close(fileDescriptor: Int)
}
