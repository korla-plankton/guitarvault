package com.guitarvault.app.ai

import android.graphics.Bitmap
import android.net.Uri

/**
 * Result of a background removal operation.
 */
sealed class BackgroundRemovalResult {
    data class Success(val bitmap: Bitmap) : BackgroundRemovalResult()
    data class Error(val message: String) : BackgroundRemovalResult()
}

/**
 * Interface for on-device background removal.
 * Implementations use ML Kit Subject Segmentation.
 */
interface BackgroundRemover {
    suspend fun removeBackground(
        inputUri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): BackgroundRemovalResult

    suspend fun removeBackground(
        inputBitmap: Bitmap,
        onProgress: ((String) -> Unit)? = null
    ): BackgroundRemovalResult
}
