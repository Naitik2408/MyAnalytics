package com.lifeanalytics.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifeanalytics.app.core.time.TimeProvider
import com.lifeanalytics.app.data.local.LifeAnalyticsDatabase
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TransactionType
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeTimeProvider(
    var currentEpochMillis: Long = 1_763_287_200_000L, // 2025-11-16T10:00:00Z
    var zone: ZoneId = ZoneId.of("UTC")
) : TimeProvider {
    override fun nowEpochMillis(): Long = currentEpochMillis
    override fun currentZoneId(): ZoneId = zone
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeAnalyticsRepositoryTest {

    private lateinit var database: LifeAnalyticsDatabase
    private lateinit var repository: LifeAnalyticsRepository
    private lateinit var fakeTime: FakeTimeProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifeAnalyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeTime = FakeTimeProvider()
        repository = LifeAnalyticsRepository(database, fakeTime)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun taskLifecycle_andDeletingTaskPreservesFocusSessionSnapshot() = runBlocking {
        val today = fakeTime.todayLocalDate()
        val task = repository.createTask(
            title = "Study Jetpack Compose",
            category = TaskCategory.STUDY,
            plannedDate = today,
            estimatedDurationMinutes = 60
        ).getOrThrow()

        // Start a linked focus session, run for 25 minutes, finish
        val session = repository.startFocusSession(taskId = task.id).getOrThrow()
        fakeTime.currentEpochMillis += 25 * 60_000L
        val finished = repository.finishFocusSession(session.id).getOrThrow()
        assertEquals(1500L, finished.focusedDurationSeconds)
        assertEquals("Study Jetpack Compose", finished.displayTitle)

        // Finishing focus session must NOT automatically complete the task
        val taskAfterFocus = repository.getTaskById(task.id)!!
        assertFalse(taskAfterFocus.isCompleted)

        // Delete the task and verify historical focus session survives with snapshot title
        repository.deleteTask(task.id).getOrThrow()
        assertNull(repository.getTaskById(task.id))

        val sessionsToday = repository.observeSessionsForDate(today).first()
        assertEquals(1, sessionsToday.size)
        assertNull(sessionsToday.first().taskId)
        assertEquals("Study Jetpack Compose", sessionsToday.first().displayTitle)
        assertEquals(1500L, sessionsToday.first().focusedDurationSeconds)
    }

    @Test
    fun focusSession_enforcesSingleActiveSessionAndExcludesPausesAndInterruptionGaps() = runBlocking {
        val s1 = repository.startFocusSession(generalDescription = "Reading").getOrThrow()

        // Starting a second session while s1 is active must fail
        val secondAttempt = repository.startFocusSession(generalDescription = "Second")
        assertTrue(secondAttempt.isFailure)

        // Run 10m, pause 5m, resume, run 15m (with normal heartbeat) -> 25m (1500s)
        fakeTime.currentEpochMillis += 10 * 60_000L
        repository.pauseFocusSession(s1.id).getOrThrow()
        fakeTime.currentEpochMillis += 5 * 60_000L
        repository.resumeFocusSession(s1.id).getOrThrow()
        fakeTime.currentEpochMillis += 8 * 60_000L
        repository.checkOrConfirmActiveSession() // Heartbeat at +8m
        fakeTime.currentEpochMillis += 7 * 60_000L
        val confirmedAt25m = repository.checkOrConfirmActiveSession()!! // Heartbeat at +15m (25m focused total)
        assertEquals(FocusSessionStatus.ACTIVE, confirmedAt25m.status)
        assertEquals(1500L, confirmedAt25m.focusedDurationSeconds)

        // Simulate unconfirmed 30-minute interruption gap (process death / force-stop)
        val lastConfirmedBeforeCrash = fakeTime.currentEpochMillis
        fakeTime.currentEpochMillis += 30 * 60_000L

        val detected = repository.checkOrConfirmActiveSession()!!
        assertEquals(FocusSessionStatus.INTERRUPTED, detected.status)
        assertEquals(1500L, detected.focusedDurationSeconds) // 30m unknown gap NOT counted

        // Resume from now: inserts pause for the 30m gap and continues
        repository.recoverSessionResumeFromNow(s1.id).getOrThrow()
        fakeTime.currentEpochMillis += 5 * 60_000L // 5m more active work
        val finalFinished = repository.finishFocusSession(s1.id).getOrThrow()
        assertEquals(1800L, finalFinished.focusedDurationSeconds) // 25m + 5m = 30m (1800s)
        assertEquals(lastConfirmedBeforeCrash + 35 * 60_000L, finalFinished.endedAtEpochMillis)
    }

    @Test
    fun habitCompletion_isUniquePerHabitAndDateAndPreservesScheduleHistory() = runBlocking {
        val day1 = fakeTime.todayLocalDate() // Sunday 2025-11-16
        val habit = repository.createHabit(
            name = "Read 10 pages",
            weekdays = setOf(day1.dayOfWeek),
            effectiveFrom = day1
        ).getOrThrow()

        // Toggle complete then incomplete then complete on same day -> single record updated
        repository.setHabitCompletionForDate(habit.id, day1, true).getOrThrow()
        repository.setHabitCompletionForDate(habit.id, day1, false).getOrThrow()
        repository.setHabitCompletionForDate(habit.id, day1, true).getOrThrow()

        val completions = repository.observeHabitCompletionsForDate(day1).first()
        assertEquals(1, completions.size)
        assertTrue(completions.first().isCompleted)

        // Future date completion must be rejected
        val futureAttempt = repository.setHabitCompletionForDate(habit.id, day1.plusDays(1), true)
        assertTrue(futureAttempt.isFailure)
    }

    @Test
    fun financeAndDailyReview_enforceConstraintsAndPreserveSourceSeparation() = runBlocking {
        val today = fakeTime.todayLocalDate()
        val tx = repository.saveTransaction(
            type = TransactionType.EXPENSE,
            amountMinor = 450_00L,
            currencyCode = "INR",
            category = "Food",
            transactionDate = today,
            note = "Groceries"
        ).getOrThrow()

        assertEquals(1, repository.observeTransactionsForDate(today).first().size)

        // Save draft then completed review for the same date -> updates unique review record
        repository.saveDailyReview(
            reviewDate = today,
            wentWell = "Focused well",
            improveTomorrow = "Start earlier",
            remember = null,
            status = ReviewStatus.DRAFT
        ).getOrThrow()

        val completedReview = repository.saveDailyReview(
            reviewDate = today,
            wentWell = "Focused well and finished tasks",
            improveTomorrow = "Start earlier",
            remember = "Good day",
            status = ReviewStatus.COMPLETED
        ).getOrThrow()

        assertEquals(ReviewStatus.COMPLETED, completedReview.status)
        assertEquals(1, repository.observeAllReviews().first().size)

        // Future date cannot be marked completed
        val futureReviewAttempt = repository.saveDailyReview(
            reviewDate = today.plusDays(1),
            wentWell = "Future",
            improveTomorrow = null,
            remember = null,
            status = ReviewStatus.COMPLETED
        )
        assertTrue(futureReviewAttempt.isFailure)

        // Deleting transaction removes it from date list
        repository.deleteTransaction(tx.id).getOrThrow()
        assertEquals(0, repository.observeTransactionsForDate(today).first().size)
    }
}
