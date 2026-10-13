import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-canonical"
class MovementPatternV35(unittest.TestCase):
 def load(self,n): return json.loads((DB/n).read_text())
 def test_integrity_and_all_records_audited(self):
  a=self.load("movement-pattern-audit-v3-5.json")
  self.assertEqual((a["canonicalCount"],a["unique"],a["missingIds"],a["duplicates"]),(1087,1087,0,0))
  self.assertTrue(a["allRecordsAudited"]); self.assertEqual(a["recordAuditCount"],1087)
  rows=self.load("movement-pattern-record-audit-v3-5.json")
  self.assertEqual(len(rows),1087); self.assertEqual(len({x["canonicalExerciseId"] for x in rows}),1087)
 def test_historical_baseline_is_preserved(self):
  a=self.load("movement-pattern-audit-v3-5.json")
  self.assertEqual(a["historicalMissing"]["count"],176); self.assertEqual(a["currentMissing"],176)
 def test_workset_is_deduplicatable_and_terminal(self):
  w=self.load("movement-pattern-workset-v3-5.json")
  u=self.load("movement-pattern-final-unresolved-v3-5.json")
  self.assertTrue(w); self.assertTrue(all(x["terminalClassification"] for x in w))
  self.assertTrue(all(x["terminalClassification"].startswith("UNRESOLVED") for x in u))
 def test_heuristic_known_values_are_not_silently_certified(self):
  a=self.load("movement-pattern-audit-v3-5.json")
  self.assertGreater(a["issuesByType"].get("HEURISTIC_PATTERN_UNCERTIFIED",0),0)
  self.assertGreater(a["issuesByType"].get("SOURCE_TAXONOMY_DERIVED",0),0)
 def test_no_false_patch_and_provenance_exact(self):
  p=self.load("movement-pattern-field-patch-v3-5.json"); q=self.load("movement-pattern-provenance-v3-5.json")
  self.assertEqual(p,[]); self.assertEqual(len(p),len(q))
 def test_multi_pattern_preserved(self):
  a=self.load("movement-pattern-audit-v3-5.json")
  self.assertGreaterEqual(a["issuesByType"].get("MULTI_PATTERN",0),1)
  self.assertIn("MULTI_PATTERN",a["modelStatus"])
 def test_semantic_separation_no_inference_patch(self):
  self.assertEqual(self.load("movement-pattern-field-patch-v3-5.json"),[])
  text=(DB/"MOVEMENT_PATTERN_SCHEMA_PROPOSAL_V3_5.md").read_text()
  for token in ("category","muscles","equipment","skill"): self.assertIn(token.lower(),text.lower())
 def test_variety_scenarios_executed(self):
  v=self.load("movement-variety-analysis-v3-5.json")
  names={x["name"] for x in v["scenarios"]}
  self.assertTrue({"CANONICAL_GLOBAL_READ_ONLY","BODYWEIGHT_MINIMAL","HOME_SYNTHETIC","GYM_SYNTHETIC"}.issubset(names))
  for s in v["scenarios"]:
   self.assertGreaterEqual(s["candidatePool"],s["recordsWithPattern"])
   self.assertGreaterEqual(s["uniquePatterns"],0)
  self.assertIn(v["finalFinding"],{"DB_DOMINANT","ENGINE_DOMINANT","MIXED","EXPECTED_FILTERING_DOMINANT","INSUFFICIENT_EVIDENCE"})
 def test_validation_artifact_contract(self):
  v=self.load("movement-pattern-validation-v3-5.json")
  self.assertEqual(v["recordAuditCount"],1087); self.assertGreaterEqual(v["scenarioCount"],4)
  self.assertFalse(v["runtimeDbModified"]); self.assertEqual(v["domainStatus"],"CLOSED_WITH_DOCUMENTED_UNRESOLVED")
if __name__=="__main__": unittest.main()
