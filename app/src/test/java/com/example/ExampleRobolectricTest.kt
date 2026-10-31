package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NewMeDatabase
import com.example.data.repository.NewMeRepository
import org.junit.Assert.assertEquals
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
  }
}

