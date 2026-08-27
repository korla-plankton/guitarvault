package com.guitarvault.app.data.merge

import com.guitarvault.app.data.model.CollectionData
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.data.model.InsuranceInfo
import com.guitarvault.app.data.model.StringInfo
import com.guitarvault.app.data.model.Valuation
import com.guitarvault.app.data.model.WishlistItem

/**
 * Non-destructive merge of an imported backup into the local collection.
 *
 * Identity matching (in order):
 *   1. UUID — same record lineage (re-imports, backups from the same device)
 *   2. Serial number + status — case-insensitive, trimmed; only when both
 *      sides have a non-blank serial. Deliberately NO fuzzy matching for
 *      serial-less guitars: a visible duplicate is safer than silently
 *      blending two different guitars' photos and histories.
 *   3. No match → the imported record is added as new (keeping its UUID,
 *      so future re-imports match at tier 1).
 *
 * Field policy for matched guitars:
 *   - Scalar specs: local value wins; imported fills blanks only
 *   - Collections (photos, history, maintenance, custom fields, provenance,
 *     value history): union by item id — idempotent
 *   - Booleans/enums with meaningful defaults (activeElectronics, cutaway,
 *     guitarType, numberOfFrets...): local wins — defaults are
 *     indistinguishable from explicit values
 *   - Status: local wins — a merge never revives a sold guitar
 */
object CollectionMerger {

    data class MergeStats(
        val guitarsAdded: Int = 0,
        val guitarsUpdated: Int = 0,
        val guitarsUnchanged: Int = 0,
        val wishlistAdded: Int = 0,
        val wishlistUpdated: Int = 0,
        val wishlistUnchanged: Int = 0
    )

    /** Outcome of a backup import. */
    sealed class BackupImportResult {
        object Replace : BackupImportResult()
        data class Merged(val stats: MergeStats) : BackupImportResult()
    }

    data class MergeResult(
        val data: CollectionData,
        val stats: MergeStats
    )

    fun merge(local: CollectionData, imported: CollectionData): MergeResult {
        var added = 0; var updated = 0; var unchanged = 0

        val localById = local.guitars.associateBy { it.id }
        // Serial index: (status, normalized serial) -> first local guitar with that serial
        val localBySerial = local.guitars
            .filter { it.serialNumber.isNotBlank() }
            .associateBy { normalizeSerial(effectiveStatusName(it) + "|" + it.serialNumber) }

        val mergedGuitars = local.guitars.toMutableList()

        for (imp in imported.guitars) {
            val match = localById[imp.id]
                ?: localBySerial[normalizeSerial(effectiveStatusName(imp) + "|" + imp.serialNumber)]

            if (match == null) {
                mergedGuitars.add(imp)
                added++
            } else {
                val merged = mergeGuitar(match, imp)
                val idx = mergedGuitars.indexOfFirst { it.id == match.id }
                mergedGuitars[idx] = merged
                if (merged != match) updated++ else unchanged++
            }
        }

        // Wishlist: UUID matching only (items have no serials)
        var wAdded = 0; var wUpdated = 0; var wUnchanged = 0
        val localWishlistById = local.wishlist.associateBy { it.id }
        val mergedWishlist = local.wishlist.toMutableList()

        for (imp in imported.wishlist) {
            val match = localWishlistById[imp.id]
            if (match == null) {
                mergedWishlist.add(imp)
                wAdded++
            } else {
                val merged = mergeWishlistItem(match, imp)
                val idx = mergedWishlist.indexOfFirst { it.id == match.id }
                mergedWishlist[idx] = merged
                if (merged != match) wUpdated++ else wUnchanged++
            }
        }

        return MergeResult(
            data = local.copy(
                guitars = mergedGuitars,
                wishlist = mergedWishlist
            ),
            stats = MergeStats(
                guitarsAdded = added, guitarsUpdated = updated, guitarsUnchanged = unchanged,
                wishlistAdded = wAdded, wishlistUpdated = wUpdated, wishlistUnchanged = wUnchanged
            )
        )
    }

    /** Backward compat: old records carry isSold/isWishlist booleans. */
    private fun effectiveStatusName(g: Guitar): String = when {
        g.isSold -> "SOLD"
        g.isWishlist -> "WISHLIST"
        else -> g.status.name
    }

