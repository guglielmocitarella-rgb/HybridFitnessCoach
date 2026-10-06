#!/usr/bin/env python3
"""Read-only R5 routing preflight; rejects any authoritative FNV mismatch."""
import argparse
import lzma
import hashlib
import json
from pathlib import Path
import sys

FAMILIES = ('field-decisions', 'evidence', 'provenance', 'patch', 'research-log')
SHARDS = 'exercise-db-canonical/r5-authoritative-shards/'
CHECKPOINT = 'exercise-db-canonical/native-capability-r5-checkpoint.json'


def git_blob(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()


def semantic_key(entry):
    return '|'.join((entry['canonicalExerciseId'], entry.get('field') or entry.get('dimension') or '', entry.get('provenanceId') or ''))


def fnv1a32_utf16(text):
    result = 2166136261
    raw = text.encode('utf-16-le')
    for i in range(0, len(raw), 2):
        result = ((result ^ int.from_bytes(raw[i:i+2], 'little')) * 16777619) & 0xffffffff
    return result


def inspect_files(files):
    """Inspect exact bytes; never infer empty content or rewrite historical data."""
    problems, counts = [], {}
    for family in FAMILIES:
        prefix = SHARDS + family + '/'
        manifest = json.loads(files[prefix + 'manifest.json'])
        entries_count = 0
        seen = set()
        if manifest.get('shardCount') != 16 or len(manifest.get('shards', [])) != 16:
            problems.append({'kind': 'MANIFEST_SHARD_COUNT', 'family': family})
            continue
        for i in range(16):
            path = prefix + f'bucket-{i:02x}.json'
            raw = files[path]
            raw = raw.encode() if isinstance(raw, str) else raw
            rows = json.loads(raw)
            if not isinstance(rows, list):
                raise ValueError('Expected JSON array: ' + path)
            descriptor = manifest['shards'][i]
            if descriptor.get('index') != i or descriptor.get('filename') != f'bucket-{i:02x}.json':
                problems.append({'kind': 'MANIFEST_DESCRIPTOR', 'path': path})
            if descriptor.get('gitBlobSha') != git_blob(raw):
                problems.append({'kind': 'MANIFEST_BLOB_HASH', 'path': path})
            if descriptor.get('entryCount') != len(rows):
                problems.append({'kind': 'MANIFEST_ENTRY_COUNT', 'path': path})
            for entry in rows:
                key = semantic_key(entry)
                if key in seen:
                    problems.append({'kind': 'DUPLICATE_SEMANTIC_KEY', 'family': family, 'key': key})
                seen.add(key)
                expected = fnv1a32_utf16(key) % 16
                if expected != i:
                    problems.append({'kind': 'WRONG_FNV_ROUTING', 'family': family,
                        'canonicalExerciseId': entry['canonicalExerciseId'], 'semanticKey': key,
                        'actualBucket': f'{i:02x}', 'expectedBucket': f'{expected:02x}',
                        'sourcePath': path, 'sourceGitBlobSha': git_blob(raw)})
            entries_count += len(rows)
        counts[family] = entries_count
        if manifest.get('totalEntryCount') != entries_count:
            problems.append({'kind': 'MANIFEST_TOTAL', 'family': family})
    checkpoint = json.loads(files[CHECKPOINT])
    return {'status': 'FAIL' if problems else 'PASS', 'families': counts,
            'checkpoint': {k: checkpoint[k] for k in ('processedResidualCells', 'resolvedCells', 'terminalUnresolvedCells', 'remainingCells', 'lastProcessedId')},
            'problems': sorted(problems, key=lambda e: (e.get('family', ''), e.get('canonicalExerciseId', ''), e['kind'])),
            'writes': 0, 'semanticChanges': 0}


def load_repository(root):
    paths = [CHECKPOINT]
    paths += [SHARDS + f + '/' + p for f in FAMILIES for p in ['manifest.json'] + [f'bucket-{i:02x}.json' for i in range(16)]]
    return {path: (root / path).read_bytes() for path in paths}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--root', type=Path)
    group.add_argument('--fixture', type=Path)
    args = parser.parse_args()
    try:
        if args.fixture:
            fixture = json.loads(lzma.decompress(args.fixture.read_bytes()))
            files = fixture['beforeFiles']
            for path, sha in fixture['beforeBlobs'].items():
                if git_blob(files[path].encode()) != sha:
                    raise ValueError('Historical Git blob mismatch: ' + path)
        else:
            files = load_repository(args.root)
        report = inspect_files(files)
    except (ValueError, OSError, KeyError, TypeError, lzma.LZMAError) as error:
        report = {'status': 'FAIL', 'error': str(error), 'writes': 0}
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
