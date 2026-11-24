#!/usr/bin/env bash
# Push secrets from .env into Vault.
#
# Usage: configure-secrets.sh [--no-db] [--no-scylla] [service ...]
#   services:   identityaccess profile apigateway message jwt storage   (default: all)
#   --no-db     only write KV secrets and JWT keys; skip the database engine config
#               (use it while the databases are down or the *_VAULT_* credentials are placeholders)
#   --no-scylla skip the ScyllaDB config of the message service only
#
# All services share the PostgreSQL instance of infrastructure/components/postgresql: each gets its
# own Vault database config and role, built from its prefixed <SERVICE>_DB_VAULT_* credentials.
# Override the target with VAULT_POSTGRES_HOST / VAULT_POSTGRES_PORT / VAULT_POSTGRES_DB.
#
# .env.example is the manifest: every variable it lists for a service must be set.
# Variables named <SERVICE>_<GROUP>_<FIELD> become key <group>.<field> in secret/<service>;
# *_DB_VAULT_* and *_SCYLLA_VAULT_* configure the database engine instead.
# 'storage' is shared by all services: STORAGE_ACCOUNT_ID / _BUCKET / _ACCESS_KEY / _SECRET_KEY go to secret/common/storage.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

ALL_SERVICES=(identityaccess profile apigateway message jwt storage)
declare -A HAS_POSTGRES=([identityaccess]=1 [profile]=1 [message]=1)
declare -A HAS_SCYLLA=([message]=1)
POSTGRES_HOST="${VAULT_POSTGRES_HOST:-postgresql-chatter}"
POSTGRES_PORT="${VAULT_POSTGRES_PORT:-5432}"
POSTGRES_DB="${VAULT_POSTGRES_DB:-chatter}"

JWT_PRIVATE_KEY="$ASSETS_DIR/keys/private-key.pem"
JWT_PUBLIC_KEY="$ASSETS_DIR/keys/public-key.pem"

services=()
with_db=true
with_scylla=true
for arg in "$@"; do
  case $arg in
    --no-db) with_db=false ;;
    --no-scylla) with_scylla=false ;;
    -*) die "unknown option '$arg'" ;;
    *) services+=("$arg") ;;
  esac
