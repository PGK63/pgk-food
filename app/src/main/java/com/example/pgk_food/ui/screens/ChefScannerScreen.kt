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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.repository.ChefRepository
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
        Text(
            text = "СКАНЕР QR",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                    onQrScanned = { qrContent ->
                        if (!isLoading) {
                            isLoading = true
                            scope.launch {
                                val res = chefRepository.validateQr(token, qrContent)
                                result = res.getOrNull()
                                isLoading = false
                            }
                        }
                    }
                )
                
                // Overlay to indicate scanning area
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Transparent)
                        .padding(2.dp)
                ) {
                    // Simple corners or frame could be added here
                }
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
        
        if (result != null) {
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (isLoading) {
            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Проверка...")
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
                onClick = { result = null },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("СКАНЕРОВАТЬ ЕЩЕ РАЗ")
            }
        }
    }
}

@Composable
fun CameraPreview(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // Using a debounced scanner to avoid multiple scans in a row
    var lastScannedTime by remember { mutableLongStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val scannerOptions = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(scannerOptions)

                val imageAnalysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(0) // ImageAnalysis.STRATEGY_KEEP_ONLY_LAST
                    .build()

                imageAnalysisUseCase.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    processImageProxy(scanner, imageProxy) { qrContent ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastScannedTime > 2000) { // 2 seconds debounce
                            lastScannedTime = currentTime
                            onQrScanned(qrContent)
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, executor)
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
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
