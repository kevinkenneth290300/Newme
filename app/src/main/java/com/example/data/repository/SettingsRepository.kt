package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.model.ThemeMode
import com.example.data.routine.AppModule

class SettingsRepository(private val database: NewMeDatabase) {
    suspend fun appearance(theme: ThemeMode, reducedMotion: Boolean) = database.withTransaction {
        val current = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(current.copy(themeMode = theme.name, reducedMotion = reducedMotion))
    }

    suspend fun dashboard(modules: Set<String>, done: Boolean = true) = database.withTransaction {
        require(modules.all { value -> AppModule.entries.any { it.name == value } }) { "Choose supported dashboard modules." }
        val current = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(current.copy(enabledModules = modules.sorted().joinToString(","), dashboardSetupDone = done))
    }
    suspend fun moments(enabled: Boolean, types: Set<String>) = database.withTransaction {
        require(types.all { it in setOf("sleep", "mood", "focus", "energy", "hydrate", "win", "profile", "comeback") }) { "Choose supported Moment types." }
        val current = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(current.copy(momentsEnabled = enabled, momentTypes = types.sorted().joinToString(",")))
    }
    suspend fun haptics(enabled: Boolean) = database.withTransaction {
        val current = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(current.copy(hapticsEnabled = enabled))
    }
}