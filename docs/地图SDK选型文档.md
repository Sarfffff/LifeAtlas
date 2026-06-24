# 地图 SDK 选型文档

记录日期：2026-06-24

适用阶段：V0.2 地图能力接入前置评估

## 背景

岁迹的核心表达是“人生地图”，当前 V0.1 已经具备：

- 手动填写地点名称
- 手动填写经纬度
- 地图页展示带坐标记录列表
- 新增和编辑页面的“地图选点”入口占位

下一阶段需要接入真实地图能力，包括：

```text
地图展示 -> marker 展示 -> 地图选点 -> 当前位置 -> 地址反查
```

## 候选方案

### 方案一：高德地图 Android SDK

适合场景：

- 主要面向中国大陆用户。
- 需要较好的国内地图覆盖、地址检索、逆地理编码和定位能力。
- 希望后续接入 POI 搜索、路线规划等国内 LBS 能力。

优势：

- 国内可用性好。
- Android 地图、定位、搜索等能力体系完整。
- 文档中包含 marker、POI、逆地理编码、权限说明等移动端常用能力。

风险与注意：

- 需要申请高德 Key。
- 需要处理 SDK 隐私合规初始化。
- 需要在隐私政策中说明地图、定位、设备信息等相关数据处理。
- SDK 接入后需关注包体积、初始化时机和权限弹窗。

官方资料要点：

- 高德 Android 地图 SDK 支持地图显示、地图交互、覆盖物、兴趣点搜索、地理编码、离线地图等能力。
- 高德文档提示，从地图 8.1.0 起，调用 SDK 接口前需要处理隐私合规接口，否则可能出现白屏或异常。

### 方案二：Google Maps SDK for Android

适合场景：

- 面向海外用户或需要全球地图覆盖。
- 后续希望接入 Google Maps Platform 的 Places、Routes 等能力。
- 可以接受 Google Cloud 项目、API Key 和 Billing 配置。

优势：

- 全球地图生态成熟。
- 支持 Kotlin 和 Java。
- 支持地图展示、手势交互、marker、polygon、overlay 等能力。

风险与注意：

- 需要 API Key。
- 需要启用 Billing。
- 有价格、用量限制和服务条款要求。
- 在中国大陆网络环境和服务可用性上存在不确定性。

官方资料要点：

- Google Maps SDK for Android 支持在 Android/Wear OS 应用中集成 Google Maps，添加 marker 并处理用户交互。
- Google 官方文档说明接入需要 API key、Billing 和项目配置，并需遵守 Google Maps Platform 服务条款。

### 方案三：MapLibre Native Android + 第三方瓦片服务

适合场景：

- 希望保持开源友好和可迁移性。
- 未来可能自托管地图瓦片或接入商业瓦片服务。
- 不希望与单一商业地图平台深度绑定。

优势：

- MapLibre Native Android 是开源地图渲染方案。
- 可选择不同瓦片源和样式。
- 对开源项目更友好，后续可迁移性较强。

风险与注意：

- MapLibre 主要解决客户端渲染，不等于免费提供稳定瓦片服务。
- 需要额外选择瓦片服务、地址检索和逆地理编码方案。
- 国内地图偏移、瓦片服务可用性和合规问题需要单独评估。
- 自托管瓦片成本较高，不适合 V0.2 早期。

官方资料要点：

- MapLibre Native Android 提供 Android API，包含地图、标注、相机、归因等包。

### 方案四：直接使用 OpenStreetMap 官方瓦片

适合场景：

- 仅用于开发期验证原型。
- 低频内部测试。

优势：

- 接入概念简单。
- 数据开放。

风险与注意：

- 不适合正式产品直接依赖。
- OpenStreetMap 官方瓦片服务由捐赠和赞助支撑，容量有限。
- 官方瓦片服务没有 SLA，重度或不合适使用可能被阻断。
- 若使用 OSM 数据，正式产品应选择商业瓦片服务或自托管服务。

官方资料要点：

- OSM 官方瓦片政策说明 OSM 数据免费可用，但官方瓦片服务器不是无限资源；服务没有 SLA，重度或不当使用可能被阻断。

## 对比结论

| 维度 | 高德地图 | Google Maps | MapLibre + 瓦片服务 | OSM 官方瓦片 |
| --- | --- | --- | --- | --- |
| 国内可用性 | 高 | 不稳定 | 取决于瓦片服务 | 不建议 |
| 海外可用性 | 一般 | 高 | 取决于瓦片服务 | 原型可用 |
| 开源友好 | 一般 | 一般 | 高 | 数据开放但服务不适合重度使用 |
| 接入复杂度 | 中 | 中 | 中到高 | 低 |
| 隐私合规 | 需重点处理 | 需重点处理 | 取决于服务 | 仍需处理请求与归因 |
| V0.2 适配度 | 高 | 中 | 中 | 低 |

## 推荐路线

V0.2 推荐优先选择：

```text
高德地图 Android SDK
```

推荐原因：

- 当前用户和开发环境更偏中国大陆。
- 当前需求是地图展示、marker、选点、定位和地址反查，高德能力覆盖较完整。
- 可以先完成“人生地图”的核心体验，再在后续版本评估 MapLibre 或海外地图方案。

同时保留抽象边界：

```text
UI 层不要直接依赖具体 SDK 数据结构
```

建议封装：

- `MapPoint`
- `MapPickerResult`
- `MapProvider`
- `LocationProvider`
- `GeocodingProvider`

这样后续如果从高德迁移到 MapLibre 或 Google Maps，业务层不需要大面积重写。

## V0.2 实施拆分

建议按以下独立模块推进：

1. 地图 SDK 接入准备
   - 申请 Key
   - 配置 Gradle 依赖
   - 配置 Manifest
   - 补充隐私合规说明

2. 地图页真实地图展示
   - 替换当前点位列表占位
   - 加载基础地图
   - 保留点位列表作为备选信息

3. 记录 marker 展示
   - 从 Room 读取带坐标记录
   - 在地图上显示 marker
   - 点击 marker 展示记录摘要

4. 地图选点
   - 新增记录页进入地图选点
   - 编辑记录页进入地图选点
   - 返回经纬度和地点名称

5. 当前位置
   - 增加定位权限申请
   - 增加权限说明
   - 支持使用当前位置填充坐标

6. 地址反查
   - 根据经纬度获取地点描述
   - 自动填充地点名称

## 暂不做的内容

V0.2 早期暂不做：

- 路线规划
- 导航
- 轨迹记录
- 离线地图
- 海量点聚合
- 自托管地图瓦片

## 参考资料

- 高德地图 Android 地图 SDK 概述：https://lbs.amap.com/api/android-sdk/summary/
- Google Maps SDK for Android Overview：https://developers.google.com/maps/documentation/android-sdk/overview
- MapLibre Native Android API：https://maplibre.org/maplibre-native/android/api/
- OpenStreetMap Tile Usage Policy：https://operations.osmfoundation.org/policies/tiles/
