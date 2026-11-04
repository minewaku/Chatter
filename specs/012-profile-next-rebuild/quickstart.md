# Quickstart: Validating profile-next

Run from the repository root. Maven is always the service's own wrapper, never a bare `mvn`. Scenario numbers refer to the spec's user stories. Contracts: [profile-api.md](contracts/profile-api.md), [image-storage.md](contracts/image-storage.md), [account-deleted-event.md](contracts/account-deleted-event.md).

## 1. Automated checks (no platform needed beyond Docker)

```bash
cd services/profile-next && ./mvnw verify
```

Testcontainers starts PostgreSQL 17 and Kafka. Expected: all modules pass, including the architecture tests (FR-016, FR-017, SC-007), the legacy-defect tests (research R14, SC-008), the two-thread save race (FR-005), 1,000 repeated notices ending in one state (SC-004), and the listener tests for unknown event types, unparseable values and the dead-letter route (FR-012).

## 2. Platform prerequisites for the manual runs

1. PostgreSQL, Kafka, Vault and the relay from specs 003 and 013 are up (`infrastructure/components/*/README.md`). Topics `identityaccess.user.v1` and `identityaccess.user.v1.profile-next.dlt` exist (`create-topics.sh`); check with `docker exec kafka-chatter kafka-topics --bootstrap-server kafka-chatter:5003 --list | grep identityaccess.user.v1` and expect both lines before starting the service (spec FR-025).
2. Vault holds `secret/common/jwt/rs256` (shared with `identity-access`) and `secret/profilenext` with `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret` (see the Vault runbook added by this feature).
3. A Cloudinary upload notification URL reachable from Cloudinary (a tunnel to the local webhook path) for scenario D only.
4. `identity-access` is running, so there is a real account and access token to use. Below, `$TOKEN` is that access token and `$ID` the account id.

