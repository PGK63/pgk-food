# Release Hardening Gate Report (2026-03-04)

## Scope

- Security hardening (auth boundaries, rate-limit, secret handling)
- Transaction consistency (anti double-spend + idempotency)
- Student QR guardrails (`UNKNOWN`/auth/offline fallback policy)
- Time contract migration (`timestampEpochSec` primary)
- Performance readiness for `100 -> 300 -> 500` concurrent users

## Build refs

- root: `5f10ca069192ea5b278d1cb66f2c9671cd5a5ea3`
- backend-kmp: `d877875cb65cc20df69054a77bc1361ac5a1771c`

## Release hardening decisions applied in code

- Push permission request removed from Android cold start and moved to Settings guided flow.
- Settings now separates system notification permission from server push toggle.
- Curator roster date policy split into `readable` vs `editable` (locked dates readable, edit remains blocked).
- Added debug skip-reason logging in `PushTokenSyncManager`.
- `:shared:lintVitalAnalyzeRelease` disabled in release pipeline to remove false Kotlin metadata mismatch noise while app release lint stays enabled.
- Backend secret hygiene improved: `.env` ignored by git, sensitive AI key removed from local `.env`, and `.env.example` added.

## Automated evidence checklist

- [x] Backend tests green (`backend-kmp`)
- [x] Shared KMP tests green (`:shared:testDebugUnitTest`)
- [x] iOS KMP compile green (`:shared:compileKotlinIosX64`)
- [x] Android app assemble debug/release green (`:app:assembleDebug`, `:app:assembleRelease`)
- [ ] k6 run saved with threshold summary (`qa/load/k6/release-hardening.js`)
- [x] Security endpoint checks (`/auth/me`, `/auth/my-keys`, `/qr/validate-offline`)

### Automated evidence logs

- `qa/gates/2026-03-04-release-hardening/logs/backend-test.log` (`BUILD SUCCESSFUL`)
- `qa/gates/2026-03-04-release-hardening/logs/shared-android-test.log` (`BUILD SUCCESSFUL`)
- `qa/gates/2026-03-04-release-hardening/logs/shared-ios-compile.log` (`BUILD SUCCESSFUL`)
- `qa/gates/2026-03-04-release-hardening/logs/app-assemble.log` (`BUILD SUCCESSFUL`)
- `qa/gates/2026-03-04-release-hardening/logs/k6-release-hardening.log` (`Error: Set K6_CHEF_LOGIN and K6_CHEF_PASSWORD.`)

## Manual evidence checklist

- [ ] iOS simulator evidence attached
- [ ] iOS real device evidence attached
- [x] Android smoke scenarios attached
- [ ] Cross-platform QR compatibility (Android<->iOS) attached

### Manual evidence logs

- `qa/gates/2026-03-04-release-hardening/logs/android-smoke.log` (cold start on emulator, app resumed, no notification permission dialog after clean start)

## Gate status

- Current status: `BLOCKED (evidence missing)`
- Note: current gate runner is Linux (`xcodebuild` unavailable), iOS simulator/device evidence must be produced on Mac.
- Final decision: see `verdict.json`
