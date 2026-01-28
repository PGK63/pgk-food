package com.example.pgk_food.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
    
    // For student management
    var expandedGroupId by remember { mutableStateOf<Int?>(null) }
    var groupStudents by remember { mutableStateOf<Map<Int, List<UserDto>>>(emptyMap()) }
    
    // For assigning curator/student
    var showAssignMemberDialog by remember { mutableStateOf<Pair<Int, String>?>(null) } // groupId to "CURATOR" or "STUDENT"
    var allUsers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    
    val scope = rememberCoroutineScope()

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
            FloatingActionButton(onClick = { showAddGroupDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить группу")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = "Управление группами", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(groups) { group ->
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
                                }
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
                                            Icon(Icons.Default.Person, contentDescription = "Назначить куратора")
                                        }
                                        IconButton(onClick = { 
                                            scope.launch {
                                                registratorRepository.removeCurator(token, group.id)
                                                refreshGroups()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Снять куратора")
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
                                                        registratorRepository.removeStudentFromGroup(token, group.id, student.userId)
                                                        loadStudents(group.id)
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Убрать из группы", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { showAssignMemberDialog = group.id to "STUDENT" },
                                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
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
            title = { Text("Создать группу") },
            text = {
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Название группы") }
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
                                        } else {
                                            registratorRepository.addStudentToGroup(token, groupId, user.userId)
                                            loadStudents(groupId)
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
}
