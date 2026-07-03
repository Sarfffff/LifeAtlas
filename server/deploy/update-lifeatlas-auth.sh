#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/lifeatlas/LifeAtlas}"
SERVICE_NAME="${SERVICE_NAME:-lifeatlas-auth}"
HEALTH_URL="${HEALTH_URL:-https://api.lifeatlas.cn/health}"

echo "==> Updating LifeAtlas repository"
cd "$APP_DIR"
git pull

echo "==> Installing production dependencies"
cd "$APP_DIR/server"
npm install --omit=dev
npm run check

echo "==> Restarting systemd service: $SERVICE_NAME"
sudo systemctl restart "$SERVICE_NAME"
sleep 2
sudo systemctl --no-pager --full status "$SERVICE_NAME"

echo "==> Checking health endpoint: $HEALTH_URL"
curl -fsS "$HEALTH_URL"
echo
echo "==> Done"
