package com.guitarvault.app.data.storage

import android.content.Context
import android.util.Log
import com.guitarvault.app.data.model.CollectionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON file-backed storage for the entire guitar collection.
 * Single source of truth — persists to collection.json in app filesDir.
 * Thread-safe via Mutex; exposes reactive StateFlow.
 */
class JsonStorage private constructor(private val context: Context) {

    companion object {
        private const val TAG = "JsonStorage"
        private const val COLLECTION_FILE = "collection.json"
        private const val PHOTOS_DIR = "photos"

        @Volatile private var INSTANCE: JsonStorage? = null

        fun getInstance(context: Context): JsonStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JsonStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val mutex = Mutex()
    private val _collection = MutableStateFlow(CollectionData())
    val collection: StateFlow<CollectionData> = _collection.asStateFlow()

    private val collectionFile: File
        get() = File(context.filesDir, COLLECTION_FILE)

    val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR).also { it.mkdirs() }

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            val file = collectionFile
            if (file.exists()) {
                val text = file.readText()
                if (text.isNotBlank()) {
                    _collection.value = json.decodeFromString<CollectionData>(text)
                    Log.i(TAG, "Loaded collection: ${_collection.value.guitars.size} guitars, ${_collection.value.wishlist.size} wishlist items")
                }
            } else {
                _collection.value = CollectionData()
                saveToDisk(_collection.value)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load collection", e)
            _collection.value = CollectionData()
        }
    }

    private fun saveToDisk(data: CollectionData) {
        try {
            val text = json.encodeToString(data)
            collectionFile.writeText(text)
            Log.d(TAG, "Saved collection: ${data.guitars.size} guitars")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save collection", e)
        }
    }

    suspend fun update(transform: (CollectionData) -> CollectionData) {
        mutex.withLock {
            val current = _collection.value
            val updated = transform(current).copy(lastModified = System.currentTimeMillis())
            _collection.value = updated
            withContext(Dispatchers.IO) {
                saveToDisk(updated)
            }
        }
    }

    fun getPhotoFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun createPhotoFile(prefix: String = "photo"): File {
        val timestamp = System.currentTimeMillis()
        return File(photosDir, "${prefix}_${timestamp}.jpg")
    }

    /** Export collection to a specified file (for backup). */
    suspend fun exportTo(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString(_collection.value)
            targetFile.writeText(text)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }

    /** Import collection from a file (replaces current data). */
    suspend fun importFrom(sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = sourceFile.readText()
            val imported = json.decodeFromString<CollectionData>(text)
            update { imported }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }
}
