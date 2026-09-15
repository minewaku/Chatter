#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

set -a
source "$ROOT_DIR/.env"
set +a

# Identity Access service secrets
: "${IDENTITYACCESS_DEBEZIUM_USERNAME:?Missing IDENTITYACCESS_DEBEZIUM_USERNAME}"
: "${IDENTITYACCESS_DEBEZIUM_PASSWORD:?Missing IDENTITYACCESS_DEBEZIUM_PASSWORD}"
: "${IDENTITYACCESS_MAIL_USERNAME:?Missing IDENTITYACCESS_MAIL_USERNAME}"
: "${IDENTITYACCESS_MAIL_PASSWORD:?Missing IDENTITYACCESS_MAIL_PASSWORD}"
: "${IDENTITYACCESS_REDIS_USERNAME:?Missing IDENTITYACCESS_REDIS_USERNAME}"
: "${IDENTITYACCESS_REDIS_PASSWORD:?Missing IDENTITYACCESS_REDIS_PASSWORD}"
: "${IDENTITYACCESS_REFRESH_SALT:?Missing IDENTITYACCESS_REFRESH_SALT}"
: "${IDENTITYACCESS_REFRESH_PASSWORD:?Missing IDENTITYACCESS_REFRESH_PASSWORD}"

# Identity Access PostgreSQL credential
: "${IDENTITYACCESS_DB_VAULT_PASSWORD:?Missing IDENTITYACCESS_DB_VAULT_PASSWORD}"

# Profile service secrets
: "${PROFILE_DEBEZIUM_USERNAME:?Missing PROFILE_DEBEZIUM_USERNAME}"
: "${PROFILE_DEBEZIUM_PASSWORD:?Missing PROFILE_DEBEZIUM_PASSWORD}"
: "${PROFILE_REDIS_USERNAME:?Missing PROFILE_REDIS_USERNAME}"
: "${PROFILE_REDIS_PASSWORD:?Missing PROFILE_REDIS_PASSWORD}"
: "${PROFILE_CLOUDINARY_CLOUD_NAME:?Missing PROFILE_CLOUDINARY_CLOUD_NAME}"
: "${PROFILE_CLOUDINARY_API_KEY:?Missing PROFILE_CLOUDINARY_API_KEY}"
: "${PROFILE_CLOUDINARY_API_SECRET:?Missing PROFILE_CLOUDINARY_API_SECRET}"
: "${PROFILE_CLOUDINARY_SECURE:?Missing PROFILE_CLOUDINARY_SECURE}"

# Profile PostgreSQL credential
: "${PROFILE_DB_VAULT_PASSWORD:?Missing PROFILE_DB_VAULT_PASSWORD}"

# API Gateway service secrets
: "${APIGATEWAY_REDIS_USERNAME:?Missing APIGATEWAY_REDIS_USERNAME}"
: "${APIGATEWAY_REDIS_PASSWORD:?Missing APIGATEWAY_REDIS_PASSWORD}"

# Message service secrets
: "${MESSAGE_DEBEZIUM_USERNAME:?Missing MESSAGE_DEBEZIUM_USERNAME}"
: "${MESSAGE_DEBEZIUM_PASSWORD:?Missing MESSAGE_DEBEZIUM_PASSWORD}"
: "${MESSAGE_REDIS_USERNAME:?Missing MESSAGE_REDIS_USERNAME}"
: "${MESSAGE_REDIS_PASSWORD:?Missing MESSAGE_REDIS_PASSWORD}"
: "${MESSAGE_CLOUDINARY_CLOUD_NAME:?Missing MESSAGE_CLOUDINARY_CLOUD_NAME}"
: "${MESSAGE_CLOUDINARY_API_KEY:?Missing MESSAGE_CLOUDINARY_API_KEY}"
: "${MESSAGE_CLOUDINARY_API_SECRET:?Missing MESSAGE_CLOUDINARY_API_SECRET}"
: "${MESSAGE_CLOUDINARY_SECURE:?Missing MESSAGE_CLOUDINARY_SECURE}"

# Message PostgreSQL and ScyllaDB credentials
: "${MESSAGE_DB_VAULT_PASSWORD:?Missing MESSAGE_DB_VAULT_PASSWORD}"
: "${MESSAGE_SCYLLA_VAULT_PASSWORD:?Missing MESSAGE_SCYLLA_VAULT_PASSWORD}"

# Shared Vault database credential
: "${VAULT_DB_USERNAME:?Missing VAULT_DB_USERNAME}"


# KV secrets

vault kv put secret/identityaccess \
  debezium.username="$IDENTITYACCESS_DEBEZIUM_USERNAME" \
  debezium.password="$IDENTITYACCESS_DEBEZIUM_PASSWORD" \
  mail.username="$IDENTITYACCESS_MAIL_USERNAME" \
  mail.password="$IDENTITYACCESS_MAIL_PASSWORD" \
  redis.username="$IDENTITYACCESS_REDIS_USERNAME" \
  redis.password="$IDENTITYACCESS_REDIS_PASSWORD" \
  refresh.salt="$IDENTITYACCESS_REFRESH_SALT" \
  refresh.password="$IDENTITYACCESS_REFRESH_PASSWORD"

