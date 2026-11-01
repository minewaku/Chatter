#!/usr/bin/env bash
# Creates the database role and the publication that the identity-access CDC relay (Debezium) uses.
# Run it by hand against the live database, after identity-access has started once so its migrations ran.
# Safe to run again: a second run changes nothing and exits 0. The password is never printed.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPONENT="$(cd "$HERE/.." && pwd)"
# shellcheck source=../../../shared/scripts/relay-env.sh
source "$HERE/../../../shared/scripts/relay-env.sh"

CONTAINER="${POSTGRES_CONTAINER:-postgresql-chatter}"

env_value() {
  # first match of KEY=VALUE in the component's .env, empty if the file or key is missing
  [[ -f $COMPONENT/.env ]] || return 0
  grep -E "^$1=" "$COMPONENT/.env" | tail -n 1 | cut -d= -f2- | tr -d '\r' || true
}
ADMIN_USER="${POSTGRES_USER:-$(env_value POSTGRES_USER)}"
ADMIN_DB="${POSTGRES_DB:-$(env_value POSTGRES_DB)}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_DB="${ADMIN_DB:-chatter}"

fail() { echo "create-relay-role: $*" >&2; exit 1; }

# RELAY_USER / RELAY_PASS are handed to psql through the container environment so they never show in a process list.
export RELAY_USER="$IDENTITYACCESS_DEBEZIUM_USERNAME" RELAY_PASS="$IDENTITYACCESS_DEBEZIUM_PASSWORD"
psql_admin() { docker exec -i -e RELAY_USER -e RELAY_PASS "$CONTAINER" psql -X -q -t -A -v ON_ERROR_STOP=1 -U "$ADMIN_USER" -d "$ADMIN_DB" "$@"; }

[[ $(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || true) == true ]] \
  || fail "container $CONTAINER is not running"
[[ $(psql_admin -c 'SHOW wal_level') == logical ]] \
  || fail "wal_level is not 'logical'. Restart the Postgres stack with the compose file of this repo (see the README)"
[[ $(psql_admin -c "SELECT to_regclass('identity_access.outbox') IS NOT NULL AND to_regclass('identity_access.relay_heartbeat') IS NOT NULL") == t ]] \
  || fail "table identity_access.outbox or identity_access.relay_heartbeat does not exist. Start identity-access once so its migrations run, then re-run this script"

psql_admin <<'SQL' >/dev/null
\getenv relay_user RELAY_USER
\getenv relay_pass RELAY_PASS

SELECT format('CREATE ROLE %I LOGIN REPLICATION PASSWORD %L', :'relay_user', :'relay_pass')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'relay_user') \gexec
SELECT format('ALTER ROLE %I LOGIN REPLICATION PASSWORD %L', :'relay_user', :'relay_pass')
WHERE EXISTS (SELECT FROM pg_roles WHERE rolname = :'relay_user') \gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'relay_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA identity_access TO %I', :'relay_user') \gexec
SELECT format('GRANT SELECT ON identity_access.outbox TO %I', :'relay_user') \gexec
-- the relay's heartbeat statement updates this one-row table so its replication slot keeps advancing
SELECT format('GRANT SELECT, UPDATE ON identity_access.relay_heartbeat TO %I', :'relay_user') \gexec

SELECT 'CREATE PUBLICATION identity_access_outbox FOR TABLE identity_access.outbox, identity_access.relay_heartbeat'
WHERE NOT EXISTS (SELECT FROM pg_publication WHERE pubname = 'identity_access_outbox') \gexec
SELECT 'ALTER PUBLICATION identity_access_outbox SET TABLE identity_access.outbox, identity_access.relay_heartbeat'
WHERE EXISTS (SELECT FROM pg_publication WHERE pubname = 'identity_access_outbox') \gexec
SQL

echo "create-relay-role: role '$RELAY_USER' (LOGIN REPLICATION) and publication identity_access_outbox are ready:"
psql_admin -c "SELECT '  publication ' || pubname || ' -> ' || schemaname || '.' || tablename FROM pg_publication_tables WHERE pubname = 'identity_access_outbox'"
