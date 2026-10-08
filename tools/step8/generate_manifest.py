#!/usr/bin/env python3
"""Build deterministic executable Step 8 cases from the frozen V3 candidate."""
import hashlib, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "exercise-db-canonical/exercise-db-v3-final-candidate.json"
EXPECTED_SHA = "0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a"
OUT = ROOT / "build/step8"
PD_CASES = ("NO_CONTEXT", "INCOMPLETE_CONTEXT", "UNKNOWN_ATHLETE", "INVALID_CONTEXT",
            "CAPACITY_MISMATCH", "EQUAL_BOUNDARY", "BELOW_CAPACITY", "ABOVE_CAPACITY")

def pd_case(record, case):
    cid = record["canonicalExerciseId"]
    source = record["resistance"]["source"]
    unit_path = "loadInput" if source == "EXTERNAL_LOAD" else "assistanceInput"
    unit = record["resistance"][unit_path].get("unitSemantics")
    if not unit:
        raise ValueError("PD fixture requires a canonical typed unit: " + cid)
    work = {"reps": None, "durationSeconds": None, "distanceMeters": None, "intervals": None,
            "variation": None, "tempo": None, "rangeOfMotion": None, "laterality": None}
    dimension_fields = {"REPS": "reps", "DURATION": "durationSeconds",
                        "DISTANCE": "distanceMeters", "INTERVALS": "intervals"}
    for dimension in record.get("prescription", {}).get("dimensions", []):
        field = dimension_fields.get(dimension.get("type"))
        if field and dimension.get("value") is not None:
            work[field] = dimension["value"]
    prescription = {"exerciseId": cid, "resistanceSource": source, "work": dict(work),
                    "resistance": {"value": 1.0, "unit": unit}}
    point = {"exerciseId": cid, "resistanceSource": source, "work": dict(work),
             "maximumFeasibleLoad": None, "minimumRequiredAssistance": None}
    if source == "EXTERNAL_LOAD":
        point["maximumFeasibleLoad"] = {"value": 1.0, "unit": unit}
    elif source == "ASSISTED":
        point["minimumRequiredAssistance"] = {"value": 1.0, "unit": unit}
    else:
        raise ValueError("unsupported PD source " + source + " for " + cid)
    context = {"prescription": prescription, "athleteCapacity": {"points": [point]}}
    expected_state, expected_reason = "ELIGIBLE", "PD_STRENGTH_CONTEXT_RESOLVED"
    if case in ("NO_CONTEXT", "INCOMPLETE_CONTEXT"):
        expected_state, expected_reason = "EXERCISE_REQUIREMENT_UNRESOLVED", "PD_STRENGTH_CONTEXT_INSUFFICIENT"
        context = None if case == "NO_CONTEXT" else {"prescription": {
            "exerciseId": cid, "resistanceSource": source, "work": work, "resistance": None},
            "athleteCapacity": None}
    elif case == "UNKNOWN_ATHLETE":
        expected_state, expected_reason = "ATHLETE_CAPABILITY_UNKNOWN", "ATHLETE_CAPABILITY_UNKNOWN"
        context["athleteCapacity"] = None
    elif case == "INVALID_CONTEXT":
        expected_state, expected_reason = "INVALID_PRESCRIPTION_CONTEXT", "PD_STRENGTH_CONTEXT_INVALID"
        context["prescription"]["exerciseId"] = cid + "::wrong"
    elif case == "CAPACITY_MISMATCH":
        expected_state, expected_reason = "EXERCISE_REQUIREMENT_UNRESOLVED", "CAPACITY_ENVELOPE_MISMATCH"
        point["work"]["variation"] = "__step8_capacity_mismatch__"
        if prescription["work"]["variation"] == "__step8_capacity_mismatch__":
            point["work"]["tempo"] = "__step8_capacity_mismatch__"
    elif case in ("BELOW_CAPACITY", "ABOVE_CAPACITY"):
        # Synthetic boundary probes; no probe is written to canonical data.
        limit = 0.0 if case == "BELOW_CAPACITY" else 2.0
        if source == "EXTERNAL_LOAD":
            point["maximumFeasibleLoad"]["value"] = limit
        else:
            point["minimumRequiredAssistance"]["value"] = 2.0 if case == "BELOW_CAPACITY" else 0.0
        expected_state = "INELIGIBLE" if case == "BELOW_CAPACITY" else "ELIGIBLE"
    return {"scenarioId": cid + "::PD::" + case, "canonicalExerciseId": cid,
            "axis": "PD", "case": case, "coverageTags": ["PD", "NATIVE_ENGINE"],
            "context": context, "expectedState": expected_state, "expectedReason": expected_reason}

