package com.example

import android.app.Application
import com.example.data.local.NewMeDatabase
import com.example.data.backup.BackupStore
import com.example.data.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class NewMeApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var reminders: ReminderScheduler
        private set
    var maintenanceError: String? = null
        private set

    override fun onCreate() {
        super.onCreate()
        reminders = ReminderScheduler(this, NewMeDatabase.getInstance(this))
        try { BackupStore.clearInterruptedStaging(this) }
        catch (error: Exception) { maintenanceError = "An interrupted restore could not be cleaned up. Saved records were kept; keep NewMe installed." }
        reminders.observe(scope)
    }

    override fun onTerminate() {
        scope.cancel()
        super.onTerminate()
    }
}