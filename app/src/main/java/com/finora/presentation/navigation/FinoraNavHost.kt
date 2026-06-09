package com.finora.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finora.presentation.accounts.AccountsScreen
import com.finora.presentation.addtransaction.AddTransactionScreen
import com.finora.presentation.goals.GoalsScreen
import com.finora.presentation.home.HomeScreen
import com.finora.presentation.settings.SettingsScreen
import com.finora.presentation.statistics.StatisticsScreen
import com.finora.presentation.transactions.TransactionsScreen

@Composable
fun FinoraNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in bottomItems.map { it.destination.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBars,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                FinoraBottomBar(navController, backStackEntry?.destination)
            }
        },
        floatingActionButton = {
            val onTransactionTabs = currentRoute == Destination.Home.route ||
                currentRoute == Destination.Transactions.route
            AnimatedVisibility(
                visible = onTransactionTabs,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(Destination.AddTransaction.create()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Добавить операцию")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Destination.AddTransaction.create()) },
                    onSeeAllTransactions = { navController.navigate(Destination.Transactions.route) },
                    onSeeAccounts = { navController.navigate(Destination.Accounts.route) },
                    onSeeGoals = { navController.navigate(Destination.Goals.route) },
                    onOpenTransaction = { id -> navController.navigate(Destination.AddTransaction.create(id)) }
                )
            }
            composable(Destination.Transactions.route) {
                TransactionsScreen(
                    onOpenTransaction = { id -> navController.navigate(Destination.AddTransaction.create(id)) }
                )
            }
            composable(Destination.Statistics.route) { StatisticsScreen() }
            composable(Destination.Goals.route) { GoalsScreen() }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    onOpenAccounts = { navController.navigate(Destination.Accounts.route) }
                )
            }
            composable(Destination.Accounts.route) {
                AccountsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Destination.AddTransaction.route,
                arguments = listOf(
                    navArgument(Destination.AddTransaction.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val id = entry.arguments?.getLong(Destination.AddTransaction.ARG_ID) ?: -1L
                AddTransactionScreen(
                    transactionId = id,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FinoraBottomBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.destination.route
            } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
