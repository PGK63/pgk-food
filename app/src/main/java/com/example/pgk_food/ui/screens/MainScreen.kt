package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.repository.*
import com.example.pgk_food.model.UserRole
import com.example.pgk_food.ui.theme.HeroCardShape
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.ShardShape
import com.example.pgk_food.ui.theme.springEntrance
import com.example.pgk_food.ui.theme.springScale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.pgk_food.ui.theme.TagShape
import com.example.pgk_food.ui.viewmodels.CuratorViewModel
import com.example.pgk_food.ui.viewmodels.ChefViewModel
import com.example.pgk_food.ui.viewmodels.StudentViewModel
import com.example.pgk_food.util.NetworkMonitor
import com.example.pgk_food.util.UiSettingsManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authRepository: AuthRepository,
    database: AppDatabase,
    networkMonitor: NetworkMonitor,
    uiSettingsManager: UiSettingsManager,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val userSession by authRepository.getUserSession().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentSubScreen by remember { mutableStateOf("dashboard") }
    var selectedMealType by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    val titleText = when (currentSubScreen) {
                        "dashboard" -> "ПГК ПИТАНИЕ"
                        "roster" -> "ОТМЕТКА ПИТАНИЯ"
                        "stats" -> "СТАТИСТИКА"
                        "users" -> "ПОЛЬЗОВАТЕЛИ"
                        "groups" -> "ГРУППЫ"
                        "reports" -> "ОТЧЕТЫ"
                        "menu" -> "УПРАВЛЕНИЕ МЕНЮ"
                        "student_menu" -> "МЕНЮ В СТОЛОВОЙ"
                        "student_scan", "qr" -> "МОЙ QR-КОД"
                        "chef_scan", "scanner" -> "СКАНЕР QR"
                        "chef_stats" -> "СТАТИСТИКА ПОВАРА"
                        "menu_manage" -> "УПРАВЛЕНИЕ МЕНЮ"
                        "coupons" -> "МОИ ТАЛОНЫ"
                        else -> ""
                    }
                    Text(
                        titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    if (currentSubScreen != "dashboard") {
                        IconButton(onClick = { currentSubScreen = "dashboard" }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                        }
                    } else {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
                        }
                    }
                },
                actions = {
                    if (currentSubScreen == "dashboard") {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    authRepository.logout()
                                    onLogout()
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                Icons.Rounded.ExitToApp, 
                                contentDescription = "Выход",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            userSession?.let { session ->
                val roles = session.roles
                val showHintsState = remember(session.userId) {
                    mutableStateOf(uiSettingsManager.shouldShowHints(session.userId))
                }
                val showHints = showHintsState.value
                val onHideHints = {
                    uiSettingsManager.hideHints(session.userId)
                    showHintsState.value = false
                }

                DisposableEffect(lifecycleOwner, session.userId) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            showHintsState.value = uiSettingsManager.shouldShowHints(session.userId)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(roles) {
                    if (selectedRole == null || selectedRole !in roles) {
                        selectedRole = roles.firstOrNull()
                    }
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with user info - only on dashboard
                    if (currentSubScreen == "dashboard") {
                        UserInfoHeader(session)
                        if (roles.size > 1) {
                            RoleSwitcher(
                                roles = roles,
                                selectedRole = selectedRole ?: roles.first(),
                                onSelect = {
                                    selectedRole = it
                                    currentSubScreen = "dashboard"
                                    selectedMealType = ""
                                }
                            )
                        }
                    }

                    val activeRole = selectedRole ?: roles.firstOrNull()

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeRole) {
                            UserRole.STUDENT -> StudentFlow(
                                session,
                                currentSubScreen,
                                selectedMealType,
                                authRepository,
                                database,
                                showHints,
                                onHideHints,
                                onNavigate = { currentSubScreen = it },
                                onMealSelect = { selectedMealType = it }
                            )
                            UserRole.CHEF -> ChefFlow(
                                session,
                                currentSubScreen,
                                database,
                                networkMonitor,
                                showHints,
                                onHideHints,
                                onNavigate = { currentSubScreen = it }
                            )
                            UserRole.REGISTRATOR -> RegistratorFlow(
                                session,
                                currentSubScreen,
                                showHints,
                                onHideHints,
                                onNavigate = { currentSubScreen = it }
                            )
                            UserRole.CURATOR -> CuratorFlow(
                                session,
                                currentSubScreen,
                                authRepository,
                                onNavigate = { currentSubScreen = it }
                            )
                            UserRole.ADMIN -> AdminFlow(
                                session,
                                currentSubScreen,
                                onNavigate = { currentSubScreen = it }
                            )
                            else -> Text("Роль не определена", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun UserRole.titleRu(): String = when (this) {
    UserRole.STUDENT -> "Студент"
    UserRole.CHEF -> "Повар"
    UserRole.REGISTRATOR -> "Регистратор"
    UserRole.CURATOR -> "Куратор"
    UserRole.ADMIN -> "Администратор"
}

@Composable
fun RoleSwitcher(
    roles: List<UserRole>,
    selectedRole: UserRole,
    onSelect: (UserRole) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = "Выберите вашу роль для работы:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roles.forEach { role ->
                val isSelected = role == selectedRole
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(role) },
                    label = { Text(role.titleRu()) },
                    shape = PillShape
                )
            }
        }
    }
}

