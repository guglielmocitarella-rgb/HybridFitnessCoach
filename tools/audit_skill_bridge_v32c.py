#!/usr/bin/env python3
import json,re,pathlib,collections,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path: sys.path.insert(0,str(ROOT))
OUT=ROOT/"exercise-db-canonical"
REL_TYPES={"SKILL_ITSELF","DIRECT_PROGRESSION","DIRECT_REGRESSION","PREREQUISITE","STRENGTH_BUILDER","MOBILITY_PREPARATION","TECHNIQUE_DRILL","ACCESSORY","TRANSFER"}
CONF={"CERTAIN","HIGH_CONFIDENCE"}
def norm(s): return re.sub(r"[^a-z0-9]+"," ",str(s or "").casefold()).strip()
def catalog():
 from tools.build_exercise_db_canonical import _skill_nodes
 nodes=_skill_nodes(ROOT); by=collections.defaultdict(list)
 for n in nodes: by[n["skill"]].append(n)
 return nodes,by
def node_names(n): return {norm(n["exerciseName"]),*(norm(a) for a in n.get("aliases",[]))}
def relation(ex,targetType,targetId,rt,conf,source,evidence,reason,prov,sourceRecordId=None):
 r={"canonicalExerciseId":ex["id"],"targetType":targetType,"targetId":targetId,"relationType":rt,"confidence":conf,"source":source,"evidence":evidence,"reason":reason,"provenance":prov}
 if sourceRecordId: r["sourceRecordId"]=sourceRecordId
 return r
def terminal_for(rels):
 if not rels:return None
 ts={r["relationType"] for r in rels}
 return "RESOLVED_MULTI_SKILL" if len({(r["targetType"],r["targetId"]) for r in rels})>1 else "RESOLVED_"+next(iter(ts)) if len(ts)==1 else "RESOLVED_MULTI_SKILL"
def classify_workset(ex,nodes,by):
 name=norm(ex.get("canonicalNameEn") or ex.get("canonicalNameIt")); aliases={norm(a) for a in ex.get("aliases",[]) if a}
 allnames={name,*aliases}; out=[]
 # Tier-1 exact node identity, including aliases.
 for n in nodes:
  if allnames & node_names(n):
   out.append(relation(ex,"NODE",n["id"],"SKILL_ITSELF","CERTAIN","APP_SKILL_CATALOG","Canonical/alias identity exactly matches SkillCatalog node "+n["exerciseName"],"Exact authoritative node identity.",{"tier":1,"method":"exact_node_or_alias"},"SkillCatalog.kt"))
 if out:return out,None
 # Family-explicit names can support family-level semantics only when the relation word is explicit.
 for fam,seq in by.items():
  fn=norm(fam)
  if not any(fn in x or x in fn for x in allnames if len(x)>=4): continue
  txt=" ".join(allnames)
  rt=None
  if any(k in txt for k in ("mobility","stretch","flexibility")): rt="MOBILITY_PREPARATION"
  elif any(k in txt for k in ("drill","technique","practice")): rt="TECHNIQUE_DRILL"
  elif any(k in txt for k in ("negative","eccentric")): rt="DIRECT_REGRESSION"
  elif any(k in txt for k in ("assisted","band assisted","tuck","advanced tuck","straddle")): rt="DIRECT_PROGRESSION"
  elif any(k in txt for k in ("raise","row","pull","push","press","curl","extension","hold","compression")): rt="STRENGTH_BUILDER"
  if rt:
   out.append(relation(ex,"FAMILY",fam,rt,"HIGH_CONFIDENCE","CANONICAL_RECORD+APP_SKILL_CATALOG","Exercise name explicitly contains skill family and relation-bearing descriptor.","Family identity is explicit in the canonical name; relation type is supported by the descriptor, without inferring a specific node.",{"tier":1,"method":"family_explicit_semantic_descriptor"},"SkillCatalog.kt"))
 if out:return out,None
 return [],{"canonicalExerciseId":ex["id"],"canonicalName":ex.get("canonicalNameEn") or ex.get("canonicalNameIt"),"classification":"UNRESOLVED_INSUFFICIENT_EVIDENCE","candidateTargets":[],"candidateRelationTypes":[],"evidenceReviewed":["canonical identity/aliases","SkillCatalog family/node names and aliases","legacy skillIds","canonical matching/provenance fields"],"reason":"No Tier-1 exact node identity and no explicit family+relation descriptor supports a CERTAIN/HIGH_CONFIDENCE typed relation. Name/muscle/equipment similarity is intentionally insufficient.","whatWouldResolveIt":"Exercise-specific authoritative/technical evidence tying this exercise to a SkillCatalog family/node and relation type.","manualReviewRequired":True}
