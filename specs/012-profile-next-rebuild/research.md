# Research: Profile Service Rebuild (profile-next)

Each entry is a decision the plan relies on. Items marked **verify** rest on provider behaviour that could not be confirmed from the repository; the task that closes each one is named.

## R1. One core slice, `profile`, plus `shared`

**Decision**: Core has a single feature slice, `com.minewaku.chatter.profilenext.profile`, with `api/{command,query,exception}` and `internal/{application,model,port}`, plus `shared/{handler,port}`.

**Rationale**: The committed `StructureRules.CORE_PACKAGES` is `{…profilenext.profile.., …profilenext.shared..}`, so the skeleton already fixes one slice. Profile text, images and account-deletion cleanup all act on the same row and share one transaction boundary; splitting them into slices would add cross-slice calls for no gain. Presentation reaches core only through `profile.api` (the existing `PresentationArchitectureTest` forbids `..internal..`).

**Alternatives**: Separate `image` and `account` slices. Rejected: they would need the `Profile` model across a slice boundary.

## R2. Saving is one atomic upsert, not read-modify-write

**Decision**: `ProfileRepository.applyEdit(userId, displayName?, bio?, now)` runs a single `INSERT … ON CONFLICT (user_id) DO UPDATE SET display_name = COALESCE(:displayName, profile.display_name), bio = COALESCE(:bio, profile.bio), updated_at = :now WHERE (profile.display_name, profile.bio) IS DISTINCT FROM (COALESCE(:displayName, profile.display_name), COALESCE(:bio, profile.bio))` and returns whether a row was written. A save that sends neither field does nothing and creates nothing.

**Rationale**: FR-005 asks for exactly one profile under concurrent first saves, and for each save to be applied whole. One statement gives both without locks or a version column: the unique key arbitrates the insert, and Postgres serialises the two updates on the row. The `IS DISTINCT FROM` guard gives "no changes, nothing rewritten" (edge case) and means `updated_at` moves only on a real change and always moves then (legacy defect 13). Partial-update semantics come from the 2026-11-04 clarification: an omitted field is `null` in the command, a bio sent as `""` is `""`.

**Alternatives**: `SELECT … FOR UPDATE` then update (two round trips, and the insert race still needs `ON CONFLICT`); optimistic version column (retries pushed onto callers).

## R3. Length rules count what a person sees

**Decision**: `DisplayName` and `Bio` are core value objects. They trim, reject control characters (`Character.isISOControl` and the Unicode `Cc`/`Cf` format category except the joiner and variation-selector code points that emoji sequences need), then count user-perceived characters with `java.text.BreakIterator.getCharacterInstance()`. A raw input longer than 1,024 UTF-16 units is refused before counting, to bound the work. The error carries the rule (`display_name_length`, `display_name_blank`, `display_name_control_characters`, `bio_length`) and the real limits (1–32, 0–190).

**Rationale**: FR-004 and the edge cases. `BreakIterator` is in the JDK, so core stays free of libraries. It follows extended grapheme clusters closely enough for emoji sequences on Java 21. Fixes legacy defects 17 (message states the real limits; control characters, not only `\n\r\t`; UTF-16 miscount).

**Alternatives**: ICU4J (a large dependency for one rule); `String.codePointCount` (still miscounts emoji sequences).

**Storage headroom**: columns are `text` with a `CHECK (char_length(display_name) <= 1024)`, not `varchar(32)`, because 32 graphemes can be far more than 32 code points. The rule lives in core; the check only stops a bug from storing megabytes. Values already stored are never re-validated on load (legacy defect 17, last bullet): value objects are built from user input only, and read models carry plain strings.

## R4. Direct upload to Cloudinary with a server-chosen public id

**Decision**: The upload permission is a Cloudinary signed-upload parameter set. The server picks the public id `chatter/profile/{avatars|banners}/{userId}/{uploadId}` (`uploadId` is a UUIDv7) and signs it together with `allowed_formats=jpg,png,webp`, a `timestamp`, and `context=userId={userId}|slot={AVATAR|BANNER}`. Because the public id is signed, the caller can upload only to that one location, for that one slot.

**Rationale**: FR-007. No temp folder and no rename step: the legacy temp-then-permanent move existed to support content-addressed sharing, which FR-017 removes. A UUIDv7 is time-ordered, which R5 uses to recognise stale confirmations.

**Limits**: the 5 MB (avatar) and 10 MB (banner) limits are enforced in two places. **verify (closed by the Cloudinary adapter task)**: whether Cloudinary offers a per-signed-upload size limit; if it does, the permission carries it. Either way, the confirmation (R5) rejects a file whose reported `bytes` exceed the slot limit and queues it for removal, so the limit holds even if the provider does not enforce it. The same applies to `allowed_formats`: the confirmation also checks the reported `format`.

