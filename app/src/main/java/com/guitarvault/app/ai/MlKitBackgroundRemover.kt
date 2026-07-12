package com.guitarvault.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.Subject
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ML Kit Subject Segmentation implementation for on-device background removal.
 *
 * Uses Google's ML Kit Subject Segmentation API which:
 * - Runs entirely on-device (no cloud calls)
 * - Detects foreground subjects and generates a FloatBuffer confidence mask
 * - Requires API 31+ (Android 12+)
 */
class MlKitBackgroundRemover private constructor(
    private val context: Context
) : BackgroundRemover {

    companion object {
        private const val TAG = "MlKitBgRemover"
        private const val CONFIDENCE_THRESHOLD = 0.5f

        @Volatile private var INSTANCE: MlKitBackgroundRemover? = null
        fun getInstance(context: Context): MlKitBackgroundRemover {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MlKitBackgroundRemover(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val segmenter: SubjectSegmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .enableMultipleSubjects(SubjectSegmenterOptions.SubjectResultOptions.Builder()
                .enableConfidenceMask()
                .build())
            .build()
        SubjectSegmentation.getClient(options)
    }

    override suspend fun removeBackground(
        inputUri: Uri,
        onProgress: ((String) -> Unit)?
    ): BackgroundRemovalResult {
        return try {
            onProgress?.invoke("Loading image...")
            val bitmap = loadBitmapFromUri(inputUri, context)
                ?: return BackgroundRemovalResult.Error("Failed to load image from URI")
            removeBackground(bitmap, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "URI background removal failed", e)
            BackgroundRemovalResult.Error(e.message ?: "Unknown error loading image")
        }
    }

    override suspend fun removeBackground(
        inputBitmap: Bitmap,
        onProgress: ((String) -> Unit)?
    ): BackgroundRemovalResult {
        return try {
            onProgress?.invoke("Preparing image for AI analysis...")

            // Downscale very large bitmaps for performance
            val scaledBitmap = scaleIfNeeded(inputBitmap, maxDimension = 1024)
            val image = InputImage.fromBitmap(scaledBitmap, 0)

            onProgress?.invoke("Running on-device AI subject detection...")

            val result = runSegmentation(image)

            onProgress?.invoke("Extracting guitar from background...")

            val outputBitmap = applyForegroundMask(scaledBitmap, result)
            BackgroundRemovalResult.Success(outputBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Background removal failed", e)
            BackgroundRemovalResult.Error(e.message ?: "Unknown AI processing error")
        }
    }

    private suspend fun runSegmentation(image: InputImage): SubjectSegmentationResult.Success {
        return suspendCancellableCoroutine { cont ->
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(SubjectSegmentationResult.Success(result))
                }
                .addOnFailureListener { e ->
                    cont.resume(SubjectSegmentationResult.Failure(e))
                }
        }.let { result ->
            when (result) {
                is SubjectSegmentationResult.Success -> result
                is SubjectSegmentationResult.Failure -> throw result.exception
            }
        }
    }
}
