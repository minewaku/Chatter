# Contract: configuration

All in `identityaccess-start/src/main/resources/application.yml`, all with the defaults shown, all overridable by environment variable in the same style as the existing keys. Invalid values stop startup with a message naming the property (same behavior as `SessionProperties`).

```yaml
identity-access:
  throttle:
    sign-in:               { max-attempts: 5, window: PT15M }
    reconfirmation:        { max-attempts: 5, window: PT15M }
    resend-gap:            { max-attempts: 1, window: PT1M }
    resend-hourly:         { max-attempts: 5, window: PT1H }
    email-change-request:  { max-attempts: 3, window: PT1H }
  resilience:
    mail:
      window-size: 10
      minimum-calls: 5
      failure-rate-percent: 50
      open-duration: PT30S
      trial-calls: 2
    session-store:
      window-size: 20
      minimum-calls: 10
      failure-rate-percent: 50
      open-duration: PT10S
      trial-calls: 3

spring:
  data:
    redis:
      timeout: 1s            # command timeout (Lettuce default is 60s)
      connect-timeout: 1s

management:
  endpoint:
    health:
      show-components: always          # names and status of every component, not only the two new ones; details stay hidden
      status:
        order: DOWN,OUT_OF_SERVICE,DEGRADED,UP,UNKNOWN
        http-mapping:
          DEGRADED: 200
  health:
    mail:
      enabled: false                   # unchanged: Spring's own mail probe stays off; our mailDelivery component replaces it
```

## Validation rules

- Throttle: `max-attempts >= 1`; `window > 0`; durations are ISO-8601.
- Resilience: `window-size >= 1`; `minimum-calls` between 1 and `window-size`; `failure-rate-percent` between 1 and 100; `open-duration > 0`; `trial-calls >= 1`.

## Dependency

`pom.xml` (parent) gains `<resilience4j.version>2.4.0</resilience4j.version>` and a `dependencyManagement` entry for `io.github.resilience4j:resilience4j-circuitbreaker`. `identityaccess-infrastructure/pom.xml` declares it. No other module depends on it, and `identityaccess-core` must not.

## Environment

No new environment variable is required. Redis timeouts apply to every Redis call, not only the guarded ones. The Redis development ACL user and the Vault-sourced mail credentials are unchanged.
