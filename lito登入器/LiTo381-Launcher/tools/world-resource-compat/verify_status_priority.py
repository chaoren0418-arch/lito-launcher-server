"""Use TW's actual priority key and creation-order comparator as the oracle."""
import hashlib
import json
from pathlib import Path
import struct
import sys
import catalog
from functools import cmp_to_key
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP
from verify_blue_identity import TW

R=Path(__file__).resolve().parent


def main():
    image=TW.read_bytes()
    names=[]
    for i in range(13):
        ptr=struct.unpack_from("<I",image,0x966020-0x400000+4*i)[0]
        names.append(image[ptr-0x400000:].split(b"\0",1)[0].decode("ascii"))
    raw,rows=catalog.native_rows()
    _,report=catalog.compile_catalog((R/"build/inputs/effectlist2.xml").read_bytes(),raw,rows)
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,(len(image)+4095)&~4095);u.mem_write(0x400000,image)
    u.mem_map(0x2000000,0x40000)
    table,a,b,stack,stop=0x2004000,0x2010000,0x2010100,0x203F000,0x2000000
    u.mem_write(0xAC4CD8,struct.pack("<I",table))
    def hook(emu,address,size,_):
        if address==stop:emu.emu_stop()
    u.hook_add(UC_HOOK_CODE,hook)
    keys=[]
    for binding in report["bindings"]:
        e=binding["effect"];eid=int(e["id"]);kind=names.index(e["kind"]);priority=int(e.get("priority",0))
        u.mem_write(table+60*eid+28,bytes([kind]))
        u.mem_write(table+60*eid+40,struct.pack("<I",priority))
        u.mem_write(a+16,struct.pack("<I",eid))
        u.reg_write(UC_X86_REG_ECX,a);u.reg_write(UC_X86_REG_ESP,stack)
        u.mem_write(stack,struct.pack("<I",stop));u.emu_start(0x510CB0,0,count=200)
        value=u.reg_read(UC_X86_REG_EAX)&255
        if value>=128:value-=256
        expected=(priority&255) if priority>0 else kind if kind<=2 else 100
        if expected>=128:expected-=256
        assert value==expected
        keys.append({"slot":binding["slot"],"effect":eid,"kind":e["kind"],"priority":priority,"key":value})
    count=0
    for left in keys:
        for right in keys:
            for t1,t2 in ((10,20),(20,10),(0xFFFFFFFF,0)):
                u.mem_write(a+4,struct.pack("<I",t1));u.mem_write(b+4,struct.pack("<I",t2))
                u.mem_write(a+16,struct.pack("<I",left["effect"]));u.mem_write(b+16,struct.pack("<I",right["effect"]))
                u.reg_write(UC_X86_REG_ESP,stack);u.mem_write(stack,struct.pack("<3I",stop,a,b))
                u.emu_start(0x510C50,0,count=500)
                expected=left["key"]<right["key"] if min(left["key"],right["key"])<100 else t1<t2
                assert bool(u.reg_read(UC_X86_REG_EAX))==expected
                count+=1
    output={"status":"passed","binary_sha256":hashlib.sha256(image).hexdigest(),
            "xml_sha256":report["xml_sha256"],"kind_names":names,"keys":keys,"comparator_cases":count,
            "consumer":{"key":"0x510CB0","comparator":"0x510C50","mode_switch":"0x510BD0"},
            "mode":"XML priority, then native creation-order comparator; stable ties",
            "not_ported":"TW user-toggle remaining-time ordering mode; independent optional UI command"}
    log=R/"build/installer-accept.log"
    if log.exists() and "--oracle-only" not in sys.argv:
        lines=[s for s in log.read_text("utf-8").splitlines() if s.startswith("PRIORITY_ORDER:")]
        if lines:
            assert len(lines)==1
            actual=list(map(int,lines[0].split(":",1)[1].split(",")))
            by_slot={row["slot"]:row for row in keys}
            def less(left,right):
                for obj,slot in ((a,left),(b,right)):
                    stamp=0xFFFFFFFF if slot==41 else slot*7919
                    u.mem_write(obj+4,struct.pack("<I",stamp))
                    u.mem_write(obj+16,struct.pack("<I",by_slot[slot]["effect"]))
                u.reg_write(UC_X86_REG_ESP,stack);u.mem_write(stack,struct.pack("<3I",stop,a,b))
                u.emu_start(0x510C50,0,count=500)
                return bool(u.reg_read(UC_X86_REG_EAX))
            def compare(left,right):
                return -1 if less(left,right) else 1 if less(right,left) else 0
            expected=sorted(sorted(by_slot,reverse=True),key=cmp_to_key(compare))
            assert actual==expected,(actual,expected)
            output["candidate_order_verified"]=True
            output["candidate_dll_sha256"]=hashlib.sha256((R/"build/candidate/ddraw.dll").read_bytes()).hexdigest()
            output["fixture_log_sha256"]=hashlib.sha256(log.read_bytes()).hexdigest()
            output["candidate_order"]=actual
    (R/"build/status-priority.json").write_text(json.dumps(output,ensure_ascii=False,indent=2),"utf-8")
    print(f"PASS {len(keys)} native priority keys and {count} native comparator cases; candidate order={output.get('candidate_order_verified',False)}")


if __name__=="__main__":main()
