import json,pathlib
D=pathlib.Path(__file__).resolve().parents[1]/'exercise-db-canonical'
def j(n):return json.loads((D/n).read_text())
def test_matrix_integrity():
 m=j('engine-readiness-matrix-v3-6.json');ids=[x['canonicalExerciseId'] for x in m];assert len(m)==1087 and len(set(ids))==1087 and all(ids)
def test_contract_and_editorial():
 c={x['class'] for x in j('engine-minimum-contract-v3-6.json')};assert {'REQUIRED_FOR_ALL','REQUIRED_CONDITIONALLY','OPTIONAL_ENGINE_ENHANCING','NOT_REQUIRED_FOR_ENGINE'}<=c;assert j('editorial-deferred-v3-6.json')['countsNotIncludedInEngineReadiness']
def test_schema():
 s={x['id'] for x in j('engine-schema-requirements-v3-6.json')};assert {'SCHEMA-EQUIPMENT','SCHEMA-RESISTANCE','SCHEMA-CAPABILITY','SCHEMA-SKILL','SCHEMA-PRESCRIPTION','SCHEMA-MOVEMENT'}<=s
def test_runtime_untouched():
 s=j('engine-readiness-summary-v3-6.json');assert s['runtimeDbModified'] is False and s['status']=='READY_FOR_V3_SCHEMA_CANDIDATE'
def test_blockers():
 p={x['priority'] for x in j('engine-blocker-queue-v3-6.json')};assert {'P0','P1','P3','P4','P5'}<=p
def test_skill_accounting():
 s=j('engine-skill-trainability-v3-6.json');assert s['expectedFamilies']==15 and s['expectedNodes']==60
def test_validation():
 v=j('engine-readiness-validation-v3-6.json');assert v['matrix']=={'count':1087,'unique':1087,'missing':0,'duplicates':0} and v['runtimeDbModified'] is False
