package com.nammarailu.buddy.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammarailu.buddy.data.local.AppPreferences
import com.nammarailu.buddy.data.local.TranslationManager
import com.nammarailu.buddy.data.model.LiveTrainInfo
import com.nammarailu.buddy.data.model.Station
import com.nammarailu.buddy.data.model.Train
import com.nammarailu.buddy.data.repository.FirebaseRepository
import com.nammarailu.buddy.data.repository.Result
import com.nammarailu.buddy.service.AlarmForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class AlarmType { TRAIN_ARRIVAL }

data class AlarmEntry(
    val id: String,
    val stationId: String,
    val stationName: String,        // Always stored in English in prefs
    val train: Train,               // train.name always stored in English in prefs
    val arrivalTime: String,        // Display: "hh:mm a" (12-hour for UI)
    val arrivalTime24h: String = "", // Raw: "HH:mm" (24-hour) for service time comparison
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class AlarmUiState(
    val stations: List<Station> = emptyList(),
    val trainsAtStation: List<LiveTrainInfo> = emptyList(),
    val selectedStation: Station? = null,
    val selectedTrain: Train? = null,
    val activeAlarms: List<AlarmEntry> = emptyList(),  // translated for display
    val isLoadingTrains: Boolean = false,
    val loadError: String? = null
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: AppPreferences,
    private val translator: TranslationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmUiState())
    val state: StateFlow<AlarmUiState> = _state.asStateFlow()

    init {
        // Translate station dropdown list whenever language or station list changes
        viewModelScope.launch {
            prefs.language.combine(repo.getStations()) { lang, result -> Pair(lang, result) }
                .collect { (lang, result) ->
                    if (result is Result.Success) {
                        val translated = result.data.map { s ->
                            s.copy(name = translator.translate(s.name, lang))
                        }
                        _state.update { it.copy(stations = translated) }
                    }
                }
        }

        // FIX: Combine language + alarmEntries so active alarms re-translate on language change
        viewModelScope.launch {
            prefs.language.combine(prefs.alarmEntries) { lang, entries ->
                Pair(lang, entries)
            }.collect { (lang, entries) ->
                val translatedEntries = entries.map { entry ->
                    entry.copy(
                        stationName = translator.translate(entry.stationName, lang),
                        train = entry.train.copy(
                            name = translator.translate(entry.train.name, lang)
                        )
                    )
                }
                _state.update { it.copy(activeAlarms = translatedEntries) }
            }
        }
    }

    fun selectStation(s: Station) {
        _state.update {
            it.copy(
                selectedStation = s,
                selectedTrain = null,
                trainsAtStation = emptyList(),
                isLoadingTrains = true,
                loadError = null
            )
        }
        viewModelScope.launch {
            // Filter out Loading so take(1) gets the first real Success/Error result
            repo.getTrainsAtStation(s.id)
                .filter { it !is Result.Loading }
                .take(1)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val now = Calendar.getInstance()
                            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                            val lang = prefs.language.first()

                            val upcoming = result.data
                                .filter { info ->
                                    if (info.arrivalTime.isBlank()) return@filter true
                                    try {
                                        val parts = info.arrivalTime.split(":")
                                        val trainMin = parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
                                        trainMin >= nowMinutes - 5
                                    } catch (_: Exception) { true }
                                }
                                .map { info ->
                                    info.copy(
                                        train         = info.train.copy(name = translator.translate(info.train.name, lang)),
                                        arrivalTime   = to12Hour(info.arrivalTime),
                                        departureTime = to12Hour(info.departureTime)
                                    )
                                }
                                .sortedWith(compareBy(
                                    { info -> if (info.arrivalTime.isBlank()) 1 else 0 },
                                    { info ->
                                        if (info.arrivalTime.isBlank()) Int.MAX_VALUE
                                        else try {
                                            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                                            val cal = Calendar.getInstance()
                                            val parsed = sdf.parse(info.arrivalTime)
                                            if (parsed == null) Int.MAX_VALUE
                                            else {
                                                cal.time = parsed
                                                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                                            }
                                        } catch (_: Exception) { Int.MAX_VALUE }
                                    }
                                ))
                            _state.update {
                                it.copy(
                                    trainsAtStation = upcoming,
                                    isLoadingTrains = false,
                                    loadError = null
                                )
                            }
                        }
                        is Result.Error -> _state.update {
                            it.copy(isLoadingTrains = false, loadError = result.message)
                        }
                        else -> { /* Loading already filtered out */ }
                    }
                }
        }
    }

    /** Convert "HH:mm" (24-hr) to "hh:mm am/pm". Returns blank if input is blank. */
    private fun to12Hour(time: String): String {
        if (time.isBlank()) return ""
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            sdf12.format(sdf24.parse(time) ?: return time)
        } catch (_: Exception) { time }
    }

    /** Convert "hh:mm a" (12-hr display) back to "HH:mm" (24-hr) for service. */
    private fun to24Hour(time: String): String {
        if (time.isBlank()) return ""
        return try {
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
            sdf24.format(sdf12.parse(time) ?: return "")
        } catch (_: Exception) { "" }
    }

    fun selectTrain(t: Train) = _state.update { it.copy(selectedTrain = t) }

    fun addAlarm() {
        val st      = _state.value
        val station = st.selectedStation ?: return
        val train   = st.selectedTrain   ?: return

        val liveInfo       = st.trainsAtStation.find { it.train.id == train.id }
        val displayArrival = liveInfo?.arrivalTime ?: ""
        val raw24h         = to24Hour(displayArrival)

        // Always store English names in prefs so translation works correctly on reload
        val entry = AlarmEntry(
            id             = "${train.id}_${station.id}_${System.currentTimeMillis()}",
            stationId      = station.id,
            stationName    = station.name,  // English name (from Firebase, pre-translation)
            train          = train.copy(name = train.name), // English name
            arrivalTime    = displayArrival,
            arrivalTime24h = raw24h,
            latitude       = station.latitude,
            longitude      = station.longitude
        )

        viewModelScope.launch {
            prefs.addAlarmEntry(entry)
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra("alarm_entry_id",   entry.id)
                putExtra("station_id",       station.id)
                putExtra("train_id",         train.id)
                putExtra("train_name",       train.name)
                putExtra("station_name",     station.name)
                putExtra("arrival_time",     displayArrival)
                putExtra("arrival_time_24h", raw24h)
                putExtra("lat",              station.latitude)
                putExtra("lng",              station.longitude)
            }
            context.startForegroundService(intent)
        }
        _state.update { it.copy(selectedStation = null, selectedTrain = null, trainsAtStation = emptyList()) }
    }

    fun removeAlarm(alarmId: String) {
        viewModelScope.launch {
            prefs.removeAlarmEntry(alarmId)
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra("remove_alarm_id", alarmId)
            }
            context.startService(intent)
        }
    }
}