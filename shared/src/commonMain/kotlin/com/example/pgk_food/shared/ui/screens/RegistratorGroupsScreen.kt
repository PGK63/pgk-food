package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.remote.dto.UserDto
import com.example.pgk_food.shared.data.repository.RegistratorRepository
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.components.AppSnackbarHostOverlay
import com.example.pgk_food.shared.ui.components.HintCatalog
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.components.InlineHint
import com.example.pgk_food.shared.ui.components.longPressHelp
import com.example.pgk_food.shared.ui.state.UiActionState
import com.example.pgk_food.shared.ui.state.isLoading
import com.example.pgk_food.shared.ui.state.runUiAction
import com.example.pgk_food.shared.ui.theme.springEntrance
import com.example.pgk_food.shared.util.HintScreenKey
import kotlinx.coroutines.launch

private sealed interface StudentGroupFilter {
    object All : StudentGroupFilter
    object NoGroup : StudentGroupFilter
    data class Specific(val groupId: Int) : StudentGroupFilter
}

private fun StudentGroupFilter.title(groups: List<GroupDto>): String = when (this) {
    StudentGroupFilter.All -> "Все группы"
    StudentGroupFilter.NoGroup -> "Без группы"
    is StudentGroupFilter.Specific -> groups.firstOrNull { it.id == groupId }?.name ?: "Группа #$groupId"
}

