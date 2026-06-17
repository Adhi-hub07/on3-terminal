package com.on3.terminal.core

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.util.UUID

class TerminalSession(
    val shellPath: String,
    val cwd: String,
    val args: Array<String>?,
    val env: Array<String>?,
    val transcriptRows: Int = 2000
) {
    val handle = UUID.randomUUID().toString()
    var sessionName: String? = null

    var onTextChanged: ((TerminalSession) -> Unit)? = null
    var onTitleChanged: ((TerminalSession) -> Unit)? = null
    var onSessionFinished: ((TerminalSession) -> Unit)? = null
    var onBell: ((TerminalSession) -> Unit)? = null
    var onColorsChanged: ((TerminalSession) -> Unit)? = null
    var onCopyTextToClipboard: ((TerminalSession, String) -> Unit)? = null
    var onPasteTextFromClipboard: ((TerminalSession) -> Unit)? = null

    var emulator: TerminalEmulator? = null
        private set

    private var terminalFileDescriptor = -1
    var shellPid = 0
        private set
    var shellExitStatus = 0
        private set

    private val processToTerminalQueue = ByteQueue(64 * 1024)
    private val terminalToProcessQueue = ByteQueue(4096)
    private val mainThreadHandler = MainThreadHandler(Looper.getMainLooper())

    fun initializeEmulator(columns: Int, rows: Int, cellWidthPx: Int, cellHeightPx: Int) {
        emulator = TerminalEmulator(columns, rows, cellWidthPx, cellHeightPx, transcriptRows)
        val emu = emulator!!
        emu.onTextChanged = { onTextChanged?.invoke(this) }
        emu.onTitleChanged = { onTitleChanged?.invoke(this) }
        emu.onBell = { onBell?.invoke(this) }
        emu.onColorsChanged = { onColorsChanged?.invoke(this) }
        emu.onCopyTextToClipboard = { text -> onCopyTextToClipboard?.invoke(this, text) }
        emu.onPasteTextFromClipboard = { onPasteTextFromClipboard?.invoke(this) }

        val processId = IntArray(1)
        val allArgs = if (args != null && args.isNotEmpty()) args else arrayOf(shellPath)
        terminalFileDescriptor = TerminalJNI.createSubprocess(
            shellPath, cwd, allArgs, env, processId,
            rows, columns, cellWidthPx, cellHeightPx
        )
        shellPid = processId[0]

        val fd = wrapFileDescriptor(terminalFileDescriptor)

        Thread(Runnable {
            try {
                FileInputStream(fd).use { input ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) return@Runnable
                        if (!processToTerminalQueue.write(buffer, 0, read)) return@Runnable
                        mainThreadHandler.sendEmptyMessage(MSG_NEW_INPUT)
                    }
                }
            } catch (_: Exception) {}
        }, "SessionInput[pid=$shellPid]").start()

        Thread(Runnable {
            try {
                FileOutputStream(fd).use { output ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val bytes = terminalToProcessQueue.read(buffer, true)
                        if (bytes == -1) return@Runnable
                        output.write(buffer, 0, bytes)
                    }
                }
            } catch (_: Exception) {}
        }, "SessionOutput[pid=$shellPid]").start()

        Thread(Runnable {
            val exitCode = TerminalJNI.waitFor(shellPid)
            mainThreadHandler.sendMessage(
                mainThreadHandler.obtainMessage(MSG_PROCESS_EXITED, exitCode)
            )
        }, "SessionWaiter[pid=$shellPid]").start()
    }

    fun updateSize(columns: Int, rows: Int, cellWidthPx: Int, cellHeightPx: Int) {
        if (emulator == null) {
            initializeEmulator(columns, rows, cellWidthPx, cellHeightPx)
        } else {
            TerminalJNI.setPtyWindowSize(terminalFileDescriptor, rows, columns, cellWidthPx, cellHeightPx)
            emulator?.resize(columns, rows, cellWidthPx, cellHeightPx)
        }
    }

    fun writeCodePoint(prependEscape: Boolean, codePoint: Int) {
        val buffer = ByteArray(5)
        var pos = 0
        if (prependEscape) buffer[pos++] = 27
        when {
            codePoint <= 0x7F -> buffer[pos++] = codePoint.toByte()
            codePoint <= 0x7FF -> {
                buffer[pos++] = (0xC0 or (codePoint shr 6)).toByte()
                buffer[pos++] = (0x80 or (codePoint and 0x3F)).toByte()
            }
            codePoint <= 0xFFFF -> {
                buffer[pos++] = (0xE0 or (codePoint shr 12)).toByte()
                buffer[pos++] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
                buffer[pos++] = (0x80 or (codePoint and 0x3F)).toByte()
            }
            else -> {
                buffer[pos++] = (0xF0 or (codePoint shr 18)).toByte()
                buffer[pos++] = (0x80 or ((codePoint shr 12) and 0x3F)).toByte()
                buffer[pos++] = (0x80 or ((codePoint shr 6) and 0x3F)).toByte()
                buffer[pos++] = (0x80 or (codePoint and 0x3F)).toByte()
            }
        }
        write(buffer, 0, pos)
    }

    fun write(data: ByteArray, offset: Int, count: Int) {
        if (shellPid > 0) terminalToProcessQueue.write(data, offset, count)
    }

    fun write(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        write(bytes, 0, bytes.size)
    }

    fun isRunning(): Boolean = shellPid != -1

    fun finish() {
        if (isRunning()) {
            try {
                Os.kill(shellPid, OsConstants.SIGKILL)
            } catch (_: Exception) {}
        }
    }

    fun getTitle(): String? = emulator?.mTitle

    fun getProcessCwd(): String? {
        if (shellPid < 1) return null
        return try {
            val symlink = "/proc/$shellPid/cwd/"
            val path = File(symlink).canonicalPath
            if (path.endsWith("/")) path else path
        } catch (_: Exception) { null }
    }

    private fun cleanupResources(exitStatus: Int) {
        shellPid = -1
        shellExitStatus = exitStatus
        terminalToProcessQueue.close()
        processToTerminalQueue.close()
        TerminalJNI.close(terminalFileDescriptor)
    }

    private fun wrapFileDescriptor(fd: Int): FileDescriptor {
        val result = FileDescriptor()
        try {
            val field = FileDescriptor::class.java.getDeclaredField("descriptor")
            field.isAccessible = true
            field.set(result, fd)
        } catch (_: Exception) {
            try {
                val field = FileDescriptor::class.java.getDeclaredField("fd")
                field.isAccessible = true
                field.set(result, fd)
            } catch (_: Exception) {}
        }
        return result
    }

    private inner class MainThreadHandler(looper: Looper) : Handler(looper) {
        private val receiveBuffer = ByteArray(64 * 1024)

        override fun handleMessage(msg: Message) {
            val bytesRead = processToTerminalQueue.read(receiveBuffer, false)
            if (bytesRead > 0) {
                emulator?.append(receiveBuffer, bytesRead)
                onTextChanged?.invoke(this@TerminalSession)
            }
            if (msg.what == MSG_PROCESS_EXITED) {
                val exitCode = msg.obj as Int
                cleanupResources(exitCode)
                val desc = "\r\n[Process completed" +
                    (if (exitCode > 0) " (code $exitCode)" else if (exitCode < 0) " (signal ${-exitCode})" else "") +
                    " - press Enter]"
                emulator?.append(desc.toByteArray(Charsets.UTF_8), desc.length)
                onTextChanged?.invoke(this@TerminalSession)
                onSessionFinished?.invoke(this@TerminalSession)
            }
        }
    }

    companion object {
        private const val MSG_NEW_INPUT = 1
        private const val MSG_PROCESS_EXITED = 4
    }
}
