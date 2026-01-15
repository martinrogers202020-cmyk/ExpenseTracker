package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetracker.R
import com.example.expensetracker.ui.design.Dimens
import com.example.expensetracker.ui.design.AppShapes

@Composable
fun AppSummaryCard(
    income: String,
    expense: String,
    balance: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Card
    ) {
        Column(
            modifier = Modifier.padding(Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            Text(stringResource(R.string.label_income), style = MaterialTheme.typography.labelMedium)
            Text(income, fontWeight = FontWeight.SemiBold)

            Divider()

            Text(stringResource(R.string.label_expenses), style = MaterialTheme.typography.labelMedium)
            Text(expense, fontWeight = FontWeight.SemiBold)

            Divider()

            Text(stringResource(R.string.label_balance), style = MaterialTheme.typography.labelMedium)
            Text(balance, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
