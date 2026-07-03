# 岁迹认证后端

这是给国内用户准备的轻量认证服务，用来替代 Firebase 作为默认登录注册链路。

## 当前接口

- `GET /health`：健康检查，返回服务状态和 SMTP 配置状态。
- `POST /api/auth/email/code/request`：发送邮箱验证码。
- `POST /api/auth/register`：邮箱验证码 + 密码注册。
- `POST /api/auth/login`：邮箱密码登录。
- `POST /api/auth/login/code`：邮箱验证码登录。
- `POST /api/auth/email/verification/request`：登录后重新发送邮箱验证码。
- `POST /api/auth/password/reset/request`：发送密码重置验证码。
- `POST /api/auth/password/reset/confirm`：验证码确认并重置密码。
- `POST /api/auth/oauth/qq`：QQ 登录服务端接口占位。
- `POST /api/auth/oauth/wechat`：微信登录服务端接口占位。

## 本地运行

```bash
cp .env.example .env
npm install
npm start
```

## App 配置

Android 项目的 `local.properties`：

```properties
lifeatlas.auth.provider=backend
lifeatlas.auth.baseUrl=https://api.lifeatlas.cn
```

## 阿里云服务器部署

推荐部署路径：

```bash
/opt/lifeatlas/LifeAtlas/server
```

部署/更新：

```bash
cd /opt/lifeatlas/LifeAtlas
git pull
cd server
npm install --omit=dev
sudo systemctl restart lifeatlas-auth
sudo systemctl status lifeatlas-auth
curl https://api.lifeatlas.cn/health
```

## SMTP 配置

服务器 `.env`：

```env
SMTP_HOST=smtpdm.aliyun.com
SMTP_PORT=465
SMTP_SECURE=true
SMTP_USER=no-reply@mail.lifeatlas.cn
SMTP_PASS=阿里云邮件推送SMTP密码
SMTP_FROM="岁迹 <no-reply@mail.lifeatlas.cn>"
```

健康检查中 `mailConfigured:true` 表示 SMTP 配置字段完整，不代表一定投递成功；仍需用真实邮箱收信测试。

## 安全策略

- 验证码只保存哈希，不保存明文。
- 验证码 10 分钟过期。
- 验证码最多尝试 5 次。
- 同一邮箱同一用途 1 分钟只能发送 1 次验证码。
- 同一邮箱同一用途 1 小时最多发送 6 次验证码。
- IP 维度限制注册、登录、验证码发送和密码重置频率。
- 同一邮箱 + IP 连续登录失败 5 次后冷却 10 分钟。
- 审计日志写入 `AUDIT_FILE`，默认 `./data/audit.log`。

## 后续升级

- 用 MySQL/PostgreSQL 替代 JSON 文件存储。
- 第三方登录正式接入 QQ/微信官方 SDK 与服务端 openid/unionid 交换。
- 云同步数据表、媒体上传、冲突合并和软删除。
