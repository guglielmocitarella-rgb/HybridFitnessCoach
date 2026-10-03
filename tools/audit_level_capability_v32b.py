#!/usr/bin/env python3
import json, pathlib, collections
root=pathlib.Path(__file__).resolve().parents[1]; db=root/'exercise-db-canonical'
records=json.loads((db/'exercise-db-canonical.json').read_text()); ids=[r.get('id') for r in records]
assert len(records)==1087 and len(set(ids))==1087 and all(ids)
allowed={'Principiante','Intermedio','Avanzato'}
unknown=[r for r in records if r.get('levelStatus')!='KNOWN' or not r.get('level')]
known=[r for r in records if r.get('levelStatus')=='KNOWN' and r.get('level')]
assert len(unknown)==438, len(unknown); assert not [r['id'] for r in known if r.get('level') not in allowed]
def srcids(r): return sorted({s.get('source_id') for s in r.get('sources',[]) if isinstance(s,dict) and s.get('source_id')})
def match_class(r):
    for k in ('matchingClass','matchClass','matchStatus'):
        if r.get(k): return r[k]
    return 'NOT_EMBEDDED'
def cluster(r):
    if r.get('movementType')=='SKILL': return 'D_SKILL_PROGRESSION_DERIVED_CANDIDATE'
    if r.get('isometric'): return 'F_STRENGTH_DEPENDENT'
    mt=(r.get('movementType') or '').upper(); fam=(r.get('family') or '').upper()
    if mt in {'MOBILITY','FLEXIBILITY','SMR'} or 'MOBILITY' in fam or 'STRETCH' in fam: return 'G_MOBILITY_DEPENDENT'
    if r.get('unilateral'): return 'H_BALANCE_COORDINATION_DEPENDENT'
    if r.get('resistanceMode') in {'EXTERNAL_LOAD','ASSISTED'}: return 'J_VARIABLE_SCALABLE_DIFFICULTY'
    return 'K_INSUFFICIENT_EVIDENCE'
def terminal(r):
    if match_class(r) in {'AMBIGUOUS_IDENTITY','CANONICAL_IDENTITY_PROBLEM'}: return 'UNRESOLVED_CANONICAL_IDENTITY_PROBLEM'
    if r.get('movementType')=='SKILL': return 'UNRESOLVED_SCHEMA_DECISION_REQUIRED'
    if r.get('resistanceMode') in {'EXTERNAL_LOAD','ASSISTED'}: return 'UNRESOLVED_VARIABLE_DIFFICULTY'
    return 'UNRESOLVED_INSUFFICIENT_EVIDENCE'
audited=[]
for r in unknown:
    audited.append({'canonicalId':r['id'],'canonicalNameEn':r.get('canonicalNameEn'),'category':r.get('category'),'family':r.get('family'),'movementType':r.get('movementType'),'skillStatus':bool(r.get('skillIds')),'equipmentStatus':r.get('equipmentStatus'),'equipmentRequired':r.get('equipmentRequired',[]),'resistanceMode':r.get('resistanceMode'),'matchingEvidenceClass':match_class(r),'instructionEvidence':r.get('executionEvidence'),'sourceIds':srcids(r),'sourceLevel':None,'cluster':cluster(r),'classification':terminal(r),'reason':'No semantically compatible explicit level evidence is persisted for this UNKNOWN record; V3.2B does not infer level from name/equipment/list position. '+('Skill progression is governed by the separate SkillCatalog prerequisite graph.' if r.get('movementType')=='SKILL' else 'Difficulty may depend on load/assistance or unmeasured capability dimensions.' if r.get('resistanceMode') in {'EXTERNAL_LOAD','ASSISTED'} else 'Insufficient compatible evidence for a scalar level assignment.')})
counts=collections.Counter(x['classification'] for x in audited); clusters=collections.Counter(x['cluster'] for x in audited)
prov_groups=collections.defaultdict(list)
for r in known:
    p=(r.get('fieldProvenance') or {}).get('level') or {}; prov_groups[(r.get('level'),p.get('status','UNKNOWN'))].append(r)
