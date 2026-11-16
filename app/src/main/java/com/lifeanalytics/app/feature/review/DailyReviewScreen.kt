package com.lifeanalytics.app.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.MetricCard
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.designsystem.SuccessGreen
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String,
    initialDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    onNavigateBack: (() -> Unit)? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var selectedDateEpoch by rememberSaveable { mutableStateOf(initialDate.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpoch)
    val isFutureDate = selectedDate.isAfter(today)

    val tasksForDate by remember(selectedDate) {
        repository.observeTasksForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val sessionsForDate by remember(selectedDate) {
        repository.observeSessionsForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val allHabits by repository.observeAllHabits().collectAsState(initial = emptyList())
    val completionsForDate by remember(selectedDate) {
        repository.observeHabitCompletionsForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val transactionsForDate by remember(selectedDate) {
        repository.observeTransactionsForDate(selectedDate)
    }.collectAsState(initial = emptyList())

    val savedReview by remember(selectedDate) {
        repository.observeReviewForDate(selectedDate)
    }.collectAsState(initial = null)

    val allSavedReviews by repository.observeAllReviews().collectAsState(initial = emptyList())

    val taskSummary = remember(tasksForDate) {
        DomainCalculations.calculateTaskSummary(tasksForDate)
    }
    val completedFocusSeconds = remember(sessionsForDate) {
        DomainCalculations.sumCompletedFocusDurationSeconds(sessionsForDate)
    }
    val habitSummary = remember(allHabits, completionsForDate, selectedDate) {
        DomainCalculations.evaluateHabitsForDate(allHabits, completionsForDate, selectedDate)
    }
    val financeSummary = remember(transactionsForDate) {
        DomainCalculations.calculateFinancePeriodSummary(transactionsForDate)
    }

    var wentWellInput by rememberSaveable(selectedDateEpoch) { mutableStateOf("") }
    var improveInput by rememberSaveable(selectedDateEpoch) { mutableStateOf("") }
    var rememberInput by rememberSaveable(selectedDateEpoch) { mutableStateOf("") }
    var loadedReviewId by rememberSaveable(selectedDateEpoch) { mutableStateOf<String?>(null) }

    LaunchedEffect(savedReview, selectedDateEpoch) {
        if (savedReview != null && loadedReviewId != "${savedReview?.id}_${savedReview?.updatedAtEpochMillis}") {
            wentWellInput = savedReview?.wentWell ?: ""
            improveInput = savedReview?.improveTomorrow ?: ""
            rememberInput = savedReview?.remember ?: ""
            loadedReviewId = "${savedReview?.id}_${savedReview?.updatedAtEpochMillis}"
        } else if (savedReview == null && loadedReviewId != null) {
            wentWellInput = ""
            improveInput = ""
            rememberInput = ""
            loadedReviewId = null
        }
    }

    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Review") },
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
                    onDateSelected = {
                        selectedDateEpoch = it.toEpochDay()
                        feedbackBanner = null
                        errorMessage = null
                    }
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

            if (feedbackBanner != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessGreen.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = feedbackBanner!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Automatic Daily Summary (Live Read-Only from source records)
            item {
                SectionHeader(
                    title = "Daily Summary",
                    actionLabel = savedReview?.status?.displayName
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Tasks",
                            value = taskSummary.completionRatioText,
                            supportingText = if (taskSummary.hasTasks) {
                                "${taskSummary.completedCount} of ${taskSummary.totalCount} completed"
                            } else {
                                "No tasks planned"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Focused time",
                            value = DateAttributionPolicy.formatDurationSeconds(completedFocusSeconds),
                            supportingText = "${sessionsForDate.count { it.status == FocusSessionStatus.COMPLETED }} session(s)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Habits",
                            value = habitSummary.completionRatioText,
                            supportingText = if (habitSummary.hasDueHabits) {
                                "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due done"
                            } else {
                                "0 scheduled"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Expenses",
                            value = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                            supportingText = if (financeSummary.totalIncomeMinor > 0L) {
                                "Income: ${MoneyUtils.formatMinorUnits(financeSummary.totalIncomeMinor, currencyCode)}"
                            } else {
                                "${financeSummary.transactionCount} transaction(s)"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Planned vs Actual Tasks Section
            item {
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
                        Text(
                            text = "Planned vs Actual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (tasksForDate.isEmpty()) {
                            Text(
                                text = "No tasks were planned for this date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            tasksForDate.forEach { task ->
                                val linkedFocusSeconds = sessionsForDate
                                    .filter { it.taskId == task.id && it.status == FocusSessionStatus.COMPLETED }
                                    .sumOf { it.focusedDurationSeconds }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) {
                                            Icons.Outlined.CheckCircle
                                        } else {
                                            Icons.Outlined.RadioButtonUnchecked
                                        },
                                        contentDescription = if (task.isCompleted) "Completed" else "Planned",
                                        tint = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val plannedText = task.estimatedDurationMinutes?.let {
                                            "Planned: ${DateAttributionPolicy.formatMinutes(it)}"
                                        } ?: "No duration estimate"
                                        val actualText = if (linkedFocusSeconds > 0L) {
                                            "Linked focus: ${DateAttributionPolicy.formatDurationSeconds(linkedFocusSeconds)}"
                                        } else {
                                            "No focus session linked"
                                        }
                                        Text(
                                            text = "$plannedText • $actualText",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Optional Reflection Prompts
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reflection (optional)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (savedReview != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = savedReview!!.status.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = wentWellInput,
                            onValueChange = {
                                wentWellInput = it
                                feedbackBanner = null
                            },
                            label = { Text("What went well today?") },
                            placeholder = { Text("Small wins, progress, or moments that worked...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = improveInput,
                            onValueChange = {
                                improveInput = it
                                feedbackBanner = null
                            },
                            label = { Text("What could be better tomorrow?") },
                            placeholder = { Text("One adjustment or priority for tomorrow...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = rememberInput,
                            onValueChange = {
                                rememberInput = it
                                feedbackBanner = null
                            },
                            label = { Text("Anything worth remembering?") },
                            placeholder = { Text("Notes, thoughts, or context from today...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isFutureDate) {
                            Text(
                                text = "Future dates can be saved as a draft, and marked complete on or after that date.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.saveDailyReview(
                                            reviewDate = selectedDate,
                                            wentWell = wentWellInput,
                                            improveTomorrow = improveInput,
                                            remember = rememberInput,
                                            status = ReviewStatus.DRAFT
                                        ).onSuccess {
                                            feedbackBanner = "Draft saved for ${DateAttributionPolicy.formatShortDate(selectedDate)}."
                                            errorMessage = null
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Could not save draft."
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Draft")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.saveDailyReview(
                                            reviewDate = selectedDate,
                                            wentWell = wentWellInput,
                                            improveTomorrow = improveInput,
                                            remember = rememberInput,
                                            status = ReviewStatus.COMPLETED
                                        ).onSuccess {
                                            feedbackBanner = "Daily review completed for ${DateAttributionPolicy.formatShortDate(selectedDate)}."
                                            errorMessage = null
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Could not complete review."
                                        }
                                    }
                                },
                                enabled = !isFutureDate,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (savedReview?.status == ReviewStatus.COMPLETED) "Update Review" else "Complete Review")
                            }
                        }
                    }
                }
            }

            // Past Reviews History
            if (allSavedReviews.isNotEmpty()) {
                item {
                    SectionHeader(title = "Saved Reviews (${allSavedReviews.size})")
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allSavedReviews.take(10).forEach { review ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDateEpoch = review.reviewDate.toEpochDay()
                                        feedbackBanner = null
                                        errorMessage = null
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (review.reviewDate == selectedDate) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = DateAttributionPolicy.formatDisplayDate(review.reviewDate),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val snippet = review.wentWell
                                            ?: review.improveTomorrow
                                            ?: review.remember
                                            ?: "Summary reviewed (no written notes)"
                                        Text(
                                            text = snippet,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = review.status.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (review.status == ReviewStatus.COMPLETED) {
                                            SuccessGreen
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
