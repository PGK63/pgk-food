package com.example.pgk_food.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import com.example.pgk_food.data.remote.dto.GroupDto
import com.example.pgk_food.data.remote.dto.UserDto
import com.example.pgk_food.data.repository.RegistratorRepository
import com.example.pgk_food.model.UserRole
import kotlinx.coroutines.launch

@Composable
fun RegistratorGroupsScreen(token: String, registratorRepository: RegistratorRepository) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showDeleteGroupDialog by remember { mutableStateOf<GroupDto?>(null) }
    
    // For student management
    var expandedGroupId by remember { mutableStateOf<Int?>(null) }
    var groupStudents by remember { mutableStateOf<Map<Int, List<UserDto>>>(emptyMap()) }
    
    // For assigning curator/student
    var showAssignMemberDialog by remember { mutableStateOf<Pair<Int, String>?>(null) } // groupId to "CURATOR" or "STUDENT"
    var allUsers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    
    // Search
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        refreshGroups()
        loadAllUsers()
    }

    Scaffold(
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
            Text(text = "Управление группами", style = MaterialTheme.typography.headlineMedium)
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
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                LazyColumn {
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
                                    Column {
                                        Text(group.name, style = MaterialTheme.typography.titleLarge)
                                        if (group.curatorName != null) {
                                            Text(
                                                "Куратор: ${group.curatorSurname} ${group.curatorName} ${group.curatorFatherName ?: ""}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            Text("Куратор не назначен", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                        }
                                        Text("Студентов: ${group.studentCount}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row {
                                        IconButton(onClick = { showAssignMemberDialog = group.id to "CURATOR" }) {
                                            Icon(Icons.Rounded.Person, contentDescription = "Назначить куратора")
                                        }
                                        IconButton(onClick = { 
                                            scope.launch {
                                                registratorRepository.removeCurator(token, group.id)
                                                refreshGroups()
                                            }
                                        }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Снять куратора")
                                        }
                                        IconButton(onClick = { showDeleteGroupDialog = group }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Удалить группу")
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
                                                Text("${student.surname} ${student.name}")
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
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Название группы") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
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
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
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
            text = { Text("Группа \"${group.name}\" будет удалена, студенты будут отвязаны.") },
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

