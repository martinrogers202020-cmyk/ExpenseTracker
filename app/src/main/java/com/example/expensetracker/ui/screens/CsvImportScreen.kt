package com.example.expensetracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.ui.viewmodel.CsvImportViewModel
import com.example.expensetracker.ui.viewmodel.CsvImportViewModelFactory
import com.example.expensetracker.util.Formatters
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = DatabaseProvider.get(context)

    val vm: CsvImportViewModel = viewModel(
        factory = CsvImportViewModelFactory(context = context, db = db)
    )

    val s by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) vm.pickAndParse(uri)
    }

    val cs = MaterialTheme.colorScheme
    val border = cs.outlineVariant

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = cs.primary)
                        Text(
                            stringResource(R.string.settings_import_csv_title),
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.surface)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (s.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.csv_import_step_pick_file), fontWeight = FontWeight.SemiBold)

                    Button(
                        onClick = {
                            picker.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/vnd.ms-excel",
                                    "text/plain"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.csv_import_choose_csv))
                    }

                    val pickedName = s.pickedName
                    if (pickedName != null) {
                        Text(
                            stringResource(R.string.csv_import_selected_file, pickedName),
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.csv_import_step_category), fontWeight = FontWeight.SemiBold)

                    var expanded by rememberSaveable { mutableStateOf(false) }
                    val selected = s.categories.firstOrNull { it.id == s.selectedCategoryId }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, border),
                        color = cs.surfaceVariant.copy(alpha = 0.35f),
                        onClick = { expanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selected?.let { "${it.emoji}  ${it.name}" }
                                    ?: stringResource(R.string.csv_import_select_category),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(stringResource(R.string.action_change), color = cs.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        s.categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.emoji}  ${c.name}") },
                                onClick = {
                                    expanded = false
                                    vm.selectCategory(c.id)
                                }
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.csv_import_tip),
                        color = cs.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.csv_import_step_preview), fontWeight = FontWeight.SemiBold)

                    if (s.rows.isEmpty()) {
                        Text(stringResource(R.string.csv_import_no_rows), color = cs.onSurfaceVariant)
                    } else {
                        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
                        Text(
                            stringResource(
                                R.string.csv_import_range,
                                s.start?.format(fmt) ?: "",
                                s.end?.format(fmt) ?: ""
                            ),
                            color = cs.onSurfaceVariant
                        )
                        Text(stringResource(R.string.csv_import_rows, s.rows.size))
                        Text(stringResource(R.string.csv_import_will_import, s.willImportCount))
                        if (s.duplicatesDetected > 0) {
                            Text(
                                stringResource(R.string.csv_import_duplicates_skipped, s.duplicatesDetected),
                                color = cs.primary
                            )
                        }

                        Divider(color = border)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatPill(
                                stringResource(R.string.label_income),
                                Formatters.money(s.previewIncomeCents),
                                border,
                                Modifier.weight(1f)
                            )
                            StatPill(
                                stringResource(R.string.label_expense),
                                Formatters.money(s.previewExpenseCents),
                                border,
                                Modifier.weight(1f)
                            )
                        }

                        Divider(color = border)

                        Text(stringResource(R.string.csv_import_sample_rows), fontWeight = FontWeight.SemiBold)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.rows.take(15)) { r ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, border),
                                    color = cs.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(r.date.toString(), fontWeight = FontWeight.SemiBold)
                                            Text(
                                                r.description,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        val money = Formatters.money(abs(r.amountCentsAbs))
                                        Text(money, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (s.warnings.isNotEmpty()) {
                        Divider(color = border)
                        Text(stringResource(R.string.csv_import_warnings_title), fontWeight = FontWeight.SemiBold)
                        s.warnings.forEach { w ->
                            Text(
                                stringResource(R.string.csv_import_warning_item, w),
                                color = cs.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    val error = s.error
                    if (error != null) {
                        Divider(color = border)
                        Text(
                            stringResource(R.string.csv_import_error, error),
                            color = cs.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val importedCount = s.importedCount
                    if (importedCount != null) {
                        Divider(color = border)
                        Text(
                            stringResource(R.string.csv_import_imported, importedCount),
                            color = cs.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { vm.importNow() },
                        enabled = !s.loading && s.rows.isNotEmpty() && (s.selectedCategoryId != null) && s.willImportCount > 0
                    ) {
                        Text(stringResource(R.string.action_import_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    border: androidx.compose.ui.graphics.Color,
    modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border),
        color = cs.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}
