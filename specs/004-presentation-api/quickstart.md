# Quickstart: Validate the Presentation API

**Feature**: [spec.md](spec.md) | Contracts: [HTTP API](contracts/http-api.md), [error model](contracts/error-model.md), [core and infrastructure changes](contracts/core-and-infra-changes.md)

Run from the repository root unless stated. Prerequisites: Docker, JDK 21, `openssl`, `curl`, `jq`. Setup steps 1-3 are the same as the infrastructure quickstart ([003 quickstart](../003-infrastructure-adapters/quickstart.md)); only the commands that differ are repeated here.

## 1. Stacks, keys, mail catcher

Start PostgreSQL and Redis from `infrastructure/components` (skip if they already run), create the development key pair under `services/identity-access-next/.local/`, export `IDENTITY_ACCESS_JWT_PRIVATE_KEY` and `IDENTITY_ACCESS_JWT_PUBLIC_KEY`, and start Mailpit exactly as in the 003 quickstart. The service must refuse to start without the keys. The old `presentation/user/.local/*.pem` files are not used.

## 2. Automated checks

```bash
cd services/identity-access-next
./mvnw test
```

Use the per-service wrapper, never a bare `mvn`. Use `test`, not `verify`: `identityaccess-start/pom.xml` binds a `check` goal that `spring-boot-maven-plugin` does not have, so `verify` stops at that module before its end-to-end tests run (an existing build issue, not part of this feature; `./mvnw verify -pl identityaccess-core,identityaccess-infrastructure,identityaccess-presentation` works). Expected: core, infrastructure, presentation, and `start` modules pass (395 tests when last run), including:

| Suite | Covers |
|-------|--------|
| Core service tests | rotation, `sid` on issued tokens (US2) |
| Redis adapter contract tests | rotation atomicity and race, revocation entries and their lifetime (SC-007, SC-009, SC-010) |
| Security tests | revoked and `sid`-less tokens, fail-closed `503`, public endpoints ignoring bad bearer tokens (SC-003, SC-008) |
| Presentation slice tests | every endpoint's statuses and bodies, error shape, no sentinel leakage (SC-002, SC-004) |
| ArchUnit presentation rules | no infrastructure or core `internal` imports (FR-026) |
| `start` end-to-end suite | full flows against real PostgreSQL and Redis containers, allow-list, secret leakage, OpenAPI lists all 11 operations (SC-001, SC-003, SC-005 to SC-010) |
| Spotless `check` | formatting |

## 3. Start the service

```bash
cd services/identity-access-next
./mvnw -pl identityaccess-start -am -DskipTests package
java -jar identityaccess-start/target/identityaccess-start-0.0.1-SNAPSHOT.jar
```

## 4. Walk the flows (US1, US2)

```bash
B=localhost:5001/api/v1
H='Content-Type: application/json'

curl -i -X POST $B/auth/register -H "$H" \
  -d '{"email":"a@example.com","username":"alice","birthday":"1999-01-01","password":"S3cure-pass!"}'   # 201 {"userId":...}
curl -i -X POST $B/auth/register -H "$H" \
  -d '{"email":"a@example.com","username":"alice","birthday":"1999-01-01","password":"S3cure-pass!"}'   # 409 verification_pending
curl -i -X POST $B/auth/authenticate -H "$H" -d '{"email":"a@example.com","password":"S3cure-pass!"}'   # 401 authentication_failed (not verified)
```

Open Mailpit (`http://localhost:8025`), follow the link (endpoint 4: `200 {"status":"verified"}`; again: `already_verified`). Then:

```bash
TOKENS=$(curl -s -X POST $B/auth/authenticate -H "$H" -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) Chrome/130.0' \
  -d '{"email":"a@example.com","password":"S3cure-pass!"}')
AT=$(echo "$TOKENS" | jq -r .accessToken); RT=$(echo "$TOKENS" | jq -r .refreshToken)
curl -s $B/sessions -H "Authorization: Bearer $AT" | jq        # one session, label like "Chrome on Linux (Desktop)"
```

Expected: the sign-in body has exactly two fields and no `Set-Cookie` header.

