package io.github.yorulmazsinan.kokpit.home.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.drive.TripSnapshot
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitFonts

private const val GAUGE_MAX_KMH = 180f
private const val SWEEP = 240f
private const val START = 150f

@Composable
fun DriveCard(trip: TripSnapshot, permissionMissing: Boolean, onRequestPermissions: () -> Unit, modifier: Modifier = Modifier) {
    KokpitCard(modifier) {
        if (permissionMissing) {
            Row(Modifier.fillMaxSize().clickable(onClick = onRequestPermissions), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.GpsNotFixed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.location_permission), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@KokpitCard
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SpeedGauge(if (trip.hasFix) trip.speedKmh else null, Modifier.fillMaxHeight().aspectRatio(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                if (!trip.hasFix) {
                    Icon(Icons.Outlined.GpsNotFixed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
                    Text(stringResource(R.string.gps_searching), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    trip.compass?.let { compass ->
                        Icon(Icons.Outlined.Navigation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Text(stringResource(compass.label), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/** 240° arc from 0 to 180 km/h with the number in the middle; null shows an empty dial. */
@Composable
private fun SpeedGauge(speedKmh: Int?, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = size.minDimension * 0.08f
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset((size.width - arcSize.width) / 2, (size.height - arcSize.height) / 2)
            drawArc(track, START, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val fraction = ((speedKmh ?: 0) / GAUGE_MAX_KMH).coerceIn(0f, 1f)
            if (fraction > 0f) {
                drawArc(fill, START, SWEEP * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                speedKmh?.toString() ?: "–",
                style = TextStyle(fontFamily = KokpitFonts.condensed, fontSize = 46.sp, lineHeight = 46.sp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("speed_value"),
            )
            Text(stringResource(R.string.speed_unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
