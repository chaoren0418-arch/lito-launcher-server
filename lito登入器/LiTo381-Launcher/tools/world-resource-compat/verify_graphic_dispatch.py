"""Replay TW own-effect dispatch, preserving special-case exclusions explicitly."""
from pathlib import Path
import hashlib
import json
import struct
import pefile
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE, UcError
from unicorn.x86_const import UC_X86_REG_EIP, UC_X86_REG_ESP, UC_X86_REG_EAX
import catalog
from verify_blue_identity import TW

R = Path(__file__).resolve().parent


def replay(image, effects, gfx):
    u=Uc(UC_ARCH_X86, UC_MODE_32)
    u.mem_map(0x400000,(len(image)+4095)&~4095)
    u.mem_write(0x400000,image)
    u.mem_map(0x2000000,0x40000)
    table,stack,stop=0x2004000,0x203F000,0x2000000
    u.mem_write(0xAC4CD8,struct.pack("<I",table))
    for key,e in effects.items():
        row=[0]*15
        for offset,field in [(0,"icon"),(4,"duration"),(8,"group"),(12,"startMsg"),
                             (16,"endMsg"),(24,"graphic"),(32,"offIcon"),(40,"priority")]:
            row[offset//4]=int(e.get(field,0))
        u.mem_write(table+60*key,struct.pack("<15I",*(value&0xFFFFFFFF for value in row)))
    manager,indices=0x2018000,0x2019000
    graphic_ids=[key for key,e in effects.items() if int(e.get("graphic",0))>0]
    u.mem_write(indices,struct.pack("<"+"I"*len(graphic_ids),*graphic_ids))
    u.mem_write(manager+0x58,struct.pack("<3I",len(graphic_ids),len(graphic_ids),indices))
    u.mem_write(0xC2F86C,struct.pack("<I",manager))
    u.reg_write(UC_X86_REG_ESP,stack)
    u.mem_write(stack,struct.pack("<5I",stop,gfx,0xFFFFFFFF,0xFFFFFFFF,1))
    results=[];lookups=[];unknown=[];completed=False
    # Only external calls actually reached are intercepted; special-case
    # dependencies that aren't qualified stop this row rather than guessing.
    def trace(emu,addr,size,_):
        nonlocal completed
        sp=emu.reg_read(UC_X86_REG_ESP)
        if addr==stop:
            completed=True
            emu.emu_stop();return
        if addr==0x44AB68:
            value=emu.reg_read(UC_X86_REG_EAX);lookups.append(value if value<0x80000000 else value-0x100000000)
        if not (0x52E7D0<=addr<0x530FEF or 0x44AB10<=addr<0x44AB6C or 0x44AD70<=addr<0x44AD87):
            ret=struct.unpack("<I",emu.mem_read(sp,4))[0]
            pop=4
            if addr==0x4EE400:
                results.append(list(struct.unpack("<4I",emu.mem_read(sp+4,16))))
            elif addr in (0x4387C0,0x79A116):
                pass # message display / compiler cookie check; neither defines status identity
            else:
                unknown.append(hex(addr));emu.emu_stop();return
            emu.reg_write(UC_X86_REG_ESP,sp+pop);emu.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,trace)
    try:u.emu_start(0x52E7D0,0,count=50000)
    except UcError as e:unknown.append(str(e))
    return {"graphic":gfx,"status_calls":results,"lookups":lookups,"unqualified_calls":unknown,
            "completed":completed}


