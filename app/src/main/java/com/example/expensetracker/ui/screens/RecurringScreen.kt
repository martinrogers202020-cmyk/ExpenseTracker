@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ui.viewmodel.RecurringViewModel
import com.example.expensetracker.ui.viewmodel.RecurringViewModelFactory

@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (Long) -> Unit,
    onMarkPaid: (Long) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val vm: RecurringViewModel = viewModel(factory = RecurringViewModelFactory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring payments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurring) {
                Icon(Icons.Outlined.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEditRecurring(item.id) }
                            ) {
                                Text(
                                    text = item.title.ifBlank { "Recurring" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(item.amountLabel)
                            }

                            Spacer(Modifier.width(8.dp))

                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { checked ->
                                    vm.toggleActive(item.id, checked)
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text("Next: ${item.nextRunLabel}")
                        Text("Frequency: ${item.frequency.name.lowercase()}")

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onMarkPaid(item.id) },
                                enabled = item.isActive
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add to expenses")
                            }
                        }
                    }
                }
            }
        }
    }
}
