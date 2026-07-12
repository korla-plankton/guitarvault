package com.guitarvault.app.data.specs

import com.guitarvault.app.data.model.Guitar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Result of a spec lookup — a set of specs found from online sources.
 */
@Serializable
data class SpecLookupResult(
    val source: String,           // "Thomann", "Reverb"
    val sourceUrl: String,
    val title: String,
    val confidence: Double,        // 0.0 - 1.0
    val specs: Map<String, String> = emptyMap()
)

/**
 * Service for looking up guitar specifications from online sources.
 * Uses Thomann and Reverb search APIs/pages.
 */
class SpecLookupService {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Search for guitar specs across all configured sources.
     */
    suspend fun lookupSpecs(guitar: Guitar): List<SpecLookupResult> = withContext(Dispatchers.IO) {
        val query = buildString {
            append(guitar.brand)
            if (guitar.model.isNotBlank()) append(" ${guitar.model}")
            if (guitar.subModel.isNotBlank()) append(" ${guitar.subModel}")
        }.trim()

        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SpecLookupResult>()

        // Search Thomann
        runCatching {
            val thomannResult = searchThomann(query, guitar)
            if (thomannResult != null) results.add(thomannResult)
        }

        // Search Reverb
        runCatching {
            val reverbResult = searchReverb(query, guitar)
            if (reverbResult != null) results.add(reverbResult)
        }

        results.sortedByDescending { it.confidence }
    }

    /**
     * Search Thomann for guitar specs.
     * Thomann has a search page that we scrape for spec tables.
     */
    private fun searchThomann(query: String, guitar: Guitar): SpecLookupResult? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.thomann.de/gb/search_dir.html?sw=$encodedQuery&filter=true"

        val html = fetchUrl(searchUrl) ?: return null

        // Parse search results — find first product link
        val productLinkRegex = Regex("""href="(/[^"]*ar[^"]*\.html)"[^>]*>""")
        val productMatch = productLinkRegex.find(html) ?: return null
        val productPath = productMatch.groupValues[1]
        val productUrl = "https://www.thomann.de$productPath"

        // Fetch product page
        val productHtml = fetchUrl(productUrl) ?: return null

