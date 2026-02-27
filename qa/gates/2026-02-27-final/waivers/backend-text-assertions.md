# Backend Test- Debt Waiver: Text Assertion Mismatch

Date: 2026-02-27
Scope: `./backend-kmp/gradlew -p backend-kmp clean test`
Status: CONDITIONAL (not sufficient for GO until manual CUR compensation is attached)

## Failed tests (exact)

1. `NotificationServiceTest.kt:99`
2. `RosterServiceTest.kt:239`
3. `StatisticsServiceTest.kt:212`

Gradle summary: `107 tests completed, 3 failed`
Log source: `qa/gates/2026-02-27-final/logs/backend-test.log`

## Root cause

Assertion strings still expect old wording:

- expected contains: `не привязан к группе`

Current backend behavior returns plural wording:

- actual: `Куратор не привязан к группам`

This is a message-text contract drift after curator multi-group update, not a confirmed functional regression in roster/statistics computation.

## Compensating control required by gate policy

Mandatory manual compensation: successful execution evidence for `CUR-01..06` on Android+iOS.

Current compensation status: NOT ATTACHED in this run.

## Decision impact

- Waiver can keep backend freeze policy intact.
- Waiver does not override missing manual CUR evidence.
- Gate remains `NO-GO` until compensation evidence and iOS mandatory evidence are attached.
