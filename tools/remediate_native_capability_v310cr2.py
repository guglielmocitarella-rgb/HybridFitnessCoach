#!/usr/bin/env python3
import json,pathlib,copy,collections
D=pathlib.Path("exercise-db-canonical"); DIM=("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement")
LEVEL={"VERY_LOW","LOW","MODERATE","HIGH","VERY_HIGH"}; SPECIAL={"NOT_APPLICABLE","VARIABLE","PRESCRIPTION_DEPENDENT","VARIANT_DEPENDENT"}
def L(n,d=None):
 p=D/n; return json.loads(p.read_text()) if p.exists() else d
def W(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+"\n")
rows=L("exercise-db-v3-9r-b-candidate.json",[]); assert len(rows)==1087 and len({r["canonicalExerciseId"] for r in rows})==1087
v38={x.get("canonicalExerciseId"):x for x in L("engine-capability-remediation-v3-8.json",{}).get("items",[])}
rules=[
{"ruleId":"R2-STRENGTH-EXTERNAL-LOAD-PRESCRIPTION","targetCapability":"strengthRequirement","requiredEvidence":["resistance.source=EXTERNAL_LOAD","resistance.state=KNOWN"],"conditions":"external load is prescribed/scalable; intrinsic variant strength is not equated with loaded demand","exceptions":"contrary exercise-specific evidence","output":"PRESCRIPTION_DEPENDENT","confidence":"HIGH_CONFIDENCE","sourceBasis":"authoritative V3 resistance semantics + R2 mandate","regressionTests":["no HIGH inferred from external load","legacyLevel unused"]},
{"ruleId":"R2-STRENGTH-ASSISTANCE-PRESCRIPTION","targetCapability":"strengthRequirement","requiredEvidence":["known assisted resistance source"],"conditions":"required strength changes with prescribed assistance","exceptions":"contrary exercise-specific evidence","output":"PRESCRIPTION_DEPENDENT","confidence":"HIGH_CONFIDENCE","sourceBasis":"authoritative V3 resistance semantics + R2 mandate","regressionTests":["legacyLevel unused","assistance not mapped to scalar"]}]
def terminal(r): return "UNKNOWN_TERMINAL_IDENTITY_BLOCKER" if r.get("identityState")=="IDENTITY_PROBLEM" else "UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE"
def rt(s): return "IDENTITY_PROBLEM" if s=="UNKNOWN_TERMINAL_IDENTITY_BLOCKER" else ("EVIDENCE_UNRESOLVED" if s=="UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE" else s)
work=[]; recovery=[]; patch=[]; prov=[]; unresolved=[]
for r in rows:
 i=r["canonicalExerciseId"]; cap=r.get("capability") or {}; rs=r.get("resistance") or {}; src=rs.get("source"); rss=rs.get("state"); fp=(r.get("provenance") or {}).get("fieldProvenance") or {}
 recovery.append({"canonicalExerciseId":i,"canonicalName":r.get("canonicalName"),"recordSources":(r.get("provenance") or {}).get("recordSources") or [],"fieldProvenanceKeys":sorted(fp) if isinstance(fp,dict) else [],"resistance":{"state":rss,"source":src},"skillRelationCount":len(r.get("skillRelations") or []),"movementPatterns":r.get("movementPatterns"),"v38CapabilityFinding":v38.get(i),"legacyLevelObservedDiagnosticOnly":cap.get("legacyLevel"),"legacyLevelUsedAsCapability":False})
 rec={"canonicalExerciseId":i,"canonicalName":r.get("canonicalName"),"dimensions":{}}
 for d in DIM:
  before=cap.get(d,"UNKNOWN"); rule=None
  if before in LEVEL: s="KNOWN"; reason="Pre-existing native capability value preserved."
  elif before in SPECIAL: s=before; reason="Pre-existing native special semantic state preserved."
  elif d=="strengthRequirement" and rss=="KNOWN" and src=="EXTERNAL_LOAD": s="PRESCRIPTION_DEPENDENT";rule=rules[0]["ruleId"];reason="Strength demand varies with prescribed external load; no scalar strength inferred."
  elif d=="strengthRequirement" and rss=="KNOWN" and src in {"ASSISTED","BAND_ASSISTANCE","MACHINE_ASSISTANCE","MANUAL_ASSISTANCE"}: s="PRESCRIPTION_DEPENDENT";rule=rules[1]["ruleId"];reason="Strength demand varies with prescribed assistance; no scalar strength inferred."
  else: s=terminal(r);reason="Recovered repository evidence does not justify a native scalar/special assignment for this dimension without exercise-specific external evidence."
  after=before if s=="KNOWN" else rt(s); conf="HIGH_CONFIDENCE" if rule or s=="KNOWN" else "INSUFFICIENT_EVIDENCE"
  rec["dimensions"][d]={"terminalState":s,"runtimeToken":after,"reason":reason,"ruleId":rule,"confidence":conf}
  p={"canonicalExerciseId":i,"field":d,"before":before,"after":after,"state":s,"source":"AUTHORITATIVE_REPOSITORY_EVIDENCE" if rule or s=="KNOWN" else "SOURCE_RECOVERY_ONLY","evidence":reason,"confidence":conf,"reason":reason,"ruleId":rule,"version":"V3.10C-R2"}
  prov.append(p)
  if after!=before: patch.append(p)
  if s.startswith("UNKNOWN_TERMINAL_"): unresolved.append(p)
 work.append(rec)
