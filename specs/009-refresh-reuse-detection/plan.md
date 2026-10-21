# Implementation Plan: Refresh Credential Reuse Detection

**Branch**: `identity-access-next` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-refresh-reuse-detection/spec.md`

## Summary

When a refresh credential that a renewal has already replaced is presented for renewal again, end its session: revoke it (which already kills the current refresh credential and, through the existing revocation entry, the session's access credentials) and record an operator-visible warning. Today `session-rotate.lua` deletes the old credential's pointer, so a replay is indistinguishable from an unknown credential. The change keeps that pointer, under a separate `session:replaced:` key that points to the same session id and expires after the retention period, and adds one new Lua script that revokes a session found through such a pointer. The happy path is untouched; the check runs only when a presented credential is not current. Simultaneous renewals with the same credential keep today's behavior (loser refused, session kept) because the loser has already found the credential current before it lost the race; only a request that begins after the winner has completed reaches the new check. Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (Spring Framework 7, Spring Security 7)

**Primary Dependencies**: None added. Uses the existing `StringRedisTemplate`, `DefaultRedisScript` loader (`RedisScripts`), `TokenDigest`, and SLF4J (already used in infrastructure).

**Storage**: Redis 7, existing key prefix `identity-access:`. One new key namespace `session:replaced:<digest>` and one new Lua script. PostgreSQL unchanged.

**Testing**: JUnit 5 + AssertJ; existing Redis adapter tests (`RedisAuthenticationSessionRepositoryTest`) extended; `RefreshAccessTokenServiceTest` and `AuthenticationTestFakes` in core extended; end-to-end suite in `identityaccess-start` (Testcontainers `postgres:17`, `redis:7`) extended; Spotless `check`

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend; touches `identityaccess-core` (small) and `identityaccess-infrastructure` (small) plus tests in both and in `identityaccess-start`. Presentation and the HTTP contract do not change.

**Performance Goals**: No added Redis round trip on a successful renewal. A refused, non-current credential costs one extra script call (the new revoke-on-reuse script), which also runs only for credentials the service does not recognise as current.

**Constraints**: No refresh credential in readable form anywhere new (keys and values hold only the digest and session id); the log record carries session id and account id only; the refusal body and status are identical to those for an unknown credential; the store being unavailable fails the request as renewal does today.

**Scale/Scope**: 1 new Lua script, 1 edited Lua script, 1 new port method, 2 production classes edited in core and 1 in infrastructure, about 5 test classes edited. Memory: one small key per renewal for the retention period, see [research.md](research.md) decision 4 for the estimate.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, `infrastructure-structure-design.txt`, and the approved spec (same basis as specs 003, 004 and 008).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits are limited to `identity-access-next/{core,infrastructure,start}` and `specs/` |
| Core owns contracts; infrastructure owns technology | Pass | Pass: the only core change is one repository-port method and its use in one service; no Redis, Lua, or logging type enters core |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: the log line has session and account ids only; the store holds digests only; the leakage e2e test is extended to cover it |
| Presentation depends only on core's public contracts | Pass | Pass: presentation is not touched |
| Earlier specs stay accurate | Needs work | Pass: specs 002 and 004 exclusions of replay detection get a "superseded by 009" note as a task |
| Git/patch paths resolved against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories and uses the per-service wrapper |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/009-refresh-reuse-detection/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── core-and-infra-changes.md   # port method, service flow, script contracts
│   └── reuse-event.md              # the operator-visible record
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (repository root: `services/identity-access-next/`)

```text
identityaccess-core/
├── src/main/java/.../authentication/internal/port/repository/AuthenticationSessionRepository.java   # edit: add revokeOnReuse
├── src/main/java/.../authentication/internal/application/command/RefreshAccessTokenService.java      # edit: call revokeOnReuse before refusing a non-current credential
└── src/test/java/.../authentication/internal/application/
    ├── AuthenticationTestFakes.java               # edit: fake remembers replaced credentials and implements revokeOnReuse
    └── RefreshAccessTokenServiceTest.java         # edit: reuse, concurrent-loser, unknown-credential, store-failure cases

identityaccess-infrastructure/
├── src/main/resources/redis/
│   ├── session-rotate.lua                         # edit: keep the old pointer under session:replaced: instead of deleting it
│   └── session-revoke-reused.lua                  # new: revoke the session a replaced pointer names
├── src/main/java/.../adapter/outbound/session/write/redis/
│   └── RedisAuthenticationSessionRepository.java  # edit: pass the replaced key and expiry to rotate; implement revokeOnReuse; log the event
└── src/test/java/.../adapter/outbound/session/write/redis/
    └── RedisAuthenticationSessionRepositoryTest.java   # edit: replaced pointer lifetime, reuse revoke, expired/revoked/unknown, concurrency

identityaccess-start/
└── src/test/java/.../start/e2e/
    ├── SessionLifecycleE2eTest.java               # edit: replay revokes; sign-out with old credential does not; access credential rejected after reuse
    └── SecretLeakageE2eTest.java                  # edit: the reuse log line holds no credential

specs/
├── 002-authentication-sessions/spec.md            # edit: note on FR-015 (superseded by 009)
└── 004-presentation-api/spec.md                   # edit: note on FR-029 and the closing note (superseded by 009)
```

**Structure Decision**: Reuse detection is a session-store concern (it depends on atomic reads of a pointer and a session hash), so the logic lives in the Redis adapter's Lua scripts, exactly like rotation and revocation. Core only decides *when* to ask (a non-current credential presented for renewal) and stays free of logging and store types. Logging the event is an adapter obligation, as the port already says for access-credential revocation. No new module, class, or bean is needed.

## Complexity Tracking

> Fill ONLY if Constitution Check has violations that must be justified

No violations.
