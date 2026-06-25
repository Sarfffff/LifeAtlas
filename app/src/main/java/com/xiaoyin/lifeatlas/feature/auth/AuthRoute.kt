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
                firebaseConfigured = uiState.firebaseConfigured,
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
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onSubmit = { viewModel.submit(onSuccess = onContinue) },
                onSwitchMode = viewModel::switchMode,
                onForgotPassword = viewModel::sendPasswordResetEmail,
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
                    if (uiState.firebaseConfigured) {
                        "Firebase 已配置：注册后会发送真实邮箱验证邮件，忘记密码会发送重置邮件。QQ 邮箱可以作为收件邮箱使用。"
                    } else {
                        "Firebase 尚未配置：当前仍是本地账号体验。把 google-services.json 放到 app 目录并启用邮箱/密码登录后，会发送真实邮箱验证和重置密码邮件。"
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
    firebaseConfigured: Boolean,
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
            text = if (firebaseConfigured) "账号类型：Firebase 邮箱账号" else "账号类型：本地账号",
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
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
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

            SecurityStatusText(uiState = uiState)

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("邮箱") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
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
            if (uiState.isRegisterMode) {
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
                        else -> "登录"
                    },
                    fontWeight = FontWeight.Black
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSwitchMode) {
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
            "安全策略：每小时最多创建 3 次本地账号，且不会覆盖已有账号。"
        }
        else -> {
            "安全策略：密码会加盐哈希后保存在本机，不保存明文密码。"
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
    firebaseConfigured: Boolean,
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
                firebaseConfigured = firebaseConfigured,
                lastLoginAt = lastLoginAt,
                failedLoginCount = failedLoginCount,
                lockedUntil = lockedUntil
            )
            Text(
                text = when {
                    verified -> "邮箱已验证"
                    firebaseConfigured -> "邮箱尚未验证。请打开邮箱中的验证链接，然后回到这里刷新状态。"
                    else -> "邮箱尚未验证。当前为本地预览验证，后续接入真实邮件验证。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted
            )
            if (!verified) {
                if (firebaseConfigured) {
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
                Text(if (firebaseConfigured) "发送重置密码邮件" else "查看重置密码说明")
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