def replay_lito(image,gfx):
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,(len(image)+4095)&~4095);u.mem_write(0x400000,image)
    u.mem_map(0x2000000,0x10000);stack=0x200F000;stop=0x2000000
    u.mem_write(stack,struct.pack("<I",stop));u.reg_write(UC_X86_REG_ESP,stack)
    from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EDX
    u.reg_write(UC_X86_REG_ECX,gfx);u.mem_write(0x834BC0,b"\0"*4)
    result={"calls":[],"unknown":[],"completed":False}
    def trace(emu,addr,size,_):
        if addr==stop:result["completed"]=True;emu.emu_stop();return
        if not 0x4AD700<=addr<0x4ADDA0:
            sp=emu.reg_read(UC_X86_REG_ESP);ret=struct.unpack("<I",emu.mem_read(sp,4))[0]
            if addr==0x487600:
                result["calls"].append(["apply",emu.reg_read(UC_X86_REG_ECX),emu.reg_read(UC_X86_REG_EDX)])
            elif addr==0x4875C0:result["calls"].append(["clear",emu.reg_read(UC_X86_REG_ECX)])
            elif addr==0x417860:
                # fastcall message uses one stack argument
                emu.reg_write(UC_X86_REG_ESP,sp+8);emu.reg_write(UC_X86_REG_EIP,ret);return
            else:result["unknown"].append(hex(addr));emu.emu_stop();return
            emu.reg_write(UC_X86_REG_ESP,sp+4);emu.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,trace)
    try:u.emu_start(0x4AD700,0,count=1000)
    except UcError as e:result["unknown"].append(str(e))
    return result


def main():
    image=TW.read_bytes()
    effects=catalog.parse_effects((R/"build/inputs/effectlist2.xml").read_bytes())
    native_table,native_rows=catalog.native_rows()
    _,report=catalog.compile_catalog((R/"build/inputs/effectlist2.xml").read_bytes(),native_table,native_rows)
    pe=pefile.PE(str(catalog.NATIVE));native=pe.get_memory_mapped_image();pe.close()
    guards=[]
    from unicorn.x86_const import UC_X86_REG_EBP,UC_X86_REG_ESI
    for is_own in (False,True):
        u=Uc(UC_ARCH_X86,UC_MODE_32)
        u.mem_map(0x400000,(len(native)+4095)&~4095);u.mem_write(0x400000,native)
        u.mem_map(0x2000000,0x10000);u.reg_write(UC_X86_REG_EBP,0x200F000)
        u.reg_write(UC_X86_REG_ESI,0x2100000 if is_own else 0x2100004)
        u.mem_write(0x834EE4,struct.pack("<I",0x2100000))
        reached=[]
        def stop_guard(emu,address,size,_):
            if address in (0x4ADED3,0x4ADF21):reached.append(address);emu.emu_stop()
        u.hook_add(UC_HOOK_CODE,stop_guard);u.emu_start(0x4ADEC0,0,count=30)
        assert reached==[0x4ADED3 if is_own else 0x4ADF21]
        guards.append({"own_object":is_own,"status_call_reached":is_own,"stop":hex(reached[0])})
    rows=[]
    for binding in report["bindings"]:
        e=binding["effect"];gfx=int(e.get("graphic",0))
        if not gfx:continue
        result=replay(image,effects,gfx)
        expected=[int(e.get("duration",0)),int(e["id"]),0,0xFFFFFFFF]
        result.update(slot=binding["slot"],effect=int(e["id"]),xml_duration=int(e.get("duration",0)))
        result["qualified_generic"]=result["completed"] and not result["unqualified_calls"] and result["status_calls"]==[expected] and int(e["id"]) in result["lookups"]
        result["qualified_no_status"]=result["completed"] and not result["unqualified_calls"] and not result["status_calls"]
        result["lito_original"]=replay_lito(native,gfx)
        rows.append(result)
    output={"status":"audited","binary_sha256":hashlib.sha256(image).hexdigest(),
            "xml_sha256":hashlib.sha256((R/"build/inputs/effectlist2.xml").read_bytes()).hexdigest(),
            "rows":rows,"qualified_generic":sum(r["qualified_generic"] for r in rows),
            "lito_own_object_guard":guards,
            "limits":["Actual graphic lookup/vector accessor executed against XML-derived table; state consumer/message display intercepted.",
                      "Special-case calls excluded; no visual animation or server started.",
                      "Only source own-object caller 0x531BDA invokes this dispatcher; never apply to other objects."]}
    (R/"build/graphic-dispatch.json").write_text(json.dumps(output,ensure_ascii=False,indent=2),"utf-8")
    print(json.dumps({"rows":len(rows),"qualified":output["qualified_generic"],
                      "excluded":[{"slot":r["slot"],"graphic":r["graphic"],"calls":r["unqualified_calls"],
                                   "status":r["status_calls"]} for r in rows if not r["qualified_generic"]]}))


if __name__=="__main__":main()
