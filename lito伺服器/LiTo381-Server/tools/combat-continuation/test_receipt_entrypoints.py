"""Offline receipt tests for the #31 entry points (#341 item 11, #407).

No JAR execution, SQL, or behavioral qualification: the entry scripts run
in-process with every child driver replaced by a stub, so only the receipt
plumbing (running -> passed/failed, formal protection fields) is exercised.
Temporary trees live under the repository's ignored scratch/ directory.
"""
import ast
from contextlib import redirect_stderr
import io
import json
from pathlib import Path
import runpy
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import regression_support as support

ROOT = support.ROOT
SCRATCH = ROOT.parents[3] / 'scratch'
TARGETED = ROOT / 'cancellation-battle-runtime/replay_targeted.py'
PLAYER = ROOT / 'cancellation-battle-runtime/replay_player_cancellation.py'
CANDIDATE = ROOT / 'candidate_regression.py'
REAL_PIN = support.pin


class EntryReceiptTests(unittest.TestCase):
    def setUp(self):
        SCRATCH.mkdir(exist_ok=True)
        self.tmp = Path(tempfile.mkdtemp(prefix='test-receipt-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.server = self.tmp / 'res/project/server'
        self.root = self.server / 'tools/combat-continuation'
        build = self.root / 'cancellation-battle-runtime/build/build.json'
        build.parent.mkdir(parents=True)
        build.write_text(json.dumps(dict(status='built', runtime={})), encoding='utf-8')
        prep = self.root / 'build/integration-regression/preparation.json'
        prep.parent.mkdir(parents=True)
        prep.write_text(json.dumps(dict(asm={})), encoding='utf-8')
        self.jar = self.tmp / 'candidate.jar'
        self.jar.write_bytes(b'not a real jar')
        self.config = self.tmp / 'isolated-test.properties'
        self.config.write_text('# offline test only\n', encoding='utf-8')
        self.out = self.tmp / 'out'
        guard = support.FormalProtection(self.server.resolve())
        self.formal = guard.formal
        self.reference = guard.reference.resolve()
        self.hashes = {self.formal: support.CURRENT_FORMAL_SHA256,
                       self.reference: support.CURRENT_FORMAL_SHA256}
        pinner = patch.object(support, 'pin', side_effect=self.pin)
        pinner.start()
        self.addCleanup(pinner.stop)
        self.child_calls = 0

    def pin(self, path):
        path = Path(path).resolve()
        if path in self.hashes:
            return dict(path=str(path), sha256=self.hashes[path])
        return REAL_PIN(path)

    def argv(self, script, *extra):
        return [str(script), *extra, '--jar', str(self.jar), '--out', str(self.out),
                '--resources', str(self.server), '--config', str(self.config)]

    def run_entry(self, script, child, *extra):
        with patch.object(sys, 'argv', self.argv(script, *extra)), \
                patch.object(subprocess, 'run', side_effect=child), \
                redirect_stderr(io.StringIO()) as err:
            try:
                runpy.run_path(str(script))
            finally:
                self.stderr = err.getvalue()

    @staticmethod
    def result_path(adapted):
        """Where the adapted driver would have written result.json."""
        adapted = Path(adapted)
        if adapted.name == 'test_boundaries.py':
            return adapted.parent / 'boundaries/result.json'
        tree = ast.parse(adapted.read_text('utf-8'))
        for node in ast.walk(tree):
            if (isinstance(node, ast.Assign) and [ast.unparse(t) for t in node.targets] == ['D']
                    and isinstance(node.value, ast.Call) and ast.unparse(node.value.func) == 'Path'):
                return Path(ast.literal_eval(node.value.args[0])) / 'result.json'
        raise AssertionError('adapted driver has no D=Path(...) destination: ' + str(adapted))

    def passing_child(self, command, check=False, **_):
        self.child_calls += 1
        result = self.result_path(command[-1])
        result.parent.mkdir(parents=True, exist_ok=True)
        result.write_text(json.dumps(dict(status='passed-offline-stub', runtime=REAL_PIN(self.jar))), encoding='utf-8')
        return subprocess.CompletedProcess(command, 0)

    def failing_child(self, command, check=False, **_):
        # #341: the first probe could not reach SQL and the child exited non-zero.
        self.child_calls += 1
        raise subprocess.CalledProcessError(1, command)

    def receipt(self, suite):
        return json.loads((self.out / suite / 'receipt.json').read_text('utf-8'))

    def assert_final(self, receipt, status, changed=False):
        self.assertEqual(status, receipt['status'])
        self.assertIs(changed, receipt['formal_deployment_changed'])
        self.assertEqual(REAL_PIN(self.jar), receipt['runtime'])

    # -- the #341 scenario: child failure must leave a failed receipt, not running --
    def check_child_failure(self, script, suite):
        with self.assertRaises(subprocess.CalledProcessError):
            self.run_entry(script, self.failing_child)
        receipt = self.receipt(suite)
        self.assert_final(receipt, 'failed')
        self.assertEqual([], receipt['tests'])
        self.assertEqual('passed', receipt['formal_protection']['status'])
        self.assertEqual(1, self.child_calls)

    def test_targeted_child_failure_writes_failed_receipt(self):
        self.check_child_failure(TARGETED, 'targeted')

    def test_player_child_failure_writes_failed_receipt(self):
        self.check_child_failure(PLAYER, 'player-cancellation')

    # -- success: every stubbed driver passes, receipt ends passed --
    def check_success(self, script, suite, count):
        self.run_entry(script, self.passing_child)
        receipt = self.receipt(suite)
        self.assert_final(receipt, 'passed')
        self.assertEqual(count, len(receipt['tests']))
        self.assertEqual(count, self.child_calls)
        support.verify_formal_protection(receipt, support.FormalProtection(self.server.resolve()))
        for row in receipt['tests']:
            self.assertIn('all behavioral assertions preserved', row['change'])

    def test_targeted_success_writes_passed_receipt(self):
        # 25 stage drivers + replacement/fault (test.py, test_boundaries.py) under --out.
        self.check_success(TARGETED, 'targeted', 27)

    def test_player_success_writes_passed_receipt(self):
        self.check_success(PLAYER, 'player-cancellation', 3)

    # -- the measured protection value must win over the False default --
    def check_formal_changed(self, script, suite):
        def child(command, check=False, **kwargs):
            self.hashes[self.formal] = '0' * 64
            return self.passing_child(command, check, **kwargs)
        with self.assertRaisesRegex(AssertionError, 'Formal deployment changed'):
            self.run_entry(script, child)
        receipt = self.receipt(suite)
        self.assert_final(receipt, 'failed', changed=True)
        self.assertEqual('failed', receipt['formal_protection']['status'])

    def test_targeted_formal_change_is_recorded(self):
        self.check_formal_changed(TARGETED, 'targeted')

    def test_player_formal_change_is_recorded(self):
        self.check_formal_changed(PLAYER, 'player-cancellation')

    # -- guard refuses to start: body never runs, receipt still failed --
    def check_guard_refuses(self, script, suite):
        self.hashes[self.formal] = support.HISTORICAL_FORMAL_SHA256
        with self.assertRaisesRegex(AssertionError, 'deployment SHA256 mismatch'):
            self.run_entry(script, self.passing_child)
        receipt = self.receipt(suite)
        self.assert_final(receipt, 'failed')
        self.assertEqual(0, self.child_calls)
        self.assertEqual('failed', receipt['formal_protection']['status'])

    def test_targeted_guard_refusal_writes_failed_receipt(self):
        self.check_guard_refuses(TARGETED, 'targeted')

    def test_player_guard_refusal_writes_failed_receipt(self):
        self.check_guard_refuses(PLAYER, 'player-cancellation')

    # -- candidate reconciliation must name the real failing driver (#341 item 08) --
    def test_candidate_reconciliation_reports_root_cause(self):
        regression = self.out / 'regression'
        log = regression / 'qualification-runs/run/replay-native-de-item-state-source.py.log'

        def child(command, **_):
            self.assertTrue(command[4].endswith('full_regression.py'), command)
            log.parent.mkdir(parents=True, exist_ok=True)
            log.write_text('CommunicationsException: Communications link failure\n', encoding='utf-8')
            manifest = log.parent / 'manifest.json'
            manifest.write_text(json.dumps(dict(
                status='failed', error='frozen qualification failed: replay-native-de-item-state-source.py',
                results=[dict(name='replay-native-de-item-state-source.py', exit_code=1,
                              log=REAL_PIN(log))])), encoding='utf-8')
            (regression / 'qualification-current.json').write_text(json.dumps(dict(
                status='failed', manifest=REAL_PIN(manifest))), encoding='utf-8')
            return subprocess.CompletedProcess(command, 1)

        with self.assertRaises(AssertionError) as caught:
            self.run_entry(CANDIDATE, child, 'cancellation-battle-runtime')
        message = str(caught.exception)
        self.assertIn('Not reconcilable', message)
        self.assertIn('failed_driver=replay-native-de-item-state-source.py', message)
        self.assertIn(str(log.resolve()), message)
        self.assertIn('FULL REGRESSION FAILED before reconciliation', self.stderr)
        self.assertIn('frozen qualification failed', self.stderr)


if __name__ == '__main__':
    unittest.main()
