---

description: "Task list for Identity Access Infrastructure Adapters"
---

# Tasks: Identity Access Infrastructure Adapters

**Input**: Design documents from `/specs/003-infrastructure-adapters/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: INCLUDED. The spec's success criteria (SC-002 to SC-010) and edge cases are only provable by tests (Testcontainers contract tests, ArchUnit, MockMvc), so test tasks are part of each story.

**Organization**: Tasks are grouped by user story. US1 (storage), US2 (adapters) and US3 (security) are all P1 and independent after Phase 2; US4 and US5 are P2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5, mapped to the user stories in spec.md
- Every task names exact file paths

## Path Conventions

Repository root is `/home/minewaku/projects/java/2026/Chatter`. Maven commands run from `services/identity-access-next` (the workspace for build commands, per `AGENTS.md`). Git/patch paths are relative to the repository root.

Path shorthands used below (expand them literally when creating files):

- `{MAIN}` = `services/identity-access-next/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure`
- `{TEST}` = `services/identity-access-next/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure`
- `{RES}` = `services/identity-access-next/identityaccess-infrastructure/src/main/resources`
- `{TRES}` = `services/identity-access-next/identityaccess-infrastructure/src/test/resources`
- `{CORE}` = `com.minewaku.chatter.identityaccess` (Java package of core; core source is under `services/identity-access-next/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess`)
- `{INFRA}` = `com.minewaku.chatter.identityaccess.infrastructure` (Java package)

Rules that apply to every task: core (`identityaccess-core`) is NOT modified; no log statement may contain a password, hash, raw verification token, raw refresh credential, access credential or private key; persistence shapes (`*Row`, `*Record`) are package-private; run `mvn spotless:apply` before finishing a task that adds Java files. Do not edit other services or existing components.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Make the module build on Spring Boot 4.0.8 with the dependencies the adapters need, and put configuration defaults in place.

- [X] T001 Rewrite the dependency section of `services/identity-access-next/identityaccess-infrastructure/pom.xml` per research 1.1–1.3 and 7.5: replace `spring-boot-starter-web` with `spring-boot-starter-webmvc`; replace `flyway-core` with `spring-boot-starter-flyway` (keep `flyway-database-postgresql`, runtime); add `spring-boot-starter-data-redis`, `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator` and `com.fasterxml.uuid:java-uuid-generator` (latest 5.x on Maven Central, pinned as a `<java-uuid-generator.version>` property); keep `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-jdbc`, `postgresql` (runtime), `password4j`. REMOVE all three `jjwt-*` dependencies, the `jsonwebtoken.version` property, `spring-cloud-starter-vault-config`, `spring-cloud-vault-config-databases` and `spring-vault-core`.
- [X] T002 Add test-scope dependencies to `services/identity-access-next/identityaccess-infrastructure/pom.xml` (after T001): `spring-boot-starter-test`, `spring-security-test`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`, `com.tngtech.archunit:archunit-junit5`. Use Spring Boot's managed Testcontainers version if the Boot 4 BOM provides one; otherwise pin the latest 1.x/2.x release in a property. Redis tests use `GenericContainer` (no extra artifact).
- [X] T003 Build the module and fix any renamed Boot 4 class/property: from `services/identity-access-next` run `mvn -pl identityaccess-infrastructure -am -DskipTests compile`; confirm spotless config in `services/identity-access-next/pom.xml` still passes (`mvn -pl identityaccess-infrastructure spotless:check`). Record any Boot 4 module/package rename you had to apply as a one-line comment in the commit message, not in code. This is the risk task from plan.md "Boot 4 module split"; later tasks assume it is green.
- [X] T004 [P] Add `services/identity-access-next/.local/` to the repository `.gitignore` (root file `/home/minewaku/projects/java/2026/Chatter/.gitignore`) so development signing keys are never committed.
- [X] T005 [P] Replace `services/identity-access-next/identityaccess-start/src/main/resources/application.yml` with the full defaults from `specs/003-infrastructure-adapters/contracts/configuration.md`: `server.port`, datasource (`jdbc:postgresql://localhost:5432/chatter?currentSchema=identity_access`, `admin`/`123456`, env-overridable), `spring.flyway.{enabled,schemas,default-schema,create-schemas}`, `spring.data.redis.*`, `spring.mail.*`, `identity-access.redis.key-prefix`, `identity-access.snowflake.worker-id`, `identity-access.session.{lifetime,retention}`, `identity-access.security.jwt.{private-key,public-key,issuer,audience,access-token-ttl}` (keys: `${IDENTITY_ACCESS_JWT_PRIVATE_KEY}` / `${IDENTITY_ACCESS_JWT_PUBLIC_KEY}` with NO fallback), `identity-access.security.public-endpoints`, `identity-access.security.cors.allowed-origins` (empty), `identity-access.verification.mail.*`, and `management.endpoints.web.exposure.include: health`. Keep `spring.application.name: identity-access`.
- [X] T006 [P] In `services/identity-access-next/identityaccess-start/src/main/java/com/minewaku/chatter/identityaccess/start/StartApplication.java` add `@ConfigurationPropertiesScan("com.minewaku.chatter.identityaccess")` so the adapters' `@ConfigurationProperties` records are bound. Component scanning already covers `{INFRA}` via the existing `scanBasePackages = "com.minewaku.chatter.identityaccess"`; leave it unchanged. Also confirm `TimeConfiguration.java` in the same package exposes the single `Clock` bean the adapters will inject; do not add a second `Clock`.

**Checkpoint**: `mvn -pl identityaccess-infrastructure -am compile` succeeds.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared building blocks used by more than one story. No story work starts before this phase is done.

**⚠️ CRITICAL**: US1–US4 all depend on these files.

