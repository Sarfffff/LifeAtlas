package com.xiaoyin.lifeatlas.core.map

import com.xiaoyin.lifeatlas.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class AmapReverseGeocoder {
    suspend fun reverseGeocode(point: MapPickerPoint): String? {
        val key = BuildConfig.AMAP_API_KEY.takeIf { it.isNotBlank() } ?: return null

        return runCatching {
            val encodedKey = URLEncoder.encode(key, Charsets.UTF_8.name())
            val url = URL(
                "https://restapi.amap.com/v3/geocode/regeo" +
                    "?key=$encodedKey" +
                    "&location=${point.longitude},${point.latitude}" +
                    "&extensions=base" +
                    "&output=json"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = NETWORK_TIMEOUT_MILLIS
                readTimeout = NETWORK_TIMEOUT_MILLIS
            }
            try {
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = Json.parseToJsonElement(response).jsonObject
                val status = root["status"]?.jsonPrimitive?.content
                if (status != "1") {
                    null
                } else {
                    root["regeocode"]
                        ?.jsonObject
                        ?.get("formatted_address")
                        ?.jsonPrimitive
                        ?.content
                        ?.takeIf { it.isNotBlank() }
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val NETWORK_TIMEOUT_MILLIS = 10_000
    }
}
