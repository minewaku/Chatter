# Research: Unify Email Ownership Proof

All items below were resolved from the spec and the existing code. No open `NEEDS CLARIFICATION` remains.

## 1. How close the two copies already are

- **Finding**: `EmailVerification` and `PendingEmailChange` have the same five data fields (`rawToken`, `userId`, an email, `createdAt`, `expiresAt`) and one timestamp for redemption (`verifiedAt` and `confirmedAt`). Their validation, `issue`, `reconstitute`, `requirePositive`, `isExpired` and the redeem method are line for line the same. The Redis side is the same: `VerificationRecord` and `PendingEmailChangeRecord` write identical hash fields (the email-change record even stores its confirmed time under `verifiedAt` so the shared invalidate script works), and both repositories call the same two Lua scripts, differing only in the key prefix (`verification:` and `email-change:`).
- **Consequence**: The merge is mostly deletion. Nothing in storage needs to move.

## 2. Where the purpose is kept

- **Decision**: The purpose is a required field of the model and a required argument of `find` and `invalidateForUser`. In Redis it is carried by the key family, not by a hash field.
- **Rationale**: A purpose field inside one shared key family would need every lookup to remember to compare it, which is the check spec 005 rejected. With the key family chosen from the purpose, a token issued for one purpose simply does not exist in the other purpose's keys, so a wrong-purpose lookup returns nothing without any comparison to forget. Making the purpose a parameter means no call compiles without stating it (spec FR-007).
- **Alternatives considered**: A `purpose` hash field plus a check on read (a forgotten check is a vulnerability; the invalidate script would also have to filter by purpose); one key family with the purpose in the digest (opaque, and changes where existing tokens live); keeping two repository interfaces behind a marker type (keeps the duplication this feature exists to remove).

## 3. Keep the existing storage layout

- **Decision**: Registration proofs keep `identity-access:verification:token:<sha256>` and `...:user:<id>`. Email-change proofs keep `identity-access:email-change:token:<sha256>` and `...:user:<id>`. The Lua scripts, hash fields, and time encoding (epoch microseconds, expiry in epoch millis) are untouched.
- **Rationale**: Spec FR-015 and SC-005 require links already mailed to keep working. Because the layout does not change, there is nothing to migrate or dual-read. The email-change layout is also kept even though no environment holds email-change proofs yet (spec 005 is unreleased), because keeping it costs nothing and avoids a second layout decision.
- **Alternatives considered**: One new combined key family with a dual-read fallback for old registration keys (extra code and a temporary state for no benefit); renaming the prefixes for symmetry (would invalidate links already sent).

## 4. Who owns issuing, and where the lifetime lives

- **Decision**: Add `EmailVerificationIssuer.issue(purpose, userId, email)`. It invalidates the user's earlier proof of that purpose, generates the token, builds the proof with the single 24 hour lifetime, saves it, and returns it. It does not send mail. `EmailVerificationIssuanceCoordinator` becomes "issuer, then `VerificationNotifier`" for registration only. `RequestEmailChangeService` calls the issuer with `EMAIL_CHANGE`, then its own `EmailChangeNotifier`, and loses its own `LIFETIME` constant.
- **Rationale**: The lifetime and the invalidate-then-store order are rules of the proof, so they belong in one place (spec FR-001, FR-003). Mail differs by purpose (wording, link, subject), so the notifier call stays with each flow (spec FR-012 and the out-of-scope note on mail). The coordinator's public constructor changes shape, which is a wiring edit in its tests and nowhere else, since callers use `issue(userId, email)`.
- **Alternatives considered**: Pass the notifier into the issuer by purpose (couples the proof to mail and forces the two mail ports to merge, which the spec leaves alone); leave the coordinator as is and add a second coordinator (a second copy of the same sequence).

## 5. What stays out of the shared mechanism: redeeming

- **Decision**: The two confirm services keep their own logic and use the shared repository plus the model methods `isExpired`, `isVerified`, `verify`. There is no shared "redeem" service.
- **Rationale**: Their order and effects differ on purpose. Registration verifies the proof, activates the account, then saves proof and account. Email change checks the account, re-checks that the address is free, applies the email, saves the account, and only then records the proof as used so that a crash between the two steps is retried safely ("an earlier attempt applied the change but did not get to record it"). Forcing one redeem routine would either change one of those orders or make the routine a callback shell around the same two services.
- **Alternatives considered**: A generic `redeem(purpose, token, effect)` taking a lambda (hides the ordering that makes the email-change retry safe; no second caller would benefit).

## 6. Naming: keep `EmailVerification`, rename the email-change vocabulary

- **Decision**: The shared model stays `EmailVerification` in `emailverification.internal.model`, with a new `purpose` field and a new `VerificationPurpose` enum beside it. In email-change code, `newEmail` becomes `email`, `confirm` becomes `verify`, `isConfirmed` becomes `isVerified`. The HTTP-facing words (`confirmed`, `already_confirmed`) in `ConfirmEmailChangeUseCase` stay.
- **Rationale**: A rename to something like `OwnershipProof` would touch every registration class and test for no behavior gain. The word "verification" already means "proof of mailbox ownership" in this module and in the spec's own language. The public result names are part of the HTTP contract through the presentation layer and must not move.
- **Alternatives considered**: Rename to `EmailOwnershipToken` (large diff across the registration flow, and the spec asks for no observable change); keep `confirmedAt` as an alias (leaves two words for one thing).

