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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recurringId == null) "Add recurring" else "Edit recurring") },
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
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = vm::updateAmount,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Text("Category", style = MaterialTheme.typography.labelMedium) }

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

            item { Text("Frequency", style = MaterialTheme.typography.labelMedium) }

            items(RecurringFrequency.values().toList()) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.updateFrequency(f) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = f.name.lowercase(),
                        fontWeight = if (state.frequency == f) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.interval.toString(),
                    onValueChange = { vm.updateInterval(it.toIntOrNull() ?: 1) },
                    label = { Text("Repeat every") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = { vm.save(type, onBack) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (recurringId == null) "Create" else "Save")
                }
            }
        }
    }
}
