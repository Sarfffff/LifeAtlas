package com.xiaoyin.lifeatlas.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.auth.SocialAuthProvider
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AuthRoute(
    onContinue: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showClearAccountConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "账号与安全",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )

        if (uiState.session.isLoggedIn) {
            LoggedInCard(
                email = uiState.session.email.orEmpty(),
                verified = uiState.session.emailVerified,
                remoteVerificationEnabled = uiState.remoteAuthConfigured,
                authModeLabel = uiState.authModeLabel,
                lastLoginAt = uiState.session.lastLoginAt,
                failedLoginCount = uiState.session.failedLoginCount,
                lockedUntil = uiState.session.loginLockedUntil,
                onSendVerification = viewModel::sendVerificationEmail,
                onRefreshVerification = viewModel::refreshEmailVerification,
                onResetPassword = viewModel::sendPasswordResetForCurrentAccount,
                onLogout = { showLogoutConfirm = true },
                onClearLocalAccount = { showClearAccountConfirm = true },
                onContinue = onContinue
            )
        } else {
            AuthFormCard(
                uiState = uiState,
                onAccountNameChange = viewModel::onAccountNameChange,
                onEmailChange = viewModel::onEmailChange,
                onVerificationCodeChange = viewModel::onVerificationCodeChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onLoginMethodChange = viewModel::switchLoginMethod,
                onSendEmailCode = viewModel::sendEmailCode,
                onSubmit = { viewModel.submit(onSuccess = onContinue) },
                onSocialLogin = { provider -> viewModel.loginWithSocialProvider(provider, onSuccess = onContinue) },
                onSwitchMode = viewModel::switchMode,
                onForgotPassword = viewModel::switchPasswordResetMode,
                onSkip = { viewModel.skipLogin(onSkipped = onContinue) }
            )
        }

        if (uiState.session.isLoggedIn) {
            AccountDangerZone(onClearLocalAccount = { showClearAccountConfirm = true })
        }

        uiState.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = WildernessTeal)
        }
        uiState.error?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WildernessMeadow.copy(alpha = 0.38f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前阶段说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
                Text(
                    when {
                        uiState.backendConfigured -> {
                            "国内后端已配置：注册登录会请求自建服务，邮箱验证和忘记密码邮件由后端通过阿里云邮件服务发送。"
                        }
                        uiState.firebaseConfigured -> {
                            "Firebase 已显式启用：注册后会发送真实邮箱验证邮件，忘记密码会发送重置邮件。"
                        }
                        else -> {
                            "当前为本地账号：不会访问 Firebase，因此国内网络不会卡在注册处理中。购买服务器后，在 local.properties 填入国内后端地址即可启用真实邮箱验证。"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessMuted
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("退出后仍会保留本机记录、照片缓存和设置。下次进入会回到账号页。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                    }
                ) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAccountConfirm = false },
            title = { Text("清除本地账号") },
            text = { Text("这只会清除登录账号信息，不会删除 Room 中的记忆、照片、标签和地点数据。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAccountConfirm = false
                        viewModel.clearLocalAccount()
                    }
                ) {
                    Text("确认清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAccountConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SecurityInfoRows(
    authModeLabel: String,
    lastLoginAt: Long?,
    failedLoginCount: Int,
    lockedUntil: Long?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WildernessMeadow.copy(alpha = 0.34f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "账号类型：$authModeLabel",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )
        Text(
            text = "最近登录：${lastLoginAt?.formatAuthTime() ?: "暂无记录"}",
            style = MaterialTheme.typography.bodyMedium,
            color = WildernessMuted
        )
        Text(
            text = if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
                "安全状态：登录保护中，${lockedUntil.formatAuthTime()} 后再试"
            } else {
                "安全状态：连续失败 $failedLoginCount 次，达到 5 次会临时冷却"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = WildernessMuted
        )
    }
}

private fun Long.formatAuthTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

@Composable
private fun AccountDangerZone(onClearLocalAccount: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("账号维护", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(
                "清除本地账号只会移除登录信息，不会删除你的记忆、照片、标签和地点。",
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted
            )
            OutlinedButton(onClick = onClearLocalAccount, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除本地账号信息")
            }
        }
    }
}

@Composable
private fun AuthFormCard(
    uiState: AuthUiState,
    onAccountNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onVerificationCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onLoginMethodChange: (AuthLoginMethod) -> Unit,
    onSendEmailCode: () -> Unit,
    onSubmit: () -> Unit,
    onSocialLogin: (SocialAuthProvider) -> Unit,
    onSwitchMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_mascot_text),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Column {
                    Text(
                        text = if (uiState.isRegisterMode) "创建岁迹账号" else "登录岁迹账号",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal
                    )
                    Text(
                        text = "第一版仍支持本地优先使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WildernessMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AuthModeChip(
                    selected = !uiState.isRegisterMode,
                    text = "登录",
                    onClick = {
                        if (uiState.isRegisterMode) onSwitchMode()
                    }
                )
                AuthModeChip(
                    selected = uiState.isRegisterMode,
                    text = "注册",
                    onClick = {
                        if (!uiState.isRegisterMode) onSwitchMode()
                    }
                )
            }

            if (!uiState.isRegisterMode && !uiState.isPasswordResetMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AuthModeChip(
                        selected = uiState.loginMethod == AuthLoginMethod.EmailCode,
                        text = "验证码登录",
                        onClick = { onLoginMethodChange(AuthLoginMethod.EmailCode) }
                    )
                    AuthModeChip(
                        selected = uiState.loginMethod == AuthLoginMethod.Password,
                        text = "密码登录",
                        onClick = { onLoginMethodChange(AuthLoginMethod.Password) }
                    )
                }
            }

            SecurityStatusText(uiState = uiState)

            if (uiState.isRegisterMode) {
                OutlinedTextField(
                    value = uiState.accountName,
                    onValueChange = onAccountNameChange,
                    label = { Text("账号名") },
                    leadingIcon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("邮箱") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            if (uiState.isRegisterMode || uiState.isPasswordResetMode || uiState.loginMethod == AuthLoginMethod.EmailCode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.verificationCode,
                        onValueChange = onVerificationCodeChange,
                        label = { Text("邮箱验证码") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedButton(
                        onClick = onSendEmailCode,
                        enabled = !uiState.isSendingCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (uiState.isSendingCode) "发送中..." else "获取邮箱验证码",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (uiState.isRegisterMode || uiState.isPasswordResetMode || uiState.loginMethod == AuthLoginMethod.Password) {
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            if (uiState.isRegisterMode || uiState.isPasswordResetMode) {
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("确认密码") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WildernessTeal, contentColor = WildernessPaper)
            ) {
                Text(
                    when {
                        uiState.isLoading -> "处理中..."
                        uiState.isRegisterMode -> "注册并登录"
                        uiState.loginMethod == AuthLoginMethod.EmailCode -> "验证码登录"
                        else -> "登录"
                    },
                    fontWeight = FontWeight.Black
                )
            }

            SocialLoginRow(
                wechatConfigured = uiState.wechatLoginConfigured,
                qqConfigured = uiState.qqLoginConfigured,
                enabled = !uiState.isLoading,
                onSocialLogin = onSocialLogin
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (uiState.isPasswordResetMode) onForgotPassword() else onSwitchMode() }) {
                    Text(if (uiState.isRegisterMode) "已有账号，去登录" else "没有账号，去注册")
                }
                TextButton(onClick = onForgotPassword) {
                    Text("忘记密码")
                }
            }
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.End)) {
                Text("跳过，继续本地使用")
            }
        }
    }
}