@Composable
fun UserInfoHeader(session: UserSessionEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .springEntrance(),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${session.surname} ${session.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.roles.joinToString(", ") { it.titleRu() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StudentFlow(
    session: UserSessionEntity,
    currentSubScreen: String,
    selectedMealType: String,
    authRepository: AuthRepository,
    database: AppDatabase,
    showHints: Boolean,
    onHideHints: () -> Unit,
    onNavigate: (String) -> Unit,
    onMealSelect: (String) -> Unit
) {
    val studentRepository = remember(database) { StudentRepository(database.offlineCouponDao()) }
    val studentFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudentViewModel(authRepository, studentRepository) as T
        }
    }
    val studentViewModel: StudentViewModel = viewModel(factory = studentFactory)

    when (currentSubScreen) {
        "dashboard" -> StudentDashboard(
            token = session.token,
            studentRepository = studentRepository,
            onCouponsClick = { onNavigate("coupons") },
            onMenuClick = { onNavigate("menu") }
        )
        "coupons" -> MyCouponsScreen(
            token = session.token,
            studentRepository = studentRepository,
            viewModel = studentViewModel,
            showHints = showHints,
            onHideHints = onHideHints,
            onCouponClick = { 
                onMealSelect(it)
                onNavigate("qr") 
            }
        )
        "qr" -> StudentQrScreen(
            session = session,
            mealType = selectedMealType,
            viewModel = studentViewModel
        )
        "menu" -> MenuScreenV2(token = session.token, studentRepository = studentRepository)
        else -> StudentDashboard(
            token = session.token,
            studentRepository = studentRepository,
            onCouponsClick = { onNavigate("coupons") }, 
            onMenuClick = { onNavigate("menu") }
        )
    }
}

@Composable
fun ChefFlow(
    session: UserSessionEntity, 
    currentSubScreen: String, 
    database: AppDatabase,
    networkMonitor: NetworkMonitor,
    showHints: Boolean,
    onHideHints: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val chefRepository = remember { 
        ChefRepository(
            database.scannedQrDao(), 
            database.transactionDao(),
            database.studentKeyDao(),
            database.permissionCacheDao()
        ) 
    }
    val chefFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val authRepo = AuthRepository(database.userSessionDao())
            return ChefViewModel(authRepo, chefRepository, networkMonitor) as T
        }
    }
    val chefViewModel: ChefViewModel = viewModel<ChefViewModel>(factory = chefFactory)

    when (currentSubScreen) {
        "dashboard" -> ChefDashboard(
            onScannerClick = { onNavigate("scanner") },
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") }
        )
        "scanner" -> ChefScannerScreen(
            token = session.token,
            viewModel = chefViewModel,
            showHints = showHints,
            onHideHints = onHideHints
        )
        "menu_manage" -> ChefMenuManageScreenV2(
            token = session.token,
            chefRepository = chefRepository
        )
        "stats" -> ChefStatsScreen(chefRepository = chefRepository)
        else -> ChefDashboard(
            onScannerClick = { onNavigate("scanner") }, 
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") }
        )
    }
}

