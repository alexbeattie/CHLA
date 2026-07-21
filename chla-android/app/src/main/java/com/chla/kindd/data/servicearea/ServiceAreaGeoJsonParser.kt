package com.chla.kindd.data.servicearea

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.Reader
import java.util.Collections
import java.util.concurrent.CancellationException

/**
 * Parses the bundled iPhone-compatible regional-center GeoJSON without Android or Maps types.
 *
 * A malformed feature or an unsupported geometry is skipped. A malformed feature collection, or
 * one with no usable service areas, returns a failure so callers can render an explicit load state.
 */
class ServiceAreaGeoJsonParser {
    fun parse(json: String): Result<List<ServiceAreaFeature>> = parse(json.reader())

    fun parse(reader: Reader): Result<List<ServiceAreaFeature>> = try {
        val collection = JsonParser().parse(reader).asJsonObject
        require(collection.requiredString("type") == "FeatureCollection") {
            "Expected a GeoJSON FeatureCollection"
        }

        val features = collection.requiredArray("features")
            .mapNotNull { it.asJsonObjectOrNull()?.let(::parseFeature) }

        require(features.isNotEmpty()) { "FeatureCollection has no usable service-area features" }
        Result.success(features.immutable())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(ServiceAreaParseException("Unable to parse service-area GeoJSON", exception))
    }

    private fun parseFeature(feature: JsonObject): ServiceAreaFeature? {
        return try {
            val properties = feature.requiredObject("properties")
            val geometry = feature.requiredObject("geometry")
            val polygons = when (geometry.requiredString("type")) {
                "Polygon" -> geometry.requiredArray("coordinates")
                    .firstOrNull()
                    ?.let(::parseRing)
                    ?.let(::listOf)
                    .orEmpty()
                "MultiPolygon" -> geometry.requiredArray("coordinates")
                    .mapNotNull { polygon ->
                        polygon.asJsonArrayOrNull()
                            ?.firstOrNull()
                            ?.let(::parseRing)
                    }
                else -> emptyList()
            }

            if (polygons.isEmpty()) return null

            ServiceAreaFeature(
                id = properties.requiredInt("OBJECTID"),
                name = properties.requiredString("REGIONALCENTER"),
                acronym = properties.requiredString("ACRONYM"),
                description = properties.requiredString("CATCHMENT_AREA_DESC"),
                polygons = polygons.map { it.immutable() }.immutable()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRing(element: JsonElement): List<ServiceAreaCoordinate>? {
        val coordinates = element.asJsonArrayOrNull()
            ?.map { point ->
                val pair = point.asJsonArrayOrNull() ?: return null
                if (pair.size() < 2 || !pair[0].isJsonPrimitive || !pair[1].isJsonPrimitive) return null

                val longitude = pair[0].asDouble
                val latitude = pair[1].asDouble
                if (!longitude.isFinite() || !latitude.isFinite() ||
                    longitude !in -180.0..180.0 || latitude !in -90.0..90.0
                ) {
                    return null
                }
                ServiceAreaCoordinate(latitude = latitude, longitude = longitude)
            }
            ?: return null

        return coordinates.takeIf { it.size >= 3 }
    }

    private fun JsonObject.requiredObject(name: String): JsonObject =
        get(name)?.asJsonObjectOrNull() ?: throw IllegalArgumentException("Missing object $name")

    private fun JsonObject.requiredArray(name: String): JsonArray =
        get(name)?.asJsonArrayOrNull() ?: throw IllegalArgumentException("Missing array $name")

    private fun JsonObject.requiredString(name: String): String =
        get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asString
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing string $name")

    private fun JsonObject.requiredInt(name: String): Int =
        get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asInt
            ?: throw IllegalArgumentException("Missing integer $name")

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        takeIf(JsonElement::isJsonObject)?.asJsonObject

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? =
        takeIf(JsonElement::isJsonArray)?.asJsonArray

    private fun <T> List<T>.immutable(): List<T> = Collections.unmodifiableList(toList())
}

class ServiceAreaParseException(message: String, cause: Throwable) : IllegalArgumentException(message, cause)
