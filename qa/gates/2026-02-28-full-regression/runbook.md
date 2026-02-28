# Full Regression Runbook (Android + iOS)

## 1. Setup

1. Checkout target branch with QR/Scanner updates.
2. Capture refs:
   - `git rev-parse HEAD`
   - `git rev-parse origin/oleg`
3. Record devices and OS versions.

## 2. Automated checks

1. Android unit tests.
2. Android debug assemble.
3. iOS build on Mac (xcodegen + xcodebuild).
4. Save logs under `logs/`.

## 3. Manual matrix execution

1. Use `traceability.csv` as source of truth.
2. Execute priority scenarios first: QR, SCN, SYNC.
3. Fill PASS/FAIL + artifact links for each scenario.

## 4. Cross-platform scanner verification

1. Generate student QR on iOS and scan on Android chef.
2. Generate student QR on Android and scan on iOS chef.
3. Verify identical business outcome and error semantics.

## 5. Gate completion

1. Update `report.md` with summary and unresolved risks.
2. Update `verdict.json` with final GO/NO-GO.
3. Ensure every FAIL scenario has a defect ID.
