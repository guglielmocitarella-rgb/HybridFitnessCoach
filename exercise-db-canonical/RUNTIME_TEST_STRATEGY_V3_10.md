# Runtime Test Strategy V3.10

Required suites:
- DB: 1087 records, unique IDs, schema/version/hash, references, provenance shape and typed relations.
- Equipment: every mode/state, UNKNOWN fail-closed, ONE_OF, OPTIONAL, ASSISTED_BY, EXTERNAL_LOAD, suspension_trainer distinct.
- Capability: four independent requirements + special states; assert no level mapping.
- Selection: replay 640 scenarios x 100 seeds; preserve 630 valid / 10 legitimate zero pools.
- Prescription: reps, seconds/duration, distance, intervals, load, assistance and combinations with units.
- Skill: 15 families/60 nodes, typed relations, prerequisites/mastery unchanged.
- Persistence: old plans, active/resume, suspend/partial, history, custom, adaptive extra, skill/library, single exercise and process recreation.
- Runner: prescribed vs actual, load/assistance, timed/reps/distance/recovery.
- Regression: all non-DB unit/lint and runner/adaptive suites.

Severity gate: S0=0 and S1=0 before cutover. S2 integration defects must be resolved before device gate; S3 is non-blocking enhancement.