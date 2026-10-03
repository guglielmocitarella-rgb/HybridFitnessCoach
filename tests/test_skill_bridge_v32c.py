import json,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; OUT=ROOT/"exercise-db-canonical"
class SkillBridgeV32CTest(unittest.TestCase):
 def setUp(self):
  self.audit=json.loads((OUT/"skill-bridge-audit-v3-2c.json").read_text()); self.rels=json.loads((OUT/"skill-relations-v3-2c.json").read_text())
 def test_integrity(self): self.assertEqual(self.audit["integrity"],{"canonical":1087,"unique":1087,"missingIds":0,"duplicates":0})
 def test_catalog_and_workset(self):
  self.assertEqual(self.audit["skillCatalog"]["verifiedSkillCount"],15); self.assertEqual(self.audit["missingUnstableWorkset"]["verifiedCount"],143)
 def test_refs_and_types(self):
  ids={n for s in self.audit["skillCatalog"]["skills"] for n in s["nodeIds"]}; allowed={"SKILL_ITSELF","DIRECT_PROGRESSION","DIRECT_REGRESSION","PREREQUISITE_EXERCISE","ACCESSORY","STRENGTH_BUILDER","MOBILITY_PREPARATION","TECHNIQUE_DRILL","TRANSFER_EXERCISE","RELATED_ONLY"}
  self.assertTrue(all(r["skillId"] in ids and r["relationType"] in allowed for r in self.rels)); self.assertEqual(len({(r["canonicalExerciseId"],r["skillId"],r["relationType"]) for r in self.rels}),len(self.rels))
 def test_schema_decision(self):
  self.assertEqual(self.audit["modelStatus"],"INSUFFICIENT_NEEDS_TYPED_RELATIONS"); self.assertTrue(all(r["classification"]=="UNRESOLVED_SCHEMA_DECISION_REQUIRED" for r in self.audit["missingUnstableWorkset"]["records"]))
if __name__=="__main__": unittest.main()
