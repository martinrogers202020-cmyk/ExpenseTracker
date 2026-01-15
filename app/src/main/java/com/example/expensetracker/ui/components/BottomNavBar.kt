package com.example.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.stringResource
import com.example.expensetracker.R
import com.example.expensetracker.ui.navigation.Routes

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun BottomNavBar(navController: NavController) {

    val items = listOf(
        BottomNavItem(
            route = Routes.HOME,
            label = stringResource(R.string.nav_home),
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_home)) }
        ),
        BottomNavItem(
            route = Routes.REPORTS,
            label = stringResource(R.string.nav_reports),
            icon = { Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.nav_reports)) }
        ),
        BottomNavItem(
            route = Routes.CATEGORIES,
            label = stringResource(R.string.nav_categories),
            icon = { Icon(Icons.Filled.Category, contentDescription = stringResource(R.string.nav_categories)) }
        ),
        BottomNavItem(
            route = Routes.SETTINGS,
            label = stringResource(R.string.nav_settings),
            icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                },
                icon = item.icon,
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}
