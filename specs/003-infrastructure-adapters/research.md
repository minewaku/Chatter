# Research: Identity Access Infrastructure Adapters

**Feature**: [spec.md](spec.md) | **Date**: 2026-10-14

Sources inspected: `services/identity-access/identityaccess-infrastructure` (old), `services/identity-access-next` core ports/models and `infrastructure-structure-design.txt`, `services/message/.../SecurityConfig.java`, `infrastructure/components/*`, and Maven Central metadata for Spring Boot 4.0.8 (Spring Framework 7.0.9, Spring Security 7.0.7, Flyway 11.14.1). The spec had no `[NEEDS CLARIFICATION]` markers; the items below resolve the open points the spec deferred to planning, and record what the code inspection found.

## 1. Platform findings that change the existing build

### 1.1 Flyway will not run with the current dependencies
- **Finding**: The parent is Spring Boot 4.0.8. Boot 4 split auto-configuration into per-technology modules, and `spring-boot-starter-flyway` (which brings `spring-boot-flyway`) exists on Central. The current infrastructure `pom.xml` declares only `flyway-core`, so `V1__create_user_account.sql` would never be applied even though `spring.flyway.enabled: true` is set.
- **Decision**: Replace `flyway-core` with `spring-boot-starter-flyway`; keep `flyway-database-postgresql` (runtime).
- **Alternatives**: Run Flyway programmatically in a `@Bean` (rejected: re-implements what the starter does).

### 1.2 Starter names
- **Finding**: `spring-boot-starter-web` is a deprecated alias of `spring-boot-starter-webmvc` in Boot 4. `spring-boot-starter-oauth2-resource-server` still resolves (it pulls `spring-boot-security-oauth2-resource-server`).
- **Decision**: Use `spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-jdbc`, `spring-boot-starter-flyway`, `spring-boot-starter-data-redis`, `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator`.
- **Alternatives**: Keep the deprecated alias (rejected: it will be removed in a later Boot line).

### 1.3 Retry needs no extra library
- **Finding**: Spring Framework 7 ships `org.springframework.resilience.annotation` (`@EnableResilientMethods`, with `@Retryable`) in `spring-context` and `org.springframework.core.retry.RetryTemplate` in `spring-core`. The old service used Resilience4j `@Retry`.
- **Decision**: Use Spring's built-in `@Retryable` on the mail notifier, enabled once in `integration/mail`. Do not add Resilience4j.
- **Alternatives**: Resilience4j (rejected: extra dependency for one annotation).

### 1.4 `AuthenticateService` needs a `Duration` bean
- **Finding**: `AuthenticateService`'s only constructor takes `Duration sessionLifetime` with no `@Value`/`@Autowired`. Spring can only wire it if exactly one `Duration` bean exists (or by parameter name, which works because the build uses `-parameters`).
- **Decision**: Infrastructure exposes one bean `Duration sessionLifetime` built from `identity-access.session.lifetime`. No other `Duration` beans may be registered; timeouts and retention are read from `@ConfigurationProperties` objects instead.
- **Alternatives**: Change the core constructor (out of scope: core is unchanged by this feature).

## 2. Persistence structure (the user's open question)

- **Decision**: Capability → role → technology, exactly as in `infrastructure-structure-design.txt`:
  `adapter/outbound/<capability>/<write|read>[/cache]/<technology>`; non-storage adapters sit directly under their capability. Per-technology connection setup lives in `integration/<technology>`.
- **Rationale**: One capability's adapters are found in one place; the CQS split is visible in folder names; adding a Redis cache over a JDBC read adapter adds one folder and moves nothing.
- **Alternatives**:
  - Technology first (old layout: `persistence/postgresql/{entity,mapper,impl}`): scatters one capability across folders, hides read vs write, and encourages shared "mega" repositories.
  - Role first (`write/user/jdbc`): scatters each capability across the tree.
- **Deviation from the design doc**: the doc places ID generators under `user/id/`. In core they are shared ports (`shared.port.TimeBasedIdGenerator`, `shared.port.UniqueStringIdGenerator`) used by user, session, and verification, so adapters go in `adapter/outbound/shared/id/`. Core port names also differ from the doc's draft names: `UserAccountQueryRepository` (not `...ViewRepository`) and `UniqueStringIdGenerator` is reused as the verification-token source instead of a separate `VerificationTokenGenerator`.
- **Folder names versus core capabilities**: core capabilities are `user`, `authentication`, `emailverification`; adapter folders keep the design doc's `user`, `authentication`, `session`, `verification`, because the session and verification stores are distinct adapters of the `authentication` and `emailverification` capabilities. The mapping port → folder is fixed in [contracts/adapter-map.md](contracts/adapter-map.md) and enforced by the structure test.

