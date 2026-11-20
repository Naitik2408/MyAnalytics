package com.lifeanalytics.app.data.demo

import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.local.dao.FinanceDao
import com.lifeanalytics.app.data.local.dao.HabitDao
import com.lifeanalytics.app.data.local.dao.TaskDao
import com.lifeanalytics.app.data.local.entity.FinanceTransactionEntity
import com.lifeanalytics.app.data.local.entity.HabitCompletionEntity
import com.lifeanalytics.app.data.local.entity.HabitEntity
import com.lifeanalytics.app.data.local.entity.HabitSchedulePeriodEntity
import com.lifeanalytics.app.data.local.entity.TaskEntity
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import com.lifeanalytics.app.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/**
 * Deterministic, isolated historical sample data seeder covering
 * **June 1, 2026 through November 18, 2026, inclusive**.
 *
 * Isolation & Data-Safety Guarantees:
 * 1. **Zero fake habits**: Never creates new `HabitEntity` rows. Instead, inspects the user's
 *    existing real habits, respects their schedules/weekdays, and attaches removable
 *    `demo_hist_` schedule periods (only when a habit started after June 1) and `demo_hist_`
 *    completions without modifying any real habit or real completion record.
 * 2. **Strict ID prefix isolation**: Every generated schedule period, completion, task, and
 *    finance transaction uses [DEMO_ID_PREFIX] (`demo_hist_`). Generated tasks and finance
 *    notes also include [DEMO_TAG] (`[Sample]`) so they can never be mistaken for personal data.
 * 3. **Legacy cleanup**: [removeLegacyOctoberDemoData] cleans up any old `demo_oct2026_`
 *    records (including the old 5 fake `[Demo]` habits) without touching personal records.
 * 4. **Idempotent & reversible**: Running [rebuildHistoricalSampleData] multiple times replaces
 *    only `demo_hist_*` rows without duplicating records or touching personal records.
 *    [removeHistoricalSampleData] removes all `demo_hist_*` rows cleanly.
 * 5. **Real analytics only**: All dashboards, summary cards, and the 365-day heatmap compute
 *    directly from these SQLite records through `LifeAnalyticsEngine` and `DomainCalculations`.
 */
object SampleDataSeeder {

    const val DEMO_ID_PREFIX = "demo_hist_"
    const val LEGACY_OCT2026_PREFIX = "demo_oct2026_"
    const val DEMO_TAG = "[Sample]"

    val SAMPLE_START_DATE: LocalDate = LocalDate.of(2026, 6, 1)
    val SAMPLE_END_DATE: LocalDate = LocalDate.of(2026, 11, 18)

    data class SeedSummary(
        val rangeLabel: String,
        val existingHabitsUsed: Int,
        val habitCompletionsSeeded: Int,
        val tasksSeeded: Int,
        val transactionsSeeded: Int
    )

    data class HistoricalSampleBundle(
        val schedulePeriods: List<HabitSchedulePeriodEntity>,
        val completions: List<HabitCompletionEntity>,
        val tasks: List<TaskEntity>,
        val transactions: List<FinanceTransactionEntity>
    )

