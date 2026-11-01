#!/usr/bin/env bash
# Outage soak for the identity-access CDC relay (spec 013, SC-001).
#
#   bash infrastructure/components/kafka/scripts/soak-relay.sh [N]      (default N=1000)
#
# Writes N announcement-style transactions straight into identity_access.outbox exactly like the service does
# (insert, then delete, in one transaction), rolls every 10th one back, and randomly stops and starts the broker and
# the Connect worker while doing so. Then it reads the isolated topic identityaccess.soak.v1 and checks that every
# committed announcement arrived (duplicates are allowed) and no rolled-back one did. Needs the relay to be running
# (see docs/runbooks/relay.md). Real topics are never touched: rows use aggregate_type 'soak'.
set -euo pipefail

N="${1:-1000}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PG_ENV="$HERE/../../postgresql/.env"
PG_CONTAINER="${POSTGRES_CONTAINER:-postgresql-chatter}"
BROKER_CONTAINER=kafka-chatter
CONNECT_CONTAINER=kafka-connect-chatter
BROKER="kafka-chatter:5003"
TOPIC="identityaccess.soak.v1"

env_value() { [[ -f $PG_ENV ]] && grep -E "^$1=" "$PG_ENV" | tail -n 1 | cut -d= -f2- | tr -d '\r' || true; }
ADMIN_USER="${POSTGRES_USER:-$(env_value POSTGRES_USER)}"; ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_DB="${POSTGRES_DB:-$(env_value POSTGRES_DB)}"; ADMIN_DB="${ADMIN_DB:-chatter}"

WORK="$(mktemp -d)"
CHAOS_PID=""
cleanup() {
  [[ -n $CHAOS_PID ]] && kill "$CHAOS_PID" 2>/dev/null || true
  docker start "$BROKER_CONTAINER" "$CONNECT_CONTAINER" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

kafka() { docker exec -i "$BROKER_CONTAINER" "$@"; }

echo "soak: creating $TOPIC"
kafka kafka-topics --create --if-not-exists --bootstrap-server "$BROKER" --replication-factor 1 --partitions 3 --topic "$TOPIC" >/dev/null

echo "soak: generating $N transactions"
: > "$WORK/committed.txt"; : > "$WORK/rolled-back.txt"; : > "$WORK/run.sql"
for i in $(seq 1 "$N"); do
  id="$(cat /proc/sys/kernel/random/uuid)"
  if (( i % 10 == 0 )); then end=ROLLBACK; echo "$id" >> "$WORK/rolled-back.txt"; else end=COMMIT; echo "$id" >> "$WORK/committed.txt"; fi
  cat >> "$WORK/run.sql" <<SQL
BEGIN;
INSERT INTO identity_access.outbox (id, aggregate_type, aggregate_id, event_type, payload, occurred_at)
VALUES ('$id', 'soak', 'soak-$i', 'SoakEvent',
        '{"eventId":"$id","userId":"soak-$i","occurredAt":"2026-01-01T00:00:00Z"}'::jsonb, now());
DELETE FROM identity_access.outbox WHERE id = '$id';
$end;
SELECT pg_sleep(0.02);
SQL
done

# Start both containers and wait until docker reports them running (a stop that is still finishing makes a single start a no-op).
ensure_running() {
  local c tries
  for c in "$BROKER_CONTAINER" "$CONNECT_CONTAINER"; do
    for tries in $(seq 1 30); do
      [[ $(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null) == true ]] && continue 2
      docker start "$c" >/dev/null 2>&1 || true
      sleep 2
    done
    echo "soak: $c would not start" >&2; exit 2
  done
}

chaos() {
  while true; do
    sleep $((3 + RANDOM % 6))
    victim=$([[ $((RANDOM % 2)) -eq 0 ]] && echo "$BROKER_CONTAINER" || echo "$CONNECT_CONTAINER")
    docker stop "$victim" >/dev/null 2>&1 || true
    sleep $((2 + RANDOM % 5))
    docker start "$victim" >/dev/null 2>&1 || true
  done
}
chaos & CHAOS_PID=$!

echo "soak: running (with random broker / relay outages)"
docker exec -i "$PG_CONTAINER" psql -X -q -t -A -v ON_ERROR_STOP=1 -U "$ADMIN_USER" -d "$ADMIN_DB" < "$WORK/run.sql" >/dev/null

# Stop the chaos loop and any docker stop/start/sleep it has in flight, so a late "docker stop" cannot undo the start below.
pkill -P "$CHAOS_PID" 2>/dev/null || true; kill "$CHAOS_PID" 2>/dev/null || true; wait "$CHAOS_PID" 2>/dev/null || true; CHAOS_PID=""
ensure_running
echo "soak: all transactions written; waiting for the relay to catch up (up to 5 minutes)"

sort -u "$WORK/committed.txt" > "$WORK/committed.sorted"
sort -u "$WORK/rolled-back.txt" > "$WORK/rolled-back.sorted"
deadline=$((SECONDS + 300))
while true; do
  kafka kafka-console-consumer --bootstrap-server "$BROKER" --topic "$TOPIC" --from-beginning \
      --timeout-ms 20000 --property print.headers=true 2>/dev/null \
    | grep -oE 'id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | cut -d: -f2 > "$WORK/delivered.all" || true
  sort -u "$WORK/delivered.all" > "$WORK/delivered.sorted"
  comm -23 "$WORK/committed.sorted" "$WORK/delivered.sorted" > "$WORK/missing.txt"
  [[ ! -s $WORK/missing.txt || $SECONDS -ge $deadline ]] && break
  ensure_running
  echo "soak: $(wc -l < "$WORK/missing.txt") still missing, waiting ..."
  sleep 10
done

comm -12 "$WORK/rolled-back.sorted" "$WORK/delivered.sorted" > "$WORK/leaked.txt"
committed=$(wc -l < "$WORK/committed.sorted"); rolled=$(wc -l < "$WORK/rolled-back.sorted")
delivered=$(wc -l < "$WORK/delivered.sorted"); total=$(wc -l < "$WORK/delivered.all")
missing=$(wc -l < "$WORK/missing.txt"); leaked=$(wc -l < "$WORK/leaked.txt")
echo "soak: committed=$committed rolled-back=$rolled delivered(distinct)=$delivered duplicates=$((total - delivered)) missing=$missing leaked=$leaked"

kafka kafka-topics --delete --bootstrap-server "$BROKER" --topic "$TOPIC" >/dev/null 2>&1 || true
if (( missing > 0 || leaked > 0 )); then echo "soak: FAILED" >&2; exit 1; fi
echo "soak: PASSED"
