# Research: Profile Image Redesign (core layer)

No `NEEDS CLARIFICATION` remained in the plan's Technical Context. The work here is resolving design choices the spec and `services/profile-next/IMAGE_REDESIGN_SPEC.md` leave open for the core, by reading the current code. Each entry: decision, rationale, alternatives.

## Findings from the code (baseline)

- Core builds and passes **58 tests** (`./mvnw -q -pl profilenext-core test`).
- The uncommitted working-tree edits are an unrelated refactor (records nested into ports, `DisplayName`/`Bio` in `Profile`, `PerceivedText`), not image-redesign work. The spec was corrected accordingly.
- The only id source in core is `UniqueStringIdGenerator.nextId(): String`; its adapter `UuidV7IdGenerator` (infrastructure) returns a UUIDv7 string.
- `StructureRules` (ArchUnit) lives in the **infrastructure** module's tests, so core has no architecture test of its own and none can run while infrastructure does not compile.
- `ProfileResponse` and `ProfileController` (presentation) read `GetProfileUseCase.Result.avatarUrl()/bannerUrl()`; `ImageUploadController` reads `Outcome.FILE_NOT_ALLOWED`.

## Decisions

### D1. `ImageId` is built by parsing the existing string id
- **Decision**: `ImageId.of(String)` parses the UUID string from `UniqueStringIdGenerator`. No new id port, no change to `UuidV7IdGenerator`, `PORT_MAP` row for it stays valid.
- **Rationale**: Smallest change; the generator already yields UUIDv7, whose lexicographic and `UUID.compareTo` order both follow time for ids from one generator.
- **Caveat**: `java.util.UUID.compareTo` compares the two longs as **signed**. For UUIDv7 the high 48 bits are a millisecond timestamp, so the most significant bit is 0 until year 10889 and signed order equals time order. `ImageIdTest` pins this with ids from the two ends of a realistic range.
- **Alternatives**: a new `ImageIdGenerator` port (extra port + adapter row for no gain); comparing strings as before (loses the typed id).

### D2. Ports exchange `OwnedImage(userId, slot, imageId)`
- **Decision**: one small record carries the three values that always travel together (promote, remove, enqueue, list entries, sweep lookups).
- **Rationale**: Removes 3-argument signatures and the chance of swapping slot and user; matches the redesign's `(userId, slot, id)` everywhere. `ConfirmedUpload` keeps the redesign's explicit fields (`userId`, `slot`, `imageId`, `format`, `bytes`, `ImageDimensions`) plus an `image()` accessor, so that document's shape is unchanged.
- **Alternatives**: loose parameters (error-prone); making `ConfirmedUpload` hold an `OwnedImage` (deviates from the redesign text for no real gain).

### D3. Nested records stay in the ports
- **Decision**: new port records (`PendingPromotion`, `ProfileView`, `StoredImageInfo`, `StoredImagePage`, `UploadPermission`, `ConfirmedUpload`, `DeletedProfile`) are nested in the port that returns them, continuing the working-tree direction. Domain values used across ports (`ImageId`, `ImageDimensions`, `OwnedImage`, `ImageRef`) stay in `model`.
- **Alternatives**: top-level records in `model` (the style being refactored away).

### D4. Confirm: verify, policy, then lock and decide, all in one transaction
- **Decision**: `ConfirmImageUploadService` stays class-level `@Transactional`. Order: `verify` (empty means `INVALID_SIGNATURE`), policy (returns `FILE_NOT_ALLOWED` before any database call), then `profiles.findForUpdate(userId)`; every later decision uses that locked state. Empty means `IGNORED`. Newer-than check is `ImageId.compareTo`.
- **Rationale**: Matches redesign 6.2; rejecting on policy before taking the lock keeps lock time minimal. The existing service already runs verify inside the transaction, harmless.
- **Test consequence**: an in-memory fake cannot show mutual exclusion. `FakeProfileStore` records the order of calls so a test asserts the lock call precedes any read of the image slot or write; the real exclusion is proven in the JDBC spec.

### D5. No profile row means `IGNORED` (risk R1)
- **Decision**: follow the redesign. `replaceImage` used to create the row; the new flow never does.
- **Rationale**: "no row" is the only signal available to tell a deleted account from a live one, and creating a row on confirm would resurrect a deleted account's profile.
- **Consequence (R1)**: a user who never saved a display name or bio has no row, so their first avatar upload is silently ignored and expires at the provider. Options for the product side, outside this spec: (a) make the client save the profile before offering upload; (b) have `RequestImageUpload` ensure a row exists (but that writes in the request path and an in-flight upload racing deletion could still resurrect it unless the row creation checks the account); (c) add an account-exists check. Recommendation: (a) now, revisit (b) with the account-deleted flow in mind. The spec states the behaviour as an edge case so it is tested rather than hidden.

### D6. Replacing an image cancels the old image's pending promotion (addition)
- **Decision**: on `APPLIED` with a previous image, besides `removals.enqueue(previous)`, call `promotions.remove(previousId)` (no-op when absent).
- **Rationale**: otherwise the worker retries a rename of a file the removal worker is deleting and ends with a false "promotion gave up" error log for an image no one references. This narrows the removal-vs-promotion race but cannot close it (a promotion already leased can still run); the sweep remains the backstop.
- **Alternatives**: leave it (noise and wasted provider calls); make removal wait for promotion (couples the two workers).

