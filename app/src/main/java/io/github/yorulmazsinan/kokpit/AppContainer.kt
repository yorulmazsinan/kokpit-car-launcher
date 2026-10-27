package io.github.yorulmazsinan.kokpit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.datastore.preferences.preferencesDataStore
import io.github.yorulmazsinan.kokpit.calendar.CalendarRepository
import io.github.yorulmazsinan.kokpit.dock.DockRepository
import io.github.yorulmazsinan.kokpit.drawer.InstalledApps
import io.github.yorulmazsinan.kokpit.drive.DriveRepository
import io.github.yorulmazsinan.kokpit.drive.LocationSource
import io.github.yorulmazsinan.kokpit.drive.TripTracker
import io.github.yorulmazsinan.kokpit.home.ThemeMode
import io.github.yorulmazsinan.kokpit.media.NowPlayingRepository
import io.github.yorulmazsinan.kokpit.settings.KeystoreSecretBox
import io.github.yorulmazsinan.kokpit.settings.SettingsRepository
import io.github.yorulmazsinan.kokpit.weather.OpenMeteoClient
import io.github.yorulmazsinan.kokpit.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private val Context.weatherStore by preferencesDataStore("weather")
private val Context.dockStore by preferencesDataStore("dock")
private val Context.settingsStore by preferencesDataStore("settings")

/** Hand-wired holder for the app's long-lived repositories. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val http = OkHttpClient()

    val locationSource = LocationSource(appContext)
    val drive = DriveRepository(scope, locationSource, TripTracker())
    val weather = WeatherRepository(OpenMeteoClient(http), appContext.weatherStore, locationSource::lastKnown)
    val calendar = CalendarRepository(appContext, scope)
    val nowPlaying = NowPlayingRepository(appContext)
    val installedApps = InstalledApps(appContext.packageManager, appContext.packageName)
    val dock = DockRepository(appContext.dockStore, installedApps, scope)
    val settings = SettingsRepository(appContext.settingsStore, KeystoreSecretBox(), scope)
    val themeMode: Flow<ThemeMode> = settings.settings.map { it.themeMode }

    fun start() {
        drive.start()
        weather.start(scope)
        calendar.start()
        nowPlaying.refreshAccess()
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { weather.refresh() }
            }
        })
    }
}
