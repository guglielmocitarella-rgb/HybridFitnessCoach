# Hybrid Fitness Coach — v0.9.4 Definition of Done

This is the binding release DoD after the v0.9.3 freeze. GitHub PR #11 is the v0.9.4 development audit trail. The v0.9.4 branch starts exactly from the frozen v0.9.3 release line.

## Baseline and release policy

- Predecessor: v0.9.3 frozen on `release/v0.9.3`.
- Validated v0.9.3 production-code checkpoint: `365c07f643065df9dd68310362edd3ad3ed3236f`.
- v0.9.3 CI evidence: Exercise DB v2 #250 SUCCESS; Android APK #802 SUCCESS.
- v0.9.4 audit baseline HEAD before implementation: `f409c45c5d9127120530cff72315b64e10747139`.
- Baseline v0.9.4 CI at that HEAD: Exercise DB v2 #258 SUCCESS; Android APK #810 SUCCESS.
- Green CI is necessary but never sufficient for release acceptance.
- Compatibility policy and engineering rules: see `AGENTS.md`.
- Status vocabulary: `VERIFIED`, `IMPLEMENTED_NOT_DEVICE_VERIFIED`, `PARTIAL`, `BLOCKED`, `DEVICE_PENDING`.
- Cross-cutting audit and technical plan are complete; implementation proceeds finding-by-finding with static pre-CI audit and exact-SHA gates.

## Frozen v0.9.4 backlog — 11 findings

### F01 — Exercise descriptions and execution contract
**Current:** repository-level F01 enrichment/evidence work is integrated and green at `e4c4f208…`: evidence tier, validator and regression coverage are present. Physical Exercise Detail acceptance remains pending.

**Expected:** every selectable exercise has usable Italian description/setup/execution content owned by the canonical exercise contract. Runtime must not silently manufacture exercise-specific technique from the exercise name.

**Acceptance:** catalog validation rejects missing required user-facing execution fields for selectable exercises; Android integration proves those fields survive DB loading; Exercise Detail renders DB-owned content. Deterministic regression required; final UI behavior is `DEVICE_PENDING` until physical-device retest.

### F02 — Recovery prescription must be source/effective and non-legacy
**Current:** source/effective recovery propagation is repository-verified through generation, runner materialization and persistence; physical timer behavior remains pending.

**Expected:** set and transition recovery come from one explicit prescription contract with traceable source/effective values and controlled fallback; no arbitrary legacy default may override canonical/programming guidance.

**Acceptance:** tests cover representative strength, hypertrophy, calisthenics/skill and fallback cases; generated plan, custom workout, runner and persisted snapshots preserve the same effective recovery. No unexplained fixed recovery jump. Device timer display remains `DEVICE_PENDING`.

### F03 — Equipment taxonomy is canonical, specific and user-readable
**Current:** canonical equipment IDs and `TrainingLocation` are the repository-level contract; localized labels no longer drive compatibility logic and Machine/Machines duplication is regression-protected.

**Expected:** canonical equipment IDs are domain data; localized labels are presentation only. Machine-based exercises use specific machine requirements where the source/catalog supports them. Selectable equipment is catalog-derived, deduplicated and alphabetically displayed.

**Acceptance:** validator/integration tests prove every exercise equipment token maps to a canonical selectable capability or an explicitly non-selectable requirement; no logic depends on localized display strings; catalog-wide compatibility tests pass.

### F04 — Custom Workout selects a training location, not a manual equipment list
**Current:** Custom Workout generation is based on `TrainingLocation`/capabilities rather than the removed `EquipmentProfile` contract. Final interaction remains device-pending.

**Expected:** the user selects the place/profile where the session will be performed; available equipment is derived from that profile. Series, reps, recovery, loads and timing remain generated automatically from athlete state.

**Acceptance:** generation API receives a location/capability context rather than an ad-hoc UI equipment list; changing location changes eligible exercises deterministically; Compose/device verification confirms the equipment checklist is gone from Custom Workout.

### F05 — Training Plan is location-independent; each session adapts at execution
**Current:** repository-level verified on `fcfcbb200…`: plan schema v5 persists session intent only; future plan rows do not persist exercise snapshots/names, and exercises are materialized at execution against the current location/capability/readiness context. Exercise DB v2 #286 and Android APK #839 are green. Device execution at different locations remains pending.

**Expected:** plan creation does not ask for a training location. The plan stores training intent/prescription independently from location. When a planned session is started, the chosen current location adapts only that session while preserving plan intent, progression and completed history.

