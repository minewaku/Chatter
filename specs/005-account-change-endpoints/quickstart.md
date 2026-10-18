# Quickstart: Validating the Account Change Endpoints

Working directory for all build commands: `services/identity-access-next`. Use the per-service `./mvnw`, never a bare `mvn`.

## Automated checks (no real mailbox needed)

```bash
./mvnw -pl identityaccess-core -am verify               # use cases, model, fakes
./mvnw -pl identityaccess-infrastructure -am verify     # Redis repository contract, mail notifier, structure rules
./mvnw -pl identityaccess-presentation -am verify       # MockMvc slices, ArchUnit, secret-handling
./mvnw verify                                           # everything, including the end-to-end suite in identityaccess-start
```

Expected: all green, including `StructureRules` (two new port rows) and `SecretHandlingTest` (new request records print nothing). The end-to-end suite captures outgoing mail in memory (`RecordingMailSender`) and reads the token from it.

## Manual walkthrough against a running service

Start the shared Docker stacks and the service as for spec 004's quickstart. Set `B=http://localhost:5001/api/v1`, `H='Content-Type: application/json'`, and `AT` to an access credential from sign-in.

Close needs the password and a wrong one keeps the session:

```bash
curl -i -X POST $B/users/@me/close -H "$H" -H "Authorization: Bearer $AT" -d '{"currentPassword":"wrong"}'        # 403 reauthentication_failed
curl -i $B/sessions -H "Authorization: Bearer $AT"                                                               # 200, still signed in
curl -i -X POST $B/users/@me/close -H "$H" -H "Authorization: Bearer $AT" -d '{}'                                # 400 validation_failed
curl -i -X DELETE $B/users/@me -H "Authorization: Bearer $AT"                                                    # 404 not_found
```

Username needs no password:

```bash
curl -i -X PUT $B/users/@me/username -H "$H" -H "Authorization: Bearer $AT" -d '{"username":"new_name"}'          # 204
curl -i -X PUT $B/users/@me/username -H "$H" -H "Authorization: Bearer $AT" -d '{"username":"new_name"}'          # 409 username_unchanged
```

Email change, old address stays until confirmed:

```bash
curl -i -X POST $B/users/@me/email-change -H "$H" -H "Authorization: Bearer $AT" -d '{"newEmail":"new@example.com","currentPassword":"wrong"}'     # 403
curl -i -X POST $B/users/@me/email-change -H "$H" -H "Authorization: Bearer $AT" -d '{"newEmail":"new@example.com","currentPassword":"S3cure-pass!"}' # 202
# sign in with the OLD email -> 200; with new@example.com -> 401
curl -i "$B/users/email-change/confirm?token=<token from the mail>"          # 200 {"status":"confirmed"}
curl -i "$B/users/email-change/confirm?token=<same token>"                   # 200 {"status":"already_confirmed"}
# sign in with new@example.com -> 200; with the OLD email -> 401
redis-cli --user admin --pass 123456 --no-auth-warning --scan --pattern 'identity-access:email-change:*'          # token and user keys, gone at expiry
```

Replacement and conflict: request a second change before confirming and check the first link now answers `400 invalid_or_expired_token`. Register an account with the pending address before confirming and check the confirm answers `409 account_already_exists`.

## Real delivery (manual, once)

Automated tests stop at the mail sender, so whether a real provider delivers is checked by the service owner by hand: point the `IDENTITY_ACCESS_MAIL_*` settings (see the owner's `email.txt`, not committed) at the real SMTP account, request an email change to a mailbox you can open, and confirm the message arrives with a working link. Nothing in the repository or the test suite reads that mailbox.
