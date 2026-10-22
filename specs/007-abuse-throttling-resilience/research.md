# Research: Abuse Throttling and Outage Resilience

No `NEEDS CLARIFICATION` remained in the plan. These are the decisions the design rests on, each checked against the current code.

## 1. Circuit breaker: resilience4j as a plain library

- **Decision**: Add `resilience4j-circuitbreaker` 2.4.0 (the plain module) and create the two breakers in code from a `CircuitBreakerRegistry`, configured from `identity-access.resilience.*`. Do not add the Spring Boot starter, AOP, or annotations.
- **Rationale**: The starters in this repo (`api-gateway`, `identity-access`) target Spring Boot 3; this service is on Boot 4.0.8 / Spring Framework 7, and I could not confirm the starter's auto-configuration against it. The plain module has no Spring dependency, so the question does not arise. Two breakers around two known calls do not need annotations, and annotations would put a library type on core services, which `StructureRules` rule 1 forbids. Spring Framework 7 ships `@Retryable` and `RetryTemplate` but no circuit breaker, so the gap is real.
- **Alternatives**: (a) Hand-written breaker: a state machine with half-open handling is the kind of code that is easy to get subtly wrong, and the library is small. (b) Spring Cloud CircuitBreaker: adds an abstraction layer with no second implementation to swap in. (c) The starter plus annotations, as in the old service: rejected for the compatibility and layering reasons above.

## 2. Throttling: Redis counters, not a Resilience4j `RateLimiter` and not Bucket4j

- **Decision**: A fixed-window counter per `(policy, subject)` in Redis, implemented by one Lua script (`INCR`, set expiry on the first hit, return count and remaining milliseconds).
- **Rationale**: The limits are keyed by business identity (an email, a user id), which the Resilience4j `RateLimiter` does not model (it is per-instance and in memory), so it fails the multi-instance assumption in the spec. Bucket4j is already in the gateway, but a token bucket's smoothing is not wanted here: "5 failures per 15 minutes" is a count in a window. A single script is the same technique the repository already uses for verification and sessions (`RedisScripts`, `redis/*.lua`), so there is nothing new to learn or test differently.
- **Alternatives**: `INCR` then `EXPIRE` from the client (two round trips, and a crash between them leaves a counter that never expires, which would be a permanent lock-out for that subject); sliding window log (more memory, no benefit at these volumes).

## 3. Hit first, clear on success (not check, then record on failure)

- **Decision**: Every attempt increments the counter *before* the work runs; the attempt is refused if the count is above the maximum; success deletes the counter.
- **Rationale**: FR-012 says concurrent requests must not exceed a limit. "Check, do work, record the failure" lets N concurrent requests all pass the check before any failure is recorded. Counting first makes the increment itself the gate, and the script's atomicity is the only thing the guarantee depends on. The cost is that a successful attempt is briefly counted and then cleared, which is harmless.
- **Consequences**: A blocked attempt still increments the count, but the expiry is only set on the first hit, so an attacker who keeps hammering cannot extend the lock past the original window. The cooling-off period of the spec is therefore the remainder of the policy window, not a separate timer. This is one duration per policy instead of two, and satisfies FR-022.
- **Malformed input**: A sign-in with an unparseable email never gets a subject, so it is refused as today and never counted (FR-011). Controller-level validation (`@Valid`) runs before core and is likewise never counted.

## 4. What the subject is, and why it is hashed

- **Decision**: Sign-in: the normalized email (`Email.value()`; the endpoint signs in by email, so "username" in the spec applies if sign-in by username is ever added). Password confirmation and email-change requests: the user id. Resend: the normalized address. The Redis key holds the SHA-256 digest of the subject, never the subject itself, via the existing `TokenDigest`.
- **Rationale**: Keys can be listed by anyone with Redis access and show up in monitoring output; a digest keeps addresses out of both. The attacker chooses sign-in and resend subjects, so unbounded distinct keys are possible, but each expires with its window, so the memory used is bounded by the request rate the gateway already limits.
- **No enumeration (SC-003)**: The limiter is called with the subject whether or not an account exists, and the refusal looks identical. The existing timing difference between a real and a missing account (a password hash is computed only for real accounts) is pre-existing, and the limiter runs before it, so it does not add one.

## 5. One guard for all password-confirmed actions

