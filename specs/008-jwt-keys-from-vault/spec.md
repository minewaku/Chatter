# Feature Specification: JWT Keys from Vault and Audience-Free Token Validation

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-20

**Status**: Implemented

**Input**: User description: "make the `identity-access-next` to load jwt keys from vault instead. Also make the service not to validate on the jwt's audience since its redundant right now"

## Clarifications

### Session 2026-10-20

- Q: Where do the signing keys come from today? → A: From locally supplied values (environment variables or a local `.env`), not from the secrets store. The store already holds the shared key pair that the other services read; the identity service is the one that does not read it. Other secrets of the identity service (mail credentials) already come from the store.
- Q: Does "not validate the audience" also mean "stop putting an audience in issued tokens"? → A: No. Only the check on incoming tokens is removed. Tokens keep carrying the audience they carry today, so nothing that reads it elsewhere is affected.
- Q: Is any other check on incoming tokens relaxed? → A: No. Signature, issuer, expiry, and the revoked-session check stay exactly as they are.

### Session 2026-10-21

- Q: When a developer runs the service locally, or a test runs it, with no secrets store available, how does it get its signing keys? → A: It does not run without the store. The secrets store is the only source of signing keys, with no local-keys setting or other local route. Developers run a local store, and automated tests create their own key pair in test setup.
- Q: If old JWT key values are still present in the service's environment or local file, what should the service do? → A: Nothing: they have no effect. The service never reads them, and it does not warn about them or refuse to start because of them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The Service Gets Its Signing Keys from the Secrets Store (Priority: P1)

An operator deploys the identity service. They provide only the credential the service needs to reach the secrets store. They do not copy any JWT key material into the service's environment, local files, or deployment settings. When the service starts, it reads the key pair from the secrets store and uses it to sign the access tokens it issues and to verify the access tokens it receives.

**Why this priority**: This is the point of the feature. Today the private key has to be placed next to the service in a local file or environment variable, which is the exposure the secrets store exists to remove. Everything else in this feature is secondary to getting the keys out of local configuration.

**Independent Test**: Remove every JWT key value from the service's local configuration, start it with only the secrets-store credential, sign in, and call a protected endpoint with the returned token. Both must succeed.

**Acceptance Scenarios**:

1. **Given** a key pair stored in the secrets store and no JWT key value in any local configuration, **When** the service starts, **Then** it starts normally, signs in a valid user, and accepts the token it issued on a protected endpoint.
2. **Given** the same setup, **When** the key pair in the store is replaced by a different valid pair and the service is restarted, **Then** tokens issued before the restart are rejected and tokens issued after it are accepted.
3. **Given** the service has started, **When** its logs, health output, and error responses are inspected, **Then** no key material appears in any of them.
4. **Given** JWT key values are still present in local configuration, **When** the service starts, **Then** it uses the values from the secrets store and the local values have no effect.

---

### User Story 2 - The Service Refuses to Run Without a Usable Key Pair (Priority: P1)

An operator misconfigures something: the secrets store is unreachable, the credential is rejected, the key pair is missing, malformed, or the two halves do not belong together. The service must not come up in a state where it signs with a wrong key, accepts everything, or rejects everything. It stops at start-up and says what is wrong.

**Why this priority**: Moving the keys to a remote store introduces new ways for them to be unavailable. Failing quietly would either issue tokens nobody can verify or leave the service unprotected, which is worse than the problem being fixed.

**Independent Test**: Start the service once for each failure (store unreachable, credential rejected, key missing, key malformed, mismatched pair) and confirm that each start attempt ends with a clear message naming the problem and no running service.

**Acceptance Scenarios**:

1. **Given** the secrets store cannot be reached, **When** the service starts, **Then** start-up fails with a message saying the signing keys could not be loaded from the store.
2. **Given** the store rejects the service's credential, **When** the service starts, **Then** start-up fails and the message distinguishes a rejected credential from an unreachable store.
3. **Given** the store has no key pair at the expected location, or only one half of it, **When** the service starts, **Then** start-up fails and the message names the missing half.
4. **Given** the stored private and public keys do not match each other, or either is not a valid key, **When** the service starts, **Then** start-up fails and the message says which problem was found, without printing the key.
5. **Given** a failed start for any reason above, **When** the operator corrects the cause and starts again, **Then** the service starts with no other change needed.

---

### User Story 3 - Incoming Tokens Are No Longer Rejected for Their Audience (Priority: P2)

A client presents an access token issued by the identity service. The service accepts it if its signature is valid, it was issued by the expected issuer, it has not expired, and its session has not been revoked. Which audience the token names, or whether it names one at all, no longer decides the outcome.

**Why this priority**: The audience check protects against tokens meant for a different system. In this deployment there is one issuer, one key pair, and one audience value for the whole platform, so the check can never reject anything real and only adds a configuration value that has to stay in step across services. Removing it simplifies operation and has no effect on any realistic request, which is why it follows the key change.

**Independent Test**: Present otherwise valid tokens that name a different audience and no audience at all; both are accepted. Present tokens that are expired, wrongly signed, from another issuer, or from a revoked session; all are still rejected.

**Acceptance Scenarios**:

