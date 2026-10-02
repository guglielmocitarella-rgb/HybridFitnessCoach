# Hybrid Fitness Coach — Project Status

**Version:** v0.9.5 (versionCode 95)
**Branch:** `feature/v0.9.5` — PR #14, draft pending physical acceptance
**Revision:** the commit containing this file; read its exact HEAD/Actions SHA before installing its APK.
**Audited input HEAD:** `58e085b5b9f8bc2d909777d6a98ecd7970f80ce5` — Android APK #871 SUCCESS.
**Updated:** 2026-10-02

## Preserved foundation

v0.9.4 remains frozen at `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07` (Exercise DB #292 / Android #845 SUCCESS).
P00 remains repository VERIFIED on `7dc3de6da7931173550618d7f63da34f5bfa4861` and regression protected here: independently enabled plans, same-day collisions, plannedWorkoutId ownership, history preservation and disabled-plan exclusion.
Existing equipment/materialization, prescription/coaching engines, progression graphs, current-schema corruption recovery and optional Health Connect were preserved. The canonical exercise DB was audited, not rewritten.

## Candidate scope

| Area | Implemented behavior | Acceptance state |
|---|---|---|
| P01 | Oggi/Piano/Progressi/Profilo; normal full-screen Compose flows; secondary library/location configuration | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P02 | Extra always offered, including scheduled days; no Home place/configuration duplication; refresh after saving | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P03 | Week/month calendar; readable weekdays/goals; 1–52 weeks; conditional Calisthenics skills; review before plan generation/regeneration | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P04 | Common origin → optional location → materialize → preview → Runner → history; single skips location and retains exact chosen exercise | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P05 | Actual-only completion; persisted timed preparation/execution/rest/pause; set RPE distinct from session effort; technique access; voluntary recovery; saved-session summary | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P06 | One common editable preview; pin/replace/delete/regenerate; unchanged occurrences preserved; duration and filters retained | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P07 | Ten independent optional assessment dimensions; pertinent movement gating; no aggregate difficulty or general-experience promotion | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P08 | DB-owned technique from library/preview/Runner; easier/current/harder neighbors; specific-source versus family-guidance label | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P09 | Actual history including assistance/rest/set RPE; weekly frequency/volume; chronological load/reps/time comparisons; skill progress | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P10 | Profile goal/frequency/skills and independent capability persisted; locations/library/Health Connect remain in Profile | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P11 | Resumable six-step optional onboarding; saved draft/profile; optional Health Connect; idempotent plan proposal/completion | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| P12 | Replaced dialog screens, duplicate Custom preview, unused load engine, old global assessment and Runner fallback removed; build/secret artifacts ignored | PARTIAL — cleanup implemented; DEVICE_PENDING inherited acceptance |
| P13 | Local test/build/lint/DB gates; exact-SHA Actions required before final APK delivery | PARTIAL — signed release BLOCKED; physical acceptance DEVICE_PENDING |

These implementation states describe repository/local evidence. Before the containing commit's exact Actions run succeeds, its CI state is IMPLEMENTED_NOT_CI_VERIFIED. A successful exact-SHA run changes that CI state only; it cannot verify physical behavior.

## Validation and provenance

Local JDK 17 / Gradle 9.6.0 / SDK 37:
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`: 403 tests, 0 failures/errors/skips; debug APK built; lint 0 errors, 2 warnings.
- Python DB tests: 9/9 pass.
- Canonical DB validation: OK, 587 movements, 113 progression edges, 14 sources, 0 errors. The 2,117 existing warnings remain disclosed; they concern coverage/metadata and were not hidden or converted into invented evidence.
- 423 movement records contain FAMILY_GUIDANCE and 164 SPECIFIC_SOURCE; this does not claim source-specific technique for every movement.
- Final review found and corrected timed skip leakage, paused recovery skipping, resistance entry during automatic timers, preview filter bypass and onboarding exit loss; additional cancellation/ownership boundaries are regression protected.
- Earlier tests requiring single-location selection, target→actual completion, pending exercise RPE, overall capability or discarded Extra configuration were explicitly replaced because the owner's v0.9.5 requirements supersede them. Skill/location test fixtures now assess the relevant independent axes; eligibility assertions were retained.
- Local bootstrap/proxy/JDK/SDK data and all build outputs stay outside committed source.

Assessment is an **APP_RULE**: optional self-reported controlled execution of catalog difficulty examples, with unknown axes conservatively limited. It is not a validated physiological test. No new physiological thresholds were invented. The old aggregate/rep-count assessment is removed.
Extra uses existing recovery/readiness/coaching ranking. Avoiding exact exercises planned today/tomorrow is an **APP_RULE** ranking penalty, not a claim of measured recovery or guaranteed noninterference.
Custom duration remains the existing **APP_RULE** estimate. Existing source-backed prescriptions/progression provenance were preserved. Supplied books were not used to invent new numeric rules.

## Release/device gate

No HFC release signing credentials are available in the local environment. PR Actions deliberately skips release signing. The best deliverable is a signature-verified **debug device-test APK**, not a production signed release. Signed release remains BLOCKED until authorized credentials are available.

The sole grouped physical checklist is in [docs/V0_9_5_DEVICE_ACCEPTANCE.md](docs/V0_9_5_DEVICE_ACCEPTANCE.md). P01–P12 physical checks and inherited F01–F11 checks remain DEVICE_PENDING. v0.9.5 is not fully VERIFIED or frozen.
