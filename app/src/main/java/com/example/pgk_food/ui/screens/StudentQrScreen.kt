package com.example.pgk_food.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.util.QrGenerator
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun StudentQrScreen(
    session: UserSessionEntity,
    mealType: String
) {
    val studentRepository = remember { StudentRepository() }
    var timeLeft by remember { mutableIntStateOf(30) }
    var qrContent by remember { mutableStateOf("") }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        val serverTime = studentRepository.getCurrentTime()
        serverTimeOffset = serverTime - System.currentTimeMillis()
    }

    LaunchedEffect(qrContent) {
        if (qrContent.isNotEmpty()) {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            qrContent = ""
        }
    }

    LaunchedEffect(qrContent) {
        if (qrContent.isEmpty()) {
            val timestamp = System.currentTimeMillis() + serverTimeOffset
            val nonce = UUID.randomUUID().toString()
            val rawData = "${session.userId}|$timestamp|$mealType|$nonce"
            val signature = "sig_${rawData.hashCode()}" 
            qrContent = "userId=${session.userId}&ts=$timestamp&type=$mealType&nonce=$nonce&sig=$signature"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ПРОПУСК В СТОЛОВУЮ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = mealType.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrContent.isNotEmpty()) {
                        val bitmap = remember(qrContent) {
                            QrGenerator.generateQrCode(qrContent, 512)
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Timer, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Обновление: $timeLeft сек",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (timeLeft == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { qrContent = "" },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ОБНОВИТЬ СЕЙЧАС")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Покажите этот код повару на раздаче",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