    private fun normalizeSerial(s: String): String = s.trim().lowercase()

    // ── Guitar field merge ────────────────────────────────────────

    private fun mergeGuitar(local: Guitar, imported: Guitar): Guitar {
        return local.copy(
            updatedAt = maxOf(local.updatedAt, imported.updatedAt),
            createdAt = minOf(local.createdAt, imported.createdAt),

            // Identity & classification — fill blanks
            brand = local.brand.ifBlank { imported.brand },
            model = local.model.ifBlank { imported.model },
            subModel = local.subModel.ifBlank { imported.subModel },
            year = local.year ?: imported.year,
            serialNumber = local.serialNumber.ifBlank { imported.serialNumber },
            countryOfOrigin = local.countryOfOrigin.ifBlank { imported.countryOfOrigin },
            productionNumber = local.productionNumber.ifBlank { imported.productionNumber },
            bodyStyle = local.bodyStyle.ifBlank { imported.bodyStyle },
            bodyShape = local.bodyShape.ifBlank { imported.bodyShape },

            // Body — fill blanks (guitarType/bodyConstruction enums: local wins)
            bodyWood = local.bodyWood.ifBlank { imported.bodyWood },
            topWood = local.topWood.ifBlank { imported.topWood },
            backWood = local.backWood.ifBlank { imported.backWood },
            sidesWood = local.sidesWood.ifBlank { imported.sidesWood },
            finish = local.finish.ifBlank { imported.finish },
            finishColor = local.finishColor.ifBlank { imported.finishColor },

            // Neck — fill blanks (numberOfFrets has default 22: local wins)
            neckWood = local.neckWood.ifBlank { imported.neckWood },
            fretboardWood = local.fretboardWood.ifBlank { imported.fretboardWood },
            neckProfile = local.neckProfile.ifBlank { imported.neckProfile },
            neckConstruction = local.neckConstruction.ifBlank { imported.neckConstruction },
            scaleLength = local.scaleLength ?: imported.scaleLength,
            fretSize = local.fretSize.ifBlank { imported.fretSize },
            fretMaterial = local.fretMaterial.ifBlank { imported.fretMaterial },
            nutWidth = local.nutWidth ?: imported.nutWidth,
            nutMaterial = local.nutMaterial.ifBlank { imported.nutMaterial },
            inlays = local.inlays.ifBlank { imported.inlays },

            // Electronics — fill blanks (activeElectronics: local wins)
            pickupConfiguration = local.pickupConfiguration.ifBlank { imported.pickupConfiguration },
            neckPickup = local.neckPickup.ifBlank { imported.neckPickup },
            middlePickup = local.middlePickup.ifBlank { imported.middlePickup },
            bridgePickup = local.bridgePickup.ifBlank { imported.bridgePickup },
            pickupBrand = local.pickupBrand.ifBlank { imported.pickupBrand },
            electronics = local.electronics.ifBlank { imported.electronics },
            controlsDescription = local.controlsDescription.ifBlank { imported.controlsDescription },
            onboardPreamp = local.onboardPreamp.ifBlank { imported.onboardPreamp },
            batteryType = local.batteryType.ifBlank { imported.batteryType },

            // Hardware — fill blanks
            bridgeType = local.bridgeType.ifBlank { imported.bridgeType },
            bridgeBrand = local.bridgeBrand.ifBlank { imported.bridgeBrand },
            tailpieceType = local.tailpieceType.ifBlank { imported.tailpieceType },
            tuningMachines = local.tuningMachines.ifBlank { imported.tuningMachines },
            tuningMachineRatio = local.tuningMachineRatio.ifBlank { imported.tuningMachineRatio },
            tremoloType = local.tremoloType.ifBlank { imported.tremoloType },
            hardwareFinish = local.hardwareFinish.ifBlank { imported.hardwareFinish },
            pickguard = local.pickguard.ifBlank { imported.pickguard },

            // Acoustic — fill blanks (cutaway: local wins)
            soundholeDiameter = local.soundholeDiameter ?: imported.soundholeDiameter,
            bracingPattern = local.bracingPattern.ifBlank { imported.bracingPattern },
            acousticPickup = local.acousticPickup.ifBlank { imported.acousticPickup },

            // Other — fill blanks / union
            weight = local.weight ?: imported.weight,
            caseType = local.caseType.ifBlank { imported.caseType },
            caseBrand = local.caseBrand.ifBlank { imported.caseBrand },
            accessories = union(local.accessories, imported.accessories),
            tags = union(local.tags, imported.tags),
            notes = local.notes.ifBlank { imported.notes },

            stringInfo = mergeStringInfo(local.stringInfo, imported.stringInfo),
            valuation = mergeValuation(local.valuation, imported.valuation),
            insurance = mergeInsurance(local.insurance, imported.insurance),

            // Collections — union by id (idempotent)
            photos = unionById(local.photos, imported.photos) { it.id },
            customFields = unionById(local.customFields, imported.customFields) { it.id },
            conditionHistory = unionById(local.conditionHistory, imported.conditionHistory) { it.id },
            maintenanceLog = unionById(local.maintenanceLog, imported.maintenanceLog) { it.id },
            provenance = unionById(local.provenance, imported.provenance) { it.id },

            // Status — local wins; a merge never resurrects a sold guitar.
            // Sold details fill blanks only.
            soldDate = local.soldDate ?: imported.soldDate,
            soldPrice = local.soldPrice ?: imported.soldPrice
        )
    }