@Composable
fun RegistratorFlow(
    session: UserSessionEntity,
    currentSubScreen: String,
    showHints: Boolean,
    onHideHints: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val registratorRepository = remember { RegistratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> RegistratorDashboard(
            onUsersClick = { onNavigate("users") },
            onGroupsClick = { onNavigate("groups") }
        )
        "users" -> RegistratorUsersScreen(
            token = session.token,
            registratorRepository = registratorRepository
        )
        "groups" -> RegistratorGroupsScreen(
            token = session.token,
            registratorRepository = registratorRepository,
            showHints = showHints,
            onHideHints = onHideHints
        )
        else -> RegistratorDashboard(onUsersClick = { onNavigate("users") }, onGroupsClick = { onNavigate("groups") })
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun CuratorFlow(session: UserSessionEntity, currentSubScreen: String, authRepository: AuthRepository, onNavigate: (String) -> Unit) {
    val curatorRepository = remember { CuratorRepository() }
    
    val curatorFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CuratorViewModel(authRepository, curatorRepository) as T
        }
    }
    val curatorViewModel: CuratorViewModel = viewModel(factory = curatorFactory)
    
    when (currentSubScreen) {
        "dashboard" -> CuratorDashboard(
            onRosterClick = { onNavigate("roster") },
            onStatsClick = { onNavigate("stats") }
        )
        "roster" -> CuratorRosterScreen(token = session.token, curatorRepository = curatorRepository)
        "stats" -> CuratorStatsScreen(token = session.token, curatorRepository = curatorRepository)
        else -> CuratorDashboard(
            onRosterClick = { onNavigate("roster") },
            onStatsClick = { onNavigate("stats") }
        )
    }
}

@Composable
fun AdminFlow(session: UserSessionEntity, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val adminRepository = remember { AdminRepository() }
    when (currentSubScreen) {
        "dashboard" -> AdminDashboard(
            onReportsClick = { onNavigate("reports") }
        )
        "reports" -> AdminReportsScreen(token = session.token, adminRepository = adminRepository)
        else -> AdminDashboard(onReportsClick = { onNavigate("reports") })
    }
}

