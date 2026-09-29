# Hybrid Fitness Coach — Project Status

VERSION: v0.9.1
BRANCH: feature/v0.9.1-training-plan
HEAD: see PR #8 / branch HEAD (this file is a release-status summary, not the authoritative live SHA)\nCI: latest fully verified checkpoint before the current audit: PR Android APK #700 + Exercise DB v2 #162 GREEN at 8c2a5a55d83c8361482f58e5c4a106d539125894\nLAST VERIFIED: 8c2a5a55d83c8361482f58e5c4a106d539125894 — unit tests, lint, PR debug APK and Exercise DB v2 validation PASSED. Later commits require their own exact-SHA CI before RC2 acceptance.

OPEN P0:
- NONE KNOWN.

OPEN P1:
- NONE KNOWN after remediation of paused timer compatibility, atomic linked history/plan deletion, and duplicate skill-performance counting.

OPEN P2:
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
- Current verified PR checkpoint #700: unit tests, Android lint and debug APK PASSED; Exercise DB v2 #162 PASSED. Signed release APK remains a final push-workflow gate for the eventual final code SHA.

NEXT:
- Repository audit remains open until the current RC2 HEAD has exact-SHA green CI and device acceptance is completed.
- Retain device-level interaction/regression as the documented external verification boundary before distribution/release acceptance.
