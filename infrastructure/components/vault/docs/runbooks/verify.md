# Verify Secrets

```bash
vault kv get secret/identityaccess
vault read database/creds/identityaccess-postgresql-approle
```

Use the corresponding path for other services (`secret/message`, `secret/common/jwt/rs256`). See [bootstrap](bootstrap.md) for setting `VAULT_ADDR` and `VAULT_TOKEN`, or the Docker fallback when the CLI is not installed.

## UI

Vault UI is available at `http://localhost:5002`. Sign in with the token from `infrastructure/components/vault/.vault-init`.
