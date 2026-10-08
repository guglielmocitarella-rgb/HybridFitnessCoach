# Step 9 — V3 Functional Gap Report

Baseline: `c1b315922d347c3048990d51ba094ab8b403d1a7`  
Frozen candidate SHA-256 verified: `0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a`  
Scope: static functional audit. No Android test/CI was run because Step 9 changes diagnostics only.

## Executive verdict

V3 is **not yet functionally complete enough for a production switch**, but the migration does not require replacing the whole application stack. V3 already provides validated asset loading, fail-closed eligibility, typed equipment/capability/skill/prescription semantics, explicit zero-pool states, deterministic ordering and a basic substitution primitive. Missing are the orchestration capabilities: athlete-context mapping, personalized ranking, durable variety, workout composition, contextual prescription/materialization, and V3 integration for skill/custom flows.

Runner, session/history, planning/scheduling, readiness/adaptation policies and much of persistence are selector-independent and should be retained behind V3-native boundaries. Historical data must remain readable through compatibility codecs/mapping, not by retaining V2 selection.

## Functional coverage matrix

| # | Functionality | Classification | Evidence / migration requirement |
|---|---|---|---|
| 1 | Caricamento catalogo | `V3_NATIVE_COMPLETE` | V3AssetLoader loads and validates ExerciseV3; runtime onCreate still installs V2. |
| 2 | Filtraggio attrezzatura | `V3_NATIVE_COMPLETE` | V3 equipment eligibility handles NONE, REQUIRED, OPTIONAL, ONE_OF, ASSISTED_BY and EXTERNAL_LOAD fail-closed. |
| 3 | Capacità fisiche e tecniche | `REQUIRES_V3_ADAPTATION` | V3 evaluates strength/technical/mobility/balance, but no AthleteProfile -> V3AthleteCapabilities runtime mapper exists and current profile axes differ. |
| 4 | Skill e progressioni | `V3_NATIVE_PARTIAL` | V3 typed skill relations filter family/node intent; current mastery/progression still derives from SkillCatalog/ProgressionEngine and history. |
| 5 | Obiettivi atleta | `REQUIRES_V3_IMPLEMENTATION` | Current generator changes role priority by AthleteProfile.goal; V3 intent has categories/skill targets but no goal-aware ranking/composition. |
| 6 | Recupero muscolare | `REUSABLE_V2_INDEPENDENT` | recoveryScores derives muscle fatigue from persisted structured snapshots/history; only its live-catalogue fallback is currently catalogue-bound. |
| 7 | Fatica e readiness | `REUSABLE_V2_INDEPENDENT` | Readiness/adaptive decisions and contextual prescription consume generic session/readiness signals. |
| 8 | Storico esercizi | `REUSABLE_V2_INDEPENDENT` | SessionRecord structured actuals and ExerciseStatistics are selector-independent after materialization. |
| 9 | Penalizzazione ripetizioni | `REQUIRES_V3_ADAPTATION` | Current generator applies a last-3-session name penalty; V3 has no history-aware score. |
| 10 | Ranking e priorità | `REQUIRES_V3_IMPLEMENTATION` | Current generator has personalized weighted ordering; V3 deterministicOrder is only stable seeded ordering. |
| 11 | Diversificazione proposte | `REQUIRES_V3_IMPLEMENTATION` | V3 has no variety memory or diversity objective; current variety is incidental to penalties/pool composition. |
| 12 | Composizione workout | `REQUIRES_V3_IMPLEMENTATION` | Current generator fills a session by goal roles/skill focus then sequences/coaches it; V3 pool does not compose workouts. |
| 13 | Durata e volume allenamento | `REQUIRES_V3_ADAPTATION` | Launch intent carries duration and current prescriptions/periodization carry volume; V3 lacks a session-level duration/volume budgeter. |
| 14 | Prescrizione serie, ripetizioni, tempo, carico e recupero | `V3_NATIVE_PARTIAL` | V3 has typed prescription dimensions and PD strength resolution; no native contextual set/rest/load materializer equivalent to current runtime. |
| 15 | Allenamenti personalizzati | `REQUIRES_V3_ADAPTATION` | Current custom/skill/library pools and regeneration operate on Exercise and current skill mastery. |
| 16 | Generazione piani | `REUSABLE_V2_INDEPENDENT` | TrainingPlanEngine schedules intent/calendar independently of V2; concrete exercises are materialized later. |
| 17 | Materializzazione sessioni | `REQUIRES_V3_ADAPTATION` | Current planned-session realization regenerates from Exercise catalogue and snapshots legacy Exercise. |
| 18 | Adattamento allenamenti | `REUSABLE_V2_INDEPENDENT` | AdaptiveCoach, ContextualPrescriptionEngine and AdaptiveScheduler are reusable policy/history components; exercise-facing boundaries need V3 inputs. |
| 19 | Sostituzione esercizi | `V3_NATIVE_PARTIAL` | validSubstitutes enforces V3 eligibility and same category but lacks movement/family/goal/history quality ranking. |
| 20 | Runner e storico | `REUSABLE_V2_INDEPENDENT` | Runner executes materialized targets/snapshots and history stores actuals; selection is not required during execution. |
| 21 | Persistence e dati utente | `REQUIRES_V3_ADAPTATION` | Profile/plans are mostly selector-independent; active/history snapshots serialize Exercise and progressionKey and need compatibility mapping/read paths. |
| 22 | Gestione casi senza esercizi eleggibili | `V3_NATIVE_COMPLETE` | V3 pool explicitly distinguishes VALID_POOL, LEGITIMATE_ZERO_POOL and INVALID_ZERO_POOL and exposes rejection reasons. |

