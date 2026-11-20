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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaCategoryPerformancePanel
import com.lifeanalytics.app.core.designsystem.LaActionPillGroup
import com.lifeanalytics.app.core.designsystem.LaActionSpec
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaDayDrilldownBottomSheet
import com.lifeanalytics.app.core.designsystem.LaIconChip
import com.lifeanalytics.app.core.designsystem.LaOverallPerformanceCard
import com.lifeanalytics.app.core.designsystem.LaPerformanceLineChart
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaYear365Calendar
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow
import com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.feature.finance.TransactionFormDialog
import com.lifeanalytics.app.feature.habits.HabitFormDialog
import com.lifeanalytics.app.feature.planning.TaskFormDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
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
    onNavigateToProfile: () -> Unit,
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
    var analyticsAnchorEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var userAdjustedAnchor by rememberSaveable { mutableStateOf(false) }
    var selectedTrendPointIdx by rememberSaveable { mutableIntStateOf(0) }
    var drilldownDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }

    // If the current month has no activity yet (e.g. on the 1st), anchor the Monthly analytics
    // card to the most recent month that does have recorded activity, so Home never shows an
    // empty trend while real history exists. Purely derived from source records.
    LaunchedEffect(allTasks.size, allCompletions.size, allTransactions.size, userAdjustedAnchor) {
        if (!userAdjustedAnchor && analyticsWindow == AnalyticsTimeWindow.MONTHLY) {
            val activityDates = sequenceOf(
                allTasks.asSequence().map { it.plannedDate },
                allCompletions.asSequence().filter { it.isCompleted }.map { it.completionDate },
                allTransactions.asSequence().map { it.transactionDate }
            ).flatten().filter { !it.isAfter(today) }
            val latestActivity = activityDates.maxOrNull()
            analyticsAnchorEpoch = if (latestActivity == null || YearMonth.from(latestActivity) == YearMonth.from(today)) {
                today.toEpochDay()
            } else {
                YearMonth.from(latestActivity).atEndOfMonth().toEpochDay()
            }
        }
    }

    val analyticsAnchorDate = LocalDate.ofEpochDay(analyticsAnchorEpoch)

    val lifePerformance = remember(
        analyticsWindow, analyticsAnchorDate, today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
    ) {
        LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = analyticsWindow,
            anchorDate = analyticsAnchorDate,
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
        analyticsWindow, analyticsAnchorDate, today, allHabits, allCompletions, allTasks, allTransactions, monthlyBudgetMinor
    ) {
        LifeAnalyticsEngine.buildPerformanceTrendSeries(
            window = analyticsWindow,
            anchorDate = analyticsAnchorDate,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = monthlyBudgetMinor
        )
    }

    // Default selected trend point to the latest scored bucket
    LaunchedEffect(trendSeries) {
        val lastScoredIdx = trendSeries.indexOfLast { it.overallScore != null }
        if (lastScoredIdx >= 0) {
            selectedTrendPointIdx = lastScoredIdx
        }
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
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showAddExpenseDialog by rememberSaveable { mutableStateOf(false) }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .pressScaleClickable(onClick = onNavigateToProfile)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Open profile and account"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LaIconChip(
                            icon = Icons.Outlined.Person,
                            backgroundColor = colors.tints.lavender.base,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
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

            // 2. 365-Day Performance Heatmap with Days Passed / Days Remaining Header
            item {
                val yearProgress = remember(today) { DateAttributionPolicy.yearProgress(today) }
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LaTheme.radii.shapeLg)
                            .background(colors.surface)
                            .padding(spacing.space5)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = yearProgress.daysPassed.toString(),
                                    style = LaTheme.typography.metricValue.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Days Passed",
                                    style = LaTheme.typography.label,
                                    color = colors.textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(38.dp)
                                    .background(colors.outline.copy(alpha = 0.7f))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = yearProgress.daysRemaining.toString(),
                                    style = LaTheme.typography.metricValue.copy(fontWeight = FontWeight.Bold),
                                    color = colors.accent
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Days Remaining",
                                    style = LaTheme.typography.label,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        LaRowDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        LaYear365Calendar(
                            calendar = yearCalendarModel,
                            selectedDate = drilldownDateEpoch?.let { LocalDate.ofEpochDay(it) },
                            onSelectDay = { cell ->
                                drilldownDateEpoch = cell.date.toEpochDay()
                            },
                            tint = colors.tints.lavender,
                            showSummaryHeader = false
                        )
                    }
                }
            }

            // 3. Quick Actions (reference layout): primary full-width "Add task", then Add habit + Add expense
            item {
                LaActionPillGroup(
                    primary = LaActionSpec("Add task", { showAddTaskDialog = true }),
                    secondary = listOf(
                        LaActionSpec("Add habit", { showAddHabitDialog = true }),
                        LaActionSpec("Add expense", { showAddExpenseDialog = true })
                    ),
                    modifier = Modifier.padding(horizontal = spacing.screenInset)
                )
            }

            // 4. Single Streamlined Performance Trend Card (Visual curve, no cluttered number tables)
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
                        "Performance trajectory"
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
                            emptyMessage = "No scored intervals yet.",
                            accessibilitySummary = "Performance trend chart."
                        )
                    }
                }
            }

            // 5. Today's Tasks (Matching Image 5 layout with View all & interactive items)
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaContainerPanel(
                        title = "Today's tasks",
                        actionLabel = "View all",
                        onActionClick = { onNavigateToPlanning(today) }
                    ) {
                        if (todayTasks.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.tints.lavender.base),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No tasks planned for today",
                                        style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Add your key tasks for today to keep your plan clear.",
                                        style = LaTheme.typography.label,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.tints.lavender.base)
                                    .pressScaleClickable { showAddTaskDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add task",
                                    style = LaTheme.typography.buttonLabel.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accent
                                )
                            }
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

            // 6. Today's Habits (Matching Image 5 layout with checkable items and Due today badges)
            item {
                Box(modifier = Modifier.padding(horizontal = spacing.screenInset)) {
                    LaContainerPanel(
                        title = "Today's habits",
                        subtitle = "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} completed",
                        actionLabel = "View all",
                        onActionClick = { onNavigateToHabits(today) }
                    ) {
                        if (habitSummary.dueHabits.isEmpty()) {
                            Text(
                                text = "No habits due today",
                                style = LaTheme.typography.label,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
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
                                        .padding(vertical = 8.dp),
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
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.habit.name,
                                        style = LaTheme.typography.body.copy(
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (item.isCompleted) colors.textSecondary else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.tints.lavender.base)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Due today",
                                            style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.accent
                                        )
                                    }
                                }
                                if (index < habitSummary.dueHabits.lastIndex) {
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

    if (showAddTaskDialog) {
        TaskFormDialog(
            initialTask = null,
            defaultDate = today,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, category, plannedDate, plannedTime, estimateMinutes, lifeArea ->
                repository.createTask(
                    title = title,
                    category = category,
                    plannedDate = plannedDate,
                    plannedTime = plannedTime,
                    estimatedDurationMinutes = estimateMinutes,
                    lifeArea = lifeArea
                ).map {
                    showAddTaskDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Task saved") }
                }
            }
        )
    }

    if (showAddHabitDialog) {
        HabitFormDialog(
            initialHabit = null,
            effectiveDate = today,
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, weekdays, lifeArea ->
                scope.launch {
                    repository.createHabit(
                        name = name,
                        weekdays = weekdays,
                        effectiveFrom = today,
                        lifeArea = lifeArea
                    ).onSuccess {
                        showAddHabitDialog = false
                        snackbarHostState.showSnackbar("Habit saved")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Unable to save habit."
                    }
                }
            },
            onDeactivate = null,
            onRequestDelete = null
        )
    }

    if (showAddExpenseDialog) {
        TransactionFormDialog(
            initialTransaction = null,
            defaultType = TransactionType.EXPENSE,
            defaultDate = today,
            currencyCode = currencyCode,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { type, amountMinor, category, date, note ->
                scope.launch {
                    repository.saveTransaction(
                        transactionId = null,
                        type = type,
                        amountMinor = amountMinor,
                        currencyCode = currencyCode,
                        category = category,
                        transactionDate = date,
                        note = note
                    ).onSuccess {
                        showAddExpenseDialog = false
                        snackbarHostState.showSnackbar("Expense saved")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Unable to save expense."
                    }
                }
            }
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
