// FILE: app/src/main/java/com/example/expensetracker/ui/screens/AdvancedReportsScreen.kt
package com.example.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.ui.viewmodel.AdvancedReportsViewModel
import com.example.expensetracker.ui.viewmodel.AdvancedReportsViewModelFactory
import java.time.LocalDate
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedReportsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = DatabaseProvider.get(context)

    val vm: AdvancedReportsViewModel = viewModel(
        factory = AdvancedReportsViewModelFactory(db = db)
    )

    val s by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    val cs = MaterialTheme.colorScheme
    val border = cs.outlineVariant
    val start = LocalDate.ofEpochDay(s.startEpochDay)
    val end = LocalDate.ofEpochDay(s.endEpochDay)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Insights, contentDescription = null, tint = cs.primary)
                        Text(
                            text = stringResource(R.string.settings_advanced_reports_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cs.surface
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                RangeCard(
                    start = start,
                    end = end,
                    border = border,
                    onRange7d = {
                        val e = s.endEpochDay
                        vm.setRange(e - 6, e)
                    },
                    onRange30d = {
                        val e = s.endEpochDay
                        vm.setRange(e - 29, e)
                    }
                )
            }

            item {
                TotalsCard(
                    incomeCents = s.totalIncomeCents,
                    expenseCents = s.totalExpenseCents,
                    netCents = s.netCents,
                    border = border
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.advanced_reports_trend_title),
                    subtitle = stringResource(R.string.advanced_reports_trend_subtitle),
                    border = border
                )
            }

            items(s.trend) { p ->
                TrendRow(
                    epochDay = p.epochDay,
                    incomeCents = p.incomeCents,
                    expenseCents = p.expenseCents,
                    netCents = p.netCents,
                    border = border
                )
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/* ---------------- UI blocks ---------------- */

@Composable
private fun RangeCard(
    start: LocalDate,
    end: LocalDate,
    border: androidx.compose.ui.graphics.Color,
    onRange7d: () -> Unit,
    onRange30d: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cs.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, border)
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cs.primary)
                    }
                }

                // NO Modifier.weight() here to avoid RowScope import issues across versions.
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.advanced_reports_range_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.advanced_reports_range_value, start, end),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Divider(color = border)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ElevatedFilterChip(
                    selected = false,
                    onClick = onRange7d,
                    label = { Text(stringResource(R.string.advanced_reports_range_last_7)) }
                )
                ElevatedFilterChip(
                    selected = false,
                    onClick = onRange30d,
                    label = { Text(stringResource(R.string.advanced_reports_range_last_30)) }
                )
            }

            Text(
                stringResource(R.string.advanced_reports_range_tip),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TotalsCard(
    incomeCents: Long,
    expenseCents: Long,
    netCents: Long,
    border: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.advanced_reports_totals_title), fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HalfWidthMetricPill(title = stringResource(R.string.label_income), value = incomeCents.toString(), border = border)
                HalfWidthMetricPill(title = stringResource(R.string.label_expense), value = expenseCents.toString(), border = border)
            }

            Divider(color = border)

            val netLabel = if (netCents >= 0) {
                stringResource(R.string.advanced_reports_net_positive)
            } else {
                stringResource(R.string.advanced_reports_net_negative)
            }
            MetricPill(
                title = netLabel,
                value = abs(netCents).toString(),
                border = border,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RowScope.HalfWidthMetricPill(
    title: String,
    value: String,
    border: androidx.compose.ui.graphics.Color
) {
    // weight is only callable INSIDE RowScope, so this is guaranteed to compile.
    MetricPill(
        title = title,
        value = value,
        border = border,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    border: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp)),
        color = cs.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    border: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Divider(color = border)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.advanced_reports_chip_daily)) })
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.advanced_reports_chip_income_vs_expense)) })
            }
        }
    }
}

@Composable
private fun TrendRow(
    epochDay: Long,
    incomeCents: Long,
    expenseCents: Long,
    netCents: Long,
    border: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme
    val date = LocalDate.ofEpochDay(epochDay).toString()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(date, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.advanced_reports_income_expense_row, incomeCents, expenseCents),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            val netText = if (netCents >= 0) "+$netCents" else netCents.toString()

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = cs.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, border),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = stringResource(R.string.advanced_reports_net_value, netText),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = cs.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
