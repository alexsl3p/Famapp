package com.kinly.famapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kinly.famapp.features.auth.AuthState
import com.kinly.famapp.features.auth.AuthViewModel
import com.kinly.famapp.features.auth.GoogleSignInHelper
import com.kinly.famapp.features.family.FamilyViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.features.tasks.TasksViewModel
import com.kinly.famapp.navigation.Screen
import com.kinly.famapp.navigation.bottomNavItems
import com.kinly.famapp.ui.components.KinlyTopBar
import com.kinly.famapp.ui.components.MeshBackground
import com.kinly.famapp.ui.screens.*
import com.kinly.famapp.ui.theme.FamAppTheme
import com.kinly.famapp.ui.theme.Primary
import com.kinly.famapp.ui.theme.PrimaryContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val isSigningIn by authViewModel.isSigningIn.collectAsState()
    val signInError by authViewModel.signInError.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground()

        when (val state = authState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is AuthState.Unauthenticated -> {
                WelcomeScreen(
                    isLoading = isSigningIn,
                    errorMessage = signInError,
                    onSignInWithGoogle = {
                        authViewModel.clearSignInError()
                        scope.launch {
                            try {
                                val helper = GoogleSignInHelper(context)
                                val (idToken, rawNonce) = helper.signIn()
                                authViewModel.signInWithGoogle(idToken, rawNonce)
                            } catch (e: Exception) {
                                authViewModel.setSignInError(e.message ?: "Ошибка входа через Google")
                            }
                        }
                    },
                    onContinueAsGuest = { authViewModel.signInAsGuest() }
                )
            }
            is AuthState.NeedsFamily -> {
                val familyViewModel: FamilyViewModel = hiltViewModel()
                val familyState by familyViewModel.uiState.collectAsState()

                OnboardingFamilyScreen(
                    onCreateFamily = { name ->
                        familyViewModel.createFamily(name) {
                            authViewModel.refreshProfile()
                        }
                    },
                    onJoinFamily = { code ->
                        familyViewModel.joinFamily(code) {
                            authViewModel.refreshProfile()
                        }
                    },
                    isLoading = familyState.isLoading,
                    error = familyState.error
                )
            }
            is AuthState.Authenticated -> {
                MainAppContent(
                    profile = state.profile,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    profile: com.kinly.famapp.data.models.Profile,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val familyId = profile.activeFamilyId ?: return

    val tasksViewModel: TasksViewModel = hiltViewModel()
    val shoppingViewModel: ShoppingViewModel = hiltViewModel()
    val familyViewModel: FamilyViewModel = hiltViewModel()

    LaunchedEffect(familyId) {
        tasksViewModel.load(familyId, profile.id)
        shoppingViewModel.load(familyId)
        familyViewModel.load(familyId)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { KinlyTopBar(userInitial = profile.initial) },
        bottomBar = {
            BottomNavBar(currentRoute = currentRoute, onItemSelected = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    tasksViewModel = tasksViewModel,
                    shoppingViewModel = shoppingViewModel,
                    familyViewModel = familyViewModel,
                    currentUserId = profile.id
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(
                    viewModel = tasksViewModel,
                    familyId = familyId,
                    currentUserId = profile.id
                )
            }
            composable(Screen.Shopping.route) {
                ShoppingScreen(viewModel = shoppingViewModel)
            }
            composable(Screen.Family.route) {
                FamilyScreen(viewModel = familyViewModel, currentUserId = profile.id)
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String?, onItemSelected: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xCC0B1326))) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Primary,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
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
                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
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
