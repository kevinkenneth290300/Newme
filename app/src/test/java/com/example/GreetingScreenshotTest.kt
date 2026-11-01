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
}

