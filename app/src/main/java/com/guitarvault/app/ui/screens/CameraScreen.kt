package com.guitarvault.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarvault.app.ai.MlKitBackgroundRemover
import com.guitarvault.app.camera.CameraCaptureManager
import com.guitarvault.app.camera.CaptureResult
import com.guitarvault.app.data.model.GuitarPhoto
import com.guitarvault.app.data.model.PhotoType
import com.guitarvault.app.ui.viewmodel.CollectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    guitarId: String,
    onCaptureComplete: () -> Unit,
    onBack: () -> Unit,
    onPhotoSaved: (GuitarPhoto) -> Unit = {},
    viewModel: CollectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isCapturing by remember { mutableStateOf(false) }
    var captureStatus by remember { mutableStateOf("") }
    var selectedPhotoType by remember { mutableStateOf(PhotoType.GENERAL) }
    var lensFacing by remember { mutableStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val backgroundRemover = remember { MlKitBackgroundRemover.getInstance(context) }
    val cameraManager = remember { CameraCaptureManager(context, backgroundRemover) }
    val previewView = remember { PreviewView(context) }

    // Start camera when permission is granted
    LaunchedEffect(hasCameraPermission, lensFacing) {
        if (hasCameraPermission) {
            cameraManager.startCamera(lifecycleOwner, previewView, lensFacing)
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (hasCameraPermission && !isCapturing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Photo type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PhotoType.entries.take(6).forEach { type ->
                            FilterChip(
                                selected = selectedPhotoType == type,
                                onClick = { selectedPhotoType = type },
                                label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flip camera
                        IconButton(onClick = {
                            lensFacing = if (lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                                androidx.camera.core.CameraSelector.LENS_FACING_FRONT
                            else
                                androidx.camera.core.CameraSelector.LENS_FACING_BACK
                        }) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera")
                        }

                        // Capture button
                        FilledIconButton(
                            onClick = {
                                if (!isCapturing) {
                                    isCapturing = true
                                    captureStatus = "Capturing..."
                                    scope.launch {
                                        val outputFile = java.io.File(
                                            context.filesDir,
                                            "photos/photo_${System.currentTimeMillis()}.png"
                                        ).also { it.parentFile?.mkdirs() }

                                        val result = cameraManager.captureAndRemoveBackground(
                                            outputFile = outputFile
                                        ) { progress -> captureStatus = progress }

                                        when (result) {
                                            is CaptureResult.Success -> {
                                                val relativePath = "photos/${outputFile.name}"
                                                val photo = GuitarPhoto(
                                                    filePath = relativePath,
                                                    backgroundRemoved = result.backgroundRemoved,
                                                    photoType = selectedPhotoType,
                                                    isPrimary = false
                                                )
                                                viewModel.addPhotoToGuitar(guitarId, photo)
                                                onPhotoSaved(photo)
                                                onCaptureComplete()
                                            }
                                            is CaptureResult.Error -> {
                                                captureStatus = "Error: ${result.message}"
                                                isCapturing = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(36.dp))
                        }

                        // Spacer to balance the flip button
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission needed", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else if (isCapturing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(captureStatus, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
