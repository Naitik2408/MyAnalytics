package com.lifeanalytics.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lifeanalytics.app.data.local.entity.DailyReviewEntity
import com.lifeanalytics.app.data.local.entity.FinanceTransactionEntity
import com.lifeanalytics.app.data.local.entity.FocusPauseIntervalEntity
import com.lifeanalytics.app.data.local.entity.FocusSessionEntity
import com.lifeanalytics.app.data.local.entity.FocusSessionWithPauses
import com.lifeanalytics.app.data.local.entity.HabitCompletionEntity
import com.lifeanalytics.app.data.local.entity.HabitEntity
import com.lifeanalytics.app.data.local.entity.HabitSchedulePeriodEntity
import com.lifeanalytics.app.data.local.entity.HabitWithSchedules
import com.lifeanalytics.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE planned_date = :plannedDate ORDER BY sort_order ASC, created_at_epoch_millis ASC")
    fun observeTasksForDate(plannedDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE planned_date BETWEEN :startDate AND :endDate ORDER BY planned_date ASC, sort_order ASC, created_at_epoch_millis ASC")
    fun observeTasksInRange(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY planned_date ASC, sort_order ASC, created_at_epoch_millis ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE planned_date = :plannedDate ORDER BY sort_order ASC, created_at_epoch_millis ASC")
    suspend fun getTasksForDate(plannedDate: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllTasks(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks ORDER BY planned_date ASC, sort_order ASC, created_at_epoch_millis ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface FocusDao {
    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE status IN ('ACTIVE', 'PAUSED', 'INTERRUPTED') ORDER BY started_at_epoch_millis DESC LIMIT 1")
    fun observeUnresolvedSession(): Flow<FocusSessionWithPauses?>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE status IN ('ACTIVE', 'PAUSED', 'INTERRUPTED') ORDER BY started_at_epoch_millis DESC")
    suspend fun getUnresolvedSessions(): List<FocusSessionWithPauses>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithPausesById(sessionId: String): FocusSessionWithPauses?

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE started_local_date = :localDate ORDER BY started_at_epoch_millis DESC")
    fun observeSessionsForDate(localDate: String): Flow<List<FocusSessionWithPauses>>

    @Transaction
    @Query("SELECT * FROM focus_sessions WHERE started_local_date BETWEEN :startDate AND :endDate ORDER BY started_at_epoch_millis DESC")
    fun observeSessionsInRange(startDate: String, endDate: String): Flow<List<FocusSessionWithPauses>>

    @Transaction
    @Query("SELECT * FROM focus_sessions ORDER BY started_at_epoch_millis DESC")
    fun observeAllSessions(): Flow<List<FocusSessionWithPauses>>

    @Query("SELECT * FROM focus_sessions ORDER BY started_at_epoch_millis ASC")
    suspend fun getAllSessionEntities(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_pause_intervals ORDER BY paused_at_epoch_millis ASC")
    suspend fun getAllPauseIntervalEntities(): List<FocusPauseIntervalEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSessions(sessions: List<FocusSessionEntity>)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPauseInterval(pause: FocusPauseIntervalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllPauseIntervals(pauses: List<FocusPauseIntervalEntity>)

    @Update
    suspend fun updatePauseInterval(pause: FocusPauseIntervalEntity)

    @Query("SELECT * FROM focus_pause_intervals WHERE session_id = :sessionId AND resumed_at_epoch_millis IS NULL ORDER BY paused_at_epoch_millis DESC LIMIT 1")
    suspend fun getOpenPauseInterval(sessionId: String): FocusPauseIntervalEntity?

    @Query("DELETE FROM focus_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM focus_pause_intervals")
    suspend fun deleteAllPauseIntervals()

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface HabitDao {
    @Transaction
    @Query("SELECT * FROM habits ORDER BY created_at_epoch_millis ASC")
    fun observeAllHabitsWithSchedules(): Flow<List<HabitWithSchedules>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitWithSchedulesById(habitId: String): HabitWithSchedules?

    @Query("SELECT * FROM habits ORDER BY created_at_epoch_millis ASC")
    suspend fun getAllHabitEntities(): List<HabitEntity>

    @Query("SELECT * FROM habit_schedule_periods ORDER BY effective_from ASC, created_at_epoch_millis ASC")
    suspend fun getAllSchedulePeriodEntities(): List<HabitSchedulePeriodEntity>

    @Query("SELECT * FROM habit_completions ORDER BY completion_date ASC, updated_at_epoch_millis ASC")
    suspend fun getAllCompletionEntities(): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSchedulePeriod(period: HabitSchedulePeriodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSchedulePeriods(periods: List<HabitSchedulePeriodEntity>)

    @Update
    suspend fun updateSchedulePeriod(period: HabitSchedulePeriodEntity)

    @Query("DELETE FROM habit_schedule_periods WHERE id = :periodId")
    suspend fun deleteSchedulePeriodById(periodId: String)

    @Query("SELECT * FROM habit_completions WHERE completion_date = :date")
    fun observeCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completion_date BETWEEN :startDate AND :endDate")
    fun observeCompletionsInRange(startDate: String, endDate: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions ORDER BY completion_date ASC")
    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date LIMIT 1")
    suspend fun getCompletionForHabitAndDate(habitId: String, date: String): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllCompletions(completions: List<HabitCompletionEntity>)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()

    @Query("DELETE FROM habit_schedule_periods")
    suspend fun deleteAllSchedulePeriods()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_transactions WHERE transaction_date = :date ORDER BY created_at_epoch_millis DESC")
    fun observeTransactionsForDate(date: String): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions WHERE transaction_date BETWEEN :startDate AND :endDate ORDER BY transaction_date DESC, created_at_epoch_millis DESC")
    fun observeTransactionsInRange(startDate: String, endDate: String): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY transaction_date DESC, created_at_epoch_millis DESC")
    fun observeAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY transaction_date ASC, created_at_epoch_millis ASC")
    suspend fun getAllTransactionEntities(): List<FinanceTransactionEntity>

    @Query("SELECT COUNT(*) FROM finance_transactions")
    suspend fun countAllTransactions(): Int

    @Query("SELECT * FROM finance_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): FinanceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: FinanceTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllTransactions(transactions: List<FinanceTransactionEntity>)

    @Query("DELETE FROM finance_transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("DELETE FROM finance_transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface DailyReviewDao {
    @Query("SELECT * FROM daily_reviews WHERE review_date = :reviewDate LIMIT 1")
    fun observeReviewForDate(reviewDate: String): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews WHERE review_date = :reviewDate LIMIT 1")
    suspend fun getReviewForDate(reviewDate: String): DailyReviewEntity?

    @Query("SELECT * FROM daily_reviews ORDER BY review_date DESC")
    fun observeAllReviews(): Flow<List<DailyReviewEntity>>

    @Query("SELECT * FROM daily_reviews ORDER BY review_date ASC")
    suspend fun getAllReviewEntities(): List<DailyReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: DailyReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllReviews(reviews: List<DailyReviewEntity>)

    @Query("DELETE FROM daily_reviews WHERE review_date = :reviewDate")
    suspend fun deleteReviewForDate(reviewDate: String)

    @Query("DELETE FROM daily_reviews")
    suspend fun deleteAllReviews()
}
