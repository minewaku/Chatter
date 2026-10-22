# Contract: core and infrastructure changes

Signatures are the contract; bodies are for the implementation phase. Packages are relative to `com.minewaku.chatter.identityaccess`.

## New core port: `shared.port.AttemptLimiter`

```java
public interface AttemptLimiter {

    /** Counts this attempt and says whether it is within the policy. Atomic: concurrent calls cannot both see room. */
    AttemptOutcome hit(AttemptPolicy policy, String subject);

    /** Forgets the subject's count for this policy (after a success). */
    void clear(AttemptPolicy policy, String subject);
}
```

- Throws `DependencyUnavailableException` if the count cannot be read or written, or the breaker is open. It never returns "allowed" because it could not check.
- The subject is a plain string; the adapter hashes it. The port never logs it.
- One adapter, `infrastructure.adapter.outbound.throttle.RedisAttemptLimiter`. `StructureRules.PORT_MAP` gains the row `shared.port.AttemptLimiter, RedisAttemptLimiter, throttle`.

## New core types (`shared.throttle`, `shared.exception`)

```java
public record AttemptPolicy(String name, int maxAttempts, Duration window) {}   // validated in the constructor
public record AttemptOutcome(boolean allowed, Duration retryAfter) {}
public record ThrottlePolicies(
        AttemptPolicy signIn, AttemptPolicy reconfirmation, AttemptPolicy resendGap,
        AttemptPolicy resendHourly, AttemptPolicy emailChangeRequest) {}

public final class TooManyAttemptsException extends RuntimeException { Duration retryAfter(); }
public final class DependencyUnavailableException extends RuntimeException { Duration retryAfter(); }
public class MailDeliveryException extends RuntimeException {}   // thrown by both notifier adapters
```

`ThrottlePolicies` is provided as a bean by `ThrottleConfiguration` in infrastructure (core only declares the type), like the `Duration` bean for sessions. Exceptions take no message that could carry a subject or address.

## New core class: `user.internal.application.ReauthenticationGuard`

```java
@Component
public class ReauthenticationGuard {
    public ReauthenticationGuard(AttemptLimiter limiter, ThrottlePolicies policies) {...}

    /** Hits the policy for this user, runs the action, clears the count unless the password was wrong. */
    public <R> R confirm(long userId, Supplier<R> action) {...}
}
```

Behavior:

1. `hit(reconfirmation, userId)`; if not allowed, throw `TooManyAttemptsException(retryAfter)` without running the action.
2. Run the action.
3. If it throws `InvalidCredentialsException` or `AuthenticationFailedException`, rethrow and keep the count.
4. Otherwise (returned, or any other exception such as a conflict) call `clear` and pass the outcome through.

Users: `ChangePasswordService`, `SoftDeleteUserAccountService`, `RequestEmailChangeService` (its whole `handle`), and `AccountVerifier` (so `LogoutSessionService` and `LogoutOtherSessionsService` are covered through it).

## Edited core services

| Class | Change |
|-------|--------|
| `AuthenticateService` | After parsing the email: `hit(signIn, email.value())`; refuse with `TooManyAttemptsException` if not allowed. On success `clear`. A malformed email stays an `AuthenticationFailedException` and is not counted |
| `RequestEmailVerificationService` | For a parsed email: `hit(resendGap, …)`, refuse if not allowed; then `hit(resendHourly, …)`, refuse if not allowed; then the existing lookup and issue. Catch `MailDeliveryException` from the issuance and return `Accepted` anyway |
| `RequestEmailChangeService` | `handle` runs inside the guard. After the password is accepted and the new address is free, `hit(emailChangeRequest, userId)` before `issuer.issue`. `MailDeliveryException` propagates |
| `RegisterUserService` | Wrap `verificationIssuance.issue(...)` in a try/catch for `MailDeliveryException`; return `new Registered(id, false)` on failure and `new Registered(id, true)` otherwise |
| `RegisterUserUseCase.Registered` | `record Registered(long userId, boolean verificationMailSent)` |

## Infrastructure

### `RedisAttemptLimiter`

```java
@Repository
class RedisAttemptLimiter implements AttemptLimiter {
    RedisAttemptLimiter(StringRedisTemplate redis, RedisKeyProperties keys, CircuitBreaker sessionStoreBreaker) {...}
}
```

