# Feature Specification: Profile Image Redesign (core layer)

**Feature Branch**: `profile-next`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "read services/profile-next/IMAGE_REDESIGN_SPEC.md but we gonna only work at the core layer first". The redesign document is the source of truth for behaviour; this spec carves out the part that lives in the `profile-next` core module (domain model, ports, use cases and application services, with their in-memory test doubles). Infrastructure (Cloudinary adapter, JDBC repositories, schema, schedulers, URL resolver, configuration), presentation (webhook controller) and the Cloudinary console setup are explicitly left for later specs.

## Scope

**In scope (core module only)**: the image-related domain model, the outbound ports the core depends on, the use cases and their application services (request an upload, confirm an upload, promote pending images, remove obsolete images, sweep unreferenced images, delete profile data, save profile, get profile), and the test doubles and tests that prove this behaviour without any real storage provider or database.

**Out of scope (later specs)**: the Cloudinary storage adapter and its signing/parsing, the JDBC repositories and the schema rewrite, scheduler/worker wiring and configuration properties, the delivery URL resolver, the webhook controller and its "always 200" handling, the MediaFlows purge and runbook, rate limiting of upload requests, and any change to other services.

**Starting point**: the working tree holds uncommitted edits in the core and adjacent modules that are **not** part of this redesign: records moved from `model/` into the ports as nested types, `Profile` using `DisplayName` and `Bio` value objects, and a `PerceivedText` change. They are a separate in-flight refactor. This feature builds on top of them (the nested-record port style is kept) and does not revert them. The core module builds today with 58 passing tests; that is the baseline.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A profile stores only image identities, never paths or addresses (Priority: P1)

A profile holds, for each of its two image slots (avatar and banner), at most one time-ordered image identity and nothing else: no provider path, no address, no metadata. Whoever needs to show an image derives where it lives from the owner, the slot and the identity. The identity's ordering reflects recency, so "which of two images is newer" is answered by comparing identities alone. The read model reports, per slot, the identity and whether the image has finished being moved into permanent storage.

**Why this priority**: Every other behaviour (confirmation, staleness, promotion, removal, sweep) is expressed in terms of this model. Without it nothing else can be built or tested.

**Independent Test**: Exercise the domain types alone, with no ports: create identities, compare them, build a profile with and without images, build a profile with one slot set, check validation of image dimensions. Compile the core module and confirm no type of the old path-and-address image model remains.

**Acceptance Scenarios**:

1. **Given** two image identities generated one after the other, **When** they are compared, **Then** the later one orders after the earlier one.
2. **Given** a profile for a user who never saved anything, **When** it is read, **Then** both image slots are empty and nothing else about images exists on it.
3. **Given** image dimensions with a zero or negative width or height, **When** they are constructed, **Then** construction is rejected.
4. **Given** an image slot, **When** its storage folder name is asked of the core, **Then** the core cannot answer: folder names are not a core concept.
5. **Given** a profile read for display, **When** an image is present in a slot, **Then** it is reported as an identity plus a promoted/not-promoted flag, and a missing image is reported as absent.

---

### User Story 2 - A user asks for permission to upload an image to their own slot (Priority: P1)

An authenticated user asks to upload an image for a slot. The core generates a fresh image identity, asks the storage port for a signed upload permission that can only target a location belonging to that user, that slot and that identity, and returns the permission (target address, method, fields, expiry) to the caller. Nothing is stored in the database. The user the permission is for always comes from the authenticated caller, never from request content.

**Why this priority**: It is the entry of the whole flow and carries the ownership guarantee: because the storage later reports back a location containing the user id, that id can be trusted only if permissions are issued exclusively for the caller's own id.

**Independent Test**: Call the use case against a fake storage and an identity generator; check the fake received exactly the caller's user id, the requested slot and a fresh identity, check the returned permission is passed through unchanged, and check no repository was touched.

**Acceptance Scenarios**:

1. **Given** an authenticated user and the avatar slot, **When** an upload is requested, **Then** the storage port is asked to permit exactly that user, slot and a newly generated identity, and the permission it returns is returned to the caller.
2. **Given** two requests in a row for the same user and slot, **When** both are served, **Then** each carries a different identity, and the second identity orders after the first.
3. **Given** any request, **When** it completes, **Then** no profile, removal or promotion record was created or changed.
4. **Given** requests for two different users, **When** each is served, **Then** the storage port is asked about exactly the user id carried by that request's command and no other. Making sure the command's user id comes from the authenticated principal, never from the request body, is the presentation layer's duty and is out of scope here.
5. **Given** the storage provider is unavailable or rate-limited, **When** a permission is requested, **Then** the corresponding existing "storage unavailable" or "rate limited" failure is surfaced and nothing is recorded.

