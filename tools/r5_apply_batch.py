#!/usr/bin/env python3
"""Apply already-decided R5 capability batches atomically; never classify semantics."""
import argparse, copy, hashlib, json, os, shutil, subprocess, sys, tempfile
from pathlib import Path

FAMILIES=("field-decisions","evidence","provenance","patch","research-log")
CELL_KEYS={"field-decisions":"decision","evidence":"evidence","provenance":"provenance","patch":"patch"}
SHARDS=Path("exercise-db-canonical/r5-authoritative-shards")
CHECKPOINT=Path("exercise-db-canonical/native-capability-r5-checkpoint.json")
TXN=Path(".r5-apply-batch-txn")
LOCK=Path(".r5-apply-batch.lock")
CORE=("processedResidualCells","resolvedCells","terminalUnresolvedCells","remainingCells","lastProcessedId")

class BatchError(RuntimeError): pass
def need(ok,msg):
    if not ok: raise BatchError(msg)
def jbytes(v): return (json.dumps(v,ensure_ascii=False,indent=2)+"\n").encode()
def canon(v): return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode()
def sha256(b): return hashlib.sha256(b).hexdigest()
def gitblob(b): return hashlib.sha1(b"blob "+str(len(b)).encode()+b"\0"+b).hexdigest()
def sk(e): return "|".join((e["canonicalExerciseId"],e.get("field") or e.get("dimension") or "",e.get("provenanceId") or ""))
def sortkey(e): return (e["canonicalExerciseId"],e.get("field") or e.get("dimension") or "",e.get("provenanceId") or "")
def fnv1a32_utf16(s):
    h=2166136261; raw=s.encode("utf-16-le")
    for i in range(0,len(raw),2): h=((h^int.from_bytes(raw[i:i+2],"little"))*16777619)&0xffffffff
    return h
def route(e): return fnv1a32_utf16(sk(e))%16
def keys(o,ks,label): need(isinstance(o,dict) and all(k in o for k in ks),f"{label}: missing required field")
def no_legacy(v,p="$"):
    if isinstance(v,dict):
        for k,x in v.items():
            need(k!="legacyLevel",f"{p}.{k}: forbidden legacyLevel"); no_legacy(x,f"{p}.{k}")
    elif isinstance(v,list):
        for i,x in enumerate(v): no_legacy(x,f"{p}[{i}]")

def validate_batch(b):
    keys(b,("schemaVersion","baselineHead","expectedPreviousCheckpoint","records"),"batch"); no_legacy(b)
    need(b["schemaVersion"]==1,"schemaVersion must be 1")
    need(isinstance(b["baselineHead"],str) and len(b["baselineHead"])==40 and all(c in "0123456789abcdefABCDEF" for c in b["baselineHead"]),"invalid baselineHead")
    cp=b["expectedPreviousCheckpoint"]
    keys(cp,("initialResidualCells","processedRecordIds","processedResidualCells","resolvedCells","terminalUnresolvedCells","remainingCells","sourcesVisited","provenanceEntries","lastProcessedId","validationState"),"expectedPreviousCheckpoint")
    need(isinstance(cp["processedRecordIds"],list),"processedRecordIds must be an array")
    need(isinstance(b["records"],list) and b["records"],"records must be non-empty")
    req=("canonicalExerciseId","dimension","previousState","newState","value","evidenceType","sourceTier","sourceTitle","sourcePublisher","sourceLocator","evidenceSummary","reasoningRule","confidence","provenanceId","researchVersion")
    preq=("canonicalExerciseId","field","before","after","provenanceId","confidence")
    seen_ids=set(); seen={f:set() for f in FAMILIES}; resolved_n=terminal_n=0; routes={f:[] for f in FAMILIES}
    for ri,r in enumerate(b["records"]):
        keys(r,("researchLog","cells"),f"records[{ri}]"); log=r["researchLog"]; cells=r["cells"]
        keys(log,("canonicalExerciseId","exerciseName","identityStatus","sources","decision","fieldApplication"),f"records[{ri}].researchLog")
        cid=log["canonicalExerciseId"]; need(isinstance(cid,str) and cid and cid not in seen_ids and cid not in cp["processedRecordIds"],f"invalid/duplicate/already-processed record {cid}")
        seen_ids.add(cid); need(isinstance(log["sources"],list) and log["sources"],f"{cid}: sources required")
        for s in log["sources"]:
            keys(s,("title","publisher","tier","locator","finding"),f"{cid}.source")
            need(all(s[k] not in ("",None) for k in ("title","publisher","locator","finding")),f"{cid}: incomplete source")
        app=log["fieldApplication"]; keys(app,("resolved","terminalUnresolved"),f"{cid}.fieldApplication")
        res,term=app["resolved"],app["terminalUnresolved"]; need(isinstance(res,dict) and isinstance(term,list) and len(term)==len(set(term)) and not(set(res)&set(term)),f"{cid}: invalid fieldApplication")
        lk=sk(log); need(lk not in seen["research-log"],f"duplicate research-log key {lk}"); seen["research-log"].add(lk); routes["research-log"].append(f"{route(log):02x}")
        fields=set()
        need(isinstance(cells,list),f"{cid}: cells must be array")
        for ci,c in enumerate(cells):
            keys(c,("decision","evidence","provenance","patch"),f"{cid}.cells[{ci}]")
            d,e,p,x=c["decision"],c["evidence"],c["provenance"],c["patch"]
            for n,v in (("decision",d),("evidence",e),("provenance",p)): keys(v,req,f"{cid}.{n}")
            keys(x,preq,f"{cid}.patch")
            dim=d["dimension"]; need(dim and dim not in fields and res.get(dim)==d["value"],f"{cid}: inconsistent resolved cell {dim}"); fields.add(dim)
            need(d["canonicalExerciseId"]==cid and d["newState"]=="KNOWN","decision must be KNOWN and match record")
            for v in (e,p):
                need((v["canonicalExerciseId"],v["dimension"],v["provenanceId"],v["value"],v["newState"])==(cid,dim,d["provenanceId"],d["value"],d["newState"]),"decision/evidence/provenance mismatch")
            need((x["canonicalExerciseId"],x["field"],x["provenanceId"],x["after"])==(cid,dim,d["provenanceId"],d["value"]),"patch mismatch")
            rk=sk(d)
            for fam,v in (("field-decisions",d),("evidence",e),("provenance",p),("patch",x)):
                k=sk(v); need(k==rk and k not in seen[fam],f"duplicate/conflicting batch semantic key {fam}:{k}"); seen[fam].add(k); routes[fam].append(f"{route(v):02x}")
            resolved_n+=1
        need(set(res)==fields,f"{cid}: resolved map must exactly match cells"); terminal_n+=len(term)
    processed=resolved_n+terminal_n; need(processed>0,"empty semantic batch")
    return {"records":len(b["records"]),"resolvedCells":resolved_n,"terminalCells":terminal_n,"processedCells":processed,"routes":routes}

