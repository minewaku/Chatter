# Feature Specification: Identity Access Infrastructure Adapters

**Feature Branch**: `master`
**Created**: 2026-10-14
**Status**: Draft
**Input**: User direction on 2026-10-14: update `identity-access-next`'s infrastructure module using the old `identity-access` service as reference (messaging excluded), adopt the resource-server security setup from the message service, settle a persistence package structure that reflects CQS, and provide one shared database stack per database type under `infrastructure/components`.

## Clarifications

### Session 2026-10-14

- Q: Is messaging (outbox, change-data-capture, Kafka routing, in-process event queue) part of this feature? → A: No. The new service has no messaging yet, so nothing is carried over from the old messaging, CDC, or outbox code.
- Q: Does the access-credential authentication filter carry over from the old service? → A: No. Incoming access credentials are validated by the framework's resource-server support, copied in shape from the message service, so a custom authentication filter would be redundant.
- Q: How are databases provisioned? → A: One shared instance per database type for the whole project, not one per service. Development credentials are `admin` / `123456`; stronger secret handling is deliberately out of scope for this personal project.
- Q: Inside the single shared relational database, does each service keep its own schema or do all share one? → A: Each service gets its own schema (user-delegated, standards-based choice). The identity-access schema holds its tables and its own migration history, so services can join the same database without clashing.
- Q: How should the database adapters represent stored data: separate persistence classes with mappers, or Spring database annotations directly on core classes? → A: Separate persistence classes with mappers. The write side maps stored rows to and from the account model; the read side maps rows directly into the core result types without an extra intermediate class. Core carries no storage annotations or storage-framework dependency.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Run the Service Against Real Storage (Priority: P1)

A developer starts one relational database and one key-value store from the shared infrastructure folder, starts the identity-access-next service, and registers an account. The account survives a service restart, and a second registration with the same email or username is rejected.

**Why this priority**: Core behaviors are verified only against test fakes. Nothing else is trustworthy until account data survives a restart.

**Independent Test**: Start the shared relational database from `infrastructure/components`, start the service, register an account, restart the service, and look the account up by identifier and by email. Then register the same email again.

**Acceptance Scenarios**:

1. **Given** the shared relational database is running and empty, **When** the service starts, **Then** the identity-access schema and its account tables are created automatically, inside that schema only, and the service reports itself healthy.
2. **Given** a registered account, **When** the service is restarted, **Then** the account can still be found by identifier, email, and username with all its data unchanged.
3. **Given** an existing account, **When** a registration reuses its email or username, **Then** no second account is stored and the existing "account already exists" outcome is returned.
4. **Given** the database is unreachable at start-up, **When** the service starts, **Then** it fails fast with a message naming the unreachable store instead of starting in a half-working state.

---

### User Story 2 - Passwords, Identifiers, and Tokens Work for Real (Priority: P1)

A person registers, verifies their email, signs in, renews access, and signs out against the running service. Every technical capability the core needs is backed by a working implementation: password hashing and checking, time-ordered numeric account identifiers, unique session identifiers, access credentials, refresh credentials, and verification emails.

**Why this priority**: The core use cases from specs 001 and 002 are only reachable end to end once every outbound dependency has a real implementation.

**Independent Test**: With both stores running, register an account, read the verification email, verify, sign in, renew access, list sessions, and sign out. Inspect stored data to confirm no plain password, verification token, or refresh credential is stored or logged.

**Acceptance Scenarios**:

1. **Given** a registration with a password, **When** the account is stored, **Then** only a salted hash with its algorithm label is stored. A correct password later matches and an incorrect one does not.
2. **Given** two services or threads creating accounts at the same moment, **When** identifiers are generated, **Then** no two identifiers are equal and later identifiers sort after earlier ones.
3. **Given** a pending account, **When** verification is requested, **Then** an email containing a working verification link is sent. The token is stored only as a digest and expires on schedule without cleanup work.
4. **Given** a successful sign-in, **When** the access credential is inspected, **Then** it is signed with the service's private key, names the account as subject and carries the account email, issuer, audience, issue time, and a configured finite lifetime. It does not use a hard-coded lifetime.
5. **Given** a refresh credential, **When** it is stored, **Then** only a digest is persisted, and an expired or revoked session remains readable for the retention period defined by service policy.
6. **Given** a failed or retried mail delivery, **When** delivery is attempted, **Then** it is retried a bounded number of times and, if still failing, the failure is reported without exposing the token in logs.

---

### User Story 3 - Protect Endpoints with Standard Token Validation (Priority: P1)