- **Decision**: A `ReauthenticationGuard` in `user.internal.application` runs an action under the `reconfirmation` policy keyed by user id: hit, refuse if over, run, clear unless the action threw `InvalidCredentialsException` (or, inside `AccountVerifier`, `AuthenticationFailedException`). It is used by change password, close account, email-change request, and by `AccountVerifier`, which the two password-confirmed session commands (`LogoutSessionService`, `LogoutOtherSessionsService`) use.
- **Rationale**: FR-006 asks for one count per person across all confirmed actions. The spec names three actions; the code has five places that check the current password, and the session ones are the same oracle. Routing all five through one class means a sixth cannot forget the rule, and the count cannot differ per action. This is wider than the three actions listed in the spec, but narrower than "all actions", and the spec's own wording ("password-confirmed actions") covers it.
- **Why clear on any outcome except a wrong password**: By the time a business rule such as "email already taken" fires, the password has been proven right. Clearing then is correct and means a legitimate user is not penalized for conflicts.
- **Transactions**: `ChangePasswordService`, `SoftDeleteUserAccountService`, and `AuthenticateService` are `@Transactional`, and a thrown exception rolls back PostgreSQL. Redis is not part of that transaction, so the recorded failure survives the rollback, which is what is wanted.

## 6. Resend and email-change request limits

- **Decision**: Resend runs two policies in order, both keyed by the normalized address: `resend-gap` (1 per 60 s), then `resend-hourly` (5 per 3600 s). A request refused by the gap does not touch the hourly counter. Email-change request is one policy (`email-change-request`, 3 per hour) keyed by user id, hit just before the token is issued, so a wrong password, an unchanged address, or a taken address never consumes it.
- **Resend answers `429`**: The existing contract says resend is "always accepted" (`202`) to avoid revealing which addresses have accounts. A `429` keyed only by the address, and applied to every well-formed address whether or not it has an account, reveals nothing about existence, so spec FR-008 and FR-010 are satisfied together. The alternative, silently returning `202` and sending nothing, hides the limit from legitimate users who retry in a loop, so it was not chosen. The "amended by 007" note on spec 004's contract records this.

## 7. Mail: breaker outside, retry inside, one dispatcher

- **Decision**: A package-private `MailDispatcher` in the mail adapter package does `breaker.executeRunnable(() -> retry.invoke(() -> mailSender.send(message)))`. Both notifiers build their message and call it. The `@Retryable` annotations and `MailConfiguration` (`@EnableResilientMethods`) are removed. Retry parameters are unchanged: 3 retries, 200 ms, multiplier 2, only for `VerificationMailException`-class failures. The dispatcher throws core's `MailDeliveryException` for both a real failure and a breaker that is open.
- **Rationale**: A breaker must see one result per logical send, not one per attempt, so it has to be outside the retry. A Spring `@Retryable` proxy cannot be wrapped from inside its own method, and a decorator bean that wraps a notifier would make a second implementation of `VerificationNotifier` and `EmailChangeNotifier`, which `PortAdapterMapTest` forbids (exactly one adapter per port). Spring Framework 7's `RetryTemplate` does the same job programmatically with the same parameters. I confirmed `RetryTemplate.invoke(Runnable)` and `RetryPolicy.builder()` (`maxRetries`, `delay`, `multiplier`, `includes`) exist in 7.0.9; whether `invoke` rethrows the original exception or a wrapper is checked by `MailDispatcherTest`, and the dispatcher unwraps if needed.
- **What counts as a failure**: Anything the sender throws after the retries. A message that cannot be built is a programming error, not an outage, and is not counted.

## 8. What each caller does when mail is unavailable

| Caller | Today | Decision |
|--------|-------|----------|
| Registration (`RegisterUserService`, `@Transactional`) | The exception propagates, the transaction rolls back, the person gets `500` and no account | Catch `MailDeliveryException` inside the service, so the transaction commits. Return `Registered(userId, verificationMailSent=false)`; the response is still `201` with `verificationMailSent: false` (FR-016) |
| Resend (`RequestEmailVerificationService`) | `500` | Catch and swallow: always `202`. A `503` only for accounts that need mail would reveal which addresses are pending, undoing the enumeration protection. The person can resend again |
| Email-change request (`RequestEmailChangeService`) | `500` | Let it propagate: `503 mail_unavailable` with `Retry-After`. The email is unchanged because the change only takes effect on confirmation, and the unused token in Redis is replaced by the next request (FR-017) |

The mail call in registration happens inside a database transaction and can hold a connection for up to four attempts of up to 5 s each. Moving it out of the transaction is a separate, larger change and is not part of this feature; the breaker bounds how often it can happen.

