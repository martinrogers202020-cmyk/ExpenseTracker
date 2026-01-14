package com.example.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.prefs.ProManager
import com.example.expensetracker.data.repo.SettingsRepository
import com.example.expensetracker.ui.viewmodel.ProViewModel
import com.example.expensetracker.ui.viewmodel.ProViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(onBack: () -> Unit) {
    val appContext = LocalContext.current.applicationContext

    // Build once
    val proManager = remember {
        ProManager(SettingsRepository(appContext))
    }

    val vm: ProViewModel = viewModel(
        factory = ProViewModelFactory(proManager)
    )

    val isPro by vm.isPro.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pro") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Enable Pro to unlock premium features.",
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = isPro,
                    onCheckedChange = { vm.setPro(it) }
                )
                Text(if (isPro) "Pro is enabled" else "Pro is disabled")
            }
        }
    }
}
