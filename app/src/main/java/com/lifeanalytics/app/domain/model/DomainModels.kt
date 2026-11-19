package com.lifeanalytics.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class LifeArea(val displayName: String, val shortName: String) {
    HEALTH_LIFESTYLE("Health & Lifestyle", "Health"),
    CAREER_PRODUCTIVITY("Career & Productivity", "Career"),
    FINANCE_WEALTH("Finance & Wealth", "Finance");

    companion object {
        fun fromNameOrNull(value: String?): LifeArea? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

enum class TaskCategory(val displayName: String) {
    STUDY("Study"),
    WORK("Work"),
    PERSONAL("Personal"),
    OTHER("Other");

    companion object {
        fun fromNameOrDefault(value: String?): TaskCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class TaskStatus {
    PLANNED,
    COMPLETED
}

data class Task(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val plannedDate: LocalDate,
    val plannedTime: LocalTime?,
    val estimatedDurationMinutes: Int?,
    val status: TaskStatus,
    val completedAtEpochMillis: Long?,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val lifeArea: LifeArea? = null
) {
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED
}

enum class FocusSessionType {
    TASK_LINKED,
    GENERAL
}

enum class FocusSessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    INTERRUPTED
}

data class FocusPauseInterval(
    val id: String,
    val sessionId: String,
    val pausedAtEpochMillis: Long,
    val resumedAtEpochMillis: Long?
)

data class FocusSession(
    val id: String,
    val taskId: String?,
    val taskTitleSnapshot: String?,
    val sessionType: FocusSessionType,
    val description: String?,
    val startedAtEpochMillis: Long,
    val startedLocalDate: LocalDate,
    val timezoneId: String,
    val endedAtEpochMillis: Long?,
    val status: FocusSessionStatus,
    val focusedDurationSeconds: Long,
    val lastConfirmedAtEpochMillis: Long,
    val lastConfirmedElapsedRealtimeMillis: Long? = null,
    val interruptionReason: String?,
    val pauses: List<FocusPauseInterval> = emptyList(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    val displayTitle: String
        get() = when {
            !taskTitleSnapshot.isNullOrBlank() -> taskTitleSnapshot
            !description.isNullOrBlank() -> description
            sessionType == FocusSessionType.TASK_LINKED -> "Planned task"
            else -> "General focus"
        }
}

enum class HabitScheduleType(val displayName: String) {
    EVERY_DAY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    CUSTOM("Custom");

    companion object {
        fun fromWeekdays(days: Set<DayOfWeek>): HabitScheduleType {
            return when (days) {
                DayOfWeek.entries.toSet() -> EVERY_DAY
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ) -> WEEKDAYS
                setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> WEEKENDS
                else -> CUSTOM
            }
        }
    }
}

data class HabitSchedulePeriod(
    val id: String,
    val habitId: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?, // Exclusive end date; null means open-ended
    val scheduleType: HabitScheduleType,
    val weekdays: Set<DayOfWeek>,
    val createdAtEpochMillis: Long
) {
    fun isEffectiveOn(date: LocalDate): Boolean {
        if (date.isBefore(effectiveFrom)) return false
        val endExclusive = effectiveTo ?: return true
        return date.isBefore(endExclusive)
    }

    fun isScheduledOn(date: LocalDate): Boolean {
        return isEffectiveOn(date) && weekdays.contains(date.dayOfWeek)
    }
}

data class Habit(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deactivatedAtEpochMillis: Long?,
    val deactivatedFromLocalDate: LocalDate?,
    val schedulePeriods: List<HabitSchedulePeriod> = emptyList(),
    val lifeArea: LifeArea? = null
) {
    fun effectiveScheduleOn(date: LocalDate): HabitSchedulePeriod? {
        val createdDate = java.time.Instant.ofEpochMilli(createdAtEpochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val earliestPeriodFrom = schedulePeriods.minOfOrNull { it.effectiveFrom } ?: createdDate
        if (date.isBefore(earliestPeriodFrom)) return null
        val deactivatedDate = deactivatedFromLocalDate
        if (deactivatedDate != null && !date.isBefore(deactivatedDate)) return null
        return schedulePeriods
            .sortedByDescending { it.effectiveFrom }
            .firstOrNull { it.isEffectiveOn(date) }
    }

    fun isDueOn(date: LocalDate): Boolean {
        val period = effectiveScheduleOn(date) ?: return false
        return period.weekdays.contains(date.dayOfWeek)
    }
}

data class HabitCompletion(
    val id: String,
    val habitId: String,
    val completionDate: LocalDate,
    val isCompleted: Boolean,
    val completedAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long
)

enum class HabitDayState {
    DUE_INCOMPLETE,
    DUE_COMPLETED,
    NOT_SCHEDULED,
    NOT_APPLICABLE
}

data class HabitForDateItem(
    val habit: Habit,
    val schedulePeriod: HabitSchedulePeriod?,
    val dayState: HabitDayState,
    val isCompleted: Boolean
)

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

object DefaultFinanceCategories {
    val expenseCategories = listOf(
        "Food",
        "Transport",
        "Shopping",
        "Bills",
        "Education",
        "Health",
        "Entertainment",
        "Other"
    )

    val incomeCategories = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Allowance",
        "Gift",
        "Refund",
        "Other"
    )

    fun categoriesFor(type: TransactionType): List<String> {
        return when (type) {
            TransactionType.EXPENSE -> expenseCategories
            TransactionType.INCOME -> incomeCategories
        }
    }

    fun isValidCategory(type: TransactionType, category: String): Boolean {
        return categoriesFor(type).any { it.equals(category.trim(), ignoreCase = true) }
    }
}

data class FinanceTransaction(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String,
    val category: String,
    val transactionDate: LocalDate,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

enum class ReviewStatus(val displayName: String) {
    DRAFT("Draft"),
    COMPLETED("Completed")
}

data class DailyReview(
    val id: String,
    val reviewDate: LocalDate,
    val wentWell: String?,
    val improveTomorrow: String?,
    val remember: String?,
    val status: ReviewStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

enum class AppThemeOption(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System default")
}

data class AppPreferences(
    val themeOption: AppThemeOption = AppThemeOption.LIGHT,
    val currencyCode: String = "INR",
    val monthlyBudgetMinor: Long? = null
)
