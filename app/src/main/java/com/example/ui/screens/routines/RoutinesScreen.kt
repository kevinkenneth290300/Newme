package com.example.ui.screens.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.journey.*
import com.example.viewmodel.NewMeViewModel
import com.example.data.local.entity.RoutineTemplateEntity
import com.example.data.routine.RoutineSchedule
import com.example.data.routine.StarterPacks
import com.example.viewmodel.RoutinesViewModel
import com.example.ui.components.notificationConsent
import java.time.LocalDate
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesScreen(viewModel: NewMeViewModel) {
    val date = LocalDate.now().toString()
    val templates by viewModel.routines.templates.collectAsStateWithLifecycle()
    val quests by remember(date) { viewModel.routines.quests(date) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var packs by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val today = LocalDate.parse(date)
    val visible = templates.filter { routine -> when (filter) {
        0 -> RoutineSchedule.isDue(routine, today)
        1 -> (0L..6L).any { RoutineSchedule.isDue(routine, today.plusDays(it)) }
        else -> true
    } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("routines_screen"), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.nav_routines), style = MaterialTheme.typography.headlineMedium)
                    Text(pluralStringResource(R.plurals.routine_progress, quests.size, quests.count { it.completed }, quests.size), style = MaterialTheme.typography.bodySmall)
                }
                GoalRing(quests.count { it.completed }, quests.size)
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.filter_today, R.string.filter_week, R.string.filter_all).forEachIndexed { index, label ->
                    FilterChip(filter == index, { filter = index }, label = { Text(stringResource(label)) })
                }
            }
        }
        if (templates.isEmpty()) item {
            EmptyState(stringResource(R.string.empty_routines), stringResource(R.string.empty_routines_hint), PosterSlot.EMPTY_ROUTINES,
                stringResource(R.string.add_my_own), { editingId = 0 }, stringResource(R.string.browse_packs), { packs = true })
        } else {
            if (visible.isEmpty()) item { Text(stringResource(R.string.no_due_routines)) }
            val categories = listOf("MORNING", "LEAVING_HOME", "AFTER_WORK", "BEFORE_SLEEP", "WEEKLY", "MONTHLY") + visible.map { it.category }.filter { it !in setOf("MORNING", "LEAVING_HOME", "AFTER_WORK", "BEFORE_SLEEP", "WEEKLY", "MONTHLY") }.distinct()
            for (category in categories) {
                val rows = visible.filter { it.category == category }
                if (rows.isNotEmpty()) item(key = "category:$category") {
                    CollapsibleSection("routines:$category", stringResource(categoryLabel(category)), rows.size.toString(), initiallyExpanded = filter == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { routine ->
                                val due = RoutineSchedule.isDue(routine, today)
                                val completed = quests.firstOrNull { it.template.id == routine.id }?.completed == true
                                val next = RoutineSchedule.next(routine, today)
                                QuestCard(routine.title, if (due) stringResource(R.string.routine_reward) else stringResource(R.string.next_due, next?.toString() ?: ""),
                                    routine.iconKey, completed, { viewModel.routines.toggle(routine.id, date, !completed) }, enabled = due)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { viewModel.routines.move(routine.id, -1) }) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.move_up)) }
                                    IconButton(onClick = { viewModel.routines.move(routine.id, 1) }) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.move_down)) }
                                    IconButton(onClick = { editingId = routine.id }) { Icon(Icons.Default.Edit, stringResource(R.string.edit_routine)) }
                                    IconButton(onClick = { deletingId = routine.id }) { Icon(Icons.Default.Delete, stringResource(R.string.delete_routine)) }
                                }
                            }
                        }
                    }
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editingId = 0 }) { Icon(Icons.Default.Add, null); Text(stringResource(R.string.add_routine)) }
                TextButton(onClick = { packs = true }) { Text(stringResource(R.string.browse_packs)) }
            } }
        }
    }
    if (packs) StarterPacksDialog(viewModel.routines) { packs = false }
    editingId?.let { id ->
        val routine = templates.firstOrNull { it.id == id }
        if (id == 0L || routine != null) key(id) { RoutineEditor(viewModel, routine) { editingId = null } }
    }
    deletingId?.let { id -> AlertDialog(onDismissRequest = { deletingId = null }, title = { Text(stringResource(R.string.delete_routine)) },
        text = { Text(stringResource(R.string.delete_routine_hint)) }, confirmButton = { TextButton(onClick = { viewModel.routines.delete(id); deletingId = null }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) } }) }
}

