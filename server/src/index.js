import cors from "cors";
import crypto from "node:crypto";
import dotenv from "dotenv";
import express from "express";
import fs from "node:fs/promises";
import jwt from "jsonwebtoken";
import nodemailer from "nodemailer";
import path from "node:path";

dotenv.config();

const app = express();
const port = Number(process.env.PORT || 8080);
const jwtSecret = process.env.JWT_SECRET || "lifeatlas-dev-secret-change-me";
const dataFile = process.env.DATA_FILE || "./data/users.json";
const appBaseUrl = (process.env.APP_BASE_URL || `http://localhost:${port}`).replace(/\/$/, "");
const registerLimit = new Map();

app.use(cors());
app.use(express.json({ limit: "512kb" }));

app.get("/health", (_request, response) => {
  response.json({ ok: true, service: "LifeAtlas Auth Server" });
});

app.post("/api/auth/register", async (request, response) => {
  try {
    const { email, password } = parseEmailPassword(request.body);
    enforceRegisterLimit(email);

    const store = await readStore();
    if (store.users[email]) {
      return response.status(409).json({ message: "该邮箱已注册，请直接登录" });
    }

    const passwordSalt = randomToken(16);
    const user = {
      email,
      passwordSalt,
      passwordHash: hashPassword(password, passwordSalt),
      emailVerified: false,
      verificationToken: randomToken(24),
      resetToken: null,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    store.users[email] = user;
    await writeStore(store);
    const mailResult = await trySendVerificationEmail(user);

    response.json(toSession(user, mailResult));
  } catch (error) {
    sendError(response, error);
  }
});

app.post("/api/auth/login", async (request, response) => {
  try {
    const { email, password } = parseEmailPassword(request.body);
    const store = await readStore();
    const user = store.users[email];
    if (!user || user.passwordHash !== hashPassword(password, user.passwordSalt)) {
      return response.status(401).json({ message: "邮箱或密码错误" });
    }
    response.json(toSession(user));
  } catch (error) {
    sendError(response, error);
  }
});

app.post("/api/auth/email/verification/request", async (request, response) => {
  try {
    const user = await requireUser(request);
    user.verificationToken = randomToken(24);
    user.updatedAt = Date.now();
    const store = await readStore();
    store.users[user.email] = user;
    await writeStore(store);
    const mailResult = await trySendVerificationEmail(user);
    response.json({
      ok: true,
      emailSent: mailResult.sent,
      message: mailResult.message
    });
  } catch (error) {
    sendError(response, error);
  }
});

app.get("/api/auth/email/verify", async (request, response) => {
  const email = normalizeEmail(String(request.query.email || ""));
  const token = String(request.query.token || "");
  const store = await readStore();
  const user = store.users[email];
  if (!user || user.verificationToken !== token) {
    return response.status(400).send("验证链接无效或已过期");
  }
  user.emailVerified = true;
  user.verificationToken = null;
  user.updatedAt = Date.now();
  await writeStore(store);
  response.send("岁迹邮箱验证成功，可以回到 App 继续使用。");
});

app.post("/api/auth/password/reset/request", async (request, response) => {
  try {
    const email = normalizeEmail(request.body?.email);
    validateEmail(email);
    const store = await readStore();
    const user = store.users[email];
    if (user) {
      user.resetToken = randomToken(24);
      user.updatedAt = Date.now();
      await writeStore(store);
      const mailResult = await trySendPasswordResetEmail(user);
      return response.json({
        ok: true,
        emailSent: mailResult.sent,
        message: mailResult.message
      });
    }
    response.json({ ok: true, emailSent: true, message: "如果邮箱已注册，重置邮件会发送到该邮箱" });
  } catch (error) {
    sendError(response, error);
  }
});

async function requireUser(request) {
  const authorization = request.headers.authorization || "";
  const token = authorization.startsWith("Bearer ") ? authorization.slice(7) : "";
  if (!token) {
    const error = new Error("请先登录账号");
    error.status = 401;
    throw error;
  }
  const payload = jwt.verify(token, jwtSecret);
  const store = await readStore();
  const user = store.users[payload.email];
  if (!user) {
    const error = new Error("账号不存在");
    error.status = 401;
    throw error;
  }
  return user;
}

function toSession(user, mailResult = null) {
  return {
    accessToken: jwt.sign({ email: user.email }, jwtSecret, { expiresIn: "7d" }),
    refreshToken: jwt.sign({ email: user.email, type: "refresh" }, jwtSecret, { expiresIn: "30d" }),
    email: user.email,
    emailVerified: Boolean(user.emailVerified),
    verificationEmailSent: mailResult?.sent ?? null,
    notice: mailResult?.message ?? null
  };
}

async function trySendVerificationEmail(user) {
  return trySendMail(() => sendVerificationEmail(user), "验证邮件已发送，请检查收件箱或垃圾邮件");
}

async function trySendPasswordResetEmail(user) {
  return trySendMail(() => sendPasswordResetEmail(user), "密码重置邮件已发送，请检查收件箱或垃圾邮件");
}

async function trySendMail(operation, successMessage) {
  try {
    ensureSmtpConfigured();
    await operation();
    return { sent: true, message: successMessage };
  } catch (error) {
    console.error("Mail delivery failed:", error.message);
    return {
      sent: false,
      message: "账号操作已完成，但邮件服务暂未配置成功。请稍后在账号与安全页重新发送邮件。"
    };
  }
}

function ensureSmtpConfigured() {
  const required = ["SMTP_HOST", "SMTP_USER", "SMTP_PASS"];
  const missing = required.filter((key) => !process.env[key] || process.env[key].startsWith("replace-with"));
  if (missing.length > 0) {
    const error = new Error(`SMTP is not configured: ${missing.join(", ")}`);
    error.status = 503;
    throw error;
  }
}

async function sendVerificationEmail(user) {
  const link = `${appBaseUrl}/api/auth/email/verify?email=${encodeURIComponent(user.email)}&token=${encodeURIComponent(user.verificationToken)}`;
  await sendMail({
    to: user.email,
    subject: "验证你的岁迹邮箱",
    text: `欢迎使用岁迹。请打开下面的链接完成邮箱验证：\n${link}`,
    html: `<p>欢迎使用岁迹。</p><p><a href="${link}">点击完成邮箱验证</a></p>`
  });
}

async function sendPasswordResetEmail(user) {
  await sendMail({
    to: user.email,
    subject: "岁迹密码重置请求",
    text: `你发起了密码重置请求。当前版本先记录请求，后续会接入重置页面。令牌：${user.resetToken}`,
    html: `<p>你发起了密码重置请求。</p><p>当前版本先记录请求，后续会接入重置页面。</p>`
  });
}

async function sendMail(message) {
  const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 465),
    secure: String(process.env.SMTP_SECURE || "true") === "true",
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS
    }
  });

  await transporter.sendMail({
    from: process.env.SMTP_FROM || process.env.SMTP_USER,
    ...message
  });
}

