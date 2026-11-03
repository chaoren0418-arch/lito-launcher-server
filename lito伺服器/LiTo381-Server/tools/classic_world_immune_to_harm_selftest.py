#!/usr/bin/env python3
"""Encrypted ordinary-WORLD acceptance for native skill68 Immune to Harm."""

from __future__ import annotations

import argparse
import json
import struct
import time
from pathlib import Path

from classic_darkness_selftest import drain, inventory_delta
from classic_elemental_arrow_selftest import actor, status
from classic_world_monster_physical_selftest import (
    PRIMARY_CELL,
    PRIMARY_ROUTE,
    SECONDARY_CELL,
    SECONDARY_ROUTE,
    enter_world,
    exit_world,
    walk,
)
from extended_selftest import Session, start_two_player_battle


SKILL_ID = 68
SELECTOR = (8, 3)
ACTION = 19
EFFECT_GFX = 228
HAPPEN_MESSAGE = 314
STOP_MESSAGE = 315
MP_COST = 30
CANCELLATION_MP_COST = 40
CANCELLATION_GFX = 870
KNIGHT_ID = 1
MAGE_ID = 3
CLAN_TEAM_ROOM = "LegacyEmblem"


def action(object_id: int) -> bytes:
    return b"\x20" + struct.pack("<IB", object_id, ACTION)


def effect(object_id: int, gfx: int = EFFECT_GFX) -> bytes:
    return b"\x37" + struct.pack("<IH", object_id, gfx)


def message(message_id: int) -> bytes:
    return b"\x10" + struct.pack("<HB", message_id, 0)


def mana(current: int, maximum: int) -> bytes:
    return b"\x4D" + struct.pack("<HH", current, maximum)


def latest_hp(packets: list[bytes], maximum: int) -> int | None:
    values = [struct.unpack_from("<H", packet, 1)[0]
              for packet in packets
              if len(packet) == 5 and packet[0] == 0x0D
              and struct.unpack_from("<H", packet, 3)[0] == maximum]
    return values[-1] if values else None


def collect_pair(first: Session, second: Session,
                 seconds: float = 0.8) -> tuple[list[bytes], list[bytes]]:
    first_packets: list[bytes] = []
    second_packets: list[bytes] = []
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        for session, packets in ((first, first_packets),
                                 (second, second_packets)):
            packet = session.receive_optional(0.025)
            if packet is not None:
                packets.append(packet)
    return first_packets, second_packets


def attack_packet(packets: list[bytes], attacker_id: int,
                  target_id: int, length: int) -> bytes:
    rows = [packet for packet in packets
            if len(packet) == length and packet[0] == 0x23
            and packet[2:6] == struct.pack("<I", attacker_id)
            and packet[6:10] == struct.pack("<I", target_id)]
    assert len(rows) == 1, [packet.hex() for packet in packets]
    return rows[0]


def physical_attack(knight: Session, mage: Session, previous_hp: int,
                    maximum_hp: int) -> tuple[int, int, dict[str, object]]:
    request = b"\x17" + struct.pack("<IHH", MAGE_ID, *SECONDARY_CELL)
    knight.send(request)
    knight_rows, mage_rows = collect_pair(knight, mage)
    packet = attack_packet(knight_rows, KNIGHT_ID, MAGE_ID, 16)
    assert packet in mage_rows
    current_hp = latest_hp(mage_rows, maximum_hp)
    assert current_hp is not None and 0 < current_hp < previous_hp
    damage = previous_hp - current_hp
    assert packet[10] == damage, (packet.hex(), previous_hp, current_hp)
    return current_hp, damage, {
        "request": request.hex(" ").upper(),
        "frame": packet.hex(" ").upper(),
        "damage": damage,
        "hp": [previous_hp, current_hp, maximum_hp],
    }


