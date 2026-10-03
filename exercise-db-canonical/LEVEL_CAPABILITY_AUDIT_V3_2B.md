# Exercise DB V3.2B — Level & Capability Audit

Source HEAD: `3212e64b60da75fe1b136843b51e8aaaddfbb407`

## Runtime semantics
`level` is a scalar hard eligibility requirement: `Principiante=1`, `Intermedio=2`, every other string=3; the generator requires `levelScore(exercise.level) <= capabilityLevelFor(exercise, capability)`. User capability is already multidimensional, while SkillCatalog separately locks progression nodes by prerequisites/mastery.

## Model decision
**INSUFFICIENT_NEEDS_CAPABILITY_MODEL**. Current persisted level values mix legacy authored labels and source difficulty labels without one formal semantic contract.

Minimum capability proposal: `technicalDifficulty`, `strengthRequirement`, `mobilityRequirement`, `balanceCoordinationRequirement`. A separate scalar `skillRequirement` is not proposed because SkillCatalog already owns prerequisites/mastery.

## Known-level quality audit
Known: **649**. Stratified sample: **18**. Systematic semantic issue: **YES**.

## UNKNOWN workset
Verified: **438/438 audited**.

- UNRESOLVED_INSUFFICIENT_EVIDENCE: **157**
- UNRESOLVED_SCHEMA_DECISION_REQUIRED: **138**
- UNRESOLVED_VARIABLE_DIFFICULTY: **143**

No UNKNOWN record met the evidence threshold for a scalar level patch. No value is inferred from name, equipment, weight, list position, or neighbouring records.

## Source normalization
No bulk three-level normalization is accepted. Existing ACE Difficulty mapping is source-specific; APP/V2 authored levels remain legacy values until their semantics are explicitly defined.

## Gate
**CLOSED_WITH_DOCUMENTED_UNRESOLVED** — all 438 UNKNOWN records are terminally classified; known levels were quality-audited; source-scale provenance was inventoried; capability-model gap is documented; no runtime DB, selection engine, Runner, equipment or resistanceMode data was modified.

## Executed validation
DB-only GitHub Actions run **37151816179** on commit `49f52dd3d41fb57acb8295fd93d7e1c2021918da`: PASS. JSON syntax, canonical validator, previous equipment/resistance regressions and V3.2B tests passed. Temporary workflow removed in the evidence commit.
