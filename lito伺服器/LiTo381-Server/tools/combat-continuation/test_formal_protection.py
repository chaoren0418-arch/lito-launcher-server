"""Offline guard tests only: no JAR execution, SQL, or behavioral qualification."""
import ast
from copy import deepcopy
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import regression_support as support


class FormalProtectionTests(unittest.TestCase):
    def setUp(self):
        self.options = SimpleNamespace(out=Path('isolated-output'), server=support.SERVER)
        self.guard = support.FormalProtection(self.options.server)
        self.hashes = {
            self.guard.formal: support.CURRENT_FORMAL_SHA256,
            self.guard.reference.resolve(): support.CURRENT_FORMAL_SHA256,
        }
        self.pinner = patch.object(support, 'pin', side_effect=self.pin)
        self.pinner.start()
        self.addCleanup(self.pinner.stop)

    def pin(self, path):
        path = Path(path).resolve()
        return dict(path=str(path), sha256=self.hashes[path])

    def protected_loop(self, filename, target):
        tree = ast.parse((support.ROOT / filename).read_text('utf8'))
        node = next(n for n in ast.walk(tree) if isinstance(n, ast.For) and ast.unparse(n.iter) == target)
        return compile(ast.Module(body=[node], type_ignores=[]), filename, 'exec')

    def seal_guard(self, out, extra=None):
        formal = dict(path=str(self.guard.formal), sha256=support.HISTORICAL_FORMAL_SHA256)
        refs = [formal] + ([] if extra is None else [extra])
        env = dict(old={'protected': refs}, options=SimpleNamespace(out=out),
                   guard=self.guard, pin=self.pin, Path=Path)
        exec(self.protected_loop('cancellation-battle-runtime/seal_validation.py', "old['protected']"), env)

    def reconcile_guard(self, out):
        formal = self.guard.formal
        env = dict(guards=[dict(path=str(formal), sha256='c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539')],
                   formal=formal, options=SimpleNamespace(out=out), current=[], changes=[],
                   pin=self.pin, sha=lambda p: self.pin(p)['sha256'], Path=Path)
        exec(self.protected_loop('reconcile_replay.py', 'guards'), env)
        return env['changes']

    def test_authorized_constants_are_exact(self):
        self.assertEqual('e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498', support.CURRENT_FORMAL_SHA256)
        self.assertEqual('75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371', support.HISTORICAL_FORMAL_SHA256)

    def test_default_mode_does_not_measure_or_add_fields(self):
        receipt = {}
        with patch.object(support, 'FormalProtection') as constructor:
            with support.formal_protection(SimpleNamespace(out=None), receipt) as guard:
                self.assertIsNone(guard)
            constructor.assert_not_called()
        self.assertEqual({}, receipt)

    def test_new_mode_records_equal_before_after_and_historical_value(self):
        receipt = {}
        with support.formal_protection(self.options, receipt):
            self.assertEqual('running', receipt['formal_protection']['status'])
        proof = receipt['formal_protection']
        self.assertEqual(proof['before'], proof['after'])
        self.assertEqual(support.HISTORICAL_FORMAL_SHA256, proof['historical_formal_at_candidate_qualification'])
        support.verify_formal_protection(receipt, self.guard)

    def test_new_mode_wrong_initial_deployment_fails_before_body(self):
        self.hashes[self.guard.formal] = support.HISTORICAL_FORMAL_SHA256
        receipt = {}
        with self.assertRaisesRegex(AssertionError, 'deployment SHA256 mismatch'):
            with support.formal_protection(self.options, receipt):
                self.fail('Body must not execute')
        self.assertEqual('failed', receipt['formal_protection']['status'])

    def test_new_mode_wrong_reference_fails_before_body(self):
        self.hashes[self.guard.reference.resolve()] = '0' * 64
        with self.assertRaisesRegex(AssertionError, 'reference SHA256 mismatch'):
            with support.formal_protection(self.options, {}):
                self.fail('Body must not execute')

    def test_new_mode_deployment_changes_during_body_fails(self):
        receipt = {}
        with self.assertRaisesRegex(AssertionError, 'deployment changed'):
            with support.formal_protection(self.options, receipt):
                self.hashes[self.guard.formal] = '0' * 64
        proof = receipt['formal_protection']
        self.assertEqual('failed', proof['status'])
        self.assertNotEqual(proof['before'], proof['after'])
        self.assertTrue(receipt['formal_deployment_changed'])

    def test_new_mode_reference_changes_during_body_fails(self):
        with self.assertRaisesRegex(AssertionError, 'reference changed'):
            with support.formal_protection(self.options, {}):
                self.hashes[self.guard.reference.resolve()] = '0' * 64

    def test_body_failure_still_records_after_and_propagates(self):
        receipt = {}
        with self.assertRaisesRegex(RuntimeError, 'behavior failed'):
            with support.formal_protection(self.options, receipt):
                raise RuntimeError('behavior failed')
        # This says only that the formal file was unchanged, not that behavior passed.
        support.verify_formal_protection(receipt, self.guard)

    def test_seal_rejects_missing_failed_or_inconsistent_suite_proof(self):
        receipt = {}
        with support.formal_protection(self.options, receipt):
            pass
        with self.assertRaises(KeyError):
            support.verify_formal_protection({}, self.guard)
        for key, value in [('status', 'failed'), ('expected_sha256', '0' * 64),
                           ('historical_formal_at_candidate_qualification', '0' * 64),
                           ('after', dict(path=str(self.guard.formal), sha256='0' * 64))]:
            changed = deepcopy(receipt)
            changed['formal_protection'][key] = value
            with self.subTest(key=key), self.assertRaises(AssertionError):
                support.verify_formal_protection(changed, self.guard)

    def test_legacy_seal_keeps_original_historical_sha_assertion(self):
        with self.assertRaises(AssertionError):
            self.seal_guard(None)
        self.hashes[self.guard.formal] = support.HISTORICAL_FORMAL_SHA256
        self.seal_guard(None)

    def test_legacy_reconcile_keeps_original_historical_sha_assertion(self):
        with self.assertRaises(AssertionError):
            self.reconcile_guard(None)
        self.hashes[self.guard.formal] = support.HISTORICAL_FORMAL_SHA256
        self.assertIn('current_accepted_0_6_69', self.reconcile_guard(None)[0])

    def test_new_seal_and_reconcile_accept_only_guarded_current_baseline(self):
        with support.formal_protection(self.options, {}):
            self.seal_guard(self.options.out)
            self.assertIn('current_accepted_0_6_71', self.reconcile_guard(self.options.out)[0])

    def test_other_protected_files_remain_exact(self):
        other = self.guard.formal.with_name('ddraw.dll')
        self.hashes[other] = '1' * 64
        ref = self.pin(other)
        self.seal_guard(self.options.out, ref)
        self.hashes[other] = '2' * 64
        with self.assertRaisesRegex(AssertionError, 'Historical protected file changed'):
            self.seal_guard(self.options.out, ref)

    def test_historical_formal_entry_cannot_be_missing_duplicated_or_relabelled(self):
        ref = dict(path=str(self.guard.formal), sha256=support.HISTORICAL_FORMAL_SHA256)
        self.guard.check_historical_list([ref])
        for refs in ([], [ref, ref]):
            with self.assertRaises(AssertionError):
                self.guard.check_historical_list(refs)
        with self.assertRaises(AssertionError):
            self.guard.check_historical(dict(ref, sha256=support.CURRENT_FORMAL_SHA256))


if __name__ == '__main__':
    unittest.main()
