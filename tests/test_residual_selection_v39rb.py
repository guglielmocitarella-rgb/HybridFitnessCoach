import json,pathlib,subprocess,sys,unittest
D=pathlib.Path("exercise-db-canonical")
class V39RB(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  subprocess.run([sys.executable,"tools/audit_residual_selection_v39rb.py"],check=True)
  cls.v=json.loads((D/"v39r-b-validation.json").read_text()); cls.s=json.loads((D/"v39r-b-full-rerun-summary.json").read_text())
 def test_gate(self): self.assertEqual((self.v["status"],self.v["stillBlocked"],self.v["invalidZeroPool"],self.v["invalidSelection"]),("PASS",0,0,0))
 def test_integrity(self): self.assertEqual((self.v["candidateCount"],self.v["unique"],self.v["missing"],self.v["duplicates"]),(1087,1087,0,0))
 def test_semantics(self): self.assertTrue(self.v["fourCapabilitiesPreserved"]); self.assertFalse(self.v["blindLevelMapping"]); self.assertFalse(self.v["globalUnknownRelaxation"]); self.assertFalse(self.v["runtimeDbModified"])
 def test_matrix(self): self.assertEqual((self.s["scenarios"],self.s["runs"],self.s["validPools"],self.s["legitimateZeroPools"]),(640,64000,630,10))
if __name__=="__main__": unittest.main()
