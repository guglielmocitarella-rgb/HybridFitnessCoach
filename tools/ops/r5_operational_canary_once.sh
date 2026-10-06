#!/usr/bin/env bash
set -uo pipefail

BOOTSTRAP="$GITHUB_SHA"
BASELINE="b72edb8237dd0ce38358953e0beb69df51d589fc"
BATCH="tools/ops/r5_operational_canary_2026-10-06_pushup_to_renegade.json"
WF=".github/workflows/r5-operational-canary-once.yml"
SELF="tools/ops/r5_operational_canary_once.sh"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

fail_closed() {
  stage="$1"
  msg="$2"
  cd "$GITHUB_WORKSPACE" || exit 1
  git reset --hard "$BOOTSTRAP" >/dev/null 2>&1 || true
  git rm -f "$BATCH" "$WF" "$SELF" >/dev/null 2>&1 || true
  result=$(python3 -c 'import json,sys; print(json.dumps({"status":"FAIL","stage":sys.argv[1],"error":sys.argv[2]},separators=(",",":")))' "$stage" "$msg")
  if ! git diff --cached --quiet; then
    git commit -m "v3.10c-r5: operational canary FAIL [skip ci]" -m "R5_CANARY_RESULT=$result" >/dev/null || true
    remote=$(git ls-remote origin refs/heads/feature/exercise-db-v3 | cut -f1)
    if [ "$remote" = "$BOOTSTRAP" ]; then
      git push origin HEAD:feature/exercise-db-v3 >/dev/null 2>&1 || true
    fi
  fi
  echo "FAIL-CLOSED stage=$stage: $msg"
  exit 1
}

cp "$BATCH" /tmp/r5-batch.json || fail_closed "batch-copy" "unable to stage batch"

git worktree add --detach /tmp/r5base "$BASELINE" >/tmp/worktree.log 2>&1 || fail_closed "worktree" "$(tail -20 /tmp/worktree.log | tr '\n' ' ')"
cd /tmp/r5base || fail_closed "worktree-cd" "cannot enter detached baseline worktree"

python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json --validate-input >/tmp/input.json 2>/tmp/input.err
rc=$?
[ "$rc" -eq 0 ] || fail_closed "validate-input" "$(cat /tmp/input.err /tmp/input.json | tail -30 | tr '\n' ' ')"

python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json >/tmp/dry.json 2>/tmp/dry.err
rc=$?
[ "$rc" -eq 0 ] || fail_closed "dry-run" "$(cat /tmp/dry.err /tmp/dry.json | tail -30 | tr '\n' ' ')"

python3 - <<'PY' >/tmp/before.json
import importlib.util,json,hashlib
from pathlib import Path
spec=importlib.util.spec_from_file_location("r5",Path("tools/r5_apply_batch.py"))
A=importlib.util.module_from_spec(spec); spec.loader.exec_module(A)
cp,rows,mans,raw=A.load_state(Path("."))
idx,bad,counts=A.inspect(rows)
snap={"counts":counts,"bad":[list(x) for x in sorted(bad)],"rows":{}}
for fam in A.FAMILIES:
    snap["rows"][fam]={}
    for bucket,arr in rows[fam].items():
        for e in arr:
            snap["rows"][fam][A.sk(e)]=hashlib.sha256(A.canon(e)).hexdigest()
candidate=Path("exercise-db-canonical/exercise-db-v3-10c-r4-candidate.json")
rb=candidate.read_bytes()
snap["candidateSha256"]=hashlib.sha256(rb).hexdigest()
snap["prescriptionDependentCount"]=rb.count(b"PRESCRIPTION_DEPENDENT")
print(json.dumps(snap,separators=(",",":")))
PY
rc=$?
[ "$rc" -eq 0 ] || fail_closed "snapshot-before" "could not snapshot authoritative state"

python3 tools/r5_apply_batch.py --root . --batch /tmp/r5-batch.json --apply >/tmp/apply.json 2>/tmp/apply.err
rc=$?
[ "$rc" -eq 0 ] || fail_closed "apply" "$(cat /tmp/apply.err /tmp/apply.json | tail -40 | tr '\n' ' ')"

