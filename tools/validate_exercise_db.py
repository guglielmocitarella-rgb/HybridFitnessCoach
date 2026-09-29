#!/usr/bin/env python3
import argparse, hashlib, json, pathlib, sys
from collections import Counter, defaultdict

ROOT=pathlib.Path(__file__).resolve().parents[1]
DB=ROOT/"exercise-db-v2"
ALLOWED_TYPES={"STRENGTH","HYPERTROPHY","CALISTHENICS","SKILL","POWER","PLYOMETRIC","CONDITIONING","AEROBIC","CORE","MOBILITY","FLEXIBILITY","SMR"}
ALLOWED_MEASURES={"REPS","SECONDS","DURATION","DISTANCE","INTERVALS"}
ALLOWED_CONF={"HIGH","MEDIUM","LOW"}
ALLOWED_REL={"MAIN_PROGRESSION","REGRESSION","PREREQUISITE","PREPARATORY_DRILL","ASSISTANCE","ADVANCED_VARIANT","RELATED"}

def load(p): return json.loads(p.read_text(encoding="utf-8"))
def norm(s): return " ".join(str(s or "").casefold().replace("-"," ").split())
def main():
    ap=argparse.ArgumentParser(); ap.add_argument("--report",default=str(DB/"VALIDATION_REPORT.json")); a=ap.parse_args()
    errors=[]; warnings=[]
    manifest=load(DB/"BUILD_MANIFEST.json")
    ex_path=DB/"exercises.json"; pr_path=DB/"progressions.json"; src_path=DB/"sources.json"; pg_path=DB/"programming_guidance.json"
    for p in (ex_path,pr_path,src_path,pg_path):
        if not p.exists(): errors.append(f"missing required artifact: {p.relative_to(ROOT)}")
    if errors:
        report={"ok":False,"errors":errors,"warnings":warnings}; pathlib.Path(a.report).write_text(json.dumps(report,indent=2)+"\n"); print(json.dumps(report,indent=2)); return 1
    exercises=load(ex_path); edges=load(pr_path); sources=load(src_path); guidelines=load(pg_path)
    if not isinstance(exercises,list): errors.append("exercises.json must be a JSON array"); exercises=[]
    if not isinstance(edges,list): errors.append("progressions.json must be a JSON array"); edges=[]
    if not isinstance(sources,list): errors.append("sources.json must be a JSON array"); sources=[]
    ids=[x.get("id") for x in exercises]; sid={x.get("source_id") for x in sources}
    for v,c in Counter(ids).items():
        if not v: errors.append("record without id")
        elif c>1: errors.append(f"duplicate id: {v}")
    names=[norm(x.get("canonicalNameEn")) for x in exercises]
    for v,c in Counter(names).items():
        if not v: errors.append("record without canonicalNameEn")
        elif c>1: errors.append(f"duplicate canonical name: {v}")
    canon={n:i for n,i in zip(names,ids)}; alias_owner={}
    for x in exercises:
        if x.get("movementType") not in ALLOWED_TYPES: errors.append(f"{x.get('id')}: invalid movementType")
        if x.get("measure") not in ALLOWED_MEASURES: errors.append(f"{x.get('id')}: invalid measure")
        if x.get("confidence") not in ALLOWED_CONF: errors.append(f"{x.get('id')}: invalid confidence")
        if not x.get("nameIt"): warnings.append(f"{x.get('id')}: missing nameIt")
        refs=x.get("sources") or []
        if not refs: errors.append(f"{x.get('id')}: movement without source")
        for s in refs:
            if s.get("source_id") not in sid: errors.append(f"{x.get('id')}: unknown source_id {s.get('source_id')}")
        for al in x.get("aliases") or []:
            n=norm(al)
            if n in canon and canon[n]!=x.get("id"): warnings.append(f"alias/canonical collision: {al}")
            if n in alias_owner and alias_owner[n]!=x.get("id"): warnings.append(f"alias collision: {al}")
            alias_owner[n]=x.get("id")
        variant=x.get("variantOf")
        if variant is not None and variant not in set(ids): errors.append(f"{x.get('id')}: unknown variantOf {variant}")
        for prereq in x.get("prerequisites") or []:
            ref=prereq if isinstance(prereq,str) else prereq.get("id") or prereq.get("exercise_id")
            if not ref or ref not in set(ids): errors.append(f"{x.get('id')}: unknown prerequisite {ref}")
        for field in ("descriptionIt","primaryMuscles","equipment","prescription","restGuidance"):
            if not x.get(field): warnings.append(f"{x.get('id')}: missing {field}")
    used_sources=set()
    for x in exercises:
        for ref in x.get("sources") or []: used_sources.add(ref.get("source_id"))
    for g in (guidelines.get("rules") or []):
        gs=g.get("source_id")
        if gs not in sid: errors.append(f"programming guideline {g.get('id')}: unknown source_id {gs}")
        else: used_sources.add(gs)
    unused=sorted(sid-used_sources)
    if unused: warnings.append(f"registered sources without materialized evidence: {unused}")
    idset=set(ids); edgekeys=set(); graph=defaultdict(list)
    for e in edges:
        f,t,r=e.get("from"),e.get("to"),e.get("relationship")
        if f not in idset or t not in idset: errors.append(f"dangling edge: {f}->{t}")
        if f==t: errors.append(f"self edge: {f}")
        if r not in ALLOWED_REL: errors.append(f"invalid edge relationship: {r}")
        k=(f,t,r)
        if k in edgekeys: errors.append(f"duplicate edge: {k}")
        edgekeys.add(k)
        refs=e.get("sources") or []
        if not refs: errors.append(f"edge without provenance: {f}->{t}")
        for source in refs:
            source_id=source.get("source_id") if isinstance(source,dict) else source
            if source_id not in sid: errors.append(f"edge {f}->{t}: unknown source_id {source_id}")
            else: used_sources.add(source_id)
        if r in {"MAIN_PROGRESSION","PREREQUISITE"}: graph[f].append(t)
    seen=set(); active=set()
    def dfs(n):
        if n in active: return True
        if n in seen: return False
        seen.add(n); active.add(n)
        if any(dfs(m) for m in graph[n]): return True
        active.remove(n); return False
    for n in list(graph):
        active.clear()
        if dfs(n): errors.append(f"cycle in MAIN_PROGRESSION/PREREQUISITE graph involving {n}"); break
    if len(exercises)!=manifest.get("canonical_movements"): errors.append(f"manifest canonical_movements={manifest.get('canonical_movements')} actual={len(exercises)}")
    if len(edges)!=manifest.get("progression_edges"): errors.append(f"manifest progression_edges={manifest.get('progression_edges')} actual={len(edges)}")
    if len(sources)!=manifest.get("sources_registered"): errors.append(f"manifest sources_registered={manifest.get('sources_registered')} actual={len(sources)}")
    conf=Counter(x.get("confidence") for x in exercises); types=Counter(x.get("movementType") for x in exercises)
    artifact_hash=hashlib.sha256(ex_path.read_bytes()).hexdigest()
    report={"ok":not errors,"counts":{"canonical_movements":len(exercises),"progression_edges":len(edges),"sources":len(sources)},"confidence":dict(sorted(conf.items())),"movement_types":dict(sorted(types.items())),"artifact_sha256":artifact_hash,"source_coverage":sorted(used_sources),"errors":errors,"warnings":warnings}
    pathlib.Path(a.report).write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False,indent=2)); return 0 if not errors else 1
if __name__=="__main__": raise SystemExit(main())
