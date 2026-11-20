package com.lifeanalytics.app.domain.analytics

import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitCompletion
import com.lifeanalytics.app.domain.model.HabitSchedulePeriod
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import com.lifeanalytics.app.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeAnalyticsEngineTest {

    private val refToday = LocalDate.of(2026, 11, 19)

    private fun makeHabit(
        id: String,
        name: String = "Habit $id",
        scheduleType: HabitScheduleType = HabitScheduleType.EVERY_DAY,
        scheduledWeekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        lifeArea: LifeArea? = LifeArea.HEALTH_LIFESTYLE,
        activeFromDate: LocalDate = LocalDate.of(2026, 1, 1),
        deactivatedFromDate: LocalDate? = null,
        isActive: Boolean = true
    ): Habit {
        val period = HabitSchedulePeriod(
            id = "sp_$id",
            habitId = id,
            effectiveFrom = activeFromDate,
            effectiveTo = null,
            scheduleType = scheduleType,
            weekdays = scheduledWeekdays,
            createdAtEpochMillis = 1000L
        )
        return Habit(
            id = id,
            name = name,
            isActive = isActive,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            deactivatedAtEpochMillis = if (deactivatedFromDate != null) 2000L else null,
            deactivatedFromLocalDate = deactivatedFromDate,
            schedulePeriods = listOf(period),
            lifeArea = lifeArea
        )
    }

    private fun makeCompletion(
        id: String,
        habitId: String,
        date: LocalDate,
        isCompleted: Boolean = true
    ): HabitCompletion {
        return HabitCompletion(
            id = id,
            habitId = habitId,
            completionDate = date,
            isCompleted = isCompleted,
            completedAtEpochMillis = if (isCompleted) 1000L else null,
            updatedAtEpochMillis = 1000L
        )
    }

    private fun makeTask(
        id: String,
        plannedDate: LocalDate = refToday,
        isCompleted: Boolean = false,
        lifeArea: LifeArea? = LifeArea.CAREER_PRODUCTIVITY
    ): Task {
        return Task(
            id = id,
            title = "Task $id",
            category = TaskCategory.WORK,
            plannedDate = plannedDate,
            plannedTime = null,
            estimatedDurationMinutes = 30,
            status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PLANNED,
            completedAtEpochMillis = if (isCompleted) 2000L else null,
            sortOrder = 0,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            lifeArea = lifeArea
        )
    }

    private fun makeTx(
        id: String,
        type: TransactionType,
        amountMinor: Long,
        date: LocalDate = refToday
    ): FinanceTransaction {
        return FinanceTransaction(
            id = id,
            type = type,
            amountMinor = amountMinor,
            currencyCode = "INR",
            category = "General",
            transactionDate = date,
            note = null,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
    }

    // -------------------------------------------------------------------------
    // 1. Health & Lifestyle Category Score Tests
    // -------------------------------------------------------------------------

    @Test
    fun `health score returns null when no health habits exist`() {
        val result = LifeAnalyticsEngine.calculateHealthCategory(
            habits = emptyList(),
            completions = emptyList(),
            startDate = refToday,
            endDate = refToday,
            today = refToday
        )
        assertNull(result.score)
        assertFalse(result.hasScheduledData)
    }

    @Test
    fun `health score excludes unclassified and career habits`() {
        val unclassifiedHabit = makeHabit(id = "h_legacy", lifeArea = null)
        val careerHabit = makeHabit(id = "h_career", lifeArea = LifeArea.CAREER_PRODUCTIVITY)

        val result = LifeAnalyticsEngine.calculateHealthCategory(
            habits = listOf(unclassifiedHabit, careerHabit),
            completions = listOf(
                makeCompletion("c1", "h_legacy", refToday, true),
                makeCompletion("c2", "h_career", refToday, true)
            ),
            startDate = refToday,
            endDate = refToday,
            today = refToday
        )
        assertNull("Unclassified and non-health habits must not be counted in Health", result.score)
        assertEquals(0, result.scheduledCount)
    }

    @Test
    fun `health score calculates scheduled adherence and ignores unscheduled dates`() {
        // Thursday 2026-11-19 is a Thursday; schedule habit only on Monday & Thursday
        val monThuHabit = makeHabit(
            id = "h1",
            scheduleType = HabitScheduleType.CUSTOM,
            scheduledWeekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            lifeArea = LifeArea.HEALTH_LIFESTYLE
        )
        val weekStart = LocalDate.of(2026, 11, 16) // Monday
        val completions = listOf(
            makeCompletion("c_mon", "h1", weekStart, true),
            makeCompletion("c_thu", "h1", refToday, false)
        )

        val result = LifeAnalyticsEngine.calculateHealthCategory(
            habits = listOf(monThuHabit),
            completions = completions,
            startDate = weekStart,
            endDate = refToday,
            today = refToday
        )
        assertEquals(2, result.scheduledCount)
        assertEquals(1, result.completedCount)
        assertEquals(50, result.score)
        assertTrue(result.hasScheduledData)
    }

    // -------------------------------------------------------------------------
    // 2. Career & Productivity Category Score Tests (40% Habits / 60% Tasks + Partial)
    // -------------------------------------------------------------------------

    @Test
    fun `career score uses 40-60 weighting when both career habits and career tasks exist`() {
        val careerHabit = makeHabit(id = "ch1", lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        // Habit adherence = 1/2 = 50%
        val start = refToday.minusDays(1)
        val completions = listOf(
            makeCompletion("c1", "ch1", start, true)
        )
        // Task completion = 3/3 = 100%
        val tasks = listOf(
            makeTask("t1", plannedDate = start, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY),
            makeTask("t2", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY),
            makeTask("t3", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        )

        val result = LifeAnalyticsEngine.calculateCareerCategory(
            habits = listOf(careerHabit),
            completions = completions,
            tasks = tasks,
            startDate = start,
            endDate = refToday,
            today = refToday
        )

        // 0.40 * 50 + 0.60 * 100 = 20 + 60 = 80
        assertEquals(50, result.habitScore)
        assertEquals(100, result.taskScore)
        assertEquals(80, result.score)
        assertFalse(result.isPartial)
    }

    @Test
    fun `career score marks partial coverage when only career tasks exist`() {
        val tasks = listOf(
            makeTask("t1", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY),
            makeTask("t2", plannedDate = refToday, isCompleted = false, lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        )
        val result = LifeAnalyticsEngine.calculateCareerCategory(
            habits = emptyList(),
            completions = emptyList(),
            tasks = tasks,
            startDate = refToday,
            endDate = refToday,
            today = refToday
        )
        assertEquals(50, result.score)
        assertTrue(result.isPartial)
        assertFalse(result.hasHabitData)
        assertTrue(result.hasTaskData)
    }

    @Test
    fun `career score marks partial coverage when only career habits exist`() {
        val careerHabit = makeHabit(id = "ch1", lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        val completions = listOf(makeCompletion("c1", "ch1", refToday, true))

        val result = LifeAnalyticsEngine.calculateCareerCategory(
            habits = listOf(careerHabit),
            completions = completions,
            tasks = emptyList(),
            startDate = refToday,
            endDate = refToday,
            today = refToday
        )
        assertEquals(100, result.score)
        assertTrue(result.isPartial)
        assertTrue(result.hasHabitData)
        assertFalse(result.hasTaskData)
    }

    @Test
    fun `career score ignores legacy unclassified tasks`() {
        val legacyTask = makeTask("t_legacy", plannedDate = refToday, isCompleted = true, lifeArea = null)
        val result = LifeAnalyticsEngine.calculateCareerCategory(
            habits = emptyList(),
            completions = emptyList(),
            tasks = listOf(legacyTask),
            startDate = refToday,
            endDate = refToday,
            today = refToday
        )
        assertNull("Unclassified legacy tasks must not be auto-assigned to Career", result.score)
    }

    // -------------------------------------------------------------------------
    // 3. Finance & Wealth Category Score & Savings Rate Tests
    // -------------------------------------------------------------------------

    @Test
    fun `finance category computes savings rate when income is positive and null when income is zero`() {
        val withIncome = LifeAnalyticsEngine.calculateFinanceCategory(
            transactions = listOf(
                makeTx("tx1", TransactionType.INCOME, 100_000L, refToday),
                makeTx("tx2", TransactionType.EXPENSE, 40_000L, refToday)
            ),
            startDate = refToday,
            endDate = refToday,
            today = refToday,
            monthlyBudgetMinor = null
        )
        assertEquals(60_000L, withIncome.netCashFlowMinor)
        assertEquals(60_000L, withIncome.savingsAmountMinor)
        assertEquals(60, withIncome.savingsRatePercent)
        assertNull("Without a user-defined monthly budget, numeric Finance score must be null", withIncome.score)

        val noIncome = LifeAnalyticsEngine.calculateFinanceCategory(
            transactions = listOf(
                makeTx("tx3", TransactionType.EXPENSE, 25_000L, refToday)
            ),
            startDate = refToday,
            endDate = refToday,
            today = refToday,
            monthlyBudgetMinor = null
        )
        assertEquals(-25_000L, noIncome.netCashFlowMinor)
        assertNull("When income is zero, savingsRatePercent must be null", noIncome.savingsRatePercent)
    }

    @Test
    fun `finance category computes budget adherence score only when monthly budget and activity exist`() {
        // Nov 2026 has 30 days; monthly budget 30,000 -> daily budget 1,000
        val withinBudget = LifeAnalyticsEngine.calculateFinanceCategory(
            transactions = listOf(makeTx("tx1", TransactionType.EXPENSE, 800L, refToday)),
            startDate = refToday,
            endDate = refToday,
            today = refToday,
            monthlyBudgetMinor = 30_000L
        )
        assertEquals(100, withinBudget.score)

        // 50% over daily budget (1,500 vs 1,000) -> 50 score
        val overBudget = LifeAnalyticsEngine.calculateFinanceCategory(
            transactions = listOf(makeTx("tx2", TransactionType.EXPENSE, 1_500L, refToday)),
            startDate = refToday,
            endDate = refToday,
            today = refToday,
            monthlyBudgetMinor = 30_000L
        )
        assertEquals(50, overBudget.score)

        // Budget set, but zero transactions in period -> score is null (do not invent 100 for unrecorded days)
        val noActivityWithBudget = LifeAnalyticsEngine.calculateFinanceCategory(
            transactions = emptyList(),
            startDate = refToday,
            endDate = refToday,
            today = refToday,
            monthlyBudgetMinor = 30_000L
        )
        assertNull(noActivityWithBudget.score)
    }

    // -------------------------------------------------------------------------
    // 4. Overall Life Score, Coverage & Period-over-Period Change Tests
    // -------------------------------------------------------------------------

    @Test
    fun `overall life score averages only available categories and reports data coverage`() {
        val healthHabit = makeHabit(id = "hh1", lifeArea = LifeArea.HEALTH_LIFESTYLE)
        // Health = 100
        val completions = listOf(makeCompletion("c1", "hh1", refToday, true))
        // Career = 50 (partial: 1 of 2 tasks done)
        val tasks = listOf(
            makeTask("t1", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY),
            makeTask("t2", plannedDate = refToday, isCompleted = false, lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        )
        // Finance = unavailable (no budget, no transactions)

        val perf = LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = AnalyticsTimeWindow.DAILY,
            anchorDate = refToday,
            today = refToday,
            habits = listOf(healthHabit),
            completions = completions,
            tasks = tasks,
            transactions = emptyList(),
            monthlyBudgetMinor = null,
            computeComparison = true
        )

        assertEquals(2, perf.availableCategoryCount)
        assertEquals("2 of 3 life areas available", perf.dataCoverageText)
        // Equal-weight average of (100, 50) = 75
        assertEquals(75, perf.overallScore)
    }

    @Test
    fun `overall life score computes period-over-period change against previous equivalent period`() {
        val yesterday = refToday.minusDays(1)
        val tasks = listOf(
            makeTask("t_prev", plannedDate = yesterday, isCompleted = false, lifeArea = LifeArea.CAREER_PRODUCTIVITY),
            makeTask("t_curr", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        )

        val perf = LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = AnalyticsTimeWindow.DAILY,
            anchorDate = refToday,
            today = refToday,
            habits = emptyList(),
            completions = emptyList(),
            tasks = tasks,
            transactions = emptyList(),
            monthlyBudgetMinor = null,
            computeComparison = true
        )

        assertEquals(100, perf.overallScore)
        assertEquals(0, perf.previousOverallScore)
        assertEquals(100, perf.changePoints)
    }

    // -------------------------------------------------------------------------
    // 5. 365-Day / 366-Day Year View Tests
    // -------------------------------------------------------------------------

    @Test
    fun `year calendar generates 365 days for non-leap year and 366 for leap year`() {
        val year2026 = LifeAnalyticsEngine.buildYearCalendar(
            year = 2026,
            today = refToday,
            habits = emptyList(),
            completions = emptyList(),
            tasks = emptyList(),
            transactions = emptyList(),
            monthlyBudgetMinor = null
        )
        assertEquals(365, year2026.totalDaysInYear)
        assertEquals(365, year2026.days.size)

        val year2024 = LifeAnalyticsEngine.buildYearCalendar(
            year = 2024,
            today = refToday,
            habits = emptyList(),
            completions = emptyList(),
            tasks = emptyList(),
            transactions = emptyList(),
            monthlyBudgetMinor = null
        )
        assertEquals(366, year2024.totalDaysInYear)
        assertEquals(366, year2024.days.size)
    }

    @Test
    fun `year calendar distinguishes elapsed scored days, elapsed no-data days, and future days`() {
        val tasks = listOf(
            makeTask("t1", plannedDate = refToday, isCompleted = true, lifeArea = LifeArea.CAREER_PRODUCTIVITY)
        )
        val cal = LifeAnalyticsEngine.buildYearCalendar(
            year = 2026,
            today = refToday,
            habits = emptyList(),
            completions = emptyList(),
            tasks = tasks,
            transactions = emptyList(),
            monthlyBudgetMinor = null
        )

        val todayCell = cal.days.first { it.date == refToday }
        val yesterdayCell = cal.days.first { it.date == refToday.minusDays(1) }
        val tomorrowCell = cal.days.first { it.date == refToday.plusDays(1) }

        assertEquals(YearDayState.ELAPSED_SCORED, todayCell.state)
        assertEquals(100, todayCell.overallScore)

        assertEquals(YearDayState.ELAPSED_NO_DATA, yesterdayCell.state)
        assertNull("Missing-data elapsed day must have null score, not 0", yesterdayCell.overallScore)

        assertEquals(YearDayState.FUTURE, tomorrowCell.state)
        assertNull(tomorrowCell.overallScore)

        assertNotNull(cal.averageYearScore)
        assertEquals(1, cal.scoredDaysCount)
    }

    @Test
    fun `category trend series projects area scores and preserves null for missing intervals`() {
        val healthHabit = makeHabit(
            id = "h1",
            lifeArea = LifeArea.HEALTH_LIFESTYLE,
            activeFromDate = refToday.minusDays(2)
        )
        val careerTask = makeTask(
            id = "c1",
            plannedDate = refToday,
            isCompleted = true,
            lifeArea = LifeArea.CAREER_PRODUCTIVITY
        )
        val completions = listOf(
            HabitCompletion(
                id = "comp1",
                habitId = "h1",
                completionDate = refToday,
                isCompleted = true,
                completedAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )

        val healthSeries = LifeAnalyticsEngine.buildCategoryTrendSeries(
            lifeArea = LifeArea.HEALTH_LIFESTYLE,
            window = AnalyticsTimeWindow.DAILY,
            anchorDate = refToday,
            today = refToday,
            habits = listOf(healthHabit),
            completions = completions,
            tasks = listOf(careerTask)
        )
        val careerSeries = LifeAnalyticsEngine.buildCategoryTrendSeries(
            lifeArea = LifeArea.CAREER_PRODUCTIVITY,
            window = AnalyticsTimeWindow.DAILY,
            anchorDate = refToday,
            today = refToday,
            habits = listOf(healthHabit),
            completions = completions,
            tasks = listOf(careerTask)
        )

        assertEquals(7, healthSeries.size)
        assertEquals(100, healthSeries.last().overallScore)
        // Earlier day before habit existed must be null, never 0
        assertNull(healthSeries.first().overallScore)

        assertEquals(7, careerSeries.size)
        assertEquals(100, careerSeries.last().overallScore)
        assertNull(careerSeries.first().overallScore)
    }
}

