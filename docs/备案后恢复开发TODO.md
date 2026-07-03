# 备案后恢复开发 TODO

记录日期：2026-07-04

## 当前状态

- `lifeatlas.cn` 备案已通过。
- `https://api.lifeatlas.cn/health` 已从本机验证可访问，返回 `HTTP 200`。
- App 本地配置已指向国内后端：

```properties
lifeatlas.auth.provider=backend
lifeatlas.auth.baseUrl=https://api.lifeatlas.cn
```

## 本轮已完成

- 后端 `/health` 增加 `mailConfigured` 字段，用于确认 SMTP 是否配置完整。
- 后端验证码仍只保存哈希，不保存明文。
- 后端验证码 10 分钟过期，最多尝试 5 次。
- 后端增加 IP 维度频率限制：
  - 验证码发送
  - 注册
  - 密码登录
  - 验证码登录
  - 忘记密码
- 后端增加登录失败锁定：
  - 同一邮箱 + IP 连续失败 5 次后冷却 10 分钟。
- 后端增加审计日志：
  - 验证码发送
  - 注册
  - 登录成功/失败
  - 重置密码请求
  - 重置密码确认
  - 第三方登录请求
- 后端预留 QQ/微信 OAuth 接口：
  - `POST /api/auth/oauth/qq`
  - `POST /api/auth/oauth/wechat`
- App 端连接重置提示已更新，不再提示“可能未备案”，改为检查 HTTPS、Nginx、防火墙和服务状态。

## 服务器部署操作

在阿里云服务器执行：

```bash
cd /opt/lifeatlas/LifeAtlas
git pull
cd server
npm install --omit=dev
sudo systemctl restart lifeatlas-auth
sudo systemctl status lifeatlas-auth
curl https://api.lifeatlas.cn/health
```

如果 `curl` 返回包含：

```json
{"ok":true,"service":"LifeAtlas Auth Server","mailConfigured":true}
```

说明新版后端和 SMTP 都已经生效。

如果 `mailConfigured:false`，检查：

```bash
sudo nano /opt/lifeatlas/LifeAtlas/server/.env
sudo systemctl restart lifeatlas-auth
journalctl -u lifeatlas-auth -f
```

服务器 `.env` 至少需要：

```env
PORT=8080
APP_BASE_URL=https://api.lifeatlas.cn
JWT_SECRET=一串足够长的随机密钥
DATA_FILE=./data/users.json
AUDIT_FILE=./data/audit.log
SMTP_HOST=smtpdm.aliyun.com
SMTP_PORT=465
SMTP_SECURE=true
SMTP_USER=no-reply@mail.lifeatlas.cn
SMTP_PASS=阿里云邮件推送SMTP密码
SMTP_FROM="岁迹 <no-reply@mail.lifeatlas.cn>"
```

## 仍需人工确认

- 阿里云邮件推送发信地址 `no-reply@mail.lifeatlas.cn` 是否已启用。
- 阿里云邮件推送 SMTP 密码是否已写入服务器 `.env`。
- 用真实 QQ 邮箱测试：
  - 注册验证码
  - 邮箱验证码登录
  - 忘记密码验证码
  - 垃圾箱/延迟投递情况

## 延后 TODO

- QQ/微信真实登录：等待开放平台 AppID/AppSecret、Android 包名和签名审核通过。
- 云同步：等待账号体系稳定后设计云端数据表、媒体上传、冲突合并和软删除策略。
- 发布上线：准备隐私政策正式版、用户协议、Release 签名、应用市场截图和介绍文案。
