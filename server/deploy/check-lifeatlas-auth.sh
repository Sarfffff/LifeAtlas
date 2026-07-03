#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-https://api.lifeatlas.cn/health}"
SERVICE_NAME="${SERVICE_NAME:-lifeatlas-auth}"

echo "==> Public health"
curl -fsS "$HEALTH_URL"
echo

if command -v systemctl >/dev/null 2>&1; then
  echo "==> Service status"
  sudo systemctl --no-pager --full status "$SERVICE_NAME" || true
fi

if command -v journalctl >/dev/null 2>&1; then
  echo "==> Recent logs"
  sudo journalctl -u "$SERVICE_NAME" -n 80 --no-pager || true
fi
