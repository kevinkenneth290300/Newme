package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.BackupUiState
import com.example.viewmodel.NewMeViewModel
import java.time.LocalDate

@Composable
fun BackupControls(viewModel: NewMeViewModel) {
    val state by viewModel.backupState.collectAsStateWithLifecycle()
    var action by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val exportPicker = rememberLauncherForActivityResult(LocalBackupDocument(create = true)) { uri ->
        if (uri != null) { selectedUri = uri; action = "EXPORT" }
    }
    val restorePicker = rememberLauncherForActivityResult(LocalBackupDocument(create = false)) { uri ->
        if (uri != null) { selectedUri = uri; action = "RESTORE" }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Local Backup", style = MaterialTheme.typography.titleMedium)
        Text("Choose device storage or Downloads. NewMe does not upload backups. Keep the password safe: it cannot be recovered.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { exportPicker.launch("NewMe-backup-${LocalDate.now()}.newme") }, enabled = state != BackupUiState.Working, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileUpload, null)
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }
            OutlinedButton(onClick = { restorePicker.launch("") }, enabled = state != BackupUiState.Working, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("Restore")
            }
        }
        if (state == BackupUiState.Working) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (val current = state) {
            is BackupUiState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is BackupUiState.Success -> Text(current.message, color = MaterialTheme.colorScheme.primary)
            else -> Unit
        }
    }
    if (action != null) {
        val exporting = action == "EXPORT"
        AlertDialog(
            onDismissRequest = { action = null; selectedUri = null; password = ""; confirmation = "" },
            title = { Text(if (exporting) "Encrypt Backup" else "Open Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(password, { password = it }, label = { Text("Backup password") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                    if (exporting) {
                        OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Repeat password") },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        Text("At least 12 characters. There is no password reset.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = password.isNotEmpty() && (!exporting || (password.length >= 12 && password == confirmation)), onClick = {
                    val uri = selectedUri
                    if (uri != null) {
                        if (exporting) viewModel.exportBackup(uri, password.toCharArray())
                        else viewModel.prepareRestore(uri, password.toCharArray())
                    }
                    action = null; selectedUri = null; password = ""; confirmation = ""
                }) { Text(if (exporting) "Encrypt & Export" else "Validate") }
            },
            dismissButton = { TextButton(onClick = { action = null; selectedUri = null; password = ""; confirmation = "" }) { Text("Cancel") } }
        )
    }
    val prepared = state as? BackupUiState.ReadyToRestore
    if (prepared != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBackup,
            title = { Text("Replace All Local Data?") },
            text = { Text("Validated backup: ${prepared.backup.recordCount} records. Restoring replaces existing records. Active tracking will be paused and private reminders will remain off.") },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Replace & Restore") } },
            dismissButton = { TextButton(onClick = viewModel::dismissBackup) { Text("Cancel") } }
        )
    }
}

private class LocalBackupDocument(private val create: Boolean) : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent = Intent(
        if (create) Intent.ACTION_CREATE_DOCUMENT else Intent.ACTION_OPEN_DOCUMENT
    ).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        if (create) putExtra(Intent.EXTRA_TITLE, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}