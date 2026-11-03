"""Read-only LiTo PE/ASM audit. All outputs remain beneath this script directory."""
from pathlib import Path
import hashlib, json, datetime, os, re, struct
import pefile, capstone
ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent.parent
LEGACY = ROOT.parent / 'lito登入器' / '舊版分析與驗收'
BIN = PROJECT / '0-客戶端' / 'LiTo.bin'
PE = LEGACY / 'runtime' / 'LiTo.unpacked-oep-current.exe'
ASM = LEGACY / 'ida-client-oep-v2' / 'full-disassembly.asm'
def stamp(): return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
def sha(data): return hashlib.sha256(data).hexdigest().upper()
def record(p):
    p=Path(p); h=hashlib.sha256()
    with p.open('rb') as f:
        for block in iter(lambda:f.read(1024*1024),b''): h.update(block)
    return {'path':str(p),'bytes':p.stat().st_size,'sha256':h.hexdigest().upper()}
def write(name,obj):
    p=ROOT/name; p.parent.mkdir(parents=True,exist_ok=True)
    temp=p.with_name(p.name+'.tmp')
    temp.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');os.replace(temp,p)
def mapping(pe,va):
    rva=va-pe.OPTIONAL_HEADER.ImageBase
    row={'va':f'{va:08X}','rva':f'{rva:08X}'}
    for s in pe.sections:
        if s.VirtualAddress<=rva<s.VirtualAddress+max(s.Misc_VirtualSize,s.SizeOfRawData):
            delta=rva-s.VirtualAddress; off=s.PointerToRawData+delta
            row.update(section=s.Name.rstrip(b'\0').decode('ascii','replace'),file_offset=f'{off:08X}' if delta<s.SizeOfRawData and off<len(pe.__data__) else None,raw_backed=delta<s.SizeOfRawData and off<len(pe.__data__))
            return row
    row.update(section=None,file_offset=None,raw_backed=False); return row
def inspect(p):
    data=p.read_bytes(); pe=pefile.PE(data=data)
    sections=[]
    for s in pe.sections:
        raw=data[s.PointerToRawData:s.PointerToRawData+s.SizeOfRawData]
        sections.append({'name':s.Name.rstrip(b'\0').decode('ascii','replace'),'rva':f'{s.VirtualAddress:08X}','virtual_size':s.Misc_VirtualSize,'raw_offset':s.PointerToRawData,'raw_size':s.SizeOfRawData,'raw_available':len(raw),'sha256':sha(raw),'entropy':s.get_entropy(),'characteristics':f'{s.Characteristics:08X}'})
    imports=[]
    for d in getattr(pe,'DIRECTORY_ENTRY_IMPORT',[]):
        imports.append({'dll':d.dll.decode('ascii','replace'),'functions':[{'name':i.name.decode('ascii','replace') if i.name else None,'ordinal':i.ordinal,'iat_va':f'{i.address:08X}'} for i in d.imports]})
    resources=[]
    def descend(node,parts):
        for e in node.entries:
            key=str(e.name) if e.name else str(e.id)
            if hasattr(e,'directory'): descend(e.directory,parts+[key])
            elif hasattr(e,'data'):
                d=e.data.struct; m=mapping(pe,pe.OPTIONAL_HEADER.ImageBase+d.OffsetToData)
                raw=pe.get_data(d.OffsetToData,d.Size)
                name='embedded/'+p.name+'/'+('_'.join(parts+[key]))+'.bin'
                (ROOT/name).parent.mkdir(parents=True,exist_ok=True); (ROOT/name).write_bytes(raw)
                resources.append({'resource_path':parts+[key],'size_declared':d.Size,'code_page':d.CodePage,**m,'output':record(ROOT/name),'exact_size':len(raw)==d.Size})
    if hasattr(pe,'DIRECTORY_ENTRY_RESOURCE'): descend(pe.DIRECTORY_ENTRY_RESOURCE,[])
    end=max([pe.OPTIONAL_HEADER.SizeOfHeaders]+[s.PointerToRawData+s.SizeOfRawData for s in pe.sections])
    return pe,{'source':record(p),'image_base':f'{pe.OPTIONAL_HEADER.ImageBase:08X}','entrypoint':mapping(pe,pe.OPTIONAL_HEADER.ImageBase+pe.OPTIONAL_HEADER.AddressOfEntryPoint),'size_of_image':pe.OPTIONAL_HEADER.SizeOfImage,'machine':f'{pe.FILE_HEADER.Machine:04X}','sections':sections,'directories':[{'name':d.name,'rva':f'{d.VirtualAddress:08X}','size':d.Size} for d in pe.OPTIONAL_HEADER.DATA_DIRECTORY],'imports':imports,'relocation_blocks':len(getattr(pe,'DIRECTORY_ENTRY_BASERELOC',[])),'resources':resources,'overlay':{'offset':end,'bytes':max(0,len(data)-end)},'parser_warnings':pe.get_warnings()}
