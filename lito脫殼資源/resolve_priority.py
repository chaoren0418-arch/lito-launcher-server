"""Resolve priority imported API evidence without launching a game or loading its DLLs."""
from audit import *
import socket
def main():
    pe=pefile.PE(str(PE)); cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    hist=LEGACY/'runtime'/'launch-reports'/'LiTo-20260805-214334.json'
    report=json.loads(hist.read_text(encoding='utf-8'))
    modules=[m for m in report['modules'] if m['name'].lower() in ('ws2_32.dll','msvcrt.dll')]
    resolutions=[]
    for va,insva,target,dll,name in [(0x4EFE56,0x4EFE56,0x758F57E0,'ws2_32.dll','inet_addr'),(0x4EFE4A,0x4EFE4B,0x758F5940,'ws2_32.dll','inet_ntoa'),(0x4EFE5C,0x4EFE5D,0x7591FF10,'ws2_32.dll','gethostbyname'),(0x4AC59A,0x4AC59B,0x773F5200,'msvcrt.dll','sprintf')]:
        module=next(m for m in modules if m['name'].lower()==dll)
        path=Path('C:/Windows/SysWOW64')/dll; lib=pefile.PE(str(path))
        export=next(e for e in lib.DIRECTORY_ENTRY_EXPORT.symbols if e.name==name.encode())
        instruction=next(cs.disasm(pe.get_data(insva-0x400000,5),insva))
        assert int(instruction.op_str,16)==target
        base=int(module['base'],16); assert base+export.address==target
        raw=lib.get_data(export.address,128); out=ROOT/'external-api'/f'{dll}-{name}.bin'; out.parent.mkdir(exist_ok=True); out.write_bytes(raw)
        resolutions.append({'main_image_thunk':mapping(pe,va),'branch_instruction':{'va':f'{insva:08X}','bytes':instruction.bytes.hex(),'target_va':f'{target:08X}'},'historical_module':module,'current_dll':record(path),'current_export':{'name':name,'ordinal':export.ordinal,'rva':f'{export.address:08X}','file_offset':f'{lib.get_offset_from_rva(export.address):08X}','forwarder':export.forwarder.decode() if export.forwarder else None},'export_bytes_128':record(out),'historical_target_equals_historical_base_plus_current_export_rva':True,'confidence':'high-confidence static identity; current DLL bytes are NOT proven historical dump-time DLL bytes','historical_dll_sha256':None})
    write('priority-api-resolution.json',{'updated_at':stamp(),'main_PE':record(PE),'module_evidence':record(hist),'resolutions':resolutions,'library_not_loaded_or_executed':True})
    ranges=[(0x4AC570,0x66,'F4 complete bounded handler through both return tails'),(0x4B3057,0x14,'F4 dispatch case; switch table lineage from ASM'),(0x46EB30,0x220,'same-address transition bounded body'),(0x469BD0,0xBD,'timer callback through ret'),(0x46F010,0x227,'return_mode 1 and 2 through tail ret'),(0x475F40,0x260,'hostname conversion caller; bounded context'),(0x4A2A50,0x360,'packet format parser bounded context'),(0x64AD74,0x100,'hostname buffer'),(0x64A9C4,0x100,'active/retry host buffer')]
    rows=[]
    for va,n,meaning in ranges:
        raw=pe.get_data(va-0x400000,n); p=ROOT/'code'/f'priority-{va:08X}.bin'; p.write_bytes(raw)
        ins=[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(raw,va)] if va<0x500000 else []
        rows.append({**mapping(pe,va),'purpose':meaning,'size':n,'source':record(PE),'output':record(p),'instructions':ins})
    write('priority-complete-tails.json',{'ranges':rows,'limits':'Explicit bounded ranges include IDA-unlabelled tail. Transitive CFG and runtime execution remain unproved.'})
    refs=json.loads((ROOT/'priority-asm-references.json').read_text(encoding='utf-8'))
    direct=[]; raw=PE.read_bytes(); needle=struct.pack('<I',0x64AD74)
    pos=raw.find(needle)
    while pos!=-1:
        direct.append({'file_offset':f'{pos:08X}','mapped_va':f'{0x400000+pos:08X}','preceding_bytes':raw[max(0,pos-8):pos].hex(),'following_bytes':raw[pos+4:pos+12].hex(),'classification':'raw literal candidate, not automatic code/xref proof'})
        pos=raw.find(needle,pos+1)
    write('reconnect-host-audit.json',{'source':record(PE),'buffer':mapping(pe,0x64AD74),'raw_buffer_bytes':256,'raw_buffer_all_zero':pe.get_data(0x24AD74,256)==bytes(256),'ASM_exact_name_references':refs['references']['64AD74'],'PE_literal_candidates':direct,'meaning':'Hostname/address string: 475F40 passes it to inet_addr then gethostbyname on INADDR_NONE. It is not source-qualified as a selected character name.','known_reads':['469B4C scans source then copies to64AE74','46F134 scans source then copies to64AE74 (see actual instruction addresses)','4760FF inet_addr;476116 gethostbyname fallback (actual addresses in priority-complete-tails)'],'explicit_writer_proven':False,'zero_scope':'Disk snapshot bytes only; not proof of original startup initial value, protected initialization, or live session value.','gaps':['Indirect/alias/heap/VM initialization not closed','Original dump-time module bytes unavailable','Need separate provenance of selected-character-name buffer']})
    # Canonical network bytes packed into a little-endian x86 EAX value.
    vectors=[{'ipv4':ip,'network_bytes':socket.inet_aton(ip).hex(),'x86_u32':f'{int.from_bytes(socket.inet_aton(ip),"little"):08X}'} for ip in ['127.0.0.1','127.0.0.2','1.2.3.4']]
    write('modern-F4-contract.json',{'updated_at':stamp(),'classification':'static source-qualified contract with high-confidence external API identities; no wire/runtime validation','opcode':244,'dispatch_va':'004B3057','handler_va':'004AC570','format_bytes_at_00502F2C':pe.get_data(0x102F2C,4).hex(),'format':'cdh','fields_after_opcode':[{'type':'c','role':'signed channel index; sign extended at4AC5AC','native_local':'ebp-1'},{'type':'d','role':'IPv4 integer compared to local inet_addr result','native_local':'ebp-8'},{'type':'h','role':'port low16 stored by46ED50','native_local':'ebp-0C'}],'sequence':['4AC589 parses opcode+1 using inline format cdh','4AC59B sprintf(local28, global62C6A4); fixed PE string127.0.0.1','4AC5A7 calls4EFE56 -> WS2_32 inet_addr','4AC5B0 checks64AD2C[signed_index]==FFFFFFFF: returns without transition','4AC5BD compares parsed d to inet_addr result','equal:4AC5C1 ->46EB30','different:4AC5CA loadsEDX=localport,ECX stillpacketIPv4 ->46ED50','46ED50 usesinet_ntoa then copies address into64AE74 and64A9C4; low16port ->64AD70/64A9C0; arms callback469B10','return_mode2:46F010 copies64AD74 host to64AE74, port2000 and later arms469BD0'],'known_constant':'62C6A4=127.0.0.1 in this fixed PE; not a universal server address','canonical_vectors':vectors,'API_documentation':'https://learn.microsoft.com/en-us/windows/win32/api/winsock/nf-winsock-inet_addr','limits':['No proof T73 receivedF4 or that F4 fixes GUI failures','Signed index bounds depend on native packet trust; do not treat malformed packets as valid input','Hostname initializer64AD74 remains unknown','Export identities match historical module bases plus current RVAs; dump-time DLL hashes absent']})
    manifest={'schema':1,'updated_at':stamp(),'phase':'BATCH1_PRIORITY_API_AND_PE_RESOURCES_READY','entrypoint':str(ROOT/'閱讀入口.md'),'modern':{'contract':record(ROOT/'modern-F4-contract.json'),'api_resolution':record(ROOT/'priority-api-resolution.json'),'reconnect_host':record(ROOT/'reconnect-host-audit.json')},'launcher':{'PE_audit':record(ROOT/'pe-audit.json'),'baseline':record(ROOT/'baseline.json'),'assessment':'GUI threeFAIL root cause unproved; embedded dump resources differ from packed disk resources'},'overall_complete':False,'next_steps':['Validated archive index/extraction batch','Global writes/call-chain/initialization audit and resource mapping']}
    write('handoff.json',manifest)
    progress=json.loads((ROOT/'監製進度.json').read_text(encoding='utf-8')); progress.update(updated_at=stamp(),phase=manifest['phase'],current_task='Extract native LT text/icon/UI archive subset with full provenance; retain unresolved host initializer.',actual_artifacts=progress['actual_artifacts']+[record(ROOT/n) for n in ['priority-api-resolution.json','priority-complete-tails.json','reconnect-host-audit.json','modern-F4-contract.json','handoff.json']]); write('監製進度.json',progress)
    print(json.dumps({'resolved_API_names':[r['current_export']['name'] for r in resolutions],'F4':'cdh','host64AD74':'zero snapshot, hostname reads; writer unknown','literal_candidates':len(direct)}))
if __name__=='__main__':main()
