package com.nammarailu.buddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nammarailu.buddy.service.AlarmForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NammaRailuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (_: Exception) {}

        // Sign in anonymously so Firebase Security Rules allow reads.
        // Without this, any rule that requires auth.uid != null returns PERMISSION_DENIED.
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnFailureListener { /* silent — app still works in offline/cached mode */ }
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            val alarmChannel = NotificationChannel(
                AlarmForegroundService.ALARM_CHANNEL_ID,
                "Journey Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires when you are within 5km of your destination"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val ongoingChannel = NotificationChannel(
                AlarmForegroundService.ONGOING_CHANNEL_ID,
                "Alarm Running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while journey alarm is active in background"
                setShowBadge(false)
            }

            nm.createNotificationChannel(alarmChannel)
            nm.createNotificationChannel(ongoingChannel)
        }
    }
}