- [X] T007 [P] Create `{MAIN}/security/TokenDigest.java`: `public final class TokenDigest` with `static String sha256Hex(String token)` returning lowercase hex SHA-256 of the UTF-8 bytes (data-model Conventions; research 5.2). Null/blank input throws `IllegalArgumentException` without echoing the input.
- [X] T008 [P] Create `{TEST}/security/TokenDigestTest.java`: known-vector test (`"abc"` → `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`), lowercase hex, 64 chars, blank input rejected with a message that does not contain the input.
- [X] T009 [P] Create `{MAIN}/security/PemKeys.java`: loads an RSA `PrivateKey` (PKCS#8 PEM) and `PublicKey` (X.509 PEM) from a property value that is either PEM text or a `file:`/`classpath:` location (research 7.5). Failure (missing, blank, malformed, or public key not matching the private key) throws `IllegalStateException` whose message names the property (`identity-access.security.jwt.private-key` / `.public-key`) and never contains key material.
- [X] T010 [P] Create `{TEST}/security/PemKeysTest.java` with generated key fixtures in the test: PEM text and `file:` location both load; blank, malformed and mismatched key pair each fail with a message naming the property and not containing key bytes. Generate a second unrelated key pair as a reusable fixture in `{TEST}/support/RsaKeyFixtures.java` (static helpers returning PEM strings and `KeyPair`s) for US2/US3 tests.
- [X] T011 [P] Create `{MAIN}/security/JwtProperties.java`: `@ConfigurationProperties("identity-access.security.jwt")` record with `privateKey`, `publicKey` (Strings), `issuer` (default `identity-access`), `audience` (default `chatter`), `accessTokenTtl` (`Duration`, default `PT15M`, must be positive). Mark with `@Validated`; a blank key or non-positive TTL fails binding with a message naming the property.
- [X] T012 [P] Create `{TEST}/support/PostgreSqlTestSupport.java`: abstract base (or JUnit extension) that starts a singleton `postgres:17` Testcontainer with database `chatter`, user `admin`, password `123456`, and exposes `DataSource`/JDBC URL with `currentSchema=identity_access`; applies Flyway (`schemas=identity_access`, `createSchemas=true`, locations `classpath:db/migration`) before tests.
- [X] T013 [P] Create `{TEST}/support/RedisTestSupport.java`: abstract base (or JUnit extension) that starts a singleton `redis:7` `GenericContainer` on port 6379, configured with the ACL user from `contracts/shared-components.md` (`--user` equivalent via a mounted/inline `redis.conf` or `withCommand`), builds a `StringRedisTemplate` against it with the test key prefix `test:`, and offers `flushAll()` for per-test cleanup.
- [X] T014 [P] Create `{TEST}/support/AccountFixtures.java`: static factory methods that build valid core `UserAccount`, `PasswordHash`, `Email`, `Username`, `UserId`, `Birthday`, `EmailVerification`, `AuthenticationSession` instances using the core factory/reconstitute methods (locate the model classes under the `internal` packages of `user`, `emailverification` and `authentication` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess` and read their signatures; do not assume a folder name). All instants use at most microsecond precision (data-model Conventions). No Spring or storage imports.

**Checkpoint**: Foundation ready — US1, US2, US3, US4, US5 can proceed in parallel.

---

## Phase 3: User Story 1 - Run the Service Against Real Storage (Priority: P1) 🎯 MVP

**Goal**: Accounts persist in PostgreSQL schema `identity_access`, survive restart, enforce unique email/username and reject stale writes.

**Independent Test**: Against a `postgres:17` Testcontainer: Flyway creates `user_account` and `flyway_schema_history` only in `identity_access`; save → find by id/email/username returns equal data; duplicate email/username and stale writes are rejected as specified; read-side results match; mapping adds <1% to a lookup.

### Tests for User Story 1 ⚠️ (write first, expect failure)

- [X] T015 [P] [US1] Create `{TEST}/adapter/outbound/user/write/jdbc/UserAccountRowMapperTest.java` (same package as the mapper): for each `AccountStatus` value, `toRow` then `toModel` returns a `UserAccount` equal to the original (SC-010, microsecond-precision instants); `deletedAt` null and non-null both round-trip; `version` null stays null on `toRow`; password hash/salt/algorithm preserved byte-for-byte (salt is `byte[]`).
- [X] T016 [P] [US1] Create `{TEST}/adapter/outbound/user/write/jdbc/JdbcUserAccountRepositoryTest.java` extending `PostgreSqlTestSupport` and wiring the real beans via `@SpringBootTest`/`@DataJdbcTest` slice: (a) save new account, `findById`/`findByEmail`/`findByUsername` return an equal model; (b) saving a second account with the same `normalized_email` throws core `DuplicateUserAccountException`, same for `username`, and the cause is retained, no raw SQL error escapes; (c) two threads registering the same email concurrently → exactly one row stored, the loser gets `DuplicateUserAccountException`; (d) load an account, update it twice from the same stale copy → second save throws `OptimisticLockingFailureException`; (e) first insert yields version 0 and each update increments it.
- [X] T017 [P] [US1] Create `{TEST}/adapter/outbound/user/read/jdbc/JdbcUserAccountQueryRepositoryTest.java` extending `PostgreSqlTestSupport`: after saving an account through the write adapter, `findById`/`findByEmail` return `FindUserByIdUseCase.Result`/`FindUserByEmailUseCase.Result` with the exact stored fields; `accessible` equals `AccountStatus.valueOf(status).isAccessible()` for every status (insert rows for each status); unknown id/email → `Optional.empty()`.
- [X] T018 [P] [US1] Create `{TEST}/integration/postgresql/FlywaySchemaPlacementTest.java` extending `PostgreSqlTestSupport`: after migration, `information_schema.tables` shows `user_account` and `flyway_schema_history` in `identity_access` and no table of this service in `public` (SC-009); then create a second schema `other_service` with its own Flyway run in the same database and assert both migrate without conflict.
- [X] T019 [P] [US1] Create `{TEST}/adapter/outbound/user/MappingCostMeasurementTest.java` (tag `@Tag("measurement")`, still run in `verify`): time N≥2,000 warmed-up `findByEmail` calls against the Testcontainer database and N≥200,000 `UserAccountRowMapper.toModel` calls; assert mapper time per call < 1% of lookup time per call (SC-010). Print both numbers in the test output.
- [X] T020 [P] [US1] Create `{TEST}/integration/postgresql/UnreachableDatabaseStartupTest.java`: start a minimal Spring context with the datasource URL pointing at a closed port (`jdbc:postgresql://localhost:1/chatter`) and Flyway enabled; assert startup fails (does not start half-working) and the failure message chain contains the datasource URL host/port or the word `datasource`/`postgresql`. If the default message does not name the store, add a `FailureAnalyzer` in `{MAIN}/integration/postgresql/PostgreSqlStartupFailureAnalyzer.java` (registered in `{RES}/META-INF/spring.factories`) that names it, without printing the password.

### Implementation for User Story 1

- [X] T021 [P] [US1] Create `{MAIN}/integration/postgresql/PostgreSqlConfiguration.java`: `@Configuration` enabling Spring Data JDBC repositories restricted to `{INFRA}.adapter.outbound.user.write.jdbc` (`@EnableJdbcRepositories(basePackages = …)`); register a `JdbcClient` bean from the `DataSource` if Boot does not provide one. No schema or credential logic here; those come from `application.yml`.
- [X] T022 [P] [US1] Create package-private `{MAIN}/adapter/outbound/user/write/jdbc/UserAccountRow.java`: Java record annotated `@Table("user_account")` mapping the columns exactly as in data-model.md §1: `@Id Long id`; `normalizedEmail` (`normalized_email`, VARCHAR(320), unique); `username` (VARCHAR(32), unique); `LocalDate birthday`; `String status` (one of the 5 `AccountStatus` names); `OffsetDateTime deletedAt` (nullable); `passwordAlgorithm` (VARCHAR(64)); `passwordHash` (VARCHAR(512)); `byte[] passwordSalt`; `OffsetDateTime createdAt`; `OffsetDateTime updatedAt`; `@Version Long version` (`null` = new, 0 on first insert). Use `@Column` names where they differ from the field name.
- [X] T023 [P] [US1] Create package-private `{MAIN}/adapter/outbound/user/write/jdbc/UserAccountRows.java`: `interface UserAccountRows extends CrudRepository<UserAccountRow, Long>` with derived queries `Optional<UserAccountRow> findByNormalizedEmail(String)` and `Optional<UserAccountRow> findByUsername(String)`.
- [X] T024 [US1] Create package-private `{MAIN}/adapter/outbound/user/write/jdbc/UserAccountRowMapper.java` (depends on T022): `UserAccountRow toRow(UserAccount)` and `UserAccount toModel(UserAccountRow)` using `UserAccount.reconstitute`; `Instant`↔`OffsetDateTime` at `ZoneOffset.UTC`; truncate nanoseconds to microseconds on write (`truncatedTo(ChronoUnit.MICROS)`); do NOT re-validate the `ck_user_account_deletion` constraint (data-model §1).
- [X] T025 [US1] Create `{MAIN}/adapter/outbound/user/write/jdbc/JdbcUserAccountRepository.java` (depends on T023, T024): `@Repository` public class implementing `{CORE}.user.internal.port.repository.UserAccountRepository` (`findById`, `findByEmail`, `findByUsername`, `save`). `save` catches Spring's `DuplicateKeyException` (SQLSTATE 23505 from either unique constraint) and rethrows core `DuplicateUserAccountException(message, cause)` with a message that contains no raw SQL; `OptimisticLockingFailureException` is NOT translated (research 6). Class and constructor are public; row/mapper/repository interface stay package-private.
- [X] T026 [P] [US1] Create `{MAIN}/adapter/outbound/user/read/jdbc/JdbcUserAccountQueryRepository.java`: `@Repository` class implementing `{CORE}.user.internal.port.query.UserAccountQueryRepository` using `JdbcClient` with explicit SQL selecting `id, normalized_email, username, birthday, status, deleted_at, created_at, updated_at`; a row-mapper lambda builds `FindUserByIdUseCase.Result` / `FindUserByEmailUseCase.Result` directly (no intermediate class, FR-010a); `accessible = AccountStatus.valueOf(status).isAccessible()`; lookups use the indexed `id` / `normalized_email` columns (email argument is the already-normalized `Email.value()`).
- [X] T027 [US1] Run `mvn -pl identityaccess-infrastructure -am verify -Dtest='*Jdbc*,UserAccountRowMapperTest,FlywaySchemaPlacementTest,MappingCostMeasurementTest,UnreachableDatabaseStartupTest'` from `services/identity-access-next` and fix failures until all US1 tests pass. Requires a running Docker daemon.

**Checkpoint**: US1 is fully functional and testable independently: accounts persist, duplicates and stale writes behave as specified, schema isolation proven.

---

## Phase 4: User Story 2 - Passwords, Identifiers, and Tokens Work for Real (Priority: P1)

**Goal**: Every remaining core outbound port has exactly one real adapter: password hashing, Snowflake and UUIDv7 ids, refresh credentials, access credentials, Redis verification and session stores, and verification mail.

**Independent Test**: With a `redis:7` Testcontainer, a mail test double and the unit tests: hash round trip, 100,000 unique ids over 8 threads, refresh token shape, JWT claims, Redis stores obey every port javadoc sentence and keep no raw secrets, mail renders the right link and retries a bounded number of times.

### 4a. Passwords, identifiers, refresh and access credentials (no store)

- [X] T028 [P] [US2] Create `{TEST}/adapter/outbound/user/password/Argon2PasswordHasherTest.java`: `hash` returns `algorithm() == "argon2id"`, non-empty `hash`, 16-byte `salt`; same password hashed twice gives different salt and hash; `matches` is true for the right password and false for a wrong one; `matches` returns false (never throws) when `passwordHash.algorithm()` is not `argon2id`; captured log output (use `OutputCaptureExtension`) never contains the password or the hash.
- [X] T029 [P] [US2] Create `{MAIN}/adapter/outbound/user/password/Argon2PasswordHasher.java`: `@Component` implementing `{CORE}.user.internal.port.PasswordHasher` with Password4j `Argon2Function` type `id`, 16-byte random salt, label `argon2id`. Use `getSaltBytes()` for the salt (NOT `getSalt().getBytes()`, research 7.1). `matches` rejects other labels with `false`. No logging of inputs.
- [X] T030 [P] [US2] Create `{TEST}/adapter/outbound/shared/id/SnowflakeIdGeneratorTest.java`: 100,000 ids across 8 threads → 0 duplicates (SC-005); ids from one thread are strictly increasing; all ids > 0; ids generated after a clock advance sort after earlier ones; with a controllable `Clock` that moves back ≤5 ms the generator waits and still returns a unique id, and moves back >5 ms throws `IllegalStateException`; `worker-id` outside 0–1023 fails construction with a message naming `identity-access.snowflake.worker-id`; two generators with different worker ids never emit the same id for the same instant.
- [X] T031 [P] [US2] Create `{MAIN}/adapter/outbound/shared/id/SnowflakeProperties.java` (`@ConfigurationProperties("identity-access.snowflake")` record: `int workerId`, optional `Instant epoch` default `2026-01-01T00:00:00Z`) and `{MAIN}/adapter/outbound/shared/id/SnowflakeIdGenerator.java` (`@Component` implementing `{CORE}.shared.port.TimeBasedIdGenerator`): 41-bit timestamp, 10-bit node id, 12-bit sequence, injected `Clock`, `synchronized nextId()`, rollback ≤5 ms waits otherwise throws (research 7.2). No database lookup.
- [X] T032 [P] [US2] Create `{TEST}/adapter/outbound/shared/id/UuidV7IdGeneratorTest.java`: output is a 36-char UUID string with version nibble 7; 10,000 ids are unique; consecutive ids sort lexicographically in generation order (with a clock/sleep step between calls).
- [X] T033 [P] [US2] Create `{MAIN}/adapter/outbound/shared/id/UuidV7IdGenerator.java`: `@Component` implementing `{CORE}.shared.port.UniqueStringIdGenerator` using `com.fasterxml.uuid.Generators.timeBasedEpochGenerator()` (research 7.3).
- [X] T034 [P] [US2] Create `{TEST}/adapter/outbound/authentication/SecureRandomRefreshTokenGeneratorTest.java`: output is Base64-URL without padding, exactly 43 characters; 10,000 tokens are unique; decoded length is 32 bytes.
- [X] T035 [P] [US2] Create `{MAIN}/adapter/outbound/authentication/SecureRandomRefreshTokenGenerator.java`: `@Component` implementing `{CORE}.authentication.internal.port.RefreshTokenGenerator` with a shared `SecureRandom`, 32 bytes, `Base64.getUrlEncoder().withoutPadding()` (research 7.4).
- [X] T036 [P] [US2] Create `{TEST}/adapter/outbound/authentication/JwtAccessTokenIssuerTest.java` (uses `RsaKeyFixtures` from T010 and a fixed `Clock`): the issued JWT is RS256; decode with `NimbusJwtDecoder.withPublicKey` and assert `sub` = user id string, `email` claim = email value, `iss`/`aud` from `JwtProperties`, `iat` = clock now, `exp` = `iat + accessTokenTtl` (use a non-default TTL such as `PT7M` to prove it is not hard-coded); the token is rejected by a decoder built from a different key pair; no log output contains the token or the private key.
- [X] T037 [US2] Create `{MAIN}/adapter/outbound/authentication/JwtAccessTokenIssuer.java` (depends on T009, T011): `@Component` implementing `{CORE}.authentication.internal.port.AccessTokenIssuer` (`String issue(UserId userId, Email email)`); builds a `NimbusJwtEncoder` over an `RSAKey` JWK from `PemKeys`/`JwtProperties`; claims `sub`, `email`, `iss`, `aud`, `iat`, `exp` via the injected `Clock`; header `alg` RS256. Invalid keys fail construction (start-up), via `PemKeys`.

### 4b. Redis foundation

- [X] T038 [P] [US2] Create `{MAIN}/integration/redis/RedisKeyProperties.java`: `@ConfigurationProperties("identity-access.redis")` record with `keyPrefix` (default `identity-access:`, must end with `:`).
- [X] T039 [P] [US2] Create `{MAIN}/integration/redis/RedisConfiguration.java`: `@Configuration` exposing a `StringRedisTemplate` (string serializers) and a helper to build `DefaultRedisScript<Long>`/`<List>` beans from `classpath:redis/*.lua` resources. Connection settings come from `spring.data.redis.*` (no hard-coding).

### 4c. Verification tokens in Redis (FR-008)

- [X] T040 [P] [US2] Create `{TEST}/adapter/outbound/verification/write/redis/VerificationRecordMapperTest.java`: `EmailVerification` → `VerificationRecord` → `EmailVerification` round trip using the presented raw token; `verifiedAt` absent stays absent; microsecond-precision instants equal after the trip; the record holds only the digest, never the raw token (assert via reflection on the record components and the Redis hash map the mapper produces).
- [X] T041 [P] [US2] Create `{TEST}/adapter/outbound/verification/write/redis/RedisEmailVerificationRepositoryTest.java` extending `RedisTestSupport`: (a) `save` then `findByPresentedToken` returns an equal model; (b) stored keys are `P verification:token:{digest}` and `P verification:user:{userId}`, no key or value contains the raw token; (c) key TTL equals `expiresAt − now` (±1 s) and the repository never extends it on a later `save` after consumption; (d) a token past `expiresAt` reads as `Optional.empty()` (use a short expiry and wait, or set `PEXPIREAT` in the past); (e) blank/null token → empty without touching Redis; (f) `invalidateForUser` deletes an outstanding (unverified) token and the pointer, but leaves a consumed token readable until expiry; (g) after Redis `FLUSHALL` the token reads as unknown (edge case: entries lost early).
- [X] T042 [P] [US2] Create `{RES}/redis/verification-invalidate-for-user.lua`: KEYS[1]=`P verification:user:{userId}` pointer key; ARGV[1]=token key prefix `P verification:token:`. Read the pointer; if the pointed token hash exists and has no `verifiedAt` field, `DEL` it; always `DEL` the pointer; return 1 if a token was deleted else 0 (data-model §2).
- [X] T043 [P] [US2] Create package-private `{MAIN}/adapter/outbound/verification/write/redis/VerificationRecord.java`: record with `userId` (long), `email` (String), `createdAt`, `expiresAt` (epoch micros as `long`), `Long verifiedAt` (nullable), plus `toHash()`/`fromHash(Map)` string-map conversion (all numbers in micros, email as text) per data-model §2.
- [X] T044 [US2] Create package-private `{MAIN}/adapter/outbound/verification/write/redis/VerificationRecordMapper.java` (depends on T043): `toRecord(EmailVerification)` and `toModel(VerificationRecord, String presentedToken)` using the core model's `reconstitute`/factory; the raw token is supplied from the presented value and is never read from Redis; truncate nanoseconds to micros.
- [X] T045 [US2] Create `{MAIN}/adapter/outbound/verification/write/redis/RedisEmailVerificationRepository.java` (depends on T038, T039, T042, T044, T007): `@Repository` implementing `{CORE}.emailverification.internal.port.repository.EmailVerificationRepository` (`save`, `findByPresentedToken`, `invalidateForUser`). Keys: `P verification:token:{TokenDigest.sha256Hex(token)}` (hash) and `P verification:user:{userId}` (string = digest of the latest outstanding token); expiry = exactly `expiresAt` (`PEXPIREAT`); `save` of a consumed verification only adds `verifiedAt` and sets the pointer only while `verifiedAt` is absent; `findByPresentedToken` blank → empty, otherwise `HGETALL`; `invalidateForUser` runs the Lua script from T042.

### 4d. Sessions in Redis (FR-009)

- [X] T046 [P] [US2] Create `{MAIN}/adapter/outbound/session/SessionProperties.java` (`@ConfigurationProperties("identity-access.session")` record: `Duration lifetime` default `P30D`, `Duration retention` default `P30D`, both positive) and `{MAIN}/adapter/outbound/session/SessionConfiguration.java` exposing the ONE bean `@Bean Duration sessionLifetime(SessionProperties p)` consumed by core `AuthenticateService` (research 1.4). No other `Duration` bean may exist anywhere in the module.
- [X] T047 [P] [US2] Create `{TEST}/adapter/outbound/session/SessionConfigurationTest.java`: a Spring context test proves exactly one `Duration` bean exists, named `sessionLifetime`, equal to the configured lifetime, and that core `AuthenticateService` can be constructed from the context (use `ApplicationContextRunner` with the core service as a bean if it is Spring-wired in `start`; otherwise assert `getBeansOfType(Duration.class)` has size 1).
- [X] T048 [P] [US2] Create the five session Lua scripts in `{RES}/redis/` per data-model §3 (timestamps are epoch micros passed in ARGV; never call Redis `TIME`): `session-create.lua` (writes `P session:{id}` hash, `P session:token:{digest}` pointer and `ZADD P session:user:{userId}` with score `issuedAt`, then sets `PEXPIREAT` on all three to `expiresAt + retention`, extending the user index to the longest value); `session-touch.lua` (if hash exists, `revokedAt` absent and `now < expiresAt`: set `lastUsedAt = max(lastUsedAt, now)`, return 1, else 0); `session-revoke-one.lua` (if hash exists, `userId` matches ARGV and `revokedAt` absent: set `revokedAt = now`, return 1 else 0); `session-revoke-others.lua` (every other retained session of the user with `revokedAt` absent gets `revokedAt = now`; skip ids whose hash has expired; return count); `session-revoke-all.lua` (same for every retained session; return count).
- [X] T049 [P] [US2] Create package-private `{MAIN}/adapter/outbound/session/write/redis/SessionRecord.java`: record holding `sessionId`, `userId`, `deviceInfo`, `issuedAt`, `expiresAt` (micros), nullable `lastUsedAt`, nullable `revokedAt`, `tokenDigest`; `toHash()` / `fromHash(Map)`; absent optional fields are omitted from the hash, not stored as empty strings.
- [X] T050 [US2] Create package-private `{MAIN}/adapter/outbound/session/write/redis/SessionRecordMapper.java` (depends on T049): `AuthenticationSession` ↔ `SessionRecord` via `reconstitute`; truncates to micros; maps the digest from the raw refresh token supplied at `create`.
- [X] T051 [P] [US2] Create `{TEST}/adapter/outbound/session/write/redis/SessionRecordMapperTest.java`: model → record → model round trip equal (including null `lastUsedAt`/`revokedAt` and set values); record exposes only a digest, never the raw refresh token.
- [X] T052 [P] [US2] Create `{TEST}/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepositoryTest.java` extending `RedisTestSupport`, encoding every sentence of the `AuthenticationSessionRepository` javadoc: (a) `create` then `findByPresentedToken(raw)` and `findBySessionId(id)` return an equal model; stored values contain only the digest; (b) `touchIfUsable` returns true and sets `lastUsedAt` for a live session, false for an unknown id, for a revoked session and for `now >= expiresAt` (boundary: exactly at `expiresAt` is false), and `lastUsedAt` never moves backwards; (c) `revokeBySessionId` returns true once, false when called again, false for a different owner; (d) `revokeOtherSessions` revokes only unrevoked sessions of that user other than the current one, returns the count, and does not touch another user's sessions; (e) `revokeAllByUserId` revokes all and returns the count; (f) expired and revoked sessions remain readable through `findByPresentedToken` until `expiresAt + retention`; the Redis TTL equals that moment (±1 s); (g) atomicity: 8 threads call `revokeBySessionId` on one session → exactly one returns true; (h) after `FLUSHALL` lookups return empty/false.
- [X] T053 [US2] Create `{MAIN}/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepository.java` (depends on T038, T039, T046, T048, T050, T007): `@Repository` implementing `{CORE}.authentication.internal.port.repository.AuthenticationSessionRepository` (`create`, `findByPresentedToken`, `findBySessionId`, `touchIfUsable`, `revokeBySessionId`, `revokeOtherSessions`, `revokeAllByUserId`). Each mutating method is one `RedisScript` call from the scripts in T048 with `now` taken from the method argument (micros); keys are `P session:{id}`, `P session:token:{digest}`, `P session:user:{userId}` (data-model §3); retention comes from `SessionProperties`.
- [X] T054 [P] [US2] Create `{TEST}/adapter/outbound/session/read/redis/RedisAuthenticationSessionViewRepositoryTest.java` extending `RedisTestSupport`: sessions created through the write adapter appear in `findByUserId` as `FindSessionsQuery.SessionView` with the stored fields; ordering is `issuedAt` descending then `sessionId` ascending (create two sessions with the same `issuedAt`); a session whose hash has expired is omitted and its id pruned from the user's sorted set; a user with 100 retained sessions returns all 100 using a bounded number of Redis round trips (assert ≤ 3 commands with a command-counting wrapper or `INFO commandstats` delta); unknown user → empty list.
- [X] T055 [US2] Create `{MAIN}/adapter/outbound/session/read/redis/RedisAuthenticationSessionViewRepository.java` (depends on T038, T039, T054): `@Repository` implementing `{CORE}.authentication.internal.port.query.AuthenticationSessionViewRepository.findByUserId`; read all ids from `P session:user:{userId}`, pipeline `HGETALL` for each, drop and `ZREM` ids whose hash is gone, sort in memory by `issuedAt` desc then `sessionId` asc (research 5.4), map straight to `FindSessionsQuery.SessionView` (no intermediate class, FR-010a).

### 4e. Verification email (FR-007)

- [X] T056 [P] [US2] Create `{MAIN}/adapter/outbound/verification/mail/VerificationMailProperties.java`: `@ConfigurationProperties("identity-access.verification.mail")` record `from`, `subject`, `publicBaseUrl`, `confirmPath` with the defaults from `contracts/configuration.md`; `publicBaseUrl` must be an absolute http(s) URL.
- [X] T057 [P] [US2] Create `{TEST}/adapter/outbound/verification/mail/VerificationLinkFactoryTest.java`: link is `{public-base-url}{confirm-path}?token={rawToken}`; trailing slash on base URL and missing leading slash on path are normalised; token is URL-encoded; a non-default base URL is honoured (no hard-coded `localhost`).
- [X] T058 [P] [US2] Create `{MAIN}/adapter/outbound/verification/mail/VerificationLinkFactory.java`: `@Component` building the link with `UriComponentsBuilder` from `VerificationMailProperties` and the raw token.
- [X] T059 [P] [US2] Create `{RES}/templates/email-verification.html`: Thymeleaf HTML template with variables `link` and `expiresAt` (formatted UTC); no inline raw-token echo outside the link.
- [X] T060 [P] [US2] Create `{MAIN}/integration/mail/MailConfiguration.java`: `@Configuration @EnableResilientMethods` (enables `@Retryable`; research 1.3). Do not add Resilience4j.
- [X] T061 [P] [US2] Create `{TEST}/adapter/outbound/verification/mail/MailVerificationNotifierTest.java`: using Mockito mocks for `JavaMailSender` and a real `SpringTemplateEngine`, assert the sent `MimeMessage` has the configured `from`/`subject`, the recipient `Email.value()`, and a body containing the link from `VerificationLinkFactory`; with a `JavaMailSender` that fails twice then succeeds, delivery succeeds after 3 attempts (use a Spring context with `@EnableResilientMethods` so the proxy applies); with a sender that always fails, exactly 4 attempts (1 + 3 retries) occur and the thrown adapter exception's message AND captured log output do NOT contain the raw token.
- [X] T062 [US2] Create `{MAIN}/adapter/outbound/verification/mail/MailVerificationNotifier.java` (depends on T056, T058, T059, T060): `@Component` implementing `{CORE}.emailverification.internal.port.VerificationNotifier.sendVerification(Email, String, Instant)`; renders the Thymeleaf template, sends via `JavaMailSender` inside a method annotated `@Retryable(maxRetries = 3, delay = …, multiplier = 2)` (Spring Framework 7 `org.springframework.resilience.annotation.Retryable`); the final failure is wrapped in an adapter exception (`VerificationMailException`, package-private or nested) whose message never contains the token and whose cause chain is logged without the token (research 9).
- [X] T063 [US2] Run `mvn -pl identityaccess-infrastructure -am verify -Dtest='Argon2*,Snowflake*,UuidV7*,SecureRandom*,JwtAccessTokenIssuer*,*Redis*,*SessionRecord*,*VerificationRecord*,SessionConfiguration*,*Mail*,VerificationLink*'` from `services/identity-access-next` and fix failures until all US2 tests pass. Requires Docker.

**Checkpoint**: US2 complete — every non-persistence core port has a working adapter; Redis stores keep digests only.

---

## Phase 5: User Story 3 - Protect Endpoints with Standard Token Validation (Priority: P1)

**Goal**: A stateless resource-server filter chain validates this service's access credentials; public endpoints and preflight stay open; bad or missing keys stop start-up.

**Independent Test**: MockMvc against a test-only controller: valid token → 200 with the subject available; missing/expired/tampered/wrong-key/wrong-issuer/wrong-audience → 401; public endpoint without token → not 401; `OPTIONS` preflight without token → 2xx; start-up with missing/malformed key → failure naming the property.

### Tests for User Story 3 ⚠️ (write first, expect failure)

- [X] T064 [P] [US3] Create `{TEST}/security/JwtDecoderTest.java`: using `JwtAccessTokenIssuer` (T037) and the decoder bean from T067, assert a freshly issued credential decodes; an expired one (fixed `Clock` in the past), a tampered one (flip a payload char), one signed by a different key pair (`RsaKeyFixtures`), one with a wrong `iss`, and one with a wrong `aud` are each rejected with `JwtException`; an access credential issued by a previous key is rejected after the configured public key is replaced.
- [X] T065 [P] [US3] Create `{TEST}/security/SecurityChainTest.java` (`@WebMvcTest`/`@SpringBootTest` + MockMvc, importing `SecurityConfiguration`, `JwtDecoderConfiguration`, `SecurityProperties`, `JwtProperties` and the test-only controller `{TEST}/security/SecurityProbeController.java` that exposes `GET /probe/protected` returning the authenticated `Jwt` subject, and `POST /api/v1/auth/register` as a public stub): (a) `Authorization: Bearer <valid>` → 200 and body equals the `sub`; (b) no header, malformed token, expired, tampered, wrong-key, wrong-issuer, wrong-audience → 401 and the controller is never invoked (assert with a call counter); (c) `POST /api/v1/auth/register` without credential → not 401; each default public endpoint in `contracts/configuration.md` (including `/swagger-ui/**` and `/v3/api-docs/**` sample paths and `/actuator/health`) is reachable without credential; (d) `OPTIONS /probe/protected` with `Origin` and `Access-Control-Request-Method` and no credential, when the origin is in `cors.allowed-origins` → 2xx with CORS headers; with the default empty origins → request is denied cross-origin (no `Access-Control-Allow-Origin` header); (e) the session is stateless (no `Set-Cookie`, no `JSESSIONID`); CSRF is not required for `POST` with a valid bearer token.
- [X] T066 [P] [US3] Create `{TEST}/security/KeyFailureStartupTest.java`: `ApplicationContextRunner` (or `@SpringBootTest` expecting failure) with the security configuration and: (a) private key property missing, (b) public key property missing, (c) private key malformed, (d) public key malformed, (e) key pair that does not match → each context fails to start and the root-cause message names the offending property (`identity-access.security.jwt.private-key` / `.public-key`) and does not contain key material.

### Implementation for User Story 3

- [X] T067 [US3] Create `{MAIN}/security/JwtDecoderConfiguration.java` (depends on T009, T011): `@Configuration` exposing a `JwtDecoder` bean built with `NimbusJwtDecoder.withPublicKey((RSAPublicKey) pemKeys.publicKey()).build()` and a `DelegatingOAuth2TokenValidator` of `JwtValidators.createDefaultWithIssuer(props.issuer())` plus an audience validator that requires `props.audience()` in `aud`. A key failure throws `IllegalStateException` from `PemKeys` so start-up stops.
- [X] T068 [P] [US3] Create `{MAIN}/security/SecurityProperties.java`: `@ConfigurationProperties("identity-access.security")` record with `List<String> publicEndpoints` (defaults exactly as in `contracts/configuration.md`) and nested `Cors cors` with `List<String> allowedOrigins` (default empty).
- [X] T069 [US3] Create `{MAIN}/security/SecurityConfiguration.java` (depends on T067, T068): adapted from `services/message/**/SecurityConfig.java` (read it first to copy the shape): `@Configuration @EnableWebSecurity` with one `SecurityFilterChain` — `sessionManagement` STATELESS, CSRF disabled, `cors` using a `CorsConfigurationSource` built from `SecurityProperties.cors().allowedOrigins()` (empty = no origin allowed), `authorizeHttpRequests`: `OPTIONS /**` permitted, `/actuator/health/**` permitted, each configured public endpoint permitted via `requestMatchers`, `anyRequest().authenticated()`; `oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))` so the `JwtDecoder` bean is used; 401 for unauthenticated requests (default bearer-token entry point). Do NOT add a custom authentication filter, access-token verifier or request/response logging filter (FR-013).
- [X] T070 [US3] Run `mvn -pl identityaccess-infrastructure -am verify -Dtest='JwtDecoderTest,SecurityChainTest,KeyFailureStartupTest'` from `services/identity-access-next` and fix failures until all US3 tests pass.

**Checkpoint**: US3 complete — protected endpoints validate credentials natively; key misconfiguration blocks start-up.

---

## Phase 6: User Story 4 - Understand Where Storage Code Lives (Priority: P2)

**Goal**: Structure rules are executable: the build fails on a layering violation, a misplaced adapter, a missing/duplicate adapter, forbidden folders or forbidden dependencies.

**Independent Test**: `ArchitectureTest` passes on the delivered code and a deliberately violating fixture is rejected by the same rules.

- [X] T071 [P] [US4] Create `{TEST}/structure/ArchitectureTest.java` (ArchUnit, `@AnalyzeClasses(packages = "com.minewaku.chatter.identityaccess")`) encoding the four rules in `contracts/adapter-map.md`: (1) no class in `{CORE}.(user|authentication|emailverification|shared)..` depends on `{INFRA}..`, `org.springframework.data..`, `org.springframework.jdbc..`, `org.springframework.data.redis..`, `com.nimbusds..`, `org.springframework.security..`, `jakarta.mail..`, `jakarta.servlet..`, `org.springframework.web..`; (2) rule 3: no class named `*Row` or `*Record` is public (and `*Properties`/`*Configuration` are only public inside `security`, `integration` or their adapter package); (3) rule 4: no package named `service`, `helper`, `config` or `persistence` exists under `{INFRA}`; (4) adapters are not referenced from core.
- [X] T072 [P] [US4] Create `{TEST}/structure/PortAdapterMapTest.java`: for each row in the table of `contracts/adapter-map.md` (hard-code the 11 rows as data in the test), find all classes in `{INFRA}.adapter.outbound..` that implement the port and assert exactly one implementation exists, it has the stated simple name, and it sits in the stated package (SC-002, FR-001). Also assert every interface under `{CORE}..internal.port..` and `{CORE}.shared.port..` is in the table so a new port forces the table to be updated.
- [X] T073 [P] [US4] Create `{TEST}/structure/ArchitectureRuleNegativeTest.java` (SC-006): compile-time fixtures under `{TEST}/structure/violations/` (a fake class in package `{CORE}.user.internal.model` importing `{INFRA}.security.TokenDigest`, and a fake adapter class named `MisplacedAdapter` in `{INFRA}.adapter.outbound.user`) are checked with ArchUnit's `ArchRule.check(JavaClasses)` built from only those fixture classes via `ClassFileImporter`; assert each rule throws `AssertionError`. Fixtures are in a test package excluded from `ArchitectureTest`'s `@AnalyzeClasses` through `ImportOption`.
- [X] T074 [P] [US4] Create `{TEST}/structure/ForbiddenDependencyTest.java` (SC-008, FR-024): assert the module's resolved classpath and sources contain no class or resource referencing outbox, CDC/Debezium, Kafka routing, Vault credential rotation or `io.jsonwebtoken` (scan `target/classes` names and `pom.xml` dependency coordinates from the test classpath; fail with the offending name). Also assert no class named `JwtAuthenticationFilter`, `GlobalLoggingFilter` or `*AccessTokenVerifier` exists.
- [X] T075 [P] [US4] Create `{TEST}/structure/LoggingRuleTest.java` (SC-004 logging rule, `contracts/configuration.md`): ArchUnit/source scan asserting that no class in `{INFRA}..` calls an SLF4J/`System.out` logging method with an argument expression whose variable name matches `(?i)password|hash|rawToken|refreshToken|accessToken|privateKey|token`; keep the allow-list explicit and empty by default. Fail with the class, line and argument name.
- [X] T076 [US4] Run `mvn -pl identityaccess-infrastructure -am verify -Dtest='ArchitectureTest,PortAdapterMapTest,ArchitectureRuleNegativeTest,ForbiddenDependencyTest,LoggingRuleTest'` from `services/identity-access-next` and make the delivered code pass: move any misplaced class, rename or relocate per `contracts/adapter-map.md`, remove leftover unused folders such as `service`, `helper`, `config`. Fix production code, not the rules.

**Checkpoint**: US4 complete — folder convention and dependency boundaries are enforced by the build.

---

## Phase 7: User Story 5 - One Shared Database Stack per Type (Priority: P2)

**Goal**: `infrastructure/components` gains one PostgreSQL and one Redis stack, both with `admin`/`123456`, on `chatter-net`, persistent data, no collisions with existing components.

**Independent Test**: From a clean checkout run each documented start command, connect with `admin`/`123456`, restart, and confirm data persists; start Kafka, Vault and both new stacks together without a port conflict.

This phase has no dependency on Phases 1–6; it can run first or in parallel. US1/US2 manual validation in [quickstart.md](quickstart.md) needs it.

- [X] T077 [P] [US5] Create `infrastructure/components/postgresql/docker-compose.yml` exactly per `contracts/shared-components.md`: service and container `postgresql-chatter`, image `postgres:17`, env `POSTGRES_USER=admin`, `POSTGRES_PASSWORD=123456`, `POSTGRES_DB=chatter`, ports `${POSTGRES_PORT:-5432}:5432`, volume `./data:/var/lib/postgresql/data`, healthcheck `pg_isready -U admin -d chatter`, `restart: unless-stopped`, network `chatter-net` declared as `networks: chatter-net: name: chatter-net` (copy the declaration style from `infrastructure/components/kafka/docker-compose.yml`). Do NOT set `wal_level=logical`, replication slots or init users.
- [X] T078 [P] [US5] Create `infrastructure/components/postgresql/.gitignore` (`data/`), `infrastructure/components/postgresql/README.md` (start `docker compose -p postgresql_chatter -f docker-compose.yml up -d` from the component folder, stop, connect `psql "postgresql://admin:123456@localhost:5432/chatter"`, port variable, schema-per-service rule, reset link) and `infrastructure/components/postgresql/docs/runbooks/reset.md` (stop, delete `data/`, start clean). Mirror the README tone of `infrastructure/components/vault/README.md`.
- [X] T079 [P] [US5] Create `infrastructure/components/redis/config/redis.conf` with `user default off`, `user admin on >123456 ~* &* +@all`, `appendonly yes`; and `infrastructure/components/redis/docker-compose.yml` per `contracts/shared-components.md`: service and container `redis-chatter`, image `redis:7`, command `redis-server /usr/local/etc/redis/redis.conf`, mounted `./config/redis.conf` and `./data:/data`, ports `${REDIS_PORT:-6379}:6379`, healthcheck `redis-cli --user admin --pass 123456 --no-auth-warning ping` expecting `PONG`, `restart: unless-stopped`, network `chatter-net` declared with `name: chatter-net`.
- [X] T080 [P] [US5] Create `infrastructure/components/redis/.gitignore` (`data/`), `infrastructure/components/redis/README.md` (start `docker compose -p redis_chatter -f docker-compose.yml up -d`, connect `redis-cli --user admin --pass 123456`, key-prefix sharing rule `identity-access:`) and `infrastructure/components/redis/docs/runbooks/reset.md`.
- [X] T081 [P] [US5] Update `infrastructure/AGENTS.md` (FR-023): under **Components** add `* \`components/postgresql/\` → \`components/postgresql/README.md\`` and `* \`components/redis/\` → \`components/redis/README.md\``. Touch nothing else in that file.
- [X] T082 [US5] Validate the stacks (depends on T077–T080): from `infrastructure/components/postgresql` and `infrastructure/components/redis` start each with its README command; confirm `psql "postgresql://admin:123456@localhost:5432/chatter" -c "select 1"` and `redis-cli --user admin --pass 123456 --no-auth-warning ping` succeed; restart both with `docker compose restart` and confirm a created table row / Redis key survives; run `docker compose config -q` in both folders and `docker ps --format '{{.Names}} {{.Ports}}'` to confirm no host-port collision with the Kafka and Vault components (existing host ports: 2181, 5002, 5003, 5440–5442, 6380, 6387–6389, 8080, 9052). Verify `git status` shows no changes under `infrastructure/components/{kafka,vault,cloudflare}` (SC-007, FR-025).

**Checkpoint**: US5 complete — shared stacks start from a clean checkout.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation across stories.

- [X] T083 Add an end-to-end wiring test `{TEST}/EndToEndWiringTest.java` (`@SpringBootTest` with `PostgreSqlTestSupport` + `RedisTestSupport`, a test `JavaMailSender` stub, generated test keys): the full application context starts; each of the 11 core ports in `contracts/adapter-map.md` resolves to exactly one bean (`getBeansOfType(port)` size 1); Flyway ran in `identity_access`; `/actuator/health` returns `UP` with both containers running (US1 scenario 1). Depends on US1–US3.
- [X] T084 Run the SC-003/SC-004 end-to-end sweep in `{TEST}/EndToEndPersistenceTest.java`: register an account through the real beans, verify its email, authenticate (issue + store a session), restart the Spring context against the same containers, and assert the account, the unexpired verification token and the session are found unchanged (SC-003); scan the full PostgreSQL `user_account` table, every Redis key/value under the prefix and the captured log output for the plain password, raw verification token and raw refresh credential and assert zero matches (SC-004).
- [X] T085 Run the full automated check from `services/identity-access-next`: `mvn -pl identityaccess-infrastructure -am verify` (unit, Testcontainers, ArchUnit, Spotless `check`); then `mvn spotless:apply` and re-run if formatting changes are needed. Expected: all green (quickstart step 4).
- [X] T086 Run the manual quickstart per `specs/003-infrastructure-adapters/quickstart.md` steps 1–7 (stacks, `.local` keys, Mailpit, `spring-boot:run`, register → verify, Redis/PostgreSQL inspection, restart persistence, protected-endpoint calls). Where the presentation module lacks the sign-in/refresh endpoints, rely on the contract tests (as quickstart step 5.2 states) and say so in the commit message. Fix any quickstart command that no longer matches the implementation in `quickstart.md` only.
- [X] T087 [P] Update `specs/003-infrastructure-adapters/quickstart.md` and `contracts/*.md` only if implementation forced a change (renamed property, port or class); otherwise leave them untouched. Do not edit `spec.md` or `plan.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 → T002 → T003 strictly sequential (same pom, then build check). T004, T005, T006 are independent and may run alongside T001–T003.
- **Foundational (Phase 2)**: depends on T003. T007–T014 are all [P].
- **US1, US2, US3, US4**: each depends only on Phases 1–2. They may proceed in parallel.
- **US5 (Phase 7)**: independent of everything above (only creates files under `infrastructure/`); needed for manual validation (T086).
- **Polish (Phase 8)**: T083–T084 need US1–US3 complete; T085–T086 need every story complete.

### Cross-story file touches

- T037 (US2 `JwtAccessTokenIssuer`) uses `PemKeys`/`JwtProperties` from Phase 2; T064 (US3 decoder test) uses T037, so if US3 is built before US2's issuer, build T037 first.
- US4 rule checks (T071–T076) assert the final package layout, so run T076 after the adapters of US1–US3 exist.

### Within Each Story

- Tests are written first and must fail before the matching implementation (they will not compile until the class exists; create an empty class or interface stub if needed to see a red run).
- Persistence records/mappers → repositories; Lua scripts before the Redis repository that loads them.
- Run the story's `mvn … -Dtest=…` task (T027, T063, T070, T076) before moving on.

### Parallel Opportunities

- Phase 2: T007–T014 all at once.
- US1: T015–T020 (tests) together; then T021, T022, T023, T026 together; T024 → T025.
- US2: sub-groups 4a (T028–T037), 4c (T040–T045) and 4d (T046–T055) and 4e (T056–T062) are independent of each other; inside each, test and production file pairs marked [P] can be written together.
- US4: T071–T075 together.
- US5: T077–T081 together.

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together:
Task: "UserAccountRowMapperTest in {TEST}/adapter/outbound/user/write/jdbc/UserAccountRowMapperTest.java"
Task: "JdbcUserAccountRepositoryTest in {TEST}/adapter/outbound/user/write/jdbc/JdbcUserAccountRepositoryTest.java"
Task: "JdbcUserAccountQueryRepositoryTest in {TEST}/adapter/outbound/user/read/jdbc/JdbcUserAccountQueryRepositoryTest.java"
Task: "FlywaySchemaPlacementTest in {TEST}/integration/postgresql/FlywaySchemaPlacementTest.java"

# Then the independent production files:
Task: "PostgreSqlConfiguration"   Task: "UserAccountRow"   Task: "UserAccountRows"   Task: "JdbcUserAccountQueryRepository"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 (build green) → Phase 2 (foundation) → Phase 3 (US1).
2. **STOP and VALIDATE**: accounts persist in `identity_access`, duplicates and stale writes behave, schema isolation holds.
3. US5 (stacks) can be done at this point so a developer can run the service against the shared PostgreSQL.

### Incremental Delivery

1. Setup + Foundational → build is green.
2. US1 → persistent accounts (MVP).
3. US2 → remaining ports have real adapters (end-to-end register → verify → sign in is possible).
4. US3 → endpoints protected natively.
5. US4 → structure rules lock the layout.
6. US5 → shared stacks documented (may be done anywhere after Phase 1).
7. Polish → end-to-end tests and quickstart run-through.

### Suggested order if working alone (matches plan.md Delivery Order)

Phase 1 → US5 (T077–T082) → Phase 2 → US2 sub-group 4a → US3 → US1 → US2 sub-groups 4b–4e → US4 → Phase 8.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps each task to a spec user story for traceability.
- Core stays unchanged; if a port cannot be implemented as declared, stop and raise a separate core change (plan.md Risks).
- Known core follow-ups (not tasks here): reorder `VerifyEmailService` (account save before token save), add a dedicated `VerificationTokenGenerator` port.
- Commit after each task or logical group; end commit messages with the attribution line required by the session reminder.