---

### User Story 3 - A verified upload notification updates the profile exactly once, and only when it is newer (Priority: P1)

When the storage provider reports that a file arrived, the core verifies the notification through the storage port. A verified notification yields who the file is for, which slot, which identity, its format, size and dimensions. The core checks the file against the image policy (allowed formats, size limit per slot, dimensions). Then, inside one transaction, it locks the owning profile and decides:

- the file breaks the policy: nothing is written, outcome `FILE_NOT_ALLOWED`;
- the owner has no profile (for example the account was deleted): nothing is written, outcome `IGNORED`;
- the identity is newer than what the slot holds (or the slot is empty): the slot is set to the new identity, the previous identity (if any) is queued for removal together with its owner, and the new identity is queued for promotion together with its owner; outcome `APPLIED`;
- the identity is not newer, or is a repeat of the stored one: nothing is written, outcome `IGNORED`.

An unverifiable notification (bad signature, stale timestamp, malformed body, a location that is not a temporary-upload location, any other kind of notification) is reported as "not accepted" without touching any state. Rejected, stale and abandoned files are never queued for removal by the core; the provider's own expiry reclaims them.

**Why this priority**: This is where an upload becomes part of a profile. Its transactional and ordering rules are what prevent a lost update, a resurrected old image, or an image attached to a deleted account.

**Independent Test**: Drive the confirm use case with a fake storage that returns scripted verified uploads and an in-memory profile store with row-lock semantics; assert the outcome and the exact records written for each of the four cases and for the unverifiable cases, including a replay of the same notification and an older notification arriving after a newer one. The fake records that the lock was taken before any read or write, since an in-memory fake cannot prove real mutual exclusion.

**Acceptance Scenarios**:

1. **Given** a profile whose avatar is empty, **When** a verified avatar upload for that user arrives, **Then** the avatar becomes the new identity, a promotion record is queued with the user id, no removal is queued, and the outcome is `APPLIED`.
2. **Given** a profile whose avatar holds identity A (possibly still waiting for promotion), **When** a verified upload with newer identity B arrives, **Then** the avatar becomes B, A is queued for removal with the user id, A's pending promotion (if any) is cancelled, B is queued for promotion with the user id, and the outcome is `APPLIED`.
3. **Given** a profile whose avatar holds B, **When** a verified upload with older identity A arrives, **Then** nothing is written and the outcome is `IGNORED`.
4. **Given** a profile whose avatar holds B, **When** the notification for B arrives again, **Then** nothing is written and the outcome is `IGNORED`.
5. **Given** a verified upload that breaks the format, size or dimension policy, **When** it is processed, **Then** nothing is written and the outcome is `FILE_NOT_ALLOWED`.
6. **Given** a verified upload for a user who has no profile, **When** it is processed, **Then** nothing is written and the outcome is `IGNORED`.
7. **Given** a notification that fails verification, **When** it is processed, **Then** it is reported as not accepted, with the existing rejection reason, and no state changes.
8. **Given** a confirmation, **When** it decides, **Then** it has first taken the owner's profile for update, and every decision (no profile, newer, not newer) is made on that locked state. This is what, together with the real row lock in a later spec, makes a confirmation racing an account deletion either land first (its identity captured by the deletion) or find no profile; the core proves the order of calls, the database spec proves the exclusion.
9. **Given** an avatar upload and a banner upload for the same user, **When** they are processed in either order, **Then** each slot is updated independently of the other.

---

### User Story 4 - Newly applied images are moved into permanent storage, with retries and a clear give-up (Priority: P2)

A new use case, "promote pending images", is run periodically by a scheduler (wired in a later spec). It takes a batch of due promotion records, hands each to a lease so concurrent runs never take the same record, and asks the storage port to promote the image from its temporary location to its permanent one. On success the record is deleted, which is what makes the image "promoted" for readers. On failure the record is rescheduled with a growing delay and an incremented attempt count. A promotion that keeps failing past a configured horizon (default two hours) is marked reported, the slot is cleared, but only if it still holds that same identity, and an error is logged.

