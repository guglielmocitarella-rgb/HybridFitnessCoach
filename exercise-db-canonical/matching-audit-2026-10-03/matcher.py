"""Read-only candidate audit. No external prose copied into checkpoints.
Scores are reproducible retrieval heuristics, never calibrated probabilities.
"""
import argparse, collections, difflib, hashlib, json, pathlib, re, unicodedata

VERSION='1.0'
ROOT=pathlib.Path(__file__).resolve().parent
WORK=ROOT.parent/'work'
SINGULAR={'pushups':'pushup','push-ups':'pushup','pullups':'pullup','chinups':'chinup','squats':'squat','lunges':'lunge','curls':'curl','raises':'raise','rows':'row','dips':'dip','extensions':'extension','presses':'press','crunches':'crunch','situps':'situp','planks':'plank','rollouts':'rollout','swings':'swing','deadlifts':'deadlift','flyes':'fly','flies':'fly','push-ups':'pushup'}
def norm(v):
    s=unicodedata.normalize('NFKD',str(v or '')).encode('ascii','ignore').decode().lower()
    for a,b in [('push-up','pushup'),('pull-up','pullup'),('chin-up','chinup'),('sit-up','situp'),('push up','pushup'),('pull up','pullup'),('chin up','chinup'),('sit up','situp')]: s=s.replace(a,b)
    return ' '.join(SINGULAR.get(t,t) for t in re.findall(r'[a-z0-9]+',s))

EQ={'body only':'bodyweight','body weight':'bodyweight','corpo libero':'bodyweight','dumbbells':'dumbbell','barbells':'barbell','bands':'resistance band','band':'resistance band','cables':'cable','exercise ball':'stability ball','swiss ball':'stability ball','medicine balls':'medicine ball','pullup bar':'pull up bar','leverage machine':'machine','trx suspension':'suspension trainer','trx':'suspension trainer'}
def eq(v):
    s=norm(v).replace('_',' ')
    return EQ.get(s,s)
def equipment(v):
    if v is None:return []
    if not isinstance(v,list):v=[v]
    return sorted(set(eq(x.get('name','') if isinstance(x,dict) else x) for x in v if x))
VARIANTS=set('incline decline seated standing kneeling supine prone weighted assisted single unilateral alternating overhead behind reverse wide narrow close neutral deficit sumo romanian stiff paused explosive eccentric isometric wall ring rings archer pike tuck straddle full half quarter 45 90'.split())

def assess(c,s):
    cn=[norm(c.get('canonicalNameEn'))]+[norm(x) for x in c.get('aliases',[]) if isinstance(x,str)]
    sn=[norm(s['name'])]+[norm(x) for x in s.get('aliases',[]) if isinstance(x,str)]
    exact=bool(set(cn)&set(sn)-{''})
    pairs=[(a,b) for a in cn for b in sn if a and b]
    best=(0,'','')
    for a,b in pairs:
        ta,tb=set(a.split()),set(b.split()); token=2*len(ta&tb)/(len(ta)+len(tb))
        score=1 if a==b else (0.65*token+0.35*difflib.SequenceMatcher(None,a,b).ratio() if token>=0.4 else token*0.65)
        if score>best[0]:best=(score,a,b)
    score,a,b=best
    ce=equipment(c.get('equipmentRequired')) if c.get('equipmentStatus')=='KNOWN' else []
    if c.get('equipmentStatus')=='KNOWN' and not ce:ce=['bodyweight']
    se=equipment(s.get('equipment'))
    shared=set(ce)&set(se)
    conflict=[]
    if ce and se and not shared:conflict.append('equipment_disjoint')
    if not exact and (set(a.split())^set(b.split()))&VARIANTS:conflict.append('variant_qualifier_difference')
    if exact and shared and not conflict:kind='MATCH ALTAMENTE PROBABILE'
    elif score>=.66:kind='MATCH AMBIGUO'
    else:kind='NESSUN MATCH'
    evidence=['normalized_name_or_alias_exact'] if exact else ['lexical_candidate_only']
    if shared:evidence.append('equipment_overlap:'+','.join(sorted(shared)))
    if not ce:evidence.append('canonical_equipment_unknown')
    return {'match_type':kind,'score':round(score,5),'method':'exact_or_normalized_alias' if exact else 'token_overlap_plus_sequence_similarity','evidence':evidence,'conflicts':conflict,'matched_names':[a,b],'canonical_equipment':ce,'source_equipment':se}

