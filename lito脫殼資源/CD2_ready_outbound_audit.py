"""Batch29 finite C_D2 raw-byte evidence. Writes only beside this script."""
import sys
sys.dont_write_bytecode=True
from audit import *
OUT=ROOT/'CD2-ready-outbound'
SERVER=ROOT.parent/'lito伺服器'
def load(p):return json.loads(Path(p).read_text('utf-8'))
def main():
    prev=load(ROOT/'CD1-mode-step-contract.json');prevpin=record(ROOT/'CD1-mode-step-contract.json')
    assert prevpin['sha256']=='1BB37D61C8E25EC195EA2084941EFDC89D2E6F2760119583357A088A04C3EC8F'
    sources=[record(p)for p in (PE,BIN,ASM)];assert sources==prev['sources']
    prior=prev['prior_primary_pins']+[{**prevpin,'expected_sha256':prevpin['sha256'],'match':True}]
    assert len(prior)==30
    for q in prior:assert record(q['path'])['sha256']==q['expected_sha256'] and record(q['path'])['bytes']==q['bytes']
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    ins={};ranges=[]
    targets=[(0x46ACB0,11,'entry_first_unknown_4EBB30'),(0x46ACD3,41,'conditional_state0_version_phase'),
      (0x46ACFC,7,'post_state0_unknown_4EBEC0'),(0x46B510,21,'direct_state0_handler'),
      (0x46B6F0,59,'state1_predicate_first_unknown_458BB0'),(0x46B72B,30,'conditional_registration_virtual64_STOP'),
      (0x46B74A,21,'direct_state1_island'),(0x46B760,79,'conditional_input_state1_callback_tail_STOP'),
      (0x407DD5,45,'command_state2_island_A'),(0x407E39,45,'command_state2_island_B'),
      (0x476A90,44,'state0_version_phase_island_raw_entry'),(0x476ABC,8,'post_state0_unknown_4EBEB0'),
      (0x46CDCB,15,'first_allocation_STOP_4F118C'),(0x46CE21,7,'second_allocation_STOP_4F118C'),
      (0x46CDEB,33,'conditional_callback46B6F0_control2_ctor_args'),(0x46CE39,33,'conditional_callback46B510_control0_ctor_args'),
      (0x502778,3,'format_cc_NUL')]
    for va,n,label in targets:
        raw=pe.get_data(va-0x400000,n);path=OUT/f'{va:08X}-{n:X}.bin';path.write_bytes(raw)
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]if va<0x500000 else []
        assert not rows or sum(len(bytes.fromhex(i['raw_hex']))for i in rows)==n
        ins.update({int(i['va'],16):i for i in rows})
        ranges.append({**mapping(pe,va),'bytes':n,'kind':'code'if rows else'data','label':label,'raw_hex':raw.hex(),'output':record(path),'instructions':rows})
    assert pe.get_data(0x502778-0x400000,3)==b'cc\0'
    reuse=next(q for q in load(ROOT/'CD1-mode-step/ranges.json')['new_ranges']if q['va']=='004154E0')
    assert record(reuse['output']['path'])==reuse['output']
    assert Path(reuse['output']['path']).read_bytes()==pe.get_data(0x4154E0-0x400000,35)
    ins.update({int(i['va'],16):i for i in reuse['instructions']})
    assertions=[]
    def expect(va,mn,op):
        q=ins[va];assert(q['mnemonic'],q['operands'])==(mn,op),(hex(va),q)
        assertions.append({**mapping(pe,va),**q,'expected_mnemonic':mn,'expected_operands':op,'match':True})
    calls=[(0x46ACE8,0x46ACF4,0,'signed versionDWORD[63D54C]>=52E0 and phaseDWORD[64AF98]==2; entry bridge after4EBB30 unknown'),
      (0x46B510,0x46B51C,0,'no local version/phase/mode/owner gate in21byte handler; caller eligibility unknown'),
      (0x46B74A,0x46B756,1,'BYTE[64AF9D]==0 OR XOR-decoded selector==0; selector ownership/mode provenance unknown'),
      (0x46B796,0x46B7A2,1,'ECX==0 and input BYTE y/Y, or selectorBYTE[64B6DC]==4 and WORD9982/7882; EDX input lifetime/codec unknown'),
      (0x407DEE,0x407DFA,2,'EAX==0 and signed version>=52E0 and phase==2; upstream command input unknown'),
      (0x407E52,0x407E5E,2,'EAX==0 and signed version>=52E0 and phase==2; upstream command input unknown'),
      (0x476AA8,0x476AB4,0,'signed version>=52E0 and phase==2; ECX object/caller unknown')]
    callrows=[]
    for start,call,state,condition in calls:
        expect(start,'push',str(state));expect(start+2,'push','0xd2');expect(start+7,'push','0x502778');expect(call,'call','0x4c6ae0');expect(call+5,'add','esp, 0xc')
        callrows.append({'state':state,'state_push':mapping(pe,start),'call':mapping(pe,call),'format_VA':'00502778','direction':'C','opcode':'D2','field_source':'literal immediate low BYTE','body_hex':f'd2{state:02x}','body_bytes':2,'frame_bytes':4,'prefix_u16LE_hex':'0400','condition':condition,'owner_and_mode':'not qualified by these finite islands','qualification':'fixed PE conditional argument/call proof; no executed UI or delivery'})
    for va,mn,op in [(0x46ACB6,'call','0x4ebb30'),(0x46ACFE,'jmp','0x4ebec0'),(0x46ACD3,'cmp','dword ptr [0x63d54c], 0x52e0'),
      (0x46ACDF,'cmp','dword ptr [0x64af98], 2'),(0x46B6F0,'mov','al, byte ptr [0x64af9d]'),(0x46B6F7,'je','0x46b74a'),
      (0x46B713,'je','0x46b74a'),(0x46B726,'call','0x458bb0'),(0x46B733,'push','0x46b760'),(0x46B746,'call','dword ptr [edx + 0x64]'),
      (0x46B760,'test','ecx, ecx'),(0x46B764,'cmp','byte ptr [0x64b6dc], 4'),(0x46B77A,'cmp','dx, 0x9982'),
      (0x46B781,'cmp','dx, 0x7882'),(0x46B7AA,'jmp','0x458ca0'),(0x407DD5,'test','eax, eax'),(0x407E39,'test','eax, eax'),
      (0x476A96,'cmp','eax, 0x52e0'),(0x476A9F,'cmp','dword ptr [0x64af98], 2'),(0x476ABF,'jmp','0x4ebeb0'),
      (0x46CDD5,'call','0x4f118c'),(0x46CE23,'call','0x4f118c'),(0x46CDF2,'push','2'),(0x46CDF9,'push','0x46b6f0'),
      (0x46CE07,'call','0x4154e0'),(0x46CE40,'push','0'),(0x46CE47,'push','0x46b510'),(0x46CE55,'call','0x4154e0'),
      (0x4154E3,'mov','eax, dword ptr [ebp + 0x18]'),(0x4154EC,'mov','ecx, dword ptr [ebp + 0x14]'),(0x4154FE,'call','0x414910')]:expect(va,mn,op)
    lines=ASM.read_text('utf-8',errors='replace').splitlines()
    hits=[{'line':i+1,'text':s}for i,s in enumerate(lines)if re.search(r'\bpush\s+0D2h\b',s,re.I)]
    assert len(hits)==11
    inventory={'ASM_source':sources[2],'literal_query':'push 0D2h','hits':hits,'actual_packet_calls':callrows,'false_UI_position_hits':['00460600→004DBD70 (one)','0046ADB0→00474A50 (three)'],
      'qualification':'finite literal direct-push navigation only; raw PE authoritative. ASM labels sub476A20 conceal independent actual entry476A90. Not whole-image indirect-caller completeness.',
      'opcode_only_fixed_PE_callsite_found':False,'opcode_only_whole_image_absence_claim':False}
    ready=load(SERVER/'server-room-ready-prestart-consumer-contract.json');net=load(SERVER/'server-room-state2-ready-cancel-network.json');candidate=load(SERVER/'server-ordinary-room-settings-order-candidate.json')
    existing={n:record(ROOT/n)for n in ['CDD-outbound-contract.json','D1-E1-prestart-contract.json','CD1-mode-step-contract.json','CD1-mode-step/scope-write-correction.json']}
    existing.update(ready_report=record(SERVER/'server-room-ready-prestart-consumer-contract.json'),cancel_report=record(SERVER/'server-room-state2-ready-cancel-network.json'),settings_order_report=record(SERVER/'server-ordinary-room-settings-order-candidate.json'))
    expected={'ready_report':'57F324FE093C80A8DAE8B7E207A9A8A628AA508E04EBB38AFE17F3C33D6274F8','cancel_report':'7168EAD2353A7A8D39F5117EB0CB7943A6D05817C4D9AEB29D5B440037B5542E','settings_order_report':'8D969CB59FA4ECD3515EEE794397E4C94D58D97328F8A410A4EA0BBA456CCF9E'}
    for k,h in expected.items():assert existing[k]['sha256']==h
    for k in ['finite-ready-IL.json','finite-source-reference.json','consumer-handoff.json','native-ready-reference.json']:existing[k]=ready['evidence'][k]
    existing.update(AD368_JAR=ready['actual_JAR'],saved_cancel_frames=net['new_network']['frames'],settings_candidate_JAR=candidate['candidate_JAR'],settings_bytecode=candidate['bytecode'],settings_JAR_entries=candidate['JAR_entries'])
    native=load(existing['native-ready-reference.json']['path']);history=native['historical_owner_envelope_reference']
    existing['historical_owner_original']=history['original'];existing['prior_native_control_original']=native['native_existing_control_contract']['original']
    for q in existing.values():assert record(q['path'])==q
    consumer=load(existing['consumer-handoff.json']['path']);il=load(existing['finite-ready-IL.json']['path'])
    selected={'ClientSession':['setReady','serverCountdown'],'Protocol':['litoReadyRequest','readyState'],'GameHub':['setReady','requestOwnerStart','cancelCountdownIfNotReady']}
    java={'IL_pin':existing['finite-ready-IL.json'],'source_reference_pin':existing['finite-source-reference.json'],'actual_JAR':ready['actual_JAR'],
      'adopted_selected_methods':{k:{n:il['members'][k][n]for n in names}for k,names in selected.items()},
      'consumer_reference_pin':existing['consumer-handoff.json'],'semantics':{k:consumer[k]for k in ['request_layout','authority_conditions','ready_state_machine','partial_and_race_boundaries']},
      'finite_publish_order':'GH lock commits ready/match/value/deadline; CS localready; per-peer ownalias D1; then STARTED E103/CANCELLED E100/NONE noE1. Uncaught send may abort after commits.',
      'old_reference_qualification_update':'consumer original cancellation/repeated1/state2 source-only statements are immutable old scope; accepted newer AD368 cancel report qualifies its exact case only.',
      'body_expansion':'No new Java parsing/units/runtime, nativeRosterReady/bot/matching/WORLD bodies not expanded; selected archived excerpts reused as data.'}
    rows=load(existing['saved_cancel_frames']['path']);saved=[]
    assert existing['saved_cancel_frames']['sha256']=='7799F0CFA1687EE425F5B60D50D36A749410C01988F246E8C4C055680A5A1C81'
    for i in range(73,89):
        q=rows[i];raw=bytes.fromhex(q['body_hex']);assert q['frame_bytes']==len(raw)+2
        saved.append({'array_index':i,'direction':q['direction'],'epoch':q['epoch'],'body_hex':q['body_hex'],'body_bytes':len(raw),'frame_bytes':q['frame_bytes']})
    savedobj={'source':existing['saved_cancel_frames'],'report':existing['cancel_report'],'rows':saved,'new_capture_frames':0,'cipher_or_old_PASS_replayed':False,
      'qualification':'AD368 server test-client only:73 D202,74/75 D1state2;76 D201,77/78 D1state1;79 ownerD2,80/82 ownerD1state1,81/83 E103;84 repeatedD201,85/87 D1state0,86/88 E100. Per-peer order; no general global ordering/freshness inference.',
      'bounded_case':'Cancellation before first tick; accepted report4second/15sample watch. No Native GUI/state0 dynamic, general race, rollback or WORLD qualification.'}
    versions={'AD368':{'JAR':ready['actual_JAR'],'level':'accepted finite ready source/IL and exact existing state2/repeatedready1/cancel test-client network','Native_current_UI_dynamic':False},
      'C7':{'JAR_SHA256':'C7B169C00955B444BCDB357E6A63625A87AABC5B23F8F3A21E75988D8270D43A','level':'accepted prior ordinary WAITING map network; Batch28 old updateSettings writes-before-decode statement remains correct for this version','ready_dynamic_transferred':False},
      '485F':{'JAR':candidate['candidate_JAR'],'report':existing['settings_order_report'],'level':'supervisor accepted finite parent12/candidate12 seeded units; network sealing not accepted','only_changed':'GameHub.updateSettings Code (+remapped debug/StackMap); other1013methods/CP/ClientSession/updateMap preserved per accepted proof',
        'order':'decode/reconfigure moved BEFORE settings/selectedmap/ready writes','general_transaction_rollback_or_race_qualification':False,'network_qualification_adopted':False},
      'historical_owner':{'reference':history,'level':'older0.7.664 Native owner-click first D2 request only; no fixedPE/current UI authority transfer'}}
    gaps={'local_intent':'seven fixedPE c,c argument calls provide D200/D201/D202 conditional outbound intent; not execution/send/delivery/ACK',
      'opcode_only':'Java AD368 parser accepts owner form and exact saved test-client row79 is D2. Older Native physical-click reference is version-scoped. No opcode-only callsite qualified in finite currentPE inventory; cannot infer it from parser.',
      'UI':'46CDD5/46CE23 allocation4F118C firstunknown; conditional args then4154FE→414910 firstunknown; no callbackstorage/click/control→ECX qualification. 46ACB6→4EBB30 stops entry; later41byte send island independent.',
      'state1':'64AF9D BYTE and XOR-decoded selector are unnamed runtime predicates; not proof of owner/mode. 46B726→458BB0 unknown; laterregistration46B746 virtual+64 STOP; callback46B760 input y/Y or selector4 WORD9982/7882 is separate conditional island; no codec/inputpointer provenance.',
      'state2':'407DD5/407E39 EAX==0, signedversion>=52E0 and phaseDWORD==2. Upstream commandstring/input producer unknown; do not expand dispatcher other commands.',
      'other_tails':'46ACFE→4EBEC0,46B7AA→458CA0,476ABF→4EBEB0 private STOP; no helper body inspection. ASM476A20 label not actual send entry476A90.',
      'D1':'server ownalias projected ready receipt, distinct from local intent/GH membership/allready/owner authority. Native alias/table/private gates and numeric phases2/3/6 remain as accepted D1-E1 contract.',
      'E1':'server countdown receipt3/2/1 and exact cancelled0; no local expiry→WORLD grant. CSROOM/GHWAITING-or-COUNTDOWN/Nativephase independent. No room/generation fields; bind independently qualified current epoch/room/join and alias.',
      'WORLD':'accepted prestart contract stops at world/preparation entries. No new WORLD/render/combat/bot/matching acceptance.',
      'transport':'accepted Batch26 c lowBYTE,frameu16LE total length and conditional4C6AE0 reused; downstreamexternal759058A0 STOP, no retry/ACK/physical authority qualification.',
      'partial_and_race':'state commits precede sequential sends; D1 failure can prevent later recipients/E1; queued tick vs cancel and inherited no generation publication barrier remain unknown.',
      'cache_cleanup':'Batch28 external pyc remains; exact cleanup auto-review rejected blocked by policy. No retry/reframe/alternative/delegation. This batch uses -B and dont_write_bytecode.'}
    write('CD2-ready-outbound/ranges.json',{'source':sources[0],'new_ranges':ranges,'necessary_reused_raw':[reuse],'reuse_index':record(ROOT/'CD1-mode-step/ranges.json'),'instruction_assertions':assertions})
    for n,obj in [('native-callsite-inventory.json',inventory),('Java-ready-reference.json',java),('saved-ready-cancel-frame-witnesses.json',savedobj),('version-evidence-table.json',versions),('intent-receipt-and-unknown-matrix.json',gaps)]:write('CD2-ready-outbound/'+n,obj)
    wire={'direction':'C','opcode':'D2','actual_fixed_PE_forms':callrows,'serializer_reference':existing['CDD-outbound-contract.json'],
      'field_lengths':['opcode1BYTE','state1BYTE literal0/1/2','body2bytes','frame4bytes incl.u16LE total4 prefix0400'],
      'opcode_only_reference':'body1/frame3 prefix0300 supported Java/historical only; not fixedPE-callsite proof','no_fields':['roomID','epoch','ownerID','name','requestID','source','timestamp'],
      'mode_and_owner':'none qualified by current finite Native predicates; server checks separate','server_ready_reference':existing['ready_report'],'unknowns':gaps}
    write('CD2-ready-outbound/wire-contract.json',wire)
    outputs=[record(p)for p in sorted(OUT.iterdir())if p.is_file()]
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    primary={'schema':'finite-native-CD2-ready-outbound-v1','updated_at':stamp(),'status':'STATIC_SEALED_CURRENT_OPCODE_ONLY_UI_AUTHORITY_DYNAMIC_OPEN','sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'outputs':outputs,
      'callsite_count':7,'callsite_state_counts':{'0':3,'1':2,'2':2},'wire':wire,'boundaries':gaps,
      'preflight':'Existing ready Native ASM reference already covers partial 46ACB0/46B510/46B6F0/state2. Added only necessary raw operands/predicates, second state2/callback/476A90 islands and firstunknown/caller boundary; ctor35bytes reused. No old PASS/generators/reviews/models rerun.',
      'verification':{'new_code_ranges':16,'new_data_ranges':1,'necessary_reused_raw_ranges':1,'instruction_assertions':len(assertions),'adopted_Java_methods':7,'saved_frame_rows':16,'prior_primary_pins_preserved':30,'new_capture_frames':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'server_packet_unchanged':packet,'Native_GUI_SQL_provider_JVM_Java_Modern_BIN_actions':0,'unknown_bodies_expanded':0,'scope_write_correction':existing['CD1-mode-step/scope-write-correction.json'],'cache_cleanup_completed':False}}
    write('CD2-ready-outbound-contract.json',primary)
    print(json.dumps({'primary':record(ROOT/'CD2-ready-outbound-contract.json'),'verification':primary['verification']},ensure_ascii=False))
if __name__=='__main__':main()
