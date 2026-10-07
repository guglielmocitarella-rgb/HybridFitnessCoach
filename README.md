# Hybrid Fitness Coach

Android/Kotlin/Jetpack Compose fitness coach for pesi, corpo libero e calisthenics.

## Current development lines

### Product/runtime
- **Active release:** v0.9.5
- **Active branch:** `feature/v0.9.5`
- **Active PR:** #14 — v0.9.5 multi-plan checkpoint
- **Frozen predecessor:** v0.9.4 runtime `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`
- **v0.9.4 rollback checkpoint:** `checkpoint/v0.9.4-f01-f11-green`
- **Compatibility policy:** backward compatibility with obsolete schemas/APIs/UI is out of scope unless explicitly reinstated.

### Exercise DB R5-R evidence line
R5-R evidence production has reached a **set-based data-complete V29 state** on the recovery line. The canonical R5 universe contains 3,835 semantic keys: 3,787 resolved in-universe, 48 terminal and 0 TRUE_MISSING. A separate set of 214 authoritative out-of-universe entries is preserved for final audit. Candidate DB/runtime DB/production path have **not** yet been integrated.

See `exercise-db-canonical/R5_R_FINAL_STATUS.md` before doing further R5 work. Do not restart evidence production while TRUE_MISSING is zero.

Do not use old PRs or old release DoDs as the current source of truth.

## Documentation map

| Document | Purpose |
|---|---|
| `PROJECT_STATUS.md` | Product branch, PR, verified baseline, current blockers and next step |
| `V0_9_5_DOD.md` | Binding v0.9.5 product roadmap and Definition of Done |
| `RELEASE_HISTORY.md` | Compact historical release ledger |
| `AGENTS.md` | Permanent engineering/repository rules |
| `exercise-db-canonical/R5_R_FINAL_STATUS.md` | Current R5-R set-based completion state and next-phase boundary |
| `exercise-db-canonical/R5_R_PERSISTENCE_GUARD.md` | Active R5-R persistence/set-membership guard |
| `exercise-db-canonical/README.md` | Canonical exercise DB status and artifact map |
| `exercise-db-v2/INTEGRATION.md` | Exercise DB integration contract |
| `V0_9_*.md` older than 0.9.5 | Historical evidence only |

## Architecture

```
Athlete + Capability Profile + Training Locations + Readiness/Recovery + History
                                  ↓
                    Active plans / Adaptive Coach
                                  ↓
                    Workout intent and scheduling
                                  ↓
                  choose TrainingLocation at start
                                  ↓
                materialize / generate concrete workout
                                  ↓
                           preview
                                  ↓
                     universal Workout Runner
                                  ↓
                  structured execution evidence
                                  ↓
                 History / Recovery / Adaptive Coach
```

A future plan stores intent, not a frozen exercise snapshot. Concrete exercises are materialized at execution time using the selected location and current athlete state.

## Current v0.9.5 direction

v0.9.5 is the active product/UX consolidation release. It includes the multi-plan model, then converges all workout origins on one start/materialization/preview/runner pipeline, improves Home/Piano/Progressi/Profilo, expands Capability Profile, completes Exercise Detail/technique access, and closes inherited device-acceptance and signed-release gates.

The exact product status belongs in `PROJECT_STATUS.md`; detailed requirements belong in `V0_9_5_DOD.md`. Exercise-database R5-R status belongs in `exercise-db-canonical/R5_R_FINAL_STATUS.md`. This README intentionally does not duplicate the historical v0.8/v0.9 checklists.
