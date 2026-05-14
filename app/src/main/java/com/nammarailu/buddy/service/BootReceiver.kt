package com.nammarailu.buddy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nammarailu.buddy.data.local.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val enabled = prefs.alarmEnabled.first()
            if (!enabled) return@launch

            val (stationId, trainId, alarmType) = prefs.alarmConfig.first()
            if (stationId.isBlank()) return@launch

            val (destLat, destLng) = prefs.alarmDestCoords.first()

            val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra("station_id", stationId)
                putExtra("train_id",   trainId)
                putExtra("alarm_type", alarmType)
                putExtra("lat",        destLat)
                putExtra("lng",        destLng)
            }
            try { context.startForegroundService(serviceIntent) } catch (_: Exception) {}
        }
    }
}