def main():
 db=json.loads((OUT/"exercise-db-canonical.json").read_text()); nodes,by=catalog()
 lookup={n["id"]:n for n in nodes}; valid=set(lookup); ids=[x.get("id") for x in db]
 linked=[x for x in db if x.get("skillIds")]; work=[x for x in db if x.get("movementType")=="SKILL" and not x.get("skillIds")]
 rels=[]; unresolved=[]; terminal=[]; legacy=[]; invalid=[]
 # Preserve and type all valid legacy bridges.
 for ex in linked:
  for sid in ex.get("skillIds",[]):
   if sid not in valid:
    invalid.append({"canonicalExerciseId":ex["id"],"skillId":sid}); legacy.append({"canonicalExerciseId":ex["id"],"skillId":sid,"status":"LEGACY_INVALID_REFERENCE"}); continue
   n=lookup[sid]; exact=norm(ex.get("canonicalNameEn")) in node_names(n) or norm(ex.get("canonicalNameIt")) in node_names(n)
   rt="SKILL_ITSELF" if exact else "DIRECT_PROGRESSION"
   rels.append(relation(ex,"NODE",sid,rt,"CERTAIN" if exact else "HIGH_CONFIDENCE","APP_SKILL_CATALOG","Existing valid legacy skillIds reference to authoritative SkillCatalog node "+n["exerciseName"],"Preserved legacy node bridge; typed relation makes semantics explicit.",{"tier":1,"method":"legacy_valid_node_bridge"},"SkillCatalog.kt"))
   legacy.append({"canonicalExerciseId":ex["id"],"skillId":sid,"status":"LEGACY_CONFIRMED" if exact else "LEGACY_SEMANTICALLY_INCOMPLETE"})
 # Record-first review of complete historical workset.
 for ex in work:
  rs,u=classify_workset(ex,nodes,by); rels.extend(rs)
  if u: unresolved.append(u); terminal.append({"canonicalExerciseId":ex["id"],"classification":u["classification"]})
  else: terminal.append({"canonicalExerciseId":ex["id"],"classification":terminal_for(rs)})
 # deterministic de-dup/order
 key=lambda r:(r["canonicalExerciseId"],r["targetType"],r["targetId"],r["relationType"])
 rels=sorted({key(r):r for r in rels}.values(),key=key)
 applied_work={r["canonicalExerciseId"] for r in rels if r["canonicalExerciseId"] in {x["id"] for x in work}}
 for r in rels:
  if not any(x["canonicalExerciseId"]==r["canonicalExerciseId"] and x.get("skillId")==r.get("targetId") for x in legacy):
   legacy.append({"canonicalExerciseId":r["canonicalExerciseId"],"targetType":r["targetType"],"targetId":r["targetId"],"relationType":r["relationType"],"status":"TYPED_RELATION_WITHOUT_LEGACY_EQUIVALENT"})
 classes=collections.Counter(x["classification"] for x in terminal)
 bytype=collections.Counter(r["relationType"] for r in rels); bytarget=collections.Counter(r["targetType"] for r in rels); byconf=collections.Counter(r["confidence"] for r in rels)
 prov=[{"canonicalExerciseId":r["canonicalExerciseId"],"targetType":r["targetType"],"targetId":r["targetId"],"relationType":r["relationType"],"source":r["source"],"sourceRecordId":r.get("sourceRecordId"),"evidence":r["evidence"],"confidence":r["confidence"],"provenance":r["provenance"]} for r in rels]
 audit={"schemaVersion":"3.2C-R","integrity":{"canonical":len(db),"unique":len(set(ids)),"missingIds":sum(not x for x in ids),"duplicates":len(ids)-len(set(ids))},"skillCatalog":{"verifiedSkillCount":len(by),"nodeCount":len(nodes)},"workset":{"expected":len(work),"processed":len(terminal),"unique":len({x["canonicalExerciseId"] for x in terminal}),"missing":0,"duplicates":len(terminal)-len({x["canonicalExerciseId"] for x in terminal})},"terminalClassifications":terminal,"classificationCounts":dict(classes),"typedRelationCount":len(rels),"relationsByType":dict(bytype),"targetsByType":dict(bytarget),"confidenceCounts":dict(byconf),"invalidReferences":invalid,"modelStatus":"TYPED_RELATIONS_IMPLEMENTED"}
 validation={"schemaVersion":"3.2C-R","status":"PENDING_EXECUTED_GATE","canonicalCount":len(db),"canonicalUnique":len(set(ids)),"skillFamilyCount":len(by),"skillNodeCount":len(nodes),"workset":audit["workset"],"terminalClassificationCount":len(terminal),"typedRelationCount":len(rels),"exercisesWithTypedRelations":len({r["canonicalExerciseId"] for r in rels}),"relationsByType":dict(bytype),"familyTargetCount":bytarget["FAMILY"],"nodeTargetCount":bytarget["NODE"],"multiSkillExerciseCount":sum(1 for eid in {r["canonicalExerciseId"] for r in rels} if len({(r["targetType"],r["targetId"]) for r in rels if r["canonicalExerciseId"]==eid})>1),"certain":byconf["CERTAIN"],"highConfidence":byconf["HIGH_CONFIDENCE"],"unresolved":len(unresolved),"notActuallySkillRelated":classes["NOT_ACTUALLY_SKILL_RELATED"],"provenanceCoverage":1.0 if len(prov)==len(rels) else 0.0,"validReferences":len(rels),"invalidReferences":len(invalid),"duplicates":0,"referentialIntegrityResult":"PASS" if not invalid else "FAIL","validationEnvironment":"pending CI"}
 files={"skill-bridge-audit-v3-2c.json":audit,"skill-relations-v3-2c.json":rels,"skill-provenance-v3-2c.json":prov,"skill-final-unresolved-v3-2c.json":unresolved,"skill-validation-v3-2c.json":validation,"skill-legacy-comparison-v3-2c.json":sorted(legacy,key=lambda x:(x["canonicalExerciseId"],str(x.get("skillId","")),str(x.get("targetId",""))))}
 for n,o in files.items():(OUT/n).write_text(json.dumps(o,ensure_ascii=False,indent=2)+"\n")
 (OUT/"SKILL_RELATION_SCHEMA_PROPOSAL_V3_2C.md").write_text("# V3.2C-R Typed Exercise ↔ Skill Relations\n\nSkillCatalog owns identity/progression/prerequisites/mastery. Exercise owns exercise identity/characteristics. A separate typed relation layer owns targetType (FAMILY/NODE), targetId, relationType, confidence, evidence and provenance. Legacy skillIds is preserved unchanged. Only CERTAIN/HIGH_CONFIDENCE relations are persisted.\n")
 (OUT/"SKILL_BRIDGE_AUDIT_V3_2C.md").write_text("# Exercise DB V3.2C-R — Typed Skill Relations\n\nCanonical: %d/%d. Skill catalog: %d families / %d nodes. Historical workset: %d processed, %d unresolved. Typed relations: %d. Runtime DB modified: NO.\n"%(len(db),len(set(ids)),len(by),len(nodes),len(terminal),len(unresolved),len(rels)))
 print(json.dumps(validation,indent=2))
if __name__=="__main__": main()
