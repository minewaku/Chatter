# Contract: Service Configuration

**Feature**: [spec.md](../spec.md)

Defaults live in `identityaccess-start/src/main/resources/application.yml`. Every value can be overridden with an environment variable (Spring relaxed binding or the `${…}` placeholders shown).

## Server

| Property | Default | Notes |
|---|---|---|
| `server.port` | `${IDENTITY_ACCESS_SERVER_PORT:5001}` | Matches the old service; avoids the Kafka UI on host port 8080 |

## Datasource and migrations

| Property | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `${IDENTITY_ACCESS_DB_URL:jdbc:postgresql://localhost:5432/chatter?currentSchema=identity_access}` | Shared database `chatter`, service schema `identity_access` |
| `spring.datasource.username` | `${IDENTITY_ACCESS_DB_USERNAME:admin}` | |
| `spring.datasource.password` | `${IDENTITY_ACCESS_DB_PASSWORD:123456}` | Development credential |
| `spring.flyway.enabled` | `true` | |
| `spring.flyway.schemas` | `identity_access` | Migration history lives here |
| `spring.flyway.default-schema` | `identity_access` | |
| `spring.flyway.create-schemas` | `true` | Creates the schema on first start |

## Redis

| Property | Default |
|---|---|
| `spring.data.redis.host` | `${IDENTITY_ACCESS_REDIS_HOST:localhost}` |
| `spring.data.redis.port` | `${IDENTITY_ACCESS_REDIS_PORT:6379}` |
| `spring.data.redis.username` | `${IDENTITY_ACCESS_REDIS_USERNAME:admin}` |
| `spring.data.redis.password` | `${IDENTITY_ACCESS_REDIS_PASSWORD:123456}` |
| `identity-access.redis.key-prefix` | `identity-access:` |

## Identifiers, sessions, tokens

| Property | Default | Notes |
|---|---|---|
| `identity-access.snowflake.worker-id` | `${IDENTITY_ACCESS_SNOWFLAKE_WORKER_ID:0}` | 0–1023, unique per running instance; out of range stops start-up |
| `identity-access.session.lifetime` | `P30D` | Exposed as the single `Duration sessionLifetime` bean |
| `identity-access.session.retention` | `P30D` | Extra time an expired or revoked session stays readable |
| `identity-access.security.jwt.private-key` | `${IDENTITY_ACCESS_JWT_PRIVATE_KEY}` (required, no fallback) | PEM text, or `file:`/`classpath:` location of a PKCS#8 PEM |
| `identity-access.security.jwt.public-key` | `${IDENTITY_ACCESS_JWT_PUBLIC_KEY}` (required, no fallback) | PEM text or location of an X.509 PEM |
| `identity-access.security.jwt.issuer` | `identity-access` | Checked on every incoming credential |
| `identity-access.security.jwt.audience` | `chatter` | Checked on every incoming credential |
| `identity-access.security.jwt.access-token-ttl` | `PT15M` | Lifetime of an access credential; also the lifetime of the revocation entry written when a session is revoked (spec 004). Credentials carry the session id in claim `sid`; a credential without it is refused. |

Missing or malformed keys, or a key pair that does not match, stop start-up with a message naming the property.

## HTTP security

| Property | Default | Notes |
|---|---|---|
| `identity-access.security.public-endpoints` | `/api/v*/auth/register`, `/api/v*/auth/authenticate`, `/api/v*/auth/refresh`, `/api/v*/auth/logout`, `/api/v*/auth/verification/resend`, `/api/v*/auth/verification/confirm`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | Ant patterns; anything else needs a valid credential |
| `identity-access.security.cors.allowed-origins` | empty | Empty denies cross-origin browsers; `OPTIONS` preflight itself needs no credential |
| (fixed) | `/actuator/health/**` public | Other actuator endpoints are not exposed |

## Mail

| Property | Default |
|---|---|
| `spring.mail.host` / `spring.mail.port` | `${IDENTITY_ACCESS_MAIL_HOST:localhost}` / `${IDENTITY_ACCESS_MAIL_PORT:1025}` |
| `identity-access.verification.mail.from` | `no-reply@chatter.local` |
| `identity-access.verification.mail.subject` | `Verify your Chatter email` |
| `identity-access.verification.mail.public-base-url` | `http://localhost:5001` |
| `identity-access.verification.mail.confirm-path` | `/api/v1/auth/verification/confirm` |

The link in the email is `{public-base-url}{confirm-path}?token={rawToken}`.

## Logging rule

No log statement in infrastructure may include a password, password hash, raw verification token, raw refresh credential, access credential, or private key. Tests assert this for the adapters' failure paths.