**Lifetime**: **verify (closed by the Cloudinary adapter task)**: Cloudinary rejects signed requests whose `timestamp` is more than about an hour old, so "short-lived" is bounded by the provider at roughly one hour. The response carries `expiresAt` computed from a configurable permission lifetime (default 15 minutes) which the client should respect; the provider's window is the hard ceiling.

## R5. Confirmation: verify, check, apply once

**Decision**: The webhook is a public endpoint (no bearer token). Its body is read raw; `ImageStorage.verify(rawBody bytes, signature, timestamp)` recomputes the provider signature (`X-Cld-Signature` over body + `X-Cld-Timestamp` + API secret, constant-time compare, timestamp within a configured tolerance, hash algorithm configurable) and returns the confirmed upload parsed from the public id (user, slot, upload id) plus size, format and URL, or nothing. The use case then:

1. refuses when the user in the signed `context` differs from the user in the public id, or the file breaks the type or size limit (limit breach: the file is queued for removal and the use case returns the outcome `FILE_NOT_ALLOWED`, which the controller answers with 403; it is a returned outcome, not an exception, so the queued removal is committed with the transaction; profile unchanged);
2. in one transaction, makes sure the profile row exists (`INSERT … ON CONFLICT DO NOTHING`), locks it (`SELECT … FOR UPDATE`), and replaces the slot only if the new upload id is newer than the stored one (UUIDv7 strings of the same format compare in time order);
3. queues the replaced file, or the stale file when the confirmation lost the comparison, for removal.

**Rationale**: FR-008. A repeat of the same confirmation finds the same upload id already stored, so nothing changes. A repeat of an old confirmation after a newer upload finds a newer id stored, so it changes nothing and its file (already removed or about to be) is queued for removal again, which is harmless because removal of a missing file counts as success. This needs no processed-event table and no content hash. A confirmation for a user with no profile creates the profile (edge case).

**Alternatives**: a table of processed confirmation ids (extra table, grows forever); trusting `context` alone without the signed public id (a forged context would need the API secret anyway, but the public id is the stronger binding).

## R6. Removal is a database work queue, drained by a scheduled worker

**Decision**: Table `pending_image_removal(public_id PK, slot, attempts, next_attempt_at, created_at, reported_at)`. Any change that makes a file obsolete inserts a row in the same transaction. A scheduled worker (every 30 seconds, configurable) takes due rows with `FOR UPDATE SKIP LOCKED`, calls `ImageStorage.remove`, deletes the row on success, and on failure increments `attempts` and sets `next_attempt_at`. Delays between attempts: 2, 4, 8, 16, 32, then 60 minutes for the remaining attempts, so 10 retries span about 6 hours (clarification 2026-11-04). After the tenth failed retry the row gets `reported_at`, an ERROR log line is written once, and the row stays for an operator. The worker never retries a reported row; the hourly sweep (R7) finishes it, because a reported file is by then unreferenced and old. All of this is configuration.

**Rationale**: FR-009. Because the row is written in the same transaction as the profile change, "removal only after commit" holds by construction, and "a failed removal never fails or undoes the profile change" holds because the removal is not attempted inside that transaction. It is a work queue read by the service's own worker, not an event the service publishes and consumes (FR-017); the class names avoid `Outbox`, which `LegacyMachineryTest` bans. `SKIP LOCKED` keeps two workers from taking the same row at the same moment, and a short lease (`next_attempt_at` pushed forward when a row is taken, default 5 minutes) keeps a second instance from taking a row whose provider call is still in flight. Removal is idempotent, so a lease that expires mid-call causes at worst one repeated delete.

**Alternatives**: `@TransactionalEventListener(AFTER_COMMIT)` (loses the removal on a crash between commit and call); Resilience4j retry in memory (loses state on restart).

## R7. Periodic cleanup lists the provider, not the database

**Decision**: An hourly job pages through the provider's Admin API listing under the prefix `chatter/profile/`, and for each page removes files that are older than the grace period (24 hours, configurable), referenced by no profile row, and without an unreported queue row (a reported row does not protect its file; the sweep removes the file and clears the row). The reference check is one query per page.

**Rationale**: FR-010 and story 3, scenario 6: "uploads that were permitted but never confirmed" leave no database trace, so only the provider's listing can find them. The grace period exceeds the permission lifetime plus any webhook delay, so a pending upload is never removed early.

