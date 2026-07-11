# 岁迹认证后端

这是给国内用户准备的轻量认证服务，用来替代 Firebase 作为默认登录注册链路。

## 当前接口

- `GET /health`：健康检查，返回服务状态和 SMTP 配置状态。
- `POST /api/auth/email/code/request`：发送邮箱验证码。
- `POST /api/auth/register`：邮箱验证码 + 密码注册。
- `POST /api/auth/login`：邮箱密码登录。
- `POST /api/auth/login/code`：邮箱验证码登录。
- `POST /api/auth/token/refresh`：使用刷新凭证续期登录态。
- `POST /api/auth/email/verification/request`：登录后重新发送邮箱验证码。
- `POST /api/auth/email/change/code/request`：发送修改登录邮箱验证码。
- `POST /api/auth/email/change/confirm`：验证码 + 当前密码确认修改邮箱。
- `POST /api/auth/password/reset/request`：发送密码重置验证码。
- `POST /api/auth/password/reset/confirm`：验证码确认并重置密码。
- `POST /api/auth/account/delete`：当前密码确认删除云端账号和轻量备份。
- `POST /api/profile/get`：读取账号名称、签名和压缩头像。
- `POST /api/profile/update`：更新账号名称、签名和压缩头像。
- `POST /api/sync/export/upload`：登录后手动上传轻量云备份。
- `POST /api/sync/export/download`：登录后读取最近一次轻量云备份。

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
npm ci --omit=dev
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
- Access Token 有效期 7 天，Refresh Token 有效期 30 天。
- 修改邮箱、重置密码会提升账号登录版本，旧 Token 自动失效。
- 修改登录邮箱和删除云端账号都需要当前密码二次确认。
- 审计日志写入 `AUDIT_FILE`，默认 `./data/audit.log`。

## 数据存储

- 当前阶段使用 `DATA_FILE` 指向的 JSON 文件保存账号、登录邮箱和轻量云备份。
- 写入采用串行队列和临时文件替换，减少并发写入覆盖风险。
- 每次写入前会保留一份 `${DATA_FILE}.bak`，便于服务器异常时人工恢复。
- 轻量云备份只保存结构化记录、标签、地点和照片引用，不保存照片原文件。

## 后续升级

- 用 MySQL/PostgreSQL 替代 JSON 文件存储。
- 云同步数据表、媒体上传、冲突合并、软删除和设备管理。
