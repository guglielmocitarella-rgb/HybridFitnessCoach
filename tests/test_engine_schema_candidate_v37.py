import json,pathlib,unittest
R=pathlib.Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'
class V37(unittest.TestCase):
 @classmethod
 def setUpClass(c):c.x=json.loads((D/'exercise-db-v3-candidate.json').read_text())
 def test_integrity(self):self.assertEqual((len(self.x),len({x['canonicalExerciseId'] for x in self.x})),(1087,1087))
 def test_none_unknown(self):self.assertFalse(any(x['equipmentRequirement']['state']!='KNOWN' and x['equipmentRequirement']['mode']=='NONE' for x in self.x))
 def test_resistance_separation(self):self.assertTrue(all('loadInput' in x['resistance'] and 'assistanceInput' in x['resistance'] for x in self.x))
 def test_no_blind_level(self):self.assertTrue(all(x['capability']['migrationRule']=='NO_BLIND_SCALAR_MAPPING' for x in self.x))
 def test_typed_skills(self):self.assertTrue(all(all(r.get('relationType') and r.get('targetType') for r in x['skillRelations']) for x in self.x))
 def test_category_group(self):self.assertTrue(all('category' in x['categoryGroup'] and 'group' in x['categoryGroup'] for x in self.x))
 def test_prescription(self):self.assertTrue(all('primaryMeasure' in x['prescription'] and 'dimensions' in x['prescription'] for x in self.x))
 def test_patterns(self):self.assertTrue(all(isinstance(x['movementPatterns']['values'],list) for x in self.x))
 def test_provenance(self):self.assertEqual(sum(bool(x['provenance']) for x in self.x),1087)
 def test_lossless(self):self.assertEqual([x['legacy'] for x in self.x],json.loads((D/'exercise-db-canonical.json').read_text()))
 def test_runtime_untouched(self):self.assertFalse(json.loads((D/'engine-v3-runtime-adapter-spec-v3-7.json').read_text())['runtimeDbModified'])
if __name__=='__main__':unittest.main()
