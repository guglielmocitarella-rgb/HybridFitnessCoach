# Exercise DB v2 — source-driven rebuild

This branch is intentionally isolated from RC2. The current app catalogue is treated only as a comparison set, not as the authority for the rebuilt database.

## Inclusion rule
A movement enters the catalogue only when at least one project source describes or prescribes it as a concrete exercise, skill, drill, stretch, mobility movement, conditioning movement, or SMR movement. Pure methods, generic training concepts, tests without a training movement, and duplicate names are not canonical exercises.

## Canonical record
Each canonical movement should support:
- stable `id`
- `canonicalNameEn`
- `nameIt`
- `aliases[]`
- `movementType`: STRENGTH | HYPERTROPHY | CALISTHENICS | SKILL | POWER | PLYOMETRIC | CONDITIONING | AEROBIC | CORE | MOBILITY | FLEXIBILITY | SMR
- `family` and `relatedFamilies[]`
- `variantOf` when applicable
- `movementPattern[]`
- `equipment[]`
- `level`
- `measure`: REPS | SECONDS | DURATION | DISTANCE | INTERVALS
- `descriptionIt`
- `setupIt`
- `executionStepsIt[]`
- `executionEvidence`: SPECIFIC_SOURCE | FAMILY_GUIDANCE | UNKNOWN. SPECIFIC_SOURCE means the execution contract is supported by exercise/section/chapter-level provenance; FAMILY_GUIDANCE is reusable biomechanical guidance and must not be presented as source-specific technique; UNKNOWN means no reviewed execution contract is available.
- `techniqueCuesIt[]`
- `commonErrorsIt[]` only when supported
- `primaryMuscles[]`
- `secondaryMuscles[]`
- `stabilizers[]` when supported
- `rangeOfMotion` when supported
- `unilateral`
- `isometric`
- `progressionEdges[]` with relationship and source
- `prerequisites[]`
- `prescription[]` with goal/context/source
- `restGuidance[]` with goal/context/source
- `contraindicationNotes[]` only when explicitly supported
- `sources[]` with source ID, source exercise name and evidence type
- `confidence`: HIGH | MEDIUM | LOW
- `video`: intentionally deferred

## Deduplication
1. Same movement, different language/name -> alias.
2. Same mechanics with only a replaceable equipment option -> one canonical movement plus equipment option unless the source treats mechanics/setup as meaningfully different.
3. Meaningful grip, ROM, leverage, unilateral, support, or kinetic-chain change -> variant/canonical selectable movement.
4. Explicit skill stage with its own technique or advancement criterion -> progression node.
5. Tempo/set/rep prescriptions are protocols, not new movements.
6. Source disagreements are retained as source-scoped evidence rather than silently reconciled.

## Progression graph
Edges use: MAIN_PROGRESSION | REGRESSION | PREREQUISITE | PREPARATORY_DRILL | ASSISTANCE | ADVANCED_VARIANT | RELATED.

## RC2 isolation
No RC2 production catalogue is replaced on this branch while RC2 is still being stabilized. Integration happens only after the rebuilt catalogue has been audited and RC2 is ready.
