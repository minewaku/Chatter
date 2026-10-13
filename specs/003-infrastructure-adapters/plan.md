# Implementation Plan: Identity Access Infrastructure Adapters

**Branch**: `master` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-infrastructure-adapters/spec.md`

## Summary

Give `identity-access-next` real implementations for every outbound port the core already declares (password hashing, Snowflake and UUIDv7 identifiers, JWT access credentials, refresh credentials, account repositories, verification and session stores, verification email), replace the old custom JWT filter with native Spring resource-server validation copied in shape from the message service, and add one shared PostgreSQL and one shared Redis stack under `infrastructure/components`. Adapters are organised capability → read/write role → technology; persistence rows are adapter-private records with hand-written mappers (core stays annotation-free). Messaging, CDC, outbox, Vault credential rotation, and the old filters are not carried over. Decisions and rationale: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (Spring Framework 7.0.9, Spring Security 7.0.7, Spring Data 2025.1, Flyway 11.14)

**Primary Dependencies**: Starters for webmvc, security, oauth2-resource-server, data-jdbc, flyway, data-redis, mail, thymeleaf, actuator; PostgreSQL driver; Password4j 1.8.4; `java-uuid-generator`; Lombok (existing). Removed: `jjwt-*`, Spring Cloud Vault starters, raw `flyway-core`

**Storage**: PostgreSQL 17 (accounts, schema `identity_access`); Redis 7 (verification tokens and sessions, key prefix `identity-access:`)

**Testing**: JUnit 5 + AssertJ; Testcontainers (`postgres:17`, `redis:7`) for adapter contract tests; ArchUnit for structure rules; MockMvc for the security chain; Spotless `check` in the build

**Target Platform**: JVM service, Linux/WSL development with Docker for the shared stacks

**Project Type**: Multi-module web-service backend; this feature fills the `identityaccess-infrastructure` module and touches `identityaccess-start` configuration

**Performance Goals**: Account lookup mapping adds under 1% of the measured database lookup time (SC-010); 100,000 Snowflake ids across 8 threads with zero duplicates (SC-005); session listing for 100 retained sessions in one round-trip-bounded read

**Constraints**: Core unchanged; no Spring Data/Redis/JWT/Mail imports in core; no raw secrets in stores or logs; cross-origin denied unless configured; tokens expire without cleanup jobs; one adapter per port

**Scale/Scope**: 11 adapters, 3 shared stores/stacks, 1 security chain, about 40 production classes, 6 Lua scripts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, `infrastructure-structure-design.txt`, and the approved spec.

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits are limited to `identity-access-next/identityaccess-infrastructure`, its `start` configuration, `infrastructure/components/{postgresql,redis}`, and `infrastructure/AGENTS.md` |
| Core owns contracts; infrastructure owns technology (`infrastructure-structure-design.txt`) | Pass | Pass: adapters implement existing ports; no core change in this feature |
| Capability → role → technology package convention; no catch-all `service`/`helper`/`config` folders | Pass | Pass: see adapter map and structure test |
| No messaging, outbox, CDC, Vault rotation, custom auth filter | Pass | Pass: dependencies removed and asserted absent (SC-008) |
| Secrets, tokens, and passwords never stored raw or logged | Pass | Pass: digests only; logging rule in configuration contract; tests on failure paths |
| Git/patch paths resolved against repository root, build commands against the workspace | Pass | Pass: quickstart commands state their working directory |

No violations; Complexity Tracking lists only the deliberate exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/003-infrastructure-adapters/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── adapter-map.md
│   ├── configuration.md
│   └── shared-components.md
├── checklists/requirements.md
└── tasks.md                  # created by /speckit-tasks
```

### Source Code (repository root)

