# Canonical exercise database candidate

This is a reproducible, structurally validated candidate. It is not release-ready: see `CHECKLIST_STATUS.md` and `VALIDATION_REPORT.json` for the remaining integration and metadata gates.

`applicationMetadata.sourceEntries[]` preserves effective values from the current Kotlin exercise catalogue, including app defaults and `withExerciseMetadata` rules. It is a separate comparison snapshot with per-field provenance, not exercise-science evidence and not a canonical prescription. `ambiguousSourceEntries[]` retains runtime metadata that matches more than one V2 identity without assigning it to either record.

The repository-wide source inventory and the fields found or still absent from internal sources are documented in `INTERNAL_SOURCE_AUDIT.md`.

Researched external values are kept in the canonical enrichment/evidence artifacts and applied through the builder/promotion pipeline. Field-level evidence and provenance remain independent; broad movement-family evidence is not silently generalized to unsupported variants.

The exact generated candidate and audit may be stored as gzip archives. Restore them with:

```sh
gzip -dk exercise-db-canonical.json.gz
gzip -dk exercise-db-canonical-audit.json.gz
```

To regenerate both from committed repository inputs:

```sh
python3 tools/build_exercise_db_canonical.py
python3 tools/validate_exercise_db_canonical.py
```

## R5-R capability evidence status

R5-R evidence production has reached **data-complete set-based state** on the V29 recovery line.

Canonical R5 partition:

- universe: 3,835 semantic keys
- resolved in universe: 3,787
- terminal in universe: 48
- TRUE_MISSING: 0
- authoritative out of universe: 214, preserved separately for final audit
- total authoritative entries: 4,001

The partition `3787 + 48 + 0 = 3835` passes set-based validation. Remaining is derived from the materialized TRUE_MISSING set; historical decrement-only remaining counters are not authoritative.

This does **not** mean runtime integration is complete. Candidate DB, runtime DB and production path remained unchanged during R5-R production.

Primary R5 documents:

- `R5_R_FINAL_STATUS.md` — completion snapshot and next-phase boundaries
- `R5_R_PERSISTENCE_GUARD.md` — active set-based persistence contract
- `NATIVE_CAPABILITY_EXTERNAL_EVIDENCE_RESOLUTION_V3_10C_R5.md` — R5 evidence-resolution history/current state
- `native-capability-r5-universe.json` — canonical semantic-key universe
- `native-capability-r5-terminal-membership.json` — terminal membership
- `native-capability-r5-true-missing.json` — materialized residual workset
- `native-capability-r5-authoritative-out-of-universe.json` — isolated out-of-universe authoritative set
- `native-capability-r5-set-validation.json` — set-partition validation

The next phase after target-branch finalization is a single final read-only audit, followed by explicit disposition of the 214 out-of-universe entries and only then candidate/runtime integration work.
