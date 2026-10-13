# Step 9 — V3 Integration Roadmap

This roadmap separates functional completion, integration, user-data safety, production switch and V2 removal. Each block is small and independently verifiable.

## Binding product decision — V3 functional continuity, clean break for test data (2026-10-08)

This section supersedes all historical-data compatibility and migration requirements in the original Step 9 roadmap. The Step 9 audit findings remain historical evidence, not an implementation mandate.

- **Functional continuity is mandatory:** V3 must retain or improve every useful V2 application capability identified by the 22-function audit. Equivalent outputs, algorithms and V2 runtime dependencies are not required.
- **Old persisted application data is disposable:** existing saved sessions, history, active workouts, plans, skill progress and settings were test data. No legacy readers, old-ID mapping, upgrade fixtures, history migration or preservation of pre-V3 data is required.
- **New V3 persistence is mandatory:** after the switch, new plans, sessions, actuals, skill progress and active workouts must save/restore correctly. A deliberate reset or clean install at cutover is acceptable; do not silently describe this as a nondestructive upgrade.
- **V3-only end state:** no V2 selection, fallback or compatibility adapter in the final runtime. Reuse independent Runner, planning, readiness and storage logic where it preserves behavior.
- **Production gate:** all 22 functional areas must have a documented V3 implementation or justified replacement and tests; cover automatic, custom, skill, library, plan and adaptive-extra origins, variety, zero-pool, prescriptions, Runner and new-data persistence before switching.

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

## Phase 3 — V3-native persistence and clean cutover

- **Block:** Persist and restore NEW V3 workouts, session actuals, history, plans and skill progress using V3-native identities.
- **Block:** Define explicit reset/clean-install behavior for disposable pre-V3 test data; no legacy migration or reader.
- **Completion gate:** New V3 data survives app restart and process recreation; old test data may be discarded.

## Phase 4 — Production switch

- **Block:** Load frozen V3 as runtime authority.
- **Block:** disable V2 generation/loading paths after gates.
- **Completion gate:** Production generation is V3-only and new V3 data is persistent; pre-V3 test data may be reset.

## Phase 5 — Final verification and V2 removal

- **Block:** functional-continuity/new-V3-persistence/variety/device tests.
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

### 10G — V3-native persistence and reset gate
Before switch, ensure new V3 active workouts, session actuals, history, plans and skill progress save/restore with V3 identities. Define a deliberate clean-cutover/reset policy for disposable pre-V3 test data; do not build legacy readers or mapping. **Done when** new V3 persistence and restart/process-recreation tests pass.

### 10H — Production switch
Load the frozen V3 candidate as runtime authority and route all generation through V3. No legacy compatibility readers are required. **Done when** exact-SHA CI/device acceptance passes and no generation path calls V2.

### 10I — Final verification and V2 removal
Run functional-continuity, new-data persistence, zero-pool, variety and device tests; then remove dead V2 loader/selection assets/adapters. **Done when** repository search/tests show no runtime V2 dependency and new V3 data remains readable.

## Non-negotiable acceptance gates

1. No silent empty workout: V3 zero-pool status reaches the caller.
2. A deliberate reset of pre-V3 test data is allowed; no legacy migration requirement.
3. New V3 workout/session snapshots and actuals must remain persistent and internally consistent.
4. Personalized ranking is distinct from deterministic tie-breaking.
5. Variety is measured dynamically; static analysis alone cannot certify the repetition issue solved.
6. Production switch occurs only after all launch origins and plan materialization use V3.
