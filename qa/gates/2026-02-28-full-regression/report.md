# Full Regression Gate Report (2026-02-28)

## Scope

- Gate target: Android + iOS full regression (functional parity baseline + QR/Scanner UX updates)
- Baseline branch for business logic parity: `origin/oleg`
- Focus areas: QR generation, scanner, offline mode, keys, sync, cross-platform QR compatibility

## Build / Environment notes

- Current local session constraints:
  - Java runtime is not installed (Gradle tasks cannot run here)
  - Full Xcode environment is not available in this session
- Device/manual execution is expected via QA infrastructure

## Priority execution order

1. `QR-01`, `QR-02`, `QR-03`
2. `SCN-01`, `SCN-02`, `SCN-03`, `SCN-04`, `SCN-05`
3. `SYNC-01`..`SYNC-05`
4. Remaining matrix scenarios (`LGN/REGU/REGG/CUR/ADM/PAR`)
5. Cross-platform test: iOS QR scanned by Android and Android QR scanned by iOS

## UI acceptance checklist for this change

- [ ] Student QR is visually centered (no left-compressed look) on Android and iOS
- [ ] Student QR is noticeably larger on typical phone screens
- [ ] Scanner camera window is larger than previous version
- [ ] Scanner overlay frame is larger than previous version
- [ ] Auto brightness boosts on QR screen entry
- [ ] Brightness is restored on QR screen exit

## Required evidence per scenario

- Platform, device model, OS version
- Timestamp (local + UTC)
- PASS/FAIL
- Repro steps and expected vs actual
- Screenshot/video path under `qa/gates/2026-02-28-full-regression/artifacts/`
- Defect ID (if FAIL)

## Commands to run in prepared environments

### Android

```bash
./gradlew :app:testDebugUnitTest :shared:testDebugUnitTest
./gradlew :app:assembleDebug
```

### iOS (Mac)

```bash
xcodegen generate --project /Users/oleg/StudioProjects/pgk-food/iosApp
xcodebuild -project /Users/oleg/StudioProjects/pgk-food/iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' build
```

## Gate status

- Current status: `IN_PROGRESS`
- Final decision: pending evidence fill-in in `traceability.csv` and artifacts
