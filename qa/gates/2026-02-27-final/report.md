# Final Release Gate Report (2026-02-27)

## Scope

- Gate target: Android+iOS full parity vs `origin/oleg`
- RC refs:
  - root: `e8cb925dfec51bdc241f45b3f85498555530504f`
  - backend nested repo: `ea54245e87d186566ddca9c888b5db2d5d5f9108`
  - parity baseline: `origin/oleg@461e44231f4a931875b3dc2e423f492131f81562`

## Evidence inventory

- `parity/git-diff-name-status.txt` captured (`167` entries)
- Fresh Android logs:
  - `logs/android-unit.log` (`BUILD SUCCESSFUL`)
  - `logs/android-assemble.log` (`BUILD SUCCESSFUL`)
- Fresh backend log:
  - `logs/backend-test.log` (`:test FAILED` with 3 known text assertion failures)
- iOS files:
  - `ios/mac-build.log` (blocked on Linux runner)
  - `ios/device-scenarios.md` (manual checklist pending)

## Command results

### 1) Freeze baseline and diff

- `git fetch origin --prune`: executed
- `git rev-parse HEAD`: captured
- `git rev-parse origin/oleg`: captured
- `git diff --name-status origin/oleg...HEAD`: captured into `parity/git-diff-name-status.txt`

Status: PASS (snapshot captured)

### 2) Fresh automation rerun

- `./gradlew --rerun-tasks :app:testDebugUnitTest :shared:testDebugUnitTest`
  - Result: PASS (`BUILD SUCCESSFUL`, see `logs/android-unit.log`)
- `./gradlew --rerun-tasks :app:assembleRelease`
  - Result: PASS (`BUILD SUCCESSFUL`, see `logs/android-assemble.log`)
- `./backend-kmp/gradlew -p backend-kmp clean test`
  - Result: CONDITIONAL FAIL (`:test FAILED`, see `logs/backend-test.log`)
  - Failures (exact):
    - `NotificationServiceTest.kt:99`
    - `RosterServiceTest.kt:239`
    - `StatisticsServiceTest.kt:212`

### 3) Endpoint parity (PAR-02)

File: `parity/endpoints_old_vs_new.csv`

Summary:
- `matched`: 36
- `intentional_compatibility`: 1 (`/api/v1/qr/validate-offline`)
- `deprecated_backend_path`: 1 (`/api/v1/qr/sync` backend-only)
- `backend_extra`: 2

Status: PASS (no user-visible endpoint gap identified)

### 4) Screen/role parity (PAR-01/PAR-03 static map)

File: `parity/screens_old_vs_new.csv`

Summary:
- `matched`: 14
- `renamed_alias`: 4
- `reworked_same_scope`: 1

Status:
- PAR-03 static mapping: PASS
- PAR-01 runtime/manual parity: PENDING manual execution

## Scenario traceability

File: `traceability.csv`

- Tracked scenarios: all provided IDs (`LGN..PAR`) are present.
- Current result distribution:
  - `NOT_EXECUTED_MANUAL`: 34
  - `PARTIAL_AUTOMATED_ONLY`: 5
  - `PASS_STATIC_PARITY`: 2

Note: source scenario list contains 41 IDs (`PAR-01..03` plus all previous groups), despite the textual label "1..40".

## Waiver status

File: `waivers/backend-text-assertions.md`

- Waiver drafted for 3 backend text assertion mismatches (`группе` vs `группам`).
- Compensating control required by policy (`CUR-01..06` manual Android+iOS evidence) is not attached yet.

Status: CONDITIONAL (insufficient for GO by itself)

## iOS gate status

- Mac build command evidence is blocked in current Linux environment.
- Real-device camera/scanner regression evidence is not attached.

Status: BLOCKED

## Final gate decision

`NO-GO`

### Active blockers

1. Missing mandatory iOS Mac build evidence (`xcodegen` + `xcodebuild` on Mac runner).
2. Missing mandatory iOS real-device scanner/camera evidence (`SCN-*`).
3. Mandatory manual smoke/regression matrix not fully executed (`LGN/QR/SCN/SYNC/REG/CUR/ADM/PAR`).
4. Backend automated suite has 3 failing text assertions; waiver is conditional until manual CUR compensation is attached.
