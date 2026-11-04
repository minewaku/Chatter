# Feature Specification: Profile Service Rebuild (profile-next)

**Feature Branch**: `profile-next`

**Created**: 2026-10-28 (updated 2026-10-29: relay decided, open questions resolved)

**Status**: Draft

**Input**: User description: "Rebuild the `profile` service as `profile-next` with the same layered structure as `identity-access`, without the heavy domain-driven machinery of the legacy service. A user can exist without a profile; the profile service knows nothing about registration or other account lifecycle, except that an account was deleted. Include, as a separate scoped area, the messaging `identity-access` needs so it can announce account deletion reliably." The legacy `services/profile` is read-only reference material.

## Clarifications

### Session 2026-11-04

- Q: If a profile edit or image confirmation lands just after the account-deleted notice was processed, how is the leftover profile handled? → A: Accepted as a documented residual race (rare, low-value data, bounded by the access-token window). The hard delete on the notice is the only removal path; no deleted-user record and no periodic profile sweep.
- Q: When a save sends only one field, what happens to the omitted field? → A: Partial update: an omitted field is left unchanged; a field sent empty is cleared (bio only, since a blank display name is refused by FR-004).
- Q: What limits does the upload permission put on an avatar or banner? → A: JPEG, PNG and WebP only; maximum 5 MB for an avatar and 10 MB for a banner.
- Q: How many times is a failed image removal retried before it is reported, and how old must an unreferenced image be before the cleanup deletes it? → A: Up to 10 retries with increasing delays (about 6 hours in total), then reported for attention; the cleanup deletes unreferenced images older than 24 hours. Both values are configuration.
- Q: Should FR-018 keep the "deleted or locked profile cannot be revived by an edit" example, given `profile-next` keeps no account state? → A: No. The example is removed; defects tied to account state are out of scope because that state is not mirrored.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See anyone's profile, even one that was never filled in (Priority: P1)

A signed-in person opens their own profile or another user's profile. If that user never edited anything, they still get a valid answer made of defaults, never a "not found". Clients can therefore render any user without first checking whether a profile exists.

**Why this priority**: Every other screen that shows a person depends on this read. It is also the smallest slice that proves the rebuilt service works end to end.

**Independent Test**: Call the read endpoints with a valid access token for (a) a user who has edited their profile and (b) a user who has never touched it. Both return a complete profile document; (b) carries the defaults.

**Acceptance Scenarios**:

1. **Given** a user who has saved a display name, bio and images, **When** any signed-in user reads that profile, **Then** the saved values are returned.
2. **Given** a user who has never edited a profile, **When** any signed-in user reads it, **Then** the response is successful and contains the user's id with empty display name, empty bio and no avatar or banner.
3. **Given** a request with no valid access token, **When** a profile is read, **Then** it is refused as unauthenticated.
4. **Given** a signed-in user, **When** they read their own profile through the "me" form, **Then** the result is the profile of the user named by their access token.

---

### User Story 2 - Edit my display name and bio (Priority: P1)

A signed-in person changes their display name and bio. The first edit creates their profile on the spot; later edits change it. People never have to "register" a profile.

**Why this priority**: Creating the profile lazily is the core behavioural change from the legacy service, and removes the need to learn about user registration.

**Independent Test**: With a user that has no profile, save a display name; read it back; save a different bio; read again and see both.

**Acceptance Scenarios**:

1. **Given** a user with no profile, **When** they save a valid display name, **Then** a profile now exists for them and later reads return it.
2. **Given** a user with a profile, **When** they save a new bio, **Then** the bio changes and the display name is untouched.
3. **Given** a display name or bio outside the allowed rules, **When** the user saves it, **Then** the save is refused with a message naming the rule and nothing is stored.
4. **Given** two saves for the same user arriving at the same moment, **When** both finish, **Then** exactly one profile exists and it holds the values of one of the saves, not a mix that either caller did not send.
5. **Given** a user, **When** they try to save a profile for a different user id, **Then** it is impossible: edits only ever apply to the user named by the access token.

---

