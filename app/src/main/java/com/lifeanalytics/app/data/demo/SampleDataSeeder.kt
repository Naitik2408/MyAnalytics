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
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import com.lifeanalytics.app.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Deterministic, isolated 1-month sample data seeder (`October 1, 2026 – October 31, 2026`
 * or the previous complete calendar month relative to `today`).
 *
 * Isolation & Safety Guarantees:
 * 1. Every record ID is deterministically prefixed with `demo_oct2026_` so it can never
 *    collide with or overwrite real user UUID records.
 * 2. Every habit/task title or transaction note includes `[Demo]` so demo records are
 *    unmistakably identifiable in the UI.
 * 3. Idempotent: Running [seedSampleMonth] multiple times replaces only the deterministic
 *    `demo_oct2026_*` IDs without creating duplicates or touching personal data.
 * 4. Reversible: [removeSampleMonth] deletes only records with the `demo_oct2026_` prefix,
 *    leaving all personal user records untouched.
 * 5. All analytics and the 365-day heatmap are computed through the real `LifeAnalyticsEngine`
 *    over these seeded SQLite records — zero hardcoded scores or UI overrides.
 */
object SampleDataSeeder {

    const val DEMO_ID_PREFIX = "demo_oct2026_"
    const val DEMO_TAG = "[Demo]"

    // Recommended monthly budget target (₹45,000.00 = 4,500,000 paise) for demo finance scoring
    const val DEMO_MONTHLY_BUDGET_MINOR = 4_500_000L

    data class SeedSummary(
        val monthLabel: String,
        val habitsSeeded: Int,
        val habitCompletionsSeeded: Int,
        val tasksSeeded: Int,
        val transactionsSeeded: Int
    )

    data class DemoHabitSpec(
        val id: String,
        val name: String,
        val lifeArea: LifeArea,
        val scheduleType: String,
        val weekdaysCsv: String,
        val dueWeekdays: Set<DayOfWeek>
    )

