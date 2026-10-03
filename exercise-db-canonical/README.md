# Canonical exercise database candidate

This is a reproducible, structurally validated candidate. It is not release-ready: see `CHECKLIST_STATUS.md` and `VALIDATION_REPORT.json` for the remaining evidence and metadata blockers.

`applicationMetadata.sourceEntries[]` preserves effective values from the current Kotlin exercise catalogue, including app defaults and `withExerciseMetadata` rules. It is a separate comparison snapshot with per-field provenance, not exercise-science evidence and not a canonical prescription. `ambiguousSourceEntries[]` retains runtime metadata that matches more than one V2 identity without assigning it to either record.

The repository-wide source inventory and the fields found or still absent from internal sources are documented in `INTERNAL_SOURCE_AUDIT.md`.

Researched external values are kept in `external-enrichment.json` and applied by the builder. Each value records its original observation, field-level evidence type, source URL, and confidence. The current ACE tranche covers equipment, difficulty, setup, execution, cues, and common errors for ten individually matched exercises; values from a broad class of movements are not generalized to unresearched variants. Conflicting ACE difficulty observations for a side-plank item are retained as unresolved and are not applied to a candidate record.

The exact generated candidate and audit are stored as gzip archives because the candidate JSON is 4.7 MB. Restore them with:

```sh
gzip -dk exercise-db-canonical.json.gz
gzip -dk exercise-db-canonical-audit.json.gz
```

To regenerate both from the committed repository inputs instead:

```sh
python3 tools/build_exercise_db_canonical.py
python3 tools/validate_exercise_db_canonical.py
```
