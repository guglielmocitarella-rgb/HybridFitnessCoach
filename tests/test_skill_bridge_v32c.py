import json,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; OUT=ROOT/"exercise-db-canonical"
class SkillBridgeV32CTest(unittest.TestCase):
 def setUp(self):
  self.audit=json.loads((OUT/"skill-bridge-audit-v3-2c.json").read_text()); self.rels=json.loads((OUT/"skill-relations-v3-2c.json").read_text()); self.unresolved=json.loads((OUT/"skill-final-unresolved-v3-2c.json").read_text()); self.prov=json.loads((OUT/"skill-provenance-v3-2c.json").read_text())
 def test_integrity(self): self.assertEqual(self.audit["integrity"],{"canonical":1087,"unique":1087,"missingIds":0,"duplicates":0})
 def test_catalog_workset_terminal(self):
  self.assertEqual(self.audit["skillCatalog"],{"verifiedSkillCount":15,"nodeCount":60}); w=self.audit["workset"]; self.assertEqual((w["expected"],w["processed"],w["unique"],w["missing"],w["duplicates"]),(143,143,143,0,0)); self.assertEqual(len(self.audit["terminalClassifications"]),143)
 def test_typed_schema(self):
  allowed={"SKILL_ITSELF","DIRECT_PROGRESSION","DIRECT_REGRESSION","PREREQUISITE","STRENGTH_BUILDER","MOBILITY_PREPARATION","TECHNIQUE_DRILL","ACCESSORY","TRANSFER"}
  self.assertTrue(all(r["targetType"] in {"FAMILY","NODE"} and r["relationType"] in allowed and r["confidence"] in {"CERTAIN","HIGH_CONFIDENCE"} for r in self.rels))
  keys=[(r["canonicalExerciseId"],r["targetType"],r["targetId"],r["relationType"]) for r in self.rels]; self.assertEqual(len(keys),len(set(keys)))
 def test_evidence_provenance(self):
  self.assertEqual(len(self.prov),len(self.rels)); self.assertTrue(all(r.get("evidence") and r.get("reason") and r.get("provenance") for r in self.rels))
 def test_unresolved_explicit(self):
  allowed={"UNRESOLVED_INSUFFICIENT_EVIDENCE","UNRESOLVED_CANONICAL_IDENTITY_PROBLEM","UNRESOLVED_TARGET_AMBIGUOUS","UNRESOLVED_RELATION_TYPE_AMBIGUOUS","UNRESOLVED_SCHEMA_DECISION_REQUIRED"}
  self.assertTrue(all(x["classification"] in allowed and x["manualReviewRequired"] for x in self.unresolved))
 def test_no_ambiguous_applied(self): self.assertFalse(any(r["confidence"]=="AMBIGUOUS" for r in self.rels))
 def test_legacy_preserved(self):
  db=json.loads((OUT/"exercise-db-canonical.json").read_text()); self.assertTrue(any(x.get("skillIds") for x in db))
if __name__=="__main__": unittest.main()
