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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
                    val raw = json.decodeFromString<CollectionData>(text)
                    val loaded = migrate(raw)
                    _collection.value = loaded
                    if (loaded.version > raw.version) {
                        // Persist migration changes back to disk
                        saveToDisk(loaded)
                    }
                    Log.i(TAG, "Loaded collection: ${loaded.guitars.size} guitars, ${loaded.wishlist.size} wishlist items")
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

    /**
     * Migrate legacy collection data to the current schema.
     * v1 -> v2: scale length was stored in mm; now decimal inches.
     * Values > 100 are unambiguously mm (shortest real scales are ~20" / 508mm).
     */
    private fun migrate(data: CollectionData): CollectionData {
        if (data.version >= 2) return data
        val migrated = data.copy(
            version = 2,
            guitars = data.guitars.map { g ->
                val len = g.scaleLength
                if (len != null && len > 100) {
                    g.copy(scaleLength = Math.round(len / 25.4 * 100) / 100.0)
                } else g
            }
        )
        Log.i(TAG, "Migrated collection data v${data.version} -> v2 (scale length mm -> inches)")
        return migrated
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
            importFromJson(text)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }

    /** Encode the current collection as pretty-printed JSON (for backup/export). */
    fun getCollectionJson(): String = json.encodeToString(_collection.value)

    /** Import collection from a JSON string (replaces current data). */
    suspend fun importFromJson(text: String): Boolean = try {
        val imported = migrate(json.decodeFromString<CollectionData>(text))
        update { imported }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Import failed", e)
        false
    }

    /** Result of a ZIP export: how many photos made it into the backup, by storage type. */
    data class ZipExportResult(
        val filePhotos: Int,      // camera captures etc. stored as files under photos/
        val embeddedPhotos: Int   // pasted/picked photos stored as base64 inside collection.json
    ) {
        val totalPhotos: Int get() = filePhotos + embeddedPhotos
    }

    /**
     * Export the collection WITH photos as a ZIP (for backup with images).
     * Layout: collection.json at the root + every referenced photo file under photos/.
     * Base64 photos (pasted/picked) live inside the JSON already.
     * Returns null on failure.
     */
    suspend fun exportZipTo(targetStream: OutputStream): ZipExportResult? = withContext(Dispatchers.IO) {
        try {
            val data = _collection.value

            // Collect all photo files referenced by the collection, and count embedded ones
            val photoFiles = mutableListOf<File>()
            var embeddedPhotos = 0
            data.guitars.forEach { guitar ->
                guitar.photos.forEach { photo ->
                    var countedAsFile = false
                    if (photo.filePath.isNotBlank()) {
                        val file = getPhotoFile(photo.filePath)
                        if (file.exists()) {
                            photoFiles.add(file)
                            countedAsFile = true
                        } else {
                            Log.w(TAG, "Photo file missing on disk, image will be lost: ${photo.filePath}")
                        }
                    }
                    photo.originalFilePath?.takeIf { it.isNotBlank() && it != photo.filePath }?.let {
                        val orig = getPhotoFile(it)
                        if (orig.exists()) photoFiles.add(orig)
                    }
                    if (!countedAsFile && photo.base64Data.isNotBlank()) embeddedPhotos++
                }
            }

            ZipOutputStream(BufferedOutputStream(targetStream)).use { zip ->
                // 1. Collection JSON (also carries all base64-embedded photos)
                zip.putNextEntry(ZipEntry(COLLECTION_FILE))
                zip.write(json.encodeToString(data).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. Photo files (relative paths, e.g. photos/photo_123.jpg)
                var filePhotos = 0
                photoFiles.distinctBy { it.path }.forEach { file ->
                    val entryName = file.relativeTo(context.filesDir).path
                    zip.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    filePhotos++
                }
                Log.i(TAG, "Exported ZIP: ${data.guitars.size} guitars, $filePhotos photo files, $embeddedPhotos embedded")
                ZipExportResult(filePhotos = filePhotos, embeddedPhotos = embeddedPhotos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZIP export failed", e)
            null
        }
    }

    /**
     * Import a backup from a stream: ZIP (collection.json + photos/) or a
     * plain JSON export from older versions. Replaces current data.
     */
    suspend fun importBackupFrom(sourceStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val buffered = BufferedInputStream(sourceStream)
            // Sniff the ZIP magic number ("PK") to stay compatible with old JSON-only exports
            buffered.mark(4)
            val header = ByteArray(2)
            val headerLen = buffered.read(header)
            buffered.reset()

            if (headerLen == 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                importZipStream(buffered)
            } else {
                importFromJson(buffered.bufferedReader().readText())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup import failed", e)
            false
        }
    }

    /** Unpack a backup ZIP: restore photo files, then load collection.json. */
    private suspend fun importZipStream(stream: InputStream): Boolean {
        var collectionJson: String? = null
        var photosRestored = 0

        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    when {
                        entry.name == COLLECTION_FILE ->
                            collectionJson = zip.readBytes().toString(Charsets.UTF_8)

                        entry.name.startsWith("$PHOTOS_DIR/") -> {
                            val target = getPhotoFile(entry.name)
                            // Zip-slip guard: only allow paths that stay inside filesDir
                            if (target.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator)) {
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                                photosRestored++
                            } else {
                                Log.w(TAG, "Skipping unsafe zip entry: ${entry.name}")
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val text = collectionJson ?: run {
            Log.e(TAG, "ZIP backup missing $COLLECTION_FILE")
            return false
        }
        Log.i(TAG, "Imported ZIP: restored $photosRestored photo files")
        return importFromJson(text)
    }
}
