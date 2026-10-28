package io.github.yorulmazsinan.kokpit.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import androidx.lifecycle.viewModelScope
import io.github.yorulmazsinan.kokpit.AppContainer
import io.github.yorulmazsinan.kokpit.calendar.CalendarState
import io.github.yorulmazsinan.kokpit.drive.TripSnapshot
import io.github.yorulmazsinan.kokpit.media.NowPlayingState
import io.github.yorulmazsinan.kokpit.weather.WeatherState
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    private fun icons(dock: Map<DockSlot, String?>): Map<DockSlot, ImageBitmap> =
        listOf(DockSlot.CARPLAY).mapNotNull { slot ->
            val target = dock[slot] ?: return@mapNotNull null
            iconCache.getOrPut(target) { container.installedApps.icon(target)?.asImageBitmap() }?.let { slot to it }
        }.toMap()

    private data class Live(
        val weather: WeatherState,
        val calendar: CalendarState,
        val media: NowPlayingState,
        val trip: TripSnapshot,
        val locationMissing: Boolean,
    )

    private val clock = flow {
        while (true) {
            emit(LocalTime.now())
            delay(30_000)
        }
    }

    private val live = combine(
        container.weather.state,
        container.calendar.state,
        container.nowPlaying.state,
        container.drive.state,
        container.drive.permissionMissing,
        ::Live,
    )

    val state: StateFlow<HomeUiState> = combine(live, container.dock.targets, combine(container.themeMode, container.accent, ::Pair), container.calls.calls, clock) { l, dock, (mode, accent), calls, now ->
        val weather = (l.weather as? WeatherState.Fresh)?.weather ?: (l.weather as? WeatherState.Stale)?.weather
        HomeUiState(
            weather = l.weather,
            calendar = l.calendar,
            media = l.media,
            trip = l.trip,
            locationPermissionMissing = l.locationMissing,
            dock = dock,
            dark = resolveDark(mode, now, weather?.sunriseLocal, weather?.sunsetLocal),
            accent = accent,
            recentCalls = calls,
            dockIcons = icons(dock),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        HomeUiState(
            weather = container.weather.state.value,
            calendar = container.calendar.state.value,
            media = container.nowPlaying.state.value,
            trip = container.drive.state.value,
            locationPermissionMissing = container.drive.permissionMissing.value,
            dock = container.dock.targets.value,
            dark = true,
        ),
    )
}
