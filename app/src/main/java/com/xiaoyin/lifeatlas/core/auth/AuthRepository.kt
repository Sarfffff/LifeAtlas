package com.xiaoyin.lifeatlas.core.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "lifeatlas_auth")

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val lastLoginAt: Long? = null,
    val skippedLogin: Boolean = false
)

class AuthRepository(context: Context) {
    private val dataStore = context.applicationContext.authDataStore

    val session: Flow<AuthSession> = dataStore.data.map { preferences ->
        AuthSession(
            isLoggedIn = preferences[IS_LOGGED_IN] ?: false,
            email = preferences[EMAIL],
            emailVerified = preferences[EMAIL_VERIFIED] ?: false,
            lastLoginAt = preferences[LAST_LOGIN_AT],
            skippedLogin = preferences[SKIPPED_LOGIN] ?: false
        )
    }

    suspend fun register(email: String, password: String, confirmPassword: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        validatePassword(password)
        require(password == confirmPassword) { "两次输入的密码不一致" }

        val salt = createSalt()
        dataStore.edit { preferences ->
            preferences[EMAIL] = normalizedEmail
            preferences[PASSWORD_SALT] = salt
            preferences[PASSWORD_HASH] = hashPassword(password, salt)
            preferences[EMAIL_VERIFIED] = false
            preferences[IS_LOGGED_IN] = true
            preferences[SKIPPED_LOGIN] = false
            preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
        }
    }

    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        val snapshot = dataStore.data.first()
        val storedEmail = snapshot[EMAIL] ?: error("当前还没有注册账号")
        val salt = snapshot[PASSWORD_SALT] ?: error("账号信息不完整，请重新注册")
        val storedHash = snapshot[PASSWORD_HASH] ?: error("账号信息不完整，请重新注册")
        require(storedEmail == normalizedEmail) { "邮箱与本机已注册账号不一致" }
        require(hashPassword(password, salt) == storedHash) { "密码错误" }

        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[SKIPPED_LOGIN] = false
            preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
        }
    }

    suspend fun markEmailVerifiedForLocalPreview() {
        dataStore.edit { preferences ->
            require(!preferences[EMAIL].isNullOrBlank()) { "请先注册或登录账号" }
            preferences[EMAIL_VERIFIED] = true
        }
    }

    suspend fun skipLogin() {
        dataStore.edit { preferences ->
            preferences[SKIPPED_LOGIN] = true
            preferences[IS_LOGGED_IN] = false
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
    }

    suspend fun clearLocalAccount() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun validateEmail(email: String) {
        require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            "请输入有效邮箱"
        }
    }

    private fun validatePassword(password: String) {
        require(password.length >= 8) { "密码至少需要 8 位" }
        require(password.any { it.isLetter() } && password.any { it.isDigit() }) {
            "密码需要同时包含字母和数字"
        }
    }

    private fun String.normalizeEmail(): String = trim().lowercase()

    private fun createSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$salt:$password".toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        val EMAIL = stringPreferencesKey("email")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val EMAIL_VERIFIED = booleanPreferencesKey("email_verified")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val SKIPPED_LOGIN = booleanPreferencesKey("skipped_login")
        val LAST_LOGIN_AT = longPreferencesKey("last_login_at")
    }
}
