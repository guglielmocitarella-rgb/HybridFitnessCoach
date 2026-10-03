# Exercise DB canonical candidate — checklist status

**Audit baseline:** `feature/v0.9.5` at `9b8a41cfe5c7ee007f37219f9987a843f3ae3911` (Android APK #873).  
**Saved candidate:** `feature/v0.9.5` commit `a09a612ea0d42e3be62397a5a3c79d0832894c3a`.  
**State:** candidate materialized, structurally validated, and saved to GitHub; **not ready for app integration**.

## Reconciliation

The code at this baseline has 587 V2 records and 583 distinct current-model entries. The runtime's exact name/alias lookup leaves 504 current-model-only entries, giving 1,091 source entries before identity review.

The canonical candidate has 1,087 identities after preserving three unique punctuation-normalized links to V2 IDs and merging the two current-model labels `Straight arm pulldown` / `Straight-arm pulldown`. The merged record retains both labels, both source locations, and the `Front lever` skill label. This is a documented identity decision based on the identical exercise label apart from punctuation and matching app-model fields.

`Muscle-Up` and `Normal Muscle-Up` remain separate V2 records. The `Muscle-Up` lookup alias points to both, so the candidate marks that app-model relationship ambiguous and does not merge the records. Their shared runtime label remains an unresolved naming issue.

## Checklist

| Checkpoint item | Status | Evidence / remaining work |
|---|---|---|
| 1. Materialize one candidate catalog | **Partial** | Candidate contains 1,087 records with stable unique IDs, original V2 content, current-model metadata, source references, and field provenance. Several required fields remain unknown. |
| 2. Resolve identity and variants | **Partial** | Three unique V2/current-model name variants were linked; the straight-arm duplicate was merged with provenance retained. Muscle-Up ambiguity remains unresolved. No progression edge was invented. |
| 3. Complete equipment and provenance | **Partial** | Equipment tokens are normalized and unknown is distinct from known-empty. A conservative, provenance-marked name rule resolves 11 explicitly named implements; 292 records still have unknown equipment requirements. |
| 4. Complete selection-critical fields | **Not complete** | Unknowns remain: equipment 292, movement pattern 429, level 504, resistance mode 336, SkillCatalog links 172. App-rule-derived values are labeled as rules, not source facts. |
| 5. Complete movement-pattern taxonomy | **Partial** | A source-marked top-level rule maps explicit V2 movement types FLEXIBILITY/MOBILITY → `mobility`, SMR → `recovery`, and CONDITIONING/AEROBIC → `conditioning`. This resolves 75 records; 429 still need more specific supported patterns. |
| 6. Link skills and progressions by stable IDs | **Partial** | All 113 V2 progression edges have valid endpoints and registered sources. 172 skill records still lack a specific stable SkillCatalog node link. |
| 7. Complete training and instruction metadata | **Not complete** | 500 current-model-only identities have no supported description, setup, or execution steps. Missing muscle, laterality, and exercise-level prescription data also remain. |
| 8. Add an automatic validator | **Complete for candidate structure** | `tools/validate_exercise_db_canonical.py` passes structural integrity, source registration, equipment-token, ID, and progression-reference checks. It keeps the release gate false while evidence is incomplete. |
| 9. Simulate reachability by location, capability, and skill | **Not run** | Must be completed against the app's actual eligibility pipeline after selection metadata is completed. Unknown equipment remains fail-closed. |
| 10. Measure current generator diversity | **Not run** | Requires running the existing generator against the reconciled catalog and representative histories/configurations. |
| 11. Design controlled ranking diversification | **Not started** | Requires reachability and diversity measurements first. |
| 12. Prepare integration and remote checkpoint | **Partial** | Candidate generator, validator, tests, checklist, and exact candidate/audit archives were pushed to `feature/v0.9.5` in commit `a09a612`. App runtime files were not changed; integration remains blocked by incomplete selection-critical and instruction data. |

## Validation snapshot

`exercise-db-canonical-audit.json` and `VALIDATION_REPORT.json` contain the machine-readable results and unresolved record IDs.

| Metric | Result |
|---|---:|
| V2 records retained | 587 |
| Current-model entries | 583 |
| Current-model-only entries before identity resolution | 504 |
| Canonical identities after reviewed links/merge | 1,087 |
| Unique stable IDs | 1,087 |
| Progression edges | 113 |
| Dangling progression edges | 0 |
| Unknown equipment | 292 |
| Missing movement patterns | 429 |
| Unknown level | 504 |
| Unknown resistance mode | 336 |
| Skill exercises without a stable node link | 172 |
| Missing supported exercise instructions | 500 |
| Remaining duplicate runtime labels | 1 (`Muscle-Up`) |

The structural validator passes with zero errors. The candidate is not release-ready because selection-critical and instruction-critical evidence remains incomplete. The explicit-name equipment rule resolves only `High-/Low-Pulley`, `Rings`, `Bench`, `Bar Pull-Up`, `Parallel Bar`, `Box`, and `Band` terms; generic names such as `Ball Bridge` remain unknown. The movement-type rule fills only broad categories whose V2 type has a direct top-level pattern mapping; it does not infer strength, skill, core, or limb mechanics. Both rules have registered provenance sources and validator checks. The existing V2 database's own Python tests and deterministic build are separate regression gates and should be run alongside the candidate tests.

Latest local verification on the reconstructed baseline snapshot plus the explicit-name equipment and broad movement-type rules: all 14 exercise-DB Python tests passed; the existing V2 deterministic builder/validator passed with artifact SHA-256 `fbf3d0cd4dc14d8156a3203f9f71d59a2a00f9856ccb20125ad986af4e69a430`; the candidate structural validator passed with zero errors and `releaseReady: false`. The candidate remains at 1,087 identities and 113 valid progression edges.

## Reproduce

From the repository root:

```bash
python3 -m unittest discover -s tests -p 'test_exercise_db_canonical.py' -v
python3 tools/build_exercise_db_canonical.py
python3 tools/validate_exercise_db_canonical.py
```

The candidate is written only under `exercise-db-canonical/`; the runtime asset and V2 database inputs are not replaced.
