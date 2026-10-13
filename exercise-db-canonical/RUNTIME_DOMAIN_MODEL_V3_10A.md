# Runtime Domain Model V3.10A

Status: native V3 runtime domain model implemented without production routing.

## Ownership
Pure Kotlin package: `com.hybridfitnesscoach.app.domain.v3`.

## Mapping
- identity -> `CanonicalExerciseId`, `SkillTargetId`
- unresolved semantics -> `SemanticState` + `SemanticValue<T>`
- equipment -> information state + requirement mode + typed `EquipmentId`
- resistance/load/assistance -> `ResistanceSpec`, `ResistanceSource`, `InputUnit`
- four capabilities -> `CapabilityRequirements`
- legacy level -> explicit `LegacyExerciseLevel`, no conversion
- category/group/movement -> three separate value types
- prescription -> multidimensional `ExercisePrescription`
- skill bridge -> typed FAMILY/NODE and four relation types
- versioning -> `V3Version`
- provenance -> optional compact diagnostic reference only on skill relation

## Coexistence and behavior
The legacy `Exercise`, `ExerciseMeasure`, `ResistanceMode`, selection, Runner, persistence, SkillCatalog and runtime DB are untouched. No loader or production consumer uses `ExerciseV3` in V3.10A.

## Validation
Contract tests cover identity, all equipment modes including UNKNOWN and suspension_trainer, non-kg resistance/assistance, four independent capabilities and semantic states, taxonomy separation, multidimensional prescription, typed skill relations, and unresolved-state separation.

## Boundary
Next phase is V3.10B Asset Loader + Schema Validation. V3.10A intentionally does not load the 1087-record candidate.
