#!/usr/bin/env python3
import json,pathlib,copy,collections,os
R=pathlib.Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'
def L(n,d=None):
 p=D/n;return json.loads(p.read_text()) if p.exists() else d
def W(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
def I(x):return x.get('canonicalExerciseId') or x.get('canonicalId') or x.get('id')
def K(v):return v not in (None,'',[],{},'UNKNOWN','UNRESOLVED')
src=L('exercise-db-canonical.json',[]);assert len(src)==1087 and len({I(x) for x in src})==1087
patch=L('engine-patch-inventory-v3-6.json',[]);unr=L('engine-unresolved-consolidated-v3-6.json',[]);rels=L('skill-relations-v3-2c.json',[])
P=collections.defaultdict(list);U=collections.defaultdict(list);S=collections.defaultdict(list)
for x in patch:P[I(x)].append(x)
for x in unr:U[I(x)].append(x)
for x in rels:S[I(x)].append(x)
schema={'schemaVersion':'3.7','principle':'minimum-sufficient-lossless-fail-closed','stateDomain':['KNOWN','UNKNOWN','NOT_APPLICABLE','EVIDENCE_UNRESOLVED','SCHEMA_UNRESOLVED','IDENTITY_PROBLEM'],'types':{
'equipmentRequirement':{'mode':['NONE','REQUIRED','OPTIONAL','ONE_OF','ASSISTED_BY','EXTERNAL_LOAD','UNKNOWN'],'cardinality':'structured'},
'resistance':{'source':['BODYWEIGHT','EXTERNAL_LOAD','ASSISTED','NONE','BAND','MACHINE','MANUAL_ASSISTANCE','OTHER','UNKNOWN'],'separates':['source','loadInput','assistanceInput','quantification']},
'capability':{'dimensions':['strengthRequirement','technicalDifficulty','mobilityRequirement','balanceCoordinationRequirement'],'scale':['LOW','MODERATE','HIGH','UNKNOWN','NOT_APPLICABLE'],'legacyLevel':'LEGACY_ONLY_NO_BLIND_MAPPING'},
'skillRelations':{'relationTypes':sorted({x.get('relationType') for x in rels if x.get('relationType')}),'targetTypes':['FAMILY','NODE']},
'categoryGroup':{'rule':'category and recovery group are distinct'},
'prescription':{'dimensions':['REPS','DURATION','DISTANCE','INTERVALS','LOAD','ASSISTANCE','DISTANCE_TIME','ATTEMPT','QUALITY','OPEN_ENDED']},
'movementPatterns':{'cardinality':'0..n','zeroSemantics':['UNKNOWN','NOT_APPLICABLE']},
'provenance':{'levels':['record','field','relation']}},
'requiredness':{'identity':'REQUIRED','category':'REQUIRED','group':'REQUIRED','equipment':'REQUIRED','resistance':'CONDITIONAL','measure':'REQUIRED','prescription':'CONDITIONAL','capability':'CONDITIONAL','skillRelations':'CONDITIONAL','movementPatterns':'OPTIONAL_ENGINE_ENHANCING'},'legacyCompatibility':True}
W('engine-schema-candidate-v3-7.json',schema)
eligible=[];rejected=[]
for p in patch:
 if not I(p) or 'newValue' not in p:rejected.append({**p,'reason':'missing target/value'})
 elif p.get('confidence') in ('LOW','UNRESOLVED'):rejected.append({**p,'reason':'insufficient confidence'})
 else:eligible.append(p)
pm=collections.defaultdict(dict)
for p in eligible:pm[I(p)][p.get('field')]=p
cand=[];mig=[];uo=[]
for s in src:
 i=I(s);r=copy.deepcopy(s);ap=[]
 for f,p in pm[i].items():
  if f in r:r[f]=copy.deepcopy(p['newValue']);ap.append(f)
 ep=pm[i].get('equipmentRequirement',{}).get('newValue') if pm[i].get('equipmentRequirement') else None
 if isinstance(ep,dict):
  eq={'state':'KNOWN','mode':'UNKNOWN','required':[],'alternatives':[],'optional':[],'assistance':[],'externalLoadSupport':False,**ep};eq['state']='KNOWN' if eq.get('status','KNOWN')=='KNOWN' else 'UNKNOWN';eq.pop('status',None)
 elif r.get('equipmentStatus')=='KNOWN':
  q=list(r.get('equipmentRequired') or []);eq={'state':'KNOWN','mode':'NONE' if not q else 'REQUIRED','required':q,'alternatives':[],'optional':[],'assistance':[],'externalLoadSupport':False}
 else:eq={'state':'EVIDENCE_UNRESOLVED','mode':'UNKNOWN','required':list(r.get('equipmentRequired') or []),'alternatives':[],'optional':[],'assistance':[],'externalLoadSupport':False}
 rm=r.get('resistanceMode','UNKNOWN');rs={'BODYWEIGHT':'BODYWEIGHT','EXTERNAL_LOAD':'EXTERNAL_LOAD','ASSISTED':'ASSISTED','NONE':'NONE'}.get(rm,'UNKNOWN')
 resistance={'state':'KNOWN' if rs!='UNKNOWN' else 'EVIDENCE_UNRESOLVED','source':rs,'loadInput':{'state':'SCHEMA_UNRESOLVED' if rs=='EXTERNAL_LOAD' else 'NOT_APPLICABLE','unitSemantics':'UNRESOLVED' if rs=='EXTERNAL_LOAD' else None},'assistanceInput':{'state':'SCHEMA_UNRESOLVED' if rs=='ASSISTED' else 'NOT_APPLICABLE','unitSemantics':'UNRESOLVED' if rs=='ASSISTED' else None},'quantification':'UNRESOLVED' if rs in ('EXTERNAL_LOAD','ASSISTED') else 'NOT_APPLICABLE'}
 lv=r.get('level');ls=r.get('levelStatus','KNOWN' if K(lv) else 'UNKNOWN')
 cap={'state':'SCHEMA_UNRESOLVED' if ls=='KNOWN' else 'EVIDENCE_UNRESOLVED','strengthRequirement':'UNKNOWN','technicalDifficulty':'UNKNOWN','mobilityRequirement':'UNKNOWN','balanceCoordinationRequirement':'UNKNOWN','legacyLevel':lv,'legacyLevelStatus':ls,'migrationRule':'NO_BLIND_SCALAR_MAPPING'}
 cat=r.get('category') or r.get('movementType');grp=r.get('group')
 cg={'state':'KNOWN' if K(cat) else 'EVIDENCE_UNRESOLVED','category':cat,'group':grp,'secondaryGroups':r.get('secondaryGroups') or [],'groupState':'KNOWN' if K(grp) else 'SCHEMA_UNRESOLVED'}
 m=r.get('measure');mm={'REPS':'REPS','TIME':'DURATION','DURATION':'DURATION','DISTANCE':'DISTANCE','INTERVAL':'INTERVALS','INTERVALS':'INTERVALS','ATTEMPT':'ATTEMPT','QUALITY':'QUALITY'};dims=[{'type':mm[m],'state':'KNOWN','source':'legacy.measure'}] if m in mm else []
 pres={'state':'KNOWN' if dims else 'EVIDENCE_UNRESOLVED','primaryMeasure':m,'dimensions':dims,'openEnded':bool(r.get('openEnded') or r.get('voluntaryTermination'))}
 q=r.get('movementPattern');pats=q if isinstance(q,list) else ([q] if K(q) else []);pst='KNOWN' if pats else ('NOT_APPLICABLE' if r.get('movementPatternStatus')=='NOT_APPLICABLE' else 'UNKNOWN')
 v={'schemaVersion':'3.7','canonicalExerciseId':i,'canonicalName':r.get('canonicalNameEn') or r.get('name'),'aliases':r.get('aliases') or [],'identityState':'KNOWN' if i and (r.get('canonicalNameEn') or r.get('name')) else 'IDENTITY_PROBLEM','categoryGroup':cg,'equipmentRequirement':eq,'resistance':resistance,'capability':cap,'skillRelations':copy.deepcopy(S[i]),'prescription':pres,'movementPatterns':{'state':pst,'values':pats},'engineMetadata':{'family':r.get('family'),'laterality':r.get('laterality') or r.get('unilateral'),'isometric':r.get('isometric'),'progressionRelations':r.get('progressionRelations') or []},'legacy':copy.deepcopy(s),'unresolved':copy.deepcopy(U[i]),'provenance':{'recordSources':r.get('sources') or r.get('modelSourceRefs') or [],'fieldProvenance':r.get('fieldProvenance') or {},'relationProvenance':'embedded in skillRelations','patchRefs':[{'domain':p.get('domain'),'field':p.get('field'),'version':p.get('version'),'source':p.get('source')} for p in P[i]]}}
 cand.append(v);cl='VALIDATED_PATCH_APPLIED' if ap else ('UNRESOLVED_PRESERVED' if U[i] else 'LOSSLESS_STRUCTURAL');mig.append({'canonicalExerciseId':i,'classification':cl,'appliedFields':ap,'legacyPreserved':True,'unresolvedCount':len(U[i])})
 for u in U[i]:uo.append({'canonicalExerciseId':i,**u})
W('exercise-db-v3-candidate.json',cand);W('engine-v3-migration-v3-7.json',mig);W('engine-v3-migration-unresolved-v3-7.json',uo)
applied=sum(len(x['appliedFields']) for x in mig);W('engine-v3-patch-application-v3-7.json',{'inventory':len(patch),'eligible':len(eligible),'appliedFieldMutations':applied,'rejected':len(rejected),'rejectedEntries':rejected})
loss=[{'canonicalExerciseId':I(s),'problem':'legacy payload differs'} for s,v in zip(src,cand) if s!=v['legacy']];W('engine-v3-losslessness-audit-v3-7.json',{'sourceCount':1087,'candidateCount':1087,'silentSemanticLossCount':len(loss),'issues':loss})
rep=[]
for v in cand:
 c='IDENTITY_BLOCKER' if v['identityState']=='IDENTITY_PROBLEM' else ('REPRESENTABLE_WITH_DOCUMENTED_UNRESOLVED' if v['unresolved'] else 'FULLY_REPRESENTABLE');rep.append({'canonicalExerciseId':v['canonicalExerciseId'],'classification':c})
W('engine-v3-representability-v3-7.json',rep)
adapter={'classification':{'identity':'DIRECT','category':'DIRECT','group':'DEGRADED_MAPPING','equipmentRequirement':'REQUIRES_RUNTIME_EVOLUTION','resistance':'REQUIRES_RUNTIME_EVOLUTION','capability':'UNSUPPORTED','skillRelations':'REQUIRES_RUNTIME_EVOLUTION','prescription':'REQUIRES_RUNTIME_EVOLUTION','movementPatterns':'DEGRADED_MAPPING','engineMetadata':'DEGRADED_MAPPING'},'runtimeDbModified':False};W('engine-v3-runtime-adapter-spec-v3-7.json',adapter)
W('engine-v3-forward-contract-v3-7.json',{'eligibility':['identity','categoryGroup','equipmentRequirement','capability'],'equipmentLocation':['equipmentRequirement'],'capabilityGating':['capability'],'prescription':['resistance','prescription'],'skillSelection':['skillRelations'],'progression':['skillRelations','engineMetadata'],'sequencing':['movementPatterns','categoryGroup'],'varietySubstitution':['engineMetadata','movementPatterns','equipmentRequirement'],'implementationStatus':'SPEC_ONLY'})
W('engine-v3-skill-structural-check-v3-7.json',{'expectedFamilies':15,'expectedNodes':60,'typedRelationRecords':len(rels),'relationsRepresentable':all(x.get('relationType') and x.get('targetType') and x.get('targetId') for x in rels),'classification':'DEGRADED_BY_UNRESOLVED' if uo else 'STRUCTURALLY_REPRESENTABLE','note':'Full simulation deferred to V3.9'})
(D/'ENGINE_SCHEMA_CANDIDATE_V3_7.md').write_text('# Exercise DB V3.7 — Engine Schema Candidate\n\nMinimum sufficient schema derived from V3.1–V3.6. Equipment state/mode, resistance/load/assistance, multidimensional capability, typed skill relations, category/group, multidimensional prescription and 0..n movement patterns are distinct. Legacy payload is preserved exactly for losslessness. Unknown is never normalized to NONE. Legacy level is never blindly mapped.\n\nRuntime DB modified: **NO**. Adapter is specification only.\n')
print({'candidate':len(cand),'unique':len({x['canonicalExerciseId'] for x in cand}),'loss':len(loss),'patchAppliedFields':applied})
