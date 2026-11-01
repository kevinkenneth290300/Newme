package com.example.ui.screens.plan

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
import com.example.data.local.entity.MealPlanEntity
import com.example.data.local.entity.MealPrepItemEntity
import com.example.data.local.entity.SupplementEntity
import com.example.ui.components.QuickProteinDialog
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel

@Composable
fun PlanScreen(viewModel: NewMeViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Meals & Protein, 1: Meal Prep Fridge, 2: Supplements

    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val totalProtein by viewModel.totalProteinLogged.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val mealPrepItems by viewModel.mealPrepItems.collectAsStateWithLifecycle()
    val allSupplements by viewModel.allSupplements.collectAsStateWithLifecycle()
    val supplementLogs by viewModel.supplementLogs.collectAsStateWithLifecycle()

    var showAddMealDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<MealPlanEntity?>(null) }
    var showQuickProteinDialog by remember { mutableStateOf(false) }
    var showAddPrepDialog by remember { mutableStateOf(false) }
    var showAddSuppDialog by remember { mutableStateOf(false) }

    val targetProtein = userProfile?.proteinTargetGrams ?: 110

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NewMeBackground)
    ) {
        // Top Bar & Tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "NUTRITION & SYSTEM PLAN",
                color = NewMePrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
            Text(
                text = "Fuel recovery, protect muscle, eliminate decision fatigue.",
                color = NewMeTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NewMeSurface)
                    .border(1.dp, NewMeBorder, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    title = "Meals",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 0 }

                TabButton(
                    title = "Meal Prep (${mealPrepItems.size})",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1.3f)
                ) { selectedTab = 1 }

                TabButton(
                    title = "Supplements",
                    isSelected = selectedTab == 2,
                    modifier = Modifier.weight(1.2f)
                ) { selectedTab = 2 }
            }
        }

        when (selectedTab) {
            0 -> {
                // Meals & Protein Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Protein Summary Card
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
                                        Text("DAILY PROTEIN TARGET", color = NewMeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "$totalProtein / ${targetProtein}g",
                                            color = NewMeTextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Button(
                                        onClick = { showQuickProteinDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("add_protein_from_plan")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimaryDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LOG FOOD", color = NewMePrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                val pProgress = (totalProtein.toFloat() / targetProtein).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { pProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = NewMePrimary,
                                    trackColor = NewMeSurfaceElevated
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (totalProtein >= targetProtein) "✓ Protein target achieved! Muscle retention secured."
                                    else "${targetProtein - totalProtein}g remaining today. Prioritize lean protein in dinner.",
                                    color = if (totalProtein >= targetProtein) NewMeTertiary else NewMeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Indian Foundation Blueprint Banner
                    if (userProfile?.usesRecommendedPlan == true) {
                        item {
                            Surface(
                                color = NewMeSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, NewMeTertiary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🇮🇳", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "INDIAN FOUNDATION PROTOCOL (${userProfile?.dietaryPreference?.uppercase() ?: "VEGETARIAN"})",
                                            color = NewMeTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "• Breakfast: 25-30g protein anchor (Besan Chilla / Eggs / Whey)\n• Lunch: Generous Dal Bowl + 200g Seasonal Sabzi + Curd\n• Dinner: Protein Focus (Paneer / Soya / Lean meat) + Salad",
                                        color = NewMeTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section Title & Add Meal
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PLANNED MEALS", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { showAddMealDialog = true },
                                modifier = Modifier.testTag("add_meal_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Meal", color = NewMePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (meals.isEmpty()) {
                        item {
                            Surface(
                                color = NewMeSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, NewMeBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No Meals Planned for Today", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Start by adding your first meal or populate recommended Indian foundation meals with high protein and 200g veg anchors.",
                                        color = NewMeTextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.applyIndianFoundationMeals() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("apply_foundation_meals_button")
                                    ) {
                                        Text("LOAD INDIAN FOUNDATION MEALS", color = NewMePrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Meals list
                    items(meals, key = { it.id }) { meal ->
                        MealCard(
                            meal = meal,
                            onToggle = { viewModel.toggleMealCompleted(meal) },
                            onEdit = { editingMeal = meal },
                            onDuplicate = { viewModel.duplicateMeal(meal) },
                            onDelete = { viewModel.deleteMeal(meal) }
                        )
                    }
                }
            }

            1 -> {
                // Meal Prep Fridge Inventory
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            color = NewMeSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NewMeTertiary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("READY TO EAT", color = NewMeTertiary, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                                        Text("Fridge Portions Prepared", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { showAddPrepDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("add_prep_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimaryDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PREP FOOD", color = NewMePrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Having prepared meals eliminates willpower depletion when hungry.",
                                    color = NewMeTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (mealPrepItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NewMeSurface)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Kitchen, contentDescription = null, tint = NewMeTextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Fridge Inventory Empty", color = NewMeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Prep chicken, eggs, or vegetables to stay ahead of cravings.", color = NewMeTextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddPrepDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary)
                                    ) {
                                        Text("ADD FIRST PREPPED FOOD", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(mealPrepItems, key = { it.id }) { item ->
                            MealPrepCard(
                                item = item,
                                onConsume = { viewModel.consumeMealPrepPortion(item) },
                                onDelete = { viewModel.deleteMealPrepItem(item) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Supplements Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Safety Disclaimer (Section 15, 30)
                    item {
                        Surface(
                            color = NewMeSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NewMeTertiary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Supplements should not replace a balanced diet or professional medical advice. Always discuss with a qualified healthcare professional.",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SCHEDULED SUPPLEMENTS", color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { showAddSuppDialog = true },
                                modifier = Modifier.testTag("add_supplement_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Supplement", color = NewMePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(allSupplements, key = { it.id }) { supp ->
                        val isTaken = supplementLogs.any { it.supplementId == supp.id }
                        SupplementCard(
                            supplement = supp,
                            isTaken = isTaken,
                            onToggleTaken = { viewModel.toggleSupplementTaken(supp, isTaken) },
                            onDelete = { viewModel.deleteSupplement(supp) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showQuickProteinDialog) {
        QuickProteinDialog(
            onLogProtein = { name, portion, grams ->
                viewModel.quickAddProtein(name, portion, grams)
            },
            onDismiss = { showQuickProteinDialog = false }
        )
    }

    if (showAddMealDialog || editingMeal != null) {
        MealEditDialog(
            initialMeal = editingMeal,
            onSave = { meal ->
                if (editingMeal != null) {
                    viewModel.updateMeal(meal)
                } else {
                    viewModel.addMeal(meal)
                }
                showAddMealDialog = false
                editingMeal = null
            },
            onDismiss = {
                showAddMealDialog = false
                editingMeal = null
            }
        )
    }

    if (showAddPrepDialog) {
        AddMealPrepDialog(
            onAdd = { name, portions, expiry, notes ->
                viewModel.addMealPrepItem(name, portions, expiry, notes)
                showAddPrepDialog = false
            },
            onDismiss = { showAddPrepDialog = false }
        )
    }

    if (showAddSuppDialog) {
        AddSupplementDialog(
            onAdd = { supp ->
                viewModel.addSupplement(supp)
                showAddSuppDialog = false
            },
            onDismiss = { showAddSuppDialog = false }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NewMePrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) NewMePrimaryDark else NewMeTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MealCard(
    meal: MealPlanEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (meal.isCompleted) NewMeSurfaceVariant else NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (meal.isCompleted) NewMeTertiary.copy(alpha = 0.4f) else NewMeBorder),
        modifier = Modifier.fillMaxWidth().testTag("meal_card_${meal.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = meal.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NewMeTertiary,
                            uncheckedColor = NewMeTextSecondary,
                            checkmarkColor = NewMePrimaryDark
                        ),
                        modifier = Modifier.size(24.dp).testTag("meal_checkbox_${meal.id}")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(meal.slot.uppercase(), color = NewMePrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                        Text(meal.name, color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NewMeTextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeCoral.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(meal.foodsDescription, color = NewMeTextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroTag(label = "PROTEIN", value = "${meal.proteinGrams}g", color = NewMePrimary)
                MacroTag(label = "CARBS", value = "${meal.carbsGrams}g", color = NewMeSecondary)
                MacroTag(label = "FATS", value = "${meal.fatsGrams}g", color = NewMeTertiary)
                MacroTag(label = "CALORIES", value = "~${meal.approxCalories} kcal", color = NewMeTextSecondary)
            }
        }
    }
}

@Composable
private fun MacroTag(label: String, value: String, color: Color) {
    Column {
        Text(label, color = NewMeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MealPrepCard(
    item: MealPrepItemEntity,
    onConsume: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NewMeBorder),
        modifier = Modifier.fillMaxWidth().testTag("prep_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.foodName, color = NewMeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${item.portionsAvailable} portions available • Exp: ${item.expiryDate}",
                    color = NewMeTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.notes.isNotBlank()) {
                    Text(item.notes, color = NewMeTextSecondary, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onConsume,
                    colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("consume_portion_${item.id}")
                ) {
                    Text("EAT 1", color = NewMePrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SupplementCard(
    supplement: SupplementEntity,
    isTaken: Boolean,
    onToggleTaken: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (isTaken) NewMeSurfaceVariant else NewMeSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isTaken) NewMeTertiary.copy(alpha = 0.5f) else NewMeBorder),
        modifier = Modifier.fillMaxWidth().testTag("supp_card_${supplement.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isTaken,
                    onCheckedChange = { onToggleTaken() },
                    colors = CheckboxDefaults.colors(checkedColor = NewMeTertiary, checkmarkColor = NewMePrimaryDark)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(supplement.name, color = NewMeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = NewMeSurfaceElevated,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                supplement.category,
                                color = if (supplement.category == "Required") NewMePrimary else NewMeTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("${supplement.dose} • ${supplement.timing}", color = NewMeTextSecondary, fontSize = 12.sp)
                    if (supplement.notes.isNotBlank()) {
                        Text(supplement.notes, color = NewMeTextMuted, fontSize = 10.sp)
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NewMeTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------- DIALOGS ----------------
@Composable
private fun MealEditDialog(
    initialMeal: MealPlanEntity?,
    onSave: (MealPlanEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var slot by remember { mutableStateOf(initialMeal?.slot ?: "Morning") }
    var name by remember { mutableStateOf(initialMeal?.name ?: "") }
    var time by remember { mutableStateOf(initialMeal?.time ?: "08:00 AM") }
    var foods by remember { mutableStateOf(initialMeal?.foodsDescription ?: "") }
    var protein by remember { mutableStateOf(initialMeal?.proteinGrams?.toString() ?: "30") }
    var carbs by remember { mutableStateOf(initialMeal?.carbsGrams?.toString() ?: "40") }
    var fats by remember { mutableStateOf(initialMeal?.fatsGrams?.toString() ?: "10") }
    var calories by remember { mutableStateOf(initialMeal?.approxCalories?.toString() ?: "370") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (initialMeal != null) "Edit Meal" else "Add Meal", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = slot,
                    onValueChange = { slot = it },
                    label = { Text("Slot (e.g. Lunch, Dinner)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = foods,
                    onValueChange = { foods = it },
                    label = { Text("Foods list (e.g. 150g Chicken, Rice)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Calories") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = NewMeTextSecondary)
                    }
                    Button(
                        onClick = {
                            val p = protein.toIntOrNull() ?: 20
                            val c = carbs.toIntOrNull() ?: 30
                            val f = fats.toIntOrNull() ?: 10
                            val cal = calories.toIntOrNull() ?: (p * 4 + c * 4 + f * 9)
                            val meal = (initialMeal ?: MealPlanEntity(
                                date = "",
                                slot = slot,
                                name = name.ifBlank { "Planned Meal" },
                                time = time,
                                foodsDescription = foods,
                                approxCalories = cal,
                                proteinGrams = p,
                                carbsGrams = c,
                                fatsGrams = f
                            )).copy(
                                slot = slot,
                                name = name.ifBlank { "Planned Meal" },
                                time = time,
                                foodsDescription = foods,
                                approxCalories = cal,
                                proteinGrams = p,
                                carbsGrams = c,
                                fatsGrams = f
                            )
                            onSave(meal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Meal", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMealPrepDialog(
    onAdd: (name: String, portions: Int, expiry: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("4") }
    var expiry by remember { mutableStateOf("In 3 days") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Meal Prep Inventory", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name (e.g. Boiled Eggs, Chicken)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = portions,
                        onValueChange = { portions = it },
                        label = { Text("Portions") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { expiry = it },
                        label = { Text("Expiry / Reminder") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val p = portions.toIntOrNull() ?: 1
                        if (name.isNotBlank()) {
                            onAdd(name, p, expiry, notes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMeTertiary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("ADD TO FRIDGE", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddSupplementDialog(
    onAdd: (SupplementEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("1 serving") }
    var timing by remember { mutableStateOf("Morning") }
    var category by remember { mutableStateOf("Optional") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NewMeSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Supplement", color = NewMeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Creatine Monohydrate)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timing,
                        onValueChange = { timing = it },
                        label = { Text("Timing") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Required, Optional, Doctor prescribed)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(SupplementEntity(name = name, dose = dose, timing = timing, category = category, notes = notes, isActive = true))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("SAVE SUPPLEMENT", color = NewMePrimaryDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
