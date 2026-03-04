package com.example.pgk_food.shared.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.platform.PlatformBackHandler
import com.example.pgk_food.shared.platformName
import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.repository.*
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.runtime.MainLoopSnapshot
import com.example.pgk_food.shared.runtime.MainLoopStateStore
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.ui.viewmodels.ChefViewModel
import com.example.pgk_food.shared.ui.viewmodels.StudentViewModel
import com.example.pgk_food.shared.util.HintScreenKey
import com.example.pgk_food.shared.util.NetworkMonitor
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun mainScreenTitle(subScreen: String): String = when (subScreen) {
    "dashboard" -> "ПГК ПИТАНИЕ"
    "roster" -> "ОТМЕТКА ПИТАНИЯ"
    "stats" -> "СТАТИСТИКА"
    "users" -> "ПОЛЬЗОВАТЕЛИ"
    "users_create" -> "СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ"
    "groups" -> "ГРУППЫ"
    "reports" -> "ОТЧЕТЫ"
    "menu" -> "МЕНЮ В СТОЛОВОЙ"
    "coupons" -> "МОИ ТАЛОНЫ"
    "qr" -> "МОЙ QR-КОД"
    "scanner" -> "СКАНЕР QR"
    "menu_manage" -> "УПРАВЛЕНИЕ МЕНЮ"
    "settings" -> "НАСТРОЙКИ"
    "categories" -> "КАТЕГОРИИ"
    else -> "ПГК ПИТАНИЕ"
}

private fun UserRole.titleRu(): String = when (this) {
    UserRole.STUDENT -> "Студент"
    UserRole.CHEF -> "Повар"
    UserRole.REGISTRATOR -> "Регистратор"
    UserRole.CURATOR -> "Куратор"
    UserRole.ADMIN -> "Администратор"
}

private fun screenAllowedForRole(role: UserRole?, screen: String): Boolean {
    if (screen == "dashboard" || screen == "settings") return true
    val roleScreens = when (role) {
        UserRole.STUDENT -> setOf("coupons", "qr", "menu")
        UserRole.CHEF -> setOf("scanner", "menu_manage", "stats")
        UserRole.REGISTRATOR -> setOf("users", "users_create", "groups")
        UserRole.CURATOR -> setOf("roster", "stats", "categories", "reports")
        UserRole.ADMIN -> setOf("reports")
        null -> emptySet()
    }
    return screen in roleScreens
}

private fun resolveSubScreenForRole(role: UserRole?, candidate: String?): String {
    val screen = candidate ?: "dashboard"
    return if (screenAllowedForRole(role, screen)) screen else "dashboard"
}

private fun parseRoleOrNull(raw: String?): UserRole? {
    if (raw.isNullOrBlank()) return null
    return runCatching { UserRole.valueOf(raw) }.getOrNull()
}

private enum class NavDirection {
    Forward,
    Backward,
}

