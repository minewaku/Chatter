#!/usr/bin/env bash
# Source this file (do not execute it): it exports the credentials of the identity-access CDC relay.
#
#   source infrastructure/shared/scripts/relay-env.sh
#
# The two values live only in infrastructure/components/postgresql/.env (gitignored; names in .env.example).
# Vault is not involved. A non-empty value already in the environment wins. The file is parsed line by line and
# never sourced, and no value is ever printed. No `set -e` here: this file runs inside the caller's shell.

_relay_env_file="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../components/postgresql" && pwd)/.env"

_relay_read_key() {
  [[ -f $_relay_env_file ]] || return 0
  local line value
  line=$(grep -E "^$1=" "$_relay_env_file" | tail -n 1 | tr -d '\r') || true
  value=${line#*=}
  value=${value#\"}; value=${value%\"}
  value=${value#\'}; value=${value%\'}
  printf '%s' "$value"
}

_relay_ok=1
for _relay_key in IDENTITYACCESS_DEBEZIUM_USERNAME IDENTITYACCESS_DEBEZIUM_PASSWORD; do
  _relay_value="${!_relay_key:-}"
  [[ -n $_relay_value ]] || _relay_value="$(_relay_read_key "$_relay_key")"
  if [[ -z $_relay_value ]]; then
    echo "relay-env: $_relay_key is empty. Set a real value in infrastructure/components/postgresql/.env" >&2
    _relay_ok=0
  elif [[ $_relay_value == change-me* ]]; then
    echo "relay-env: $_relay_key is still a placeholder (change-me...). Set a real value in infrastructure/components/postgresql/.env" >&2
    _relay_ok=0
  else
    export "$_relay_key=$_relay_value"
  fi
done
unset -f _relay_read_key
unset _relay_key _relay_value _relay_env_file

if [[ $_relay_ok -ne 1 ]]; then
  unset _relay_ok
  return 1 2>/dev/null || exit 1
fi
unset _relay_ok
