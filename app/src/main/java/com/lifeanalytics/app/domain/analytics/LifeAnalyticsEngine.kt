package com.lifeanalytics.app.domain.analytics

import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitCompletion
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Authoritative rule-based Life Analytics Engine implementing the 19 November 2026
 * Product Decisions specification (`docs/01` through `docs/13` & `AGENTS.md`).
 *
 * Principles:
 * - Strict separation between raw tracking records and the 3 V1 Life Areas:
 *   `HEALTH_LIFESTYLE`, `CAREER_PRODUCTIVITY`, and `FINANCE_WEALTH`.
 * - Legacy records with `lifeArea == null` are never auto-guessed into a specific life area.
 * - Never substitutes zero for missing or untracked data.
 * - Career score combines 40% Career-habit adherence and 60% Task completion rate when both
 *   have eligible data; falls back to 100% of the available component and flags `isPartial = true`
 *   when one component has no eligible data.
 * - Finance score is computed ONLY when a valid user-defined budget exists and finance activity
 *   is recorded in the period; otherwise displays financial metrics without a numeric score.
 * - Overall Life Score is the equal-weight average of available category scores (1, 2, or 3).
 */
object LifeAnalyticsEngine {

    const val CAREER_HABIT_WEIGHT = 0.40
    const val CAREER_TASK_WEIGHT = 0.60

    /**
     * Evaluates Health & Lifestyle score for [startDate]..[endDate] inclusive (clamped to [today]).
     * Based primarily on scheduled `HEALTH_LIFESTYLE` habit adherence:
     * `(completed scheduled health habits / total scheduled health habits) * 100`.
     */
    fun calculateHealthCategory(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task> = emptyList(),
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate
    ): HealthCategoryResult {
        val healthHabits = habits.filter { it.lifeArea == LifeArea.HEALTH_LIFESTYLE }
        val activeHealthHabitCount = healthHabits.count { it.isActive }
        val habitSummary = DomainCalculations.calculateHabitPeriodConsistency(
            habits = healthHabits,
            completions = completions,
            startDate = startDate,
            endDate = endDate,
            today = today
        )

        val effectiveEnd = if (endDate.isAfter(today)) today else endDate
        val healthTasksInPeriod = if (effectiveEnd.isBefore(startDate)) {
            emptyList()
        } else {
            tasks.filter {
                it.lifeArea == LifeArea.HEALTH_LIFESTYLE &&
                    !it.plannedDate.isBefore(startDate) &&
                    !it.plannedDate.isAfter(effectiveEnd)
            }
        }
        val healthTasksDone = healthTasksInPeriod.count { it.isCompleted }
        val healthTasksTotal = healthTasksInPeriod.size

        val totalScheduled = habitSummary.totalDueOccurrences
        val completedScheduled = habitSummary.completedDueOccurrences
        val score = habitSummary.consistencyPercent

        val statusLabel = when {
            healthHabits.isEmpty() -> "No Health & Lifestyle habits classified yet"
            totalScheduled == 0 -> "0 scheduled occurrences in period"
            else -> "$completedScheduled / $totalScheduled scheduled health habits ($score%)"
        }

        return HealthCategoryResult(
            score = score,
            completedScheduledHabits = completedScheduled,
            totalScheduledHabits = totalScheduled,
            activeHabitCount = activeHealthHabitCount,
            healthTasksCompleted = healthTasksDone,
            healthTasksPlanned = healthTasksTotal,
            statusLabel = statusLabel
        )
    }

