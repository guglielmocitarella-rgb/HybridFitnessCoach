#!/usr/bin/env python3
import copy, importlib.util, json, os, subprocess, sys, tempfile, unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
SPEC=importlib.util.spec_from_file_location("r5_apply_batch", HERE/"r5_apply_batch.py")
A=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(A)


def run(cmd, cwd=None, check=True):
    r=subprocess.run(cmd,cwd=cwd,text=True,capture_output=True)
    if check and r.returncode:
        raise RuntimeError(f"cmd failed {cmd}\nstdout={r.stdout}\nstderr={r.stderr}")
    return r

def write_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(A.jbytes(obj))

def common(cid="cm-target",dim="timed",value="YES",pid="prov-cm-target-timed",state="KNOWN"):
    return {
        "canonicalExerciseId":cid,"dimension":dim,"previousState":"UNPROCESSED","newState":state,
        "value":value,"evidenceType":"DIRECT","sourceTier":"T1","sourceTitle":"Fixture source",
        "sourcePublisher":"Fixture publisher","sourceLocator":"fixture://source","evidenceSummary":"fixture evidence",
        "reasoningRule":"fixture rule","confidence":"HIGH","provenanceId":pid,"researchVersion":"R5-R"
    }

def make_record(cid="cm-target", value="YES", terminal=("loadRequired",)):
    d=common(cid=cid,value=value,pid=f"prov-{cid}-timed")
    e=copy.deepcopy(d); p=copy.deepcopy(d)
    x={"canonicalExerciseId":cid,"field":"timed","before":"UNPROCESSED","after":value,
       "provenanceId":d["provenanceId"],"confidence":"HIGH"}
    log={"canonicalExerciseId":cid,"exerciseName":"Target","identityStatus":"VERIFIED",
         "sources":[{"title":"Fixture source","publisher":"Fixture publisher","tier":"T1","locator":"fixture://source","finding":"supports target"}],
         "decision":"RESOLVED","fieldApplication":{"resolved":{"timed":value},"terminalUnresolved":list(terminal)}}
    return {"researchLog":log,"cells":[{"decision":d,"evidence":e,"provenance":p,"patch":x}]}

def seed_pd_entry():
    d=common(cid="seed-prescription-dependent",dim="externalLoad",value="PRESCRIPTION_DEPENDENT",
             pid="prov-seed-pd",state="PRESCRIPTION_DEPENDENT")
    e=copy.deepcopy(d); p=copy.deepcopy(d)
    x={"canonicalExerciseId":"seed-prescription-dependent","field":"externalLoad","before":"UNPROCESSED",
       "after":"PRESCRIPTION_DEPENDENT","provenanceId":"prov-seed-pd","confidence":"HIGH"}
    log={"canonicalExerciseId":"seed-prescription-dependent","exerciseName":"Seed PD","identityStatus":"VERIFIED",
         "sources":[{"title":"Seed","publisher":"Fixture","tier":"T1","locator":"fixture://seed","finding":"seed"}],
         "decision":"PRESCRIPTION_DEPENDENT","fieldApplication":{"resolved":{},"terminalUnresolved":["externalLoad"]}}
    return {"field-decisions":d,"evidence":e,"provenance":p,"patch":x,"research-log":log}