5. **Gateway routing (checked by T076, nothing changed).** `services/api-gateway` has discovery routing switched off (`spring.cloud.gateway.server.webflux.discovery.locator.enabled=false` in `application.properties`) and `bucket4j.yml` holds only rate-limit rules (`/api/v*/auth/**`, `api/v*/users/**`, `api/v*/guilds/**`, `api/index`; none for profiles). Routes are static, in `route/AuthenticationRoute.java`: `/api/v*/profiles/**` and `/api/v*/profiles/webhooks/**` go to `lb://PROFILE`, the legacy service, and the old Cloudinary webhooks `/api/v*/webhooks/cloudinary/{avatars,banners}/**` are whitelisted in `config/SecurityConfig.java`. So **a static route to `profile-next` is needed** before clients reach it through the gateway, and it has to cover `/api/v*/profiles/**` (including the public `/api/v*/profiles/uploads/notifications`, which the gateway's security whitelist does not list). Two more gateway facts: its CORS `allowedMethods` has no `PATCH`, which `PATCH /@me` needs for browser calls, and the service's own port is 5011 while the gateway's default is 5001 (which `identity-access` also defaults to). Direct calls to `localhost:5011`, as in the scenarios below, are unaffected.

Start the service: `cd services/profile-next && ./mvnw -pl profilenext-start spring-boot:run`.

## 3. Scenarios

**A. Read a profile that was never saved (Story 1)**

```bash
curl -s -H "Authorization: Bearer $TOKEN" localhost:5011/api/v1/profiles/@me
curl -s -H "Authorization: Bearer $TOKEN" localhost:5011/api/v1/profiles/$ID
curl -s -o /dev/null -w '%{http_code}\n' localhost:5011/api/v1/profiles/$ID          # 401
curl -s -o /dev/null -w '%{http_code}\n' -H "Authorization: Bearer $TOKEN" localhost:5011/api/v1/profiles/abc   # 400
```

Expected: 200 with `displayName` and `bio` empty and `avatarUrl`, `bannerUrl`, `createdAt`, `updatedAt` null; then 401; then 400, never 404.

**B. Edit (Story 2)**

```bash
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"displayName":"Mine"}' localhost:5011/api/v1/profiles/@me   # 204
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"bio":"Hello"}' localhost:5011/api/v1/profiles/@me          # 204
curl -s -H "Authorization: Bearer $TOKEN" localhost:5011/api/v1/profiles/@me
```

Expected: both values present after the two saves. A blank display name, a 33-character name or a 191-character bio returns 400 `validation_failed` naming the rule, and the stored values are unchanged.

**C. Concurrent first save (FR-005)**: covered by the automated two-thread test; for a manual check, send two `PATCH` calls at once for a fresh account and confirm one row in `profile_next.profile`.

**D. Images (Story 3)**: request `POST /api/v1/profiles/avatar/upload-signature`, post the returned `fields` and an image to `uploadUrl`, wait for the webhook, then read the profile. Expected: `avatarUrl` is the new image. Upload a second image: the first public id appears in `pending_image_removal`, is removed within about a minute, and the row disappears. Post the same webhook body twice with `curl`: the second changes nothing. Post it with a changed byte: 401.

**E. Account deletion (Story 4 and 5)**: with the profile from B and D in place, close the account in `identity-access` with `POST /api/v1/users/@me/close` and the body `{"currentPassword":"…"}`. Expected within a few seconds: the `profile` row is gone, both images are queued and then removed, and `GET /api/v1/profiles/$ID` returns defaults. Replay the notice by resetting the group offset: no error, same end state. Publish `{"nonsense":true}` to `identityaccess.user.v1` and confirm it lands on `identityaccess.user.v1.profile-next.dlt` while a later valid notice is still processed.

**F. Outage behaviour (Story 3, scenario 5; Story 4, scenario 3)**: stop the network path to Cloudinary (wrong API secret in Vault is enough), replace an avatar, and confirm the profile shows the new image while `pending_image_removal.attempts` climbs. Restore access and confirm the row clears.

**G. Sweep (Story 3, scenario 6)**: upload an image without letting the webhook arrive, wait past the grace period (set `profile-next.images.sweep.grace-period` to `PT1M` for the check), and confirm the sweep removes the file.

## 4. Done when

Section 1 passes and scenarios A, B, D, E and F behave as stated. Record the result in this file under a new heading, as spec 009 did for its walkthrough.

## 5. Test trace

Every requirement is covered by at least one automated test. Class names are under `services/profile-next`; method names are given where one test of a class carries the requirement. FR-019 to FR-025 belong to spec 013 and are covered by its tests in `services/identity-access` and the relay scripts.

| Requirement | Covered by |
|---|---|
| FR-001 defaults for an unsaved profile | `GetProfileServiceTest`, `JdbcProfileQueryRepositoryTest`, `ProfileControllerReadTest`, `ProfileFlowE2eTest.readThenEditNameThenBioShowsBothAndStartsFromDefaults` |
| FR-002 `@me` | `ProfileControllerReadTest.atMeReadsTheProfileOfTheTokenSubject`, `ProfileFlowE2eTest` |
| FR-003 partial save, first save creates | `SaveProfileServiceTest`, `JdbcProfileRepositoryTest` (first save, bio-only, empty bio clears), `ProfileControllerSaveTest`, `ProfileFlowE2eTest.oneUsersTokenCanNeverChangeAnotherUsersRow` |
| FR-004 name and bio rules | `DisplayNameTest`, `BioTest`, `ProfileControllerSaveTest.aRuleFailureIsAValidationFailure...`, `ProfileFlowE2eTest.aRefusedSaveChangesNothing` |
| FR-005 concurrent saves | `JdbcProfileRepositoryTest.twoConcurrentFirstSavesLeaveOneRowAndNeverMixTheirValues` (50 rounds) |
| FR-006 no revocation list | `ProfileFlowE2eTest.aTokenForAnAccountClosedElsewhereIsStillAccepted...`, `AccountDeletionE2eTest.aStillValidTokenForTheDeletedUserIsAcceptedForReads` |
| FR-007 upload permission | `RequestImageUploadServiceTest`, `CloudinaryImageStorageTest` (permit), `ImageUploadControllerTest`, `ImageFlowE2eTest.permissionThenConfirmationShowsTheNewImage` |
| FR-008 confirmation | `ConfirmImageUploadServiceTest`, `CloudinaryImageStorageTest` (verify), `ImageUploadControllerTest`, `ImageFlowE2eTest` (repeat, bad signature, file over limit) |
| FR-009 removal after commit, retries | `RemoveObsoleteImagesServiceTest`, `JdbcPendingImageRemovalRepositoryTest`, `ImageFlowE2eTest.whenTheProviderFails...` |
| FR-010 periodic cleanup | `SweepUnreferencedImagesServiceTest`, `ImageFlowE2eTest.aNeverConfirmedOldFileIsRemovedBySweep`; values come from `ImageProperties` |
| FR-011 delete on notice, idempotent | `DeleteProfileDataServiceTest`, `AccountDeletedListenerTest`, `AccountDeletionE2eTest` |
| FR-012 order, retry, dead letter, unknown types | `AccountDeletedListenerTest` (all cases), `AccountDeletedMessageTest` |
| FR-013 publishes nothing | `LegacyMachineryTest`; the only producer use is the dead-letter recoverer in `AccountDeletedConsumerConfiguration` |
| FR-014 timestamps, injectable clock | `SaveProfileServiceTest.usesTheInjectedClock`, `JdbcProfileRepositoryTest.updatedAtChangesOnEveryRealChange` |
| FR-015 error shape | `ApiExceptionHandlerTest`, `ProfileControllerSaveTest`, `ImageUploadControllerTest`, `StartSmokeTest` |
| FR-016 layout rules | `ArchitectureTest`, `PortAdapterMapTest`, `ArchitectureRuleNegativeTest`, `PresentationArchitectureTest` |
| FR-017 no legacy machinery | `LegacyMachineryTest`, `LoggingRuleTest` |
| FR-018 legacy defects | the R14 table below |
| SC-001 reads always succeed | `ProfileFlowE2eTest.readsForAnyExistingIdAlwaysSucceed` |
| SC-002 name set and read back | `ProfileFlowE2eTest.readThenEditNameThenBioShowsBothAndStartsFromDefaults` |
| SC-003 deleted data gone | `AccountDeletionE2eTest.aNoticeRemovesTheProfileAndTheWorkerRemovesBothImages`, `...whenStorageFailsTheRowIsStillRemovedAndTheRemovalsRetryLater` (seconds, not the 5-minute or 1-hour bounds, which need the real platform) |
| SC-004 1,000 repeats | `AccountDeletedListenerTest.aThousandDeliveries...`, `AccountDeletionE2eTest.aThousandRepeatsOfOneNoticeEndInTheSameStateAsOne` |
| SC-005 broker outage never fails a deletion | spec 013 (`identity-access` outbox and `soak-relay.sh`) |
| SC-006 one stored file per slot | `ImageFlowE2eTest.aSecondUploadQueuesTheFirstAndTheWorkerRemovesIt` |
| SC-007 no legacy classes, rules on every build | `LegacyMachineryTest`, `ArchitectureTest` |
| SC-008 legacy defects tested | the R14 table below |
| SC-009 same layout as identity-access | `PortAdapterMapTest` (one row per port, fixed packages) and the package layout itself |

Legacy defects (research R14) to their tests:

| # | Defect | Test |
|---|---|---|
| 5 | Shared asset deleted regardless of other users | `DeleteProfileDataServiceTest.removesOnlyThatUsersImages` |
| 6 | Domain coupled to Spring Data | `ArchitectureTest` |
| 10 | Hard-wired `Instant.now()`; clock-backwards as a business error | `SaveProfileServiceTest.usesTheInjectedClock` |
| 13 | Audit timestamp not updated on edits | `JdbcProfileRepositoryTest.updatedAtChangesOnEveryRealChange` |
| 14 | Replaced event emitted when nothing was replaced | `ConfirmImageUploadServiceTest.queuesNothingWhenSlotWasEmpty` |
| 17 | Wrong limits in the message, only `\n\r\t` rejected, UTF-16 count, validation on load | `DisplayNameTest`, `BioTest` |
| 11/12 | Revivable deleted state, mirrored account state | out of scope, no account state is kept |

## 6. Walkthrough result

**2026-11-04, automated checks only.** `cd services/profile-next && ./mvnw verify` passes in all four modules (core 57, infrastructure 57, presentation 54, start 22 tests; PostgreSQL 17 and Kafka 7.6 through Testcontainers).

The manual scenarios of section 3 were **not** run against the real platform in this session: Vault, the Kafka broker and relay, and `identity-access` were not running, and no Cloudinary credentials were available (so scenarios D and G could not be run either). What covers each scenario instead, through real HTTP, PostgreSQL and Kafka:

| Scenario | Automated equivalent |
|---|---|
| A read a never-saved profile | `ProfileFlowE2eTest.readThenEditNameThenBioShowsBothAndStartsFromDefaults`, `...readsForAnyExistingIdAlwaysSucceed`, `StartSmokeTest` (401) |
| B edit | `ProfileFlowE2eTest` (edit, refused save) |
| C concurrent first save | `JdbcProfileRepositoryTest.twoConcurrentFirstSavesLeaveOneRowAndNeverMixTheirValues` |
| D images | `ImageFlowE2eTest` with a stand-in for the provider; the real Cloudinary signing and webhook checks are covered by `CloudinaryImageStorageTest` against the SDK's own signer |
| E account deletion | `AccountDeletionE2eTest` with a notice written as `identity-access`'s relay writes it (key, `id` and `eventType` headers) and `AccountDeletedListenerTest` for the dead-letter route; the end-to-end link from `POST /api/v1/users/@me/close` through the relay is spec 013's scenario |
| F outage behaviour | `ImageFlowE2eTest.whenTheProviderFails...`, `AccountDeletionE2eTest.whenStorageFails...` |
| G sweep | `ImageFlowE2eTest.aNeverConfirmedOldFileIsRemovedBySweep` (grace period zero) |

Still to do by hand when the platform is up: scenarios A, B, D, E, F against `localhost:5011`, and a check with a real Cloudinary account that its notification signature algorithm and timestamp window match the defaults (research, *Verified during implementation*).
