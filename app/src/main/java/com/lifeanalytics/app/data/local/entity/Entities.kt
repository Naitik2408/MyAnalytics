package com.lifeanalytics.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["planned_date"]),
        Index(value = ["status"]),
        Index(value = ["planned_date", "status"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "planned_date")
    val plannedDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "planned_time")
    val plannedTime: String?, // HH:mm
    @ColumnInfo(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int?,
    @ColumnInfo(name = "status")
    val status: String, // PLANNED, COMPLETED
    @ColumnInfo(name = "completed_at_epoch_millis")
    val completedAtEpochMillis: Long?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "life_area")
    val lifeArea: String? = null
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["started_local_date"]),
        Index(value = ["status"]),
        Index(value = ["started_at_epoch_millis"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String?,
    @ColumnInfo(name = "task_title_snapshot")
    val taskTitleSnapshot: String?,
    @ColumnInfo(name = "session_type")
    val sessionType: String, // TASK_LINKED, GENERAL
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "started_local_date")
    val startedLocalDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String,
    @ColumnInfo(name = "ended_at_epoch_millis")
    val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "status")
    val status: String, // ACTIVE, PAUSED, COMPLETED, INTERRUPTED
    @ColumnInfo(name = "focused_duration_seconds")
    val focusedDurationSeconds: Long,
    @ColumnInfo(name = "last_confirmed_at_epoch_millis")
    val lastConfirmedAtEpochMillis: Long,
    @ColumnInfo(name = "interruption_reason")
    val interruptionReason: String?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "focus_pause_intervals",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"])
    ]
)
data class FocusPauseIntervalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "paused_at_epoch_millis")
    val pausedAtEpochMillis: Long,
    @ColumnInfo(name = "resumed_at_epoch_millis")
    val resumedAtEpochMillis: Long?
)

data class FocusSessionWithPauses(
    @Embedded
    val session: FocusSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val pauses: List<FocusPauseIntervalEntity>
)

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["is_active"])
    ]
)
data class HabitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "deactivated_at_epoch_millis")
    val deactivatedAtEpochMillis: Long?,
    @ColumnInfo(name = "deactivated_from_local_date")
    val deactivatedFromLocalDate: String?, // YYYY-MM-DD exclusive end date
    @ColumnInfo(name = "life_area")
    val lifeArea: String? = null
)

@Entity(
    tableName = "habit_schedule_periods",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id"]),
        Index(value = ["habit_id", "effective_from"])
    ]
)
data class HabitSchedulePeriodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "habit_id")
    val habitId: String,
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: String, // YYYY-MM-DD inclusive
    @ColumnInfo(name = "effective_to")
    val effectiveTo: String?, // YYYY-MM-DD exclusive; null = open
    @ColumnInfo(name = "schedule_type")
    val scheduleType: String, // EVERY_DAY, WEEKDAYS, WEEKENDS, CUSTOM
    @ColumnInfo(name = "weekdays_csv")
    val weekdaysCsv: String, // e.g. "1,2,3,4,5" (1=Monday .. 7=Sunday)
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long
)

data class HabitWithSchedules(
    @Embedded
    val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habit_id"
    )
    val schedules: List<HabitSchedulePeriodEntity>
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id", "completion_date"], unique = true),
        Index(value = ["completion_date"])
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "habit_id")
    val habitId: String,
    @ColumnInfo(name = "completion_date")
    val completionDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "completed_at_epoch_millis")
    val completedAtEpochMillis: Long?,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "finance_transactions",
    indices = [
        Index(value = ["transaction_date"]),
        Index(value = ["type"]),
        Index(value = ["transaction_date", "type"])
    ]
)
data class FinanceTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    val type: String, // INCOME, EXPENSE
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "transaction_date")
    val transactionDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)

@Entity(
    tableName = "daily_reviews",
    indices = [
        Index(value = ["review_date"], unique = true)
    ]
)
data class DailyReviewEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "review_date")
    val reviewDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "went_well")
    val wentWell: String?,
    @ColumnInfo(name = "improve_tomorrow")
    val improveTomorrow: String?,
    @ColumnInfo(name = "remember")
    val remember: String?,
    @ColumnInfo(name = "status")
    val status: String, // DRAFT, COMPLETED
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
)
