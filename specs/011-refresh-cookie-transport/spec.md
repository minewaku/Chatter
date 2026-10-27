# Feature Specification: Refresh Credential as a Cookie

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-27

**Status**: Implemented

**Input**: User description: "The API serves browser clients, so the refresh credential should not be readable by page scripts. Return it as an HttpOnly cookie instead of in the JSON body. A mobile variant can be added later as its own adapter." This reverses the transport decision of spec 004 (FR-004, FR-007, FR-008, FR-009), which put the refresh credential in the JSON body and in the request body. Core is unchanged: `RefreshAccessTokenUseCase` and `LogoutCurrentSessionUseCase` still take and return the credential as a string.

## Decisions

- **D1 Transport.** Sign-in and renewal set the refresh credential in a `Set-Cookie` header and return only `{"accessToken": "..."}` in the body. Renewal and sign-out read the credential from the cookie. A `refreshToken` field in a request body is ignored.
- **D2 Cookie attributes.** `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` equal to the session lifetime. Name `refresh_token`. Name, path, `Secure` and `Max-Age` are configurable under `identity-access.web.refresh-cookie`; `Max-Age` defaults to `identity-access.session.lifetime`. `Secure` is switched off only for plain-HTTP local development.
- **D3 Sign-out clears the cookie.** Logout always answers `204` and sends an expired cookie (`Max-Age=0`, same name and path) whether or not a session was revoked, so the caller still cannot tell if a session existed.
- **D4 Failures set no cookie.** A refused renewal is the same `401 authentication_failed` as before and does not touch the cookie.
- **D5 Cross-site protection.** CSRF protection stays disabled because no endpoint authenticates from a cookie alone except these two, and both are covered by `SameSite=Strict` plus the CORS origin check: Spring's CORS filter answers `403` to any request carrying an `Origin` header that is not in `identity-access.security.cors.allowed-origins`, before the request reaches the controller. CORS now sets `Access-Control-Allow-Credentials: true`; the allowed origins are an explicit list, never a wildcard.
- **D6 Mobile.** A non-browser variant, if needed, is a separate adapter that calls the same use cases and carries the credential in the body. It is out of scope here.

## Requirements

- **FR-001**: `POST /api/v1/auth/authenticate` MUST respond `200` with exactly `{"accessToken": "..."}` and a `Set-Cookie` header per D2. The refresh credential MUST NOT appear in the body.
- **FR-002**: `POST /api/v1/auth/refresh` MUST read the credential from the cookie only, respond `200` with exactly `{"accessToken": "..."}` and a replacement cookie per D2. An absent or blank cookie is the generic `401 authentication_failed`.
- **FR-003**: `POST /api/v1/auth/logout` MUST read the credential from the cookie only and respond `204` with no body and the clearing cookie of D3. An absent or blank cookie revokes nothing and still gets `204`.
- **FR-004**: Neither endpoint accepts or documents a request body. The OpenAPI document MUST describe the cookie as a cookie parameter named `refresh_token`.
- **FR-005**: No log entry or response body contains the refresh credential (spec 004 FR-022 still applies). The credential appears only in `Set-Cookie` response headers and the `Cookie` request header.
- **FR-006**: A call to `/auth/refresh` or `/auth/logout` from an origin outside the allowed list MUST be refused with `403` before it reaches the endpoint.

## Success Criteria

- **SC-001**: In end-to-end tests, the refresh credential is found in 0 response bodies and in `Set-Cookie` headers only.
- **SC-002**: Every `Set-Cookie` header issued carries `HttpOnly`, `Secure`, `SameSite=Strict`, the auth `Path` and the session `Max-Age`.
- **SC-003**: Rotation, reuse detection and sign-out behave exactly as in specs 004 and 009 once the credential is presented as a cookie.

## Changes this feature makes to earlier specs

- Spec 004: FR-004 (cookie transport not carried over), FR-007, FR-008, FR-009, user-story scenarios 1 and 7, the `RefreshRequest`, `LogoutRequest` and `TokenPairResponse` types (replaced by `AccessTokenResponse`), contract sections 2, 5 and 6, and quickstart steps 4 to 6. Each is marked "Superseded by spec 011" in place.
- Spec 002: no change; its contracts describe core, which still deals in strings.
