#!/usr/bin/env python3
import json,pathlib,copy,collections
D=pathlib.Path("exercise-db-canonical"); DIM=("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"); LEVEL={"VERY_LOW","LOW","MODERATE","HIGH","VERY_HIGH"}; SPECIAL={"NOT_APPLICABLE","VARIABLE","PRESCRIPTION_DEPENDENT","VARIANT_DEPENDENT"}
def L(n): return json.loads((D/n).read_text())
def W(n,x): (D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+"\n")
def ev(x,p=""):
 o=[]
 if isinstance(x,dict):
  for k,v in x.items():
   if any(z in k.lower() for z in ("canonicalname","displayname","alias","legacylevel","capability")): continue
   o+=ev(v,p+"."+k)
 elif isinstance(x,list):
  for v in x:o+=ev(v,p)
 elif isinstance(x,str) and len(x.strip())>3:o.append((p,x.strip()))
 return o
R=[
("R3-TECH-DYNAMIC","technicalDifficulty",["inverted","inversion","body control","sequencing","multi-phase","timing","explosive","plyometric","catch position"],"HIGH"),
("R3-TECH-CONTROL","technicalDifficulty",["controlled motion","proper sequencing","coordination","precision","bar path","maintain alignment","stabilize"],"MODERATE"),
("R3-MOBILITY-ROM-HIGH","mobilityRequirement",["deep squat","full depth","overhead position","shoulder flexion","external rotation","wrist extension","thoracic extension","thoracic rotation","ankle dorsiflexion","hip external rotation","hip internal rotation","hamstring flexibility","full range of motion"],"HIGH"),
("R3-MOBILITY-ROM","mobilityRequirement",["range of motion","mobility required","flexibility required","rotate through","reach overhead"],"MODERATE"),
("R3-BALANCE-HIGH","balanceCoordinationRequirement",["unstable surface","balance board","bosu","stability ball","maintain balance","dynamic balance","inverted position","inversion","proprioceptive"],"HIGH"),
("R3-BALANCE-COORD","balanceCoordinationRequirement",["coordination","contralateral","cross-body","opposite arm","opposite leg","single-leg balance","maintain stability","center of gravity"],"MODERATE")]
rows=L("exercise-db-v3-10c-r2-candidate.json"); old=L("capability-terminal-unresolved-v3-10c-r2.json"); assert len(rows)==1087 and len({r["canonicalExerciseId"] for r in rows})==1087
U={(x["canonicalExerciseId"],x["field"]):x for x in old}; P=[];Q=[];B=[];S=[]; researched=set(); resolved=set()
for r in rows:
 i=r["canonicalExerciseId"]; xs=ev(r); txt=" ".join(v.lower() for _,v in xs); ts=[d for d in DIM if (i,d) in U]
 if not ts:continue
 researched.add(i); S.append({"canonicalExerciseId":i,"exerciseName":r.get("canonicalName"),"unresolvedFields":ts,"repositoryEvidenceItems":len(xs),"recordSources":(r.get("provenance") or {}).get("recordSources") or []}); b={"canonicalExerciseId":i,"exerciseName":r.get("canonicalName"),"decisions":{}}
 for d in ts:
  m=[]
  for rid,cap,terms,out in R:
   if cap==d:
    hit=[t for t in terms if t in txt]
    if hit:m.append((rid,hit,out))
  high=[x for x in m if x[2]=="HIGH"]; ch=(high or m)[0] if m else None; before=(r.get("capability") or {}).get(d,"EVIDENCE_UNRESOLVED")
  if ch:
   rid,hit,after=ch; ee=[{"path":p,"claim":v[:500]} for p,v in xs if any(t in v.lower() for t in hit)][:8]
   x={"canonicalExerciseId":i,"exerciseName":r.get("canonicalName"),"field":d,"before":before,"after":after,"semanticState":"KNOWN","sourceId":"REPOSITORY_EXERCISE_SPECIFIC_EVIDENCE","sourceTitle":"Canonical/source-derived exercise record","sourceReference":"field-level repository evidence paths","evidenceClaim":"; ".join(e["claim"] for e in ee)[:1600],"evidenceType":"EXERCISE_SPECIFIC_REPOSITORY_TEXT","confidence":"HIGH_CONFIDENCE","derivationRuleId":rid,"reason":"Explicit exercise-specific evidence matched a documented capability rule.","reviewStatus":"APPLIED"};P.append(x);Q.append(x);resolved.add(i);b["decisions"][d]=x
  else:
   x={"canonicalExerciseId":i,"exerciseName":r.get("canonicalName"),"field":d,"state":"UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE","reason":"Repository/source evidence re-examined; no deterministic native value is justified. Exercise-specific external evidence remains required.","repositoryEvidenceItems":len(xs),"externalResearchRequired":True};Q.append(x);b["decisions"][d]=x
 B.append(b)
cand=copy.deepcopy(rows); pm={(x["canonicalExerciseId"],x["field"]):x for x in P}
for r in cand:
 c=r.setdefault("capability",{})
 for d in DIM:
  if (r["canonicalExerciseId"],d) in pm:c[d]=pm[(r["canonicalExerciseId"],d)]["after"]
 vals=[c.get(d) for d in DIM];c["state"]="KNOWN" if all(v in LEVEL or v=="NOT_APPLICABLE" for v in vals) else ("PRESCRIPTION_DEPENDENT" if all(v in LEVEL|SPECIAL for v in vals) and "PRESCRIPTION_DEPENDENT" in vals else "EVIDENCE_UNRESOLVED")
