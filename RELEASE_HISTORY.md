# Hybrid Fitness Coach — Release / Build History

This file is the compact release ledger. GitHub commits and workflow runs remain the source of truth.

| Line | Status | Frozen / verified reference | CI evidence | Notes |
|---|---|---|---|---|
| v0.8 evolved | Historical | repository history | historical Android CI | Universal runner, structured sessions, persistence/recovery foundations. |
| v0.9.0 | Historical / closed | repository history | historical CI | Progression, sequencing and Adaptive Coach development line. |
| v0.9.1 | Historical / closed | repository history / PR #8 audit trail | historical RC2 CI | Multi-week plan, recovery, exercise detail/progression and persistence work. |
| v0.9.2 | Frozen | `release/v0.9.2` at `9c6229485ee622f3e0d825bc20c5adf58fb39d92` | green checkpoint recorded in release history | Closed predecessor baseline. |
| v0.9.3 | Frozen | `release/v0.9.3`; validated production-code checkpoint `365c07f643065df9dd68310362edd3ad3ed3236f` | Exercise DB v2 #250: success; Android APK #802: success | Repository acceptance complete. Interaction/device acceptance transfers to v0.9.4. |
| v0.9.4 | Active | `feature/v0.9.4`; verified checkpoint `0735f62152221299cbdc894eb023456f685c05fa` | Exercise DB v2 #273: success; Android APK #826: success | F03/F04 equipment/location cleanup and F02/F06 propagation cleanup verified at repository level. Device acceptance and remaining F01–F11 work continue. |

## Release discipline

Frozen release branches are references, not development branches. New behavior, fixes discovered during device acceptance, schema changes and UX changes go to the current feature branch. A green CI build is necessary but does not substitute for device acceptance where interaction, lifecycle, timers, Health Connect or visual behavior is involved.

## v0.9.3 acceptance boundary

Repository-level contracts and deterministic tests are recorded in `V0_9_3_DOD.md`. Exercise DB v2 #250 and Android APK #802 passed on the final production-code checkpoint. Documentation-only closure commits followed; they do not change app runtime behavior. The signed APK artifact from the final frozen line is the baseline used to begin v0.9.4 device acceptance.


## v0.9.4 branch naming correction

The active line is `feature/v0.9.4`. The historical `feature/v0.9.1-training-plan` ref reached the same verified checkpoint during development but is no longer the canonical development branch. The previously divergent experimental v0.9.4 head `982653691f18f3b2313a2289a761b3126fec9dc4` is preserved on `archive/v0.9.4-pre-f02-f06-audit` for traceability.