def load_state(root):
    cp=json.loads((root/CHECKPOINT).read_text())
    rows={}; manifests={}; raw={str(CHECKPOINT):(root/CHECKPOINT).read_bytes()}
    for fam in FAMILIES:
        d=root/SHARDS/fam; mp=d/"manifest.json"; m=json.loads(mp.read_text()); raw[str(mp.relative_to(root))]=mp.read_bytes()
        need(m.get("shardCount")==16 and len(m.get("shards",[]))==16,f"{fam}: invalid manifest")
        rows[fam]={}; total=0
        for i in range(16):
            p=d/f"bucket-{i:02x}.json"; rb=p.read_bytes(); need(rb,f"empty shard {p}"); a=json.loads(rb); need(isinstance(a,list),f"non-array shard {p}")
            desc=m["shards"][i]; need(desc.get("index")==i and desc.get("filename")==p.name and desc.get("entryCount")==len(a) and desc.get("gitBlobSha")==gitblob(rb),f"{fam}: manifest mismatch {i:02x}")
            rows[fam][i]=a; raw[str(p.relative_to(root))]=rb; total+=len(a)
        need(m.get("totalEntryCount")==total,f"{fam}: manifest total mismatch"); manifests[fam]=m
    return cp,rows,manifests,raw

def inspect(rows):
    idx={}; bad=set()
    for fam in FAMILIES:
        idx[fam]={}
        for bucket,arr in rows[fam].items():
            for e in arr:
                k=sk(e); need(k not in idx[fam],f"{fam}: duplicate existing key {k}"); idx[fam][k]=(bucket,e)
                want=route(e)
                if want!=bucket: bad.add((fam,k,bucket,want))
    base=set(idx["field-decisions"])
    for fam in ("evidence","provenance","patch"): need(set(idx[fam])==base,f"cross-family key mismatch: {fam}")
    return idx,bad,{f:len(idx[f]) for f in FAMILIES}

def derive(cp,b,s):
    n=copy.deepcopy(cp); ids=[r["researchLog"]["canonicalExerciseId"] for r in b["records"]]
    n["processedRecordIds"]=list(cp["processedRecordIds"])+ids
    n["processedResidualCells"]+=s["processedCells"]; n["resolvedCells"]+=s["resolvedCells"]; n["terminalUnresolvedCells"]+=s["terminalCells"]; n["remainingCells"]-=s["processedCells"]
    n["sourcesVisited"]+=len(ids); n["provenanceEntries"]+=s["resolvedCells"]; n["lastProcessedId"]=ids[-1]
    need(n["remainingCells"]>=0 and n["processedResidualCells"]+n["remainingCells"]==n["initialResidualCells"] and n["resolvedCells"]+n["terminalUnresolvedCells"]==n["processedResidualCells"],"checkpoint arithmetic failure")
    return n

