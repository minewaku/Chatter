# Implementation Plan: Authentication and Sessions

**Branch**: `master` | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-authentication-sessions/spec.md`

## Summary

Add the approved authentication capability to `identityaccess-core`: sign in active accounts, issue an access credential and a non-rotating refresh credential, renew access through a bounded `AuthenticationSession`, revoke the current or selected sessions, list safe session views, and revoke sessions when password change, suspension, or deletion succeeds. Reuse account and password rules; define plain core ports for session persistence, credential generation, and access-token issuance. Keep token hashing, UUIDv7 generation, JWT signing, cookies, and datastore wiring behind adapters outside this core delivery. See [research.md](research.md) for decisions and [contracts/](contracts/) for the planned interfaces.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Existing Spring context/transaction support in core; JDK time types; no new core library  
**Storage**: Storage-neutral core ports; no adapter or migration in this feature. Future adapter stores only a refresh-credential digest and must support conditional session changes.  
**Testing**: JUnit 5 through Maven, with in-memory fakes and fixed `Clock`  
**Target Platform**: JVM service module `services/identity-access-next/identityaccess-core`  
**Project Type**: Multi-module web-service backend; this feature is the core capability  
**Performance Goals**: Meet spec SC-006 (at least 95% of eligible sign-in and renewal attempts finish within 2 seconds in a standard environment with responsive stores); support a view of 100 retained sessions for one account  
**Constraints**: `ACTIVE` accounts only; finite injectable session lifetime; UUIDv7 session ID separate from a refresh credential with at least 128 bits of unpredictability; no readable stored refresh credential; no rotation, generations, grace period, replay detection, JWT logic in core, or message-based coordination  
**Scale/Scope**: One `AuthenticationSession` per successful sign-in, repeatable renewal without new sessions, scoped listing and revocation for at least 100 retained sessions; six public use cases and updates to three existing account-change services

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The repository's `.specify/memory/constitution.md` is an unratified template with no project-specific principles or enforceable gates. This plan therefore checks the approved spec and source brief as its active constraints.

| Gate | Before research | After design |
|------|-----------------|--------------|
| Scope stays in identityaccess-core; adapters and presentation remain separate | Pass | Pass |
| Public use cases are capability-owned; models and ports remain internal | Pass | Pass |
| Core ports are technology-neutral; JWT, hashing, cookies, and storage format stay outside core | Pass | Pass |
| Account-change effects are synchronous; no messaging or generic token package is added | Pass | Pass |
| No refresh rotation, generations, grace period, or replay-detection algorithm | Pass | Pass |

No gate violations require a complexity exception. The later adapter implementation must prove that account saves and session revocation meet FR-012 atomically; a core annotation alone is insufficient across unrelated stores.

## Project Structure

### Documentation (this feature)

```text
specs/002-authentication-sessions/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── commands.md
│   └── ports.md
└── tasks.md                  # created by $speckit-tasks
```

### Source Code (repository root)

```text
services/identity-access-next/
├── identityaccess-core/
│   └── src/
│       ├── main/java/com/minewaku/chatter/identityaccess/
│       │   ├── authentication/
│       │   │   ├── command/               # public command contracts
│       │   │   ├── query/                 # public session-view contract
│       │   │   └── internal/
│       │   │       ├── application/       # use-case services and revocation coordinator
│       │   │       ├── model/             # AuthenticationSession and lifecycle rules
│       │   │       └── port/              # generation, issuance, persistence, view ports
│       │   ├── user/internal/application/command/
│       │   │   ├── ChangePasswordService.java
│       │   │   ├── SuspendUserAccountService.java
│       │   │   └── SoftDeleteUserAccountService.java
│       │   └── shared/handler/UseCaseHandler.java
│       └── test/java/com/minewaku/chatter/identityaccess/
│           ├── authentication/internal/{application,model}/
│           └── user/internal/application/
├── identityaccess-infrastructure/      # future adapters; unchanged in this feature
├── identityaccess-presentation/        # future caller/cookie wiring; unchanged
└── identityaccess-start/               # future composition changes; unchanged
```

**Structure Decision**: Follow the brief's `authentication/` capability layout, using the existing `UseCaseHandler` contract style. Keep a narrow authentication-owned revocation coordinator as the synchronous entry point for the three user-account services. Put public `DeviceInfo` inside the authenticate use-case contract and keep stored session data in `internal/model`.

## Implementation Sequence

1. Define `AuthenticationSession`, device-label validation, UUIDv7 ID generator, finite lifetime policy, and port contracts. Test issuance, expiry boundary, last use, and one-time revocation with a fixed clock.
2. Add sign-in and renewal services using `UserAccountRepository`, `PasswordHasher.matches`, the session ports, and `AccessTokenIssuer`. Ensure absent/inaccessible/wrong-password cases return no credential and that a failed session write never reports sign-in success.
3. Add current-session sign-out, password-confirmed targeted and other-session sign-out, and the safe session query. Exercise ownership, idempotent counts, retained expired/revoked views, and 100-session listing.
4. Add direct revocation coordination to successful password change, suspension, and deletion. Preserve no-op/rejected behavior, update existing service fakes, and verify failure propagation. Document the atomicity obligation for future adapters.
5. Run the focused core test and formatting gates in [quickstart.md](quickstart.md). Runtime token/cookie/database validation waits for adapter and presentation implementation outside this feature.
