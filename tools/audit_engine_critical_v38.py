#!/usr/bin/env python3
import json, pathlib, copy, collections, subprocess, sys, os
D=pathlib.Path("exercise-db-canonical")
CAND=D/"exercise-db-v3-candidate.json"
UNR=D/"engine-v3-migration-unresolved-v3-7.json"
def load(p):
    return json.loads(p.read_text())
def dump(name,obj):
    (D/name).write_text(json.dumps(obj,indent=2,ensure_ascii=False)+"\n")
def cid(x):
    return x.get("canonicalExerciseId") or x.get("canonicalId") or x.get("id")
def cname(x):
    return x.get("canonicalName") or x.get("canonicalNameEn") or x.get("name") or ""
def classify(u):
    d=str(u.get("domain","")).lower()
    c=str(u.get("classification") or u.get("finalClassification") or u.get("terminalClassification") or "").upper()
    reason=str(u.get("reason","")).lower()
    if d in {"description","setup","execution","technique","errors","media","editorial"}: return "P5_EDITORIAL_OR_UX_DEFERRED"
    if "IDENTITY" in c or d=="identity":
        return "P1_ENGINE_FUNCTIONAL_BLOCKER" if "AMBIG" in c else "P2_MATERIAL_COVERAGE_BLOCKER"
    if "SCHEMA" in c or "RUNTIME" in c or "schema" in reason or "runtime" in reason:
        return "P3_SCHEMA_OR_RUNTIME_DEPENDENCY"
    if d in {"movement","movementpattern","family"}:
        return "P4_ENGINE_ENHANCING_NON_BLOCKING"
    if d in {"equipment","capability","skill"}:
        return "P2_MATERIAL_COVERAGE_BLOCKER"
    if d in {"prescription","resistance","load","assistance"}:
        return "P2_MATERIAL_COVERAGE_BLOCKER" if "INSUFFICIENT" in c else "P3_SCHEMA_OR_RUNTIME_DEPENDENCY"
    if d in {"category","group"}:
        return "P2_MATERIAL_COVERAGE_BLOCKER"
    return "SAFE_DOCUMENTED_UNRESOLVED"
def dedup_key(u):
    return (cid(u),str(u.get("domain","unknown")).lower(),str(u.get("field") or u.get("issue") or u.get("cluster") or u.get("classification") or u.get("finalClassification") or "state"))
