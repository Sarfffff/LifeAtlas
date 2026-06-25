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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun AuthRoute(
    onContinue: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                onVerify = viewModel::markEmailVerifiedForLocalPreview,
                onLogout = viewModel::logout,
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
                onForgotPassword = viewModel::sendPasswordResetPlaceholder,
                onSkip = { viewModel.skipLogin(onSkipped = onContinue) }
            )
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
                    "现在先提供本地账号体验：邮箱、密码哈希、登录状态会持久化保存在本机。接入 Firebase 后，会替换为真实邮箱验证、忘记密码和云端账号。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessMuted
                )
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
                Text(if (uiState.isRegisterMode) "注册并登录" else "登录", fontWeight = FontWeight.Black)
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
private fun LoggedInCard(
    email: String,
    verified: Boolean,
    onVerify: () -> Unit,
    onLogout: () -> Unit,
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
            Text(
                text = if (verified) "邮箱已验证" else "邮箱尚未验证。当前为本地预览验证，后续接入真实邮件验证。",
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted
            )
            if (!verified) {
                OutlinedButton(onClick = onVerify, modifier = Modifier.fillMaxWidth()) {
                    Text("本地标记邮箱已验证")
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WildernessTeal)) {
                Text("继续使用岁迹")
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }
}
