# Exercise database matching audit — final

Integrity: **1087/1087**, unique **1087**, missing **0**, duplicates **0**.

## Match classification

- CERTAIN: 0 (0.0%)
- HIGH_PROBABILITY: 270 (24.84%)
- AMBIGUOUS: 495 (45.54%)
- NO_MATCH: 322 (29.62%)

CERTAIN remains zero deliberately: current evidence does not establish certainty.

## Per dataset

| Dataset | Records | Candidate coverage | High probability | Ambiguous | No match | Instruction | Media | License |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| yuhonas/free-exercise-db | 876 | 442 (40.66%) | 76 | 366 | 645 | 436 | 441 | SAFE_TO_USE |
| kinetic-place/exercises-json | 899 | 455 (41.86%) | 89 | 366 | 632 | 455 | 0 | ATTRIBUTION_REQUIRED |
| ArelliGoutham/exercise-database | 1376 | 629 (57.87%) | 176 | 453 | 458 | 628 | 629 | ATTRIBUTION_REQUIRED |
| RepDB/exercise-dataset | 609 | 561 (51.61%) | 108 | 453 | 526 | 561 | 561 | ATTRIBUTION_REQUIRED |
| rthepen/workout-database | 1015 | 610 (56.12%) | 143 | 467 | 477 | 610 | 599 | ATTRIBUTION_REQUIRED |
| harshvishu/free-exercise-db-with-videos | 317 | 458 (42.13%) | 83 | 375 | 629 | 458 | 458 | LICENSE_UNCLEAR |

## Field audit

| Field | Current | Source available | High-probability | Ambiguous | Still missing |
|---|---:|---:|---:|---:|---:|
| equipment | 990 | 45 | 0 | 45 | 52 |
| resistanceMode | 976 | 0 | 0 | 0 | 111 |
| movementPattern | 911 | 0 | 0 | 0 | 176 |
| level | 649 | 280 | 49 | 231 | 158 |
| skillLink | 944 | 0 | 0 | 0 | 143 |
| primaryMuscles | 0 | 721 | 235 | 486 | 366 |
| secondaryMuscles | 0 | 753 | 262 | 491 | 334 |
| bodyRegion | 0 | 662 | 163 | 499 | 425 |
| description | 189 | 534 | 167 | 367 | 364 |
| setup | 189 | 0 | 0 | 0 | 898 |
| execution | 189 | 594 | 204 | 390 | 304 |
| techniqueCues | 36 | 731 | 251 | 480 | 320 |
| commonErrors | 34 | 445 | 80 | 365 | 608 |

## Historical blockers

| Category | Baseline | High-probability | Ambiguous | Residual | Improvement |
|---|---:|---:|---:|---:|---:|
| equipment_unknown | 97 | 0 | 45 | 97 | 0.0% |
| equipment_unsupported_by_app | 18 | 12 | 5 | 6 | 66.67% |
| movement_pattern_missing | 176 | 0 | 0 | 176 | 0.0% |
| instruction_incomplete | 500 | 150 | 208 | 350 | 30.0% |
| level_unknown | 438 | 49 | 231 | 389 | 11.19% |
| resistance_mode_unknown | 111 | 0 | 0 | 111 | 0.0% |
| skill_links_missing | 143 | 0 | 0 | 143 | 0.0% |

## Media and licensing

- **yuhonas/free-exercise-db**: 2082 references / 441 canonical exercises; {'photo': 2082}; **SAFE_TO_USE** — Public-domain dedication in pinned LICENSE.md.
- **kinetic-place/exercises-json**: 0 references / 0 canonical exercises; {}; **ATTRIBUTION_REQUIRED** — MIT notice required.
- **ArelliGoutham/exercise-database**: 4479 references / 629 canonical exercises; {'image': 4479}; **ATTRIBUTION_REQUIRED** — MIT notice required.
- **RepDB/exercise-dataset**: 2409 references / 561 canonical exercises; {'illustration': 2409}; **ATTRIBUTION_REQUIRED** — In-app use with attribution; dataset redistribution prohibited; paid preview animations evaluation-only.
- **rthepen/workout-database**: 1446 references / 599 canonical exercises; {'video': 1446}; **ATTRIBUTION_REQUIRED** — MIT notice required.
- **harshvishu/free-exercise-db-with-videos**: 5714 references / 458 canonical exercises; {'video': 2857, 'thumbnail': 2857}; **LICENSE_UNCLEAR** — Repository is MIT; external video URLs need separate media-rights review.

## Conflicts

{"variant_qualifier_difference": 2219, "equipment_disjoint": 3339, "multiple_supported_candidates_in_source": 55}

## Manual review queue

817 canonical exercises require review.

## Next phase

Use HIGH_PROBABILITY only as a field-by-field enrichment candidate queue; do not auto-import AMBIGUOUS. Review media rights separately. Keep the canonical database unchanged until reviewed enrichment goes through the existing builder/provenance path.
