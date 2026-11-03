"""Audit remaining non-icon status packet routing without altering their logic."""
from pathlib import Path
import hashlib
import json
import shutil
import struct
import subprocess
import pefile
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESI,UC_X86_REG_ESP
import catalog

R=Path(__file__).resolve().parent
def pin(p):return {"path":str(p),"sha256":hashlib.sha256(p.read_bytes()).hexdigest()}

def main():
    paired=json.loads((R/"build/presentation-server.json").read_text("utf-8"))
    jar=Path(paired["candidate"]["path"])
    assert pin(jar)==paired["candidate"]
    javac=Path(shutil.which("javac"));java=javac.with_name("java.exe")
    work=R/"build/presentation-probe"
    subprocess.run([str(javac),"-d",str(work),str(R/"StatusFamilyProbe.java")],check=True)
    run=subprocess.run([str(java),"-Xverify:all","-cp",str(work)+";"+str(jar),
                        "StatusFamilyProbe"],capture_output=True,text=True,check=True)
    cases=dict(line.split("=",1) for line in run.stdout.splitlines())
    native=pefile.PE(str(catalog.NATIVE)).get_memory_mapped_image()
    routes=[]
    for label,raw in cases.items():
        packet=bytes.fromhex(raw);family=label.split("/")[0]
        expected={"poison":0x4ACCB0,"blind":0x4A6F40,"invisible":0x4ACE30,
                  "paralysis":0x4A6A80}[family]
        u=Uc(UC_ARCH_X86,UC_MODE_32)
        u.mem_map(0x400000,(len(native)+4095)&~4095);u.mem_write(0x400000,native)
        u.mem_map(0x2000000,0x10000)
        u.mem_write(0x2001000,packet);u.mem_write(0x64BBD4,struct.pack("<I",4))
        u.mem_write(0x64AF98,struct.pack("<I",3))
        u.reg_write(UC_X86_REG_ESI,0x2001000);u.reg_write(UC_X86_REG_ESP,0x200F000)
        reached=[]
        def trace(emu,address,size,_):
            if not 0x4B2970<=address<0x4B3208:
                reached.append(address);emu.emu_stop()
        u.hook_add(UC_HOOK_CODE,trace);u.emu_start(0x4B29DB,0,count=100)
        assert reached==[expected],(label,raw,[hex(a) for a in reached],hex(expected))
        routes.append({"case":label,"packet":raw,"native_receiver":hex(expected)})
    result={"status":"non-icon-native-routing-qualified","inputs":[pin(jar),pin(catalog.NATIVE),
        pin(R/"StatusFamilyProbe.java")],"routes":routes,
        "classification":{
            "icon_paths":"shield selectors, strength/dexterity, breath, speed/brave, own graphic, packetbox34/56/154: separate compiled receiver tests",
            "non_icon_paths":"poison actor color, blindness viewport, invisibility actor visibility, paralysis input/state remain native",
            "not_resource_presentation":"characterStatus/magicStatus/criminalStatus and lobby/buddy/server status retain their existing gameplay/UI contracts"},
        "limits":["This proves paired writer-to-native routing, not a new implementation of negative spell gameplay.",
                  "No claim that all optional TW-only entity flags or every server skill are ported by this resource candidate."]}
    (R/"build/status-family-coverage.json").write_text(json.dumps(result,indent=2),"utf-8")
    print(f"PASS {len(routes)} non-icon status packets reach their existing native receivers")

if __name__=="__main__":main()