Counts: **3 complete**, **3 partial**, **6 reusable V2-independent**, **6 requiring V3 adaptation**, **4 requiring V3 implementation**.

## Architectural gaps

### G1 — Athlete profile to V3 capability context (P0)
**Evidence:** AthleteProfile.toCapabilityProfile maps legacy movement axes; V3 expects strength/technical/mobility/balance semantic values plus PD context.  
**Available:** V3 capability requirements and PD resolver  
**Build:** Pure profile/context mapper with explicit unknown semantics and PD inputs  
**Tests:** axis mapping; unknown fail-closed; PD context.  
**Acceptance:** All V3 eligibility requests derive capabilities without legacy levelScore/capabilityLevelFor.

### G2 — History-aware personalized ranking (P0)
**Evidence:** Current generator scores repetition, muscle recovery, priority skill and planned-name avoidance; V3 deterministicOrder is not personalized.  
**Available:** Eligible V3 pool plus deterministic tie-breaker  
**Build:** Explainable V3 ranking using goal/history/recovery/readiness/skill/plan context  
**Tests:** recent penalty; recovery penalty; goal/skill priority; deterministic ties.  
**Acceptance:** Ranking is personalized, deterministic for equal context and exposes score reasons.

### G3 — Diversity memory and workout composition (P0)
**Evidence:** V3 pool returns eligible exercises only; current generator composes up to six and diversity is a score/order side effect.  
**Available:** V3 pool and future ranking  
**Build:** Composer with balance, duplicate-family limits, duration/volume budget, bounded variety memory and explicit relaxation  
**Tests:** multi-session diversity; small pool; movement balance; duration budget.  
**Acceptance:** Complete workouts are produced when feasible and constraint relaxation is explainable.

### G4 — Native V3 prescription/materialization (P0)
**Evidence:** V3 prescription describes dimensions; current effectiveRunnerPrescription/plannedExerciseState create executable sets/rest and contextual adaptation.  
**Available:** V3 dimensions and PD resolver  
**Build:** V3 contextual prescription resolver and Runner materializer for reps/time/distance/load/assistance/rest/open-ended  
**Tests:** all measures; required load/assistance; open-ended; readiness; restore.  
**Acceptance:** Composed V3 workouts materialize executable Runner state without V2/legacy prescription functions.

### G5 — Skill mastery/progression bridge (P1)
**Evidence:** Current SkillCatalog/ProgressionEngine derives mastery from history; V3 skill relations currently filter targets.  
**Available:** Typed V3 skill relations  
**Build:** Canonical V3 mastery/focus resolver and progression-aware ranking/prescription  
**Tests:** locked/available/in-progress/mastered; progress/regress; equipment/location.  
**Acceptance:** Skill workouts select reachable canonical nodes without V2.

