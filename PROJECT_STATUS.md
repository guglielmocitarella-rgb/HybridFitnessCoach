# Hybrid Fitness Coach — Project Status

VERSION: v0.9.1
BRANCH: feature/v0.9.1-training-plan
HEAD: 5cdb7b89517a57f1fd42506ca548a59af0c6dfc5
CI: pending for current HEAD
LAST VERIFIED: 02c30ae1606cde18228a8e4bd4e0bc0e49d79ad0 — unit tests passed in CI #296 before release build stage

OPEN P0:
- NONE KNOWN. Linked history deletion + plan restoration now use one SharedPreferences commit and have regression coverage.

OPEN P1:
- Progression/history duplicate-count audit in progress; regression test added at current HEAD.

OPEN P2:
- Backward-compatibility audit of persisted training-plan snapshots and legacy runner/history formats.
- Device-level interaction/regression remains outside repository-only verification.

TEST STATUS:
- CI #295 GREEN at 5a8e8550f9f4946c64c409fa394d5be82debd154.
- CI #296: Run unit tests PASSED at 02c30ae1606cde18228a8e4bd4e0bc0e49d79ad0; full workflow result pending/being superseded during audit.
- Current HEAD 5cdb7b89517a57f1fd42506ca548a59af0c6dfc5: CI pending.

NEXT:
- Gate current progression/history regression test through CI.
- Fix only if the test proves duplicate counting.
- Continue backward compatibility and remaining P1/P2 audit.
