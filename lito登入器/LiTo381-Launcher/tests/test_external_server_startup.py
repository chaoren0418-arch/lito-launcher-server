"""T140 / Issue #404: pure checks for src/external_server_startup.py (no Server, no LiTo.bin, no Windows APIs)."""
from pathlib import Path
import contextlib, copy, importlib.util, json, unittest

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


if __name__ == '__main__':
    unittest.main()
