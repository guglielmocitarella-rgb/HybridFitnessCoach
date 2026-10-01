# Hybrid Fitness Coach — Project Status

VERSION: v0.9.4
BRANCH: feature/v0.9.4
SOURCE OF TRUTH: GitHub branch `feature/v0.9.4`, `V0_9_4_DOD.md`, and the exact branch HEAD.
PREVIOUS RELEASES: v0.9.0, v0.9.1 and v0.9.2 are historical; v0.9.3 is frozen on `release/v0.9.3`. The former `feature/v0.9.1-training-plan` ref is historical naming only and is not the active release line.
COMPATIBILITY: backward compatibility remains out of scope unless the owner explicitly reverses the policy.

LAST VERIFIED CHECKPOINT:
- HEAD `0735f62152221299cbdc894eb023456f685c05fa`
- Exercise DB v2 #273: GREEN
- Android APK #826: GREEN
- F03/F04 equipment/location cleanup is verified at repository level: legacy `EquipmentProfile` and duplicate Machine/Machines contracts were removed.
- F06 propagation cleanup is verified at repository level: coarse `AthleteProfile.level` was removed from model/persistence/decision paths; generation and prescription use assessed capability dimensions with conservative unassessed behavior.
- F02 propagation cleanup is verified at repository level: effective/source-backed recovery is propagated through set and exercise-transition recovery and persisted runner snapshots.

CURRENT RELEASE STATE:
- v0.9.4 is the only active development line.
- `feature/v0.9.4` is the primary development branch.
- v0.9.3 remains frozen; do not backport v0.9.4 product work.
- Repository CI is not a substitute for physical-device acceptance.

OPEN WORK, IN ORDER:
1. Continue the remaining `V0_9_4_DOD.md` findings from the verified checkpoint; next planned UI block is F10 Home plan presentation/iconography followed by F11 preview reset, unless a dependency audit changes the order.
2. Preserve F02/F06 regression coverage while evolving plan/location behavior.
3. Run exact-SHA CI for subsequent candidates.
4. Perform targeted physical-device acceptance for all `DEVICE_PENDING` findings before release closure.

STATUS TERMINOLOGY:
Use VERIFIED, IMPLEMENTED_NOT_DEVICE_VERIFIED, PARTIAL, or BLOCKED. Do not reopen old RC2 gates as current requirements.
