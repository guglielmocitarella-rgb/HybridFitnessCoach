import json,random,unittest,pathlib,subprocess,sys
D=pathlib.Path("exercise-db-canonical")
class V39(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  subprocess.run([sys.executable,"tools/simulate_engine_v39.py"],check=True)
 def test_same_seed(self):
  a=random.Random(42); b=random.Random(42); self.assertEqual([a.random() for _ in range(20)],[b.random() for _ in range(20)])
 def test_integrity(self):
  s=json.loads((D/"simulation-summary-v3-9.json").read_text()); self.assertEqual((s["candidateCount"],s["uniqueIds"],s["missingIds"],s["duplicateIds"]),(1087,1087,0,0))
 def test_skill_coverage(self): self.assertEqual(len(json.loads((D/"skill-simulation-results-v3-9.json").read_text())),60)
 def test_no_invalid_behavior(self):
  s=json.loads((D/"simulation-summary-v3-9.json").read_text()); self.assertEqual((s["falseEligibilityCount"],s["invalidPrescriptionCount"],s["invalidSkillProgressionCount"],s["invalidSubstitutionCount"]),(0,0,0,0))
 def test_runtime_untouched(self): self.assertFalse(json.loads((D/"simulation-summary-v3-9.json").read_text())["runtimeDbModified"])
if __name__=="__main__": unittest.main()
