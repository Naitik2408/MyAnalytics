package com.lifeanalytics.app.data.notifications

import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.Task
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * Pure, deterministic reminder scheduling calculations for tasks and habits.
 * Kept free of Android framework dependencies so every boundary condition
 * (past times, completed items, weekday schedules, deactivated habits, timezone
 * shifts, and request-code isolation) is directly unit-testable.
 */
object NotificationScheduleCalculator {

    private const val TASK_CODE_MASK = 0x3FFFFFFF
    private const val HABIT_CODE_OFFSET = 0x40000000

    /**
     * Computes the exact local trigger time for a scheduled [task].
     * Returns `null` if:
     * - the task is already completed
     * - the task has no [Task.plannedTime]
     * - the computed trigger time is in the past or equal to [now]
     */
    fun computeTaskTriggerTime(
        task: Task,
        offsetMinutes: Int,
        now: ZonedDateTime
    ): ZonedDateTime? {
        if (task.isCompleted) return null
        val plannedTime = task.plannedTime ?: return null
        val clampedOffset = offsetMinutes.coerceIn(0, 120).toLong()
        val eventTime = ZonedDateTime.of(task.plannedDate, plannedTime, now.zone)
        val triggerTime = eventTime.minusMinutes(clampedOffset)
        return if (triggerTime.isAfter(now)) triggerTime else null
    }

    /**
     * Finds the next future trigger time for an active [habit] with an optional [Habit.reminderTime].
     * Scans up to [lookaheadDays] starting from [now]'s local date:
     * - Respects [Habit.isDueOn] (active schedule period + scheduled weekdays + deactivation boundary)
     * - Skips any date present in [completedDates] (so an already-completed occurrence is not reminded)
     * - Skips any occurrence whose trigger time is at or before [now]
     */
    fun computeNextHabitTriggerTime(
        habit: Habit,
        completedDates: Set<LocalDate>,
        now: ZonedDateTime,
        lookaheadDays: Int = 14
    ): ZonedDateTime? {
        if (!habit.isActive) return null
        val reminderTime = habit.reminderTime ?: return null
        val startDate = now.toLocalDate()
        val limit = lookaheadDays.coerceAtLeast(1)

        for (dayOffset in 0 until limit) {
            val candidateDate = startDate.plusDays(dayOffset.toLong())
            if (!habit.isDueOn(candidateDate)) continue
            if (completedDates.contains(candidateDate)) continue
            val candidateTrigger = ZonedDateTime.of(candidateDate, reminderTime, now.zone)
            if (candidateTrigger.isAfter(now)) {
                return candidateTrigger
            }
        }
        return null
    }

    /**
     * Stable, deterministic PendingIntent request code for a task ID.
     * Guaranteed strictly positive and disjoint from [habitRequestCode].
     */
    fun taskRequestCode(taskId: String): Int {
        val hash = abs(stableHash32("task:$taskId")) and TASK_CODE_MASK
        return if (hash == 0) 1 else hash
    }

    /**
     * Stable, deterministic PendingIntent request code for a habit ID.
     * Guaranteed strictly positive and disjoint from [taskRequestCode].
     */
    fun habitRequestCode(habitId: String): Int {
        val hash = abs(stableHash32("habit:$habitId")) and TASK_CODE_MASK
        val nonZero = if (hash == 0) 1 else hash
        return HABIT_CODE_OFFSET or nonZero
    }

    private fun stableHash32(input: String): Int {
        var h = 2166136261L
        for (ch in input) {
            h = (h xor ch.code.toLong()) * 16777619L
        }
        return (h xor (h ushr 32)).toInt()
    }
}
