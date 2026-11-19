package com.lifeanalytics.app.feature.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaAnimatedCheckbox
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaHorizontalTrackBar
import com.lifeanalytics.app.core.designsystem.LaModuleEmptyState
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun DailyPlanningScreen(
    repository: LifeAnalyticsRepository,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    openAddDialogInitially: Boolean = false,
    onConsumedInitialAddDialog: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas),
            contentPadding = PaddingValues(
                start = spacing.screenInset,
                end = spacing.screenInset,
                top = spacing.space3,
                bottom = spacing.dockBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            item {
                LaScreenHeader(
                    title = "Daily Planning",
                    subtitle = DateAttributionPolicy.formatDisplayDate(selectedDate),
                    onNavigateBack = onNavigateBack,
                    trailingContent = {
                        LaPrimaryButton(
                            text = "Add task",
                            icon = Icons.Outlined.Add,
                            onClick = {
                                editingTask = null
                                showTaskDialog = true
                            }
                        )
                    }
                )
            }

            item {
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

            // Lavender Hero Panel for Daily Task Progress (Rule 6: no 0 / 0)
            item {
                val plannedTimeText = summary.totalEstimatedMinutes?.let {
                    "Planned time (estimated): ${DateAttributionPolicy.formatSummaryMinutes(it)}"
                } ?: "No tasks planned"

                LaHeroPanel(
                    title = "Your progress",
                    subtitle = plannedTimeText,
                    icon = Icons.Outlined.CheckCircleOutline,
                    containerColor = tints.lavender.strong
                ) {
                    if (!summary.hasTasks) {
                        Text(
                            text = "0",
                            style = typography.metricEmptyValue,
                            color = colors.onTintSecondary
                        )
                    } else {
                        Text(
                            text = "${summary.completedCount} / ${summary.totalCount}",
                            style = typography.metricValue,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.space2))
                        LaHorizontalTrackBar(
                            label = "${summary.completedCount} of ${summary.totalCount} tasks completed",
                            valueText = "${((summary.completedCount * 100) / summary.totalCount)}%",
                            progress = summary.completedCount.toFloat() / summary.totalCount.toFloat(),
                            tint = tints.lavender,
                            useInkFill = true
                        )
                    }
                }
            }

            // Category Filter Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    LaFilterChip(
                        label = "All",
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null }
                    )
                    TaskCategory.entries.forEach { category ->
                        LaFilterChip(
                            label = category.displayName,
                            selected = selectedCategoryFilter == category,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                            }
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Tasks")
            }

            if (tasks.isEmpty()) {
                item {
                    LaContainerPanel {
                        LaModuleEmptyState(
                            icon = Icons.Outlined.EventNote,
                            iconTint = tints.lavender.base,
                            title = "No tasks planned for this day",
                            message = "Add the tasks you intend to work on. Planned time and duration estimates are optional.",
                            actionLabel = "Add task",
                            onActionClick = {
                                editingTask = null
                                showTaskDialog = true
                            }
                        )
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                item {
                    LaContainerPanel {
                        LaModuleEmptyState(
                            icon = Icons.Outlined.EventNote,
                            iconTint = tints.lavender.base,
                            title = "No tasks in this category",
                            message = "No tasks match the selected category filter on this date.",
                            actionLabel = "Clear filter",
                            onActionClick = { selectedCategoryFilter = null }
                        )
                    }
                }
            } else {
                if (incompleteTasks.isNotEmpty()) {
                    item {
                        LaContainerPanel {
                            incompleteTasks.forEachIndexed { index, task ->
                                TaskRowItem(
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
                                    onDelete = { taskPendingDelete = task }
                                )
                                if (index < incompleteTasks.lastIndex) {
                                    LaRowDivider()
                                }
                            }
                        }
                    }
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(spacing.space1))
                        Text(
                            text = "Completed (${completedTasks.size})",
                            style = typography.sectionTitle,
                            color = colors.textSecondary
                        )
                    }
                    item {
                        LaContainerPanel {
                            completedTasks.forEachIndexed { index, task ->
                                TaskRowItem(
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
                                    onDelete = { taskPendingDelete = task }
                                )
                                if (index < completedTasks.lastIndex) {
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

    if (showTaskDialog) {
        val wasEditing = editingTask != null
        TaskFormDialog(
            initialTask = editingTask,
            defaultDate = selectedDate,
            onDismiss = { showTaskDialog = false },
            onSave = { title, category, plannedDate, plannedTime, estimateMinutes, lifeArea ->
                val result = if (editingTask == null) {
                    repository.createTask(
                        title = title,
                        category = category,
                        plannedDate = plannedDate,
                        plannedTime = plannedTime,
                        estimatedDurationMinutes = estimateMinutes,
                        lifeArea = lifeArea
                    )
                } else {
                    repository.updateTask(
                        taskId = editingTask!!.id,
                        title = title,
                        category = category,
                        plannedDate = plannedDate,
                        plannedTime = plannedTime,
                        estimatedDurationMinutes = estimateMinutes,
                        lifeArea = lifeArea
                    )
                }
                result.map {
                    showTaskDialog = false
                    editingTask = null
                    scope.launch {
                        snackbarHostState.showSnackbar(if (wasEditing) "Task updated" else "Task saved")
                    }
                }
            }
        )
    }

    taskPendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete this task?",
            message = "\"${target.title}\" will be removed from your daily plan.",
            onConfirm = {
                scope.launch {
                    repository.deleteTask(target.id)
                        .onSuccess {
                            snackbarHostState.showSnackbar("Task deleted")
                        }
                        .onFailure { errorMessage = it.message }
                    taskPendingDelete = null
                }
            },
            onDismiss = { taskPendingDelete = null }
        )
    }
}

@Composable
private fun TaskRowItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing

    val metadataParts = buildList {
        task.lifeArea?.let { add(it.displayName) }
        add(task.category.displayName)
        task.plannedTime?.let { add(DateAttributionPolicy.formatDisplayTime(it)) }
        if (task.estimatedDurationMinutes != null) {
            add("Est. ${DateAttributionPolicy.formatSummaryMinutes(task.estimatedDurationMinutes)}")
        }
        if (task.isCompleted) {
            add("Completed")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = spacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .semantics {
                    contentDescription = if (task.isCompleted) {
                        "Mark ${task.title} incomplete"
                    } else {
                        "Mark ${task.title} complete"
                    }
                }
                .padding(end = spacing.space2)
        ) {
            LaAnimatedCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing.space2)
        ) {
            Text(
                text = task.title,
                style = typography.body.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (task.isCompleted) colors.textSecondary else colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadataParts.joinToString(" · "),
                style = typography.label,
                color = colors.textSecondary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaCircularIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "Edit ${task.title}",
                onClick = onEdit,
                containerColor = colors.surfaceVariant,
                contentColor = colors.textSecondary
            )
            LaCircularIconButton(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete ${task.title}",
                onClick = onDelete,
                containerColor = tints.rose.soft,
                contentColor = colors.error
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
        estimatedMinutes: Int?,
        lifeArea: com.lifeanalytics.app.domain.model.LifeArea
    ) -> Result<Unit>
) {
    val colors = LaTheme.colors
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var selectedLifeArea by rememberSaveable {
        mutableStateOf<com.lifeanalytics.app.domain.model.LifeArea?>(initialTask?.lifeArea)
    }
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

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        shape = radii.shapeXlTop,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.cardPaddingLarge)
                .padding(bottom = spacing.space6)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Text(
                text = if (initialTask == null) "Add task" else "Edit task",
                style = typography.sectionTitle,
                color = colors.textPrimary
            )

            LaTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = "Task name",
                placeholder = "e.g. Prepare project presentation",
                singleLine = true
            )

            Text(
                text = "Life Area *",
                style = typography.label,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                com.lifeanalytics.app.domain.model.LifeArea.entries.forEach { area ->
                    LaFilterChip(
                        label = area.displayName,
                        selected = selectedLifeArea == area,
                        onClick = {
                            selectedLifeArea = area
                            validationError = null
                        }
                    )
                }
            }

            Text(
                text = "Category",
                style = typography.label,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                TaskCategory.entries.forEach { cat ->
                    LaFilterChip(
                        label = cat.displayName,
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                }
            }

            LaTextField(
                value = plannedDateIso,
                onValueChange = {
                    plannedDateIso = it
                    validationError = null
                },
                label = "Planned date (YYYY-MM-DD)",
                singleLine = true
            )

            LaTextField(
                value = plannedTimeText,
                onValueChange = {
                    plannedTimeText = it
                    validationError = null
                },
                label = "Planned time (optional, HH:mm)",
                placeholder = "e.g. 10:00 or leave blank",
                singleLine = true
            )

            LaTextField(
                value = estimatedMinutesText,
                onValueChange = {
                    estimatedMinutesText = it
                    validationError = null
                },
                label = "Estimated duration in minutes (optional)",
                placeholder = "e.g. 30 or leave blank",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                listOf(15, 30, 45, 60).forEach { preset ->
                    Surface(
                        shape = radii.shapeFull,
                        color = colors.surfaceVariant,
                        modifier = Modifier.clickable { estimatedMinutesText = preset.toString() }
                    ) {
                        Text(
                            text = "${preset}m",
                            style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = spacing.space3, vertical = 6.dp)
                        )
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = typography.label,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(spacing.space2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                LaSecondaryButton(
                    text = "Cancel",
                    enabled = !isSaving,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                LaPrimaryButton(
                    text = if (initialTask == null) "Save task" else "Update task",
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (title.trim().isEmpty()) {
                            validationError = "Enter a task name to continue."
                            return@LaPrimaryButton
                        }

                        val area = selectedLifeArea
                        if (area == null) {
                            validationError = "Select a Life Area (Health & Lifestyle, Career & Productivity, or Finance & Wealth)."
                            return@LaPrimaryButton
                        }

                        val parsedDate = try {
                            DateAttributionPolicy.parseIsoDate(plannedDateIso.trim())
                        } catch (_: Exception) {
                            validationError = "Enter a valid date in YYYY-MM-DD format."
                            return@LaPrimaryButton
                        }

                        val trimmedTime = plannedTimeText.trim()
                        val parsedTime = if (trimmedTime.isEmpty()) {
                            null
                        } else {
                            DateAttributionPolicy.parseLocalTimeOrNull(trimmedTime) ?: run {
                                validationError = "Enter planned time in 24-hour HH:mm format (e.g. 09:30 or 14:00)."
                                return@LaPrimaryButton
                            }
                        }

                        val trimmedEstimate = estimatedMinutesText.trim()
                        val parsedEstimate = if (trimmedEstimate.isEmpty()) {
                            null
                        } else {
                            val value = trimmedEstimate.toIntOrNull()
                            if (value == null || value <= 0 || value > 1440) {
                                validationError = "Estimated duration must be between 1 and 1440 minutes."
                                return@LaPrimaryButton
                            }
                            value
                        }

                        isSaving = true
                        scope.launch {
                            val res = onSave(title, category, parsedDate, parsedTime, parsedEstimate, area)
                            isSaving = false
                            res.onFailure { validationError = it.message ?: "Could not save task." }
                        }
                    }
                )
            }
        }
    }
}
