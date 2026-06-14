package com.finora.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finora.FinoraApp
import com.finora.data.remote.SupabaseModule
import com.finora.presentation.accounts.AccountsScreen
import com.finora.presentation.addtransaction.AddTransactionScreen
import com.finora.presentation.ai.AiChatScreen
import com.finora.presentation.analytics.AnalyticsScreen
import com.finora.presentation.auth.AuthScreen
import com.finora.presentation.components.OfflineBanner
import com.finora.presentation.goals.GoalsScreen
import com.finora.presentation.guide.GuideController
import com.finora.presentation.guide.GuideOverlay
import com.finora.presentation.guide.GuideScreen
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController

import com.finora.presentation.guide.guideTarget
import com.finora.presentation.home.HomeScreen
import com.finora.presentation.onboarding.OnboardingScreen
import com.finora.presentation.budget.BudgetScreen
import com.finora.presentation.challenges.ChallengesScreen
import com.finora.presentation.recurring.RecurringRulesScreen
import com.finora.presentation.settings.SettingsScreen
import com.finora.presentation.templates.TemplatesScreen
import com.finora.presentation.tips.TaxScreen
import com.finora.presentation.transactions.TransactionsScreen
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private const val NAV_ANIM_MS = 300

@Composable
fun FinoraNavHost(
    navController: NavHostController = rememberNavController(),
    startRouteOverride: String? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as FinoraApp
    val settingsRepo = app.container.settings
    val scope = rememberCoroutineScope()

    // ─── Offline monitoring ─────────────────────────────────────────────
    val isOffline by app.container.networkMonitor.isOnline
        .collectAsStateWithLifecycle(initialValue = true)
        .let { online ->
            // Invert: isOffline = !isOnline
            androidx.compose.runtime.derivedStateOf { !online.value }
        }

    // Retry pending sync when connectivity returns
    LaunchedEffect(isOffline) {
        if (!isOffline) app.container.syncManager.retryPendingSync()
    }

    // ─── Auth + prefs ────────────────────────────────────────────────────
    val sessionStatus by SupabaseModule.client.auth.sessionStatus
        .collectAsStateWithLifecycle(initialValue = null)
    val authSkipped by settingsRepo.authSkipped
        .collectAsStateWithLifecycle(initialValue = false)
    val onboardingCompleted by settingsRepo.onboardingCompleted
        .collectAsStateWithLifecycle(initialValue = true)
    val guideCompleted by settingsRepo.guideCompleted
        .collectAsStateWithLifecycle(initialValue = true)

    val isAuthenticated = sessionStatus is SessionStatus.Authenticated
    val canAccessApp = isAuthenticated || authSkipped

    // ─── Guide controller ────────────────────────────────────────────────
    val guideController = remember { GuideController() }

    // Wire up screen navigation for the guide
    LaunchedEffect(Unit) {
        guideController.navigateToScreen = { screen ->
            val route = when (screen) {
                GuideScreen.HOME -> Destination.Home.route
                GuideScreen.TRANSACTIONS -> Destination.Transactions.route
                GuideScreen.GOALS -> Destination.Goals.route
                GuideScreen.SETTINGS -> Destination.Settings.route
            }
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    // ─── Auth navigation ─────────────────────────────────────────────────
    LaunchedEffect(sessionStatus, authSkipped, onboardingCompleted) {
        when {
            canAccessApp -> {
                if (currentRoute == Destination.Auth.route) {
                    val next = if (!onboardingCompleted) Destination.Onboarding.route
                    else Destination.Home.route
                    navController.navigate(next) {
                        popUpTo(Destination.Auth.route) { inclusive = true }
                    }
                }
            }
            sessionStatus is SessionStatus.NotAuthenticated -> {
                if (currentRoute != Destination.Auth.route) {
                    navController.navigate(Destination.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // NOTE: Auto-upload LaunchedEffect was REMOVED.
    // Upload/download is now exclusively handled by:
    //   • AuthViewModel (download on login, upload on register)
    //   • SettingsViewModel (upload before sign-out)
    //   • SyncManager.scheduleUpload() (debounced after data changes)

    // Start guide after onboarding
    LaunchedEffect(onboardingCompleted, guideCompleted) {
        if (onboardingCompleted && !guideCompleted && canAccessApp) {
            guideController.start()
        }
    }

    val showBars = currentRoute in bottomItems.map { it.destination.route }

    CompositionLocalProvider(LocalGuideController provides guideController) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBars,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        FinoraBottomBar(
                            navController = navController,
                            currentDestination = backStackEntry?.destination,
                            guideController = guideController
                        )
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
                            contentColor = Color.White,
                            modifier = Modifier.guideTarget(guideController, GuideStep.FAB)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Добавить операцию")
                        }
                    }
                }
            ) { innerPadding ->
                // ─── Offline banner ──────────────────────────────
                Column(modifier = Modifier.padding(innerPadding)) {
                    OfflineBanner(isOffline = isOffline)
                }

                val startDest = startRouteOverride ?: when {
                    canAccessApp && onboardingCompleted -> Destination.Home.route
                    canAccessApp -> Destination.Onboarding.route
                    else -> Destination.Auth.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    modifier = Modifier.padding(innerPadding),
                    // Smooth slide transitions for all routes
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(NAV_ANIM_MS)
                        ) + fadeIn(tween(NAV_ANIM_MS))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(NAV_ANIM_MS)
                        ) + fadeOut(tween(NAV_ANIM_MS))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(NAV_ANIM_MS)
                        ) + fadeIn(tween(NAV_ANIM_MS))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(NAV_ANIM_MS)
                        ) + fadeOut(tween(NAV_ANIM_MS))
                    }
                ) {
                    composable(Destination.Auth.route) {
                        AuthScreen(
                            onSkipped = {
                                navController.navigate(Destination.Onboarding.route) {
                                    popUpTo(Destination.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Destination.Onboarding.route) {
                        OnboardingScreen(
                            onFinish = {
                                scope.launch { settingsRepo.setOnboardingCompleted(true) }
                                navController.navigate(Destination.Home.route) {
                                    popUpTo(Destination.Onboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Destination.Home.route) {
                        HomeScreen(
                            onAddTransaction = { navController.navigate(Destination.AddTransaction.create()) },
                            onSeeAllTransactions = { navController.navigate(Destination.Transactions.route) },
                            onSeeAccounts = { navController.navigate(Destination.Accounts.route) },
                            onSeeGoals = { navController.navigate(Destination.Goals.route) },
                            onOpenAi = { navController.navigate(Destination.AiChat.route) },
                            onOpenTransaction = { id -> navController.navigate(Destination.AddTransaction.create(id)) },
                            onOpenTax = { navController.navigate(Destination.TaxDeduction.route) }
                        )
                    }
                    composable(Destination.TaxDeduction.route) {
                        TaxScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Destination.Transactions.route) {
                        TransactionsScreen(
                            onOpenTransaction = { id -> navController.navigate(Destination.AddTransaction.create(id)) }
                        )
                    }
                    composable(Destination.Analytics.route) {
                        AnalyticsScreen()
                    }
                    composable(Destination.AiChat.route) {
                        AiChatScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Destination.Goals.route) { GoalsScreen() }
                    composable(Destination.Settings.route) {
                        SettingsScreen(
                            onOpenAccounts = { navController.navigate(Destination.Accounts.route) },
                            onOpenRecurring = { navController.navigate(Destination.RecurringRules.route) },
                            onOpenTemplates = { navController.navigate(Destination.Templates.route) }
                        )
                    }
                    composable(Destination.RecurringRules.route) {
                        RecurringRulesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Destination.Budgets.route) {
                        BudgetScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Destination.Templates.route) {
                        TemplatesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Destination.Challenges.route) {
                        ChallengesScreen(onBack = { navController.popBackStack() })
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

            // ─── Guide overlay (on all main screens) ─────────────────────
            if (guideController.isActive && showBars) {
                GuideOverlay(
                    controller = guideController,
                    onFinish = { scope.launch { settingsRepo.setGuideCompleted(true) } }
                )
            }
        }
    }
}

@Composable
private fun FinoraBottomBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    guideController: GuideController
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomItems.forEachIndexed { index, item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.destination.route
            } == true

            val guideStep = when (index) {
                1 -> GuideStep.NAV_TRANSACTIONS
                3 -> GuideStep.NAV_GOALS
                4 -> GuideStep.NAV_SETTINGS
                else -> -1
            }

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
                modifier = if (guideStep >= 0) Modifier.guideTarget(guideController, guideStep)
                else Modifier,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