    /**
     * Evaluates Career & Productivity score for [startDate]..[endDate] inclusive (clamped to [today]).
     */
    fun calculateCareerCategory(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate
    ): CareerCategoryResult {
        val effectiveEnd = if (endDate.isAfter(today)) today else endDate
        val careerHabits = habits.filter { it.lifeArea == LifeArea.CAREER_PRODUCTIVITY }
        val habitSummary = DomainCalculations.calculateHabitPeriodConsistency(
            habits = careerHabits,
            completions = completions,
            startDate = startDate,
            endDate = endDate,
            today = today
        )

        val careerTasks = if (effectiveEnd.isBefore(startDate)) {
            emptyList()
        } else {
            tasks.filter {
                it.lifeArea == LifeArea.CAREER_PRODUCTIVITY &&
                    !it.plannedDate.isBefore(startDate) &&
                    !it.plannedDate.isAfter(effectiveEnd)
            }
        }

        val completedTasks = careerTasks.count { it.isCompleted }
        val totalPlannedTasks = careerTasks.size
        val taskCompletionPct = if (totalPlannedTasks > 0) {
            ((completedTasks * 100.0) / totalPlannedTasks).roundToInt().coerceIn(0, 100)
        } else {
            null
        }

        val habitAdherencePct = habitSummary.consistencyPercent

        val periodSessions = if (effectiveEnd.isBefore(startDate)) {
            emptyList()
        } else {
            sessions.filter {
                it.status == FocusSessionStatus.COMPLETED &&
                    !it.startedLocalDate.isBefore(startDate) &&
                    !it.startedLocalDate.isAfter(effectiveEnd)
            }
        }
        val focusSeconds = DomainCalculations.sumCompletedFocusDurationSeconds(periodSessions)

        val (score, isPartial, mode, label) = when {
            habitAdherencePct != null && taskCompletionPct != null -> {
                val combined = (CAREER_HABIT_WEIGHT * habitAdherencePct + CAREER_TASK_WEIGHT * taskCompletionPct)
                    .roundToInt()
                    .coerceIn(0, 100)
                Quad(
                    combined,
                    false,
                    CareerCoverageMode.FULL_40_60,
                    "Habits ${habitSummary.completedDueOccurrences}/${habitSummary.totalDueOccurrences} (40%) • Tasks $completedTasks/$totalPlannedTasks (60%)"
                )
            }
            habitAdherencePct != null && taskCompletionPct == null -> {
                Quad(
                    habitAdherencePct,
                    true,
                    CareerCoverageMode.PARTIAL_HABITS_ONLY,
                    "Partial (career habits only: ${habitSummary.completedDueOccurrences}/${habitSummary.totalDueOccurrences}) — no planned career tasks"
                )
            }
            habitAdherencePct == null && taskCompletionPct != null -> {
                Quad(
                    taskCompletionPct,
                    true,
                    CareerCoverageMode.PARTIAL_TASKS_ONLY,
                    "Partial (career tasks only: $completedTasks/$totalPlannedTasks) — no scheduled career habits"
                )
            }
            else -> {
                Quad(
                    null,
                    false,
                    CareerCoverageMode.INSUFFICIENT_DATA,
                    "No classified Career & Productivity habits or tasks in period"
                )
            }
        }

        return CareerCategoryResult(
            score = score,
            isPartial = isPartial,
            coverageMode = mode,
            careerHabitAdherencePercent = habitAdherencePct,
            completedCareerHabits = habitSummary.completedDueOccurrences,
            totalScheduledCareerHabits = habitSummary.totalDueOccurrences,
            taskCompletionPercent = taskCompletionPct,
            completedTasks = completedTasks,
            totalPlannedTasks = totalPlannedTasks,
            focusDurationSeconds = focusSeconds,
            statusLabel = label
        )
    }

