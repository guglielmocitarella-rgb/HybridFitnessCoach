# Step 9 — V3 Integration Roadmap

This roadmap separates functional completion, integration, user-data safety, production switch and V2 removal. Each block is small and independently verifiable.

## Phase 1 — V3 functional completion

- **Block:** G1 capability/context mapper.
- **Block:** G2 ranking/explanations.
- **Block:** G3 diversity/composer.
- **Block:** G4 prescription/materializer.
- **Block:** G5 skill bridge.
- **Block:** G8 zero-pool policy.
- **Completion gate:** V3 creates executable workouts from profile+context+history without V2.

## Phase 2 — Workout-generation integration

- **Block:** Route automatic/custom/skill/library/plan materialization through V3.
- **Block:** G7 substitution/custom integration.
- **Completion gate:** All launch origins use V3 selection/ranking/composition while Runner stays stable.

## Phase 3 — User-data safety and migration

- **Block:** G6 compatibility reader/mapping.
- **Block:** fixtures for old active/history/skill nodes.
- **Completion gate:** Existing data survives upgrade and historical reads need no V2 selector.

## Phase 4 — Production switch

- **Block:** Load frozen V3 as runtime authority.
- **Block:** disable V2 generation/loading paths after gates.
- **Completion gate:** Production generation is V3-only with no data loss.

## Phase 5 — Final verification and V2 removal

- **Block:** regression/migration/variety/device tests.
- **Block:** remove dead V2 selection/loader assets after compatibility separation.
- **Completion gate:** No runtime V2 dependency remains and acceptance suites pass.

## Ordered implementation blocks

### 9→10A — Athlete/context bridge
Implement a pure profile + readiness/history + location -> V3 selection context boundary. Test every capability state, equipment mode and PD strength context. **Done when** V3 eligibility can be invoked from production-shaped inputs without legacy levelScore/capabilityLevelFor.

### 10B — Native ranking and explanations
Add history/recovery/goal/skill/plan-aware scoring over an eligible V3 pool. Keep deterministic ordering only as a tie-breaker. **Done when** score components are inspectable and recent/recovery/goal tests pass.

### 10C — Diversity + workout composer
Compose sessions under movement/category/skill balance and duration/volume budgets; add bounded variety memory and explicit constraint relaxation. **Done when** repeated-generation tests show controlled variety and small pools degrade explicitly.

### 10D — Native prescription/materialization
Resolve V3 prescription dimensions into Runner sets, targets, load/assistance and rest, including contextual adaptation/open-ended work. **Done when** every supported V3 prescription becomes valid Runner state without V2/legacy prescription functions.

### 10E — Skill/custom/library/substitution
Move mastery focus, custom filters, pinned regeneration and substitution quality to V3 IDs/relations/ranking. **Done when** every non-plan launch origin is V3-only.

### 10F — Plan materialization
Keep the existing intent/calendar engine, but materialize planned sessions through the same V3 composer/prescriber. **Done when** plan execution and adaptive-extra workouts share one V3 pipeline.

### 10G — Data compatibility gate
Before switch, add versioned readers/mapping fixtures for legacy active workouts, session snapshots and stored skill-node IDs. Never rewrite historical actuals. **Done when** upgrade fixtures restore active work, render history, preserve plan ownership and safely represent unmapped identities.

### 10H — Production switch
Load the frozen V3 candidate as runtime authority and route all generation through V3. Keep compatibility readers only. **Done when** exact-SHA CI/device acceptance passes and no generation path calls V2.

### 10I — Final verification and V2 removal
Run regression, migration, zero-pool, variety and device tests; then remove dead V2 loader/selection assets/adapters. **Done when** repository search/tests show no runtime V2 dependency while old persisted records remain readable.

## Non-negotiable acceptance gates

1. No silent empty workout: V3 zero-pool status reaches the caller.
2. No destructive persistence migration and no regeneration of an active workout during upgrade.
3. Historical snapshots remain historical truth when canonical exercises change.
4. Personalized ranking is distinct from deterministic tie-breaking.
5. Variety is measured dynamically; static analysis alone cannot certify the repetition issue solved.
6. Production switch occurs only after all launch origins and plan materialization use V3.
