# Release Hardening Gate Runbook

## 1) Backend test suite

```bash
./backend-kmp/gradlew -p backend-kmp test --no-daemon
```

Save logs to:
`qa/gates/2026-03-04-release-hardening/logs/backend-test.log`

## 2) Shared/KMP tests

```bash
./gradlew :shared:testDebugUnitTest --no-daemon
./gradlew :shared:compileKotlinIosX64 --no-daemon
```

Save logs to:
- `qa/gates/2026-03-04-release-hardening/logs/shared-android-test.log`
- `qa/gates/2026-03-04-release-hardening/logs/shared-ios-compile.log`

## 3) Load test (k6)

```bash
mkdir -p qa/gates/2026-03-04-release-hardening/logs
# if k6 is not in PATH in this workspace, use ./.tools/k6
k6 run qa/load/k6/release-hardening.js \
  -e K6_BASE_URL=http://localhost:8080 \
  -e K6_CHEF_LOGIN=<chef_login> \
  -e K6_CHEF_PASSWORD=<chef_password> \
  -e K6_STUDENT_LOGIN=<student_login> \
  -e K6_STUDENT_PASSWORD=<student_password> \
  | tee qa/gates/2026-03-04-release-hardening/logs/k6-release-hardening.log
```

## 4) Security checks

- `GET /api/v1/auth/me` without JWT -> `401`
- `GET /api/v1/auth/my-keys` without JWT -> `401`
- `POST /api/v1/qr/validate-offline` without role -> `401/403` (default config)

## 5) Finalize gate

1. Attach iOS simulator + iOS device evidence.
2. Attach Android evidence and cross-platform QR evidence.
3. Update `verdict.json` to `GO` only if all mandatory checks are complete.
