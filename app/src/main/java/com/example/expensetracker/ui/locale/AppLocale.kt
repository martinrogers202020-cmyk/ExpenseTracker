package com.example.expensetracker.ui.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

data class AppLocaleState(
    val locale: Locale,
    val languageTag: String
)

val LocalAppLocale = staticCompositionLocalOf {
    val fallback = Locale.getDefault()
    AppLocaleState(locale = fallback, languageTag = fallback.toLanguageTag())
}

@Composable
fun ProvideAppLocale(
    languageTag: String,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val resolvedTag = languageTag.ifBlank { Locale.getDefault().toLanguageTag() }
    val locale = remember(resolvedTag) { Locale.forLanguageTag(resolvedTag) }
    val configuration = remember(locale, baseConfiguration) {
        Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            }
        }
    }
    val localizedContext = remember(locale, baseConfiguration) {
        baseContext.createConfigurationContext(configuration)
    }

    SideEffect {
        Locale.setDefault(locale)
    }

    CompositionLocalProvider(
        LocalAppLocale provides AppLocaleState(locale = locale, languageTag = resolvedTag),
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
        content = content
    )
}

fun Context.withAppLocale(languageTag: String): Context {
    val resolvedTag = languageTag.ifBlank { Locale.getDefault().toLanguageTag() }
    val locale = Locale.forLanguageTag(resolvedTag)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setLocales(LocaleList(locale))
        }
    }
    return createConfigurationContext(configuration)
}