function parseEmailPassword(body) {
  const email = normalizeEmail(body?.email);
  const password = String(body?.password || "");
  validateEmail(email);
  if (password.length < 8 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    const error = new Error("密码至少 8 位，并同时包含字母和数字");
    error.status = 400;
    throw error;
  }
  return { email, password };
}

function validateEmail(email) {
  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    const error = new Error("请输入有效邮箱");
    error.status = 400;
    throw error;
  }
}

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function enforceRegisterLimit(email) {
  const now = Date.now();
  const key = `${email}:${Math.floor(now / 3600000)}`;
  const count = registerLimit.get(key) || 0;
  if (count >= 3) {
    const error = new Error("注册操作过于频繁，请稍后再试");
    error.status = 429;
    throw error;
  }
  registerLimit.set(key, count + 1);
}

function hashPassword(password, salt) {
  return crypto.scryptSync(password, salt, 64).toString("hex");
}

function randomToken(bytes) {
  return crypto.randomBytes(bytes).toString("hex");
}

async function readStore() {
  try {
    const raw = await fs.readFile(dataFile, "utf8");
    return JSON.parse(raw);
  } catch {
    return { users: {} };
  }
}

async function writeStore(store) {
  await fs.mkdir(path.dirname(dataFile), { recursive: true });
  await fs.writeFile(dataFile, JSON.stringify(store, null, 2));
}

function sendError(response, error) {
  response.status(error.status || 500).json({
    message: error.message || "服务暂时不可用"
  });
}

app.listen(port, () => {
  console.log(`LifeAtlas auth server listening on ${port}`);
});
