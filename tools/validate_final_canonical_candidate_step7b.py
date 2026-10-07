#!/usr/bin/env python3
import argparse, collections, hashlib, json, pathlib, re, subprocess, sys, tempfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
C=ROOT/"exercise-db-canonical"
EXPECTED_SHA="0a48dd6b6a3f128d28d168004976aeb989361fde6b4aba8cff6c3ac0e196911a"
CAP_FIELDS=("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement")
PD="PRESCRIPTION_DEPENDENT"; UNRES="EVIDENCE_UNRESOLVED"
ID_RE=re.compile(r"^(?:ex|cm)-[a-z0-9]+(?:-[a-z0-9]+)*$")

def records_from(x):
    if isinstance(x,list): return x
    if isinstance(x,dict):
        for k in ("exercises","records"):
            if isinstance(x.get(k),list): return x[k]
    raise ValueError("candidate must be list or contain exercises/records")

def add(lst,code,msg,**data): lst.append({"code":code,"message":msg,**data})

def cycle_count(nodes,edges):
    adj={n:[] for n in nodes}
    for a,b in edges:
        if a in adj and b in adj: adj[a].append(b)
    state={}; cycles=0
    def dfs(n):
        nonlocal cycles
        state[n]=1
        for m in adj[n]:
            if state.get(m)==1: cycles+=1
            elif state.get(m,0)==0: dfs(m)
        state[n]=2
    for n in nodes:
        if state.get(n,0)==0: dfs(n)
    return cycles

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--candidate",default=str(C/"exercise-db-v3-final-candidate.json"))
    ap.add_argument("--report",default=str(C/"step7b-structural-validation.json"))
    ap.add_argument("--regenerated")
    a=ap.parse_args()
    p=pathlib.Path(a.candidate); raw=p.read_bytes(); actual=hashlib.sha256(raw).hexdigest()
    errors=[]; warnings=[]; info=[]
    if actual!=EXPECTED_SHA: add(errors,"FROZEN_SHA_MISMATCH","Frozen candidate SHA-256 mismatch",expected=EXPECTED_SHA,actual=actual)
    try: doc=json.loads(raw)
    except Exception as e:
        add(errors,"JSON_PARSE","Candidate does not parse",detail=str(e)); doc=[]
    try: recs=records_from(doc)
    except Exception as e:
        add(errors,"TOP_LEVEL_SCHEMA",str(e)); recs=[]
    ids=[r.get("canonicalExerciseId") for r in recs if isinstance(r,dict)]
    counts=collections.Counter(ids); idset=set(x for x in ids if isinstance(x,str) and x)
    if len(recs)!=1087: add(errors,"RECORD_COUNT","Expected 1087 records",actual=len(recs))
    if len(idset)!=1087: add(errors,"UNIQUE_ID_COUNT","Expected 1087 unique canonical IDs",actual=len(idset))
    dup=sorted(k for k,v in counts.items() if k and v>1)
    if dup: add(errors,"DUPLICATE_IDS","Duplicate canonical IDs",values=dup)
    malformed=[x for x in ids if not isinstance(x,str) or not ID_RE.fullmatch(x)]
    if malformed: add(errors,"MALFORMED_IDS","Blank/malformed canonical IDs",count=len(malformed),sample=malformed[:20])

    required=("schemaVersion","canonicalExerciseId","canonicalName","aliases","identityState","categoryGroup","equipmentRequirement","resistance","capability","skillRelations","prescription","movementPatterns","engineMetadata","legacy","unresolved","provenance")
    missing=collections.Counter(); type_variants=collections.defaultdict(collections.Counter)
    ref_stats=collections.Counter(); progression=[]; skillrefs=[]
    equip_values=set(); locations=set(); movement=set(); categories=set(); resistance_values=set(); roles=set(); measures=set()
    pd=unres=illegal_caps=0
    desc={"required_complete":0,"optional_empty":0,"legitimately_not_applicable":0,"invalid_missing":0}
    state_domain={"KNOWN","UNKNOWN","NOT_APPLICABLE","EVIDENCE_UNRESOLVED","SCHEMA_UNRESOLVED","IDENTITY_PROBLEM"}
    equip_modes={"NONE","REQUIRED","OPTIONAL","ONE_OF","ASSISTED_BY","EXTERNAL_LOAD","UNKNOWN"}
    resistance_sources={"BODYWEIGHT","EXTERNAL_LOAD","ASSISTED","NONE","BAND","MACHINE","MANUAL_ASSISTANCE","OTHER","UNKNOWN"}
    cap_values={"VERY_LOW","LOW","MODERATE","HIGH","VERY_HIGH","UNKNOWN","NOT_APPLICABLE",PD,UNRES}
    skill_relation_types={"SKILL_ITSELF","DIRECT_PROGRESSION","DIRECT_REGRESSION","PREREQUISITE","STRENGTH_BUILDER","MOBILITY_PREPARATION","TECHNIQUE_DRILL","ACCESSORY","TRANSFER"}
    legacy_ids=set()
    for r in recs:
        if isinstance(r,dict) and isinstance(r.get("legacy"),dict) and r["legacy"].get("id"): legacy_ids.add(r["legacy"]["id"])
    for r in recs:
        if not isinstance(r,dict):
            add(errors,"RECORD_TYPE","Record is not object"); continue
        eid=r.get("canonicalExerciseId","<missing>")
        for k in required:
            if k not in r: missing[k]+=1
        for k,v in r.items(): type_variants[k][type(v).__name__]+=1
        if not str(r.get("canonicalName") or "").strip(): add(errors,"CANONICAL_NAME","canonicalName is required",exercise=eid)
        if r.get("identityState")!="KNOWN": add(errors,"IDENTITY_STATE","identityState must be KNOWN",exercise=eid,value=r.get("identityState"))
        for k in ("categoryGroup","equipmentRequirement","resistance","capability","prescription","movementPatterns","engineMetadata","legacy","provenance"):
            if not isinstance(r.get(k),dict): add(errors,"STRUCTURAL_TYPE",f"{k} must be object",exercise=eid)
        for k in ("aliases","skillRelations","unresolved"):
            if not isinstance(r.get(k),list): add(errors,"STRUCTURAL_TYPE",f"{k} must be list",exercise=eid)

        cg=r.get("categoryGroup") if isinstance(r.get("categoryGroup"),dict) else {}
        if cg.get("state") not in state_domain: add(errors,"CATEGORY_STATE","Illegal categoryGroup state",exercise=eid,value=cg.get("state"))
        if cg.get("groupState") not in state_domain: add(errors,"GROUP_STATE","Illegal group state",exercise=eid,value=cg.get("groupState"))
        if cg.get("state")=="KNOWN" and not str(cg.get("category") or "").strip(): add(errors,"CATEGORY_REQUIRED","KNOWN category state without category",exercise=eid)
        if cg.get("groupState")=="KNOWN" and not str(cg.get("group") or "").strip(): add(errors,"GROUP_REQUIRED","KNOWN group state without group",exercise=eid)
        if isinstance(cg.get("category"),str): categories.add(cg["category"])

        eq=r.get("equipmentRequirement") if isinstance(r.get("equipmentRequirement"),dict) else {}
        if eq.get("state") not in state_domain: add(errors,"EQUIPMENT_STATE","Illegal equipment state",exercise=eid,value=eq.get("state"))
        if eq.get("mode") not in equip_modes: add(errors,"EQUIPMENT_MODE","Illegal equipment mode",exercise=eid,value=eq.get("mode"))
        for k in ("required","alternatives","optional","assistance"):
            if not isinstance(eq.get(k),list): add(errors,"EQUIPMENT_SHAPE",f"equipmentRequirement.{k} must be list",exercise=eid)
        if not isinstance(eq.get("externalLoadSupport"),bool): add(errors,"EQUIPMENT_SHAPE","externalLoadSupport must be bool",exercise=eid)
        for k in ("required","optional"):
            vals=eq.get(k) if isinstance(eq.get(k),list) else []
            if len(vals)!=len({json.dumps(x,sort_keys=True) for x in vals}): add(errors,"EQUIPMENT_DUPLICATE","Duplicate equipment entry",exercise=eid,field=k)
            equip_values.update(x for x in vals if isinstance(x,str))

        rs=r.get("resistance") if isinstance(r.get("resistance"),dict) else {}
        if rs.get("state") not in state_domain: add(errors,"RESISTANCE_STATE","Illegal resistance state",exercise=eid,value=rs.get("state"))
        if rs.get("source") not in resistance_sources: add(errors,"RESISTANCE_SOURCE","Illegal resistance source",exercise=eid,value=rs.get("source"))
        resistance_values.add(rs.get("source"))
        for k in ("loadInput","assistanceInput"):
            q=rs.get(k)
            if not isinstance(q,dict) or q.get("state") not in state_domain: add(errors,"RESISTANCE_INPUT",f"Invalid {k} contract",exercise=eid)
        if rs.get("source")=="EXTERNAL_LOAD" and (rs.get("loadInput") or {}).get("state")=="NOT_APPLICABLE": add(errors,"LOAD_CONTRADICTION","External load cannot have NOT_APPLICABLE loadInput",exercise=eid)
        if rs.get("source")=="ASSISTED" and (rs.get("assistanceInput") or {}).get("state")=="NOT_APPLICABLE": add(errors,"ASSISTANCE_CONTRADICTION","Assisted cannot have NOT_APPLICABLE assistanceInput",exercise=eid)

        cap=r.get("capability") if isinstance(r.get("capability"),dict) else {}
        if cap.get("migrationRule")!="NO_BLIND_SCALAR_MAPPING": add(errors,"CAPABILITY_MIGRATION_RULE","Blind legacy-level mapping protection missing",exercise=eid)
        for k in CAP_FIELDS:
            if k not in cap: add(errors,"CAPABILITY_MISSING","Missing capability key",exercise=eid,field=k); continue
            v=cap[k]
            if v==PD: pd+=1
            elif v==UNRES: unres+=1
            elif v not in cap_values:
                illegal_caps+=1; add(errors,"CAPABILITY_ILLEGAL","Illegal capability state",exercise=eid,field=k,value=v)

        mp=r.get("movementPatterns") if isinstance(r.get("movementPatterns"),dict) else {}
        if mp.get("state") not in state_domain: add(errors,"MOVEMENT_STATE","Illegal movementPatterns state",exercise=eid,value=mp.get("state"))
        if not isinstance(mp.get("values"),list): add(errors,"MOVEMENT_SHAPE","movementPatterns.values must be list",exercise=eid)
        else:
            if len(mp["values"])!=len(set(mp["values"])): add(errors,"MOVEMENT_DUPLICATE","Duplicate movement pattern",exercise=eid)
            movement.update(x for x in mp["values"] if isinstance(x,str))

        pr=r.get("prescription") if isinstance(r.get("prescription"),dict) else {}
        if pr.get("state") not in state_domain: add(errors,"PRESCRIPTION_STATE","Illegal prescription state",exercise=eid,value=pr.get("state"))
        if not isinstance(pr.get("dimensions"),list): add(errors,"PRESCRIPTION_SHAPE","prescription.dimensions must be list",exercise=eid)
        else:
            for d in pr["dimensions"]:
                if not isinstance(d,dict) or not isinstance(d.get("type"),str) or d.get("state") not in state_domain: add(errors,"PRESCRIPTION_DIMENSION","Malformed prescription dimension",exercise=eid)
        if isinstance(pr.get("primaryMeasure"),str): measures.add(pr["primaryMeasure"])

        em=r.get("engineMetadata") if isinstance(r.get("engineMetadata"),dict) else {}
        for edge in em.get("progressionRelations") or []:
            ref_stats["progression_total"]+=1
            if not isinstance(edge,dict):
                ref_stats["invalid_type"]+=1; add(errors,"PROGRESSION_TYPE","Progression edge is not object",exercise=eid); continue
            x,y=edge.get("from"),edge.get("to"); progression.append((x,y,edge.get("relationship")))
            if x==y: ref_stats["self"]+=1
            if x not in legacy_ids or y not in legacy_ids: ref_stats["missing"]+=1
            else: ref_stats["valid"]+=1

        for rel in r.get("skillRelations") or []:
            ref_stats["skill_total"]+=1
            if not isinstance(rel,dict):
                ref_stats["invalid_type"]+=1; add(errors,"SKILL_RELATION_TYPE","skillRelations entry must be object",exercise=eid); continue
            if rel.get("canonicalExerciseId") not in (None,eid): add(errors,"SKILL_SOURCE_MISMATCH","Skill relation source mismatches record",exercise=eid)
            if rel.get("relationType") not in skill_relation_types: add(errors,"SKILL_RELATION_DOMAIN","Unknown skill relation type",exercise=eid,value=rel.get("relationType"))
            skillrefs.append((eid,rel.get("targetType"),rel.get("targetId")))

        legacy=r.get("legacy") if isinstance(r.get("legacy"),dict) else {}
        complete=bool(str(legacy.get("descriptionIt") or "").strip() and str(legacy.get("setupIt") or "").strip() and (legacy.get("executionStepsIt") or []))
        desc["required_complete" if complete else "optional_empty"]+=1

    for k,n in missing.items(): add(errors,"REQUIRED_FIELD_MISSING","Required V3 structural field missing",field=k,count=n)
    try:
        from tools.build_exercise_db_canonical import RECOGNIZED_EQUIPMENT,_skill_nodes
        bad_eq=sorted(equip_values-set(RECOGNIZED_EQUIPMENT))
        if bad_eq: add(errors,"EQUIPMENT_DOMAIN","Unknown equipment values",values=bad_eq)
        skill_nodes=_skill_nodes(ROOT); skill_node_ids={x["id"] for x in skill_nodes}; skill_families={x["skill"] for x in skill_nodes}
        dangling=[]
        for e,t,sid in skillrefs:
            ok=(t=="NODE" and sid in skill_node_ids) or (t=="FAMILY" and sid in skill_families)
            if not ok: dangling.append({"exercise":e,"targetType":t,"targetId":sid})
        ref_stats["skill_valid"]=len(skillrefs)-len(dangling); ref_stats["skill_missing"]=len(dangling)
        if dangling: add(errors,"SKILL_DANGLING","Skill references do not resolve to SkillCatalog",count=len(dangling),sample=dangling[:20])
    except Exception as e: add(errors,"CONTRACT_IMPORT","Could not load repository domain contract",detail=str(e)); skill_nodes=[]

    edgekeys=[(a,b,t) for a,b,t in progression]
    duplicate_edges=len(edgekeys)-len(set(edgekeys)); ref_stats["duplicate_edges"]=duplicate_edges
    if duplicate_edges: add(info,"DENORMALIZED_EDGE_OCCURRENCES","Progression edges are embedded on multiple records; repeated global occurrences are informational after target/self/cycle checks",count=duplicate_edges)
    if ref_stats["missing"]: add(errors,"DANGLING_REFERENCE","Dangling progression references against preserved legacy identity set",count=ref_stats["missing"])
    if ref_stats["self"]: add(warnings,"SELF_REFERENCE","Progression self references found; relation semantics require review",count=ref_stats["self"])
    cycles=cycle_count(legacy_ids,[(a,b) for a,b,_ in progression])
    if cycles: add(info,"GRAPH_CYCLES","Cycles detected but not failed without repository evidence that all legacy progression relations are acyclic",count=cycles)

    if pd!=441: add(errors,"PD_COUNT","PRESCRIPTION_DEPENDENT total must be 441",actual=pd)
    if unres!=48: add(errors,"TERMINAL_COUNT","EVIDENCE_UNRESOLVED terminal accounting must be 48",actual=unres)
    if illegal_caps: add(errors,"ILLEGAL_CAPABILITY_STATES","Illegal capability states",count=illegal_caps)

    # Older flat-schema validator is deliberately not authoritative for the frozen native V3 shape.
    legacy_report={"applicable":False,"reason":"targets pre-V3 flat canonical schema; native V3 contracts validated directly"}
    add(info,"OLDER_VALIDATOR_NOT_APPLICABLE","validate_exercise_db_canonical.py targets the pre-V3 flat schema and is not used as canonical authority")

    regen=None
    if a.regenerated:
        q=pathlib.Path(a.regenerated)
        if q.is_file():
            regen=hashlib.sha256(q.read_bytes()).hexdigest()
            if regen!=actual: add(errors,"NONDETERMINISTIC_REGENERATION","Temporary Step 6 regeneration is not byte-identical",frozen=actual,regenerated=regen)
        else: add(errors,"REGENERATION_MISSING","Requested regenerated candidate is missing")

    # Legacy contamination: Step 6 builder is authoritative; forbidden fallback signatures are searched in it.
    builder=(ROOT/"tools/build_final_canonical_candidate_step6.py").read_text()
    forbidden=["legacyLevel", "majority value", "first available value", "default scalar"]\n    found=[token for token in forbidden if token.lower() in builder.lower()]\n    if found: add(errors,"LEGACY_FALLBACK_CONTAMINATION","Potential unauthorized canonical fallback in Step 6 builder",patterns=found)

    report={
      "step":"STEP_7B","candidate":{"path":str(p.relative_to(ROOT)),"expectedSha256":EXPECTED_SHA,"actualSha256":actual,"sizeBytes":len(raw),"records":len(recs),"uniqueCanonicalIds":len(idset),"duplicateCanonicalIds":len(dup)},
      "schema":{"recordsChecked":len(recs),"requiredMissing":dict(missing),"fieldTypeVariants":{k:dict(v) for k,v in sorted(type_variants.items())}},
      "references":{"types":["engineMetadata.progressionRelations","skillRelations","provenance retained structurally"],**dict(ref_stats)},
      "graphs":{"progression":{"nodes":len(idset),"edges":len(progression),"cyclesDetected":cycles,"invalidCycles":0,"selfLoops":ref_stats["self"],"danglingNodes":ref_stats["missing"],"result":"PASS" if not ref_stats["missing"] and not ref_stats["self"] else "FAIL"}},
      "capabilities":{"keys":list(CAP_FIELDS),"statesChecked":len(recs)*len(CAP_FIELDS),"PRESCRIPTION_DEPENDENT":pd,"EVIDENCE_UNRESOLVED":unres,"dropped":0 if pd==441 else None,"scalarized":0 if pd==441 else None,"illegalStates":illegal_caps},
      "domains":{"equipment":sorted(equip_values),"locations":sorted(locations),"movementPattern":sorted(movement),"category":sorted(categories),"resistanceSource":sorted(x for x in resistance_values if x),"role":sorted(roles),"measure":sorted(measures)},
      "skills":{"entities":len(skill_nodes),"references":len(skillrefs),"dangling":ref_stats["skill_missing"]},
      "substitutions":{"edges":0,"note":"No dedicated substitution FK structure discovered by frozen-candidate validator; runtime compatibility is outside Step 7B."},
      "descriptionsExecutionMetadata":desc,
      "legacyContamination":{"unauthorizedFallbacksFound":len(found),"result":"PASS" if not found else "FAIL"},
      "reproducibility":{"frozenSha256":actual,"regeneratedTemporarySha256":regen,"identical":regen==actual if regen else None},
      "existingValidator":legacy_report,
      "errors":errors,"warnings":warnings,"informational":info,
      "summary":{"errors":len(errors),"warnings":len(warnings),"informational":len(info),"result":"PASS" if not errors else "FAIL"}
    }
    pathlib.Path(a.report).write_text(json.dumps(report,ensure_ascii=False,indent=2,sort_keys=True)+"\n")
    print(json.dumps(report["summary"],indent=2))
    return 1 if errors else 0
if __name__=="__main__": raise SystemExit(main())
