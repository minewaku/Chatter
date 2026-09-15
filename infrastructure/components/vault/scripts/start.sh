#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

set -a
source "$ROOT_DIR/.env"
set +a

: "${VAULT_ADDR:?Missing VAULT_ADDR}"
: "${VAULT_UNSEAL_KEY:?Missing VAULT_UNSEAL_KEY}"
: "${VAULT_TOKEN:?Missing VAULT_TOKEN}"

docker compose \
  -p vault_service_chatter \
  --project-directory "$ROOT_DIR/infrastructure/components/vault" \
  -f "$ROOT_DIR/infrastructure/components/vault/docker-compose.yml" \
  up -d

vault operator unseal "$VAULT_UNSEAL_KEY"

vault token lookup > /dev/null

echo "Vault is running, unsealed, and authenticated."