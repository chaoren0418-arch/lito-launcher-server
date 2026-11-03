"""Build a presentation-only catalog from the client's actual effectlist2.xml.

Native slots and XML effect IDs are different namespaces. This binding preserves
packet selectors, group rules, durations and messages. No database is opened.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import xml.etree.ElementTree as ET
import zlib

import pefile

ROOT = Path(__file__).resolve().parent
LAUNCHER = ROOT.parents[1]
NATIVE = LAUNCHER.parent / "舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe"
EXTENDED = [
    (77, 107, "DRAGON_SKIN_S", 3080),
    (78, 116, "MIRROR_IMAGE_S", 3123),
    (79, 118, "ILLUSION_ORGE_S", 3117),
    (80, 120, "CONCENTRATION_S", 3097),
    (81, 121, "ILLUSION_LICH_S", 3115),
    (82, 123, "PATIENCE_S", 3127),
    (83, 126, "ILLUSION_DIA_GOLEM_S", 3113),
    (84, 128, "INSIGHT_S", 3119),
    (85, 129, "PANIC_S", 3125),
    (86, 130, "ILLUSION_AVATAR_S", 3111),
    (87, 82, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (88, 92, "STR_STATUS_EFFECT_LOVE_DOLL", 0),
    (89, 512, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (90, 513, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (91, 514, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (92, 515, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (93, 516, "STR_STATUS_EFFECT_MAGIC_DOLL", 0),
    (94, 91, "POTION_OF_MANA2_S", 0),
    (95, 110, "BLOODLUST_S", 0),
]
SLOTS = 96


def sha(data):
    return hashlib.sha256(data).hexdigest()


def native_rows(path=NATIVE):
    pe = pefile.PE(str(path))
    raw = pe.get_data(0x6328D8 - pe.OPTIONAL_HEADER.ImageBase, 77 * 20)
    pe.close()
    if len(raw) != 1540:
        raise ValueError("native table truncated")
    rows = list(struct.iter_unpack("<5I", raw))
    if rows[0] != (481, 0, 0, 849, 0) or rows[76] != (0, 0, 0, 0, 0):
        raise ValueError("unsupported native table")
    return raw, rows


def parse_effects(raw):
    if len(raw) > 2_000_000 or b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
        raise ValueError("unbounded/external XML is not supported")
    root = ET.fromstring(raw)
    if root.tag != "effectlist" or len(root) != int(root.get("count", "-1")):
        raise ValueError("effectlist count/tag mismatch")
    effects = {}
    for e in root:
        if e.tag != "effect":
            raise ValueError("unexpected effect node")
        key = int(e.attrib["id"])
        if key in effects or not 0 <= key < 65536:
            raise ValueError("duplicate/out-of-range effect ID")
        if key != len(effects):
            raise ValueError("native XML effect row order must match IDs")
        for field in ("icon", "offIcon", "graphic", "group", "priority"):
            if field in e.attrib and not 0 <= int(e.attrib[field]) <= 65535:
                raise ValueError("out-of-range " + field)
        effects[key] = dict(e.attrib)
    return effects


def compile_catalog(xml, raw_native, rows):
    effects = parse_effects(xml)
    records, audit = [], []
    # Slot 38 is independently identified by packet-box34 in both binaries.
    # Its old startMsg aliases meditation; validate its XML identity explicitly.
    # See verify_blue_identity.py. Slot 76 remains reserved.
    for slot in range(76):
        effect = effects.get(slot)
        if slot == 38:
            if (rows[slot] != (1478, 600, 0, 910, 0) or not effect or
                    effect.get("startMsgName") != "POTION_OF_MANA1_S" or
                    int(effect.get("startMsg", "-1")) != 3121):
                raise ValueError("packet-box34 blue-potion identity mismatch")
        elif not effect or int(effect.get("startMsg", "-1")) != rows[slot][3]:
            raise ValueError(f"legacy identity mismatch slot={slot}")
        records.append((slot, slot, int(effect["icon"]), rows[slot][0], rows[slot][3]))
        audit.append({"slot": slot, "effect": effect, "old_icon": rows[slot][0]})
    for slot, effect_id, name, old_icon in EXTENDED:
        effect = effects.get(effect_id)
        if not effect or effect.get("startMsgName") != name:
            raise ValueError(f"extended identity mismatch slot={slot}")
        records.append((slot, effect_id, int(effect["icon"]), old_icon, 0))
        audit.append({"slot": slot, "effect": effect, "old_icon": old_icon})
    payload = b"".join(struct.pack("<5I", *row) for row in records)
    header = struct.pack("<6I", 0x3143534C, 1, SLOTS, len(records),
                         zlib.crc32(raw_native), zlib.crc32(payload))
    return header + payload, {
        "xml_sha256": sha(xml), "native_table_sha256": sha(raw_native),
        "mapped_slots": len(records), "native_slots": SLOTS,
        "changed_icons": [{"slot": r["slot"], "from": r["old_icon"], "to": int(r["effect"]["icon"])}
                          for r in audit if r["old_icon"] != int(r["effect"]["icon"])],
        "bindings": audit,
        "unmapped": [{"slot": 76, "reason": "reserved empty native row; preserve"}],
        "behavior": "icons only; packet duration, group, messages, selectors unchanged",
        "not_yet_consumed": ["offIcon", "graphic", "XML group/priority and remove rules"],
        "source_database_modified": False,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", type=Path, default=ROOT / "build/inputs/effectlist2.xml")
    ap.add_argument("--output", type=Path, default=ROOT / "build/candidate")
    args = ap.parse_args()
    native, rows = native_rows()
    data, audit = compile_catalog(args.xml.read_bytes(), native, rows)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "shared_status.bin").write_bytes(data)
    (args.output / "status-catalog.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({"mapped": audit["mapped_slots"], "changed_icons": audit["changed_icons"],
                      "catalog_sha256": sha(data)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
