package io.github.yorulmazsinan.kokpit.connect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.yorulmazsinan.kokpit.KokpitApp
import io.github.yorulmazsinan.kokpit.R
import io.github.yorulmazsinan.kokpit.intro.IntroActivity
import io.github.yorulmazsinan.kokpit.intro.pickIntroVideo
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * Head units sleep rather than power off when the ignition goes off. This service turns
 * screen off/on into sleep/wake events and runs the wake-up routine: trip bookkeeping,
 * hotspot connection and the intro video.
 */
class KokpitService : Service() {
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Screen events only keep the trip clock honest; ignition comes from POWER_ACTIONS.
            val now = SystemClock.elapsedRealtime()
            val container = (application as KokpitApp).container
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> container.drive.onSleep(now)
                Intent.ACTION_SCREEN_ON -> container.drive.onWake(now)
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onPower(intent.action)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.service_running))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, SERVICE_NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val power = IntentFilter().apply { POWER_ACTIONS.keys.forEach(::addAction) }
        ContextCompat.registerReceiver(this, powerReceiver, power, ContextCompat.RECEIVER_EXPORTED)
        // Started again after QuickBoot killed us at ignition off: that start is the wake-up.
        PowerEventLog.takeSleepAt(this)?.let { sleepAt ->
            val now = SystemClock.elapsedRealtime()
            log("restart after sleep")
            onEvent(WakeEvent.Wake(now, sleptFor(sleepAt, now)))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_POWER_ACTION)?.let(::onPower)
        if (intent?.getBooleanExtra(EXTRA_COLD_BOOT, false) == true) {
            log("boot")
            onEvent(WakeEvent.Wake(SystemClock.elapsedRealtime(), sleptMs = null))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        unregisterReceiver(powerReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onPower(action: String?) {
        val signal = powerSignalFor(action) ?: return
        val now = SystemClock.elapsedRealtime()
        log("$signal ($action)")
        when (signal) {
            PowerSignal.OFF -> {
                PowerEventLog.storeSleepAt(this, now)
                onEvent(WakeEvent.Sleep(now))
            }
            PowerSignal.ON -> {
                val slept = sleptFor(PowerEventLog.takeSleepAt(this), now)
                onEvent(WakeEvent.Wake(now, slept))
            }
        }
    }

    private fun log(event: String) {
        PowerEventLog.add(this, "${java.time.LocalDateTime.now().withNano(0)} $event")
    }

    private fun onEvent(event: WakeEvent) {
        WakeEvents.emit(event)
        val container = (application as KokpitApp).container
        when (event) {
            is WakeEvent.Sleep -> container.drive.onSleep(event.elapsedMs)
            is WakeEvent.Wake -> {
                container.drive.onWake(event.elapsedMs)
                container.scope.launch { playIntro(event.sleptMs) }
                container.wifi.reconnect(container.scope)
            }
        }
    }

    private suspend fun playIntro(sleptMs: Long?) {
        val container = (application as KokpitApp).container
        val intro = container.settings.current().intro
        val videos = container.introLibrary.list()
        val video = pickIntroVideo(intro, videos, sleptMs, Random.Default) ?: return
        container.settings.update { it.copy(intro = it.intro.copy(lastPlayedIndex = videos.indexOf(video))) }
        startActivity(IntroActivity.intent(this, video.id, intro.withSound, returnHome = true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        const val CHANNEL_ID = "kokpit"
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val EXTRA_COLD_BOOT = "cold_boot"
        private const val EXTRA_POWER_ACTION = "power_action"

        fun start(context: Context, coldBoot: Boolean = false, powerAction: String? = null) {
            val intent = Intent(context, KokpitService::class.java)
                .putExtra(EXTRA_COLD_BOOT, coldBoot)
                .putExtra(EXTRA_POWER_ACTION, powerAction)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
