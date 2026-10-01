# Exercise DB v2 — current integration contract

Exercise DB v2 is integrated into the active v0.9.5 line, inherited from the frozen v0.9.4 baseline. The canonical database is source/evidence driven; compatibility with obsolete app persistence schemas is not a release requirement.

## Canonical artifacts
- `exercise-db-v2/exercises.json`: canonical movement records.
- `exercise-db-v2/progressions.json`: source-backed relationships between canonical IDs.
- `exercise-db-v2/sources.json`: normalized source registry.
- `exercise-db-v2/programming_guidance.json`: goal/context-scoped general prescription/rest evidence; never promote these rows to exercise-specific evidence.
- `exercise-db-v2/BUILD_MANIFEST.json`: deterministic counts, validation state and artifact hash.

Run `python tools/build_exercise_db.py` to verify deterministic canonical artifacts and manifest hashes, then `python tools/validate_exercise_db.py` for schema, provenance and raw-inventory reconciliation. The build command verifies committed artifacts; it must not fabricate missing evidence.

## Runtime import

Parse by stable `id`. Display `nameIt` in Italian and retain `canonicalNameEn`/aliases for lookup. Equipment compatibility uses canonical equipment IDs/capabilities and `TrainingLocation`; localized labels are presentation only.

Progression relationships are queried from `progressions.json` by `from`, `to`, and `relationship`. Validated graph relationships retain their source provenance.

Prescription/rest evidence remains source/context scoped. General rules come from `programming_guidance.json`; exercise-specific fields count as source-based only when evidence is attached to that movement. Absence means no verified exercise-specific guidance and must not be silently upgraded.

Execution content uses explicit evidence tiers. F01 repository validation requires usable canonical execution content for selectable exercises while preserving the distinction between source-backed technique, app rules/family guidance, fallbacks and unverified material.

## Current integration boundary

- F01 evidence tier/validator/tests are green at checkpoint `e4c4f208…`.
- Canonical equipment integrates with `TrainingLocation`; localized equipment strings are not a compatibility contract.
- Plan schema v5 intent-only integration is repository-level verified on `fcfcbb200…`; planned sessions do not retain DB exercise snapshots. Concrete exercise definitions are materialized for the active runner and retained in execution/history evidence where required.
- Frozen v0.9.4 evidence: Exercise DB v2 #292 SUCCESS and Android APK #845 SUCCESS at runtime `cfacd7e3…`.\n- v0.9.5 continues from that integrated database; current release status belongs in `PROJECT_STATUS.md` and must not be duplicated here.
- Legacy plan schema migration and legacy-only catalogue retention are not required solely for backward compatibility.
- Videos remain deferred unless separately promoted into the active release DoD.