### User Story 3 - Change my avatar and banner (Priority: P2)

A signed-in person uploads a new avatar or banner image directly to the image storage provider using a short-lived permission issued by the service. When the provider confirms the upload, the profile shows the new image and the previous image is removed from storage.

**Why this priority**: Images make profiles feel personal, but text editing already delivers a usable service without them.

**Independent Test**: Request upload permission, upload an image, deliver the provider's confirmation, then read the profile and see the new image; confirm the earlier image is gone from storage.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **When** they ask to upload an avatar or banner, **Then** they receive a short-lived, single-purpose permission limited to their own image location.
2. **Given** a genuine provider confirmation for a finished upload, **When** it reaches the service, **Then** the profile's image reference is updated and the previous image is scheduled for removal.
3. **Given** a confirmation whose signature is invalid, or that names a user other than the one who requested the upload, **When** it reaches the service, **Then** it is rejected and the profile is unchanged.
4. **Given** the same confirmation delivered twice, **When** it is processed, **Then** the second delivery changes nothing.
5. **Given** the storage provider is unreachable when the previous image should be removed, **When** the removal fails, **Then** the profile still shows the new image and the removal is retried later until it succeeds.
6. **Given** uploads that were permitted but never confirmed, or removals that kept failing, **When** the periodic cleanup runs, **Then** the leftover files are removed.

---

### User Story 4 - Deleting an account removes the person's profile data (Priority: P2)

When an account is deleted in `identity-access`, the service learns about it and removes that user's profile and stored images. Nobody has to ask; no personal profile data is left behind.

**Why this priority**: This is the one lifecycle fact the profile service must honour, and it is a privacy obligation. It ranks below the core read/edit flows because they can ship and be used first.

**Independent Test**: Deliver an account-deleted notice for a user who has a profile with images; confirm the profile reads as defaults and the images are gone. Deliver the same notice again; confirm nothing breaks.

**Acceptance Scenarios**:

1. **Given** a user with a profile and images, **When** the account-deleted notice for that user is processed, **Then** the profile row is removed and, once that has committed, the stored images are removed.
2. **Given** the same notice delivered more than once, or for a user who never had a profile, **When** it is processed, **Then** it succeeds and changes nothing further.
3. **Given** the image storage provider is down while processing, **When** the notice is processed, **Then** the profile data is still removed and the image removal is retried until it succeeds.
4. **Given** a notice that cannot be understood, **When** it arrives, **Then** it is set aside for inspection and does not block notices behind it.
5. **Given** a user whose account was deleted, **When** their old, still-valid access token is used, **Then** the request is judged only by token validity (see FR-006); the service holds no blacklist.

---

### User Story 5 - Account deletion is announced reliably by identity-access (Priority: P2)

This story belongs to `identity-access`, not to `profile-next`. When an account is soft-deleted, `identity-access` records the announcement in the same transaction as the deletion, so it can never be lost or sent for a deletion that was rolled back. A standalone change-capture process, separate from the service, reads the announcement from the database's change log and publishes it to the message broker, catching up after any outage.

**Why this priority**: Story 4 cannot work without it. It is split out so the two services can be built and tested independently.

**Independent Test**: Soft-delete an account while the broker is down; confirm the deletion succeeded. Bring the broker up; confirm exactly one announcement is published for that user. Roll back a deletion; confirm nothing is published. Stop and restart the relay process during a burst of deletions; confirm every deletion is published at least once.

**Acceptance Scenarios**:

1. **Given** an account soft-deletion that commits, **When** it commits, **Then** one announcement for that user was written atomically with the deletion.
2. **Given** a deletion that rolls back, **When** the transaction ends, **Then** no announcement exists and none is ever published.
3. **Given** the broker is unavailable, **When** an account is deleted, **Then** the deletion succeeds and is unaffected; the announcement is published once the broker is back.
4. **Given** the relay process was stopped, **When** it starts again, **Then** it resumes from where it stopped and publishes every announcement written meanwhile.
5. **Given** the relay crashed after publishing but before recording its position, **When** it restarts, **Then** the announcement may be published again; consumers MUST tolerate this (Story 4, scenario 2).

