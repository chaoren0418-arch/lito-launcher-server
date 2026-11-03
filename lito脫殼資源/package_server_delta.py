"""Publish a documentation-only server handoff; no Java/SQL/game/test actions."""
from audit import *
import shutil

def load(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
    existing=load('server-packet-delta.json') if (ROOT/'server-packet-delta.json').is_file() else None
    previous_manifest=record(ROOT/'artifact-manifest.json')
    previous_verification=load('delivery-verification.json')
    assert previous_verification['all_match']
    previous_verification_pin=record(ROOT/'delivery-verification.json')
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    folder=ROOT/'server-packet-evidence';folder.mkdir(exist_ok=True)
    ranges=[]
    for va,n,label in [(0x4AC520,0x48,'F3 count and DWORD prefix writer throughret'),(0x4B304B,0x18,'F3/F4 receive dispatch cases'),(0x4A2A8A,0x5D,'c/h/d parser byte assembly'),(0x5025A8,2,'F3 inline c format'),(0x63424C,2,'F3 per-entry inline d format')]:
        raw=pe.get_data(va-0x400000,n);p=folder/f'VA-{va:08X}.bin';p.write_bytes(raw)
        ranges.append({**mapping(pe,va),'purpose':label,'size':len(raw),'output':record(p),'instructions':[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]if va<0x500000 else []})
    assert pe.get_data(0x5025A8-0x400000,2)==b'c\0' and pe.get_data(0x63424C-0x400000,2)==b'd\0'
    write('server-packet-evidence/ranges.json',{'source_PE':load('baseline.json')['sources'][1],'ranges':ranges,'method':'New bounded static source read for handoff documentation; no execution or test replay. Inline c/d raw bytes checked.'})
    modern_source=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'/'native_entry_transfer.rs'
    p=folder/'native_entry_transfer-adoption-snapshot.rs';shutil.copyfile(modern_source,p)
    adoption={'source_at_read':record(modern_source),'snapshot':record(p),'observation':'Source already decodes S_F3(243), S_F4(244), F4 cdh with h16 and LE integer fields; added bounded count/index guards. R506 header records API identity adoption. Source presence only, no rerun or runtime acceptance claimed.'}
    baseline=load('baseline.json')['sources'];f4=load('modern-F4-contract.json')
    common={'direction':'S','direction_meaning':'server -> client; receive dispatcher plus existing Modern decoder; no new runtime capture','classification':'KNOWN_CONTRACT_CORROBORATION','new_opcode':False,'wire_contract_corrected_this_batch':False,'string_encoding':{'applies':False,'reason':'These numeric packet bodies contain no string field; formatter letters c/d/h are ASCII analysis metadata, not wire strings.'},'source_BIN':baseline[0],'source_PE':baseline[1],'source_ASM':baseline[2],'java_comparison':{'status':'NOT_COMPARED','specific_differences':None,'meaning':'未比較既有 Java 協定，不能宣告 Java 已一致或需要變更。','SQL_change_established':False},'dynamic_evidence':{'new_capture':False,'T73_F3_F4_receipt_proven':False,'scope':'Existing pure API vectors do not prove wire delivery or GUI repair.'}}
    entries=[{**common,'opcode_hex':'0xF3','opcode_decimal':243,'dispatch':mapping(pe,0x4B304B),'handler':mapping(pe,0x4AC520),'body_length_including_opcode':'2 + 4*max(signed_count,0); no framing/encryption header included; original handler exact-length/trailing validation is not proved','fields':[{'order':0,'body_offset':0,'name':'opcode','bytes':1,'signed':False,'endianness':'N/A'},{'order':1,'body_offset':1,'name':'count','bytes':1,'signed':True,'native_type':'int8, JLE skips loop for <=0; MOVSX for loop bound','endianness':'N/A'},{'order':2,'body_offset':2,'name':'channel_gate_values','bytes_each':4,'repeat':'max(signed_count,0)','signed':'opaque32 bits; stored DWORD; FFFFFFFF sentinel used by F4','endianness':'little','native_destination':'0064AD2C + 4*i'}],'trigger_and_transition':['Receive0xF3 ->4B304B ->4AC520; parse c after opcode','For signed count>0, parse d repeatedly into channel table prefix; suffix remains unchanged','For signed count<=0, no table entries are consumed/written; no direct scene transition in bounded handler'],'static_evidence':[record(ROOT/'server-packet-evidence/ranges.json'),record(ROOT/'host-alias-lifecycle.json')],'unknowns':['Native local count bound not observed; table allocation is ten DWORDs ending64AD54. Existing Modern bounds count<=10; valid server range0..10 is a compatibility constraint, not an original clamp.','Original receive framing/truncation/trailing-byte validation is not closed by this handler audit','Java sender/serializer and actual session packet ownership not compared']},
      {**common,'opcode_hex':'0xF4','opcode_decimal':244,'dispatch':mapping(pe,0x4B3057),'handler':mapping(pe,0x4AC570),'format':'cdh','body_length_including_opcode':8,'length_scope':'1 opcode +1 c+4 d+2 h; original surrounding exact-length/trailing validation is not established','fields':[{'order':0,'body_offset':0,'name':'opcode','bytes':1,'signed':False,'endianness':'N/A'},{'order':1,'body_offset':1,'name':'channel_index','bytes':1,'signed':True,'endianness':'N/A','native_use':'sign extended for64AD2C[index]'},{'order':2,'body_offset':2,'name':'IPv4_address_word','bytes':4,'signed':'32-bit address bits; comparison not arithmetic','endianness':'little integer assembly; its four wire bytes are IPv4 network octets','example':{'wire_bytes':'7F000001','native_x86_u32_hex':'0100007F','IPv4':'127.0.0.1'}},{'order':3,'body_offset':6,'name':'port_word','bytes':2,'signed':'unsigned16 wire bits; native uses/stores low16, some consumers read signed WORD','endianness':'little','example':{'wire_bytes':'D007','unsigned_value':2000}}],'trigger_and_transition':f4['sequence'],'static_evidence':[record(ROOT/'modern-F4-contract.json'),record(ROOT/'priority-complete-tails.json'),record(ROOT/'priority-api-resolution.json'),record(ROOT/'server-packet-evidence/ranges.json')],'unknowns':f4['limits']+['Original handler has no local signed-index bounds check; server must remain within ten-cell valid indices0..9','Dump-time DLL hash/private branch execution and native same-epoch transition outcomes are not established','Java sender port width/endian/IP conversion unexamined'],'analysis_corrections_not_wire_changes':['502F2C contains inline ASCII cdh+NUL; an IDA dd rendering is not an indirect format pointer','inet_addr/inet_ntoa/sprintf identity evidence resolves interpretation and equality branch; it does not create a new opcode or change field widths']}]
    status={'server_chat_id':'01a1020a-733e-74e2-9fd1-923830ac5278','handoff_status':'AVAILABLE_TO_SUPERVISOR','notification_status':'PENDING_SUPERVISOR_FORWARD','sync_status':'PENDING_JAVA_COMPARISON','adoption_status':'UNKNOWN_NOT_CONFIRMED','delivery_method':'Supervisor reads this directory and notifies server chat; this script sends no messages.','Java_SQL_modified_by_unpacker':False,'restricted_server_work_taken_over_or_bypassed':False}
    doc={'schema':1,'batch':'BATCH2_SERVER_PACKET_HANDOFF','updated_at':stamp(),'generator':record(Path(__file__)),'summary':{'new_opcodes':0,'corrected_wire_contracts':0,'known_contracts_corroborated':2,'no_established_new_packet_delta':True,'meaning':'本批新增 opcode 0，已證實線上格式更正 0；F3/F4/CDH 為既知契約補證。尚未比較 Java，待伺服器組核對後決定同步候選。'},'consumer_status':status,'existing_Modern_contract':adoption,'packets':entries,'non_wire_evidence':['API target identities, rand port expression, saved-host writer and selected-alias lifetime are implementation evidence, not new packet opcodes.','C9 saved-alias replay remains referenced in host-alias-lifecycle.json; this batch does not add or correct its wire schema.'],'future_batch_rule':'Every new/corrected packet must publish direction, hex/decimal opcode, ordered widths/signedness/endian/encoding, trigger/state transitions, VA/RVA and source hashes, static/dynamic evidence, unknowns, concrete Java difference or NOT_COMPARED, and notification/adoption status.'}
    if existing and (ROOT/'server-adoption-ack.json').is_file():
        status=existing['consumer_status'];doc['consumer_status']=status;doc['summary']=existing['summary'];doc['adoption_ack']=record(ROOT/'server-adoption-ack.json')
        prior={p['opcode_decimal']:p for p in existing['packets']}
        for p in doc['packets']:
            if p['opcode_decimal'] in prior:
                p['java_comparison']=prior[p['opcode_decimal']]['java_comparison']
                if 'consumer_adoption_status' in prior[p['opcode_decimal']]:p['consumer_adoption_status']=prior[p['opcode_decimal']]['consumer_adoption_status']
        doc['packets'] += [p for p in existing['packets'] if p['opcode_decimal'] not in [243,244]]
    write('server-packet-delta.json',doc)
    reader=ROOT/'閱讀入口.md';s=reader.read_text(encoding='utf-8');marker='\n伺服器交接：'
    if marker in s:s=s.split(marker)[0]
    note=doc['summary']['meaning'] if (ROOT/'server-adoption-ack.json').is_file() else 'Java未比較，待總監製轉交；消費者採用未確認。'
    reader.write_text(s+marker+'server-packet-delta.json；'+note+' 每批維持方向/欄位/狀態/來源/消費者採用記錄。\n',encoding='utf-8')
    handoff=load('handoff.json');phase=handoff['phase'] if (ROOT/'server-adoption-ack.json').is_file() else 'BATCH2_SERVER_HANDOFF_READY';handoff.update(updated_at=stamp(),phase=phase,entrypoint=record(reader),server={'packet_delta':record(ROOT/'server-packet-delta.json'),'consumer_status':status});write('handoff.json',handoff)
    # Update only new/modified documentary files; preserve already verified artifact pins.
    manifest=load('artifact-manifest.json');changed=['package_server_delta.py','server-packet-delta.json','閱讀入口.md','handoff.json','finish_batch.py']
    changed +=[str(p.relative_to(ROOT)) for p in folder.iterdir() if p.is_file()]
    updated={str((ROOT/n).resolve()).casefold():record(ROOT/n) for n in changed}
    rows={str(Path(x['path']).resolve()).casefold():x for x in manifest['entries']};rows.update(updated)
    manifest.update(updated_at=stamp(),generation=record(ROOT/'finish_batch.py'),entries=sorted(rows.values(),key=lambda x:x['path']),documentation_delta={'generator':record(Path(__file__)),'previous_manifest_sha256':previous_manifest['sha256'],'previous_delivery_verification_sha256':previous_verification_pin['sha256'],'checks':'Changed/new documentary files only; no previously passed tests replayed.'});write('artifact-manifest.json',manifest)
    # Verify only changes. Previous full batch verification remains inherited evidence.
    checks=[dict(pin,match=record(Path(pin['path']))==pin)for pin in updated.values()]
    assert all(x['match'] for x in checks)
    inherited=previous_verification.get('prior_full_verification',{'manifest_sha256':previous_manifest['sha256'],'verification_sha256':previous_verification_pin['sha256'],'sealed_files':previous_verification['sealed_files'],'all_match':previous_verification['all_match']})
    validation={'updated_at':stamp(),'manifest':record(ROOT/'artifact-manifest.json'),'sealed_files':len(manifest['entries']),'all_match':True,'scope':'Previous full verification plus verified documentation delta; no resource/API/runtime tests replayed.','prior_full_verification':inherited,'prior_delivery_verification_sha256':previous_verification_pin['sha256'],'changed_files_checked':checks,'original_game_launched':False,'GUI_accepted':False}
    write('delivery-verification.json',validation)
    progress=load('監製進度.json');task=progress['current_task'] if (ROOT/'server-adoption-ack.json').is_file() else 'Server packet handoff prepared; waiting for supervisor forwarding and server comparison/adoption evidence.';progress.update(updated_at=stamp(),phase=handoff['phase'],current_task=task,server=status,packet_delta_summary=doc['summary'])
    paths={x['path'] for x in progress['actual_artifacts']}|{str(ROOT/'server-packet-delta.json'),str(ROOT/'server-packet-evidence/ranges.json')};progress['actual_artifacts']=[record(Path(p))for p in sorted(paths)]
    progress['verification']['delivery_all_hashes_match']=True;progress['verification']['no_previous_tests_replayed_for_handoff']=True
    progress['handoffs']['server']='F3/F4 adopted; additional known-contract state inserver-packet-delta.json' if (ROOT/'server-adoption-ack.json').is_file() else 'server-packet-delta.json; available to supervisor, forwarding pending, Java comparison/adoption unconfirmed'
    if not (ROOT/'server-adoption-ack.json').is_file():progress['next_steps']=list(dict.fromkeys(progress['next_steps']+['Supervisor forwards server-packet-delta to server chat; server compares Java and reports adopted/pending/rejected with evidence.']))
    write('監製進度.json',progress)
    print(json.dumps({'new_opcodes':0,'corrected_wire_contracts':0,'known_contracts':2,'changed_files_verified':len(checks),'server_status':status}))

if __name__=='__main__':main()
