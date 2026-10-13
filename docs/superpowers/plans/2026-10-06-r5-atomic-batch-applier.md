# R5 Atomic Batch Applier Implementation Plan

Goal: persist already decided, evidence-backed batches with deterministic routing, no semantic inference, checkpoint last and recoverable writes.

Authority: user-supplied Atomic Batch Applier Remediation TXT; explicit autonomous execution and commit/push after PASS. Baseline d9dcd79802a6b3a0844461eb564132df5a17cc42. PR metadata stale is non-blocking.

Architecture: Python standard-library helper; raw per-family entries preserve evidence independently; validate in memory; durable write-ahead journal, exclusive lock, per-file replacement and checkpoint last; content-addressed receipt for replay. Only authoritative shards/manifests/checkpoint and receipts are writable.

Constraints: no research, no new cells, no candidate/runtime edits; checkpoint 1432/1413/19/2403; 441 prescription-dependent cells preserved. Historical prone/push 695264… to b631c29… is the only regression batch.

1. Acquire hash-verified R5 state, immutable residual allowlist and historical fixture. Store a compact regression fixture with exact Git blob identities.
2. Write targeted failing tests in tests/test_r5_apply_batch.py: valid batch, duplicate/conflict, stale baseline/checkpoint, missing evidence/provenance, routing, replay, arithmetic, referential integrity and preservation. Include interruptions at each write, malformed input, journal tampering and concurrent callers.
3. Implement tools/r5_apply_batch.py and transaction helper; validate original state and proposed outputs; never write on validation failure. Run focused tests after changes.
4. Run full historical lossless regression and unchanged-authority gate; document exact JSON contract and recovery limits in docs/R5_ATOMIC_BATCH_APPLIER.md.
5. Review implementation, run final focused suite, publish only new helper/test/documentation/fixture files using a Git tree based on authorized HEAD and compare-and-swap ref update. Verify final remote tree and branch HEAD. Stop.

Review focus: forged receipts; partial writes; stale HEAD during writes; false-empty/truncated inputs; authoritative historical metadata irregularities. Tests must distinguish existing immutable records from new records and may not normalize historical payloads.

Execution ledger: remote baseline/checkpoint verified; current and historical R5 blobs acquired through connector (direct git authentication unavailable). Initial observation: no remote modifications at acquisition.

Gate result: FAIL. Historical and current exact blobs have 11 research-log routing mismatches confirmed independently with repository Guard V2 functions. User-required stop applied. Completed: hash-verified fixture, lossless historical payload extraction, read-only preflight and 11 focused tests. Unvalidated write-capable drafts excluded from safe checkpoint. Remote checkpoint must be INCOMPLETE — RESUMABLE; no authoritative changes. Resume details: docs/r5-atomic-batch-applier/RESUME.md.

User later authorized a safe incomplete checkpoint before interruption or gate failure. This supersedes the initial PASS-only publication step for the verified read-only tooling and fixture. Independent review confirmed blocker; review findings addressed with failing-then-passing coverage for complete fixture hash inventory and truncated XZ JSON reporting. Write-capable drafts excluded.