vault kv put secret/profile \
  debezium.username="$PROFILE_DEBEZIUM_USERNAME" \
  debezium.password="$PROFILE_DEBEZIUM_PASSWORD" \
  redis.username="$PROFILE_REDIS_USERNAME" \
  redis.password="$PROFILE_REDIS_PASSWORD" \
  cloudinary.cloud_name="$PROFILE_CLOUDINARY_CLOUD_NAME" \
  cloudinary.api_key="$PROFILE_CLOUDINARY_API_KEY" \
  cloudinary.api_secret="$PROFILE_CLOUDINARY_API_SECRET" \
  cloudinary.secure="$PROFILE_CLOUDINARY_SECURE"

vault kv put secret/apigateway \
  redis.username="$APIGATEWAY_REDIS_USERNAME" \
  redis.password="$APIGATEWAY_REDIS_PASSWORD"

vault kv put secret/message \
  debezium.username="$MESSAGE_DEBEZIUM_USERNAME" \
  debezium.password="$MESSAGE_DEBEZIUM_PASSWORD" \
  redis.username="$MESSAGE_REDIS_USERNAME" \
  redis.password="$MESSAGE_REDIS_PASSWORD" \
  cloudinary.cloud_name="$MESSAGE_CLOUDINARY_CLOUD_NAME" \
  cloudinary.api_key="$MESSAGE_CLOUDINARY_API_KEY" \
  cloudinary.api_secret="$MESSAGE_CLOUDINARY_API_SECRET" \
  cloudinary.secure="$MESSAGE_CLOUDINARY_SECURE"


# Identity Access PostgreSQL

vault write database/config/identityaccess-postgresql \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@identityaccess-postgresql-chatter:5440/chatter?sslmode=disable" \
  allowed_roles="identityaccess-postgresql-approle" \
  username="$VAULT_DB_USERNAME" \
  password="$IDENTITYACCESS_DB_VAULT_PASSWORD"

vault write database/roles/identityaccess-postgresql-approle \
  db_name="identityaccess-postgresql" \
  creation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-role-orm.sql" \
  revocation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-revoke-orm.sql" \
  renewal_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-renew-orm.sql" \
  default_ttl=1h \
  max_ttl=24h


# Profile PostgreSQL

vault write database/config/profile-postgresql \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@profile-postgresql-chatter:5441/chatter?sslmode=disable" \
  allowed_roles="profile-postgresql-approle" \
  username="$VAULT_DB_USERNAME" \
  password="$PROFILE_DB_VAULT_PASSWORD"

vault write database/roles/profile-postgresql-approle \
  db_name="profile-postgresql" \
  creation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-role-orm.sql" \
  revocation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-revoke-orm.sql" \
  renewal_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-renew-orm.sql" \
  default_ttl=1h \
  max_ttl=24h


# Message PostgreSQL

vault write database/config/message-postgresql \
  plugin_name="postgresql-database-plugin" \
  connection_url="postgresql://{{username}}:{{password}}@message-postgresql-chatter:5442/chatter?sslmode=disable" \
  allowed_roles="message-postgresql-approle" \
  username="$VAULT_DB_USERNAME" \
  password="$MESSAGE_DB_VAULT_PASSWORD"

vault write database/roles/message-postgresql-approle \
  db_name="message-postgresql" \
  creation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-role-orm.sql" \
  revocation_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-revoke-orm.sql" \
  renewal_statements=@"$ROOT_DIR/infrastructure/vault/assets/sql/postgresql-renew-orm.sql" \
  default_ttl=1h \
  max_ttl=24h


# Message ScyllaDB

vault write database/config/message-scylladb \
  plugin_name="cassandra-database-plugin" \
  hosts="message-scylladb-chatter" \
  port="9052" \
  protocol_version=4 \
  username="$VAULT_DB_USERNAME" \
  password="$MESSAGE_SCYLLA_VAULT_PASSWORD" \
  tls=false \
  allowed_roles="message-scylladb-approle"

vault write database/roles/message-scylladb-approle \
  db_name="message-scylladb" \
  creation_statements=@"$ROOT_DIR/infrastructure/vault/assets/cql/cassandra-role-orm.cql" \
  revocation_statements=@"$ROOT_DIR/infrastructure/vault/assets/cql/cassandra-revoke-orm.cql" \
  default_ttl=1h \
  max_ttl=24h


# Shared JWT keys

vault kv put secret/common/jwt/rs256 \
  private-key=@"$ROOT_DIR/infrastructure/vault/assets/keys/private-key.pem" \
  public-key=@"$ROOT_DIR/infrastructure/vault/assets/keys/public-key.pem"
