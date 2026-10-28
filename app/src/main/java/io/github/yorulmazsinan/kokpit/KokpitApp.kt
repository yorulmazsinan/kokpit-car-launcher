package io.github.yorulmazsinan.kokpit

import android.app.Application
import io.github.yorulmazsinan.kokpit.system.installCrashLog

class KokpitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLog(this)
        container = AppContainer(this).also { it.start() }
    }
}