class FixtureRepo:
    def __init__(self, preexisting_bad_research_route=True):
        self.td=tempfile.TemporaryDirectory(); self.root=Path(self.td.name)
        run(["git","init","-q"],self.root); run(["git","config","user.email","fixture@example.test"],self.root); run(["git","config","user.name","Fixture"],self.root)
        self.cp={"initialResidualCells":100,"processedRecordIds":["seed-prescription-dependent"],
                 "processedResidualCells":1,"resolvedCells":1,"terminalUnresolvedCells":0,"remainingCells":99,
                 "sourcesVisited":1,"provenanceEntries":1,"lastProcessedId":"seed-prescription-dependent","validationState":"PASS"}
        write_json(self.root/A.CHECKPOINT,self.cp)
        seed=seed_pd_entry()
        for fam in A.FAMILIES:
            d=self.root/A.SHARDS/fam; d.mkdir(parents=True,exist_ok=True)
            buckets=[[] for _ in range(16)]
            entry=copy.deepcopy(seed[fam]); b=A.route(entry)
            if fam=="research-log" and preexisting_bad_research_route: b=(b+1)%16
            buckets[b].append(entry)
            shards=[]
            for i,arr in enumerate(buckets):
                rb=A.jbytes(arr); p=d/f"bucket-{i:02x}.json"; p.write_bytes(rb)
                shards.append({"index":i,"filename":p.name,"entryCount":len(arr),"byteSize":len(rb),"gitBlobSha":A.gitblob(rb)})
            manifest={"shardCount":16,"totalEntryCount":1,"shards":shards,
                      "semanticComparison":{"reconstructedEntryCount":1,"missing":0,"duplicate":0,"semanticMutation":0,"result":"PASS"}}
            write_json(d/"manifest.json",manifest)
        run(["git","add","."],self.root); run(["git","commit","-qm","fixture baseline"],self.root)
        self.head=run(["git","rev-parse","HEAD"],self.root).stdout.strip()
    def close(self): self.td.cleanup()
    def batch(self,cid="cm-target",value="YES",terminal=("loadRequired",)):
        return {"schemaVersion":1,"baselineHead":self.head,"expectedPreviousCheckpoint":copy.deepcopy(self.cp),"records":[make_record(cid,value,terminal)]}
    def save_batch(self,b,name="batch.json"):
        p=self.root/name; write_json(p,b); return p
    def commit_all(self,msg="fixture mutation"):
        run(["git","add","."],self.root); run(["git","commit","-qm",msg],self.root); self.head=run(["git","rev-parse","HEAD"],self.root).stdout.strip(); return self.head
    def refresh_manifest(self,fam):
        d=self.root/A.SHARDS/fam; m=json.loads((d/"manifest.json").read_text()); total=0
        for i in range(16):
            p=d/f"bucket-{i:02x}.json"; rb=p.read_bytes(); arr=json.loads(rb); total+=len(arr)
            m["shards"][i].update({"entryCount":len(arr),"byteSize":len(rb),"gitBlobSha":A.gitblob(rb)})
        m["totalEntryCount"]=total; m["semanticComparison"].update({"reconstructedEntryCount":total,"missing":0,"duplicate":0,"semanticMutation":0,"result":"PASS"})
        write_json(d/"manifest.json",m)
    def append_entry(self,fam,entry,bucket=None):
        b=A.route(entry) if bucket is None else bucket; p=self.root/A.SHARDS/fam/f"bucket-{b:02x}.json"; arr=json.loads(p.read_text()); arr.append(copy.deepcopy(entry)); arr.sort(key=A.sortkey); write_json(p,arr); self.refresh_manifest(fam)

