package com.example.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.*
import com.example.ui.components.journey.*
import com.example.util.NutritionCalculator
import com.example.viewmodel.NewMeViewModel
import com.example.ui.theme.NewMeTheme
import com.example.ui.components.notificationConsent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: NewMeViewModel, onLaunchToday: () -> Unit) {
    val initial = viewModel.userProfile.value
    val context = LocalContext.current
    val consent = notificationConsent(viewModel)
    val initialSettings = viewModel.appSettings.value
    val saving by viewModel.savingOnboarding.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var modules by rememberSaveable { mutableStateOf(initialSettings?.enabledModules.orEmpty()) }
    var age by rememberSaveable { mutableIntStateOf(initial?.age ?: 25) }
    var sex by rememberSaveable { mutableStateOf(initial?.sex ?: "Other") }
    var height by rememberSaveable { mutableFloatStateOf(initial?.heightCm ?: 170f) }
    var weight by rememberSaveable { mutableFloatStateOf(initial?.currentWeightKg ?: 70f) }
    var targetWeight by rememberSaveable { mutableFloatStateOf(initial?.targetWeightKg ?: weight) }
    var goal by rememberSaveable { mutableStateOf(initial?.primaryGoal?.let { runCatching { NutritionGoal.fromStored(it).name }.getOrDefault(NutritionGoal.GENERAL_HEALTH.name) } ?: NutritionGoal.GENERAL_HEALTH.name) }
    var dailyKind by rememberSaveable { mutableStateOf(initialSettings?.dailyGoalType ?: DailyGoalKind.TASKS.name) }
    var dailyTarget by rememberSaveable { mutableIntStateOf(initialSettings?.dailyGoalTarget ?: 3) }
    var packs by rememberSaveable { mutableStateOf("") }
    var mealIdeas by rememberSaveable { mutableStateOf(initial?.usesRecommendedPlan ?: false) }
    var cycle by rememberSaveable { mutableStateOf(initialSettings?.cycleEnabled ?: false) }
    var theme by rememberSaveable { mutableStateOf(initialSettings?.themeMode ?: ThemeMode.SYSTEM.name) }
    var reduced by rememberSaveable { mutableStateOf(initialSettings?.reducedMotion ?: false) }
    var haptics by rememberSaveable { mutableStateOf(initialSettings?.hapticsEnabled ?: false) }
    var voice by rememberSaveable { mutableStateOf(initial?.voiceEnabled ?: false) }
    var reminders by rememberSaveable { mutableStateOf(initialSettings?.taskRemindersEnabled ?: false) }
    var tone by rememberSaveable { mutableStateOf(initial?.companionTone ?: "BALANCED") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var disclaimer by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }
    val headings = listOf(R.string.setup_meet, R.string.setup_journey, R.string.setup_body, R.string.setup_goal, R.string.setup_streak, R.string.setup_packs, R.string.setup_look)
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
        LinearProgressIndicator(progress = { (step + 1) / 7f }, modifier = Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).verticalScroll(scroll).testTag("onboarding_step_$step"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PosterImage(if (step == 0) PosterSlot.WELCOME else if (step == 6) PosterSlot.LEVELUP else PosterSlot.MORNING, Modifier.fillMaxWidth().height(160.dp))
            Text(stringResource(headings[step]), style = MaterialTheme.typography.headlineMedium)
            when (step) {
                0 -> OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.your_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                1 -> ModuleChoices(modules.split(',').filter { it.isNotBlank() }.toSet(), { modules = it.sorted().joinToString(",") }, includeCycle = false)
                2 -> {
                    NumberStepper(stringResource(R.string.age_years), age.toFloat(), 18f..100f, 1f) { age = it.toInt() }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male" to R.string.sex_male, "Female" to R.string.sex_female, "Other" to R.string.sex_other).forEach { choice -> FilterChip(sex == choice.first, { sex = choice.first; if (sex != "Female") cycle = false }, label = { Text(stringResource(choice.second)) }) }
                    }
                    NumberStepper(stringResource(R.string.height_cm), height, 100f..250f, 1f) { height = it }
                    NumberStepper(stringResource(R.string.weight_kg), weight, 30f..300f, 0.5f) { weight = it }
                    if (sex == "Female") Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(cycle, { cycle = it }); Text(stringResource(R.string.cycle_optional), Modifier.weight(1f)) }
                }
                3 -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(NutritionGoal.GENERAL_HEALTH to R.string.goal_health, NutritionGoal.FAT_LOSS to R.string.goal_fat_loss, NutritionGoal.MUSCLE_GAIN to R.string.goal_muscle,
                            NutritionGoal.RECOMPOSITION to R.string.goal_recomposition, NutritionGoal.TARGET_WEIGHT to R.string.goal_weight).forEach { choice -> OptionTile(stringResource(choice.second), Icons.Default.Flag, goal == choice.first.name, { goal = choice.first.name }) }
                    }
                    if (goal == NutritionGoal.TARGET_WEIGHT.name) NumberStepper(stringResource(R.string.target_weight_kg), targetWeight, 30f..300f, 0.5f) { targetWeight = it }
                }
                4 -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(dailyKind == "TASKS", { dailyKind = "TASKS"; dailyTarget = 3 }, label = { Text(stringResource(R.string.goal_tasks)) })
                        FilterChip(dailyKind == "ACTIVITY_MINUTES", { dailyKind = "ACTIVITY_MINUTES"; dailyTarget = 20 }, label = { Text(stringResource(R.string.goal_minutes)) })
                    }
                    NumberStepper(stringResource(R.string.daily_target), dailyTarget.toFloat(), if (dailyKind == "TASKS") 1f..50f else 1f..1440f, 1f) { dailyTarget = it.toInt() }
                }
                5 -> {
                    StarterChoices(packs.split(',').filter { it.isNotBlank() }.toSet(), { packs = it.joinToString(",") })
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(mealIdeas, { mealIdeas = it }); Text(stringResource(R.string.meal_ideas), Modifier.weight(1f)) }
                }
                6 -> {
                    NewMeTheme(themeMode = ThemeMode.valueOf(theme), reducedMotion = true) {
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EmberMascot(size = 64.dp)
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode ->
                        FilterChip(theme == mode.name, { theme = mode.name }, label = { Text(stringResource(when (mode) { ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark; else -> R.string.theme_system })) })
                    } }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(reduced, { reduced = it }); Text(stringResource(R.string.reduced_motion), Modifier.weight(1f)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(haptics, { haptics = it }); Text(stringResource(R.string.haptics), Modifier.weight(1f)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(voice, { voice = it }); Text(stringResource(R.string.offline_voice), Modifier.weight(1f)) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("BALANCED" to R.string.tone_balanced, "CALM" to R.string.tone_calm, "DIRECT" to R.string.tone_direct).forEach { choice -> FilterChip(tone == choice.first, { tone = choice.first }, label = { Text(stringResource(choice.second)) }) } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(reminders, { enabled -> if (enabled) consent { reminders = true } else reminders = false })
                        Text(stringResource(R.string.setup_task_reminders), Modifier.weight(1f))
                    }
                }
            }
            Text(stringResource(R.string.short_disclaimer), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { disclaimer = true }) { Text(stringResource(R.string.learn_more)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) TextButton(enabled = !saving, onClick = { step--; error = null }) { Text(stringResource(R.string.back)) }
            Button(enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("onboarding_next"), onClick = {
                try {
                    if (step == 0) require(name.trim().isNotEmpty() && name.length <= 80)
                    if (step == 2) NutritionCalculator.validateMetrics(age, height, weight)
                    if (step == 4) DailyGoalKind.valueOf(dailyKind).validate(dailyTarget)
                    if (step < 6) { step++; error = null }
                    else {
                        val selected = modules.split(',').filter { it.isNotBlank() }.toSet() + if (cycle && sex == "Female") setOf("CYCLE") else emptySet()
                        val activity = initial?.primaryActivityType?.let(ActivityKind::fromStored) ?: if ("GYM" in selected) ActivityKind.GYM else if ("ACTIVITY" in selected) ActivityKind.WALKING else ActivityKind.NONE
                        val targets = NutritionCalculator.calculateTargets(age, sex, height, weight, activity.name, goal, if (goal == "TARGET_WEIGHT") targetWeight else null)
                        val profile = (initial ?: UserProfileEntity(workType = "", commonFoods = "", identityTraits = "", startingReason = "")).copy(name = name.trim(), age = age, sex = sex, heightCm = height,
                            currentWeightKg = weight, initialWeightKg = initial?.initialWeightKg ?: weight, targetWeightKg = if (goal == "TARGET_WEIGHT") targetWeight else initial?.targetWeightKg,
                            primaryGoal = goal, primaryActivityType = activity.name, selectedActivities = initial?.selectedActivities?.takeIf { it.isNotBlank() } ?: if (activity != ActivityKind.NONE) activity.name else "",
                            bmr = targets.bmr, tdee = targets.tdee, calorieTarget = targets.calorieTarget, proteinTargetGrams = targets.proteinGrams, carbsTargetGrams = targets.carbsGrams,
                            fatTargetGrams = targets.fatGrams, waterTargetLiters = targets.waterLiters, usesRecommendedPlan = mealIdeas, voiceEnabled = voice, companionTone = tone)
                        viewModel.completeJourneyOnboarding(profile, OnboardingPreferences(DailyGoalKind.valueOf(dailyKind), dailyTarget, cycle && sex == "Female"), selected,
                            packs.split(',').filter { it.isNotBlank() }.toSet(), ThemeMode.valueOf(theme), reduced, haptics, reminders, onLaunchToday)
                    }
                } catch (failure: IllegalArgumentException) { error = failure.message ?: context.getString(R.string.invalid_setup) }
            }) { Text(stringResource(if (saving) R.string.saving else if (step == 6) R.string.begin_journey else R.string.next)) }
        }
    }
    if (disclaimer) AlertDialog(onDismissRequest = { disclaimer = false }, title = { Text(stringResource(R.string.privacy_health)) }, text = { Text(stringResource(R.string.setup_disclaimer_detail)) },
        confirmButton = { TextButton(onClick = { disclaimer = false }) { Text(stringResource(R.string.done)) } })
}