# Vault Infrastructure

HashiCorp Vault instance used for secrets, dynamic database credentials, and JWT keys across the Chatter services.

## Quick Start

```powershell
docker compose -p vault_service_chatter -f docker-compose.yml up -d
```

Then follow the [bootstrap runbook](docs/runbooks/bootstrap.md) to initialize, unseal, log in, and enable secret engines. Configuration of service secrets lives in the [configure secrets runbook](docs/runbooks/configure-secrets.md).

## Layout

```
docker-compose.yml                   # Standalone Vault server
config/local.json                    # Dev config referenced by docker-compose
assets/json/*.json                   # Static secret payloads per service
assets/sql/*.sql                     # PostgreSQL/MySQL statements for DB roles
assets/cql/*.cql                     # Scylla/Cassandra statements
assets/keys/*.pem                    # Example JWT key pair
assets/scripts/startup.sh            # Sample unseal/login helper
docs/runbooks/*.md                   # Operational procedures
docs/references/*.md                 # Secret paths, DB role tables, deprecated flows
data/                                 # Vault storage (left checked-in for now)
```

## Runbooks & References
- [Bootstrap Vault](docs/runbooks/bootstrap.md)
- [Configure service secrets](docs/runbooks/configure-secrets.md)
- [Token & UI tips](docs/runbooks/manage-tokens.md)
- [Secret path reference](docs/references/secret-paths.md)
- [Database role reference](docs/references/db-roles.md)

## Notes
- Commands assume you are inside `infrastructure/components/vault`.
- Files under `assets/` are placeholders; never commit real secrets or production keys.
- The `data/` directory currently tracks dev state. Clear it when you need a clean Vault Store, or move it outside the repo if storage churn becomes noisy.
