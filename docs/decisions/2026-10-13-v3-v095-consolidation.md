# ADR 2026-10-13 — V3 + v0.9.5 consolidation, source of truth, and safe branch retirement

**Decision date:** 2026-10-13  
**Status:** ADOPTED FOR WORKSTREAM ORGANIZATION; NO PRODUCTION DB SWITCH APPROVED  
**Repository:** `guglielmocitarella-rgb/HybridFitnessCoach`  
**Active consolidation branch:** `feature/v3-integration-v095-stabilization`  
**Exact branch creation base:** `f2698fc2ec51472a34d12502a3b74d89322ec186`, the verified head of `feature/exercise-db-v3` / draft PR #15.

## Why we made this decision

The repository has many historical feature, checkpoint, backup and R5 recovery branches. A v0.9.5 physical-device acceptance on 2026-10-02 had useful verified behavior, but did **not** accept the release (notably, systemic Calisthenics Skill Tree generation failure). Later DB V3 evidence work was separated and completed through R5 V29; V3 integration and athlete assessment/ranking proceeded in isolation. The product version and the DB version were therefore easy to confuse.

Tests in V3.9R-B showed positive simulation coverage while using an explicit `legacyLevel` compatibility fallback. Native four-dimensional requirements had not been materialized in that candidate. R5 evidence remediation and Step 6 later produced a distinct, frozen final candidate. On 2026-10-08, Android packaging was nevertheless wired to the *older* V3.9R-B JSON; successful APK/loader/schema/count checks do **not** establish that the final R5 candidate was packaged. As of the verified 2026-10-09 head, production workouts still use V2, and V3 runs in a separate/shadow path.

One active consolidation branch, precise artifact identities, explicit test gates and an audit-based branch inventory are preferable to further parallel release attempts.

## Precisely which V3 branch and JSON are authoritative