def present(x):return x is not None and x!='' and x!=[] and x!={}
def normalize_source(raw,i):
    data=raw['exercises'] if isinstance(raw,dict) else raw
    out=[]
    for pos,r in enumerate(data):
        s={'id':str(r.get('id',pos)),'name':r.get('name') or r.get('name_en') or r.get('exercise_name',{}).get('en'),'aliases':r.get('aliases',[]),'equipment':equipment(r.get('equipment') if i!=4 else r.get('material',{}).get('id')),'fields':{},'assets':[],'source_ids':r.get('sourceIds',{}),'raw_keys':sorted(r)}
        def add(target,path,value):
            if present(value):s['fields'][target]={'path':path,'value':value}
        add('equipment','material.id' if i==4 else 'equipment',s['equipment'])
        for target,options in {'level':['level','difficultyLevel','difficulty'],'description':['description','description_en','shortDescription'],'primaryMuscles':['primaryMuscles','primary_muscles'],'secondaryMuscles':['secondaryMuscles','secondary_muscles'],'bodyRegion':['body_part','bodyPart'],'techniqueCues':['tips','tips_en','formCues'],'commonErrors':['commonMistakes'],'execution':['instructions_en','steps']}.items():
            for key in options:
                if present(r.get(key)):add(target,key,r[key]);break
        if isinstance(r.get('instructions'),list):add('execution','instructions',r['instructions'])
        if i==1:
            for target,typ in [('primaryMuscles','primary'),('secondaryMuscles','secondary')]:add(target,'muscleGroups[type='+typ+']',[x['slug'] for x in r.get('muscleGroups',[]) if x['type']==typ])
        if i==4:
            add('level','attributes.difficulty',r.get('attributes',{}).get('difficulty'))
            add('primaryMuscles','target_muscles.primary',r.get('target_muscles',{}).get('primary'))
            add('secondaryMuscles','target_muscles.secondary',r.get('target_muscles',{}).get('secondary'))
            add('execution','instructions.en',r.get('instructions',{}).get('en'))
            add('techniqueCues','form_cues.en',r.get('form_cues',{}).get('en'))
        for target in ['resistanceMode','movementPattern','skillLink','setup']:
            add(target,target,r.get(target))
        # Boolean bodyweight and force push/pull are retained, not promoted to app enums.
        s['auxiliary']={k:r[k] for k in ['force','forceType','force_type','mechanic','mechanics','isBodyweight','is_bodyweight','unilateral','is_unilateral','category'] if k in r}
        def asset(kind,path,locator):
            if locator:s['assets'].append({'kind':kind,'field':path,'locator':locator})
        if i==0:
            for x in r.get('images',[]):asset('photo','images','exercises/'+x)
        if i==2:
            for x in r.get('imageUrls',[]):asset('image','imageUrls',x)
        if i==3:
            for style,values in r.get('images',{}).items():
                for pose,x in values.items():asset('illustration','images.'+style+'.'+pose,x)
        if i==4:
            for x in r.get('media',{}).get('videos',[]):asset('video','media.videos.youtube_id','https://www.youtube.com/watch?v='+x['youtube_id'])
        if i==5:
            for k,v in r.get('videos',{}).items():asset('video','videos.'+k,v)
            for k,v in r.get('thumbnails',{}).items():asset('thumbnail','thumbnails.'+k,v)
        out.append(s)
    return out

def canonical_missing(c):
    specific=c.get('executionEvidence') in ['SPECIFIC_SOURCE','EXTERNAL_TECHNICAL_REFERENCE']
    missing=[]
    pairs={'equipment':c.get('equipmentStatus')=='KNOWN','level':c.get('levelStatus')=='KNOWN','resistanceMode':c.get('resistanceMode') not in [None,'UNKNOWN'],'movementPattern':bool(c.get('movementPattern')),'skillLink':bool(c.get('skillIds')) or c.get('movementType')!='SKILL','primaryMuscles':bool(c.get('primaryMuscles')),'secondaryMuscles':bool(c.get('secondaryMuscles')),'bodyRegion':bool(c.get('bodyRegion'))}
    for k,v in pairs.items():
        if not v:missing.append(k)
    for k,f in [('description','descriptionIt'),('setup','setupIt'),('execution','executionStepsIt'),('techniqueCues','techniqueCuesIt'),('commonErrors','commonErrorsIt')]:
        if not specific or not present(c.get(f)):missing.append(k)
    return missing

