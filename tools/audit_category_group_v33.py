#!/usr/bin/env python3
from collections import Counter
import json,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/'exercise-db-canonical'
ALLOWED={'PUSH','PULL','LEGS','CORE','CARDIO','MOBILITY','SKILL','OTHER'}
def adapter(r):
 m=str(r.get('movementType') or '').upper(); f=str(r.get('family') or '').upper()
 if m=='CORE' or 'CORE' in f or 'L_SIT' in f:return 'CORE'
 if m in {'AEROBIC','CONDITIONING'} or f=='CARDIO':return 'CARDIO'
 if m in {'MOBILITY','FLEXIBILITY','SMR'} or 'MOBILITY' in f or f=='STRETCH':return 'MOBILITY'
 if m=='SKILL' or f in {'PLANCHE','FRONT_LEVER','BACK_LEVER','HANDSTAND','HUMAN_FLAG','MUSCLE_UP','SKILL'}:return 'SKILL'
 if 'PULL' in f or f in {'BACK','BICEPS','UPRIGHT_ROW','SHRUG','PULLOVER','REAR_DELTOID'}:return 'PULL'
 if 'PUSH' in f or f in {'CHEST','CHEST_FLY','DIP','TRICEPS','SHOULDER','HANDSTAND_PUSH_UP'}:return 'PUSH'
 if f in {'LEG','SQUAT','HINGE','LOWER_BODY','SINGLE_LEG_SQUAT','NORDIC_HAMSTRING','LUNGE','STEP_UP','CALF','POSTERIOR_CHAIN'}:return 'LEGS'
 return 'OTHER'
def groups(r):
 out=[]
 for e in ((r.get('applicationMetadata') or {}).get('sourceEntries') or [])+((r.get('applicationMetadata') or {}).get('ambiguousSourceEntries') or []):
  g=(e.get('snapshot') or {}).get('group')
  if g:out.append(g)
 return sorted(set(out))
