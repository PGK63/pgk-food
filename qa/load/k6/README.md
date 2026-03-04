# k6 Release Hardening Load Test

## Goal
Validate SLO targets for:
- `POST /api/v1/qr/validate`: `p95 <= 500ms`
- `POST /api/v1/transactions/batch`: `p95 <= 900ms`
- Error rate for both endpoints: `< 1%`

## Scenarios
`qa/load/k6/release-hardening.js` runs two parallel scenarios:
- `qr_validate`: ramp `100 -> 300 -> 500` VU, then down.
- `sync_batch`: ramp `100 -> 300 -> 500` VU, then down.

## Required env
- `K6_BASE_URL` (example: `https://food.pgk.apis.alspio.com`)
- `K6_CHEF_LOGIN` (CHEF or ADMIN login for `/qr/validate` and `/transactions/batch`)
- `K6_CHEF_PASSWORD`
- `K6_STUDENT_LOGIN` (STUDENT login for dynamic ECDSA signing in load test)
- `K6_STUDENT_PASSWORD`

Optional:
- `K6_QR_MEAL_TYPE` (`BREAKFAST` or `LUNCH`, default `LUNCH`)
- `K6_SYNC_MEAL_TYPE` (`BREAKFAST` or `LUNCH`, default `LUNCH`)
- `K6_QR_NONCE_PREFIX` base nonce prefix
- `K6_QR_USER_ID` (override student UUID for QR endpoint)
- `K6_SYNC_STUDENT_ID` (override student UUID for sync endpoint)
- `K6_DISABLE_DYNAMIC_SIGN=true` (legacy mode, disables on-the-fly ECDSA signing)
- `K6_QR_SIGNATURE` required only with `K6_DISABLE_DYNAMIC_SIGN=true`

## Run
```bash
k6 run qa/load/k6/release-hardening.js \
  -e K6_BASE_URL=http://localhost:8080 \
  -e K6_CHEF_LOGIN=chef-test \
  -e K6_CHEF_PASSWORD=secret \
  -e K6_STUDENT_LOGIN=student-test \
  -e K6_STUDENT_PASSWORD=student-secret
```

## Evidence
Save output to gate artifacts:
```bash
mkdir -p qa/gates/2026-03-04-release-hardening/logs
k6 run qa/load/k6/release-hardening.js ... \
  | tee qa/gates/2026-03-04-release-hardening/logs/k6-release-hardening.log
```
