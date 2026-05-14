package com.nammarailu.buddy.ui.screens.coach

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nammarailu.buddy.R
import com.nammarailu.buddy.data.model.Coach
import com.nammarailu.buddy.ui.components.*
import com.nammarailu.buddy.ui.theme.RailuColors
import com.nammarailu.buddy.viewmodel.CoachViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachPositionScreen(
    onBack: () -> Unit,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.coach_position), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.isLoading -> repeat(3) { TrainCardSkeleton() }
                state.error != null -> ErrorState(state.error!!) {}
                state.coachPosition == null -> EmptyState(stringResource(R.string.no_coach_data), Icons.Default.ViewWeek)
                else -> {
                    val coaches = state.coachPosition!!.coaches.sortedBy { it.position }

                    Text(stringResource(R.string.engine_front), color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(10.dp))

                    // Horizontal scrollable coach layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        coaches.forEachIndexed { idx, coach ->
                            CoachBlock(
                                coach = coach,
                                isSelected = state.selectedCoachIndex == idx,
                                onClick = { viewModel.selectCoach(idx) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.rear), color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End))

                    Spacer(Modifier.height(24.dp))

                    // Legend
                    GradientCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.coach_legend), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            val legend = listOf(
                                "Locomotive" to RailuColors.CoachEngine,
                                "General (GS)" to RailuColors.CoachGen,
                                "Ladies Coach" to RailuColors.CoachLadies,
                                "Sleeper (SL)" to RailuColors.CoachSleeper,
                                "AC 3-Tier (3A)" to RailuColors.CoachAC3,
                                "AC 2-Tier (2A)" to RailuColors.CoachAC2,
                                "Pantry Car" to RailuColors.CoachPantry
                            )
                            legend.forEach { (name, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                    Spacer(Modifier.width(10.dp))
                                    Text(name, color = MaterialTheme.colorScheme.onSurface.copy(0.7f), fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Selected coach detail
                    if (state.selectedCoachIndex >= 0) {
                        val coach = coaches.getOrNull(state.selectedCoachIndex)
                        coach?.let {
                            Spacer(Modifier.height(16.dp))
                            GradientCard(modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                            .background(coachColor(it.type))
                                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp))
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Coach: ${it.label}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
                                        Text("Type: ${coachTypeName(it.type)}", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 13.sp)
                                        Text("Position: ${it.position + 1} from engine", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachBlock(coach: Coach, isSelected: Boolean, onClick: () -> Unit) {
    val color = coachColor(coach.type)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(60.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) color else color.copy(alpha = 0.4f))
                .border(2.dp, if (isSelected) RailuColors.Purple else Color.Transparent, RoundedCornerShape(10.dp))
        ) {
            Text(coach.label, color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(6.dp))
        Text("${coach.position + 1}", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 10.sp)
    }
}

fun coachColor(type: String): Color = when (type) {
    "ENGINE"  -> RailuColors.CoachEngine
    "GEN", "SLR" -> RailuColors.CoachGen
    "LADIES"  -> RailuColors.CoachLadies
    "SL"      -> RailuColors.CoachSleeper
    "AC3"     -> RailuColors.CoachAC3
    "AC2"     -> RailuColors.CoachAC2
    "PANTRY"  -> RailuColors.CoachPantry
    else      -> Color.Gray
}

fun coachTypeName(type: String): String = when (type) {
    "ENGINE"  -> "Locomotive"
    "GEN"     -> "General Coach"
    "SLR"     -> "Seating cum Luggage Rake"
    "LADIES"  -> "Ladies Coach"
    "SL"      -> "Sleeper Class"
    "AC3"     -> "AC 3-Tier"
    "AC2"     -> "AC 2-Tier"
    "PANTRY"  -> "Pantry Car"
    else      -> type
}
