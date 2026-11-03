"""Finite receive/selection boundary evidence from fixed PE. Never execute native code."""
from audit import *

def main():
    pe=pefile.PE(str(PE)); image=PE.read_bytes()
    assert sha(image)=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
    accepted={
        'S1E-initialization-gates.json':'841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC',
        'class4-slot-provenance.json':'845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8',
        'skill-gates/ranges.json':'885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C',
        'sealed-batch3/accepted-20261005/snapshot-manifest.json':'140302D62027784D900DC628B550255F44E50F8D1275AA17B5321272C63C1DF0'}
    for n,h in accepted.items():assert record(ROOT/n)['sha256']==h
    cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    specs=[
        (0x4B37C0,0x103,'receive_frame_pump'),(0x4B38D0,0x1C,'receive_state_init'),
        (0x4C6B30,0x64E,'network_message_dispatcher_code'),(0x4C7180,0x34,'network_message_switch_data'),
        (0x47F340,0xCBA,'window_procedure_code'),(0x480290,0x10E,'window_setup'),
        (0x482098,0xBA,'main_message_loop'),(0x4F7448,0x28,'window_API_pointer_data'),
        (0x4C66C0,0x3E,'receive_decode_leaf'),(0x4C6700,0x37,'send_encode_leaf'),
        (0x4C6AE0,0x50,'packet_frame_sender'),(0x4C6770,0x11A,'socket_send_code'),
        (0x4C688C,0x28,'send_error_switch_data'),(0x4C65F0,0xC9,'close_connection_code'),
        (0x480620,0x14F,'socket_error_handler_code'),(0x4F7314,4,'format_pointer_data'),
        (0x4EFE62,6,'recv_thunk'),(0x4EFE6E,6,'asyncselect_thunk'),
        (0x4EFE7A,6,'get_last_error_thunk'),(0x4EFEA4,5,'send_thunk'),
        (0x4928C0,0x6D2,'ordinary_selection_code'),(0x4934A0,0x4F,'shortcut_selection_code'),
        (0x493150,0x59,'invalid_selection_optional_UI_prefix'),(0x4A7550,0x173,'return_C9_sender_code'),
        (0x4D65F0,0xFE,'timer_schedule_code'),(0x4D69F0,0x1F,'timer_clock_prefix'),
        (0x4962B0,0xB0,'lobby_mouse_dispatch'),(0x4966A0,0x10F,'lobby_UI_setup'),
        (0x41B420,0x87,'UI_collection_mouse_dispatch'),(0x630DF8,0x144,'selection_UI_nodes_data'),
        (0x631EC8,8,'selection_UI_node_source_data'),
        (0x495584,0x22,'UI_builder_node_stride_and_switch'),
        (0x495785,0x43,'UI_builder_type2'),(0x495811,0x49,'UI_builder_type3'),
        (0x414910,0x6E,'UI_base_constructor_prefix'),(0x4154E0,0x41,'UI_type2_constructor'),
        (0x415A30,0xAB,'UI_type3_constructor'),(0x4F7B60,0x58,'UI_type2_vtable_data'),
        (0x4F7C68,0x58,'UI_type3_vtable_data'),(0x414A80,0x12B,'UI_mouse_handler'),
        (0x414C00,0x3B,'UI_release_callback'),(0x414C40,8,'UI_callback_tailjump'),
        (0x415C90,0x1A,'UI_type3_callback')]
    rows=[]; insmap={}
    for va,n,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n
        out=ROOT/f'receive-boundary/VA-{va:08X}.bin';out.parent.mkdir(exist_ok=True);out.write_bytes(raw)
        isdata=label.endswith('_data'); ins=[] if isdata else [{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(raw,va)]
        for i in ins:insmap[int(i['va'],16)]=i
        rows.append({**mapping(pe,va),'bytes':n,'label':label,'kind':'data' if isdata else 'code','output':record(out),'instructions':ins,'decoded_bytes':sum(len(bytes.fromhex(i['bytes'])) for i in ins),'scope':'Aligned entry/known label; linear disassembly is evidence of listed instructions, not exhaustive reachable CFG or execution.'})
    write('receive-boundary/ranges.json',{'source_PE':record(PE),'generator':record(Path(__file__)),'ranges':rows})
    def req(va,mn,op):
        i=insmap[va];assert i['mnemonic']==mn and i['operands']==op;return i
    checks=[req(*args) for args in [
        (0x4B3868,'call','0x4b2970'),(0x4B38B4,'call','0x4efe62'),
        (0x4C7062,'call','0x4b37c0'),(0x4C704A,'call','0x4efe6e'),
        (0x47F365,'call','0x4c6b30'),(0x47FFB8,'call','0x4c6b30'),
        (0x4802B9,'mov','dword ptr [ebp - 0x28], 0x47f340'),
        (0x492AD4,'mov','byte ptr [0x75f694], al'),
        (0x492B9F,'mov','dword ptr [0x635b80], ecx'),
        (0x492B61,'call','0x4cd1c0'),(0x492B69,'jmp','0x492e6b'),
        (0x492D80,'call','0x4c9190'),(0x492D8A,'xor','eax, eax'),
        (0x492D91,'mov','dword ptr [0x834888], eax'),
        (0x492F02,'call','0x4c6ae0'),(0x492F42,'mov','edi, 0x64ad54'),
        (0x4934E9,'jmp','0x4928c0'),(0x4A76B2,'call','0x4c6ae0'),
        (0x4C6B27,'call','0x4c6770'),(0x4C67D0,'call','0x4efea4'),
        (0x414C45,'jmp','dword ptr [eax + 0x24]')]]
    # Switch data proves event-low16=1 maps to the pump case, independently of ASM comments.
    slot=pe.get_data(0x4C7194-0x400000,1)[0]
    event_target=struct.unpack('<I',pe.get_data(0x4C7180-0x400000+slot*4,4))[0]
    assert event_target==0x4C7062
    pointers={f'{va:08X}':f'{struct.unpack("<I",pe.get_data(va-0x400000,4))[0]:08X}' for va in [0x4F7314,0x4F7450,0x4F7454,0x4F7458,0x4F745C,0x4F7460,0x4F7464,0x630E08,0x630E44,0x630E80,0x630EBC,0x630F34,0x631ECC,0x4F7B80,0x4F7BA0,0x4F7C88,0x4F7CA8]}
    assert pointers['004F7314']=='773F5200'
    assert pointers['00630F34']=='004928C0'
    assert all(pointers[f'{v:08X}']=='004934A0' for v in [0x630E08,0x630E44,0x630E80,0x630EBC])
    # Candidate direct branches: raw rel32 pattern scan is explicitly not all indirect callers.
    candidates={f'{v:08X}':[] for v in [0x4B37C0,0x4B2970,0x4C6B30,0x4928C0]}
    for off in range(0x1000,min(0xF4000,len(image)-5)):
        if image[off] not in (0xE8,0xE9):continue
        target=(0x400000+off+5+struct.unpack_from('<i',image,off+1)[0])&0xFFFFFFFF
        key=f'{target:08X}'
        if key in candidates:candidates[key].append({'VA':f'{0x400000+off:08X}','kind':'call' if image[off]==0xE8 else 'jmp','raw_hex':image[off:off+5].hex(),'instruction_boundary_confirmed':(0x400000+off) in insmap})
    assert record(ASM)['sha256']=='2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A'
    lines=ASM.read_text(encoding='utf-8').splitlines()
    excerpts=[]
    for a,b,label in [(271364,271412,'WndProc entry'),(272862,272910,'WndProc network default'),(273056,273195,'Window setup'),(276769,276881,'Message loop'),(309077,309183,'Selection entry'),(309630,309716,'Selection C9 and savedalias'),(310368,310428,'Selection shortcut'),(315658,315780,'Lobby mouse'),(316227,316334,'Lobby setup'),(374693,374803,'Receive pump'),(415087,415160,'Network dispatcher'),(349943,349984,'Return C9')]:
        out=ROOT/f'receive-boundary/asm-{a}-{b}.txt';out.write_text('\n'.join(f'{i+1}: {lines[i]}' for i in range(a-1,b))+'\n',encoding='utf-8');excerpts.append({'label':label,'first_line':a,'last_line':b,'output':record(out)})
    write('receive-boundary/asm-excerpts.json',{'source_ASM':record(ASM),'warning':'Different recovered-PE fingerprint. ASM only locates labels; fixed PE captures control claims and external targets.','excerpts':excerpts})
    api=[]
    for dll,names in [('ws2_32.dll',['WSAAsyncSelect','send','WSAGetLastError']),('wsock32.dll',['recv']),('msvcrt.dll',['sprintf'])]:
        p=Path('C:/Windows/SysWOW64')/dll;d=pefile.PE(str(p))
        for name in names:
            e=next(x for x in d.DIRECTORY_ENTRY_EXPORT.symbols if x.name and x.name.decode()==name)
            api.append({'current_DLL':record(p),'export':name,'export_RVA':f'{e.address:08X}','forwarder':e.forwarder.decode()if e.forwarder else None,'current_DLL_is_historical_DLL_proven':False})
    expected={'WSAAsyncSelect':0x160C0,'send':0x158A0,'WSAGetLastError':0x5D50,'recv':0x1E40,'sprintf':0x75200}
    assert all(int(x['export_RVA'],16)==expected[x['export']]for x in api)
    write('receive-boundary/api-identities.json',{'updated_at':stamp(),'current_exports':api,'historical_base_support':record(ROOT/'priority-api-resolution.json'),'identities':[{'thunk_VA':'004EFE6E','branch_VA':'004EFE6F','fixed_target_VA':'759060C0','historical_WS2_base':'758F0000','current_export_RVA':'000160C0','name':'WSAAsyncSelect','status':'HIGH_CONFIDENCE_STATIC_IDENTITY; exact historical DLL hash not available'},{'thunk_VA':'004EFEA4','fixed_target_VA':'759058A0','historical_WS2_base':'758F0000','current_export_RVA':'000158A0','name':'send','status':'HIGH_CONFIDENCE_STATIC_IDENTITY'},{'thunk_VA':'004EFE7A','branch_VA':'004EFE7B','fixed_target_VA':'758F5D50','historical_WS2_base':'758F0000','current_export_RVA':'00005D50','name':'WSAGetLastError','status':'HIGH_CONFIDENCE_STATIC_IDENTITY'},{'pointer_VA':'004F7314','fixed_pointer_value':'773F5200','historical_MSVCRT_base':'77380000','current_export_RVA':'00075200','name':'sprintf','status':'HIGH_CONFIDENCE_STATIC_IDENTITY; two/three cdecl formatting args, not MessageBox'},{'thunk_VA':'004EFE62','branch_VA':'004EFE63','fixed_target_VA':'71581E40','name_candidate':'WSOCK32.recv','current_export_RVA':'00001E40','inferred_base_if_candidate_correct':'71580000','historical_report_WSOCK32_base':'71650000','matches_report_base':False,'ASM_target':'70E91E40','status':'ABI_AND_CURRENT_EXPORT_RVA_CANDIDATE_ONLY; no matching historical module base pin'}],'private_window_calls':{'window_setup_registration_call':'004802F9 -> 05AE0922','window_creation_call':'0048035A -> 05AE0000','message_pointer_values':pointers,'resolved_API_bodies':False,'meaning':'WNDCLASSEX/MSG-compatible call forms; private targets absent. Do not identify actual PeekMessage/DispatchMessage/RegisterClassEx implementation from signature alone.'},'official_semantics_sources':[{'url':'https://learn.microsoft.com/en-us/windows/win32/api/winsock2/nf-winsock2-wsaasyncselect','supports':'For the identified API, window message registration, nonblocking socket mode, FD_READ1/FD_WRITE2/FD_CONNECT16/FD_CLOSE32. Does not prove captured execution/thread identity.'},{'url':'https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-peekmessagea','supports':'An actual PeekMessage call can dispatch pending nonqueued messages. Private message pointer05AF04D8 not proven this API; no inferred reentry event.'}]})
    # All scoped code facts below cite raw captures; previous skill conclusions stay immutable.
    report={
        'updated_at':stamp(),'classification':'FINITE_STATIC_RECEIVE_AND_SELECTION_BOUNDARY_CORROBORATION',
        'source_PE':record(PE),'source_ASM':record(ASM),'accepted_batch3_preserved':[record(ROOT/n) for n in accepted],
        'range_manifest':record(ROOT/'receive-boundary/ranges.json'),'ASM_excerpt_manifest':record(ROOT/'receive-boundary/asm-excerpts.json'),'API_evidence':record(ROOT/'receive-boundary/api-identities.json'),
        'receive_path':{'ordered_edges':['WndProc47F340:47F365 or47FFB8 ->4C6B30','uMsg401; lParam high16 error0; low16event1 ->4C7062','4C7062 ->4B37C0','complete body ->4B3868 ->4B2970','prior accepted double opcode maps ->4B2AD3 ->4AEE70 ->4C9220 withglobal834888'],
            'event_switch':{'low16_event':1,'byte_table_VA':'004C7194','slot':slot,'DWORD_target':f'{event_target:08X}'},
            'frame_state':{'socket':'008343E8','remaining':'007607A4','body_flag':'007607A8','destination_cursor':'007607AC','buffer':'007607B0','body_length':'00760750'},
            'init':'4B38D0 sets remaining2/bodyflag0/cursorbuffer; startupworker47C8C3 caller retained from prior evidence',
            'frame_model':'Partial recv accumulation: first2 bytes LE total_length; remaining=total_length-2; complete body optionally decoded by4C66C0, immediately dispatched; repeat recv until<=0.',
            'complete_frame_dispatch_synchronous_with_pump':True,'skill_owner_wait_queue_found':False,
            'buffer_is_owner_readiness_queue':False,'multiple_complete_frames_can_dispatch_in_one_pump_call':True,
            'no_queue_scope':'Full4B37C0 plus message case contain no834888/class/order-ready predicate or deferred S1E queue. OS message queue/partial-byte accumulator are distinct from skill readiness. Unknown private hooks/indirect callers remain outside scope.',
            'exact_packet_length_and_malformed_frame_guards_closed':False},
        'thread_model':{'async_registration':'4C7041 pushes eventmask23h;4C7043 message401h;4C7048 windowargument;4C7049 socket;4C704A calls4EFE6E (WSAAsyncSelect identity).',
            'static_window_setup':'480290 creates48byteclass-like structure with47F340 at4802B9, passes at4802F9 toprivate05AE0922; creation at48035A->05AE0000 storesresult64BBEC at480362.',
            'main_loop':'4820A7/B3 load4F7464/60/5C; private05AF04D8 invoked4820CD withMSG-like64B6B0,HWnd/filter0,remove1;4820F8/4820FF private05AF0194/05AF0000; idle4D6570 andindirect62C9C0. Call forms compatible withPeekMessage/Translate/Dispatch, bodiesunrestored.',
            'ordinary_UI_stack':'WndProc mouse ->indirect62C9BC; lobbysetup4966A0 stores4962B0;4962B0 ->41B420/virtual+20;type2/3node constructors preservecallback/param at+24/+28;vtable mouse ->414A80 ->release+34 ->callback+40 ->414C40 or415C90;nodes630F34 confirm4928C0 and630E08/44/80/BC shortcut4934A0.',
            'source_model':'Conditional on these registrations and object vtables being the live values, both receive and selection paths run through this WndProc call stack. This is program-path evidence, not a sampled OS thread ID or exclusive-caller proof.',
            'rel32_candidate_scan_scope':'Fixed mapped PE RVA00001000..000F3FFF, rawE8/E9 rel32patterns checkedagainstknowncapturedinstructionboundaries. This scanexcludesindirectcalls/protectedexternalcode andisnot a wholeprogramCFG.',
            'actual_same_thread_proven':False,'private_message_API_bodies_restored':False,'all_indirect_or_protected_callers_excluded':False,'direct_rel32_candidates':candidates},
        'selection_boundaries':[
            {'path':'ordinary_new_owner','entry':'004928C0','class_store':'00492AD4 ->75F694','class4_order_store':'00492B9F ->635B80','owner_construct_call':'00492D80 ->4C9190','owner_publish_store':'00492D91 ->834888','C9_send_call':'00492F02 ->4C6AE0','program_order':'class/order selection -> owner constructor/publish -> C9 sender call','success_guard':'No explicit readyflag test adjacent toC9. Allocationfailure492D58->492D8A zeroesEAX thenpublishesNULL at492D91. Downstream failurecontinuation reachingC9 isNOTproved; laterconstructors/calls mayabort orfault. Publisher instruction alone doesnotcertifyliveowner/table.','unconditional_safe_boundary':False},
            {'path':'ordinary_reuse_owner','entry':'004928C0','preconditions':'648EBC/834888/834958 allnonzero at492B2E..492B50','reset_call':'00492B61 ->4CD1C0','join_jump':'00492B69 ->492E6B','C9_send_call':'00492F02','program_order':'class/order -> reuse/reset -> join -> C9','pointer_nonzero_proves_current_epoch_or_valid_table':False,'unconditional_safe_boundary':False},
            {'path':'shortcut_selection','entry':'004934A0','conditions':'selectedrow+18>=2 setsBL; laterBL true ->ECX8','tailjump':'004934E9 ->4928C0','meaning':'Shortcut reuses full ordinary callback; no localclass/owner skip inunmodifiedfixedPE path. Virtualrow/redrawcalls still requireliveobject evidence.','unconditional_safe_boundary':False},
            {'path':'return_saved_alias_C9','entry':'004A7550','conditions':'64AFBC or64AFBD nonzero ->4A769A;64AFBE zero','alias_argument':'004A76A3 ->64AD54','C9_send_call':'004A76B2 ->4C6AE0','after_call':'004A76BA writes64AFBE1','same_branch_initializes_class_order_owner':False,'meaning':'This localbranch skips4928C0 andcontainsnoownerconstruct/reset/readinesscheck. Priorreturn preparation/inheritedowner mustbeproved separately. C9 alone isnot a universalreadinesssignal.','unconditional_safe_boundary':False}],
        'C9_post_send_state':{'ordinary_saved_alias_copy':'64AD54 onlycopiedafter492F02 at492F42/4A/51','one_shot_return_flag':'64AFBE setafter4A76B2 at4A76BA','meaning':'If actualreentry occurs during sender, savedalias orone-shotflag may still holdpriorstate. Noactualnestedreceive/duplicateC9 proved.'},
        'reentrancy_audit':{'explicit_pump_call_in_receiver':True,'explicit_receive_pump_call_in_C9_formatter_or_send_body':False,'receive_decode_and_send_encode':'4C66C0/4C6700 completeleafloops; no call-out/messagepump','send_calls':'4C6AE0 builds/frameheaders ->4C6770;4C67D0 send,partialsend loop;errorpaths sprintf through4F7314,480620,4C65F0;formatpointer is773F5200/sprintf. No directMessageBox inferred.',
            'failure_private_call':'480764 ->05AD0986 in480620 tail; unknown body. Closepath4803A0 transitivecallbacks notfullyclosed.',
            'selection_callouts':'4928E8 4C8790;492914 494120 (virtualselected-rowquery);492958 49A510;49299C 46EA30;492ACA 446DF0;constructors/allocator;492DD2/492DDF indirectvirtualcalls;492E72/492E9E4C3E90;priorownerpublishedbeforethese lattercalls. Fulltransitiveprivate/hookclosure notclaimed.',
            'timer_registration':'4D65F0 stores deadline/callback/context in834C08array andincrements834C00; itdoesnot directlyinvokeregisteredcallback. Itcalls4D69F0 andallocator/copy, soabsent privatehooks cannotbeprovedfromstoreloopalone.',
            'actual_nested_S1E_proven':False,'no_reentry_proven':False,'sleep_is_ordering_proof':False},
        'source_only_server_boundary':{'ordinary_fresh_C9_conditional_support':True,'any_C9_unconditionally_sufficient':False,'startBattle_name_alone_sufficient':False,
            'conditional_requirements':['C9 belongs toordinary/unmodified4928C0 or4934A0 tailpath, andmatches selectedcharacter/connection epoch','Allocation/reset succeeded;global834888 validwithlive+2Ccount/+58table;class75F694/order635B80 consistent','No concurrent/nested S1E orreset changesstate acrosspublication/send/receive; thread/callmodelmustbeestablished','Return4A7550 orpatchedbypass paths haveindependentlyproveninheritedowner/class/order/reset state','Metadata fallback andiconmatch gates fromacceptedbatch3 stillapply; readiness doesnotguaranteepopulatedrows'],
            'conclusion':'Static source establishes ordinary program order and identifies return C9 exception. It does not establish an unconditional server send boundary or prove first-lobby success. Server lifecycle timing comparison must distinguish admission/return/duplicate paths; no product change requested by this evidence.'},
        'minimal_dynamic_requirements':[
            {'need':'Sameconnection/character/owner epoch and threadidentity','points':['47F340 entry uMsg401','4C7062/4B3868','4928C0 or4934A0 entry','4A769A/4A76B2 returnbranch'],'fields':['PID+creationFILETIME','OSthreadID','HWND','connectionID','selectedalias/pathorigin','monotonicsequence/timestamp']},
            {'need':'Readiness andordering, not justpointerpresence','points':['492AD4/classorder branch','before/after492B61 reset','before/after492D80 then492D91','492F02 or4A76B2','4AEEF3/4C9220 and4CD050'],'fields':['75F694','635B80','834888','owner+2C','owner+58','ownerallocation/result/epoch','sameowner reset counter','metadata647E20/647E24 availability']},
            {'need':'S1E semantic inputs/output andnestedcall evidence','fields':['rawopcode/count/bitmap andbodylength','setbitindex/icon/fallback/duplicate decision','rowsbefore/aftermatch','allS1F/SDA/reset ordering','nested callstack-depth vsnormalWndProc dispatch','returnAFBC/AFBD/AFBE flagstates'], 'purpose':'Separate early delivery, failed ownerallocation, inheritedreturnowner, duplicateindex/nomatch, resetafterfill, andactualreentry. Timer/sleep/resourcecounts cannot substitute.'}],
        'runtime_readiness_complete':False,'new_opcode':False,'wire_contract_changed':False,
        'original_game_launched':False,'process_opened':False,'GUI_accepted':False,
        'excluded_work':'No class4 closed32 producer rerun, icon/resource extraction, Java/SQL edit, sharedBIN patch, native/injection/screenshot, or server lifecycle execution.'}
    write('S1E-receive-selection-boundary.json',report)
    write('receive-boundary/new-evidence-verification.json',{'updated_at':stamp(),'fixed_PE':record(PE),'asserted_instructions':checks,'network_event_switch_target_verified':True,'callback_pointer_values':pointers,'range_output_hashes_and_fixed_PE_bytes_match':all(record(Path(r['output']['path']))==r['output'] and Path(r['output']['path']).read_bytes()==pe.get_data(int(r['va'],16)-0x400000,r['bytes'])for r in rows),'accepted_batch3_primary_pins_and_snapshot_manifest_unchanged':True,'old_tests_replayed':False,'runtime_assertions':False})
    print(json.dumps({'new_ranges':len(rows),'fixed_PE_checks':len(checks),'first_event_target':f'{event_target:08X}','report':record(ROOT/'S1E-receive-selection-boundary.json')},ensure_ascii=False))

if __name__=='__main__':main()
