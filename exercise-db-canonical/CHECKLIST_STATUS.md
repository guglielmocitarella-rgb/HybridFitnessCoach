# Exercise DB canonical candidate — checklist status

**Audit baseline:** `feature/v0.9.5` at `9b8a41cfe5c7ee007f37219f9987a843f3ae3911` (Android APK #873).  
**Latest remote checkpoint before this audit block:** `feature/v0.9.5` commit `13d071a04737f3a14d48eaaf35c34df1cd173762` (Android APK workflow #883 succeeded on this SHA).  
**State:** candidate materialized and structurally validated; internal-source audit now complete; **not ready for app integration**.

## Reconciliation

The code at this baseline has 587 V2 records and 583 distinct current-model entries. The runtime's exact name/alias lookup leaves 504 current-model-only entries, giving 1,091 source entries before identity review.

The canonical candidate has 1,087 identities after preserving three unique punctuation-normalized links to V2 IDs and merging the two current-model labels `Straight arm pulldown` / `Straight-arm pulldown`. The merged record retains both labels, both source locations, and the `Front lever` skill label. This is a documented identity decision based on the identical exercise label apart from punctuation and matching app-model fields.

`Muscle-Up` and `Normal Muscle-Up` remain separate V2 records. The `Muscle-Up` lookup alias points to both, so the candidate marks that app-model relationship ambiguous and does not merge the records or copy selection metadata from either app match. The candidate uses their distinct canonical names as runtime labels; the source app's shared alias remains explicitly recorded as ambiguous.

## Checklist

| Checkpoint item | Status | Evidence / remaining work |
|---|---|---|
| 1. Materialize one candidate catalog | **Partial** | Candidate contains 1,087 records with stable unique IDs, original V2 content, current-model metadata, source references, and field provenance. A separate `applicationMetadata` snapshot now preserves all 583 runtime entries' effective direct values/defaults/rules; several exercise-science fields remain unknown. |
| 2. Resolve identity and variants | **Partial** | Three unique V2/current-model name variants were linked; the straight-arm duplicate was merged with provenance retained. Muscle-Up identities stay separate with ambiguous app provenance and distinct candidate runtime labels. No progression edge was invented. |
| 3. Complete equipment and provenance | **Partial** | Equipment tokens are normalized and unknown is distinct from known-empty. The internal audit found 18 records with known `suspension_trainer`, which current `EquipmentCatalog.kt` does not support; the validator now tracks that app-compatibility blocker separately. 294 records still have unknown equipment requirements. |
| 4. Complete selection-critical fields | **Not complete** | Unknowns remain: equipment 294, movement pattern 201, level 506, resistance mode 338, SkillCatalog links 143. Another 18 records use known equipment not currently supported by the app catalog. Ambiguous Muscle-Up metadata stays unassigned. App-rule-derived values are labeled separately from exercise-science evidence. |
| 5. Complete movement-pattern taxonomy | **Partial** | Direct V2 movement types resolve 75 broad patterns. Whitelisted V2-family rules resolve 230, including the coherent `CORE` family and explicit exceptions for mixed pull-up/push-up families. 201 remain unknown; internal raw inventories contain no explicit movement-pattern column. |
| 6. Link skills and progressions by stable IDs | **Partial** | All 113 V2 progression edges have valid endpoints and registered sources. The builder reads active legacy `SkillProgressionNode` entries and both positional and mapped aliases as the app does; 60/60 active SkillCatalog nodes resolve to a runtime exercise label or alias. 143 candidate skill exercises still lack a specific node link. |
| 7. Complete training and instruction metadata | **Not complete** | 500 current-model-only identities have no instructions. Internal V2 records provide supported description/setup/execution for the 587 V2 movements, but internal inventories contain no cues, errors, anatomical fields, or exercise-specific prescription for the remaining entries. External exercise-specific references are required after this source audit. |
| 8. Add an automatic validator | **Complete for the current candidate gates** | `tools/validate_exercise_db_canonical.py` checks IDs, dangling refs, registered provenance, equipment tokens and app compatibility, SkillCatalog IDs, `EXTERNAL_LOAD`/equipment consistency, and incomplete-evidence release gating. Further reachability and domain checks remain. |
| 9. Simulate reachability by location, capability, and skill | **Partial** | Name/alias coverage confirms all 60 active SkillCatalog nodes resolve to at least one runtime exercise label. Full eligibility simulation by location, equipment, capability, and workout filters remains blocked by incomplete selection metadata. Unknown equipment remains fail-closed. |
| 10. Measure current generator diversity | **Not run** | Requires running the existing generator against the reconciled catalog and representative histories/configurations. |
| 11. Design controlled ranking diversification | **Not started** | Requires reachability and diversity measurements first. |
| 12. Prepare integration and remote checkpoint | **Partial** | Candidate generator, validator, tests, checklist, report, and deterministic candidate/audit archives are checkpointed on `feature/v0.9.5` at `13d071a04737f3a14d48eaaf35c34df1cd173762`; this internal-source audit block is pending its checkpoint. App runtime files were not integrated; integration remains blocked by incomplete selection-critical and instruction data. |
| 13. Audit internal/originating sources before external research | **Complete for repository sources** | Audited V2 assets and 11 raw inventories, all runtime catalogs, `ExerciseMetadata.kt`, equipment/skill catalogs, legacy nodes, V2 adapter, builders, validators, manifests, docs and available imports. Added `INTERNAL_SOURCE_AUDIT.md`; restored the omitted Kotlin helper locally and preserved runtime metadata with per-field provenance. Remaining UNKNOWN fields have no sufficient per-exercise evidence in repository inputs. |

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
| Unknown equipment | 294 |
| Equipment tokens unsupported by current app | 18 (`suspension_trainer`) |
| Missing movement patterns | 201 |
| Unknown level | 506 |
| Unknown resistance mode | 338 |
| Skill exercises without a stable node link | 143 |
| Missing supported exercise instructions | 500 |
| Remaining duplicate runtime labels in candidate | 0 |
| Runtime application entries audited/preserved | 583 |

The structural validator passes with zero errors. The candidate is not release-ready because selection-critical and instruction-critical evidence remains incomplete. The repository-wide internal audit found no further per-exercise technical fields for unresolved V2-only records: the raw TSVs contain inventory names/families/locators, not equipment, level, resistance, movement pattern, or technique. Runtime-only metadata is now preserved separately; it is not promoted into exercise-science claims. Eighteen otherwise known suspension-trainer records remain unsupported by the current app equipment enum. All rule-derived values have registered provenance, and the validator keeps these unresolved/app-compatibility blockers visible. The existing V2 deterministic build and tests remain separate regression gates.

Latest local verification for this internal-source audit block: `python3 -m unittest discover -s tests -v` passed (23 tests); both V2 and canonical builders and validators passed; canonical validator reports `ok: true`, zero errors, and `releaseReady: false`; candidate remains at 1,087 unique IDs and 113 progression edges, with 582 unambiguous app metadata snapshots plus 2 ambiguous references; both regenerated gzip artifacts pass `gzip -t`. The remaining blockers are equipment 294, app-unsupported equipment 18, movement pattern 201, level 506, resistance mode 338, stable skill links 143, and supported instructions 500. Android APK #883 succeeded on the previous checkpoint `13d071a`; this audit block does not modify runtime integration files.

## Reproduce

From the repository root:

```bash
python3 -m unittest discover -s tests -p 'test_exercise_db_canonical.py' -v
python3 tools/build_exercise_db_canonical.py
python3 tools/validate_exercise_db_canonical.py
```

The candidate is written only under `exercise-db-canonical/`; the runtime asset and V2 database inputs are not replaced.
