# Implementation Plan: Registration Email Verification Core

**Branch**: `001-email-verification-core` (Spec Kit identifier; checkout is `master`) | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-email-verification-core/spec.md`, narrowed by the user's 2026-10-10 decision. The registration portions of `services/identity-access-next/missing-core-behaviors-part-2-emailverification.txt` remain the implementation brief; its email-change portions belong to a separate feature.

## Summary

Add token-gated activation of newly registered accounts in `identityaccess-core`. Successful registration synchronously saves one verification and requests one delivery. A pending account may request a replacement without disclosing account existence. Presenting its current, unexpired token consumes it and activates the matching account. Reuse the existing account model and command patterns; add no email-change work or infrastructure adapters.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Existing Spring Boot 4.0.8 parent, `spring-context`, and `spring-tx` in `identityaccess-core`; existing USER value objects and account repository.

**Storage**: Core repository interfaces only. Persistence technology and schema are deferred to a later adapter feature.

**Testing**: JUnit through `spring-boot-starter-test`, deterministic core fakes, fixed `Clock`, focused Maven core tests, and workspace formatting/reactor checks. Real rollback and concurrency require later adapter integration tests.

**Target Platform**: Java server core module of the `identity-access-next` Maven reactor.

**Project Type**: Core application/domain library in a multi-module service.

**Performance Goals**: No latency or throughput target is specified for the core-only feature. Each eligible issuance requests exactly one delivery before returning.

**Constraints**: Registration verification only; default token lifetime 24 hours via configurable positive `Duration`; injected `Clock`; synchronous coordination; token absent from results/logs. No endpoint, adapter, limiter, event, generic token framework, message template, or activation by account ID.

**Scale/Scope**: Two public verification use cases, one verification model, three technology-neutral ports, and integration with the existing registration command. Email change is excluded. Data volume is not specified and does not change these core contracts.

## Constitution Check

*GATE: Checked before Phase 0 and again after Phase 1.*

`.specify/memory/constitution.md` is an unfilled template; it has no ratified project principles or enforceable gate. The operative constraints are the narrowed spec and the relevant registration portions of the source brief. The design satisfies their core-only, synchronous, transactional, token-gated boundaries. No exception is required.

## Project Structure

### Documentation (this feature)

```text
specs/001-email-verification-core/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── commands.md
│   └── ports.md
└── tasks.md                  # Existing task list is stale until $speckit-tasks runs
```

### Source Code (repository root)

```text
services/identity-access-next/
└── identityaccess-core/
    └── src/
        ├── main/java/com/minewaku/chatter/identityaccess/
        │   ├── emailverification/
        │   │   ├── command/                 # Public request and verify use cases
        │   │   └── internal/
        │   │       ├── application/         # Issuance and command handlers
        │   │       ├── model/               # EmailVerification lifecycle
        │   │       └── port/                # Repository, generator, notifier
        │   └── user/internal/application/command/
        │       └── RegisterUserService.java # New-registration integration only
        └── test/java/com/minewaku/chatter/identityaccess/
            ├── emailverification/           # Model and command tests
            └── user/                        # Existing registration tests
```

**Structure Decision**: Add the verification capability inside `identityaccess-core`; reuse USER's `Email`, `UserId`, `UserAccount`, and repository. The existing `UserAccount.activateAfterVerification()` already performs the pending-to-active transition. No change-email source file is in this feature.

## Phase 0: Research Decisions

[research.md](research.md) records the choices and alternatives. No `NEEDS CLARIFICATION` item remains. Key decisions are: restrict issuance to newly registered or still-pending accounts; preserve registration's existing results; share internal issuance between registration and replacement requests; retain used tokens through expiry; and use a supplied clock with a configurable 24-hour default.

## Phase 1: Design and Contracts

- [data-model.md](data-model.md) defines account and verification fields, invariants, and result precedence.
- [commands.md](contracts/commands.md) fixes public request/verify results and the new-registration effect.
- [ports.md](contracts/ports.md) defines technology-neutral repository, generator, and notifier contracts.
- [quickstart.md](quickstart.md) gives runnable validation scenarios after implementation.

The registration and verification handlers coordinate account/token writes in transactions. Delivery is requested synchronously and failure propagates. Provider acceptance cannot be rolled back after a later commit failure; a still-pending account can request a replacement. Core fake tests cannot prove database rollback or concurrent single use; the later adapter must do so.

**Post-design gate**: Rechecked scope and contracts against the spec and constitution. No email-change effect, adapter, endpoint, limiter, event, or public activation-by-ID path is introduced. The existing `tasks.md` still covers email change and must be regenerated before implementation.
