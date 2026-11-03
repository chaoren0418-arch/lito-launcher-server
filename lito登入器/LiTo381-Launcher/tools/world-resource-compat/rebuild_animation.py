"""Recover the qualified compiler unchanged and rebuild the shared world table.

Archived helpers are content-addressed provenance, not a new implementation.
This script writes only this candidate directory.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import struct
import sys
import zipfile

R = Path(__file__).resolve().parent
L = R.parents[1]
B = R / "build"
SOURCE = Path("D:/lineage開發/2-登入器/打包建置/編碼器/TW13081901.txt")
CLIENT = Path("D:/lineage開發/0-客戶端/TW13081901.txt")
ARCHIVE = L.parent / "docs/history/development-evidence.zip"
PREFIX = "LiTo381-Launcher/runtime/shared-resource-migration-20261011/"


def sha(b):
    return hashlib.sha256(b).hexdigest()


def recover():
    vendor = B / "compiler"
    vendor.mkdir(parents=True, exist_ok=True)
    provenance = []
    with zipfile.ZipFile(ARCHIVE) as z:
        for member, name in [("catalog.py", "animation_catalog.py"), ("frame_ir.py", "frame_ir.py"),
                             ("native_parser_harness.py", "native_parser_harness.py")]:
            raw = z.read(PREFIX + member)
            out = vendor / name
            out.write_bytes(raw)
            provenance.append({"archive": str(ARCHIVE), "member": PREFIX + member,
                               "sha256": sha(raw), "candidate_copy": str(out)})
    sys.path.insert(0, str(vendor))
    import animation_catalog
    return animation_catalog, provenance


def build():
    compiler, provenance = recover()
    source = SOURCE.read_bytes()
    if source != CLIENT.read_bytes():
        raise ValueError("encoder TXT and actual shared-client TXT differ; choose/reconcile explicitly")
    manifest = json.loads((L / "app/common_catalog_manifest.json").read_text("utf-8"))
    if sha(source) != manifest["source_sha256"]:
        raise ValueError("new source revision requires compatibility qualification")
    rows = compiler.parse(source)
    offsets, payload, sounds, metadata = [0], bytearray(), [], {}
    for row in rows:
        compact = compiler.compact(row, 143, sounds, metadata)
        encoded = bytes((((v + 1) << 1) ^ 0xB1) & 255 for v in compact)
        if bytes((((v ^ 0xB1) >> 1) - 1) & 255 for v in encoded) != compact:
            raise ValueError("native encoding loses source byte")
        payload.extend(encoded + b"\0")
        offsets.append(len(payload))
    fades = metadata.get("fade_events", [])
    blob = bytearray(struct.pack("<8I", 0x3152544C, 1, len(rows), 285, len(payload),
                                 len(sounds), len(fades), 0))
    blob.extend(struct.pack("<" + "I" * len(offsets), *offsets))
    blob.extend(payload)
    for e in sounds:
        values = [(v + 32768) % 65536 - 32768 for v in e["sounds"]]
        blob.extend(struct.pack("<4I5hH", e["sprite"], e["action"], e["frame"], len(values),
                                *(values + [0] * (5 - len(values))), 0))
    for e in fades:
        blob.extend(struct.pack("<5I", *(e[k] for k in ("sprite", "action", "frame", "flag", "duration"))))
    original = (L / "app/common_catalog.bin").read_bytes()
    if bytes(blob) != original or sha(blob) != manifest["payload_sha256"]:
        raise ValueError("retained compiler no longer reproduces formal world catalog")
    if json.loads(json.dumps(metadata)) != manifest["metadata"] or sounds != manifest["sound_events"]:
        raise ValueError("post-parser extensions differ")
    (B / "candidate/common_catalog.bin").write_bytes(blob)
    (B / "candidate/common_catalog_manifest.json").write_bytes((L / "app/common_catalog_manifest.json").read_bytes())
    report = {"status": "byte-exact-source-rebuild", "sprites": len(rows),
              "source": str(SOURCE), "source_sha256": sha(source),
              "payload_sha256": sha(blob), "bytes": len(blob), "compiler_provenance": provenance,
              "lito_extra_rows_preserved": 285, "formal_updated": False,
              "source_database_modified": False, "native_timing_execution_verified": False,
              "note": "No missing row placeholders; every source row compiled. Client execution timings are a separate gate."}
    (B / "animation-rebuild.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "compiler_provenance"}, ensure_ascii=False))
    return compiler, rows


if __name__ == "__main__":
    build()
