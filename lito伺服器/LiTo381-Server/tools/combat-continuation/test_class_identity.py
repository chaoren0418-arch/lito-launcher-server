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


def make_class(text='hello', hook=False, debug=False, shuffle=False, wide=False, extra=False, value=1, this=NAME,
               inner=(), hook_name='beforeSave'):
    """A class with run()V: ldc text; pop; [hook]; goto L; L: return  and get()I: iconst_<value>; ireturn.

    ``inner`` rows (inner, outer, simple name, flags) form an InnerClasses attribute, in the given order.
    """
    pool = Pool()
    if shuffle:  # different constant-pool order / padding, same program
        for padding in ('zzz', 'Code', '()I', 'get', text, 'padding'):
            pool.utf8(padding)
        pool.string(text)
    this_name = this.removesuffix('.class')
    this_index, super_index = pool.cls(this_name), pool.cls('java/lang/Object')
    code_name = pool.utf8('Code')
    string = pool.string(text)
    hook_index = pool.method(HOOK, hook_name, '()V') if hook else None
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
    attributes = []
    if debug:
        attributes.append(u2(source) + u4(2) + u2(source_value))
    if inner:
        table = u2(len(inner)) + b''.join(u2(pool.cls(i)) + u2(pool.cls(o)) + u2(pool.utf8(s)) + u2(f) for i, o, s, f in inner)
        attributes.append(u2(pool.utf8('InnerClasses')) + u4(len(table)) + table)
    tail += u2(len(attributes)) + b''.join(attributes)
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


KERNEL = 'local/lito381/server/NativeDeInitializerKernel'
KNAME = KERNEL + '$Name'
ACTOR = KERNEL + '$Actor'
SERVICES = 'local/lito381/server/NativeDeCachedFactoryServices.class'


class Jar(dict):
    """Minimal runtime stand-in: read() raises KeyError for a missing entry, like ZipFile."""
    read = dict.__getitem__


def services(name_flags, actor_flags=0x0009, reverse=False, extra_row=False, **kwargs):
    rows = [(KNAME, KERNEL, 'Name', name_flags), (ACTOR, KERNEL, 'Actor', actor_flags)]
    if extra_row:
        rows.append((KERNEL + '$Extra', KERNEL, 'Extra', 0x0008))
    return make_class(this=SERVICES, inner=rows[::-1] if reverse else rows, **kwargs)


def name_class(flags):
    """The inner class itself; its own InnerClasses row carries its declared flags."""
    return make_class(this=KNAME + '.class', inner=[(KNAME, KERNEL, 'Name', flags)])


