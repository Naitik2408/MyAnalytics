package com.lifeanalytics.app.data.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeanalytics.app.LifeAnalyticsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Standard Android AlarmManager-based scheduler for daily automatic cloud backup.
 * Requires zero extra third-party or WorkManager dependencies, working natively on Android 8.0+ (API 26-35).
 */
class DailyBackupAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? LifeAnalyticsApplication ?: return
        val backupManager = app.backupManager
        val state = backupManager.statusState.value
        if (!state.isSignedIn || !state.hasCachedPassphrase || !state.autoBackupDailyEnabled) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            backupManager.performCloudBackup()
        }
    }
}

object BackupScheduler {
    private const val REQUEST_CODE = 4099

    fun scheduleDailyBackup(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyBackupAlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        // Schedule inexact repeating daily alarm (battery and OEM friendly)
        val intervalMillis = AlarmManager.INTERVAL_DAY
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancelDailyBackup(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyBackupAlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        alarmManager.cancel(pendingIntent)
    }
}
