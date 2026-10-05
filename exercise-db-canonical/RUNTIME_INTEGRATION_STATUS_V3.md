# Runtime Integration Status V3

| Gate | Origin | Status / reason | Required before |
|---|---|---|---|
| V3_AUTHORITATIVE_ASSET_FULL_KOTLIN_EXECUTION_GATE | V3.10B / V3.10B-X / V3.10B-X2 | DEFERRED_MANDATORY_PRE_PRODUCTION — MATERIALIZATION_ENVIRONMENT_LIMITATION | PRODUCTION_ACTIVATION |
| V3_NATIVE_SELECTION_ENGINE_GATE | V3.10C-R | FURTHER_EVIDENCE_REMEDIATION_REQUIRED — authoritative candidate preserves UNKNOWN for all four scalar capability dimensions on 1087/1087 records; V3.9R-B coverage used explicit legacyLevel compatibility fallback and did not persist native capability mutations | NATIVE_SELECTION_VALIDATION |

## V3.10C-R finding
PR #15 / branch baseline was verified at `47e5d580ad77dc3b5af8c82d641fad893aa4dc19`. Candidate aliases resolve to blob `b400dbd49405d5e2350f7fab0849780e4f8d06f2`.

No candidate wiring, schema mapping, loader mapping, state-collapse, patch-application, or stale-asset bug was demonstrated for capability. V3.10B's UNKNOWN state is the state serialized by the authoritative candidate. V3.9R-B did not materialize four-dimensional capability values; its simulator used legacyLevel compatibility when native capability was unavailable.

Therefore S1 is a real capability data/evidence gap. V3.10C correctly keeps legacyLevel fallback disabled and UNKNOWN fail-closed. No global relaxation is authorized.

## Production isolation
Production path modified: NO. Runtime DB modified: NO. Runner/persistence/workout generator modified: NO. Native V3 remains shadow-only.

## Next gate
Evidence-first four-dimensional capability remediation with field-level provenance, followed by authoritative materialization validation, 640 native smoke/drift, then 64,000 deterministic rerun. S2 remains separately deferred mandatory pre-production.


## V3.10C-R2 checkpoint
V3.10C-R excluded materialization loss as primary root cause: four native dimensions were defined but not populated, while V3.9R-B used diagnostic legacyLevel compatibility. R2 processed 1087/1087 records and terminally classified 4348/4348 fields without legacy mapping/fallback or global UNKNOWN relaxation. Repository evidence supports limited special-state assignments; remaining fields are terminal unresolved pending exercise-specific external evidence. S1 remains open; native 640/64,000 gates are not executed. Production runtime remains unchanged. S2 remains separate and mandatory pre-production.

## V3.10C-R2 — problems observed and agreed resolution strategy

### Confirmed problems
1. **Capability evidence coverage is now the primary S1 blocker, not schema design.** The four approved dimensions remain `strengthRequirement`, `technicalDifficulty`, `mobilityRequirement`, and `balanceCoordinationRequirement`; they are not to be redesigned or collapsed back into legacy `level`.
2. **R2 completed semantic classification but not evidence resolution.** All 4348/4348 fields are terminally classified and generic/non-terminal UNKNOWN is zero, but terminal classification must not be mistaken for a populated native capability dataset.
3. **Strength has partial structured semantics.** 441/1087 `strengthRequirement` fields are `PRESCRIPTION_DEPENDENT`; the remaining 646 require exercise-specific evidence resolution.
4. **The other three dimensions remain evidence-poor.** `technicalDifficulty`, `mobilityRequirement`, and `balanceCoordinationRequirement` are 1087/1087 `UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE`. These values must not be mass-filled from exercise names, category, equipment, skill status, or legacyLevel.
5. **Selection validation is intentionally blocked.** Native 640-scenario smoke and 64,000-run rerun must not be used to hide the evidence gap. They reopen only after a materially improved evidence-native candidate passes capability/materialization prerequisites.
6. **S2 is separate.** `V3_AUTHORITATIVE_ASSET_FULL_KOTLIN_EXECUTION_GATE` remains `DEFERRED_MANDATORY_PRE_PRODUCTION` and is not solved by capability research.

### Resolution strategy — V3.10C-R3 Capability Evidence Resolution
The next phase is evidence resolution, not another schema redesign.

Use a **record-first / evidence-once / multi-field** workflow:
- start from `capability-terminal-unresolved-v3-10c-r2.json`;
- for each exercise recover its original source record and all already-persisted provenance/evidence;
- evaluate all four capability dimensions in the same evidence pass;
- only where original sources are insufficient, perform exercise-specific external research using authoritative/technical sources;
- store field-level evidence, confidence, rationale, derivation rule and provenance;
- preserve legitimate semantic states such as `PRESCRIPTION_DEPENDENT`, `VARIABLE`, `VARIANT_DEPENDENT`, and `NOT_APPLICABLE`;
- never infer values solely from names, category, equipment, movementType=SKILL, unilateral status, or legacyLevel;
- never reintroduce legacyLevel mapping/fallback or global UNKNOWN relaxation.

### Prioritization without test overfitting
Prioritize unresolved records that materially affect the 640 native-selection scenarios so S1 can be retested as early as possible. This is only execution priority: R3 must continue across the complete unresolved workset rather than stopping once the simulation passes. The objective is an evidence-rich database, not a database tuned to the test matrix.

### Revalidation sequence
After a materially improved evidence-native candidate exists:
1. rebuild the V3 candidate;
2. validate 1087/1087 identity/integrity and capability provenance;
3. validate ExerciseV3 materialization with zero semantic loss;
4. execute the 640-scenario native smoke and classify every zero pool;
5. perform drift analysis against V3.9R-B, explicitly separating previous legacy-fallback artifacts from real regressions;
6. if the smoke is semantically valid, execute the 64,000 deterministic rerun and 60/60 SkillNode coverage;
7. close S1 only if native selection semantics pass without legacy fallback or false normalization.

### Data-reuse policy
Evidence gathered in R3 must be persisted in reusable provenance/evidence artifacts. Where technically applicable, it may later support descriptions, execution instructions, progressions, or other editorial enrichment, avoiding duplicate research. Capability evidence remains field-specific: reuse does not authorize automatic propagation to unrelated fields.

### Non-goals
R3 must not modify the production runtime DB, production Runner, production persistence, or activate the Native V3 Selection Engine. It must not reopen the four-dimensional capability model. Production activation remains blocked until the relevant native-selection gate and the separate S2 gate are satisfied.

\n\n## V3.10C-R3 checkpoint\nR3 re-examined the complete R2 residual queue using exercise-specific repository evidence and deterministic rules excluding names, aliases, legacyLevel and existing capability values. Remaining terminal unresolved require exercise-specific external research. S1 remains open; production/runtime DB/Runner/persistence unchanged; S2 remains separate.\n