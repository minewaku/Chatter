# Vault Infrastructure

HashiCorp Vault instance used for secrets, dynamic database credentials, and JWT keys across the Chatter services.

## Quick Start

Run from this folder (the scripts work from anywhere):

```bash
cd infrastructure/components/vault
cp .env.example .env          # then fill in the values; .env is gitignored
scripts/generate-keys.sh      # first time only: JWT key pair
scripts/start.sh --configure  # start, init/unseal, push secrets
```

`start.sh` is idempotent: on a fresh Vault it initialises it and stores the unseal key and root token in `.vault-init` (gitignored); on later runs it only starts and unseals.

## How credentials flow

```
.env  (gitignored, you edit)  ──►  scripts/configure-secrets.sh  ──►  Vault
.env.example (committed, names only; also the list of required variables)
```

See [configure secrets](docs/runbooks/configure-secrets.md) for the naming convention and per-service usage.

## Layout

```
docker-compose.yml        # Standalone Vault server (127.0.0.1:5002, file storage in data/)
.env / .env.example      # Credentials you fill in (gitignored) / committed template and manifest
scripts/start.sh          # Start, initialise on first run, unseal, optionally configure
scripts/configure-secrets.sh  # .env -> Vault (KV, database roles, JWT keys), per service
scripts/generate-keys.sh  # RS256 key pair into assets/keys/
scripts/lib.sh            # Shared helpers (env parsing, JSON building, vault wrapper)
assets/sql, assets/cql    # Dynamic role statements for PostgreSQL / ScyllaDB
assets/keys               # JWT key pair (gitignored)
docs/runbooks             # Operational procedures
data/                     # Vault storage (gitignored)
.vault-init               # Unseal key + root token written by start.sh (gitignored)
```

## Runbooks

- [Bootstrap Vault](docs/runbooks/bootstrap.md)
- [Configure service secrets](docs/runbooks/configure-secrets.md)
- [Identity Access mail credentials](docs/runbooks/identity-access-mail.md)
- [Verify secrets](docs/runbooks/verify.md)
- [Reset Vault](docs/runbooks/reset.md)

## Notes

- The scripts use the `vault` CLI when installed and otherwise run it inside the `vault-chatter` container, so only Docker is required.
- Secrets are never passed as command-line arguments; they travel on stdin.
- This setup is for local development: one unseal key, no TLS, root token, and dynamic database roles with superuser rights. Do not reuse it for production.
