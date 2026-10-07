#!/usr/bin/env python3
"""Build the Step 6 final canonical candidate from already-persisted decisions only.

This builder intentionally performs no research, heuristics, legacy fallback, or
runtime mutation. It fails closed when the persisted Step 5/R5 contracts do not
fully determine the result.
"""
import argparse
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
C = ROOT / "exercise-db-canonical"

DEFAULTS = {
    "candidate": C / "exercise-db-v3-10c-r4-candidate.json",
    "patch": C / "native-capability-r5-patch.json",
    "universe": C / "native-capability-r5-universe.json",
    "terminal": C / "native-capability-r5-terminal-membership.json",
    "true_missing": C / "native-capability-r5-true-missing.json",
    "set_validation": C / "native-capability-r5-set-validation.json",
    "materialization_validation": C / "native-capability-r5-materialization-validation.json",
    "disposition": C / "semantic-disposition-step5.json",
}
CAPABILITY_FIELDS = {
    "strengthRequirement", "technicalDifficulty",
    "mobilityRequirement", "balanceCoordinationRequirement",
}
PD = "PRESCRIPTION_DEPENDENT"
UNRESOLVED = "EVIDENCE_UNRESOLVED"
ACTIONS = {
    "KEEP_SEMANTIC_STATE", "APPLY_R5_SCALAR",
    "EXCLUDE", "IGNORE_AS_ALREADY_APPLIED",
}


class BuildError(RuntimeError):
    pass


def load_json(path):
    path = Path(path)
    if not path.is_file():
        raise BuildError(f"missing authoritative input: {path}")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise BuildError(f"invalid authoritative input {path}: {exc}") from exc


def semantic_key(exercise_id, field):
    return f"{exercise_id}::{field}"


def records_from(candidate):
    if isinstance(candidate, list):
        return candidate
    if isinstance(candidate, dict):
        for key in ("exercises", "records"):
            if isinstance(candidate.get(key), list):
                return candidate[key]
    raise BuildError("candidate must be a list or contain exercises/records list")


def index_unique(items, key_fn, label):
    out = {}
    duplicates = []
    for item in items:
        key = key_fn(item)
        if key in out:
            duplicates.append(key)
        else:
            out[key] = item
    if duplicates:
        raise BuildError(f"duplicate {label}: {sorted(set(duplicates))[:10]}")
    return out


