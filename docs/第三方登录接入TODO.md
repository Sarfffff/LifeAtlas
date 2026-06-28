# 第三方登录接入 TODO

记录日期：2026-06-28

## 本轮已完成

- 登录页新增“其他方式”区域，整合 QQ 与微信入口，默认不打扰邮箱验证码/密码登录主流程。
- App 构建配置新增：
  - `lifeatlas.auth.wechatAppId`
  - `lifeatlas.auth.qqAppId`
- 认证仓库新增 QQ/微信配置状态判断。
- 后端客户端预留 OAuth 交换接口：
  - `POST /api/auth/oauth/qq`
  - `POST /api/auth/oauth/wechat`
- 未配置 AppID 或后端 OAuth 未完成时，点击 QQ/微信会给出明确提示，不会卡在处理中。

## 后续需要你准备

- QQ 登录：到 QQ 互联申请移动应用，取得 AppID、AppKey，并配置 Android 包名与签名。
- 微信登录：到微信开放平台申请移动应用，取得 AppID、AppSecret，并配置 Android 包名、签名和回调。
- 本地配置示例：

```properties
lifeatlas.auth.qqAppId=你的QQ互联AppID
lifeatlas.auth.wechatAppId=你的微信开放平台AppID
```

## 后续开发任务

- 接入 QQ 官方 Android SDK，拿到授权 `code` 或 `accessToken`。
- 接入微信官方 Android SDK，拿到授权 `code`。
- 服务端实现 `/api/auth/oauth/qq` 和 `/api/auth/oauth/wechat`：
  - 使用授权码向平台换取 openid/unionid。
  - 若用户不存在则自动创建岁迹账号。
  - 返回岁迹自己的 `accessToken` / `refreshToken`。
- 增加第三方账号绑定/解绑页面。
- 增加同一邮箱、QQ、微信账号合并策略，避免重复账号。
