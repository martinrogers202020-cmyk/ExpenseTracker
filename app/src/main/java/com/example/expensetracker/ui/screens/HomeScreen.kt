// FILE: app/src/main/java/com/example/expensetracker/ui/screens/HomeScreen.kt
package com.example.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.R
import com.example.expensetracker.ui.components.SectionTitle
import com.example.expensetracker.ui.state.HomeCategoryUi
import com.example.expensetracker.ui.state.HomeUiState
import com.example.expensetracker.ui.state.TxTypeFilter
import com.example.expensetracker.util.Formatters

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TxTypeFilter) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenExpenses: () -> Unit
) {
    val listState = rememberLazyListState()

    // ----- theme-ish local colors -----
    val themeBackground = MaterialTheme.colorScheme.background
    val bg = remember(themeBackground) {
        Brush.verticalGradient(
            listOf(
                themeBackground,
                Color(0xFFF7F4FF),
                Color(0xFFF2EEFF)
            )
        )
    }

    val accent = remember { Color(0xFF7B5CFF) }
    val accentSoft = remember { Color(0xFFF5F1FF) }
    val border = remember { Color(0xFFE4DBFF) }
    val textPrimary = remember { Color(0xFF2C2746) }
    val textSecondary = remember { Color(0xFF8B84A8) }
    val expenseColor = remember { Color(0xFFD45B5B) }
    val incomeColor = remember { Color(0xFF2E7D32) }

    val monthLabel by remember(state.month) { derivedStateOf { state.month.toString() } }
    val incomeStr by remember(state.summary.incomeCents) { derivedStateOf { Formatters.money(state.summary.incomeCents) } }
    val expenseStr by remember(state.summary.expenseCents) { derivedStateOf { Formatters.money(state.summary.expenseCents) } }
    val balanceStr by remember(state.summary.balanceCents) { derivedStateOf { Formatters.money(state.summary.balanceCents) } }
    val itemsCount by remember(state.transactions.size) { derivedStateOf { state.transactions.size } }

    var lastMonthKey by rememberSaveable { mutableStateOf(state.month.toString()) }
    LaunchedEffect(state.month) {
        val key = state.month.toString()
        if (key != lastMonthKey) lastMonthKey = key
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Text(stringResource(R.string.home_add_symbol), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                item(key = "month_header") {
                    MonthHeader(
                        monthLabel = monthLabel,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onPrev = onPrevMonth,
                        onNext = onNextMonth
                    )
                }

                item(key = "summary") {
                    SummaryCard(
                        income = incomeStr,
                        expense = expenseStr,
                        balance = balanceStr,
                        balanceIsNegative = state.summary.balanceCents < 0,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        expenseColor = expenseColor,
                        onAddIncome = onAddIncomeClick,
                        onAddExpense = onAddExpenseClick
                    )
                }

                item(key = "filters") {
                    FiltersCard(
                        searchQuery = state.searchQuery,
                        onSearchChange = onSearchChange,
                        typeFilter = state.typeFilter,
                        onTypeFilterChange = onTypeFilterChange,
                        categories = state.categories,
                        selectedCategoryId = state.categoryFilterId,
                        onCategoryFilterChange = onCategoryFilterChange,
                        onClearFilters = onClearFilters,
                        itemsCount = itemsCount,
                        accent = accent,
                        accentSoft = accentSoft,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                item(key = "recent_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(stringResource(R.string.home_recent_transactions))
                        TextButton(onClick = onOpenExpenses) { Text(stringResource(R.string.action_view_all)) }
                    }
                }

                items(
                    items = state.transactions,
                    key = { it.id }
                ) { tx ->
                    val dateStr = remember(tx.epochDay) { Formatters.dateFromEpochDay(tx.epochDay) }
                    val amountStr = remember(tx.amountCents, tx.type) { Formatters.signedMoney(tx.amountCents, tx.type) }
                    val isExpense = remember(tx.type) { tx.type.name.contains("EXPENSE") }

                    TransactionRowCard(
                        title = tx.title,
                        note = tx.note,
                        date = dateStr,
                        amount = amountStr,
                        amountColor = if (isExpense) expenseColor else incomeColor,
                        textSecondary = textSecondary,
                        onClick = { onEditTransaction(tx.id) }
                    )
                }
            }
        }
    }
}

