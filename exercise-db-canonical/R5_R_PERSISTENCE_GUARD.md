# R5-R authoritative-ledger persistence guard

This guard documents the fail-closed persistence contract discovered during V3.10C-R5-R remediation.

1. Never interpret an empty `fetch_file.content` as an empty repository file when Git tree metadata reports a non-zero blob size.
2. Before replacing any authoritative R5 register, verify the current tree blob SHA and size. Abort if the retrieved source cannot be parsed completely.
3. Build the replacement in memory/temp storage, parse it, verify non-zero cardinality, uniqueness of `provenanceId`, and expected old-cardinality + validated-delta before replacement.
4. After write, verify the commit tree blob size is non-zero and the commit diff contains the expected additions/deletions. For files above the connector content limit, validate via tree/blob metadata and commit diff rather than `fetch_file.content`.
5. Checkpoint is always the final write after field-decisions, evidence, provenance, patch and research/source artifacts cross-validate.
6. Any mismatch is fail-closed: do not advance processed/resolved/remaining counters.

Root cause recorded 2026-10-05: GitHub connector `fetch_file` returned empty content for ~1 MiB JSON files while the Git tree and commit diff proved the blobs were intact. This was a read-path size limitation/false-empty, not repository truncation.
