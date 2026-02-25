package com.example.pgk_food.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pgk_food.data.remote.dto.QrPayload
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.ui.theme.GlassSurface
import com.example.pgk_food.ui.theme.HeroCardShape
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.springEntrance
import com.example.pgk_food.util.QrCrypto
import com.example.pgk_food.util.QrGenerator
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Composable
fun StudentQrScreen(
    session: UserSessionEntity,
    mealType: String
) {
    val studentRepository = remember { StudentRepository() }
    var timeLeft by remember { mutableIntStateOf(30) }
    var qrContent by remember { mutableStateOf("") }
    var serverTimeOffset by remember { mutableLongStateOf(0L) }
    
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        val serverTime = studentRepository.getCurrentTime()
        serverTimeOffset = serverTime - System.currentTimeMillis()
        refreshTrigger++
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            val timestampMs = System.currentTimeMillis() + serverTimeOffset
            val timestampSec = timestampMs / 1000
            val roundedTimestamp = (timestampSec / 30) * 30
            val nonce = UUID.randomUUID().toString()
            val privateKey = session.privateKey
            if (privateKey.isNullOrBlank()) {
                qrContent = "ERROR_KEY"
                timeLeft = 0
                return@LaunchedEffect
            }

            val signature = QrCrypto.generateSignature(
                userId = session.userId,
                timestamp = roundedTimestamp,
                mealType = mealType,
                nonce = nonce,
                privateKeyBase64 = privateKey
            )
            if (signature.isBlank()) {
                qrContent = "ERROR_SIG"
                timeLeft = 0
                return@LaunchedEffect
            }

            val payload = QrPayload(
                userId = session.userId,
                timestamp = roundedTimestamp,
                mealType = mealType,
                nonce = nonce,
                signature = signature
            )

            qrContent = Json.encodeToString(payload)
            timeLeft = 30
        }
    }

    LaunchedEffect(qrContent) {
        if (qrContent.isNotEmpty() && !qrContent.startsWith("ERROR")) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    // Animated glow border
    val infiniteTransition = rememberInfiniteTransition(label = "qr-glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // QR Card — Glassmorphism
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .springEntrance(),
            shape = HeroCardShape
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
                
                val displayMealType = when (mealType.uppercase()) {
                    "BREAKFAST" -> "ЗАВТРАК"
                    "LUNCH" -> "ОБЕД"
                    "DINNER" -> "УЖИН"
                    "SNACK" -> "ПОЛДНИК"
                    "SPECIAL" -> "СПЕЦ. ПИТАНИЕ"
                    else -> mealType
                }

                Text(
                    text = displayMealType.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // QR code box with animated glow border
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha),
                            shape = MaterialTheme.shapes.large
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrContent.startsWith("ERROR")) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (qrContent == "ERROR_KEY")
                                    "Ключ не найден.\nПопробуйте перезайти\nв приложение."
                                else
                                    "Ошибка подписи.\nПопробуйте обновить.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { refreshTrigger++ }) {
                                Text("Обновить")
                            }
                        }
                    } else if (qrContent.isNotEmpty()) {
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
                
                // Timer badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Timer, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Обновление: $timeLeft сек",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (timeLeft == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { refreshTrigger++ },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = PillShape
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ОБНОВИТЬ СЕЙЧАС")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Покажите этот код повару на раздаче",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
