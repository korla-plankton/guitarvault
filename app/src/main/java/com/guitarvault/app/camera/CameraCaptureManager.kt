package com.guitarvault.app.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
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
     * Capture a photo and save it (no auto background removal).
     * Background removal is now a manual action via the magic wand button.
     */
    suspend fun capturePhoto(
        outputFile: File,
        onProgress: ((String) -> Unit)? = null
    ): CaptureResult {
        onProgress?.invoke("Capturing photo...")
        val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

        // Step 1: Capture photo with CameraX
        val captureSuccess = capturePhotoInternal(tempFile)
        if (!captureSuccess) {
            return CaptureResult.Error("Failed to capture photo")
        }

        // Step 2: Load captured bitmap and correct its orientation.
        // CameraX writes JPEGs in sensor orientation with rotation recorded only in
        // the EXIF tag; BitmapFactory ignores that tag, so portrait shots decode
        // sideways. Read the tag and rotate the pixels before re-saving (PNG has no
        // EXIF, so the rotation must be baked in here).
        val decoded = BitmapFactory.decodeFile(tempFile.absolutePath)
            ?: return CaptureResult.Error("Failed to decode captured image")
        val bitmap = applyExifRotation(tempFile, decoded)

        // Step 3: Save to output file
        withContext(Dispatchers.IO) {
            outputFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // Cleanup temp
        tempFile.delete()

        onProgress?.invoke("Done!")

        return CaptureResult.Success(
            file = outputFile,
            backgroundRemoved = false,
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

    /** Rotate a decoded bitmap according to the EXIF orientation tag of the source file. */
    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (rotation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF orientation", e)
            bitmap
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