An API client calls a protected endpoint with a valid access credential and is accepted. A call with a missing, expired, tampered, or foreign-issuer credential is rejected. Registration, sign-in, verification, and refresh endpoints remain reachable without a credential.

**Why this priority**: Without this, session-list and revoke endpoints would either be open to anyone or blocked for everyone.

**Independent Test**: Call a protected endpoint with a freshly issued credential, then with no credential, an expired credential, and a credential signed by a different key. Call each public endpoint with no credential.

**Acceptance Scenarios**:

1. **Given** a credential issued by this service, **When** it is presented to a protected endpoint, **Then** the request is accepted and the authenticated account identity is available to the endpoint.
2. **Given** a missing, malformed, expired, or wrongly signed credential, **When** presented to a protected endpoint, **Then** the response is "unauthenticated" and no endpoint logic runs.
3. **Given** no credential, **When** a public endpoint (registration, sign-in, verification request and confirmation, refresh, API documentation) is called, **Then** the request proceeds.
4. **Given** a cross-origin preflight request, **When** received, **Then** it is answered without requiring a credential.
5. **Given** the configured public key is missing or malformed, **When** the service starts, **Then** start-up fails with a clear message rather than accepting or rejecting all requests silently.

---

### User Story 4 - Understand Where Storage Code Lives (Priority: P2)

A maintainer opening the infrastructure module can tell, from folder names alone, which business capability an adapter serves, whether it is on the read side or the write side, and which storage technology it uses. Adding a cache or a second store for an existing capability does not require reorganizing existing folders.

**Why this priority**: The old service grouped code by technology first. That hid which adapter belonged to which core port and made the read/write split invisible.

**Independent Test**: For every outbound port declared in core, locate its single implementation using only the documented path convention. Confirm that no class sits in a catch-all `service`, `helper`, or `config` folder when it belongs to a capability.

**Acceptance Scenarios**:

1. **Given** any core outbound port, **When** a maintainer follows the documented convention, **Then** exactly one implementation is found at the predicted location.
2. **Given** a capability with both a write-side repository and a read-side repository, **When** the folders are listed, **Then** the two sides are separate siblings with no shared mutable code.
3. **Given** a read-side cache backed by a different technology than its source of truth, **When** it is added, **Then** it lives under its own technology folder beneath the read side and no existing folder moves.
4. **Given** the infrastructure module, **When** an automated structure check runs, **Then** core classes import no storage, token, mail, or HTTP library types, and adapters are not referenced from core.

---

### User Story 5 - One Shared Database Stack per Type (Priority: P2)

A developer finds, under `infrastructure/components`, one ready-to-run stack for each database type the project uses (relational and key-value), each with the same development credentials, instead of a database bundled into every service.

**Why this priority**: Per-service databases duplicate setup and make local start-up slow. This also unblocks the other services from sharing the same stacks later.

**Independent Test**: From a clean checkout, start each stack with its documented command, connect with `admin` / `123456`, and confirm the service can use it. Stop and restart and confirm data persists.

**Acceptance Scenarios**:

1. **Given** a clean checkout, **When** the documented start command for the relational stack is run, **Then** one database server starts, accepts the `admin` credentials, and contains a database usable by this service.
2. **Given** a clean checkout, **When** the documented start command for the key-value stack is run, **Then** one server starts and accepts the `admin` credentials.
3. **Given** either stack is stopped and started again, **When** reconnecting, **Then** previously stored data is still there.
4. **Given** the existing components (message broker, secrets vault, tunnel), **When** the new stacks are added, **Then** the existing components are unchanged and network names and host ports do not collide.

---

### Edge Cases

- Two registrations race on the same email or username: exactly one account is stored and the other receives the "already exists" outcome, with no raw storage error exposed.
- An account is saved from stale data: the stale write is rejected and not silently applied.
- A verification token or refresh session expires while a request is in flight: the boundary follows core rules (expired at the exact moment current time reaches expiry).
- The key-value store loses entries early (restart, eviction): affected verification tokens read as unknown and sessions read as absent. No wrong account is ever activated or authenticated.
- The relational store restarts mid-request: the request fails cleanly and no partial account is stored.
- The process clock moves backwards: identifier generation waits for or rejects the rollback rather than issuing duplicates.
- An access credential issued by a previous key is presented after key replacement: it is rejected.
- A log line is emitted for any request, response, or exception: it never contains a raw password, raw verification token, raw refresh credential, or private key.

## Requirements *(mandatory)*

### Functional Requirements

**Adoption from the old service**

