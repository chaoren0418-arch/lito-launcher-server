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

    # -- #435: --continue-per-case --
    def per_case_child(self, failing_folder):
        def child(command, check=False, **_):
            self.child_calls += 1
            result = self.result_path(command[-1])
            result.parent.mkdir(parents=True, exist_ok=True)
            if result.parent.name == failing_folder:
                result.write_text(json.dumps(dict(status='failed', error='boom in ' + failing_folder)), encoding='utf-8')
                raise subprocess.CalledProcessError(1, command)
            result.write_text(json.dumps(dict(status='passed-offline-stub', runtime=REAL_PIN(self.jar))), encoding='utf-8')
            return subprocess.CompletedProcess(command, 0)
        return child

    def check_per_case_failure(self, script, suite, count, folder):
        with self.assertRaises(SystemExit) as caught:
            self.run_entry(script, self.per_case_child(folder), '--continue-per-case')
        self.assertEqual(2, caught.exception.code)
        receipt = self.receipt(suite)
        self.assert_final(receipt, 'failed')
        self.assertEqual(count, self.child_calls)  # every case ran despite the failure
        self.assertEqual(('case-failures', 'completed'), (receipt['failure_kind'], receipt['entry_outcome']))
        self.assertEqual(dict(planned=count, passed=count - 1, failed=1, not_run=0, **{'blocked-prerequisite': 0}),
                         receipt['case_summary'])
        failed = [row for row in receipt['cases'] if row['status'] == 'failed']
        self.assertEqual(1, len(failed))
        self.assertTrue(failed[0]['case'].endswith('/' + folder), failed[0])
        self.assertEqual('boom in ' + folder, failed[0]['first_failure'])
        self.assertEqual(count - 1, len(receipt['tests']))
        self.assertEqual('passed', receipt['formal_protection']['status'])

    def test_targeted_per_case_runs_every_case(self):
        self.check_per_case_failure(TARGETED, 'targeted', 27, 'races')

    def test_player_per_case_runs_every_case(self):
        self.check_per_case_failure(PLAYER, 'player-cancellation', 3, 'boundaries')

    def test_per_case_all_passing_is_passed(self):
        self.run_entry(PLAYER, self.per_case_child(None), '--continue-per-case')
        receipt = self.receipt('player-cancellation')
        self.assert_final(receipt, 'passed')
        self.assertIsNone(receipt['failure_kind'])
        self.assertEqual(3, receipt['case_summary']['passed'])

    def test_per_case_entry_abort_is_not_a_case_failure(self):
        self.hashes[self.formal] = support.HISTORICAL_FORMAL_SHA256
        with self.assertRaisesRegex(AssertionError, 'deployment SHA256 mismatch'):
            self.run_entry(TARGETED, self.passing_child, '--continue-per-case')
        receipt = self.receipt('targeted')
        self.assert_final(receipt, 'failed')
        self.assertEqual(('entry-aborted', 'aborted'), (receipt['failure_kind'], receipt['entry_outcome']))
        self.assertIn('deployment SHA256 mismatch', receipt['entry_error'])
        self.assertEqual(27, receipt['case_summary']['not_run'])

    def test_default_mode_receipt_has_no_case_fields(self):
        self.run_entry(PLAYER, self.passing_child)
        self.assertNotIn('cases', self.receipt('player-cancellation'))


FULL = ROOT / 'full_regression.py'
AFFECTED = ROOT / 'affected_regressions.py'
LIMITS = ('-Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 '
          '-Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')


