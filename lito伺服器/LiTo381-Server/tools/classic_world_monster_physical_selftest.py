#!/usr/bin/env python3
"""Encrypted WORLD Ogre Type=1 box-attack acceptance."""

from __future__ import annotations

import argparse
import json
import struct
import time
from collections.abc import Callable
from pathlib import Path

from classic_darkness_selftest import drain
from classic_elemental_arrow_selftest import actor, status
from classic_world_tree_call_selftest import (
    DESTINATION,
    REQUEST,
    TELEPORT_UNLOCK,
    assert_native_teleport_packets,
    move,
)
from extended_selftest import Session, start_two_player_battle


OGRE = (0x65001012, 566, 18, 45278, 33314, 32339, "$759")
PRIMARY_CELL = (33313, 32339)
SECONDARY_CELL = (33312, 32339)
PRIMARY_ROUTE = (
    "001111111111111111111113221111111111311221111111111111111111111111111111111111111111111111111111111111111111111111111111111223333333333333333333333333333333333333333333333333333333333333233333333333333333333333333333333333333333333112222332113121333333333333321333"
)
SECONDARY_ROUTE = (
    "00111111111111111111111322111111111131122111111111111111111111111111111111111111111111111111111111111111111111111111111111122333333333333333333333333333333333333333333333333333333333333323333333333333333333333333333333333333333333311222233211312133333333333332233"
)
DX = (0, 1, 1, 1, 0, -1, -1, -1)
DY = (-1, -1, 0, 1, 1, 1, 0, -1)


def enter_world(session: Session, character: str,
                object_id: int, current_mp: int,
                maximum_mp: int) -> tuple[tuple[int, int], list[bytes]]:
    session.send(REQUEST)
    portal = drain(session)
    assert_native_teleport_packets(
        portal, object_id, current_mp - 10, maximum_mp)
    session.send(b"\x08")
    rebuilt = drain(session)
    assert rebuilt.count(TELEPORT_UNLOCK) == 1
    rebuilt_id, x, y = actor(rebuilt, character)
    assert (rebuilt_id, x, y) == (object_id, DESTINATION[1], DESTINATION[2])
    return (x, y), portal + rebuilt


def hide(session: Session, object_id: int) -> list[bytes]:
    invisibility_on = b"\x34" + struct.pack("<IB", object_id, 1)
    session.send(b"\x14\x07\x03" + struct.pack("<I", 1))
    packets = drain(session, 0.3)
    assert invisibility_on in packets, [row.hex() for row in packets]
    return packets


def walk(session: Session, observer: Session, route: str,
         start: tuple[int, int], destination: tuple[int, int]) -> list[bytes]:
    x, y = start
    packets: list[bytes] = []
    for index, digit in enumerate(route, 1):
        heading = int(digit)
        # Native C_MOVE chhc carries the current/source tile.  The server and
        # LiTo.bin derive the adjacent destination from the heading.
        session.send(move(x, y, heading))
        x += DX[heading]
        y += DY[heading]
        if index % 16 == 0:
            packets.extend(drain(session, 0.03))
            drain(observer, 0.01)
    packets.extend(drain(session, 0.5))
    drain(observer, 0.1)
    assert (x, y) == destination, (x, y, destination)
    return packets


def parse_attack(packet: bytes) -> dict[str, int]:
    assert len(packet) == 16 and packet[0] == 0x23, packet.hex()
    return {
        "action": packet[1],
        "attacker_object_id": struct.unpack_from("<I", packet, 2)[0],
        "target_object_id": struct.unpack_from("<I", packet, 6)[0],
        "damage": packet[10],
        "heading": packet[11],
        "sequence": struct.unpack_from("<I", packet, 12)[0],
    }


def latest_hp(packets: list[bytes], maximum_hp: int, fallback: int) -> int:
    values = [struct.unpack_from("<HH", packet, 1)[0]
              for packet in packets
              if len(packet) == 5 and packet[0] == 0x0D
              and struct.unpack_from("<H", packet, 3)[0] == maximum_hp]
    return values[-1] if values else fallback


def exit_world(session: Session) -> None:
    session.send(b"\x71\x00")
    deadline = time.monotonic() + 2.0
    while time.monotonic() < deadline:
        packet = session.receive_optional(0.1)
        if packet is None:
            continue
        if packet[0] == 0x78:
            assert packet == b"\x78\x00", packet.hex()
            return
    raise AssertionError("WORLD exit lacked S_EXIT_GAME")


