#!/usr/bin/env python3
import csv, json, pathlib, subprocess, sys
from collections import Counter
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-v2"
def load(name): return json.loads((DB/name).read_text(encoding="utf-8"))
def main():
    rows=list(csv.DictReader((DB/"sources.tsv").open(encoding="utf-8"),delimiter="\t"))
    expected_sources=sorted(({"source_id":r["source_id"],"title":r["title"],"primary_use":r["primary_use"]} for r in rows),key=lambda x:x["source_id"])
    sources=load("sources.json")
    if sources!=expected_sources: raise SystemExit("sources.json is not the deterministic materialization of sources.tsv")
    exercises=load("exercises.json"); progressions=load("progressions.json"); manifest=load("BUILD_MANIFEST.json")
    expected={
      "canonical_movements":len(exercises),
      "progression_edges":len(progressions),
      "sources_registered":len(sources),
      "movement_types":dict(sorted(Counter(x.get("movementType") for x in exercises).items())),
      "confidence":dict(sorted(Counter(x.get("confidence") for x in exercises).items())),
    }
    for k,v in expected.items():
        if manifest.get(k)!=v: raise SystemExit(f"manifest mismatch for {k}: {manifest.get(k)!r} != {v!r}")
    # Reviewed canonical JSON is an input artifact: this build verifies it and never fabricates missing evidence.
    return subprocess.call([sys.executable,str(ROOT/"tools/validate_exercise_db.py")])
if __name__=="__main__": raise SystemExit(main())
