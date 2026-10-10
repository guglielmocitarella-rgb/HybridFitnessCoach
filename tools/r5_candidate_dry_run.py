#!/usr/bin/env python3
"""R5 C1 diagnostic only: read pinned checkout, simulate in memory, write reports."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import resource
import signal
import subprocess
import sys

from r5_apply_batch import gitblob, route, sk, sortkey

BASELINE = 'f2698fc2ec51472a34d12502a3b74d89322ec186'
DATA = 'exercise-db-canonical/'
CANDIDATE = DATA + 'exercise-db-v3-10c-r4-candidate.json'
DIMENSIONS = ('strengthRequirement', 'technicalDifficulty', 'mobilityRequirement',
              'balanceCoordinationRequirement')
LEVELS = {'VERY_LOW', 'LOW', 'MODERATE', 'HIGH', 'VERY_HIGH'}
SPECIAL = {'NOT_APPLICABLE', 'VARIABLE', 'PRESCRIPTION_DEPENDENT', 'VARIANT_DEPENDENT'}
VALUES = LEVELS | SPECIAL | {'EVIDENCE_UNRESOLVED'}
MAX_FILE_BYTES = 32 * 1024 * 1024
MAX_TOTAL_BYTES = 128 * 1024 * 1024
MAX_REPORT_BYTES = 32 * 1024 * 1024
EXPECTED_CATEGORIES = {'ORPHAN_NO_TARGET': 148, 'PRESCRIPTION_DEPENDENT_VS_R5_SCALAR': 55,
                       'R3_VS_R5': 9, 'R3_IDENTICAL': 2}

# Exact baseline blob identities: the CLI never accepts caller-supplied replacements.
INPUT_PINS = {
    "exercise-db-canonical/exercise-db-v3-10c-r4-candidate.json": "d2b3361183dd5694167c7ca8323268e1d6d13d38",
    "exercise-db-canonical/native-capability-r5-authoritative-out-of-universe.json": "3ef4e83e5f92d05eb42b4370f714d1c92cd83add",
    "exercise-db-canonical/native-capability-r5-out-of-universe-disposition.json": "ec298c4758bf34cfe775096ee77f18dea80f8df2",
    "exercise-db-canonical/native-capability-r5-terminal-membership.json": "f2b09d4d01f345e308e8d4e7631ae8751b4379c7",
    "exercise-db-canonical/native-capability-r5-true-missing.json": "b8fe7cc86b642eb9cdf13cffa76f06b6ec193f33",
    "exercise-db-canonical/native-capability-r5-universe.json": "7974db07b4812551804435cdb7a6dead2e39e0f4",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-00.json": "4a308f5fbc26d8bb3ee5cb09a7fa1ba752955536",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-01.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-02.json": "0579c799223b5681af9b136ba5e15a6e73c4f0be",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-03.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-04.json": "464ed6a392f8c9bbcae79d346cac342c10ad4e83",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-05.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-06.json": "7fb935b78da799bf088cd0f5d35c059f85220fe2",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-07.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-08.json": "f99d01b39bd778c114bd545548079e9e36d31195",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-09.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0a.json": "ea442474fb3c02a76a7a2da1dbafaf3317337b8c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0b.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0c.json": "5e15150a1ac7f7cd7e77a9c992da5fab08030cb7",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0d.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0e.json": "9e1902fedcb285eab0bc23ce2c10e899fea32ae3",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/bucket-0f.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/field-decisions/manifest.json": "34c33f0149a46ef15923ad42df578c96c43019d7",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-00.json": "33994c0e695465e3cdd7b03a20e5baebc2fc238d",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-01.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-02.json": "6433b196202b1ed5320de1d2ec3e7d3e6510a0ef",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-03.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-04.json": "d2ac71baee0bf5ac73bc70d975fb26e90bc9e6c6",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-05.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-06.json": "b6ea4af83024594ac6f7178939891b8d7478b7a4",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-07.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-08.json": "327c99816a60a0c71a7545c7c5ac01df8a1c6507",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-09.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0a.json": "efe51e3a32fb7104e69c2367404a145cbeb50c75",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0b.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0c.json": "6f353d6913dee8d0433688dbc775bad62f3a1aa0",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0d.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0e.json": "ff3369cbd1f6caeba9693a0f9c882c6a9c91943d",
    "exercise-db-canonical/r5-authoritative-shards/patch/bucket-0f.json": "fe51488c7066f6687ef680d6bfaa4f7768ef205c",
    "exercise-db-canonical/r5-authoritative-shards/patch/manifest.json": "2380b814c6eb8d3667afffe09cf8ee9d5d8d0d5b"
}


class DryRunError(RuntimeError):
    pass


class LimitError(DryRunError):
    pass


def need(condition, message):
    if not condition:
        raise DryRunError(message)


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2,
                       allow_nan=False) + '\n').encode('utf-8')


def parse_json(raw, label):
    def pairs(items):
        result = {}
        for key, value in items:
            need(key not in result, f'{label}: duplicate JSON member {key}')
            result[key] = value
        return result

    def constant(value):
        raise DryRunError(f'{label}: invalid JSON constant {value}')

    try:
        return json.loads(raw.decode('utf-8'), object_pairs_hook=pairs, parse_constant=constant)
    except (ValueError, UnicodeError, RecursionError) as error:
        raise DryRunError(f'{label}: invalid JSON: {error}') from error


def no_symlinks(path):
    path = Path(os.path.abspath(path))
    need(not any(part.is_symlink() for part in (path, *path.parents)),
         f'symlink path forbidden: {path}')


def check_output(root, output):
    root = root.resolve()
    no_symlinks(output)
    output = Path(os.path.abspath(output))
    need(not output.is_relative_to(root), 'report directory must be outside checkout')
    need(not output.exists(), 'report directory already exists')
    need(output.parent.is_dir(), 'report parent directory must already exist')
    return output


class Reader:
    def __init__(self, root, pins, max_file_bytes=MAX_FILE_BYTES, max_total_bytes=MAX_TOTAL_BYTES):
        self.root = root
        self.pins = pins
        self.max_file_bytes = max_file_bytes
        self.max_total_bytes = max_total_bytes
        self.total = 0
        self.raw = {}
        self.ordering_observations = []

    def load(self, relative):
        need(relative in self.pins, f'unpinned input: {relative}')
        path = self.root / relative
        no_symlinks(path)
        need(path.is_file(), f'missing input: {relative}')
        size = path.stat().st_size
        if size > self.max_file_bytes or self.total + size > self.max_total_bytes:
            raise LimitError(f'input size limit: {relative}')
        with path.open('rb') as handle:
            raw = handle.read(self.max_file_bytes + 1)
        if len(raw) > self.max_file_bytes:
            raise LimitError(f'input grew beyond size limit: {relative}')
        need(len(raw) == size, f'input changed during read: {relative}')
        need(gitblob(raw) == self.pins[relative], f'Git blob SHA mismatch: {relative}')
        self.total += len(raw)
        self.raw[relative] = raw
        return parse_json(raw, relative)

    def verify_unchanged(self):
        for relative, old in self.raw.items():
            path = self.root / relative
            no_symlinks(path)
            need(path.stat().st_size == len(old), f'input size changed: {relative}')
            with path.open('rb') as handle:
                new = handle.read(len(old) + 1)
            need(new == old, f'input bytes changed: {relative}')


def cellkey(row):
    return row['canonicalExerciseId'] + '::' + (row.get('field') or row.get('dimension'))


def validate_cell(row, family):
    need(isinstance(row, dict), f'{family}: row must be object')
    field = 'field' if family == 'patch' else 'dimension'
    required = ('canonicalExerciseId', field, 'provenanceId', 'confidence')
    required += ('before', 'after') if family == 'patch' else (
        'previousState', 'newState', 'value', 'evidenceType', 'sourceTier', 'sourceTitle',
        'sourcePublisher', 'sourceLocator', 'reasoningRule', 'researchVersion')
    need(all(key in row for key in required), f'{family}: missing required member')
    need(all(isinstance(row[key], str) and row[key] for key in required if key != 'sourceTier'),
         f'{family}: invalid string member')
    need(set(row) <= set(required) | {'evidenceSummary'}, f'{family}: unsupported row member')
    if 'evidenceSummary' in row:
        need(isinstance(row['evidenceSummary'], str) and row['evidenceSummary'],
             f'{family}: invalid evidenceSummary')
    need(row[field] in DIMENSIONS, f'{family}: unknown capability dimension')
    value = row['after'] if family == 'patch' else row['value']
    need(value in LEVELS, f'{family}: resolved value must be a capability level')
    if family == 'patch':
        need(row['before'] in VALUES | {'UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE'},
             'patch: invalid before value')
    else:
        need(row['newState'] == 'KNOWN', 'decision must be KNOWN')
        need(type(row['sourceTier']) is int and 1 <= row['sourceTier'] <= 4,
             'decision: invalid sourceTier')


def load_family(reader, family, expected_total):
    prefix = DATA + 'r5-authoritative-shards/' + family + '/'
    manifest = reader.load(prefix + 'manifest.json')
    need(isinstance(manifest, dict) and manifest.get('schemaVersion') == 'V3.10C-R5-R-PS-2',
         f'{family}: unsupported manifest schema')
    need(manifest.get('partitionAlgorithmVersion') == 1 and manifest.get('shardCount') == 16,
         f'{family}: partition version/count')
    need(manifest.get('orderingKeyRule') == ['canonicalExerciseId', 'field-or-dimension', 'provenanceId'],
         f'{family}: ordering contract')
    descriptions = manifest.get('shards')
    need(isinstance(descriptions, list) and len(descriptions) == 16, f'{family}: shard descriptions')
    rows = []
    semantic = set()
    cells = set()
    provenance = set()
    for index in range(16):
        name = f'bucket-{index:02x}.json'
        desc = descriptions[index]
        need(isinstance(desc, dict) and desc.get('index') == index and desc.get('filename') == name,
             f'{family}: shard index/name mismatch')
        path = prefix + name
        shard = reader.load(path)
        need(isinstance(shard, list), f'{family}: non-array shard')
        raw = reader.raw[path]
        need(desc.get('byteSize') == len(raw) and desc.get('entryCount') == len(shard)
             and desc.get('gitBlobSha') == gitblob(raw), f'{family}: manifest integrity mismatch')
        for row in shard:
            validate_cell(row, family)
            key = sk(row)
            cell = cellkey(row)
            pid = row['provenanceId']
            need(key not in semantic and cell not in cells and pid not in provenance,
                 f'{family}: duplicate semantic key/cell/provenance')
            need(route(row) == index, f'{family}: incorrect UTF-16 routing')
            semantic.add(key)
            cells.add(cell)
            provenance.add(pid)
        # Historical families use mixed persisted comparator conventions. Pinned bytes
        # prove identity; deterministic diagnostic traversal does not rewrite their order.
        if shard != sorted(shard, key=sortkey):
            reader.ordering_observations.append({'family': family, 'shard': name,
                'observation': 'PERSISTED_ORDER_DIFFERS_FROM_PYTHON_TUPLE_ORDER',
                'inputRewritten': False})
        rows.extend(shard)
    need(len(rows) == expected_total == manifest.get('totalEntryCount'), f'{family}: total mismatch')
    return sorted(rows, key=sortkey)


def keyset(document, count, label):
    need(isinstance(document, dict), f'{label}: object required')
    keys = document.get('semanticKeys')
    need(isinstance(keys, list) and all(isinstance(k, str) and k.count('::') == 1 for k in keys),
         f'{label}: invalid keys')
    need(len(keys) == len(set(keys)) == count == document.get('totalSemanticKeys'),
         f'{label}: cardinality/duplicates')
    for key in keys:
        cid, dim = key.split('::')
        need(cid and dim in DIMENSIONS, f'{label}: invalid identity/dimension')
    return set(keys)


def index_candidate(rows):
    need(isinstance(rows, list), 'candidate must be an array')
    result = {}
    for position, row in enumerate(rows):
        need(isinstance(row, dict), 'candidate record must be object')
        cid = row.get('canonicalExerciseId')
        need(isinstance(cid, str) and cid and cid not in result, 'candidate duplicate/invalid ID')
        capability = row.get('capability')
        need(isinstance(capability, dict) and all(capability.get(d) in VALUES for d in DIMENSIONS),
             f'{cid}: invalid capability schema')
        need(capability.get('state') in {'KNOWN', 'PRESCRIPTION_DEPENDENT', 'EVIDENCE_UNRESOLVED'},
             f'{cid}: invalid aggregate state')
        result[cid] = position
    return result


def aggregate(capability):
    values = [capability[d] for d in DIMENSIONS]
    if all(value in LEVELS | {'NOT_APPLICABLE'} for value in values):
        return 'KNOWN'
    if all(value in LEVELS | SPECIAL for value in values) and 'PRESCRIPTION_DEPENDENT' in values:
        return 'PRESCRIPTION_DEPENDENT'
    return 'EVIDENCE_UNRESOLVED'


def verify_diff(before, after, changes, aggregate_changes):
    expected = copy.deepcopy(before)
    index = index_candidate(expected)
    for change in changes + aggregate_changes:
        cid = change['canonicalExerciseId']
        field = change['field']
        need(expected[index[cid]]['capability'][field] == change['before'], 'diff precondition mismatch')
        expected[index[cid]]['capability'][field] = change['after']
    need(expected == after, 'unauthorized candidate diff')


def simulate(rows, patches, universe, terminals):
    index = index_candidate(rows)
    result = copy.deepcopy(rows)
    report = {key: [] for key in ('safeChanges', 'aggregateChanges', 'alreadyAligned',
                                 'additionalConflicts', 'preservedTerminals', 'outsidePatchKeys')}
    touched = set()
    seen = set()
    for patch in sorted(patches, key=sortkey):
        validate_cell(patch, 'patch')
        key = cellkey(patch)
        need(key not in seen, 'duplicate patch cell')
        seen.add(key)
        if key not in universe:
            report['outsidePatchKeys'].append(key)
            continue
        cid, field = patch['canonicalExerciseId'], patch['field']
        need(cid in index, f'in-universe target missing: {key}')
        current = rows[index[cid]]['capability'][field]
        if key in terminals:
            report['preservedTerminals'].append(key)
            continue
        if current == patch['after']:
            report['alreadyAligned'].append(key)
        elif current == 'EVIDENCE_UNRESOLVED' and patch['before'] == current:
            result[index[cid]]['capability'][field] = patch['after']
            touched.add(cid)
            report['safeChanges'].append({'canonicalExerciseId': cid, 'field': field,
                'path': f'$[{index[cid]}].capability.{field}', 'before': current,
                'after': patch['after'], 'provenanceId': patch['provenanceId']})
        else:
            report['additionalConflicts'].append({'semanticKey': key, 'current': current,
                'patchBefore': patch['before'], 'proposed': patch['after'], 'applied': False})
    for cid in sorted(touched):
        old = rows[index[cid]]['capability']['state']
        new = aggregate(result[index[cid]]['capability'])
        if old != new:
            result[index[cid]]['capability']['state'] = new
            report['aggregateChanges'].append({'canonicalExerciseId': cid, 'field': 'state',
                'path': f'$[{index[cid]}].capability.state', 'before': old, 'after': new})
    verify_diff(rows, result, report['safeChanges'], report['aggregateChanges'])
    return result, report


def distribution(rows):
    result = {dimension: {} for dimension in DIMENSIONS}
    for row in rows:
        for dimension in DIMENSIONS:
            value = row['capability'][dimension]
            result[dimension][value] = result[dimension].get(value, 0) + 1
    return result


def git(root, *args):
    process = subprocess.run(['git', '--no-optional-locks', '-C', str(root), *args], capture_output=True,
                             text=True, timeout=30, check=False)
    need(process.returncode == 0, f'Git verification failed: {args[0]}')
    return process.stdout.strip()


def isolate_outside(rows, decision_index, patch_keys, outside, dispositions):
    index = index_candidate(rows)
    entries = dispositions.get('entries') if isinstance(dispositions, dict) else None
    need(dispositions.get('schemaVersion') == 'V3.10C-R5-MATERIALIZATION-DISPOSITION-1'
         and dispositions.get('total') == 214 and dispositions.get('counts') == EXPECTED_CATEGORIES
         and isinstance(entries, list) and len(entries) == 214, 'disposition schema/count mismatch')
    actual_categories = dict.fromkeys(EXPECTED_CATEGORIES, 0)
    isolated = []
    seen = set()
    for entry in sorted(entries, key=lambda item: item['semanticKey']):
        key = entry.get('semanticKey')
        need(key in outside and key not in seen and key == cellkey(entry), 'disposition key mismatch')
        seen.add(key)
        decision = decision_index[key]
        need((entry.get('r5Value'), entry.get('r5ProvenanceId')) ==
             (decision['value'], decision['provenanceId']), 'disposition/decision mismatch')
        category = entry.get('category')
        need(category in EXPECTED_CATEGORIES and entry.get('disposition') == category,
             'unknown disposition category')
        actual_categories[category] += 1
        cid, field = entry['canonicalExerciseId'], entry['field']
        current = rows[index[cid]]['capability'][field] if cid in index else None
        if category == 'ORPHAN_NO_TARGET':
            need(cid not in index, f'orphan unexpectedly has target: {key}')
        elif category == 'PRESCRIPTION_DEPENDENT_VS_R5_SCALAR':
            need(current == 'PRESCRIPTION_DEPENDENT', f'PD disposition differs from candidate: {key}')
        elif category == 'R3_VS_R5':
            need(current == entry.get('r3Value') and current in LEVELS
                 and current != decision['value'], f'R3 conflict differs from candidate: {key}')
        else:
            need(current == entry.get('r3Value') == decision['value'], f'R3 equivalence mismatch: {key}')
        isolated.append({'semanticKey': key, 'category': category, 'current': current,
                         'authoritativeDecision': decision, 'hasPatch': key in patch_keys, 'applied': False})
    need(seen == outside and actual_categories == EXPECTED_CATEGORIES, 'disposition partition mismatch')
    return isolated, actual_categories


def run_diagnostic(root):
    need(git(root, 'rev-parse', 'HEAD') == BASELINE, 'data checkout HEAD differs from pinned baseline')
    need(git(root, 'rev-parse', '--show-toplevel') == str(root), 'root must be checkout root')
    initial_status = git(root, 'status', '--porcelain=v1', '--untracked-files=all')
    need(not initial_status, 'data checkout is not clean')
    for relative, expected in INPUT_PINS.items():
        need(git(root, 'rev-parse', f'{BASELINE}:{relative}') == expected,
             f'baseline tree blob differs: {relative}')
    reader = Reader(root, INPUT_PINS)
    rows = reader.load(CANDIDATE)  # Raw bytes checked BEFORE JSON is parsed.
    index = index_candidate(rows)
    need(len(rows) == 1087, 'candidate record count differs from R4 contract')
    patches = load_family(reader, 'patch', 3992)
    decisions = load_family(reader, 'field-decisions', 4001)
    universe = keyset(reader.load(DATA + 'native-capability-r5-universe.json'), 3835, 'universe')
    outside = keyset(reader.load(DATA + 'native-capability-r5-authoritative-out-of-universe.json'),
                     214, 'out-of-universe')
    missing = keyset(reader.load(DATA + 'native-capability-r5-true-missing.json'), 0, 'TRUE_MISSING')
    terminal_doc = reader.load(DATA + 'native-capability-r5-terminal-membership.json')
    need(isinstance(terminal_doc, dict) and terminal_doc.get('totalMembershipCount') == 48,
         'terminal membership count')
    terminal_rows = terminal_doc.get('entries')
    need(isinstance(terminal_rows, list) and len(terminal_rows) == 48, 'terminal entries count')
    terminals = set()
    for entry in terminal_rows:
        need(isinstance(entry, dict) and entry.get('dimension') in DIMENSIONS
             and isinstance(entry.get('canonicalExerciseId'), str)
             and entry.get('semanticKey') == cellkey(entry)
             and entry.get('status') == 'TERMINAL_UNRESOLVED', 'invalid terminal member')
        need(entry['semanticKey'] not in terminals, 'duplicate terminal member')
        terminals.add(entry['semanticKey'])
    need(terminals <= universe and not outside & universe, 'invalid set membership')
    patch_index = {cellkey(p): p for p in patches}
    decision_index = {cellkey(d): d for d in decisions}
    resolved = set(patch_index) & universe
    need(len(resolved) == 3787 and not resolved & terminals and resolved | terminals | missing == universe,
         'R5 partition mismatch')
    need(set(decision_index) & universe == resolved and set(decision_index) - universe == outside,
         'authoritative decision membership mismatch')
    need(set(patch_index) <= set(decision_index), 'patch without authoritative decision')
    for key, patch in patch_index.items():
        decision = decision_index[key]
        need((patch['after'], patch['provenanceId'], patch['confidence']) ==
             (decision['value'], decision['provenanceId'], decision['confidence']),
             f'patch/decision relationship mismatch: {key}')
    need(len(set(patch_index) - universe) == 205 and len(outside - set(patch_index)) == 9,
         'out-of-universe patch coverage mismatch')
    dispositions = reader.load(DATA + 'native-capability-r5-out-of-universe-disposition.json')
    isolated, actual_categories = isolate_outside(rows, decision_index, set(patch_index), outside, dispositions)
    for key in terminals:
        cid, dim = key.split('::')
        need(cid in index and rows[index[cid]]['capability'][dim] == 'EVIDENCE_UNRESOLVED',
             f'terminal candidate mismatch: {key}')
    before_dist = distribution(rows)
    need(before_dist['strengthRequirement'].get('PRESCRIPTION_DEPENDENT', 0) == 441
         and all(before_dist[d].get('PRESCRIPTION_DEPENDENT', 0) == 0 for d in DIMENSIONS[1:]),
         'PD distribution differs from contract')
    result, simulation = simulate(rows, patches, universe, terminals)
    after_dist = distribution(result)
    for position, original in enumerate(rows):
        for dim in DIMENSIONS:
            if original['capability'][dim] == 'PRESCRIPTION_DEPENDENT':
                need(result[position]['capability'][dim] == 'PRESCRIPTION_DEPENDENT', 'PD mutated')
    for key in terminals:
        cid, dim = key.split('::')
        need(result[index[cid]]['capability'][dim] == rows[index[cid]]['capability'][dim], 'terminal mutated')
    reader.verify_unchanged()
    need(git(root, 'status', '--porcelain=v1', '--untracked-files=all') == initial_status,
         'checkout changed during diagnostic')
    additional = simulation['additionalConflicts']
    return {'schemaVersion': 'R5-C1-DRY-RUN-1',
        'status': 'DRY_RUN_COMPLETE_WITH_UNRESOLVED_PRECEDENCE',
        'baselineHead': BASELINE, 'candidateBlobSha': INPUT_PINS[CANDIDATE],
        'inputBlobShas': INPUT_PINS, 'candidatePersisted': False, 'inputWrites': 0,
        'counts': {'records': len(rows), 'universe': len(universe), 'resolved': len(resolved),
            'terminals': len(terminals), 'trueMissing': len(missing), 'patches': len(patches),
            'decisions': len(decisions), 'outside': len(outside), 'outsideWithPatch': 205,
            'outsideWithoutPatch': 9, 'precedenceConflicts': 64,
            'safeChanges': len(simulation['safeChanges']),
            'aggregateChanges': len(simulation['aggregateChanges']),
            'alreadyAligned': len(simulation['alreadyAligned']), 'additionalConflicts': len(additional)},
        'outsideCategories': actual_categories, 'isolatedOutsideUniverse': isolated,
        'orderingObservations': reader.ordering_observations,
        'precedenceConflicts': [entry for entry in isolated if entry['category'] in
                               {'PRESCRIPTION_DEPENDENT_VS_R5_SCALAR', 'R3_VS_R5'}],
        'distributionBefore': before_dist, 'distributionAfter': after_dist,
        'invariants': {'gitBaseline': 'PASS', 'inputBlobShas': 'PASS', 'setPartition': 'PASS',
                       'allowedDiffOnly': 'PASS', 'pdPreserved': 'PASS', 'terminalsPreserved': 'PASS',
                       'inputBytesUnchanged': 'PASS', 'checkoutUnchanged': 'PASS'}, **simulation}


def markdown(report):
    lines = ['# R5 C1 dry-run', '', f"Status: `{report['status']}`", '',
             'Diagnostic only. Candidate and Android runtime were not written.', '']
    if 'error' in report:
        lines += ['Failure: ' + report['error'].replace('\n', ' '), '']
        return '\n'.join(lines)
    lines += [f"Baseline: `{report['baselineHead']}`", '', '| Count | Value |', '|---|---:|']
    lines += [f'| {key} | {value} |' for key, value in sorted(report['counts'].items())]
    lines += ['', '## Safe cell changes', '', '| ID | Field | Before | After | Provenance |', '|---|---|---|---|---|']
    for change in report['safeChanges']:
        lines.append('| ' + ' | '.join(str(change[key]) for key in
                                      ('canonicalExerciseId', 'field', 'before', 'after', 'provenanceId')) + ' |')
    lines += ['', '## Aggregate changes', '']
    lines += [json.dumps(entry, ensure_ascii=False, sort_keys=True) for entry in report['aggregateChanges']]
    lines += ['', '## Preserved precedence conflicts', '', '| Semantic key | Category | Current | R5 |', '|---|---|---|---|']
    for entry in report['precedenceConflicts']:
        lines.append(f"| {entry['semanticKey']} | {entry['category']} | {entry['current']} | {entry['authoritativeDecision']['value']} |")
    lines += ['', '## Additional conflicts', '']
    lines += [json.dumps(entry, ensure_ascii=False, sort_keys=True) for entry in report['additionalConflicts']]
    lines += ['', 'All 214 outside-universe decisions and all input identities are in the JSON report.',
              'This result does not authorize candidate promotion.', '']
    return '\n'.join(lines)


def write_reports(output, report):
    payloads = {'r5-c1-dry-run-report.json': json_bytes(report),
                'r5-c1-dry-run-report.md': markdown(report).encode('utf-8')}
    if sum(map(len, payloads.values())) > MAX_REPORT_BYTES:
        raise LimitError('report size limit exceeded')
    output.mkdir(mode=0o700)
    for name, raw in payloads.items():
        with (output / name).open('xb') as handle:
            handle.write(raw)


def alarm_handler(_signal, _frame):
    raise LimitError('diagnostic timeout: 300 seconds')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--report-dir', type=Path, required=True)
    args = parser.parse_args()
    output = None
    code = 0
    try:
        no_symlinks(args.root)
        root = args.root.resolve(strict=True)
        output = check_output(root, args.report_dir)
        resource.setrlimit(resource.RLIMIT_AS, (2 * 1024**3, 2 * 1024**3))
        signal.signal(signal.SIGALRM, alarm_handler)
        signal.alarm(300)
        report = run_diagnostic(root)
    except (LimitError, MemoryError, subprocess.TimeoutExpired) as error:
        code = 3
        report = {'schemaVersion': 'R5-C1-DRY-RUN-1', 'status': 'FAIL_OPERATIONAL_LIMIT',
                  'error': str(error) or 'memory limit exceeded', 'inputWrites': 0}
    except (DryRunError, OSError, KeyError, TypeError, ValueError, RecursionError) as error:
        code = 2
        report = {'schemaVersion': 'R5-C1-DRY-RUN-1', 'status': 'FAIL_CLOSED',
                  'error': str(error), 'inputWrites': 0}
    finally:
        signal.alarm(0)
    if output is not None:
        try:
            write_reports(output, report)
        except (OSError, DryRunError) as error:
            print(f'report generation failed: {error}', file=sys.stderr)
            return 3 if isinstance(error, LimitError) else 2
    print(json.dumps({'status': report['status'], 'exitCode': code,
                      'error': report.get('error'), 'counts': report.get('counts')}, sort_keys=True))
    return code


if __name__ == '__main__':
    sys.exit(main())
