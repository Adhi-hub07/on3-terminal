package com.on3.terminal

import android.app.Application
import com.on3.terminal.core.TerminalColors

class TermiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TermiteApplication
            private set
    }
}
