# Hybrid Fitness Coach — V0.9.1 persistent DoD

## RC2 checkpoint — 29 settembre 2026

This checkpoint supersedes older CI labels below; those notes document earlier commits. RC2 remains **not ready**. PR CI #354 is green at `0f5309d30fac5d14a195a3db291406e2bf387173`: unit tests, Android lint and PR debug APK build passed. Signed-release steps are intentionally skipped on the PR workflow, so this run is not signed-artifact evidence.

| Area | Current evidence | Remaining gate |
|---|---|---|
| Persistence and recovery | Atomic session, linked-plan and active-runner finalization; recovery and rollback unit tests; push #341 and PR #342 green at `6875da4` | Install and exercise interrupted save/restart/recovery on device |
| History and workout detail | Structured actual sets, load and RPE, linked planned-session detail; deletion reconciles history and plan in one write | Device regression, including the previously reported reappearing deletion |
| Multi-week calendar | Configurable 1–52 weeks, reschedule cascade, restore original date, future skipped workout classification and Italian dates; planning tests | Device navigation and date/action acceptance |
| Runners and rest | Persisted set state and timers, custom set counter/skip/resume, explicit finish confirmation; runner tests | Device interaction, timer/background behavior and source-specific rest audit |
| Equipment and progression | Custom options limited to configured equipment plus bodyweight; skill links and progression tests | Device validation of generation, locked exercise behavior and navigation |
| Exercise evidence and video | General ACSM prescription evidence; verified ACE video for Plank only | Exercise-specific verified evidence registry and video coverage for the rest of the catalog |
| Release artifact | Push [#341](https://github.com/guglielmocitarella-rgb/HybridFitnessCoach/actions/runs/36537617414) and PR [#342](https://github.com/guglielmocitarella-rgb/HybridFitnessCoach/actions/runs/36537621571) green at `6875da4e26c3ca0f3dcdc653c224e454b88bafcd`; signed APK artifact digest `sha256:f7d4979878cba4ec2445438ad358640b12f2c7ec9fcde89336702d08cff8d192` | Recheck both workflows and artifact for the eventual final code commit; install and accept on device |

Confirmed UI defects corrected after the earlier checkpoints: future skipped sessions were shown as past; dates used ISO format; the custom runner hid the current set and exercise skip control; custom equipment showed unconfigured options; both runners saved without confirmation. The reported scroll direction, locking behavior and deletion reappearance remain **hypotheses pending device reproduction**, not verified fixes. General heuristic recovery intervals remain visible in the legacy catalog; do not label them exercise-specific sourced prescriptions.

Baseline: v0.9 final HEAD `933acc5c1d10875f53d366bcec62f40fe9b0509e`.
Branch: `feature/v0.9.1-training-plan`.
Legend: [x] verified; [~] partial; [ ] pending. Completion requires implementation, deterministic tests, E2E wiring, visible UX where applicable, and green CI.

| Gate | Status | Tests | E2E | UX | CI |
|---|---|---|---|---|---|
| app version 0.9.1 | [x] | n/a | [x] | n/a | green #263 |
| Evidence/provenance model | [~] registry + measurable coverage audit; ACSM general resistance-prescription evidence + Exercise Detail source visibility; exercise-specific migration pending | added | partial | visible | green #263 |
| Contextual Prescription Engine | [~] bounded adaptation + fail-closed verified numeric-baseline registry; exercise-specific baselines still pending | added | applied in custom and automatic runners when a verified baseline exists; resume preserves persisted targets | legacy status visible | green #398 |
| Multi-week Training Plan | [~] configurable 1–52 weeks, generated from profile and persisted; progression still pending | added | partial | visible | pending |
| Calendar past/future | [~] past/future list, actions and actual set detail for linked historical sessions | added | partial | visible | pending |
| Immutable workout history snapshots | [~] actual structured sets and full exercise definitions in newly saved plans; legacy plans auto-migrate on load when every exercise resolves; missing catalog entries remain unresolved | existing | partial | partial | pending |
| Skip/reschedule/recover semantics | [~] skip, manual move, and completion reconciliation; sourced recovery gap pending | added | partial | partial | pending |
| Exercise Detail 2.0 + stats + video | [~] real stats and video UI; one verified ACE Plank video; catalog-wide video coverage pending | added | partial | partial | pending |
| Runner integration | [~] today's and calendar-selected planned sessions launch with persistent identity; effective prescriptions persist and resume without recalculation; on-device flow pending | added | wired | partial | green #398 |
| Progression / Skill Tree integration | [~] Exercise Detail opens linked Skill Tree nodes | existing v0.9 | partial | visible | pending |
| Adaptive Coach uses history + plan | [~] history/readiness + persisted plan disruptions/upcoming sessions | added | wired Home | visible rationale | green #263 |
| Source-based rest/prescriptions | [~] ACSM general resistance baselines wired into both runners; exercise-specific audit remains pending | added | automatic/custom runner + persisted target/rest snapshot | visible in runner targets/rest | green #398 |
| Exercise database source audit | [~] deterministic coverage audit + ACSM general prescription source; exercise-specific entries pending | added | partial | n/a | green #263 |
| Equipment/generator + sequencing/locking | [~] v0.9 baseline preserved | existing | existing | existing | pending |
| Data migration/backward compatibility | [x] v0.9 session defaults, legacy active-runner defaults, plan schema v1→v2 migration, unresolved legacy exercises, malformed records and future schema versions covered fail-closed | added | persisted/reloaded | n/a | green #380 |
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
- Deleting the only linked historical session reopens its planned workout while retaining the other scheduled dates; duplicate linked records keep the workout completed. History deletion and linked-plan reconciliation are committed together by `AppStore.deleteSession`; reload regressions cover sole-linked, duplicate-linked and unlinked deletion cases.

- Provenance coverage is fail-closed: only verified registry/general source mappings count. Exercises without applicable verified evidence remain explicitly uncovered rather than receiving fabricated citations.
- Verified source migration started with ACSM GETP 12th ed., Chapter 5 resistance-training frequency/volume material (physical PDF pp. 353, 360). This is intentionally classified as prescription-only general evidence; it is not used as exercise-specific technique evidence.
- Exercise Detail now renders provenance coverage and verified source title/section/location; missing exercise-specific technique evidence is explicitly shown rather than inferred from generic instructions.

- CI #263 is green at HEAD `1b0f13a256dfd49d6eff36432110260f3115807e`; checklist statuses were reconciled against current code after that run.

- Numeric Prescription Engine wiring is fail-closed: general ACSM resistance prescription supports bounded numeric baselines only for eligible resistance exercises. Mobility and cardio are excluded even when bodyweight is the resistance mode; unsupported exercises retain legacy targets.

- Custom Runner consumes eligible contextual resistance prescriptions for set count, rep target and inter-set rest; unsupported exercises retain the legacy fallback.\n
- Exercise Detail presents a verified ACE Plank video linked from ACE's exercise video library, with an explicit unavailable state for other exercises; coverage of every exercise remains open.
- CI run 36446247422 passed at code HEAD `3de13bb98c00a49193ee46589026e86350c1868d`; signed artifact `HybridFitnessCoach-v0.9.1-signed` was present and unexpired at verification.

- Latest code run 36447095691 passed at `af9cf8dbfdbb141aa36e70dfbf92b575007d2f72`; signed APK artifact was extracted and verified present. The testable build is not a v0.9.1 release designation.

- PR CI #354 passed at HEAD `0f5309d30fac5d14a195a3db291406e2bf387173`; unit tests, Android lint and PR debug APK build are green. The paused runner now persists remaining rest seconds separately, with process-recreation and legacy compatibility regression coverage. Signed-release steps were skipped in this PR run and remain a separate final artifact gate.

- PR CI #380 passed at `2899e6e0e82afb883ff70b1723056c492d4aadab`; skill generation/regeneration now share the same eligibility pool, with locked-node, equipment and graph-integrity regressions. Backward-compatibility audit confirms deterministic coverage for legacy session/runner/plan data and future-schema fail-closed behavior.

- PR CI #398 passed at `f5fa6bae3dc114f6c894096c4907a1792af15be6`; automatic runner now materializes the same fail-closed effective prescription used by the prescription layer, and restored workouts explicitly retain their persisted set/target/rest snapshot rather than recalculating against new readiness/history.
