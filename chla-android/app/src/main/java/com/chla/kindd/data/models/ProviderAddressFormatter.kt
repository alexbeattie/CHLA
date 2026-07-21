package com.chla.kindd.data.models

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Locale

data class ProviderAddress(
    val streetLine: String?,
    val localityLine: String?
) {
    val lines: List<String>
        get() = listOfNotNull(streetLine.nonBlank(), localityLine.nonBlank())

    val singleLine: String
        get() = lines.joinToString(", ")
}

object ProviderAddressFormatter {
    private const val MAX_JSON_STRING_DEPTH = 3

    private val streetKeys = listOf("street", "street_address", "address", "address_line_1")
    private val cityKeys = listOf("city")
    private val stateKeys = listOf("state")
    private val zipKeys = listOf("zip", "zip_code")

    fun format(
        rawAddress: String?,
        city: String?,
        state: String?,
        zipCode: String?
    ): ProviderAddress {
        val raw = rawAddress.nonBlank()
        val fallbackCity = city.nonBlank()
        val fallbackState = state.nonBlank()
        val fallbackZip = zipCode.nonBlank()

        val parsed = when {
            raw == null -> ParsedAddress()
            raw.isJsonLikeAddress() -> parseJsonAddress(raw)
            else -> ParsedAddress(streetParts = listOf(raw))
        }

        val streetLine = parsed.streetParts
            .mapNotNull(String?::nonBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .joinToString(", ")
            .nonBlank()

        val localityLine = formatLocality(
            city = (parsed.city ?: fallbackCity).unlessContainedIn(streetLine),
            state = (parsed.state ?: fallbackState).unlessContainedIn(streetLine),
            zipCode = (parsed.zipCode ?: fallbackZip).unlessContainedIn(streetLine)
        )

        return ProviderAddress(streetLine = streetLine, localityLine = localityLine)
    }

    private fun parseJsonAddress(raw: String): ParsedAddress {
        val element = runCatching { JsonParser().parse(raw) }.getOrNull() ?: return ParsedAddress()
        return parseElement(element)
    }

    private fun parseElement(element: JsonElement): ParsedAddress = when {
        element.isJsonObject -> parseObject(element.asJsonObject)
        element.isJsonArray -> element.asJsonArray.fold(ParsedAddress()) { address, fragment ->
            address.merge(parseArrayFragment(fragment))
        }
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
            ParsedAddress(streetParts = listOfNotNull(decodeStringFragment(element.asString)))
        }
        else -> ParsedAddress()
    }

    private fun parseArrayFragment(element: JsonElement): ParsedAddress = when {
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
            ParsedAddress(streetParts = listOfNotNull(decodeStringFragment(element.asString)))
        }
        element.isJsonObject || element.isJsonArray -> parseElement(element)
        else -> ParsedAddress()
    }

    private fun parseObject(value: JsonObject): ParsedAddress = ParsedAddress(
        streetParts = listOfNotNull(value.stringValue(streetKeys).safeAddressFragment()),
        city = value.stringValue(cityKeys),
        state = value.stringValue(stateKeys),
        zipCode = value.stringValue(zipKeys)
    )

    private fun decodeStringFragment(value: String, depth: Int = 0): String? {
        val fragment = value.nonBlank() ?: return null
        if (!fragment.startsWith("\"")) return fragment.safeAddressFragment()
        if (depth >= MAX_JSON_STRING_DEPTH) return null

        val nested = runCatching { JsonParser().parse(fragment) }.getOrNull() ?: return null
        if (!nested.isJsonPrimitive || !nested.asJsonPrimitive.isString) return null

        return decodeStringFragment(nested.asString, depth + 1)
    }

    private fun formatLocality(city: String?, state: String?, zipCode: String?): String? {
        val cityAndState = when {
            city != null && state != null -> "$city, $state"
            city != null -> city
            else -> state
        }

        return listOfNotNull(cityAndState, zipCode).joinToString(" ").nonBlank()
    }

    private data class ParsedAddress(
        val streetParts: List<String> = emptyList(),
        val city: String? = null,
        val state: String? = null,
        val zipCode: String? = null
    ) {
        fun merge(other: ParsedAddress): ParsedAddress = ParsedAddress(
            streetParts = streetParts + other.streetParts,
            city = city ?: other.city,
            state = state ?: other.state,
            zipCode = zipCode ?: other.zipCode
        )
    }

    private fun JsonObject.stringValue(keys: List<String>): String? {
        val matchingEntry = entrySet().firstOrNull { entry ->
            keys.any { key -> entry.key.equals(key, ignoreCase = true) }
        } ?: return null

        return matchingEntry.value
            .takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            .nonBlank()
    }

    private fun String?.unlessContainedIn(text: String?): String? =
        this?.takeUnless { component -> text.containsComponent(component) }

    private fun String?.containsComponent(component: String): Boolean {
        if (this == null) return false

        val normalizedText = normalizeForComparison(this)
        val normalizedComponent = normalizeForComparison(component)
        return normalizedComponent.isNotEmpty() &&
            " $normalizedText ".contains(" $normalizedComponent ")
    }

    private fun normalizeForComparison(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String.isJsonLikeAddress(): Boolean =
    startsWith("{") || startsWith("[") || startsWith("\"") || this == "null"

private fun String?.safeAddressFragment(): String? = nonBlank()?.takeUnless { value ->
    value.isJsonLikeAddress()
}
