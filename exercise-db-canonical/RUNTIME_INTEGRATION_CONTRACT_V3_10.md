# Runtime Integration Contract V3.10

Baseline remote verified at 5e138744c39106aa3ef5da689235b90ffeeb2348; validated V3.9R-B parent 589605a06dc04d8f980d5610b9bb553dfe39726c. Candidate: 1087/1087 unique, missing=0, duplicate=0. Simulation: 630 valid pools + 10 legitimate HOME_BODYWEIGHT/PULL zero pools; 64,000 deterministic runs; SkillNode 60/60; S0=0 S1=0; runtime DB unchanged.

## Decision
**READY_FOR_RUNTIME_IMPLEMENTATION**

## Architecture
Use a native V3 runtime domain model. A V3-to-legacy adapter is forbidden as the authoritative runtime path because current ExerciseDbV2.toLegacyExercise flattens V3 semantics. Legacy compatibility exists only at persistence/input boundaries.

Pipeline: validated V3 asset -> versioned parser -> strict validation -> V3 domain model -> selection -> multidimensional prescription -> Runner materialization -> versioned persistence/history.

## Contract
- Identity: canonical ID is authoritative; aliases/names are lookup/display only.
- Equipment: preserve KNOWN/UNKNOWN and NONE/REQUIRED/OPTIONAL/ONE_OF/ASSISTED_BY/EXTERNAL_LOAD. suspension_trainer is distinct and MUST NOT alias rings/bands.
- Resistance: separate resistance source, load input, assistance input and unit semantics. Do not assume kg.
- Capability: preserve exactly strengthRequirement, technicalDifficulty, mobilityRequirement, balanceCoordinationRequirement. Athlete capability is a separate model. No legacy level -> four-capability mapping.
- Legacy level: KEEP_FOR_UI_AND_BACKWARD_COMPATIBILITY, IGNORE_FOR_NATIVE_SELECTION after migration.
- Category/group/movement: category functional ENGINE_CRITICAL; group anatomical/legacy; movementPattern 0..n ENGINE_ENHANCING and not an arbitrary eligibility blocker.
- Prescription: REPS, DURATION/SECONDS, DISTANCE, INTERVALS plus LOAD, ASSISTANCE, ATTEMPT, QUALITY, OPEN_ENDED combinations. Prescribed and actual remain distinct.
- Skill: SkillCatalog stays source of truth for graph/prerequisites/mastery. Exercise V3 provides typed SKILL_ITSELF/DIRECT_PROGRESSION/DIRECT_REGRESSION/STRENGTH_BUILDER relations targeting FAMILY/NODE.
- States: KNOWN, UNKNOWN, NOT_APPLICABLE, VARIABLE, PRESCRIPTION_DEPENDENT, VARIANT_DEPENDENT and documented unresolved are explicit and serializable. UNKNOWN is never coerced.
- Provenance: full at build-time; compact source/evidence IDs may be bundled for diagnostics; selection never depends on provenance.
- Editorial/media: schema-compatible future extension only; out of scope.

## Backward compatibility
Read old persisted schemas and write versioned new schemas. Never destructively rewrite old data. New snapshots carry schemaVersion, canonicalExerciseId and display snapshot. Name-only references require an explicit identity compatibility resolver; ambiguous names fail closed.

## Source of truth
V3 asset owns exercise identity/engine metadata; SkillCatalog owns skill graph/mastery; V3 typed relations own Exercise-to-Skill bridge; persisted sessions own actual performance; versioned alias table owns legacy identity compatibility.

## Preservation
PREVIOUS V3 ARTIFACTS PRESERVED: YES
V3.9R-B CANDIDATE MODIFIED: NO
RUNTIME PRODUCTION CODE MODIFIED: NO
APK BUILT: NO
