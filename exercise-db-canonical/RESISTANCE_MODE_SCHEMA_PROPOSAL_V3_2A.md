# Resistance Mode Schema Proposal V3.2A

## Finding
The current scalar model is not sufficient for every canonical exercise. Runtime values are BODYWEIGHT, EXTERNAL_LOAD, ASSISTED and NONE; V3 additionally uses UNKNOWN only as an unresolved audit state.

EXTERNAL_LOAD currently controls a kg load input and ASSISTED controls a kg assistance input. That is safe for kilogram-prescribed weights/machines, but not for elastic-band tension, partner/manual support, hand support, battle-rope inertia, or identities that legitimately vary between bodyweight and loaded forms.

## Proposed V3 separation
Keep resistance source separate from load-entry semantics:

- resistanceSource: BODYWEIGHT | EXTERNAL_MASS | ELASTIC | MACHINE_STACK | CABLE | MANUAL_ASSISTANCE | SUPPORT_ASSISTANCE | IMPLEMENT_INERTIA | NONE | VARIABLE | UNKNOWN
- loadInputMode: KG_LOAD | KG_ASSISTANCE | BAND_LEVEL | NONE | VARIABLE | UNKNOWN
- resistanceMode may remain as a compatibility projection only: BODYWEIGHT / EXTERNAL_LOAD / ASSISTED / NONE.

Equipment remains equipment; MACHINE/CABLE/BAND must not be duplicated as the sole resistanceMode value.

## Migration / engine implications
V3 integration should derive the legacy resistanceMode only when the projection is lossless. Schema-decision-required records remain fail-closed until the engine supports the richer dimensions. No runtime code is changed in V3.2A.

Affected IDs:
- `ex-assisted-leg-raise` — Assisted Leg Raise
- `ex-assisted-pistol-squat` — Assisted Pistol Squat
- `ex-assisted-pull-up` — Assisted Pull-Up
- `ex-dragon-flag-swimmers` — Dragon-Flag Swimmers
- `ex-low-alternating-wave-with-battle-ropes` — Low Alternating Wave with Battle Ropes
- `ex-negative-dragon-flag` — Negative Dragon Flag
- `ex-single-leg-dragon-flag` — Single-Leg Dragon Flag
- `ex-straddle-flag` — Straddle Flag
- `ex-tucked-dragon-flag` — Tucked Dragon Flag
- `ex-tucked-flag` — Tucked Flag
- `ex-vertical-flag` — Vertical Flag
- `ex-vertical-human-flag` — Vertical Human Flag
