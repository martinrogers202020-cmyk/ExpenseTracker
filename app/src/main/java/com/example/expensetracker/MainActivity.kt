package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerAppTheme
import com.example.expensetracker.ui.viewmodel.SettingsViewModel
import com.example.expensetracker.ui.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(context)
            )

            val prefs by settingsVm.appearance.collectAsStateWithLifecycle(
                initialValue = AppearancePrefs()
            )

            LaunchedEffect(prefs.languageTag) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(prefs.languageTag)
                )
            }

            ExpenseTrackerAppTheme(prefs = prefs) {
                Surface(modifier = Modifier) {
                    NavGraph()
                }
            }
        }
    }
}
