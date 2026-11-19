package com.lifeanalytics.app.domain.growth

import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/**
 * Pure Kotlin GrowthEngine implementing Growth Direction rules (docs/14_GROWTH_ENGINE.md).
 * Zero Android UI dependencies.
 */
object GrowthEngine {

    fun evaluate(
        input: GrowthInputData,
        config: GrowthEngineConfig = GrowthEngineConfig()
    ): GrowthDirectionResult {
        val window = input.window
        val today = input.today

        // 1. Determine period date ranges according to completed-days rule (Section 4 & 5)
        val (currStart, currEnd, prevStart, prevEnd) = calculatePeriodBounds(window, today)

        // 2. Check window unlock conditions
        val lockStatus = checkWindowLock(
            window = window,
            today = today,
            currStart = currStart,
            currEnd = currEnd,
            prevStart = prevStart,
            prevEnd = prevEnd,
            recordedDates = input.recordedDates,
            config = config
        )

        val currentLabel = formatPeriodLabel(window, currStart, currEnd, isCurrent = true)
        val previousLabel = formatPeriodLabel(window, prevStart, prevEnd, isCurrent = false)

        if (lockStatus is WindowLockStatus.Locked) {
            val emptyPillar = { name: String ->
                PillarVerdictResult(
                    pillarName = name,
                    verdict = null,
                    currentDisplayValue = "—",
                    previousDisplayValue = "—",
                    deltaPercentPoints = null,
                    reason = lockStatus.reason
                )
            }
            return GrowthDirectionResult(
                window = window,
                lockStatus = lockStatus,
                currentPeriodLabel = currentLabel,
                previousPeriodLabel = previousLabel,
                overallVerdict = OverallVerdict.BUILDING_BASELINE,
                habitsPillar = emptyPillar("Habits"),
                tasksPillar = emptyPillar("Tasks"),
                financePillar = emptyPillar("Finance")
            )
        }

        // 3. Evaluate Habits Pillar
        val habitsPillar = evaluateHabits(
            window = window,
            currentCompleted = input.currentHabitsCompleted,
            currentDue = input.currentHabitsDue,
            previousCompleted = input.previousHabitsCompleted,
            previousDue = input.previousHabitsDue,
            config = config
        )

        // 4. Evaluate Tasks Pillar (with Focus supporting line)
        val tasksPillar = evaluateTasks(
            window = window,
            currentCompleted = input.currentTasksCompleted,
            currentPlanned = input.currentTasksPlanned,
            previousCompleted = input.previousTasksCompleted,
            previousPlanned = input.previousTasksPlanned,
            currentFocusSeconds = input.currentFocusDurationSeconds,
            previousFocusSeconds = input.previousFocusDurationSeconds,
            config = config
        )

        // 5. Evaluate Finance Pillar
        val financePillar = evaluateFinance(
            window = window,
            currentIncomeMinor = input.currentIncomeMinor,
            currentExpenseMinor = input.currentExpenseMinor,
            previousIncomeMinor = input.previousIncomeMinor,
            previousExpenseMinor = input.previousExpenseMinor,
            hasCurrentTransactions = input.currentPeriodHasTransactions,
            previousRecordedDays = input.previousPeriodRecordedDays,
            isCurrentPeriodClosed = input.isCurrentPeriodClosed
        )

        // 6. Evaluate Overall Verdict
        val activeVerdicts = listOfNotNull(habitsPillar.verdict, tasksPillar.verdict, financePillar.verdict)
        val overall = calculateOverallVerdict(activeVerdicts)

        return GrowthDirectionResult(
            window = window,
            lockStatus = lockStatus,
            currentPeriodLabel = currentLabel,
            previousPeriodLabel = previousLabel,
            overallVerdict = overall,
            habitsPillar = habitsPillar,
            tasksPillar = tasksPillar,
            financePillar = financePillar
        )
    }

