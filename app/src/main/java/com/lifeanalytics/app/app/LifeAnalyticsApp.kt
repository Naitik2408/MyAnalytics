package com.lifeanalytics.app.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
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
import com.lifeanalytics.app.data.repository.LifeAnalyticsRepository
import com.lifeanalytics.app.data.repository.PreferencesRepository
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.feature.activity.ActivityHubScreen
import com.lifeanalytics.app.feature.finance.FinanceScreen
import com.lifeanalytics.app.feature.focus.FocusScreen
import com.lifeanalytics.app.feature.habits.DailyHabitsScreen
import com.lifeanalytics.app.feature.home.HomeDashboardScreen
import com.lifeanalytics.app.feature.insights.InsightsScreen
import com.lifeanalytics.app.feature.planning.DailyPlanningScreen
import com.lifeanalytics.app.data.backup.BackupManager
import com.lifeanalytics.app.feature.review.DailyReviewScreen
import com.lifeanalytics.app.feature.settings.SettingsScreen
import java.time.LocalDate

enum class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Home),
    FINANCE("finance", "Finance", Icons.Outlined.AccountBalanceWallet),
    ACTIVITY("activity", "Activity", Icons.Outlined.Timer),
    INSIGHTS("insights", "Insights", Icons.Outlined.BarChart)
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
    var preselectedFocusTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    val primaryRoutes = remember { PrimaryDestination.entries.map { it.route }.toSet() }
    val showBottomDock = currentRoute in primaryRoutes
    val dockItems = remember {
        PrimaryDestination.entries.map {
            LaDockItem(route = it.route, label = it.label, icon = it.icon)
        }
    }

    // Window insets applied once at root shell (Section 7.1)
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
                    onNavigateToPlanning = { date ->
                        planningDateEpoch = date.toEpochDay()
                        navController.navigate("planning")
                    },
                    onNavigateToFocus = { taskId ->
                        preselectedFocusTaskId = taskId
                        navController.navigate("focus")
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
                    onNavigateToDailyReview = { date ->
                        reviewDateEpoch = date.toEpochDay()
                        navController.navigate("review")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToInsights = {
                        navController.navigate(PrimaryDestination.INSIGHTS.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(PrimaryDestination.FINANCE.route) {
                FinanceScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode,
                    initialDayFilter = financeDayFilterEpoch?.let { LocalDate.ofEpochDay(it) }
                )
            }

            composable(PrimaryDestination.ACTIVITY.route) {
                ActivityHubScreen(
                    repository = repository,
                    onOpenPlanning = { date ->
                        planningDateEpoch = date.toEpochDay()
                        navController.navigate("planning")
                    },
                    onOpenFocus = {
                        preselectedFocusTaskId = null
                        navController.navigate("focus")
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

            composable(PrimaryDestination.INSIGHTS.route) {
                InsightsScreen(
                    repository = repository,
                    currencyCode = preferences.currencyCode
                )
            }

            composable("planning") {
                DailyPlanningScreen(
                    repository = repository,
                    selectedDate = LocalDate.ofEpochDay(planningDateEpoch),
                    today = today,
                    onDateChange = { newDate -> planningDateEpoch = newDate.toEpochDay() },
                    onStartFocusForTask = { task ->
                        preselectedFocusTaskId = task.id
                        navController.navigate("focus")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("focus") {
                FocusScreen(
                    repository = repository,
                    today = today,
                    preselectedTaskId = preselectedFocusTaskId,
                    onConsumedPreselectedTask = { preselectedFocusTaskId = null },
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