class R5ApplyBatchTargetedTests(unittest.TestCase):
    def setUp(self): self.fx=FixtureRepo()
    def tearDown(self): self.fx.close()

    def test_01_valid_batch_applied_correctly(self):
        b=self.fx.batch(); p=A.build_plan(self.fx.root,b); self.assertEqual(p["mode"],"APPLY_READY")
        r=A.apply(self.fx.root,p); self.assertEqual(r["mode"],"APPLIED")
        cp=json.loads((self.fx.root/A.CHECKPOINT).read_text()); self.assertEqual(cp["processedResidualCells"],3); self.assertEqual(cp["resolvedCells"],2); self.assertEqual(cp["terminalUnresolvedCells"],1); self.assertEqual(cp["remainingCells"],97)

    def test_02_duplicate_semantic_key_same_batch_rejected(self):
        b=self.fx.batch(); b["records"][0]["cells"].append(copy.deepcopy(b["records"][0]["cells"][0]))
        with self.assertRaises(A.BatchError): A.validate_batch(b)

    def test_03_conflicting_existing_value_rejected(self):
        b=self.fx.batch(); other=make_record(value="NO");
        self.fx.append_entry("research-log",other["researchLog"])
        for fam,key in A.CELL_KEYS.items(): self.fx.append_entry(fam,other["cells"][0][key])
        self.fx.commit_all("seed conflict"); b["baselineHead"]=self.fx.head
        with self.assertRaisesRegex(A.BatchError,"conflicting existing keys"): A.build_plan(self.fx.root,b)

    def test_04_stale_baseline_rejected(self):
        b=self.fx.batch(); b["baselineHead"]="0"*40
        with self.assertRaisesRegex(A.BatchError,"stale baselineHead"): A.build_plan(self.fx.root,b)

    def test_05_stale_expected_checkpoint_rejected(self):
        b=self.fx.batch(); b["expectedPreviousCheckpoint"]["lastProcessedId"]="stale"
        with self.assertRaisesRegex(A.BatchError,"stale checkpoint"): A.build_plan(self.fx.root,b)

    def test_06_missing_evidence_rejected(self):
        b=self.fx.batch(); del b["records"][0]["cells"][0]["evidence"]
        with self.assertRaises(A.BatchError): A.validate_batch(b)

    def test_07_missing_provenance_rejected(self):
        b=self.fx.batch(); del b["records"][0]["cells"][0]["provenance"]
        with self.assertRaises(A.BatchError): A.validate_batch(b)

    def test_08_wrong_fnv_routing_rejected(self):
        b=self.fx.batch(); s=A.validate_batch(b); after=A.derive(b["expectedPreviousCheckpoint"],b,s)
        rec=b["records"][0]; self.fx.append_entry("research-log",rec["researchLog"],(A.route(rec["researchLog"])+1)%16)
        for fam,key in A.CELL_KEYS.items(): self.fx.append_entry(fam,rec["cells"][0][key])
        write_json(self.fx.root/A.CHECKPOINT,after); self.fx.commit_all("misrouted replay seed"); b["baselineHead"]=self.fx.head
        with self.assertRaisesRegex(A.BatchError,"partial replay/partial write detected"): A.build_plan(self.fx.root,b)

    def test_09_replay_idempotent_zero_double_increment(self):
        b=self.fx.batch(); A.apply(self.fx.root,A.build_plan(self.fx.root,b)); cp1=(self.fx.root/A.CHECKPOINT).read_bytes()
        p2=A.build_plan(self.fx.root,b); self.assertEqual(p2["mode"],"IDEMPOTENT_REPLAY"); r=A.apply(self.fx.root,p2); self.assertEqual(r["writes"],0); self.assertEqual(cp1,(self.fx.root/A.CHECKPOINT).read_bytes())

    def test_10_invalid_input_checkpoint_does_not_advance(self):
        b=self.fx.batch(); del b["records"][0]["cells"][0]["evidence"]; bp=self.fx.save_batch(b); before=(self.fx.root/A.CHECKPOINT).read_bytes()
        r=run([sys.executable,str(HERE/"r5_apply_batch.py"),"--root",str(self.fx.root),"--batch",str(bp),"--apply"],check=False)
        self.assertEqual(r.returncode,1); self.assertEqual(before,(self.fx.root/A.CHECKPOINT).read_bytes())

    def test_11_prescription_dependent_preserved(self):
        _,rows,_,_=A.load_state(self.fx.root); before=copy.deepcopy(rows)
        b=self.fx.batch(); A.apply(self.fx.root,A.build_plan(self.fx.root,b)); _,after,_,_=A.load_state(self.fx.root)
        key="seed-prescription-dependent|externalLoad|prov-seed-pd"
        for fam in ("field-decisions","evidence","provenance","patch"):
            bi=A.route(seed_pd_entry()[fam]); got={A.sk(e):e for e in after[fam][bi]}[key]; old={A.sk(e):e for e in before[fam][bi]}[key]; self.assertEqual(old,got); self.assertEqual(got.get("newState",got.get("after")),"PRESCRIPTION_DEPENDENT")

    def test_12_checkpoint_arithmetic(self):
        b=self.fx.batch(); s=A.validate_batch(b); n=A.derive(b["expectedPreviousCheckpoint"],b,s)
        self.assertEqual((n["processedResidualCells"],n["resolvedCells"],n["terminalUnresolvedCells"],n["remainingCells"]),(3,2,1,97)); self.assertEqual(n["processedResidualCells"]+n["remainingCells"],n["initialResidualCells"])

    def test_13_cross_family_referential_integrity(self):
        b=self.fx.batch(); A.apply(self.fx.root,A.build_plan(self.fx.root,b)); _,rows,_,_=A.load_state(self.fx.root); idx,_,_=A.inspect(rows)
        base=set(idx["field-decisions"]); self.assertEqual(base,set(idx["evidence"])); self.assertEqual(base,set(idx["provenance"])); self.assertEqual(base,set(idx["patch"]))
        logs={e["canonicalExerciseId"] for arr in rows["research-log"].values() for e in arr}
        for k in base: self.assertIn(k.split("|",1)[0],logs)

    def test_14_manifest_and_counts_updated(self):
        b=self.fx.batch(); p=A.build_plan(self.fx.root,b); touched=p["touchedBuckets"]; A.apply(self.fx.root,p)
        for fam in A.FAMILIES:
            m=json.loads((self.fx.root/A.SHARDS/fam/"manifest.json").read_text()); expected=2 if fam=="research-log" else 2
            self.assertEqual(m["totalEntryCount"],expected)
            for hx in touched[fam]:
                i=int(hx,16); rb=(self.fx.root/A.SHARDS/fam/f"bucket-{i:02x}.json").read_bytes(); self.assertEqual(m["shards"][i]["gitBlobSha"],A.gitblob(rb)); self.assertEqual(m["shards"][i]["entryCount"],len(json.loads(rb)))

    def test_15_checkpoint_written_last(self):
        b=self.fx.batch(); p=A.build_plan(self.fx.root,b); seen=[]; orig=A.areplace; root=self.fx.root.resolve()
        def wrapped(path,data):
            path=Path(path)
            try: rel=str(path.relative_to(root))
            except ValueError: rel=None
            if rel in p["outputs"]: seen.append(rel)
            return orig(path,data)
        A.areplace=wrapped
        try: A.apply(self.fx.root,p)
        finally: A.areplace=orig
        self.assertTrue(seen); self.assertEqual(seen[-1],str(A.CHECKPOINT)); self.assertNotIn(str(A.CHECKPOINT),seen[:-1])

    def test_15b_data_validation_precedes_checkpoint_replace(self):
        b=self.fx.batch(); plan=A.build_plan(self.fx.root,b); orig_replace=A.areplace; orig_need=A.need; root=self.fx.root.resolve(); checkpoint=str(A.CHECKPOINT); checkpoint_seen=[]
        def wrapped_replace(path,data):
            path=Path(path)
            try: rel=str(path.relative_to(root))
            except ValueError: rel=None
            if rel==checkpoint and data==plan["outputs"][checkpoint]: checkpoint_seen.append(True)
            return orig_replace(path,data)
        def fail_first_postwrite(ok,msg):
            if str(msg).startswith("post-write verify failed"):
                raise A.BatchError("injected post-write validation failure")
            return orig_need(ok,msg)
        A.areplace=wrapped_replace; A.need=fail_first_postwrite
        try:
            with self.assertRaisesRegex(A.BatchError,"injected post-write validation failure"): A.apply(self.fx.root,plan)
        finally:
            A.areplace=orig_replace; A.need=orig_need
        self.assertEqual(checkpoint_seen,[],"checkpoint must not be replaced before data validation succeeds")

    def test_16_fault_recovery_replay_converges_losslessly(self):
        b=self.fx.batch(); bp=self.fx.save_batch(b); p=A.build_plan(self.fx.root,b); before_cp=(self.fx.root/A.CHECKPOINT).read_bytes(); orig=A.areplace; root=self.fx.root.resolve(); checkpoint=str(A.CHECKPOINT)
        def crash_before_checkpoint(path,data):
            path=Path(path)
            try: rel=str(path.relative_to(root))
            except ValueError: rel=None
            if rel==checkpoint: raise SystemExit(97)
            return orig(path,data)
        A.areplace=crash_before_checkpoint
        try:
            with self.assertRaises(SystemExit): A.apply(self.fx.root,p)
        finally: A.areplace=orig
        self.assertEqual(before_cp,(self.fx.root/A.CHECKPOINT).read_bytes()); self.assertTrue((self.fx.root/A.TXN).exists())
        r1=run([sys.executable,str(HERE/"r5_apply_batch.py"),"--root",str(self.fx.root),"--batch",str(bp),"--apply"],check=False)
        self.assertEqual(r1.returncode,1); self.assertIn("rolled back",r1.stdout); self.assertEqual(before_cp,(self.fx.root/A.CHECKPOINT).read_bytes())
        r2=run([sys.executable,str(HERE/"r5_apply_batch.py"),"--root",str(self.fx.root),"--batch",str(bp),"--apply"],check=False); self.assertEqual(r2.returncode,0,r2.stdout+r2.stderr); out2=json.loads(r2.stdout); self.assertEqual(out2["mode"],"APPLIED")
        cp_after=(self.fx.root/A.CHECKPOINT).read_bytes()
        r3=run([sys.executable,str(HERE/"r5_apply_batch.py"),"--root",str(self.fx.root),"--batch",str(bp),"--apply"],check=False); self.assertEqual(r3.returncode,0); out3=json.loads(r3.stdout); self.assertEqual(out3["mode"],"IDEMPOTENT_REPLAY"); self.assertEqual(out3["writes"],0); self.assertEqual(cp_after,(self.fx.root/A.CHECKPOINT).read_bytes())

    def test_17_preexisting_research_routing_violation_is_preserved_not_global_failure(self):
        b=self.fx.batch(); p=A.build_plan(self.fx.root,b); self.assertEqual(p["preexistingRoutingViolations"],1); A.apply(self.fx.root,p); _,rows,_,_=A.load_state(self.fx.root); _,bad,_=A.inspect(rows); self.assertEqual(len(bad),1)

if __name__=="__main__": unittest.main(verbosity=2)
