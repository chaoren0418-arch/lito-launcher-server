"""Offline tests for the #418 class identity guards (no JAR, JVM or SQL).

Synthetic class files are assembled in Python and stored in small zips under
the repository's ignored scratch/ directory.
"""
from contextlib import redirect_stderr
import hashlib
import io
import json
from pathlib import Path
import shutil
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import class_identity as ci
import regression_support as support

ROOT = support.ROOT
SCRATCH = ROOT.parents[3] / 'scratch'
NAME = 'local/lito381/server/GameStateStore.class'
OTHER = 'local/lito381/server/Other.class'
HOOK = 'local/lito381/server/DeItemTransactionProbe'


def u2(value):
    return struct.pack('>H', value)


def u4(value):
    return struct.pack('>I', value)


class Pool:
    def __init__(self):
        self.entries, self.index = [], {}

    def add(self, key, payload):
        if key not in self.index:
            self.entries.append(payload)
            self.index[key] = len(self.entries)
        return self.index[key]

    def utf8(self, text):
        raw = text.encode('utf-8')
        return self.add(('u', text), b'\x01' + u2(len(raw)) + raw)

    def cls(self, name):
        return self.add(('c', name), b'\x07' + u2(self.utf8(name)))

    def string(self, text):
        return self.add(('s', text), b'\x08' + u2(self.utf8(text)))

    def method(self, owner, name, descriptor):
        nat = self.add(('n', name, descriptor), b'\x0c' + u2(self.utf8(name)) + u2(self.utf8(descriptor)))
        return self.add(('m', owner, name, descriptor), b'\x0a' + u2(self.cls(owner)) + u2(nat))

    def bytes(self):
        return u2(len(self.entries) + 1) + b''.join(self.entries)


def make_class(text='hello', hook=False, debug=False, shuffle=False, wide=False, extra=False, value=1, this=NAME):
    """A class with run()V: ldc text; pop; [hook]; goto L; L: return  and get()I: iconst_<value>; ireturn."""
    pool = Pool()
    if shuffle:  # different constant-pool order / padding, same program
        for padding in ('zzz', 'Code', '()I', 'get', text, 'padding'):
            pool.utf8(padding)
        pool.string(text)
    this_name = this.removesuffix('.class')
    this_index, super_index = pool.cls(this_name), pool.cls('java/lang/Object')
    code_name = pool.utf8('Code')
    string = pool.string(text)
    hook_index = pool.method(HOOK, 'beforeSave', '()V') if hook else None
    lines = pool.utf8('LineNumberTable') if debug else None
    source = pool.utf8('SourceFile') if debug else None
    source_value = pool.utf8('Synthetic.java') if debug else None
    load = (b'\x13' + u2(string)) if wide else (b'\x12' + bytes([string]))
    body = load + b'\x57' + ((b'\xb8' + u2(hook_index)) if hook else b'')
    body += b'\xa7' + struct.pack('>h', 3) + b'\xb1'

    def method(name, descriptor, code, stack):
        attrs = b''
        count = 0
        if debug:
            table = u2(1) + u2(0) + u2(7)
            attrs += u2(lines) + u4(len(table)) + table
            count = 1
        payload = u2(stack) + u2(1) + u4(len(code)) + code + u2(0) + u2(count) + attrs
        return u2(0x0001) + u2(pool.utf8(name)) + u2(pool.utf8(descriptor)) + u2(1) + u2(code_name) + u4(len(payload)) + payload

    methods = [method('run', '()V', body, 1), method('get', '()I', bytes([0x03 + value]) + b'\xac', 1)]
    if extra:
        methods.append(method('extra', '()I', b'\x04\xac', 1))
    tail = u2(0x0021) + u2(this_index) + u2(super_index) + u2(0) + u2(0) + u2(len(methods)) + b''.join(methods)
    if debug:
        tail += u2(1) + u2(source) + u4(2) + u2(source_value)
    else:
        tail += u2(0)
    return b'\xca\xfe\xba\xbe' + u2(0) + u2(61) + pool.bytes() + tail


