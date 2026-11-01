package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.NewMeDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.model.ThemeMode

class SettingsRepository(private val database: NewMeDatabase) {
    suspend fun appearance(theme: ThemeMode, reducedMotion: Boolean) = database.withTransaction {
        val current = database.settingsDao().get() ?: AppSettingsEntity()
        database.settingsDao().save(current.copy(themeMode = theme.name, reducedMotion = reducedMotion))
    }
}