## 5. Refresh rotation (US2, SC-009)

```bash
NEW=$(curl -s -X POST $B/auth/refresh -H "$H" -H "Authorization: Bearer $AT" -d "{\"refreshToken\":\"$RT\"}")   # 200 pair; old AT header is ignored (FR-035)
curl -i -X POST $B/auth/refresh -H "$H" -d "{\"refreshToken\":\"$RT\"}"                                         # 401 authentication_failed (old credential dead)
curl -s $B/sessions -H "Authorization: Bearer $AT" | jq '.[0] | {expiresAt,lastUsedAt}'                          # expiry moved forward
```

## 6. Immediate sign-out (US2, SC-008)

Sign in twice (two sessions: `AT1/RT1`, `AT2/RT2`), then:

```bash
curl -i -X POST $B/auth/logout -H "$H" -d "{\"refreshToken\":\"$RT1\"}"                   # 204
curl -i $B/sessions -H "Authorization: Bearer $AT1"                                         # 401 unauthenticated, immediately
curl -i $B/sessions -H "Authorization: Bearer $AT2"                                         # 200, other device unaffected
redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:access-revoked:*'   # one entry
redis-cli --user admin --pass 123456 --no-auth-warning pttl "$(redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:access-revoked:*' | head -1)"   # at most 900000 ms
```

After the TTL passes the entry is gone while the session stays revoked: `POST /auth/refresh` with `RT1` still returns `401`.

## 7. Session management (US3)

```bash
curl -i -X POST $B/sessions/<otherSessionId>/revoke -H "$H" -H "Authorization: Bearer $AT2" -d '{"currentPassword":"wrong"}'   # 403 reauthentication_failed (not 401)
curl -i -X POST $B/sessions/<otherSessionId>/revoke -H "$H" -H "Authorization: Bearer $AT2" -d '{"currentPassword":"S3cure-pass!"}'   # 204
curl -i -X POST $B/sessions/does-not-exist/revoke -H "$H" -H "Authorization: Bearer $AT2" -d '{"currentPassword":"S3cure-pass!"}'   # 404 session_not_found
curl -s -X POST $B/sessions/revoke-others -H "$H" -H "Authorization: Bearer $AT2" -d '{"currentPassword":"S3cure-pass!"}'   # {"revokedCount":0} (repeat is 0)
```

## 8. Account (US4) and errors (US5)

```bash
curl -i -X PUT $B/users/@me/password -H "$H" -H "Authorization: Bearer $AT2" -d '{"currentPassword":"S3cure-pass!","newPassword":"An0ther-pass!"}'   # 204, all sessions revoked
curl -i $B/sessions -H "Authorization: Bearer $AT2"                                                                                               # 401
curl -i -X POST $B/auth/register -H "$H" -d '{bad json'                                                                                           # 400 malformed_request
curl -i -X POST $B/auth/register -H "$H" -d '{"email":"nope"}'                                                                                    # 400 validation_failed with errors[]
```

Every failure above has the same top-level fields (`status`, `code`, `message`, `timestamp`).

## 9. Documentation and secrets

- `curl -s localhost:5001/v3/api-docs | jq '.paths | length'` lists the endpoint paths; `http://localhost:5001/swagger-ui.html` renders without a credential.
- Search the service log for the passwords, the verification token, and `$RT`: none appear (SC-005).
- Stop Redis (`docker stop`), call a protected endpoint: `503 service_unavailable`. Start it again and the endpoint recovers. Public endpoints that need Redis (sign-in) fail with the standard `500`/`503` shape, never a stack trace.

## 10. Clean up

Same as the 003 quickstart (remove Mailpit, `docker compose down` each stack).

## Walkthrough record (2026-10-17)

Sections 3 to 9 were run against the packaged jar and the already-running `postgresql-chatter` and `redis-chatter` stacks, with a small local SMTP sink in place of Mailpit. Every step answered as documented; no deviation. Not run by hand: stopping Redis in section 9 (covered by `RevocationStoreFailureE2eTest`, which pauses the Redis container). Test accounts and their Redis keys were removed afterwards.
