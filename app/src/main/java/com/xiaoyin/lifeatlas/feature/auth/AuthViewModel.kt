package com.xiaoyin.lifeatlas.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.auth.AuthRepository
import com.xiaoyin.lifeatlas.core.auth.AuthSession
import com.xiaoyin.lifeatlas.core.auth.SocialAuthProvider
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthUiState(
    val session: AuthSession = AuthSession(),
    val accountName: String = "",
    val email: String = "",
    val verificationCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isRegisterMode: Boolean = false,
    val isPasswordResetMode: Boolean = false,
    val loginMethod: AuthLoginMethod = AuthLoginMethod.EmailCode,
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val firebaseConfigured: Boolean = false,
    val backendConfigured: Boolean = false,
    val wechatLoginConfigured: Boolean = false,
    val qqLoginConfigured: Boolean = false,
    val remoteAuthConfigured: Boolean = false,
    val authModeLabel: String = "本地账号",
    val message: String? = null,
    val error: String? = null
)

enum class AuthLoginMethod {
    EmailCode,
    Password
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val exportService = ExportServiceProvider.exportService(application)
    private val formState = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> = MutableStateFlow(AuthUiState()).also { target ->
        viewModelScope.launch {
            combine(authRepository.session, formState) { session, form ->
                form.copy(
                    session = session,
                    firebaseConfigured = authRepository.isFirebaseActive(),
                    backendConfigured = authRepository.isBackendConfigured(),
                    wechatLoginConfigured = authRepository.isSocialLoginConfigured(SocialAuthProvider.WeChat),
                    qqLoginConfigured = authRepository.isSocialLoginConfigured(SocialAuthProvider.QQ),
                    remoteAuthConfigured = authRepository.isRemoteAuthConfigured(),
                    authModeLabel = authRepository.authModeLabel()
                )
            }.collect { target.value = it }
        }
    }

    fun onAccountNameChange(value: String) {
        formState.update { it.copy(accountName = value, error = null, message = null) }
    }

    fun onEmailChange(value: String) {
        formState.update { it.copy(email = value, error = null, message = null) }
    }

    fun onVerificationCodeChange(value: String) {
        formState.update { it.copy(verificationCode = value.filter(Char::isDigit).take(6), error = null, message = null) }
    }

