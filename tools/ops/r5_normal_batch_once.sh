#!/usr/bin/env bash
set -uo pipefail
BOOTSTRAP="$GITHUB_SHA"
BASELINE="87ebbda5c559227a6a8befd7e9d562f189d792d2"
BATCH="tools/ops/r5_normal_batch_2026-10-06_respirazione_to_single_arm_cable.json"
WF=".github/workflows/r5-normal-batch-once.yml"
SELF="tools/ops/r5_normal_batch_once.sh"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
fail_closed(){ stage="$1"; msg="$2"; cd "$GITHUB_WORKSPACE" || exit 1; git reset --hard "$BOOTSTRAP" >/dev/null 2>&1 || true; git rm -f "$BATCH" "$WF" "$SELF" >/dev/null 2>&1 || true; result=$(python3 -c 'import json,sys; print(json.dumps({"status":"FAIL","stage":sys.argv[1],"error":sys.argv[2]},separators=(",",":")))' "$stage" "$msg"); if ! git diff --cached --quiet; then git commit -m "v3.10c-r5: normal batch FAIL [skip ci]" -m "R5_BATCH_RESULT=$result" >/dev/null || true; remote=$(git ls-remote origin refs/heads/feature/exercise-db-v3 | cut -f1); if [ "$remote" = "$BOOTSTRAP" ]; then git push origin HEAD:feature/exercise-db-v3 >/dev/null 2>&1 || true; fi; fi; echo "FAIL-CLOSED stage=$stage: $msg"; exit 1; }
cp "$BATCH" /tmp/r5-batch.json || fail_closed batch-copy "unable to stage batch"
git worktree add --detach /tmp/r5base "$BASELINE" >/tmp/worktree.log 2>&1 || fail_closed worktree "$(tail -20 /tmp/worktree.log | tr '\n' ' ')"
cd /tmp/r5base || fail_closed worktree-cd "cannot enter detached baseline worktree"
python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json --validate-input >/tmp/input.json 2>/tmp/input.err || fail_closed validate-input "$(cat /tmp/input.err /tmp/input.json | tail -30 | tr '\n' ' ')"
python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json >/tmp/dry.json 2>/tmp/dry.err || fail_closed dry-run "$(cat /tmp/dry.err /tmp/dry.json | tail -30 | tr '\n' ' ')"
python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json --apply >/tmp/apply.json 2>/tmp/apply.err || fail_closed apply "$(cat /tmp/apply.err /tmp/apply.json | tail -40 | tr '\n' ' ')"
python3 - <<'PY' >/tmp/result.json || fail_closed validation "post-apply integrity assertions failed"
import importlib.util,json,hashlib,subprocess
from pathlib import Path
spec=importlib.util.spec_from_file_location("r5",Path("tools/r5_apply_batch.py")); A=importlib.util.module_from_spec(spec); spec.loader.exec_module(A)
batch=json.loads(Path("/tmp/r5-batch.json").read_text()); apply=json.loads(Path("/tmp/apply.json").read_text()); dry=json.loads(Path("/tmp/dry.json").read_text())
summary=A.validate_batch(batch)
assert summary["records"]==8 and summary["resolvedCells"]==28 and summary["terminalCells"]==0 and summary["processedCells"]==28
cp,rows,mans,raw=A.load_state(Path(".")); idx,bad,counts=A.inspect(rows)
assert (cp["processedResidualCells"],cp["resolvedCells"],cp["terminalUnresolvedCells"],cp["remainingCells"])==(1486,1464,22,2349)
assert cp["sourcesVisited"]==409 and cp["provenanceEntries"]==1464
assert cp["lastProcessedId"]=="cm-single-arm-cable-chest-press"
assert cp["resolvedCells"]+cp["terminalUnresolvedCells"]==cp["processedResidualCells"]
assert cp["processedResidualCells"]+cp["remainingCells"]==cp["initialResidualCells"]==3835
assert counts["field-decisions"]==1464 and counts["evidence"]==1464 and counts["provenance"]==1464 and counts["patch"]==1464 and counts["research-log"]==409
assert len(bad)==11
candidate=Path("exercise-db-canonical/exercise-db-v3-10c-r4-candidate.json"); rb=candidate.read_bytes()
assert hashlib.sha1(rb).hexdigest()=="d2b3361183dd5694167c7ca8323268e1d6d13d38"
assert rb.count(b"PRESCRIPTION_DEPENDENT")==441
for rec in batch["records"]:
  log=rec["researchLog"]; found=idx["research-log"].get(A.sk(log)); assert found and found[0]==A.route(log) and found[1]==log
  for cell in rec["cells"]:
    for fam,key in A.CELL_KEYS.items():
      e=cell[key]; found=idx[fam].get(A.sk(e)); assert found and found[0]==A.route(e) and found[1]==e
changed=subprocess.check_output(["git","diff","--name-only","HEAD"],text=True).splitlines()
assert changed and all(p=="exercise-db-canonical/native-capability-r5-checkpoint.json" or p.startswith("exercise-db-canonical/r5-authoritative-shards/") for p in changed)
assert apply["status"]=="PASS" and apply["mode"]=="APPLIED" and apply["checkpointLast"] is True
print(json.dumps({"status":"PASS","applier":apply["mode"],"processedDelta":28,"resolvedDelta":28,"terminalDelta":0,"checkpoint":{"processed":1486,"resolved":1464,"terminal":22,"remaining":2349},"integrityCounts":counts,"routingViolations":len(bad),"prescriptionDependent":441,"researchLogCount":counts["research-log"],"provenanceCount":cp["provenanceEntries"],"lastProcessed":cp["lastProcessedId"],"changedFiles":changed,"touchedBuckets":dry.get("touchedBuckets",{})},separators=(",",":")))
PY
mapfile -t changed < <(git diff --name-only HEAD)
cd "$GITHUB_WORKSPACE" || fail_closed workspace-cd "cannot return to bootstrap workspace"
for p in "${changed[@]}"; do mkdir -p "$(dirname "$p")"; cp "/tmp/r5base/$p" "$p" || fail_closed copy-output "unable to copy $p"; done
git rm -f "$BATCH" "$WF" "$SELF" >/dev/null || fail_closed cleanup-stage "unable to stage one-shot cleanup"
git add exercise-db-canonical/r5-authoritative-shards exercise-db-canonical/native-capability-r5-checkpoint.json
remote=$(git ls-remote origin refs/heads/feature/exercise-db-v3 | cut -f1); [ "$remote" = "$BOOTSTRAP" ] || fail_closed publish-lease "branch advanced during batch"
result=$(cat /tmp/result.json)
git commit -m "v3.10c-r5: apply respirazione-to-single-arm-cable batch [skip ci]" -m "R5_BATCH_RESULT=$result" >/dev/null || fail_closed commit "unable to commit batch outputs"
git push origin HEAD:feature/exercise-db-v3 >/tmp/push.log 2>&1 || fail_closed push "$(tail -40 /tmp/push.log | tr '\n' ' ')"
