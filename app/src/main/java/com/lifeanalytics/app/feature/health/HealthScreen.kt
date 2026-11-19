package com.lifeanalytics.app.feature.health

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
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.feature.habits.HabitFormDialog
import com.lifeanalytics.app.feature.planning.TaskFormDialog
import kotlinx.coroutines.launch

@Composable
fun HealthScreen(
    repository: LifeAnalyticsRepository,
    onNavigateToAllHabits: (() -> Unit)? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val todayCompletions by remember(today) {
        repository.observeHabitCompletionsForDate(today)
    }.collectAsState(initial = emptyList())

    val allTasks by repository.observeAllTasks().collectAsState(initial = emptyList())
    val todayTasks by remember(today) {
        repository.observeTasksForDate(today)
    }.collectAsState(initial = emptyList())

    val healthHabits = remember(allHabits) {
        allHabits.filter { it.lifeArea == LifeArea.HEALTH_LIFESTYLE }
    }

    val healthTasksToday = remember(todayTasks) {
        todayTasks.filter { it.lifeArea == LifeArea.HEALTH_LIFESTYLE }
    }

    val todayHabitSummary = remember(healthHabits, todayCompletions, today) {
        DomainCalculations.evaluateHabitsForDate(healthHabits, todayCompletions, today)
    }

    val allCompletions by repository.observeAllHabitCompletions().collectAsState(initial = emptyList())
    val allTransactions by repository.observeAllTransactions().collectAsState(initial = emptyList())

    val healthPerformance = remember(today, allHabits, allCompletions, allTasks, allTransactions) {
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
        ).health
    }

    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
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
                    title = "Health & Lifestyle",
                    subtitle = "Daily routines, wellness habits, and health adherence."
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

            // Health Score Hero Panel
            item {
                LaHeroPanel(
                    title = "Health Performance",
                    subtitle = "Monthly Habit Adherence",
                    icon = Icons.Outlined.FavoriteBorder,
                    containerColor = colors.tints.lavender.strong
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val scoreDisplay = healthPerformance.score?.let { "$it / 100" } ?: "No data"
                            Text(
                                text = scoreDisplay,
                                style = LaTheme.typography.metricValue,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val adherenceText = "${healthPerformance.completedScheduledHabits} of ${healthPerformance.totalScheduledHabits} completed (${healthPerformance.score ?: 0}%)"
                            Text(
                                text = adherenceText,
                                style = LaTheme.typography.label,
                                color = colors.onTintSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${healthHabits.size} Habits",
                                style = LaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${todayHabitSummary.completedDueCount}/${todayHabitSummary.totalDueCount} today",
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
                    HealthActionButton(
                        label = "Add habit",
                        onClick = { showAddHabitDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    HealthActionButton(
                        label = "Add task",
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Today's Scheduled Health Habits
            item {
                LaContainerPanel(
                    title = "Today's health habits",
                    subtitle = "${todayHabitSummary.completedDueCount} of ${todayHabitSummary.totalDueCount} completed",
                    actionLabel = onNavigateToAllHabits?.let { "All habits" },
                    onActionClick = onNavigateToAllHabits
                ) {
                    if (todayHabitSummary.dueHabits.isEmpty()) {
                        Text(
                            text = "No health habits scheduled for today. Add routines like water, workouts, or meditation.",
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

            // Today's Health Tasks
            if (healthTasksToday.isNotEmpty()) {
                item {
                    LaContainerPanel(
                        title = "Health tasks for today",
                        subtitle = "${healthTasksToday.count { it.isCompleted }} of ${healthTasksToday.size} completed"
                    ) {
                        healthTasksToday.forEachIndexed { index, task ->
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
                            }
                            if (index < healthTasksToday.lastIndex) {
                                LaRowDivider()
                            }
                        }
                    }
                }
            }

            // All Health Habits Management
            item {
                LaContainerPanel(
                    title = "All health habits",
                    subtitle = "${healthHabits.count { it.isActive }} active habits configured"
                ) {
                    if (healthHabits.isEmpty()) {
                        Text(
                            text = "No health habits created yet. Tap '+ Add habit' to start tracking your health routines.",
                            style = LaTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        healthHabits.forEachIndexed { index, habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleClickable { editingHabit = habit }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.name,
                                        style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
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
                                        .background(if (habit.isActive) colors.tints.lavender.base else colors.canvas)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (habit.isActive) "Active" else "Paused",
                                        style = LaTheme.typography.chartMicroLabel.copy(fontWeight = FontWeight.Medium),
                                        color = if (habit.isActive) colors.accent else colors.textSecondary
                                    )
                                }
                            }
                            if (index < healthHabits.lastIndex) {
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

    if (editingHabit != null) {
        val habitToEdit = editingHabit!!
        HabitFormDialog(
            initialHabit = habitToEdit,
            effectiveDate = today,
            onDismiss = { editingHabit = null },
            onSave = { name, weekdays, area ->
                scope.launch {
                    repository.updateHabit(
                        habitId = habitToEdit.id,
                        name = name,
                        weekdays = weekdays,
                        effectiveDate = today,
                        lifeArea = area
                    ).onSuccess {
                        editingHabit = null
                    }.onFailure { errorMessage = it.message }
                }
            },
            onDeactivate = {
                scope.launch {
                    repository.deactivateHabit(habitToEdit.id, today)
                        .onSuccess { editingHabit = null }
                        .onFailure { errorMessage = it.message }
                }
            },
            onRequestDelete = {
                scope.launch {
                    repository.deleteHabitPermanently(habitToEdit.id)
                        .onSuccess { editingHabit = null }
                        .onFailure { errorMessage = it.message }
                }
            }
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
}

@Composable
private fun HealthActionButton(
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
