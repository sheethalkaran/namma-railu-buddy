package com.nammarailu.buddy.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nammarailu.buddy.MainActivity
import com.nammarailu.buddy.R
import com.nammarailu.buddy.viewmodel.AlarmEntry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.nammarailu.buddy.data.local.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Unified alarm service that:
 * - Fires a "5km proximity" alert when the user is within 5 km of the station
 * - Fires an "at station" full-screen popup + persistent notification at exact arrival time
 * - Auto-removes expired alarms and shows a single "time over" notification
 */
@AndroidEntryPoint
class AlarmForegroundService : Service() {

    @Inject lateinit var prefs: AppPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track which (alarmId, eventKey) combos we've already notified
    private val fired    = mutableSetOf<String>()
    // Alarm IDs to remove (signalled by UI or time expiry)
    private val toRemove = mutableSetOf<String>()
    // Station coordinates keyed by alarm entry ID (populated from Intent extras)
    private val stationCoords = mutableMapOf<String, Pair<Double, Double>>()
    
    // Prevent the CPU from sleeping while alarms are active
    private var wakeLock: PowerManager.WakeLock? = null
    private var isLoopRunning = false

    companion object {
        const val NOTIF_ID           = 1001
        const val ONGOING_CHANNEL_ID = "ongoing_channel"
        const val ALARM_CHANNEL_ID   = "alarm_channel"

        /** 5 km proximity radius in metres */
        const val PROXIMITY_RADIUS_M = 5_000f

        const val EXTRA_SHOW_POPUP   = "alarm_triggered"
        const val EXTRA_TRAIN_NAME   = "popup_train_name"
        const val EXTRA_STATION_NAME = "popup_station_name"
    }

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NammaRailu:AlarmWakeLock")
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Collect station coords and alarm metadata from intent
        val entryId = intent?.getStringExtra("alarm_entry_id")
        val lat     = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
        val lng     = intent?.getDoubleExtra("lng", 0.0) ?: 0.0
        if (entryId != null && (lat != 0.0 || lng != 0.0)) {
            stationCoords[entryId] = Pair(lat, lng)
        }
        // Handle remove-alarm request from UI
        intent?.getStringExtra("remove_alarm_id")?.let { id -> toRemove.add(id) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildForegroundNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, buildForegroundNotification())
        }
        startWatchLoop()
        return START_STICKY
    }

    private fun startWatchLoop() {
        if (isLoopRunning) return
        isLoopRunning = true
        scope.launch {
            while (isActive) {
                try {
                    val alarms = try { prefs.alarmEntries.first() } catch (_: Exception) { emptyList() }
                    val active = alarms.filter { it.id !in toRemove }

                    if (active.isEmpty()) {
                        stopSelf()
                        return@launch
                    }

                    val now    = Calendar.getInstance()
                    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                    // Get user location for 5km check
                    val userLocation = getUserLocation()

                    for (entry in active) {
                        // ── 5km Proximity alert ──────────────────────────────────────
                        if (userLocation != null && entry.latitude != 0.0 && entry.longitude != 0.0) {
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                userLocation.latitude, userLocation.longitude,
                                entry.latitude, entry.longitude,
                                results
                            )
                            val proximityKey = "${entry.id}|proximity"
                            if (results[0] <= PROXIMITY_RADIUS_M && proximityKey !in fired) {
                                fired.add(proximityKey)
                                triggerNotification(
                                    notifId     = entry.id.hashCode() + 5,
                                    title       = "🚂 ${entry.train.name} — Approaching",
                                    body        = "You're within 5 km of ${entry.stationName}. Head to the platform!",
                                    autoCancel  = true,
                                    persistent  = false
                                )
                            }
                        }

                        // ── At-arrival and time-over logic ───────────────────────────
                        // Use raw 24h time for comparison; fall back to display time
                        val rawTime = entry.arrivalTime24h.ifBlank { entry.arrivalTime }
                        if (rawTime.isBlank()) continue

                        val trainMin = parseTimeToMinutes(rawTime) ?: continue
                        var diff     = trainMin - nowMin
                        
                        // Fix midnight rollover (e.g. now = 23:55, train = 00:15 => diff was -1420, now +20)
                        if (diff < -720) diff += 1440
                        if (diff > 720)  diff -= 1440

                        when {
                            diff in 9..11 -> {
                                // Train is approaching (~10 mins / 5km away)
                                val approachKey = "${entry.id}|approaching_10min"
                                if (approachKey !in fired) {
                                    fired.add(approachKey)
                                    triggerNotification(
                                        notifId      = entry.id.hashCode() + 6,
                                        title        = "🚂 ${entry.train.name} — Approaching",
                                        body         = "${entry.train.name} is about 10 mins (approx 5km) away from ${entry.stationName}.",
                                        autoCancel   = true,
                                        persistent   = false,
                                        showPopup    = true,
                                        trainName    = entry.train.name,
                                        stationName  = entry.stationName,
                                        isApproaching = true
                                    )
                                }
                            }
                            diff in -1..1 -> {
                                // Train is at station RIGHT NOW
                                val key = "${entry.id}|now"
                                if (key !in fired) {
                                    fired.add(key)
                                    // Cancel the proximity notifications (no longer needed)
                                    cancelNotification(entry.id.hashCode() + 5)
                                    cancelNotification(entry.id.hashCode() + 6)
                                    // Fire persistent "at station" notification + in-app popup
                                    triggerNotification(
                                        notifId      = entry.id.hashCode() + 1,
                                        title        = "🚂 ${entry.train.name} is HERE!",
                                        body         = "${entry.train.name} is now at ${entry.stationName}. Board immediately!",
                                        autoCancel   = true,
                                        persistent   = false,
                                        showPopup    = true,
                                        trainName    = entry.train.name,
                                        stationName  = entry.stationName
                                    )
                                }
                            }
                            diff < -5 -> {
                                // Train has departed — show "time over" notice, then remove alarm
                                val doneKey = "${entry.id}|done"
                                if (doneKey !in fired) {
                                    fired.add(doneKey)
                                    // Dismiss the "at station" notification
                                    cancelNotification(entry.id.hashCode() + 1)
                                    // Show single persistent "alarm over" notification
                                    triggerNotification(
                                        notifId    = entry.id.hashCode() + 99,
                                        title      = "⏰ ${entry.train.name} — Alarm Over",
                                        body       = "${entry.train.name} has departed ${entry.stationName}.",
                                        autoCancel = true,
                                        persistent = false
                                    )
                                }
                                toRemove.add(entry.id)
                                prefs.removeAlarmEntry(entry.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log error or ignore to keep loop running
                }
                delay(30_000L) // poll every 30 seconds
            }
        }
    }

    /**
     * Parse "HH:mm" (24-hour) or "hh:mm a" (12-hour) to minutes since midnight.
     */
    private fun parseTimeToMinutes(time: String): Int? {
        val t = time.trim()
        if (t.isBlank()) return null
        return try {
            if (!t.contains("AM", ignoreCase = true) && !t.contains("PM", ignoreCase = true)) {
                // 24-hour format: "HH:mm"
                val parts = t.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            } else {
                // 12-hour format: "hh:mm a"
                val cal = Calendar.getInstance()
                cal.time = SimpleDateFormat("hh:mm a", Locale.US).parse(t) ?: return null
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            }
        } catch (_: Exception) { null }
    }

    private suspend fun getUserLocation(): android.location.Location? = try {
        withTimeout(5000L) {
            LocationServices
                .getFusedLocationProviderClient(this@AlarmForegroundService)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
        }
    } catch (_: Exception) { null }

    private fun cancelNotification(notifId: Int) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    @Suppress("DEPRECATION")
    private fun triggerNotification(
        notifId: Int,
        title: String,
        body: String,
        autoCancel: Boolean,
        persistent: Boolean,
        showPopup: Boolean = false,
        trainName: String = "",
        stationName: String = "",
        isApproaching: Boolean = false
    ) {
        // Vibrate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 1000), -1)
            )
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 1000), -1))
            } else {
                v?.vibrate(longArrayOf(0, 500, 300, 500, 300, 1000), -1)
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_alarm_screen", true)
            if (showPopup) {
                putExtra(EXTRA_SHOW_POPUP,   true)
                putExtra(EXTRA_TRAIN_NAME,   trainName)
                putExtra(EXTRA_STATION_NAME, stationName)
                putExtra("is_approaching",   isApproaching)
            }
        }
        val pi = PendingIntent.getActivity(
            this, notifId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_train_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(autoCancel)
            .setOngoing(persistent)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 300, 500, 300, 1000))
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        )
    }

    private fun buildForegroundNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_train_notif)
            .setContentTitle("Namma Railu Buddy")
            .setContentText("Train alarms active — watching your trains")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