    /**
     * Evaluates Finance & Wealth metrics and optional goal/budget-aware score for [startDate]..[endDate].
     */
    fun calculateFinanceCategory(
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long? = null,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate
    ): FinanceCategoryResult {
        val effectiveEnd = if (endDate.isAfter(today)) today else endDate
        val periodTransactions = if (effectiveEnd.isBefore(startDate)) {
            emptyList()
        } else {
            transactions.filter {
                !it.transactionDate.isBefore(startDate) && !it.transactionDate.isAfter(effectiveEnd)
            }
        }

        val summary = DomainCalculations.calculateFinancePeriodSummary(periodTransactions)
        val validMonthlyBudget = monthlyBudgetMinor?.takeIf { it > 0L }

        val elapsedDays = if (effectiveEnd.isBefore(startDate)) {
            0L
        } else {
            ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1L
        }

        val periodBudgetTarget = if (validMonthlyBudget != null && elapsedDays > 0L) {
            val isExactFullMonth = startDate.dayOfMonth == 1 &&
                endDate == YearMonth.from(startDate).atEndOfMonth()
            if (isExactFullMonth) {
                validMonthlyBudget
            } else {
                val monthDays = YearMonth.from(startDate).lengthOfMonth().coerceAtLeast(28)
                ((validMonthlyBudget.toDouble() * elapsedDays.toDouble()) / monthDays.toDouble())
                    .roundToInt()
                    .toLong()
                    .coerceAtLeast(1L)
            }
        } else {
            null
        }

        val budgetUsedPct = if (periodBudgetTarget != null && periodBudgetTarget > 0L) {
            ((summary.totalExpenseMinor * 100.0) / periodBudgetTarget).roundToInt()
        } else {
            null
        }

        val financeScore = if (periodBudgetTarget != null && periodBudgetTarget > 0L && periodTransactions.isNotEmpty()) {
            if (summary.totalExpenseMinor <= periodBudgetTarget) {
                100
            } else {
                val overageRatio = (summary.totalExpenseMinor - periodBudgetTarget).toDouble() / periodBudgetTarget.toDouble()
                (100.0 - (overageRatio * 100.0)).roundToInt().coerceIn(0, 100)
            }
        } else {
            null
        }

        val statusLabel = when {
            validMonthlyBudget == null && periodTransactions.isEmpty() ->
                "No transactions or budget target in period"
            validMonthlyBudget == null ->
                "Set a monthly budget target in Finance to enable Finance score"
            periodTransactions.isEmpty() ->
                "No finance transactions recorded in this period"
            else ->
                "Budget used: ${budgetUsedPct ?: 0}% ($financeScore pts)"
        }

        return FinanceCategoryResult(
            score = financeScore,
            incomeMinor = summary.totalIncomeMinor,
            expensesMinor = summary.totalExpenseMinor,
            netCashFlowMinor = summary.netForPeriodMinor,
            savingsAmountMinor = summary.savingsAmountMinor,
            savingsRatePercent = summary.savingsRatePercent,
            periodBudgetTargetMinor = periodBudgetTarget,
            budgetUsedPercent = budgetUsedPct,
            transactionCount = summary.transactionCount,
            statusLabel = statusLabel
        )
    }

    /**
     * Window-based overload for evaluating overall and per-category life performance
     * for [window] anchored at [anchorDate].
     */
    fun evaluatePeriodPerformance(
        window: AnalyticsTimeWindow,
        anchorDate: LocalDate,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long?,
        computeComparison: Boolean = true
    ): PeriodLifePerformance {
        val (startDate, endDate) = when (window) {
            AnalyticsTimeWindow.DAILY -> anchorDate to anchorDate
            AnalyticsTimeWindow.WEEKLY -> DateAttributionPolicy.isoWeekRange(anchorDate)
            AnalyticsTimeWindow.MONTHLY -> DateAttributionPolicy.monthRange(YearMonth.from(anchorDate))
            AnalyticsTimeWindow.YEARLY -> LocalDate.of(anchorDate.year, 1, 1) to LocalDate.of(anchorDate.year, 12, 31)
        }
        val periodLabel = when (window) {
            AnalyticsTimeWindow.DAILY -> DateAttributionPolicy.formatDisplayDate(startDate)
            AnalyticsTimeWindow.WEEKLY -> "${DateAttributionPolicy.formatShortDate(startDate)} – ${DateAttributionPolicy.formatShortDate(endDate)}"
            AnalyticsTimeWindow.MONTHLY -> DateAttributionPolicy.formatMonthYear(YearMonth.from(startDate))
            AnalyticsTimeWindow.YEARLY -> "${startDate.year}"
        }
        val comparisonLabel = when (window) {
            AnalyticsTimeWindow.DAILY -> "vs previous day"
            AnalyticsTimeWindow.WEEKLY -> "vs previous week"
            AnalyticsTimeWindow.MONTHLY -> "vs previous month"
            AnalyticsTimeWindow.YEARLY -> "vs previous year"
        }

        return evaluatePeriodPerformance(
            habits = habits,
            completions = completions,
            tasks = tasks,
            sessions = sessions,
            transactions = transactions,
            monthlyBudgetMinor = monthlyBudgetMinor,
            startDate = startDate,
            endDate = endDate,
            today = today,
            includePreviousComparison = computeComparison,
            periodLabel = periodLabel,
            comparisonLabel = comparisonLabel
        )
    }

