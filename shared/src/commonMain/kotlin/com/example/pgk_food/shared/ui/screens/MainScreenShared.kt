package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.repository.*
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.runtime.MainLoopSnapshot
import com.example.pgk_food.shared.runtime.MainLoopStateStore
import com.example.pgk_food.shared.ui.viewmodels.ChefViewModel
import com.example.pgk_food.shared.ui.viewmodels.StudentViewModel
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
    "groups" -> "ГРУППЫ"
    "reports" -> "ОТЧЕТЫ"
    "menu" -> "МЕНЮ В СТОЛОВОЙ"
    "coupons" -> "МОИ ТАЛОНЫ"
    "qr" -> "МОЙ QR-КОД"
    "scanner" -> "СКАНЕР QR"
    "menu_manage" -> "УПРАВЛЕНИЕ МЕНЮ"
    "settings" -> "НАСТРОЙКИ"
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
        UserRole.REGISTRATOR -> setOf("users", "groups")
        UserRole.CURATOR -> setOf("roster", "stats")
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
    val studentViewModel = remember(session.userId) { StudentViewModel(authRepository, studentRepository) }
    val chefViewModel = remember(session.userId) { ChefViewModel(authRepository, chefRepository, networkMonitor) }

    val roles = session.roles
    var currentSubScreen by remember(session.userId) { mutableStateOf("dashboard") }
    var selectedMealType by remember(session.userId) { mutableStateOf("") }
    var selectedRole by remember(session.userId) { mutableStateOf<UserRole?>(null) }
    var showHints by remember(session.userId) { mutableStateOf(uiSettingsManager.shouldShowHints(session.userId)) }
    var isSnapshotRestored by remember(session.userId) { mutableStateOf(false) }

    LaunchedEffect(session.userId) {
        val restored = mainLoopStateStore.restore(session.userId)
        val restoredRole = parseRoleOrNull(restored?.selectedRole)
            ?.takeIf { it in roles }
        val activeRole = restoredRole ?: roles.firstOrNull()
        selectedRole = activeRole
        currentSubScreen = resolveSubScreenForRole(activeRole, restored?.currentSubScreen)
        selectedMealType = restored?.selectedMealType.orEmpty()
        showHints = uiSettingsManager.shouldShowHints(session.userId)
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
        }
    }

    LaunchedEffect(session.userId, selectedRole, currentSubScreen, selectedMealType, isSnapshotRestored) {
        if (!isSnapshotRestored) return@LaunchedEffect
        mainLoopStateStore.save(
            session.userId,
            MainLoopSnapshot(
                selectedRole = selectedRole?.name,
                currentSubScreen = currentSubScreen,
                selectedMealType = selectedMealType,
            )
        )
    }

    LaunchedEffect(currentSubScreen, session.userId) {
        if (currentSubScreen != "settings") {
            showHints = uiSettingsManager.shouldShowHints(session.userId)
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
                    if (currentSubScreen != "dashboard") {
                        IconButton(onClick = { currentSubScreen = "dashboard" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentSubScreen = "settings" },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                    IconButton(
                        onClick = { scope.launch { onLogout() } },
                        modifier = Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Выход", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                UserInfoHeaderShared(session)
                if (roles.size > 1) {
                    RoleSwitcherShared(roles, selectedRole ?: roles.first()) {
                        selectedRole = it
                        currentSubScreen = "dashboard"
                        selectedMealType = ""
                    }
                }
                if (currentSubScreen == "settings") {
                    SettingsScreen(
                        userId = session.userId,
                        token = session.token,
                        roles = session.roles,
                        uiSettingsManager = uiSettingsManager,
                        notificationRepository = notificationRepository,
                        onBack = { currentSubScreen = "dashboard" }
                    )
                } else {
                    when (selectedRole ?: roles.firstOrNull()) {
                        UserRole.STUDENT -> StudentFlowShared(
                            session = session,
                            currentSubScreen = currentSubScreen,
                            selectedMealType = selectedMealType,
                            studentRepository = studentRepository,
                            studentViewModel = studentViewModel,
                            showHints = showHints,
                            onHideHints = {
                                uiSettingsManager.hideHints(session.userId)
                                showHints = uiSettingsManager.shouldShowHints(session.userId)
                            },
                            onNavigate = { currentSubScreen = it },
                            onMealSelect = {
                                selectedMealType = it
                                currentSubScreen = "qr"
                            }
                        )
                        UserRole.CHEF -> ChefFlowShared(
                            session = session,
                            currentSubScreen = currentSubScreen,
                            chefRepository = chefRepository,
                            chefViewModel = chefViewModel,
                            showHints = showHints,
                            onHideHints = {
                                uiSettingsManager.hideHints(session.userId)
                                showHints = uiSettingsManager.shouldShowHints(session.userId)
                            },
                            onNavigate = { currentSubScreen = it }
                        )
                        UserRole.REGISTRATOR -> RegistratorFlowShared(session, currentSubScreen) { currentSubScreen = it }
                        UserRole.CURATOR -> CuratorFlowShared(session, currentSubScreen) { currentSubScreen = it }
                        UserRole.ADMIN -> AdminFlowShared(session, currentSubScreen) { currentSubScreen = it }
                        else -> Text("Роль не определена", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSwitcherShared(roles: List<UserRole>, selectedRole: UserRole, onSelect: (UserRole) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Выберите вашу роль для работы:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roles.forEach { role ->
                FilterChip(selected = role == selectedRole, onClick = { onSelect(role) }, label = { Text(role.titleRu()) })
            }
        }
    }
}

@Composable
fun UserInfoHeaderShared(session: UserSession) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("${session.surname} ${session.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(session.roles.joinToString(", ") { it.titleRu() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    showHints: Boolean,
    onHideHints: () -> Unit,
    onNavigate: (String) -> Unit,
    onMealSelect: (String) -> Unit
) {
    when (currentSubScreen) {
        "dashboard" -> StudentDashboardShared(session.token, studentRepository, { onNavigate("coupons") }, { onNavigate("menu") })
        "coupons" -> MyCouponsScreen(
            token = session.token,
            studentRepository = studentRepository,
            viewModel = studentViewModel,
            showHints = showHints,
            onHideHints = onHideHints,
            onCouponClick = onMealSelect
        )
        "qr" -> StudentQrScreenShared(session, selectedMealType, studentViewModel)
        "menu" -> MenuScreenV2(token = session.token, studentRepository = studentRepository)
        else -> StudentDashboardShared(session.token, studentRepository, { onNavigate("coupons") }, { onNavigate("menu") })
    }
}

@Composable
fun ChefFlowShared(
    session: UserSession,
    currentSubScreen: String,
    chefRepository: ChefRepository,
    chefViewModel: ChefViewModel,
    showHints: Boolean,
    onHideHints: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (currentSubScreen) {
        "dashboard" -> ChefDashboardShared({ onNavigate("scanner") }, { onNavigate("menu_manage") }, { onNavigate("stats") })
        "scanner" -> ChefScannerScreenShared(
            token = session.token,
            viewModel = chefViewModel,
            showHints = showHints,
            onHideHints = onHideHints
        )
        "menu_manage" -> ChefMenuManageScreenV2(token = session.token, chefRepository = chefRepository)
        "stats" -> ChefStatsScreenShared(chefRepository)
        else -> ChefDashboardShared({ onNavigate("scanner") }, { onNavigate("menu_manage") }, { onNavigate("stats") })
    }
}

@Composable
fun RegistratorFlowShared(session: UserSession, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val registratorRepository = remember { RegistratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> RegistratorDashboardShared({ onNavigate("users") }, { onNavigate("groups") })
        "users" -> RegistratorUsersScreen(token = session.token, registratorRepository = registratorRepository)
        "groups" -> RegistratorGroupsScreen(token = session.token, registratorRepository = registratorRepository)
        else -> RegistratorDashboardShared({ onNavigate("users") }, { onNavigate("groups") })
    }
}

@Composable
fun CuratorFlowShared(session: UserSession, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val curatorRepository = remember { CuratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> CuratorDashboardShared(session.token, curatorRepository, { onNavigate("roster") }, { onNavigate("stats") })
        "roster" -> CuratorRosterScreen(token = session.token, curatorId = session.userId, curatorRepository = curatorRepository)
        "stats" -> CuratorStatsScreen(token = session.token, curatorId = session.userId, curatorRepository = curatorRepository)
        else -> CuratorDashboardShared(session.token, curatorRepository, { onNavigate("roster") }, { onNavigate("stats") })
    }
}

@Composable
fun AdminFlowShared(session: UserSession, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val adminRepository = remember { AdminRepository() }
    when (currentSubScreen) {
        "dashboard" -> AdminDashboardShared { onNavigate("reports") }
        "reports" -> AdminReportsScreen(token = session.token, adminRepository = adminRepository)
        else -> AdminDashboardShared { onNavigate("reports") }
    }
}

@Composable
fun StudentDashboardShared(token: String, studentRepository: StudentRepository, onCouponsClick: () -> Unit, onMenuClick: () -> Unit) {
    var mealsResponse by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(token) {
        studentRepository.getMealsToday(token).onSuccess { mealsResponse = it }.onFailure { }
        isLoading = false
    }
    DashboardLayoutShared(title = "Кабинет Студента") {
        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (mealsResponse != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Моё питание на сегодня", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    MealRightItemShared("Завтрак", mealsResponse!!.isBreakfastAllowed)
                    MealRightItemShared("Обед", mealsResponse!!.isLunchAllowed)
                    MealRightItemShared("Ужин", mealsResponse!!.isDinnerAllowed)
                    MealRightItemShared("Полдник", mealsResponse!!.isSnackAllowed)
                    MealRightItemShared("Спец. питание", mealsResponse!!.isSpecialAllowed)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButtonShared("Мои талоны", Icons.Default.ConfirmationNumber, onCouponsClick, Modifier.weight(1f))
            DashboardButtonShared("Меню", Icons.Default.RestaurantMenu, onMenuClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun MealRightItemShared(label: String, isAllowed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Surface(color = if (isAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
            Text(if (isAllowed) "Разрешено" else "Нет", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable fun ChefDashboardShared(onScannerClick: () -> Unit, onMenuManageClick: () -> Unit, onStatsClick: () -> Unit) {
    DashboardLayoutShared("Кабинет Повара") {
        DashboardButtonShared("Сканер QR", Icons.Default.QrCodeScanner, onScannerClick)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButtonShared("Меню", Icons.Default.Edit, onMenuManageClick, Modifier.weight(1f))
            DashboardButtonShared("История", Icons.Default.History, onStatsClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun ChefStatsScreenShared(chefRepository: ChefRepository) {
    val history by chefRepository.getScanHistory().collectAsState(initial = emptyList())
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Статистика повара", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCardShared("Всего", history.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCardShared("Успех", history.count { it.status == "success" }.toString(), Color(0xFF238636), Modifier.weight(1f))
            StatCardShared("Ошибки", history.count { it.status == "error" }.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text("История сканирований", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(history) { HistoryItemShared(it) } }
    }
}

@Composable fun StatCardShared(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable fun HistoryItemShared(item: SharedScannedQrRecord) {
    val dt = remember(item.timestamp) { Instant.fromEpochMilliseconds(item.timestamp).toLocalDateTime(TimeZone.currentSystemDefault()) }
    val ts = "${dt.dayOfMonth.toString().padStart(2,'0')}.${dt.monthNumber.toString().padStart(2,'0')} ${dt.hour.toString().padStart(2,'0')}:${dt.minute.toString().padStart(2,'0')}"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (item.status == "success") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(item.studentName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(item.mealType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(ts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun RegistratorDashboardShared(onUsersClick: () -> Unit, onGroupsClick: () -> Unit) {
    DashboardLayoutShared("Кабинет Регистратора") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButtonShared("Пользователи", Icons.Default.Group, onUsersClick, Modifier.weight(1f))
            DashboardButtonShared("Группы", Icons.Default.Class, onGroupsClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun CuratorDashboardShared(token: String, curatorRepository: CuratorRepository, onRosterClick: () -> Unit, onStatsClick: () -> Unit) {
    var notification by remember { mutableStateOf<RosterDeadlineNotificationDto?>(null) }
    LaunchedEffect(token) { curatorRepository.getRosterDeadlineNotification(token).onSuccess { notification = it } }
    DashboardLayoutShared("Кабинет Куратора") {
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (body.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(body) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButtonShared("Табель", Icons.Default.ListAlt, onRosterClick, Modifier.weight(1f))
            DashboardButtonShared("Статистика", Icons.Default.BarChart, onStatsClick, Modifier.weight(1f))
        }
    }
}

@Composable fun AdminDashboardShared(onReportsClick: () -> Unit) = DashboardLayoutShared("Кабинет Администратора") { DashboardButtonShared("Отчеты", Icons.Default.Assessment, onReportsClick) }

@Composable fun DashboardLayoutShared(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun DashboardButtonShared(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
