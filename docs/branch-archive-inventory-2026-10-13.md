# Branch archive inventory — 2026-10-13

**Status:** CLASSIFIED / LOGICAL ARCHIVE ONLY / NO BRANCHES DELETED  
**Primary integration base:** `feature/exercise-db-v3` @ `f2698fc2ec51472a34d12502a3b74d89322ec186`  
**New active working branch:** `feature/v3-integration-v095-stabilization` (initially created at that exact commit).  
**Decision record:** `docs/decisions/2026-10-13-v3-v095-consolidation.md`

## Audited Git refs

All **43 pre-existing branches** are recorded with their exact heads below; there are **44 branches after creation** of the consolidation branch. The source tip list was read from GitHub on 2026-10-13. Relations were computed with GitHub commit ancestry compare against the V3 tip; **A = ancestor** (the V3 branch already contains every commit on this ref), **D = diverged** (unique commits are present), **S = successor/descendant** of the V3 baseline (five extra commits), **BASE** = V3 branch itself, **MAIN** = the default branch (also diverged). Counts excluding BASE/MAIN: A=24, D=16, S=1.

The classification **is not a deletion order**. `ARCHIVE_CANDIDATE` means no unique commit relative to the V3 baseline, **not** that it has ceased to be valuable as an acceptance marker. `RETAIN` means do not retire it even if it is an ancestor. `REVIEW_UNIQUE` means no deletion without a diff audit. No tags, Git ref deletion, forced updates or PR changes have been executed.

## Ref-by-ref inventory