**Why this priority**: It completes the lifecycle (temporary to permanent) and is what makes the system self-healing after a failed rename, but the profile already points at the right image the moment story 3 completes, so it can be delivered after stories 1 to 3.

**Independent Test**: Run the promote use case with a fake storage that succeeds, fails transiently, or fails forever, an in-memory promotion queue and a fake clock. Check the record disappears on success, is rescheduled on failure with increasing delay, and after the horizon the slot is cleared only when it still holds the same identity and the record is marked reported.

**Acceptance Scenarios**:

1. **Given** a due promotion record and a storage that succeeds, **When** the promoter runs, **Then** the record is deleted.
2. **Given** a due promotion record and a storage that fails, **When** the promoter runs, **Then** the record's attempt count rises and its next attempt is later, by a delay that grows with attempts up to a cap.
3. **Given** a record created more than the horizon ago whose promotion still fails, **When** the promoter runs, **Then** the record is marked reported, the slot is cleared if and only if it still holds that identity, and an error is logged.
4. **Given** the slot has since been replaced by a newer identity, **When** an exhausted promotion for the older identity is given up, **Then** the newer identity is untouched.
5. **Given** a promotion record that is leased by one run, **When** another run asks for due records, **Then** the leased record is not returned to it.
6. **Given** a promotion that had in fact succeeded before the previous run crashed, **When** the next run asks the storage port to promote it and the port returns normally, **Then** the promotion counts as done and the record is deleted. (The port returning normally when the permanent copy already exists is its contract, FR-009, and is verified in the infrastructure spec.)
7. **Given** no due records, **When** the promoter runs, **Then** it does nothing and reports zero.

---

### User Story 5 - Replaced and deleted images are removed from storage, with retries (Priority: P2)

The existing "remove obsolete images" use case keeps its behaviour but is re-keyed: each queue record now carries the owner, the slot and the image identity, because the profile row may be gone when the worker runs. For each due record it asks the storage port to remove the image (both its temporary and permanent locations; an already-missing image counts as success). Success deletes the record; failure reschedules it with backoff; exhausting the retry limit marks it reported and leaves the file for the backstop sweep.

**Why this priority**: Keeps storage from filling with replaced images. Behaviour is unchanged apart from the key, so it is lower risk than the new flows.

**Independent Test**: Run the use case with a fake storage and an in-memory removal queue keyed by identity with the owner; verify success, transient failure, exhaustion, and the owner and slot being passed to the storage port.

**Acceptance Scenarios**:

1. **Given** a due removal record for an owner, slot and identity, **When** the worker runs, **Then** the storage port is asked to remove exactly that owner, slot and identity and the record is deleted on success.
2. **Given** a removal that fails, **When** the worker runs, **Then** attempts increase and the next attempt moves later by a growing delay.
3. **Given** a removal that exhausts its retry limit, **When** the worker runs, **Then** the record is marked reported, is no longer taken, and the file is left for the backstop sweep.
4. **Given** the image is already gone at the provider, **When** removal runs, **Then** it counts as success.
5. **Given** the same identity is queued twice, **When** the second enqueue runs, **Then** it is ignored (one record per identity).

---

### User Story 6 - Deleting an account releases all of its images and queued work (Priority: P2)

When profile data for a user is deleted (because the account was deleted), the core does, in one transaction: delete the profile row while capturing the image identities it held; queue each captured identity for removal together with the user id and slot; delete any promotion records of that user. A user with no profile does nothing. A repeated deletion changes nothing further.

**Why this priority**: Privacy and cleanliness on account deletion; depends on the queues from stories 4 and 5.

**Independent Test**: Run the delete-profile-data use case against in-memory stores: a user with both images and a pending promotion ends with no profile, both identities queued for removal with the user id, and no promotion record. A user with no profile ends with no change.

**Acceptance Scenarios**:

1. **Given** a profile with avatar and banner and a pending promotion, **When** the profile data is deleted, **Then** the profile is gone, both identities are queued for removal with the user id and slots, and the promotion record is gone.
2. **Given** a profile with only a banner, **When** the data is deleted, **Then** exactly one removal is queued.
3. **Given** no profile for the user, **When** the data is deleted, **Then** nothing is queued and nothing fails.
4. **Given** the same deletion is repeated, **When** it runs again, **Then** no further change occurs.
5. **Given** any failure part-way, **When** the transaction ends, **Then** none of the three steps are visible. The core guarantees the three steps run inside one transaction boundary (checked by a test); the actual rollback is proved against the real database in the JDBC spec.

