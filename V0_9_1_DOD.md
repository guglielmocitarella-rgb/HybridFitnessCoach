# Hybrid Fitness Coach — V0.9.1 persistent DoD
Baseline: v0.9 final HEAD `933acc5c1d10875f53d366bcec62f40fe9b0509e`.
Branch: `feature/v0.9.1-training-plan`.
Legend: [x] verified; [~] partial; [ ] pending. Completion requires implementation, deterministic tests, E2E wiring, visible UX where applicable, and green CI.

| Gate | Status | Tests | E2E | UX | CI |
|---|---|---|---|---|---|
| app version 0.9.1 | [x] | n/a | [x] | n/a | pending |
| Evidence/provenance model | [~] architecture; catalog migration pending | added | pending | pending | pending |
| Contextual Prescription Engine | [~] sourced baseline + bounded adaptation | added | pending | pending | pending |
| Multi-week Training Plan | [~] configurable 1–52 weeks, generated from profile and persisted; progression still pending | added | partial | visible | pending |
| Calendar past/future | [~] past/future list, actions and actual set detail for linked historical sessions | added | partial | visible | pending |
| Immutable workout history snapshots | [~] actual structured sets and full exercise definitions in newly saved plans; legacy plan snapshots depend on catalog until migrated | existing | partial | partial | pending |
| Skip/reschedule/recover semantics | [~] skip, manual move, and completion reconciliation; sourced recovery gap pending | added | partial | partial | pending |
| Exercise Detail 2.0 + stats + video | [~] real exercise stats; verified video metadata only | added | partial | partial | pending |
| Runner integration | [~] today's planned session launches with persistent identity; full calendar launch pending | added | partial | partial | pending |
| Progression / Skill Tree integration | [~] Exercise Detail opens linked Skill Tree nodes | existing v0.9 | partial | visible | pending |
| Adaptive Coach uses history + plan | [ ] | [ ] | [ ] | [ ] | [ ] |
| Source-based rest/prescriptions | [~] unsourced baseline rejected; catalog migration pending | added | pending | pending | pending |
| Exercise database source audit | [ ] | existing validators | pending | [ ] | [ ] |
| Equipment/generator + sequencing/locking | [~] v0.9 baseline preserved | existing | existing | existing | pending |
| Data migration/backward compatibility | [~] optional session and plan fields; upgrade regression pending | partial | partial | n/a | pending |
| UX/dashboard/calendar polish | [~] plan preview, duration choice, past/future actions | n/a | partial | partial | pending |
| Full regression + signed release APK | [~] latest green run before exercise snapshot persistence: 36424787394 | green at prior HEAD | partial | n/a | pending |

Audit notes:
- Repository/code/CI are authoritative; stale README status is not evidence.
- Existing v0.9 runner, progression, sequencing, Health Connect, equipment and catalog are preserved incrementally.
- No technical exercise facts are added without a real project source. Evidence architecture permits missing metadata while preventing an unsourced baseline from being represented as source-based.

- The runner stores the planned workout ID in active and historical sessions. Ambiguous or custom sessions do not consume a planned workout by exercise overlap alone.
- Automatic completion currently uses no assumed physiological recovery gap; contextual, sourced scheduling remains pending.
- The source catalog and exercise videos still require verified primary references. A green CI build is not a v0.9.1 release gate by itself.

- Plan schema v2 stores complete exercise definitions per planned workout; schema v1 loads by catalog name and is upgraded on the next save. Old sessions with no planned workout ID cannot be linked unambiguously to a historical plan.