def git_head(root):
    r=subprocess.run(["git","-C",str(root),"rev-parse","HEAD"],capture_output=True,text=True); need(r.returncode==0,"cannot read git HEAD"); return r.stdout.strip()
def clean(root):
    r=subprocess.run(["git","-C",str(root),"status","--porcelain","--",str(SHARDS),str(CHECKPOINT)],capture_output=True,text=True); need(r.returncode==0 and not r.stdout.strip(),"authoritative paths are dirty")

def build_plan(root,b):
    s=validate_batch(b); cp,rows,mans,raw=load_state(root); idx,bad,counts=inspect(rows); exp=b["expectedPreviousCheckpoint"]; after=derive(exp,b,s)
    payload={f:[] for f in FAMILIES}
    for r in b["records"]:
        payload["research-log"].append(r["researchLog"])
        for c in r["cells"]:
            for fam,k in CELL_KEYS.items(): payload[fam].append(c[k])
    exact=absent=0; conflicts=[]; misrouted=[]
    for fam in FAMILIES:
        for e in payload[fam]:
            found=idx[fam].get(sk(e))
            if not found: absent+=1
            elif found[1]!=e: conflicts.append(f"{fam}:{sk(e)}")
            else:
                exact+=1
                if found[0]!=route(e): misrouted.append(f"{fam}:{sk(e)}")
    total=sum(map(len,payload.values()))
    if not conflicts and exact==total and cp==after and not misrouted:
        return {"status":"PASS","mode":"IDEMPOTENT_REPLAY","batchId":sha256(canon(b)),"summary":s,"preexistingRoutingViolations":len(bad)}
    need(not conflicts,"conflicting existing keys: "+",".join(conflicts[:8])); need(exact==0 and absent==total,"partial replay/partial write detected")
    need(cp==exp,"stale checkpoint"); need(git_head(root)==b["baselineHead"],"stale baselineHead"); clean(root)
    nr=copy.deepcopy(rows); touched={f:set() for f in FAMILIES}
    for fam in FAMILIES:
        for e in payload[fam]:
            bucket=route(e); nr[fam][bucket].append(copy.deepcopy(e)); touched[fam].add(bucket)
        for bucket in touched[fam]: nr[fam][bucket].sort(key=sortkey)
    nidx,nbad,ncounts=inspect(nr); need(nbad==bad,"new routing violation or historical routing mutation")
    for fam in ("field-decisions","evidence","provenance","patch"): need(ncounts[fam]==counts[fam]+s["resolvedCells"],f"{fam}: count delta")
    need(ncounts["research-log"]==counts["research-log"]+s["records"],"research-log count delta")
    for fam in FAMILIES:
        for k,(_,e) in idx[fam].items(): need(nidx[fam][k][1]==e,f"pre-existing payload mutated {fam}:{k}")
    out={}; nm=copy.deepcopy(mans)
    for fam in FAMILIES:
        if not touched[fam]: continue
        for bucket in sorted(touched[fam]):
            rel=SHARDS/fam/f"bucket-{bucket:02x}.json"; rb=jbytes(nr[fam][bucket]); out[str(rel)]=rb
            d=nm[fam]["shards"][bucket]; d["entryCount"]=len(nr[fam][bucket]); d["byteSize"]=len(rb); d["gitBlobSha"]=gitblob(rb)
        totaln=sum(len(nr[fam][i]) for i in range(16)); m=nm[fam]; m["totalEntryCount"]=totaln
        if isinstance(m.get("semanticComparison"),dict):
            m["semanticComparison"].update({"reconstructedEntryCount":totaln,"missing":0,"duplicate":0,"semanticMutation":0,"result":"PASS"})
        out[str(SHARDS/fam/"manifest.json")]=jbytes(m)
    out[str(CHECKPOINT)]=jbytes(after)
    need(all(p in raw for p in out),"write escaped authoritative allowlist")
    return {"status":"PASS","mode":"APPLY_READY","batchId":sha256(canon(b)),"summary":s,"checkpointAfter":{k:after[k] for k in CORE},"preexistingRoutingViolations":len(bad),"touchedBuckets":{f:[f"{i:02x}" for i in sorted(v)] for f,v in touched.items() if v},"outputs":out,"old":{p:raw[p] for p in out}}

