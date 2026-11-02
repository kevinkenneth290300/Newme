package com.example.ui.screens.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.components.BackupControls
import com.example.ui.components.GoalSettings
import com.example.ui.components.AppearanceSettings
import com.example.ui.components.CycleControls
import com.example.ui.components.ReminderControls
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel

@Composable
internal fun LegacySystemDetails(
    viewModel: NewMeViewModel,
    onRerunOnboarding: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val levelInfo by viewModel.levelInfo.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val profile = userProfile

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NewMeBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text("SETTINGS & ROUTINE", color = NewMePrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                Text("Personal targets and preferences", color = NewMeTextSecondary, fontSize = 12.sp)
            }
        }

        // Profile & Level Card
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (!profile?.name.isNullOrBlank()) profile!!.name else "NewMe Operative", color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(if (!profile?.goalDescription.isNullOrBlank()) profile!!.goalDescription else (profile?.primaryGoal ?: "Discipline & Daily Consistency"), color = NewMeTextSecondary, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = NewMePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NewMeBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TargetPill("CALORIES", "${profile?.calorieTarget ?: 2000}", NewMePrimary)
                        TargetPill("PROTEIN", "${profile?.proteinTargetGrams ?: 100}g", NewMeSecondary)
                        TargetPill("CARBS", "${profile?.carbsTargetGrams ?: 200}g", NewMeTertiary)
                        TargetPill("FATS", "${profile?.fatTargetGrams ?: 60}g", NewMeCoral)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TargetPill("WATER", "%.1f L".format(profile?.waterTargetLiters ?: 2.5f), NewMeSecondary)
                        TargetPill("SLEEP", "%.1f h".format(profile?.sleepTargetHours ?: 7.5f), NewMeSecondary)
                        TargetPill("ACTIVITY", profile?.primaryActivityType?.take(10) ?: "Gym", NewMePurple)
                        TargetPill("PLAN", profile?.dietaryPreference?.take(10) ?: "Veg", NewMePrimary)
                    }
                }
            }
        }

        // Personal Companion & Voice Settings Card
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PERSONAL COMPANION & VOICE",
                                color = NewMePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                            Text(
                                "Philosophy: You are not doing this alone.",
                                color = NewMeTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Identity Trait
                    Text("BECOMING IDENTITY", color = NewMeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = NewMeSurfaceDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NewMeBorder)
                    ) {
                        Text(
                            text = profile?.identityTraits?.ifBlank { "Disciplined & Grounded" } ?: "Disciplined & Grounded",
                            color = NewMeTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Companion Tone Selector
                    Text("COMPANION TONE", color = NewMeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val tones = listOf(
                        "Balanced Companion",
                        "Direct & Challenging",
                        "Calm & Grounded",
                        "Philosophical & Reflective"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tones.forEach { toneOption ->
                            val isSelected = (profile?.companionTone ?: "Balanced Companion").contains(toneOption.take(6), ignoreCase = true)
                            Surface(
                                color = if (isSelected) NewMePrimary.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (profile != null) {
                                            viewModel.updateProfile(profile.copy(companionTone = toneOption))
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        toneOption,
                                        color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = NewMeBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice companion switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Voice Readouts (TTS)", color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Plays grounded, calm voice guidance for reflections", color = NewMeTextMuted, fontSize = 10.sp)
                        }
                        Switch(
                            checked = profile?.voiceEnabled ?: false,
                            onCheckedChange = { checked ->
                                if (profile != null) {
                                    viewModel.updateProfile(profile.copy(voiceEnabled = checked))
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NewMePrimaryDark, checkedTrackColor = NewMePrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (profile != null) {
                                viewModel.updateProfile(profile.copy(hasSeenFirstMorningWelcome = false))
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Replay First Morning Welcome Alignment", fontSize = 11.sp, color = NewMePrimary)
                    }
                }
            }
        }

        // The Non-Negotiables (Section 21)
        item {
            Text("SUPPORTIVE DAILY ROUTINES", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RuleCard(
                    number = "01",
                    title = "Return To A Manageable Target",
                    description = "Choose a manageable nutrition target. An imperfect day does not erase your progress."
                )
                RuleCard(
                    number = "02",
                    title = "Rest Supports Recovery",
                    description = "Rest supports your daily routine. Your sleep target is an estimate, not a medical prescription."
                )
                RuleCard(
                    number = "03",
                    title = "Win The Next Decision",
                    description = "A missed workout or unplanned meal does not erase your progress. Choose one manageable next step."
                )
                RuleCard(
                    number = "04",
                    title = "Meal Prep Protects Willpower",
                    description = "Preparing food that suits your preferences can make a busy day easier."
                )
            }
        }

        // Gamification & XP Breakdown
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DISCIPLINE LEVEL SYSTEM", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Earn XP once for each eligible action: +30 XP for a completed workout, +25 XP for a saved tracked session, +15 XP for a water or protein target, and +20 XP for a sleep target. Your earned XP stays after missed days.",
                        color = NewMeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Level: LEVEL ${levelInfo.level}", color = NewMePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${levelInfo.currentXp} Total XP", color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Medical & Offline Architecture Disclaimer
        item {
            Surface(
                color = NewMeSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NewMeSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local Storage & Privacy", color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Records stay in app-private storage. The Room database itself is not password-encrypted. Automatic cloud backup and device transfer are disabled; this cannot remove copies made previously. No account, analytics, or automatic upload.",
                        color = NewMeTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("An update is not a backup. Uninstalling, clearing app data, losing the phone, or losing a backup password can lose your records.", color = NewMeTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Notice: NewMe is a discipline and habit tracking tool, not a medical diagnosis provider. Consult healthcare professionals before extreme dietary changes.",
                        color = NewMeTextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item { BackupControls(viewModel) }
        item { GoalSettings(viewModel) }
        item { AppearanceSettings(viewModel) }
        item { CycleControls(viewModel) }
        item { ReminderControls(viewModel) }

        // Recalibrate / Re-run Onboarding
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRerunOnboarding() }
                    .testTag("rerun_onboarding_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NewMePrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recalibrate Onboarding Protocol", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Re-run the setup flow to update metrics and recalculate targets", color = NewMeTextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NewMeTextMuted)
                }
            }
        }

        // Reset Data Action
        item {
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NewMeCoral),
                border = BorderStroke(1.dp, NewMeCoral.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reset_data_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESET ALL DATA / CLEAN SLATE", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onSave = { updated ->
                viewModel.updateProfile(updated)
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All App Data?", color = NewMeTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently clear all logs, daily checklist history, environment records, and return to an un-onboarded Day 1 state.", color = NewMeTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData(onSaved = {
                            showResetConfirmDialog = false
                            onRerunOnboarding()
                        })
                    }
                ) {
                    Text("RESET EVERYTHING", color = NewMeCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = NewMeTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TargetPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RuleCard(number: String, title: String, description: String) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(
                number,
                color = NewMePrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(description, color = NewMeTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
internal fun EditProfileDialog(
    profile: UserProfileEntity,
    onSave: (UserProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var protein by remember { mutableStateOf(profile.proteinTargetGrams.toString()) }
    var water by remember { mutableStateOf(profile.waterTargetLiters.toString()) }
    var sleep by remember { mutableStateOf(profile.sleepTargetHours.toString()) }
    var workouts by remember { mutableStateOf(profile.weeklyWorkoutsTarget.toString()) }
    var goal by remember { mutableStateOf(profile.goalDescription) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Edit Profile & Targets", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = water,
                        onValueChange = { water = it },
                        label = { Text("Water (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sleep,
                        onValueChange = { sleep = it },
                        label = { Text("Sleep (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = workouts,
                        onValueChange = { workouts = it },
                        label = { Text("Gym Days/Wk") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Primary Goal Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val p = protein.toIntOrNull() ?: 110
                        val w = water.toFloatOrNull() ?: 2.5f
                        val s = sleep.toFloatOrNull() ?: 7.5f
                        val g = workouts.toIntOrNull() ?: 4
                        onSave(
                            profile.copy(
                                name = name.trim(),
                                proteinTargetGrams = p,
                                waterTargetLiters = w,
                                sleepTargetHours = s,
                                weeklyWorkoutsTarget = g,
                                goalDescription = goal
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("SAVE TARGETS", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
