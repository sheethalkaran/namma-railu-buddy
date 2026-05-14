package com.nammarailu.buddy.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nammarailu.buddy.R
import com.nammarailu.buddy.ui.components.GradientCard
import com.nammarailu.buddy.ui.theme.RailuColors
import com.nammarailu.buddy.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var langExpanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "en" to "English",
        "kn" to "ಕನ್ನಡ",
        "hi" to "हिंदी"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

        // Appearance
        SettingsSection(stringResource(R.string.appearance)) {
            SettingsToggleRow(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.dark_mode),
                subtitle = stringResource(R.string.dark_mode_subtitle),
                checked = state.isDarkMode,
                onCheckedChange = viewModel::setDarkMode
            )
        }

        // Language
        SettingsSection(stringResource(R.string.language)) {
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, tint = RailuColors.Purple, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.app_language), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.currently, languages.find { it.first == state.language }?.second ?: "English"),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 13.sp)
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = it }
                    ) {
                        OutlinedButton(
                            onClick = { langExpanded = true },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(languages.find { it.first == state.language }?.second ?: "English")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                            languages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { viewModel.setLanguage(code); langExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notifications
        SettingsSection(stringResource(R.string.notifications)) {
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.enable_notifications),
                subtitle = stringResource(R.string.notifications_subtitle),
                checked = state.notificationsEnabled,
                onCheckedChange = viewModel::setNotifications
            )
        }

        // About
        SettingsSection(stringResource(R.string.about)) {
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Train, null, tint = RailuColors.Purple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("v1.0.0 · ${stringResource(R.string.tagline)}", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(
                        stringResource(R.string.about_desc),
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f), fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = RailuColors.Purple, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        content()
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = RailuColors.Purple, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RailuColors.Purple
                )
            )
        }
    }
}
