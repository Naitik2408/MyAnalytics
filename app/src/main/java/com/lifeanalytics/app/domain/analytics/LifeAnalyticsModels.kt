package com.lifeanalytics.app.domain.analytics

import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.Task
import java.time.LocalDate

enum class AnalyticsTimeWindow(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class HealthCategoryResult(
    val score: Int?, // 0..100 or null when no scheduled health habits in period
    val completedScheduledHabits: Int,
    val totalScheduledHabits: Int,
    val activeHabitCount: Int = 0,
    val healthTasksCompleted: Int = 0,
    val healthTasksPlanned: Int = 0,
    val statusLabel: String
) {
    val hasEligibleData: Boolean get() = score != null
    val hasScheduledData: Boolean get() = totalScheduledHabits > 0
    val completedCount: Int get() = completedScheduledHabits
    val scheduledCount: Int get() = totalScheduledHabits
    val summaryText: String get() = statusLabel
}

enum class CareerCoverageMode {
    FULL_40_60, // Both career habits (40%) and tasks (60%) have eligible data
    PARTIAL_HABITS_ONLY,
    PARTIAL_TASKS_ONLY,
    INSUFFICIENT_DATA
}

data class CareerCategoryResult(
    val score: Int?, // 0..100 or null when neither component has eligible data
    val isPartial: Boolean,
    val coverageMode: CareerCoverageMode,
    val careerHabitAdherencePercent: Int?,
    val completedCareerHabits: Int,
    val totalScheduledCareerHabits: Int,
    val taskCompletionPercent: Int?,
    val completedTasks: Int,
    val totalPlannedTasks: Int,
    val focusDurationSeconds: Long,
    val statusLabel: String
) {
    val hasEligibleData: Boolean get() = score != null
    val hasHabitData: Boolean get() = careerHabitAdherencePercent != null
    val hasTaskData: Boolean get() = taskCompletionPercent != null
    val habitScore: Int? get() = careerHabitAdherencePercent
    val taskScore: Int? get() = taskCompletionPercent
    val habitCompletedCount: Int get() = completedCareerHabits
    val habitScheduledCount: Int get() = totalScheduledCareerHabits
    val taskCompletedCount: Int get() = completedTasks
    val taskPlannedCount: Int get() = totalPlannedTasks
    val summaryText: String get() = statusLabel
}

data class FinanceCategoryResult(
    val score: Int?, // 0..100 ONLY when a valid budget target exists AND finance data/period is active
    val incomeMinor: Long,
    val expensesMinor: Long,
    val netCashFlowMinor: Long,
    val savingsAmountMinor: Long,
    val savingsRatePercent: Int?, // null when incomeMinor == 0L
    val periodBudgetTargetMinor: Long?, // prorated or monthly budget target; null if no budget configured
    val budgetUsedPercent: Int?, // (expenses / periodBudgetTarget) * 100 when target > 0
    val transactionCount: Int,
    val statusLabel: String
) {
    val hasEligibleData: Boolean get() = score != null
    val hasFinanceActivity: Boolean get() = transactionCount > 0
    val totalIncomeMinor: Long get() = incomeMinor
    val totalExpenseMinor: Long get() = expensesMinor
    val summaryText: String get() = statusLabel
}

data class PeriodLifePerformance(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val periodLabel: String = "",
    val comparisonLabel: String = "vs previous period",
    val overallScore: Int?, // Equal-weight average of available category scores; null if 0 categories available
    val previousOverallScore: Int?,
    val changeVsPreviousPoints: Int?, // overallScore - previousOverallScore when both exist
    val availableCategoriesCount: Int, // 0..3
    val totalCategoriesCount: Int = 3,
    val coverageLabel: String, // e.g., "2 of 3 life areas available"
    val consistencyPercent: Int?, // % of elapsed days in period with active execution / adherence
    val activeTrackedDays: Int,
    val elapsedDaysInPeriod: Int,
    val health: HealthCategoryResult,
    val career: CareerCategoryResult,
    val finance: FinanceCategoryResult
) {
    val availableCategoryCount: Int get() = availableCategoriesCount
    val dataCoverageText: String get() = coverageLabel
    val changePoints: Int? get() = changeVsPreviousPoints
    val activeScoredDaysCount: Int get() = activeTrackedDays
}

data class PerformanceTrendPoint(
    val label: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val overallScore: Int?, // null when no category had eligible data in this bucket
    val healthScore: Int?,
    val careerScore: Int?,
    val financeScore: Int?
) {
    val availableCategoriesCount: Int
        get() = listOfNotNull(healthScore, careerScore, financeScore).size
}

enum class YearCalendarDayState {
    ELAPSED_SCORED,   // Past or today with a valid Overall Life Score (intensity 1..4)
    ELAPSED_NO_DATA,  // Past or today with no eligible scored category (distinct neutral missing-data style)
    FUTURE            // After today (neutral unfilled cell)
}

typealias YearDayState = YearCalendarDayState

data class YearDayCellModel(
    val date: LocalDate,
    val dayOfYear: Int,
    val state: YearCalendarDayState,
    val overallScore: Int?,
    val intensityLevel: Int, // 0 for NO_DATA/FUTURE, 1..4 for ELAPSED_SCORED
    val healthScore: Int?,
    val careerScore: Int?,
    val financeScore: Int?,
    val completedHabitsCount: Int,
    val dueHabitsCount: Int,
    val completedTasksCount: Int,
    val plannedTasksCount: Int,
    val focusSeconds: Long,
    val transactionCount: Int
)

data class YearCalendarModel(
    val year: Int,
    val isLeapYear: Boolean,
    val totalDaysInYear: Int, // 365 or 366
    val elapsedDaysCount: Int,
    val scoredDaysCount: Int,
    val days: List<YearDayCellModel>
) {
    val elapsedDaysWithoutScoreCount: Int
        get() = (elapsedDaysCount - scoredDaysCount).coerceAtLeast(0)

    val futureDaysCount: Int
        get() = (totalDaysInYear - elapsedDaysCount).coerceAtLeast(0)

    val averageYearScore: Int?
        get() {
            val scores = days.mapNotNull { it.overallScore }
            return if (scores.isNotEmpty()) {
                kotlin.math.round(scores.average()).toInt().coerceIn(0, 100)
            } else {
                null
            }
        }
}

data class DayDrilldownModel(
    val date: LocalDate,
    val state: YearCalendarDayState,
    val performance: PeriodLifePerformance,
    val dueHabits: List<Pair<Habit, Boolean>>,
    val tasksForDate: List<Task>,
    val habitsDueCount: Int,
    val habitsCompletedCount: Int,
    val completedHabitNames: List<String>,
    val incompleteHabitNames: List<String>,
    val focusSessionsForDate: List<FocusSession>,
    val transactionsForDate: List<FinanceTransaction>
) {
    val overallScore: Int? get() = performance.overallScore
    val completedFocusSeconds: Long
        get() = focusSessionsForDate.sumOf { it.focusedDurationSeconds.coerceAtLeast(0L) }
}

typealias DayLifeDrilldown = DayDrilldownModel
