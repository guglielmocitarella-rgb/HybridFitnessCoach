# Exercise DB V3.1A — Equipment semantics & location compatibility

Baseline remote HEAD: `cc1aaeff0f7ef8bd6541dfeceaddb59f491b4aae`.

## Finding
The existing builder deliberately encodes the app label **Corpo libero** as `equipmentRequired=[]` with `equipmentStatus=KNOWN`, while unresolved equipment is `equipmentStatus=UNKNOWN`. The runtime, however, rejects every empty requirement set in `locationCompatible()`. Therefore empty-known and unknown must not share runtime behavior.

## 377 empty equipment
- LEGITIMATE_NO_EQUIPMENT: **79**
- BODYWEIGHT: **197**
- ASSISTED_MOVEMENT: **9**
- SEMANTICALLY_AMBIGUOUS: **87**
- MISSING_EQUIPMENT_DATA: **3**
- WEIGHTED_BODYWEIGHT: **2**

## 97 UNKNOWN
- REQUIRES_EXTERNAL_RESEARCH: **87**
- SCHEMA_SEMANTICS_PROBLEM: **5**
- RESOLVABLE_WITH_REVIEW: **3**
- LEGITIMATE_NO_EQUIPMENT: **2**

No UNKNOWN row had HIGH-confidence `equipmentRequired` evidence already present in `external-enrichment.json`; ambiguous record matching was not promoted.

## 18 unsupported
All 18 are TRX identities using the canonical V3 token `suspension_trainer`. V3 already recognizes that token; the current runtime `EquipmentId` does not. Remapping it to bands/rings/rope would lose information. Decision: preserve `suspension_trainer` in V3 and add an equivalent runtime capability only at the later integration stage.

## V3 equipment model decision
`equipmentRequired: List<Equipment>` alone is insufficient. V3 needs an explicit requirement semantic with at least:
- status: KNOWN / UNKNOWN
- mode: NONE / REQUIRED / OPTIONAL / ONE_OF / ASSISTED_BY / EXTERNAL_LOAD
- separate lists/slots for required, alternatives, optional equipment, assistance and external-load modality.

`BODYWEIGHT` is a resistance semantic, not physical equipment. A no-equipment bodyweight exercise should therefore be `mode=NONE` plus `resistanceMode=BODYWEIGHT`, not a fake required equipment token.

## Location compatibility contract
- UNKNOWN: fail closed.
- NONE: equipment does not block location.
- REQUIRED: all required capabilities must exist.
- OPTIONAL: absence does not block.
- ONE_OF: at least one alternative must exist.
- ASSISTED_BY: base support plus a valid assistance alternative.
- EXTERNAL_LOAD: base support plus the encoded external-load capability/modality.

Required simulations are persisted in the JSON audit. Runtime `locationCompatible()` is intentionally unchanged in V3.1A.

## Mutation policy
No runtime DB, Runner or selection-engine code was changed. No AMBIGUOUS match was imported. Canonical exercise fields were not mass-rewritten in this phase; unresolved rows remain explicit remediation items.
