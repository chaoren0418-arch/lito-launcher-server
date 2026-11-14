"""T140 / Issue #404: pure checks for src/external_server_startup.py (no Server, no LiTo.bin, no Windows APIs)."""
from pathlib import Path
import contextlib, copy, hashlib, importlib.util, io, json, os, sys, tempfile, unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location('external_server_startup', ROOT / 'src' / 'external_server_startup.py')
helper = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helper)

VALID = {'schemaVersion': 1, 'kind': 'external', 'profileId': 't014-p24-external', 'displayName': 'T014 P24',
         'host': '127.0.0.1', 'port': 42083, 'staticDatabase': 'p24_380db_lito_exp',
         'sources': [{'id': 1, 'name': 'P24LiToExp', 'database': 'p24_accounts_lito_exp'}]}


def changed(**values):
    value = copy.deepcopy(VALID); value.update(values); return value


class ProfileTests(unittest.TestCase):
    def test_checked_in_profile_is_valid(self):
        profile = helper.load_profile(ROOT / 'server-profiles' / 'external' / 'p24-experiment-42083.json')
        self.assertEqual(profile['port'], 42083)
        self.assertEqual(helper.expected_endpoints(profile), [('127.0.0.1', 42083), ('127.0.0.1', 42084)])

    def test_invalid_profiles_rejected(self):
        cases = [dict(host='192.168.1.10'), dict(host='0.0.0.0'), dict(host='localhost'), dict(host='::1'), dict(host='127.1'),
                 dict(port=80), dict(port=65530), dict(port='42083'), dict(kind='standard'), dict(schemaVersion=4),
                 dict(profileId='bad id'), dict(displayName=' '), dict(staticDatabase='p24-static'), dict(sources=[]),
                 dict(sources=[{'id': 9, 'name': 'A', 'database': 'db_a'}]),
                 dict(sources=[{'id': 1, 'name': 'A,B', 'database': 'db_a'}]),
                 dict(sources=[{'id': 1, 'name': 'A', 'database': 'db_a'}, {'id': 1, 'name': 'B', 'database': 'db_b'}]),
                 dict(sources=[{'id': 1, 'name': 'A', 'database': 'db_a'}, {'id': 2, 'name': 'B', 'database': 'db_a'}]),
                 dict(sources=[{'id': 1, 'name': 'A', 'database': 'p24_380db_lito_exp'}]),
                 dict(sources=[{'id': 1, 'name': 'A', 'database': 'db_a'}, {'id': 2, 'name': 'a', 'database': 'db_b'}])]
        for case in cases:
            with self.subTest(case=case), self.assertRaises(helper.ExternalProfileError):
                helper.validate_profile(changed(**case))

    def test_effective_matches_formal_select_source_inputs(self):
        profile = helper.validate_profile(changed(sources=[{'id': 1, 'name': 'A', 'database': 'db_a'},
                                                           {'id': 3, 'name': 'B', 'database': 'db_b'}]))
        effective = helper.effective_from_profile(profile)
        self.assertEqual(effective['bind'], '127.0.0.1')
        self.assertEqual(effective['login.source.port.base'], '42083')
        self.assertEqual(effective['login.classic.server.ids'], '1,3')
        self.assertEqual(effective['login.classic.server.names'], 'A,B')
        self.assertEqual(effective['database.dynamic.databases'], 'db_a,db_b')
        self.assertEqual(helper.expected_endpoints(profile), [('127.0.0.1', 42083), ('127.0.0.1', 42084), ('127.0.0.1', 42086)])

    def test_native_profile_keeps_port_and_is_loadable_by_probe_rules(self):
        profile = helper.validate_profile(VALID)
        selected = {'schemaVersion': 4, 'host': '127.0.0.1', 'port': 42084, 'profileId': 'lito-source-1'}
        native = helper.native_profile(selected, profile)
        self.assertEqual(native['port'], 42084)
        self.assertEqual(native['nativeSourcePortBase'], 42083)
        self.assertEqual(native['nativeSourceIds'], [1])
        self.assertTrue(native['localServerConfig'].endswith('.properties'))

    def test_endpoint_check_only_connects(self):
        calls = []

        @contextlib.contextmanager
        def fake(address, timeout):
            calls.append(address)
            if address[1] == 42084: raise ConnectionRefusedError('refused')
            yield object()
        result = helper.check_endpoints(helper.validate_profile(VALID), connect=fake)
        self.assertEqual(calls, [('127.0.0.1', 42083), ('127.0.0.1', 42084)])
        self.assertEqual([r['connect'] for r in result], [True, False])
        self.assertTrue(all(r['payload_bytes_sent'] == 0 for r in result))


