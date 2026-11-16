package com.lifeanalytics.app

import android.app.Application
import com.lifeanalytics.app.data.backup.BackupManager
import com.lifeanalytics.app.data.local.LifeAnalyticsDatabase
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository

class LifeAnalyticsApplication : Application() {
    val database: LifeAnalyticsDatabase by lazy {
        LifeAnalyticsDatabase.getInstance(this)
    }

    val repository: LifeAnalyticsRepository by lazy {
        LifeAnalyticsRepository(database = database)
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context = this)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            context = this,
            database = database,
            preferencesRepository = preferencesRepository
        )
    }
}
