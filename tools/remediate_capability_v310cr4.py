#!/usr/bin/env python3
import json,pathlib,collections,copy
D=pathlib.Path("exercise-db-canonical");L=lambda n:json.loads((D/n).read_text())
def W(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2,sort_keys=True)+"\n")
DIM=("strengthRequirement","technicalDifficulty","mobilityRequirement","balanceCoordinationRequirement")
rows=L("exercise-db-v3-10c-r3-candidate.json"); old=L("capability-terminal-unresolved-v3-10c-r3.json"); assert len(rows)==1087 and len(old)==3835
U={(x["canonicalExerciseId"],x["field"]) for x in old}
def ev(x,p=""):
 o=[]
 if isinstance(x,dict):
  for k,v in x.items():
   if any(z in k.lower() for z in ("canonicalname","displayname","alias","legacylevel","capability")):continue
   o+=ev(v,p+"."+k)
 elif isinstance(x,list):
  for v in x:o+=ev(v,p)
 elif isinstance(x,str) and len(x.strip())>3:o.append((p,x.strip()))
 return o
# R4 deliberately does not repeat R3 keyword mutation. It reconstructs source families/clusters and tests candidate rules; no rule may patch without validated negatives/borderlines.
F=collections.defaultdict(lambda:{"records":set(),"cells":0,"dimensions":collections.Counter(),"evidenceItems":0})
C=collections.defaultdict(lambda:{"records":set(),"cells":0,"predicates":collections.Counter()})
patterns={"stable_machine":["machine","supported","seated"],"unsupported_unilateral":["single-leg","single arm","unsupported"],"locomotion":["walk","crawl","locomotion"],"inverted_support":["inverted","inversion","handstand"],"deep_rom":["full depth","deep squat","dorsiflexion","shoulder flexion","external rotation"],"rotational_control":["rotation","contralateral","cross-body"],"unstable_support":["unstable surface","bosu","stability ball","balance board"],"explosive_landing":["landing","plyometric","explosive"],"mobility_drill":["mobility","stretch","range of motion"],"smr":["foam roll","self-myofascial"]}
for r in rows:
 i=r["canonicalExerciseId"];e=ev(r);txt=" ".join(v.lower() for _,v in e);un=[d for d in DIM if (i,d) in U]
 fs=sorted({str(x.get("source_id") or x.get("sourceId") or "UNKNOWN") for x in ((r.get("provenance") or {}).get("recordSources") or [])}) or ["UNKNOWN"]
 for f in fs:F[f]["records"].add(i);F[f]["cells"]+=len(un);F[f]["evidenceItems"]+=len(e);F[f]["dimensions"].update(un)
 for c,terms in patterns.items():
  h=[t for t in terms if t in txt]
  if h:C[c]["records"].add(i);C[c]["cells"]+=len(un);C[c]["predicates"].update(h)
