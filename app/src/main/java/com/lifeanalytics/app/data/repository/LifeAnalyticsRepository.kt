package com.lifeanalytics.app.data.repository

import androidx.room.withTransaction
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.core.time.SystemTimeProvider
import com.lifeanalytics.app.core.time.TimeProvider
import com.lifeanalytics.app.data.local.LifeAnalyticsDatabase
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
import com.lifeanalytics.app.domain.model.DailyReview
import com.lifeanalytics.app.domain.model.DefaultFinanceCategories
import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.FocusPauseInterval
import com.lifeanalytics.app.domain.model.FocusSession
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.FocusSessionType
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitCompletion
import com.lifeanalytics.app.domain.model.HabitSchedulePeriod
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LifeAnalyticsRepository(
    private val database: LifeAnalyticsDatabase,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    private val taskDao = database.taskDao()
    private val focusDao = database.focusDao()
    private val habitDao = database.habitDao()
    private val financeDao = database.financeDao()
    private val reviewDao = database.dailyReviewDao()

    // =========================================================================
    // TASKS (Daily Planning)
    // =========================================================================

    fun observeTasksForDate(date: LocalDate): Flow<List<Task>> {
        val iso = DateAttributionPolicy.formatIsoDate(date)
        return taskDao.observeTasksForDate(iso).map { list ->
            DomainCalculations.sortTasksForDay(list.map { it.toDomain() })
        }
    }

    fun observeTasksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> {
        val startIso = DateAttributionPolicy.formatIsoDate(startDate)
        val endIso = DateAttributionPolicy.formatIsoDate(endDate)
        return taskDao.observeTasksInRange(startIso, endIso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllTasks(): Flow<List<Task>> {
        return taskDao.observeAllTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    suspend fun createTask(
        title: String,
        category: TaskCategory,
        plannedDate: LocalDate,
        plannedTime: LocalTime? = null,
        estimatedDurationMinutes: Int? = null,
        lifeArea: LifeArea? = null
    ): Result<Task> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a task name."))
        }
        if (estimatedDurationMinutes != null && (estimatedDurationMinutes <= 0 || estimatedDurationMinutes > 1440)) {
            return Result.failure(IllegalArgumentException("Estimated duration must be between 1 and 1440 minutes."))
        }

        val now = timeProvider.nowEpochMillis()
        val plannedDateIso = DateAttributionPolicy.formatIsoDate(plannedDate)
        val existingCount = taskDao.getTasksForDate(plannedDateIso).size
        val entity = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            category = category.name,
            plannedDate = plannedDateIso,
            plannedTime = plannedTime?.let { DateAttributionPolicy.formatLocalTime(it) },
            estimatedDurationMinutes = estimatedDurationMinutes,
            status = TaskStatus.PLANNED.name,
            completedAtEpochMillis = null,
            sortOrder = existingCount,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            lifeArea = lifeArea?.name
        )
        taskDao.upsertTask(entity)
        return Result.success(entity.toDomain())
    }

    suspend fun updateTask(
        taskId: String,
        title: String,
        category: TaskCategory,
        plannedDate: LocalDate,
        plannedTime: LocalTime?,
        estimatedDurationMinutes: Int?,
        lifeArea: LifeArea? = null
    ): Result<Task> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a task name."))
        }
        if (estimatedDurationMinutes != null && (estimatedDurationMinutes <= 0 || estimatedDurationMinutes > 1440)) {
            return Result.failure(IllegalArgumentException("Estimated duration must be between 1 and 1440 minutes."))
        }

        val existing = taskDao.getTaskById(taskId)
            ?: return Result.failure(IllegalArgumentException("Task not found."))
        val now = timeProvider.nowEpochMillis()
        val updated = existing.copy(
            title = trimmedTitle,
            category = category.name,
            plannedDate = DateAttributionPolicy.formatIsoDate(plannedDate),
            plannedTime = plannedTime?.let { DateAttributionPolicy.formatLocalTime(it) },
            estimatedDurationMinutes = estimatedDurationMinutes,
            updatedAtEpochMillis = now,
            lifeArea = lifeArea?.name ?: existing.lifeArea
        )
        taskDao.upsertTask(updated)
        return Result.success(updated.toDomain())
    }

    suspend fun setTaskCompleted(taskId: String, completed: Boolean): Result<Task> {
        val existing = taskDao.getTaskById(taskId)
            ?: return Result.failure(IllegalArgumentException("Task not found."))
        val now = timeProvider.nowEpochMillis()
        val updated = existing.copy(
            status = if (completed) TaskStatus.COMPLETED.name else TaskStatus.PLANNED.name,
            completedAtEpochMillis = if (completed) now else null,
            updatedAtEpochMillis = now
        )
        taskDao.upsertTask(updated)
        return Result.success(updated.toDomain())
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        // ForeignKey.SET_NULL on focus_sessions.task_id + task_title_snapshot ensures
        // historical focus sessions remain intact and readable after task deletion.
        taskDao.deleteTaskById(taskId)
        return Result.success(Unit)
    }

    // =========================================================================
    // FOCUS SESSIONS (Active Work & Focus Timer)
    // =========================================================================

    fun observeUnresolvedSession(): Flow<FocusSession?> {
        return focusDao.observeUnresolvedSession().map { it?.toDomain() }
    }

    fun observeSessionsForDate(date: LocalDate): Flow<List<FocusSession>> {
        val iso = DateAttributionPolicy.formatIsoDate(date)
        return focusDao.observeSessionsForDate(iso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeSessionsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<FocusSession>> {
        val startIso = DateAttributionPolicy.formatIsoDate(startDate)
        val endIso = DateAttributionPolicy.formatIsoDate(endDate)
        return focusDao.observeSessionsInRange(startIso, endIso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllSessions(): Flow<List<FocusSession>> {
        return focusDao.observeAllSessions().map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Starts a focus session linked to an existing task or as general focus.
     * Enforces the single-active-session rule atomically in a database transaction.
     */
    suspend fun startFocusSession(
        taskId: String? = null,
        generalDescription: String? = null
    ): Result<FocusSession> = database.withTransaction {
        val unresolved = focusDao.getUnresolvedSessions()
        if (unresolved.isNotEmpty()) {
            return@withTransaction Result.failure(
                IllegalStateException("A focus session is already in progress. Return to or finish the current session first.")
            )
        }

        val linkedTask = if (taskId != null) {
            taskDao.getTaskById(taskId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Selected task no longer exists."))
        } else {
            null
        }

        val now = timeProvider.nowEpochMillis()
        val zoneId = timeProvider.currentZoneId().id
        val startedLocalDate = DateAttributionPolicy.localDateForEvent(now, zoneId)
        val sessionType = if (linkedTask != null) FocusSessionType.TASK_LINKED else FocusSessionType.GENERAL
        val cleanDescription = generalDescription?.trim()?.takeIf { it.isNotEmpty() }

        val entity = FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            taskId = linkedTask?.id,
            taskTitleSnapshot = linkedTask?.title,
            sessionType = sessionType.name,
            description = cleanDescription,
            startedAtEpochMillis = now,
            startedLocalDate = DateAttributionPolicy.formatIsoDate(startedLocalDate),
            timezoneId = zoneId,
            endedAtEpochMillis = null,
            status = FocusSessionStatus.ACTIVE.name,
            focusedDurationSeconds = 0L,
            lastConfirmedAtEpochMillis = now,
            interruptionReason = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        focusDao.insertSession(entity)
        Result.success(FocusSessionWithPauses(entity, emptyList()).toDomain())
    }

    /**
     * Atomically creates a new task and starts a linked focus session
     * (docs/06_ACTIVE_WORK_FOCUS.md Section 6.2).
     */
    suspend fun createTaskAndStartFocus(
        title: String,
        category: TaskCategory,
        plannedDate: LocalDate,
        estimatedDurationMinutes: Int? = null,
        lifeArea: LifeArea? = null
    ): Result<FocusSession> = database.withTransaction {
        val unresolved = focusDao.getUnresolvedSessions()
        if (unresolved.isNotEmpty()) {
            return@withTransaction Result.failure(
                IllegalStateException("A focus session is already in progress. Return to or finish the current session first.")
            )
        }
        val taskResult = createTask(
            title = title,
            category = category,
            plannedDate = plannedDate,
            plannedTime = null,
            estimatedDurationMinutes = estimatedDurationMinutes,
            lifeArea = lifeArea
        )
        val task = taskResult.getOrElse { return@withTransaction Result.failure(it) }
        startFocusSession(taskId = task.id, generalDescription = null)
    }

    suspend fun pauseFocusSession(sessionId: String): Result<FocusSession> = database.withTransaction {
        val current = focusDao.getSessionWithPausesById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Focus session not found."))
        if (current.session.status != FocusSessionStatus.ACTIVE.name) {
            return@withTransaction Result.failure(IllegalStateException("Only a running session can be paused."))
        }
        val openPause = focusDao.getOpenPauseInterval(sessionId)
        if (openPause != null) {
            return@withTransaction Result.failure(IllegalStateException("Session is already paused."))
        }

        val now = timeProvider.nowEpochMillis()
        val safeNow = maxOf(now, current.session.startedAtEpochMillis)
        val pauseEntity = FocusPauseIntervalEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            pausedAtEpochMillis = safeNow,
            resumedAtEpochMillis = null
        )
        focusDao.insertPauseInterval(pauseEntity)

        val updatedPauses = (current.pauses + pauseEntity).map { it.toDomain() }
        val durationSoFar = DomainCalculations.calculateFocusedDurationSeconds(
            startedAtEpochMillis = current.session.startedAtEpochMillis,
            endOrNowEpochMillis = safeNow,
            pauses = updatedPauses
        )
        val updatedSession = current.session.copy(
            status = FocusSessionStatus.PAUSED.name,
            focusedDurationSeconds = durationSoFar,
            lastConfirmedAtEpochMillis = safeNow,
            updatedAtEpochMillis = safeNow
        )
        focusDao.updateSession(updatedSession)
        val reloaded = focusDao.getSessionWithPausesById(sessionId)!!
        Result.success(reloaded.toDomain())
    }

    suspend fun resumeFocusSession(sessionId: String): Result<FocusSession> = database.withTransaction {
        val current = focusDao.getSessionWithPausesById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Focus session not found."))
        if (current.session.status != FocusSessionStatus.PAUSED.name) {
            return@withTransaction Result.failure(IllegalStateException("Only a paused session can be resumed."))
        }
        val openPause = focusDao.getOpenPauseInterval(sessionId)
            ?: return@withTransaction Result.failure(IllegalStateException("No open pause interval found."))

        val now = timeProvider.nowEpochMillis()
        val safeResume = maxOf(now, openPause.pausedAtEpochMillis)
        val closedPause = openPause.copy(resumedAtEpochMillis = safeResume)
        focusDao.updatePauseInterval(closedPause)

        val updatedSession = current.session.copy(
            status = FocusSessionStatus.ACTIVE.name,
            lastConfirmedAtEpochMillis = safeResume,
            updatedAtEpochMillis = safeResume
        )
        focusDao.updateSession(updatedSession)
        val reloaded = focusDao.getSessionWithPausesById(sessionId)!!
        Result.success(reloaded.toDomain())
    }

    suspend fun finishFocusSession(sessionId: String): Result<FocusSession> = database.withTransaction {
        val current = focusDao.getSessionWithPausesById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Focus session not found."))
        val status = FocusSessionStatus.valueOf(current.session.status)
        if (status == FocusSessionStatus.COMPLETED) {
            return@withTransaction Result.success(current.toDomain())
        }

        val now = timeProvider.nowEpochMillis()
        val openPause = focusDao.getOpenPauseInterval(sessionId)
        val endTimestamp = when {
            status == FocusSessionStatus.INTERRUPTED -> {
                openPause?.pausedAtEpochMillis ?: current.session.lastConfirmedAtEpochMillis
            }
            openPause != null -> {
                val safeClose = maxOf(now, openPause.pausedAtEpochMillis)
                focusDao.updatePauseInterval(openPause.copy(resumedAtEpochMillis = safeClose))
                safeClose
            }
            else -> maxOf(now, current.session.startedAtEpochMillis)
        }

        val refreshedPauses = focusDao.getSessionWithPausesById(sessionId)?.pauses.orEmpty().map { it.toDomain() }
        val finalDuration = DomainCalculations.calculateFocusedDurationSeconds(
            startedAtEpochMillis = current.session.startedAtEpochMillis,
            endOrNowEpochMillis = endTimestamp,
            pauses = refreshedPauses
        )

        val finishedSession = current.session.copy(
            endedAtEpochMillis = endTimestamp,
            status = FocusSessionStatus.COMPLETED.name,
            focusedDurationSeconds = finalDuration,
            lastConfirmedAtEpochMillis = endTimestamp,
            updatedAtEpochMillis = maxOf(now, endTimestamp)
        )
        focusDao.updateSession(finishedSession)
        val reloaded = focusDao.getSessionWithPausesById(sessionId)!!
        Result.success(reloaded.toDomain())
    }

    /**
     * Updates the heartbeat timestamp while the timer is actively running in-process,
     * or flags the session as INTERRUPTED if an unverified time gap or clock jump occurred.
     */
    suspend fun checkOrConfirmActiveSession(
        maxUnconfirmedActiveGapMillis: Long = 12 * 60 * 1000L
    ): FocusSession? = database.withTransaction {
        val unresolved = focusDao.getUnresolvedSessions().firstOrNull() ?: return@withTransaction null
        val domain = unresolved.toDomain()
        val now = timeProvider.nowEpochMillis()

        if (domain.status == FocusSessionStatus.INTERRUPTED) {
            return@withTransaction domain
        }

        val interruptionReason = DomainCalculations.checkSessionInterruptionReason(
            session = domain,
            nowEpochMillis = now,
            maxUnconfirmedActiveGapMillis = maxUnconfirmedActiveGapMillis
        )
        if (interruptionReason != null) {
            val confirmedDuration = DomainCalculations.calculateConfirmedFocusDurationSeconds(domain)
            val interrupted = unresolved.session.copy(
                status = FocusSessionStatus.INTERRUPTED.name,
                focusedDurationSeconds = confirmedDuration,
                interruptionReason = interruptionReason,
                updatedAtEpochMillis = maxOf(now, unresolved.session.lastConfirmedAtEpochMillis)
            )
            focusDao.updateSession(interrupted)
            return@withTransaction focusDao.getSessionWithPausesById(domain.id)?.toDomain()
        }

        if (domain.status == FocusSessionStatus.ACTIVE && now > unresolved.session.lastConfirmedAtEpochMillis) {
            val currentDuration = DomainCalculations.calculateFocusedDurationSeconds(
                startedAtEpochMillis = domain.startedAtEpochMillis,
                endOrNowEpochMillis = now,
                pauses = domain.pauses
            )
            val confirmed = unresolved.session.copy(
                focusedDurationSeconds = currentDuration,
                lastConfirmedAtEpochMillis = now,
                updatedAtEpochMillis = now
            )
            focusDao.updateSession(confirmed)
            return@withTransaction focusDao.getSessionWithPausesById(domain.id)?.toDomain()
        }

        domain
    }

    /**
     * Explicit recovery option 1 (docs/06_ACTIVE_WORK_FOCUS.md Section 13.2):
     * Finish the interrupted session at its last confirmed timestamp so no unknown gap is counted.
     */
    suspend fun recoverSessionFinishAtLastConfirmed(sessionId: String): Result<FocusSession> = database.withTransaction {
        val current = focusDao.getSessionWithPausesById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found."))
        val domain = current.toDomain()
        val confirmedEnd = maxOf(domain.lastConfirmedAtEpochMillis, domain.startedAtEpochMillis)
        val openPause = focusDao.getOpenPauseInterval(sessionId)
        if (openPause != null) {
            val closeAt = maxOf(confirmedEnd, openPause.pausedAtEpochMillis)
            focusDao.updatePauseInterval(openPause.copy(resumedAtEpochMillis = closeAt))
        }
        val refreshedPauses = focusDao.getSessionWithPausesById(sessionId)?.pauses.orEmpty().map { it.toDomain() }
        val confirmedDuration = DomainCalculations.calculateFocusedDurationSeconds(
            startedAtEpochMillis = domain.startedAtEpochMillis,
            endOrNowEpochMillis = confirmedEnd,
            pauses = refreshedPauses
        )
        val finished = current.session.copy(
            endedAtEpochMillis = confirmedEnd,
            status = FocusSessionStatus.COMPLETED.name,
            focusedDurationSeconds = confirmedDuration,
            interruptionReason = current.session.interruptionReason ?: "Recovered at last confirmed time",
            updatedAtEpochMillis = timeProvider.nowEpochMillis()
        )
        focusDao.updateSession(finished)
        Result.success(focusDao.getSessionWithPausesById(sessionId)!!.toDomain())
    }

    /**
     * Explicit recovery option 2 (docs/06_ACTIVE_WORK_FOCUS.md Section 13.2):
     * Resume from now, recording the unknown interruption gap as a pause interval so the
     * unverified gap is excluded from focused duration.
     */
    suspend fun recoverSessionResumeFromNow(sessionId: String): Result<FocusSession> = database.withTransaction {
        val current = focusDao.getSessionWithPausesById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found."))
        val now = timeProvider.nowEpochMillis()
        val gapStart = maxOf(current.session.lastConfirmedAtEpochMillis, current.session.startedAtEpochMillis)
        val safeNow = maxOf(now, gapStart)

        val openPause = focusDao.getOpenPauseInterval(sessionId)
        if (openPause != null) {
            focusDao.updatePauseInterval(openPause.copy(resumedAtEpochMillis = safeNow))
        } else if (safeNow > gapStart) {
            focusDao.insertPauseInterval(
                FocusPauseIntervalEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    pausedAtEpochMillis = gapStart,
                    resumedAtEpochMillis = safeNow
                )
            )
        }

        val resumed = current.session.copy(
            status = FocusSessionStatus.ACTIVE.name,
            lastConfirmedAtEpochMillis = safeNow,
            updatedAtEpochMillis = safeNow
        )
        focusDao.updateSession(resumed)
        Result.success(focusDao.getSessionWithPausesById(sessionId)!!.toDomain())
    }

    // =========================================================================
    // HABITS (Daily Habits)
    // =========================================================================

    fun observeAllHabits(): Flow<List<Habit>> {
        return habitDao.observeAllHabitsWithSchedules().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeHabitCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>> {
        val iso = DateAttributionPolicy.formatIsoDate(date)
        return habitDao.observeCompletionsForDate(iso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeHabitCompletionsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitCompletion>> {
        val startIso = DateAttributionPolicy.formatIsoDate(startDate)
        val endIso = DateAttributionPolicy.formatIsoDate(endDate)
        return habitDao.observeCompletionsInRange(startIso, endIso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllHabitCompletions(): Flow<List<HabitCompletion>> {
        return habitDao.observeAllCompletions().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createHabit(
        name: String,
        weekdays: Set<DayOfWeek>,
        effectiveFrom: LocalDate = timeProvider.todayLocalDate(),
        lifeArea: LifeArea? = null
    ): Result<Habit> = database.withTransaction {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withTransaction Result.failure(IllegalArgumentException("Enter a habit name."))
        }
        if (weekdays.isEmpty()) {
            return@withTransaction Result.failure(IllegalArgumentException("Select at least one scheduled weekday."))
        }

        val now = timeProvider.nowEpochMillis()
        val habitId = UUID.randomUUID().toString()
        val habitEntity = HabitEntity(
            id = habitId,
            name = trimmed,
            isActive = true,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            deactivatedAtEpochMillis = null,
            deactivatedFromLocalDate = null,
            lifeArea = lifeArea?.name
        )
        val scheduleType = HabitScheduleType.fromWeekdays(weekdays)
        val periodEntity = HabitSchedulePeriodEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            effectiveFrom = DateAttributionPolicy.formatIsoDate(effectiveFrom),
            effectiveTo = null,
            scheduleType = scheduleType.name,
            weekdaysCsv = encodeWeekdaysCsv(weekdays),
            createdAtEpochMillis = now
        )

        habitDao.insertHabit(habitEntity)
        habitDao.insertSchedulePeriod(periodEntity)
        Result.success(HabitWithSchedules(habitEntity, listOf(periodEntity)).toDomain())
    }

    /**
     * Updates a habit's name and schedule with effective-date history preservation
     * (docs/07_DAILY_HABITS.md Section 9.2 & docs/11_DATA_ARCHITECTURE.md Section 6.5):
     * Past schedule periods prior to [effectiveDate] remain untouched; any period open on or
     * before [effectiveDate] is closed at [effectiveDate] (exclusive) and a new period starts
     * on [effectiveDate].
     */
    suspend fun updateHabit(
        habitId: String,
        name: String,
        weekdays: Set<DayOfWeek>,
        effectiveDate: LocalDate = timeProvider.todayLocalDate(),
        lifeArea: LifeArea? = null
    ): Result<Habit> = database.withTransaction {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withTransaction Result.failure(IllegalArgumentException("Enter a habit name."))
        }
        if (weekdays.isEmpty()) {
            return@withTransaction Result.failure(IllegalArgumentException("Select at least one scheduled weekday."))
        }

        val existing = habitDao.getHabitWithSchedulesById(habitId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found."))
        val now = timeProvider.nowEpochMillis()
        val effectiveIso = DateAttributionPolicy.formatIsoDate(effectiveDate)
        val newScheduleType = HabitScheduleType.fromWeekdays(weekdays)
        val newWeekdaysCsv = encodeWeekdaysCsv(weekdays)

        val updatedHabit = existing.habit.copy(
            name = trimmed,
            updatedAtEpochMillis = now,
            lifeArea = lifeArea?.name ?: existing.habit.lifeArea
        )
        habitDao.updateHabit(updatedHabit)

        // Preserve periods strictly before effectiveDate; adjust or replace periods starting on/after effectiveDate
        val sortedPeriods = existing.schedules.sortedBy { it.effectiveFrom }
        for (period in sortedPeriods) {
            val fromDate = DateAttributionPolicy.parseIsoDate(period.effectiveFrom)
            val toDate = period.effectiveTo?.let { DateAttributionPolicy.parseIsoDate(it) }
            if (!fromDate.isBefore(effectiveDate)) {
                // Starts on or after effectiveDate -> replace with new period
                habitDao.deleteSchedulePeriodById(period.id)
            } else if (toDate == null || toDate.isAfter(effectiveDate)) {
                // Started before effectiveDate and extends past effectiveDate -> close at effectiveDate (exclusive)
                habitDao.updateSchedulePeriod(period.copy(effectiveTo = effectiveIso))
            }
        }

        val newPeriod = HabitSchedulePeriodEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            effectiveFrom = effectiveIso,
            effectiveTo = existing.habit.deactivatedFromLocalDate,
            scheduleType = newScheduleType.name,
            weekdaysCsv = newWeekdaysCsv,
            createdAtEpochMillis = now
        )
        habitDao.insertSchedulePeriod(newPeriod)

        val reloaded = habitDao.getHabitWithSchedulesById(habitId)!!
        Result.success(reloaded.toDomain())
    }

    /**
     * Deactivates a habit from [effectiveDate] onward while preserving all past schedules and completions.
     */
    suspend fun deactivateHabit(
        habitId: String,
        effectiveDate: LocalDate = timeProvider.todayLocalDate()
    ): Result<Habit> = database.withTransaction {
        val existing = habitDao.getHabitWithSchedulesById(habitId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found."))
        val now = timeProvider.nowEpochMillis()
        val effectiveIso = DateAttributionPolicy.formatIsoDate(effectiveDate)

        for (period in existing.schedules) {
            val fromDate = DateAttributionPolicy.parseIsoDate(period.effectiveFrom)
            val toDate = period.effectiveTo?.let { DateAttributionPolicy.parseIsoDate(it) }
            if (!fromDate.isBefore(effectiveDate)) {
                habitDao.deleteSchedulePeriodById(period.id)
            } else if (toDate == null || toDate.isAfter(effectiveDate)) {
                habitDao.updateSchedulePeriod(period.copy(effectiveTo = effectiveIso))
            }
        }

        val updated = existing.habit.copy(
            isActive = false,
            deactivatedAtEpochMillis = now,
            deactivatedFromLocalDate = effectiveIso,
            updatedAtEpochMillis = now
        )
        habitDao.updateHabit(updated)
        Result.success(habitDao.getHabitWithSchedulesById(habitId)!!.toDomain())
    }

    suspend fun deleteHabitPermanently(habitId: String): Result<Unit> {
        habitDao.deleteHabitById(habitId)
        return Result.success(Unit)
    }

    /**
     * Marks a habit complete or incomplete for a specific local date.
     * Rejects future dates (docs/07_DAILY_HABITS.md Section 8) and updates the unique
     * (habit_id, completion_date) record without creating duplicates.
     */
    suspend fun setHabitCompletionForDate(
        habitId: String,
        date: LocalDate,
        completed: Boolean
    ): Result<HabitCompletion> = database.withTransaction {
        val today = timeProvider.todayLocalDate()
        if (date.isAfter(today)) {
            return@withTransaction Result.failure(
                IllegalArgumentException("Habit completion can only be recorded on or before today.")
            )
        }
        val habit = habitDao.getHabitWithSchedulesById(habitId)?.toDomain()
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found."))
        if (!habit.isDueOn(date)) {
            return@withTransaction Result.failure(
                IllegalArgumentException("This habit is not scheduled on the selected date.")
            )
        }

        val dateIso = DateAttributionPolicy.formatIsoDate(date)
        val now = timeProvider.nowEpochMillis()
        val existing = habitDao.getCompletionForHabitAndDate(habitId, dateIso)
        val entity = if (existing != null) {
            existing.copy(
                isCompleted = completed,
                completedAtEpochMillis = if (completed) now else null,
                updatedAtEpochMillis = now
            )
        } else {
            HabitCompletionEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completionDate = dateIso,
                isCompleted = completed,
                completedAtEpochMillis = if (completed) now else null,
                updatedAtEpochMillis = now
            )
        }
        habitDao.upsertCompletion(entity)
        Result.success(entity.toDomain())
    }

    // =========================================================================
    // FINANCE TRANSACTIONS
    // =========================================================================

    fun observeTransactionsForDate(date: LocalDate): Flow<List<FinanceTransaction>> {
        val iso = DateAttributionPolicy.formatIsoDate(date)
        return financeDao.observeTransactionsForDate(iso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeTransactionsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<FinanceTransaction>> {
        val startIso = DateAttributionPolicy.formatIsoDate(startDate)
        val endIso = DateAttributionPolicy.formatIsoDate(endDate)
        return financeDao.observeTransactionsInRange(startIso, endIso).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllTransactions(): Flow<List<FinanceTransaction>> {
        return financeDao.observeAllTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun countAllTransactions(): Int = financeDao.countAllTransactions()

    suspend fun saveTransaction(
        transactionId: String? = null,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        category: String,
        transactionDate: LocalDate,
        note: String?
    ): Result<FinanceTransaction> {
        if (amountMinor <= 0L) {
            return Result.failure(IllegalArgumentException("Enter an amount greater than zero."))
        }
        val trimmedCategory = category.trim()
        if (trimmedCategory.isEmpty() || !DefaultFinanceCategories.isValidCategory(type, trimmedCategory)) {
            return Result.failure(
                IllegalArgumentException("Select a valid ${type.displayName.lowercase()} category.")
            )
        }
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanNote != null && cleanNote.length > 300) {
            return Result.failure(IllegalArgumentException("Note must be 300 characters or fewer."))
        }

        val now = timeProvider.nowEpochMillis()
        val existing = transactionId?.let { financeDao.getTransactionById(it) }
        val entity = FinanceTransactionEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            type = type.name,
            amountMinor = amountMinor,
            currencyCode = currencyCode.uppercase(),
            category = trimmedCategory,
            transactionDate = DateAttributionPolicy.formatIsoDate(transactionDate),
            note = cleanNote,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now
        )
        financeDao.upsertTransaction(entity)
        return Result.success(entity.toDomain())
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        financeDao.deleteTransactionById(transactionId)
        return Result.success(Unit)
    }

    // =========================================================================
    // DAILY REVIEW
    // =========================================================================

    fun observeReviewForDate(date: LocalDate): Flow<DailyReview?> {
        val iso = DateAttributionPolicy.formatIsoDate(date)
        return reviewDao.observeReviewForDate(iso).map { it?.toDomain() }
    }

    fun observeAllReviews(): Flow<List<DailyReview>> {
        return reviewDao.observeAllReviews().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveDailyReview(
        reviewDate: LocalDate,
        wentWell: String?,
        improveTomorrow: String?,
        remember: String?,
        status: ReviewStatus
    ): Result<DailyReview> = database.withTransaction {
        val today = timeProvider.todayLocalDate()
        if (reviewDate.isAfter(today) && status == ReviewStatus.COMPLETED) {
            return@withTransaction Result.failure(
                IllegalArgumentException("Future dates cannot be marked as completed reviews.")
            )
        }

        val cleanWentWell = wentWell?.trim()?.takeIf { it.isNotEmpty() }
        val cleanImprove = improveTomorrow?.trim()?.takeIf { it.isNotEmpty() }
        val cleanRemember = remember?.trim()?.takeIf { it.isNotEmpty() }

        if ((cleanWentWell?.length ?: 0) > 2000 ||
            (cleanImprove?.length ?: 0) > 2000 ||
            (cleanRemember?.length ?: 0) > 2000
        ) {
            return@withTransaction Result.failure(
                IllegalArgumentException("Each reflection field must be 2,000 characters or fewer.")
            )
        }

        val dateIso = DateAttributionPolicy.formatIsoDate(reviewDate)
        val now = timeProvider.nowEpochMillis()
        val existing = reviewDao.getReviewForDate(dateIso)
        val entity = DailyReviewEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            reviewDate = dateIso,
            wentWell = cleanWentWell,
            improveTomorrow = cleanImprove,
            remember = cleanRemember,
            status = status.name,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now
        )
        reviewDao.upsertReview(entity)
        Result.success(entity.toDomain())
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = id,
            title = title,
            category = TaskCategory.fromNameOrDefault(category),
            plannedDate = DateAttributionPolicy.parseIsoDate(plannedDate),
            plannedTime = DateAttributionPolicy.parseLocalTimeOrNull(plannedTime),
            estimatedDurationMinutes = estimatedDurationMinutes,
            status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PLANNED),
            completedAtEpochMillis = completedAtEpochMillis,
            sortOrder = sortOrder,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            lifeArea = LifeArea.fromNameOrNull(lifeArea)
        )
    }

    private fun FocusPauseIntervalEntity.toDomain(): FocusPauseInterval {
        return FocusPauseInterval(
            id = id,
            sessionId = sessionId,
            pausedAtEpochMillis = pausedAtEpochMillis,
            resumedAtEpochMillis = resumedAtEpochMillis
        )
    }

    private fun FocusSessionWithPauses.toDomain(): FocusSession {
        val domainPauses = pauses.sortedBy { it.pausedAtEpochMillis }.map { it.toDomain() }
        return FocusSession(
            id = session.id,
            taskId = session.taskId,
            taskTitleSnapshot = session.taskTitleSnapshot,
            sessionType = runCatching { FocusSessionType.valueOf(session.sessionType) }
                .getOrDefault(FocusSessionType.GENERAL),
            description = session.description,
            startedAtEpochMillis = session.startedAtEpochMillis,
            startedLocalDate = DateAttributionPolicy.parseIsoDate(session.startedLocalDate),
            timezoneId = session.timezoneId,
            endedAtEpochMillis = session.endedAtEpochMillis,
            status = runCatching { FocusSessionStatus.valueOf(session.status) }
                .getOrDefault(FocusSessionStatus.COMPLETED),
            focusedDurationSeconds = session.focusedDurationSeconds,
            lastConfirmedAtEpochMillis = session.lastConfirmedAtEpochMillis,
            interruptionReason = session.interruptionReason,
            pauses = domainPauses,
            createdAtEpochMillis = session.createdAtEpochMillis,
            updatedAtEpochMillis = session.updatedAtEpochMillis
        )
    }

    private fun HabitSchedulePeriodEntity.toDomain(): HabitSchedulePeriod {
        val days = decodeWeekdaysCsv(weekdaysCsv)
        return HabitSchedulePeriod(
            id = id,
            habitId = habitId,
            effectiveFrom = DateAttributionPolicy.parseIsoDate(effectiveFrom),
            effectiveTo = effectiveTo?.let { DateAttributionPolicy.parseIsoDate(it) },
            scheduleType = runCatching { HabitScheduleType.valueOf(scheduleType) }
                .getOrDefault(HabitScheduleType.fromWeekdays(days)),
            weekdays = days,
            createdAtEpochMillis = createdAtEpochMillis
        )
    }

    private fun HabitWithSchedules.toDomain(): Habit {
        return Habit(
            id = habit.id,
            name = habit.name,
            isActive = habit.isActive,
            createdAtEpochMillis = habit.createdAtEpochMillis,
            updatedAtEpochMillis = habit.updatedAtEpochMillis,
            deactivatedAtEpochMillis = habit.deactivatedAtEpochMillis,
            deactivatedFromLocalDate = habit.deactivatedFromLocalDate?.let { DateAttributionPolicy.parseIsoDate(it) },
            schedulePeriods = schedules.sortedBy { it.effectiveFrom }.map { it.toDomain() },
            lifeArea = LifeArea.fromNameOrNull(habit.lifeArea)
        )
    }

    private fun HabitCompletionEntity.toDomain(): HabitCompletion {
        return HabitCompletion(
            id = id,
            habitId = habitId,
            completionDate = DateAttributionPolicy.parseIsoDate(completionDate),
            isCompleted = isCompleted,
            completedAtEpochMillis = completedAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
    }

    private fun FinanceTransactionEntity.toDomain(): FinanceTransaction {
        return FinanceTransaction(
            id = id,
            type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            category = category,
            transactionDate = DateAttributionPolicy.parseIsoDate(transactionDate),
            note = note,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
    }

    private fun DailyReviewEntity.toDomain(): DailyReview {
        return DailyReview(
            id = id,
            reviewDate = DateAttributionPolicy.parseIsoDate(reviewDate),
            wentWell = wentWell,
            improveTomorrow = improveTomorrow,
            remember = remember,
            status = runCatching { ReviewStatus.valueOf(status) }.getOrDefault(ReviewStatus.DRAFT),
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
    }

    companion object {
        fun encodeWeekdaysCsv(days: Set<DayOfWeek>): String {
            return days.map { it.value }.sorted().joinToString(",")
        }

        fun decodeWeekdaysCsv(csv: String): Set<DayOfWeek> {
            if (csv.isBlank()) return emptySet()
            return csv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .map { DayOfWeek.of(it) }
                .toSet()
        }
    }
}
