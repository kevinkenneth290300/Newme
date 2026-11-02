package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.gym.GymScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.plan.PlanScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.system.SystemScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.*
import com.example.viewmodel.NewMeViewModel
import com.example.viewmodel.AppLoadState
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.screens.routines.RoutinesScreen
import com.example.ui.components.journey.MomentHost
import com.example.ui.components.journey.CelebrationHost
import com.example.ui.screens.system.DashboardSetup

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val tag: String) {
    object Splash : Screen("splash", "Splash", Icons.Default.Bolt, "nav_splash")
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Person, "nav_onboarding")
    object Today : Screen("today", "Today", Icons.Default.Bolt, "nav_today")
    object Progress : Screen("progress", "Progress", Icons.Default.TrendingUp, "nav_progress")
    object Plan : Screen("plan", "Plan", Icons.Default.RestaurantMenu, "nav_plan")
    object Gym : Screen("gym", "Gym", Icons.Default.FitnessCenter, "nav_gym")
    object System : Screen("system", "System", Icons.Default.Shield, "nav_system")
    object Routines : Screen("routines", "Routines", Icons.Default.Home, "nav_routines")
}

val NAV_ITEMS = listOf(
    Screen.Today,
    Screen.Routines,
    Screen.Plan,
    Screen.Gym,
    Screen.Progress
)

object NavigationRules {
    fun visible(modules: String?): List<Screen> {
        val enabled = modules?.split(',')?.toSet() ?: setOf("ROUTINES", "MEALS", "GYM")
        return NAV_ITEMS.filter { screen -> when (screen) {
            Screen.Routines -> "ROUTINES" in enabled
            Screen.Plan -> "MEALS" in enabled
            Screen.Gym -> "GYM" in enabled
            else -> true
        } }
    }
    fun label(screen: Screen): Int = when (screen) {
        Screen.Today -> R.string.nav_today; Screen.Routines -> R.string.nav_routines
        Screen.Plan -> R.string.nav_plan; Screen.Gym -> R.string.nav_gym; Screen.Progress -> R.string.nav_progress
        else -> R.string.nav_settings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMeApp(
    viewModel: NewMeViewModel = viewModel(),
    startDestination: String = Screen.Splash.route
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val operationError by viewModel.operationError.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val visibleTabs = NavigationRules.visible(settings?.enabledModules)
    val snackbarHost = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAppResumed() }

    LaunchedEffect(operationError) {
        operationError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearOperationError()
        }
    }
    LaunchedEffect(userProfile, loadState, currentRoute) {
        if (loadState == AppLoadState.Ready && userProfile == null &&
            currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(navController.graph.id)
                launchSingleTop = true
            }
        }
    }
    if (loadState is AppLoadState.Error) {
        Column(
            Modifier.fillMaxSize().background(NewMeBackground).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Local Data Unavailable", style = MaterialTheme.typography.titleLarge, color = NewMeTextPrimary)
            Spacer(Modifier.height(12.dp))
            Text((loadState as AppLoadState.Error).message, color = NewMeTextSecondary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::retryLoading) { Text("Retry") }
        }
        return
    }

    val showBottomBar = currentRoute in NAV_ITEMS.map { it.route }

    LaunchedEffect(settings?.enabledModules, currentRoute) {
        if (settings != null && showBottomBar && visibleTabs.none { it.route == currentRoute }) {
            navController.navigate(Screen.Today.route) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        modifier = Modifier.fillMaxSize().background(NewMeBackground),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            if (showBottomBar || currentRoute == Screen.System.route) TopAppBar(
                title = { Text(stringResource(if (currentRoute == Screen.System.route) R.string.nav_settings else R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (currentRoute == Screen.System.route) IconButton(onClick = { if (!navController.popBackStack()) navController.navigate(Screen.Today.route) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (showBottomBar) IconButton(onClick = { navController.navigate(Screen.System.route) { launchSingleTop = true } }, modifier = Modifier.testTag("profile_settings")) {
                        Icon(Icons.Default.AccountCircle, stringResource(R.string.open_settings), Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = NewMeSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .border(width = 1.dp, color = NewMeBorder)
                        .testTag("bottom_navigation_bar")
                ) {
                    visibleTabs.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = stringResource(NavigationRules.label(screen)),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(NavigationRules.label(screen)),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    letterSpacing = 0.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NewMePrimary,
                                selectedTextColor = NewMePrimary,
                                unselectedIconColor = NewMeTextMuted,
                                unselectedTextColor = NewMeTextMuted,
                                indicatorColor = NewMePrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag(screen.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NewMeBackground)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        userProfile = userProfile,
                        isLoading = loadState == AppLoadState.Loading,
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        },
                        onNavigateToToday = {
                            navController.navigate(Screen.Today.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onLaunchToday = {
                            navController.navigate(Screen.Today.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Today.route) {
                    TodayScreen(
                        viewModel = viewModel,
                        onNavigateToPlan = {
                            navController.navigate(Screen.Plan.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToGym = {
                            navController.navigate(Screen.Gym.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(viewModel = viewModel)
                }
                composable(Screen.Routines.route) { RoutinesScreen(viewModel) }
                composable(Screen.Plan.route) {
                    PlanScreen(viewModel = viewModel)
                }
                composable(Screen.Gym.route) {
                    GymScreen(viewModel = viewModel)
                }
                composable(Screen.System.route) {
                    SystemScreen(
                        viewModel = viewModel,
                        onRerunOnboarding = {
                            navController.navigate(Screen.Onboarding.route) {
                                launchSingleTop = true
                                if (userProfile == null) popUpTo(navController.graph.id)
                            }
                        }
                    )
                }
            }
            MomentHost(viewModel, showBottomBar)
            CelebrationHost(viewModel, showBottomBar)
            if (showBottomBar && userProfile?.isOnboarded == true && settings != null && settings?.dashboardSetupDone == false) DashboardSetup(viewModel)
        }
    }
}