    /**
     * Evaluates overall and per-category life performance for [startDate]..[endDate],
     * comparing against the equivalent previous period.
     */
    fun evaluatePeriodPerformance(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long?,
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate,
        includePreviousComparison: Boolean = true,
        periodLabel: String = "",
        comparisonLabel: String = "vs previous period"
    ): PeriodLifePerformance {
        val health = calculateHealthCategory(
            habits = habits,
            completions = completions,
            tasks = tasks,
            startDate = startDate,
            endDate = endDate,
            today = today
        )
        val career = calculateCareerCategory(
            habits = habits,
            completions = completions,
            tasks = tasks,
            sessions = sessions,
            startDate = startDate,
            endDate = endDate,
            today = today
        )
        val finance = calculateFinanceCategory(
            transactions = transactions,
            monthlyBudgetMinor = monthlyBudgetMinor,
            startDate = startDate,
            endDate = endDate,
            today = today
        )

        val availableScores = listOfNotNull(health.score, career.score, finance.score)
        val availableCount = availableScores.size
        val overallScore = if (availableScores.isNotEmpty()) {
            availableScores.average().roundToInt().coerceIn(0, 100)
        } else {
            null
        }

        val coverageLabel = when (availableCount) {
            0 -> "Not enough data yet (0 of 3 life areas)"
            3 -> "All 3 of 3 life areas tracked"
            else -> "$availableCount of 3 life areas available"
        }

        // Elapsed days and consistency (days in period with recorded adherence/completion)
        val effectiveEnd = if (endDate.isAfter(today)) today else endDate
        val elapsedDays = if (effectiveEnd.isBefore(startDate)) {
            0
        } else {
            (ChronoUnit.DAYS.between(startDate, effectiveEnd).toInt() + 1).coerceAtLeast(0)
        }

        var activeTrackedDays = 0
        if (elapsedDays > 0) {
            val completedHabitDates = completions.filter { it.isCompleted }.map { it.completionDate }.toSet()
            val completedTaskDates = tasks.filter { it.isCompleted }.map { it.plannedDate }.toSet()
            val focusDates = sessions.filter { it.status == FocusSessionStatus.COMPLETED }.map { it.startedLocalDate }.toSet()
            val txDates = transactions.map { it.transactionDate }.toSet()

            var cursor = startDate
            while (!cursor.isAfter(effectiveEnd)) {
                val hasActivity = completedHabitDates.contains(cursor) ||
                    completedTaskDates.contains(cursor) ||
                    focusDates.contains(cursor) ||
                    txDates.contains(cursor)
                if (hasActivity) activeTrackedDays++
                cursor = cursor.plusDays(1)
            }
        }
        val consistencyPct = if (elapsedDays > 0 && (availableCount > 0 || activeTrackedDays > 0)) {
            ((activeTrackedDays * 100.0) / elapsedDays).roundToInt().coerceIn(0, 100)
        } else {
            null
        }

        var prevOverallScore: Int? = null
        var deltaPoints: Int? = null
        if (includePreviousComparison && elapsedDays > 0) {
            val spanDays = ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1L
            val prevEnd = startDate.minusDays(1)
            val prevStart = prevEnd.minusDays(spanDays - 1L)
            val prevEval = evaluatePeriodPerformance(
                habits = habits,
                completions = completions,
                tasks = tasks,
                sessions = sessions,
                transactions = transactions,
                monthlyBudgetMinor = monthlyBudgetMinor,
                startDate = prevStart,
                endDate = prevEnd,
                today = today,
                includePreviousComparison = false
            )
            prevOverallScore = prevEval.overallScore
            if (overallScore != null && prevOverallScore != null) {
                deltaPoints = overallScore - prevOverallScore
            }
        }

        val resolvedPeriodLabel = periodLabel.ifBlank {
            if (startDate == endDate) {
                DateAttributionPolicy.formatDisplayDate(startDate)
            } else {
                "${DateAttributionPolicy.formatShortDate(startDate)} – ${DateAttributionPolicy.formatShortDate(endDate)}"
            }
        }

        return PeriodLifePerformance(
            startDate = startDate,
            endDate = endDate,
            periodLabel = resolvedPeriodLabel,
            comparisonLabel = comparisonLabel,
            overallScore = overallScore,
            previousOverallScore = prevOverallScore,
            changeVsPreviousPoints = deltaPoints,
            availableCategoriesCount = availableCount,
            totalCategoriesCount = 3,
            coverageLabel = coverageLabel,
            consistencyPercent = consistencyPct,
            activeTrackedDays = activeTrackedDays,
            elapsedDaysInPeriod = elapsedDays,
            health = health,
            career = career,
            finance = finance
        )
    }

