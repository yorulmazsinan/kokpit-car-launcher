package io.github.yorulmazsinan.kokpit.dock

/**
 * Packages tried, in order, when the user hasn't picked an app for a slot. Vendor apps
 * (Bluetooth, phone, radio) differ per head unit; add them from diagnostics reports.
 */
object KnownPackages {
    val candidates: Map<DockSlot, List<String>> = mapOf(
        DockSlot.CARPLAY to listOf("com.zjinnova.zlink"),
        DockSlot.MAPS to listOf(
            "com.google.android.apps.maps",
            "ru.yandex.yandexnavi",
            "ru.yandex.yandexmaps",
            "com.waze",
        ),
    )
}
