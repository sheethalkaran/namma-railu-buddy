package com.nammarailu.buddy.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammarailu.buddy.data.local.AppPreferences
import com.nammarailu.buddy.data.local.TranslationManager
import com.nammarailu.buddy.data.model.LiveTrainInfo
import com.nammarailu.buddy.data.model.Station
import com.nammarailu.buddy.data.repository.FirebaseRepository
import com.nammarailu.buddy.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class LiveStationUiState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val trains: List<LiveTrainInfo> = emptyList(),
    val currentTimeStr: String = "",
    val error: String? = null
)

@HiltViewModel
class LiveStationViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: AppPreferences,
    private val translator: TranslationManager,          // ← injected
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stationId = savedStateHandle.get<String>("stationId") ?: ""
    private val _state = MutableStateFlow(LiveStationUiState())
    val state: StateFlow<LiveStationUiState> = _state.asStateFlow()

    init {
        loadData()
        startClockTicker()
    }

    private fun loadData() {
        viewModelScope.launch {
            val lang = prefs.language.first()
            repo.getStations().take(1).collect { result ->
                if (result is Result.Success) {
                    val station = result.data.find { s -> s.id == stationId }
                    val translatedStation = station?.copy(
                        name = translator.translate(station.name, lang)
                    )
                    _state.update { it.copy(station = translatedStation) }
                }
            }
        }
        viewModelScope.launch {
            val lang = prefs.language.first()
            repo.getTrainsAtStation(stationId).collect { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> {
                        // Translate every train name from Firebase and convert times to 12hr
                        val translatedTrains = result.data.map { info ->
                            info.copy(
                                train = info.train.copy(
                                    name = translator.translate(info.train.name, lang),
                                    type = translator.translate(info.train.type, lang)
                                ),
                                arrivalTime   = to12Hour(info.arrivalTime),
                                departureTime = to12Hour(info.departureTime)
                            )
                        }
                        val filtered = filterPastTrains(translatedTrains)
                        _state.update { it.copy(isLoading = false, trains = filtered) }
                    }
                    is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            while (true) {
                val cal = Calendar.getInstance()
                val timeStr = sdf12.format(cal.time)
                val allTrains = _state.value.trains
                val filtered  = filterPastTrains(allTrains)
                _state.update { it.copy(trains = filtered, currentTimeStr = timeStr) }
                delay(60_000L)
            }
        }
    }

    private fun filterPastTrains(trains: List<LiveTrainInfo>): List<LiveTrainInfo> {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
        return trains
            .filter { info ->
                if (info.arrivalTime.isBlank()) return@filter true
                try {
                    val cal = Calendar.getInstance()
                    cal.time = sdf12.parse(info.arrivalTime) ?: return@filter true
                    val trainMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                    trainMinutes >= currentMinutes - 10
                } catch (_: Exception) { true }
            }
            .sortedBy { info ->
                if (info.arrivalTime.isBlank()) Int.MAX_VALUE
                else try {
                    val cal = Calendar.getInstance()
                    cal.time = sdf12.parse(info.arrivalTime) ?: return@sortedBy Int.MAX_VALUE
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                } catch (_: Exception) { Int.MAX_VALUE }
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

    fun retry() = loadData()
}
