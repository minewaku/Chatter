# Implementation Plan: Unify Email Ownership Proof

**Branch**: `identity-access-next` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-unify-email-ownership-proof/spec.md`

## Summary

Registration verification and email change each keep a copy of the same thing: a short-lived, single-use token that ties an account to an address. Today that is `EmailVerification` (in `emailverification`) and `PendingEmailChange` (in `user`), with two repositories, two Redis adapters, two record shapes and two mappers that differ only in names. This feature keeps the `emailverification` package as the single owner of that proof, adds a required **purpose** (`REGISTRATION`, `EMAIL_CHANGE`) to the model and to every repository call, and deletes the email-change copy. Each flow keeps its own redeem rules and its own mail port.

The storage layout does not change. The two purposes already use separate Redis key families (`verification:*` and `email-change:*`) with identical hash fields and the same two Lua scripts, so one repository class can serve both by choosing the key family from the purpose. Registration links already sent keep working with no migration, and the purpose-binding that spec 005 got from separate key families is kept, now enforced by a required argument instead of by having two classes. Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (unchanged)

**Primary Dependencies**: No new dependency. Reuses Spring Data Redis, `TokenDigest`, `RedisScripts`, `RedisTime`, `UniqueStringIdGenerator`

**Storage**: Redis, unchanged layout: `identity-access:verification:{token,user}:` and `identity-access:email-change:{token,user}:`. `verification-save.lua` and `verification-invalidate-for-user.lua` unchanged. PostgreSQL untouched

**Testing**: JUnit 5 + AssertJ; core service tests with in-memory fakes; Redis adapter contract test (Testcontainers); ArchUnit/structure rules; e2e suite in `identityaccess-start` runs unchanged; Spotless `check`

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend. Touches `identityaccess-core` and `identityaccess-infrastructure` only. `identityaccess-presentation` and `identityaccess-start` are not edited; their tests are the regression net

**Performance Goals**: No change. Same Redis calls per request as today

**Constraints**: No observable change to any request, response, status, error code, mail subject, mail body, or link from specs 001 to 005. Raw tokens are never stored or logged. Core imports no infrastructure, Redis, or mail type. Registration tokens issued before the release stay redeemable

**Scale/Scope**: 1 new enum, 1 new issuer service, about 8 production classes edited, 6 production classes deleted (3 core, 3 infrastructure), 5 test classes deleted or folded, 0 Lua scripts changed

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, the infrastructure structure rules, and the approved spec (same basis as specs 003 to 005).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits stay in `identity-access-next/{core,infrastructure}` and `specs/` |
| Core owns contracts; infrastructure owns technology | Pass | Pass: the merged port stays a plain core interface; Redis types stay in infrastructure |
| Presentation unaffected | Pass | Pass: no presentation or `start` main code is edited. `VerifyEmailUseCase` and `ConfirmEmailChangeUseCase` keep their result types |
| Every core port has exactly one adapter in the expected package (`StructureRules`) | Needs work | Pass: the `PendingEmailChangeRepository` row is removed with the port; the `EmailVerificationRepository` row is unchanged |
| Persistence records and mappers stay package-private | Pass | Pass: `VerificationRecord` and `VerificationRecordMapper` stay package-private; the email-change pair is deleted |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: raw token still only in memory, digest only in Redis, no new logging |
| No observable behavior change | Pass | Pass: result types, error mapping, mail ports, link factory and templates untouched; regression net listed in [quickstart.md](quickstart.md) |
| Earlier specs stay accurate | Needs work | Pass: spec 005 research section 4, data model, and plan "Structure Decision" get a short "superseded by 006" note as a task |
| Git/patch paths against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories and uses the per-service `./mvnw` |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/006-unify-email-ownership-proof/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── core-and-infra-changes.md
│   └── unchanged-surface.md
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (repository root: `services/identity-access-next/`)

```text
identityaccess-core/src/main/java/.../
├── emailverification/internal/
│   ├── model/
│   │   ├── EmailVerification.java            # edited: gains a required purpose; "verified" stays the one word
│   │   └── VerificationPurpose.java          # new enum: REGISTRATION, EMAIL_CHANGE
│   ├── port/repository/EmailVerificationRepository.java   # edited: purpose on find and invalidate
│   └── application/
│       ├── EmailVerificationIssuer.java      # new: the one place that invalidates, generates, stores; owns the 24h lifetime
│       ├── EmailVerificationIssuanceCoordinator.java   # edited: registration only = issuer + VerificationNotifier
│       └── VerifyEmailService.java           # edited: passes REGISTRATION
├── user/internal/
│   ├── model/PendingEmailChange.java                          # DELETED
│   ├── port/repository/PendingEmailChangeRepository.java     # DELETED
│   ├── port/EmailChangeNotifier.java                          # unchanged (mail stays separate)
│   └── application/command/
│       ├── RequestEmailChangeService.java     # edited: uses the issuer with EMAIL_CHANGE; own LIFETIME constant removed
│       ├── ConfirmEmailChangeService.java     # edited: reads and saves through EmailVerificationRepository with EMAIL_CHANGE
│       └── SoftDeleteUserAccountService.java  # edited: invalidateForUser(EMAIL_CHANGE, id)

identityaccess-core/src/test/java/.../
├── emailverification/internal/model/EmailVerificationTest.java        # edited: both purposes, absorbs PendingEmailChangeTest cases
├── emailverification/internal/application/
│   ├── EmailVerificationTestFakes.java        # edited: public so user tests can reuse it
│   ├── EmailVerificationIssuerTest.java       # new: lifetime once, replace per purpose, purposes independent
│   └── EmailVerificationIssuanceCoordinatorTest.java, VerifyEmailServiceTest.java, RequestEmailVerificationServiceTest.java   # wiring only
├── user/internal/model/PendingEmailChangeTest.java                    # DELETED
└── user/internal/application/
    ├── UserAccountTestFakes.java              # edited: in-memory pending-change fake removed
    └── EmailChangeApplicationServiceTest.java, UserAccountApplicationServiceTest.java   # wiring only

identityaccess-infrastructure/src/main/
├── java/.../adapter/outbound/verification/write/redis/
│   ├── RedisEmailVerificationRepository.java  # edited: key family chosen from the purpose
│   ├── VerificationRecord.java                # unchanged
│   └── VerificationRecordMapper.java          # edited: purpose in, purpose out
└── java/.../adapter/outbound/emailchange/     # DELETED (3 classes)

identityaccess-infrastructure/src/test/
├── .../verification/write/redis/{RedisEmailVerificationRepositoryTest, VerificationRecordMapperTest}.java   # edited: run per purpose, add cross-purpose cases
├── .../emailchange/                           # DELETED (2 test classes)
└── .../structure/StructureRules.java          # edited: PendingEmailChangeRepository row removed
```

**Structure Decision**: The proof lives in `emailverification` because that package is named for exactly this concept and already owns its storage port, its Redis adapter, and its lifetime. The `user` services reach it the way `RegisterUserService` already does (a direct call into `emailverification.internal`); a separate pass-through facade is not added because the repository and issuer already are the public surface of that capability and no rule in the repository forbids the call (see research.md, section 7). Purpose is a required argument on every repository operation and part of the stored key, so the separation spec 005 relied on (FR-026) cannot be bypassed by omission. The two confirm services stay separate because their effects differ; only the proof is shared.

## Complexity Tracking

No constitution violations to justify.