## 9. Redis: short timeouts, one breaker on the request path

- **Decision**: Set `spring.data.redis.timeout` and `connect-timeout` to 1 s (Lettuce's default command timeout is 60 s, which is what makes an outage slow today). Wrap two things in the `sessionStore` breaker: the revocation lookup in `RevokedAccessTokenFilter`, and every call of `RedisAttemptLimiter`. The session and verification repositories are not wrapped.
- **Rationale**: Those are the two calls on the hot path (every signed-in request, and every sign-in and confirmation). They share one breaker, so a store outage is detected once and both fail fast together. Wrapping the session and verification repositories would add more guarded calls for flows that already sit behind one of the two wrapped calls and are bounded by the 1 s timeout.
- **Fail closed**: The filter already turns any `RuntimeException` into `AuthenticationServiceException` and then `503`; a `CallNotPermittedException` is a `RuntimeException`, so the existing catch covers it with no change to the fail-closed logic. In the limiter, a store failure or an open breaker becomes `DependencyUnavailableException`, mapped to `503` (FR-014); an unreadable counter is never read as "zero attempts".
- **Retry-After**: The filter's entry point already answers `Retry-After: 5`. The two new `503` responses use the breaker's wait duration, rounded up to whole seconds.

## 10. Breaker settings

- **Decision**: Count-based windows, configured per breaker:
  - `mail`: window 10, minimum 5 calls, failure rate 50 %, open for 30 s, 2 trial calls.
  - `sessionStore`: window 20, minimum 10 calls, failure rate 50 %, open for 10 s, 3 trial calls.
- **Rationale**: Mail is slow to fail (each failed send already burns about 4 attempts) and slow to recover, so it opens on a small window and stays open longer. The store is hit on every request, so it needs a larger window to avoid opening on a couple of blips, and a short hold so a recovery is noticed quickly (SC-007). Both are properties, so tuning needs no code change (FR-022).

## 11. Health

- **Decision**: Two `HealthIndicator` beans, `mailDelivery` and `sessionStore`, report `UP` when the breaker is closed and a custom `DEGRADED` status when it is open or half-open. In `application.yml`: add `DEGRADED` to the status order (after `OUT_OF_SERVICE`, before `UP`), map it to HTTP `200`, and set `management.endpoint.health.show-components: always` (names and statuses only, no details).
- **Rationale**: The spec asks for "degraded", not "down", and a `DOWN` here would make an orchestrator restart a healthy instance because a mail provider is slow. Component names are shown without details so the output carries no credentials or recipients (FR-021). The health endpoint is public, so showing component status to anonymous callers is a minor information disclosure, accepted for a personal project and easy to tighten later with a health group.
- **Observed after implementation**: `show-components` is a switch for the whole endpoint, not for one component. With it on, the public health response also names and gives the status of every other component (`db`, `redis`, `diskSpace`, `ssl`, ...), not only the two new ones; before this feature only the overall status was shown. Details stay hidden. This is a small widening of what an anonymous caller sees. If it is unwanted, restrict `/actuator/health` to authenticated callers or expose only the new components through a health group.
- **Alternative**: Resilience4j's own Actuator health indicator: part of the Boot starter, rejected with it in section 1.

## 12. New error codes and the existing 503 gap

- **Decision**: `TOO_MANY_ATTEMPTS` (429, "Too many attempts. Try again later.") and `MAIL_UNAVAILABLE` (503, "The email could not be sent right now. Try again later."). Both carry `Retry-After`. `DependencyUnavailableException` maps to the existing `SERVICE_UNAVAILABLE`.
- **Rationale**: Messages are fixed per code and never echo input (existing rule in `ErrorCode`). A distinct `MAIL_UNAVAILABLE` lets a client say "resend later" instead of a generic outage.
- **Gap found, not fixed here**: Several endpoints already document `SERVICE_UNAVAILABLE`, but a Redis failure inside an application service (not the filter) surfaces today as `500 internal_error`, because only the security chain maps it. This feature maps only the new exception types. Broadening that mapping to all Redis and database failures is a separate decision.

## 13. Out of scope, confirmed

- Gateway fixes (rule-aware bucket key, trusted forwarded address): another service, already excluded by the spec.
- Time limiter, bulkhead, database breaker: excluded by the spec; mail already has socket timeouts, and Hikari bounds database waits.
- Moving the mail send out of the registration transaction: noted in section 8.
