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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pgk_food.shared.ui.components.HowItWorksCard
import com.example.pgk_food.shared.ui.theme.HeroCardShape
import com.example.pgk_food.shared.ui.theme.PillShape
import com.example.pgk_food.shared.ui.viewmodels.ChefViewModel
import com.example.pgk_food.shared.ui.viewmodels.ScanState
import com.example.pgk_food.shared.ui.viewmodels.SyncState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ChefScannerScreenShared(
    token: String,
    viewModel: ChefViewModel,
    showHints: Boolean,
    onHideHints: () -> Unit,
) {
    val scanState by viewModel.scanState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isOfflineMode by viewModel.isOffline.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanResetKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
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

    LaunchedEffect(token) {
        viewModel.updateUnsyncedCount()
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
                                Icon(Icons.Default.CloudSync, contentDescription = "Синхронизировать")
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.downloadData() }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Скачать данные")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isCompactHeight = maxHeight < 760.dp
            val horizontalPadding = if (isCompactHeight) 16.dp else 24.dp
            val verticalPadding = if (isCompactHeight) 12.dp else 24.dp
            val cameraHeightCandidate = if (isCompactHeight) maxHeight * 0.44f else maxHeight * 0.5f
            val cameraHeight = when {
                cameraHeightCandidate < 280.dp -> 280.dp
                cameraHeightCandidate > 430.dp -> 430.dp
                else -> cameraHeightCandidate
            }
            val frameByHeight = cameraHeight - if (isCompactHeight) 22.dp else 28.dp
            val frameByWidth = maxWidth - if (isCompactHeight) 56.dp else 72.dp
            val frameSizeCandidate = if (frameByHeight < frameByWidth) frameByHeight else frameByWidth
            val scannerFrameSize = when {
                frameSizeCandidate < 244.dp -> 244.dp
                frameSizeCandidate > 372.dp -> 372.dp
                else -> frameSizeCandidate
            }
            val hintsTopSpacer = if (isCompactHeight) 8.dp else 12.dp
            val hintsBottomSpacer = if (isCompactHeight) 16.dp else 24.dp
            val loadingTopSpacer = if (isCompactHeight) 24.dp else 40.dp
            val resultTopSpacer = if (isCompactHeight) 16.dp else 24.dp
            val controlsSpacer = if (isCompactHeight) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
            }

            if (showHints) {
                Spacer(modifier = Modifier.height(hintsTopSpacer))
                HowItWorksCard(
                    steps = listOf(
                        "Сканируйте QR студента и дождитесь карточки результата.",
                        "Если есть отказ, исправьте причину и повторите проверку.",
                        "При потере сети сканер автоматически уходит в оффлайн с последующей синхронизацией.",
                        "Сверяйте тип питания и группу в карточке перед выдачей."
                    ),
                    note = "Онлайн-результат приоритетнее оффлайн-проверки.",
                    onHideHints = onHideHints,
                )
                Spacer(modifier = Modifier.height(hintsBottomSpacer))
            } else {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 20.dp))
            }
            val isScannerActive =
                hasCameraPermission && scanState is ScanState.Idle && syncState is SyncState.Idle

            if (hasCameraPermission && scanState is ScanState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cameraHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CameraPreview(
                        isActive = isScannerActive,
                        torchEnabled = torchEnabled,
                        resetKey = scanResetKey,
                        onQrScanned = { qrContent ->
                            viewModel.scanQr(qrContent)
                        },
                        onError = { _ -> }
                    )
                    ScannerOverlay(
                        isActive = isScannerActive,
                        frameSize = scannerFrameSize,
                    )
                }
            } else if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cameraHeight)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
                Spacer(modifier = Modifier.height(loadingTopSpacer))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(controlsSpacer))
                Text(if (syncState is SyncState.Loading) (syncState as SyncState.Loading).message else "Проверка...")
            }

            if (scanState is ScanState.Error) {
                Spacer(modifier = Modifier.height(resultTopSpacer))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ошибка", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text((scanState as ScanState.Error).message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(controlsSpacer))
                Button(onClick = { viewModel.resetScanState() }, modifier = Modifier.fillMaxWidth(), shape = PillShape) {
                    Text("ПОПРОБОВАТЬ СНОВА")
                }
            }

            if (scanState is ScanState.Success) {
                val scanResponse = (scanState as ScanState.Success).response
                val isValid = scanResponse.isValid
                Spacer(modifier = Modifier.height(resultTopSpacer))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = HeroCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isValid)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                if (isValid) "ОТМЕЧЕНО" else "ОТКАЗАНО",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
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

                Spacer(modifier = Modifier.height(controlsSpacer))

                Button(
                    onClick = {
                        viewModel.resetScanState()
                        scanResetKey++
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
                }
            }

            if (hasCameraPermission && scanState is ScanState.Idle) {
                Spacer(modifier = Modifier.height(controlsSpacer))
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
}

@Composable
private fun CameraPreview(
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
            } catch (_: Exception) {
            }
            try {
                scanner.close()
            } catch (_: Exception) {
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
private fun ScannerOverlay(
    isActive: Boolean,
    frameSize: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(frameSize)
                .border(
                    width = 2.dp,
                    color = if (isActive) Color(0xFF00E676) else Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
        )
        if (isActive) ScannerLine(frameSize = frameSize)
    }
}

@Composable
private fun ScannerLine(frameSize: Dp) {
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner-line-offset"
    )
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.size(frameSize)) {
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
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    barcodeScanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                barcode.rawValue?.let(onQrScanned)
            }
        }
        .addOnFailureListener { e ->
            Log.e("CameraPreview", "Barcode scanning failed", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
