package com.xiaoyin.lifeatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xiaoyin.lifeatlas.core.map.AmapPrivacyInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AmapPrivacyInitializer.initialize(this)
        setContent {
            LifeAtlasApp()
        }
    }
}
