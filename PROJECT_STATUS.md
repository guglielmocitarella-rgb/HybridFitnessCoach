# Hybrid Fitness Coach — Project Status

VERSION: v0.9.1
BRANCH: feature/v0.9.1-training-plan
HEAD: 71b8502093b9207603ef0d33aaef8f7be0b63854
LAST VERIFIED CHECKPOINT: 8be8725 (A2 persistence recovery; CI 36465032499 green)

COMPLETED:
- A2 fail-closed persistence recovery.
- Removed heuristic plan completion: planned sessions now require persisted plannedWorkoutId.
- Corrected active workout elapsed-time persistence so paused wall-clock time is excluded.
- Aligned custom-runner start/completion totals with source-based target set counts.

OPEN:
- Continue P1/P2 audit of training-plan edge cases, runner/calendar wiring, recovery rules and upgrade paths.
- Complete source/video coverage gates already tracked in V0_9_1_DOD.md.
- Device-level interaction/regression remains outside repository-only verification.

KNOWN ISSUES:
- Deleting linked history and restoring plan state is not a transactional multi-key write.
- Source-based recovery gap is not yet available; callers intentionally use zero where no verified rule exists.

TEST STATUS:
- CI 36465032499 passed at 8be8725.
- Current HEAD CI 36466423331 is in progress; current changes are NOT VERIFIED until it completes.

NEXT:
- Read current CI result.
- If green, continue P1/P2 audit and remediation; if red, diagnose and fix before proceeding.
