import json,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/'exercise-db-canonical'
class CategoryGroupV33Test(unittest.TestCase):
 def setUp(self):
  self.a=json.loads((DB/'category-group-audit-v3-3.json').read_text()); self.p=json.loads((DB/'category-group-field-patch-v3-3.json').read_text()); self.u=json.loads((DB/'category-group-final-unresolved-v3-3.json').read_text())
 def test_integrity(self): self.assertEqual({'canonical':1087,'unique':1087,'missing':0,'duplicates':0},self.a['integrity'])
 def test_category_enum(self): self.assertTrue(set(self.a['categoryDistribution']) <= {'PUSH','PULL','LEGS','CORE','CARDIO','MOBILITY','SKILL','OTHER','<MISSING>'})
 def test_distinct_semantics(self): self.assertEqual('ENGINE_CRITICAL',self.a['contract']['category']['classification']); self.assertEqual('ENGINE_ENHANCING',self.a['contract']['group']['classification'])
 def test_no_blind_patch(self): self.assertEqual([],self.p['patches'])
 def test_terminal_unresolved(self):
  allowed={'UNRESOLVED_SCHEMA_DECISION_REQUIRED','UNRESOLVED_SOURCE_TAXONOMY_CONFLICT'}
  self.assertTrue(all(x['classification'] in allowed for x in self.u['records']))
 def test_workset_dedup(self): self.assertEqual(self.a['worksetCount'],self.a['worksetUnique']); self.assertEqual(self.a['worksetUnique'],len({x['canonicalExerciseId'] for x in self.a['records']}))
 def test_patch_provenance(self):
  prov=json.loads((DB/'category-group-provenance-v3-3.json').read_text()); self.assertEqual(len(self.p['patches']),len(prov['entries']))
if __name__=='__main__':unittest.main()
