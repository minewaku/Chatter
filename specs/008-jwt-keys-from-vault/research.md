# Research: JWT Keys from Vault and Audience-Free Token Validation

Date: 2026-10-21. No `NEEDS CLARIFICATION` was left in the Technical Context; this records the decisions the spec left to planning.

## 1. How the service reads the key pair

**Decision**: A loader calls Vault directly through the `VaultOperations` bean that Spring Cloud Vault's `VaultAutoConfiguration` registers (`vaultTemplate`, conditional on `spring.cloud.vault.enabled`), using `opsForKeyValue("secret", KV_2).get("common/jwt/rs256")`. It runs once, from a `@Bean` method that returns the `RsaKeyPair`.

**Rationale**:
- FR-004 needs locally supplied key values to have no effect. Anything delivered as a property (environment variable, `.env`, `application.yml`) outranks or collides with a Vault property source, so a property-based design cannot guarantee it. A direct read never looks at the Environment.
- FR-006 needs five distinguishable failures. A property-based design only ever sees "the property is absent", whatever the cause.
- The existing `fail-fast: false` is there so a missing mail secret does not stop the service. A direct read fails on its own terms and leaves that setting alone.

**Alternatives considered**:
- Add `vault://secret/common/jwt/rs256` to `spring.config.import` and bind the keys as properties. Rejected for the three reasons above. It also forces the Vault field names `private-key` and `public-key` into the global property namespace, where they can collide.
- Set `fail-fast: true`. Rejected: it changes the mail behaviour that the spec says stays as it is, and it still cannot name the cause.
- Use Spring Cloud Vault's `VaultTemplate` through a hand-built second client. Rejected: the auto-configured one already carries the host, port, scheme and token the rest of the service uses.

## 1b. Where the loader lives

**Decision**: In `identityaccess-start` (`start.configuration`), not in `identityaccess-infrastructure`.

**Rationale**: found when the first build ran. `ForbiddenDependencyTest` (spec 003, FR-024) forbids `org.springframework.vault..` and `org.springframework.cloud.vault..` dependencies, class names containing `Vault`, and `vault` in the infrastructure pom. Those rules were written to keep the old service's Vault rotation out; they are blunt, but relaxing an architecture guard is a larger decision than this feature needs. The start module is the composition root and already depends on Vault for the mail credentials, so wiring the secrets source there is also the better layering.

**Alternatives considered**: narrow the rule to forbid only the old rotation types. Rejected for now: a separate decision with its own review.

## 2. Bean shape and who consumes it

**Decision**: `SigningKeysConfiguration` exposes one `PemKeys.RsaKeyPair` bean. `JwtDecoderConfiguration.jwtDecoder` and `JwtAccessTokenIssuer` take that bean instead of calling `PemKeys.load(JwtProperties)` themselves.

**Rationale**: Today both classes parse the same properties separately, so a change of source would be made in two places and Vault would be read twice. One bean means one read (FR-009) and one place for failure handling.

**Alternatives considered**: keep `PemKeys.load(JwtProperties)` and let `JwtProperties` be filled from Vault. Rejected: it keeps key material in a properties record, which is the local route being removed.

## 3. Removing the local routes inside `PemKeys`

**Decision**: `PemKeys` keeps the PEM parsing and the matching check, and loses `load(JwtProperties)` and the `file:` / `classpath:` resource loading. It gains `parse(String privatePem, String publicPem)`, called by the loader. The property names in its messages are replaced by a short description of the Vault field.

**Rationale**: FR-014 says there is no other route. The `file:` support is exactly such a route (a key path in a local file). It is also unused: no `application.yml` or script supplies one.

**Alternatives considered**: keep `file:` for tests. Rejected: tests do not need it; they build the pair in memory (`RsaKeyFixtures`) or seed a container.

## 4. Failure classification

**Decision**: The loader maps outcomes to a `SigningKeysUnavailableException` carrying a `Reason`:

| Reason | Trigger |
|---|---|
| `VAULT_DISABLED` | no `VaultOperations` bean (`spring.cloud.vault.enabled=false`) |
| `STORE_UNREACHABLE` | I/O failure reaching Vault (`ResourceAccessException`) |
| `CREDENTIAL_REJECTED` | Vault answers 403 (`VaultException` carrying a 403 status) |
| `SECRET_NOT_FOUND` | the read returns no secret (404) |
| `FIELD_MISSING` | the secret has no `private-key`, no `public-key`, or one is blank (message names which) |
| `KEY_INVALID` | not valid PEM, not a PKCS#8 RSA private key, or not an X.509 RSA public key |
| `KEY_PAIR_MISMATCH` | the two keys do not form a pair |

Messages contain the Vault mount, path, field name and the reason only. Never a value, never a fragment of one, and the cause chain is not attached for `KEY_INVALID` (the underlying parser message can quote input).

