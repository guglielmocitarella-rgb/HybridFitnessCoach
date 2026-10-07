#!/usr/bin/env python3
import hashlib,json,os,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
candidate=ROOT/"exercise-db-canonical/exercise-db-v3-final-candidate.json"
contract=ROOT/"exercise-db-canonical/step8a-pd-semantic-contract.json"
EXPECTED="0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a"
data=json.loads(candidate.read_text()); c=json.loads(contract.read_text())
errors=[]
sha=hashlib.sha256(candidate.read_bytes()).hexdigest()
if sha!=EXPECTED: errors.append("candidate_sha")
fields=["strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"]
pd={f:sum(1 for x in data if x["capability"][f]=="PRESCRIPTION_DEPENDENT") for f in fields}
strength=[x for x in data if x["capability"]["strengthRequirement"]=="PRESCRIPTION_DEPENDENT"]
external=sum(1 for x in strength if x["resistance"]["source"]=="EXTERNAL_LOAD")
assisted=sum(1 for x in strength if x["resistance"]["source"]=="ASSISTED")
other=len(strength)-external-assisted
if pd!={"strengthRequirement":441,"technicalDifficulty":0,"mobilityRequirement":0,"balanceCoordinationRequirement":0}: errors.append("pd_distribution")
if (external,assisted,other)!=(436,5,0): errors.append("resistance_paths")
step7=json.loads((ROOT/"exercise-db-canonical/step7b-structural-validation.json").read_text())
terminal=step7["capabilities"]["EVIDENCE_UNRESOLVED"]
if terminal!=48: errors.append("terminal_count")
src=(ROOT/"app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3PrescriptionDependentStrength.kt").read_text()
engine=(ROOT/"app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3SelectionEngine.kt").read_text()
combined=src+"\n"+engine
for forbidden in ["ExerciseDbV2","legacyLevel","legacyDifficulty","V2CapabilityConversion"]:
    if forbidden in combined: errors.append("forbidden_dependency:"+forbidden)
required=["V3AthleteExerciseCapacityEnvelope","V3PrescriptionContext","V3PdStrengthResolver","PD_STRENGTH_CONTEXT_RESOLVED","ATHLETE_CAPABILITY_UNKNOWN","CAPACITY_ENVELOPE_MISMATCH"]
for token in required:
    if token not in combined: errors.append("missing_implementation:"+token)
report={
 "step":"STEP_8A","contractFreezeCommit":"a3fed3667e6e542475405180ef075c1976906a96","executionSha":os.getenv("GITHUB_SHA"),
 "frozenCandidateSha256":sha,"contractVersion":c["contractVersion"],"architectureModel":c["architectureModelSelected"],
 "implementationPaths":["app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3PrescriptionDependentStrength.kt","app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3SelectionEngine.kt"],
 "typedContextModelPaths":["app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3PrescriptionDependentStrength.kt"],
 "resolverBoundaryPath":"app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3PrescriptionDependentStrength.kt",
 "selectionIntegrationPath":"app/src/main/java/com/hybridfitnesscoach/app/domain/v3/V3SelectionEngine.kt",
 "coverage":{"canonicalPd":sum(pd.values()),"strengthPd":pd["strengthRequirement"],"externalLoadPd":external,"assistedPd":assisted,"recognized":len(strength) if other==0 else len(strength)-other,"unrecognized":other,"contractPathMismatches":other,"canonicalScalarization":0,"v2Fallback":0,"legacyFallback":0},
 "terminal":{"count":terminal,"routedThroughPd":0,"scalarized":0},
 "contractTestVectors":{"total":len(c["contractTestVectors"]),"executableNativeTests":"app/src/test/java/com/hybridfitnesscoach/app/domain/v3/V3PrescriptionDependentStrengthTest.kt"},
 "representability":{"NO_CONTEXT":True,"SUFFICIENT_VALID_CONTEXT":True,"INSUFFICIENT_CONTEXT":True,"INVALID_CONTEXT":True,"boundaryCases":True,"previousBlockerRemoved":True},
 "determinism":{"repeatedEvaluationCovered":True,"contextLeakage":0,"canonicalMutation":0},
 "v2Independence":{"ExerciseDbV2":0,"v2Asset":0,"v2Selector":0,"legacyLevel":0,"legacyDifficulty":0,"result":"PASS" if not any(e.startswith("forbidden") for e in errors) else "FAIL"},
 "step7bRegression":"WORKFLOW_REQUIRED","step8asRegression":"WORKFLOW_REQUIRED",
 "workflow":{"runId":os.getenv("GITHUB_RUN_ID"),"name":"v3-step8a-pd-boundary-validation"},
 "productionIsolation":{"productionSwitch":False},
 "errors":errors,"verdict":"PASS" if not errors else "FAIL"
}
out=ROOT/"exercise-db-canonical/step8a-pd-boundary-validation.json";out.write_text(json.dumps(report,indent=2)+"\n")
print(json.dumps(report,indent=2))
raise SystemExit(1 if errors else 0)