| Branch | Exact head SHA | Compared with V3 | Proposed disposition |
|---|---|---|---|
| `archive/v0.9.4-pre-f02-f06-audit` | `982653691f18f3b2313a2289a761b3126fec9dc4` | D | REVIEW_UNIQUE |
| `audit/p0-atomic-delete-red` | `a5d7b68733ca2aba6f0bbf63bc60e9efe0f9348b` | D | REVIEW_UNIQUE |
| `backup/exercise-db-r5-v8-safe` | `ff0939e48dda5bf2db51b69bc26711766d70336b` | A | ARCHIVE_CANDIDATE |
| `backup/exercise-db-r5-v16-safe` | `63ba030d115e74d082011dec61b900b2ad3a5b91` | A | ARCHIVE_CANDIDATE |
| `backup/r5-v29-complete-2026-10-07` | `a280a6b23095525782b259cbd2b28d371f570138` | A | RETAIN |
| `checkpoint/v0.9.4-capability-location-green` | `20e377e875e987fe6471a52f0f0a3082fa34bc8a` | A | ARCHIVE_CANDIDATE |
| `checkpoint/v0.9.4-f01-evidence-green` | `e4c4f208b8f6909aa76680b9b7316bef5f10ebc1` | A | ARCHIVE_CANDIDATE |
| `checkpoint/v0.9.4-f01-f11-green` | `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07` | A | RETAIN |
| `checkpoint/v0.9.4-f05-f07-green` | `fcfcbb200da73a6fa2475d5c29ee63eeeaf05ecf` | A | ARCHIVE_CANDIDATE |
| `checkpoint/v0.9.4-f08-green` | `f96d426964630c354c2aa4f08880c1caceb232be` | A | ARCHIVE_CANDIDATE |
| `checkpoint/v0.9.4-f09-green` | `801f4a827933cecaeb5f1780e016a62735f388a5` | A | ARCHIVE_CANDIDATE |
| `checkpoint/v0.9.4-f10-green` | `1ec072b36e2cde04bf16a7d8a190a28c6c276f44` | A | ARCHIVE_CANDIDATE |
| `copilot/feature-v093-audit` | `5ea8b6c8de8dc75f9f61d8601f63eeddff598cc5` | A | ARCHIVE_CANDIDATE |
| `copilot/featurev091-training-plan` | `ab5c7cec24cba52c73e586f3c1ccf72d6a805e25` | D | REVIEW_UNIQUE |
| `copilot/featurev091-training-plan-audit` | `72101ee88df979a9c2a7cc04c5d7be82f0791423` | A | ARCHIVE_CANDIDATE |
| `feature/exercise-db-v2-source-driven` | `d6d6cf7a906953865069ce362ecba8e0f7ed74a8` | D | REVIEW_UNIQUE |
| `feature/exercise-db-v3` | `f2698fc2ec51472a34d12502a3b74d89322ec186` | BASE | RETAIN |
| `feature/v0.2-app` | `99fa0a31eebfdc8f766ee49048638ea8daea367c` | D | REVIEW_UNIQUE |
| `feature/v0.2-interactive` | `1ec0ea6492e108d0ae765fca9fbc9d93b2d88282` | D | REVIEW_UNIQUE |
| `feature/v0.3-persistence-profile` | `d79f6a921466b02ab407e38bcbe00284ff1e02e1` | D | REVIEW_UNIQUE |
| `feature/v0.4-coach-engine` | `20a2d08516d146ffa0020624fc6dc29277695438` | D | REVIEW_UNIQUE |
| `feature/v0.5-release-signing` | `1eab737fb7ee0b4060774c6cd898a1189977f4d0` | D | REVIEW_UNIQUE |
| `feature/v0.6-adaptive-history` | `d6aecbcde8330499025c84040d66e016ace5abce` | D | REVIEW_UNIQUE |
| `feature/v0.7-recovery-engine` | `6bccfc46701624f8aef897fca865e5d67c157179` | D | REVIEW_UNIQUE |
| `feature/v0.8-custom-workouts` | `88ce901bdda2e39fdcf53cf6245e4becd2df63d0` | A | ARCHIVE_CANDIDATE |
| `feature/v0.9-adaptive-progression` | `933acc5c1d10875f53d366bcec62f40fe9b0509e` | A | ARCHIVE_CANDIDATE |
| `feature/v0.9.1-training-plan` | `0735f62152221299cbdc894eb023456f685c05fa` | A | ARCHIVE_CANDIDATE |
| `feature/v0.9.3` | `42476dd561d375fc6d80d9dfb16015b552f058ac` | A | ARCHIVE_CANDIDATE |
| `feature/v0.9.4` | `cb7cf5c79d0f4dbe501aa364ed1627a928e5ec5c` | A | ARCHIVE_CANDIDATE |
| `feature/v0.9.5` | `80d47d2f7e9e3cb941b9363ae3d77e5c6cf93127` | D | RETAIN |
| `main` | `5deda6e3a5ce9bc6ce04a954f6c6589b3777ffa0` | MAIN | RETAIN |
| `main-2233976235869140404` | `b3231c961e4849dbb500b840f95a14f9894ba9d7` | D | REVIEW_UNIQUE |
| `r5-v12-promotion-temp` | `48778f4a0d4b0da8afe375c5faff62f33453d0fc` | A | ARCHIVE_CANDIDATE |
| `r5-v28-staging` | `c7290a3d1c429f228975265d78f2d785f3c4b082` | A | ARCHIVE_CANDIDATE |
| `r5-v29-finalization-docs` | `8b9cdfbfe1ff3d47bf55e4ae5a8b4ad14c50b15c` | A | ARCHIVE_CANDIDATE |
| `r5-v29-promotion-recovery` | `a280a6b23095525782b259cbd2b28d371f570138` | A | ARCHIVE_CANDIDATE |
| `release/v0.9.2` | `9c6229485ee622f3e0d825bc20c5adf58fb39d92` | A | ARCHIVE_CANDIDATE |
| `release/v0.9.3` | `2ef3a10a1d2b67a1153ad4996a8e1e39e228d55b` | D | REVIEW_UNIQUE |
| `release/v0.9.4` | `cb7cf5c79d0f4dbe501aa364ed1627a928e5ec5c` | A | RETAIN |
| `staging/exercise-db-r5-v9` | `6d1c564ae9e521e5563967189f04a7266ced8dab` | D | REVIEW_UNIQUE |
| `work/exercise-db-v2-completion` | `45ab284315592a432a1293b4dd06465e26123185` | D | REVIEW_UNIQUE |
| `work/v0.9.4-equipment-location-cleanup` | `79eeb9e2ce94c7dbab5c7d9c19908db9875943c9` | A | ARCHIVE_CANDIDATE |
| `work/v3-r5-candidate-consolidation` | `861cb4b4f84e1af8f232682f0bc73bd680bb847c` | S | RETAIN |

## Gate before any physical retirement

1. Re-fetch heads and ensure the saved SHA has not moved; rerun ancestry comparison against the intended surviving ref.
2. Confirm the ref is not the default branch, open PR head, protected branch, frozen owner acceptance baseline, needed release/checkpoint, R5 V29 recovery, or unique/diverged side branch.
3. Preserve recovery access in a durable tag or approved immutable snapshot **before** deleting any ref. There is no authorized tag/deletion operation in this consolidation step, so **physical retirement is deferred**.
4. For divergences (including early app milestones and release/v0.9.3), inspect their unique commits and either port what is needed or deliberately retain a recovery ref.
5. `work/v3-r5-candidate-consolidation` contains five Oct 10 diagnostics/metadata commits not in PR #15. Its final DB candidate blob is identical to the official V3 branch, but those commits are **not merged** and remain intentionally preserved.

## Objective of archiving

Reduce *active* workstreams to one branch while preserving forensics, not cosmetically reduce GitHub's branch counter by deleting unreviewed work. No claims of `main` release, DB V3 production switch, or device acceptance follow from this inventory.
