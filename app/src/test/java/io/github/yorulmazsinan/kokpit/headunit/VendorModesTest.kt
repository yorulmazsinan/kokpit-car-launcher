package io.github.yorulmazsinan.kokpit.headunit

import org.junit.Assert.assertEquals
import org.junit.Test

class VendorModesTest {
    @Test
    fun opensTheVendorScreenOnlyWhenTheSourceIsNotAlreadyOn() {
        assertEquals(true, needsVendorOpen(learnedMode = null, currentMode = 3))
        assertEquals(true, needsVendorOpen(learnedMode = 3, currentMode = null))
        assertEquals(true, needsVendorOpen(learnedMode = 3, currentMode = 5))
        assertEquals(false, needsVendorOpen(learnedMode = 3, currentMode = 3))
    }
}
