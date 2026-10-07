# Step 7A — Production Runtime Integration Audit and V3 Adapter Contract

Status: STEP_7A_COMPLETE

## 1–4. Entry state and freeze verification
- Initial HEAD: `d05b79a5049989d56e526dbfaba245dcddac8bed`
- Authoritative Step 6 promotion state: MATCH.
- Step 6 freeze report: MATCH; `records=1087`, `uniqueCanonicalIds=1087`, `TRUE_MISSING=0`, `materialization=3787/3787`, `prescriptionDependentPreserved=441`, `productionIntegrated=false`, `runtimeSwitchPerformed=false`.
- Frozen candidate path: `exercise-db-canonical/exercise-db-v3-final-candidate.json`.
- Frozen candidate SHA-256 recorded by the promotion commit/freeze report: `0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a`.
- The repository candidate was read successfully at the authoritative HEAD: 1087 records. Direct semantic scan finds exactly 441 `PRESCRIPTION_DEPENDENT` capability/prescription occurrences.
- No freeze contradiction found.

## 5. Current V2 load chain
Production boot is direct and not DI-backed:

`MainActivity.onCreate()`
→ `ExerciseDbV2.load(context)`
→ Android asset `exercise-db-v2/exercises.json`
→ `ExerciseDbV2.parseExercises(JSONArray)`
→ `List<ExerciseV2Record>`
→ `installExerciseDbV2(records)`
→ `ExerciseDbV2.install(records)` + unique-name/alias matching against `legacyExercises`
→ `ExerciseDbV2.toLegacyExercise(record, legacy?)`
→ global mutable `exercises: List<Exercise>`
→ planning/selection/custom/skill/runner consumers.

Parallel V2 registries are loaded at boot from:
- `exercise-db-v2/sources.json`
- `exercise-db-v2/progressions.json`
- `exercise-db-v2/programming_guidance.json`.

Load-time behavior: whole-file `readText()` + org.json DOM parsing; no lazy/streaming loader; installed records/registries are process-memory singleton state. Parse errors propagate; `installExerciseDbV2` only falls back to the legacy catalogue if the mapped result becomes empty.

## 6. Production composition
The production selector is `MainActivity.onCreate()`; it directly invokes V2. No repository/provider/DI boundary selects V2 vs V3. The mutable global `exercises` list is the effective runtime catalogue. Tests can install alternate V2 records through `installExerciseDbV2` / `ExerciseDbV2.install`.

The V3 package (`ExerciseV3Domain`, `V3AssetLoader`, `V3SelectionEngine`) is isolated and explicitly has no production routing.

## 7. Runtime consumer inventory
- Generation/selection/custom workouts: `MainActivity.kt`, `V091Planning.kt`, coaching helpers.
- Equipment/location: `EquipmentCatalog.kt`, `locationCompatible`, profile equipment filters.
- Capability/difficulty: `CapabilityDimensions.kt` and legacy level compatibility.
- Substitution/regeneration: coaching/regeneration functions and V3 isolated selection engine.
- Plans/custom/single exercise: launch/materialization paths in `MainActivity.kt` and launch persistence.
- Skill/calisthenics: `SkillCatalog.kt`, mastery/progression logic, skill workout focus.
- Runner: `WorkoutInfrastructure.kt`, `TimedRunner.kt`, `TimedExecutionControls.kt`.
- History/persistence: `AppStore.kt`, `WorkoutLaunchPersistence.kt`.
- Search/filter/detail: global `exercises`, name/alias lookup, equipment/group filters, exercise detail.

## 8–10. Runtime-required contract and V3 compatibility matrix
The compatibility matrix is domain-level: one classification per runtime semantic domain, avoiding double-counting individual records.

| Domain | Classification | Runtime/V3 finding |
|---|---|---|
| identity | DIRECT | canonicalExerciseId is authoritative; runtime already has nullable exerciseId/progressionKey seam |
| equipment | LEGACY_COMPATIBILITY | V3 modes/states/sets cannot be flattened to one display string authoritatively |
| location | DERIVED | eligibility derives from V3 equipment requirement + training environment |
| resistance | LEGACY_COMPATIBILITY | runtime enum has BODYWEIGHT/EXTERNAL_LOAD/ASSISTED/NONE; V3 is richer and unit-aware |
| load | PRESCRIPTION_DEPENDENT | must be resolved from prescription/context where applicable; never universalized |
| assistance | PRESCRIPTION_DEPENDENT | same; separate assistance semantics and units |
| fourCapability | PRESCRIPTION_DEPENDENT | V3 preserves independent capability states including prescription-dependent values |
| legacyLevel | LEGACY_COMPATIBILITY | retained for UI/backcompat only; forbidden as native V3 selection authority |
| category | DIRECT | V3 category is engine-critical |
| group | DIRECT | V3 group/groupState is authoritative when KNOWN; nullable state must remain explicit |
| movementPattern | LEGACY_COMPATIBILITY | V3 is 0..n + state; runtime is a scalar string |
| measure | LEGACY_COMPATIBILITY | runtime single enum cannot represent all V3 multidimensional prescription combinations |
| prescription | PRESCRIPTION_DEPENDENT | V3 dimensions/open-ended/state must be materialized with prescription context |
| skillBridge | LEGACY_COMPATIBILITY | runtime uses legacy skill names; V3 uses typed FAMILY/NODE relations |
| unresolved | DIRECT | explicit semantic states must survive unchanged |
| provenance | UNUSED_LEGACY | not required for selection; retain build-time/diagnostic evidence, do not make runtime eligibility depend on it |
| persistence | LEGACY_COMPATIBILITY | old name/snapshot schemas require explicit read compatibility and versioned new writes |

