package com.example.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.expensetracker.ui.state.CategoryIcon

fun categoryIcon(icon: CategoryIcon): ImageVector {
    return when (icon) {
        CategoryIcon.GROCERIES -> Icons.Outlined.ShoppingCart
        CategoryIcon.FOOD -> Icons.Outlined.Restaurant
        CategoryIcon.TRANSPORT -> Icons.Outlined.DirectionsBus
        CategoryIcon.RENT -> Icons.Outlined.Home
        CategoryIcon.SHOPPING -> Icons.Outlined.Store
        CategoryIcon.ENTERTAINMENT -> Icons.Outlined.Movie
        CategoryIcon.HEALTH -> Icons.Outlined.MedicalServices
        CategoryIcon.SUBSCRIPTIONS -> Icons.Outlined.Autorenew
        CategoryIcon.INCOME -> Icons.Outlined.AttachMoney
        CategoryIcon.OTHER -> Icons.Outlined.Category
    }
}
