# Internal source audit — canonical exercise candidate

Audit date: 2026-10-03  
Remote checkpoint audited: `feature/v0.9.5` at `13d071a04737f3a14d48eaaf35c34df1cd173762`  
Scope: identify project-internal fields and mappings that can be transferred before consulting external technical references.

## Result

The audit found one material extraction gap: the local reconstruction had omitted `ExerciseMetadata.kt`, although the runtime catalogues apply `withExerciseMetadata` to their entries. The function is present at the audited remote checkpoint and is byte-identical at the database's documented source baseline `9b8a41cfe5c7ee007f37219f9987a843f3ae3911`. It supplies runtime category, broad movement pattern, resistance mode, secondary groups, and technical-complexity defaults. The builder already mirrored some of these rules but did not preserve the complete effective runtime record or field-level provenance.

The builder now stores the app's effective values under `applicationMetadata.sourceEntries[]`, with per-field statuses for direct catalogue values, constructor defaults, and `withExerciseMetadata` rules. Ambiguous matches go into `ambiguousSourceEntries[]`; their values are retained for audit and are not assigned to a V2 identity. These values remain app-model evidence and do not fill exercise-science fields such as technique, cues, anatomical muscles, or contraindications.

The internal audit does **not** find technical data for all remaining UNKNOWN fields. After exact runtime links, active rules, aliases, and source inventories are considered, unresolved V2 values have no sufficient exercise-specific evidence in the repository. External references are now appropriate for those residuals, field by field.

## Repository materials reviewed

| Internal material | Audit finding |
|---|---|
| `app/src/main/assets/exercise-db-v2/exercises.json` | 587 source-driven V2 records. All contain source-specific description, setup, and execution steps; the source model has no populated `movementPattern` or `level` values. Equipment is populated on 232 records. Cues, common errors, primary/secondary muscles, stabilizers, ROM, exercise-specific prescription/rest, and contraindication fields are empty. |
| `app/src/main/assets/exercise-db-v2/progressions.json` | 113 sourced edges; endpoints retained by stable V2 IDs. |
| `app/src/main/assets/exercise-db-v2/programming_guidance.json` | 17 source-scoped guidance rules. They are not keyed to individual exercise IDs and are not copied as exercise-specific prescriptions. |
| `app/src/main/assets/exercise-db-v2/sources.json` and `exercise-db-v2/sources.tsv` | 14 registered sources, including the app model and rule sources in the candidate. |
| `exercise-db-v2/raw/*.tsv` | All 11 raw inventories reviewed (691 rows total). Their columns are source name, movement type, family, source ID, evidence type, locator, and status. They do not contain per-exercise equipment, level, resistance mode, instructions, muscles, cues, or prescriptions. |
| `exercise-db-v2/BUILD_MANIFEST.json`, `SOURCE_COVERAGE.md`, `SCHEMA.md`, `INTEGRATION.md` | Confirm the source-driven scope, inventory-only raw imports, deferred video, current integration isolation, and intended record fields. |
| Runtime catalogue: `MainActivity.kt` and `ExtendedExerciseCatalog.kt` | 583 distinct runtime entries parsed. Their explicit constructor values, labels, roles, levels, skills, measures, and targets are retained where present; defaults and helper results are recorded separately. |
| `ExerciseMetadata.kt` | Exact helper implementation reviewed and recovered locally. It fills group/category defaults, heuristic broad pattern, resistance mode, group-derived secondary groups, and technical complexity. It is not a technique source. |
| `EquipmentCatalog.kt` | Stable equipment tokens and display aliases reviewed. It does not currently define a `suspension_trainer` token. |
| `ExerciseDbV2.kt` | Legacy adapter and fallback behavior reviewed. It does not supply the missing V2 exercise-science content. |
| `SkillCatalog.kt` and `MainActivity.kt` legacy `SkillProgressionNode`s | Positional nodes, map aliases, active legacy-node filtering, prerequisites, and stable IDs reviewed. All 60 active nodes resolve to a runtime exercise name or alias. |
| `tools/build_exercise_db.py`, `tools/validate_exercise_db.py`, canonical builder/validator, tests | Existing raw-inventory coverage, deterministic build, identity reconciliation, field mapping, rule provenance, and validation gates reviewed. |
| `exercise-db-canonical/README.md`, checklist, candidate, audit, and validation report | Previous candidate assumptions and quantitative blockers compared with regenerated output. |
| Historical assets and imports | The repository tree contains only the four V2 exercise database assets; no older exercise-database asset, separate import table, or full source-book/manual files were present. The uploaded `Testo incollato(10).txt` is a work prompt with stale checkpoint instructions, not exercise data. |

## What the runtime catalogue contains

