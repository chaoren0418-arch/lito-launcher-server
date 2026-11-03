"""Qualify existing JAR producers against original LiTo receiver dispatch."""
from pathlib import Path
import hashlib
import json
import struct
import subprocess
import shutil

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EDX
from unicorn.x86_const import UC_X86_REG_ESP
import catalog

R = Path(__file__).resolve().parent
JAR = catalog.LAUNCHER / "app/server/LiTo381-SharedResources-candidate.jar"


def pin(p):
    return {"path": str(p), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}


def native_slot(image, packet):
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000, (len(image)+4095)&~4095)
    u.mem_write(0x400000, image)
    u.mem_map(0x2000000, 0x10000)
    u.reg_write(UC_X86_REG_ESP, 0x200F000)
    u.reg_write(UC_X86_REG_ECX, 0x2001000)
    u.mem_write(0x2001000, packet)
    calls = []

    def hook(emu, address, size, _):
        sp = emu.reg_read(UC_X86_REG_ESP)
        if address == 0x4A2A50:
            # Variadic packet decoder is intercepted, not claimed as covered.
            # Stack: return, packet+1, format, &word, &byte.
            ret, data, fmt, word, byte = struct.unpack("<5I", emu.mem_read(sp, 20))
            assert bytes(emu.mem_read(fmt, 3)) == b"hc\0"
            emu.mem_write(word, bytes(emu.mem_read(data, 2)))
            emu.mem_write(byte, bytes(emu.mem_read(data+2, 1)))
            emu.reg_write(UC_X86_REG_EAX, data+3)
            emu.reg_write(UC_X86_REG_ESP, sp+4)
            emu.reg_write(UC_X86_REG_EIP, ret)
        elif address == 0x487600:
            calls.append([emu.reg_read(UC_X86_REG_ECX), emu.reg_read(UC_X86_REG_EDX)])
            emu.emu_stop()

    from unicorn.x86_const import UC_X86_REG_EIP
    u.hook_add(UC_HOOK_CODE, hook)
    u.emu_start(0x4AFF30, 0, count=500)
    assert len(calls) == 1
    return calls[0]


def main():
    javac = Path(shutil.which("javac"))
    subprocess.run([str(javac), "-d", str(R / "build"), str(R / "ShieldWireProbe.java")], check=True)
    result = subprocess.run([str(javac.with_name("java.exe")), "-cp", str(R / "build")+";"+str(JAR), "ShieldWireProbe"],
                            check=True, capture_output=True, text=True, timeout=20)
    expected = {3: (5, 4), 99: (3, 5), 151: (6, 6)}
    pe = pefile.PE(str(catalog.NATIVE))
    image = pe.get_memory_mapped_image()
    pe.close()
    rows = []
    for line in result.stdout.splitlines():
        skill, selector, hexb = line.split(",")
        skill, selector = int(skill), int(selector)
        assert selector == expected[skill][0]
        packet = bytes.fromhex(hexb)
        assert packet == bytes([109, 88, 2, selector])
        seconds, slot = native_slot(image, packet)
        assert seconds == 600
        rows.append({"skill": skill, "selector": selector, "packet": hexb,
                     "original_slot": slot, "required_slot": expected[skill][1],
                     "conflict": slot != expected[skill][1]})
    assert len(rows) == 3 and sum(r["conflict"] for r in rows) == 2
    output = {"status": "qualified", "jar": pin(JAR), "native": pin(catalog.NATIVE),
              "bindings": rows, "limitations": ["Native packet decoder stubbed to documented hc layout.",
                  "Status consumer intercepted; no timer or UI executed.",
                  "Selectors 7/10 are not emitted by this JAR probe; no new binding added for them."],
              "database_modified": False, "server_started": False}
    (R / "build/shield-wire.json").write_text(json.dumps(output, ensure_ascii=False, indent=2), "utf-8")
    print(json.dumps(output["bindings"]))


if __name__ == "__main__":
    main()
