package io.github.yorulmazsinan.kokpit.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import io.github.yorulmazsinan.kokpit.headunit.HeadUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Follows the phone call on the paired phone: the Bluetooth hands-free broadcast gives the
 * number, the vendor service is polled for the state (some units skip the broadcast).
 */
class CallMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val headUnit: HeadUnit,
    private val calls: CallLogRepository,
) {
    private val _call = MutableStateFlow<ActiveCall?>(null)
    val call: StateFlow<ActiveCall?> = _call.asStateFlow()
    private var lastNumber: String? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val call = intent.getParcelableExtra<android.os.Parcelable>(EXTRA_CALL) ?: return
            val state = runCatching { call.javaClass.getMethod("getState").invoke(call) as Int }.getOrNull()
            val number = runCatching { call.javaClass.getMethod("getNumber").invoke(call) as String? }.getOrNull()
            if (!number.isNullOrBlank()) lastNumber = number
            scope.launch { update(state) }
        }
    }

    fun start() {
        runCatching {
            ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION_CALL_CHANGED), ContextCompat.RECEIVER_EXPORTED)
        }
        scope.launch {
            while (true) {
                val state = withContext(Dispatchers.IO) { headUnit.phone?.callState() }
                if (state != null) update(state)
                delay(if (_call.value != null) 1_000 else 2_000)
            }
        }
    }

    fun answer() = scope.launch(Dispatchers.IO) { headUnit.phone?.answer() }

    fun hangUp() = scope.launch(Dispatchers.IO) { headUnit.phone?.hangUp() }

    private suspend fun update(state: Int?) {
        val phase = callPhaseFor(state)
        val previous = _call.value
        if (phase == null) {
            if (previous != null) {
                _call.value = null
                lastNumber = null
                calls.refresh()
            }
            return
        }
        val number = lastNumber ?: previous?.number
        val name = previous?.name?.takeIf { previous.number == number } ?: number?.let { nameFor(it) }
        val connectedAt = previous?.connectedAt ?: SystemClock.elapsedRealtime().takeIf { phase == CallPhase.ACTIVE }
        _call.value = ActiveCall(phase, number, name, connectedAt)
    }

    private suspend fun nameFor(number: String): String? = withContext(Dispatchers.IO) {
        calls.calls.value.firstOrNull { sameNumber(it.number, number) && !it.name.isNullOrBlank() }?.name
            ?: runCatching {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull()
    }

    private companion object {
        const val ACTION_CALL_CHANGED = "android.bluetooth.headsetclient.profile.action.CALL_CHANGED"
        const val EXTRA_CALL = "android.bluetooth.headsetclient.extra.CALL"
    }
}
