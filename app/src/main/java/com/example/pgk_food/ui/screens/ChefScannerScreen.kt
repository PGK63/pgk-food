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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.repository.ChefRepository
import com.example.pgk_food.ui.theme.HeroCardShape
import com.example.pgk_food.ui.theme.PillShape
import com.example.pgk_food.ui.theme.springEntrance
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.example.pgk_food.ui.viewmodels.ChefViewModel
import com.example.pgk_food.ui.viewmodels.ScanState
import com.example.pgk_food.ui.viewmodels.SyncState
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefScannerScreen(token: String, viewModel: ChefViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    
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

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            snackbarHostState.showSnackbar((syncState as SyncState.Success).message)
            viewModel.resetSyncState()
        } else if (syncState is SyncState.Error) {
            snackbarHostState.showSnackbar((syncState as SyncState.Error).message)
            viewModel.resetSyncState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Сканер питания", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (unsyncedCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(unsyncedCount.toString()) } },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = { viewModel.syncTransactions() }) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = "Синхронизировать")
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.downloadData() }) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = "Скачать данные")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
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
                        text = if (isOfflineMode) "ОФФЛАЙН (ЛОКАЛЬНО)" else "ОНЛАЙН (СЕРВЕР)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOfflineMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Switch(
                    checked = isOfflineMode,
                    onCheckedChange = { isOfflineMode = it },
                    modifier = Modifier.scale(0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val isScannerActive = hasCameraPermission && scanState is ScanState.Idle && syncState is SyncState.Idle

            if (hasCameraPermission && scanState is ScanState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CameraPreview(
                        isActive = isScannerActive,
                        torchEnabled = torchEnabled,
                        resetKey = scanResetKey,
                        onQrScanned = { qrContent ->
                            viewModel.scanQr(qrContent, isOfflineMode)
                        },
                        onError = { error ->
                            // viewModel.setError(error)
                        }
                    )
                    ScannerOverlay(isActive = isScannerActive)
                }
            } else if (!hasCameraPermission) {
                // ... (Permission UI)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Нужно разрешение на камеру", modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = PillShape
                        ) {
                            Text("Разрешить")
                        }
                    }
                }
            }
            
            if (scanState is ScanState.Loading || syncState is SyncState.Loading) {
                Spacer(modifier = Modifier.height(40.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (syncState is SyncState.Loading) (syncState as SyncState.Loading).message else "Проверка...")
            }

            if (scanState is ScanState.Error) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ошибка", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text((scanState as ScanState.Error).message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetScanState() }, modifier = Modifier.fillMaxWidth(), shape = PillShape) {
                    Text("ПОПРОБОВАТЬ СНОВА")
                }
            }
            
            if (scanState is ScanState.Success) {
                val scanResponse = (scanState as ScanState.Success).response
                val isValid = scanResponse.isValid
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = HeroCardShape,
                    modifier = Modifier.fillMaxWidth().springEntrance()
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isValid) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (isValid)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isValid) "ОТМЕЧЕНО" else "ОТКАЗАНО", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
                            if (scanResponse.studentName != null) {
                                Text(scanResponse.studentName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            if (scanResponse.groupName != null) {
                                Text("Группа: ${scanResponse.groupName}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (scanResponse.errorMessage != null) {
                                Text(scanResponse.errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        viewModel.resetScanState()
                        scanResetKey++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
                }
            }

            if (scanState is ScanState.Idle && !hasCameraPermission) {
               // ...
            }
            
            if (hasCameraPermission && scanState is ScanState.Idle) {
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
            } catch (_: Exception) { }
            try {
                scanner.close()
            } catch (_: Exception) { }
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
                    .setBackpressureStrategy(0)
                    .build()

                imageAnalysisUseCase.setAnalyzer(analyzerExecutor) { imageProxy ->
                    if (!isActiveState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    processImageProxy(scanner, imageProxy) { qrContent ->
                        val currentTime = System.currentTimeMillis()
                        if (isActiveState && currentTime - lastScannedTime > 2000) {
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
                    color = if (isActive)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.large
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
                .background(MaterialTheme.colorScheme.tertiary)
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
