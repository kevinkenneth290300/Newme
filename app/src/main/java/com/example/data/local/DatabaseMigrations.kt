package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (id INTEGER NOT NULL, themeMode TEXT NOT NULL, reducedMotion INTEGER NOT NULL, dailyGoalType TEXT NOT NULL, dailyGoalTarget INTEGER NOT NULL, goalChosen INTEGER NOT NULL, cycleEnabled INTEGER NOT NULL, cycleRemindersEnabled INTEGER NOT NULL, taskRemindersEnabled INTEGER NOT NULL, quietStartMinutes INTEGER NOT NULL, quietEndMinutes INTEGER NOT NULL, legacyRewardCutoffDate TEXT NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS daily_state (date TEXT NOT NULL, goalType TEXT NOT NULL, goalTarget INTEGER NOT NULL, goalEnabled INTEGER NOT NULL, calorieTarget INTEGER NOT NULL, proteinTargetGrams INTEGER NOT NULL, waterTargetMl INTEGER NOT NULL, sleepTargetMinutes INTEGER NOT NULL, targetsKnown INTEGER NOT NULL, activityEnabled INTEGER NOT NULL, PRIMARY KEY(date))")
            db.execSQL("CREATE TABLE IF NOT EXISTS reward_ledger (eventKey TEXT NOT NULL, date TEXT NOT NULL, xp INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(eventKey))")
            db.execSQL("CREATE TABLE IF NOT EXISTS legacy_reward_exclusions (eventKey TEXT NOT NULL, PRIMARY KEY(eventKey))")
            db.execSQL("INSERT INTO app_settings VALUES (1, 'SYSTEM', 0, 'TASKS', 3, 0, 0, 0, 0, 1320, 420, DATE('now', 'localtime'))")
            val datedTables = listOf("daily_checklist", "meal_plans", "water_logs", "sleep_logs", "workout_sessions", "activity_sessions", "daily_checkins", "environment_tasks", "protein_logs", "cardio_logs")
            val dates = datedTables.joinToString(" UNION ") { "SELECT date FROM $it" }
            db.execSQL("INSERT OR IGNORE INTO daily_state SELECT date, 'TASKS', 3, 0, 0, 0, 0, 0, 0, 0 FROM ($dates)")
            val completed = listOf("daily_checklist" to "task", "meal_plans" to "meal", "activity_sessions" to "activity", "environment_tasks" to "environment")
            completed.forEach { (table, kind) ->
                db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT '$kind:' || id FROM $table")
            }
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'workout:' || date FROM workout_sessions")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'sleep:' || date FROM sleep_logs")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'water:' || date FROM water_logs")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'checkin:' || date || ':' || timeOfDay FROM daily_checkins")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'mealprep:' || id FROM meal_prep_items")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'protein:' || id FROM protein_logs")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'cardio:' || id FROM cardio_logs")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'supplement:' || supplementId || ':' || date FROM supplement_logs")
            db.execSQL("INSERT OR IGNORE INTO legacy_reward_exclusions SELECT 'supplement-definition:' || id FROM supplements")
            db.execSQL("CREATE TABLE IF NOT EXISTS cycle_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startDate TEXT NOT NULL, endDate TEXT, symptoms TEXT NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cycle_entries_startDate ON cycle_entries(startDate)")
            db.execSQL("CREATE TABLE IF NOT EXISTS activity_tracks (sessionKey TEXT NOT NULL, activityType TEXT NOT NULL, startEpochMillis INTEGER NOT NULL, endEpochMillis INTEGER, status TEXT NOT NULL, elapsedMillis INTEGER NOT NULL, lastElapsedRealtime INTEGER NOT NULL, lastWallEpochMillis INTEGER NOT NULL, lastStepCounter INTEGER NOT NULL, steps INTEGER NOT NULL, distanceMeters REAL NOT NULL, gpsEnabled INTEGER NOT NULL, routeSegment INTEGER NOT NULL, warning TEXT NOT NULL, PRIMARY KEY(sessionKey))")
            db.execSQL("CREATE TABLE IF NOT EXISTS active_tracking (id INTEGER NOT NULL, sessionKey TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(sessionKey) REFERENCES activity_tracks(sessionKey) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_tracking_sessionKey ON active_tracking(sessionKey)")
            db.execSQL("CREATE TABLE IF NOT EXISTS tracking_day_totals (sessionKey TEXT NOT NULL, date TEXT NOT NULL, elapsedMillis INTEGER NOT NULL, steps INTEGER NOT NULL, distanceMeters REAL NOT NULL, PRIMARY KEY(sessionKey, date), FOREIGN KEY(sessionKey) REFERENCES activity_tracks(sessionKey) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracking_day_totals_date ON tracking_day_totals(date)")
            db.execSQL("CREATE TABLE IF NOT EXISTS route_points (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionKey TEXT NOT NULL, epochMillis INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, accuracyMeters REAL NOT NULL, segment INTEGER NOT NULL, FOREIGN KEY(sessionKey) REFERENCES activity_tracks(sessionKey) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_route_points_sessionKey ON route_points(sessionKey)")
        }
    }
}