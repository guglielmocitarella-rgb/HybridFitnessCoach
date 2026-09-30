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
| Plan ownership | Goal, frequency/calendar days and priority skills are plan-owned, independent from AthleteProfile, and persisted by the current schema | VERIFIED at repository level |

Last fully green checkpoint before documentation normalization: `9488f2049e3e28a034c25ee2ee41cecec6f0fc54`, Exercise DB v2 #220 and Android APK #772.

## Remaining gates

- [ ] Audit and remove compatibility baggage that exists only for pre-v0.9.3 data/behavior.
- [ ] Reconcile every remaining PR #10 product requirement against current implementation and deterministic tests; change production only for demonstrated gaps.
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
