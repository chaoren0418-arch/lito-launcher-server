"""Replay TW's original self/peer brave-status branches; no game or SQL."""
import hashlib
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EBP, UC_X86_REG_ESP, UC_X86_REG_EIP
import catalog
from verify_blue_identity import TW

R = Path(__file__).resolve().parent


def replay(image, mode, seconds, own, class_id):
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, (len(image)+4095)&~4095)
    u.mem_write(0x400000, image)
    u.mem_map(0x2000000, 0x20000)
    frame, stack, actor = 0x2010000, 0x2008000, 0x2001000
    u.reg_write(UC_X86_REG_EBP, frame)
    u.reg_write(UC_X86_REG_ESP, stack)
    u.mem_write(frame-12, struct.pack("<I", actor))
    u.mem_write(frame-5, bytes([mode]))
    u.mem_write(frame-16, struct.pack("<H", seconds))
    u.mem_write(0xC2D2B8, struct.pack("<I", actor if own else actor+0x100))
    u.mem_write(0xC31544, bytes([class_id]))
    calls = []

    def trace(emu, address, size, _):
        if address == 0x534F30:
            emu.emu_stop()
            return
        if 0x534D3D <= address < 0x534F30:
            return
        sp = emu.reg_read(UC_X86_REG_ESP)
        ret = struct.unpack("<I", emu.mem_read(sp, 4))[0]
        extra = 0
        if address == 0x4EE400:
            calls.append(["apply", *struct.unpack("<4i", emu.mem_read(sp+4, 16))])
        elif address == 0x4EE2D0:
            calls.append(["clear", *struct.unpack("<2I", emu.mem_read(sp+4, 8))])
        elif address == 0x579E10:
            extra = 4  # thiscall UI state, one stack parameter
        elif address != 0x4387C0:
            raise AssertionError(hex(address))
        emu.reg_write(UC_X86_REG_ESP, sp+4+extra)
        emu.reg_write(UC_X86_REG_EIP, ret)

    u.hook_add(UC_HOOK_CODE, trace)
    u.emu_start(0x534D3D, 0, count=500)
    expected = []
    if own:
        if mode == 0:
            expected = [["clear", v, 0] for v in (2, 37, 110)]
        elif mode in (1, 3, 5, 6):
            expected = [["apply", seconds, 110 if mode == 6 else 2, 0, -1]]
        elif mode == 4:
            effect = {2:24, 3:42}.get(class_id, 37)
            expected = [["apply", seconds, effect, 0, -1]]
    assert calls == expected, (mode, seconds, own, class_id, calls, expected)
    assert u.mem_read(actor+0x29, 1) == bytes([mode])
    return {"mode":mode, "seconds":seconds, "own":own, "class":class_id, "calls":calls}


def main():
    image = TW.read_bytes()
    rows = [replay(image, mode, seconds, own, cls)
            for mode in (0, 1, 3, 4, 5, 6)
            for seconds in (0, 1, 600, 32767)
            for own in (False, True)
            for cls in (0, 2, 3)]
    effects = catalog.parse_effects((R/"build/inputs/effectlist2.xml").read_bytes())
    assert effects[110]["startMsgName"] == "BLOODLUST_S"
    assert int(effects[110]["icon"]) == 3075
    result = {"status":"passed", "input":{"path":str(TW),
        "sha256":hashlib.sha256(image).hexdigest()}, "cases":rows,
        "bloodlust_effect":effects[110],
        "scope":"TW status identity/removal and actor mode; animation-rate modifiers require separate audit"}
    (R/"build/brave-receiver.json").write_text(json.dumps(result, indent=2), "utf-8")
    print(f"PASS {len(rows)} TW brave status branches, including own/peer, mode6 and removal")


if __name__ == "__main__":
    main()
