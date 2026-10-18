# Implementation Plan: Account Change Endpoints

**Branch**: `identity-access-next` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-account-change-endpoints/spec.md`

## Summary

Add the missing account actions to `identityaccess-presentation` and make the three behaviors match the spec: **close** now needs the current password (and moves from `DELETE /users/@me` to `POST /users/@me/close` so the password rides in the body), **change username** drops its password check, and **change email** becomes a two-step flow. Step one (`POST /users/@me/email-change`, password-confirmed) stores a *pending email change* and mails a token to the new address only; the account keeps its old email. Step two (`GET /users/email-change/confirm?token=…`, public) applies the new email. The pending change lives in Redis next to the existing verification tokens, reusing their Lua scripts with a separate key prefix, so there is no database migration and a registration token can never complete an email change. Core gets two use cases (request, confirm), a small model and two ports; infrastructure gets one Redis repository, one mail notifier, and a template. Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (unchanged)

**Primary Dependencies**: No new dependency. Reuses Spring Data Redis, Spring Mail, Thymeleaf, and the existing `TokenDigest`, `RedisScripts`, and `UniqueStringIdGenerator`

**Storage**: Redis for the pending change (key prefix `identity-access:email-change:`); PostgreSQL unchanged, no migration

**Testing**: JUnit 5 + AssertJ; core service tests with in-memory fakes; MockMvc slice tests per controller; Redis adapter contract test (Testcontainers) for the new repository; mail notifier test with a mocked `JavaMailSender`; e2e suite in `identityaccess-start` using the existing in-memory `RecordingMailSender`; ArchUnit/structure rules; Spotless `check`. No test reads a real mailbox (spec Assumptions).

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend: touches `identityaccess-core`, `identityaccess-infrastructure`, `identityaccess-presentation`, `identityaccess-start` (config and tests)

**Performance Goals**: No new hot path. One extra Redis read on confirm and one `findByEmail` on request and confirm

**Constraints**: A wrong password never ends a session; passwords and tokens are never logged or echoed; presentation imports no infrastructure type and no core `internal` type; core imports no mail or Redis type; the public confirm endpoint ignores `Authorization` like the other public endpoints

**Scale/Scope**: 5 endpoints touched or added (1 changed verb, 2 changed bodies, 2 new), about 14 new production classes, 6 existing production classes edited, 2 Lua scripts reused unchanged, 1 template

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, the infrastructure structure rules, and the approved spec (same basis as specs 003 and 004).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits stay in `identity-access-next/{core,infrastructure,presentation,start}` and `specs/` |
| Core owns contracts; presentation and infrastructure own technology | Pass | Pass: new core ports (`PendingEmailChangeRepository`, `EmailChangeNotifier`) are plain interfaces; Redis and mail types live only in infrastructure |
| Presentation depends only on core's public contracts | Pass | Pass: new controllers use the two new `api` use cases and the changed ones; the ArchUnit rule is extended to cover the new `web` and `dto` classes automatically |
| Every core port has exactly one adapter in the expected package (infrastructure `StructureRules`) | Needs work | Pass: two rows added to the port table (see [core-and-infra-changes.md](contracts/core-and-infra-changes.md)) |
| HTTP errors belong to presentation; stable lowercase codes | Pass | Pass: two new codes (`username_unchanged`, `email_unchanged`) in the existing `ErrorCode` enum |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: new request records override `toString`; `SecretHandlingTest` picks them up by the `Request` suffix; sentinel e2e test extended |
| Earlier specs stay accurate | Needs work | Pass: spec 004 contract (the `DELETE /users/@me` row and FR-016) is updated as a task, see [contracts/core-and-infra-changes.md](contracts/core-and-infra-changes.md) |
| Git/patch paths resolved against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories and uses the per-service `./mvnw` |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/005-account-change-endpoints/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── http-api.md
│   └── core-and-infra-changes.md
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (repository root: `services/identity-access-next/`)

```text
identityaccess-core/src/main/java/.../user/
├── api/command/
│   ├── ChangeUsernameUseCase.java          # Command(userId, newUsername): password removed
│   ├── SoftDeleteUserAccountUseCase.java   # Command(userId, currentPassword)
│   ├── RequestEmailChangeUseCase.java      # new: replaces ChangeEmailUseCase (deleted)
│   └── ConfirmEmailChangeUseCase.java      # new: token -> Confirmed | AlreadyConfirmed | InvalidOrExpired
├── api/exception/DuplicateUserAccountException.java   # adds a message-only constructor
├── internal/model/{UserAccount.java, PendingEmailChange.java (new)}
├── internal/port/{EmailChangeNotifier.java (new), repository/PendingEmailChangeRepository.java (new)}
└── internal/application/command/
    ├── ChangeUsernameService.java, SoftDeleteUserAccountService.java   # edited
    └── RequestEmailChangeService.java, ConfirmEmailChangeService.java  # new (ChangeEmailService deleted)

identityaccess-infrastructure/src/main/
├── java/.../adapter/outbound/emailchange/
│   ├── write/redis/{RedisPendingEmailChangeRepository, PendingEmailChangeRecord, PendingEmailChangeRecordMapper}.java
│   (the notifier itself, MailEmailChangeNotifier, sits in adapter/outbound/verification/mail beside MailVerificationNotifier)
├── java/.../adapter/outbound/verification/mail/{VerificationLinkFactory, VerificationMailProperties}.java   # second link + subject
├── java/.../security/SecurityProperties.java       # default public list gains the confirm path
└── resources/templates/email-change.html           # new

identityaccess-presentation/src/main/java/.../presentation/user/
├── web/{UserController.java (edited), EmailChangeController.java (new)}
└── dto/{CloseAccountRequest, ChangeUsernameRequest, ChangeEmailRequest, EmailChangeStatusResponse}.java   # new
presentation/shared/error/ErrorCode.java            # + USERNAME_UNCHANGED, EMAIL_UNCHANGED

identityaccess-start/src/main/resources/application.yml   # public endpoint + email-change mail settings
identityaccess-start/src/test/java/.../e2e/               # extended AccountManagementE2eTest, new EmailChangeE2eTest
```

**Structure Decision** *(superseded by [spec 006](../006-unify-email-ownership-proof/spec.md): the proof now lives in `emailverification` for both purposes; only the request and confirm services stay in `user`)*: The pending-change logic sits in core's `user` package because it is a change to the user account, not a new account state, and the existing `emailverification` package only knows registration. Infrastructure follows the capability-first layout with a new `emailchange/write/redis` adapter folder; the mail notifier sits beside `MailVerificationNotifier` because they share a package-private exception, the link factory, and the properties. Presentation keeps the `user/web` + `user/dto` layout from the last refactor; the public confirm endpoint gets its own small controller so the protected `UserController` stays free of public routes.

## Complexity Tracking

No constitution violations to justify.
