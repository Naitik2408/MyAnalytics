package com.lifeanalytics.app.feature.career

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
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
import kotlinx.coroutines.launch

@Composable
fun CareerScreen(
    repository: LifeAnalyticsRepository,
    onNavigateToAllTasks: (() -> Unit)? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val careerPerformance = remember(today, allHabits, allCompletions, allTasks, allTransactions) {
        LifeAnalyticsEngine.evaluatePeriodPerformance(
            window = AnalyticsTimeWindow.MONTHLY,
            anchorDate = today,
            today = today,
            habits = allHabits,
            completions = allCompletions,
            tasks = allTasks,
            transactions = allTransactions,
            monthlyBudgetMinor = null,
            computeComparison = false
        ).career
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
                    subtitle = "Tasks, learning habits, skill-building, and focus."
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

            // Career Score Hero Panel
            item {
                LaHeroPanel(
                    title = "Career Performance",
                    subtitle = "60% Tasks · 40% Learning Habits",
                    icon = Icons.Outlined.WorkOutline,
                    containerColor = colors.tints.lavender.strong
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val scoreDisplay = careerPerformance.score?.let { "$it / 100" } ?: "No data"
                            Text(
                                text = scoreDisplay,
                                style = LaTheme.typography.metricValue,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val breakdownText = "Tasks: ${careerPerformance.taskCompletionPercent ?: 0}% · Habits: ${careerPerformance.careerHabitAdherencePercent ?: 0}%"
                            Text(
                                text = breakdownText,
                                style = LaTheme.typography.label,
                                color = colors.onTintSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${careerPerformance.completedTasks}/${careerPerformance.totalPlannedTasks} Tasks",
                                style = LaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${careerPerformance.completedCareerHabits}/${careerPerformance.totalScheduledCareerHabits} Habits",
                                style = LaTheme.typography.chartMicroLabel,
                                color = colors.onTintSecondary
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CareerActionButton(
                        label = "Add task",
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    CareerActionButton(
                        label = "Add habit",
                        onClick = { showAddHabitDialog = true },
                        modifier = Modifier.weight(1f)
                    )
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

            // Learning & Career Habits (Skill-building, Reading, English speaking)
            item {
                LaContainerPanel(
                    title = "Learning & productivity habits",
                    subtitle = "${todayHabitSummary.completedDueCount} of ${todayHabitSummary.totalDueCount} completed today"
                ) {
                    if (careerHabits.isEmpty()) {
                        Text(
                            text = "No learning habits configured yet. Add habits like reading, English speaking, or coding practice.",
                            style = LaTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        careerHabits.forEachIndexed { index, habit ->
                            val isCompletedToday = todayCompletions.any { it.habitId == habit.id && it.isCompleted }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleClickable {
                                        scope.launch {
                                            repository.setHabitCompletionForDate(
                                                habitId = habit.id,
                                                date = today,
                                                completed = !isCompletedToday
                                            ).onFailure { errorMessage = it.message }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaAnimatedCheckbox(
                                    checked = isCompletedToday,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            repository.setHabitCompletionForDate(
                                                habitId = habit.id,
                                                date = today,
                                                completed = checked
                                            ).onFailure { errorMessage = it.message }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.name,
                                        style = LaTheme.typography.body.copy(
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (isCompletedToday) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (isCompletedToday) colors.textSecondary else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val scheduleDesc = habit.effectiveScheduleOn(today)?.scheduleType?.name ?: "Scheduled"
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
                            if (index < careerHabits.lastIndex) {
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
                }
                result.map { }
            }
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
                    }.onFailure { errorMessage = it.message }
                }
            },
            onDeactivate = null,
            onRequestDelete = null
        )
    }
}

@Composable
private fun CareerActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LaTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.5f), CircleShape)
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = LaTheme.typography.buttonLabel.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
    }
}
