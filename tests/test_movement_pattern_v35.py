import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-canonical"
class MovementPatternV35(unittest.TestCase):
 def load(self,n): return json.loads((DB/n).read_text())
 def test_integrity(self):
  a=self.load("movement-pattern-audit-v3-5.json"); self.assertEqual(a["canonicalCount"],1087); self.assertEqual(a["unique"],1087); self.assertTrue(a["allRecordsAudited"])
 def test_historical(self): self.assertEqual(self.load("movement-pattern-audit-v3-5.json")["historicalMissing"]["count"],176)
 def test_unresolved_terminal(self):
  for x in self.load("movement-pattern-final-unresolved-v3-5.json"): self.assertTrue(x["terminalClassification"].startswith("UNRESOLVED"))
 def test_no_false_patch(self): self.assertEqual(self.load("movement-pattern-field-patch-v3-5.json"),[])
 def test_provenance(self): self.assertEqual(len(self.load("movement-pattern-field-patch-v3-5.json")),len(self.load("movement-pattern-provenance-v3-5.json")))
 def test_variety_schema(self):
  v=self.load("movement-variety-analysis-v3-5.json"); self.assertIn(v["finalFinding"],{"DB_DOMINANT","ENGINE_DOMINANT","MIXED","EXPECTED_FILTERING_DOMINANT","INSUFFICIENT_EVIDENCE"}); self.assertTrue(v["scenarios"])
if __name__=="__main__": unittest.main()
