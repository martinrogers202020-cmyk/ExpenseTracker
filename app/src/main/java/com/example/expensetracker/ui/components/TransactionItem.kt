package com.example.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetracker.ui.design.Dimens
import com.example.expensetracker.ui.design.AppShapes

@Composable
fun TransactionItem(
    emoji: String,
    title: String,
    note: String,
    amount: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = AppShapes.Card
    ) {
        Row(
            modifier = Modifier.padding(Dimens.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("$emoji $title", fontWeight = FontWeight.SemiBold)
                if (note.isNotBlank()) {
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(amount, fontWeight = FontWeight.SemiBold)
        }
    }
}
