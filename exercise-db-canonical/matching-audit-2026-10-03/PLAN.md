# Audit matching implementation plan

Goal: read-only deterministic audit of 1087 canonical exercises against six pinned source snapshots.
Architecture: Python standard library; normalized source adapters; conservative candidate matching; 100-record atomic checkpoints; GitHub connector persistence before next tranche.
Spec: user request 2026-10-03, all 21 sections.
Constraints: no canonical/app/validator changes; no manual CI; no full external text/media redistribution; preserve source identity and hashes.

- [ ] Verify pinned source snapshots and runtime selection inputs.
- [ ] Test name-only false positives, variant/equipment conflicts, duplicate candidates and corrupt checkpoint rejection.
- [ ] Implement candidate retrieval, classification, field evidence and media references.
- [ ] Process and remotely verify 11 checkpoints, 100 records each except final 87.
- [ ] Aggregate disjoint match classes, field/media coverage, simulation, conflicts, strategy and engine reports.
Review focus: name similarity is insufficient; apparatus may be auxiliary; source forks are not independent evidence; generated/family cues do not establish completeness; previews do not establish production rights.

