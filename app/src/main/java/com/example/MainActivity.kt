package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import com.example.data.model.ThemeMode
import com.example.viewmodel.NewMeViewModel
import com.example.ui.NewMeApp
import com.example.ui.theme.NewMeTheme
import com.example.data.reminder.ReminderScheduler
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      val scheduler = ReminderScheduler.from(this@MainActivity)
      try { scheduler.reconcile() }
      catch (cancelled: CancellationException) { throw cancelled }
      catch (error: Exception) { scheduler.reportFailure() }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val model: NewMeViewModel = viewModel()
      val settings by model.appSettings.collectAsStateWithLifecycle()
      val systemMotionOff = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
      NewMeTheme(themeMode = ThemeMode.entries.firstOrNull { it.name == settings?.themeMode } ?: ThemeMode.SYSTEM,
        reducedMotion = settings?.reducedMotion == true || systemMotionOff) {
        NewMeApp(viewModel = model)
      }
    }
  }
}

