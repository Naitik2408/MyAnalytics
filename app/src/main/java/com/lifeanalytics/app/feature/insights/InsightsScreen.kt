package com.lifeanalytics.app.feature.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.MetricCard
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.designsystem.SuccessGreen
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.Task
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var periodType by rememberSaveable { mutableStateOf(InsightsPeriodType.WEEKLY) }
    var anchorDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Life Analytics & Insights") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightsPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = periodType == type,
                            onClick = { periodType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            item {
                PeriodRangeSelectorCard(
                    periodType = periodType,
                    startDate = currentStart,
                    endDate = currentEnd,
                    today = today,
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
                    onResetToToday = { anchorDateEpoch = today.toEpochDay() }
                )
            }

            // 4 Overview MetricCards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            label = "Tasks done",
                            value = currentTaskSummary.completionRatioText,
                            supportingText = formatComparisonText(taskComparison, "tasks"),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Focused time",
                            value = DateAttributionPolicy.formatDurationSeconds(currentFocusSeconds),
                            supportingText = formatDurationComparisonText(focusComparison),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val habitPctText = currentHabitSummary.consistencyPercent?.let { "$it%" } ?: "No due days"
                        val prevHabitPct = previousHabitSummary.consistencyPercent
                        val habitCompText = when {
                            currentHabitSummary.totalDueOccurrences == 0 -> "0 scheduled occurrences"
                            prevHabitPct != null && currentHabitSummary.consistencyPercent != null -> {
                                val diff = currentHabitSummary.consistencyPercent - prevHabitPct
                                val sign = if (diff >= 0) "+" else ""
                                "${currentHabitSummary.completedDueOccurrences}/${currentHabitSummary.totalDueOccurrences} ($sign${diff} pts vs prev)"
                            }
                            else -> "${currentHabitSummary.completedDueOccurrences} of ${currentHabitSummary.totalDueOccurrences} due done"
                        }
                        MetricCard(
                            label = "Habit consistency",
                            value = habitPctText,
                            supportingText = habitCompText,
                            modifier = Modifier.weight(1f)
                        )

                        val netColor = when {
                            currentFinanceSummary.netForPeriodMinor > 0L -> SuccessGreen
                            currentFinanceSummary.netForPeriodMinor < 0L -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val prevExpense = previousFinanceSummary.totalExpenseMinor
                        val currExpense = currentFinanceSummary.totalExpenseMinor
                        val financeSupport = if (prevExpense > 0L || currExpense > 0L) {
                            "Expenses: ${MoneyUtils.formatMinorUnits(currExpense, currencyCode)}"
                        } else {
                            "Income minus expenses"
                        }
                        MetricCard(
                            label = "Net for period",
                            value = MoneyUtils.formatSignedNetMinorUnits(currentFinanceSummary.netForPeriodMinor, currencyCode),
                            supportingText = financeSupport,
                            valueColor = netColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Tasks Analytics Card
            item {
                TasksAnalyticsCard(
                    periodType = periodType,
                    startDate = currentStart,
                    endDate = currentEnd,
                    tasks = currentTasks,
                    totalEstimatedMinutes = currentTaskSummary.totalEstimatedMinutes
                )
            }

            // 2. Focus Analytics Card
            item {
                FocusAnalyticsCard(
                    periodType = periodType,
                    startDate = currentStart,
                    endDate = currentEnd,
                    completedSessions = completedSessionsInPeriod,
                    totalFocusSeconds = currentFocusSeconds
                )
            }

            // 3. Habits Analytics Card
            item {
                HabitsAnalyticsCard(
                    habitPeriodSummary = currentHabitSummary
                )
            }

            // 4. Finance Analytics Card
            item {
                FinanceAnalyticsCard(
                    financeSummary = currentFinanceSummary,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

@Composable
private fun PeriodRangeSelectorCard(
    periodType: InsightsPeriodType,
    startDate: LocalDate,
    endDate: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetToToday: () -> Unit
) {
    val isCurrentPeriod = !today.isBefore(startDate) && !today.isAfter(endDate)
    val rangeTitle = when (periodType) {
        InsightsPeriodType.DAILY -> DateAttributionPolicy.formatDisplayDate(startDate)
        InsightsPeriodType.WEEKLY -> "${DateAttributionPolicy.formatShortDate(startDate)} – ${DateAttributionPolicy.formatShortDate(endDate)}"
        InsightsPeriodType.MONTHLY -> DateAttributionPolicy.formatMonthYear(YearMonth.from(startDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous period"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = rangeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isCurrentPeriod) "Current ${periodType.label.lowercase()}" else "Selected ${periodType.label.lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentPeriod) {
                    TextButton(onClick = onResetToToday) {
                        Text("Current")
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next period"
                    )
                }
            }
        }
    }
}

private data class ChartBucket(
    val label: String,
    val primaryValue: Float,
    val secondaryValue: Float = 0f
)

@Composable
private fun TasksAnalyticsCard(
    periodType: InsightsPeriodType,
    startDate: LocalDate,
    endDate: LocalDate,
    tasks: List<Task>,
    totalEstimatedMinutes: Int?
) {
    val buckets = remember(periodType, startDate, endDate, tasks) {
        buildDateBuckets(periodType, startDate, endDate).map { (label, rangeStart, rangeEnd) ->
            val inBucket = tasks.filter { !it.plannedDate.isBefore(rangeStart) && !it.plannedDate.isAfter(rangeEnd) }
            val completed = inBucket.count { it.isCompleted }.toFloat()
            val total = inBucket.size.toFloat()
            ChartBucket(label = label, primaryValue = completed, secondaryValue = total)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(title = "Task Completion")
            val completedTotal = tasks.count { it.isCompleted }
            val estimateText = totalEstimatedMinutes?.let {
                " • Planned estimate: ${DateAttributionPolicy.formatMinutes(it)}"
            } ?: ""
            Text(
                text = "$completedTotal of ${tasks.size} planned tasks completed$estimateText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks planned in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DualBarChart(
                    buckets = buckets,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    contentDescriptionText = "Task completion bar chart showing $completedTotal completed out of ${tasks.size} planned tasks."
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = "Completed")
                    LegendDot(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), label = "Planned")
                }
            }
        }
    }
}

@Composable
private fun FocusAnalyticsCard(
    periodType: InsightsPeriodType,
    startDate: LocalDate,
    endDate: LocalDate,
    completedSessions: List<com.lifeanalytics.app.domain.model.FocusSession>,
    totalFocusSeconds: Long
) {
    val buckets = remember(periodType, startDate, endDate, completedSessions) {
        buildDateBuckets(periodType, startDate, endDate).map { (label, rangeStart, rangeEnd) ->
            val seconds = completedSessions
                .filter { !it.startedLocalDate.isBefore(rangeStart) && !it.startedLocalDate.isAfter(rangeEnd) }
                .sumOf { it.focusedDurationSeconds }
            val minutes = (seconds / 60f)
            ChartBucket(label = label, primaryValue = minutes, secondaryValue = minutes)
        }
    }

    val sessionCount = completedSessions.size
    val avgSeconds = if (sessionCount > 0) totalFocusSeconds / sessionCount else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(title = "Focused Work")
            val summaryLine = if (sessionCount > 0) {
                "$sessionCount completed session(s) • Average ${DateAttributionPolicy.formatDurationSeconds(avgSeconds)}"
            } else {
                "0 completed sessions in this period"
            }
            Text(
                text = summaryLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (sessionCount == 0) {
                Text(
                    text = "No completed focus sessions recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DualBarChart(
                    buckets = buckets,
                    primaryColor = MaterialTheme.colorScheme.secondary,
                    secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                    contentDescriptionText = "Focus duration bar chart showing ${DateAttributionPolicy.formatDurationSeconds(totalFocusSeconds)} across $sessionCount sessions."
                )
                LegendDot(color = MaterialTheme.colorScheme.secondary, label = "Focused minutes (paused time excluded)")
            }
        }
    }
}

@Composable
private fun HabitsAnalyticsCard(
    habitPeriodSummary: HabitPeriodSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(title = "Habit Consistency")
            if (habitPeriodSummary.perHabitConsistency.isEmpty()) {
                Text(
                    text = "No habits created yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                habitPeriodSummary.perHabitConsistency.forEach { item ->
                    val ratio = if (item.totalDueOccurrences > 0) {
                        (item.completedDueOccurrences.toFloat() / item.totalDueOccurrences.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.habit.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val rightText = if (item.totalDueOccurrences > 0) {
                                "${item.completedDueOccurrences} / ${item.totalDueOccurrences} (${item.consistencyPercent}%)"
                            } else {
                                "No due days in period"
                            }
                            Text(
                                text = rightText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceAnalyticsCard(
    financeSummary: FinancePeriodSummary,
    currencyCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(title = "Finance Summary")
            if (!financeSummary.hasTransactions) {
                Text(
                    text = "No income or expense transactions recorded in this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxFlow = maxOf(financeSummary.totalIncomeMinor, financeSummary.totalExpenseMinor, 1L)
                val incomeRatio = (financeSummary.totalIncomeMinor.toFloat() / maxFlow.toFloat()).coerceIn(0f, 1f)
                val expenseRatio = (financeSummary.totalExpenseMinor.toFloat() / maxFlow.toFloat()).coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Income", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = MoneyUtils.formatMinorUnits(financeSummary.totalIncomeMinor, currencyCode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SuccessGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { incomeRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = SuccessGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expenses", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LinearProgressIndicator(
                        progress = { expenseRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (financeSummary.expensesByCategoryMinor.isNotEmpty() && financeSummary.totalExpenseMinor > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Top expense categories",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    financeSummary.expensesByCategoryMinor.entries.forEach { (category, amountMinor) ->
                        val pct = ((amountMinor * 100.0) / financeSummary.totalExpenseMinor).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$category ($pct%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = MoneyUtils.formatMinorUnits(amountMinor, currencyCode),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualBarChart(
    buckets: List<ChartBucket>,
    primaryColor: Color,
    secondaryColor: Color,
    contentDescriptionText: String
) {
    val maxVal = buckets.maxOfOrNull { maxOf(it.primaryValue, it.secondaryValue) }?.coerceAtLeast(1f) ?: 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDescriptionText }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val count = buckets.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.52f).coerceAtMost(36.dp.toPx())

            buckets.forEachIndexed { index, bucket ->
                val centerX = index * slotWidth + slotWidth / 2f
                val left = centerX - barWidth / 2f

                val secRatio = (bucket.secondaryValue / maxVal).coerceIn(0f, 1f)
                val secHeight = (size.height * secRatio).coerceAtLeast(if (bucket.secondaryValue > 0f) 4.dp.toPx() else 2.dp.toPx())
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(left, size.height - secHeight),
                    size = Size(barWidth, secHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                if (bucket.primaryValue > 0f) {
                    val priRatio = (bucket.primaryValue / maxVal).coerceIn(0f, 1f)
                    val priHeight = (size.height * priRatio).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(left, size.height - priHeight),
                        size = Size(barWidth, priHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                buckets.add(Triple("W$weekIdx", cursor, chunkEnd))
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
        return "0m in previous period"
    }
    val prevFormatted = DateAttributionPolicy.formatDurationSeconds(comparison.previousValue)
    if (comparison.previousValue == 0L) {
        return "Up from 0m previously"
    }
    val sign = if (comparison.delta >= 0) "+" else ""
    val pctText = comparison.percentChange?.let { "$sign$it%" } ?: ""
    return "$pctText vs $prevFormatted prev"
}
