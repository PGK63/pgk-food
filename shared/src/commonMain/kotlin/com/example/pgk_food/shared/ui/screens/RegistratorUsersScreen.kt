@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.shared.data.remote.dto.*
import com.example.pgk_food.shared.data.repository.RegistratorRepository
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.SectionShape
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import kotlinx.coroutines.launch

@Composable
fun RegistratorUsersScreen(token: String, registratorRepository: RegistratorRepository) {
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogInitialGroupId by remember { mutableStateOf<Int?>(null) }
    var credentialsDialog by remember { mutableStateOf<UserCredentials?>(null) }

    // Search & filter state
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterGroupId by remember { mutableStateOf<Int?>(null) }
    var filterRole by remember { mutableStateOf<UserRole?>(null) }

    // User detail sheet
    var selectedUser by remember { mutableStateOf<UserDto?>(null) }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading

    fun loadData() {
        scope.launch {
            isLoading = true
            val usersResult = registratorRepository.getUsers(token)
            users = usersResult.getOrDefault(emptyList())
            val groupsResult = registratorRepository.getGroups(token)
            groups = groupsResult.getOrDefault(emptyList())
            if (usersResult.isFailure || groupsResult.isFailure) {
                snackbarHostState.showSnackbar("Не удалось обновить данные пользователей")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // Filtered users
    val filteredUsers = remember(users, searchQuery, filterGroupId, filterRole) {
        users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.surname.contains(searchQuery, ignoreCase = true) ||
                user.login.contains(searchQuery, ignoreCase = true)
            val matchesGroup = filterGroupId == null || user.groupId == filterGroupId
            val matchesRole = filterRole == null || filterRole in user.roles
            matchesQuery && matchesGroup && matchesRole
        }
    }

    // Group users by groupId
    val groupedUsers: List<Pair<String, List<UserDto>>> = remember(filteredUsers, groups) {
        val groupMap = groups.associateBy { it.id }
        filteredUsers
            .groupBy { it.groupId }
            .entries
            .sortedBy { entry -> entry.key ?: Int.MAX_VALUE }
            .map { entry ->
                val groupId = entry.key
                val usersList = entry.value
                val groupName = if (groupId != null) groupMap[groupId]?.name ?: "Группа $groupId" else "Без группы"
                groupName to usersList
            }
    }

    val hasActiveFilters = filterGroupId != null || filterRole != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    createDialogInitialGroupId = null
                    showCreateDialog = true
                },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "Создать пользователя")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Начните искать") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterGroupId != null,
                    onClick = { showFilterSheet = true },
                    label = {
                        Text(
                            if (filterGroupId != null) groups.find { it.id == filterGroupId }?.name ?: "Группа"
                            else "Группа"
                        )
                    },
                    leadingIcon = {
                        if (filterGroupId != null) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    shape = MaterialTheme.shapes.small
                )
                FilterChip(
                    selected = filterRole != null,
                    onClick = { showFilterSheet = true },
                    label = { Text(filterRole?.name ?: "Роль") },
                    leadingIcon = {
                        if (filterRole != null) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    shape = MaterialTheme.shapes.small
                )
                if (hasActiveFilters) {
                    IconButton(
                        onClick = { filterGroupId = null; filterRole = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Сбросить фильтры", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (isActionLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedUsers.forEach { grouped ->
                        val groupName = grouped.first
                        val groupUsers = grouped.second
                        // Group header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = groupName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = {
                                    createDialogInitialGroupId = groupUsers.firstOrNull()?.groupId
                                    showCreateDialog = true
                                }) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Добавить в группу",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        // User items
                        items(groupUsers, key = { it.userId }) { user ->
                            UserRow(
                                user = user,
                                onClick = { selectedUser = user },
                                onSettingsClick = { selectedUser = user },
                                onDeleteClick = {
                                    scope.launch {
                                        val ok = runUiAction(
                                            actionState = actionState,
                                            successMessage = "Пользователь удален",
                                            fallbackErrorMessage = "Ошибка удаления пользователя",
                                        ) {
                                            registratorRepository.deleteUser(token, user.userId)
                                        }
                                        if (ok) loadData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            groups = groups,
            selectedGroupId = filterGroupId,
            selectedRole = filterRole,
            onGroupSelected = { filterGroupId = it },
            onRoleSelected = { filterRole = it },
            onApply = { showFilterSheet = false },
            onDismiss = { showFilterSheet = false }
        )
    }

    // User detail bottom sheet
    selectedUser?.let { user ->
        UserDetailSheet(
            user = user,
            groups = groups,
            isProcessing = isActionLoading,
            onDismiss = { selectedUser = null },
            onCopyCredentials = {
                clipboardManager.setText(AnnotatedString("Логин: ${user.login}"))
                scope.launch { snackbarHostState.showSnackbar("Логин скопирован") }
            },
            onResetPassword = {
                scope.launch {
                    val result = registratorRepository.resetPassword(token, user.userId)
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Пароль сброшен",
                        fallbackErrorMessage = "Ошибка сброса пароля",
                        emitSuccessFeedback = false
                    ) { result }
                    if (ok) {
                        result.getOrNull()?.let {
                            credentialsDialog = UserCredentials(it.login, it.passwordClearText)
                            selectedUser = null
                        }
                    }
                }
            },
            onUpdateRoles = { roles ->
                scope.launch {
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Роли обновлены",
                        fallbackErrorMessage = "Ошибка обновления ролей",
                    ) {
                        registratorRepository.updateRoles(token, user.userId, roles)
                    }
                    if (ok) {
                        loadData()
                        selectedUser = null
                    }
                }
            },
            onDelete = {
                scope.launch {
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Пользователь удален",
                        fallbackErrorMessage = "Ошибка удаления пользователя",
                    ) {
                        registratorRepository.deleteUser(token, user.userId)
                    }
                    if (ok) {
                        loadData()
                        selectedUser = null
                    }
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateUserDialog(
            groups = groups,
            initialGroupId = createDialogInitialGroupId,
            isSubmitting = isActionLoading,
            onDismiss = {
                showCreateDialog = false
                createDialogInitialGroupId = null
            },
            onConfirm = { request ->
                scope.launch {
                    val result = registratorRepository.createUser(token, request)
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Пользователь создан",
                        fallbackErrorMessage = "Ошибка создания пользователя",
                        emitSuccessFeedback = false
                    ) { result }
                    if (ok) {
                        showCreateDialog = false
                        createDialogInitialGroupId = null
                        loadData()
                        result.getOrNull()?.let {
                            credentialsDialog = UserCredentials(it.login, it.passwordClearText)
                        }
                    }
                }
            }
        )
    }

    credentialsDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { credentialsDialog = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Доступы созданы") },
            text = {
                Column {
                    Text("Скопируйте логин и пароль для передачи пользователю:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Логин: ${result.login}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Пароль: ${result.password}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString("Логин: ${result.login}\nПароль: ${result.password}"))
                    scope.launch { snackbarHostState.showSnackbar("Данные скопированы") }
                    credentialsDialog = null
                }) {
                    Text("Копировать и закрыть")
                }
            }
        )
    }
}

// ─── User row in the grouped list ─────────────────────────────────
@Composable
private fun UserRow(
    user: UserDto,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.surname} ${user.name} ${user.fatherName ?: ""}".trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.roles.joinToString(", ") { it.name } +
                        (if (user.groupId != null) " • @${user.login}" else " • @${user.login}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Настройки", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Filter Bottom Sheet ─────────────────────────────────────────
@Composable
private fun FilterBottomSheet(
    groups: List<GroupDto>,
    selectedGroupId: Int?,
    selectedRole: UserRole?,
    onGroupSelected: (Int?) -> Unit,
    onRoleSelected: (UserRole?) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SectionShape
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Фильтры",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    onGroupSelected(null)
                    onRoleSelected(null)
                }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group filter
            Text("Группа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGroupId == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text("Все") },
                    shape = MaterialTheme.shapes.small
                )
                groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroupId == group.id,
                        onClick = { onGroupSelected(if (selectedGroupId == group.id) null else group.id) },
                        label = { Text(group.name) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Role filter
            Text("Роль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRole == null,
                    onClick = { onRoleSelected(null) },
                    label = { Text("Все") },
                    shape = MaterialTheme.shapes.small
                )
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { onRoleSelected(if (selectedRole == role) null else role) },
                        label = { Text(role.name) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Применить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── User Detail Bottom Sheet ────────────────────────────────────
@Composable
private fun UserDetailSheet(
    user: UserDto,
    groups: List<GroupDto>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onCopyCredentials: () -> Unit,
    onResetPassword: () -> Unit,
    onUpdateRoles: (List<UserRole>) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRolesDialog by remember(user.userId) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SectionShape
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss, enabled = !isProcessing) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                }
            }

            Text(
                text = "${user.surname} ${user.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            user.fatherName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val groupName = groups.find { it.id == user.groupId }?.name
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Группа: ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        groupName ?: "Не назначена",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Роли:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    user.roles.forEach { role ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                role.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCopyCredentials,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Скопировать логин\nи пароль", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onResetPassword,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Сменить пароль", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showRolesDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Изменить роли", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Удалить пользователя", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRolesDialog) {
        RolesSelectionDialog(
            initialRoles = user.roles,
            onDismiss = { showRolesDialog = false },
            onConfirm = { roles ->
                showRolesDialog = false
                onUpdateRoles(roles)
            }
        )
    }
}

private data class UserCredentials(
    val login: String,
    val password: String
)

@Composable
fun CreateUserDialog(
    groups: List<GroupDto>,
    initialGroupId: Int? = null,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (CreateUserRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setOf(UserRole.STUDENT)) }
    var selectedGroupId by remember(initialGroupId) { mutableStateOf(initialGroupId) }
    var expandedGroup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Создать пользователя") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Фамилия") }, shape = MaterialTheme.shapes.medium)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, shape = MaterialTheme.shapes.medium)
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Отчество") }, shape = MaterialTheme.shapes.medium)

                Text("Роли", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = role in selectedRoles,
                            onClick = {
                                selectedRoles = if (role in selectedRoles) selectedRoles - role else selectedRoles + role
                            },
                            label = { Text(role.name) },
                            shape = MaterialTheme.shapes.small
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
                            shape = MaterialTheme.shapes.medium
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
                enabled = !isSubmitting && name.isNotBlank() && surname.isNotBlank() && selectedRoles.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создание...")
                } else {
                    Text("Создать")
                }
            }
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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Изменить роли") },
        text = {
            Column {
                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRoles = if (role in selectedRoles) selectedRoles - role else selectedRoles + role
                            }
                            .padding(vertical = 4.dp),
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
