package io.github.yorulmazsinan.kokpit

import android.app.Application

class KokpitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this).also { it.start() }
    }
}