## 3. Persistence objects and mappers (clarified: Option A)

- **Decision**: Separate persistence-only records inside each adapter package, hand-written mappers, no storage annotations in core.
  - **JDBC write side**: package-private `UserAccountRow` record annotated with Spring Data JDBC (`@Table`, `@Id`, `@Version`) plus a package-private `CrudRepository`. Core `UserAccount.version()` is a nullable `Long` where `null` means "new", which is exactly Spring Data JDBC's `@Version` convention, so optimistic locking and insert/update detection come for free.
  - **JDBC read side**: `JdbcClient` with explicit SQL and a row mapper that builds the core result records (`FindUserByIdUseCase.Result`, `FindUserByEmailUseCase.Result`) directly. No intermediate class.
  - **Redis**: package-private record per structure (`SessionRecord`, `VerificationRecord`) converted to and from string maps by a mapper. The session view repository maps directly to `FindSessionsQuery.SessionView`.
- **Cost**: One account mapping copies about 10 fields and allocates a few small objects (about 0.1–5 µs). A local database round trip is about 0.3–2 ms, and an Argon2 hash is tens to hundreds of ms. SC-010 turns this into a measured check (<1% of a real lookup) instead of an assumption.
- **Alternatives**: Annotating core classes (rejected: core gains a Spring Data dependency, needs converters for `Email`, `Username`, `UserId`, `PasswordHash`, and breaks the "core imports no JDBC/Redis" rule). MapStruct (rejected: about 10 fields per mapper does not justify an annotation processor next to Lombok).

## 4. Where each store lives, and what that means for atomicity

- **Decision**: Accounts in PostgreSQL; verification tokens and sessions in Redis as source of truth (as agreed in the design doc and spec assumptions).
- **Cross-store behaviour found in core** (not changed by this feature):
  1. `ChangePasswordService`, suspend, and soft-delete **save the account, then call** `AuthenticationSessionRevocationCoordinator.revokeAllFor`. A Redis failure throws inside the `@Transactional` method, so the PostgreSQL write rolls back. If PostgreSQL's commit fails after revocation, sessions are revoked unnecessarily. Both outcomes are safe: a revoked session never becomes valid again.
  2. `VerifyEmailService` calls `verificationRepository.save(verification)` **before** `userRepository.save(account)`. If the account write then fails (optimistic conflict or commit failure) the token reads as consumed while the account is still pending. This is recoverable: a pending account can request a replacement token, and the replacement invalidates the old one. It is not fixed here.
- **Follow-up for core (separate change)**: reorder `VerifyEmailService` to save the account first, or move verification tokens into PostgreSQL. PostgreSQL storage was rejected here because spec FR-008 requires tokens to expire without a cleanup job, and the design doc chose Redis.
- **Alternatives**: Sessions in PostgreSQL (atomic with account changes and simpler ordering by SQL, but needs a retention cleanup job, which the spec excludes).

## 5. Redis design

### 5.1 Time and precision
- **Decision**: Store instants as epoch **microseconds** (integers). Lua doubles hold integers exactly up to 2^53 (about 9.0e15) and current epoch microseconds are about 1.8e15. PostgreSQL `timestamptz` also keeps microseconds. Round-trip tests therefore compare at microsecond precision; mappers truncate nanoseconds.
- **Alternatives**: Milliseconds (loses precision, round-trip inequality); ISO strings (cannot be compared inside Lua).

### 5.2 Digests
- **Decision**: SHA-256 (hex) of the presented token is the lookup key and stored value. Tokens are 128+ bits of entropy, so a fast unsalted hash is correct; per-token salting would prevent lookup by presented value.
- **Alternatives**: Argon2 for tokens (rejected: designed for low-entropy secrets; adds tens of ms to every refresh); HMAC with a server pepper (reasonable hardening, deferred as unnecessary for a personal project).

