import copy
import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest
import subprocess
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
try:
    import r5_candidate_dry_run as dry
except ImportError:
    dry = None


class DryRunTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(dry, 'dedicated dry-run module is missing')

    def row(self, value='EVIDENCE_UNRESOLVED'):
        return {'canonicalExerciseId': 'ex-one', 'name': 'untouched',
                'capability': {**dict.fromkeys(dry.DIMENSIONS, 'LOW'),
                               'mobilityRequirement': value,
                               'state': 'EVIDENCE_UNRESOLVED'}}

    def patch(self, after='HIGH'):
        return {'canonicalExerciseId': 'ex-one', 'field': 'mobilityRequirement',
                'before': 'EVIDENCE_UNRESOLVED', 'after': after,
                'provenanceId': 'R5-test', 'confidence': 'HIGH_CONFIDENCE'}

    def test_safe_transition_changes_only_cell_and_aggregate(self):
        original = [self.row()]
        before = copy.deepcopy(original)
        result, report = dry.simulate(original, [self.patch()], {'ex-one::mobilityRequirement'}, set())
        self.assertEqual(original, before)
        self.assertEqual(result[0]['capability']['mobilityRequirement'], 'HIGH')
        self.assertEqual(result[0]['capability']['state'], 'KNOWN')
        self.assertEqual(result[0]['name'], 'untouched')
        self.assertEqual(len(report['safeChanges']), 1)
        self.assertEqual(len(report['aggregateChanges']), 1)

    def test_pd_terminal_and_outside_are_preserved(self):
        for value, universe, terminal in [
            ('PRESCRIPTION_DEPENDENT', {'ex-one::mobilityRequirement'}, set()),
            ('EVIDENCE_UNRESOLVED', {'ex-one::mobilityRequirement'}, {'ex-one::mobilityRequirement'}),
            ('LOW', set(), set()),
        ]:
            with self.subTest(value=value, universe=universe):
                rows = [self.row(value)]
                result, _ = dry.simulate(rows, [self.patch()], universe, terminal)
                self.assertEqual(rows, result)

    def test_stale_before_is_conflict_and_not_applied(self):
        rows = [self.row('MODERATE')]
        result, report = dry.simulate(rows, [self.patch()], {'ex-one::mobilityRequirement'}, set())
        self.assertEqual(rows, result)
        self.assertEqual(len(report['additionalConflicts']), 1)

    def test_repeated_simulation_is_noop_and_reports_are_deterministic(self):
        args = ([self.row()], [self.patch()], {'ex-one::mobilityRequirement'}, set())
        first, report = dry.simulate(*args)
        _, repeat = dry.simulate(*args)
        self.assertEqual(dry.json_bytes(report), dry.json_bytes(repeat))
        second, report2 = dry.simulate(first, args[1], args[2], args[3])
        self.assertEqual(first, second)
        self.assertEqual(report2['safeChanges'], [])
        self.assertEqual(len(report2['alreadyAligned']), 1)

    def test_missing_target_and_duplicate_id_fail(self):
        with self.assertRaises(dry.DryRunError):
            dry.simulate([], [self.patch()], {'ex-one::mobilityRequirement'}, set())
        with self.assertRaises(dry.DryRunError):
            dry.simulate([self.row(), self.row()], [], set(), set())

    def test_json_duplicate_keys_and_nan_are_rejected(self):
        for raw in (b'{"a":1,"a":2}', b'{"a":NaN}', b''):
            with self.assertRaises(dry.DryRunError):
                dry.parse_json(raw, 'test')

    def test_blob_hash_includes_git_header(self):
        self.assertEqual(dry.gitblob(b''), 'e69de29bb2d1d6434b8b29ae775ad8c2e48c5391')
        self.assertNotEqual(dry.gitblob(b'[]\n'), dry.gitblob(b'[]'))

    def test_utf16_routing_matches_explicit_surrogates(self):
        entry = self.patch()
        entry['canonicalExerciseId'] = 'ex-\U0001f600'
        key = dry.sk(entry)
        h = 2166136261
        raw = key.encode('utf-16-le')
        for pos in range(0, len(raw), 2):
            h = ((h ^ int.from_bytes(raw[pos:pos+2], 'little')) * 16777619) & 0xffffffff
        self.assertEqual(dry.route(entry), h % 16)

    def test_report_destination_rejects_inside_existing_and_symlink(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory) / 'repo'
            root.mkdir()
            with self.assertRaises(dry.DryRunError):
                dry.check_output(root, root / 'reports')
            outside = pathlib.Path(directory) / 'reports'
            outside.mkdir()
            with self.assertRaises(dry.DryRunError):
                dry.check_output(root, outside)
            link = pathlib.Path(directory) / 'link'
            link.symlink_to(root, target_is_directory=True)
            with self.assertRaises(dry.DryRunError):
                dry.check_output(root, link / 'reports')

    def test_input_sha_limit_and_symlink_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            path = root / 'input.json'
            path.write_bytes(b'[]\n')
            reader = dry.Reader(root, {'input.json': '0' * 40})
            with self.assertRaises(dry.DryRunError):
                reader.load('input.json')
            reader = dry.Reader(root, {'input.json': dry.gitblob(b'[]\n')}, max_file_bytes=2)
            with self.assertRaises(dry.LimitError):
                reader.load('input.json')
            path.unlink()
            target = root / 'target'
            target.write_bytes(b'[]\n')
            path.symlink_to(target)
            with self.assertRaises(dry.DryRunError):
                dry.Reader(root, {'input.json': dry.gitblob(b'[]\n')}).load('input.json')

    def test_unexpected_diff_is_rejected(self):
        before = [self.row()]
        after = copy.deepcopy(before)
        after[0]['name'] = 'changed'
        with self.assertRaises(dry.DryRunError):
            dry.verify_diff(before, after, [], [])

    def test_historical_before_is_readable_but_not_silently_normalized(self):
        patch = self.patch()
        patch['before'] = 'UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE'
        dry.validate_cell(patch, 'patch')
        rows = [self.row()]
        result, report = dry.simulate(rows, [patch], {'ex-one::mobilityRequirement'}, set())
        self.assertEqual(result, rows)
        self.assertEqual(len(report['additionalConflicts']), 1)

    def test_persisted_decision_schema_accepts_optional_summary_and_tier_four(self):
        row = {'canonicalExerciseId': 'ex-one', 'dimension': 'mobilityRequirement',
               'previousState': 'UNKNOWN_TERMINAL_INSUFFICIENT_EVIDENCE', 'newState': 'KNOWN',
               'value': 'LOW', 'sourceTier': 4}
        for key in ('provenanceId', 'confidence', 'evidenceType', 'sourceTitle', 'sourcePublisher',
                    'sourceLocator', 'reasoningRule', 'researchVersion'):
            row[key] = 'persisted-value'
        dry.validate_cell(row, 'field-decisions')
        row['sourceTier'] = 5
        with self.assertRaises(dry.DryRunError):
            dry.validate_cell(row, 'field-decisions')

    def test_family_integrity_and_deterministic_traversal(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            prefix = dry.DATA + 'r5-authoritative-shards/patch/'
            rows = [self.patch()]
            extra = self.patch('LOW')
            extra['canonicalExerciseId'] = 'ex-two'
            extra['provenanceId'] = 'R5-two'
            rows.append(extra)
            pins = {}
            manifest = {'schemaVersion': 'V3.10C-R5-R-PS-2', 'partitionAlgorithmVersion': 1,
                        'shardCount': 16, 'totalEntryCount': 2,
                        'orderingKeyRule': ['canonicalExerciseId', 'field-or-dimension', 'provenanceId'],
                        'shards': []}
            for index in range(16):
                path = prefix + f'bucket-{index:02x}.json'
                values = list(reversed([row for row in rows if dry.route(row) == index]))
                raw = dry.json_bytes(values)
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(raw)
                pins[path] = dry.gitblob(raw)
                manifest['shards'].append({'index': index, 'filename': target.name,
                    'entryCount': len(values), 'byteSize': len(raw), 'gitBlobSha': pins[path]})
            path = prefix + 'manifest.json'
            raw = dry.json_bytes(manifest)
            (root / path).write_bytes(raw)
            pins[path] = dry.gitblob(raw)
            actual = dry.load_family(dry.Reader(root, pins), 'patch', 2)
            self.assertEqual(actual, sorted(rows, key=dry.sortkey))
            (root / (prefix + 'bucket-01.json')).unlink()
            with self.assertRaises(dry.DryRunError):
                dry.load_family(dry.Reader(root, pins), 'patch', 2)

    def test_cli_failure_produces_reports_and_preserves_input_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory) / 'repo'
            root.mkdir()
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            marker = root / 'candidate.json'
            marker.write_bytes(b'original candidate bytes')
            output = pathlib.Path(directory) / 'report'
            process = subprocess.run([sys.executable, str(ROOT / 'tools/r5_candidate_dry_run.py'),
                                      '--root', str(root), '--report-dir', str(output)],
                                     capture_output=True, text=True, timeout=15)
            self.assertEqual(process.returncode, 2, process.stderr)
            report = json.loads((output / 'r5-c1-dry-run-report.json').read_text())
            self.assertEqual(report['status'], 'FAIL_CLOSED')
            self.assertEqual(report['inputWrites'], 0)
            self.assertTrue((output / 'r5-c1-dry-run-report.md').is_file())
            self.assertEqual(marker.read_bytes(), b'original candidate bytes')

    def test_timeout_handler_and_total_input_limit_fail_explicitly(self):
        with self.assertRaises(dry.LimitError):
            dry.alarm_handler(None, None)
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            raw = b'[]\n'
            for name in ('one.json', 'two.json'):
                (root / name).write_bytes(raw)
            reader = dry.Reader(root, dict.fromkeys(('one.json', 'two.json'), dry.gitblob(raw)),
                                max_total_bytes=4)
            reader.load('one.json')
            with self.assertRaises(dry.LimitError):
                reader.load('two.json')

    def test_input_mutation_after_read_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            path = root / 'input.json'
            path.write_bytes(b'[]\n')
            reader = dry.Reader(root, {'input.json': dry.gitblob(b'[]\n')})
            reader.load('input.json')
            path.write_bytes(b'{}\n')
            with self.assertRaises(dry.DryRunError):
                reader.verify_unchanged()

    def test_workflow_is_manual_read_only_and_preserves_exit_codes(self):
        text = (ROOT / '.github/workflows/r5-c1-candidate-dry-run.yml').read_text()
        self.assertIn('  workflow_dispatch:', text)
        self.assertIn('  contents: read', text)
        self.assertEqual(text.count('persist-credentials: false'), 2)
        self.assertIn('timeout-minutes: 15', text)
        for forbidden in ('  push:', '  pull_request:', 'contents: write', 'git push', 'git commit',
                          'continue-on-error', '|| true', 'secrets.', 'id-token:', '--apply'):
            self.assertNotIn(forbidden, text)
        import re
        self.assertEqual(len(re.findall(r'uses: [\w/-]+@[0-9a-f]{40}', text)), 4)

    def test_git_verification_disables_index_refresh_writes(self):
        with patch.object(dry.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, '', '')) as run:
            dry.git(pathlib.Path('/checkout'), 'status', '--porcelain=v1')
        self.assertIn('--no-optional-locks', run.call_args.args[0])

    def test_aggregate_state_handles_contextual_and_not_applicable_fields(self):
        capability = dict.fromkeys(dry.DIMENSIONS, 'LOW')
        capability['strengthRequirement'] = 'PRESCRIPTION_DEPENDENT'
        self.assertEqual(dry.aggregate(capability), 'PRESCRIPTION_DEPENDENT')
        capability['mobilityRequirement'] = 'VARIABLE'
        self.assertEqual(dry.aggregate(capability), 'PRESCRIPTION_DEPENDENT')
        capability['strengthRequirement'] = 'NOT_APPLICABLE'
        capability['mobilityRequirement'] = 'LOW'
        self.assertEqual(dry.aggregate(capability), 'KNOWN')

    def test_manifest_byte_size_mismatch_is_a_hard_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            prefix = dry.DATA + 'r5-authoritative-shards/patch/'
            manifest = {'schemaVersion': 'V3.10C-R5-R-PS-2', 'partitionAlgorithmVersion': 1,
                        'shardCount': 16, 'totalEntryCount': 0,
                        'orderingKeyRule': ['canonicalExerciseId', 'field-or-dimension', 'provenanceId'],
                        'shards': []}
            pins = {}
            for index in range(16):
                name = f'bucket-{index:02x}.json'
                path = root / (prefix + name)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b'[]\n')
                pins[prefix + name] = dry.gitblob(b'[]\n')
                manifest['shards'].append({'index': index, 'filename': name, 'entryCount': 0,
                    'byteSize': 2 if index == 4 else 3, 'gitBlobSha': dry.gitblob(b'[]\n')})
            raw = dry.json_bytes(manifest)
            (root / (prefix + 'manifest.json')).write_bytes(raw)
            pins[prefix + 'manifest.json'] = dry.gitblob(raw)
            with self.assertRaisesRegex(dry.DryRunError, 'manifest integrity mismatch'):
                dry.load_family(dry.Reader(root, pins), 'patch', 0)

    def test_checkout_guards_reject_wrong_head_dirty_tree_and_wrong_pin(self):
        root = pathlib.Path('/checkout')
        for values, message in [
            (['0' * 40], 'HEAD differs'),
            ([dry.BASELINE, str(root), ' M candidate.json'], 'not clean'),
            ([dry.BASELINE, str(root), '', '0' * 40], 'tree blob differs'),
        ]:
            with self.subTest(message=message):
                with patch.object(dry, 'git', side_effect=values):
                    with self.assertRaisesRegex(dry.DryRunError, message):
                        dry.run_diagnostic(root)

    def test_all_214_outside_decisions_include_64_unresolved_precedence_conflicts(self):
        rows, decisions, entries = [], {}, []
        for category, count in dry.EXPECTED_CATEGORIES.items():
            for index in range(count):
                cid = f'{category}-{index}'
                key = cid + '::mobilityRequirement'
                current = {'ORPHAN_NO_TARGET': None, 'PRESCRIPTION_DEPENDENT_VS_R5_SCALAR':
                           'PRESCRIPTION_DEPENDENT', 'R3_VS_R5': 'LOW', 'R3_IDENTICAL': 'HIGH'}[category]
                value = 'HIGH'
                decisions[key] = {'canonicalExerciseId': cid, 'dimension': 'mobilityRequirement',
                                  'value': value, 'provenanceId': 'pid-' + cid}
                entry = {'semanticKey': key, 'canonicalExerciseId': cid, 'field': 'mobilityRequirement',
                         'category': category, 'disposition': category, 'r5Value': value,
                         'r5ProvenanceId': 'pid-' + cid, 'r3Value': current}
                entries.append(entry)
                if current is not None:
                    row = self.row(current)
                    row['canonicalExerciseId'] = cid
                    rows.append(row)
        before = copy.deepcopy(rows)
        patch_keys = set(decisions)
        for key in sorted(key for key in decisions if key.startswith('R3_VS_R5')):
            patch_keys.remove(key)
        doc = {'schemaVersion': 'V3.10C-R5-MATERIALIZATION-DISPOSITION-1', 'total': 214,
               'counts': dry.EXPECTED_CATEGORIES, 'entries': entries}
        isolated, categories = dry.isolate_outside(rows, decisions, patch_keys, set(decisions), doc)
        self.assertEqual(rows, before)
        self.assertEqual(len(isolated), 214)
        self.assertEqual(sum(e['category'] in {'PRESCRIPTION_DEPENDENT_VS_R5_SCALAR', 'R3_VS_R5'}
                             for e in isolated), 64)
        self.assertEqual(sum(not e['hasPatch'] for e in isolated), 9)
        self.assertTrue(all(e['applied'] is False for e in isolated))
        self.assertEqual(categories, dry.EXPECTED_CATEGORIES)
        doc['entries'][0]['r5Value'] = 'fabricated'
        with self.assertRaises(dry.DryRunError):
            dry.isolate_outside(rows, decisions, patch_keys, set(decisions), doc)


if __name__ == '__main__':
    unittest.main()
