package io.github.yorulmazsinan.kokpit.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PowerSignalsTest {
    @Test
    fun autochipsQuickBootMeansIgnition() {
        assertEquals(PowerSignal.ON, powerSignalFor("autochips.intent.action.QB_POWERON"))
        assertEquals(PowerSignal.OFF, powerSignalFor("autochips.intent.action.QB_POWEROFF"))
    }

    @Test
    fun otherVendorPowerBroadcasts() {
        assertEquals(PowerSignal.ON, powerSignalFor("comm.power.POWER_ON"))
        assertEquals(PowerSignal.OFF, powerSignalFor("comm.power.POWER_OFF"))
        assertEquals(PowerSignal.OFF, powerSignalFor("com.cusc.action.ACC_OFF"))
    }

    @Test
    fun screenEventsAreNotIgnition() {
        assertNull(powerSignalFor("android.intent.action.SCREEN_ON"))
        assertNull(powerSignalFor(null))
    }

    @Test
    fun sleepDurationSurvivesProcessRestart() {
        assertEquals(40 * 60_000L, sleptFor(storedSleepAt = 10 * 60_000L, nowElapsed = 50 * 60_000L))
        assertNull(sleptFor(storedSleepAt = null, nowElapsed = 1000L))
        assertNull("after a real reboot elapsed time restarts", sleptFor(storedSleepAt = 50_000L, nowElapsed = 1000L))
    }
}
