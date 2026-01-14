// FILE: app/src/main/java/com/example/expensetracker/ui/navigation/ExpenseNavGraph.kt
package com.example.expensetracker.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        composable(Routes.HOME) { Text("Home (TODO)") }
        composable(Routes.CATEGORIES) { Text("Categories (TODO)") }
        composable(Routes.REPORTS) { Text("Reports (TODO)") }

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
            Text("Import bank CSV (TODO)")
        }

        composable(Routes.BACKUP_RESTORE) {
            // Replace with your real screen later if you have it:
            Text("Backup & Restore (TODO)")
        }

        composable(Routes.NOTIFICATIONS) {
            // Replace with your real screen later if you have it:
            Text("Notifications (TODO)")
        }

        composable(Routes.PAYWALL) {
            // Replace with your real screen later if you have it:
            Text("Paywall (TODO)")
        }
    }
}
