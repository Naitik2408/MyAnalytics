package com.lifeanalytics.app.feature.finance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaCollapsedEmptyRow
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaDateRangePill
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
import com.lifeanalytics.app.core.designsystem.LaModuleEmptyState
import com.lifeanalytics.app.core.designsystem.LaPeriodEmptyCard
import com.lifeanalytics.app.core.designsystem.LaPillBarChart
import com.lifeanalytics.app.core.designsystem.LaPillBarEntry
import com.lifeanalytics.app.core.designsystem.LaPrimaryButton
import com.lifeanalytics.app.core.designsystem.LaRingChart
import com.lifeanalytics.app.core.designsystem.LaRingSlice
import com.lifeanalytics.app.core.designsystem.LaRowDivider
import com.lifeanalytics.app.core.designsystem.LaScreenHeader
import com.lifeanalytics.app.core.designsystem.LaSecondaryButton
import com.lifeanalytics.app.core.designsystem.LaSegmentedControl
import com.lifeanalytics.app.core.designsystem.LaStatTile
import com.lifeanalytics.app.core.designsystem.LaTextButton
import com.lifeanalytics.app.core.designsystem.LaTextField
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.designsystem.pressScaleClickable
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.domain.model.DefaultFinanceCategories
import com.lifeanalytics.app.domain.model.FinanceTransaction
import com.lifeanalytics.app.domain.model.TransactionType
import com.lifeanalytics.app.domain.usecase.DomainCalculations
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.launch

private enum class FinancePeriodScope(val label: String) {
    MONTH("Monthly"),
    DAY("Daily")
}

private enum class TransactionFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expense"),
    INCOME("Income")
}

