"""Bounded fixed-PE resource bank/caller/entry initializer audit, no Native execution."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,struct,csv,re
from pathlib import Path
from datetime import datetime
import capstone
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'Native-image-bank-boundary'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
    p=Path(p);b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def load(p):return json.loads(Path(p).read_text('utf-8-sig'))
def save(n,j):
    p=OUT/n;assert p.resolve().is_relative_to(ROOT);OUT.mkdir(exist_ok=True);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(p):
    p=Path(p);before=pin(p);b=p.read_bytes();after=pin(p)
    return b,{'before':before,'content_sha256':sha(b),'after':after,'stable':before==after and before['sha256']==sha(b),'read_at':datetime.now().astimezone().isoformat()}
def main():
    prior=load(ROOT/'Native-selection-resource-gap-contract.json');pepin=prior['sources'][0]
    # Select by basename instead of relying on inherited list order.
    pepin=next(q for q in prior['sources'] if Path(q['path']).name=='LiTo.unpacked-oep-current.exe');asmpin=next(q for q in prior['sources'] if Path(q['path']).name=='full-disassembly.asm')
    pb,per=read(pepin['path']);ab,ar=read(asmpin['path']);assert per['before']==pepin and ar['before']==asmpin and per['stable'] and ar['stable'];als=ab.decode('utf-8',errors='replace').splitlines()
    direct0=[{'line':i+1,'text':s} for i,s in enumerate(als) if '834BD0' in s];direct4=[{'line':i+1,'text':s} for i,s in enumerate(als) if '834BD4' in s and ('offset dword_834BD4' in s or 'mov     dword_834BD4' in s or s.startswith('dword_834BD4'))]
    # Only endpoint/initializer/entry-boundary contexts; no called allocator/resolver/decoder body.
    contexts=[('PNG bank initializer4D4A50',444747,444816),('IMG bank initializer4D4940',444598,444667),('IMG entry constructor4D4D40',445182,445204),('PNG entry constructor4D5220',445909,445929),('IMG reader formatting/provider boundary4D4D90',445241,445430),('PNG reader formatting/provider boundary4D5270',445966,446066)]
    captured=[{'name':name,'start_line':a,'end_line':z,'lines':[{'line':i,'text':als[i-1]} for i in range(a,z+1)]} for name,a,z in contexts]
    save('existing-ASM-boundary-navigation.json',{'read':ar,'direct_PNG_global_references':direct0,'selected_IMG_initializer_global_references':direct4,'contexts':captured,'qualification':'ASM navigation only. FixedPE bytes below are authoritative; IDA recorded loaded pointers differ from fixed disk zeros. Context endpoints may contain a call or next local label, never its callee body. No whole-program indirectwriter/absence claim.'})
    nt=struct.unpack_from('<I',pb,0x3c)[0];count=struct.unpack_from('<H',pb,nt+6)[0];opts=struct.unpack_from('<H',pb,nt+20)[0];base=struct.unpack_from('<I',pb,nt+52)[0];sec=[]
    for i in range(count):
        o=nt+24+opts+40*i;vs,rv,rs,fp=struct.unpack_from('<4I',pb,o+8);sec.append((rv,rs,fp))
    def raw(va,n):
        rv=va-base
        for start,size,fp in sec:
            if start<=rv and rv+n<=start+size:
                off=fp+rv-start;return pb[off:off+n],{'va':f'{va:08X}','rva':f'{rv:08X}','file_offset':off,'bytes':n,'source':pepin}
        raise AssertionError('non raw-backed')
    cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);codes=[]
    ranges=[('IMGbank_initializer',0x4D4940,0x90),('PNGbank_initializer',0x4D4A50,0x90),('IMGentry_constructor',0x4D4D40,0x1c),('PNGentry_constructor',0x4D5220,0x1c),('slot_image_consumer',0x414960,0x3c),('numeric_frame_prefix',0x415DA0,0x31),('PNGbank_mode1_consumer',0x415E4C,0x4e),('PNGbank_mode0_consumer',0x415EB9,0x6b),('IMGformat_provider_boundary',0x4D4D90,0x17f),('PNGformat_provider_boundary',0x4D5270,0x90)]
    for name,va,n in ranges:
        b,origin=raw(va,n);ins=[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(b,va)];assert sum(len(bytes.fromhex(x['bytes'])) for x in ins)==n,(name,ins[-1])
        codes.append({'name':name,'origin':origin,'raw_hex':b.hex(),'raw_sha256':sha(b),'instructions':ins,'scope':'Endpoint or at most one necessary direct initializer/entry bridge. Calls to allocator, formatter, resolver, file reader, decoder, logging/exit/render and all external/private bodies stop at call sites. Numericframeprefix does not expand state/lifecycle helper.'})
    data=[]
    fields=[('banks_disk',0x834BD0,8),('entry_vtables',0x4FA734,8),('default_provider_disk',0x64798C,4),('first_routed_provider_bucket_disk',0x6479E8,4),('IMG_selector_byte_disk',0x64B6DC,1),('IMG_formatter_pointer_disk',0x4F7314,4)]
    for name,va,n in fields:
        b,origin=raw(va,n);data.append({'name':name,'origin':origin,'raw_hex':b.hex(),'raw_sha256':sha(b),'u32_values':list(struct.unpack('<'+'I'*(n//4),b)) if n%4==0 else None,'live_value_claim':False})
    formats=[]
    for va,expected in [(0x6365B0,b'%d.img'),(0x6365B8,b'%de.img'),(0x6365C0,b'%dj.img'),(0x6365C8,b'%dh.img'),(0x6365D0,b'%dc.img'),(0x6365D8,b'%d.png')]:
        b,origin=raw(va,len(expected)+1);assert b==expected+b'\0';formats.append({'literal':expected.decode(),'origin':origin,'raw_hex':b.hex(),'raw_sha256':sha(b)})
    # Qualified jump table only confirms IMG byte selector formatting; no class/sex enum inference.
    jmp_ins=next(x for c in codes if c['name']=='IMGformat_provider_boundary' for x in c['instructions'] if x['mnemonic']=='jmp' and 'eax*4' in x['operands']);addr=int(re.search(r'0x([0-9a-f]+)',jmp_ins['operands']).group(1),16);b,origin=raw(addr,20)
    data.append({'name':'IMG_selector_jump_table','origin':origin,'raw_hex':b.hex(),'raw_sha256':sha(b),'u32_values':list(struct.unpack('<5I',b)),'live_value_claim':False})
    # Direct zero writers are bounded single statements from already selected global xrefs.
    for name,va,end,globalva in [('PNG_global_clear',0x4D4AF0,0x4D4B46,0x834BD0),('IMG_global_clear',0x4D49E0,0x4D4A36,0x834BD4)]:
        b,_=raw(va,end-va);needle=b'\xc7\x05'+struct.pack('<I',globalva)+b'\0'*4;off=b.index(needle);v=va+off;ib,origin=raw(v,10);i=next(cs.disasm(ib,v));codes.append({'name':name,'origin':origin,'raw_hex':ib.hex(),'raw_sha256':sha(ib),'instructions':[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}],'scope':'Only the direct global-zero statement; teardown helper bodies not adopted/expanded.'})
    save('fixed-PE-bank-boundaries.json',{'PE_read':per,'code_slices':codes,'data_slices':data,'filename_literals':formats,'new_binary_files_written':False,'new_asset_extraction':False,'runtime_read':False})
    # New exact queries are only the four source-proven IMG suffix variants for79.
    index=ROOT/'resources/indices/LTsurf.csv';b,ir=read(index);man=load(ROOT/'resources/archive-manifest.json');ref=next(x['index_table'] for x in man['archives'] if x['namespace']=='native' and x['stem']=='LTsurf');assert ir['before']==ref and ir['stable'];rows=list(csv.DictReader(b.decode('utf-8-sig').splitlines()));wanted={'79e.img','79c.img','79j.img','79h.img'};hits=[r for r in rows if r['name'].lower() in wanted]
    save('79-source-qualified-suffix-lookup.json',{'index_read':ir,'exact_new_queries':sorted(wanted),'matches':hits,'qualification':'Only four newly source-proven suffix literals for79 in one existing LTsurf index. Prior five-index803PNG/plain79 lookup is inherited Batch35, not replayed. Does not exclude other resolver/provider namespaces, languages, loosefiles or dynamic assets.','old_lookup_replayed':False})
    print(json.dumps({'code_slices':len(codes),'data_slices':len(data),'filename_literals':len(formats),'instruction_rows':sum(len(x['instructions']) for x in codes),'code_bytes':sum(x['origin']['bytes'] for x in codes),'data_bytes':sum(x['origin']['bytes'] for x in data)+sum(x['origin']['bytes'] for x in formats),'bank_raw_values':next(x for x in data if x['name']=='banks_disk')['u32_values'],'79_suffix_matches':hits},ensure_ascii=False))
if __name__=='__main__':main()
