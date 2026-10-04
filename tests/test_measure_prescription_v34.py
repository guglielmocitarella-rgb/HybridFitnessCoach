import json,pathlib,subprocess,sys,unittest
R=pathlib.Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'
class V34(unittest.TestCase):
 @classmethod
 def setUpClass(c):subprocess.run([sys.executable,str(R/'tools/audit_measure_prescription_v34.py')],check=True,cwd=R)
 def j(s,n):return json.loads((D/n).read_text())
 def test_integrity(s):v=s.j('measure-prescription-validation-v3-4.json');s.assertEqual((v['canonicalCount'],v['canonicalUnique'],v['canonicalMissing'],v['canonicalDuplicates']),(1087,1087,0,0))
 def test_historical(s):s.assertEqual(s.j('measure-prescription-validation-v3-4.json')['historical500Meaning'],'INSTRUCTION_COMPLETENESS_500')
 def test_no_false_normalization(s):s.assertEqual(s.j('measure-prescription-field-patch-v3-4.json'),[])
 def test_terminal(s):s.assertTrue(all(x['classification'].startswith('UNRESOLVED_') for x in s.j('measure-prescription-final-unresolved-v3-4.json')))
 def test_matrix(s):s.assertTrue({'SUPPORTED','SUPPORTED_WITH_LOSS','NOT_SUPPORTED','SCHEMA_DEPENDENT'}<={x['status'] for x in s.j('measure-prescription-compatibility-v3-4.json')})
 def test_prior_artifacts(s):
  for n in ['equipment-validation-v3-1c.json','resistance-mode-validation-v3-2a.json','level-validation-v3-2b.json','skill-validation-v3-2c.json','category-group-validation-v3-3.json']:s.assertTrue((D/n).exists())
