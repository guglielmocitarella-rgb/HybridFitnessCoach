# Hybrid Fitness Coach — v0.9.3 Definition of Done

This is the **only active release DoD**. GitHub PR #10 and its exact HEAD are the live source of truth. Older v0.9/v0.9.1/v0.9.2 documents are historical evidence only.

## Release policy

- v0.9.0, v0.9.1 and v0.9.2 are closed/historical; `release/v0.9.2` is the frozen predecessor reference.
- Backward compatibility with earlier builds, persistence schemas, data models, APIs or behavior is **not required now or in future releases**, unless the owner explicitly changes this policy.
- Do not retain migrations, legacy fallbacks, compatibility adapters, nullable states or tests solely for older versions.
- Current-schema data integrity, recovery, corruption detection and fail-closed validation remain requirements.
- A green build is necessary but not sufficient: repository verification and device acceptance are separate gates.

## Verified v0.9.3 contracts

| Area | Repository evidence | Status |
|---|---|---|
| Skill workout mastery | Current progression node is selected; locked nodes excluded; regeneration retains focus; mastery set/rep prescription survives adaptive regeneration | VERIFIED |
| Recovery prescription | Source-based effective recovery reaches set and exercise-transition recovery; interval recovery retains precedence | VERIFIED |
| Equipment contract | Canonical equipment tokens are selectable and round-trip through generator compatibility across the catalog | VERIFIED |
| Exercise progression detail | Every MAIN_PROGRESSION edge resolves with from= easier and to= harder in Exercise Detail semantics | VERIFIED |
| RPE default | Untouched runner RPE 5 is materialized into structured history and can qualify Progression Engine mastery | VERIFIED |
| Plan ownership | Goal, frequency/calendar days and priority skills are plan-owned, independent from AthleteProfile, and persisted by the current schema; athlete persistence now owns only stable athlete fields | VERIFIED at repository level |
| Compatibility cleanup | Current active-workout schema v2 and training-plan schema v3 require explicit version markers; compatibility-only implicit v1 acceptance is removed | VERIFIED |
| Calendar reschedule | Reschedule is cascading; original date is retained and can be explicitly restored; Italian calendar date formatting is used | VERIFIED at repository level; device interaction pending |
| Fitness-level assessment | Athlete level can be assessed from current capability inputs; deterministic boundary tests verify Principiante/Intermedio/Avanzato mapping and the same `levelScore` ceiling consumed by automatic/custom workout filtering | VERIFIED at repository level; device interaction pending |
| Exercise library/equipment | Measurement/type filter is absent; equipment options are catalog-driven, deduplicated and alphabetically sorted | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| Exercise detail action/progress | Detail exposes direct Execute action, executed repetition totals, loaded-volume statistics, and explicit easier/harder progression relations | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| Completed workout detail | Structured executed-set detail, including actual reps/load/rest, is rendered from session history | IMPLEMENTED_NOT_DEVICE_VERIFIED |
| Generated workout preview | Summary, Adaptive Coach and regenerate controls are outside the exercise `LazyColumn`; only the exercise list scrolls; pinned regeneration behavior remains regression-covered | IMPLEMENTED_NOT_DEVICE_VERIFIED |

Current fully green checkpoint: `6add2030ae5dbe5eb765bad0bcfa4ba5922e50d8`, Exercise DB v2 #249 and Android APK #801. The PR #10 product-scope reconciliation is complete at repository level: every declared product requirement is either repository-verified or explicitly retained as `IMPLEMENTED_NOT_DEVICE_VERIFIED`; no interaction-only behavior is promoted to verified without device evidence. Direct Execute materializes the automatic effective prescription; deterministic recovery tests cover process recreation and runner position/performance; Home suggestion tests bind today's runnable plan session to the suggested workout; fitness-assessment tests cover boundaries and the production difficulty ceiling.

## Remaining gates

- [x] Audit and remove compatibility baggage that exists only for pre-v0.9.3 data/behavior. Current active-workout schema v2 and training-plan schema v3 require explicit `schemaVersion`; current-schema recovery/corruption protection remains intact.
- [x] Reconcile every PR #10 product requirement against current implementation and deterministic tests; production changes were limited to demonstrated gaps. Remaining unverified items are interaction/device acceptance only.
- [ ] Exact-SHA final Exercise DB validation and Android unit/lint/build CI.
- [ ] Device acceptance: calendar interactions, workout generation/preview, runner/timers/background restore, history/detail, plan configuration, equipment UX, skill navigation and other interaction-only behavior.
- [ ] Final release artifact verification after the accepted code SHA.

## Engineering acceptance rules

For each remaining requirement: inspect current HEAD → define the contract → inspect all direct consumers/tests → add or strengthen a focused test when evidence is missing → apply the minimum production fix only if the test demonstrates a real gap → run relevant regression → run Actions only after predictable local/static impacts have been reconciled.

Do not use CI to discover mechanically predictable consequences such as stale schema-version assertions after intentionally changing a schema contract.

## Compatibility-baggage audit boundary

Candidates include old schema readers/migrations, legacy active-runner/session defaults, old catalog/plan alias compatibility and tests whose sole purpose is accepting pre-v0.9.3 representations. Removal is appropriate when they have no current-schema role. Do **not** remove current-schema transactional recovery, rollback, corruption checks, unsupported/future-schema fail-closed handling where it protects the current app, or immutable historical snapshots created by the current build.

## Device acceptance boundary

Repository tests cannot prove Compose interaction, Android lifecycle/background timing, visual hierarchy, scrolling, dialog behavior or real-device Health Connect behavior. Those items remain IMPLEMENTED_NOT_DEVICE_VERIFIED until tested on the final APK.
