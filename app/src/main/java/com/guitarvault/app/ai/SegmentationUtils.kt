package com.guitarvault.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult as MlKitResult
import java.io.InputStream
import java.nio.FloatBuffer

/**
 * Wrapper for ML Kit segmentation result — success or failure.
 */
sealed class SubjectSegmentationResult {
    data class Success(val mlKitResult: MlKitResult) : SubjectSegmentationResult()
    data class Failure(val exception: Exception) : SubjectSegmentationResult()
}

/**
 * Applies the foreground confidence mask from ML Kit to produce a bitmap
 * with the background removed (replaced with transparency).
 *
 * If the ML Kit foregroundBitmap is directly available we use it; otherwise
 * we fall back to the confidence mask and threshold it ourselves.
 */
internal fun applyForegroundMask(
    original: Bitmap,
    result: SubjectSegmentationResult.Success
): Bitmap {
    // Preferred path: ML Kit provides a ready-made foreground bitmap
    val foregroundBitmap = result.mlKitResult.foregroundBitmap
    if (foregroundBitmap != null) {
        return foregroundBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    // Fallback: use the confidence mask to build an alpha channel
    val subjects = result.mlKitResult.subjects
    if (subjects.isNullOrEmpty()) {
        // No subject detected — return original unchanged
        return original.copy(Bitmap.Config.ARGB_8888, true)
    }

    // Combine confidence masks from all detected subjects
    val width = original.width
    val height = original.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Get pixels from original
    val pixels = IntArray(width * height)
    original.getPixels(pixels, 0, width, 0, 0, width, height)

    // For each subject, get its confidence mask and blend
    val bestConfidence = FloatArray(width * height) { 0f }

    for (subject in subjects) {
        val mask: FloatBuffer? = try { subject.confidenceMask } catch (e: Exception) { null }
        if (mask != null) {
            mask.rewind()
            for (i in 0 until minOf(mask.remaining(), bestConfidence.size)) {
                val conf = mask.get()
                if (conf > bestConfidence[i]) {
                    bestConfidence[i] = conf
                }
            }
        }
    }

    // Apply threshold to create alpha channel
    for (i in pixels.indices) {
        val confidence = bestConfidence[i]
        val alpha = if (confidence >= 0.5f) {
            // Smooth edge: map [threshold, 1.0] → [128, 255]
            ((confidence - 0.5f) / 0.5f).coerceIn(0f, 1f).let {
                (128 + (it * 127)).toInt()
            }
        } else {
            0
        }
        pixels[i] = (alpha shl 24) or (pixels[i] and 0x00FFFFFF)
    }

    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

/**
 * Scales a bitmap so its largest dimension does not exceed maxDimension.
 * Helps keep ML Kit processing fast for very high-res camera photos.
 */
internal fun scaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val maxDim = maxOf(width, height)
    if (maxDim <= maxDimension) return bitmap

    val scale = maxDimension.toFloat() / maxDim
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

/**
 * Loads a bitmap from a content URI, handling EXIF rotation.
 */
internal fun loadBitmapFromUri(
    uri: Uri,
    context: Context? = null
): Bitmap? {
    if (context == null) return null
    return try {
        val input: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(input)
        input.close()

        // Handle EXIF rotation
        val rotated = applyExifRotation(context, uri, bitmap)
        rotated
    } catch (e: Exception) {
        null
    }
}

private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
        val rotation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = android.graphics.Matrix()
        when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap
    }
}
