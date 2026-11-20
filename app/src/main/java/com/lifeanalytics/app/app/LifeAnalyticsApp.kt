package com.lifeanalytics.app.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeanalytics.app.core.designsystem.LaDockItem
import com.lifeanalytics.app.core.designsystem.LaFloatingDock
import com.lifeanalytics.app.core.designsystem.LaTheme
import com.lifeanalytics.app.core.time.DateAttributionPolicy
import com.lifeanalytics.app.data.backup.BackupManager
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.feature.activity.ActivityHubScreen
import com.lifeanalytics.app.feature.finance.FinanceScreen
import com.lifeanalytics.app.feature.habits.DailyHabitsScreen
import com.lifeanalytics.app.feature.home.HomeDashboardScreen
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WorkOutline
import com.lifeanalytics.app.feature.career.CareerScreen
import com.lifeanalytics.app.feature.health.HealthScreen
import com.lifeanalytics.app.feature.insights.InsightsScreen
import com.lifeanalytics.app.feature.planning.DailyPlanningScreen
import com.lifeanalytics.app.feature.profile.ProfileScreen
import com.lifeanalytics.app.feature.review.DailyReviewScreen
import com.lifeanalytics.app.feature.settings.SettingsScreen
import java.time.LocalDate

enum class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Home),
    HEALTH("health", "Health", Icons.Outlined.FavoriteBorder),
    CAREER("career", "Career", Icons.Outlined.WorkOutline),
    FINANCE("finance", "Finance", Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun LifeAnalyticsApp(
    repository: LifeAnalyticsRepository,
    preferencesRepository: PreferencesRepository,
    preferences: AppPreferences,
    backupManager: BackupManager? = null
) {
    val colors = LaTheme.colors
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: PrimaryDestination.HOME.route

    val today = remember { DateAttributionPolicy.todayLocalDate() }
    var planningDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var habitsDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var reviewDateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var financeDayFilterEpoch by rememberSaveable { mutableStateOf<Long?>(null) }

    // Historical sample data (June 1 – Nov 18, 2026) is generated on-device for the user's
    // own existing habits/tasks. All generated rows use the `demo_hist_` ID prefix, never touch
    // real rows, and are rebuilt idempotently on launch while enabled (so newly created habits
    // receive history too). The legacy October-2026 [Demo] habits are always cleaned up.
    // Turning it off in Settings removes every generated row and stops regeneration.
    LaunchedEffect(Unit) {
        repository.removeLegacyOctoberDemoData()
        if (preferencesRepository.isSampleHistoryEnabled()) {
            repository.rebuildHistoricalSampleData(preferences.currencyCode)
        }
    }

    val primaryRoutes = remember { PrimaryDestination.entries.map { it.route }.toSet() }
    val showBottomDock = currentRoute in primaryRoutes
    val dockItems = remember {
        PrimaryDestination.entries.map {
            LaDockItem(route = it.route, label = it.label, icon = it.icon)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = PrimaryDestination.HOME.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(PrimaryDestination.HOME.route) {
                HomeDashboardScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode,
                    monthlyBudgetMinor = preferences.monthlyBudgetMinor,
                    onNavigateToPlanning = { date ->
                        planningDateEpoch = date.toEpochDay()
                        navController.navigate("planning")
                    },
                    onNavigateToHabits = { date ->
                        habitsDateEpoch = date.toEpochDay()
                        navController.navigate("habits")
                    },
                    onNavigateToFinanceDay = { date ->
                        financeDayFilterEpoch = date.toEpochDay()
                        navController.navigate(PrimaryDestination.FINANCE.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile")
                    },
                    onNavigateToInsights = {
                        navController.navigate("insights")
                    }
                )
            }

            composable(PrimaryDestination.HEALTH.route) {
                HealthScreen(
                    repository = repository,
                    onNavigateToAllHabits = {
                        habitsDateEpoch = today.toEpochDay()
                        navController.navigate("habits")
                    }
                )
            }

            composable(PrimaryDestination.CAREER.route) {
                CareerScreen(
                    repository = repository,
                    onNavigateToAllTasks = {
                        planningDateEpoch = today.toEpochDay()
                        navController.navigate("planning")
                    }
                )
            }

            composable(PrimaryDestination.FINANCE.route) {
                FinanceScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode,
                    monthlyBudgetMinor = preferences.monthlyBudgetMinor,
                    onUpdateMonthlyBudget = { budgetMinor ->
                        preferencesRepository.setMonthlyBudgetMinor(budgetMinor)
                    },
                    initialDayFilter = financeDayFilterEpoch?.let { LocalDate.ofEpochDay(it) }
                )
            }

            composable("activity") {
                ActivityHubScreen(
                    repository = repository,
                    onOpenPlanning = { date ->
                        planningDateEpoch = date.toEpochDay()
                        navController.navigate("planning")
                    },
                    onOpenHabits = { date ->
                        habitsDateEpoch = date.toEpochDay()
                        navController.navigate("habits")
                    },
                    onOpenReview = { date ->
                        reviewDateEpoch = date.toEpochDay()
                        navController.navigate("review")
                    }
                )
            }

            composable("insights") {
                InsightsScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode,
                    monthlyBudgetMinor = preferences.monthlyBudgetMinor
                )
            }

            composable("planning") {
                DailyPlanningScreen(
                    repository = repository,
                    selectedDate = LocalDate.ofEpochDay(planningDateEpoch),
                    today = today,
                    onDateChange = { newDate -> planningDateEpoch = newDate.toEpochDay() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("habits") {
                DailyHabitsScreen(
                    repository = repository,
                    initialDate = LocalDate.ofEpochDay(habitsDateEpoch),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("review") {
                DailyReviewScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode,
                    initialDate = LocalDate.ofEpochDay(reviewDateEpoch),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                ProfileScreen(
                    backupManager = backupManager,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDailyReview = {
                        reviewDateEpoch = today.toEpochDay()
                        navController.navigate("review")
                    },
                    onOpenInsights = { navController.navigate("insights") },
                    onOpenTasks = {
                        planningDateEpoch = today.toEpochDay()
                        navController.navigate("planning")
                    },
                    onOpenHabits = {
                        habitsDateEpoch = today.toEpochDay()
                        navController.navigate("habits")
                    },
                    onOpenActivity = { navController.navigate("activity") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    preferences = preferences,
                    preferencesRepository = preferencesRepository,
                    repository = repository,
                    backupManager = backupManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomDock) {
            LaFloatingDock(
                items = dockItems,
                currentRoute = currentRoute,
                onItemSelected = { item ->
                    if (item.route == PrimaryDestination.FINANCE.route) {
                        financeDayFilterEpoch = null
                    }
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