done
(( ${#services[@]} > 0 )) || services=("${ALL_SERVICES[@]}")

load_config

# ---- validate everything before writing anything
for svc in "${services[@]}"; do
  if [[ $svc == jwt ]]; then
    need_file "$JWT_PRIVATE_KEY"
    need_file "$JWT_PUBLIC_KEY"
    continue
  fi
  [[ " ${ALL_SERVICES[*]} " == *" $svc "* ]] || die "unknown service '$svc' (known: ${ALL_SERVICES[*]})"
  mapfile -t vars < <(manifest_vars "${svc^^}")
  (( ${#vars[@]} > 0 )) || die "no ${svc^^}_* variables in .env.example"
  for v in "${vars[@]}"; do
    if ! $with_db && [[ $v == *_DB_VAULT_* || $v == *_SCYLLA_VAULT_* ]]; then continue; fi
    if ! $with_scylla && [[ $v == *_SCYLLA_VAULT_* ]]; then continue; fi
    need_var "$v"
  done
  if ! $with_db; then continue; fi
  if [[ -n ${HAS_POSTGRES[$svc]:-} ]]; then
    need_file "$ASSETS_DIR/sql/postgresql-role-orm.sql"
    need_file "$ASSETS_DIR/sql/postgresql-revoke-orm.sql"
    need_file "$ASSETS_DIR/sql/postgresql-renew-orm.sql"
  fi
  if $with_scylla && [[ -n ${HAS_SCYLLA[$svc]:-} ]]; then
    need_file "$ASSETS_DIR/cql/cassandra-role-orm.cql"
    need_file "$ASSETS_DIR/cql/cassandra-revoke-orm.cql"
  fi
done
fail_if_missing

require_unsealed_vault
ensure_engine secret kv-v2
ensure_engine database database

# ---- writers

configure_kv() {
  local svc=$1 prefix=${1^^} var field pairs=()
  while IFS= read -r var; do
    [[ $var == *_DB_VAULT_* || $var == *_SCYLLA_VAULT_* ]] && continue
    field="${var#"${prefix}_"}"
    field="${field,,}"
    field="${field/_/.}"
    pairs+=("$field=${!var}")
  done < <(manifest_vars "$prefix")
  (( ${#pairs[@]} > 0 )) || return 0
  kv_upsert "$svc" "$(json_obj "${pairs[@]}")"
  log "kv: secret/$svc (${#pairs[@]} keys)"
}

configure_postgres() {
  local svc=$1 prefix=${1^^}
  local db="$svc-postgresql" role="$svc-postgresql-approle"
  local user_var="${prefix}_DB_VAULT_USERNAME" pass_var="${prefix}_DB_VAULT_PASSWORD"

  vault_write "database/config/$db" "$(json_obj \
    plugin_name=postgresql-database-plugin \
    "connection_url=postgresql://{{username}}:{{password}}@$POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB?sslmode=disable" \
    "allowed_roles=$role" \
    "username=${!user_var}" \
    "password=${!pass_var}")"

  vault_write "database/roles/$role" "$(json_obj \
    "db_name=$db" \
    "creation_statements=$(< "$ASSETS_DIR/sql/postgresql-role-orm.sql")" \
    "revocation_statements=$(< "$ASSETS_DIR/sql/postgresql-revoke-orm.sql")" \
    "renewal_statements=$(< "$ASSETS_DIR/sql/postgresql-renew-orm.sql")" \
    default_ttl=1h \
    max_ttl=24h)"
  log "database: $db -> role $role"
}

configure_scylla() {
  local svc=$1 prefix=${1^^}
  local db="$svc-scylladb" role="$svc-scylladb-approle"
  local user_var="${prefix}_SCYLLA_VAULT_USERNAME" pass_var="${prefix}_SCYLLA_VAULT_PASSWORD"

  vault_write "database/config/$db" "$(json_obj \
    plugin_name=cassandra-database-plugin \
    "hosts=$svc-scylladb-chatter" \
    port:=9052 \
    protocol_version:=4 \
    tls:=false \
    "allowed_roles=$role" \
    "username=${!user_var}" \
    "password=${!pass_var}")"

  vault_write "database/roles/$role" "$(json_obj \
    "db_name=$db" \
    "creation_statements=$(< "$ASSETS_DIR/cql/cassandra-role-orm.cql")" \
    "revocation_statements=$(< "$ASSETS_DIR/cql/cassandra-revoke-orm.cql")" \
    default_ttl=1h \
    max_ttl=24h)"
  log "database: $db -> role $role"
}

configure_jwt() {
  local private_key public_key
  IFS= read -r -d '' private_key < "$JWT_PRIVATE_KEY" || true
  IFS= read -r -d '' public_key < "$JWT_PUBLIC_KEY" || true
  kv_upsert common/jwt/rs256 "$(json_obj "private-key=$private_key" "public-key=$public_key")"
  log "kv: secret/common/jwt/rs256"
}

configure_storage() {
  kv_upsert common/storage "$(json_obj \
    "storage.account-id=$STORAGE_ACCOUNT_ID" \
    "storage.bucket=$STORAGE_BUCKET" \
    "storage.access-key=$STORAGE_ACCESS_KEY" \
    "storage.secret-key=$STORAGE_SECRET_KEY")"
  log "kv: secret/common/storage"
}

for svc in "${services[@]}"; do
  if [[ $svc == jwt ]]; then
    configure_jwt
    continue
  fi
  if [[ $svc == storage ]]; then
    configure_storage
    continue
  fi
  configure_kv "$svc"
  if ! $with_db; then continue; fi
  if [[ -n ${HAS_POSTGRES[$svc]:-} ]]; then
    configure_postgres "$svc"
  fi
  if $with_scylla && [[ -n ${HAS_SCYLLA[$svc]:-} ]]; then
    configure_scylla "$svc"
  fi
done

log "Done: ${services[*]}"
