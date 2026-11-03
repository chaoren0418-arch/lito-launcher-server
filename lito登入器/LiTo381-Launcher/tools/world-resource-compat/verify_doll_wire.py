"""Qualify actual TW packet-box branches and LiTo's unsupported dispatch.

Execute the native scalar readers and graphic lookup. Status apply/clear are
intercepted. No game, SQL, or server runs; do not imply a receiver fix exists.
"""
import hashlib
import json
from pathlib import Path
import struct

import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EBP, UC_X86_REG_ESP, UC_X86_REG_EIP
from unicorn.x86_const import UC_X86_REG_ESI, UC_X86_REG_EAX, UC_X86_REG_ECX
import catalog
from verify_blue_identity import TW

R = Path(__file__).resolve().parent


def pin(path):
    return {"path":str(path),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}


def setup(image, effects):
    u = Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000,(len(image)+4095)&~4095)
    u.mem_write(0x400000,image)
    u.mem_map(0x2000000,0x50000)
    table, manager, indices = 0x2004000,0x2018000,0x2019000
    u.mem_write(0xAC4CD8,struct.pack("<I",table))
    for key,e in effects.items():
        row=[0]*15
        for offset,field in ((0,"icon"),(4,"duration"),(8,"group"),(12,"startMsg"),
                             (16,"endMsg"),(24,"graphic"),(32,"offIcon"),(40,"priority")):
            row[offset//4]=int(e.get(field,0))
        u.mem_write(table+60*key,struct.pack("<15I",*(value & 0xFFFFFFFF for value in row)))
    keys=[key for key,e in effects.items() if int(e.get("graphic",0))>0]
    u.mem_write(indices,struct.pack("<"+"I"*len(keys),*keys))
    u.mem_write(manager+0x58,struct.pack("<3I",len(keys),len(keys),indices))
    u.mem_write(0xC2F86C,struct.pack("<I",manager))
    return u


def replay_tw(image, effects, subcode, seconds, graphic):
    u=setup(image,effects)
    frame,stack,packet=0x2030000,0x2020000,0x2040000
    payload=struct.pack("<HH",seconds,graphic) if subcode==56 else struct.pack("<HI",seconds,graphic)
    u.mem_write(packet,payload)
    u.mem_write(frame+8,struct.pack("<I",packet))
    u.reg_write(UC_X86_REG_EBP,frame);u.reg_write(UC_X86_REG_ESP,stack)
    branch=struct.unpack_from("<I",image,0x5415B4-0x400000+subcode*4)[0]
    calls=[];done=False
    def trace(emu,address,size,_):
        nonlocal done
        if address==0x541596:
            done=True;emu.emu_stop()
        elif address in (0x4EE400,0x4EE2D0):
            sp=emu.reg_read(UC_X86_REG_ESP)
            count=4 if address==0x4EE400 else 2
            args=list(struct.unpack("<"+"I"*count,emu.mem_read(sp+4,count*4)))
            calls.append(["apply" if count==4 else "clear",*args])
            ret=struct.unpack("<I",emu.mem_read(sp,4))[0]
            emu.reg_write(UC_X86_REG_ESP,sp+4);emu.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,trace)
    u.emu_start(branch,0,count=50000)
    assert done
    consumed=struct.unpack("<I",u.mem_read(frame+8,4))[0]-packet
    assert consumed==len(payload)
    return {"subcode":subcode,"seconds":seconds,"graphic":graphic,
            "body_bytes_consumed":consumed,"calls":calls}