private fun navContentTransition(direction: NavDirection): ContentTransform {
    return if (direction == NavDirection.Forward) {
        (slideInHorizontally(
            animationSpec = tween(260),
            initialOffsetX = { width -> width / 3 },
        ) + fadeIn(animationSpec = tween(220))).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(260),
                targetOffsetX = { width -> -width / 4 },
            ) + fadeOut(animationSpec = tween(180))
        )
    } else {
        (slideInHorizontally(
            animationSpec = tween(260),
            initialOffsetX = { width -> -width / 3 },
        ) + fadeIn(animationSpec = tween(220))).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(260),
                targetOffsetX = { width -> width / 4 },
            ) + fadeOut(animationSpec = tween(180))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenShared(
    session: UserSession,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { NotificationRepository() }
    val uiSettingsManager = remember { com.example.pgk_food.shared.util.UiSettingsManager() }
    val mainLoopStateStore = remember { MainLoopStateStore() }
    val authRepository = remember { AuthRepository() }
    val studentRepository = remember { StudentRepository() }
    val chefRepository = remember { ChefRepository() }
    val networkMonitor = remember { NetworkMonitor() }
    val studentViewModel =
        remember(session.userId) { StudentViewModel(authRepository, studentRepository) }
    val chefViewModel =
        remember(session.userId) { ChefViewModel(authRepository, chefRepository, networkMonitor) }

    val roles = session.roles
    var currentSubScreen by remember(session.userId) { mutableStateOf("dashboard") }
    var backStack by remember(session.userId) { mutableStateOf(emptyList<String>()) }
    var navDirection by remember(session.userId) { mutableStateOf(NavDirection.Forward) }
    var selectedMealType by remember(session.userId) { mutableStateOf("") }
    var selectedRole by remember(session.userId) { mutableStateOf<UserRole?>(null) }
    var showGlobalHints by remember(session.userId) {
        mutableStateOf(
            uiSettingsManager.shouldShowHints(
                session.userId
            )
        )
    }
    var hintsVersion by remember(session.userId) { mutableIntStateOf(0) }
    var isSnapshotRestored by remember(session.userId) { mutableStateOf(false) }
    val isIos = platformName() == "iOS"

    fun navigateTo(subScreen: String) {
        if (subScreen == currentSubScreen) return
        navDirection = NavDirection.Forward
        backStack = backStack + currentSubScreen
        currentSubScreen = subScreen
    }

    fun popScreen() {
        val last = backStack.lastOrNull()
        if (last != null) {
            navDirection = NavDirection.Backward
            backStack = backStack.dropLast(1)
            currentSubScreen = last
            return
        }
        if (currentSubScreen != "dashboard") {
            navDirection = NavDirection.Backward
            currentSubScreen = "dashboard"
        }
    }

    val canPop = backStack.isNotEmpty() || currentSubScreen != "dashboard"

    LaunchedEffect(session.userId) {
        val restored = mainLoopStateStore.restore(session.userId)
        val restoredRole = parseRoleOrNull(restored?.selectedRole)
            ?.takeIf { it in roles }
        val activeRole = restoredRole ?: roles.firstOrNull()
        val restoredSubScreen = if (restored?.currentSubScreen == "users_create") {
            "users"
        } else {
            restored?.currentSubScreen
        }
        selectedRole = activeRole
        currentSubScreen = resolveSubScreenForRole(activeRole, restoredSubScreen)
        backStack = emptyList()
        selectedMealType = restored?.selectedMealType.orEmpty()
        showGlobalHints = uiSettingsManager.shouldShowHints(session.userId)
        isSnapshotRestored = true
    }

    LaunchedEffect(roles, selectedRole, currentSubScreen, isSnapshotRestored) {
        if (!isSnapshotRestored) return@LaunchedEffect
        val safeRole = (selectedRole ?: roles.firstOrNull())
            ?.takeIf { it in roles }
        if (safeRole != selectedRole) {
            selectedRole = safeRole
            return@LaunchedEffect
        }
        val safeSubScreen = resolveSubScreenForRole(safeRole, currentSubScreen)
        if (safeSubScreen != currentSubScreen) {
            currentSubScreen = safeSubScreen
            backStack = emptyList()
        }
    }

    LaunchedEffect(
        session.userId,
        selectedRole,
        currentSubScreen,
        selectedMealType,
        isSnapshotRestored
    ) {
        if (!isSnapshotRestored) return@LaunchedEffect
        val snapshotSubScreen = if (
            (selectedRole ?: roles.firstOrNull()) == UserRole.REGISTRATOR &&
            currentSubScreen == "users_create"
        ) {
            "users"
        } else {
            currentSubScreen
        }
        mainLoopStateStore.save(
            session.userId,
            MainLoopSnapshot(
                selectedRole = selectedRole?.name,
                currentSubScreen = snapshotSubScreen,
                selectedMealType = selectedMealType,
            )
        )
    }

    LaunchedEffect(currentSubScreen, session.userId) {
        if (currentSubScreen != "settings") {
            showGlobalHints = uiSettingsManager.shouldShowHints(session.userId)
            hintsVersion += 1
        }
    }
    val shouldShowScreenHints: (HintScreenKey) -> Boolean = { screen ->
        hintsVersion
        if (!showGlobalHints) {
            false
        } else {
            uiSettingsManager.shouldShowScreenHints(session.userId, screen)
        }
    }
    val onDismissScreenHints: (HintScreenKey) -> Unit = { screen ->
        uiSettingsManager.hideScreenHints(session.userId, screen)
        hintsVersion += 1
    }
    val density = LocalDensity.current
    val edgeSwipeModifier = if (isIos && canPop) {
        Modifier.pointerInput(canPop, currentSubScreen, backStack) {
            val edgeWidthPx = with(density) { 32.dp.toPx() }
            val popThresholdPx = with(density) { 72.dp.toPx() }
            var startedFromEdge = false
            var accumulatedDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    startedFromEdge = offset.x <= edgeWidthPx
                    accumulatedDrag = 0f
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (!startedFromEdge) return@detectHorizontalDragGestures
                    if (dragAmount <= 0f) return@detectHorizontalDragGestures
                    accumulatedDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    if (startedFromEdge && accumulatedDrag >= popThresholdPx) {
                        popScreen()
                    }
                    startedFromEdge = false
                    accumulatedDrag = 0f
                },
                onDragCancel = {
                    startedFromEdge = false
                    accumulatedDrag = 0f
                },
            )
        }
    } else {
        Modifier
    }

    PlatformBackHandler(enabled = true) {
        if (canPop) {
            popScreen()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        mainScreenTitle(currentSubScreen),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    if (canPop) {
                        IconButton(onClick = { popScreen() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    } else {
                        IconButton(onClick = { navigateTo("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                },
                actions = {
                    if (currentSubScreen == "dashboard" && !canPop) {
                        IconButton(
                            onClick = { scope.launch { onLogout() } },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Выход",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(edgeSwipeModifier)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentSubScreen == "dashboard") {
                    UserInfoHeaderShared(session)
                    if (roles.size > 1) {
                        RoleSwitcherShared(roles, selectedRole ?: roles.first()) {
                            selectedRole = it
                            backStack = emptyList()
                            navDirection = NavDirection.Backward
                            currentSubScreen = "dashboard"
                            selectedMealType = ""
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentSubScreen,
                        transitionSpec = { navContentTransition(navDirection) },
                        label = "main-shell-content",
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { targetSubScreen ->
                        if (targetSubScreen == "settings") {
                            SettingsScreen(
                                userId = session.userId,
                                token = session.token,
                                roles = session.roles,
                                uiSettingsManager = uiSettingsManager,
                                notificationRepository = notificationRepository,
                                onBack = { popScreen() }
                            )
                        } else {
                            when (selectedRole ?: roles.firstOrNull()) {
                                UserRole.STUDENT -> StudentFlowShared(
                                    session = session,
                                    currentSubScreen = targetSubScreen,
                                    selectedMealType = selectedMealType,
                                    studentRepository = studentRepository,
                                    studentViewModel = studentViewModel,
                                    showHints = shouldShowScreenHints,
                                    onDismissHints = onDismissScreenHints,
                                    onNavigate = { navigateTo(it) },
                                    onMealSelect = {
                                        selectedMealType = it
                                        navigateTo("qr")
                                    }
                                )

                                UserRole.CHEF -> ChefFlowShared(
                                    session = session,
                                    currentSubScreen = targetSubScreen,
                                    chefRepository = chefRepository,
                                    chefViewModel = chefViewModel,
                                    showHints = shouldShowScreenHints,
                                    onDismissHints = onDismissScreenHints,
                                    onNavigate = { navigateTo(it) }
                                )

                                UserRole.REGISTRATOR -> RegistratorFlowShared(
                                    session = session,
                                    currentSubScreen = targetSubScreen,
                                    showHints = shouldShowScreenHints,
                                    onDismissHints = onDismissScreenHints,
                                    onNavigate = { navigateTo(it) },
                                    onNavigateBack = { popScreen() },
                                )

                                UserRole.CURATOR -> CuratorFlowShared(
                                    session,
                                    targetSubScreen,
                                    shouldShowScreenHints,
                                    onDismissScreenHints,
                                ) { navigateTo(it) }

                                UserRole.ADMIN -> AdminFlowShared(
                                    session,
                                    targetSubScreen,
                                    shouldShowScreenHints,
                                    onDismissScreenHints,
                                ) { navigateTo(it) }

                                else -> Text(
                                    "Роль не определена",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSwitcherShared(
    roles: List<UserRole>,
    selectedRole: UserRole,
    onSelect: (UserRole) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            "Выберите вашу роль для работы:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roles.forEach { role ->
                FilterChip(
                    selected = role == selectedRole,
                    onClick = { onSelect(role) },
                    label = { Text(role.titleRu()) })
            }
        }
    }
}

@Composable
fun UserInfoHeaderShared(session: UserSession) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).springEntrance(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.4f
            )
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "${session.surname} ${session.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    session.roles.joinToString(", ") { it.titleRu() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StudentFlowShared(
    session: UserSession,
    currentSubScreen: String,
    selectedMealType: String,
    studentRepository: StudentRepository,
    studentViewModel: StudentViewModel,
    showHints: (HintScreenKey) -> Boolean,
    onDismissHints: (HintScreenKey) -> Unit,
    onNavigate: (String) -> Unit,
    onMealSelect: (String) -> Unit
) {
    when (currentSubScreen) {
        "dashboard" -> StudentDashboardShared(
            token = session.token,
            studentRepository = studentRepository,
            onCouponsClick = { onNavigate("coupons") },
            onMenuClick = { onNavigate("menu") },
            showHints = showHints(HintScreenKey.STUDENT_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.STUDENT_DASHBOARD) },
        )

        "coupons" -> MyCouponsScreen(
            token = session.token,
            studentRepository = studentRepository,
            viewModel = studentViewModel,
            showHints = showHints(HintScreenKey.STUDENT_COUPONS),
            onDismissHints = { onDismissHints(HintScreenKey.STUDENT_COUPONS) },
            onCouponClick = onMealSelect
        )

        "qr" -> StudentQrScreenShared(
            session = session,
            mealType = selectedMealType,
            viewModel = studentViewModel,
            showHints = showHints(HintScreenKey.STUDENT_QR),
            onDismissHints = { onDismissHints(HintScreenKey.STUDENT_QR) },
        )
        "menu" -> MenuScreenV2(
            token = session.token,
            studentRepository = studentRepository,
            showHints = showHints(HintScreenKey.STUDENT_MENU),
            onDismissHints = { onDismissHints(HintScreenKey.STUDENT_MENU) },
        )
        else -> StudentDashboardShared(
            token = session.token,
            studentRepository = studentRepository,
            onCouponsClick = { onNavigate("coupons") },
            onMenuClick = { onNavigate("menu") },
            showHints = showHints(HintScreenKey.STUDENT_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.STUDENT_DASHBOARD) },
        )
    }
}

@Composable
fun ChefFlowShared(
    session: UserSession,
    currentSubScreen: String,
    chefRepository: ChefRepository,
    chefViewModel: ChefViewModel,
    showHints: (HintScreenKey) -> Boolean,
    onDismissHints: (HintScreenKey) -> Unit,
    onNavigate: (String) -> Unit
) {
    when (currentSubScreen) {
        "dashboard" -> ChefDashboardShared(
            onScannerClick = { onNavigate("scanner") },
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") },
            showHints = showHints(HintScreenKey.CHEF_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.CHEF_DASHBOARD) },
        )

        "scanner" -> ChefScannerScreenShared(
            token = session.token,
            viewModel = chefViewModel,
            showHints = showHints(HintScreenKey.CHEF_SCANNER),
            onDismissHints = { onDismissHints(HintScreenKey.CHEF_SCANNER) },
        )

        "menu_manage" -> ChefMenuManageScreenV2(
            token = session.token,
            chefRepository = chefRepository,
            showHints = showHints(HintScreenKey.CHEF_MENU_MANAGE),
            onDismissHints = { onDismissHints(HintScreenKey.CHEF_MENU_MANAGE) },
        )

        "stats" -> ChefStatsScreenShared(
            chefRepository = chefRepository,
            showHints = showHints(HintScreenKey.CHEF_STATS),
            onDismissHints = { onDismissHints(HintScreenKey.CHEF_STATS) },
        )
        else -> ChefDashboardShared(
            onScannerClick = { onNavigate("scanner") },
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") },
            showHints = showHints(HintScreenKey.CHEF_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.CHEF_DASHBOARD) },
        )
    }
}

@Composable
fun RegistratorFlowShared(
    session: UserSession,
    currentSubScreen: String,
    showHints: (HintScreenKey) -> Boolean,
    onDismissHints: (HintScreenKey) -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val registratorRepository = remember { RegistratorRepository() }
    var createUserInitialGroupId by remember(session.userId) { mutableStateOf<Int?>(null) }
    var usersReloadKey by remember(session.userId) { mutableIntStateOf(0) }
    when (currentSubScreen) {
        "dashboard" -> RegistratorDashboardShared(
            onUsersClick = { onNavigate("users") },
            onGroupsClick = { onNavigate("groups") },
            showHints = showHints(HintScreenKey.REGISTRATOR_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.REGISTRATOR_DASHBOARD) },
        )
        "users" -> RegistratorUsersScreen(
            token = session.token,
            registratorRepository = registratorRepository,
            reloadKey = usersReloadKey,
            showHints = showHints(HintScreenKey.REGISTRATOR_USERS),
            onDismissHints = { onDismissHints(HintScreenKey.REGISTRATOR_USERS) },
            onCreateUserClick = { groupId ->
                createUserInitialGroupId = groupId
                onNavigate("users_create")
            }
        )
        "users_create" -> RegistratorCreateUserRoute(
            token = session.token,
            registratorRepository = registratorRepository,
            initialGroupId = createUserInitialGroupId,
            showHints = showHints(HintScreenKey.REGISTRATOR_USER_CREATE),
            onDismissHints = { onDismissHints(HintScreenKey.REGISTRATOR_USER_CREATE) },
            onBack = {
                createUserInitialGroupId = null
                onNavigateBack()
            },
            onUserCreated = {
                createUserInitialGroupId = null
                usersReloadKey += 1
                onNavigateBack()
            }
        )

        "groups" -> RegistratorGroupsScreen(
            token = session.token,
            registratorRepository = registratorRepository,
            showHints = showHints(HintScreenKey.REGISTRATOR_GROUPS),
            onDismissHints = { onDismissHints(HintScreenKey.REGISTRATOR_GROUPS) },
        )

        else -> RegistratorDashboardShared(
            onUsersClick = { onNavigate("users") },
            onGroupsClick = { onNavigate("groups") },
            showHints = showHints(HintScreenKey.REGISTRATOR_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.REGISTRATOR_DASHBOARD) },
        )
    }
}

@Composable
fun CuratorFlowShared(
    session: UserSession,
    currentSubScreen: String,
    showHints: (HintScreenKey) -> Boolean,
    onDismissHints: (HintScreenKey) -> Unit,
    onNavigate: (String) -> Unit
) {
    val curatorRepository = remember { CuratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> CuratorDashboardShared(
            token = session.token,
            curatorRepository = curatorRepository,
            onRosterClick = { onNavigate("roster") },
            onStatsClick = { onNavigate("stats") },
            onCategoriesClick = { onNavigate("categories") },
            onReportsClick = { onNavigate("reports") },
            showHints = showHints(HintScreenKey.CURATOR_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_DASHBOARD) },
        )

        "roster" -> CuratorRosterScreen(
            token = session.token,
            curatorId = session.userId,
            curatorRepository = curatorRepository,
            showHints = showHints(HintScreenKey.CURATOR_ROSTER),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_ROSTER) },
            onNavigateToCategories = { onNavigate("categories") },
        )

        "stats" -> CuratorStatsScreen(
            token = session.token,
            curatorId = session.userId,
            curatorRepository = curatorRepository,
            showHints = showHints(HintScreenKey.CURATOR_STATS),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_STATS) },
        )

        "categories" -> CuratorCategoriesScreen(
            token = session.token,
            curatorId = session.userId,
            curatorRepository = curatorRepository,
            showHints = showHints(HintScreenKey.CURATOR_CATEGORIES),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_CATEGORIES) },
        )

        "reports" -> CuratorReportsScreen(
            token = session.token,
            curatorId = session.userId,
            curatorRepository = curatorRepository,
            showHints = showHints(HintScreenKey.CURATOR_REPORTS),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_REPORTS) },
        )

        else -> CuratorDashboardShared(
            token = session.token,
            curatorRepository = curatorRepository,
            onRosterClick = { onNavigate("roster") },
            onStatsClick = { onNavigate("stats") },
            onCategoriesClick = { onNavigate("categories") },
            onReportsClick = { onNavigate("reports") },
            showHints = showHints(HintScreenKey.CURATOR_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.CURATOR_DASHBOARD) },
        )
    }
}

