# Hybrid Fitness Coach — Release / Build History

This file is the compact release ledger. GitHub commits and workflow runs remain the source of truth.

| Line | Status | Frozen / verified reference | CI evidence | Notes |
|---|---|---|---|---|
| v0.8 evolved | Historical | repository history | historical Android CI | Universal runner, structured sessions, persistence/recovery foundations. |
| v0.9.0 | Historical / closed | repository history | historical CI | Progression, sequencing and Adaptive Coach development line. |
| v0.9.1 | Historical / closed | repository history / PR #8 audit trail | historical RC2 CI | Multi-week plan, recovery, exercise detail/progression and persistence work. |
| v0.9.2 | Frozen | `release/v0.9.2` at `9c6229485ee622f3e0d825bc20c5adf58fb39d92` | green checkpoint recorded in release history | Closed predecessor baseline. |
| v0.9.3 | Frozen | `release/v0.9.3`; validated production-code checkpoint `365c07f643065df9dd68310362edd3ad3ed3236f` | Exercise DB v2 #250: success; Android APK #802: success | Repository acceptance complete. Interaction/device acceptance transfers to v0.9.4. |
| v0.9.4 | Active | verified HEAD `f96d426964630c354c2aa4f08880c1caceb232be`; checkpoint `checkpoint/v0.9.4-f08-green` | Exercise DB v2 #288 success; Android APK #841 success; PR #11 mergeable at verification time. | F01–F08 repository-level verified in their current scopes; F07 device UX pending; next block F09 monthly calendar. |

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