### D7. Promotion give-up is idempotent, not transactional
- **Decision**: `PromotePendingImagesService` is not transactional (as the removal worker). On failure past the horizon it first calls `profiles.clearImageIf(user, slot, id)`, then `promotions.markFailed(id, attempts, now, reportedAt = now)`, then logs at error level.
- **Rationale**: Clear-then-report can be repeated safely: if the process dies between the two, the row is still unreported, the next run fails again, clears again (a no-op because the slot no longer matches) and reports. The reverse order could leave a reported row and a slot that still points at an expiring temporary file, with nothing to fix it. This avoids a cross-repository transaction and the self-invocation pitfall of an annotated helper in the same class.
- **Alternatives**: a separate `@Transactional` collaborator bean (more code, same outcome).

### D8. Promotion timing: `PromotionPolicy`, horizon measured from creation
- **Decision**: `PromotionPolicy(Duration horizon, List<Duration> delays, int pollBatchSize, Duration lease)` with `delayBeforeRetry(n)` identical to `RemovalPolicy`. A failure is final when `now >= createdAt + horizon`; otherwise next attempt is `now + delayBeforeRetry(attempts)`. The queue row therefore exposes `createdAt` to the service (`PendingPromotion(OwnedImage, attempts, createdAt)`).
- **Rationale**: The redesign says "past the horizon (default 2 h)" and "mirror the removal worker" for backoff; this is the smallest reading that satisfies both. `maxRetries` is deliberately absent: the horizon is the only limit.
- **Edge**: if the first delay already crosses the horizon, the next run gives up on its first failure after the horizon; the check happens only when an attempt fails.

### D9. Dimensions are checked against a per-slot maximum only
- **Decision**: `ImagePolicy` gains `maxDimensionAvatar` and `maxDimensionBanner` (defaults 1024 and 2048 live in infrastructure config later); `permits(ConfirmedUpload)` is false when width or height exceeds the slot maximum, when bytes exceed the slot limit, or when the format is not allowed. No minimum. Values equal to the limit pass.
- **Rationale**: The redesign puts dimensions in the policy without a rule; the provider already applies `c_limit` at upload, so the check is a second line of defence that costs nothing.
- **Alternatives**: no dimension rule (leaves `ImageDimensions` unused by policy, contradicting the redesign); a minimum size (invented requirement).

