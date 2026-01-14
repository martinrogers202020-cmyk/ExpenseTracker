package com.example.expensetracker.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object NavAnimations {

    fun enter(): EnterTransition =
        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()

    fun exit(): ExitTransition =
        slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeOut()

    fun popEnter(): EnterTransition =
        slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeIn()

    fun popExit(): ExitTransition =
        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
}
