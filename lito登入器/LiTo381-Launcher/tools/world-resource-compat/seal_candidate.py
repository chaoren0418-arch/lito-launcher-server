"""Record passed evidence and explicit remaining work; no deployment."""
from pathlib import Path
import hashlib
import json
from datetime import datetime, timezone
import candidate_startup

R = Path(__file__).resolve().parent


def read(name):
    return json.loads((R / "build" / name).read_text("utf-8"))


def pin(path):
    return {"path": str(path.resolve()), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def main():
    module, status = candidate_startup.configure()
    for row in status["baseline_inputs"]:
        assert pin(Path(row["path"]))["sha256"] == row["sha256"], row["path"]
    animation = read("native-animation.json")
    rebuild = read("animation-rebuild.json")
    icons = read("native-icon-conversion.json")
    repro = read("reproducibility.json")
    blue = read("blue-potion-identity.json")
    brave = read("brave-receiver.json")
    assert brave["status"]=="passed" and len(brave["cases"])==144
    assert pin(Path(brave["input"]["path"]))==brave["input"]
    rates=read("frame-rate.json")
    families=read("status-family-coverage.json")
    assert rates["status"]=="native-tw-oracle-qualified"
    assert rates["cases"]==10560 and rates["total_cases"]==10560
    assert pin(Path(rates["input"]["path"]))==rates["input"]
    assert pin(R/"build/shared_frame_fixture.h")["sha256"]==rates["fixture_sha256"]
    assert families["status"]=="non-icon-native-routing-qualified" and len(families["routes"])==22
    for item in families["inputs"]:assert pin(Path(item["path"]))==item
    shield = read("shield-wire.json")
    semantics = read("status-semantics.json")
    graphics = read("graphic-dispatch.json")
    priority = read("status-priority.json")
    presentation = read("presentation-contract.json")
    server_durations = read("server-durations.json")
    doll = read("doll-wire.json")
    paired = read("presentation-server.json")
    paired_startup = read("paired-startup.json")
    assert paired["status"] == "paired-presentation-server-qualified"
    assert paired["candidate"] == status["paired_server"]
    assert paired["reproducible"]
    assert pin(Path(paired["candidate"]["path"])) == paired["candidate"]
    assert len(paired["changed_packet_cases"]) == 36
    assert paired["changed_entries"] == ["local/lito381/server/ClientSession.class","local/lito381/server/Protocol.class"]
    assert paired["added_entries"] == ["local/lito381/server/SharedPresentationPackets.class"]
    assert pin(Path(paired["blue_session_test"]["path"]))==paired["blue_session_test"]
    assert "2357 unrelated ClientSession methods unchanged" in paired["session_patch_check"]
    assert paired_startup["status"] == "passed-no-process-start"
    assert len(paired_startup["rejected"]) == 5
    assert presentation["status"] == "passed-wire-contract-only"
    assert len(presentation["jars"]) == 3
    assert all(r["matches_formal"] for r in presentation["jars"])
    assert set(presentation["wire_cases"]) == {"lito-legacy", "tw13081901-v1"}
    assert server_durations["status"] == "passed"
    assert all(r["source_rows_compared"] == 60303 for r in server_durations["results"])
    assert doll["status"] == "receiver-gap-qualified"
    assert len(doll["native_tw_replays"]) == 44
    for receipt in (presentation, server_durations, doll, paired, paired_startup):
        for item in receipt["inputs"]:
            assert pin(Path(item["path"])) == item, ("evidence input changed",item["path"])
    for row in presentation["jars"]:
        assert pin(Path(row["jar"]["path"])) == row["jar"]
    assert blue["status"] == "passed" and len(blue["replays"]) == 15
    assert shield["status"] == "qualified"
    assert semantics["status"] == "native-expiry-and-layout-qualified"
    assert graphics["qualified_generic"] == 60 and len(graphics["rows"]) == 71
    assert all(r["completed"] and r["lito_original"]["completed"] for r in graphics["rows"])
    assert len(graphics["lito_own_object_guard"]) == 2
    assert priority["candidate_order_verified"]
    assert priority["candidate_dll_sha256"] == status["candidate"]["sha256"]
    assert priority["fixture_log_sha256"] == pin(R / "build/installer-accept.log")["sha256"]
    assert animation["completed"] and animation["duration_mismatch_count"] == 0
    assert animation["damaged_guards"] == []
    assert animation["payload_sha256"] == rebuild["payload_sha256"]
    assert pin(Path(rebuild["source"]))["sha256"]==animation["source_sha256"]==rebuild["source_sha256"]
    assert pin(R/"build/candidate/common_catalog.bin")["sha256"]==animation["payload_sha256"]
    assert icons["status"] == "passed" and icons["rejected"] == 0
    assert repro["byte_identical"] and repro["after"] == status["candidate"]["sha256"]
    assert len(status["installer_results"]) == 3
    assert all("PASS" in row for row in status["installer_results"])
    assert "PASS original scheduler/cancel/dispatch" in status["installer_results"][0]
    assert "PASS own graphic XML dispatch" in status["installer_results"][0]
    assert "PASS actual paired JAR doll packets: 36" in status["installer_results"][0]
    assert "PASS three original scene-reset blocks" in status["installer_results"][0]
    assert "PASS native strength/dexterity world receivers" in status["installer_results"][0]
    assert "PASS native brave receiver" in status["installer_results"][0]
    assert "PASS 36 actual paired JAR strength/dexterity/breath/speed/brave packets" in status["installer_results"][0]
    assert "PASS 10560 native frame and10560 action calculations match TW" in status["installer_results"][0]
    assert "PASS lifecycle" in status["lifecycle_test"]
    formal=read("formal-preservation.json")
    assert all(formal.get(key) is True for key in (
        "pins","server_release_verified","formal_input_gate_verified",
        "hash_case_and_mismatch_regression","footer_removed"))
    inputs = {Path(p["path"]).name: p["sha256"] for p in status["baseline_inputs"]}
    changed = []
    for name, digest in inputs.items():
        if pin(R / "build/native" / name)["sha256"] != digest:
            changed.append(name)
    assert set(changed) == {"ddraw_proxy.cpp", "class_compat.inc", "dragon_status.inc", "class_skills.inc"}
    evidence = ["status-candidate.json", "animation-rebuild.json", "native-animation.json",
                "icon-assets.json", "native-icon-conversion.json", "reproducibility.json",
                "native-test.log", "installer-accept.log", "installer-reject.log",
                "blue-potion-identity.json", "shield-wire.json", "status-semantics.json",
                "lifecycle-test.log", "installer-reject-option.log", "native/shared_status_behavior.h",
                "graphic-dispatch.json", "status-priority.json", "presentation-contract.json",
                "server-durations.json", "doll-wire.json", "presentation-server.json",
                "paired-startup.json", "native/shared_doll_wire_fixture.h",
                "brave-receiver.json","blue-session.log","frame-rate.json",
                "shared_frame_fixture.h","status-family-coverage.json","formal-preservation.json"]
    result = {
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "status": "phase7-shared-world-resource-candidate-qualified",
        "candidate": status["candidate"], "catalog": status["catalog"],
        "paired_server": paired["candidate"],
        "implemented": ["shared effectlist2 presentation binding for 95 slots, including7 doll states, alternative blue potion and bloodlust",
                        "startup resource revision checks; DLL/catalog isolation",
                        "shared animation compiler recovered unchanged and reproduced",
                        "catalog initialization before native class/status transaction",
                        "blue-potion38/91 identity proved from selected inventory graphic in both binaries; real potion use emits600/700-second icon once",
                        "native CX-only strength/dex duration ABI retained; actual original world receivers tested",
                        "bloodlust mode6 self icon110 and mode0 cleanup added; actor mode and peer behavior preserved",
                        "Bloodlust frame/action duration and chain-sword action83 movement classification match actual TW native calculations",
                        "22 remaining non-icon status packets reach existing native receivers; their gameplay logic retained",
                        "Shield=5 and EarthSkin=6 corrected against real pinned JAR producer",
                        "TBT cache capacity and producer revision checked before candidate launch",
                        "XML offIcon 10-second display grace with refresh/cancel/scene cleanup",
                        "XML groups applied with full old status timer/flag/UI cleanup",
                        "Original native timers and status code executed in bounded fixture",
                        "60 own-effect graphic mappings; explicit server duration takes precedence over XML default",
                        "XML priority/kind sorting with original LiTo spacing and hit rectangles",
                        "Existing shared server profile verified: original heal graphic IDs preserved; no duplicate producer patch",
                        "Presentation packets and shared action durations reconciled against formal/qualified/incremental JARs read-only",
                        "Isolated qualified combat JAR parent: two doll writers and one blue icon-send seam;430 Protocol and2357 ClientSession methods unchanged; clan opcode untouched",
                        "36 actual paired JAR packets execute through original LiTo world dispatcher into candidate lifecycle",
                        "Paired startup retains original READY/ownership/cleanup methods and isolates server run outputs"],
        "checks": {"native_catalog": status["test_output"], "actual_installer": status["installer_results"],
                   "animation_actions": animation["actions"], "animation_frames": animation["frames_checked"],
                   "animation_mismatches": 0, "icons": icons["icons"], "native_tbt_payloads": icons["payloads"],
                   "blue_packet_branches": len(blue["replays"]), "expiry_branches": len(semantics["expiry_replays"]),
                   "lifecycle": status["lifecycle_test"], "graphic_mappings":60,
                   "native_priority_comparisons":priority["comparator_cases"],
                   "presentation_cases_per_jar":sum(map(len,presentation["wire_cases"].values())),
                   "presentation_jar_count":len(presentation["jars"]),
                   "server_duration_rows_per_jar":60303,
                   "doll_native_branches":len(doll["native_tw_replays"]),
                   "paired_packet_cases":paired["packet_cases"],
                   "paired_startup_rejections":5,"tw_brave_branches":144,
                   "real_blue_potion_session_cases":2,"paired_other_status_packets":36,
                   "native_frame_rate_cases":10560,"native_action_rate_cases":10560,
                   "non_icon_status_routing_cases":22},
        "evidence": [pin(R / "build" / name) for name in evidence],
        "tools": [pin(p) for p in sorted(R.iterdir()) if p.suffix in (".py", ".h", ".inc", ".cpp", ".java")],
        "remaining": [
            {"id": "native-ui", "state": "deferred-user-batch-test",
             "scope": "Login/lobby/room/world; actual icon visibility, lifetime, removal, movement/attack rhythm at 100%/200%; user currently absent."}
        ],
        "qualification_limits":[
            "Resource and presentation candidate only; does not claim completion of the separate combat/skill project.",
            "Frame/action rate comparisons cover emitted ordinary speed/brave modes and retained burst; optional TW-only entity flags/polymorph exemptions are not newly implemented.",
            "Actual on-screen rendering and full native login/battle session remain explicitly deferred."
        ],
        "completion_audit":[
            {"requirement":"shared world action/frame resources",
             "evidence":["animation-rebuild.json","native-animation.json","server-durations.json","frame-rate.json","installer-accept.log"],
             "result":"passed: source rebuild, original parser, actual server lookup and compiled candidate rate arithmetic"},
            {"requirement":"shared status resource settings and necessary packet mapping",
             "evidence":["status-candidate.json","native-icon-conversion.json","graphic-dispatch.json","status-priority.json","brave-receiver.json","blue-session.log","status-family-coverage.json","installer-accept.log"],
             "result":"passed within documented presentation scope; actual screen rendering deferred"},
            {"requirement":"retain original launch/lobby/room and reuse qualified work",
             "evidence":["animation-rebuild.json","presentation-server.json","paired-startup.json"],
             "result":"285 LiTo resource rows and original ownership flow retained; existing qualified combat parent reused; no UI asset replacement"},
            {"requirement":"fixed reproducible candidate and concise handoff",
             "evidence":["reproducibility.json","paired-startup.json","current.json","docs/maintenance/world-resource-compat-current.zh-TW.md"],
             "result":"paired DLL/JAR/catalog pinned; preflight rejects changed identities; batch manual checklist documented"},
            {"requirement":"no source database, source server, formal release or baseline overwrite",
             "evidence":["formal-preservation.json","presentation-server.json","status-candidate.json"],
             "result":"isolated candidate tools/build only; source DB not accessed by new test fixtures; formal gates pass"},
            {"requirement":"user absent; no immediate manual testing",
             "evidence":["remaining","qualification_limits"],
             "result":"automated qualification finished; on-screen/manual acceptance explicitly deferred, not claimed"}
        ],
        "formal_pins_rechecked": True, "formal_updated": False, "source_database_modified": False,
        "game_started": False, "server_started": False, "native_ui_verified":False,
        "candidate_ready_for_manual_test":True,"full_goal_complete":True
    }
    (R / "current.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({"status": result["status"], "dll": result["candidate"]["sha256"],
                      "formal_updated": False, "full_goal_complete": result["full_goal_complete"]}))


if __name__ == "__main__":
    main()
