package com.example.ui.screens.gym

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ExerciseLogEntity
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel

@Composable
fun GymScreen(viewModel: NewMeViewModel) {
    val workoutSession by viewModel.workoutSession.collectAsStateWithLifecycle()
    val exercises by viewModel.workoutExercises.collectAsStateWithLifecycle()
    val cardioLogs by viewModel.cardioLogs.collectAsStateWithLifecycle()

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAddCardioDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }

    val currentSession = workoutSession

    var title by remember(currentSession) { mutableStateOf(currentSession?.title ?: "TODAY'S WORKOUT") }
    var warmUpDone by remember(currentSession) { mutableStateOf(currentSession?.warmUpDone ?: false) }
    var strengthDone by remember(currentSession) { mutableStateOf(currentSession?.strengthDone ?: false) }
    var cardioDone by remember(currentSession) { mutableStateOf(currentSession?.cardioDone ?: false) }
    var coolDownDone by remember(currentSession) { mutableStateOf(currentSession?.coolDownDone ?: false) }
    var isCompleted by remember(currentSession) { mutableStateOf(currentSession?.isCompleted ?: false) }
    var durationMinutes by remember(currentSession) { mutableStateOf(currentSession?.durationMinutes ?: 0) }
    var notes by remember(currentSession) { mutableStateOf(currentSession?.notes ?: "") }

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
                    Text("GYM & TRAINING", color = NewMePurple, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                    Text("Track progressive overload. Lift with intent.", color = NewMeTextSecondary, fontSize = 12.sp)
                }

                Surface(
                    color = if (isCompleted) NewMeTertiary.copy(alpha = 0.2f) else NewMeSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isCompleted) NewMeTertiary else NewMeBorder)
                ) {
                    Text(
                        if (isCompleted) "✓ SESSION DONE" else "IN PROGRESS",
                        color = if (isCompleted) NewMeTertiary else NewMePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Active Split Selector & Overview Card
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
                        Text(title, color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("$durationMinutes MINS", color = NewMePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("SESSION CHECKPOINTS:", color = NewMeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Checkpoints row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SessionCheckpoint(label = "Warm-up", checked = warmUpDone, onToggle = {
                            warmUpDone = !warmUpDone
                            viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                        }, modifier = Modifier.weight(1f))

                        SessionCheckpoint(label = "Strength", checked = strengthDone, onToggle = {
                            strengthDone = !strengthDone
                            viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                        }, modifier = Modifier.weight(1f))

                        SessionCheckpoint(label = "Cardio", checked = cardioDone, onToggle = {
                            cardioDone = !cardioDone
                            viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                        }, modifier = Modifier.weight(1f))

                        SessionCheckpoint(label = "Cool-down", checked = coolDownDone, onToggle = {
                            coolDownDone = !coolDownDone
                            viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                        }, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    if (notes.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NewMeSurfaceElevated)
                                .clickable { showEditNotesDialog = true }
                                .padding(10.dp)
                        ) {
                            Text("Note: $notes", color = NewMeTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            isCompleted = !isCompleted
                            viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) NewMeSurfaceElevated else NewMePurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("toggle_session_completed")
                    ) {
                        Text(
                            if (isCompleted) "SESSION LOGGED (+30 XP)" else "MARK SESSION COMPLETED (+30 XP)",
                            color = if (isCompleted) NewMeTertiary else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Exercises Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXERCISES & PROGRESSION", color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { showAddExerciseDialog = true },
                    modifier = Modifier.testTag("add_exercise_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NewMePurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Exercise", color = NewMePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Exercises list
        if (exercises.isEmpty()) {
            item {
                Surface(
                    color = NewMeSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No exercises logged yet", color = NewMeTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showAddExerciseDialog = true }) {
                            Text("Add First Exercise", color = NewMePurple)
                        }
                    }
                }
            }
        } else {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onDelete = { viewModel.deleteExercise(exercise) }
                )
            }
        }

        // Cardio Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CARDIO & STEPS", color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("15–20 min incline walk or steady-state", color = NewMeTextSecondary, fontSize = 11.sp)
                }
                TextButton(
                    onClick = { showAddCardioDialog = true },
                    modifier = Modifier.testTag("add_cardio_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NewMeSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Cardio", color = NewMeSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(cardioLogs, key = { it.id }) { cardio ->
            CardioCard(
                cardio = cardio,
                onDelete = { viewModel.deleteCardio(cardio) }
            )
        }
    }

    // Dialogs
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onAdd = { name, category, setsSummary, prevPerf ->
                val sessionId = currentSession?.id ?: 1L
                viewModel.addExercise(sessionId, name, category, setsSummary, prevPerf)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    if (showAddCardioDialog) {
        AddCardioDialog(
            onAdd = { type, duration, distance, intensity, calories ->
                viewModel.logCardio(type, duration, distance, intensity, calories)
                showAddCardioDialog = false
            },
            onDismiss = { showAddCardioDialog = false }
        )
    }

    if (showEditNotesDialog) {
        EditNotesDialog(
            currentNotes = notes,
            onSave = { updated ->
                notes = updated
                viewModel.saveWorkoutSession(title, durationMinutes, warmUpDone, strengthDone, cardioDone, coolDownDone, isCompleted, notes)
                showEditNotesDialog = false
            },
            onDismiss = { showEditNotesDialog = false }
        )
    }
}

@Composable
private fun SessionCheckpoint(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (checked) NewMeSurfaceVariant else NewMeSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (checked) NewMeTertiary.copy(alpha = 0.5f) else Color.Transparent),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (checked) "✓" else "○",
                color = if (checked) NewMeTertiary else NewMeTextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                color = if (checked) NewMeTextPrimary else NewMeTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseLogEntity,
    onDelete: () -> Unit
) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = Modifier.fillMaxWidth().testTag("exercise_card_${exercise.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(exercise.category.uppercase(), color = NewMePurple, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(exercise.exerciseName, color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(exercise.setsSummary, color = NewMeTextSecondary, fontSize = 13.sp)

            if (exercise.previousPerformance.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = NewMeSurfaceElevated,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        exercise.previousPerformance,
                        color = NewMeTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioCard(
    cardio: com.example.data.local.entity.CardioLogEntity,
    onDelete: () -> Unit
) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(cardio.cardioType, color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${cardio.durationMinutes} min • ${cardio.distanceKm} km • ${cardio.intensity} • ~${cardio.calories} kcal",
                    color = NewMeSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(
    onAdd: (name: String, category: String, setsSummary: String, prevPerf: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Chest") }
    var setsSummary by remember { mutableStateOf("3 sets: 10 reps") }
    var prevPerf by remember { mutableStateOf("Target: +1 rep or +1.25kg") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Exercise & Sets", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise (e.g. Incline DB Press)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Muscle / Category (Chest, Back, Legs...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = setsSummary,
                    onValueChange = { setsSummary = it },
                    label = { Text("Sets & Reps (e.g. 4 sets: 40kg×8, 40kg×8)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prevPerf,
                    onValueChange = { prevPerf = it },
                    label = { Text("Progression Target / Previous Record") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name, category, setsSummary, prevPerf)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePurple),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("SAVE EXERCISE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddCardioDialog(
    onAdd: (type: String, duration: Int, distance: Float, intensity: String, calories: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf("Incline Treadmill Walk") }
    var duration by remember { mutableStateOf("20") }
    var distance by remember { mutableStateOf("1.5") }
    var intensity by remember { mutableStateOf("Moderate") }
    var calories by remember { mutableStateOf("110") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Log Cardio Session", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Cardio Type (e.g. Walking, Cycling)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance (km)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = intensity,
                        onValueChange = { intensity = it },
                        label = { Text("Intensity") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories (~kcal)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val d = duration.toIntOrNull() ?: 15
                        val dist = distance.toFloatOrNull() ?: 1.0f
                        val cal = calories.toIntOrNull() ?: 80
                        onAdd(type, d, dist, intensity, cal)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMeSecondary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("LOG CARDIO", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditNotesDialog(
    currentNotes: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notesText by remember { mutableStateOf(currentNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Session Notes", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (e.g. felt strong, felt tight)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSave(notesText) },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Save Notes", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
