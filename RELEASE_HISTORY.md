# Hybrid Fitness Coach — Release History

Git commits and workflow runs are the authoritative evidence. This file is deliberately compact; detailed old DoDs remain historical records and are not active instructions.

| Release | Status | Reference | Notes |
|---|---|---|---|
| v0.2 | Historical | repository history | Early interactive prototype. |
| v0.8 evolved | Historical | repository history | Universal runner, structured sessions, persistence/recovery foundations. |
| v0.9.0 | Historical / closed | repository history | Progression, sequencing and Adaptive Coach development. |
| v0.9.1 | Historical / closed | PR #8 audit trail | Multi-week planning, recovery, exercise detail/progression and persistence work. |
| v0.9.2 | Frozen | `release/v0.9.2` | Closed predecessor baseline. |
| v0.9.3 | Frozen | `release/v0.9.3`; production checkpoint `365c07f643065df9dd68310362edd3ad3ed3236f` | Exercise DB #250 and Android APK #802 green. |
| v0.9.4 | Frozen | runtime `cfacd7e3cc06a1db4ee4f8ec5f0518474e52ce07`; `checkpoint/v0.9.4-f01-f11-green` | Exercise DB #292 and Android APK #845 green. Signed/device acceptance transferred to v0.9.5. |
| v0.9.5 | Active | `feature/v0.9.5`, PR #13 | Product/UX consolidation and multi-plan development. Current candidate is not yet CI-green. |

## Branch discipline

- Frozen release branches are references, not development branches.
- Current product changes go to `feature/v0.9.5`.
- Old `feature/v0.9.1-training-plan`, `feature/v0.9.3`, isolated Exercise DB workstreams and experimental/archive branches are historical references only.
- No backward-compatibility layer is required solely to preserve obsolete app schemas or behavior.
- Device acceptance and signed-release evidence must be recorded against the exact candidate SHA.

## PR ledger

Closed as historical housekeeping on 2026-10-01: #1, #8, #9, #10, #12.  
Already closed: #11 (v0.9.4).  
Active: #13 (v0.9.5).