    fun onPasswordChange(value: String) {
        formState.update { it.copy(password = value, error = null, message = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        formState.update { it.copy(confirmPassword = value, error = null, message = null) }
    }

    fun switchMode() {
        formState.update {
            it.copy(
                isRegisterMode = !it.isRegisterMode,
                isPasswordResetMode = false,
                error = null,
                message = null,
                password = "",
                confirmPassword = "",
                verificationCode = ""
            )
        }
    }

    fun switchLoginMethod(method: AuthLoginMethod) {
        formState.update {
            it.copy(
                loginMethod = method,
                error = null,
                message = null,
                password = "",
                verificationCode = ""
            )
        }
    }

    fun switchPasswordResetMode() {
        formState.update {
            it.copy(
                isRegisterMode = false,
                isPasswordResetMode = !it.isPasswordResetMode,
                loginMethod = AuthLoginMethod.Password,
                error = null,
                message = null,
                password = "",
                confirmPassword = "",
                verificationCode = ""
            )
        }
    }

    fun sendEmailCode() {
        val state = formState.value
        val email = state.email.trim()
        val purpose = when {
            state.isRegisterMode -> "register"
            state.isPasswordResetMode -> "reset"
            else -> "login"
        }
        viewModelScope.launch {
            when {
                email.isBlank() -> {
                    formState.update { it.copy(error = "请先填写邮箱，再获取验证码", message = null) }
                    return@launch
                }
                !email.matches(EMAIL_REGEX) -> {
                    formState.update { it.copy(error = "请输入有效邮箱，例如 1685508220@qq.com", message = null) }
                    return@launch
                }
            }

            formState.update { it.copy(isSendingCode = true, error = null, message = "正在发送验证码...") }
            runCatching {
                authRepository.requestEmailCode(email, purpose)
            }.onSuccess {
                val notice = authRepository.session.first().authNotice
                formState.update {
                    it.copy(
                        isSendingCode = false,
                        message = notice ?: "验证码已发送，请检查邮箱收件箱或垃圾邮件。",
                        error = null
                    )
                }
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isSendingCode = false,
                        error = error.message ?: "验证码发送失败，请稍后再试",
                        message = null
                    )
                }
            }
        }
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val state = formState.value
        val email = state.email.trim()
        val requiresCode = state.isRegisterMode || state.isPasswordResetMode || state.loginMethod == AuthLoginMethod.EmailCode
        val requiresPassword = state.isRegisterMode || state.isPasswordResetMode || state.loginMethod == AuthLoginMethod.Password
        viewModelScope.launch {
            when {
                email.isBlank() -> {
                    formState.update { it.copy(error = "请先填写邮箱", message = null) }
                    return@launch
                }
                !email.matches(EMAIL_REGEX) -> {
                    formState.update { it.copy(error = "请输入有效邮箱，例如 1685508220@qq.com", message = null) }
                    return@launch
                }
                state.isRegisterMode && state.accountName.isBlank() -> {
                    formState.update { it.copy(error = "请先填写账号名", message = null) }
                    return@launch
                }
                requiresCode && state.verificationCode.length != 6 -> {
                    formState.update { it.copy(error = "请输入 6 位邮箱验证码", message = null) }
                    return@launch
                }
                requiresPassword && state.password.isBlank() -> {
                    formState.update { it.copy(error = "请先填写密码", message = null) }
                    return@launch
                }
                (state.isRegisterMode || state.isPasswordResetMode) && state.confirmPassword.isBlank() -> {
                    formState.update { it.copy(error = "请再次输入密码", message = null) }
                    return@launch
                }
            }

            formState.update { it.copy(isLoading = true, error = null, message = null) }
            runCatching {
                if (state.isPasswordResetMode) {
                    authRepository.confirmPasswordReset(
                        email = email,
                        code = state.verificationCode,
                        password = state.password,
                        confirmPassword = state.confirmPassword
                    )
                } else if (state.isRegisterMode) {
                    authRepository.register(
                        email = email,
                        password = state.password,
                        confirmPassword = state.confirmPassword,
                        accountName = state.accountName,
                        verificationCode = state.verificationCode
                    )
                } else if (state.loginMethod == AuthLoginMethod.EmailCode) {
                    authRepository.loginWithEmailCode(email, state.verificationCode)
                } else {
                    authRepository.login(email, state.password)
                }
            }.onSuccess {
                val authNotice = authRepository.session.first().authNotice
                formState.update {
                    it.copy(
                        isLoading = false,
                        password = "",
                        confirmPassword = "",
                        verificationCode = "",
                        isPasswordResetMode = false,
                        message = when {
                            state.isRegisterMode && !authNotice.isNullOrBlank() -> "注册成功。$authNotice"
                            state.isRegisterMode -> "注册成功，已登录岁迹。"
                            state.isPasswordResetMode -> "密码已重置，请使用新密码登录"
                            else -> "登录成功"
                        }
                    )
                }
                if (!state.isPasswordResetMode) {
                    syncCloudBackupAfterLogin()
                    onSuccess()
                }
            }.onFailure { error ->
                formState.update { it.copy(isLoading = false, error = error.message ?: "操作失败，请稍后再试") }
            }
        }
    }

