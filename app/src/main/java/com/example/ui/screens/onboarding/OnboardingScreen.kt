package com.example.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.theme.*
import com.example.util.NutritionCalculator
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate
import kotlin.random.Random

@Composable
fun OnboardingScreen(
    viewModel: NewMeViewModel,
    onLaunchToday: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 11

    // Step 1: Name & Greeting
    var name by remember { mutableStateOf("") }

    // Step 2: What are you trying to change? (multi-select)
    val changeOptions = listOf(
        "Lose body fat",
        "Build / protect muscle",
        "Become more disciplined",
        "Improve sleep",
        "Eat better",
        "Become more active",
        "Build better routines",
        "Improve confidence",
        "Feel mentally stronger",
        "Become more consistent"
    )
    var selectedChanges by remember {
        mutableStateOf(setOf("Become more disciplined", "Become more consistent", "Eat better"))
    }

    // Step 3: Why starting now?
    val reasonOptions = listOf(
        "I keep starting and stopping",
        "I know what to do, but I don't stay consistent",
        "My health has taken a back seat",
        "I want to feel better about myself",
        "I want structure in my day",
        "I want accountability",
        "I want to become more disciplined",
        "I simply want to become better",
        "Something else"
    )
    var selectedReason by remember { mutableStateOf("I keep starting and stopping") }
    var customReasonText by remember { mutableStateOf("") }

    // Step 4: Who are you becoming? (up to 3 identity traits)
    val identityOptions = listOf(
        "Disciplined" to "⚔️",
        "Strong" to "💪",
        "Calm" to "🧘",
        "Healthy" to "🌱",
        "Consistent" to "🔄",
        "Focused" to "🎯",
        "Confident" to "🦁",
        "Energetic" to "⚡",
        "Resilient" to "🛡️"
    )
    var selectedTraits by remember {
        mutableStateOf(setOf("Disciplined", "Consistent", "Calm"))
    }

    // Step 5: Starting metrics
    var ageText by remember { mutableStateOf("26") }
    var sex by remember { mutableStateOf("Male") }
    var heightText by remember { mutableStateOf("172") }
    var weightText by remember { mutableStateOf("72.0") }

    // Optional metrics with "I don't know this yet" toggles
    var bodyFatText by remember { mutableStateOf("") }
    var knowsBodyFat by remember { mutableStateOf(false) }
    var muscleText by remember { mutableStateOf("") }
    var knowsMuscle by remember { mutableStateOf(false) }
    var waistText by remember { mutableStateOf("") }
    var knowsWaist by remember { mutableStateOf(false) }
    var inBodyText by remember { mutableStateOf("") }
    var knowsInBody by remember { mutableStateOf(false) }

    // Step 6: What would progress look like?
    val progressGoalOptions = listOf(
        "Fat-loss goal" to "Leaner waist, reduced body fat percentage",
        "Muscle goal" to "Strength increases, skeletal muscle hypertrophy",
        "Target weight" to "Move toward a specific scale weight",
        "General health goal" to "Energy, longevity, vitality, peace of mind",
        "I don't know yet — help me decide" to "Sensible balanced foundation targets"
    )
    var selectedProgressGoal by remember { mutableStateOf("General health goal") }
    var targetWeightText by remember { mutableStateOf("") }

    // Step 7: Real life fit
    val wakeTimeOptions = listOf("Before 6", "6–7", "7–8", "8+")
    var selectedWakeTime by remember { mutableStateOf("6–7") }

    val sleepTimeOptions = listOf("9–10", "10–11", "11–12", "After 12")
    var selectedSleepTime by remember { mutableStateOf("10–11") }

    val workOptions = listOf("Office", "Remote", "Hybrid", "Shift work", "Other")
    var selectedWork by remember { mutableStateOf("Office") }

    val trainFreqOptions = listOf("2 days/week", "3 days/week", "4 days/week", "5+ days/week")
    var selectedTrainFreq by remember { mutableStateOf("3 days/week") }

    val difficultTimeOptions = listOf("Morning", "Afternoon", "Evening", "Late night", "Weekends")
    var selectedDifficultTime by remember { mutableStateOf("Evening") }

    // Step 8: Food made easier
    val dietOptions = listOf("Vegetarian", "Eggetarian", "Non-vegetarian", "Other")
    var selectedDiet by remember { mutableStateOf("Vegetarian") }

    val commonFoodList = listOf(
        "Rice", "Roti", "Eggs", "Chicken", "Fish",
        "Paneer", "Dal", "Oats", "Vegetables", "Fruit", "Curd / Yogurt", "Tofu"
    )
    var selectedCommonFoods by remember {
        mutableStateOf(setOf("Rice", "Roti", "Dal", "Vegetables", "Paneer"))
    }

    val foodAvoidOptions = listOf(
        "Late-night snacking", "Deep fried snacks", "Sugary drinks / soda",
        "Refined flour (Maida)", "Alcohol", "Excessive sweets"
    )
    var selectedAvoidedFoods by remember {
        mutableStateOf(setOf("Sugary drinks / soda", "Late-night snacking"))
    }

    val cookingSituationOptions = listOf("I cook", "Someone cooks for me", "Mostly eat outside", "Combination")
    var selectedCooking by remember { mutableStateOf("I cook") }

    val mealPrepOptions = listOf("I meal prep regularly", "Sometimes", "Rarely")
    var selectedMealPrep by remember { mutableStateOf("Sometimes") }

    // Step 9: How should NewMe talk to you?
    val toneOptions = listOf(
        Triple("BALANCED", "Balanced", "Support me when I'm struggling. Challenge me when I'm making excuses."),
        Triple("CALM COMPANION", "Calm Companion", "Thoughtful, gentle and reassuring."),
        Triple("DIRECT COACH", "Direct Coach", "Honest, practical and willing to challenge you."),
        Triple("STRICT ACCOUNTABILITY", "Strict Accountability", "Push me when I'm avoiding what I know I should do.")
    )
    var selectedTone by remember { mutableStateOf("BALANCED") }

    // Step 10: Voice
    var voiceChoice by remember { mutableStateOf(false) }

    // Execution
    fun saveAndLaunch() {
        val parsedAge = ageText.toIntOrNull() ?: 26
        val parsedHeight = heightText.toFloatOrNull() ?: 172f
        val parsedWeight = weightText.toFloatOrNull() ?: 72f
        val parsedTargetWeight = targetWeightText.toFloatOrNull()
        val parsedWaist = if (knowsWaist) waistText.toFloatOrNull() else null
        val parsedBodyFat = if (knowsBodyFat) bodyFatText.toFloatOrNull() else null
        val parsedMuscle = if (knowsMuscle) muscleText.toFloatOrNull() else null
        val parsedInBody = if (knowsInBody) inBodyText.toIntOrNull() else null

        val primaryActivity = when (selectedTrainFreq) {
            "4 days/week", "5+ days/week" -> "Gym / Strength Training"
            "2 days/week", "3 days/week" -> "Gym / Strength Training"
            else -> "General Active / Home Workouts"
        }

        val calculatedTargets = NutritionCalculator.calculateTargets(
            age = parsedAge,
            sex = sex,
            heightCm = parsedHeight,
            weightKg = parsedWeight,
            activityType = primaryActivity,
            goal = selectedProgressGoal
        )

        val cleanName = name.trim().ifBlank { "Jay" }
        val generatedCode = "NEWME-${cleanName.take(3).uppercase()}-${Random.nextInt(1000, 9999)}"

        val finalStartingReason = if (selectedReason == "Something else" && customReasonText.isNotBlank()) {
            customReasonText.trim()
        } else {
            selectedReason
        }

        val profile = UserProfileEntity(
            id = 1,
            name = cleanName,
            age = parsedAge,
            sex = sex,
            heightCm = parsedHeight,
            initialWeightKg = parsedWeight,
            currentWeightKg = parsedWeight,
            targetWeightKg = parsedTargetWeight,
            bodyFatPercent = parsedBodyFat,
            skeletalMuscleKg = parsedMuscle,
            waistCm = parsedWaist,
            inBodyScore = parsedInBody,
            primaryActivityType = primaryActivity,
            primaryGoal = selectedProgressGoal,
            bmr = calculatedTargets.bmr,
            tdee = calculatedTargets.tdee,
            calorieTarget = calculatedTargets.calorieTarget,
            proteinTargetGrams = calculatedTargets.proteinGrams,
            carbsTargetGrams = calculatedTargets.carbsGrams,
            fatTargetGrams = calculatedTargets.fatGrams,
            waterTargetLiters = calculatedTargets.waterLiters,
            sleepTargetHours = 7.5f,
            weeklyWorkoutsTarget = when (selectedTrainFreq) {
                "5+ days/week" -> 5
                "4 days/week" -> 4
                "3 days/week" -> 3
                else -> 2
            },
            goalDescription = selectedChanges.joinToString(", "),
            selectedGoals = selectedChanges.joinToString(","),
            selectedActivities = "Gym / Strength Training, Brisk Walking",
            activityFrequencies = "Gym: $selectedTrainFreq; Walking: Daily",
            trackingLevel = "Focused",
            dietaryPreference = selectedDiet,
            usesSupplements = true,
            usesRecommendedPlan = true,
            usesHealthConnect = false,
            voiceEnabled = voiceChoice,
            speakMorning = voiceChoice,
            speakMidday = false,
            speakEvening = voiceChoice,
            startDate = LocalDate.now().toString(),
            isOnboarded = true,
            currentLevel = 1,
            currentXp = 0,
            currentStreak = 0,
            longestStreak = 0,
            perfectDays = 0,
            recoveryDays = 0,
            isRecoveryActive = false,
            currentMomentum = 0,
            identityTraits = selectedTraits.joinToString(","),
            startingReason = finalStartingReason,
            progressGoalType = selectedProgressGoal,
            wakeTimeSlot = selectedWakeTime,
            sleepTimeSlot = selectedSleepTime,
            workType = selectedWork,
            trainingFrequency = selectedTrainFreq,
            difficultTimeOfDay = selectedDifficultTime,
            dietType = selectedDiet,
            commonFoods = selectedCommonFoods.joinToString(","),
            avoidedFoods = selectedAvoidedFoods.joinToString(","),
            cookingSituation = selectedCooking,
            mealPrepHabit = selectedMealPrep,
            companionTone = selectedTone,
            friendCode = generatedCode,
            hasSeenFirstMorningWelcome = false
        )

        viewModel.completeOnboarding(profile)
        onLaunchToday()
    }

    Scaffold(
        containerColor = NewMeBackground,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NewMeBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NewMePrimary
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NEWME COMPANION",
                            color = NewMePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Step ${step + 1} of $totalSteps",
                            color = NewMeTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    // Progress indicator
                    Text(
                        text = "${((step + 1) * 100 / totalSteps)}%",
                        color = NewMeTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NewMePrimary,
                    trackColor = NewMeSurfaceDarker
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(NewMeBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    // ==========================================
                    // SCREEN 1: Good to meet you
                    // ==========================================
                    0 -> {
                        item {
                            Text(
                                text = "Good to meet you.",
                                color = NewMeTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "I'm NewMe.\nThis isn't about becoming perfect.\nIt's about becoming more consistent, one decision at a time.",
                                color = NewMeTextSecondary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "What should I call you?",
                                color = NewMePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Enter your name (e.g., Jay)", color = NewMeTextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NewMePrimary,
                                    unfocusedBorderColor = NewMeBorder,
                                    focusedTextColor = NewMeTextPrimary,
                                    unfocusedTextColor = NewMeTextPrimary,
                                    focusedContainerColor = NewMeSurface,
                                    unfocusedContainerColor = NewMeSurface
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("name_input_field")
                            )
                        }
                    }

                    // ==========================================
                    // SCREEN 2: What are you trying to change?
                    // ==========================================
                    1 -> {
                        item {
                            Text(
                                text = "What are you trying to change?",
                                color = NewMeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select as many as apply to you right now.",
                                color = NewMeTextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                changeOptions.forEach { opt ->
                                    val isSelected = selectedChanges.contains(opt)
                                    Surface(
                                        color = if (isSelected) NewMePrimary.copy(alpha = 0.12f) else NewMeSurface,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) NewMePrimary else NewMeBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedChanges = if (isSelected) {
                                                    if (selectedChanges.size > 1) selectedChanges - opt else selectedChanges
                                                } else {
                                                    selectedChanges + opt
                                                }
                                            }
                                            .testTag("change_opt_$opt")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = opt,
                                                color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = NewMePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 3: Why starting NewMe now?
                    // ==========================================
                    2 -> {
                        item {
                            Text(
                                text = "Why are you starting NewMe now?",
                                color = NewMeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Honesty here helps NewMe calibrate its tone.",
                                color = NewMeTextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reasonOptions.forEach { r ->
                                    val isSelected = selectedReason == r
                                    Surface(
                                        color = if (isSelected) NewMePrimary.copy(alpha = 0.12f) else NewMeSurface,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) NewMePrimary else NewMeBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedReason = r }
                                            .testTag("reason_$r")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = r,
                                                color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = NewMePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedReason == "Something else") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = customReasonText,
                                        onValueChange = { customReasonText = it },
                                        placeholder = { Text("What brought you here today? (Optional)", color = NewMeTextMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedTextColor = NewMeTextPrimary,
                                            unfocusedTextColor = NewMeTextPrimary,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 4: Who are you becoming? (Identity)
                    // ==========================================
                    3 -> {
                        item {
                            Text(
                                text = "Who are you becoming?",
                                color = NewMeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose up to 3 identity traits.",
                                color = NewMePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "These traits will anchor your daily reflections and companion messages.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                identityOptions.forEach { (trait, icon) ->
                                    val isSelected = selectedTraits.contains(trait)
                                    Surface(
                                        color = if (isSelected) NewMePrimary.copy(alpha = 0.15f) else NewMeSurface,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) NewMePrimary else NewMeBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) {
                                                    if (selectedTraits.size > 1) selectedTraits = selectedTraits - trait
                                                } else {
                                                    if (selectedTraits.size < 3) {
                                                        selectedTraits = selectedTraits + trait
                                                    }
                                                }
                                            }
                                            .testTag("trait_$trait")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(icon, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = trait,
                                                    color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = NewMePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 5: Starting metrics
                    // ==========================================
                    4 -> {
                        item {
                            Text(
                                text = "Let's understand where you're starting.",
                                color = NewMeTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Basic metrics let us calculate sensible nutritional foundations.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Sex selection
                            Text("Sex", color = NewMeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { s ->
                                    val isSelected = sex == s
                                    Surface(
                                        color = if (isSelected) NewMePrimary else NewMeSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable { sex = s },
                                        border = BorderStroke(1.dp, if (isSelected) NewMePrimary else NewMeBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = s,
                                                color = if (isSelected) NewMePrimaryDark else NewMeTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Age & Height
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Age", color = NewMeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { ageText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Height (cm)", color = NewMeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = heightText,
                                        onValueChange = { heightText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Current Weight
                            Text("Current Weight (kg)", color = NewMeTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NewMePrimary,
                                    unfocusedBorderColor = NewMeBorder,
                                    focusedContainerColor = NewMeSurface,
                                    unfocusedContainerColor = NewMeSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = NewMeBorder)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "OPTIONAL METRICS",
                                color = NewMePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Do not worry if you don't know these yet. You can add them anytime in Progress.",
                                color = NewMeTextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Body fat %
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Body fat %", color = NewMeTextSecondary, fontSize = 13.sp)
                                if (knowsBodyFat) {
                                    OutlinedTextField(
                                        value = bodyFatText,
                                        onValueChange = { bodyFatText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    TextButton(onClick = { knowsBodyFat = true }) {
                                        Text("I know this", color = NewMePrimary, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Waist cm
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Waist circumference (cm)", color = NewMeTextSecondary, fontSize = 13.sp)
                                if (knowsWaist) {
                                    OutlinedTextField(
                                        value = waistText,
                                        onValueChange = { waistText = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    TextButton(onClick = { knowsWaist = true }) {
                                        Text("I know this", color = NewMePrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 6: What would progress look like?
                    // ==========================================
                    5 -> {
                        item {
                            Text(
                                text = "What would progress look like?",
                                color = NewMeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose the outcome you wish to orient toward. We do not force a target weight.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                progressGoalOptions.forEach { (goalTitle, goalDesc) ->
                                    val isSelected = selectedProgressGoal == goalTitle
                                    Surface(
                                        color = if (isSelected) NewMePrimary.copy(alpha = 0.12f) else NewMeSurface,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) NewMePrimary else NewMeBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedProgressGoal = goalTitle }
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = goalTitle,
                                                    color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = NewMePrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = goalDesc,
                                                color = NewMeTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                if (selectedProgressGoal == "Target weight") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = targetWeightText,
                                        onValueChange = { targetWeightText = it },
                                        placeholder = { Text("Target weight in kg (e.g. 68.0)", color = NewMeTextMuted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NewMePrimary,
                                            unfocusedBorderColor = NewMeBorder,
                                            focusedContainerColor = NewMeSurface,
                                            unfocusedContainerColor = NewMeSurface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 7: Real life fit
                    // ==========================================
                    6 -> {
                        item {
                            Text(
                                text = "Let's make this fit your real life.",
                                color = NewMeTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quick selections to schedule your daily companion rhythm.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Wake time
                            Text("Wake time", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                wakeTimeOptions.forEach { t ->
                                    FilterChip(
                                        selected = selectedWakeTime == t,
                                        onClick = { selectedWakeTime = t },
                                        label = { Text(t) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sleep time
                            Text("Typical sleep time", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                sleepTimeOptions.forEach { t ->
                                    FilterChip(
                                        selected = selectedSleepTime == t,
                                        onClick = { selectedSleepTime = t },
                                        label = { Text(t) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Work
                            Text("Work environment", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                workOptions.forEach { w ->
                                    FilterChip(
                                        selected = selectedWork == w,
                                        onClick = { selectedWork = w },
                                        label = { Text(w, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Training frequency
                            Text("Training frequency target", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                trainFreqOptions.forEach { f ->
                                    FilterChip(
                                        selected = selectedTrainFreq == f,
                                        onClick = { selectedTrainFreq = f },
                                        label = { Text(f, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Most difficult time of day
                            Text("Most difficult time of day to stay disciplined", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                difficultTimeOptions.forEach { d ->
                                    FilterChip(
                                        selected = selectedDifficultTime == d,
                                        onClick = { selectedDifficultTime = d },
                                        label = { Text(d, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 8: Food made easier
                    // ==========================================
                    7 -> {
                        item {
                            Text(
                                text = "Let's make food easier.",
                                color = NewMeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "We emphasize protein, sustainability, and food quality. No complicated calorie math.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Diet
                            Text("Dietary Preference", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                dietOptions.forEach { d ->
                                    FilterChip(
                                        selected = selectedDiet == d,
                                        onClick = { selectedDiet = d },
                                        label = { Text(d, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Common foods
                            Text("Common foods you eat (tap to select)", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Split across multi-line row layout
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                commonFoodList.chunked(4).forEach { rowChunk ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        rowChunk.forEach { food ->
                                            val isSel = selectedCommonFoods.contains(food)
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    selectedCommonFoods = if (isSel) selectedCommonFoods - food else selectedCommonFoods + food
                                                },
                                                label = { Text(food, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NewMePrimary,
                                                    selectedLabelColor = NewMePrimaryDark
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Cooking situation
                            Text("Cooking situation", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                cookingSituationOptions.forEach { c ->
                                    val isSel = selectedCooking == c
                                    Surface(
                                        color = if (isSel) NewMePrimary.copy(alpha = 0.12f) else NewMeSurface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSel) NewMePrimary else NewMeBorder),
                                        modifier = Modifier.fillMaxWidth().clickable { selectedCooking = c }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(c, color = if (isSel) NewMeTextPrimary else NewMeTextSecondary, fontSize = 13.sp)
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = NewMePrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Meal prep
                            Text("Meal prep habit", color = NewMePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                mealPrepOptions.forEach { p ->
                                    FilterChip(
                                        selected = selectedMealPrep == p,
                                        onClick = { selectedMealPrep = p },
                                        label = { Text(p, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NewMePrimary,
                                            selectedLabelColor = NewMePrimaryDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 9: How should NewMe talk to you?
                    // ==========================================
                    8 -> {
                        item {
                            Text(
                                text = "How should NewMe talk to you?",
                                color = NewMeTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose the voice of your companion. You can always change this in System.",
                                color = NewMeTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                toneOptions.forEach { (key, title, desc) ->
                                    val isSelected = selectedTone == key
                                    Surface(
                                        color = if (isSelected) NewMePrimary.copy(alpha = 0.15f) else NewMeSurface,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) NewMePrimary else NewMeBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedTone = key }
                                            .testTag("tone_$key")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = title,
                                                    color = if (isSelected) NewMeTextPrimary else NewMeTextSecondary,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = NewMePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\"$desc\"",
                                                color = NewMeTextMuted,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 10: Spoken companion
                    // ==========================================
                    9 -> {
                        item {
                            Text(
                                text = "Would you like NewMe to speak to you?",
                                color = NewMeTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NewMe can read morning anchors and evening reflections aloud with measured, calm cadence.",
                                color = NewMeTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    color = if (voiceChoice) NewMePrimary.copy(alpha = 0.15f) else NewMeSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (voiceChoice) NewMePrimary else NewMeBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { voiceChoice = true }
                                        .testTag("voice_option_yes")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔊", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Yes, talk to me",
                                                color = NewMeTextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Read reflections via Android TextToSpeech",
                                                color = NewMeTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (voiceChoice) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NewMePrimary)
                                        }
                                    }
                                }

                                Surface(
                                    color = if (!voiceChoice) NewMePrimary.copy(alpha = 0.15f) else NewMeSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (!voiceChoice) NewMePrimary else NewMeBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { voiceChoice = false }
                                        .testTag("voice_option_no")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📖", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Text is enough for now",
                                                color = NewMeTextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Read privately on screen without audio",
                                                color = NewMeTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (!voiceChoice) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NewMePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SCREEN 11: Commitment
                    // ==========================================
                    10 -> {
                        item {
                            Text(
                                text = "One thing before we begin.",
                                color = NewMeTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Surface(
                                color = NewMeSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, NewMeBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "You will have bad days.\n\nYou will miss workouts.\nYou will eat something you didn't plan.\nYou will lose motivation sometimes.\n\nNewMe isn't here to punish you for that.\n\nThe rule is simple:\n\nNever let one bad decision become two.",
                                        color = NewMeTextSecondary,
                                        fontSize = 15.sp,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = NewMeBorder)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "“You don't control everything that happens today. You do control what you do next.”",
                                        color = NewMePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = { saveAndLaunch() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("start_newme_journey_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "I UNDERSTAND — START MY NEWME JOURNEY",
                                    color = NewMePrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation action (for steps 0 to 9)
            if (step < totalSteps - 1) {
                Surface(
                    color = NewMeBackground.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { step++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_continue_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NewMePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Continue",
                                color = NewMePrimaryDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = NewMePrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