**Acceptance:** domain tests create a plan without location, execute equivalent sessions under different location profiles and prove compatible substitutions without rewriting completed/past sessions or unrelated future history. Location is never a required PlanConfiguration field.

### F06 — Fitness/Capability Profile replaces coarse self-declared level
**Current:** coarse `AthleteProfile.level` has been removed from current model/persistence/decision paths. Assessed training experience plus Push/Pull capability dimensions drive eligibility with conservative unassessed behavior.

**Expected:** exercise difficulty eligibility is derived from a structured Fitness/Capability Profile based on meaningful baseline capabilities/tests. The assessment must use exercises appropriate for establishing a base level and must constrain generated difficulty without pretending one global label measures every capability.

**Acceptance:** explicit capability dimensions and deterministic classification/eligibility tests exist; assessment exercises and thresholds have documented provenance (`SOURCE_BASED`, `APP_RULE`, `FALLBACK`, or `UNVERIFIED`); generator cannot select an exercise above the relevant demonstrated capability solely because a fallback says “Intermedio”.

### F07 — Existing multi-week plan is editable with controlled future regeneration
**Current:** domain/persistence support for editable configuration and controlled future-only regeneration is repository-level verified on `fcfcbb200…`, preserving completed/partial/past evidence. Explicit confirmation/device UX remains pending.

**Expected:** goal, weekly frequency/days and priority skills remain editable after creation. A controlled regeneration updates only eligible future sessions; completed/partial history and immutable execution evidence remain intact.

**Acceptance:** domain tests cover each editable field, persistence, regeneration boundary and completed-session protection. UI requires an explicit review/confirmation before destructive future regeneration. Device verification required.

### F08 — Adaptive Coach works with or without an active plan
**Current:** coaching has history-only logic plus optional plan adjustments, but Home/session generation paths must be audited to ensure absence of a TrainingPlan never disables useful adaptive generation.

**Expected:** Adaptive Coach derives a valid recommendation from current capability, readiness/recovery and actual history even when no multi-week plan exists; an active plan adds context rather than becoming a prerequisite.

**Acceptance:** deterministic tests cover no-plan/new-user, no-plan/history, plan-present and unavailable Health Connect cases; no fabricated progression signal is introduced when evidence is insufficient.

### F09 — Real monthly calendar for the multi-week plan
**Current:** plan scheduling exists in the domain, but the required scrollable monthly calendar presentation is absent/incomplete.

**Expected:** a graphical monthly calendar can navigate months and exposes planned, rescheduled, completed, partial and skipped sessions with coherent day/session actions, cascading reschedule and restore-original-date behavior.

**Acceptance:** calendar state mapping is unit-testable independently of Compose; reschedule/restore/completed protection regressions remain green; final navigation, markers and actions are `DEVICE_PENDING`.

### F10 — Home plan presentation and iconography are coherent
**Current:** the multi-week plan section on Home has inconsistent presentation/icons and does not provide the intended clear plan-at-a-glance hierarchy.

**Expected:** Home communicates current plan state, next/today session and relevant actions consistently; icons have one meaning each and match the rest of the app.

**Acceptance:** presentation state is derived from plan domain state rather than duplicated mutable UI state; Compose/device acceptance checks visual hierarchy, labels and icon semantics. `DEVICE_PENDING`.

### F11 — Generated-workout preview always opens at the first exercise
**Current:** implementation already has an explicit `previewGeneration` reset boundary. `LaunchedEffect(previewGeneration)` resets preview scroll, and `previewNavigationAfterGeneration()` derives selection from the newly generated list. Generation/regeneration set the first new exercise as current while pinned exercises are preserved. Strengthened regression evidence is pending CI.

**Expected:** every newly generated/regenerated preview starts from exercise 1 while preserving only intentionally locked exercises; summary/Adaptive Coach/regeneration controls remain fixed and exercise list scrolls top-to-bottom.

**Acceptance:** preview state has an explicit reset boundary keyed to generation identity; regression test covers regeneration/locked exercises and prevents stale selected/scroll state from leaking. Final scrolling behavior is `DEVICE_PENDING`.

## Verified implementation checkpoints — 2026-10-01

Latest immutable green rollback checkpoint: `1ec072b36e2cde04bf16a7d8a190a28c6c276f44` (`checkpoint/v0.9.4-f10-green`).

CI evidence at that checkpoint:
- Exercise DB v2 #284 — SUCCESS.
- Android APK #837 — SUCCESS.

