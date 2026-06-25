package com.xiaoyin.lifeatlas.core.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private val Context.authDataStore by preferencesDataStore(name = "lifeatlas_auth")

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val lastLoginAt: Long? = null,
    val skippedLogin: Boolean = false,
    val failedLoginCount: Int = 0,
    val loginLockedUntil: Long? = null
)

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.authDataStore

    val session: Flow<AuthSession> = dataStore.data.map { preferences ->
        val firebaseUser = currentFirebaseUser()
        AuthSession(
            isLoggedIn = firebaseUser != null || (preferences[IS_LOGGED_IN] ?: false),
            email = firebaseUser?.email ?: preferences[EMAIL],
            emailVerified = firebaseUser?.isEmailVerified ?: (preferences[EMAIL_VERIFIED] ?: false),
            lastLoginAt = preferences[LAST_LOGIN_AT],
            skippedLogin = preferences[SKIPPED_LOGIN] ?: false,
            failedLoginCount = preferences[FAILED_LOGIN_COUNT]?.toInt() ?: 0,
            loginLockedUntil = preferences[LOGIN_LOCKED_UNTIL]
        )
    }

    suspend fun register(email: String, password: String, confirmPassword: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        validatePassword(password)
        require(password == confirmPassword) { "两次输入的密码不一致" }

        if (isFirebaseConfigured()) {
            registerWithFirebase(normalizedEmail, password)
            return
        }

        val snapshot = dataStore.data.first()
        enforceRegisterRateLimit(snapshot[REGISTER_WINDOW_START], snapshot[REGISTER_COUNT])
        require(snapshot[EMAIL].isNullOrBlank()) { "本机已存在账号，请先登录或在设置中清除本地账号" }

        val salt = createSalt()
        dataStore.edit { preferences ->
            recordRegisterAttempt(preferences)
            preferences[EMAIL] = normalizedEmail
            preferences[PASSWORD_SALT] = salt
            preferences[PASSWORD_HASH] = hashPassword(password, salt)
            preferences[EMAIL_VERIFIED] = false
            preferences[IS_LOGGED_IN] = true
            preferences[SKIPPED_LOGIN] = false
            preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
            preferences[FAILED_LOGIN_COUNT] = 0L
            preferences.remove(LOGIN_LOCKED_UNTIL)
            preferences.remove(LAST_FAILED_LOGIN_AT)
        }
    }

    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)

        if (isFirebaseConfigured()) {
            loginWithFirebase(normalizedEmail, password)
            return
        }

        val snapshot = dataStore.data.first()
        enforceLoginLock(snapshot[LOGIN_LOCKED_UNTIL])
        val storedEmail = snapshot[EMAIL] ?: error("当前还没有注册账号")
        val salt = snapshot[PASSWORD_SALT] ?: error("账号信息不完整，请重新注册")
        val storedHash = snapshot[PASSWORD_HASH] ?: error("账号信息不完整，请重新注册")
        if (storedEmail != normalizedEmail || hashPassword(password, salt) != storedHash) {
            recordFailedLogin()
            error("邮箱或密码错误")
        }

        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[SKIPPED_LOGIN] = false
            preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
            preferences[FAILED_LOGIN_COUNT] = 0L
            preferences.remove(LOGIN_LOCKED_UNTIL)
            preferences.remove(LAST_FAILED_LOGIN_AT)
        }
    }

    suspend fun markEmailVerifiedForLocalPreview() {
        if (isFirebaseConfigured()) {
            refreshFirebaseEmailVerification()
            return
        }
        dataStore.edit { preferences ->
            require(!preferences[EMAIL].isNullOrBlank()) { "请先注册或登录账号" }
            preferences[EMAIL_VERIFIED] = true
        }
    }

    suspend fun sendEmailVerification() {
        val auth = firebaseAuthOrError()
        val user = auth.currentUser ?: error("请先登录账号")
        require(!user.isEmailVerified) { "邮箱已经验证" }
        user.sendEmailVerification().await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        val auth = firebaseAuthOrError()
        auth.sendPasswordResetEmail(normalizedEmail).await()
    }

    suspend fun skipLogin() {
        dataStore.edit { preferences ->
            preferences[SKIPPED_LOGIN] = true
            preferences[IS_LOGGED_IN] = false
        }
    }

    suspend fun logout() {
        if (isFirebaseConfigured()) {
            FirebaseAuth.getInstance().signOut()
        }
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
        }
    }

    suspend fun clearLocalAccount() {
        if (isFirebaseConfigured()) {
            FirebaseAuth.getInstance().signOut()
        }
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

    private fun enforceLoginLock(lockedUntil: Long?) {
        val now = System.currentTimeMillis()
        if (lockedUntil != null && lockedUntil > now) {
            val minutes = ((lockedUntil - now) / 60_000L).coerceAtLeast(1L)
            error("登录尝试过于频繁，请约 $minutes 分钟后再试")
        }
    }

    private fun enforceRegisterRateLimit(windowStart: Long?, count: Long?) {
        val now = System.currentTimeMillis()
        val isSameWindow = windowStart != null && now - windowStart < REGISTER_WINDOW_MS
        if (isSameWindow && (count ?: 0L) >= MAX_REGISTER_ATTEMPTS_PER_WINDOW) {
            error("注册操作过于频繁，请稍后再试")
        }
    }

    private suspend fun recordFailedLogin() {
        dataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            val previousFailedAt = preferences[LAST_FAILED_LOGIN_AT] ?: 0L
            val previousCount = preferences[FAILED_LOGIN_COUNT] ?: 0L
            val count = if (now - previousFailedAt > LOGIN_FAILURE_WINDOW_MS) 1L else previousCount + 1L
            preferences[FAILED_LOGIN_COUNT] = count
            preferences[LAST_FAILED_LOGIN_AT] = now
            if (count >= MAX_LOGIN_FAILURES) {
                preferences[LOGIN_LOCKED_UNTIL] = now + LOGIN_LOCK_MS
            }
        }
    }

    private fun recordRegisterAttempt(preferences: MutablePreferences) {
        val now = System.currentTimeMillis()
        val windowStart = preferences[REGISTER_WINDOW_START] ?: now
        val isSameWindow = now - windowStart < REGISTER_WINDOW_MS
        if (isSameWindow) {
            preferences[REGISTER_COUNT] = (preferences[REGISTER_COUNT] ?: 0L) + 1L
        } else {
            preferences[REGISTER_WINDOW_START] = now
            preferences[REGISTER_COUNT] = 1L
        }
    }

    private suspend fun registerWithFirebase(email: String, password: String) {
        val snapshot = dataStore.data.first()
        enforceRegisterRateLimit(snapshot[REGISTER_WINDOW_START], snapshot[REGISTER_COUNT])
        val auth = firebaseAuthOrError()
        val result = withTimeout(FIREBASE_REQUEST_TIMEOUT_MS) {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
        val verificationSent = withTimeoutOrNull(FIREBASE_EMAIL_TIMEOUT_MS) {
            result.user?.sendEmailVerification()?.await()
            true
        } == true
        dataStore.edit { preferences ->
            recordRegisterAttempt(preferences)
            saveFirebaseSession(preferences, email, emailVerified = result.user?.isEmailVerified == true)
            preferences[LAST_VERIFICATION_EMAIL_SENT] = if (verificationSent) System.currentTimeMillis() else 0L
        }
    }

    private suspend fun loginWithFirebase(email: String, password: String) {
        val snapshot = dataStore.data.first()
        enforceLoginLock(snapshot[LOGIN_LOCKED_UNTIL])
        runCatching {
            withTimeout(FIREBASE_REQUEST_TIMEOUT_MS) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            }
        }.onSuccess { result ->
            val user = result.user
            dataStore.edit { preferences ->
                saveFirebaseSession(preferences, user?.email ?: email, emailVerified = user?.isEmailVerified == true)
            }
        }.onFailure {
            recordFailedLogin()
            error("邮箱或密码错误，或账号尚未注册")
        }
    }

    private suspend fun refreshFirebaseEmailVerification() {
        val auth = firebaseAuthOrError()
        val user = auth.currentUser ?: error("请先登录账号")
        user.reload().await()
        val refreshedUser = auth.currentUser ?: error("请先登录账号")
        dataStore.edit { preferences ->
            preferences[EMAIL_VERIFIED] = refreshedUser.isEmailVerified
        }
        require(refreshedUser.isEmailVerified) { "邮箱尚未验证，请先打开邮箱里的验证链接" }
    }

    private fun saveFirebaseSession(
        preferences: MutablePreferences,
        email: String,
        emailVerified: Boolean
    ) {
        preferences[EMAIL] = email.normalizeEmail()
        preferences[EMAIL_VERIFIED] = emailVerified
        preferences[IS_LOGGED_IN] = true
        preferences[SKIPPED_LOGIN] = false
        preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
        preferences[FAILED_LOGIN_COUNT] = 0L
        preferences.remove(LOGIN_LOCKED_UNTIL)
        preferences.remove(LAST_FAILED_LOGIN_AT)
    }

    private fun firebaseAuthOrError(): FirebaseAuth {
        require(isFirebaseConfigured()) {
            "尚未配置 Firebase。请先把 google-services.json 放到 app 目录，并在 Firebase 控制台启用邮箱/密码登录。"
        }
        return FirebaseAuth.getInstance()
    }

    fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    private fun currentFirebaseUser() = if (isFirebaseConfigured()) {
        FirebaseAuth.getInstance().currentUser
    } else {
        null
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
        val FAILED_LOGIN_COUNT = longPreferencesKey("failed_login_count")
        val LAST_FAILED_LOGIN_AT = longPreferencesKey("last_failed_login_at")
        val LOGIN_LOCKED_UNTIL = longPreferencesKey("login_locked_until")
        val REGISTER_WINDOW_START = longPreferencesKey("register_window_start")
        val REGISTER_COUNT = longPreferencesKey("register_count")
        val LAST_VERIFICATION_EMAIL_SENT = longPreferencesKey("last_verification_email_sent")

        const val MAX_LOGIN_FAILURES = 5L
        const val LOGIN_FAILURE_WINDOW_MS = 15 * 60 * 1000L
        const val LOGIN_LOCK_MS = 10 * 60 * 1000L
        const val REGISTER_WINDOW_MS = 60 * 60 * 1000L
        const val MAX_REGISTER_ATTEMPTS_PER_WINDOW = 3L
        const val FIREBASE_REQUEST_TIMEOUT_MS = 25_000L
        const val FIREBASE_EMAIL_TIMEOUT_MS = 12_000L
    }
}
