"""Verified resumable checkpoint tests. These do not claim the applier gate passed."""
import copy
import lzma
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import r5_batch_preflight as preflight

FIXTURE = ROOT / 'tests/fixtures/r5-prone-push.json.xz'


class PreflightTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(lzma.decompress(FIXTURE.read_bytes()))

    def test_exact_historical_git_blobs(self):
        self.assertEqual(set(self.fixture['beforeFiles']), set(self.fixture['beforeBlobs']))
        for path, sha in self.fixture['beforeBlobs'].items():
            self.assertEqual(preflight.git_blob(self.fixture['beforeFiles'][path].encode()), sha, path)

    def test_known_fnv_vectors_and_utf16_surrogate_pair(self):
        self.assertEqual(preflight.fnv1a32_utf16(''), 2166136261)
        self.assertEqual(preflight.fnv1a32_utf16('hello'), 0x4f9f2cab)
        # Independently expand U+1F600 into the two authoritative JS code units.
        h = ((2166136261 ^ 0xd83d) * 16777619) & 0xffffffff
        h = ((h ^ 0xde00) * 16777619) & 0xffffffff
        self.assertEqual(preflight.fnv1a32_utf16('\U0001f600'), h)

    def test_semantic_key_matches_guard(self):
        self.assertEqual(preflight.semantic_key({'canonicalExerciseId': 'id', 'dimension': 'field', 'provenanceId': 'p'}), 'id|field|p')
        self.assertEqual(preflight.semantic_key({'canonicalExerciseId': 'id'}), 'id||')

    def test_historical_routing_failure_is_exact_and_read_only(self):
        original = copy.deepcopy(self.fixture['beforeFiles'])
        report = preflight.inspect_files(original)
        self.assertEqual(report['status'], 'FAIL')
        self.assertEqual(len(report['problems']), 11)
        self.assertEqual({e['kind'] for e in report['problems']}, {'WRONG_FNV_ROUTING'})
        self.assertEqual({e['family'] for e in report['problems']}, {'research-log'})
        self.assertEqual(original, self.fixture['beforeFiles'])
        self.assertEqual(report['writes'], 0)
        self.assertEqual(report['checkpoint']['processedResidualCells'], 1405)

    def test_four_other_families_route_correctly(self):
        report = preflight.inspect_files(self.fixture['beforeFiles'])
        self.assertEqual(report['families'], {'field-decisions': 1386, 'evidence': 1386, 'provenance': 1386, 'patch': 1386, 'research-log': 385})
        self.assertFalse(any(p['family'] != 'research-log' for p in report['problems']))

    def test_fixture_batch_arithmetic(self):
        batch = self.fixture['batch']
        before = batch['expectedPreviousCheckpoint']; after = self.fixture['afterCheckpoint']
        cells = [c for r in batch['records'] for c in r['cells']]
        self.assertEqual(len(batch['records']), 8)
        self.assertEqual(len(cells), 27)
        self.assertEqual(after['processedResidualCells'] - before['processedResidualCells'], 27)
        self.assertEqual(after['resolvedCells'] - before['resolvedCells'], 27)
        self.assertEqual(after['terminalUnresolvedCells'], before['terminalUnresolvedCells'])
        self.assertEqual([after[k] for k in ('processedResidualCells', 'resolvedCells', 'terminalUnresolvedCells', 'remainingCells')], [1432, 1413, 19, 2403])
        self.assertEqual(len({preflight.semantic_key(c['decision']) for c in cells}), 27)

    def test_fixture_delta_is_lossless_at_payload_level(self):
        # Checks extraction only. Does NOT execute the applier or satisfy its regression gate.
        for family, input_key in [('field-decisions', 'decision'), ('evidence', 'evidence'), ('provenance', 'provenance'), ('patch', 'patch'), ('research-log', None)]:
            rows = sum([json.loads(v) for p, v in self.fixture['beforeFiles'].items() if '/' + family + '/bucket-' in p], [])
            for record in self.fixture['batch']['records']:
                rows.extend([record['researchLog']] if input_key is None else [c[input_key] for c in record['cells']])
            rows.sort(key=lambda e: (e['canonicalExerciseId'], e.get('field', e.get('dimension', '')), e.get('provenanceId', '')))
            raw = json.dumps(rows, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), self.fixture['afterSemanticSha256'][family])

    def test_cli_exits_nonzero_for_exact_historical_fixture(self):
        result = subprocess.run([sys.executable, str(ROOT/'tools/r5_batch_preflight.py'), '--fixture', str(FIXTURE)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(json.loads(result.stdout)['status'], 'FAIL')

    def test_truncated_fixture_returns_machine_readable_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'truncated.xz'
            path.write_bytes(FIXTURE.read_bytes()[:80])
            result = subprocess.run([sys.executable, str(ROOT/'tools/r5_batch_preflight.py'), '--fixture', str(path)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(json.loads(result.stdout)['status'], 'FAIL')
            self.assertEqual(result.stderr, '')

    def test_false_empty_file_is_rejected(self):
        files = dict(self.fixture['beforeFiles'])
        files[preflight.SHARDS + 'evidence/bucket-00.json'] = ''
        with self.assertRaises(ValueError):
            preflight.inspect_files(files)

    def test_corrupted_nonempty_blob_is_reported(self):
        files = dict(self.fixture['beforeFiles'])
        files[preflight.SHARDS + 'evidence/bucket-00.json'] += ' '
        report = preflight.inspect_files(files)
        self.assertTrue(any(p['kind'] == 'MANIFEST_BLOB_HASH' for p in report['problems']))


if __name__ == '__main__':
    unittest.main()
