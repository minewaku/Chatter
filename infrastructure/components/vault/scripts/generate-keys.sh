#!/usr/bin/env bash
# Generate the shared RS256 JWT key pair under assets/keys/ (gitignored).
#
# Usage: generate-keys.sh [--force]
set -euo pipefail

KEY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/assets/keys"
private_key="$KEY_DIR/private-key.pem"
public_key="$KEY_DIR/public-key.pem"

if [[ ${1:-} != --force && ( -e $private_key || -e $public_key ) ]]; then
  echo "error: $KEY_DIR already has a key pair; pass --force to replace it" >&2
  exit 1
fi

mkdir -p "$KEY_DIR"
(umask 077; openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key")
openssl pkey -in "$private_key" -pubout -out "$public_key"
chmod 600 "$private_key"

echo "Wrote $private_key and $public_key. Load them with: scripts/configure-secrets.sh jwt"
