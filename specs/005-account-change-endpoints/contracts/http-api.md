# HTTP Contract: Account Change Endpoints

Base path `/api/v1`. Errors use the existing body `{status, code, message, timestamp, errors?}` ([spec 004 error model](../../004-presentation-api/contracts/error-model.md)). "Protected" means a valid access credential; "public" means the `Authorization` header is ignored.

| # | Endpoint | Access | Success | Use case |
|---|----------|--------|---------|----------|
| 1 | `POST /users/@me/close` | protected | `204` no body | `SoftDeleteUserAccountUseCase` |
| 2 | `PUT /users/@me/username` | protected | `204` no body | `ChangeUsernameUseCase` |
| 3 | `POST /users/@me/email-change` | protected | `202` no body | `RequestEmailChangeUseCase` |
| 4 | `GET /users/email-change/confirm?token=…` | public | `200` `{"status": "confirmed" \| "already_confirmed"}` | `ConfirmEmailChangeUseCase` |

`DELETE /users/@me` no longer exists; no handler matches that path, so it answers `404 not_found`.

## 1. Close account

Request: `{"currentPassword": "..."}`

| Outcome | Status | Code |
|---------|--------|------|
| Closed (all sessions ended) | `204` | |
| Missing, blank, or oversize password | `400` | `validation_failed` |
| Unreadable body | `400` | `malformed_request` |
| Missing, invalid, expired, or revoked credential | `401` | `unauthenticated` |
| Wrong password, including one that could never meet the password policy (session stays valid) | `403` | `reauthentication_failed` |
| Revocation store unreadable | `503` | `service_unavailable` |

## 2. Change username

Request: `{"username": "new_name"}`

| Outcome | Status | Code |
|---------|--------|------|
| Changed | `204` | |
| Blank or over-long username | `400` | `validation_failed` with `errors[{field: "username"}]` |
| Username rejected by the registration rules in core (for example a leading dot) | `400` | `validation_failed`, generic message, no `errors` list |
| Missing credential | `401` | `unauthenticated` |
| Account not active | `403` | `account_inaccessible` |
| Same as the current username | `409` | `username_unchanged` |
| Used by another account | `409` | `account_already_exists` |

## 3. Request email change

Request: `{"newEmail": "new@example.com", "currentPassword": "..."}`

| Outcome | Status | Code |
|---------|--------|------|
| Accepted; verification mail sent to the new address; account email unchanged | `202` | |
| Malformed or over-long email, or blank password | `400` | `validation_failed` with `errors[{field}]` |
| Email rejected by the registration rules in core | `400` | `validation_failed`, generic message, no `errors` list |
| Missing credential | `401` | `unauthenticated` |
| Wrong password, including one that could never meet the password policy (session stays valid, nothing stored or sent) | `403` | `reauthentication_failed` |
| Account not active | `403` | `account_inaccessible` |
| Same as the current email (nothing stored, no mail) | `409` | `email_unchanged` |
| Used by another account (nothing stored, no mail) | `409` | `account_already_exists` |

A second accepted request replaces the pending one; the earlier link stops working.

## 4. Confirm email change

The link in the mail: `{public-base-url}/api/v1/users/email-change/confirm?token=<token>`. Response bodies carry `Cache-Control: no-store`.

| Outcome | Status | Code |
|---------|--------|------|
| New email now in effect | `200` | body `status: confirmed` |
| Token already used | `200` | body `status: already_confirmed` |
| Blank, unknown, replaced, discarded, expired, or wrong-purpose token; account no longer active | `400` | `invalid_or_expired_token` |
| Another account took the address meanwhile (pending change dropped) | `409` | `account_already_exists` |

## OpenAPI

All four are documented with `@Operation`, `@ApiResponse`, and `@DocumentedErrors`; password fields carry `Schema.AccessMode.WRITE_ONLY`. Tags: endpoints 1–3 under "Account", endpoint 4 under "Account" with no security requirement.

## New error codes

| Code | Status | Message |
|------|--------|---------|
| `username_unchanged` | `409` | "The username was not changed." |
| `email_unchanged` | `409` | "The email was not changed." |
