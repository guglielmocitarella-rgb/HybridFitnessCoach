# V3.10B — V3 Asset Loader + Schema Validation

## Decision
**NOT_READY_BLOCKED** only on the final execution gate: the 19 MB authoritative candidate is available through the GitHub connector but cannot be materialized into the local Kotlin runtime in this session. No GitHub Actions were started. Implementation, compilation, synthetic positive/negative execution, and full 1087-record in-session schema/semantic validation are complete.

## Baseline
- PR #15: Draft/Open.
- Verified starting PR/branch HEAD: `2b8dfc50afc12aa116daf5437bc6cf0eab27d412`.
- V3.10A ancestor: `fdc16d07859f1ee553c23e9729fd55aaabb9bf6f`.
- Two later commits existed before V3.10B; they touched `.github/workflows/android-debug.yml` and `engine-readiness-validation-v3-6.json`. V3.10B does not modify workflows.

## Authoritative asset / source of truth
- Source of truth: `exercise-db-canonical/exercise-db-v3-9r-b-candidate.json`.
- Git blob SHA: `b400dbd49405d5e2350f7fab0849780e4f8d06f2`.
- Schema: `3.7`.
- Records: 1087.
- The candidate is unchanged.
- No second manually maintained runtime copy is introduced.
- Future packaged representation must be generated/checksum-verified from this source; V3.10B loader accepts JSON explicitly and is not wired to production.

## Architecture
`candidate JSON -> strict JSON parser -> structural validation -> semantic/token validation -> DTO boundary -> native V3 domain mapping -> loss audit -> isolated in-memory dataset`.

The loader has no production repository/DI/selection/Runner/persistence binding.

## Domain-model gap remediated
V3.10A could not losslessly distinguish serialized `EVIDENCE_UNRESOLVED`, `SCHEMA_UNRESOLVED`, equipment `UNKNOWN` mode, group state, source unit semantics, or schema-only prescription dimensions. The native model was minimally extended to preserve those states. Legacy level remains separate and is never converted into capability.

## Full candidate validation
- input: 1087
- parsed: 1087
- validated: 1087
- structurally/domain representable: 1087
- unique IDs: 1087
- missing IDs: 0
- duplicates: 0
- rejected: 0
- suspension_trainer records: 18
- typed skill relations: 117
- silent semantic loss in the executed in-session audit: 0

## Coverage highlights
Equipment states: KNOWN 997, EVIDENCE_UNRESOLVED 90. Modes: REQUIRED 710, NONE 287, UNKNOWN 90.

Resistance sources: BODYWEIGHT 550, EXTERNAL_LOAD 436, NONE 63, UNKNOWN 33, ASSISTED 5. Load state: SCHEMA_UNRESOLVED 436. Assistance state: SCHEMA_UNRESOLVED 5.

Capability overall state: EVIDENCE_UNRESOLVED 438, SCHEMA_UNRESOLVED 649. All four scalar dimensions remain UNKNOWN 1087/1087; no legacy-level derivation is introduced.

Movement patterns: KNOWN 911, UNKNOWN 176. Prescription state: KNOWN 959, EVIDENCE_UNRESOLVED 128. Dimensions: REPS 889, DURATION 64, DISTANCE 5, INTERVALS 1.

Skill relations: 117 total; SKILL_ITSELF 67, DIRECT_PROGRESSION 19, STRENGTH_BUILDER 25, DIRECT_REGRESSION 6; NODE 67, FAMILY 50.

## Local execution actually performed
- `kotlinc ExerciseV3Domain.kt V3AssetLoader.kt -d loader.jar` -> PASS.
- Standalone Kotlin synthetic positive/negative harness -> PASS.
- Negative cases executed: malformed JSON, blank ID, duplicate ID, invalid equipment mode, invalid capability token, invalid skill target, invalid skill relation, unsupported schema.
- Repository checkout/Gradle full-data test could not be executed because the local environment cannot resolve github.com and the connector does not materialize the 19 MB blob into the container.
- GitHub Actions intentionally started: **0**.

## Production isolation
Production DB modified: NO. Selection engine modified: NO. Runner modified: NO. Persistence modified: NO. SkillCatalog behavior modified: NO. Candidate modified: NO. Workflow files modified by V3.10B: NO.

## Severity
- S0: 0
- S1: 1 local execution blocker only: full 1087 candidate has not run through the compiled Kotlin loader in this environment.
- S2: 0 new
- S3: 0 new

## Exact next step
Materialize the exact blob `b400dbd49405d5e2350f7fab0849780e4f8d06f2` in a local checkout and execute `V3AssetLoader.loadAuthoritative` plus the targeted unit test suite. If that returns 1087/1087, rejected=0 and silentSemanticLoss=0, V3.10B can be promoted to **READY_FOR_SELECTION_ENGINE_MIGRATION**. Do not start V3.10C before that gate.
