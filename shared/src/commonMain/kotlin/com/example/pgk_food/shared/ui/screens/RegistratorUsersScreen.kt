@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.pgk_food.shared.data.remote.dto.*
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.RegistratorRepository
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.ui.components.LocalAppSnackbarDispatcher
import com.example.pgk_food.shared.ui.components.CredentialsDialog
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.components.UserCredentialsUi
import com.example.pgk_food.shared.ui.components.longPressHelp
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.theme.SectionShape
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.model.AccountStatus
import com.example.pgk_food.shared.model.titleRu
import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.launch

private fun UserRole.titleRu(): String = when (this) {
    UserRole.ADMIN -> "Администратор"
    UserRole.REGISTRATOR -> "Регистратор"
    UserRole.CHEF -> "Повар"
    UserRole.CURATOR -> "Куратор"
    UserRole.STUDENT -> "Студент"
}

private fun StudentCategory.titleRu(): String = when (this) {
    StudentCategory.SVO -> "СВО"
    StudentCategory.MANY_CHILDREN -> "Многодетные"
}

@Composable
fun RegistratorUsersScreen(
    token: String,
    registratorRepository: RegistratorRepository,
    reloadKey: Int = 0,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
    onCreateUserClick: (Int?) -> Unit = {},
) {
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val snackbarDispatcher = LocalAppSnackbarDispatcher.current
    val clipboardManager = LocalClipboardManager.current
    val hintContent = remember { HintCatalog.content(HintScreenKey.REGISTRATOR_USERS) }

    var credentialsDialog by remember { mutableStateOf<UserCredentialsUi?>(null) }

    // Search & filter state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterGroupId by rememberSaveable { mutableStateOf<Int?>(null) }
    var filterRoleRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val filterRole = filterRoleRaw
        ?.let { raw -> runCatching { UserRole.valueOf(raw) }.getOrNull() }

    // User detail sheet
    var selectedUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteCandidate by remember { mutableStateOf<UserDto?>(null) }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading

    fun loadData() {
        scope.launch {
            isLoading = true
            val usersResult = registratorRepository.getUsers(token)
            users = usersResult.getOrDefault(emptyList())
            selectedUser = selectedUser?.let { selected ->
                users.firstOrNull { it.userId == selected.userId }
            }
            val groupsResult = registratorRepository.getGroups(token)
            groups = groupsResult.getOrDefault(emptyList())
            if (usersResult.isFailure || groupsResult.isFailure) {
                snackbarDispatcher.show("Не удалось обновить данные пользователей")
            }
            isLoading = false
        }
    }

    LaunchedEffect(reloadKey) { loadData() }

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
    val groupedUsers: List<Triple<Int?, String, List<UserDto>>> = remember(filteredUsers, groups) {
        val groupMap = groups.associateBy { it.id }
        filteredUsers
            .groupBy { user -> user.groupId }
            .entries
            .sortedBy { entry -> entry.key ?: Int.MAX_VALUE }
            .map { entry ->
                val groupId = entry.key
                val usersList = entry.value
                val groupName = if (groupId != null) groupMap[groupId]?.name ?: "Группа $groupId" else "Без группы"
                Triple(groupId, groupName, usersList)
            }
    }

    val hasActiveFilters = filterGroupId != null || filterRole != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateUserClick(null) },
                modifier = Modifier.longPressHelp(
                    actionId = "users.create",
                    fallbackDescription = "Создать пользователя",
                ),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "Создать пользователя")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (showHints) {
                    HowItWorksCard(
                        title = hintContent.title,
                        steps = hintContent.steps,
                        note = hintContent.note,
                        onDismiss = onDismissHints,
                        modifier = Modifier.springEntrance(10),
                    )
                    hintContent.inlineHints.firstOrNull()?.let { inline ->
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineHint(text = inline, modifier = Modifier.springEntrance(15))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Начните искать") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.longPressHelp(
                                    actionId = "search.clear",
                                    fallbackDescription = "Очистить",
                                ),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().springEntrance(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips row
                FlowRow(
                    modifier = Modifier.fillMaxWidth().springEntrance(60),
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
                        label = { Text(filterRole?.titleRu() ?: "Роль") },
                        leadingIcon = {
                            if (filterRole != null) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = MaterialTheme.shapes.small
                    )
                    if (hasActiveFilters) {
                        IconButton(
                            onClick = { filterGroupId = null; filterRoleRaw = null },
                            modifier = Modifier
                                .size(32.dp)
                                .longPressHelp(
                                    actionId = "users.filter.reset",
                                    fallbackDescription = "Сбросить фильтры",
                                )
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
                        groupedUsers.forEachIndexed { groupIndex, (groupId, groupName, groupUsers) ->
                            val groupDelay = 120 + (groupIndex.coerceAtMost(9) * 45)
                            // Group header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 4.dp)
                                        .springEntrance(groupDelay),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    IconButton(
                                        onClick = { onCreateUserClick(groupId) },
                                        modifier = Modifier.longPressHelp(
                                            actionId = "users.group.quick-create",
                                            fallbackDescription = "Добавить в группу",
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Добавить в группу",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            // User items
                            itemsIndexed(groupUsers, key = { _, user -> user.userId }) { userIndex, user ->
                                UserRow(
                                    user = user,
                                    groupName = groups.find { it.id == user.groupId }?.name,
                                    animationDelayMs = groupDelay + ((userIndex.coerceAtMost(5) + 1) * 30),
                                    onClick = { selectedUser = user },
                                    onSettingsClick = { selectedUser = user },
                                    onDeleteClick = {
                                        deleteCandidate = user
                                    }
                                )
                            }
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
            onRoleSelected = { filterRoleRaw = it?.name },
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
                scope.launch {
                    val result = registratorRepository.resetPassword(token, user.userId)
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Пароль сброшен",
                        fallbackErrorMessage = "Ошибка сброса пароля",
                        emitSuccessFeedback = false
                    ) { result }
                    if (ok) {
                        result.getOrNull()?.let { reset ->
                            clipboardManager.setText(
                                AnnotatedString("Логин: ${reset.login}\nПароль: ${reset.passwordClearText}")
                            )
                            snackbarDispatcher.show("Логин и новый пароль скопированы")
                            selectedUser = null
                        }
                    }
                }
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
                            credentialsDialog = UserCredentialsUi(it.login, it.passwordClearText)
                            selectedUser = null
                        }
                    }
                }
            },
            onUpdateRoles = { roles, groupId, studentCategory ->
                scope.launch {
                    val result = registratorRepository.updateRoles(
                        token = token,
                        userId = user.userId,
                        roles = roles,
                        groupId = groupId,
                        studentCategory = studentCategory,
                    )
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Роли обновлены",
                        fallbackErrorMessage = "Ошибка обновления ролей",
                    ) { result }
                    if (ok) {
                        val activeUserId = SessionStore.session.value?.userId
                        val updatedUserId = result.getOrNull()?.userId
                        if (activeUserId != null && activeUserId == updatedUserId) {
                            val refreshResult = authRepository.refreshCurrentSession(token)
                            if (refreshResult.isFailure) {
                                snackbarDispatcher.show("Роли сохранены, но не удалось обновить текущую сессию")
                            }
                        }
                        loadData()
                        selectedUser = null
                    }
                }
            },
            onUpdateLifecycle = { status ->
                scope.launch {
                    val result = registratorRepository.updateLifecycle(
                        token = token,
                        userId = user.userId,
                        status = status,
                    )
                    val ok = runUiAction(
                        actionState = actionState,
                        successMessage = "Статус обновлен",
                        fallbackErrorMessage = "Ошибка смены статуса",
                    ) { result }
                    if (ok) {
                        loadData()
                        selectedUser = null
                    }
                }
            },
            onDelete = {
                deleteCandidate = user
            }
        )
    }

    deleteCandidate?.let { user ->
        AlertDialog(
            onDismissRequest = { if (!isActionLoading) deleteCandidate = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Удалить пользователя?") },
            text = {
                Column {
                    Text(
                        "Пользователь \"${user.surname} ${user.name}\" (${user.login}) будет удален без возможности восстановления."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Проверьте ФИО и логин перед подтверждением.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = runUiAction(
                                actionState = actionState,
                                successMessage = "Пользователь удален",
                                fallbackErrorMessage = "Ошибка удаления пользователя",
                            ) {
                                registratorRepository.deleteUser(token, user.userId)
                            }
                            if (ok) {
                                deleteCandidate = null
                                if (selectedUser?.userId == user.userId) {
                                    selectedUser = null
                                }
                                loadData()
                            }
                        }
                    },
                    enabled = !isActionLoading,
                ) {
                    Text(if (isActionLoading) "Удаление..." else "Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteCandidate = null },
                    enabled = !isActionLoading,
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    credentialsDialog?.let { result ->
        CredentialsDialog(
            credentials = result,
            onDismiss = { credentialsDialog = null },
            onCopiedAndDismissed = {
                scope.launch { snackbarDispatcher.show("Данные скопированы") }
                credentialsDialog = null
            }
        )
    }
}

// ─── User row in the grouped list ─────────────────────────────────
@Composable
private fun UserRow(
    user: UserDto,
    groupName: String?,
    animationDelayMs: Int = 0,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isExpelled = user.accountStatus == AccountStatus.FROZEN_EXPELLED
    val detailParts = mutableListOf(
        user.roles.joinToString(", ") { it.titleRu() },
        groupName ?: "Без группы",
    )
    if (isExpelled) {
        detailParts += user.accountStatus.titleRu()
    }
    detailParts += user.login

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .springEntrance(animationDelayMs)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpelled) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
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
                    fontWeight = FontWeight.Medium,
                    color = if (isExpelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detailParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.longPressHelp(
                    actionId = "users.row.edit",
                    fallbackDescription = "Настройки",
                ),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Настройки", modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.longPressHelp(
                    actionId = "users.row.delete",
                    fallbackDescription = "Удалить",
                ),
            ) {
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
        shape = SectionShape,
        dragHandle = null,
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
                        label = { Text(role.titleRu()) },
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
    onUpdateRoles: (List<UserRole>, Int?, StudentCategory?) -> Unit,
    onUpdateLifecycle: (AccountStatus) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRolesDialog by remember(user.userId) { mutableStateOf(false) }
    var showUnfreezeConfirm by remember(user.userId) { mutableStateOf(false) }
    val isExpelled = user.accountStatus == AccountStatus.FROZEN_EXPELLED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SectionShape,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onDismiss,
                    enabled = !isProcessing,
                    modifier = Modifier.longPressHelp(
                        actionId = "dialog.close",
                        fallbackDescription = "Закрыть",
                    ),
                ) {
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

            if (isExpelled) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Куратор пометил к отчислению",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            user.accountStatus.titleRu(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

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
                                role.titleRu(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isExpelled) {
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
                    Text("Сбросить пароль\nи скопировать", fontWeight = FontWeight.Bold)
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
            } else {
                Button(
                    onClick = { showUnfreezeConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Разморозить", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

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

    if (showUnfreezeConfirm) {
        AlertDialog(
            onDismissRequest = { showUnfreezeConfirm = false },
            title = { Text("Подтвердите разморозку") },
            text = { Text("Пользователь снова получит доступ к системе.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnfreezeConfirm = false
                        onUpdateLifecycle(AccountStatus.ACTIVE)
                    },
                    enabled = !isProcessing
                ) { Text("Разморозить") }
            },
            dismissButton = {
                TextButton(onClick = { showUnfreezeConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showRolesDialog) {
        RolesSelectionDialog(
            initialRoles = user.roles,
            groups = groups,
            initialGroupId = user.groupId,
            initialStudentCategory = user.studentCategory,
            onDismiss = { showRolesDialog = false },
            onConfirm = { roles, groupId, studentCategory ->
                showRolesDialog = false
                onUpdateRoles(roles, groupId, studentCategory)
            }
        )
    }
}

@Composable
fun RolesSelectionDialog(
    initialRoles: List<UserRole>,
    groups: List<GroupDto>,
    initialGroupId: Int?,
    initialStudentCategory: StudentCategory?,
    onDismiss: () -> Unit,
    onConfirm: (List<UserRole>, Int?, StudentCategory?) -> Unit
) {
    var selectedRoles by remember { mutableStateOf(initialRoles.toSet()) }
    var selectedGroupId by remember(initialGroupId) { mutableStateOf(initialGroupId) }
    var selectedStudentCategory by remember(initialStudentCategory) { mutableStateOf(initialStudentCategory) }
    var groupExpanded by remember { mutableStateOf(false) }
    val studentRoleAdded = UserRole.STUDENT in selectedRoles && UserRole.STUDENT !in initialRoles
    val studentRoleSelected = UserRole.STUDENT in selectedRoles
    val canConfirm = selectedRoles.isNotEmpty() &&
        (!studentRoleAdded || (selectedGroupId != null && selectedStudentCategory != null))

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
                        Text(role.titleRu())
                    }
                }

                if (UserRole.STUDENT in selectedRoles) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Группа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = groupExpanded,
                        onExpandedChange = { groupExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = groups.find { it.id == selectedGroupId }?.name ?: "Без группы",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Без группы") },
                                onClick = {
                                    selectedGroupId = null
                                    groupExpanded = false
                                }
                            )
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        selectedGroupId = group.id
                                        groupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (studentRoleAdded && selectedGroupId == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Для добавления роли студента выберите группу",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Категория", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedStudentCategory == category,
                                onClick = {
                                    selectedStudentCategory = if (selectedStudentCategory == category) {
                                        null
                                    } else {
                                        category
                                    }
                                },
                                label = { Text(category.titleRu()) },
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                    if (studentRoleAdded && selectedStudentCategory == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Для добавления роли студента выберите категорию",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedRoles.toList(),
                        selectedGroupId,
                        if (studentRoleSelected) selectedStudentCategory else null,
                    )
                },
                enabled = canConfirm
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
