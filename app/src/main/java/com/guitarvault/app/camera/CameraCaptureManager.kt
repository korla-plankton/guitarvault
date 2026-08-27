package com.guitarvault.app.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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
     * Capture a photo and save it (no auto background removal).
     * Background removal is a manual action via the magic wand button.
     *
     * Snappy path: the JPEG that CameraX writes is kept as-is — no decode +
     * PNG re-encode round trip. Orientation lives in the EXIF tag, which
     * every display path we use honours (Coil, FullScreenPhotoViewer, the
     * segmentation pipeline). Keeping the JPEG also shrinks files ~5x.
     */
    suspend fun capturePhoto(
        outputFile: File,
        onProgress: ((String) -> Unit)? = null
    ): CaptureResult {
        onProgress?.invoke("Capturing photo...")

        val success = withContext(Dispatchers.IO) {
            capturePhotoInternal(outputFile)
        }
        if (!success) {
            return CaptureResult.Error("Failed to capture photo")
        }

        onProgress?.invoke("Done!")

        return CaptureResult.Success(
            file = outputFile,
            backgroundRemoved = false,
            bitmap = null
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

    private suspend fun capturePhotoInternal(targetFile: File): Boolean {
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
        val bitmap: Bitmap? = null
    ) : CaptureResult()
    data class Error(val message: String) : CaptureResult()
}
