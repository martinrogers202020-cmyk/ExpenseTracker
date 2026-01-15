@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.model.RecurringFrequency
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.ui.viewmodel.AddEditRecurringViewModel
import com.example.expensetracker.ui.viewmodel.AddEditRecurringViewModelFactory

@Composable
fun AddEditRecurringScreen(
    type: TransactionType,
    recurringId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val vm: AddEditRecurringViewModel =
        viewModel(factory = AddEditRecurringViewModelFactory(context))

    val state by vm.state.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(recurringId) {
        if (recurringId != null) vm.load(recurringId)
    }

    val frequencyOptions = remember { RecurringFrequency.values().toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (recurringId == null) {
                            stringResource(R.string.recurring_add_title)
                        } else {
                            stringResource(R.string.recurring_edit_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null) }
                },
                actions = {
                    if (recurringId != null) {
                        IconButton(onClick = { vm.delete(onBack) }) {
                            Icon(Icons.Outlined.DeleteOutline, null)
                        }
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = vm::updateNote,
                    label = { Text(stringResource(R.string.label_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = vm::updateAmount,
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelMedium) }

            items(categories, key = { it.id }) { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.updateCategory(c.id) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "${c.emoji} ${c.name}",
                        fontWeight = if (state.categoryId == c.id) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            item { Text(stringResource(R.string.recurring_frequency_title), style = MaterialTheme.typography.labelMedium) }

            items(
                items = frequencyOptions,
                key = { it.name }
            ) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.updateFrequency(f) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = frequencyLabel(f),
                        fontWeight = if (state.frequency == f) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.interval.toString(),
                    onValueChange = { vm.updateInterval(it.toIntOrNull() ?: 1) },
                    label = { Text(stringResource(R.string.recurring_repeat_every)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = { vm.save(type, onBack) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (recurringId == null) {
                            stringResource(R.string.action_create)
                        } else {
                            stringResource(R.string.action_save)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun frequencyLabel(frequency: RecurringFrequency): String =
    when (frequency) {
        RecurringFrequency.DAILY -> stringResource(R.string.recurring_frequency_daily)
        RecurringFrequency.MONTHLY -> stringResource(R.string.recurring_frequency_monthly)
        RecurringFrequency.YEARLY -> stringResource(R.string.recurring_frequency_yearly)
    }
