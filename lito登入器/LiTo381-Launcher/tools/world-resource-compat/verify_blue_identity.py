"""Replay real packet-box 34 branches; stop at the status consumer, no game/SQL."""
import hashlib
import json
from pathlib import Path
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EBP, UC_X86_REG_ESP, UC_X86_REG_ESI
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_EDX
from unicorn.x86_const import UC_X86_REG_EAX
import catalog

R = Path(__file__).resolve().parent
TW = Path(r"D:\lineage開發\經典版UI資源\證據原檔\TW13081901.memory.bin")
SOURCE = Path(r"D:\lineage開發\1-伺服器")


def pin(p):
    return {"path": str(p), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}


def replay(image, branch, consumer, stop, seconds, tw=False, condition=0):
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, (len(image) + 4095) & ~4095)
    u.mem_write(0x400000, image)
    u.mem_map(0x2000000, 0x20000)
    frame, stack, packet = 0x2010000, 0x2008000, 0x2018000
    u.reg_write(UC_X86_REG_EBP, frame)
    u.reg_write(UC_X86_REG_ESP, stack)
    u.mem_write(packet, struct.pack("<H", seconds))
    if tw:
        u.mem_write(frame + 8, struct.pack("<I", packet))
        u.mem_write(0x9A9198, struct.pack("<I", condition))
    else:
        u.reg_write(UC_X86_REG_ESI, packet)
    calls = []

    def trace(emu, address, size, _):
        if address == consumer:
            if tw:
                sp = emu.reg_read(UC_X86_REG_ESP)
                args = struct.unpack("<4I", emu.mem_read(sp + 4, 16))
                calls.append(list(args))
            else:
                calls.append([emu.reg_read(UC_X86_REG_ECX),
                              emu.reg_read(UC_X86_REG_EDX)])
            emu.emu_stop()
        elif address == stop:
            emu.emu_stop()

    u.hook_add(UC_HOOK_CODE, trace)
    u.emu_start(branch, 0, count=200)
    expected = [] if seconds == 0 else [[seconds, 91 if condition == 0x9A1 else 38,
                                        0, 0xFFFFFFFF] if tw else [seconds, 38]]
    assert calls == expected, (seconds, tw, condition, calls)
    return {"seconds": seconds, "conditional_global": condition if tw else None,
            "calls": calls}


def main():
    pe = pefile.PE(str(catalog.NATIVE))
    native = pe.get_memory_mapped_image()
    pe.close()
    tw = TW.read_bytes()
    li_branch = struct.unpack_from("<I", native, 0x4B24F4 - 0x400000 + 34*4)[0]
    tw_branch = struct.unpack_from("<I", tw, 0x5415B4 - 0x400000 + 34*4)[0]
    assert (li_branch, tw_branch) == (0x4B1B39, 0x53ACFF)
    tests = []
    for seconds in (0, 1, 10, 600, 32767):
        tests.append({"client": "LiTo", **replay(native, li_branch, 0x487600, 0x4B24E1, seconds)})
        for condition in (0, 0x9A1):
            tests.append({"client": "TW", **replay(tw, tw_branch, 0x4EE400, 0x541596,
                                                  seconds, True, condition)})
    packet = next(SOURCE.rglob("S_PacketBox.java"))
    producer = next(SOURCE.rglob("UserMprBlue.java"))
    assert "ICON_BLUEPOTION = 34" in packet.read_text("utf-8")
    assert "S_PacketBox.ICON_BLUEPOTION" in producer.read_text("utf-8")
    effect = catalog.parse_effects((R / "build/inputs/effectlist2.xml").read_bytes())[38]
    assert effect["startMsgName"] == "POTION_OF_MANA1_S"
    identities=[]
    # Both item constructors copy their fourth argument (inventory graphic H)
    # into their item row; the item-selection paths then publish that same
    # signed word. Replay the actual stores, not an inferred class/map field.
    for client,image,constructor,constructor_stop,selection,selection_stop,field,global_address in (
        ("LiTo",native,0x442889,0x4428A9,0x40BA1B,0x40BA25,0x6E,0x647EC0),
        ("TW",tw,0x4ADEE8,0x4ADEF6,0x417300,0x417310,0x9A,0x9A9198),
    ):
        for graphic in (0,2464,2465,2466,65535):
            u=Uc(UC_ARCH_X86,UC_MODE_32)
            u.mem_map(0x400000,(len(image)+4095)&~4095);u.mem_write(0x400000,image)
            u.mem_map(0x2000000,0x10000)
            frame,item=0x2008000,0x2001000
            u.reg_write(UC_X86_REG_EBP,frame);u.reg_write(UC_X86_REG_ESI,item)
            u.mem_write(frame-0x10,struct.pack("<I",item))
            u.mem_write(frame+0x14,struct.pack("<I",graphic))
            u.emu_start(constructor,constructor_stop,count=30)
            assert struct.unpack("<H",u.mem_read(item+field,2))[0]==graphic
            u.reg_write(UC_X86_REG_EAX,item)
            u.mem_write(frame-4,struct.pack("<I",item))
            u.emu_start(selection,selection_stop,count=15)
            signed=graphic if graphic<32768 else graphic-65536
            actual=struct.unpack("<i",u.mem_read(global_address,4))[0]
            assert actual==signed
            identities.append({"client":client,"graphic":graphic,"selected_value":actual,
                "constructor":hex(constructor),"selection":hex(selection),
                "item_graphic_field":hex(field),"selected_graphic_global":hex(global_address)})
    result = {
        "status": "passed", "inputs": [pin(p) for p in (catalog.NATIVE, TW, packet, producer)],
        "subcode": 34, "native_slot": 38, "shared_effect": 38,
        "LiTo_branch": hex(li_branch), "TW_branch": hex(tw_branch),
        "LiTo_branch_bytes": native[li_branch-0x400000:li_branch-0x400000+21].hex(),
        "TW_branch_bytes": tw[tw_branch-0x400000:0x53AD5A-0x400000].hex(),
        "effect": effect, "replays": tests,
        "selected_item_identity_replays":identities,
        "condition_identity":"Last selected inventory graphic, copied from item constructor fourth argument; LiTo0x647EC0 corresponds to TW0x9A9198.",
        "limits": ["Status consumers intercepted; no UI rendered.",
                   "TW selected inventory graphic2465 selects effect91; candidate receiver must use proved LiTo counterpart.",
                   "Both branches ignore zero duration; removal behavior unchanged."],
        "source_database_modified": False, "game_started": False,
    }
    (R / "build/blue-potion-identity.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2), "utf-8")
    print(f"PASS {len(tests)} native branch replays: packet-box34 -> slot38; TW conditional91 recorded")


if __name__ == "__main__":
    main()