@Composable
fun StudentDashboard(
    token: String,
    studentRepository: StudentRepository,
    onCouponsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    var mealsResponse by remember { mutableStateOf<MealsTodayResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(token) {
        studentRepository.getMealsToday(token).onSuccess {
            mealsResponse = it
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    DashboardLayout(title = "Кабинет Студента") {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (mealsResponse != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .springEntrance(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Моё питание на сегодня",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MealRightItem("Завтрак", mealsResponse!!.isBreakfastAllowed)
                    MealRightItem("Обед", mealsResponse!!.isLunchAllowed)
                    MealRightItem("Ужин", mealsResponse!!.isDinnerAllowed)
                    MealRightItem("Полдник", mealsResponse!!.isSnackAllowed)
                    MealRightItem("Спец. питание", mealsResponse!!.isSpecialAllowed)
                    
                    mealsResponse!!.reason?.let {
                        if (it.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                "Причина: $it", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        AdaptiveTwoButtonRow(
            firstText = "Мои талоны",
            firstIcon = Icons.Rounded.ConfirmationNumber,
            onFirstClick = onCouponsClick,
            secondText = "Меню",
            secondIcon = Icons.Rounded.RestaurantMenu,
            onSecondClick = onMenuClick
        )
    }
}

@Composable
fun MealRightItem(label: String, isAllowed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Surface(
            color = if (isAllowed)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
            shape = TagShape
        ) {
            Text(
                if (isAllowed) "Разрешено" else "Нет",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isAllowed)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun AdaptiveTwoButtonRow(
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
                DashboardButton(firstText, firstIcon, onFirstClick, Modifier.fillMaxWidth())
                DashboardButton(secondText, secondIcon, onSecondClick, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardButton(firstText, firstIcon, onFirstClick, Modifier.weight(1f))
                DashboardButton(secondText, secondIcon, onSecondClick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ChefDashboard(onScannerClick: () -> Unit, onMenuManageClick: () -> Unit, onStatsClick: () -> Unit) {
    DashboardLayout(title = "Кабинет Повара") {
        DashboardButton("Сканер QR", Icons.Rounded.QrCodeScanner, onScannerClick)
        Spacer(modifier = Modifier.height(8.dp))
        AdaptiveTwoButtonRow(
            firstText = "Меню",
            firstIcon = Icons.Rounded.Edit,
            onFirstClick = onMenuManageClick,
            secondText = "История",
            secondIcon = Icons.Rounded.History,
            onSecondClick = onStatsClick
        )
    }
}

@Composable
fun ChefStatsScreen(chefRepository: ChefRepository) {
    val history by chefRepository.getScanHistory()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    val totalScanned = history.size
    val successScanned = history.count { it.status == "success" }
    val errorScanned = history.count { it.status == "error" }

    var expandedCard by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Big stat cards
        item {
            BigStatCard(
                title = "СЕГОДНЯ ОТСКАНИРОВАНО",
                value = totalScanned.toString(),
                suffix = "Раз",
                isExpanded = expandedCard == "today",
                onToggle = { expandedCard = if (expandedCard == "today") null else "today" },
                expandedContent = {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ОТСКАНИРОВАНО",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailStatRow("Успешно:", successScanned.toString())
                        DetailStatRow("Неудачно:", errorScanned.toString())
                    }
                }
            )
        }

        item {
            BigStatCard(
                title = "ВСЕГО УСПЕШНЫХ",
                value = successScanned.toString(),
                suffix = "Раз",
                isExpanded = false,
                onToggle = {}
            )
        }

        item {
            BigStatCard(
                title = "ОШИБОК",
                value = errorScanned.toString(),
                suffix = "Раз",
                isExpanded = false,
                onToggle = {}
            )
        }

        // History section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("История сканирований", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(history) { item ->
            HistoryItem(item)
        }
    }
}

@Composable
private fun BigStatCard(
    title: String,
    value: String,
    suffix: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .springEntrance(),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    suffix,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expandedContent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "подробнее",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onToggle() }
                )
            }
        }

        if (isExpanded && expandedContent != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                expandedContent()
            }
        }
    }
}

@Composable
private fun DetailStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = ShardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun HistoryItem(item: ScannedQrEntity) {
    val sdf = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    val isSuccess = item.status == "success"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Статус: ${if (isSuccess) "принят" else "отклонен"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                sdf.format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RegistratorDashboard(onUsersClick: () -> Unit, onGroupsClick: () -> Unit) {
    DashboardLayout(title = "Кабинет Регистратора") {
        AdaptiveTwoButtonRow(
            firstText = "Пользователи",
            firstIcon = Icons.Rounded.Group,
            onFirstClick = onUsersClick,
            secondText = "Группы",
            secondIcon = Icons.Rounded.Class,
            onSecondClick = onGroupsClick
        )
    }
}

@Composable
fun CuratorDashboard(
    onRosterClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    DashboardLayout(title = "Кабинет Куратора") {
        AdaptiveTwoButtonRow(
            firstText = "Табель",
            firstIcon = Icons.Rounded.ListAlt,
            onFirstClick = onRosterClick,
            secondText = "Статистика",
            secondIcon = Icons.Rounded.BarChart,
            onSecondClick = onStatsClick
        )
    }
}

@Composable
fun AdminDashboard(onReportsClick: () -> Unit) {
    DashboardLayout(title = "Кабинет Администратора") {
        DashboardButton("Отчеты", Icons.Rounded.Assessment, onReportsClick)
    }
}

@Composable
fun DashboardLayout(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title, 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.springEntrance()
        )
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
fun DashboardButton(
    text: String, 
    icon: ImageVector, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 92.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
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
