---

description: "Task list for loading JWT keys from Vault and dropping the audience check"
---

# Tasks: JWT Keys from Vault and Audience-Free Token Validation

**Input**: Design documents from `specs/008-jwt-keys-from-vault/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (`signing-keys-secret.md`, `token-validation.md`), quickstart.md

**Tests**: Included. The spec requires 100% rejection or acceptance outcomes (SC-002, SC-004, SC-005, SC-007) and that existing tests keep passing (SC-008), and the plan lists the test classes.

**Organization**: Keys stop being properties and become an injected `RsaKeyPair` bean in the Foundational phase, which compiles and keeps all unit tests green with no Vault involved. User Story 1 adds the Vault loader and makes the running service start from it. User Story 2 adds the failure classification and the refusal to start. User Story 3 is independent of the other two (the audience check) and can be done at any point after Phase 2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Repository root: `/home/minewaku/projects/java/2026/Chatter-identity-access-next`. Build commands run from `services/identity-access-next` with the per-service `./mvnw`, never a bare `mvn`. Git paths are relative to the repository root. `verify` on the full reactor fails on a pre-existing `check` goal in `identityaccess-start`; use `-pl identityaccess-infrastructure -am verify` for infrastructure and `-pl identityaccess-start -am test` for the e2e suite.

Path shorthands used below (all relative to the repository root):

- `SEC_MAIN` = `services/identity-access-next/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure/security`
- `SEC_TEST` = `services/identity-access-next/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure/security`
- `INFRA_TEST` = `services/identity-access-next/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure`
- `E2E` = `services/identity-access-next/identityaccess-start/src/test/java/com/minewaku/chatter/identityaccess/start/e2e`
- `START_RES` = `services/identity-access-next/identityaccess-start/src/main/resources`
- `VAULT_DOCS` = `infrastructure/components/vault/docs/runbooks`

---

## Phase 1: Setup

**Purpose**: Establish that the starting point is green and that the tooling the tests need is available.

- [X] T001 From `services/identity-access-next`, confirm Docker is up (`docker ps`) and run `./mvnw -pl identityaccess-infrastructure -am verify`; record that it passes before any edit. Note which tests currently pass so later failures are attributable.
- [X] T002 [P] Confirm the Vault image used by the tests is available: `hashicorp/vault:1.17.6` is already present locally (confirmed), so no pull is needed. Write the tag you confirmed into the constant used by T013. Dev mode (`server -dev`) mounts `secret/` as KV version 2 automatically; check with `docker run --rm -d -e VAULT_DEV_ROOT_TOKEN_ID=t -p 18200:8200 hashicorp/vault:<tag>` then `curl -s -H "X-Vault-Token: t" localhost:18200/v1/sys/mounts | grep -o '"secret/"'`, and stop the container afterwards.
- [X] T003 [P] Confirm `git status --short -- services/identity-access-next` shows no uncommitted source changes unrelated to this feature. (Found 10 modified files under `identityaccess-core` from earlier unrelated work, plus pending changes under `infrastructure/components/vault`; this feature must stage only its own files and leave those unstaged.) The untracked `services/identity-access-next/email.txt` is the owner's credentials file and must stay untouched and uncommitted.

---

## Phase 2: Foundational (keys become an injected bean)

**Purpose**: Remove key material from `JwtProperties` and `PemKeys`' property handling, and make the decoder and issuer take an `RsaKeyPair` bean. After this phase nothing reads a key from a property, but no production code creates the bean yet, so the service cannot start; User Story 1 closes that.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. The module does not compile between T004 and T012; treat T004 to T012 as one change and run the build at the checkpoint.

- [X] T004 Add `spring-vault-core` (no version; managed by the Spring Boot BOM) as a compile dependency in `services/identity-access-next/identityaccess-infrastructure/pom.xml`, next to the other `spring-boot-starter-*` dependencies. Do not add any other dependency; Mockito and Testcontainers are already available for tests.
- [X] T005 [P] Create `SEC_MAIN/SigningKeysUnavailableException.java`: a public unchecked `extends IllegalStateException` with a nested public `enum Reason { VAULT_DISABLED, STORE_UNREACHABLE, CREDENTIAL_REJECTED, SECRET_NOT_FOUND, FIELD_MISSING, KEY_INVALID, KEY_PAIR_MISMATCH }`, a constructor `(Reason reason, String message)` and `(Reason reason, String message, Throwable cause)`, and a `reason()` accessor. The message must never hold key text (see [data-model.md](data-model.md)).
- [X] T006 Edit `SEC_MAIN/PemKeys.java`: delete `load(JwtProperties)` and the `file:` / `classpath:` branch and the `DefaultResourceLoader` import; add `public static RsaKeyPair parse(String privatePem, String publicPem)` that decodes the two values, builds the keys, and checks they match. Replace the `PRIVATE_KEY_PROPERTY` / `PUBLIC_KEY_PROPERTY` constants with field names `private-key` and `public-key` used in the messages. Failures throw `SigningKeysUnavailableException` with `KEY_INVALID` (blank/not PEM/not Base64/not a PKCS#8 RSA private key/not an X.509 RSA public key) or `KEY_PAIR_MISMATCH` ("private-key and public-key do not form a matching key pair"). The `KEY_INVALID` exception must not chain the parser's cause and its message must not contain any part of the input (see [contracts/signing-keys-secret.md](contracts/signing-keys-secret.md)). Keep `RsaKeyPair` and the begin/end-line and whitespace tolerance exactly as today (FR-008). A blank value is reported as `FIELD_MISSING` by the loader (T023), so `parse` may assume non-blank input but must still reject blank defensively as `KEY_INVALID`.
- [X] T007 Edit `SEC_MAIN/JwtProperties.java` per [data-model.md](data-model.md): remove `privateKey` and `publicKey`; the record becomes `JwtProperties(String issuer, String audience, Duration accessTokenTtl, Keys keys)` with a nested `public record Keys(String backend, String path)` whose compact constructor defaults a blank `backend` to `secret` and a blank `path` to `common/jwt/rs256`, and a null `keys` becomes `new Keys(null, null)`. Keep the existing defaults (`issuer` = `identity-access`, `audience` = `chatter`, `accessTokenTtl` = 15 minutes) and the rule that `accessTokenTtl` "must be positive".
- [X] T008 Edit `services/identity-access-next/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure/adapter/outbound/authentication/JwtAccessTokenIssuer.java`: the constructor becomes `JwtAccessTokenIssuer(PemKeys.RsaKeyPair keys, JwtProperties properties, Clock clock)`; delete the `PemKeys.load(properties)` call. The claims written by `issue(...)` do not change, including `.audience(List.of(properties.audience()))` (FR-012).
- [X] T009 Edit `SEC_MAIN/JwtDecoderConfiguration.java`: `jwtDecoder` becomes `JwtDecoder jwtDecoder(JwtProperties properties, PemKeys.RsaKeyPair keys)` and uses `keys.publicKey()`; delete the `PemKeys.load` call. Leave the issuer and audience validators as they are for now (User Story 3 removes the audience one). Update the class Javadoc so it no longer says an invalid key stops start-up through `PemKeys`; it now says the key pair is supplied by `SigningKeysConfiguration`.
- [X] T010 [P] Add `public static PemKeys.RsaKeyPair keys(KeyPair pair)` to `INFRA_TEST/support/RsaKeyFixtures.java`, returning `new PemKeys.RsaKeyPair((RSAPrivateKey) pair.getPrivate(), (RSAPublicKey) pair.getPublic())`. It is the only way tests build keys from now on.
- [X] T011 Update the tests that built keys through `JwtProperties` so they compile and keep their current expectations:
  - `SEC_TEST/JwtDecoderTest.java`: `properties(...)` becomes `new JwtProperties(issuer, audience, Duration.ofMinutes(15), null)`; `issue(...)` takes the key pair too and builds `new JwtAccessTokenIssuer(RsaKeyFixtures.keys(pair), properties, clock)`; the decoder is built with `new JwtDecoderConfiguration().jwtDecoder(properties, RsaKeyFixtures.keys(pair))`. Keep the `aCredentialWithTheWrongAudienceIsRejected` test unchanged for now (User Story 3 changes it).
  - `INFRA_TEST/adapter/outbound/authentication/JwtAccessTokenIssuerTest.java`: new constructor and `JwtProperties` shape; the "broken key" test at its line ~69 is replaced by a test that `PemKeys.parse(" ", publicPem)` throws `SigningKeysUnavailableException` (it belongs in `PemKeysTest`, T012; delete it here).
  - `SEC_TEST/SecurityChainTest.java` and `SEC_TEST/SecurityChainConfig.java`: remove the two `DynamicPropertySource` key entries; add to `SecurityChainConfig` a `@Bean PemKeys.RsaKeyPair signingKeys()` returning `RsaKeyFixtures.keys(SecurityChainTest.PAIR)` (make `PAIR` package-visible), and change the private `token(...)` helper to the new issuer constructor.
  - `SEC_TEST/RevokedAccessTokenFilterTest.java`: same treatment as `SecurityChainTest` (find how it supplies keys today, replace that with an `RsaKeyPair` bean in its test configuration, and change its `token(...)` helper).
  - `INFRA_TEST/adapter/outbound/session/write/redis/RedisSessionWriterFactory.java` and `RedisAuthenticationSessionRepositoryTest.java`: `new JwtProperties(null, null, null, null, accessTokenTtl)` becomes `new JwtProperties(null, null, accessTokenTtl, null)`.
- [X] T012 Edit `SEC_TEST/PemKeysTest.java`: replace `PemKeys.load(props(...))` with `PemKeys.parse(privatePem, publicPem)`; delete the test that reads keys from `file:` locations (that route no longer exists); the missing, garbage and mismatched cases now expect `SigningKeysUnavailableException` with reasons `KEY_INVALID`, `KEY_INVALID` and `KEY_PAIR_MISMATCH`. Add one test asserting that the `KEY_INVALID` message does not contain the garbage input and that the exception has no cause. Delete `SEC_TEST/KeyFailureStartupTest.java`; its cases move to the loader and start-up tests in User Story 2.

**Checkpoint**: From `services/identity-access-next`, `./mvnw -pl identityaccess-infrastructure -am verify` passes with no Vault in the picture. `grep -rn "PemKeys.load\|privateKey()\|publicKey()" identityaccess-infrastructure/src` shows only `RsaKeyPair` accessors.

---

## Phase 3: User Story 1 - The Service Gets Its Signing Keys from the Secrets Store (Priority: P1) 🎯 MVP

**Goal**: A service started with no JWT key value locally reads the pair from Vault at `secret/common/jwt/rs256`, signs and verifies with it, and ignores any leftover local value.

**Independent Test**: Seed a Vault container with a key pair, start the service with only the Vault coordinates and token, sign in and call a protected endpoint: both succeed. Start it again with garbage in the old key properties: same result.

### Tests for User Story 1

- [X] T013 [P] [US1] Create `INFRA_TEST/support/VaultContainerSupport.java`: a small test helper that starts one shared `GenericContainer<?>` of the image tag from T002 with `VAULT_DEV_ROOT_TOKEN_ID=test-root-token`, command `server -dev -dev-listen-address=0.0.0.0:8200`, exposed port 8200 and a wait strategy on `/v1/sys/health`; exposes `host()`, `port()`, `rootToken()`, and `put(String path, Map<String,String> fields)` / `delete(String path)` that call `POST /v1/secret/data/<path>` (body `{"data": {...}}`) and `DELETE /v1/secret/metadata/<path>` with the JDK `HttpClient` and header `X-Vault-Token`. No new library.
- [X] T014 [P] [US1] Create `SEC_TEST/VaultSigningKeyLoaderTest.java` with the success cases first: (a) keys seeded at `common/jwt/rs256` load and `RsaKeyPair` round-trips a sign/verify; (b) keys with surrounding whitespace and begin/end lines load (FR-008); (c) a custom `keys.backend` / `keys.path` in `JwtProperties.Keys` is honoured, using a second path seeded in the container. Build the `VaultTemplate` by hand: `new VaultTemplate(VaultEndpoint.create(host, port) with scheme http, new TokenAuthentication(token))`. The failure cases are added in User Story 2 (T021).
- [X] T015 [P] [US1] Edit `E2E/E2eSupport.java`: start a `VaultContainerSupport` container in the static block next to PostgreSQL and Redis; seed `common/jwt/rs256` with `TestKeys.privatePem()` and `TestKeys.publicPem()` as fields `private-key` and `public-key`; remove `"spring.cloud.vault.enabled=false"` from the `@SpringBootTest` properties; in `properties(...)` replace the two `identity-access.security.jwt.*-key` registrations with `spring.cloud.vault.scheme` = `http`, `.host`, `.port` and `.token` = the container's values. The e2e module needs the helper from T013, which lives in the infrastructure test tree: copy it to `E2E/VaultContainerSupport.java` (same code, package `...start.e2e`) instead of adding a test-jar dependency.
- [X] T016 [P] [US1] Create `E2E/LeftoverKeysE2eTest.java` extending `E2eSupport`, with a `@DynamicPropertySource` method named `leftoverKeyProperties` (not `properties`, which would hide the base class's method) that registers `identity-access.security.jwt.private-key` and `identity-access.security.jwt.public-key` as garbage (the properties the service used to read). Assert that the context starts (the test body registers a verified account, signs in, and calls a protected endpoint with the issued token) and that a token signed with `TestKeys` verifies, i.e. the Vault pair, not the garbage, is in use (spec User Story 1 scenario 4, FR-004, SC-007).

### Implementation for User Story 1

- [X] T017 [US1] Create `SEC_MAIN/VaultSigningKeyLoader.java` (package-private `final class`) with `PemKeys.RsaKeyPair load()` taking `VaultOperations` and `JwtProperties.Keys` in its constructor. It reads `vault.opsForKeyValue(keys.backend(), KeyValueBackend.KV_2).get(keys.path())`, takes fields `private-key` and `public-key` from `getData()`, and returns `PemKeys.parse(...)`. For now a null response, a missing field or any exception from Vault propagates as a `SigningKeysUnavailableException` with a placeholder reason; the full classification is User Story 2 (T023). Never log a field value.
- [X] T018 [US1] Create `SEC_MAIN/SigningKeysConfiguration.java`: a `@Configuration` with `@Bean PemKeys.RsaKeyPair signingKeys(ObjectProvider<VaultOperations> vault, JwtProperties properties)` that calls the loader once. When no `VaultOperations` bean exists it throws `SigningKeysUnavailableException(VAULT_DISABLED, "Signing keys can only be loaded from Vault, but Vault support is disabled")`. Add `@EnableConfigurationProperties(JwtProperties.class)` only if no other class already enables it (grep for it first). Check that the bean is picked up by the component scan of `StartApplication`.
- [X] T019 [US1] Edit `START_RES/application.yml`: remove `private-key` and `public-key` under `identity-access.security.jwt` (lines ~107-108) and keep `issuer`, `audience`, `access-token-ttl`; under `spring.cloud.vault` replace `enabled: ${IDENTITY_ACCESS_VAULT_ENABLED:true}` with `enabled: true`; add under `identity-access.security.jwt` a commented `keys:` block showing the defaults (`backend: secret`, `path: common/jwt/rs256`) without setting them. Leave `fail-fast: false`, the `kv` block and `token: ${VAULT_TOKEN:}` as they are (the mail credentials depend on them).
- [X] T020 [P] [US1] Edit `VAULT_DOCS/identity-access-mail.md`: delete the table row for `IDENTITY_ACCESS_VAULT_ENABLED`; add a short section "JWT signing keys" stating that the service also reads `secret/common/jwt/rs256` (fields `private-key`, `public-key`, written by `configure-secrets.sh jwt`), that the same `VAULT_TOKEN` must be allowed to read it, that the service refuses to start without it, and that `IDENTITY_ACCESS_JWT_PRIVATE_KEY` / `IDENTITY_ACCESS_JWT_PUBLIC_KEY` are no longer read and can be removed from any `.env`. Keep the document's existing tone and table style.

**Checkpoint**: `./mvnw -pl identityaccess-infrastructure -am verify` and `./mvnw -pl identityaccess-start -am test` pass; User Story 1 works on its own against a seeded Vault, with every existing e2e class running on the Vault-sourced keys.

---

## Phase 4: User Story 2 - The Service Refuses to Run Without a Usable Key Pair (Priority: P1)

**Goal**: Each of the seven failure cases ends in a refused start with its own message and no key text anywhere.

**Independent Test**: Run the loader against a container for each case and run a Spring context for the start-up behaviour; every case fails with the expected reason and message.

### Tests for User Story 2

- [X] T021 [P] [US2] Extend `SEC_TEST/VaultSigningKeyLoaderTest.java` with one test per reason, asserting `reason()` and the message prefix from [contracts/signing-keys-secret.md](contracts/signing-keys-secret.md): `STORE_UNREACHABLE` (a template pointing at a closed local port; get a free port by opening and closing a `ServerSocket`), `CREDENTIAL_REJECTED` (a template using the wrong token), `CREDENTIAL_REJECTED` for a blank token, `SECRET_NOT_FOUND` (nothing at a fresh path), `FIELD_MISSING` for each of: no `public-key`, no `private-key`, blank `public-key`; `KEY_INVALID` for a garbage private key and for a garbage public key; `KEY_PAIR_MISMATCH` (private key from one pair, public key from another). Every failure test also asserts that none of the seeded secret values, nor any 8-character fragment of them, appears in the message or in any cause message. Run them and write down what Vault and Spring Vault actually do for the 403 and the blank token; T023 and T024 must match that behaviour (this is the open detail in [research.md](research.md) section 4).
- [X] T022 [P] [US2] Create `SEC_TEST/SigningKeysStartupTest.java` using `ApplicationContextRunner` with `SigningKeysConfiguration`, `JwtDecoderConfiguration` and `JwtProperties`: (a) no `VaultOperations` bean fails the context with `VAULT_DISABLED` in the root cause chain; (b) a `VaultOperations` bean backed by the container and a valid secret starts it; (c) after a failed start, correcting the secret and starting a new runner succeeds with no other change (spec User Story 2 scenario 5); (d) FR-009: after the context has started, make the `VaultOperations` bean fail on every further call and assert that issuing and decoding a token still work; (e) when the old `identity-access.security.jwt.private-key` and `.public-key` properties are set to valid keys of a different pair and the store is unreachable, the context still fails, so local values are never a fallback (FR-004, SC-007). Reuse a helper like the old `rootMessages(Throwable)` to read the cause chain.

### Implementation for User Story 2

- [X] T023 [US2] Edit `SEC_MAIN/VaultSigningKeyLoader.java` to classify outcomes per [research.md](research.md) section 4 and [contracts/signing-keys-secret.md](contracts/signing-keys-secret.md): `ResourceAccessException` (or the cause type that T021 showed) → `STORE_UNREACHABLE` naming `scheme://host:port` (read it from `VaultTemplate.getVaultEndpoint()` if available, otherwise take it as a constructor argument from `SigningKeysConfiguration`); a 403 `VaultException` → `CREDENTIAL_REJECTED`; null response → `SECRET_NOT_FOUND`; absent or blank field → `FIELD_MISSING` naming the field; `PemKeys.parse` exceptions pass through. Use exactly the message wording of the contract's start-up table. Chain the cause only for `STORE_UNREACHABLE` and `CREDENTIAL_REJECTED`.
- [X] T024 [US2] Make `SigningKeysConfiguration` read the property `spring.cloud.vault.token` from the `Environment` first and throw `CREDENTIAL_REJECTED` ("no credential configured") when it is blank, regardless of what Spring's own message says; cover it with a case in `SigningKeysStartupTest`.
- [X] T025 [P] [US2] Edit `E2E/SecretLeakageE2eTest.java`: add the test key pair to the sentinels it searches for in logs and responses (`TestKeys.privatePem()` body and `TestKeys.publicPem()` body, matched on their Base64 text without the begin/end lines), so a start-up or request path that prints either fails SC-002's "no key content in any output". Also fetch `/actuator/health` and `/actuator/info` and search them for the same sentinels (spec User Story 1 scenario 3 names health output). Keep the existing sentinels.

