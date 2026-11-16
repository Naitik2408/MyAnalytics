package com.lifeanalytics.app.feature.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LocalExtendedColors
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPlanningScreen(
    repository: LifeAnalyticsRepository,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onStartFocusForTask: (Task) -> Unit,
    openAddDialogInitially: Boolean = false,
    onConsumedInitialAddDialog: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val tasks by repository.observeTasksForDate(selectedDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val summary = remember(tasks) { DomainCalculations.calculateTaskSummary(tasks) }

    var selectedCategoryFilter by rememberSaveable { mutableStateOf<TaskCategory?>(null) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(openAddDialogInitially) {
        if (openAddDialogInitially) {
            editingTask = null
            showTaskDialog = true
            onConsumedInitialAddDialog()
        }
    }

    val filteredTasks = remember(tasks, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            tasks
        } else {
            tasks.filter { it.category == selectedCategoryFilter }
        }
    }
    val incompleteTasks = remember(filteredTasks) { filteredTasks.filter { !it.isCompleted } }
    val completedTasks = remember(filteredTasks) { filteredTasks.filter { it.isCompleted } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Planning") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add task", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DateSelectorBar(
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = onDateChange
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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Your progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!summary.hasTasks) {
                            Text(
                                text = "No tasks planned",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Planned time: No time estimates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${summary.completedCount} of ${summary.totalCount} tasks completed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    summary.completedCount.toFloat() / summary.totalCount.toFloat()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val plannedTimeText = summary.totalEstimatedMinutes?.let {
                                "Planned time (estimated): ${DateAttributionPolicy.formatSummaryMinutes(it)}"
                            } ?: "Planned time: No time estimates"
                            Text(
                                text = plannedTimeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All") }
                    )
                    TaskCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                            },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Tasks",
                    actionLabel = "+ Add task",
                    onActionClick = {
                        editingTask = null
                        showTaskDialog = true
                    }
                )
            }

            if (tasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No tasks planned for this day",
                        message = "Add the tasks you intend to work on. Planned time and duration estimates are optional.",
                        actionLabel = "Add task",
                        onActionClick = {
                            editingTask = null
                            showTaskDialog = true
                        }
                    )
                }
            } else if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No tasks in this category",
                        message = "No tasks match the selected category filter on this date.",
                        actionLabel = "Clear filter",
                        onActionClick = { selectedCategoryFilter = null }
                    )
                }
            } else {
                items(incompleteTasks, key = { it.id }) { task ->
                    TaskRowCard(
                        task = task,
                        onToggleComplete = {
                            scope.launch {
                                repository.setTaskCompleted(task.id, !task.isCompleted)
                                    .onFailure { errorMessage = it.message }
                            }
                        },
                        onEdit = {
                            editingTask = task
                            showTaskDialog = true
                        },
                        onDelete = { taskPendingDelete = task },
                        onStartFocus = { onStartFocusForTask(task) }
                    )
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed (${completedTasks.size})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(completedTasks, key = { it.id }) { task ->
                        TaskRowCard(
                            task = task,
                            onToggleComplete = {
                                scope.launch {
                                    repository.setTaskCompleted(task.id, !task.isCompleted)
                                        .onFailure { errorMessage = it.message }
                                }
                            },
                            onEdit = {
                                editingTask = task
                                showTaskDialog = true
                            },
                            onDelete = { taskPendingDelete = task },
                            onStartFocus = null
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }

    if (showTaskDialog) {
        TaskFormDialog(
            initialTask = editingTask,
            defaultDate = selectedDate,
            onDismiss = { showTaskDialog = false },
            onSave = { title, category, plannedDate, plannedTime, estimateMinutes ->
                val result = if (editingTask == null) {
                    repository.createTask(
                        title = title,
                        category = category,
                        plannedDate = plannedDate,
                        plannedTime = plannedTime,
                        estimatedDurationMinutes = estimateMinutes
                    )
                } else {
                    repository.updateTask(
                        taskId = editingTask!!.id,
                        title = title,
                        category = category,
                        plannedDate = plannedDate,
                        plannedTime = plannedTime,
                        estimatedDurationMinutes = estimateMinutes
                    )
                }
                result.map {
                    showTaskDialog = false
                    editingTask = null
                }
            }
        )
    }

    taskPendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete this task?",
            message = "\"${target.title}\" will be removed from your plan. Any focus sessions already recorded with this task will be preserved in your focus history.",
            onConfirm = {
                scope.launch {
                    repository.deleteTask(target.id)
                        .onFailure { errorMessage = it.message }
                    taskPendingDelete = null
                }
            },
            onDismiss = { taskPendingDelete = null }
        )
    }
}

