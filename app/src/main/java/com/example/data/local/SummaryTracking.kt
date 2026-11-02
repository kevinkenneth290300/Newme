package com.example.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

object SummaryTracking {
    fun install(db: SupportSQLiteDatabase) {
        db.query("SELECT 1 FROM sqlite_master WHERE type = 'trigger' AND name = 'summary_daily_state_insert'").use {
            if (it.moveToFirst()) return
        }
        val tables = listOf("daily_state", "daily_checklist", "meal_plans", "protein_logs", "water_logs", "sleep_logs",
            "workout_sessions", "activity_sessions", "cardio_logs", "daily_checkins", "reward_ledger", "routine_completions", "tracking_day_totals")
        for (table in tables) {
            for (event in listOf("INSERT", "UPDATE", "DELETE")) {
                val records = if (event == "UPDATE") listOf("OLD", "NEW") else listOf(if (event == "DELETE") "OLD" else "NEW")
                val changes = records.joinToString(" ") { "INSERT INTO summary_dirty(date) SELECT $it.date WHERE NOT EXISTS(SELECT 1 FROM summary_dirty WHERE date = $it.date);" }
                db.execSQL("CREATE TRIGGER IF NOT EXISTS summary_${table}_${event.lowercase()} AFTER $event ON $table BEGIN $changes END")
            }
        }
    }
}