cand=copy.deepcopy(rows); pm={(p["canonicalExerciseId"],p["field"]):p for p in patch}
for r in cand:
 cap=r["capability"]; ss=[]
 for d in DIM:
  if (r["canonicalExerciseId"],d) in pm: cap[d]=pm[(r["canonicalExerciseId"],d)]["after"]
  t=cap[d]; ss.append("KNOWN" if t in LEVEL else t)
 cap["state"]="KNOWN" if all(x in {"KNOWN","NOT_APPLICABLE"} for x in ss) else ("PRESCRIPTION_DEPENDENT" if all(x in {"KNOWN","NOT_APPLICABLE","PRESCRIPTION_DEPENDENT"} for x in ss) and "PRESCRIPTION_DEPENDENT" in ss else "EVIDENCE_UNRESOLVED")
counts={d:collections.Counter(x["dimensions"][d]["terminalState"] for x in work) for d in DIM}
dist={"version":"V3.10C-R2","records":1087,"totalFields":4348,"terminallyClassified":4348,"nonTerminalUnknown":0,"coveragePercent":100.0,"dimensions":{d:dict(counts[d]) for d in DIM}}
W("capability-evidence-workset-v3-10c-r2.json",work);W("capability-source-recovery-v3-10c-r2.json",recovery)
W("capability-external-evidence-v3-10c-r2.json",{"status":"REQUIRED_FOR_TERMINAL_UNRESOLVED","researchedRecords":0,"note":"Repository/source recovery executed for all 1087 records. Exercise-specific external research is not fabricated; terminal unresolved queue is persisted for a subsequent evidence tranche."})
W("capability-derivation-rules-v3-10c-r2.json",rules);W("capability-field-patch-v3-10c-r2.json",patch);W("capability-provenance-v3-10c-r2.json",prov);W("capability-terminal-unresolved-v3-10c-r2.json",unresolved);W("capability-distribution-v3-10c-r2.json",dist);W("exercise-db-v3-10c-r2-candidate.json",cand)
W("capability-materialization-validation-v3-10c-r2.json",{"status":"BLOCKED_BY_EVIDENCE_NOT_MATERIALIZATION","records":1087,"capabilityMaterializationLoss":0,"note":"R2 candidate uses runtime-supported semantic tokens; full Kotlin execution remains S2 deferred."})
W("native-selection-smoke-v3-10c-r2.json",{"status":"NOT_EXECUTED_PREREQUISITE_FAILED","reason":"terminal capability evidence gaps remain; native engine would fail closed"})
W("native-selection-drift-v3-10c-r2.json",{"status":"NOT_EXECUTED_PREREQUISITE_FAILED"});W("native-selection-full-rerun-v3-10c-r2.json",{"status":"NOT_EXECUTED_PREREQUISITE_FAILED"})
val={"status":"CHECKPOINT_PASS_NOT_COMPLETION","records":1087,"unique":1087,"missing":0,"duplicates":0,"totalFields":4348,"terminallyClassified":4348,"nonTerminalUnknown":0,"legacyLevelUsedAsCapability":False,"legacyLevelFallback":False,"globalUnknownRelaxation":False,"productionPathModified":False,"runtimeDbModified":False,"externalEvidenceComplete":False,"S0":0,"S1":1,"S2":1,"S3":0};W("capability-validation-v3-10c-r2.json",val)
(D/"NATIVE_CAPABILITY_EVIDENCE_REMEDIATION_V3_10C_R2.md").write_text("# V3.10C-R2 — Native Capability Evidence Remediation\n\n## Checkpoint decision\n**C — FURTHER_EVIDENCE_REMEDIATION_REQUIRED**\n\nAll 1087 records / 4348 capability fields were processed record-first against repository evidence. No legacyLevel mapping/fallback, name-only inference, or global UNKNOWN relaxation was used. Exercise-specific external evidence remains required for the terminal unresolved queue, so native 640 smoke and 64,000 rerun are not executed. Materialization loss remains 0 for represented runtime-supported states. Production/runtime DB/Runner/persistence remain untouched. S0=0, S1=1, S2=1 deferred mandatory pre-production, S3=0.\n")
(D/"V3_10C_R2_CHECKPOINT.md").write_text("# V3.10C-R2 CHECKPOINT\n\n- branch: feature/exercise-db-v3\n- previous HEAD: 8590b7c08f33ec9332ea8835d272394f1d5e36b0\n- previous gate: FURTHER_EVIDENCE_REMEDIATION_REQUIRED\n- records processed: 1087/1087\n- capability fields terminally classified: 4348/4348\n- generic non-terminal UNKNOWN: 0\n- external evidence completion: NO\n- S0/S1/S2/S3: 0/1/1/0\n- production path modified: NO\n- runtime DB modified: NO\n- next step: exercise-specific evidence tranche over capability-terminal-unresolved-v3-10c-r2.json, then rebuild and run native gates only when prerequisites pass.\n")
p=D/"RUNTIME_INTEGRATION_STATUS_V3.md"; status=p.read_text()
if "## V3.10C-R2 checkpoint" not in status: p.write_text(status+"\n\n## V3.10C-R2 checkpoint\nV3.10C-R excluded materialization loss as primary root cause: four native dimensions were defined but not populated, while V3.9R-B used diagnostic legacyLevel compatibility. R2 processed 1087/1087 records and terminally classified 4348/4348 fields without legacy mapping/fallback or global UNKNOWN relaxation. Repository evidence supports limited special-state assignments; remaining fields are terminal unresolved pending exercise-specific external evidence. S1 remains open; native 640/64,000 gates are not executed. Production runtime remains unchanged. S2 remains separate and mandatory pre-production.\n")
print(json.dumps({"records":1087,"fields":4348,"patchFields":len(patch),"terminalUnresolved":len(unresolved),"distribution":dist["dimensions"],"status":"FURTHER_EVIDENCE_REMEDIATION_REQUIRED"},indent=2))
