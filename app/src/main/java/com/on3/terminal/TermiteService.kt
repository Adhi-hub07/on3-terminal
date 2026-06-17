package com.on3.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.on3.terminal.core.TerminalSession

class TermiteService : Service() {

    inner class LocalBinder : Binder() {
        val service: TermiteService get() = this@TermiteService
    }

    private val binder = LocalBinder()
    private val sessions = mutableListOf<TerminalSession>()
    private var currentSession: TerminalSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        sessions.forEach { it.finish() }
        sessions.clear()
        super.onDestroy()
    }

    fun createSession(
        shellPath: String = DEFAULT_SHELL,
        cwd: String = filesDir.absolutePath,
        args: Array<String>? = null,
        env: Array<String>? = defaultEnv
    ): TerminalSession {
        val session = TerminalSession(shellPath, cwd, args, env)
        session.onSessionFinished = {
            sessions.remove(it)
            if (sessions.isEmpty()) {
                updateNotification()
            }
        }
        sessions.add(session)
        updateNotification()
        return session
    }

    fun removeSession(session: TerminalSession) {
        session.finish()
        sessions.remove(session)
        updateNotification()
    }

    fun getSessions(): List<TerminalSession> = sessions

    fun getCurrentSession(): TerminalSession? = currentSession

    fun setCurrentSession(session: TerminalSession?) {
        currentSession = session
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, TermiteActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("On3 Terminal")
            .setContentText("${sessions.size} session(s)")
            .setContentIntent(contentIntent)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (sessions.isNotEmpty()) {
            nm.notify(NOTIFICATION_ID, buildNotification())
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Terminal Sessions",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "on3_terminal_sessions"
        private const val NOTIFICATION_ID = 1
        const val DEFAULT_SHELL = "/data/data/com.termux/files/usr/bin/bash"
        val defaultEnv = arrayOf(
            "HOME=/data/data/com.termux/files/home",
            "PREFIX=/data/data/com.termux/files/usr",
            "TMPDIR=/data/data/com.termux/files/usr/tmp",
            "TERM=xterm-256color",
            "SHELL=bash",
            "LANG=en_US.UTF-8",
            "PATH=/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets"
        )
    }
}
