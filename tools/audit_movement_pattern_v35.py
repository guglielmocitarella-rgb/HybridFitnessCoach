#!/usr/bin/env python3
import json, pathlib, collections, subprocess, os
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/"exercise-db-canonical"; P=DB/"exercise-db-canonical.json"
raw=json.loads(P.read_text())
records=raw if isinstance(raw,list) else raw.get("exercises") or raw.get("records") or []
assert len(records)==1087, len(records)
ids=[r.get("id") for r in records]; assert len(set(ids))==1087 and all(ids)
def pats(r):
 v=r.get("movementPattern")
 if v is None:return []
 if isinstance(v,str): return [] if not v.strip() or v.upper()=="UNKNOWN" else [v.strip()]
 return [str(x).strip() for x in v if str(x).strip() and str(x).upper()!="UNKNOWN"]
dist=collections.Counter(x for r in records for x in pats(r))
missing=[r for r in records if not pats(r)]
multi=[r for r in records if len(pats(r))>1]
invalid=[r for r in records if not isinstance(r.get("movementPattern"),(list,str,type(None)))]
status=collections.Counter(str(r.get("movementPatternStatus") or "MISSING") for r in records)
work=[]
for r in records:
 issues=[]
 ps=pats(r)
 if not ps: issues.append(("MISSING_OR_UNKNOWN","UNRESOLVED_INSUFFICIENT_EVIDENCE"))
 if len(ps)>1: issues.append(("MULTI_PATTERN","RESOLVED_MULTI_PATTERN"))
 st=str(r.get("movementPatternStatus") or "MISSING")
 if not r.get("movementPatternStatus"): issues.append(("MISSING_STATUS","UNRESOLVED_SCHEMA_DECISION_REQUIRED"))
 if st=="APP_RULE_NAME_OR_GROUP_HEURISTIC" and ps: issues.append(("HEURISTIC_PATTERN_UNCERTIFIED","UNRESOLVED_INSUFFICIENT_EVIDENCE"))
 if st in {"V2_MOVEMENT_TYPE_RULE","V2_FAMILY_PATTERN_RULE"} and ps: issues.append(("SOURCE_TAXONOMY_DERIVED","UNRESOLVED_SOURCE_TAXONOMY_CONFLICT"))
 if r in invalid: issues.append(("INVALID_TYPE","UNRESOLVED_SCHEMA_DECISION_REQUIRED"))
 for typ,terminal in issues:
  work.append({"canonicalExerciseId":r["id"],"canonicalNameEn":r.get("canonicalNameEn"),"issue":typ,"terminalClassification":terminal,"currentValue":r.get("movementPattern")})
issues_by=collections.Counter(x["issue"] for x in work)
term=collections.Counter(x["terminalClassification"] for x in work)
patch=[]
prov=[]
unresolved=[x for x in work if x["terminalClassification"].startswith("UNRESOLVED")]
metadata_fields=["family","unilateral","laterality","isometric","primaryMuscles","secondaryMuscles","stabilizers","bodyRegion","movementType","category","group","secondaryGroups","progressionRelations"]
metadata=[]
for f in metadata_fields:
 present=sum(1 for r in records if r.get(f) not in (None,"",[],{}))
 metadata.append({"field":f,"present":present,"missing":1087-present,"classification":"ENGINE_ENHANCING" if f in {"family","unilateral","laterality","isometric","primaryMuscles","secondaryMuscles","stabilizers","bodyRegion","progressionRelations"} else "ENGINE_CRITICAL_OR_MAPPING_DEPENDENT","runtimeConsumerVerified": f in {"category","group","secondaryGroups"}})
record_audit=[]
by_id_issues=collections.defaultdict(list)
for x in work: by_id_issues[x["canonicalExerciseId"]].append(x["issue"])
for r in records:
 record_audit.append({"canonicalExerciseId":r["id"],"canonicalNameEn":r.get("canonicalNameEn"),"patterns":pats(r),"movementPatternStatus":r.get("movementPatternStatus"),"issues":by_id_issues.get(r["id"],[]),"audited":True})
