package com.finora.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Auth : Destination("auth")
    data object Onboarding : Destination("onboarding")
    data object Home : Destination("home")
    data object Transactions : Destination("transactions")
    data object Analytics : Destination("analytics")
    data object Goals : Destination("goals")
    data object Settings : Destination("settings")
    data object Accounts : Destination("accounts")
    data object AiChat : Destination("ai_chat")

    data object RecurringRules : Destination("recurring_rules")
    data object TaxDeduction : Destination("tax_deduction")
    data object Budgets : Destination("budgets")
    data object Templates : Destination("templates")
    data object Challenges : Destination("challenges")
    data object AddTransaction : Destination("add_transaction?id={id}") {
        fun create(id: Long = -1L) = "add_transaction?id=$id"
        const val ARG_ID = "id"
    }
}

data class BottomItem(
    val destination: Destination,
    val label: String,
    val icon: ImageVector
)

val bottomItems = listOf(
    BottomItem(Destination.Home, "Главная", Icons.Rounded.Home),
    BottomItem(Destination.Transactions, "Операции", Icons.Rounded.SwapVert),
    BottomItem(Destination.Analytics, "Аналитика", Icons.Rounded.Analytics),
    BottomItem(Destination.Goals, "Цели", Icons.Rounded.TrackChanges),
    BottomItem(Destination.Settings, "Настройки", Icons.Rounded.Settings)
)