def main():
    candidate=load(CAND); unresolved=load(UNR)
    if isinstance(candidate,dict):
        records=candidate.get("exercises") or candidate.get("records") or candidate.get("candidate") or []
    else: records=candidate
    assert len(records)==1087, len(records)
    ids=[cid(r) for r in records]; assert None not in ids and len(set(ids))==1087
    ded={}
    for u in unresolved:
        k=dedup_key(u)
        if k not in ded: ded[k]=u
    work=[]
    for i,(k,u) in enumerate(ded.items(),1):
        p=classify(u)
        work.append({
          "blockerId":f"V38-{i:04d}","canonicalExerciseId":cid(u),"canonicalName":cname(u),
          "domain":u.get("domain","unknown"),"fieldDimension":u.get("field") or u.get("issue") or u.get("cluster"),
          "unresolvedType":u.get("classification") or u.get("finalClassification") or u.get("terminalClassification"),
          "reason":u.get("reason"),"evidence":u.get("evidence") or u.get("evidenceChecked") or u.get("evidenceReviewed"),
          "provenance":u.get("sourceIds") or u.get("provenance"),"confidence":u.get("confidence"),
          "schemaState":u.get("classification"),"currentSafeBehavior":"FAIL_CLOSED" if u.get("failClosed") or str(u.get("engineConsequence","")).lower().find("fail closed")>=0 else "DOCUMENTED_UNRESOLVED",
          "engineConsumer":u.get("domain"),"engineConsequence":u.get("engineConsequence"),
          "failClosedConsequence":"EXCLUSION_OR_DEGRADED_PATH" if p in {"P2_MATERIAL_COVERAGE_BLOCKER","P3_SCHEMA_OR_RUNTIME_DEPENDENCY"} else "NONE_OR_ENHANCING",
          "coverageConsequence":"MATERIAL_POSSIBLE" if p=="P2_MATERIAL_COVERAGE_BLOCKER" else "NON_MATERIAL_OR_RUNTIME_DEPENDENT",
          "existingRemediationAttempts":u.get("evidenceChecked") or u.get("evidenceReviewed"),
          "externalResearchRequired":p=="P2_MATERIAL_COVERAGE_BLOCKER" and "INSUFFICIENT" in str(u).upper(),
          "manualDecisionRequired":p in {"P1_ENGINE_FUNCTIONAL_BLOCKER","P3_SCHEMA_OR_RUNTIME_DEPENDENCY"},
          "runtimeEvolutionRequired":p=="P3_SCHEMA_OR_RUNTIME_DEPENDENCY",
          "identityIssue":p=="P1_ENGINE_FUNCTIONAL_BLOCKER","terminalForV38":p not in {"P0_ENGINE_SAFETY_BLOCKER","P1_ENGINE_FUNCTIONAL_BLOCKER"},
          "priority":p
        })
    byid=collections.defaultdict(list)
    for w in work: byid[w["canonicalExerciseId"]].append(w)
    matrix=[]
    for r in records:
        x=cid(r); ws=byid.get(x,[]); cnt=collections.Counter(w["priority"] for w in ws)
        p0=cnt["P0_ENGINE_SAFETY_BLOCKER"]; p1=cnt["P1_ENGINE_FUNCTIONAL_BLOCKER"]
        ready=(p0==0 and p1==0)
        domains=collections.defaultdict(list)
        for w in ws: domains[str(w["domain"]).lower()].append(w["priority"])
        def status(keys,default="READY"):
            ps=sum((domains.get(k,[]) for k in keys),[])
            if "P1_ENGINE_FUNCTIONAL_BLOCKER" in ps or "P0_ENGINE_SAFETY_BLOCKER" in ps:return "BLOCKED"
            if "P2_MATERIAL_COVERAGE_BLOCKER" in ps:return "READY_WITH_SAFE_UNRESOLVED"
            if "P3_SCHEMA_OR_RUNTIME_DEPENDENCY" in ps:return "SCHEMA_DEPENDENCY"
            if "P4_ENGINE_ENHANCING_NON_BLOCKING" in ps:return "ENHANCING_GAP"
            return default
        matrix.append({
          "canonicalExerciseId":x,"canonicalName":cname(r) or r.get("canonicalNameEn",""),
          "identityStatus":status(["identity"]),"categoryStatus":status(["category","group"]),
          "equipmentStatus":status(["equipment"]),"locationStatus":status(["equipment","location"]),
          "resistanceStatus":status(["resistance"]),"loadInputStatus":status(["resistance","load"]),
          "assistanceInputStatus":status(["resistance","assistance"]),"capabilityStatus":status(["capability"]),
          "prescriptionStatus":status(["prescription"]),"skillStatus":status(["skill"]),
          "movementPatternStatus":status(["movement","movementpattern"]),"familyStatus":status(["family"]),
          "P0Count":p0,"P1Count":p1,"P2Count":cnt["P2_MATERIAL_COVERAGE_BLOCKER"],"P3Count":cnt["P3_SCHEMA_OR_RUNTIME_DEPENDENCY"],
          "safeUnresolvedCount":cnt["SAFE_DOCUMENTED_UNRESOLVED"]+cnt["P4_ENGINE_ENHANCING_NON_BLOCKING"]+cnt["P5_EDITORIAL_OR_UX_DEFERRED"],
          "failClosedBehavior":"SAFE_EXCLUSION_OR_DEGRADED" if cnt["P2_MATERIAL_COVERAGE_BLOCKER"] else "NO_MATERIAL_FAIL_CLOSED",
          "coverageImpact":"MATERIAL_POSSIBLE" if cnt["P2_MATERIAL_COVERAGE_BLOCKER"] else "LOW_OR_NONE",
          "engineReadyForSimulation":ready,"blockingReasons":[w["blockerId"] for w in ws if w["priority"] in {"P0_ENGINE_SAFETY_BLOCKER","P1_ENGINE_FUNCTIONAL_BLOCKER"}],
          "remediationsApplied":[],"provenanceRefs":[w["blockerId"] for w in ws]
        })
    counts=collections.Counter(w["priority"] for w in work)
    # V3.8 deliberately applies no mutation without new CERTAIN/HIGH evidence.
    layer={"schemaVersion":"3.8","base":"V3.7","mutations":[],"certainRemediations":0,"highConfidenceRemediations":0,
           "policy":"No unresolved is normalized merely to improve coverage; V3.7 validated mutations remain preserved."}
    engine_candidate=copy.deepcopy(candidate)
    residual=[{
      "blockerId":w["blockerId"],"priority":w["priority"],"domain":w["domain"],"canonicalIds":[w["canonicalExerciseId"]],
      "skillImpact":w["domain"]=="skill","locationImpact":w["domain"]=="equipment","engineConsequence":w["engineConsequence"],
      "reason":w["reason"],"requiredAction":"V3.9 simulation measurement" if w["priority"] in {"P2_MATERIAL_COVERAGE_BLOCKER","P4_ENGINE_ENHANCING_NON_BLOCKING"} else ("runtime/schema integration gate" if w["priority"]=="P3_SCHEMA_OR_RUNTIME_DEPENDENCY" else "targeted evidence/manual resolution"),
      "requiresExternalResearch":w["externalResearchRequired"],"requiresSchemaChange":w["priority"]=="P3_SCHEMA_OR_RUNTIME_DEPENDENCY",
      "requiresRuntimeChange":w["runtimeEvolutionRequired"],"requiresHumanDecision":w["manualDecisionRequired"],
      "safeToDeferToV39":w["priority"] not in {"P0_ENGINE_SAFETY_BLOCKER","P1_ENGINE_FUNCTIONAL_BLOCKER"},
      "safeToDeferPostIntegration":w["priority"] in {"P4_ENGINE_ENHANCING_NON_BLOCKING","P5_EDITORIAL_OR_UX_DEFERRED","SAFE_DOCUMENTED_UNRESOLVED"},
      "completionCriterion":"Resolve with CERTAIN/HIGH evidence or demonstrate safe measured impact."
    } for w in work]
    # Skill path accounting is structural in V3.8; V3.9 performs behavioral simulation.
    skill_unr=[w for w in work if str(w["domain"]).lower()=="skill"]
    skill={"expectedFamilies":15,"expectedNodes":60,"typedRelations":117,
           "familyRows":[{"skillFamily":f"FAMILY_{i+1:02d}","skillNode":None,"candidateExercises":None,"eligibleAfterFailClosed":None,"typedRelations":None,"progressions":None,"regressions":None,"strengthBuilders":None,"equipmentBlockers":None,"capabilityBlockers":None,"prescriptionBlockers":None,"identityBlockers":None,"schemaRuntimeBlockers":None,"trainabilityStatus":"STRUCTURALLY_REPRESENTABLE_DEGRADED_BY_UNRESOLVED","blockingReasons":[]} for i in range(15)],
           "nodeRows":[{"skillFamily":None,"skillNode":f"NODE_{i+1:02d}","candidateExercises":None,"eligibleAfterFailClosed":None,"typedRelations":None,"progressions":None,"regressions":None,"strengthBuilders":None,"equipmentBlockers":None,"capabilityBlockers":None,"prescriptionBlockers":None,"identityBlockers":None,"schemaRuntimeBlockers":None,"trainabilityStatus":"STRUCTURALLY_REPRESENTABLE_DEGRADED_BY_UNRESOLVED","blockingReasons":[]} for i in range(60)],
           "unresolvedSkillDimensions":len(skill_unr),"note":"Names/path behavior are intentionally not fabricated; V3.9 must simulate real path eligibility."}
    failclosed={"classification":"SAFE_BUT_MATERIAL_COVERAGE_LOSS" if counts["P2_MATERIAL_COVERAGE_BLOCKER"] else "SAFE_LOW_IMPACT",
      "totalFailClosedExclusionsBefore":sum(1 for w in work if w["currentSafeBehavior"]=="FAIL_CLOSED"),
      "totalFailClosedExclusionsAfter":sum(1 for w in work if w["currentSafeBehavior"]=="FAIL_CLOSED"),
      "unsafeBlockingCount":counts["P0_ENGINE_SAFETY_BLOCKER"],"materialCoverageLossCount":counts["P2_MATERIAL_COVERAGE_BLOCKER"],
      "byDomain":dict(collections.Counter(str(w["domain"]) for w in work if w["priority"]=="P2_MATERIAL_COVERAGE_BLOCKER"))}
    ready=sum(1 for m in matrix if m["engineReadyForSimulation"])
    summary={"sourceCandidateCount":1087,"rawUnresolved":len(unresolved),"deduplicatedSemanticUnresolved":len(work),
      "before":dict(counts),"after":dict(counts),"certainRemediations":0,"highConfidenceRemediations":0,
      "ambiguousNotApplied":sum(1 for w in work if "AMBIG" in str(w["unresolvedType"]).upper()),
      "insufficientEvidenceNotApplied":sum(1 for w in work if "INSUFFICIENT" in str(w["unresolvedType"]).upper()),
      "engineReadyForSimulationCount":ready,"notEngineReadyCount":1087-ready,
      "status":"READY_FOR_SELECTION_PRESCRIPTION_SIMULATION" if counts["P0_ENGINE_SAFETY_BLOCKER"]==0 and counts["P1_ENGINE_FUNCTIONAL_BLOCKER"]==0 else "REMEDIATION_STILL_REQUIRED"}
    dump("engine-critical-workset-v3-8.json",work)
    dump("engine-critical-classification-v3-8.json",{"counts":dict(counts),"items":work})
    dump("engine-remediation-layer-v3-8.json",layer)
    dump("exercise-db-v3-engine-ready-candidate.json",engine_candidate)
    dump("engine-ready-matrix-v3-8.json",matrix)
    dump("engine-skill-path-matrix-v3-8.json",skill)
    dump("engine-fail-closed-impact-v3-8.json",failclosed)
    dump("engine-residual-blocker-queue-v3-8.json",residual)
    for dom,name in [("equipment","engine-equipment-location-remediation-v3-8.json"),("resistance","engine-resistance-load-remediation-v3-8.json"),("capability","engine-capability-remediation-v3-8.json"),("skill","engine-skill-remediation-v3-8.json"),("prescription","engine-prescription-remediation-v3-8.json"),("identity","engine-identity-remediation-v3-8.json")]:
        dump(name,{"domain":dom,"items":[w for w in work if str(w["domain"]).lower()==dom],"mutationsApplied":[]})
    dump("engine-remediation-provenance-v3-8.json",{"baseCandidate":"exercise-db-v3-candidate.json","worksetSource":"engine-v3-migration-unresolved-v3-7.json","mutations":[],"coverage":1087})
    dump("engine-critical-remediation-summary-v3-8.json",summary)
    md=f"""# Exercise DB V3.8 — Engine-Critical Remediation\n\nV3.8 classifies the V3.7 unresolved set by actual engine consequence. It does not normalize unknown values or modify the runtime DB.\n\n- Source candidate: 1087/1087\n- Raw unresolved: {len(unresolved)}\n- Deduplicated semantic unresolved: {len(work)}\n- P0: {counts['P0_ENGINE_SAFETY_BLOCKER']}\n- P1: {counts['P1_ENGINE_FUNCTIONAL_BLOCKER']}\n- P2: {counts['P2_MATERIAL_COVERAGE_BLOCKER']}\n- P3: {counts['P3_SCHEMA_OR_RUNTIME_DEPENDENCY']}\n- P4: {counts['P4_ENGINE_ENHANCING_NON_BLOCKING']}\n- P5: {counts['P5_EDITORIAL_OR_UX_DEFERRED']}\n- Safe documented: {counts['SAFE_DOCUMENTED_UNRESOLVED']}\n- Engine-ready for simulation: {ready}/1087\n- New V3.8 data mutations: 0 (no new CERTAIN/HIGH evidence was introduced)\n- Runtime DB modified: NO\n\nDecision: **{summary['status']}**\n\nV3.9 is not started by this phase.\n"""
    (D/"ENGINE_CRITICAL_REMEDIATION_V3_8.md").write_text(md)
    print(json.dumps(summary,indent=2))
if __name__=="__main__": main()
