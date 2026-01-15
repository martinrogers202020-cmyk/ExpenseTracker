package com.example.expensetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.MerchantRuleEntity
import com.example.expensetracker.ui.viewmodel.MerchantRulesViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantRulesScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { DatabaseProvider.get(context) }

    val vm: MerchantRulesViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MerchantRulesViewModel(db) as T
        }
    })

    val state by vm.state.collectAsStateWithLifecycle()

    var search by rememberSaveable { mutableStateOf("") }

    val filteredRules = remember(state.rules, search) {
        val q = search.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) state.rules
        else state.rules.filter { rule ->
            rule.pattern.lowercase(Locale.ROOT).contains(q) ||
                    rule.matchType.lowercase(Locale.ROOT).contains(q)
        }
    }

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingRuleId by rememberSaveable { mutableStateOf<Long?>(null) }

    val editingRule = remember(state.rules, editingRuleId) {
        state.rules.firstOrNull { it.id == editingRuleId }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Rule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.merchant_rules_title), fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRuleId = null
                showEditor = true
            }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(stringResource(R.string.action_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.merchant_rules_tip_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.merchant_rules_tip_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        categories = state.categories,
                        onToggle = { enabled -> vm.toggle(rule, enabled) },
                        onEdit = {
                            editingRuleId = rule.id
                            showEditor = true
                        },
                        onDelete = { vm.delete(rule.id) }
                    )
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showEditor) {
        RuleEditorDialog(
            categories = state.categories,
            initial = editingRule,
            onDismiss = { showEditor = false },
            onSave = { vm.upsert(it); showEditor = false }
        )
    }
}

@Composable
private fun RuleCard(
    rule: MerchantRuleEntity,
    categories: List<CategoryEntity>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val border = MaterialTheme.colorScheme.outlineVariant
    val categoryName = categories.firstOrNull { it.id == rule.categoryId }?.name
        ?: stringResource(R.string.merchant_rules_unknown_category)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        rule.pattern,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(
                            R.string.merchant_rules_summary,
                            matchTypeLabel(rule.matchType),
                            categoryName,
                            rule.priority
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_edit))
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditorDialog(
    categories: List<CategoryEntity>,
    initial: MerchantRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (MerchantRuleEntity) -> Unit
) {
    val matchTypes = listOf(
        MatchTypeOption("CONTAINS", stringResource(R.string.merchant_rules_match_contains)),
        MatchTypeOption("STARTS_WITH", stringResource(R.string.merchant_rules_match_starts_with)),
        MatchTypeOption("REGEX", stringResource(R.string.merchant_rules_match_regex))
    )

    var pattern by rememberSaveable { mutableStateOf(initial?.pattern ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial?.enabled ?: true) }
    var priorityText by rememberSaveable { mutableStateOf((initial?.priority ?: 0).toString()) }

    var matchType by rememberSaveable {
        mutableStateOf((initial?.matchType ?: "CONTAINS").uppercase(Locale.ROOT))
    }
    var selectedCategoryId by rememberSaveable {
        mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    stringResource(R.string.merchant_rules_add_title)
                } else {
                    stringResource(R.string.merchant_rules_edit_title)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(R.string.merchant_rules_pattern_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Match type dropdown
                var mtExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = mtExpanded,
                    onExpandedChange = { mtExpanded = !mtExpanded }
                ) {
                    OutlinedTextField(
                        value = matchTypeLabel(matchType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.merchant_rules_match_type_label)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = mtExpanded,
                        onDismissRequest = { mtExpanded = false }
                    ) {
                        matchTypes.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    matchType = option.value
                                    mtExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category dropdown
                var catExpanded by remember { mutableStateOf(false) }
                val selectedName = categories.firstOrNull { it.id == selectedCategoryId }
                    ?.let { "${it.emoji} ${it.name}" }
                    ?: stringResource(R.string.action_select)

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_category)) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.emoji} ${c.name}") },
                                onClick = {
                                    selectedCategoryId = c.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priorityText,
                    onValueChange = { priorityText = it },
                    label = { Text(stringResource(R.string.merchant_rules_priority_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.merchant_rules_enabled_label), modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = pattern.trim()
                if (p.isBlank()) return@Button
                val prio = priorityText.toIntOrNull() ?: 0

                onSave(
                    MerchantRuleEntity(
                        id = initial?.id ?: 0L,
                        pattern = p,
                        matchType = matchType,
                        categoryId = selectedCategoryId,
                        priority = prio,
                        enabled = enabled,
                        createdAtEpochMs = initial?.createdAtEpochMs ?: System.currentTimeMillis()
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private data class MatchTypeOption(
    val value: String,
    val label: String
)

@Composable
private fun matchTypeLabel(value: String): String =
    when (value.uppercase(Locale.ROOT)) {
        "CONTAINS" -> stringResource(R.string.merchant_rules_match_contains)
        "STARTS_WITH" -> stringResource(R.string.merchant_rules_match_starts_with)
        "REGEX" -> stringResource(R.string.merchant_rules_match_regex)
        else -> value
    }