    /**
     * Determines current and previous period boundaries for each window.
     * Habits and Tasks evaluate completed local days (< today).
     */
    fun calculatePeriodBounds(window: GrowthWindow, today: LocalDate): PeriodBounds {
        return when (window) {
            GrowthWindow.DAY -> {
                val yesterday = today.minusDays(1)
                val dayBefore = today.minusDays(2)
                PeriodBounds(yesterday, yesterday, dayBefore, dayBefore)
            }
            GrowthWindow.WEEK -> {
                val currWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currWeekEnd = today.minusDays(1) // Completed days only (Mon through yesterday)
                val prevWeekStart = currWeekStart.minusWeeks(1)
                val prevWeekEnd = currWeekStart.minusDays(1) // Full Mon-Sun of previous week
                PeriodBounds(currWeekStart, currWeekEnd, prevWeekStart, prevWeekEnd)
            }
            GrowthWindow.MONTH -> {
                val currMonthStart = today.withDayOfMonth(1)
                val currMonthEnd = today.minusDays(1) // 1st through yesterday
                val prevMonthStart = currMonthStart.minusMonths(1)
                val prevMonthEnd = currMonthStart.minusDays(1) // Full previous calendar month
                PeriodBounds(currMonthStart, currMonthEnd, prevMonthStart, prevMonthEnd)
            }
            GrowthWindow.YEAR -> {
                val currYearStart = today.withDayOfYear(1)
                val currYearEnd = today.minusDays(1) // Jan 1 through yesterday
                val prevYearStart = currYearStart.minusYears(1)
                val prevYearEnd = currYearStart.minusDays(1) // Full previous calendar year
                PeriodBounds(currYearStart, currYearEnd, prevYearStart, prevYearEnd)
            }
        }
    }

    private fun checkWindowLock(
        window: GrowthWindow,
        today: LocalDate,
        currStart: LocalDate,
        currEnd: LocalDate,
        prevStart: LocalDate,
        prevEnd: LocalDate,
        recordedDates: Set<LocalDate>,
        config: GrowthEngineConfig
    ): WindowLockStatus {
        when (window) {
            GrowthWindow.DAY -> {
                // At least 1 recorded day on each of the 2 days (yesterday and day before)
                val hasYesterday = recordedDates.contains(currStart)
                val hasDayBefore = recordedDates.contains(prevStart)
                if (!hasYesterday || !hasDayBefore) {
                    val missing = when {
                        !hasYesterday && !hasDayBefore -> "No activity recorded yesterday or the day before"
                        !hasYesterday -> "No activity recorded yesterday"
                        else -> "No activity recorded the day before yesterday"
                    }
                    return WindowLockStatus.Locked(
                        reason = missing,
                        unlockHint = "Unlocks after recording activity for 2 consecutive days"
                    )
                }
            }
            GrowthWindow.WEEK -> {
                // Minimum current: >= 3 completed days in current week
                val completedDaysInCurrentWeek = countDaysBetween(currStart, currEnd).coerceAtLeast(0)
                if (completedDaysInCurrentWeek < config.minCompletedDaysWeek) {
                    val remaining = config.minCompletedDaysWeek - completedDaysInCurrentWeek
                    return WindowLockStatus.Locked(
                        reason = "Needs at least ${config.minCompletedDaysWeek} completed days this week ($completedDaysInCurrentWeek completed)",
                        unlockHint = "Unlocks on Thursday with 3 days recorded this week"
                    )
                }
                // Minimum previous: >= 5 recorded days in previous week
                val prevRecordedCount = countRecordedDaysInRange(prevStart, prevEnd, recordedDates)
                if (prevRecordedCount < config.minRecordedDaysWeek) {
                    return WindowLockStatus.Locked(
                        reason = "Previous week had $prevRecordedCount of ${config.minRecordedDaysWeek} required recorded days",
                        unlockHint = "Requires ≥ 5 recorded days in the previous week"
                    )
                }
            }
            GrowthWindow.MONTH -> {
                // Minimum current: >= 7 completed days in current month
                val completedDaysInCurrentMonth = countDaysBetween(currStart, currEnd).coerceAtLeast(0)
                if (completedDaysInCurrentMonth < config.minCompletedDaysMonth) {
                    return WindowLockStatus.Locked(
                        reason = "Needs at least ${config.minCompletedDaysMonth} completed days this month ($completedDaysInCurrentMonth completed)",
                        unlockHint = "Unlocks on day 8 with 7 days recorded this month"
                    )
                }
                // Minimum previous: >= 14 recorded days in previous month
                val prevRecordedCount = countRecordedDaysInRange(prevStart, prevEnd, recordedDates)
                if (prevRecordedCount < config.minRecordedDaysMonth) {
                    return WindowLockStatus.Locked(
                        reason = "Previous month had $prevRecordedCount of ${config.minRecordedDaysMonth} required recorded days",
                        unlockHint = "Requires ≥ 14 recorded days in the previous month"
                    )
                }
            }
            GrowthWindow.YEAR -> {
                // Minimum current: >= 30 completed days in current year
                val completedDaysInCurrentYear = countDaysBetween(currStart, currEnd).coerceAtLeast(0)
                if (completedDaysInCurrentYear < config.minCompletedDaysYear) {
                    return WindowLockStatus.Locked(
                        reason = "Needs at least ${config.minCompletedDaysYear} completed days this year ($completedDaysInCurrentYear completed)",
                        unlockHint = "Unlocks after January 30 with 30 days recorded this year"
                    )
                }
                // Minimum previous: >= 90 recorded days in previous year
                val prevRecordedCount = countRecordedDaysInRange(prevStart, prevEnd, recordedDates)
                if (prevRecordedCount < config.minRecordedDaysYear) {
                    return WindowLockStatus.Locked(
                        reason = "Previous year had $prevRecordedCount of ${config.minRecordedDaysYear} required recorded days",
                        unlockHint = "Requires ≥ 90 recorded days in the previous year"
                    )
                }
            }
        }
        return WindowLockStatus.Unlocked
    }

