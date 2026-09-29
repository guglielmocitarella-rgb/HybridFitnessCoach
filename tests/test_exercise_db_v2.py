import json, pathlib, subprocess, sys, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-v2"
class ExerciseDbV2Test(unittest.TestCase):
    def test_required_artifacts_parse(self):
        for n in ("BUILD_MANIFEST.json","exercises.json","progressions.json","sources.json"):
            with (DB/n).open(encoding="utf-8") as f: json.load(f)
    def test_validator(self):
        self.assertEqual(subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")]),0)
    def test_deterministic_rebuild(self):
        tracked=("exercises.json","progressions.json","sources.json","BUILD_MANIFEST.json")
        before={n:(DB/n).read_bytes() for n in tracked}
        self.assertEqual(subprocess.call([sys.executable,str(ROOT/"tools/build_exercise_db.py")]),0)
        after={n:(DB/n).read_bytes() for n in tracked}
        self.assertEqual(before,after)
if __name__=="__main__": unittest.main()