DDRAW = b'pinned-ddraw-bytes'
DDRAW_SHA = hashlib.sha256(DDRAW).hexdigest().upper()


def snapshot(directory):
    return {p.name: (p.read_bytes(), p.stat().st_mtime_ns) for p in sorted(Path(directory).iterdir())}


class ClientCompatTests(unittest.TestCase):
    """T139 / Issue #412: the external entry only reads the (simulated) client directory."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.client = Path(self.temp.name) / 'client'
        self.client.mkdir()
        (self.client / 'litoddraw.dll').write_bytes(DDRAW)
        # The formal helper writes the ini in text mode, i.e. CRLF on Windows.
        (self.client / 'lito381-compat.ini').write_bytes(helper.compat_ini_text(200).replace('\n', '\r\n').encode('ascii'))

    def tearDown(self):
        self.temp.cleanup()

    def test_no_copy_or_rewrite_of_client_files_in_source(self):
        source = (ROOT / 'src' / 'external_server_startup.py').read_text(encoding='utf-8')
        self.assertNotIn('copyfile', source)
        self.assertNotIn('shutil', source)
        for line in source.splitlines():
            if 'CLIENT' in line:
                self.assertNotRegex(line, r"write_text|write_bytes|open\([^)]*['\"][wax]", line)

    def test_formal_pin_matches(self):
        formal = (ROOT / 'src' / 'development_startup.py').read_text(encoding='utf-8')
        self.assertIn("compat\\\\litoddraw.dll'): '" + helper.CLIENT_DDRAW_SHA + "'", formal)
        self.assertIn("'[Window]\\nWidth=640\\nHeight=480\\nScalePercent='+str(args.scale)+", formal)
        self.assertIn("'\\n[Diagnostics]\\nLogging=1\\n[NumericBreakthrough]\\nEnabled=1\\nSchema=4\\n'", formal)

    def test_same_dll_and_ini_pass_without_writing(self):
        before = snapshot(self.client)
        receipt = helper.verify_client_compat(self.client, 200, ddraw_sha=DDRAW_SHA)
        self.assertEqual(receipt['writes'], 0)
        self.assertEqual(receipt['litoddraw_dll']['sha256'], DDRAW_SHA)
        self.assertEqual(snapshot(self.client), before)

    def test_lf_ini_is_also_accepted(self):
        (self.client / 'lito381-compat.ini').write_bytes(helper.compat_ini_text(150).encode('ascii'))
        helper.verify_client_compat(self.client, 150, ddraw_sha=DDRAW_SHA)

    def test_different_dll_refused_and_untouched(self):
        (self.client / 'litoddraw.dll').write_bytes(b'other-ddraw')
        before = snapshot(self.client)
        with self.assertRaises(helper.ClientCompatMismatch) as caught:
            helper.verify_client_compat(self.client, 200, ddraw_sha=DDRAW_SHA)
        actual = hashlib.sha256(b'other-ddraw').hexdigest().upper()
        self.assertIn('litoddraw.dll', str(caught.exception))
        self.assertIn(actual, str(caught.exception))
        self.assertIn(DDRAW_SHA, str(caught.exception))
        self.assertEqual(caught.exception.differences[0]['problem'], 'sha256')
        self.assertEqual(snapshot(self.client), before)

    def test_missing_dll_refused(self):
        (self.client / 'litoddraw.dll').unlink()
        before = snapshot(self.client)
        with self.assertRaises(helper.ClientCompatMismatch) as caught:
            helper.verify_client_compat(self.client, 200, ddraw_sha=DDRAW_SHA)
        self.assertEqual(caught.exception.differences[0]['problem'], 'missing')
        self.assertEqual(snapshot(self.client), before)

    def test_ini_scale_mismatch_refused_with_current_scale(self):
        before = snapshot(self.client)
        with self.assertRaises(helper.ClientCompatMismatch) as caught:
            helper.verify_client_compat(self.client, 150, ddraw_sha=DDRAW_SHA)
        message = str(caught.exception)
        self.assertIn('ScalePercent=200', message)
        self.assertIn('200%', message)
        self.assertEqual(caught.exception.differences[0]['problem'], 'scale')
        self.assertEqual(caught.exception.differences[0]['actual_scale_percent'], '200')
        self.assertEqual(snapshot(self.client), before)

    def test_ini_other_content_refused(self):
        (self.client / 'lito381-compat.ini').write_bytes(helper.compat_ini_text(200).replace('Logging=1', 'Logging=0').encode('ascii'))
        before = snapshot(self.client)
        with self.assertRaises(helper.ClientCompatMismatch) as caught:
            helper.verify_client_compat(self.client, 200, ddraw_sha=DDRAW_SHA)
        self.assertEqual(caught.exception.differences[0]['problem'], 'content')
        self.assertEqual(snapshot(self.client), before)


class RunRootTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(self.temp.name)
        self.client = self.base / 'client'
        self.client.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def test_existing_directory_accepted(self):
        root = self.base / 'runs-here'
        root.mkdir()
        self.assertEqual(helper.resolve_run_root(str(root), self.client, is_reparse=lambda node: False), Path(os.path.abspath(root)))

    def test_relative_missing_and_blank_refused(self):
        for value in ('', '   ', 'relative/dir', str(self.base / 'missing')):
            with self.subTest(value=value), self.assertRaises(helper.RunRootError):
                helper.resolve_run_root(value, self.client, is_reparse=lambda node: False)

    def test_inside_client_refused(self):
        inside = self.client / 'runs'
        inside.mkdir()
        for value in (inside, self.client):
            with self.subTest(value=value), self.assertRaises(helper.RunRootError):
                helper.resolve_run_root(str(value), self.client, is_reparse=lambda node: False)

    def test_junction_on_path_refused(self):
        parent = self.base / 'linked'
        root = parent / 'runs'
        root.mkdir(parents=True)
        with self.assertRaises(helper.RunRootError) as caught:
            helper.resolve_run_root(str(root), self.client, is_reparse=lambda node: Path(node) == Path(os.path.abspath(parent)))
        self.assertIn('junction', str(caught.exception))

    def test_real_symlink_refused(self):
        target = self.base / 'target'
        target.mkdir()
        link = self.base / 'link'
        try:
            os.symlink(target, link, target_is_directory=True)
        except (OSError, NotImplementedError) as error:
            self.skipTest('symlink not permitted here: ' + str(error))
        with self.assertRaises(helper.RunRootError):
            helper.resolve_run_root(str(link), self.client)


class FakeFormalHelper:
    """Stand-in for development_startup: same attributes main() reads before anything is created."""

    def __init__(self, client, error=None):
        self.CLIENT = Path(client); self.DELIVERY = Path(client) / 'delivery'; self.DLL = 'dll'
        self.PINS = {self.DLL: helper.CLIENT_DDRAW_SHA}; self.error = error; self.checked = []

    def check_source(self, source):
        self.checked.append(source)
        if self.error: raise self.error


class PreStartRefusalTests(unittest.TestCase):
    """T152 / Issue #443: refusals before run creation must end with failed + finished(cleanup_verified)."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(self.temp.name)
        self.client = self.base / 'client'; self.client.mkdir()
        self.profile = self.base / 'profile.json'
        self.profile.write_text(json.dumps(VALID), encoding='utf-8')

    def tearDown(self):
        self.temp.cleanup()

    def run_main(self, fake, run_id='external-test'):
        argv = ['--serve', '--run-id', run_id, '--client-directory', str(self.client), '--scale', '200',
                '--service', 'StandardArena', '--source-id', '1', '--external-profile', str(self.profile),
                '--run-root', str(self.base)]
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = helper.main(argv, formal_helper=lambda: fake)
        events = [json.loads(line) for line in out.getvalue().splitlines() if line.strip()]
        return code, events, err.getvalue()

    def assert_refused(self, code, events, stage):
        self.assertEqual(code, 1)
        self.assertEqual([e['event'] for e in events], ['failed', 'finished'])
        self.assertEqual(events[0]['stage'], stage)
        self.assertEqual(events[0]['writes'], 0)
        self.assertTrue(events[1]['cleanup_verified'])
        self.assertFalse(events[1]['accepted'])
        self.assertFalse((self.base / 'runs').exists())
        self.assertEqual(snapshot(self.client), {})

    def test_formal_pin_mismatch_ends_with_finished_event(self):
        error = ValueError('Startup file changed: D:\\lineage開發\\0-客戶端\\TW13081901.txt')
        fake = FakeFormalHelper(self.client, error)
        code, events, stderr = self.run_main(fake)
        self.assert_refused(code, events, 'check_source')
        self.assertEqual(fake.checked, [str(self.client)])
        self.assertIn('TW13081901.txt', events[0]['reason'])
        self.assertIn('TW13081901.txt', stderr)

    def test_check_source_runs_before_client_compat(self):
        fake = FakeFormalHelper(self.client)  # check passes; client has no litoddraw.dll / ini
        code, events, _ = self.run_main(fake)
        self.assert_refused(code, events, 'client_compat')
        self.assertEqual(fake.checked, [str(self.client)])
        self.assertTrue(events[0]['differences'])

    def test_bad_run_id_and_profile_also_finish(self):
        code, events, _ = self.run_main(FakeFormalHelper(self.client), run_id='development-x')
        self.assert_refused(code, events, 'arguments')
        self.profile.write_text(json.dumps(changed(kind='formal')), encoding='utf-8')
        code, events, _ = self.run_main(FakeFormalHelper(self.client))
        self.assert_refused(code, events, 'external_profile')

    def test_formal_check_is_the_same_function_as_standard_flow(self):
        source = (ROOT / 'src' / 'external_server_startup.py').read_text(encoding='utf-8')
        formal = (ROOT / 'src' / 'development_startup.py').read_text(encoding='utf-8')
        self.assertIn('check_source(args.client_directory)', formal)
        self.assertIn('dev.check_source(args.client_directory)', source)
        # Issue #448: TW13081901.txt is no longer SHA-pinned by the formal helper.
        self.assertNotIn("\\\\0-客戶端\\\\TW13081901.txt'): '", formal)
        self.assertIn("UNPINNED = (CLIENT/'TW13081901.txt',)", formal)


