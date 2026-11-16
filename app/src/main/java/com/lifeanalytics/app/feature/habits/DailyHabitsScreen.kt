package com.lifeanalytics.app.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.MetricCard
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitForDateItem
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import com.lifeanalytics.app.domain.usecase.HabitConsistencyItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHabitsScreen(
    repository: LifeAnalyticsRepository,
    initialDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    onNavigateBack: (() -> Unit)? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(initialDate.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    val isFutureDate = selectedDate.isAfter(today)

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val completionsForDate by remember(selectedDate) {
        repository.observeHabitCompletionsForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val thirtyDaysAgo = remember(today) { today.minusDays(29) }
    val recentCompletions by remember(thirtyDaysAgo, today) {
        repository.observeHabitCompletionsInRange(thirtyDaysAgo, today)
    }.collectAsState(initial = emptyList())

    val dailySummary = remember(allHabits, completionsForDate, selectedDate) {
        DomainCalculations.evaluateHabitsForDate(allHabits, completionsForDate, selectedDate)
    }

    val consistency30d = remember(allHabits, recentCompletions, thirtyDaysAgo, today) {
        DomainCalculations.calculateHabitPeriodConsistency(
            habits = allHabits,
            completions = recentCompletions,
            startDate = thirtyDaysAgo,
            endDate = today,
            today = today
        )
    }
    val consistencyByHabitId = remember(consistency30d) {
        consistency30d.perHabitConsistency.associateBy { it.habit.id }
    }

    val inactiveHabits = remember(allHabits) {
        allHabits.filter { !it.isActive }
    }

    val scope = rememberCoroutineScope()
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Habits") },
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
                actions = {
                    TextButton(onClick = { showAddHabitDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Habit", fontWeight = FontWeight.SemiBold)
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateSelectorBar(
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = { selectedDateEpochDay = it.toEpochDay() }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Scheduled on date",
                        value = dailySummary.completionRatioText,
                        supportingText = if (dailySummary.hasDueHabits) {
                            "${dailySummary.completedDueCount} of ${dailySummary.totalDueCount} completed"
                        } else {
                            "0 due on this weekday"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Last 30 days",
                        value = consistency30d.consistencyPercent?.let { "$it%" } ?: "No due days",
                        supportingText = if (consistency30d.totalDueOccurrences > 0) {
                            "${consistency30d.completedDueOccurrences} / ${consistency30d.totalDueOccurrences} due days done"
                        } else {
                            "Consistency across due days"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isFutureDate) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Viewing a future date. Scheduled habits are shown below, and completion can be checked on or before today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (allHabits.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No habits created yet",
                        message = "Create recurring habits with daily, weekday, weekend, or custom schedules to track consistency.",
                        actionLabel = "Create First Habit",
                        onActionClick = { showAddHabitDialog = true }
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = "Scheduled for ${DateAttributionPolicy.formatShortDate(selectedDate)} (${dailySummary.dueHabits.size})"
                    )
                }

                if (dailySummary.dueHabits.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No habits scheduled for this date",
                            message = "Your active habits are scheduled on other weekdays."
                        )
                    }
                } else {
                    items(dailySummary.dueHabits, key = { "due_${it.habit.id}" }) { item ->
                        HabitDateRow(
                            item = item,
                            consistency = consistencyByHabitId[item.habit.id],
                            isFutureDate = isFutureDate,
                            onToggleCompleted = { checked ->
                                scope.launch {
                                    repository.setHabitCompletionForDate(
                                        habitId = item.habit.id,
                                        date = selectedDate,
                                        completed = checked
                                    ).onFailure { err ->
                                        errorMessage = err.message ?: "Unable to update habit."
                                    }
                                }
                            },
                            onEdit = { habitToEdit = item.habit }
                        )
                    }
                }

                if (dailySummary.otherActiveHabits.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader(
                            title = "Not scheduled on this date (${dailySummary.otherActiveHabits.size})"
                        )
                    }
                    items(dailySummary.otherActiveHabits, key = { "not_sched_${it.habit.id}" }) { item ->
                        HabitNotScheduledRow(
                            item = item,
                            consistency = consistencyByHabitId[item.habit.id],
                            onEdit = { habitToEdit = item.habit }
                        )
                    }
                }

                if (inactiveHabits.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionHeader(
                            title = "Inactive habits (${inactiveHabits.size})"
                        )
                    }
                    items(inactiveHabits, key = { "inactive_${it.id}" }) { habit ->
                        InactiveHabitRow(
                            habit = habit,
                            consistency = consistencyByHabitId[habit.id],
                            onDelete = { habitToDelete = habit }
                        )
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        HabitFormDialog(
            initialHabit = null,
            effectiveDate = today,
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, weekdays ->
                scope.launch {
                    repository.createHabit(
                        name = name,
                        weekdays = weekdays,
                        effectiveFrom = selectedDate.coerceAtMost(today)
                    ).onSuccess {
                        showAddHabitDialog = false
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Could not save habit."
                    }
                }
            },
            onDeactivate = null,
            onRequestDelete = null
        )
    }

    if (habitToEdit != null) {
        val editing = habitToEdit!!
        HabitFormDialog(
            initialHabit = editing,
            effectiveDate = today,
            onDismiss = { habitToEdit = null },
            onSave = { name, weekdays ->
                scope.launch {
                    repository.updateHabit(
                        habitId = editing.id,
                        name = name,
                        weekdays = weekdays,
                        effectiveDate = today
                    ).onSuccess {
                        habitToEdit = null
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Could not update habit."
                    }
                }
            },
            onDeactivate = if (editing.isActive) {
                {
                    scope.launch {
                        repository.deactivateHabit(editing.id, today)
                            .onSuccess { habitToEdit = null }
                            .onFailure { err -> errorMessage = err.message }
                    }
                }
            } else null,
            onRequestDelete = {
                habitToEdit = null
                habitToDelete = editing
            }
        )
    }

    if (habitToDelete != null) {
        val deleting = habitToDelete!!
        ConfirmDeleteDialog(
            title = "Delete habit permanently?",
            message = "Deleting \"${deleting.name}\" will remove the habit and all of its historical completion records. To stop tracking it going forward while keeping past history, use Deactivate instead.",
            confirmLabel = "Delete permanently",
            onConfirm = {
                scope.launch {
                    repository.deleteHabitPermanently(deleting.id)
                    habitToDelete = null
                }
            },
            onDismiss = { habitToDelete = null }
        )
    }
}

@Composable
private fun HabitDateRow(
    item: HabitForDateItem,
    consistency: HabitConsistencyItem?,
    isFutureDate: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFutureDate) { onToggleCompleted(!item.isCompleted) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                enabled = !isFutureDate,
                onCheckedChange = onToggleCompleted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scheduleLabel = formatScheduleLabel(item.schedulePeriod?.weekdays.orEmpty())
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = scheduleLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (consistency != null && consistency.totalDueOccurrences > 0) {
                        Text(
                            text = "30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences} (${consistency.consistencyPercent}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit ${item.habit.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitNotScheduledRow(
    item: HabitForDateItem,
    consistency: HabitConsistencyItem?,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val scheduleLabel = formatScheduleLabel(item.schedulePeriod?.weekdays.orEmpty())
                val consistencyText = if (consistency != null && consistency.totalDueOccurrences > 0) {
                    " • 30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences} (${consistency.consistencyPercent}%)"
                } else ""
                Text(
                    text = "Not scheduled today ($scheduleLabel)$consistencyText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit ${item.habit.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InactiveHabitRow(
    habit: Habit,
    consistency: HabitConsistencyItem?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val deactivatedText = habit.deactivatedFromLocalDate?.let {
                    "Deactivated ${DateAttributionPolicy.formatShortDate(it)}"
                } ?: "Inactive"
                val consistencyText = if (consistency != null && consistency.totalDueOccurrences > 0) {
                    " • 30d: ${consistency.completedDueOccurrences}/${consistency.totalDueOccurrences}"
                } else ""
                Text(
                    text = "$deactivatedText$consistencyText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete ${habit.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitFormDialog(
    initialHabit: Habit?,
    effectiveDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (name: String, weekdays: Set<DayOfWeek>) -> Unit,
    onDeactivate: (() -> Unit)?,
    onRequestDelete: (() -> Unit)?
) {
    val currentPeriod = initialHabit?.effectiveScheduleOn(effectiveDate)
        ?: initialHabit?.schedulePeriods?.lastOrNull()
    var name by rememberSaveable { mutableStateOf(initialHabit?.name ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            currentPeriod?.weekdays?.takeIf { it.isNotEmpty() } ?: DayOfWeek.entries.toSet()
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val everydaySet = DayOfWeek.entries.toSet()
    val weekdaysSet = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
    val weekendsSet = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialHabit == null) "Add Habit" else "Edit Habit")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Habit name *") },
                    placeholder = { Text("e.g., Read 20 pages, Exercise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick schedule",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedDays == everydaySet,
                        onClick = {
                            selectedDays = everydaySet
                            validationError = null
                        },
                        label = { Text("Every day") }
                    )
                    FilterChip(
                        selected = selectedDays == weekdaysSet,
                        onClick = {
                            selectedDays = weekdaysSet
                            validationError = null
                        },
                        label = { Text("Weekdays") }
                    )
                    FilterChip(
                        selected = selectedDays == weekendsSet,
                        onClick = {
                            selectedDays = weekendsSet
                            validationError = null
                        },
                        label = { Text("Weekends") }
                    )
                }

                Text(
                    text = "Scheduled weekdays *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                                validationError = null
                            },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                            }
                        )
                    }
                }

                if (initialHabit != null) {
                    Text(
                        text = "Schedule edits apply from today forward and preserve past schedule history.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onDeactivate != null || onRequestDelete != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onDeactivate != null) {
                            TextButton(onClick = onDeactivate) {
                                Text("Deactivate habit")
                            }
                        }
                        if (onRequestDelete != null) {
                            TextButton(onClick = onRequestDelete) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Enter a habit name."
                        return@Button
                    }
                    if (selectedDays.isEmpty()) {
                        validationError = "Select at least one scheduled weekday."
                        return@Button
                    }
                    onSave(trimmed, selectedDays)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatScheduleLabel(weekdays: Set<DayOfWeek>): String {
    if (weekdays.isEmpty()) return "Not scheduled"
    return when (HabitScheduleType.fromWeekdays(weekdays)) {
        HabitScheduleType.EVERY_DAY -> "Every day"
        HabitScheduleType.WEEKDAYS -> "Weekdays"
        HabitScheduleType.WEEKENDS -> "Weekends"
        HabitScheduleType.CUSTOM -> weekdays
            .sortedBy { it.value }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    }
}
