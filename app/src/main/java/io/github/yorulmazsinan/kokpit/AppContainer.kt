package io.github.yorulmazsinan.kokpit

import android.content.Context
import io.github.yorulmazsinan.kokpit.drive.DriveRepository
import io.github.yorulmazsinan.kokpit.drive.LocationSource
import io.github.yorulmazsinan.kokpit.drive.TripTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired holder for the app's long-lived repositories. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val locationSource = LocationSource(appContext)
    val drive = DriveRepository(scope, locationSource, TripTracker())
}