### D10. Read model: `ProfileView` with `ImageRef`; the use case returns ids, not URLs
- **Decision**: `ProfileQueryRepository.findByUserId` returns `Optional<ProfileView>` (user id, `DisplayName`, `Bio`, `ImageRef avatar`, `ImageRef banner`, timestamps). `Profile` (write model) keeps plain `ImageId`s and is what `findForUpdate` returns. `GetProfileUseCase.Result` carries `ImageResult(String imageId, boolean promoted)` per slot, null when absent.
- **Rationale**: Core cannot build an address (that is infrastructure's resolver, redesign sections 4 and 10); the caller builds the URL from user id, slot and these values.
- **Alternatives**: put a URL-resolver port in core (extra port for something that is pure string formatting and provider-specific).

### D11. API cleanups forced by the model
- `UploadConfirmationRejectedException.Reason.USER_MISMATCH` is removed: the owner is parsed from the signed key, so it cannot differ from itself. Unverifiable or unparsable notifications use `INVALID_SIGNATURE`.
- `Outcome.FILE_NOT_ALLOWED` stays but its doc no longer says the file is queued for removal.
- Both are small breaks for presentation; see R2.

### D12. Port contracts are checked on the fakes, not by a shared contract suite
- **Decision**: no abstract contract test base reused by the JDBC tests. The fakes encode the contract (lease hides rows, duplicates ignored, reported rows not taken, clear-if-equal) and have their own few tests so a wrong fake cannot make a service test pass falsely.
- **Alternatives**: a contract base class in a core `test-jar` for infrastructure to extend (valuable, but needs build changes and infrastructure work that this spec excludes; worth proposing for the JDBC spec).

### D13. How the dependency rule is checked while ArchUnit cannot run
- **Decision**: `quickstart.md` includes a search of the core's imports for forbidden packages (`..infrastructure..`, `org.springframework.jdbc|data|kafka|web|security`, `jakarta.servlet`, `com.cloudinary`). The infrastructure spec adds the `PendingImagePromotionRepository` row to `PORT_MAP` and runs the real rules.
- **Alternatives**: add ArchUnit to core (new dependency and duplicate of the existing rules).

### D14. Sweep keeps its structure
- **Decision**: unchanged flow (page, filter by grace period, subtract referenced and actively queued, remove, stop early on rate limit, skip on unavailable). Lookups use `OwnedImage` so the later JDBC query can load profiles by primary key; removal-queue lookups use `ImageId`. The sweep never queries or removes temporary images because the port's `list` only returns permanent ones (contract, tested with a fake that would fail the test if asked otherwise).

## Risks and open items

- **R1 – Never-saved users cannot attach an image** (D5). Needs a product decision; not blocking this spec.
- **R2 – API breaks seen by presentation**: `GetProfileUseCase.Result.avatarUrl/bannerUrl` → `avatar/banner` references (`ProfileResponse`, `ProfileController`); `Reason.USER_MISMATCH` removed; `ConfirmImageUploadUseCase` doc. Presentation spec must adapt and build the URL via the infrastructure resolver, which presentation can only reach if the resolver is exposed through a port or passed from the start module; to be decided there.
- **R3 – Signed `UUID.compareTo`** (D1) is safe for UUIDv7; the test pins it.
- **R4 – Sibling modules stop compiling** (measured with `./mvnw -fae clean test-compile` from `services/profile-next` after the core work: core succeeds, infrastructure and presentation fail, start is skipped). javac stops at the first error phase, so the compiler lists only some files; the full set, found by searching for the changed API, is:
  - *Infrastructure, main*: `JdbcProfileRepository`, `JdbcProfileQueryRepository`, `JdbcPendingImageRemovalRepository`, `CloudinaryImageStorage`, `CloudinaryConfiguration` (builds `ImagePolicy`, which gained two dimension limits); new `JdbcPendingImagePromotionRepository`, a promotion scheduler, `ImageUrlResolver`, `V1__create_profile.sql`.
  - *Infrastructure, test*: `JdbcProfileRepositoryTest`, `JdbcProfileQueryRepositoryTest`, `JdbcPendingImageRemovalRepositoryTest`, `CloudinaryImageStorageTest`, `StructureRules.PORT_MAP` (new row for `PendingImagePromotionRepository`).
  - *Presentation, main*: `ProfileResponse` (read `avatar`/`banner` instead of `avatarUrl`/`bannerUrl`), `ImageUploadController`, `UploadPermissionResponse`, `ApiExceptionHandler` (`Reason.USER_MISMATCH` is gone). *Test*: `ImageUploadControllerTest`, `ProfileControllerReadTest`.
  - *Start, test*: `FakeCloudStorage`, `ImageFlowE2eTest`, `AccountDeletionE2eTest`, `ProfileFlowE2eTest`; wiring for `PromotionPolicy` and `DeleteProfileDataService`'s new constructor parameter.
  The promotion table in the redesign already has `created_at`, which D8 needs; the repository just has to return it.
- **R5 – Environment**: build with the per-service `./mvnw` (never bare `mvn`; see memory note on WSL), run from `services/profile-next`.

## Implementation notes

- Implemented 2026-11-08. Core result: 126 tests, 0 failures; both old-model searches and the import search return nothing.
- Deviation from the plan, small: the `DeleteProfileDataService` constructor took its final form (with the promotion queue) in US1 rather than US6, per the analysis fix; `FakeImageStorage` gained `failPermit` and `rateLimitPermit` in the same step as the fake rewrite instead of in US2.
- Test-only detail worth knowing: `SaveProfileServiceTest.RecordingProfiles` declares its own `calls` list, which hides the `calls` list that `FakeProfileStore` now has for the lock-order checks. Harmless, but rename one of them if it ever confuses.

## Next specs (what the core now expects)

**Infrastructure spec**
- `CloudinaryImageStorage` implements `permit`, `verify`, `promote`, `remove`, `list` on `OwnedImage` exactly as in `contracts/ports.md` (signing with `overwrite=false` and the `c_limit` transformation, parsing the owner, slot and id only from the signed temporary key, promote counting an existing permanent copy as success, `list` returning permanent images only). Folder names live there.
- JDBC: `JdbcProfileRepository` (`findForUpdate` is `SELECT ... FOR UPDATE`; `clearImageIf` is a conditional `UPDATE`), `JdbcProfileQueryRepository` (`promoted = NOT EXISTS` promotion row, `findReferenced` by primary key), `JdbcPendingImageRemovalRepository` re-keyed by image id with `user_id` and `slot`, new `JdbcPendingImagePromotionRepository` (`created_at` returned in `PendingPromotion`; `remove` and `deleteByUserId`), `V1__create_profile.sql` per the redesign section 5.
- Proof that only a real database can give: row-lock exclusion for racing confirmations and deletion, transaction rollback of the three account-deletion steps, and the lease (`FOR UPDATE SKIP LOCKED`). Consider a shared contract-test base for the queues (research D12).
- `StructureRules.PORT_MAP` gets a row for `PendingImagePromotionRepository`; add a scheduler for `PromotePendingImagesUseCase` mirroring the removal one; build `PromotionPolicy` (horizon default `PT2H`), the two new `ImagePolicy` dimension limits (1024 and 2048) and `ImageUrlResolver` from properties.

**Presentation spec**
- `ProfileResponse` builds the address from user id, slot, `imageId` and `promoted` through the resolver; `ImageUploadController` always answers `200`; `ApiExceptionHandler` drops `USER_MISMATCH`; the upload-request controller must take the user id from the authenticated principal only.

**Open product risk R1** still stands: a user with no saved profile cannot attach an image, because a confirmation for a user without a row is `IGNORED`.