class ClassNormalizationTests(unittest.TestCase):
    def test_pool_order_debug_and_wide_ldc_are_normalized(self):
        a = ci.normalize(make_class())
        b = ci.normalize(make_class(debug=True, shuffle=True, wide=True))
        self.assertNotEqual(make_class(), make_class(debug=True, shuffle=True, wide=True))
        self.assertEqual(ci.differences(a, b), [])
        self.assertEqual(ci.normalized_sha256(a), ci.normalized_sha256(b))
        self.assertEqual(a['methods']['run()V']['attributes']['Code'][0]['instructions'],
                         ['ldc string:"hello"', 'pop', 'goto L3', 'return'])

    def test_semantic_differences_are_listed_per_method(self):
        diff = ci.differences(ci.normalize(make_class()), ci.normalize(make_class(text='bye', value=2, extra=True)))
        self.assertEqual(diff, ['method extra()I only in compared class', 'method get()I differs', 'method run()V differs'])

    def test_exact_mode_never_normalizes(self):
        entry = ci.compare(NAME, make_class(), make_class(debug=True), ci.EXACT)
        self.assertEqual((entry['identical'], entry['result']), (False, 'bytes-differ'))
        self.assertNotIn('compared_normalized_sha256', entry)

    def test_overlay_hooks_are_stripped_and_branches_relabelled(self):
        entry = ci.compare_overlay(NAME, make_class(hook=True, shuffle=True), make_class(debug=True), (HOOK,))
        self.assertTrue(entry['identical'], entry)
        self.assertEqual(entry['removed_probe_calls'], ['method:' + HOOK + '.beforeSave:()V'])
        self.assertEqual(ci.compare_overlay(NAME, make_class(), make_class(), (HOOK,))['result'], 'overlay-has-no-probe-hook')
        other = ci.compare_overlay(NAME, make_class(hook=True), make_class(extra=True), (HOOK,))
        self.assertEqual(other['result'], 'overlay-differs-beyond-probe-hooks')
        self.assertIn('method extra()I only in reference class', other['differences'])