    private fun evaluateHabits(
        window: GrowthWindow,
        currentCompleted: Int,
        currentDue: Int,
        previousCompleted: Int,
        previousDue: Int,
        config: GrowthEngineConfig
    ): PillarVerdictResult {
        if (currentDue == 0 && previousDue == 0) {
            return PillarVerdictResult(
                pillarName = "Habits",
                verdict = null,
                currentDisplayValue = "0 due",
                previousDisplayValue = "0 due",
                deltaPercentPoints = null,
                reason = "No habits scheduled in period"
            )
        }

        if (currentDue == 0 || previousDue == 0) {
            return PillarVerdictResult(
                pillarName = "Habits",
                verdict = null,
                currentDisplayValue = if (currentDue > 0) "${(currentCompleted * 100) / currentDue}%" else "0 due",
                previousDisplayValue = if (previousDue > 0) "${(previousCompleted * 100) / previousDue}%" else "0 due",
                deltaPercentPoints = null,
                reason = "No habits scheduled in period"
            )
        }

        val currentRate = (currentCompleted * 100.0) / currentDue
        val previousRate = (previousCompleted * 100.0) / previousDue
        val delta = (currentRate - previousRate).toInt()

        val currPct = currentRate.toInt()
        val prevPct = previousRate.toInt()

        // Rule: Both periods at 100%: Verdict is Steady with reason: "Maintained 100%"
        if (currPct == 100 && prevPct == 100) {
            return PillarVerdictResult(
                pillarName = "Habits",
                verdict = GrowthVerdict.STEADY,
                currentDisplayValue = "100%",
                previousDisplayValue = "100%",
                deltaPercentPoints = 0,
                reason = "Maintained 100%"
            )
        }

        val threshold = if (window == GrowthWindow.DAY) config.dayDeltaThresholdPercent else config.periodDeltaThresholdPercent
        val verdict = when {
            delta >= threshold -> GrowthVerdict.IMPROVING
            delta <= -threshold -> GrowthVerdict.SLIPPING
            else -> GrowthVerdict.STEADY
        }

        val reason = when (verdict) {
            GrowthVerdict.IMPROVING -> "Up $delta% vs previous period"
            GrowthVerdict.SLIPPING -> "Down ${abs(delta)}% vs previous period"
            GrowthVerdict.STEADY -> if (delta == 0) "Same rate as previous period" else "${if (delta > 0) "+$delta%" else "$delta%"} vs previous period"
        }

        return PillarVerdictResult(
            pillarName = "Habits",
            verdict = verdict,
            currentDisplayValue = "$currPct% ($currentCompleted/$currentDue)",
            previousDisplayValue = "$prevPct% ($previousCompleted/$previousDue)",
            deltaPercentPoints = delta,
            reason = reason
        )
    }