    val demoHabits: List<DemoHabitSpec> = listOf(
        DemoHabitSpec(
            id = "${DEMO_ID_PREFIX}habit_water",
            name = "Drink 2 litres of water $DEMO_TAG",
            lifeArea = LifeArea.HEALTH_LIFESTYLE,
            scheduleType = "EVERY_DAY",
            weekdaysCsv = "1,2,3,4,5,6,7",
            dueWeekdays = DayOfWeek.entries.toSet()
        ),
        DemoHabitSpec(
            id = "${DEMO_ID_PREFIX}habit_meditation",
            name = "Meditation (15 min) $DEMO_TAG",
            lifeArea = LifeArea.HEALTH_LIFESTYLE,
            scheduleType = "EVERY_DAY",
            weekdaysCsv = "1,2,3,4,5,6,7",
            dueWeekdays = DayOfWeek.entries.toSet()
        ),
        DemoHabitSpec(
            id = "${DEMO_ID_PREFIX}habit_exercise",
            name = "Physical activity & mobility $DEMO_TAG",
            lifeArea = LifeArea.HEALTH_LIFESTYLE,
            scheduleType = "CUSTOM",
            weekdaysCsv = "1,2,3,4,5,6",
            dueWeekdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
            )
        ),
        DemoHabitSpec(
            id = "${DEMO_ID_PREFIX}habit_reading",
            name = "Reading for learning (30 min) $DEMO_TAG",
            lifeArea = LifeArea.CAREER_PRODUCTIVITY,
            scheduleType = "EVERY_DAY",
            weekdaysCsv = "1,2,3,4,5,6,7",
            dueWeekdays = DayOfWeek.entries.toSet()
        ),
        DemoHabitSpec(
            id = "${DEMO_ID_PREFIX}habit_english",
            name = "English speaking practice $DEMO_TAG",
            lifeArea = LifeArea.CAREER_PRODUCTIVITY,
            scheduleType = "WEEKDAYS",
            weekdaysCsv = "1,2,3,4,5",
            dueWeekdays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        )
    )

    /**
     * Resolves the target sample month. For `2026-11-19`, returns `October 2026`
     * (`2026-10-01` .. `2026-10-31`).
     */
    fun targetSampleMonth(today: LocalDate = LocalDate.of(2026, 11, 19)): YearMonth {
        return YearMonth.from(today).minusMonths(1)
    }

    /**
     * Builds the deterministic 1-month sample entities without touching any DAO.
     * Exposed as a pure function so unit tests can verify analytics and heatmap scores directly.
     */
    fun buildSampleMonthEntities(
        yearMonth: YearMonth = YearMonth.of(2026, 10),
        currencyCode: String = "INR"
    ): SampleEntityBundle {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val zone = ZoneId.systemDefault()
        val createdBaseMillis = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()

        // 1. Habits & Schedule Periods (active ONLY during October 1..31, 2026 so they do not pollute other months)
        val deactivatedDateIso = DateAttributionPolicy.formatIsoDate(monthEnd.plusDays(1))
        val deactivatedMillis = monthEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val habitEntities = demoHabits.mapIndexed { idx, spec ->
            HabitEntity(
                id = spec.id,
                name = spec.name,
                isActive = false, // Deactivated on Nov 1 so October is a clean self-contained historical month
                createdAtEpochMillis = createdBaseMillis + idx * 1000L,
                updatedAtEpochMillis = createdBaseMillis + idx * 1000L,
                deactivatedAtEpochMillis = deactivatedMillis,
                deactivatedFromLocalDate = deactivatedDateIso,
                lifeArea = spec.lifeArea.name
            )
        }

        val scheduleEntities = demoHabits.mapIndexed { idx, spec ->
            HabitSchedulePeriodEntity(
                id = "${DEMO_ID_PREFIX}sched_${idx + 1}",
                habitId = spec.id,
                effectiveFrom = DateAttributionPolicy.formatIsoDate(monthStart),
                effectiveTo = deactivatedDateIso,
                scheduleType = spec.scheduleType,
                weekdaysCsv = spec.weekdaysCsv,
                createdAtEpochMillis = createdBaseMillis + idx * 1000L
            )
        }

        // Day-by-day profile across the 31 days of October 2026:
        // - Unrecorded rest/offline days (ELAPSED_NO_DATA): Oct 11 and Oct 25 -> 0 tasks, 0 habit completions, 0 transactions
        // - Low-performance days (~20-35%): Oct 5, Oct 14, Oct 19, Oct 28
        // - Average-performance days (~45-68%): Oct 2, 4, 7, 9, 13, 16, 18, 21, 23, 27, 30
        // - High-performance days (~75-100%): Oct 1, 3, 6, 8, 10, 12, 15, 17, 20, 22, 24, 26, 29, 31
        val noDataDays = setOf(11, 25)
        val lowDays = setOf(5, 14, 19, 28)
        val averageDays = setOf(2, 4, 7, 9, 13, 16, 18, 21, 23, 27, 30)

        val completions = mutableListOf<HabitCompletionEntity>()
        val tasks = mutableListOf<TaskEntity>()
        val transactions = mutableListOf<FinanceTransactionEntity>()

        val careerTaskCatalog = listOf(
            "Draft Q4 architecture review notes" to TaskCategory.WORK,
            "Complete Kotlin coroutines & Flow practice module" to TaskCategory.STUDY,
            "Prepare sprint demo walkthrough slides" to TaskCategory.WORK,
            "Refactor Room DAO queries and unit tests" to TaskCategory.WORK,
            "Read chapter on distributed systems consensus" to TaskCategory.STUDY,
            "Review pull requests and document API contract" to TaskCategory.WORK,
            "Practice 20-minute technical presentation in English" to TaskCategory.STUDY,
            "Write post-mortem summary for staging deployment" to TaskCategory.WORK,
            "Solve 2 algorithm design problems" to TaskCategory.STUDY,
            "Update engineering roadmap milestones" to TaskCategory.WORK
        )

        val healthTaskCatalog = listOf(
            "Morning 5km outdoor run & stretching" to TaskCategory.PERSONAL,
            "Prepare high-protein meal prep for 3 days" to TaskCategory.PERSONAL,
            "Evening yoga and posture mobility routine" to TaskCategory.PERSONAL,
            "Schedule annual dental & blood panel checkup" to TaskCategory.PERSONAL
        )

        val financeTaskCatalog = listOf(
            "Reconcile monthly expense ledger & categories" to TaskCategory.PERSONAL,
            "Review SIP mutual fund allocation for Q4" to TaskCategory.PERSONAL,
            "Pay utility and broadband bills on time" to TaskCategory.PERSONAL
        )

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateIso = DateAttributionPolicy.formatIsoDate(date)
            val dayStartMillis = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

            if (day in noDataDays) {
                // Intentionally leave 0 habit completions, 0 planned tasks, and 0 finance transactions
                // so the 365-day heatmap demonstrates the distinct ELAPSED_NO_DATA visual state
                // only if no habits are due? Wait: if habits are scheduled on Oct 11/25, a scheduled habit
                // with 0 completions gives 0% score unless no habits are scheduled on that weekday!
                // Wait: on Sundays (Oct 4, 11, 18, 25), let's check:
                // If we want Oct 11 and Oct 25 (both Sundays!) to be true ELAPSED_NO_DATA days
                // (no scheduled habits, no tasks, no transactions), we simply schedule the demo habits
                // on Monday–Saturday (`1,2,3,4,5,6`)! Then Sundays have 0 scheduled habits unless tasks
                // or transactions are logged on that Sunday (like Oct 4 and Oct 18 which will have tasks/finance,
                // while Oct 11 and Oct 25 have no tasks and no transactions -> genuine ELAPSED_NO_DATA!).
                continue
            }

            val dueHabitsForDay = demoHabits.filter { date.dayOfWeek in it.dueWeekdays }

            // Habit completions based on day profile
            dueHabitsForDay.forEachIndexed { hIdx, spec ->
                val completed = when {
                    day in lowDays -> hIdx == 0 // Only 1 of due habits completed (~20-25%)
                    day in averageDays -> (hIdx + day) % 2 == 0 // ~50-60% completed
                    else -> !(day % 3 == 0 && hIdx == dueHabitsForDay.lastIndex) // 80-100% completed
                }
                if (completed) {
                    completions.add(
                        HabitCompletionEntity(
                            id = "${DEMO_ID_PREFIX}comp_${day}_${hIdx + 1}",
                            habitId = spec.id,
                            completionDate = dateIso,
                            isCompleted = true,
                            completedAtEpochMillis = dayStartMillis + (hIdx + 1) * 1800_000L,
                            updatedAtEpochMillis = dayStartMillis + (hIdx + 1) * 1800_000L
                        )
                    )
                }
            }

            // Daily tasks (2 to 3 tasks per active day with realistic variation)
            val primaryCareer = careerTaskCatalog[(day * 3) % careerTaskCatalog.size]
            val secondaryCareer = careerTaskCatalog[(day * 3 + 4) % careerTaskCatalog.size]

            val taskSpecsForDay = mutableListOf(
                Triple(primaryCareer.first, primaryCareer.second, LifeArea.CAREER_PRODUCTIVITY),
                Triple(secondaryCareer.first, secondaryCareer.second, LifeArea.CAREER_PRODUCTIVITY)
            )
            if (day % 3 == 0) {
                val hTask = healthTaskCatalog[(day / 3) % healthTaskCatalog.size]
                taskSpecsForDay.add(Triple(hTask.first, hTask.second, LifeArea.HEALTH_LIFESTYLE))
            } else if (day % 5 == 0) {
                val fTask = financeTaskCatalog[(day / 5) % financeTaskCatalog.size]
                taskSpecsForDay.add(Triple(fTask.first, fTask.second, LifeArea.FINANCE_WEALTH))
            }

            taskSpecsForDay.forEachIndexed { tIdx, (rawTitle, category, area) ->
                val isDone = when {
                    day in lowDays -> false // 0% tasks done on low days
                    day in averageDays -> tIdx == 0 // 1 of 2-3 tasks done (~33-50%)
                    else -> !(day % 4 == 0 && tIdx == taskSpecsForDay.lastIndex) // 67-100% done
                }
                val status = if (isDone) TaskStatus.COMPLETED.name else TaskStatus.PLANNED.name
                val completedAt = if (isDone) dayStartMillis + (tIdx + 2) * 3600_000L else null
                tasks.add(
                    TaskEntity(
                        id = "${DEMO_ID_PREFIX}task_${day}_${tIdx + 1}",
                        title = "$rawTitle $DEMO_TAG",
                        category = category.name,
                        plannedDate = dateIso,
                        plannedTime = String.format("%02d:00", 9 + tIdx * 2),
                        estimatedDurationMinutes = 30 + (tIdx * 15),
                        status = status,
                        completedAtEpochMillis = completedAt,
                        sortOrder = tIdx,
                        createdAtEpochMillis = dayStartMillis,
                        updatedAtEpochMillis = completedAt ?: dayStartMillis,
                        lifeArea = area.name
                    )
                )
            }

            // Finance transactions (realistic monthly income + varied daily expenses)
            if (day == 1) {
                transactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_income_salary",
                        type = TransactionType.INCOME.name,
                        amountMinor = 8_500_000L, // ₹85,000.00
                        currencyCode = currencyCode,
                        category = "Salary",
                        transactionDate = dateIso,
                        note = "Monthly salary credit $DEMO_TAG",
                        createdAtEpochMillis = dayStartMillis,
                        updatedAtEpochMillis = dayStartMillis
                    )
                )
            } else if (day == 15) {
                transactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_income_freelance",
                        type = TransactionType.INCOME.name,
                        amountMinor = 1_500_000L, // ₹15,000.00
                        currencyCode = currencyCode,
                        category = "Freelance",
                        transactionDate = dateIso,
                        note = "Technical consulting payout $DEMO_TAG",
                        createdAtEpochMillis = dayStartMillis,
                        updatedAtEpochMillis = dayStartMillis
                    )
                )
            }

            // Add expense entries on ~18 days of the month (including some over-daily-budget days on lowDays)
            if (day % 2 == 1 || day in lowDays) {
                val (amountMinor, cat, noteText) = when {
                    day == 5 -> Triple(320_000L, "Shopping", "Electronics & desk accessories")
                    day == 14 -> Triple(280_000L, "Bills", "Home internet & utility bill")
                    day == 19 -> Triple(260_000L, "Entertainment", "Weekend dinner & movie")
                    day == 28 -> Triple(295_000L, "Health", "Pharmacy & supplements")
                    day % 5 == 0 -> Triple(125_000L, "Food", "Groceries & fresh produce")
                    day % 3 == 0 -> Triple(85_000L, "Transport", "Metro pass & cab commute")
                    else -> Triple(110_000L, "Food", "Lunch & coffee")
                }
                transactions.add(
                    FinanceTransactionEntity(
                        id = "${DEMO_ID_PREFIX}tx_exp_$day",
                        type = TransactionType.EXPENSE.name,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        category = cat,
                        transactionDate = dateIso,
                        note = "$noteText $DEMO_TAG",
                        createdAtEpochMillis = dayStartMillis + 7200_000L,
                        updatedAtEpochMillis = dayStartMillis + 7200_000L
                    )
                )
            }
        }

        return SampleEntityBundle(
            yearMonth = yearMonth,
            habits = habitEntities,
            schedulePeriods = scheduleEntities,
            completions = completions,
            tasks = tasks,
            transactions = transactions
        )
    }

    data class SampleEntityBundle(
        val yearMonth: YearMonth,
        val habits: List<HabitEntity>,
        val schedulePeriods: List<HabitSchedulePeriodEntity>,
        val completions: List<HabitCompletionEntity>,
        val tasks: List<TaskEntity>,
        val transactions: List<FinanceTransactionEntity>
    )

    suspend fun seedSampleMonth(
        taskDao: TaskDao,
        habitDao: HabitDao,
        financeDao: FinanceDao,
        yearMonth: YearMonth = YearMonth.of(2026, 10),
        currencyCode: String = "INR"
    ): SeedSummary {
        // Adjust demo habits so all 5 are Mon-Sat (weekdays 1..6) ensuring Oct 11 and Oct 25 (Sundays)
        // have 0 scheduled habits and 0 tasks/transactions -> genuine ELAPSED_NO_DATA days in the heatmap!
        val bundle = buildSampleMonthEntitiesWithSundayRest(yearMonth, currencyCode)

        // Idempotent upsert via OnConflictStrategy.REPLACE on deterministic demo_oct2026_* primary keys
        taskDao.upsertAllTasks(bundle.tasks)
        habitDao.upsertAllHabits(bundle.habits)
        habitDao.upsertAllSchedulePeriods(bundle.schedulePeriods)
        habitDao.upsertAllCompletions(bundle.completions)
        financeDao.upsertAllTransactions(bundle.transactions)

        return SeedSummary(
            monthLabel = DateAttributionPolicy.formatMonthYear(yearMonth),
            habitsSeeded = bundle.habits.size,
            habitCompletionsSeeded = bundle.completions.size,
            tasksSeeded = bundle.tasks.size,
            transactionsSeeded = bundle.transactions.size
        )
    }

    /**
     * Ensures Sundays without tasks/transactions (Oct 11 & Oct 25) have 0 due habits so the
     * 365-day heatmap displays both scored elapsed days (intensity 1..4) AND elapsed no-data days.
     */
    fun buildSampleMonthEntitiesWithSundayRest(
        yearMonth: YearMonth = YearMonth.of(2026, 10),
        currencyCode: String = "INR"
    ): SampleEntityBundle {
        val monToSat = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
        val base = buildSampleMonthEntities(yearMonth, currencyCode)
        val adjustedSchedules = base.schedulePeriods.map { sp ->
            if (sp.weekdaysCsv == "1,2,3,4,5,6,7") {
                sp.copy(scheduleType = "CUSTOM", weekdaysCsv = "1,2,3,4,5,6")
            } else {
                sp
            }
        }
        val adjustedCompletions = base.completions.filter { comp ->
            val date = DateAttributionPolicy.parseIsoDate(comp.completionDate)
            date.dayOfWeek in monToSat
        }
        return base.copy(
            schedulePeriods = adjustedSchedules,
            completions = adjustedCompletions
        )
    }

    suspend fun removeSampleMonth(
        taskDao: TaskDao,
        habitDao: HabitDao,
        financeDao: FinanceDao
    ) {
        val tasks = taskDao.getAllTasks().filter { it.id.startsWith(DEMO_ID_PREFIX) }
        tasks.forEach { taskDao.deleteTaskById(it.id) }

        val demoHabitsList = habitDao.getAllHabitEntities().filter { it.id.startsWith(DEMO_ID_PREFIX) }
        // Deleting a habit by ID cascades to habit_schedule_periods and habit_completions via ForeignKey.CASCADE
        demoHabitsList.forEach { habitDao.deleteHabitById(it.id) }

        val demoTxs = financeDao.getAllTransactionEntities().filter { it.id.startsWith(DEMO_ID_PREFIX) }
        demoTxs.forEach { financeDao.deleteTransactionById(it.id) }
    }
}
