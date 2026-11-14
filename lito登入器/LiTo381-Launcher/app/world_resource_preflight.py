"""Preflight by default. --launch explicitly invokes the existing owned startup.

This file does not replace the formal launcher. A future native launch still
uses its exact READY/claim/identity/cleanup flow; no gate is bypassed.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import sys

R = Path(__file__).resolve().parent / "resources/world"
L = R.parents[1]

# Issue #453 (T155): Tile.idx, Sprite*.idx and Tile.pak/effectlist2.xml are shared
# client data that is updated under the agreed rules (same ruling as #448 for
# TW13081901.txt). Their whole-file SHA-256 is NOT pinned any more. Instead the
# preflight checks that each index is readable (exact 28-byte record layout, every
# member inside its .pak) and that the parts LiTo actually consumes are unchanged:
#   * effectlist2.xml decodes, parses, and the 95 rows compiled into
#     compat/shared_status.bin + the compat DLL tables are identical;
#   * every status icon / list.ico still resolves to the qualified (archive, offset,
#     size) set and those payload bytes keep their SHA-256.
# The receipt's archiveIndexSha256/encodedSha256 and icon-assets index_pins stay as
# provenance only. Do not add whole-index SHA pins back.
CONSUMED_EFFECT_IDS = tuple(range(76)) + (107, 116, 118, 120, 121, 123, 126, 128, 129, 130,
                                          82, 92, 512, 513, 514, 515, 516, 91, 110)
# consumed_effects_sha256() of the effectlist2.xml shared_status.bin was compiled from
# (tools/world-resource-compat/build/inputs/effectlist2.xml, decoded SHA 07efd481...).
CONSUMED_EFFECTS_SHA256 = "5b3e6bb09379a65f457e7d0087102dd2d149c30c9797b74059a038eabe7ca8e0"
EFFECTLIST_LIMIT = 2_000_000


def _xtime(value):
    value <<= 1
    return (value ^ 0x11B) if value & 0x100 else value


def _gmul(a, b):
    result = 0
    while b:
        if b & 1:
            result ^= a
        a = _xtime(a)
        b >>= 1
    return result


def _sbox():
    box = [0] * 256
    for value in range(256):
        inverse = next((x for x in range(1, 256) if _gmul(value, x) == 1), 0) if value else 0
        result = inverse
        for shift in range(1, 5):
            result ^= ((inverse << shift) | (inverse >> (8 - shift))) & 0xFF
        box[value] = result ^ 0x63
    return box


_SBOX = _sbox()
_INV_SBOX = [0] * 256
for _index_value, _box_value in enumerate(_SBOX):
    _INV_SBOX[_box_value] = _index_value
_MUL = {factor: [_gmul(value, factor) for value in range(256)] for factor in (9, 11, 13, 14)}
# Lineage XML archive cipher (same rule as PakViewer XmlCracker): byte 0 'X' -> '<',
# bytes 4.. are AES-128-CBC without padding; a trailing partial block is XORed with
# the previous ciphertext block.
_XML_KEY = bytes((0xDC, 0x84, 0x01, 0x21, 0x2A, 0x40, 0x20, 0x0A, 0xDD, 0x25, 0xB9, 0xA7, 0x0D, 0xB9, 0xC9, 0x4E))
_XML_IV = bytes((0x3E, 0x09, 0x78, 0xAA, 0xC4, 0xD5, 0x30, 0x63, 0x30, 0x0C, 0x5F, 0x9A, 0x80, 0x7F, 0x22, 0x46))


def _round_keys(key):
    words = [list(key[i:i+4]) for i in range(0, 16, 4)]
    rcon = 1
    for i in range(4, 44):
        word = list(words[i-1])
        if i % 4 == 0:
            word = [_SBOX[b] for b in word[1:] + word[:1]]
            word[0] ^= rcon
            rcon = _xtime(rcon) & 0xFF
        words.append([a ^ b for a, b in zip(words[i-4], word)])
    return [sum(words[r*4:r*4+4], []) for r in range(11)]


def _decrypt_block(block, keys):
    state = [a ^ b for a, b in zip(block, keys[10])]
    for round_index in range(9, -1, -1):
        # InvShiftRows (state is column-major: index = row + 4*column) + InvSubBytes
        state = [_INV_SBOX[state[(r + 4 * ((c - r) % 4))]] for c in range(4) for r in range(4)]
        state = [a ^ b for a, b in zip(state, keys[round_index])]
        if round_index:
            mixed = []
            for c in range(4):
                a0, a1, a2, a3 = state[4*c:4*c+4]
                mixed += [_MUL[14][a0] ^ _MUL[11][a1] ^ _MUL[13][a2] ^ _MUL[9][a3],
                          _MUL[9][a0] ^ _MUL[14][a1] ^ _MUL[11][a2] ^ _MUL[13][a3],
                          _MUL[13][a0] ^ _MUL[9][a1] ^ _MUL[14][a2] ^ _MUL[11][a3],
                          _MUL[11][a0] ^ _MUL[13][a1] ^ _MUL[9][a2] ^ _MUL[14][a3]]
            state = mixed
    return state


def decode_archive_xml(raw):
    if raw[:1] == b"<":
        return bytes(raw)
    if raw[:1] != b"X" or len(raw) <= 4:
        raise ValueError("unrecognized archive XML encoding")
    keys = _round_keys(_XML_KEY)
    body = raw[4:]
    full = len(body) // 16 * 16
    out = bytearray()
    previous = _XML_IV
    for start in range(0, full, 16):
        block = body[start:start+16]
        out += bytes(a ^ b for a, b in zip(_decrypt_block(block, keys), previous))
        previous = block
    out += bytes(a ^ b for a, b in zip(body[full:], previous))
    return b"<" + bytes(raw[1:4]) + bytes(out)


def parse_effects(raw):
    """Same acceptance rules as tools/world-resource-compat/catalog.parse_effects."""
    import xml.etree.ElementTree as ET
    if len(raw) > EFFECTLIST_LIMIT or b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
        raise ValueError("unbounded/external XML is not supported")
    root = ET.fromstring(raw)
    if root.tag != "effectlist" or len(root) != int(root.get("count", "-1")):
        raise ValueError("effectlist count/tag mismatch")
    effects = {}
    for element in root:
        if element.tag != "effect":
            raise ValueError("unexpected effect node")
        key = int(element.attrib["id"])
        if key != len(effects):
            raise ValueError("native XML effect row order must match IDs")
        effects[key] = dict(element.attrib)
    return effects


def consumed_effects_sha256(effects):
    """Fingerprint of exactly what shared_status.bin and the compat DLL were compiled from."""
    first_graphic = {}
    for key in sorted(effects):
        graphic = int(effects[key].get("graphic", "0"))
        if graphic > 0:
            first_graphic.setdefault(graphic, key)
    rows = []
    for key in CONSUMED_EFFECT_IDS:
        effect = effects.get(key)
        graphic = int(effect.get("graphic", "0")) if effect else 0
        rows.append([key, sorted(effect.items()) if effect else None, first_graphic.get(graphic) if graphic else None])
    return hashlib.sha256(json.dumps(rows, separators=(",", ":")).encode("utf-8")).hexdigest()


def read_index(index, wanted=None):
    """Readable-format check for a shared .idx; returns {member: [(offset, size), ...]}."""
    index = Path(index)
    raw = index.read_bytes()
    if len(raw) < 4:
        raise ValueError("shared index unreadable: " + str(index))
    count = struct.unpack_from("<I", raw)[0]
    if len(raw) != 4 + count * 28:
        raise ValueError("shared index layout changed: " + str(index))
    limit = index.with_suffix(".pak").stat().st_size
    members = {}
    for i in range(count):
        offset, name, size = struct.unpack_from("<I20sI", raw, 4+28*i)
        if offset + size > limit:
            raise ValueError("shared index member outside archive: " + str(index))
        name = name.split(b"\0")[0].lower()
        if wanted is None or name in wanted:
            members.setdefault(name, []).append((offset, size))
    return members


def _payload_sha256(archive, offset, size):
    with Path(archive).open("rb") as stream:
        stream.seek(offset)
        return hashlib.sha256(stream.read(size)).hexdigest()


def verify_effectlist(receipt):
    index = Path(receipt["archive"])
    members = read_index(index, {b"effectlist2.xml"}).get(b"effectlist2.xml", [])
    if len(members) != 1:
        raise ValueError("effectlist2.xml archive identity changed")
    offset, size = members[0]
    if size > EFFECTLIST_LIMIT:
        raise ValueError("effectlist2.xml archive bounds changed")
    with index.with_suffix(".pak").open("rb") as stream:
        stream.seek(offset)
        encoded = stream.read(size)
    try:
        effects = parse_effects(decode_archive_xml(encoded))
    except ValueError:
        raise
    except Exception as error:
        raise ValueError("shared effectlist2.xml unreadable: " + str(error)) from error
    if consumed_effects_sha256(effects) != CONSUMED_EFFECTS_SHA256:
        raise ValueError("shared effectlist2.xml status rows changed; rebuild shared_status.bin and the compat DLL before launch")
    return effects


def verify_icon_routing(icons):
    def key(archive, offset, size):
        return (os.path.normcase(str(Path(archive))), offset, size)
    wanted = {b"list.ico"} | {icon["member"].lower().encode("ascii") for icon in icons["assets"]}
    located = {}
    for row in icons["index_pins"]:
        archive = Path(row["path"]).with_suffix(".pak")
        for member, places in read_index(row["path"], wanted).items():
            located.setdefault(member, set()).update(key(archive, o, s) for o, s in places)
    # Cache sizing: an unchanged TBT member can still be unreachable when list.ico changes.
    expected = {key(row["archive"], row["offset"], row["bytes"]) for row in icons["capacity_candidates"]}
    if located.get(b"list.ico", set()) != expected:
        raise ValueError("shared TBT cache capacity changed; requalify list.ico")
    for row in icons["capacity_candidates"]:
        if _payload_sha256(row["archive"], row["offset"], row["bytes"]) != row["sha256"]:
            raise ValueError("shared TBT cache capacity changed; requalify list.ico")
    for icon in icons["assets"]:
        expected = {key(row["archive"], row["offset"], row["bytes"]) for row in icon["candidates"]}
        if located.get(icon["member"].lower().encode("ascii"), set()) != expected:
            raise ValueError("shared status icon routing changed: " + icon["member"])
        for row in icon["candidates"]:
            if _payload_sha256(row["archive"], row["offset"], row["bytes"]) != row["sha256"]:
                raise ValueError("shared status icon changed: " + icon["member"])


def verify_resource_sources():
    receipt_path = R / "inputs/effectlist2.xml.extraction.json"
    if not receipt_path.exists():
        receipt_path = R / "inputs/extraction.json"
    verify_effectlist(json.loads(receipt_path.read_text("utf-8")))
    verify_icon_routing(json.loads((R / "icon-assets.json").read_text("utf-8")))


def configure(client,dll):
    option=os.environ.get('LITO_SHARED_EXPIRED_ICONS','')
    if option not in ('','0','1'):raise ValueError('LITO_SHARED_EXPIRED_ICONS must be 0 or 1')
    verify_resource_sources()
    app=Path(__file__).resolve().parent
    if (Path(client)/'ddraw.dll').exists():raise ValueError('Unexpected client ddraw.dll; use the LiTo-specific DLL')
    receipt=app/'resources/class-icons/resources.json'
    os.environ['LITO_NAMED_DDRAW_SHA256']=hashlib.sha256(Path(dll).read_bytes()).hexdigest()
    os.environ['LITO_CLASS_ICON_RECEIPT']=str(receipt)
    os.environ['LITO_CLASS_ICON_RECEIPT_SHA256']=hashlib.sha256(receipt.read_bytes()).hexdigest()
    os.environ['LITO_SHARED_STATUS_CATALOG']=str(app/'compat/shared_status.bin')
