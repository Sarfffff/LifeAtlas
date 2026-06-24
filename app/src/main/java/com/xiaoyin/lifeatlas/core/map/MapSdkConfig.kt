package com.xiaoyin.lifeatlas.core.map

import com.xiaoyin.lifeatlas.BuildConfig

object MapSdkConfig {
    val provider: MapProvider = MapProvider.Amap
    val isAmapConfigured: Boolean = BuildConfig.AMAP_CONFIGURED
    val statusText: String = if (isAmapConfigured) "已配置" else "未配置"
    val localPropertyKey: String = "lifeatlas.amap.apiKey"
    val localPropertyExample: String = "$localPropertyKey=你的高德 Android Key"
    val setupSteps: List<String> = listOf(
        "在高德开放平台创建 Android 应用并获取 Key。",
        "把调试签名 SHA1 和包名 ${BuildConfig.APPLICATION_ID} 填到高德控制台。",
        "在项目根目录 local.properties 添加：$localPropertyExample。",
        "重新构建并安装 APK，然后到设置页确认 Key 状态为已配置。"
    )
    val iconRefreshTips: List<String> = listOf(
        "安装新包前先卸载手机里的旧版岁迹。",
        "如果桌面仍显示旧图标，重启手机或清理桌面启动器缓存。",
        "确认安装的是 dist/LifeAtlas-v1.0-wilderness-ui-debug.apk 的最新修改时间版本。"
    )
}

enum class MapProvider(val displayName: String) {
    Amap("高德地图"),
}
