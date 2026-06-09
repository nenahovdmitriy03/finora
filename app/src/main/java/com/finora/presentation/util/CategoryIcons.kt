package com.finora.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

private val iconMap: Map<String, ImageVector> = mapOf(
    "cart" to Icons.Rounded.ShoppingCart,
    "restaurant" to Icons.Rounded.Restaurant,
    "car" to Icons.Rounded.DirectionsCar,
    "home" to Icons.Rounded.Home,
    "health" to Icons.Rounded.LocalHospital,
    "movie" to Icons.Rounded.Movie,
    "shopping" to Icons.Rounded.ShoppingBag,
    "phone" to Icons.Rounded.Smartphone,
    "school" to Icons.Rounded.School,
    "flight" to Icons.Rounded.Flight,
    "subscription" to Icons.Rounded.Subscriptions,
    "category" to Icons.Rounded.Category,
    "salary" to Icons.Rounded.Payments,
    "work" to Icons.Rounded.Work,
    "gift" to Icons.Rounded.CardGiftcard,
    "invest" to Icons.Rounded.TrendingUp,
    "wallet" to Icons.Rounded.AccountBalanceWallet,
    "target" to Icons.Rounded.TrackChanges,
    "card" to Icons.Rounded.CreditCard,
    "cash" to Icons.Rounded.Payments,
    "savings" to Icons.Rounded.Savings,
    "bank" to Icons.Rounded.AccountBalance,
    "games" to Icons.Rounded.SportsEsports,
    "pets" to Icons.Rounded.Pets,
    "bills" to Icons.Rounded.Calculate
)

fun iconForKey(key: String): ImageVector = iconMap[key] ?: Icons.Rounded.Category

/** Keys offered when creating a custom category. */
val expenseIconKeys = listOf(
    "cart", "restaurant", "car", "home", "health", "movie",
    "shopping", "phone", "school", "flight", "subscription",
    "games", "pets", "bills", "category"
)

val incomeIconKeys = listOf(
    "salary", "work", "gift", "invest", "savings", "category"
)

/** Keys offered when creating an account. */
val accountIconKeys = listOf("card", "cash", "wallet", "savings", "bank")