- **FR-001**: The infrastructure module MUST provide exactly one implementation for each outbound port already declared in core: password hashing, time-based identifier generation, unique string identifier generation, access-credential issuing, refresh-credential generation, account repository, account query repository, email-verification repository, verification notifier, session repository, and session view repository.
- **FR-002**: Password hashing MUST use a memory-hard salted algorithm labelled `argon2id`, MUST return a hash, salt, and algorithm label compatible with the existing account schema, and MUST NOT log or otherwise expose the raw password or the hash.
- **FR-003**: Time-ordered numeric account identifiers MUST be unique across concurrent callers and across service instances. The instance discriminator MUST come from configuration (the existing `identity-access.snowflake.worker-id` setting) and MUST NOT depend on a database lookup as in the old service.
- **FR-004**: Unique string identifiers for sessions MUST be time-ordered and globally unique.
- **FR-005**: Access credentials MUST be asymmetric-key signed, carry subject, email, issuer, audience, issue time, and expiry, and take their lifetime from configuration. The private and public keys MUST be loaded from configuration, and an invalid or missing key MUST stop start-up.
- **FR-006**: Refresh credentials MUST be opaque, unpredictable, and generated from a cryptographically secure random source. The old reversible-encryption scheme, generation counter, and replay handling MUST NOT be carried over.
- **FR-007**: Verification emails MUST be rendered from a template, built with a verification link that honours the configured public base address of the service, and sent with bounded retry. The old hard-coded `localhost` link MUST NOT be carried over.
- **FR-008**: Verification tokens MUST be stored only as digests and MUST expire on their own at their expiry time without a scheduled cleanup job.
- **FR-009**: Sessions MUST store only a digest of the refresh credential, MUST support the atomic create, touch-if-usable, revoke-one, revoke-others, and revoke-all operations defined by the core port, and MUST remain readable after expiry or revocation until the service's retention policy removes them.
- **FR-010**: Account persistence MUST use the existing `user_account` schema and its migration, enforce unique email and username, and use the stored version to reject stale writes. Stored rows MUST be represented by persistence-only classes that live in the adapter and are converted to and from the core account model by mappers, and core classes MUST carry no storage annotations.
- **FR-010a**: Read-side adapters MUST map stored data directly into the core query result types and MUST NOT introduce an additional intermediate class where the stored shape already matches the result.

**Security**

- **FR-011**: HTTP protection MUST be a stateless resource-server configuration that validates signed access credentials against the configured public key, adapted from the message service's `SecurityConfig`, including cross-origin preflight handling and a public-endpoint allow-list.
- **FR-012**: The public-endpoint allow-list MUST contain at least registration, sign-in, verification request, verification confirmation, session renewal, sign-out by refresh credential, and API documentation. All other endpoints MUST require a valid credential.
- **FR-013**: The old custom authentication filter and access-token verifier MUST NOT be carried over. The old request/response body logging filter MUST NOT be carried over, because it records raw passwords and tokens.
- **FR-014**: Cross-origin permissions MUST come from configuration, not from the placeholder origin used by the old service.

**Structure**

- **FR-015**: Storage adapters MUST be organised by business capability first, then by role (write side or read side), then by storage technology. Adapters that are not storage, such as hashing, identifier generation, mail, and credential issuing, MUST sit directly under their capability without artificial read/write folders. This follows `identity-access-next/infrastructure-structure-design.txt`.
- **FR-016**: Configuration shared by a storage technology (connections, serialization, migrations) MUST be separate from the capability-specific adapters that use it. Capability-specific keys, mapping, expiry, and lookup behaviour MUST stay with that adapter.
- **FR-017**: Old catch-all `service`, `helper`, and `config` folders MUST NOT be recreated. Each item carried over MUST be placed by capability, with only genuinely cross-cutting pieces (key loading, security, per-technology connection setup) kept in dedicated security and integration folders.
- **FR-018**: Redis-backed adapters MUST be placed by role, not by lifetime. Where the key-value store is the source of truth (verification tokens, sessions) it sits under the write or read side directly. Where it only holds an optional copy of data owned by the relational store, it sits under a read-side `cache` folder.
- **FR-019**: An automated structure check MUST fail the build if core imports any infrastructure type or any storage, token, mail, or HTTP library type, or if an adapter sits outside the documented location.

**Shared database stacks**

