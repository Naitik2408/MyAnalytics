package com.lifeanalytics.app.feature.finance

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifeanalytics.app.core.common.MoneyUtils
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.core.designsystem.ConfirmDeleteDialog
import com.lifeanalytics.app.core.designsystem.DateSelectorBar
import com.lifeanalytics.app.core.designsystem.EmptyStateCard
import com.lifeanalytics.app.core.designsystem.ErrorBanner
import com.lifeanalytics.app.core.designsystem.MetricCard
import com.lifeanalytics.app.core.designsystem.SectionHeader
import com.lifeanalytics.app.core.designsystem.SuccessGreen
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

private enum class FinancePeriodScope {
    MONTH,
    DAY
}

private enum class TransactionFilter {
    ALL,
    EXPENSE,
    INCOME
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedYear by rememberSaveable { mutableStateOf((initialDayFilter ?: today).year) }
    var selectedMonth by rememberSaveable { mutableStateOf((initialDayFilter ?: today).monthValue) }
    var selectedDayEpoch by rememberSaveable { mutableStateOf((initialDayFilter ?: today).toEpochDay()) }
    var typeFilter by rememberSaveable { mutableStateOf(TransactionFilter.ALL) }

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

    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<FinanceTransaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Transaction", fontWeight = FontWeight.SemiBold)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = periodScope == FinancePeriodScope.MONTH,
                        onClick = { periodScope = FinancePeriodScope.MONTH },
                        label = { Text("Monthly view") }
                    )
                    FilterChip(
                        selected = periodScope == FinancePeriodScope.DAY,
                        onClick = { periodScope = FinancePeriodScope.DAY },
                        label = { Text("Daily view") }
                    )
                }
            }

            item {
                if (periodScope == FinancePeriodScope.MONTH) {
                    MonthSelectorBar(
                        yearMonth = selectedYearMonth,
                        currentYearMonth = YearMonth.from(today),
                        onMonthChanged = { ym ->
                            selectedYear = ym.year
                            selectedMonth = ym.monthValue
                        }
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
                        label = "Total Income",
                        value = MoneyUtils.formatMinorUnits(summary.totalIncomeMinor, currencyCode),
                        supportingText = "Recorded inflows",
                        valueColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Total Expenses",
                        value = MoneyUtils.formatMinorUnits(summary.totalExpenseMinor, currencyCode),
                        supportingText = "Recorded outflows",
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val netColor = when {
                    summary.netForPeriodMinor > 0L -> SuccessGreen
                    summary.netForPeriodMinor < 0L -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                MetricCard(
                    label = "Net for this period",
                    value = MoneyUtils.formatSignedNetMinorUnits(summary.netForPeriodMinor, currencyCode),
                    supportingText = "Income minus expenses in selected period (not account balance)",
                    valueColor = netColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (summary.expensesByCategoryMinor.isNotEmpty() && summary.totalExpenseMinor > 0L) {
                item {
                    ExpenseCategoryBreakdownCard(
                        expensesByCategoryMinor = summary.expensesByCategoryMinor,
                        totalExpenseMinor = summary.totalExpenseMinor,
                        currencyCode = currencyCode
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Transactions (${filteredTransactions.size})",
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = typeFilter == TransactionFilter.ALL,
                            onClick = { typeFilter = TransactionFilter.ALL },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = typeFilter == TransactionFilter.EXPENSE,
                            onClick = { typeFilter = TransactionFilter.EXPENSE },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = typeFilter == TransactionFilter.INCOME,
                            onClick = { typeFilter = TransactionFilter.INCOME },
                            label = { Text("Income") }
                        )
                    }
                }
            }

            if (transactionsInPeriod.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No transactions in this period",
                        message = "Record an expense or income entry to track your cash flow for this period.",
                        actionLabel = "Add Transaction",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No matching transactions",
                        message = "No ${typeFilter.name.lowercase()} transactions found in this period.",
                        actionLabel = "Show All",
                        onActionClick = { typeFilter = TransactionFilter.ALL }
                    )
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onEdit = { transactionToEdit = tx },
                        onDelete = { transactionToDelete = tx }
                    )
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
            message = "Delete this ${deleting.type.displayName.lowercase()} of ${MoneyUtils.formatMinorUnits(deleting.amountMinor, deleting.currencyCode)} (${deleting.category})? Totals for this period will update immediately.",
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
private fun MonthSelectorBar(
    yearMonth: YearMonth,
    currentYearMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit
) {
    val isCurrentMonth = yearMonth == currentYearMonth
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChanged(yearMonth.minusMonths(1)) }) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous month"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DateAttributionPolicy.formatMonthYear(yearMonth),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isCurrentMonth) "Current month" else "Selected month",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentMonth) {
                    TextButton(onClick = { onMonthChanged(currentYearMonth) }) {
                        Text("This month")
                    }
                }
                IconButton(onClick = { onMonthChanged(yearMonth.plusMonths(1)) }) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next month"
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseCategoryBreakdownCard(
    expensesByCategoryMinor: Map<String, Long>,
    totalExpenseMinor: Long,
    currencyCode: String
) {
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
                text = "Expenses by category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            expensesByCategoryMinor.entries.forEach { (category, amountMinor) ->
                val share = (amountMinor.toFloat() / totalExpenseMinor.toFloat()).coerceIn(0f, 1f)
                val pct = (share * 100f).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$category ($pct%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = MoneyUtils.formatMinorUnits(amountMinor, currencyCode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: FinanceTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) SuccessGreen else MaterialTheme.colorScheme.error
    val signPrefix = if (isIncome) "+" else "-"
    val typeLabel = if (isIncome) "Income" else "Expense"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amountColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                    contentDescription = typeLabel,
                    tint = amountColor,
                    modifier = Modifier.padding(8.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$signPrefix${MoneyUtils.formatMinorUnits(transaction.amountMinor, transaction.currencyCode)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTransaction == null) "Add Transaction" else "Edit Transaction")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                validationError = null
                            },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        validationError = null
                    },
                    label = { Text("Amount ($currencySymbol) *") },
                    placeholder = { Text("e.g., 250 or 49.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriesForType.forEach { category ->
                        FilterChip(
                            selected = selectedCategory.equals(category, ignoreCase = true),
                            onClick = {
                                selectedCategory = category
                                validationError = null
                            },
                            label = { Text(category) }
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change")
                    }
                }

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = {
                        noteInput = it
                        validationError = null
                    },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Short description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = MoneyUtils.parseToMinorUnitsOrNull(amountInput)
                    if (amountMinor == null) {
                        validationError = "Enter a valid positive amount (up to 2 decimal places)."
                        return@Button
                    }
                    if (!DefaultFinanceCategories.isValidCategory(selectedType, selectedCategory)) {
                        validationError = "Select a category."
                        return@Button
                    }
                    if (noteInput.trim().length > 300) {
                        validationError = "Note must be 300 characters or fewer."
                        return@Button
                    }
                    onSave(
                        selectedType,
                        amountMinor,
                        selectedCategory,
                        selectedDate,
                        noteInput.trim().takeIf { it.isNotEmpty() }
                    )
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

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            selectedDateEpoch = picked.toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