- Key: `keys.keyPrefix() + "throttle:" + policy.name() + ":" + TokenDigest.sha256Hex(subject)`.
- `hit`: runs `throttle-hit.lua` inside `sessionStoreBreaker`; maps `CallNotPermittedException` and `DataAccessException` to `DependencyUnavailableException(retryAfter = breaker wait)`.
- `clear`: `DEL` inside the breaker, same mapping.

### `throttle-hit.lua`

```lua
-- KEYS[1] counter key; ARGV[1] window in milliseconds
-- returns { count, remainingMillis }
local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
end
local ttl = redis.call('PTTL', KEYS[1])
if ttl < 0 then
  redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
  ttl = tonumber(ARGV[1])
end
return { count, ttl }
```

### `MailDispatcher` (package-private, mail adapter package)

```java
@Component
class MailDispatcher {
    MailDispatcher(JavaMailSender sender, CircuitBreaker mailBreaker) {...}   // builds the RetryTemplate itself

    /** breaker( retry( send ) ). Throws MailDeliveryException for a failure after retries or an open breaker. */
    void send(MimeMessage message) {...}
}
```

`MailVerificationNotifier` and `MailEmailChangeNotifier` keep building the message and call `dispatcher.send(message)`; their `@Retryable` and the `MailException | MessagingException` translation move into the dispatcher (message-building errors still become `MailDeliveryException`). The existing `VerificationMailException` is kept only if retry needs a narrower type; otherwise it is replaced by core's `MailDeliveryException`.

### Resilience wiring

```java
@ConfigurationProperties("identity-access.resilience")
record ResilienceProperties(BreakerProperties mail, BreakerProperties sessionStore) {}
// BreakerProperties: windowSize, minimumCalls, failureRatePercent, openDuration, trialCalls (validated, defaulted)

@Configuration
class ResilienceConfiguration {
    @Bean CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties p) {...}
    @Bean("mailBreaker") CircuitBreaker mailBreaker(CircuitBreakerRegistry r) {...}
    @Bean("sessionStoreBreaker") CircuitBreaker sessionStoreBreaker(CircuitBreakerRegistry r) {...}
}
```

No `Duration` bean is added (existing rule): durations stay inside properties records.

### `DependencyHealthIndicators`

Two `HealthIndicator` beans, named `mailDelivery` and `sessionStore`: `Health.up()` when the breaker is `CLOSED`, otherwise `Health.status("DEGRADED").build()` with no details.

### `RevokedAccessTokenFilter`

The constructor gains an optional breaker. The one `redis.hasKey(...)` call becomes `breaker.executeSupplier(() -> redis.hasKey(...))` when a breaker is present. The existing `catch (RuntimeException e)` already turns a `CallNotPermittedException` into `AuthenticationServiceException` and so into `503`. `SecurityConfiguration` passes the breaker from an `ObjectProvider<CircuitBreaker>` using the `sessionStoreBreaker` qualifier, and the infrastructure security tests, which build the chain without it, keep working.

## Presentation

```java
// ErrorCode
TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later."),
MAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The email could not be sent right now. Try again later."),

// ApiErrors
public static ResponseEntity<ApiError> response(ErrorCode code, Duration retryAfter)   // adds Retry-After, ceil seconds, at least 1

// ApiExceptionHandler
@ExceptionHandler(TooManyAttemptsException.class)           -> 429 too_many_attempts + Retry-After
@ExceptionHandler(DependencyUnavailableException.class)     -> 503 service_unavailable + Retry-After
@ExceptionHandler(MailDeliveryException.class)              -> 503 mail_unavailable + Retry-After (fixed default)
```

The handler ordering matters: `IllegalArgumentException` has a handler, and none of the three new exceptions extends it. `RegisterResponse` becomes `record RegisterResponse(long userId, boolean verificationMailSent)`; `AuthController.register` passes `registered.verificationMailSent()`.

## Test contracts

- `InMemoryAttemptLimiter` (core test) and `RedisAttemptLimiter` pass the same assertions: first hit allowed, over the limit refused with a positive `retryAfter` no longer than the window, `clear` resets, different subjects and different policies are independent.
- `RedisAttemptLimiterTest` additionally proves: 50 concurrent hits against a limit of 10 allow exactly 10; a refused hit does not extend the expiry; the stored key contains no part of the raw subject; a paused store gives `DependencyUnavailableException`; an open breaker gives it without waiting on the store.
- `MailDispatcherTest` proves: one logical send that fails after its retries counts once toward the breaker; the breaker opens after the configured failures; an open breaker throws `MailDeliveryException` without calling the sender; after the open duration a good send closes it.
