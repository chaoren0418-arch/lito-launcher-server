"""Execute the deployed reader + original x86 parser without a game window.

Only OS/allocator/lexer adapters are supplied by the retained harness.
This proves table construction and per-frame duration, not renderer wall time.
"""
import hashlib
import json
from pathlib import Path
import re
import struct
import sys
import pefile
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EAX
import catalog as status_catalog
from rebuild_animation import R, L, B, recover, SOURCE


def main():
    compiler, _ = recover()
    import native_parser_harness as native
    from frame_ir import parse_action
    native.PE = status_catalog.NATIVE
    manifest = json.loads((L / "app/common_catalog_manifest.json").read_text("utf-8"))
    lt_raw = (B / "inputs/LTlist.spr").read_bytes()
    if hashlib.sha256(lt_raw).hexdigest() != manifest["lito_source_sha256"]:
        raise ValueError("LT archive differs from retained profile")
    shared = compiler.parse(SOURCE.read_bytes())
    lt = compiler.parse(lt_raw)
    layout = json.loads((L / "app/native-layout-patches.json").read_text("utf-8"))
    patches = [(int(p["address"], 16), bytes.fromhex(p["new"])) for p in layout["patches"]]
    h = native.Harness([], patches, [compiler.compact(s, 143) for s in lt], native_base_loader=True)
    u = h.uc
    dll = pefile.PE(str(L / "app/native_fade_extension.dll"))
    base = dll.OPTIONAL_HEADER.ImageBase
    u.mem_map(base, (dll.OPTIONAL_HEADER.SizeOfImage + 4095) & ~4095)
    u.mem_write(base, dll.get_memory_mapped_image())
    exports = {s.name.decode().lstrip("_@").split("@")[0]: base + s.address
               for s in dll.DIRECTORY_ENTRY_EXPORT.symbols}
    blob = (B / "candidate/common_catalog.bin").read_bytes()
    storage = h.alloc(len(blob)); u.mem_write(storage, blob)
    sounds = h.alloc(len(manifest["sound_events"]) * 16)

    def call(name, args=()):
        sp = 0x101FFE00
        u.mem_write(sp, struct.pack("<" + "I" * (len(args) + 1), 0x10000000, *args))
        u.reg_write(UC_X86_REG_ESP, sp)
        u.emu_start(exports[name], 0x10000000, count=10_000_000)
        assert u.reg_read(UC_X86_REG_ESP) == sp + 4 + 4 * len(args)
        return u.reg_read(UC_X86_REG_EAX)

    assert call("SharedConfigure", (storage, len(blob), sounds, len(manifest["sound_events"]))) == 1
    u.mem_write(0x4D3FF0, b"\xE9" + struct.pack("<i", exports["SharedLoad"] - 0x4D3FF5) + b"\x90")
    u.reg_write(UC_X86_REG_ESP, 0x101FFF00)
    u.mem_write(0x101FFF00, struct.pack("<I", 0x10000000))
    report = h.run(max_slices=6)
    assert report["completed"] and report["count"] == 30285 and report["base_count"] == 30000
    assert call("SharedPostparse") == 1
    assert call("SharedState") == 3
    table = h.d(0x834E70)
    actions, frames_checked, failures = {}, 0, []
    for rows, offset in [(shared, 0), (lt, 30000)]:
        for sprite in rows:
            rate = 24
            for c in sprite.commands:
                if c.opcode == 110:
                    rate = int(re.findall(r"-?\d+", c.body)[0])
                elif c.opcode < 100:
                    key = (offset + sprite.id, c.opcode)
                    if c.alias_sprite is not None:
                        prior = actions.get((offset + c.alias_sprite, c.opcode))
                        if prior is not None:
                            actions[key] = prior
                    else:
                        direction, frames = parse_action(c.body)
                        actions[key] = (direction, [f.ticks * 1000 // rate for f in frames])
    for (sprite, action), (direction, durations) in actions.items():
        row = table + (sprite * 143 + action) * 12
        actual_header = struct.unpack("<Bb", u.mem_read(row, 2))
        if actual_header != (direction, len(durations)):
            failures.append({"sprite": sprite, "action": action, "header": list(actual_header),
                             "expected": [direction, len(durations)]})
            continue
        ptr = h.d(row + 8)
        for i, expected in enumerate(durations):
            actual = struct.unpack("<H", u.mem_read(ptr + i * 20 + 10, 2))[0] ^ 0x1812
            frames_checked += 1
            if actual != expected:
                failures.append({"sprite": sprite, "action": action, "frame": i,
                                 "milliseconds": actual, "expected": expected})
    report.update(actions=len(actions), frames_checked=frames_checked,
                  duration_mismatch_count=len(failures), duration_mismatches=failures[:100],
                  original_reader_and_parser=True, compiled_postparse=True,
                  native_game_started=False, renderer_wall_time_verified=False,
                  source_sha256=manifest["source_sha256"], payload_sha256=manifest["payload_sha256"])
    (B / "native-animation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "duration_mismatches"}))
    if failures:
        raise RuntimeError(f"{len(failures)} native timing/header differences; see retained receipt")


if __name__ == "__main__":
    main()
