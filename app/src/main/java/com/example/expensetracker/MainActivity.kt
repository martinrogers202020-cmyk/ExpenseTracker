package com.example.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.repo.SettingsRepository
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerAppTheme
import com.example.expensetracker.ui.viewmodel.SettingsViewModel
import com.example.expensetracker.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private val settingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking {
            val prefs = settingsRepository.appearance.first()
            val locales = LocaleListCompat.forLanguageTags(prefs.languageTag)
            AppCompatDelegate.setApplicationLocales(locales)
        }

        setContent {
            val context = LocalContext.current

            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(context)
            )

            val prefs by settingsVm.appearance.collectAsStateWithLifecycle(
                initialValue = AppearancePrefs()
            )

            ExpenseTrackerAppTheme(prefs = prefs) {
                Surface(modifier = Modifier) {
                    NavGraph()
                }
            }
        }
    }
}
