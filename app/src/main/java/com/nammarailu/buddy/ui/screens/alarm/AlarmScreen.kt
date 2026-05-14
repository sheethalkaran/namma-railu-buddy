package com.nammarailu.buddy.ui.screens.alarm

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nammarailu.buddy.R
import com.nammarailu.buddy.service.AlarmForegroundService
import com.nammarailu.buddy.viewmodel.AlarmEntry
import com.nammarailu.buddy.viewmodel.AlarmViewModel
import com.nammarailu.buddy.ui.components.GradientCard
import com.nammarailu.buddy.ui.theme.RailuColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AlarmScreen(viewModel: AlarmViewModel = hiltViewModel()) {
    val state   by viewModel.state.collectAsState()
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    // Check if we were launched with an "alarm triggered" extra (train at station)
    val activity = context as? Activity
    var showArrivalDialog by remember { mutableStateOf(false) }
    var arrivalTrainName  by remember { mutableStateOf("") }
    var arrivalStation    by remember { mutableStateOf("") }
    var isApproaching     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val intent = activity?.intent
        if (intent?.getBooleanExtra(AlarmForegroundService.EXTRA_SHOW_POPUP, false) == true) {
            arrivalTrainName = intent.getStringExtra(AlarmForegroundService.EXTRA_TRAIN_NAME) ?: ""
            arrivalStation   = intent.getStringExtra(AlarmForegroundService.EXTRA_STATION_NAME) ?: ""
            isApproaching    = intent.getBooleanExtra("is_approaching", false)
            showArrivalDialog = true
            // Clear the extras so it doesn't re-trigger on recomposition
            intent.removeExtra(AlarmForegroundService.EXTRA_SHOW_POPUP)
        }
    }

    // In-app pop-up: train is at station
    if (showArrivalDialog) {
        AlertDialog(
            onDismissRequest = { showArrivalDialog = false },
            icon = {
                Icon(Icons.Default.Train, contentDescription = null,
                    tint = RailuColors.Purple, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("🚂 Train at Station!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    if (isApproaching) {
                        "$arrivalTrainName is about 10 mins (approx 5km) away from $arrivalStation."
                    } else {
                        "$arrivalTrainName is now at $arrivalStation.\nPlease board immediately!"
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showArrivalDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RailuColors.Purple)
                ) { Text("Got it!", fontWeight = FontWeight.Bold) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    var stationExpanded by remember { mutableStateOf(false) }
    var trainExpanded   by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.alarm_system),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.alarm_desc),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                    fontSize = 13.sp
                )
            }

            // ── Add Alarm Card ──────────────────────────────────────────────
            item {
                GradientCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.add_alarm),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )

                        // Station dropdown
                        ExposedDropdownMenuBox(
                            expanded = stationExpanded,
                            onExpandedChange = { stationExpanded = it }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.selectedStation?.name ?: stringResource(R.string.choose_station),
                                onValueChange = {},
                                label = { Text(stringResource(R.string.select_station)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(stationExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = RailuColors.Purple,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = stationExpanded,
                                onDismissRequest = { stationExpanded = false }
                            ) {
                                state.stations.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (${s.code})") },
                                        onClick = { viewModel.selectStation(s); stationExpanded = false }
                                    )
                                }
                            }
                        }

                        // Train dropdown (only shown after station selected)
                        if (state.selectedStation != null) {
                            when {
                                state.isLoadingTrains -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = RailuColors.Purple
                                        )
                                        Text(
                                            "Loading upcoming trains…",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                        )
                                    }
                                }
                                state.loadError != null -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, null,
                                            tint = RailuColors.Delayed,
                                            modifier = Modifier.size(18.dp))
                                        Text(
                                            "Could not load trains. Tap station to retry.",
                                            fontSize = 13.sp,
                                            color = RailuColors.Delayed
                                        )
                                    }
                                }
                                else -> {
                                    ExposedDropdownMenuBox(
                                        expanded = trainExpanded,
                                        onExpandedChange = { trainExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = state.selectedTrain?.let {
                                                val arrival = state.trainsAtStation.find { i -> i.train.id == it.id }?.arrivalTime ?: ""
                                                "${it.number} · ${it.name}" + if (arrival.isNotBlank()) " ($arrival)" else ""
                                            } ?: stringResource(R.string.choose_train),
                                            onValueChange = {},
                                            label = { Text(stringResource(R.string.select_train)) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(trainExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = RailuColors.Purple,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = trainExpanded,
                                            onDismissRequest = { trainExpanded = false }
                                        ) {
                                            if (state.trainsAtStation.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "No upcoming trains",
                                                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                                        )
                                                    },
                                                    onClick = { trainExpanded = false }
                                                )
                                            } else {
                                                state.trainsAtStation.forEach { info ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text("${info.train.number} · ${info.train.name}", fontWeight = FontWeight.SemiBold)
                                                                if (info.arrivalTime.isNotBlank())
                                                                    Text("Arrives at ${info.arrivalTime}", fontSize = 12.sp, color = RailuColors.Purple)
                                                            }
                                                        },
                                                        onClick = { viewModel.selectTrain(info.train); trainExpanded = false }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Info row: 5km proximity trigger (no mention of minutes)
                        if (state.selectedTrain != null) {
                            val arrivalTime = state.trainsAtStation.find { it.train.id == state.selectedTrain!!.id }?.arrivalTime ?: ""
                            Surface(
                                color = RailuColors.Purple.copy(0.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("You'll be notified:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RailuColors.Purple)
                                    Text(
                                        "• When you're within 5 km of ${state.selectedStation?.name ?: "the station"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                                    )
                                    if (arrivalTime.isNotBlank())
                                        Text(
                                            "• At $arrivalTime — when the train is at the station",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                                        )
                                }
                            }
                        }

                        // Add Alarm Button
                        Button(
                            onClick = {
                                if (!permissionState.allPermissionsGranted) {
                                    permissionState.launchMultiplePermissionRequest()
                                } else {
                                    viewModel.addAlarm()
                                    scope.launch { snackbarHostState.showSnackbar("Alarm set!") }
                                }
                            },
                            enabled = state.selectedStation != null && state.selectedTrain != null,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RailuColors.Purple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Alarm, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.add_alarm), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // ── Active Alarms List ──────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.active_alarms),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.activeAlarms.isEmpty()) {
                item {
                    GradientCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AlarmOff, null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                    modifier = Modifier.size(36.dp))
                                Text(
                                    stringResource(R.string.no_active_alarms),
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.activeAlarms, key = { it.id }) { entry ->
                    AlarmEntryCard(entry = entry, onRemove = { viewModel.removeAlarm(entry.id) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AlarmEntryCard(entry: AlarmEntry, onRemove: () -> Unit) {
    GradientCard(modifier = Modifier.fillMaxWidth()) {
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
                Icon(Icons.Default.Train, null, tint = RailuColors.Purple, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.train.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Text(
                    "#${entry.train.number} @ ${entry.stationName}",
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    fontSize = 12.sp
                )
                if (entry.arrivalTime.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = RailuColors.Purple.copy(0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Arrives ${entry.arrivalTime}",
                            color = RailuColors.Purple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = RailuColors.Purple.copy(0.7f), modifier = Modifier.size(12.dp))
                        Text(
                            "Alert within 5 km of station",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, null, tint = RailuColors.Delayed)
            }
        }
    }
}
