# Hybrid Fitness Coach — Project Status

VERSION: v0.9.1
BRANCH: feature/v0.9.1-training-plan
HEAD: 886dbd8bac35ecac74f275b0cf0c5e46001e6664
CI: pending for current HEAD
LAST VERIFIED: CI #295 GREEN; CI #296 unit tests PASSED. Later audit commits require final CI gate.

OPEN P0:
- NONE KNOWN.

OPEN P1:
- NONE KNOWN after remediation of paused timer compatibility, atomic linked history/plan deletion, and duplicate skill-performance counting.

OPEN P2:
- Final CI verification of backward-compatibility regression coverage.
- Device-level interaction/regression is not executable through repository CI and remains an explicit external verification boundary.

VERIFIED AUDIT AREAS:
- Training-plan 0/1/N boundary invariants and stable workout IDs.
- Calendar/runner/history reconciliation and linked-session deletion recovery.
- Paused timer restore for current and legacy persisted state.
- Custom target-set source semantics.
- Recovery values and active-rest persistence/reload.
- Progression/history duplicate counting protection.
- Legacy sessions, v1 training-plan migration, unsupported/corrupt persistence fail-closed behavior, and legacy active-workout defaults.
- Dead unused recovery-calibration path removed; runtime uses persisted Exercise recovery values.

TEST STATUS:
- Repository unit-test gate has passed on preceding audit checkpoints.
- Current HEAD requires final Android APK workflow: unit tests, signed release build, signature verification, artifact upload.

NEXT:
- Require current HEAD CI GREEN.
- If red, diagnose/fix/repeat.
- If green, v0.9.1 repository audit closes with no known P0/P1 and critical P2 repository paths verified; retain device-level interaction testing as documented external verification.
