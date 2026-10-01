# Hybrid Fitness Coach — Release / Build History

This file is the compact release ledger. GitHub commits and workflow runs remain the source of truth.

| Line | Status | Frozen / verified reference | CI evidence | Notes |
|---|---|---|---|---|
| v0.8 evolved | Historical | repository history | historical Android CI | Universal runner, structured sessions, persistence/recovery foundations. |
| v0.9.0 | Historical / closed | repository history | historical CI | Progression, sequencing and Adaptive Coach development line. |
| v0.9.1 | Historical / closed | repository history / PR #8 audit trail | historical RC2 CI | Multi-week plan, recovery, exercise detail/progression and persistence work. |
| v0.9.2 | Frozen | `release/v0.9.2` at `9c6229485ee622f3e0d825bc20c5adf58fb39d92` | green checkpoint recorded in release history | Closed predecessor baseline. |
| v0.9.3 | Frozen | `release/v0.9.3`; validated production-code checkpoint `365c07f643065df9dd68310362edd3ad3ed3236f` | Exercise DB v2 #250: success; Android APK #802: success | Repository acceptance complete. Interaction/device acceptance transfers to v0.9.4. |
| v0.9.4 | Frozen | runtime `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`; final documentation closure follows on same frozen line | Exercise DB v2 #292 success; Android APK #845 success | Repository baseline complete. Signed-release and physical-device/UI acceptance transferred to v0.9.5 by release decision. |

## Release discipline

Frozen release branches are references, not development branches. New behavior, fixes discovered during device acceptance, schema changes and UX changes go to the current feature branch. A green CI build is necessary but does not substitute for device acceptance where interaction, lifecycle, timers, Health Connect or visual behavior is involved.

## v0.9.3 acceptance boundary

Repository-level contracts and deterministic tests are recorded in `V0_9_3_DOD.md`. Exercise DB v2 #250 and Android APK #802 passed on the final production-code checkpoint. Documentation-only closure commits followed; they do not change app runtime behavior. The signed APK artifact from the final frozen line is the baseline used to begin v0.9.4 device acceptance.


## v0.9.4 branch naming correction

The active line is `feature/v0.9.4`. The historical `feature/v0.9.1-training-plan` ref reached the same verified checkpoint during development but is no longer the canonical development branch. The previously divergent experimental v0.9.4 head `982653691f18f3b2313a2289a761b3126fec9dc4` is preserved on `archive/v0.9.4-pre-f02-f06-audit` for traceability.

## v0.9.4 F01/F05/F07 checkpoints — 2026-10-01

- `e4c4f208b8f6909aa76680b9b7316bef5f10ebc1` is the immutable F01 evidence green rollback point (`checkpoint/v0.9.4-f01-evidence-green`), with Exercise DB v2 #284 and Android APK #837 successful.
- `d182d34e81b8ef678675002462467bf41672f66b` is the subsequent F05/F07 candidate pushed by fast-forward to `feature/v0.9.4`. It introduces plan schema v5 intent-only persistence and execution-time materialization. CI was still pending when this ledger entry was written and must be recorded separately after completion.

## v0.9.4 F05/F07 green verification — 2026-10-01

- Verified HEAD: `fcfcbb200da73a6fa2475d5c29ee63eeeaf05ecf`.
- Exercise DB v2 #286 — SUCCESS.
- Android APK #839 — SUCCESS.
- F05 location-independent plan persistence/materialization is repository-level verified.
- F07 future-only regeneration/domain persistence is repository-level verified; explicit confirmation and physical-device UX remain acceptance work.
- Next dependency-ordered block: F08 Adaptive Coach with/without active plan. Existing implementation seam is present, but its remaining plan fixtures must be aligned to the intent-only contract before F08 can be marked verified.

## v0.9.4 F08 green verification — 2026-10-01

- Verified SHA: `f96d426964630c354c2aa4f08880c1caceb232be`.
- Frozen rollback: `checkpoint/v0.9.4-f08-green`.
- Exercise DB v2 #288 — SUCCESS.
- Android APK #841 — SUCCESS.
- Adaptive Coach regression contract now covers no-plan, clean active plan, disrupted plan, completed evidence and low-readiness precedence using intent-only planned sessions.
- No production-code change was required for F08 closure; the existing conservative plan-aware behavior passed the exact-SHA gates after test/contract cleanup.
- Next block: F09 monthly calendar audit.

## v0.9.4 F09/F10 green verification — 2026-10-01

- F09 verified SHA: `801f4a827933cecaeb5f1780e016a62735f388a5`; checkpoint `checkpoint/v0.9.4-f09-green`; Exercise DB v2 #290 and Android APK #843 successful.
- F10 verified SHA: `1ec072b36e2cde04bf16a7d8a190a28c6c276f44`; checkpoint `checkpoint/v0.9.4-f10-green`; Exercise DB v2 #291 and Android APK #844 successful.
- F09 centralizes calendar action policy and covers all planned-workout states, including dormant ADAPTED presentation.
- F10 centralizes Home plan presentation and aligns deterministic same-day workout selection across Home and the legacy resolver.
- Next: close F11 regression evidence, then run the global F01–F11 pre-CI audit.

## v0.9.4 F01–F11 final repository checkpoint — 2026-10-01

- Runtime SHA: `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`.
- Immutable rollback: `checkpoint/v0.9.4-f01-f11-green`.
- Exercise DB v2 #292: success.
- Android APK #845: success; PR debug APK uploaded. Signed-release restore/build/signature/upload steps were skipped because the run was triggered by `pull_request`.
- F11 strengthened empty/regeneration/pinned-position regression evidence is green on the final runtime SHA.
- Release remains open until a signed v0.9.4 APK is produced and signature-verified from the exact runtime SHA, followed by targeted physical-device acceptance of all DEVICE_PENDING clauses.

## v0.9.4 closure / v0.9.5 handoff — 2026-10-01

- v0.9.4 runtime is frozen at `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`; checkpoint `checkpoint/v0.9.4-f01-f11-green`.
- CI artifact #845 provides the exact-SHA debug APK used as the v0.9.4 deliverable.
- Remaining signed-release and physical-device/UI acceptance are transferred to v0.9.5.
- v0.9.5 must branch from the final v0.9.4 frozen line and inherit all DEVICE_PENDING acceptance items before adding/closing further release scope.