@Composable
private fun TaskRowCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: (() -> Unit)?
) {
    val extendedColors = LocalExtendedColors.current
    val metadataParts = buildList {
        add(task.category.displayName)
        task.plannedTime?.let { add(DateAttributionPolicy.formatDisplayTime(it)) }
        if (task.estimatedDurationMinutes != null) {
            add("Est. ${DateAttributionPolicy.formatSummaryMinutes(task.estimatedDurationMinutes)}")
        }
        if (task.isCompleted) {
            add("Completed")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.semantics {
                    contentDescription = if (task.isCompleted) {
                        "Mark ${task.title} incomplete"
                    } else {
                        "Mark ${task.title} complete"
                    }
                }
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = extendedColors.success
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metadataParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onStartFocus != null && !task.isCompleted) {
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.semantics {
                        contentDescription = "Start focus session for ${task.title}"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.semantics {
                    contentDescription = "Edit ${task.title}"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics {
                    contentDescription = "Delete ${task.title}"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskFormDialog(
    initialTask: Task?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: suspend (
        title: String,
        category: TaskCategory,
        plannedDate: LocalDate,
        plannedTime: LocalTime?,
        estimatedMinutes: Int?
    ) -> Result<Unit>
) {
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var category by rememberSaveable { mutableStateOf(initialTask?.category ?: TaskCategory.OTHER) }
    var plannedDateIso by rememberSaveable {
        mutableStateOf(DateAttributionPolicy.formatIsoDate(initialTask?.plannedDate ?: defaultDate))
    }
    var plannedTimeText by rememberSaveable {
        mutableStateOf(initialTask?.plannedTime?.let { DateAttributionPolicy.formatLocalTime(it) } ?: "")
    }
    var estimatedMinutesText by rememberSaveable {
        mutableStateOf(initialTask?.estimatedDurationMinutes?.toString() ?: "")
    }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (initialTask == null) "Add task" else "Edit task") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Task name (required)") },
                    placeholder = { Text("e.g. Prepare project presentation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = plannedDateIso,
                    onValueChange = {
                        plannedDateIso = it
                        validationError = null
                    },
                    label = { Text("Planned date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = plannedTimeText,
                    onValueChange = {
                        plannedTimeText = it
                        validationError = null
                    },
                    label = { Text("Planned time (optional, HH:mm)") },
                    placeholder = { Text("e.g. 10:00 or leave blank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = estimatedMinutesText,
                    onValueChange = {
                        estimatedMinutesText = it
                        validationError = null
                    },
                    label = { Text("Estimated duration in minutes (optional)") },
                    placeholder = { Text("e.g. 30 or leave blank") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { preset ->
                        TextButton(onClick = { estimatedMinutesText = preset.toString() }) {
                            Text("${preset}m")
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val parsedDate = try {
                        DateAttributionPolicy.parseIsoDate(plannedDateIso.trim())
                    } catch (_: Exception) {
                        validationError = "Enter a valid date in YYYY-MM-DD format."
                        return@Button
                    }

                    val trimmedTime = plannedTimeText.trim()
                    val parsedTime = if (trimmedTime.isEmpty()) {
                        null
                    } else {
                        DateAttributionPolicy.parseLocalTimeOrNull(trimmedTime) ?: run {
                            validationError = "Enter planned time in 24-hour HH:mm format (e.g. 09:30 or 14:00)."
                            return@Button
                        }
                    }

                    val trimmedEstimate = estimatedMinutesText.trim()
                    val parsedEstimate = if (trimmedEstimate.isEmpty()) {
                        null
                    } else {
                        val value = trimmedEstimate.toIntOrNull()
                        if (value == null || value <= 0 || value > 1440) {
                            validationError = "Estimated duration must be between 1 and 1440 minutes."
                            return@Button
                        }
                        value
                    }

                    isSaving = true
                    scope.launch {
                        val res = onSave(title, category, parsedDate, parsedTime, parsedEstimate)
                        isSaving = false
                        res.onFailure { validationError = it.message ?: "Could not save task." }
                    }
                }
            ) {
                Text(if (initialTask == null) "Save task" else "Save changes")
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
