"""Run exact shared TBT payloads through the compiled adapter and x86 converter."""
import hashlib
import json
from pathlib import Path
import struct
import sys
import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EAX, UC_X86_REG_ECX
from catalog import ROOT, LAUNCHER, NATIVE


def main():
    sys.path.insert(0, str(LAUNCHER / "app"))
    from shared_bootstrap import map_compatibility_image
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    game = pefile.PE(str(NATIVE))
    image = game.get_memory_mapped_image()
    u.mem_map(0x400000, (len(image)+4095)&~4095)
    u.mem_write(0x400000, image)
    adapter = (LAUNCHER / "app/native_fade_extension.dll").read_bytes()
    image, exports, _ = map_compatibility_image(adapter, 0x68000000)
    u.mem_map(0x68000000, (len(image)+4095)&~4095)
    u.mem_write(0x68000000, image)
    u.mem_map(0x10000000, 0x10000)
    next_base = 0x20000000
    allocations, freed = {}, []

    def allocate(size):
        nonlocal next_base
        if not 0 < size < 1_000_000:
            raise ValueError("allocation out of bounds")
        span = (size+4095)&~4095
        base = next_base
        next_base += span+4096
        u.mem_map(base, span)
        pointer = base+span-size
        allocations[pointer] = size
        return pointer

    def api(uc, address, size, _):
        sp = uc.reg_read(UC_X86_REG_ESP)
        argument = struct.unpack("<I", uc.mem_read(sp+4, 4))[0]
        if address == 0x4F118C:
            uc.reg_write(UC_X86_REG_EAX, allocate(argument))
        else:
            if argument in freed or argument not in allocations:
                raise ValueError("invalid/double free")
            freed.append(argument)

    for address in (0x4F118C, 0x4F103C):
        u.mem_write(address, b"\xC3")
        u.hook_add(UC_HOOK_CODE, api, begin=address, end=address)

    def call(address, args=(), ecx=0):
        sp = 0x1000F000
        u.reg_write(UC_X86_REG_ESP, sp)
        u.reg_write(UC_X86_REG_ECX, ecx)
        u.mem_write(sp, struct.pack("<"+"I"*(1+len(args)), 0x10000800, *args))
        u.emu_start(address, 0x10000800, count=3_000_000)
        return u.reg_read(UC_X86_REG_EAX)

    def convert(data):
        out = bytearray(data)
        pos = 4
        for _ in range(out[3]):
            spans = out[pos]
            pos += 1
            for _ in range(spans):
                count = out[pos+1]
                pos += 2
                for _ in range(count):
                    value = struct.unpack_from("<H", out, pos)[0]
                    struct.pack_into("<H", out, pos, (value+(value&0x7FE0))&65535)
                    pos += 2
        return bytes(out)

    assets = json.loads((ROOT / "build/icon-assets.json").read_text("utf-8"))
    results = []
    for icon in assets["assets"]:
        for source in icon["candidates"]:
            with Path(source["archive"]).open("rb") as stream:
                stream.seek(source["offset"])
                raw = stream.read(source["bytes"])
            assert hashlib.sha256(raw).hexdigest() == source["sha256"]
            original = allocate(len(raw))
            u.mem_write(original, raw)
            normalized = call(exports["NormalizeTbt"], (original, len(raw)))
            if not normalized:
                raise ValueError("native rejected " + icon["member"])
            normalized_bytes = bytes(u.mem_read(normalized, allocations[normalized]))
            expected = convert(normalized_bytes)
            call(0x43DA40, ecx=normalized)
            assert bytes(u.mem_read(normalized, len(expected))) == expected
            results.append({"icon": icon["icon"], "source": source,
                            "normalization_reallocated": normalized != original,
                            "native_converter_passed": True, "pixel_bytes_match": True})
    report = {"status": "passed", "icons": len(assets["assets"]), "payloads": len(results),
              "adapter_sha256": hashlib.sha256(adapter).hexdigest(),
              "repaired": call(exports["TbtRepairCount"]), "rejected": call(exports["TbtRejectCount"]),
              "cases": results, "guarded_allocations": len(allocations),
              "native_window_started": False, "on_screen_render_verified": False}
    (ROOT / "build/native-icon-conversion.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "cases"}))


if __name__ == "__main__":
    main()
