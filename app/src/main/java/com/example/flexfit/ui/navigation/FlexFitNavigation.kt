package com.example.flexfit.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flexfit.ui.screens.home.HomeScreen
import com.example.flexfit.ui.screens.workout.WorkoutScreen
import com.example.flexfit.ui.screens.progress.ProgressScreen
import com.example.flexfit.ui.screens.profile.ProfileScreen
import com.example.flexfit.ui.screens.pullup.PullUpSelectScreen
import com.example.flexfit.ui.screens.pullup.PullUpCameraScreen
import com.example.flexfit.ui.screens.shoulderpress.ShoulderPressTrainingScreen
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.LightBackground
import com.example.flexfit.ui.theme.TextTertiary

@Composable
fun FlexFitNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if bottom bar should be shown
    val showBottomBar = when (currentDestination?.route) {
        Screen.PullUpSelect.route,
        Screen.PullUpCamera.route,
        Screen.ShoulderPressTraining.route,
        Screen.Calibration.route -> false
        else -> true
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = LightBackground
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepPurple,
                                selectedTextColor = DeepPurple,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Workout.route) {
                WorkoutScreen(
                    onOpenPullUpSelection = {
                        navController.navigate(Screen.PullUpSelect.route)
                    },
                    onStartShoulderPress = { mode ->
                        navController.navigate(Screen.ShoulderPressTraining.createRoute(mode))
                    }
                )
            }
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            
            // Pull-up related screens
            composable(Screen.PullUpSelect.route) {
                PullUpSelectScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onStartTraining = { pullUpType, mode ->
                        navController.navigate(Screen.PullUpCamera.createRoute(pullUpType.name.lowercase(), mode))
                    }
                )
            }
            
            composable(Screen.PullUpCamera.route) { backStackEntry ->
                val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "normal"
                val mode = backStackEntry.arguments?.getString("mode") ?: "camera"
                
                PullUpCameraScreen(
                    exerciseType = exerciseType,
                    mode = mode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShoulderPressTraining.route) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "camera"

                ShoulderPressTrainingScreen(
                    mode = mode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Calibration.route) {
                // CalibrationScreen will be implemented later
                CalibrationPlaceholder(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun CalibrationPlaceholder(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Calibration",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack) {
                Text("Back")
            }
        }
    }
}
