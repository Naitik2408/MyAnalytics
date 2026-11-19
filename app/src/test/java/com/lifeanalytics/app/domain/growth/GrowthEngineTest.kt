package com.lifeanalytics.app.domain.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GrowthEngineTest {

    private val baseToday = LocalDate.of(2026, 11, 19) // Thursday

    private fun createBaseInput(
        today: LocalDate = baseToday,
        window: GrowthWindow = GrowthWindow.WEEK,
        recordedDates: Set<LocalDate> = run {
            // Include yesterday and day before, plus previous week Mon-Fri (Nov 9..Nov 13) and previous month
            val dates = mutableSetOf<LocalDate>()
            for (i in 1..40) {
                dates.add(today.minusDays(i.toLong()))
            }
            dates
        }
    ): GrowthInputData {
        return GrowthInputData(
            today = today,
            window = window,
            recordedDates = recordedDates,
            currentHabitsCompleted = 10,
            currentHabitsDue = 10,
            previousHabitsCompleted = 8,
            previousHabitsDue = 10,
            currentTasksCompleted = 5,
            currentTasksPlanned = 5,
            previousTasksCompleted = 4,
            previousTasksPlanned = 5,
            currentFocusDurationSeconds = 3600L,
            previousFocusDurationSeconds = 1800L,
            currentIncomeMinor = 500000L,
            currentExpenseMinor = 200000L,
            previousIncomeMinor = 500000L,
            previousExpenseMinor = 300000L,
            currentPeriodHasTransactions = true,
            previousPeriodRecordedDays = 15,
            isCurrentPeriodClosed = false
        )
    }

    // Scenario 1: Boundaries and completed-days rule (Today excluded from Habit/Task completed days)
    @Test
    fun testCompletedDaysRuleExcludesToday() {
        val boundsWeek = GrowthEngine.calculatePeriodBounds(GrowthWindow.WEEK, baseToday)
        assertEquals(baseToday.minusDays(1), boundsWeek.currentEnd)
        assertTrue(!boundsWeek.currentEnd.isAfter(baseToday.minusDays(1)))

        val boundsMonth = GrowthEngine.calculatePeriodBounds(GrowthWindow.MONTH, baseToday)
        assertEquals(baseToday.minusDays(1), boundsMonth.currentEnd)
        assertEquals(LocalDate.of(2026, 11, 1), boundsMonth.currentStart)

        val boundsDay = GrowthEngine.calculatePeriodBounds(GrowthWindow.DAY, baseToday)
        assertEquals(baseToday.minusDays(1), boundsDay.currentStart)
        assertEquals(baseToday.minusDays(1), boundsDay.currentEnd)
        assertEquals(baseToday.minusDays(2), boundsDay.previousStart)
        assertEquals(baseToday.minusDays(2), boundsDay.previousEnd)
    }

    // Scenario 2: Day window unlocking condition (2 consecutive days)
    @Test
    fun testDayWindowLockStatus() {
        val yesterday = baseToday.minusDays(1)
        val dayBefore = baseToday.minusDays(2)

        // Missing yesterday
        val lockedInput = createBaseInput(window = GrowthWindow.DAY, recordedDates = setOf(dayBefore))
        val lockedRes = GrowthEngine.evaluate(lockedInput)
        assertTrue(lockedRes.lockStatus is WindowLockStatus.Locked)
        assertEquals(OverallVerdict.BUILDING_BASELINE, lockedRes.overallVerdict)

        // Both recorded
        val unlockedInput = createBaseInput(window = GrowthWindow.DAY, recordedDates = setOf(yesterday, dayBefore))
        val unlockedRes = GrowthEngine.evaluate(unlockedInput)
        assertTrue(unlockedRes.lockStatus is WindowLockStatus.Unlocked)
    }

    // Scenario 3: Week window unlocking condition (>= 3 completed days in current, >= 5 in previous)
    @Test
    fun testWeekWindowLockStatus() {
        // Monday: today = 2026-11-16. Current week has completed days = 0 (before Mon).
        val monday = LocalDate.of(2026, 11, 16)
        val monInput = createBaseInput(today = monday, window = GrowthWindow.WEEK)
        val monRes = GrowthEngine.evaluate(monInput)
        assertTrue(monRes.lockStatus is WindowLockStatus.Locked)

        // Thursday: today = 2026-11-19. Completed days = 3 (Mon, Tue, Wed).
        // If previous week has only 4 recorded days -> Locked
        val prevWeekFewDays = setOf(
            monday.minusDays(1), monday.minusDays(2), monday.minusDays(3), monday.minusDays(4)
        )
        val lockedPrev = createBaseInput(today = baseToday, window = GrowthWindow.WEEK, recordedDates = prevWeekFewDays)
        assertTrue(GrowthEngine.evaluate(lockedPrev).lockStatus is WindowLockStatus.Locked)

        // Previous week has 5 recorded days -> Unlocked
        val prevWeek5Days = setOf(
            monday.minusDays(1), monday.minusDays(2), monday.minusDays(3), monday.minusDays(4), monday.minusDays(5)
        )
        val unlockedWeek = createBaseInput(today = baseToday, window = GrowthWindow.WEEK, recordedDates = prevWeek5Days)
        assertTrue(GrowthEngine.evaluate(unlockedWeek).lockStatus is WindowLockStatus.Unlocked)
    }

    // Scenario 4: Habit delta threshold (Day: +-15 pts; Week/Month/Year: +-5 pts)
    @Test
    fun testHabitsDeltaThresholds() {
        // Day window: delta = +10 pts -> Steady (< +15)
        val daySteady = createBaseInput(
            window = GrowthWindow.DAY,
            recordedDates = setOf(baseToday.minusDays(1), baseToday.minusDays(2))
        ).copy(
            currentHabitsCompleted = 8, currentHabitsDue = 10, // 80%
            previousHabitsCompleted = 7, previousHabitsDue = 10 // 70%
        )
        assertEquals(GrowthVerdict.STEADY, GrowthEngine.evaluate(daySteady).habitsPillar.verdict)

        // Day window: delta = +20 pts -> Improving (>= +15)
        val dayImproving = daySteady.copy(currentHabitsCompleted = 9) // 90% vs 70%
        assertEquals(GrowthVerdict.IMPROVING, GrowthEngine.evaluate(dayImproving).habitsPillar.verdict)

        // Week window: delta = +10 pts -> Improving (>= +5)
        val weekImproving = createBaseInput(window = GrowthWindow.WEEK).copy(
            currentHabitsCompleted = 8, currentHabitsDue = 10,
            previousHabitsCompleted = 7, previousHabitsDue = 10
        )
        assertEquals(GrowthVerdict.IMPROVING, GrowthEngine.evaluate(weekImproving).habitsPillar.verdict)

        // Week window: delta = -6 pts -> Slipping (<= -5)
        val weekSlipping = createBaseInput(window = GrowthWindow.WEEK).copy(
            currentHabitsCompleted = 74, currentHabitsDue = 100,
            previousHabitsCompleted = 80, previousHabitsDue = 100
        )
        assertEquals(GrowthVerdict.SLIPPING, GrowthEngine.evaluate(weekSlipping).habitsPillar.verdict)
    }

    // Scenario 5: Habits 100% maintenance rule ("Maintained 100%" -> Steady)
    @Test
    fun testHabitsBothAt100Percent() {
        val input = createBaseInput().copy(
            currentHabitsCompleted = 10, currentHabitsDue = 10,
            previousHabitsCompleted = 12, previousHabitsDue = 12
        )
        val res = GrowthEngine.evaluate(input)
        assertEquals(GrowthVerdict.STEADY, res.habitsPillar.verdict)
        assertEquals("Maintained 100%", res.habitsPillar.reason)
    }

    // Scenario 6: Habit zero denominator
    @Test
    fun testHabitsZeroDenominator() {
        val input = createBaseInput().copy(
            currentHabitsCompleted = 0, currentHabitsDue = 0,
            previousHabitsCompleted = 0, previousHabitsDue = 0
        )
        val res = GrowthEngine.evaluate(input)
        assertNull(res.habitsPillar.verdict)
        assertEquals("No habits scheduled in period", res.habitsPillar.reason)
    }

    // Scenario 7: Tasks minimum denominator rule
    @Test
    fun testTasksMinimumDenominator() {
        // Week window requires min 4 tasks
        val lowTasks = createBaseInput(window = GrowthWindow.WEEK).copy(
            currentTasksCompleted = 2, currentTasksPlanned = 3,
            previousTasksCompleted = 4, previousTasksPlanned = 4
        )
        val res = GrowthEngine.evaluate(lowTasks)
        assertNull(res.tasksPillar.verdict)
        assertEquals("Plan more tasks to compare", res.tasksPillar.reason)
        assertNotNull(res.tasksPillar.supportingText)
        assertTrue(res.tasksPillar.supportingText!!.startsWith("Focus:"))
    }

    // Scenario 8: Tasks 100% maintenance rule
    @Test
    fun testTasksBothAt100Percent() {
        val input = createBaseInput(window = GrowthWindow.WEEK).copy(
            currentTasksCompleted = 5, currentTasksPlanned = 5,
            previousTasksCompleted = 6, previousTasksPlanned = 6
        )
        val res = GrowthEngine.evaluate(input)
        assertEquals(GrowthVerdict.STEADY, res.tasksPillar.verdict)
        assertEquals("Maintained 100%", res.tasksPillar.reason)
    }

    // Scenario 9: Finance in Day/Week windows (no verdict, descriptive only)
    @Test
    fun testFinanceDayAndWeekNoVerdict() {
        val weekInput = createBaseInput(window = GrowthWindow.WEEK)
        val res = GrowthEngine.evaluate(weekInput)
        assertNull(res.financePillar.verdict)
        assertTrue(res.financePillar.reason.startsWith("Spent ₹"))
    }

    // Scenario 10: Finance in Month window (Open period vs Closed period)
    @Test
    fun testFinanceMonthVerdict() {
        // Base today is Nov 19. Previous month is Oct 1..Oct 31.
        val monthDates = mutableSetOf<LocalDate>()
        for (i in 1..18) {
            monthDates.add(baseToday.minusDays(i.toLong())) // Nov 1..Nov 18
        }
        for (i in 1..31) {
            monthDates.add(LocalDate.of(2026, 10, i)) // Oct 1..Oct 31 (31 recorded days)
        }

        val monthInput = createBaseInput(window = GrowthWindow.MONTH, recordedDates = monthDates).copy(
            currentIncomeMinor = 1000000L,
            currentExpenseMinor = 400000L, // Net = +600,000
            previousIncomeMinor = 1000000L,
            previousExpenseMinor = 500000L, // Net = +500,000
            previousPeriodRecordedDays = 20,
            isCurrentPeriodClosed = false
        )

        // Open month, ahead of previous net -> Improving
        val resAhead = GrowthEngine.evaluate(monthInput)
        assertEquals(GrowthVerdict.IMPROVING, resAhead.financePillar.verdict)
        assertTrue(resAhead.financePillar.reason.contains("Ahead of last month by"))

        // Open month, behind previous net -> No verdict ("more to beat last month")
        val behindInput = monthInput.copy(currentExpenseMinor = 600000L) // Net = +400,000 vs 500,000
        val resBehind = GrowthEngine.evaluate(behindInput)
        assertNull(resBehind.financePillar.verdict)
        assertTrue(resBehind.financePillar.reason.contains("more to beat last month"))

        // Closed month, saved more -> Improving
        val closedSavedMore = monthInput.copy(isCurrentPeriodClosed = true)
        val resSavedMore = GrowthEngine.evaluate(closedSavedMore)
        assertEquals(GrowthVerdict.IMPROVING, resSavedMore.financePillar.verdict)

        // Closed month, saved less -> Slipping
        val closedSavedLess = monthInput.copy(currentExpenseMinor = 700000L, isCurrentPeriodClosed = true) // Net = +300,000 vs 500,000
        val resSavedLess = GrowthEngine.evaluate(closedSavedLess)
        assertEquals(GrowthVerdict.SLIPPING, resSavedLess.financePillar.verdict)

        // No income recorded -> "Waiting for income entry"
        val noIncome = monthInput.copy(currentIncomeMinor = 0L)
        val resNoIncome = GrowthEngine.evaluate(noIncome)
        assertNull(resNoIncome.financePillar.verdict)
        assertEquals("Waiting for income entry", resNoIncome.financePillar.reason)
    }

    // Scenario 11: Overall verdict aggregation combinations
    @Test
    fun testOverallVerdictAggregation() {
        assertEquals(OverallVerdict.BUILDING_BASELINE, GrowthEngine.calculateOverallVerdict(emptyList()))
        assertEquals(OverallVerdict.IMPROVING, GrowthEngine.calculateOverallVerdict(listOf(GrowthVerdict.IMPROVING, GrowthVerdict.STEADY)))
        assertEquals(OverallVerdict.SLIPPING, GrowthEngine.calculateOverallVerdict(listOf(GrowthVerdict.SLIPPING, GrowthVerdict.STEADY)))
        assertEquals(OverallVerdict.MIXED, GrowthEngine.calculateOverallVerdict(listOf(GrowthVerdict.IMPROVING, GrowthVerdict.SLIPPING)))
        assertEquals(OverallVerdict.STEADY, GrowthEngine.calculateOverallVerdict(listOf(GrowthVerdict.STEADY, GrowthVerdict.STEADY)))
    }

    // Scenario 12: Supporting line for Focus under Tasks
    @Test
    fun testFocusSupportingLineUnderTasks() {
        val input = createBaseInput(window = GrowthWindow.WEEK).copy(
            currentFocusDurationSeconds = 7200L, // 2h
            previousFocusDurationSeconds = 3600L  // 1h
        )
        val res = GrowthEngine.evaluate(input)
        assertEquals("Focus: 2h (was 1h)", res.tasksPillar.supportingText)
    }
}
