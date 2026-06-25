package com.xiaoyin.lifeatlas.core.auth

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AuthApiClient(
    private val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun register(email: String, password: String): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/register",
            bodyText = json.encodeToString(
                AuthApiEmailPasswordRequest.serializer(),
                AuthApiEmailPasswordRequest(email, password)
            )
        )
    }

    suspend fun login(email: String, password: String): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/login",
            bodyText = json.encodeToString(
                AuthApiEmailPasswordRequest.serializer(),
                AuthApiEmailPasswordRequest(email, password)
            )
        )
    }

    suspend fun requestEmailVerification(accessToken: String) {
        post<Unit>(
            path = "/api/auth/email/verification/request",
            bodyText = "{}",
            bearerToken = accessToken
        )
    }

    suspend fun requestPasswordReset(email: String) {
        post<Unit>(
            path = "/api/auth/password/reset/request",
            bodyText = json.encodeToString(
                AuthApiEmailRequest.serializer(),
                AuthApiEmailRequest(email)
            )
        )
    }

    private suspend inline fun <reified T> post(
        path: String,
        bodyText: String,
        bearerToken: String? = null
    ): T {
        return withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = REQUEST_TIMEOUT_MS
                readTimeout = REQUEST_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (!bearerToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
            }

            runCatching {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(bodyText)
                }
                val responseCode = connection.responseCode
                val responseText = connection.readResponseText(responseCode)
                if (responseCode !in 200..299) {
                    val apiError = runCatching {
                        json.decodeFromString(AuthApiError.serializer(), responseText)
                    }.getOrNull()
                    error(apiError?.message ?: "服务器请求失败：HTTP $responseCode")
                }
                if (T::class == Unit::class) {
                    Unit as T
                } else {
                    json.decodeFromString(responseText)
                }
            }.also {
                connection.disconnect()
            }.getOrThrow()
        }
    }

    private fun HttpURLConnection.readResponseText(responseCode: Int): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 15_000
    }
}

@Serializable
data class AuthApiSession(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String? = null,
    val email: String,
    val emailVerified: Boolean = false
)

@Serializable
private data class AuthApiEmailPasswordRequest(
    val email: String,
    val password: String
)

@Serializable
private data class AuthApiEmailRequest(
    val email: String
)

@Serializable
private data class AuthApiError(
    val message: String
)