    /**
     * Builds the Performance-Over-Time trend line series for Daily (last 7 days),
     * Weekly (last 6 weeks), Monthly (last 6 months), or Yearly (12 months of current year).
     * Never fabricates data: buckets with no eligible category data have `overallScore = null`.
     */
    fun buildPerformanceTrendSeries(
        window: AnalyticsTimeWindow,
        anchorDate: LocalDate,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long?
    ): List<PerformanceTrendPoint> {
        val clampedAnchor = if (anchorDate.isAfter(today)) today else anchorDate
        val buckets = when (window) {
            AnalyticsTimeWindow.DAILY -> {
                (6L downTo 0L).map { offset ->
                    val d = clampedAnchor.minusDays(offset)
                    val label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    Triple(label, d, d)
                }
            }
            AnalyticsTimeWindow.WEEKLY -> {
                val (currWeekStart, _) = DateAttributionPolicy.isoWeekRange(clampedAnchor)
                (5L downTo 0L).map { weekOffset ->
                    val wStart = currWeekStart.minusWeeks(weekOffset)
                    val wEnd = wStart.plusDays(6)
                    val label = "${wStart.dayOfMonth} ${wStart.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
                    Triple(label, wStart, wEnd)
                }
            }
            AnalyticsTimeWindow.MONTHLY -> {
                val currYm = YearMonth.from(clampedAnchor)
                (5L downTo 0L).map { monthOffset ->
                    val ym = currYm.minusMonths(monthOffset)
                    val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    Triple(label, ym.atDay(1), ym.atEndOfMonth())
                }
            }
            AnalyticsTimeWindow.YEARLY -> {
                val year = clampedAnchor.year
                (1..12).map { month ->
                    val ym = YearMonth.of(year, month)
                    val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    Triple(label, ym.atDay(1), ym.atEndOfMonth())
                }
            }
        }

        return buckets.map { (label, bStart, bEnd) ->
            if (bStart.isAfter(today)) {
                PerformanceTrendPoint(
                    label = label,
                    startDate = bStart,
                    endDate = bEnd,
                    overallScore = null,
                    healthScore = null,
                    careerScore = null,
                    financeScore = null
                )
            } else {
                val perf = evaluatePeriodPerformance(
                    habits = habits,
                    completions = completions,
                    tasks = tasks,
                    sessions = sessions,
                    transactions = transactions,
                    monthlyBudgetMinor = monthlyBudgetMinor,
                    startDate = bStart,
                    endDate = bEnd,
                    today = today,
                    includePreviousComparison = false
                )
                PerformanceTrendPoint(
                    label = label,
                    startDate = bStart,
                    endDate = bEnd,
                    overallScore = perf.overallScore,
                    healthScore = perf.health.score,
                    careerScore = perf.career.score,
                    financeScore = perf.finance.score
                )
            }
        }
    }

