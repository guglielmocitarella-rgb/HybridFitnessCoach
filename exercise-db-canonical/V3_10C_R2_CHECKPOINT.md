# V3.10C-R2 CHECKPOINT

- branch: feature/exercise-db-v3
- R2 initial HEAD: 8590b7c08f33ec9332ea8835d272394f1d5e36b0
- R2 final HEAD / PR HEAD before documentation update: f9a1ab29da925c153cdb1babac0c2cd13f6d6f1a
- R2 candidate blob: 26d9afe09149db75cede2ee572266680538dd9bb
- R2 final status: C — FURTHER_EVIDENCE_REMEDIATION_REQUIRED
- records processed: 1087/1087
- capability fields terminally classified: 4348/4348
- generic non-terminal UNKNOWN: 0
- legacyLevel used as capability: NO
- legacyLevel fallback: NO
- global UNKNOWN relaxation: NO
- production path modified: NO
- runtime DB modified: NO
- S0/S1/S2/S3: 0/1/1/0

## Capability evidence state

- strengthRequirement: 441 PRESCRIPTION_DEPENDENT; 646 terminal unresolved requiring exercise-specific evidence.
- technicalDifficulty: 1087/1087 UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE.
- mobilityRequirement: 1087/1087 UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE.
- balanceCoordinationRequirement: 1087/1087 UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE.
- native 640 smoke: NOT EXECUTED by design because the capability/evidence prerequisite is not satisfied.
- native 64,000 rerun: NOT EXECUTED by design.

Terminal classification is not equivalent to evidence resolution. R2 correctly avoided false normalization; the residual blocker is evidence coverage.

## Problems confirmed

1. The four-dimensional capability schema is not the current problem and must not be reopened.
2. V3.10C-R excluded capability materialization loss as the primary root cause.
3. V3.9R-B selection coverage depended on diagnostic legacyLevel compatibility when native capability was unavailable; this is not acceptable for the authoritative Native V3 path.
4. The authoritative candidate still lacks sufficient exercise-specific evidence for most native capability values.
5. Passing selection tests by relaxing UNKNOWN, mapping legacyLevel, or inferring from names would hide rather than solve S1.
6. S2 remains a separate mandatory pre-production gate.

## Agreed next phase: V3.10C-R3 — Capability Evidence Resolution

Use `capability-terminal-unresolved-v3-10c-r2.json` as the authoritative residual queue.

Method:
- record-first, evidence-once, multi-field;
- recover original source evidence first;
- evaluate strength, technical difficulty, mobility, and balance/coordination together for each exercise;
- use exercise-specific external research only when source recovery is insufficient;
- require field-level provenance and confidence;
- preserve legitimate special states;
- no name-only inference;
- no legacyLevel mapping/fallback;
- no global UNKNOWN relaxation.

Prioritize records affecting the 640 selection scenarios to obtain an early S1 signal, but do not stop when the simulation passes: continue over the complete residual workset.

## Revalidation after evidence remediation

1. regenerate evidence-native candidate;
2. validate 1087/1087 identity/integrity and provenance;
3. validate ExerciseV3 materialization with zero semantic loss;
4. run 640 native-selection smoke;
5. classify zero pools and perform drift analysis;
6. if valid, run 64,000 deterministic simulations and 60/60 SkillNode coverage;
7. close S1 only without legacy fallback or false normalization.

## Evidence reuse

Persist R3 evidence so it can later be reused, where semantically appropriate, for descriptions, execution instructions, progressions, and other database enrichment. Reuse never authorizes automatic propagation without field-specific justification.

## Exact next step

Execute V3.10C-R3 evidence resolution over the complete R2 residual queue. Do not modify production runtime DB, Runner, persistence, or activate the Native V3 Selection Engine. Keep S2 separate as DEFERRED_MANDATORY_PRE_PRODUCTION.