---

### Edge Cases

- A display name made only of spaces, control characters, or an emoji sequence: length is counted in characters as a person sees them, not in internal code units, and blank names are refused.
- A bio exactly at the maximum length is accepted; one character over is refused.
- Names and bios are stored as sent after trimming; no case-folding is applied to display names (they are not unique).
- The user saves with no changes: the call succeeds and nothing is rewritten.
- A signed-in user reads a profile id that is not a valid identifier: refused as a bad request, not as "not found".
- A user's token is still valid for a few minutes after logout or ban: the profile service accepts it for that window (FR-006).
- The account-deleted notice arrives while the same user is saving an edit: the edit may win the race and leave a profile behind. Only a later delivery of the notice removes it; this residual is accepted (see Assumptions).
- The provider's confirmation arrives for a user whose profile does not exist yet: the profile is created with that image.
- Image removal keeps failing: after 10 retries it is reported for attention, never silently dropped.

## Requirements *(mandatory)*

### Functional Requirements

**Profile service (`profile-next`)**

- **FR-001**: The service MUST return a profile document for any valid user id when asked by a signed-in caller. If no profile has been saved, it MUST return defaults (the user id, empty display name, empty bio, no avatar, no banner) with a successful status.
- **FR-002**: A signed-in user MUST be able to read their own profile through a "me" form that resolves the user from the access token.
- **FR-003**: A signed-in user MUST be able to save a display name and bio. If no profile exists for them, saving MUST create it. A save MUST change only the fields it sends: an omitted field is left unchanged, and a bio sent empty clears it. A save that sends neither field succeeds, changes nothing and creates no profile. Saving MUST apply only to the user named by the access token.
- **FR-004**: Display names MUST be 1 to 32 characters after trimming and MUST NOT contain control characters. Bios MUST be at most 190 characters after trimming and MAY be empty. Lengths MUST be counted in user-perceived characters. Violations MUST be refused with a response that names the broken rule.
- **FR-005**: Concurrent first-time saves for the same user MUST result in exactly one profile. Concurrent saves MUST each be applied whole: the final state equals the values of one of the saves, never a mix of the two.
- **FR-006**: The service MUST accept any access token that `identity-access` issued, that is unexpired and correctly signed, even if the session behind it has since been logged out or the account banned. The service MUST NOT keep a revocation list.
- **FR-007**: A signed-in user MUST be able to request a short-lived upload permission for their avatar and, separately, for their banner. The permission MUST be limited to that user's own image location and that one purpose, and MUST restrict the upload to JPEG, PNG or WebP files of at most 5 MB for an avatar and 10 MB for a banner.
- **FR-008**: When the storage provider confirms an upload, the service MUST verify the confirmation's authenticity, update the user's avatar or banner reference, and schedule removal of the previous image. A confirmation that fails verification, names a different user, or is a repeat MUST NOT change anything.
- **FR-009**: Image removal MUST happen only after the change that made the old image obsolete has been committed. A failed removal MUST NOT fail or undo the profile change, MUST be retried up to 10 times with increasing delays (about 6 hours in total), and MUST then be reported for attention if it never succeeds. A removal that has exhausted its retries and been reported MUST still be finished by the periodic cleanup (FR-010).
- **FR-010**: A periodic cleanup MUST remove stored images that no profile references and that are older than a 24-hour grace period. The retry count, delays and grace period MUST be configuration values, not constants in code.
- **FR-011**: The service MUST consume an account-deleted notice carrying an event id, the user id and the time it occurred, and MUST remove that user's profile and, after commit, their stored images. Processing MUST be idempotent and MUST succeed when no profile exists.
- **FR-012**: Notices MUST be consumed in order per user. A notice that still fails after several retries with increasing delays MUST be moved to a dead-letter topic without blocking later notices. Unknown fields and unknown event types in a notice MUST be ignored, not treated as failures.
- **FR-013**: The service MUST NOT publish any events; the only record it writes to the broker is a failed account-deleted notice to its own dead-letter topic (FR-012). It MUST NOT react to any account event other than account deletion.
- **FR-014**: Each profile MUST record when it was created and last changed, using an injectable time source so the behaviour is testable.
- **FR-015**: Errors MUST use the same error-response shape and code vocabulary as `identity-access`, so clients handle both services alike.

