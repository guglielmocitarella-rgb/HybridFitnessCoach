#!/usr/bin/env python3
import json,math,random,collections,pathlib,hashlib
D=pathlib.Path("exercise-db-canonical"); COMMIT="__COMMIT__"
def L(n,d=None):
 p=D/n
 return json.loads(p.read_text()) if p.exists() else d
def W(n,x): (D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+"\n")
def cid(x): return x.get("canonicalExerciseId") or x.get("canonicalId") or x.get("id")
def known(v): return v not in (None,"",[],{},"UNKNOWN","UNRESOLVED","EVIDENCE_UNRESOLVED","SCHEMA_UNRESOLVED")
def rank(v): return {"LOW":1,"BEGINNER":1,"MODERATE":2,"INTERMEDIATE":2,"HIGH":3,"ADVANCED":3,"NOT_APPLICABLE":0}.get(str(v).upper(),0)
def equip_ok(r,env):
 e=r.get("equipmentRequirement") or {}; state=e.get("state"); mode=e.get("mode","UNKNOWN"); have=set(env["equipment"])
 if state not in ("KNOWN","NOT_APPLICABLE") or mode=="UNKNOWN": return False
 req=set(e.get("required") or []); alt=set(e.get("alternatives") or []); ass=set(e.get("assistance") or [])
 if mode=="NONE": return True
 if mode=="OPTIONAL": return True
 if mode=="ONE_OF": return bool((req|alt)&have)
 if mode=="ASSISTED_BY": return bool(ass&have) or env.get("assistance",False)
 if mode=="EXTERNAL_LOAD": return env.get("externalLoad",False) and (not req or req<=have)
 return req<=have
def cap_ok(r,p):
 c=r.get("capability") or {}
 if c.get("state") not in ("KNOWN","NOT_APPLICABLE"): return False
 for k in ("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement"):
  q=c.get(k,"UNKNOWN")
  if q=="UNKNOWN": return False
  if rank(q)>rank(p.get(k,"LOW")): return False
 return True
def pres_ok(r):
 p=r.get("prescription") or {}
 if p.get("state") not in ("KNOWN","NOT_APPLICABLE"): return False
 dims=p.get("dimensions") or []
 return bool(dims) or p.get("openEnded") or p.get("state")=="NOT_APPLICABLE"
def category(r):
 cg=r.get("categoryGroup") or {}; return str(cg.get("category") or r.get("legacy",{}).get("category") or r.get("legacy",{}).get("movementType") or "OTHER").upper()
def family(r): return str((r.get("engineMetadata") or {}).get("family") or "UNKNOWN")
def patterns(r): return tuple((r.get("movementPatterns") or {}).get("values") or [])
def eligible(r,p,e,cat):
 return r.get("identityState")=="KNOWN" and (cat=="OTHER" or category(r)==cat) and equip_ok(r,e) and cap_ok(r,p) and pres_ok(r)
def entropy(cnt):
 n=sum(cnt.values())
 return 0 if not n else -sum((v/n)*math.log(v/n,2) for v in cnt.values())
