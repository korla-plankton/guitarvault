package com.guitarvault.app.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Isolated process service for ML Kit Subject Segmentation.
 *
 * Runs in a separate process (android:process=":segmentation") so that
 * if ML Kit's native GPU delegate crashes (SIGSEGV from MTE on Android 16),
 * only this process dies — the main app survives and can fall back to
 * saving the original photo.
 *
 * Must call startForeground() within 5 seconds of startForegroundService().
 */
class SegmentationIsolatedService : Service() {

    companion object {
        private const val TAG = "SegmentationService"
        private const val CHANNEL_ID = "seg_service"
        private const val NOTIFICATION_ID = 42
        const val EXTRA_INPUT_PATH = "input_path"
        const val EXTRA_OUTPUT_PATH = "output_path"
    }

    private val segmenter: SubjectSegmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground immediately
        startForeground(NOTIFICATION_ID, createNotification())

        val inputPath = intent?.getStringExtra(EXTRA_INPUT_PATH)
        val outputPath = intent?.getStringExtra(EXTRA_OUTPUT_PATH)

        if (inputPath == null || outputPath == null) {
            Log.e(TAG, "Missing input/output path")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        Thread {
            runBlocking {
                try {
                    val inputFile = File(inputPath)
                    if (!inputFile.exists()) {
                        Log.e(TAG, "Input file not found: $inputPath")
                        return@runBlocking
                    }

                    val bitmap = BitmapFactory.decodeFile(inputPath)
                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap")
                        return@runBlocking
                    }

                    val scaled = scaleIfNeeded(bitmap, 1024)
                    val image = InputImage.fromBitmap(scaled, 0)

                    val result = withTimeoutOrNull(30_000L) {
                        runSegmentation(image)
                    }

                    if (result != null) {
                        val outputFile = File(outputPath)
                        FileOutputStream(outputFile).use { out ->
                            result.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        Log.i(TAG, "Segmentation successful, saved to $outputPath")
                    } else {
                        Log.w(TAG, "Segmentation timed out or failed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Segmentation error", e)
                } finally {
                    stopSelf(startId)
                }
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        // Create notification channel (required Android 8+)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background removal processing"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("De-GAS")
            .setContentText("Processing photo with AI...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private suspend fun runSegmentation(image: InputImage): Bitmap? {
        return suspendCancellableCoroutine { cont ->
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    val foregroundBitmap = result.foregroundBitmap
                    if (foregroundBitmap != null) {
                        cont.resume(foregroundBitmap.copy(Bitmap.Config.ARGB_8888, true))
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Segmentation failed", e)
                    cont.resume(null)
                }
        }
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