class InnerClassFlagTests(unittest.TestCase):
    """#428 D2: historical 0x18 (static final) vs Maven 0x08 (static) for NativeDeInitializerKernel$Name."""
    HISTORICAL = services(0x0018)

    def compare(self, compared, runtime_name_flags=0x0008, runtime=True, reference=None):
        jar = Jar({SERVICES: compared})
        if runtime_name_flags is not None:
            jar[KNAME + '.class'] = name_class(runtime_name_flags)
        return ci.compare(SERVICES, reference or self.HISTORICAL, compared, ci.MAVEN, runtime=jar if runtime else None)

    def test_final_bit_of_another_class_matching_the_runtime_passes(self):
        entry = self.compare(services(0x0008, reverse=True, debug=True, shuffle=True))
        self.assertTrue(entry['identical'], entry)
        self.assertEqual(entry['result'], 'normalized-identical-inner-class-flags')
        self.assertEqual(entry['inner_class_flag_evidence'], [dict(
            inner_class='class:' + KNAME, outer_class='class:' + KERNEL, simple_name='utf8:Name',
            reference_flags=0x0018, compared_flags=0x0008, runtime_declared_flags=0x0008)])
        self.assertNotEqual(entry['reference_normalized_sha256'], entry['compared_normalized_sha256'])

    def test_table_order_alone_stays_plain_normalized_identity(self):
        entry = self.compare(services(0x0018, reverse=True), runtime=False)
        self.assertEqual(entry['result'], 'normalized-identical')
        self.assertNotIn('inner_class_flag_evidence', entry)

    def test_exact_mode_is_unaffected(self):
        entry = ci.compare(SERVICES, self.HISTORICAL, services(0x0008), ci.EXACT, runtime=Jar())
        self.assertEqual((entry['identical'], entry['result']), (False, 'bytes-differ'))

    def assert_rejected(self, entry, reason):
        self.assertFalse(entry['identical'], entry)
        self.assertEqual(entry['result'], 'normalized-differ')
        self.assertIn('class attribute InnerClasses', entry['differences'])
        self.assertIn(reason, entry.get('inner_class_flag_check', ''))

    def test_without_runtime_evidence_fails(self):
        self.assert_rejected(self.compare(services(0x0008), runtime=False), 'no runtime')

    def test_runtime_inner_class_missing_fails(self):
        self.assert_rejected(self.compare(services(0x0008), runtime_name_flags=None), 'declares nothing')

    def test_row_not_matching_the_runtime_declaration_fails(self):
        # The compared row claims non-final but the shipped Name class is still final.
        self.assert_rejected(self.compare(services(0x0008), runtime_name_flags=0x0018), 'declares 0x0018')

    def test_visibility_change_fails(self):
        self.assert_rejected(self.compare(services(0x0018, actor_flags=0x0008), runtime_name_flags=0x0018),
                             'beyond ACC_FINAL')
        self.assert_rejected(self.compare(services(0x0009), runtime_name_flags=0x0009), 'beyond ACC_FINAL')
        self.assert_rejected(self.compare(services(0x0002 | 0x0008), runtime_name_flags=0x000a), 'beyond ACC_FINAL')

    def test_different_inner_class_set_fails(self):
        self.assert_rejected(self.compare(services(0x0018, extra_row=True)), 'inner class set differs')

    def test_own_row_flags_never_tolerated(self):
        # NativeDeInitializerKernel$Name itself: its own declaration really changed.
        own_old, own_new = name_class(0x0018), name_class(0x0008)
        entry = ci.compare(KNAME + '.class', own_old, own_new, ci.MAVEN, runtime=Jar({KNAME + '.class': own_new}))
        self.assert_rejected(entry, 'own InnerClasses flags differ')

    def test_inner_flag_change_plus_code_change_fails(self):
        entry = self.compare(services(0x0008, value=2))
        self.assertFalse(entry['identical'])
        self.assertEqual(entry['differences'], ['class attribute InnerClasses', 'method get()I differs'])
        self.assertNotIn('inner_class_flag_check', entry)


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

    def test_maven_mode_inner_final_flag_checked_against_packaged_inner_class(self):
        # #428 D2: the guard copy passes the runtime ZipFile, so the flag-only row is verified there.
        historical = {SERVICES: services(0x0018)}
        seen, receipt = self.run_loop(ci.MAVEN, historical, {SERVICES: services(0x0008), KNAME + '.class': name_class(0x0008)})
        self.assertEqual(seen, [(SERVICES, False)])
        entry = json.loads(receipt.read_text('utf-8'))['entries'][0]
        self.assertEqual(entry['result'], 'normalized-identical-inner-class-flags')
        with self.assertRaises(AssertionError):  # shipped Name still final: the row is not its representation
            self.run_loop(ci.MAVEN, historical, {SERVICES: services(0x0008), KNAME + '.class': name_class(0x0018)})
        with self.assertRaises(AssertionError):
            self.run_loop(ci.EXACT, historical, {SERVICES: services(0x0008), KNAME + '.class': name_class(0x0008)})


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


