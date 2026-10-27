package io.github.yorulmazsinan.kokpit

import android.content.Context

/** Hand-wired holder for the app's long-lived repositories. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}