---

### User Story 7 - The backstop sweep removes only permanent images nobody owns (Priority: P3)

The existing "sweep unreferenced images" use case is adapted to the new keys. It pages through the permanent images listed by the storage port; each entry carries an owner, a slot, an identity and a creation time. It skips entries younger than a grace period, entries referenced by the owner's profile in that slot, and entries with an active removal record; every other entry is removed. It only ever looks at permanent images, never temporary ones. The listing is paged and resumable.

**Why this priority**: A safety net for the rare races and exhausted retries described in the failure table of the redesign; the primary flows work without it.

**Independent Test**: Run the sweep with a fake storage that lists a mix of young, referenced, queued and unowned entries across several pages, and an in-memory store; check exactly the unowned old unqueued ones are removed, paging is followed to the end, and no temporary entry is ever requested.

**Acceptance Scenarios**:

1. **Given** a listed permanent image younger than the grace period, **When** the sweep runs, **Then** it is skipped.
2. **Given** a listed image whose owner's profile holds that identity in that slot, **When** the sweep runs, **Then** it is skipped.
3. **Given** a listed image with an active removal record, **When** the sweep runs, **Then** it is skipped.
4. **Given** a listed image old enough, unreferenced and not queued, **When** the sweep runs, **Then** it is removed.
5. **Given** a listing in several pages, **When** the sweep runs, **Then** all pages are processed and the cursor is followed until exhausted.
6. **Given** an entry whose owner has no profile, **When** the sweep runs, **Then** it counts as unreferenced and is removed once old enough.
7. **Given** the provider is briefly unavailable for one removal inside a page, **When** the sweep runs, **Then** that file is skipped and logged, the rest of the sweep continues, and it is retried on the next run. **Given** the provider asks to back off, **Then** the sweep stops early and reports what it removed so far (existing behaviour).

---

### User Story 8 - Saving and reading a profile work with the new image model (Priority: P3)

Saving display name and bio, and reading a profile, keep their current behaviour but operate on the new model. Reading returns display name, bio and, per slot, the image reference (identity plus promoted flag), or absence. Saving never touches image slots. The read side reports `promoted = false` exactly while a promotion record exists for that image.

**Why this priority**: Needed so the module compiles and behaves coherently end to end, but the observable behaviour does not change for text fields.

**Independent Test**: Existing save-profile and get-profile tests, updated to the new model: save creates and updates text fields without disturbing images; get returns the right reference and flag from in-memory fakes.

**Acceptance Scenarios**:

1. **Given** a profile with an avatar identity and a pending promotion for it, **When** it is read, **Then** the avatar is reported with `promoted = false`.
2. **Given** the promotion record has been deleted, **When** it is read again, **Then** the avatar is reported with `promoted = true`.
3. **Given** a user who never saved, **When** the profile is read, **Then** defaults are returned with both image slots absent.
4. **Given** a profile with images, **When** display name or bio is saved, **Then** the images are unchanged.
5. **Given** invalid display name or bio, **When** saving, **Then** the existing invalid-value failure is raised and nothing is written.

---

### Edge Cases

- The owner of an upload is taken only from the signed location, never from a separate field, so a mismatch between the two cannot exist. A notification whose location does not parse as a temporary-upload location is not accepted.
- Two confirmations for the same user and slot processed at the same time: the profile lock serialises them; the newer identity wins regardless of arrival order; the loser is ignored or queued for removal as its position dictates.
- A confirmation for the avatar of a user whose profile was created only by an earlier text save: the profile exists (row present), so the image is applied.
- A promotion that is given up clears the slot, so the user loses the new image **and** the previous one (the previous was already queued for removal when the new one was applied). The redesign accepts this: giving up happens only after the horizon of repeated failures, and the user simply uploads again.
- A confirmation arriving after account deletion: no profile, ignored, nothing created.
- A user who has never saved a display name or bio has no profile row (the first text save creates it). An image confirmation for that user is therefore `IGNORED` and the file expires at the provider. This follows the redesign (no row means ignored, which is what protects deleted accounts) and is recorded as an open risk in the plan; it is not silently worked around here.
- A promotion record for a user whose profile was deleted: account deletion removes it; if one survives, giving up must not recreate the profile and must not fail (clearing a slot of a missing profile changes nothing).
- A removal and a promotion for the same identity running concurrently: a permanent orphan may result; accepted, the backstop sweep covers it.
- The removal queue is asked to enqueue an identity already queued: ignored, no duplicate, no attempt reset.
- Retry delay arithmetic reaching its cap: the delay stays at the cap and does not overflow.
- Promotion horizon equal to or shorter than the first backoff step: the record is given up on its first failure past the horizon, not retried further.
- Image dimensions or byte size exactly at the allowed limit: accepted; one over: rejected.
- Format comparison is case-insensitive.

