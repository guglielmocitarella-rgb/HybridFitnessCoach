#!/usr/bin/env python3
import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("build_final_canonical_candidate_step6.py")
SPEC = importlib.util.spec_from_file_location("step6_builder", MODULE_PATH)
B = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(B)


def record(eid, value="EVIDENCE_UNRESOLVED"):
    return {
        "canonicalExerciseId": eid,
        "capability": {
            "strengthRequirement": value,
            "technicalDifficulty": "EVIDENCE_UNRESOLVED",
            "mobilityRequirement": "EVIDENCE_UNRESOLVED",
            "balanceCoordinationRequirement": "EVIDENCE_UNRESOLVED",
        },
    }


def base_data():
    return {
        "candidate": [record("ex-a")],
        "patch": [],
        "universe": {"semanticKeys": []},
        "terminal": {"entries": []},
        "true_missing": {"semanticKeys": []},
        "set_validation": {},
        "materialization_validation": {"patch": {"inUniverseParity": "0/0"}},
        "disposition": {"counts": {"precedenceRequiredAfterDisposition": 0}, "decisions": []},
    }


def decision(action, eid="ex-a", field="strengthRequirement", value="LOW"):
    return {
        "semanticKey": f"{eid}::{field}",
        "canonicalExerciseId": eid,
        "field": field,
        "r5Value": value,
        "candidateBuilderAction": action,
    }


class Step6BuilderTests(unittest.TestCase):
    def test_deterministic_r5_application(self):
        d = base_data()
        d["universe"]["semanticKeys"] = ["ex-a::strengthRequirement"]
        d["patch"] = [{"canonicalExerciseId": "ex-a", "field": "strengthRequirement", "after": "HIGH"}]
        a, _ = B.build_candidate(copy.deepcopy(d), False)
        b, _ = B.build_candidate(copy.deepcopy(d), False)
        self.assertEqual(B.serialize_canonical(a), B.serialize_canonical(b))
        self.assertEqual(a[0]["capability"]["strengthRequirement"], "HIGH")

    def test_keep_preserves_prescription_dependent(self):
        d = base_data()
        d["candidate"][0]["capability"]["strengthRequirement"] = "PRESCRIPTION_DEPENDENT"
        d["disposition"]["decisions"] = [decision("KEEP_SEMANTIC_STATE")]
        out, report = B.build_candidate(d, False)
        self.assertEqual(out[0]["capability"]["strengthRequirement"], "PRESCRIPTION_DEPENDENT")
        self.assertEqual(report["prescriptionDependentPreserved"], 1)

    def test_apply_r5_scalar_exact(self):
        d = base_data()
        d["patch"] = [{"canonicalExerciseId": "ex-a", "field": "strengthRequirement", "after": "HIGH"}]
        d["disposition"]["decisions"] = [decision("APPLY_R5_SCALAR", value="HIGH")]
        out, _ = B.build_candidate(d, False)
        self.assertEqual(out[0]["capability"]["strengthRequirement"], "HIGH")

    def test_exclude_does_not_mutate_unrelated_record(self):
        d = base_data()
        before = copy.deepcopy(d["candidate"])
        d["disposition"]["decisions"] = [decision("EXCLUDE", eid="missing")]
        out, report = B.build_candidate(d, False)
        self.assertEqual(out, before)
        self.assertEqual(report["orphanExcluded"], 1)

    def test_ignore_is_true_noop(self):
        d = base_data()
        d["candidate"][0]["capability"]["strengthRequirement"] = "LOW"
        before = copy.deepcopy(d["candidate"])
        d["disposition"]["decisions"] = [decision("IGNORE_AS_ALREADY_APPLIED", value="LOW")]
        out, report = B.build_candidate(d, False)
        self.assertEqual(out, before)
        self.assertEqual(report["redundantNoOp"], 1)

    def test_duplicate_disposition_fails(self):
        d = base_data()
        x = decision("EXCLUDE", eid="missing")
        d["disposition"]["decisions"] = [x, copy.deepcopy(x)]
        with self.assertRaises(B.BuildError):
            B.build_candidate(d, False)

    def test_missing_authoritative_input_fails(self):
        with tempfile.TemporaryDirectory() as td:
            with self.assertRaises(B.BuildError):
                B.load_json(Path(td) / "absent.json")

    def test_unresolved_precedence_fails_repository_contract(self):
        d = base_data()
        d["disposition"]["counts"]["precedenceRequiredAfterDisposition"] = 1
        with self.assertRaises(B.BuildError):
            B._assert_repository_contract(d)

    def test_serialization_order_is_deterministic(self):
        a = [record("ex-z"), record("ex-a")]
        b = [record("ex-a"), record("ex-z")]
        self.assertEqual(B.serialize_canonical(a), B.serialize_canonical(b))


if __name__ == "__main__":
    unittest.main()
