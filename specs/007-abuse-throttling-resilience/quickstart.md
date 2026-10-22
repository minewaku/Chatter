# Quickstart: validating abuse throttling and outage resilience

Working directory for every build command: `services/identity-access-next/`. Use the per-service wrapper (`./mvnw`), never a bare `mvn`. Git and patch paths are relative to the repository root.

Docker must be running: the Redis adapter tests and the end-to-end suite start Redis and PostgreSQL containers.

## 1. Fast checks (no containers)

```bash
./mvnw -pl identityaccess-core test
```

Expected: all green, including `ReauthenticationGuardTest`, and the edited service tests run against `InMemoryAttemptLimiter`. They prove, per [spec.md](spec.md):

- Sign-in: the sixth attempt within the window is refused even with the right password; a success clears the count; an unknown email behaves the same (US1, FR-001 to FR-005).
- Confirmation: wrong passwords across change-password, close, email-change and the two session commands share one count; a correct one clears it; a refused attempt never reaches the password check (US2, FR-006, FR-007).
- Resend: the second request inside a minute and the sixth in an hour are refused and send nothing; a refused resend never touches the hourly count (US3, FR-008).
- Registration: when the notifier throws `MailDeliveryException` the account is still saved and the result says the mail was not sent (US4, FR-016).

## 2. Adapter and wiring checks (containers)

```bash
./mvnw -pl identityaccess-infrastructure test
```

Expected: green, including

- `RedisAttemptLimiterTest`: exactly `max` of 50 concurrent hits are allowed; a refused hit leaves the expiry unchanged; the stored key does not contain the raw subject; a paused Redis gives `DependencyUnavailableException`; an open breaker gives it without waiting.
- `MailDispatcherTest`: one failed logical send counts once; the breaker opens, rejects without calling the sender, and closes after the hold-off.
- `RevokedAccessTokenFilterTest`: an open breaker is a `503`, never a pass-through.
- `DependencyHealthIndicatorsTest`: `UP` when closed, `DEGRADED` when open or half-open, no details.
- `PortAdapterMapTest` and `ArchitectureTest`: the new port has exactly one adapter in `throttle`; core imports no Redis, mail, or resilience type; `LoggingRuleTest` still passes.

## 3. End to end

```bash
./mvnw -pl identityaccess-start -am test
```

`./mvnw verify` currently fails at the very end, after every test has passed, because `identityaccess-start/pom.xml` runs a `check` goal of `spring-boot-maven-plugin` that does not exist (present since commit `2d97fea`, unrelated to this feature). Use `test` until that is fixed.

Expected: the existing suite passes unchanged (SC-009), plus:

| Test | Proves |
|------|--------|
| `ThrottlingE2eTest` | `429 too_many_attempts` with a positive `Retry-After` for sign-in (also for an unknown email, same body), password confirmation (and that the session stays valid), resend, and email change; recovery after the window (the test config uses a short window) |
| `MailOutageE2eTest` | With the recording sender set to fail: registration answers `201` with `verificationMailSent: false` inside 2 seconds and the account is pending; resend still answers `202`; email change answers `503 mail_unavailable` and the email is unchanged; after the sender recovers and the hold-off ends, a resend delivers mail without a restart |
| `RevocationStoreFailureE2eTest` | With Redis paused: signed-in requests are `503` within 2 seconds, none succeed; sign-in is `503`; after unpausing, requests succeed again with no restart |

## 4. Manual check against a running service

Start the shared stack and the service as in the repository README (Redis from `infrastructure/components/redis`). Then, with a registered and verified account:

```bash
# six wrong sign-ins for the same email, from the same shell
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:5001/api/v1/auth/authenticate \
    -H 'Content-Type: application/json' -d '{"email":"you@example.com","password":"wrong-Password-1"}'
done
```

Expected: five `401`, then `429`. A seventh request with the correct password is also `429` until the `Retry-After` has passed. Then:

```bash
curl -s http://localhost:5001/actuator/health | jq .components
```

Expected: `mailDelivery` and `sessionStore` both `UP` (next to Spring's own `db`, `redis`, `diskSpace`, and so on, see research section 11). Pause Redis (`docker pause <redis container>`), send a few signed-in requests, and the `sessionStore` component reads `DEGRADED` while the responses are fast `503`s; `docker unpause` and it returns to `UP` after the hold-off.

## 5. Format and style

```bash
./mvnw spotless:check
```

## Traceability

| Spec item | Where it is proven |
|-----------|--------------------|
| US1, FR-001 to FR-005, SC-001, SC-003 | core `AuthenticateServiceTest`, `ThrottlingE2eTest` |
| US2, FR-006, FR-007, SC-002 | `ReauthenticationGuardTest`, `ThrottlingE2eTest` |
| US3, FR-008 to FR-010, SC-004 | `RequestEmailVerificationServiceTest`, `ThrottlingE2eTest` |
| US4, FR-015 to FR-018, SC-005, SC-007 | `MailDispatcherTest`, `MailOutageE2eTest` |
| US5, FR-019, FR-020, SC-006 | `RevokedAccessTokenFilterTest`, `RevocationStoreFailureE2eTest` |
| US6, FR-021, SC-008 | `DependencyHealthIndicatorsTest`, manual check 4 |
| FR-012 | `RedisAttemptLimiterTest` concurrency case |
| FR-013 | `OpenApiE2eTest` (the new codes appear on each endpoint) |
| FR-014 | `RedisAttemptLimiterTest`, `ThrottlingE2eTest` with Redis paused |
| FR-022 | `ThrottlePropertiesTest`, `ResiliencePropertiesTest` (defaults and invalid values) |
| FR-023 | The unchanged existing suites |