## Requirements *(mandatory)*

### Functional Requirements

**Domain model**

- **FR-001**: The core MUST represent an image by an identity that is time-ordered, comparable, and generated by the existing identity generator; comparing two identities MUST tell which is more recent.
- **FR-002**: The core MUST represent image dimensions as a width and a height that are both positive; construction with a non-positive value MUST be rejected.
- **FR-003**: A profile MUST hold, per slot, an optional image identity and nothing else about the image (no provider path, no address, no format, size or dimensions).
- **FR-004**: The previous model of an image as a path plus an address MUST be removed from the core, along with every use of it.
- **FR-005**: The image slot concept MUST remain (avatar, banner) but MUST NOT expose any storage folder name; folder names are not known to the core.
- **FR-006**: The read model MUST report each present image as an identity plus a flag saying whether it has been promoted to permanent storage; the write model MUST NOT carry a promotion state.
- **FR-007**: A confirmed upload, as produced by the storage port's verification, MUST carry exactly: owner, slot, image identity, format, size in bytes and dimensions. It MUST NOT carry a raw provider path or an address.

**Ports**

- **FR-008**: The storage port MUST offer these operations, with no provider type visible in any signature: permit an upload (owner, slot, identity, now); verify a notification (raw body, signature, timestamp, now) returning a confirmed upload or nothing; promote (owner, slot, identity); remove (owner, slot, identity), where an already-missing image is success; and list permanent images by cursor, each entry giving owner, slot, identity and creation time.
- **FR-009**: The storage port's promotion MUST treat a permanent copy that already exists as success, so a repeated promotion after a crash completes instead of failing. This is a contract on the port, written into its documentation in the core and verified against the real adapter in the infrastructure spec; the core's part is to delete the record whenever promotion returns normally.
- **FR-010**: The profile repository port MUST provide: apply a text edit; load a profile for update under a row lock so concurrent image confirmations for one user serialise; set a slot's image identity; clear a slot only if it still holds a given identity; delete a profile returning the identities it held.
- **FR-011**: The removal queue port MUST be keyed by image identity and each record MUST carry owner and slot, so a removal can run after the profile row is gone.
- **FR-012**: A new promotion queue port MUST exist, keyed by image identity, each record carrying owner and slot, supporting: enqueue (ignored when already present), take due records with a lease so two callers never receive the same record, delete on success, cancel one record by identity (no-op when absent), record a failed attempt with next time and optional reported time, and delete all records of a user.
- **FR-013**: The read-side port MUST return, per slot, an image reference whose promoted flag is true exactly when no promotion record exists for that identity.

**Use cases and behaviour**