**Structure (applies to both services touched)**

- **FR-016**: `profile-next` MUST follow the module and package layout of `identity-access`: a core module with no dependency on persistence, web or messaging frameworks, containing feature-sliced public contracts and internal application, model and port packages; separate infrastructure, presentation and start modules. Every use case MUST be a single-method handler of a command or query. This MUST be enforced by automated architecture tests, as `identity-access` does.
- **FR-017**: The following legacy machinery MUST NOT exist in `profile-next`: an aggregate base class holding a queue of domain events; synchronous and asynchronous event dispatchers; a split between domain events and integration events; an asset record with content-addressed deduplication and reference counting; any outbox, change-data-capture or other loop in which the service consumes events it published itself.
- **FR-018**: Every defect in `services/profile/profile-domain-review.txt` that concerns behaviour kept in `profile-next` MUST be absent from it, and a test MUST exist for each such defect (examples: updated timestamps always change on update; a name length message states the real limits). Defects tied to account state (deleted, locked, banned, unverified) are out of scope, because `profile-next` does not mirror that state.

**Account-deletion announcement in `identity-access` (separate scope)**

The message layout, topic, relay settings and database roles are in [contracts/account-deleted-event.md](contracts/account-deleted-event.md).

- **FR-019**: When `identity-access` soft-deletes an account, it MUST record an account-deleted announcement (event id, user id, time) in the same transaction as the deletion. If the deletion rolls back, no announcement may remain. Account operations MUST NOT depend on broker availability.
- **FR-020**: A change-capture relay running as a standalone process, separate from `identity-access`, MUST publish each announcement to the message broker at least once, keyed by user id, and MUST resume from its last recorded position after a restart. `identity-access` itself MUST hold no broker client and contain no relay code.
- **FR-021**: Each announcement MUST reach the broker with: key = user id; value = a flat JSON object with the event id, user id and time of deletion and no personal data; headers carrying the event id and the event type; topic chosen by the kind of record it belongs to (`identityaccess.user.v1` for user events), not by event type, so that events about one user stay in order. Additive changes keep the topic; a breaking change creates a new versioned topic.
- **FR-022**: Spec 003 (FR-024 and SC-008) and the architecture test that forbids messaging and outbox classes in `identity-access` MUST be amended so that writing the announcement record is allowed. The bans on secret-store credential rotation and on the legacy request filters MUST remain.
- **FR-023**: The announcements record MUST NOT grow without bound: each row is removed in the same transaction that writes it, so the table is empty at rest and the relay reads announcements from the database's change log.
- **FR-024**: The relay MUST use its own database credential, limited to reading the announcements record and to replication, and MUST capture no other table. While no announcements arrive, it MUST keep advancing its position so the database does not retain change log for it.
- **FR-025**: The relay's broker producer MUST wait for full acknowledgement and MUST be idempotent. The `profile-next` consumer's dead-letter topic MUST exist before the service starts.

**Scope decisions**

- **FR-026**: Deleted-user data MUST be removed immediately on the notice. A reinstated account starts with an empty profile.
- **FR-027**: Profiles in the legacy `profile` service MUST NOT be migrated. Users start with empty profiles.
- **FR-028**: `identity-access` MUST publish only the account-deleted event in this feature. An "access revoked" signal for the `message` service is out of scope and will be specified separately; the topic and header design above MUST allow adding event types without breaking existing consumers.

### Key Entities *(include if feature involves data)*

