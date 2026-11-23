package com.lifeanalytics.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.lifeanalytics.app.app.LifeAnalyticsApp
import com.lifeanalytics.app.core.designsystem.LifeAnalyticsTheme
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.AppThemeOption
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LifeAnalyticsApplication
        val repository = app.repository
        val preferencesRepository = app.preferencesRepository
        val backupManager = app.backupManager
        val reminderManager = app.reminderManager

        setContent {
            val preferences by preferencesRepository.preferencesFlow.collectAsState(
                initial = AppPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (preferences.themeOption) {
                AppThemeOption.SYSTEM -> systemDark
                AppThemeOption.LIGHT -> false
                AppThemeOption.DARK -> true
            }

            LifeAnalyticsTheme(darkTheme = useDarkTheme) {
                LifeAnalyticsApp(
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    preferences = preferences,
                    backupManager = backupManager,
                    reminderManager = reminderManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as LifeAnalyticsApplication
        lifecycleScope.launch {
            app.repository.checkOrConfirmActiveSession()
        }
    }
}
