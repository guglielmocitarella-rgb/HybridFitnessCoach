# R5-R authoritative-ledger persistence guard V3 — set-based

Status: ACTIVE_SET_BASED / R5_DATA_COMPLETE

## Real storage layout
Authoritative incremental storage is `exercise-db-canonical/r5-authoritative-shards/` with exactly five families: `field-decisions`, `evidence`, `provenance`, `research-log`, and `patch`. Each family has 16 deterministic shards plus a manifest. Historical monoliths are preserved as migration snapshots/compatibility artifacts; after promotion they are not the incremental write authority.

Partition algorithm/version: FNV1a32 UTF-16-code-unit implementation used by the persisted migration, over `canonicalExerciseId|field-or-dimension|provenanceId`, unsigned result modulo 16. Ordering uses the same semantic key. Do not change the partition implementation without a separately gated migration.

## Set-based source of truth
R5 no longer treats decrement-only checkpoint arithmetic as authoritative.

- `native-capability-r5-universe.json` materializes the canonical R5 universe.
- `native-capability-r5-terminal-membership.json` materializes terminal semantic keys.
- `native-capability-r5-true-missing.json` materializes the workset.
- `native-capability-r5-authoritative-out-of-universe.json` isolates authoritative entries outside the canonical universe.
- `native-capability-r5-set-validation.json` validates the partition.

The canonical universe is reconstructed deterministically from the R3 unresolved semantic-key set minus the R3 resolved patch. Its cardinality is 3,835 unique keys.

At V29 data completion the partition is:

`3787 RESOLVED_IN_UNIVERSE + 48 TERMINAL_IN_UNIVERSE + 0 TRUE_MISSING = 3835 U_R5`.

The 214 authoritative out-of-universe entries remain preserved and are excluded from this partition pending final audit.

`remainingCells` MUST equal `|TRUE_MISSING|`. Legacy counter arithmetic is historical metadata only and MUST NOT be used to invent or identify remaining capability keys.

## Fail-closed contract
1. Connector `content=""` is not proof of an empty Git object when tree/blob metadata reports non-zero content. Resolve blob/tree metadata or direct blob before destructive action.
2. Read checkpoint + family manifest + canonical universe + TRUE_MISSING first. Determine affected FNV buckets, verify JSON/schema/key uniqueness, and update only those shards.
3. Every newly processed semantic key MUST belong to `U_R5` and to the current `TRUE_MISSING`. A key outside the universe or already classified MUST be rejected.
4. A write is idempotent by semantic key. Existing identical entries are no-op; conflicting duplicates abort.
5. Validate replacement shards before write and re-read remote shard/blob after write. Manifest advances only after all affected shards validate.
6. Detect missing shards, manifest count/blob mismatch, duplicate/conflicting semantic keys, orphan evidence/provenance, decision without evidence/provenance, and patch without matching decision.
7. No resolved/terminal/checkpoint advancement unless decision/evidence/provenance/patch and required research/source trail are mutually consistent.
8. After a promotion reconstruct `TRUE_MISSING_NEW = U_R5 - RESOLVED_IN_UNIVERSE_NEW - TERMINAL_IN_UNIVERSE_NEW`; derive `remainingCells` from its cardinality.
9. Any interrupted write resumes from remote HEAD/tree. Never replay a write blindly.
10. Checkpoint is the final semantic write after cross-family and set-partition validation.
11. Every promotion/update requires remote branch HEAD and final ref verification.

## Completion state
R5 evidence production is complete when `TRUE_MISSING = 0`. V29 reached that state on the recovery line at commit `a280a6b23095525782b259cbd2b28d371f570138` with set-based validation PASS. Formal target-branch finalization is tracked separately from semantic data completion.

See `R5_R_FINAL_STATUS.md` for the completion snapshot and next-phase boundaries.

## Regression requirements
The validator/test suite must cover deterministic FNV1a32 mod 16, stable assignment, 16 shards for all five families, interrupted-write idempotence, connector false-empty protection, orphan/missing evidence/provenance, duplicate/conflicting entries, manifest mismatch, universe membership, TRUE_MISSING membership, rejection of already-classified keys, set-partition reconstruction, and checkpoint-advance rejection on integrity failure.

Historical root cause of the sharded persistence migration: CONNECTOR_TRANSPORT_SIZE_LIMIT / MONOLITHIC_INCREMENTAL_PERSISTENCE_SCALABILITY_GAP. The later set-based migration additionally corrected COUNTER_ONLY_REMAINING_WITHOUT_MATERIALIZED_UNIVERSE semantics.
