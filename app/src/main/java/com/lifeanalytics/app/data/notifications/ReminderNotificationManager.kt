package com.lifeanalytics.app.data.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifeanalytics.app.MainActivity
import com.lifeanalytics.app.R
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.Task
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/**
 * Coordinates local Android notifications and AlarmManager scheduling for
 * Life Analytics tasks and habits.
 *
 * Scheduling Strategy:
 * - Uses [AlarmManager.setAndAllowWhileIdle] by default so reminders work in Doze mode
 *   without requiring Android 12+ special exact-alarm permissions.
 * - If exact alarms happen to be permitted (`canScheduleExactAlarms()` on API 31+),
 *   upgrades to `setExactAndAllowWhileIdle` with a safe fallback on `SecurityException`.
 * - Every alarm uses a deterministic request code + `FLAG_UPDATE_CURRENT` so editing
 *   or resyncing replaces the existing alarm cleanly without duplicates.
 */
class ReminderNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager: AlarmManager? =
        appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun ensureNotificationChannels() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val taskChannel = NotificationChannel(
            CHANNEL_TASK_REMINDERS,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for scheduled tasks with a planned date and time."
        }

        val habitChannel = NotificationChannel(
            CHANNEL_HABIT_REMINDERS,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recurring reminders on your active habit days."
        }

        manager.createNotificationChannel(taskChannel)
        manager.createNotificationChannel(habitChannel)
    }

    fun hasNotificationPermission(): Boolean {
        val notificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        if (!notificationsEnabled) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleTaskReminder(
        task: Task,
        preferences: AppPreferences,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ) {
        if (!preferences.notificationsEnabled || !preferences.taskRemindersEnabled) {
            cancelTaskReminder(task.id)
            return
        }

        val triggerTime = NotificationScheduleCalculator.computeTaskTriggerTime(
            task = task,
            offsetMinutes = preferences.taskReminderOffsetMinutes,
            now = now
        )
        if (triggerTime == null) {
            cancelTaskReminder(task.id)
            return
        }

        val pendingIntent = buildAlarmPendingIntent(
            type = TYPE_TASK,
            entityId = task.id,
            requestCode = NotificationScheduleCalculator.taskRequestCode(task.id)
        )
        scheduleAlarmAt(triggerTime.toInstant().toEpochMilli(), pendingIntent)
    }

    fun cancelTaskReminder(taskId: String) {
        val pendingIntent = buildAlarmPendingIntent(
            type = TYPE_TASK,
            entityId = taskId,
            requestCode = NotificationScheduleCalculator.taskRequestCode(taskId)
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        NotificationManagerCompat.from(appContext)
            .cancel(NotificationScheduleCalculator.taskRequestCode(taskId))
    }

    fun scheduleHabitReminder(
        habit: Habit,
        completedDates: Set<LocalDate>,
        preferences: AppPreferences,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ) {
        if (!preferences.notificationsEnabled || !preferences.habitRemindersEnabled) {
            cancelHabitReminder(habit.id)
            return
        }

        val triggerTime = NotificationScheduleCalculator.computeNextHabitTriggerTime(
            habit = habit,
            completedDates = completedDates,
            now = now
        )
        if (triggerTime == null) {
            cancelHabitReminder(habit.id)
            return
        }

        val pendingIntent = buildAlarmPendingIntent(
            type = TYPE_HABIT,
            entityId = habit.id,
            requestCode = NotificationScheduleCalculator.habitRequestCode(habit.id)
        )
        scheduleAlarmAt(triggerTime.toInstant().toEpochMilli(), pendingIntent)
    }

    fun cancelHabitReminder(habitId: String) {
        val pendingIntent = buildAlarmPendingIntent(
            type = TYPE_HABIT,
            entityId = habitId,
            requestCode = NotificationScheduleCalculator.habitRequestCode(habitId)
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        NotificationManagerCompat.from(appContext)
            .cancel(NotificationScheduleCalculator.habitRequestCode(habitId))
    }

    /**
     * Idempotent full synchronization across all tasks and habits in Room.
     * Safe to invoke on app launch, device reboot, timezone/time changes, backup restore,
     * and whenever Settings notification preferences change.
     */
    suspend fun syncAllReminders(
        repository: LifeAnalyticsRepository,
        preferencesRepository: PreferencesRepository,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    ) {
        ensureNotificationChannels()
        val prefs = preferencesRepository.preferencesFlow.first()

        val allTasks = repository.observeAllTasks().first()
        for (task in allTasks) {
            scheduleTaskReminder(task = task, preferences = prefs, now = now)
        }

        val allHabits = repository.observeAllHabits().first()
        val allCompletions = repository.observeAllHabitCompletions().first()
        val completedDatesByHabit = allCompletions
            .filter { it.isCompleted }
            .groupBy { it.habitId }
            .mapValues { (_, items) -> items.map { it.completionDate }.toSet() }

        for (habit in allHabits) {
            val completedDates = completedDatesByHabit[habit.id].orEmpty()
            scheduleHabitReminder(
                habit = habit,
                completedDates = completedDates,
                preferences = prefs,
                now = now
            )
        }
    }

    fun postTaskNotification(task: Task) {
        if (!hasNotificationPermission()) return
        ensureNotificationChannels()

        val timeText = task.plannedTime?.let { DateAttributionPolicy.formatLocalTime(it) }
        val areaText = task.lifeArea?.shortName ?: task.category.displayName
        val body = if (timeText != null) {
            "Scheduled at $timeText • $areaText"
        } else {
            areaText
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(task.title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NotificationScheduleCalculator.taskRequestCode(task.id), notification)
        }
    }

    fun postHabitNotification(habit: Habit) {
        if (!hasNotificationPermission()) return
        ensureNotificationChannels()

        val areaPrefix = habit.lifeArea?.shortName?.let { "$it • " } ?: ""
        val body = "${areaPrefix}Scheduled for today"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(habit.name)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NotificationScheduleCalculator.habitRequestCode(habit.id), notification)
        }
    }

    fun postTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false
        ensureNotificationChannels()
        val zone = ZoneId.systemDefault()
        val nowTime = DateAttributionPolicy.formatLocalTime(ZonedDateTime.now(zone).toLocalTime())
        val notification = NotificationCompat.Builder(appContext, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("Life Analytics reminders active")
            .setContentText("Test notification delivered at $nowTime (${zone.id})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent())
            .build()
        return runCatching {
            NotificationManagerCompat.from(appContext).notify(999_001, notification)
            true
        }.getOrDefault(false)
    }

    private fun scheduleAlarmAt(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager ?: return
        try {
            if (canScheduleExactAlarms()) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, buildOpenAppIntent())
                manager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun buildAlarmPendingIntent(
        type: String,
        entityId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type)
            putExtra(EXTRA_ENTITY_ID, entityId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildOpenAppIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_TASK_REMINDERS = "la_task_reminders_v2"
        const val CHANNEL_HABIT_REMINDERS = "la_habit_reminders_v2"
        const val ACTION_TRIGGER_REMINDER = "com.lifeanalytics.app.ACTION_TRIGGER_REMINDER"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val EXTRA_ENTITY_ID = "extra_entity_id"
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"

        fun openAppNotificationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }

        fun openExactAlarmSettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            }
        }
    }
}
