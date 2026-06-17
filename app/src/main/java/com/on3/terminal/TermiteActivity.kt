package com.on3.terminal

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.on3.terminal.core.TerminalSession
import com.on3.terminal.view.TerminalView

class TermiteActivity : AppCompatActivity(), ServiceConnection {

    private var service: TermiteService? = null
    private lateinit var terminalView: TerminalView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sessionsListView: ListView
    private var sessionsAdapter: SessionsAdapter? = null
    private var lastToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termite)

        terminalView = findViewById(R.id.terminal_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        sessionsListView = findViewById(R.id.sessions_list)

        setupTerminalView()
        setupDrawer()

        val intent = Intent(this, TermiteService::class.java)
        startService(intent)
        bindService(intent, this, 0)
    }

    override fun onDestroy() {
        service?.let {
            it.setCurrentSession(null)
            it.getSessions().forEach { session ->
                session.onTextChanged = null
                session.onTitleChanged = null
                session.onSessionFinished = null
            }
        }
        unbindService(this)
        super.onDestroy()
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = (binder as TermiteService.LocalBinder).service
        service?.let { svc ->
            val sessions = svc.getSessions()
            if (sessions.isEmpty()) {
                svc.createSession()
            }
            val targetSession = svc.getCurrentSession() ?: svc.getSessions().lastOrNull()
            targetSession?.let { attachSession(it) }
            svc.setCurrentSession(targetSession)
            setupSessionsList(svc)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawers()
        } else {
            finish()
        }
    }

    private fun setupTerminalView() {
        terminalView.onSingleTapUp = { requestFocusOnTerminal() }
        terminalView.onLongPress = { false }
        terminalView.onScale = { scale -> scale.coerceIn(0.5f, 3f) }
    }

    private fun setupDrawer() {
        findViewById<ImageButton>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.new_session_button).setOnClickListener {
            service?.let { svc ->
                val session = svc.createSession()
                svc.setCurrentSession(session)
                attachSession(session)
                sessionsAdapter?.notifyDataSetChanged()
                drawerLayout.closeDrawers()
            }
        }
    }

    private fun setupSessionsList(svc: TermiteService) {
        sessionsAdapter = SessionsAdapter(svc.getSessions(), svc)
        sessionsListView.adapter = sessionsAdapter
        sessionsListView.setOnItemClickListener { _, _, position, _ ->
            val session = svc.getSessions().getOrNull(position) ?: return@setOnItemClickListener
            svc.setCurrentSession(session)
            attachSession(session)
            drawerLayout.closeDrawers()
        }
    }

    private fun attachSession(session: TerminalSession) {
        session.onTextChanged = { terminalView.onScreenUpdated() }
        session.onTitleChanged = { sessionsAdapter?.notifyDataSetChanged() }
        session.onSessionFinished = {
            runOnUiThread {
                showToast("Session completed")
                sessionsAdapter?.notifyDataSetChanged()
            }
        }
        terminalView.attachSession(session)
        terminalView.onScreenUpdated()
    }

    private fun requestFocusOnTerminal() {
        terminalView.requestFocus()
    }

    private fun showToast(text: String) {
        lastToast?.cancel()
        lastToast = Toast.makeText(this, text, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP, 0, 0)
            show()
        }
    }

    private class SessionsAdapter(
        private val sessions: List<TerminalSession>,
        private val svc: TermiteService
    ) : BaseAdapter() {
        override fun getCount(): Int = sessions.size
        override fun getItem(pos: Int): Any = sessions[pos]
        override fun getItemId(pos: Int): Long = pos.toLong()
        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val tv = (convertView as? TextView) ?: TextView(parent.context).apply {
                setPadding(24, 16, 24, 16)
                textSize = 14f
            }
            val session = sessions[pos]
            val title = session.getTitle() ?: session.sessionName ?: "Session ${pos + 1}"
            val pid = if (session.isRunning()) " [${session.shellPid}]" else " [done]"
            tv.text = "$title$pid"
            if (session == svc.getCurrentSession()) {
                tv.setBackgroundColor(0x330000FF.toInt())
            } else {
                tv.setBackgroundColor(0x00000000)
            }
            return tv
        }
    }
}