    private fun evaluateTasks(
        window: GrowthWindow,
        currentCompleted: Int,
        currentPlanned: Int,
        previousCompleted: Int,
        previousPlanned: Int,
        currentFocusSeconds: Long,
        previousFocusSeconds: Long,
        config: GrowthEngineConfig
    ): PillarVerdictResult {
        val minTasks = when (window) {
            GrowthWindow.DAY -> config.minTasksDay
            GrowthWindow.WEEK -> config.minTasksWeek
            GrowthWindow.MONTH -> config.minTasksMonth
            GrowthWindow.YEAR -> config.minTasksYear
        }

        val supporting = "Focus: ${DateAttributionPolicy.formatDurationSeconds(currentFocusSeconds)} (was ${DateAttributionPolicy.formatDurationSeconds(previousFocusSeconds)})"

        if (currentPlanned < minTasks || previousPlanned < minTasks) {
            val currDisp = if (currentPlanned > 0) "$currentCompleted/$currentPlanned" else "0"
            val prevDisp = if (previousPlanned > 0) "$previousCompleted/$previousPlanned" else "0"
            return PillarVerdictResult(
                pillarName = "Tasks",
                verdict = null,
                currentDisplayValue = currDisp,
                previousDisplayValue = prevDisp,
                deltaPercentPoints = null,
                reason = "Plan more tasks to compare",
                supportingText = supporting
            )
        }

        val currentRate = (currentCompleted * 100.0) / currentPlanned
        val previousRate = (previousCompleted * 100.0) / previousPlanned
        val delta = (currentRate - previousRate).toInt()

        val currPct = currentRate.toInt()
        val prevPct = previousRate.toInt()

        if (currPct == 100 && prevPct == 100) {
            return PillarVerdictResult(
                pillarName = "Tasks",
                verdict = GrowthVerdict.STEADY,
                currentDisplayValue = "100%",
                previousDisplayValue = "100%",
                deltaPercentPoints = 0,
                reason = "Maintained 100%",
                supportingText = supporting
            )
        }

        val threshold = if (window == GrowthWindow.DAY) config.dayDeltaThresholdPercent else config.periodDeltaThresholdPercent
        val verdict = when {
            delta >= threshold -> GrowthVerdict.IMPROVING
            delta <= -threshold -> GrowthVerdict.SLIPPING
            else -> GrowthVerdict.STEADY
        }

        val reason = when (verdict) {
            GrowthVerdict.IMPROVING -> "Up $delta% vs previous period"
            GrowthVerdict.SLIPPING -> "Down ${abs(delta)}% vs previous period"
            GrowthVerdict.STEADY -> if (delta == 0) "Same rate as previous period" else "${if (delta > 0) "+$delta%" else "$delta%"} vs previous period"
        }

        return PillarVerdictResult(
            pillarName = "Tasks",
            verdict = verdict,
            currentDisplayValue = "$currPct% ($currentCompleted/$currentPlanned)",
            previousDisplayValue = "$prevPct% ($previousCompleted/$previousPlanned)",
            deltaPercentPoints = delta,
            reason = reason,
            supportingText = supporting
        )
    }

    private fun evaluateFinance(
        window: GrowthWindow,
        currentIncomeMinor: Long,
        currentExpenseMinor: Long,
        previousIncomeMinor: Long,
        previousExpenseMinor: Long,
        hasCurrentTransactions: Boolean,
        previousRecordedDays: Int,
        isCurrentPeriodClosed: Boolean
    ): PillarVerdictResult {
        val currNetMinor = currentIncomeMinor - currentExpenseMinor
        val prevNetMinor = previousIncomeMinor - previousExpenseMinor

        val currNetFormatted = MoneyUtils.formatMinorUnits(currNetMinor)
        val prevNetFormatted = MoneyUtils.formatMinorUnits(prevNetMinor)

        // Day and Week windows show net and spent amounts with a neutral "vs previous" note, but no verdict.
        if (window == GrowthWindow.DAY || window == GrowthWindow.WEEK) {
            val note = "Spent ${MoneyUtils.formatMinorUnits(currentExpenseMinor)} (prev ${MoneyUtils.formatMinorUnits(previousExpenseMinor)})"
            return PillarVerdictResult(
                pillarName = "Finance",
                verdict = null,
                currentDisplayValue = currNetFormatted,
                previousDisplayValue = prevNetFormatted,
                deltaPercentPoints = null,
                reason = note
            )
        }

        // Section 6.2: Previous period has no transactions or < 14 recorded days -> No verdict ("Not enough data")
        if (previousIncomeMinor == 0L && previousExpenseMinor == 0L || previousRecordedDays < 14) {
            return PillarVerdictResult(
                pillarName = "Finance",
                verdict = null,
                currentDisplayValue = currNetFormatted,
                previousDisplayValue = prevNetFormatted,
                deltaPercentPoints = null,
                reason = "Not enough data"
            )
        }

        // No income recorded in current period: No verdict ("Waiting for income entry")
        if (currentIncomeMinor <= 0L) {
            return PillarVerdictResult(
                pillarName = "Finance",
                verdict = null,
                currentDisplayValue = currNetFormatted,
                previousDisplayValue = prevNetFormatted,
                deltaPercentPoints = null,
                reason = "Waiting for income entry"
            )
        }

        val netDeltaMinor = currNetMinor - prevNetMinor
        val diffFormatted = MoneyUtils.formatMinorUnits(abs(netDeltaMinor))

        if (!isCurrentPeriodClosed) {
            // Current period is open
            return if (currNetMinor > prevNetMinor) {
                PillarVerdictResult(
                    pillarName = "Finance",
                    verdict = GrowthVerdict.IMPROVING,
                    currentDisplayValue = currNetFormatted,
                    previousDisplayValue = prevNetFormatted,
                    deltaPercentPoints = null,
                    reason = "Ahead of last month by $diffFormatted"
                )
            } else {
                val neededFormatted = MoneyUtils.formatMinorUnits(prevNetMinor - currNetMinor)
                PillarVerdictResult(
                    pillarName = "Finance",
                    verdict = null,
                    currentDisplayValue = currNetFormatted,
                    previousDisplayValue = prevNetFormatted,
                    deltaPercentPoints = null,
                    reason = "$neededFormatted more to beat last month"
                )
            }
        } else {
            // Current period is closed
            return when {
                currNetMinor > prevNetMinor -> PillarVerdictResult(
                    pillarName = "Finance",
                    verdict = GrowthVerdict.IMPROVING,
                    currentDisplayValue = currNetFormatted,
                    previousDisplayValue = prevNetFormatted,
                    deltaPercentPoints = null,
                    reason = "Saved $diffFormatted more than previous month"
                )
                currNetMinor < prevNetMinor -> PillarVerdictResult(
                    pillarName = "Finance",
                    verdict = GrowthVerdict.SLIPPING,
                    currentDisplayValue = currNetFormatted,
                    previousDisplayValue = prevNetFormatted,
                    deltaPercentPoints = null,
                    reason = "Saved $diffFormatted less than previous month"
                )
                else -> PillarVerdictResult(
                    pillarName = "Finance",
                    verdict = GrowthVerdict.STEADY,
                    currentDisplayValue = currNetFormatted,
                    previousDisplayValue = prevNetFormatted,
                    deltaPercentPoints = null,
                    reason = "Same net as previous month"
                )
            }
        }
    }

