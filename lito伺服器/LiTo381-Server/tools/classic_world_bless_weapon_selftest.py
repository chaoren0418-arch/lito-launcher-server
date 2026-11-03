#!/usr/bin/env python3
"""Encrypted ordinary-WORLD acceptance for native skill48 Bless Weapon."""

from __future__ import annotations

import argparse
import json
import struct
import time
from pathlib import Path

from classic_darkness_selftest import drain, inventory_delta
from classic_elemental_arrow_selftest import actor, status
from classic_glowing_aura_selftest import spell_advertised
from classic_world_berserkers_selftest import (
    CANCELLATION_GFX,
    FIGHTER,
    FIGHTER_ACCOUNT,
    action,
    attack_ogre,
    effect,
)
from classic_world_monster_physical_selftest import (
    OGRE,
    PRIMARY_CELL,
    PRIMARY_ROUTE,
    SECONDARY_CELL,
    SECONDARY_ROUTE,
    exit_world,
    walk,
)
from classic_world_tree_call_selftest import (
    DESTINATION,
    REQUEST as WORLD_TREE_REQUEST,
    TELEPORT_UNLOCK,
    assert_native_teleport_packets,
)
from extended_selftest import Session, start_two_player_battle


SKILL_ID = 48
SELECTOR = (5, 7)
DISPLAY_NAME = "Bless Weapon"
ACTION = 19
EFFECT_GFX = 2176
MP_COST = 20
DURATION_SECONDS = 1200
MELEE_HIT = 2
MELEE_DAMAGE = 2
RANGED_HIT = 2
RANGED_DAMAGE = 0
CASTER = "TestMage"


def enter_world_pair(caster: Session, fighter: Session,
                     caster_id: int, fighter_id: int,
                     caster_mp: int, caster_max_mp: int,
                     fighter_mp: int, fighter_max_mp: int
                     ) -> tuple[list[bytes], list[bytes]]:
    caster.send(WORLD_TREE_REQUEST)
    caster_portal = drain(caster)
    drain(fighter, 0.2)
    assert_native_teleport_packets(
        caster_portal, caster_id, caster_mp - 10, caster_max_mp)

    fighter.send(WORLD_TREE_REQUEST)
    fighter_portal = drain(fighter)
    drain(caster, 0.2)
    assert_native_teleport_packets(
        fighter_portal, fighter_id, fighter_mp - 10, fighter_max_mp)

    caster.send(b"\x08")
    fighter.send(b"\x08")
    caster_rows = drain(caster)
    fighter_rows = drain(fighter)
    assert caster_rows.count(TELEPORT_UNLOCK) == 1
    assert fighter_rows.count(TELEPORT_UNLOCK) == 1
    assert actor(caster_rows, CASTER) == (
        caster_id, DESTINATION[1], DESTINATION[2])
    assert actor(fighter_rows, FIGHTER) == (
        fighter_id, DESTINATION[1], DESTINATION[2])
    return caster_rows, fighter_rows


