# Canonical DB remote execution blocker

Date: 2026-10-03
Checkpoint before exception: `067275bec47fe17af355b4aca74e6f212e6ec3bc`

## Problem

The normal-chat GitHub connector can read the canonical candidate blob, but write operations carrying the complete regenerated `exercise-db-canonical/exercise-db-canonical.json` (~10 MB pretty-printed) fail at the transport layer with an HTTP/2 body error. Small GitHub writes succeed. A first enrichment write was immediately reverted; the net diff from the prior verified checkpoint `ffe67e94b37ab81eb987d61cb125caa924cfaf45` is empty.

## Files affected

The blocker applies to generated canonical artifacts, principally:
- `exercise-db-canonical/exercise-db-canonical.json`
- `exercise-db-canonical/exercise-db-canonical-audit.json`
- `exercise-db-canonical/VALIDATION_REPORT.json`
- gzip convenience artifacts

It does not require any runtime, UI, generator, scheduling, persistence, Health Connect, history, readiness, or navigation change.

## Why an exception is required

The required checkpoint order includes tests, builder, validator, regenerated artifacts, and a remote checkpoint. Without a repository-side runner, the chat connector can update enrichment inputs but cannot safely publish the regenerated large artifact. Leaving inputs and generated outputs mismatched is not acceptable.

## Alternatives considered

1. Leave generated artifacts stale: rejected because it violates the checkpoint contract.
2. Force-push or rewrite history: rejected.
3. Change or split the canonical schema only to bypass connector limits: rejected as unnecessary data-model churn.
4. Return to a transient local/Work workspace: rejected because GitHub is the required source of truth and persistence.
5. Add a narrowly scoped GitHub Actions workflow for the canonical DB: selected.

## Minimal exception

Add one workflow under `.github/workflows/` that is triggered only by canonical DB input/builder/validator/test changes. It runs the existing canonical unit tests, builder and validator, verifies `releaseReady` remains false while blockers exist, regenerates gzip convenience artifacts, and fast-forwards generated artifacts back to the same feature branch only after all checks pass.

## Risk and controls

Risk: an automated artifact commit could recurse or write unintended files.

Controls:
- path-scoped trigger;
- generated-only commit step;
- no force push;
- no merge and no write to `main`;
- explicit branch guard for `feature/v0.9.5`;
- artifact commit message contains `[skip ci]`;
- workflow stages only canonical generated artifacts;
- existing runtime sources are never staged.