@Composable
fun AdminFlowShared(
    session: UserSession,
    currentSubScreen: String,
    showHints: (HintScreenKey) -> Boolean,
    onDismissHints: (HintScreenKey) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val adminRepository = remember { AdminRepository() }
    val registratorRepository = remember { RegistratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> AdminDashboardShared(
            onReportsClick = { onNavigate("reports") },
            showHints = showHints(HintScreenKey.ADMIN_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.ADMIN_DASHBOARD) },
        )
        "reports" -> AdminReportsScreen(
            token = session.token,
            adminRepository = adminRepository,
            loadGroups = { registratorRepository.getGroups(session.token) },
            showHints = showHints(HintScreenKey.ADMIN_REPORTS),
            onDismissHints = { onDismissHints(HintScreenKey.ADMIN_REPORTS) },
        )
        else -> AdminDashboardShared(
            onReportsClick = { onNavigate("reports") },
            showHints = showHints(HintScreenKey.ADMIN_DASHBOARD),
            onDismissHints = { onDismissHints(HintScreenKey.ADMIN_DASHBOARD) },
        )
    }
}

@Composable
private fun ScreenHintBlock(
    screen: HintScreenKey,
    showHints: Boolean,
    onDismissHints: () -> Unit,
) {
    if (!showHints) return
    val hint = remember(screen) { HintCatalog.content(screen) }
    HowItWorksCard(
        title = hint.title,
        steps = hint.steps,
        note = hint.note,
        onDismiss = onDismissHints,
    )
}