/* ------------------------- small sections ------------------------- */

@Composable
private fun MonthHeader(
    monthLabel: String,
    textPrimary: Color,
    textSecondary: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.action_prev), tint = textPrimary)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.nav_home), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(monthLabel, color = textSecondary)
        }

        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.action_next), tint = textPrimary)
        }
    }
}

@Composable
private fun SummaryCard(
    income: String,
    expense: String,
    balance: String,
    balanceIsNegative: Boolean,
    border: Color,
    textPrimary: Color,
    textSecondary: Color,
    expenseColor: Color,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.label_income), color = textSecondary)
                    Text(
                        income,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.label_expenses), color = textSecondary)
                    Text(
                        expense,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor
                    )
                }
            }

            Divider(color = border)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_balance), color = textSecondary)
                Text(
                    balance,
                    fontWeight = FontWeight.Bold,
                    color = if (balanceIsNegative) expenseColor else textPrimary
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SoftActionButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_add_income),
                    container = Color(0xFFEDE9FF),
                    textColor = textPrimary,
                    onClick = onAddIncome
                )
                SoftActionButton(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.home_add_expense),
                    container = Color(0xFFF3D6D6),
                    textColor = textPrimary,
                    onClick = onAddExpense
                )
            }
        }
    }
}

@Composable
private fun FiltersCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    typeFilter: TxTypeFilter,
    onTypeFilterChange: (TxTypeFilter) -> Unit,
    categories: List<HomeCategoryUi>,
    selectedCategoryId: Long?,
    onCategoryFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    itemsCount: Int,
    accent: Color,
    accentSoft: Color,
    border: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    SoftCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(stringResource(R.string.action_search), color = textSecondary) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = border,
                    unfocusedBorderColor = border,
                    cursorColor = accent
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillChip(stringResource(R.string.home_filter_all), typeFilter == TxTypeFilter.ALL, accent, accentSoft, border) {
                    onTypeFilterChange(TxTypeFilter.ALL)
                }
                PillChip(stringResource(R.string.label_expenses), typeFilter == TxTypeFilter.EXPENSE, accent, accentSoft, border) {
                    onTypeFilterChange(TxTypeFilter.EXPENSE)
                }
                PillChip(stringResource(R.string.label_income), typeFilter == TxTypeFilter.INCOME, accent, accentSoft, border) {
                    onTypeFilterChange(TxTypeFilter.INCOME)
                }
            }

            Text(stringResource(R.string.label_category), color = textSecondary)
            CategoryDropdown(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelected = onCategoryFilterChange,
                border = border
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearFilters) { Text(stringResource(R.string.action_clear_filters), color = accent) }
                Text(stringResource(R.string.home_items_count, itemsCount), color = textSecondary)
            }
        }
    }
}

/* ------------------------- helpers ------------------------- */

@Composable
private fun SoftCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SoftActionButton(
    modifier: Modifier = Modifier,
    label: String,
    container: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = container),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
private fun PillChip(
    text: String,
    selected: Boolean,
    accent: Color,
    accentSoft: Color,
    border: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) accent.copy(alpha = 0.16f) else accentSoft
    val stroke = if (selected) accent.copy(alpha = 0.30f) else border

    Surface(
        onClick = onClick,
        color = bg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, stroke)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<HomeCategoryUi>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
    border: Color
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val allCategoriesLabel = stringResource(R.string.home_all_categories)

    val selectedLabel by remember(categories, selectedId) {
        derivedStateOf {
            categories.firstOrNull { it.id == selectedId }?.label ?: allCategoriesLabel
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = { Text(stringResource(R.string.home_dropdown_indicator), color = MaterialTheme.colorScheme.onSurface) },
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = border,
                unfocusedBorderColor = border
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_all_categories)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.label) },
                    onClick = {
                        onSelected(c.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    title: String,
    note: String,
    date: String,
    amount: String,
    amountColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = textSecondary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (note.isNotBlank()) Text(note, color = textSecondary)
                Text(date, color = textSecondary)
            }

            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
