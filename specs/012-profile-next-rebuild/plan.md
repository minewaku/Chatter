# Implementation Plan: Profile Service Rebuild (profile-next)

**Branch**: `profile-next` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/012-profile-next-rebuild/spec.md`

## Summary

`profile-next` is a Spring Boot 4 service with the four-module layout of `identity-access`. It answers profile reads with defaults when nothing was saved, saves display name and bio through one atomic upsert (so the first save creates the profile and concurrent saves cannot mix), hands out Cloudinary signed-upload permissions and applies the provider's webhook confirmation, and deletes a user's profile and images when `identity-access` announces the account deletion on Kafka. Image removal never runs inside the profile transaction: a row in `pending_image_removal` is written with the change, and a scheduled worker deletes the file with bounded retries. An hourly sweep removes provider files that no profile references. The account-deleted announcement side (outbox and relay in `identity-access`) is already built by spec 013, so this plan covers only the `profile-next` service and the small platform additions it needs. Decisions and rationale: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (Jackson 3 under `tools.jackson`; annotations stay in `com.fasterxml.jackson.annotation`)

**Primary Dependencies**: Core: `spring-tx`, `spring-context` (already in the skeleton). Infrastructure: `spring-boot-starter-data-jdbc` (for `JdbcClient` and the datasource), `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-flyway` plus `flyway-database-postgresql`, `postgresql`, `java-uuid-generator` 5.2.0, `cloudinary-http5` (**verify** the version that matches Cloudinary's current Java SDK when the adapter task starts). Presentation: `spring-boot-starter-webmvc` (present), `spring-boot-starter-validation`, `spring-kafka`. Start: Spring Cloud Vault for the public key and Cloudinary credentials, as in `identity-access`

**Storage**: PostgreSQL 17, schema `profile_next` (tables `profile`, `pending_image_removal`); Cloudinary for image files

**Testing**: JUnit 5, AssertJ, ArchUnit (rules already in the skeleton), Testcontainers PostgreSQL and Kafka, fixed `Clock`, in-memory port fakes for core tests

**Target Platform**: Linux containers on the shared `chatter-net` network; the service itself runs on port 5011

**Project Type**: Multi-module Maven web service (`core`, `infrastructure`, `presentation`, `start`)

**Performance Goals**: Profile read or save in one database round trip; deletion notice fully applied (rows gone, removal queued) within seconds of arrival, images gone within 5 minutes under normal operation (SC-003)

**Constraints**: Core has no persistence, web or messaging types; no revocation list (FR-006); the service publishes no events (FR-013); no legacy machinery (FR-017); credentials only from Vault or the environment; log calls never mention token, secret, hash or credential words (existing `LoggingRuleTest`)

**Scale/Scope**: Reads and saves scale with signed-in users; deletion notices are rare; the sweep pages the provider listing once an hour

## Constitution Check

`.specify/memory/constitution.md` is still the unfilled template, so there are no ratified gates. The binding rules come from `AGENTS.md` (touch only what the task needs; leave sibling services alone) and from the structure rules the skeleton already enforces.

| Rule | Status |
|---|---|
| Only relevant files touched; legacy `services/profile` read-only | Pass: changes are in `services/profile-next`, `infrastructure/components/{kafka,vault}`, and `specs/`. `identity-access` and `services/profile` untouched |
| Core has no persistence, web or messaging types (FR-016) | Pass: ports use core records only; `ImageStorage` returns `UploadPermission`, `ConfirmedUpload`, `StoredImagePage`, not Cloudinary types |
| Every outbound port has exactly one adapter at a stated package (FR-016) | Pass: five rows added to `PORT_MAP` (data-model.md) |
| Use cases are single-method handlers of a command or query (FR-016) | Pass: seven handlers, each `UseCaseHandler<C, R>` |
| No legacy machinery (FR-017) | Pass: no aggregate root, dispatchers, integration events, shared asset record, or self-consumed events. The removal queue is a plain work table read by the service's own worker and is named `PendingImageRemoval`, not `Outbox` |
| Error shape and codes match `identity-access` (FR-015) | Pass: copied `ApiError` and code subset, listed in `contracts/profile-api.md` |
| Presentation reaches core only through `profile.api` | Pass: the Kafka listener and scheduler are presentation adapters and call use cases |

Re-check after design: unchanged. No complexity justification is needed beyond the table at the end.

## Source layout

### Documentation (this feature)

```text
specs/012-profile-next-rebuild/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── account-deleted-event.md   # existing (spec 012, amended by 013)
│   ├── profile-api.md             # HTTP contract
│   └── image-storage.md           # ImageStorage port and Cloudinary adapter
├── checklists/requirements.md
└── tasks.md                       # produced by /speckit-tasks
```

### Source code (repository root)

```text
services/profile-next/
├── pom.xml                                         # + dependencyManagement for versions not in the Boot BOM
├── profilenext-core/src/
│   ├── main/java/com/minewaku/chatter/profilenext/
│   │   ├── shared/handler/UseCaseHandler.java      # exists
│   │   ├── shared/port/UniqueStringIdGenerator.java                 # NEW
│   │   └── profile/
│   │       ├── api/command/{SaveProfile,RequestImageUpload,ConfirmImageUpload,DeleteProfileData,RemoveObsoleteImages,SweepUnreferencedImages}UseCase.java
│   │       ├── api/query/GetProfileUseCase.java
│   │       ├── api/exception/{InvalidProfileValue,UploadConfirmationRejected,ImageStorageUnavailable}Exception.java
│   │       └── internal/
│   │           ├── model/{UserId,DisplayName,Bio,ImageSlot,StoredImage,Profile,ProfileEdit}.java
│   │           ├── port/ImageStorage.java
│   │           ├── port/repository/{ProfileRepository,PendingImageRemovalRepository}.java
│   │           ├── port/query/ProfileQueryRepository.java
│   │           └── application/{command/*Service.java, query/GetProfileService.java}
│   └── test/java/…/profile/                        # value-object tests, service tests with port fakes (R14 table)
├── profilenext-infrastructure/src/
│   ├── main/
│   │   ├── java/…/infrastructure/
│   │   │   ├── adapter/outbound/profile/write/jdbc/JdbcProfileRepository.java        # NEW (public bean)
│   │   │   ├── adapter/outbound/profile/read/jdbc/JdbcProfileQueryRepository.java    # NEW
│   │   │   ├── adapter/outbound/image/removal/jdbc/JdbcPendingImageRemovalRepository.java   # NEW
│   │   │   ├── adapter/outbound/image/storage/cloudinary/{CloudinaryImageStorage,CloudinaryConfiguration,ImageProperties}.java   # NEW
│   │   │   ├── adapter/outbound/shared/id/UuidV7IdGenerator.java                      # NEW
│   │   │   ├── integration/postgresql/PostgreSqlConfiguration.java                    # NEW
│   │   │   (core also gets, under profile/internal/application/command: ImagePolicy, RemovalPolicy, SweepPolicy; under profile/api/exception: ImageStorageRateLimitedException;
│   │   │    presentation gets shared/error/InvalidIdentifierException and account/messaging/UnreadableNoticeException)
│   │   │   └── security/{SecurityConfiguration,JwtDecoderConfiguration,JwtProperties,PemKeys}.java   # NEW (no revocation filter)
│   │   └── resources/db/migration/V1__create_profile.sql                              # NEW
│   └── test/java/…/infrastructure/
│       ├── structure/StructureRules.java           # PORT_MAP: five rows
│       ├── adapter/outbound/**                     # Testcontainers and signature-vector tests
│       └── support/PostgreSqlTestSupport.java      # NEW
├── profilenext-presentation/src/main/java/…/presentation/
│   ├── profile/{web/ProfileController.java, dto/{ProfileResponse,SaveProfileRequest}.java}
│   ├── image/{web/ImageUploadController.java, dto/UploadPermissionResponse.java, scheduling/ImageMaintenanceScheduler.java}
│   ├── account/messaging/{AccountDeletedListener.java, AccountDeletedMessage.java, AccountDeletedConsumerConfiguration.java}
│   └── shared/{error/{ApiError,ApiErrors,ErrorCode,ApiExceptionHandler,ApiAuthenticationEntryPoint,ApiAccessDeniedHandler}.java, security/AccessPrincipal.java}
└── profilenext-start/src/
    ├── main/java/…/start/{signing/{SigningKeysConfiguration,VaultPublicKeyLoader}.java, time/TimeConfiguration.java}
    ├── main/resources/application.yml              # datasource, flyway, vault, kafka, images, security
    └── test/java/…/start/e2e/{ProfileFlowE2eTest,ImageFlowE2eTest,AccountDeletionE2eTest}.java

infrastructure/components/
├── vault/scripts/configure-secrets.sh + docs/runbooks/profile-next-cloudinary.md   # secret/profilenext keys
└── kafka/docs/runbooks/dead-letter-replay.md                                       # replaying identityaccess.user.v1.profile-next.dlt
                                                                                    # (topics already exist from spec 013; the legacy profile topics stay, services/profile is read-only)
```

**Structure decision**: Layout follows `identity-access` file for file, so a developer finds a behaviour's use case, port and adapter in the same place in both services (SC-009). Inbound adapters (web, Kafka listener, scheduler) all live in `presentation`, grouped by capability, because the presentation architecture test already allows `presentation.<capability>.web` and forbids `internal` and infrastructure imports. The port-to-adapter map has one row per port: `ProfileRepository`, `ProfileQueryRepository`, `PendingImageRemovalRepository`, `ImageStorage`, `UniqueStringIdGenerator`.

## Delivery order

1. **Foundation**: dependency management, `application.yml`, Flyway V1, Postgres test support, `Clock` and Vault public-key wiring, error shape, security config. The service starts and refuses unauthenticated calls.
2. **Story 1 and 2** (P1): value objects, `GetProfile`, `SaveProfile`, the two JDBC adapters, `ProfileController`. This is the smallest slice that proves the service end to end.
3. **Story 3** (P2): `ImageStorage` and its adapter, the removal queue and worker, the upload and webhook endpoints, the sweep.
4. **Story 4** (P2): `DeleteProfileData`, the listener, retry and dead-letter wiring.
5. **Closing**: PORT_MAP rows and architecture-test confirmation, Vault and dead-letter runbooks, quickstart walkthrough, requirement-to-test trace.

Story 5 (the announcement side in `identity-access`) is delivered by spec 013 and is not repeated here: that covers FR-019 to FR-024 and the topic and relay parts of FR-025. FR-027 (no migration of legacy profiles) needs no work: `V1__create_profile.sql` creates empty tables and nothing reads the legacy database.

## Open points for the owner

- **Schema or database**: research R10 follows `identity-access` (own schema in the shared `chatter` database). Spec 012's assumption says "own database in a shared instance". Changing it later is a connection-string change.
- **`PATCH /@me` replaces the legacy `PUT`** because saves are now partial. Clients and the gateway must switch verb and path.
- **Two provider facts are unverified** (research R4 and R7): a per-upload size limit on signed Cloudinary uploads, and the Admin API rate limit. Both have a safe fallback built in, so neither blocks the plan; each is closed by the task that builds that adapter.

## Complexity Tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| A `pending_image_removal` table and worker | FR-009 needs a removal that survives a crash and is retried for hours without touching the profile transaction | In-memory retry loses work on restart; an after-commit hook loses the removal if the process dies between commit and call |
| Provider listing for the sweep | Permitted-but-never-confirmed uploads leave no database row, so only the provider can say what exists | A database-only sweep can never find the very files story 3, scenario 6 is about |
| Own copy of `ApiError` and codes in presentation | FR-015 requires the same shape and vocabulary, and the project decided against shared libraries (spec 012 assumptions) | A shared module couples two services' release cycles for about five small classes |
