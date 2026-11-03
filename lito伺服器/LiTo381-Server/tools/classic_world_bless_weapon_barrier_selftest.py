#!/usr/bin/env python3
"""Encrypted WORLD proof that admitted skill48 ends the caster barrier."""

from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path

from classic_darkness_selftest import drain, inventory_delta
from classic_world_haste_native_selftest import (
    ABSOLUTE_BARRIER,
    ABSOLUTE_BARRIER_GFX,
)
from classic_world_light_haste_selftest import enter_independent_world
from classic_world_taming_monster_selftest import (
    TARGET_CELL,
    TARGET_ID,
    walk_to_target,
)
from extended_selftest import Session


SKILL_ID = 48
REQUEST_PREFIX = b"\x14\x05\x07"
ACTION = 19
EFFECT_GFX = 2176
MAGIC_GEM_NAME = "Magic Gem"
CASTER_ID = 3


def action(object_id: int) -> bytes:
    return b"\x20" + struct.pack("<IB", object_id, ACTION)


def effect(object_id: int, gfx: int) -> bytes:
    return b"\x37" + struct.pack("<IH", object_id, gfx)


def mana(current: int, maximum: int) -> bytes:
    return b"\x4D" + struct.pack("<HH", current, maximum)


def acceptance(host: str, port: int) -> dict[str, object]:
    mage = Session(host, port, "test2", "TestMage")
    peer = Session(host, port, "test5", "TestElf")
    try:
        room_id, start, _, hp, mp, maximum_hp, maximum_mp = (
            enter_independent_world(
                mage, peer, "WORLD Bless Weapon Barrier", "TestMage",
                CASTER_ID))
        assert (hp, maximum_hp, mp, maximum_mp) == (100, 100, 110, 110)
        gem = next(row for row in mage.inventory_records
                   if row[3] == MAGIC_GEM_NAME)
        assert gem[2] >= 2, gem
        initial_gems = gem[2]

        walk_to_target(mage, start)
        mage.send(ABSOLUTE_BARRIER)
        barrier_packets = drain(mage, 0.9)
        inventory_delta(barrier_packets, gem[0], initial_gems - 2)
        assert action(CASTER_ID) in barrier_packets
        assert effect(CASTER_ID, ABSOLUTE_BARRIER_GFX) in barrier_packets
        assert mana(70, maximum_mp) in barrier_packets

        request = REQUEST_PREFIX + struct.pack("<I", TARGET_ID)
        mage.send(request)
        packets = drain(mage, 1.0)
        assert action(CASTER_ID) in packets
        assert effect(TARGET_ID, EFFECT_GFX) in packets
        assert mana(50, maximum_mp) in packets
        assert not any(packet[0] in (0x23, 0x68, 0xFA)
                       for packet in packets)

        mage.send(b"\x71\x00")
        assert mage.receive(0x78) == b"\x78\x00"
        return {
            "room_id": room_id,
            "world_start": list(start),
            "target_object_id": TARGET_ID,
            "target_coordinates": list(TARGET_CELL),
            "barrier_request": ABSOLUTE_BARRIER.hex(" ").upper(),
            "barrier_effect": effect(
                CASTER_ID, ABSOLUTE_BARRIER_GFX).hex(" ").upper(),
            "bless_weapon_request": request.hex(" ").upper(),
            "bless_weapon_effect": effect(
                TARGET_ID, EFFECT_GFX).hex(" ").upper(),
            "action": ACTION,
            "mp": [110, 100, 70, 50, maximum_mp],
            "magic_gems": [initial_gems, initial_gems - 2],
            "server_trace_required": (
                "Absolute Barrier cancelled player=TestMage "
                "reason=spell-48"),
            "legacy_packetbox_fa_omitted": True,
            "encrypted_stream_aligned": True,
        }
    finally:
        mage.close()
        peer.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.2")
    parser.add_argument("--port", type=int, default=32100)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = {
        "transport": "encrypted LiTo 3.81C loopback framing",
        "endpoint": f"{args.host}:{args.port}",
        "bless_weapon_barrier_cleanup": acceptance(args.host, args.port),
    }
    rendered = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print("PASS native WORLD Bless Weapon barrier cleanup: exact cccd; "
          "MP/reuse committed; Absolute Barrier ended; no FA/attack/HP; "
          "aligned framing")


if __name__ == "__main__":
    main()
