package com.kinly.famapp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kinly.famapp.features.auth.GoogleSignInResult
import com.kinly.famapp.features.family.FamilyViewModel
import com.kinly.famapp.features.inventory.InventoryViewModel
import com.kinly.famapp.features.notifications.NotificationViewModel
import com.kinly.famapp.features.products.ProductViewModel
import com.kinly.famapp.features.shopping.ShoppingViewModel
import com.kinly.famapp.features.stats.StatsViewModel
import com.kinly.famapp.features.tasks.TasksViewModel
import com.kinly.famapp.features.update.UpdateViewModel
import com.kinly.famapp.navigation.Screen
import com.kinly.famapp.navigation.bottomNavItems
import com.kinly.famapp.features.voice.VoiceCommand
import com.kinly.famapp.features.voice.VoiceCommandParser
import com.kinly.famapp.ui.components.KinlyTopBar
import com.kinly.famapp.ui.components.MeshBackground
import com.kinly.famapp.ui.components.UpdateDialog
import com.kinly.famapp.ui.components.VoiceDictationDialog
import com.kinly.famapp.ui.screens.*
import com.kinly.famapp.ui.theme.AccentGradient
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

    // Проверка обновлений (само-обновление через Supabase Storage)
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.state.collectAsState()
    LaunchedEffect(Unit) { updateViewModel.checkForUpdate() }
    UpdateDialog(
        state = updateState,
        onUpdate = { updateViewModel.startUpdate(context) },
        onDismiss = { updateViewModel.dismiss() }
    )

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (val parsed = GoogleSignInHelper.parseResult(result.data)) {
            is GoogleSignInResult.Success ->
                scope.launch { authViewModel.signInWithGoogle(parsed.idToken, "") }
            is GoogleSignInResult.Failure -> {
                // 12501 = пользователь сам отменил — не показываем как ошибку.
                if (parsed.statusCode != 12501) {
                    authViewModel.setSignInError(parsed.message)
                } else {
                    authViewModel.clearSignInError()
                }
            }
        }
    }

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
                        val client = GoogleSignInHelper.getClient(context)
                        client.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(client.signInIntent)
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
    val productViewModel: ProductViewModel = hiltViewModel()
    val inventoryViewModel: InventoryViewModel = hiltViewModel()
    val statsViewModel: StatsViewModel = hiltViewModel()
    val notificationViewModel: NotificationViewModel = hiltViewModel()

    val familyUiState by familyViewModel.uiState.collectAsState()
    val notificationState by notificationViewModel.uiState.collectAsState()
    val shoppingUiState by shoppingViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val voiceScope = rememberCoroutineScope()
    var showVoiceDialog by remember { mutableStateOf(false) }

    val handleVoiceResult: (String) -> Unit = { spoken ->
        when (val cmd = VoiceCommandParser.parse(spoken)) {
            is VoiceCommand.AddShopping -> {
                val listId = shoppingUiState.currentList?.id ?: shoppingUiState.lists.firstOrNull()?.id
                if (listId != null) {
                    cmd.titles.forEach { shoppingViewModel.addItem(listId, it) }
                    val msg = if (cmd.titles.size == 1) "🛒 В список: ${cmd.titles.first()}"
                              else "🛒 Добавлено (${cmd.titles.size}): ${cmd.titles.joinToString(", ")}"
                    voiceScope.launch { snackbarHostState.showSnackbar(msg) }
                } else {
                    voiceScope.launch { snackbarHostState.showSnackbar("Сначала создайте список покупок") }
                }
            }
            is VoiceCommand.AddTask -> {
                cmd.titles.forEach { tasksViewModel.createTask(title = it) }
                val msg = if (cmd.titles.size == 1) "✅ Задача: ${cmd.titles.first()}"
                          else "✅ Задачи (${cmd.titles.size}): ${cmd.titles.joinToString(", ")}"
                voiceScope.launch { snackbarHostState.showSnackbar(msg) }
            }
            is VoiceCommand.Unknown -> {
                voiceScope.launch { snackbarHostState.showSnackbar("Не понял: «${cmd.raw}». Скажите «добавь…» или «задача…»") }
            }
        }
    }

    if (showVoiceDialog) {
        VoiceDictationDialog(
            onResult = handleVoiceResult,
            onDismiss = { showVoiceDialog = false }
        )
    }

    LaunchedEffect(familyId) {
        tasksViewModel.load(familyId, profile.id)
        shoppingViewModel.load(familyId)
        familyViewModel.load(familyId)
        productViewModel.load(familyId)
        inventoryViewModel.load(familyId)
        statsViewModel.load(familyId)
        notificationViewModel.load(profile.id)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            KinlyTopBar(
                userInitial = profile.initial,
                avatarUrl = profile.avatarUrl,
                unreadCount = notificationState.unreadCount,
                onAvatarClick = { navController.navigate(Screen.Profile.route) },
                onBellClick = { navController.navigate(Screen.Notifications.route) }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    // Если открыт не-табовый экран (уведомления/профиль) — убираем его без
                    // сохранения, чтобы при возврате на вкладку он не всплывал снова.
                    if (currentRoute == Screen.Notifications.route || currentRoute == Screen.Profile.route) {
                        navController.popBackStack()
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onVoiceClick = { showVoiceDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    currentUserId = profile.id,
                    userName = profile.displayName,
                    onOpenShopping = {
                        navController.navigate(Screen.Shopping.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(
                    viewModel = tasksViewModel,
                    familyId = familyId,
                    currentUserId = profile.id,
                    members = familyUiState.members
                )
            }
            composable(Screen.Shopping.route) {
                ShoppingContainerScreen(
                    shoppingViewModel = shoppingViewModel,
                    productViewModel = productViewModel,
                    inventoryViewModel = inventoryViewModel,
                    statsViewModel = statsViewModel
                )
            }
            composable(Screen.Family.route) {
                FamilyScreen(viewModel = familyViewModel, currentUserId = profile.id)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profile = profile,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    viewModel = notificationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemSelected: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xCC0B1326))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(30.dp))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Первые две вкладки
            bottomNavItems.take(2).forEach { item ->
                NavCell(item, currentRoute == item.route) { onItemSelected(item.route) }
            }

            // Центральная круглая кнопка-микрофон
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(AccentGradient))
                        .border(3.dp, Color(0xCC0B1326), CircleShape)
                        .clickable { onVoiceClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Голосовая команда",
                        tint = Color(0xFF0B1326),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Последние две вкладки
            bottomNavItems.drop(2).forEach { item ->
                NavCell(item, currentRoute == item.route) { onItemSelected(item.route) }
            }
        }
    }
}

@Composable
private fun RowScope.NavCell(
    item: com.kinly.famapp.navigation.BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (selected) Color(0xFF0B1326) else Color(0xFF9090A0),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Primary else Color(0xFF9090A0)
        )
    }
}
