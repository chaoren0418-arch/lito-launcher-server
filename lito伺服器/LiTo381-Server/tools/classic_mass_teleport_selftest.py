#!/usr/bin/env python3
"""Encrypted TCP acceptance for native skill 69 Mass Teleport."""

from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path

from classic_heal_all_selftest import drain_portal_rebuild
from extended_selftest import (
    Session,
    drain_battle_bootstrap,
    joined_room,
    join_existing_room,
    read_z,
    z,
)
from quest_battle_selftest import actor, assert_map_delta, assert_mode_delta


ACTION = 19
GFX = 2236
QUESTION = 748
MODE_HONOR = 5
MAP_HONOR = 510
MP_BEFORE = 100
MP_COST = 60
CLAN_TEAM_ROOM = "LegacyEmblem"


def inventory_delta(session: Session, object_id: int, count: int) -> None:
    expected = (b"\x17" + struct.pack("<I", object_id) if count == 0
                else b"\x1A" + struct.pack("<II", object_id, count))
    assert session.receive(expected[0]) == expected


def complete_portal(session: Session, object_id: int,
                    destination: tuple[int, int],
                    consume_request: bool = True) -> bytes:
    if consume_request:
        assert session.receive(0x55) == (
            b"\x55\x00\x40" + struct.pack("<I", object_id))
    session.send(b"\x4E\x00")
    portal = session.receive(0x5A)
    assert portal == b"\x5A" + struct.pack("<HHH", MAP_HONOR, *destination), portal.hex()
    session.send(b"\x08")
    drain_portal_rebuild(session)
    return portal


def consume_peer_portal_move(session: Session, object_id: int,
                             destination: tuple[int, int]) -> None:
    assert session.receive(0x12) == b"\x12" + struct.pack(
        "<IHHB", object_id, *destination, 0)
    minimap = session.receive(0xE8)
    assert minimap[1:5] == struct.pack("<I", object_id), minimap.hex()
    assert minimap[7:11] == struct.pack("<HH", *destination), minimap.hex()


