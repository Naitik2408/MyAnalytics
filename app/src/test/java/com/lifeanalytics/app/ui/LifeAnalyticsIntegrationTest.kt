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

        // 1. Insert personal user habits (Health daily + Career weekdays-only), a personal task, and a real completion
        val personalTask = repository.createTask(
            title = "Personal real user task",
            category = TaskCategory.WORK,
            plannedDate = today,
            plannedTime = null,
            estimatedDurationMinutes = 25,
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.CAREER_PRODUCTIVITY
        ).getOrThrow()

        val personalHealthHabit = repository.createHabit(
            name = "Drink 2L water",
            weekdays = DayOfWeek.entries.toSet(),
            effectiveFrom = today.minusDays(5),
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.HEALTH_LIFESTYLE
        ).getOrThrow()

        val personalCareerHabit = repository.createHabit(
            name = "English speaking practice",
            weekdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            effectiveFrom = today.minusDays(5),
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.CAREER_PRODUCTIVITY
        ).getOrThrow()

        val realCompletionDate = today.minusDays(2) // 2026-11-17 (Tuesday)
        val realCompletion = repository.setHabitCompletionForDate(
            habitId = personalHealthHabit.id,
            date = realCompletionDate,
            completed = true
        ).getOrThrow()

        assertFalse(repository.hasHistoricalSampleData())

        // 2. Build June 1 – November 18, 2026 historical sample data
        val summary1 = repository.rebuildHistoricalSampleData("INR").getOrThrow()
        assertEquals("Jun 1 – Nov 18, 2026", summary1.rangeLabel)
        assertEquals(2, summary1.existingHabitsUsed)
        assertTrue(summary1.habitCompletionsSeeded >= 150)
        assertTrue(summary1.tasksSeeded >= 150)
        assertTrue(summary1.transactionsSeeded >= 90)
        assertTrue(repository.hasHistoricalSampleData())

        // Zero fake habits created — only the user's 2 real habits exist
        val allHabitsAfterSeed = repository.observeAllHabits().first()
        assertEquals(2, allHabitsAfterSeed.size)
        assertTrue(allHabitsAfterSeed.none { it.name.contains("[Demo]") || it.name.contains("[Sample]") })

        // Verify unscheduled days respected: weekday-only career habit has 0 Saturday/Sunday completions
        val allCompletions = repository.observeAllHabitCompletions().first()
        val careerCompletions = allCompletions.filter { it.habitId == personalCareerHabit.id }
        assertTrue(careerCompletions.isNotEmpty())
        assertTrue(
            "Weekday-only habit must never have weekend completions",
            careerCompletions.none {
                it.completionDate.dayOfWeek == DayOfWeek.SATURDAY || it.completionDate.dayOfWeek == DayOfWeek.SUNDAY
            }
        )

        // Verify real completion on 2026-11-17 was preserved (same ID, not overwritten)
        val preservedCompletion = allCompletions.first {
            it.habitId == personalHealthHabit.id && it.completionDate == realCompletionDate
        }
        assertEquals(realCompletion.id, preservedCompletion.id)

        // Verify all generated sample records fall strictly within June 1 – November 18, 2026
        val sampleStart = java.time.LocalDate.of(2026, 6, 1)
        val sampleEnd = java.time.LocalDate.of(2026, 11, 18)
        val allTasks = repository.observeAllTasks().first()
        val sampleTasks = allTasks.filter { it.id.startsWith(com.lifeanalytics.app.data.demo.SampleDataSeeder.DEMO_ID_PREFIX) }
        assertTrue(sampleTasks.all { !it.plannedDate.isBefore(sampleStart) && !it.plannedDate.isAfter(sampleEnd) })

        val allTransactions = repository.observeAllTransactions().first()
        assertTrue(allTransactions.all { !it.transactionDate.isBefore(sampleStart) && !it.transactionDate.isAfter(sampleEnd) })

        // 3. Re-run rebuildHistoricalSampleData to verify idempotency (no duplicate records)
        val totalTasksAfterFirst = allTasks.size
        val totalCompletionsAfterFirst = allCompletions.size
        val totalTxAfterFirst = allTransactions.size

        repository.rebuildHistoricalSampleData("INR").getOrThrow()
        assertEquals(totalTasksAfterFirst, repository.observeAllTasks().first().size)
        assertEquals(totalCompletionsAfterFirst, repository.observeAllHabitCompletions().first().size)
        assertEquals(totalTxAfterFirst, repository.observeAllTransactions().first().size)

        // 4. Verify analytics across June, October, and November reflect real calculated counts
        val octPerformance = com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.MONTHLY,
            anchorDate = java.time.LocalDate.of(2026, 10, 15),
            today = today,
            habits = repository.observeAllHabits().first(),
            completions = repository.observeAllHabitCompletions().first(),
            tasks = repository.observeAllTasks().first(),
            transactions = repository.observeAllTransactions().first(),
            monthlyBudgetMinor = 4_500_000L,
            computeComparison = true
        )
        assertEquals(3, octPerformance.availableCategoryCount)
        assertNotNull(octPerformance.overallScore)
        assertTrue(octPerformance.health.totalScheduledHabits > 0)
        assertTrue(octPerformance.health.completedScheduledHabits in 1 until octPerformance.health.totalScheduledHabits)
        assertTrue(octPerformance.health.healthTasksPlanned > 0)
        assertTrue(octPerformance.career.totalPlannedTasks > 0)
        assertTrue(octPerformance.career.totalScheduledCareerHabits > 0)
        assertTrue(octPerformance.finance.totalIncomeMinor > 0L)
        assertTrue(octPerformance.finance.totalExpenseMinor > 0L)

        // Verify 365-day heatmap has scored days across Jun–Nov and FUTURE days after today
        val heatmap = com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine.buildYearCalendar(
            year = 2026,
            today = today,
            habits = repository.observeAllHabits().first(),
            completions = repository.observeAllHabitCompletions().first(),
            tasks = repository.observeAllTasks().first(),
            transactions = repository.observeAllTransactions().first(),
            monthlyBudgetMinor = 4_500_000L
        )
        assertEquals(365, heatmap.totalDaysInYear)
        assertTrue(heatmap.scoredDaysCount >= 150)
        val dec1 = heatmap.days.first { it.date == java.time.LocalDate.of(2026, 12, 1) }
        assertEquals(com.lifeanalytics.app.domain.analytics.YearCalendarDayState.FUTURE, dec1.state)

        // 5. Remove historical sample data and verify all personal records remain completely intact
        repository.removeHistoricalSampleData().getOrThrow()
        assertFalse(repository.hasHistoricalSampleData())

        val remainingTasks = repository.observeAllTasks().first()
        val remainingHabits = repository.observeAllHabits().first()
        val remainingCompletions = repository.observeAllHabitCompletions().first()
        val remainingTransactions = repository.observeAllTransactions().first()
        assertEquals(1, remainingTasks.size)
        assertEquals(personalTask.id, remainingTasks.first().id)
        assertEquals(2, remainingHabits.size)
        assertEquals(1, remainingCompletions.size)
        assertEquals(realCompletion.id, remainingCompletions.first().id)
        assertEquals(0, remainingTransactions.size)
    }

    @Test
    fun optionalTaskLifeArea_andHabitReminderTime_persistAndExcludeUnassignedFromAreaAnalytics() = runBlocking {
        val today = timeProvider.todayLocalDate()

        // 1. Create an unassigned task (lifeArea = null)
        val unassigned = repository.createTask(
            title = "Pick up a package for someone",
            category = TaskCategory.PERSONAL,
            plannedDate = today,
            plannedTime = null,
            estimatedDurationMinutes = null,
            lifeArea = null
        ).getOrThrow()
        assertEquals(null, unassigned.lifeArea)

        // 2. Create a Career task, then clear its Life Area via updateTask
        val careerTask = repository.createTask(
            title = "Prepare interview notes",
            category = TaskCategory.WORK,
            plannedDate = today,
            plannedTime = java.time.LocalTime.of(10, 0),
            estimatedDurationMinutes = 30,
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.CAREER_PRODUCTIVITY
        ).getOrThrow()
        assertEquals(com.lifeanalytics.app.domain.model.LifeArea.CAREER_PRODUCTIVITY, careerTask.lifeArea)

        val cleared = repository.updateTask(
            taskId = careerTask.id,
            title = careerTask.title,
            category = careerTask.category,
            plannedDate = careerTask.plannedDate,
            plannedTime = careerTask.plannedTime,
            estimatedDurationMinutes = careerTask.estimatedDurationMinutes,
            lifeArea = null
        ).getOrThrow()
        assertEquals(null, cleared.lifeArea)

        // 3. Verify unassigned tasks are NOT counted in Health or Career analytics
        val allTasks = repository.observeAllTasks().first()
        val perf = com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow.DAILY,
            anchorDate = today,
            today = today,
            habits = emptyList(),
            completions = emptyList(),
            tasks = allTasks,
            transactions = emptyList(),
            monthlyBudgetMinor = null,
            computeComparison = false
        )
        assertEquals(0, perf.health.healthTasksPlanned)
        assertEquals(0, perf.career.totalPlannedTasks)

        // 4. Create habit with reminderTime, then update and clear reminderTime
        val habit = repository.createHabit(
            name = "Morning stretch",
            weekdays = DayOfWeek.entries.toSet(),
            effectiveFrom = today,
            lifeArea = com.lifeanalytics.app.domain.model.LifeArea.HEALTH_LIFESTYLE,
            reminderTime = java.time.LocalTime.of(7, 30)
        ).getOrThrow()
        assertEquals(java.time.LocalTime.of(7, 30), habit.reminderTime)

        val updatedHabit = repository.updateHabit(
            habitId = habit.id,
            name = habit.name,
            weekdays = DayOfWeek.entries.toSet(),
            effectiveDate = today,
            lifeArea = habit.lifeArea,
            reminderTime = null
        ).getOrThrow()
        assertEquals(null, updatedHabit.reminderTime)
    }
}