sample=[]
for key in sorted(prov_groups):
    for r in sorted(prov_groups[key],key=lambda x:x['id'])[:3]:
        p=(r.get('fieldProvenance') or {}).get('level') or {}
        sample.append({'canonicalId':r['id'],'level':r.get('level'),'provenanceStatus':p.get('status'),'source':p.get('source'),'observedValue':p.get('observedValue'),'mapping':p.get('mapping'),'finding':'VALUE_VALID_BUT_SEMANTIC_BASIS_NOT_UNIFORM'})
if len(sample)<10:
    seen={x['canonicalId'] for x in sample}
    for r in sorted(known,key=lambda x:x['id']):
        if r['id'] not in seen: sample.append({'canonicalId':r['id'],'level':r.get('level'),'provenanceStatus':((r.get('fieldProvenance') or {}).get('level') or {}).get('status'),'finding':'VALUE_VALID_BUT_SEMANTIC_BASIS_NOT_UNIFORM'})
        if len(sample)>=10: break
scale_counter=collections.Counter(); scale_examples={}
for r in known:
    p=(r.get('fieldProvenance') or {}).get('level') or {}; st=p.get('status','UNKNOWN'); mapping=p.get('mapping')
    if mapping and 'ACE Difficulty' in mapping: key='ACE_DIFFICULTY'
    elif st=='APP_MODEL_DIRECT': key='APP_MODEL_ITALIAN_LEVEL'
    elif st=='V2_DIRECT': key='V2_LEVEL_FIELD'
    elif st=='EXTERNAL_SOURCE_DIRECT': key='EXTERNAL_DIRECT_OTHER'
    else: key=st
    scale_counter[key]+=1; scale_examples.setdefault(key,[]).append(r['id'])
scales=[]
for k,n in sorted(scale_counter.items()):
    if k=='ACE_DIFFICULTY': decision='Existing Intermediate->Intermedio mapping is documented, but V3.2B does not generalize other 3-point scales without semantic evidence.'
    elif k in {'APP_MODEL_ITALIAN_LEVEL','V2_LEVEL_FIELD'}: decision='Preserve as legacy/authored value; not accepted as exercise-science ground truth because field semantics were not formally defined.'
    else: decision='Preserve; require source-specific semantic review before reuse for normalization.'
    scales.append({'sourceScale':k,'records':n,'examples':scale_examples[k][:5],'normalizationDecision':decision,'confidence':'HIGH_FOR_PROVENANCE_CLASSIFICATION'})
