package com.xiaoyin.lifeatlas.core.map

import com.xiaoyin.lifeatlas.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class AmapReverseGeocoder {
    suspend fun reverseGeocode(point: MapPickerPoint): ReverseGeocodeResult {
        val key = BuildConfig.AMAP_API_KEY.takeIf { it.isNotBlank() } ?: return ReverseGeocodeResult.MissingKey

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
                    ReverseGeocodeResult.ServiceUnavailable
                } else {
                    val address = root["regeocode"]
                        ?.jsonObject
                        ?.get("formatted_address")
                        ?.jsonPrimitive
                        ?.content
                        ?.takeIf { it.isNotBlank() }
                    if (address == null) {
                        ReverseGeocodeResult.AddressNotFound
                    } else {
                        ReverseGeocodeResult.Success(address)
                    }
                }
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            when (error) {
                is IOException -> ReverseGeocodeResult.NetworkUnavailable
                else -> ReverseGeocodeResult.ServiceUnavailable
            }
        }
    }

    private companion object {
        const val NETWORK_TIMEOUT_MILLIS = 10_000
    }
}

sealed interface ReverseGeocodeResult {
    data class Success(val address: String) : ReverseGeocodeResult
    data object MissingKey : ReverseGeocodeResult
    data object NetworkUnavailable : ReverseGeocodeResult
    data object ServiceUnavailable : ReverseGeocodeResult
    data object AddressNotFound : ReverseGeocodeResult
}
