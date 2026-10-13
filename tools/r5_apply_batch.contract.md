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

The current repository contains 11 pre-existing research-log routing violations. They are not normalized by this applier. A new batch must introduce zero new routing violations and must leave the pre-existing violation set unchanged.

## Checkpoint, idempotency, and arithmetic

Checkpoint arithmetic is derived from the validated payload:
resolved cells + terminal unresolved cells advance `processedResidualCells`; resolved cells advance `resolvedCells` and `provenanceEntries`; terminal fields advance `terminalUnresolvedCells`; record count advances `sourcesVisited`; `remainingCells` decreases by processed cells.

A replay is accepted without mutation only when every batch payload is already present exactly, is correctly routed, and the current checkpoint equals the checkpoint deterministically derived from `expectedPreviousCheckpoint` plus that batch. An accepted replay performs zero writes, so counters and semantic rows are not duplicated.

## Atomicity and recovery boundary

The repository does not provide filesystem-wide transactions. The implemented safety contract is therefore recoverable transactionality over the authoritative files touched by one batch:

1. validate the batch and current authoritative state;
2. stage old and new bytes for every touched file under `.r5-apply-batch-txn/<batchId>/`;
3. write a journal containing old/new hashes for every staged target;
4. atomically replace the touched shard/manifest data files;
5. validate those replaced data files against the journaled new hashes;
6. atomically replace the checkpoint **last**;
7. validate the checkpoint hash and remove the transaction journal.

If an exception occurs after the journal exists, recovery accepts only targets whose bytes still match either the journaled old or new hash, then restores all journaled old bytes. A process interruption can therefore leave a recoverable mixed old/new data state, but not a committed checkpoint written before data validation. On the next same-batch `--apply`, the CLI rolls the interrupted transaction back and returns a controlled failure asking for one rerun; that rerun revalidates from the restored state and applies normally. A later replay is idempotent with zero writes.

This is intentionally not a claim of global filesystem atomicity. An unrelated external writer that ignores the applier lock remains outside this transaction boundary; hash checks cause the applier to fail rather than silently accept an unexpected semantic mutation.

## CLI

Input-only validation:

`python3 tools/r5_apply_batch.py --batch BATCH.json --validate-input`

Repository-aware dry run (default, no writes):

`python3 tools/r5_apply_batch.py --root . --batch BATCH.json`

Apply:

`python3 tools/r5_apply_batch.py --root . --batch BATCH.json --apply`

Writes are restricted to the five R5 authoritative shard families, their manifests, and the R5 checkpoint. Runtime/candidate DB and production paths are not writable by this helper.
