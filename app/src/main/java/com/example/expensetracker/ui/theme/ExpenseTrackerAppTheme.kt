package com.example.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.expensetracker.data.datastore.AccentChoice
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.data.datastore.ThemeMode
import com.example.expensetracker.ui.locale.ProvideAppLocale

@Composable
fun ExpenseTrackerAppTheme(
    prefs: AppearancePrefs,
    content: @Composable () -> Unit
) {
    ProvideAppLocale(languageTag = prefs.languageTag) {
        val darkTheme = when (prefs.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }

        val context = LocalContext.current
        val dynamic = prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val colorScheme = when {
            dynamic && darkTheme -> dynamicDarkColorScheme(context)
            dynamic && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> appDarkScheme(prefs.accentChoice)
            else -> appLightScheme(prefs.accentChoice)
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // or AppTypography if you have it
            content = content
        )
    }
}

private fun accentColor(choice: AccentChoice): Color {
    return when (choice) {
        AccentChoice.PURPLE -> ProPurple
        AccentChoice.BLUE -> BrandPrimary
        AccentChoice.PINK -> Color(0xFFEC4899)
        AccentChoice.GREEN -> Color(0xFF22C55E)
    }
}

private fun appLightScheme(accentChoice: AccentChoice) = lightColorScheme(
    primary = accentColor(accentChoice),
    onPrimary = Color.White,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = CardBackground,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,

    outlineVariant = DividerLight,

    error = ExpenseRed,
    onError = Color.White
)

private fun appDarkScheme(accentChoice: AccentChoice) = darkColorScheme(
    primary = accentColor(accentChoice),
    onPrimary = Color.Black,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,

    outlineVariant = DarkDivider,

    error = DarkExpenseRed,
    onError = Color.Black
)