def digest(v):return hashlib.sha256(json.dumps(v,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
def valid_checkpoint(d,ids,snapshot):
    if d.get('ids')!=ids or d.get('snapshot')!=snapshot or not d.get('rows'):return False
    return d.get('digest')==digest({k:v for k,v in d.items() if k!='digest'})

def run(start,count):
    db=json.loads((WORK/'canonical.json').read_text())
    manifest=json.loads((WORK/'manifest.json').read_text())
    snapshot=digest({'manifest':manifest,'version':VERSION,'canonical':digest(db),'matcher':hashlib.sha256(pathlib.Path(__file__).read_bytes()).hexdigest()})
    selection=sorted(db,key=lambda x:x['id'])[start:start+count]
    ids=[x['id'] for x in selection]
    path=ROOT/'checkpoints'/f'{start:04d}-{start+len(ids)-1:04d}.json'
    if path.exists():
        if not valid_checkpoint(json.loads(path.read_text()),ids,snapshot):raise ValueError('Invalid or stale checkpoint: '+str(path))
        print(json.dumps({'skipped_valid':str(path),'count':len(ids)}));return
    sources=[normalize_source(json.loads((WORK/f'source{i}.json').read_text()),i) for i in range(len(manifest['sources']))]
    rows=[]
    for c in selection:
        missing=canonical_missing(c)
        for i,source in enumerate(sources):
            candidates=[]
            for s in source:
                result=assess(c,s)
                if result['match_type']=='NESSUN MATCH':continue
                candidates.append((s,result))
            probable=[x for x in candidates if x[1]['match_type']=='MATCH ALTAMENTE PROBABILE']
            if len(probable)>1:
                for s,result in probable:result['match_type']='MATCH AMBIGUO';result['conflicts'].append('multiple_supported_candidates_in_source')
            meta=manifest['sources'][i]
            base={'canonical_id':c['id'],'canonical_name':c['canonicalNameEn'],'source_dataset':meta['repository'],'source_snapshot_tree':meta['tree'],'canonical_missing':missing}
            if not candidates:
                rows.append({**base,'source_record_id':None,'match_type':'NESSUN MATCH','score':0,'method':'full_source_scan_no_candidate_above_threshold','evidence':[],'conflicts':[],'available_fields':{},'candidate_completion_fields':[],'assets':[]})
            for s,result in sorted(candidates,key=lambda x:(-x[1]['score'],x[0]['id'])):
                assets=[]
                for a in s['assets']:
                    local=not a['locator'].startswith('http')
                    exists=any(x['path']==a['locator'] for x in meta['assets']) if local else None
                    assets.append({**a,'repository_path_exists':exists,'availability':'repository_tree_verified' if exists else 'external_reference_not_fetched' if not local else 'missing_in_tree','rights':'SEE_SOURCE_MANIFEST_NOT_CLEARED_FOR_IMPORT'})
                fields={k:{'source_path':v['path'],'value_fingerprint':digest(v['value']),'item_count':len(v['value']) if isinstance(v['value'],list) else 1} for k,v in s['fields'].items()}
                rows.append({**base,**result,'source_record_id':s['id'],'source_exercise_name':s['name'],'source_ids':s['source_ids'],'available_fields':fields,'candidate_completion_fields':sorted(set(missing)&set(fields)),'assets':assets,'provenance':{'source_record_index':next(j for j,t in enumerate(source) if t is s),'source_file':next(f for f in meta['files'] if f['path'].endswith('exercises.json')),'canonical_blob':manifest['canonicalBlob']},'review_required':True})
    result={'schema_version':1,'matcher_version':VERSION,'snapshot':snapshot,'canonical_commit':manifest['canonicalCommit'],'start_index':start,'end_index':start+len(ids)-1,'ids':ids,'source_count':len(sources),'source_record_counts':[len(s) for s in sources],'comparisons':len(ids)*sum(map(len,sources)),'rows':rows,'errors':[],'statistics':dict(collections.Counter(x['match_type'] for x in rows))}
    result['digest']=digest(result)
    path.parent.mkdir(parents=True,exist_ok=True)
    temp=path.with_suffix('.tmp');temp.write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n');temp.replace(path)
    assert valid_checkpoint(json.loads(path.read_text()),ids,snapshot)
    print(json.dumps({'checkpoint':str(path),'count':len(ids),'last_id':ids[-1],'rows':len(rows),'digest':result['digest']}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--start',type=int,required=True);p.add_argument('--count',type=int,default=100);a=p.parse_args();run(a.start,a.count)
