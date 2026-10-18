#!/usr/bin/env bash
# Start the Vault container, initialise it on first run, and unseal it.
#
# Usage: start.sh [--configure]
#   --configure   also run configure-secrets.sh for every service afterwards
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

configure=false
case "${1:-}" in
  --configure) configure=true ;;
  "") ;;
  *) die "unknown argument '$1' (usage: start.sh [--configure])" ;;
esac

load_config

docker compose \
  -p vault_service_chatter \
  --project-directory "$VAULT_DIR" \
  -f "$VAULT_DIR/docker-compose.yml" \
  up -d

wait_for_vault

# First run: generate the unseal key and root token and keep them in a gitignored file.
if ! vault_is_initialized; then
  [[ ! -e $INIT_FILE ]] || die "Vault is not initialised but $INIT_FILE exists; delete it or restore data/"
  raw="$INIT_FILE.raw"
  (umask 077; vault operator init -key-shares=1 -key-threshold=1 -format=json > "$raw")
  unseal_key="$(tr -d '\n' < "$raw" | sed -n 's/.*"unseal_keys_b64": *\[ *"\([^"]*\)".*/\1/p')"
  root_token="$(sed -n 's/.*"root_token": *"\([^"]*\)".*/\1/p' "$raw")"
  if [[ -z $unseal_key || -z $root_token ]]; then
    die "Could not parse the init output; the raw output is kept in $raw"
  fi
  (umask 077; printf 'VAULT_UNSEAL_KEY=%s\nVAULT_TOKEN=%s\n' "$unseal_key" "$root_token" > "$INIT_FILE")
  rm -f "$raw"
  export VAULT_UNSEAL_KEY="$unseal_key" VAULT_TOKEN="$root_token"
  log "Initialised Vault; unseal key and root token saved to $INIT_FILE"
fi

if vault_is_sealed; then
  [[ -n ${VAULT_UNSEAL_KEY:-} ]] || die "Vault is sealed and VAULT_UNSEAL_KEY is not set (.env or $INIT_FILE)"
  unseal_vault "$VAULT_UNSEAL_KEY"
fi

require_unsealed_vault
log "Vault is running, unsealed, and authenticated at $VAULT_ADDR."

if $configure; then
  "$(dirname "${BASH_SOURCE[0]}")/configure-secrets.sh"
fi
