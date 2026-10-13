# Exercise DB V3 — Development Strategy

**Updated:** 2026-10-04  
**Authoritative branch:** `feature/exercise-db-v3`  
**Authoritative PR:** #15 (Draft)

## Decision

Exercise DB V3 will be completed in two deliberately separated macro-phases.

### Phase A — Engine readiness first

Before editorial enrichment or media work, complete everything required for the Hybrid Fitness Coach engine to select, prescribe, sequence, adapt, substitute and progress exercises correctly.

V3.1 through V3.5 have audited the real engine contract and the principal semantic domains without replacing the runtime database:

- V3.1 / V3.1A-B-C — engine contract, equipment and location semantics.
- V3.2A — resistance mode semantics.
- V3.2B — level/capability semantics.
- V3.2C — typed Exercise ↔ Skill relations.
- V3.3 — category/group semantics.
- V3.4 — measure/prescription semantics.
- V3.5 — movement pattern, engine-enhancing metadata and variety/repetitivity audit.

The next gate is **V3.6 — Engine Readiness Consolidation & Gap Matrix**. It must consolidate V3.1–V3.5 record-by-record, identify the actual remaining engine blockers, distinguish data gaps from schema/runtime integration gaps, and define the minimum contract that a future Exercise V3 candidate must satisfy.

After V3.6, the expected path is:

1. **V3.7 — Engine Schema Candidate**: define the coherent V3 schema from the audited requirements rather than extending legacy fields ad hoc.
2. **V3.8 — Engine-Critical Remediation**: apply validated CERTAIN/HIGH-confidence remediation to an isolated V3 candidate, not the runtime DB.
3. **V3.9 — Selection / Prescription Simulation**: exercise the candidate across representative location, equipment, capability and skill scenarios; measure eligibility, variety, progression and explainability.
4. **Integration gate** only after the candidate passes those checks.

The runtime database, Runner and selection/progression engines remain unchanged until that gate.

### Phase B — Editorial and media enrichment

Description, setup, execution, technique cues, common errors, images, GIF/video and licensing remain valuable, but they are intentionally **not blockers for Phase A** unless a specific item is proven necessary for engine correctness.

They will be completed as a later content/UX development stream, with provenance and licensing preserved.

## Rationale

The V3 audits showed that raw field completeness is not equivalent to engine readiness. Several legacy concepts are structurally insufficient or need richer semantics, including equipment requirement modes, resistance/load input, multidimensional capability, typed skill relations, multidimensional prescription and multi-pattern representation.

Therefore the project must not chase 100% non-null coverage or automatically import ambiguous values. A record can be engine-ready when required semantics are reliable, or when a documented unresolved value has an explicitly safe behavior. Conversely, a populated but semantically unreliable field is not considered ready.

## Preservation rule

All V3.1–V3.5 artifacts, matching 1087/1087, provenance, patch layers, unresolved queues, tests and validators are evidence and must be preserved. Future phases must build on the current remote HEAD and must not reset to historical SHAs or regenerate already validated matching without evidence of corruption.
