import collections,gzip,json,pathlib
ROOT=pathlib.Path(__file__).resolve().parent; BASE=ROOT.parent; CP=ROOT/'checkpoints'
FIELDS=['equipment','resistanceMode','movementPattern','level','skillLink','primaryMuscles','secondaryMuscles','bodyRegion','description','setup','execution','techniqueCues','commonErrors']; INSTR=['description','setup','execution','techniqueCues','commonErrors']
LICENSES={'yuhonas/free-exercise-db':('Unlicense','SAFE_TO_USE','Public-domain dedication in pinned LICENSE.md.'),'kinetic-place/exercises-json':('MIT','ATTRIBUTION_REQUIRED','MIT notice required.'),'ArelliGoutham/exercise-database':('MIT','ATTRIBUTION_REQUIRED','MIT notice required.'),'RepDB/exercise-dataset':('RepDB Free Tier v1.0','ATTRIBUTION_REQUIRED','In-app use with attribution; dataset redistribution prohibited; paid preview animations evaluation-only.'),'rthepen/workout-database':('MIT','ATTRIBUTION_REQUIRED','MIT notice required.'),'harshvishu/free-exercise-db-with-videos':('MIT','LICENSE_UNCLEAR','Repository is MIT; external video URLs need separate media-rights review.')}
def load(p):
 o=gzip.open if p.suffix=='.gz' else open
 with o(p,'rt') as f:return json.load(f)
def present(v):return v not in (None,'',[],{})
def miss(c):
 m=set(); specific=c.get('executionEvidence') in ['SPECIFIC_SOURCE','EXTERNAL_TECHNICAL_REFERENCE']
 checks={'equipment':c.get('equipmentStatus')=='KNOWN','level':c.get('levelStatus')=='KNOWN','resistanceMode':c.get('resistanceMode') not in [None,'UNKNOWN'],'movementPattern':bool(c.get('movementPattern')),'skillLink':bool(c.get('skillIds')) or c.get('movementType')!='SKILL','primaryMuscles':bool(c.get('primaryMuscles')),'secondaryMuscles':bool(c.get('secondaryMuscles')),'bodyRegion':bool(c.get('bodyRegion'))}
 for k,v in checks.items():
  if not v:m.add(k)
 for k,f in [('description','descriptionIt'),('setup','setupIt'),('execution','executionStepsIt'),('techniqueCues','techniqueCuesIt'),('commonErrors','commonErrorsIt')]:
  if not specific or not present(c.get(f)):m.add(k)
 return m
