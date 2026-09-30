# Hybrid Fitness Coach — Project Status

VERSION: v0.9.3
BRANCH: feature/v0.9.3
SOURCE OF TRUTH: GitHub PR #10 and the exact branch HEAD.
PREVIOUS RELEASES: v0.9.0, v0.9.1 and v0.9.2 are closed/historical. The frozen v0.9.2 reference is `release/v0.9.2`. They are not active release gates for v0.9.3.
COMPATIBILITY: backward compatibility is permanently out of scope from v0.9.3 onward unless the owner explicitly reverses the policy.

LAST VERIFIED CHECKPOINT:
- HEAD `9488f2049e3e28a034c25ee2ee41cecec6f0fc54`
- Exercise DB v2 #220: GREEN
- Android APK #772: GREEN
- Plan configuration is owned by TrainingPlan rather than AthleteProfile and persisted by the current schema.

CURRENT RELEASE STATE:
- v0.9.3 is the only active release line.
- PR #10 remains the live development/acceptance surface.
- Repository CI is not a substitute for device acceptance.

VERIFIED RECENT AUDIT AREAS:
- Skill workout candidate selection, locked-node exclusion, focus retention on regeneration and mastery prescription.
- Source/effective recovery propagation through set and exercise-transition recovery.
- Exercise DB v2 equipment taxonomy → selectable equipment → generator compatibility, catalog-wide.
- Exercise progression graph direction → Exercise Detail easier/harder semantics, catalog-wide.
- Default runner RPE 5 → structured session history → Progression Engine/mastery.
- Training-plan configuration decoupled from athlete profile and stored as plan-owned metadata.

OPEN WORK, IN ORDER:
1. Compatibility-baggage audit: identify and remove code/tests retained solely for pre-v0.9.3 migrations or legacy behavior. Preserve current-schema recovery, corruption handling and fail-closed validation.
2. Maintain `V0_9_3_DOD.md` as the sole active release DoD and reconcile remaining PR #10 requirements against current code/tests.
3. Complete focused audits for any v0.9.3 requirement not yet demonstrated end-to-end.
4. Run exact-SHA CI for the final candidate.
5. Device acceptance on the final APK; record device-only findings separately from repository verification.

STATUS TERMINOLOGY:
Use VERIFIED, IMPLEMENTED_NOT_DEVICE_VERIFIED, PARTIAL, or BLOCKED. Do not reopen old RC2 gates as current requirements.
