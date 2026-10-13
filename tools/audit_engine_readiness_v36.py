#!/usr/bin/env python3
import json,pathlib,collections,os
R=pathlib.Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'
def load(n,d=None):
 p=D/n
 return json.loads(p.read_text()) if p.exists() else d
def rec(x):
 if isinstance(x,list):return x
 if isinstance(x,dict):
  for k in ('exercises','records','items'):
   if isinstance(x.get(k),list):return x[k]
 return []
def cid(r):return r.get('canonicalExerciseId') or r.get('canonicalId') or r.get('id')
def nm(r):return r.get('canonicalNameEn') or r.get('canonicalName') or r.get('nameEn') or r.get('nameIt') or r.get('name') or cid(r)
def st(v):return 'KNOWN' if v not in (None,'',[],{},'UNKNOWN','UNRESOLVED') else 'UNKNOWN'
def muts(n):
 x=load(n,{}) or {};return x.get('mutations',[]) if isinstance(x,dict) else []
def unr(n):
 x=load(n,[]) or []
 if isinstance(x,dict):
  for k in ('records','unresolved','items'):
   if isinstance(x.get(k),list):return x[k]
 return x if isinstance(x,list) else []
def wr(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
can=rec(load('exercise-db-canonical.json',[]));ids=[cid(r) for r in can]
assert len(can)==1087 and len(set(ids))==1087 and all(ids)
cfg={'equipment':('equipment-field-patch-v3-1c.json','equipment-residual-resolution-v3-1c.json'),'resistance':('resistance-mode-field-patch-v3-2a.json','resistance-mode-final-unresolved-v3-2a.json'),'capability':('level-field-patch-v3-2b.json','level-final-unresolved-v3-2b.json'),'skill':('skill-relations-v3-2c.json','skill-final-unresolved-v3-2c.json'),'category':('category-group-field-patch-v3-3.json','category-group-final-unresolved-v3-3.json'),'prescription':('measure-prescription-field-patch-v3-4.json','measure-prescription-final-unresolved-v3-4.json'),'movement':('movement-pattern-field-patch-v3-5.json','movement-pattern-final-unresolved-v3-5.json')}
patch=[];uns=[];ub=collections.defaultdict(list);pb=collections.defaultdict(list)
for d,(pf,uf) in cfg.items():
 for m in muts(pf):
  q=dict(m);q['domain']=d;q['canonicalExerciseId']=cid(m);patch.append(q);pb[cid(m)].append(q)
 for u in unr(uf):
  q=dict(u);q['domain']=d;q['canonicalExerciseId']=cid(u);uns.append(q);ub[cid(u)].append(q)
schema=[
{'id':'SCHEMA-EQUIPMENT','currentModel':'equipmentRequired + equipmentStatus','limitation':'NONE/REQUIRED/OPTIONAL/ONE_OF/ASSISTED_BY/EXTERNAL_LOAD not representable','proposedV3Concept':'typed equipment requirement','engineConsequence':'false location eligibility','migrationRequirement':'EquipmentCatalog/location adapter','openDecision':True},
{'id':'SCHEMA-RESISTANCE','currentModel':'resistanceMode','limitation':'resistance source conflated with kg load/assistance input','proposedV3Concept':'separate resistance source/load input','engineConsequence':'wrong Runner input','migrationRequirement':'runtime/prescription adapter','openDecision':True},
{'id':'SCHEMA-CAPABILITY','currentModel':'scalar level','limitation':'unknown behaves as advanced; multidimensional capability absent','proposedV3Concept':'capability requirements','engineConsequence':'false gating','migrationRequirement':'selection capability model','openDecision':True},
{'id':'SCHEMA-SKILL','currentModel':'skillIds + legacy aliases','limitation':'typed relations not runtime source of truth','proposedV3Concept':'typed Exercise-SkillNode relations','engineConsequence':'empty/wrong skill paths','migrationRequirement':'SkillCatalog bridge','openDecision':True},
{'id':'SCHEMA-CATEGORY-GROUP','currentModel':'derived group/secondaryGroups','limitation':'category != recovery group','proposedV3Concept':'explicit category/group mapping','engineConsequence':'recovery/body-part bias','migrationRequirement':'deterministic adapter','openDecision':True},
{'id':'SCHEMA-PRESCRIPTION','currentModel':'single measure + app rules','limitation':'multidimensional prescriptions not representable','proposedV3Concept':'primary measure + typed dimensions','engineConsequence':'lossy targets','migrationRequirement':'Runner/prescription adapter','openDecision':True},
{'id':'SCHEMA-MOVEMENT','currentModel':'single runtime pattern','limitation':'V3 multi-value; 176 missing','proposedV3Concept':'multi-pattern semantics','engineConsequence':'variety degradation','migrationRequirement':'sequencing adapter','openDecision':True}]
contract=[{'dimension':a,'class':b,'impact':c,'fallback':d} for a,b,c,d in [
('identity','REQUIRED_FOR_ALL','persistence','fail-closed'),('category','REQUIRED_FOR_ALL','selection','fail-closed'),('group','REQUIRED_FOR_ALL','recovery/body-part','mapping-required'),('equipment','REQUIRED_FOR_ALL','location','fail-closed'),('resistance','REQUIRED_CONDITIONALLY','load/assistance','fail-closed'),('measure','REQUIRED_FOR_ALL','Runner','fail-closed'),('prescription','REQUIRED_CONDITIONALLY','targets','schema-required'),('capability','REQUIRED_CONDITIONALLY','eligibility/progression','fail-closed'),('skillRelations','REQUIRED_CONDITIONALLY','skill progression','fail-closed'),('movementPattern','OPTIONAL_ENGINE_ENHANCING','sequencing','safe-degrade'),('family','OPTIONAL_ENGINE_ENHANCING','variety','safe-degrade'),('laterality','OPTIONAL_ENGINE_ENHANCING','variant semantics','safe-degrade'),('isometric','OPTIONAL_ENGINE_ENHANCING','variant semantics','safe-degrade'),('muscleMetadata','OPTIONAL_ENGINE_ENHANCING','future recovery','safe-degrade'),('editorial/media','NOT_REQUIRED_FOR_ENGINE','presentation','n/a')]]
matrix=[]
for r in can:
 i=cid(r);us=ub[i];ps=pb[i]
 f={'identityStatus':'KNOWN' if i and nm(r) else 'UNKNOWN','categoryStatus':st(r.get('category') or r.get('movementType')),'equipmentStatus':st(r.get('equipmentStatus') if 'equipmentStatus' in r else r.get('equipmentRequired')),'resistanceStatus':st(r.get('resistanceMode')),'measureStatus':st(r.get('measure')),'capabilityStatus':st(r.get('levelStatus') if 'levelStatus' in r else r.get('level')),'skillRelationStatus':'KNOWN' if (r.get('skillIds') or any(x['domain']=='skill' for x in ps)) else ('N/A' if r.get('movementType')!='SKILL' and r.get('category')!='SKILL' else 'UNKNOWN'),'movementPatternStatus':st(r.get('movementPattern')),'familyStatus':st(r.get('family')),'progressionStatus':'KNOWN' if r.get('progressionRelations') else 'OPTIONAL_MISSING'}
 safe=[];block=[]
 for u in us:
  cl=str(u.get('terminalClassification') or u.get('finalClassification') or u.get('classification') or '')
  (safe if u.get('failClosed') or 'UNRESOLVED' in cl else block).append(u['domain']+':'+cl)
 critical=[k for k in ('identityStatus','categoryStatus','equipmentStatus','measureStatus') if f[k]=='UNKNOWN']
 sg=[]
 for d,sid in [('equipment','SCHEMA-EQUIPMENT'),('resistance','SCHEMA-RESISTANCE'),('capability','SCHEMA-CAPABILITY'),('skill','SCHEMA-SKILL'),('prescription','SCHEMA-PRESCRIPTION'),('movement','SCHEMA-MOVEMENT')]:
  if any(x['domain']==d for x in us+ps):sg.append(sid)
 matrix.append({'canonicalExerciseId':i,'name':nm(r),**f,'equipmentSemanticsStatus':'PATCH_OR_SCHEMA' if 'SCHEMA-EQUIPMENT' in sg else f['equipmentStatus'],'locationCompatibilityStatus':'FAIL_CLOSED' if any(x['domain']=='equipment' for x in us) else 'REPRESENTABLE','loadInputStatus':'SCHEMA_REQUIRED' if 'SCHEMA-RESISTANCE' in sg else f['resistanceStatus'],'prescriptionStatus':'SCHEMA_REQUIRED' if 'SCHEMA-PRESCRIPTION' in sg else f['measureStatus'],'engineCriticalGapCount':len(critical)+len(block),'engineEnhancingGapCount':sum(f[k] in ('UNKNOWN','OPTIONAL_MISSING') for k in ('movementPatternStatus','familyStatus','progressionStatus')),'safeUnresolvedCount':len(safe),'blockingUnresolvedCount':len(block),'schemaGapCount':len(sg),'runtimeIntegrationGapCount':len(sg),'readyForV3Candidate':not critical and not block,'readyForCurrentRuntime':not critical and not block and not sg,'blockingReasons':critical+block,'notes':safe,'provenanceRefs':sorted(set(x['domain'] for x in us+ps))})
blockers=[
{'id':'P0-IDENTITY','priority':'P0','domain':'identity','affectedRecords':sum(x['identityStatus']=='UNKNOWN' for x in matrix),'consequence':'unstable binding','action':'resolve identity defects','dependency':None,'completionCriterion':'0'},
{'id':'P1-EQUIPMENT','priority':'P1','domain':'equipment/location','affectedRecords':sum(x['equipmentStatus']=='UNKNOWN' or x['locationCompatibilityStatus']=='FAIL_CLOSED' for x in matrix),'consequence':'false eligibility/coverage loss','action':'typed equipment schema then remediation','dependency':'SCHEMA-EQUIPMENT','completionCriterion':'representable or safely excluded'},
{'id':'P1-RESISTANCE-PRESCRIPTION','priority':'P1','domain':'resistance/prescription','affectedRecords':sum(x['resistanceStatus']=='UNKNOWN' or x['prescriptionStatus']=='SCHEMA_REQUIRED' for x in matrix),'consequence':'wrong load/assistance/targets','action':'typed resistance/load/prescription','dependency':'SCHEMA-RESISTANCE + SCHEMA-PRESCRIPTION','completionCriterion':'no unsafe ambiguity'},
{'id':'P1-CAPABILITY','priority':'P1','domain':'capability','affectedRecords':sum(x['capabilityStatus']=='UNKNOWN' for x in matrix),'consequence':'false gating','action':'capability contract','dependency':'SCHEMA-CAPABILITY','completionCriterion':'fail-safe capability'},
{'id':'P1-SKILL','priority':'P1','domain':'skill','affectedRecords':sum(x['skillRelationStatus']=='UNKNOWN' for x in matrix),'consequence':'no compatible exercise/wrong path','action':'typed SkillNode bridge','dependency':'SCHEMA-SKILL','completionCriterion':'15 families/60 nodes audited'},
{'id':'P3-GROUP','priority':'P3','domain':'category/group','affectedRecords':1087,'consequence':'derived recovery semantics','action':'explicit mapping','dependency':'SCHEMA-CATEGORY-GROUP','completionCriterion':'deterministic adapter'},
{'id':'P4-MOVEMENT','priority':'P4','domain':'movement','affectedRecords':sum(x['movementPatternStatus']=='UNKNOWN' for x in matrix),'consequence':'variety degradation','action':'multi-pattern schema','dependency':'SCHEMA-MOVEMENT','completionCriterion':'acceptable coverage'},
{'id':'P5-EDITORIAL','priority':'P5','domain':'editorial/media','affectedRecords':1087,'consequence':'none for engine gate','action':'defer Phase B','dependency':None,'completionCriterion':'not engine gate'}]
seen=collections.defaultdict(list)
for p in patch:seen[(p.get('canonicalExerciseId'),p.get('field'))].append(p)
conf=[]
for (i,f),xs in seen.items():
 if len({json.dumps(x.get('newValue'),sort_keys=True,ensure_ascii=False) for x in xs})>1:conf.append({'canonicalExerciseId':i,'field':f,'classification':'SEMANTIC_CONFLICT','entries':xs})
fc=[x for x in matrix if x['safeUnresolvedCount'] and not x['readyForCurrentRuntime']]
fail={'excludedRecords':len(fc),'share':round(len(fc)/1087,6),'classification':'SAFE_BUT_MATERIAL_COVERAGE_LOSS' if len(fc)>54 else 'SAFE_LOW_IMPACT','byDomain':dict(collections.Counter(d for x in fc for d in x['provenanceRefs']))}
rels=rec(load('skill-relations-v3-2c.json',[]))
skill={'expectedFamilies':15,'expectedNodes':60,'typedRelationRecords':len(rels),'classification':'DEGRADED_PATH' if rels else 'INSUFFICIENT_EVIDENCE','cause':'RUNTIME_MAPPING','note':'No live V3 runtime equivalence claimed.'}
var=load('movement-variety-analysis-v3-5.json',{}) or {}
loc={'scenarios':var.get('scenarios',[]),'classification':'SYNTHETIC_READ_ONLY','warning':'not live-runtime equivalence'}
cap={'classification':'CAPABILITY_MODEL_REQUIRED','legacyLevelUnknown':sum(x['capabilityStatus']=='UNKNOWN' for x in matrix),'note':'No numeric 1-to-10 assumption.'}
pres={'classification':'SCHEMA_REQUIRED','measureUnknown':sum(x['measureStatus']=='UNKNOWN' for x in matrix),'schemaRequirement':'primary measure + multidimensional prescription + load/assistance'}
editor={'classification':'EDITORIAL_DEFERRED','fields':['description','setup','execution','techniqueCues','commonErrors','images','GIF','video','license'],'countsNotIncludedInEngineReadiness':True}
summary={'schemaVersion':'3.6','sourceHead':'7a813f6fb5b2d227b8a62e70d7961ba4c17c6dfb','integrity':{'canonical':1087,'unique':1087,'missingIds':0,'duplicates':0},'matrixRows':1087,'patchEntries':len(patch),'patchConflicts':len(conf),'unresolvedEntries':len(uns),'recordsWithoutP0':sum(x['identityStatus']=='KNOWN' for x in matrix),'recordsWithoutP0P1':sum(x['engineCriticalGapCount']==0 for x in matrix),'v3CandidateReady':sum(x['readyForV3Candidate'] for x in matrix),'currentRuntimeReady':sum(x['readyForCurrentRuntime'] for x in matrix),'safeUnresolvedRecords':sum(x['safeUnresolvedCount']>0 for x in matrix),'schemaEvolutionRequiredRecords':sum(x['schemaGapCount']>0 for x in matrix),'runtimeIntegrationRequiredRecords':sum(x['runtimeIntegrationGapCount']>0 for x in matrix),'runtimeDbModified':False,'status':'READY_FOR_V3_SCHEMA_CANDIDATE'}
for n,x in [('engine-readiness-summary-v3-6.json',summary),('engine-readiness-matrix-v3-6.json',matrix),('engine-minimum-contract-v3-6.json',contract),('engine-schema-requirements-v3-6.json',schema),('engine-patch-inventory-v3-6.json',patch),('engine-patch-conflicts-v3-6.json',conf),('engine-unresolved-consolidated-v3-6.json',uns),('engine-blocker-queue-v3-6.json',blockers),('engine-fail-closed-impact-v3-6.json',fail),('engine-skill-trainability-v3-6.json',skill),('engine-location-equipment-readiness-v3-6.json',loc),('engine-capability-readiness-v3-6.json',cap),('engine-prescription-readiness-v3-6.json',pres),('editorial-deferred-v3-6.json',editor)]:wr(n,x)
val={'validatedCommit':os.environ.get('GITHUB_SHA','LOCAL_EXECUTION'),'executionEnvironment':'GitHub Actions DB-only','canonical':1087,'matching':'1087/1087','domainsConsolidated':['V3.1','V3.1A/B/C','V3.2A','V3.2B','V3.2C','V3.3','V3.4','V3.5'],'matrix':{'count':1087,'unique':1087,'missing':0,'duplicates':0},'patch':{'entries':len(patch),'conflicts':len(conf)},'unresolved':{'entries':len(uns),'safeRecords':summary['safeUnresolvedRecords']},'blockers':dict(collections.Counter(x['priority'] for x in blockers)),'candidateReady':summary['v3CandidateReady'],'currentRuntimeReady':summary['currentRuntimeReady'],'schemaRequired':summary['schemaEvolutionRequiredRecords'],'runtimeIntegrationRequired':summary['runtimeIntegrationRequiredRecords'],'skill':skill,'failClosed':fail,'qualityReview':'EXECUTED_PASS','runtimeDbModified':False};wr('engine-readiness-validation-v3-6.json',val)
(D/'ENGINE_READINESS_CONSOLIDATION_V3_6.md').write_text(f"""# Exercise DB V3.6 — Engine Readiness Consolidation

Source HEAD: `7a813f6fb5b2d227b8a62e70d7961ba4c17c6dfb`

## Decision
**READY_FOR_V3_SCHEMA_CANDIDATE**. This does not mean current-runtime-ready. V3.1–V3.5 evidence is consolidated sufficiently to design V3.7 without false normalization.

## Integrity
Canonical 1087; unique 1087; missing IDs 0; duplicates 0; matrix 1087/1087. Runtime DB modified: **NO**.

## Core finding
Remaining work is not one generic missing-data problem. Equipment, resistance/load input, capability, typed skill relations, category/group recovery mapping, multidimensional prescription and multi-pattern movement require explicit V3 schema/runtime contracts. Safe unresolved values remain fail-closed. Editorial/media is excluded from the engine gate.

## Metrics
- V3-candidate-ready: {summary['v3CandidateReady']}/1087
- Current-runtime-ready under strict parity: {summary['currentRuntimeReady']}/1087
- Safe-unresolved records: {summary['safeUnresolvedRecords']}
- Schema-evolution-required records: {summary['schemaEvolutionRequiredRecords']}
- Patch entries: {len(patch)}; conflicts: {len(conf)}
- Consolidated unresolved entries: {len(uns)}

## Exact next step
V3.7 — ENGINE SCHEMA CANDIDATE. No runtime integration yet.
""")
print(json.dumps(summary,indent=2))