def acceptance(
        host: str, port: int,
        before_attack: Callable[[Session, Session, list[bytes], list[bytes]], None]
        | None = None,
        expected_damage: int = 42) -> dict[str, object]:
    primary = Session(host, port, "test15", "TestWeaponDummy")
    primary_peer = Session(host, port, "test5", "TestElf")
    secondary = None
    secondary_peer = None
    try:
        primary_room_id, primary_rows, _ = start_two_player_battle(
            primary, primary_peer, "Native WORLD Monster Physical A",
            return_bootstrap=True)
        primary_id, _, _ = actor(primary_rows, "TestWeaponDummy")
        primary_hp, primary_mp, primary_max_hp, primary_max_mp = status(primary_rows)
        assert (primary_id, primary_hp, primary_max_hp) == (16, 2000, 2000)

        primary_start, primary_pre_packets = enter_world(
            primary, "TestWeaponDummy", primary_id, primary_mp, primary_max_mp)
        drain(primary_peer, 0.6)
        primary_pre_packets.extend(hide(primary, primary_id))
        primary_peer.close()

        secondary = Session(host, port, "test2", "TestMage")
        secondary_peer = Session(host, port, "test", "TestKnight")
        secondary_room_id, secondary_rows, _ = start_two_player_battle(
            secondary, secondary_peer, "Native WORLD Monster Physical B",
            return_bootstrap=True)
        secondary_id, _, _ = actor(secondary_rows, "TestMage")
        secondary_hp, secondary_mp, secondary_max_hp, secondary_max_mp = status(
            secondary_rows)
        assert secondary_hp == secondary_max_hp
        secondary_start, secondary_pre_packets = enter_world(
            secondary, "TestMage", secondary_id, secondary_mp, secondary_max_mp)
        drain(secondary_peer, 0.6)
        secondary_pre_packets.extend(hide(secondary, secondary_id))
        secondary_peer.close()
        primary_pre_packets.extend(drain(primary, 0.2))

        primary_walk = walk(primary, secondary, PRIMARY_ROUTE,
                            primary_start, PRIMARY_CELL)
        secondary_walk = walk(secondary, primary, SECONDARY_ROUTE,
                              secondary_start, SECONDARY_CELL)
        primary_pre_packets.extend(primary_walk)
        secondary_pre_packets.extend(secondary_walk)
        observed = primary_walk + secondary_walk
        exact_actor = next(
            row for row in observed
            if row[0] == 0x0B and row[5:9] == struct.pack("<I", OGRE[0]))
        assert struct.unpack_from("<HH", exact_actor, 1) == (OGRE[4], OGRE[5])

        primary_pre_packets.extend(drain(primary, 0.2))
        secondary_pre_packets.extend(drain(secondary, 0.2))
        invisibility_off = b"\x34" + struct.pack("<IB", primary_id, 0)
        if before_attack is not None:
            before_attack(primary, secondary,
                          primary_pre_packets, secondary_pre_packets)
        primary_before = latest_hp(
            primary_pre_packets, primary_max_hp, primary_hp)
        secondary_before = latest_hp(
            secondary_pre_packets, secondary_max_hp, secondary_hp)
        revealed_before_attack = invisibility_off in primary_pre_packets
        if not revealed_before_attack:
            primary.send(b"\x14\x07\x03" + struct.pack("<I", 2))

        primary_packets: list[bytes] = []
        secondary_packets: list[bytes] = []
        primary_after = None
        secondary_after = None
        attack_packet = None
        primary_saw_attack = False
        secondary_saw_attack = False
        deadline = time.monotonic() + 4.0
        while time.monotonic() < deadline and (
                primary_after is None or secondary_after is None
                or attack_packet is None):
            for session, packets, is_primary in (
                    (primary, primary_packets, True),
                    (secondary, secondary_packets, False)):
                packet = session.receive_optional(0.04)
                if packet is None:
                    continue
                packets.append(packet)
                if (packet[0] == 0x23 and len(packet) == 16
                        and packet[2:6] == struct.pack("<I", OGRE[0])):
                    if is_primary:
                        primary_saw_attack = True
                        if attack_packet is None:
                            attack_packet = packet
                    else:
                        secondary_saw_attack = True
                if len(packet) == 5 and packet[0] == 0x0D:
                    hp, maximum = struct.unpack_from("<HH", packet, 1)
                    if is_primary and maximum == primary_max_hp:
                        if primary_saw_attack and primary_after is None:
                            primary_after = hp
                        elif not primary_saw_attack:
                            primary_before = hp
                    elif not is_primary and maximum == secondary_max_hp:
                        if secondary_saw_attack and secondary_after is None:
                            secondary_after = hp
                        elif not secondary_saw_attack:
                            secondary_before = hp

        assert revealed_before_attack or invisibility_off in primary_packets, [
            row.hex() for row in primary_packets]
        assert attack_packet is not None, [row.hex() for row in primary_packets]
        parsed = parse_attack(attack_packet)
        assert parsed == {
            "action": 30,
            "attacker_object_id": OGRE[0],
            "target_object_id": primary_id,
            "damage": expected_damage,
            "heading": 6,
            "sequence": 0,
        }, parsed
        assert primary_after == primary_before - expected_damage, (
            primary_before, primary_after)
        assert secondary_after == secondary_before - expected_damage, (
            secondary_before, secondary_after)

        all_packets = primary_packets + secondary_packets
        ogre_attacks = [row for row in all_packets
                        if len(row) == 16 and row[0] == 0x23
                        and row[2:6] == struct.pack("<I", OGRE[0])]
        assert ogre_attacks
        assert all(struct.unpack_from("<I", row, 6)[0] == primary_id
                   for row in ogre_attacks), [row.hex() for row in ogre_attacks]
        assert not any(row[0] == 0x2E for row in all_packets)
        assert not any(row[0] == 0x37 and row[1:5] == struct.pack("<I", OGRE[0])
                       for row in all_packets)

        exit_world(primary)
        exit_world(secondary)
        # Leave both encrypted sockets open until the 50 ms AI clock has
        # observed their post-WORLD phase and discarded its member snapshot.
        time.sleep(0.3)
        return {
            "room_ids": [primary_room_id, secondary_room_id],
            "primary_object_id": primary_id,
            "secondary_object_id": secondary_id,
            "world_entry": {"map": DESTINATION[0],
                            "x": DESTINATION[1], "y": DESTINATION[2]},
            "monster": {
                "object_id": OGRE[0], "spawn_id": OGRE[1],
                "spawn_number": OGRE[2], "npc_id": OGRE[3],
                "name_id": OGRE[6], "x": OGRE[4], "y": OGRE[5],
            },
            "primary_cell": list(PRIMARY_CELL),
            "secondary_cell": list(SECONDARY_CELL),
            "route_steps": {
                "primary": len(PRIMARY_ROUTE),
                "secondary": len(SECONDARY_ROUTE),
            },
            "fixture": {"branch": 1, "trigger_roll": 1,
                        "level_roll": 1, "critical_roll": 99},
            "source_damage": expected_damage,
            "mobskill_leverage": 20,
            "packet": attack_packet.hex(" ").upper(),
            "decoded": parsed,
            "hp": {
                "primary": [primary_hp, primary_before, primary_after],
                "secondary": [secondary_hp, secondary_before, secondary_after],
            },
            "primary_action_targets": sorted(set(
                struct.unpack_from("<I", row, 6)[0] for row in ogre_attacks)),
            "attack_all_packets": 0,
            "secondary_attack_actions": 0,
            "encrypted_stream_aligned": True,
        }
    finally:
        primary.close()
        try:
            primary_peer.close()
        except OSError:
            pass
        if secondary is not None:
            secondary.close()
        if secondary_peer is not None:
            try:
                secondary_peer.close()
            except OSError:
                pass


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.2")
    parser.add_argument("--port", type=int, default=32100)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    evidence = {
        "transport": "encrypted LiTo 3.81C loopback framing",
        "endpoint": f"{args.host}:{args.port}",
        "classic_world_monster_physical": acceptance(args.host, args.port),
    }
    if args.output:
        args.output.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
    print("PASS WORLD NPC Ogre Type=1; primary S_ATTACK action30; "
          "two independent 42-HP commits; S_ATTACK_ALL=0; stream aligned")


if __name__ == "__main__":
    main()
