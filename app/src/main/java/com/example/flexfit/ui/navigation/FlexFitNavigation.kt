package com.example.flexfit.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flexfit.data.model.ExerciseType
import com.example.flexfit.data.repository.BodyCalibrationRepository
import com.example.flexfit.ui.i18n.l10n
import com.example.flexfit.ui.screens.home.HomeScreen
import com.example.flexfit.ui.screens.workout.WorkoutScreen
import com.example.flexfit.ui.screens.workout.WorkoutSetupScreen
import com.example.flexfit.ui.screens.progress.ProgressScreen
import com.example.flexfit.ui.screens.profile.BodyCalibrationScreen
import com.example.flexfit.ui.screens.profile.EditProfileScreen
import com.example.flexfit.ui.screens.profile.ProfileInfoScreen
import com.example.flexfit.ui.screens.profile.ProfileInfoType
import com.example.flexfit.ui.screens.profile.ProfileScreen
import com.example.flexfit.ui.screens.pullup.PullUpCameraScreen
import com.example.flexfit.ui.screens.shoulderpress.ShoulderPressTrainingScreen
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.TextTertiary

@Composable
fun FlexFitNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bodyProportions by BodyCalibrationRepository.bodyProportions.collectAsState()
    val hasBodyCalibration = bodyProportions?.isComplete == true
    
    // Determine if bottom bar should be shown
    val showBottomBar = when (currentDestination?.route) {
        Screen.WorkoutSetup.route,
        Screen.PullUpCamera.route,
        Screen.ShoulderPressTraining.route,
        Screen.ProfileEdit.route,
        Screen.ProfileInfo.route,
        Screen.BodyCalibration.route,
        Screen.Calibration.route -> false
        else -> true
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { flexFitEnterTransition() },
            exitTransition = { flexFitExitTransition() },
            popEnterTransition = { flexFitPopEnterTransition() },
            popExitTransition = { flexFitPopExitTransition() }
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Workout.route) {
                WorkoutScreen(
                    isBodyCalibrated = hasBodyCalibration,
                    onOpenBodyCalibration = { navController.navigate(Screen.BodyCalibration.route) },
                    onOpenExerciseSetup = { exercise ->
                        if (hasBodyCalibration) {
                            navController.navigate(Screen.WorkoutSetup.createRoute(exercise.name.lowercase()))
                        } else {
                            navController.navigate(Screen.BodyCalibration.route)
                        }
                    }
                )
            }
            composable(Screen.Progress.route) { ProgressScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.ProfileEdit.route) },
                    onOpenProfileInfo = { type ->
                        navController.navigate(Screen.ProfileInfo.createRoute(type.routeValue))
                    },
                    onOpenBodyCalibration = { navController.navigate(Screen.BodyCalibration.route) }
                )
            }
            composable(Screen.ProfileEdit.route) {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.BodyCalibration.route) {
                BodyCalibrationScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ProfileInfo.route) { backStackEntry ->
                val type = ProfileInfoType.fromRouteValue(backStackEntry.arguments?.getString("type"))
                ProfileInfoScreen(
                    type = type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WorkoutSetup.route) { backStackEntry ->
                val routeValue = backStackEntry.arguments?.getString("exerciseType")
                val exercise = ExerciseType.entries.firstOrNull {
                    it.name.equals(routeValue, ignoreCase = true)
                } ?: ExerciseType.PULL_UP

                WorkoutSetupScreen(
                    exercise = exercise,
                    onNavigateBack = { navController.popBackStack() },
                    onStartPullUp = { pullUpType, mode ->
                        navController.navigate(Screen.PullUpCamera.createRoute(pullUpType.name.lowercase(), mode))
                    },
                    onStartShoulderPress = { mode ->
                        navController.navigate(Screen.ShoulderPressTraining.createRoute(mode))
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

        if (showBottomBar) {
            FloatingGlassBottomBar(
                isSelected = { screen ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                },
                onItemClick = { screen ->
                    if (screen.route == Screen.Home.route && currentDestination?.route != Screen.Home.route) {
                        val returnedHome = navController.popBackStack(Screen.Home.route, false)
                        if (!returnedHome) {
                            navController.navigate(Screen.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(screen.route) {
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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.flexFitEnterTransition(): EnterTransition {
    val direction = transitionDirection(
        initialRoute = initialState.destination.route,
        targetRoute = targetState.destination.route,
        forwardDirection = AnimatedContentTransitionScope.SlideDirection.Left,
        backwardDirection = AnimatedContentTransitionScope.SlideDirection.Right
    )

    return slideIntoContainer(
        towards = direction,
        animationSpec = pageOffsetTween()
    ) + fadeIn(
        animationSpec = tween(durationMillis = 90)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.flexFitExitTransition(): ExitTransition {
    val direction = transitionDirection(
        initialRoute = initialState.destination.route,
        targetRoute = targetState.destination.route,
        forwardDirection = AnimatedContentTransitionScope.SlideDirection.Left,
        backwardDirection = AnimatedContentTransitionScope.SlideDirection.Right
    )

    return slideOutOfContainer(
        towards = direction,
        animationSpec = pageOffsetTween()
    ) + fadeOut(
        animationSpec = tween(durationMillis = 90)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.flexFitPopEnterTransition(): EnterTransition {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = pageOffsetTween()
    ) + fadeIn(
        animationSpec = tween(durationMillis = 90)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.flexFitPopExitTransition(): ExitTransition {
    return slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = pageOffsetTween()
    ) + fadeOut(
        animationSpec = tween(durationMillis = 90)
    )
}

private fun transitionDirection(
    initialRoute: String?,
    targetRoute: String?,
    forwardDirection: AnimatedContentTransitionScope.SlideDirection,
    backwardDirection: AnimatedContentTransitionScope.SlideDirection
): AnimatedContentTransitionScope.SlideDirection {
    val initialIndex = bottomNavIndex(initialRoute)
    val targetIndex = bottomNavIndex(targetRoute)

    return if (initialIndex != -1 && targetIndex != -1) {
        if (targetIndex >= initialIndex) forwardDirection else backwardDirection
    } else {
        forwardDirection
    }
}

private fun bottomNavIndex(route: String?): Int {
    return Screen.bottomNavItems.indexOfFirst { it.route == route }
}

private fun pageOffsetTween() = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 220)

@Composable
private fun FloatingGlassBottomBar(
    isSelected: (Screen) -> Boolean,
    onItemClick: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(34.dp),
                    ambientColor = DeepPurple.copy(alpha = 0.20f),
                    spotColor = DeepPurple.copy(alpha = 0.28f)
                )
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.82f),
                            Color.White.copy(alpha = 0.58f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            DeepPurple.copy(alpha = 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                FloatingNavItem(
                    icon = if (isSelected(screen)) screen.selectedIcon else screen.unselectedIcon,
                    label = l10n(screen.title),
                    selected = isSelected(screen),
                    onClick = { onItemClick(screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = springSpec,
        label = "bottom_nav_item_scale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = springSpec,
        label = "bottom_nav_pill_alpha"
    )
    val itemHeight by animateDpAsState(
        targetValue = if (selected) 48.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bottom_nav_item_height"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .graphicsLayer { alpha = pillAlpha }
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepPurple.copy(alpha = 0.18f),
                            AccentPurple.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.34f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(26.dp)
                )
        )

        Row(
            modifier = Modifier
                .scale(scale)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) DeepPurple else TextTertiary,
                modifier = Modifier
                    .size(if (selected) 23.dp else 22.dp)
                    .background(
                        color = if (selected) Color.White.copy(alpha = 0.40f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(if (selected) 2.dp else 0.dp)
            )
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
                text = l10n("Calibration"),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = l10n("Coming soon"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack) {
                Text(l10n("Back"))
            }
        }
    }
}
