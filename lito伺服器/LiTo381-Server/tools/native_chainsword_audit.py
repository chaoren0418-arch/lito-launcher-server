#!/usr/bin/env python3
"""Strict source/client/database/TCP/release audit for LiTo server 0.7.191."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path


VERSION = "0.7.191"
CLIENT_SHA256 = "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292"
IDA_SHA256 = "2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A"
RETAINED_ATTACK_SHA256 = "32FDF8BE1999699EE4329C649C91CD2FB13DA8A2A8ED086430A5DC0D78D5BDA7"
RETAINED_PC_SHA256 = "325C74B6D0225449679D8A74FECFC12318F24A3745507C906AD408E8EAD7983D"
RETAINED_C_ATTACK_SHA256 = "D9A27EB3F7FDEDC7C8F5D43F8BC98A4EC6531315571E1FD3CC2D2B913F439C9F"
RETAINED_PACKET_BOX_SHA256 = "EFCDEE788DE408CA466228106E58DDE32817E1ACE690326F269402822F83E3B7"
CUSTOMIZED_ATTACK_SHA256 = "1D97440ECEFC5F1D0B93AA8849FC9084A3B037A10B7771C777086DBD434D9C42"
SPR_ACTION_SHA256 = "4AF7B6084ABFAF0EE46C6E649480EFB275999597713019E0608EB05BDF91B969"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def sha256(path: Path) -> str:
    require(path.is_file(), f"missing hash input: {path}")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def read_text(path: Path) -> str:
    require(path.is_file(), f"missing text input: {path}")
    raw = path.read_bytes()
    for encoding in ("utf-8-sig", "utf-16", "cp950"):
        try:
            return raw.decode(encoding)
        except UnicodeDecodeError:
            pass
    raise RuntimeError(f"unsupported text encoding: {path}")


def require_text(path: Path, *anchors: str) -> str:
    content = read_text(path)
    for anchor in anchors:
        require(anchor in content, f"missing source anchor {anchor!r} in {path.name}")
    return content


def project_root(script: Path) -> Path:
    for candidate in (script.parent, *script.parents):
        if (candidate / "pom.xml").is_file() and (candidate / "src/main/java").is_dir():
            return candidate
    raise RuntimeError(f"cannot locate project root from {script}")


def report_counts(path: Path) -> dict[str, int]:
    root = ET.parse(path).getroot()
    return {key: int(root.attrib.get(key, "0"))
            for key in ("tests", "failures", "errors", "skipped")}


def exact_report(path: Path, tests: int) -> None:
    observed = report_counts(path)
    expected = {"tests": tests, "failures": 0, "errors": 0, "skipped": 0}
    require(observed == expected, f"unexpected JUnit report {path.name}: {observed}")


def junit_counts(directory: Path) -> dict[str, int]:
    reports = sorted(directory.glob("TEST-*.xml"))
    require(reports, "no JUnit XML reports")
    result = {"suites": len(reports), "tests": 0, "failures": 0,
              "errors": 0, "skipped": 0}
    for report in reports:
        counts = report_counts(report)
        for key in ("tests", "failures", "errors", "skipped"):
            result[key] += counts[key]
    return result


def integration_evidence(path: Path) -> dict[str, int]:
    exact_report(path, 1)
    output = ET.parse(path).getroot().findtext(".//system-out", default="")
    special = re.findall(
        r"source=(.*?) special-weapon-rows=69 claw=24 edoryu=28 "
        r"kiringku=9 chainsword=8", output)
    chainswords = re.findall(
        r"source=(.*?) chainsword-rows=8 range-two=8 item-272-exact=1", output)
    require(len(special) == 2 and len(set(special)) == 2,
            f"unexpected special-weapon source evidence: {special}")
    require(chainswords == special,
            f"chainsword/source rows do not match: {chainswords} vs {special}")
    return {"tests": 1, "failures": 0, "errors": 0, "skipped": 0,
            "sources": 2, "chainsword_rows_per_source": 8,
            "range_two_per_source": 8, "item_272_exact_per_source": 1}


def note_int(note: str, key: str) -> int:
    matched = re.search(rf"(?:^| ){re.escape(key)}=(-?\d+)(?: |$)", note)
    require(matched is not None, f"attack trace lacks integer {key}")
    return int(matched.group(1))


def paired(rows: list[dict[str, object]], opcode: str, marker: str,
           pairs: int) -> list[dict[str, object]]:
    selected = [row for row in rows
                if row.get("opcode") == opcode and marker in str(row.get("note", ""))]
    require(len(selected) == pairs * 2,
            f"{opcode}/{marker} has {len(selected)} frames, expected {pairs * 2}")
    result: list[dict[str, object]] = []
    for index in range(0, len(selected), 2):
        first, second = selected[index:index + 2]
        require(first.get("plain") == second.get("plain"), "recipient packets differ")
        require(first.get("note") == second.get("note"), "recipient traces differ")
        result.append(first)
    return result


def tcp_evidence(release: Path) -> dict[str, object]:
    envelope = json.loads(read_text(release / "chainsword-special-weapon-evidence.json"))
    require(envelope.get("transport") == "encrypted LiTo 3.81C loopback framing",
            "unexpected TCP transport")
    require(envelope.get("endpoint") == "127.0.0.2:32100", "unexpected TCP endpoint")
    result = envelope["result"]["chainsword"]
    require(result["room_id"] == 3, "unexpected chainsword room")
    require(result["attacker_object_id"] == 18 and result["target_object_id"] == 16,
            "unexpected chainsword TCP identities")
    require(result["attack_action"] == 25 and result["attacks"] == 40,
            "unexpected chainsword action/count")
    require(result["initial_hp"] == 2000 and result["target_max_hp"] == 2000,
            "unexpected target HP fixture")
    damages = [int(value) for value in result["damages"]]
    require(len(damages) == 40 and all(6 <= value <= 36 for value in damages),
            f"unexpected chainsword damage sequence: {damages}")
    require(result["target_hp"] == 2000 - sum(damages),
            "chainsword target HP does not match wire damage")

    frames_path = release / "frames.jsonl"
    rows = [json.loads(line) for line in read_text(frames_path).splitlines() if line.strip()]
    attacks = paired(rows, "0x23", "chainsword=true ", 40)
    wire_damages = [int(str(row["plain"]).split()[10], 16) for row in attacks]
    require(wire_damages == damages, "capture/evidence chainsword damage mismatch")
    require(all(str(row["plain"]).split()[1] == "19" for row in attacks),
            "chainsword did not use selected sprite's spear-attack fallback 25")

    previous_after = 0
    seen_levels: set[int] = set()
    seen_bonuses: set[int] = set()
    triggered = 0
    for row in attacks:
        note = str(row["note"])
        before = note_int(note, "chainsword-weakness-before")
        after = note_int(note, "chainsword-weakness-after")
        bonus = note_int(note, "chainsword-weakness-bonus")
        remaining = note_int(note, "chainsword-weakness-remaining-nanos")
        require(before == previous_after, "chainsword exposure continuity changed")
        require(0 <= before <= 3 and 0 <= after <= 3,
                "chainsword exposure level is outside 0..3")
        require("chainsword-weakness-target-reset=false" in note
                and "chainsword-weakness-expired=false" in note,
                "unexpected target reset/expiry in continuous TCP fixture")
        require("chainsword-weakness-ui=unavailable-in-selected-LiTo-bin" in note,
                "selected-client UI boundary is missing from trace")
        if "chainsword-weakness-triggered=true" in note:
            triggered += 1
            require(bonus == after * 3 and bonus in (3, 6, 9),
                    "triggered chainsword bonus does not match exposure level")
            require(remaining == 16_000_000_000,
                    "triggered chainsword exposure did not refresh to 16 seconds")
            seen_bonuses.add(bonus)
        else:
            require(bonus == 0, "untriggered chainsword attack has exposure bonus")
            require(0 <= remaining <= 16_000_000_000,
                    "invalid chainsword exposure remaining time")
        seen_levels.add(after)
        previous_after = after
    require(triggered > 0 and seen_bonuses == {3, 6, 9},
            f"TCP did not exercise all exposure bonuses: {seen_bonuses}")
    require(seen_levels == {0, 1, 2, 3},
            f"TCP did not exercise all exposure states: {seen_levels}")

    actor_notes = [str(row.get("note", "")) for row in rows if row.get("opcode") == "0x0B"]
    require(any("name=TestChainsword gfx=6658 walk-action=24 attack-action=25" in note
                for note in actor_notes), "chainsword actor/fallback identity is missing")
    require(not any(str(row.get("plain", "")).startswith("FA 4B") for row in rows),
            "incompatible legacy weakness UI packet was emitted")
    return {"frames": len(rows), "file_bytes": frames_path.stat().st_size,
            "wire_bytes": sum(int(row["wire_length"]) for row in rows),
            "chainsword_attacks": len(attacks), "triggered_attacks": triggered,
            "states_seen": sorted(seen_levels), "bonuses_seen": sorted(seen_bonuses),
            "damage_min": min(damages), "damage_max": max(damages),
            "damage_sum": sum(damages), "frames_sha256": sha256(frames_path)}


def sprite_evidence(path: Path) -> dict[str, object]:
    require(sha256(path) == SPR_ACTION_SHA256, "spr_action resource changed")
    actions: dict[int, set[int]] = {6658: set(), 6661: set()}
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        rows = csv.DictReader(line for line in stream if not line.startswith("#"))
        for row in rows:
            sprite = int(row["spr_id"])
            if sprite in actions:
                actions[sprite].add(int(row["act_id"]))
    for sprite, observed in actions.items():
        require({24, 25}.issubset(observed), f"sprite {sprite} lacks spear family 24/25")
        require(83 not in observed and 84 not in observed,
                f"sprite {sprite} unexpectedly contains chainsword family 83/84")
    return {str(sprite): sorted(values) for sprite, values in actions.items()}


def build_evidence(project: Path, release: Path) -> dict[str, object]:
    implementation = {
        "Accounts.java": project / "src/main/java/local/lito381/server/Accounts.java",
        "BattleAnimationCatalog.java": project / "src/main/java/local/lito381/server/BattleAnimationCatalog.java",
        "BattleChainswordWeakness.java": project / "src/main/java/local/lito381/server/BattleChainswordWeakness.java",
        "ClientSession.java": project / "src/main/java/local/lito381/server/ClientSession.java",
        "LegacyEquipmentPhysicalIntegrationTest.java": project / "src/test/java/local/lito381/server/LegacyEquipmentPhysicalIntegrationTest.java",
    }
    require_text(implementation["Accounts.java"],
                 "case 18 -> BattleActionCodes.CHAINSWORD_WALK",
                 'chainswordPlayer ? "TestChainsword"',
                 "272, 18, 16, 12, 2, 2, 5, 0, 0",
                 "0, 0.0D, 0, false, false, 3033")
    require_text(implementation["BattleAnimationCatalog.java"],
                 "resolveEquippedWalkAction", "CHAINSWORD_WALK",
                 "return requireWalkAction(spriteId, BattleActionCodes.SPEAR_WALK)")
    require_text(implementation["BattleChainswordWeakness.java"],
                 "PROC_CHANCE = 20", "DURATION_NANOS = 16_000_000_000L",
                 "damageBonus = level * 3", "consumedLevel * 20",
                 "targetObjectId != target")
    require_text(implementation["ClientSession.java"],
                 "chainswordWeakness.attack(", "chainswordWeaknessOutcome.damageBonus()",
                 "chainsword-weakness-ui=unavailable-in-selected-LiTo-bin",
                 "chainswordWeakness.clear()")
    require_text(implementation["LegacyEquipmentPhysicalIntegrationTest.java"],
                 "chainsword-rows=", "range-two=", "item-272-exact=")

    reference_root = project.parent / "1-伺服器/其他src參考用"
    retained_attack = reference_root / "rk760src/com/lineage/server/model/L1AttackPc.java"
    retained_pc = reference_root / "rk760src/com/lineage/server/model/Instance/L1PcInstance.java"
    retained_c_attack = reference_root / "rk760src/com/lineage/server/clientpackets/C_Attack.java"
    retained_packet = reference_root / "381n8src/com/lineage/server/serverpackets/S_PacketBoxDk.java"
    customized_attack = reference_root / "381n8src/com/lineage/server/model/L1AttackPc.java"
    require(sha256(retained_attack) == RETAINED_ATTACK_SHA256, "retained attack source changed")
    require(sha256(retained_pc) == RETAINED_PC_SHA256, "retained player source changed")
    require(sha256(retained_c_attack) == RETAINED_C_ATTACK_SHA256,
            "retained C_Attack source changed")
    require(sha256(retained_packet) == RETAINED_PACKET_BOX_SHA256,
            "retained weakness packet source changed")
    require(sha256(customized_attack) == CUSTOMIZED_ATTACK_SHA256,
            "comparison attack source changed")
    require_text(retained_attack, "int weaponchance = 20", "dmg += 3;", "dmg += 6;",
                 "dmg += 9;", "_pc.get_tmp_targetid() != _targetId",
                 "_damage += 20 + doll", "_damage += 40 + doll", "_damage += 60 + doll")
    require_text(retained_c_attack, "h_time - pc.get_weaknss_t() > 16L")
    require_text(retained_pc, "containsChainswordSpr(getTempCharGfx())",
                 "return ActionCodes.ACTION_SpearWalk")
    require_text(retained_packet, "writeC(0x4b)", "writeC(lv)")
    require_text(customized_attack, "dmg += 50; // 修改: Vulnerability1",
                 "dmg += 70; // 修改: Vulnerability2",
                 "dmg += 100; // 修改: Vulnerability3")

    client = Path("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin")
    ida = project.parent / "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"
    require(sha256(client) == CLIENT_SHA256, "selected LiTo.bin hash changed")
    require(sha256(ida) == IDA_SHA256, "static IDA evidence hash changed")
    require_text(ida, "sub_4CB5F0 proc near", "cmp     esi, 0AFh",
                 "sub_4B07C0 proc near", "cmp     eax, 28h",
                 "sub_4AC1C0 proc near", 'aDdsd db \'ddsd\',0')

    counts = junit_counts(release / "surefire-reports")
    expected_counts = {"suites": 131, "tests": 572, "failures": 0,
                       "errors": 0, "skipped": 0}
    require(counts == expected_counts, f"JUnit release counts changed: {counts}")
    exact_report(release / "surefire-reports/TEST-local.lito381.server.BattleChainswordWeaknessTest.xml", 4)
    exact_report(release / "surefire-reports/TEST-local.lito381.server.BattleAnimationCatalogTest.xml", 5)
    integration = integration_evidence(release / "legacy-physical-integration.xml")
    tcp = tcp_evidence(release)
    sprites = sprite_evidence(project / "src/main/resources/battle/spr-action-380.csv")
    require((release / "server.stderr.log").stat().st_size == 0,
            "server stderr is not empty")
    config = require_text(release / "server.chainsword-selftest.properties",
                          "port=32100", "database.mode=memory",
                          "database.read.only=true",
                          "battle.natural.hp.regen.enabled=false",
                          "battle.natural.mp.regen.enabled=false")
    require("port=2000" not in config, "validation config targets legacy port 2000")

    jars = [project / "target/LiTo381-Server.jar",
            project / "publish/LiTo381-Server.jar",
            release / "LiTo381-Server.jar"]
    jar_hashes = [sha256(path) for path in jars]
    require(len(set(jar_hashes)) == 1, "target/publish/release JAR mismatch")
    require(f"<version>{VERSION}</version>" in read_text(project / "pom.xml"),
            "POM version mismatch")
    return {
        "release": VERSION,
        "boundary": {
            "selected_client_read_only": str(client),
            "selected_client_sha256": CLIENT_SHA256,
            "static_ida_sha256": IDA_SHA256,
            "legacy_database_mode": "parameterized read-only SELECT",
            "official_client_resources_modified": False,
            "legacy_port_2000_modified": False,
        },
        "chainsword_slice": {
            "implemented": [
                "Dragon Knight type 5 plus chainsword weapon type 18 eligibility",
                "target-bound three-level exposure with 20/40 percent roll windows",
                "16-second refresh/expiry and +3/+6/+9 exposure damage",
                "Foe Slayer +20/+40/+60 consumption core with target validation",
                "native sprite-resource fallback 83/84 to spear actions 24/25",
                "battle/character lifecycle clearing and traceable state",
            ],
            "selected_client_limits": [
                "generic post-175 selector producer reaches skill 187 as exact 14 17 02",
                "S_EVENT subtype switch supports only 0..40; legacy weakness subtype 75 is absent",
                "opcode 0xFA is selected-client S_LITO_CLAN ddsd, not legacy S_PacketBox",
                "therefore skill187 is invocable while incompatible FA 4B exposure UI remains omitted",
            ],
            "reference_selection": {
                "selected": "retained classic 3.81 +3/+6/+9 path",
                "excluded_variant": "separately preserved customized +50/+70/+100 path",
            },
            "sprite_actions": sprites,
        },
        "verification": {"junit": counts,
                         "legacy_mysql_integration": integration,
                         "encrypted_tcp": tcp, "stderr_bytes": 0},
        "hashes": {
            "implementation": {name: sha256(path) for name, path in implementation.items()},
            "retained_attack": RETAINED_ATTACK_SHA256,
            "retained_player": RETAINED_PC_SHA256,
            "retained_c_attack": RETAINED_C_ATTACK_SHA256,
            "retained_packet_box": RETAINED_PACKET_BOX_SHA256,
            "customized_comparison_attack": CUSTOMIZED_ATTACK_SHA256,
            "spr_action": SPR_ACTION_SHA256,
            "jar": jar_hashes[0],
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release-dir", type=Path,
                        default=Path("runtime/release-0.7.191-staging"))
    args = parser.parse_args()
    project = project_root(Path(__file__).resolve())
    release = (args.release_dir if args.release_dir.is_absolute()
               else project / args.release_dir).resolve()
    try:
        evidence = build_evidence(project, release)
        (release / "NATIVE-CHAINSWORD-EVIDENCE.json").write_text(
            json.dumps(evidence, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")
    except Exception as failure:
        print(f"FAIL {failure}")
        raise SystemExit(1) from failure
    print("PASS LiTo 0.7.191 chainsword source/client/database/TCP/release audit")


if __name__ == "__main__":
    main()
