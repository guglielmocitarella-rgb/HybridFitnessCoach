# R5-R authoritative-ledger persistence guard V2

Status: ACTIVE_AFTER_MIGRATION_GATE_PASS

## Real storage layout
Authoritative incremental storage is `exercise-db-canonical/r5-authoritative-shards/` with exactly five families: `field-decisions`, `evidence`, `provenance`, `research-log`, and `patch`. Each family has 16 deterministic shards plus a manifest. Historical monoliths are preserved as migration snapshots/compatibility artifacts; after promotion they are not the incremental write authority.

Partition algorithm/version: FNV1a32 UTF-16-code-unit implementation used by the persisted migration, over `canonicalExerciseId|field-or-dimension|provenanceId`, unsigned result modulo 16. Ordering uses the same semantic key. Do not change the partition implementation without a separately gated migration.

## Fail-closed contract
1. Connector `content=""` is not proof of an empty Git object when tree/blob metadata reports non-zero content. Resolve the blob/tree metadata or direct blob before any destructive action.
2. Read checkpoint + family manifest first. Determine the affected FNV bucket(s), verify JSON/schema/key uniqueness, and update only those shards.
3. A write is idempotent by semantic key. Existing identical entries are no-op; conflicting duplicates abort.
4. Validate the replacement shard before write. Re-read remote shard/blob after write. Manifest advances only after all affected shards validate.
5. Detect missing shards, manifest count/blob mismatch, duplicate/conflicting semantic keys, orphan evidence, orphan provenance, decision without evidence/provenance, and patch without matching decision.
6. No processed/resolved checkpoint advancement unless decision + evidence + provenance + patch are mutually consistent and required research/source trail is persisted.
7. Any interrupted write resumes from remote HEAD/tree. Never replay a write blindly.
8. Checkpoint is the final semantic write after cross-family validation. Storage-only migration does not advance capability counters.
9. Every promotion/update requires remote branch HEAD and PR HEAD verification.

## Regression requirements
The validator must cover deterministic FNV1a32 mod 16, stable assignment, 16 shards for all five families, interrupted-write idempotence, connector false-empty protection, orphan/missing evidence/provenance, duplicate/conflicting entries, manifest mismatch, and checkpoint-advance rejection on integrity failure.

Root cause: CONNECTOR_TRANSPORT_SIZE_LIMIT / MONOLITHIC_INCREMENTAL_PERSISTENCE_SCALABILITY_GAP. Real repository data loss was not observed.
