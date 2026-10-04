#!/usr/bin/env python3
import json,pathlib,sys
D=pathlib.Path("exercise-db-canonical")
L=lambda n:json.loads((D/n).read_text())
m=L("engine-ready-matrix-v3-8.json"); s=L("engine-critical-remediation-summary-v3-8.json"); q=L("engine-residual-blocker-queue-v3-8.json"); f=L("engine-fail-closed-impact-v3-8.json")
ids=[x["canonicalExerciseId"] for x in m]
assert len(m)==1087 and len(set(ids))==1087 and None not in ids
assert s["sourceCandidateCount"]==1087
assert all(x["priority"] in {"P0_ENGINE_SAFETY_BLOCKER","P1_ENGINE_FUNCTIONAL_BLOCKER","P2_MATERIAL_COVERAGE_BLOCKER","P3_SCHEMA_OR_RUNTIME_DEPENDENCY","P4_ENGINE_ENHANCING_NON_BLOCKING","P5_EDITORIAL_OR_UX_DEFERRED","SAFE_DOCUMENTED_UNRESOLVED"} for x in q)
assert f["unsafeBlockingCount"]==s["after"].get("P0_ENGINE_SAFETY_BLOCKER",0)
assert L("engine-remediation-layer-v3-8.json")["mutations"]==[]
assert L("engine-remediation-provenance-v3-8.json")["coverage"]==1087
print("V3.8 validator PASS")
