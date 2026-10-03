# Exercise DB V3 — Engine Data Contract Audit

Baseline commit audited: `c743ee48303629ad245fb9fa1a9dd79063e05527`
Scope: V3.1 only. No mass enrichment and no runtime DB replacement.

## Integrity and preserved evidence

- Canonical records: **1087**
- Unique IDs: **1087**
- Missing IDs: **0**
- Duplicate IDs: **0**
- Progression edges: **113**
- Existing matching audit preserved: 1087/1087 processed in 12 checkpoints.
- Matching: HIGH_PROBABILITY 270; AMBIGUOUS 495; NO_MATCH 322; CERTAIN intentionally 0.
- Existing audit, checkpoints, manual-review queue, media/provenance report and qualitative review were not regenerated or deleted.

## Runtime architecture actually audited

The current app loads Exercise DB V2 through `ExerciseDbV2.kt` and converts records into the runtime `Exercise` model. Exercise DB V3 remains isolated and is not installed into the app. Therefore the contract below is derived from what the current runtime actually reads, not merely from V3 schema presence.

Relevant runtime paths:
- `MainActivity.kt`: Exercise model, location compatibility, level scoring, workout generation, sequencing.
- `ExerciseDbV2.kt`: loader/parser and V2→runtime conversion.
- `EquipmentCatalog.kt`: canonical equipment token mapping and location requirements.
- `ExerciseMetadata.kt`: legacy metadata fallbacks/heuristics.
- `WorkoutInfrastructure.kt`: custom/skill filtering, Runner materialization, load/assistance targets.
- `CoachingEngine.kt`: recovery and adaptive target changes.
- `SkillCatalog.kt`: skill graph eligibility.
- `V091Planning.kt`: source-backed prescription eligibility.
- `tools/validate_exercise_db_canonical.py`: V3 structural/evidence validator.

## Engine data contract