@Composable
private fun SocialLoginRow(
    wechatConfigured: Boolean,
    qqConfigured: Boolean,
    enabled: Boolean,
    onSocialLogin: (SocialAuthProvider) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(WildernessMeadow.copy(alpha = 0.42f))
            )
            Text("其他方式", style = MaterialTheme.typography.labelMedium, color = WildernessMuted)
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(WildernessMeadow.copy(alpha = 0.42f))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialLoginButton(
                text = "微信",
                configured = wechatConfigured,
                enabled = enabled,
                onClick = { onSocialLogin(SocialAuthProvider.WeChat) },
                modifier = Modifier.weight(1f)
            )
            SocialLoginButton(
                text = "QQ",
                configured = qqConfigured,
                enabled = enabled,
                onClick = { onSocialLogin(SocialAuthProvider.QQ) },
                modifier = Modifier.weight(1f)
            )
        }
        if (!wechatConfigured || !qqConfigured) {
            Text(
                text = "第三方登录入口已整理好；配置开放平台 AppID 和后端 OAuth 后即可切换为真实登录。",
                style = MaterialTheme.typography.bodySmall,
                color = WildernessMuted
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    configured: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (configured) text else "$text · 待配置",
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun AuthModeChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
            )
        }
    )
}

@Composable
private fun SecurityStatusText(uiState: AuthUiState) {
    val lockedUntil = uiState.session.loginLockedUntil
    val now = System.currentTimeMillis()
    val text = when {
        lockedUntil != null && lockedUntil > now -> {
            val minutes = ((lockedUntil - now) / 60_000L).coerceAtLeast(1L)
            "账号保护中：登录尝试过于频繁，请约 $minutes 分钟后再试。"
        }
        uiState.session.failedLoginCount > 0 -> {
            "安全提醒：已连续失败 ${uiState.session.failedLoginCount} 次，连续失败 5 次会临时冷却。"
        }
        uiState.isRegisterMode -> {
            if (uiState.remoteAuthConfigured) {
                "安全策略：注册请求会走 ${uiState.authModeLabel}，每小时最多提交 3 次，避免恶意注册。"
            } else {
                "安全策略：每小时最多创建 3 次本地账号，且不会覆盖已有账号。"
            }
        }
        else -> {
            if (uiState.remoteAuthConfigured) {
                "安全策略：当前使用 ${uiState.authModeLabel}；登录失败 5 次会临时冷却。"
            } else {
                "安全策略：密码会加盐哈希后保存在本机，不保存明文密码。"
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = WildernessMuted
    )
}

@Composable
private fun LoggedInCard(
    email: String,
    verified: Boolean,
    remoteVerificationEnabled: Boolean,
    authModeLabel: String,
    lastLoginAt: Long?,
    failedLoginCount: Int,
    lockedUntil: Long?,
    onSendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onResetPassword: () -> Unit,
    onLogout: () -> Unit,
    onClearLocalAccount: () -> Unit,
    onContinue: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(
                imageVector = if (verified) Icons.Outlined.CheckCircle else Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = WildernessTeal,
                modifier = Modifier.size(42.dp)
            )
            Text(email, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            SecurityInfoRows(
                authModeLabel = authModeLabel,
                lastLoginAt = lastLoginAt,
                failedLoginCount = failedLoginCount,
                lockedUntil = lockedUntil
            )
            Text(
                text = when {
                    verified -> "邮箱已验证"
                    remoteVerificationEnabled -> "邮箱尚未验证。请打开邮箱中的验证链接，然后回到这里刷新状态。"
                    else -> "邮箱尚未验证。当前为本地预览验证，配置国内后端后会接入真实邮件验证。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted
            )
            if (!verified) {
                if (remoteVerificationEnabled) {
                    OutlinedButton(onClick = onSendVerification, modifier = Modifier.fillMaxWidth()) {
                        Text("重新发送验证邮件")
                    }
                    OutlinedButton(onClick = onRefreshVerification, modifier = Modifier.fillMaxWidth()) {
                        Text("我已验证，刷新状态")
                    }
                } else {
                    OutlinedButton(onClick = onRefreshVerification, modifier = Modifier.fillMaxWidth()) {
                        Text("本地标记邮箱已验证")
                    }
                }
            }
            OutlinedButton(onClick = onResetPassword, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (remoteVerificationEnabled) "发送重置密码邮件" else "查看重置密码说明")
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WildernessTeal)) {
                Text("继续使用岁迹")
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }
}
