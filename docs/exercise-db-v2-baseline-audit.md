# Exercise DB V2 baseline audit — isolated candidate

Branch: `work/exercise-db-v2-completion`
Scope: current 587-record V2 asset only. This report does not claim that these 587 records are the complete runtime catalogue of the latest APK.

## Baseline counts

- Records: 587
- Records whose description uses the generic “è un esercizio della famiglia …” construction: 587/587
- executionEvidence = FAMILY_GUIDANCE: 423/587
- executionEvidence = SPECIFIC_SOURCE: 164/587
- confidence MEDIUM: 420/587
- confidence HIGH: 167/587
- movementPattern populated: 0/587

The current JSON has fields that are structurally present but universally empty/null in this baseline, including technique cues, common errors, primary/secondary muscles, stabilizers, movement pattern, level, prerequisites, prescription, rest guidance, and several biomechanical descriptors.

## Runtime/schema gap

The current Kotlin `ExerciseV2Record` parser consumes only a subset of fields present in exercises.json:
- consumed: identity/names, movementType, family, equipment, level, measure, description/setup/execution steps, executionEvidence, sources, confidence.
- present in JSON but currently not represented/consumed by ExerciseV2Record: relatedFamilies, variantOf, movementPattern, techniqueCuesIt, commonErrorsIt, primaryMuscles, secondaryMuscles, stabilizers, rangeOfMotion, unilateral, isometric, prerequisites, prescription, restGuidance, contraindicationNotes, video, localizationStatus.

Therefore filling those JSON fields alone would not make them available to the current selection/runtime model.

## Provenance observation

Current source references frequently establish that an exercise exists in a source (index/TOC/heading) rather than supporting detailed execution or anatomy. Evidence types observed include INDEX, TOC_HEADING, LEVEL_HEADING, SOURCE_HEADING, EXERCISE_HEADING, EXERCISE_SECTION, TOC_EXERCISE, CHAPTER_HEADING and INDEX_HEADING.

A catalogue entry must not promote identity-only evidence into exercise-specific technique/anatomy evidence.

## Consequence for selection diversity

The candidate database currently cannot supply movement-pattern diversity because movementPattern is empty for all 587 records. Many other potentially discriminating fields are also unavailable to the runtime parser. This is a plausible database-side contributor to repetitive selection, but it does not establish that the selection algorithm is itself correct; that requires a separate ranking/selection audit against the final 1091-item runtime catalogue.

## Candidate completion order

1. Preserve stable IDs and source provenance.
2. Define controlled vocabularies and N/A/UNKNOWN semantics.
3. Populate selection-critical metadata first: equipment semantics, movement pattern, category/role, primary muscles, difficulty, laterality, resistance mode and variant relationships.
4. Populate exercise-specific execution material with suitable evidence.
5. Populate progression and programming metadata with scoped evidence.
6. Add machine validation and readiness states.
7. Only after the latest Work state is published, map/merge against the actual 1091-item runtime catalogue and audit duplicates/legacy composition.