| V3/runtime concept | Type / allowed values | Runtime fallback | Actual engine effect | Missing/wrong consequence | Class |
|---|---|---|---|---|---|
| id / progressionKey | non-empty stable string | legacy name lookup exists only when progressionKey absent | persisted identity, DB lookup, restore | wrong ID can bind wrong definition; absent ID falls back to name and is less stable | ENGINE_CRITICAL |
| canonicalNameEn / nameIt / runtimeName | string | nameIt falls back to canonicalNameEn in V2 parser | identity/display; name and aliases also participate in skill/history matching | bad naming can break legacy/skill/history resolution | ENGINE_CRITICAL |
| equipmentRequired / equipment | list of EquipmentCatalog tokens | legacy display-name parsing; V2 canonical empty list becomes empty requirement set | location eligibility and equipment filters | unknown/unsupported/empty requirements can make an exercise unavailable or mis-eligible | ENGINE_CRITICAL |
| equipmentStatus | KNOWN/UNKNOWN | none semantically | validator evidence state for equipment | UNKNOWN means location eligibility cannot be trusted | ENGINE_CRITICAL |
| level | Principiante/Intermedio/Avanzato | V2 conversion uses legacy level or "Non valutato"; levelScore maps every non-beginner/non-intermediate value to score 3 | capability eligibility | missing/unknown silently behaves as advanced and can suppress selection | ENGINE_CRITICAL |
| levelStatus | KNOWN/UNKNOWN | none | validator evidence state | UNKNOWN marks level filtering as evidence-incomplete | ENGINE_CRITICAL |
| measure | REPS/SECONDS/DURATION/DISTANCE/INTERVALS | parser default REPS | Runner target/input type, completion validation, volume, skill prescription | wrong value changes what the user must perform/record | ENGINE_CRITICAL |
| resistanceMode | BODYWEIGHT/EXTERNAL_LOAD/ASSISTED/NONE; V3 also permits UNKNOWN as unresolved | runtime Exercise defaults NONE; metadata heuristics may infer legacy values | load vs assistance targets, adaptive intensity behavior, resistance prescription eligibility | wrong/default value can request wrong input or omit required load/assistance | ENGINE_CRITICAL |
| skillIds / runtime skills | stable SkillCatalog links | runtime still relies heavily on Exercise.skills/name aliases | skill eligibility, focus generation, mastery prescription | missing/wrong links can make skill workouts empty or train wrong nodes | ENGINE_CRITICAL for skill-related exercises; ENGINE_ENHANCING otherwise |
| category | PUSH/PULL/LEGS/CORE/CARDIO/MOBILITY/SKILL/OTHER | V2 conversion derives from movementType/family/legacy | planned focus category, source prescription eligibility | wrong category changes focus/prescription; OTHER reduces semantics | ENGINE_CRITICAL |
| role | Base/Forza/Progressione/Prerequisito/Accessorio/Recupero/Conditioning | Base | candidate ordering, support pool, default prescription/rest | wrong role changes workout composition and targets | ENGINE_CRITICAL |
| movementType / family | controlled strings in V3 builder | runtime category/group are derived from them when legacy metadata absent | conversion into category/group; family also supports semantics | wrong values can indirectly misclassify exercise | ENGINE_CRITICAL while V3→runtime mapping depends on derivation |
| movementPattern | list in V3; runtime currently uses one string | `withExerciseMetadata` derives heuristically from name/category | sequencing/setup-cost diversity; no current eligibility filter found | missing reduces sequencing quality, but does not itself block selection | ENGINE_ENHANCING |
| movementPatternStatus | provenance/status enum | none | validator/evidence only | missing evidence weakens trust but does not directly change runtime | PROVENANCE_LEGAL |
| primaryMuscles / secondaryMuscles / stabilizers | lists | runtime currently uses legacy group + secondaryGroups instead | V3 muscle arrays are not wired into current selection engine | absence currently does not block engine; explicit muscle model would improve recovery/body-part filtering | ENGINE_ENHANCING |
| group / secondaryGroups equivalent | runtime strings/lists | derived group from category; secondaryGroups often empty | recovery penalty and body-part filters | wrong group can bias recovery and body-part selection | ENGINE_CRITICAL runtime requirement; V3 should expose an explicit mapping instead of relying on names |
| unilateral / laterality | V3 nullable boolean vs runtime enum BILATERAL/UNILATERAL/ALTERNATING/NONE | runtime defaults BILATERAL | no direct selection filter found in audited generator | wrong default loses variant semantics; currently not a blocker | ENGINE_ENHANCING |
| isometric | optional boolean | none | no direct selection use found | no current engine blocker | ENGINE_ENHANCING |
| progressionRelations | graph edges | empty | exercise relations/progression UX; SkillCatalog uses a separate graph | missing reduces progression/substitution capability but base generation still works | ENGINE_ENHANCING |
| aliases | list | empty | identity/legacy resolution and potential skill alias compatibility | missing aliases can reduce matching robustness | ENGINE_ENHANCING |
| targetSets / targetValue / prescription / restGuidance | prescription data | app-rule prescriptions and recommended rest | Runner materialization and targets | missing V3 prescription is currently survivable because app rules fill it | ENGINE_ENHANCING |
| descriptionIt / setupIt / executionStepsIt / techniqueCuesIt / commonErrorsIt | text/list | empty/family guidance | exercise explanation | no selection failure | EDITORIAL_MEDIA |
| video / media | media metadata | absent | presentation only | no engine failure | EDITORIAL_MEDIA |
| sources / modelSourceRefs / fieldProvenance / confidence / evidence statuses | provenance objects/enums | none | validator/auditability, not selection | missing provenance prevents trustworthy promotion even when runtime could operate | PROVENANCE_LEGAL |
| license / attribution/media rights | source/media metadata | none | legal production eligibility | unknown rights block production use of media, not workout generation | PROVENANCE_LEGAL |

## Important runtime findings

