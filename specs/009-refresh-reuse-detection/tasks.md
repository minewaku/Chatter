---

description: "Task list for refresh credential reuse detection"
---

# Tasks: Refresh Credential Reuse Detection

**Input**: Design documents from `specs/009-refresh-reuse-detection/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/core-and-infra-changes.md, contracts/reuse-event.md, quickstart.md

**Tests**: Included. The plan and quickstart commit to adapter, service, and end-to-end coverage, and the spec's success criteria (SC-001 to SC-007) are stated as test outcomes. Within each story, write the tests first and see them fail before the implementation tasks.

**Organization**: Grouped by user story. Paths are relative to the repository root. The service lives in `services/identity-access-next/`; build commands use the per-service wrapper `./mvnw` from that directory, never bare `mvn`. `<core>` = `services/identity-access-next/identityaccess-core/src`, `<infra>` = `services/identity-access-next/identityaccess-infrastructure/src`, `<start>` = `services/identity-access-next/identityaccess-start/src`, and `<pkg>` = `com/minewaku/chatter/identityaccess`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 = replaying a replaced credential ends the session; US2 = normal use is unaffected; US3 = the event is visible to operators

## Phase 1: Setup

**Purpose**: Establish a green baseline and settle the one open design cost before touching code.

- [X] T001 From `services/identity-access-next`, run `./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify` and confirm it passes before any change, so later failures are attributable. Docker must be running (Testcontainers).
- [X] T002 Confirm the memory window with the owner. Default, and what the spec requires (FR-003, FR-012): a replaced pointer lives `retention` after the replacement (30 days by default), about 290 KB per active session that renews every 15 minutes (see `specs/009-refresh-reuse-detection/research.md` decision 4). If the owner wants it shorter, amend spec FR-003, its Assumptions, and research decision 4 **before** T009; otherwise record "keep retention" in research decision 4 and continue.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The port method and its test double, which every story's tests and implementation rely on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. `identityaccess-infrastructure` does not compile between T003 and T010 because the Redis adapter does not implement the new method yet; that is expected.

- [X] T003 Add to `<core>/main/java/<pkg>/authentication/internal/port/repository/AuthenticationSessionRepository.java` the method `boolean revokeOnReuse(String presentedRefreshToken, Instant now);` with the Javadoc given in `contracts/core-and-infra-changes.md` section 1 (revokes the session of a replaced credential if still unrevoked and unexpired at `now`; blank returns false; adapter records the event and writes the access-credential revocation entry). Leave every other method untouched.
- [X] T004 Update the in-memory repository in `<core>/test/java/<pkg>/authentication/internal/application/AuthenticationTestFakes.java`: in `rotate`, record `presentedRefreshToken -> sessionId` in a new map `replacedByToken` when the rotation succeeds; implement `revokeOnReuse(token, now)` to look the token up in `replacedByToken`, and when the session exists and `isUsable(now)` call `session.revoke(now)` and return true, otherwise return false (blank returns false); add a counter `revokeOnReuseCalls` incremented on every call, matching the existing `rotateCalls` and `revokeAllCalls` style.

**Checkpoint**: `./mvnw -pl identityaccess-core -am test-compile` succeeds.

---

## Phase 3: User Story 1 - Replaying a replaced refresh credential ends the session (Priority: P1) 🎯 MVP

**Goal**: A replaced refresh credential presented for renewal is refused, revokes exactly its own session, makes that session's refresh and access credentials unusable at once, and gets the same refusal as an unknown credential (spec FR-001 to FR-005, FR-008, FR-010, FR-012).

**Independent Test**: Sign in, renew once, present the first credential again. Expect the refusal, the session revoked, the credential from the renewal rejected, an access credential from the session rejected, and a second session of the account still working.

### Tests for User Story 1 (write first; they must fail before T008 to T011)

- [X] T005 [P] [US1] Extend `<core>/test/java/<pkg>/authentication/internal/application/RefreshAccessTokenServiceTest.java`: (a) after one renewal, presenting the old credential throws `AuthenticationFailedException` and calls `revokeOnReuse` exactly once, and the session ends up revoked in the fake; (b) after three renewals, each of the three earlier credentials triggers the same result; (c) the refusal is the same exception type and carries nothing that differs from the unknown-credential case; (d) a second session of the same account stays usable; (e) a session that is already revoked or expired is not changed (the fake returns false) and the refusal is the same; (f) `revokeOnReuse` throwing propagates, and no access or refresh credential is issued.
- [X] T006 [P] [US1] Extend `<infra>/test/java/<pkg>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepositoryTest.java`: (a) after `rotate`, `session:replaced:<sha256(old)>` exists with the session id as value, `session:token:<sha256(old)>` does not, and its lifetime is `retention` from the replacement (compute as the existing `rotationRecomputesKeyLifetimes` test does) and never later than the session hash's lifetime; (b) after three rotations all three earlier digests have replaced pointers; (c) `revokeOnReuse(old)` returns true, sets `revokedAt`, writes `access-revoked:<userId>:<sessionId>` with the access-token lifetime as TTL, and leaves another session of the same user unchanged; (d) a second `revokeOnReuse(old)` returns false and does not move `revokedAt`; (e) `revokeOnReuse` returns false and changes nothing for the current credential, an unknown credential, a blank credential, an already-revoked session, an expired session, and a replaced pointer whose session hash is gone.
- [X] T007 [P] [US1] Update and extend `<start>/test/java/<pkg>/start/e2e/SessionLifecycleE2eTest.java`. First fix the existing `refreshRotatesTheCredentialAndRenewsTheSession`: its `chained` step renews with the credential from the first renewal *after* the old credential was replayed, which will now be 401 because the replay revokes the session; reorder so the chained renewal happens before the replay, or move the replay into its own test. Then add tests from `quickstart.md` section 2: (1) replay is refused with the same status and body as an unknown credential; (2) the credential issued by the replaying session's last renewal no longer renews; (3) an access credential from that session gets 401 `unauthenticated` on `GET /sessions` immediately; (4) a second session of the account still gets a non-401 on `GET /sessions`; (5) after several renewals, the oldest credential revokes the session.

### Implementation for User Story 1

- [X] T008 [US1] Edit `<infra>/main/resources/redis/session-rotate.lua` per `contracts/core-and-infra-changes.md` section 4: document `KEYS[4]` (replaced pointer key, `<prefix>session:replaced:<oldDigest>`) and `ARGV[7]` (replaced pointer expiry, epoch millis) in the header comment, and where the script does `DEL KEYS[2]` first run `SET KEYS[4] ARGV[1]` and `PEXPIREAT KEYS[4] tonumber(ARGV[7])`. Existing keys, arguments, checks, and `return 0` paths stay as they are; nothing is written on any failure path.
- [X] T009 [P] [US1] Create `<infra>/main/resources/redis/session-revoke-reused.lua` exactly as specified in `contracts/core-and-infra-changes.md` section 5: `KEYS[1]` replaced pointer key; `ARGV[1]` session hash key prefix, `ARGV[2]` now (epoch micros), `ARGV[3]` revocation entry key prefix, `ARGV[4]` access credential lifetime (millis); steps 1 to 7 in order (missing pointer, missing session, already revoked, `now >= expiresAt` each return nil; otherwise `HSET revokedAt`, `SET <revocation prefix><userId>:<sessionId> now PX lifetime`, return `"<userId>:<sessionId>"`). Mirror the comment style of `session-revoke-one.lua`.
- [X] T010 [US1] Edit `<infra>/main/java/<pkg>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepository.java` (after T008 and T009): add `private final String replacedKeyPrefix = keys.keyPrefix() + "session:replaced:"`; load `REVOKE_REUSED = RedisScripts.load("session-revoke-reused.lua", String.class)`; in `rotate` add the fourth key `replacedKeyPrefix + TokenDigest.sha256Hex(presentedRefreshToken)` and the seventh argument `Long.toString(RedisTime.toMillisFloor(RedisTime.toMicros(now)) + retentionMillis)`; implement `revokeOnReuse(token, now)`: blank returns false, otherwise execute `REVOKE_REUSED` with key `replacedKeyPrefix + digest` and arguments `sessionKeyPrefix`, `micros(now)`, `revocationKeyPrefix`, `accessTokenTtlMillis`, and return `result != null`. Do not log yet (T019).
- [X] T011 [US1] Edit `<core>/main/java/<pkg>/authentication/internal/application/command/RefreshAccessTokenService.java`: split the lookup so that when `sessions.findByPresentedToken(command.refreshToken())` is empty, call `sessions.revokeOnReuse(command.refreshToken(), now)` and then throw `AuthenticationFailedException`; keep the usable-session and accessible-owner filters, the issue-then-rotate order, and the `rotated == false` refusal exactly as they are, with no `revokeOnReuse` call on those paths. Update the class Javadoc to mention reuse. Follow the flow in `contracts/core-and-infra-changes.md` section 2.
- [X] T012 [US1] Run `./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify` and then `./mvnw -pl identityaccess-start -am test`; T005, T006, and T007 must now pass and nothing previously green may fail (use `test`, not `verify`, for `identityaccess-start`, which has a pre-existing `check` failure).

**Checkpoint**: User Story 1 works and is testable on its own. This is the MVP; the protection is complete here and US2 and US3 add guarantees and visibility.

---

## Phase 4: User Story 2 - Normal use is unaffected (Priority: P1)

**Goal**: In-order renewal, unknown credentials, and simultaneous renewals behave exactly as before and never revoke a session (spec FR-006, FR-007, FR-013, SC-004).

**Independent Test**: Renew 50 times in order and confirm no refusal and no revocation; fire simultaneous renewals with one credential and confirm one winner, the rest refused, and the session still working with its new credential.

### Tests for User Story 2

- [X] T013 [P] [US2] Extend `<core>/test/java/<pkg>/authentication/internal/application/RefreshAccessTokenServiceTest.java`: (a) a successful renewal makes zero `revokeOnReuse` calls; (b) blank, empty, and never-issued credentials throw `AuthenticationFailedException` without revoking any session in the fake; (c) with the fake's `failRotate` flag set (the concurrent-loser case), the call is refused, `revokeOnReuseCalls` stays 0, and the session is still usable and still renews with its current credential; (d) a session whose owner is no longer accessible is refused without a `revokeOnReuse` call.
- [X] T014 [P] [US2] Extend `<infra>/test/java/<pkg>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepositoryTest.java`: in `onlyOneOfManyConcurrentRotationsWithTheSameCredentialSucceeds` also assert the session has no `revokedAt` and no `access-revoked` entry, and that exactly one new pointer is current; add a test that each failed `rotate` (missing session, revoked session, expired session, wrong credential) leaves no `session:replaced:` key behind.
- [X] T015 [P] [US2] Add to `<start>/test/java/<pkg>/start/e2e/SessionLifecycleE2eTest.java`: (a) 50 consecutive renewals, always with the newest credential, all return 200 and the session is never revoked and keeps the same `sid`; (b) a random 43-character credential, a blank one, and one with another account's session prefix all return the standard refusal and do not change any session of the signed-in account (check with `GET /sessions` before and after).

- [X] T016 [US2] Run the same commands as T012. These tests are expected to pass against the T008 to T011 implementation; a failure means User Story 1 regressed normal use and must be fixed there, not by loosening the test.

**Checkpoint**: User Stories 1 and 2 both pass independently.

---

## Phase 5: User Story 3 - The event is visible to operators (Priority: P2)

**Goal**: Each reuse revocation produces exactly one operator-visible log line with no credential value, and ordinary sign-outs and no-change cases produce none (spec FR-011, SC-006).

**Independent Test**: Trigger a reuse revocation, read the captured log, and find one `refresh-reuse` line naming the session and account; trigger a normal sign-out and find none.

### Tests for User Story 3

- [X] T017 [P] [US3] In `<infra>/test/java/<pkg>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepositoryTest.java`, capture log output the way `MailEmailChangeNotifierTest` does (`OutputCaptureExtension`) and assert: a reuse revocation logs exactly one `WARN` line `Refresh reuse detected: session revoked reason=refresh-reuse sessionId=<id> userId=<id>`; a second presentation of the same old credential logs nothing; the no-change cases from T006(e) log nothing; `revokeBySessionId`, `revokeOtherSessions`, and `revokeAllByUserId` log no `refresh-reuse` line; the output contains neither the raw credential nor its SHA-256 digest.
- [X] T018 [P] [US3] Extend `<start>/test/java/<pkg>/start/e2e/SecretLeakageE2eTest.java` (already uses `OutputCaptureExtension`): add a reuse step (renew, then replay the old credential) and assert the captured logs contain `reason=refresh-reuse` once and none of the raw refresh credentials, their digests, or the access tokens; add to `SessionLifecycleE2eTest.java` a test that a second session of the same account listing `GET /sessions` shows the reuse-revoked session with a `revokedAt` value like any other revoked session.

### Implementation for User Story 3

- [X] T019 [US3] Edit `<infra>/main/java/<pkg>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepository.java` (after T010): add an SLF4J logger (same style as `ResilienceConfiguration`) and, in `revokeOnReuse`, when the script returns `"<userId>:<sessionId>"`, split it at the first `:` and log once at `WARN` the exact message in `contracts/reuse-event.md`, with only `sessionId` and `userId`. Log nothing when the script returns null, for blank input, or on exceptions (the exception propagates unchanged). Never log the credential, its digest, the email, or the device label.
- [X] T020 [US3] Run the same commands as T012; T017 and T018 must pass.

**Checkpoint**: All three stories work independently.

---

## Phase 6: Polish & cross-cutting concerns

**Purpose**: Keep the earlier specs accurate, check formatting, and run the full validation.

- [X] T021 [P] In `specs/002-authentication-sessions/spec.md`, append to FR-015 the text "(replay detection added by spec 009)". Change nothing else in that file.
- [X] T022 [P] In `specs/004-presentation-api/spec.md`, append "(superseded by spec 009)" to the replay-detection sentence in FR-029 and to the "spec 003 FR-006's 'no replay handling' still stands" clause in the closing note. Change nothing else.
- [X] T023 From `services/identity-access-next`, run Spotless over the modules this feature touched: `./mvnw -pl identityaccess-core,identityaccess-infrastructure,identityaccess-start spotless:apply`, then `./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify` and `./mvnw -pl identityaccess-start -am test`; all must pass with no unrelated formatting changes in the diff.
- [X] T024 Run `quickstart.md` section 3 (manual check against a running service) and section 4 (acceptance map), and tick each mapped spec item (SC-001 to SC-007, FR-013, sign-out unchanged) against a passing test or the observed log and Redis output. *Run 2026-10-22 against the jar from `services/identity-access` with Vault, PostgreSQL, Redis and Mailpit: renewing with the replaced credential answered `401 authentication_failed`, the same body as an unknown credential; the credential from the first renewal was then refused; both access credentials answered `401` at once; the log held one `Refresh reuse detected: session revoked reason=refresh-reuse` line with session and user ids and no credential; `session:replaced:<digest>` held a session id with a lifetime of about 30 days. `RefreshAccessTokenServiceTest` (17), `RedisAuthenticationSessionRepositoryTest` (28), `SessionLifecycleE2eTest` (12) and `SecretLeakageE2eTest` (3) pass, which covers the rest of the acceptance map.*

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none; T002 must be settled before T008.
- **Foundational (Phase 2)**: after Setup; blocks all stories. `identityaccess-infrastructure` compiles again only after T010.
- **User Story 1 (Phase 3)**: after Foundational. MVP.
- **User Story 2 (Phase 4)**: after Foundational for writing; its tests run meaningfully after US1 implementation (T011), because they assert behavior of the final flow.
- **User Story 3 (Phase 5)**: T017 and T018 can be written after Foundational, but T019 edits the same file as T010, so it follows US1.
- **Polish (Phase 6)**: after the stories you intend to ship.

### Within each story

- Tests first and failing, then implementation.
- Lua scripts before the adapter that calls them (T008, T009 before T010); adapter before service (T010 before T011) so the service never calls a missing implementation.
- T010 and T019 edit the same file: sequential.

### User story dependencies

- **US1** has no dependency on other stories.
- **US2** adds only tests and exercises US1 code.
- **US3** edits the adapter after US1 and adds log assertions.

### Parallel opportunities

- T005, T006, T007 (different files); T009 beside T008; T013, T014, T015; T017, T018; T021, T022.
- Different stories' test tasks can be written in parallel once Phase 2 is done.

## Parallel Example: User Story 1 tests

```text
Task: "Extend RefreshAccessTokenServiceTest with reuse cases (T005)"
Task: "Extend RedisAuthenticationSessionRepositoryTest with replaced-pointer and revokeOnReuse cases (T006)"
Task: "Update and extend SessionLifecycleE2eTest (T007)"
```

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 and the T002 decision.
2. Phase 2: port method and fake.
3. Phase 3: tests, scripts, adapter, service. Stop and validate: replay ends the session, access credentials die at once, other sessions untouched.
4. This alone closes the security gap; it can ship without US2 and US3, but without US3 there is no operator signal.

### Incremental delivery

1. US1: the protection.
2. US2: regression guards for normal and concurrent use (tests only).
3. US3: the operator log and its leakage checks.
4. Polish.

## Notes

- Existing end-to-end test `SessionLifecycleE2eTest.refreshRotatesTheCredentialAndRenewsTheSession` asserts a 200 renewal after a replay and will fail once US1 lands; T007 fixes it deliberately.
- The memory cost of the 30-day window is the known risk (T002); the behavior does not change if the window is later shortened, only FR-003's wording.
- Presentation and the HTTP contract are not touched, and no new dependency is added.
- Commit after each task or logical group.