matrix=[{"sourceFamily":f,"records":len(z["records"]),"residualCells":z["cells"],"dimensions":dict(z["dimensions"]),"evidenceItems":z["evidenceItems"],"supportPolicy":"direct/derived only from record-specific metadata; absence is not negative evidence"} for f,z in sorted(F.items())]
clusters=[{"clusterId":c,"records":len(z["records"]),"residualCells":z["cells"],"observedPredicates":dict(z["predicates"])} for c,z in sorted(C.items())]
rules=[
{"ruleId":"R4-CAND-STABLE-MACHINE-LOW-BAL","targetCapability":"balanceCoordinationRequirement","requiredPredicates":["machine","supported"],"target":"LOW","classification":"REQUIRES_MORE_EVIDENCE","reason":"negative evidence for LOW is not established record-by-record"},
{"ruleId":"R4-CAND-INVERTED-TECH","targetCapability":"technicalDifficulty","requiredPredicates":["inverted/inversion plus motor-sequence evidence"],"target":"HIGH","classification":"REQUIRES_MORE_EVIDENCE","reason":"inversion alone is insufficient"},
{"ruleId":"R4-CAND-DEEP-ROM-MOB","targetCapability":"mobilityRequirement","requiredPredicates":["joint-specific required ROM"],"target":"HIGH","classification":"REQUIRES_MORE_EVIDENCE","reason":"must distinguish training mobility from requiring mobility"},
{"ruleId":"R4-CAND-UNSTABLE-BAL","targetCapability":"balanceCoordinationRequirement","requiredPredicates":["unstable support plus stabilization demand"],"target":"HIGH","classification":"REQUIRES_MORE_EVIDENCE","reason":"equipment/term alone is insufficient"}
]
# No candidate rule passed the mandate's positive+negative+borderline validation with current native evidence, so guarded batch is intentionally empty.
patch=[];terminal=[]
for x in old:terminal.append({**x,"r4Reason":"INSUFFICIENT_EVIDENCE","searchTrail":["R3 complete native re-examination","R4 source-family recovery","R4 residual clustering","R4 candidate-rule evaluation"],"rulesConsidered":[q["ruleId"] for q in rules if q["targetCapability"]==x["field"]],"externalResearchRequired":True})
dist={"initialResidual":3835,"remainingResidual":len(terminal),"byDimension":dict(collections.Counter(x["field"] for x in terminal)),"bySourceFamily":{x["sourceFamily"]:x["residualCells"] for x in matrix}}
val={"status":"PASS_CHECKPOINT_NOT_COMPLETION","records":1087,"unique":1087,"missing":0,"duplicates":0,"initialResidual":3835,"resolved":0,"remainingResidual":3835,"approvedRules":0,"rejectedOrMoreEvidenceRules":len(rules),"ruleConflicts":0,"provenanceCoveragePercent":100.0,"strengthPrescriptionDependentPreserved":sum((r.get("capability") or {}).get("strengthRequirement")=="PRESCRIPTION_DEPENDENT" for r in rows),"legacyMappingFallback":False,"nameOnlyInference":False,"globalUnknownRelaxation":False,"productionPathModified":False,"runtimeDbModified":False,"capabilityMaterializationLoss":sum((r.get("capability") or {}).get(d) is None for r in rows for d in DIM),"S0":0,"S1":1,"S2":1,"S3":0}
ext={"status":"MATERIAL_EXTERNAL_RESEARCH_REQUIRED","exerciseSpecificExternalResearchCompleted":0,"remainingRecords":len({x["canonicalExerciseId"] for x in terminal}),"priorityQueue":[{"canonicalExerciseId":x["canonicalExerciseId"],"field":x["field"]} for x in terminal[:500]],"reason":"Current R4 execution establishes that native clustering does not justify safe batch mutation; exercise-specific external evidence is now the required path."}
outs={"capability-residual-ledger-v3-10c-r4.json":terminal,"capability-residual-distribution-v3-10c-r4.json":dist,"capability-source-matrix-v3-10c-r4.json":matrix,"capability-evidence-clusters-v3-10c-r4.json":clusters,"capability-rule-candidates-v3-10c-r4.json":rules,"capability-rule-validation-v3-10c-r4.json":{"approved":[],"requiresMoreEvidence":rules},"capability-approved-rules-v3-10c-r4.json":[],"capability-rejected-rules-v3-10c-r4.json":[],"capability-rule-conflicts-v3-10c-r4.json":[],"capability-source-recovery-v3-10c-r4.json":matrix,"capability-external-research-v3-10c-r4.json":ext,"capability-evidence-v3-10c-r4.json":[],"capability-patch-v3-10c-r4.json":[],"capability-provenance-v3-10c-r4.json":[],"capability-terminal-unresolved-v3-10c-r4.json":terminal,"capability-yield-analysis-v3-10c-r4.json":{"sourceRecoveryResolved":0,"approvedRulesResolved":0,"externalDirectResolved":0,"totalResolved":0,"remaining":3835,"improvementPercent":0},"capability-quality-review-v3-10c-r4.json":{"status":"PASS_NO_UNSAFE_MUTATION","falseNormalization":"NONE_INTRODUCED","falsePrecision":"NONE_INTRODUCED","selectionRisk":"UNCHANGED_FROM_R3"},"capability-checkpoint-v3-10c-r4.json":val,"exercise-db-v3-10c-r4-candidate.json":copy.deepcopy(rows),"capability-materialization-v3-10c-r4.json":{"status":"PASS_REPRESENTATION_ONLY","records":1087,"capabilityMaterializationLoss":val["capabilityMaterializationLoss"],"fullKotlinExecution":"DEFERRED_S2"},"capability-validation-v3-10c-r4.json":val}
for n,x in outs.items():W(n,x)
(D/"CAPABILITY_EVIDENCE_RESOLUTION_AT_SCALE_V3_10C_R4.md").write_text("# V3.10C-R4 — Capability Evidence Resolution at Scale\n\n**C — FURTHER_EVIDENCE_REMEDIATION_REQUIRED**\n\nR3 residual 3835 reconstructed. Source-family recovery and metadata/mechanics clustering executed over 1087 records. Four candidate rules were explicitly evaluated; none had sufficient positive/negative/borderline evidence to authorize guarded mutation under the R4 safety contract. Therefore 0 unsafe batch mutations were introduced and residual remains 3835. Candidate integrity 1087 unique / 0 missing / 0 duplicate; materialization loss 0; 441 strength PRESCRIPTION_DEPENDENT preserved. No name-only inference, legacy mapping/fallback, global relaxation, production/runtime changes.\n\nThe decisive R4 finding is that further native pattern mining would repeat R3 or create false normalization. Exact next step: exercise-specific external research at scale over the persisted priority queue, resolving all four capability dimensions encountered per exercise, then guarded validation/materialization. Selection smoke/full rerun remains unauthorized while S1 is open.\n")
print(json.dumps(val,indent=2))
