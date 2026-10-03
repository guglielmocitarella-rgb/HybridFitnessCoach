import json
import pathlib
import tempfile
import unittest

from tools.build_exercise_db_canonical import build_database
from tools.validate_exercise_db_canonical import validate_database


ROOT = pathlib.Path(__file__).resolve().parents[1]


class ExerciseDbCanonicalTest(unittest.TestCase):
    def test_reconciles_the_exact_runtime_catalogue_without_collapsing_distinct_ids(self):
        with tempfile.TemporaryDirectory() as temporary:
            result = build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())

        self.assertEqual(result["counts"]["v2_records"], 587)
        self.assertEqual(result["counts"]["current_model_only_source_entries"], 504)
        self.assertEqual(result["counts"]["current_model_duplicates_merged"], 1)
        self.assertEqual(result["counts"]["current_model_only"], 500)
        self.assertEqual(result["counts"]["canonical_records"], 1087)
        self.assertEqual(len({record["id"] for record in records}), 1087)
        by_id = {record["id"]: record for record in records}

        muscle_up = [record for record in records if record["canonicalNameEn"] in {"Muscle-Up", "Normal Muscle-Up"}]
        self.assertEqual({record["id"] for record in muscle_up}, {"ex-muscle-up", "ex-normal-muscle-up"})
        self.assertEqual(len(muscle_up), 2)
        self.assertTrue(all(record["identityResolution"] == "AMBIGUOUS_CURRENT_MODEL_LINK" for record in muscle_up))

        straight_pulldown = [record for record in records if record.get("identityResolution") == "MERGED_EQUIVALENT_LABELS"]
        self.assertEqual(len(straight_pulldown), 1)
        self.assertEqual(set(straight_pulldown[0]["sourceNames"]), {"Straight arm pulldown", "Straight-arm pulldown"})
        self.assertIn("Straight-arm pulldown", straight_pulldown[0]["aliases"])

        for exercise_id, runtime_name in [
            ("ex-one-arm-pull-up", "One arm pull-up"),
            ("ex-one-arm-push-up", "One arm push-up"),
            ("ex-pseudo-planche-push-up", "Pseudo planche push-up"),
        ]:
            self.assertIn(runtime_name, by_id[exercise_id]["runtimeModelNames"])
            self.assertEqual(by_id[exercise_id]["identityResolution"], "MERGED_V2_AND_CURRENT_MODEL")
        self.assertFalse(any(record["id"] in {"cm-one-arm-pull-up", "cm-one-arm-push-up", "cm-pseudo-planche-push-up"} for record in records))

    def test_retains_v2_source_record_content_and_marks_unsupported_legacy_guidance_unknown(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}
        source = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/exercises.json").read_text())
        original = next(record for record in source if record["id"] == "ex-4-point-burpee")
        self.assertEqual(by_id[original["id"]]["descriptionIt"], original["descriptionIt"])
        legacy = next(record for record in records if record.get("identitySource") == "CURRENT_MODEL")
        self.assertEqual(legacy["executionEvidence"], "UNKNOWN")
        self.assertEqual(legacy["instructionCompleteness"], "MISSING")
        self.assertTrue(legacy["fieldProvenance"])

    def test_explicit_equipment_in_exercise_name_resolves_only_unambiguous_items(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        expected = {
            "ex-high-pulley-curl": ["cable"],
            "ex-low-pulley-curl": ["cable"],
            "ex-rings-muscle-up": ["rings"],
            "ex-bench-dip": ["bench"],
            "ex-bench-step": ["bench"],
            "ex-prone-bench-rear-deltoid-raise": ["bench"],
            "ex-bar-pull-up-eccentrics": ["pull_up_bar"],
            "ex-parallel-bar-dip-eccentrics": ["dip_bars"],
            "ex-parallel-bar-jumping-dips": ["dip_bars"],
            "ex-box-headstand-pushup": ["box"],
            "ex-band-assisted-planche": ["resistance_band"],
        }
        for exercise_id, equipment in expected.items():
            self.assertEqual(by_id[exercise_id]["equipmentStatus"], "KNOWN")
            self.assertEqual(by_id[exercise_id]["equipmentRequired"], equipment)
            self.assertEqual(
                by_id[exercise_id]["fieldProvenance"]["equipmentRequired"]["status"],
                "NAME_EXPLICIT_EQUIPMENT_RULE",
            )

        self.assertEqual(by_id["ex-ball-bridge"]["equipmentStatus"], "UNKNOWN")

    def test_broad_movement_types_fill_only_their_unambiguous_top_level_patterns(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        expected = {
            "ex-calf-stretch": ["mobility"],
            "ex-banded-external-rotation": ["mobility"],
            "ex-adductor-foam-rolling": ["recovery"],
            "ex-4-point-burpee": ["conditioning"],
            "ex-jogging-and-running": ["conditioning"],
        }
        for exercise_id, pattern in expected.items():
            self.assertEqual(by_id[exercise_id]["movementPattern"], pattern)
            self.assertEqual(
                by_id[exercise_id]["fieldProvenance"]["movementPattern"]["status"],
                "V2_MOVEMENT_TYPE_RULE",
            )

        self.assertEqual(by_id["ex-front-lever"]["movementPatternStatus"], "UNKNOWN")

    def test_v2_pattern_families_resolve_known_mechanics_and_preserve_mixed_families(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        expected = {
            "ex-barbell-row": ["horizontal-pull"],
            "ex-lat-pulldown-machine": ["vertical-pull"],
            "ex-barbell-standing-shoulder-press": ["vertical-push"],
            "ex-back-squat": ["knee-dominant"],
            "ex-deadlift": ["hinge"],
            "ex-dip": ["vertical-push"],
            "ex-pull-up": ["vertical-pull"],
            "ex-one-arm-rows": ["horizontal-pull"],
            "ex-box-headstand-pushup": ["vertical-push"],
            "ex-wide-grip-push-up": ["horizontal-push"],
            "ex-vertical-chest-press-machine": ["vertical-push"],
        }
        for exercise_id, pattern in expected.items():
            self.assertEqual(by_id[exercise_id]["movementPattern"], pattern)
            self.assertEqual(
                by_id[exercise_id]["fieldProvenance"]["movementPattern"]["status"],
                "V2_FAMILY_PATTERN_RULE",
            )

        self.assertEqual(by_id["ex-front-lever"]["movementPatternStatus"], "UNKNOWN")

    def test_validator_accepts_integrity_but_never_calls_incomplete_evidence_release_ready(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
            sources = json.loads((output / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())

        report = validate_database(records, progressions, sources)
        self.assertEqual(report["errors"], [])
        self.assertFalse(report["releaseReady"])
        self.assertGreater(report["blockers"]["equipment_unknown"], 0)
        self.assertGreater(report["blockers"]["instruction_incomplete"], 0)

        broken = list(records) + [dict(records[0])]
        invalid = validate_database(broken, progressions, sources)
        self.assertTrue(any("duplicate id" in error for error in invalid["errors"]))


if __name__ == "__main__":
    unittest.main()