## 7. How `user` reaches the proof

- **Decision**: `RequestEmailChangeService`, `ConfirmEmailChangeService` and `SoftDeleteUserAccountService` import `EmailVerificationIssuer`, `EmailVerificationRepository`, `EmailVerification` and `VerificationPurpose` from `emailverification.internal` directly. No facade class is added.
- **Rationale**: `RegisterUserService` already imports `emailverification.internal.application.EmailVerificationIssuanceCoordinator`, and `VerifyEmailService` already imports `user.internal` types, so the packages already call into each other's internals and no ArchUnit rule in the repository forbids it (the structure rules only constrain core against infrastructure). A facade that forwards `issue`, `find`, `invalidate` and `save` would add a class per method and no safety. My earlier statement that `user` cannot reach `emailverification.internal` was wrong about the codebase; spec 005's own research (section 5) repeated the same assumption, and this feature corrects it.
- **Alternatives considered**: A public `emailverification.api` package with a facade (the right move if a boundary rule is introduced later, and cheap to add then because callers are three classes); moving the model and port to `shared` (the model needs `user.internal` types `UserId` and `Email`, so it cannot sit lower than `user`).

## 8. Infrastructure: one repository, key family from the purpose

- **Decision**: `RedisEmailVerificationRepository` keeps one pair of Lua script constants and computes the token and user key prefixes from the purpose in a private `switch` over the enum. The `switch` has no default branch, so adding a purpose later fails to compile until its key family is stated. `VerificationRecordMapper.toModel` takes the purpose; `toRecord` reads it from the model. `PendingEmailChangeRecord`, `PendingEmailChangeRecordMapper`, `RedisPendingEmailChangeRepository` and the `emailchange` adapter folder are deleted.
- **Rationale**: Spec FR-009: adding a third purpose must not change how the existing two are stored or found. One new enum constant and one new `switch` arm do that. `VerificationRecord` stays package-private and unchanged.
- **Alternatives considered**: A `Map<VerificationPurpose, String>` filled at construction (a missing entry fails at runtime instead of compile time); two repository beans from one class with different prefixes (two beans for one port breaks the "exactly one implementation" rule in `StructureRules`).

## 9. Structure rules and tests

- **Decision**: Remove the `PendingEmailChangeRepository` row from `StructureRules.PORT_MAP`. `assertPortMap` fails if a row names a missing port, and `assertEveryCorePortIsMapped` fails if a port is missing a row, so the two edits must land together.
- **Test plan**: `PendingEmailChangeTest` cases fold into `EmailVerificationTest` and run for both purposes (a model-level case is the same either way). `RedisPendingEmailChangeRepositoryTest` (185 lines) and `PendingEmailChangeRecordMapperTest` fold into `RedisEmailVerificationRepositoryTest` and `VerificationRecordMapperTest`, which run each case per purpose and add the cross-purpose cases: a token saved under one purpose is not found, and not invalidated, under the other; invalidating one purpose leaves the other's proof for the same user. `EmailChangeApplicationServiceTest` and `UserAccountApplicationServiceTest` swap their in-memory pending-change fake for the in-memory `EmailVerificationRepository` fake from `EmailVerificationTestFakes`, which becomes public for that reuse. The existing assertion that the email-change lifetime equals the registration lifetime is replaced by an `EmailVerificationIssuerTest` assertion that both purposes get the same lifetime from the same code.

## 10. Redeem is not atomic today, and stays that way

- **Finding**: Both confirm flows read the proof, check it, change the account, then write the proof back, with no lock and no version check on the account save. Two requests at the same instant can both pass the check. The outcome is still the same final state, because both apply the same change (activate the same account, or set the same email), but both callers may be told "verified" instead of one hearing "already verified".
- **Decision**: Do not add an atomic redeem step. This is a restructuring, so the behavior stays as it is. The spec edge case is worded to match (end state identical, no double effect), not to promise a single winner.
- **Alternatives considered**: A Lua redeem-once script (a real hardening, but a behavior change and a new script; worth a separate spec if wanted).

## 11. Spec 005 documents that this supersedes

- **Decision**: Add a one-line "superseded by spec 006" note to spec 005's `research.md` (section 4 on where the pending change is stored, and the package-boundary claim in section 5), `data-model.md` (the `PendingEmailChange` entity), and `plan.md` ("Structure Decision"). The notes point to this feature; they do not rewrite 005, whose HTTP contract and behavior stay correct.
- **Rationale**: Earlier specs must stay accurate, and a reader of 005 would otherwise look for a `PendingEmailChange` class that no longer exists.

## 12. Rollout and compatibility

- **Decision**: No deployment ordering, flag, or migration. Old and new code read and write the same keys with the same fields. An older instance running during a rolling deploy still finds registration proofs the new code wrote, and the reverse.
- **Rationale**: Spec FR-015 and SC-005, given the unchanged layout from section 3.
