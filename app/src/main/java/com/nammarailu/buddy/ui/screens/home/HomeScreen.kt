package com.nammarailu.buddy.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nammarailu.buddy.R
import com.nammarailu.buddy.data.model.Station
import com.nammarailu.buddy.ui.components.*
import com.nammarailu.buddy.ui.theme.RailuColors
import com.nammarailu.buddy.viewmodel.HomeViewModel
import com.nammarailu.buddy.viewmodel.NearbyStation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToLive: (String) -> Unit,
    onNavigateToStationSelect: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    // Fix #5: request permission automatically if not granted
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.requestLocation()
    }
    // Auto-request permission when screen loads if not granted
    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            viewModel.requestLocation()
        }
    }
    // Fix #6: use local TextFieldValue so cursor stays at end of typed text
    var searchFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    // Sync clear from viewmodel (e.g. after station selected)
    LaunchedEffect(state.query) {
        if (state.query.isBlank() && searchFieldValue.text.isNotBlank()) {
            searchFieldValue = TextFieldValue("")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header with search ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(RailuColors.DeepBlueMid, RailuColors.DeepBlue)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Train, null,
                            tint = RailuColors.Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold,
                                fontSize = 20.sp, color = Color.White)
                            Text(stringResource(R.string.tagline), fontSize = 12.sp,
                                color = RailuColors.TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = searchFieldValue,
                        onValueChange = { tfv ->
                            // Always keep cursor at the position the user typed
                            searchFieldValue = tfv
                            viewModel.onQueryChange(tfv.text)
                        },
                        placeholder = { Text(stringResource(R.string.search_placeholder),
                            color = Color.White.copy(0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = RailuColors.Purple) },
                        trailingIcon = {
                            if (searchFieldValue.text.isNotBlank()) {
                                IconButton(onClick = {
                                    searchFieldValue = TextFieldValue("")
                                    viewModel.onQueryChange("")
                                }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.White.copy(0.7f))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = RailuColors.CardDark,
                            unfocusedContainerColor = RailuColors.CardDark,
                            focusedBorderColor      = RailuColors.Purple,
                            unfocusedBorderColor    = RailuColors.DividerColor,
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White
                        ),
                        shape  = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // ── Search Results ──────────────────────────────────────────────────
        if (state.searchResults.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_results)) }
            items(state.searchResults, key = { it.id }) { station ->
                StationItem(station) {
                    viewModel.onStationSelected(station)
                    onNavigateToLive(station.id)
                }
            }
        }

        // ── Quick action cards ──────────────────────────────────────────────
        item {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_actions), fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NavQuickCard(
                        icon = Icons.Default.Train,
                        label = stringResource(R.string.live_station),
                        color = RailuColors.Purple,
                        onClick = {
                            if (state.recentStations.isNotEmpty())
                                onNavigateToLive(state.recentStations.first().id)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NavQuickCard(
                        icon = Icons.Default.Campaign,
                        label = stringResource(R.string.platform_ping),
                        color = RailuColors.Warning,
                        onClick = { onNavigateToStationSelect("platform") },
                        modifier = Modifier.weight(1f)
                    )
                    NavQuickCard(
                        icon = Icons.Default.ViewWeek,
                        label = stringResource(R.string.coach_map),
                        color = RailuColors.OnTime,
                        onClick = { onNavigateToStationSelect("coach") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Nearby Stations (based on GPS) ──────────────────────────────────
        if (state.nearbyStations.isNotEmpty() && state.query.isBlank()) {
            item { SectionHeader(stringResource(R.string.nearby_stations)) }
            items(state.nearbyStations, key = { "nearby_${it.station.id}" }) { nearby ->
                NearbyStationItem(nearby) {
                    viewModel.onStationSelected(nearby.station)
                    onNavigateToLive(nearby.station.id)
                }
            }
        } else if (state.locationError != null && state.query.isBlank()) {
            item {
                LocationErrorCard(
                    message = state.locationError!!,
                    onRetry = viewModel::requestLocation
                )
            }
        }

        // ── Loading skeleton ────────────────────────────────────────────────
        if (state.isLoading && state.allStations.isEmpty()) {
            items(3) { TrainCardSkeleton() }
        }

        // ── Recent Stations ─────────────────────────────────────────────────
        if (state.recentStations.isNotEmpty() && state.query.isBlank()) {
            item { SectionHeader(stringResource(R.string.recent_stations)) }
            items(state.recentStations, key = { "recent_${it.id}" }) { station ->
                StationItem(station, icon = Icons.Default.History) {
                    viewModel.onStationSelected(station)
                    onNavigateToLive(station.id)
                }
            }
        }

        // ── All Stations ────────────────────────────────────────────────────
        if (state.allStations.isNotEmpty() && state.query.isBlank()) {
            item { SectionHeader(stringResource(R.string.coastal_stations)) }
            items(state.allStations, key = { it.id }) { station ->
                StationItem(station) {
                    viewModel.onStationSelected(station)
                    onNavigateToLive(station.id)
                }
            }
        }

        state.error?.let { err ->
            item { ErrorState(err) { viewModel.requestLocation() } }
        }
    }
}

@Composable
fun StationItem(
    station: Station,
    icon: ImageVector = Icons.Default.Place,
    onClick: () -> Unit
) {
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RailuColors.Purple.copy(alpha = 0.15f))
            ) {
                Icon(icon, null, tint = RailuColors.Purple, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(station.name, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Text("${station.code} · ${station.zone}",
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

/** Nearby station card — shows distance badge */
@Composable
fun NearbyStationItem(nearby: NearbyStation, onClick: () -> Unit) {
    val distStr = if (nearby.distanceKm < 1.0)
        "${(nearby.distanceKm * 1000).toInt()} m"
    else
        "${"%.1f".format(nearby.distanceKm)} km"

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RailuColors.OnTime.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.MyLocation, null,
                    tint = RailuColors.OnTime, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(nearby.station.name, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Text("${nearby.station.code} · ${nearby.station.zone}",
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
            }
            // Distance badge
            Surface(
                color = RailuColors.OnTime.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = distStr,
                    color = RailuColors.OnTime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

@Composable
fun LocationErrorCard(message: String, onRetry: () -> Unit) {
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOff, null,
                tint = RailuColors.Warning, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Location unavailable", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(message, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 11.sp)
            }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry), color = RailuColors.Purple, fontSize = 12.sp)
            }
        }
    }
}
