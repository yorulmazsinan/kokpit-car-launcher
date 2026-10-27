package io.github.yorulmazsinan.kokpit.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.home.cards.ClockCard
import io.github.yorulmazsinan.kokpit.home.cards.Dock
import io.github.yorulmazsinan.kokpit.home.cards.DriveCard
import io.github.yorulmazsinan.kokpit.home.cards.NowPlayingCard
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens
import java.time.LocalDateTime

@Composable
fun HomeScreen(state: HomeUiState, now: LocalDateTime, actions: HomeActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
    ) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
            ClockCard(
                now = now,
                weather = state.weather,
                calendar = state.calendar,
                onAssistant = actions.onAssistant,
                onRequestPermissions = actions.onRequestPermissions,
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
            )
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                NowPlayingCard(
                    state = state.media,
                    onOpenPlayer = { actions.onDock(DockSlot.BLUETOOTH) },
                    onGrantAccess = actions.onGrantMediaAccess,
                    onPrevious = actions.onPrevious,
                    onPlayPause = actions.onPlayPause,
                    onNext = actions.onNext,
                    modifier = Modifier.weight(1.3f).fillMaxWidth(),
                )
                DriveCard(
                    trip = state.trip,
                    permissionMissing = state.locationPermissionMissing,
                    onRequestPermissions = actions.onRequestPermissions,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
        Dock(actions.onDock, actions.onDockLongPress)
    }
}