    /**
     * Projects [buildPerformanceTrendSeries] onto a single [LifeArea] so domain screens
     * (Health, Career, Finance) can render their own trajectory in [LaPerformanceLineChart]
     * without duplicating bucket logic or inventing zero values for missing intervals.
     */
    fun buildCategoryTrendSeries(
        lifeArea: LifeArea,
        window: AnalyticsTimeWindow,
        anchorDate: LocalDate,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction> = emptyList(),
        monthlyBudgetMinor: Long? = null
    ): List<PerformanceTrendPoint> {
        return buildPerformanceTrendSeries(
            window = window,
            anchorDate = anchorDate,
            today = today,
            habits = habits,
            completions = completions,
            tasks = tasks,
            sessions = sessions,
            transactions = transactions,
            monthlyBudgetMinor = monthlyBudgetMinor
        ).map { point ->
            val areaScore = when (lifeArea) {
                LifeArea.HEALTH_LIFESTYLE -> point.healthScore
                LifeArea.CAREER_PRODUCTIVITY -> point.careerScore
                LifeArea.FINANCE_WEALTH -> point.financeScore
            }
            point.copy(overallScore = areaScore)
        }
    }

    /**
     * Builds the 365-Day (or 366-Day in leap years) Year Calendar View (`docs/04_HOME_DASHBOARD.md`
     * & `docs/09_LIFE_ANALYTICS.md` 19 Nov 2026 update):
     * - One dot/cell per calendar day of [year].
     * - Elapsed days (`!date.isAfter(today)`) are visually distinct from future days (`date.isAfter(today)`).
     * - Elapsed days with a valid Overall Life Score have `state = ELAPSED_SCORED` and `intensityLevel` 1..4:
     *   - 1..39 -> level 1
     *   - 40..69 -> level 2
     *   - 70..89 -> level 3
     *   - 90..100 -> level 4
     *   - 0 -> level 1 (an actual recorded 0% score, distinct from missing data)
     * - Elapsed days with NO score (`overallScore == null`) have `state = ELAPSED_NO_DATA` and
     *   `intensityLevel = 0`, which renders with a distinct missing-data style, NEVER the lowest-performance color.
     * - Future days have `state = FUTURE` and `intensityLevel = 0`.
     */
    fun buildYearCalendar(
        year: Int,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long?
    ): YearCalendarModel {
        val isLeap = Year.isLeap(year.toLong())
        val totalDays = if (isLeap) 366 else 365
        val startOfYear = LocalDate.of(year, 1, 1)

        // Pre-index records by date for fast O(365) construction without 365 full list scans
        val completionsByDate = completions.groupBy { it.completionDate }
        val tasksByDate = tasks.groupBy { it.plannedDate }
        val sessionsByDate = sessions
            .filter { it.status == FocusSessionStatus.COMPLETED }
            .groupBy { it.startedLocalDate }
        val transactionsByDate = transactions.groupBy { it.transactionDate }

        val healthHabits = habits.filter { it.lifeArea == LifeArea.HEALTH_LIFESTYLE }
        val careerHabits = habits.filter { it.lifeArea == LifeArea.CAREER_PRODUCTIVITY }
        val validMonthlyBudget = monthlyBudgetMinor?.takeIf { it > 0L }

        var elapsedCount = 0
        var scoredCount = 0
        val dayCells = ArrayList<YearDayCellModel>(totalDays)

        for (dayOffset in 0 until totalDays) {
            val date = startOfYear.plusDays(dayOffset.toLong())
            val dayOfYear = dayOffset + 1
            val dayCompletions = completionsByDate[date].orEmpty()
            val dayTasks = tasksByDate[date].orEmpty()
            val daySessions = sessionsByDate[date].orEmpty()
            val dayTransactions = transactionsByDate[date].orEmpty()

            val completedHabitIds = dayCompletions.filter { it.isCompleted }.map { it.habitId }.toSet()

            // All due habits on this date (for daily summary count)
            var allDueCount = 0
            var allCompletedCount = 0
            for (h in habits) {
                if (h.isDueOn(date)) {
                    allDueCount++
                    if (completedHabitIds.contains(h.id)) allCompletedCount++
                }
            }

            if (date.isAfter(today)) {
                dayCells.add(
                    YearDayCellModel(
                        date = date,
                        dayOfYear = dayOfYear,
                        state = YearCalendarDayState.FUTURE,
                        overallScore = null,
                        intensityLevel = 0,
                        healthScore = null,
                        careerScore = null,
                        financeScore = null,
                        completedHabitsCount = 0,
                        dueHabitsCount = allDueCount,
                        completedTasksCount = dayTasks.count { it.isCompleted },
                        plannedTasksCount = dayTasks.size,
                        focusSeconds = 0L,
                        transactionCount = dayTransactions.size
                    )
                )
                continue
            }

            elapsedCount++

            // 1. Health score for date
            var healthDue = 0
            var healthCompleted = 0
            for (h in healthHabits) {
                if (h.isDueOn(date)) {
                    healthDue++
                    if (completedHabitIds.contains(h.id)) healthCompleted++
                }
            }
            val healthScore = if (healthDue > 0) {
                ((healthCompleted * 100.0) / healthDue).roundToInt().coerceIn(0, 100)
            } else null

            // 2. Career score for date (40% career habits / 60% career tasks, or partial)
            var careerHabitDue = 0
            var careerHabitCompleted = 0
            for (h in careerHabits) {
                if (h.isDueOn(date)) {
                    careerHabitDue++
                    if (completedHabitIds.contains(h.id)) careerHabitCompleted++
                }
            }
            val careerHabitScore = if (careerHabitDue > 0) {
                ((careerHabitCompleted * 100.0) / careerHabitDue).roundToInt().coerceIn(0, 100)
            } else null

            val careerTasks = dayTasks.filter { it.lifeArea == LifeArea.CAREER_PRODUCTIVITY }
            val careerTaskScore = if (careerTasks.isNotEmpty()) {
                ((careerTasks.count { it.isCompleted } * 100.0) / careerTasks.size).roundToInt().coerceIn(0, 100)
            } else null

            val careerScore = when {
                careerHabitScore != null && careerTaskScore != null ->
                    (CAREER_HABIT_WEIGHT * careerHabitScore + CAREER_TASK_WEIGHT * careerTaskScore)
                        .roundToInt()
                        .coerceIn(0, 100)
                careerHabitScore != null -> careerHabitScore
                careerTaskScore != null -> careerTaskScore
                else -> null
            }

            // 3. Finance score for date
            val financeScore = if (validMonthlyBudget != null && dayTransactions.isNotEmpty()) {
                val monthDays = YearMonth.from(date).lengthOfMonth().coerceAtLeast(28)
                val dailyBudget = (validMonthlyBudget.toDouble() / monthDays.toDouble()).roundToInt().toLong().coerceAtLeast(1L)
                val dayExpense = dayTransactions
                    .filter { it.type == com.lifeanalytics.app.domain.model.TransactionType.EXPENSE && it.amountMinor > 0L }
                    .sumOf { it.amountMinor }
                if (dayExpense <= dailyBudget) {
                    100
                } else {
                    val overageRatio = (dayExpense - dailyBudget).toDouble() / dailyBudget.toDouble()
                    (100.0 - (overageRatio * 100.0)).roundToInt().coerceIn(0, 100)
                }
            } else null

            val available = listOfNotNull(healthScore, careerScore, financeScore)
            val overallScore = if (available.isNotEmpty()) {
                available.average().roundToInt().coerceIn(0, 100)
            } else null

            val state = if (overallScore != null) {
                scoredCount++
                YearCalendarDayState.ELAPSED_SCORED
            } else {
                YearCalendarDayState.ELAPSED_NO_DATA
            }

            val intensity = scoreToIntensityLevel(overallScore)
            val focusSecs = daySessions.sumOf { it.focusedDurationSeconds.coerceAtLeast(0L) }

            dayCells.add(
                YearDayCellModel(
                    date = date,
                    dayOfYear = dayOfYear,
                    state = state,
                    overallScore = overallScore,
                    intensityLevel = intensity,
                    healthScore = healthScore,
                    careerScore = careerScore,
                    financeScore = financeScore,
                    completedHabitsCount = allCompletedCount,
                    dueHabitsCount = allDueCount,
                    completedTasksCount = dayTasks.count { it.isCompleted },
                    plannedTasksCount = dayTasks.size,
                    focusSeconds = focusSecs,
                    transactionCount = dayTransactions.size
                )
            )
        }

        return YearCalendarModel(
            year = year,
            isLeapYear = isLeap,
            totalDaysInYear = totalDays,
            elapsedDaysCount = elapsedCount,
            scoredDaysCount = scoredCount,
            days = dayCells
        )
    }

