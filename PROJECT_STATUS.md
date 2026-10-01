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

Current verified runtime HEAD after F01–F11 closure:
- SHA `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`
- Immutable rollback: `checkpoint/v0.9.4-f01-f11-green`.
- Exercise DB v2 #292 — **SUCCESS**.
- Android APK #845 — **SUCCESS** (debug artifact; signed-release steps skipped on PR event).
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
| F08 Adaptive Coach with/without plan | VERIFIED repository-level on `f96d426…` | Device presentation/behavior acceptance |
| F09 Monthly calendar | VERIFIED repository-level on `801f4a827…` | Physical-device interaction acceptance |
| F10 Home plan presentation/iconography | VERIFIED repository-level on `1ec072b36…` | Physical-device presentation acceptance |
| F11 Preview reset | VERIFIED repository-level on `cfacd7e3…` | Physical-device scrolling/reset acceptance |

## Next work

F01–F11 automated repository gates are green on exact runtime SHA `cfacd7e3…`; `checkpoint/v0.9.4-f01-f11-green` is the immutable rollback. Exercise DB v2 #292 and Android APK #845 succeeded. The PR workflow produced a debug APK only; signed-release steps were skipped by event policy. Next: signed release APK from the exact runtime SHA, then targeted physical-device acceptance.

## Status terminology

Use `VERIFIED`, `IMPLEMENTED_NOT_CI_VERIFIED`, `IMPLEMENTED_NOT_DEVICE_VERIFIED`, `PARTIAL`, `BLOCKED`, or `DEVICE_PENDING`. A green repository workflow is necessary but is never proof of physical-device behavior.

## v0.9.4 closure

- Runtime baseline: `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`.
- Repository checkpoint: `checkpoint/v0.9.4-f01-f11-green`.
- CI: Exercise DB v2 #292 success; Android APK #845 success.
- Deliverable: CI debug artifact `HybridFitnessCoach-v0.9.4-device-test` from the exact runtime SHA.
- Remaining signed-release and DEVICE_PENDING acceptance are intentionally transferred to v0.9.5.
- v0.9.5 starts from the frozen v0.9.4 line; no v0.9.4 runtime changes are permitted after closure.
