package com.lifeanalytics.app.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaMicroProgressBar
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.LaTintedTile
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.FocusSessionStatus
import com.lifeanalytics.app.domain.model.ReviewStatus
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun DailyReviewScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String,
    initialDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    onNavigateBack: (() -> Unit)? = null
) {
    val colors = LaTheme.colors
    val tints = colors.tints
    val typography = LaTheme.typography
    val spacing = LaTheme.spacing
    val radii = LaTheme.radii
    val snackbarHostState = remember { SnackbarHostState() }

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
                    title = "Daily Review",
                    subtitle = DateAttributionPolicy.formatDisplayDate(selectedDate),
                    onNavigateBack = onNavigateBack
                )
            }

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
                    LaTintedTile(
                        containerColor = tints.mint.soft,
                        shape = radii.shapeSm
                    ) {
                        Text(
                            text = feedbackBanner!!,
                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary
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

            // 2x2 Bento of live source metrics using module tints (Rule 2, Rule 6, Rule 8)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                    ) {
                        LaStatTile(
                            label = "Tasks",
                            value = if (taskSummary.hasTasks) taskSummary.completionRatioText else "0",
                            supportingText = if (taskSummary.hasTasks) {
                                "${taskSummary.completedCount} of ${taskSummary.totalCount} completed"
                            } else {
                                "No tasks planned"
                            },
                            icon = Icons.Outlined.CheckCircleOutline,
                            containerColor = tints.lavender.base,
                            isEmptyValue = !taskSummary.hasTasks,
                            minHeight = 148.dp,
                            modifier = Modifier.weight(1f),
                            microVisual = if (taskSummary.hasTasks) {
                                {
                                    LaMicroProgressBar(
                                        progress = taskSummary.completedCount.toFloat() / taskSummary.totalCount.toFloat(),
                                        trackColor = colors.surface.copy(alpha = 0.65f),
                                        fillColor = colors.ink
                                    )
                                }
                            } else null
                        )
                        LaStatTile(
                            label = "Focused time",
                            value = DateAttributionPolicy.formatSummaryDuration(completedFocusSeconds),
                            supportingText = "${sessionsForDate.count { it.status == FocusSessionStatus.COMPLETED }} session(s)",
                            icon = Icons.Outlined.Timer,
                            containerColor = tints.mint.base,
                            isEmptyValue = completedFocusSeconds == 0L,
                            minHeight = 148.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                    ) {
                        LaStatTile(
                            label = "Habits",
                            value = if (habitSummary.hasDueHabits) habitSummary.completionRatioText else "0",
                            supportingText = if (habitSummary.hasDueHabits) {
                                "${habitSummary.completedDueCount} of ${habitSummary.totalDueCount} due done"
                            } else {
                                "0 scheduled"
                            },
                            icon = Icons.Outlined.Repeat,
                            containerColor = tints.butter.base,
                            isEmptyValue = !habitSummary.hasDueHabits,
                            minHeight = 148.dp,
                            modifier = Modifier.weight(1f),
                            microVisual = if (habitSummary.hasDueHabits) {
                                {
                                    LaMicroProgressBar(
                                        progress = habitSummary.completedDueCount.toFloat() / habitSummary.totalDueCount.toFloat(),
                                        trackColor = colors.surface.copy(alpha = 0.65f),
                                        fillColor = colors.ink
                                    )
                                }
                            } else null
                        )
                        LaStatTile(
                            label = "Expenses",
                            value = MoneyUtils.formatMinorUnits(financeSummary.totalExpenseMinor, currencyCode),
                            supportingText = if (financeSummary.totalIncomeMinor > 0L) {
                                "Income: ${MoneyUtils.formatMinorUnits(financeSummary.totalIncomeMinor, currencyCode)}"
                            } else {
                                "${financeSummary.transactionCount} transaction(s)"
                            },
                            icon = Icons.Outlined.AccountBalanceWallet,
                            containerColor = tints.blue.base,
                            isEmptyValue = !financeSummary.hasTransactions,
                            minHeight = 148.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

        // Planned vs Actual Tasks Section
        item {
            LaContainerPanel(title = "Planned vs Actual") {
                if (tasksForDate.isEmpty()) {
                    Text(
                        text = "No tasks were planned for this date.",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                } else {
                    tasksForDate.forEachIndexed { index, task ->
                        val linkedFocusSeconds = sessionsForDate
                            .filter { it.taskId == task.id && it.status == FocusSessionStatus.COMPLETED }
                            .sumOf { it.focusedDurationSeconds }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = spacing.space2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (task.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.success),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Completed",
                                        tint = colors.surface,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.textSecondary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(spacing.space3))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary
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
                                    style = typography.label,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        if (index < tasksForDate.lastIndex) {
                            LaRowDivider()
                        }
                    }
                }
            }
        }

        // Optional Reflection Prompts (Section 4.4 tint.rose for Daily Review)
        item {
            LaContainerPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reflection (optional)",
                        style = typography.sectionTitle,
                        color = colors.textPrimary
                    )
                    if (savedReview != null) {
                        Surface(
                            shape = radii.shapeFull,
                            color = tints.rose.base
                        ) {
                            Text(
                                text = savedReview!!.status.displayName,
                                style = typography.chartMicroLabel,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space3))

                Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    LaTextField(
                        value = wentWellInput,
                        onValueChange = {
                            wentWellInput = it
                            feedbackBanner = null
                        },
                        label = "What went well today?",
                        placeholder = "Small wins, progress, or moments that worked...",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )

                    LaTextField(
                        value = improveInput,
                        onValueChange = {
                            improveInput = it
                            feedbackBanner = null
                        },
                        label = "What could be better tomorrow?",
                        placeholder = "One adjustment or priority for tomorrow...",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )

                    LaTextField(
                        value = rememberInput,
                        onValueChange = {
                            rememberInput = it
                            feedbackBanner = null
                        },
                        label = "Anything worth remembering?",
                        placeholder = "Notes, thoughts, or context from today...",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )

                    if (isFutureDate) {
                        Text(
                            text = "Future dates can be saved as a draft, and marked complete on or after that date.",
                            style = typography.label,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        LaSecondaryButton(
                            text = "Save Draft",
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
                                        snackbarHostState.showSnackbar("Draft saved")
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Could not save draft."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        LaPrimaryButton(
                            text = if (savedReview?.status == ReviewStatus.COMPLETED) "Update Review" else "Complete Review",
                            enabled = !isFutureDate,
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
                                        snackbarHostState.showSnackbar("Review completed")
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Could not complete review."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
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
                LaContainerPanel {
                    val recentReviews = allSavedReviews.take(10)
                    recentReviews.forEachIndexed { index, review ->
                        val isSelected = review.reviewDate == selectedDate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(radii.shapeSm)
                                .background(
                                    if (isSelected) tints.rose.soft else colors.surface
                                )
                                .clickable {
                                    selectedDateEpoch = review.reviewDate.toEpochDay()
                                    feedbackBanner = null
                                    errorMessage = null
                                }
                                .padding(horizontal = spacing.space3, vertical = spacing.space2),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DateAttributionPolicy.formatDisplayDate(review.reviewDate),
                                    style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                val snippet = review.wentWell
                                    ?: review.improveTomorrow
                                    ?: review.remember
                                    ?: "Summary reviewed (no written notes)"
                                Text(
                                    text = snippet,
                                    style = typography.label,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(spacing.space2))
                            Surface(
                                shape = radii.shapeFull,
                                color = if (review.status == ReviewStatus.COMPLETED) tints.mint.soft else tints.butter.soft
                            ) {
                                Text(
                                    text = review.status.displayName,
                                    style = typography.chartMicroLabel,
                                    color = if (review.status == ReviewStatus.COMPLETED) colors.success else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (index < recentReviews.lastIndex) {
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
}
