package com.lifeanalytics.app.feature.career

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaActionPillGroup
import com.lifeanalytics.app.core.designsystem.LaActionSpec
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaCategoryPeriodSelector
import com.lifeanalytics.app.core.designsystem.LaCompletionCountTile
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaHorizontalTrackBar
import com.lifeanalytics.app.core.designsystem.LaPerformanceLineChart
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.analytics.AnalyticsTimeWindow
import com.lifeanalytics.app.domain.analytics.LifeAnalyticsEngine
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.feature.habits.HabitFormDialog
import com.lifeanalytics.app.feature.planning.TaskFormDialog
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun CareerScreen(
    repository: LifeAnalyticsRepository,
    onNavigateToAllTasks: (() -> Unit)? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var periodWindow by rememberSaveable { mutableStateOf(AnalyticsTimeWindow.MONTHLY) }
    var anchorDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var selectedTrendIdx by rememberSaveable { mutableIntStateOf(0) }
    val anchorDate = LocalDate.ofEpochDay(anchorDateEpoch)

    val (periodStart, periodEnd) = remember(periodWindow, anchorDate) {
        when (periodWindow) {
            AnalyticsTimeWindow.WEEKLY -> DateAttributionPolicy.isoWeekRange(anchorDate)
            else -> DateAttributionPolicy.monthRange(YearMonth.from(anchorDate))
        }
    }

    val allTasks by repository.observeAllTasks().collectAsState(initial = emptyList())
    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())

    val careerTasksToday = remember(todayTasks) {
        todayTasks.filter { it.lifeArea == LifeArea.CAREER_PRODUCTIVITY }
    }

    val careerHabits = remember(allHabits) {
        allHabits.filter { it.lifeArea == LifeArea.CAREER_PRODUCTIVITY }
    }

    val todayHabitSummary = remember(careerHabits, todayCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(careerHabits, todayCompletions, today)
    }

    val allCompletions by repository.observeAllHabitCompletions().collectAsState(initial = emptyList())
    val allTransactions by repository.observeAllTransactions().collectAsState(initial = emptyList())

    val careerPerformance = remember(
        periodWindow, anchorDate, today, allHabits, allCompletions, allTasks, allTransactions
    ) {
        LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = periodWindow,
            anchorDate = anchorDate,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = null,
            computeComparison = false
        ).career
    }

    val careerTrendSeries = remember(
        periodWindow, anchorDate, today, allHabits, allCompletions, allTasks
    ) {
        LifeAnalyticsEngine.buildCategoryTrendSeries(
            lifeArea = LifeArea.CAREER_PRODUCTIVITY,
            window = periodWindow,
            anchorDate = anchorDate,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks
        )
    }

    LaunchedEffect(careerTrendSeries) {
        val lastScored = careerTrendSeries.indexOfLast { it.overallScore != null }
        if (lastScored >= 0) selectedTrendIdx = lastScored
    }

    val periodHabitBreakdown = remember(careerHabits, allCompletions, periodStart, periodEnd, today) {
        DomainCalculations.calculateHabitPeriodConsistency(
            habits = careerHabits,
            completions = allCompletions,
            startDate = periodStart,
            endDate = periodEnd,
            today = today
        ).perHabitConsistency
            .filter { it.totalDueOccurrences > 0 }
            .sortedBy { it.consistencyPercent ?: 101 }
    }

    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
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
            contentPadding = PaddingValues(
                start = spacing.screenInset,
                end = spacing.screenInset,
                top = spacing.space2,
                bottom = spacing.dockBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            item {
                LaScreenHeader(
                    title = "Career & Productivity",
                    subtitle = "Tasks, learning habits, and skill-building progress."
                )
            }

            item {
                LaCategoryPeriodSelector(
                    window = periodWindow,
                    anchorDate = anchorDate,
                    today = today,
                    onWindowChange = { periodWindow = it },
                    onAnchorChange = { anchorDateEpoch = it.toEpochDay() }
                )
            }

            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null }
                    )
                }
            }

            // Paired Summary Cards: Career Tasks & Career Habits
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                ) {
                    LaCompletionCountTile(
                        label = "Career Tasks",
                        completed = careerPerformance.completedTasks,
                        total = careerPerformance.totalPlannedTasks,
                        icon = Icons.Outlined.TaskAlt,
                        containerColor = colors.tints.blue.base,
                        modifier = Modifier.weight(1f)
                    )
                    LaCompletionCountTile(
                        label = "Career Habits",
                        completed = careerPerformance.completedCareerHabits,
                        total = careerPerformance.totalScheduledCareerHabits,
                        icon = Icons.Outlined.Repeat,
                        containerColor = colors.tints.butter.base,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Action Buttons (matching Home & Health button dimensions)
            item {
                LaActionPillGroup(
                    secondary = listOf(
                        LaActionSpec("Add habit", { showAddHabitDialog = true }),
                        LaActionSpec("Add task", { showAddTaskDialog = true })
                    )
                )
            }

            // Career Progress Trend (same visual chart system in blue tint)
            if (careerTrendSeries.any { it.overallScore != null }) {
                item {
                    val safeIdx = selectedTrendIdx.coerceIn(0, (careerTrendSeries.size - 1).coerceAtLeast(0))
                    val selectedPt = careerTrendSeries.getOrNull(safeIdx)
                    val subtitle = if (selectedPt?.overallScore != null) {
                        "${selectedPt.label} • ${selectedPt.overallScore}/100"
                    } else {
                        "Career output & learning trend"
                    }
                    LaContainerPanel(
                        title = "Career progress trend",
                        subtitle = subtitle
                    ) {
                        LaPerformanceLineChart(
                            points = careerTrendSeries,
                            selectedIndex = safeIdx,
                            onSelectIndex = { selectedTrendIdx = it },
                            tint = colors.tints.blue,
                            emptyMessage = "No career performance data in these intervals.",
                            accessibilitySummary = "Career progress trend chart."
                        )
                    }
                }
            }

            // Per-Habit Learning Consistency Breakdown
            if (periodHabitBreakdown.isNotEmpty()) {
                item {
                    LaContainerPanel(
                        title = "Learning habit consistency",
                        subtitle = "Scheduled completions in selected period"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            periodHabitBreakdown.forEach { item ->
                                val ratio = if (item.totalDueOccurrences > 0) {
                                    item.completedDueOccurrences.toFloat() / item.totalDueOccurrences.toFloat()
                                } else 0f
                                val pctText = item.consistencyPercent?.let { " ($it%)" } ?: ""
                                LaHorizontalTrackBar(
                                    label = item.habit.name,
                                    valueText = "${item.completedDueOccurrences}/${item.totalDueOccurrences}$pctText",
                                    progress = ratio,
                                    tint = colors.tints.blue
                                )
                            }
                        }
                    }
                }
            }

            // Today's Career Tasks
            item {
                LaContainerPanel(
                    title = "Today's career tasks",
                    subtitle = "${careerTasksToday.count { it.isCompleted }} of ${careerTasksToday.size} completed",
                    actionLabel = onNavigateToAllTasks?.let { "All tasks" },
                    onActionClick = onNavigateToAllTasks
                ) {
                    if (careerTasksToday.isEmpty()) {
                        Text(
                            text = "No career tasks scheduled for today. Add work items, projects, or study plans.",
                            style = LaTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        careerTasksToday.forEachIndexed { index, task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleClickable {
                                        scope.launch {
                                            repository.setTaskCompleted(task.id, !task.isCompleted)
                                                .onFailure { errorMessage = it.message }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaAnimatedCheckbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            repository.setTaskCompleted(task.id, checked)
                                                .onFailure { errorMessage = it.message }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
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
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.tints.lavender.base)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.category.displayName,
                                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent
                                    )
                                }
                            }
                            if (index < careerTasksToday.lastIndex) {
                                LaRowDivider()
                            }
                        }
                    }
                }
            }

            // Today's Learning & Career Habits
            item {
                LaContainerPanel(
                    title = "Learning & productivity habits",
                    subtitle = "${todayHabitSummary.completedDueCount} of ${todayHabitSummary.totalDueCount} completed today"
                ) {
                    if (todayHabitSummary.dueHabits.isEmpty()) {
                        Text(
                            text = "No learning habits scheduled for today. Add habits like reading, English speaking, or coding practice.",
                            style = LaTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        todayHabitSummary.dueHabits.forEachIndexed { index, item ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.habit.name,
                                        style = LaTheme.typography.body.copy(
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (item.isCompleted) colors.textSecondary else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val scheduleDesc = item.schedulePeriod?.scheduleType?.displayName ?: "Scheduled"
                                    Text(
                                        text = scheduleDesc,
                                        style = LaTheme.typography.chartMicroLabel,
                                        color = colors.textSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.tints.lavender.base)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Due today",
                                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent
                                    )
                                }
                            }
                            if (index < todayHabitSummary.dueHabits.lastIndex) {
                                LaRowDivider()
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

    if (showAddTaskDialog) {
        TaskFormDialog(
            initialTask = null,
            defaultDate = today,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, category, date, time, minutes, area ->
                val result = repository.createTask(
                    title = title,
                    category = category,
                    plannedDate = date,
                    plannedTime = time,
                    estimatedDurationMinutes = minutes,
                    lifeArea = area
                )
                if (result.isSuccess) {
                    showAddTaskDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Task saved") }
                }
                result.map { }
            },
            defaultLifeArea = LifeArea.CAREER_PRODUCTIVITY
        )
    }

    if (showAddHabitDialog) {
        HabitFormDialog(
            initialHabit = null,
            effectiveDate = today,
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, weekdays, area ->
                scope.launch {
                    repository.createHabit(
                        name = name,
                        weekdays = weekdays,
                        effectiveFrom = today,
                        lifeArea = area
                    ).onSuccess {
                        showAddHabitDialog = false
                        snackbarHostState.showSnackbar("Habit saved")
                    }.onFailure { errorMessage = it.message }
                }
            },
            onDeactivate = null,
            onRequestDelete = null,
            defaultLifeArea = LifeArea.CAREER_PRODUCTIVITY
        )
    }
}