### 5.3 Atomic operations
- **Decision**: Use Redis Lua scripts (loaded as `RedisScript` resources) for: session create, `touchIfUsable`, `revokeBySessionId`, `revokeOtherSessions`, `revokeAllByUserId`, and verification `invalidateForUser`. Callers pass `now` explicitly (from the injected `Clock`), so Redis `TIME` is never used. Scripts compute keys from arguments, which is valid on a single node; the spec's shared stack is single node.
- **Alternatives**: `WATCH/MULTI` (verbose retries, no conditional logic); a Redis function library (needs Redis 7 function management; scripts are simpler).

### 5.4 Key layout (prefix `identity-access:`, configurable)
See [data-model.md](data-model.md). Session ordering requirement ("issuedAt descending then sessionId ascending") is not what a sorted set returns for ties (reverse order is descending by member), so the view repository reads all of a user's session ids and sorts in memory. A user's retained session count is small (spec SC-004 uses 100).

### 5.5 Retention and expiry
- **Decision**: Verification tokens use the exact expiry as the Redis expiry (spec: expired at the exact moment now reaches expiry, absent equals invalid). Sessions use `expiresAt + identity-access.session.retention` (default 30 days) so expired and revoked sessions stay readable; the per-user index key is refreshed to the longest retention and stale members are pruned when read.
- **Alternatives**: Scheduled cleanup (excluded by FR-008/FR-024).

### 5.6 Credentials
Redis 6+ ACL user `admin` with password `123456`; the `default` user is disabled via a mounted `redis.conf`. Spring: `spring.data.redis.username/password`.

## 6. Account storage details

- **Schema isolation**: the connection sets `currentSchema=identity_access`; Flyway uses `schemas=identity_access` with `create-schemas=true`, so tables and `flyway_schema_history` land in that schema. `V1__create_user_account.sql` stays unqualified and unchanged.
- **Time columns**: use `OffsetDateTime` (UTC) in the row record, which `pgjdbc` maps natively to `timestamptz`. Mapper converts to and from `Instant`. This avoids `Instant`→`Timestamp` time-zone shifts.
- **Duplicates**: translate Spring's `DuplicateKeyException` (SQLSTATE 23505, from either unique constraint) into core `DuplicateUserAccountException`. `OptimisticLockingFailureException` is not translated; the presentation layer maps it.
- **Version**: Spring Data JDBC sets version 0 on first insert when the field is `null`, matching `version BIGINT NOT NULL DEFAULT 0`.

## 7. Cryptography and tokens

### 7.1 Password hashing
- **Decision**: Password4j Argon2 (type `id`), 16-byte random salt, algorithm label `argon2id`. `hash()` returns the encoded result as `hash` and the raw salt via `getSaltBytes()` (the old code used `getSalt().getBytes()`, which can corrupt non-UTF-8 bytes). `matches()` returns `false` for any algorithm label other than `argon2id` and never logs input. Unit tests prove a round trip and a wrong-password rejection.
- **Alternatives**: Spring Security `Argon2PasswordEncoder` (needs BouncyCastle; Password4j is already a dependency).

### 7.2 Snowflake identifiers
- **Decision**: Rewrite compactly instead of porting the old class (which came from a third-party snippet and needs a database lookup for the datacenter id). Layout: 41-bit timestamp (custom epoch 2026-01-01), 10-bit node id from `identity-access.snowflake.worker-id` (0–1023), 12-bit sequence; injected `Clock`; synchronized `nextId()`; on a clock rollback up to 5 ms wait, otherwise throw. IDs are positive (`UserId` requires > 0).
- **Alternatives**: Port the old 5+5-bit class (rejected: needs `VariableServerRepository`); DB sequence (rejected: the core port is a numeric generator and the application owns the layout).

### 7.3 Unique string ids and verification tokens
- **Decision**: One adapter, `java-uuid-generator`'s time-based epoch generator (UUIDv7, already used by the old service), implements `UniqueStringIdGenerator`. Core reuses it for session ids **and** for verification tokens. UUIDv7 carries 74 random bits, which is acceptable for a 24-hour single-use token, but the spec's "unpredictable" wording is better served by a dedicated 128-bit random generator.
- **Follow-up for core (separate change)**: introduce a `VerificationTokenGenerator` port (the design doc already lists one). Until then FR-001's one-adapter-per-port rule keeps a single implementation.

### 7.4 Refresh credentials
`SecureRandom` 32 bytes, Base64 URL without padding (43 characters); digest stored with SHA-256 (5.2).

