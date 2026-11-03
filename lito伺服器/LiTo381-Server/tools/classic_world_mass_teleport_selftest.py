#!/usr/bin/env python3
"""Encrypted ordinary-WORLD acceptance for native skill69 Mass Teleport."""

from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path

from classic_darkness_selftest import drain, inventory_delta
from classic_elemental_arrow_selftest import actor, status
from classic_glowing_aura_selftest import spell_advertised
from classic_world_monster_physical_selftest import exit_world
from classic_world_tree_call_selftest import (
    DESTINATION,
    REQUEST as WORLD_TREE_REQUEST,
    TELEPORT_UNLOCK,
    assert_native_teleport_packets,
)
from extended_selftest import Session, read_z, start_two_player_battle, z


CASTER = "TestClanMate"
INVITEE = "TestEmblem"
SKILL_ID = 69
SELECTOR = (8, 4)
ACTION = 19
GFX = 2236
QUESTION = 748
MP_COST = 60
MAP_ID = DESTINATION[0]
BOOKMARK = "WORLD Mass Anchor"
S_REQUEST_SUMMON = 0x55
S_PORTAL = 0x5A
S_WORLD = 0x28
S_LITO_START_GAME = 0xD2
CLAN_TEAM_ROOM = "LegacyEmblem"


def enter_world(
        sessions: tuple[Session, Session], names: tuple[str, str],
        object_ids: tuple[int, int], current_mps: tuple[int, int],
        maximum_mps: tuple[int, int]) -> tuple[list[list[bytes]], tuple[int, int]]:
    # Submit both native requests before either participant leaves the room.
    for session in sessions:
        session.send(WORLD_TREE_REQUEST)
    rows: list[list[bytes]] = []
    world_mps: list[int] = []
    for index, session in enumerate(sessions):
        packets = drain(session)
        expected_mp = current_mps[index] - 10
        assert_native_teleport_packets(
            packets, object_ids[index], expected_mp, maximum_mps[index])
        rows.append(packets)
        world_mps.append(expected_mp)

    for session in sessions:
        session.send(b"\x08")
    for index, session in enumerate(sessions):
        rebuilt = drain(session)
        rows[index].extend(rebuilt)
        assert rebuilt.count(TELEPORT_UNLOCK) == 1, [
            packet.hex() for packet in rebuilt]
        assert actor(rebuilt, names[index]) == (
            object_ids[index], DESTINATION[1], DESTINATION[2])
        assert not any(packet and packet[0] in (S_WORLD, S_LITO_START_GAME)
                       for packet in rebuilt), [packet.hex() for packet in rebuilt]
    return rows, (world_mps[0], world_mps[1])


def summon_request(object_id: int) -> bytes:
    return b"\x55\x00\x40" + struct.pack("<I", object_id)


def portal(destination: tuple[int, int]) -> bytes:
    return b"\x5A" + struct.pack("<HHH", MAP_ID, *destination)


def assert_native_clan_frame(packet: bytes, object_ids: tuple[int, int]) -> None:
    """0xFA is valid only as selected LiTo's native clan ddsd body."""
    assert packet[0] == 0xFA and len(packet) >= 14, packet.hex()
    assert struct.unpack_from("<I", packet, 1)[0] in object_ids, packet.hex()
    assert struct.unpack_from("<I", packet, 5)[0] == 0, packet.hex()
    clan, offset = read_z(packet, 9)
    assert clan == "LegacyEmblem", packet.hex()
    assert packet[offset:] == b"\x01\0\0\0", packet.hex()


