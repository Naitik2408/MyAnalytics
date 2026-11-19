package com.lifeanalytics.app.domain.usecase

import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.FocusPauseInterval
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitCompletion
import com.lifeanalytics.app.domain.model.HabitDayState
import com.lifeanalytics.app.domain.model.HabitForDateItem
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TransactionType
import java.time.LocalDate
import kotlin.math.abs

data class TaskSummary(
    val completedCount: Int,
    val totalCount: Int,
    val tasksWithEstimateCount: Int,
    val totalEstimatedMinutes: Int? // null when no task has an estimate
) {
    val hasTasks: Boolean get() = totalCount > 0
    val completionRatioText: String
        get() = if (totalCount == 0) "0 / 0" else "$completedCount / $totalCount"
}

data class HabitDailySummary(
    val completedDueCount: Int,
    val totalDueCount: Int,
    val totalActiveHabitsCount: Int,
    val dueHabits: List<HabitForDateItem>,
    val otherActiveHabits: List<HabitForDateItem>
) {
    val hasDueHabits: Boolean get() = totalDueCount > 0
    val completionRatioText: String
        get() = if (totalDueCount == 0) "No habits scheduled" else "$completedDueCount / $totalDueCount"
}

data class HabitConsistencyItem(
    val habit: Habit,
    val completedDueOccurrences: Int,
    val totalDueOccurrences: Int,
    val consistencyPercent: Int? // null when totalDueOccurrences == 0
)

data class HabitPeriodSummary(
    val completedDueOccurrences: Int,
    val totalDueOccurrences: Int,
    val consistencyPercent: Int?, // null when totalDueOccurrences == 0
    val perHabitConsistency: List<HabitConsistencyItem>
)

data class FinancePeriodSummary(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val netForPeriodMinor: Long,
    val transactionCount: Int,
    val expensesByCategoryMinor: Map<String, Long>
) {
    val hasTransactions: Boolean get() = transactionCount > 0
    val savingsAmountMinor: Long get() = totalIncomeMinor - totalExpenseMinor
    /**
     * Savings rate = ((income - expenses) / income) * 100, ONLY when income > 0
     * (docs/08_FINANCE.md & docs/09_LIFE_ANALYTICS.md 19 Nov 2026 update).
     */
    val savingsRatePercent: Int?
        get() = if (totalIncomeMinor > 0L) {
            (((totalIncomeMinor - totalExpenseMinor) * 100.0) / totalIncomeMinor).toInt()
        } else {
            null
        }
}

data class PeriodComparison(
    val currentValue: Long,
    val previousValue: Long,
    val delta: Long,
    val percentChange: Int? // null when previousValue == 0L
)

object DomainCalculations {

