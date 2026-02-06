package com.example.pgk_food.ui.screens

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
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.repository.ChefRepository
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefScannerScreen(token: String, chefRepository: ChefRepository) {
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
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "СКАНЕР QR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            if (hasCameraPermission && result == null) {
                IconButton(
                    onClick = { torchEnabled = !torchEnabled }
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val isScannerActive = hasCameraPermission && result == null && !isLoading

        if (hasCameraPermission && result == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview(
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
                    onError = { error ->
                        scannerError = error
                    }
                )
                
                ScannerOverlay(isActive = isScannerActive)
            }
        } else if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Нужно разрешение на камеру", modifier = Modifier.padding(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Разрешить")
                    }
                }
            }
        }

        if (hasCameraPermission && result == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Офлайн-режим",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Проверка без интернета",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOfflineMode,
                    onCheckedChange = { isOfflineMode = it }
                )
            }
        }
        
        if (result != null) {
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (isLoading) {
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Проверка...")
        }

        scannerError?.let { error ->
            if (!isLoading && result == null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ошибка сканирования",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        result?.let {
            val isValid = it.isValid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValid) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isValid) "ПИТАНИЕ РАЗРЕШЕНО" else "ОТКАЗАНО",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        it.studentName?.let { name -> 
                            Text(
                                text = name, 
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            ) 
                        }
                        it.groupName?.let { group -> 
                            Text(
                                text = "Группа: $group",
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        }
                        it.mealType?.let { meal ->
                            Text(
                                text = "Питание: $meal",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        it.errorMessage?.let { error -> 
                            Text(
                                text = error, 
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    result = null
                    scannerError = null
                    isLoading = false
                    scanResetKey++
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
            }
        }

        if (hasCameraPermission && result == null) {
            Spacer(modifier = Modifier.height(16.dp))
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
fun CameraPreview(
    isActive: Boolean,
    torchEnabled: Boolean,
    resetKey: Int,
    onQrScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    
    // Using a debounced scanner to avoid multiple scans in a row
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val isActiveState by rememberUpdatedState(isActive)
    val onQrScannedState by rememberUpdatedState(onQrScanned)
    val onErrorState by rememberUpdatedState(onError)

    LaunchedEffect(resetKey) {
        lastScannedTime = 0L
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
                // Ignore cleanup errors.
            }
            try {
                scanner.close()
            } catch (_: Exception) {
                // Ignore cleanup errors.
            }
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(0) // ImageAnalysis.STRATEGY_KEEP_ONLY_LAST (compat)
                    .build()

                imageAnalysisUseCase.setAnalyzer(analyzerExecutor) { imageProxy ->
                    if (!isActiveState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    processImageProxy(scanner, imageProxy) { qrContent ->
                        val currentTime = System.currentTimeMillis()
                        if (isActiveState && currentTime - lastScannedTime > 2000) { // 2 seconds debounce
                            lastScannedTime = currentTime
                            onQrScannedState(qrContent)
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                    onErrorState("Не удалось запустить камеру")
                }
            }, executor)
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerOverlay(isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(
                    width = 2.dp,
                    color = if (isActive) Color(0xFF00E676) else Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
        )
        if (isActive) {
            ScannerLine()
        }
    }
}

@Composable
private fun ScannerLine() {
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner-line-offset"
    )

    BoxWithConstraints(
        modifier = Modifier.size(220.dp)
    ) {
        val y = maxHeight * offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = y)
                .background(Color(0xFF00E676))
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onQrScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let {
                        onQrScanned(it)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("CameraPreview", "Barcode scanning failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
