package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode
import com.example.data.repository.Levels
import com.example.ui.components.journey.*
import com.example.ui.theme.NewMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h851dp-xxhdpi")
class JourneyScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `visual kit renders both themes large fonts and reduced motion`() {
        RuntimeEnvironment.setFontScale(1.5f)
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val group = mutableStateOf(0)
        compose.setContent {
            NewMeTheme(themeMode = theme.value, reducedMotion = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("kit"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (group.value) {
                        0 -> {
                            Row { EmberExpression.entries.forEach { EmberMascot(EmberStage.FLAME, it, 62.dp) } }
                            PosterImage(PosterSlot.MORNING, Modifier.fillMaxWidth().height(160.dp))
                            QuestCard("Synthetic quest", "Routine +10 XP", "sun", false, {})
                            QuestCard("Completed quest", "Routine +10 XP", "water", true, {})
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OptionTile("Morning", Icons.Default.WbSunny, true, {})
                                GoalRing(2, 3)
                            }
                            StreakFlame(3)
                            LevelBar(Levels.info(1240))
                            CollapsibleSection("sample", "Details", initiallyExpanded = true) { StatChip("Saved state", Icons.Default.WbSunny) }
                        }
                        1 -> {
                            WaterBottle(500, 2000, {})
                            SleepMoon(450)
                            MoodFaces("Okay", {})
                            ProteinIconGrid { _, _ -> }
                            EmptyState("No routines", "Nothing due today", PosterSlot.EMPTY_ROUTINES, "Add my own", {})
                            ConfettiBurst(true)
                        }
                        else -> {
                            for (row in 0..7) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (column in 0..2) {
                                    val index = row * 3 + column
                                    BadgeEmblem(index, index / 5, index % 4, index % 3 != 0, 0.5f, label = "Synthetic badge $index")
                                }
                            }
                        }
                    }
                }
            }
        }
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (index in 0..2) {
                compose.runOnIdle { theme.value = mode; group.value = index }
                compose.waitForIdle()
                val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
                assertTrue(root.width > 200f && root.height > 400f)
                compose.onRoot().captureRoboImage("build/outputs/roborazzi/kit_${mode.name.lowercase()}_$index.png")
            }
        }
    }

    @Test
    fun `mascot evolution keeps resting and cheerful states distinct`() {
        assertEquals(EmberStage.SPARK, EmberStage.forLevel(2))
        assertEquals(EmberStage.EMBER, EmberStage.forLevel(4))
        assertEquals(EmberStage.FLAME, EmberStage.forLevel(6))
        assertEquals(EmberStage.BLAZE, EmberStage.forLevel(8))
        assertEquals(EmberStage.NOVA, EmberStage.forLevel(9))
        assertEquals(24, JourneyIcons.badges.distinct().size)
    }
}