class GroupEntryTests(EntryReceiptTests):
    """existing-20 / affected-18 with stub drivers under --resources (#435)."""

    def setUp(self):
        super().setUp()
        self.build_dir = self.server / 'tools/resource-database/build'
        self.build_dir.mkdir(parents=True)
        self.regression = self.out / 'regression'

    def stub_existing(self):
        text = Path(FULL).read_text('utf-8')
        names = ast.literal_eval(text[text.index('names=[') + 6:text.index(']', text.index('names=[')) + 1])
        for index, name in enumerate(names):
            body = ("D=B/'de-item-state-development'" if index < 12 else "D=B/'de-player-action-development'") + '\n'
            if name == support.COMPONENTS_DRIVER:
                body += support.COMPONENTS_GUARD + '\n'
            (self.build_dir / name).write_text(body + '# ' + LIMITS + '\n', encoding='utf-8')
        return names

    def existing_child(self, failing=(), writes_replay=True):
        def child(command, **_):
            self.child_calls += 1
            name = Path(command[-1]).name
            self.ran.append(name)
            if name == 'replay-native-de-item-state.py' and writes_replay and name not in failing:
                (self.regression / 'replay-current.json').write_text('{}', encoding='utf-8')
            return subprocess.CompletedProcess(command, 1 if name in failing else 0)
        return child

    def run_existing(self, child):
        self.ran = []
        with self.assertRaises(SystemExit) as caught:
            self.run_entry(FULL, child, '--continue-per-case')
        self.assertEqual(2, caught.exception.code)
        current = json.loads((self.regression / 'qualification-current.json').read_text('utf-8'))
        return json.loads(Path(current['manifest']['path']).read_text('utf-8'))

    def test_existing_19_20_blocked_when_existing_11_fails(self):
        names = self.stub_existing()
        manifest = self.run_existing(self.existing_child(failing={'replay-native-de-item-state.py'}))
        self.assertEqual('failed', manifest['status'])
        self.assertEqual(18, self.child_calls)
        self.assertNotIn('replay-player-action-main.py', self.ran)
        blocked = [row for row in manifest['cases'] if row['status'] == 'blocked-prerequisite']
        self.assertEqual(['replay-player-action-main-interrupt.py', 'replay-player-action-main.py'],
                         [row['case'] for row in blocked])
        self.assertIn('replay-native-de-item-state.py', blocked[0]['hint'])
        self.assertEqual(dict(planned=20, passed=17, failed=1, not_run=0, **{'blocked-prerequisite': 2}),
                         manifest['case_summary'])
        self.assertEqual(len(names), 20)

    def test_existing_19_20_blocked_without_replay_current(self):
        self.stub_existing()
        manifest = self.run_existing(self.existing_child(writes_replay=False))
        self.assertEqual(2, manifest['case_summary']['blocked-prerequisite'])
        self.assertEqual('case-failures', manifest['failure_kind'])

    def test_existing_all_cases_with_prerequisite(self):
        self.stub_existing()
        self.ran = []
        self.run_entry(FULL, self.existing_child(), '--continue-per-case')
        self.assertEqual(20, self.child_calls)
        self.assertLess(self.ran.index('replay-native-de-item-state.py'), self.ran.index('replay-player-action-main.py'))
        current = json.loads((self.regression / 'qualification-current.json').read_text('utf-8'))
        self.assertTrue(current['status'].startswith('passed-'))

    def stub_affected(self, existing_status):
        text = Path(AFFECTED).read_text('utf-8')
        start = text.index('names=[')
        names = ast.literal_eval(text[start + 6:text.index(']', start) + 1])
        header = ("S=Path(__file__).resolve().parents[3];B=Path(__file__).resolve().parent;"
                  "D=B/'de-native-item-weapon-clock-development'\n")
        for name in names:
            (self.build_dir / name).write_text(header, encoding='utf-8')
        self.regression.mkdir(parents=True, exist_ok=True)
        (self.regression / 'qualification-current.json').write_text(json.dumps(dict(status=existing_status)),
                                                                    encoding='utf-8')
        return names

    def affected_receipt(self):
        current = json.loads((self.regression / 'affected-current.json').read_text('utf-8'))
        return json.loads(Path(current['manifest']['path']).read_text('utf-8'))

    def test_affected_default_keeps_the_existing_qualification_assertion(self):
        self.stub_affected('failed')
        with self.assertRaises(AssertionError):
            self.run_entry(AFFECTED, self.passing_child)
        self.assertEqual(0, self.child_calls)

    def test_affected_per_case_records_failed_prerequisite(self):
        names = self.stub_affected('failed')
        with self.assertRaises(support.PrerequisiteFailed):
            self.run_entry(AFFECTED, lambda command, **_: (setattr(self, 'child_calls', self.child_calls + 1),
                                                           subprocess.CompletedProcess(command, 0))[1],
                           '--continue-per-case')
        self.assertEqual(len(names), self.child_calls)
        receipt = self.affected_receipt()
        self.assertEqual(('failed', 'entry-prerequisite-failed', 'completed'),
                         (receipt['status'], receipt['failure_kind'], receipt['entry_outcome']))
        self.assertEqual([dict(name='existing-20 qualification', status='failed', satisfied=False)],
                         receipt['entry_prerequisites'])
        self.assertEqual(len(names), receipt['case_summary']['passed'])

    def test_affected_per_case_case_failure(self):
        names = self.stub_affected('passed-x')
        failing = names[3]

        def child(command, **_):
            self.child_calls += 1
            return subprocess.CompletedProcess(command, 1 if Path(command[-1]).name == failing else 0)
        with self.assertRaises(SystemExit):
            self.run_entry(AFFECTED, child, '--continue-per-case')
        receipt = self.affected_receipt()
        self.assertEqual(len(names), self.child_calls)
        self.assertEqual(('case-failures', 1), (receipt['failure_kind'], receipt['case_summary']['failed']))
        self.assertEqual(failing, [row['case'] for row in receipt['cases'] if row['status'] == 'failed'][0])