**Rationale**: matches FR-005 to FR-007 and the five misconfiguration cases in SC-002 (the two extra reasons, `VAULT_DISABLED` and `SECRET_NOT_FOUND`/`FIELD_MISSING` splits, give the operator a more exact message).

**Observed during implementation** (against a real Vault 1.17.6 dev container, `VaultSigningKeyLoaderTest`): an unreachable store surfaces as `ResourceAccessException`; a rejected credential surfaces as a `VaultException` whose message starts `Status 403` (the loader matches that text, there is no status accessor); a missing secret returns `null`. With an empty `VAULT_TOKEN` Spring Cloud Vault fails first, while the environment is prepared, with "Cannot create authentication mechanism for TOKEN. This method requires either a Token (spring.cloud.vault.token) or a token file at ~/.vault-token", which already names the credential; the loader's own blank-token check covers contexts without that import.

## 5. What happens to `IDENTITY_ACCESS_VAULT_ENABLED`

**Decision**: Remove the flag. `spring.cloud.vault.enabled` is fixed to `true` in `application.yml`.

**Rationale**: With Vault as the only source, switching it off can only produce a service that refuses to start, so the switch is a trap, not an option. The runbook table that lists it is updated in the same change.

**Alternatives considered**: keep the flag and fail with `VAULT_DISABLED`. Rejected: dead configuration that always fails. `VAULT_DISABLED` stays as a reason only for contexts that set the property directly.

## 6. Where the secret lives and whether it is configurable

**Decision**: Defaults `backend = secret` and `path = common/jwt/rs256`, exposed as `identity-access.security.jwt.keys.backend` and `identity-access.security.jwt.keys.path` in `JwtProperties`. The field names `private-key` and `public-key` are constants, not settings.

**Rationale**: the location is not secret, tests need to point the loader at their container's seeded path, and a future split of the private and public halves (out of scope) would only change the path. The field names are part of the contract with `configure-secrets.sh` and `profile`, so making them settable would invite drift.

## 7. Audience

**Decision**: Delete `audienceValidator` and the `DelegatingOAuth2TokenValidator` wrapper from `JwtDecoderConfiguration`. The decoder uses `JwtValidators.createDefaultWithIssuer(issuer)`. `JwtProperties.audience` stays, with its default `chatter`, and is used only by `JwtAccessTokenIssuer` when it writes the claim.

**Rationale**: FR-010 to FR-013. `createDefaultWithIssuer` still checks expiry, not-before and issuer (FR-011). The session-revocation filter is a separate filter and is untouched.

**Alternatives considered**: remove `audience` from `JwtProperties` and hard-code the claim. Rejected: FR-012 keeps the claim, and the value is already configurable and tested.

## 8. Test strategy for Vault

**Decision**: A `hashicorp/vault` container in dev mode (`-dev`, fixed root token, KV v2 mounted at `secret/`) started with `GenericContainer`, in two places:
- `VaultSigningKeyLoaderTest` (infrastructure): builds a `VaultTemplate` by hand against the container, seeds secrets over its HTTP API, and covers all seven reasons plus success. Unreachable uses a closed port; rejected uses a wrong token; the other cases seed different secret contents.
- `E2eSupport` (start): the existing PostgreSQL and Redis containers gain a Vault container seeded with `TestKeys`. Properties `spring.cloud.vault.host/port/scheme/token` point at it.

**Rationale**: the failures that matter (403, 404, connection refused) are exactly what a mock would only imitate. The suite already runs containers for Redis and PostgreSQL, so this adds no new kind of dependency, and `GenericContainer` needs no new library.

**Alternatives considered**: Mockito mocks of `VaultOperations`. Rejected for the main matrix because they would encode my guess of Spring Vault's exception types. `testcontainers-vault` module. Rejected: not in the local repository, and `GenericContainer` is enough.

## 9. Migration and operations

**Decision**: No data migration. The pair is already in Vault. The operator removes `IDENTITY_ACCESS_JWT_PRIVATE_KEY` and `IDENTITY_ACCESS_JWT_PUBLIC_KEY` from wherever they set them; if they forget, the values are ignored (spec clarification, no warning). A new environment needs only a Vault token (`VAULT_TOKEN`) plus a one-time `configure-secrets.sh jwt` if the pair is not there yet.

**Rationale**: matches the spec's clarifications. The runbook note covers the one new prerequisite that did not exist before: Vault must be up, unsealed and seeded before the service starts, including on a developer machine.

## 10. What is deliberately left alone

- Splitting `private-key` and `public-key` into separate paths, and per-service Vault policies (spec Assumptions: out of scope). Today every service uses the root or dev token, so the exposure of the private key to `profile` and the gateway is unchanged by this feature.
- `profile`, `api-gateway` and `message` token validation.
- The revoked-session filter and its Redis keys.
- Key rotation without a restart.
