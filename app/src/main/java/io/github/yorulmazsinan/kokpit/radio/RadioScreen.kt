package io.github.yorulmazsinan.kokpit.radio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.ui.components.HeaderClock
import io.github.yorulmazsinan.kokpit.ui.components.ScreenScaffold
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitFonts

const val RADIO_PRESET_SLOTS = 5

/**
 * Kokpit's radio layout. Tuning the actual tuner needs the head unit's vendor radio
 * commands, which are not wired yet; until then the screen offers the vendor radio app.
 */
@Composable
fun RadioScreen(
    frequency: Int,
    presets: List<Int>,
    onTune: (Int) -> Unit,
    onSavePreset: () -> Unit,
    onOpenVendorRadio: () -> Unit,
    onBack: () -> Unit,
    connected: Boolean = false,
    onSeek: (Int) -> Unit = {},
) {
    ScreenScaffold(stringResource(R.string.radio_title), onBack, actions = { HeaderClock() }) {
        Column(
            Modifier.fillMaxSize().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        ) {
            if (!connected) Surface(
                onClick = onOpenVendorRadio,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("radio_notice"),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.radio_not_connected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                shape = RoundedCornerShape(Dimens.CardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Row(Modifier.fillMaxSize().padding(Dimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                    TuneButton(Icons.Outlined.ChevronLeft, R.string.radio_down, "radio_down") { onTune(FmBand.step(frequency, -1)) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                FmBand.format(frequency),
                                style = TextStyle(fontFamily = KokpitFonts.condensed, fontSize = 84.sp, lineHeight = 84.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.testTag("radio_frequency"),
                            )
                            Text(" MHz", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        TuningScale(frequency, Modifier.fillMaxWidth(0.85f).height(28.dp))
                        Row(Modifier.fillMaxWidth(0.85f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("87.5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("108.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SeekButton(Icons.Outlined.FastRewind, R.string.radio_seek_down, "radio_seek_down") { onSeek(-1) }
                            SeekButton(Icons.Outlined.FastForward, R.string.radio_seek_up, "radio_seek_up") { onSeek(+1) }
                        }
                    }
                    TuneButton(Icons.Outlined.ChevronRight, R.string.radio_up, "radio_up") { onTune(FmBand.step(frequency, +1)) }
                }
            }
            Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until RADIO_PRESET_SLOTS).forEach { i ->
                    val preset = presets.getOrNull(i)
                    PresetTile(
                        label = preset?.let(FmBand::format) ?: "—",
                        selected = preset == frequency,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) { preset?.let(onTune) }
                }
                Surface(
                    onClick = onSavePreset,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.radio_save), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TuneButton(icon: ImageVector, label: Int, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(80.dp).testTag(tag),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, stringResource(label), modifier = Modifier.size(40.dp)) }
    }
}

@Composable
private fun SeekButton(icon: ImageVector, label: Int, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.heightIn(min = 52.dp).testTag(tag),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PresetTile(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = TextStyle(fontFamily = KokpitFonts.condensed, fontSize = 22.sp),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TuningScale(frequency: Int, modifier: Modifier) {
    val tick = MaterialTheme.colorScheme.outlineVariant
    val marker = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val steps = (FmBand.MAX - FmBand.MIN) / 5
        for (i in 0..steps) {
            val x = size.width * i / steps
            val major = (FmBand.MIN + i * 5) % 50 == 0
            drawLine(tick, Offset(x, if (major) 0f else size.height * 0.45f), Offset(x, size.height * 0.75f), strokeWidth = if (major) 3f else 2f)
        }
        val mx = size.width * FmBand.fraction(frequency)
        drawLine(marker, Offset(mx, 0f), Offset(mx, size.height), strokeWidth = 6f)
    }
}
