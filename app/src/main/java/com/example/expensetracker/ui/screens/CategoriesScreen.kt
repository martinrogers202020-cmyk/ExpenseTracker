package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ui.viewmodel.CategoriesViewModel
import com.example.expensetracker.ui.viewmodel.CategoriesViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModelFactory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(state.categories, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) state.categories
        else state.categories.filter {
            it.name.lowercase().contains(q) || it.emoji.contains(q)
        }
    }

    val bg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFFF7F4FF),
            Color(0xFFF2EEFF)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF7B5CFF),
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // search bar (rounded, soft)
            SearchPill(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search categories"
            )

            Spacer(Modifier.height(14.dp))

            // error banner (if any)
            state.error?.let { msg ->
                SoftCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = vm::clearError) { Text("OK") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (filtered.isEmpty()) {
                SoftCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("No categories yet", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap + to add a category.",
                            color = Color(0xFF6F6A86)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filtered, key = { it.id }) { cat ->
                        CategoryGlassRow(
                            emoji = cat.emoji,
                            name = cat.name,
                            isDefault = cat.isDefault,
                            onEdit = { editId = cat.id },
                            onDelete = { vm.deleteCategory(cat.id) }
                        )
                    }
                }
            }
        }
    }

    // ADD
    if (showAddDialog) {
        CategoryDialog(
            title = "Add Category",
            initialName = "",
            initialEmoji = "🏷️",
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji ->
                vm.addCategory(name.trim(), emoji.trim().ifBlank { "🏷️" })
                showAddDialog = false
            }
        )
    }

    // EDIT
    val editCat = state.categories.firstOrNull { it.id == editId }
    if (editCat != null) {
        CategoryDialog(
            title = "Edit Category",
            initialName = editCat.name,
            initialEmoji = editCat.emoji,
            onDismiss = { editId = null },
            onSave = { name, emoji ->
                vm.updateCategory(editCat.id, name.trim(), emoji.trim().ifBlank { "🏷️" })
                editId = null
            }
        )
    }
}

@Composable
private fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val shape = RoundedCornerShape(22.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text(placeholder, color = Color(0xFF7A7394)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF7A7394)) },
        singleLine = true,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFFFFFFF),
            unfocusedContainerColor = Color(0xFFFFFFFF),
            focusedBorderColor = Color(0xFFE3DAFF),
            unfocusedBorderColor = Color(0xFFE3DAFF),
            cursorColor = Color(0xFF7B5CFF)
        )
    )
}

@Composable
private fun SoftCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFFFF),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun CategoryGlassRow(
    emoji: String,
    name: String,
    isDefault: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFFFFFFF),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // big emoji "icon"
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C2746))
                Spacer(Modifier.height(2.dp))
                if (isDefault) {
                    Text(text = "Default", color = Color(0xFF8B84A8))
                }
            }

            // action pill (edit | delete)
            ActionPill(
                leftIcon = Icons.Outlined.Edit,
                rightIcon = Icons.Outlined.DeleteOutline,
                onLeft = onEdit,
                onRight = onDelete
            )
        }
    }
}

@Composable
private fun ActionPill(
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    val pillShape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(Color(0xFFF5F1FF))
            .border(1.dp, Color(0xFFE4DBFF), pillShape)
            .height(46.dp)
            .width(110.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onLeft,
            modifier = Modifier.weight(1f)
        ) {
            Icon(leftIcon, contentDescription = "Edit", tint = Color(0xFF6A4DFF))
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFFE4DBFF))
        )

        IconButton(
            onClick = onRight,
            modifier = Modifier.weight(1f)
        ) {
            Icon(rightIcon, contentDescription = "Delete", tint = Color(0xFF6A4DFF))
        }
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("Emoji") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, emoji) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
