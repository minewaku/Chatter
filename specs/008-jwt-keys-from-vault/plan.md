# Implementation Plan: JWT Keys from Vault and Audience-Free Token Validation

**Branch**: `identity-access-next` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-jwt-keys-from-vault/spec.md`

## Summary

Today the identity service gets its RSA signing key pair from two plain properties (`identity-access.security.jwt.private-key` and `.public-key`), which `application.yml` fills from the environment variables `IDENTITY_ACCESS_JWT_PRIVATE_KEY` and `IDENTITY_ACCESS_JWT_PUBLIC_KEY`. The same pair already sits in Vault at `secret/common/jwt/rs256` (written by `configure-secrets.sh jwt`, read by `profile`), but the identity service never reads that path. This feature makes Vault the only source, removes every local route, and drops the audience check on incoming tokens.

The approach is a small loader, in the application (`start`) module, that reads the pair straight from Vault with the Vault client Spring Cloud Vault already provides, instead of binding it as properties. Reading it directly is the only way to meet three requirements together: ignore any locally supplied key value (FR-004), tell an unreachable store from a rejected credential from a missing key (FR-006), and not depend on `fail-fast: false`, which the mail credentials need. The loader produces one `RsaKeyPair` bean, read once at start-up (FR-009), and both the token issuer and the JWT decoder take that bean. The audience validator is deleted from the decoder; the issuer keeps writing the audience claim (FR-012).

Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8, Spring Cloud 2025.1.1 (Spring Cloud Vault 5.0.1, Spring Vault 4.0.0) (all unchanged)

**Primary Dependencies**: No new dependency. The loader uses `spring-vault-core`, already on the classpath of `identityaccess-start` through `spring-cloud-starter-vault-config`. `identityaccess-infrastructure` stays free of Vault types (see the Constitution Check). Spring Security OAuth2 resource server (unchanged)

**Storage**: Vault KV v2, mount `secret`, path `common/jwt/rs256`, fields `private-key` and `public-key`. No database or Redis change

**Testing**: JUnit 5 + AssertJ. New loader test against a real Vault dev-mode container (`hashicorp/vault`, started as a `GenericContainer`, as Redis already is). Existing decoder, issuer, security-chain and `PemKeys` tests edited. The e2e suite runs against the same container. Spotless `check`

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks (Vault on port 5002)

**Project Type**: Multi-module web-service backend. Touches `identityaccess-infrastructure` (key parsing, properties, issuer, decoder, tests) and `identityaccess-start` (the Vault loader and configuration, `application.yml`, tests). `identityaccess-core` and `identityaccess-presentation` are not edited

**Performance Goals**: No change at request time. One extra Vault read at start-up

**Constraints**: Key material never appears in logs, health output, error responses, or failure messages. Locally supplied key values have no effect. The service refuses to start on any of the failure cases in FR-005 and says which one. No request, response, status or error code changes for any valid or invalid token except the audience case

**Scale/Scope**: 3 new production classes (loader and configuration in `start`, failure exception in infrastructure), 4 edited (`PemKeys`, `JwtProperties`, `JwtDecoderConfiguration`, `JwtAccessTokenIssuer`), 1 `application.yml`, about 12 test classes edited, 6 new test classes and helpers, 1 runbook rewrite

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, the infrastructure structure rules, and the approved spec (same basis as specs 003 to 006).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits stay in `identity-access-next/{infrastructure,start}`, `specs/`, and one Vault runbook note. `profile`, `api-gateway`, `message` and `configure-secrets.sh` are not edited |
| Core owns contracts; infrastructure owns technology | Pass | Pass: core and presentation do not change; the Vault client is used only in the composition root |
| Secrets, tokens, and passwords never logged or echoed | Needs work | Pass: failure messages carry the secret path and field name only; a test asserts no key content in any failure message and in the e2e leakage test |
| Infrastructure structure rules (`StructureRules`, `ForbiddenDependencyTest`) | Pass | Needed a change of plan: `ForbiddenDependencyTest` (spec 003, FR-024) forbids `org.springframework.vault..` dependencies, class names containing `Vault`, and `vault` in the infrastructure pom. The loader and its configuration therefore live in `identityaccess-start`; infrastructure only receives a `PemKeys.RsaKeyPair` bean. The guard is unchanged |
| No behavior change except what the spec states | Pass | Pass: the only observable differences are the audience case and the start-up failure messages. Matrix in [contracts/token-validation.md](contracts/token-validation.md) |
| No local route for signing keys (spec FR-014) | Needs work | Pass: `file:` and `classpath:` loading is removed from `PemKeys`, the key properties are removed from `JwtProperties`, and tests supply keys by building the `RsaKeyPair` or by seeding a Vault container |
| Earlier specs stay accurate | Pass | Pass: no earlier spec describes where the keys come from |
| Git/patch paths against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories and uses the per-service `./mvnw` |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/008-jwt-keys-from-vault/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── signing-keys-secret.md   # Vault layout, configuration, start-up outcomes
│   └── token-validation.md      # accept/reject matrix for incoming tokens
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 ($speckit-tasks, not created here)
```

