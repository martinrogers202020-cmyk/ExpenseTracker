// FILE: app/src/main/java/com/example/expensetracker/ui/screens/SettingsScreen.kt
package com.example.expensetracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.datastore.ThemeMode
import com.example.expensetracker.ui.viewmodel.SettingsViewModel
import com.example.expensetracker.ui.viewmodel.SettingsViewModelFactory

@Composable
fun SettingsScreen(
    proEnabled: Boolean,
    onTogglePro: (Boolean) -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAdvancedReports: () -> Unit,
    onOpenCsvImport: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val prefs by vm.appearance.collectAsStateWithLifecycle()

    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val border = cs.outlineVariant
    val textPrimary = cs.onSurface
    val textSecondary = cs.onSurfaceVariant

    val bg = Brush.verticalGradient(listOf(cs.background, cs.background, cs.background))

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    ) { padding ->

        LazyColumnWithSpacing(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            bottomPadding = 110.dp
        ) {

            item { SectionHeader("Pro Mode", textPrimary) }

            item {
                ProCard(
                    proEnabled = proEnabled,
                    onTogglePro = onTogglePro,
                    onUpgradeClick = onOpenPaywall,
                    onRestoreClick = { /* TODO later */ }
                )
            }

            item { SectionHeader("Preferences", textPrimary) }

            item {
                SettingsItem(
                    icon = { Icon(Icons.Outlined.Notifications, contentDescription = null, tint = accent) },
                    title = "Notifications",
                    subtitle = "Daily reminder & budget alerts",
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant,
                    onClick = onOpenNotifications
                )
            }

            item {
                SettingsItem(
                    icon = { Icon(Icons.Outlined.Cloud, contentDescription = null, tint = accent) },
                    title = "Backup & restore",
                    subtitle = "Export / import using Drive or files",
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant,
                    onClick = onOpenBackupRestore
                )
            }

            item {
                SettingsItem(
                    icon = { Icon(Icons.Outlined.ShowChart, contentDescription = null, tint = accent) },
                    title = "Advanced reports",
                    subtitle = "Custom ranges + trends",
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant,
                    onClick = onOpenAdvancedReports
                )
            }

            item {
                SettingsItem(
                    icon = { Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = accent) },
                    title = "Import bank CSV",
                    subtitle = "Add withdrawals & deposits from a CSV file",
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant,
                    onClick = onOpenCsvImport
                )
            }

            item {
                AppearanceItem(
                    prefsThemeMode = prefs.themeMode,
                    prefsDynamicColor = prefs.dynamicColor,
                    onThemeMode = vm::setThemeMode,
                    onDynamicColor = vm::setDynamicColor,
                    accent = accent,
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant
                )
            }

            item { SectionHeader("Security", textPrimary) }

            item {
                SoftCard(border = border) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SecurityBadge(
                                text = "Good",
                                accent = accent,
                                borderColor = border,
                                textPrimary = textPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Security status", fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Text(
                                    "Your data stays on device. Add protection features below.",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Divider(color = border)

                        SecurityActionRow(
                            icon = { Icon(Icons.Outlined.Security, contentDescription = null, tint = accent) },
                            title = "Local data only",
                            subtitle = "Transactions are stored on this device.",
                            trailingText = "On",
                            borderColor = border,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = { }
                        )

                        Divider(color = border)

                        var appLockEnabled by rememberSaveable { mutableStateOf(false) }
                        SecurityToggleRow(
                            icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = accent) },
                            title = "App lock",
                            subtitle = "Require authentication to open the app.",
                            checked = appLockEnabled,
                            onCheckedChange = { appLockEnabled = it },
                            accent = accent,
                            borderColor = border,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )

                        AnimatedVisibility(visible = appLockEnabled) {
                            SecurityMiniCard(border = border) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Lock method", fontWeight = FontWeight.SemiBold, color = textPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(onClick = { }, label = { Text("Biometric") })
                                        AssistChip(onClick = { }, label = { Text("PIN") })
                                    }
                                    Text(
                                        "Wire later using BiometricPrompt / PIN screen.",
                                        color = textSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Divider(color = border)

                        SecurityActionRow(
                            icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = accent) },
                            title = "Privacy & permissions",
                            subtitle = "Review what the app can access.",
                            trailingText = "View",
                            borderColor = border,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = { }
                        )
                    }
                }
            }

            item { SectionHeader("About", textPrimary) }

            item {
                SettingsItem(
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = accent) },
                    title = "Version",
                    subtitle = "1.0.0",
                    borderColor = border,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    container = cs.surface,
                    pill = cs.surfaceVariant,
                    trailing = null,
                    onClick = {}
                )
            }
        }
    }
}

