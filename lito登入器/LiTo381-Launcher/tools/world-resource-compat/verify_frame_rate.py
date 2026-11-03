"""Execute TW's frame-rate function for emitted speed/brave modes.

Table lookup, protected integer decoding and non-special sprite classification
are controlled inputs. The native rate arithmetic and action predicate execute.
"""
import hashlib
import itertools
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EIP, UC_X86_REG_ESP
from verify_blue_identity import TW

R=Path(__file__).resolve().parent
ACTIONS=(0,1,4,5,11,12,18,19,20,21,24,25,40,46,47,50,51,54,58,62,83,84)


def main():
    image=TW.read_bytes()
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,(len(image)+4095)&~4095);u.mem_write(0x400000,image)
    u.mem_map(0x2000000,0x20000)
    actor,table,stack,stop=0x2001000,0x2002000,0x201F000,0x2000000
    selected={}
    complete=False

    def trace(emu,address,size,_):
        nonlocal complete
        if address==stop:
            complete=True;emu.emu_stop();return
        if 0x5AE900<=address<=0x5AEBD9 or 0x5AE530<=address<=0x5AE773:return
        sp=emu.reg_read(UC_X86_REG_ESP)
        ret=struct.unpack("<I",emu.mem_read(sp,4))[0]
        if address==0x449750:value=table
        elif address==0x402800:
            key=emu.reg_read(UC_X86_REG_ECX)
            value={0xBDC7C8:0,0xBDC738:selected["speed"],0xBDC7BC:selected["brave"]}[key]
        elif address==0x4010F0:value=0 # ordinary actor, not a sprite exempt from brave
        else:raise AssertionError(hex(address))
        emu.reg_write(UC_X86_REG_EAX,value)
        emu.reg_write(UC_X86_REG_ESP,sp+4);emu.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,trace)
    cases=[];totals=[]
    u.mem_write(0xC2D2BC,struct.pack("<I",0x2004000))
    for action,speed,brave,duration,own,burst in itertools.product(
            ACTIONS,(0,1,2,17,18),(0,1,3,4,5,6),(1,41,80,333),(0,1),(0,3)):
        selected.update(speed=speed,brave=brave)
        u.mem_write(actor,b"\0"*0x140)
        u.mem_write(actor+12,struct.pack("<I",1))
        u.mem_write(actor+0x14,bytes([action]))
        u.mem_write(actor+0x24,bytes([speed]))
        u.mem_write(actor+0x29,bytes([brave]))
        u.mem_write(actor+0x124,struct.pack("<I",burst))
        u.mem_write(0xABF4B4,struct.pack("<I",1 if own else 2))
        u.mem_write(0xABF751,b"\0")
        for index in range(3):u.mem_write(table+index*32+14,struct.pack("<H",duration^0x1812))
        u.mem_write(stack,struct.pack("<3I",stop,action,0))
        u.reg_write(UC_X86_REG_ESP,stack);u.reg_write(UC_X86_REG_ECX,actor)
        complete=False
        u.emu_start(0x5AE900,0,count=1000)
        assert complete and u.reg_read(UC_X86_REG_ESP)==stack+12
        result=u.reg_read(UC_X86_REG_EAX)
        assert result<=duration*2
        cases.append([action,speed,brave,duration,own,burst,result])
        if not burst:
            for frames in (1,3):
                u.mem_write(0x2004000+action*8+1,bytes([frames]))
                u.mem_write(stack,struct.pack("<2I",stop,action))
                u.reg_write(UC_X86_REG_ESP,stack);u.reg_write(UC_X86_REG_ECX,actor)
                complete=False;u.emu_start(0x5AE530,0,count=1000)
                assert complete and u.reg_read(UC_X86_REG_ESP)==stack+8
                totals.append([action,speed,brave,duration,own,frames,u.reg_read(UC_X86_REG_EAX)])
    header="struct SharedFrameCase {int action,speed,brave,duration,own,burst,expected;};\n"
    header+="static const SharedFrameCase kSharedFrameCases[]={\n"
    header+=",\n".join("{"+",".join(map(str,row))+"}" for row in cases)+"\n};\n"
    header+="struct SharedTotalCase {int action,speed,brave,duration,own,frames,expected;};\n"
    header+="static const SharedTotalCase kSharedTotalCases[]={\n"
    header+=",\n".join("{"+",".join(map(str,row))+"}" for row in totals)+"\n};\n"
    (R/"build/shared_frame_fixture.h").write_text(header,"ascii")
    receipt={"status":"native-tw-oracle-qualified","input":{"path":str(TW),
        "sha256":hashlib.sha256(image).hexdigest()},"cases":len(cases),"total_cases":len(totals),"actions":ACTIONS,
        "speed_modes":[0,1,2,17,18],"brave_modes":[0,1,3,4,5,6],
        "durations":[1,41,80,333],"own_and_peer":True,"burst_counts":[0,3],"total_frame_counts":[1,3],
        "fixture_sha256":hashlib.sha256((R/"build/shared_frame_fixture.h").read_bytes()).hexdigest(),
        "mode6_nonmovement":"TW multiplies frame time by3/4; server attack acceptance tolerance is separately0.775",
        "limits":"Ordinary actors, no extra polymorph exemptions or TW-only ancillary flags; frame arithmetic, not wall-clock rendering."}
    (R/"build/frame-rate.json").write_text(json.dumps(receipt,indent=2),"utf-8")
    print(f"PASS {len(cases)} actual TW frame and {len(totals)} action calculations; fixture generated")


if __name__=="__main__":main()
