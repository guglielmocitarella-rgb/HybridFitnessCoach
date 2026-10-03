# Exercise DB V3.1C — Equipment residual resolution

Source HEAD: `ee6b41710f93f20a7325319639aae8f776785365`

## Reconstructed residual
- 95 unique residual IDs reconstructed from V3.1B artifacts.
- Prior classes: 36 REMAINS_AMBIGUOUS, 10 REQUIRES_MANUAL_REVIEW, 49 NO_RELIABLE_EVIDENCE.
- 6 V3.1B resolved IDs excluded from the residual set.

## Targeted evidence result
- UNRESOLVED_INTRINSICALLY_AMBIGUOUS: **32**
- UNRESOLVED_INSUFFICIENT_EVIDENCE: **39**
- RESOLVED_BODYWEIGHT: **3**
- UNRESOLVED_CANONICAL_IDENTITY_PROBLEM: **9**
- RESOLVED_NO_EQUIPMENT: **4**
- UNRESOLVED_SCHEMA_DECISION_REQUIRED: **8**

Newly resolved identities: **7**. All other records receive a terminal unresolved reason rather than a generic REMAINS_AMBIGUOUS label.

## Clusters
- E_ASSISTED: **7**
- A_SPECIFIC_NAME_SOURCE_MISSING: **20**
- D_EXERCISE_FAMILY_VARIANT: **47**
- K_POSSIBLE_CANONICAL_ALIAS_OR_IDENTITY: **5**
- J_GENUINELY_EQUIPMENT_INDEPENDENT: **5**
- B_GENERIC_MULTIPLE_IMPLEMENTATIONS: **7**
- C_EQUIPMENT_IMPLICIT_BIOMECHANICS: **2**
- F_WEIGHTED_BODYWEIGHT: **1**
- L_OTHER: **1**

## Semantics
All new positive resolutions are no-apparatus requirements: `equipmentStatus=KNOWN`, requirement mode `NONE`, empty required capabilities. BODYWEIGHT is a final audit class, not an equipment token. No UNKNOWN→NONE promotion is made without external field evidence.

Human-flag/dragon-flag cases are not lossy-remapped to `pull_up_bar` or `bench` when authoritative technique permits support concepts outside the current token set; those cases remain schema-decision-required.

## Preservation
No runtime DB, selection engine, matching checkpoint, prior V3.1A/V3.1B patch, or prior provenance is modified. V3.1C is additive.

## Executed validation
GitHub Actions DB-only validation run: 37150227664 on commit `9cc28486d65518e1679da2cdce26007438ead726`.

- JSON syntax validation: PASS.
- V3 canonical validator: PASS after repairing a pre-existing double-escaped regex in `tools/validate_exercise_db_canonical.py`; validation strictness was not reduced.
- Canonical regression suite / V3.1A semantics: PASS — 26 tests.
- V3.1C equipment regression suite: PASS — 4 tests.
- Explicit integrity: canonical 1087, unique 1087, duplicates 0.
- Persisted baseline validator remains `releaseReady=false`, with 97 equipment UNKNOWN and 18 equipment tokens unsupported by the current app, because V3.1C is patch-only and intentionally does not rewrite the runtime/canonical DB in place.

### Domain gate
Equipment evidence review is sufficiently closed to move to the next domain **as an audited patch layer**, not as an integration-ready runtime state. The 88 terminal unresolved records remain fail-closed and must not be silently treated as NONE; schema/identity cases remain explicit debt for later integration design.
