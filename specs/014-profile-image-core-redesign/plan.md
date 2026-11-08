# Implementation Plan: Profile Image Redesign (core layer)

**Branch**: `profile-next` (spec directory `014-profile-image-core-redesign`) | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/014-profile-image-core-redesign/spec.md`

## Summary

Replace the core's `StoredImage(publicId, url)` with a model in which a profile stores one time-ordered `ImageId` per slot and nothing else. Ports speak in `OwnedImage(userId, slot, imageId)`, so the core never sees a path, a folder name or an address. Uploads land in temporary storage; confirming one (a transaction that first locks the owner's profile) sets the slot, queues the replaced image for removal, cancels its pending promotion and queues the new image for promotion. A new, non-transactional promotion worker use case moves each image to permanent storage with backoff, and gives up after a horizon by clearing the slot if it still holds that image. The removal, delete-profile-data and sweep services are re-keyed to the new identity. Reads return an image id plus a promoted flag per slot; building the address moves to a later layer. Everything is proven with in-memory fakes in the core module alone. Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 parent (core uses only `spring-tx`, `spring-context`, `slf4j-api`)

**Primary Dependencies**: none added. Core stays free of persistence, web, messaging and provider types

**Storage**: N/A in this feature. The core only declares ports; the PostgreSQL and Cloudinary adapters are separate specs

**Testing**: JUnit 5 and AssertJ via `spring-boot-starter-test`; hand-written in-memory fakes and a fixed `Clock`; no Mockito needed in core. Baseline: 58 tests, all passing, run with `./mvnw -q -pl profilenext-core test` from `services/profile-next`

**Target Platform**: Linux (developed on WSL2); library module of a Spring Boot service

**Project Type**: One module (`profilenext-core`) of a four-module Maven service (`core`, `infrastructure`, `presentation`, `start`)

**Performance Goals**: Core unit test suite under 60 seconds (SC-002). No runtime targets; all work is orchestration

**Constraints**: Core imports nothing from infrastructure, Spring Data/JDBC, Kafka, security, web or a provider SDK; only the `shared.port.UniqueStringIdGenerator` (string UUIDv7) is available for ids; no module other than core is edited

**Scale/Scope**: About 8 new and 25 changed or deleted files in one module (see Source layout)

## Constitution Check

`.specify/memory/constitution.md` is still the unfilled template, so there are no ratified gates. The binding rules are `AGENTS.md` and the structure rules the service already enforces.

| Rule | Status |
|---|---|
| Only files relevant to the task are touched; sibling services left alone (`AGENTS.md`) | Pass: only `services/profile-next/profilenext-core` and `specs/014-…`. See Complexity Tracking for the knock-on effect on sibling modules of the same service |
| Core has no persistence, web, messaging or provider types | Pass: new ports use core records only (`OwnedImage`, `ImageRef`, nested records); no `UUID`-based provider type leaks (`ImageId` wraps `java.util.UUID` only) |
| Each outbound port has exactly one adapter at a stated package (`StructureRules.PORT_MAP`) | Deferred, not violated: `PendingImagePromotionRepository` is a new port and needs a row plus an adapter in the infrastructure spec. Existing rows stay valid because no existing port is renamed or removed |
| Use cases are single-method handlers (`UseCaseHandler<C, R>`) | Pass: one new use case (`PromotePendingImagesUseCase`) follows the pattern |
| Work-queue tables are plain queues, not an outbox (spec 012 FR-017) | Pass: the promotion queue mirrors the removal queue and is named `PendingImagePromotion` |
| Uncommitted unrelated refactor is preserved | Pass: nested port records, `DisplayName`/`Bio` in `Profile` are kept (spec Scope) |

Re-check after design: unchanged. The one item that needs a recorded justification is below.

## Project Structure

### Documentation (this feature)

```text
specs/014-profile-image-core-redesign/
├── plan.md              # This file
├── research.md          # Phase 0: decisions, rationale, rejected alternatives, risks
├── data-model.md        # Phase 1: types, ports' data, state transitions
├── quickstart.md        # Phase 1: how to prove the feature in the core alone
├── contracts/
│   ├── ports.md         # Outbound port contracts (storage, repositories, queues)
│   └── use-cases.md     # Inbound use-case contracts (api package) and what changes
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 ($speckit-tasks) - not created here
```

### Source code (all under `services/profile-next/profilenext-core/src`)

```text
main/java/com/minewaku/chatter/profilenext/profile/
├── api/
│   ├── command/
│   │   ├── PromotePendingImagesUseCase.java          # NEW  Command(), Result(promoted, failed, reported)
│   │   └── ConfirmImageUploadUseCase.java            # doc: FILE_NOT_ALLOWED no longer queues a removal
│   ├── exception/UploadConfirmationRejectedException.java   # drop Reason.USER_MISMATCH
│   └── query/GetProfileUseCase.java                  # Result: avatar/banner = ImageResult(imageId, promoted)
└── internal/
    ├── model/
    │   ├── ImageId.java            # NEW  record(UUID), Comparable, of(String)
    │   ├── ImageDimensions.java    # NEW  record(width, height), both > 0
    │   ├── OwnedImage.java         # NEW  record(UserId, ImageSlot, ImageId)
    │   ├── ImageRef.java           # NEW  record(ImageId, boolean promoted)  (read model)
    │   ├── ImageSlot.java          # drop folder()
    │   ├── Profile.java            # avatar/banner become ImageId
    │   └── StoredImage.java        # DELETED
    ├── port/
    │   ├── ImageStorage.java                          # permit/verify/promote/remove/list on OwnedImage
    │   ├── query/ProfileQueryRepository.java          # ProfileView with ImageRef; findReferenced(OwnedImage…)
    │   └── repository/
    │       ├── ProfileRepository.java                 # findForUpdate, setImage, clearImageIf, deleteByUserId
    │       ├── PendingImageRemovalRepository.java     # keyed by ImageId, rows carry OwnedImage
    │       └── PendingImagePromotionRepository.java   # NEW
    └── application/
        ├── command/
        │   ├── ImagePolicy.java                       # + per-slot max dimension, permits(ConfirmedUpload)
        │   ├── PromotionPolicy.java                   # NEW  horizon, delays, batch, lease
        │   ├── PromotePendingImagesService.java       # NEW
        │   ├── RequestImageUploadService.java         # ImageId.of(ids.nextId()), permit(OwnedImage)
        │   ├── ConfirmImageUploadService.java         # lock, decide, queue (see research D4–D6)
        │   ├── RemoveObsoleteImagesService.java       # re-keyed
        │   ├── DeleteProfileDataService.java          # + delete promotion rows
        │   ├── SweepUnreferencedImagesService.java    # re-keyed, permanent images only
        │   └── SaveProfileService.java                # unchanged behaviour; compile fixes only
        └── query/GetProfileService.java               # maps ImageRef to ImageResult

