package com.example.viewmodel

import com.example.data.backup.PreparedBackup

sealed interface AppLoadState {
    data object Loading : AppLoadState
    data object Ready : AppLoadState
    data class Error(val message: String) : AppLoadState
}

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Working : BackupUiState
    data class ReadyToRestore(val backup: PreparedBackup) : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}