def main():
    raw = SOURCE.read_bytes()
    actual = hashlib.sha256(raw).hexdigest()
    if actual != EXPECTED_SHA:
        raise SystemExit("frozen candidate SHA mismatch: " + actual)
    records = json.loads(raw)
    ids = [x["canonicalExerciseId"] for x in records]
    if len(records) != 1087 or len(set(ids)) != len(ids):
        raise SystemExit("canonical identity count or uniqueness mismatch")
    pd = [x for x in records if x["capability"]["strengthRequirement"] == "PRESCRIPTION_DEPENDENT"]
    terminal_doc = json.loads((ROOT / "exercise-db-canonical/native-capability-r5-terminal-membership.json").read_text())
    terminal = terminal_doc.get("entries", [])
    external = [x for x in pd if x["resistance"]["source"] == "EXTERNAL_LOAD"]
    assisted = [x for x in pd if x["resistance"]["source"] == "ASSISTED"]
    if (len(pd), len(terminal), len(external), len(assisted)) != (441, 48, 436, 5):
        raise SystemExit("frozen PD/terminal membership inventory mismatch")
    by_id = {x["canonicalExerciseId"]: x for x in records}
    semantic_keys = [x.get("semanticKey") for x in terminal]
    if len(set(semantic_keys)) != 48 or any(not key for key in semantic_keys):
        raise SystemExit("terminal semantic-key membership is incomplete or duplicated")
    for entry in terminal:
        record = by_id.get(entry.get("canonicalExerciseId"))
        dimension = entry.get("dimension")
        if record is None or record["capability"].get(dimension) != "EVIDENCE_UNRESOLVED":
            raise SystemExit("terminal membership disagrees with frozen candidate: " + str(entry))
    rows = []
    for record in sorted(pd, key=lambda x: x["canonicalExerciseId"]):
        rows.extend(pd_case(record, case) for case in PD_CASES)
    for entry in sorted(terminal, key=lambda x: x["semanticKey"]):
        cid = entry["canonicalExerciseId"]
        dimension = entry["dimension"]
        rows.append({"scenarioId": entry["semanticKey"] + "::TERMINAL_WITH_CONTEXT",
                     "semanticKey": entry["semanticKey"], "canonicalExerciseId": cid,
                     "dimension": dimension, "axis": "TERMINAL", "case": "TERMINAL_WITH_CONTEXT",
                     "coverageTags": ["TERMINAL", "NATIVE_ENGINE"],
                     "expectedState": "TERMINAL_CANONICAL_STATE",
                     "expectedReason": "TERMINAL_CANONICAL_STATE"})
    manifest = {"schema": "V3-STEP8-MATRIX-1", "candidateSha256": actual,
                "canonicalCount": len(records), "pdCount": len(pd),
                "externalLoadPdCount": len(external), "assistedPdCount": len(assisted),
                "terminalCount": len(terminal), "pdCasesPerExercise": len(PD_CASES),
                "plannedCount": len(rows), "scenarios": rows}
    payload = json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode()
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "scenario-manifest.json").write_bytes(payload + b"\n")
    inventory = {"schema": "V3-STEP8-INVENTORY-1", "candidateSha256": actual,
                 "canonicalCount": len(records), "pdCount": len(pd),
                 "externalLoadPdCount": len(external), "assistedPdCount": len(assisted),
                 "terminalCount": len(terminal), "plannedNativeScenarios": len(rows),
                 "plannedPdScenarios": len(pd) * len(PD_CASES),
                 "plannedTerminalScenarios": len(terminal),
                 "manifestSha256": hashlib.sha256(payload).hexdigest(),
                 "testCapacityValuesAreSyntheticProbes": True}
    (OUT / "inventory.json").write_text(json.dumps(inventory, indent=2) + "\n")
    print(json.dumps(inventory, indent=2))

if __name__ == "__main__":
    main()
