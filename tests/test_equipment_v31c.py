import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
D=ROOT/'exercise-db-canonical'
class EquipmentV31CTest(unittest.TestCase):
 def load(self,n): return json.loads((D/n).read_text())
 def test_residual_is_exact_and_excludes_v31b_resolved(self):
  b=self.load('equipment-evidence-resolution-v3-1b.json'); c=self.load('equipment-residual-resolution-v3-1c.json')
  residual={r['canonicalId'] for r in b['records'] if r['finalClassification'] in {'REMAINS_AMBIGUOUS','REQUIRES_MANUAL_REVIEW','NO_RELIABLE_EVIDENCE'}}
  resolved={r['canonicalId'] for r in b['records'] if r['finalClassification'].startswith('RESOLVED_')}
  ids=[r['canonicalId'] for r in c['records']]
  self.assertEqual(len(ids),95); self.assertEqual(len(set(ids)),95); self.assertEqual(set(ids),residual); self.assertFalse(set(ids)&resolved)
 def test_terminal_classes_and_fail_closed(self):
  c=self.load('equipment-residual-resolution-v3-1c.json'); allowed={'RESOLVED_CERTAIN','RESOLVED_HIGH_CONFIDENCE','RESOLVED_NO_EQUIPMENT','RESOLVED_BODYWEIGHT','RESOLVED_OPTIONAL','RESOLVED_ONE_OF','RESOLVED_ASSISTED_BY','RESOLVED_EXTERNAL_LOAD','UNRESOLVED_INTRINSICALLY_AMBIGUOUS','UNRESOLVED_INSUFFICIENT_EVIDENCE','UNRESOLVED_SCHEMA_DECISION_REQUIRED','UNRESOLVED_CANONICAL_IDENTITY_PROBLEM'}
  self.assertTrue(all(r['finalClassification'] in allowed for r in c['records']))
  self.assertTrue(all(r.get('failClosed') is True for r in c['records'] if r['finalClassification'].startswith('UNRESOLVED_')))
 def test_patch_is_additive_and_semantically_safe(self):
  p=self.load('equipment-field-patch-v3-1c.json'); self.assertFalse(p['runtimeDatabaseModified']); self.assertFalse(p['selectionEngineModified'])
  for m in p['mutations']:
   if m['field']=='equipmentStatus': self.assertEqual((m['oldValue'],m['newValue']),('UNKNOWN','KNOWN'))
   if m['field']=='equipmentRequirement': self.assertEqual(m['newValue'],{'status':'KNOWN','mode':'NONE','required':[]})
 def test_provenance_for_every_new_resolution(self):
  c=self.load('equipment-residual-resolution-v3-1c.json'); p=self.load('equipment-provenance-v3-1c.json')
  rid={r['canonicalId'] for r in c['records'] if r['finalClassification'].startswith('RESOLVED_')}; pid={e['canonicalId'] for e in p['entries']}
  self.assertEqual(rid,pid); self.assertTrue(all(e['source'].get('url') and e['evidence'] and e['confidence'] for e in p['entries']))
if __name__=='__main__': unittest.main()
