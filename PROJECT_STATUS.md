# Hybrid Fitness Coach — Project Status

**Version:** v0.9.5 (versionCode 95)  
**Branch:** `feature/v0.9.5` — PR #14, draft  
**Last green device-test baseline:** `551c28535c01a28371fd2df2de975dc8265ecc0e` — Android APK #872 SUCCESS  
**Updated:** 2026-10-02 — owner device verification completed with remediation findings

## Current state

The repository candidate reached a green automated baseline (403 JVM tests, 9/9 Python DB tests, assembleDebug success, lint 0 errors/2 warnings; canonical DB 587 movements / 113 progression edges / 0 validation errors). Physical verification on 2026-10-02 is now complete enough to replace DEVICE_PENDING with **COMPLETED_WITH_FINDINGS**.

The release is **not accepted**. The most important blocker is systemic Calisthenics Skill Tree generation: the owner observed “no compatible exercise” for every tested skill/location path. Additional onboarding, location, Runner, history/progress and catalog-quality findings are recorded in `docs/V0_9_5_DEVICE_ACCEPTANCE.md`.

## Gate summary

| Area | State |
|---|---|
| P00 preserved multi-plan foundation | VERIFIED / regression protect |
| P01–P11 original implementation | IMPLEMENTED; selected areas REOPENED by device findings |
| Automated baseline #872 @ 551c285… | PASS (historical baseline only) |
| Owner physical verification | COMPLETED_WITH_FINDINGS |
| Calisthenics Skill Tree | CRITICAL / RELEASE BLOCKER |
| Exercise catalog semantic completeness | AUDIT REQUIRED |
| P12 device acceptance | REMEDIATION_REQUIRED |
| P13 release/freeze | BLOCKED |
| Release signing | BLOCKED pending authorized credentials |

## Accepted behavior to preserve

Cascading reschedule and restore-original-date behavior; Extra generation and alternatives; common exercise detail/technique access; fixed Runner progress header; next-set transition/recovery flow; persisted resistance values; ability to finish onboarding without creating a plan. These must receive regression coverage while remediation is performed.

## Remediation priorities

1. **R0 — Skill Tree unblock:** reproduce systemic failure, audit skill/exercise/equipment eligibility, fix bodyweight-vs-equipment location filtering, and add trainable-skill coverage tests.
2. **R1 — Catalog integrity:** audit all exercises for technique descriptions, progression/related links, equipment metadata and tappable related navigation.
3. **R2 — Runner correctness/UX:** responsive exercise panel/scroll, required load when applicable, unlimited voluntary sets for single-exercise mode.
4. **R3 — Onboarding/profile:** persisted default location, Health Connect de-duplication/enrichment verification, concrete independent capability assessment, first-plan input transparency.
5. **R4 — Plan/Profile UX:** derive weekly count from selected days, calendar long-press edit, remove redundant location navigation.
6. **R5 — History/Progress:** consolidate summary/detail, resolve unexplained metric dash, add per-exercise totals and chronological progression graph.
7. **R6 — Validation:** full JVM/Python/DB/lint/build exact-SHA CI, then focused device acceptance of every reopened item, then release signing/freeze gate.

## Evidence discipline

The #872 PASS proves only the old `551c285...` candidate. It does not validate any subsequent remediation. No v0.9.5 freeze is declared until the critical Skill Tree path and all release-blocking acceptance items pass on a new exact-SHA candidate.