```text
services/identity-access-next/
├── pom.xml                                   # no change expected
├── identityaccess-infrastructure/
│   ├── pom.xml                               # dependency changes (research 1.1–1.3, 7.5)
│   └── src/
│       ├── main/
│       │   ├── java/com/minewaku/chatter/identityaccess/infrastructure/
│       │   │   ├── adapter/outbound/{shared/id, user/{password,write/jdbc,read/jdbc},
│       │   │   │                     authentication, session/{write/redis,read/redis},
│       │   │   │                     verification/{mail,write/redis}}
│       │   │   ├── security/
│       │   │   └── integration/{postgresql,redis,mail}
│       │   └── resources/
│       │       ├── db/migration/V1__create_user_account.sql   # exists, unchanged
│       │       ├── redis/*.lua
│       │       └── templates/email-verification.html
│       └── test/java/com/minewaku/chatter/identityaccess/infrastructure/
│           ├── adapter/outbound/…            # unit + contract tests per adapter
│           ├── security/                     # issuer/decoder + MockMvc chain tests
│           ├── structure/                    # ArchUnit rules
│           └── support/                      # Testcontainers base classes
├── identityaccess-start/
│   └── src/main/resources/application.yml    # defaults from contracts/configuration.md
├── identityaccess-presentation/              # unchanged
├── identityaccess-core/                      # unchanged
└── .local/                                   # git-ignored dev keys (quickstart); add to .gitignore

infrastructure/
├── AGENTS.md                                 # list new components
└── components/
    ├── postgresql/{docker-compose.yml, README.md, .gitignore, docs/runbooks/reset.md}
    └── redis/{docker-compose.yml, config/redis.conf, README.md, .gitignore, docs/runbooks/reset.md}
```

**Structure Decision**: Follow `identity-access-next/infrastructure-structure-design.txt` (capability → role → technology) with the two noted deviations (shared ID adapters under `shared/id`; folder names per adapter map). Existing component folders are mirrored for the two new database stacks.

## Delivery Order (input for `/speckit-tasks`)

1. **Build and components**: fix the infrastructure `pom.xml`; add the two component folders and `infrastructure/AGENTS.md` lines; add `application.yml` defaults and `.gitignore` entry for `.local/`.
2. **Foundation adapters (no stores)**: `TokenDigest`, `PemKeys`, `JwtProperties`; Argon2 hasher; Snowflake; UUIDv7; refresh generator. Unit-tested.
3. **Security**: `SecurityConfiguration`, `JwtDecoderConfiguration`, `JwtAccessTokenIssuer`; MockMvc chain tests; key-failure start-up test.
4. **Account persistence**: `PostgreSqlConfiguration`; row, mapper, `JdbcUserAccountRepository`, `JdbcUserAccountQueryRepository`; Flyway placement and duplicate/stale tests; mapping-cost measurement.
5. **Redis**: `RedisConfiguration`; verification repository + scripts; session repository, view repository + scripts; contract tests.
6. **Mail**: `MailConfiguration`, template, link factory, notifier with retry; failure-path log tests.
7. **Structure and cleanup**: ArchUnit rules; dependency-absence check (no messaging, Vault, jjwt); Spotless; quickstart run-through.

## Risks and Follow-ups

- **Account-activation ordering (research 4)**: `VerifyEmailService` saves the Redis token before the account. A failed account write leaves a consumed token on a pending account; recovery is a replacement token. Recommend a separate core change.
- **Verification token source (research 7.3)**: core reuses `UniqueStringIdGenerator` (UUIDv7) for tokens; a dedicated random generator port is a recommended core follow-up.
- **Synchronous mail in registration (research 9)**: an unreachable SMTP server fails registration; accepted until an outbox exists.
- **Boot 4 module split**: starter/auto-configuration names were verified against Central metadata but the module has not been built locally in this session; the first task must build the module and fix any renamed class or property.
- **Docker for tests**: Testcontainers integration tests need a running Docker daemon.

## Complexity Tracking

| Exception | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Redis Lua scripts | Session create/touch/revoke and verification invalidation must be conditional and atomic | `WATCH/MULTI` needs retry loops and cannot express "only if unrevoked and not expired" cleanly |
| Persistence-only row/record classes plus mappers | Core must stay free of storage annotations (clarified decision) | Annotating core classes would make core depend on Spring Data and need converters for value objects |
| Two data stores with no shared transaction | Spec and design doc place accounts in PostgreSQL and tokens/sessions in Redis with exact expiry and no cleanup job | Putting everything in PostgreSQL needs scheduled cleanup, which FR-008 excludes |
