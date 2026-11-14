"""T155 / Issue #453: app/world_resource_preflight.py no longer pins whole shared index / XML SHA.

Pure checks on temporary fixtures (no client directory, no LiTo.bin, no third-party modules).
"""
from pathlib import Path
import hashlib, importlib.util, json, os, struct, tempfile, unittest

ROOT = Path(__file__).resolve().parents[1]
PREFLIGHT = ROOT / 'app' / 'world_resource_preflight.py'
SPEC = importlib.util.spec_from_file_location('world_resource_preflight_under_test', PREFLIGHT)
wrp = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(wrp)

# Encrypted with the reference AES-128-CBC implementation (python `cryptography`), 2-byte trailing block.
ENCRYPTED_XML = bytes.fromhex(
    '583f786db1246ac4aaeabfd57a44b6313426e3b642f5f3ebbd3f30eb40fc2380b85fe5aec0d8c4fe030a74a90c228cfb'
    'bbdf89a64efb6b69fad5d597e068777331b1cdb11cfe6ae33d43eb3f78485eef35b12bb768c0')
PLAIN_XML = b'<?xml version="1.0"?>\r\n<effectlist count="1"><effect id="0" icon="5397"/></effectlist>'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def write_archive(index, members):
    """members: list of (name, payload). Writes index (.idx) + .pak, returns {name: (offset, size)}."""
    pak = bytearray(b'PAKHEADER')
    rows, places = [], {}
    for name, payload in members:
        places.setdefault(name, []).append((len(pak), len(payload)))
        rows.append(struct.pack('<I20sI', len(pak), name.encode('ascii'), len(payload)))
        pak += payload
    Path(index).write_bytes(struct.pack('<I', len(rows)) + b''.join(rows))
    Path(index).with_suffix('.pak').write_bytes(bytes(pak))
    return places


def effect_xml(count=540, changes=None):
    changes = changes or {}
    rows = []
    for i in range(count):
        attrs = {'id': str(i), 'icon': str(1000 + i), 'duration': '60', 'startMsg': str(2000 + i),
                 'startMsgName': 'EFFECT_%d' % i}
        if i in (5, 82):
            attrs['graphic'] = str(9000 + i)
        attrs.update(changes.get(i, {}))
        rows.append('<effect ' + ' '.join('%s="%s"' % item for item in attrs.items()) + '/>')
    return ('<?xml version="1.0"?>\r\n<effectlist count="%d">\n' % count + '\n'.join(rows) + '\n</effectlist>').encode('ascii')


class DecodeTests(unittest.TestCase):
    def test_fips197_block(self):
        keys = wrp._round_keys(bytes(range(16)))
        out = wrp._decrypt_block(bytes.fromhex('69c4e0d86a7b0430d8cdb78070b4c55a'), keys)
        self.assertEqual(bytes(out).hex(), '00112233445566778899aabbccddeeff')

    def test_archive_xml_cipher_with_partial_tail(self):
        self.assertEqual(wrp.decode_archive_xml(ENCRYPTED_XML), PLAIN_XML)

    def test_plain_xml_passes_through_and_garbage_is_refused(self):
        self.assertEqual(wrp.decode_archive_xml(PLAIN_XML), PLAIN_XML)
        with self.assertRaises(ValueError):
            wrp.decode_archive_xml(b'\x00garbage')

    def test_pinned_fingerprint_matches_the_xml_shared_status_was_compiled_from(self):
        source = ROOT / 'tools' / 'world-resource-compat' / 'build' / 'inputs' / 'effectlist2.xml'
        raw = source.read_bytes()  # attribute values are line-ending independent
        self.assertEqual(wrp.consumed_effects_sha256(wrp.parse_effects(raw)), wrp.CONSUMED_EFFECTS_SHA256)
        self.assertEqual(len(wrp.CONSUMED_EFFECT_IDS), 95)