@Composable
fun StudentDashboardShared(
    token: String,
    studentRepository: StudentRepository,
    onCouponsClick: () -> Unit,
    onMenuClick: () -> Unit,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    var mealsResponse by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(token) {
        studentRepository.getMealsToday(token).onSuccess { mealsResponse = it }.onFailure { }
        isLoading = false
    }
    DashboardLayoutShared(title = "Кабинет Студента") {
        ScreenHintBlock(
            screen = HintScreenKey.STUDENT_DASHBOARD,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        if (showHints) {
            Spacer(Modifier.height(12.dp))
        }
        if (isLoading) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (mealsResponse != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).springEntrance(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Моё питание на сегодня",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    MealRightItemShared("Завтрак", mealsResponse!!.isBreakfastAllowed)
                    MealRightItemShared("Обед", mealsResponse!!.isLunchAllowed)
                }
            }
        }
        AdaptiveTwoButtonRowShared(
            firstText = "Мои талоны",
            firstIcon = Icons.Default.ConfirmationNumber,
            onFirstClick = onCouponsClick,
            secondText = "Меню",
            secondIcon = Icons.Default.RestaurantMenu,
            onSecondClick = onMenuClick
        )
    }
}

@Composable
fun MealRightItemShared(label: String, isAllowed: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Surface(
            color = if (isAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(
                alpha = 0.1f
            ), shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                if (isAllowed) "Разрешено" else "Нет",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AdaptiveTwoButtonRowShared(
    firstText: String,
    firstIcon: ImageVector,
    onFirstClick: () -> Unit,
    secondText: String,
    secondIcon: ImageVector,
    onSecondClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 520.dp
        if (isCompact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardButtonShared(firstText, firstIcon, onFirstClick, Modifier.fillMaxWidth())
                DashboardButtonShared(
                    secondText,
                    secondIcon,
                    onSecondClick,
                    Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardButtonShared(firstText, firstIcon, onFirstClick, Modifier.weight(1f))
                DashboardButtonShared(secondText, secondIcon, onSecondClick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ChefDashboardShared(
    onScannerClick: () -> Unit,
    onMenuManageClick: () -> Unit,
    onStatsClick: () -> Unit,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    DashboardLayoutShared("Кабинет Повара") {
        ScreenHintBlock(
            screen = HintScreenKey.CHEF_DASHBOARD,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        if (showHints) {
            Spacer(Modifier.height(12.dp))
        }
        DashboardButtonShared(
            text = "Сканер QR",
            icon = Icons.Default.QrCodeScanner,
            onClick = onScannerClick,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        AdaptiveTwoButtonRowShared(
            firstText = "Меню",
            firstIcon = Icons.Default.Edit,
            onFirstClick = onMenuManageClick,
            secondText = "История",
            secondIcon = Icons.Default.History,
            onSecondClick = onStatsClick
        )
    }
}

@Composable
fun ChefStatsScreenShared(
    chefRepository: ChefRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    val history by chefRepository.getScanHistory().collectAsState(initial = emptyList())
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            "Статистика повара",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        ScreenHintBlock(
            screen = HintScreenKey.CHEF_STATS,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCardShared(
                "Всего",
                history.size.toString(),
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f)
            )
            StatCardShared(
                "Успех",
                history.count { it.status == "success" }.toString(),
                Color(0xFF238636),
                Modifier.weight(1f)
            )
            StatCardShared(
                "Ошибки",
                history.count { it.status == "error" }.toString(),
                MaterialTheme.colorScheme.error,
                Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "История сканирований",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { HistoryItemShared(it) }
        }
    }
}

@Composable
fun StatCardShared(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.springEntrance(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun HistoryItemShared(item: SharedScannedQrRecord) {
    val dt = remember(item.timestamp) {
        Instant.fromEpochMilliseconds(item.timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val ts = "${dt.dayOfMonth.toString().padStart(2, '0')}.${
        dt.monthNumber.toString().padStart(2, '0')
    } ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status == "success") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.3f
            )
        )
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    item.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.mealType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                ts,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RegistratorDashboardShared(
    onUsersClick: () -> Unit,
    onGroupsClick: () -> Unit,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    DashboardLayoutShared("Кабинет Регистратора") {
        ScreenHintBlock(
            screen = HintScreenKey.REGISTRATOR_DASHBOARD,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        if (showHints) {
            Spacer(Modifier.height(12.dp))
        }
        AdaptiveTwoButtonRowShared(
            firstText = "Пользователи",
            firstIcon = Icons.Default.Group,
            onFirstClick = onUsersClick,
            secondText = "Группы",
            secondIcon = Icons.Default.Class,
            onSecondClick = onGroupsClick
        )
    }
}

@Composable
fun CuratorDashboardShared(
    token: String,
    curatorRepository: CuratorRepository,
    onRosterClick: () -> Unit,
    onStatsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onReportsClick: () -> Unit,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    var notification by remember { mutableStateOf<RosterDeadlineNotificationDto?>(null) }
    LaunchedEffect(token) {
        curatorRepository.getRosterDeadlineNotification(token).onSuccess { notification = it }
    }
    DashboardLayoutShared("Кабинет Куратора") {
        ScreenHintBlock(
            screen = HintScreenKey.CURATOR_DASHBOARD,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        if (showHints) {
            Spacer(Modifier.height(12.dp))
        }
        notification?.let { data ->
            val showCard = data.needsReminder || !data.reason.isNullOrBlank()
            if (showCard) {
                val title = if (data.needsReminder) "Нужно заполнить табель" else "Уведомление"
                val body = when {
                    data.needsReminder && data.deadlineDate != null && data.daysUntilDeadline != null -> "Заполните табель на следующую неделю. Дедлайн: ${data.deadlineDate} (через ${data.daysUntilDeadline} дн.)"
                    data.needsReminder && data.deadlineDate != null -> "Заполните табель на следующую неделю. Дедлайн: ${data.deadlineDate}"
                    data.needsReminder -> "Заполните табель на следующую неделю."
                    !data.reason.isNullOrBlank() -> data.reason ?: ""
                    else -> ""
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (body.isNotBlank()) {
                            Spacer(Modifier.height(8.dp)); Text(body)
                        }
                    }
                }
            }
        }
        AdaptiveTwoButtonRowShared(
            firstText = "Табель",
            firstIcon = Icons.Default.ListAlt,
            onFirstClick = onRosterClick,
            secondText = "Статистика",
            secondIcon = Icons.Default.BarChart,
            onSecondClick = onStatsClick
        )
        Spacer(Modifier.height(12.dp))
        AdaptiveTwoButtonRowShared(
            firstText = "Категории",
            firstIcon = Icons.Default.Badge,
            onFirstClick = onCategoriesClick,
            secondText = "Отчеты",
            secondIcon = Icons.Default.Assessment,
            onSecondClick = onReportsClick
        )
    }
}

@Composable
fun AdminDashboardShared(
    onReportsClick: () -> Unit,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) =
    DashboardLayoutShared("Кабинет Администратора") {
        ScreenHintBlock(
            screen = HintScreenKey.ADMIN_DASHBOARD,
            showHints = showHints,
            onDismissHints = onDismissHints,
        )
        if (showHints) {
            Spacer(Modifier.height(12.dp))
        }
        DashboardButtonShared(
            "Отчеты",
            Icons.Default.Assessment,
            onReportsClick,
            Modifier.fillMaxWidth()
        )
    }

@Composable
fun DashboardLayoutShared(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.springEntrance()
        )
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun DashboardButtonShared(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 92.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
