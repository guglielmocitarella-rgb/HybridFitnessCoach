# Hybrid Fitness Coach — Project Status

**Version:** v0.9.5  
**Branch:** `feature/v0.9.5`  
**Pull request:** #14 — `v0.9.5 — multi-plan checkpoint`  
**Repository-verified P00 checkpoint:** `7dc3de6da7931173550618d7f63da34f5bfa4861`  
**Source of truth:** current GitHub HEAD + this file + `V0_9_5_DOD.md`.

## Frozen predecessor

v0.9.4 runtime is frozen at `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`, checkpoint `checkpoint/v0.9.4-f01-f11-green`.

Repository evidence:
- Exercise DB v2 #292 — SUCCESS
- Android APK #845 — SUCCESS
- F01–F11 repository-level gates closed
- signed release and physical-device clauses intentionally transferred to v0.9.5

No new product work belongs on v0.9.4.

## Current v0.9.5 candidate

P00 multi-plan foundation is **VERIFIED at repository level** on `7dc3de6da7931173550618d7f63da34f5bfa4861`.

Evidence:
- Android APK #857 — **SUCCESS** on that exact SHA; the workflow includes JVM unit tests, Android lint and PR debug APK build.
- deterministic tests cover persistent independent enable/disable across reload;
- disabled plans are excluded from enabled-plan loading and active coaching inputs;
- Adaptive Coach aggregates enabled plans;
- same-day workouts from distinct plans remain distinct;
- finished sessions update only the owning plan through `plannedWorkoutId`;
- deleting one plan preserves other plans and already-recorded history;
- plan IDs are unique and obsolete single-plan Home resolution contracts were removed.

Exercise DB v2 was not changed by the P00 closure commits; its workflow is now path-filtered and remains independently validated when DB inputs change.

Physical interaction/visual acceptance is **not** implied by P00 repository verification and remains under the v0.9.5 device gates.

## P01 differential audit

P01.1 is **PARTIAL / IMPLEMENTED_NOT_DEVICE_VERIFIED**:
- top-level navigation already is Oggi / Piano / Progressi / Profilo;
- Luoghi/attrezzatura and Libreria are secondary sections under Profilo;
- athlete profile is no longer hidden in Progressi;
- plan creation/configuration belongs to Piano rather than Home.

Still pending:
- physical verification of accessibility, scroll, back navigation, empty/error states and destructive actions;
- complex dialog-based flows still exist and must be reduced where they are acting as screens.

P01.2 is **PARTIAL**. The main architectural gap is the workout-start pipeline: origins do not yet all pass through one explicit choose-location -> materialize/generate -> preview -> universal Runner gate. In particular, library/single-exercise execution still constructs a workout directly and opens `WorkoutDialog`.

**Next engineering step:** implement the universal workout-start gate/pipeline foundation and route one origin at a time through it, preserving the existing universal execution/persistence behavior. Start with library/single-exercise because it exposes the current bypass clearly, then migrate plan/extra/custom/skill origins without creating parallel runners.

## Active roadmap

The binding scope is `V0_9_5_DOD.md`.

Execution order from the current checkpoint:
1. P01.2/P04 universal workout-start pipeline foundation;
2. complete P01 interaction/device acceptance as flows become screen-based;
3. P02/P03 Home + Plan/calendar UX;
4. P05 universal Runner, exercise technique access and timed-exercise preparation countdown;
5. P06 custom workout on the common pipeline;
6. P07 Capability Profile 2.0;
7. P08 Exercise Detail/database/progressions;
8. P09/P10 Progress/History/Profile;
9. P11 onboarding;
10. P12 cleanup + inherited device acceptance;
11. P13 signed release/device acceptance/freeze.

## Status vocabulary

Use only:
- **VERIFIED** — supported by the required repository/device evidence for the stated scope;
- **IMPLEMENTED_NOT_CI_VERIFIED** — code exists but the exact candidate has not passed required CI;
- **IMPLEMENTED_NOT_DEVICE_VERIFIED** — repository gates pass, physical interaction still pending;
- **PARTIAL** — requirement only partly implemented;
- **BLOCKED** — cannot currently progress;
- **DEVICE_PENDING** — explicitly awaiting physical-device acceptance.

A green build never substitutes for physical-device behavior where interaction, lifecycle, timers, Health Connect or visual presentation matters.

## PR hygiene

#14 is the active development PR. #13 is superseded/closed; historical PRs #1, #8, #9, #10 and #12 were closed on 2026-10-01 as housekeeping; their Git history remains available for audit. PR #11 (v0.9.4) was already closed.