def run(host: str, port: int) -> dict[str, object]:
    invitee = Session(host, port, "test4", "TestEmblem")
    caster = Session(host, port, "test6", "TestClanMate")
    try:
        room_id = joined_room(
            caster, CLAN_TEAM_ROOM, team_room_name=CLAN_TEAM_ROOM)
        team_state = invitee.receive(0xF6)
        team_name, team_offset = read_z(team_state, 1)
        assert team_name == CLAN_TEAM_ROOM and team_state[team_offset] == 2
        join_existing_room(invitee, room_id)
        assert caster.receive(0xD0)[1:5] == struct.pack("<I", room_id)
        caster.receive(0xCD)
        invitee.send(b"\xD3\x01")
        for session in (caster, invitee):
            assert session.receive(0xD4)[-1] == 1
        caster.send(b"\xD1\x02\x01")
        for session in (caster, invitee):
            assert_mode_delta(session.receive(0xD5), room_id, MODE_HONOR)
            assert_map_delta(session.receive(0xD5), room_id, MAP_HONOR)
        caster.send(b"\xD2\x01")
        caster.receive(0xD1)
        invitee.receive(0xD1)
        invitee.send(b"\xD2\x01")
        caster.receive(0xD1)
        invitee.receive(0xD1)
        caster_bootstrap = drain_battle_bootstrap(
            caster, countdown=True, expected_actors=15)
        drain_battle_bootstrap(invitee, countdown=True, expected_actors=15)
        actors = {
            row["name"]: row
            for row in (actor(packet) for packet in caster_bootstrap
                        if packet[0] == 0x0B)
            if row["name"] in {"TestEmblem", "TestClanMate"}
        }
        caster_actor = actors["TestClanMate"]
        invitee_actor = actors["TestEmblem"]
        caster_id = caster_actor["object_id"]
        invitee_id = invitee_actor["object_id"]

        # Put the same-clan invitee inside the retained three-tile radius.
        adjacent = (caster_actor["x"] - 1, caster_actor["y"])
        invitee.send(b"\x2E" + struct.pack("<HH", *adjacent))
        complete_portal(invitee, invitee_id, adjacent)
        consume_peer_portal_move(caster, invitee_id, adjacent)

        # The selected client stores no bookmark id.  Its callback returns the
        # exact server-published map/x/y triple, which the cast echoes as hhh.
        bookmark_name = "Mass Anchor"
        destination = (caster_actor["x"], caster_actor["y"])
        caster.send(b"\x2C" + z(bookmark_name))
        expected_bookmark = (b"\x30" + z(bookmark_name)
                             + struct.pack("<HHH", *destination, MAP_HONOR))
        assert caster.receive(0x30) == expected_bookmark

        gem = next(row for row in caster.inventory_records if row[3] == "Magic Gem")
        assert gem[2] >= 1, gem
        request = b"\x14\x08\x04" + struct.pack(
            "<HHH", MAP_HONOR, *destination)
        caster.send(request)

        ask = invitee.receive(0x24)
        assert ask == b"\x24" + struct.pack("<H", QUESTION) + b"\0", ask.hex()
        action = b"\x20" + struct.pack("<IB", caster_id, ACTION)
        effect = b"\x37" + struct.pack("<IH", caster_id, GFX)
        for session in (caster, invitee):
            assert session.receive(0x20) == action
            assert session.receive(0x37) == effect
        inventory_delta(caster, gem[0], gem[2] - 1)
        status = caster.receive(0x0C)
        assert struct.unpack_from("<HHHH", status, 16) == (
            50, MP_BEFORE - MP_COST, 50, MP_BEFORE), status.hex()
        assert caster.receive(0x4D) == b"\x4D" + struct.pack(
            "<HH", MP_BEFORE - MP_COST, MP_BEFORE)
        assert caster.receive(0x55) == (
            b"\x55\x00\x40" + struct.pack("<I", caster_id))

        # Accept once.  The effect precedes the invitee's own readiness
        # handshake and both server-owned destinations must be identical.
        answer = b"\x23" + struct.pack("<HB", QUESTION, 1) + b"\0"
        invitee.send(answer)
        invitee_effect = b"\x37" + struct.pack("<IH", invitee_id, GFX)
        for session in (caster, invitee):
            assert session.receive(0x37) == invitee_effect

        assert invitee.receive(0x55) == (
            b"\x55\x00\x40" + struct.pack("<I", invitee_id))
        caster_portal = complete_portal(
            caster, caster_id, destination, consume_request=False)
        consume_peer_portal_move(invitee, caster_id, destination)
        invitee_portal = complete_portal(
            invitee, invitee_id, destination, consume_request=False)
        consume_peer_portal_move(caster, invitee_id, destination)
        assert caster_portal == invitee_portal

        # Replaying the consumed answer must not create a second portal.
        invitee.send(answer)
        assert invitee.receive_optional(0.45) is None
        caster.send(b"\x71\x00")
        invitee.send(b"\x71\x00")
        assert caster.receive(0x78) == b"\x78\x00"
        assert invitee.receive(0x78) == b"\x78\x00"
        return {
            "room_id": room_id,
            "skill_id": 69,
            "selector": [8, 4],
            "request": request.hex(" ").upper(),
            "bookmark": bookmark_name,
            "map_id": MAP_HONOR,
            "destination": list(destination),
            "caster": "TestClanMate",
            "invitee": "TestEmblem",
            "caster_object_id": caster_id,
            "invitee_object_id": invitee_id,
            "question": QUESTION,
            "action": ACTION,
            "effect_gfx": GFX,
            "mp_before": MP_BEFORE,
            "mp_after": MP_BEFORE - MP_COST,
            "gem_before": gem[2],
            "gem_after": gem[2] - 1,
            "accepted_destinations_equal": caster_portal == invitee_portal,
            "replay_blocked": True,
        }
    finally:
        invitee.close()
        caster.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.2")
    parser.add_argument("--port", type=int, default=32100)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = run(args.host, args.port)
    evidence = {
        "transport": "encrypted LiTo 3.81C loopback framing",
        "endpoint": f"{args.host}:{args.port}",
        "result": result,
    }
    if args.output:
        args.output.write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
    print("PASS native Mass Teleport TCP: ccchhh map/x/y bookmark; "
          "same-clan question748 accepted; caster/invitee portal destinations equal; "
          "MP60 + one Magic Gem consumed; replay blocked")


if __name__ == "__main__":
    main()
