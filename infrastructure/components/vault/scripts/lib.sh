#!/usr/bin/env bash
# Shared helpers for the Vault scripts. Source it; do not execute it.
#
# Secrets never appear on a command line: values are read from the environment,
# assembled into JSON by bash builtins and piped to `vault ... -` on stdin.

VAULT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="$(git -C "$VAULT_DIR" rev-parse --show-toplevel)"
ASSETS_DIR="$VAULT_DIR/assets"
INIT_FILE="$VAULT_DIR/.vault-init"
VAULT_CONTAINER="${VAULT_CONTAINER:-vault-chatter}"

log() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- env file

# Resolution order: $VAULT_ENV_FILE, this folder's .env, the main checkout's copy of it,
# then a legacy repository-root .env. Git worktrees do not share untracked files, so the
# main checkout is a fallback.
find_env_file() {
  local common_dir main_root candidate vault_rel
  if [[ -n ${VAULT_ENV_FILE:-} ]]; then
    printf '%s\n' "$VAULT_ENV_FILE"
    return 0
  fi
  common_dir="$(git -C "$ROOT_DIR" rev-parse --path-format=absolute --git-common-dir 2>/dev/null || true)"
  main_root="${common_dir%/.git}"
  vault_rel="${VAULT_DIR#"$ROOT_DIR"/}"
  for candidate in "$VAULT_DIR/.env" "${main_root:+$main_root/$vault_rel/.env}" "$ROOT_DIR/.env"; do
    if [[ -n $candidate && -f $candidate ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

# Parse KEY=VALUE lines without executing them. Values may contain spaces, '$' and '#';
# wrap them in single or double quotes to keep leading/trailing whitespace.
# Variables already present in the environment win over the file.
load_env_file() {
  local file=$1 line key value
  [[ -r $file ]] || die "Cannot read $file"
  while IFS= read -r line || [[ -n $line ]]; do
    line="${line%$'\r'}"
    line="${line#"${line%%[![:space:]]*}"}"
    [[ -z $line || $line == \#* ]] && continue
    line="${line#export }"
    [[ $line == *=* ]] || continue
    key="${line%%=*}"
    value="${line#*=}"
    key="${key%"${key##*[![:space:]]}"}"
    [[ $key =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    if [[ $value =~ ^\"(.*)\"[[:space:]]*$ || $value =~ ^\'(.*)\'[[:space:]]*$ ]]; then
      value="${BASH_REMATCH[1]}"
    else
      value="${value%"${value##*[![:space:]]}"}"
    fi
    [[ -n ${!key+x} ]] && continue
    export "$key=$value"
  done < "$file"
}

load_config() {
  local env_file
  if env_file="$(find_env_file)"; then
    load_env_file "$env_file"
    log "Loaded $env_file"
  else
    log "No .env found (set VAULT_ENV_FILE to use another path); using the process environment only"
  fi
  if [[ -f $INIT_FILE ]]; then
    load_env_file "$INIT_FILE"
  fi
  export VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:5002}"
}

# Variable names the template (.env.example) declares for a service prefix.
manifest_vars() {
  grep -E "^${1}_[A-Z0-9_]+=" "$VAULT_DIR/.env.example" | cut -d= -f1 || true
}

# ------------------------------------------------------------ input checks

MISSING=()
need_var() { [[ -n ${!1:-} ]] || MISSING+=("variable $1"); }
need_file() { [[ -f $1 ]] || MISSING+=("file $1"); }
fail_if_missing() {
  if (( ${#MISSING[@]} > 0 )); then
    printf 'error: missing required input (see .env.example):\n' >&2
    printf '  - %s\n' "${MISSING[@]}" >&2
    exit 1
  fi
}

# -------------------------------------------------------------------- JSON

json_str() {
  local s=$1
  s=${s//\\/\\\\}
  s=${s//\"/\\\"}
  s=${s//$'\n'/\\n}
  s=${s//$'\r'/\\r}
  s=${s//$'\t'/\\t}
  printf '"%s"' "$s"
}

# json_obj key=value ...   string values
#          key:=literal    raw JSON (numbers, booleans)
json_obj() {
  local out='{' sep='' arg
  for arg in "$@"; do
    if [[ $arg =~ ^([A-Za-z0-9_.-]+):=(.*)$ ]]; then
      out+="$sep\"${BASH_REMATCH[1]}\":${BASH_REMATCH[2]}"
    elif [[ $arg =~ ^([A-Za-z0-9_.-]+)=(.*)$ ]]; then
      out+="$sep\"${BASH_REMATCH[1]}\":$(json_str "${BASH_REMATCH[2]}")"
    else
      die "json_obj: bad argument '${arg%%=*}'"
    fi
    sep=,
  done
  printf '%s}' "$out"
}

# ------------------------------------------------------------------- vault

# Use the host CLI when installed, otherwise the one inside the Vault container.
vault() {
  if type -P vault >/dev/null 2>&1; then
    command vault "$@"
  else
    docker exec -i -e VAULT_TOKEN "$VAULT_CONTAINER" vault "$@"
  fi
}

vault_status() { vault status -format=json 2>/dev/null || true; }

wait_for_vault() {
  local i
  for ((i = 0; i < 60; i++)); do
    if [[ $(vault_status) == *'"initialized"'* ]]; then
      return 0
    fi
    sleep 1
  done
  die "Vault did not become reachable (container $VAULT_CONTAINER, address $VAULT_ADDR)"
}

vault_is_initialized() { [[ $(vault_status) =~ \"initialized\":\ *true ]]; }
vault_is_sealed() { [[ $(vault_status) =~ \"sealed\":\ *true ]]; }

# The CLI only accepts the unseal key as an argument, which would expose it in the process
# list; the HTTP API takes it in the request body instead.
unseal_vault() {
  local key=$1
  type -P curl >/dev/null || die "curl is required to unseal Vault without exposing the key"
  json_obj "key=$key" \
    | curl -fsS -X PUT --data-binary @- "$VAULT_ADDR/v1/sys/unseal" >/dev/null \
    || die "Unsealing failed; check VAULT_UNSEAL_KEY"
}

require_unsealed_vault() {
  wait_for_vault
  vault_is_initialized || die "Vault is not initialised; run scripts/start.sh first"
  ! vault_is_sealed || die "Vault is sealed; run scripts/start.sh first"
  [[ -n ${VAULT_TOKEN:-} ]] || die "VAULT_TOKEN is not set (.env or $INIT_FILE)"
  vault token lookup >/dev/null || die "VAULT_TOKEN was rejected by Vault"
}

ensure_engine() {
  local path=$1 type=$2
  if vault secrets list -format=json | grep -q "\"$path/\":"; then
    return 0
  fi
  vault secrets enable -path="$path" "$type" >/dev/null
  log "enabled $type engine at $path/"
}

# vault_write <path> <json>
vault_write() { printf '%s' "$2" | vault write "$1" - >/dev/null; }

# kv_upsert <path under secret/> <json>: patch when the secret exists, create it otherwise.
kv_upsert() {
  if vault kv get -mount=secret "$1" >/dev/null 2>&1; then
    printf '%s' "$2" | vault kv patch -mount=secret "$1" - >/dev/null
  else
    printf '%s' "$2" | vault kv put -mount=secret "$1" - >/dev/null
  fi
}