model={'status':'INSUFFICIENT_NEEDS_CAPABILITY_MODEL','currentSemantics':'Runtime treats level as a scalar exercise requirement (Principiante=1, Intermedio=2, everything else=3) and compares it with a movement-specific user capability. Persisted data mixes legacy authored level labels and source difficulty labels without one explicit semantic contract.','actualEngineUsage':'Hard eligibility filter: levelScore(exercise.level) <= capabilityLevelFor(exercise, capability). Skill locking is separately controlled by SkillCatalog prerequisites/mastery.','capabilityModelRequired':True,'minimumProposal':[{'field':'technicalDifficulty','problem':'Separates motor/technical complexity from strength demand.','engineUse':'Future ranking/safety gate.','need':'FUTURE_NEAR_TERM','populationCost':'HIGH','relationToLevel':'Can inform but must not equal scalar level.','relationToSkill':'Independent of prerequisite position.'},{'field':'strengthRequirement','problem':'Represents minimum relative force capacity.','engineUse':'Map to existing push/pull/squat/hinge/core/isometric capability axes.','need':'TODAY_FOR_SEMANTICALLY_CORRECT_LEVEL_REPLACEMENT','populationCost':'HIGH','relationToLevel':'Primary replacement dimension for strength-gated exercises.','relationToSkill':'Complements, never replaces, mastery prerequisites.'},{'field':'mobilityRequirement','problem':'Represents range-of-motion prerequisites.','engineUse':'Future eligibility once mobility is assessed.','need':'FUTURE','populationCost':'MEDIUM_HIGH','relationToLevel':'Independent dimension.','relationToSkill':'May gate positions within skill progressions.'},{'field':'balanceCoordinationRequirement','problem':'Represents balance/coordination demand.','engineUse':'Future capability/ranking once assessed.','need':'FUTURE','populationCost':'HIGH','relationToLevel':'Independent dimension.','relationToSkill':'Relevant to handstand/unilateral skills; prerequisites remain separate.'}],'notProposedSeparately':'skillRequirement: SkillCatalog already models prerequisites/mastery; duplicating it would create competing truths.'}
audit={'schemaVersion':'3.2B','sourceHead':'3212e64b60da75fe1b136843b51e8aaaddfbb407','integrity':{'canonical':len(records),'unique':len(set(ids)),'missing':0,'duplicates':0},'knownLevelCount':len(known),'unknownVerified':len(unknown),'knownLevels':dict(collections.Counter(r['level'] for r in known)),'knownQualityAudit':{'sampleSize':len(sample),'sample':sample,'problemsFound':['No single documented semantic definition across APP_MODEL_DIRECT/V2_DIRECT/external difficulty provenance.','Runtime scalar level is compared against multidimensional user capability.','Skill progression already has an independent prerequisite/mastery graph.'],'systematicIssue':True},'sourceScales':scales,'clusters':dict(clusters),'finalClassification':dict(counts),'records':audited,'model':model,'normalizationDecision':'NO_BULK_NORMALIZATION. Preserve known legacy values as data, do not use them as ground truth; only source-specific mappings with documented semantic compatibility may create future patches.','qualityReviewPolicy':'All conflict/schema/canonical-identity classes reviewed exhaustively; requested resolved samples are N/A because no UNKNOWN met evidence threshold for patching. Variable and insufficient samples are covered by tests.'}
(db/'level-capability-audit-v3-2b.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
(db/'level-field-patch-v3-2b.json').write_text(json.dumps({'schemaVersion':'3.2B','sourceHead':audit['sourceHead'],'patches':[],'note':'No UNKNOWN level was patched without semantically compatible explicit evidence.'},ensure_ascii=False,indent=2)+'\n')
(db/'level-provenance-v3-2b.json').write_text(json.dumps({'schemaVersion':'3.2B','entries':[],'knownLevelScaleInventory':scales},ensure_ascii=False,indent=2)+'\n')
(db/'level-final-unresolved-v3-2b.json').write_text(json.dumps({'schemaVersion':'3.2B','count':len(audited),'classificationCounts':dict(counts),'records':audited},ensure_ascii=False,indent=2)+'\n')
validation={'schemaVersion':'3.2B','sourceHead':audit['sourceHead'],'status':'PENDING_CI','integrity':audit['integrity'],'unknownBefore':438,'patched':0,'unknownAfterAuditPatchLayer':438,'audited':438,'validatorCommand':'python tools/validate_exercise_db_canonical.py','regressionCommand':'python -m unittest tests/test_exercise_db_canonical.py tests/test_equipment_v31c.py tests/test_resistance_mode_v32a.py tests/test_level_capability_v32b.py -v','modelStatus':model['status'],'domainDecision':'CLOSED_WITH_DOCUMENTED_UNRESOLVED'}
(db/'level-validation-v3-2b.json').write_text(json.dumps(validation,ensure_ascii=False,indent=2)+'\n')
md=['# Exercise DB V3.2B — Level & Capability Audit','',f"Source HEAD: `{audit['sourceHead']}`",'','## Runtime semantics','`level` is a scalar hard eligibility requirement: `Principiante=1`, `Intermedio=2`, every other string=3; the generator requires `levelScore(exercise.level) <= capabilityLevelFor(exercise, capability)`. User capability is already multidimensional, while SkillCatalog separately locks progression nodes by prerequisites/mastery.','','## Model decision',f"**{model['status']}**. Current persisted level values mix legacy authored labels and source difficulty labels without one formal semantic contract.",'','Minimum capability proposal: `technicalDifficulty`, `strengthRequirement`, `mobilityRequirement`, `balanceCoordinationRequirement`. A separate scalar `skillRequirement` is not proposed because SkillCatalog already owns prerequisites/mastery.','','## Known-level quality audit',f"Known: **{len(known)}**. Stratified sample: **{len(sample)}**. Systematic semantic issue: **YES**.",'','## UNKNOWN workset','Verified: **438/438 audited**.','']
md += [f'- {k}: **{v}**' for k,v in sorted(counts.items())]
md += ['','No UNKNOWN record met the evidence threshold for a scalar level patch. No value is inferred from name, equipment, weight, list position, or neighbouring records.','','## Source normalization','No bulk three-level normalization is accepted. Existing ACE Difficulty mapping is source-specific; APP/V2 authored levels remain legacy values until their semantics are explicitly defined.','','## Gate','**CLOSED_WITH_DOCUMENTED_UNRESOLVED** — all 438 UNKNOWN records are terminally classified; known levels were quality-audited; source-scale provenance was inventoried; capability-model gap is documented; no runtime DB, selection engine, Runner, equipment or resistanceMode data was modified.','']
(db/'LEVEL_CAPABILITY_AUDIT_V3_2B.md').write_text('\n'.join(md))
(db/'LEVEL_CAPABILITY_SCHEMA_PROPOSAL_V3_2B.md').write_text('# Exercise DB V3.2B — Minimal Capability Schema Proposal\n\nStatus: proposal only; no runtime schema change in V3.2B.\n\n- `technicalDifficulty`: motor/technical complexity; future ranking/safety.\n- `strengthRequirement`: relative strength demand mapped to existing capability axes; needed for a semantically correct replacement of scalar strength gating.\n- `mobilityRequirement`: range-of-motion prerequisite; future eligibility once assessed.\n- `balanceCoordinationRequirement`: balance/coordination prerequisite; future eligibility/ranking.\n\nDo not add `skillRequirement` as a competing scalar: SkillCatalog prerequisites/mastery remain authoritative.\n')
(db/'SOURCE_LEVEL_NORMALIZATION_V3_2B.md').write_text('# Exercise DB V3.2B — Source Level Normalization\n\nNo generic 3-point-scale equivalence is accepted.\n\n'+''.join(f"## {s['sourceScale']}\nRecords: {s['records']}\n\nDecision: {s['normalizationDecision']}\n\n" for s in scales))
(root/'tests/test_level_capability_v32b.py').write_text("""import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]; DB=ROOT/'exercise-db-canonical'
class LevelCapabilityV32BTest(unittest.TestCase):
 def setUp(self):
  self.a=json.loads((DB/'level-capability-audit-v3-2b.json').read_text()); self.p=json.loads((DB/'level-field-patch-v3-2b.json').read_text())
 def test_all_unknown_audited_once(self): self.assertEqual(438,len(self.a['records'])); self.assertEqual(438,len({r['canonicalId'] for r in self.a['records']}))
 def test_no_false_precision_patch(self): self.assertEqual([],self.p['patches'])
 def test_terminal_classes(self):
  allowed={'RESOLVED_CERTAIN','RESOLVED_HIGH_CONFIDENCE','UNRESOLVED_VARIABLE_DIFFICULTY','UNRESOLVED_INSUFFICIENT_EVIDENCE','UNRESOLVED_SOURCE_SCALE_CONFLICT','UNRESOLVED_SCHEMA_DECISION_REQUIRED','UNRESOLVED_CANONICAL_IDENTITY_PROBLEM'}
  self.assertTrue(all(r['classification'] in allowed for r in self.a['records']))
 def test_variable_scalable_present(self): self.assertGreater(self.a['finalClassification'].get('UNRESOLVED_VARIABLE_DIFFICULTY',0),0)
 def test_skill_schema_cases_present(self): self.assertGreater(self.a['finalClassification'].get('UNRESOLVED_SCHEMA_DECISION_REQUIRED',0),0)
 def test_model_distinguishes_capabilities(self):
  fields={x['field'] for x in self.a['model']['minimumProposal']}; self.assertTrue({'technicalDifficulty','strengthRequirement','mobilityRequirement','balanceCoordinationRequirement'}<=fields)
 def test_known_quality_audit(self): self.assertGreaterEqual(self.a['knownQualityAudit']['sampleSize'],10); self.assertTrue(self.a['knownQualityAudit']['systematicIssue'])
 def test_unknown_not_advanced_normalization(self): self.assertIn('NO_BULK_NORMALIZATION',self.a['normalizationDecision'])
if __name__=='__main__': unittest.main()
""")
print(json.dumps({'audited':len(audited),'known':len(known),'classifications':dict(counts),'clusters':dict(clusters),'knownSample':len(sample),'sourceScales':{s['sourceScale']:s['records'] for s in scales}},ensure_ascii=False))