def areplace(path,data):
    path.parent.mkdir(parents=True,exist_ok=True); fd,tmp=tempfile.mkstemp(dir=path.parent,prefix=path.name+".",suffix=".tmp")
    try:
        with os.fdopen(fd,"wb") as h: h.write(data); h.flush(); os.fsync(h.fileno())
        os.replace(tmp,path)
    finally:
        try: os.unlink(tmp)
        except FileNotFoundError: pass
def lock(root):
    try: return os.open(root/LOCK,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
    except FileExistsError as e: raise BatchError("writer lock exists") from e
def unlock(root,fd):
    os.close(fd)
    try: (root/LOCK).unlink()
    except FileNotFoundError: pass

def recover(root,bid):
    tr=root/TXN
    if not tr.exists(): return False
    ds=[p for p in tr.iterdir() if p.is_dir()]
    if not ds: return False
    need(len(ds)==1,"multiple interrupted transactions"); d=ds[0]; j=json.loads((d/"journal.json").read_text()); need(j.get("batchId")==bid,"interrupted transaction is for a different batch")
    for i,x in enumerate(j["entries"]):
        cur=sha256((root/x["path"]).read_bytes()); old=(d/f"{i:03d}.old").read_bytes()
        need(cur in (x["oldSha256"],x["newSha256"]) and sha256(old)==x["oldSha256"],f"cannot safely recover {x['path']}")
    for i,x in enumerate(j["entries"]): areplace(root/x["path"],(d/f"{i:03d}.old").read_bytes())
    shutil.rmtree(d)
    try: tr.rmdir()
    except OSError: pass
    return True

def apply(root,p):
    if p["mode"]=="IDEMPOTENT_REPLAY": return {"status":"PASS","mode":"IDEMPOTENT_REPLAY","batchId":p["batchId"],"writes":0}
    fd=lock(root)
    try:
        d=root/TXN/p["batchId"]; d.mkdir(parents=True,exist_ok=False); cp=str(CHECKPOINT); order=sorted(x for x in p["outputs"] if x!=cp)+[cp]; entries=[]
        for i,rel in enumerate(order):
            old,new=p["old"][rel],p["outputs"][rel]; need((root/rel).read_bytes()==old,f"target changed after validation {rel}")
            (d/f"{i:03d}.old").write_bytes(old); (d/f"{i:03d}.new").write_bytes(new); entries.append({"path":rel,"oldSha256":sha256(old),"newSha256":sha256(new),"checkpointLast":rel==cp})
        areplace(d/"journal.json",jbytes({"version":1,"batchId":p["batchId"],"state":"PREPARED","entries":entries}))
        for i,rel in enumerate(order): areplace(root/rel,(d/f"{i:03d}.new").read_bytes())
        for x in entries: need(sha256((root/x["path"]).read_bytes())==x["newSha256"],f"post-write verify failed {x['path']}")
        shutil.rmtree(d)
        try: (root/TXN).rmdir()
        except OSError: pass
        return {"status":"PASS","mode":"APPLIED","batchId":p["batchId"],"writes":len(order),"checkpointLast":True}
    except Exception:
        try:
            if (root/TXN/p["batchId"]/"journal.json").exists(): recover(root,p["batchId"])
        except Exception: pass
        raise
    finally: unlock(root,fd)

def public(p): return {k:v for k,v in p.items() if k not in ("outputs","old")}
def main():
    a=argparse.ArgumentParser(description=__doc__); a.add_argument("--batch",required=True,type=Path); a.add_argument("--root",type=Path,default=Path(".")); a.add_argument("--validate-input",action="store_true"); a.add_argument("--apply",action="store_true"); x=a.parse_args()
    try:
        need(not(x.validate_input and x.apply),"--validate-input and --apply are mutually exclusive"); b=json.loads(x.batch.read_text()); s=validate_batch(b); bid=sha256(canon(b))
        if x.validate_input: r={"status":"PASS","mode":"INPUT_ONLY","batchId":bid,"summary":s,"writes":0}
        else:
            root=x.root.resolve()
            if x.apply and (root/TXN).exists():
                fd=lock(root)
                try: rec=recover(root,bid)
                finally: unlock(root,fd)
                if rec: raise BatchError("interrupted same-batch transaction rolled back; rerun to revalidate")
            p=build_plan(root,b); r=public(p)|{"writes":0}
            if x.apply: r.update(apply(root,p))
        print(json.dumps(r,ensure_ascii=False,indent=2)); return 0
    except (BatchError,OSError,json.JSONDecodeError,KeyError,TypeError,ValueError) as e:
        print(json.dumps({"status":"FAIL","error":str(e),"writes":0},ensure_ascii=False,indent=2)); return 1
if __name__=="__main__": sys.exit(main())
