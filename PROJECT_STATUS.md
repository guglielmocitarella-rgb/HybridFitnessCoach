# Hybrid Fitness Coach — Project Status

**Version:** v0.9.5  
**Branch:** `feature/v0.9.5`  
**Pull request:** #13 — `v0.9.5 — multi-plan foundation`  
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

Pre-documentation HEAD: `2f339e93547cea38df7abec68ab476ca29f9bb0e`.

Implemented in the current multi-plan foundation:
- multiple plans can coexist;
- plans have independent identity/ownership;
- plans can be enabled/disabled;
- disabled plans do not contribute to active coaching;
- Adaptive Coach aggregates enabled plans;
- same-day sessions from different active plans are represented rather than silently collapsed;
- regression tests cover aggregate planned evidence and disruptions.

Verification on that exact HEAD:
- Exercise DB v2 #36896846427 — **SUCCESS**
- Android APK #36896846526 — **FAILURE**

Therefore the current v0.9.5 candidate is **IMPLEMENTED_NOT_CI_VERIFIED**, not a green checkpoint. Diagnose and close the Android failure before treating the multi-plan foundation as verified or moving to the next CI checkpoint.

## Active roadmap

The binding scope is `V0_9_5_DOD.md`.

Execution order:
1. close/verify multi-plan foundation;
2. P01 navigation and information architecture;
3. P02/P03 Home + Plan/calendar UX;
4. P04 universal workout-start gate;
5. P05 universal Runner, exercise technique access and timed-exercise preparation countdown;
6. P06 custom workout on the common pipeline;
7. P07 Capability Profile 2.0;
8. P08 Exercise Detail/database/progressions;
9. P09/P10 Progress/History/Profile;
10. P11 onboarding;
11. P12 cleanup + inherited device acceptance;
12. P13 signed release/device acceptance/freeze.

## Status vocabulary

Use only:
- **VERIFIED** — supported by the required repository/device evidence;
- **IMPLEMENTED_NOT_CI_VERIFIED** — code exists but the exact candidate has not passed required CI;
- **IMPLEMENTED_NOT_DEVICE_VERIFIED** — repository gates pass, physical interaction still pending;
- **PARTIAL** — requirement only partly implemented;
- **BLOCKED** — cannot currently progress;
- **DEVICE_PENDING** — explicitly awaiting physical-device acceptance.

A green build never substitutes for physical-device behavior where interaction, lifecycle, timers, Health Connect or visual presentation matters.

## PR hygiene

#13 is the only active development PR. Historical PRs #1, #8, #9, #10 and #12 were closed on 2026-10-01 as housekeeping; their Git history remains available for audit. PR #11 (v0.9.4) was already closed.
