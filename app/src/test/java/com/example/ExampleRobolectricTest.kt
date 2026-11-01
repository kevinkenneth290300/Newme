package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupStore
import com.example.data.local.NewMeDatabase
import com.example.data.local.DatabaseMigrations
import com.example.data.local.entity.*
import com.example.data.model.DailyGoalKind
import com.example.data.model.OnboardingPreferences
import com.example.data.repository.NewMeRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.DailyProgressRepository
import com.example.data.repository.DailyProgressRules
import com.example.data.cycle.CycleRepository
import com.example.data.activity.ActivityTrackingRepository
import com.example.data.activity.ActivityTrackingService
import com.example.data.model.ActivityKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NewMe", appName)
  }

  @Test
  fun `level info calculation progress`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = NewMeDatabase.getInstance(context)
    val repo = NewMeRepository(db)

    val info1 = repo.calculateLevelInfo(1240)
    assertEquals(3, info1.level)
    assertEquals(1200, info1.currentLevelBaseXp)
    assertEquals(2000, info1.nextLevelTargetXp)
    assertTrue(info1.progress in 0.04f..0.06f)
  }

  @Test
  fun `completeOnboarding persists metrics and sets isOnboarded true`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = NewMeRepository(db)

    val customProfile = com.example.data.local.entity.UserProfileEntity(
      id = 1,
      name = "Marcus",
      age = 30,
      sex = "Male",
      heightCm = 180f,
      initialWeightKg = 82.5f,
      currentWeightKg = 82.5f,
      bodyFatPercent = 18.5f,
      skeletalMuscleKg = 38.0f,
      proteinTargetGrams = 165,
      waterTargetLiters = 3.2f,
      sleepTargetHours = 8.0f,
      weeklyWorkoutsTarget = 5,
      goalDescription = "Hypertrophy & Raw Strength",
      startDate = java.time.LocalDate.now().toString(),
      isOnboarded = false
    )

    repo.completeOnboarding(customProfile)

    val savedProfile = db.userProfileDao().getUserProfileDirect()
    org.junit.Assert.assertNotNull(savedProfile)
    assertEquals("Marcus", savedProfile?.name)
    assertEquals(82.5f, savedProfile?.currentWeightKg ?: 0f, 0.01f)
    assertEquals(165, savedProfile?.proteinTargetGrams)
    assertEquals(3.2f, savedProfile?.waterTargetLiters ?: 0f, 0.01f)
    assertEquals(true, savedProfile?.isOnboarded)

    // Verify baseline body measurement was persisted
    val today = java.time.LocalDate.now().toString()
    val measurement = db.bodyMeasurementDao().getMeasurementForDateDirect(today)
    org.junit.Assert.assertNotNull(measurement)
    assertEquals(82.5f, measurement?.weightKg ?: 0f, 0.01f)
    assertEquals("Onboarding baseline", measurement?.notes)
    db.close()
  }

  @Test
  fun `recalibration preserves earned progress and historical records`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val original = db.userProfileDao().getUserProfileDirect()!!.copy(
        currentXp = 1240, currentLevel = 3, currentStreak = 4, longestStreak = 9,
        perfectDays = 6, hasSeenFirstMorningWelcome = true
      )
      db.userProfileDao().insertOrUpdateProfile(original)
      val today = java.time.LocalDate.now().toString()
      val item = db.dailyChecklistDao().getChecklistForDateDirect(today).first()
      db.dailyChecklistDao().setCompleted(item.id, true, "08:00 AM")
      db.bodyMeasurementDao().insertMeasurement(BodyMeasurementEntity(date = "2020-01-01", weightKg = 72f))
      repo.resetOnboarding()
      repo.completeOnboarding(UserProfileEntity(name = "Updated User", currentWeightKg = 68f, proteinTargetGrams = 110))

      val saved = db.userProfileDao().getUserProfileDirect()!!
      assertEquals(1240, saved.currentXp)
      assertEquals(3, saved.currentLevel)
      assertEquals(4, saved.currentStreak)
      assertEquals(9, saved.longestStreak)
      assertEquals(original.startDate, saved.startDate)
      assertEquals(original.initialWeightKg, saved.initialWeightKg, 0.01f)
      assertEquals(110, saved.proteinTargetGrams)
      assertTrue(saved.hasSeenFirstMorningWelcome)
      assertTrue(db.dailyChecklistDao().getChecklistForDateDirect(today).first().isCompleted)
      assertEquals(2, db.bodyMeasurementDao().getAllMeasurements().first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `onboarding retry is idempotent and days wait for onboarding`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      val today = java.time.LocalDate.now().toString()
      repo.ensureDateInitialized(today)
      assertTrue(db.dailyChecklistDao().getChecklistForDateDirect(today).isEmpty())
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val count = db.dailyChecklistDao().getChecklistForDateDirect(today).size
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      assertEquals(count, db.dailyChecklistDao().getChecklistForDateDirect(today).size)
      assertEquals(1, db.bodyMeasurementDao().getAllMeasurements().first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `reset clears every user table and returns to absent profile`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val today = java.time.LocalDate.now().toString()
      db.waterDao().insertOrUpdateWaterLog(WaterLogEntity(today, 500))
      db.dailyCheckinDao().insertOrUpdateCheckin(DailyCheckinEntity(date = today, timeOfDay = "MORNING"))
      db.activitySessionDao().insertActivitySession(ActivitySessionEntity(date = today, activityType = "Walking", title = "Test walk", durationMinutes = 10))
      db.friendConnectionDao().insertFriend(FriendConnectionEntity(friendCode = "LOCAL", friendName = "Test entry"))
      repo.resetAllData()
      org.junit.Assert.assertNull(db.userProfileDao().getUserProfileDirect())
      val tables = mutableListOf<String>()
      db.openHelper.readableDatabase.query(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table', 'android_metadata')"
      ).use { cursor -> while (cursor.moveToNext()) tables.add(cursor.getString(0)) }
      for (table in tables) {
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
          assertTrue(cursor.moveToFirst())
          assertEquals("Reset left records in $table", 0, cursor.getInt(0))
        }
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `encrypted backup restores every user table from a consistent snapshot`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      db.waterDao().insertOrUpdateWaterLog(WaterLogEntity("2020-01-01", 1234))
      db.sleepDao().insertOrUpdateSleepLog(SleepLogEntity("2020-01-01", durationMinutes = 420))
      db.dailyCheckinDao().insertOrUpdateCheckin(DailyCheckinEntity(date = "2020-01-01", timeOfDay = "EVENING", mattersMost = "Synthetic intention"))
      db.activitySessionDao().insertActivitySession(ActivitySessionEntity(date = "2020-01-01", activityType = "Cycling", title = "Synthetic ride", durationMinutes = 20))
      val store = BackupStore(db)
      val password = "synthetic-test-password".toCharArray()
      val encrypted = store.export(password)
      val prepared = store.prepare(encrypted, password)
      assertTrue(prepared.recordCount > 0)
      repo.resetAllData()
      store.restore(prepared)
      val restored = store.prepare(store.export(password), password)
      assertEquals(prepared.tables, restored.tables)
      assertEquals("Synthetic User", db.userProfileDao().getUserProfileDirect()?.name)
    } finally {
      db.close()
    }
  }

  @Test
  fun `preparing cancelling or rejecting restore never changes current records`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val original = db.userProfileDao().getUserProfileDirect()
      val store = BackupStore(db)
      val password = "synthetic-test-password".toCharArray()
      val encrypted = store.export(password)
      store.prepare(encrypted, password)
      assertEquals(original, db.userProfileDao().getUserProfileDirect())
      var rejected = false
      try { store.prepare(encrypted, "incorrect-test-password".toCharArray()) }
      catch (error: java.io.IOException) { rejected = true }
      assertTrue(rejected)
      assertEquals(original, db.userProfileDao().getUserProfileDirect())
      encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
      rejected = false
      try { store.prepare(encrypted, password) }
      catch (error: java.io.IOException) { rejected = true }
      assertTrue(rejected)
      assertEquals(original, db.userProfileDao().getUserProfileDirect())
      org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
        BackupStore.requireLocalDocument(android.net.Uri.parse("content://cloud.example/synthetic.newme"))
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `version four migration preserves every original table and value`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val schema = javaClass.classLoader!!.getResourceAsStream("com.example.data.local.NewMeDatabase/4.json")!!
      .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
    val entities = schema.getJSONArray("entities")
    val name = "synthetic-migration.db"
    context.deleteDatabase(name)
    val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
      androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
          override fun onCreate(sql: androidx.sqlite.db.SupportSQLiteDatabase) {
            for (index in 0 until entities.length()) {
              val entity = entities.getJSONObject(index)
              val table = entity.getString("tableName")
              sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
              val fields = entity.getJSONArray("fields")
              val columns = List(fields.length()) { fields.getJSONObject(it).getString("columnName") }
              val values = List(fields.length()) { fieldIndex ->
                val field = fields.getJSONObject(fieldIndex)
                val column = field.getString("columnName")
                when {
                  column == "currentXp" -> 1240
                  column == "currentLevel" -> 3
                  column == "currentStreak" -> 4
                  column == "longestStreak" -> 9
                  column == "age" -> 30
                  column == "heightCm" -> 170.0
                  column.contains("WeightKg") || column == "weightKg" -> 70.5
                  column == "date" || column == "prepDate" || column == "startDate" -> "2020-01-01"
                  column == "timeOfDay" -> "MORNING"
                  column == "source" -> "Manual"
                  !field.optBoolean("notNull") -> null
                  field.getString("affinity") == "INTEGER" -> 1
                  field.getString("affinity") == "REAL" -> 2.5
                  else -> "synthetic_$column"
                }
              }
              val names = columns.joinToString(",") { "`$it`" }
              val placeholders = columns.joinToString(",") { "?" }
              sql.execSQL("INSERT INTO `$table` ($names) VALUES ($placeholders)", values.toTypedArray())
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) sql.execSQL(setup.getString(index))
          }
          override fun onUpgrade(sql: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()
    )
    val before = (0 until entities.length()).associate { index ->
      val table = entities.getJSONObject(index).getString("tableName")
      table to snapshotRows(helper.writableDatabase, table)
    }
    helper.close()
    val db = androidx.room.Room.databaseBuilder(context, NewMeDatabase::class.java, name)
      .addMigrations(DatabaseMigrations.MIGRATION_4_5).allowMainThreadQueries().build()
    try {
      val sql = db.openHelper.writableDatabase
      assertEquals(5, sql.version)
      for ((table, rows) in before) assertEquals("Changed records in $table", rows, snapshotRows(sql, table))
      assertEquals(1240, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(4, db.userProfileDao().getUserProfileDirect()?.currentStreak)
      assertEquals(0, db.rewardDao().count())
      assertTrue(db.rewardDao().isExcluded("task:1"))
      assertEquals(false, db.dailyStateDao().get("2020-01-01")?.targetsKnown)
    } finally {
      db.close()
      context.deleteDatabase(name)
    }
  }

  @Test
  fun `intentionally emptied day remains empty and initialization is concurrent safe`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val date = java.time.LocalDate.now().toString()
      val count = db.dailyChecklistDao().getChecklistForDateDirect(date).size
      (1..8).map { async { repo.ensureDateInitialized(date) } }.awaitAll()
      assertEquals(count, db.dailyChecklistDao().getChecklistForDateDirect(date).size)
      db.dailyChecklistDao().clearChecklistForDate(date)
      db.nutritionDao().getMealsForDateDirect(date).forEach { db.nutritionDao().deleteMeal(it) }
      repo.ensureDateInitialized(date)
      assertTrue(db.dailyChecklistDao().getChecklistForDateDirect(date).isEmpty())
      assertTrue(db.nutritionDao().getMealsForDateDirect(date).isEmpty())
      org.junit.Assert.assertNotNull(db.dailyStateDao().get(date))
    } finally {
      db.close()
    }
  }

  private fun snapshotRows(sql: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<List<String?>> = buildList {
    sql.query("SELECT * FROM `$table` ORDER BY rowid").use { cursor ->
      while (cursor.moveToNext()) add(List(cursor.columnCount) { if (cursor.isNull(it)) null else cursor.getString(it) })
    }
  }

  @Test
  fun `onboarding saves explicit goals and optional cycle choice without invented meals`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User", sex = "Female", usesRecommendedPlan = false),
        OnboardingPreferences(DailyGoalKind.ACTIVITY_MINUTES, 20, cycleEnabled = true))
      val settings = db.settingsDao().get()!!
      assertTrue(settings.goalChosen)
      assertTrue(settings.cycleEnabled)
      assertFalse(settings.cycleRemindersEnabled)
      val today = java.time.LocalDate.now().toString()
      assertEquals("ACTIVITY_MINUTES", db.dailyStateDao().get(today)?.goalType)
      assertEquals(20, db.dailyStateDao().get(today)?.goalTarget)
      assertTrue(db.nutritionDao().getMealsForDateDirect(today).isEmpty())
      assertTrue(db.activitySessionDao().getActivitySessionsForDateDirect(today).isEmpty())
      repo.completeOnboarding(db.userProfileDao().getUserProfileDirect()!!.copy(sex = "Other"),
        OnboardingPreferences(DailyGoalKind.TASKS, 2, cycleEnabled = false))
      assertFalse(db.settingsDao().get()!!.cycleEnabled)
    } finally {
      db.close()
    }
  }

  @Test
  fun `checklist retries undo and redo never duplicate earned XP`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val date = java.time.LocalDate.now().toString()
      val item = db.dailyChecklistDao().getChecklistForDateDirect(date).first()
      (1..12).map { async { repo.toggleChecklistItem(item) } }.awaitAll()
      val once = db.userProfileDao().getUserProfileDirect()!!.currentXp
      assertEquals(item.xpReward, once)
      repo.toggleChecklistItem(db.dailyChecklistDao().getItem(item.id)!!)
      repo.toggleChecklistItem(db.dailyChecklistDao().getItem(item.id)!!)
      assertEquals(once, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(1, db.rewardDao().count())
    } finally {
      db.close()
    }
  }

  @Test
  fun `concurrent unique rewards and stale profile updates preserve every XP increment`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val stale = db.userProfileDao().getUserProfileDirect()!!
      val game = GamificationRepository(db)
      val date = java.time.LocalDate.now().toString()
      (1..20).map { index -> async { game.reward("synthetic:$index", date, 10) } }.awaitAll()
      (1..8).map { async { game.reward("synthetic:1", date, 10) } }.awaitAll()
      repo.saveProfile(stale.copy(name = "Updated User"))
      assertEquals(200, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(20, db.rewardDao().count())
    } finally {
      db.close()
    }
  }

  @Test
  fun `meal sleep workout and checkin resaves earn each reward once`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val date = java.time.LocalDate.now().toString()
      val mealId = repo.addMeal(MealPlanEntity(date = date, slot = "Lunch", name = "Synthetic meal", time = "12:00", foodsDescription = "Synthetic", approxCalories = 400, proteinGrams = 20, carbsGrams = 40, fatsGrams = 10))
      val meal = db.nutritionDao().getMeal(mealId)!!
      repeat(5) { repo.toggleMealCompleted(meal) }
      repo.toggleMealCompleted(db.nutritionDao().getMeal(mealId)!!)
      repo.toggleMealCompleted(db.nutritionDao().getMeal(mealId)!!)
      repeat(5) { repo.logSleep(date, "22:00", "06:00", 480, 4) }
      repeat(5) { repo.saveWorkoutSession(WorkoutSessionEntity(date = date, durationMinutes = 45, isCompleted = true)) }
      (1..6).map { async { repo.saveDailyCheckin(DailyCheckinEntity(date = date, timeOfDay = "MORNING", mattersMost = "Synthetic intention")) } }.awaitAll()
      assertEquals(80, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(4, db.rewardDao().count())
      assertEquals(1, db.dailyCheckinDao().getAllCheckinsForDate(date).first().size)
      db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM workout_sessions WHERE date = '$date'").use {
        assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `concurrent water increments are atomic and undo cannot repeat the target bonus`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User", waterTargetLiters = 2.5f))
      val date = java.time.LocalDate.now().toString()
      (1..30).map { async { repo.addWater(date, 100) } }.awaitAll()
      assertEquals(3000, db.waterDao().getLog(date)?.amountMl)
      val once = db.userProfileDao().getUserProfileDirect()!!.currentXp
      assertEquals(15, once)
      repo.setWater(date, 0)
      repo.setWater(date, 3000)
      assertEquals(once, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(1, db.rewardDao().count())
    } finally {
      db.close()
    }
  }

  @Test
  fun `recovery choices never invent completion hydration streaks or XP`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val date = java.time.LocalDate.now().toString()
      repeat(3) { repo.applyRecoveryPlan(date, "Synthetic obstacle", "Synthetic plan") }
      repo.restoreMomentum()
      assertEquals(0, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(0, db.userProfileDao().getUserProfileDirect()?.currentStreak)
      assertFalse(db.recoveryDao().getLog(date)!!.isCompleted)
      org.junit.Assert.assertNull(db.waterDao().getLog(date))
    } finally {
      db.close()
    }
  }

  @Test
  fun `welcome intention saves once and shown reflections never earn XP`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      val date = java.time.LocalDate.now().toString()
      repeat(3) { repo.recordReflectionShown("synthetic-reflection", date, "MORNING") }
      assertEquals(0, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(1, db.reflectionHistoryDao().getRecentReflectionIds().size)
      repeat(3) { repo.saveWelcomeIntention("Calm", "Synthetic intention") }
      assertEquals(20, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals("Synthetic intention", db.dailyCheckinDao().getCheckinDirect(date, "MORNING")?.mattersMost)
      assertTrue(db.userProfileDao().getUserProfileDirect()!!.hasSeenFirstMorningWelcome)
      assertEquals(1, db.dailyCheckinDao().getAllCheckinsForDate(date).first().size)
    } finally {
      db.close()
    }
  }

  @Test
  fun `goal changes preserve previous day snapshots and achievements unlock once`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val clock = java.time.Clock.fixed(java.time.Instant.parse("2020-01-10T12:00:00Z"), java.time.ZoneOffset.UTC)
      val repo = NewMeRepository(db, clock)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.TASKS, 1))
      val today = "2020-01-10"
      val yesterday = "2020-01-09"
      val original = db.dailyStateDao().get(today)!!.copy(date = yesterday)
      db.dailyStateDao().insert(original)
      val item = db.dailyChecklistDao().getChecklistForDateDirect(today).first()
      repo.toggleChecklistItem(item)
      val game = GamificationRepository(db, clock)
      game.refresh()
      val first = db.achievementDao().getAll().first { it.id == "first_day" }
      assertTrue(first.isUnlocked)
      game.refresh()
      assertEquals(first, db.achievementDao().getAll().first { it.id == "first_day" })
      repo.setDailyGoal(DailyGoalKind.ACTIVITY_MINUTES, 20)
      assertEquals(original, db.dailyStateDao().get(yesterday))
      assertEquals("ACTIVITY_MINUTES", db.dailyStateDao().get(today)?.goalType)
      val progress = DailyProgressRepository(db)
      val before = DailyProgressRules.score(progress.records(yesterday))
      db.mealPrepDao().insertMealPrepItem(MealPrepItemEntity(foodName = "Synthetic prep", portionsAvailable = 2, prepDate = today))
      val after = DailyProgressRules.score(progress.records(yesterday))
      assertEquals(before, after)
      assertEquals(0, after.mealPrepMax)
      assertEquals(0, after.supplementMax)
    } finally {
      db.close()
    }
  }

  @Test
  fun `cycle edits opt out backup and reset preserve privacy and real entries`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java)
      .allowMainThreadQueries().build()
    try {
      val repo = NewMeRepository(db)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User", sex = "Female"))
      val cycle = CycleRepository(db)
      cycle.enabled(true)
      cycle.save(CycleEntryEntity(startDate = "2020-01-01", endDate = "2020-01-05", symptoms = "Synthetic optional symptom"))
      cycle.save(CycleEntryEntity(startDate = "2020-01-01", endDate = "2020-01-06", symptoms = "Edited synthetic symptom"))
      assertEquals(1, db.cycleDao().getAll().size)
      cycle.enabled(false)
      assertEquals(1, db.cycleDao().getAll().size)
      assertFalse(db.settingsDao().get()!!.cycleRemindersEnabled)
      val expected = db.cycleDao().getAll()
      val store = BackupStore(db)
      val password = "synthetic-test-password".toCharArray()
      val prepared = store.prepare(store.export(password), password)
      repo.resetAllData()
      assertTrue(db.cycleDao().getAll().isEmpty())
      store.restore(prepared)
      assertEquals(expected, db.cycleDao().getAll())
      assertFalse(db.settingsDao().get()!!.cycleRemindersEnabled)
      cycle.delete(expected.first())
      assertTrue(db.cycleDao().getAll().isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun `one tracked session pauses resumes stops and rewards exactly once`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
    try {
      val clock = java.time.Clock.fixed(java.time.Instant.parse("2020-01-10T12:00:00Z"), java.time.ZoneOffset.UTC)
      val repo = NewMeRepository(db, clock)
      repo.completeOnboarding(UserProfileEntity(name = "Synthetic User"), OnboardingPreferences(DailyGoalKind.ACTIVITY_MINUTES, 1))
      var elapsed = 1000L
      val tracker = ActivityTrackingRepository(db, clock) { elapsed }
      val track = tracker.start(ActivityKind.WALKING, false)
      var rejected = false
      try { tracker.start(ActivityKind.CYCLING, false) } catch (failure: IllegalStateException) { rejected = true }
      assertTrue(rejected)
      repeat(3) { elapsed += 20_000; tracker.tick() }
      tracker.pause()
      val paused = db.activityTrackingDao().getActive()!!
      elapsed += 100_000
      tracker.tick()
      assertEquals(paused.elapsedMillis, db.activityTrackingDao().getActive()?.elapsedMillis)
      tracker.resume()
      elapsed += 5000
      tracker.steps(5000.0)
      tracker.steps(5005.0)
      val finished = tracker.finish()!!
      assertEquals(65_000L, finished.elapsedMillis)
      assertEquals(5, finished.steps)
      org.junit.Assert.assertNull(db.activityTrackingDao().getActive())
      org.junit.Assert.assertNull(tracker.finish())
      assertEquals(25, db.userProfileDao().getUserProfileDirect()?.currentXp)
      assertEquals(1, db.userProfileDao().getUserProfileDirect()?.currentStreak)
      assertEquals(1, DailyProgressRules.activityMinutes(DailyProgressRepository(db).records("2020-01-10").movement))
      tracker.deleteFinished(track.sessionKey)
      assertEquals(0L, db.activityTrackingDao().getMillis("2020-01-10"))
      assertTrue(db.activitySessionDao().getActivitySessionsForDateDirect("2020-01-10").isEmpty())
      assertEquals(25, db.userProfileDao().getUserProfileDirect()?.currentXp)
    } finally { db.close() }
  }

  @Test
  fun `interrupted tracker and restored active session stay paused without guessed duration`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
    try {
      val clock = java.time.Clock.fixed(java.time.Instant.parse("2020-01-10T12:00:00Z"), java.time.ZoneOffset.UTC)
      NewMeRepository(db, clock).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      var elapsed = 1000L
      val tracker = ActivityTrackingRepository(db, clock) { elapsed }
      tracker.start(ActivityKind.SWIMMING, false)
      elapsed += 5000; tracker.tick()
      elapsed += 100_000; tracker.tick()
      assertEquals("PAUSED", db.activityTrackingDao().getActive()?.status)
      assertEquals(5000L, db.activityTrackingDao().getActive()?.elapsedMillis)
      val backup = BackupStore(db)
      val password = "synthetic-test-password".toCharArray()
      val prepared = backup.prepare(backup.export(password), password)
      backup.restore(prepared)
      assertEquals("PAUSED", db.activityTrackingDao().getActive()?.status)
      assertEquals(5000L, db.activityTrackingDao().getActive()?.elapsedMillis)
      tracker.discard()
      org.junit.Assert.assertNull(db.activityTrackingDao().getActive())
    } finally { db.close() }
  }

  @Test
  fun `GPS accepts only credible local points and route backup deletion never uploads`() = runBlocking<Unit> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
    try {
      val clock = java.time.Clock.fixed(java.time.Instant.parse("2020-01-10T12:00:00Z"), java.time.ZoneOffset.UTC)
      NewMeRepository(db, clock).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
      var elapsed = 1000L
      val tracker = ActivityTrackingRepository(db, clock) { elapsed }
      val track = tracker.start(ActivityKind.CYCLING, true)
      fun point(longitude: Double, time: Long, accuracy: Float = 5f) = android.location.Location("synthetic").apply {
        latitude = 0.0; this.longitude = longitude; this.time = time; this.accuracy = accuracy
      }
      assertFalse(tracker.point(point(0.0, clock.millis(), 100f)))
      assertTrue(tracker.point(point(0.0, clock.millis() - 10_000)))
      assertTrue(tracker.point(point(0.0001, clock.millis() - 5000)))
      assertFalse(tracker.point(point(0.0001, clock.millis() - 5000)))
      assertFalse(tracker.point(point(50.0, clock.millis())))
      assertTrue(db.activityTrackingDao().getActive()!!.distanceMeters > 10.0)
      tracker.pause()
      val original = db.activityTrackingDao().observeRoute(track.sessionKey).first()
      assertEquals(2, original.size)
      val backup = BackupStore(db)
      val password = "synthetic-test-password".toCharArray()
      val prepared = backup.prepare(backup.export(password), password)
      tracker.discard()
      backup.restore(prepared)
      assertEquals(original, db.activityTrackingDao().observeRoute(track.sessionKey).first())
      tracker.deleteRoute(track.sessionKey)
      assertTrue(db.activityTrackingDao().observeRoute(track.sessionKey).first().isEmpty())
      NewMeRepository(db, clock).resetAllData()
      org.junit.Assert.assertNull(db.activityTrackingDao().getActive())
      assertTrue(db.activityTrackingDao().observeRoute(track.sessionKey).first().isEmpty())
    } finally { db.close() }
  }

  @Test
  @Config(sdk = [34])
  fun `denied activity and location permissions keep sensor and GPS access off`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse(ActivityTrackingService.hasStepAccess(context))
    assertFalse(ActivityTrackingService.canUseGps(context))
  }
}

