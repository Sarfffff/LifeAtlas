# 岁迹认证后端

这是给国内用户准备的轻量认证服务雏形，用来替代 Firebase 作为默认登录注册链路。

## 当前接口

- `POST /api/auth/register`：邮箱密码注册，注册后发送验证邮件。
- `POST /api/auth/login`：邮箱密码登录，返回 `accessToken`。
- `POST /api/auth/email/verification/request`：重新发送验证邮件，需要 `Bearer accessToken`。
- `GET /api/auth/email/verify`：邮箱验证落地页。
- `POST /api/auth/password/reset/request`：发送密码重置邮件预留接口。
- `GET /health`：健康检查。

## 本地运行

```bash
cp .env.example .env
npm install
npm start
```

## App 配置

在 Android 项目的 `local.properties` 中写入：

```properties
lifeatlas.auth.provider=backend
lifeatlas.auth.baseUrl=https://api.lifeatlas.cn
```

如果暂时不填，App 会继续使用本地账号，不会访问 Firebase。

## 阿里云服务器部署

当前推荐服务器：

- Ubuntu 22.04
- 公网 IP：`47.122.124.84`
- 后端监听：`127.0.0.1:8080`
- 对外域名：`https://api.lifeatlas.cn`

部署模板：

- `deploy/nginx-lifeatlas.conf.example`
- `deploy/lifeatlas-auth.service.example`

具体步骤见项目文档《阿里云服务器部署清单》。

## 后续升级点

- 将 `data/users.json` 替换为 MySQL 或 PostgreSQL。
- 增加邮箱验证码有效期和重试冷却。
- 增加密码重置页面。
- 增加阿里云短信验证码接口。
- 增加云同步数据表、媒体文件上传和冲突合并策略。
