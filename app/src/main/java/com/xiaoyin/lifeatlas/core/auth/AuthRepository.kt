package com.xiaoyin.lifeatlas.core.auth

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.xiaoyin.lifeatlas.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private val Context.authDataStore by preferencesDataStore(name = "lifeatlas_auth")

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val authNotice: String? = null,
    val lastLoginAt: Long? = null,
    val skippedLogin: Boolean = false,
    val failedLoginCount: Int = 0,
    val loginLockedUntil: Long? = null
)

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.authDataStore
    private val authApiBaseUrl = BuildConfig.AUTH_API_BASE_URL.trim()
    private val authProvider = BuildConfig.AUTH_PROVIDER.trim().lowercase()
    private val authApiClient = authApiBaseUrl.takeIf { it.isNotBlank() }?.let(::AuthApiClient)

    val session: Flow<AuthSession> = dataStore.data.map { preferences ->
        val firebaseUser = currentFirebaseUser()
        val backendAccessToken = preferences[BACKEND_ACCESS_TOKEN]
        AuthSession(
            isLoggedIn = backendAccessToken != null || firebaseUser != null || (preferences[IS_LOGGED_IN] ?: false),
            email = preferences[EMAIL] ?: firebaseUser?.email,
            emailVerified = preferences[EMAIL_VERIFIED] ?: firebaseUser?.isEmailVerified ?: false,
            authNotice = preferences[AUTH_NOTICE],
            lastLoginAt = preferences[LAST_LOGIN_AT],
            skippedLogin = preferences[SKIPPED_LOGIN] ?: false,
            failedLoginCount = preferences[FAILED_LOGIN_COUNT]?.toInt() ?: 0,
            loginLockedUntil = preferences[LOGIN_LOCKED_UNTIL]
        )
    }

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        accountName: String = "旷野小旅人",
        verificationCode: String = ""
    ) {
        val normalizedEmail = email.normalizeEmail()
        val normalizedAccountName = accountName.trim()
        validateEmail(normalizedEmail)
        validatePassword(password)
        require(password == confirmPassword) { "两次输入的密码不一致" }
        require(normalizedAccountName.length in 2..24) { "账号名需要 2-24 个字符" }

        when {
            isBackendConfigured() -> registerWithBackend(normalizedEmail, password, normalizedAccountName, verificationCode)
            shouldUseFirebase() -> registerWithFirebase(normalizedEmail, password)
            else -> registerLocal(normalizedEmail, password)
        }
    }

    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)

        when {
            isBackendConfigured() -> loginWithBackend(normalizedEmail, password)
            shouldUseFirebase() -> loginWithFirebase(normalizedEmail, password)
            else -> loginLocal(normalizedEmail, password)
        }
    }

    suspend fun requestEmailCode(email: String, purpose: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        require(isBackendConfigured()) { "邮箱验证码需要先启用国内后端账号服务" }
        val result = withTimeout(BACKEND_REQUEST_TIMEOUT_MS) {
            requireNotNull(authApiClient) { "国内后端地址未配置" }
                .requestEmailCode(normalizedEmail, purpose)
        }
        dataStore.edit { preferences ->
            preferences[AUTH_NOTICE] = result.message
        }
        require(result.emailSent) { result.message }
    }

    suspend fun loginWithEmailCode(email: String, code: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        require(code.trim().matches(Regex("^\\d{6}$"))) { "请输入 6 位邮箱验证码" }
        require(isBackendConfigured()) { "邮箱验证码登录需要先启用国内后端账号服务" }
        val result = withTimeout(BACKEND_REQUEST_TIMEOUT_MS) {
            requireNotNull(authApiClient) { "国内后端地址未配置" }
                .loginWithEmailCode(normalizedEmail, code.trim())
        }
        dataStore.edit { preferences ->
            saveBackendSession(preferences, result)
        }
    }

    suspend fun markEmailVerifiedForLocalPreview() {
        if (shouldUseFirebase()) {
            refreshFirebaseEmailVerification()
            return
        }
        dataStore.edit { preferences ->
            require(!preferences[EMAIL].isNullOrBlank()) { "请先注册或登录账号" }
            preferences[EMAIL_VERIFIED] = true
        }
    }

    suspend fun sendEmailVerification() {
        if (isBackendConfigured()) {
            val token = dataStore.data.first()[BACKEND_ACCESS_TOKEN] ?: error("请先登录账号")
            val result = requireNotNull(authApiClient) { "国内后端地址未配置" }.requestEmailVerification(token)
            dataStore.edit { preferences ->
                preferences[AUTH_NOTICE] = result.message
            }
            require(result.emailSent) { result.message }
            return
        }

        if (!shouldUseFirebase()) {
            markEmailVerifiedForLocalPreview()
            return
        }

        val auth = firebaseAuthOrError()
        val user = auth.currentUser ?: error("请先登录账号")
        require(!user.isEmailVerified) { "邮箱已经验证" }
        user.sendEmailVerification().await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        when {
            isBackendConfigured() -> {
                val result = requireNotNull(authApiClient) { "国内后端地址未配置" }
                    .requestPasswordReset(normalizedEmail)
                require(result.emailSent) { result.message }
            }
            shouldUseFirebase() -> firebaseAuthOrError().sendPasswordResetEmail(normalizedEmail).await()
            else -> Unit
        }
    }

    suspend fun confirmPasswordReset(
        email: String,
        code: String,
        password: String,
        confirmPassword: String
    ) {
        val normalizedEmail = email.normalizeEmail()
        validateEmail(normalizedEmail)
        validatePassword(password)
        require(password == confirmPassword) { "两次输入的密码不一致" }
        require(code.trim().matches(Regex("^\\d{6}$"))) { "请输入 6 位邮箱验证码" }
        require(isBackendConfigured()) { "重置密码需要先启用国内后端账号服务" }
        val result = withTimeout(BACKEND_REQUEST_TIMEOUT_MS) {
            requireNotNull(authApiClient) { "国内后端地址未配置" }
                .confirmPasswordReset(normalizedEmail, code.trim(), password)
        }
        dataStore.edit { preferences ->
            preferences[AUTH_NOTICE] = result.message
        }
        require(result.ok) { result.message }
    }

    suspend fun skipLogin() {
        dataStore.edit { preferences ->
            preferences[SKIPPED_LOGIN] = true
            preferences[IS_LOGGED_IN] = false
            preferences.remove(BACKEND_ACCESS_TOKEN)
            preferences.remove(BACKEND_REFRESH_TOKEN)
        }
    }

    suspend fun logout() {
        if (shouldUseFirebase()) {
            FirebaseAuth.getInstance().signOut()
        }
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences.remove(BACKEND_ACCESS_TOKEN)
            preferences.remove(BACKEND_REFRESH_TOKEN)
        }
    }

    suspend fun clearLocalAccount() {
        if (shouldUseFirebase()) {
            FirebaseAuth.getInstance().signOut()
        }
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    fun isBackendConfigured(): Boolean = authApiBaseUrl.isNotBlank() && authProvider != "firebase"

    fun isFirebaseActive(): Boolean = shouldUseFirebase()

    fun isRemoteAuthConfigured(): Boolean = isBackendConfigured() || shouldUseFirebase()

    fun authModeLabel(): String = when {
        isBackendConfigured() -> "国内后端账号"
        shouldUseFirebase() -> "Firebase 邮箱账号"
        else -> "本地账号"
    }

    private suspend fun registerLocal(email: String, password: String) {
        val snapshot = dataStore.data.first()
        enforceRegisterRateLimit(snapshot[REGISTER_WINDOW_START], snapshot[REGISTER_COUNT])
        require(snapshot[EMAIL].isNullOrBlank()) { "本机已存在账号，请先登录或在设置中清除本地账号" }

        val salt = createSalt()
        dataStore.edit { preferences ->
            recordRegisterAttempt(preferences)
            preferences[EMAIL] = email
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

    private suspend fun loginLocal(email: String, password: String) {
        val snapshot = dataStore.data.first()
        enforceLoginLock(snapshot[LOGIN_LOCKED_UNTIL])
        val storedEmail = snapshot[EMAIL] ?: error("当前还没有注册账号")
        val salt = snapshot[PASSWORD_SALT] ?: error("账号信息不完整，请重新注册")
        val storedHash = snapshot[PASSWORD_HASH] ?: error("账号信息不完整，请重新注册")
        if (storedEmail != email || hashPassword(password, salt) != storedHash) {
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

    private suspend fun registerWithBackend(
        email: String,
        password: String,
        accountName: String,
        verificationCode: String
    ) {
        val snapshot = dataStore.data.first()
        enforceRegisterRateLimit(snapshot[REGISTER_WINDOW_START], snapshot[REGISTER_COUNT])
        require(verificationCode.trim().matches(Regex("^\\d{6}$"))) { "请输入 6 位邮箱验证码" }
        val result = withTimeout(BACKEND_REQUEST_TIMEOUT_MS) {
            requireNotNull(authApiClient) { "国内后端地址未配置" }
                .register(email, password, accountName, verificationCode.trim())
        }
        dataStore.edit { preferences ->
            recordRegisterAttempt(preferences)
            saveBackendSession(preferences, result)
        }
    }

    private suspend fun loginWithBackend(email: String, password: String) {
        val snapshot = dataStore.data.first()
        enforceLoginLock(snapshot[LOGIN_LOCKED_UNTIL])
        runCatching {
            withTimeout(BACKEND_REQUEST_TIMEOUT_MS) {
                requireNotNull(authApiClient) { "国内后端地址未配置" }.login(email, password)
            }
        }.onSuccess { result ->
            dataStore.edit { preferences ->
                saveBackendSession(preferences, result)
            }
        }.onFailure { error ->
            recordFailedLogin()
            error(error.message ?: "登录失败，请检查邮箱或密码")
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
        require(refreshedUser.isEmailVerified) { "邮箱尚未验证，请先打开邮箱中的验证链接" }
    }

    private fun saveBackendSession(preferences: MutablePreferences, session: AuthApiSession) {
        preferences[EMAIL] = session.email.normalizeEmail()
        preferences[EMAIL_VERIFIED] = session.emailVerified
        if (session.notice.isNullOrBlank()) {
            preferences.remove(AUTH_NOTICE)
        } else {
            preferences[AUTH_NOTICE] = session.notice
        }
        preferences[BACKEND_ACCESS_TOKEN] = session.accessToken
        if (session.refreshToken.isNullOrBlank()) {
            preferences.remove(BACKEND_REFRESH_TOKEN)
        } else {
            preferences[BACKEND_REFRESH_TOKEN] = session.refreshToken
        }
        preferences[IS_LOGGED_IN] = true
        preferences[SKIPPED_LOGIN] = false
        preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
        preferences[FAILED_LOGIN_COUNT] = 0L
        preferences.remove(LOGIN_LOCKED_UNTIL)
        preferences.remove(LAST_FAILED_LOGIN_AT)
    }

    private fun saveFirebaseSession(
        preferences: MutablePreferences,
        email: String,
        emailVerified: Boolean
    ) {
        preferences[EMAIL] = email.normalizeEmail()
        preferences[EMAIL_VERIFIED] = emailVerified
        preferences.remove(AUTH_NOTICE)
        preferences[IS_LOGGED_IN] = true
        preferences[SKIPPED_LOGIN] = false
        preferences[LAST_LOGIN_AT] = System.currentTimeMillis()
        preferences[FAILED_LOGIN_COUNT] = 0L
        preferences.remove(LOGIN_LOCKED_UNTIL)
        preferences.remove(LAST_FAILED_LOGIN_AT)
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

    private fun firebaseAuthOrError(): FirebaseAuth {
        require(shouldUseFirebase()) {
            "当前未启用 Firebase。国内正式版请配置自建后端，邮箱验证由阿里云邮件服务发送。"
        }
        return FirebaseAuth.getInstance()
    }

    private fun shouldUseFirebase(): Boolean = authProvider == "firebase" && isFirebaseConfigured()

    private fun currentFirebaseUser() = if (shouldUseFirebase()) {
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
        val BACKEND_ACCESS_TOKEN = stringPreferencesKey("backend_access_token")
        val BACKEND_REFRESH_TOKEN = stringPreferencesKey("backend_refresh_token")
        val AUTH_NOTICE = stringPreferencesKey("auth_notice")

        const val MAX_LOGIN_FAILURES = 5L
        const val LOGIN_FAILURE_WINDOW_MS = 15 * 60 * 1000L
        const val LOGIN_LOCK_MS = 10 * 60 * 1000L
        const val REGISTER_WINDOW_MS = 60 * 60 * 1000L
        const val MAX_REGISTER_ATTEMPTS_PER_WINDOW = 3L
        const val FIREBASE_REQUEST_TIMEOUT_MS = 25_000L
        const val FIREBASE_EMAIL_TIMEOUT_MS = 12_000L
        const val BACKEND_REQUEST_TIMEOUT_MS = 15_000L
    }
}
