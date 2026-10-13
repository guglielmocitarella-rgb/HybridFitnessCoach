import json,pathlib,unittest
D=pathlib.Path("exercise-db-canonical")
class V38(unittest.TestCase):
 def j(self,n): return json.loads((D/n).read_text())
 def test_matrix(self):
  m=self.j("engine-ready-matrix-v3-8.json"); self.assertEqual(len(m),1087); self.assertEqual(len({x["canonicalExerciseId"] for x in m}),1087)
 def test_no_false_normalization(self): self.assertEqual(self.j("engine-remediation-layer-v3-8.json")["mutations"],[])
 def test_skill_accounting(self):
  s=self.j("engine-skill-path-matrix-v3-8.json"); self.assertEqual(s["expectedFamilies"],15); self.assertEqual(s["expectedNodes"],60); self.assertEqual(s["typedRelations"],117)
 def test_runtime_untouched(self): self.assertEqual(self.j("engine-remediation-provenance-v3-8.json")["coverage"],1087)
 def test_exhaustive_classification(self):
  s=self.j("engine-critical-remediation-summary-v3-8.json"); self.assertEqual(sum(s["before"].values()),s["deduplicatedSemanticUnresolved"])
