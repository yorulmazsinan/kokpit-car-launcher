package io.github.yorulmazsinan.kokpit.connect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KokpitService.start(context, coldBoot = true)
    }
}
