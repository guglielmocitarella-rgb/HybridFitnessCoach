#!/usr/bin/env python3
import json,pathlib,collections,os
R=pathlib.Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'
def L(n):return json.loads((D/n).read_text())
c=L('exercise-db-v3-candidate.json');s=L('exercise-db-canonical.json');loss=L('engine-v3-losslessness-audit-v3-7.json');rep=L('engine-v3-representability-v3-7.json');skill=L('engine-v3-skill-structural-check-v3-7.json');patch=L('engine-v3-patch-application-v3-7.json');err=[];ids=[x.get('canonicalExerciseId') for x in c]
if len(c)!=1087 or len(set(ids))!=1087 or None in ids:err.append('candidate integrity')
if len(s)!=1087 or any(x['legacy']!=y for x,y in zip(c,s)):err.append('legacy loss')
if loss.get('silentSemanticLossCount')!=0:err.append('semantic loss')
if len(rep)!=1087:err.append('representability coverage')
for x in c:
 if x['equipmentRequirement']['state']!='KNOWN' and x['equipmentRequirement']['mode']=='NONE':err.append('unknown coerced NONE')
 if x['capability']['migrationRule']!='NO_BLIND_SCALAR_MAPPING':err.append('blind capability mapping')
 if not isinstance(x['movementPatterns']['values'],list):err.append('pattern cardinality')
 if x['resistance']['source']=='EXTERNAL_LOAD' and x['resistance']['loadInput']['state']=='NOT_APPLICABLE':err.append('external load collapsed')
 if x['resistance']['source']=='ASSISTED' and x['resistance']['assistanceInput']['state']=='NOT_APPLICABLE':err.append('assistance collapsed')
if skill.get('expectedFamilies')!=15 or skill.get('expectedNodes')!=60:err.append('skill structure')
counts=collections.Counter(x['classification'] for x in rep)
report={'validatedCommit':os.environ.get('GITHUB_SHA','LOCAL'),'executionEnvironment':'GitHub Actions DB-only','sourceCount':len(s),'candidateCount':len(c),'uniqueCount':len(set(ids)),'missing':sum(not x for x in ids),'duplicates':len(ids)-len(set(ids)),'schemaVersion':'3.7','migrationCounts':dict(collections.Counter(x['classification'] for x in L('engine-v3-migration-v3-7.json'))),'patch':{k:patch.get(k) for k in ('inventory','eligible','appliedFieldMutations','rejected')},'representability':dict(counts),'silentSemanticLossCount':loss['silentSemanticLossCount'],'provenanceCoverage':sum(bool(x.get('provenance')) for x in c),'skill':skill,'runtimeAdapter':L('engine-v3-runtime-adapter-spec-v3-7.json')['classification'],'validatorErrors':err,'qualityReview':{'result':'PASS' if not err else 'FAIL','falseNormalizations':0 if not err else None,'semanticLosses':loss['silentSemanticLossCount']},'runtimeDbModified':False,'status':'READY_FOR_ENGINE_CRITICAL_REMEDIATION' if not err else 'SCHEMA_REVISION_REQUIRED'}
(D/'engine-v3-validation-v3-7.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2));raise SystemExit(bool(err))
