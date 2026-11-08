# Quickstart: validating the core image redesign

Everything here runs on the core module alone: no database, no provider, no network. Run from `services/profile-next` with the service's own wrapper (never a bare `mvn`).

## Prerequisites

- Java 21 on the path; the wrapper downloads Maven on first use.
- Working tree on branch `profile-next`. The sibling modules are expected not to compile until their specs land, so always use `-pl profilenext-core`.

## 1. Baseline (before starting)

```bash
cd services/profile-next
./mvnw -q -pl profilenext-core test
```

Expected: success; 58 tests (read `profilenext-core/target/surefire-reports`).

## 2. Whole core suite after the change

```bash
./mvnw -q -pl profilenext-core test
```

Expected: success, no failures or errors, in well under 60 seconds (SC-002, SC-009). Result of the implementation: **126 tests, 0 failures, 0 errors**; about 9 seconds including Maven start-up, under 1 second of test time. Use `clean test` when counting: the surefire reports directory keeps reports of deleted tests otherwise and inflates the sum.

## 3. Per story (fast loop)

| Story | Command (class filter) | Expected |
|---|---|---|
| 1 identity model | `./mvnw -q -pl profilenext-core test -Dtest='ImageIdTest,ImageDimensionsTest,OwnedImageTest,ImageSlotTest,ConfirmedUploadTest'` | ordering incl. the signed-compare pin, dimension rejection, no folder on a slot |
| 2 request | `-Dtest=RequestImageUploadServiceTest` | fake storage saw caller's user, slot and a fresh id; second id orders after the first; no repository touched |
| 3 confirm | `-Dtest=ConfirmImageUploadServiceTest` | decision-table rows 1 to 6, replay, out-of-order, avatar vs banner independence, lock call precedes any read or write |
| 4 promote | `-Dtest=PromotePendingImagesServiceTest` | success deletes record; failure reschedules with growing delay capped; expiry clears slot only when equal and reports; leased row not retaken; row deleted whenever promote returns normally; batch continues after one failure |
| 5 remove | `-Dtest=RemoveObsoleteImagesServiceTest` | owner, slot, id passed through; retry/backoff/give-up; duplicate enqueue ignored |
| 6 delete data | `-Dtest=DeleteProfileDataServiceTest` | both ids queued with user id, promotion rows gone, repeat is a no-op |
| 7 sweep | `-Dtest=SweepUnreferencedImagesServiceTest` | only old, unreferenced, unqueued permanent images removed; all pages followed; rate limit stops early |
| 8 save/get | `-Dtest='SaveProfileServiceTest,GetProfileServiceTest'` | images untouched by save; `promoted` flips when the promotion record is deleted |

## 4. Old model is gone (SC-001, SC-009)

Run from the repository root. Three tests assert by reflection that old members are gone and so name them on purpose; they are excluded from the test search.

```bash
OLD='StoredImage\b|publicId|\.folder\(\)|avatarUrl|bannerUrl|USER_MISMATCH|replaceImage|Replacement'
grep -rnE "$OLD" services/profile-next/profilenext-core/src/main ; echo "main exit=$?"
grep -rnE "$OLD" services/profile-next/profilenext-core/src/test \
  --exclude=ConfirmedUploadTest.java --exclude=ProfileTest.java --exclude=ImageSlotTest.java ; echo "test exit=$?"
```

Expected: no matches (both `exit=1`). `StoredImageInfo` and `StoredImagePage` are new-style port records and are allowed (the `\b` keeps `StoredImage` exact).

## 5. Dependency rule (FR-032, research D13)

From the repository root:

```bash
grep -rnE "^import (com\.minewaku\.chatter\.profilenext\.infrastructure|org\.springframework\.(jdbc|data|kafka|web|security)|jakarta\.servlet|com\.cloudinary)" services/profile-next/profilenext-core/src/main ; echo "exit=$?"
```

Expected: no matches (`exit=1`). Core may import `org.springframework.stereotype` and `org.springframework.transaction`, as today.

## 6. Hand-off check

Confirm the breakage list in `research.md` (R4) is accurate by compiling the siblings once and noting the errors:

```bash
./mvnw -q -pl profilenext-infrastructure,profilenext-presentation -am compile 2>&1 | grep -E "ERROR.*\.java" | head
```

Expected: errors only in the files listed in R4, none in core. This step documents the gap; it is not a pass/fail gate for this feature.