test/java/com/minewaku/chatter/profilenext/profile/internal/
├── model/        ImageIdTest (NEW), ImageDimensionsTest (NEW), OwnedImageTest (NEW), ImageSlotTest (update),
│                 StoredImageTest (DELETED)
├── port/         ConfirmedUploadTest (rewrite: ConfirmedUpload invariants)
└── application/
    ├── fakes/    FakeImageStorage, FakeProfileStore, FakeRemovalQueue (update); FakePromotionQueue (NEW);
    │             ImageIds (NEW test fixture: deterministic ordered ids)
    └── …         RequestImageUploadServiceTest, ConfirmImageUploadServiceTest, PromotePendingImagesServiceTest (NEW),
                  RemoveObsoleteImagesServiceTest, DeleteProfileDataServiceTest, SweepUnreferencedImagesServiceTest,
                  SaveProfileServiceTest, GetProfileServiceTest (all update)
```

**Structure Decision**: keep the existing hexagonal layout and package names; add no new package. New model types live in `internal/model`; the one new port next to its sibling in `internal/port/repository`; the new use case in `api/command` with its service in `internal/application/command`, exactly as the removal worker is laid out. Nested records in ports continue the direction of the working-tree refactor.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| After this feature `profilenext-infrastructure`, `-presentation` and `-start` do not compile against the core (they still use `StoredImage`, `publicId`, `avatarUrl`, `USER_MISMATCH`), so a whole-service `./mvnw verify` fails until the follow-on specs land | The spec's scope is core-only by the user's decision, and the model change is a breaking one at the port boundary | A compatibility layer in core (keeping `StoredImage`, string public ids, URL fields) would keep the old model alive in the module whose whole point is to drop it, and would have to be deleted again later. Rejected in spec FR-033. Mitigation: verify with `-pl profilenext-core`, keep the work off the main branch until infrastructure and presentation specs are done, and list the exact breakages in `research.md` (R4) so the next spec starts from a checklist |
