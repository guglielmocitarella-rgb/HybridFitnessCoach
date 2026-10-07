#!/usr/bin/env python3
import hashlib,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
cand=ROOT/"exercise-db-canonical/exercise-db-v3-final-candidate.json"
contract_path=ROOT/"exercise-db-canonical/step8a-pd-semantic-contract.json"
EXPECTED="0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a"
FIELDS=["strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"]
errors=[]
actual=hashlib.sha256(cand.read_bytes()).hexdigest()
if actual!=EXPECTED: errors.append(f"candidate SHA mismatch: {actual}")
c=json.loads(contract_path.read_text())
data=json.loads(cand.read_text())
if c.get("frozenCandidateSha256")!=EXPECTED: errors.append("contract frozenCandidateSha256 mismatch")
if set(c.get("capabilityContracts",{}))!=set(FIELDS): errors.append("all four capability contracts required")
pd={f:0 for f in FIELDS}; terminal=0
for ex in data:
    cap=ex.get("capability",{})
    for f in FIELDS:
        v=cap.get(f)
        if v=="PRESCRIPTION_DEPENDENT": pd[f]+=1
        if v=="EVIDENCE_UNRESOLVED": terminal+=1
if sum(pd.values())!=441: errors.append(f"PD count {sum(pd.values())} != 441")
if pd!={"strengthRequirement":441,"technicalDifficulty":0,"mobilityRequirement":0,"balanceCoordinationRequirement":0}: errors.append(f"unexpected PD distribution {pd}")
# Authoritative terminal accounting is field-level and frozen by Step 6/7B.
step7=json.loads((ROOT/"exercise-db-canonical/step7b-structural-validation.json").read_text())
terminal_authoritative=step7["capabilities"]["EVIDENCE_UNRESOLVED"]
if terminal_authoritative!=48 or c.get("terminalCount")!=48: errors.append("terminal count != 48")
required={"ELIGIBLE","INELIGIBLE","EXERCISE_REQUIREMENT_UNRESOLVED","ATHLETE_CAPABILITY_UNKNOWN","INVALID_PRESCRIPTION_CONTEXT","UNSUPPORTED_CONTEXT","TERMINAL_CANONICAL_STATE"}
if not required.issubset(set(c.get("outcomeStates",[]))): errors.append("required outcome states missing")
if c.get("v2DependencyPolicy",{}).get("V2_DEPENDENCY_FOR_PD_RESOLUTION")!=0: errors.append("V2 dependency must be 0")
imm=c.get("canonicalImmutabilityPolicy",{})
if not imm.get("canonicalPdRemainPd") or imm.get("writeBackResolvedValue"): errors.append("canonical immutability violated")
sc=set(c.get("step8ScenarioClasses",{}))
if not {"NO_CONTEXT","SUFFICIENT_VALID_CONTEXT","INSUFFICIENT_CONTEXT","INVALID_CONTEXT","BOUNDARY_CONTEXT"}.issubset(sc): errors.append("Step 8 scenario classes missing")
vec=c.get("contractTestVectors",[])
for f in FIELDS:
    if not any(v.get("capability")==f for v in vec): errors.append(f"test vector missing for {f}")
a=c.get("applicabilitySummary",{})
if a.get("CONTRACT_COVERED")!=441 or a.get("CONTRACT_NOT_APPLICABLE")!=0 or a.get("CONTRACT_AMBIGUOUS")!=0: errors.append("441 applicability accounting invalid")
if c.get("unresolvedDesignQuestions"): errors.append("unresolved design questions remain")
if c.get("blockingFindings"): errors.append("blocking findings remain")
if c.get("terminalPolicy",{}).get("terminalResolvableByPrescription") is not False: errors.append("terminal separation invalid")
report={"candidateSha256":actual,"records":len(data),"pdDistribution":pd,"canonicalPdCount":sum(pd.values()),"terminalCountAuthoritative":terminal_authoritative,"contractCovered":a.get("CONTRACT_COVERED"),"contractAmbiguous":a.get("CONTRACT_AMBIGUOUS"),"v2Dependency":c.get("v2DependencyPolicy",{}).get("V2_DEPENDENCY_FOR_PD_RESOLUTION"),"errors":errors,"result":"PASS" if not errors else "FAIL"}
out=ROOT/"exercise-db-canonical/step8a-pd-semantic-contract-validation.json"
out.write_text(json.dumps(report,indent=2)+"\n")
print(json.dumps(report,indent=2))
sys.exit(1 if errors else 0)