**Checkpoint**: `./mvnw -pl identityaccess-infrastructure -am verify` shows the seven reasons failing correctly and `SigningKeysStartupTest` green; `./mvnw -pl identityaccess-start -am test` still green.

---

## Phase 5: User Story 3 - Incoming Tokens Are No Longer Rejected for Their Audience (Priority: P2)

**Goal**: A token with a valid signature, expected issuer, future expiry and live session is accepted whatever audience it names, or none; every other rejection stays.

**Independent Test**: Present otherwise valid tokens with another audience and with no audience: both accepted. Present expired, foreign-signature, wrong-issuer and revoked-session tokens: all still rejected.

### Tests for User Story 3

- [X] T026 [P] [US3] Edit `SEC_TEST/JwtDecoderTest.java`: replace `aCredentialWithTheWrongAudienceIsRejected` with `aCredentialNamingAnotherAudienceIsAccepted` (decodes, and the decoded `aud` is `another-app`); add `aCredentialWithNoAudienceIsAccepted` by encoding a token directly with `NimbusJwtEncoder` and no `aud` claim (the issuer always writes one); keep the expired, tampered, foreign-key, wrong-issuer and replaced-key tests unchanged.
- [X] T027 [P] [US3] Edit `SEC_TEST/SecurityChainTest.java`: find the case in the "unauthorized" list (and any dedicated test) that uses an audience other than `chatter` and move it to the accepted cases (200 on `/probe/protected`); the remaining cases in the list (null, malformed, expired, tampered, foreign key, wrong issuer) must still return 401.
- [X] T028 [P] [US3] Extend `E2E/TestKeys.java`: add `static String tokenWithAudience(long userId, String sessionId, String audienceOrNull)` that signs with the service's test key, issuer `identity-access`, and the given audience (no claim when null), by passing a null or empty audience through the existing `signed(...)` helper (change it to skip `.audience(...)` when the argument is null). Then create `E2E/AudienceE2eTest.java` extending `E2eSupport`: sign in a verified account, take its `sid` from the issued token via `claim(...)`, and assert 200 on a protected endpoint for `tokenWithAudience(..., "another-app")` and `tokenWithAudience(..., null)`; assert 401 for `expiredToken`, `foreignKeyToken` and `wrongIssuerToken`; assert a freshly issued token has `aud` equal to `["chatter"]` (spec scenarios 1 to 6, SC-004, SC-005).