fun categoryLabel(category: String): Int = when (category) {
    "MORNING" -> R.string.category_morning; "LEAVING_HOME" -> R.string.category_leaving; "AFTER_WORK" -> R.string.category_after_work
    "BEFORE_SLEEP" -> R.string.category_sleep; "WEEKLY" -> R.string.category_weekly; "MONTHLY" -> R.string.category_monthly
    else -> R.string.category_custom
}

@Composable
fun StarterPacksDialog(model: RoutinesViewModel, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.browse_packs)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StarterPacks.catalog.forEach { pack ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(pack.id in selected.split(','), { checked -> selected = (selected.split(',').filter { it.isNotBlank() && it != pack.id } + if (checked) listOf(pack.id) else emptyList()).joinToString(",") })
                    Text(stringResource(pack.titleRes), modifier = Modifier.weight(1f))
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { model.applyPacks(selected.split(',').filter { it.isNotBlank() }.toSet(), onDismiss) }) { Text(stringResource(R.string.add_selected)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineEditor(viewModel: NewMeViewModel, existing: RoutineTemplateEntity?, onDismiss: () -> Unit) {
    val model = viewModel.routines
    val consent = notificationConsent(viewModel)
    val context = LocalContext.current
    val schedules by model.schedules.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    val schedule = schedules.firstOrNull { it.routineId == existing?.id }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: "MORNING") }
    var icon by rememberSaveable { mutableStateOf(existing?.iconKey ?: "sun") }
    var recurrence by rememberSaveable { mutableStateOf(existing?.recurrence ?: "DAILY") }
    var mask by rememberSaveable { mutableIntStateOf(existing?.daysOfWeekMask ?: 127) }
    var monthDay by rememberSaveable { mutableIntStateOf(existing?.dayOfMonth ?: 1) }
    var remind by rememberSaveable { mutableStateOf(schedule?.enabled ?: false) }
    var minute by rememberSaveable { mutableIntStateOf(schedule?.minuteOfDay ?: 540) }
    var sensitive by rememberSaveable { mutableStateOf(schedule?.timeSensitive ?: false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(stringResource(if (existing == null) R.string.add_routine else R.string.edit_routine)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, enabled = !saving, singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JourneyIcons.keys.take(10).forEach { key -> IconToggleButton(icon == key, { icon = key }) { Icon(JourneyIcons.get(key), key) } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("MORNING", "LEAVING_HOME", "AFTER_WORK", "BEFORE_SLEEP", "WEEKLY", "MONTHLY", "CUSTOM").forEach { value ->
                    FilterChip(category == value, { category = value }, label = { Text(stringResource(categoryLabel(value))) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("DAILY" to R.string.repeat_daily, "WEEKDAYS" to R.string.repeat_weekdays, "WEEKLY" to R.string.repeat_weekly, "MONTHLY" to R.string.repeat_monthly).forEach { value ->
                    FilterChip(recurrence == value.first, { recurrence = value.first; if (recurrence == "WEEKLY") mask = 1 shl (LocalDate.now().dayOfWeek.value - 1) }, label = { Text(stringResource(value.second)) })
                }
            }
            if (recurrence in setOf("WEEKDAYS", "WEEKLY")) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl (day.value - 1)
                    FilterChip(mask and bit != 0, { mask = if (recurrence == "WEEKLY") bit else mask xor bit }, label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())) })
                }
            }
            if (recurrence == "MONTHLY") Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthDay = (monthDay - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, stringResource(R.string.decrease)) }
                Text(stringResource(R.string.month_day, monthDay), Modifier.weight(1f))
                IconButton(onClick = { monthDay = (monthDay + 1).coerceAtMost(31) }) { Icon(Icons.Default.Add, stringResource(R.string.increase)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(remind, { remind = it }); Text(stringResource(R.string.reminder), Modifier.weight(1f)) }
            if (remind) {
                TextButton(onClick = { android.app.TimePickerDialog(context, { _, hour, minutes -> minute = hour * 60 + minutes }, minute / 60, minute % 60, true).show() }) { Text(java.time.LocalTime.ofSecondOfDay(minute * 60L).toString()) }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(sensitive, { sensitive = it }); Text(stringResource(R.string.time_sensitive), Modifier.weight(1f)) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = {
        val routine = (existing ?: RoutineTemplateEntity(title = title, startDate = LocalDate.now().toString())).copy(title = title.trim(), category = category, iconKey = icon,
            recurrence = recurrence, daysOfWeekMask = if (recurrence in setOf("DAILY", "MONTHLY")) 127 else mask, dayOfMonth = monthDay)
        try { RoutineSchedule.validate(routine); if (remind) consent { model.save(routine, remind, minute, sensitive, onDismiss) } else model.save(routine, remind, minute, sensitive, onDismiss) }
        catch (failure: IllegalArgumentException) { error = context.getString(R.string.invalid_routine) }
    }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}