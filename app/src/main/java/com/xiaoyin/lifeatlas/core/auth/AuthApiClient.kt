package com.xiaoyin.lifeatlas.core.auth

import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.SocketException
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

    suspend fun requestEmailCode(email: String, purpose: String): AuthApiMailResult {
        return post<AuthApiMailResult>(
            path = "/api/auth/email/code/request",
            bodyText = json.encodeToString(
                AuthApiEmailCodeRequest.serializer(),
                AuthApiEmailCodeRequest(email, purpose)
            )
        )
    }

    suspend fun register(email: String, password: String, accountName: String, code: String): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/register",
            bodyText = json.encodeToString(
                AuthApiRegisterRequest.serializer(),
                AuthApiRegisterRequest(email, password, accountName, code)
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

    suspend fun loginWithEmailCode(email: String, code: String): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/login/code",
            bodyText = json.encodeToString(
                AuthApiEmailCodeLoginRequest.serializer(),
                AuthApiEmailCodeLoginRequest(email, code)
            )
        )
    }

    suspend fun requestEmailVerification(accessToken: String): AuthApiMailResult {
        return post<AuthApiMailResult>(
            path = "/api/auth/email/verification/request",
            bodyText = "{}",
            bearerToken = accessToken
        )
    }

    suspend fun requestPasswordReset(email: String): AuthApiMailResult {
        return post<AuthApiMailResult>(
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
            }.recoverCatching { error ->
                throw error.toUserFacingNetworkError()
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

private fun Throwable.toUserFacingNetworkError(): Throwable {
    if (this is SocketException && message?.contains("reset", ignoreCase = true) == true) {
        return IllegalStateException("无法连接账号服务：连接被重置。当前域名可能未完成备案或 HTTPS 入口未配置，请先使用本地模式，或临时把后端地址切到可访问的服务器入口。")
    }
    return this
}

@Serializable
data class AuthApiSession(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String? = null,
    val email: String,
    val emailVerified: Boolean = false,
    val verificationEmailSent: Boolean? = null,
    val notice: String? = null
)

@Serializable
data class AuthApiMailResult(
    val ok: Boolean = true,
    val emailSent: Boolean = false,
    val message: String = "邮件服务暂不可用，请稍后重试"
)

@Serializable
private data class AuthApiEmailPasswordRequest(
    val email: String,
    val password: String
)

@Serializable
private data class AuthApiRegisterRequest(
    val email: String,
    val password: String,
    val accountName: String,
    val code: String
)

@Serializable
private data class AuthApiEmailCodeRequest(
    val email: String,
    val purpose: String
)

@Serializable
private data class AuthApiEmailCodeLoginRequest(
    val email: String,
    val code: String
)

@Serializable
private data class AuthApiEmailRequest(
    val email: String
)

@Serializable
private data class AuthApiError(
    val message: String
)
