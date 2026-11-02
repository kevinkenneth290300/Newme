package com.example.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.BadgeCatalog
import com.example.ui.components.journey.*
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(viewModel: NewMeViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val level by viewModel.levelInfo.collectAsStateWithLifecycle()
    val progress by viewModel.achievementProgress.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val summaries by viewModel.daySummaries.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableIntStateOf(-1) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val pins = settings?.pinnedBadges?.split(',').orEmpty()
    val unlocked = progress.count { it.achievement.isUnlocked }
    val visible = BadgeCatalog.all.filter { category == -1 || it.category == category }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EmberMascot(EmberStage.forLevel(level.level), EmberExpression.FOCUSED, 92.dp)
                Column(Modifier.weight(1f)) { Text(stringResource(BadgeCatalog.rank(level.level)), style = MaterialTheme.typography.headlineMedium); StreakFlame(profile?.currentStreak ?: 0); LevelBar(level) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset-- }) { Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_month)) }
                Text(month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + month.year, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(enabled = monthOffset < 0, onClick = { monthOffset++ }) { Icon(Icons.Default.ChevronRight, stringResource(R.string.next_month)) }
            }
            val startPadding = month.atDay(1).dayOfWeek.value - 1
            val cells = List(startPadding) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (column in 0..6) {
                        val date = week.getOrNull(column)
                        val met = summaries.firstOrNull { it.date == date?.toString() }?.goalMet == true
                        Surface(Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(8.dp), color = if (met) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                date?.let { Text(it.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall) }
                                if (met) Icon(Icons.Default.Check, stringResource(R.string.goal_met), Modifier.size(12.dp))
                            }
                        }
                    }
                } }
            }
        }
        item { Row { Text(stringResource(R.string.badges_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); Text(stringResource(R.string.quest_count, unlocked, BadgeCatalog.all.size)) } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-1 to R.string.filter_all, 0 to R.string.badge_consistency, 1 to R.string.badge_wellbeing, 2 to R.string.badge_movement, 3 to R.string.badge_mind, 4 to R.string.badge_milestones).forEach { option ->
                FilterChip(category == option.first, { category = option.first }, label = { Text(stringResource(option.second)) })
            }
        } }
        visible.chunked(3).forEach { row -> item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { badge ->
                val earned = progress.firstOrNull { it.achievement.id == badge.id }
                TextButton(onClick = { detail = badge.id }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgeEmblem(BadgeCatalog.all.indexOf(badge), badge.category, badge.tier, earned?.achievement?.isUnlocked == true,
                            (earned?.current ?: 0).toFloat() / badge.target, label = stringResource(badge.title))
                        Text(stringResource(badge.title), style = MaterialTheme.typography.labelSmall)
                        if (badge.id in pins) Icon(Icons.Default.PushPin, stringResource(R.string.pinned), Modifier.size(14.dp))
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        } } }
        item {
            val paired = summaries.filter { it.sleepMinutes > 0 && it.mood.isNotBlank() }
            if (paired.size >= 3) Text(pluralStringResource(R.plurals.sleep_mood_observation, paired.size, paired.size), style = MaterialTheme.typography.bodySmall)
            CollapsibleSection("measurements", stringResource(R.string.measurements)) { Box(Modifier.fillMaxWidth().height(560.dp)) { LegacyProgressDetails(viewModel) } }
        }
    }
    detail?.let { id ->
        val badge = BadgeCatalog.all.first { it.id == id }
        val earned = progress.firstOrNull { it.achievement.id == id }
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(stringResource(badge.title)) }, text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BadgeEmblem(BadgeCatalog.all.indexOf(badge), badge.category, badge.tier, earned?.achievement?.isUnlocked == true, (earned?.current ?: 0).toFloat() / badge.target)
                Text(stringResource(badge.hint))
                Text(stringResource(R.string.quest_count, earned?.current ?: 0, badge.target))
                earned?.achievement?.unlockedDate?.let { Text(stringResource(R.string.unlocked_on, it)) }
                TextButton(enabled = earned?.achievement?.isUnlocked == true && (id in pins || pins.filter { it.isNotBlank() }.size < 3), onClick = { viewModel.badges.pin(id) }) {
                    Text(stringResource(if (id in pins) R.string.unpin_badge else R.string.pin_badge))
                }
            }
        }, confirmButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.done)) } })
    }
}