class GuardTests(unittest.TestCase):
    def setUp(self):
        SCRATCH.mkdir(exist_ok=True)
        self.tmp = Path(tempfile.mkdtemp(prefix='test-class-identity-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.receipt = self.tmp / 'class-identity.json'

    def jar(self, label, entries):
        path = self.tmp / (label + '.jar')
        with zipfile.ZipFile(path, 'w') as z:
            for name, data in entries.items():
                z.writestr(name, data)
        return path

    def build(self, jar, mode):
        value = dict(runtime=support.pin(jar))
        if mode != ci.EXACT:
            value['class_identity'] = dict(mode=mode)
        return value

    def guard(self, base, runtime, mode, overlay=None):
        base_jar, runtime_jar = self.jar('base', {NAME: base}), self.jar('runtime', {NAME: runtime})
        with zipfile.ZipFile(base_jar) as a, zipfile.ZipFile(runtime_jar) as z:
            support.assert_class_identity(a, z, NAME, self.build(runtime_jar, mode), self.receipt, overlay)

    def overlay(self, files):
        root = self.tmp / 'memory-observer'
        for name, data in files.items():
            (root / name).parent.mkdir(parents=True, exist_ok=True)
            (root / name).write_bytes(data)
        return root

    def test_candidate_identical_bytes_pass(self):
        self.guard(make_class(), make_class(), ci.EXACT)
        self.assertFalse(self.receipt.exists())

    def test_candidate_different_bytes_fail(self):
        with self.assertRaisesRegex(AssertionError, 'exact bytes'):
            self.guard(make_class(), make_class(debug=True), ci.EXACT)

    def test_maven_normalized_identical_passes_with_receipt(self):
        base, maven = make_class(), make_class(debug=True, shuffle=True)
        self.guard(base, maven, ci.MAVEN)
        value = json.loads(self.receipt.read_text('utf-8'))
        self.assertEqual((value['class_identity_mode'], value['status']), (ci.MAVEN, 'passed'))
        entry = value['entries'][0]
        self.assertEqual(entry['result'], 'normalized-identical')
        self.assertEqual(entry['reference_sha256'], hashlib.sha256(base).hexdigest())
        self.assertEqual(entry['compared_sha256'], hashlib.sha256(maven).hexdigest())
        self.assertEqual(entry['reference_normalized_sha256'], entry['compared_normalized_sha256'])

    def test_maven_normalized_difference_fails_and_lists_methods(self):
        with self.assertRaisesRegex(AssertionError, r'normalized-differ; method get\(\)I differs'):
            self.guard(make_class(), make_class(value=2), ci.MAVEN)
        value = json.loads(self.receipt.read_text('utf-8'))
        self.assertEqual(value['status'], 'failed')
        self.assertEqual(value['entries'][0]['differences'], ['method get()I differs'])

    def test_maven_overlay_from_runtime_passes(self):
        maven = make_class(extra=True)  # Maven-only methods do not matter: the overlay is built from it
        self.guard(make_class(), maven, ci.MAVEN, self.overlay({NAME: make_class(hook=True, extra=True, shuffle=True)}))
        entry = json.loads(self.receipt.read_text('utf-8'))['entries'][0]
        self.assertEqual(entry['result'], 'runtime-class-plus-probe-hooks-only')
        self.assertEqual(entry['historical_base_comparison']['result'], 'normalized-differ')

    def test_maven_overlay_replacing_unexpected_class_fails(self):
        overlay = self.overlay({NAME: make_class(hook=True), OTHER: make_class(this=OTHER)})
        with self.assertRaisesRegex(AssertionError, 'overlay-replaces-unexpected-classes'):
            self.guard(make_class(), make_class(), ci.MAVEN, overlay)

    def test_maven_overlay_from_historical_base_fails(self):
        # An overlay that would swap the Maven class back to the historical one.
        with self.assertRaisesRegex(AssertionError, 'overlay-differs-beyond-probe-hooks'):
            self.guard(make_class(), make_class(extra=True), ci.MAVEN, self.overlay({NAME: make_class(hook=True)}))


class ComponentGuardTests(unittest.TestCase):
    """The adapted corpse/vital fixture loop keeps its own allowlist assertion."""
    ALLOW = ['local/lito381/server/Allowed.class']
    LOOP = ("for row in rows:\n"
            " name=row['name']\n"
            " if name in native.namelist():\n"
            "  " + support.COMPONENTS_GUARD + ";assert hashlib.sha256(qualified.read(name)).hexdigest()==row['sha256']\n"
            "  if changed:assert name in ALLOW\n"
            "  seen.append((name,changed))\n")

    def setUp(self):
        SCRATCH.mkdir(exist_ok=True)
        self.tmp = Path(tempfile.mkdtemp(prefix='test-class-identity-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, self.tmp, True)

    def run_loop(self, mode, historical, packaged):
        receipt = self.tmp / 'components.class-identity.json'
        text = support.adapt_components_identity(self.LOOP, mode, receipt)
        paths = []
        for label, entries in (('qualified', historical), ('native', packaged)):
            paths.append(self.tmp / (label + '.jar'))
            with zipfile.ZipFile(paths[-1], 'w') as z:
                for name, data in entries.items():
                    z.writestr(name, data)
        rows = [dict(name=n, sha256=hashlib.sha256(d).hexdigest()) for n, d in historical.items()]
        seen = []
        with zipfile.ZipFile(paths[0]) as qualified, zipfile.ZipFile(paths[1]) as native:
            exec(compile(text, 'adapted-components', 'exec'),
                 dict(rows=rows, native=native, qualified=qualified, hashlib=hashlib, Path=Path, ALLOW=self.ALLOW,
                      seen=seen, class_identity_changed=support.class_identity_changed))
        return seen, receipt

    def test_exact_mode_is_the_original_comparison(self):
        self.assertEqual(self.run_loop(ci.EXACT, {NAME: make_class()}, {NAME: make_class()})[0], [(NAME, False)])
        with self.assertRaises(AssertionError):
            self.run_loop(ci.EXACT, {NAME: make_class()}, {NAME: make_class(debug=True)})

    def test_maven_mode_accepts_normalized_identical_and_records_it(self):
        seen, receipt = self.run_loop(ci.MAVEN, {NAME: make_class()}, {NAME: make_class(debug=True, shuffle=True)})
        self.assertEqual(seen, [(NAME, False)])
        value = json.loads(receipt.read_text('utf-8'))
        self.assertEqual((value['class_identity_mode'], value['entries'][0]['result']), (ci.MAVEN, 'normalized-identical'))

    def test_maven_mode_unexpected_class_change_fails(self):
        with self.assertRaises(AssertionError):
            self.run_loop(ci.MAVEN, {NAME: make_class()}, {NAME: make_class(value=3)})
        allowed = self.ALLOW[0]
        seen, _ = self.run_loop(ci.MAVEN, {allowed: make_class(this=allowed)}, {allowed: make_class(this=allowed, value=3)})
        self.assertEqual(seen, [(allowed, True)])


class AdaptationTests(unittest.TestCase):
    def test_legacy_driver_rewrite_keeps_original_file(self):
        original = ROOT / 'cancellation-battle-runtime/test_boundaries.py'
        text = original.read_text('utf-8')
        self.assertEqual(len(support.OVERLAY_ASSERT.findall(text)), 1)
        adapted = support.adapt_driver(text, '/tmp/isolated.properties')
        self.assertEqual(support.OVERLAY_ASSERT.findall(adapted), [])
        self.assertEqual(adapted.count("assert_class_identity(a,z,'" + NAME + "',build,run/'class-identity.json',overlay)"), 1)
        self.assertEqual(adapted.count("'ObserveDeItemTransaction',overlay_source(old,jar,build),overlay)"), 1)
        compile(adapted, 'adapted', 'exec')

    def test_overlay_source_by_mode(self):
        self.assertEqual(support.overlay_source('old', 'jar', {}), 'old')
        self.assertEqual(support.overlay_source('old', 'jar', dict(class_identity=dict(mode=ci.MAVEN))), 'jar')


class OptionTests(unittest.TestCase):
    def setUp(self):
        SCRATCH.mkdir(exist_ok=True)
        self.tmp = Path(tempfile.mkdtemp(prefix='test-class-identity-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.server = self.tmp / 'res/project/server'
        self.receipt_jar = self.tmp / 'candidate.jar'
        self.receipt_jar.write_bytes(b'candidate')
        self.maven = self.tmp / 'maven.jar'
        self.maven.write_bytes(b'maven')
        build = self.server / 'tools/combat-continuation/cancellation-battle-runtime/build/build.json'
        build.parent.mkdir(parents=True)
        build.write_text(json.dumps(dict(runtime=support.pin(self.receipt_jar))), encoding='utf-8')

    def options(self, *extra):
        argv = ['x', '--resources', str(self.server), '--out', str(self.tmp / 'out'), *extra]
        with patch.object(sys, 'argv', argv):
            return support.Options()

    def test_default_is_exact_and_not_forwarded(self):
        options = self.options('--jar', str(self.maven))
        self.assertNotIn('class_identity', options.build('cancellation-battle-runtime'))
        self.assertNotIn('--class-identity', options.child_args())

    def test_maven_mode_is_recorded_and_forwarded(self):
        options = self.options('--jar', str(self.maven), '--class-identity', ci.MAVEN)
        build = options.build('cancellation-battle-runtime')
        self.assertEqual(build['class_identity']['mode'], ci.MAVEN)
        self.assertEqual(build['class_identity']['stage_receipt_runtime'], support.pin(self.receipt_jar))
        self.assertEqual(options.child_args()[-2:], ['--class-identity', ci.MAVEN])

    def test_maven_mode_refuses_the_candidate_receipt(self):
        options = self.options('--jar', str(self.receipt_jar), '--class-identity', ci.MAVEN)
        with self.assertRaisesRegex(AssertionError, 'must use --class-identity exact'):
            options.build('cancellation-battle-runtime')

    def test_unknown_mode_is_rejected(self):
        with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            self.options('--class-identity', 'loose')


if __name__ == '__main__':
    unittest.main()