# ---------------------------------------------------------------- #435 / T149
INVENTORY = 'local/lito381/server/RuntimeInventory'
PREFLIGHT = 'local/lito381/server/NativeCastMaterialPreflightProbe'
AFTER_COPY = '(L' + INVENTORY + ';L' + INVENTORY + ';)L' + INVENTORY + ';'


def inventory_class(hook=False, receiver=True, extra_load=False):
    """RuntimeInventory with copy(): aload_0; [aload_0;] [invokestatic afterCopy]; areturn."""
    pool = Pool()
    this_index, super_index = pool.cls(INVENTORY), pool.cls('java/lang/Object')
    code_name = pool.utf8('Code')
    hook_index = pool.method(PREFLIGHT, 'afterCopy', AFTER_COPY) if hook else None
    body = b'\x2a'
    if hook:
        body += (b'\x2a' if receiver else b'') + b'\xb8' + u2(hook_index)
    if extra_load:
        body += b'\x2a\x57'  # aload_0; pop: an unrelated change next to the hook
    body += b'\xb0'
    payload = u2(2) + u2(1) + u4(len(body)) + body + u2(0) + u2(0)
    method = (u2(0x0001) + u2(pool.utf8('copy')) + u2(pool.utf8('()L' + INVENTORY + ';')) + u2(1) + u2(code_name)
              + u4(len(payload)) + payload)
    tail = u2(0x0021) + u2(this_index) + u2(super_index) + u2(0) + u2(0) + u2(1) + method + u2(0)
    return b'\xca\xfe\xba\xbe' + u2(0) + u2(61) + pool.bytes() + tail


class ReceiverHookTests(unittest.TestCase):
    """ObserveNativeMaterialPreflight inserts ``aload_0; invokestatic afterCopy`` before copy()'s areturn."""
    HOOKS = support.OBSERVERS['ObserveNativeMaterialPreflight']['receiver_hooks']

    def compare(self, overlay, hooks=None):
        return ci.compare_overlay(INVENTORY + '.class', overlay, inventory_class(), (PREFLIGHT,),
                                  self.HOOKS if hooks is None else hooks)

    def test_receiver_load_is_part_of_the_hook(self):
        entry = self.compare(inventory_class(hook=True))
        self.assertTrue(entry['identical'], entry)
        self.assertEqual(entry['removed_probe_calls'], ['aload_0', 'method:' + PREFLIGHT + '.afterCopy:' + AFTER_COPY])

    def test_undeclared_receiver_load_stays_a_difference(self):
        entry = self.compare(inventory_class(hook=True), hooks=())
        self.assertEqual(entry['result'], 'overlay-differs-beyond-probe-hooks')

    def test_receiver_hook_without_its_load_is_rejected(self):
        # The method's own aload_0 would be consumed as the hook's: the remaining code no longer matches.
        entry = self.compare(inventory_class(hook=True, receiver=False))
        self.assertFalse(entry['identical'], entry)
        self.assertEqual(entry['result'], 'overlay-differs-beyond-probe-hooks')

    def test_other_change_beside_the_hook_fails(self):
        self.assertEqual(self.compare(inventory_class(hook=True, extra_load=True))['result'],
                         'overlay-differs-beyond-probe-hooks')


# Synthetic driver texts with the exact guard shapes of the #433 drivers.
RUNTIME_DRIVER = (
    "with log.open('w')as out:\n"
    "  overlay=run/'memory-observer'\n"
    "  with zipfile.ZipFile(old)as a,zipfile.ZipFile(jar)as b:assert a.read('" + NAME + "')==b.read('" + NAME + "')\n"
    "  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',old,overlay]);invoke([J/'javac.exe'])\n"
    "  for mode,runtime in [('before',old),('after',jar)]:\n"
    "   branch=run/mode;runtimecp=str(classes)+';'+str(overlay)+';'+cp;invoke([J/'java.exe','-cp',runtimecp])\n"
    "   assert result['cases']==8\n")
