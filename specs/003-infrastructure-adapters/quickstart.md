# Quickstart: Validate the Infrastructure Adapters

**Feature**: [spec.md](spec.md) | Contracts: [adapter map](contracts/adapter-map.md), [configuration](contracts/configuration.md), [shared components](contracts/shared-components.md)

Run from the repository root unless stated. Prerequisites: Docker, JDK 21, Maven, `openssl`, `curl`.

## 1. Start the shared stacks (US5)

```bash
( cd infrastructure/components/postgresql && docker compose -p postgresql_chatter -f docker-compose.yml up -d )
( cd infrastructure/components/redis      && docker compose -p redis_chatter      -f docker-compose.yml up -d )
```

Expected: both containers `healthy`. Check the logins:

```bash
psql "postgresql://admin:123456@localhost:5432/chatter" -c "select 1"
redis-cli --user admin --pass 123456 --no-auth-warning ping      # PONG
```

## 2. Create a development signing key pair (US2, US3)

```bash
mkdir -p services/identity-access-next/.local
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out services/identity-access-next/.local/jwt-private.pem
openssl rsa -pubout -in services/identity-access-next/.local/jwt-private.pem -out services/identity-access-next/.local/jwt-public.pem
export IDENTITY_ACCESS_JWT_PRIVATE_KEY=file:$PWD/services/identity-access-next/.local/jwt-private.pem
export IDENTITY_ACCESS_JWT_PUBLIC_KEY=file:$PWD/services/identity-access-next/.local/jwt-public.pem
```

`.local/` is git-ignored. Without these variables the service must refuse to start (edge case in the spec).

## 3. Start a mail catcher (development only)

```bash
docker run -d --name mailpit-chatter --network chatter-net -p 1025:1025 -p 8025:8025 axllent/mailpit
```

Open `http://localhost:8025` to read verification emails.

## 4. Run the automated checks

```bash
cd services/identity-access-next
mvn -pl identityaccess-infrastructure -am verify
```

Expected: unit tests, Testcontainers integration tests, ArchUnit structure test, and Spotless check pass. This covers SC-002, SC-005, SC-006, SC-008, SC-010 and the adapter contract tests.

## 5. Start the service and walk the flow

```bash
mvn -pl identityaccess-start -am spring-boot:run
```

Expected on first start: log lines show Flyway migrating schema `identity_access`; `curl localhost:5001/actuator/health` returns `{"status":"UP"}`; `\dt identity_access.*` in `psql` lists `user_account` and `flyway_schema_history`, and nothing exists in `public` (SC-009).

Register (US1/US2). Use the public endpoint list from the configuration contract:

```bash
curl -i -X POST localhost:5001/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"a@example.com","username":"alice","birthday":"1999-01-01","password":"S3cure-pass!"}'
```

Then:
1. Open Mailpit, copy the link, and confirm it (verification confirm endpoint). Re-using it before expiry reports already-verified.
2. Sign in, renew, list sessions, and sign out once those endpoints exist in the presentation module. Until then the adapter contract and security tests cover these paths.
3. In Redis, `redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:*'` shows only digest-based keys; no raw token appears in any key or value.
4. In PostgreSQL, `select password_algorithm, left(password_hash, 10) from identity_access.user_account;` shows `argon2id` and `$argon2id$`.

## 6. Restart persistence (SC-003)

Restart the service, then `docker compose restart` each stack. Look up the account, the unexpired verification token, and the session: all unchanged.

## 7. Protected endpoint behaviour (US3)

With a credential issued by the service: `curl -H "Authorization: Bearer $TOKEN" …` is accepted. Missing, expired, tampered, wrong-issuer, or foreign-key credentials return `401`. Public endpoints and an `OPTIONS` preflight need no credential.

## 8. Clean up

```bash
docker rm -f mailpit-chatter
( cd infrastructure/components/postgresql && docker compose -p postgresql_chatter down )
( cd infrastructure/components/redis      && docker compose -p redis_chatter down )
```

Data stays in each component's `data/` folder; see the reset runbooks to wipe it.