    /**
     * Default task ordering (docs/05_DAILY_PLANNING.md Section 10.2):
     * 1. Incomplete tasks before completed tasks.
     * 2. Tasks with planned times ordered by planned time.
     * 3. Tasks without planned times ordered consistently after scheduled tasks by sortOrder and createdAt.
     */
    fun sortTasksForDay(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenBy { it.plannedTime == null }
                .thenBy { it.plannedTime }
                .thenBy { it.sortOrder }
                .thenBy { it.createdAtEpochMillis }
                .thenBy { it.id }
        )
    }

    /**
     * Task summary calculation (docs/05_DAILY_PLANNING.md Section 7 & 15):
     * Distinguishes no tasks from tasks without duration estimates.
     */
    fun calculateTaskSummary(tasks: List<Task>): TaskSummary {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val estimates = tasks.mapNotNull { task ->
            task.estimatedDurationMinutes?.takeIf { it > 0 }
        }
        return TaskSummary(
            completedCount = completed,
            totalCount = total,
            tasksWithEstimateCount = estimates.size,
            totalEstimatedMinutes = if (estimates.isEmpty()) null else estimates.sum()
        )
    }

    /**
     * Authoritative focus duration calculation (docs/06_ACTIVE_WORK_FOCUS.md Section 11.1):
     * Excludes all paused intervals and prevents negative or impossible durations.
     */
    fun calculateFocusedDurationSeconds(
        startedAtEpochMillis: Long,
        endOrNowEpochMillis: Long,
        pauses: List<FocusPauseInterval>
    ): Long {
        if (endOrNowEpochMillis <= startedAtEpochMillis) return 0L

        val clampedIntervals = pauses.mapNotNull { pause ->
            val pauseStart = pause.pausedAtEpochMillis.coerceAtLeast(startedAtEpochMillis)
            val rawEnd = pause.resumedAtEpochMillis ?: endOrNowEpochMillis
            val pauseEnd = rawEnd.coerceAtMost(endOrNowEpochMillis)
            if (pauseEnd > pauseStart) pauseStart to pauseEnd else null
        }.sortedBy { it.first }

        val mergedPauses = mutableListOf<Pair<Long, Long>>()
        for (interval in clampedIntervals) {
            if (mergedPauses.isEmpty()) {
                mergedPauses.add(interval)
            } else {
                val last = mergedPauses.last()
                if (interval.first <= last.second) {
                    mergedPauses[mergedPauses.lastIndex] = last.first to maxOf(last.second, interval.second)
                } else {
                    mergedPauses.add(interval)
                }
            }
        }

        val totalWallMillis = endOrNowEpochMillis - startedAtEpochMillis
        val totalPausedMillis = mergedPauses.sumOf { (start, end) -> end - start }
        val activeMillis = (totalWallMillis - totalPausedMillis).coerceAtLeast(0L)
        return activeMillis / 1000L
    }

    /**
     * Computes the reliable focused duration up to the last confirmed timestamp for recovery.
     */
    fun calculateConfirmedFocusDurationSeconds(session: FocusSession): Long {
        val safeEnd = when (session.status) {
            FocusSessionStatus.COMPLETED -> session.endedAtEpochMillis ?: session.lastConfirmedAtEpochMillis
            FocusSessionStatus.PAUSED -> {
                val openPauseStart = session.pauses.lastOrNull { it.resumedAtEpochMillis == null }?.pausedAtEpochMillis
                openPauseStart ?: session.lastConfirmedAtEpochMillis
            }
            FocusSessionStatus.ACTIVE, FocusSessionStatus.INTERRUPTED -> session.lastConfirmedAtEpochMillis
        }.coerceAtLeast(session.startedAtEpochMillis)
        return calculateFocusedDurationSeconds(
            startedAtEpochMillis = session.startedAtEpochMillis,
            endOrNowEpochMillis = safeEnd,
            pauses = session.pauses
        )
    }

    /**
     * Detects whether a running or paused session suffered an unverified interruption
     * (device reboot, force-stop/process death, or wall-clock jump) using both wall-clock
     * and monotonic boot clock anchors (docs/06_ACTIVE_WORK_FOCUS.md Section 12 & 13).
     *
     * - If [nowEpochMillis] jumped backward before [session.startedAtEpochMillis] or
     *   [session.lastConfirmedAtEpochMillis], a wall-clock jump occurred.
     * - If [nowElapsedRealtimeMillis] and [session.lastConfirmedElapsedRealtimeMillis] are present
     *   and their delta diverges from the wall-clock delta by more than 60s (or monotonic clock
     *   reset to a smaller value after device reboot), the device rebooted or clock was changed.
     * - If the session was ACTIVE and an unconfirmed gap exceeds [maxUnconfirmedActiveGapMillis]
     *   without a matching monotonic continuity check, it is flagged as interrupted.
     */
    fun checkSessionInterruptionReason(
        session: FocusSession,
        nowEpochMillis: Long,
        nowElapsedRealtimeMillis: Long? = null,
        maxUnconfirmedActiveGapMillis: Long = 12 * 60 * 1000L // 12 minutes unconfirmed gap
    ): String? {
        if (session.status != FocusSessionStatus.ACTIVE && session.status != FocusSessionStatus.PAUSED) {
            return null
        }
        if (nowEpochMillis < session.startedAtEpochMillis || nowEpochMillis + 5_000L < session.lastConfirmedAtEpochMillis) {
            return "Device clock changed while the focus session was in progress."
        }

        val wallDelta = nowEpochMillis - session.lastConfirmedAtEpochMillis
        val previousElapsed = session.lastConfirmedElapsedRealtimeMillis
        if (nowElapsedRealtimeMillis != null && previousElapsed != null) {
            val monotonicDelta = nowElapsedRealtimeMillis - previousElapsed
            if (monotonicDelta < -5_000L) {
                return "Device rebooted while the focus session was in progress."
            }
            if (abs(wallDelta - monotonicDelta) > 60_000L) {
                return "Device clock or power state changed while the focus session was in progress."
            }
        }

        if (session.status == FocusSessionStatus.ACTIVE && wallDelta > maxUnconfirmedActiveGapMillis) {
            return "Session was interrupted for ${(wallDelta / 60_000L).coerceAtLeast(1L)} min without confirmation."
        }
        return null
    }

    /**
     * Sums valid focused duration for completed sessions attributed to a date or period.
     */
    fun sumCompletedFocusDurationSeconds(sessions: List<FocusSession>): Long {
        return sessions
            .filter { it.status == FocusSessionStatus.COMPLETED }
            .sumOf { it.focusedDurationSeconds.coerceAtLeast(0L) }
    }

    /**
     * Evaluates habits for a single local date (docs/07_DAILY_HABITS.md Section 7.3 & 11).
     */
    fun evaluateHabitsForDate(
        habits: List<Habit>,
        completionsForDate: List<HabitCompletion>,
        date: LocalDate
    ): HabitDailySummary {
        val completionMap = completionsForDate.associateBy { it.habitId }
        val items = habits
            .sortedWith(compareBy<Habit> { it.createdAtEpochMillis }.thenBy { it.id })
            .map { habit ->
                val effectivePeriod = habit.effectiveScheduleOn(date)
                val isCompleted = completionMap[habit.id]?.isCompleted == true
                val state = when {
                    effectivePeriod == null -> HabitDayState.NOT_APPLICABLE
                    !effectivePeriod.weekdays.contains(date.dayOfWeek) -> HabitDayState.NOT_SCHEDULED
                    isCompleted -> HabitDayState.DUE_COMPLETED
                    else -> HabitDayState.DUE_INCOMPLETE
                }
                HabitForDateItem(
                    habit = habit,
                    schedulePeriod = effectivePeriod,
                    dayState = state,
                    isCompleted = isCompleted
                )
            }

        val dueItems = items.filter {
            it.dayState == HabitDayState.DUE_COMPLETED || it.dayState == HabitDayState.DUE_INCOMPLETE
        }
        val otherActiveItems = items.filter { it.dayState == HabitDayState.NOT_SCHEDULED }
        val activeCount = items.count { it.dayState != HabitDayState.NOT_APPLICABLE }

        return HabitDailySummary(
            completedDueCount = dueItems.count { it.isCompleted },
            totalDueCount = dueItems.size,
            totalActiveHabitsCount = activeCount,
            dueHabits = dueItems,
            otherActiveHabits = otherActiveItems
        )
    }

    /**
     * Calculates habit consistency across [startDate]..[endDate] inclusive,
     * excluding dates after [today] so future scheduled days are never counted as missed.
     */
    fun calculateHabitPeriodConsistency(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate
    ): HabitPeriodSummary {
        val effectiveEnd = if (endDate.isAfter(today)) today else endDate
        if (effectiveEnd.isBefore(startDate)) {
            return HabitPeriodSummary(
                completedDueOccurrences = 0,
                totalDueOccurrences = 0,
                consistencyPercent = null,
                perHabitConsistency = habits.map {
                    HabitConsistencyItem(it, 0, 0, null)
                }
            )
        }

        val completionLookup = completions
            .filter { it.isCompleted }
            .map { it.habitId to it.completionDate }
            .toSet()

        val dates = generateSequence(startDate) { current ->
            val next = current.plusDays(1)
            if (next.isAfter(effectiveEnd)) null else next
        }.toList()

        val perHabit = habits
            .sortedWith(compareBy<Habit> { it.createdAtEpochMillis }.thenBy { it.id })
            .map { habit ->
                var dueCount = 0
                var completedCount = 0
                for (date in dates) {
                    if (habit.isDueOn(date)) {
                        dueCount++
                        if (completionLookup.contains(habit.id to date)) {
                            completedCount++
                        }
                    }
                }
                val pct = if (dueCount == 0) null else ((completedCount * 100.0) / dueCount).toInt()
                HabitConsistencyItem(
                    habit = habit,
                    completedDueOccurrences = completedCount,
                    totalDueOccurrences = dueCount,
                    consistencyPercent = pct
                )
            }

        val totalDue = perHabit.sumOf { it.totalDueOccurrences }
        val totalCompleted = perHabit.sumOf { it.completedDueOccurrences }
        val overallPct = if (totalDue == 0) null else ((totalCompleted * 100.0) / totalDue).toInt()

        return HabitPeriodSummary(
            completedDueOccurrences = totalCompleted,
            totalDueOccurrences = totalDue,
            consistencyPercent = overallPct,
            perHabitConsistency = perHabit
        )
    }

    /**
     * Calculates Finance income, expense, net for period, and category breakdown
     * using exact integer minor units (docs/08_FINANCE.md Section 12).
     */
    fun calculateFinancePeriodSummary(
        transactions: List<FinanceTransaction>
    ): FinancePeriodSummary {
        var income = 0L
        var expense = 0L
        val categoryMap = linkedMapOf<String, Long>()

        for (tx in transactions) {
            if (tx.amountMinor <= 0L) continue
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountMinor
                TransactionType.EXPENSE -> {
                    expense += tx.amountMinor
                    categoryMap[tx.category] = (categoryMap[tx.category] ?: 0L) + tx.amountMinor
                }
            }
        }

        val sortedCategoryMap = categoryMap.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }

        return FinancePeriodSummary(
            totalIncomeMinor = income,
            totalExpenseMinor = expense,
            netForPeriodMinor = income - expense,
            transactionCount = transactions.size,
            expensesByCategoryMinor = sortedCategoryMap
        )
    }

    /**
     * Compares two equivalent periods safely without dividing by zero.
     */
    fun comparePeriods(currentValue: Long, previousValue: Long): PeriodComparison {
        val delta = currentValue - previousValue
        val pct = if (previousValue == 0L) {
            null
        } else {
            ((delta * 100.0) / previousValue).toInt()
        }
        return PeriodComparison(
            currentValue = currentValue,
            previousValue = previousValue,
            delta = delta,
            percentChange = pct
        )
    }
}
