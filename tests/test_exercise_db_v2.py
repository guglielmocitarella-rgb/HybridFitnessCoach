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