The 583 parsed runtime entries now have a complete source snapshot in the candidate: 582 unambiguous source snapshots (including both reviewed punctuation variants retained on their merged record) and two references to the same ambiguous `Muscle-Up` row, one for each conflicting V2 record. All 583 distinct runtime names are represented. The two ambiguous records keep selection-critical values UNKNOWN.

The Kotlin source explicitly supplies these field values on the indicated number of runtime entries: `category` 443; `movementPattern` 34; `resistanceMode` 437; `measure` 448; `targetSets` 36; `targetValue` 84; `intervalRestSec` 1; `laterality` 2; `technicalComplexity` 4. Explicit `secondaryGroups`, `intensity`, `systemicFatigue`, `progressionKey`, `setRestSec`, and `exerciseRestSec` occur on zero entries. Constructor defaults and `withExerciseMetadata` results are also preserved, with separate provenance.

`withExerciseMetadata` overwrites runtime `technicalComplexity` to 3 for SKILL, 2 for role `Forza`, and 1 otherwise. When `secondaryGroups` is empty, it derives them from the group label suffix. These are preserved as effective runtime metadata, not treated as anatomical findings. Runtime rest defaults are role-based in `MainActivity.kt` (150 seconds for `Forza`, 120 for `Progressione`, 90 for `Prerequisito` and other roles).

## Field-by-field UNKNOWN audit

| Field | Candidate state after internal reconciliation | Internal source conclusion |
|---|---|---|
| Equipment | 294 unknown; every one is a V2-only identity with no unique runtime entry. A further 18 records have known `suspension_trainer` requirements, which is absent from the app's current `EquipmentCatalog`. | V2 has equipment on 232/587 records; raw inventories have no equipment column. Exact runtime equipment labels and explicit implement names have already been mapped. The 18 known suspension-trainer items must stay visible as an app-capability blocker; they are not unknown equipment. |
| Resistance mode | 338 unknown, all V2-only identities without a unique runtime record. | Runtime direct values and helper rules are captured for matching records. Raw V2 inventory has no resistance-mode field. Equipment-token and movement-type fallbacks are provenance-marked and only apply where their conditions are known. |
| Movement pattern | 201 missing, all V2-only identities without a unique runtime record. | Runtime exact values and `ExerciseMetadata.kt` rules, direct V2 type mappings, and whitelisted family mappings have been applied. Raw V2 inventory contains only broad movement type/family, not a movement-pattern column. Mixed remaining families cannot be assigned safely from those labels alone. |
| Level | 506 unknown: 504 no-match V2 records plus the two ambiguous `Muscle-Up` identities. | `exercises.json` has no level values. Runtime levels were transferred where a unique mapping exists; shared aliases are retained as ambiguous. Raw inventories have no level/difficulty column. |
| Skill links | 143 missing: 138 V2-only records and 5 current-model records. | All active SkillCatalog nodes and aliases were parsed. The five current-model records preserve `skillLabels` but do not match a specific exercise node; choosing a progression-node ID from only a parent skill label would overstate the evidence. V2 raw inventories contain no stable SkillCatalog IDs. |
| Instructions | 500 incomplete, all current-model-only identities. | Runtime catalogues contain no setup or execution instructions. The 587 V2 records already carry source-specific descriptions, setup, and execution steps. Inventory-only TSV rows cannot supply missing technique. |
| Technical cues, errors, anatomy, ROM, stabilizers, contraindications | No reliable per-exercise internal values were found for the missing fields. | V2 JSON fields are empty; runtime group labels and programming categories are preserved only as app metadata and are not promoted to anatomical or safety claims. The 17 programming-guidance rules are source/scoped guidance, not exercise-specific technique. |
| Application programming metadata | Preserved for the 583 runtime entries in the separate app snapshot. | Explicit app values, Kotlin defaults, equipment-label normalization, active skill aliases, and helper-derived results are all retained with source IDs and field-level statuses. These do not replace source-backed exercise prescriptions. |

## Compatibility and release gate

The app's enum tokens and canonical recognised equipment tokens differ: `suspension_trainer` is recognised in the candidate but is not defined by `EquipmentCatalog.kt`; the app has a `bodyweight` capability token that is represented as an empty required-equipment set in the canonical candidate instead. The validator now reports unsupported-but-recognised equipment separately and keeps it in the release gate. Runtime DB integration remains deferred.

The internal-source audit checkpoint `9844c49651c6c9641f97270ff27fb03ff5f11dea` did not use an external source. A subsequent, separately verified enrichment block now uses the American Council on Exercise (ACE) Exercise Library for seven individually matched records. It resolves only exercise-specific `equipmentRequired` and `level` values and stores each original label, evidence type, URL, confidence, and any vocabulary mapping in `exercise-db-canonical/external-enrichment.json`. The ACE difficulty label is translated to the candidate's Italian level vocabulary. No generalization is made to related variants. This first external block reduced unknown equipment from 294 to 287 and unknown level from 506 to 499; the internal audit findings above describe the pre-enrichment baseline.