### Source Code (repository root)

```text
services/identity-access-next/
├── identityaccess-infrastructure/            # pom unchanged: no Vault dependency (ForbiddenDependencyTest)
│   └── src/
│       ├── main/java/.../infrastructure/
│       │   ├── security/
│       │   │   ├── PemKeys.java                         # edit: parse(private, public); public field names; no file:/classpath: loading
│       │   │   ├── JwtProperties.java                   # edit: drop privateKey/publicKey; add keys location
│       │   │   ├── JwtDecoderConfiguration.java         # edit: take the RsaKeyPair bean; audience validator deleted
│       │   │   └── SigningKeysUnavailableException.java # new: carries a Reason, message never holds key text
│       │   └── adapter/outbound/authentication/
│       │       └── JwtAccessTokenIssuer.java            # edit: constructor takes RsaKeyPair
│       └── test/java/.../infrastructure/
│           ├── security/    PemKeysTest, JwtDecoderTest, SecurityChain*Test, SecurityChainConfig, RevokedAccessTokenFilterTest   # edit
│           ├── e2e/         E2eProperties, E2eTestApplication   # edit: supplies its own RsaKeyPair bean
│           ├── support/     RsaKeyFixtures                      # edit: keys(KeyPair)
│           └── adapter/outbound/...                             # edit: new JwtProperties shape
└── identityaccess-start/
    ├── src/main/java/.../start/configuration/
    │   ├── SigningKeysConfiguration.java        # new: the RsaKeyPair bean, read once at start-up
    │   └── VaultSigningKeyLoader.java           # new, package-private: read, classify, parse
    ├── src/main/resources/application.yml       # edit: drop key properties and IDENTITY_ACCESS_VAULT_ENABLED
    └── src/test/java/.../start/
        ├── configuration/  VaultSigningKeyLoaderTest, SigningKeysStartupTest   # new: real Vault container
        ├── support/        VaultContainerSupport, RsaKeys                       # new
        └── e2e/            E2eSupport, TestKeys, SecretLeakageE2eTest           # edit
                            AudienceE2eTest, LeftoverKeysE2eTest                 # new

infrastructure/components/vault/docs/runbooks/
└── identity-access-mail.md                      # edit: JWT keys section, flag removed
```

**Structure Decision**: Reading the secrets store is a composition-root concern, like the mail credentials already are, so the loader and its configuration sit in `identityaccess-start/configuration`. Infrastructure keeps everything about keys that is not Vault-specific (PEM parsing, the pair type, the failure type, the issuer and decoder) and receives the pair as a bean. This keeps the existing architecture guard intact without relaxing it.

## Complexity Tracking

> Empty: no Constitution Check violations.
