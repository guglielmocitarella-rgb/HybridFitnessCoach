#!/usr/bin/env python3
import json,re,pathlib,collections,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path: sys.path.insert(0,str(ROOT))
OUT=ROOT/"exercise-db-canonical"
REL_TYPES={"SKILL_ITSELF","DIRECT_PROGRESSION","DIRECT_REGRESSION","PREREQUISITE_EXERCISE","ACCESSORY","STRENGTH_BUILDER","MOBILITY_PREPARATION","TECHNIQUE_DRILL","TRANSFER_EXERCISE","RELATED_ONLY"}
def norm(s): return re.sub(r"[^a-z0-9]+"," ",str(s).casefold()).strip()
def catalog():
 from tools.build_exercise_db_canonical import _skill_nodes
 nodes=_skill_nodes(ROOT); by=collections.defaultdict(list)
 for n in nodes: by[n["skill"]].append(n)
 return nodes,by
def classify_node(n,by):
 seq=by[n["skill"]]; last=seq[-1]["id"]==n["id"]
 return "SKILL_ITSELF" if last and (norm(n["exerciseName"])==norm(n["skill"]) or norm(n["skill"]) in norm(n["exerciseName"])) else "DIRECT_PROGRESSION"
def main():
 db=json.loads((OUT/"exercise-db-canonical.json").read_text()); nodes,by=catalog(); lookup={n["id"]:n for n in nodes}; valid=set(lookup)
 linked=[x for x in db if x.get("skillIds")]; missing=[x for x in db if x.get("movementType")=="SKILL" and not x.get("skillIds")]
 rels=[]; suspicious=[]; invalid=[]; classes=collections.Counter()
 for ex in linked:
  for sid in ex.get("skillIds",[]):
   if sid not in valid: invalid.append({"exerciseId":ex["id"],"skillId":sid}); continue
   n=lookup[sid]; rt=classify_node(n,by)
   r={"canonicalExerciseId":ex["id"],"skillId":sid,"skillFamily":n["skill"],"relationType":rt,"source":"APP_SKILL_CATALOG","sourceRecordId/url":"SkillCatalog.kt + MainActivity.kt","evidence":"Existing exact SkillCatalog node/alias bridge: "+n["exerciseName"],"confidence":"HIGH","reason":"Internal authoritative node identity; no accessory inference.","provenance":{"tier":1,"method":"runtime_catalog_exact_identity"}}
   rels.append(r); classes["RESOLVED_"+rt]+=1
   names={norm(n["exerciseName"]),*(norm(a) for a in n.get("aliases",[]))}
   if norm(ex.get("canonicalNameEn")) not in names and norm(ex.get("canonicalNameIt")) not in names: suspicious.append({"exerciseId":ex["id"],"skillId":sid,"reason":"persisted link is not an exact canonical/alias identity"})
 records=[]
 for ex in missing:
  rec={"canonicalExerciseId":ex["id"],"canonicalName":ex.get("canonicalNameEn"),"movementType":ex.get("movementType"),"existingSkillIds":ex.get("skillIds",[]),"category":ex.get("category"),"group":ex.get("group"),"family":ex.get("family"),"level":ex.get("level"),"equipment":ex.get("equipmentRequired"),"resistanceMode":ex.get("resistanceMode"),"matchingEvidence":ex.get("matching"),"instructions":{"description":bool(ex.get("descriptionIt")),"setup":bool(ex.get("setupIt")),"execution":bool(ex.get("executionStepsIt"))},"classification":"UNRESOLVED_SCHEMA_DECISION_REQUIRED","reason":"movementType=SKILL is broader than exact SkillCatalog node identity; no Tier-1 exact node/alias bridge exists. Typed utility/progression semantics must not be invented into skillIds."}
  records.append(rec); classes[rec["classification"]]+=1
 ids=[x.get("id") for x in db]; skills=[{"skill":k,"nodeIds":[n["id"] for n in v],"nodeNames":[n["exerciseName"] for n in v]} for k,v in by.items()]
 audit={"schemaVersion":"3.2C","sourceHead":"ca11aca5cba07c95a8778bad321127b818264107","integrity":{"canonical":len(db),"unique":len(set(ids)),"missingIds":sum(not x for x in ids),"duplicates":len(ids)-len(set(ids))},"skillCatalog":{"verifiedSkillCount":len(by),"nodeCount":len(nodes),"skills":skills},"currentSemantics":{"skillIds":"SkillCatalog progression-node IDs resolved by exact runtime exercise name/alias; not a typed utility relation.","movementTypeSKILL":"Broad exercise classification; does not prove membership in a SkillCatalog family.","mastery":"Owned by SkillCatalog/ProgressionEngine prerequisites and mastery criteria; not duplicated into Exercise."},"modelStatus":"INSUFFICIENT_NEEDS_TYPED_RELATIONS","existingLinks":{"exercisesWithLinks":len(linked),"totalLinks":sum(len(x.get("skillIds",[])) for x in linked),"validReferences":len(rels),"invalidReferences":len(invalid),"suspiciousLinks":len(suspicious),"systematicIssue":bool(suspicious or invalid)},"missingUnstableWorkset":{"verifiedCount":len(missing),"records":records},"classificationCounts":dict(classes),"invalidReferences":invalid,"suspiciousLinks":suspicious}
 unresolved=[r for r in records if r["classification"].startswith("UNRESOLVED")]
 validation={"schemaVersion":"3.2C","status":"PENDING_CI","canonical":len(db),"unique":len(set(ids)),"skillCatalogCount":len(by),"skillNodeCount":len(nodes),"missingWorksetAudited":len(records),"existingLinksAudited":sum(len(x.get("skillIds",[])) for x in linked),"referentialIntegrity":"PASS" if not invalid else "FAIL","modelStatus":"INSUFFICIENT_NEEDS_TYPED_RELATIONS"}
 files={"skill-bridge-audit-v3-2c.json":audit,"skill-relations-v3-2c.json":rels,"skill-provenance-v3-2c.json":[{k:r[k] for k in ("canonicalExerciseId","skillId","relationType","source","evidence","confidence","provenance")} for r in rels],"skill-final-unresolved-v3-2c.json":unresolved,"skill-validation-v3-2c.json":validation}
 for name,obj in files.items(): (OUT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+"\n")
 (OUT/"SKILL_RELATION_SCHEMA_PROPOSAL_V3_2C.md").write_text("# Exercise DB V3.2C — Skill Relation Schema Proposal\n\n**Status: INSUFFICIENT_NEEDS_TYPED_RELATIONS.** Keep SkillCatalog authoritative for prerequisites/mastery. Keep Exercise authoritative for exercise identity/metadata. Add a separate typed Exercise↔Skill relation layer; do not overload legacy skillIds with accessory, strength, mobility, transfer or technique semantics.\n\nMinimum relation fields: canonicalExerciseId, skillId, relationType, confidence, provenance. skillIds remains a compatibility bridge only for exact SkillCatalog node identity until integration explicitly migrates it.\n")
 md="# Exercise DB V3.2C — Skill Bridge Audit\n\nSource HEAD: ca11aca5cba07c95a8778bad321127b818264107\n\n## Runtime semantics\nmovementType=SKILL is broad. skillIds stores active SkillCatalog node IDs resolved from exact exercise names/aliases. Runtime mastery/locking are separately owned by SkillCatalog/ProgressionEngine.\n\n## Verified catalog\n**%d skill families, %d nodes.** The branch exposes %d effective families, not 16.\n\n## Model decision\n**INSUFFICIENT_NEEDS_TYPED_RELATIONS.** A node-ID list cannot distinguish identity, progression/regression, prerequisite, accessory, strength, mobility, technique or transfer.\n\n## Existing links\nExercises with links: **%d**. Total links: **%d**. Valid references: **%d**. Suspicious: **%d**. Invalid: **%d**.\n\n## Missing/unstable workset\nVerified movementType=SKILL without skillIds: **%d**. Every record is audited. Without Tier-1 exact identity these remain **UNRESOLVED_SCHEMA_DECISION_REQUIRED** rather than receiving invented membership links.\n\n## Ownership\nSkillCatalog owns family/prerequisites/mastery/progression order. Exercise owns exercise identity/metadata. Typed Exercise↔Skill relations own relationship type/confidence/provenance.\n\n## Gate\nPending executed validation. No runtime DB, selection engine, Runner, equipment, resistanceMode or level/capability data is modified.\n"%(len(by),len(nodes),len(by),len(linked),sum(len(x.get("skillIds",[])) for x in linked),len(rels),len(suspicious),len(invalid),len(missing))
 (OUT/"SKILL_BRIDGE_AUDIT_V3_2C.md").write_text(md)
 print(json.dumps({"canonical":len(db),"unique":len(set(ids)),"skills":len(by),"nodes":len(nodes),"linkedExercises":len(linked),"links":sum(len(x.get("skillIds",[])) for x in linked),"missingWorkset":len(missing),"suspicious":len(suspicious),"invalid":len(invalid)},indent=2))
if __name__=="__main__": main()