1. **Authoritative app/DB workstream for this consolidation:** `feature/exercise-db-v3` (PR #15), at `f2698fc2ec51472a34d12502a3b74d89322ec186`. The new branch was created **from this exact SHA**, not from `main`, the V29 backup, V3.9R-B, or an arbitrary R5 staging branch.
2. **Final validated source candidate, NOT YET production-integrated:** `exercise-db-canonical/exercise-db-v3-final-candidate.json`. Step 6 `final-canonical-candidate-step6-validation.json` declares SHA-256 `0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a`, 19,054,419 bytes, 1,087 unique canonical exercise IDs, 3,787 resolved R5 states, 48 terminal states, zero true-missing R5 keys, 441 preserved prescription-dependent strength states, no production switch.
3. **Historical V29 evidence-completion checkpoint:** `backup/r5-v29-complete-2026-10-07` at `a280a6b23095525782b259cbd2b28d371f570138`. This is an ancestor of the official V3 branch (121 later commits); keep it for recovery, **not** as the integration head.
4. **Older V3.9R-B candidate, currently still selected by `app/build.gradle.kts` for the shadow APK asset:** `exercise-db-canonical/exercise-db-v3-9r-b-candidate.json`, 19,050,937 bytes. It has the same 1,087 IDs and schema 3.7 but must **not** be mistaken for the final R5 evidence-rich database. Existing `V3PackagedAssetTest` checks the packaged asset but does not pin the Step 6 final SHA.
5. **Later side branch to preserve, not silently merge:** `work/v3-r5-candidate-consolidation` at `861cb4b4f84e1af8f232682f0bc73bd680bb847c`, dated 2026-10-10, has **five commits beyond** `f2698fc2`. Its diff adds read-only R5 C1 recovery diagnostics/tests/workflow and fixes the `field-decisions` manifest bucket-04 byte-size metadata. Both branches contain the exact same frozen final-candidate Git blob. The C1 documentation states `IMPLEMENTED_NOT_BASELINE_EXECUTED` and a historic manifest-size blocker; keep these five commits for later evaluation, not as evidence of a newer production DB.

## Immutable references and branch policy

- `main` remains untouched. Draft PR #14 (`feature/v0.9.5`) and draft PR #15 (`feature/exercise-db-v3`) remain open, unchanged as baseline/reference PRs. No automatic rebase, force-push, merge, deletion or release.
- The `feature/v0.9.5` tip diverges from `feature/exercise-db-v3` by four non-shared commits; inspect those commits before declaring parity or transferring a fix. Its physical device audit remains authoritative for observed product behavior.
- Keep R5 V29 backup, V3 final candidate SHA, v0.9.4 green checkpoint, relevant release checkpoints, and the 2026-10-10 diagnostic descendant retrievable.
- **Logical archive**: an ancestor-only branch may be marked inactive/eligible to retire in `docs/branch-archive-inventory-2026-10-13.md`. This is not Git branch deletion. A physically retired branch requires a separately verified durable ref/tag or equivalent recovery policy and a capability that safely deletes the branch. Diverged, descendant, PR-head, backup and not-yet-audited branches are never deleted as part of this decision.
- All reference tips and intended archive outcomes are recorded in the inventory. This avoids claiming the repository is physically clean before it is.

## Work phases and acceptance gates

**A — Inventory & provenance (this workstream begins here).** Document exact heads and ancestry, protect outstanding unique commits, verify final-vs-packaged asset identity and preserve the v0.9.5 owner audit. No speculative merge of PR #14 or the side branch.

**B — Shadow V3 asset correctness.** Make the *isolated* packaged V3 asset derive from the frozen Step 6 final candidate only, with SHA-256, byte count, 1,087 unique IDs, 441 PD and 48 terminal invariants. Ensure the APK test reads the **actual packaged bytes** and asserts final-candidate identity (not a separate test-only fixture). Fail closed on mismatch. Keep V2 production workouts intact while this is proven.

**C — Finish V3 engine safely.** Validate native G1 athlete capability assessment, eligibility, contextual PD resolution, explainable ranking, variety/composer, skill relations, exercise substitution, plan/custom/extra/single/library origins, prescription materialization and explicit zero-pool handling. Do not use `legacyLevel` to silently supply native capability requirements.

**D — v0.9.5 device-audit remediation.** Retest and resolve the 2026-10-02 issues against a pinned APK, starting with the systemic Skill Tree failure, location/equipment filtering and persistence, Runner scroll/load/unlimited sets, then onboarding/Health Connect, calendar, history/progress, exercise descriptions and progression UX. Preserve known-good behaviors (rescheduling, alternatives, actual loads, recovery, technique).

**E — V3-native persistence & controlled cutover.** New V3 workouts, plans, session actuals, skill state and history must round-trip across process death; deliberate reset of disposable old test data is allowed per Step 9 roadmap. Only after all launch origins and device/test gates pass, authorize the V3 production switch (Step 10H); verify V2 removal later (Step 10I), not prematurely.

**F — Exact-SHA acceptance.** JVM/DB/Python invariants, lint, Android APK build, artifact SHA-256, packaged DB SHA-256, UI/process recreation/physical-device regression and review must pass before merging any new consolidation PR into `main`. Release signing is a distinct authorization gate.

## Current status, not a claim of completion

This ADR records the **choice and rationale only**. Opening the branch and writing documentation does **not** solve the packaging mismatch, C1 manifest issue, skill bug, incomplete V3 workflow or physical-device acceptance. No old branches are removed, no PR is merged, and no native production switch is performed by this document.

## Evidence

- `docs/V0_9_5_DEVICE_ACCEPTANCE.md` — user-observed physical tests, 2026-10-02.
- `exercise-db-canonical/final-canonical-candidate-step6-validation.json` — exact frozen candidate.
- `exercise-db-canonical/step7a-runtime-integration-audit.md`, `exercise-db-canonical/step9-v3-integration-roadmap.md`.
- `docs/step10b2-isolated-ranking.md` — the actual packaged old candidate and isolated final positive fixture.
- `app/build.gradle.kts`, `app/src/test/java/com/hybridfitnesscoach/app/domain/v3/V3PackagedAssetTest.kt`.
- `docs/r5-c1-recovery/README.md` **on the side branch**, not imported into this baseline.
- GitHub PR #14 and PR #15.
