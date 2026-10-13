# Runtime Migration Plan V3.10

1. Introduce V3 domain types without production routing.
2. Add strict V3 asset loader/validator and indexed repository.
3. Build canonical identity + legacy alias resolver before selection migration.
4. Introduce athlete-capability compatibility input while keeping the four exercise requirements native.
5. Migrate selection/location/equipment fail-closed behind a feature flag/parallel evaluator.
6. Migrate multidimensional prescription into immutable Runner snapshots; preserve actual separately.
7. Add typed Exercise-to-Skill bridge; SkillCatalog remains graph authority.
8. Add persistence schema v2 readers/writers with old-schema read compatibility.
9. Execute simulation/regression/parity and rollback rehearsal.
10. Enable V3 path only after all gates; device/APK validation last.

Rollback keeps old loader/readers until end-to-end validation. Already-written V3 sessions must remain readable even if V3 generation is disabled. No destructive user-data migration.