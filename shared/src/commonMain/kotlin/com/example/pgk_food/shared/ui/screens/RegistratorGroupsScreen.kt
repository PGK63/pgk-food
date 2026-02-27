package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.remote.dto.UserDto
import com.example.pgk_food.shared.data.repository.RegistratorRepository
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistratorGroupsScreen(
    token: String,
    registratorRepository: RegistratorRepository,
    showHints: Boolean = true,
    onHideHints: () -> Unit = {}
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

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    fun refreshGroups() {
        scope.launch {
            isLoading = true
            val result = registratorRepository.getGroups(token)
            groups = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    fun loadStudents(groupId: Int) {
        scope.launch {
            val result = registratorRepository.getUsers(token, groupId)
            val students = result.getOrDefault(emptyList())
            groupStudents = groupStudents + (groupId to students)
        }
    }

    fun loadAllUsers() {
        scope.launch {
            val result = registratorRepository.getUsers(token)
            allUsers = result.getOrDefault(emptyList())
        }
    }

    suspend fun transferGroup(group: GroupDto, targetName: String) {
        val newGroupNameValue = targetName.trim()
        if (newGroupNameValue.isEmpty()) {
            snackbarHostState.showSnackbar("Введите новое название группы")
            return
        }

        var transferError: String? = null
        val movedStudents = mutableListOf<String>()
        var curatorReassigned = false

        registratorRepository.createGroup(token, newGroupNameValue).onFailure {
            transferError = "Не удалось создать новую группу: ${it.message ?: "неизвестная ошибка"}"
        }
        if (transferError != null) {
            snackbarHostState.showSnackbar(transferError!!)
            return
        }

        val freshGroups = registratorRepository.getGroups(token).getOrDefault(emptyList())
        val newGroup = freshGroups
            .filter { it.id != group.id && it.name == newGroupNameValue }
            .maxByOrNull { it.id }

        if (newGroup == null) {
            snackbarHostState.showSnackbar("Новая группа создана, но не найдена в списке")
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
                    transferError = "Не удалось перенести студента ${student.surname} ${student.name}: ${it.message ?: "неизвестная ошибка"}"
                }
        }

        if (transferError == null && group.curatorId != null) {
            registratorRepository.assignCurator(token, newGroup.id, group.curatorId).onSuccess {
                curatorReassigned = true
            }.onFailure {
                transferError = "Не удалось переназначить куратора: ${it.message ?: "неизвестная ошибка"}"
            }
        }

        if (transferError == null) {
            registratorRepository.deleteGroup(token, group.id).onFailure {
                transferError = "Студенты перенесены, но удалить старую группу не удалось: ${it.message ?: "неизвестная ошибка"}"
            }
        }

        if (transferError != null) {
            val rollbackErrors = mutableListOf<String>()

            movedStudents.forEach { studentId ->
                registratorRepository.addStudentToGroup(token, group.id, studentId).onFailure {
                    rollbackErrors += "Не удалось вернуть студента $studentId"
                }
            }

            if (curatorReassigned && group.curatorId != null) {
                registratorRepository.assignCurator(token, group.id, group.curatorId).onFailure {
                    rollbackErrors += "Не удалось вернуть куратора"
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

        refreshGroups()
        loadAllUsers()
        loadStudents(group.id)
        loadStudents(newGroup.id)
    }

    LaunchedEffect(Unit) {
        refreshGroups()
        loadAllUsers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGroupDialog = true },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить группу")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Управление группами",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Найдено: ${filteredGroups.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showHints) {
                Spacer(modifier = Modifier.height(8.dp))
                HowItWorksCard(
                    steps = listOf(
                        "Операции с группами доступны только ролям REGISTRATOR/ADMIN.",
                        "При переносе группы выбирайте целевую группу по ID, а не только по названию.",
                        "Если есть дубликаты названий (например две ИСП-31), сверяйте куратора и число студентов.",
                        "Перед удалением группы убедитесь, что студенты уже перенесены."
                    ),
                    note = "Правило безопасности: при дубликатах выбор только по ID.",
                    onHideHints = onHideHints
                )
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
                    items(filteredGroups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (expandedGroupId == group.id) {
                                        expandedGroupId = null
                                    } else {
                                        expandedGroupId = group.id
                                        loadStudents(group.id)
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
                                        if (group.curatorName != null) {
                                            Text(
                                                text = "Куратор: ${group.curatorSurname} ${group.curatorName} ${group.curatorFatherName ?: ""}",
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
                                        IconButton(onClick = { showAssignMemberDialog = group.id to "CURATOR" }) {
                                            Icon(Icons.Rounded.Person, contentDescription = "Назначить куратора", modifier = Modifier.size(22.dp))
                                        }
                                        IconButton(onClick = { showTransferGroupDialog = group }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Перевести/переименовать", modifier = Modifier.size(22.dp))
                                        }
                                        if (group.curatorId != null) {
                                            IconButton(onClick = { 
                                                scope.launch {
                                                    registratorRepository.removeCurator(token, group.id)
                                                    refreshGroups()
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Rounded.PersonOff, 
                                                    contentDescription = "Снять куратора",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = { showDeleteGroupDialog = group }) {
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
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        registratorRepository.removeStudentFromGroup(token, student.userId)
                                                        loadStudents(group.id)
                                                        refreshGroups()
                                                    }
                                                }) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Действие требует роль REGISTRATOR/ADMIN. При совпадении названий ориентируйтесь по ID группы.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        registratorRepository.createGroup(token, newGroupName)
                        newGroupName = ""
                        showAddGroupDialog = false
                        refreshGroups()
                    }
                }) {
                    Text("Создать")
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
                    enabled = targetGroupName.isNotBlank()
                ) {
                    Text("Перенести")
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
        val filteredUsers = allUsers.filter { user ->
            if (role == "CURATOR") user.roles.contains(UserRole.CURATOR)
            else user.roles.contains(UserRole.STUDENT)
        }

        AlertDialog(
            onDismissRequest = { showAssignMemberDialog = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(if (role == "CURATOR") "Назначить куратора" else "Добавить студента") },
            text = {
                Column {
                    Text(
                        text = "Действие требует роль REGISTRATOR/ADMIN.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            if (role == "CURATOR") {
                                item {
                                    ListItem(
                                        headlineContent = { Text("Снять текущего куратора", color = MaterialTheme.colorScheme.error) },
                                        leadingContent = { Icon(Icons.Rounded.PersonOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                registratorRepository.removeCurator(token, groupId)
                                                refreshGroups()
                                                showAssignMemberDialog = null
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                            items(filteredUsers) { user ->
                                ListItem(
                                    headlineContent = { Text("${user.surname} ${user.name}") },
                                    supportingContent = { Text(user.login) },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            if (role == "CURATOR") {
                                                registratorRepository.assignCurator(token, groupId, user.userId)
                                                refreshGroups()
                                            } else {
                                                registratorRepository.addStudentToGroup(token, groupId, user.userId)
                                                loadStudents(groupId)
                                                refreshGroups()
                                            }
                                            showAssignMemberDialog = null
                                        }
                                    }
                                )
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
                        registratorRepository.deleteGroup(token, group.id)
                        showDeleteGroupDialog = null
                        refreshGroups()
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) { Text("Отмена") }
            }
        )
    }
}