Counts:
- DIRECT: **4**
- DERIVED: **1**
- PRESCRIPTION_DEPENDENT: **4**
- LEGACY_COMPATIBILITY: **7**
- MISSING_RUNTIME_CONTRACT: **0**
- UNUSED_LEGACY: **1**

### Non-DIRECT mappings
1. Equipment: adapter must expose structured requirement semantics; a legacy display string may be produced only for old UI/snapshot compatibility.
2. Location: compute compatibility from environment equipment and structured V3 requirement.
3. Resistance: preserve source, load unit, assistance unit, quantification and semantic state; legacy enum is compatibility-only.
4. Load/assistance: resolve at prescription materialization time; preserve prescribed vs actual.
5. Four capabilities: never derive from legacy level; unresolved/prescription-dependent states fail closed or require context.
6. Legacy level: presentation/backward compatibility only.
7. Movement pattern: do not collapse multiple patterns into an authoritative scalar.
8. Measure: legacy runner enum is a projection of the actual V3 prescription dimensions, not the source of truth.
9. Prescription: keep multidimensional combinations (REPS/DURATION/DISTANCE/INTERVALS/LOAD/ASSISTANCE/ATTEMPT/QUALITY/OPEN_ENDED).
10. Skill bridge: typed relations map to existing SkillCatalog graph authority without replacing that graph.
11. Persistence: read old schemas; write versioned new snapshots with canonical ID + display snapshot; no destructive rewrite.
12. Provenance: runtime selection does not require full provenance.

## 11. PRESCRIPTION_DEPENDENT findings
The frozen candidate contains exactly 441 prescription-dependent occurrences in the audited capability/prescription surface. Current `Exercise` and `WorkoutSetState` can carry materialized scalar targets/load/assistance, but cannot represent an unresolved prescription-dependent semantic value. Therefore the semantic boundary must be **before** runner materialization: V3 remains rich; a prescription resolver receives athlete/session/readiness/workout context and emits immutable concrete runner targets. No scalar fallback is allowed at DB-load time.

Naïve integration loses semantics at `ExerciseDbV2.toLegacyExercise`, legacy `Exercise.level`, scalar `movementPattern`, `ResistanceMode`, `ExerciseMeasure`, and default `prescriptionFor(ex)`.

## 12–13. Identity and persistence
Canonical ID must become the primary exercise identity. Existing persistence already stores `WorkoutExerciseState.exerciseId` and an `Exercise.progressionKey` inside snapshots, but history and multiple lookup paths still retain exercise names.

Categories:
- stable/same identity: canonical IDs already present in current source-backed V2/progressionKey data and unchanged in V3;
- remappable identity: known legacy aliases/name-only references that resolve uniquely;
- legacy-only identity: current-model-only/legacy catalogue entries not represented by a canonical ID require compatibility handling, not silent deletion;
- V3-only identity: canonical V3 identities not previously reachable by persisted legacy name require normal new-ID handling;
- ambiguous mapping: normalized-name collisions must fail closed;
- persisted-data migration risk: active/resume, history, custom/single-exercise snapshots and name-only references.

Policy: **read old + write versioned new; no destructive rewrite**. No persisted-data migration is executed in 7A.

## 14. Asset / packaging feasibility
Current V2 assets are packaged under `app/src/main/assets` and loaded synchronously with whole-file `readText()`/DOM parsing. The frozen V3 candidate is ~19.05 MB and currently lives outside app assets. Packaging it unchanged would materially increase APK asset payload and the existing V2 loading strategy would imply at least the raw String plus parser/object graph plus mapped runtime graph in memory. The current code also risks duplicate representations if V3 DTO/domain + legacy `Exercise` copies coexist.

