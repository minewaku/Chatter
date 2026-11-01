#!/usr/bin/env bash
# Registers (creates or updates) every connector definition in ../connectors with the Kafka Connect worker.
# Safe to run again. Needs the relay credentials in the shell: the worker resolves ${env:...} placeholders itself.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONNECT_URL="${CONNECT_URL:-http://localhost:5004}"
# shellcheck source=../../../shared/scripts/relay-env.sh
source "$HERE/../../../shared/scripts/relay-env.sh"

echo "register-connectors: waiting for $CONNECT_URL ..."
for _ in $(seq 1 60); do
  curl -sf "$CONNECT_URL/connectors" >/dev/null 2>&1 && break
  sleep 2
done
curl -sf "$CONNECT_URL/connectors" >/dev/null 2>&1 || { echo "register-connectors: Kafka Connect did not answer within 120 seconds" >&2; exit 1; }

for file in "$HERE"/../connectors/*.json; do
  name="$(basename "$file" .json)"
  curl -sf -X PUT -H 'Content-Type: application/json' --data @"$file" "$CONNECT_URL/connectors/$name/config" >/dev/null \
    || { echo "register-connectors: registering $name failed" >&2; exit 1; }
  echo "register-connectors: registered $name"
  sleep 3
  curl -s "$CONNECT_URL/connectors/$name/status"; echo
done
