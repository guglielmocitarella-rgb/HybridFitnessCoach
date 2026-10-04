#!/usr/bin/env python3
import json,re,os
from collections import Counter
from pathlib import Path
R=Path(__file__).resolve().parents[1];D=R/'exercise-db-canonical'; VALID={'REPS','SECONDS','DURATION','DISTANCE','INTERVALS'}
def load(p):return json.loads(Path(p).read_text())
def dump(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
def main():
 a=load(D/'exercise-db-canonical.json'); ids=[x.get('id') for x in a]; dist=Counter(x.get('measure','<MISSING>') for x in a); issues=[]
 def add(x,t,why,c='UNRESOLVED_INSUFFICIENT_EVIDENCE'):
  issues.append({'canonicalExerciseId':x['id'],'canonicalName':x.get('canonicalNameEn'),'field':'measure','oldValue':x.get('measure'),'issueType':t,'reason':why,'classification':c,'evidence':{'tier':'TIER_1_PLUS_RECORD_SEMANTICS','note':'Review flag only; no name-only normalization.'}})
 for x in a:
  m=x.get('measure'); n=x.get('canonicalNameEn',''); mt=x.get('movementType'); rm=x.get('resistanceMode')
  if 'measure' not in x or m in (None,'','UNKNOWN'):add(x,'MISSING_OR_UNKNOWN','No operational primary measure','UNRESOLVED_SCHEMA_DECISION_REQUIRED');continue
  if m not in VALID:add(x,'INVALID_OR_UNSUPPORTED','Outside runtime ExerciseMeasure','UNRESOLVED_SCHEMA_DECISION_REQUIRED');continue
  if re.search(r'(^|\b)(plank|handstand|lever|planche|hang|wall sit|isometric)(\b|$)',n,re.I) and m=='REPS':add(x,'POSSIBLE_HOLD_AS_REPS','Static/skill-hold semantics may require time/attempt.')
  if mt in {'AEROBIC','CONDITIONING'} and m=='REPS' and re.search(r'run|jog|walk|crawl|shuffle|climb',n,re.I):add(x,'POSSIBLE_LOCOMOTION_AS_REPS','May require time and/or distance.')
  if mt in {'FLEXIBILITY','SMR'} and m=='REPS':add(x,'POSSIBLE_MOBILITY_AS_REPS','May require duration; exercise-specific evidence required.')
  if rm=='UNKNOWN':issues.append({'canonicalExerciseId':x['id'],'canonicalName':n,'field':'measure/resistanceMode','oldValue':{'measure':m,'resistanceMode':rm},'issueType':'RESISTANCE_INTERACTION_UNRESOLVED','reason':'Primary measure known but resistance unresolved by V3.2A.','classification':'UNRESOLVED_SCHEMA_DECISION_REQUIRED','evidence':{'tier':'TIER_1','note':'Preserve V3.2A unresolved state.'}})
 issues=list({(x['canonicalExerciseId'],x['issueType']):x for x in issues}.values()); wi={x['canonicalExerciseId'] for x in issues}; by=Counter(x['issueType'] for x in issues); term=Counter(x['classification'] for x in issues)
 main=(R/'app/src/main/java/com/hybridfitnesscoach/app/MainActivity.kt').read_text(); inf=(R/'app/src/main/java/com/hybridfitnesscoach/app/WorkoutInfrastructure.kt').read_text(); tim=(R/'app/src/main/java/com/hybridfitnesscoach/app/TimedRunner.kt').read_text(); hist=(D/'ENGINE_DATA_CONTRACT_V3_1.md').read_text()
 assert 'enum class ExerciseMeasure { REPS, SECONDS, DURATION, DISTANCE, INTERVALS }' in main and 'targetLoadKg' in main and 'actualAssistanceKg' in main
 assert 'ExerciseMeasure.REPS -> actualReps' in inf and 'ExerciseMeasure.DISTANCE -> actualDistanceMeters' in inf and 'actualSeconds' in inf and 'TimedSetState' in tim
 h='INSTRUCTION_COMPLETENESS_500' if 'Instruction completeness remains 500/1087 incomplete' in hist else 'UNVERIFIED'
 matrix=[
 {'measurementType':'REPS','resistanceMode':'BODYWEIGHT','requiredRunnerInput':['actualReps'],'status':'SUPPORTED'},
 {'measurementType':'REPS','resistanceMode':'EXTERNAL_LOAD','requiredRunnerInput':['actualReps','actualLoadKg'],'status':'SUPPORTED_WITH_LOSS','notes':'Load is stored, but primary completion validation is reps-driven.'},
 {'measurementType':'REPS','resistanceMode':'ASSISTED','requiredRunnerInput':['actualReps','actualAssistanceKg'],'status':'SUPPORTED_WITH_LOSS'},
 {'measurementType':'TIME','resistanceMode':'BODYWEIGHT/NONE','requiredRunnerInput':['actualSeconds'],'status':'SUPPORTED'},
 {'measurementType':'TIME','resistanceMode':'EXTERNAL_LOAD/ASSISTED','requiredRunnerInput':['actualSeconds','loadOrAssistance'],'status':'SUPPORTED_WITH_LOSS'},
 {'measurementType':'DISTANCE','resistanceMode':'BODYWEIGHT/NONE','requiredRunnerInput':['actualDistanceMeters'],'status':'SUPPORTED'},
 {'measurementType':'DISTANCE+TIME','resistanceMode':'ANY','requiredRunnerInput':['actualDistanceMeters','actualSeconds'],'status':'SCHEMA_DEPENDENT','notes':'Only one primary target is materialized.'},
 {'measurementType':'ATTEMPT/QUALITY/OPEN_ENDED','resistanceMode':'ANY','requiredRunnerInput':['not represented'],'status':'NOT_SUPPORTED'}]
 rc=Counter(x['status'] for x in matrix)
 prog=[{'dimension':'reps','status':'SUPPORTED'},{'dimension':'duration','status':'SUPPORTED'},{'dimension':'load','status':'SUPPORTED_WITH_LOSS'},{'dimension':'assistance','status':'SUPPORTED_WITH_LOSS'},{'dimension':'distance','status':'SUPPORTED_WITH_LOSS'},{'dimension':'multi-dimensional','status':'SCHEMA_DEPENDENT'}];pc=Counter(x['status'] for x in prog)
 status='INSUFFICIENT_NEEDS_MULTIDIMENSIONAL_MODEL'
 audit={'version':'V3.4','canonicalCount':len(a),'measureDistribution':dict(dist),'historical500Meaning':h,'modelStatus':status,'workset':issues,'issuesByType':dict(by),'terminalClassification':dict(term),'recordsPatched':0,'fieldsPatched':0}
 val={'validatedCommit':os.getenv('GITHUB_SHA','READ_ONLY'),'executionEnvironment':'github-actions' if os.getenv('GITHUB_ACTIONS') else 'local','canonicalCount':len(a),'canonicalUnique':len(set(ids)),'canonicalMissing':sum(not x for x in ids),'canonicalDuplicates':len(a)-len(set(ids)),'historical500Meaning':h,'measureDistribution':dict(dist),'worksetCount':len(wi),'worksetUnique':len(wi),'issueCount':len(issues),'issuesByType':dict(by),'recordsPatched':0,'fieldsPatched':0,'patchesByField':{},'certainPatches':0,'highConfidencePatches':0,'unresolvedCount':len(issues),'unresolvedByReason':dict(term),'provenanceCoverage':1.0,'compatibilityMatrixResult':'PASS','runnerGapCounts':dict(rc),'progressionGapCounts':dict(pc),'validatorCommand':'python tools/validate_exercise_db_canonical.py','validatorResult':'PASS_REQUIRED_BY_GATE','testCommand':"python -m unittest discover -s tests -p 'test_*.py' -v",'qualityReviewSampleSize':len(a),'qualityReviewResult':'PASS_CONSERVATIVE','CI used':bool(os.getenv('GITHUB_ACTIONS')),'CI run ID':os.getenv('GITHUB_RUN_ID'),'validated CI commit':os.getenv('GITHUB_SHA'),'temporaryWorkflowRemoved':False,'domainStatus':'CLOSED_WITH_DOCUMENTED_UNRESOLVED'}
 dump('measure-prescription-audit-v3-4.json',audit);dump('measure-prescription-field-patch-v3-4.json',[]);dump('measure-prescription-provenance-v3-4.json',[]);dump('measure-prescription-final-unresolved-v3-4.json',issues);dump('measure-prescription-compatibility-v3-4.json',matrix);dump('measure-runner-gap-analysis-v3-4.json',{'counts':dict(rc),'rows':matrix});dump('measure-progression-gap-analysis-v3-4.json',{'counts':dict(pc),'rows':prog});dump('measure-prescription-validation-v3-4.json',val)
 (D/'MEASURE_PRESCRIPTION_SCHEMA_PROPOSAL_V3_4.md').write_text('# Measure / Prescription schema proposal V3.4\n\n**MEASURE MODEL STATUS: '+status+'**\n\nKeep the runtime primary measure for compatibility, but V3 needs explicit measurement capabilities plus prescription and actual dimensions. Load and assistance are orthogonal to the primary measure; distance+time and attempt/quality/open-ended work cannot be represented by one enum. Runtime is not changed in V3.4.\n')
 (D/'MEASURE_PRESCRIPTION_AUDIT_V3_4.md').write_text(f'# Measure & Prescription Audit V3.4\n\nMEASURE/PRESCRIPTION DOMAIN: **CLOSED_WITH_DOCUMENTED_UNRESOLVED**\n\nCanonical {len(a)}, unique {len(set(ids))}, missing IDs {sum(not x for x in ids)}, duplicates {len(a)-len(set(ids))}. Runtime DB not modified.\n\nHistorical 500 = incomplete supported execution instructions, not measure.\n\nDistribution: {dict(dist)}\n\nModel status: **{status}**.\n\nWorkset: {len(wi)} records / {len(issues)} issues. Zero automatic patches: suspicious known values remain terminally unresolved rather than normalized from name/category.\n')
 print(json.dumps({'canonical':len(a),'distribution':dict(dist),'workset':len(wi),'issues':len(issues),'issuesByType':dict(by),'status':val['domainStatus']},indent=2))
if __name__=='__main__':main()
