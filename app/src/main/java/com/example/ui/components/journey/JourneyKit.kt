package com.example.ui.components.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.LevelInfo
import com.example.ui.theme.LocalJourneyColors
import com.example.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.sin

object JourneyIcons {
    val keys = listOf("sun", "bed", "water", "move", "focus", "tidy", "meal", "laundry", "inbox", "calendar", "key", "phone", "clothes", "moon", "protein", "walk", "run", "cycle", "dance", "swim")
    fun get(key: String): ImageVector = when (key) {
        "sun" -> Icons.Default.WbSunny; "bed" -> Icons.Default.Bed; "water" -> Icons.Default.WaterDrop
        "move", "walk" -> Icons.Default.DirectionsWalk; "run" -> Icons.Default.DirectionsRun
        "cycle" -> Icons.AutoMirrored.Filled.DirectionsBike; "dance" -> Icons.Default.MusicNote; "swim" -> Icons.Default.Pool
        "focus" -> Icons.Default.CenterFocusStrong; "tidy" -> Icons.Default.CleaningServices
        "meal" -> Icons.Default.Restaurant; "laundry" -> Icons.Default.LocalLaundryService
        "inbox" -> Icons.Default.Inbox; "calendar" -> Icons.Default.CalendarMonth; "key" -> Icons.Default.Key
        "phone" -> Icons.Default.PhoneAndroid; "clothes" -> Icons.Default.Checkroom; "moon" -> Icons.Default.DarkMode
        "protein" -> Icons.Default.EggAlt; else -> Icons.Default.AutoAwesome
    }
    val badges = listOf(Icons.Default.Flag, Icons.Default.WbSunny, Icons.Default.LocalFireDepartment,
        Icons.Default.Bolt, Icons.Default.Whatshot, Icons.Default.Stars, Icons.Default.MilitaryTech, Icons.Default.RestartAlt,
        Icons.Default.EggAlt, Icons.Default.WaterDrop, Icons.Default.Kitchen, Icons.Default.Bedtime,
        Icons.Default.FitnessCenter, Icons.Default.Route, Icons.Default.Sports, Icons.Default.DirectionsWalk,
        Icons.AutoMirrored.Filled.DirectionsBike, Icons.Default.SportsGymnastics, Icons.Default.CleaningServices,
        Icons.Default.Psychology, Icons.Default.DoneAll, Icons.Default.WbTwilight, Icons.Default.WorkspacePremium, Icons.Default.Spa)
}

@Composable
fun QuestCard(title: String, helper: String, icon: String, completed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val surface by animateColorAsState(if (completed) colors.primaryContainer else colors.surface, tween(if (LocalReducedMotion.current) 0 else 220), label = "quest completion")
    Surface(onClick = onToggle, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = RoundedCornerShape(20.dp), color = surface,
        border = BorderStroke(2.dp, if (completed) colors.primary else colors.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(JourneyIcons.get(icon), null, Modifier.size(32.dp), tint = if (completed) colors.primary else colors.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(helper, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Icon(if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                stringResource(if (completed) R.string.quest_completed else R.string.quest_not_completed), Modifier.size(28.dp), tint = colors.primary)
        }
    }
}

@Composable
fun OptionTile(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 82.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(20.dp), color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(2.dp, if (selected) colors.primary else colors.outlineVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = if (selected) colors.primary else colors.secondary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun StatChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun CollapsibleSection(key: String, title: String, count: String = "", initiallyExpanded: Boolean = false, content: @Composable () -> Unit) {
    var expanded by rememberSaveable(key) { mutableStateOf(initiallyExpanded) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("section_$key")) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (count.isNotBlank()) Text(count, style = MaterialTheme.typography.labelMedium)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                stringResource(if (expanded) R.string.collapse else R.string.expand))
        }
        if (expanded) content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun EmptyState(title: String, helper: String, slot: PosterSlot, action: String, onAction: () -> Unit, secondary: String? = null, onSecondary: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PosterImage(slot, Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1.5f), EmberExpression.REST)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(action) }
        secondary?.let { TextButton(onClick = onSecondary) { Text(it) } }
    }
}