**Cost**: the Admin API is rate-limited per account (**verify (closed by the cleanup task)**); the job pages at the provider's maximum page size and stops cleanly when told to back off, resuming next hour. At this scale (a handful of users, hourly) this is not a constraint; it is recorded here because it is the first place scale would bite.

## R8. The consumer: Spring Kafka in presentation

**Decision**: `AccountDeletedListener` lives in `presentation/account/messaging`, as an inbound adapter beside the web controllers, and calls `DeleteProfileDataUseCase`. Group `profile-next`, topic `identityaccess.user.v1`, string key and value, JSON parsed by the listener into a presentation record (own copy of the shape). Rules:

- Header `eventType` other than `UserSoftDeleted`: acknowledged and skipped. Unknown JSON fields ignored.
- Unparseable value, or a missing `eventId`/`userId`/`occurredAt`: not retryable, straight to the dead-letter topic.
- Any other failure (for example the database is down): retried with exponential backoff (1 s start, ×2, 30 s cap, about 2 minutes in all), then published to `identityaccess.user.v1.profile-next.dlt` by `DeadLetterPublishingRecoverer`, and the listener moves on.
- `DeleteProfileDataUseCase` deletes the profile row (success when none), and queues the two images, in one transaction.

**Rationale**: FR-011, FR-012, and the contract in `contracts/account-deleted-event.md`. The listener container uses one thread per partition and the key is the user id, so notices for one user are handled in order; blocking retries keep that order until the record moves to the dead-letter topic. The retry values are configuration. SC-003's one-hour bound after an outage is met because a storage outage does not fail the notice (removal is queued), and a broker outage only delays arrival. A database outage longer than the retry window sends the notice to the dead-letter topic; replaying it is a documented runbook step, not automatic.

**Alternatives**: non-blocking retry topics (reorders notices for one user); Spring Cloud Stream (more machinery than one listener needs).

## R9. Authentication: public key from Vault, no revocation

**Decision**: Resource server with `NimbusJwtDecoder` using the public key read once at start from Vault `secret/common/jwt/rs256` (`public-key`), issuer checked (`identity-access`), audience not checked, as in `identity-access`. The caller id is `sub` as a `long`. There is no Redis and no revoked-token filter. Public paths: `/actuator/health/**` and the webhook path.

**Rationale**: FR-006. Copying the `identity-access` start-module pattern (`signing/`) keeps one way of loading keys. Only the public key is read; `profile-next` never holds the private key.

## R10. Persistence: `JdbcClient`, Flyway, schema `profile_next`

**Decision**: `JdbcClient` adapters (no Spring Data repositories) and Flyway migration `V1__create_profile.sql` in schema `profile_next`, in the same shared database `chatter` that `identity-access` uses (`identity_access` schema).

**Rationale**: Same reasoning as spec 013's plan: no aggregate to map, and the upsert in R2 is one hand-written statement. The `identity-access` precedent is a schema inside `chatter`. Spec 012's assumption says "its own database inside one shared instance"; a schema is the weaker form of that, and the connection URL is the only thing that changes to move to a database, because no query names the schema. **Flag for the user**: this plan follows the `identity-access` precedent; say if you want a separate database now.

## R11. Errors: same shape and codes, own copy

**Decision**: `ApiError` and a subset of `ErrorCode` are copied into `profilenext-presentation/shared/error` with identical names, statuses and messages (`validation_failed`, `malformed_request`, `unauthenticated`, `forbidden`, `not_found`, `method_not_allowed`, `unsupported_media_type`, `internal_error`, `service_unavailable`). Rule failures use `validation_failed` with a `FieldError` naming the rule (R3).

**Rationale**: FR-015. Each service owns its copy, as with the event shape; there is no shared library. The contract file lists the codes so a reviewer can compare.

## R12. Existing build rules the new code must respect

- `LoggingRuleTest` rejects any log call whose arguments mention `password`, `hash`, `token`, `secret`, `credential`, `privateKey`. Log fields are named `userId`, `publicId`, `attempts`, `eventId`; never a variable called `token` or `hash`. The webhook secret variable is named `apiSecret` and is never logged.
- `LegacyMachineryTest` rejects class names containing `Outbox`, `Cdc`, `Debezium`, `DomainEvent`, `IntegrationEvent`, `EventDispatcher`, `AggregateRoot`, `AssetReference`.
- `StructureRules.PORT_MAP` must gain one row per outbound port with exactly one adapter at the stated package, and `assertEveryCorePortIsMapped` forces the table to be kept current.
- `persistenceShapesArePackagePrivate` makes every class named `*Row`, `*Rows`, `*Record`, `*RowMapper` package-private.

