package com.guitarvault.app.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.guitarvault.app.ai.BackgroundRemover
import com.guitarvault.app.ai.BackgroundRemovalResult
import com.guitarvault.app.ai.SegmentationIsolatedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Camera capture + AI background removal pipeline.
 *
 * Flow: CameraX capture → temp file → isolated process ML Kit segmentation → output file
 *
 * The ML Kit segmentation runs in a separate process (:segmentation) so that
 * if it crashes (SIGSEGV from MTE on Android 16), the main app survives
 * and falls back to saving the original photo.
 */
class CameraCaptureManager(
    private val context: Context,
    private val backgroundRemover: BackgroundRemover
) {
    companion object { private const val TAG = "CameraCaptureManager" }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val imageCapture: ImageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Capture a photo, then run AI background removal in an isolated process.
     * If the isolated process crashes, falls back to the original photo.
     */
    suspend fun captureAndRemoveBackground(
        outputFile: File,
        onProgress: ((String) -> Unit)? = null
    ): CaptureResult {
        onProgress?.invoke("Capturing photo...")
        val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

        // Step 1: Capture photo with CameraX
        val captureSuccess = capturePhoto(tempFile)
        if (!captureSuccess) {
            return CaptureResult.Error("Failed to capture photo")
        }

        // Step 2: Load captured bitmap
        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            ?: return CaptureResult.Error("Failed to decode captured image")

        // Step 3: Save original first (as fallback)
        withContext(Dispatchers.IO) {
            outputFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // Step 4: Try background removal in isolated process
        onProgress?.invoke("AI: Detecting guitar subject...")

        val segInputFile = File(context.cacheDir, "seg_input_${System.currentTimeMillis()}.jpg")
        val segOutputFile = File(context.cacheDir, "seg_output_${System.currentTimeMillis()}.png")

        withContext(Dispatchers.IO) {
            segInputFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }

        // Start the isolated service
        val serviceIntent = Intent(context, SegmentationIsolatedService::class.java).apply {
            putExtra(SegmentationIsolatedService.EXTRA_INPUT_PATH, segInputFile.absolutePath)
            putExtra(SegmentationIsolatedService.EXTRA_OUTPUT_PATH, segOutputFile.absolutePath)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Wait for output file (with timeout)
        var bgRemoved = false
        val maxWaitMs = 35_000L
        val checkIntervalMs = 500L
        var waited = 0L

        withContext(Dispatchers.IO) {
            while (waited < maxWaitMs) {
                if (segOutputFile.exists() && segOutputFile.length() > 0) {
                    // Success! Replace output with the background-removed version
                    segOutputFile.copyTo(outputFile, overwrite = true)
                    bgRemoved = true
                    onProgress?.invoke("Done! Background removed.")
                    break
                }
                delay(checkIntervalMs)
                waited += checkIntervalMs

                // Check if the segmentation process is still alive
                // If it crashed, the output file will never appear
                if (waited % 5000 == 0L) {
                    onProgress?.invoke("Processing AI... (${waited / 1000}s)")
                }
            }
        }

        // Cleanup temp files
        segInputFile.delete()
        segOutputFile.delete()
        tempFile.delete()

        if (!bgRemoved) {
            onProgress?.invoke("Saved (AI unavailable — original kept)")
            Log.w(TAG, "Background removal did not complete, using original photo")
        }

        return CaptureResult.Success(
            file = outputFile,
            backgroundRemoved = bgRemoved,
            bitmap = bitmap
        )
    }

    /**
     * Process an existing image (from gallery) through background removal.
     */
    suspend fun processExistingImage(
        sourceUri: Uri,
        outputFile: File,
        onProgress: ((String) -> Unit)? = null
    ): CaptureResult {
        onProgress?.invoke("Loading image...")

        val result = try {
            backgroundRemover.removeBackground(sourceUri) { msg ->
                onProgress?.invoke(msg)
            }
        } catch (e: Exception) {
            BackgroundRemovalResult.Error(e.message ?: "Unknown error")
        }

        val bitmap = when (result) {
            is BackgroundRemovalResult.Success -> result.bitmap
            is BackgroundRemovalResult.Error -> {
                return CaptureResult.Error(result.message)
            }
        }

        withContext(Dispatchers.IO) {
            outputFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        return CaptureResult.Success(
            file = outputFile,
            backgroundRemoved = true,
            bitmap = bitmap
        )
    }

    private suspend fun capturePhoto(targetFile: File): Boolean {
        return suspendCancellableCoroutine { cont ->
            val outputOptions = ImageCapture.OutputFileOptions.Builder(targetFile).build()
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (cont.isActive) cont.resume(true)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Capture failed", exception)
                        if (cont.isActive) cont.resume(false)
                    }
                }
            )
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}

sealed class CaptureResult {
    data class Success(
        val file: File,
        val backgroundRemoved: Boolean,
        val bitmap: Bitmap
    ) : CaptureResult()
    data class Error(val message: String) : CaptureResult()
}