# Inherited EntryReceiptTests cases run once, in EntryReceiptTests only.
for _name in [n for n in dir(EntryReceiptTests) if n.startswith('test_')]:
    setattr(GroupEntryTests, _name, None)


class CaseRunnerTests(unittest.TestCase):
    @staticmethod
    def fail(message='broken'):
        def body():
            raise AssertionError(message)
        return body

    def test_default_mode_stops_at_the_first_failure(self):
        cases = support.CaseRunner(False, ['a', 'b'])
        with self.assertRaisesRegex(AssertionError, 'broken'):
            cases.run('a', self.fail())
        self.assertEqual({}, cases.fields(AssertionError('broken')))
        cases.finish()
        self.assertEqual([], cases.cases)

    def test_per_case_mode_records_and_continues(self):
        ran = []
        cases = support.CaseRunner(True, ['a', 'b', 'c'])
        cases.run('a', lambda: ran.append('a'))
        self.assertIsNone(cases.run('b', self.fail('b broke')))
        cases.run('c', lambda: ran.append('c'))
        self.assertEqual(['a', 'c'], ran)
        with self.assertRaises(support.CaseFailures) as caught:
            cases.finish()
        self.assertEqual(2, caught.exception.code)
        fields = cases.fields(caught.exception)
        self.assertEqual(('case-failures', 'completed'), (fields['failure_kind'], fields['entry_outcome']))
        self.assertEqual('AssertionError: b broke', fields['cases'][1]['first_failure'])
        self.assertEqual(dict(planned=3, passed=2, failed=1, not_run=0, **{'blocked-prerequisite': 0}),
                         fields['case_summary'])

    def test_evidence_message_preferred(self):
        cases = support.CaseRunner(True, ['a'])
        cases.run('a', self.fail(), lambda since: 'assert in result.json')
        self.assertEqual('assert in result.json', cases.cases[0]['first_failure'])

    def test_interrupt_and_outside_errors_abort_the_entry(self):
        cases = support.CaseRunner(True, ['a', 'b'])

        def interrupt():
            raise KeyboardInterrupt
        with self.assertRaises(KeyboardInterrupt):
            cases.run('a', interrupt)
        fields = cases.fields(KeyboardInterrupt())
        self.assertEqual(('entry-aborted', 'aborted'), (fields['failure_kind'], fields['entry_outcome']))
        self.assertEqual(2, fields['case_summary']['not_run'])

    def test_blocked_case_never_passes(self):
        cases = support.CaseRunner(True, ['a', 'b'])
        cases.run('a', lambda: None)
        cases.block('b', 'producer failed', 'run the producer first')
        with self.assertRaises(support.CaseFailures):
            cases.finish()
        self.assertEqual('blocked-prerequisite', cases.status('b'))
        self.assertEqual('entry-prerequisite-failed', cases.fields(support.PrerequisiteFailed('x'))['failure_kind'])

    def test_failure_helpers(self):
        tmp = Path(tempfile.mkdtemp(prefix='test-case-runner-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, tmp, True)
        log = tmp / 'case.log'
        log.write_text('RUN x\nTraceback\nAssertionError: wanted 8\nfooter\n', encoding='utf-8')
        self.assertEqual('AssertionError: wanted 8', support.log_failure(log))
        self.assertIsNone(support.log_failure(log, since=log.stat().st_mtime + 60))
        self.assertIsNone(support.log_failure(tmp / 'missing.log'))
        result = tmp / 'result.json'
        result.write_text(json.dumps(dict(status='failed', error='cases 7 != 8')), encoding='utf-8')
        self.assertEqual('cases 7 != 8', support.result_failure(result))
        result.write_text(json.dumps(dict(status='passed-x', error='ignored')), encoding='utf-8')
        self.assertIsNone(support.result_failure(result))


if __name__ == '__main__':
    unittest.main()
