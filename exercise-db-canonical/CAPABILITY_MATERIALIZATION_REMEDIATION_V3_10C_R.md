# V3.10C-R — Native Capability Semantics & Materialization Remediation

## Decision
**FURTHER_EVIDENCE_REMEDIATION_REQUIRED**

Remote baseline verified at `47e5d580ad77dc3b5af8c82d641fad893aa4dc19`; PR #15 and `feature/exercise-db-v3` were aligned. Authoritative candidate is `exercise-db-v3-9r-b-candidate.json`, blob `b400dbd49405d5e2350f7fab0849780e4f8d06f2`, shared identically by the V3 candidate aliases.

## Root cause
The apparent V3.10C materialization loss is not a loader drop. V3.10B already documented that the authoritative candidate carries all four scalar capability dimensions as UNKNOWN for 1087/1087 records. V3.9R-B did not persist four-dimensional capability mutations: `residual-capability-patch-v3-9r-b.json` has empty `recordMutations` and `capabilityMutations`.

The V3.9R-B simulator's `cap()` function uses native four-dimensional capability only when all four dimensions are non-UNKNOWN; otherwise it explicitly falls back to `legacyLevel` when known. Therefore its 630 valid pools + 10 legitimate zero pools cannot be used as evidence that four-dimensional capability values existed in the candidate.

Classification: **CAPABILITY_DATA_GAP / SOURCE_EVIDENCE_GAP**, with historical **selection simulation legacy compatibility** explaining the coverage discrepancy. No AUTHORITATIVE_CANDIDATE_WIRING_BUG, LOADER_MAPPING_BUG, STATE_COLLAPSE_BUG, PATCH_APPLICATION_BUG, or STALE_ASSET_BUG was demonstrated.

## Lineage
V3.9R/V3.9R-B preserved the four field names as schema dimensions but did not materialize evidence-backed values. V3.9R-B candidate -> V3.10 schema -> V3.10B loader -> ExerciseV3 preserves the serialized UNKNOWN state. Thus represented UNKNOWN is preserved losslessly; there is no justified represented KNOWN/special-state capability value to recover from the candidate.

## Gate
- candidate identity: 1087 expected / unique 1087 / missing 0 / duplicates 0, as already validated by V3.10B and V3.9R-B
- authoritative candidate wiring: PASS
- capability materialization loss for represented candidate values: 0 demonstrated
- four native capability values available for selection: FAIL (1087/1087 UNKNOWN in every scalar dimension)
- legacyLevel fallback: NO in V3.10C
- global UNKNOWN relaxation: NO
- production path modified: NO
- runtime DB modified: NO
- native 640 smoke: NOT EXECUTED because the prerequisite native capability materialization/evidence gate is not satisfied
- 64,000 full rerun: NOT EXECUTED
- S0=0
- S1=1 CAPABILITY_DATA_GAP/SOURCE_EVIDENCE_GAP
- S2=1 V3_AUTHORITATIVE_ASSET_FULL_KOTLIN_EXECUTION_GATE deferred mandatory pre-production
- S3=0

## Exact next step
Perform evidence-first four-dimensional capability remediation on the true residual workset. Do not derive the four dimensions from legacyLevel. Persist evidence/provenance per dimension and only then rerun V3.10C-R materialization, native 640 smoke, drift analysis and 64,000-run gate.
