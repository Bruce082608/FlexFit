package com.example.flexfit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Workout : Screen(
        route = "workout",
        title = "Workout",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )

    data object Progress : Screen(
        route = "progress",
        title = "Progress",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart
    )

    data object Profile : Screen(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object ProfileEdit : Screen(
        route = "profile_edit",
        title = "Edit Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object ProfileInfo : Screen(
        route = "profile_info/{type}",
        title = "Profile Info",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    ) {
        fun createRoute(type: String) = "profile_info/$type"
    }

    data object WorkoutSetup : Screen(
        route = "workout_setup/{exerciseType}",
        title = "Workout Setup",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ) {
        fun createRoute(exerciseType: String) = "workout_setup/$exerciseType"
    }

    data object PullUpCamera : Screen(
        route = "pullup_camera/{exerciseType}/{mode}",
        title = "Pull-up Camera",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ) {
        fun createRoute(exerciseType: String, mode: String) = "pullup_camera/$exerciseType/$mode"
    }

    data object ShoulderPressTraining : Screen(
        route = "shoulder_press/{mode}",
        title = "Shoulder Press",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ) {
        fun createRoute(mode: String) = "shoulder_press/$mode"
    }

    data object Calibration : Screen(
        route = "calibration",
        title = "Calibration",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems = listOf(Home, Workout, Progress, Profile)
    }
}
