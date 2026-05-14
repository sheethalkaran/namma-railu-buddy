package com.nammarailu.buddy.ui.screens.livestation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nammarailu.buddy.R
import com.nammarailu.buddy.data.model.LiveTrainInfo
import com.nammarailu.buddy.data.model.TrainStatus
import com.nammarailu.buddy.ui.components.*
import com.nammarailu.buddy.ui.theme.RailuColors
import com.nammarailu.buddy.viewmodel.LiveStationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStationScreen(
    onNavigateToPlatform: (trainId: String, stationId: String) -> Unit,
    onNavigateToCoach: (trainId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LiveStationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.station?.name ?: stringResource(R.string.live_station),
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.station != null) {
                                Text(
                                    "${state.station!!.code} · ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                            }
                            LivePulse()
                            if (state.currentTimeStr.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.currentTimeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RailuColors.Purple
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding())) {
                items(4) { TrainCardSkeleton() }
            }
            state.error != null -> ErrorState(state.error!!) { viewModel.retry() }
            state.trains.isEmpty() -> EmptyState(
                stringResource(R.string.no_trains_found), Icons.Default.Train
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(), bottom = 16.dp
                )
            ) {
                item {
                    Text(
                        stringResource(R.string.trains_at_station, state.station?.name ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(state.trains, key = { it.train.id }) { info ->
                    TrainCard(
                        info           = info,
                        onPlatformPing = { onNavigateToPlatform(info.train.id, state.station?.id ?: "") },
                        onCoachMap     = { onNavigateToCoach(info.train.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrainCard(
    info: LiveTrainInfo,
    onPlatformPing: () -> Unit,
    onCoachMap: () -> Unit
) {
    val arrivingSoon = remember(info.arrivalTime) {
        if (info.arrivalTime.isBlank()) false
        else try {
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            val cal = Calendar.getInstance()
            cal.time = sdf12.parse(info.arrivalTime) ?: return@remember false
            val trainMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val now = Calendar.getInstance()
            val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            trainMin - nowMin in 0..15
        } catch (_: Exception) { false }
    }

    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        info.train.name, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, maxLines = 1
                    )
                    Text(
                        "#${info.train.number} · ${info.train.type}",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp
                    )
                }

                // FIX 3 & 4: Platform badge area
                // If platformUpdate exists → show PlatformBadge (e.g. "P5")
                // If no update yet        → show "?" placeholder
                // After user pings → Firebase listener updates platformUpdate in real-time
                //   so the "?" automatically becomes "P5" without any extra code here
                if (info.platformUpdate != null) {
                    PlatformBadge(info.platformUpdate.platform_number)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        stringResource(R.string.arrival_label),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                    Text(
                        info.arrivalTime.ifBlank { "--:--" },
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = RailuColors.Purple
                    )
                }
                if (arrivingSoon) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = RailuColors.Warning.copy(0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Arriving soon",
                            color = RailuColors.Warning, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))

            // FIX 3 & 4: Status row
            // When platformUpdate is null  → "No platform info yet"  (unchanged — correct)
            // When platformUpdate exists   → StatusChip (On Time / Delayed) + confirmed count
            //   This replaces the "no platform info yet" text as soon as a user submits a ping
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (info.platformUpdate != null) {
                    val update = info.platformUpdate

                    // Status chip: On Time / Delayed / Warning
                    StatusChip(update.trainStatus)

                    // Confirmed count chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ThumbUp, null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                // FIX 4: Shows e.g. "1 confirmed" after first ping,
                                //         updates to "2 confirmed", "3 confirmed", etc.
                                "${update.confirmations_count} ${stringResource(R.string.confirmed)}",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    // No ping yet for this train at this station
                    Text(
                        stringResource(R.string.no_platform_info),
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPlatformPing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RailuColors.Purple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RailuColors.Purple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.ping_platform_btn),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onCoachMap,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ViewWeek, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.coach_map), fontSize = 12.sp)
                }
            }
        }
    }
}