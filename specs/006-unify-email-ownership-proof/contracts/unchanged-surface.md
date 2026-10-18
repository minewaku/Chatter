# Contract: Surface That Must Not Change

This feature has no external contract of its own. Its requirement is that every contract below is identical before and after. A change to any row is a defect in this work, not a deliverable.

## HTTP

| Contract | Source | How it is checked |
|----------|--------|-------------------|
| `POST /api/v1/auth/register` and its responses | spec 001, 004 `contracts/http-api.md` | Existing `VerificationControllerTest`, registration e2e tests |
| `GET /api/v1/auth/verification/confirm?token=…` → `200 verified` / `already_verified`, `400 invalid_or_expired_token` | spec 004 | `VerificationControllerTest`, e2e |
| `POST /api/v1/auth/verification/resend` → `202`, no account state leaked | spec 004 | `VerificationControllerTest`, e2e |
| `POST /api/v1/users/@me/email-change` → `202`, `403`, `409 account_already_exists`, `409 email_unchanged`, `400 validation_failed` | spec 005 `contracts/http-api.md` | `EmailChangeControllerTest`, `EmailChangeE2eTest` |
| `GET /api/v1/users/email-change/confirm?token=…` → `200 confirmed` / `already_confirmed`, `400 invalid_or_expired_token`, `409 account_already_exists` | spec 005 | `EmailChangeControllerTest`, `EmailChangeE2eTest` |
| `POST /api/v1/users/@me/close` discards a pending email change | spec 005 FR-027 | `AccountManagementE2eTest`, `UserAccountApplicationServiceTest` |

No presentation or `start` main source file is edited. Their test sources are not edited either, apart from any import that stops compiling; none is expected, since the use case interfaces are untouched.

## Core use cases

`VerifyEmailUseCase`, `RequestEmailVerificationUseCase`, `RequestEmailChangeUseCase`, `ConfirmEmailChangeUseCase`, `SoftDeleteUserAccountUseCase`: command and result records are unchanged.

## Mail

`VerificationNotifier` and `EmailChangeNotifier` keep their signatures. `MailVerificationNotifier`, `MailEmailChangeNotifier`, `VerificationLinkFactory`, `VerificationMailProperties`, and the `email-verification.html` and `email-change.html` templates are not edited. Subjects, link paths, and bodies are identical.

## Stored data

| Item | Required state |
|------|----------------|
| Redis key names for both purposes | Identical to today (see [core-and-infra-changes.md](core-and-infra-changes.md)) |
| Hash fields and time encoding | Identical |
| `verification-save.lua`, `verification-invalidate-for-user.lua` | Byte-identical |
| PostgreSQL schema and migrations | No change |

## Configuration

No new or changed property. The 24 hour lifetime stays a constant, as for both flows today.

## Cross-check

A proof written by the code before this feature (registration or email change) must be read, replaced, invalidated, and redeemed by the code after it, and the reverse, because both read and write the same keys and fields. The Redis repository test proves this by writing a hash directly in the old layout and reading it through the new repository.
