package io.github.yorulmazsinan.kokpit.dock

/**
 * Packages tried, in order, when the user hasn't picked an app for a slot. Vendor apps
 * (Bluetooth, phone, radio) differ per head unit; add them from diagnostics reports.
 */
object KnownPackages {
    val candidates: Map<DockSlot, List<String>> = mapOf(
        // FF/TS head units (MT8227L): screens are only tagged for the vendor launcher,
        // so they are listed as components. PhoneLink is Apple CarPlay; EasyConnection is
        // Android Auto/mirroring and only a fallback.
        DockSlot.CARPLAY to listOf(
            "com.ts.carplayapp/com.autochips.carplayapp.LoadingActivity",
            "com.zjinnova.zlink",
            "net.easyconn",
        ),
        DockSlot.RADIO to listOf("com.ts.MainUI/com.ts.main.radio.RadioMainActivity"),
        DockSlot.BLUETOOTH to listOf("com.ts.MainUI/com.ts.bt.BtMusicActivity"),
        DockSlot.PHONE to listOf("com.ts.MainUI/com.ts.bt.BtConnectActivity"),
        DockSlot.MAPS to listOf(
            "com.google.android.apps.maps",
            "ru.yandex.yandexnavi",
            "ru.yandex.yandexmaps",
            "com.waze",
        ),
    )
}