def main():
    effects=catalog.parse_effects((R/"build/inputs/effectlist2.xml").read_bytes())
    image=TW.read_bytes()
    pe=pefile.PE(str(catalog.NATIVE));native=pe.get_memory_mapped_image();pe.close()
    rows=[]
    for seconds in (0,1,600,32767):
        for gfx in (0,3103,3104,3113,3114):
            row=replay_tw(image,effects,56,seconds,gfx)
            effect=92 if 3104<=gfx<=3113 else 82
            assert row["calls"]==([["apply",seconds,effect,0,0xFFFFFFFF]] if seconds else
                                  [["clear",82,0],["clear",92,0]])
            rows.append(row)
        for gfx in (512,513,514,515,516,65535):
            row=replay_tw(image,effects,154,seconds,gfx)
            if gfx==65535:
                assert row["calls"]==[]
            else:
                assert row["calls"]==([["apply",seconds,gfx,0,0xFFFFFFFF]] if seconds else
                                      [["clear",gfx,0]])
            rows.append(row)
    rejected=[]
    for code in (56,154):
        u=Uc(UC_ARCH_X86,UC_MODE_32)
        u.mem_map(0x400000,(len(native)+4095)&~4095);u.mem_write(0x400000,native)
        u.mem_map(0x2000000,0x10000)
        u.mem_write(0x2001000,bytes([code])+b"\0"*12)
        u.reg_write(UC_X86_REG_ESI,0x2001000)
        u.reg_write(UC_X86_REG_ECX,4)
        reached=[]
        def trace(emu,address,size,_):
            if address==0x4B24E1:reached.append(address);emu.emu_stop()
        u.hook_add(UC_HOOK_CODE,trace)
        # Start after SEH/probe and before native byte read. World state=4.
        u.emu_start(0x4B07EE,0,count=30)
        assert reached==[0x4B24E1]
        rejected.append(code)
    contract=json.loads((R/"build/presentation-contract.json").read_text("utf-8"))
    wire=contract["wire_cases"]["tw13081901-v1"]
    assert len(bytes.fromhex(wire["doll/600"]))==4
    assert bytes.fromhex(wire["dollCustom/5935/600"])[2:8]==struct.pack("<HI",600,5935)
    dispatch=[]
    for opcode in (0xFA,0x7B):
        u=Uc(UC_ARCH_X86,UC_MODE_32)
        u.mem_map(0x400000,(len(native)+4095)&~4095);u.mem_write(0x400000,native)
        u.mem_map(0x2000000,0x10000)
        u.mem_write(0x2001000,bytes([opcode,56,88,2]))
        u.mem_write(0x64BBD4,struct.pack("<I",4))
        u.mem_write(0x64AF98,struct.pack("<I",3))
        u.reg_write(UC_X86_REG_ESI,0x2001000)
        u.reg_write(UC_X86_REG_ESP,0x200F000)
        reached=[]
        def trace(emu,address,size,_):
            if address in (0x4AC1C0,0x4B07C0):
                reached.append(address);emu.emu_stop()
        u.hook_add(UC_HOOK_CODE,trace)
        u.emu_start(0x4B29DB,0,count=80)
        assert reached==[0x4AC1C0 if opcode==0xFA else 0x4B07C0]
        dispatch.append({"opcode":opcode,"receiver":hex(reached[0])})
    assert bytes.fromhex(wire["doll/600"])[0]==0xFA
    report={
        "status":"receiver-gap-qualified",
        "inputs":[pin(TW),pin(catalog.NATIVE),pin(R/"build/inputs/effectlist2.xml"),
                  pin(R/"build/presentation-contract.json")],
        "native_tw_replays":rows,"unmodified_lito_rejected_subcodes":rejected,
        "actual_world_opcode_dispatch":dispatch,
        "binding_rows":[effects[key] for key in (82,92,512,513,514,515,516)],
        "findings":[
            "Current producer opcodeFA reaches native4AC1C0, not packet-box4B07C0. Native packet-box opcode is7B.",
            "Protocol also reserves FA as S_LITO_CLAN. Do not globally reinterpret FA; correct only the isolated doll producer and add a7B subcode adapter together.",
            "TW56 reads seconds and a second sprite word; 3104..3113 selects effect92, otherwise82; zero clears both.",
            "Current LiTo server56 has only seconds. Adapter must not read the absent second word; generic82 is the evidenced existing producer contract.",
            "TW154 reads seconds+DWORD graphic and resolves XML graphic, not XML icon; 512..516 resolve to corresponding doll effects.",
            "Current server154 emits the DWORD as H(iconId)+H(0); its trailing removal word is not consumed by this TW branch; seconds0 removes.",
        ],
        "limits":["Native apply/clear intercepted; UI not rendered.","Candidate DLL receiver adapter not implemented by this audit.",
                  "No assumption that arbitrary source database iconId values are graphics; source DB not read or changed."],
        "formal_updated":False,"source_database_modified":False,
    }
    (R/"build/doll-wire.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),"utf-8")
    print(f"PASS {len(rows)} TW packet-box replays and 2 LiTo unsupported branches; exact producer gap recorded")


if __name__=="__main__":
    main()