    /**
     * Pure deterministic builder for June 1 – November 18, 2026 historical sample records,
     * driven by the user's real existing habits, schedules, completions, and tasks.
     * Exposed as a pure function so unit tests can verify boundary conditions directly.
     */
    fun buildHistoricalSampleBundle(
        existingHabits: List<HabitEntity>,
        existingSchedulePeriods: List<HabitSchedulePeriodEntity>,
        existingCompletions: List<HabitCompletionEntity>,
        existingTasks: List<TaskEntity>,
        currencyCode: String = "INR",
        startDate: LocalDate = SAMPLE_START_DATE,
        endDate: LocalDate = SAMPLE_END_DATE
    ): HistoricalSampleBundle {
        val zone = ZoneId.systemDefault()
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()

        // Filter to real (non-demo) records only
        val realHabits = existingHabits.filter { !isDemoId(it.id) }
        val realSchedulesByHabit = existingSchedulePeriods
            .filter { !isDemoId(it.id) }
            .groupBy { it.habitId }
        val realCompletionKeys = existingCompletions
            .filter { !isDemoId(it.id) }
            .map { it.habitId to it.completionDate }
            .toSet()
        val realTasks = existingTasks.filter { !isDemoId(it.id) }

        val generatedSchedules = mutableListOf<HabitSchedulePeriodEntity>()
        val generatedCompletions = mutableListOf<HabitCompletionEntity>()
        val generatedTasks = mutableListOf<TaskEntity>()
        val generatedTransactions = mutableListOf<FinanceTransactionEntity>()

        // ---------------------------------------------------------------------
        // 1. Historical habit completions for existing real habits ONLY
        // ---------------------------------------------------------------------
        for (habit in realHabits) {
            val habitRealPeriods = realSchedulesByHabit[habit.id].orEmpty()
            if (habitRealPeriods.isEmpty()) continue

            val earliestPeriod = habitRealPeriods.minBy { it.effectiveFrom }
            val earliestFromDate = DateAttributionPolicy.parseIsoDate(earliestPeriod.effectiveFrom)
            val deactivatedFromDate = habit.deactivatedFromLocalDate?.let {
                DateAttributionPolicy.parseIsoDate(it)
            }

            // If the real habit was created after startDate, attach an isolated demo schedule
            // period covering [startDate, earliestFromDate) with the exact same weekdays.
            val historicalPeriod: HabitSchedulePeriodEntity? = if (earliestFromDate.isAfter(startDate)) {
                val periodEnd = if (deactivatedFromDate != null && deactivatedFromDate.isBefore(earliestFromDate)) {
                    deactivatedFromDate
                } else {
                    earliestFromDate
                }
                if (periodEnd.isAfter(startDate)) {
                    HabitSchedulePeriodEntity(
                        id = "${DEMO_ID_PREFIX}sched_${habit.id}",
                        habitId = habit.id,
                        effectiveFrom = DateAttributionPolicy.formatIsoDate(startDate),
                        effectiveTo = DateAttributionPolicy.formatIsoDate(periodEnd),
                        scheduleType = earliestPeriod.scheduleType,
                        weekdaysCsv = earliestPeriod.weekdaysCsv,
                        createdAtEpochMillis = startMillis
                    ).also { generatedSchedules.add(it) }
                } else {
                    null
                }
            } else {
                null
            }

            val combinedPeriods = (habitRealPeriods + listOfNotNull(historicalPeriod))
                .sortedByDescending { it.effectiveFrom }

            var date = startDate
            while (!date.isAfter(endDate)) {
                // Respect deactivation boundary
                if (deactivatedFromDate != null && !date.isBefore(deactivatedFromDate)) {
                    date = date.plusDays(1)
                    continue
                }

                val dateIso = DateAttributionPolicy.formatIsoDate(date)

                // Find effective schedule on `date`
                val activePeriod = combinedPeriods.firstOrNull { period ->
                    val from = DateAttributionPolicy.parseIsoDate(period.effectiveFrom)
                    val to = period.effectiveTo?.let { DateAttributionPolicy.parseIsoDate(it) }
                    !date.isBefore(from) && (to == null || date.isBefore(to))
                }

                if (activePeriod != null) {
                    val dueWeekdays = LifeAnalyticsRepository.decodeWeekdaysCsv(activePeriod.weekdaysCsv)
                    val isScheduled = date.dayOfWeek in dueWeekdays

                    // Respect schedule strictly AND never overwrite an existing real completion
                    if (isScheduled && (habit.id to dateIso) !in realCompletionKeys) {
                        if (shouldCompleteHabitOnDate(habit.id, date)) {
                            val completedAt = date.atTime(8, 30)
                                .atZone(zone).toInstant().toEpochMilli() +
                                (stableHash("${habit.id}:$dateIso") % 3600) * 1000L
                            generatedCompletions.add(
                                HabitCompletionEntity(
                                    id = "${DEMO_ID_PREFIX}comp_${habit.id}_$dateIso",
                                    habitId = habit.id,
                                    completionDate = dateIso,
                                    isCompleted = true,
                                    completedAtEpochMillis = completedAt,
                                    updatedAtEpochMillis = completedAt
                                )
                            )
                        }
                    }
                }
                date = date.plusDays(1)
            }
        }

        // ---------------------------------------------------------------------
        // 2. Historical task records reusing existing task categories/titles
        // ---------------------------------------------------------------------
        val realTaskDatesByArea: Map<Pair<LocalDate, LifeArea?>, Boolean> = realTasks
            .associate { (DateAttributionPolicy.parseIsoDate(it.plannedDate) to LifeArea.fromNameOrNull(it.lifeArea)) to true }

        val userCareerTemplates = realTasks
            .filter { LifeArea.fromNameOrNull(it.lifeArea) == LifeArea.CAREER_PRODUCTIVITY }
            .map { it.title to TaskCategory.fromNameOrDefault(it.category) }
            .distinctBy { it.first }
        val userHealthTemplates = realTasks
            .filter { LifeArea.fromNameOrNull(it.lifeArea) == LifeArea.HEALTH_LIFESTYLE }
            .map { it.title to TaskCategory.fromNameOrDefault(it.category) }
            .distinctBy { it.first }

        val careerCatalog = userCareerTemplates.ifEmpty {
            listOf(
                "Review project milestones & notes" to TaskCategory.WORK,
                "Focused study & skill practice" to TaskCategory.STUDY,
                "Complete planned sprint deliverable" to TaskCategory.WORK,
                "English speaking & reading session" to TaskCategory.STUDY,
                "Organize weekly priorities" to TaskCategory.WORK
            )
        }
        val healthCatalog = userHealthTemplates.ifEmpty {
            listOf(
                "Outdoor walk & stretching session" to TaskCategory.PERSONAL,
                "Plan balanced meals for the day" to TaskCategory.PERSONAL,
                "Evening mobility & recovery routine" to TaskCategory.PERSONAL
            )
        }

        var taskDate = startDate
        while (!taskDate.isAfter(endDate)) {
            val dateIso = DateAttributionPolicy.formatIsoDate(taskDate)
            val dayOfYear = taskDate.dayOfYear
            val dayStartMillis = taskDate.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

            // Leave occasional Sundays completely quiet so heatmap reflects natural rest days
            val isQuietRestDay = taskDate.dayOfWeek == DayOfWeek.SUNDAY && (dayOfYear % 2 == 0)
            if (!isQuietRestDay) {
                var sortIdx = 0

                // Career tasks: 1–2 on weekdays, 0–1 on weekends
                if ((taskDate to LifeArea.CAREER_PRODUCTIVITY) !in realTaskDatesByArea) {
                    val careerCount = when (taskDate.dayOfWeek) {
                        DayOfWeek.SUNDAY -> 0
                        DayOfWeek.SATURDAY -> if (dayOfYear % 3 == 0) 1 else 0
                        else -> if (dayOfYear % 3 == 0) 2 else 1
                    }
                    for (i in 0 until careerCount) {
                        val (rawTitle, category) = careerCatalog[(dayOfYear + i * 2) % careerCatalog.size]
                        val isDone = shouldCompleteTaskOnDate("career_$i", taskDate)
                        val completedAt = if (isDone) dayStartMillis + (i + 2) * 3600_000L else null
                        generatedTasks.add(
                            TaskEntity(
                                id = "${DEMO_ID_PREFIX}task_${dateIso}_c${i + 1}",
                                title = "$rawTitle $DEMO_TAG",
                                category = category.name,
                                plannedDate = dateIso,
                                plannedTime = String.format("%02d:00", 9 + i * 3),
                                estimatedDurationMinutes = 30 + (i * 15),
                                status = if (isDone) TaskStatus.COMPLETED.name else TaskStatus.PLANNED.name,
                                completedAtEpochMillis = completedAt,
                                sortOrder = sortIdx++,
                                createdAtEpochMillis = dayStartMillis,
                                updatedAtEpochMillis = completedAt ?: dayStartMillis,
                                lifeArea = LifeArea.CAREER_PRODUCTIVITY.name
                            )
                        )
                    }
                }

                // Health task: ~3-4 days per week
                if ((taskDate to LifeArea.HEALTH_LIFESTYLE) !in realTaskDatesByArea && (dayOfYear % 2 == 1)) {
                    val (rawTitle, category) = healthCatalog[(dayOfYear / 2) % healthCatalog.size]
                    val isDone = shouldCompleteTaskOnDate("health_0", taskDate)
                    val completedAt = if (isDone) dayStartMillis + 3 * 3600_000L else null
                    generatedTasks.add(
                        TaskEntity(
                            id = "${DEMO_ID_PREFIX}task_${dateIso}_h1",
                            title = "$rawTitle $DEMO_TAG",
                            category = category.name,
                            plannedDate = dateIso,
                            plannedTime = "07:30",
                            estimatedDurationMinutes = 30,
                            status = if (isDone) TaskStatus.COMPLETED.name else TaskStatus.PLANNED.name,
                            completedAtEpochMillis = completedAt,
                            sortOrder = sortIdx++,
                            createdAtEpochMillis = dayStartMillis,
                            updatedAtEpochMillis = completedAt ?: dayStartMillis,
                            lifeArea = LifeArea.HEALTH_LIFESTYLE.name
                        )
                    )
                }
            }
            taskDate = taskDate.plusDays(1)
        }

        // ---------------------------------------------------------------------
        // 3. Finance sample transactions across June 1 – November 18, 2026
        // ---------------------------------------------------------------------
        val expenseCatalog = listOf(
            Triple("Food", "Groceries & fresh produce", 95_000L..165_000L),
            Triple("Food", "Lunch & coffee", 35_000L..85_000L),
            Triple("Transport", "Commute & fuel", 45_000L..120_000L),
            Triple("Bills", "Internet & utility bill", 140_000L..260_000L),
            Triple("Health", "Pharmacy & wellness supplies", 60_000L..185_000L),
            Triple("Shopping", "Household essentials", 110_000L..290_000L),
            Triple("Entertainment", "Dinner & weekend outing", 80_000L..215_000L),
            Triple("Education", "Books & learning resources", 75_000L..195_000L)
        )

        var txDate = startDate
        while (!txDate.isAfter(endDate)) {
            val dateIso = DateAttributionPolicy.formatIsoDate(txDate)
            val dayMillis = txDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            val monthIndex = txDate.monthValue - 6 // 0..5 for Jun..Nov

            // Monthly income on the 1st (with realistic month-to-month variation)
            if (txDate.dayOfMonth == 1) {
                val salaryMinor = 8_200_000L + (monthIndex % 3) * 250_000L + (if (monthIndex == 4) 300_000L else 0L)
                generatedTransactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_${dateIso}_inc_salary",
                        type = TransactionType.INCOME.name,
                        amountMinor = salaryMinor,
                        currencyCode = currencyCode,
                        category = "Salary",
                        transactionDate = dateIso,
                        note = "Monthly salary credit $DEMO_TAG",
                        createdAtEpochMillis = dayMillis,
                        updatedAtEpochMillis = dayMillis
                    )
                )
            }