def load_formal_helper():
    spec = importlib.util.spec_from_file_location('development_startup_under_test', ROOT / 'src' / 'development_startup.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)  # module level only binds kernel32 (Windows CI); nothing is started
    return module


class FormalCheckSourceTests(unittest.TestCase):
    """T153 / Issue #448: check_source ignores TW13081901.txt content but still refuses every other pinned change."""

    @classmethod
    def setUpClass(cls):
        cls.dev = load_formal_helper()

    def setUp(self):
        dev = self.dev
        self.saved = {name: getattr(dev, name) for name in ('PINS', 'UNPINNED', 'CLIENT', 'SELECTED', 'BIN_SHA', 'DELIVERY', 'DLL')}
        self.temp = tempfile.TemporaryDirectory()
        base = Path(os.path.realpath(self.temp.name))
        self.client = base / 'client'; self.client.mkdir()
        self.app = base / 'app'; self.app.mkdir()
        self.tw = self.client / 'TW13081901.txt'; self.tw.write_bytes(b'#0 original sprite table\r\n')
        (self.client / 'LiTo.bin').write_bytes(b'fake-bin')
        self.catalog = self.app / 'common_catalog.bin'; self.catalog.write_bytes(b'catalog-v1')
        self.jar = self.app / 'LiTo381-SharedResources-candidate.jar'; self.jar.write_bytes(b'jar-v1')
        dev.PINS = {self.catalog: sha(b'catalog-v1'), self.jar: sha(b'jar-v1')}
        dev.CLIENT = dev.SELECTED = self.client
        dev.UNPINNED = (self.client / 'TW13081901.txt',)
        dev.BIN_SHA = sha(b'fake-bin'); dev.DELIVERY = self.app; dev.DLL = self.app / 'litoddraw.dll'
        self.configured = []
        fake = type(sys)('world_resource_preflight')
        fake.configure = lambda client, dll: self.configured.append((client, dll))
        self.saved_module = sys.modules.get('world_resource_preflight')
        sys.modules['world_resource_preflight'] = fake

    def tearDown(self):
        for name, value in self.saved.items(): setattr(self.dev, name, value)
        if self.saved_module is None: sys.modules.pop('world_resource_preflight', None)
        else: sys.modules['world_resource_preflight'] = self.saved_module
        if str(self.app) in sys.path: sys.path.remove(str(self.app))
        self.temp.cleanup()

    def test_real_pin_table_no_longer_contains_tw_txt_but_keeps_the_rest(self):
        saved = self.saved
        self.assertFalse([p for p in saved['PINS'] if Path(p).name.lower() == 'tw13081901.txt'])
        self.assertEqual(tuple(saved['UNPINNED']), (saved['CLIENT'] / 'TW13081901.txt',))
        names = {Path(p).name for p in saved['PINS']}
        for kept in ('common_catalog.bin', 'common_catalog_manifest.json', 'litoddraw.dll',
                     'LiTo381-SharedResources-candidate.jar', 'world_resource_preflight.py'):
            self.assertIn(kept, names)
        self.assertEqual(len(saved['PINS']), 32)
        self.assertEqual(saved['BIN_SHA'], 'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292')

    def test_baseline_passes(self):
        self.dev.check_source(str(self.client))
        self.assertEqual(self.configured, [(self.client, self.app / 'litoddraw.dll')])

    def test_tw_txt_content_change_still_passes(self):
        for content in (b'#0 new sprite table with 30 more ids\r\n' * 3, b'', bytes(range(256))):
            with self.subTest(size=len(content)):
                self.tw.write_bytes(content)
                self.dev.check_source(str(self.client))

    def test_tw_txt_missing_is_refused(self):
        self.tw.unlink()
        with self.assertRaises(ValueError) as caught:
            self.dev.check_source(str(self.client))
        self.assertIn('Startup file missing', str(caught.exception))
        self.assertIn('TW13081901.txt', str(caught.exception))

    def test_tw_txt_directory_is_refused(self):
        self.tw.unlink(); self.tw.mkdir()
        with self.assertRaises(ValueError):
            self.dev.check_source(str(self.client))

    def test_other_pinned_file_change_still_refused(self):
        for path in (self.catalog, self.jar):
            with self.subTest(path=path.name):
                original = path.read_bytes()
                path.write_bytes(original + b'-changed')
                try:
                    with self.assertRaises(ValueError) as caught:
                        self.dev.check_source(str(self.client))
                    self.assertIn('Startup file changed', str(caught.exception))
                    self.assertIn(path.name, str(caught.exception))
                finally:
                    path.write_bytes(original)
        self.assertEqual(self.configured, [])

    def test_lito_bin_change_still_refused(self):
        (self.client / 'LiTo.bin').write_bytes(b'other-bin')
        with self.assertRaises(ValueError) as caught:
            self.dev.check_source(str(self.client))
        self.assertIn('LiTo.bin', str(caught.exception))

    def test_other_client_directory_still_refused(self):
        other = self.client.parent / 'other-client'; other.mkdir()
        with self.assertRaises(ValueError):
            self.dev.check_source(str(other))


def sha(data):
    return hashlib.sha256(data).hexdigest().upper()


if __name__ == '__main__':
    unittest.main()
