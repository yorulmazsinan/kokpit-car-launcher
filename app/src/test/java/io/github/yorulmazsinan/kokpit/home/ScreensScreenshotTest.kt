package io.github.yorulmazsinan.kokpit.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.yorulmazsinan.kokpit.calls.CallEntry
import io.github.yorulmazsinan.kokpit.calls.CallType
import io.github.yorulmazsinan.kokpit.media.NowPlaying
import io.github.yorulmazsinan.kokpit.media.NowPlayingState
import io.github.yorulmazsinan.kokpit.music.BtMusicScreen
import io.github.yorulmazsinan.kokpit.phone.Contact
import io.github.yorulmazsinan.kokpit.phone.PhoneContent
import io.github.yorulmazsinan.kokpit.radio.RadioScreen
import io.github.yorulmazsinan.kokpit.ui.theme.DesignScale
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "tr-w683dp-h360dp-land-hdpi")
class ScreensScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun phone() {
        val calls = listOf(
            CallEntry("Ece", "0555", CallType.MISSED, 1_798_000_000_000L),
            CallEntry("Ofis", "0212", CallType.OUTGOING, 1_797_900_000_000L),
            CallEntry(null, "0532 111 22 33", CallType.INCOMING, 1_797_800_000_000L),
            CallEntry("Annem", "0533", CallType.INCOMING, 1_797_700_000_000L),
        )
        compose.setContent { KokpitTheme { DesignScale { PhoneContent(calls, listOf(Contact("Annem", "0533", true)), onBack = {}, onDial = {}) } } }
        save("screen-phone")
    }

    @Test
    fun music() {
        val np = NowPlaying("Gesi Bağları", "Barış Manço", "Bluetooth", null, isPlaying = true, durationMs = 221_000, positionMs = 84_000)
        compose.setContent { KokpitTheme { DesignScale { BtMusicScreen(NowPlayingState.Playing(np), {}, {}, {}, {}, {}) } } }
        save("screen-music")
    }

    @Test
    fun radio() {
        compose.setContent { KokpitTheme { DesignScale { RadioScreen(984, listOf(984, 914, 953, 974, 1040), {}, {}, {}, {}) } } }
        save("screen-radio")
    }
}