### Implementation for User Story 3

- [X] T029 [US3] Edit `SEC_MAIN/JwtDecoderConfiguration.java`: delete `audienceValidator(...)` and the `DelegatingOAuth2TokenValidator`, `OAuth2Error`, `OAuth2TokenValidator`, `OAuth2TokenValidatorResult` and `Jwt` imports it needed; call `decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()))`. Update the class Javadoc to say the decoder checks signature, expiry, not-before and issuer, and that audience is deliberately not checked (FR-010 to FR-013). Leave `JwtProperties.audience` in place, used by the issuer only.

**Checkpoint**: `T026` to `T028` pass; User Story 3 holds independently of the Vault work.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Format, structure rules, stale references, and the end-to-end validation in the quickstart.

- [X] T030 From `services/identity-access-next`, run `./mvnw spotless:apply` and then `./mvnw -pl identityaccess-infrastructure -am verify`; the infrastructure `StructureRules` (ArchUnit) must pass with the new package-private `VaultSigningKeyLoader` and the public exception. Fix any rule that the new classes break by moving or narrowing them, not by relaxing the rule.
- [X] T031 [P] Search for stale references and remove them: `grep -rn "IDENTITY_ACCESS_JWT_PRIVATE_KEY\|IDENTITY_ACCESS_JWT_PUBLIC_KEY\|IDENTITY_ACCESS_VAULT_ENABLED\|identity-access.security.jwt.private-key\|identity-access.security.jwt.public-key" --include=*.java --include=*.yml --include=*.md --include=*.sh --include=*.env* .` from the repository root, ignoring `target/` and `specs/` history files. Anything left that sets or documents these as inputs is deleted or reworded; the spec and plan files of feature 008 keep their historical mentions.
- [X] T032 Run `./mvnw -pl identityaccess-start -am test` and confirm the whole e2e suite passes against the Vault container (SC-008). Tests that exist only to assert the audience rejection were removed in T026 and T027; nothing else may be edited to make a test pass.
- [X] T033 Walk through [quickstart.md](quickstart.md) sections 3 to 5 against the real Vault stack (`infrastructure/components/vault`): start the service with no JWT variables, sign in, call a protected endpoint, repeat with leftover variables set, then trigger each failure in the section 4 table and confirm the message matches [contracts/signing-keys-secret.md](contracts/signing-keys-secret.md). Restore the pair with `configure-secrets.sh jwt` and confirm the service starts again with no other change. Record the result in the task's checkbox note.
- [X] T034 [P] In `specs/008-jwt-keys-from-vault/spec.md` set `**Status**: Draft` to `Implemented` only after T030 to T033 pass; leave the rest of the spec untouched.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies; T002 and T003 can run alongside T001.
- **Foundational (Phase 2)**: depends on Setup. Blocks all user stories. T004 first; T005, T010 are independent files; T006 to T009 depend on T005 and T007 (the compile order is `SigningKeysUnavailableException` → `PemKeys` → `JwtProperties` → issuer and decoder); T011 and T012 depend on T006 to T010.
- **User Story 1 (Phase 3)**: depends on Foundational. T013 to T016 are tests and fixtures; T017 → T018 → T019 are the implementation chain; T020 is a docs file and independent.
- **User Story 2 (Phase 4)**: depends on User Story 1 (it extends the loader and its tests).
- **User Story 3 (Phase 5)**: depends only on Foundational. It can run in parallel with User Stories 1 and 2, or first.
- **Polish (Phase 6)**: depends on all stories you intend to ship.

