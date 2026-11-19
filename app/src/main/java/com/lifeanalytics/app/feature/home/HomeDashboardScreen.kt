package com.lifeanalytics.app.feature.home

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaCategoryPerformancePanel
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaDayDrilldownBottomSheet
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaOverallPerformanceCard
import com.lifeanalytics.app.core.designsystem.LaPerformanceLineChart
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaYear365Calendar
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow
import com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeDashboardScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String,
    monthlyBudgetMinor: Long? = null,
    onNavigateToPlanning: (LocalDate) -> Unit,
    onNavigateToHabits: (LocalDate) -> Unit,
    onNavigateToFinanceDay: (LocalDate) -> Unit,
    onNavigateToDailyReview: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInsights: () -> Unit = {}
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    val greeting = remember { DateAttributionPolicy.greetingForHour(LocalTime.now().hour) }
    val (weekStart, weekEnd) = remember(today) { DateAttributionPolicy.isoWeekRange(today) }

    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val weekTasks by remember(weekStart, weekEnd) {
        repository.observeTasksInRange(weekStart, weekEnd)
    }.collectAsState(initial = emptyList())

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayHabitCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())
    val weekHabitCompletions by remember(weekStart, weekEnd) {
        repository.observeHabitCompletionsInRange(weekStart, weekEnd)
    }.collectAsState(initial = emptyList())

    val allTasks by repository.observeAllTasks().collectAsState(initial = emptyList())
    val allCompletions by repository.observeAllHabitCompletions().collectAsState(initial = emptyList())
    val allTransactions by repository.observeAllTransactions().collectAsState(initial = emptyList())

    var analyticsWindow by rememberSaveable { mutableStateOf(AnalyticsTimeWindow.MONTHLY) }
    var selectedTrendPointIdx by rememberSaveable { mutableIntStateOf(0) }
    var drilldownDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }

    val lifePerformance = remember(
        analyticsWindow, today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
    ) {
        LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = analyticsWindow,
            anchorDate = today,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = monthlyBudgetMinor,
            computeComparison = true
        )
    }

    val trendSeries = remember(
        analyticsWindow, today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
    ) {
        LifeAnalyticsEngine.buildPerformanceTrendSeries(
            window = analyticsWindow,
            anchorDate = today,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = monthlyBudgetMinor
        )
    }

    val yearCalendarModel = remember(
        today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
    ) {
        LifeAnalyticsEngine.buildYearCalendar(
            year = today.year,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = monthlyBudgetMinor
        )
    }

    val taskSummary = remember(todayTasks) {
        DomainCalculations.calculateTaskSummary(todayTasks)
    }
    val habitSummary = remember(allHabits, todayHabitCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(allHabits, todayHabitCompletions, today)
    }

    val weekDays = remember(weekStart) {
        (0L..6L).map { weekStart.plusDays(it) }
    }
    val todayIndexInWeek = remember(weekDays, today) {
        weekDays.indexOf(today).coerceAtLeast(0)
    }
    var selectedWeekIndex by rememberSaveable { mutableIntStateOf(todayIndexInWeek) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = spacing.dockBottomClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            // 1. Date / Calendar Section (Compact Header Sheet + 7-Day Week Strip)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LaTheme.radii.shapeHeaderSheetBottom)
                        .background(colors.surface)
                        .padding(
                            start = spacing.screenInset,
                            end = spacing.screenInset,
                            top = spacing.space2,
                            bottom = spacing.space4
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            LaIconChip(
                                icon = Icons.Outlined.Person,
                                backgroundColor = colors.tints.lavender.base,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = greeting,
                                    style = LaTheme.typography.sectionTitle,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = DateAttributionPolicy.formatDisplayDate(today),
                                    style = LaTheme.typography.label,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LaCircularIconButton(
                                icon = Icons.Outlined.RateReview,
                                contentDescription = "Daily Review",
                                onClick = { onNavigateToDailyReview(today) }
                            )
                            LaCircularIconButton(
                                icon = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                onClick = onNavigateToSettings
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.space3))

                    HomeWeekStrip(
                        weekDays = weekDays,
                        today = today,
                        selectedDay = weekDays.getOrElse(selectedWeekIndex) { today },
                        weekTasks = weekTasks,
                        weekHabitCompletions = weekHabitCompletions,
                        onSelectDay = { idx, date ->
                            selectedWeekIndex = idx
                            drilldownDateEpoch = date.toEpochDay()
                        }
                    )
                }
            }

            if (errorMessage != null) {
                item {
                    Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                        ErrorBanner(
                            message = errorMessage!!,
                            onDismiss = { errorMessage = null }
                        )
                    }
                }
            }

            // 2. 365-Day Performance Heatmap (Immediately after Date/Calendar section)
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    val yearSub = buildString {
                        append("${yearCalendarModel.scoredDaysCount} scored • ")
                        append("${yearCalendarModel.elapsedDaysWithoutScoreCount} no data")
                        yearCalendarModel.averageYearScore?.let { avg ->
                            append(" • Avg $avg/100")
                        }
                    }
                    LaContainerPanel(
                        title = "${yearCalendarModel.year} performance",
                        subtitle = yearSub
                    ) {
                        LaYear365Calendar(
                            calendar = yearCalendarModel,
                            selectedDate = drilldownDateEpoch?.let { LocalDate.ofEpochDay(it) },
                            onSelectDay = { cell ->
                                drilldownDateEpoch = cell.date.toEpochDay()
                            },
                            tint = colors.tints.lavender
                        )
                    }
                }
            }

            // 3. Overall Life Performance / Score
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaOverallPerformanceCard(
                        performance = lifePerformance,
                        selectedWindow = analyticsWindow,
                        onSelectWindow = {
                            analyticsWindow = it
                            selectedTrendPointIdx = 0
                        },
                        showWindowSelector = true
                    )
                }
            }

            // 4. Category-wise Performance
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaCategoryPerformancePanel(
                        performance = lifePerformance,
                        currencyCode = currencyCode
                    )
                }
            }

            // 5. Performance Trend / Graph
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    val safeTrendIdx = selectedTrendPointIdx.coerceIn(
                        0,
                        (trendSeries.size - 1).coerceAtLeast(0)
                    )
                    val selectedPt = trendSeries.getOrNull(safeTrendIdx)
                    val chartSubtitle = if (selectedPt?.overallScore != null) {
                        "${selectedPt.label} • ${selectedPt.overallScore}/100"
                    } else {
                        "${analyticsWindow.label} score trend"
                    }
                    LaContainerPanel(
                        title = "Performance trend",
                        subtitle = chartSubtitle,
                        actionLabel = "Insights",
                        onActionClick = onNavigateToInsights
                    ) {
                        LaPerformanceLineChart(
                            points = trendSeries,
                            selectedIndex = safeTrendIdx,
                            onSelectIndex = { selectedTrendPointIdx = it },
                            tint = colors.tints.lavender,
                            emptyMessage = "No scored intervals in this window yet.",
                            accessibilitySummary = "Performance trend chart for ${analyticsWindow.label} window."
                        )
                    }
                }
            }

            // 6. Daily Habits and Tasks Summary (Compact Today Execution Panel — no redundant Add buttons)
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaContainerPanel(
                        title = "Today's habits & tasks",
                        subtitle = "Habits ${habitSummary.completedDueCount}/${habitSummary.totalDueCount} • Tasks ${taskSummary.completedCount}/${taskSummary.totalCount}"
                    ) {
                        // Habits subsection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Habits (${habitSummary.completedDueCount}/${habitSummary.totalDueCount})",
                                style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Manage",
                                style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                                modifier = Modifier
                                    .pressScaleClickable { onNavigateToHabits(today) }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (habitSummary.dueHabits.isEmpty()) {
                            Text(
                                text = "No habits due today",
                                style = LaTheme.typography.label,
                                color = colors.textSecondary
                            )
                        } else {
                            habitSummary.dueHabits.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScaleClickable {
                                            scope.launch {
                                                repository.setHabitCompletionForDate(
                                                    habitId = item.habit.id,
                                                    date = today,
                                                    completed = !item.isCompleted
                                                ).onFailure { errorMessage = it.message }
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LaAnimatedCheckbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                repository.setHabitCompletionForDate(
                                                    habitId = item.habit.id,
                                                    date = today,
                                                    completed = checked
                                                ).onFailure { errorMessage = it.message }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.habit.name,
                                        style = LaTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    item.habit.lifeArea?.let { area ->
                                        Text(
                                            text = area.shortName,
                                            style = LaTheme.typography.chartMicroLabel,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                if (index < habitSummary.dueHabits.lastIndex) {
                                    LaRowDivider()
                                }
                            }
                        }

                        LaRowDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Tasks subsection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tasks (${taskSummary.completedCount}/${taskSummary.totalCount})",
                                style = LaTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Manage",
                                style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                                modifier = Modifier
                                    .pressScaleClickable { onNavigateToPlanning(today) }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (todayTasks.isEmpty()) {
                            Text(
                                text = "No tasks planned for today",
                                style = LaTheme.typography.label,
                                color = colors.textSecondary
                            )
                        } else {
                            val previewTasks = todayTasks.take(5)
                            previewTasks.forEachIndexed { index, task ->
                                HomeTaskListRow(
                                    task = task,
                                    onToggleComplete = { checked ->
                                        scope.launch {
                                            repository.setTaskCompleted(task.id, checked)
                                                .onFailure { errorMessage = it.message }
                                        }
                                    },
                                    onOpenPlanning = { onNavigateToPlanning(today) }
                                )
                                if (index < previewTasks.lastIndex) {
                                    LaRowDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.dockBottomClearance)
        )
    }

    if (drilldownDateEpoch != null) {
        val selectedDate = LocalDate.ofEpochDay(drilldownDateEpoch!!)
        val drilldown = remember(
            selectedDate, today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
        ) {
            LifeAnalyticsEngine.buildDayDrilldown(
                date = selectedDate,
                today = today,
                habits = allHabits,
                completions = allCompletions,
                tasks = allTasks,
                transactions = allTransactions,
                monthlyBudgetMinor = monthlyBudgetMinor
            )
        }
        LaDayDrilldownBottomSheet(
            drilldown = drilldown,
            currencyCode = currencyCode,
            onDismiss = { drilldownDateEpoch = null },
            onOpenPlanningForDate = onNavigateToPlanning,
            onOpenHabitsForDate = onNavigateToHabits,
            onOpenFinanceForDate = onNavigateToFinanceDay
        )
    }
}

@Composable
private fun HomeWeekStrip(
    weekDays: List<LocalDate>,
    today: LocalDate,
    selectedDay: LocalDate,
    weekTasks: List<Task>,
    weekHabitCompletions: List<com.lifeanalytics.app.domain.model.HabitCompletion>,
    onSelectDay: (Int, LocalDate) -> Unit
) {
    val colors = LaTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEachIndexed { idx, date ->
            val isToday = date == today
            val isSelected = date == selectedDay
            val hasCompletedTask = weekTasks.any { it.plannedDate == date && it.isCompleted }
            val hasCompletedHabit = weekHabitCompletions.any {
                it.completionDate == date && it.isCompleted
            }
            val hasAnyActivity = hasCompletedTask || hasCompletedHabit

            val tileBg = when {
                isToday -> colors.tints.lavender.strong
                isSelected -> colors.tints.lavender.base
                else -> colors.tints.lavender.soft
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tileBg)
                    .pressScaleClickable { onSelectDay(idx, date) }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = LaTheme.typography.chartMicroLabel.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) colors.textPrimary else colors.onTintSecondary,
                    maxLines = 1
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = LaTheme.typography.body.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
                if (hasAnyActivity) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colors.ink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Activity recorded",
                            tint = colors.onInk,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeTaskListRow(
    task: Task,
    onToggleComplete: (Boolean) -> Unit,
    onOpenPlanning: () -> Unit
) {
    val colors = LaTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onOpenPlanning)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LaAnimatedCheckbox(
            checked = task.isCompleted,
            onCheckedChange = onToggleComplete
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = task.title,
            style = LaTheme.typography.body.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (task.isCompleted) colors.textSecondary else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.lifeArea?.shortName ?: task.category.displayName,
            style = LaTheme.typography.chartMicroLabel,
            color = colors.textSecondary
        )
    }
}
