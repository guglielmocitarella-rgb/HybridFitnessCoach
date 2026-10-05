# Selection Engine Migration V3.10C

## Status
**SELECTION_REMEDIATION_REQUIRED**

Native V3 shadow selection is implemented in `domain/v3/V3SelectionEngine.kt`. It is not registered in production composition, does not replace the runtime DB, and does not modify Runner or persistence.

## Verified baseline
Initial branch/PR HEAD: `57017e52ddefdcd3a98b5213367dd695ef12e0ec`. PR #15 and `feature/exercise-db-v3` were aligned. Authoritative candidate remains `exercise-db-v3-9r-b-candidate.json`, blob `b400dbd49405d5e2350f7fab0849780e4f8d06f2`.

V3.9R-B reference: 630 valid pools, 10 legitimate HOME_BODYWEIGHT/PULL zero pools, 64,000 deterministic runs, invalid selection 0.

## Architecture
Old production boundary remains legacy `Exercise` + `customWorkoutCandidatePool` / existing workout composition. New boundary is isolated `ExerciseV3 -> V3SelectionEngine` for tests/simulation only.

Hard filters: identity/category when requested, structured equipment, four independent capabilities, typed skill target when requested, prescription feasibility when requested. Movement pattern/group remain non-blocking unless a future explicit intent contract makes them selection-critical.

Equipment preserves KNOWN/UNKNOWN and NONE/REQUIRED/OPTIONAL/ONE_OF/ASSISTED_BY/EXTERNAL_LOAD. `suspension_trainer` remains a distinct token. Capability UNKNOWN/unresolved fails closed. There is deliberately no legacy-level fallback.

Skill matching consumes typed FAMILY/NODE relations. Prescription stays multidimensional; selection only checks feasibility when requested and never converts it to legacy `ExerciseMeasure`.

Determinism uses a stable seeded key plus canonical ID tie-break. Substitution reuses the same eligibility evaluation and category hard constraint.

## Critical migration finding
V3.10B reports all four scalar capability dimensions UNKNOWN for 1087/1087 records, with capability overall state EVIDENCE_UNRESOLVED or SCHEMA_UNRESOLVED. V3.9R-B's residual simulation code can fall back to legacyLevel when native capability is not materially selectable. V3.10C explicitly forbids that fallback.

Therefore the validated V3.9R-B 630/10 coverage cannot honestly be claimed for the native V3 engine without either:
1. authoritative four-dimensional capability data/semantics for the candidate, or
2. a newly authorized native capability policy that is not a hidden legacy-level mapping.

This is classified as `UNEXPECTED_SELECTION_DRIFT / CAPABILITY_SEMANTICS_BUG`, severity S1. No permissive UNKNOWN handling was introduced.

## Validation
Targeted JUnit coverage was added for bodyweight, all equipment modes, suspension trainer, equipment UNKNOWN, four independent capabilities, no level fallback, irrelevant unresolved metadata, typed FAMILY/NODE relations, multidimensional prescription, deterministic ordering, explicit zero-pool classification, and substitution hard constraints.

The full 1087 candidate loader execution and 640/64,000 native selection rerun remain `DEFERRED_MANDATORY_PRE_PRODUCTION` because the exact candidate blob is not materializable into the local execution filesystem in this environment. GitHub Actions were not used.

## Production isolation
Production path modified: NO.
Runtime DB switched: NO.
Runner modified: NO.
Persistence modified: NO.
Candidate modified: NO.
Workflows modified: NO.
GitHub Actions intentionally started: 0.

## Severity
S0=0. S1=1 (native capability semantics/candidate coverage). S2=1 (deferred full-candidate execution gate). S3=0.

## Next step
Do not proceed automatically. Resolve the native four-capability selection contract/candidate data gap, then rerun V3.10C targeted tests and the authoritative full-candidate 640/64,000 comparison when materialization is available.