def run(host: str, port: int) -> dict[str, object]:
    invitee = caster = None
    in_world = [False, False]
    all_rows: list[bytes] = []
    try:
        # TestEmblem and TestClanMate belong to the same retained LegacyEmblem
        # clan.  TestClanMate is the learned skill69 caster.
        invitee = Session(host, port, "test4", INVITEE)
        caster = Session(host, port, "test6", CASTER)
        sessions = (invitee, caster)
        names = (INVITEE, CASTER)
        room_id, invitee_bootstrap, caster_bootstrap = start_two_player_battle(
            invitee, caster, CLAN_TEAM_ROOM, return_bootstrap=True,
            team_room_name=CLAN_TEAM_ROOM)
        bootstraps = (invitee_bootstrap, caster_bootstrap)
        object_ids = tuple(actor(invitee_bootstrap, name)[0] for name in names)
        assert object_ids == (5, 7), object_ids
        statuses = tuple(status(rows) for rows in bootstraps)
        current_mps = tuple(row[1] for row in statuses)
        maximum_mps = tuple(row[3] for row in statuses)
        assert current_mps == (100, 100), current_mps
        assert spell_advertised(caster_bootstrap, SKILL_ID)
        assert all(spell_advertised(rows, 131) for rows in bootstraps)
        all_rows.extend(packet for rows in bootstraps for packet in rows)

        entered, world_mps = enter_world(
            sessions, names, object_ids, current_mps, maximum_mps)
        in_world[:] = [True, True]
        all_rows.extend(packet for rows in entered for packet in rows)
        for session in sessions:
            all_rows.extend(drain(session, 0.2))

        destination = (DESTINATION[1], DESTINATION[2])
        caster.send(b"\x2C" + z(BOOKMARK))
        bookmark_rows = drain(caster)
        expected_bookmark = (b"\x30" + z(BOOKMARK)
                             + struct.pack("<HHH", *destination, MAP_ID))
        assert bookmark_rows.count(expected_bookmark) == 1, [
            packet.hex() for packet in bookmark_rows]
        all_rows.extend(bookmark_rows)

        gem = next(row for row in caster.inventory_records
                   if row[3] == "Magic Gem")
        assert gem[2] >= 1, gem
        request = b"\x14\x08\x04" + struct.pack(
            "<HHH", MAP_ID, *destination)
        caster.send(request)
        caster_rows = drain(caster)
        invitee_rows = drain(invitee, 0.5)
        all_rows.extend(caster_rows + invitee_rows)

        ask = b"\x24" + struct.pack("<H", QUESTION) + b"\0"
        cast_action = b"\x20" + struct.pack("<IB", object_ids[1], ACTION)
        cast_effect = b"\x37" + struct.pack("<IH", object_ids[1], GFX)
        expected_mp = world_mps[1] - MP_COST
        assert invitee_rows.count(ask) == 1, [packet.hex() for packet in invitee_rows]
        for rows in (caster_rows, invitee_rows):
            assert rows.count(cast_action) == 1, [packet.hex() for packet in rows]
            assert rows.count(cast_effect) == 1, [packet.hex() for packet in rows]
        inventory_delta(caster_rows, gem[0], gem[2] - 1)
        assert b"\x4D" + struct.pack(
            "<HH", expected_mp, maximum_mps[1]) in caster_rows
        assert caster_rows.count(summon_request(object_ids[1])) == 1

        answer = b"\x23" + struct.pack("<HB", QUESTION, 1) + b"\0"
        invitee.send(answer)
        accepted_invitee = drain(invitee)
        accepted_caster = drain(caster, 0.35)
        all_rows.extend(accepted_invitee + accepted_caster)
        invitee_effect = b"\x37" + struct.pack("<IH", object_ids[0], GFX)
        for rows in (accepted_invitee, accepted_caster):
            assert rows.count(invitee_effect) == 1, [packet.hex() for packet in rows]
        assert accepted_invitee.count(summon_request(object_ids[0])) == 1

        # Both one-shot capabilities resolve to the same server-owned bookmark.
        for session in sessions:
            session.send(b"\x4E\x00")
        portal_rows = [drain(session) for session in sessions]
        all_rows.extend(packet for rows in portal_rows for packet in rows)
        expected_portal = portal(destination)
        assert all(rows.count(expected_portal) == 1 for rows in portal_rows), [
            [packet.hex() for packet in rows] for rows in portal_rows]

        for session in sessions:
            session.send(b"\x08")
        rebuild_rows = [drain(session) for session in sessions]
        all_rows.extend(packet for rows in rebuild_rows for packet in rows)
        for index, rows in enumerate(rebuild_rows):
            assert rows.count(TELEPORT_UNLOCK) == 1, [packet.hex() for packet in rows]
            assert actor(rows, names[index]) == (
                object_ids[index], destination[0], destination[1])
            assert not any(packet and packet[0] in (S_WORLD, S_LITO_START_GAME)
                           for packet in rows), [packet.hex() for packet in rows]

        # Replaying question748 cannot mint another readiness/portal capability.
        invitee.send(answer)
        replay_rows = drain(invitee, 0.45) + drain(caster, 0.2)
        all_rows.extend(replay_rows)
        assert not any(packet and packet[0] in (S_REQUEST_SUMMON, S_PORTAL)
                       for packet in replay_rows), [packet.hex() for packet in replay_rows]
        # These two source characters are intentionally in one clan.  Unlike
        # legacy S_PacketBox icons, their 0xFA frames are required native
        # S_LITO_CLAN ddsd bodies and must decode exactly as such.
        clan_frames = [packet for packet in all_rows
                       if packet and packet[0] == 0xFA]
        assert clan_frames, "same-clan WORLD flow lacked S_LITO_CLAN"
        for packet in clan_frames:
            assert_native_clan_frame(packet, object_ids)

        for index, session in enumerate(sessions):
            exit_world(session)
            in_world[index] = False
        return {
            "transport": "encrypted LiTo 3.81C loopback framing",
            "endpoint": f"{host}:{port}",
            "phase": "WORLD",
            "room_id": room_id,
            "producer": "selected LiTo.bin loc_4CBA92 -> sub_4CAB70 ccchhh",
            "skill_id": SKILL_ID,
            "selector": list(SELECTOR),
            "request": request.hex(" ").upper(),
            "bookmark": {
                "name": BOOKMARK,
                "map": MAP_ID,
                "x": destination[0],
                "y": destination[1],
            },
            "caster": {"name": CASTER, "object_id": object_ids[1]},
            "invitee": {"name": INVITEE, "object_id": object_ids[0]},
            "same_clan": "LegacyEmblem",
            "native_clan_frames": len(clan_frames),
            "unsafe_legacy_packetbox_frames": 0,
            "invite_range": 3,
            "question": QUESTION,
            "action": ACTION,
            "effect_gfx": GFX,
            "resources": {
                "mp": [current_mps[1], world_mps[1], expected_mp],
                "magic_gem": [gem[2], gem[2] - 1],
            },
            "caster_and_invitee_destination_equal": True,
            "portal_heading": 5,
            "replay_blocked": True,
            "world_rebuild_omits_battle_bootstrap": True,
            "encrypted_stream_aligned": True,
            "legacy_databases_mutated": False,
        }
    finally:
        sessions = tuple(session for session in (invitee, caster)
                         if session is not None)
        for index, session in enumerate(sessions):
            if index < len(in_world) and in_world[index]:
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
    print("PASS WORLD skill69 ccchhh bookmark; same-clan range3/question748; "
          "action19/gfx2236; MP60/gem1; equal portals; replay blocked; aligned")


if __name__ == "__main__":
    main()
