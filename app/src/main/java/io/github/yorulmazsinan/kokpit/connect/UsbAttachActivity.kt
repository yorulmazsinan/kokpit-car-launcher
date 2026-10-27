package io.github.yorulmazsinan.kokpit.connect

import android.app.Activity
import android.os.Bundle
import io.github.yorulmazsinan.kokpit.KokpitApp
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.system.safeStart

/**
 * Receives "iPhone plugged in" (Apple USB vendor id). Enabled only while the user has
 * "open CarPlay on USB" on, so it never competes with the CarPlay app otherwise.
 */
class UsbAttachActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KokpitApp).container
        if (container.settings.settings.value.autoCarPlayOnUsb) {
            container.dock.targets.value[DockSlot.CARPLAY]
                ?.let(container.installedApps::launchIntent)
                ?.let { safeStart(this, it) }
        }
        finish()
    }
}
