package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.theme.*

// ----------------- WATER QUICK ADD -----------------
@Composable
fun QuickWaterDialog(
    currentAmountMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customAmount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("quick_water_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Log Water Intake", color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "%.2f / %.2f L".format(currentAmountMl / 1000f, targetMl / 1000f),
                            color = NewMeSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                val progress = (currentAmountMl.toFloat() / targetMl).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = NewMeSecondary,
                    trackColor = NewMeSurfaceElevated
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("Quick Add:", color = NewMeTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(10.dp))

                val increments = listOf(250, 500, 750, 1000)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    increments.forEach { ml ->
                        Button(
                            onClick = {
                                onAddWater(ml)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NewMeSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, NewMeSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .testTag("quick_water_${ml}ml_button")
                        ) {
                            Text(
                                if (ml >= 1000) "+1 L" else "+${ml}ml",
                                color = NewMeSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        label = { Text("Custom (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("custom_water_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val amount = customAmount.toIntOrNull()
                            if (amount != null && amount > 0) {
                                onAddWater(amount)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NewMeSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp).testTag("custom_water_add_button")
                    ) {
                        Text("Add", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------- PROTEIN QUICK ADD -----------------
data class QuickFood(
    val name: String,
    val defaultPortion: String,
    val defaultProteinGrams: Int
)

val DEFAULT_PROTEIN_FOODS = listOf(
    QuickFood("Whey Protein", "1 Scoop (30g)", 24),
    QuickFood("Farm Eggs", "2 Whole Eggs", 12),
    QuickFood("Chicken Breast", "100g Cooked", 31),
    QuickFood("Fresh Paneer", "100g Raw", 18),
    QuickFood("White Fish", "100g Cooked", 22),
    QuickFood("Cow Milk", "250 ml", 8),
    QuickFood("Greek Curd / Dahi", "150g Cup", 10),
    QuickFood("Boiled Chana", "100g Cooked", 9)
)

@Composable
fun QuickProteinDialog(
    onLogProtein: (foodName: String, portion: String, grams: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFood by remember { mutableStateOf<QuickFood?>(null) }
    var foodNameInput by remember { mutableStateOf("") }
    var portionInput by remember { mutableStateOf("") }
    var proteinGramsInput by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("quick_protein_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log Protein Food", color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedFood == null && !isCustom) {
                    Text("Select source or enter custom:", color = NewMeTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DEFAULT_PROTEIN_FOODS) { food ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NewMeSurfaceElevated)
                                    .border(1.dp, NewMeBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedFood = food
                                        foodNameInput = food.name
                                        portionInput = food.defaultPortion
                                        proteinGramsInput = food.defaultProteinGrams.toString()
                                    }
                                    .padding(12.dp)
                                    .testTag("protein_food_${food.name.replace(" ", "_").lowercase()}")
                            ) {
                                Column {
                                    Text(food.name, color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(food.defaultPortion, color = NewMeTextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("+${food.defaultProteinGrams}g Protein", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            isCustom = true
                            foodNameInput = ""
                            portionInput = "1 Serving"
                            proteinGramsInput = "20"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("custom_protein_button")
                    ) {
                        Text("+ Custom Food Entry", color = NewMePrimary)
                    }
                } else {
                    // Edit portion & grams confirmation
                    Text("Confirm Details", color = NewMeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = foodNameInput,
                        onValueChange = { foodNameInput = it },
                        label = { Text("Food Name") },
                        modifier = Modifier.fillMaxWidth().testTag("protein_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portionInput,
                        onValueChange = { portionInput = it },
                        label = { Text("Portion Description") },
                        modifier = Modifier.fillMaxWidth().testTag("protein_portion_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = proteinGramsInput,
                        onValueChange = { proteinGramsInput = it },
                        label = { Text("Protein (Grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("protein_grams_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedFood = null
                                isCustom = false
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back", color = NewMeTextSecondary)
                        }
                        Button(
                            onClick = {
                                val grams = proteinGramsInput.toIntOrNull() ?: 0
                                if (foodNameInput.isNotBlank() && grams > 0) {
                                    onLogProtein(foodNameInput, portionInput, grams)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).height(48.dp).testTag("confirm_protein_log_button")
                        ) {
                            Text("LOG PROTEIN", color = Color(0xFF1A0E00), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------- SLEEP LOG DIALOG -----------------
@Composable
fun QuickSleepDialog(
    onLogSleep: (bedtime: String, wakeTime: String, durationMinutes: Int, quality: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var bedtime by remember { mutableStateOf("11:00 PM") }
    var wakeTime by remember { mutableStateOf("06:30 AM") }
    var hours by remember { mutableStateOf("7") }
    var minutes by remember { mutableStateOf("30") }
    var quality by remember { mutableStateOf(4) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("quick_sleep_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log Sleep & Recovery", color = NewMeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bedtime,
                        onValueChange = { bedtime = it },
                        label = { Text("Bedtime") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wakeTime,
                        onValueChange = { wakeTime = it },
                        label = { Text("Wake Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Duration (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Quality Rating (1-5):", color = NewMeTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (1..5).forEach { star ->
                        Button(
                            onClick = { quality = star },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (quality == star) NewMePrimary else NewMeSurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(44.dp).testTag("sleep_star_$star")
                        ) {
                            Text(
                                "$star★",
                                color = if (quality == star) Color(0xFF1A0E00) else NewMeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val totalMin = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
                if (totalMin in 1..419) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NewMeCoral.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Text(
                            "Recovery is becoming a bottleneck. Aim for 7h+ to maximize fat loss and muscle retention.",
                            color = NewMeCoral,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val duration = (hours.toIntOrNull() ?: 7) * 60 + (minutes.toIntOrNull() ?: 0)
                        onLogSleep(bedtime, wakeTime, duration, quality)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_sleep_button")
                ) {
                    Text("SAVE SLEEP LOG", color = Color(0xFF003824), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------- DYNAMIC ACTIVITY QUICK ADD -----------------
@Composable
fun QuickActivityDialog(
    initialActivityType: String = "Walking",
    onLogActivity: (type: String, title: String, durationMin: Int, intensity: String, distanceKm: Float?, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val activityTypes = listOf(
        "Walking" to "🚶",
        "Running" to "🏃",
        "Yoga" to "🧘",
        "Gym" to "🏋️",
        "Dance" to "💃",
        "Sports" to "🏸",
        "Active" to "🏠"
    )

    var selectedType by remember { mutableStateOf(initialActivityType) }
    var title by remember { mutableStateOf("Daily $initialActivityType") }
    var durationText by remember { mutableStateOf("35") }
    var intensity by remember { mutableStateOf("Moderate") }
    var distanceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("quick_activity_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Log Movement & Activity", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NewMeTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Activity Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activityTypes.forEach { (type, emoji) ->
                        val isSelected = selectedType.equals(type, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NewMePrimary.copy(alpha = 0.2f) else NewMeSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedType = type
                                    title = "$type Session"
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(emoji, fontSize = 16.sp)
                                Text(
                                    type,
                                    fontSize = 9.sp,
                                    color = if (isSelected) NewMePrimary else NewMeTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NewMePrimary,
                        unfocusedBorderColor = NewMeBorder,
                        focusedTextColor = NewMeTextPrimary,
                        unfocusedTextColor = NewMeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("activity_duration_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NewMePrimary,
                            unfocusedBorderColor = NewMeBorder,
                            focusedTextColor = NewMeTextPrimary,
                            unfocusedTextColor = NewMeTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        label = { Text("Dist. km (opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NewMePrimary,
                            unfocusedBorderColor = NewMeBorder,
                            focusedTextColor = NewMeTextPrimary,
                            unfocusedTextColor = NewMeTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Intensity
                Text("Intensity", color = NewMeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Light", "Moderate", "High").forEach { level ->
                        val isSelected = intensity == level
                        Surface(
                            color = if (isSelected) NewMePrimary else NewMeSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { intensity = level }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    level,
                                    color = if (isSelected) NewMePrimaryDark else NewMeTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull() ?: 30
                        val distance = distanceText.toFloatOrNull()
                        onLogActivity(
                            selectedType,
                            title.ifBlank { "$selectedType Session" },
                            duration,
                            intensity,
                            distance,
                            notes
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_activity_button")
                ) {
                    Text("LOG ACTIVITY (+20 PTS)", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

