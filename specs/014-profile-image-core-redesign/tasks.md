---

description: "Task list for the profile image redesign, core layer"
---

# Tasks: Profile Image Redesign (core layer)

**Input**: Design documents from `/specs/014-profile-image-core-redesign/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ports.md, contracts/use-cases.md, quickstart.md

**Tests**: Required (spec FR-030 and FR-031). Test tasks come before the implementation they cover; for new behaviour, write the test first and watch it fail.

**Organization**: By user story. The model and port swap is one compile unit, so User Story 1 carries it and leaves the module green; Stories 2 to 8 then add behaviour and tests on top of a green build.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US8, matching `spec.md`

## Path Conventions

Everything is in one module, `services/profile-next/profilenext-core`. Two aliases are used below; expand them literally:

- `MAIN` = `services/profile-next/profilenext-core/src/main/java/com/minewaku/chatter/profilenext/profile`
- `TEST` = `services/profile-next/profilenext-core/src/test/java/com/minewaku/chatter/profilenext/profile/internal`

Build and test only the core, from `services/profile-next`, with the service wrapper (never bare `mvn`, never the other modules):

```bash
cd services/profile-next && ./mvnw -q -pl profilenext-core test
```

Single class: add `-Dtest=ClassName`. Test results: `profilenext-core/target/surefire-reports`.

**Standing rules for every task**: do not touch any module other than `profilenext-core`; do not revert the existing uncommitted refactor (nested port records, `DisplayName`/`Bio` in `Profile`); core imports no infrastructure, Spring Data/JDBC/Kafka/web/security or provider types; do not commit unless asked.

---

## Phase 1: Setup

**Purpose**: establish the baseline before changing anything.

- [X] T001 Run `cd services/profile-next && ./mvnw -q -pl profilenext-core test` and confirm it passes with 58 tests total (sum the `Tests run` lines in `profilenext-core/target/surefire-reports/*.txt`). Stop and report if it does not.
- [X] T002 List every reference to the old image model so later removal can be verified: the old-model search, run as two commands from the repository root so deliberate negative assertions in a few tests do not count: `OLD='StoredImage\b|publicId|\.folder\(\)|avatarUrl|bannerUrl|USER_MISMATCH|replaceImage|Replacement'`, then `grep -rnE "$OLD" services/profile-next/profilenext-core/src/main` and `grep -rnE "$OLD" services/profile-next/profilenext-core/src/test --exclude=ConfirmedUploadTest.java --exclude=ProfileTest.java --exclude=ImageSlotTest.java`. Expect many matches now; both commands must return none at T049 and again at T072. (Those three excluded tests assert by reflection that the old members are gone and so name them on purpose.)

---

## Phase 2: Foundational (additive; the build stays green)

**Purpose**: new value types, policy, queue port and its fake. All are new files, so nothing existing breaks. Every user story uses them.

**⚠️ CRITICAL**: no user story starts before T016 passes.

- [X] T003 [P] Create `MAIN/internal/model/ImageId.java`: `record ImageId(UUID value) implements Comparable<ImageId>`. Rules (data-model): "not null"; `static ImageId of(String)` "rejects text that is not a UUID" with `IllegalArgumentException`; `compareTo` = `value.compareTo(other.value)`; add `isNewerThan(ImageId other)` = `compareTo(other) > 0` (a repeat is not newer).
- [X] T004 [P] Create `TEST/model/ImageIdTest.java`: later id orders after earlier for two ids from `com.fasterxml.uuid`-style UUIDv7 values built by hand (timestamp in the high 48 bits); pin the signed-compare caveat (research D1) with one id near timestamp 0 and one near 2026-era milliseconds; equal ids are not newer; `of("not-a-uuid")` and `of(null)` throw; `of(uuid.toString())` round-trips.
- [X] T005 [P] Create `MAIN/internal/model/ImageDimensions.java`: `record ImageDimensions(int width, int height)`; compact constructor rejects non-positive values with `IllegalArgumentException` ("both greater than zero").
- [X] T006 [P] Create `TEST/model/ImageDimensionsTest.java`: 1x1 accepted; width 0, height 0, negative width, negative height each rejected.
- [X] T007 [P] Create `MAIN/internal/model/OwnedImage.java`: `record OwnedImage(UserId userId, ImageSlot slot, ImageId imageId)`; all three parts non-null (`Objects.requireNonNull`).
- [X] T008 [P] Create `TEST/model/OwnedImageTest.java`: null user, null slot, null id each rejected; equal parts give equal records.
- [X] T009 [P] Create `MAIN/internal/model/ImageRef.java`: read-model record `ImageRef(ImageId id, boolean promoted)`; id non-null.
- [X] T010 [P] Create test fixture `TEST/application/fakes/ImageIds.java`: `static ImageId at(int n)` returning a deterministic, strictly increasing UUIDv7-shaped id: `new UUID(((long) n << 16) | 0x7000L, 0x8000_0000_0000_0000L | n)`. `at(2)` must be newer than `at(1)`.
- [X] T011 [P] Create `MAIN/internal/application/command/PromotionPolicy.java`: `record PromotionPolicy(Duration horizon, List<Duration> delays, int pollBatchSize, Duration lease)`; `delayBeforeRetry(int retryNumber)` identical to `RemovalPolicy.delayBeforeRetry` (last delay repeats); `boolean expired(Instant createdAt, Instant now)` true when `now` is not before `createdAt + horizon`.
- [X] T012 [P] Create `TEST/application/PromotionPolicyTest.java`: delays grow then repeat the last; `expired` is false one second before the horizon, true exactly at it and after.
- [X] T013 [P] Create `MAIN/internal/port/repository/PendingImagePromotionRepository.java` exactly per `contracts/ports.md`: `enqueue(OwnedImage, Instant now)`, `List<PendingPromotion> takeDue(Instant now, int limit, Duration lease)`, `markPromoted(ImageId)`, `markFailed(ImageId, int attempts, Instant nextAttemptAt, Instant reportedAt)`, `remove(ImageId)`, `deleteByUserId(UserId)`, plus nested `record PendingPromotion(OwnedImage image, int attempts, Instant createdAt)`.
- [X] T014 Create `TEST/application/fakes/FakePromotionQueue.java` implementing T013 in memory, following `FakeRemovalQueue`'s rules: duplicate enqueue ignored without resetting attempts; `takeDue` returns unreported rows with `nextAttemptAt <= now`, oldest first, up to `limit`, and pushes each returned row's `nextAttemptAt` to `now + lease`; reported rows are never returned; `remove` is a no-op when absent. Expose a public `rows` map keyed by `ImageId` with `attempts`, `nextAttemptAt`, `createdAt`, `reportedAt`, and `OwnedImage`.
- [X] T015 Create `TEST/application/fakes/FakePromotionQueueTest.java` (research D12): duplicate ignored, lease hides a row from a second `takeDue`, reported row not returned, `remove` of a missing id is a no-op, `deleteByUserId` removes only that user's rows, `takeDue` honours `limit`.
- [X] T016 Run the full core suite; it must be green with 58 plus the new tests.

**Checkpoint**: value types, promotion policy, promotion port and fake exist and are tested; nothing old has changed.

---

## Phase 3: User Story 1 - A profile stores only image identities (Priority: P1) 🎯 MVP

**Goal**: `Profile` holds at most one `ImageId` per slot and nothing else; the old path-and-address model is gone from the core; ports speak `OwnedImage`; the module builds and is green again.

**Independent Test**: `ImageIdTest`, `ImageDimensionsTest`, `ImageSlotTest`, `ProfileTest`, `ConfirmedUploadTest`, `ImagePolicyTest` pass; both T002 searches return nothing; the whole core suite is green.

**Compile-unit warning**: T022 to T042 form one atomic change. The module does not compile between the first of them and T049; do the edits in this order and run the build only at T049. `ConfirmImageUploadService` is deleted here and rebuilt from scratch in US3 (T053 to T055); until then the confirm use case has no implementation in core, which is intentional.

### Tests for User Story 1

- [X] T017 [P] [US1] Update `TEST/model/ImageSlotTest.java`: drop assertions on `folder()`; assert the enum has exactly `AVATAR` and `BANNER`, and that `ImageSlot` declares no `folder` method (reflection on `getDeclaredMethods`).
- [X] T018 [P] [US1] Delete `TEST/model/StoredImageTest.java` (the type it covers is removed).
- [X] T019 [P] [US1] Create `TEST/model/ProfileTest.java`: `Profile.defaultsFor(UserId.of(7))` has null `displayName`, empty `Bio`, null `avatar` and `banner`, null timestamps; `imageIn(AVATAR)` and `imageIn(BANNER)` return the matching `ImageId` or null; a profile has no component of type `String` named like path, url or public id (reflection over `Profile.class.getRecordComponents()`: allowed types are `UserId`, `DisplayName`, `Bio`, `ImageId`, `Instant`).
- [X] T020 [P] [US1] Rewrite `TEST/port/ConfirmedUploadTest.java` for the new record: `image()` returns `new OwnedImage(userId, slot, imageId)`; null `ImageDimensions` rejected; carries `format`, `bytes`; no `publicId`, `url` or `publicIdMatchesOwner` member (reflection).
- [X] T021 [P] [US1] Create `TEST/application/ImagePolicyTest.java` for `permits(ConfirmedUpload)`: a file exactly at the byte and dimension limits for each slot passes; one byte, or one pixel in width, or one pixel in height over fails; format comparison is case-insensitive (`PNG` allowed when `png` is configured); a disallowed format (`gif`) fails; avatar and banner limits are independent.

### Implementation for User Story 1

- [X] T022 [US1] Edit `MAIN/internal/model/ImageSlot.java`: remove the `folder` field, constructor argument and `folder()`; the enum is just `AVATAR, BANNER`; update the Javadoc (folder names live in the adapter, FR-005).
- [X] T023 [US1] Edit `MAIN/internal/model/Profile.java`: `avatar` and `banner` become nullable `ImageId` (type was `StoredImage`); keep `userId`, `displayName` (nullable `DisplayName`), `bio` (`Bio`), `createdAt`, `updatedAt` (null while no row exists); keep `defaultsFor(UserId)`; add `ImageId imageIn(ImageSlot slot)`. Update the Javadoc.
- [X] T024 [US1] Delete `MAIN/internal/model/StoredImage.java`.
- [X] T025 [US1] Rewrite `MAIN/internal/port/ImageStorage.java` per `contracts/ports.md` and `data-model.md`: `UploadPermission permit(OwnedImage image, Instant now)`; `Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now)`; `void promote(OwnedImage image)`; `void remove(OwnedImage image)`; `StoredImagePage list(String cursor)` (permanent images only). Nested records: `UploadPermission(uploadUrl, method, fields, expiresAt)` (unchanged), `ConfirmedUpload(UserId userId, ImageSlot slot, ImageId imageId, String format, long bytes, ImageDimensions dimensions)` with `OwnedImage image()` and no `publicIdMatchesOwner`, `StoredImageInfo(OwnedImage image, Instant createdAt)`, `StoredImagePage(List<StoredImageInfo> items, String nextCursor)`. Javadoc each operation with the contract text (promote treats an existing permanent copy as success; remove treats missing as success).
- [X] T026 [P] [US1] Rewrite `MAIN/internal/port/repository/ProfileRepository.java` per `contracts/ports.md`: keep `boolean applyEdit(UserId, DisplayName, Bio, Instant)`; add `Optional<Profile> findForUpdate(UserId)` (exclusive lock until the transaction ends; empty when no row), `void setImage(UserId, ImageSlot, ImageId, Instant now)`, `boolean clearImageIf(UserId, ImageSlot, ImageId, Instant now)`, keep `Optional<DeletedProfile> deleteByUserId(UserId)` with nested `record DeletedProfile(ImageId avatar, ImageId banner)` (nullable parts); remove `replaceImage` and `Replacement`.
- [X] T027 [P] [US1] Rewrite `MAIN/internal/port/query/ProfileQueryRepository.java`: `Optional<ProfileView> findByUserId(UserId)`; `Set<ImageId> findReferenced(Collection<OwnedImage> images)`; nested `record ProfileView(UserId userId, DisplayName displayName, Bio bio, ImageRef avatar, ImageRef banner, Instant createdAt, Instant updatedAt)` with static `defaultsFor(UserId)` (null name, `Bio.of("")`, null images, null timestamps). Remove `findReferencedPublicIds`.
- [X] T028 [P] [US1] Rewrite `MAIN/internal/port/repository/PendingImageRemovalRepository.java`: `enqueue(OwnedImage, Instant)`, `List<PendingRemoval> takeDue(Instant, int, Duration)`, `markRemoved(ImageId)`, `markFailed(ImageId, int attempts, Instant nextAttemptAt, Instant reportedAt)`, `Set<ImageId> findActive(Collection<ImageId>)`, `clear(ImageId)`; nested `record PendingRemoval(OwnedImage image, int attempts)`.
- [X] T029 [US1] Edit `MAIN/internal/application/command/ImagePolicy.java`: record becomes `(long maxBytesAvatar, long maxBytesBanner, int maxDimensionAvatar, int maxDimensionBanner, Set<String> allowedFormats, Duration permissionLifetime, Duration webhookTolerance)`; keep `maxBytes(slot)` and `allows(format)`; add `maxDimension(slot)` and `boolean permits(ConfirmedUpload upload)` = "format allowed and bytes ≤ limit and width ≤ limit and height ≤ limit" for the upload's slot (research D9).
- [X] T030 [P] [US1] Edit `MAIN/api/query/GetProfileUseCase.java`: in `Result`, replace `String avatarUrl, String bannerUrl` with `ImageResult avatar, ImageResult banner` (each null when the slot is empty); add nested `record ImageResult(String imageId, boolean promoted)`; keep the other components.
- [X] T031 [P] [US1] Edit `MAIN/api/exception/UploadConfirmationRejectedException.java`: remove `Reason.USER_MISMATCH`, leaving `INVALID_SIGNATURE` (research D11).
- [X] T032 [P] [US1] Edit `MAIN/api/command/ConfirmImageUploadUseCase.java`: change the `FILE_NOT_ALLOWED` Javadoc to say nothing is written and the file is left to expire at the provider (it is no longer queued for removal); `IGNORED` Javadoc also covers "no profile for the user".
- [X] T033 [US1] Rewrite `TEST/application/fakes/FakeImageStorage.java` for the new port: record `permits` as `(OwnedImage image, Instant now)`; `permit` returns `UploadPermission("https://upload.example/image", "POST", Map.of("image_id", id string, "user_id", ..., "slot", ...), now + 900 s)` with no folder or path strings; `verifyResult` as before; `promote` records into `promoteCalls` (list of `OwnedImage`) and throws `ImageStorageUnavailableException` when the image is in a `Set<ImageId> failingPromotions` (public); `remove` records into `removeCalls`, and throws `ImageStorageUnavailableException` when `failRemoval`, else adds to `removed`; `list` as before over `pages` with the rate-limit trigger kept (the fake lists only what a test gives it; Javadoc that the real port returns permanent images only). Keep the `page(...)` helper.
- [X] T034 [US1] Rewrite `TEST/application/fakes/FakeProfileStore.java` implementing `ProfileRepository` and `ProfileQueryRepository` with a public `rows` map keyed by user id value, a constructor taking `FakePromotionQueue` and a no-arg constructor that creates its own. `applyEdit` as today (create on first save). `findForUpdate` returns the row and appends `"findForUpdate:<user>"` to a public `calls` list. `setImage` and `clearImageIf` (equal-only) update the row and append to `calls`. `deleteByUserId` returns `DeletedProfile(avatar, banner)`. `findByUserId` returns `ProfileView` with `ImageRef(id, promoted = promotions has no row for id)`. `findReferenced` returns the ids held by the stated owner in the stated slot.
- [X] T035 [US1] Rewrite `TEST/application/fakes/FakeRemovalQueue.java` for the new port: rows keyed by `ImageId`, `Row` holds `OwnedImage`, `attempts`, `nextAttemptAt`, `reportedAt`; same lease, duplicate and reported rules as before; `findActive` returns ids of unreported rows.
- [X] T036 [US1] Edit `MAIN/internal/application/command/RequestImageUploadService.java`: `ImageId id = ImageId.of(ids.nextId())`; `storage.permit(new OwnedImage(UserId.of(command.userId()), ImageSlot.valueOf(command.slot()), id), clock.instant())`; same `Result`. No repository dependency.
- [X] T037 [US1] Edit `MAIN/internal/application/command/RemoveObsoleteImagesService.java`: call `storage.remove(row.image())`, `removals.markRemoved(row.image().imageId())`, `markFailed(row.image().imageId(), ...)`; log lines use `imageId=` and `userId=` instead of `publicId=`. Behaviour (retries, backoff, give-up) unchanged.
- [X] T038 [US1] Edit `MAIN/internal/application/command/SweepUnreferencedImagesService.java`: filter `StoredImageInfo` by `createdAt` before the cutoff; `referenced = profiles.findReferenced(oldImages)`; `active = removals.findActive(ids of oldImages)`; for each remaining image `storage.remove(image)` then `removals.clear(image.imageId())`; rate-limit and unavailable handling unchanged; log `imageId=`.
- [X] T039 [US1] Edit `MAIN/internal/application/command/DeleteProfileDataService.java` (final form, so its constructor never changes again): constructor `(ProfileRepository profiles, PendingImageRemovalRepository removals, PendingImagePromotionRepository promotions, Clock clock)`; in `handle`, queue each non-null captured `ImageId` as `removals.enqueue(new OwnedImage(userId, slot, id), clock.instant())` for `AVATAR` and `BANNER`, then call `promotions.deleteByUserId(userId)` (also when there was no profile; it is idempotent); keep `@Transactional` and the info log.
- [X] T040 [US1] Edit `MAIN/internal/application/query/GetProfileService.java`: read `ProfileView` (default `ProfileView.defaultsFor(userId)`), map each non-null `ImageRef` to `GetProfileUseCase.ImageResult(ref.id().value().toString(), ref.promoted())`, null stays null.
- [X] T041 [US1] Check `MAIN/internal/application/command/SaveProfileService.java` still compiles against the new `ProfileRepository`; change only what the compiler demands.
- [X] T042 [US1] Delete `MAIN/internal/application/command/ConfirmImageUploadService.java` and `TEST/application/ConfirmImageUploadServiceTest.java` (rebuilt from scratch in US3).
- [X] T043 [P] [US1] Update `TEST/application/RequestImageUploadServiceTest.java` to compile and pass with the new fake and `ImageId`: the id supplier lambda returns `ImageIds.at(1).value().toString()`; assert `permit.image()` equals `new OwnedImage(UserId.of(7), AVATAR, ImageIds.at(1))`. Drop the `public_id` path assertions.
- [X] T044 [P] [US1] Update `TEST/application/RemoveObsoleteImagesServiceTest.java` to the new fakes and `OwnedImage`; all existing cases keep their meaning.
- [X] T045 [P] [US1] Update `TEST/application/SweepUnreferencedImagesServiceTest.java` to the new fakes, `StoredImageInfo(OwnedImage, Instant)` and `FakeImageStorage.page(...)`; all existing cases keep their meaning.
- [X] T046 [P] [US1] Update `TEST/application/DeleteProfileDataServiceTest.java` to the new fakes and the new constructor (pass a `FakePromotionQueue`, shared with `FakeProfileStore`): profile rows are built with `ImageId`s; removals are asserted as `OwnedImage` records with the user id and slot.
- [X] T047 [P] [US1] Update `TEST/application/GetProfileServiceTest.java`: expect `result.avatar().imageId()` string and `promoted` (true when the fake promotion queue has no row); defaults case expects null avatar and banner.
- [X] T048 [P] [US1] Update `TEST/application/SaveProfileServiceTest.java` only as far as compilation requires.
- [X] T049 [US1] Run the full core suite (must be green) and both T002 searches (must now return no matches). Fix anything left over. This is the first build since T022.

**Checkpoint**: US1 complete. `Profile` has no path, address or metadata; the module is green; confirm has no implementation yet (US3).

---

## Phase 4: User Story 2 - Request permission to upload to one's own slot (Priority: P1)

**Goal**: a fresh `ImageId` per request; a permission for the caller's own user and slot; no records written.

**Independent Test**: `RequestImageUploadServiceTest` passes with a fake storage only.

- [X] T050 [US2] Extend `TEST/application/RequestImageUploadServiceTest.java` with: (a) two consecutive requests for the same user and slot use different ids and the second is newer (`isNewerThan`) — drive the supplier from a counter over `ImageIds.at(n)`; (b) the service is constructed with only `FakeImageStorage`, the id supplier and the clock, so no repository can be touched (assert the constructor signature by reflection: no parameter of a type in `..port.repository..` or `..port.query..`); (c) when the fake storage throws `ImageStorageUnavailableException` or `ImageStorageRateLimitedException` from `permit`, the exception propagates unchanged (add a `failPermit` switch to `FakeImageStorage` for this); (d) a non-UUID id from the supplier fails with `IllegalArgumentException` before any storage call; (e) the service passes exactly `command.userId()` and the requested slot to `permit` and takes no user id from anywhere else (assert on `storage.permits`; with commands for users 7 and 8 the permits are for 7 and 8). That the command's `userId` comes from the authenticated principal is the presentation spec's job, not asserted here.
- [X] T051 [US2] Make T050 pass: adjust `MAIN/internal/application/command/RequestImageUploadService.java` only if a case fails (expected: no change) and add the `failPermit` switch to `TEST/application/fakes/FakeImageStorage.java`.

**Checkpoint**: Stories 1 and 2 pass independently.

---

## Phase 5: User Story 3 - A verified upload updates the profile once, only when newer (Priority: P1)

**Goal**: confirm behaves exactly as the decision table in `data-model.md`.

**Independent Test**: `ConfirmImageUploadServiceTest` with `FakeImageStorage`, `FakeProfileStore`, `FakeRemovalQueue`, `FakePromotionQueue`.

### Tests for User Story 3 (write first, expect them to fail)

- [X] T052 [US3] Create `TEST/application/ConfirmImageUploadServiceTest.java` (setup: `NOW` fixed clock; `ImagePolicy` with avatar 5 MiB / 1024 px, banner 10 MiB / 2048 px, formats `jpg,png,webp`; users 7 and 8; ids from `ImageIds.at`). One test per case, asserting outcome plus the exact contents of `profiles.rows`, `removals.rows`, `promotions.rows`:
  1. verification empty → throws `UploadConfirmationRejectedException` with `Reason.INVALID_SIGNATURE`; nothing written; `profiles.calls` empty.
  2. format `gif` → `FILE_NOT_ALLOWED`; nothing written; `profiles.calls` empty (policy runs before the lock).
  3. bytes over the slot limit, width over the limit, height over the limit → `FILE_NOT_ALLOWED` each; exactly at the limit → not rejected.
  4. no profile for the user → `IGNORED`; no row created; `profiles.calls` equals `["findForUpdate:7"]`.
  5. profile exists, avatar empty → `APPLIED`; avatar is the new id; one promotion row `OwnedImage(7, AVATAR, id)`; no removal row.
  6. avatar holds A (promotion row for A still present), confirm newer B → `APPLIED`; avatar is B; removal row for `OwnedImage(7, AVATAR, A)`; A's promotion row gone; promotion row for B.
  7. avatar holds B, confirm older A → `IGNORED`; nothing changed.
  8. avatar holds B, confirm B again → `IGNORED`; nothing changed; still exactly one promotion row.
  9. replay of the same notification three times after `APPLIED` → one promotion row, no removal rows.
  10. avatar and banner of one user confirmed in either order → each slot set independently, one promotion row each.
  11. call order: for every case that reaches the repository, `profiles.calls.get(0)` is `findForUpdate:<user>` and any `setImage` comes after it.
  12. a confirmation after `DeleteProfileDataService` ran for the user (use the real service with the same fakes) → `IGNORED` and no row is recreated.
- [X] T053 [US3] Add call recording to `TEST/application/fakes/FakeProfileStore.java` if T034 did not already provide it: `calls` gets `"findForUpdate:<id>"`, `"setImage:<id>:<slot>"`, `"clearImageIf:<id>:<slot>"` in order.

### Implementation for User Story 3

- [X] T054 [US3] Create `MAIN/internal/application/command/ConfirmImageUploadService.java` implementing `ConfirmImageUploadUseCase`, class annotated `@Service` and `@Transactional`; constructor `(ImageStorage storage, ProfileRepository profiles, PendingImageRemovalRepository removals, PendingImagePromotionRepository promotions, ImagePolicy policy, Clock clock)`. Flow, in this order and no other: (1) `storage.verify(...)`, empty → throw `new UploadConfirmationRejectedException(Reason.INVALID_SIGNATURE)`; (2) `!policy.permits(upload)` → return `FILE_NOT_ALLOWED` with no repository call; (3) `profiles.findForUpdate(upload.userId())`, empty → `IGNORED`; (4) `current = profile.imageIn(upload.slot())`; if `current != null && !upload.imageId().isNewerThan(current)` → `IGNORED`; (5) `profiles.setImage(...)`; if `current != null`: `removals.enqueue(new OwnedImage(user, slot, current), now)` and `promotions.remove(current)`; `promotions.enqueue(upload.image(), now)`; return `APPLIED`. Javadoc states: rejected, stale and abandoned files are never queued for removal (spec FR-019).
- [X] T055 [US3] Run `-Dtest=ConfirmImageUploadServiceTest` until green, then the full core suite.

**Checkpoint**: Stories 1 to 3 pass. An upload can be requested and applied to a profile.

---

## Phase 6: User Story 4 - Applied images are promoted with retries and a clear give-up (Priority: P2)

**Goal**: the promotion worker use case.

**Independent Test**: `PromotePendingImagesServiceTest` with `FakeImageStorage`, `FakePromotionQueue`, `FakeProfileStore`, a mutable test clock.

- [X] T056 [P] [US4] Create `MAIN/api/command/PromotePendingImagesUseCase.java`: `extends UseCaseHandler<Command, Result>`; `record Command()`; `record Result(int promoted, int failed, int reported)`; Javadoc per `contracts/use-cases.md`.
- [X] T057 [US4] Create `TEST/application/PromotePendingImagesServiceTest.java` (write first). Setup: `PromotionPolicy(horizon = 2 h, delays = [1 min, 5 min, 30 min], pollBatchSize = 10, lease = 5 min)`; a `Clock` the test can advance (small mutable clock helper inside the test). Cases:
  1. no due rows → `Result(0,0,0)`, no storage call.
  2. success → row deleted, `storage.promoteCalls` holds the `OwnedImage`, `Result(1,0,0)`; `FakeProfileStore.findByUserId` now reports `promoted = true`.
  3. failure before the horizon → `attempts` 1, `nextAttemptAt = now + 1 min`, not reported, `Result(0,1,0)`; second failure → 2 and `+5 min`; fourth failure → delay stays `30 min` (last repeats, never longer).
  4. row not due (`nextAttemptAt` in the future) → untouched.
  5. failure at `createdAt + 2 h` exactly → `reportedAt` set, slot cleared (profile avatar null), error logged (assert via the fake or just the outcome), `Result(0,1,1)`; the row is never returned by a later `takeDue`.
  6. exhausted promotion for an older id while the slot already holds a newer id → newer id untouched, row still reported.
  7. exhausted promotion for a user with no profile → no exception, no profile created, row reported.
  8. crash simulation: `clearImageIf` already applied (slot null) but row unreported → next failing run clears nothing more and reports the row (research D7).
  9. leased row: after one `handle` that fails, a second `handle` at the same instant takes nothing.
  10. batch of three where the middle fails (`failingPromotions`) → the other two promoted, `Result(2,1,0)`.
  11. a second `handle` for an id whose first run promoted at the fake but crashed before the row was deleted (simulate by re-adding the row): the fake returns normally again → row deleted, counted as promoted. This proves the service deletes the row whenever `promote` returns normally; that the real adapter returns normally when the permanent copy exists is the port contract (`contracts/ports.md`) verified in the infrastructure spec.
- [X] T058 [US4] Create `MAIN/internal/application/command/PromotePendingImagesService.java` implementing T056, `@Service`, **not** transactional (Javadoc like `RemoveObsoleteImagesService`); constructor `(ImageStorage storage, PendingImagePromotionRepository promotions, ProfileRepository profiles, PromotionPolicy policy, Clock clock)`. Flow: `due = promotions.takeDue(now, policy.pollBatchSize(), policy.lease())`; per row: try `storage.promote(row.image())`, `promotions.markPromoted(id)`, `promoted++`; catch `RuntimeException`: `failed++`; `attempts = row.attempts() + 1`; if `policy.expired(row.createdAt(), now)`: first `profiles.clearImageIf(user, slot, id, now)`, then `promotions.markFailed(id, attempts, now, now)`, log at error level with `imageId` and `userId`, `reported++`; else `promotions.markFailed(id, attempts, now.plus(policy.delayBeforeRetry(attempts)), null)` and log at warn. One failure never stops the loop. No log message contains the words token, secret, hash or credential (existing logging rule).
- [X] T059 [US4] Run `-Dtest=PromotePendingImagesServiceTest` until green, then the full core suite.

**Checkpoint**: the whole upload lifecycle (request, confirm, promote) works against fakes.

---

## Phase 7: User Story 5 - Replaced and deleted images are removed, with retries (Priority: P2)

**Goal**: the removal worker, re-keyed, with no loss of behaviour.

**Independent Test**: `RemoveObsoleteImagesServiceTest` and `FakeRemovalQueueTest`.

- [X] T060 [P] [US5] Create `TEST/application/fakes/FakeRemovalQueueTest.java` (research D12): duplicate `enqueue` for the same `ImageId` ignored without resetting attempts; lease hides a row; reported row not returned; `findActive` excludes reported rows; `clear` and `markRemoved` delete.
- [X] T061 [US5] Extend `TEST/application/RemoveObsoleteImagesServiceTest.java`: (a) the storage receives exactly the row's `OwnedImage` (user, slot, id) — assert on `storage.removeCalls`; (b) a row whose profile no longer exists is removed all the same (the service never reads a profile); (c) the same image queued twice yields one removal call; (d) exhausted retries set `reportedAt` and the row is not taken again; (e) removal of an image the fake reports as already gone counts as success (fake returns normally).
- [X] T062 [US5] Make T061 pass; expected change is limited to `MAIN/internal/application/command/RemoveObsoleteImagesService.java` log text. Run the full core suite.

---

## Phase 8: User Story 6 - Deleting an account releases images and queued work (Priority: P2)

**Goal**: one transaction: delete profile, queue removals, drop promotion rows.

**Independent Test**: `DeleteProfileDataServiceTest`.

- [X] T063 [US6] Extend `TEST/application/DeleteProfileDataServiceTest.java` (write first): (a) profile with avatar A and banner B plus promotion rows for both → profile gone, two removal rows `OwnedImage(7, AVATAR, A)` and `OwnedImage(7, BANNER, B)`, zero promotion rows for user 7; (b) banner only → exactly one removal row; (c) no profile → nothing queued, no exception; (d) another user's promotion rows are untouched; (e) running the same command again → no further change; (f) the service class carries `@Transactional` (reflection on the annotation); this is all the core can assert about atomicity, since rollback of the three steps is proved in the JDBC spec.
- [X] T064 [US6] Make T063 pass. The service already has its final form from T039, so no production change is expected; if a case fails, fix `MAIN/internal/application/command/DeleteProfileDataService.java` (order: delete profile, queue removals, delete the user's promotion rows).
- [X] T065 [US6] Run `-Dtest=DeleteProfileDataServiceTest` then the full core suite.

---

## Phase 9: User Story 7 - The backstop sweep removes only unowned permanent images (Priority: P3)

**Goal**: sweep rules hold under the new keys.

**Independent Test**: `SweepUnreferencedImagesServiceTest`.

- [X] T066 [US7] Extend `TEST/application/SweepUnreferencedImagesServiceTest.java` (write first): grace period (younger skipped, older considered); referenced by the owner's profile in that slot → skipped; referenced id but in the **other** slot of the owner → not referenced, removed; active removal row → skipped; reported removal row → not active, so removed; owner has no profile → removed once old enough; three pages followed by cursor until `nextCursor` is null (assert `storage.listCursors`); one `ImageStorageUnavailableException` for one image → that image skipped, others in the page removed; `ImageStorageRateLimitedException` → sweep stops early and returns the count so far; after a removal the removal row for that id is cleared.
- [X] T067 [US7] Make T066 pass; changes are limited to `MAIN/internal/application/command/SweepUnreferencedImagesService.java`. Document in its Javadoc that `list` returns permanent images only, so temporary files are never touched. Run the full core suite.

---

## Phase 10: User Story 8 - Save and read a profile on the new model (Priority: P3)

**Goal**: text saves leave images alone; reads report id and promoted flag.

**Independent Test**: `SaveProfileServiceTest`, `GetProfileServiceTest`.

- [X] T068 [P] [US8] Extend `TEST/application/SaveProfileServiceTest.java`: saving display name or bio on a profile that has avatar and banner leaves both unchanged; invalid display name or bio throws `InvalidProfileValueException` and writes nothing; nothing sent returns `Result(false)` and writes nothing.
- [X] T069 [P] [US8] Extend `TEST/application/GetProfileServiceTest.java`: avatar with a promotion row → `promoted == false`; after `FakePromotionQueue.markPromoted(id)` → `promoted == true`; banner empty → `banner()` is null; user who never saved → defaults with both images null and zero timestamps null; the returned `imageId` string equals `ImageId.value().toString()`.
- [X] T070 [US8] Make T068 and T069 pass (expected: no production change beyond US1). Run the full core suite.

---

## Phase 11: Polish & Cross-Cutting

- [X] T071 Run the complete core suite one last time and confirm the count (58 baseline, minus deleted `StoredImageTest` cases, plus new ones) with zero failures and zero errors; confirm it ran in under 60 seconds (SC-002).
- [X] T072 Run quickstart step 4: both T002 searches (`src/main`, and `src/test` with the three excluded files) must return no matches (SC-001, SC-009).
- [X] T073 Run quickstart step 5: `grep -rnE "^import (com\.minewaku\.chatter\.profilenext\.infrastructure|org\.springframework\.(jdbc|data|kafka|web|security)|jakarta\.servlet|com\.cloudinary)" services/profile-next/profilenext-core/src/main` must return no matches (FR-032).
- [X] T074 Run quickstart step 6 (compile the sibling modules once, `./mvnw -q -pl profilenext-infrastructure,profilenext-presentation -am compile 2>&1 | grep -E "ERROR.*\.java"`), and compare the failing files with research R4. Add any file R4 misses; confirm no error points into `profilenext-core`. This documents the gap; do not fix the sibling modules here.
- [X] T075 Update `specs/014-profile-image-core-redesign/quickstart.md` step 2 with the final test count, and in `research.md` R4 list the exact sibling files that fail to compile.
- [X] T076 Hand-off note: append a short "Next specs" section to `specs/014-profile-image-core-redesign/research.md` listing, for the infrastructure spec (Cloudinary adapter, JDBC repositories including the promotion repository with `created_at`, `V1__create_profile.sql`, `StructureRules.PORT_MAP` row for `PendingImagePromotionRepository`, scheduler, `ImageUrlResolver`, properties) and the presentation spec (`ProfileResponse`/`ProfileController` URL building, `ImageUploadController` always `200`), what the core now expects, citing `contracts/ports.md` and `contracts/use-cases.md`. Also restate risk R1 (a user with no saved profile cannot attach an image).

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 → Phase 2 → Phase 3 (US1)**: strictly sequential. US1 is the compile unit; nothing else can build until T049.
- **After T049** the stories can proceed in any order or in parallel, except where they share a file:
  - US3 (T052 to T055) and US8 (T069) both rely on `FakeProfileStore` from T034/T053; do T053 before T069. (`DeleteProfileDataService` is final from T039, so US3 case 12 and US6 do not depend on each other.)
  - US2 (T051) and US4 (T057) both touch `FakeImageStorage`; do T051 first or merge the edits.
- **Polish (Phase 11)** after all stories.

### Within each story

Tests first (they must fail for new behaviour), then the implementation, then the full core suite.

### Parallel opportunities

- Phase 2: T003 to T013 are all different new files, so T003, T005, T007, T009, T010, T011, T013 can start together; their tests (T004, T006, T008, T012) follow their types; T014 needs T013 and T007; T015 needs T014.
- Phase 3: tests T017 to T021 in parallel; ports T026, T027, T028 in parallel; API edits T030, T031, T032 in parallel; test updates T043 to T048 in parallel after the fakes (T033 to T035) exist.
- Phases 4 to 10: once T049 is green, US2, US4, US5, US7 and US8 can run in parallel by different people.

### Parallel example (Phase 2)

```text
Agent A: T003 ImageId → T004 ImageIdTest
Agent B: T005 ImageDimensions → T006 test
Agent C: T007 OwnedImage → T008 test → T013 promotion port → T014 FakePromotionQueue → T015 test
Agent D: T009 ImageRef, T010 ImageIds fixture, T011 PromotionPolicy → T012 test
```

---

## Implementation Strategy

### MVP first

Phases 1 to 3 give a clean, green core whose profile stores only identities (US1). The spec treats US1, US2 and US3 as the P1 core; stop after Phase 5 for the smallest slice that matches the redesign's request-and-confirm path (promotion arrives in Phase 6).

### Incremental delivery

1. Setup, Foundational (green), US1 (the swap, green at T049).
2. US2 then US3: request and confirm work against fakes.
3. US4: promotion completes the lifecycle.
4. US5, US6, US7, US8 in any order; each ends with a green full suite.
5. Polish: greps, sibling-module breakage inventory, hand-off notes.

### Notes

- Total tasks: 76 (Setup 2, Foundational 14, US1 33, US2 2, US3 4, US4 4, US5 3, US6 3, US7 2, US8 3, Polish 6).
- Do not edit infrastructure, presentation or start; their breakage is expected and recorded (research R4).
- Commit only when asked; if asked, one commit per checkpoint is a good grain (T016, T049, T055, T059, T070).
