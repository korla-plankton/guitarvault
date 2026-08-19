package com.guitarvault.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Reads an image from a content URI (photo picker) and converts to base64.
 */
object GalleryImageReader {

    private const val TAG = "GalleryImageReader"

    /**
     * Read an image from a content URI and return as base64 string.
     * Downscaling large images to keep storage manageable.
     * Applies EXIF orientation so portrait JPEGs aren't stored sideways.
     */
    fun readImageAsBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap != null) bitmapToBase64(applyExifRotation(context, uri, bitmap)) else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read image from URI: $uri", e)
            null
        }
    }

    /** Rotate a decoded bitmap according to the EXIF orientation tag of the source URI. */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            input.close()
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

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        val scaled = scaleIfNeeded(bitmap, maxDimension = 1024)
        scaled.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun scaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxDim
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }
}
