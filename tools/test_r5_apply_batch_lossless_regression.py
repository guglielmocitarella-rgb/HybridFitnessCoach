#!/usr/bin/env python3
import copy, importlib.util, json, subprocess, tempfile, unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
SPEC=importlib.util.spec_from_file_location("r5_apply_batch", HERE/"r5_apply_batch.py")
A=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(A)
FIXTURE=HERE/"fixtures"/"r5_prone_push_lossless_batch.json"

def run(cmd,cwd=None):
    r=subprocess.run(cmd,cwd=cwd,text=True,capture_output=True)
    if r.returncode:
        raise RuntimeError(f"cmd failed {cmd}\nstdout={r.stdout}\nstderr={r.stderr}")
    return r

def write_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(A.jbytes(obj))

class PronePushLosslessRegression(unittest.TestCase):
    def test_historical_prone_push_batch_reconstructs_authoritative_target(self):
        fixture=json.loads(FIXTURE.read_text())
        meta=fixture["_regression"]
        batch=copy.deepcopy(fixture)
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            run(["git","init","-q"],root)
            run(["git","config","user.email","regression@example.test"],root)
            run(["git","config","user.name","Regression Fixture"],root)
            write_json(root/A.CHECKPOINT,batch["expectedPreviousCheckpoint"])
            for fam in A.FAMILIES:
                d=root/A.SHARDS/fam
                d.mkdir(parents=True,exist_ok=True)
                shards=[]
                for i in range(16):
                    p=d/f"bucket-{i:02x}.json"
                    rb=A.jbytes([])
                    p.write_bytes(rb)
                    shards.append({"index":i,"filename":p.name,"entryCount":0,"byteSize":len(rb),"gitBlobSha":A.gitblob(rb)})
                write_json(d/"manifest.json",{
                    "shardCount":16,
                    "totalEntryCount":0,
                    "shards":shards,
                    "semanticComparison":{"reconstructedEntryCount":0,"missing":0,"duplicate":0,"semanticMutation":0,"result":"PASS"}
                })
            run(["git","add","."],root)
            run(["git","commit","-qm","historical predecessor fixture"],root)
            batch["baselineHead"]=run(["git","rev-parse","HEAD"],root).stdout.strip()

            summary=A.validate_batch(batch)
            self.assertEqual((summary["records"],summary["resolvedCells"],summary["terminalCells"],summary["processedCells"]),(8,27,0,27))
            plan=A.build_plan(root,batch)
            self.assertEqual(plan["mode"],"APPLY_READY")
            result=A.apply(root,plan)
            self.assertEqual(result["mode"],"APPLIED")

            checkpoint=json.loads((root/A.CHECKPOINT).read_text())
            self.assertEqual(checkpoint,meta["authoritativeTargetCheckpoint"])
            self.assertEqual((checkpoint["processedResidualCells"],checkpoint["resolvedCells"],checkpoint["terminalUnresolvedCells"],checkpoint["remainingCells"]),(1432,1413,19,2403))
            self.assertEqual(checkpoint["lastProcessedId"],"cm-push-up-inclinato")

            _,rows,_,_=A.load_state(root)
            actual={fam:{} for fam in A.FAMILIES}
            for fam in A.FAMILIES:
                for arr in rows[fam].values():
                    for e in arr:
                        actual[fam][A.sk(e)]=e

            expected={fam:{} for fam in A.FAMILIES}
            for rec in batch["records"]:
                log=rec["researchLog"]
                expected["research-log"][A.sk(log)]=log
                for cell in rec["cells"]:
                    for fam,key in A.CELL_KEYS.items():
                        e=cell[key]
                        expected[fam][A.sk(e)]=e
            self.assertEqual(actual,expected)

            replay=A.build_plan(root,batch)
            self.assertEqual(replay["mode"],"IDEMPOTENT_REPLAY")
            cp_before=(root/A.CHECKPOINT).read_bytes()
            rr=A.apply(root,replay)
            self.assertEqual(rr["writes"],0)
            self.assertEqual(cp_before,(root/A.CHECKPOINT).read_bytes())

if __name__=="__main__":
    unittest.main(verbosity=2)
