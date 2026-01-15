package com.example.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.R
import com.example.expensetracker.ui.viewmodel.NotificationsViewModel
import com.example.expensetracker.ui.viewmodel.NotificationsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val vm: NotificationsViewModel = viewModel(factory = NotificationsViewModelFactory(context))
    val prefs by vm.state.collectAsStateWithLifecycle()

    val bg = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFFF7F4FF), Color(0xFFF2EEFF)))
    val accent = Color(0xFF7B5CFF)
    val border = Color(0xFFE4DBFF)
    val textPrimary = Color(0xFF2C2746)
    val textSecondary = Color(0xFF8B84A8)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_notifications),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            SoftCard(border = border) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.notifications_enable_title),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.notifications_enable_subtitle),
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = prefs.enabled,
                        onCheckedChange = vm::setEnabled,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = accent.copy(alpha = 0.35f),
                            checkedThumbColor = accent
                        )
                    )
                }
            }

            SoftCard(border = border) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.notifications_reminder_time_title),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.notifications_reminder_time_subtitle),
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    TimePickRow(
                        enabled = prefs.enabled,
                        hour = prefs.hour,
                        minute = prefs.minute,
                        onTimeChange = vm::setTime,
                        accent = accent,
                        border = border,
                        textPrimary = textPrimary
                    )
                }
            }

            SoftCard(border = border) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.notifications_types_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                    ToggleRow(
                        title = stringResource(R.string.notifications_daily_title),
                        subtitle = stringResource(R.string.notifications_daily_subtitle),
                        checked = prefs.dailyReminder,
                        enabled = prefs.enabled,
                        onCheckedChange = vm::setDailyReminder,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    Divider(color = border)

                    ToggleRow(
                        title = stringResource(R.string.notifications_budget_title),
                        subtitle = stringResource(R.string.notifications_budget_subtitle),
                        checked = prefs.budgetAlerts,
                        enabled = prefs.enabled,
                        onCheckedChange = vm::setBudgetAlerts,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftCard(border: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) textPrimary else textSecondary)
            Text(subtitle, color = textSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent.copy(alpha = 0.35f),
                checkedThumbColor = accent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickRow(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    accent: Color,
    border: Color,
    textPrimary: Color
) {
    var hourExpanded by rememberSaveable { mutableStateOf(false) }
    var minExpanded by rememberSaveable { mutableStateOf(false) }

    val hours = remember { (0..23).toList() }
    val mins = remember { (0..59 step 5).toList() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = hourExpanded,
            onExpandedChange = { if (enabled) hourExpanded = !hourExpanded }
        ) {
            OutlinedTextField(
                value = hour.toString().padStart(2, '0'),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(stringResource(R.string.notifications_hour_label)) },
                modifier = Modifier.weight(1f).menuAnchor(),
                shape = RoundedCornerShape(18.dp)
            )
            ExposedDropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                hours.forEach { h ->
                    DropdownMenuItem(
                        text = { Text(h.toString().padStart(2, '0')) },
                        onClick = {
                            onTimeChange(h, minute)
                            hourExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = minExpanded,
            onExpandedChange = { if (enabled) minExpanded = !minExpanded }
        ) {
            OutlinedTextField(
                value = minute.toString().padStart(2, '0'),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(stringResource(R.string.notifications_min_label)) },
                modifier = Modifier.weight(1f).menuAnchor(),
                shape = RoundedCornerShape(18.dp)
            )
            ExposedDropdownMenu(expanded = minExpanded, onDismissRequest = { minExpanded = false }) {
                mins.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.toString().padStart(2, '0')) },
                        onClick = {
                            onTimeChange(hour, m)
                            minExpanded = false
                        }
                    )
                }
            }
        }

        Surface(
            color = accent.copy(alpha = if (enabled) 0.14f else 0.08f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.20f))
        ) {
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