- **FR-020**: `infrastructure/components` MUST gain one relational-database component and one key-value-store component. Each MUST include a container definition, a README with start/stop/connect instructions, and persistent local data, following the layout used by the existing components.
- **FR-021**: Both stacks MUST accept the development credentials `admin` / `123456`, join the shared `chatter-net` network, expose documented host ports that do not collide with existing components, and not enable change-data-capture or replication settings.
- **FR-022**: The relational component MUST create one shared database on first start, and the identity-access service MUST own a dedicated schema in it, holding its tables and its own migration history, with no objects in other services' schemas or in the default schema. Key-value entries written by this service MUST carry a service-specific key prefix so other services can share the same store. The service's default connection settings MUST point at the shared stacks so a developer needs no extra configuration.
- **FR-023**: `infrastructure/AGENTS.md` MUST list the new components so future work is routed correctly.

**Explicit exclusions**

- **FR-024**: The following old-service items MUST NOT be carried over: outbox, change-data-capture engine, Kafka routing, in-process event queue and listeners, scheduled outbox and session cleanup, secrets-vault credential rotation, reversible refresh-token encryption, and the infrastructure-level global exception handler (HTTP errors belong to the presentation module).
- **FR-025**: Other services' own database definitions and their configuration MUST NOT be modified by this feature.

### Key Entities

- **Outbound adapter**: An infrastructure class implementing exactly one core outbound port, placed by capability, role, and technology.
- **Shared database stack**: A runnable, project-wide instance of one database type, with development credentials and persistent data, shared by all services. Services are separated by schema (relational) or key prefix (key-value), not by separate databases.
- **Signing key pair**: The asymmetric keys, supplied through configuration, used to sign access credentials and validate incoming ones.
- **Stored account**: The persisted representation of a user account, including password hash, salt, algorithm label, status, and version. It is a persistence-only class owned by the adapter, separate from the core account model.
- **Stored verification token**: A digest of a verification token with its account, email, expiry, and used/invalidated state.
- **Stored session**: A digest of a refresh credential with session identifier, account, device information, issue/expiry/last-use/revocation times, kept for the retention period.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer with a clean checkout can start both shared stacks and the service, and complete register → verify → sign in → renew → list sessions → sign out in under 10 minutes using only the README instructions.
- **SC-002**: 100% of outbound ports declared in core have exactly one implementation at the location predicted by the documented convention.
- **SC-003**: After a service restart, 100% of accounts, unexpired verification tokens, and retained sessions created before the restart are still found with unchanged data.
- **SC-004**: In end-to-end tests, 0 plain passwords, raw verification tokens, or raw refresh credentials appear in stored data or log output, and 100% of protected-endpoint requests with a missing, expired, tampered, or foreign-key credential are rejected.
- **SC-005**: Generating 100,000 account identifiers across 8 concurrent callers produces 0 duplicates.
- **SC-006**: The structure check fails on a deliberately introduced violation (core importing an adapter type, or an adapter in the wrong place) and passes on the delivered code.
- **SC-007**: Each shared stack starts from a clean checkout with one documented command and accepts the `admin` credentials, and existing components start exactly as before.
- **SC-009**: After the service starts against an empty shared database, 100% of its tables and its migration history sit in the identity-access schema, and a second service schema added to the same database migrates without conflict.
- **SC-010**: For every persisted model, a round trip (core model → stored form → core model) in tests returns a model equal to the original, and for the account lookup path the mapping adds less than 1% to the measured time of a lookup against the real database.
- **SC-008**: The infrastructure module contains zero references to messaging, outbox, change-data-capture, or secrets-vault credential rotation.

## Assumptions

- Core ports from specs 001 and 002 are the contract. This feature adds adapters and does not change core behavior. If an adapter cannot implement a port as declared, the port is amended in a separate core change.
- The persistence structure recommended here is the one already agreed in `infrastructure-structure-design.txt`: capability → role (write/read) → technology. It is chosen over technology-first (old layout) because that layout scatters one capability across folders and hides the read/write split, and over role-first because it scatters one capability across the tree. Read/write separation here is logical (CQS), not separate databases.
- Sessions and email-verification tokens are stored in the key-value store as the source of truth, as in the design document. Accounts are stored in the relational database.
- Session retention duration, access-credential lifetime, and verification-token lifetime are configuration values with sensible development defaults. Their exact numbers are decided during planning.
- Signing keys are supplied through configuration properties. The secrets vault may populate them, but vault-managed credential rotation is dropped because the datasource now uses fixed development credentials.
- Development credentials are committed deliberately. This personal project does not need production-grade secret handling.
- The old service's database containers and their data are not migrated and not removed. Other services keep using theirs until separately migrated.
- Host ports for the new stacks are chosen during planning from the project's existing port ranges and documented in each README.
- Presentation endpoints and request/response shapes for verification, sign-in, renewal, session listing, and sign-out are outside this feature unless needed for a public-endpoint allow-list entry.
