package com.example.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.BodyMeasurementEntity
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate

@Composable
fun ProgressScreen(viewModel: NewMeViewModel) {
    val measurements by viewModel.bodyMeasurements.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()

    var showAddCheckInDialog by remember { mutableStateOf(false) }

    val latest = measurements.maxByOrNull { it.date }
    val earliest = measurements.minByOrNull { it.date }

    val currentWeight = latest?.weightKg ?: userProfile?.currentWeightKg ?: 0f
    val initialWeight = earliest?.weightKg ?: userProfile?.initialWeightKg ?: currentWeight
    val weightDiff = if (initialWeight > 0f && currentWeight > 0f) currentWeight - initialWeight else 0f

    val currentFat = latest?.bodyFatPercent ?: userProfile?.bodyFatPercent ?: 0f
    val initialFat = earliest?.bodyFatPercent ?: userProfile?.bodyFatPercent ?: currentFat
    val fatDiff = if (initialFat > 0f && currentFat > 0f) currentFat - initialFat else 0f

    val currentMuscle = latest?.skeletalMuscleKg ?: userProfile?.skeletalMuscleKg ?: 0f
    val initialMuscle = earliest?.skeletalMuscleKg ?: userProfile?.skeletalMuscleKg ?: currentMuscle
    val muscleDiff = if (initialMuscle > 0f && currentMuscle > 0f) currentMuscle - initialMuscle else 0f

    val currentWaist = latest?.waistCm ?: 0f
    val initialWaist = earliest?.waistCm ?: currentWaist
    val waistDiff = if (initialWaist > 0f && currentWaist > 0f) currentWaist - initialWaist else 0f

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BODY TRANSFORMATION", color = NewMePrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Track trends, not daily noise. Muscle retained, fat lost.", color = NewMeTextSecondary, fontSize = 12.sp)
                }

                Button(
                    onClick = { showAddCheckInDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_checkin_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimaryDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CHECK-IN", color = NewMePrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recomposition Verdict Card
        item {
            Surface(
                color = Color(0xFF0D1C17),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NewMeTertiary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = NewMeTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "TRUE BODY RECOMPOSITION",
                            color = NewMeTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (measurements.isNotEmpty()) {
                                "Muscle gained (%+.1f kg) while fat changed (%+.1f%%). Scale weight alone is deceptive.".format(
                                    muscleDiff,
                                    fatDiff
                                )
                            } else {
                                "Log your initial body check-in to start tracking true body recomposition and trend analytics."
                            },
                            color = NewMeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Key Metric Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "CURRENT WEIGHT",
                        value = if (currentWeight > 0f) "%.1f kg".format(currentWeight) else "—",
                        change = if (measurements.size > 1) "%+.1f kg net".format(weightDiff) else "Baseline",
                        isPositiveOutcome = weightDiff <= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "BODY FAT",
                        value = if (currentFat > 0f) "%.1f %%".format(currentFat) else "—",
                        change = if (measurements.size > 1) "%+.1f %% net".format(fatDiff) else "Baseline",
                        isPositiveOutcome = fatDiff <= 0,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        label = "SKELETAL MUSCLE",
                        value = if (currentMuscle > 0f) "%.1f kg".format(currentMuscle) else "—",
                        change = if (measurements.size > 1) "%+.1f kg muscle".format(muscleDiff) else "Baseline",
                        isPositiveOutcome = muscleDiff >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "WAIST MEASUREMENT",
                        value = if (currentWaist > 0f) "%.1f cm".format(currentWaist) else "—",
                        change = if (measurements.size > 1) "%+.1f cm reduction".format(waistDiff) else "Baseline",
                        isPositiveOutcome = waistDiff <= 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Secondary InBody Indicators (BMR, Visceral, Score)
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIndicator(label = "InBody Score", value = if (latest?.inBodyScore != null) "${latest.inBodyScore}/100" else "—", color = NewMePrimary)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = NewMeBorder)
                    MiniIndicator(label = "Visceral Fat", value = if (latest?.visceralFat != null) "Level ${latest.visceralFat.toInt()}" else "—", color = NewMeSecondary)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = NewMeBorder)
                    val displayBmr = latest?.bmr ?: userProfile?.bmr ?: 0
                    MiniIndicator(label = "BMR", value = if (displayBmr > 0) "$displayBmr kcal" else "—", color = NewMeTertiary)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = NewMeBorder)
                    MiniIndicator(label = "BMI", value = if (latest?.bmi != null) "%.1f".format(latest.bmi) else "—", color = Color(0xFF818CF8))
                }
            }
        }

        // Discipline & Streak Milestones
        item {
            Surface(
                color = NewMeSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NewMeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DISCIPLINE & MOMENTUM STATS", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatPill("CURRENT STREAK", "${userProfile?.currentStreak ?: 0} Days", NewMePrimary)
                        StatPill("LONGEST STREAK", "${userProfile?.longestStreak ?: 0} Days", NewMeSecondary)
                        StatPill("PERFECT DAYS", "${userProfile?.perfectDays ?: 0}", NewMeTertiary)
                        StatPill("RECOVERY DAYS", "${userProfile?.recoveryDays ?: 0}", Color(0xFF818CF8))
                    }
                }
            }
        }

        // Check-in History List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CHECK-IN HISTORY", color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (measurements.isNotEmpty()) {
                    Text("${measurements.size} ENTRIES", color = NewMeTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (measurements.isEmpty()) {
            item {
                Surface(
                    color = NewMeSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NewMeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No body check-ins logged yet. Tap '+ CHECK-IN' above to record weight, body fat %, muscle mass, and measurements.",
                        color = NewMeTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(measurements.sortedByDescending { it.date }, key = { it.id }) { checkIn ->
            CheckInCard(
                checkIn = checkIn,
                onDelete = { viewModel.deleteMeasurement(checkIn) }
            )
        }

        // Achievements & Badges Shelf
        item {
            Text("ACHIEVEMENTS UNLOCKED", color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        items(achievements.chunked(2)) { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { ach ->
                    AchievementCard(achievement = ach, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Add Check-In Dialog
    if (showAddCheckInDialog) {
        AddCheckInDialog(
            latest = latest,
            onSave = { measurement ->
                viewModel.saveMeasurement(measurement)
                showAddCheckInDialog = false
            },
            onDismiss = { showAddCheckInDialog = false }
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    change: String,
    isPositiveOutcome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = NewMeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                change,
                color = if (isPositiveOutcome) NewMeTertiary else NewMeCoral,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MiniIndicator(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = NewMeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CheckInCard(
    checkIn: BodyMeasurementEntity,
    onDelete: () -> Unit
) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = Modifier.fillMaxWidth().testTag("checkin_item_${checkIn.date}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(checkIn.date, color = NewMePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Weight", color = NewMeTextMuted, fontSize = 10.sp)
                    Text("%.1f kg".format(checkIn.weightKg), color = NewMeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                checkIn.bodyFatPercent?.let { fat ->
                    Column {
                        Text("Body Fat", color = NewMeTextMuted, fontSize = 10.sp)
                        Text("%.1f%%".format(fat), color = NewMeSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                checkIn.skeletalMuscleKg?.let { muscle ->
                    Column {
                        Text("Muscle", color = NewMeTextMuted, fontSize = 10.sp)
                        Text("%.1f kg".format(muscle), color = NewMeTertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                checkIn.waistCm?.let { waist ->
                    Column {
                        Text("Waist", color = NewMeTextMuted, fontSize = 10.sp)
                        Text("%.1f cm".format(waist), color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (checkIn.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(checkIn.notes, color = NewMeTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: com.example.data.local.entity.AchievementEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (achievement.isUnlocked) Color(0xFF19202E) else NewMeSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (achievement.isUnlocked) NewMePrimary.copy(alpha = 0.5f) else NewMeBorder
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (achievement.isUnlocked) "🏆" else "🔒",
                    fontSize = 18.sp
                )
                if (achievement.isUnlocked) {
                    Text(
                        "UNLOCKED",
                        color = NewMePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                achievement.title,
                color = if (achievement.isUnlocked) NewMeTextPrimary else NewMeTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                achievement.description,
                color = NewMeTextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun AddCheckInDialog(
    latest: BodyMeasurementEntity?,
    onSave: (BodyMeasurementEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var weight by remember { mutableStateOf(latest?.weightKg?.toString() ?: "70.5") }
    var fat by remember { mutableStateOf(latest?.bodyFatPercent?.toString() ?: "35.0") }
    var muscle by remember { mutableStateOf(latest?.skeletalMuscleKg?.toString() ?: "25.8") }
    var waist by remember { mutableStateOf(latest?.waistCm?.toString() ?: "84.5") }
    var inBodyScore by remember { mutableStateOf(latest?.inBodyScore?.toString() ?: "74") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Log Body Check-In", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Body Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = muscle,
                        onValueChange = { muscle = it },
                        label = { Text("Muscle (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waist,
                        onValueChange = { waist = it },
                        label = { Text("Waist (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inBodyScore,
                    onValueChange = { inBodyScore = it },
                    label = { Text("InBody Score (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Observations") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val w = weight.toFloatOrNull() ?: 70.0f
                        val f = fat.toFloatOrNull()
                        val m = muscle.toFloatOrNull()
                        val waistVal = waist.toFloatOrNull()
                        val score = inBodyScore.toIntOrNull() ?: 70
                        onSave(
                            BodyMeasurementEntity(
                                date = date,
                                weightKg = w,
                                bodyFatPercent = f,
                                skeletalMuscleKg = m,
                                waistCm = waistVal,
                                inBodyScore = score,
                                notes = notes
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("SAVE CHECK-IN", color = Color(0xFF1A0E00), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