1. **Equipment is stricter than the old roadmap implied.** `locationCompatible` requires a non-empty resolved requirement set and all requirements must exist at the location. An empty canonical equipment list therefore does not mean “works everywhere”; through the current path it can mean “compatible nowhere”.
2. **Unknown level is not neutral.** `levelScore` maps anything other than Principiante/Intermedio to score 3, so “Non valutato” behaves as advanced.
3. **movementPattern is not currently an eligibility filter.** It influences sequencing by penalizing adjacent identical patterns. It is ENGINE_ENHANCING under the current implementation, not ENGINE_CRITICAL.
4. **Muscle arrays are not yet the source of truth.** Current recovery/body-part behavior uses `group` and `secondaryGroups`. V3 should eventually provide an explicit muscle→runtime-group contract rather than assume empty primary/secondary arrays are already engine blockers.
5. **resistanceMode is operational.** It controls external-load/assistance targets and affects adaptive/prescription behavior. UNKNOWN cannot be allowed to silently map to NONE in an integration candidate.
6. **Skill identity remains dual-path.** V3 has `skillIds`, while runtime selection/mastery still relies on Exercise.skills and name/alias matching. V3 integration must define a deterministic SkillCatalog bridge.

## V3 completeness against the real contract

Current persisted validator baseline:

| Finding | Count | HIGH_PROBABILITY | AMBIGUOUS | NO_MATCH |
|---|---:|---:|---:|---:|
| equipmentStatus UNKNOWN | 97 | 0 | 45 | 52 |
| equipment token unsupported by current app | 18 | 12 | 5 | 1 |
| level UNKNOWN | 438 | 49 | 231 | 158 |
| resistanceMode UNKNOWN | 111 | 0 | 56 | 55 |
| SKILL movement without stable skillIds | 143 | 4 | 63 | 76 |
| movementPattern missing | 176 | 26 | 92 | 58 |

The unsupported equipment count is caused by the V3 token `suspension_trainer`, which is recognized by the canonical builder but absent from the current `EquipmentCatalog`.

There are also **377 records with an empty equipmentRequired list** (66 HIGH_PROBABILITY / 144 AMBIGUOUS / 167 NO_MATCH). This is tracked separately from equipmentStatus because some empty lists may be semantically legitimate in the source model, but the current runtime's non-empty requirement rule means every such case must be normalized before V3 integration.

Instruction completeness remains 500/1087 incomplete, but this is EDITORIAL_MEDIA for V3.1 and must not be mixed into engine-critical remediation.

## Matching evidence interpretation

The existing 1087/1087 matching audit is preserved and reused. Matching class is **not** field truth:

- HIGH_PROBABILITY means a strong record-level candidate exists. It is a field-review queue, not automatic permission to import every field.
- AMBIGUOUS must remain manual/field-specific review.
- NO_MATCH requires deterministic derivation from already-owned evidence where possible, otherwise additional research.
- CERTAIN remains zero by design.

Therefore a HIGH_PROBABILITY row is labelled as “candidate evidence available” rather than “resolved” unless the persisted field provenance already establishes the field.

## Deterministic remediation order

1. **P0 identity/integrity:** keep 1087/1087 IDs and all references stable.
2. **P1 equipment/location:** resolve 97 UNKNOWN; decide semantics for 377 empty requirements; add/support or remap `suspension_trainer` without weakening validation.
3. **P2 resistance:** resolve 111 UNKNOWN with field-level evidence.
4. **P3 level/capability:** resolve 438 UNKNOWN or define an explicit non-selectable/unknown policy; never rely on score-3 accidental fallback.
5. **P4 skill bridge:** resolve 143 missing skill links and define deterministic V3 skillIds → runtime SkillCatalog/aliases.
6. **P5 runtime category/group mapping:** certify category/role/movementType/family → Exercise.category/group/secondaryGroups.
7. **P6 measure contract:** preserve the currently complete measure enum and validate compatibility during integration.
8. **P7 enhancing metadata:** movement patterns, muscles, laterality and progression/substitution relationships.
9. **P8 editorial/media/legal:** instructions and media only after engine-critical semantics are stable.

Within each phase sort by:
(a) already-proven field provenance,
(b) HIGH_PROBABILITY candidate requiring field review,
(c) AMBIGUOUS,
(d) NO_MATCH/additional research,
then canonical ID ascending.

## Gate result

V3 integrity is good, but **V3.1 engine completeness is NOT yet satisfied**. The main blockers are equipment/location semantics, resistance mode, level/capability semantics and skill bridging. No validator was weakened and no app database was replaced.
