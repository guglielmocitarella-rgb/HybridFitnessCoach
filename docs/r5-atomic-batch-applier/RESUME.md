# INCOMPLETE — RESUMABLE: Atomic Batch Applier

Completion gate: **FAIL**. No working applier has been accepted or promoted.

## Authorized baseline and unchanged state

- Branch: `feature/exercise-db-v3`.
- Parent baseline: `d9dcd79802a6b3a0844461eb564132df5a17cc42`.
- PR #15 metadata remains `b631c29f95f693934525dd21706d3717e3b64c57`: already classified `STALE_PR_METADATA — NON_BLOCKING`; do not repair it.
- Checkpoint: **1432 processed / 1413 resolved / 19 terminal / 2403 remaining**.
- Last processed: `cm-push-up-inclinato`. Do not start `cm-push-up-to-shoulder-tap`.
- This commit adds only tooling, tests, fixture, example and documentation. Every previously existing repository file, including all authoritative shards, checkpoint, candidate/runtime databases and production paths, is unchanged. The existing 441 prescription-dependent state is preserved by unchanged blobs, not re-audited.

## Concrete completion-gate blocker

The exact Git blobs contain **11 research-log rows in the wrong FNV bucket**. This is present in both the historical `6952648897bb713617ff1b89b74d1c45e834bf2c` state and the authorized baseline. The function defined by the existing repository `validate_guard_v2.py` independently produces the same 11 mismatches. Other four families route correctly.

| Canonical ID | Persisted bucket | Required bucket |
|---|---:|---:|
| cm-knee-push-up | 04 | 0c |
| cm-landmine-press | 00 | 08 |
| cm-mixed-grip-pull-up | 04 | 0c |
| cm-mobilit-polsi | 08 | 00 |
| cm-mountain-climber | 00 | 08 |
| cm-neutral-grip-pull-up | 0c | 04 |
| cm-nordic-curl-assistito | 00 | 08 |
| cm-pallof-press | 00 | 08 |
| cm-panca-inclinata-manubri | 04 | 0c |
| cm-pause-pull-up | 04 | 0c |
| cm-pendlay-row | 0c | 04 |

`INCOMPLETE_RESUMABLE.json` includes the exact paths, source blob SHAs, semantic keys, expected and actual buckets, and counters. No relocation, migration, semantic correction or weakened routing check has been performed. The required lossless applier regression cannot PASS from a starting state that fails the required routing invariant. Per the user's explicit stop condition, implementation is not promoted past that gate.

## Persisted, verified work

- `tools/r5_batch_preflight.py`: read-only routing/manifest/key preflight, machine-readable JSON and nonzero exit on failure.
- `tests/test_r5_batch_preflight.py`: **11 passing tests**, including Git blob identity, exact blocker reproduction, FNV vectors, UTF-16 surrogate handling, corruption/false-empty rejection, historical batch arithmetic and lossless payload extraction.
- `tests/fixtures/r5-prone-push.json.xz`: exact original pre-batch files, original Git blob identities, batch payload, final checkpoint, and final family semantic fingerprints.
- `tools/examples/r5-prone-push.batch.json`: extracted historical batch, **27 cells / 8 records**, no new capability decisions.
- Fixture extraction reproduces the existing final payloads in all five families, but this is **not** a successful run of an applier. Do not relabel it `regression lossless PASS`.

The preliminary applier implementation and its failing development tests were **not promoted**: they had not passed validation. The safe checkpoint includes the coherent verified work, rather than publishing an unvalidated write-capable command. No applier invocation is available yet.

## Reproduce, without research or writes

```bash
python3 -m unittest discover -s tests -p test_r5_batch_preflight.py -v
# Expected: 11 tests PASS.
python3 tools/r5_batch_preflight.py --root .
# Expected: exit 1, FAIL, exactly 11 WRONG_FNV_ROUTING entries.
python3 tools/r5_batch_preflight.py --fixture tests/fixtures/r5-prone-push.json.xz
# Expected: exit 1, same 11 historical routing violations.
```

## Proposed input contract (not yet accepted implementation)

The checked-in example is exact historical evidence, not a template permitting invented values:

- `schemaVersion`: integer `1`.
- `baselineHead`: exact Git HEAD against which the batch is intended.
- `expectedPreviousCheckpoint`: complete previous checkpoint object, compared losslessly.
- `records`: ordered array; each item has `researchLog` and `cells`.
- `researchLog`: complete authoritative object with `canonicalExerciseId`, `exerciseName`, `identityStatus`, `sources` (title, publisher, tier, locator, finding), decision and `fieldApplication` (resolved field/value map and terminalUnresolved fields).
- `cells`: one item per resolved semantic cell, with separate complete `decision`, `evidence`, `provenance`, `patch` objects. Keep original evidence and provenance texts independently: historical summaries are not always identical.
- Decision/evidence/provenance carry canonicalExerciseId, dimension, previousState, newState, value, evidenceType, sourceTier, sourceTitle, sourcePublisher, sourceLocator, evidenceSummary, reasoningRule, confidence, provenanceId and researchVersion.
- Patch carries canonicalExerciseId, field, before, after, provenanceId and confidence.
- Do not accept a user-supplied shard route: compute FNV1a32 over UTF-16 code units of `canonicalExerciseId|field-or-dimension|provenanceId`, unsigned modulo 16.
- Record order supplies appended checkpoint IDs and last processed ID. Counters must be derived from validated payloads.

## Remaining work and recovery design

1. Resolve the concrete routing-gate incompatibility under a separately authorized, narrowly scoped persistence remediation. Do not silently normalize the authoritative baseline or re-open semantic research. Preserve these exact historical fixture blobs as evidence.
2. Finish and validate the write-capable helper and input contract. Existing source-index-only initial pilot (15 records / 56 decisions) and UTF-16-length historical manifest metadata must be handled explicitly, preserving old payloads rather than fabricating logs or silently normalizing text.
3. Complete all targeted applier tests (duplicate/conflict, stale state, evidence/provenance, routing, replay, partial input, protected state, arithmetic, references), plus interruption/recovery tests.
4. Execute the strict historical lossless regression and unchanged-authority completion gate. Commit/push only the validated result; verify branch ref remotely. No new R5 cells.

Planned write semantics: validate input and all required state, build outputs in memory, validate cross-family result, acquire an exclusive writer lock, persist a write-ahead journal with old/new bytes and hashes, replace affected shards and manifests, persist an idempotency receipt, write checkpoint LAST, re-read and verify, remove journal. Interrupted replay must accept only the identical batch and original HEAD, with each affected file exactly before or after; reject unexpected modifications. This is per-file atomic replacement with a checkpoint commit marker, **not an atomic filesystem-wide snapshot**. Readers must refuse an in-flight journal; callers must not commit partial files. POSIX locking and directory fsync need explicit platform support. None of these write/recovery guarantees is yet certified by this checkpoint.

Independent review confirmed the 11 routing mismatches using the original Guard V2 functions. Review fixes tested: full fixture hash-inventory coverage and machine-readable rejection of truncated XZ files.
