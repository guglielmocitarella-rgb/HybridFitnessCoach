import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/'exercise-db-canonical'
class LevelCapabilityV32BTest(unittest.TestCase):
 def setUp(self):
  self.a=json.loads((DB/'level-capability-audit-v3-2b.json').read_text()); self.p=json.loads((DB/'level-field-patch-v3-2b.json').read_text())
 def test_all_unknown_audited_once(self): self.assertEqual(438,len(self.a['records'])); self.assertEqual(438,len({r['canonicalId'] for r in self.a['records']}))
 def test_no_false_precision_patch(self): self.assertEqual([],self.p['patches'])
 def test_terminal_classes(self):
  allowed={'RESOLVED_CERTAIN','RESOLVED_HIGH_CONFIDENCE','UNRESOLVED_VARIABLE_DIFFICULTY','UNRESOLVED_INSUFFICIENT_EVIDENCE','UNRESOLVED_SOURCE_SCALE_CONFLICT','UNRESOLVED_SCHEMA_DECISION_REQUIRED','UNRESOLVED_CANONICAL_IDENTITY_PROBLEM'}
  self.assertTrue(all(r['classification'] in allowed for r in self.a['records']))
 def test_variable_scalable_present(self): self.assertGreater(self.a['finalClassification'].get('UNRESOLVED_VARIABLE_DIFFICULTY',0),0)
 def test_skill_schema_cases_present(self): self.assertGreater(self.a['finalClassification'].get('UNRESOLVED_SCHEMA_DECISION_REQUIRED',0),0)
 def test_model_distinguishes_capabilities(self):
  fields={x['field'] for x in self.a['model']['minimumProposal']}; self.assertTrue({'technicalDifficulty','strengthRequirement','mobilityRequirement','balanceCoordinationRequirement'}<=fields)
 def test_known_quality_audit(self): self.assertGreaterEqual(self.a['knownQualityAudit']['sampleSize'],10); self.assertTrue(self.a['knownQualityAudit']['systematicIssue'])
 def test_unknown_not_advanced_normalization(self): self.assertIn('NO_BULK_NORMALIZATION',self.a['normalizationDecision'])
if __name__=='__main__': unittest.main()
