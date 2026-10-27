package io.github.yorulmazsinan.kokpit.home.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.yorulmazsinan.kokpit.R

@Composable
fun relativeDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) stringResource(R.string.duration_hours_minutes, h.toInt(), m.toInt())
    else stringResource(R.string.duration_minutes, m.toInt())
}
