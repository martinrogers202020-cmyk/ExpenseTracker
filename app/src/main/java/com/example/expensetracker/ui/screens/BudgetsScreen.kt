@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.ui.state.BudgetItemUi
import com.example.expensetracker.ui.state.CategoryOptionUi
import com.example.expensetracker.ui.viewmodel.BudgetsViewModel
import com.example.expensetracker.ui.viewmodel.BudgetsViewModelFactory
import com.example.expensetracker.util.Formatters

@Composable
fun BudgetsScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val vm: BudgetsViewModel = viewModel(factory = BudgetsViewModelFactory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_budgets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Outlined.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // Month selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::previousMonth) {
                    Icon(Icons.Outlined.KeyboardArrowLeft, null)
                }
                Text(state.monthLabel, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = vm::nextMonth) {
                    Icon(Icons.Outlined.KeyboardArrowRight, null)
                }
            }

            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.budgets_empty_state))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.budgetId }) { item ->
                        BudgetCard(item) {
                            vm.deleteBudget(item.budgetId)
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AddBudgetDialog(
                categories = state.categories,
                onDismiss = { showAdd = false },
                onSave = { categoryId, cents ->
                    vm.upsertBudget(categoryId, cents)
                    showAdd = false
                }
            )
        }
    }
}

@Composable
private fun BudgetCard(
    item: BudgetItemUi,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.categoryEmoji}  ${item.categoryName}",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, null)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(
                    R.string.budgets_spent_of,
                    Formatters.money(item.spentCents),
                    Formatters.money(item.limitCents)
                )
            )

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddBudgetDialog(
    categories: List<CategoryOptionUi>,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    // Dropdown state
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = categories.firstOrNull { it.id == selectedCategoryId }?.label ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budgets_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ✅ CATEGORY DROPDOWN
                Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.budgets_select_category)) },
                        isError = categoryError != null,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryError = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                categoryError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text(stringResource(R.string.budgets_monthly_limit_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val selectCategoryError = stringResource(R.string.budgets_select_category_error)
            val validAmountError = stringResource(R.string.budgets_valid_amount_error)
            TextButton(onClick = {
                val catId = selectedCategoryId
                if (catId == null) {
                    categoryError = selectCategoryError
                    return@TextButton
                }

                val value = amountText.replace(",", ".").toDoubleOrNull()
                if (value == null || value <= 0) {
                    amountError = validAmountError
                    return@TextButton
                }

                onSave(catId, (value * 100).toLong())
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
