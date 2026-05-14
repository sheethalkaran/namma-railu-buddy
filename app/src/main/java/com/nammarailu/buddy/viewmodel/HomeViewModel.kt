package com.nammarailu.buddy.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.nammarailu.buddy.data.local.AppPreferences
import com.nammarailu.buddy.data.local.TranslationManager
import com.nammarailu.buddy.data.model.Station
import com.nammarailu.buddy.data.repository.FirebaseRepository
import com.nammarailu.buddy.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

data class NearbyStation(
    val station: Station,
    val distanceKm: Double
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val allStations: List<Station> = emptyList(),
    val searchResults: List<Station> = emptyList(),
    val recentStations: List<Station> = emptyList(),
    val nearestStation: Station? = null,
    val nearbyStations: List<NearbyStation> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val query: String = "",
    val error: String? = null,
    val locationError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: AppPreferences,
    private val locationClient: FusedLocationProviderClient,
    private val translator: TranslationManager          // ← injected
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        loadStations()
        observeRecents()
        observeSearch()
        requestLocation()
    }

    private fun loadStations() {
        viewModelScope.launch {
            // Fix 8: Combine stations with language so re-translation happens on language change
            prefs.language.combine(repo.getStations()) { lang, result -> Pair(lang, result) }
                .collect { (lang, result) ->
                    when (result) {
                        is Result.Loading -> _state.update { it.copy(isLoading = true) }
                        is Result.Success -> {
                            val translated = result.data.map { station ->
                                station.copy(name = translator.translate(station.name, lang))
                            }
                            _state.update { it.copy(isLoading = false, allStations = translated) }
                            val lat = _state.value.userLat
                            val lng = _state.value.userLng
                            if (lat != null && lng != null) computeNearby(lat, lng)
                        }
                        is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
        }
    }

    private fun observeRecents() {
        viewModelScope.launch {
            // Fix 8: Combine recents with language so names translate when language changes
            prefs.language.combine(prefs.recentStations) { lang, recents -> Pair(lang, recents) }
                .collect { (lang, recents) ->
                    val translated = recents.map { s ->
                        s.copy(name = translator.translate(s.name, lang))
                    }
                    _state.update { it.copy(recentStations = translated) }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _query.debounce(300).collect { q ->
                val results = if (q.isBlank()) emptyList()
                else _state.value.allStations.filter {
                    it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
                }
                _state.update { it.copy(query = q, searchResults = results) }
            }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun onStationSelected(station: Station) {
        viewModelScope.launch { prefs.addRecentStation(station) }
        _query.value = ""
        _state.update { it.copy(query = "", searchResults = emptyList()) }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        try {
            locationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        _state.update { it.copy(userLat = loc.latitude, userLng = loc.longitude, locationError = null) }
                        computeNearby(loc.latitude, loc.longitude)
                    } else {
                        _state.update { it.copy(locationError = "Location unavailable — please enable GPS") }
                    }
                }
                .addOnFailureListener { ex ->
                    _state.update { it.copy(locationError = ex.localizedMessage ?: "Location failed") }
                }
        } catch (_: SecurityException) {
            _state.update { it.copy(locationError = "Location permission not granted") }
        }
    }

    private fun computeNearby(lat: Double, lng: Double) {
        val all = _state.value.allStations
        if (all.isEmpty()) return
        val withDist = all.map { s -> NearbyStation(s, haversineKm(lat, lng, s.latitude, s.longitude)) }
            .sortedBy { it.distanceKm }
        val nearest = withDist.firstOrNull()?.station
        val nearby = withDist.filter { it.distanceKm <= 30.0 }.take(5).ifEmpty { withDist.take(3) }
        _state.update { it.copy(nearestStation = nearest, nearbyStations = nearby) }
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
