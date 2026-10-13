# Step 8A-S — Native V3 Prescription-Dependent Semantic Contract

Status: **FROZEN**  
Contract: `V3-STEP8AS-1`  
Frozen candidate SHA-256: `0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a`

## Normative architecture
V3 uses **MODEL D — HYBRID / structured contextual evaluation**. Canonical semantic states are immutable. Contextual evaluation is transient and V3-only. V2, legacyLevel and legacy difficulty MUST NOT participate.

The frozen candidate contains exactly **441 PRESCRIPTION_DEPENDENT states, all 441 on strengthRequirement**. technicalDifficulty, mobilityRequirement and balanceCoordinationRequirement contain zero PD states. Of the 441 strength PD records, 436 use EXTERNAL_LOAD and 5 ASSISTED.

## Why not scalar resolution
A PD strength requirement is not an intrinsic CapabilityLevel. The concrete load or assistance changes the demand. Manufacturing VERY_LOW..VERY_HIGH from load would require arbitrary exercise-specific thresholds and would discard unit/work-context information. Therefore current PD strength MUST be evaluated directly against typed athlete capacity; it MUST NOT be converted to or persisted as CapabilityLevel.

## strengthRequirement
PRESCRIPTION_DEPENDENT **MUST** mean that strength eligibility depends on the concrete resistance prescription. Sufficient context MUST contain the canonical exercise identity, resistance source, relevant work dimensions, a quantitative prescribed LOAD or ASSISTANCE with typed unit, and an athlete exercise-specific capacity envelope expressed for compatible work/variation dimensions and units.

For EXTERNAL_LOAD, the prescription is ELIGIBLE iff prescribed load is <= the athlete's validated feasible load under the matched context. For ASSISTED resistance, the prescription is ELIGIBLE iff prescribed assistance is >= the athlete's validated minimum required assistance under the matched context. Equality is inclusive. Incompatible units or semantically impossible values are INVALID_PRESCRIPTION_CONTEXT. Missing load/assistance, missing matched capacity, or incomplete work context is EXERCISE_REQUIREMENT_UNRESOLVED. Missing athlete capacity is distinct from unresolved exercise context and emits ATHLETE_CAPABILITY_UNKNOWN where the exercise context itself is complete.

Reps/duration/distance/interval context MAY constrain which capacity-envelope point is comparable; they MUST NOT be mapped directly to a CapabilityLevel. legacyLevel and V2 mappings MUST NOT influence the result.

## Other capability dimensions
The frozen candidate has zero PD states for technicalDifficulty, mobilityRequirement and balanceCoordinationRequirement. Their current KNOWN values remain ordinary ordinal requirements. If a future canonical version introduces PD, technical PD MUST be driven only by typed execution constraints (variation/tempo/ROM/laterality), mobility PD only by typed ROM/position constraints, and balance/coordination PD only by typed support/stability/laterality/execution constraints. Load magnitude alone MUST NOT create those requirements. Until such typed context exists, a future PD instance MUST fail as EXERCISE_REQUIREMENT_UNRESOLVED; no scalar may be invented.

## Context outcomes
Sufficient valid context yields ELIGIBLE or INELIGIBLE. Absent or partial context yields EXERCISE_REQUIREMENT_UNRESOLVED. Malformed or semantically impossible context yields INVALID_PRESCRIPTION_CONTEXT. Unsupported typed context yields UNSUPPORTED_CONTEXT. Unknown athlete-side capacity is ATHLETE_CAPABILITY_UNKNOWN. EVIDENCE_UNRESOLVED is TERMINAL_CANONICAL_STATE and MUST NOT become resolvable from prescription context.

## Immutability and isolation
All 441 canonical PD states MUST remain PD. Contextual results MUST be transient, MUST NOT be written into the candidate, MUST NOT leak across sessions, and MUST NOT be promoted to canonical truth by a global cache. V2 dependency for PD resolution is exactly zero.

## Step 8 oracle
Step 8 can construct NO_CONTEXT, SUFFICIENT_VALID_CONTEXT, INSUFFICIENT_CONTEXT, INVALID_CONTEXT and boundary contexts. For external load, equality with matched capacity is the inclusive boundary. For assistance, equality with minimum required assistance is the inclusive boundary. Repeated identical context MUST be deterministic; changing prescription context MAY change eligibility without changing canonical data.

## Applicability
All 441 PD states are CONTRACT_COVERED. CONTRACT_NOT_APPLICABLE = 0. CONTRACT_AMBIGUOUS = 0. Canonical semantic mismatches = 0.

## Design decision
The athlete-side four-level capability profile alone is insufficient for the current PD strength semantics. A typed exercise-specific capacity envelope is therefore a **MISSING_BUT_REQUIRED_V3_INPUT** for Step 8A implementation. This is not a blocker to the semantic contract: it is the explicit native V3 boundary Step 8A must implement. The contract intentionally avoids arbitrary thresholds and preserves prescription information.

## Terminal invariant
`PRESCRIPTION_DEPENDENT != EVIDENCE_UNRESOLVED`. The 48 terminal states MUST remain terminal and MUST NOT be resolved by prescription.

## Verdict
STEP_8AS_COMPLETE_READY_FOR_STEP_8A_IMPLEMENTATION
