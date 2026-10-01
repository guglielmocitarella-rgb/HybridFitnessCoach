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

Current branch HEAD after the F05/F07 plan-domain cleanup:
- SHA `d182d34e81b8ef678675002462467bf41672f66b`
- Fast-forwarded from the green baseline; no force push.
- CI status: **PENDING** at documentation update time. Do not report this SHA as green until exact-SHA workflows complete.
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
| F05 Location-independent Training Plan | IMPLEMENTED_NOT_CI_VERIFIED on `d182d34e…` | Exact-SHA CI + device execution at different locations |
| F06 Capability Profile | VERIFIED repository-level | Device assessment/UX acceptance |
| F07 Editable plan / future regeneration | IMPLEMENTED_NOT_CI_VERIFIED on `d182d34e…` | Exact-SHA CI + explicit UI confirmation/device acceptance |
| F08 Adaptive Coach with/without plan | PARTIAL / next domain audit | Deterministic closure + device acceptance |
| F09 Monthly calendar | PARTIAL | Complete presentation/interaction acceptance |
| F10 Home plan presentation/iconography | PARTIAL | UI cleanup + device acceptance |
| F11 Preview reset | PARTIAL | Explicit generation reset regression + device acceptance |

## Next work

Do not modify the pushed F05/F07 candidate while its exact-SHA CI is unresolved unless a compile/test blocker is reported. If CI is green, freeze a new checkpoint and continue in dependency order: F08 → F09 → F10 → F11 → global F01–F11 audit → final exact-SHA CI → signed APK → physical-device acceptance.

## Status terminology

Use `VERIFIED`, `IMPLEMENTED_NOT_CI_VERIFIED`, `IMPLEMENTED_NOT_DEVICE_VERIFIED`, `PARTIAL`, `BLOCKED`, or `DEVICE_PENDING`. A green repository workflow is necessary but is never proof of physical-device behavior.
