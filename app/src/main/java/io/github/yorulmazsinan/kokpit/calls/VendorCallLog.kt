package io.github.yorulmazsinan.kokpit.calls

import android.content.Context
import android.net.Uri
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Autochips units keep Bluetooth call history in their own provider, not in Android's call
 * log. The URI lives in a framework class (`com.autochips.bluetooth.BluetoothCallLog`), so it
 * is looked up at runtime; on other units this is simply null.
 */
object VendorCallLog {
    private const val CLASS_NAME = "com.autochips.bluetooth.BluetoothCallLog"
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    val uri: Uri? by lazy {
        runCatching { Class.forName(CLASS_NAME).getField("CONTENT_URI").get(null) as? Uri }.getOrNull()
    }

    fun query(context: Context, limit: Int = 30): List<CallEntry> {
        val uri = uri ?: return emptyList()
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val name = c.getColumnIndex("display_name")
                val number = c.getColumnIndex("phone_number")
                val type = c.getColumnIndex("call_type")
                val time = c.getColumnIndex("call_time")
                if (number < 0) return@use emptyList()
                // Oldest first in the provider; walk backwards for the newest calls.
                buildList {
                    if (!c.moveToLast()) return@buildList
                    do {
                        val entry = vendorCallEntry(
                            name = name.takeIf { it >= 0 }?.let(c::getString),
                            number = c.getString(number),
                            type = type.takeIf { it >= 0 }?.let(c::getString),
                            time = time.takeIf { it >= 0 }?.let(c::getString),
                        )
                        if (entry != null) add(entry)
                    } while (size < limit && c.moveToPrevious())
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** A provider row as a [CallEntry]; times look like `20261028T183005`. */
    fun vendorCallEntry(name: String?, number: String?, type: String?, time: String?, zone: ZoneId = ZoneId.systemDefault()): CallEntry? {
        if (number.isNullOrBlank()) return null
        val callType = when (type?.lowercase()) {
            "outgoing" -> CallType.OUTGOING
            "missed" -> CallType.MISSED
            else -> CallType.INCOMING
        }
        val timeMs = time?.replace("T", "")?.let { runCatching { LocalDateTime.parse(it, TIME_FORMAT) }.getOrNull() }
            ?.atZone(zone)?.toInstant()?.toEpochMilli() ?: 0L
        return CallEntry(name, number, callType, timeMs)
    }
}
