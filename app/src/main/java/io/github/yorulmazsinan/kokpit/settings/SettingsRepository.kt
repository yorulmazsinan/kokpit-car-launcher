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
import io.github.yorulmazsinan.kokpit.ui.theme.ThemeAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class KokpitSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: ThemeAccent = ThemeAccent.GAUGE_GREEN,
    val hotspotSsid: String? = null,
    val hotspotPassword: String? = null,
    val hotspotPasswordUnreadable: Boolean = false,
    val autoWifi: Boolean = true,
    val autoCarPlayOnUsb: Boolean = false,
    val intro: IntroSettings = IntroSettings(),
    val radioFrequency: Int = 984,
    val radioPresets: List<Int> = emptyList(),
    val useKokpitScreens: Boolean = true,
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
        accent = p[ACCENT]?.let { runCatching { ThemeAccent.valueOf(it) }.getOrNull() } ?: ThemeAccent.GAUGE_GREEN,
        hotspotSsid = p[SSID],
        hotspotPassword = p[PASSWORD]?.let(secrets::decrypt),
        hotspotPasswordUnreadable = p[PASSWORD] != null && p[PASSWORD]?.let(secrets::decrypt) == null,
        autoWifi = p[AUTO_WIFI] ?: true,
        autoCarPlayOnUsb = p[AUTO_CARPLAY] ?: false,
        radioFrequency = p[RADIO_FREQ] ?: 984,
        useKokpitScreens = p[KOKPIT_SCREENS] ?: true,
        radioPresets = p[RADIO_PRESETS]?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty(),
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
        p[ACCENT] = s.accent.name
        p.setOrRemove(SSID, s.hotspotSsid)
        when {
            s.hotspotPassword != null -> runCatching { secrets.encrypt(s.hotspotPassword) }
                .onSuccess { p[PASSWORD] = it }
                .onFailure { p.remove(PASSWORD) }
            !s.hotspotPasswordUnreadable -> p.remove(PASSWORD)
        }
        p[AUTO_WIFI] = s.autoWifi
        p[RADIO_FREQ] = s.radioFrequency
        p[KOKPIT_SCREENS] = s.useKokpitScreens
        p[RADIO_PRESETS] = s.radioPresets.joinToString(",")
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
        val ACCENT = stringPreferencesKey("theme_accent")
        val SSID = stringPreferencesKey("hotspot_ssid")
        val PASSWORD = stringPreferencesKey("hotspot_password_sealed")
        val AUTO_WIFI = booleanPreferencesKey("auto_wifi")
        val RADIO_FREQ = intPreferencesKey("radio_frequency")
        val KOKPIT_SCREENS = booleanPreferencesKey("use_kokpit_screens")
        val RADIO_PRESETS = stringPreferencesKey("radio_presets")
        val AUTO_CARPLAY = booleanPreferencesKey("auto_carplay_usb")
        val INTRO_MODE = stringPreferencesKey("intro_mode")
        val INTRO_SELECTED = stringPreferencesKey("intro_selected")
        val INTRO_EVERY_WAKE = booleanPreferencesKey("intro_every_wake")
        val INTRO_SOUND = booleanPreferencesKey("intro_sound")
        val INTRO_LAST_INDEX = intPreferencesKey("intro_last_index")
    }
}
