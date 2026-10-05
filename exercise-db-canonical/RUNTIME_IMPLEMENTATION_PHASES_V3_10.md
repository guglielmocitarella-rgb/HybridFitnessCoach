# Runtime Implementation Phases V3.10

## A Runtime Domain Model
V3 identity/equipment/resistance/capability/state/prescription/relation types. No production routing. Gate: contract tests; zero lossy conversion.

## B Asset Loader + Validation
Strict parser, schema/hash/count/reference validation and indexes. Gate: 1087 unique, frozen candidate identity.

## C Identity + Backward Compatibility Foundation
Canonical resolver and persisted-data adapters. Gate: old plans/active/history/custom/single fixtures readable.

## D Selection Engine Migration
Native equipment/location/four-capability/category logic; movement enhancing only. Gate: preserve V3.9R-B 630 valid + 10 legitimate zero pools, invalid selection=0.

## E Prescription + Runner Contract
Typed prescription -> immutable runner snapshot. Gate: reps/time/distance/interval/load/assistance combinations and prescribed/actual separation.

## F Skill Bridge
Typed relations keyed by canonical ID; SkillCatalog retains graph/mastery. Gate: 15 families/60 nodes, no dangling refs.

## G Persistence + End-to-End
Versioned write path, old readers, process recreation, rollback rehearsal. Gate: full regression green.

## H Device/APK Validation
Only after G. Gate: signed-off device protocol.

Exact next step: **V3.10A / Phase A only**.