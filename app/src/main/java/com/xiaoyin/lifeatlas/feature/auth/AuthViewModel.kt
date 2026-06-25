package com.xiaoyin.lifeatlas.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.auth.AuthRepository
import com.xiaoyin.lifeatlas.core.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val session: AuthSession = AuthSession(),
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val firebaseConfigured: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val formState = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> = MutableStateFlow(AuthUiState()).also { target ->
        viewModelScope.launch {
            combine(authRepository.session, formState) { session, form ->
                form.copy(
                    session = session,
                    firebaseConfigured = authRepository.isFirebaseConfigured()
                )
            }.collect { target.value = it }
        }
    }

    fun onEmailChange(value: String) {
        formState.update { it.copy(email = value, error = null, message = null) }
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
                error = null,
                message = null,
                password = "",
                confirmPassword = ""
            )
        }
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val state = formState.value
        viewModelScope.launch {
            if (state.email.isBlank() || state.password.isBlank()) {
                formState.update { it.copy(error = "请先填写邮箱和密码", message = null) }
                return@launch
            }
            if (state.isRegisterMode && state.confirmPassword.isBlank()) {
                formState.update { it.copy(error = "请再次输入密码", message = null) }
                return@launch
            }
            formState.update { it.copy(isLoading = true, error = null, message = null) }
            runCatching {
                if (state.isRegisterMode) {
                    authRepository.register(state.email, state.password, state.confirmPassword)
                } else {
                    authRepository.login(state.email, state.password)
                }
            }.onSuccess {
                formState.update {
                    it.copy(
                        isLoading = false,
                        password = "",
                        confirmPassword = "",
                        message = if (state.isRegisterMode) {
                            if (authRepository.isFirebaseConfigured()) {
                                "注册成功。系统已尝试发送验证邮件，请检查邮箱；如果没收到，可登录后重新发送。"
                            } else {
                                "注册成功。当前是本地账号，接入 Firebase 后会发送真实邮箱验证邮件。"
                            }
                        } else {
                            "登录成功"
                        }
                    )
                }
                onSuccess()
            }.onFailure { error ->
                formState.update { it.copy(isLoading = false, error = error.message ?: "操作失败") }
            }
        }
    }

    fun refreshEmailVerification() {
        viewModelScope.launch {
            runCatching { authRepository.markEmailVerifiedForLocalPreview() }
                .onSuccess {
                    formState.update {
                        it.copy(
                            message = if (authRepository.isFirebaseConfigured()) {
                                "邮箱验证状态已刷新。"
                            } else {
                                "已标记为邮箱已验证。本功能后续会替换为真实邮件验证。"
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
                    formState.update { it.copy(message = "验证邮件已发送，请检查邮箱收件箱或垃圾邮件。", error = null) }
                }
                .onFailure { error ->
                    formState.update { it.copy(error = error.message ?: "验证邮件发送失败", message = null) }
                }
        }
    }

    fun sendPasswordResetEmail() {
        val email = formState.value.email.trim()
        viewModelScope.launch {
            if (!authRepository.isFirebaseConfigured()) {
                formState.update {
                    it.copy(
                        message = if (email.isBlank()) {
                            "请先输入邮箱。配置 Firebase 后会向该邮箱发送重置密码邮件。"
                        } else {
                            "已记录重置请求。配置 Firebase 后会向 $email 发送重置密码邮件。"
                        },
                        error = null
                    )
                }
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
}
