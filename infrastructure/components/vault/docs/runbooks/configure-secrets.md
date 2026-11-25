# Vault Configure Secrets

Push application secrets, database roles, and JWT keys into an initialised, unsealed Vault.

## 1. Fill in `.env`

```bash
cp .env.example .env
```

`.env` and `.env.example` live in `infrastructure/components/vault/`; `.env` is gitignored. In a git worktree the scripts fall back to the main checkout's copy; set `VAULT_ENV_FILE=/path/to/file` to use another file.

Format: `KEY=VALUE`, one per line, no inline comments. The file is parsed, not sourced, so passwords with spaces, `$` or `#` work as-is; use quotes only to keep leading or trailing whitespace. Variables already set in your shell win over the file.

`.env.example` doubles as the manifest: every `<SERVICE>_*` variable listed there must be non-empty for that service to be configured. Add new secrets to `.env.example` and `.env`; no script change is needed for plain KV secrets.

## 2. Run

```bash
infrastructure/components/vault/scripts/generate-keys.sh                       # once, creates assets/keys/*.pem
infrastructure/components/vault/scripts/configure-secrets.sh                   # all services
infrastructure/components/vault/scripts/configure-secrets.sh identityaccess    # one service
```

Add `--no-db` to write only KV secrets and JWT keys, skipping the database engine (useful while the databases are down or the `*_VAULT_*` credentials are still placeholders).

Add `--no-scylla` to skip only the message service's ScyllaDB config (there is no Scylla component under `infrastructure/components` yet).

Services: `identityaccess`, `apigateway`, `message`, `jwt`, `storage`. All inputs are validated before anything is written, and missing variables are listed together. The script enables the `secret` (kv-v2) and `database` engines when absent.

## Copying infrastructure credentials

Credentials of shared infrastructure live in the component's own `.env` (`../postgresql/.env`, `../redis/.env`) because its docker-compose reads them. Copy them into `vault/.env` under every service prefix; after rotating one, update both files and re-run `configure-secrets.sh`.

## Naming convention

| Variable | Result |
|----------|--------|
| `<SERVICE>_<GROUP>_<FIELD>` | `secret/<service>`, key `<group>.<field>` (e.g. `IDENTITYACCESS_MAIL_PASSWORD` -> `mail.password`; `MESSAGE_REDIS_PASSWORD` -> `redis.password`) |
| `<SERVICE>_DB_VAULT_USERNAME/PASSWORD` | Admin account for `database/config/<service>-postgresql` and role `<service>-postgresql-approle`. Every service connects to the one shared instance (`postgresql-chatter:5432/chatter`, override with `VAULT_POSTGRES_HOST/PORT/DB`), so the values are the same under each prefix |
| `MESSAGE_SCYLLA_VAULT_USERNAME/PASSWORD` | Same for `message-scylladb` |
| `assets/keys/*.pem` | `secret/common/jwt/rs256` (`private-key`, `public-key`) |
| `STORAGE_ACCOUNT_ID`, `STORAGE_BUCKET`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY` | `secret/common/storage` (`storage.account-id`, `storage.bucket`, `storage.access-key`, `storage.secret-key`), shared by all services; each service uses its own key prefix inside the one bucket |

Existing KV secrets are patched, so keys not managed by `.env` survive a re-run. Role statements live in `assets/sql` and `assets/cql`.

## Changing one secret

Edit `.env`, then re-run `configure-secrets.sh <service>`. For a quick one-off: `vault kv patch secret/<service> key=value`.

## Notes

- Dynamic database roles are created with superuser rights because the ORMs manage schemas. Tighten the statements in `assets/` before using this beyond local development.
- `database/config/*` verifies the connection, so the target database containers must be running when you configure that service.