- **FR-014**: Requesting an upload MUST generate a fresh identity, obtain a permission from the storage port for the authenticated user, requested slot and that identity, and return it; it MUST NOT write any record. The user MUST come only from the authenticated caller.
- **FR-015**: Confirming an upload MUST, after successful verification and a passing policy check, lock the owner's profile; if none exists it MUST write nothing and return `IGNORED`.
- **FR-016**: Confirming an upload MUST apply the identity only when the slot is empty or holds an older identity; on apply it MUST, in the same transaction, queue the replaced identity (if any) for removal, cancel the replaced identity's pending promotion (if any), and queue the new identity for promotion, each with the owner, and return `APPLIED`. Cancelling is an addition to the redesign document: without it the promotion worker retries a rename of a file that is being deleted and finally logs a false give-up error.
- **FR-017**: Confirming an upload that is a repeat of the stored identity, or older than it, MUST write nothing and return `IGNORED`.
- **FR-018**: Confirming an upload that breaks the format, size or dimension policy MUST write nothing and return `FILE_NOT_ALLOWED`.
- **FR-019**: Confirming MUST NOT queue rejected, stale, repeated or abandoned files for removal; reclaiming them is the provider's responsibility.
- **FR-020**: A notification that fails verification (signature, freshness, shape, or a location that is not a temporary-upload location of a known user and slot) MUST be reported with the existing "rejected" failure carrying the reason `INVALID_SIGNATURE`, and MUST NOT change any state. The reason `USER_MISMATCH` can no longer occur, because the owner is read only from the signed location, and is removed.
- **FR-021**: The image policy MUST check the format (case-insensitive, against the configured list), the byte size against a per-slot limit, and the dimensions against a per-slot limit, and MUST accept values exactly at the limit.
- **FR-022**: Promoting pending images MUST be a new use case: take a batch of due records with a lease, call the storage port's promote for each, delete the record on success, and on failure increment attempts and move the next attempt later with a delay that grows with attempts up to a cap.
- **FR-023**: A promotion still failing at or beyond the horizon MUST mark its record reported, clear the slot only if it still holds that identity, and log an error; a reported record MUST NOT be taken again.
- **FR-024**: Promoting MUST NOT stop at the first failure: one failing record MUST NOT prevent the others in the batch from being attempted.
- **FR-025**: Removing obsolete images MUST pass the owner, slot and identity of each due record to the storage port and apply the existing retry, backoff and give-up behaviour; a given-up record MUST be marked reported and left for the sweep.
- **FR-026**: Deleting profile data MUST, in one transaction, delete the profile capturing its identities, queue each for removal with owner and slot, and delete the user's promotion records; with no profile it MUST change nothing, and repeating it MUST change nothing further.
- **FR-027**: The backstop sweep MUST page through permanent images only, skip those younger than the grace period, those referenced by the owner's profile in that slot, and those with an active removal record, remove the rest, follow the cursor to the end and survive a single removal failure.
- **FR-028**: Saving a profile MUST edit display name and bio only and leave image slots untouched; reading a profile MUST return text fields and per-slot image references as described in FR-006 and FR-013. Because the core no longer knows addresses, the read use case's result carries, per slot, the image identity and promoted flag instead of an address; building the address is the job of a later layer.
- **FR-029**: Retry timing for promotion (first delay, growth, cap, batch size, lease, horizon) MUST be supplied to the application service as a policy value, mirroring the removal policy, so it can later be bound to configuration without changing the service.

**Verification**

- **FR-030**: Every behaviour above MUST be provable with in-memory test doubles for the storage port, the repositories and the clock, without a real provider, database or network.
- **FR-031**: The existing tests for the replaced model (`StoredImageTest`, `ConfirmedUploadTest`, fakes and the command-service tests) MUST be updated or replaced so none refers to the old model, and new tests MUST exist for: image identity ordering, dimension validation, the promotion service (success, retry, exhaustion, partial batch failure, concurrent lease), the confirm service's four outcomes and its unverifiable cases, and account deletion with a pending promotion.
- **FR-032**: The core module MUST build and its tests pass on their own, and the dependency rule MUST still hold: the core imports no infrastructure, provider, persistence or web type. The architecture tests that enforce this live in the infrastructure module and cannot run until that module compiles again, so in the meantime the rule is checked by a search of the core's imports (see the quickstart); the architecture tests' port-to-adapter table gains a row for the new promotion queue in the infrastructure spec.
- **FR-033**: Modules outside core (infrastructure, presentation, start) are not changed by this feature. They are expected not to compile against the new ports until their own specs land; that gap MUST be noted in the handoff and MUST NOT be papered over by shims in core.

### Key Entities *(include if feature involves data)*

