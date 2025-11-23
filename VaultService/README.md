# VaultService — Quick Start

This document explains how to run the Vault server for this project, initialize and unseal it, enable engines, and store secrets used by the services in this repo.

## Run with Docker




Start Vault using Docker Compose:

```bash
docker compose -p vault_service_chatter -f docker-compose.yml up -d
```

## Initialize Vault

Initialize Vault and generate unseal keys plus the initial root token:

```bash
vault operator init -key-shares=1 -key-threshold=1
```

> [!NOTE]
> - This command generates 1 unseal key and 1 initial root token (because the **-key-shares** and **-key-threshold** are both 1).
> - Vault encrypts data at rest; it must be unsealed before use.

## Unseal the Vault

Use the unseal command and provide the unseal key(s) produced during init:

```bash
vault operator unseal
```

Then provided the required number of unseal keys based on the key-threshold.

## Login

Authenticate using the initial root token from the init step:

```bash
vault login <root_token>
```

## Enable KV engine for secrets

Enable the KV (key/value) engine at the `secret` path (using KV v2):

```bash
vault secrets enable -path=secret kv-v2
```

> [!NOTE]
> This creates a `./secret` namespace that stores secrets in a key–value structure (kv-v2 supports versioned secret values).

## Enable database secrets engine (dynamic secrets)

To allow Vault to manage database credentials dynamically (MySQL, Postgres, Redis, etc.):

```bash
vault secrets enable database
```
vault secrets enable database
vault secrets disable database/authentication-service
vault secrets disable database/apigateway

Useful references:
- Understand static vs dynamic secrets: https://developer.hashicorp.com/vault/tutorials/get-started/understand-static-dynamic-secrets
- Database secrets engine API: https://developer.hashicorp.com/vault/api-docs/secret/databases
- Spring Vault configuration (see each service's application-dev.properties): https://docs.spring.io/spring-cloud-vault/reference/advanced-topics.html

## Putting secrets into Vault

> [!NOTE]
> The database engine doesn't support nested structures

### 1. For authentication-service
<br>

**Put secrets for authentication-service**

```bash
vault kv put secret/authentication-service - < ./vault/tmp/authentication-sv-secrets.json
```
<br>

**Create MySQL database config with dynamic secrets; access restricted to the "approle" role only**
```bash copy
vault write database/config/authentication-service-mysql \
  plugin_name=mysql-database-plugin \
  connection_url="{{username}}:{{password}}@tcp(authentication-mysql-chatter:3314)/chatter" \
  allowed_roles=authentication-service-mysql-approle \
  username="vault" \
  password="FT6Qwf4NqRRT9BhA"
```

```bash copy
vault write database/roles/authentication-service-mysql-approle \
    db_name="authentication-service-mysql" \
    creation_statements=@./vault/tmp/creation_statements/mysql-role-orm.sql \
    default_ttl=1h \
    max_ttl=24h
```
<br>

**Create PostgreSQL database config with dynamic secrets; access restricted to the "approle" role only**
```bash copy
vault write database/config/authentication-service-postgres \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@authentication-postgresql-chatter:5440/chatter?sslmode=disable" \
  allowed_roles="authentication-service-postgres-approle" \
  username="vault" \
  password="FT6Qwf4NqRRT9BhA"
```

```bash copy
vault write database/roles/authentication-service-postgres-approle \
  db_name="authentication-service-postgres" \
  creation_statements=@./vault/tmp/creation_statements/postgres-role-orm.sql \
  default_ttl=1h \
  max_ttl=24h
```
<br>

### 2. For apigateway

```bash
vault kv put secret/api-gateway - < ./vault/tmp/api-gateway-secrets.json
```
<br>


```bash
vault kv put secret/authorization-service - < ./vault/tmp/authorization-sv-secrets.json
vault kv put secret/apigateway-service - < ./vault/tmp/apigateway-sv-secrets.json
vault kv put secret/profiles-service - < ./vault/tmp/profiles-sv-secrets.json
```

### For cerbos-bridge-service
```bash
vault kv put secret/cerbos-bridge-service - < ./vault/tmp/cerbos-bridge-sv-secrets.json
```

### For apigateway-service
```bash
vault kv put secret/apigateway-service - < ./vault/tmp/apigateway-sv-secrets.json
```

### For jwt keys
```bash
vault kv put secret/common/jwt/rs256 private-key=@./vault/tmp/private-key.pem public-key=@./vault/tmp/public-key.pem
```

```bash
vault kv put secret/api-gateway private-key=@./vault/tmp/private-key.pem public-key=@./vault/tmp/public-key.pem
```

## Verify secrets

```bash
vault kv get secret/authentication-service
```

## Vault UI

Open the Vault UI at the Vault server's address and port to manage secrets using the web interface.

---

If you want, I can:
- Add a short script to populate the `./vault/tmp` files from environment variables for CI usage.
- Replace example passwords and secrets in the README with placeholders and explain secure handling.
- Add a minimal troubleshooting section for common Vault errors.

Which of these would you like next?


> [!CAUTION]
> Spring Vault hasn't supported database engine for Redis yet so these configurations are deprecated. I only keep these below as examples for future projects <br>
> See the full list of backend databases that are supported by Spring Vault [here](https://cloud.spring.io/spring-cloud-vault/reference/html/#vault.config.backends.database-backends)

```bash
vault write database/config/authentication-service-redis \
  plugin_name="redis-database-plugin" \
  host="authentication-redis-chatter" \
  port=6387 \
  tls=false \
  username="vault" \
  password="XIqnIWD0DRb7Axwg" \
  allowed_roles="authentication-service-redis-approle"
```

```bash
vault write database/roles/authentication-service-redis-approle \
    db_name="authentication-service-redis" \
    creation_statements='["+@admin"]' \
    default_ttl="1h" \
    max_ttl="24h"
```
<br>

<br>

**Create Redis database config with dynamic secrets; access restricted to the "approle" role only**
```bash
vault write database/config/api-gateway-redis \
  plugin_name="redis-database-plugin" \
  host="api-gateway-redis-chatter" \
  port=6380 \
  tls=false \
  username="vault" \
  password="coFY4E7Nultq6lxM" \
  allowed_roles="api-gateway-redis-approle"
```


```bash
vault write database/roles/api-gateway-redis-approle \
    db_name="api-gateway-redis" \
    creation_statements='["+@admin"]' \
    default_ttl="1h" \
    max_ttl="24h"
```