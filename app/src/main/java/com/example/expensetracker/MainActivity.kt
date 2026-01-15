package com.example.expensetracker

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.metrics.performance.JankStats
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerAppTheme
import com.example.expensetracker.ui.viewmodel.SettingsViewModel
import com.example.expensetracker.ui.viewmodel.SettingsViewModelFactory

class MainActivity : AppCompatActivity() {
    private var jankStats: JankStats? = null
    private val logJankFrames: Boolean = BuildConfig.DEBUG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (logJankFrames) {
            jankStats = JankStats.createAndTrack(window) { frameData ->
                if (frameData.isJank) {
                    Log.d(
                        "JankStats",
                        "Jank frame: duration=${frameData.frameDurationUiNanos}ns " +
                            "state=${frameData.states}"
                    )
                }
            }
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

    override fun onDestroy() {
        jankStats?.isTrackingEnabled = false
        jankStats = null
        super.onDestroy()
    }
}