        // Parse spec table from product page
        val specs = mutableMapOf<String, String>()
        val specRowRegex = Regex("""<tr[^>]*>\s*<td[^>]*>([^<]+)</td>\s*<td[^>]*>([^<]+)</td>\s*</tr>""")
        specRowRegex.findAll(productHtml).forEach { match ->
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                specs[key] = value
            }
        }

        // Extract title
        val titleRegex = Regex("""<title>([^<]+)</title>""")
        val title = titleRegex.find(productHtml)?.groupValues?.get(1)?.trim() ?: query

        return SpecLookupResult(
            source = "Thomann",
            sourceUrl = productUrl,
            title = title,
            confidence = if (specs.isNotEmpty()) 0.8 else 0.3,
            specs = specs
        )
    }

    /**
     * Search Reverb for guitar specs.
     * Uses Reverb's public search API.
     */
    private fun searchReverb(query: String, guitar: Guitar): SpecLookupResult? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://reverb.com/api/listings?query=$encodedQuery&product_type=electric-guitar"

        val response = fetchUrl(searchUrl) ?: return null

        // Parse JSON response for first listing
        runCatching {
            val rootObj = parseJsonObject(response) ?: return null
            val listings = rootObj["listings"]?.asArray()
            if (listings != null && listings.isNotEmpty()) {
                val firstListing = listings[0].asObject()
                if (firstListing != null) {
                    val specs = mutableMapOf<String, String>()
                    firstListing["make"]?.let { specs["Brand"] = it.toString().trim('"') }
                    firstListing["model"]?.let { specs["Model"] = it.toString().trim('"') }
                    firstListing["finish"]?.let { specs["Finish"] = it.toString().trim('"') }
                    firstListing["year"]?.let { specs["Year"] = it.toString().trim('"') }
                    firstListing["category"]?.let { specs["Category"] = it.toString().trim('"') }

                    val title = firstListing["title"]?.toString()?.trim('"') ?: query
                    val webUrl = firstListing["web_link"]?.toString()?.trim('"')
                        ?: "https://reverb.com/marketplace?query=$encodedQuery"

                    return SpecLookupResult(
                        source = "Reverb",
                        sourceUrl = webUrl,
                        title = title,
                        confidence = if (specs.isNotEmpty()) 0.7 else 0.3,
                        specs = specs
                    )
                }
            }
        }

        return null
    }

    /**
     * Map raw spec keys from online sources to De-GAS Guitar fields.
     */
    fun mapSpecsToGuitar(specs: Map<String, String>): Map<String, String> {
        val mapping = mutableMapOf<String, String>()

        specs.forEach { (key, value) ->
            val normalizedKey = key.lowercase().trim()
            val mappedField = when {
                normalizedKey.contains("body") && normalizedKey.contains("material") -> "bodyWood"
                normalizedKey.contains("top") && normalizedKey.contains("material") -> "topWood"
                normalizedKey.contains("neck") && normalizedKey.contains("material") -> "neckWood"
                normalizedKey.contains("fretboard") || normalizedKey.contains("fingerboard") -> "fretboardWood"
                normalizedKey == "scale length" || normalizedKey.contains("scale") -> "scaleLength"
                normalizedKey.contains("fret") && normalizedKey.contains("count") -> "numberOfFrets"
                normalizedKey.contains("nut") && normalizedKey.contains("width") -> "nutWidth"
                normalizedKey.contains("nut") && normalizedKey.contains("material") -> "nutMaterial"
                normalizedKey.contains("pickup") && normalizedKey.contains("config") -> "pickupConfiguration"
                normalizedKey.contains("neck") && normalizedKey.contains("pickup") -> "neckPickup"
                normalizedKey.contains("bridge") && normalizedKey.contains("pickup") -> "bridgePickup"
                normalizedKey.contains("bridge") && !normalizedKey.contains("pickup") -> "bridgeType"
                normalizedKey.contains("tuning") || normalizedKey.contains("tuner") -> "tuningMachines"
                normalizedKey.contains("finish") -> "finishColor"
                normalizedKey.contains("weight") -> "weight"
                normalizedKey.contains("colour") || normalizedKey.contains("color") -> "finishColor"
                normalizedKey.contains("country") -> "countryOfOrigin"
                normalizedKey.contains("series") -> "subModel"
                normalizedKey.contains("body shape") -> "bodyShape"
                normalizedKey.contains("body type") -> "bodyConstruction"
                normalizedKey.contains("inlay") || normalizedKey.contains("inlays") -> "inlays"
                normalizedKey.contains("hardware") -> "hardwareFinish"
                normalizedKey.contains("string") && normalizedKey.contains("number") -> "numberOfStrings"
                else -> null
            }
            if (mappedField != null && value.isNotBlank()) {
                mapping[mappedField] = value
            }
        }

        return mapping
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.setRequestProperty("Accept", "text/html,application/json")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

// JSON helper extensions using kotlinx.serialization
private val jsonParser = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private fun parseJsonObject(text: String): kotlinx.serialization.json.JsonObject? {
    return try { jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject } catch (e: Exception) { null }
}

private fun parseJsonArray(text: String): kotlinx.serialization.json.JsonArray? {
    return try { jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray } catch (e: Exception) { null }
}

private fun kotlinx.serialization.json.JsonElement?.asObject(): kotlinx.serialization.json.JsonObject? {
    return try { this as? kotlinx.serialization.json.JsonObject } catch (e: Exception) { null }
}

private fun kotlinx.serialization.json.JsonElement?.asArray(): kotlinx.serialization.json.JsonArray? {
    return try { this as? kotlinx.serialization.json.JsonArray } catch (e: Exception) { null }
}