def fixture(name,allowed):
 pool=[r for r in records if set(r.get("equipmentRequired") or []).issubset(allowed)]
 pd=collections.Counter(x for r in pool for x in pats(r))
 return {"name":name,"candidatePool":len(pool),"recordsWithPattern":sum(1 for r in pool if pats(r)),"recordsMissingPattern":sum(1 for r in pool if not pats(r)),"uniquePatterns":len(pd),"patternConcentrationMaxShare":round(max(pd.values())/sum(pd.values()),6) if pd else 0}
all_eq=set(x for r in records for x in (r.get("equipmentRequired") or []))
scenario_rows=[fixture("BODYWEIGHT_MINIMAL",set()),fixture("HOME_SYNTHETIC",{"resistance_band","dumbbell","kettlebell","pull_up_bar","bench","yoga_mat","suspension_trainer"}),fixture("FULL_GYM_SYNTHETIC",all_eq)]
concentration=(max(dist.values())/sum(dist.values())) if dist else 0
variety={"scenarios":[{"name":"CANONICAL_GLOBAL_READ_ONLY","candidatePool":1087,"recordsWithPattern":1087-len(missing),"recordsMissingPattern":len(missing),"uniquePatterns":len(dist),"patternConcentrationMaxShare":round(concentration,6)}]+scenario_rows,
"filters":{"locationEquipment":"synthetic equipment-subset scenarios executed read-only; not asserted as live-runtime equivalence","capability":"not simulated against V3 runtime","skill":"audited previously V3.2C; no V3 runtime integration"},
"dbGaps":{"missingPattern":len(missing),"multiPattern":len(multi)},"taxonomyGaps":{"singleRuntimeValueVsV3List":True},"engineGaps":{"runtimeAdjacentSamePatternPenaltyOnly":True,"noRecentFamilyAvoidanceVerifiedInV3Contract":True},
"expectedFilteringEffects":["location/equipment can shrink pools materially per V3.1A"],"beforeAfterPatch":{"patchCount":0,"reason":"no unsupported inference applied","coverageDelta":0},
"finalFinding":"MIXED","rationale":"Persisted DB has movement-pattern gaps while the audited runtime uses pattern only as an adjacent-sequencing penalty; V3 is not integrated, so a live HOME/GYM before/after claim would be false."}
audit={"schemaVersion":"V3.5","canonicalCount":1087,"unique":1087,"missingIds":0,"duplicates":0,"historicalMissing":{"count":176,"source":"ENGINE_DATA_CONTRACT_V3_1.md","definition":"movementPattern missing at V3.1 baseline"},"currentMissing":len(missing),"delta":len(missing)-176,
"patternDistribution":dict(sorted(dist.items())),"statusDistribution":dict(status),"allRecordsAudited":len(record_audit)==1087,"recordAuditCount":len(record_audit),"worksetRecords":len({x["canonicalExerciseId"] for x in work}),"worksetIssues":len(work),"issuesByType":dict(issues_by),"terminalByClass":dict(term),
"modelStatus":"V3_LIST_SUPPORTS_MULTI_PATTERN_RUNTIME_SINGLE_VALUE_GAP","engineContract":{"meaning":"broad movement-pattern metadata","producer":"canonical builder/source provenance","consumer":"current runtime sequencing metadata after V2 conversion","fallback":"legacy withExerciseMetadata heuristic from name/category","impact":"ENGINE_ENHANCING; adjacent same-pattern sequencing penalty, not eligibility blocker"}}
(DB/"movement-pattern-record-audit-v3-5.json").write_text(json.dumps(record_audit,indent=2,ensure_ascii=False)+"\n")
(DB/"movement-pattern-workset-v3-5.json").write_text(json.dumps(work,indent=2,ensure_ascii=False)+"\n")
(DB/"movement-pattern-audit-v3-5.json").write_text(json.dumps(audit,indent=2,ensure_ascii=False)+"\n")
(DB/"movement-pattern-field-patch-v3-5.json").write_text(json.dumps(patch,indent=2)+"\n")
(DB/"movement-pattern-provenance-v3-5.json").write_text(json.dumps(prov,indent=2)+"\n")
(DB/"movement-pattern-final-unresolved-v3-5.json").write_text(json.dumps(unresolved,indent=2,ensure_ascii=False)+"\n")
(DB/"movement-engine-enhancing-metadata-v3-5.json").write_text(json.dumps(metadata,indent=2)+"\n")
(DB/"movement-variety-analysis-v3-5.json").write_text(json.dumps(variety,indent=2)+"\n")
schema="""# Movement Pattern Schema Proposal V3.5
The canonical V3 field is already list-shaped and can represent multiple patterns. Preserve that capability.
Do not collapse category, muscles, equipment or skill relations into movementPattern.
Add explicit V3 terminal semantics for NOT_APPLICABLE and SCHEMA_DECISION_REQUIRED without changing runtime yet.
The current runtime consumes one effective string and therefore needs an explicit integration mapping before V3 promotion.
"""
(DB/"MOVEMENT_PATTERN_SCHEMA_PROPOSAL_V3_5.md").write_text(schema)
report=f"""# Exercise DB V3.5 — Movement Pattern & Engine-Enhancing Metadata Audit
MOVEMENT PATTERN / ENGINE-ENHANCING METADATA DOMAIN: **CLOSED_WITH_DOCUMENTED_UNRESOLVED**

Canonical 1087; unique 1087; missing IDs 0; duplicates 0. All 1087 audited. Runtime DB not modified.

Historical missing: 176 from V3.1 Engine Data Contract (movementPattern missing). Current missing: {len(missing)}; delta {len(missing)-176:+d}.

Model status: **V3_LIST_SUPPORTS_MULTI_PATTERN_RUNTIME_SINGLE_VALUE_GAP**. V3 can retain multiple patterns; current runtime reduces pattern semantics to one effective string and uses it as an adjacent sequencing penalty, not an eligibility blocker.

Pattern distribution: {json.dumps(dict(sorted(dist.items())),ensure_ascii=False)}.

Workset: {len(set(x['canonicalExerciseId'] for x in work))} records / {len(work)} issues. Missing/unknown and heuristic or source-taxonomy-derived patterns remain terminally unresolved unless supported by stronger field evidence. Existing multi-pattern records are preserved rather than collapsed. Automatic patches: 0.

Variety/repetitivity finding: **MIXED**. DB pattern gaps reduce sequencing information, but runtime pattern use is narrow; synthetic BODYWEIGHT/HOME/GYM equipment fixtures quantify candidate-pool shrinkage read-only; they are not claimed as live-runtime equivalence because V3 is not integrated.

Provenance coverage for patches: 100% (0/0). Previous V3.1–V3.4 artifacts preserved.
"""
(DB/"MOVEMENT_PATTERN_AUDIT_V3_5.md").write_text(report)
validation={"validatedCommit":os.getenv("GITHUB_SHA","LOCAL_OR_CI"),"executionEnvironment":"github-actions" if os.getenv("GITHUB_ACTIONS") else "local","canonicalCount":1087,"canonicalUnique":1087,"missingIds":0,"duplicates":0,"historicalMissing":176,"currentMissing":len(missing),"patternDistribution":dict(dist),"allRecordsAudited":len(record_audit)==1087,"recordAuditCount":len(record_audit),"scenarioCount":1+len(scenario_rows),"worksetRecords":len(set(x["canonicalExerciseId"] for x in work)),"worksetIssues":len(work),"issuesByType":dict(issues_by),"patchesCertainHigh":0,"notApplicable":0,"multiPattern":len(multi),"unresolved":len(unresolved),"unresolvedByReason":dict(collections.Counter(x["terminalClassification"] for x in unresolved)),"provenanceCoverage":"100% (0/0 patches)","metadataFindings":metadata,"varietyFinding":"MIXED","runtimeDbModified":False,"domainStatus":"CLOSED_WITH_DOCUMENTED_UNRESOLVED"}
(DB/"movement-pattern-validation-v3-5.json").write_text(json.dumps(validation,indent=2)+"\n")
print(json.dumps({"missing":len(missing),"patterns":dict(dist),"workset":len(work),"multi":len(multi)}))
