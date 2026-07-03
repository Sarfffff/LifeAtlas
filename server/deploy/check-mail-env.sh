#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/lifeatlas/LifeAtlas/server}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"

echo "==> Checking mail env file: $ENV_FILE"

if [ ! -f "$ENV_FILE" ]; then
  echo "MISSING_ENV_FILE=$ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

missing=0

check_required() {
  local name="$1"
  local value="${!name:-}"
  if [ -z "$value" ]; then
    echo "MISSING=$name"
    missing=1
  elif [[ "$value" == replace-with* ]] || [[ "$value" == "你的"* ]]; then
    echo "PLACEHOLDER=$name"
    missing=1
  else
    if [ "$name" = "SMTP_PASS" ]; then
      echo "OK=$name(length:${#value})"
    else
      echo "OK=$name=$value"
    fi
  fi
}

check_required SMTP_HOST
check_required SMTP_PORT
check_required SMTP_SECURE
check_required SMTP_USER
check_required SMTP_PASS
check_required SMTP_FROM

if [ "$missing" -ne 0 ]; then
  echo "RESULT=mailConfigured:false"
  exit 1
fi

echo "RESULT=mailConfigured:true"
