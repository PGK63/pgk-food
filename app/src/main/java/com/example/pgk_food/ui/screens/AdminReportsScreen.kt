package com.example.pgk_food.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminReportsScreen(token: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Отчеты", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Ежедневный отчет", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = { /* Export PDF */ }, modifier = Modifier.weight(1f)) {
                        Text("PDF")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* Export CSV */ }, modifier = Modifier.weight(1f)) {
                        Text("CSV")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Еженедельный отчет", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = { /* Export PDF */ }, modifier = Modifier.weight(1f)) {
                        Text("PDF")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* Export CSV */ }, modifier = Modifier.weight(1f)) {
                        Text("CSV")
                    }
                }
            }
        }
    }
}
