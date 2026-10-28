package io.github.yorulmazsinan.kokpit

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.yorulmazsinan.kokpit.dock.AppPickerScreen
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.drawer.AppDrawerScreen
import io.github.yorulmazsinan.kokpit.home.HomeRoute
import io.github.yorulmazsinan.kokpit.home.HomeUiState
import io.github.yorulmazsinan.kokpit.home.HomeViewModel
import io.github.yorulmazsinan.kokpit.headunit.HeadUnitPhone
import io.github.yorulmazsinan.kokpit.intro.IntroSettingsScreen
import io.github.yorulmazsinan.kokpit.media.NowPlayingState
import io.github.yorulmazsinan.kokpit.music.BtMusicScreen
import io.github.yorulmazsinan.kokpit.phone.PhoneScreen
import io.github.yorulmazsinan.kokpit.radio.RADIO_PRESET_SLOTS
import io.github.yorulmazsinan.kokpit.radio.RadioScreen
import io.github.yorulmazsinan.kokpit.system.safeStart
import io.github.yorulmazsinan.kokpit.settings.SettingsScreen
import io.github.yorulmazsinan.kokpit.system.ActivityExplorerScreen
import io.github.yorulmazsinan.kokpit.system.DiagnosticsScreen
import io.github.yorulmazsinan.kokpit.system.PackageActivitiesScreen
import io.github.yorulmazsinan.kokpit.system.SystemToolsScreen
import io.github.yorulmazsinan.kokpit.ui.theme.DesignScale
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KokpitApp).container
        setContent {
            val home: HomeViewModel = viewModel { HomeViewModel(container) }
            val state by home.state.collectAsStateWithLifecycle()
            KokpitTheme(dark = state.dark, accent = state.accent) {
                KokpitNavHost(container, state)
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val DRAWER = "drawer"
    const val SETTINGS = "settings"
    const val INTRO = "intro"
    const val PHONE = "phone"
    const val MUSIC = "music"
    const val RADIO = "radio"
    const val PICKER = "picker/{slot}"
    const val SYSTEM_TOOLS = "system-tools"
    const val EXPLORER = "explorer"
    const val PACKAGE_ACTIVITIES = "explorer/{pkg}"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
private fun KokpitNavHost(container: AppContainer, homeState: HomeUiState) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            DesignScale {
                HomeRoute(
                    container = container,
                    state = homeState,
                    onOpenDrawer = { nav.navigate(Routes.DRAWER) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onPickApp = { nav.navigate("picker/${it.name}") },
                    onOpenScreen = { nav.navigate(it) },
                )
            }
        }
        composable(Routes.PICKER) { entry ->
            val slot = DockSlot.valueOf(entry.arguments?.getString("slot").orEmpty())
            AppPickerScreen(slot, onBack = { nav.popBackStack() }) { pkg ->
                scope.launch { container.dock.setOverride(slot, pkg) }
                nav.popBackStack()
            }
        }
        composable(Routes.DRAWER) { AppDrawerScreen(container.installedApps, onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onPickApp = { nav.navigate("picker/${it.name}") },
                onOpenIntro = { nav.navigate(Routes.INTRO) },
                onOpenSystemTools = { nav.navigate(Routes.SYSTEM_TOOLS) },
                onOpenDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) },
                onOpenPreview = { nav.navigate(it) },
            )
        }
        composable(Routes.INTRO) { IntroSettingsScreen(container, onBack = { nav.popBackStack() }) }
        composable(Routes.SYSTEM_TOOLS) {
            SystemToolsScreen(
                onBack = { nav.popBackStack() },
                onOpenExplorer = { nav.navigate(Routes.EXPLORER) },
                onOpenDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(Routes.EXPLORER) {
            ActivityExplorerScreen(onBack = { nav.popBackStack() }, onPickPackage = { nav.navigate("explorer/$it") })
        }
        composable(Routes.PACKAGE_ACTIVITIES) { entry ->
            PackageActivitiesScreen(entry.arguments?.getString("pkg").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.PHONE) {
            val device by container.headUnit.btDevice.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { container.headUnit.refresh() }
            DesignScale {
                PhoneScreen(
                    calls = homeState.recentCalls,
                    deviceName = device,
                    onBack = { nav.popBackStack() },
                    vendorDial = { number -> container.headUnit.phone?.dial(number) == true },
                )
            }
        }
        composable(Routes.MUSIC) {
            val vendor = container.headUnit.phone
            LaunchedEffect(Unit) { withContext(Dispatchers.IO) { vendor?.openMusic() } }
            fun vendorOr(fallback: () -> Unit, action: HeadUnitPhone.() -> Boolean) {
                scope.launch { if (withContext(Dispatchers.IO) { vendor?.action() } != true) fallback() }
            }
            val playing = (homeState.media as? NowPlayingState.Playing)?.nowPlaying?.isPlaying == true
            DesignScale {
                BtMusicScreen(
                    state = homeState.media,
                    onBack = { nav.popBackStack() },
                    onPrevious = { vendorOr(container.nowPlaying::previous) { previous() } },
                    onPlayPause = { vendorOr(container.nowPlaying::playPause) { if (playing) pause() else play() } },
                    onNext = { vendorOr(container.nowPlaying::next) { next() } },
                    onGrantAccess = { safeStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                )
            }
        }
        composable(Routes.RADIO) {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val live by container.headUnit.frequency.collectAsStateWithLifecycle()
            val radio = container.headUnit.radio
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) { radio?.open() }
                while (true) {
                    container.headUnit.refresh()
                    delay(1000)
                }
            }
            val frequency = live ?: settings.radioFrequency
            fun tune(f: Int) {
                scope.launch {
                    withContext(Dispatchers.IO) { radio?.tune(f) }
                    container.settings.update { it.copy(radioFrequency = f) }
                    container.headUnit.refresh()
                }
            }
            DesignScale {
                RadioScreen(
                    frequency = frequency,
                    presets = settings.radioPresets,
                    onTune = ::tune,
                    onSavePreset = {
                        scope.launch {
                            container.settings.update {
                                it.copy(radioPresets = (it.radioPresets - frequency + frequency).takeLast(RADIO_PRESET_SLOTS))
                            }
                        }
                    },
                    onOpenVendorRadio = {
                        homeState.dock[DockSlot.RADIO]?.let(container.installedApps::launchIntent)?.let { safeStart(context, it) }
                    },
                    onBack = { nav.popBackStack() },
                    connected = radio != null,
                    onSeek = { direction ->
                        scope.launch {
                            withContext(Dispatchers.IO) { if (direction > 0) radio?.seekUp() else radio?.seekDown() }
                        }
                    },
                )
            }
        }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { nav.popBackStack() }) }
    }
}

