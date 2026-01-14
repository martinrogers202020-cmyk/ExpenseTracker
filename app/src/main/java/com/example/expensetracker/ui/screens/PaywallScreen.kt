package com.example.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PaywallPlanUi(
    val id: String,
    val title: String,
    val price: String,
    val badge: String? = null,
    val benefits: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    proEnabled: Boolean,
    plans: List<PaywallPlanUi>,
    selectedPlanId: String,
    onSelectPlan: (String) -> Unit,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Upgrade to Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 6.dp
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (proEnabled) "Pro is active ✅" else "Unlock Pro features",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Advanced reports, export, budgets, and premium charts.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            plans.forEach { plan ->
                val selected = plan.id == selectedPlanId

                Surface(
                    onClick = { onSelectPlan(plan.id) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shadowElevation = if (selected) 10.dp else 6.dp
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(plan.title, style = MaterialTheme.typography.titleMedium)
                                    if (plan.badge != null) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(onClick = {}, label = { Text(plan.badge) })
                                    }
                                }
                                Text(plan.price, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (selected) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (plan.benefits.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                plan.benefits.forEach { b ->
                                    Text("• $b", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = { onPurchase(selectedPlanId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Continue")
            }

            TextButton(
                onClick = onRestore,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Restore purchases")
            }
        }
    }
}
