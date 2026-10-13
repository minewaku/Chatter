# Research: Authentication and Sessions

**Feature**: `specs/002-authentication-sessions/spec.md`  
**Date**: 2026-10-11

## Decision 1: Keep this delivery in identityaccess-core

**Decision**: Add the authentication capability under `services/identity-access-next/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/authentication/`. Put public use cases in `command/` and `query/`; put application services, models, and ports under `internal/`. The plan defines storage and token contracts but does not implement adapters, HTTP endpoints, cookies, or JWT signing.

**Rationale**: The source brief and FR-001 explicitly limit this feature to the core. Existing core use cases extend `shared/handler/UseCaseHandler` and use constructor-injected services. The separate email-verification capability provides a precedent for direct synchronous cross-capability coordination.

**Alternatives considered**: A generic token package would conflict with the brief. Implementing presentation and storage now would expand scope beyond the approved spec.

## Decision 2: Separate session identity from refresh proof

**Decision**: `AuthenticationSession` has a UUIDv7 session ID. A separate refresh credential contains 32 cryptographically random bytes encoded as an opaque URL-safe string. The core receives the credential only transiently. The future storage adapter derives a deterministic SHA-256 digest for indexed lookup and stores no plaintext credential. A session list never contains the credential or digest.

**Rationale**: The spec requires at least 128 bits of unpredictability. UUIDv7 carries a timestamp and has at most 74 unconstrained random bits, so it is suitable for a time-ordered ID but cannot itself be the refresh proof ([RFC 9562 §5.7](https://www.rfc-editor.org/rfc/rfc9562.html#section-5.7)). A 32-byte random secret exceeds the requirement. OWASP recommends unpredictable, meaningless session identifiers from a cryptographically secure generator ([OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)). The digest is adequate for lookup of a high-entropy random secret; password hashing remains separate.

**Alternatives considered**: Using the UUIDv7 ID as the bearer credential fails the entropy target. A token format with embedded ID and secret, encryption codec, or rotating credential adds behavior the brief excludes. Storing plaintext would violate FR-005.

## Decision 3: Use storage-neutral, conditional session operations

**Decision**: Define core ports to create a session with a presented refresh credential, find by presented credential or session ID, update last use only while usable, revoke one session conditionally, revoke all other sessions for an account, revoke all sessions for an account, and list safe session views. The presented-credential lookup and create methods pass the raw value across the port; hashing, indexing, persistence, and serialization belong to its adapter. Conditional methods return whether or how many records actually changed.

**Rationale**: The brief's draft `findBySessionId` alone cannot resolve an opaque presented credential. The account-facing command contracts require idempotent, ownership-scoped revocation. Atomic conditional operations prevent two concurrent sign-outs from both reporting a new revocation and keep last-use changes from overwriting revocation.

**Alternatives considered**: Fetching all sessions and scanning for a token would be costly and would require readable token storage. Read-then-save revocation without a conditional write has a race. A separate general token repository would violate capability ownership.

**Failure ordering**: Generate access and refresh strings in memory before creating a sign-in session, and return them only after `create` succeeds. During renewal, generate the access string before `touchIfUsable`, then return it only if the conditional touch succeeds. This avoids recording last use when issuance fails and avoids returning credentials when persistence rejects the operation.

## Decision 4: Inject time and lifetime policy

**Decision**: Use the existing UTC `Clock` and an injected positive `Duration` to issue bounded sessions. Expiry is exclusive: the session is unusable when `now >= expiresAt`. `lastUsedAt` begins absent and changes only after successful renewal. Revocation time is written once. Core does not choose a production lifetime or record-retention period; the composition layer must supply those policies when adapters are introduced.

**Rationale**: The spec deliberately leaves lifetime and retention to service policy. The email-verification capability already uses `Clock` and `Duration` with an exact expiry boundary. Injected time makes boundary tests deterministic.

**Alternatives considered**: Hard-coding a 30-day or other lifetime would invent a product policy. Using `Instant.now()` throughout would make boundary and ordering tests fragile.

## Decision 5: Reuse account eligibility and password matching

**Decision**: Look up accounts through `UserAccountRepository`, normalize email through `Email`, require `UserAccount.isAccessible()`, and verify the submitted password through `PasswordHasher.matches`. Only `ACTIVE` is accessible; `PENDING_VERIFICATION`, `LOCKED`, `SUSPENDED`, and `DELETED` are denied. Present absent account, wrong password, and inaccessible account as the same authentication failure. Treat malformed submitted passwords as failed authentication, without exposing whether an account exists.

**Rationale**: Existing account rules and the Part 1 prerequisite already define these behaviors. `PlainPassword` currently enforces the registration strength format, so authentication must normalize its validation error to the generic sign-in failure. Targeted and other-session sign-out require password matching before any session mutation.

**Alternatives considered**: Duplicating account status rules in authentication risks divergence. Reporting distinct sign-in causes would expose account state. Weakening the global password type is unnecessary for this scope.

## Decision 6: Coordinate account changes synchronously

**Decision**: Introduce an authentication-owned revocation coordinator called by existing password-change, suspension, and soft-delete services only when their account mutation actually changes state. The caller's account-save and revoke steps remain synchronous in the existing transaction boundary; failure propagates instead of reporting success. A production adapter arrangement must provide a shared atomic commit or equivalent coordination so an account change cannot complete while its old sessions remain renewable.

**Rationale**: FR-012 requires every session to be revoked on successful changes. `RegisterUserService` already calls the email-verification coordinator synchronously. A direct coordinator avoids restoring messaging or putting storage details into user services. The core-only plan can define the required port behavior, while actual multi-store transaction design belongs to the later adapter implementation.

**Alternatives considered**: An event listener would violate the brief and delay the security effect. Calling a repository directly from each user service duplicates coordination. Assuming `@Transactional` spans unrelated stores without adapter support would leave a false guarantee.

## Decision 7: Preserve the listed session controls and access-token boundary

**Decision**: Implement the five listed public commands and one query from the approved spec, including password-confirmed targeted and other-session sign-out. Access token creation remains behind `AccessTokenIssuer.issue(UserId, Email)`; core neither parses nor signs tokens. Revocation stops refresh but does not claim that an already-issued, self-contained access token is instantly invalidated.

**Rationale**: FR-001 and FR-008–011 explicitly include those controls, settling the brief's earlier optional wording for this feature. The brief locates JWT signing in infrastructure. The spec's measurable revocation outcome concerns previously issued refresh credentials.

**Alternatives considered**: Deferring named controls would leave approved requirements unplanned. Putting JWT claims or cookie settings in core would violate the capability boundary.

## Decision 8: Test the core contract without pretending runtime wiring exists

**Decision**: Use JUnit 5 core tests with in-memory port fakes, a fixed `Clock`, deterministic credential and ID generators, and tests for state, authorization, exact expiry, idempotent counts, and failure ordering. Run `mvn -pl identityaccess-core -am test` from `services/identity-access-next`. Document adapter and HTTP checks as future integration gates rather than runnable steps in this core-only feature.

**Rationale**: The current service has no authentication adapters and only a registration HTTP entry point. Existing core tests already use in-memory fakes. The command checks this feature's module without touching sibling services.

**Alternatives considered**: An HTTP/DB end-to-end script cannot run until adapters and presentation bindings exist. A full repository build is broader than needed for this plan.

## Decision 9: Keep device information minimal in core

**Decision**: Expose `DeviceInfo(String label)` as a value type nested in `AuthenticateUseCase`, then store the validated label in `AuthenticationSession` and show it in `SessionView`. Require a nonblank label of at most 200 characters. Presentation may derive it from client data later, but core does not model IP addresses or browser headers.

**Rationale**: The brief requires `DeviceInfo` in sign-in and session views but specifies no fields. A bounded human-readable label is enough for a person to recognize a session, and a nested contract type keeps `command/` limited to public use-case files.

**Alternatives considered**: An opaque, unbounded object would make persistence and views unclear. IP and user-agent fields would add transport details and privacy requirements outside the core feature.
