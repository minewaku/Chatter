# Vault Bootstrap

Start a local Vault, initialise it on first run, and unseal it.

> Run from anywhere inside the repository.

## One command

```bash
infrastructure/components/vault/scripts/start.sh
```

It will:

1. Start the `vault-chatter` container and wait until it answers.
2. On first run, run `vault operator init -key-shares=1 -key-threshold=1` and save the unseal key and root token to `infrastructure/components/vault/.vault-init` (mode 600, gitignored).
3. Unseal Vault if it is sealed and check that the token works.

Add `--configure` to also push the secrets from `.env` ([configure secrets](configure-secrets.md)).

`VAULT_UNSEAL_KEY` and `VAULT_TOKEN` set in `.env` or the environment take precedence over `.vault-init`.

## Use the CLI yourself

```bash
export VAULT_ADDR=http://127.0.0.1:5002
export VAULT_TOKEN=$(sed -n 's/^VAULT_TOKEN=//p' infrastructure/components/vault/.vault-init)
```

If the `vault` binary is not installed: `docker exec -i -e VAULT_TOKEN vault-chatter vault <command>`.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Vault is not initialised but .vault-init exists` | `data/` was deleted but `.vault-init` was not; see [reset](reset.md). |
| `Vault is sealed and VAULT_UNSEAL_KEY is not set` | Restore `.vault-init` or set `VAULT_UNSEAL_KEY` in `.env`. |
| `Vault did not become reachable` | `docker logs vault-chatter`; check that port 5002 is free. |

## Next

[Configure service secrets](configure-secrets.md).
