# Vault Bootstrap

Initialize the local Vault instance.

> Run from the repository root.

## Start

```bash
docker compose \
  -p vault_service_chatter \
  --project-directory infrastructure/components/vault \
  -f infrastructure/components/vault/docker-compose.yml \
  up -d
```

## Initialize

```bash
vault operator init -key-shares=1 -key-threshold=1
```

Store the generated:

* Unseal key
* Initial root token

## Unseal

```bash
vault operator unseal
```

Provide the generated unseal key.

## Login

```bash
vault login <root_token>
```

## Enable Secret Engines

```bash
vault secrets enable -path=secret kv-v2
vault secrets enable database
```

## Next

Configure application secrets and database roles using [configure-secrets.md](configure-secrets.md).
