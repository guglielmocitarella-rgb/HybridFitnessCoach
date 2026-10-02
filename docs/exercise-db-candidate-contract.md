# Exercise DB candidate contract (draft)

Status: isolated design artifact. It does not replace the runtime database.

## Goal
Define the minimum data contract required for a complete, source-traceable exercise catalogue suitable for Hybrid Fitness Coach selection, execution UI, equipment/location filtering, programming, progression, and history.

## Principles
1. Stable identity is independent of display name.
2. Exercise-specific facts must not be fabricated from family templates.
3. Family-level guidance may be used only when explicitly tagged as inherited/fallback.
4. Every fact that affects execution, safety, eligibility, progression, or programming must carry provenance or an explicit APP_RULE classification.
5. Unknown is represented as unknown/null, never as an invented default.
6. Runtime selection must be able to distinguish exercise variants and avoid collapsing meaningful variants.
7. The contract is versioned and migration-safe.

## Required identity fields
- id: stable unique ID
- canonicalNameEn
- nameIt
- aliases (may be empty)
- family
- movementType
- measure

## Required eligibility/selection metadata
These fields must be explicit before a record is selection-ready:
- equipment requirements (including bodyweight/no-equipment explicitly)
- movementPattern
- category / primary movement role
- primaryMuscles
- level or difficulty model
- laterality
- resistanceMode
- location/capability requirements where equipment alone is insufficient
- variantOf when the exercise is a variant
- contraindication/suitability flags only when sourced; otherwise unknown

## Required execution-card metadata
For an exercise to be execution-ready:
- descriptionIt: exercise-specific
- setupIt: exercise-specific
- executionStepsIt: exercise-specific ordered steps
- techniqueCuesIt
- commonErrorsIt
- rangeOfMotion where meaningful
- executionEvidence must distinguish EXERCISE_SPECIFIC from FAMILY_GUIDANCE/FALLBACK/UNKNOWN

A generic family template does not satisfy execution completeness.

## Programming metadata
Programming and coaching values should be separated from anatomical/execution facts:
- supported goals/roles
- prescription constraints/ranges where sourced
- rest guidance where sourced
- measure-specific target semantics
- load/assistance applicability
- intensity/systemic-fatigue/technical-complexity ratings only with defined scale and provenance
- APP_RULE defaults must remain identifiable as app policy, not source facts

## Progression metadata
- prerequisites
- easier/harder progression edges
- skill relationships
- variant relationships
- progression edge provenance

## Provenance
Each sourced claim or coherent claim group should retain:
- source_id
- evidence_type
- locator
- scope (exercise-specific, family-level, programming guidance, etc.)
- confidence

Record-level sources alone are insufficient when only a TOC heading supports identity but not execution/anatomy.

## Completeness states
A record should expose machine-checkable readiness:
- IDENTITY_READY
- SELECTION_READY
- EXECUTION_READY
- PROGRAMMING_READY
- PROGRESSION_READY
- COMPLETE

COMPLETE requires all applicable readiness states. Non-applicable fields must be explicitly marked N/A rather than silently empty.

## Validation gates
Reject COMPLETE when:
- IDs or canonical lookup keys collide ambiguously
- required selection metadata is unknown
- execution text is generic family guidance
- execution/anatomy claims lack suitable evidence
- equipment is empty without explicit no-equipment/bodyweight meaning
- variant/progression references point to missing IDs
- enum/token values are outside the controlled vocabulary
- source references are unresolved

## Migration note
The current runtime model and parser consume only a subset of the JSON fields. The candidate schema should therefore be evolved separately and only integrated after the actual 1091-item runtime catalogue from the latest Work build is available and mapped.
