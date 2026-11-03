"""Verify exact shared icon members, without extracting or modifying PAKs."""
from pathlib import Path
import hashlib
import json
import struct
from catalog import ROOT

CLIENT = Path("D:/lineage開發/0-客戶端")


def main():
    report = json.loads((ROOT / "build/status-candidate.json").read_text("utf-8"))
    indices = {}
    index_pins = []
    for p in sorted(CLIENT.glob("Sprite*.idx")):
        raw = p.read_bytes()
        count = struct.unpack_from("<I", raw)[0]
        if len(raw) != 4 + count * 28:
            raise ValueError("unsupported index layout: " + str(p))
        members = {}
        for i in range(count):
            offset, name, size = struct.unpack_from("<I20sI", raw, 4 + 28 * i)
            name = name.split(b"\0")[0].decode("ascii").casefold()
            members.setdefault(name, []).append((offset, size))
        indices[p.stem] = members
        index_pins.append({"path": str(p), "sha256": hashlib.sha256(raw).hexdigest()})
    # The native TBT cache count comes from list.ico using the same sharded
    # archive lookup. Do not assume the smaller legacy base archive's count.
    capacity_candidates = []
    name = "list.ico"
    for archive in (f"Sprite{sum(name.encode('ascii')) % 16:02}", "Sprite"):
        for offset, size in indices[archive].get(name, []):
            path = CLIENT / (archive + ".pak")
            if not 0 < size < 32 or offset + size > path.stat().st_size:
                raise ValueError("invalid TBT cache count member")
            with path.open("rb") as stream:
                stream.seek(offset)
                raw_count = stream.read(size)
            count = int(raw_count.strip())
            if not 0 < count <= 100000:
                raise ValueError("unsupported TBT cache count")
            capacity_candidates.append({"archive": str(path), "offset": offset,
                "bytes": size, "sha256": hashlib.sha256(raw_count).hexdigest(), "count": count})
    if not capacity_candidates:
        raise ValueError("missing list.ico")
    capacity = capacity_candidates[0]["count"]
    assets, missing = [], []
    active_icons = {int(r["effect"]["icon"]) for r in report["bindings"]}
    off_icons = {int(r["effect"].get("offIcon", "0")) for r in report["bindings"]} - {0}
    for icon in sorted(active_icons | off_icons):
        if icon >= capacity:
            raise ValueError(f"status icon {icon} outside TBT cache capacity {capacity}")
        # Status draw 0x4A016E indexes 0x648EC8, the TBT cache filled by
        # native_tbt_hooks.py at 0x43F9D5. IMG lookup is a different consumer.
        name = f"{icon}.tbt"
        shard = f"Sprite{sum(name.encode('ascii')) % 16:02}"
        choices = []
        for archive in [shard, "Sprite"]:
            for offset, size in indices[archive].get(name, []):
                path = CLIENT / (archive + ".pak")
                if size <= 0 or offset < 0 or offset + size > path.stat().st_size:
                    raise ValueError("member outside PAK")
                with path.open("rb") as stream:
                    stream.seek(offset)
                    data = stream.read(size)
                if len(data) != size:
                    raise ValueError("short archive read")
                choices.append({"archive": str(path), "offset": offset, "bytes": size,
                                "sha256": hashlib.sha256(data).hexdigest(),
                                "header_hex": data[:12].hex()})
        if not choices:
            missing.append(name)
        assets.append({"icon": icon, "member": name, "candidates": choices})
    output = {"status": "indexed-payloads-present" if not missing else "missing-members",
              "assets": assets, "missing": missing, "index_pins": index_pins,
              "cache_capacity": capacity, "capacity_candidates": capacity_candidates,
              "active_icons": len(active_icons), "off_icons": len(off_icons),
              "consumer": {"status_draw": "0x4A016E", "cache": "0x648EC8",
                           "normalization_hook": "0x43F9D5", "format": "TBT"},
              "native_decode_and_render_verified": False, "source_modified": False}
    previous = ROOT / "build/icon-assets.json"
    if previous.exists():
        old = json.loads(previous.read_text("utf-8"))
        if old.get("missing") and any(s.endswith(".img") for s in old["missing"]):
            (ROOT / "build/icon-assets-initial-img-assumption.json").write_bytes(previous.read_bytes())
    (ROOT / "build/icon-assets.json").write_text(json.dumps(output, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({"icons": len(assets), "missing": missing}))
    if missing:
        raise ValueError("shared icon missing")


if __name__ == "__main__":
    main()
