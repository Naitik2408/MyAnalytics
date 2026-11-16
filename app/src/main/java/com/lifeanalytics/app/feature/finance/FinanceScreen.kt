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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.LaCircularIconButton
import com.lifeanalytics.app.core.designsystem.LaContainerPanel
import com.lifeanalytics.app.core.designsystem.LaDateRangePill
import com.lifeanalytics.app.core.designsystem.LaFilterChip
import com.lifeanalytics.app.core.designsystem.LaHeroPanel
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
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<FinanceTransaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colors = LaTheme.colors
    val spacing = LaTheme.spacing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(
            start = spacing.screenInset,
            end = spacing.screenInset,
            top = spacing.space2,
            bottom = spacing.dockBottomClearance
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        // 1. Header with Screen Title & Single Primary Ink "Add" Button
        item {
            LaScreenHeader(
                title = "Finance",
                subtitle = "Income, expenses, and cash flow"
            ) {
                LaPrimaryButton(
                    text = "Add entry",
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

        // 2. Tinted Blue Hero Panel for Net Amount + Paired Income & Expense Bento Tiles
        item {
            val netSignText = when {
                summary.netForPeriodMinor > 0L -> "Positive net inflow"
                summary.netForPeriodMinor < 0L -> "Net outflow"
                else -> "Balanced ($0 net)"
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tileGutter)) {
                LaHeroPanel(
                    title = "Net for this period",
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
                        modifier = Modifier.weight(1f)
                    )
                    LaStatTile(
                        label = "Total Expenses",
                        value = MoneyUtils.formatMinorUnits(summary.totalExpenseMinor, currencyCode),
                        icon = Icons.Outlined.ArrowUpward,
                        supportingText = "Recorded outflows (-)",
                        containerColor = colors.tints.rose.base,
                        valueColor = colors.expense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Spending Trend Pill Bars & Category Ring (Ghost states when empty)
        item {
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
                    chartHeight = 132.dp,
                    emptyMessage = "No spending recorded in this period yet",
                    accessibilitySummary = "Finance spending trend chart for selected period."
                )
            }
        }

        item {
            LaContainerPanel(
                title = "Expenses by category",
                subtitle = if (categoryRingSlices.isNotEmpty()) {
                    "Top categories by share of spending"
                } else {
                    "Category breakdown appears once expenses are recorded"
                }
            ) {
                LaRingChart(
                    slices = categoryRingSlices,
                    emptyMessage = "No expense categories recorded in this period yet.",
                    accessibilitySummary = "Category breakdown ring chart for expenses."
                )
            }
        }

        // 4. Transactions Container: Title on its own row, Filter Chips below it (No word breaking!)
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

                if (transactionsInPeriod.isEmpty()) {
                    EmptyStateCard(
                        title = "No transactions in this period",
                        message = "Use the Add entry button at the top to record an expense or income.",
                        tintSoftColor = colors.tints.blue.soft
                    )
                } else if (filteredTransactions.isEmpty()) {
                    EmptyStateCard(
                        title = "No matching transactions",
                        message = "No ${typeFilter.label.lowercase()} entries found in this period.",
                        actionLabel = "Show all",
                        onActionClick = { typeFilter = TransactionFilter.ALL },
                        tintSoftColor = colors.tints.blue.soft
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
                }
            },
            onDismiss = { transactionToDelete = null }
        )
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    initialTransaction: FinanceTransaction?,
    defaultType: TransactionType = TransactionType.EXPENSE,
    defaultDate: LocalDate = DateAttributionPolicy.todayLocalDate(),
    currencyCode: String,
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
    val currencySymbol = SupportedCurrency.fromCode(currencyCode).symbol
    val colors = LaTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = LaTheme.radii.shapeMd,
        containerColor = colors.surface,
        title = {
            Text(
                text = if (initialTransaction == null) "Add transaction" else "Edit transaction",
                style = LaTheme.typography.sectionTitle,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = LaTheme.typography.label,
                        color = colors.error
                    )
                }

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

                LaTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        validationError = null
                    },
                    label = "Amount ($currencySymbol) *",
                    placeholder = "e.g. 250 or 49.50",
                    isError = validationError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

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
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date: ${DateAttributionPolicy.formatShortDate(selectedDate)}",
                        style = LaTheme.typography.body,
                        color = colors.textPrimary
                    )
                    LaSecondaryButton(
                        text = "Change",
                        icon = Icons.Outlined.CalendarMonth,
                        onClick = { showDatePicker = true }
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
            }
        },
        confirmButton = {
            LaPrimaryButton(
                text = "Save",
                onClick = {
                    val amountMinor = MoneyUtils.parseToMinorUnitsOrNull(amountInput)
                    if (amountMinor == null) {
                        validationError = "Enter a valid positive amount (up to 2 decimal places)."
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
                }
            )
        },
        dismissButton = {
            LaSecondaryButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )

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