## R13. Testing approach

JUnit 5, AssertJ, ArchUnit (as in the skeleton). Core use cases are unit-tested with in-memory fakes of the ports and a fixed `Clock`. Adapters use Testcontainers PostgreSQL (`postgres:17`). The listener test uses Testcontainers Kafka (`confluentinc/cp-kafka:7.6.0`, the image the shared Kafka component already uses). The Cloudinary adapter is tested against known signature vectors computed with the SDK's own signer and against a stubbed HTTP server for `destroy` and listing; real Cloudinary appears only in the manual quickstart. The concurrency rule (FR-005) is tested with two threads released together against the real database.

## R14. Legacy defects that apply, and where each is tested (FR-018, SC-008)

| # | Legacy defect | How it is absent | Test |
|---|---|---|---|
| 5 | Shared content-addressed asset deleted regardless of other users | No sharing: each image path names its owner | `UserDeletionTest.removesOnlyThatUsersImages` |
| 6 | Domain coupled to Spring Data | Core has no persistence types | `ArchitectureTest` (existing rule 1) |
| 10 | Hard-wired `Instant.now()`; clock-backwards becomes a business error | `Clock` injected; no such check | `SaveProfileServiceTest.usesTheInjectedClock` |
| 13 | Audit timestamp not updated on edits | `updated_at` set in the same statement as the change | `JdbcProfileRepositoryTest.updatedAtChangesOnEveryRealChange` |
| 14 | Replaced event emitted when nothing was replaced | No events; a removal is queued only for a file that existed | `ConfirmImageUploadServiceTest.queuesNothingWhenSlotWasEmpty` |
| 17 | Message states wrong limits; only `\n\r\t` rejected; UTF-16 count; validation on load | R3 | `DisplayNameTest`, `BioTest` |
| 11/12 | Revivable deleted state, account state mirrored | Out of scope (spec clarification 2026-11-04): no account state is kept | none |

Defects 1–4, 7–9, 15, 16, 18 and 19 concern `Asset`, domain events, dispatchers or exception base classes that do not exist in `profile-next`; `LegacyMachineryTest` and `ArchitectureTest` cover their absence.

## Verified during implementation

Closed by task T058 on 2026-11-04. Where a fact could not be confirmed from the provider's documentation, that is said, and the fallback already built in stays.

**(a) Per-upload size limit on a signed Cloudinary upload.** Not found. A search of Cloudinary's published documentation turned up no per-request maximum-file-size parameter for signed uploads; size limits appear to be account-level (a free account's upload cap) or client-side (the upload widget). The adapter therefore signs only `public_id`, `allowed_formats`, `context`, `timestamp` and `notification_url`, and the size rule is enforced at confirmation: `ConfirmImageUploadService` refuses a file whose reported `bytes` exceed the slot limit (5 MB avatar, 10 MB banner), queues it for removal, and returns `FILE_NOT_ALLOWED` (HTTP 403). The limit therefore holds whether or not the provider enforces one, at the cost that an oversized file is uploaded before it is removed. If Cloudinary documents a signable size parameter later, add it to `CloudinaryImageStorage.permit`; nothing else changes.

**(b) Admin API rate limit, webhook signature algorithm and timestamp window.**

- *Rate limit*: Cloudinary documents the Admin API as rate-limited per account, 500 requests per hour on the free plan and from 2,000 per hour on paid plans; Upload API calls are not limited. A limited call is answered with HTTP 420, which the SDK raises as `RateLimited`. `SdkCloudinaryClient` maps that to `ProviderRateLimitedException`, the adapter to `ImageStorageRateLimitedException`, and the sweep stops cleanly and resumes next hour (`SweepUnreferencedImagesServiceTest.stopsCleanlyWhenTheProviderReportsARateLimit`). The listing asks for 500 resources per page, the Admin API maximum, so one sweep of a small account is a handful of calls.
- *Webhook signature*: the signature is the hex digest of the raw body, the `X-Cld-Timestamp` value and the API secret. The algorithm is SHA-1 by default and SHA-256 when the account is set to use it; this was not re-confirmed against the live account, so it stays configuration (`profile-next.images.signature-algorithm`, default `SHA-1`, `SHA-256` accepted) and both are covered by `CloudinaryImageStorageTest`. Operators whose account signs with SHA-256 must set the property, as the Vault runbook says.
- *Timestamp window*: the default tolerance is two hours (`profile-next.images.webhook-tolerance`), matching the window the SDK's notification check uses. It was not confirmed against the live account; it is configuration.
