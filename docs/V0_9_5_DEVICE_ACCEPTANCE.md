# v0.9.5 — device acceptance / owner verification

**Owner verification completed:** 2026-10-02 on the device-test candidate derived from `551c28535c01a28371fd2df2de975dc8265ecc0e` / Android APK #872.

This document records observed physical behavior. It does **not** declare v0.9.5 released: the verification exposed remediation items below, including one release-blocking calisthenics Skill bug.

## Observed OK

- Onboarding flow is usable; Health Connect connection is convenient and weight recovery works.
- General goal choices (strength, hypertrophy, general fitness) and Calisthenics sub-focus are acceptable.
- Plan rescheduling cascade works as desired: shifting a session shifts following sessions coherently; returning to the original date restores the chain.
- Extra workout generation, exercise details, pin/regenerate and replacement alternatives were observed working.
- Runner fixed top progress bar is appreciated; repetitions flow, load persistence between sets, next-set transition and exercise technique access are broadly useful.
- Plan can be finished without creating the initial proposed program.
- History/progress areas contain useful data and are usable as a base for the next remediation.

## Required remediation tree

### A — Onboarding / athlete profile
- **A1 Health Connect enrichment — VERIFY/API:** determine whether Health Connect can provide height and age/birth data; determine the correct source for display name. Import only data actually exposed/authorized. Weight import already observed working.
- **A2 Remove duplicate Health Connect step — UX:** once Health Connect has already been connected earlier in onboarding, do not present a redundant optional Health Connect step later. Target onboarding becomes 5 logical steps where appropriate.
- **A3 Capability assessment — UX/DATA:** independent axes such as horizontal push/pull and vertical push/pull are too generic. Explain each capability, offer representative catalog exercises and let the user record meaningful performance (e.g. controlled repetitions/hold where applicable) rather than opaque labels such as “not assessed / main / front raises”. Keep assessment optional and independent.
- **A4 First-plan transparency — UX:** if onboarding retains the first-program proposal, explicitly show which inputs drive it (goal, capability/level, selected training location/equipment, availability/days, duration and other actually-used inputs). Do not imply unused inputs.
- **A5 Training location semantics — BUG/UX:** clearly distinguish “default/current training location” from “configure locations”. Selecting Casa/Palestra/etc. must persist visibly and be used by generation.

### B — Plan creation / calendar
- **B1 Remove duplicate weekly-count input — UX:** if exact training weekdays are selected, derive “workouts per week” from those selected days rather than asking for the same information twice.
- **B2 Calendar long-press — UX:** implement the previously planned user-friendly long-press/action on a scheduled calendar session to edit/reschedule it.
- **B3 Preserve cascade behavior — REGRESSION:** current +1-day cascading reschedule behavior was owner-accepted and must be regression protected.

### C — Workout generation / Runner
- **C1 Runner layout/scroll — BUG/UX:** current exercise panel can open partially outside the useful viewport; editing reps/values and reaching “Complete set” requires confusing scrolling. Rework layout so current exercise, inputs and primary action are immediately understandable and reachable.
- **C2 Required resistance — DATA QUALITY:** when an exercise/set requires external resistance, block set completion until a valid load/assistance value has been entered. Bodyweight-only exercises remain valid without artificial load.
- **C3 Single-exercise unlimited sets — BUG/UX:** Library → Execute must not impose a fixed “1/3 … completed” prescription. Show “Set 1, Set 2, Set 3…” and continuously allow Complete set → recovery → next set until the user voluntarily ends the single-exercise session; no manual “Add set” gate.
- **C4 Preserve accepted Runner behavior — REGRESSION:** next-set transition, recovery/skip recovery, persisted load and technique access were owner-accepted.

### D — History / Progress / exercise analytics
- **D1 Remove redundant saved-summary/detail presentation — UX:** consolidate duplicate “saved partial/summary/detail” information into one clear hierarchy.
- **D2 Performance metric dash — BUG/UX:** investigate the unexplained “—” shown next to metrics such as best reps and max load; label the represented datum or remove the redundant placeholder.
- **D3 Exercise history detail — FEATURE:** tapping an executed exercise must open its detail with total performed reps, total volume where meaningful, chronological load/reps/time data and a graphical progression indicator.

### E — Profile / navigation
- **E1 Locations duplicate entry — UX:** choose one clean navigation model. If the active/default location card in Profile opens location management, remove the separate redundant “Locations” entry (or vice versa); do not keep two equivalent entry points.
- **E2 Profile visual cleanup — UX:** simplify grouping and hierarchy while retaining Athlete Profile configuration.

### F — Calisthenics Skill Tree — RELEASE BLOCKER
- **F1 Skill workout generation fails for all tested skills — CRITICAL BUG:** “Train this skill” currently returns “No compatible exercise, change focus or location” across the tested skill tree. Reproduce across skills and fix the selection/materialization pipeline.
- **F2 Location/equipment eligibility semantics — CRITICAL:** bodyweight movements (e.g. handstand/frog-stand-type work when no equipment is required) must be eligible in every location. Equipment-dependent progressions are eligible only when the selected location owns the required equipment (e.g. pull-up bar).
- **F3 Full catalog skill audit — DATA/TEST:** audit skill → progression → exercise links, required equipment and location filtering for the complete catalog; add automated coverage so every trainable skill has at least one reachable compatible path under a valid equipment configuration.

### G — Exercise catalog quality / graph
- **G1 Technique coverage — DATA:** verify all exercises have an execution description/instructions. Missing descriptions must be identified and completed with provenance appropriate to the catalog; do not silently invent source-specific technique.
- **G2 Relations coverage — DATA:** every exercise should expose meaningful related navigation where catalog evidence permits: easier/harder progression and/or relevant same-pattern/muscle-family alternatives. Do not fabricate a progression edge merely to satisfy coverage.
- **G3 Related exercise navigation — FEATURE:** easier/harder/alternative items displayed in exercise detail must be tappable and open the target exercise detail directly.
- **G4 Catalog integrity tests — TEST:** add checks for missing technique, dangling relation IDs, impossible equipment requirements, orphan trainable skills and non-navigable relation targets. Existing 587-movement / 113-edge baseline is not by itself sufficient evidence of semantic completeness.

## Gate state after owner verification

| Gate | State |
|---|---|
| Repository implementation P01–P11 | previously implemented; remediation now reopened where listed above |
| Automated candidate #872 | PASS on old candidate SHA |
| Physical owner verification | **COMPLETED WITH FINDINGS** |
| Skill Tree generation | **BLOCKED / CRITICAL** |
| Catalog completeness audit | **REQUIRED** |
| P12 acceptance | **FAILED → REMEDIATION REQUIRED** |
| P13 release/freeze | **BLOCKED** |
| Signed production release | **BLOCKED** (credentials/gate still separate) |

The old #872 green result remains historical evidence for `551c285...`; it must not be reused as proof for remediation commits. Every remediation candidate requires exact-SHA tests/CI and a focused physical retest.
