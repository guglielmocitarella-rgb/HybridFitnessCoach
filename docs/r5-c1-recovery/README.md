# R5 C1 diagnostic recovery

This tool never writes the candidate, authoritative inputs or Android runtime. It has no apply,
commit, push or export-candidate mode. Only JSON and Markdown reports are written outside the
data checkout. This package adds four files; it relies on the existing `tools/r5_apply_batch.py`
only for pure hashing, semantic key and UTF-16 routing helpers.

## Local execution

Use Linux and Python 3.12. Put the four files at their repository-relative locations in a
development checkout. Keep a separate, clean data checkout at exactly
`f2698fc2ec51472a34d12502a3b74d89322ec186`. The tool does not download any inputs.

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests -p test_r5_candidate_dry_run.py -v
PYTHONDONTWRITEBYTECODE=1 python3 tools/r5_candidate_dry_run.py \
  --root /absolute/path/to/clean-baseline-checkout \
  --report-dir /absolute/path/to/new-report-directory
```

The report parent must exist; the report directory must not exist and must be outside the
data checkout. Symlink inputs and output paths are rejected. Never copy these four files
into the data checkout: it must remain clean at the baseline. Local modifications to tools
are allowed only in the separate tools checkout.

## Integrity and semantics

Forty expected Git blob identities are pinned in the script, including the 19,104,670-byte R4
candidate (`d2b3361183dd5694167c7ca8323268e1d6d13d38`). Git HEAD and baseline-tree blobs are
checked. Git commands use `--no-optional-locks`, preventing status checks from refreshing
the Git index on disk. Raw bytes are hashed with the Git blob header before JSON parsing. Duplicate JSON
members, missing files, malformed schemas, invalid routing, duplicate cells/provenance,
manifest SHA/count/byte-size mismatches and set inconsistencies stop execution.

The 16 patch shards contain 3,992 rows; the 16 field-decisions shards contain 4,001.
Both families are read, because all 214 outside-universe decisions must be isolated even
though nine have no patch. The canonical partition is 3,787 resolved plus 48 terminals
equals 3,835, with TRUE_MISSING zero. Out-of-universe entries are 148 orphan cells, 55
PD/scalar conflicts, nine R3/R5 conflicts and two R3-identical cells. All remain unchanged.

Only an in-universe, nonterminal cell currently equal to `EVIDENCE_UNRESOLVED`, with a patch
whose `before` is exactly that same value, can change to its authoritative scalar `after`.
Already-aligned values are no-ops. Other precondition mismatches are reported without
application. PD states are never scalarized; the 441 original strength PD cells are preserved.
Terminal membership, IDs, order and every unrelated field are preserved. Aggregate
`capability.state` is recalculated only for changed records, using the R3 rule. A full semantic
diff proves that no other mutation occurred. No consolidated candidate is serialized.

Historical `UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE` is accepted as a persisted patch `before`,
but is never silently equated to canonical `EVIDENCE_UNRESOLVED`. The baseline contains 761
such patch rows: target mismatches will remain diagnostic conflicts, requiring a separately
justified policy before any future consolidation. Decisions support the two observed pinned
schema shapes, with optional `evidenceSummary`, and source tiers 1 through 4. Missing evidence
summaries are preserved without invention.

Historical shard order is checked and recorded when it differs from Python tuple order.
The manifest names ordering fields but does not specify one comparator across historical
producers. The diagnostic sorts only its in-memory traversal; pinned input bytes and their
order are never rewritten. Routing, duplicate, hash and count checks remain strict.

## Verified input blocker

Reading the baseline support artifacts exposed an existing manifest inconsistency:
`field-decisions/bucket-04.json` has 507 entries and the expected blob
`464ed6a392f8c9bbcae79d346cac342c10ad4e83`, but its actual UTF-8 byte length is **485,431**
while the manifest declares **485,381**. The dry-run deliberately fails on this mismatch.
This package does not repair the manifest, alter pinned hashes or bypass validation.
An authorized input repair would require a new baseline and updated input pins in a later task.

## Reports and limits

Files: `r5-c1-dry-run-report.json` and `r5-c1-dry-run-report.md`. Successful diagnostics
include input identities, distributions, safe changes, aggregate changes, already-aligned
cells, complete isolated decisions, all 64 precedence conflicts, additional conflicts and
invariants. Stable ordering and no timestamps make results reproducible.

Exit codes:

- `0`: complete diagnostic with unresolved precedence; not promotion approval.
- `2`: fail closed on unverifiable inputs, schema or invariants.
- `3`: operational limit exceeded.

Controlled errors produce failure reports where the output destination is valid. Unsafe output
destinations produce stderr only. A killed process or exhausted memory may prevent report
generation; absence of reports causes the workflow's artifact step to fail.

Limits: 300-second process alarm, 2 GiB address-space limit, 32 MiB per input, 128 MiB total
inputs, 32 MiB total report bytes, 30 seconds per Git command. GitHub job timeout is 15 minutes.

## Manual workflow

Only `workflow_dispatch` exists. Permission is `contents: read`; all other token permissions
are absent. Checkout actions persist no credentials. There are no secrets, installs,
Android builds, commit/push commands or calls to another workflow. Actions are pinned to full
commit SHAs. Tests and diagnostics preserve nonzero exit codes. Report upload runs even after
a controlled failure, without hiding that failure. Artifacts expire after 14 days.

The tools checkout uses the dispatch revision; the data checkout uses the immutable baseline.
The data checkout is made nonwritable. Reports go to runner temporary storage. Concurrency
allows one run per workflow/ref and does not cancel an existing run.

GitHub requires a dispatch workflow to exist on the default branch before manual triggering:
https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow
Publication and manual triggering are separate actions, not performed by this package.

## Validation performed for this implementation

All 23 focused tests passed locally, including safe transitions, protected states, precondition
conflicts, isolation of 214 decisions and 64 precedence conflicts, deterministic replay,
strict JSON, hashes, Unicode routing, unsafe paths, baseline guards, manifest-size failure,
operational limits, failure-report CLI behavior and workflow safety.
Independent code review identified Git status optional-lock writes; the fix and regression
test passed. Validation status: **IMPLEMENTED_NOT_BASELINE_EXECUTED**.
All 39 support files were retrieved and their raw-byte blob identities matched the baseline
tree. All 3,992 patch entries passed schema, manifest, routing and duplicate checks. Reading
field-decisions correctly stopped on the byte-size mismatch described above. The real R4
candidate was not downloaded and no complete candidate simulation is claimed.
