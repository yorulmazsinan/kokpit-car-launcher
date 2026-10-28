package io.github.yorulmazsinan.kokpit.radio

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "tr-w1024dp-h600dp-land-mdpi")
class RadioScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsFrequencyNoticeAndTunesUp() {
        var tuned = 0
        compose.setContent { KokpitTheme { RadioScreen(984, listOf(984, 914), onTune = { tuned = it }, onSavePreset = {}, onOpenVendorRadio = {}, onBack = {}) } }
        compose.onNodeWithTag("radio_frequency").assertTextEquals("98.4")
        compose.onNodeWithTag("radio_notice").assertExists()
        compose.onNodeWithTag("radio_up").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(985, tuned)
    }
}
