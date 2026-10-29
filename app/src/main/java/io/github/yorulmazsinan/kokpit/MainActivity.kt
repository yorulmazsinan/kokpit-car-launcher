package io.github.yorulmazsinan.kokpit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.yorulmazsinan.kokpit.calls.CallBar
import io.github.yorulmazsinan.kokpit.dock.AppPickerScreen
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.drawer.AppDrawerScreen
import io.github.yorulmazsinan.kokpit.headunit.HeadUnitPhone
import io.github.yorulmazsinan.kokpit.headunit.VendorSource
import io.github.yorulmazsinan.kokpit.home.HomeRoute
import io.github.yorulmazsinan.kokpit.home.HomeUiState
import io.github.yorulmazsinan.kokpit.home.HomeViewModel
import io.github.yorulmazsinan.kokpit.home.cards.Dock
import io.github.yorulmazsinan.kokpit.intro.IntroSettingsScreen
import io.github.yorulmazsinan.kokpit.media.NowPlayingState
import io.github.yorulmazsinan.kokpit.music.BtMusicScreen
import io.github.yorulmazsinan.kokpit.phone.PhoneScreen
import io.github.yorulmazsinan.kokpit.player.PlayerScreen
import io.github.yorulmazsinan.kokpit.radio.RADIO_PRESET_SLOTS
import io.github.yorulmazsinan.kokpit.radio.RadioScreen
import io.github.yorulmazsinan.kokpit.settings.QuickSettingsScreen
import io.github.yorulmazsinan.kokpit.settings.SettingsScreen
import io.github.yorulmazsinan.kokpit.system.ActivityExplorerScreen
import io.github.yorulmazsinan.kokpit.system.DiagnosticsScreen
import io.github.yorulmazsinan.kokpit.system.PackageActivitiesScreen
import io.github.yorulmazsinan.kokpit.system.SystemToolsScreen
import io.github.yorulmazsinan.kokpit.system.safeStart
import io.github.yorulmazsinan.kokpit.ui.components.DockedLayout
import io.github.yorulmazsinan.kokpit.ui.theme.DesignScale
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KokpitApp).container
        hideSystemBars()
        setContent {
            val home: HomeViewModel = viewModel { HomeViewModel(container) }
            val state by home.state.collectAsStateWithLifecycle()
            KokpitTheme(dark = state.dark, accent = state.accent) {
                // One scale for every screen, so sizes match wherever you go.
                DesignScale { KokpitShell(container, state) }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Kokpit owns the whole screen; a swipe from the edge brings the bars back for a moment. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        /** Back to Kokpit after a vendor screen took over (its audio source keeps playing). */
        fun bringToFront(context: Context) {
            safeStart(context, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
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
    const val QUICK = "quick"
    const val PLAYER = "player"
}

/** Dock slots with a Kokpit screen; those screens and home keep the dock on the bottom. */
private val KOKPIT_SCREENS = mapOf(DockSlot.PHONE to Routes.PHONE, DockSlot.BLUETOOTH to Routes.MUSIC, DockSlot.RADIO to Routes.RADIO)
private val DOCKED_ROUTES = setOf(Routes.HOME, Routes.PLAYER) + KOKPIT_SCREENS.values

@Composable
private fun KokpitShell(container: AppContainer, homeState: HomeUiState) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val active = KOKPIT_SCREENS.entries.firstOrNull { it.value == route }?.key

    fun launchSlot(slot: DockSlot): Boolean {
        val intent = homeState.dock[slot]?.let(container.installedApps::launchIntent) ?: return false
        return safeStart(context, intent)
    }

    fun openSlot(slot: DockSlot) {
        val screen = KOKPIT_SCREENS[slot]
        when {
            slot == DockSlot.APPS -> nav.navigate(Routes.DRAWER)
            // Tapping the lit button again goes home.
            slot == active -> nav.popBackStack(Routes.HOME, inclusive = false)
            screen != null && container.settings.settings.value.useKokpitScreens -> nav.navigate(screen) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
            !launchSlot(slot) -> nav.navigate("picker/${slot.name}")
        }
    }

    fun longPressSlot(slot: DockSlot) {
        if (slot == DockSlot.APPS) nav.navigate(Routes.SETTINGS) else nav.navigate("picker/${slot.name}")
    }

    val dock: (@Composable () -> Unit)? = if (route in DOCKED_ROUTES) {
        { Dock(::openSlot, ::longPressSlot, appIcons = homeState.dockIcons, active = active) }
    } else {
        null
    }
    val call by container.callMonitor.call.collectAsStateWithLifecycle()
    val banner: (@Composable () -> Unit)? = call?.let { active ->
        {
            CallBar(
                active,
                onAnswer = { container.callMonitor.answer() },
                onHangUp = { container.callMonitor.hangUp() },
                onOpen = { if (route != Routes.PHONE) nav.navigate(Routes.PHONE) { launchSingleTop = true } },
            )
        }
    }
    DockedLayout(dock, banner) { KokpitNavHost(nav, container, homeState, ::openSlot, ::longPressSlot) }
}

/**
 * Opens the vendor screen for [source] when needed (that is what switches the audio source)
 * and brings Kokpit straight back once it has taken over.
 */
@Composable
private fun SwitchVendorSource(container: AppContainer, source: VendorSource, open: () -> Boolean?) {
    val context = LocalContext.current
    val lifecycle = (context as ComponentActivity).lifecycle
    LaunchedEffect(source) {
        val modes = container.vendorModes
        if (!withContext(Dispatchers.IO) { modes.needsOpen(source) }) return@LaunchedEffect
        if (withContext(Dispatchers.IO) { open() } != true) return@LaunchedEffect
        val left = withTimeoutOrNull(3_000) { lifecycle.currentStateFlow.first { !it.isAtLeast(Lifecycle.State.RESUMED) } }
        if (left != null) MainActivity.bringToFront(context)
        delay(1_500)
        withContext(Dispatchers.IO) { modes.learn(source) }
    }
}

@Composable
private fun KokpitNavHost(
    nav: NavHostController,
    container: AppContainer,
    homeState: HomeUiState,
    onDock: (DockSlot) -> Unit,
    onDockLongPress: (DockSlot) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                container = container,
                state = homeState,
                onOpenSettings = { nav.navigate(Routes.QUICK) },
                onPickApp = { nav.navigate("picker/${it.name}") },
                onDock = onDock,
                onDockLongPress = onDockLongPress,
            )
        }
        composable(Routes.PICKER) { entry ->
            val slot = DockSlot.valueOf(entry.arguments?.getString("slot").orEmpty())
            AppPickerScreen(slot, onBack = { nav.popBackStack() }) { pkg ->
                scope.launch { container.dock.setOverride(slot, pkg) }
                nav.popBackStack()
            }
        }
        composable(Routes.DRAWER) {
            AppDrawerScreen(
                container.installedApps,
                onBack = { nav.popBackStack() },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenPlayer = { nav.navigate(Routes.PLAYER) },
            )
        }
        composable(Routes.QUICK) {
            QuickSettingsScreen(container, onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.PLAYER) { PlayerScreen(container, onBack = { nav.popBackStack() }) }
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
            val calls by container.calls.calls.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                container.headUnit.refresh()
                container.calls.refresh()
            }
            PhoneScreen(
                calls = calls,
                deviceName = device,
                onBack = { nav.popBackStack() },
                vendorDial = { number -> container.headUnit.phone?.dial(number) == true },
            )
        }
        composable(Routes.MUSIC) {
            val vendor = container.headUnit.phone
            SwitchVendorSource(container, VendorSource.BT_MUSIC) { vendor?.openMusic() }
            fun vendorOr(fallback: () -> Unit, action: HeadUnitPhone.() -> Boolean) {
                scope.launch { if (withContext(Dispatchers.IO) { vendor?.action() } != true) fallback() }
            }
            val playing = (homeState.media as? NowPlayingState.Playing)?.nowPlaying?.isPlaying == true
            BtMusicScreen(
                state = homeState.media,
                onBack = { nav.popBackStack() },
                onPrevious = { vendorOr(container.nowPlaying::previous) { previous() } },
                onPlayPause = { vendorOr(container.nowPlaying::playPause) { if (playing) pause() else play() } },
                onNext = { vendorOr(container.nowPlaying::next) { next() } },
                onGrantAccess = { safeStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        composable(Routes.RADIO) {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val live by container.headUnit.frequency.collectAsStateWithLifecycle()
            val radio = container.headUnit.radio
            SwitchVendorSource(container, VendorSource.RADIO) { radio?.open() }
            LaunchedEffect(Unit) {
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
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { nav.popBackStack() }) }
    }
}
