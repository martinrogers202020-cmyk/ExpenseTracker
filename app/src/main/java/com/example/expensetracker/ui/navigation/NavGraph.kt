// FILE: app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt
package com.example.expensetracker.ui.navigation

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.R
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.recurring.RecurringEngine
import com.example.expensetracker.data.recurring.RecurringReminderScheduler
import com.example.expensetracker.ui.screens.AddEditRecurringScreen
import com.example.expensetracker.ui.screens.AddEditTransactionScreen
import com.example.expensetracker.ui.screens.AddExpenseScreen
import com.example.expensetracker.ui.screens.AddIncomeScreen
import com.example.expensetracker.ui.screens.AdvancedReportsScreen
import com.example.expensetracker.ui.screens.BackupRestoreScreen
import com.example.expensetracker.ui.screens.BudgetsScreen
import com.example.expensetracker.ui.screens.CategoriesScreen
import com.example.expensetracker.ui.screens.CsvImportScreen
import com.example.expensetracker.ui.screens.ExpensesScreen
import com.example.expensetracker.ui.screens.HomeScreen
import com.example.expensetracker.ui.screens.MerchantRulesScreen // ✅ IMPORTANT FIX
import com.example.expensetracker.ui.screens.NotificationsScreen
import com.example.expensetracker.ui.screens.PaywallPlanUi
import com.example.expensetracker.ui.screens.PaywallScreen
import com.example.expensetracker.ui.screens.RecurringScreen
import com.example.expensetracker.ui.screens.ReportsScreen
import com.example.expensetracker.ui.screens.SettingsScreen
import com.example.expensetracker.ui.screens.puzzle.ApplyOperationScreen
import com.example.expensetracker.ui.screens.puzzle.DailyPuzzleScreen
import com.example.expensetracker.ui.screens.puzzle.PuzzleOperation
import com.example.expensetracker.ui.screens.puzzle.PuzzleResultsScreen
import com.example.expensetracker.ui.screens.puzzle.SelectNumberScreen
import com.example.expensetracker.ui.screens.puzzle.sharePuzzleResults
import com.example.expensetracker.ui.viewmodel.HomeViewModel
import com.example.expensetracker.ui.viewmodel.HomeViewModelFactory
import com.example.expensetracker.ui.viewmodel.PuzzleViewModel
import com.example.expensetracker.ui.viewmodel.PuzzleViewModelFactory
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