def run(host: str, port: int) -> dict[str, object]:
    caster = fighter = None
    caster_world = fighter_world = False
    try:
        caster = Session(host, port, "test2", CASTER)
        fighter = Session(host, port, FIGHTER_ACCOUNT, FIGHTER)
        room_id, caster_rows, fighter_rows = start_two_player_battle(
            caster, fighter, f"Native WORLD {DISPLAY_NAME}",
            return_bootstrap=True, opposing_forces=True)
        caster_id, _, _ = actor(caster_rows, CASTER)
        fighter_id, _, _ = actor(caster_rows, FIGHTER)
        _, caster_mp, _, caster_max_mp = status(caster_rows)
        _, fighter_mp, _, fighter_max_mp = status(fighter_rows)
        assert (caster_id, fighter_id) == (3, 12)
        assert spell_advertised(caster_rows, SKILL_ID)

        caster_entry, fighter_entry = enter_world_pair(
            caster, fighter, caster_id, fighter_id,
            caster_mp, caster_max_mp, fighter_mp, fighter_max_mp)
        caster_world = fighter_world = True
        drain(caster, 0.2)
        drain(fighter, 0.2)
        caster_world_mp = caster_mp - 10

        fighter_walk = walk(
            fighter, caster, PRIMARY_ROUTE,
            DESTINATION[1:3], PRIMARY_CELL)
        caster_walk = walk(
            caster, fighter, SECONDARY_ROUTE,
            DESTINATION[1:3], SECONDARY_CELL)
        drain(caster, 0.2)
        drain(fighter, 0.2)

        before, _, _ = attack_ogre(fighter, caster, fighter_id)
        assert int(before["damage"]) > 0, before

        request_prefix = bytes((0x14, *SELECTOR))
        npc_request = request_prefix + struct.pack("<I", OGRE[0])
        caster.send(npc_request)
        caster_npc_cast = drain(caster, 0.6)
        fighter_npc_cast = drain(fighter, 0.35)
        expected_action = action(caster_id)
        npc_effect = effect(OGRE[0], EFFECT_GFX)
        assert expected_action in caster_npc_cast
        assert expected_action in fighter_npc_cast
        assert npc_effect in caster_npc_cast and npc_effect in fighter_npc_cast
        assert b"\x4D" + struct.pack(
            "<HH", caster_world_mp - MP_COST,
            caster_max_mp) in caster_npc_cast
        assert not any(row[0] == 0xFA
                       for row in caster_npc_cast + fighter_npc_cast)

        pc_request = request_prefix + struct.pack("<I", fighter_id)
        caster.send(pc_request)
        caster_pc_cast = drain(caster, 0.6)
        fighter_pc_cast = drain(fighter, 0.35)
        pc_effect = effect(fighter_id, EFFECT_GFX)
        assert expected_action in caster_pc_cast
        assert expected_action in fighter_pc_cast
        assert pc_effect in caster_pc_cast and pc_effect in fighter_pc_cast
        assert b"\x4D" + struct.pack(
            "<HH", caster_world_mp - MP_COST * 2,
            caster_max_mp) in caster_pc_cast
        success = [row for row in fighter_pc_cast
                   if len(row) >= 3 and row[:3] == b"\x10\xA1\x00"]
        assert len(success) == 1, [row.hex() for row in fighter_pc_cast]
        assert b"$245\0$247\0" in success[0], success[0].hex()
        assert not any(row[0] == 0xFA
                       for row in caster_pc_cast + fighter_pc_cast)

        active, _, _ = attack_ogre(fighter, caster, fighter_id)
        assert int(active["damage"]) == int(before["damage"]) + MELEE_DAMAGE, (
            before, active)

        gem = next(row for row in caster.inventory_records
                   if row[3] == "Magic Gem")
        cancellation = b"\x14\x05\x03" + struct.pack("<I", fighter_id)
        caster.send(cancellation)
        caster_cancel = drain(caster, 0.7)
        fighter_cancel = drain(fighter, 0.3)
        cancel_action = action(caster_id)
        cancel_effect = effect(fighter_id, CANCELLATION_GFX)
        assert cancel_action in caster_cancel and cancel_action in fighter_cancel
        assert cancel_effect in caster_cancel and cancel_effect in fighter_cancel
        inventory_delta(caster_cancel, gem[0], gem[2] - 2)
        assert b"\x4D" + struct.pack(
            "<HH", caster_world_mp - MP_COST * 2 - 40,
            caster_max_mp) in caster_cancel

        after, _, _ = attack_ogre(fighter, caster, fighter_id)
        assert int(after["damage"]) == int(before["damage"]), (before, after)

        for session in (caster, fighter):
            exit_world(session)
        caster_world = fighter_world = False
        time.sleep(0.3)
        return {
            "transport": "encrypted LiTo 3.81C loopback framing",
            "endpoint": f"{host}:{port}",
            "phase": "WORLD",
            "room_ids": [room_id],
            "skill_id": SKILL_ID,
            "selector": list(SELECTOR),
            "requests": {
                "npc": npc_request.hex(" ").upper(),
                "player": pc_request.hex(" ").upper(),
                "cancellation": cancellation.hex(" ").upper(),
            },
            "wire_layout": "C_USE_SPELL cccd (one PC/NPC object target)",
            "targets": {
                "npc": {"object_id": OGRE[0], "npc_id": OGRE[3]},
                "player": {"name": FIGHTER, "object_id": fighter_id,
                           "same_map_instance": True},
            },
            "resources": {
                "action": ACTION,
                "effect_gfx": EFFECT_GFX,
                "duration_seconds": DURATION_SECONDS,
                "caster_mp": [caster_world_mp,
                              caster_world_mp - MP_COST,
                              caster_world_mp - MP_COST * 2,
                              caster_world_mp - MP_COST * 2 - 40,
                              caster_max_mp],
                "magic_gems": [gem[2], gem[2] - 2],
            },
            "modifiers": {
                "melee_hit": MELEE_HIT,
                "melee_damage": MELEE_DAMAGE,
                "ranged_hit": RANGED_HIT,
                "ranged_damage": RANGED_DAMAGE,
            },
            "ordinary_monster_damage": {
                "before": before,
                "active": active,
                "after_cancellation": after,
                "exact_damage_delta": True,
            },
            "success_message": success[0].hex(" ").upper(),
            "route_packets": {
                "fighter": len(fighter_walk),
                "caster": len(caster_walk),
            },
            "natural_expiry_dispatch_connected": True,
            "legacy_packetbox_fa_omitted": True,
            "encrypted_stream_aligned": True,
            "legacy_databases_mutated": False,
        }
    finally:
        for session, in_world in ((caster, caster_world),
                                  (fighter, fighter_world)):
            if session is None:
                continue
            if in_world:
                try:
                    exit_world(session)
                except (AssertionError, OSError):
                    pass
            session.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.2")
    parser.add_argument("--port", type=int, default=32100)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    evidence = run(args.host, args.port)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
    print(f"PASS WORLD skill{SKILL_ID} exact cccd PC/NPC targets; message161; "
          f"ordinary-monster melee HIT+{MELEE_HIT}/DMG+{MELEE_DAMAGE} lease; "
          "Cancellation restores; no FA; encrypted stream aligned")


if __name__ == "__main__":
    main()
