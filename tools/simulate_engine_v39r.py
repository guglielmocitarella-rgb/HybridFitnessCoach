#!/usr/bin/env python3
"""V3.9R non-coercive capability remediation wrapper.

Keeps the four V3 capability dimensions authoritative when fully evidenced.
If they are unresolved, an explicitly KNOWN legacyLevel may be used only as a
compatibility gate. It is never copied/projected into V3 capability fields.
"""
import json, pathlib
import simulate_engine_v39 as v39

D=pathlib.Path("exercise-db-canonical")
DIMS=("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement")

def cap_eval(r,p):
    c=r.get("capability") or {}
    if c.get("state") in ("KNOWN","NOT_APPLICABLE") and all(c.get(k,"UNKNOWN")!="UNKNOWN" for k in DIMS):
        return all(v39.rank(c.get(k))<=v39.rank(p.get(k,"LOW")) for k in DIMS), "V3_NATIVE"
    lv=c.get("legacyLevel")
    if c.get("legacyLevelStatus")=="KNOWN" and v39.known(lv):
        athlete=min(v39.rank(p.get(k,"LOW")) for k in DIMS)
        return v39.rank(lv)<=athlete, "LEGACY_COMPATIBILITY"
    return False, "FAIL_CLOSED_UNRESOLVED"

v39.cap_ok=lambda r,p: cap_eval(r,p)[0]
v39.main()

sc=json.loads((D/"simulation-scenarios-v3-9.json").read_text())
summary=json.loads((D/"simulation-summary-v3-9.json").read_text())
empty=[x["scenarioId"] for x in sc if x.get("eligiblePool",0)==0]
summary["selectionPass"]=len(sc)-len(empty)
summary["selectionFail"]=len(empty)
summary["S1"]=1 if empty else 0
summary["status"]="NOT_READY_BLOCKED" if empty else "READY_FOR_RUNTIME_ADAPTER_WITH_DOCUMENTED_CAPABILITY_DEBT"
summary["capabilitySelectionPolicy"]="V3_NATIVE_ELSE_EXPLICIT_LEGACY_COMPATIBILITY_NO_DIMENSION_MAPPING"
summary["legacyCompatibilityIsCapabilityMapping"]=False
(D/"simulation-summary-v3-9.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n")

failures=json.loads((D/"simulation-failures-v3-9.json").read_text())
runtime=[x for x in failures if x.get("failureId")=="V39-F-002"]
if empty:
    runtime.insert(0,{"failureId":"V39R-F-001","severity":"S1_CORE_FUNCTION_BLOCKED","failureClass":"COVERAGE_GAP","scenarioIds":empty,
      "rootCause":"Some required scenarios remain empty after non-coercive compatibility handling.",
      "requiredRemediation":"Evidence-first capability/coverage remediation; unresolved dimensions remain fail-closed."})
(D/"simulation-failures-v3-9.json").write_text(json.dumps(runtime,ensure_ascii=False,indent=2)+"\n")
(D/"simulation-blocker-queue-v3-9.json").write_text(json.dumps([] if not empty else [{
 "blockerId":"V39R-B-001","severity":"S1_CORE_FUNCTION_BLOCKED","failureClass":"COVERAGE_GAP","scenarioIds":empty,
 "requiredRemediation":"Evidence-first capability/coverage remediation; no blind legacy-level mapping.",
 "safeToIntegrateBeforeFix":False}],ensure_ascii=False,indent=2)+"\n")

report={
 "policy":"V3 native capability when fully evidenced; otherwise explicit audited legacy compatibility only",
 "dimensionsPreserved":list(DIMS),"blindLegacyMapping":False,"emptyScenarios":len(empty),
 "scenarioCount":len(sc),"status":summary["status"]}
(D/"capability-selection-remediation-v3-9r.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
print(json.dumps(report,indent=2))
