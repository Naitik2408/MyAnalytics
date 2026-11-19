package com.lifeanalytics.app.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.LaCollapsedEmptyRow
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaDateRangePill
import com.lifeanalytics.app.core.designsystem.LaHeatDayCell
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaHorizontalTrackBar
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaMicroProgressBar
import com.lifeanalytics.app.core.designsystem.LaMiniHeatCalendar
import com.lifeanalytics.app.core.designsystem.LaPeriodEmptyCard
import com.lifeanalytics.app.core.designsystem.LaPillBarChart
import com.lifeanalytics.app.core.designsystem.LaPillBarEntry
import com.lifeanalytics.app.core.designsystem.LaRingChart
import com.lifeanalytics.app.core.designsystem.LaRingSlice
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSegmentedControl
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.designsystem.LaVerdictChip
import com.lifeanalytics.app.core.designsystem.TintSteps
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.growth.GrowthEngine
import com.lifeanalytics.app.domain.growth.GrowthInputData
import com.lifeanalytics.app.domain.growth.GrowthWindow
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.domain.usecase.FinancePeriodSummary
import com.lifeanalytics.app.domain.usecase.HabitPeriodSummary
import com.lifeanalytics.app.domain.usecase.PeriodComparison
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class InsightsPeriodType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@Composable
fun InsightsScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var periodType by rememberSaveable { mutableStateOf(InsightsPeriodType.WEEKLY) }
    var anchorDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var selectedFocusBarIdx by rememberSaveable { mutableIntStateOf(0) }
    var selectedTaskBarIdx by rememberSaveable { mutableIntStateOf(0) }

    val anchorDate = LocalDate.ofEpochDay(anchorDateEpoch)

    val (currentStart, currentEnd) = remember(periodType, anchorDate) {
        when (periodType) {
            InsightsPeriodType.DAILY -> anchorDate to anchorDate
            InsightsPeriodType.WEEKLY -> DateAttributionPolicy.isoWeekRange(anchorDate)
            InsightsPeriodType.MONTHLY -> DateAttributionPolicy.monthRange(YearMonth.from(anchorDate))
        }
    }

    val (previousStart, previousEnd) = remember(periodType, currentStart) {
        when (periodType) {
            InsightsPeriodType.DAILY -> currentStart.minusDays(1) to currentStart.minusDays(1)
            InsightsPeriodType.WEEKLY -> currentStart.minusWeeks(1) to currentStart.minusDays(1)
            InsightsPeriodType.MONTHLY -> {
                val prevMonth = YearMonth.from(currentStart).minusMonths(1)
                DateAttributionPolicy.monthRange(prevMonth)
            }
        }
    }

    // Observe current and previous ranges
    val currentTasks by remember(currentStart, currentEnd) {
        repository.observeTasksInRange(currentStart, currentEnd)
    }.collectAsState(initial = emptyList())
    val previousTasks by remember(previousStart, previousEnd) {
        repository.observeTasksInRange(previousStart, previousEnd)
    }.collectAsState(initial = emptyList())

    val currentSessions by remember(currentStart, currentEnd) {
        repository.observeSessionsInRange(currentStart, currentEnd)
    }.collectAsState(initial = emptyList())
    val previousSessions by remember(previousStart, previousEnd) {
        repository.observeSessionsInRange(previousStart, previousEnd)
    }.collectAsState(initial = emptyList())

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val currentHabitCompletions by remember(currentStart, currentEnd) {
        repository.observeHabitCompletionsInRange(currentStart, currentEnd)
    }.collectAsState(initial = emptyList())
    val previousHabitCompletions by remember(previousStart, previousEnd) {
        repository.observeHabitCompletionsInRange(previousStart, previousEnd)
    }.collectAsState(initial = emptyList())

    val currentTransactions by remember(currentStart, currentEnd) {
        repository.observeTransactionsInRange(currentStart, currentEnd)
    }.collectAsState(initial = emptyList())
    val previousTransactions by remember(previousStart, previousEnd) {
        repository.observeTransactionsInRange(previousStart, previousEnd)
    }.collectAsState(initial = emptyList())

    // Calculations
    val currentTaskSummary = remember(currentTasks) {
        DomainCalculations.calculateTaskSummary(currentTasks)
    }
    val previousTaskSummary = remember(previousTasks) {
        DomainCalculations.calculateTaskSummary(previousTasks)
    }

    val currentFocusSeconds = remember(currentSessions) {
        DomainCalculations.sumCompletedFocusDurationSeconds(currentSessions)
    }
    val previousFocusSeconds = remember(previousSessions) {
        DomainCalculations.sumCompletedFocusDurationSeconds(previousSessions)
    }
    val completedSessionsInPeriod = remember(currentSessions) {
        currentSessions.filter { it.status == FocusSessionStatus.COMPLETED }
    }

    val currentHabitSummary = remember(allHabits, currentHabitCompletions, currentStart, currentEnd, today) {
        DomainCalculations.calculateHabitPeriodConsistency(
            habits = allHabits,
            completions = currentHabitCompletions,
            startDate = currentStart,
            endDate = currentEnd,
            today = today
        )
    }
    val previousHabitSummary = remember(allHabits, previousHabitCompletions, previousStart, previousEnd, today) {
        DomainCalculations.calculateHabitPeriodConsistency(
            habits = allHabits,
            completions = previousHabitCompletions,
            startDate = previousStart,
            endDate = previousEnd,
            today = today
        )
    }

    val currentFinanceSummary = remember(currentTransactions) {
        DomainCalculations.calculateFinancePeriodSummary(currentTransactions)
    }
    val previousFinanceSummary = remember(previousTransactions) {
        DomainCalculations.calculateFinancePeriodSummary(previousTransactions)
    }

    val taskComparison = remember(currentTaskSummary, previousTaskSummary) {
        DomainCalculations.comparePeriods(
            currentTaskSummary.completedCount.toLong(),
            previousTaskSummary.completedCount.toLong()
        )
    }
    val focusComparison = remember(currentFocusSeconds, previousFocusSeconds) {
        DomainCalculations.comparePeriods(currentFocusSeconds, previousFocusSeconds)
    }

    val growthWindow = when (periodType) {
        InsightsPeriodType.DAILY -> GrowthWindow.DAY
        InsightsPeriodType.WEEKLY -> GrowthWindow.WEEK
        InsightsPeriodType.MONTHLY -> GrowthWindow.MONTH
    }

    val growthDirection = remember(
        growthWindow, today, currentTasks, previousTasks,
        currentHabitSummary, previousHabitSummary,
        currentFocusSeconds, previousFocusSeconds,
        currentFinanceSummary, previousFinanceSummary,
        currentTransactions, previousTransactions
    ) {
        val recordedDates = mutableSetOf<LocalDate>()
        // Recorded days in period
        currentHabitCompletions.forEach { if (it.isCompleted) recordedDates.add(it.completionDate) }
        previousHabitCompletions.forEach { if (it.isCompleted) recordedDates.add(it.completionDate) }
        currentTasks.forEach { task ->
            recordedDates.add(task.plannedDate)
            task.completedAtEpochMillis?.let { ms ->
                recordedDates.add(DateAttributionPolicy.localDateForEvent(ms, null))
            }
        }
        previousTasks.forEach { task ->
            recordedDates.add(task.plannedDate)
            task.completedAtEpochMillis?.let { ms ->
                recordedDates.add(DateAttributionPolicy.localDateForEvent(ms, null))
            }
        }
        currentSessions.forEach { if (it.status == FocusSessionStatus.COMPLETED) recordedDates.add(it.startedLocalDate) }
        previousSessions.forEach { if (it.status == FocusSessionStatus.COMPLETED) recordedDates.add(it.startedLocalDate) }
        currentTransactions.forEach { recordedDates.add(it.transactionDate) }
        previousTransactions.forEach { recordedDates.add(it.transactionDate) }

        val prevFinanceRecordedDays = previousTransactions.map { it.transactionDate }.toSet().size
        val isClosed = currentEnd.isBefore(today)

        val input = GrowthInputData(
            today = today,
            window = growthWindow,
            recordedDates = recordedDates,
            currentHabitsCompleted = currentHabitSummary.completedDueOccurrences,
            currentHabitsDue = currentHabitSummary.totalDueOccurrences,
            previousHabitsCompleted = previousHabitSummary.completedDueOccurrences,
            previousHabitsDue = previousHabitSummary.totalDueOccurrences,
            currentTasksCompleted = currentTaskSummary.completedCount,
            currentTasksPlanned = currentTaskSummary.totalCount,
            previousTasksCompleted = previousTaskSummary.completedCount,
            previousTasksPlanned = previousTaskSummary.totalCount,
            currentFocusDurationSeconds = currentFocusSeconds,
            previousFocusDurationSeconds = previousFocusSeconds,
            currentIncomeMinor = currentFinanceSummary.totalIncomeMinor,
            currentExpenseMinor = currentFinanceSummary.totalExpenseMinor,
            previousIncomeMinor = previousFinanceSummary.totalIncomeMinor,
            previousExpenseMinor = previousFinanceSummary.totalExpenseMinor,
            currentPeriodHasTransactions = currentTransactions.isNotEmpty(),
            previousPeriodRecordedDays = prevFinanceRecordedDays,
            isCurrentPeriodClosed = isClosed
        )
        GrowthEngine.evaluate(input)
    }

    // Build chart buckets
    val dateBuckets = remember(periodType, currentStart, currentEnd) {
        buildDateBuckets(periodType, currentStart, currentEnd)
    }

    val focusBarEntries = remember(dateBuckets, completedSessionsInPeriod) {
        dateBuckets.map { (label, bStart, bEnd) ->
            val secs = completedSessionsInPeriod
                .filter { !it.startedLocalDate.isBefore(bStart) && !it.startedLocalDate.isAfter(bEnd) }
                .sumOf { it.focusedDurationSeconds }
            LaPillBarEntry(
                label = label,
                value = secs / 60f,
                valueLabel = DateAttributionPolicy.formatDurationSeconds(secs)
            )
        }
    }

    val taskBarEntries = remember(dateBuckets, currentTasks) {
        dateBuckets.map { (label, bStart, bEnd) ->
            val inBucket = currentTasks.filter {
                !it.plannedDate.isBefore(bStart) && !it.plannedDate.isAfter(bEnd)
            }
            val done = inBucket.count { it.isCompleted }.toFloat()
            val total = inBucket.size.toFloat()
            LaPillBarEntry(
                label = label,
                value = done,
                valueLabel = "${done.toInt()}/${total.toInt()}",
                secondaryValue = total
            )
        }
    }

    // Build mini heat calendar cells for Habit / Activity consistency
    val heatCells = remember(currentStart, currentEnd, allHabits, currentHabitCompletions, today) {
        val dates = generateSequence(currentStart) { d ->
            val next = d.plusDays(1)
            if (next.isAfter(currentEnd)) null else next
        }.toList().take(28)

        dates.map { d ->
            if (d.isAfter(today)) {
                LaHeatDayCell(dayLabel = d.dayOfMonth.toString(), intensityLevel = 0)
            } else {
                val eval = DomainCalculations.evaluateHabitsForDate(
                    habits = allHabits,
                    completionsForDate = currentHabitCompletions.filter { it.completionDate == d },
                    date = d
                )
                val level = when {
                    eval.totalDueCount == 0 -> 0
                    eval.completedDueCount == 0 -> 1
                    eval.completedDueCount * 2 < eval.totalDueCount -> 2
                    eval.completedDueCount < eval.totalDueCount -> 3
                    else -> 4
                }
                LaHeatDayCell(
                    dayLabel = d.dayOfMonth.toString(),
                    intensityLevel = level
                )
            }
        }
    }

    val isPeriodCompletelyEmpty = !currentTaskSummary.hasTasks &&
        currentFocusSeconds == 0L &&
        currentHabitSummary.totalDueOccurrences == 0 &&
        !currentFinanceSummary.hasTransactions

    // Non-causal key takeaway sentence (Section 17)
    val keyTakeaway = remember(
        currentTaskSummary,
        currentFocusSeconds,
        currentHabitSummary,
        currentFinanceSummary
    ) {
        when {
            currentFocusSeconds > 0L && currentTaskSummary.completedCount > 0 ->
                "In this period, you recorded ${DateAttributionPolicy.formatDurationSeconds(currentFocusSeconds)} of focused work alongside ${currentTaskSummary.completedCount} completed task(s)."
            currentTaskSummary.hasTasks ->
                "${currentTaskSummary.completedCount} of ${currentTaskSummary.totalCount} planned tasks were completed in this period."
            currentHabitSummary.totalDueOccurrences > 0 ->
                "You completed ${currentHabitSummary.completedDueOccurrences} of ${currentHabitSummary.totalDueOccurrences} scheduled habit occurrences (${currentHabitSummary.consistencyPercent ?: 0}%)."
            currentFinanceSummary.hasTransactions ->
                "Recorded ${currentFinanceSummary.transactionCount} finance transaction(s) in this period."
            else -> null
        }
    }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing
    val fontScale = LocalDensity.current.fontScale

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(
            start = spacing.screenInset,
            end = spacing.screenInset,
            top = spacing.space2,
            bottom = spacing.dockBottomClearance
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        // 1. Compact Header: Title + Segmented Control + Pill Date-Range Control ("Today" / "This week" / "This month")
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LaScreenHeader(
                    title = "Insights",
                    subtitle = "Patterns from your recorded activity"
                )

                LaSegmentedControl(
                    options = InsightsPeriodType.entries,
                    selectedOption = periodType,
                    onOptionSelected = {
                        periodType = it
                        selectedFocusBarIdx = 0
                        selectedTaskBarIdx = 0
                    },
                    labelProvider = { it.label },
                    modifier = Modifier.fillMaxWidth()
                )

                val isCurrentPeriod = !today.isBefore(currentStart) && !today.isAfter(currentEnd)
                val rangeTitle = when (periodType) {
                    InsightsPeriodType.DAILY -> DateAttributionPolicy.formatDisplayDate(currentStart)
                    InsightsPeriodType.WEEKLY -> "${DateAttributionPolicy.formatShortDate(currentStart)} – ${DateAttributionPolicy.formatShortDate(currentEnd)}"
                    InsightsPeriodType.MONTHLY -> DateAttributionPolicy.formatMonthYear(YearMonth.from(currentStart))
                }
                val currentBadgeLabel = when (periodType) {
                    InsightsPeriodType.DAILY -> if (isCurrentPeriod) "Today" else "Selected day"
                    InsightsPeriodType.WEEKLY -> if (isCurrentPeriod) "This week" else "Selected week"
                    InsightsPeriodType.MONTHLY -> if (isCurrentPeriod) "This month" else "Selected month"
                }
                val resetButtonLabel = when (periodType) {
                    InsightsPeriodType.DAILY -> "Today"
                    InsightsPeriodType.WEEKLY -> "This week"
                    InsightsPeriodType.MONTHLY -> "This month"
                }

                LaDateRangePill(
                    title = rangeTitle,
                    badgeText = currentBadgeLabel,
                    onPrevious = {
                        anchorDateEpoch = when (periodType) {
                            InsightsPeriodType.DAILY -> anchorDate.minusDays(1).toEpochDay()
                            InsightsPeriodType.WEEKLY -> anchorDate.minusWeeks(1).toEpochDay()
                            InsightsPeriodType.MONTHLY -> anchorDate.minusMonths(1).toEpochDay()
                        }
                    },
                    onNext = {
                        anchorDateEpoch = when (periodType) {
                            InsightsPeriodType.DAILY -> anchorDate.plusDays(1).toEpochDay()
                            InsightsPeriodType.WEEKLY -> anchorDate.plusWeeks(1).toEpochDay()
                            InsightsPeriodType.MONTHLY -> anchorDate.plusMonths(1).toEpochDay()
                        }
                    },
                    onResetToCurrent = if (!isCurrentPeriod) {
                        { anchorDateEpoch = today.toEpochDay() }
                    } else null,
                    resetLabel = resetButtonLabel
                )
            }
        }

        // 2. Key Takeaway Sentence (shown only when real records exist so we never duplicate empty banners)
        if (keyTakeaway != null) {
            item {
                LaTintedTile(
                    containerColor = colors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LaIconChip(
                            icon = Icons.Outlined.BarChart,
                            onTintedSurface = false
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = keyTakeaway,
                            style = LaTheme.typography.body,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Lavender Hero Trend Panel (Focused Time Pill Bars; flat ghost pills when empty per Rule 5)
        item {
            val safeIdx = selectedFocusBarIdx.coerceIn(0, (focusBarEntries.size - 1).coerceAtLeast(0))
            val sessionCount = completedSessionsInPeriod.size
            val avgSeconds = if (sessionCount > 0) currentFocusSeconds / sessionCount else 0L
            val heroSubtitle = if (sessionCount > 0) {
                "${DateAttributionPolicy.formatDurationSeconds(currentFocusSeconds)} total • $sessionCount session(s) • Avg ${DateAttributionPolicy.formatDurationSeconds(avgSeconds)} (${formatDurationComparisonText(focusComparison)})"
            } else {
                "0m focused time (${formatDurationComparisonText(focusComparison)})"
            }

            LaHeroPanel(
                title = "Focused work trend",
                subtitle = heroSubtitle,
                icon = Icons.Outlined.Timer,
                containerColor = colors.tints.lavender.strong
            ) {
                LaPillBarChart(
                    entries = focusBarEntries,
                    selectedIndex = safeIdx,
                    onSelectIndex = { selectedFocusBarIdx = it },
                    barTint = colors.tints.lavender,
                    onHeroSurface = true,
                    chartHeight = if (sessionCount > 0) 132.dp else 96.dp,
                    emptyMessage = "", // Hero subtitle already communicates empty status (Rule 5)
                    accessibilitySummary = "Focused work trend chart: $heroSubtitle"
                )
            }
        }

        // 4. Supporting Comparison Tiles (Rules 2, 6, 8)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackColumns = maxWidth < 340.dp || fontScale >= 1.35f
                val taskRatio = if (currentTaskSummary.totalCount > 0) {
                    currentTaskSummary.completedCount.toFloat() / currentTaskSummary.totalCount.toFloat()
                } else 0f
                val habitPctText = currentHabitSummary.consistencyPercent?.let { "$it%" } ?: "—"
                val prevHabitPct = previousHabitSummary.consistencyPercent
                val habitCompText = when {
                    currentHabitSummary.totalDueOccurrences == 0 -> "0 scheduled occurrences"
                    prevHabitPct != null && currentHabitSummary.consistencyPercent != null -> {
                        val diff = currentHabitSummary.consistencyPercent - prevHabitPct
                        val sign = if (diff >= 0) "+" else ""
                        "${currentHabitSummary.completedDueOccurrences}/${currentHabitSummary.totalDueOccurrences} ($sign$diff pts vs prev)"
                    }
                    else -> "${currentHabitSummary.completedDueOccurrences} of ${currentHabitSummary.totalDueOccurrences} due done"
                }
                val netColor = when {
                    currentFinanceSummary.netForPeriodMinor > 0L -> colors.income
                    currentFinanceSummary.netForPeriodMinor < 0L -> colors.expense
                    else -> colors.textPrimary
                }

                val tasksTile: @Composable (Modifier) -> Unit = { mod ->
                    LaStatTile(
                        label = "Tasks done",
                        value = if (currentTaskSummary.hasTasks) currentTaskSummary.completionRatioText else "0",
                        icon = Icons.AutoMirrored.Outlined.EventNote,
                        supportingText = if (currentTaskSummary.hasTasks) {
                            formatComparisonText(taskComparison, "tasks")
                        } else {
                            "No tasks planned"
                        },
                        containerColor = colors.tints.lavender.base,
                        isEmptyValue = !currentTaskSummary.hasTasks,
                        minHeight = 144.dp,
                        modifier = mod,
                        trailingHeader = {
                            LaVerdictChip(verdict = growthDirection.tasksPillar.verdict)
                        },
                        microVisual = if (currentTaskSummary.hasTasks) {
                            { LaMicroProgressBar(progress = taskRatio) }
                        } else null
                    )
                }

                val habitsTile: @Composable (Modifier) -> Unit = { mod ->
                    LaStatTile(
                        label = "Habit consistency",
                        value = habitPctText,
                        icon = Icons.Outlined.Repeat,
                        supportingText = habitCompText,
                        containerColor = colors.tints.butter.base,
                        isEmptyValue = currentHabitSummary.consistencyPercent == null,
                        minHeight = 144.dp,
                        modifier = mod,
                        trailingHeader = {
                            LaVerdictChip(verdict = growthDirection.habitsPillar.verdict)
                        }
                    )
                }

                val netTile: @Composable (Modifier) -> Unit = { mod ->
                    LaStatTile(
                        label = "Net for period",
                        value = MoneyUtils.formatSignedNetMinorUnits(currentFinanceSummary.netForPeriodMinor, currencyCode),
                        icon = Icons.Outlined.AccountBalanceWallet,
                        supportingText = "Expenses: ${MoneyUtils.formatMinorUnits(currentFinanceSummary.totalExpenseMinor, currencyCode)}",
                        containerColor = colors.tints.blue.base,
                        valueColor = netColor,
                        isEmptyValue = !currentFinanceSummary.hasTransactions,
                        minHeight = 132.dp,
                        modifier = mod,
                        trailingHeader = {
                            LaVerdictChip(verdict = growthDirection.financePillar.verdict)
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                    if (stackColumns) {
                        tasksTile(Modifier.fillMaxWidth())
                        habitsTile(Modifier.fillMaxWidth())
                        netTile(Modifier.fillMaxWidth())
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                        ) {
                            tasksTile(Modifier.weight(1f))
                            habitsTile(Modifier.weight(1f))
                        }
                        netTile(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 5. Level C Period-Empty Consolidation (v1.3 Rule 4C)
        if (isPeriodCompletelyEmpty) {
            item {
                LaPeriodEmptyCard(
                    icon = Icons.Outlined.BarChart,
                    title = "No activity recorded in this period",
                    message = "Complete tasks, log focus sessions, check off habits, or record finance entries to populate your period comparisons and charts.",
                    chipColor = colors.tints.lavender.soft
                )
            }
        } else {
            // Task Completion section: full chart if tasks exist, else 1-line collapsed row (Rule 4C)
            item {
                if (!currentTaskSummary.hasTasks) {
                    LaCollapsedEmptyRow(
                        sectionTitle = "Task completion",
                        emptyText = "no tasks planned in this period",
                        icon = Icons.AutoMirrored.Outlined.EventNote
                    )
                } else {
                    val estimateLine = currentTaskSummary.totalEstimatedMinutes?.let {
                        " • Planned estimate: ${DateAttributionPolicy.formatMinutes(it)}"
                    } ?: ""
                    LaContainerPanel(
                        title = "Task completion",
                        subtitle = "${currentTaskSummary.completedCount} of ${currentTaskSummary.totalCount} planned tasks completed$estimateLine"
                    ) {
                        LaPillBarChart(
                            entries = taskBarEntries,
                            selectedIndex = selectedTaskBarIdx.coerceIn(0, (taskBarEntries.size - 1).coerceAtLeast(0)),
                            onSelectIndex = { selectedTaskBarIdx = it },
                            barTint = colors.tints.lavender,
                            chartHeight = 124.dp,
                            emptyMessage = "",
                            accessibilitySummary = "Task completion chart: ${currentTaskSummary.completedCount} of ${currentTaskSummary.totalCount} tasks completed."
                        )
                    }
                }
            }

            // Habit Consistency section: full heat calendar + track bars if due habits exist, else 1-line collapsed row
            item {
                if (currentHabitSummary.totalDueOccurrences == 0) {
                    LaCollapsedEmptyRow(
                        sectionTitle = "Habit consistency",
                        emptyText = "no data yet",
                        icon = Icons.Outlined.Repeat
                    )
                } else {
                    HabitsInsightsContainer(
                        habitPeriodSummary = currentHabitSummary,
                        heatCells = heatCells,
                        butterTint = colors.tints.butter
                    )
                }
            }

            // Finance Summary section: full chart if transactions exist, else 1-line collapsed row
            item {
                if (!currentFinanceSummary.hasTransactions) {
                    LaCollapsedEmptyRow(
                        sectionTitle = "Finance summary",
                        emptyText = "no transactions in this period",
                        icon = Icons.Outlined.AccountBalanceWallet
                    )
                } else {
                    FinanceInsightsContainer(
                        financeSummary = currentFinanceSummary,
                        currencyCode = currencyCode
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitsInsightsContainer(
    habitPeriodSummary: HabitPeriodSummary,
    heatCells: List<LaHeatDayCell>,
    butterTint: TintSteps
) {
    LaContainerPanel(
        title = "Habit consistency",
        subtitle = if (habitPeriodSummary.totalDueOccurrences > 0) {
            "${habitPeriodSummary.completedDueOccurrences} of ${habitPeriodSummary.totalDueOccurrences} scheduled occurrences completed (${habitPeriodSummary.consistencyPercent ?: 0}%)"
        } else {
            "Daily completion intensity across the period"
        }
    ) {
        LaMiniHeatCalendar(
            cells = heatCells,
            tint = LaTheme.colors.tints.lavender,
            accessibilitySummary = "Habit consistency heat calendar across ${heatCells.size} days."
        )

        if (habitPeriodSummary.perHabitConsistency.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                habitPeriodSummary.perHabitConsistency.forEachIndexed { idx, item ->
                    val ratio = if (item.totalDueOccurrences > 0) {
                        item.completedDueOccurrences.toFloat() / item.totalDueOccurrences.toFloat()
                    } else 0f
                    val valueText = if (item.totalDueOccurrences > 0) {
                        "${item.completedDueOccurrences}/${item.totalDueOccurrences} (${item.consistencyPercent}%)"
                    } else {
                        "No due days"
                    }
                    LaHorizontalTrackBar(
                        label = item.habit.name,
                        valueText = valueText,
                        progress = ratio,
                        tint = butterTint,
                        useInkFill = idx == 0
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceInsightsContainer(
    financeSummary: FinancePeriodSummary,
    currencyCode: String
) {
    val colors = LaTheme.colors
    val slices = remember(financeSummary, currencyCode) {
        if (financeSummary.totalExpenseMinor <= 0L) {
            emptyList()
        } else {
            financeSummary.expensesByCategoryMinor.entries.take(5).map { (cat, minor) ->
                val pct = ((minor * 100.0) / financeSummary.totalExpenseMinor).toInt().coerceAtLeast(1)
                LaRingSlice(
                    label = cat,
                    amountValue = minor.toFloat(),
                    formattedValue = MoneyUtils.formatMinorUnits(minor, currencyCode),
                    percentage = pct
                )
            }
        }
    }

    LaContainerPanel(
        title = "Finance summary",
        subtitle = if (financeSummary.hasTransactions) {
            "${financeSummary.transactionCount} transaction(s) in this period"
        } else {
            "Income, expenses, and category share"
        }
    ) {
        val maxFlow = maxOf(financeSummary.totalIncomeMinor, financeSummary.totalExpenseMinor, 1L)
        val incomeRatio = if (financeSummary.hasTransactions) {
            financeSummary.totalIncomeMinor.toFloat() / maxFlow.toFloat()
        } else 0f
        val expenseRatio = if (financeSummary.hasTransactions) {
            financeSummary.totalExpenseMinor.toFloat() / maxFlow.toFloat()
        } else 0f

        LaHorizontalTrackBar(
            label = "Income (+)",
            valueText = MoneyUtils.formatMinorUnits(financeSummary.totalIncomeMinor, currencyCode),
            progress = incomeRatio,
            tint = colors.tints.mint
        )
        Spacer(modifier = Modifier.height(10.dp))
        LaHorizontalTrackBar(
            label = "Expenses (-)",
            valueText = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
            progress = expenseRatio,
            tint = colors.tints.rose
        )
        Spacer(modifier = Modifier.height(16.dp))
        LaRingChart(
            slices = slices,
            emptyMessage = "No expense categories recorded in this period.",
            accessibilitySummary = "Finance expense breakdown ring chart."
        )
    }
}

private fun buildDateBuckets(
    periodType: InsightsPeriodType,
    startDate: LocalDate,
    endDate: LocalDate
): List<Triple<String, LocalDate, LocalDate>> {
    return when (periodType) {
        InsightsPeriodType.DAILY -> {
            listOf(Triple(startDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), startDate, endDate))
        }
        InsightsPeriodType.WEEKLY -> {
            (0L..6L).map { offset ->
                val d = startDate.plusDays(offset)
                Triple(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), d, d)
            }
        }
        InsightsPeriodType.MONTHLY -> {
            val buckets = mutableListOf<Triple<String, LocalDate, LocalDate>>()
            var cursor = startDate
            var weekIdx = 1
            while (!cursor.isAfter(endDate)) {
                val chunkEnd = cursor.plusDays(6).let { if (it.isAfter(endDate)) endDate else it }
                buckets.add(Triple("Wk $weekIdx", cursor, chunkEnd))
                cursor = chunkEnd.plusDays(1)
                weekIdx++
            }
            buckets
        }
    }
}

private fun formatComparisonText(comparison: PeriodComparison, unitLabel: String): String {
    if (comparison.previousValue == 0L && comparison.currentValue == 0L) {
        return "Same as previous period (0)"
    }
    if (comparison.previousValue == 0L) {
        return "+${comparison.currentValue} $unitLabel vs 0 previously"
    }
    val sign = if (comparison.delta >= 0) "+" else ""
    val pctText = comparison.percentChange?.let { " ($sign$it%)" } ?: ""
    return "$sign${comparison.delta} vs prev period$pctText"
}

private fun formatDurationComparisonText(comparison: PeriodComparison): String {
    if (comparison.previousValue == 0L && comparison.currentValue == 0L) {
        return "0m previously"
    }
    val prevFormatted = DateAttributionPolicy.formatDurationSeconds(comparison.previousValue)
    if (comparison.previousValue == 0L) {
        return "Up from 0m previously"
    }
    val sign = if (comparison.delta >= 0) "+" else ""
    val pctText = comparison.percentChange?.let { "$sign$it%" } ?: ""
    return "$pctText vs $prevFormatted prev"
}
