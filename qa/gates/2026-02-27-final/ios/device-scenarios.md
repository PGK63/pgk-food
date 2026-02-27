# iOS Real Device Smoke/Regression Evidence

Status: BLOCKED (no real-device execution evidence attached in this Linux gate run)
Required policy: Hybrid CI+Device (Mac build + iPhone camera/scanner run)

## Mandatory scenario checklist (iOS)

- [ ] LGN-01 valid login per role
- [ ] LGN-02 invalid login error handling
- [ ] LGN-03 kill/restart session restore
- [ ] LGN-04 forced 401 logout/re-login
- [ ] QR-01 QR generation + 60s rotation
- [ ] QR-02 missing keys -> download -> regenerate
- [ ] QR-03 time sync fallback
- [ ] SCN-01 online valid scan
- [ ] SCN-02 offline auto-mode scan
- [ ] SCN-03 reject paths (invalid sig/expired/no permission/already eaten)
- [ ] SCN-04 camera denied/re-grant flow
- [ ] SCN-05 result card fields
- [ ] SYNC-01 unsynced count increments
- [ ] SYNC-02 manual sync button
- [ ] SYNC-03 auto sync on reconnect
- [ ] SYNC-04 partial sync keeps successful tx
- [ ] SYNC-05 duplicate hash idempotency
- [ ] REGU-01 create user + copy credentials
- [ ] REGU-02 search by FIO/login
- [ ] REGU-03 filter by group/role
- [ ] REGU-04 update roles
- [ ] REGU-05 reset password + copy
- [ ] REGU-06 delete user
- [ ] REGG-01 create/delete group
- [ ] REGG-02 assign/remove curator
- [ ] REGG-03 add/remove student
- [ ] REGG-04 transfer/rename rollback handling
- [ ] CUR-01 roster load by date
- [ ] CUR-02 multi-group selector
- [ ] CUR-03 meal toggles
- [ ] CUR-04 copy day
- [ ] CUR-05 student search
- [ ] CUR-06 save/reload consistency
- [ ] ADM-01 daily report generation
- [ ] ADM-02 totals include offline tx
- [ ] ADM-03 fraud list by range
- [ ] ADM-04 resolve fraud verify state
- [ ] ADM-05 CSV/PDF/copy export
- [ ] PAR-01 role flow parity
- [ ] PAR-02 endpoint parity (manual confirmation)
- [ ] PAR-03 screen-level parity

## Required attachments per scenario

- timestamp (UTC/local)
- tester and device model/iOS version
- video/screenshot link
- app build ref
- PASS/FAIL and defect link
