# Hybrid Fitness Coach — V0.9.1 persistent DoD
Baseline: v0.9 final HEAD `933acc5c1d10875f53d366bcec62f40fe9b0509e`.
Branch: `feature/v0.9.1-training-plan`.
Legend: [x] verified; [~] partial; [ ] pending. Completion requires implementation, deterministic tests, E2E wiring, visible UX where applicable, and green CI.

| Gate | Status | Tests | E2E | UX | CI |
|---|---|---|---|---|---|
| app version 0.9.1 | [x] | n/a | [x] | n/a | pending |
| Evidence/provenance model | [~] architecture; catalog migration pending | added | pending | pending | pending |
| Contextual Prescription Engine | [~] sourced baseline + bounded adaptation | added | pending | pending | pending |
| Multi-week Training Plan | [~] dated scheduler | added | pending generator/persistence | pending | pending |
| Calendar past/future | [ ] | [ ] | [ ] | [ ] | [ ] |
| Immutable workout history snapshots | [ ] | [ ] | [ ] | [ ] | [ ] |
| Skip/reschedule/recover semantics | [~] domain operations started | added | pending | pending | pending |
| Exercise Detail 2.0 + stats + video | [ ] | [ ] | [ ] | [ ] | [ ] |
| Runner integration | [ ] | [ ] | [ ] | [ ] | [ ] |
| Progression / Skill Tree integration | [ ] | existing v0.9 | pending v0.9.1 | [ ] | [ ] |
| Adaptive Coach uses history + plan | [ ] | [ ] | [ ] | [ ] | [ ] |
| Source-based rest/prescriptions | [~] unsourced baseline rejected; catalog migration pending | added | pending | pending | pending |
| Exercise database source audit | [ ] | existing validators | pending | [ ] | [ ] |
| Equipment/generator + sequencing/locking | [~] v0.9 baseline preserved | existing | existing | existing | pending |
| Data migration/backward compatibility | [ ] | [ ] | [ ] | n/a | [ ] |
| UX/dashboard/calendar polish | [ ] | n/a | [ ] | [ ] | [ ] |
| Full regression + signed release APK | [ ] | [ ] | [ ] | n/a | [ ] |

Audit notes:
- Repository/code/CI are authoritative; stale README status is not evidence.
- Existing v0.9 runner, progression, sequencing, Health Connect, equipment and catalog are preserved incrementally.
- No technical exercise facts are added without a real project source. Evidence architecture permits missing metadata while preventing an unsourced baseline from being represented as source-based.
