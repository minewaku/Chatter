# Vault Configure Secrets

Configure application secrets and database credentials after Vault is initialized, unsealed, and authenticated.

## Configure

```bash
bash infrastructure/vault/scripts/configure-secrets.sh
```

The script configures:

* Application KV secrets
* PostgreSQL dynamic credentials
* Message-service ScyllaDB credentials
* Shared JWT key material

Vault configuration in `infrastructure/vault/scripts/configure-secrets.sh` is the canonical source of truth.

Configuration payloads are stored under `infrastructure/vault/assets/`.
