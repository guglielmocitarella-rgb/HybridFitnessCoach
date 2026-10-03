# Canonical exercise database candidate

This is a reproducible, structurally validated candidate. It is not release-ready: see `CHECKLIST_STATUS.md` and `VALIDATION_REPORT.json` for the remaining evidence and metadata blockers.

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