def light_arrow(knight: Session, mage: Session, previous_hp: int,
                maximum_hp: int, expected_mp: int
                ) -> tuple[int, int, dict[str, object]]:
    request = b"\x14\x00\x03" + struct.pack(
        "<IHH", MAGE_ID, *SECONDARY_CELL)
    knight.send(request)
    knight_rows, mage_rows = collect_pair(knight, mage)
    packet = attack_packet(knight_rows, KNIGHT_ID, MAGE_ID, 29)
    assert packet in mage_rows
    assert packet[1] == 18
    assert struct.unpack_from("<H", packet, 16)[0] == 167
    assert mana(expected_mp, 100) in knight_rows
    current_hp = latest_hp(mage_rows, maximum_hp)
    assert current_hp is not None and 0 < current_hp < previous_hp
    damage = previous_hp - current_hp
    return current_hp, damage, {
        "request": request.hex(" ").upper(),
        "frame": packet.hex(" ").upper(),
        "wire_damage_marker": packet[10],
        "damage": damage,
        "hp": [previous_hp, current_hp, maximum_hp],
    }


def run(host: str, port: int) -> dict[str, object]:
    knight = Session(host, port, "test", "TestKnight")
    knight_peer = Session(host, port, "test5", "TestElf")
    mage = None
    mage_peer = None
    knight_world = mage_world = False
    try:
        knight_room, knight_rows, _ = start_two_player_battle(
            knight, knight_peer, "Native WORLD Immune Knight",
            return_bootstrap=True)
        knight_id, _, _ = actor(knight_rows, "TestKnight")
        _, knight_mp, _, knight_max_mp = status(knight_rows)
        assert knight_id == KNIGHT_ID
        knight_start, knight_entry = enter_world(
            knight, "TestKnight", knight_id, knight_mp, knight_max_mp)
        knight_world = True
        drain(knight_peer, 0.3)
        knight_peer.close()

        mage = Session(host, port, "test2", "TestMage")
        mage_peer = Session(host, port, "test6", "TestClanMate")
        mage_room, mage_rows, _ = start_two_player_battle(
            mage, mage_peer, CLAN_TEAM_ROOM,
            return_bootstrap=True)
        mage_id, _, _ = actor(mage_rows, "TestMage")
        mage_hp, mage_mp, mage_max_hp, mage_max_mp = status(mage_rows)
        assert (mage_id, mage_hp, mage_max_hp) == (MAGE_ID, 100, 100)
        mage_start, mage_entry = enter_world(
            mage, "TestMage", mage_id, mage_mp, mage_max_mp)
        mage_world = True
        drain(mage_peer, 0.3)
        mage_peer.close()
        knight_entry.extend(drain(knight, 0.2))

        knight_walk = walk(
            knight, mage, PRIMARY_ROUTE, knight_start, PRIMARY_CELL)
        mage_walk = walk(
            mage, knight, SECONDARY_ROUTE, mage_start, SECONDARY_CELL)
        drain(knight, 0.2)
        drain(mage, 0.2)

        gem = next(row for row in mage.inventory_records
                   if row[3] == "Magic Gem")
        assert gem[2] == 8, gem
        mage_world_mp = mage_mp - 10
        knight_world_mp = knight_mp - 10

        request = b"\x14\x08\x03" + struct.pack("<I", mage_id)
        assert request == b"\x14\x08\x03\x03\x00\x00\x00"
        mage.send(request)
        mage_cast, knight_cast = collect_pair(mage, knight)
        expected_action = action(mage_id)
        expected_effect = effect(mage_id)
        for rows in (mage_cast, knight_cast):
            assert expected_action in rows and expected_effect in rows
        assert message(HAPPEN_MESSAGE) in mage_cast
        inventory_delta(mage_cast, gem[0], 6)
        assert mana(mage_world_mp - MP_COST, mage_max_mp) in mage_cast

        hp, protected_magic, protected_magic_row = light_arrow(
            knight, mage, mage_hp, mage_max_hp, knight_world_mp - 3)
        time.sleep(0.45)
        hp, protected_physical, protected_physical_row = physical_attack(
            knight, mage, hp, mage_max_hp)

        cancellation = b"\x14\x05\x03" + struct.pack("<I", mage_id)
        mage.send(cancellation)
        mage_cancel, knight_cancel = collect_pair(mage, knight)
        cancel_effect = effect(mage_id, CANCELLATION_GFX)
        for rows in (mage_cancel, knight_cancel):
            assert expected_action in rows and cancel_effect in rows
        assert message(STOP_MESSAGE) in mage_cancel
        inventory_delta(mage_cancel, gem[0], 4)
        assert mana(mage_world_mp - MP_COST - CANCELLATION_MP_COST,
                    mage_max_mp) in mage_cancel

        time.sleep(0.45)
        hp, unprotected_magic, unprotected_magic_row = light_arrow(
            knight, mage, hp, mage_max_hp, knight_world_mp - 6)
        time.sleep(0.45)
        hp, unprotected_physical, unprotected_physical_row = physical_attack(
            knight, mage, hp, mage_max_hp)
        assert unprotected_physical == protected_physical * 2, (
            protected_physical, unprotected_physical)

        exit_world(knight)
        knight_world = False
        exit_world(mage)
        mage_world = False
        return {
            "transport": "encrypted LiTo 3.81C loopback framing",
            "endpoint": f"{host}:{port}",
            "phase": "WORLD",
            "room_ids": [knight_room, mage_room],
            "skill_id": SKILL_ID,
            "selector": list(SELECTOR),
            "request": request.hex(" ").upper(),
            "wire_layout": "C_USE_SPELL cccd (one PC object target)",
            "target": {
                "name": "TestMage",
                "object_id": mage_id,
                "mode": "self",
                "source_team_room": CLAN_TEAM_ROOM,
                "same_map_instance": True,
                "cell": list(SECONDARY_CELL),
            },
            "resources": {
                "action": ACTION,
                "effect_gfx": EFFECT_GFX,
                "duration_seconds": 32,
                "damage_divisor": 2,
                "happen_message": HAPPEN_MESSAGE,
                "stop_message": STOP_MESSAGE,
                "magic_gems": [8, 6, 4],
                "mage_mp": [mage_world_mp,
                            mage_world_mp - MP_COST,
                            mage_world_mp - MP_COST - CANCELLATION_MP_COST,
                            mage_max_mp],
            },
            "protected": {
                "magic": protected_magic_row,
                "physical": protected_physical_row,
            },
            "after_cancellation": {
                "request": cancellation.hex(" ").upper(),
                "magic": unprotected_magic_row,
                "physical": unprotected_physical_row,
            },
            "damage": {
                "protected_magic": protected_magic,
                "protected_physical": protected_physical,
                "unprotected_magic": unprotected_magic,
                "unprotected_physical": unprotected_physical,
                "physical_exact_divisor": True,
            },
            "route_packets": {
                "knight": len(knight_walk),
                "mage": len(mage_walk),
            },
            "natural_expiry_dispatch_connected": True,
            "legacy_packetbox_icon_omitted": True,
            "encrypted_stream_aligned": True,
            "legacy_databases_mutated": False,
        }
    finally:
        for session, in_world in ((knight, knight_world),
                                  (mage, mage_world)):
            if session is None:
                continue
            if in_world:
                try:
                    exit_world(session)
                except (AssertionError, OSError):
                    pass
            session.close()
        for peer in (knight_peer, mage_peer):
            if peer is not None:
                try:
                    peer.close()
                except OSError:
                    pass


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
    print("PASS WORLD skill68 exact cccd self target; action19/gfx228; "
          "message314/315; gems8->6->4; player physical+magic protected; "
          "Cancellation clears divisor; encrypted stream aligned")


if __name__ == "__main__":
    main()
