package com.example.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.ui.viewmodel.AddEditViewModel
import com.example.expensetracker.ui.viewmodel.AddEditViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    txId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext

    // ✅ key the VM so editing different ids won't reuse old state
    val vm: AddEditViewModel = viewModel(
        key = "AddEditViewModel_${txId ?: "new"}",
        factory = AddEditViewModelFactory(context)
    )

    val state by vm.uiState.collectAsState()

    LaunchedEffect(txId) {
        if (txId != null) vm.loadForEdit(txId)
    }

    var catMenu by remember { mutableStateOf(false) }
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit Transaction" else "Add Transaction") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { vm.setType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { vm.setType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Amount") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category field (click anywhere to open menu)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory?.label ?: "Select",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { catMenu = true },
                    trailingIcon = {
                        IconButton(onClick = { catMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Open categories"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = catMenu,
                    onDismissRequest = { catMenu = false }
                ) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            onClick = {
                                vm.setCategory(cat.id)
                                catMenu = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { vm.save(onBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }

            if (state.isEdit) {
                OutlinedButton(
                    onClick = { vm.delete(onBack) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
