package io.github.yorulmazsinan.kokpit.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.yorulmazsinan.kokpit.calendar.CalendarState
import io.github.yorulmazsinan.kokpit.calendar.UpcomingEvent
import io.github.yorulmazsinan.kokpit.calls.CallEntry
import io.github.yorulmazsinan.kokpit.calls.CallType
import io.github.yorulmazsinan.kokpit.dock.DockSlot
import io.github.yorulmazsinan.kokpit.drive.CompassPoint
import io.github.yorulmazsinan.kokpit.drive.TripSnapshot
import io.github.yorulmazsinan.kokpit.media.NowPlaying
import io.github.yorulmazsinan.kokpit.media.NowPlayingState
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme
import io.github.yorulmazsinan.kokpit.ui.theme.ThemeAccent
import io.github.yorulmazsinan.kokpit.weather.Weather
import io.github.yorulmazsinan.kokpit.weather.WeatherState
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the home screen at common head-unit sizes into app/build/screenshots for review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val now = LocalDateTime.of(2026, 10, 27, 15, 2)

    private fun state(dark: Boolean) = HomeUiState(
        weather = WeatherState.Fresh(
            Weather(18.4, 2, true, 21.0, 12.1, LocalTime.of(6, 58), LocalTime.of(17, 21), 0L),
        ),
        calendar = CalendarState.Next(
            UpcomingEvent("Müşteri toplantısı", now.withHour(16).withMinute(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
        ),
        media = NowPlayingState.Playing(NowPlaying("Gesi Bağları", "Barış Manço", "Spotify", null, isPlaying = true)),
        trip = TripSnapshot(speedKmh = 62, compass = CompassPoint.NW, averageKmh = 48, hasFix = true, bearingDeg = 315f),
        locationPermissionMissing = false,
        dock = DockSlot.entries.associateWith { null },
        dark = dark,
        recentCalls = listOf(
            CallEntry("Ece", "0555", CallType.MISSED, 3L),
            CallEntry("Ofis", "0212", CallType.OUTGOING, 2L),
            CallEntry(null, "0532 111 22 33", CallType.INCOMING, 1L),
        ),
    )

    private fun capture(name: String, dark: Boolean = true, accent: ThemeAccent = ThemeAccent.GAUGE_GREEN) {
        compose.setContent { KokpitTheme(dark = dark, accent = accent) { HomeScreen(state(dark), now, HomeActions()) } }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 0)
    }

    @Test @Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
    fun home1024x600() = capture("home-1024x600")

    @Test @Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
    fun home1024x600Light() = capture("home-1024x600-light", dark = false)

    @Test @Config(qualifiers = "tr-w800dp-h480dp-land-mdpi")
    fun home800x480() = capture("home-800x480")

    @Test @Config(qualifiers = "tr-w1280dp-h720dp-land-mdpi")
    fun home1280x720() = capture("home-1280x720")

    @Test @Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
    fun accentGaugeGreen() = capture("accent-gauge_green", accent = ThemeAccent.GAUGE_GREEN)

    @Test @Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
    fun accentChrome() = capture("accent-chrome", accent = ThemeAccent.CHROME)

    @Test @Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
    fun accentNightBlue() = capture("accent-night_blue", accent = ThemeAccent.NIGHT_BLUE)
}
