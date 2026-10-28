package io.github.yorulmazsinan.kokpit.home.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.rotate
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
import kotlin.math.roundToInt

private const val GAUGE_MAX_KMH = 180f
private const val SWEEP = 240f
private const val START = 150f

/** Centred 0–180 km/h arc speedometer. */
@Composable
fun SpeedCard(trip: TripSnapshot, permissionMissing: Boolean, onRequestPermissions: () -> Unit, modifier: Modifier = Modifier) {
    KokpitCard(modifier) {
        if (permissionMissing) {
            Row(Modifier.fillMaxSize().clickable(onClick = onRequestPermissions), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.GpsNotFixed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.location_permission), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@KokpitCard
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SpeedGauge(if (trip.hasFix) trip.speedKmh else null, Modifier.aspectRatio(1f))
        }
    }
}

/** Heading for now: an arrow turned to the direction of travel, its name and degrees. */
@Composable
fun CompassCard(trip: TripSnapshot, modifier: Modifier = Modifier) {
    KokpitCard(modifier.testTag("compass_card")) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val bearing = trip.bearingDeg
            val compass = trip.compass
            Icon(
                Icons.Outlined.Navigation,
                null,
                tint = if (bearing != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp).rotate(bearing ?: 0f),
            )
            Spacer(Modifier.width(16.dp))
            if (compass != null && bearing != null) {
                Text(stringResource(compass.fullLabel), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text("${bearing.roundToInt() % 360}°", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("—", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SpeedGauge(speedKmh: Int?, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            val stroke = size.minDimension * 0.075f
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
                style = TextStyle(fontFamily = KokpitFonts.condensed, fontSize = 56.sp, lineHeight = 56.sp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("speed_value"),
            )
            Text(
                if (speedKmh != null) stringResource(R.string.speed_unit) else stringResource(R.string.gps_searching),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