- **Profile**: one per user at most, identified by the user's id. Holds display name, bio, avatar reference, banner reference, and created/last-changed times. Does not exist until the user first saves something; a missing profile is presented as defaults.
- **Image reference**: the storage provider's identifier and public address for one avatar or banner. Belongs to exactly one profile slot; it is never shared between users.
- **Pending image removal**: a record that a stored file is obsolete and still needs deleting, with a retry count. Exists only until the removal succeeds.
- **Account-deleted notice**: an event id, the user id, and the time of deletion. Consumed by `profile-next`.
- **Account-deleted announcement (in identity-access)**: a record written with the deletion and read by the relay from the change log. It exists only for the duration of one transaction; it carries the same fields as the notice plus the kind of record it belongs to and the event type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user who never edited anything can be shown to any other signed-in user with no error and no extra request: 100% of profile reads for existing user ids succeed.
- **SC-002**: A user can set a display name and read it back in two requests without any prior setup step.
- **SC-003**: After an account is deleted, 100% of that user's profile data and stored images are gone within 5 minutes under normal operation, and within 1 hour after a broker or storage-provider outage ends.
- **SC-004**: 1,000 repeated deliveries of the same account-deleted notice produce exactly the same end state as one delivery.
- **SC-005**: Soft-deleting an account while the message broker is down never fails or slows the deletion, and no deletion is ever left without its announcement: over a test of 1,000 deletions with random broker and relay-process outages, 0 announcements are lost and 0 are written for rolled-back deletions.
- **SC-006**: Replacing an image leaves exactly one stored file for that slot once removals have settled; zero files remain for deleted users.
- **SC-007**: No class or package from the removed legacy machinery (FR-017) exists in `profile-next`, and the automated architecture checks for FR-016 pass on every build.
- **SC-008**: Every behavioural defect listed in the legacy review that applies to `profile-next` has a failing-then-passing test.
- **SC-009**: A developer who knows `identity-access` can locate the use case, port and adapter for any `profile-next` behaviour on the first try, because the packages are laid out the same way.

## Assumptions

- Only signed-in callers can read profiles; public, anonymous reading is out of scope.
- The profile API keeps the legacy shape for reads and upload permissions: reading by id and by "me", and upload permissions for avatar and banner under `/api/v1/profiles`. Saving is one endpoint, `PATCH /api/v1/profiles/@me`, because saves are partial (clarification 2026-11-04).
- A profile does not store the username. Clients combine the profile with identity data for a name to show when the display name is empty.
- Display name limits (1–32) and bio limit (190) carry over from the legacy service. Display names do not have to be unique.
- Account state (locked, banned, unverified) is not mirrored in `profile-next`; the service neither checks it nor stores it. This is a deliberate removal of the legacy cross-service coupling.
- Cloudinary stays the image provider, with signed direct uploads and a webhook confirmation, as in the legacy service. Per-user deduplication of identical images is not required.
- A reading cache is not part of this feature. Reads go straight to the database; a cache is reconsidered only if measured read load justifies it.
- The relay uses Debezium change-data-capture over the database's change log, running as a standalone process; Kafka Connect, decided by spec 013. It uses plain JSON, with no schema registry, and each service owns its own copy of the event's shape; there is no shared event library.
- Each service has its own database inside one shared database instance, with logical decoding enabled once for that instance. *(Amended by spec 013: the shared instance is kept for now with logical decoding enabled on it, and each service is expected to get its own instance later.)* *(Amended by plan 012: each service starts with its own schema inside the shared database, as `identity-access` does; moving to a separate database later only changes the connection URL.)*
- A deleted account's edit racing with its own deletion may leave a profile behind that only a redelivered notice removes. This is accepted: the access-token window (FR-006) makes it unavoidable without a blacklist, the window is short, and the data is low-value. No deleted-user record and no profile sweep are kept; the periodic cleanup (FR-010) concerns images only.
- The message broker is already available to the environment. The standalone relay process and logical decoding are new platform requirements introduced here, with the connector definition kept alongside the other shared infrastructure.
- The `message` service and the "access revoked" signal are out of scope for `profile-next` (FR-028).
- How the service code is organised for the announcement (ports, packages, adapters) is a codebase decision for the plan, not part of this spec.
- Legacy `services/profile` stays read-only reference and is not modified.
