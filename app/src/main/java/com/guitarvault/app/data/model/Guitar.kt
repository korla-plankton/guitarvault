package com.guitarvault.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ── Photo Management ──────────────────────────────────────────────

@Serializable
data class GuitarPhoto(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String = "",           // relative path under app filesDir/photos/; empty if base64 only
    val base64Data: String = "",         // base64-encoded image data (for pasted/placeholder photos)
    val originalBase64Data: String = "", // base64 of original before background removal (for undo)
    val originalFilePath: String? = null,  // original file path before background removal; null if none
    val backgroundRemoved: Boolean = false,
    val caption: String = "",
    val isPrimary: Boolean = false,
    val photoType: PhotoType = PhotoType.GENERAL,
    val capturedAt: Long = System.currentTimeMillis()
) {
    /** Whether this photo is stored as base64 data (no file on disk). */
    val isBase64: Boolean get() = base64Data.isNotEmpty()
}

@Serializable
enum class PhotoType(val displayName: String) {
    GENERAL("General"),
    FRONT("Front"),
    BACK("Back"),
    HEADSTOCK("Headstock"),
    NECK("Neck"),
    BODY("Body"),
    PICKUPS("Pickups"),
    ELECTRONICS("Electronics"),
    HARDWARE("Hardware"),
    CASE("Case"),
    DAMAGE("Damage/Issue"),
    REPAIR("Repair Document")
}

// ── Custom Fields ─────────────────────────────────────────────────

@Serializable
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String,
    val fieldType: CustomFieldType = CustomFieldType.TEXT
)

@Serializable
enum class CustomFieldType(val displayName: String) {
    TEXT("Text"),
    NUMBER("Number"),
    DATE("Date"),
    BOOLEAN("Yes/No"),
    URL("URL")
}

// ── Valuation ─────────────────────────────────────────────────────

@Serializable
data class Valuation(
    val purchasePrice: Double? = null,
    val purchaseDate: Long? = null,
    val purchaseSource: String = "",        // store, individual, online, etc.
    val currentValue: Double? = null,
    val valueHistory: List<ValueEntry> = emptyList(),
    val estimatedValue: Double? = null
)

@Serializable
data class ValueEntry(
    val id: String = UUID.randomUUID().toString(),
    val value: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    val source: String = "",               // appraisal, market research, etc.
    val notes: String = ""
)

// ── Insurance ─────────────────────────────────────────────────────

@Serializable
data class InsuranceInfo(
    val insured: Boolean = false,
    val insuredValue: Double? = null,
    val provider: String = "",
    val policyNumber: String = "",
    val coverageType: String = "",         // declared value, replacement cost, etc.
    val deductible: Double? = null,
    val policyStart: Long? = null,
    val policyEnd: Long? = null,
    val notes: String = ""
)

// ── Condition Tracking ────────────────────────────────────────────

@Serializable
data class ConditionRecord(
    val id: String = UUID.randomUUID().toString(),
    val rating: ConditionRating,
    val recordedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val issues: List<String> = emptyList()  // e.g. "scratch on lower bout", "fret wear"
)

@Serializable
enum class ConditionRating(val displayName: String, val sortOrder: Int) {
    MINT("Mint", 5),
    NEAR_MINT("Near Mint", 4),
    EXCELLENT("Excellent", 3),
    VERY_GOOD("Very Good", 2),
    GOOD("Good", 1),
    FAIR("Fair", 0),
    POOR("Poor", -1)
}

// ── Maintenance Log ───────────────────────────────────────────────

@Serializable
data class MaintenanceEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: MaintenanceType,
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val cost: Double? = null,
    val technician: String = "",
    val notes: String = ""
)

@Serializable
enum class MaintenanceType(val displayName: String) {
    STRING_CHANGE("String Change"),
    SETUP("Setup / Adjustment"),
    REPAIR("Repair"),
    REFRET("Refret"),
    REFINISH("Refinish"),
    ELECTRONICS("Electronics Work"),
    HARDWARE("Hardware Replacement"),
    CLEANING("Cleaning / Conditioning"),
    INSPECTION("Inspection"),
    STORAGE("Storage / Climate"),
    OTHER("Other")
}

// ── Provenance / History ──────────────────────────────────────────

@Serializable
data class ProvenanceEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val eventType: String,               // "Acquired", "Sold", "Traded", "Inherited", etc.
    val description: String,
    val party: String = ""               // who was involved
)

