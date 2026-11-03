"""Read only the one filename classifier and its admissible fixed route cells."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,struct
from pathlib import Path
from datetime import datetime
import capstone
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'Native-filename-route-contract'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
    p=Path(p);b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def load(p):return json.loads(Path(p).read_text('utf-8-sig'))
def read(p):
    p=Path(p);before=pin(p);b=p.read_bytes();after=pin(p);return b,{'before':before,'content_sha256':sha(b),'after':after,'stable':before==after and before['sha256']==sha(b),'read_at':datetime.now().astimezone().isoformat()}
def save(n,j):
    OUT.mkdir(exist_ok=True);p=OUT/n;assert p.resolve().is_relative_to(ROOT);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    prior=load(ROOT/'Native-image-bank-boundary-contract.json');src=next(q for q in prior['sources'] if Path(q['path']).name=='LiTo.unpacked-oep-current.exe');b,meta=read(src['path']);assert meta['before']==src and meta['stable']
    frag=ROOT.parent/'lito重建版/LiTo381-Modern/target/r498-character-ui-study/sub_43FFC0.asm';fb,fm=read(frag);assert fm['stable'] and fm['before']['sha256']=='37310897CB0F7D075E7E9142245A072F9E62BB951828F10DF96873D227E83D3D'
    save('existing-resolver-ASM.json',{'read':fm,'source_lines':[{'line':i+1,'text':s} for i,s in enumerate(fb.decode('utf-8-sig').splitlines())],'scope':'Only existing R498 direct43FFC0 body; callers in header are navigation, not opened. No other privatefunction expansion.'})
    nt=struct.unpack_from('<I',b,60)[0];n=struct.unpack_from('<H',b,nt+6)[0];opts=struct.unpack_from('<H',b,nt+20)[0];base=struct.unpack_from('<I',b,nt+52)[0]
    def raw(va,size):
        rv=va-base
        for i in range(n):
            vs,st,rs,fp=struct.unpack_from('<4I',b,nt+24+opts+i*40+8)
            if st<=rv and rv+size<=st+rs:
                off=fp+rv-st;return b[off:off+size],{'source':src,'va':f'{va:08X}','rva':f'{rv:08X}','file_offset':off,'bytes':size}
        raise AssertionError('not raw-backed')
    code,origin=raw(0x43FFC0,65);cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins=[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(code,0x43FFC0)];assert b''.join(bytes.fromhex(i['bytes']) for i in ins)==code and ins[-1]['va']=='00440000' and ins[-1]['mnemonic']=='ret' and not any(i['mnemonic']=='call' for i in ins)
    table,torg=raw(0x6479E8,64);cells=[{'nonnegative_route_index':i,'va':f'{0x6479E8+4*i:08X}','raw_u32':v,'live_provider_identity':None} for i,v in enumerate(struct.unpack('<16I',table))]
    save('fixed-PE-resolver-and-route-cells.json',{'PE_read':meta,'resolver':{'origin':origin,'raw_hex':code.hex(),'raw_sha256':sha(code),'instructions':ins},'admissible_nonnegative_route_cells':{'origin':torg,'raw_hex':table.hex(),'raw_sha256':sha(table),'cells':cells,'qualification':'Exactly16 addresses used by current shortASCII filename domain,6479E8..647A24. Allfixedrawzero;notliveproviderabsenceorallocatedarrayextent. Generalnegative signedremainder notallowed bythis caller-domain proof.'},'calls_within_resolver':0,'static_string_or_lookup_table_inside_resolver':False,'no_reader_or_provider_writer_body_opened':True})
    # Illustrative offline byte arithmetic, no target function/model/process execution or index search.
    names=['0.png','79.png','809.png','79.img','79e.img','79c.img','79j.img','79h.img','0.PNG','19.png','91.png']
    examples=[]
    for name in names:
        total=sum(name.encode('ascii'));route=total&15;examples.append({'source_requested_name_example':name,'unsigned_ASCII_byte_sum':total,'route':route,'bucket_va':f'{0x6479E8+4*route:08X}','provider':'unknown;fixedrawcellzero','qualification':'Sourceformula arithmetic witness only;formatter/fileexistence/provider/read notexecuted. No filenameassetlookup.'})
    save('source-arithmetic-route-examples.json',{'examples':examples,'not_Native_execution_or_test_PASS':True,'not_new_filename_search':True,'case_policy':'No explicitcasefold in43FFC0;ASCIIcase toggles change sum bymultiplesof32,so thesebucketexamplescollide. Doesnotprovebackingfilelookupcasepolicy.','ordering_policy':'Byteadditionignoresbyteorder;19.png/91.pngcollision demonstrates routeisnotuniqueidentity/namespace.'})
    print(json.dumps({'resolver_bytes':len(code),'instruction_rows':len(ins),'nested_calls':0,'route_cells':len(cells),'raw_values':list(struct.unpack('<16I',table)),'example_count':len(examples)},ensure_ascii=False))
if __name__=='__main__':main()
