package io.github.yorulmazsinan.kokpit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens

/** A screen with the dock fixed underneath it, so the dock never moves between screens. */
@Composable
fun DockedLayout(dock: (@Composable () -> Unit)?, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (dock != null) {
            Box(Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenPadding)) { dock() }
        }
    }
}