def main():
 fs=sorted(CP.glob('*.json.gz'))+sorted(CP.glob('*.json')); cps=[load(p) for p in fs]; ids=[x for d in cps for x in d['ids']]
 canon=json.loads((BASE/'work'/'canonical.json').read_text()); byid={c['id']:c for c in canon}; assert len(ids)==len(set(ids))==1087 and set(ids)==set(byid)
 rows=[r for d in cps for r in d['rows']]; manifest=json.loads((BASE/'work'/'manifest.json').read_text()); srcs=[s['repository'] for s in manifest['sources']]
 bc=collections.defaultdict(list)
 for r in rows:bc[r['canonical_id']].append(r)
 recs=[]; cls=collections.Counter(); review=[]
 for cid in sorted(byid):
  rr=bc[cid]; p=[r for r in rr if r['match_type']=='MATCH ALTAMENTE PROBABILE']; a=[r for r in rr if r['match_type']=='MATCH AMBIGUO']; cl='HIGH_PROBABILITY' if p else ('AMBIGUOUS' if a else 'NO_MATCH'); cls[cl]+=1
  sel=sorted(p or a,key=lambda r:(-r.get('score',0),r.get('source_dataset',''),str(r.get('source_record_id'))))[:10]
  recs.append({'canonical_id':cid,'canonical_name':byid[cid].get('canonicalNameEn'),'classification':cl,'canonical_missing':sorted(miss(byid[cid])),'source_candidates':sel,'candidate_count':len(p)+len(a)})
  if cl!='HIGH_PROBABILITY' or any(r.get('conflicts') for r in sel):review.append({'canonical_id':cid,'canonical_name':byid[cid].get('canonicalNameEn'),'classification':cl,'reasons':sorted(set(x for r in sel for x in r.get('conflicts',[]))) or ['no_high_probability_match']})
 ds={}
 for si,s in enumerate(srcs):
  g=collections.defaultdict(list)
  for r in rows:
   if r['source_dataset']==s:g[r['canonical_id']].append(r)
  co=collections.Counter(); candn=0; ins=set(); med=set(); conf=collections.Counter(); fav=collections.Counter()
  for cid in byid:
   rr=g.get(cid,[]); p=[r for r in rr if r['match_type']=='MATCH ALTAMENTE PROBABILE']; a=[r for r in rr if r['match_type']=='MATCH AMBIGUO']; co['HIGH_PROBABILITY' if p else 'AMBIGUOUS' if a else 'NO_MATCH']+=1; cand=p+a
   if cand:candn+=1
   for r in cand:
    fav.update(r.get('available_fields',{}).keys()); conf.update(r.get('conflicts',[]))
    if any(f in r.get('available_fields',{}) for f in INSTR):ins.add(cid)
    if r.get('assets'):med.add(cid)
  lic=LICENSES[s]; ds[s]={'source_records':cps[0]['source_record_counts'][si],'canonical_with_candidate':candn,'CERTAIN':0,'HIGH_PROBABILITY':co['HIGH_PROBABILITY'],'AMBIGUOUS':co['AMBIGUOUS'],'NO_MATCH':co['NO_MATCH'],'coverage_pct':round(candn*100/1087,2),'instruction_coverage_canonical':len(ins),'media_coverage_canonical':len(med),'available_field_candidate_rows':dict(fav),'conflicts':dict(conf),'license':{'license':lic[0],'status':lic[1],'note':lic[2]}}
 fa={}
 for f in FIELDS:
  cur=hi=am=0
  for cid,c in byid.items():
   if f not in miss(c):cur+=1;continue
   rr=bc[cid]
   if any(r['match_type']=='MATCH ALTAMENTE PROBABILE' and f in r.get('available_fields',{}) for r in rr):hi+=1
   elif any(r['match_type']=='MATCH AMBIGUO' and f in r.get('available_fields',{}) for r in rr):am+=1
  fa[f]={'CURRENT':cur,'MATCHED_SOURCE_AVAILABLE':hi+am,'CERTAINLY_RESOLVABLE':0,'HIGH_PROBABILITY_RESOLVABLE':hi,'AMBIGUOUS':am,'STILL_MISSING':1087-cur-hi-am,'current_pct':round(cur*100/1087,2),'potential_high_pct':round((cur+hi)*100/1087,2)}
 val=json.loads((BASE/'VALIDATION_REPORT.json').read_text()); aud=json.loads((BASE/'exercise-db-canonical-audit.json').read_text()); unresolved=aud.get('unresolved_ids',{}); fmap={'equipment_unknown':'equipment','equipment_unsupported_by_app':'equipment','movement_pattern_missing':'movementPattern','instruction_incomplete':'execution','level_unknown':'level','resistance_mode_unknown':'resistanceMode','skill_links_missing':'skillLink'}; blockers={}
 for k,n in val['blockers'].items():
  f=fmap[k]; S=set(unresolved.get(k,[])); hi=am=0
  for cid in S:
   rr=bc.get(cid,[])
   if any(r['match_type']=='MATCH ALTAMENTE PROBABILE' and f in r.get('available_fields',{}) for r in rr):hi+=1
   elif any(r['match_type']=='MATCH AMBIGUO' and f in r.get('available_fields',{}) for r in rr):am+=1
  if not S:hi=min(n,fa[f]['HIGH_PROBABILITY_RESOLVABLE']);am=min(n-hi,fa[f]['AMBIGUOUS'])
  blockers[k]={'baseline':n,'CERTAIN':0,'HIGH_PROBABILITY':hi,'AMBIGUOUS':am,'residual_after_high_probability':n-hi,'potential_improvement_pct':round(hi*100/n,2) if n else 0}
 media={}
 for s in srcs:
  kinds=collections.Counter(); refs=0; cids=set(); lic=LICENSES[s]
  for r in rows:
   if r['source_dataset']!=s or r['match_type']=='NESSUN MATCH':continue
   for a in r.get('assets',[]):refs+=1;kinds[a.get('kind','unknown')]+=1;cids.add(r['canonical_id'])
  media[s]={'references':refs,'canonical_exercises':len(cids),'types':dict(kinds),'license_status':lic[1],'license':lic[0],'note':lic[2]}
 conflicts=collections.Counter(x for r in rows for x in r.get('conflicts',[])); matching={'CERTAIN':0,'HIGH_PROBABILITY':cls['HIGH_PROBABILITY'],'AMBIGUOUS':cls['AMBIGUOUS'],'NO_MATCH':cls['NO_MATCH']}
 summary={'integrity':{'canonical':1087,'processed':1087,'unique':1087,'missing':0,'duplicates':0,'checkpoint_count':len(fs)},'matching':matching,'matching_pct':{k:round(v*100/1087,2) for k,v in matching.items()},'datasets':ds,'fields':fa,'historical_blockers':blockers,'instructions':{f:fa[f] for f in INSTR},'media':media,'conflicts':dict(conflicts),'review_queue_count':len(review),'classification_note':'CERTAIN intentionally remains zero: matcher v1.0 calls exact-name+equipment evidence probable, not certain.'}
 (ROOT/'aggregated-matches.json.gz').write_bytes(gzip.compress((json.dumps({'schema_version':1,'records':recs},ensure_ascii=False,separators=(',',':'))+'\n').encode(),mtime=0)); (ROOT/'audit-summary.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n'); (ROOT/'manual-review-queue.json').write_text(json.dumps(review,indent=2,ensure_ascii=False)+'\n'); (ROOT/'media-provenance-report.json').write_text(json.dumps(media,indent=2,ensure_ascii=False)+'\n')
 L=['# Exercise database matching audit — final','',f"Integrity: **1087/1087**, unique **1087**, missing **0**, duplicates **0**.",'','## Match classification','']+[f"- {k}: {matching[k]} ({summary['matching_pct'][k]}%)" for k in matching]+['','CERTAIN remains zero deliberately: current evidence does not establish certainty.','','## Per dataset','','| Dataset | Records | Candidate coverage | High probability | Ambiguous | No match | Instruction | Media | License |','|---|---:|---:|---:|---:|---:|---:|---:|---|']
 for s,d in ds.items():L.append(f"| {s} | {d['source_records']} | {d['canonical_with_candidate']} ({d['coverage_pct']}%) | {d['HIGH_PROBABILITY']} | {d['AMBIGUOUS']} | {d['NO_MATCH']} | {d['instruction_coverage_canonical']} | {d['media_coverage_canonical']} | {d['license']['status']} |")
 L+=['','## Field audit','','| Field | Current | Source available | High-probability | Ambiguous | Still missing |','|---|---:|---:|---:|---:|---:|']
 for f,d in fa.items():L.append(f"| {f} | {d['CURRENT']} | {d['MATCHED_SOURCE_AVAILABLE']} | {d['HIGH_PROBABILITY_RESOLVABLE']} | {d['AMBIGUOUS']} | {d['STILL_MISSING']} |")
 L+=['','## Historical blockers','','| Category | Baseline | High-probability | Ambiguous | Residual | Improvement |','|---|---:|---:|---:|---:|---:|']
 for k,d in blockers.items():L.append(f"| {k} | {d['baseline']} | {d['HIGH_PROBABILITY']} | {d['AMBIGUOUS']} | {d['residual_after_high_probability']} | {d['potential_improvement_pct']}% |")
 L+=['','## Media and licensing','']
 for s,d in media.items():L.append(f"- **{s}**: {d['references']} references / {d['canonical_exercises']} canonical exercises; {d['types']}; **{d['license_status']}** — {d['note']}")
 L+=['','## Conflicts','',json.dumps(dict(conflicts),ensure_ascii=False),'',f"## Manual review queue\n\n{len(review)} canonical exercises require review.",'','## Next phase','','Use HIGH_PROBABILITY only as a field-by-field enrichment candidate queue; do not auto-import AMBIGUOUS. Review media rights separately. Keep the canonical database unchanged until reviewed enrichment goes through the existing builder/provenance path.']
 (ROOT/'AUDIT_REPORT.md').write_text('\n'.join(L)+'\n'); print(json.dumps(summary['integrity']|matching))
if __name__=='__main__':main()
