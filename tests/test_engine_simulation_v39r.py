import json, pathlib, subprocess, sys, unittest
D=pathlib.Path("exercise-db-canonical")
class V39R(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  subprocess.run([sys.executable,"tools/simulate_engine_v39r.py"],check=True)
  cls.s=json.loads((D/"simulation-summary-v3-9.json").read_text())
  cls.r=json.loads((D/"capability-selection-remediation-v3-9r.json").read_text())
 def test_no_blind_mapping(self):
  self.assertFalse(self.r["blindLegacyMapping"])
  self.assertFalse(self.s["legacyCompatibilityIsCapabilityMapping"])
 def test_dimensions_preserved(self):
  self.assertEqual(self.r["dimensionsPreserved"],["strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"])
 def test_integrity(self):
  self.assertEqual((self.s["candidateCount"],self.s["uniqueIds"],self.s["missingIds"],self.s["duplicateIds"]),(1087,1087,0,0))
 def test_no_invalid_behavior(self):
  self.assertEqual((self.s["falseEligibilityCount"],self.s["invalidPrescriptionCount"],self.s["invalidSkillProgressionCount"],self.s["invalidSubstitutionCount"]),(0,0,0,0))
if __name__=="__main__": unittest.main()