private fun normalizeGroupName(raw: String): String {
    return raw.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun Throwable?.apiCodeOrNull(): String? {
    return (this as? ApiCallException)?.apiError?.code
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistratorGroupsScreen(
    token: String,
    registratorRepository: RegistratorRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {}
) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showDeleteGroupDialog by remember { mutableStateOf<GroupDto?>(null) }
    var showTransferGroupDialog by remember { mutableStateOf<GroupDto?>(null) }
    
    // For student management
    var expandedGroupId by remember { mutableStateOf<Int?>(null) }
    var groupStudents by remember { mutableStateOf<Map<Int, List<UserDto>>>(emptyMap()) }
    
    // For assigning curator/student
    var showAssignMemberDialog by remember { mutableStateOf<Pair<Int, String>?>(null) } // groupId to "CURATOR" or "STUDENT"
    var allUsers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    
    // Search
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionState = remember { mutableStateOf<UiActionState>(UiActionState.Idle) }
    val isActionLoading = actionState.value.isLoading
    val hintContent = remember { HintCatalog.content(HintScreenKey.REGISTRATOR_GROUPS) }

    fun Throwable.userMessageOr(default: String): String {
        val api = (this as? ApiCallException)?.apiError
        return api?.userMessage?.ifBlank { default } ?: message ?: default
    }

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val normalizedNewGroupName = remember(newGroupName) { normalizeGroupName(newGroupName) }
    val hasDuplicateGroupName = remember(groups, normalizedNewGroupName) {
        normalizedNewGroupName.isNotBlank() && groups.any {
            normalizeGroupName(it.name).equals(normalizedNewGroupName, ignoreCase = true)
        }
    }

    suspend fun refreshGroups(showLoader: Boolean = true) {
        if (showLoader) isLoading = true
        val result = registratorRepository.getGroups(token)
        groups = result.getOrDefault(emptyList())
        val existingGroupIds = groups.map { it.id }.toSet()
        groupStudents = groupStudents.filterKeys { it in existingGroupIds }
        if (expandedGroupId != null && expandedGroupId !in existingGroupIds) {
            expandedGroupId = null
        }
        if (result.isFailure) {
            snackbarHostState.showSnackbar("Не удалось обновить список групп")
        }
        if (showLoader) isLoading = false
    }

    suspend fun loadStudents(groupId: Int, force: Boolean = false) {
        if (!force && groupStudents.containsKey(groupId)) return
        val result = registratorRepository.getUsers(token, groupId)
        val students = result.getOrDefault(emptyList())
        groupStudents = groupStudents + (groupId to students)
        if (result.isFailure) {
            snackbarHostState.showSnackbar("Не удалось загрузить студентов группы")
        }
    }

    suspend fun loadAllUsers(force: Boolean = false) {
        if (!force && allUsers.isNotEmpty()) return
        val result = registratorRepository.getUsers(token)
        allUsers = result.getOrDefault(emptyList())
        if (result.isFailure) {
            snackbarHostState.showSnackbar("Не удалось загрузить список пользователей")
        }
    }

    suspend fun refreshUiAfterMutation(vararg groupIds: Int) {
        refreshGroups(showLoader = false)
        loadAllUsers(force = true)
        groupIds.toSet().forEach { loadStudents(groupId = it, force = true) }
    }

    suspend fun transferGroup(group: GroupDto, targetName: String) {
        actionState.value = UiActionState.Loading
        val newGroupNameValue = normalizeGroupName(targetName)
        if (newGroupNameValue.isEmpty()) {
            snackbarHostState.showSnackbar("Введите новое название группы")
            actionState.value = UiActionState.Idle
            return
        }
        val localDuplicate = groups.any {
            it.id != group.id && normalizeGroupName(it.name).equals(newGroupNameValue, ignoreCase = true)
        }
        if (localDuplicate) {
            snackbarHostState.showSnackbar("Группа с таким названием уже существует")
            actionState.value = UiActionState.Idle
            return
        }

        var transferError: String? = null
        val movedStudents = mutableListOf<String>()
        val reassignedCuratorIds = mutableListOf<String>()

        val createGroupResult = registratorRepository.createGroup(token, newGroupNameValue)
        createGroupResult.onFailure {
            transferError = if (it.apiCodeOrNull() == "ACCESS_DENIED") {
                "Нет доступа к созданию группы. Выполните повторный вход."
            } else {
                "Не удалось создать новую группу: ${it.userMessageOr("неизвестная ошибка")}"
            }
        }
        if (transferError != null) {
            snackbarHostState.showSnackbar(transferError!!)
            actionState.value = UiActionState.Idle
            return
        }

        val freshGroups = registratorRepository.getGroups(token).getOrDefault(emptyList())
        val newGroup = freshGroups
            .filter {
                it.id != group.id && normalizeGroupName(it.name).equals(newGroupNameValue, ignoreCase = true)
            }
            .maxByOrNull { it.id }

        if (newGroup == null) {
            snackbarHostState.showSnackbar("Новая группа создана, но не найдена в списке")
            actionState.value = UiActionState.Idle
            return
        }

        val students = registratorRepository.getUsers(token, group.id)
            .getOrDefault(emptyList())
            .filter { UserRole.STUDENT in it.roles }

        students.forEach { student ->
            if (transferError != null) return@forEach
            registratorRepository.addStudentToGroup(token, newGroup.id, student.userId)
                .onSuccess { movedStudents += student.userId }
                .onFailure {
                    transferError = "Не удалось перенести студента ${student.surname} ${student.name}: ${it.userMessageOr("неизвестная ошибка")}"
                }
        }

        if (transferError == null && group.curators.isNotEmpty()) {
            group.curators.forEach { curator ->
                if (transferError != null) return@forEach
                registratorRepository.assignCurator(token, newGroup.id, curator.id).onSuccess {
                    reassignedCuratorIds += curator.id
                }.onFailure {
                    transferError = "Не удалось переназначить куратора ${curator.surname} ${curator.name}: ${it.userMessageOr("неизвестная ошибка")}"
                }
            }
        }

        if (transferError == null) {
            registratorRepository.deleteGroup(token, group.id).onFailure {
                transferError = "Студенты перенесены, но удалить старую группу не удалось: ${it.userMessageOr("неизвестная ошибка")}"
            }
        }

        if (transferError != null) {
            val rollbackErrors = mutableListOf<String>()

            movedStudents.forEach { studentId ->
                registratorRepository.addStudentToGroup(token, group.id, studentId).onFailure {
                    rollbackErrors += "Не удалось вернуть студента $studentId"
                }
            }

            if (reassignedCuratorIds.isNotEmpty()) {
                reassignedCuratorIds.forEach { curatorId ->
                    registratorRepository.assignCurator(token, group.id, curatorId).onFailure {
                        rollbackErrors += "Не удалось вернуть куратора $curatorId"
                    }
                }
            }

            registratorRepository.deleteGroup(token, newGroup.id).onFailure {
                rollbackErrors += "Не удалось удалить временную группу"
            }

            val message = if (rollbackErrors.isEmpty()) {
                transferError!!
            } else {
                "$transferError Откат выполнен частично: ${rollbackErrors.joinToString("; ")}"
            }
            snackbarHostState.showSnackbar(message)
        } else {
            snackbarHostState.showSnackbar("Группа успешно переведена/переименована")
        }

        refreshUiAfterMutation(group.id, newGroup.id)
        actionState.value = UiActionState.Idle
    }

    LaunchedEffect(Unit) {
        refreshGroups()
        loadAllUsers(force = true)
    }
    LaunchedEffect(showAssignMemberDialog) {
        if (showAssignMemberDialog != null) {
            loadAllUsers(force = true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGroupDialog = true },
                modifier = Modifier.longPressHelp(
                    actionId = "groups.create",
                    fallbackDescription = "Добавить группу",
                ),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить группу")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Управление группами",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.springEntrance()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по названию группы") },
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
                modifier = Modifier.fillMaxWidth().springEntrance(60),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Найдено: ${filteredGroups.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.springEntrance(90)
            )
            if (isActionLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (showHints) {
                Spacer(modifier = Modifier.height(8.dp))
                HowItWorksCard(
                    title = hintContent.title,
                    steps = hintContent.steps,
                    note = hintContent.note,
                    onDismiss = onDismissHints,
                    modifier = Modifier.springEntrance(120),
                )
                hintContent.inlineHints.firstOrNull()?.let { inline ->
                    Spacer(modifier = Modifier.height(8.dp))
                    InlineHint(
                        text = inline,
                        modifier = Modifier.springEntrance(135)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredGroups.isEmpty()) {
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
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(filteredGroups, key = { _, group -> group.id }) { index, group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .springEntrance((index.coerceAtMost(9) * 45) + 150)
                                .clickable {
                                    if (expandedGroupId == group.id) {
                                        expandedGroupId = null
                                    } else {
                                        expandedGroupId = group.id
                                        scope.launch { loadStudents(group.id, force = true) }
                                    }
                                },
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (group.curators.isNotEmpty()) {
                                            Text(
                                                text = "Кураторы: " + group.curators.joinToString("; ") {
                                                    "${it.surname} ${it.name} ${it.fatherName}"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = "Куратор не назначен",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text("Студентов: ${group.studentCount}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { showAssignMemberDialog = group.id to "CURATOR" },
                                            modifier = Modifier.longPressHelp(
                                                actionId = "groups.assign.curator",
                                                fallbackDescription = "Назначить куратора",
                                            ),
                                        ) {
                                            Icon(Icons.Rounded.Person, contentDescription = "Назначить куратора", modifier = Modifier.size(22.dp))
                                        }
                                        IconButton(
                                            onClick = { showTransferGroupDialog = group },
                                            modifier = Modifier.longPressHelp(
                                                actionId = "groups.transfer",
                                                fallbackDescription = "Перевести/переименовать",
                                            ),
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Перевести/переименовать", modifier = Modifier.size(22.dp))
                                        }
                                        if (group.curators.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                scope.launch {
                                                    val ok = runUiAction(
                                                        actionState = actionState,
                                                        successMessage = "Кураторы сняты",
                                                        fallbackErrorMessage = "Ошибка снятия куратора",
                                                    ) {
                                                        var hasError = false
                                                        group.curators.forEach { curator ->
                                                            val removeResult = registratorRepository.removeCurator(
                                                                token = token,
                                                                groupId = group.id,
                                                                curatorId = curator.id
                                                            )
                                                            if (removeResult.isFailure) hasError = true
                                                        }
                                                        if (hasError) Result.failure<Unit>(IllegalStateException("Не всех кураторов удалось снять"))
                                                        else Result.success(Unit)
                                                    }
                                                    if (ok) refreshUiAfterMutation(group.id)
                                                }
                                            },
                                                modifier = Modifier.longPressHelp(
                                                    actionId = "groups.unassign.curator",
                                                    fallbackDescription = "Снять куратора",
                                                ),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.PersonOff, 
                                                    contentDescription = "Снять куратора",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { showDeleteGroupDialog = group },
                                            modifier = Modifier.longPressHelp(
                                                actionId = "groups.delete",
                                                fallbackDescription = "Удалить группу",
                                            ),
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = "Удалить группу",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                if (expandedGroupId == group.id) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Студенты:", style = MaterialTheme.typography.titleMedium)
                                    
                                    val students = groupStudents[group.id] ?: emptyList()
                                    if (students.isEmpty()) {
                                        Text("Нет студентов", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        students.forEach { student ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${student.surname} ${student.name}",
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val ok = runUiAction(
                                                                actionState = actionState,
                                                                successMessage = "Студент убран из группы",
                                                                fallbackErrorMessage = "Ошибка удаления студента из группы",
                                                            ) {
                                                                registratorRepository.removeStudentFromGroup(token, student.userId)
                                                            }
                                                            if (ok) {
                                                                refreshUiAfterMutation(group.id)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.longPressHelp(
                                                        actionId = "groups.member.remove",
                                                        fallbackDescription = "Убрать из группы",
                                                    ),
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Убрать из группы", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { showAssignMemberDialog = group.id to "STUDENT" },
                                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Добавить студента")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AppSnackbarHostOverlay(hostState = snackbarHostState)
        }
    }
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Создать группу") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Название группы") },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    if (hasDuplicateGroupName) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Группа с таким названием уже существует",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Действие требует роль Регистратор/Администратор. При совпадении названий ориентируйтесь по ID группы.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (normalizedNewGroupName.isBlank()) {
                            snackbarHostState.showSnackbar("Введите название группы")
                            return@launch
                        }
                        if (hasDuplicateGroupName) {
                            snackbarHostState.showSnackbar("Группа с таким названием уже существует")
                            return@launch
                        }
                        val result = registratorRepository.createGroup(token, normalizedNewGroupName)
                        val ok = runUiAction(
                            actionState = actionState,
                            successMessage = "Группа создана",
                            fallbackErrorMessage = "Ошибка создания группы",
                        ) {
                            result
                        }
                        if (ok) {
                            newGroupName = ""
                            showAddGroupDialog = false
                            refreshUiAfterMutation()
                        } else if (result.exceptionOrNull().apiCodeOrNull() == "ACCESS_DENIED") {
                            snackbarHostState.showSnackbar("Нет доступа к созданию группы. Выполните повторный вход.")
                        }
                    }
                }, enabled = !isActionLoading && normalizedNewGroupName.isNotBlank() && !hasDuplicateGroupName) {
                    if (isActionLoading) {
                        Text("Создание...")
                    } else {
                        Text("Создать")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    showTransferGroupDialog?.let { group ->
        var targetGroupName by remember(group.id) { mutableStateOf(group.name) }
        AlertDialog(
            onDismissRequest = { showTransferGroupDialog = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Перевести/переименовать группу") },
            text = {
                Column {
                    Text(
                        text = "Будет создана новая группа, студенты и куратор будут перенесены.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetGroupName,
                        onValueChange = { targetGroupName = it },
                        label = { Text("Новое название") },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            showTransferGroupDialog = null
                            transferGroup(group, targetGroupName)
                        }
                    },
                    enabled = targetGroupName.isNotBlank() && !isActionLoading
                ) {
                    Text(if (isActionLoading) "Перенос..." else "Перенести")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferGroupDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAssignMemberDialog != null) {
        val (groupId, role) = showAssignMemberDialog!!
        var memberSearchQuery by remember(showAssignMemberDialog) { mutableStateOf("") }
        var studentGroupFilter by remember(showAssignMemberDialog) {
            mutableStateOf<StudentGroupFilter>(StudentGroupFilter.All)
        }
        var isStudentGroupFilterExpanded by remember(showAssignMemberDialog) { mutableStateOf(false) }

        val queryTokens = remember(memberSearchQuery) {
            memberSearchQuery.trim()
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
        }
        val groupForDialog = remember(groups, groupId) { groups.firstOrNull { it.id == groupId } }
        val assignedCuratorIds = remember(groupForDialog) {
            groupForDialog?.curators?.map { it.id }?.toSet().orEmpty()
        }
        val assignedCurators = remember(groupForDialog, queryTokens) {
            groupForDialog?.curators
                .orEmpty()
                .asSequence()
                .filter { curator ->
                    if (queryTokens.isEmpty()) return@filter true
                    val searchable = buildString {
                        append(curator.surname)
                        append(' ')
                        append(curator.name)
                        append(' ')
                        append(curator.fatherName)
                    }.lowercase()
                    queryTokens.all { token -> searchable.contains(token) }
                }
                .sortedWith(
                    compareBy(
                        { it.surname.lowercase() },
                        { it.name.lowercase() },
                        { it.fatherName.lowercase() },
                    )
                )
                .toList()
        }

        val selectableUsers = remember(allUsers, role, groupId, studentGroupFilter, queryTokens, assignedCuratorIds) {
            allUsers
                .asSequence()
                .filter { user ->
                    if (role == "CURATOR") user.roles.contains(UserRole.CURATOR) && user.userId !in assignedCuratorIds
                    else user.roles.contains(UserRole.STUDENT)
                }
                .filter { user ->
                    if (role != "STUDENT") return@filter true
                    if (user.groupId == groupId) return@filter false
                    when (val filter = studentGroupFilter) {
                        StudentGroupFilter.All -> true
                        StudentGroupFilter.NoGroup -> user.groupId == null
                        is StudentGroupFilter.Specific -> user.groupId == filter.groupId
                    }
                }
                .filter { user ->
                    if (queryTokens.isEmpty()) return@filter true
                    val searchable = buildString {
                        append(user.surname)
                        append(' ')
                        append(user.name)
                        append(' ')
                        append(user.fatherName.orEmpty())
                        append(' ')
                        append(user.login)
                    }.lowercase()
                    queryTokens.all { token -> searchable.contains(token) }
                }
                .sortedWith(
                    compareBy<UserDto>(
                        { it.surname.lowercase() },
                        { it.name.lowercase() },
                        { it.fatherName.orEmpty().lowercase() },
                        { it.login.lowercase() }
                    )
                )
                .toList()
        }

        AlertDialog(
            onDismissRequest = { showAssignMemberDialog = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(if (role == "CURATOR") "Назначить куратора" else "Добавить студента") },
            text = {
                Column {
                    Text(
                        text = "Действие требует роль Регистратор/Администратор.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        placeholder = { Text("Поиск по ФИО или логину") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (memberSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { memberSearchQuery = "" },
                                    modifier = Modifier.longPressHelp(
                                        actionId = "search.clear",
                                        fallbackDescription = "Очистить",
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (role == "CURATOR") {
                            "Найдено: ${assignedCurators.size + selectableUsers.size}"
                        } else {
                            "Найдено: ${selectableUsers.size}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (role == "STUDENT") {
                        Spacer(modifier = Modifier.height(10.dp))
                        ExposedDropdownMenuBox(
                            expanded = isStudentGroupFilterExpanded,
                            onExpandedChange = { isStudentGroupFilterExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = studentGroupFilter.title(groups),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Фильтр по группе") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStudentGroupFilterExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = isStudentGroupFilterExpanded,
                                onDismissRequest = { isStudentGroupFilterExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Все группы") },
                                    onClick = {
                                        studentGroupFilter = StudentGroupFilter.All
                                        isStudentGroupFilterExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Без группы") },
                                    onClick = {
                                        studentGroupFilter = StudentGroupFilter.NoGroup
                                        isStudentGroupFilterExpanded = false
                                    }
                                )
                                groups.sortedBy { it.name }.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = {
                                            studentGroupFilter = StudentGroupFilter.Specific(group.id)
                                            isStudentGroupFilterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            if (role == "CURATOR") {
                                item {
                                    ListItem(
                                        headlineContent = { Text("Снять всех кураторов", color = MaterialTheme.colorScheme.error) },
                                        leadingContent = { Icon(Icons.Rounded.PersonOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                val group = groups.firstOrNull { it.id == groupId }
                                                val ok = runUiAction(
                                                    actionState = actionState,
                                                    successMessage = "Кураторы сняты",
                                                    fallbackErrorMessage = "Ошибка снятия куратора",
                                                ) {
                                                    val curators = group?.curators.orEmpty()
                                                    var hasError = false
                                                    curators.forEach { curator ->
                                                        val removeResult = registratorRepository.removeCurator(
                                                            token = token,
                                                            groupId = groupId,
                                                            curatorId = curator.id
                                                        )
                                                        if (removeResult.isFailure) hasError = true
                                                    }
                                                    if (hasError) Result.failure<Unit>(IllegalStateException("Не всех кураторов удалось снять"))
                                                    else Result.success(Unit)
                                                }
                                                if (ok) {
                                                    refreshUiAfterMutation(groupId)
                                                }
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                item {
                                    Text(
                                        text = "Уже назначены",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                if (assignedCurators.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Нет назначенных кураторов",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                } else {
                                    items(assignedCurators, key = { it.id }) { curator ->
                                        ListItem(
                                            headlineContent = { Text("${curator.surname} ${curator.name}") },
                                            supportingContent = {
                                                Text(
                                                    text = "Уже добавлен",
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val ok = runUiAction(
                                                                actionState = actionState,
                                                                successMessage = "Куратор снят",
                                                                fallbackErrorMessage = "Ошибка снятия куратора",
                                                            ) {
                                                                registratorRepository.removeCurator(
                                                                    token = token,
                                                                    groupId = groupId,
                                                                    curatorId = curator.id
                                                                )
                                                            }
                                                            if (ok) refreshUiAfterMutation(groupId)
                                                        }
                                                    },
                                                    modifier = Modifier.longPressHelp(
                                                        actionId = "groups.unassign.curator",
                                                        fallbackDescription = "Снять куратора",
                                                    ),
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.PersonOff,
                                                        contentDescription = "Снять куратора",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                                item {
                                    Text(
                                        text = "Доступные к назначению",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                if (selectableUsers.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Ничего не найдено",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                } else {
                                    items(selectableUsers) { user ->
                                        ListItem(
                                            headlineContent = { Text("${user.surname} ${user.name}") },
                                            supportingContent = { Text(user.login) },
                                            modifier = Modifier.clickable {
                                                scope.launch {
                                                    val ok = runUiAction(
                                                        actionState = actionState,
                                                        successMessage = "Куратор назначен",
                                                        fallbackErrorMessage = "Ошибка назначения куратора",
                                                    ) {
                                                        registratorRepository.assignCurator(token, groupId, user.userId)
                                                    }
                                                    if (ok) refreshUiAfterMutation(groupId)
                                                }
                                            }
                                        )
                                    }
                                }
                            } else if (selectableUsers.isEmpty()) {
                                item {
                                    Text(
                                        text = "Ничего не найдено",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            } else {
                                items(selectableUsers) { user ->
                                    ListItem(
                                        headlineContent = { Text("${user.surname} ${user.name}") },
                                        supportingContent = { Text(user.login) },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                val ok = runUiAction(
                                                    actionState = actionState,
                                                    successMessage = "Студент добавлен в группу",
                                                    fallbackErrorMessage = "Ошибка добавления студента в группу",
                                                ) {
                                                    registratorRepository.addStudentToGroup(token, groupId, user.userId)
                                                }
                                                if (ok) refreshUiAfterMutation(groupId)
                                                if (actionState.value !is UiActionState.Error) {
                                                    showAssignMemberDialog = null
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAssignMemberDialog = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    showDeleteGroupDialog?.let { group ->
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Удалить группу?") },
            text = {
                Column {
                    Text("Группа \"${group.name}\" (ID: ${group.id}) будет удалена, студенты будут отвязаны.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "При дубликатах названий проверяйте ID группы перед подтверждением.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = runUiAction(
                            actionState = actionState,
                            successMessage = "Группа удалена",
                            fallbackErrorMessage = "Ошибка удаления группы",
                        ) {
                            registratorRepository.deleteGroup(token, group.id)
                        }
                        if (ok) {
                            showDeleteGroupDialog = null
                            refreshUiAfterMutation()
                        }
                    }
                }, enabled = !isActionLoading) { Text(if (isActionLoading) "Удаление..." else "Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) { Text("Отмена") }
            }
        )
    }
}
