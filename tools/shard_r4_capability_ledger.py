#!/usr/bin/env python3
import json,hashlib,pathlib
D=pathlib.Path("exercise-db-canonical"); src=D/"capability-terminal-unresolved-v3-10c-r4.json"; out=D/"r4-ledger-shards"; out.mkdir(exist_ok=True)
rows=json.loads(src.read_text()); orig=list(rows)
rows=sorted(rows,key=lambda x:(x["canonicalExerciseId"],x["field"]))
groups=[]; cur=[]; size=0
for x in rows:
 s=len(json.dumps(x,ensure_ascii=False,separators=(",",":")).encode())+2
 if cur and size+s>60000: groups.append(cur);cur=[];size=0
 cur.append(x);size+=s
if cur:groups.append(cur)
manifest={"version":"V3.10C-R4-INFRA-SHARDS-1","sourcePath":str(src),"sourceSha256":hashlib.sha256(src.read_bytes()).hexdigest(),"ordering":["canonicalExerciseId","field"],"totalCells":len(rows),"totalUniqueRecords":len({x["canonicalExerciseId"] for x in rows}),"shards":[]}
recon=[]
for i,g in enumerate(groups,1):
 p=out/f"shard-{i:03d}.json"; b=(json.dumps(g,ensure_ascii=False,indent=2)+"\n").encode();p.write_bytes(b);recon+=g
 manifest["shards"].append({"index":i,"path":str(p),"sha256":hashlib.sha256(b).hexdigest(),"bytes":len(b),"cells":len(g),"uniqueRecords":len({x["canonicalExerciseId"] for x in g}),"firstCanonicalExerciseId":g[0]["canonicalExerciseId"],"lastCanonicalExerciseId":g[-1]["canonicalExerciseId"]})
key=lambda x:json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(",",":"))
assert len(rows)==3835 and len(recon)==3835
assert len({(x["canonicalExerciseId"],x["field"]) for x in recon})==3835
assert sorted(map(key,orig))==sorted(map(key,recon))
manifest["validation"]={"cellsReconstructed":3835,"missingCells":0,"duplicateCellKeys":0,"semanticMutation":False,"sourcePreserved":True,"maxShardBytes":max(x["bytes"] for x in manifest["shards"])}
(out/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n")
print(json.dumps(manifest["validation"]))

# R5 exact unprocessed workset (infrastructure only)
ck=json.loads((D/"native-capability-r5-checkpoint.json").read_text()); processed=set(ck["processedRecordIds"])
unprocessed=[x for x in rows if x["canonicalExerciseId"] not in processed]; assert len(unprocessed)==3779
for p in out.glob("r5-unprocessed-shard-*.json"): p.unlink()
ug=[];cur=[];size=0
for x in unprocessed:
 s=len(json.dumps(x,ensure_ascii=False,separators=(",",":")).encode())+2
 if cur and size+s>60000: ug.append(cur);cur=[];size=0
 cur.append(x);size+=s
if cur:ug.append(cur)
um={"baseCells":len(rows),"processedCells":len(rows)-len(unprocessed),"remainingUnprocessed":len(unprocessed),"processedRecordIds":sorted(processed),"shards":[]}
for i,g in enumerate(ug,1):
 p=out/f"r5-unprocessed-shard-{i:03d}.json";b=(json.dumps(g,ensure_ascii=False,indent=2)+"\n").encode();p.write_bytes(b)
 um["shards"].append({"index":i,"path":str(p),"sha256":hashlib.sha256(b).hexdigest(),"bytes":len(b),"cells":len(g),"firstCanonicalExerciseId":g[0]["canonicalExerciseId"],"lastCanonicalExerciseId":g[-1]["canonicalExerciseId"]})
(out/"r5-unprocessed-summary.json").write_text(json.dumps(um,ensure_ascii=False,indent=2)+"\n")
