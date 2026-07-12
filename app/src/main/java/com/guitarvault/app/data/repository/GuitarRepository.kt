package com.guitarvault.app.data.repository

import android.util.Log
import com.guitarvault.app.data.model.*
import com.guitarvault.app.data.storage.JsonStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class GuitarRepository(private val storage: JsonStorage) {

    val guitars: Flow<List<Guitar>> = storage.collection.map { it.guitars }
    val wishlist: Flow<List<WishlistItem>> = storage.collection.map { it.wishlist }

    // ── Guitar CRUD ───────────────────────────────────────────────

    suspend fun addGuitar(guitar: Guitar) {
        storage.update { it.copy(guitars = it.guitars + guitar) }
    }

    suspend fun updateGuitar(guitar: Guitar) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { if (it.id == guitar.id) guitar.copy(updatedAt = System.currentTimeMillis()) else it }
            )
        }
    }

    suspend fun deleteGuitar(guitarId: String) {
        // Also delete photo files
        val guitar = storage.collection.value.guitars.find { it.id == guitarId }
        guitar?.photos?.forEach { photo ->
            try {
                storage.getPhotoFile(photo.filePath).delete()
                photo.originalFilePath?.let { storage.getPhotoFile(it).delete() }
            } catch (e: Exception) {
                Log.w("GuitarRepository", "Failed to delete photo: ${photo.filePath}", e)
            }
        }
        storage.update { it.copy(guitars = it.guitars.filter { g -> g.id != guitarId }) }
    }

    fun getGuitarById(id: String): Guitar? {
        return storage.collection.value.guitars.find { it.id == id }
    }

    // ── Photo Management ──────────────────────────────────────────

    suspend fun addPhoto(guitarId: String, photo: GuitarPhoto) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        // If this is marked primary, unmark others
                        val photos = if (photo.isPrimary) {
                            g.photos.map { it.copy(isPrimary = false) } + photo
                        } else {
                            g.photos + photo
                        }
                        g.copy(photos = photos, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    suspend fun removePhoto(guitarId: String, photoId: String) {
        val guitar = getGuitarById(guitarId) ?: return
        val photo = guitar.photos.find { it.id == photoId } ?: return

        // Delete files
        try {
            storage.getPhotoFile(photo.filePath).delete()
            photo.originalFilePath?.let { storage.getPhotoFile(it).delete() }
        } catch (e: Exception) {
            Log.w("GuitarRepository", "Failed to delete photo file", e)
        }

        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(photos = g.photos.filter { it.id != photoId })
                    } else g
                }
            )
        }
    }

    suspend fun updatePhoto(guitarId: String, photo: GuitarPhoto) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        val photos = if (photo.isPrimary) {
                            g.photos.map { if (it.id == photo.id) photo else it.copy(isPrimary = false) }
                        } else {
                            g.photos.map { if (it.id == photo.id) photo else it }
                        }
                        g.copy(photos = photos, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    fun getPhotoFile(relativePath: String): File = storage.getPhotoFile(relativePath)

    fun createPhotoFile(prefix: String = "photo"): File = storage.createPhotoFile(prefix)

    // ── Condition ─────────────────────────────────────────────────

    suspend fun addConditionRecord(guitarId: String, record: ConditionRecord) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(conditionHistory = g.conditionHistory + record, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Maintenance ───────────────────────────────────────────────

    suspend fun addMaintenanceEntry(guitarId: String, entry: MaintenanceEntry) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(maintenanceLog = g.maintenanceLog + entry, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Valuation ─────────────────────────────────────────────────

    suspend fun updateValuation(guitarId: String, valuation: Valuation) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(valuation = valuation, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    suspend fun addValueEntry(guitarId: String, entry: ValueEntry) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        val newVal = g.valuation.copy(
                            valueHistory = g.valuation.valueHistory + entry,
                            currentValue = entry.value
                        )
                        g.copy(valuation = newVal, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Insurance ─────────────────────────────────────────────────

    suspend fun updateInsurance(guitarId: String, insurance: InsuranceInfo) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(insurance = insurance, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Custom Fields ─────────────────────────────────────────────

    suspend fun addCustomField(guitarId: String, field: CustomField) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(customFields = g.customFields + field, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    suspend fun removeCustomField(guitarId: String, fieldId: String) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(customFields = g.customFields.filter { it.id != fieldId }, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Provenance ────────────────────────────────────────────────

    suspend fun addProvenanceEntry(guitarId: String, entry: ProvenanceEntry) {
        storage.update { col ->
            col.copy(
                guitars = col.guitars.map { g ->
                    if (g.id == guitarId) {
                        g.copy(provenance = g.provenance + entry, updatedAt = System.currentTimeMillis())
                    } else g
                }
            )
        }
    }

    // ── Wishlist CRUD ─────────────────────────────────────────────

    suspend fun addWishlistItem(item: WishlistItem) {
        storage.update { it.copy(wishlist = it.wishlist + item) }
    }

    suspend fun updateWishlistItem(item: WishlistItem) {
        storage.update { col ->
            col.copy(wishlist = col.wishlist.map { if (it.id == item.id) item else it })
        }
    }

    suspend fun deleteWishlistItem(itemId: String) {
        storage.update { col ->
            col.copy(wishlist = col.wishlist.filter { it.id != itemId })
        }
    }

    /** Convert a wishlist item to an owned guitar. */
    suspend fun promoteWishlistToGuitar(itemId: String, purchasePrice: Double? = null): Guitar? {
        val item = storage.collection.value.wishlist.find { it.id == itemId } ?: return null
        val guitar = Guitar(
            brand = item.brand,
            model = item.model,
            year = item.year,
            guitarType = item.guitarType,
            valuation = Valuation(purchasePrice = purchasePrice),
            tags = item.tags,
            notes = item.notes,
            isWishlist = false
        )
        addGuitar(guitar)
        deleteWishlistItem(itemId)
        return guitar
    }

    // ── Stats ─────────────────────────────────────────────────────

    fun getCollectionStats(): CollectionStats {
        val guitars = storage.collection.value.guitars.filter { !it.isSold }
        val totalValue = guitars.sumOf { it.valuation.currentValue ?: it.valuation.purchasePrice ?: 0.0 }
        val totalInvested = guitars.sumOf { it.valuation.purchasePrice ?: 0.0 }
        val totalInsured = guitars.filter { it.insurance.insured }.sumOf { it.insurance.insuredValue ?: 0.0 }
        return CollectionStats(
            totalGuitars = guitars.size,
            totalValue = totalValue,
            totalInvested = totalInvested,
            totalInsured = totalInsured,
            wishlistCount = storage.collection.value.wishlist.size
        )
    }
}

data class CollectionStats(
    val totalGuitars: Int,
    val totalValue: Double,
    val totalInvested: Double,
    val totalInsured: Double,
    val wishlistCount: Int
)