@Composable
fun FinanceScreen(
    repository: LifeAnalyticsRepository,
    currencyCode: String,
    monthlyBudgetMinor: Long? = null,
    onUpdateMonthlyBudget: suspend (Long?) -> Unit = {},
    initialDayFilter: LocalDate? = null
) {
    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var periodScope by rememberSaveable {
        mutableStateOf(if (initialDayFilter != null) FinancePeriodScope.DAY else FinancePeriodScope.MONTH)
    }
    var selectedYear by rememberSaveable { mutableIntStateOf((initialDayFilter ?: today).year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf((initialDayFilter ?: today).monthValue) }
    var selectedDayEpoch by rememberSaveable { mutableStateOf((initialDayFilter ?: today).toEpochDay()) }
    var typeFilter by rememberSaveable { mutableStateOf(TransactionFilter.ALL) }
    var selectedTrendBarIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedYearMonth = YearMonth.of(selectedYear, selectedMonth)
    val selectedDay = LocalDate.ofEpochDay(selectedDayEpoch)

    val rangeStart = if (periodScope == FinancePeriodScope.MONTH) {
        selectedYearMonth.atDay(1)
    } else {
        selectedDay
    }
    val rangeEnd = if (periodScope == FinancePeriodScope.MONTH) {
        selectedYearMonth.atEndOfMonth()
    } else {
        selectedDay
    }

    val transactionsInPeriod by remember(rangeStart, rangeEnd) {
        repository.observeTransactionsInRange(rangeStart, rangeEnd)
    }.collectAsState(initial = emptyList())

    val summary = remember(transactionsInPeriod) {
        DomainCalculations.calculateFinancePeriodSummary(transactionsInPeriod)
    }

    val filteredTransactions = remember(transactionsInPeriod, typeFilter) {
        when (typeFilter) {
            TransactionFilter.ALL -> transactionsInPeriod
            TransactionFilter.EXPENSE -> transactionsInPeriod.filter { it.type == TransactionType.EXPENSE }
            TransactionFilter.INCOME -> transactionsInPeriod.filter { it.type == TransactionType.INCOME }
        }
    }

    // Build spending trend pill bar buckets from real source transactions
    val spendingTrendEntries = remember(periodScope, rangeStart, rangeEnd, transactionsInPeriod, currencyCode) {
        if (periodScope == FinancePeriodScope.MONTH) {
            val buckets = mutableListOf<LaPillBarEntry>()
            var cursor = rangeStart
            var weekNum = 1
            while (!cursor.isAfter(rangeEnd)) {
                val chunkEnd = cursor.plusDays(6).let { if (it.isAfter(rangeEnd)) rangeEnd else it }
                val expenseMinor = transactionsInPeriod
                    .filter {
                        it.type == TransactionType.EXPENSE &&
                            !it.transactionDate.isBefore(cursor) &&
                            !it.transactionDate.isAfter(chunkEnd)
                    }
                    .sumOf { it.amountMinor }
                buckets.add(
                    LaPillBarEntry(
                        label = "Wk $weekNum",
                        value = expenseMinor / 100f,
                        valueLabel = MoneyUtils.formatMinorUnits(expenseMinor, currencyCode)
                    )
                )
                cursor = chunkEnd.plusDays(1)
                weekNum++
            }
            buckets
        } else {
            val expenseMinor = summary.totalExpenseMinor
            val incomeMinor = summary.totalIncomeMinor
            listOf(
                LaPillBarEntry(
                    label = "Income",
                    value = incomeMinor / 100f,
                    valueLabel = MoneyUtils.formatMinorUnits(incomeMinor, currencyCode)
                ),
                LaPillBarEntry(
                    label = "Expense",
                    value = expenseMinor / 100f,
                    valueLabel = MoneyUtils.formatMinorUnits(expenseMinor, currencyCode)
                )
            )
        }
    }

    // Build category breakdown ring slices
    val categoryRingSlices = remember(summary, currencyCode) {
        if (summary.totalExpenseMinor <= 0L) {
            emptyList()
        } else {
            summary.expensesByCategoryMinor.entries.take(5).map { (cat, minor) ->
                val pct = ((minor * 100.0) / summary.totalExpenseMinor).toInt().coerceAtLeast(1)
                LaRingSlice(
                    label = cat,
                    amountValue = minor.toFloat(),
                    formattedValue = MoneyUtils.formatMinorUnits(minor, currencyCode),
                    percentage = pct
                )
            }
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<FinanceTransaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
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
            verticalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            // 1. Header with Screen Title & Single Primary Ink "Add" (+) Button
            item {
                LaScreenHeader(
                    title = "Finance",
                    subtitle = "Income, expenses, and cash flow"
                ) {
                    LaPrimaryButton(
                        text = "Add",
                        icon = Icons.Outlined.Add,
                        onClick = { showAddDialog = true }
                    )
                }
            }

            // Compact Segmented Monthly/Daily Control + Compact Month/Date Pill Control
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LaSegmentedControl(
                        options = FinancePeriodScope.entries,
                        selectedOption = periodScope,
                        onOptionSelected = {
                            periodScope = it
                            selectedTrendBarIndex = 0
                        },
                        labelProvider = { it.label },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (periodScope == FinancePeriodScope.MONTH) {
                        val currentYm = YearMonth.from(today)
                        val isCurrentMonth = selectedYearMonth == currentYm
                        LaDateRangePill(
                            title = DateAttributionPolicy.formatMonthYear(selectedYearMonth),
                            badgeText = if (isCurrentMonth) "This month" else "Selected month",
                            onPrevious = {
                                val prev = selectedYearMonth.minusMonths(1)
                                selectedYear = prev.year
                                selectedMonth = prev.monthValue
                            },
                            onNext = {
                                val next = selectedYearMonth.plusMonths(1)
                                selectedYear = next.year
                                selectedMonth = next.monthValue
                            },
                            onResetToCurrent = if (!isCurrentMonth) {
                                {
                                    selectedYear = currentYm.year
                                    selectedMonth = currentYm.monthValue
                                }
                            } else null,
                            resetLabel = "This month"
                        )
                    } else {
                        DateSelectorBar(
                            selectedDate = selectedDay,
                            today = today,
                            onDateSelected = { date ->
                                selectedDayEpoch = date.toEpochDay()
                                selectedYear = date.year
                                selectedMonth = date.monthValue
                            }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onDismiss = { errorMessage = null }
                    )
                }
            }

            // 2. Tinted Blue Hero Panel for Net Amount + Paired Income & Expense Bento Tiles + Savings Rate & Budget Adherence (19 Nov 2026 spec)
            item {
                val netSignText = when {
                    summary.netForPeriodMinor > 0L -> "Positive net inflow"
                    summary.netForPeriodMinor < 0L -> "Net outflow"
                    else -> "Balanced (${MoneyUtils.formatMinorUnits(0L, currencyCode)} net)"
                }
                val savingsRateText = summary.savingsRatePercent?.let { "$it%" } ?: "No income"
                val savingsSupporting = if (summary.totalIncomeMinor > 0L) {
                    "Savings: ${MoneyUtils.formatSignedNetMinorUnits(summary.savingsAmountMinor, currencyCode)}"
                } else {
                    "Record income to compute rate"
                }

                val activeBudget = monthlyBudgetMinor?.takeIf { it > 0L }
                val periodBudgetMinor = if (activeBudget != null) {
                    if (periodScope == FinancePeriodScope.MONTH) {
                        activeBudget
                    } else {
                        (activeBudget / selectedYearMonth.lengthOfMonth().coerceAtLeast(28)).coerceAtLeast(1L)
                    }
                } else null

                val budgetValueText = if (periodBudgetMinor != null) {
                    val usedPct = ((summary.totalExpenseMinor * 100.0) / periodBudgetMinor).toInt()
                    "$usedPct% used"
                } else {
                    "Set target"
                }
                val budgetSupporting = if (periodBudgetMinor != null) {
                    val label = if (periodScope == FinancePeriodScope.MONTH) "Monthly budget" else "Daily budget share"
                    "$label: ${MoneyUtils.formatMinorUnits(periodBudgetMinor, currencyCode)} • Tap to edit"
                } else {
                    "Tap to set monthly expense budget"
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                    LaHeroPanel(
                        title = "Net cash flow for period",
                        subtitle = "$netSignText • Income minus expenses (not account balance)",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        containerColor = colors.tints.blue.strong
                    ) {
                        Text(
                            text = MoneyUtils.formatSignedNetMinorUnits(summary.netForPeriodMinor, currencyCode),
                            style = LaTheme.typography.timerValue,
                            color = colors.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                    ) {
                        LaStatTile(
                            label = "Total Income",
                            value = MoneyUtils.formatMinorUnits(summary.totalIncomeMinor, currencyCode),
                            icon = Icons.Outlined.ArrowDownward,
                            supportingText = "Recorded inflows (+)",
                            containerColor = colors.tints.mint.base,
                            valueColor = colors.income,
                            isEmptyValue = summary.totalIncomeMinor == 0L,
                            minHeight = 140.dp,
                            modifier = Modifier.weight(1f)
                        )
                        LaStatTile(
                            label = "Total Expenses",
                            value = MoneyUtils.formatMinorUnits(summary.totalExpenseMinor, currencyCode),
                            icon = Icons.Outlined.ArrowUpward,
                            supportingText = "Recorded outflows (-)",
                            containerColor = colors.tints.rose.base,
                            valueColor = colors.expense,
                            isEmptyValue = summary.totalExpenseMinor == 0L,
                            minHeight = 140.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.tileGutter)
                    ) {
                        LaStatTile(
                            label = "Savings Rate",
                            value = savingsRateText,
                            icon = Icons.Outlined.AccountBalanceWallet,
                            supportingText = savingsSupporting,
                            containerColor = colors.tints.lavender.base,
                            isEmptyValue = summary.savingsRatePercent == null,
                            minHeight = 136.dp,
                            modifier = Modifier.weight(1f)
                        )
                        LaStatTile(
                            label = "Budget Target",
                            value = budgetValueText,
                            icon = Icons.Outlined.Edit,
                            supportingText = budgetSupporting,
                            containerColor = colors.tints.butter.base,
                            isEmptyValue = periodBudgetMinor == null,
                            minHeight = 136.dp,
                            onClick = { showBudgetDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Level C Period-Empty Consolidation (v1.3 Rule 4C)
            if (transactionsInPeriod.isEmpty()) {
                item {
                    LaPeriodEmptyCard(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "No transactions in this period",
                        message = "Record an expense or income to see weekly spending trends, category shares, and your transaction log.",
                        actionLabel = "Record first entry",
                        onActionClick = { showAddDialog = true },
                        chipColor = colors.tints.blue.soft
                    )
                }
            } else {
                // Partial or full data in period: collapse empty expense charts to 1-line rows, or render full charts
                item {
                    if (summary.totalExpenseMinor <= 0L && periodScope == FinancePeriodScope.MONTH) {
                        LaCollapsedEmptyRow(
                            sectionTitle = "Spending trend by week",
                            emptyText = "no expenses recorded yet",
                            icon = Icons.Outlined.AccountBalanceWallet
                        )
                    } else {
                        LaContainerPanel(
                            title = if (periodScope == FinancePeriodScope.MONTH) {
                                "Spending trend by week"
                            } else {
                                "Daily inflow vs outflow"
                            },
                            subtitle = if (summary.totalExpenseMinor > 0L) {
                                "Total expenses: ${MoneyUtils.formatMinorUnits(summary.totalExpenseMinor, currencyCode)}"
                            } else {
                                "Zero expenses recorded in this period"
                            }
                        ) {
                            LaPillBarChart(
                                entries = spendingTrendEntries,
                                selectedIndex = selectedTrendBarIndex.coerceIn(
                                    0,
                                    (spendingTrendEntries.size - 1).coerceAtLeast(0)
                                ),
                                onSelectIndex = { selectedTrendBarIndex = it },
                                barTint = colors.tints.blue,
                                chartHeight = 124.dp,
                                emptyMessage = "",
                                accessibilitySummary = "Finance spending trend chart for selected period."
                            )
                        }
                    }
                }

                item {
                    if (categoryRingSlices.isEmpty()) {
                        LaCollapsedEmptyRow(
                            sectionTitle = "Expenses by category",
                            emptyText = "no expenses recorded yet",
                            icon = Icons.Outlined.ArrowUpward
                        )
                    } else {
                        LaContainerPanel(
                            title = "Expenses by category",
                            subtitle = "Top categories by share of spending"
                        ) {
                            LaRingChart(
                                slices = categoryRingSlices,
                                emptyMessage = "No expense categories recorded in this period yet.",
                                accessibilitySummary = "Category breakdown ring chart for expenses."
                            )
                        }
                    }
                }

                // 4. Transactions Container: Title on its own row, Filter Chips below it
                item {
                    LaContainerPanel(
                        title = "Transactions (${filteredTransactions.size})",
                        subtitle = "Newest first"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransactionFilter.entries.forEach { filter ->
                                LaFilterChip(
                                    label = filter.label,
                                    selected = typeFilter == filter,
                                    onClick = { typeFilter = filter },
                                    selectedColor = colors.tints.blue.strong
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (filteredTransactions.isEmpty()) {
                            LaModuleEmptyState(
                                icon = Icons.Outlined.AccountBalanceWallet,
                                title = "No matching transactions",
                                message = "No ${typeFilter.label.lowercase()} entries found in this period.",
                                actionLabel = "Show all",
                                onActionClick = { typeFilter = TransactionFilter.ALL },
                                chipColor = colors.tints.blue.soft
                            )
                        } else {
                            filteredTransactions.forEachIndexed { idx, tx ->
                                TransactionListRow(
                                    transaction = tx,
                                    onEdit = { transactionToEdit = tx },
                                    onDelete = { transactionToDelete = tx }
                                )
                                if (idx < filteredTransactions.lastIndex) {
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

    if (showAddDialog) {
        val defaultDate = if (periodScope == FinancePeriodScope.DAY) selectedDay else today
        TransactionFormDialog(
            initialTransaction = null,
            defaultType = TransactionType.EXPENSE,
            defaultDate = defaultDate,
            currencyCode = currencyCode,
            onDismiss = { showAddDialog = false },
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
                        showAddDialog = false
                        snackbarHostState.showSnackbar("${type.displayName} saved")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Unable to save transaction."
                    }
                }
            }
        )
    }

    if (transactionToEdit != null) {
        val editing = transactionToEdit!!
        TransactionFormDialog(
            initialTransaction = editing,
            defaultType = editing.type,
            defaultDate = editing.transactionDate,
            currencyCode = editing.currencyCode,
            onDismiss = { transactionToEdit = null },
            onSave = { type, amountMinor, category, date, note ->
                scope.launch {
                    repository.saveTransaction(
                        transactionId = editing.id,
                        type = type,
                        amountMinor = amountMinor,
                        currencyCode = editing.currencyCode,
                        category = category,
                        transactionDate = date,
                        note = note
                    ).onSuccess {
                        transactionToEdit = null
                        snackbarHostState.showSnackbar("Entry updated")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Unable to update transaction."
                    }
                }
            }
        )
    }

    if (transactionToDelete != null) {
        val deleting = transactionToDelete!!
        ConfirmDeleteDialog(
            title = "Delete transaction?",
            message = "Delete this ${deleting.type.displayName.lowercase()} of ${MoneyUtils.formatMinorUnits(deleting.amountMinor, deleting.currencyCode)} (${deleting.category})? Period totals will update immediately.",
            onConfirm = {
                scope.launch {
                    repository.deleteTransaction(deleting.id)
                    transactionToDelete = null
                    snackbarHostState.showSnackbar("Entry deleted")
                }
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    if (showBudgetDialog) {
        MonthlyBudgetFormDialog(
            currentBudgetMinor = monthlyBudgetMinor,
            currencyCode = currencyCode,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { newBudgetMinor ->
                scope.launch {
                    onUpdateMonthlyBudget(newBudgetMinor)
                    showBudgetDialog = false
                    snackbarHostState.showSnackbar(
                        if (newBudgetMinor != null) "Monthly budget target updated" else "Monthly budget cleared"
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyBudgetFormDialog(
    currentBudgetMinor: Long?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSaveBudget: (Long?) -> Unit
) {
    val colors = LaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by rememberSaveable {
        mutableStateOf(
            currentBudgetMinor?.takeIf { it > 0L }?.let { MoneyUtils.minorUnitsToEditableString(it) } ?: ""
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = LaTheme.radii.shapeXlTop,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = LaTheme.spacing.screenInset, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Monthly Expense Budget",
                style = LaTheme.typography.screenTitle,
                color = colors.textPrimary
            )
            Text(
                text = "Set a monthly expense target ($currencyCode) to evaluate budget adherence and enable the Finance & Wealth category score. Leave blank or clear to track finances without a numeric score.",
                style = LaTheme.typography.body,
                color = colors.textSecondary
            )

            LaTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    validationError = null
                },
                label = "Monthly expense budget ($currencyCode)",
                placeholder = "e.g. 25000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = LaTheme.typography.label,
                    color = colors.error
                )
            }

            if (currentBudgetMinor != null && currentBudgetMinor > 0L) {
                LaTextButton(
                    text = "Clear monthly budget target",
                    color = colors.error,
                    onClick = { onSaveBudget(null) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                LaPrimaryButton(
                    text = "Save budget",
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isEmpty()) {
                            onSaveBudget(null)
                            return@LaPrimaryButton
                        }
                        val parsed = MoneyUtils.parseToMinorUnitsOrNull(trimmed)
                        if (parsed == null || parsed <= 0L) {
                            validationError = "Enter a valid positive budget amount."
                            return@LaPrimaryButton
                        }
                        onSaveBudget(parsed)
                    },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
private fun TransactionListRow(
    transaction: FinanceTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LaTheme.colors
    val isIncome = transaction.type == TransactionType.INCOME
    val semanticColor = if (isIncome) colors.income else colors.expense
    val chipBg = if (isIncome) colors.tints.mint.base else colors.tints.rose.base
    val signPrefix = if (isIncome) "+" else "-"
    val typeLabel = if (isIncome) "Income" else "Expense"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(chipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = typeLabel,
                tint = semanticColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = transaction.category,
                    style = LaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(LaTheme.radii.shapeFull)
                        .background(colors.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = LaTheme.typography.chartMicroLabel,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val dateStr = DateAttributionPolicy.formatShortDate(transaction.transactionDate)
            val subtitle = if (!transaction.note.isNullOrBlank()) {
                "$dateStr • ${transaction.note}"
            } else {
                dateStr
            }
            Text(
                text = subtitle,
                style = LaTheme.typography.label,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$signPrefix${MoneyUtils.formatMinorUnits(transaction.amountMinor, transaction.currencyCode)}",
            style = LaTheme.typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
            color = semanticColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        LaCircularIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = "Edit transaction",
            onClick = onEdit,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(4.dp))
        LaCircularIconButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete transaction",
            onClick = onDelete,
            contentColor = colors.error,
            size = 44.dp
        )
    }
}

/**
 * Step 3 — Add/Edit Entry Bottom Sheet (`ModalBottomSheet`, `radius.xl = 40.dp` top corners, drag handle):
 * - Large tabular ₹ amount field at the top with numeric decimal keyboard
 * - Income / Expense segmented control
 * - Category chips, Date chip, optional short note
 * - Primary action label states outcome ("Save expense" / "Save income")
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    initialTransaction: FinanceTransaction?,
    defaultType: TransactionType = TransactionType.EXPENSE,
    defaultDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    @Suppress("UNUSED_PARAMETER") currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amountMinor: Long,
        category: String,
        transactionDate: LocalDate,
        note: String?
    ) -> Unit
) {
    var selectedType by rememberSaveable {
        mutableStateOf(initialTransaction?.type ?: defaultType)
    }
    var amountInput by rememberSaveable {
        mutableStateOf(
            initialTransaction?.let { MoneyUtils.minorUnitsToEditableString(it.amountMinor) } ?: ""
        )
    }
    val categoriesForType = remember(selectedType) {
        DefaultFinanceCategories.categoriesFor(selectedType)
    }
    var selectedCategory by rememberSaveable(selectedType) {
        val initialCat = initialTransaction?.category?.takeIf {
            DefaultFinanceCategories.isValidCategory(selectedType, it)
        }
        mutableStateOf(initialCat ?: categoriesForType.first())
    }
    var selectedDateEpoch by rememberSaveable {
        mutableStateOf((initialTransaction?.transactionDate ?: defaultDate).toEpochDay())
    }
    var noteInput by rememberSaveable {
        mutableStateOf(initialTransaction?.note ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedDate = LocalDate.ofEpochDay(selectedDateEpoch)
    val parsedAmountMinor = MoneyUtils.parseToMinorUnitsOrNull(amountInput)
    val colors = LaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = LaTheme.radii.shapeXlTop,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LaTheme.spacing.screenInset, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialTransaction == null) "Add entry" else "Edit entry",
                style = LaTheme.typography.screenTitle,
                color = colors.textPrimary
            )

            // 1. Income / Expense Segmented Control
            LaSegmentedControl(
                options = TransactionType.entries,
                selectedOption = selectedType,
                onOptionSelected = {
                    selectedType = it
                    validationError = null
                },
                labelProvider = { it.displayName },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Large Tabular Amount Field at Top with ₹ Symbol
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Amount (₹) *",
                    style = LaTheme.typography.label,
                    color = if (validationError != null) colors.error else colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        validationError = null
                    },
                    prefix = {
                        Text(
                            text = "₹ ",
                            style = LaTheme.typography.metricValue,
                            color = if (selectedType == TransactionType.INCOME) colors.income else colors.expense
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = LaTheme.typography.metricValue,
                            color = colors.textSecondary.copy(alpha = 0.55f)
                        )
                    },
                    singleLine = true,
                    isError = validationError != null,
                    textStyle = LaTheme.typography.metricValue.copy(color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = LaTheme.radii.shapeMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        errorContainerColor = colors.surfaceVariant,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = colors.error,
                        cursorColor = colors.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = validationError!!,
                        style = LaTheme.typography.chartMicroLabel,
                        color = colors.error
                    )
                } else if (amountInput.isNotBlank() && parsedAmountMinor == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a positive amount up to 2 decimal places (e.g. 250 or 49.50).",
                        style = LaTheme.typography.chartMicroLabel,
                        color = colors.error
                    )
                }
            }

            // 3. Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category *",
                    style = LaTheme.typography.label,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesForType.forEach { category ->
                        LaFilterChip(
                            label = category,
                            selected = selectedCategory.equals(category, ignoreCase = true),
                            onClick = {
                                selectedCategory = category
                                validationError = null
                            },
                            selectedColor = if (selectedType == TransactionType.INCOME) {
                                colors.tints.mint.strong
                            } else {
                                colors.tints.blue.strong
                            }
                        )
                    }
                }
            }

            // 4. Date Chip & Optional Short Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date",
                    style = LaTheme.typography.label,
                    color = colors.textSecondary
                )
                LaFilterChip(
                    label = DateAttributionPolicy.formatShortDate(selectedDate),
                    selected = true,
                    onClick = { showDatePicker = true },
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    selectedColor = colors.tints.lavender.soft
                )
            }

            LaTextField(
                value = noteInput,
                onValueChange = {
                    noteInput = it
                    validationError = null
                },
                label = "Note (optional)",
                placeholder = "Short description"
            )

            // 5. Pinned Primary Ink Action + Cancel
            val primaryCtaLabel = when {
                initialTransaction != null && selectedType == TransactionType.EXPENSE -> "Update expense"
                initialTransaction != null -> "Update income"
                selectedType == TransactionType.EXPENSE -> "Save expense"
                else -> "Save income"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LaSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                LaPrimaryButton(
                    text = primaryCtaLabel,
                    enabled = parsedAmountMinor != null && noteInput.trim().length <= 300,
                    onClick = {
                        val amountMinor = parsedAmountMinor
                        if (amountMinor == null) {
                            validationError = "Enter a valid positive amount in ₹."
                            return@LaPrimaryButton
                        }
                        if (!DefaultFinanceCategories.isValidCategory(selectedType, selectedCategory)) {
                            validationError = "Select a category."
                            return@LaPrimaryButton
                        }
                        if (noteInput.trim().length > 300) {
                            validationError = "Note must be 300 characters or fewer."
                            return@LaPrimaryButton
                        }
                        onSave(
                            selectedType,
                            amountMinor,
                            selectedCategory,
                            selectedDate,
                            noteInput.trim().takeIf { it.isNotEmpty() }
                        )
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                LaTextButton(
                    text = "Select",
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            selectedDateEpoch = picked.toEpochDay()
                        }
                        showDatePicker = false
                    }
                )
            },
            dismissButton = {
                LaTextButton(
                    text = "Cancel",
                    color = colors.textSecondary,
                    onClick = { showDatePicker = false }
                )
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
