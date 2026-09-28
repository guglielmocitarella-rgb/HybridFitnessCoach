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
| Immutable workout history snapshots | [~] actual structured sets and full exercise definitions in newly saved plans; legacy plans auto-migrate on load when every exercise resolves; missing catalog entries remain unresolved | existing | partial | partial | pending |
| Skip/reschedule/recover semantics | [~] skip, manual move, and completion reconciliation; sourced recovery gap pending | added | partial | partial | pending |
| Exercise Detail 2.0 + stats + video | [~] real stats and video UI; one verified ACE Plank video; catalog-wide video coverage pending | added | partial | partial | pending |
| Runner integration | [~] today's and calendar-selected planned sessions launch with persistent identity; on-device flow pending | added | partial | partial | pending |
| Progression / Skill Tree integration | [~] Exercise Detail opens linked Skill Tree nodes | existing v0.9 | partial | visible | pending |
| Adaptive Coach uses history + plan | [~] history/readiness + persisted plan disruptions/upcoming sessions | added | wired Home | visible rationale | green #263 |
| Source-based rest/prescriptions | [~] ACSM general resistance baselines in custom runner; legacy automatic runner and exercise-specific audit pending | added | pending | pending | pending |
| Exercise database source audit | [~] deterministic coverage audit + ACSM general prescription source; exercise-specific entries pending | added | partial | n/a | green #263 |
| Equipment/generator + sequencing/locking | [~] v0.9 baseline preserved | existing | existing | existing | pending |
| Data migration/backward compatibility | [~] optional session and plan fields; upgrade regression pending | partial | partial | n/a | pending |
| UX/dashboard/calendar polish | [~] plan preview, duration choice, past/future actions | n/a | partial | partial | pending |
| Full regression + signed release APK | [~] latest green run: 36446247422 at 3de13bb; on-device regression pending | green at prior HEAD | partial | n/a | pending |

Audit notes:
- Repository/code/CI are authoritative; stale README status is not evidence.
- Existing v0.9 runner, progression, sequencing, Health Connect, equipment and catalog are preserved incrementally.
- No technical exercise facts are added without a real project source. Evidence architecture permits missing metadata while preventing an unsourced baseline from being represented as source-based.

- The runner stores the planned workout ID in active and historical sessions. Ambiguous or custom sessions do not consume a planned workout by exercise overlap alone.
- Automatic completion currently uses no assumed physiological recovery gap; contextual, sourced scheduling remains pending.
- The source catalog and exercise videos still require verified primary references. A green CI build is not a v0.9.1 release gate by itself.

- Plan schema v2 stores complete exercise definitions per planned workout; schema v1 loads by catalog name and auto-upgrades on load only if all exercise names resolve; otherwise the original stored plan is preserved. Old sessions with no planned workout ID cannot be linked unambiguously to a historical plan.

- Calendar launch routes the selected planned workout ID through Home to the existing runner. An already active workout resumes instead of being replaced. Device-level interaction remains to be checked.
- Deleting the only linked historical session reopens its planned workout while retaining the other scheduled dates; duplicate linked records keep the workout completed. The deletion operation and plan write are not transactional.

- Provenance coverage is fail-closed: only verified registry/general source mappings count. Exercises without applicable verified evidence remain explicitly uncovered rather than receiving fabricated citations.
- Verified source migration started with ACSM GETP 12th ed., Chapter 5 resistance-training frequency/volume material (physical PDF pp. 353, 360). This is intentionally classified as prescription-only general evidence; it is not used as exercise-specific technique evidence.
- Exercise Detail now renders provenance coverage and verified source title/section/location; missing exercise-specific technique evidence is explicitly shown rather than inferred from generic instructions.

- CI #263 is green at HEAD `1b0f13a256dfd49d6eff36432110260f3115807e`; checklist statuses were reconciled against current code after that run.

- Numeric Prescription Engine wiring is fail-closed: general ACSM resistance prescription supports bounded numeric baselines only for eligible resistance exercises. Mobility and cardio are excluded even when bodyweight is the resistance mode; unsupported exercises retain legacy targets.

- Custom Runner consumes eligible contextual resistance prescriptions for set count, rep target and inter-set rest; unsupported exercises retain the legacy fallback.\n
- Exercise Detail presents a verified ACE Plank video linked from ACE's exercise video library, with an explicit unavailable state for other exercises; coverage of every exercise remains open.
- CI run 36446247422 passed at code HEAD `3de13bb98c00a49193ee46589026e86350c1868d`; signed artifact `HybridFitnessCoach-v0.9.1-signed` was present and unexpired at verification.

- Latest code run 36447095691 passed at `af9cf8dbfdbb141aa36e70dfbf92b575007d2f72`; signed APK artifact was extracted and verified present. The testable build is not a v0.9.1 release designation.