### G6 — User-data identity compatibility (P0 before switch)
**Evidence:** Active/history structured exercises contain name, exerciseId/progressionKey and full legacy Exercise snapshot; plans persist intent rather than concrete exercises.  
**Available:** Canonical V3 IDs and StoreRecovery  
**Build:** Legacy snapshot reader plus canonical mapping policy; preserve raw historical snapshot  
**Tests:** old active restore; old history; unknown legacy identity; plan intent; no destructive rewrite.  
**Acceptance:** Existing data stays readable/executable where appropriate without a V2 selector.

### G7 — V3 custom/library/substitution integration (P1)
**Evidence:** Current custom/regeneration paths use Exercise; V3 substitution quality is category-only.  
**Available:** V3 eligibility and validSubstitutes  
**Build:** V3 custom filters, skill focus and quality-aware substitution ranking  
**Tests:** custom filters; skill focus; pinned items; semantic substitutions.  
**Acceptance:** All launch origins resolve through V3-only selection/composition.

### G8 — Zero-pool UX/diagnostics bridge (P1)
**Evidence:** V3 distinguishes legitimate and invalid zero pools with rejection reasons; current runtime flows do not consume that status.  
**Available:** V3SelectionPool status/rejections  
**Build:** Application relaxation/error policy and user-facing diagnostic mapping  
**Tests:** legitimate empty; invalid context; equipment/capability explanation.  
**Acceptance:** No silent empty workout; caller distinguishes legitimate empty intent from invalid context.

## Repetition analysis

### OBSERVED_IN_CODE
- Current automatic generation counts names in the three most recent sessions and applies +45 per occurrence.
- Muscle recovery penalties are 140/85/40 below recovery thresholds 40/60/80.
- Priority skill subtracts 70 and avoidPlannedNames adds 100.
- Location/equipment and capability filtering happen before ranking, so location changes can materially shrink/change the pool.
- After numeric score, preferred role order is used; V3 deterministicOrder has no history memory.

### HYPOTHESIS_REQUIRING_TEST
- A narrow location pool may dominate repeat penalties and cause repeated exercises.
- Equal score/role candidates may repeatedly favor a stable subset.
- Three-session name memory may be too short for desired long-horizon variety.

### NOT_SUPPORTED_BY_EVIDENCE
- Static analysis does not prove a user-visible repetition bug or quantify frequency.
- Static analysis does not prove location is the sole cause.

### Required future tests
- Generate at least 20 sessions per representative location and measure exercise/family recurrence.
- Change location while holding profile/history constant.
- Inject recent history and verify rank movement.
- Simulate recovery over time and verify re-entry.
- Verify deterministic replay for identical context and controlled change when history changes.
- Test small pools and distinguish intentional from avoidable repeats.

## User-data safety

| Area | Risk | Required handling |
|---|---|---|
| Athlete profile and locations | LOW | Keep directly; map capabilities/equipment into V3 requests without destructive migration. |
| Training plans | LOW | Keep intent/schedule directly; materialize through V3 after switch. |
| Active workout | HIGH | Restore from persisted snapshot; map identity only for future references; never regenerate mid-workout. |
| Session history | HIGH | Keep immutable actuals and legacy snapshots readable; add canonical mapping/read compatibility; no V2 selector required for display. |
| Skill progression keys | MEDIUM | Map stored skill_node_* IDs explicitly to V3 family/node identities before switch. |
| Unknown/retired references | MEDIUM | Preserve historical label/snapshot and mark unmapped identity; never drop or silently substitute. |

Plans are comparatively safe because current persistence stores intent/schedule and materializes exercises at execution time. Active workouts and history are the critical compatibility surface because they contain legacy Exercise snapshots plus IDs/names. Preserve those snapshots as historical truth; add canonical identity mapping where available; do not regenerate/delete them and do not retain V2 selection merely to display history.

## Static validation

- JSON/Markdown classifications come from the same matrix.
- Frozen candidate content SHA-256 was re-read and verified before persistence.
- No runtime source/model/production composition is modified by Step 9.
- No V2 dependency is added.
- No dynamic benchmark claim is made.

**Verdict:** `STEP_9_FUNCTIONAL_AUDIT_COMPLETE`