LIST_DRIVER = ("with zipfile.ZipFile(Path(parent['runtime']['path']))as old,zipfile.ZipFile(jar)as new:\n"
               " for name in ['local/lito381/server/Main.class','local/lito381/server/SourceLogin.class']:"
               "assert old.read(name)==new.read(name)\n")
FROZEN_DRIVER = ("with zipfile.ZipFile(jar)as z:\n"
                 " with zipfile.ZipFile(Path(parent['runtime']['path']))as prior:assert prior.read('" + NAME
                 + "')==z.read('" + NAME + "')\n")


class GuardAdaptationTests(unittest.TestCase):
    RECEIPT = Path('/tmp/run/driver.class-identity.json')

    def adapt(self, name, text, mode=ci.MAVEN):
        return support.adapt_class_guards(name, text, mode, self.RECEIPT)

    def test_exact_mode_leaves_every_driver_verbatim(self):
        for text in (RUNTIME_DRIVER, LIST_DRIVER, FROZEN_DRIVER):
            self.assertEqual(self.adapt('probe-player-action-authority.py', text, ci.EXACT), text)

    def test_runtime_overlay_driver(self):
        adapted = self.adapt('timed-weapon-probe-native-cast-material-fault.py', RUNTIME_DRIVER)
        compile(support.adapt_driver(adapted, '/tmp/isolated.properties'), 'adapted', 'exec')
        self.assertNotIn('assert a.read', adapted)
        self.assertIn("deferred_class_guard('" + NAME + "')", adapted)
        self.assertIn("for source,target in runtime_overlay_jobs(old,jar,overlay)];"
                      "assert_runtime_overlays(old,jar,overlay,'ObserveDeItemTransaction',Path(", adapted)
        self.assertIn("str(runtime_overlay(runtime,old,jar,overlay))", adapted)
        self.assertIn("assert result['cases']==8", adapted)  # behavioral assertions verbatim

    def test_list_and_frozen_guards(self):
        listed = self.adapt('replay-player-action-main-owned.py', LIST_DRIVER)
        self.assertIn('assert_normalized_class(old,new,name,Path(', listed)
        frozen = self.adapt('replay-player-action-transaction.py', FROZEN_DRIVER)
        self.assertIn("regenerate_frozen_overlay(prior,z,overlay,jar,'ObserveDeItemTransaction'", frozen)
        self.assertIn(support.OBSERVE_DE_ITEM_TRANSACTION_SHA256, frozen)
        for text in (listed, frozen):
            compile(support.adapt_driver(text, '/tmp/isolated.properties'), 'adapted', 'exec')

    def test_known_driver_without_its_guard_fails(self):
        with self.assertRaisesRegex(AssertionError, 'Expected one known class guard'):
            self.adapt('probe-player-action-authority.py', 'print(1)\n')
        self.assertEqual(self.adapt('other-driver.py', 'print(1)\n'), 'print(1)\n')

    def test_unknown_observer_or_ambiguous_guard_fails(self):
        with self.assertRaisesRegex(AssertionError, 'Unknown observer'):
            self.adapt('x.py', RUNTIME_DRIVER.replace('ObserveDeItemTransaction', 'ObserveSomethingElse'))
        with self.assertRaisesRegex(AssertionError, 'Ambiguous'):
            self.adapt('x.py', LIST_DRIVER + LIST_DRIVER)
        with self.assertRaisesRegex(AssertionError, 'per-branch overlay'):
            self.adapt('x.py', RUNTIME_DRIVER.replace("str(overlay)+';'+cp", "str(overlay)+cp"))


class TmpJarCase(unittest.TestCase):
    """Scratch directory, receipt path and zip helper (no tests of its own)."""

    def setUp(self):
        SCRATCH.mkdir(exist_ok=True)
        self.tmp = Path(tempfile.mkdtemp(prefix='test-class-identity-', dir=SCRATCH))
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.receipt = self.tmp / 'class-identity.json'

    jar = GuardTests.jar