python3 - <<'PY' >/tmp/canary.json
import importlib.util,json,hashlib,subprocess
from pathlib import Path
spec=importlib.util.spec_from_file_location("r5",Path("tools/r5_apply_batch.py"))
A=importlib.util.module_from_spec(spec); spec.loader.exec_module(A)
batch=json.loads(Path("/tmp/r5-batch.json").read_text())
before=json.loads(Path("/tmp/before.json").read_text())
apply=json.loads(Path("/tmp/apply.json").read_text())
dry=json.loads(Path("/tmp/dry.json").read_text())
summary=A.validate_batch(batch)
assert summary["records"]==8
assert summary["resolvedCells"]==23
assert summary["terminalCells"]==3
assert summary["processedCells"]==26
cp,rows,mans,raw=A.load_state(Path("."))
idx,bad,counts=A.inspect(rows)
exp=batch["expectedPreviousCheckpoint"]
assert cp["processedResidualCells"]-exp["processedResidualCells"]==26
assert cp["resolvedCells"]-exp["resolvedCells"]==23
assert cp["terminalUnresolvedCells"]-exp["terminalUnresolvedCells"]==3
assert exp["remainingCells"]-cp["remainingCells"]==26
assert cp["resolvedCells"]+cp["terminalUnresolvedCells"]==cp["processedResidualCells"]
assert cp["processedResidualCells"]+cp["remainingCells"]==cp["initialResidualCells"]==3835
assert (cp["processedResidualCells"],cp["resolvedCells"],cp["terminalUnresolvedCells"],cp["remainingCells"])==(1458,1436,22,2377)
assert cp["sourcesVisited"]==401
assert cp["provenanceEntries"]==1436
assert cp["lastProcessedId"]=="cm-renegade-row"
for fam in ("field-decisions","evidence","provenance","patch"):
    assert counts[fam]==before["counts"][fam]+23
assert counts["research-log"]==before["counts"]["research-log"]+8
for fam in A.FAMILIES:
    for bucket,arr in rows[fam].items():
        for e in arr:
            k=A.sk(e)
            if k in before["rows"][fam]:
                assert hashlib.sha256(A.canon(e)).hexdigest()==before["rows"][fam][k]
assert [list(x) for x in sorted(bad)]==before["bad"]
for rec in batch["records"]:
    log=rec["researchLog"]
    found=idx["research-log"].get(A.sk(log))
    assert found and found[0]==A.route(log) and found[1]==log
    for cell in rec["cells"]:
        for fam,key in A.CELL_KEYS.items():
            e=cell[key]
            found=idx[fam].get(A.sk(e))
            assert found and found[0]==A.route(e) and found[1]==e
candidate=Path("exercise-db-canonical/exercise-db-v3-10c-r4-candidate.json")
rb=candidate.read_bytes()
assert hashlib.sha256(rb).hexdigest()==before["candidateSha256"]
assert rb.count(b"PRESCRIPTION_DEPENDENT")==441==before["prescriptionDependentCount"]
allowed_prefix="exercise-db-canonical/r5-authoritative-shards/"
allowed_cp="exercise-db-canonical/native-capability-r5-checkpoint.json"
changed=subprocess.check_output(["git","diff","--name-only","HEAD"],text=True).splitlines()
assert changed and all(p==allowed_cp or p.startswith(allowed_prefix) for p in changed)
assert apply["status"]=="PASS" and apply["mode"]=="APPLIED" and apply["checkpointLast"] is True
result={
  "status":"PASS",
  "applier":apply["mode"],
  "processedDelta":26,
  "resolvedDelta":23,
  "terminalDelta":3,
  "checkpoint":{"processed":1458,"resolved":1436,"terminal":22,"remaining":2377},
  "integrityCounts":counts,
  "routingViolations":len(bad),
  "prescriptionDependent":441,
  "researchLogCount":counts["research-log"],
  "provenanceCount":cp["provenanceEntries"],
  "lastProcessed":cp["lastProcessedId"],
  "changedFiles":changed,
  "touchedBuckets":dry.get("touchedBuckets",{})
}
print(json.dumps(result,separators=(",",":")))
PY
rc=$?
[ "$rc" -eq 0 ] || fail_closed "canary-validation" "post-apply integrity assertions failed"

# Contract-compatible routing validation is enforced by the canary assertions above:
# the historical violation set must remain unchanged and every new key must be FNV-routed.

mapfile -t changed < <(git diff --name-only HEAD)
cd "$GITHUB_WORKSPACE" || fail_closed "workspace-cd" "cannot return to bootstrap workspace"
for p in "${changed[@]}"; do
  mkdir -p "$(dirname "$p")"
  cp "/tmp/r5base/$p" "$p" || fail_closed "copy-output" "unable to copy $p"
done

git rm -f "$BATCH" "$WF" "$SELF" >/dev/null || fail_closed "cleanup-stage" "unable to stage one-shot cleanup"
git add exercise-db-canonical/r5-authoritative-shards exercise-db-canonical/native-capability-r5-checkpoint.json

remote=$(git ls-remote origin refs/heads/feature/exercise-db-v3 | cut -f1)
[ "$remote" = "$BOOTSTRAP" ] || fail_closed "publish-lease" "branch advanced during canary"

result=$(cat /tmp/canary.json)
git commit -m "v3.10c-r5: apply operational canary pushup-to-renegade [skip ci]" -m "R5_CANARY_RESULT=$result" >/dev/null || fail_closed "commit" "unable to commit canary outputs"
git push origin HEAD:feature/exercise-db-v3 >/tmp/push.log 2>&1 || fail_closed "push" "$(tail -40 /tmp/push.log | tr '\n' ' ')"
