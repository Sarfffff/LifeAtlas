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

    suspend fun refreshSession(refreshToken: String): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/token/refresh",
            bodyText = json.encodeToString(
                AuthApiRefreshRequest.serializer(),
                AuthApiRefreshRequest(refreshToken)
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

    suspend fun confirmPasswordReset(email: String, code: String, password: String): AuthApiMailResult {
        return post<AuthApiMailResult>(
            path = "/api/auth/password/reset/confirm",
            bodyText = json.encodeToString(
                AuthApiPasswordResetConfirmRequest.serializer(),
                AuthApiPasswordResetConfirmRequest(email, code, password)
            )
        )
    }

    suspend fun requestEmailChangeCode(accessToken: String, newEmail: String): AuthApiMailResult {
        return post<AuthApiMailResult>(
            path = "/api/auth/email/change/code/request",
            bodyText = json.encodeToString(
                AuthApiEmailChangeRequest.serializer(),
                AuthApiEmailChangeRequest(newEmail)
            ),
            bearerToken = accessToken
        )
    }

    suspend fun confirmEmailChange(
        accessToken: String,
        newEmail: String,
        code: String,
        password: String
    ): AuthApiSession {
        return post<AuthApiSession>(
            path = "/api/auth/email/change/confirm",
            bodyText = json.encodeToString(
                AuthApiEmailChangeConfirmRequest.serializer(),
                AuthApiEmailChangeConfirmRequest(newEmail, code, password)
            ),
            bearerToken = accessToken
        )
    }

    suspend fun deleteAccount(accessToken: String, password: String): AuthApiActionResult {
        return post<AuthApiActionResult>(
            path = "/api/auth/account/delete",
            bodyText = json.encodeToString(
                AuthApiPasswordRequest.serializer(),
                AuthApiPasswordRequest(password)
            ),
            bearerToken = accessToken
        )
    }

    suspend fun uploadCloudBackup(accessToken: String, data: String): AuthApiCloudBackupResult {
        return post<AuthApiCloudBackupResult>(
            path = "/api/sync/export/upload",
            bodyText = json.encodeToString(
                AuthApiCloudBackupUploadRequest.serializer(),
                AuthApiCloudBackupUploadRequest(data)
            ),
            bearerToken = accessToken
        )
    }

    suspend fun downloadCloudBackup(accessToken: String): AuthApiCloudBackupPayload {
        return post<AuthApiCloudBackupPayload>(
            path = "/api/sync/export/download",
            bodyText = "{}",
            bearerToken = accessToken
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
                    throw AuthApiException(
                        statusCode = responseCode,
                        message = apiError?.message ?: "服务器请求失败：HTTP $responseCode"
                    )
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
    if (this is AuthApiException) return this
    if (this is SocketException && message?.contains("reset", ignoreCase = true) == true) {
        return IllegalStateException("无法连接账号服务：连接被重置。请检查 HTTPS 证书、Nginx 反向代理、服务器防火墙和账号服务是否正常运行。")
    }
    return this
}

class AuthApiException(
    val statusCode: Int,
    override val message: String
) : IllegalStateException(message)

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
    val message: String = "邮件服务暂不可用，请稍后再试"
)
@Serializable
data class AuthApiCloudBackupResult(
    val ok: Boolean = true,
    val updatedAt: Long? = null,
    val size: Long = 0L,
    val message: String = "云端轻量备份已保存"
)

@Serializable
data class AuthApiActionResult(
    val ok: Boolean = true,
    val message: String
)

@Serializable
data class AuthApiCloudBackupPayload(
    val ok: Boolean = true,
    val exists: Boolean = false,
    val data: String? = null,
    val updatedAt: Long? = null,
    val size: Long = 0L,
    val message: String = "暂无云端备份"
)

@Serializable
private data class AuthApiCloudBackupUploadRequest(
    val data: String
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
private data class AuthApiRefreshRequest(
    val refreshToken: String
)

@Serializable
private data class AuthApiEmailRequest(
    val email: String
)

@Serializable
private data class AuthApiPasswordResetConfirmRequest(
    val email: String,
    val code: String,
    val password: String
)

@Serializable
private data class AuthApiEmailChangeRequest(
    val newEmail: String
)

@Serializable
private data class AuthApiEmailChangeConfirmRequest(
    val newEmail: String,
    val code: String,
    val password: String
)

@Serializable
private data class AuthApiPasswordRequest(
    val password: String
)

@Serializable
private data class AuthApiError(
    val message: String
)