class FixtureTestCase(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.base = Path(os.path.realpath(self.temp.name))
        self.saved = wrp.CONSUMED_EFFECTS_SHA256

    def tearDown(self):
        wrp.CONSUMED_EFFECTS_SHA256 = self.saved
        self.temp.cleanup()


class IndexFormatTests(FixtureTestCase):
    def test_readable_index(self):
        idx = self.base / 'Tile.idx'
        write_archive(idx, [('a.xml', b'1234'), ('b.spr', b'xy')])
        self.assertEqual(wrp.read_index(idx), {b'a.xml': [(9, 4)], b'b.spr': [(13, 2)]})

    def test_bad_layout_is_refused(self):
        idx = self.base / 'Tile.idx'
        write_archive(idx, [('a.xml', b'1234')])
        idx.write_bytes(idx.read_bytes() + b'\0')
        with self.assertRaisesRegex(ValueError, 'layout changed'):
            wrp.read_index(idx)

    def test_member_outside_archive_is_refused(self):
        idx = self.base / 'Tile.idx'
        write_archive(idx, [('a.xml', b'1234')])
        idx.with_suffix('.pak').write_bytes(b'short')
        with self.assertRaisesRegex(ValueError, 'outside archive'):
            wrp.read_index(idx)


class EffectlistTests(FixtureTestCase):
    def tile(self, xml, others=()):
        idx = self.base / 'Tile.idx'
        write_archive(idx, list(others) + [('effectlist2.xml', xml)])
        return {'archive': str(idx), 'archiveIndexSha256': '0' * 64, 'encodedSha256': '0' * 64}

    def setUp(self):
        super().setUp()
        wrp.CONSUMED_EFFECTS_SHA256 = wrp.consumed_effects_sha256(wrp.parse_effects(effect_xml()))

    def test_index_and_unconsumed_rows_may_change(self):
        # Like the 2026-11 client: other Tile members change, effect 517 gains a graphic, 538/539 appended.
        changed = effect_xml(542, {517: {'graphic': '517'}, 540: {'graphic': '540'}})
        receipt = self.tile(changed, others=[('new-member.til', b'x' * 50), ('another.til', b'yy')])
        self.assertEqual(len(wrp.verify_effectlist(receipt)), 542)

    def test_consumed_row_change_is_refused(self):
        for key in (5, 75, 107, 516):
            with self.subTest(effect=key):
                receipt = self.tile(effect_xml(changes={key: {'icon': '4242'}}))
                with self.assertRaisesRegex(ValueError, 'status rows changed'):
                    wrp.verify_effectlist(receipt)

    def test_earlier_unconsumed_row_stealing_a_consumed_graphic_is_refused(self):
        receipt = self.tile(effect_xml(changes={76: {'graphic': '9082'}}))
        with self.assertRaisesRegex(ValueError, 'status rows changed'):
            wrp.verify_effectlist(receipt)

    def test_unreadable_xml_is_refused(self):
        for payload in (b'Xxx\x00' + b'\x01' * 40, b'<effectlist count="3"></effectlist>', b'\x00binary'):
            with self.subTest(payload=payload[:8]):
                with self.assertRaises(ValueError):
                    wrp.verify_effectlist(self.tile(payload))

    def test_missing_or_duplicate_member_is_refused(self):
        idx = self.base / 'Tile.idx'
        write_archive(idx, [('other.xml', b'<a/>')])
        with self.assertRaisesRegex(ValueError, 'identity changed'):
            wrp.verify_effectlist({'archive': str(idx)})
        write_archive(idx, [('effectlist2.xml', effect_xml()), ('effectlist2.xml', effect_xml())])
        with self.assertRaisesRegex(ValueError, 'identity changed'):
            wrp.verify_effectlist({'archive': str(idx)})


class IconRoutingTests(FixtureTestCase):
    def build(self, sprite_members, sprite00_members):
        a = self.base / 'Sprite.idx'; b = self.base / 'Sprite00.idx'
        pa = write_archive(a, sprite_members); pb = write_archive(b, sprite00_members)
        return a, b, pa, pb

    def icons(self, a, b, pa, pb):
        pak_a, pak_b = str(a.with_suffix('.pak')), str(b.with_suffix('.pak'))
        def cand(pak, places, name, payload):
            offset, size = places[name][0]
            return {'archive': pak, 'offset': offset, 'bytes': size, 'sha256': sha(payload)}
        return {'index_pins': [{'path': str(a), 'sha256': '0' * 64}, {'path': str(b), 'sha256': '0' * 64}],
                'capacity_candidates': [cand(pak_a, pa, 'list.ico', b'2364')],
                'assets': [{'icon': 480, 'member': '480.tbt',
                            'candidates': [cand(pak_a, pa, '480.tbt', b'ICON-A'), cand(pak_b, pb, '480.tbt', b'ICON-B')]}]}

    def setUp(self):
        super().setUp()
        self.a, self.b, pa, pb = self.build([('list.ico', b'2364'), ('480.tbt', b'ICON-A')], [('480.tbt', b'ICON-B')])
        self.data = self.icons(self.a, self.b, pa, pb)

    def test_baseline_and_new_unrelated_members_pass(self):
        wrp.verify_icon_routing(self.data)
        # Rewrite indexes with extra members appended (new SHA, same icon locations).
        write_archive(self.a, [('list.ico', b'2364'), ('480.tbt', b'ICON-A'), ('9999.spr', b'new sprite')])
        write_archive(self.b, [('480.tbt', b'ICON-B'), ('7777.img', b'new image')])
        wrp.verify_icon_routing(self.data)

    def test_moved_icon_is_refused(self):
        write_archive(self.b, [('pad.img', b'pad'), ('480.tbt', b'ICON-B')])
        with self.assertRaisesRegex(ValueError, 'routing changed: 480.tbt'):
            wrp.verify_icon_routing(self.data)

    def test_shadowing_copy_is_refused(self):
        write_archive(self.b, [('480.tbt', b'ICON-B'), ('480.tbt', b'ICON-C')])
        with self.assertRaisesRegex(ValueError, 'routing changed'):
            wrp.verify_icon_routing(self.data)

    def test_icon_bytes_change_is_refused(self):
        write_archive(self.b, [('480.tbt', b'ICON-X')])
        with self.assertRaisesRegex(ValueError, 'status icon changed: 480.tbt'):
            wrp.verify_icon_routing(self.data)

    def test_list_ico_change_is_refused(self):
        write_archive(self.a, [('list.ico', b'9999'), ('480.tbt', b'ICON-A')])
        with self.assertRaisesRegex(ValueError, 'list.ico'):
            wrp.verify_icon_routing(self.data)
        write_archive(self.b, [('480.tbt', b'ICON-B'), ('list.ico', b'1')])
        write_archive(self.a, [('list.ico', b'2364'), ('480.tbt', b'ICON-A')])
        with self.assertRaisesRegex(ValueError, 'list.ico'):
            wrp.verify_icon_routing(self.data)


class FullPreflightTests(FixtureTestCase):
    def test_stale_whole_file_pins_in_receipts_no_longer_block(self):
        wrp.CONSUMED_EFFECTS_SHA256 = wrp.consumed_effects_sha256(wrp.parse_effects(effect_xml()))
        world = self.base / 'resources' / 'world'; (world / 'inputs').mkdir(parents=True)
        tile = self.base / 'Tile.idx'; write_archive(tile, [('effectlist2.xml', effect_xml())])
        (world / 'inputs' / 'effectlist2.xml.extraction.json').write_text(json.dumps(
            {'archive': str(tile), 'archiveIndexSha256': 'stale', 'encodedSha256': 'stale'}), 'utf-8')
        a = self.base / 'Sprite.idx'; places = write_archive(a, [('list.ico', b'2364')])
        offset, size = places['list.ico'][0]
        (world / 'icon-assets.json').write_text(json.dumps({
            'index_pins': [{'path': str(a), 'sha256': 'stale'}],
            'capacity_candidates': [{'archive': str(a.with_suffix('.pak')), 'offset': offset, 'bytes': size, 'sha256': sha(b'2364')}],
            'assets': []}), 'utf-8')
        saved = wrp.R
        wrp.R = world
        try:
            wrp.verify_resource_sources()
        finally:
            wrp.R = saved


class PinTableTests(unittest.TestCase):
    def test_formal_helper_pins_this_preflight(self):
        text = (ROOT / 'src' / 'development_startup.py').read_text('utf-8')
        # The file is stored with LF; the deployed app/ copy keeps LF, a Windows CI checkout may add CR.
        actual = sha(PREFLIGHT.read_bytes().replace(b'\r\n', b'\n')).upper()
        self.assertIn("app\\\\world_resource_preflight.py'): '" + actual + "'", text)
        pins = json.loads((ROOT / 'app' / 'runtime-pins.json').read_text('utf-8-sig'))
        values = [v for k, v in pins.items() if k.endswith('world_resource_preflight.py')]
        self.assertEqual(values, [actual])

    def test_no_whole_file_index_pin_left(self):
        code = PREFLIGHT.read_text('utf-8')
        self.assertNotIn('receipt["archiveIndexSha256"]', code)
        self.assertNotIn('receipt["encodedSha256"]', code)
        self.assertNotIn('index_pins"]:\n        if hashlib', code)


if __name__ == '__main__':
    unittest.main()
