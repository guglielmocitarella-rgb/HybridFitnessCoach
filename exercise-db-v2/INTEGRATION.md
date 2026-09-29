# Exercise DB v2 — integration contract

This branch intentionally does **not** replace the RC2 production catalogues.

## Canonical artifacts
- `exercise-db-v2/exercises.json`: canonical movement records.
- `exercise-db-v2/progressions.json`: source-backed relationships between canonical IDs.
- `exercise-db-v2/sources.json`: normalized source registry generated from `sources.tsv`.
- `exercise-db-v2/BUILD_MANIFEST.json`: counts, validation state and artifact hash.

Run `python tools/build_exercise_db.py`; it normalizes ordering/serialization, regenerates the source registry and manifest counts, then runs the validator. It does not infer missing exercise evidence.

## RC2 import
Parse by stable `id`. Display `nameIt` in Italian and retain `canonicalNameEn` for canonical lookup. Resolve legacy names against canonical names and `aliases[]`; persist the v2 stable ID after a successful migration. Ambiguous aliases must not be auto-migrated.

Equipment filters should compare normalized tokens from `equipment[]` with the athlete profile equipment taxonomy. Do not compare localized display labels.

Progression relationships are queried from `progressions.json` by `from`, `to`, and `relationship`. `MAIN_PROGRESSION` and `PREREQUISITE` are validated for referential integrity and cycles.

Prescription and rest evidence remain source/context scoped. The consumer must select entries by goal/context; absence means “no source-backed v2 guidance”, not permission to synthesize an exercise-specific value.

Provenance is exposed through each movement's `sources[]` and edge `sources[]`. Field-specific evidence, when present, must remain attached to that field rather than being promoted to whole-record evidence.

## Deferred / intentionally missing
Videos are deferred. Unsupported technique cues, common errors, contraindications, muscles, prescriptions or rest guidance may be empty. Empty evidence-backed fields must not be backfilled merely to reach completeness.

## Compatibility
Integration into `feature/v0.9.1-training-plan` must be a separate change. Do not delete the legacy catalogue until legacy-name migration and persisted workout/history compatibility are tested.
