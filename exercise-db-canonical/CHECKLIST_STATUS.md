# Exercise DB canonical candidate — checklist status

**Audit baseline:** `feature/v0.9.5` at `9b8a41cfe5c7ee007f37219f9987a843f3ae3911` (Android APK #873).  
**Checkpoint this enrichment block started from:** `feature/v0.9.5` commit `dd69bee6983c1dcecd71211ad730675ebdb29834`.  
**State:** internal-source audit complete; first ACE equipment, level, and technique tranche verified; **not ready for app integration**.

## Reconciliation

The code at this baseline has 587 V2 records and 583 distinct current-model entries. The runtime's exact name/alias lookup leaves 504 current-model-only entries, giving 1,091 source entries before identity review.

The canonical candidate has 1,087 identities after preserving three unique punctuation-normalized links to V2 IDs and merging the two current-model labels `Straight arm pulldown` / `Straight-arm pulldown`. The merged record retains both labels, both source locations, and the `Front lever` skill label. This is a documented identity decision based on the identical exercise label apart from punctuation and matching app-model fields.

`Muscle-Up` and `Normal Muscle-Up` remain separate V2 records. The `Muscle-Up` lookup alias points to both, so the candidate marks that app-model relationship ambiguous and does not merge the records or copy selection metadata from either app match. The candidate uses their distinct canonical names as runtime labels; the source app's shared alias remains explicitly recorded as ambiguous.

## Checklist

| Checkpoint item | Status | Evidence / remaining work |
|---|---|---|
| 1. Materialize one candidate catalog | **Partial** | Candidate contains 1,087 records with stable unique IDs, original V2 content, current-model metadata, source references, and field provenance. A separate `applicationMetadata` snapshot now preserves all 583 runtime entries' effective direct values/defaults/rules; several exercise-science fields remain unknown. |
| 2. Resolve identity and variants | **Partial** | Three unique V2/current-model name variants were linked; the straight-arm duplicate was merged with provenance retained. Muscle-Up identities stay separate with ambiguous app provenance and distinct candidate runtime labels. No progression edge was invented. |
| 3. Complete equipment and provenance | **Partial** | Equipment tokens are normalized and unknown is distinct from known-empty. The internal audit found 18 records with known `suspension_trainer`, which current `EquipmentCatalog.kt` does not support; this remains a compatibility blocker. ACE exercise-specific evidence resolved equipment for 32 records; 262 still have unknown equipment. |
| 4. Complete selection-critical fields | **Not complete** | Remaining unknowns: equipment 272, movement pattern 201, level 484, resistance mode 338, SkillCatalog links 143. Another 18 records use known equipment not currently supported by the app catalog. Ambiguous Muscle-Up metadata stays unassigned. App-rule-derived values are labeled separately from exercise-science evidence. |
| 5. Complete movement-pattern taxonomy | **Partial** | Direct V2 movement types resolve 75 broad patterns. Whitelisted V2-family rules resolve 230, including the coherent `CORE` family and explicit exceptions for mixed pull-up/push-up families. 201 remain unknown; internal raw inventories contain no explicit movement-pattern column. |
| 6. Link skills and progressions by stable IDs | **Partial** | All 113 V2 progression edges have valid endpoints and registered sources. The builder reads active legacy `SkillProgressionNode` entries and both positional and mapped aliases as the app does; 60/60 active SkillCatalog nodes resolve to a runtime exercise label or alias. 143 candidate skill exercises still lack a specific node link. |
| 7. Complete training and instruction metadata | **Not complete** | 500 current-model-only identities have no instructions. Thirty-one V2 records now have exercise-specific ACE descriptions, setup, execution, cues, and common errors; one additional reviewed record has ACE equipment/level evidence only. Other missing cues, anatomy, and prescriptions still require individual evidence. |
| 8. Add an automatic validator | **Complete for the current candidate gates** | `tools/validate_exercise_db_canonical.py` checks IDs, dangling refs, registered provenance, external evidence URL/type/confidence, equipment tokens and app compatibility, SkillCatalog IDs, `EXTERNAL_LOAD`/equipment consistency, and incomplete-evidence release gating. Further reachability and domain checks remain. |
| 9. Simulate reachability by location, capability, and skill | **Partial** | Name/alias coverage confirms all 60 active SkillCatalog nodes resolve to at least one runtime exercise label. Full eligibility simulation by location, equipment, capability, and workout filters remains blocked by incomplete selection metadata. Unknown equipment remains fail-closed. |
| 10. Measure current generator diversity | **Not run** | Requires running the existing generator against the reconciled catalog and representative histories/configurations. |
| 11. Design controlled ranking diversification | **Not started** | Requires reachability and diversity measurements first. |
| 12. Prepare integration and remote checkpoint | **Partial** | Internal audit and first external enrichment block are checkpointed independently on `feature/v0.9.5`; latest checkpoint SHA is recorded in the commit history. App runtime files were not integrated; integration remains blocked by incomplete selection-critical and instruction data. |
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
| Unknown equipment | 222 |
| Equipment tokens unsupported by current app | 18 (`suspension_trainer`) |
| Missing movement patterns | 201 |
| Unknown level | 443 |
| Unknown resistance mode | 338 |
| Skill exercises without a stable node link | 143 |
| Missing supported exercise instructions | 500 |
| Remaining duplicate runtime labels in candidate | 0 |
| Runtime application entries audited/preserved | 583 |
| External ACE records enriched | 72 records; equipment evidence 72, level evidence 63, technique evidence retained for 31 |

The structural validator passes with zero errors. The candidate is not release-ready because selection-critical and instruction-critical evidence remains incomplete. The repository-wide internal audit found no further per-exercise technical fields for unresolved V2-only records: the raw TSVs contain inventory names/families/locators, not equipment, level, resistance, movement pattern, or technique. Runtime-only metadata is now preserved separately; it is not promoted into exercise-science claims. Eighteen otherwise known suspension-trainer records remain unsupported by the current app equipment enum. All rule-derived values have registered provenance, and the validator keeps these unresolved/app-compatibility blockers visible. The existing V2 deterministic build and tests remain separate regression gates.

The current equipment-first checkpoint covers 72 reviewed ACE-backed records. This tranche screened all 242 remaining equipment-UNKNOWN IDs and applied 20 HIGH-confidence mappings, reducing equipment UNKNOWN from 242 to 222 and level UNKNOWN from 463 to 443. The ExRx exercise database was evaluated as the secondary structured source, but no mapping from it was promoted in this tranche because directly verifiable exercise-specific equivalence did not improve on the ACE matches without increasing ambiguity. No new technique fields were promoted from generic/listing evidence. Existing ambiguity exclusions remain unchanged. Canonical CI passed; builder, validator and deterministic gzip checks passed; validation remains `ok: true`, zero errors and `releaseReady: false`. Runtime integration files remain unchanged.

## Reproduce

From the repository root:

```bash
python3 -m unittest discover -s tests -p 'test_exercise_db_canonical.py' -v
python3 tools/build_exercise_db_canonical.py
python3 tools/validate_exercise_db_canonical.py
```

The candidate is written only under `exercise-db-canonical/`; the runtime asset and V2 database inputs are not replaced.