Repository-level verified progress at/before the checkpoint:
- **F01 — VERIFIED repository-level:** evidence tier, validator and deterministic tests integrated; device presentation remains pending.
- **F02 — VERIFIED repository-level:** source/effective recovery reaches runner prescriptions and persisted active-workout evidence.
- **F03/F04 — VERIFIED foundation/repository-level:** `TrainingLocation` is canonical; obsolete `EquipmentProfile` and localized matching debt removed.
- **F06 — VERIFIED repository-level:** coarse athlete level removed; multidimensional capability drives eligibility/prescription.

Current verified HEAD: `1ec072b36e2cde04bf16a7d8a190a28c6c276f44` (`checkpoint/v0.9.4-f10-green`).
- **F05 — VERIFIED repository-level:** schema 5 plan persistence is intent-only and execution-time materialization is location-aware.
- **F07 — VERIFIED domain/persistence:** future regeneration preserves completed/partial/past evidence; explicit confirmation/device UX remains pending.
- **F08 — VERIFIED repository-level:** deterministic no-plan/active-plan/disruption/completed/readiness behavior is covered with intent-only fixtures.
- Exercise DB v2 #291 — SUCCESS.
- Android APK #844 — SUCCESS.
- PR #11 was open and mergeable at verification time.
- **F09 — VERIFIED repository-level:** monthly calendar presentation/action policy is covered on `801f4a827…`; device interaction acceptance remains.
- **F10 — VERIFIED repository-level:** Home plan presentation and deterministic today/next selection are covered on `1ec072b36…`; device presentation acceptance remains.
- **F11 — IMPLEMENTED_NOT_CI_VERIFIED:** reset boundary exists; strengthened empty/regeneration/pinned-position regression evidence is the current candidate.
- **Next:** global F01–F11 pre-CI audit before the final candidate push.

These statuses do not mark v0.9.4 complete. Remaining acceptance clauses and F01–F11 device/UI requirements continue to apply.

## Cross-cutting audit findings at baseline HEAD

The baseline already exposes several structural coupling points that must be removed before UI work:

1. `ExerciseDbV2.toLegacyExercise()` translates canonical equipment IDs into localized strings and supplies legacy level/recovery defaults.
2. ~~`PrescriptionContext` still accepts a single `athleteLevel:String`.~~ Resolved at the verified checkpoint: prescription consumes a numeric relevant capability level derived from the structured Capability Profile.
3. ~~`TrainingPlan` persisted concrete future exercise snapshots.~~ Resolved on candidate `d182d34e…`: persisted future sessions are intent-only and realization occurs at execution.
4. ~~`AppStore` persisted concrete plan snapshots.~~ Resolved on candidate `d182d34e…`: plan schema v5 persists intent only; active/history evidence remains concrete.
5. Coaching supports optional `TrainingPlan?`; F08 is repository-verified and this seam must remain regression-protected.
6. Reschedule/completion protection remains regression-protected after the F05/F07 model change.

## Mandatory implementation order

1. **DB / Exercise contract:** F01 + F02.
2. **Equipment / Location domain:** F03 + F04 + F05.
3. **Capability Profile:** F06.
4. **Prescription / Recovery integration:** close all F02/F06 propagation gaps.
5. **Training Plan domain and persistence:** F05 + F07, preserving completed-session evidence.
6. **Adaptive generation:** F08 and location-aware realization of a planned session.
7. **Calendar / Home UI:** F09 + F10.
8. **Preview/UI polish:** F11.
9. **Regression suite and CI gates.**
10. **Signed APK and targeted physical-device acceptance of F01–F11.**

## Verification matrix and gate policy

Every finding that can be expressed as deterministic domain/data behavior must receive a regression test before it can be marked `VERIFIED`. Compose-only or physical interaction behavior is marked `DEVICE_PENDING` after implementation, never `VERIFIED` from JVM tests or a green APK build alone.

Required automated gates for the final candidate:
- Exercise DB deterministic build + validator + catalog contract tests.
- Android JVM regression suite.
- Android lint.
- Debug/release build as applicable.
- Persistence round-trip/current-schema integrity tests.
- Equipment/location compatibility tests.
- Capability eligibility tests.
- Prescription/recovery propagation tests.
- Plan edit/regeneration/completed-session-protection tests.
- Adaptive generation with and without plan.
- Calendar presentation-state tests.
- Preview reset-state tests.

## Release gate

v0.9.4 is not complete until:
1. all F01–F11 are implemented with required automated regressions;
2. final exact-SHA Exercise DB and Android CI are green;
3. a new signed APK is produced and tied to that exact SHA;
4. physical-device acceptance explicitly passes all F01–F11 marked `DEVICE_PENDING`;
5. no green CI run is reported as proof of device acceptance.
