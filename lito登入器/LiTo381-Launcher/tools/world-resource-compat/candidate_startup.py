"""Preflight by default. --launch explicitly invokes the existing owned startup.

This file does not replace the formal launcher. A future native launch still
uses its exact READY/claim/identity/cleanup flow; no gate is bypassed.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import sys

R = Path(__file__).resolve().parent
L = R.parents[1]

def verify_resource_sources():
    receipt_path = R / "build/inputs/effectlist2.xml.extraction.json"
    if not receipt_path.exists():
        receipt_path = R / "build/inputs/extraction.json"
    receipt = json.loads(receipt_path.read_text("utf-8"))
    index = Path(receipt["archive"])
    raw = index.read_bytes()
    if hashlib.sha256(raw).hexdigest() != receipt["archiveIndexSha256"]:
        raise ValueError("shared Tile index changed; re-export and qualify resources")
    count = struct.unpack_from("<I", raw)[0]
    if len(raw) != 4 + count * 28:
        raise ValueError("shared Tile index layout changed")
    members = []
    for i in range(count):
        offset, name, size = struct.unpack_from("<I20sI", raw, 4+28*i)
        if name.split(b"\0")[0].lower() == b"effectlist2.xml":
            members.append((offset, size))
    if len(members) != 1:
        raise ValueError("effectlist2.xml archive identity changed")
    offset, size = members[0]
    archive = index.with_suffix(".pak")
    if size > 2_000_000 or offset+size > archive.stat().st_size:
        raise ValueError("effectlist2.xml archive bounds changed")
    with archive.open("rb") as stream:
        stream.seek(offset)
        encoded = stream.read(size)
    if hashlib.sha256(encoded).hexdigest() != receipt["encodedSha256"]:
        raise ValueError("shared effectlist2.xml changed; rebuild candidate catalog before launch")
    icons = json.loads((R / "build/icon-assets.json").read_text("utf-8"))
    for row in icons["index_pins"]:
        if hashlib.sha256(Path(row["path"]).read_bytes()).hexdigest() != row["sha256"]:
            raise ValueError("shared icon index changed; requalify resource routing")
    # Pin cache sizing too: an unchanged TBT member can still be unreachable
    # when list.ico changes without changing the archive index.
    for row in icons["capacity_candidates"]:
        with Path(row["archive"]).open("rb") as stream:
            stream.seek(row["offset"])
            payload = stream.read(row["bytes"])
        if hashlib.sha256(payload).hexdigest() != row["sha256"]:
            raise ValueError("shared TBT cache capacity changed; requalify list.ico")
    for icon in icons["assets"]:
        for row in icon["candidates"]:
            with Path(row["archive"]).open("rb") as stream:
                stream.seek(row["offset"])
                payload = stream.read(row["bytes"])
            if hashlib.sha256(payload).hexdigest() != row["sha256"]:
                raise ValueError("shared status icon changed: " + icon["member"])


def configure():
    option = os.environ.get("LITO_SHARED_EXPIRED_ICONS", "")
    if option not in ("", "0", "1"):
        raise ValueError("LITO_SHARED_EXPIRED_ICONS must be 0 or 1")
    verify_resource_sources()
    # Selector 5 is ambiguous across legacy dialects. Only use this adapter
    # with the actual JAR whose catalog/writer was independently executed.
    wire = json.loads((R / "build/shield-wire.json").read_text("utf-8"))
    jar = wire["jar"]
    wire_source=Path(jar["path"])
    # 0.6.70 publication replaced the live formal JAR. Historical proof keeps
    # its original hash and now resolves to the byte-identical fixed input.
    if wire_source==L/'app/server/LiTo381-SharedResources-candidate.jar':
        wire_source=R/'inputs/formal-0.6.69.jar'
    if wire["status"] != "qualified" or hashlib.sha256(wire_source.read_bytes()).hexdigest() != jar["sha256"]:
        raise ValueError("shared shield packet producer changed; requalify dialect before launch")
    report = json.loads((R / "build/status-candidate.json").read_text("utf-8"))
    for key in ("candidate", "catalog", "behavior_header", "paired_server",
                "paired_server_receipt", "paired_wire_fixture"):
        row = report[key]
        actual = hashlib.sha256(Path(row["path"]).read_bytes()).hexdigest()
        if actual != row["sha256"]:
            raise ValueError("candidate seal mismatch: " + key)
    spec = importlib.util.spec_from_file_location("shared_world_owned_startup",
                                                R / "inputs/development_startup-0.6.69.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.DLL = Path(report["candidate"]["path"])
    entry=R/"candidate_server_entry.py"
    paired_spec=importlib.util.spec_from_file_location("paired_server_preflight",entry)
    paired=importlib.util.module_from_spec(paired_spec)
    paired_spec.loader.exec_module(paired)
    if paired.JAR != Path(report["paired_server"]["path"]):
        raise ValueError("paired server entry mismatch")
    module.ENTRY=entry
    module.PINS[entry]=hashlib.sha256(entry.read_bytes()).hexdigest().upper()
    module.PINS[paired.JAR]=paired.JAR_SHA256.upper()
    catalog = Path(report["catalog"]["path"]).resolve()
    module.PINS[module.DLL] = report["candidate"]["sha256"].upper()
    module.PINS[catalog] = report["catalog"]["sha256"].upper()
    # Keep original formal pins too: no unrelated in-flight candidate is adopted.
    module.check_source(module.CLIENT)
    os.environ["LITO_SHARED_STATUS_CATALOG"] = str(catalog)
    return module, report


def main():
    module, report = configure()
    if len(sys.argv) == 1 or sys.argv[1] == "--check-only":
        print(json.dumps({"status": "candidate-startup-pins-verified",
            "native_started": False, "server_started": False,
            "candidate_dll": str(module.DLL), "catalog": report["catalog"]["path"],
            "paired_server":report["paired_server"],"paired_entry":str(module.ENTRY),
            "formal_updated": False}, ensure_ascii=False))
        return 0
    if sys.argv[1] != "--launch":
        raise ValueError("Use --check-only, or --launch followed by original startup arguments")
    sys.argv = [str(module.__file__), *sys.argv[2:]]
    return module.main()


if __name__ == "__main__":
    sys.exit(main())
