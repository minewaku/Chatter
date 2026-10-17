# Implementation Plan: Identity Access Presentation API

**Branch**: `master` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-presentation-api/spec.md`

## Summary

Expose the identity-access core use cases over HTTP from `identityaccess-presentation`: 11 JSON endpoints (6 public, 5 protected), one structured error body with stable codes, device labels parsed from `User-Agent`, and OpenAPI documentation. Two behaviors the spec adds need small changes below the controllers: **refresh-token rotation** (core use case, session port, one new Redis script) and **immediate access-credential revocation** (a `sid` claim on the access token, short-lived per-session revocation entries written by the same Redis scripts that revoke sessions, and a servlet filter that fails closed). Presentation depends only on core's public contracts; the security chain in infrastructure gets its error responses from beans that presentation supplies, so the two modules never import each other. Decisions and rationale: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (Spring Framework 7, Spring Security 7)

**Primary Dependencies**: Presentation adds `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` (for entry-point and bearer-token types), `springdoc-openapi-starter-webmvc-ui` 3.1.1, `nl.basjes.parse.useragent:yauaa` 8.2.0; keeps `webmvc` and `validation`. No new infrastructure dependency.

**Storage**: Redis 7 (session store and the new revocation entries, existing key prefix `identity-access:`); PostgreSQL unchanged

**Testing**: JUnit 5 + AssertJ; MockMvc slice tests per controller with mocked use cases; ArchUnit for presentation boundaries; Testcontainers (`postgres:17`, `redis:7`) end-to-end suite in `identityaccess-start`; existing Redis adapter contract tests extended for rotation and revocation entries; Spotless `check`

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend; fills `identityaccess-presentation`, touches `identityaccess-core` (small), `identityaccess-infrastructure` (small), and `identityaccess-start` (tests, configuration)

**Performance Goals**: Revocation check adds one Redis round trip per protected request (two `GET`s pipelined into one `MGET`); the device-label parser is built once at start-up and adds under 1 ms per sign-in after warm-up

**Constraints**: Core and presentation never log or echo passwords, verification tokens, or refresh credentials; presentation imports no infrastructure type and no core `internal` type; public endpoints ignore `Authorization`; revocation store failure fails closed with `503`

**Scale/Scope**: 11 endpoints, about 25 production classes in presentation, 4 core classes touched plus 4 relocated exceptions, 5 infrastructure classes touched or added, 1 new Lua script, 2 extended Lua scripts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, `infrastructure-structure-design.txt`, and the approved spec (same basis as spec 003).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits are limited to `identity-access-next/{core,infrastructure,presentation,start}` and `specs/` |
| Core owns contracts; presentation and infrastructure own technology | Pass | Pass: core changes are contract-level (rotation result, session id on token issue, relocated exceptions) with no HTTP, Redis, or JWT type |
| Presentation depends only on core's public contracts | Pass | Pass: the four exceptions it must map move to core `api` packages; an ArchUnit rule enforces it (FR-026) |
| HTTP errors belong to presentation (spec 003 FR-024) | Pass | Pass: infrastructure's chain delegates 401, 403, and 503 bodies to beans presentation provides |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: no body logging, handler logs only code and exception class, sentinel end-to-end test |
| Earlier specs stay accurate | Needs work | Pass: specs 002 and 003 contract docs are updated as tasks (see [core-and-infra-changes.md](contracts/core-and-infra-changes.md)) |
| Git/patch paths resolved against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories |

No violations. Complexity Tracking lists the one deliberate exception.

## Project Structure

### Documentation (this feature)

```text
specs/004-presentation-api/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── http-api.md
│   ├── error-model.md
│   └── core-and-infra-changes.md
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (repository root: `services/identity-access-next/`)

```text
identityaccess-core/                                   # small contract changes
├── src/main/java/.../authentication/api/exception/    # AuthenticationFailedException (moved from internal)
├── src/main/java/.../user/api/exception/              # InvalidCredentialsException, DuplicateUserAccountException,
│                                                      #   InvalidAccountStateTransitionException (moved from internal)
├── src/main/java/.../authentication/api/command/RefreshAccessTokenUseCase.java   # Refreshed gains refreshToken
├── src/main/java/.../authentication/internal/port/AccessTokenIssuer.java         # issue(userId, email, sessionId)
├── src/main/java/.../authentication/internal/port/repository/AuthenticationSessionRepository.java  # rotate replaces touchIfUsable
├── src/main/java/.../authentication/internal/application/command/{AuthenticateService,RefreshAccessTokenService}.java
└── src/test/.../AuthenticationTestFakes.java and affected service tests

identityaccess-infrastructure/                         # small adapter and security changes
├── src/main/java/.../adapter/outbound/authentication/JwtAccessTokenIssuer.java   # adds sid claim
├── src/main/java/.../adapter/outbound/session/write/redis/RedisAuthenticationSessionRepository.java  # rotate + revocation entries
├── src/main/java/.../security/RevokedAccessTokenFilter.java          # new: fail-closed revocation check
├── src/main/java/.../security/PublicEndpointBearerTokenResolver.java # new: ignore Authorization on public endpoints
├── src/main/java/.../security/SecurityConfiguration.java            # wires the above and optional entry-point beans
├── src/main/resources/redis/{session-rotate.lua (new), session-revoke-one.lua, session-revoke-many.lua}
└── src/test/...                                                      # contract and security tests extended

identityaccess-presentation/
├── pom.xml
└── src/main/java/com/minewaku/chatter/identityaccess/presentation/
    ├── authentication/    # AuthController, request/response records, DeviceLabelResolver
    ├── session/           # SessionController, SessionResponse, request records
    ├── user/              # UserController, ChangePasswordRequest
    └── shared/
        ├── error/         # ApiError, ErrorCode, ApiExceptionHandler, ApiAuthenticationEntryPoint, ApiAccessDeniedHandler
        ├── security/      # AccessPrincipal (userId + sessionId from Jwt)
        └── openapi/       # OpenApiConfiguration
    src/test/java/.../presentation/
    ├── authentication/ session/ user/     # MockMvc slice tests
    ├── shared/error/                      # error-shape and mapping tests
    └── structure/                         # ArchUnit boundary rules

identityaccess-start/
├── src/main/resources/application.yml     # springdoc and device-label settings only
└── src/test/java/.../start/               # Testcontainers end-to-end suite (new)
```

**Structure Decision**: Presentation is organised by business capability (`authentication`, `session`, `user`) with one `shared` package for cross-cutting error, security, and OpenAPI pieces, mirroring the capability-first rule used by infrastructure (spec 003 FR-015). The old `presentation/user/` stubs and `.local/` key files are removed or moved into the new packages. Controllers are not split into `command`/`query` folders because each controller serves one URL family.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Revocation entries are written inside the session repository's Lua scripts, so a "session store" adapter also owns the access-credential deny list | Revocation and the deny entry must happen in one atomic step on every revocation path (FR-031); a separate write after the script leaves a gap and a path that can forget it | A separate `AccessCredentialRevocations` port called by core services would need every current and future revoke path to remember to call it, and cannot be atomic with the session write |
