# Exercise DB V3.6 — Engine Readiness Consolidation

Source HEAD: `7a813f6fb5b2d227b8a62e70d7961ba4c17c6dfb`

## Decision
**READY_FOR_V3_SCHEMA_CANDIDATE**. This does not mean current-runtime-ready. V3.1–V3.5 evidence is consolidated sufficiently to design V3.7 without false normalization.

## Integrity
Canonical 1087; unique 1087; missing IDs 0; duplicates 0; matrix 1087/1087. Runtime DB modified: **NO**.

## Core finding
Remaining work is not one generic missing-data problem. Equipment, resistance/load input, capability, typed skill relations, category/group recovery mapping, multidimensional prescription and multi-pattern movement require explicit V3 schema/runtime contracts. Safe unresolved values remain fail-closed. Editorial/media is excluded from the engine gate.

## Metrics
- V3-candidate-ready: 990/1087
- Current-runtime-ready under strict parity: 36/1087
- Safe-unresolved records: 1052
- Schema-evolution-required records: 1051
- Patch entries: 92; conflicts: 0
- Consolidated unresolved entries: 1927

## Exact next step
V3.7 — ENGINE SCHEMA CANDIDATE. No runtime integration yet.
