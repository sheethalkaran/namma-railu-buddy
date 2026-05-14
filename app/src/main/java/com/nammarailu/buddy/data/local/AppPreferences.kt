package com.nammarailu.buddy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nammarailu.buddy.data.model.Station
import com.nammarailu.buddy.viewmodel.AlarmEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("namma_railu_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val gson = Gson()

    companion object {
        val DARK_MODE        = booleanPreferencesKey("dark_mode")
        val LANGUAGE         = stringPreferencesKey("language")
        val NOTIFICATIONS    = booleanPreferencesKey("notifications")
        val RECENT_STATIONS  = stringPreferencesKey("recent_stations")
        // Legacy single-alarm keys kept for BootReceiver compatibility
        val ALARM_STATION_ID = stringPreferencesKey("alarm_station_id")
        val ALARM_TRAIN_ID   = stringPreferencesKey("alarm_train_id")
        val ALARM_TYPE       = stringPreferencesKey("alarm_type")
        val ALARM_ENABLED    = booleanPreferencesKey("alarm_enabled")
        val ALARM_DEST_LAT   = doublePreferencesKey("alarm_dest_lat")
        val ALARM_DEST_LNG   = doublePreferencesKey("alarm_dest_lng")
        // New: list of AlarmEntry as JSON
        val ALARM_ENTRIES    = stringPreferencesKey("alarm_entries")
    }

    val isDarkMode: Flow<Boolean>  = context.dataStore.data.map { it[DARK_MODE] ?: true }
    val language: Flow<String>     = context.dataStore.data.map { it[LANGUAGE]  ?: "en" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val alarmEnabled: Flow<Boolean>         = context.dataStore.data.map { it[ALARM_ENABLED]  ?: false }

    val recentStations: Flow<List<Station>> = context.dataStore.data.map { prefs ->
        val json = prefs[RECENT_STATIONS] ?: return@map emptyList()
        val type = object : TypeToken<List<Station>>() {}.type
        gson.fromJson<List<Station>>(json, type) ?: emptyList()
    }

    val alarmConfig: Flow<Triple<String, String, String>> = context.dataStore.data.map {
        Triple(
            it[ALARM_STATION_ID] ?: "",
            it[ALARM_TRAIN_ID]   ?: "",
            it[ALARM_TYPE]       ?: "TRAIN_ARRIVAL"
        )
    }

    val alarmDestCoords: Flow<Pair<Double, Double>> = context.dataStore.data.map {
        Pair(it[ALARM_DEST_LAT] ?: 0.0, it[ALARM_DEST_LNG] ?: 0.0)
    }

    val alarmEntries: Flow<List<AlarmEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[ALARM_ENTRIES] ?: return@map emptyList()
        val type = object : TypeToken<List<AlarmEntry>>() {}.type
        try { gson.fromJson<List<AlarmEntry>>(json, type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }

    suspend fun setDarkMode(v: Boolean)      = context.dataStore.edit { it[DARK_MODE]     = v }
    suspend fun setLanguage(lang: String)    = context.dataStore.edit { it[LANGUAGE]      = lang }
    suspend fun setNotifications(v: Boolean) = context.dataStore.edit { it[NOTIFICATIONS] = v }
    suspend fun setAlarmEnabled(v: Boolean)  = context.dataStore.edit { it[ALARM_ENABLED] = v }

    suspend fun addRecentStation(station: Station) {
        context.dataStore.edit { prefs ->
            val type = object : TypeToken<List<Station>>() {}.type
            val current: MutableList<Station> = try {
                gson.fromJson(prefs[RECENT_STATIONS] ?: "[]", type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
            current.removeAll { it.id == station.id }
            current.add(0, station)
            prefs[RECENT_STATIONS] = gson.toJson(current.take(5))
        }
    }

    suspend fun saveAlarmConfig(
        stationId: String, trainId: String, type: String,
        destLat: Double = 0.0, destLng: Double = 0.0
    ) {
        context.dataStore.edit {
            it[ALARM_STATION_ID] = stationId
            it[ALARM_TRAIN_ID]   = trainId
            it[ALARM_TYPE]       = type
            it[ALARM_ENABLED]    = true
            it[ALARM_DEST_LAT]   = destLat
            it[ALARM_DEST_LNG]   = destLng
        }
    }

    suspend fun addAlarmEntry(entry: AlarmEntry) {
        context.dataStore.edit { prefs ->
            val type = object : TypeToken<List<AlarmEntry>>() {}.type
            val current: MutableList<AlarmEntry> = try {
                gson.fromJson(prefs[ALARM_ENTRIES] ?: "[]", type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
            current.removeAll { it.id == entry.id }
            current.add(0, entry)
            prefs[ALARM_ENTRIES] = gson.toJson(current)
            prefs[ALARM_ENABLED] = true
        }
    }

    suspend fun removeAlarmEntry(id: String) {
        context.dataStore.edit { prefs ->
            val type = object : TypeToken<List<AlarmEntry>>() {}.type
            val current: MutableList<AlarmEntry> = try {
                gson.fromJson(prefs[ALARM_ENTRIES] ?: "[]", type) ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
            current.removeAll { it.id == id }
            prefs[ALARM_ENTRIES] = gson.toJson(current)
            if (current.isEmpty()) prefs[ALARM_ENABLED] = false
        }
    }
}
