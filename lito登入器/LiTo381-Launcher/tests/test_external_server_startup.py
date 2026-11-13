"""T140 / Issue #404: pure checks for src/external_server_startup.py (no Server, no LiTo.bin, no Windows APIs)."""
from pathlib import Path
import contextlib, copy, hashlib, importlib.util, json, os, tempfile, unittest

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


if __name__ == '__main__':
    unittest.main()
