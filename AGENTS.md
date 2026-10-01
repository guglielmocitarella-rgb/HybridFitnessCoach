# Repository Guidelines

## Project Structure & Module Organization

Single-module Android app using Kotlin, Jetpack Compose, and Material 3.

- `app/src/main/java/com/hybridfitnesscoach/app/`: UI (`MainActivity.kt`), workout infrastructure, coaching/readiness engines, catalogs, planning, and Health Connect.
- `app/src/main/res/`: styles, launcher artwork, and Android resources.
- `app/src/test/java/com/hybridfitnesscoach/app/`: JVM regression and integration tests.
- `.github/workflows/`: Android tests, builds, and signing.
- Root `V0*` documents: release goals, audits, and definitions of done; update relevant evidence when release scope changes.

## Build, Test, and Development Commands

Use JDK 17, Gradle 9.6.0 (matching CI), and Android SDK 37. No Gradle wrapper is checked in; use installed `gradle`.

- `gradle :app:assembleDebug`: build APK in `app/build/outputs/apk/debug/`.
- `gradle :app:installDebug`: install on a connected device/emulator, then launch there.
- `gradle :app:testDebugUnitTest`: run JVM tests.
- `gradle :app:lintDebug`: run Android Lint.
- `gradle :app:assembleRelease`: build with release signing configured.

Signing uses `HFC_KEYSTORE_PATH`, `HFC_KEYSTORE_PASSWORD`, `HFC_KEY_ALIAS`, and `HFC_KEY_PASSWORD` environment variables.

## Coding, Tests, and Contributions

Use four-space indentation, PascalCase types/Kotlin filenames, camelCase functions/properties, and nearby Compose conventions. Avoid unrelated reformatting. No dedicated ktlint/Detekt configuration is present. Keep domain calculations deterministic and separate from Android UI/storage where practical.

Tests use JUnit 4.13.2, `*Test` files/classes, and descriptive camelCase `@Test` methods. No numeric coverage threshold is configured. Cover changed behavior; run the full JVM suite before submitting and manually verify affected Compose flows on a device/emulator.

Use small, descriptive commits with imperative subjects, following history: `Migrate intact legacy plans to exercise snapshots on load`. PRs should describe behavior changes, link relevant issues/release gates, record validation, and include UI screenshots when applicable. Ensure CI tests and the debug build pass.

# Hybrid Fitness Coach — Permanent Agent Instructions

## Current Release Target

Target: **v0.9.5**. Primary development branch: `feature/v0.9.5`; active PR: **#13**. **v0.9.4 is frozen** at runtime `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07` (`checkpoint/v0.9.4-f01-f11-green`); do not add product changes to older release lines.

Deliver a robust, tested, maintainable, scientifically honest release. A compiling APK alone is insufficient.

## Compatibility Policy — Permanent

Backward compatibility with earlier Hybrid Fitness Coach builds, persistence schemas, data models, APIs, or behaviors is **not a requirement for v0.9.5 and will not be a requirement for future releases** unless the owner explicitly reverses this policy. Do not retain legacy fields, fallbacks, migration code, compatibility adapters, nullable states, or tests solely to preserve older versions. Prefer the cleanest correct current-domain model and remove obsolete compatibility paths when they add complexity or ambiguity. Existing development/test data may be reset when a schema change requires it. This policy does not permit accidental loss or corruption of data created by the current supported build/schema.

## Owner and Autonomy

The owner is not a programmer. Make normal engineering decisions autonomously; do not ask the owner to choose Kotlin implementations, architectures, algorithms, libraries, persistence strategies, database structures, Gradle configurations, CI implementations, testing strategies, refactoring approaches, or equivalent technical solutions.

When alternatives are reasonable: inspect existing architecture → compare alternatives → choose the safest, most maintainable solution → implement → test → briefly document the decision.