1. **Given** a token with a valid signature, expected issuer, future expiry and live session, **When** it names an audience other than the platform's, **Then** the request is accepted.
2. **Given** the same token with no audience at all, **When** it is presented, **Then** the request is accepted.
3. **Given** a token that is expired, **When** it is presented, **Then** the request is rejected exactly as today.
4. **Given** a token with an invalid signature, or from an unexpected issuer, **When** it is presented, **Then** the request is rejected exactly as today.
5. **Given** a token whose session was revoked, **When** it is presented, **Then** the request is rejected exactly as today.
6. **Given** a freshly issued token, **When** its contents are inspected, **Then** it still names the same audience as before this feature.
7. **Given** the service's configuration, **When** an operator reviews what must be set for token validation, **Then** no audience value is required.

---

### Edge Cases

- The store is reachable at start-up but becomes unreachable later: the service keeps running with the keys it already loaded. Keys are read once at start-up, not on each request.
- The store holds the keys with surrounding whitespace, line breaks, or the usual begin/end lines: the same formats accepted today are still accepted.
- The service is started for local development where no store is available: it does not start. A developer runs a local store holding a key pair, the same way as for the mail credentials.
- Automated tests of the service: they create their own key pair in test setup and do not depend on the real store.
- Old JWT key values are left in the environment or a local file: they are never read and have no effect, whether or not the store is available.
- Other services that verify tokens with the shared public key: they continue to accept tokens from the identity service, because the key pair they use is the one the identity service now reads.
- A token issued under one key pair presented after the pair is rotated and the service restarted: it is rejected as an invalid signature, which is the expected outcome of rotation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The service MUST obtain its token signing private key and its verification public key from the secrets store at start-up.
- **FR-002**: The service MUST read the same shared key pair that the other services already use to verify its tokens, so tokens it issues remain verifiable by them without any change to them.
- **FR-003**: The service MUST NOT require any JWT key value in its environment, local configuration files, or deployment settings.
- **FR-004**: The service MUST ignore any JWT key values supplied locally. They MUST have no effect on which keys it uses, including when the secrets store is unavailable.
- **FR-005**: The service MUST NOT start if the secrets store is unreachable, the service's credential is rejected, either key is missing, either key is not valid, or the two keys do not form a matching pair.
- **FR-006**: Each start-up failure in FR-005 MUST produce a message that identifies which of the conditions occurred, so an operator can tell them apart.
- **FR-007**: Key material MUST NOT appear in logs, health output, error responses, or start-up failure messages.
- **FR-008**: The service MUST accept the same key formats it accepts today.
- **FR-009**: The service MUST load the keys once at start-up and MUST keep working with them if the store later becomes unavailable.
- **FR-010**: The service MUST NOT reject an incoming token because of the audience it names, or because it names none.
- **FR-011**: The service MUST continue to reject incoming tokens with an invalid signature, an unexpected issuer, an expired lifetime, or a revoked session, with the same outcomes as before.
- **FR-012**: The service MUST continue to include the platform audience in the tokens it issues, unchanged.
- **FR-013**: The service MUST NOT require an audience value to be configured for validating incoming tokens.
- **FR-014**: The service MUST NOT provide any setting or route for supplying signing keys other than the secrets store.

### Key Entities

- **Signing Key Pair**: The private key used to sign access tokens and the matching public key used to verify them. Stored once, shared by every service that verifies the platform's tokens, and read by the identity service only at start-up.
- **Access Token**: The credential the service issues and later receives. Carries issuer, expiry, session, and audience claims. After this feature only issuer, expiry, session and signature decide whether an incoming one is accepted.
- **Secrets Store Credential**: The only secret an operator supplies to the service locally; it grants the service read access to what it needs from the store.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A deployed instance runs with zero JWT key values present in its environment, local configuration files, or deployment settings, and signs in a user and serves a protected request successfully.
- **SC-002**: Every one of the five misconfiguration cases (store unreachable, credential rejected, key missing, key invalid, pair mismatched) ends in a refused start with a message that names the case, in 100% of attempts, and no key content appears in any output.
- **SC-003**: 100% of tokens issued by the service are accepted by each other service that verifies tokens with the shared public key, before and after this feature.
- **SC-004**: A token that is otherwise valid is accepted whether it names the platform audience, a different audience, or none, in 100% of attempts.
- **SC-005**: Tokens that are expired, wrongly signed, from another issuer, or from a revoked session are rejected in 100% of attempts, as before.
- **SC-006**: An operator can set up a new environment for the service with one fewer secret value to copy by hand than today (the key pair), and no audience value to keep consistent.
- **SC-007**: A start with locally supplied key values and an unavailable store is refused in 100% of attempts, and a start with both is unaffected by the local values in 100% of attempts.
- **SC-008**: All existing sign-in, session, and protected-endpoint tests pass after the change, apart from tests that exist only to assert the audience rejection being removed.

## Assumptions

- The key pair is already stored in the secrets store at the shared location that the other services read, and the existing setup script that populates it stays the way it gets there. Creating or rotating keys is out of scope.
- Reading the keys once at start-up is enough. Rotating keys without restarting the service is out of scope.
- The identity service reads the full shared pair, including the private key. Splitting the stored private and public halves so that verifying-only services cannot read the private key, and giving each service its own store credential and access policy, is a worthwhile hardening but is out of scope here and should be tracked separately.
- Removing the audience check applies to the identity service only. Whether the other services should validate issuer or audience is out of scope.
- The audience claim in issued tokens stays, so changing it is not part of this feature.
- The credential for reaching the secrets store continues to be supplied locally, as it is for the mail credentials today. Reducing that dependency is out of scope.
- A start-up failure when the store is unavailable is acceptable for this service. The existing behaviour that tolerates an unreachable store for optional secrets such as mail credentials is not changed by this feature.
