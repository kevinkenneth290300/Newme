package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.backup.BackupStore
import com.example.data.local.DatabaseMigrations
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.*
import com.example.data.repository.*
import com.example.data.routine.RoutinesRepository
import com.example.data.reminder.ReminderRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JourneyRobolectricTest {
    @get:Rule val migration = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), NewMeDatabase::class.java)
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2020-03-01T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `schema six upgrade preserves synthetic values in every released table`() {
        val schema = context.assets.open("com.example.data.local.NewMeDatabase/6.json").bufferedReader()
            .use { JSONObject(it.readText()).getJSONObject("database") }
        val entities = schema.getJSONArray("entities")
        val name = "synthetic-journey-migration.db"
        val old = migration.createDatabase(name, 6)
        val columns = mutableMapOf<String, List<String>>()
        val before = mutableMapOf<String, List<String?>>()
        old.execSQL("PRAGMA defer_foreign_keys = ON")
        for (index in 0 until entities.length()) {
            val entity = entities.getJSONObject(index)
            val table = entity.getString("tableName")
            val fields = entity.getJSONArray("fields")
            val names = List(fields.length()) { fields.getJSONObject(it).getString("columnName") }
            val values: List<Any?> = List(fields.length()) { fieldIndex ->
                val field = fields.getJSONObject(fieldIndex)
                val column = field.getString("columnName")
                when {
                    column == "sessionKey" -> "synthetic-session"
                    column == "date" || column == "startDate" || column == "prepDate" -> "2020-01-01"
                    column == "startEpochMillis" -> 1L
                    column == "timeOfDay" -> "MORNING"
                    column == "status" -> "PAUSED"
                    column == "currentXp" -> 1240L
                    column == "currentLevel" -> 3L
                    column == "age" -> 30L
                    column == "heightCm" -> 170.0
                    !field.optBoolean("notNull") -> null
                    field.getString("affinity") == "INTEGER" -> 1L
                    field.getString("affinity") == "REAL" -> 70.0
                    else -> "synthetic_$column"
                }
            }
            old.execSQL("INSERT INTO `$table` (${names.joinToString(",") { "`$it`" }}) VALUES (${names.joinToString(",") { "?" }})", values.toTypedArray())
            columns[table] = names
            old.query("SELECT * FROM `$table`").use { cursor -> cursor.moveToFirst(); before[table] = names.indices.map { if (cursor.isNull(it)) null else cursor.getString(it) } }
        }
        old.close()
        val upgraded = migration.runMigrationsAndValidate(name, 7, true, DatabaseMigrations.MIGRATION_6_7)
        for ((table, names) in columns) upgraded.query("SELECT ${names.joinToString(",") { "`$it`" }} FROM `$table`").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Changed released records in $table", before[table], names.indices.map { if (cursor.isNull(it)) null else cursor.getString(it) })
        }
        upgraded.query("SELECT dashboardSetupDone, enabledModules FROM app_settings").use { cursor ->
            cursor.moveToFirst(); assertEquals(0, cursor.getInt(0)); assertTrue(cursor.getString(1).contains("ROUTINES"))
        }
        upgraded.close()
    }

    @Test
    fun `routine rewards deletion and backup persist`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
        try {
            NewMeRepository(db, clock).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
            val routines = RoutinesRepository(db, clock)
            assertTrue(db.routineDao().getAll().isEmpty())
            db.settingsDao().save(AppSettingsEntity(enabledModules = "WATER"))
            routines.applyPacks(context, setOf("morning"))
            assertEquals(5, db.routineDao().getAll().size)
            assertEquals(setOf("WATER", "ROUTINES"), db.settingsDao().get()!!.enabledModules.split(',').toSet())
            val routine = db.routineDao().getAll().first()
            repeat(3) { routines.complete(routine.id, "2020-03-01", true) }
            assertEquals(10, db.userProfileDao().getUserProfileDirect()!!.currentXp)
            routines.complete(routine.id, "2020-03-01", false)
            routines.complete(routine.id, "2020-03-01", true)
            assertEquals(10, db.userProfileDao().getUserProfileDirect()!!.currentXp)
            ReminderRepository(db, clock).save("routine:${routine.id}", 13 * 60, true, false, routineId = routine.id)
            assertTrue(ReminderRepository(db, clock).plan().any { it.key == "routine:${routine.id}" })
            routines.delete(routine.id)
            routines.applyPacks(context, setOf("morning"))
            assertEquals(4, db.routineDao().getAll().size)
            assertNull(db.reminderDao().get("routine:${routine.id}"))
            val backup = BackupStore(db, context)
            val password = "synthetic-test-password".toCharArray()
            val prepared = backup.prepare(backup.export(password), password)
            NewMeRepository(db, clock).resetAllData()
            backup.restore(prepared)
            assertEquals(4, db.routineDao().getAll().size)
            assertEquals(10, db.userProfileDao().getUserProfileDirect()!!.currentXp)
        } finally { db.close() }
    }

    @Test
    fun `summary streak matches pure rules and write query count stays constant with two thousand cached days`() = runBlocking {
        val queries = AtomicInteger()
        val db = Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries()
            .setQueryCallback({ sql, _ ->
                if (sql.trimStart().startsWith("SELECT", ignoreCase = true) && !sql.contains("room_table_modification_log")) queries.incrementAndGet()
            }, Executor { command -> command.run() }).build()
        try {
            NewMeRepository(db, clock).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
            val summaries = DaySummaryRepository(db, clock)
            summaries.sync()
            queries.set(0)
            summaries.sync()
            val smallCount = queries.get()
            assertTrue(smallCount > 0)
            val today = LocalDate.now(clock)
            val qualifying = mutableSetOf<LocalDate>()
            db.withTransaction {
                val current = db.dailyStateDao().get(today.toString())!!
                for (offset in 1..2000) {
                    val date = today.minusDays(offset.toLong())
                    val met = offset % 3 != 0
                    if (met) qualifying.add(date)
                    db.dailyStateDao().insert(current.copy(date = date.toString()))
                    db.daySummaryDao().save(DaySummaryEntity(date.toString(), met, if (met) 3 else 0, 3, 0, 0, clock.millis()))
                    db.daySummaryDao().clearDirty(date.toString())
                }
            }
            queries.set(0)
            summaries.sync()
            assertEquals(smallCount, queries.get())
            assertEquals(DailyProgressRules.streak(today, qualifying), db.daySummaryDao().streak(today.toString()))
        } finally { db.close() }
    }

    @Test
    fun `moment partial answers survive restart and completion rewards once without overriding manual sleep`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
        try {
            val morning = Clock.fixed(Instant.parse("2020-03-01T08:00:00Z"), ZoneOffset.UTC)
            NewMeRepository(db, morning).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
            db.settingsDao().save((db.settingsDao().get() ?: AppSettingsEntity()).copy(dashboardSetupDone = true, momentTypes = "sleep,mood,focus"))
            val repository = com.example.data.moment.MomentsRepository(db, morning)
            var prepared = repository.prepare()!!
            assertEquals(0, db.userProfileDao().getUserProfileDirect()!!.currentXp)
            prepared = repository.answer(prepared.candidate, "sleep", "450:4")!!
            prepared = com.example.data.moment.MomentsRepository(db, morning).prepare()!!
            assertEquals("450:4", prepared.answers["sleep"])
            prepared = repository.answer(prepared.candidate, "mood", "Good")!!
            val candidate = prepared.candidate
            assertNull(repository.answer(candidate, "focus", "Move"))
            assertEquals("Moment", db.sleepDao().getLog("2020-03-01")!!.source)
            assertEquals(450, db.sleepDao().getLog("2020-03-01")!!.durationMinutes)
            assertEquals("Good", db.dailyCheckinDao().getCheckinDirect("2020-03-01", "MORNING")!!.mood)
            assertEquals(20, db.userProfileDao().getUserProfileDirect()!!.currentXp)
            assertNull(repository.answer(candidate, "focus", "Move"))
            assertEquals(20, db.userProfileDao().getUserProfileDirect()!!.currentXp)
            assertNull(repository.prepare())
            NewMeRepository(db, morning).resetAllData()
            NewMeRepository(db, morning).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
            db.settingsDao().save(AppSettingsEntity(dashboardSetupDone = true, momentTypes = "sleep,mood,focus"))
            val manual = SleepLogEntity("2020-03-01", bedtime = "22:30", wakeTime = "05:30", durationMinutes = 420, qualityRating = 2)
            db.sleepDao().insertOrUpdateSleepLog(manual)
            val withManual = repository.prepare()!!
            assertEquals("mood", withManual.card!!.type)
            val focused = repository.answer(withManual.candidate, "mood", "Good")!!
            assertNull(repository.answer(focused.candidate, "focus", "Move"))
            assertEquals(manual, db.sleepDao().getLog("2020-03-01"))
        } finally { db.close() }
    }

    @Test
    fun `routine form failure rolls back both writes`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, NewMeDatabase::class.java).allowMainThreadQueries().build()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined)
        try {
            NewMeRepository(db, clock).completeOnboarding(UserProfileEntity(name = "Synthetic User"))
            val errors = mutableListOf<String>()
            val writes = com.example.util.UiWriteQueue(scope, errors::add)
            val model = com.example.viewmodel.RoutinesViewModel(db, ApplicationProvider.getApplicationContext<android.app.Application>(), clock, scope, writes)
            var saved = false
            model.save(RoutineTemplateEntity(title = "Synthetic routine", startDate = "2020-03-01"), true, -1, false) { saved = true }
            writes.awaitIdle()
            assertFalse(saved)
            assertEquals(1, errors.size)
            assertTrue(db.routineDao().getAll().isEmpty())
            assertTrue(db.reminderDao().getAll().isEmpty())
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            db.close()
        }
    }
}