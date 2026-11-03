#!/usr/bin/env python3
"""Encrypted ordinary-WORLD acceptance for native skill67 Shape Change."""

from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path

from classic_darkness_selftest import drain, inventory_delta
from classic_elemental_arrow_selftest import actor, status
from classic_glowing_aura_selftest import spell_advertised
from classic_shape_change_selftest import (
    RANDOM_GFX,
    decode_hypertext,
)
from classic_world_berserkers_selftest import (
    FIGHTER,
    FIGHTER_ACCOUNT,
    attack_ogre,
)
from classic_world_bless_weapon_selftest import enter_world_pair
from classic_world_monster_physical_selftest import (
    PRIMARY_CELL,
    PRIMARY_ROUTE,
    SECONDARY_CELL,
    SECONDARY_ROUTE,
    exit_world,
    walk,
)
from extended_selftest import Session, read_z, start_two_player_battle, z


SKILL_ID = 67
SELECTOR = (8, 2)
ACTION = 19
EFFECT_GFX = 230
MP_COST = 80
DURATION_SECONDS = 7200
CASTER = "TestMage"


def request(target_id: int) -> bytes:
    return b"\x14\x08\x02" + struct.pack("<I", target_id)


def action(caster_id: int) -> bytes:
    return b"\x20" + struct.pack("<IB", caster_id, ACTION)


def effect(target_id: int) -> bytes:
    return b"\x37" + struct.pack("<IH", target_id, EFFECT_GFX)


def assert_cast(rows: list[bytes], peer_rows: list[bytes],
                caster_id: int, target_id: int,
                gem_id: int, expected_gems: int,
                expected_mp: int, maximum_mp: int) -> None:
    inventory_delta(rows, gem_id, expected_gems)
    assert action(caster_id) in rows and action(caster_id) in peer_rows
    assert effect(target_id) in rows and effect(target_id) in peer_rows
    assert b"\x4D" + struct.pack("<HH", expected_mp, maximum_mp) in rows


def changed_description(rows: list[bytes], object_id: int) -> str:
    matches = [row for row in rows
               if row[0] == 0x18
               and row[1:5] == struct.pack("<I", object_id)]
    assert len(matches) == 1, [row.hex() for row in rows]
    return read_z(matches[0], 5)[0]


def equip_ring(caster: Session, peer: Session, object_id: int) -> None:
    caster.send(b"\x1C" + struct.pack("<I", object_id) + z(""))
    rows = drain(caster)
    peer_rows = drain(peer, 0.35)
    assert changed_description(rows, object_id) == "+0 $261 ($117)"
    assert b"\x26\x02\x01" in rows
    assert peer_rows == []


def equip_sword(caster: Session, peer: Session, caster_id: int,
                object_id: int) -> None:
    caster.send(b"\x1C" + struct.pack("<I", object_id) + z(""))
    rows = drain(caster)
    peer_rows = drain(peer, 0.35)
    assert changed_description(rows, object_id) == "+0 $100 ($9)"
    wield = b"\x1D" + struct.pack("<IB", caster_id, 4)
    assert wield in rows and wield in peer_rows


def select_world_form(caster: Session, peer: Session, caster_id: int,
                      action_name: str, gfx: int, walk_action: int,
                      forced_sword_id: int | None = None
                      ) -> tuple[list[bytes], list[bytes]]:
    caster.send(b"\x27" + struct.pack("<I", caster_id)
                + z(action_name) + z(""))
    rows = drain(caster)
    peer_rows = drain(peer, 0.35)
    close = b"\x2A" + struct.pack("<I", caster_id) + b"\0\0\0\0"
    polymorph = b"\x27" + struct.pack("<IHB", caster_id, gfx, walk_action)
    assert close in rows and close not in peer_rows
    assert polymorph in rows and polymorph in peer_rows
    if forced_sword_id is not None:
        assert changed_description(rows, forced_sword_id) == "+0 $100"
        wield = b"\x1D" + struct.pack("<IB", caster_id, walk_action)
        assert wield in rows and wield in peer_rows
    return rows, peer_rows