    fun loginWithSocialProvider(provider: SocialAuthProvider, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            formState.update { it.copy(isLoading = true, error = null, message = null) }
            runCatching {
                authRepository.loginWithSocialProvider(provider)
            }.onSuccess {
                formState.update {
                    it.copy(
                        isLoading = false,
                        message = "${provider.displayName}登录成功",
                        error = null
                    )
                }
                onSuccess()
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "${provider.displayName}登录暂不可用",
                        message = null
                    )
                }
            }
        }
    }

    fun refreshEmailVerification() {
        viewModelScope.launch {
            runCatching { authRepository.markEmailVerifiedForLocalPreview() }
                .onSuccess {
                    formState.update {
                        it.copy(
                            message = if (authRepository.isRemoteAuthConfigured()) {
                                "邮箱验证状态已刷新。"
                            } else {
                                "已标记为邮箱已验证。配置国内后端后会替换为真实邮件验证。"
                            },
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    formState.update { it.copy(error = error.message ?: "邮箱验证失败", message = null) }
                }
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            runCatching { authRepository.sendEmailVerification() }
                .onSuccess {
                    val authNotice = authRepository.session.first().authNotice
                    formState.update {
                        it.copy(
                            message = authNotice ?: "验证邮件已发送，请检查邮箱收件箱或垃圾邮件。",
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    formState.update { it.copy(error = error.message ?: "验证邮件发送失败", message = null) }
                }
        }
    }

    fun sendPasswordResetEmail() {
        val email = formState.value.email.trim()
        viewModelScope.launch {
            if (!authRepository.isRemoteAuthConfigured()) {
                formState.update {
                    it.copy(
                        message = if (email.isBlank()) {
                            "请先输入邮箱。配置国内后端后会向该邮箱发送重置密码邮件。"
                        } else {
                            "已记录重置请求。配置国内后端后会向 $email 发送重置密码邮件。"
                        },
                        error = null
                    )
                }
                return@launch
            }

            if (email.isBlank()) {
                formState.update { it.copy(error = "请先填写邮箱", message = null) }
                return@launch
            }
            if (!email.matches(EMAIL_REGEX)) {
                formState.update { it.copy(error = "请输入有效邮箱，例如 1685508220@qq.com", message = null) }
                return@launch
            }

            runCatching { authRepository.sendPasswordResetEmail(email) }
                .onSuccess {
                    formState.update { it.copy(message = "密码重置邮件已发送，请检查邮箱收件箱或垃圾邮件。", error = null) }
                }
                .onFailure { error ->
                    formState.update { it.copy(error = error.message ?: "密码重置邮件发送失败", message = null) }
                }
        }
    }

    fun sendPasswordResetForCurrentAccount() {
        viewModelScope.launch {
            val email = authRepository.session.first().email.orEmpty()
            if (email.isBlank()) {
                formState.update { it.copy(error = "当前账号没有邮箱信息", message = null) }
                return@launch
            }
            if (!authRepository.isRemoteAuthConfigured()) {
                formState.update {
                    it.copy(
                        message = "当前是本地账号。配置国内后端后，会向 $email 发送真实重置密码邮件。",
                        error = null
                    )
                }
                return@launch
            }

            runCatching { authRepository.sendPasswordResetEmail(email) }
                .onSuccess {
                    formState.update { it.copy(message = "密码重置邮件已发送至 $email，请检查收件箱或垃圾邮件。", error = null) }
                }
                .onFailure { error ->
                    formState.update { it.copy(error = error.message ?: "密码重置邮件发送失败", message = null) }
                }
        }
    }

    fun skipLogin(onSkipped: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.skipLogin()
            formState.update { it.copy(message = "已继续使用本地模式", error = null) }
            onSkipped()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            formState.update { it.copy(message = "已退出登录", error = null) }
        }
    }

    fun clearLocalAccount() {
        viewModelScope.launch {
            authRepository.clearLocalAccount()
            formState.update {
                it.copy(
                    email = "",
                    password = "",
                    confirmPassword = "",
                    message = "本地账号已清除",
                    error = null
                )
            }
        }
    }

    private suspend fun syncCloudBackupAfterLogin() {
        if (!authRepository.isBackendConfigured()) return
        val hasLocalRecords = withContext(Dispatchers.IO) { exportService.hasLocalRecords() }

        runCatching {
            val backup = authRepository.downloadCloudBackup()
            val backupData = backup.data
            val shouldRestore = backup.exists &&
                !backupData.isNullOrBlank() &&
                withContext(Dispatchers.IO) {
                    !hasLocalRecords || exportService.hasOnlyStarterRecords()
                }
            if (shouldRestore) {
                withContext(Dispatchers.IO) { exportService.importJson(requireNotNull(backupData)) }
                backup
            } else {
                null
            }
        }.onSuccess { backup ->
            if (backup != null) {
                formState.update {
                    it.copy(
                        message = "已从云端恢复记录、标签、地点和照片引用。照片原文件仍需通过完整备份包恢复。",
                        error = null
                    )
                }
                return
            }
        }.onFailure { error ->
            formState.update {
                it.copy(
                    message = "登录成功，但云端备份暂未恢复：${error.message ?: "请稍后重试"}",
                    error = null
                )
            }
            return
        }

        if (hasLocalRecords) {
            runCatching {
                val backupJson = withContext(Dispatchers.IO) { exportService.exportJson() }
                authRepository.uploadCloudBackup(backupJson)
            }.onSuccess {
                formState.update {
                    it.copy(
                        message = "登录成功，已为当前记录保存一份云端轻量备份。",
                        error = null
                    )
                }
            }
            return
        }
    }
}
