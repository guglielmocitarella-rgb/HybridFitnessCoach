# R5 terminal membership recovery report

Baseline: `b8c6b6d2f380253e4ae75b39b4c760b8e806c020`

Result: **SUCCESS — 16/16 recovered deterministically**.

The historical counter was reconciled from 30 to 19 at `1ca495efd4f3bcf9cd4b7072e286abef143787a8`. At the persisted historical research-log state `5f233f2b9b7956d93c9163aa9ed9c0ba15cb6d77`, three terminal fields were explicitly field-scoped, while four records were classified whole-record terminal with zero resolved fields: `ex-90-degree-push-up`, `cm-ab-wheel-narrow-knee-rollout`, `cm-ab-wheel-rollout-to-plank`, and `cm-ab-wheel-wide-knee-rollout`. R3 residual materializes all four capability dimensions for each record, and the R3 resolved patch contains none of those 16 keys. Therefore those four persisted whole-record terminal decisions deterministically expand to exactly 16 semantic keys.

Later terminal-counter increments were 19→22, 22→32, 32→35, 35→45, and 45→48. Together with the three already field-scoped historical terminals, these account for the 32 memberships already materialized in the authoritative research-log shards. Final membership is 32 existing + 16 recovered = 48.

Validation: 48 unique; 48/48 in canonical `U_R5`; zero overlap with authoritative semantic keys; zero overlap between existing and recovered membership. No authoritative decisions, candidate/runtime/production data, Actions, V26 production, or out-of-universe remediation were touched.