def run(host: str, port: int) -> dict[str, object]:
    caster = fighter = None
    caster_world = fighter_world = False
    try:
        caster = Session(host, port, "test2", CASTER)
        fighter = Session(host, port, FIGHTER_ACCOUNT, FIGHTER)
        room_id, caster_rows, fighter_rows = start_two_player_battle(
            caster, fighter, "Native WORLD Shape Change",
            return_bootstrap=True, opposing_forces=True)
        caster_id, _, _ = actor(caster_rows, CASTER)
        fighter_id, _, _ = actor(caster_rows, FIGHTER)
        _, caster_mp, _, caster_max_mp = status(caster_rows)
        _, fighter_mp, _, fighter_max_mp = status(fighter_rows)
        assert (caster_id, fighter_id) == (3, 12)
        assert caster_max_mp == 400
        assert spell_advertised(caster_rows, SKILL_ID)

        enter_world_pair(caster, fighter, caster_id, fighter_id,
                         caster_mp, caster_max_mp,
                         fighter_mp, fighter_max_mp)
        caster_world = fighter_world = True
        drain(caster, 0.2)
        drain(fighter, 0.2)
        world_mp = caster_mp - 10

        # Put the transformed caster beside the retained Ogre so the native
        # bow-only 20/21 action family is consumed by ordinary-WORLD combat.
        caster_walk = walk(caster, fighter, PRIMARY_ROUTE,
                           (33051, 32337), PRIMARY_CELL)
        fighter_walk = walk(fighter, caster, SECONDARY_ROUTE,
                            (33051, 32337), SECONDARY_CELL)
        drain(caster, 0.2)
        drain(fighter, 0.2)

        gem = next(row for row in caster.inventory_records
                   if row[3] == "Magic Gem")
        ring = next(row for row in caster.inventory_records
                    if row[3].endswith("$261"))
        sword = next(row for row in caster.inventory_records
                     if row[3].endswith("$100"))
        scroll = next(row for row in caster.inventory_records if row[1] == 16)
        assert gem[2] >= 3

        # Target without Polymorph Control ability2 receives the retained
        # 21-row weighted random form, not the monlist UI.
        random_request = request(fighter_id)
        caster.send(random_request)
        random_rows = drain(caster, 0.65)
        random_peer = drain(fighter, 0.35)
        assert_cast(random_rows, random_peer, caster_id, fighter_id,
                    gem[0], gem[2] - 1, world_mp - MP_COST, caster_max_mp)
        assert not any(row[0] == 0x2A for row in random_peer)
        appearances = [row for row in random_peer
                       if row[0] == 0x27
                       and row[1:5] == struct.pack("<I", fighter_id)]
        assert len(appearances) == 1, [row.hex() for row in random_peer]
        random_gfx = struct.unpack_from("<H", appearances[0], 5)[0]
        assert random_gfx in RANDOM_GFX
        assert appearances[0] in random_rows

        equip_ring(caster, fighter, ring[0])
        equip_sword(caster, fighter, caster_id, sword[0])

        menu_request = request(caster_id)
        caster.send(menu_request)
        menu_rows = drain(caster, 0.65)
        menu_peer = drain(fighter, 0.35)
        assert_cast(menu_rows, menu_peer, caster_id, caster_id,
                    gem[0], gem[2] - 2,
                    world_mp - MP_COST * 2, caster_max_mp)
        menus = [row for row in menu_rows if row[0] == 0x2A
                 and decode_hypertext(row) == (caster_id, "monlist", "", [])]
        assert len(menus) == 1 and menus[0] not in menu_peer

        selected_rows, selected_peer = select_world_form(
            caster, fighter, caster_id,
            "re skeleton archer", 3871, 20, sword[0])
        assert selected_rows and selected_peer

        # The retained ShapeChange flag is a one-shot capability. Replaying
        # the same native C_HACTION without another successful cast must not
        # publish a second appearance or consume a newer menu token.
        replay_request = (b"\x27" + struct.pack("<I", caster_id)
                          + z("re skeleton archer") + z(""))
        caster.send(replay_request)
        replay_rows = drain(caster, 0.45)
        replay_peer = drain(fighter, 0.25)
        assert not any(row[0] in (0x27, 0x2A) for row in replay_rows)
        assert replay_peer == []

        hit, hit_rows, hit_peer = attack_ogre(caster, fighter, caster_id)
        attack_packet = bytes.fromhex(str(hit["packet"]))
        assert attack_packet[1] == 21, attack_packet.hex()
        # The deterministic hit is admitted (d20=20/d100=1), but forced
        # sword takeoff leaves this mage with source damage 2 against the
        # retained Ogre's reduction 4.  Zero damage is therefore the exact
        # defensive result, not a movement action or a miss.
        assert int(hit["damage"]) == 0
        assert attack_packet in hit_rows and attack_packet in hit_peer

        caster.send(menu_request)
        blocked_form_cast = drain(caster, 0.65)
        blocked_form_peer = drain(fighter, 0.35)
        assert_cast(blocked_form_cast, blocked_form_peer,
                    caster_id, caster_id, gem[0], gem[2] - 3,
                    world_mp - MP_COST * 3, caster_max_mp)
        assert any(row[0] == 0x2A for row in blocked_form_cast)
        select_world_form(caster, fighter, caster_id,
                          "re giant ant", 1037, 0)

        # The selected DB isSkillUse=0 flag must block an ordinary-WORLD
        # spell before action/effect/MP mutation and retain stream alignment.
        caster.send(b"\x14\x00\x00" + struct.pack("<I", caster_id))
        blocked = drain(caster, 0.55)
        blocked_peer = drain(fighter, 0.25)
        assert b"\x25\x07" in blocked
        assert b"\x10\x1D\x01\x00" in blocked
        assert not any(row[0] in (0x20, 0x37, 0x4D) for row in blocked)
        assert blocked_peer == []

        # isSkillUse=0 blocks spells, but the native use-type16 empty option
        # remains the retained escape path. It must restore the WORLD actor to
        # its base appearance for both owner and same-instance observer.
        clear_request = b"\x1C" + struct.pack("<I", scroll[0]) + z("")
        caster.send(clear_request)
        clear_rows = drain(caster, 0.65)
        clear_peer = drain(fighter, 0.35)
        inventory_delta(clear_rows, scroll[0], scroll[2] - 1)
        restored = [row for row in clear_rows
                    if row[0] == 0x27
                    and row[1:5] == struct.pack("<I", caster_id)]
        assert len(restored) == 1 and restored[0] in clear_peer
        assert struct.unpack_from("<H", restored[0], 5)[0] != 1037

        caster.send(b"\x71\x00")
        assert caster.receive(0x78) == b"\x78\x00"
        assert fighter.receive_optional(0.15) is None

        all_rows = (random_rows + random_peer + menu_rows + menu_peer
                    + selected_rows + selected_peer + replay_rows + replay_peer
                    + hit_rows + hit_peer + blocked_form_cast + blocked_form_peer
                    + blocked + clear_rows + clear_peer)
        assert not any(row[0] == 0xFA for row in all_rows)
        for session in (caster, fighter):
            exit_world(session)
        caster_world = fighter_world = False
        return {
            "transport": "encrypted LiTo 3.81C loopback framing",
            "endpoint": f"{host}:{port}",
            "phase": "WORLD",
            "room_ids": [room_id],
            "skill_id": SKILL_ID,
            "selector": list(SELECTOR),
            "wire_layout": "C_USE_SPELL cccd (target object dword)",
            "requests": {
                "random_target": random_request.hex(" ").upper(),
                "controlled_self": menu_request.hex(" ").upper(),
            },
            "no_control_ring": {
                "weighted_rows": 21,
                "total_weight": 1_000_000,
                "target_object_id": fighter_id,
                "applied_gfx": random_gfx,
            },
            "control_ring": {
                "catalog_id": 20281,
                "ability_type": 2,
                "html": "monlist",
                "one_shot_world_token": True,
                "stale_replay_packets": len(replay_rows) + len(replay_peer),
            },
            "first_form": {
                "selection": "re skeleton archer",
                "gfx": 3871,
                "walk_action": 20,
                "world_attack_action": 21,
                "world_attack_damage": int(hit["damage"]),
                "world_attack_hit": True,
                "source_damage": 2,
                "ogre_damage_reduction": 4,
                "forced_sword_unequip": True,
            },
            "second_form": {
                "selection": "re giant ant",
                "gfx": 1037,
                "skill_usable": False,
                "blocked_message": 285,
            },
            "world_restore": {
                "request": clear_request.hex(" ").upper(),
                "use_type": 16,
                "same_instance_publication": True,
                "restored_gfx": struct.unpack_from("<H", restored[0], 5)[0],
            },
            "resources": {
                "action": ACTION,
                "effect_gfx": EFFECT_GFX,
                "duration_seconds": DURATION_SECONDS,
                "caster_mp": [world_mp, world_mp - MP_COST,
                              world_mp - MP_COST * 2,
                              world_mp - MP_COST * 3],
                "magic_gems": [gem[2], gem[2] - 1,
                               gem[2] - 2, gem[2] - 3],
            },
            "route_packets": {
                "caster": len(caster_walk),
                "fighter": len(fighter_walk),
            },
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
    print("PASS WORLD skill67 exact cccd player target; weighted random form; "
          "control-ring monlist token; bow 20/21 and forced unequip; "
          "isSkillUse=0 block285; no FA; encrypted stream aligned")


if __name__ == "__main__":
    main()
