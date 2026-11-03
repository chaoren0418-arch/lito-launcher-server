"""Qualify TW expiry semantics with real x86 code before porting offIcon."""
import hashlib
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP, UC_X86_REG_ESP
import catalog
from verify_blue_identity import TW

R = Path(__file__).resolve().parent


def replay(image, off_icon, option):
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, (len(image)+4095)&~4095)
    u.mem_write(0x400000, image)
    u.mem_map(0x2000000, 0x10000)
    table, manager, stack, stop = 0x2001000, 0x2005000, 0x200F000, 0x2000000
    slot = 38
    u.mem_write(0xAC4CD8, struct.pack("<I", table))
    u.mem_write(0xAC4CD0, struct.pack("<I", manager))
    u.mem_write(table+slot*60+32, struct.pack("<I", off_icon))
    u.mem_write(0x9661D8, bytes([option]))
    u.mem_write(0xABF4C8+slot, b"\1")
    u.mem_write(0xABF6B8+slot, b"\1")
    events = []

    def api(emu, address, size, _):
        sp = emu.reg_read(UC_X86_REG_ESP)
        if address == stop:
            emu.emu_stop()
        elif address == 0x590BA0:
            ret, delay, callback, value = struct.unpack("<4I", emu.mem_read(sp,16))
            events.append(["schedule", delay, callback, value])
            emu.reg_write(UC_X86_REG_ESP, sp+4)
            emu.reg_write(UC_X86_REG_EIP, ret)
        elif address == 0x511A10:
            ret, value = struct.unpack("<2I", emu.mem_read(sp,8))
            events.append(["remove", value])
            emu.reg_write(UC_X86_REG_ESP, sp+8)
            emu.reg_write(UC_X86_REG_EIP, ret)

    u.hook_add(UC_HOOK_CODE, api)

    def run(entry):
        u.reg_write(UC_X86_REG_ESP, stack)
        u.mem_write(stack, struct.pack("<2I", stop, slot))
        u.emu_start(entry, 0, count=300)

    run(0x4EE260)
    visible = bytes(u.mem_read(0xABF4C8+slot,1))[0]
    active = bytes(u.mem_read(0xABF6B8+slot,1))[0]
    delayed = bool(off_icon and option)
    assert visible == 0 and active == int(delayed)
    assert events == ([["schedule", 10000, 0x4EE230, slot]] if delayed else [["remove", slot]])
    before = list(events)
    if delayed:
        run(0x4EE230)
        assert bytes(u.mem_read(0xABF6B8+slot,1)) == b"\0"
        assert events[-1] == ["remove",slot]
    return {"off_icon": off_icon, "option": option, "after_expiry": {
        "visible": visible, "active": active, "calls": before},
        "after_optional_callback": events}


def main():
    image = TW.read_bytes()
    # XML parser local row starts EBP-0x48 and copies 15 DWORDs to the
    # table at AC4CD8; these instruction bytes bind names to real offsets.
    anchors = {
        "offIcon_name": (0x44A715, bytes.fromhex("6854d58c00")),
        "offIcon_store": (0x44A737, bytes.fromhex("8945d8")), # -28 => row+20
        "priority_store": (0x44A761, bytes.fromhex("8945e0")), # -20 => row+28
        "graphic_store": (0x44A612, bytes.fromhex("8955d0")), # -30 => row+18
        "group_store": (0x44A523, bytes.fromhex("8945c0")), # -40 => row+08
        "copy_row": (0x44A873, bytes.fromhex("b90f0000008d75b8f3a5")),
    }
    for _, (va, expected) in anchors.items():
        assert image[va-0x400000:va-0x400000+len(expected)] == expected, hex(va)
    tests = [replay(image, icon, flag) for icon in (0,5400) for flag in (0,1)]
    raw, rows = catalog.native_rows()
    effects = catalog.parse_effects((R / "build/inputs/effectlist2.xml").read_bytes())
    differences = [{"slot":i, "native_group": rows[i][2],
                    "xml_group": int(effects[i].get("group","0")),
                    "name": effects[i].get("startMsgName")}
                   for i in range(76) if rows[i][2] != int(effects[i].get("group","0"))]
    result = {
        "status": "native-expiry-and-layout-qualified",
        "binary": {"path": str(TW), "sha256": hashlib.sha256(image).hexdigest()},
        "anchors": {k: {"va":hex(a),"bytes":b.hex()} for k,(a,b) in anchors.items()},
        "TW_row_offsets": {"group":8, "startMsg":12, "endMsg":16,
                           "graphic":24, "offIcon":32, "priority":40},
        "expiry_replays": tests, "group_differences": differences,
        "implementation_status": "audit-only; not applied to candidate",
        "findings": [
            "offIcon requires expiry mode AND client option 0x9661D8, retaining a status entry for 10 seconds.",
            "LiTo 0x4875A0 immediately clears active and removes entry; swapping icon alone cannot reproduce offIcon.",
            "TW group replacement calls full state cleanup; LiTo 0x49F9F0 removes matching UI entries. Do not copy changed groups until cleanup is ported.",
            "TW effect row +0x0C is startMsg, not graphic. The 0x4EE600 use of +0x0C must not be treated as an SPR trigger.",
            "Priority/render-graphic consumers remain to be qualified."
        ],
        "limits": ["Scheduler and UI removal intercepted; no game or live timer.",
                   "Parser field stores anchored; full XML parser not executed."],
        "database_modified": False, "formal_updated": False,
    }
    (R / "build/status-semantics.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), "utf-8")
    print("PASS 4 expiry branches + delayed removal; 6 parser-layout anchors; no runtime semantics changed")


if __name__ == "__main__":
    main()
