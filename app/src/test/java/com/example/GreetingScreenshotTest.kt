package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelStore
import com.example.ui.NewMeApp
import com.example.ui.Screen
import com.example.ui.theme.NewMeTheme
import com.example.data.model.ThemeMode
import com.example.data.model.DailyGoalKind
import com.example.data.model.OnboardingPreferences
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.NewMeDatabase
import com.example.data.cycle.CycleRepository
import com.example.data.local.entity.CycleEntryEntity
import com.example.ui.components.CycleControls
import com.example.ui.components.ReminderControls
import com.example.viewmodel.NewMeViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun newme_app_screenshot() {
    composeTestRule.setContent { NewMeTheme { NewMeApp() } }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/newme_today.png")
  }

  @Test
  fun `both themes render real today state with enlarged text and bounded labels`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val model = NewMeViewModel(application)
    val store = ViewModelStore().apply { put("synthetic-screenshot", model) }
    runBlocking {
      model.repository.resetAllData()
      model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.TASKS, 3))
      model.repository.setFirstMorningWelcomeSeen()
    }
    val theme = mutableStateOf(ThemeMode.LIGHT)
    composeTestRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
        NewMeTheme(themeMode = theme.value, reducedMotion = true) {
          NewMeApp(viewModel = model, startDestination = Screen.Today.route)
        }
      }
    }
    try {
      composeTestRule.waitUntil(15_000) { model.userProfile.value?.hasSeenFirstMorningWelcome == true }
      for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
        composeTestRule.runOnIdle { theme.value = mode }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        val root = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val texts = composeTestRule.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
        texts.filter { it.boundsInRoot.overlaps(root) }.forEach { node ->
          assertTrue("Text escaped horizontal screen bounds", node.boundsInRoot.left >= root.left - 1f && node.boundsInRoot.right <= root.right + 1f)
        }
        val path = "src/test/screenshots/newme_${mode.name.lowercase()}_large.png"
        composeTestRule.onRoot().captureRoboImage(filePath = path)
        if (System.getProperty("roborazzi.test.record") == "true") {
          val image = android.graphics.BitmapFactory.decodeFile(path) ?: error("Screenshot was not generated.")
          assertTrue(image.width > 200 && image.height > 400)
          val pixels = mutableSetOf<Int>()
          for (sampleY in 0 until image.height step (image.height / 32).coerceAtLeast(1)) {
            for (sampleX in 0 until image.width step (image.width / 32).coerceAtLeast(1)) pixels.add(image.getPixel(sampleX, sampleY))
          }
          assertTrue("Rendered screenshot is blank", pixels.size > 3)
          image.recycle()
        }
      }
    } finally {
      store.clear()
    }
  }

  @Test
  fun `cycle opt out hides saved history and re enabling restores it`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val model = NewMeViewModel(application)
    val store = ViewModelStore().apply { put("synthetic-cycle", model) }
    val database = NewMeDatabase.getInstance(application)
    val cycle = CycleRepository(database)
    runBlocking {
      model.repository.resetAllData()
      model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User", sex = "Female"),
        OnboardingPreferences(DailyGoalKind.TASKS, 3, cycleEnabled = true))
      cycle.save(CycleEntryEntity(startDate = "2020-01-01", symptoms = "Synthetic private history"))
    }
    composeTestRule.setContent { NewMeTheme(reducedMotion = true) { CycleControls(model) } }
    try {
      composeTestRule.waitUntil(15_000) { model.cycleEntries.value.size == 1 }
      composeTestRule.onNodeWithText("Synthetic private history").assertIsDisplayed()
      composeTestRule.onNodeWithContentDescription("Cycle tracking").performClick()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(15_000) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        model.appSettings.value?.cycleEnabled == false && model.cycleEntries.value.isEmpty()
      }
      composeTestRule.onNodeWithText("Synthetic private history").assertDoesNotExist()
      composeTestRule.onNodeWithText("2020-01-01").assertDoesNotExist()
      assertTrue(runBlocking { database.cycleDao().getAll().size == 1 })
      composeTestRule.onNodeWithContentDescription("Cycle tracking").performClick()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      composeTestRule.waitForIdle()
      composeTestRule.waitUntil(15_000) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        model.cycleEntries.value.size == 1
      }
      composeTestRule.onNodeWithText("Synthetic private history").assertIsDisplayed()
    } finally { store.clear() }
  }

  @Test
  fun `reminder settings and quiet hours fit at large font scale in both themes`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val model = NewMeViewModel(application)
    val store = ViewModelStore().apply { put("synthetic-reminders", model) }
    val database = NewMeDatabase.getInstance(application)
    runBlocking {
      model.repository.resetAllData()
      model.repository.completeOnboarding(UserProfileEntity(name = "Synthetic User", sex = "Female"),
        OnboardingPreferences(DailyGoalKind.TASKS, 3, cycleEnabled = true))
      com.example.data.reminder.ReminderRepository(database).setTaskEnabled(true)
    }
    val theme = mutableStateOf(ThemeMode.LIGHT)
    org.robolectric.RuntimeEnvironment.setFontScale(1.8f)
    composeTestRule.setContent {
      NewMeTheme(themeMode = theme.value, reducedMotion = true) { ReminderControls(model) }
    }
    try {
      composeTestRule.waitUntil(15_000) { model.appSettings.value?.taskRemindersEnabled == true }
      for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
        composeTestRule.runOnIdle { theme.value = mode }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Task notifications").assertIsDisplayed()
        val root = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val texts = composeTestRule.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
        texts.filter { it.boundsInRoot.overlaps(root) }.forEach { node ->
          assertTrue("Reminder text escaped horizontal bounds", node.boundsInRoot.left >= root.left - 1f && node.boundsInRoot.right <= root.right + 1f)
        }
      }
      composeTestRule.onNodeWithContentDescription("Edit quiet hours").performClick()
      composeTestRule.onNodeWithText("Quiet Hours").assertIsDisplayed()
      composeTestRule.onNodeWithText("Cancel").performClick()
    } finally { store.clear() }
  }
}

