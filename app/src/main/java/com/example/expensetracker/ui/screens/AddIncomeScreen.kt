package com.example.expensetracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.ui.viewmodel.AddTransactionViewModel
import com.example.expensetracker.ui.viewmodel.AddTransactionViewModelFactory
import java.time.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(onBack: () -> Unit) {
    AddTransactionScreen(
        screenTitle = "Add Income",
        isIncome = true,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(onBack: () -> Unit) {
    AddTransactionScreen(
        screenTitle = "Add Expense",
        isIncome = false,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionScreen(
    screenTitle: String,
    isIncome: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val vm: AddTransactionViewModel = viewModel(factory = AddTransactionViewModelFactory(context))

    val categories by vm.categories.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var attachmentUri by rememberSaveable { mutableStateOf<String?>(null) }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> attachmentUri = uri?.toString() }

    LaunchedEffect(saved) {
        if (saved) {
            vm.clearSaved()
            onBack()
        }
    }

    val purple = Color(0xFF7B5CFF)
    val fieldBg = Color(0xFFF3F3F6)
    val textPrimary = Color(0xFF2C2746)
    val textSecondary = Color(0xFF8B84A8)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            RoundedField(
                label = "Amount",
                value = amount,
                onValueChange = { amount = it },
                bg = fieldBg,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            SimpleDropdownField(
                label = "Date",
                bg = fieldBg,
                valueText = date,
                options = remember { (0..31).map { LocalDate.now().minusDays(it.toLong()).toString() } },
                optionText = { it },
                onPick = { picked -> date = picked },
            )

            SimpleDropdownField(
                label = "Category",
                bg = fieldBg,
                valueText = categories.firstOrNull { it.id == categoryId }?.name ?: "Select",
                options = categories,
                optionText = { c: CategoryEntity -> c.name },
                onPick = { picked: CategoryEntity -> categoryId = picked.id },
            )
            Text(
                text = "Categories loaded: ${categories.size}",
                color = Color.Red
            )

            RoundedField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                bg = fieldBg,
                keyboardOptions = KeyboardOptions.Default,
                minLines = 3
            )

            AttachmentRow(
                label = "Add attachment",
                value = attachmentUri,
                onPick = { attachmentPicker.launch("*/*") },
                bg = fieldBg,
                iconTint = purple,
                textSecondary = textSecondary
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (isIncome) {
                        vm.saveIncome(amount, date, categoryId, notes, attachmentUri)
                    } else {
                        vm.saveExpense(amount, date, categoryId, notes, attachmentUri)
                    }
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF6A4DFF), Color(0xFF7B5CFF))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (saving) "Saving..." else "Save",
                        color = MaterialTheme.colorScheme.surface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    bg: Color,
    keyboardOptions: KeyboardOptions,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = bg,
            unfocusedContainerColor = bg,
            disabledContainerColor = bg,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun AttachmentRow(
    label: String,
    value: String?,
    onPick: () -> Unit,
    bg: Color,
    iconTint: Color,
    textSecondary: Color
) {
    Surface(
        onClick = onPick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = iconTint)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = value ?: "Tap to choose a file",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SimpleDropdownField(
    label: String,
    bg: Color,
    valueText: String,
    options: List<T>,
    optionText: (T) -> String,
    onPick: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),   // ✅ REQUIRED for ExposedDropdownMenuBox
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = bg,
                unfocusedContainerColor = bg,
                disabledContainerColor = bg,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(optionText(item)) },
                    onClick = {
                        onPick(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