### Within Each User Story

- Tests are written and shown failing before the implementation that makes them pass (T014/T015/T016 before T017 to T019; T021/T022 before T023/T024; T026 to T028 before T029).
- `JwtDecoderConfiguration.java` is edited by T009 and then T029; `VaultSigningKeyLoader.java` by T017 then T023; `VaultSigningKeyLoaderTest.java` by T014 then T021. Do not run those pairs in parallel.

### Parallel Opportunities

- Setup: T002, T003.
- Foundational: T005 and T010 together; T011's sub-bullets touch different files and can be split.
- User Story 1: T013, T014, T015, T016 (different files); T020 alongside the implementation chain.
- User Story 2: T021, T022, T025.
- User Story 3: T026, T027, T028.
- Stories 1 and 3 can proceed in parallel once Phase 2 is done.

---

## Parallel Example: User Story 1

```bash
# After Phase 2, in parallel (different files):
Task: "Create INFRA_TEST/support/VaultContainerSupport.java"            # T013
Task: "Create SEC_TEST/VaultSigningKeyLoaderTest.java (success cases)"  # T014
Task: "Edit E2E/E2eSupport.java for the Vault container"                # T015
Task: "Create E2E/LeftoverKeysE2eTest.java"                             # T016
Task: "Edit VAULT_DOCS/identity-access-mail.md"                         # T020
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1 and Phase 2 (keys become an injected bean; unit tests green).
2. Complete Phase 3: the service starts from Vault and ignores local values.
3. **Stop and validate**: run the e2e suite and quickstart section 3. The service works end to end against Vault. User Story 1 alone leaves failure messages unspecific (a placeholder reason), so do not deploy before User Story 2.

### Incremental Delivery

1. Setup + Foundational → bean-based keys, nothing visible changes.
2. User Story 1 → keys from Vault (MVP).
3. User Story 2 → refuse to start with precise messages. Ship 1 and 2 together.
4. User Story 3 → audience check removed; ship any time after Phase 2.

### Order Suggested for One Developer

Phase 1 → Phase 2 → US3 (small, finishes the decoder file while it is fresh) → US1 → US2 → Polish.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks.
- [Story] label maps each task to a user story in `spec.md`.
- Never log, assert-print or commit key text. The test key pair is generated in memory at test time.
- Do not edit `profile`, `api-gateway`, `message` or `configure-secrets.sh`; splitting the private and public halves of the secret and per-service Vault policies are out of scope (spec Assumptions).
- Commit after each task or logical group; use the per-service `./mvnw`, never bare `mvn`.

---

## Implementation Notes (deviations from the plan)

- **Location of the Vault code.** The loader and its configuration live in `identityaccess-start`
  (`start/configuration/VaultSigningKeyLoader.java`, `SigningKeysConfiguration.java`), not in `identityaccess-infrastructure`
  as planned. The infrastructure module has an architecture guard from spec 003 (`ForbiddenDependencyTest`, FR-024) that
  forbids `org.springframework.vault..` dependencies, class names containing `Vault`, and the word `vault` in its pom. The
  start module is the composition root and already holds the Vault dependency, so the guard stays untouched.
  `PemKeys`, `JwtProperties`, `SigningKeysUnavailableException`, the issuer and the decoder stay in infrastructure and
  receive the pair as a `PemKeys.RsaKeyPair` bean. Affected tasks: T004 (no pom change in infrastructure), T013, T014, T017,
  T018, T021, T022 (code and tests are under `identityaccess-start`).
- **Test helpers.** One `start/support/VaultContainerSupport` (public, with the template helpers) and `start/support/RsaKeys`
  serve the loader tests, the start-up tests and `E2eSupport`; there is no second copy. The infrastructure end-to-end
  application (`E2eTestApplication`) supplies its own `RsaKeyPair` bean.
- **E2E wiring (T015).** Spring Cloud Vault reads its connection while the environment is prepared, before
  `@DynamicPropertySource` values exist, so `E2eSupport` sets `spring.cloud.vault.*` as system properties in its static block.
- **Image (T002).** `hashicorp/vault:1.17.6`, already present locally.
- **Messages.** For key problems the location is appended: `Signing keys could not be loaded: private-key is not a valid
  PKCS#8 RSA private key (secret/common/jwt/rs256)`. The contract was updated to match.
