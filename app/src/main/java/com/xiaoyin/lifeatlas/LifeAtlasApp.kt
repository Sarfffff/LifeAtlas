package com.xiaoyin.lifeatlas

import androidx.compose.runtime.Composable
import com.xiaoyin.lifeatlas.core.ui.theme.LifeAtlasTheme
import com.xiaoyin.lifeatlas.core.sync.AutomaticCloudBackupEffect
import com.xiaoyin.lifeatlas.navigation.LifeAtlasNavHost

@Composable
fun LifeAtlasApp() {
    LifeAtlasTheme {
        AutomaticCloudBackupEffect()
        LifeAtlasNavHost()
    }
}