def serialize_canonical(candidate):
    records = records_from(candidate)
    records.sort(key=lambda r: r["canonicalExerciseId"])
    return json.dumps(candidate, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _assert_repository_contract(data):
    candidate_records = records_from(data["candidate"])
    ids = [r.get("canonicalExerciseId") for r in candidate_records]
    if len(candidate_records) != 1087:
        raise BuildError(f"candidate records != 1087: {len(candidate_records)}")
    if len(set(ids)) != 1087 or None in ids:
        raise BuildError("canonical IDs are not exactly 1087 unique non-null IDs")

    v = data["set_validation"]
    expected_counts = {
        "universe": 3835, "resolvedInUniverse": 3787,
        "terminalInUniverse": 48, "trueMissing": 0,
        "authoritativeOutOfUniverse": 214,
    }
    if v.get("counts") != expected_counts:
        raise BuildError(f"R5 set validation mismatch: {v.get('counts')!r}")
    if v.get("result", "PASS") != "PASS":
        raise BuildError("R5 set validation is not PASS")

    mv = data["materialization_validation"]
    if mv.get("result") != "PASS":
        raise BuildError("R5 materialization validation is not PASS")
    if mv.get("patch", {}).get("inUniverseParity") != "3787/3787":
        raise BuildError("materialization parity != 3787/3787")

    universe = data["universe"].get("semanticKeys", [])
    if len(universe) != 3835 or len(set(universe)) != 3835:
        raise BuildError("R5 universe is not exactly 3835 unique semantic keys")
    terminal = data["terminal"].get("entries", [])
    terminal_keys = [e.get("semanticKey") for e in terminal]
    if len(terminal_keys) != 48 or len(set(terminal_keys)) != 48:
        raise BuildError("terminal membership is not exactly 48 unique keys")
    missing = data["true_missing"].get("semanticKeys", [])
    if missing or data["true_missing"].get("totalSemanticKeys") != 0:
        raise BuildError("TRUE_MISSING is not zero")

    disp = data["disposition"]
    counts = disp.get("counts", {})
    if counts.get("precedenceRequiredAfterDisposition") != 0:
        raise BuildError("unresolved precedence remains")
    if counts.get("globalPrescriptionDependent") != 441:
        raise BuildError("Step 5 global PRESCRIPTION_DEPENDENT count != 441")
    decisions = disp.get("decisions", [])
    if len(decisions) != 214:
        raise BuildError("Step 5 disposition must contain exactly 214 decisions")


def build_candidate(data, enforce_repository_contract=True):
    if enforce_repository_contract:
        _assert_repository_contract(data)

    candidate = copy.deepcopy(data["candidate"])
    records = records_from(candidate)
    by_id = index_unique(records, lambda r: r.get("canonicalExerciseId"), "canonicalExerciseId")

    universe = set(data["universe"].get("semanticKeys", []))
    terminal_entries = data["terminal"].get("entries", [])
    terminal_keys = {e["semanticKey"] for e in terminal_entries}
    true_missing = set(data["true_missing"].get("semanticKeys", []))

    patch = data["patch"]
    if not isinstance(patch, list):
        raise BuildError("R5 patch must be a list")
    patch_by_key = index_unique(
        patch,
        lambda p: semantic_key(p.get("canonicalExerciseId"), p.get("field")),
        "R5 patch semantic key",
    )

    decisions = data["disposition"].get("decisions", [])
    decision_by_key = index_unique(
        decisions, lambda d: d.get("semanticKey"), "semantic disposition"
    )
    for d in decisions:
        action = d.get("candidateBuilderAction")
        if action not in ACTIONS:
            raise BuildError(f"unsupported/non-disposed action for {d.get('semanticKey')}: {action}")

    resolved_keys = universe - terminal_keys - true_missing
    missing_materialization = sorted(k for k in resolved_keys if k not in patch_by_key)
    if missing_materialization:
        raise BuildError(f"missing authoritative R5 materialization: {missing_materialization[:10]}")

    report = {
        "sourceCandidateRecords": len(records),
        "outputRecords": None,
        "canonicalIdsUnique": len(by_id),
        "duplicateIds": 0,
        "r5ResolvedApplied": 0,
        "prescriptionDependentSource": 0,
        "prescriptionDependentPreserved": 0,
        "prescriptionDependentDropped": 0,
        "prescriptionDependentKeepActions": 0,
        "accidentallyScalarized": 0,
        "terminalAccounted": 0,
        "orphanExcluded": 0,
        "redundantNoOp": 0,
        "unresolvedPrecedence": data["disposition"].get("counts", {}).get(
            "precedenceRequiredAfterDisposition"
        ),
        "trueMissing": len(true_missing),
        "materializationParity": data["materialization_validation"].get("patch", {}).get(
            "inUniverseParity"
        ),
        "buildConflictsErrors": [],
    }

    source_pd = set()
    for r in records:
        cap = r.get("capability")
        if not isinstance(cap, dict):
            raise BuildError(f"missing capability object: {r.get('canonicalExerciseId')}")
        for field in CAPABILITY_FIELDS:
            if cap.get(field) == PD:
                source_pd.add(semantic_key(r["canonicalExerciseId"], field))
    report["prescriptionDependentSource"] = len(source_pd)

    # Apply exactly the resolved R5 universe. No out-of-universe patch entry is
    # allowed through this path.
    for key in sorted(resolved_keys):
        p = patch_by_key[key]
        eid, field = key.split("::", 1)
        if field not in CAPABILITY_FIELDS:
            raise BuildError(f"unsupported capability field in R5 universe: {key}")
        record = by_id.get(eid)
        if record is None:
            raise BuildError(f"R5 universe target absent from candidate: {key}")
        if key in decision_by_key:
            raise BuildError(f"in-universe key unexpectedly has Step 5 disposition: {key}")
        record["capability"][field] = p["after"]
        report["r5ResolvedApplied"] += 1

    # Apply only the four persisted Step 5 actions for out-of-universe state.
    for key in sorted(decision_by_key):
        d = decision_by_key[key]
        action = d["candidateBuilderAction"]
        eid, field = key.split("::", 1)
        record = by_id.get(eid)
        if action == "EXCLUDE":
            report["orphanExcluded"] += 1
            continue
        if record is None:
            raise BuildError(f"{action} target absent from candidate: {key}")
        if field not in CAPABILITY_FIELDS:
            raise BuildError(f"unsupported disposition field: {key}")
        if action == "KEEP_SEMANTIC_STATE":
            if record["capability"].get(field) != PD:
                raise BuildError(f"KEEP_SEMANTIC_STATE target is not PRESCRIPTION_DEPENDENT: {key}")
            report["prescriptionDependentKeepActions"] += 1
        elif action == "APPLY_R5_SCALAR":
            p = patch_by_key.get(key)
            if p is None:
                raise BuildError(f"APPLY_R5_SCALAR missing authoritative R5 patch: {key}")
            if p.get("after") != d.get("r5Value"):
                raise BuildError(f"R5 scalar disagreement between patch/disposition: {key}")
            record["capability"][field] = p["after"]
        elif action == "IGNORE_AS_ALREADY_APPLIED":
            # A true no-op: require the candidate already to equal the persisted value.
            expected = d.get("r5Value")
            if record["capability"].get(field) != expected:
                raise BuildError(f"IGNORE_AS_ALREADY_APPLIED is not already applied: {key}")
            report["redundantNoOp"] += 1

    # Terminal policy is existing-model preservation: membership is authoritative,
    # and the final candidate retains the unresolved representation already present.
    for e in terminal_entries:
        key = e["semanticKey"]
        eid, field = key.split("::", 1)
        record = by_id.get(eid)
        if record is None:
            raise BuildError(f"terminal target absent from candidate: {key}")
        if record["capability"].get(field) != UNRESOLVED:
            raise BuildError(f"terminal key would require a new representation: {key}")
        report["terminalAccounted"] += 1

    output_pd = set()
    for r in records:
        for field in CAPABILITY_FIELDS:
            if r["capability"].get(field) == PD:
                output_pd.add(semantic_key(r["canonicalExerciseId"], field))
    report["prescriptionDependentPreserved"] = len(source_pd & output_pd)
    report["prescriptionDependentDropped"] = len(source_pd - output_pd)
    report["accidentallyScalarized"] = len(
        (source_pd - output_pd) & {
            k for k, d in decision_by_key.items()
            if d["candidateBuilderAction"] == "KEEP_SEMANTIC_STATE"
        }
    )
    report["outputRecords"] = len(records)
    report["canonicalIdsUnique"] = len({r["canonicalExerciseId"] for r in records})

    if enforce_repository_contract:
        required = {
            "sourceCandidateRecords": 1087,
            "outputRecords": 1087,
            "canonicalIdsUnique": 1087,
            "duplicateIds": 0,
            "r5ResolvedApplied": 3787,
            "prescriptionDependentSource": 441,
            "prescriptionDependentDropped": 0,
            "accidentallyScalarized": 0,
            "terminalAccounted": 48,
            "orphanExcluded": 148,
            "redundantNoOp": 2,
            "unresolvedPrecedence": 0,
            "trueMissing": 0,
            "materializationParity": "3787/3787",
        }
        bad = {k: (report.get(k), v) for k, v in required.items() if report.get(k) != v}
        if bad:
            raise BuildError(f"hard assertions failed: {bad}")
    return candidate, report


def load_authoritative(paths=None):
    paths = paths or DEFAULTS
    return {name: load_json(path) for name, path in paths.items()}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", required=True, type=Path)
    ap.add_argument("--report", required=True, type=Path)
    args = ap.parse_args()
    try:
        data = load_authoritative()
        candidate, report = build_candidate(data, enforce_repository_contract=True)
        payload = serialize_canonical(candidate)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    except BuildError as exc:
        raise SystemExit(f"STEP_6_BUILD_FAILED: {exc}") from exc


if __name__ == "__main__":
    main()