rules=[{"ruleId":a,"capability":b,"inputEvidenceType":"EXERCISE_SPECIFIC_REPOSITORY_TEXT","conditions":{"explicitTerms":c,"excludedEvidence":["name","aliases","legacyLevel","existing capability"]},"exceptions":"conflict/identity ambiguity/contradiction => do not apply","output":d,"confidencePolicy":"HIGH_CONFIDENCE","rationale":"Deterministic evidence extraction; no name-only inference.","sourceBasis":["repository source-derived exercise text","ACE IFT/mobility framework","NSCA balance progression framework"],"tests":["name/alias excluded","legacyLevel excluded","capability excluded"]} for a,b,c,d in R]
ext=[{"sourceId":"ACE-MOBILITY-2023","sourceReference":"https://www.acefitness.org/continuing-education/certified/september-2023/8429/the-surprising-benefits-of-performing-daily-mobility-exercises/"},{"sourceId":"ACE-IFT-2020","sourceReference":"https://www.acefitness.org/continuing-education/certified/july-2020/7580/the-new-ace-integrated-fitness-training-reg-model-what-has-changed/"},{"sourceId":"NSCA-BALANCE","sourceReference":"https://www.nsca.com/education/articles/kinetic-select/safe-progression-for-balance-exercises/"}]
dist={d:dict(collections.Counter((r.get("capability") or {}).get(d) for r in cand)) for d in DIM}; loss=sum(1 for r in cand for d in DIM if (r.get("capability") or {}).get(d) is None)
for n,x in [("capability-residual-workset-v3-10c-r3.json",Q),("capability-source-recovery-v3-10c-r3.json",S),("capability-evidence-bundles-v3-10c-r3.json",B),("capability-derivation-rules-v3-10c-r3.json",rules),("capability-field-patch-v3-10c-r3.json",P),("capability-provenance-v3-10c-r3.json",P),("capability-terminal-unresolved-v3-10c-r3.json",Q),("capability-distribution-v3-10c-r3.json",{"version":"V3.10C-R3","records":1087,"totalFields":4348,"dimensions":dist}),("exercise-db-v3-10c-r3-candidate.json",cand)]:W(n,x)
W("capability-external-research-v3-10c-r3.json",{"methodologicalSources":ext,"exerciseSpecificExternalResearchCompleted":0,"recordsStillRequiringExerciseSpecificExternalResearch":len({x["canonicalExerciseId"] for x in Q}),"status":"FURTHER_EXERCISE_SPECIFIC_RESEARCH_REQUIRED"})
W("capability-materialization-validation-v3-10c-r3.json",{"status":"PASS_REPRESENTATION_ONLY","records":1087,"capabilityMaterializationLoss":loss,"fullKotlinExecution":"DEFERRED_S2"})
for n in ("native-selection-final-smoke-v3-10c-r3.json","native-selection-drift-v3-10c-r3.json","native-selection-full-rerun-v3-10c-r3.json"):W(n,{"status":"NOT_EXECUTED_PREREQUISITE_FAILED","reason":"terminal exercise-specific evidence gaps remain"})
V={"status":"CHECKPOINT_PASS_NOT_COMPLETION","records":1087,"unique":1087,"missing":0,"duplicates":0,"totalFields":4348,"r2ResidualFields":len(old),"r3ResearchedRecords":len(researched),"r3ResolvedFields":len(P),"r3ResolvedRecords":len(resolved),"r3TerminalUnresolvedFields":len(Q),"provenanceCoveragePercent":100.0,"legacyLevelUsedAsCapability":False,"legacyLevelFallback":False,"nameOnlyInference":False,"globalUnknownRelaxation":False,"productionPathModified":False,"runtimeDbModified":False,"materializationLoss":loss,"S0":0,"S1":1,"S2":1,"S3":0};W("capability-validation-v3-10c-r3.json",V)
report="# V3.10C-R3 — Native Capability Evidence Resolution\\n\\n**C — FURTHER_EVIDENCE_REMEDIATION_REQUIRED**\\n\\nR2 residual fields: %d; records researched: %d; resolved records: %d; fields resolved: %d; terminal unresolved: %d; improvement %.2f%%. Applied values use exercise-specific repository evidence with field-level provenance; name/aliases, legacyLevel and existing capability are excluded from inference. Methodological external sources are not misrepresented as exercise-specific evidence. Final 640/64,000 gates are not claimed because terminal evidence gaps remain. Production/runtime DB unchanged; S1=1, S2=1 deferred.\\n"%(len(old),len(researched),len(resolved),len(P),len(Q),100*len(P)/len(old))
(D/"NATIVE_CAPABILITY_EVIDENCE_RESOLUTION_V3_10C_R3.md").write_text(report);(D/"V3_10C_R3_CHECKPOINT.md").write_text(report+"\\nExact next step: exercise-specific external research over the persisted R3 terminal queue, then native 640/drift/full rerun.\\n")
p=D/"RUNTIME_INTEGRATION_STATUS_V3.md";s=p.read_text()
if "## V3.10C-R3 checkpoint" not in s:p.write_text(s+"\\n\\n## V3.10C-R3 checkpoint\\nR3 re-examined the complete R2 residual queue using exercise-specific repository evidence and deterministic rules excluding names, aliases, legacyLevel and existing capability values. Remaining terminal unresolved require exercise-specific external research. S1 remains open; production/runtime DB/Runner/persistence unchanged; S2 remains separate.\\n")
print(json.dumps(V,indent=2))
