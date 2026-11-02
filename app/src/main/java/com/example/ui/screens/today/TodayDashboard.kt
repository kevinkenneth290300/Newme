package com.example.ui.screens.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DayPart
import com.example.data.repository.BadgeCatalog
import com.example.ui.components.*
import com.example.ui.components.journey.*
import com.example.ui.screens.routines.StarterPacksDialog
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(viewModel: NewMeViewModel, onNavigateToPlan: () -> Unit = {}, onNavigateToGym: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val level by viewModel.levelInfo.collectAsStateWithLifecycle()
    val goal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val tasks by viewModel.checklist.collectAsStateWithLifecycle()
    val routines by remember(date) { viewModel.routines.quests(date) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val water by viewModel.waterLog.collectAsStateWithLifecycle()
    val targets by viewModel.dailyTargets.collectAsStateWithLifecycle()
    val saving by viewModel.savingQuickLog.collectAsStateWithLifecycle()
    val modules = settings?.enabledModules?.split(',')?.toSet().orEmpty()
    val listState = rememberLazyListState()
    val heroVisible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.key == "hero" } } }
    var log by rememberSaveable { mutableStateOf<String?>(null) }
    var rough by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var packs by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberJourneyHaptic(settings?.hapticsEnabled == true)
    val canComplete = !LocalDate.parse(date).isAfter(LocalDate.now())
    val due = routines.filter { "ROUTINES" in modules }
    val todayCount = tasks.size + due.size
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("today_dashboard"), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DateNavigator(date, viewModel::selectDate) }
        item(key = "hero") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmberMascot(EmberStage.forLevel(level.level), if ((profile?.currentStreak ?: 0) == 0 && (profile?.longestStreak ?: 0) > 0) EmberExpression.REST else EmberExpression.HAPPY, 92.dp, heroVisible)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val greeting = when (DayPart.at(LocalTime.now().hour)) {
                        DayPart.MORNING -> R.string.greeting_morning; DayPart.MIDDAY -> R.string.greeting_midday
                        DayPart.EVENING -> R.string.greeting_evening; DayPart.NIGHT -> R.string.greeting_night
                    }
                    Text(stringResource(greeting, profile?.name.orEmpty()), style = MaterialTheme.typography.titleLarge)
                    StreakFlame(profile?.currentStreak ?: 0)
                    LevelBar(level)
                    Row {
                        settings?.pinnedBadges?.split(',')?.take(3)?.forEach { id ->
                            BadgeCatalog.all.firstOrNull { it.id == id }?.let { badge -> BadgeEmblem(BadgeCatalog.all.indexOf(badge), badge.category, badge.tier, true, 1f, Modifier.size(34.dp), stringResource(badge.title)) }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_actions)) }
                    DropdownMenu(menu, { menu = false }) { DropdownMenuItem(text = { Text(stringResource(R.string.rough_day)) }, onClick = { menu = false; rough = true }) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.todays_quests), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (goal.met) R.string.companion_goal_met else if (todayCount == 0) R.string.companion_empty else R.string.companion_next), style = MaterialTheme.typography.bodySmall)
                    if (goal.exceedsQuests) Text(stringResource(R.string.goal_exceeds_quests), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (goal.available) GoalRing(goal.current, goal.target)
            }
        }
        if (todayCount == 0) item {
            EmptyState(stringResource(R.string.empty_quests), stringResource(R.string.empty_quests_hint), PosterSlot.EMPTY_ROUTINES,
                stringResource(R.string.browse_packs), { packs = true })
        }
        tasks.take(5).forEach { task -> item(key = "task:${task.id}") {
            QuestCard(task.title, stringResource(R.string.task_reward, task.xpReward), "focus", task.isCompleted,
                { haptic(); viewModel.toggleChecklistItem(task) }, enabled = canComplete)
        } }
        due.take((5 - tasks.size.coerceAtMost(5)).coerceAtLeast(0)).forEach { quest -> item(key = "routine:${quest.template.id}") {
            QuestCard(quest.template.title, stringResource(R.string.routine_reward), quest.template.iconKey, quest.completed,
                { haptic(); viewModel.routines.toggle(quest.template.id, date, !quest.completed) }, enabled = canComplete)
        } }
        item {
            Text(stringResource(R.string.quick_log), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if ("WATER" in modules) OptionTile(stringResource(R.string.log_water), Icons.Default.WaterDrop, false, { log = "water" })
                if ("SLEEP" in modules) OptionTile(stringResource(R.string.log_sleep), Icons.Default.Bedtime, false, { log = "sleep" })
                if ("MEALS" in modules) OptionTile(stringResource(R.string.log_meal), Icons.Default.Restaurant, false, { log = "meal" })
                if ("ACTIVITY" in modules || "GYM" in modules) OptionTile(stringResource(R.string.log_move), Icons.Default.DirectionsRun, false, { log = "move" })
                OptionTile(stringResource(R.string.log_mood), Icons.Default.SentimentSatisfied, false, { log = "mood" })
            }
        }
        item {
            CollapsibleSection("today_details", stringResource(R.string.details)) {
                Box(Modifier.fillMaxWidth().height(500.dp)) { LegacyTodayDetails(viewModel, onNavigateToPlan, onNavigateToGym) }
            }
        }
    }
    if (packs) StarterPacksDialog(viewModel.routines) { packs = false }
    if (rough) FallingOffMinimumViableDayDialog({ rough = false }, { reason -> viewModel.applyMinimumViableDay(reason); rough = false })
    if (log == "water") AlertDialog(onDismissRequest = { log = null }, title = { Text(stringResource(R.string.log_water)) }, text = {
        WaterBottle(water?.amountMl ?: 0, targets?.waterTargetMl ?: 2000, { viewModel.addWater(250); haptic() })
    }, confirmButton = { TextButton(onClick = { log = null }) { Text(stringResource(R.string.done)) } })
    if (log == "sleep") SleepLogSheet(saving, { log = null }) { minutes, quality -> viewModel.saveQuickSleep(minutes, quality) { log = null } }
    if (log == "meal") MealLogSheet(saving, { log = null }, onNavigateToPlan) { name, grams -> viewModel.saveQuickProtein(name, grams) { log = null } }
    if (log == "move") AlertDialog(onDismissRequest = { log = null }, title = { Text(stringResource(R.string.log_move)) }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { ActivityControls(viewModel) } },
        confirmButton = { TextButton(onClick = { log = null }) { Text(stringResource(R.string.done)) } })
    if (log == "mood") {
        var mood by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { if (!saving) log = null }, title = { Text(stringResource(R.string.mood_question)) }, text = { MoodFaces(mood, { mood = it }) },
            confirmButton = { TextButton(enabled = mood != null && !saving, onClick = { viewModel.saveQuickMood(mood!!) { log = null } }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { log = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun SleepLogSheet(saving: Boolean, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var hours by rememberSaveable { mutableFloatStateOf(7.5f) }
    var quality by rememberSaveable { mutableIntStateOf(3) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(stringResource(R.string.log_sleep)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SleepMoon((hours * 60).toInt())
            Slider(hours, { hours = it }, valueRange = 0.25f..24f, steps = 94, enabled = !saving)
            Row { (1..5).forEach { value -> IconToggleButton(quality == value, { quality = value }) { Icon(if (quality >= value) Icons.Default.Star else Icons.Default.StarBorder, stringResource(R.string.sleep_quality, value)) } } }
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = { onSave((hours * 60).toInt(), quality) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun MealLogSheet(saving: Boolean, onDismiss: () -> Unit, onPlan: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(stringResource(R.string.log_meal)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProteinIconGrid { food, protein -> name = food; grams = protein }
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.food_name)) }, enabled = !saving)
            Text(stringResource(R.string.protein_grams, grams))
            Slider(grams.toFloat(), { grams = it.toInt() }, valueRange = 1f..100f, steps = 98, enabled = !saving)
            Text(stringResource(R.string.food_estimate), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onDismiss(); onPlan() }) { Text(stringResource(R.string.open_plan)) }
        }
    }, confirmButton = { TextButton(enabled = !saving && name.isNotBlank() && grams > 0, onClick = { onSave(name, grams) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}