def main():
    expected=[(BIN,1941568,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'),(PE,7094272,'4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'),(ASM,41179755,'2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A')]
    baseline=[dict(record(p),expected_bytes=n,expected_sha256=h,verified=record(p)['sha256']==h and p.stat().st_size==n) for p,n,h in expected]
    if not all(x['verified'] for x in baseline): raise RuntimeError('Baseline drift; no analysis permitted')
    write('baseline.json',{'updated_at':stamp(),'sources':baseline,'rules_checked':[str(PROJECT.parent/'AGENTS.md'),str(PROJECT/'AGENTS.md'),str(ROOT.parent/'AGENTS.md'),str(ROOT/'AGENTS.md')],'applicable_AGENTS_found':[]})
    audits=[]
    for p in [BIN,PE]:
        pe,a=inspect(p); audits.append(a)
    write('pe-audit.json',{'updated_at':stamp(),'images':audits,'limitations':'Parsed PE is not proof of complete unpacking, loader-valid IAT, executed initialization, or original behavior.'})
    pe=pefile.PE(str(PE)); cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    targets=[(0x4AC570,0x70),(0x4EFE56,7),(0x469BD0,0x80),(0x46ED50,0x2C0),(0x46EB30,0x90),(0x46F010,0x180),(0x469B10,0x130),(0x46DE60,0x190),(0x64AD74,0x100),(0x64AE74,0x100),(0x64AFB8,0x20),(0x64AFBC,4),(0x62C6A4,0x20),(0x502F2C,0x10),(0x834888,4),(0x834884,4)]
    ranges=[]
    for va,n in targets:
        m=mapping(pe,va); raw=pe.get_data(va-pe.OPTIONAL_HEADER.ImageBase,n) if m['raw_backed'] else b''
        name=f'code/VA-{va:08X}.bin'; (ROOT/name).parent.mkdir(exist_ok=True); (ROOT/name).write_bytes(raw)
        ins=[]
        if va<0x500000:
            for i in cs.disasm(raw,va):
                row={'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
                if i.mnemonic in ('call','jmp') and i.op_str.startswith('0x'):
                    dest=int(i.op_str,16); row['destination']=mapping(pe,dest); row['external_to_main_image']=not(pe.OPTIONAL_HEADER.ImageBase<=dest<pe.OPTIONAL_HEADER.ImageBase+pe.OPTIONAL_HEADER.SizeOfImage)
                ins.append(row)
        ranges.append({**m,'requested_size':n,'raw_hex':raw.hex(),'output':record(ROOT/name),'instructions':ins,'scope':'bounded bytes; not complete CFG or execution proof'})
    write('priority-ranges.json',{'source':record(PE),'ranges':ranges})
    lines=ASM.read_text(encoding='utf-8',errors='replace').splitlines(); header='\n'.join(lines[:25]); refs={}
    for name in ['64AD74','64AE74','64AD2C','62C6A4','64AFB8','64AFBC','469BD0','46ED50','46EB30','4AC570','4EFE56','834888','834884']:
        hits=[]
        for index,line in enumerate(lines):
            if name.lower() in line.lower():
                hits.append({'line':index+1,'text':line,'context':lines[max(0,index-5):index+9]})
        refs[name]=hits
    write('priority-asm-references.json',{'source':record(ASM),'ida_header':header,'references':refs,'warning':'IDA input fingerprint in header differs from current PE. Each code claim must compare raw bytes; IDA function endp can precede reachable tail.'})
    candidates=[]
    for base in [LEGACY/'runtime',LEGACY/'tools',PROJECT/'0-客戶端'/'工具包']:
        for dp,dirs,files in os.walk(base,followlinks=False):
            dirs[:]=[d for d in dirs if not (Path(dp)/d).is_junction() and d not in ['client-isolated','client-clean','node_modules','.git','__pycache__']]
            for name in files:
                if re.search(r'unpack|dump|oep|scylla|iat|pefix|\.dmp$',name,re.I):
                    p=Path(dp)/name; row={'path':str(p),'bytes':p.stat().st_size}
                    if p.stat().st_size<=32*1024*1024: row=record(p)
                    candidates.append(row)
    write('existing-tool-dump-inventory.json',{'items':candidates,'scope':'runtime/tools/client-toolbox; junctions and live client folders excluded; no processes opened'})
    progress={'updated_at':stamp(),'phase':'STATIC_BASELINE_AND_PRIORITY_BYTES_VERIFIED','current_task':'Resolve external target identity and reconnect-name indirect writers; extract original external archives.','actual_artifacts':[record(ROOT/n) for n in ['baseline.json','pe-audit.json','priority-ranges.json','priority-asm-references.json','existing-tool-dump-inventory.json']],'verification':{'three_baselines_match':True,'native_started':False,'process_opened':False,'GUI_accepted':False},'gaps':['External DLL identity at dump time not yet proved','64AD74 indirect writes not yet audited','Runtime initialization differs from disk snapshot','Actual T73 first lobby skills were not sampled'],'handoffs':{'modern':'priority-ranges.json + priority-asm-references.json','launcher':'pe-audit.json; GUI failures unresolved'},'next_steps':['Resolve priority thunk and indirect writer','Extract resource indices with bounded validated codec','Publish handoff.json and reader']}
    write('監製進度.json',progress)
    print(json.dumps({'baselines':baseline,'pe_summary':[{'path':a['source']['path'],'sections':len(a['sections']),'imports':len(a['imports']),'resources':len(a['resources']),'entrypoint':a['entrypoint']} for a in audits],'inventory_count':len(candidates)},ensure_ascii=False))
if __name__=='__main__': main()