def main():
 rs=json.loads((DB/'exercise-db-canonical.json').read_text()); ids=[r.get('id') for r in rs]
 cats=Counter(); gs=Counter(); prov=Counter(); issues=[]; work={}; unresolved=[]
 for r in rs:
  rid=r.get('id'); c=r.get('category'); cats[str(c) if c is not None else '<MISSING>']+=1
  cp=((r.get('fieldProvenance') or {}).get('category') or {}).get('status','MISSING'); prov[cp]+=1
  rg=groups(r)
  for g in rg:gs[g]+=1
  rec=[]
  if c is None:rec.append(('CATEGORY_MISSING','UNRESOLVED_SCHEMA_DECISION_REQUIRED','Canonical category is required by current engine contract.'))
  elif c not in ALLOWED:rec.append(('CATEGORY_INVALID','UNRESOLVED_SOURCE_TAXONOMY_CONFLICT','Category is outside ExerciseCategory.'))
  elif c=='OTHER':rec.append(('CATEGORY_OTHER','UNRESOLVED_SCHEMA_DECISION_REQUIRED','OTHER is fail-safe but non-discriminating.'))
  if cp=='APP_RULE_RUNTIME_CONVERSION' and c!=adapter(r):rec.append(('CATEGORY_ADAPTER_CONFLICT','UNRESOLVED_SOURCE_TAXONOMY_CONFLICT','Persisted category disagrees with adapter reconstruction.'))
  if len(rg)>1:rec.append(('GROUP_SOURCE_CONFLICT','UNRESOLVED_SOURCE_TAXONOMY_CONFLICT','Runtime snapshots disagree on legacy group.'))
  if rec:
   work[rid]={'canonicalExerciseId':rid,'category':c,'categoryProvenance':cp,'movementType':r.get('movementType'),'family':r.get('family'),'runtimeGroups':rg,'issues':[{'type':a,'status':b,'reason':d} for a,b,d in rec]}
   for a,b,d in rec:
    issues.append(a); unresolved.append({'canonicalExerciseId':rid,'field':'category' if a.startswith('CATEGORY') else 'group','issue':a,'classification':b,'evidence':{'category':c,'categoryProvenance':cp,'movementType':r.get('movementType'),'family':r.get('family'),'runtimeGroups':rg},'reason':d})
 integrity={'canonical':len(rs),'unique':len(set(ids)),'missing':sum(not x for x in ids),'duplicates':len(ids)-len(set(ids))}
 audit={'schemaVersion':'3.3','sourceHead':'7089472638799bccb50ab7ff0e75508279191ce7','integrity':integrity,'contract':{'category':{'meaning':'Broad runtime functional class used by capability mapping/eligibility and planning.','classification':'ENGINE_CRITICAL','allowed':sorted(ALLOWED)},'group':{'meaning':'Legacy/body-part runtime label; not a canonical V3 top-level field.','classification':'ENGINE_ENHANCING','schemaDecision':'Do not alias to category; future anatomy must be explicit.'}},'categoryDistribution':dict(cats),'groupDistributionFromRuntimeSnapshots':dict(gs),'categoryProvenance':dict(prov),'worksetCount':len(work),'worksetUnique':len(work),'issueCount':len(issues),'issuesByType':dict(Counter(issues)),'records':list(work.values())}
 patch={'schemaVersion':'3.3','patches':[],'note':'No blind category/group normalization.'}; provenance={'schemaVersion':'3.3','entries':[],'coverage':'0/0'}
 final={'schemaVersion':'3.3','count':len(unresolved),'byReason':dict(Counter(x['classification'] for x in unresolved)),'records':unresolved}
 val={'schemaVersion':'3.3','validatedCommit':None,'executionEnvironment':'GitHub Actions DB-only','canonicalCount':len(rs),'canonicalUnique':len(set(ids)),'canonicalMissing':integrity['missing'],'canonicalDuplicates':integrity['duplicates'],'categoryDistribution':dict(cats),'groupDistribution':dict(gs),'worksetCount':len(work),'worksetUnique':len(work),'issueCount':len(issues),'issuesByType':dict(Counter(issues)),'recordsPatched':0,'fieldsPatched':0,'patchesByField':{},'certainPatches':0,'highConfidencePatches':0,'unresolvedCount':len(unresolved),'unresolvedByReason':final['byReason'],'provenanceCoverage':'0/0 patches (complete)','validatorCommand':'python tools/validate_exercise_db_canonical.py','validatorResult':'PENDING','testCommand':'python -m unittest tests.test_exercise_db_canonical tests.test_equipment_v31c tests.test_resistance_mode_v32a tests.test_level_capability_v32b tests.test_skill_bridge_v32c tests.test_category_group_v33 -v','testsExecuted':None,'testsPassed':None,'testsFailed':None,'testsErrors':None,'qualityReviewSampleSize':0,'qualityReviewResult':'PENDING','falsePositivesFound':0,'systematicIssuesFound':0,'correctionsApplied':0,'CI used':True,'CI run ID':None,'domainDecision':'PENDING'}
 for n,x in [('category-group-audit-v3-3.json',audit),('category-group-field-patch-v3-3.json',patch),('category-group-provenance-v3-3.json',provenance),('category-group-final-unresolved-v3-3.json',final),('category-group-validation-v3-3.json',val)]: (DB/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
 (DB/'CATEGORY_GROUP_AUDIT_V3_3.md').write_text(f"# Exercise DB V3.3 — Category & Group Audit\n\nSource HEAD: \`7089472638799bccb50ab7ff0e75508279191ce7\`\n\n**category — ENGINE_CRITICAL.** Broad functional class used by capability/planning.\n\n**group — ENGINE_ENHANCING legacy taxonomy.** Body-part/source label used by runtime filtering/recovery; not a top-level canonical V3 field and not interchangeable with category.\n\nIntegrity: {integrity}. Workset: {len(work)} records / {len(issues)} issues. No blind patches.\n")
 (DB/'CATEGORY_GROUP_SCHEMA_PROPOSAL_V3_3.md').write_text("# V3.3 Category / Group Schema Proposal\n\nProposal only; runtime unchanged. Retain \`category\` as broad functional engine class. Do not add \`group\` as its alias: preserve legacy group in application/source metadata until explicit bodyRegion/muscle taxonomy is adopted. Keep family, movementType and movementPattern separate.\n")
 print(json.dumps({'integrity':integrity,'category':dict(cats),'groups':dict(gs),'workset':len(work),'issues':dict(Counter(issues))},ensure_ascii=False))
if __name__=='__main__':main()