class RuntimeOverlayTests(TmpJarCase):
    """Parent and JAR under test each run with an overlay of their own class (#435)."""
    PARENT, MAVEN = make_class(), make_class(extra=True, debug=True, shuffle=True)

    def jars(self):
        return self.jar('old', {NAME: self.PARENT}), self.jar('new', {NAME: self.MAVEN})

    def overlays(self, runtime, parent, extra=None):
        root = self.tmp / 'memory-observer'
        for directory, data in ((root, runtime), (support.parent_overlay(root), parent)):
            if data is not None:
                (directory / NAME).parent.mkdir(parents=True, exist_ok=True)
                (directory / NAME).write_bytes(data)
        if extra:
            (root / OTHER).parent.mkdir(parents=True, exist_ok=True)
            (root / OTHER).write_bytes(extra)
        return root

    def check(self, runtime, parent, extra=None):
        old, new = self.jars()
        support.assert_runtime_overlays(old, new, self.overlays(runtime, parent, extra), 'ObserveDeItemTransaction',
                                        self.receipt)
        return json.loads(self.receipt.read_text('utf-8'))['entries'][-1]

    def test_each_runtime_with_its_own_overlay_passes(self):
        entry = self.check(make_class(hook=True, extra=True), make_class(hook=True, shuffle=True))
        self.assertEqual(entry['result'], 'runtime-overlays-hooks-only')
        self.assertTrue(entry['same_probe_hooks'])
        self.assertEqual(entry['historical_base_comparison'][0]['result'], 'normalized-differ')  # recorded only
        self.assertEqual(json.loads(self.receipt.read_text('utf-8'))['status'], 'passed')

    def test_historical_overlay_swapped_into_the_maven_runtime_fails(self):
        with self.assertRaisesRegex(AssertionError, 'runtime-overlay: .*overlay-differs-beyond-probe-hooks'):
            self.check(make_class(hook=True), make_class(hook=True))

    def test_parent_overlay_from_the_wrong_class_fails(self):
        with self.assertRaisesRegex(AssertionError, 'parent-overlay'):
            self.check(make_class(hook=True, extra=True), make_class(hook=True, extra=True))

    def test_missing_parent_overlay_or_extra_class_fails(self):
        with self.assertRaisesRegex(AssertionError, 'parent-overlay'):
            self.check(make_class(hook=True, extra=True), None)
        with self.assertRaisesRegex(AssertionError, 'runtime-overlay'):
            self.check(make_class(hook=True, extra=True), make_class(hook=True), extra=make_class(this=OTHER))

    def test_overlays_without_the_same_hooks_fail(self):
        with self.assertRaisesRegex(AssertionError, 'parent-overlay: .*overlay-has-no-probe-hook'):
            self.check(make_class(hook=True, extra=True), make_class())
        with self.assertRaisesRegex(AssertionError, 'probe hooks differ between overlays'):
            self.check(make_class(hook=True, extra=True), make_class(hook=True, hook_name='beforeFlush'))

    def test_branch_overlay_selection(self):
        old, new = self.jars()
        root = self.tmp / 'memory-observer'
        self.assertEqual(support.runtime_overlay(new, old, new, root), root)
        self.assertEqual(support.runtime_overlay(old, old, new, root), support.parent_overlay(root))
        with self.assertRaisesRegex(AssertionError, 'No observer overlay'):
            support.runtime_overlay(self.tmp / 'other.jar', old, new, root)
        self.assertEqual(support.runtime_overlay_jobs(old, new, root), [(new, root), (old, support.parent_overlay(root))])


