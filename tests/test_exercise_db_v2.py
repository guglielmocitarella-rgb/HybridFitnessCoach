import json, pathlib, subprocess, sys, unittest, tempfile, shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-v2"
class ExerciseDbV2Test(unittest.TestCase):
    def test_required_artifacts_parse(self):
        for n in ("BUILD_MANIFEST.json","exercises.json","progressions.json","sources.json","programming_guidance.json"):
            with (DB/n).open(encoding="utf-8") as f: json.load(f)
    def test_validator(self):
        self.assertEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
    def test_deterministic_rebuild(self):
        tracked=("exercises.json","progressions.json","sources.json","programming_guidance.json","BUILD_MANIFEST.json")
        before={n:(DB/n).read_bytes() for n in tracked}
        self.assertEqual(subprocess.call([sys.executable,str(ROOT/"tools/build_exercise_db.py")]),0)
        after={n:(DB/n).read_bytes() for n in tracked}
        self.assertEqual(before,after)
    def test_validator_rejects_unmaterialized_raw_inventory_row(self):
        raw=DB/"raw"/"complete_calisthenics_2e.tsv"
        original=raw.read_text(encoding="utf-8")
        with raw.open("a",encoding="utf-8") as handle:
            handle.write("Definitely Not A Canonical Exercise\tSTRENGTH\tTEST\tCOMPLETE_CAL2\tINDEX\tPENDING_DEDUP\n")
        try: self.assertNotEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
        finally: raw.write_text(original,encoding="utf-8")

    def test_validator_rejects_missing_f01_execution_contract(self):
        original=(DB/"exercises.json").read_text(encoding="utf-8")
        data=json.loads(original)
        data[0]["descriptionIt"]=None
        data[0]["setupIt"]=None
        data[0]["executionStepsIt"]=[]
        (DB/"exercises.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        try: self.assertNotEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
        finally: (DB/"exercises.json").write_text(original,encoding="utf-8")

    def test_all_exercises_have_f01_execution_contract(self):
        data=json.loads((DB/"exercises.json").read_text(encoding="utf-8"))
        self.assertEqual(len(data),587)
        self.assertTrue(all(str(x.get("descriptionIt") or "").strip() for x in data))
        self.assertTrue(all(str(x.get("setupIt") or "").strip() for x in data))
        self.assertTrue(all(x.get("executionStepsIt") and all(str(s).strip() for s in x["executionStepsIt"]) for x in data))

    def test_execution_evidence_tiers_are_explicit_and_consistent(self):
        data=json.loads((DB/"exercises.json").read_text(encoding="utf-8"))
        allowed={"SPECIFIC_SOURCE","FAMILY_GUIDANCE","UNKNOWN"}
        self.assertTrue(all(x.get("executionEvidence") in allowed for x in data))
        specific={"EXERCISE_HEADING","EXERCISE_SECTION","SOURCE_HEADING","CHAPTER_HEADING"}
        for x in data:
            if x["executionEvidence"]=="SPECIFIC_SOURCE":
                self.assertTrue(any(ref.get("evidence_type") in specific for ref in x.get("sources",[])),x["id"])

    def test_validator_rejects_unknown_progression_source(self):
        original=(DB/"progressions.json").read_text(encoding="utf-8")
        data=json.loads(original); data[0]["sources"][0]["source_id"]="NOT_REGISTERED"
        (DB/"progressions.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        try: self.assertNotEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
        finally: (DB/"progressions.json").write_text(original,encoding="utf-8")
    def test_validator_rejects_dangling_variant_and_prerequisite(self):
        original=(DB/"exercises.json").read_text(encoding="utf-8")
        data=json.loads(original); data[0]["variantOf"]="missing-id"; data[1]["prerequisites"]=["missing-id"]
        (DB/"exercises.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        try: self.assertNotEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
        finally: (DB/"exercises.json").write_text(original,encoding="utf-8")
if __name__=="__main__": unittest.main()
