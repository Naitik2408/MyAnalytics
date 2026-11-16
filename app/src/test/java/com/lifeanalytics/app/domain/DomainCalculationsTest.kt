package com.lifeanalytics.app.domain

import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.FocusPauseInterval
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.FocusSessionType
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitCompletion
import com.lifeanalytics.app.domain.model.HabitDayState
import com.lifeanalytics.app.domain.model.HabitSchedulePeriod
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainCalculationsTest {

    @Test
    fun focusDuration_excludesMultiplePausesAndClampsNegative() {
        // Example from docs/06_ACTIVE_WORK_FOCUS.md Section 11.2:
        // Start 10:00, Pause 10:25..10:35 (10m), Finish 11:10 -> 70m wall, 10m pause = 60m (3600s)
        val base = 1_700_000_000_000L
        val start = base
        val pauseStart = base + 25 * 60_000L
        val pauseEnd = base + 35 * 60_000L
        val finish = base + 70 * 60_000L

        val pauses = listOf(
            FocusPauseInterval(
                id = "p1",
                sessionId = "s1",
                pausedAtEpochMillis = pauseStart,
                resumedAtEpochMillis = pauseEnd
            )
        )
        val duration = DomainCalculations.calculateFocusedDurationSeconds(
            startedAtEpochMillis = start,
            endOrNowEpochMillis = finish,
            pauses = pauses
        )
        assertEquals(3600L, duration)
        assertEquals("1h", DateAttributionPolicy.formatSummaryDuration(duration))

        // Negative or inverted clock timestamps must clamp to 0
        val negative = DomainCalculations.calculateFocusedDurationSeconds(
            startedAtEpochMillis = finish,
            endOrNowEpochMillis = start,
            pauses = emptyList()
        )
        assertEquals(0L, negative)
    }

    @Test
    fun focusInterruption_detectsUnconfirmedProcessGapAndClockJump() {
        val start = 1_700_000_000_000L
        val confirmedAt = start + 10 * 60_000L
        val activeSession = FocusSession(
            id = "s1",
            taskId = null,
            taskTitleSnapshot = null,
            sessionType = FocusSessionType.GENERAL,
            description = "Deep work",
            startedAtEpochMillis = start,
            startedLocalDate = LocalDate.of(2026, 11, 16),
            timezoneId = "Asia/Kolkata",
            endedAtEpochMillis = null,
            status = FocusSessionStatus.ACTIVE,
            focusedDurationSeconds = 600L,
            lastConfirmedAtEpochMillis = confirmedAt,
            interruptionReason = null,
            pauses = emptyList(),
            createdAtEpochMillis = start,
            updatedAtEpochMillis = confirmedAt
        )

        // Normal heartbeat within 30 seconds -> no interruption
        assertNull(
            DomainCalculations.checkSessionInterruptionReason(
                session = activeSession,
                nowEpochMillis = confirmedAt + 30_000L
            )
        )

        // Process killed / reboot gap of 20 minutes -> flagged as interrupted
        val gapReason = DomainCalculations.checkSessionInterruptionReason(
            session = activeSession,
            nowEpochMillis = confirmedAt + 20 * 60_000L
        )
        assertNotNull(gapReason)
        assertEquals(600L, DomainCalculations.calculateConfirmedFocusDurationSeconds(activeSession))

        // Backward clock jump -> flagged as interrupted
        val clockReason = DomainCalculations.checkSessionInterruptionReason(
            session = activeSession,
            nowEpochMillis = start - 60_000L
        )
        assertNotNull(clockReason)
    }

    @Test
    fun taskSummary_distinguishesNoEstimatesFromZeroAndOrdersIncompleteFirst() {
        val date = LocalDate.of(2026, 11, 16)
        val t1 = Task(
            id = "1",
            title = "Completed task",
            category = TaskCategory.STUDY,
            plannedDate = date,
            plannedTime = LocalTime.of(9, 0),
            estimatedDurationMinutes = null,
            status = TaskStatus.COMPLETED,
            completedAtEpochMillis = 100L,
            sortOrder = 0,
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 100L
        )
        val t2 = Task(
            id = "2",
            title = "Incomplete unscheduled",
            category = TaskCategory.PERSONAL,
            plannedDate = date,
            plannedTime = null,
            estimatedDurationMinutes = null,
            status = TaskStatus.PLANNED,
            completedAtEpochMillis = null,
            sortOrder = 1,
            createdAtEpochMillis = 20L,
            updatedAtEpochMillis = 20L
        )
        val t3 = Task(
            id = "3",
            title = "Incomplete scheduled",
            category = TaskCategory.WORK,
            plannedDate = date,
            plannedTime = LocalTime.of(10, 0),
            estimatedDurationMinutes = null,
            status = TaskStatus.PLANNED,
            completedAtEpochMillis = null,
            sortOrder = 2,
            createdAtEpochMillis = 30L,
            updatedAtEpochMillis = 30L
        )

        val sorted = DomainCalculations.sortTasksForDay(listOf(t1, t2, t3))
        assertEquals(listOf("3", "2", "1"), sorted.map { it.id })

        val summaryNoEstimates = DomainCalculations.calculateTaskSummary(sorted)
        assertEquals(1, summaryNoEstimates.completedCount)
        assertEquals(3, summaryNoEstimates.totalCount)
        assertNull(summaryNoEstimates.totalEstimatedMinutes)

        val summaryWithEstimates = DomainCalculations.calculateTaskSummary(
            listOf(t1.copy(estimatedDurationMinutes = 45), t3.copy(estimatedDurationMinutes = 60))
        )
        assertEquals(105, summaryWithEstimates.totalEstimatedMinutes)
    }

    @Test
    fun habitScheduleHistory_preservesPastDueDatesAndExcludesUnscheduledDays() {
        // Habit scheduled Mon/Wed/Fri from Nov 2..Nov 8, then changed to Every Day from Nov 9 onward
        val habitId = "h1"
        val period1 = HabitSchedulePeriod(
            id = "p1",
            habitId = habitId,
            effectiveFrom = LocalDate.of(2026, 11, 2), // Monday
            effectiveTo = LocalDate.of(2026, 11, 9), // Exclusive
            scheduleType = HabitScheduleType.CUSTOM,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtEpochMillis = 1000L
        )
        val period2 = HabitSchedulePeriod(
            id = "p2",
            habitId = habitId,
            effectiveFrom = LocalDate.of(2026, 11, 9), // Monday
            effectiveTo = null,
            scheduleType = HabitScheduleType.EVERY_DAY,
            weekdays = DayOfWeek.entries.toSet(),
            createdAtEpochMillis = 2000L
        )
        val habit = Habit(
            id = habitId,
            name = "Morning walk",
            isActive = true,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 2000L,
            deactivatedAtEpochMillis = null,
            deactivatedFromLocalDate = null,
            schedulePeriods = listOf(period1, period2)
        )

        // Tuesday Nov 3 was NOT scheduled under period1, even though period2 is Every Day
        val tueNov3 = LocalDate.of(2026, 11, 3)
        val tueSummary = DomainCalculations.evaluateHabitsForDate(listOf(habit), emptyList(), tueNov3)
        assertEquals(0, tueSummary.totalDueCount)
        assertEquals(1, tueSummary.otherActiveHabits.size)
        assertEquals(HabitDayState.NOT_SCHEDULED, tueSummary.otherActiveHabits.first().dayState)

        // Tuesday Nov 10 IS scheduled under period2
        val tueNov10 = LocalDate.of(2026, 11, 10)
        val tue10Summary = DomainCalculations.evaluateHabitsForDate(listOf(habit), emptyList(), tueNov10)
        assertEquals(1, tue10Summary.totalDueCount)

        // Consistency for Nov 2..Nov 8 (3 due days: Mon Nov 2, Wed Nov 4, Fri Nov 6)
        val completions = listOf(
            HabitCompletion("c1", habitId, LocalDate.of(2026, 11, 2), true, 100L, 100L),
            HabitCompletion("c2", habitId, LocalDate.of(2026, 11, 4), true, 200L, 200L)
        )
        val consistency = DomainCalculations.calculateHabitPeriodConsistency(
            habits = listOf(habit),
            completions = completions,
            startDate = LocalDate.of(2026, 11, 2),
            endDate = LocalDate.of(2026, 11, 8),
            today = LocalDate.of(2026, 11, 16)
        )
        assertEquals(2, consistency.completedDueOccurrences)
        assertEquals(3, consistency.totalDueOccurrences)
        assertEquals(66, consistency.consistencyPercent)
    }

    @Test
    fun moneyAndFinanceCalculations_useExactIntegerMinorUnits() {
        assertEquals(35000L, MoneyUtils.parseToMinorUnitsOrNull("350", SupportedCurrency.INR))
        assertEquals(35050L, MoneyUtils.parseToMinorUnitsOrNull("350.50", SupportedCurrency.INR))
        assertNull(MoneyUtils.parseToMinorUnitsOrNull("0", SupportedCurrency.INR))
        assertNull(MoneyUtils.parseToMinorUnitsOrNull("-10", SupportedCurrency.INR))
        assertNull(MoneyUtils.parseToMinorUnitsOrNull("12.345", SupportedCurrency.INR))
        assertNull(MoneyUtils.parseToMinorUnitsOrNull("₹100", SupportedCurrency.INR))

        val txs = listOf(
            FinanceTransaction("1", TransactionType.INCOME, 25000_00L, "INR", "Salary", LocalDate.of(2026, 11, 1), null, 1L, 1L),
            FinanceTransaction("2", TransactionType.EXPENSE, 450_50L, "INR", "Food", LocalDate.of(2026, 11, 15), null, 2L, 2L),
            FinanceTransaction("3", TransactionType.EXPENSE, 120_25L, "INR", "Transport", LocalDate.of(2026, 11, 16), null, 3L, 3L)
        )
        val summary = DomainCalculations.calculateFinancePeriodSummary(txs)
        assertEquals(25000_00L, summary.totalIncomeMinor)
        assertEquals(570_75L, summary.totalExpenseMinor)
        assertEquals(24429_25L, summary.netForPeriodMinor)
        assertTrue(MoneyUtils.formatMinorUnits(summary.totalExpenseMinor, "INR").contains("570.75"))
    }
}
