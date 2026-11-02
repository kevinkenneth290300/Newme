package com.example

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.ui.NewMeApp
import com.example.ui.NavigationRules
import com.example.ui.Screen
import com.example.ui.components.journey.CollapsibleSection
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.routines.RoutinesScreen
import com.example.ui.theme.NewMeTheme
import com.example.viewmodel.NewMeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class JourneyWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val database get() = NewMeDatabase.getInstance(application)
    private fun await(condition: () -> Boolean) = compose.waitUntil(15_000) {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        condition()
    }
    private fun model(): NewMeViewModel = NewMeViewModel(application)

    @Test
    fun `module tabs hide disabled routes and profile opens focused Settings`() {
        assertEquals(listOf(Screen.Today, Screen.Progress), NavigationRules.visible(""))
        val model = model()
        val store = ViewModelStore().apply { put("synthetic-navigation", model) }
        runBlocking {
            model.repository.resetAllData()
            model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.TASKS, 1))
            database.settingsDao().save(database.settingsDao().get()!!.copy(enabledModules = "ROUTINES", dashboardSetupDone = true, momentsEnabled = false))
        }
        compose.setContent { NewMeTheme(reducedMotion = true) { NewMeApp(model, Screen.Today.route) } }
        try {
            await { model.appSettings.value?.dashboardSetupDone == true }
            compose.onNodeWithTag("nav_routines").assertIsDisplayed()
            compose.onNodeWithTag("nav_plan").assertDoesNotExist()
            compose.onNodeWithTag("nav_gym").assertDoesNotExist()
            compose.onNodeWithTag("nav_system").assertDoesNotExist()
            compose.onNodeWithTag("profile_settings").performClick()
            compose.onNodeWithTag("settings_hub").assertIsDisplayed()
            compose.onNodeWithTag("settings_profile").performClick()
            compose.onNodeWithText(application.getString(R.string.edit_targets)).assertIsDisplayed()
        } finally { store.clear() }
    }

    @Test
    fun `routines show only due dates and collapse state restores`() {
        val model = model()
        val store = ViewModelStore().apply { put("synthetic-routines", model) }
        val today = LocalDate.now()
        runBlocking {
            model.repository.resetAllData()
            model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.TASKS, 1))
            database.routineDao().insert(RoutineTemplateEntity(title = "Due synthetic routine", startDate = today.toString()))
            database.routineDao().insert(RoutineTemplateEntity(title = "Future synthetic routine", startDate = today.plusDays(7).toString()))
        }
        compose.setContent { NewMeTheme(reducedMotion = true) { RoutinesScreen(model) } }
        try {
            await { model.routines.templates.value.size == 2 }
            compose.onNodeWithText("Due synthetic routine").assertIsDisplayed()
            compose.onNodeWithText("Future synthetic routine").assertDoesNotExist()
            compose.onNodeWithText(application.getString(R.string.filter_all)).performClick()
            compose.onNodeWithText("Future synthetic routine").performScrollTo().assertIsDisplayed()
        } finally { store.clear() }
    }

    @Test
    fun `collapsible state survives saved instance restoration`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { NewMeTheme(reducedMotion = true) { CollapsibleSection("restore", "Synthetic details") { androidx.compose.material3.Text("Saved section body") } } }
        compose.onNodeWithTag("section_restore").performClick()
        compose.onNodeWithText("Saved section body").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Saved section body").assertIsDisplayed()
    }

    @Test
    fun `seven step onboarding creates explicit profile without sample quests`() {
        runBlocking { com.example.data.repository.NewMeRepository(database).resetAllData() }
        val model = model()
        val store = ViewModelStore().apply { put("synthetic-onboarding", model) }
        compose.setContent { NewMeTheme(reducedMotion = true) { OnboardingScreen(model) {} } }
        try {
            compose.onNode(hasSetTextAction()).performTextReplacement("Synthetic User")
            repeat(6) { compose.onNodeWithTag("onboarding_next").performClick() }
            compose.onNodeWithTag("onboarding_next").performClick()
            await { model.userProfile.value?.isOnboarded == true }
            assertEquals("Synthetic User", runBlocking { database.userProfileDao().getUserProfileDirect()!!.name })
            assertTrue(runBlocking { database.routineDao().getAll().isEmpty() })
            assertTrue(runBlocking { database.dailyChecklistDao().getChecklistForDateDirect(LocalDate.now().toString()).isEmpty() })
            assertTrue(runBlocking { database.settingsDao().get()!!.dashboardSetupDone })
        } finally { store.clear() }
    }

    @Test
    fun `illustrated Moment answers update a checkin exactly once`() {
        val model = model()
        val store = ViewModelStore().apply { put("synthetic-moment", model) }
        runBlocking {
            model.repository.resetAllData()
            model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.TASKS, 1))
            database.settingsDao().save(database.settingsDao().get()!!.copy(dashboardSetupDone = true, momentTypes = "mood", momentsEnabled = true))
        }
        compose.setContent { NewMeTheme(reducedMotion = true) { NewMeApp(model, Screen.Today.route) } }
        try {
            await { model.moments.current.value != null }
            compose.onNodeWithTag("moment_host").assertIsDisplayed()
            compose.onNodeWithText(application.getString(R.string.mood_good)).performClick()
            compose.onNodeWithText(application.getString(R.string.save)).performClick()
            await { model.moments.current.value == null }
            assertEquals(20, runBlocking { database.userProfileDao().getUserProfileDirect()!!.currentXp })
            assertEquals(1, runBlocking { database.rewardDao().countMatching("checkin:%") })
        } finally { store.clear() }
    }
}