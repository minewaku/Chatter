# VaultService — Quick Start

This document provides a step-by-step guide on how to run the Vault server for this project, initialize and unseal it, enable secret engines, and securely store the secrets required by the various services in this repository.

## 1. Setup and Initialization

### 1.1 Run with Docker
Start the Vault service using Docker Compose:
```powershell
docker compose -p vault_service_chatter -f docker-compose.yml up -d
```

### 1.2 Initialize Vault
Initialize Vault and generate the unseal keys along with the initial root token:
```bash
vault operator init -key-shares=1 -key-threshold=1
```

> [!NOTE]
> - This command generates exactly **1 unseal key** and **1 initial root token**.
> - After initialization, Vault remains in a **sealed** state. You must run the unseal command below before you can use it.

### 1.3 Unseal Vault
Unlock Vault using the key generated during the initialization step:
```bash
vault operator unseal
```
*You will be prompted to provide the required number of unseal keys based on your configured `key-threshold`.*

### 1.4 Login
Authenticate using the initial root token obtained from the initialization step:
```bash
vault login <root_token>
```

---

## 2. Enable Secret Engines

### 2.1 Key-Value (KV) Engine for Static Secrets
Enable the KV (Key/Value) engine at the `secret` path using KV version 2:
```bash
vault secrets enable -path=secret kv-v2
```

> [!NOTE]
> This creates a `secret/` namespace that stores static secrets in a key-value structure. KV-v2 supports versioning for secret values.

### 2.2 Database Engine for Dynamic Secrets
To allow Vault to dynamically manage database credentials (e.g., MySQL, PostgreSQL), enable the database secrets engine:
```bash
vault secrets enable database
```

*(Optional)* If you ever need to disable specific database secret engines, you can use:
```bash
vault secrets disable database/authentication-service
vault secrets disable database/apigateway
```

**Useful References:**
- [Understand static vs dynamic secrets](https://developer.hashicorp.com/vault/tutorials/get-started/understand-static-dynamic-secrets)
- [Database secrets engine API](https://developer.hashicorp.com/vault/api-docs/secret/databases)
- [Spring Vault configuration](https://docs.spring.io/spring-cloud-vault/reference/advanced-topics.html)

---

## 3. Populating Secrets into Vault

> [!NOTE]
> The database engine does not support nested structures.

### 3.1 IdentityAccess Service

**Add static secrets:**
```bash
vault kv put secret/identityaccess - < ./vault/tmp/secrets/identityaccess-secrets.json
```

**Create PostgreSQL dynamic database config:**
*(Access is restricted to the "approle" role only)*
```bash
vault write database/config/identityaccess-postgresql \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@identityaccess-postgresql-chatter:5440/chatter?sslmode=disable" \
  allowed_roles="identityaccess-postgresql-approle" \
  username="vault" \
  password="FT6Qwf4NqRRT9BhA"
```

**Create the database role:**
```bash
vault write database/roles/identityaccess-postgresql-approle \
  db_name="identityaccess-postgresql" \
  creation_statements=@./vault/tmp/creation_statements/postgresql-role-orm.sql \
  revocation_statements=@./vault/tmp/revocation_statements/postgresql-revoke-orm.sql \
  default_ttl=1h \
  max_ttl=24h
```

### 3.2 Profile Service

**Add static secrets:**
```bash
vault kv put secret/profile - < ./vault/tmp/secrets/profile-secrets.json
```

**Create PostgreSQL dynamic database config:**
*(Access is restricted to the "approle" role only)*
```bash
vault write database/config/profile-postgresql \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@profile-postgresql-chatter:5441/chatter?sslmode=disable" \
  allowed_roles="profile-postgresql-approle" \
  username="vault" \
  password="QIUbomaKaq463g25"
```

**Create the database role:**
```bash
vault write database/roles/profile-postgresql-approle \
  db_name="profile-postgresql" \
  creation_statements=@./vault/tmp/creation_statements/postgresql-role-orm.sql \
  revocation_statements=@./vault/tmp/revocation_statements/postgresql-revoke-orm.sql \
  default_ttl=1h \
  max_ttl=24h
```

### 3.3 ApiGateway Service

**Add static secrets:**
```bash
vault kv put secret/apigateway - < ./vault/tmp/secrets/apigateway-secrets.json
```

### 3.4 Cerbos Bridge Service

**Add static secrets:**
```bash
vault kv put secret/cerbos-bridge - < ./vault/tmp/secrets/cerbos-bridge-secrets.json
```

### 3.5 JWT Keys

**Add static secrets for common JWT keys:**
```bash
vault kv put secret/common/jwt/rs256 private-key=@./vault/tmp/keys/private-key.pem public-key=@./vault/tmp/keys/public-key.pem
```

---

## 4. Verification

Verify that your secrets have been stored correctly:
```bash
vault kv get secret/identityaccess
```

---

## 5. Vault UI

You can also manage your secrets using the web interface. Open the Vault UI by navigating to the Vault server's address and port in your web browser, and log in using your root token.


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