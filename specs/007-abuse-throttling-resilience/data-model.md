# Data Model: Abuse Throttling and Outage Resilience

Nothing is added to PostgreSQL. All new state is short-lived and lives in Redis or in memory. Names below are the spec's key entities.

## Attempt Counter (Redis)

One integer per throttle subject and policy. It exists only while its window is open.

| Part | Value |
|------|-------|
| Key | `{prefix}throttle:{policy}:{sha256hex(subject)}` where `{prefix}` is the existing `identity-access:` key prefix |
| Value | Decimal count of attempts in the current window |
| Expiry | The policy window, set on the first attempt and never extended (a blocked attempt does not extend it) |
| Removed by | Expiry, or `clear` after a successful sign-in or a correct password confirmation |

Rules:

- The subject is never stored; only its SHA-256 digest (same helper as the token digests).
- Increment and first-hit expiry happen in one script call, so two concurrent attempts cannot both see "below the limit" (FR-012).
- If a key somehow has no expiry, the script sets one, so a counter cannot become a permanent block.
- Losing the key (a flushed store) only starts the window over.

## Throttle Policy (configuration, immutable)

`AttemptPolicy(name, maxAttempts, window)`. `ThrottlePolicies` groups the five that the services use:

| Policy name | Subject | Default | Used by |
|-------------|---------|---------|---------|
| `sign-in` | Normalized email | 5 per 15 min | `AuthenticateService` |
| `reconfirmation` | User id | 5 per 15 min | `ReauthenticationGuard` (change password, close account, email-change request, two session commands) |
| `resend-gap` | Normalized address | 1 per 1 min | `RequestEmailVerificationService`, checked first |
| `resend-hourly` | Normalized address | 5 per 1 h | `RequestEmailVerificationService`, checked second |
| `email-change-request` | User id | 3 per 1 h | `RequestEmailChangeService` |

Validation (at startup, same style as `SessionProperties`): `maxAttempts >= 1`, `window` positive, and every policy has a distinct name. Missing values fall back to the defaults above.

## Attempt Outcome (in-memory)

`AttemptOutcome(allowed, retryAfter)`. `allowed` is `count <= maxAttempts`. `retryAfter` is the remaining window and is meaningful when `allowed` is false; it is never longer than the policy window.

## Throttle Subject

A string the service chooses; the adapter hashes it. It is a normalized email or a decimal user id. It never contains a password or token.

## Dependency Health (in-memory)

Two named circuit breakers, each with a state and a failure rate over a sliding count window.

| Breaker | Guards | States | Surfaces as |
|---------|--------|--------|-------------|
| `mail` | Every send in `MailDispatcher` (retry runs inside it) | CLOSED, OPEN, HALF_OPEN | Health component `mailDelivery` |
| `sessionStore` | The revocation lookup and every `RedisAttemptLimiter` call | CLOSED, OPEN, HALF_OPEN | Health component `sessionStore` |

State transitions follow the library's rules: CLOSED opens when the failure rate reaches the threshold after the minimum number of calls; OPEN rejects immediately until the wait ends; HALF_OPEN lets a few trial calls through and closes or reopens depending on their result. The state is per instance and is not persisted.

Health mapping: CLOSED is `UP`; OPEN and HALF_OPEN are `DEGRADED`.

## Exceptions (core, `shared.exception`)

| Type | Meaning | Carries | Mapped to |
|------|---------|---------|-----------|
| `TooManyAttemptsException` | A policy refused the attempt | `retryAfter` | `429 too_many_attempts`, `Retry-After` |
| `DependencyUnavailableException` | The counter store could not be used (failure or open breaker) | `retryAfter` | `503 service_unavailable`, `Retry-After` |
| `MailDeliveryException` | A notifier could not deliver (failure after retries, or breaker open) | none | Registration: caught; resend: caught; email change: `503 mail_unavailable`, `Retry-After` |

None of them carries a subject, address, token, or link.

## Result type change

`RegisterUserUseCase.Registered(long userId)` becomes `Registered(long userId, boolean verificationMailSent)`. The other two results are unchanged. The HTTP body mirrors it (see [contracts/http-surface.md](contracts/http-surface.md)).

## State transitions worth testing

- Counter: absent, then count 1 with expiry, then count up to max (allowed), then max+1 and above (refused, expiry unchanged), then gone (window end or clear).
- Re-confirmation: wrong password increments and stays; correct password clears; a refused attempt never reaches the password check.
- Mail breaker: closed, then open after repeated failures, then half-open after the wait, then closed after good trial calls (or open again after a bad one).