    private fun mergeStringInfo(local: StringInfo, imported: StringInfo): StringInfo {
        return local.copy(
            brand = local.brand.ifBlank { imported.brand },
            model = local.model.ifBlank { imported.model },
            gauge = local.gauge.ifBlank { imported.gauge },
            material = local.material.ifBlank { imported.material },
            lastChangedDate = local.lastChangedDate ?: imported.lastChangedDate
        )
    }

    private fun mergeValuation(local: Valuation, imported: Valuation): Valuation {
        return local.copy(
            purchasePrice = local.purchasePrice ?: imported.purchasePrice,
            purchaseDate = local.purchaseDate ?: imported.purchaseDate,
            purchaseSource = local.purchaseSource.ifBlank { imported.purchaseSource },
            currentValue = local.currentValue ?: imported.currentValue,
            estimatedValue = local.estimatedValue ?: imported.estimatedValue,
            valueHistory = unionById(local.valueHistory, imported.valueHistory) { it.id }
        )
    }

    private fun mergeInsurance(local: InsuranceInfo, imported: InsuranceInfo): InsuranceInfo {
        // Favor keeping insurance info: if either side says insured, it's insured.
        return local.copy(
            insured = local.insured || imported.insured,
            insuredValue = local.insuredValue ?: imported.insuredValue,
            provider = local.provider.ifBlank { imported.provider },
            policyNumber = local.policyNumber.ifBlank { imported.policyNumber },
            coverageType = local.coverageType.ifBlank { imported.coverageType },
            deductible = local.deductible ?: imported.deductible,
            policyStart = local.policyStart ?: imported.policyStart,
            policyEnd = local.policyEnd ?: imported.policyEnd,
            notes = local.notes.ifBlank { imported.notes }
        )
    }

    private fun mergeWishlistItem(local: WishlistItem, imported: WishlistItem): WishlistItem {
        return local.copy(
            createdAt = minOf(local.createdAt, imported.createdAt),
            brand = local.brand.ifBlank { imported.brand },
            model = local.model.ifBlank { imported.model },
            year = local.year ?: imported.year,
            targetPrice = local.targetPrice ?: imported.targetPrice,
            notes = local.notes.ifBlank { imported.notes },
            searchUrls = union(local.searchUrls, imported.searchUrls),
            specificSpecs = local.specificSpecs.ifBlank { imported.specificSpecs },
            tags = union(local.tags, imported.tags)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** Ordered union preserving local order first. */
    private fun union(local: List<String>, imported: List<String>): List<String> =
        local + imported.filter { it !in local }

    /** Union by key, local entries win on key collision. */
    private fun <T> unionById(local: List<T>, imported: List<T>, key: (T) -> String): List<T> {
        val localKeys = local.mapTo(mutableSetOf(), key)
        return local + imported.filter { key(it) !in localKeys }
    }
}