private fun todayEpochDayUtc(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    // reset time to midnight UTC
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val millis = cal.timeInMillis
    return millis / 86_400_000L
}
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    var proEnabled by rememberSaveable { mutableStateOf(false) }
    val puzzleVm: PuzzleViewModel = viewModel(factory = PuzzleViewModelFactory(appContext))

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                // Anything that can touch disk / Room should be here
                val db = DatabaseProvider.get(appContext)
                RecurringEngine(db).runIfDue(todayEpochDayUtc())

            }

            // Scheduling alarms/work is fine on main
            RecurringReminderScheduler.ensureScheduled(appContext)

        } catch (t: Throwable) {
            Log.e("NavGraph", "Startup work failed", t)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()

    val hideBottomBar =
        route == Routes.ADD_INCOME ||
                route == Routes.ADD_EXPENSE ||
                route.startsWith(Routes.EDIT_TX) ||
                route == Routes.ADD_RECURRING ||
                route.startsWith(Routes.EDIT_RECURRING) ||
                route == Routes.PAYWALL ||
                route == Routes.BACKUP_RESTORE ||
                route == Routes.NOTIFICATIONS ||
                route == Routes.ADVANCED_REPORTS ||
                route == Routes.CSV_IMPORT ||
                route == Routes.MERCHANT_RULES ||
                route == Routes.DAILY_PUZZLE ||
                route == Routes.SELECT_NUMBER ||
                route == Routes.APPLY_OPERATION ||
                route == Routes.PUZZLE_RESULTS // ✅ usually you want full screen here too

    Scaffold(
        bottomBar = { if (!hideBottomBar) AppBottomBar(navController) }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(appContext))
                val state by vm.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    state = state,
                    onPrevMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onSearchChange = vm::setSearchQuery,
                    onTypeFilterChange = vm::setTypeFilter,
                    onCategoryFilterChange = vm::setCategoryFilter,
                    onClearFilters = vm::clearFilters,
                    onOpenExpenses = { navController.navigate(Routes.EXPENSES) },
                    onAddIncomeClick = { navController.navigate(Routes.ADD_INCOME) },
                    onAddExpenseClick = { navController.navigate(Routes.ADD_EXPENSE) },
                    onOpenReports = { navController.navigate(Routes.REPORTS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onEditTransaction = { id -> navController.navigate("${Routes.EDIT_TX}/$id") },
                    onOpenDailyPuzzle = { navController.navigate(Routes.DAILY_PUZZLE) }
                )
            }

            composable(Routes.EXPENSES) {
                ExpensesScreen(
                    onBack = { navController.popBackStack() },
                    onEditExpense = { id -> navController.navigate("${Routes.EDIT_TX}/$id") }
                )
            }

            composable("${Routes.EDIT_TX}/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull()
                AddEditTransactionScreen(
                    txId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_INCOME) {
                AddIncomeScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ADD_EXPENSE) {
                AddExpenseScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.BUDGETS) {
                BudgetsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.REPORTS) {
                ReportsScreen(
                    onBack = { navController.popBackStack() },
                    proEnabled = proEnabled,
                    onOpenPaywall = { navController.navigate(Routes.PAYWALL) }
                )
            }

            composable(Routes.ADVANCED_REPORTS) {
                AdvancedReportsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CSV_IMPORT) {
                CsvImportScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(onBack = { navController.popBackStack() })
            }

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

            composable(Routes.BACKUP_RESTORE) {
                BackupRestoreScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.RECURRING) {
                RecurringScreen(
                    onBack = { navController.popBackStack() },
                    onAddRecurring = { navController.navigate(Routes.ADD_RECURRING) },
                    onEditRecurring = { id -> navController.navigate("${Routes.EDIT_RECURRING}/$id") },
                    onMarkPaid = {
                        scope.launch {
                            val db = DatabaseProvider.get(appContext)
                            RecurringEngine(db).runIfDue(todayEpochDayUtc())

                        }
                    }
                )
            }

            composable(Routes.ADD_RECURRING) {
                AddEditRecurringScreen(
                    type = TransactionType.EXPENSE,
                    recurringId = null,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("${Routes.EDIT_RECURRING}/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull()
                AddEditRecurringScreen(
                    type = TransactionType.EXPENSE,
                    recurringId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PAYWALL) {
                val plans = listOf(
                    PaywallPlanUi(
                        id = "monthly",
                        title = stringResource(R.string.paywall_plan_monthly),
                        price = stringResource(R.string.paywall_price_monthly),
                        benefits = listOf(
                            stringResource(R.string.paywall_benefit_advanced_reports),
                            stringResource(R.string.paywall_benefit_budget_alerts),
                            stringResource(R.string.paywall_benefit_csv_export)
                        )
                    ),
                    PaywallPlanUi(
                        id = "yearly",
                        title = stringResource(R.string.paywall_plan_yearly),
                        price = stringResource(R.string.paywall_price_yearly),
                        badge = stringResource(R.string.paywall_badge_best_value),
                        benefits = listOf(
                            stringResource(R.string.paywall_benefit_everything_monthly),
                            stringResource(R.string.paywall_benefit_priority_features),
                            stringResource(R.string.paywall_benefit_yearly_savings)
                        )
                    ),
                    PaywallPlanUi(
                        id = "lifetime",
                        title = stringResource(R.string.paywall_plan_lifetime),
                        price = stringResource(R.string.paywall_price_lifetime),
                        badge = stringResource(R.string.paywall_badge_one_time),
                        benefits = listOf(
                            stringResource(R.string.paywall_benefit_all_pro),
                            stringResource(R.string.paywall_benefit_pay_once),
                            stringResource(R.string.paywall_benefit_future_features)
                        )
                    )
                )

                var selectedPlanId by rememberSaveable { mutableStateOf("yearly") }

                PaywallScreen(
                    onBack = { navController.popBackStack() },
                    proEnabled = proEnabled,
                    selectedPlanId = selectedPlanId,
                    plans = plans,
                    onSelectPlan = { selectedPlanId = it },
                    onPurchase = { proEnabled = true },
                    onRestore = { proEnabled = true }
                )
            }

            composable(Routes.MERCHANT_RULES) {
                MerchantRulesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DAILY_PUZZLE) {
                val state by puzzleVm.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    puzzleVm.refreshDay()
                }
                DailyPuzzleScreen(
                    uiState = state,
                    onStart = {
                        puzzleVm.resetAttempt()
                        navController.navigate(Routes.SELECT_NUMBER)
                    },
                    onViewResults = { navController.navigate(Routes.PUZZLE_RESULTS) },
                    onRefresh = { puzzleVm.refreshDay() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SELECT_NUMBER) {
                val state by puzzleVm.uiState.collectAsStateWithLifecycle()
                SelectNumberScreen(
                    uiState = state,
                    onSelect = { index, value -> puzzleVm.selectCell(index, value) },
                    onNext = { navController.navigate(Routes.APPLY_OPERATION) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.APPLY_OPERATION) {
                val state by puzzleVm.uiState.collectAsStateWithLifecycle()
                ApplyOperationScreen(
                    uiState = state,
                    onApply = { op ->
                        val completed = when (op) {
                            PuzzleOperation.AddTwo -> puzzleVm.applyOperation { it + 2 }
                            PuzzleOperation.MinusThree -> puzzleVm.applyOperation { it - 3 }
                            PuzzleOperation.Double -> puzzleVm.applyOperation { it * 2 }
                            PuzzleOperation.Half -> puzzleVm.applyOperation { it / 2 }
                        }

                        if (completed) {
                            navController.navigate(Routes.PUZZLE_RESULTS)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PUZZLE_RESULTS) {
                val state by puzzleVm.uiState.collectAsStateWithLifecycle()
                val shareText = puzzleVm.buildShareText(state.target, state.lastResult)
                PuzzleResultsScreen(
                    uiState = state,
                    onShare = { sharePuzzleResults(context, shareText) },
                    onPlayAgain = { navController.navigate(Routes.SELECT_NUMBER) },
                    onBack = { navController.popBackStack() },
                    shareText = shareText
                )
            }
        }
    }
}
