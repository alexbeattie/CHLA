package com.chla.kindd.data.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.lang.reflect.Type

class StringListJsonAdapter : JsonDeserializer<List<String>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String>? {
        if (json == null || json.isJsonNull) return null

        val values = when {
            json.isJsonArray -> json.asJsonArray.map { element ->
                if (!element.isJsonPrimitive) {
                    throw JsonParseException("Expected string values in array")
                }
                element.asString
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> parseString(json.asString)
            else -> throw JsonParseException("Expected a string or string array")
        }

        return values
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }

    private fun parseString(value: String): List<String> {
        val trimmedValue = value.trim()
        if (trimmedValue.isEmpty()) return emptyList()

        if (trimmedValue.startsWith("[") && trimmedValue.endsWith("]")) {
            val parsed = try {
                JsonParser().parse(trimmedValue)
            } catch (_: JsonParseException) {
                null
            }

            if (parsed?.isJsonArray == true) {
                return parsed.asJsonArray.map { element ->
                    if (!element.isJsonPrimitive) {
                        throw JsonParseException("Expected string values in encoded array")
                    }
                    element.asString
                }
            }
        }

        return trimmedValue.split(",")
    }
}
