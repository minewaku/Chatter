# Quickstart: Validating the Unified Email Ownership Proof

Working directory for all build commands: `services/identity-access-next`. Use the per-service `./mvnw`, never a bare `mvn`.

Because this is a restructuring, validation is mostly "the existing suites still pass with the same expected outcomes", plus a few new cases for the shared mechanism.

## Automated checks

```bash
./mvnw -pl identityaccess-core -am verify               # model, issuer, registration and email-change services
./mvnw -pl identityaccess-infrastructure -am verify     # Redis repository per purpose, structure rules (port map)
./mvnw verify                                           # everything, including presentation slices and the e2e suite
./mvnw spotless:check                                   # formatting
```

Expected: all green. The presentation tests (`VerificationControllerTest`, `EmailChangeControllerTest`) and the e2e tests (registration, `EmailChangeE2eTest`, `AccountManagementE2eTest`) pass without any edit to their expected outcomes. That is the proof of spec SC-002 and SC-003.

### What the new or changed tests must show

| Spec requirement | Test |
|------------------|------|
| One definition of lifetime, replacement, expiry, single use (FR-001, FR-003 to FR-005, SC-001) | `EmailVerificationIssuerTest`: both purposes get the same 24 hour lifetime from the same code; issuing again replaces the earlier proof of that purpose; `EmailVerificationTest` runs the expiry and single-use cases for both purposes |
| Purpose is required (FR-007) | Compile-time: there is no overload of `issue`, `findByPresentedToken` or `invalidateForUser` without a purpose. A reviewer check, not a test |
| A token never crosses purposes (FR-008, SC-004) | `RedisEmailVerificationRepositoryTest`: a token saved as `REGISTRATION` is not found as `EMAIL_CHANGE`, and the reverse. `EmailChangeApplicationServiceTest`: a registration token presented to the email-change confirm returns `InvalidOrExpired`. `VerifyEmailServiceTest`: an email-change token returns `InvalidOrExpired` |
| Purposes are independent per user (FR-004, User Story 2 scenario 4) | `RedisEmailVerificationRepositoryTest` and the fake repository test: invalidating one purpose leaves the other's proof for the same user |
| Close still discards a pending email change (FR-014, User Story 3) | `UserAccountApplicationServiceTest` |
| Old data stays readable (FR-015, SC-005) | `RedisEmailVerificationRepositoryTest` writes a hash by hand in the existing layout for each purpose and redeems it through the new repository |
| The port map is consistent (FR-017) | `PortAdapterMapTest` (no row for the removed port, no unmapped port) |
| The duplicate is gone (FR-017, SC-006) | `git grep -n PendingEmailChange -- services/identity-access-next` returns nothing under `src/` |

## Manual walkthrough against a running service

Start the shared Docker stacks and the service as for spec 004's quickstart. Set `B=http://localhost:5001/api/v1` and `H='Content-Type: application/json'`. The goal is to see both flows work and to see that the Redis keys are where they always were.

Registration still works:

```bash
curl -i -X POST $B/auth/register -H "$H" -d '{...registration body from spec 004...}'                       # 201
# open the mailed link, or: curl -i "$B/auth/verification/confirm?token=<token from the mail>"             # 200 {"status":"verified"}
curl -i "$B/auth/verification/confirm?token=<same token>"                                                  # 200 {"status":"already_verified"}
redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:verification:*'  # token and user keys
```

Email change still works, in the same key family as before:

```bash
curl -i -X POST $B/users/@me/email-change -H "$H" -H "Authorization: Bearer $AT" -d '{"newEmail":"new@example.com","currentPassword":"S3cure-pass!"}'   # 202
redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:email-change:*'   # token and user keys
curl -i "$B/users/email-change/confirm?token=<token from the mail>"                                        # 200 {"status":"confirmed"}
curl -i "$B/users/email-change/confirm?token=<same token>"                                                 # 200 {"status":"already_confirmed"}
```

Purposes stay apart (spec SC-004):

```bash
curl -i "$B/users/email-change/confirm?token=<a registration token>"                                       # 400 invalid_or_expired_token
curl -i "$B/auth/verification/confirm?token=<an email-change token>"                                       # 400 invalid_or_expired_token
```

Links sent before the release (spec SC-005): register an account on the previous build, leave it unverified, deploy this build, then open the old link. Expect `200 {"status":"verified"}`.

## Review checks

- `git grep -n "24" -- 'services/identity-access-next/identityaccess-core/src/main/**/emailverification/**' 'services/identity-access-next/identityaccess-core/src/main/**/user/**'` shows the 24 hour lifetime in exactly one place (`EmailVerificationIssuer`).
- The change set contains no edit to `identityaccess-presentation/src/main`, `identityaccess-start/src/main`, any `.lua` file, any mail template, or any Flyway migration.
