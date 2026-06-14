package com.kinly.famapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kinly.famapp.navigation.Screen
import com.kinly.famapp.navigation.bottomNavItems
import com.kinly.famapp.ui.components.KinlyTopBar
import com.kinly.famapp.ui.components.MeshBackground
import com.kinly.famapp.ui.screens.*
import com.kinly.famapp.ui.theme.FamAppTheme
import com.kinly.famapp.ui.theme.Primary
import com.kinly.famapp.ui.theme.PrimaryContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamAppTheme {
                KinlyApp()
            }
        }
    }
}

@Composable
fun KinlyApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        // Mesh background
        MeshBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                KinlyTopBar()
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Shopping.route) { ShoppingScreen() }
                composable(Screen.Family.route) { FamilyScreen() }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC0B1326))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Primary,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onItemSelected(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B1326),
                        selectedTextColor = Primary,
                        unselectedIconColor = Color(0xFF9090A0),
                        unselectedTextColor = Color(0xFF9090A0),
                        indicatorColor = PrimaryContainer
                    )
                )
            }
        }
    }
}
