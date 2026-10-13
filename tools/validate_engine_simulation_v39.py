#!/usr/bin/env python3
import json,pathlib
D=pathlib.Path("exercise-db-canonical")
def L(n): return json.loads((D/n).read_text())
def main():
 s=L("simulation-summary-v3-9.json"); sc=L("simulation-scenarios-v3-9.json"); sk=L("skill-simulation-results-v3-9.json")
 c=L("exercise-db-v3-engine-ready-candidate.json"); c=c.get("records",c) if isinstance(c,dict) else c
 ids=[x.get("canonicalExerciseId") or x.get("id") for x in c]
 assert len(c)==1087 and len(set(ids))==1087 and None not in ids
 assert len(sk)==60 and len({x["nodeId"] for x in sk})==60
 assert len(sc)==640 and all("seed" in x and "scenarioId" in x for x in sc)
 assert s["S0"]==0 and s["S1"]>=1 and s["status"]=="NOT_READY_BLOCKED" and s["runtimeDbModified"] is False and s["silentSemanticLossCount"]==0 and s["falseNormalizationCount"]==0
 allowed={"DATA_GAP","IDENTITY_GAP","SCHEMA_GAP","RUNTIME_ADAPTER_GAP","RUNTIME_MODEL_GAP","SELECTION_ALGORITHM_GAP","RANKING_VARIETY_GAP","CAPABILITY_GAP","SKILL_GRAPH_GAP","PRESCRIPTION_GAP","EQUIPMENT_LOCATION_GAP","SUBSTITUTION_GAP","TEST_HARNESS_BUG","VALIDATOR_BUG","TEST_EXPECTATION_BUG"}
 assert all(x["failureClass"] in allowed for x in L("simulation-failures-v3-9.json"))
 v={"status":"PASS","candidate":1087,"unique":1087,"missing":0,"duplicates":0,"scenarioSchema":"PASS","skillNodeCoverage":"60/60","determinismMetadata":"PASS","runtimeDbModified":False,"summaryConsistency":"PASS"}
 (D/"simulation-validation-v3-9.json").write_text(json.dumps(v,indent=2)+"\n"); print(v)
if __name__=="__main__": main()
