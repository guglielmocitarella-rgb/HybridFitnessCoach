#!/usr/bin/env python3
import csv, hashlib, json, pathlib, subprocess, sys
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-v2"
def dump(path,obj): path.write_text(json.dumps(obj,ensure_ascii=False,sort_keys=True,indent=2)+"\n",encoding="utf-8")
def main():
    rows=list(csv.DictReader((DB/"sources.tsv").open(encoding="utf-8"),delimiter="\t"))
    sources=[{"source_id":r["source_id"],"title":r["title"],"primary_use":r["primary_use"]} for r in rows]
    sources.sort(key=lambda x:x["source_id"]); dump(DB/"sources.json",sources)
    # exercises.json and progressions.json are canonical reviewed inputs. Build never fabricates exercise-specific evidence.
    for name in ("exercises.json","progressions.json"):
        if not (DB/name).exists(): raise SystemExit(f"missing reviewed canonical input: exercise-db-v2/{name}")
        data=json.loads((DB/name).read_text(encoding="utf-8"))
        dump(DB/name,data)
    manifest=json.loads((DB/"BUILD_MANIFEST.json").read_text(encoding="utf-8"))
    ex=json.loads((DB/"exercises.json").read_text()); pr=json.loads((DB/"progressions.json").read_text())
    from collections import Counter
    manifest["canonical_movements"]=len(ex); manifest["progression_edges"]=len(pr); manifest["sources_registered"]=len(sources)
    manifest["movement_types"]=dict(sorted(Counter(x.get("movementType") for x in ex).items()))
    manifest["confidence"]=dict(sorted(Counter(x.get("confidence") for x in ex).items()))
    manifest["build_output"]="exercise-db-v2/exercises.json"; manifest["artifact_sha256"]=hashlib.sha256((DB/"exercises.json").read_bytes()).hexdigest()
    dump(DB/"BUILD_MANIFEST.json",manifest)
    return subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")])
if __name__=="__main__": raise SystemExit(main())
