package com.xiaoyin.lifeatlas.core.map

import com.xiaoyin.lifeatlas.BuildConfig

object MapSdkConfig {
    val provider: MapProvider = MapProvider.Amap
    val isAmapConfigured: Boolean = BuildConfig.AMAP_CONFIGURED
    val amapApiKey: String = BuildConfig.AMAP_API_KEY
}

enum class MapProvider {
    Amap,
}