// ── Strings ───────────────────────────────────────────────────────

@Serializable
data class StringInfo(
    val brand: String = "",
    val model: String = "",
    val gauge: String = "",              // e.g. "10-46"
    val material: String = "",           // phosphor bronze, nickel wound, etc.
    val lastChangedDate: Long? = null
)

// ── Core Guitar Model ─────────────────────────────────────────────

@Serializable
data class Guitar(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Identity
    val brand: String = "",
    val model: String = "",
    val subModel: String = "",           // e.g. "Standard", "Custom", "Deluxe"
    val year: Int? = null,
    val serialNumber: String = "",
    val countryOfOrigin: String = "",
    val productionNumber: String = "",   // limited edition number

    // Classification
    val guitarType: GuitarType = GuitarType.ELECTRIC,
    val bodyStyle: String = "",          // e.g. "Solid Body", "Hollow Body", "Dreadnought"
    val bodyShape: String = "",          // e.g. "Stratocaster", "Les Paul", "Super Strat"

    // Body
    val bodyWood: String = "",           // e.g. "Alder", "Mahogany", "Rosewood"
    val topWood: String = "",            // e.g. "Flame Maple", "Spruce"
    val backWood: String = "",
    val sidesWood: String = "",          // for acoustics
    val finish: String = "",             // e.g. "Gloss Poly", "Nitro", "Satin"
    val finishColor: String = "",        // e.g. "Sunburst", "Black", "Candy Apple Red"
    val bodyConstruction: String = "",   // solid, chambered, semi-hollow, hollow

    // Neck
    val neckWood: String = "",           // e.g. "Maple", "Mahogany"
    val fretboardWood: String = "",      // e.g. "Rosewood", "Maple", "Ebony"
    val neckProfile: String = "",        // e.g. "C", "D", "V", "Slim Taper"
    val neckConstruction: String = "",   // bolt-on, set neck, neck-through
    val scaleLength: Double? = null,     // mm e.g. 648 for Fender, 628 for Gibson
    val numberOfFrets: Int = 22,
    val fretSize: String = "",           // e.g. "Medium Jumbo", "Jumbo", "Vintage"
    val fretMaterial: String = "",       // e.g. "Nickel Silver", "Stainless Steel"
    val nutWidth: Double? = null,        // mm
    val nutMaterial: String = "",        // e.g. "Bone", "Tusq", "Plastic"
    val inlays: String = "",             // e.g. "Dot", "Block", "Trapezoid", "None"

    // Electronics
    val pickupConfiguration: String = "",// e.g. "SSS", "HH", "HSH", "P90"
    val neckPickup: String = "",
    val middlePickup: String = "",
    val bridgePickup: String = "",
    val pickupBrand: String = "",
    val electronics: String = "",        // e.g. "3-way switch, 1 vol, 2 tone"
    val controlsDescription: String = "",
    val activeElectronics: Boolean = false,
    val onboardPreamp: String = "",
    val batteryType: String = "",        // if active

    // Hardware
    val bridgeType: String = "",         // e.g. "Tune-o-matic", "Floyd Rose", "Hardtail"
    val bridgeBrand: String = "",
    val tailpieceType: String = "",
    val tuningMachines: String = "",     // brand/model
    val tuningMachineRatio: String = "", // e.g. "18:1"
    val tremoloType: String = "",        // if applicable
    val hardwareFinish: String = "",     // e.g. "Chrome", "Gold", "Nickel"
    val pickguard: String = "",          // material/color

    // Acoustic-specific
    val soundholeDiameter: Double? = null,  // mm
    val bracingPattern: String = "",
    val acousticPickup: String = "",
    val cutaway: Boolean = false,

    // Other
    val handedness: Handedness = Handedness.RIGHT,
    val numberOfStrings: Int = 6,
    val weight: Double? = null,          // kg
    val caseIncluded: Boolean = false,
    val caseType: String = "",           // hard, soft, gig bag
    val caseBrand: String = "",
    val accessories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String = "",

    // Strings
    val stringInfo: StringInfo = StringInfo(),

    // Collections
    val photos: List<GuitarPhoto> = emptyList(),
    val customFields: List<CustomField> = emptyList(),
    val valuation: Valuation = Valuation(),
    val insurance: InsuranceInfo = InsuranceInfo(),
    val conditionHistory: List<ConditionRecord> = emptyList(),
    val maintenanceLog: List<MaintenanceEntry> = emptyList(),
    val provenance: List<ProvenanceEntry> = emptyList(),

    // Status
    val isWishlist: Boolean = false,      // deprecated — use status
    val isSold: Boolean = false,          // deprecated — use status
    val status: GuitarStatus = GuitarStatus.OWNED,
    val soldDate: Long? = null,
    val soldPrice: Double? = null
) {
    val displayName: String
        get() = buildString {
            append(brand)
            if (model.isNotBlank()) append(" $model")
            if (subModel.isNotBlank()) append(" $subModel")
            if (year != null) append(" ($year)")
        }.trim().ifEmpty { "Untitled Guitar" }

    val primaryPhoto: GuitarPhoto?
        get() = photos.find { it.isPrimary } ?: photos.firstOrNull()

    val currentCondition: ConditionRecord?
        get() = conditionHistory.maxByOrNull { it.recordedAt }

    val totalInvestment: Double
        get() = valuation.purchasePrice ?: 0.0

    /**
     * Spec completeness: what fraction of the key spec fields are filled.
     * Returns 0.0 to 1.0.
     */
    val specCompleteness: Float
        get() {
            val fields = listOf(
                brand, model, year?.toString() ?: "", serialNumber, countryOfOrigin,
                bodyWood, topWood, neckWood, fretboardWood, neckProfile,
                scaleLength?.toString() ?: "", numberOfFrets.toString(), nutWidth?.toString() ?: "",
                nutMaterial, fretSize, inlays, finish, finishColor,
                pickupConfiguration, neckPickup, bridgePickup, pickupBrand,
                bridgeType, tuningMachines, hardwareFinish,
                bodyShape, bodyConstruction, weight?.toString() ?: "",
                stringInfo.brand, stringInfo.gauge
            )
            val filled = fields.count { it.isNotBlank() }
            return filled.toFloat() / fields.size
        }
}

