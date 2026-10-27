package io.github.yorulmazsinan.kokpit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.datastore.preferences.preferencesDataStore
import io.github.yorulmazsinan.kokpit.drive.DriveRepository
import io.github.yorulmazsinan.kokpit.drive.LocationSource
import io.github.yorulmazsinan.kokpit.drive.TripTracker
import io.github.yorulmazsinan.kokpit.weather.OpenMeteoClient
import io.github.yorulmazsinan.kokpit.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private val Context.weatherStore by preferencesDataStore("weather")

/** Hand-wired holder for the app's long-lived repositories. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val http = OkHttpClient()

    val locationSource = LocationSource(appContext)
    val drive = DriveRepository(scope, locationSource, TripTracker())
    val weather = WeatherRepository(OpenMeteoClient(http), appContext.weatherStore, locationSource::lastKnown)

    fun start() {
        drive.start()
        weather.start(scope)
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { weather.refresh() }
            }
        })
    }
}