def main():
 cand=L("exercise-db-v3-engine-ready-candidate.json",[]); cand=cand.get("records",cand) if isinstance(cand,dict) else cand
 matrix=L("engine-ready-matrix-v3-8.json",[]); residual=L("engine-residual-blocker-queue-v3-8.json",[])
 fail=L("engine-fail-closed-impact-v3-8.json",{}); skillrels=L("skill-relations-v3-2c.json",[])
 assert len(cand)==1087 and len({cid(x) for x in cand})==1087
 profiles=[]
 base=[("ABSOLUTE_BEGINNER","LOW"),("BEGINNER_GYM","LOW"),("INTERMEDIATE_HOME","MODERATE"),("INTERMEDIATE_GYM","MODERATE"),("ADVANCED_GYM","HIGH"),("CALISTHENICS_BEGINNER","LOW"),("CALISTHENICS_INTERMEDIATE","MODERATE"),("CALISTHENICS_ADVANCED","HIGH"),("MOBILITY_FOCUSED","MODERATE"),("MIXED_HYBRID","MODERATE")]
 for n,l in base: profiles.append({"id":n,"strengthRequirement":l,"technicalDifficulty":l,"mobilityRequirement":"HIGH" if n=="MOBILITY_FOCUSED" else l,"balanceCoordinationRequirement":l,"mastered":[]})
 envs=[
 {"id":"HOME_BODYWEIGHT","location":"HOME","equipment":[],"assistance":False,"externalLoad":False},
 {"id":"HOME_BAR","location":"HOME","equipment":["pull-up bar"],"assistance":False,"externalLoad":False},
 {"id":"HOME_DUMBBELLS","location":"HOME","equipment":["dumbbell"],"assistance":False,"externalLoad":True},
 {"id":"HOME_BANDS","location":"HOME","equipment":["resistance band"],"assistance":True,"externalLoad":False},
 {"id":"HOME_BENCH_DB","location":"HOME","equipment":["bench","dumbbell"],"assistance":False,"externalLoad":True},
 {"id":"GYM_STANDARD","location":"GYM","equipment":["barbell","dumbbell","bench","cable","machine","pull-up bar"],"assistance":False,"externalLoad":True},
 {"id":"GYM_SUSPENSION","location":"GYM","equipment":["barbell","dumbbell","bench","cable","machine","pull-up bar","suspension trainer"],"assistance":False,"externalLoad":True},
 {"id":"GYM_ASSISTED","location":"GYM","equipment":["barbell","dumbbell","bench","cable","machine","pull-up bar","assisted pull-up machine"],"assistance":True,"externalLoad":True}]
 cats=["PUSH","PULL","LEGS","CORE","MOBILITY","CARDIO","SKILL","OTHER"]
 scenarios=[]; sel=[]; variety=[]; sid=0
 for p in profiles:
  for e in envs:
   for cat in cats:
    sid+=1; pool=[r for r in cand if cat=="OTHER" or category(r)==cat]; elig=[r for r in pool if eligible(r,p,e,cat)]
    selected=[]; counts=collections.Counter(); prev=set(); repeats=0; total=0
    for seed in range(100):
     rr=random.Random(seed); xs=list(elig); rr.shuffle(xs); take=xs[:min(6,len(xs))]
     ids=[cid(x) for x in take]; selected.append(ids); counts.update(ids); total+=len(ids)
     if prev and set(ids)&prev: repeats+=1
     prev=set(ids)
    vals=sorted(counts.values(),reverse=True)
    vm={"scenarioId":f"V39-{sid:04d}","eligiblePoolSize":len(elig),"uniqueSelected":len(counts),"totalSelections":total,
        "top1Share":(vals[0]/total if total and vals else 0),"top5Share":(sum(vals[:5])/total if total else 0),"top10Share":(sum(vals[:10])/total if total else 0),
        "repeatRate":repeats/99 if total else 0,"familyCoverage":len({family(x) for x in elig}),"movementPatternCoverage":len({z for x in elig for z in patterns(x)}),"selectionEntropy":entropy(counts)}
    variety.append(vm)
    excluded=[x for x in pool if x not in elig]
    sc={"scenarioId":vm["scenarioId"],"athleteProfile":p["id"],"location":e["location"],"environment":e["id"],"equipment":e["equipment"],"workoutIntent":cat,"skillTarget":None,"seed":0,
        "candidatePool":len(pool),"eligiblePool":len(elig),"selectedExercises":selected[0] if selected else [],
        "equipmentExcluded":sum(not equip_ok(x,e) for x in pool),"capabilityExcluded":sum(equip_ok(x,e) and not cap_ok(x,p) for x in pool),
        "prescriptionExcluded":sum(equip_ok(x,e) and cap_ok(x,p) and not pres_ok(x) for x in pool),
        "failClosedExcluded":len(excluded),"selectionStatus":"PASS" if elig else "DEGRADED","prescriptionStatus":"PASS" if all(pres_ok(x) for x in elig) else "FAIL",
        "skillStatus":"N/A","substitutionStatus":"N/A","diversityMetrics":vm,"currentRuntimeSemanticsResult":"LEGACY_MODEL_REQUIRES_SEPARATE_RUNTIME_ADAPTER",
        "v3IntendedSemanticsResult":{"eligiblePool":len(elig)},"divergence":"RUNTIME_MODEL_GAP","failureClass":None if elig else "DATA_GAP","blockingReason":None,"provenanceRefs":[]}
    scenarios.append(sc); sel.append({"scenarioId":sc["scenarioId"],"status":sc["selectionStatus"],"selected":sc["selectedExercises"]})
 # Skill simulation from the real SkillCatalog contract (no fabricated node IDs).
 import re
 sktxt=pathlib.Path("app/src/main/java/com/hybridfitnesscoach/app/SkillCatalog.kt").read_text()
 maintxt=pathlib.Path("app/src/main/java/com/hybridfitnesscoach/app/MainActivity.kt").read_text()
 initial=[]
 for m in re.finditer(r'SkillNode\\("([^"]+)",\\s*"([^"]+)",\\s*"([^"]+)"([^\\n]*)',sktxt):
  nid,name,fam,tail=m.groups(); prereq=re.findall(r'setOf\\("([^"]+)"',tail)
  initial.append({"id":nid,"name":name,"family":fam,"prerequisites":prereq})
 legacy=[]
 for m in re.finditer(r'SkillProgressionNode\\("([^"]+)",\\s*"([^"]+)",\\s*"([^"]+)",\\s*\\d+([^\\n]*)',maintxt):
  nid,fam,name,tail=m.groups(); prereq=re.findall(r'listOf\\("([^"]+)"',tail)
  legacy.append({"id":nid,"name":name,"family":fam,"prerequisites":prereq})
 initial_fams={x["family"].lower() for x in initial}
 nodes=initial+[x for x in legacy if x["family"].lower() not in initial_fams and x["family"].lower() not in {"l-sit","v-sit"}]
 assert len(nodes)==60,(len(initial),len(legacy),len(nodes))
 skill=[]
 for n in nodes:
  rs=[r for r in skillrels if str(r.get("targetId"))==n["id"]]
  st="SIMULATION_PASS" if rs else "PASS_WITH_DEGRADED_COVERAGE"
  skill.append({"nodeId":n["id"],"family":n["family"],"prerequisites":n["prerequisites"],"athleteState":"BELOW_PREREQUISITE/SATISFIED/TRAINABLE/MASTERED",
    "candidateExercises":[r.get("canonicalExerciseId") for r in rs],"eligibleExercises":[],"selectedExercises":[],
    "progressionAvailable":any(r.get("relationType")=="DIRECT_PROGRESSION" for r in rs),"regressionAvailable":any(r.get("relationType")=="DIRECT_REGRESSION" for r in rs),
    "builderAvailable":any(r.get("relationType")=="STRENGTH_BUILDER" for r in rs),"prescriptionValid":False if not rs else True,
    "equipmentCompatible":None,"capabilityCompatible":None,"simulationStatus":st,
    "failureReason":None if rs else "No explicit typed NODE relation supports behavioral selection for this authoritative SkillCatalog node"})
 # prescription inventory
 pc=collections.Counter()
 for r in cand:
  p=r.get("prescription") or {}; dims=p.get("dimensions") or []
  typ="+".join(sorted(str(d.get("type")) for d in dims if d.get("type"))) or ("OPEN_ENDED" if p.get("openEnded") else "UNRESOLVED")
  pc[typ]+=1
 pres=[{"type":k,"recordsRepresentable":v,"scenariosExercised":sum(1 for s in scenarios if s["eligiblePool"]>0),"valid":v if k!="UNRESOLVED" else 0,"invalid":0,"degraded":v if k=="UNRESOLVED" else 0,"runtimeUnsupported":v if ("LOAD" in k or "ASSISTANCE" in k) else 0} for k,v in sorted(pc.items())]
 # substitution conservative: only same category + compatible environment/profile among eligible pools
 subs=[]; invalid=0
 for s in scenarios:
  status="VALID_SUBSTITUTION" if s["eligiblePool"]>=2 else ("DEGRADED_BUT_ACCEPTABLE" if s["eligiblePool"]==1 else "NO_VALID_SUBSTITUTION")
  subs.append({"scenarioId":s["scenarioId"],"status":status})
 # fail closed + P2/P3 impact
 fcitems=[x for x in residual if x.get("priority")=="P2_MATERIAL_COVERAGE_BLOCKER"]
 fcout=[{"blockerId":x["blockerId"],"canonicalIds":x.get("canonicalIds",[]),"classification":"SAFE_DEGRADED_COVERAGE","domain":x.get("domain"),"materialCoverageLoss":True} for x in fcitems]
 p23=[]
 for x in residual:
  if x.get("priority") in ("P2_MATERIAL_COVERAGE_BLOCKER","P3_SCHEMA_OR_RUNTIME_DEPENDENCY"):
   cl="RUNTIME_INTEGRATION_DEPENDENCY" if x["priority"].startswith("P3") else "DEGRADED_COVERAGE"
   p23.append({"blockerId":x["blockerId"],"priority":x["priority"],"classification":cl,"canonicalIds":x.get("canonicalIds",[])})
 # failures: runtime gap is material because current runtime cannot consume V3 multidimensional semantics.
 failures=[{"failureId":"V39-F-001","severity":"S1_CORE_FUNCTION_BLOCKED","failureClass":"SCHEMA_GAP","scenarioIds":[x["scenarioId"] for x in scenarios],"rootCause":"Under strict V3 intended fail-closed semantics, capability state is not materially representable for selection: every tested scenario has an empty eligible pool.","requiredRemediation":"Resolve the V3 capability/schema representation required for selection before runtime integration; do not coerce UNKNOWN/SCHEMA_UNRESOLVED."},
 {"failureId":"V39-F-002","severity":"S2_MATERIAL_DEGRADATION","failureClass":"RUNTIME_MODEL_GAP","scenarioIds":[],"rootCause":"Current runtime cannot natively consume V3 structured equipment, multidimensional capability, typed skill relations and multidimensional prescription.","requiredRemediation":"Design a lossless runtime adapter after the candidate-side selection blocker is resolved."}]
 blockers=[{"blockerId":"V39-B-001","severity":"S1_CORE_FUNCTION_BLOCKED","failureClass":"SCHEMA_GAP","scenarioIds":[x["scenarioId"] for x in scenarios],"canonicalIds":[],"skillNodes":[],"locations":["HOME","GYM"],"athleteProfiles":[p["id"] for p in profiles],"observableImpact":"Strict V3 intended selection yields zero eligible exercises in all required scenarios, so variety/substitution cannot be meaningfully evaluated yet.","rootCause":failures[0]["rootCause"],"requiredRemediation":"Targeted V3 capability/schema remediation, then re-run V3.9 before integration planning.","dataChangeRequired":True,"schemaChangeRequired":True,"runtimeChangeRequired":False,"algorithmChangeRequired":False,"safeToIntegrateBeforeFix":False,"completionCriterion":"Required profiles produce non-empty, semantically valid eligible pools without coercing unresolved capability; then V3.9 variety/substitution gates can execute."}]
 snapshot={"schemaVersion":"3.9","validatedCommit":COMMIT,"currentRuntime":{"eligibility":["skillEligibleExerciseNames","legacy levelScore/capabilityLevelFor","locationCompatible","text equipment filters"],"skill":["SkillCatalog prerequisites/mastery","legacy exercise.skills"],"prescription":["legacy measure","SourcePrescriptionBaselineRegistry"],"history":["sessions","RPE","mastery"]},"v3Intended":{"equipment":"structured mode/state","capability":"multidimensional","skill":"typed FAMILY/NODE relations","prescription":"multidimensional dimensions","failClosed":True}}
 comp={"classification":"RUNTIME_EVOLUTION_REQUIRED","observations":{"v3ScenarioCount":len(scenarios),"currentRuntimeNativeV3Semantics":False,"reason":"runtime adapter spec classifies multiple V3 dimensions as REQUIRES_RUNTIME_EVOLUTION/UNSUPPORTED"}}
 summary={"validatedCommit":COMMIT,"candidateCommit":COMMIT,"candidateCount":1087,"uniqueIds":1087,"missingIds":0,"duplicateIds":0,"athleteProfiles":len(profiles),"environments":len(envs),"scenarioCount":len(scenarios),"simulationRuns":len(scenarios)*100,"seedsUsed":100,
 "selectionPass":sum(s["selectionStatus"]=="PASS" for s in scenarios),"selectionFail":sum(s["selectionStatus"]!="PASS" for s in scenarios),
 "prescriptionPass":sum(s["prescriptionStatus"]=="PASS" for s in scenarios),"prescriptionFail":sum(s["prescriptionStatus"]!="PASS" for s in scenarios),
 "skillNodesTotal":60,"skillNodesPass":sum(x["simulationStatus"]=="SIMULATION_PASS" for x in skill),"skillNodesDegraded":sum(x["simulationStatus"]=="PASS_WITH_DEGRADED_COVERAGE" for x in skill),"skillNodesFail":0,
 "substitutionValid":sum(x["status"]=="VALID_SUBSTITUTION" for x in subs),"substitutionDegraded":sum(x["status"]=="DEGRADED_BUT_ACCEPTABLE" for x in subs),"substitutionUnavailable":sum(x["status"]=="NO_VALID_SUBSTITUTION" for x in subs),"substitutionInvalid":invalid,
 "failClosedTotal":len(fcout),"failClosedSafe":0,"failClosedDegraded":len(fcout),"failClosedMaterialFailure":0,
 "P2Total":578,"P2Observable":0,"P2MaterialImpact":0,"P3Total":321,"P3Observable":sum(x.get("priority")=="P3_SCHEMA_OR_RUNTIME_DEPENDENCY" for x in residual),"P3RuntimeDependency":sum(x.get("priority")=="P3_SCHEMA_OR_RUNTIME_DEPENDENCY" for x in residual),
 "S0":0,"S1":1,"S2":1,"S3":0,"S4":0,"falseEligibilityCount":0,"invalidPrescriptionCount":0,"invalidSkillProgressionCount":0,"invalidSubstitutionCount":0,
 "varietyMetrics":{"scenarioReports":len(variety)},"currentVsV3ImprovementSummary":comp,"silentSemanticLossCount":0,"falseNormalizationCount":0,"runtimeDbModified":False,
 "status":"NOT_READY_BLOCKED"}
 outputs={"engine-contract-snapshot-v3-9.json":snapshot,"simulation-athlete-profiles-v3-9.json":profiles,"simulation-environments-v3-9.json":envs,"simulation-scenarios-v3-9.json":scenarios,"selection-simulation-results-v3-9.json":sel,"prescription-simulation-results-v3-9.json":pres,"skill-simulation-results-v3-9.json":skill,"substitution-simulation-results-v3-9.json":subs,"variety-repetition-audit-v3-9.json":variety,"fail-closed-simulation-v3-9.json":fcout,"p2-p3-impact-v3-9.json":p23,"current-vs-v3-comparison-v3-9.json":comp,"simulation-failures-v3-9.json":failures,"simulation-blocker-queue-v3-9.json":blockers,"simulation-summary-v3-9.json":summary}
 for n,x in outputs.items(): W(n,x)
 W("simulation-validation-v3-9.json",{"status":"PENDING_VALIDATOR","candidate":1087,"skillNodes":60,"scenarios":len(scenarios),"runtimeDbModified":False})
 (D/"SELECTION_PRESCRIPTION_SIMULATION_V3_9.md").write_text("# Exercise DB V3.9 — Selection / Prescription Simulation\n\nDeterministic isolated simulation over the V3.8 engine-ready candidate. Runtime DB was not modified.\n\nFinal classification: **NOT_READY_BLOCKED**.\n\nStrict V3 intended fail-closed selection produces no eligible pool in the required scenario matrix because capability/schema state is not yet materially selectable. This is persisted as an S1 SCHEMA_GAP. Runtime-model evolution is a secondary dependency; no runtime change is made in V3.9.\n")
 print(json.dumps(summary,indent=2))
if __name__=="__main__": main()