@Serializable
enum class GuitarType(val displayName: String) {
    ELECTRIC("Electric"),
    ACOUSTIC("Acoustic"),
    CLASSICAL("Classical"),
    BASS("Bass"),
    SEMI_HOLLOW("Semi-Hollow"),
    HOLLOW_BODY("Hollow Body"),
    RESONATOR("Resonator"),
    LAP_STEEL("Lap Steel"),
    PEDAL_STEEL("Pedal Steel"),
    UKULELE("Ukulele"),
    MANDOLIN("Mandolin"),
    BANJO("Banjo"),
    OTHER("Other")
}

@Serializable
enum class Handedness(val displayName: String) {
    RIGHT("Right-Handed"),
    LEFT("Left-Handed")
}

@Serializable
enum class GuitarStatus(val displayName: String) {
    OWNED("Owned"),
    SOLD("Sold"),
    WISHLIST("Wishlist")
}

// ── Wishlist ──────────────────────────────────────────────────────

@Serializable
data class WishlistItem(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val brand: String = "",
    val model: String = "",
    val year: Int? = null,
    val guitarType: GuitarType = GuitarType.ELECTRIC,
    val targetPrice: Double? = null,
    val priority: WishlistPriority = WishlistPriority.MEDIUM,
    val notes: String = "",
    val searchUrls: List<String> = emptyList(),  // Reverb, eBay saved searches, etc.
    val maxAcceptableCondition: ConditionRating = ConditionRating.GOOD,
    val specificSpecs: String = "",     // "Must have rosewood fretboard"
    val tags: List<String> = emptyList(),
    val notified: Boolean = false       // future: price alert notifications
) {
    val displayName: String
        get() = buildString {
            append(brand)
            if (model.isNotBlank()) append(" $model")
            if (year != null) append(" ($year)")
        }.trim().ifEmpty { "Untitled Item" }
}

@Serializable
enum class WishlistPriority(val displayName: String, val sortOrder: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
    GRAIL("Grail", 3)
}

// ── Collection Root (for JSON persistence) ────────────────────────

@Serializable
data class CollectionData(
    val guitars: List<Guitar> = emptyList(),
    val wishlist: List<WishlistItem> = emptyList(),
    val version: Int = 1,
    val lastModified: Long = System.currentTimeMillis()
)
