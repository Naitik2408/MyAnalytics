package com.lifeanalytics.app.domain.growth

import java.time.LocalDate

enum class GrowthWindow(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

enum class GrowthVerdict(val label: String) {
    IMPROVING("Improving"),
    STEADY("Steady"),
    SLIPPING("Slipping")
}

enum class OverallVerdict(val label: String) {
    IMPROVING("Improving"),
    STEADY("Steady"),
    SLIPPING("Slipping"),
    MIXED("Mixed"),
    BUILDING_BASELINE("Building baseline")
}

sealed interface WindowLockStatus {
    object Unlocked : WindowLockStatus
    data class Locked(val reason: String, val unlockHint: String) : WindowLockStatus
}

data class PillarVerdictResult(
    val pillarName: String,
    val verdict: GrowthVerdict?, // null if no verdict
    val currentDisplayValue: String,
    val previousDisplayValue: String,
    val deltaPercentPoints: Int?, // null if not applicable (e.g. Finance)
    val reason: String,
    val supportingText: String? = null
) {
    val hasActiveVerdict: Boolean get() = verdict != null
}

data class GrowthDirectionResult(
    val window: GrowthWindow,
    val lockStatus: WindowLockStatus,
    val currentPeriodLabel: String,
    val previousPeriodLabel: String,
    val overallVerdict: OverallVerdict,
    val habitsPillar: PillarVerdictResult,
    val tasksPillar: PillarVerdictResult,
    val financePillar: PillarVerdictResult
) {
    val isUnlocked: Boolean get() = lockStatus is WindowLockStatus.Unlocked
}

/**
 * Raw input bundle for GrowthEngine calculation.
 * Zero UI dependencies; strictly populated from source records.
 */
data class GrowthInputData(
    val today: LocalDate,
    val window: GrowthWindow,
    val recordedDates: Set<LocalDate>,
    // Habits
    val currentHabitsCompleted: Int,
    val currentHabitsDue: Int,
    val previousHabitsCompleted: Int,
    val previousHabitsDue: Int,
    // Tasks
    val currentTasksCompleted: Int,
    val currentTasksPlanned: Int,
    val previousTasksCompleted: Int,
    val previousTasksPlanned: Int,
    // Focus
    val currentFocusDurationSeconds: Long,
    val previousFocusDurationSeconds: Long,
    // Finance
    val currentIncomeMinor: Long,
    val currentExpenseMinor: Long,
    val previousIncomeMinor: Long,
    val previousExpenseMinor: Long,
    val currentPeriodHasTransactions: Boolean,
    val previousPeriodRecordedDays: Int,
    val isCurrentPeriodClosed: Boolean
)