Ask only when a choice materially changes product behavior, user experience, privacy, cost, or scope, or when explicit approval is required for destructive Git operations. For ordinary technical failures: diagnose → fix → test → continue. A failed command/test is not itself a reason to stop.

## Engineering Priorities

Prefer, in order: correctness → current-schema data integrity → testability → maintainability → traceability → scientific honesty → simplicity. Backward compatibility is explicitly out of scope. Avoid speculative or cosmetic large refactors.

## Current Release Areas

Verify these areas; do not assume they are broken. Inspect current HEAD before changing any area:

- Robust/versioned persistence for the current schema; legacy migration compatibility is not required.
- Real performance/fatigue integration, Prescription Engine, recovery-aware scheduler, 1–52-week programming, and structured rest prescriptions.
- Raw exercise catalog validation, exercise evidence provenance, skill progression provenance, and exercise video infrastructure with verified coverage.
- Exercise Detail, calendar, Health Connect/readiness, and architecture/scalability.
- GitHub Actions, Android lint, and end-to-end verification.

## Regression Protection

Preserve working behavior unless a verified requirement requires change: equipment filtering; workout sequencing; exercise locking; Foam Roller and Ab Wheel support; workout runner; actual completed-set persistence and statistics derived from those sets; Skill Tree; original/current/actual session dates; completed-session protection; offline operation; optional Health Connect; release signing.

## Scientific Evidence

Explicitly distinguish `SOURCE_BASED`, `APP_RULE`, `FALLBACK`, and `UNVERIFIED`.

Never fabricate citations, URLs, authors, page numbers, scientific conclusions, mastery thresholds, or physiological claims. A general training-category source does not validate every exercise or numeric prescription in that category. When evidence cannot be verified, preserve functionality with an explicitly identified fallback where appropriate instead of inventing evidence.

## Persistence and Migration

Treat data written by the current supported schema as valuable. Never silently discard or corrupt valid current-schema athlete profiles, training history, plans, completed sets, equipment configuration, or progression information. Schema/persistence changes do **not** require migration from older app versions: prefer a clean schema and explicit reset over compatibility machinery. Tests should validate the current schema and current-build recovery/integrity. Handle corruption explicitly; do not convert a valid current-schema store to an empty dataset.

## Health Connect

Keep Health Connect optional. The app must function when it is unavailable, permissions are denied, or health data is missing/incomplete. Distinguish application heuristics from scientifically sourced thresholds.

## Testing Workflow

For meaningful changes: inspect → implement → focused tests → fix → focused retest → relevant regression tests → commit.

Compilation alone does not prove correctness. Never remove or weaken tests merely to obtain a green build.

## GitHub Actions

Use and improve existing Actions infrastructure; do not create parallel CI without necessity. Progressively verify, when applicable: unit tests, Android lint, raw catalog validation, persistence, migrations, prescription engine, scheduler, progressions, v0.9 regressions, release build, APK signature, and APK artifact.

Never hide required-check failures with `continue-on-error`, `|| true`, or equivalent mechanisms.

## Git Safety

Never force push or delete `main`, rewrite shared history without necessity, delete tests just for green CI, overwrite valid current-schema user data without an explicit current-schema transition, or commit passwords, API keys, signing secrets, or keystores. Require explicit owner approval before destructive Git operations.

## Definition of Done Reporting

Use `VERIFIED`, `IMPLEMENTED_NOT_DEVICE_VERIFIED`, `PARTIAL`, or `BLOCKED`. Report `VERIFIED` only with supporting evidence. A successful build alone does not mean the release is complete.

## Task Scope and Context Efficiency

Work only on the requested area and strictly necessary supporting changes. Do not repeatedly audit the full repository unless explicitly requested. Before implementing a reported problem, verify it still exists in current HEAD. If already fixed, demonstrate that with appropriate tests/evidence instead of rewriting working code.

Reuse project documentation, tests, and Git history rather than rediscovering settled decisions. End focused tasks by reporting what was verified, what changed, tests executed, results, commit(s), and remaining limitations.
