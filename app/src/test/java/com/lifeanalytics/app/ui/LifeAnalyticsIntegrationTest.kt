package com.lifeanalytics.app.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifeanalytics.app.core.time.TimeProvider
import com.lifeanalytics.app.data.local.LifeAnalyticsDatabase
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppThemeOption
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeAnalyticsIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: LifeAnalyticsDatabase
    private lateinit var repository: LifeAnalyticsRepository
    private lateinit var preferencesRepository: PreferencesRepository

    private var fakeEpochMillis = 1_763_283_600_000L // 2025-11-16T09:00:00Z
    private var fakeElapsedMillis = 500_000L

    private val timeProvider = object : TimeProvider {
        override fun nowEpochMillis(): Long = fakeEpochMillis
        override fun elapsedRealtimeMillis(): Long = fakeElapsedMillis
        override fun currentZoneId(): ZoneId = ZoneId.of("UTC")
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifeAnalyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifeAnalyticsRepository(database, timeProvider)

        val dataStoreFile = File(tempFolder.newFolder(), "test_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            produceFile = { dataStoreFile }
        )
        preferencesRepository = PreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun preferencesRepository_persistsThemeAndCurrency() = runBlocking {
        val initial = preferencesRepository.preferencesFlow.first()
        assertEquals(AppThemeOption.LIGHT, initial.themeOption)
        assertEquals("INR", initial.currencyCode)

        preferencesRepository.setThemeOption(AppThemeOption.DARK)
        preferencesRepository.setCurrencyCode("USD")

        val updated = preferencesRepository.preferencesFlow.first()
        assertEquals(AppThemeOption.DARK, updated.themeOption)
        assertEquals("USD", updated.currencyCode)
    }

    @Test
    fun crossModuleConsistency_homeInsightsAndDailyReviewReflectSingleSourceOfTruth() = runBlocking {
        val today = timeProvider.todayLocalDate()

        // 1. Create 2 tasks for today, complete 1
        val task1 = repository.createTask(
            title = "Write architecture doc",
            category = TaskCategory.WORK,
            plannedDate = today,
            plannedTime = LocalTime.of(10, 0),
            estimatedDurationMinutes = 60
        ).getOrThrow()
        val task2 = repository.createTask(
            title = "Review pull requests",
            category = TaskCategory.WORK,
            plannedDate = today,
            plannedTime = LocalTime.of(14, 0),
            estimatedDurationMinutes = 30
        ).getOrThrow()
        repository.setTaskCompleted(task1.id, true).getOrThrow()

        // 2. Run a 25-minute focus session linked to task2 (without completing task2!)
        val session = repository.startFocusSession(taskId = task2.id).getOrThrow()
        fakeEpochMillis += 25 * 60 * 1000L
        fakeElapsedMillis += 25 * 60 * 1000L
        val finishedSession = repository.finishFocusSession(session.id).getOrThrow()
        assertEquals(FocusSessionStatus.COMPLETED, finishedSession.status)
        assertEquals(1500L, finishedSession.focusedDurationSeconds)

        // Verify task2 is STILL incomplete (focus session never auto-completes a task)
        val reloadedTask2 = repository.getTaskById(task2.id)
        assertNotNull(reloadedTask2)
        assertFalse(reloadedTask2!!.isCompleted)

        // 3. Create a daily habit and mark it completed for today
        val habit = repository.createHabit(
            name = "Evening walk",
            weekdays = DayOfWeek.entries.toSet(),
            effectiveFrom = today.minusDays(7)
        ).getOrThrow()
        repository.setHabitCompletionForDate(habit.id, today, true).getOrThrow()

        // 4. Record an expense and an income for today
        repository.saveTransaction(
            type = TransactionType.INCOME,
            amountMinor = 500_000L, // ₹5,000.00
            currencyCode = "INR",
            category = "Freelance",
            transactionDate = today,
            note = "Client milestone"
        ).getOrThrow()
        repository.saveTransaction(
            type = TransactionType.EXPENSE,
            amountMinor = 35_000L, // ₹350.00
            currencyCode = "INR",
            category = "Food",
            transactionDate = today,
            note = "Lunch"
        ).getOrThrow()

        // 5. Save a Daily Review and verify source summaries match across Home, Review, and Insights
        val review = repository.saveDailyReview(
            reviewDate = today,
            wentWell = "Solid focus session on PR review",
            improveTomorrow = "Start earlier",
            remember = null,
            status = ReviewStatus.COMPLETED
        ).getOrThrow()
        assertEquals(ReviewStatus.COMPLETED, review.status)

        val tasksToday = repository.observeTasksForDate(today).first()
        val sessionsToday = repository.observeSessionsForDate(today).first()
        val habitsAll = repository.observeAllHabits().first()
        val completionsToday = repository.observeHabitCompletionsForDate(today).first()
        val transactionsToday = repository.observeTransactionsForDate(today).first()

        val taskSummary = DomainCalculations.calculateTaskSummary(tasksToday)
        val focusSeconds = DomainCalculations.sumCompletedFocusDurationSeconds(sessionsToday)
        val habitSummary = DomainCalculations.evaluateHabitsForDate(habitsAll, completionsToday, today)
        val financeSummary = DomainCalculations.calculateFinancePeriodSummary(transactionsToday)

        assertEquals(1, taskSummary.completedCount)
        assertEquals(2, taskSummary.totalCount)
        assertEquals(90, taskSummary.totalEstimatedMinutes)
        assertEquals(1500L, focusSeconds)
        assertEquals(1, habitSummary.completedDueCount)
        assertEquals(1, habitSummary.totalDueCount)
        assertEquals(500_000L, financeSummary.totalIncomeMinor)
        assertEquals(35_000L, financeSummary.totalExpenseMinor)
        assertEquals(465_000L, financeSummary.netForPeriodMinor)
    }

    @Test
    fun manifest_declaresNoForbiddenPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()
        val forbidden = setOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET
        )
        for (perm in forbidden) {
            assertFalse("Manifest must not request $perm", requested.contains(perm))
        }
        assertTrue(true)
    }
}
