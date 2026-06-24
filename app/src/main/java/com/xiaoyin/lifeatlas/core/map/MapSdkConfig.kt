package com.xiaoyin.lifeatlas.core.map

import com.xiaoyin.lifeatlas.BuildConfig

object MapSdkConfig {
    val provider: MapProvider = MapProvider.Amap
    val isAmapConfigured: Boolean = BuildConfig.AMAP_CONFIGURED
}

enum class MapProvider(val displayName: String) {
    Amap("高德地图"),
}
