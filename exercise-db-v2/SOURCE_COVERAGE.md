# Source coverage — Exercise DB v2

This report is a completion gate. A registered source is **INGESTED** only when a versioned raw inventory exists and its accepted movements have been merged into the canonical artifact with provenance.

| source_id | status | raw inventory |
|---|---|---|
| COMPLETE_CAL2 | INGESTED | raw/complete_calisthenics_2e.tsv |
| STARTING_STRENGTH3 | INGESTED | raw/starting_strength_3e.tsv |
| SCIENCE_STRENGTH | INGESTED | raw/science_strength.tsv |
| VELLA_ANATOMY | INGESTED | raw/vella_anatomy.tsv |
| NSCA_TECH | INGESTED | raw/nsca_technique.tsv |
| ACSM12 | GUIDANCE_ONLY_PENDING | — |
| NSCA_ESS5 | PENDING | — |
| SCHOENFELD_HYP2 | GUIDANCE_ONLY_PENDING | — |
| DELAVIER3 | PARTIAL_INGESTED | raw/delavier3.tsv |
| OVERCOMING_GRAVITY | INGESTED | raw/overcoming_gravity.tsv |
| COMPLETE_CAL1 | PARTIAL_INGESTED | raw/complete_calisthenics_1e.tsv |
| CAL_BEGINNER_50 | INGESTED | raw/calisthenics_beginner_50.tsv |
| CAL_BEGINNER_150 | PARTIAL_INGESTED | raw/calisthenics_beginner_150.tsv |
| ULTIMATE_CAL_COLLECTION | PARTIAL_INGESTED | raw/ultimate_cal_collection.tsv |

INTEGRATION_READY is forbidden while any exercise-repertoire source is PENDING, CROSSCHECK_PENDING, or PARTIAL_INGESTED. Guidance-only sources may finish without adding canonical movements, but their programming evidence must be materialized separately if used.

Current canonical count is derived from exercises.json, never from a target number.
