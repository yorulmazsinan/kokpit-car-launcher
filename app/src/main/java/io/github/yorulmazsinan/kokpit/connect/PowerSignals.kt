package io.github.yorulmazsinan.kokpit.connect

/** Ignition, as reported by head-unit firmware (screen on/off is not reliable on these units). */
enum class PowerSignal { ON, OFF }

/** Broadcasts that mean ignition on/off. Autochips QuickBoot units send QB_POWERON/QB_POWEROFF. */
val POWER_ACTIONS: Map<String, PowerSignal> = mapOf(
    "autochips.intent.action.QB_POWERON" to PowerSignal.ON,
    "autochips.intent.action.QB_POWEROFF" to PowerSignal.OFF,
    "comm.power.POWER_ON" to PowerSignal.ON,
    "comm.power.POWER_OFF" to PowerSignal.OFF,
    "com.cusc.action.ACC_OFF" to PowerSignal.OFF,
)

fun powerSignalFor(action: String?): PowerSignal? = action?.let(POWER_ACTIONS::get)

/**
 * How long the unit slept, from a sleep time stored before the process was killed.
 * QuickBoot keeps `elapsedRealtime` running; a value lower than the stored one means a real reboot.
 */
fun sleptFor(storedSleepAt: Long?, nowElapsed: Long): Long? =
    storedSleepAt?.takeIf { it <= nowElapsed }?.let { nowElapsed - it }