    /**
     * Builds the drilldown details for a single calendar date when tapped in the 365-day view.
     */
    fun buildDayDrilldown(
        date: LocalDate,
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        tasks: List<Task>,
        sessions: List<FocusSession> = emptyList(),
        transactions: List<FinanceTransaction>,
        monthlyBudgetMinor: Long?
    ): DayDrilldownModel {
        val perf = evaluatePeriodPerformance(
            habits = habits,
            completions = completions,
            tasks = tasks,
            sessions = sessions,
            transactions = transactions,
            monthlyBudgetMinor = monthlyBudgetMinor,
            startDate = date,
            endDate = date,
            today = today,
            includePreviousComparison = true
        )
        val dayTasks = DomainCalculations.sortTasksForDay(tasks.filter { it.plannedDate == date })
        val dayCompletions = completions.filter { it.completionDate == date }
        val habitEval = DomainCalculations.evaluateHabitsForDate(habits, dayCompletions, date)
        val dueHabitPairs = habitEval.dueHabits.map { it.habit to it.isCompleted }
        val completedNames = habitEval.dueHabits.filter { it.isCompleted }.map { it.habit.name }
        val incompleteNames = habitEval.dueHabits.filter { !it.isCompleted }.map { it.habit.name }
        val daySessions = sessions.filter {
            it.startedLocalDate == date && it.status == FocusSessionStatus.COMPLETED
        }
        val dayTransactions = transactions.filter { it.transactionDate == date }

        val dayState = when {
            date.isAfter(today) -> YearCalendarDayState.FUTURE
            perf.overallScore != null -> YearCalendarDayState.ELAPSED_SCORED
            else -> YearCalendarDayState.ELAPSED_NO_DATA
        }

        return DayDrilldownModel(
            date = date,
            state = dayState,
            performance = perf,
            dueHabits = dueHabitPairs,
            tasksForDate = dayTasks,
            habitsDueCount = habitEval.totalDueCount,
            habitsCompletedCount = habitEval.completedDueCount,
            completedHabitNames = completedNames,
            incompleteHabitNames = incompleteNames,
            focusSessionsForDate = daySessions,
            transactionsForDate = dayTransactions
        )
    }

    fun scoreToIntensityLevel(score: Int?): Int {
        if (score == null) return 0
        return when {
            score >= 90 -> 4
            score >= 70 -> 3
            score >= 40 -> 2
            else -> 1
        }
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