- **T024.** Kept. In a real boot Spring Cloud Vault fails first for a missing token ("requires either a Token
  (spring.cloud.vault.token) ..."), which already names the credential; the extra check covers contexts where that
  import is absent and is tested in `SigningKeysStartupTest`.
- **T025.** Done as a new test (`noSigningKeyMaterialReachesLogsHealthInfoOrErrorResponses`) beside the existing
  sentinel test, covering logs, `/actuator/health`, `/actuator/info` and error responses.
- **Manual walkthrough (T033), run against the real Vault stack on 2026-10-21.** Packaged jar started with no
  `IDENTITY_ACCESS_JWT_*` variables and only `VAULT_TOKEN`: it started and `/actuator/health` answered. Tokens signed with the
  private key read from Vault were accepted for audience `chatter`, `another-app` and none (the answer was the application's own
  `403 account_inaccessible` for an unknown user, i.e. past authentication); expired, wrong-issuer and foreign-key tokens got
  401. Starting with garbage `IDENTITY_ACCESS_JWT_*` values set still worked (ignored). Refused starts, each with its own
  message: Vault port closed (`Vault is not reachable at http://localhost:59999`), wrong token (`Vault rejected the service
  credential for secret/common/jwt/rs256`), another path (`no secret at secret/does/not/exist`), no token (Spring Cloud Vault's
  "requires either a Token (spring.cloud.vault.token) ..."). No key text appeared in any log. The Vault data was only read, never
  changed, so the missing-field, invalid-key and mismatched-pair cases (quickstart 4, last three rows) were not repeated by hand;
  `VaultSigningKeyLoaderTest` covers them against a Vault container.