This is a concrete reason **not** to copy the candidate into assets and route production in 7A. No optimization is prescribed yet; Step 7B must choose packaging/loading with measurement and tests.

## 15. Existing relevant tests
Existing repository tests cover:
- V2 schema/load/integration: `ExerciseDbV2Test`, `ExerciseDbV2IntegrationTest`.
- V3 domain/loader: `RuntimeDomainModelV310ATest`, `V3AssetLoaderV310BTest`.
- V3 selection: `V3SelectionEngineV310CTest`.
- selection/adaptive/generation: `AdaptiveRegressionTest`, `CoachingIntegrationTest`, planning/regression suites.
- equipment: `EquipmentDomainContractTest`.
- skill: `SkillIntegrationTest`, `SkillMasteryPrescriptionTest`.
- runner/lifecycle: `WorkoutRunnerEngineTest`, `UniversalRunnerLifecycleTest`, `UniversalRunnerSnapshotTest`, `V095TimedRunnerTest`, runner input/capability tests.
- persistence/history: `RunnerPersistenceTest`, `AppStoreRecoveryTest`, `Rc2HistoryDetailTest`.
- prescription: `CapabilityPrescriptionContractTest`, `RecoveryPrescriptionContractTest`, `Rc2PrescriptionRegressionTest`.

## 16. Mandatory pre-switch tests
Before cutover:
- authoritative final-candidate loader test: exact 1087, unique IDs, expected SHA/version and 441 prescription-dependent preservation;
- V3 indexed repository/identity resolver tests including ambiguous legacy names fail-closed;
- V2↔V3 persisted identity compatibility fixtures for plan, active/resume, history, custom, skill/library and single-exercise origins;
- structured equipment/location parity including UNKNOWN, OPTIONAL, ONE_OF, ASSISTED_BY, EXTERNAL_LOAD and suspension_trainer;
- four-capability tests with no legacy-level scalar fallback;
- multidimensional prescription → runner materialization tests, including prescribed-vs-actual and load/assistance units;
- typed skill bridge tests preserving SkillCatalog graph/mastery authority;
- 640-scenario × 100-seed selection replay and zero-pool classification;
- rollback rehearsal: V3-written sessions remain readable after generation selection returns to V2;
- packaging/load memory/time measurement on Android before release gate;
- full existing non-DB/runner/adaptive regression suite.

## 17. Minimum V3 adapter contract
Use repository-native V3 components already present and add the smallest explicit boundary:

`frozen canonical V3 JSON`
→ **V3 asset source + V3AssetLoader**
→ **V3ExerciseRepository** (indexed by CanonicalExerciseId; immutable)
→ **LegacyIdentityResolver** (canonical ID → known alias → unique normalized name; ambiguous fails closed)
→ **V3 runtime services**:
  - structured equipment/location evaluator,
  - four-capability evaluator,
  - typed skill bridge,
  - prescription resolver/materializer
→ existing runner via immutable materialized workout snapshot.

Compatibility adapter responsibilities are limited to old persistence/UI surfaces. It must never be the authoritative selection model.

Validation: exact schema/count/unique IDs/semantic states at load. Error behavior: fail closed; do not silently fall back from malformed V3 to semantically flattened V2 inside a V3-selected run.

## 18. Reversible switch architecture
Introduce a small local implementation selector behind a repository/provider boundary, e.g. `ExerciseRuntimeProvider` with deterministic build/local configuration:
- `V2_BASELINE`
- `V3_CANONICAL`.

Keep V2 loader and old persistence readers during coexistence. The switch chooses generation/selection implementation only; persisted versioned snapshots remain readable independently of the current generator. No remote feature-flag infrastructure is required.

Rollback = select V2 generation while retaining V3-capable readers. This avoids user-data loss.

## 19–20. Files changed and checks
Step 7A documentation only:
- `exercise-db-canonical/step7a-runtime-integration-audit.md`

Checks performed:
- authoritative branch/HEAD read;
- Step 6 freeze report read and invariant comparison;
- frozen candidate read and structural semantic scan (1087 records; 441 prescription-dependent occurrences);
- repository tree recursive inventory;
- production V2 loader/composition inspection;
- runtime model/persistence inspection;
- V3 domain/loader/selection inspection;
- existing runtime migration/compatibility/test artifacts cross-checked against current implementation.

No APK built. No production runtime test was mutated or weakened.

## 21. Explicit confirmations
- V2 untouched: **YES**
- `ExerciseDbV2.load()` untouched: **YES**
- frozen V3 candidate untouched: **YES**
- Step 6 freeze report untouched: **YES**
- production composition untouched: **YES**
- runtime switch performed: **NO**
- Step 7B started: **NO**

## Final status
**STEP_7A_COMPLETE**