### 7.5 Access credentials
- **Decision**: Issue with Spring Security's `NimbusJwtEncoder` using an `RSAKey` JWK, algorithm RS256, claims `sub` (user id), `email`, `iss`, `aud`, `iat`, `exp`. Validate with `NimbusJwtDecoder.withPublicKey` plus issuer and audience validators. One JOSE library (Nimbus, already pulled in by the resource-server starter) for both directions, so `jjwt-*` can be removed.
- **Lifetime**: `identity-access.security.jwt.access-token-ttl`, default 15 minutes (the old constant `90000000` ms is about 25 hours despite its comment).
- **Keys**: `private-key`/`public-key` properties accept either PEM text or a `file:`/`classpath:` location. Missing or malformed keys fail start-up.
- **Alternatives**: Keep `jjwt` for issuing (two JWT stacks for no benefit).

## 8. Security configuration

- **Decision**: Copy the shape of the message service's `SecurityConfig`: stateless, CSRF off, `oauth2ResourceServer(jwt)`, `OPTIONS` permitted, white-list then `anyRequest().authenticated()`. Changes from the original: CORS origins come from properties (empty by default, which denies cross-origin use), issuer/audience validation is added to the decoder, the white-list is a property with identity-access defaults, and `/actuator/health/**` is public.
- **Default public endpoints** (matching the old service's names): `/api/v*/auth/register`, `/api/v*/auth/authenticate`, `/api/v*/auth/refresh`, `/api/v*/auth/logout`, `/api/v*/auth/verification/resend`, `/api/v*/auth/verification/confirm`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`. Only registration exists in presentation today; the others are forward declarations and are overridable.
- **Not carried over**: `JwtAuthenticationFilter`, `IAccessTokenVerifier`, `GlobalLoggingFilter` (logs request and response bodies, which include passwords and tokens).

## 9. Mail

- **Decision**: `VerificationNotifier` adapter renders `templates/email-verification.html` with Thymeleaf and sends through `JavaMailSender`. The link is `{public-base-url}{confirm-path}?token={rawToken}` from `identity-access.verification.mail.*`. `@Retryable` (3 retries, exponential backoff) wraps the send; the final failure is wrapped in an adapter exception whose message never contains the token.
- **Consequence**: core calls the notifier synchronously inside the registration transaction, so an unreachable mail server fails the registration and rolls back the account. The Redis token is orphaned and expires by itself. Asynchronous delivery would need an outbox, which is excluded.
- **Development mail sink**: no mail component is added to `infrastructure/components` (the spec asks for database stacks only). The quickstart uses a one-line `docker run` of an SMTP catcher; promote it to a component later if wanted.

## 10. Shared database stacks

- **Decision**: Two components mirroring the Kafka/Vault layout: `infrastructure/components/postgresql` (image `postgres:17`) and `infrastructure/components/redis` (image `redis:7`), each with `docker-compose.yml`, `README.md`, `.gitignore` (data directory), and a short runbook.
- **Ports**: standard container and host ports, `5432` and `6379`, overridable via `POSTGRES_PORT` / `REDIS_PORT`. Existing host ports in use: 2181, 5002, 5003, 5440–5442, 6380, 6387–6389, 8080, 9052, so there is no collision. The old per-service containers are untouched.
- **Credentials**: PostgreSQL `admin`/`123456`, database `chatter`; Redis ACL user `admin`/`123456`.
- **Persistence**: bind-mounted `./data` (matching Kafka/Vault) with Redis append-only persistence enabled.
- **Not set**: `wal_level=logical` and replication slots (only needed for the dropped CDC engine).
- **Network**: join `chatter-net` using the same `name: chatter-net` declaration the other components use.

## 11. Verification strategy

- **Unit**: hasher, Snowflake (100,000 ids × 8 threads, rollback), refresh generator, token digest, PEM loader, issuer + decoder round trip (including expired, tampered, foreign key, wrong issuer/audience), link factory, mappers (microsecond round trip).
- **Integration** (Testcontainers `postgres:17` and `redis:7`): the same behavioural expectations as the core port javadocs for the JDBC repositories and each Redis adapter, Flyway schema placement, concurrent duplicate registration, stale write.
- **Web slice**: a test-only controller to prove public/protected/preflight behaviour of the security chain.
- **Structure**: ArchUnit rules (core has no infrastructure or storage/token/mail/HTTP imports; each core outbound port has exactly one implementation at the location in the adapter map).
- **Formatting**: the build runs Spotless `check`; run `mvn spotless:apply` before committing.
- **Alternative to Testcontainers**: running against the shared stacks (rejected: tests would depend on and mutate developer data).
