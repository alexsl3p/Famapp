package com.kinly.famapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Shopping : Screen("shopping")
    object Family : Screen("family")
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Home",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        label = "Tasks",
        route = Screen.Tasks.route,
        selectedIcon = Icons.Outlined.Checklist,
        unselectedIcon = Icons.Outlined.Checklist
    ),
    BottomNavItem(
        label = "Shopping",
        route = Screen.Shopping.route,
        selectedIcon = Icons.Filled.ShoppingBasket,
        unselectedIcon = Icons.Outlined.ShoppingBasket
    ),
    BottomNavItem(
        label = "Family",
        route = Screen.Family.route,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )
)
