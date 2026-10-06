# R5 Atomic Batch Applier — input contract v1

The applier persists only capability decisions that were already determined and evidence-backed. It performs no capability research, no semantic classification, no `legacyLevel` mapping/fallback, and accepts no caller-supplied shard route.

## Top-level JSON

Required fields:

- `schemaVersion`: integer `1`.
- `baselineHead`: exact 40-hex Git HEAD expected before a new apply.
- `expectedPreviousCheckpoint`: complete authoritative checkpoint object. A new apply requires lossless equality with `exercise-db-canonical/native-capability-r5-checkpoint.json`.
- `records`: ordered non-empty array. Order determines appended `processedRecordIds` and the resulting `lastProcessedId`.

Each record contains:

- `researchLog`: complete authoritative research-log payload with `canonicalExerciseId`, `exerciseName`, `identityStatus`, non-empty `sources`, `decision`, and `fieldApplication`.
- `cells`: one item for each resolved capability. Terminal unresolved fields belong only in `researchLog.fieldApplication.terminalUnresolved`.

Each source requires `title`, `publisher`, `tier`, `locator`, and `finding`.

Each resolved cell contains four complete payloads: `decision`, `evidence`, `provenance`, and `patch`.

`decision`, `evidence`, and `provenance` require:
`canonicalExerciseId`, `dimension`, `previousState`, `newState`, `value`, `evidenceType`, `sourceTier`, `sourceTitle`, `sourcePublisher`, `sourceLocator`, `evidenceSummary`, `reasoningRule`, `confidence`, `provenanceId`, `researchVersion`.

`patch` requires:
`canonicalExerciseId`, `field`, `before`, `after`, `provenanceId`, `confidence`.

The executable validator requires all four cell payloads to have the same semantic key/value relationship, requires resolved `newState = KNOWN`, and requires `researchLog.fieldApplication.resolved` to match the cells exactly.

## Routing and conflict rules

The semantic key is:

`canonicalExerciseId|field-or-dimension|provenanceId`

For research-log rows it is `canonicalExerciseId||`.

The applier computes the authoritative bucket itself as FNV1a32 over UTF-16 code units of the semantic key, unsigned modulo 16. Input must not attempt to override routing.

Duplicate semantic keys in the batch are rejected. Existing unequal payloads for the same semantic key are conflicts and are rejected. A mixture of already-existing and absent payloads is rejected as a partial replay/write.

The current repository contains 11 pre-existing research-log routing violations. Microphase A does not normalize them. A new batch must introduce zero new routing violations and must leave the pre-existing violation set unchanged.

## Checkpoint and replay

Checkpoint arithmetic is derived from the validated payload:
resolved cells + terminal unresolved cells advance `processedResidualCells`; resolved cells advance `resolvedCells` and `provenanceEntries`; terminal fields advance `terminalUnresolvedCells`; record count advances `sourcesVisited`; `remainingCells` decreases by processed cells.

Checkpoint is written last.

A replay is accepted without mutation only when every batch payload is already present exactly, is correctly routed, and the current checkpoint equals the checkpoint deterministically derived from `expectedPreviousCheckpoint` plus that batch.

## CLI

Input-only validation:

`python3 tools/r5_apply_batch.py --batch BATCH.json --validate-input`

Repository-aware dry run (default, no writes):

`python3 tools/r5_apply_batch.py --root . --batch BATCH.json`

Apply:

`python3 tools/r5_apply_batch.py --root . --batch BATCH.json --apply`

Writes are restricted to the five R5 authoritative shard families, their manifests, and the R5 checkpoint. Runtime/candidate DB and production paths are not writable by this helper.
