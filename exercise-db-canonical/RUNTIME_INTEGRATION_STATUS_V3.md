# Runtime Integration Status V3

| Gate | Origin | Status / reason | Required before |
|---|---|---|---|
| V3_AUTHORITATIVE_ASSET_FULL_KOTLIN_EXECUTION_GATE | V3.10B / V3.10B-X / V3.10B-X2 | DEFERRED_MANDATORY_PRE_PRODUCTION — MATERIALIZATION_ENVIRONMENT_LIMITATION | PRODUCTION_ACTIVATION |
| V3_NATIVE_SELECTION_ENGINE_GATE | V3.10C | SELECTION_REMEDIATION_REQUIRED — native four-capability coverage cannot use legacyLevel fallback | NEXT_RUNTIME_MIGRATION_PHASE |

Required loader evidence before production: exact candidate bytes; Git blob identity; real `V3AssetLoader.loadAuthoritative`; input/parsed/validated/mapped 1087; rejected/missing/duplicates 0; silent semantic loss 0.

Allowed before loader gate: domain development, shadow integration, isolated simulation and local non-production tests. Forbidden: V3 production repository/default DB, Runner production switch, persistence migration, or APK/device claims of completed V3 production integration.

## V3.10C shadow boundary
`V3SelectionEngine` consumes `ExerciseV3` directly and is not bound into production DI/composition. Production behavior remains unchanged. The implementation preserves structured equipment, four capability dimensions, typed skill relations, multidimensional prescription, domain-aware unresolved handling, deterministic ordering, explicit legitimate/invalid zero-pool status, and substitution hard constraints.

Known blocker: V3.10B reports all four scalar capability requirements UNKNOWN for the authoritative candidate. V3.9R-B coverage used a legacyLevel fallback in residual simulation; V3.10C forbids that fallback. This is S1 until native capability semantics/data are resolved.

## GitHub Actions policy
Free Actions budget exhausted. Local-first validation is mandatory. Do not create temporary workflows or intentionally trigger Actions without explicit user authorization.

## Push policy
Develop/test locally; no push-as-test; target one substantive commit and one push per phase; use `[skip ci]` where compatible; extra push only for demonstrated exceptional repository/infrastructure correction.
