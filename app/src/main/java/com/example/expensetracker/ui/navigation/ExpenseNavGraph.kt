// FILE: app/src/main/java/com/example/expensetracker/ui/navigation/ExpenseNavGraph.kt
package com.example.expensetracker.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.R
import com.example.expensetracker.ui.screens.AdvancedReportsScreen
import com.example.expensetracker.ui.screens.SettingsScreen

@Composable
fun ExpenseNavGraph() {
    val navController = rememberNavController()
    var proEnabled by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Routes.SETTINGS
    ) {
        // Placeholders if you don't have screens yet
        composable(Routes.HOME) { Text(stringResource(R.string.placeholder_home)) }
        composable(Routes.CATEGORIES) { Text(stringResource(R.string.placeholder_categories)) }
        composable(Routes.REPORTS) { Text(stringResource(R.string.placeholder_reports)) }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                proEnabled = proEnabled,
                onTogglePro = { proEnabled = it },
                onOpenPaywall = { navController.navigate(Routes.PAYWALL) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) },
                onOpenAdvancedReports = { navController.navigate(Routes.ADVANCED_REPORTS) },
                onOpenCsvImport = { navController.navigate(Routes.CSV_IMPORT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADVANCED_REPORTS) {
            AdvancedReportsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CSV_IMPORT) {
            // Replace with your real screen later:
            // CsvImportScreen(onBack = { navController.popBackStack() })
            Text(stringResource(R.string.placeholder_import_csv))
        }

        composable(Routes.BACKUP_RESTORE) {
            // Replace with your real screen later if you have it:
            Text(stringResource(R.string.placeholder_backup_restore))
        }

        composable(Routes.NOTIFICATIONS) {
            // Replace with your real screen later if you have it:
            Text(stringResource(R.string.placeholder_notifications))
        }

        composable(Routes.PAYWALL) {
            // Replace with your real screen later if you have it:
            Text(stringResource(R.string.placeholder_paywall))
        }
    }
}