@Composable
fun LevelBar(info: LevelInfo) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { info.progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.level_progress, info.level, info.currentXp, info.nextLevelTargetXp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun GoalRing(current: Int, target: Int, modifier: Modifier = Modifier) {
    val progress = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(modifier.size(68.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 5.dp.toPx()
            drawArc(track, -90f, 360f, false, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            drawArc(primary, -90f, progress * 360f, false, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.quest_count, current, target), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StreakFlame(days: Int) = StatChip(stringResource(R.string.streak_days, days), if (days > 0) Icons.Default.LocalFireDepartment else Icons.Default.Bedtime)

@Composable
fun BadgeEmblem(index: Int, category: Int, tier: Int, unlocked: Boolean, progress: Float, modifier: Modifier = Modifier, label: String = "", isNew: Boolean = false) {
    val extended = LocalJourneyColors.current
    val colors = MaterialTheme.colorScheme
    val ring = if (!unlocked) colors.outline else listOf(extended.bronze, extended.silver, extended.gold, extended.nova)[tier.coerceIn(0, 3)]
    Box(modifier.size(84.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.45f
            val sides = when (category) { 0 -> 6; 1 -> 8; 2 -> 5; 3 -> 4; else -> 10 }
            val path = Path()
            for (corner in 0 until sides) {
                val angle = -Math.PI / 2 + corner * Math.PI * 2 / sides
                val point = center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
                if (corner == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(path, if (unlocked) colors.surfaceVariant else colors.surface)
            drawPath(path, ring, style = Stroke(if (unlocked) 4.dp.toPx() else 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
            if (tier > 0 && unlocked) drawCircle(ring.copy(alpha = 0.65f), radius * 0.7f, center, style = Stroke(1.dp.toPx()))
            if (!unlocked) drawArc(colors.secondary, -90f, 360 * progress.coerceIn(0f, 1f), false, Offset(size.width * 0.18f, size.height * 0.18f), Size(size.width * 0.64f, size.height * 0.64f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Icon(JourneyIcons.badges[index.mod(JourneyIcons.badges.size)], null, Modifier.size(30.dp), tint = if (unlocked) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.7f))
        if (!unlocked) Icon(Icons.Default.Lock, stringResource(R.string.badge_locked), Modifier.align(Alignment.BottomEnd).size(18.dp), tint = colors.onSurfaceVariant)
        if (isNew) Text(stringResource(R.string.badge_new), style = MaterialTheme.typography.labelSmall, color = colors.tertiary, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun ConfettiBurst(active: Boolean, modifier: Modifier = Modifier) {
    if (!active || LocalReducedMotion.current) return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    var burst by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) { burst = 1f }
    val progress by animateFloatAsState(burst, tween(700), label = "celebration")
    Canvas(modifier.fillMaxWidth().height(100.dp).clearAndSetSemantics {}) {
        for (index in 0 until 20) {
            val angle = index * Math.PI * 2 / 20
            val center = Offset(size.width / 2 + (cos(angle) * size.width * 0.35f * progress).toFloat(), size.height / 2 + (sin(angle) * size.height * 0.42f * progress).toFloat())
            drawCircle(listOf(primary, secondary, tertiary)[index % 3], 3.dp.toPx(), center)
        }
    }
}

@Composable
fun WaterBottle(amountMl: Int, targetMl: Int, onAdd: () -> Unit, enabled: Boolean = true) {
    val description = stringResource(R.string.water_bottle_label, amountMl)
    val water = MaterialTheme.colorScheme.secondary
    val line = MaterialTheme.colorScheme.onSurface
    val ratio = (amountMl.toFloat() / targetMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val fill by animateFloatAsState(ratio, tween(if (LocalReducedMotion.current) 0 else 350), label = "water fill")
    TextButton(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).semantics { contentDescription = description }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(74.dp, 120.dp)) {
                val body = Path().apply { moveTo(size.width * 0.3f, size.height * 0.18f); lineTo(size.width * 0.3f, size.height * 0.06f); lineTo(size.width * 0.7f, size.height * 0.06f); lineTo(size.width * 0.7f, size.height * 0.18f); lineTo(size.width * 0.86f, size.height * 0.3f); lineTo(size.width * 0.86f, size.height * 0.94f); lineTo(size.width * 0.14f, size.height * 0.94f); lineTo(size.width * 0.14f, size.height * 0.3f); close() }
                clipPath(body) { drawRect(water.copy(alpha = 0.65f), Offset(0f, size.height * (0.94f - 0.8f * fill)), Size(size.width, size.height)) }
                drawPath(body, line, style = Stroke(2.dp.toPx()))
                drawLine(line, Offset(size.width * 0.28f, size.height * 0.08f), Offset(size.width * 0.72f, size.height * 0.08f), 4.dp.toPx())
            }
            Text(stringResource(R.string.add_glass), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SleepMoon(minutes: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.sleep_hours, minutes / 60, minutes % 60)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(72.dp).semantics { contentDescription = label }) {
            drawCircle(colors.tertiary, size.minDimension * 0.38f, center)
            drawCircle(colors.surface, size.minDimension * 0.34f, center + Offset(size.width * 0.2f, -size.height * 0.16f))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodFaces(selected: String?, onSelect: (String) -> Unit) {
    val moods = listOf("Tired" to R.string.mood_tired, "Stressed" to R.string.mood_stressed, "Okay" to R.string.mood_okay, "Good" to R.string.mood_good, "Great" to R.string.mood_great)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        moods.forEachIndexed { index, mood ->
            val color = MaterialTheme.colorScheme.primary
            val ink = MaterialTheme.colorScheme.onSurface
            Surface(onClick = { onSelect(mood.first) }, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, if (selected == mood.first) color else MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.widthIn(min = 66.dp).heightIn(min = 80.dp).semantics { this.selected = selected == mood.first }) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(32.dp)) {
                        drawCircle(color.copy(alpha = 0.18f))
                        drawCircle(ink, 1.7.dp.toPx(), Offset(size.width * 0.35f, size.height * 0.38f)); drawCircle(ink, 1.7.dp.toPx(), Offset(size.width * 0.65f, size.height * 0.38f))
                        val mouth = Path().apply { moveTo(size.width * 0.3f, size.height * 0.68f); quadraticTo(size.width * 0.5f, size.height * (0.58f + index * 0.06f), size.width * 0.7f, size.height * 0.68f) }
                        drawPath(mouth, ink, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text(stringResource(mood.second), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProteinIconGrid(onSelect: (String, Int) -> Unit) {
    val foods = listOf(Triple(R.string.food_eggs, "protein", 12), Triple(R.string.food_lentils, "meal", 9), Triple(R.string.food_tofu, "meal", 12), Triple(R.string.food_yogurt, "water", 10))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        foods.forEach { food ->
            val name = stringResource(food.first)
            OptionTile(name, JourneyIcons.get(food.second), false, { onSelect(name, food.third) })
        }
    }
}

@Composable
fun rememberJourneyHaptic(enabled: Boolean): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val reduced = LocalReducedMotion.current
    return { if (enabled && !reduced) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
}