package com.example.pgk_food.ui.screens

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
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.repository.*
import com.example.pgk_food.model.UserRole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authRepository: AuthRepository,
    database: AppDatabase,
    onLogout: () -> Unit
) {
    val userSession by authRepository.getUserSession().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    var currentSubScreen by remember { mutableStateOf("dashboard") }
    var selectedMealType by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (currentSubScreen == "dashboard") "ПГК ПИТАНИЕ" else "НАЗАД",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
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
                        onClick = {
                            scope.launch {
                                authRepository.logout()
                                onLogout()
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.ExitToApp, 
                            contentDescription = "Выход",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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

                LaunchedEffect(roles) {
                    if (selectedRole == null || selectedRole !in roles) {
                        selectedRole = if (UserRole.ADMIN in roles) UserRole.ADMIN else roles.firstOrNull()
                    }
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with user info
                    UserInfoHeader(session)
                    if (UserRole.ADMIN in roles && roles.size > 1) {
                        RoleSwitcher(
                            roles = roles,
                            selectedRole = selectedRole ?: UserRole.ADMIN,
                            onSelect = {
                                selectedRole = it
                                currentSubScreen = "dashboard"
                                selectedMealType = ""
                            }
                        )
                    }

                    val activeRole = if (UserRole.ADMIN in roles) {
                        selectedRole
                    } else {
                        when {
                            UserRole.STUDENT in roles -> UserRole.STUDENT
                            UserRole.CHEF in roles -> UserRole.CHEF
                            UserRole.REGISTRATOR in roles -> UserRole.REGISTRATOR
                            UserRole.CURATOR in roles -> UserRole.CURATOR
                            UserRole.ADMIN in roles -> UserRole.ADMIN
                            else -> null
                        }
                    }

                    when (activeRole) {
                        UserRole.STUDENT -> StudentFlow(
                            session,
                            currentSubScreen,
                            selectedMealType,
                            onNavigate = { currentSubScreen = it },
                            onMealSelect = { selectedMealType = it }
                        )
                        UserRole.CHEF -> ChefFlow(
                            session,
                            currentSubScreen,
                            database,
                            onNavigate = { currentSubScreen = it }
                        )
                        UserRole.REGISTRATOR -> RegistratorFlow(
                            session,
                            currentSubScreen,
                            onNavigate = { currentSubScreen = it }
                        )
                        UserRole.CURATOR -> CuratorFlow(
                            session,
                            currentSubScreen,
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

@Composable
fun RoleSwitcher(
    roles: List<UserRole>,
    selectedRole: UserRole,
    onSelect: (UserRole) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = "Роль:",
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
                    label = { Text(role.name) }
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
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
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
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${session.surname} ${session.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.roles.joinToString(", ") { it.name },
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
    onNavigate: (String) -> Unit,
    onMealSelect: (String) -> Unit
) {
    val studentRepository = remember { StudentRepository() }
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
            onCouponClick = { 
                onMealSelect(it)
                onNavigate("qr") 
            }
        )
        "qr" -> StudentQrScreen(
            session = session,
            mealType = selectedMealType
        )
        "menu" -> MenuScreen(token = session.token, studentRepository = studentRepository)
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
    onNavigate: (String) -> Unit
) {
    val chefRepository = remember { ChefRepository(database.scannedQrDao()) }
    when (currentSubScreen) {
        "dashboard" -> ChefDashboard(
            onScannerClick = { onNavigate("scanner") },
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") }
        )
        "scanner" -> ChefScannerScreen(token = session.token, chefRepository = chefRepository)
        "menu_manage" -> ChefMenuManageScreen(token = session.token, chefRepository = chefRepository)
        "stats" -> ChefStatsScreen(chefRepository = chefRepository)
        else -> ChefDashboard(
            onScannerClick = { onNavigate("scanner") }, 
            onMenuManageClick = { onNavigate("menu_manage") },
            onStatsClick = { onNavigate("stats") }
        )
    }
}

@Composable
fun RegistratorFlow(session: UserSessionEntity, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val registratorRepository = remember { RegistratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> RegistratorDashboard(
            onUsersClick = { onNavigate("users") },
            onGroupsClick = { onNavigate("groups") }
        )
        "users" -> RegistratorUsersScreen(token = session.token, registratorRepository = registratorRepository)
        "groups" -> RegistratorGroupsScreen(token = session.token, registratorRepository = registratorRepository)
        else -> RegistratorDashboard(onUsersClick = { onNavigate("users") }, onGroupsClick = { onNavigate("groups") })
    }
}

@Composable
fun CuratorFlow(session: UserSessionEntity, currentSubScreen: String, onNavigate: (String) -> Unit) {
    val curatorRepository = remember { CuratorRepository() }
    when (currentSubScreen) {
        "dashboard" -> CuratorDashboard(
            token = session.token,
            curatorRepository = curatorRepository,
            onRosterClick = { onNavigate("roster") },
            onStatsClick = { onNavigate("stats") }
        )
        "roster" -> CuratorRosterScreen(token = session.token, curatorRepository = curatorRepository)
        "stats" -> CuratorStatsScreen(token = session.token, curatorRepository = curatorRepository)
        else -> CuratorDashboard(
            token = session.token,
            curatorRepository = curatorRepository,
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
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButton("Мои талоны", Icons.Default.ConfirmationNumber, onCouponsClick, Modifier.weight(1f))
            DashboardButton("Меню", Icons.Default.RestaurantMenu, onMenuClick, Modifier.weight(1f))
        }
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
            color = if (isAllowed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                if (isAllowed) "Разрешено" else "Нет",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ChefDashboard(onScannerClick: () -> Unit, onMenuManageClick: () -> Unit, onStatsClick: () -> Unit) {
    DashboardLayout(title = "Кабинет Повара") {
        DashboardButton("Сканер QR", Icons.Default.QrCodeScanner, onScannerClick)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButton("Меню", Icons.Default.Edit, onMenuManageClick, Modifier.weight(1f))
            DashboardButton("История", Icons.Default.History, onStatsClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun ChefStatsScreen(chefRepository: ChefRepository) {
    val history by chefRepository.getScanHistory()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    val totalScanned = history.size
    val successScanned = history.count { it.status == "success" }
    val errorScanned = history.count { it.status == "error" }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Статистика повара", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Всего", totalScanned.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Успех", successScanned.toString(), Color(0xFF238636), Modifier.weight(1f))
            StatCard("Ошибки", errorScanned.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("История сканирований", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { item ->
                HistoryItem(item)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun HistoryItem(item: ScannedQrEntity) {
    val sdf = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status == "success") 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.studentName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(item.mealType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButton("Пользователи", Icons.Default.Group, onUsersClick, Modifier.weight(1f))
            DashboardButton("Группы", Icons.Default.Class, onGroupsClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun CuratorDashboard(
    token: String,
    curatorRepository: CuratorRepository,
    onRosterClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    var notification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        curatorRepository.getRosterDeadlineNotification(token).onSuccess {
            notification = it
        }
    }

    DashboardLayout(title = "Кабинет Куратора") {
        notification?.let { text ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardButton("Табель", Icons.Default.ListAlt, onRosterClick, Modifier.weight(1f))
            DashboardButton("Статистика", Icons.Default.BarChart, onStatsClick, Modifier.weight(1f))
        }
    }
}

@Composable
fun AdminDashboard(onReportsClick: () -> Unit) {
    DashboardLayout(title = "Кабинет Администратора") {
        DashboardButton("Отчеты", Icons.Default.Assessment, onReportsClick)
    }
}

@Composable
fun DashboardLayout(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title, 
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
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
            .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
