package com.guitarvault.app.util

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Reads an image from the system clipboard and converts it to base64.
 * Handles both URI-clipboard (copied from browser) and bitmap-clipboard.
 */
object ClipboardImageReader {

    private const val TAG = "ClipboardImage"

    /**
     * Read an image from the clipboard. Returns base64 string, or null if no image found.
     */
    fun readImageAsBase64(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)

            // Try URI first (copied image URL from browser)
            val uri = item.uri
            if (uri != null) {
                val base64 = uriToBase64(context, uri)
                if (base64 != null) return base64
            }

            // Try text that might be a URI
            val text = item.text?.toString() ?: ""
            if (text.startsWith("content://") || text.startsWith("file://")) {
                val base64 = uriToBase64(context, Uri.parse(text))
                if (base64 != null) return base64
            }

            // Try raw bitmap
            if (item.uri == null && item.text == null) {
                // Some clipboards provide a direct bitmap
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        context.contentResolver, uri
                    )
                    if (bitmap != null) {
                        return bitmapToBase64(bitmap)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No bitmap in clip item $i", e)
                }
            }
        }

        return null
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap != null) bitmapToBase64(bitmap) else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read URI as image: $uri", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Downscale very large images to keep base64 manageable
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
