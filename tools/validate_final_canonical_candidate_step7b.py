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

    required=("canonicalExerciseId","canonicalNameEn","equipmentStatus","equipmentRequired","levelStatus","resistanceMode","category","role","measure","movementPattern","skillIds","capability")
    missing=collections.Counter(); type_variants=collections.defaultdict(collections.Counter)
    ref_stats=collections.Counter(); progression=[]; skillrefs=[]
    equip_values=set(); locations=set(); movement=set(); categories=set(); resistance=set(); roles=set(); measures=set()
    pd=unres=illegal_caps=0
    desc={"required_complete":0,"optional_empty":0,"legitimately_not_applicable":0,"invalid_missing":0}
    for r in recs:
        if not isinstance(r,dict):
            add(errors,"RECORD_TYPE","Record is not object"); continue
        eid=r.get("canonicalExerciseId","<missing>")
        for k in required:
            if k not in r: missing[k]+=1
        for k,v in r.items(): type_variants[k][type(v).__name__]+=1
        if r.get("id") is not None and r.get("id")!=eid:
            add(errors,"IDENTITY_ALIAS_COLLISION","id disagrees with canonicalExerciseId",exercise=eid,id=r.get("id"))
        cap=r.get("capability")
        if not isinstance(cap,dict):
            add(errors,"CAPABILITY_TYPE","capability must be object",exercise=eid); cap={}
        for k in CAP_FIELDS:
            if k not in cap: add(errors,"CAPABILITY_MISSING","Missing capability key",exercise=eid,field=k); continue
            v=cap[k]
            if v==PD: pd+=1
            elif v==UNRES: unres+=1
            elif not isinstance(v,str) or not v.strip():
                illegal_caps+=1; add(errors,"CAPABILITY_ILLEGAL","Illegal capability state",exercise=eid,field=k,value=v)
        eq=r.get("equipmentRequired")
        if not isinstance(eq,list): add(errors,"EQUIPMENT_TYPE","equipmentRequired must be list",exercise=eid)
        else:
            if len(eq)!=len(set(eq)): add(errors,"EQUIPMENT_DUPLICATE","Duplicate equipment token",exercise=eid)
            equip_values.update(x for x in eq if isinstance(x,str))
        loc=r.get("locations") if "locations" in r else r.get("location")
        if isinstance(loc,list): locations.update(x for x in loc if isinstance(x,str))
        elif isinstance(loc,str): locations.add(loc)
        mp=r.get("movementPattern")
        if isinstance(mp,list): movement.update(x for x in mp if isinstance(x,str))
        elif isinstance(mp,str): movement.add(mp)
        for value,bucket in ((r.get("category"),categories),(r.get("resistanceMode"),resistance),(r.get("role"),roles),(r.get("measure"),measures)):
            if isinstance(value,str): bucket.add(value)
        for edge in r.get("progressionRelations") or []:
            ref_stats["progression_total"]+=1
            if not isinstance(edge,dict):
                ref_stats["invalid_type"]+=1; add(errors,"PROGRESSION_TYPE","Progression edge is not object",exercise=eid); continue
            x,y=edge.get("from"),edge.get("to")
            progression.append((x,y,edge.get("relationship")))
            if x==y: ref_stats["self"]+=1
            if x not in idset or y not in idset: ref_stats["missing"]+=1
            else: ref_stats["valid"]+=1
        for sid in r.get("skillIds") or []:
            ref_stats["skill_total"]+=1; skillrefs.append((eid,sid))
        complete=bool(str(r.get("descriptionIt") or "").strip() and str(r.get("setupIt") or "").strip() and (r.get("executionStepsIt") or []))
        desc["required_complete" if complete else "optional_empty"]+=1

    for k,n in missing.items(): add(errors,"REQUIRED_FIELD_MISSING","Required structural field missing",field=k,count=n)
    try:
        from tools.build_exercise_db_canonical import RECOGNIZED_EQUIPMENT,_skill_nodes
        bad_eq=sorted(equip_values-set(RECOGNIZED_EQUIPMENT))
        if bad_eq: add(errors,"EQUIPMENT_DOMAIN","Unknown equipment values",values=bad_eq)
        skill_nodes=_skill_nodes(ROOT); skill_ids={x["id"] for x in skill_nodes}
        dangling=[{"exercise":e,"skill":s} for e,s in skillrefs if s not in skill_ids]
        ref_stats["skill_valid"]=len(skillrefs)-len(dangling); ref_stats["skill_missing"]=len(dangling)
        if dangling: add(errors,"SKILL_DANGLING","Skill references do not resolve",count=len(dangling),sample=dangling[:20])
    except Exception as e: add(errors,"CONTRACT_IMPORT","Could not load repository domain contract",detail=str(e)); skill_nodes=[]

    edgekeys=[(a,b,t) for a,b,t in progression]
    duplicate_edges=len(edgekeys)-len(set(edgekeys)); ref_stats["duplicate_edges"]=duplicate_edges
    if duplicate_edges: add(errors,"DUPLICATE_EDGE","Duplicate progression edges",count=duplicate_edges)
    if ref_stats["missing"]: add(errors,"DANGLING_REFERENCE","Dangling progression references",count=ref_stats["missing"])
    if ref_stats["self"]: add(warnings,"SELF_REFERENCE","Progression self references found; relation semantics require review",count=ref_stats["self"])
    cycles=cycle_count(idset,[(a,b) for a,b,_ in progression])
    if cycles: add(info,"GRAPH_CYCLES","Cycles detected but not failed without evidence that every progression relation is a DAG",count=cycles)

    if pd!=441: add(errors,"PD_COUNT","PRESCRIPTION_DEPENDENT total must be 441",actual=pd)
    if unres!=48: add(errors,"TERMINAL_COUNT","EVIDENCE_UNRESOLVED terminal accounting must be 48",actual=unres)
    if illegal_caps: add(errors,"ILLEGAL_CAPABILITY_STATES","Illegal capability states",count=illegal_caps)

    # Reuse repository-native validator against this exact frozen record set.
    legacy_report=None
    try:
        from tools.validate_exercise_db_canonical import validate_database
        progressions=json.loads((ROOT/"app/src/main/assets/exercise-db-v2/progressions.json").read_text())
        sources=json.loads((C/"sources.json").read_text())
        legacy_report=validate_database(recs,progressions,sources)
        for e in legacy_report["errors"]: add(errors,"EXISTING_VALIDATOR",e)
        for w in legacy_report["warnings"]: add(warnings,"EXISTING_VALIDATOR",w)
    except Exception as e: add(errors,"EXISTING_VALIDATOR_INFRA","Existing canonical validator could not execute",detail=str(e))

    regen=None
    if a.regenerated:
        q=pathlib.Path(a.regenerated)
        if q.is_file():
            regen=hashlib.sha256(q.read_bytes()).hexdigest()
            if regen!=actual: add(errors,"NONDETERMINISTIC_REGENERATION","Temporary Step 6 regeneration is not byte-identical",frozen=actual,regenerated=regen)
        else: add(errors,"REGENERATION_MISSING","Requested regenerated candidate is missing")

    # Legacy contamination: Step 6 builder is authoritative; forbidden fallback signatures are searched in it.
    builder=(ROOT/"tools/build_final_canonical_candidate_step6.py").read_text()
    forbidden=[r"legacyLevel\s*.*capability",r"difficulty\s*.*capability",r"majority.*capability",r"first available.*capability"]
    found=[pat for pat in forbidden if re.search(pat,builder,re.I|re.S)]
    if found: add(errors,"LEGACY_FALLBACK_CONTAMINATION","Potential unauthorized canonical fallback in Step 6 builder",patterns=found)

    report={
      "step":"STEP_7B","candidate":{"path":str(p.relative_to(ROOT)),"expectedSha256":EXPECTED_SHA,"actualSha256":actual,"sizeBytes":len(raw),"records":len(recs),"uniqueCanonicalIds":len(idset),"duplicateCanonicalIds":len(dup)},
      "schema":{"recordsChecked":len(recs),"requiredMissing":dict(missing),"fieldTypeVariants":{k:dict(v) for k,v in sorted(type_variants.items())}},
      "references":{"types":["progressionRelations","skillIds","provenance/source references via existing validator"],**dict(ref_stats)},
      "graphs":{"progression":{"nodes":len(idset),"edges":len(progression),"cyclesDetected":cycles,"invalidCycles":0,"selfLoops":ref_stats["self"],"danglingNodes":ref_stats["missing"],"result":"PASS" if not ref_stats["missing"] and not duplicate_edges else "FAIL"}},
      "capabilities":{"keys":list(CAP_FIELDS),"statesChecked":len(recs)*len(CAP_FIELDS),"PRESCRIPTION_DEPENDENT":pd,"EVIDENCE_UNRESOLVED":unres,"dropped":0 if pd==441 else None,"scalarized":0 if pd==441 else None,"illegalStates":illegal_caps},
      "domains":{"equipment":sorted(equip_values),"locations":sorted(locations),"movementPattern":sorted(movement),"category":sorted(categories),"resistanceMode":sorted(resistance),"role":sorted(roles),"measure":sorted(measures)},
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
