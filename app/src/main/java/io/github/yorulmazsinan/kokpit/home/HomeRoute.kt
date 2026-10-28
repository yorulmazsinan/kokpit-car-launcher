package io.github.yorulmazsinan.kokpit.home

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.yorulmazsinan.kokpit.AppContainer
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.system.safeStart
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RUNTIME_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.READ_CALL_LOG,
)

/** Dock slots Kokpit has its own screens for (navigation routes). */
private val KOKPIT_SCREENS = mapOf(DockSlot.PHONE to "phone", DockSlot.BLUETOOTH to "music", DockSlot.RADIO to "radio")

@Composable
fun HomeRoute(
    container: AppContainer,
    state: HomeUiState,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickApp: (DockSlot) -> Unit,
    onOpenScreen: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        container.drive.refreshPermission()
        container.calls.refresh()
        container.scope.launch {
            container.calendar.refresh()
            container.weather.refresh()
        }
    }
    LaunchedEffect(Unit) {
        if (container.permissionsRequested) return@LaunchedEffect
        container.permissionsRequested = true
        val missing = RUNTIME_PERMISSIONS.filter { context.checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }
    LifecycleResumeEffect(Unit) {
        container.nowPlaying.refreshAccess()
        container.scope.launch { container.dock.refreshInstalled() }
        onPauseOrDispose { }
    }

    fun launchSlot(slot: DockSlot): Boolean {
        val intent = state.dock[slot]?.let(container.installedApps::launchIntent) ?: return false
        return safeStart(context, intent)
    }

    HomeScreen(
        state = state,
        now = now,
        actions = HomeActions(
            onDock = { slot ->
                val ownScreen = KOKPIT_SCREENS[slot]
                when {
                    slot == DockSlot.APPS -> onOpenDrawer()
                    ownScreen != null && container.settings.settings.value.useKokpitScreens -> onOpenScreen(ownScreen)
                    !launchSlot(slot) -> onPickApp(slot)
                }
            },
            onDockLongPress = { slot -> if (slot == DockSlot.APPS) onOpenSettings() else onPickApp(slot) },
            onOpenSettings = onOpenSettings,
            onOpenCalendar = {
                safeStart(context, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
            },
            onOpenMaps = { if (!launchSlot(DockSlot.MAPS)) onPickApp(DockSlot.MAPS) },
            onRefreshWeather = { container.scope.launch { container.weather.refresh() } },
            onAssistant = {
                if (!launchSlot(DockSlot.ASSISTANT)) {
                    Toast.makeText(context, R.string.assistant_not_set, Toast.LENGTH_LONG).show()
                }
            },
            onPlayPause = container.nowPlaying::playPause,
            onNext = container.nowPlaying::next,
            onPrevious = container.nowPlaying::previous,
            onRequestPermissions = { permissionLauncher.launch(RUNTIME_PERMISSIONS) },
            onGrantMediaAccess = { safeStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        ),
    )
}