            // Occasional freelance/side income mid-month (not identical every month)
            if (txDate.dayOfMonth == 14 && monthIndex in setOf(0, 2, 3, 5)) {
                val sideMinor = 950_000L + (stableHash("side_$dateIso") % 8) * 125_000L
                generatedTransactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_${dateIso}_inc_side",
                        type = TransactionType.INCOME.name,
                        amountMinor = sideMinor,
                        currencyCode = currencyCode,
                        category = "Freelance",
                        transactionDate = dateIso,
                        note = "Consulting & side project payout $DEMO_TAG",
                        createdAtEpochMillis = dayMillis + 1800_000L,
                        updatedAtEpochMillis = dayMillis + 1800_000L
                    )
                )
            }

            // Realistic expenses on ~55% of days with varied amounts and categories
            val hash = stableHash("exp_$dateIso")
            if (hash % 100 < 58) {
                val (cat, noteText, range) = expenseCatalog[hash % expenseCatalog.size]
                val span = (range.last - range.first).coerceAtLeast(1L)
                val stepOffset = ((stableHash("amt_$dateIso") % 40) * span) / 40L
                // Round to nearest 500 minor units (₹5) for natural-looking amounts
                val amountMinor = ((range.first + stepOffset) / 500L) * 500L
                generatedTransactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_${dateIso}_exp",
                        type = TransactionType.EXPENSE.name,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        category = cat,
                        transactionDate = dateIso,
                        note = "$noteText $DEMO_TAG",
                        createdAtEpochMillis = dayMillis + 3600_000L,
                        updatedAtEpochMillis = dayMillis + 3600_000L
                    )
                )
            }

            txDate = txDate.plusDays(1)
        }

        return HistoricalSampleBundle(
            schedulePeriods = generatedSchedules,
            completions = generatedCompletions,
            tasks = generatedTasks,
            transactions = generatedTransactions
        )
    }

    /**
     * Rebuilds the June 1 – November 18, 2026 sample dataset idempotently for the user's
     * existing real habits & tasks, without modifying or deleting any real personal records.
     */
    suspend fun rebuildHistoricalSampleData(
        taskDao: TaskDao,
        habitDao: HabitDao,
        financeDao: FinanceDao,
        currencyCode: String = "INR"
    ): SeedSummary {
        // 1. Clean up any legacy Oct-2026 demo records and any previous `demo_hist_` rows
        removeLegacyOctoberDemoData(taskDao, habitDao, financeDao)
        removeHistoricalSampleData(taskDao, habitDao, financeDao)

        // 2. Inspect existing real habits, schedules, completions, and tasks
        val existingHabits = habitDao.getAllHabitEntities()
        val existingSchedules = habitDao.getAllSchedulePeriodEntities()
        val existingCompletions = habitDao.getAllCompletionEntities()
        val existingTasks = taskDao.getAllTasks()

        val bundle = buildHistoricalSampleBundle(
            existingHabits = existingHabits,
            existingSchedulePeriods = existingSchedules,
            existingCompletions = existingCompletions,
            existingTasks = existingTasks,
            currencyCode = currencyCode
        )

        // 3. Insert only isolated `demo_hist_*` records
        if (bundle.schedulePeriods.isNotEmpty()) {
            habitDao.upsertAllSchedulePeriods(bundle.schedulePeriods)
        }
        if (bundle.completions.isNotEmpty()) {
            habitDao.upsertAllCompletions(bundle.completions)
        }
        if (bundle.tasks.isNotEmpty()) {
            taskDao.upsertAllTasks(bundle.tasks)
        }
        if (bundle.transactions.isNotEmpty()) {
            financeDao.upsertAllTransactions(bundle.transactions)
        }

        val realHabitsUsed = existingHabits.count { !isDemoId(it.id) }
        return SeedSummary(
            rangeLabel = "Jun 1 – Nov 18, 2026",
            existingHabitsUsed = realHabitsUsed,
            habitCompletionsSeeded = bundle.completions.size,
            tasksSeeded = bundle.tasks.size,
            transactionsSeeded = bundle.transactions.size
        )
    }

    /**
     * Removes only `demo_hist_*` schedule periods, completions, tasks, and transactions.
     * Real user habits, real schedules, real completions, real tasks, and real transactions
     * are never modified or deleted.
     */
    suspend fun removeHistoricalSampleData(
        taskDao: TaskDao,
        habitDao: HabitDao,
        financeDao: FinanceDao
    ) {
        taskDao.getAllTasks()
            .filter { it.id.startsWith(DEMO_ID_PREFIX) }
            .forEach { taskDao.deleteTaskById(it.id) }

        habitDao.getAllCompletionEntities()
            .filter { it.id.startsWith(DEMO_ID_PREFIX) }
            .forEach { habitDao.deleteCompletionById(it.id) }

        habitDao.getAllSchedulePeriodEntities()
            .filter { it.id.startsWith(DEMO_ID_PREFIX) }
            .forEach { habitDao.deleteSchedulePeriodById(it.id) }

        financeDao.getAllTransactionEntities()
            .filter { it.id.startsWith(DEMO_ID_PREFIX) }
            .forEach { financeDao.deleteTransactionById(it.id) }
    }

    /**
     * Removes legacy `demo_oct2026_*` records (including the old 5 `[Demo]`-tagged habits)
     * so no artificial `[Demo]` habits remain in the user's database.
     */
    suspend fun removeLegacyOctoberDemoData(
        taskDao: TaskDao,
        habitDao: HabitDao,
        financeDao: FinanceDao
    ) {
        taskDao.getAllTasks()
            .filter { it.id.startsWith(LEGACY_OCT2026_PREFIX) }
            .forEach { taskDao.deleteTaskById(it.id) }

        // Deleting legacy demo habits cascades to their schedule periods and completions
        habitDao.getAllHabitEntities()
            .filter { it.id.startsWith(LEGACY_OCT2026_PREFIX) }
            .forEach { habitDao.deleteHabitById(it.id) }

        financeDao.getAllTransactionEntities()
            .filter { it.id.startsWith(LEGACY_OCT2026_PREFIX) }
            .forEach { financeDao.deleteTransactionById(it.id) }
    }

    fun isDemoId(id: String): Boolean {
        return id.startsWith(DEMO_ID_PREFIX) || id.startsWith(LEGACY_OCT2026_PREFIX)
    }

    /**
     * Deterministic natural-variation adherence (~68–84% overall, never 100%, with
     * realistic monthly waves and occasional missed/low days).
     */
    private fun shouldCompleteHabitOnDate(habitId: String, date: LocalDate): Boolean {
        val monthWave = when (date.monthValue) {
            6 -> 72
            7 -> 78
            8 -> 66 // realistic mid-summer dip
            9 -> 82
            10 -> 76
            else -> 80
        }
        // Slight weekend dip for realism
        val weekendAdj = if (date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY) -8 else 0
        // Occasional off-day (roughly 2 days per month)
        val dayHash = stableHash("day_mood_${date.toEpochDay()}") % 100
        if (dayHash < 6) return false

        val habitOffset = (stableHash(habitId) % 15) - 7 // -7..+7 per habit
        val threshold = (monthWave + weekendAdj + habitOffset).coerceIn(45, 88)
        val roll = stableHash("habit_${habitId}_${date.toEpochDay()}") % 100
        return roll < threshold
    }

    private fun shouldCompleteTaskOnDate(slotKey: String, date: LocalDate): Boolean {
        val monthWave = when (date.monthValue) {
            6 -> 70
            7 -> 76
            8 -> 64
            9 -> 80
            10 -> 75
            else -> 78
        }
        val roll = stableHash("task_${slotKey}_${date.toEpochDay()}") % 100
        return roll < monthWave
    }

    private fun stableHash(input: String): Int {
        var h = 1125899906842597L
        for (ch in input) {
            h = 31L * h + ch.code.toLong()
        }
        return abs((h xor (h ushr 32)).toInt())
    }
}
