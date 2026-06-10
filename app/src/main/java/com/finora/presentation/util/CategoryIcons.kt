package com.finora.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Liquor
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Central icon registry. Keys are stored in the DB (category/account.iconKey);
 * keep keys stable, you may freely add new ones.
 */
private val iconMap: Map<String, ImageVector> = mapOf(
    // Food & drink
    "cart" to Icons.Rounded.ShoppingCart,
    "grocery" to Icons.Rounded.LocalGroceryStore,
    "restaurant" to Icons.Rounded.Restaurant,
    "fastfood" to Icons.Rounded.Fastfood,
    "pizza" to Icons.Rounded.LocalPizza,
    "cafe" to Icons.Rounded.LocalCafe,
    "bar" to Icons.Rounded.LocalBar,
    "liquor" to Icons.Rounded.Liquor,
    "cake" to Icons.Rounded.Cake,
    "icecream" to Icons.Rounded.Icecream,
    // Transport
    "car" to Icons.Rounded.DirectionsCar,
    "taxi" to Icons.Rounded.LocalTaxi,
    "bus" to Icons.Rounded.DirectionsBus,
    "bike" to Icons.Rounded.DirectionsBike,
    "boat" to Icons.Rounded.DirectionsBoat,
    "fuel" to Icons.Rounded.LocalGasStation,
    "flight" to Icons.Rounded.Flight,
    // Home & utilities
    "home" to Icons.Rounded.Home,
    "light" to Icons.Rounded.Lightbulb,
    "power" to Icons.Rounded.Bolt,
    "water" to Icons.Rounded.WaterDrop,
    "wifi" to Icons.Rounded.Wifi,
    "furniture" to Icons.Rounded.Chair,
    "cleaning" to Icons.Rounded.CleaningServices,
    "repair" to Icons.Rounded.Construction,
    // Health & beauty
    "health" to Icons.Rounded.LocalHospital,
    "medical" to Icons.Rounded.MedicalServices,
    "medication" to Icons.Rounded.Medication,
    "vaccine" to Icons.Rounded.Vaccines,
    "fitness" to Icons.Rounded.FitnessCenter,
    "spa" to Icons.Rounded.Spa,
    "beauty" to Icons.Rounded.ContentCut,
    "brush" to Icons.Rounded.Brush,
    // Entertainment & sport
    "movie" to Icons.Rounded.Movie,
    "games" to Icons.Rounded.SportsEsports,
    "music" to Icons.Rounded.MusicNote,
    "headphones" to Icons.Rounded.Headphones,
    "soccer" to Icons.Rounded.SportsSoccer,
    "basketball" to Icons.Rounded.SportsBasketball,
    "casino" to Icons.Rounded.Casino,
    "nightlife" to Icons.Rounded.Nightlife,
    "party" to Icons.Rounded.Celebration,
    // Shopping & personal
    "shopping" to Icons.Rounded.ShoppingBag,
    "clothes" to Icons.Rounded.Checkroom,
    "jewelry" to Icons.Rounded.Diamond,
    // Tech & comm
    "phone" to Icons.Rounded.Smartphone,
    "computer" to Icons.Rounded.Computer,
    "tv" to Icons.Rounded.Tv,
    "camera" to Icons.Rounded.CameraAlt,
    "subscription" to Icons.Rounded.Subscriptions,
    // Education
    "school" to Icons.Rounded.School,
    "book" to Icons.Rounded.MenuBook,
    "science" to Icons.Rounded.Science,
    // Travel
    "luggage" to Icons.Rounded.Luggage,
    "beach" to Icons.Rounded.BeachAccess,
    "hotel" to Icons.Rounded.Hotel,
    "map" to Icons.Rounded.Map,
    "world" to Icons.Rounded.Public,
    // Family & misc
    "pets" to Icons.Rounded.Pets,
    "kids" to Icons.Rounded.ChildCare,
    "toys" to Icons.Rounded.Toys,
    "gift" to Icons.Rounded.CardGiftcard,
    "star" to Icons.Rounded.Star,
    "bills" to Icons.Rounded.Calculate,
    "receipt" to Icons.Rounded.Receipt,
    "category" to Icons.Rounded.Category,
    // Income & finance
    "salary" to Icons.Rounded.Payments,
    "work" to Icons.Rounded.Work,
    "business" to Icons.Rounded.BusinessCenter,
    "invest" to Icons.Rounded.TrendingUp,
    "money" to Icons.Rounded.AttachMoney,
    "coin" to Icons.Rounded.MonetizationOn,
    "paid" to Icons.Rounded.Paid,
    "exchange" to Icons.Rounded.CurrencyExchange,
    "percent" to Icons.Rounded.Percent,
    "award" to Icons.Rounded.EmojiEvents,
    "deal" to Icons.Rounded.Handshake,
    "redeem" to Icons.Rounded.Redeem,
    "target" to Icons.Rounded.TrackChanges,
    // Accounts
    "card" to Icons.Rounded.CreditCard,
    "cash" to Icons.Rounded.Payments,
    "wallet" to Icons.Rounded.AccountBalanceWallet,
    "savings" to Icons.Rounded.Savings,
    "bank" to Icons.Rounded.AccountBalance
)

fun iconForKey(key: String): ImageVector = iconMap[key] ?: Icons.Rounded.Category

/** Keys offered when creating a custom expense category. */
val expenseIconKeys = listOf(
    "cart", "grocery", "restaurant", "fastfood", "pizza", "cafe", "bar", "liquor", "cake", "icecream",
    "car", "taxi", "bus", "bike", "boat", "fuel", "flight",
    "home", "light", "power", "water", "wifi", "furniture", "cleaning", "repair",
    "health", "medical", "medication", "vaccine", "fitness", "spa", "beauty", "brush",
    "movie", "games", "music", "headphones", "soccer", "basketball", "casino", "nightlife", "party",
    "shopping", "clothes", "jewelry",
    "phone", "computer", "tv", "camera", "subscription",
    "school", "book", "science",
    "luggage", "beach", "hotel", "map", "world",
    "pets", "kids", "toys", "gift", "star", "bills", "receipt", "category"
)

/** Keys offered when creating a custom income category. */
val incomeIconKeys = listOf(
    "salary", "work", "business", "invest", "money", "coin", "paid", "exchange",
    "percent", "award", "deal", "redeem", "gift", "savings", "star", "category"
)

/** Keys offered when creating an account. */
val accountIconKeys = listOf("card", "cash", "wallet", "savings", "bank", "coin", "money", "business")
