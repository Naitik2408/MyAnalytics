package com.lifeanalytics.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeanalytics.app.LifeAnalyticsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores and recalculates pending task & habit reminders after device reboot,
 * timezone change, manual system clock change, or app package update.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supported = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!supported) return

        val app = context.applicationContext as? LifeAnalyticsApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.reminderManager.syncAllReminders(
                    repository = app.repository,
                    preferencesRepository = app.preferencesRepository
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
