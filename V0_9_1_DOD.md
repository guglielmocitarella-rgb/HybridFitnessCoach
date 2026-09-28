# Hybrid Fitness Coach — V0.9.1 persistent DoD
Baseline: v0.9 final HEAD `933acc5c1d10875f53d366bcec62f40fe9b0509e`.
Branch: `feature/v0.9.1-training-plan`.
Legend: [x] verified; [~] partial; [ ] pending. Completion requires implementation, deterministic tests, E2E wiring, visible UX where applicable, and green CI.

| Gate | Status | Tests | E2E | UX | CI |
|---|---|---|---|---|---|
| app version 0.9.1 | [x] | n/a | [x] | n/a | green #263 |
| Evidence/provenance model | [~] registry + measurable coverage audit; ACSM general resistance-prescription evidence + Exercise Detail source visibility; exercise-specific migration pending | added | partial | visible | green #263 |
| Contextual Prescription Engine | [~] bounded adaptation + fail-closed verified numeric-baseline registry; verified exercise baselines pending | added | applied in custom Runner when verified baseline exists | legacy status visible | pending |
| Multi-week Training Plan | [~] configurable 1–52 weeks, generated from profile and persisted; progression still pending | added | partial | visible | pending |
| Calendar past/future | [~] past/future list, actions and actual set detail for linked historical sessions | added | partial | visible | pending |
| Immutable workout history snapshots | [~] actual structured sets and full exercise definitions in newly saved plans; legacy plan snapshots depend on catalog until migrated | existing | partial | partial | pending |
| Skip/reschedule/recover semantics | [~] skip, manual move, and completion reconciliation; sourced recovery gap pending | added | partial | partial | pending |
| Exercise Detail 2.0 + stats + video | [~] real exercise stats; verified video metadata only | added | partial | partial | pending |
| Runner integration | [~] today's and calendar-selected planned sessions launch with persistent identity; on-device flow pending | added | partial | partial | pending |
| Progression / Skill Tree integration | [~] Exercise Detail opens linked Skill Tree nodes | existing v0.9 | partial | visible | pending |
| Adaptive Coach uses history + plan | [~] history/readiness + persisted plan disruptions/upcoming sessions | added | wired Home | visible rationale | green #263 |
| Source-based rest/prescriptions | [~] unsourced baseline rejected; catalog migration pending | added | pending | pending | pending |
| Exercise database source audit | [~] deterministic coverage audit + ACSM general prescription source; exercise-specific entries pending | added | partial | n/a | green #263 |
| Equipment/generator + sequencing/locking | [~] v0.9 baseline preserved | existing | existing | existing | pending |
| Data migration/backward compatibility | [~] optional session and plan fields; upgrade regression pending | partial | partial | n/a | pending |
| UX/dashboard/calendar polish | [~] plan preview, duration choice, past/future actions | n/a | partial | partial | pending |
| Full regression + signed release APK | [~] latest green run with calendar launch and history reconciliation: 36428260384 | green at prior HEAD | partial | n/a | pending |

Audit notes:
- Repository/code/CI are authoritative; stale README status is not evidence.
- Existing v0.9 runner, progression, sequencing, Health Connect, equipment and catalog are preserved incrementally.
- No technical exercise facts are added without a real project source. Evidence architecture permits missing metadata while preventing an unsourced baseline from being represented as source-based.

- The runner stores the planned workout ID in active and historical sessions. Ambiguous or custom sessions do not consume a planned workout by exercise overlap alone.
- Automatic completion currently uses no assumed physiological recovery gap; contextual, sourced scheduling remains pending.
- The source catalog and exercise videos still require verified primary references. A green CI build is not a v0.9.1 release gate by itself.

- Plan schema v2 stores complete exercise definitions per planned workout; schema v1 loads by catalog name and is upgraded on the next save. Old sessions with no planned workout ID cannot be linked unambiguously to a historical plan.

- Calendar launch routes the selected planned workout ID through Home to the existing runner. An already active workout resumes instead of being replaced. Device-level interaction remains to be checked.
- Deleting the only linked historical session reopens its planned workout while retaining the other scheduled dates; duplicate linked records keep the workout completed. The deletion operation and plan write are not transactional.

- Provenance coverage is fail-closed: only verified registry/general source mappings count. Exercises without applicable verified evidence remain explicitly uncovered rather than receiving fabricated citations.
- Verified source migration started with ACSM GETP 12th ed., Chapter 5 resistance-training frequency/volume material (physical PDF pp. 353, 360). This is intentionally classified as prescription-only general evidence; it is not used as exercise-specific technique evidence.
- Exercise Detail now renders provenance coverage and verified source title/section/location; missing exercise-specific technique evidence is explicitly shown rather than inferred from generic instructions.\n
- CI #263 is green at HEAD `1b0f13a256dfd49d6eff36432110260f3115807e`; checklist statuses were reconciled against current code after that run.

- Numeric Prescription Engine wiring is fail-closed: general ACSM prescription provenance does not authorize exercise-specific sets/reps/rest values. Until a verified numeric baseline is registered, Runner/Exercise Detail label those targets as legacy rather than source-based.

- Custom Runner now consumes verified contextual resistance prescriptions for set count, rep target and inter-set rest; unsupported exercises retain the legacy fallback and are labelled accordingly.\n