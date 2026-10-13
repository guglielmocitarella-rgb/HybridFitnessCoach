#!/usr/bin/env python3
import json, hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1]; C=R/"exercise-db-canonical"
load=lambda p: json.loads((C/p).read_text(encoding="utf-8"))
contract=load("semantic-disposition-step5.json"); v=load("native-capability-r5-set-validation.json"); m=load("native-capability-r5-materialization-validation.json"); cand=load("exercise-db-v3-10c-r4-candidate.json")
assert v["counts"]=={"universe":3835,"resolvedInUniverse":3787,"terminalInUniverse":48,"trueMissing":0,"authoritativeOutOfUniverse":214}
assert m["patch"]["inUniverseParity"]=="3787/3787"
ds=contract["decisions"]; assert len(ds)==214 and len({x["semanticKey"] for x in ds})==214
assert sum(x["initialCategory"]=="PRESCRIPTION_DEPENDENT_VS_R5_SCALAR" for x in ds)==55
assert sum(x["initialCategory"]=="R3_VS_R5" for x in ds)==9
assert sum(x["finalDisposition"]=="EXCLUDED_NO_TARGET" for x in ds)==148
assert sum(x["finalDisposition"]=="NO_OP" for x in ds)==2
assert sum(x["finalDisposition"] in ("PRESERVE_PRESCRIPTION_DEPENDENT","USE_R5_SCALAR") for x in ds)==64
records=cand if isinstance(cand,list) else cand.get("exercises",cand.get("records",[])); pd=set()
for r in records:
 c=r.get("capability",{})
 for f in ("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"):
  if c.get(f)=="PRESCRIPTION_DEPENDENT": pd.add(r["canonicalExerciseId"]+"::"+f)
assert len(pd)==441
for x in ds:
 if x["initialCategory"]=="PRESCRIPTION_DEPENDENT_VS_R5_SCALAR": assert x["semanticKey"] in pd and x["finalDisposition"]=="PRESERVE_PRESCRIPTION_DEPENDENT"
payload=json.dumps(sorted((x["semanticKey"],x["finalDisposition"],x["precedence"],x["candidateBuilderAction"]) for x in ds),separators=(",",":"))
print(json.dumps({"result":"PASS","precedenceRequired":0,"prescriptionDependent":len(pd),"orphan":148,"redundant":2,"universe":3835,"resolved":3787,"terminal":48,"trueMissing":0,"materialized":"3787/3787","determinismSha256":hashlib.sha256(payload.encode()).hexdigest()},sort_keys=True))
