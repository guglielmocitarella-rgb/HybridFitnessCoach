# Exercise DB V3.1B — Equipment evidence resolution

Baseline remote HEAD: `f7bfae98d7f677cc003249dc5e6c5f5a4dc37c43`.

## Integrity and preserved baseline
- Canonical: **1087**
- Unique: **1087**
- Missing: **0**
- Duplicates: **0**
- Matching remains **1087/1087**; no matching checkpoint was regenerated.
- V3.1A persisted counts re-read: 377 empty equipment, 97 equipment UNKNOWN, 18 unsupported runtime tokens.
- All 18 unsupported rows remain `suspension_trainer`; this is a runtime capability gap, not a V3 data rewrite.

## Workset
The deduplicated equipment-remediation workset is **101 records**. The overlap is substantial: the UNKNOWN rows are mostly the same identities already present among problematic empty-equipment rows, so raw category totals were not added.

## Evidence outcome
- RESOLVED_CERTAIN: **0**
- RESOLVED_HIGH_CONFIDENCE: **1**
- RESOLVED_NO_EQUIPMENT: **0**
- RESOLVED_BODYWEIGHT: **0**
- RESOLVED_OPTIONAL: **0**
- RESOLVED_ONE_OF: **1**
- RESOLVED_ASSISTED_BY: **2**
- RESOLVED_EXTERNAL_LOAD: **2**
- REMAINS_AMBIGUOUS: **36**
- REQUIRES_MANUAL_REVIEW: **10**
- NO_RELIABLE_EVIDENCE: **49**

Only 6 identities received a candidate remediation. Every mutation has source, URL, evidence, confidence and reason in the patch/provenance artifacts. The remaining 95 identities are explicitly enumerated; source headings/index presence were not treated as apparatus proof.

## Candidate model
V3.1A semantics are reused unchanged: status KNOWN/UNKNOWN and modes NONE, REQUIRED, OPTIONAL, ONE_OF, ASSISTED_BY, EXTERNAL_LOAD. No parallel semantic model was introduced. This phase persists a deterministic patch instead of duplicating all 1087 records.

## Location-semantics gate
Logical invariants checked for the patch:
- UNKNOWN is never rewritten to NONE without field evidence.
- NONE remains non-blocking.
- REQUIRED carries required capabilities.
- OPTIONAL would remain non-blocking (no V3.1B mutation needed this class).
- ONE_OF requires at least one listed alternative.
- ASSISTED_BY records base support separately from assistance alternatives.
- EXTERNAL_LOAD remains distinct from bodyweight and records load capability.
- `suspension_trainer` is preserved and not remapped.

## Validation execution
The GitHub connector allowed source/tree inspection and deterministic artifact generation, but it does not provide a repository execution shell. A separate container shell was tested for repository materialization; outbound GitHub DNS/network access was unavailable, so the full branch could not be cloned there. Therefore the V3 Python validator and Kotlin regression test were **not executed locally in this session**, and this report does not claim TEST PASSED.

Before commit, the generated JSON objects were parsed/constructed in-process and checked for: 101 unique workset IDs, classification total = 101, every RESOLVED row carrying evidence/source/confidence, every patch mutation carrying provenance, UNKNOWN→NONE absence, ONE_OF alternatives non-empty, ASSISTED_BY assistance alternatives non-empty, EXTERNAL_LOAD load alternatives/required capability present, and no suspension_trainer rewrite.

## Qualitative sample
- no-equipment/bodyweight: V3.1A resolved rows were sampled but not reopened.
- required: Scapula Foam Rolling → foam_roller.
- one-of: Back Foot Elevated Split Squat → bench or box support.
- assisted: Assisted Pull-Up and Assisted Single-Leg Squat.
- external load: Weighted Pistols / Weighted Single-Leg Squat.
- suspension trainer: preserved unchanged as runtime gap.
- unresolved: source-index-only and ambiguous-match records remain explicit.

No runtime DB or selection-engine code is changed by V3.1B.

