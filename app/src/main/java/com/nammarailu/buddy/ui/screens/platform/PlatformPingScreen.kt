package com.nammarailu.buddy.ui.screens.platform

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.nammarailu.buddy.ui.components.*
import com.nammarailu.buddy.ui.theme.RailuColors
import com.nammarailu.buddy.viewmodel.PlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformPingScreen(
    onBack: () -> Unit,
    viewModel: PlatformViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.platform_ping), fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current community confirmed platform info card
            state.currentUpdate?.let { update ->
                GradientCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformBadge(update.platform_number, Modifier.size(56.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "${stringResource(R.string.confirm_platform, update.platform_number)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${update.confirmations_count} ${stringResource(R.string.confirmed)}",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                stringResource(R.string.which_platform),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                // FIX 2: Tell user they can edit after submitting
                if (state.submitted && !state.isUpdating)
                    stringResource(R.string.tap_edit_to_change)
                else
                    stringResource(R.string.tap_platform),
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            // Platform number grid (1–10)
            // FIX 2: Grid is interactive when: not yet submitted OR currently in update mode
            val gridInteractive = !state.submitted || state.isUpdating
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(10) { i ->
                    val num      = i + 1
                    val selected = state.selectedPlatform == num
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    selected -> RailuColors.Purple
                                    !gridInteractive -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                2.dp,
                                if (selected) RailuColors.Purple else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = gridInteractive) { viewModel.selectPlatform(num) }
                    ) {
                        Text(
                            text = "$num",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = when {
                                selected         -> Color.White
                                !gridInteractive -> MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                else             -> MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // FIX 2: Show success state (with visible Edit button) OR the confirm form
            AnimatedContent(
                targetState = state.submitted && !state.isUpdating,
                label = "submit_state"
            ) { showSuccess ->
                if (showSuccess) {
                    // ── SUCCESS STATE ──────────────────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = RailuColors.OnTime,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            stringResource(R.string.thanks_points),
                            color = RailuColors.OnTime,
                            fontWeight = FontWeight.SemiBold
                        )

                        // FIX 2: Prominent Edit / Change Answer button
                        Button(
                            onClick = { viewModel.enterUpdateMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = RailuColors.Purple),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.change_answer),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // ── CONFIRM FORM (first ping OR update mode) ───────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.submitPing() },
                            enabled = state.selectedPlatform > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RailuColors.Purple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Campaign, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(
                                    R.string.confirm_platform,
                                    if (state.selectedPlatform > 0) state.selectedPlatform.toString() else ""
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // FIX 2: In update mode show "Cancel" (goes back to success view without saving)
                        if (state.isUpdating) {
                            OutlinedButton(
                                onClick = { viewModel.cancelUpdate() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.cancel), fontSize = 14.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.not_sure), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}