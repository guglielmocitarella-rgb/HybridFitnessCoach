#!/usr/bin/env python3
"""Deterministic Step 8B0 inventory/manifest for frozen V3 data.

This produces planning inputs, not a claim that scenarios have been executed.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "exercise-db-canonical/exercise-db-v3-final-candidate.json"
EXPECTED_SHA = "0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a"
OUT = ROOT / "build/step8"
def main():
    raw = SOURCE.read_bytes()
    actual = hashlib.sha256(raw).hexdigest()
    if actual != EXPECTED_SHA:
        raise SystemExit(f"frozen candidate SHA mismatch: {actual}")
    records = json.loads(raw)
    ids = [x["canonicalExerciseId"] for x in records]
    if len(ids) != 1087 or len(set(ids)) != 1087:
        raise SystemExit("canonical identity count or uniqueness mismatch")
    rows = []
    for record in sorted(records, key=lambda x: x["canonicalExerciseId"]):
        cid = record["canonicalExerciseId"]
        strength = record["capability"]["strengthRequirement"]
        source = record["resistance"]["source"]
        classes = ["BASE", "EQUIPMENT_ABSENT", "CATEGORY_MISMATCH", "REPEAT"]
        if strength == "PRESCRIPTION_DEPENDENT":
            classes += ["NO_CONTEXT", "ATHLETE_UNKNOWN", "EQUAL_CAPACITY",
                        "BELOW_CAPACITY", "ABOVE_CAPACITY", "INVALID_CONTEXT", "CONTEXT_ISOLATION"]
        if strength == "EVIDENCE_UNRESOLVED":
            classes += ["TERMINAL_WITH_CONTEXT"]
        for name in classes:
            rows.append({"id": f"{cid}::{name}", "canonicalExerciseId": cid,
                         "class": name, "resistanceSource": source,
                         "strengthState": strength, "status": "PLANNED_NOT_EXECUTED"})
    keys = [r["id"] for r in rows]
    if len(set(keys)) != len(keys):
        raise SystemExit("duplicate scenario identifiers")
    pd = [r for r in records if r["capability"]["strengthRequirement"] == "PRESCRIPTION_DEPENDENT"]
    terminal = [r for r in records if r["capability"]["strengthRequirement"] == "EVIDENCE_UNRESOLVED"]
    if len(pd) != 441 or len(terminal) != 48:
        raise SystemExit(f"unexpected PD/terminal count: {len(pd)}/{len(terminal)}")
    if sum(r["resistance"]["source"] == "EXTERNAL_LOAD" for r in pd) != 436:
        raise SystemExit("unexpected external load count")
    if sum(r["resistance"]["source"] == "ASSISTED" for r in pd) != 5:
        raise SystemExit("unexpected assisted count")
    payload = json.dumps(rows, sort_keys=True, separators=(",", ":")).encode()
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "scenario-manifest.json").write_bytes(payload + b"\n")
    inventory = {
        "candidateSha256": actual, "canonicalCount": len(ids),
        "pdCount": len(pd), "terminalStrengthCount": len(terminal),
        "plannedCount": len(rows), "executedCount": 0,
        "manifestSha256": hashlib.sha256(payload).hexdigest(),
        "limitations": [
            "Planning manifest only; no executable Kotlin fixture binding",
            "No exhaustive cross-axis cardinality or exclusion proof",
            "Terminal count only covers strengthRequirement state",
            "Skill, progression and equipment cross-axis coverage not yet established"
        ]
    }
    (OUT / "inventory.json").write_text(json.dumps(inventory, indent=2) + "\n")
    print(json.dumps(inventory, indent=2))

if __name__ == "__main__":
    main()