/* ---------------- Appearance ---------------- */

@Composable
private fun AppearanceItem(
    prefsThemeMode: ThemeMode,
    prefsDynamicColor: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    accent: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    container: Color,
    pill: Color
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val subtitle = buildString {
        append(
            when (prefsThemeMode) {
                ThemeMode.SYSTEM -> "System default"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }
        )
        if (prefsDynamicColor) append(" • Dynamic")
    }

    SettingsItem(
        icon = { Icon(Icons.Outlined.Palette, contentDescription = null, tint = accent) },
        title = "Appearance",
        subtitle = subtitle,
        borderColor = borderColor,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        container = container,
        pill = pill,
        trailing = {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = textSecondary
            )
        },
        onClick = { expanded = !expanded }
    )

    AnimatedVisibility(visible = expanded) {
        SoftCard(border = borderColor) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text("Theme", fontWeight = FontWeight.SemiBold, color = textPrimary)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = prefsThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = prefsThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = prefsThemeMode == ThemeMode.DARK,
                        onClick = { onThemeMode(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }

                Divider(color = borderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Dynamic colors", fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text(
                            "Uses system colors (Android 12+)",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked = prefsDynamicColor,
                        onCheckedChange = onDynamicColor,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = accent,
                            uncheckedTrackColor = accent.copy(alpha = 0.35f),
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

/* ---------------- shared UI helpers + Pro UI ---------------- */

@Composable
private fun SectionHeader(text: String, textPrimary: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = textPrimary
    )
}

@Composable
private fun SoftCard(border: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    container: Color,
    pill: Color,
    trailing: (@Composable () -> Unit)? = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = container,
        contentColor = textPrimary,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pill,
                border = BorderStroke(1.dp, borderColor),
                tonalElevation = 0.dp
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { icon() }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }
            }

            if (trailing != null) {
                CompositionLocalProvider(LocalContentColor provides textSecondary) { trailing() }
            }
        }
    }
}

@Composable
private fun ProCard(
    proEnabled: Boolean,
    onTogglePro: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val border = cs.outlineVariant

    SoftCard(border = border) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Pro",
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (proEnabled) "Active" else "Not active",
                        color = cs.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = proEnabled,
                    onCheckedChange = onTogglePro,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = accent,
                        checkedThumbColor = cs.surface,
                        uncheckedTrackColor = accent.copy(alpha = 0.35f),
                        uncheckedThumbColor = cs.surface
                    )
                )
            }

            Divider(color = border)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProFeatureRow("Advanced Reports (custom ranges, trends)")
                ProFeatureRow("Budgets + alerts (category limits)")
                ProFeatureRow("Export CSV / PDF statements")
                ProFeatureRow("Recurring transactions")
                ProFeatureRow("Backup & restore (Drive / cloud)")
            }

            Divider(color = border)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.weight(1f),
                    enabled = !proEnabled
                ) { Text(if (proEnabled) "Pro active" else "Upgrade") }

                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f)
                ) { Text("Restore") }
            }

            Text(
                "Replace the toggle with Google Play Billing later.",
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ProFeatureRow(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = cs.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, cs.outlineVariant)
        ) {
            Text(
                "✓",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = cs.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text = text, color = cs.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

/* ---------- Security UI ---------- */

@Composable
private fun SecurityBadge(
    text: String,
    accent: Color,
    borderColor: Color,
    textPrimary: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecurityActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailingText: String,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { icon() }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(subtitle, color = textSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    trailingText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { icon() }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(subtitle, color = textSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = accent.copy(alpha = 0.35f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun SecurityMiniCard(
    border: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, border),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
    }
}

/* ---------- LazyColumn wrapper ---------- */

@Composable
private fun LazyColumnWithSpacing(
    modifier: Modifier,
    bottomPadding: Dp,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = bottomPadding),
        content = content
    )
}
