package com.example.ui.screens.system

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ThemeMode
import com.example.ui.components.*
import com.example.ui.components.journey.*
import com.example.ui.screens.onboarding.ModuleChoices
import com.example.ui.screens.routines.StarterPacksDialog
import com.example.viewmodel.NewMeViewModel

@Composable
fun DashboardSetup(viewModel: NewMeViewModel) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(settings?.enabledModules.orEmpty()) }
    val original = settings?.enabledModules.orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    AlertDialog(onDismissRequest = { viewModel.configureDashboard(original) }, title = { Text(stringResource(R.string.dashboard_setup)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PosterImage(PosterSlot.WELCOME, Modifier.fillMaxWidth().height(120.dp))
            Text(stringResource(R.string.dashboard_preserves_data), style = MaterialTheme.typography.bodySmall)
            ModuleChoices(selected.split(',').filter { it.isNotBlank() }.toSet(), { selected = it.joinToString(",") })
        }
    }, confirmButton = { TextButton(onClick = { viewModel.configureDashboard(selected.split(',').filter { it.isNotBlank() }.toSet()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = { viewModel.configureDashboard(original) }) { Text(stringResource(R.string.skip)) } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemScreen(viewModel: NewMeViewModel, onRerunOnboarding: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val friends by viewModel.friendConnections.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var packs by rememberSaveable { mutableStateOf(false) }
    var reset by rememberSaveable { mutableStateOf(false) }
    BackHandler(section != null) { section = null }
    val pages = listOf("profile" to R.string.settings_profile, "dashboard" to R.string.settings_dashboard, "appearance" to R.string.settings_appearance,
        "reminders" to R.string.settings_reminders, "cycle" to R.string.settings_cycle, "activity" to R.string.settings_activity,
        "backup" to R.string.settings_backup, "privacy" to R.string.settings_privacy, "about" to R.string.settings_about, "data" to R.string.settings_data)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("settings_hub"), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (section == null) {
            item { Text(stringResource(R.string.settings_heading), style = MaterialTheme.typography.headlineMedium) }
            pages.forEachIndexed { index, page -> item {
                QuestCard(stringResource(page.second), "", listOf("focus", "sun", "moon", "calendar", "calendar", "move", "inbox", "key", "sun", "tidy")[index], false,
                    { section = page.first }, Modifier.testTag("settings_${page.first}"))
            } }
        } else {
            item { TextButton(onClick = { section = null }) { Icon(Icons.Default.ChevronLeft, null); Text(stringResource(R.string.settings_heading)) } }
            item { Text(stringResource(pages.first { it.first == section }.second), style = MaterialTheme.typography.headlineMedium) }
            when (section) {
                "profile" -> {
                    item { Text(profile?.name.orEmpty(), style = MaterialTheme.typography.titleLarge); TextButton(onClick = { editProfile = true }) { Text(stringResource(R.string.edit_targets)) }; GoalSettings(viewModel) }
                    item { TextButton(onClick = onRerunOnboarding) { Text(stringResource(R.string.recalibrate)) } }
                    if (friends.isNotEmpty()) item { FriendAccountabilityCard(friends, profile?.friendCode.orEmpty(), viewModel::addFriend, viewModel::removeFriend) }
                }
                "dashboard" -> {
                    item { ModuleChoices(settings?.enabledModules.orEmpty().split(',').filter { it.isNotBlank() }.toSet(), viewModel::configureDashboard) }
                    item { TextButton(onClick = { packs = true }) { Text(stringResource(R.string.browse_packs)) } }
                }
                "appearance" -> {
                    item { AppearanceSettings(viewModel) }
                    item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Switch(settings?.hapticsEnabled == true, viewModel::configureHaptics); Text(stringResource(R.string.haptics), Modifier.weight(1f)) } }
                    item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Switch(profile?.voiceEnabled == true, { enabled -> profile?.let { viewModel.updateProfile(it.copy(voiceEnabled = enabled)) } }); Text(stringResource(R.string.offline_voice), Modifier.weight(1f)) } }
                }
                "reminders" -> {
                    item { ReminderControls(viewModel) }
                    item {
                        val enabled = settings?.momentTypes.orEmpty().split(',').filter { it.isNotBlank() }.toSet()
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Switch(settings?.momentsEnabled == true, { viewModel.configureMoments(it, enabled) }); Text(stringResource(R.string.moments_title), Modifier.weight(1f)) }
                        listOf("sleep" to R.string.log_sleep, "mood" to R.string.log_mood, "focus" to R.string.moment_focus_question, "energy" to R.string.moment_energy_question,
                            "hydrate" to R.string.log_water, "win" to R.string.moment_win_question, "profile" to R.string.profile_questions, "comeback" to R.string.badge_comeback).forEach { type ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(type.first in enabled, { checked -> viewModel.configureMoments(settings?.momentsEnabled == true, if (checked) enabled + type.first else enabled - type.first) }); Text(stringResource(type.second), Modifier.weight(1f)) }
                        }
                    }
                }
                "cycle" -> item { CycleControls(viewModel) }
                "activity" -> item { ActivityControls(viewModel) }
                "backup" -> item { BackupControls(viewModel) }
                "privacy" -> item { Text(stringResource(R.string.privacy_detail), style = MaterialTheme.typography.bodyMedium) }
                "about" -> item { Text(stringResource(R.string.about_detail), style = MaterialTheme.typography.bodyMedium) }
                "data" -> item { Text(stringResource(R.string.reset_warning), style = MaterialTheme.typography.bodyMedium); TextButton(onClick = { reset = true }) { Text(stringResource(R.string.reset_data), color = MaterialTheme.colorScheme.error) } }
            }
        }
    }
    if (editProfile) profile?.let { EditProfileDialog(it, { changed -> viewModel.updateProfile(changed); editProfile = false }, { editProfile = false }) }
    if (packs) StarterPacksDialog(viewModel.routines) { packs = false }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text(stringResource(R.string.reset_data)) }, text = { Text(stringResource(R.string.reset_warning)) },
        confirmButton = { TextButton(onClick = { viewModel.resetAllData { reset = false; section = null } }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text(stringResource(R.string.cancel)) } })
}