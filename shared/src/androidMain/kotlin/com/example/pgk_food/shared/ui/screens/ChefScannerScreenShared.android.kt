package com.example.pgk_food.shared.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pgk_food.shared.data.remote.dto.QrValidationResponse
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ChefScannerScreenShared(token: String, chefRepository: ChefRepository) {
    var result by remember { mutableStateOf<QrValidationResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted: Boolean -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ScannerScreenLayout(
        hasCameraPermission = hasCameraPermission,
        result = result,
        isLoading = isLoading,
        scannerError = scannerError,
        torchEnabled = torchEnabled,
        isOfflineMode = isOfflineMode,
        onToggleTorch = { torchEnabled = !torchEnabled },
        onToggleOfflineMode = { isOfflineMode = !isOfflineMode },
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onReset = {
            result = null
            scannerError = null
            isLoading = false
            scanResetKey++
        },
        scannerContent = {
            val isScannerActive = hasCameraPermission && result == null && !isLoading
            AndroidCameraPreview(
                isActive = isScannerActive,
                torchEnabled = torchEnabled,
                resetKey = scanResetKey,
                onQrScanned = { qrContent ->
                    if (!isLoading) {
                        scannerError = null
                        isLoading = true
                        scope.launch {
                            val res = chefRepository.validateQr(token, qrContent, isOfflineMode)
                            result = res.getOrNull()
                            if (result == null) {
                                scannerError = res.exceptionOrNull()?.localizedMessage ?: "Ошибка проверки QR"
                            }
                            isLoading = false
                        }
                    }
                },
                onError = { scannerError = it }
            )
            ScannerOverlay(isActive = isScannerActive)
        }
    )
}

@Composable
private fun AndroidCameraPreview(
    isActive: Boolean,
    torchEnabled: Boolean,
    resetKey: Int,
    onQrScanned: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        BarcodeScanning.getClient(options)
    }

    var lastScannedTime by remember { mutableLongStateOf(0L) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val isActiveState by rememberUpdatedState(isActive)
    val onQrScannedState by rememberUpdatedState(onQrScanned)
    val onErrorState by rememberUpdatedState(onError)

    LaunchedEffect(resetKey) { lastScannedTime = 0L }
    LaunchedEffect(torchEnabled, camera) { camera?.cameraControl?.enableTorch(torchEnabled) }

    DisposableEffect(Unit) {
        onDispose {
            try { cameraProviderFuture.get().unbindAll() } catch (_: Exception) {}
            try { scanner.close() } catch (_: Exception) {}
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(0)
                    .build()
                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    if (!isActiveState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    processImageProxy(scanner, imageProxy) { qrContent ->
                        val now = System.currentTimeMillis()
                        if (isActiveState && now - lastScannedTime > 2000) {
                            lastScannedTime = now
                            onQrScannedState(qrContent)
                        }
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    Log.e("SharedCameraPreview", "Binding failed", e)
                    onErrorState("Не удалось запустить камеру")
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy, onQrScanned: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    barcodeScanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) barcode.rawValue?.let(onQrScanned)
        }
        .addOnFailureListener { Log.e("SharedCameraPreview", "Barcode scanning failed", it) }
        .addOnCompleteListener { imageProxy.close() }
}

@Composable
internal fun ScannerScreenLayout(
    hasCameraPermission: Boolean,
    result: QrValidationResponse?,
    isLoading: Boolean,
    scannerError: String?,
    torchEnabled: Boolean,
    isOfflineMode: Boolean,
    onToggleTorch: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onRequestPermission: () -> Unit,
    onReset: () -> Unit,
    scannerContent: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "РЕЖИМ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isOfflineMode) "ОФФЛАЙН (АВТО)" else "ОНЛАЙН (СЕРВЕР)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOfflineMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (hasCameraPermission && result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Оффлайн", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isOfflineMode, onCheckedChange = { onToggleOfflineMode() })
                    IconButton(onClick = onToggleTorch) {
                        Icon(if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (hasCameraPermission && result == null) {
            HowItWorksCard(
                steps = listOf(
                    "Сканируйте QR студента и дождитесь карточки результата.",
                    "При отказе смотрите причину в тексте ошибки перед повторной проверкой.",
                    "При нестабильной сети возможен оффлайн-режим с отложенной синхронизацией.",
                    "Сверяйте тип питания и группу в карточке перед выдачей."
                ),
                note = "Онлайн-результат приоритетнее оффлайн-проверки."
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }

        if (hasCameraPermission && result == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black),
                contentAlignment = Alignment.Center,
                content = scannerContent
            )
        } else if (!hasCameraPermission) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Нужно разрешение на камеру", modifier = Modifier.padding(16.dp))
                    Button(onClick = onRequestPermission) { Text("Разрешить") }
                }
            }
        }

        if (result != null) Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Проверка...")
        }

        scannerError?.let { error ->
            if (!isLoading && result == null) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ошибка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        result?.let {
            val isValid = it.isValid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isValid) "ОТМЕЧЕНО" else "ОТКАЗАНО",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        it.studentName?.let { name -> Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold) }
                        it.groupName?.let { group -> Text("Группа: $group", style = MaterialTheme.typography.bodySmall) }
                        it.mealType?.let { meal -> Text("Питание: $meal", style = MaterialTheme.typography.bodySmall) }
                        it.errorMessage?.let { msg ->
                            Text(
                                msg,
                                color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
            }
        }

        if (hasCameraPermission && result == null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Наведите камеру на QR-код студента",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun ScannerOverlay(isActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(220.dp).border(
                width = 2.dp,
                color = if (isActive) Color(0xFF00E676) else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        )
        if (isActive) ScannerLine()
    }
}

@Composable
private fun ScannerLine() {
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "scanner-line-offset"
    )
    BoxWithConstraints(modifier = Modifier.size(220.dp)) {
        val y = maxHeight * offset
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = y).background(Color(0xFF00E676))
        )
    }
}
