package io.github.yorulmazsinan.kokpit.headunit

/** Audio sources the vendor only switches to when its own screen opens. */
enum class VendorSource { RADIO, BT_MUSIC }

/**
 * The vendor app changes the audio source in its screen's start-up, so Kokpit opens that
 * screen and comes straight back. The unit's work mode seen afterwards is remembered, so a
 * source that is already playing is not opened again (no flash of the vendor screen).
 */
class VendorModes(private val common: () -> HeadUnitCommon?) {
    private val learned = mutableMapOf<VendorSource, Int>()

    fun needsOpen(source: VendorSource): Boolean = needsVendorOpen(learned[source], common()?.workMode())

    fun learn(source: VendorSource) {
        common()?.workMode()?.let { learned[source] = it }
    }
}

/** Open unless the current work mode is known to be this source already. */
fun needsVendorOpen(learnedMode: Int?, currentMode: Int?): Boolean =
    learnedMode == null || currentMode == null || learnedMode != currentMode
