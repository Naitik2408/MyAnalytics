package com.lifeanalytics.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeanalytics.app.LifeAnalyticsApplication
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives scheduled task and habit reminder alarms even when the app is in the background
 * or closed. Re-validates against live Room records and user preferences before posting,
 * and schedules the next recurring occurrence for active habits.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderNotificationManager.ACTION_TRIGGER_REMINDER) return
        val type = intent.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_TYPE) ?: return
        val entityId = intent.getStringExtra(ReminderNotificationManager.EXTRA_ENTITY_ID) ?: return

        val app = context.applicationContext as? LifeAnalyticsApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.preferencesRepository.preferencesFlow.first()
                if (!prefs.notificationsEnabled) return@launch

                val manager = app.reminderManager
                val now = ZonedDateTime.now(ZoneId.systemDefault())

                when (type) {
                    ReminderNotificationManager.TYPE_TASK -> {
                        if (!prefs.taskRemindersEnabled) return@launch
                        val task = app.repository.getTaskById(entityId) ?: return@launch
                        if (!task.isCompleted && task.plannedTime != null) {
                            manager.postTaskNotification(task)
                        }
                    }

                    ReminderNotificationManager.TYPE_HABIT -> {
                        if (!prefs.habitRemindersEnabled) return@launch
                        val habit = app.repository.observeAllHabits().first()
                            .firstOrNull { it.id == entityId } ?: return@launch
                        val today = now.toLocalDate()
                        val completions = app.repository.observeAllHabitCompletions().first()
                        val completedDates = completions
                            .filter { it.habitId == entityId && it.isCompleted }
                            .map { it.completionDate }
                            .toSet()

                        if (habit.isActive && habit.reminderTime != null &&
                            habit.isDueOn(today) && !completedDates.contains(today)
                        ) {
                            manager.postHabitNotification(habit)
                        }

                        // Chain the next scheduled occurrence strictly after `now`
                        manager.scheduleHabitReminder(
                            habit = habit,
                            completedDates = completedDates,
                            preferences = prefs,
                            now = now.plusSeconds(1)
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
