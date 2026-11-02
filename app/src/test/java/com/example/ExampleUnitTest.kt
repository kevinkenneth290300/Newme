package com.example

import com.example.data.backup.BackupCipher
import com.example.util.UiWriteQueue
import com.example.util.NutritionCalculator
import com.example.data.model.NutritionGoal
import com.example.data.repository.DailyProgressRules
import com.example.data.repository.MeasurementTrends
import com.example.data.local.entity.BodyMeasurementEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.companion.CompanionEngine
import com.example.data.companion.ActionType
import com.example.data.repository.DailyGoalProgress
import com.example.data.model.DailyGoalKind
import com.example.data.cycle.CycleRules
import com.example.data.cycle.CyclePrediction
import com.example.data.local.entity.CycleEntryEntity
import com.example.data.activity.TrackingRules
import com.example.util.DateSelection
import com.example.data.reminder.ReminderRules
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.DailyChecklistItemEntity
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.*
import org.junit.Test
import java.security.GeneralSecurityException

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `encrypted backup round trips and uses fresh randomness`() {
    val cipher = BackupCipher()
    val payload = "{\"fixture\":\"synthetic data only\"}".toByteArray()
    val password = "synthetic-test-password".toCharArray()
    val first = cipher.encrypt(payload, password)
    val second = cipher.encrypt(payload, password)
    assertFalse(first.contentEquals(second))
    assertArrayEquals(payload, cipher.decrypt(first, password))
    assertFalse(first.toString(Charsets.ISO_8859_1).contains("synthetic data only"))
  }

  @Test
  fun `wrong password and tampering reject encrypted backup`() {
    val cipher = BackupCipher()
    val password = "synthetic-test-password".toCharArray()
    val backup = cipher.encrypt("synthetic".toByteArray(), password)
    assertThrows(GeneralSecurityException::class.java) {
      cipher.decrypt(backup, "incorrect-test-password".toCharArray())
    }
    backup[backup.lastIndex] = (backup.last().toInt() xor 1).toByte()
    assertThrows(GeneralSecurityException::class.java) { cipher.decrypt(backup, password) }
  }

  @Test
  fun `short passwords and unknown backup formats are rejected`() {
    val cipher = BackupCipher()
    assertThrows(IllegalArgumentException::class.java) { cipher.encrypt(byteArrayOf(1), "short".toCharArray()) }
    assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(ByteArray(100), "synthetic-test-password".toCharArray()) }
  }

  @Test
  fun `backup barrier waits for pending writes and blocks new mutations`() = runTest {
    val errors = mutableListOf<String>()
    val queue = UiWriteQueue(this, errors::add)
    val ready = CompletableDeferred<Unit>()
    var saved = false
    queue.launch { ready.await(); saved = true }
    val barrier = async { queue.withMaintenance { assertTrue(saved) } }
    runCurrent()
    assertFalse(barrier.isCompleted)
    assertTrue(queue.isMaintaining)
    var blockedWriteRan = false
    queue.launch { blockedWriteRan = true }
    ready.complete(Unit)
    advanceUntilIdle()
    barrier.await()
    assertFalse(blockedWriteRan)
    assertFalse(queue.isMaintaining)
    assertEquals(1, errors.size)
  }

  @Test
  fun `write failures surface and cancellation is not treated as success`() = runTest {
    val errors = mutableListOf<String>()
    val queue = UiWriteQueue(this, errors::add)
    queue.launch { error("Synthetic write failure") }
    runCurrent()
    assertEquals(1, errors.size)
    val cancelled = queue.launch { awaitCancellation() }
    runCurrent()
    cancelled.cancelAndJoin()
    queue.awaitIdle()
    assertEquals(1, errors.size)
  }

  @Test
  fun `stable nutrition goals and legacy onboarding labels map to intended estimates`() {
    for (goal in listOf("Fat loss", "Fat-loss goal", NutritionGoal.FAT_LOSS.name)) {
      val targets = NutritionCalculator.calculateTargets(30, "Male", 170f, 70f, "Walking", goal)
      assertTrue(targets.calorieTarget < targets.tdee)
    }
    for (goal in listOf("Muscle gain", "Muscle goal", NutritionGoal.MUSCLE_GAIN.name)) {
      val targets = NutritionCalculator.calculateTargets(30, "Male", 170f, 70f, "Walking", goal)
      assertTrue(targets.calorieTarget > targets.tdee)
    }
    val targets = NutritionCalculator.calculateTargets(30, "Other", 170f, 70f, "Walking", "Target weight", 65f)
    assertTrue(targets.calorieTarget < targets.tdee)
    assertTrue(targets.explanation.contains("estimates"))
  }

  @Test
  fun `invalid or non finite nutrition input never becomes fictional defaults`() {
    assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.calculateTargets(0, "Male", 170f, 70f, "Walking", "Fat loss") }
    assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.calculateTargets(30, "Male", Float.NaN, 70f, "Walking", "Fat loss") }
    assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.calculateTargets(30, "Male", 170f, Float.POSITIVE_INFINITY, "Walking", "Fat loss") }
    assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.validateMetrics(30, 170f, 70f, bodyFatPercent = Float.NaN) }
    assertThrows(IllegalArgumentException::class.java) { NutritionCalculator.calculateTargets(30, "Male", 170f, 70f, "Walking", "Unsupported goal") }
  }

  @Test
  fun `unfinished current day does not break yesterday streak but missed yesterday does`() {
    val today = LocalDate.of(2020, 1, 10)
    val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
    assertEquals(3, DailyProgressRules.streak(today, days).current)
    assertEquals(4, DailyProgressRules.streak(today, days + today).current)
    assertEquals(0, DailyProgressRules.streak(today, days - today.minusDays(1)).current)
    assertEquals(3, DailyProgressRules.streak(today, days + today.plusDays(1)).longest)
  }

  @Test
  fun `date rollover follows today without moving intentionally selected history`() {
    val first = LocalDate.of(2020, 1, 10)
    val dates = DateSelection(first)
    assertEquals(first.plusDays(1), dates.refresh(first.plusDays(1)))
    dates.select(first.minusDays(2))
    assertEquals(first.minusDays(2), dates.refresh(first.plusDays(2)))
    dates.select(dates.today)
    assertEquals(first.plusDays(3), dates.refresh(first.plusDays(3)))
  }

  @Test
  fun `missing or same date measurements never manufacture body progress`() {
    val first = BodyMeasurementEntity(id = 1, date = "2020-01-01", weightKg = 70f)
    val second = BodyMeasurementEntity(id = 2, date = "2020-01-02", weightKg = 69f, skeletalMuscleKg = 30f)
    val trends = MeasurementTrends.from(listOf(first, second))
    assertEquals(-1f, trends.weight.change!!, 0.001f)
    assertNull(trends.muscle.change)
    assertNull(trends.bodyFat.current)
    val sameDate = MeasurementTrends.from(listOf(second, second.copy(id = 3, weightKg = 68f)))
    assertNull(sameDate.weight.change)
    val complete = MeasurementTrends.from(listOf(second, second.copy(id = 3, date = "2020-01-03", skeletalMuscleKg = 31f)))
    assertEquals(1f, complete.muscle.change!!, 0.001f)
  }

  @Test
  fun `local companion is deterministic and does not assume location or a workout plan`() {
    val profile = UserProfileEntity(name = "Synthetic User", selectedActivities = "WALKING", usesRecommendedPlan = false)
    val first = CompanionEngine.generateCompanionMessage(profile, currentHour = 19, rotationSeed = 123)
    val second = CompanionEngine.generateCompanionMessage(profile, currentHour = 19, rotationSeed = 123)
    assertEquals(first, second)
    assertFalse(first.greeting.contains("home", true))
    assertNotEquals(ActionType.START_WORKOUT, first.actionType)
    val goal = DailyGoalProgress(DailyGoalKind.TASKS, 3, 3, true)
    val met = CompanionEngine.generateCompanionMessage(profile, currentHour = 9, dailyGoal = goal)
    assertTrue(met.message.contains("goal is met"))
    val historical = CompanionEngine.generateCompanionMessage(profile, isHistorical = true)
    assertTrue(historical.message.contains("saved records"))
  }

  @Test
  fun `cycle estimates require three reliable starts and never invent future cycles`() {
    val today = LocalDate.of(2020, 3, 1)
    val regular = listOf("2020-01-01", "2020-01-29", "2020-02-26").map { CycleEntryEntity(startDate = it) }
    assertEquals(CyclePrediction.Insufficient, CycleRules.predict(regular.take(2), today))
    val estimate = CycleRules.predict(regular, today) as CyclePrediction.Estimate
    assertEquals(LocalDate.of(2020, 3, 25), estimate.date)
    assertEquals(28, estimate.medianDays)
    val irregular = listOf("2020-01-01", "2020-01-17", "2020-02-26").map { CycleEntryEntity(startDate = it) }
    assertEquals(CyclePrediction.Uncertain, CycleRules.predict(irregular, today))
    assertEquals(CyclePrediction.Uncertain, CycleRules.predict(regular, LocalDate.of(2020, 6, 1)))
  }

  @Test
  fun `cumulative step sensor readings baseline and reset without invented steps`() {
    assertEquals(0, TrackingRules.steps(-1, 5000.0)!!.added)
    assertEquals(7, TrackingRules.steps(5000, 5007.0)!!.added)
    assertEquals(0, TrackingRules.steps(5007, 2.0)!!.added)
    assertEquals(3, TrackingRules.steps(2, 5.0)!!.added)
    assertNull(TrackingRules.steps(5, Double.NaN))
  }

  @Test
  fun `timer interruptions are not counted and intervals split at local midnight`() {
    assertEquals(5000L, TrackingRules.credibleElapsed(1000, 6000))
    assertNull(TrackingRules.credibleElapsed(10_000, 1000))
    assertNull(TrackingRules.credibleElapsed(1000, 100_000))
    val end = java.time.Instant.parse("2020-01-02T00:00:05Z").toEpochMilli()
    val split = TrackingRules.splitDuration(end, 10_000, java.time.ZoneOffset.UTC)
    assertEquals(5000L, split["2020-01-01"])
    assertEquals(5000L, split["2020-01-02"])
  }

  @Test
  fun `reminders respect quiet hours local zones and delivered dates`() {
    val now = java.time.ZonedDateTime.parse("2020-01-10T21:00:00Z")
    val settings = AppSettingsEntity(taskRemindersEnabled = true)
    val reminder = ReminderEntity("daily", "DAILY", minuteOfDay = 1380, enabled = true)
    val first = ReminderRules.next(reminder, settings, now)!!
    assertEquals(java.time.Instant.parse("2020-01-11T07:00:00Z").toEpochMilli(), first.epochMillis)
    assertFalse(first.timeSensitive)
    val moved = ReminderRules.next(reminder, settings, now.withZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")))!!
    assertNotEquals(first.epochMillis, moved.epochMillis)
    val delivered = ReminderRules.next(reminder.copy(lastDeliveredToken = "2020-01-10"), settings, now)!!
    assertEquals("2020-01-11", delivered.token)
    assertNull(ReminderRules.next(reminder, settings.copy(taskRemindersEnabled = false), now))
    assertEquals(now, ReminderRules.afterQuietHours(now, 0, 0))
    assertEquals(18, ReminderRules.afterQuietHours(now.withHour(15), 13 * 60, 18 * 60).hour)
  }

  @Test
  fun `task completion and cycle opt out suppress reminders and cycle is never exact`() {
    val now = java.time.ZonedDateTime.parse("2020-03-01T08:00:00Z")
    val settings = AppSettingsEntity(taskRemindersEnabled = true, cycleEnabled = true, cycleRemindersEnabled = true)
    val task = DailyChecklistItemEntity(id = 7, date = "2020-03-01", section = "Test", title = "Synthetic")
    val reminder = ReminderEntity("task:7", "TASK", taskId = 7, enabled = true, timeSensitive = true)
    assertTrue(ReminderRules.next(reminder, settings, now, task)!!.timeSensitive)
    assertNull(ReminderRules.next(reminder, settings, now, task.copy(isCompleted = true)))
    assertNull(ReminderRules.next(reminder.copy(minuteOfDay = 1380), settings, now, task))
    val cycle = ReminderEntity("cycle", "CYCLE", enabled = true, timeSensitive = true)
    val estimate = CyclePrediction.Estimate(LocalDate.of(2020, 3, 25), 28, 2)
    assertFalse(ReminderRules.next(cycle, settings, now, prediction = estimate)!!.timeSensitive)
    assertNull(ReminderRules.next(cycle, settings.copy(cycleEnabled = false), now, prediction = estimate))
    assertNull(ReminderRules.next(cycle, settings.copy(cycleRemindersEnabled = false), now, prediction = estimate))
    assertNull(ReminderRules.next(cycle, settings, now, prediction = CyclePrediction.Uncertain))
  }

  @Test
  fun `routine recurrence handles weekdays weekly and clamped leap year month ends`() {
    val daily = com.example.data.local.entity.RoutineTemplateEntity(title = "Synthetic", startDate = "2020-01-01")
    assertTrue(com.example.data.routine.RoutineSchedule.isDue(daily, LocalDate.of(2020, 2, 29)))
    assertFalse(com.example.data.routine.RoutineSchedule.isDue(daily, LocalDate.of(2019, 12, 31)))
    val weekly = daily.copy(recurrence = "WEEKLY", daysOfWeekMask = 32)
    assertTrue(com.example.data.routine.RoutineSchedule.isDue(weekly, LocalDate.of(2020, 2, 29)))
    assertFalse(com.example.data.routine.RoutineSchedule.isDue(weekly, LocalDate.of(2020, 3, 1)))
    val weekdays = daily.copy(recurrence = "WEEKDAYS", daysOfWeekMask = 31)
    assertTrue(com.example.data.routine.RoutineSchedule.isDue(weekdays, LocalDate.of(2020, 3, 2)))
    assertFalse(com.example.data.routine.RoutineSchedule.isDue(weekdays, LocalDate.of(2020, 3, 1)))
    for (day in 29..31) {
      val monthly = daily.copy(recurrence = "MONTHLY", dayOfMonth = day)
      assertTrue(com.example.data.routine.RoutineSchedule.isDue(monthly, LocalDate.of(2020, 2, 29)))
      assertTrue(com.example.data.routine.RoutineSchedule.isDue(monthly, LocalDate.of(2021, 2, 28)))
    }
    assertEquals(LocalDate.of(2020, 3, 31), com.example.data.routine.RoutineSchedule.next(daily.copy(recurrence = "MONTHLY", dayOfMonth = 31), LocalDate.of(2020, 3, 1)))
  }

  @Test
  fun `shared day parts include all boundary hours and map night checkins to evening`() {
    assertEquals(com.example.data.model.DayPart.NIGHT, com.example.data.model.DayPart.at(3))
    assertEquals(com.example.data.model.DayPart.MORNING, com.example.data.model.DayPart.at(4))
    assertEquals(com.example.data.model.DayPart.MIDDAY, com.example.data.model.DayPart.at(12))
    assertEquals(com.example.data.model.DayPart.EVENING, com.example.data.model.DayPart.at(17))
    assertEquals("EVENING", com.example.data.model.DayPart.at(22).checkinPeriod)
  }

  @Test
  fun `training arc text palette maintains AA contrast in both themes`() {
    for ((text, background) in com.example.ui.theme.JourneyContrast.textPairs) {
      assertTrue("Insufficient text contrast", com.example.ui.theme.JourneyContrast.ratio(text, background) >= 4.5)
    }
  }

  @Test
  fun `moments cadence suppresses completed skipped tracking and permission states`() {
    val clock = java.time.Clock.fixed(java.time.Instant.parse("2020-03-01T08:00:00Z"), java.time.ZoneOffset.UTC)
    val settings = AppSettingsEntity(dashboardSetupDone = true)
    val first = com.example.data.moment.MomentRules.next(clock, com.example.data.model.DayPart.MORNING, settings, emptyList(), emptyList(), false, true)!!
    assertEquals("2020-03-01:MORNING", first.key)
    assertNull(com.example.data.moment.MomentRules.next(clock, com.example.data.model.DayPart.MORNING, settings, emptyList(), emptyList(), true, true))
    assertNull(com.example.data.moment.MomentRules.next(clock, com.example.data.model.DayPart.MORNING, settings, emptyList(), emptyList(), false, false))
    assertNull(com.example.data.moment.MomentRules.next(clock, com.example.data.model.DayPart.MORNING, settings, emptyList(), emptyList(), false, true, permissionOpen = true))
    var log = com.example.data.local.entity.MomentLogEntity(first.key, first.date, first.part, first.type, updatedAt = clock.millis())
    repeat(2) { log = com.example.data.moment.MomentRules.snoozed(log, clock.millis()); assertEquals("SNOOZED", log.status) }
    log = com.example.data.moment.MomentRules.snoozed(log, clock.millis())
    assertEquals("SKIPPED", log.status)
    assertNull(com.example.data.moment.MomentRules.next(clock, com.example.data.model.DayPart.MORNING, settings, emptyList(), listOf(log), false, true))
    assertFalse(com.example.data.moment.MomentRules.isNewOpen(599_999, 0, false))
    assertTrue(com.example.data.moment.MomentRules.isNewOpen(600_000, 0, false))
    assertTrue(com.example.data.moment.MomentRules.isNewOpen(0, null, true))
  }

  @Test
  fun `all twenty four badges have distinct ids and bounded truthful rule progress`() {
    val catalog = com.example.data.repository.BadgeCatalog.all
    assertEquals(24, catalog.size)
    assertEquals(24, catalog.map { it.id }.distinct().size)
    for (badge in catalog) {
      assertEquals(0, com.example.data.repository.BadgeValues(emptyMap()).current(badge))
      assertEquals(badge.target - 1, com.example.data.repository.BadgeValues(mapOf(badge.metric to badge.target - 1)).current(badge))
      assertEquals(badge.target, com.example.data.repository.BadgeValues(mapOf(badge.metric to badge.target + 5)).current(badge))
    }
    assertTrue(catalog.any { it.id == "seven_day_momentum" })
    assertTrue(catalog.any { it.id == "recovery_builder" })
  }

  @Test
  fun `celebration queue never repeats an unlocked badge`() {
    val queue = com.example.viewmodel.CelebrationQueue()
    queue.offer("first_step", com.example.viewmodel.JourneyCelebration(badgeId = "first_step"))
    queue.offer("first_step", com.example.viewmodel.JourneyCelebration(badgeId = "first_step"))
    queue.offer("level:2", com.example.viewmodel.JourneyCelebration(level = 2))
    assertEquals("first_step", queue.peek()?.badgeId)
    queue.remove()
    assertEquals(2, queue.peek()?.level)
    queue.remove()
    queue.offer("first_step", com.example.viewmodel.JourneyCelebration(badgeId = "first_step"))
    assertNull(queue.peek())
  }
}