- **Image identity**: a time-ordered unique identifier for one uploaded image. Comparing two identities tells which is newer. The only thing a profile remembers about an image.
- **Image slot**: avatar or banner; each slot of a profile holds at most one identity.
- **Image dimensions**: width and height in pixels, both positive; used only to check an incoming file against policy.
- **Profile**: a user's display name, bio, and per-slot image identity, with created and updated times.
- **Image reference (read model)**: an identity plus a promoted flag, telling a reader whether to look in permanent or temporary storage.
- **Confirmed upload**: what a verified provider notification yields: owner, slot, identity, format, size and dimensions.
- **Upload permission**: the signed instructions a client uses to upload one file to the user's own location: target address, method, fields and expiry.
- **Pending promotion**: a queued task to move one image from temporary to permanent storage; carries identity, owner, slot, attempt count, next attempt time, created time and reported time.
- **Pending removal**: a queued task to delete one image from storage; same shape as a pending promotion.
- **Stored image listing entry**: an item the sweep sees: owner, slot, identity and creation time of a permanent image.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A profile record contains zero image attributes other than the two slot identities; no path, address or metadata field remains anywhere in the core module (verified by search and compilation).
- **SC-002**: All eight user stories' acceptance scenarios are covered by automated tests that run with no network, database or provider and complete in under 60 seconds for the core module. Three properties can only be shown on the real database or adapter and are covered by their own specs, not here: row-lock exclusion (Story 3, scenario 8), transaction rollback (Story 6, scenario 5) and the port's "permanent copy already exists" behaviour (FR-009).
- **SC-003**: In 100% of the confirm scenarios (apply, replace, stale, repeat, policy failure, no profile, unverifiable) the set of records written matches the expected set exactly; in the not-applied cases the number of records written is zero.
- **SC-004**: Replaying the same confirmation any number of times, or delivering confirmations out of order, leaves the profile holding the newest identity ever confirmed and never produces more than one promotion record or more than one removal record per identity.
- **SC-005**: A permission request never results in a database write, and the user id it asks the storage port about is always exactly the one in the command, shown by a test that fails if either happens. (That the command's user id is the authenticated caller's is checked in the presentation spec.)
- **SC-006**: After an account deletion, zero profile rows, zero promotion records for the user, and exactly one removal record per image the profile held exist.
- **SC-007**: A promotion that fails indefinitely is given up no later than the first attempt past the configured horizon, and by then it has been retried with strictly non-decreasing delays that never exceed the cap.
- **SC-008**: The sweep never requests or removes a temporary image, a young image, a referenced image or an image with an active removal record, in any combination of the listed scenarios.
- **SC-009**: The core module compiles and its full test suite passes (the baseline is 58 tests, all passing today), and no file, test or test double in the core refers to the old path-and-address image model.

## Assumptions

- The behaviour is exactly that of `IMAGE_REDESIGN_SPEC.md` (sections 3, 6, 10 as far as they concern core, and 12 for core tests); where this spec and that document disagree, that document wins and this spec is corrected.
- The existing identity generator for time-ordered identities, the existing clock abstraction, the existing use-case handler base and the existing exception types for rejected confirmations and storage failures are reused unchanged.
- The existing removal and sweep policies (batch size, lease, backoff, grace period) keep their current values and shape; the new promotion policy mirrors the removal one, with a default horizon of two hours.
- The per-slot dimension limits used in the image policy come from the same per-slot limits that the infrastructure later uses to bound uploaded images (1024 for avatar, 2048 for banner as the redesign's defaults); the core receives them as policy values and does not read configuration.
- Only the infrastructure adapter knows storage key layout and folder names; the core passes owner, slot and identity and never builds or parses a path.
- Rate limiting of upload requests is deferred until after the core is finished, as the redesign says; this spec adds none.
- The webhook's "always answers 200" rule is a presentation concern and out of scope; the core only needs to return outcomes or throw the existing rejection, and the presentation spec will map them.
- The core and infrastructure modules will not build together between this spec and the follow-on infrastructure spec; that is accepted and tracked as a dependency rather than avoided with compatibility code.
- Scheduled invocation of the promotion use case, the Cloudinary adapter, the schema change and the URL resolver are separate specs that depend on this one.
- The uncommitted edits in the working tree belong to an unrelated refactor (nested port records, `DisplayName`/`Bio` in `Profile`). They are kept and built upon; this feature does not revert or re-do them.
- The image policy checks dimensions against a maximum per slot only, no minimum. The provider already bounds stored images to that maximum, so the check is a second line of defence; the redesign names dimensions as part of the policy but gives no number, so this is a plan-level default.
- A user with no saved profile cannot attach an image until the first text save creates the profile row. This is accepted from the redesign and flagged as a risk for the product side to settle (see the plan); changing it would mean letting a confirmation create a profile, which would let a deleted account reappear.
