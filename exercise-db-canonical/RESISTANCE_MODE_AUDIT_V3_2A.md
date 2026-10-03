# Exercise DB V3.2A — Resistance Mode Audit

Source HEAD: `12b5b23185213f75a712c97fb76a23355aa06032`

## Model decision
Current runtime values: BODYWEIGHT, EXTERNAL_LOAD, ASSISTED, NONE. V3 UNKNOWN is an unresolved evidence state and must never silently become NONE. NONE is formalized in this audit as NOT_APPLICABLE for mobility/flexibility/SMR/locomotion where load prescription is not part of the exercise contract.

The current model is **not semantically sufficient for every exercise** because EXTERNAL_LOAD and ASSISTED also select kg-valued Runner inputs. See `RESISTANCE_MODE_SCHEMA_PROPOSAL_V3_2A.md`.

## Workset
Verified resistanceMode UNKNOWN: **111 unique canonical IDs**. All 111 receive one terminal V3.2A classification.

## Final classification
- RESOLVED_HIGH_CONFIDENCE: **48**
- UNRESOLVED_INSUFFICIENT_EVIDENCE: **5**
- UNRESOLVED_SCHEMA_DECISION_REQUIRED: **12**
- UNRESOLVED_INTRINSICALLY_VARIABLE: **7**
- RESOLVED_NOT_APPLICABLE: **24**
- UNRESOLVED_CANONICAL_IDENTITY_PROBLEM: **9**
- RESOLVED_CERTAIN: **6**

Patchable now: **78**. Terminal unresolved: **33**. Canonical/runtime DB is not rewritten.

## Evidence policy
Assignments use the preserved V3 identity/matching/equipment evidence and the audited runtime contract. Equipment is context, not a resistanceMode truth source. Empty equipment never implies BODYWEIGHT. Cases requiring richer resistance/load-input semantics remain unresolved rather than being forced.

## Quality review
The regression test samples resolved bodyweight, external load, NOT_APPLICABLE, variable, insufficient-evidence, schema-decision and canonical-identity cases and checks equipment/resistance disagreement handling and UNKNOWN != NONE.

## Executed validation
DB-only GitHub Actions run **37151172994** on commit `eafc90e4449917487853a317bce36152e95535be`: PASS. JSON syntax, canonical validator, combined canonical/equipment/resistance regression suite (**40 tests**) and explicit integrity all passed. Canonical integrity remained 1087/1087 unique. The temporary V3.2A workflow is removed by the final evidence commit.

## Domain gate
**CLOSED_WITH_DOCUMENTED_UNRESOLVED** — 111/111 audited; 78 deterministic patch entries; 33 terminal unresolved with explicit cause; UNKNOWN is distinct from NONE/NOT_APPLICABLE; schema limitations are documented and no runtime DB or selection engine was modified.
