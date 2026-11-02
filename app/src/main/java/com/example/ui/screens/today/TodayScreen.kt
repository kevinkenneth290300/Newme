package com.example.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.companion.ActionType
import com.example.data.companion.MoodState
import com.example.data.companion.TimeOfDayContext
import com.example.data.local.entity.ActivitySessionEntity
import com.example.data.local.entity.DailyChecklistItemEntity
import com.example.data.local.entity.EnvironmentTaskEntity
import com.example.data.local.entity.FriendConnectionEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun LegacyTodayDetails(
    viewModel: NewMeViewModel,
    onNavigateToPlan: () -> Unit = {},
    onNavigateToGym: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayDisplay by viewModel.dayDisplayString.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val levelInfo by viewModel.levelInfo.collectAsStateWithLifecycle()
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val reminderSchedules by viewModel.reminders.collectAsStateWithLifecycle()
    val disciplineScore by viewModel.disciplineScore.collectAsStateWithLifecycle()
    val totalProtein by viewModel.totalProteinLogged.collectAsStateWithLifecycle()
    val waterLog by viewModel.waterLog.collectAsStateWithLifecycle()
    val sleepLog by viewModel.sleepLog.collectAsStateWithLifecycle()
    val activitySessions by viewModel.activitySessions.collectAsStateWithLifecycle()
    val environmentTasks by viewModel.environmentTasks.collectAsStateWithLifecycle()
    val friendConnections by viewModel.friendConnections.collectAsStateWithLifecycle()
    val companionMessage by viewModel.companionMessage.collectAsStateWithLifecycle()
    val currentMood by viewModel.currentMood.collectAsStateWithLifecycle()
    val isVoiceSpeaking by viewModel.isVoiceSpeaking.collectAsStateWithLifecycle()
    val dailyReflection by viewModel.dailyReflection.collectAsStateWithLifecycle()
    val checkins by viewModel.dailyCheckins.collectAsStateWithLifecycle()
    val savingCheckin by viewModel.savingCheckin.collectAsStateWithLifecycle()
    val offlineVoiceAvailable by viewModel.offlineVoiceAvailable.collectAsStateWithLifecycle()
    val workout by viewModel.workoutSession.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val targets by viewModel.dailyTargets.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reflectionVisible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.key == "reflection" } } }
    LaunchedEffect(reflectionVisible, dailyReflection?.id, selectedDate) {
        if (reflectionVisible) dailyReflection?.let { viewModel.reflectionWasShown(it.id) }
    }

    var showBreakdownDialog by remember { mutableStateOf(false) }
    var showFallingOffDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showProteinDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var showDetailedCheckin by rememberSaveable { mutableStateOf(false) }
    var reminderTask by remember { mutableStateOf<DailyChecklistItemEntity?>(null) }
    reminderTask?.let { TaskReminderDialog(viewModel, it) { reminderTask = null } }


    val userCode = userProfile?.friendCode?.ifBlank {
        "NEWME-${userProfile?.name?.take(3)?.uppercase()?.ifBlank { "USR" } ?: "USR"}-2026"
    } ?: "NEWME-USR-2026"

    val targetProtein = targets?.takeIf { it.targetsKnown }?.proteinTargetGrams ?: 0
    val targetWaterMl = targets?.takeIf { it.targetsKnown }?.waterTargetMl ?: 0
    val currentWater = waterLog?.amountMl ?: 0
    val currentSleepMin = sleepLog?.durationMinutes ?: 0
    val sleepHours = currentSleepMin / 60
    val sleepMins = currentSleepMin % 60

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(NewMeBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Navigator
        item {
            DateNavigator(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )
        }

        // Header: Brand & Streak Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEWME",
                        color = NewMePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                    Text(
                        text = dayDisplay,
                        color = NewMeTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("${userProfile?.currentStreak ?: 0} day streak", color = NewMeTertiary, style = MaterialTheme.typography.labelMedium)
                    Text("Level ${levelInfo.level}", color = NewMeTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // =========================================================================
        // COMPANION CONVERSATION CARD (Top Priority)
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Goal", style = MaterialTheme.typography.titleMedium, color = NewMeTextPrimary)
                Text(if (dailyGoal.available) "${dailyGoal.current} / ${dailyGoal.target} ${dailyGoal.kind.unit}" else "No goal recorded for this date", color = NewMeTextSecondary)
                if (dailyGoal.available) LinearProgressIndicator(progress = { (dailyGoal.current.toFloat() / dailyGoal.target.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                if (dailyGoal.met) Text("Goal met", color = NewMeTertiary)
                else if (userProfile?.currentStreak == 0 && (userProfile?.longestStreak ?: 0) > 0) Text("A fresh start. Your earned XP stays with you.", color = NewMeTextSecondary)
                HorizontalDivider(color = NewMeBorder)
            }
        }
        item(key = "reflection") {
            DailyReflectionCard(dailyReflection, isVoiceSpeaking, viewModel::speakText, viewModel::stopSpeaking,
                { viewModel.loadTodayReflection(viewModel.reflectionPeriod, currentMood) }, { showDetailedCheckin = true })
        }
        item { ActivityControls(viewModel) }
        // =========================================================================
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("companion_conversation_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Top header: Greeting & Tone & Speaker Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = companionMessage.greeting.uppercase(),
                                color = NewMePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                            val identityEcho = companionMessage.identityEcho
                            if (identityEcho != null) {
                                Text(
                                    text = identityEcho,
                                    color = NewMeTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Voice Readout Button
                        IconButton(
                            onClick = {
                                if (isVoiceSpeaking) {
                                    viewModel.stopVoice()
                                } else {
                                    val voiceText = companionMessage.optionalVoiceText
                                        ?: "${companionMessage.greeting}. ${companionMessage.message}"
                                    viewModel.speakCompanionMessage(voiceText)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("voice_tts_button")
                        ) {
                            Icon(
                                imageVector = if (isVoiceSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isVoiceSpeaking) "Stop Voice" else "Listen to Companion",
                                tint = if (isVoiceSpeaking) NewMeCoral else NewMePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Thoughtful companion message
                    Text(
                        text = companionMessage.message,
                        color = NewMeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NewMeBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Mood Selector Chips (Direct in card)
                    Text(
                        text = "HOW ARE YOU STARTING RIGHT NOW?",
                        color = NewMeTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val moods = listOf(
                            MoodState.TIRED,
                            MoodState.OKAY,
                            MoodState.READY,
                            MoodState.UNMOTIVATED,
                            MoodState.MOTIVATED,
                            MoodState.STRESSED
                        )
                        moods.forEach { m ->
                            val isSelected = currentMood.contains(m.displayName, ignoreCase = true)
                            Surface(
                                color = if (isSelected) NewMePrimary else NewMeSurfaceDarker,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { viewModel.setMood(m.displayName) }
                                    .testTag("mood_chip_${m.displayName}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = m.emoji,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // YOUR NEXT MOVE (Intelligent priority recommendation)
        // =========================================================================
        val nextAction = companionMessage.suggestedAction
        val actionType = companionMessage.actionType
        if (nextAction != null) {
            item {
                Surface(
                    color = NewMeSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NewMePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("your_next_move_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = NewMePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "YOUR NEXT MOVE",
                                        color = NewMePrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nextAction,
                                color = NewMeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                when (actionType) {
                                    ActionType.START_WORKOUT -> onNavigateToGym()
                                    ActionType.COMPLETE_MEAL -> onNavigateToPlan()
                                    ActionType.LOG_WATER -> showWaterDialog = true
                                    ActionType.LOG_PROTEIN -> showProteinDialog = true
                                    ActionType.SLEEP_PREP -> showSleepDialog = true
                                    ActionType.RECOVERY_DAY -> showFallingOffDialog = true
                                    ActionType.CHECK_IN -> showDetailedCheckin = true
                                    else -> onNavigateToPlan()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("execute_next_move_button")
                        ) {
                            Text(
                                text = when (actionType) {
                                    ActionType.START_WORKOUT -> "START"
                                    ActionType.COMPLETE_MEAL -> "MEALS"
                                    ActionType.LOG_WATER -> "+ WATER"
                                    ActionType.LOG_PROTEIN -> "+ PROTEIN"
                                    ActionType.SLEEP_PREP -> "SLEEP"
                                    ActionType.RECOVERY_DAY -> "RECOVER"
                                    else -> "VIEW"
                                },
                                color = NewMePrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // TODAY'S MOMENTUM & QUICK METRICS
        // =========================================================================
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBreakdownDialog = true }
                    .testTag("todays_momentum_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TODAY'S MOMENTUM",
                                color = NewMeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                            Text(
                                if (disciplineScore.hasTargetHistory) "${disciplineScore.totalScore}% DISCIPLINE SCORE" else "INSUFFICIENT TARGET HISTORY",
                                color = NewMePrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Progress ring or mini pill
                        Surface(
                            color = NewMeSurfaceDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NewMeBorder)
                        ) {
                            Text(
                                text = "VIEW DETAILS",
                                color = NewMePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { disciplineScore.totalScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NewMePrimary,
                        trackColor = NewMeSurfaceDarker
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5-Pillar Metric Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Protein
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PROTEIN", color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (targetProtein > 0) "${totalProtein}/${targetProtein}g" else "${totalProtein}g logged", color = NewMeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Water
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WATER", color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (targetWaterMl > 0) "${"%.1f".format(currentWater / 1000f)}/${"%.1f".format(targetWaterMl / 1000f)}L" else "${"%.1f".format(currentWater / 1000f)}L logged", color = NewMeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Movement
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MOVEMENT", color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (activitySessions.any { it.isCompleted }) "${activitySessions.count { it.isCompleted }} LOGGED" else "NOT LOGGED", color = NewMeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Workout
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WORKOUT", color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (workout?.isCompleted == true) "LOGGED" else "NOT LOGGED", color = NewMeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Sleep
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SLEEP", color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (currentSleepMin > 0) "${sleepHours}h ${sleepMins}m" else "RECORD", color = if (currentSleepMin > 0) NewMeTextPrimary else NewMeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // QUICK ACTIONS ROW
        // =========================================================================
        item {
            Surface(
                color = NewMeSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickActionButton(
                        icon = "💧",
                        label = "+ Water",
                        onClick = { showWaterDialog = true },
                        testTag = "quick_action_water"
                    )
                    QuickActionButton(
                        icon = "🥩",
                        label = "+ Protein",
                        onClick = { showProteinDialog = true },
                        testTag = "quick_action_protein"
                    )
                    QuickActionButton(
                        icon = "🏃",
                        label = "+ Move",
                        onClick = { showActivityDialog = true },
                        testTag = "quick_action_move"
                    )
                    QuickActionButton(
                        icon = "🛌",
                        label = "Sleep",
                        onClick = { showSleepDialog = true },
                        testTag = "quick_action_sleep"
                    )
                    QuickActionButton(
                        icon = "🥗",
                        label = "Meals",
                        onClick = onNavigateToPlan,
                        testTag = "quick_action_plan"
                    )
                    QuickActionButton(
                        icon = "🏋️",
                        label = "Gym",
                        onClick = onNavigateToGym,
                        testTag = "quick_action_gym"
                    )
                }
            }
        }

        // =========================================================================
        // DAILY MOVEMENT & ACTIVITY SECTION
        // =========================================================================
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "DAILY MOVEMENT & ACTIVITY",
                                color = NewMePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (activitySessions.isNotEmpty()) NewMeTertiary.copy(alpha = 0.2f) else NewMeSurfaceDarker,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (activitySessions.isNotEmpty()) "${activitySessions.size} LOGGED" else "PENDING",
                                    color = if (activitySessions.isNotEmpty()) NewMeTertiary else NewMeTextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = { showActivityDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("add_activity_session_button")
                        ) {
                            Text("+ LOG ACTIVITY", color = NewMePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (activitySessions.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "No movement recorded today. Keep your promise — log a walk, workout, run, or yoga session to earn momentum points.",
                            color = NewMeTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activitySessions.forEach { session ->
                                Surface(
                                    color = NewMeSurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NewMeBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(session.title, color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            val details = buildString {
                                                append("${session.durationMinutes} min • ${session.intensity} intensity")
                                                if (session.distanceKm > 0f) append(" • ${session.distanceKm} km")
                                            }
                                            Text(details, color = NewMeTextSecondary, fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteActivitySession(session) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ENVIRONMENT & LIVING SPACE SECTION
        // =========================================================================

        // =========================================================================
        // ACTIVE FOCUS CHECKLIST (Adherence tracking)
        // =========================================================================
        item {
            val completedCount = checklist.count { it.isCompleted }
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ACTIVE FOCUS",
                            color = NewMePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                        Text(
                            "1-Tap adherence tracking",
                            color = NewMeTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        "$completedCount / ${checklist.size} DONE",
                        color = NewMeTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val sections = listOf(
            "MORNING" to "Morning Routine",
            "LUNCH" to "Lunch Adherence",
            "ACTIVITY" to "Activity & Movement",
            "PRE_WORKOUT" to "Pre-Workout Prep",
            "GYM" to "Gym & Movement",
            "POST_WORKOUT" to "Post-Workout Recovery",
            "DINNER" to "Dinner Execution",
            "NIGHT" to "Night & Sleep Target"
        )

        sections.forEach { (sectionKey, sectionTitle) ->
            val itemsForSection = checklist.filter { it.section == sectionKey }
            if (itemsForSection.isNotEmpty()) {
                item(key = "section_$sectionKey") {
                    ChecklistSectionHeader(title = sectionKey, subtitle = sectionTitle)
                }
                items(itemsForSection, key = { it.id }) { item ->
                    ChecklistItemCard(
                        item = item,
                        onToggle = { viewModel.toggleChecklistItem(item) },
                        onReminder = { reminderTask = item },
                        reminderEnabled = reminderSchedules.any { it.taskId == item.id && it.enabled }
                    )
                }
            }
        }

        // =========================================================================
        // ACCOUNTABILITY ALLIES
        // =========================================================================

        // =========================================================================
        // I'M FALLING OFF BUTTON
        // =========================================================================
        item {
            Button(
                onClick = { showFallingOffDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NewMeSurfaceDarker,
                    contentColor = NewMeCoral
                ),
                border = BorderStroke(1.dp, NewMeCoral.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("falling_off_button")
            ) {
                Text(
                    "⚠️ I'M FALLING OFF — MINIMUM VIABLE DAY",
                    color = NewMeCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }

    // =========================================================================
    // DIALOGS & MODALS
    // =========================================================================


    if (showDetailedCheckin) {
        DailyCheckinDialog(initialTimeOfDay = viewModel.reflectionPeriod, onDismiss = { showDetailedCheckin = false },
            initialEntry = checkins.firstOrNull { it.timeOfDay == viewModel.reflectionPeriod },
            actualCompletedPercent = disciplineScore.totalScore, isSaving = savingCheckin,
            onSaveCheckin = { period, mood, virtue, matters, unfinished, status, percent ->
                viewModel.recordDailyCheckin(period, mood, virtue, matters, unfinished, status, percent,
                    onSaved = { showDetailedCheckin = false })
            })
    }

    // Discipline Breakdown Dialog
    if (showBreakdownDialog) {
        DisciplineBreakdownDialog(
            breakdown = disciplineScore,
            onDismiss = { showBreakdownDialog = false }
        )
    }

    // Falling Off / Minimum Viable Day Dialog
    if (showFallingOffDialog) {
        FallingOffMinimumViableDayDialog(
            onDismiss = { showFallingOffDialog = false },
            onApplyMinimumViableDay = { reason ->
                viewModel.applyMinimumViableDay(reason)
                showFallingOffDialog = false
            }
        )
    }

    // Quick Water Log Dialog
    if (showWaterDialog) {
        QuickWaterDialog(
            currentAmountMl = currentWater,
            targetMl = targetWaterMl,
            onAddWater = { amount ->
                viewModel.addWater(amount)
                showWaterDialog = false
            },
            onDismiss = { showWaterDialog = false }
        )
    }

    // Quick Protein Log Dialog
    if (showProteinDialog) {
        QuickProteinDialog(
            onLogProtein = { foodName, portion, grams ->
                viewModel.quickAddProtein(foodName, portion, grams)
                showProteinDialog = false
            },
            onDismiss = { showProteinDialog = false }
        )
    }

    // Quick Sleep Dialog
    if (showSleepDialog) {
        QuickSleepDialog(
            onLogSleep = { bedtime, wakeTime, durationMin, quality ->
                viewModel.logSleep(bedtime, wakeTime, durationMin, quality)
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false }
        )
    }

    // Quick Activity Dialog
    if (showActivityDialog) {
        QuickActivityDialog(
            onLogActivity = { type, title, duration, intensity, distance, notes ->
                viewModel.logActivitySession(type, title, duration, intensity, distance, notes)
                showActivityDialog = false
            },
            onDismiss = { showActivityDialog = false }
        )
    }
}

// =========================================================================
// SUB-COMPONENTS
// =========================================================================

@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Surface(
            color = NewMeSurfaceDarker,
            shape = CircleShape,
            border = BorderStroke(1.dp, NewMeBorder),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = NewMeTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FirstMorningWelcomeDialog(
    name: String,
    onDismiss: () -> Unit,
    onComplete: (mood: String, intention: String) -> Unit,
    isSaving: Boolean = false
) {
    val dayOfWeek = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    var selectedMood by rememberSaveable { mutableStateOf("") }
    var selectedFocus by rememberSaveable { mutableStateOf("") }

    val moodOptions = listOf(
        "Tired" to "😴",
        "Okay" to "🙂",
        "Ready" to "💪",
        "Unmotivated" to "😐",
        "Motivated" to "🔥",
        "Stressed" to "😟"
    )

    val focusOptions = listOf(
        "Stay disciplined",
        "Don't skip my workout",
        "Eat according to plan",
        "Protect my sleep",
        "Don't overthink",
        "Win the next decision"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        title = {
            Column {
                Text(
                    text = "Good morning, $name.",
                    color = NewMePrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today is $dayOfWeek.",
                    color = NewMeTextMuted,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Stoic anchor quote
                Surface(
                    color = NewMeSurfaceDarker,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NewMeBorder)
                ) {
                    Text(
                        text = "“You don't control everything that happens today. You don't control other people's moods. You do control what you do next. So let's focus there.”",
                        color = NewMeTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Mood selector
                Text(
                    "How are you starting today?",
                    color = NewMeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    moodOptions.forEach { (m, emoji) ->
                        val isSel = selectedMood == m
                        Surface(
                            color = if (isSel) NewMePrimary else NewMeSurfaceDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) NewMePrimary else NewMeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { selectedMood = m }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Intention selection
                Text(
                    "Choose one intention for today:",
                    color = NewMeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    focusOptions.forEach { opt ->
                        val isSel = selectedFocus == opt
                        Surface(
                            color = if (isSel) NewMePrimary.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) NewMePrimary else NewMeBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFocus = opt }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    opt,
                                    color = if (isSel) NewMeTextPrimary else NewMeTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && selectedMood.isNotBlank() && selectedFocus.isNotBlank(),
                onClick = { onComplete(selectedMood, selectedFocus) },
                colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("begin_today_button")
            ) {
                Text(
                    if (isSaving) "Saving..." else "Begin Today",
                    color = NewMePrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    )
}

@Composable
fun FallingOffMinimumViableDayDialog(
    onDismiss: () -> Unit,
    onApplyMinimumViableDay: (reason: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("I'm exhausted") }

    val reasons = listOf(
        "I'm exhausted",
        "I'm craving junk",
        "Work is overwhelming",
        "I'm travelling",
        "I don't want to train",
        "I don't have my planned food",
        "I lost motivation",
        "I feel overwhelmed",
        "I slept badly",
        "I already messed up today"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        title = {
            Column {
                Text(
                    "⚠️ I'm Falling Off",
                    color = NewMeCoral,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Never let one bad decision become two.",
                    color = NewMeTextMuted,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Tell me what's happening:",
                        color = NewMeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(reasons) { r ->
                    val isSel = selectedReason == r
                    Surface(
                        color = if (isSel) NewMeCoral.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSel) NewMeCoral else NewMeBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                r,
                                color = if (isSel) NewMeTextPrimary else NewMeTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NewMeCoral, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = NewMeSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NewMeBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "YOUR MINIMUM VIABLE DAY:",
                                color = NewMePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1. Get your protein\n2. Drink enough water\n3. Move for 10-20 minutes\n4. Eat one proper meal\n5. Protect tonight's sleep", color = NewMeTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyMinimumViableDay(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = NewMeCoral),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("apply_minimum_viable_day_button")
            ) {
                Text(
                    "APPLY MINIMUM VIABLE DAY & PROTECT MOMENTUM",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = NewMeTextMuted)
            }
        }
    )
}

@Composable
fun ChecklistSectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = NewMePrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Text(
            text = subtitle,
            color = NewMeTextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ChecklistItemCard(
    item: DailyChecklistItemEntity,
    onToggle: () -> Unit,
    onReminder: (() -> Unit)? = null,
    reminderEnabled: Boolean = false
) {
    Surface(
        color = if (item.isCompleted) NewMeSurfaceDarker else NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (item.isCompleted) NewMeBorder else NewMeBorderStrong),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("checklist_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (item.isCompleted) NewMePrimary else Color.Transparent,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.5.dp, if (item.isCompleted) NewMePrimary else NewMeBorderStrong),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = NewMePrimaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (item.isCompleted) NewMeTextMuted else NewMeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                )
            }

            onReminder?.let { action ->
                IconButton(onClick = action, enabled = !item.isCompleted) {
                    Icon(if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = "Task reminder")
                }
            }

            Surface(
                color = if (item.isCompleted) NewMePrimary.copy(alpha = 0.15f) else NewMeSurfaceDarker,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "+${item.xpReward} XP",
                    color = if (item.isCompleted) NewMePrimary else NewMeTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
