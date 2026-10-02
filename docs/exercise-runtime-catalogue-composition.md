# Runtime catalogue composition — pre-final evidence

This note records what can be proven from the currently published feature/v0.9.5 source. It is not yet the final audit of the local APK whose UI shows 1091 exercises.

## Published source composition

The published code builds `legacyExercises` from:
- `coreExerciseCatalog`
- `extendedExerciseCatalog`
then deduplicates those by normalized exercise name.

At application startup, `installExerciseDbV2(ExerciseDbV2.load(this))`:
1. migrates every V2 record into an `Exercise`;
2. retains legacy/current-model exercises not represented by V2;
3. combines both sets;
4. deduplicates by V2 progressionKey when present, otherwise normalized name;
5. assigns the result to the global `exercises` list.

Workout generation defaults to this global `exercises` catalogue and filters it for location compatibility before selection.

Therefore the app does not select workouts from the V2 JSON alone.

## Static counts in currently published feature/v0.9.5

- V2 JSON: 587 records
- core catalogue source: 206 Exercise constructors
- extended catalogue source: 394 Exercise constructors

These constructor counts are source-level counts, not the final deduplicated runtime count. They also do not explain the 1091 shown by the newer local APK. The final 1091 audit must use the exact source state that produced that APK after Work publishes it.

## Audit rule

Do not treat 587 as the application catalogue size. The authoritative target for the next full audit is the 1091-entry runtime catalogue observed in the test APK. Recompute exact V2/legacy/current-model overlap and runtime eligibility from the published Work state before any mass DB completion or deletion.
