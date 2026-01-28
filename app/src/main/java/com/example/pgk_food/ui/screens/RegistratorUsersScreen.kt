package com.example.pgk_food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.remote.dto.*
import com.example.pgk_food.data.repository.RegistratorRepository
import com.example.pgk_food.model.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistratorUsersScreen(token: String, registratorRepository: RegistratorRepository) {
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var resetPasswordResult by remember { mutableStateOf<ResetPasswordResponse?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            val usersResult = registratorRepository.getUsers(token)
            users = usersResult.getOrDefault(emptyList())
            val groupsResult = registratorRepository.getGroups(token)
            groups = groupsResult.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Создать пользователя")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПОЛЬЗОВАТЕЛИ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onDelete = {
                                scope.launch {
                                    registratorRepository.deleteUser(token, user.userId)
                                    loadData()
                                    snackbarHostState.showSnackbar("Пользователь удален")
                                }
                            },
                            onResetPassword = {
                                scope.launch {
                                    val result = registratorRepository.resetPassword(token, user.userId)
                                    result.onSuccess {
                                        resetPasswordResult = it
                                    }.onFailure {
                                        snackbarHostState.showSnackbar("Ошибка сброса пароля")
                                    }
                                }
                            },
                            onUpdateRoles = { roles ->
                                scope.launch {
                                    registratorRepository.updateRoles(token, user.userId, roles)
                                    loadData()
                                    snackbarHostState.showSnackbar("Роли обновлены")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            groups = groups,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                scope.launch {
                    val result = registratorRepository.createUser(token, request)
                    result.onSuccess {
                        showCreateDialog = false
                        loadData()
                        resetPasswordResult = ResetPasswordResponse(it.passwordClearText)
                    }.onFailure {
                        snackbarHostState.showSnackbar("Ошибка создания пользователя")
                    }
                }
            }
        )
    }

    resetPasswordResult?.let { result ->
        AlertDialog(
            onDismissRequest = { resetPasswordResult = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Доступы созданы") },
            text = {
                Column {
                    Text("Скопируйте пароль для передачи пользователю:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.passwordClearText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(result.passwordClearText))
                    scope.launch { snackbarHostState.showSnackbar("Пароль скопирован") }
                    resetPasswordResult = null
                }) {
                    Text("Копировать и закрыть")
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserDto,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit,
    onUpdateRoles: (List<UserRole>) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRolesDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.surname.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.surname} ${user.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.login} • ${user.roles.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Сбросить пароль") },
                        leadingIcon = { Icon(Icons.Default.LockReset, null) },
                        onClick = { showMenu = false; onResetPassword() }
                    )
                    DropdownMenuItem(
                        text = { Text("Изменить роли") },
                        leadingIcon = { Icon(Icons.Default.Shield, null) },
                        onClick = { showMenu = false; showRolesDialog = true }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }

    if (showRolesDialog) {
        RolesSelectionDialog(
            initialRoles = user.roles,
            onDismiss = { showRolesDialog = false },
            onConfirm = { showRolesDialog = false; onUpdateRoles(it) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateUserDialog(
    groups: List<GroupDto>,
    onDismiss: () -> Unit,
    onConfirm: (CreateUserRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setSet(UserRole.STUDENT)) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var expandedGroup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Создать пользователя") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Фамилия") }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Отчество") }, shape = RoundedCornerShape(12.dp))
                
                Text("Роли", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = role in selectedRoles,
                            onClick = {
                                selectedRoles = if (role in selectedRoles) selectedRoles - role else selectedRoles + role
                            },
                            label = { Text(role.name) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (UserRole.STUDENT in selectedRoles || UserRole.CURATOR in selectedRoles) {
                    Text("Группа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = expandedGroup,
                        onExpandedChange = { expandedGroup = it }
                    ) {
                        OutlinedTextField(
                            value = groups.find { it.id == selectedGroupId }?.name ?: "Без группы",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                            DropdownMenuItem(text = { Text("Без группы") }, onClick = { selectedGroupId = null; expandedGroup = false })
                            groups.forEach { group ->
                                DropdownMenuItem(text = { Text(group.name) }, onClick = { selectedGroupId = group.id; expandedGroup = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(CreateUserRequest(selectedRoles.toList(), name, surname, fatherName.ifBlank { null }, selectedGroupId)) },
                enabled = name.isNotBlank() && surname.isNotBlank() && selectedRoles.isNotEmpty()
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun RolesSelectionDialog(
    initialRoles: List<UserRole>,
    onDismiss: () -> Unit,
    onConfirm: (List<UserRole>) -> Unit
) {
    var selectedRoles by remember { mutableStateOf(initialRoles.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Изменить роли") },
        text = {
            Column {
                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedRoles = if (role in selectedRoles) selectedRoles - role else selectedRoles + role
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = role in selectedRoles,
                            onCheckedChange = { checked ->
                                selectedRoles = if (checked) selectedRoles + role else selectedRoles - role
                            }
                        )
                        Text(role.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedRoles.toList()) }, enabled = selectedRoles.isNotEmpty()) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

fun <T> setSet(vararg elements: T): Set<T> = elements.toSet()
