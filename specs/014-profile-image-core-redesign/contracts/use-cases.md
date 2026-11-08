# Contract: use cases (`…profile.api`, called by presentation and schedulers)

Every use case is a single-method `UseCaseHandler<Command, Result>`. Only changes against today's code are marked; unchanged use cases are listed to show nothing else moves.

| Use case | Command | Result | Change |
|---|---|---|---|
| `RequestImageUploadUseCase` | `userId` (from the authenticated caller only), `slot` | `uploadUrl`, `method`, `fields`, `expiresAt` | None to the shape. Behaviour: fresh `ImageId`, permission for the caller's own slot, writes nothing |
| `ConfirmImageUploadUseCase` | `rawBody`, `signature`, `timestamp` | `Outcome` = `APPLIED` / `IGNORED` / `FILE_NOT_ALLOWED` | Shape unchanged. `FILE_NOT_ALLOWED` no longer queues the file. Throws `UploadConfirmationRejectedException` with `INVALID_SIGNATURE` only |
| `PromotePendingImagesUseCase` | none | `promoted`, `failed`, `reported` (counts) | **New** |
| `RemoveObsoleteImagesUseCase` | none | `removed`, `failed`, `reported` | None to the shape |
| `SweepUnreferencedImagesUseCase` | none | `removed` | None to the shape |
| `DeleteProfileDataUseCase` | `eventId`, `userId`, `occurredAt` | empty | None to the shape; now also drops promotion records |
| `SaveProfileUseCase` | `userId`, `displayName?`, `bio?` | `changed` | None |
| `GetProfileUseCase` | `userId` | `userId`, `displayName`, `bio`, `avatar`, `banner`, `createdAt`, `updatedAt` | **`avatarUrl`/`bannerUrl` (String) are replaced by `avatar`/`banner`, each null or `ImageResult(imageId: String, promoted: boolean)`** |

## `PromotePendingImagesUseCase`

- `promoted`: records whose promotion succeeded and were deleted in this run.
- `failed`: records whose attempt failed in this run (includes those given up).
- `reported`: of those, records given up in this run (slot cleared if it still held the image; error logged).
- One failing record never stops the rest of the batch. No due records gives `0, 0, 0`.

## Errors

| Exception | Raised by | Change |
|---|---|---|
| `UploadConfirmationRejectedException` (`Reason.INVALID_SIGNATURE`) | confirm | `Reason.USER_MISMATCH` removed (cannot occur) |
| `ImageStorageUnavailableException`, `ImageStorageRateLimitedException` | request upload; sweep (handled inside) | none |
| `InvalidProfileValueException` | save | none |

## What presentation must do later (not in this spec)

- Map `GetProfileUseCase.Result.avatar/banner` to a delivery address using user id, slot, image id and the promoted flag, via the infrastructure resolver.
- Keep answering `200` to the provider whatever `ConfirmImageUploadUseCase` returns or throws.
- Never read the user id of `RequestImageUploadUseCase.Command` from the request body.
- Call `PromotePendingImagesUseCase` from a scheduler that mirrors the removal one.
