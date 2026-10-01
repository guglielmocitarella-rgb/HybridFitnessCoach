# Hybrid Fitness Coach — Project Status

VERSION: v0.9.4
BRANCH: feature/v0.9.4
PR: #11
SOURCE OF TRUTH: GitHub branch `feature/v0.9.4`, `V0_9_4_DOD.md`, and the exact branch HEAD.
PREVIOUS RELEASES: v0.9.0–v0.9.2 are historical; v0.9.3 is frozen on `release/v0.9.3`.
COMPATIBILITY: backward compatibility is out of scope unless the owner explicitly reverses the policy.

## Verified green baseline

Immutable rollback checkpoint:
- `checkpoint/v0.9.4-f01-evidence-green`
- SHA `e4c4f208b8f6909aa76680b9b7316bef5f10ebc1`
- Exercise DB v2 #284 — SUCCESS
- Android APK #837 — SUCCESS
- F01 evidence tier/validator/tests integrated.
- Earlier repository-verified foundations preserved: F02 recovery propagation, F03 canonical equipment/location, F04 TrainingLocation-based custom generation, F06 multidimensional capability propagation.

## Current development candidate

Current verified branch HEAD after F05/F07 and documentation alignment:
- SHA `fcfcbb200da73a6fa2475d5c29ee63eeeaf05ecf`
- Code candidate `d182d34e81b8ef678675002462467bf41672f66b` plus documentation-only commits; no force push.
- Exercise DB v2 #286 — **SUCCESS**.
- Android APK #839 — **SUCCESS**.
- PR #11 — open and mergeable at verification time.
- Plan persistence schema: **v5 intent-only** for planned sessions.
- Future `PlannedWorkout` persistence no longer stores concrete exercise snapshots/names.
- Planned sessions are materialized against the current `TrainingLocation`, athlete capability, history and readiness at execution.
- Active/completed workout evidence remains concrete in runner/history state.
- Legacy plan snapshot fallback and schema 3/4 plan migration paths are intentionally removed under the no-backward-compatibility policy.
- Future plan regeneration preserves completed/partial/past evidence and changes only eligible future sessions.
- Static pre-CI audit covered the complete Kotlin tree (36 files) and removed stale snapshot-based regression fixtures before push.

## F01–F11 status

| Finding | Repository status | Remaining gate |
|---|---|---|
| F01 Exercise execution contract/evidence | VERIFIED on `e4c4f208…` | Device presentation acceptance |
| F02 Recovery prescription propagation | VERIFIED repository-level | Device timer acceptance |
| F03 Equipment taxonomy | VERIFIED repository-level | Device presentation acceptance |
| F04 Custom Workout uses TrainingLocation | VERIFIED foundation/repository-level | Device UX acceptance |
| F05 Location-independent Training Plan | VERIFIED repository-level on `fcfcbb200…` | Device execution at different locations |
| F06 Capability Profile | VERIFIED repository-level | Device assessment/UX acceptance |
| F07 Editable plan / future regeneration | VERIFIED domain/persistence on `fcfcbb200…` | Explicit UI confirmation/device acceptance |
| F08 Adaptive Coach with/without plan | PARTIAL — implementation seam exists | Next: intent-only fixture cleanup + deterministic with/without-plan audit + device acceptance |
| F09 Monthly calendar | PARTIAL | Complete presentation/interaction acceptance |
| F10 Home plan presentation/iconography | PARTIAL | UI cleanup + device acceptance |
| F11 Preview reset | PARTIAL | Explicit generation reset regression + device acceptance |

## Next work

F05/F07 exact-SHA gates are green. Freeze `fcfcbb200…` as the next rollback checkpoint before code changes. Continue with an F08-only static/domain audit: align coaching tests with intent-only `PlannedWorkout`, prove deterministic behavior with and without an active plan, and avoid touching F09–F11 until F08 is closed. Then proceed F09 → F10 → F11 → global F01–F11 audit → final exact-SHA CI → signed APK → physical-device acceptance.

## Status terminology

Use `VERIFIED`, `IMPLEMENTED_NOT_CI_VERIFIED`, `IMPLEMENTED_NOT_DEVICE_VERIFIED`, `PARTIAL`, `BLOCKED`, or `DEVICE_PENDING`. A green repository workflow is necessary but is never proof of physical-device behavior.
