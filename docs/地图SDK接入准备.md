# 地图 SDK 接入准备

记录日期：2026-06-24

适用阶段：V0.2 地图能力接入前置准备

## 目标

在不提交真实地图 Key、不替换现有地图页 UI 的前提下，完成高德地图 Android SDK 接入前的工程准备。

本阶段完成：

- 本地 Key 配置入口
- Manifest Key 占位
- 地图网络权限声明
- `BuildConfig` 配置注入
- 地图供应商配置对象
- 后续接入真实 SDK 的注意事项

## 本地 Key 配置

真实 Key 不进入 Git。

申请高德 Key 后，在本机 `local.properties` 增加：

```properties
lifeatlas.amap.apiKey=你的高德AndroidKey
```

仓库中的代码会读取该值，并注入：

- `BuildConfig.AMAP_API_KEY`
- `BuildConfig.AMAP_CONFIGURED`
- `AndroidManifest.xml` 中的 `com.amap.api.v2.apikey`

如果没有配置 Key，项目仍可正常构建，只是后续真实地图 SDK 不能正常显示地图。

## 已增加权限

当前只增加地图展示所需的基础网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

暂不增加定位权限。

定位权限会在“当前位置”模块中单独实现，避免过早扩大权限范围。

## 已增加配置入口

新增：

```text
app/src/main/java/com/xiaoyin/lifeatlas/core/map/MapSdkConfig.kt
```

当前提供：

- `MapSdkConfig.provider`
- `MapSdkConfig.isAmapConfigured`
- `MapProvider.Amap`

后续接入地图 SDK 时，UI 和业务层应优先依赖项目内的地图配置和抽象模型，不要把高德 SDK 对象直接扩散到业务层。

## 后续接入真实 SDK 前必须完成

1. 申请高德开放平台 Android Key。
2. 确认应用包名：

```text
com.xiaoyin.lifeatlas
```

3. 配置发布签名 SHA1。
4. 在 `local.properties` 增加 `lifeatlas.amap.apiKey`。
5. 在调用任何高德 SDK 接口前完成隐私合规初始化。
6. 在隐私政策中说明地图服务、网络请求和定位相关数据处理。

## 已接入依赖

当前已固定接入：

```text
com.amap.api:3dmap:10.0.600
```

依赖版本统一记录在：

```text
gradle/libs.versions.toml
```

## 暂不做

本阶段暂不做：

- 不提交真实 Key
- 不申请定位权限
- 不实现地图选点

## 验证方式

命令行构建：

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat" :app:assembleDebug --no-daemon
```

预期结果：

```text
BUILD SUCCESSFUL
```

## 后续模块建议

下一步建议实现：

```text
地图记录 marker 展示
```

可以把 Room 中已有经纬度的记录渲染成地图 marker，并保留下方列表作为辅助信息。
