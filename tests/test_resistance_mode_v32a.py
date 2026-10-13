import json, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
A=json.loads((ROOT/'exercise-db-canonical/resistance-mode-audit-v3-2a.json').read_text())
P=json.loads((ROOT/'exercise-db-canonical/resistance-mode-field-patch-v3-2a.json').read_text())
U=json.loads((ROOT/'exercise-db-canonical/resistance-mode-final-unresolved-v3-2a.json').read_text())
class ResistanceModeV32ATest(unittest.TestCase):
 def test_all_111_audited_unique(self):
  rs=A['records']; self.assertEqual(111,len(rs)); self.assertEqual(111,len({r['canonicalId'] for r in rs}))
 def test_unknown_is_not_not_applicable(self):
  na=[r for r in A['records'] if r['classification']=='RESOLVED_NOT_APPLICABLE']; self.assertTrue(na); self.assertTrue(all(r['newValue']=='NONE' for r in na))
  self.assertTrue(all(r['classification']!='RESOLVED_NOT_APPLICABLE' for r in U['records']))
 def test_patch_values_valid(self):
  allowed={'BODYWEIGHT','EXTERNAL_LOAD','ASSISTED','NONE'}; self.assertTrue(P['mutations']); self.assertTrue(all(m['newValue'] in allowed for m in P['mutations']))
 def test_bodyweight_examples(self):
  ids={r['canonicalId'] for r in A['records'] if r.get('newValue')=='BODYWEIGHT'}
  for x in ['ex-bodyweight-sit-ups','ex-judo-push-up','ex-normal-muscle-up','ex-stability-ball-rollout','ex-star-jumps']: self.assertIn(x,ids)
 def test_external_examples(self):
  ids={r['canonicalId'] for r in A['records'] if r.get('newValue')=='EXTERNAL_LOAD'}
  for x in ['ex-weighted-dip','ex-weighted-pistols','ex-oblique-twist-with-plate-weight','ex-rope-triceps-pushdown','ex-stair-climb-with-biceps-curl']: self.assertIn(x,ids)
 def test_not_applicable_examples(self):
  ids={r['canonicalId'] for r in A['records'] if r['classification']=='RESOLVED_NOT_APPLICABLE'}
  for x in ['ex-forward-fold','ex-glute-stretch','ex-jogging-and-running','ex-sprints','ex-scapula-foam-rolling']: self.assertIn(x,ids)
 def test_variable_examples(self):
  ids={r['canonicalId'] for r in U['records'] if r['classification']=='UNRESOLVED_INTRINSICALLY_VARIABLE'}
  for x in ['ex-russian-twist','ex-side-to-side-squat','ex-lunge-to-row','ex-back-foot-elevated-split-squat','ex-neutral-grip-horizontal-row']: self.assertIn(x,ids)
 def test_schema_and_identity_terminal(self):
  s={r['canonicalId'] for r in U['records'] if r['classification']=='UNRESOLVED_SCHEMA_DECISION_REQUIRED'}
  for x in ['ex-assisted-pull-up','ex-assisted-pistol-squat','ex-low-alternating-wave-with-battle-ropes']: self.assertIn(x,s)
  i={r['canonicalId'] for r in U['records'] if r['classification']=='UNRESOLVED_CANONICAL_IDENTITY_PROBLEM'}; self.assertIn('ex-rows',i|{''}) if False else self.assertIn('ex-boxer',i)
 def test_equipment_not_resistance_truth(self):
  by={r['canonicalId']:r for r in A['records']}; self.assertEqual('BODYWEIGHT',by['ex-stability-ball-rollout']['newValue']); self.assertEqual('NONE',by['ex-skippingjump-rope']['newValue'])
 def test_schema_declared_insufficient(self): self.assertFalse(A['semanticDecision']['schemaSufficient'])
if __name__=='__main__': unittest.main()