    fun calculateOverallVerdict(verdicts: List<GrowthVerdict>): OverallVerdict {
        if (verdicts.isEmpty()) return OverallVerdict.BUILDING_BASELINE

        val improvingCount = verdicts.count { it == GrowthVerdict.IMPROVING }
        val slippingCount = verdicts.count { it == GrowthVerdict.SLIPPING }

        return when {
            improvingCount >= 1 && slippingCount == 0 -> OverallVerdict.IMPROVING
            slippingCount >= 1 && improvingCount == 0 -> OverallVerdict.SLIPPING
            improvingCount >= 1 && slippingCount >= 1 -> OverallVerdict.MIXED
            else -> OverallVerdict.STEADY
        }
    }

    private fun countRecordedDaysInRange(start: LocalDate, end: LocalDate, recordedDates: Set<LocalDate>): Int {
        if (end.isBefore(start)) return 0
        var count = 0
        var d = start
        while (!d.isAfter(end)) {
            if (recordedDates.contains(d)) count++
            d = d.plusDays(1)
        }
        return count
    }

    private fun countDaysBetween(start: LocalDate, end: LocalDate): Int {
        if (end.isBefore(start)) return 0
        var count = 0
        var d = start
        while (!d.isAfter(end)) {
            count++
            d = d.plusDays(1)
        }
        return count
    }

    private fun formatPeriodLabel(window: GrowthWindow, start: LocalDate, end: LocalDate, isCurrent: Boolean): String {
        return when (window) {
            GrowthWindow.DAY -> if (isCurrent) "Yesterday" else "Day before yesterday"
            GrowthWindow.WEEK -> if (isCurrent) "This week (${DateAttributionPolicy.formatShortDate(start)}–${DateAttributionPolicy.formatShortDate(end)})"
            else "Last week (${DateAttributionPolicy.formatShortDate(start)}–${DateAttributionPolicy.formatShortDate(end)})"
            GrowthWindow.MONTH -> if (isCurrent) "This month (${start.month.name.lowercase().replaceFirstChar { it.uppercase() }})"
            else "Last month (${start.month.name.lowercase().replaceFirstChar { it.uppercase() }})"
            GrowthWindow.YEAR -> if (isCurrent) "This year (${start.year})" else "Last year (${start.year})"
        }
    }
}

data class PeriodBounds(
    val currentStart: LocalDate,
    val currentEnd: LocalDate,
    val previousStart: LocalDate,
    val previousEnd: LocalDate
)
