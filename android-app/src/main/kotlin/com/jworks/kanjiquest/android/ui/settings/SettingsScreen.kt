package com.jworks.kanjiquest.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.Color
import com.jworks.kanjiquest.core.domain.model.LevelProgression
import com.jworks.kanjiquest.core.domain.model.UserLevel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDevChat: () -> Unit = {},
    onRetakeAssessment: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Audio Settings
            SettingsSection(title = "Audio", icon = Icons.Default.Info) {
                SwitchSetting(
                    title = "Sound Effects",
                    subtitle = "Button clicks, correct/incorrect feedback",
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.toggleSound() }
                )
                SwitchSetting(
                    title = "Background Music",
                    subtitle = "Ambient music during gameplay",
                    checked = uiState.musicEnabled,
                    onCheckedChange = { viewModel.toggleMusic() }
                )
                SwitchSetting(
                    title = "Auto-Play Audio",
                    subtitle = "Automatically play kanji pronunciation",
                    checked = uiState.autoPlayAudio,
                    onCheckedChange = { viewModel.toggleAutoPlayAudio() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Settings
            SettingsSection(title = "Gameplay", icon = Icons.Default.Settings) {
                ClickableSetting(
                    title = "Difficulty Level",
                    subtitle = uiState.difficulty.displayName,
                    onClick = { showDifficultyDialog = true }
                )
                ClickableSetting(
                    title = "Daily XP Goal",
                    subtitle = "${uiState.dailyGoal} XP per day",
                    onClick = { showDailyGoalDialog = true }
                )
                SwitchSetting(
                    title = "Show Hints",
                    subtitle = "Display helpful hints during games",
                    checked = uiState.showHints,
                    onCheckedChange = { viewModel.toggleShowHints() }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ClickableSetting(
                    title = "Retake Assessment",
                    subtitle = "Re-test your kanji proficiency level",
                    onClick = onRetakeAssessment
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Settings
            SettingsSection(title = "Notifications", icon = Icons.Default.Notifications) {
                SwitchSetting(
                    title = "Study Reminders",
                    subtitle = "Daily reminders to practice",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications() }
                )
                SwitchSetting(
                    title = "Vibrations",
                    subtitle = "Haptic feedback for interactions",
                    checked = uiState.vibrationsEnabled,
                    onCheckedChange = { viewModel.toggleVibrations() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Settings
            SettingsSection(title = "Appearance", icon = Icons.Default.Star) {
                ClickableSetting(
                    title = "Theme",
                    subtitle = uiState.theme.displayName,
                    onClick = { showThemeDialog = true }
                )
            }

            // Developer Section (only visible to registered developers)
            if (uiState.isDeveloper) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(title = "Developer", icon = Icons.Default.Build) {
                    ClickableSetting(
                        title = "Dev Chat",
                        subtitle = "Chat with the KanjiQuest dev agent",
                        onClick = onDevChat
                    )
                }
            }

            // Admin Section (only visible to admins)
            if (uiState.isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsSection(title = "Admin", icon = Icons.Default.Build) {
                    Text(
                        text = "View app as different user level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val levelOptions = listOf(null to "Default (Admin)", UserLevel.FREE to "Free User", UserLevel.PREMIUM to "Premium User", UserLevel.ADMIN to "Admin")
                    levelOptions.forEach { (level, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAdminOverrideLevel(level) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.adminOverrideLevel == level,
                                onClick = { viewModel.setAdminOverrideLevel(level) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (level == uiState.effectiveLevel && uiState.adminOverrideLevel == level) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(active)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Current effective level: ${uiState.effectiveLevel.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Player Level Override
                    Text(
                        text = "Player Level Override",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val overrideActive = uiState.adminPlayerLevelOverride != null
                    val displayLevel = uiState.adminPlayerLevelOverride ?: uiState.currentPlayerLevel
                    var sliderLevel by remember(displayLevel) { mutableFloatStateOf(displayLevel.toFloat()) }
                    val previewTier = LevelProgression.getTierForLevel(sliderLevel.roundToInt())
                    val previewGrades = previewTier.unlockedGrades.joinToString(", ") { "G$it" }

                    Text(
                        text = "Level ${sliderLevel.roundToInt()} — ${previewTier.nameEn} (${previewTier.nameJp})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Unlocked grades: $previewGrades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = sliderLevel,
                        onValueChange = { sliderLevel = it },
                        valueRange = 1f..45f,
                        steps = 43
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1", style = MaterialTheme.typography.bodySmall)
                        Text("45", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.setAdminPlayerLevelOverride(sliderLevel.roundToInt()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                viewModel.setAdminPlayerLevelOverride(null)
                                sliderLevel = uiState.currentPlayerLevel.toFloat()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = overrideActive
                        ) {
                            Text("Clear")
                        }
                    }
                    if (overrideActive) {
                        Text(
                            text = "Override active: Level ${uiState.adminPlayerLevelOverride}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset to Defaults",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Daily Goal Dialog
    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = uiState.dailyGoal,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { newGoal ->
                viewModel.setDailyGoal(newGoal)
                showDailyGoalDialog = false
            }
        )
    }

    // Difficulty Dialog
    if (showDifficultyDialog) {
        SelectionDialog(
            title = "Select Difficulty",
            options = DifficultyLevel.entries,
            selectedOption = uiState.difficulty,
            onDismiss = { showDifficultyDialog = false },
            onSelect = { difficulty ->
                viewModel.setDifficulty(difficulty)
                showDifficultyDialog = false
            },
            optionLabel = { it.displayName }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        SelectionDialog(
            title = "Select Theme",
            options = AppTheme.entries,
            selectedOption = uiState.theme,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                viewModel.setTheme(theme)
                showThemeDialog = false
            },
            optionLabel = { it.displayName }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings?") },
            text = { Text("This will reset all settings to their default values. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ClickableSetting(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DailyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentGoal.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily XP Goal") },
        text = {
            Column {
                Text(
                    text = "How much XP do you want to earn each day?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${sliderValue.roundToInt()} XP",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..100f,
                    steps = 17 // 10, 15, 20, 25, ... 100
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10", style = MaterialTheme.typography.bodySmall)
                    Text("100", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.roundToInt()) }) {
                Text("Set Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onSelect(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (option != options.last()) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
