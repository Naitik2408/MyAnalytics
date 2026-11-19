package com.lifeanalytics.app.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lifeanalytics.app.core.designsystem.DarkLaColors
import com.lifeanalytics.app.core.designsystem.DefaultLaTypeScale
import com.lifeanalytics.app.core.designsystem.LaRadii
import com.lifeanalytics.app.core.designsystem.LightLaColors
import com.lifeanalytics.app.core.designsystem.ModuleTint
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
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
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
    fun designSystemV12_tokensMeetContrastTabularAndModuleTintRequirements() {
        fun contrastRatio(fg: Color, bg: Color): Float {
            val l1 = fg.luminance()
            val l2 = bg.luminance()
            val brightest = max(l1, l2)
            val darkest = min(l1, l2)
            return (brightest + 0.05f) / (darkest + 0.05f)
        }

        // Light mode WCAG AA >= 4.5:1 checks
        assertTrue(contrastRatio(LightLaColors.textPrimary, LightLaColors.surface) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.textSecondary, LightLaColors.surface) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onInk, LightLaColors.ink) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onTintSecondary, LightLaColors.tints.lavender.base) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onTintSecondary, LightLaColors.tints.mint.base) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onTintSecondary, LightLaColors.tints.blue.base) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onTintSecondary, LightLaColors.tints.rose.base) >= 4.5f)
        assertTrue(contrastRatio(LightLaColors.onTintSecondary, LightLaColors.tints.butter.base) >= 4.5f)

        // Dark mode WCAG AA >= 4.5:1 checks
        assertTrue(contrastRatio(DarkLaColors.textPrimary, DarkLaColors.surface) >= 4.5f)
        assertTrue(contrastRatio(DarkLaColors.textSecondary, DarkLaColors.surface) >= 4.5f)
        assertTrue(contrastRatio(DarkLaColors.onInk, DarkLaColors.ink) >= 4.5f)
        assertTrue(contrastRatio(DarkLaColors.onTintSecondary, DarkLaColors.tints.lavender.base) >= 4.5f)

        // Tabular figures enabled on metrics, timers, and chart micro labels
        assertEquals("tnum", DefaultLaTypeScale.metricValue.fontFeatureSettings)
        assertEquals("tnum", DefaultLaTypeScale.timerValue.fontFeatureSettings)
        assertEquals("tnum", DefaultLaTypeScale.chartMicroLabel.fontFeatureSettings)

        // Module tint map (Section 4.4)
        assertEquals(LightLaColors.tints.lavender, LightLaColors.tintFor(ModuleTint.TASKS))
        assertEquals(LightLaColors.tints.mint, LightLaColors.tintFor(ModuleTint.FOCUS))
        assertEquals(LightLaColors.tints.butter, LightLaColors.tintFor(ModuleTint.HABITS))
        assertEquals(LightLaColors.tints.blue, LightLaColors.tintFor(ModuleTint.FINANCE))
        assertEquals(LightLaColors.tints.rose, LightLaColors.tintFor(ModuleTint.REVIEW))
        assertEquals(LightLaColors.tints.lavender, LightLaColors.tintFor(ModuleTint.INSIGHTS))

        // Concentric radius helper (Section 6.2)
        val radii = LaRadii()
        assertNotNull(radii.concentricInner(32.dp, 20.dp))
    }

    @Test
    fun manifest_declaresNoForbiddenPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()
        // V1.1 Permission Policy:
        // - INTERNET and ACCESS_NETWORK_STATE are approved and declared solely for user-initiated Supabase Cloud Backup & Restore.
        // - Sensitive device permissions (camera, mic, location, contacts, storage) remain strictly forbidden.
        val forbidden = setOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        for (perm in forbidden) {
            assertFalse("Manifest must not request $perm", requested.contains(perm))
        }
        assertTrue("Manifest must declare INTERNET for cloud backup", requested.contains(android.Manifest.permission.INTERNET))
    }

    @Test
    fun sampleDataset_seedsOctober2026WithVariationPreservesPersonalDataAndPopulatesHeatmap() = runBlocking {
        // Set reference clock to 2026-11-19T09:00:00Z
        fakeEpochMillis = java.time.LocalDate.of(2026, 11, 19)
            .atTime(9, 0)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val today = timeProvider.todayLocalDate()
        assertEquals(java.time.LocalDate.of(2026, 11, 19), today)

        // 1. Insert a personal user task and personal habit first
        val personalTask = repository.createTask(
            title = "Personal real user task",
            category = TaskCategory.PERSONAL,
            plannedDate = today,
            plannedTime = null,
            estimatedDurationMinutes = 25,
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.CAREER_PRODUCTIVITY
        ).getOrThrow()

        val personalHabit = repository.createHabit(
            name = "Personal real habit",
            weekdays = DayOfWeek.entries.toSet(),
            effectiveFrom = today.minusDays(5),
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.HEALTH_LIFESTYLE
        ).getOrThrow()

        assertFalse(repository.hasDemoSampleData())

        // 2. Seed the October 2026 sample dataset
        val summary1 = repository.seedDemoSampleMonth("INR").getOrThrow()
        assertEquals("October 2026", summary1.monthLabel)
        assertEquals(5, summary1.habitsSeeded)
        assertTrue(summary1.tasksSeeded >= 50)
        assertTrue(summary1.habitCompletionsSeeded >= 60)
        assertTrue(summary1.transactionsSeeded >= 15)
        assertTrue(repository.hasDemoSampleData())

        // 3. Re-run seedDemoSampleMonth to verify idempotency (no duplicate records)
        val totalTasksAfterFirstSeed = repository.observeAllTasks().first().size
        val totalHabitsAfterFirstSeed = repository.observeAllHabits().first().size
        val totalTxAfterFirstSeed = repository.observeAllTransactions().first().size

        repository.seedDemoSampleMonth("INR").getOrThrow()
        assertEquals(totalTasksAfterFirstSeed, repository.observeAllTasks().first().size)
        assertEquals(totalHabitsAfterFirstSeed, repository.observeAllHabits().first().size)
        assertEquals(totalTxAfterFirstSeed, repository.observeAllTransactions().first().size)

        // 4. Verify 365-day heatmap and October 2026 analytics calculated via real LifeAnalyticsEngine
        val allHabits = repository.observeAllHabits().first()
        val allCompletions = repository.observeAllHabitCompletions().first()
        val allTasks = repository.observeAllTasks().first()
        val allTransactions = repository.observeAllTransactions().first()
        val demoBudget = com.lifeanalytics.app.data.demo.SampleDataSeeder.DEMO_MONTHLY_BUDGET_MINOR

        val octPerformance = com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.MONTHLY,
            anchorDate = java.time.LocalDate.of(2026, 10, 15),
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = demoBudget,
            computeComparison = false
        )

        // All 3 life areas (Health, Career, Finance) must have valid calculated scores in October 2026
        assertEquals(3, octPerformance.availableCategoryCount)
        assertNotNull(octPerformance.overallScore)
        assertNotNull(octPerformance.health.score)
        assertNotNull(octPerformance.career.score)
        assertNotNull(octPerformance.finance.score)
        assertFalse(octPerformance.career.isPartial)

        val heatmap = com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine.buildYearCalendar(
            year = 2026,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = demoBudget
        )
        assertEquals(365, heatmap.totalDaysInYear)

        val octCells = heatmap.days.filter {
            it.date.monthValue == 10
        }
        assertEquals(31, octCells.size)

        // Verify variation across October 2026:
        // - Unrecorded rest days (Oct 11 & Oct 25) have ELAPSED_NO_DATA and null score
        val oct11 = octCells.first { it.date.dayOfMonth == 11 }
        val oct25 = octCells.first { it.date.dayOfMonth == 25 }
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_NO_DATA, oct11.state)
        assertEquals(null, oct11.overallScore)
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_NO_DATA, oct25.state)
        assertEquals(null, oct25.overallScore)

        // - Low-performance day (Oct 5) has lower score and lower intensity than high-performance day (Oct 1)
        val oct1 = octCells.first { it.date.dayOfMonth == 1 }
        val oct2 = octCells.first { it.date.dayOfMonth == 2 }
        val oct5 = octCells.first { it.date.dayOfMonth == 5 }
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_SCORED, oct1.state)
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_SCORED, oct2.state)
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.ELAPSED_SCORED, oct5.state)
        assertTrue("High day Oct 1 (${oct1.overallScore}) must exceed Average day Oct 2 (${oct2.overallScore})", oct1.overallScore!! > oct2.overallScore!!)
        assertTrue("Average day Oct 2 (${oct2.overallScore}) must exceed Low day Oct 5 (${oct5.overallScore})", oct2.overallScore!! > oct5.overallScore!!)

        // - Future days (after Nov 19, 2026) have FUTURE state
        val dec1 = heatmap.days.first { it.date == java.time.LocalDate.of(2026, 12, 1) }
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.FUTURE, dec1.state)

        // 5. Remove demo data and verify personal records remain completely intact
        repository.removeDemoSampleMonth().getOrThrow()
        assertFalse(repository.hasDemoSampleData())

        val remainingTasks = repository.observeAllTasks().first()
        val remainingHabits = repository.observeAllHabits().first()
        assertEquals(1, remainingTasks.size)
        assertEquals(personalTask.id, remainingTasks.first().id)
        assertEquals(1, remainingHabits.size)
        assertEquals(personalHabit.id, remainingHabits.first().id)
    }
}

