package com.example.expensetracker

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
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
                val normalizedTag = prefs.languageTag.ifBlank { "en" }
                val didChange = applyAppLanguage(context, normalizedTag)
                if (didChange) {
                    (context as? Activity)?.recreate()
                }
            }

            ExpenseTrackerAppTheme(prefs = prefs) {
                Surface(modifier = Modifier) {
                    NavGraph()
                }
            }
        }
    }
}

private fun applyAppLanguage(context: Context, languageTag: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val newLocales = LocaleList.forLanguageTags(languageTag)
        val currentLocales = localeManager.applicationLocales
        if (currentLocales.toLanguageTags() != newLocales.toLanguageTags()) {
            localeManager.applicationLocales = newLocales
            true
        } else {
            false
        }
    } else {
        val newLocales = LocaleListCompat.forLanguageTags(languageTag)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (currentLocales.toLanguageTags() != newLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(newLocales)
            true
        } else {
            false
        }
    }
}
