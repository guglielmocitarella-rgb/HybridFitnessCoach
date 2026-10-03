import json
import pathlib
import tempfile
import unittest

from tools.build_exercise_db_canonical import _skill_nodes, build_database, norm, parse_runtime_catalogue
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

    def test_preserves_runtime_application_metadata_and_its_field_provenance(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        carry = by_id["cm-suitcase-carry"]["applicationMetadata"]["sourceEntries"][0]
        self.assertEqual(carry["snapshot"]["measure"], "DISTANCE")
        self.assertEqual(carry["snapshot"]["targetSets"], 3)
        self.assertEqual(carry["snapshot"]["targetValue"], 30.0)
        self.assertEqual(carry["snapshot"]["movementPattern"], "loaded-carry")
        self.assertEqual(carry["fieldProvenance"]["targetValue"]["status"], "APP_MODEL_ENTRY")
        self.assertEqual(carry["source"]["source_id"], "APP_MODEL_9B8A41C")

        one_arm_pull_up = by_id["ex-one-arm-pull-up"]["applicationMetadata"]["sourceEntries"][0]
        self.assertEqual(one_arm_pull_up["snapshot"]["equipmentLabel"], "Sbarra")
        self.assertEqual(one_arm_pull_up["snapshot"]["technicalComplexity"], 3)
        self.assertEqual(
            by_id["ex-one-arm-pull-up"]["fieldProvenance"]["movementPattern"]["source"]["source_id"],
            "APP_EXERCISE_METADATA",
        )
        self.assertEqual(
            by_id["ex-one-arm-pull-up"]["fieldProvenance"]["skillIds"]["source"]["source_id"],
            "APP_SKILL_CATALOG",
        )

        parallel_dip = next(
            record["applicationMetadata"]["sourceEntries"][0]
            for record in records
            if record.get("canonicalNameEn") == "Dip alle parallele"
        )
        self.assertEqual(parallel_dip["snapshot"]["secondaryGroups"], ["Tricipiti"])
        self.assertEqual(parallel_dip["fieldProvenance"]["secondaryGroups"]["status"], "APP_EXERCISE_METADATA_RULE")

    def test_applies_external_ace_equipment_and_level_enrichment_with_per_field_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            result = build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
        by_id = {record["id"]: record for record in records}
        expected = {
            "ex-bear-crawls": ([], "Intermedio"),
            "ex-bent-knee-push-up": ([], "Principiante"),
            "ex-bodyweightair-squat": ([], "Principiante"),
            "ex-childs-pose": ([], "Principiante"),
            "ex-cobra-stretch": ([], "Principiante"),
            "ex-contralateral-limb-raises": ([], "Principiante"),
            "ex-bodyweight-crunches": ([], "Principiante"),
            "ex-front-plank": ([], "Intermedio"),
            "ex-forward-step-lunge": ([], "Intermedio"),
            "ex-mountain-climbers": ([], "Avanzato"),
            "ex-superman": ([], "Principiante"),
            "ex-tuck-jump": ([], "Intermedio"),
            "ex-deadbug": ([], "Principiante"),
            "ex-hammer-curl": (["dumbbell"], "Intermedio"),
            "ex-inchworm": ([], "Avanzato"),
            "ex-kneeling-iliopsoas-stretch": ([], "Principiante"),
            "ex-prone-walkout": (["stability_ball"], "Intermedio"),
            "ex-reverse-wrist-curl": (["bench", "dumbbell"], "Principiante"),
            "ex-single-leg-squat": ([], "Intermedio"),
            "ex-standing-triceps-stretch": ([], "Principiante"),
            "ex-triceps-kickback": (["dumbbell"], "Principiante"),
            "ex-wrist-curl": (["bench", "dumbbell"], "Principiante"),
            "ex-jumping-squat": ([], "Principiante"),
            "ex-standing-jump-and-reach": ([], "Intermedio"),
            "ex-plank-to-push-up": ([], "Intermedio"),
            "ex-high-to-low-plank": ([], "Intermedio"),
            "ex-standing-chest-and-anterior-shoulder-stretch": ([], "Principiante"),
            "ex-single-leg-deadlift": ([], "Avanzato"),
            "ex-calf-stretch": ([], "Principiante"),
            "ex-supine-lying-deep-external-rotators-stretch": ([], "Principiante"),
            "ex-quadruped-leg-lift": ([], "Principiante"),
            "ex-hip-flexor-stretch": ([], "Principiante"),
        }
        self.assertIn("ACE_EXERCISE_LIBRARY", {source["source_id"] for source in sources})
        for exercise_id, (equipment, level) in expected.items():
            record = by_id[exercise_id]
            self.assertEqual(record["equipmentRequired"], equipment)
            self.assertEqual(record["equipmentStatus"], "KNOWN")
            self.assertEqual(record["level"], level)
            self.assertEqual(record["levelStatus"], "KNOWN")
            for field in ("equipmentRequired", "level"):
                provenance = record["fieldProvenance"][field]
                self.assertEqual(provenance["status"], "EXTERNAL_SOURCE_DIRECT")
                self.assertEqual(provenance["source"]["source_id"], "ACE_EXERCISE_LIBRARY")
                self.assertTrue(provenance["source"]["locator"].startswith("https://www.acefitness.org/"))
                self.assertTrue(provenance["source"]["evidence_type"])
        plank = by_id["ex-front-plank"]
        self.assertIn("avambracci", plank["setupIt"])
        self.assertGreaterEqual(len(plank["executionStepsIt"]), 3)
        self.assertEqual(plank["fieldProvenance"]["executionStepsIt"]["status"], "EXTERNAL_SOURCE_DIRECT")
        self.assertEqual(plank["fieldProvenance"]["instructions"]["source"]["source_id"], "ACE_EXERCISE_LIBRARY")
        self.assertEqual(result["externalEnrichmentAudit"]["fields_applied"]["executionStepsIt"], 31)
        self.assertEqual(len(result["externalEnrichmentAudit"]["unresolved_conflicts"]), 1)

    def test_validator_rejects_external_field_provenance_without_url_or_confidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        record = next(item for item in records if item["id"] == "ex-bent-knee-push-up")
        record["fieldProvenance"]["equipmentRequired"]["source"]["locator"] = ""
        record["fieldProvenance"]["level"].pop("confidence")
        report = validate_database(records, progressions, sources)
        self.assertTrue(any("external field provenance lacks a source URL" in error for error in report["errors"]))
        self.assertTrue(any("external field provenance lacks a valid confidence" in error for error in report["errors"]))

    def test_validator_rejects_unregistered_runtime_metadata_provenance(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        broken = [dict(record) for record in records]
        source_entry = next(record["applicationMetadata"]["sourceEntries"] for record in broken if record.get("applicationMetadata", {}).get("sourceEntries"))
        source_entry[0]["fieldProvenance"]["technicalComplexity"]["source_id"] = "UNREGISTERED_METADATA_RULE"

        report = validate_database(broken, progressions, sources)
        self.assertTrue(any("unregistered runtime metadata source" in error for error in report["errors"]))

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

        self.assertEqual(by_id["ex-ball-bridge"]["equipmentStatus"], "KNOWN")
        self.assertEqual(by_id["ex-ball-bridge"]["equipmentRequired"], ["stability_ball"])
        self.assertEqual(by_id["ex-ball-bridge"]["fieldProvenance"]["equipmentRequired"]["status"], "EXTERNAL_SOURCE_DIRECT")

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
            "ex-abdominal-crunch": ["core-control"],
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

    def test_skill_links_include_active_legacy_skill_catalog_nodes_by_stable_id(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        self.assertIn("front_tuck", by_id["ex-tuck-front-lever"]["skillIds"])
        self.assertIn("front_full", by_id["ex-full-front-lever"]["skillIds"])
        self.assertIn("planche_adv_tuck", by_id["ex-advanced-tuck-planche"]["skillIds"])
        self.assertIn("backlever_full", by_id["ex-back-lever"]["skillIds"])
        self.assertNotIn("back_5", by_id["ex-back-lever"]["skillIds"])

    def test_skill_catalog_positional_and_alias_map_names_match_real_runtime_exercises(self):
        nodes = _skill_nodes(ROOT)
        by_id = {node["id"]: node for node in nodes}
        runtime_names = {norm(entry["name"]) for entry in parse_runtime_catalogue(ROOT)}
        expected = {
            "muscleup_negative": "Muscle-up negativo",
            "handstand_wall_shoulder_tap": "Handstand shoulder taps",
            "handstand_freestanding": "Freestanding handstand hold",
            "backlever_tuck": "Back lever tuck",
            "backlever_advanced_tuck": "Back lever advanced tuck",
            "flag_tuck": "Human flag tuck",
            "vsit_compression": "Compression lift",
            "pistol_assisted": "Pistol squat assistito",
            "pull_3": "Chest to bar pull-up",
            "dip_2": "Dip alle parallele",
            "hspu_3": "Wall handstand push-up",
            "dragon_2": "Dragon flag negativa",
            "shrimp_2": "Shrimp squat assistito",
        }
        for node_id, exercise_name in expected.items():
            node = by_id[node_id]
            self.assertIn(norm(exercise_name), runtime_names)
            self.assertIn(norm(exercise_name), {norm(node["exerciseName"]), *(norm(a) for a in node["aliases"])})
        for node in nodes:
            eligible_names = {norm(node["exerciseName"]), *(norm(alias) for alias in node["aliases"])}
            self.assertTrue(eligible_names & runtime_names, f'{node["id"]} has no runtime exercise label or alias')

    def test_ambiguous_muscle_up_runtime_alias_does_not_copy_selection_metadata(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}

        for exercise_id in ("ex-muscle-up", "ex-normal-muscle-up"):
            record = by_id[exercise_id]
            self.assertEqual(record["identityResolution"], "AMBIGUOUS_CURRENT_MODEL_LINK")
            self.assertEqual(record["equipmentStatus"], "KNOWN")
            self.assertEqual(record["equipmentRequired"], ["rings"])
            self.assertEqual(record["fieldProvenance"]["equipmentRequired"]["status"], "EXTERNAL_SOURCE_DIRECT")
            self.assertEqual(record["levelStatus"], "UNKNOWN")
            self.assertEqual(record["movementPatternStatus"], "UNKNOWN")
            self.assertEqual(record["resistanceMode"], "UNKNOWN")
            self.assertEqual(record["applicationMetadata"]["sourceEntries"], [])
            self.assertEqual(len(record["applicationMetadata"]["ambiguousSourceEntries"]), 1)
        self.assertEqual(by_id["ex-muscle-up"]["runtimeName"], "Muscle-Up")
        self.assertEqual(by_id["ex-normal-muscle-up"]["runtimeName"], "Normal Muscle-Up")
        self.assertNotEqual(by_id["ex-muscle-up"]["runtimeName"], by_id["ex-normal-muscle-up"]["runtimeName"])

    def test_validator_rejects_skill_ids_missing_from_the_active_skill_catalog(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        broken = [dict(record) for record in records]
        exercise = next(record for record in broken if record.get("skillIds"))
        exercise["skillIds"] = ["skill_id_that_does_not_exist"]

        report = validate_database(broken, progressions, sources)
        self.assertTrue(any("unknown SkillCatalog ID" in error for error in report["errors"]))

    def test_validator_rejects_external_load_without_explicit_external_equipment(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        broken = [dict(record) for record in records]
        exercise = next(record for record in broken if record["id"] == "ex-front-lever")
        exercise["resistanceMode"] = "EXTERNAL_LOAD"

        report = validate_database(broken, progressions, sources)
        self.assertTrue(any("EXTERNAL_LOAD" in error and "equipment" in error for error in report["errors"]))

    def test_validator_blocks_equipment_tokens_not_supported_by_the_app_catalog(self):
        with tempfile.TemporaryDirectory() as temporary:
            build_database(ROOT, pathlib.Path(temporary))
            records = json.loads((pathlib.Path(temporary) / "exercise-db-canonical.json").read_text())
            sources = json.loads((pathlib.Path(temporary) / "sources.json").read_text())
            progressions = json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        exercise = next(record for record in records if record["id"] == "ex-front-lever")
        exercise["equipmentRequired"] = ["suspension_trainer"]
        exercise["equipmentStatus"] = "KNOWN"

        report = validate_database(records, progressions, sources)
        self.assertGreater(report["blockers"]["equipment_unsupported_by_app"], 0)
        self.assertFalse(report["releaseReady"])

    def test_resolves_high_confidence_external_load_after_equipment_enrichment(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
        by_id = {record["id"]: record for record in records}
        expected_ids = {
            "ex-back-extension-apparatus", "ex-glute-ham-raise", "ex-hammer-curl",
            "ex-hang-power-clean", "ex-hang-power-snatch", "ex-lateral-shoulder-raise",
            "ex-leg-press-calf-raise", "ex-muscle-snatch", "ex-neutral-grip-vertical-pulldown",
            "ex-one-arm-reverse-pushdown", "ex-overhead-squat", "ex-power-snatch",
            "ex-push-jerk", "ex-push-presspush-jerk", "ex-pushdown", "ex-reverse-pushdown",
            "ex-reverse-wrist-curl", "ex-split-jerk", "ex-squat-push-press",
            "ex-standing-unilateral-leg-curl", "ex-stiff-leg-deadlift",
            "ex-sumo-dead-lift-high-pull", "ex-trap-bar-deadlift", "ex-triceps-kickback",
            "ex-unilateral-cuffed-leg-extension", "ex-unilateral-leg-extension",
            "ex-upright-row", "ex-wide-grip-vertical-pulldown", "ex-wrist-curl",
        }
        self.assertEqual(len(expected_ids), 29)
        for exercise_id in expected_ids:
            record = by_id[exercise_id]
            self.assertEqual(record["resistanceMode"], "EXTERNAL_LOAD")
            provenance = record["fieldProvenance"]["resistanceMode"]
            self.assertEqual(provenance["status"], "EQUIPMENT_DETERMINISTIC")
            self.assertEqual(provenance["confidence"], "HIGH")
            self.assertEqual(provenance["source"]["source_id"], "V2_EQUIPMENT_RESISTANCE_RULE")

    def test_resolves_reviewed_self_loaded_families_as_bodyweight(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
            candidates = json.loads((output / "RESISTANCE_MODE_BODYWEIGHT_CANDIDATES.json").read_text())
        by_id = {record["id"]: record for record in records}
        self.assertEqual(candidates["screenedResistanceUnknown"], 309)
        self.assertEqual(candidates["count"], 95)
        self.assertLessEqual(result["counts"]["resistance_mode_unknown"], 187)
        candidate_ids = [item["id"] for item in candidates["candidates"]]
        self.assertEqual(candidate_ids, sorted(candidate_ids))
        self.assertEqual(len(candidate_ids), len(set(candidate_ids)))
        for item in candidates["candidates"]:
            self.assertEqual(item["confidence"], "HIGH")
            self.assertTrue(all(item["exclusionsVerified"].values()))
            record = by_id[item["id"]]
            self.assertEqual(record["resistanceMode"], "BODYWEIGHT")
            provenance = record["fieldProvenance"]["resistanceMode"]
            self.assertEqual(provenance["status"], "EQUIPMENT_FAMILY_DETERMINISTIC")
            self.assertEqual(provenance["confidence"], "HIGH")
            self.assertEqual(provenance["source"]["source_id"], "V2_EQUIPMENT_RESISTANCE_RULE")

        for excluded_id in ("ex-assisted-dip", "ex-weighted-dip", "ex-banded-biceps-curl", "ex-muscle-up", "ex-normal-muscle-up"):
            self.assertNotEqual(by_id[excluded_id]["resistanceMode"], "BODYWEIGHT")

    def test_resolves_non_resistance_mobility_and_smr_as_none(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
            candidates = json.loads((output / "RESISTANCE_MODE_NONE_CANDIDATES.json").read_text())
        by_id = {record["id"]: record for record in records}
        self.assertEqual(candidates["screenedResistanceUnknown"], 214)
        self.assertGreater(candidates["count"], 0)
        candidate_ids = [item["id"] for item in candidates["candidates"]]
        self.assertEqual(candidate_ids, sorted(candidate_ids))
        self.assertEqual(len(candidate_ids), len(set(candidate_ids)))
        for item in candidates["candidates"]:
            self.assertEqual(item["candidateResistanceMode"], "NONE")
            self.assertEqual(item["confidence"], "HIGH")
            self.assertTrue(all(item["exclusionsVerified"].values()))
            record = by_id[item["id"]]
            self.assertEqual(record["resistanceMode"], "NONE")
            provenance = record["fieldProvenance"]["resistanceMode"]
            self.assertEqual(provenance["status"], "MOBILITY_SEMANTICS_DETERMINISTIC")
            self.assertEqual(provenance["confidence"], "HIGH")
        self.assertEqual(by_id["ex-adductor-foam-rolling"]["resistanceMode"], "NONE")
        for excluded_id in ("ex-ab-wheel-eccentrics", "ex-alternate-arm-and-leg-raises-on-ball"):
            self.assertNotEqual(by_id[excluded_id]["resistanceMode"], "NONE")
        self.assertLessEqual(result["counts"]["resistance_mode_unknown"], 214 - candidates["count"])

    def test_resolves_general_core_conditioning_self_loaded_as_bodyweight(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
            candidates = json.loads((output / "RESISTANCE_MODE_BODYWEIGHT_GENERAL_CANDIDATES.json").read_text())
        by_id = {record["id"]: record for record in records}
        self.assertEqual(candidates["screenedResistanceUnknown"], 187)
        self.assertGreaterEqual(candidates["count"], 20)
        self.assertLessEqual(candidates["count"], 50)
        candidate_ids = [item["id"] for item in candidates["candidates"]]
        self.assertEqual(candidate_ids, sorted(candidate_ids))
        self.assertEqual(len(candidate_ids), len(set(candidate_ids)))
        for item in candidates["candidates"]:
            self.assertEqual(item["confidence"], "HIGH")
            self.assertTrue(all(item["exclusionsVerified"].values()))
            record = by_id[item["id"]]
            self.assertEqual(record["resistanceMode"], "BODYWEIGHT")
            provenance = record["fieldProvenance"]["resistanceMode"]
            self.assertEqual(provenance["status"], "GENERAL_SELF_LOADED_DETERMINISTIC")
            self.assertEqual(provenance["confidence"], "HIGH")
        for expected_id in ("ex-deadbug", "ex-front-plank", "ex-mountain-climbers", "ex-bear-crawls", "ex-quadruped-leg-lift"):
            self.assertIn(expected_id, candidate_ids)
        for excluded_id in ("ex-ab-wheel-eccentrics", "ex-alternate-arm-and-leg-raises-on-ball", "ex-prone-walkout", "ex-weighted-dip"):
            self.assertNotEqual(by_id[excluded_id]["resistanceMode"], "BODYWEIGHT")
        self.assertEqual(result["counts"]["resistance_mode_unknown"], 187 - candidates["count"])

    def test_record_first_band_batch_keeps_field_level_evidence_independent(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            records = json.loads((output / "exercise-db-canonical.json").read_text())
            batch = json.loads((output / "RECORD_FIRST_ENRICHMENT_BATCH_01.json").read_text())
        by_id = {record["id"]: record for record in records}
        self.assertEqual(batch["strategy"], "RECORD_FIRST_ENRICHMENT")
        self.assertEqual(batch["screenedResistanceUnknown"], 165)
        self.assertEqual(batch["count"], 15)
        self.assertEqual(batch["summary"]["resistanceModeResolved"], 15)
        self.assertGreater(batch["summary"]["movementPatternResolved"], 0)
        self.assertEqual(result["counts"]["resistance_mode_unknown"], 150)
        for item in batch["records"]:
            record = by_id[item["id"]]
            self.assertEqual(record["equipmentRequired"], ["resistance_band"])
            self.assertEqual(record["resistanceMode"], "EXTERNAL_LOAD")
            resistance_prov = record["fieldProvenance"]["resistanceMode"]
            self.assertEqual(resistance_prov["confidence"], "HIGH")
            self.assertEqual(resistance_prov["status"], "EQUIPMENT_DETERMINISTIC")
            # Level and instructions must not be promoted merely because
            # resistance mode has high-confidence evidence.
            if "level" in item["missingFieldsBefore"]:
                self.assertIn("level", item["remainingIncompleteFields"])
            for field in ("descriptionIt", "setupIt", "executionStepsIt", "techniqueCuesIt", "commonErrorsIt"):
                if field in item["missingFieldsBefore"]:
                    self.assertIn(field, item["remainingIncompleteFields"])
            for resolved in item["resolvedFields"]:
                self.assertEqual(resolved["confidence"], "HIGH")
        self.assertEqual(by_id["ex-banded-biceps-curl"]["movementPattern"], ["pull-accessory"])
        self.assertEqual(by_id["ex-banded-biceps-curl"]["fieldProvenance"]["movementPattern"]["status"], "REVIEWED_CANONICAL_MECHANICS_RULE")
        self.assertIsNone(by_id["ex-banded-biceps-curl"]["level"])

    def test_generates_deterministic_resistance_unknown_operational_report(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = pathlib.Path(temporary)
            result = build_database(ROOT, output)
            report_path = output / "RESISTANCE_MODE_UNKNOWN.json"
            first_bytes = report_path.read_bytes()
            operational = json.loads(first_bytes)
            validation = validate_database(
                json.loads((output / "exercise-db-canonical.json").read_text()),
                json.loads((ROOT / "app/src/main/assets/exercise-db-v2/progressions.json").read_text()),
                json.loads((output / "sources.json").read_text()),
            )

            self.assertEqual(operational["count"], validation["blockers"]["resistance_mode_unknown"])
            self.assertEqual(operational["count"], result["counts"]["resistance_mode_unknown"])
            ids = [record["id"] for record in operational["records"]]
            self.assertEqual(len(ids), len(set(ids)))
            self.assertEqual(ids, sorted(ids))
            self.assertTrue(all(record["resistanceMode"] == "UNKNOWN" for record in operational["records"]))

            build_database(ROOT, output)
            self.assertEqual(first_bytes, report_path.read_bytes())

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
