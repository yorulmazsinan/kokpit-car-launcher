package io.github.yorulmazsinan.kokpit.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.yorulmazsinan.kokpit.home.ThemeMode
import io.github.yorulmazsinan.kokpit.intro.IntroMode
import io.github.yorulmazsinan.kokpit.intro.IntroSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class KokpitSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val hotspotSsid: String? = null,
    val hotspotPassword: String? = null,
    val autoWifi: Boolean = true,
    val autoCarPlayOnUsb: Boolean = false,
    val intro: IntroSettings = IntroSettings(),
)

class SettingsRepository(
    private val store: DataStore<Preferences>,
    private val secrets: SecretBox,
    scope: CoroutineScope,
) {
    val settings: StateFlow<KokpitSettings> =
        store.data.map(::read).stateIn(scope, SharingStarted.Eagerly, KokpitSettings())

    suspend fun current(): KokpitSettings = read(store.data.first())

    suspend fun update(transform: (KokpitSettings) -> KokpitSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun read(p: Preferences) = KokpitSettings(
        themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
        hotspotSsid = p[SSID],
        hotspotPassword = p[PASSWORD]?.let(secrets::decrypt),
        autoWifi = p[AUTO_WIFI] ?: true,
        autoCarPlayOnUsb = p[AUTO_CARPLAY] ?: false,
        intro = IntroSettings(
            mode = p[INTRO_MODE]?.let { runCatching { IntroMode.valueOf(it) }.getOrNull() } ?: IntroMode.OFF,
            selectedId = p[INTRO_SELECTED],
            everyWake = p[INTRO_EVERY_WAKE] ?: false,
            withSound = p[INTRO_SOUND] ?: false,
            lastPlayedIndex = p[INTRO_LAST_INDEX] ?: -1,
        ),
    )

    private fun write(p: MutablePreferences, s: KokpitSettings) {
        p[THEME] = s.themeMode.name
        p.setOrRemove(SSID, s.hotspotSsid)
        p.setOrRemove(PASSWORD, s.hotspotPassword?.let(secrets::encrypt))
        p[AUTO_WIFI] = s.autoWifi
        p[AUTO_CARPLAY] = s.autoCarPlayOnUsb
        p[INTRO_MODE] = s.intro.mode.name
        p.setOrRemove(INTRO_SELECTED, s.intro.selectedId)
        p[INTRO_EVERY_WAKE] = s.intro.everyWake
        p[INTRO_SOUND] = s.intro.withSound
        p[INTRO_LAST_INDEX] = s.intro.lastPlayedIndex
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val SSID = stringPreferencesKey("hotspot_ssid")
        val PASSWORD = stringPreferencesKey("hotspot_password_sealed")
        val AUTO_WIFI = booleanPreferencesKey("auto_wifi")
        val AUTO_CARPLAY = booleanPreferencesKey("auto_carplay_usb")
        val INTRO_MODE = stringPreferencesKey("intro_mode")
        val INTRO_SELECTED = stringPreferencesKey("intro_selected")
        val INTRO_EVERY_WAKE = booleanPreferencesKey("intro_every_wake")
        val INTRO_SOUND = booleanPreferencesKey("intro_sound")
        val INTRO_LAST_INDEX = intPreferencesKey("intro_last_index")
    }
}
