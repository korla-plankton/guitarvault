package com.guitarvault.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarvault.app.data.model.*
import com.guitarvault.app.data.repository.CollectionStats
import com.guitarvault.app.data.repository.GuitarRepository
import com.guitarvault.app.data.storage.JsonStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CollectionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val storage = JsonStorage.getInstance(application)
    private val repository = GuitarRepository(storage)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(CollectionViewMode.LIST)
    val viewMode: StateFlow<CollectionViewMode> = _viewMode.asStateFlow()

    private val _filterType = MutableStateFlow<GuitarType?>(null)
    val filterType: StateFlow<GuitarType?> = _filterType.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.DATE_ADDED)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val guitars: StateFlow<List<Guitar>> = combine(
        repository.guitars,
        _searchQuery,
        _filterType,
        _sortMode
    ) { allGuitars, query, typeFilter, sort ->
        allGuitars
            .filter { !it.isSold }
            .filter { guitar ->
                (typeFilter == null || guitar.guitarType == typeFilter) &&
                (query.isBlank() || guitar.displayName.contains(query, ignoreCase = true) ||
                 guitar.brand.contains(query, ignoreCase = true) ||
                 guitar.model.contains(query, ignoreCase = true) ||
                 guitar.serialNumber.contains(query, ignoreCase = true) ||
                 guitar.tags.any { it.contains(query, ignoreCase = true) })
            }
            .let { filtered ->
                when (sort) {
                    SortMode.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
                    SortMode.NAME -> filtered.sortedBy { it.displayName.lowercase() }
                    SortMode.BRAND -> filtered.sortedBy { it.brand.lowercase().ifBlank { "zzz" } }
                    SortMode.YEAR -> filtered.sortedByDescending { it.year ?: 0 }
                    SortMode.VALUE -> filtered.sortedByDescending { it.valuation.currentValue ?: it.valuation.purchasePrice ?: 0.0 }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<CollectionStats> = repository.guitars
        .map { repository.getCollectionStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionStats(0, 0.0, 0.0, 0.0, 0))

    val wishlist: StateFlow<List<WishlistItem>> = repository.wishlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Actions ───────────────────────────────────────────────────

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setViewMode(mode: CollectionViewMode) { _viewMode.value = mode }
    fun setFilterType(type: GuitarType?) { _filterType.value = type }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }

    fun addGuitar(guitar: Guitar) = viewModelScope.launch {
        repository.addGuitar(guitar)
    }

    fun updateGuitar(guitar: Guitar) = viewModelScope.launch {
        repository.updateGuitar(guitar)
    }

    fun deleteGuitar(guitarId: String) = viewModelScope.launch {
        repository.deleteGuitar(guitarId)
    }

    fun getGuitarById(id: String): Guitar? = repository.getGuitarById(id)

    fun getPhotoFile(relativePath: String): java.io.File = repository.getPhotoFile(relativePath)

    /**
     * Returns a Coil-loadable model for a GuitarPhoto.
     * - Base64 photos → decoded ByteArray (Coil supports ByteArray natively)
     * - File photos → File object
     */
    fun getPhotoModel(photo: com.guitarvault.app.data.model.GuitarPhoto?): Any? {
        if (photo == null) return null
        if (photo.base64Data.isNotEmpty()) {
            return try {
                android.util.Base64.decode(photo.base64Data, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }
        if (photo.filePath.isNotBlank()) {
            val file = getPhotoFile(photo.filePath)
            if (file.exists()) return file
        }
        return null
    }

    fun createPhotoFile(prefix: String = "photo"): java.io.File = repository.createPhotoFile(prefix)

    // ── Background Removal ────────────────────────────────────────

    private val _bgRemovalProgress = MutableStateFlow<Map<String, String>>(emptyMap())
    val bgRemovalProgress: StateFlow<Map<String, String>> = _bgRemovalProgress.asStateFlow()

    fun removeBackgroundFromPhoto(
        guitarId: String,
        photoId: String,
        onResult: (Boolean, String) -> Unit
    ) = viewModelScope.launch {
        val guitar = getGuitarById(guitarId) ?: run {
            onResult(false, "Guitar not found"); return@launch
        }
        val photo = guitar.photos.find { it.id == photoId } ?: run {
            onResult(false, "Photo not found"); return@launch
        }

        _bgRemovalProgress.value = _bgRemovalProgress.value + (photoId to "Processing...")

        try {
            // Get the current image as a bitmap
            val bitmap = getPhotoBitmap(photo) ?: run {
                _bgRemovalProgress.value = _bgRemovalProgress.value - photoId
                onResult(false, "Could not load image"); return@launch
            }

            // Run ML Kit segmentation
            val remover = com.guitarvault.app.ai.MlKitBackgroundRemover.getInstance(getApplication())
            _bgRemovalProgress.value = _bgRemovalProgress.value + (photoId to "AI processing...")

            val result = remover.removeBackground(bitmap) { progress ->
                _bgRemovalProgress.value = _bgRemovalProgress.value + (photoId to progress)
            }

            when (result) {
                is com.guitarvault.app.ai.BackgroundRemovalResult.Success -> {
                    // Convert result bitmap to base64
                    val outputStream = java.io.ByteArrayOutputStream()
                    result.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    val newBase64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

                    // Save original for undo, replace with processed
                    val updatedPhoto = if (photo.isBase64) {
                        photo.copy(
                            originalBase64Data = photo.base64Data,
                            base64Data = newBase64,
                            backgroundRemoved = true
                        )
                    } else {
                        // File-based photo — convert to base64 for the processed version
                        photo.copy(
                            originalFilePath = photo.filePath,
                            base64Data = newBase64,
                            backgroundRemoved = true
                        )
                    }
                    repository.updatePhoto(guitarId, updatedPhoto)
                    _bgRemovalProgress.value = _bgRemovalProgress.value - photoId
                    onResult(true, "Background removed")
                }
                is com.guitarvault.app.ai.BackgroundRemovalResult.Error -> {
                    _bgRemovalProgress.value = _bgRemovalProgress.value - photoId
                    onResult(false, result.message)
                }
            }
        } catch (e: Exception) {
            _bgRemovalProgress.value = _bgRemovalProgress.value - photoId
            onResult(false, e.message ?: "Unknown error")
        }
    }

    fun undoBackgroundRemoval(guitarId: String, photoId: String) = viewModelScope.launch {
        val guitar = getGuitarById(guitarId) ?: return@launch
        val photo = guitar.photos.find { it.id == photoId } ?: return@launch

        val restoredPhoto = if (photo.originalBase64Data.isNotEmpty()) {
            photo.copy(
                base64Data = photo.originalBase64Data,
                originalBase64Data = "",
                backgroundRemoved = false
            )
        } else if (photo.originalFilePath != null) {
            photo.copy(
                filePath = photo.originalFilePath!!,
                originalFilePath = null,
                base64Data = "",
                backgroundRemoved = false
            )
        } else return@launch

        repository.updatePhoto(guitarId, restoredPhoto)
    }

    /** Helper to get a Bitmap from either base64 or file photo. */
    private fun getPhotoBitmap(photo: GuitarPhoto): android.graphics.Bitmap? {
        if (photo.base64Data.isNotEmpty()) {
            return try {
                val bytes = android.util.Base64.decode(photo.base64Data, android.util.Base64.NO_WRAP)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        }
        if (photo.filePath.isNotBlank()) {
            val file = getPhotoFile(photo.filePath)
            if (file.exists()) return android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        }
        return null
    }

    fun addPhotoToGuitar(guitarId: String, photo: GuitarPhoto) = viewModelScope.launch {
        repository.addPhoto(guitarId, photo)
    }

    fun removePhotoFromGuitar(guitarId: String, photoId: String) = viewModelScope.launch {
        repository.removePhoto(guitarId, photoId)
    }

    fun setPrimaryPhoto(guitarId: String, photoId: String) = viewModelScope.launch {
        val guitar = getGuitarById(guitarId) ?: return@launch
        val photo = guitar.photos.find { it.id == photoId } ?: return@launch
        repository.updatePhoto(guitarId, photo.copy(isPrimary = true))
    }

    fun addConditionRecord(guitarId: String, record: ConditionRecord) = viewModelScope.launch {
        repository.addConditionRecord(guitarId, record)
    }

    fun addMaintenanceEntry(guitarId: String, entry: MaintenanceEntry) = viewModelScope.launch {
        repository.addMaintenanceEntry(guitarId, entry)
    }

    fun addCustomField(guitarId: String, field: CustomField) = viewModelScope.launch {
        repository.addCustomField(guitarId, field)
    }

    fun removeCustomField(guitarId: String, fieldId: String) = viewModelScope.launch {
        repository.removeCustomField(guitarId, fieldId)
    }

    fun updateValuation(guitarId: String, valuation: Valuation) = viewModelScope.launch {
        repository.updateValuation(guitarId, valuation)
    }

    fun updateInsurance(guitarId: String, insurance: InsuranceInfo) = viewModelScope.launch {
        repository.updateInsurance(guitarId, insurance)
    }

    fun markAsSold(guitarId: String, soldPrice: Double, soldDate: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val guitar = getGuitarById(guitarId) ?: return@launch
        updateGuitar(guitar.copy(
            isSold = true,
            soldPrice = soldPrice,
            soldDate = soldDate,
            valuation = guitar.valuation.copy(currentValue = soldPrice)
        ))
    }

    fun undoSold(guitarId: String) = viewModelScope.launch {
        val guitar = getGuitarById(guitarId) ?: return@launch
        updateGuitar(guitar.copy(isSold = false, soldPrice = null, soldDate = null))
    }

    fun exportCollection(targetFile: java.io.File) = viewModelScope.launch {
        storage.exportTo(targetFile)
    }

    fun importCollection(sourceFile: java.io.File) = viewModelScope.launch {
        storage.importFrom(sourceFile)
    }

    // Wishlist
    fun addWishlistItem(item: WishlistItem) = viewModelScope.launch {
        repository.addWishlistItem(item)
    }
    fun updateWishlistItem(item: WishlistItem) = viewModelScope.launch {
        repository.updateWishlistItem(item)
    }
    fun deleteWishlistItem(itemId: String) = viewModelScope.launch {
        repository.deleteWishlistItem(itemId)
    }

    companion object {
        fun factory(app: android.app.Application): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app) {}
    }

    enum class CollectionViewMode { LIST, GROUPED, GRID }
}

enum class SortMode(val displayName: String) {
    DATE_ADDED("Date Added"),
    NAME("Name"),
    BRAND("Brand"),
    YEAR("Year"),
    VALUE("Value")
}