class NormalizedListGuardTests(TmpJarCase):
    """existing 15: Main/SourceLogin compared normalized; any code difference still fails."""

    def run_guard(self, base, runtime):
        a, z = self.jar('old', {NAME: base}), self.jar('new', {NAME: runtime})
        with zipfile.ZipFile(a) as old, zipfile.ZipFile(z) as new:
            support.assert_normalized_class(old, new, NAME, self.receipt)

    def test_representation_only_passes(self):
        self.run_guard(make_class(), make_class(debug=True, shuffle=True, wide=True))
        self.assertEqual(json.loads(self.receipt.read_text('utf-8'))['entries'][0]['result'], 'normalized-identical')

    def test_code_difference_fails(self):
        with self.assertRaisesRegex(AssertionError, r'normalized-differ; method get\(\)I differs'):
            self.run_guard(make_class(), make_class(value=2))
        self.assertEqual(json.loads(self.receipt.read_text('utf-8'))['status'], 'failed')


class FrozenOverlayTests(TmpJarCase):
    """existing 16 / composed Fault+Clock: frozen parent overlay regenerated from the JAR under test."""

    def setUp(self):
        super().setUp()
        self.source = self.tmp / 'ObserveDeItemTransaction.java'
        self.source.write_text('// observer source stand-in\n', encoding='utf-8')
        self.calls = []

    def regenerate(self, frozen, written, sha=None, parent=None, runtime=None):
        overlay = self.tmp / 'memory-observer'
        (overlay / NAME).parent.mkdir(parents=True, exist_ok=True)
        (overlay / NAME).write_bytes(frozen)
        old = self.jar('old', {NAME: parent or make_class()})
        new = self.jar('new', {NAME: runtime or make_class(extra=True)})

        def writer(observer, source, asm, runtime_jar, target, work):
            self.calls.append((observer, Path(runtime_jar)))
            (Path(target) / NAME).parent.mkdir(parents=True, exist_ok=True)
            (Path(target) / NAME).write_bytes(written)

        with patch.object(support, 'write_observer_overlay', side_effect=writer), \
                zipfile.ZipFile(old) as prior, zipfile.ZipFile(new) as z:
            support.regenerate_frozen_overlay(prior, z, overlay, new, 'ObserveDeItemTransaction', self.source, [],
                                              self.tmp / 'tools', self.receipt, sha)
        return overlay, json.loads(self.receipt.read_text('utf-8'))['entries'][-1]

    def test_frozen_overlay_is_replaced_by_the_runtime_overlay(self):
        overlay, entry = self.regenerate(make_class(hook=True), make_class(hook=True, extra=True),
                                         sha=hashlib.sha256(self.source.read_bytes()).hexdigest())
        self.assertEqual(entry['result'], 'regenerated-overlay-hooks-only')
        self.assertEqual((overlay / NAME).read_bytes(), make_class(hook=True, extra=True))
        self.assertEqual((Path(str(overlay) + '-historical') / NAME).read_bytes(), make_class(hook=True))
        self.assertEqual(len(self.calls), 1)

    def test_frozen_overlay_that_is_not_the_parent_plus_hooks_fails_before_regeneration(self):
        with self.assertRaisesRegex(AssertionError, 'frozen overlay is not the parent class plus hooks'):
            self.regenerate(make_class(hook=True, value=2), make_class(hook=True, extra=True))
        self.assertEqual(self.calls, [])

    def test_regenerated_overlay_that_swaps_the_class_fails(self):
        with self.assertRaisesRegex(AssertionError, 'regenerated overlay: .*overlay-differs-beyond-probe-hooks'):
            self.regenerate(make_class(hook=True), make_class(hook=True))

    def test_regenerated_overlay_without_hooks_fails(self):
        with self.assertRaisesRegex(AssertionError, 'overlay-has-no-probe-hook'):
            self.regenerate(make_class(hook=True), make_class(extra=True))

    def test_other_observer_source_fails(self):
        with self.assertRaisesRegex(AssertionError, 'observer source is not the one'):
            self.regenerate(make_class(hook=True), make_class(hook=True, extra=True), sha='0' * 64)
        self.assertEqual(self.calls, [])


if __name__ == '__main__':
    unittest.main()
