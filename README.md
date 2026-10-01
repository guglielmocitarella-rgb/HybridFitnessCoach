# Hybrid Fitness Coach

Android/Kotlin/Jetpack Compose fitness coach for pesi, corpo libero e calisthenics.

## Current development line

- **Active release:** v0.9.5
- **Active branch:** `feature/v0.9.5`
- **Active PR:** #13 — v0.9.5 multi-plan foundation
- **Frozen predecessor:** v0.9.4 runtime `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`
- **v0.9.4 rollback checkpoint:** `checkpoint/v0.9.4-f01-f11-green`
- **Compatibility policy:** backward compatibility with obsolete schemas/APIs/UI is out of scope unless explicitly reinstated.

Do not use old PRs or old release DoDs as the current source of truth.

## Documentation map

| Document | Purpose |
|---|---|
| `PROJECT_STATUS.md` | Current branch, PR, verified baseline, current blockers and next step |
| `V0_9_5_DOD.md` | Binding v0.9.5 product roadmap and Definition of Done |
| `RELEASE_HISTORY.md` | Compact historical release ledger |
| `AGENTS.md` | Permanent engineering/repository rules |
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

The exact status belongs in `PROJECT_STATUS.md`; detailed requirements belong in `V0_9_5_DOD.md`. This README intentionally does not duplicate the historical v0.8/v0.9 checklists.
