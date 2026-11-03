"""Batch13 index existing raw for saved host versus character alias; no old audits run."""
from audit import *

OUT=ROOT/'reconnect-source-index'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
BATCH12='0DEEF1249E3F98A780994680575AF5EE0003DDEEDF6C9A95A5EC109D26184DCE'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[]
    for p,h in [(PE,FIXED),(BIN,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'),
      (ASM,'2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A')]:
        q=record(p);assert q['sha256']==h;sources.append(q)
    previous=load('status-family-50-51-52-56-contract.json');assert record(ROOT/'status-family-50-51-52-56-contract.json')['sha256']==BATCH12
    preserved=previous['prior_primary_pins']+[{**record(ROOT/'status-family-50-51-52-56-contract.json'),'expected_sha256':BATCH12,'match':True}]
    for p in preserved:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    old={n:record(ROOT/n)for n in ['host-alias-lifecycle.json','reconnect-host-audit.json','modern-F4-contract.json',
      'S1E-receive-selection-boundary.json','priority-api-resolution.json']}
    assert old['host-alias-lifecycle.json']['sha256']=='CA359ECAFEAA2BD97239414858F62C02C9AD9DCBEA728921C7E62EBFD66EB644'
    inherited=[];ins={};owners={}
    entries=[('lifecycle/ranges.json',x)for x in [0x47C8A0,0x47CAD0,0x480A60,0x481831,0x481DC7,0x492F07,0x64AD2C,0x64B7E4]]
    entries += [('receive-boundary/ranges.json',x)for x in [0x4928C0,0x4A7550,0x4C6AE0,0x480620,0x4C6B30]]
    entries += [('priority-ranges.json',x)for x in [0x469B10,0x46ED50]]
    entries += [('priority-complete-tails.json',x)for x in [0x469BD0,0x46F010,0x475F40]]
    for file,va in entries:
        row=next(x for x in load(file)['ranges']if int(x['va'],16)==va);pin=row['output'];raw=Path(pin['path']).read_bytes()
        assert record(Path(pin['path']))==pin and raw==pe.get_data(va-0x400000,len(raw))
        if va<0x500000:
            # Decode from the already established range entry, not literal candidate offsets.
            for x in cs.disasm(raw,va):
                ins[x.address]={'va':f'{x.address:08X}','raw_hex':x.bytes.hex(),'mnemonic':x.mnemonic,'operands':x.op_str};owners[x.address]=pin
        inherited.append({'source_index':record(ROOT/file),'range':pin,'va':f'{va:08X}','raw_matches_fixed_PE':True,
          'scope':'Reuse established bounded raw; not a new fullCFG/runtime execution claim','old_case_or_producer_replayed':False})
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**{k:v for k,v in ins[va].items()if k!='va'},'range':owners[va]}
    expected=[(0x47C9B7,'call','0x50104cd'),(0x47C9BC,'mov','edi, 0x64b7e4'),(0x47C9C6,'repne scasb','al, byte ptr es:[edi]'),
      (0x47C9C8,'not','ecx'),(0x47C9D0,'mov','edi, 0x64ad74'),(0x47C9D8,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x47C9DF,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x47C9E1,'mov','ecx, 0xa'),
      (0x47C9E6,'mov','edi, 0x64ad2c'),(0x47C9EB,'rep stosd','dword ptr es:[edi], eax'),
      (0x480BB2,'mov','edi, dword ptr [eax + edx*4]'),(0x480BC1,'mov','edi, 0x64b7e4'),
      (0x480BC9,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),(0x480BD0,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),
      (0x480C77,'mov','edi, 0x64b7e4'),(0x480C7F,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x480C86,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x481848,'mov','edi, 0x64b950'),
      (0x481861,'mov','edi, 0x64b7e4'),(0x481869,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x481870,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x481DC7,'xor','esi, esi'),(0x481DC9,'mov','edi, esi'),
      (0x481DD2,'repne scasb','al, byte ptr es:[edi]'),(0x481DDC,'mov','edi, 0x64b7e4'),
      (0x469B4C,'mov','edi, 0x64ad74'),(0x469B67,'mov','edi, 0x64ae74'),(0x469B76,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x469B86,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x46F13B,'mov','edi, 0x64ad74'),
      (0x46F152,'mov','edi, 0x64ae74'),(0x46F160,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x46F174,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x4760FF,'push','0x64ad74'),(0x476104,'call','0x4efe56'),
      (0x476113,'push','0x64ad74'),(0x476118,'call','0x4efe5c'),(0x4806F6,'push','0x64b7e4'),(0x480703,'call','0x773f5200'),
      (0x492922,'lea','eax, [eax + eax*4]'),(0x492925,'lea','edx, [eax + eax*8]'),
      (0x492931,'lea','ebx, [edx*4 + 0x62faa0]'),(0x492F33,'mov','edi, ebx'),(0x492F38,'repne scasb','al, byte ptr es:[edi]'),
      (0x492F42,'mov','edi, 0x64ad54'),(0x492F4A,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
      (0x492F51,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x4A76A3,'push','0x64ad54'),
      (0x4A76A8,'push','0xc9'),(0x4A76AD,'push','0x502498'),(0x4A76B2,'call','0x4c6ae0'),
      (0x4A76BA,'mov','byte ptr [0x64afbe], 1'),(0x4C6B00,'call','0x4c68d0'),
      (0x47C9C1,'or','ecx, 0xffffffff'),(0x47C9C4,'xor','eax, eax'),(0x492F0C,'xor','eax, eax'),
      (0x480BA4,'mov','eax, dword ptr [0x64b498]'),(0x480B9E,'idiv','dword ptr [0x64b49c]'),
      (0x480BAC,'mov','dword ptr [0x64b4a0], edx'),
      (0x469B7A,'mov','word ptr [0x64ad70], 0x7d0'),(0x469B8D,'movsx','ecx, word ptr [0x64ad70]'),
      (0x469C46,'mov','word ptr [0x64ad70], cx'),(0x46EEC5,'mov','word ptr [0x64ad70], ax'),
      (0x46F176,'mov','word ptr [0x64ad70], 0x7d0'),(0x4C6CBF,'movsx','eax, word ptr [0x64ad70]')]
    assertions=[]
    for at,mn,op in expected:
        assert(ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at));assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    # Fixed-address literal inventory is finite; never decode candidates at an unaligned offset.
    literal_inventory={};image=PE.read_bytes()
    for target in [0x64AD74,0x64AD54,0x64AD70,0x64B7E4]:
        needle=struct.pack('<I',target);cursor=0;hits=[]
        while (at:=image.find(needle,cursor))>=0:
            candidate=at+0x400000;cursor=at+1
            matches=[va for va,x in ins.items()if va<=candidate and candidate+4<=va+len(bytes.fromhex(x['raw_hex']))and f'0x{target:x}'in x['operands']]
            assert len(matches)==1,(hex(target),hex(candidate),matches)
            hits.append({'literal_storage':mapping(pe,candidate),'confirmed_aligned_instruction':w(matches[0]),
              'classification':'Confirmed instruction operand, not independent writer/clear proof'})
        literal_inventory[f'{target:08X}']=hits
    assert[len(literal_inventory[k])for k in ['0064AD74','0064AD54','0064AD70','0064B7E4']]==[5,2,11,7]
    write('reconnect-source-index/direct-address-reference-index.json',{'source':sources[0],'inventory':literal_inventory,
      'scope':'Only these four fixed-address literal patterns; no wholeprogram decode, alias completeness or transitive callback claim',
      'clear_result':'No confirmedliteraloperand is a direct savedhost/name clear. Other aliases,bulk clears orprivateinitialization remain unknown.'})
    # One genuinely useful data gap: make the existing C9 format literal directly reusable.
    fmt=pe.get_data(0x102498,3);assert fmt==b'cs\0';out=OUT/'C9-format-00502498.bin';out.write_bytes(fmt)
    data={'label':'ExistingC9format','source':sources[0],**mapping(pe,0x502498),'raw_hex':fmt.hex(),'output':record(out)}
    write('reconnect-source-index/ranges.json',{'new_code_ranges':0,'new_data_ranges':1,'new_data':data,'inherited_ranges':inherited})
    legacy=load('host-alias-lifecycle.json');api=load('priority-api-resolution.json')
    api_inferences=[x for x in api['resolutions']if x['current_export']['name']in ['inet_addr','gethostbyname','sprintf']]
    boundaries=[{'callsite':w(0x47C9B7),'target':mapping(pe,0x50104CD),'body_sha256':None,'unknown':'No fixedPE body; successfulreturnbeforehostcopy unobserved'},
      {'callsite':w(0x480703),'target':mapping(pe,0x773F5200),'body_sha256':None,'unknown':'Externalbody unavailable; oldsprintf identityinference only, successfulformat/read not proven'},
      {'callsite':w(0x476104),'target':mapping(pe,0x4EFE56),'next_external':'758F57E0','external_body_sha256':None,'unknown':'Oldinet_addr identityinference; no dump-timeDLLbytes/liveconversionqualification'},
      {'callsite':w(0x476118),'target':mapping(pe,0x4EFE5C),'next_external':'7591FF10','external_body_sha256':None,'unknown':'Oldgethostbyname identityinference; no DNS/nativeexecution'},
      {'callsite':w(0x4C6B00),'target':mapping(pe,0x4C68D0),'body_sha256':None,'unknown':'Second-layerpacketformatter notexpanded; cs argument/source proven, encoding/bodyserialization notnewlyproved'},
      {'callsite':w(0x4C6AE8),'target':mapping(pe,0x4F11A0),'body_sha256':None,'unknown':'Sendersecond-layerstack/probe helper notexpanded orqualified'},
      {'callsite':w(0x4C6B27),'target':mapping(pe,0x4C6770),'body_sha256':None,'unknown':'Existingtransport boundary; no socket/send/private/externalexecution repeated'},
      {'callsite':w(0x481836),'target':None,'body_sha256':None,'unknown':'IndirectEAX providercall output64B950; target andupstreamencoding unidentified'}]
    source_contract={'saved_startup_host':{'address':mapping(pe,0x64AD74),'storage':'Inline256byte region, not pointer global; address immediates arepassed/scanned',
      'acquisition_event':'47C8A0login/initworker afterprivate050104CD returns; bytecopy64B7E4→64AD74 includingfirstNUL',
      'writer':{'start':w(0x47C9BC),'destination':w(0x47C9D0),'copyDWORD':w(0x47C9D8),'copyBYTE':w(0x47C9DF)},
      'startup_sources_already_established':legacy['host_initializer']['sources'][:3],
      'default_source_indirection':{'list_pointer':w(0x480BA4),'count_not_string_length':w(0x480B9E),
        'selected_index':w(0x480BAC),'string_pointer':w(0x480BB2),
        'meaning':'64B498 holdslistpointer;64B49C listcount and64B4A0 index, nothostlength. [listpointer+index*4] yieldsborrowedstringpointer;copy ownsbytesin64B7E4.'},
      'fourth_candidate_qualification':{'prior_report':old['host-alias-lifecycle.json'],'candidate':'00481DC7..00481DEB',
        'fixed_fragment':'If enteredat481DC7, xorESI/ movEDI,ESI yieldsEDI0 before481DD2scasb; no validstringpointer source proven by thisfragment',
        'usable_COM_derived_host_proven':False,'unknown':'Upstream/privatecontrol,registersetup oractualexecution notclosed; do not assign successfulCOMstring acquisition'},
      'encoding':'NULterminated8bit bytes copied unchanged; no Unicode/UTF8/CP950 conversion oncopy. HostAPIoldinference ischar*,specificupstreamcodepage/IDN normalization unproved.',
      'capacity':256,'capacity_evidence':'Inline span toknowncurrent-host64AE74 andASMbounds, notoriginalPDB/allocator typeproof',
      'in_bounds_payload_max_bytes_excluding_NUL':255,'native_enforced_maximum_proven':False,
      'lifetime':'Copiedbytes independentofstartupsourcepointer. Later64B7E4 overwrite doesnotautomaticallyrefreshsavedhost. Newworker copy canoverwrite; no epoch/nevercleared claim.',
      'consumers':['469B4Ccopies→64AE74 inmasterreturntimer','46F13Bcopies→64AE74 inreturnmode2',
        '4760FF/476113 passinlineaddress toinet_addr/gethostbyname inferredAPIs'],
      'clear':'No directclear in5confirmedaddress references. Worker47C9E6clearsthe40bytechannel tableending64AD54, notsavedhost.',
      'private_precondition':boundaries[0]},
      'saved_character_alias':{'address':mapping(pe,0x64AD54),'storage':'Inline28bytes until64AD70; notDWORD pointer despiteIDA dwordlabel',
        'acquisition_event':'Ordinary4928C0 selection, afterC9call492F02 returns, scansselectedrecordEBX at492F33 andcopythroughNUL at492F42/4A/51',
        'record_source':'EBX=0062FAA0+selected_index*0xB4, established492922/25/31; nothost/address norincomingframebody',
        'upstream_roster_population_and_encoding':'Notexpanded. No newlyprovenSopcode,codepage,legalnamecharlimit ornetworkacquisition timing.',
        'copy':{'scan':w(0x492F38),'destination':w(0x492F42),'DWORDcopy':w(0x492F4A),'BYTEcopy':w(0x492F51)},
        'encoding':'Opaque8bit throughfirstNUL, includingNUL copied; no conversion inthispath. Bytecapacity isnotcharcount.',
        'capacity':28,'capacity_evidence':'Inline span64AD54..64AD70 beforeobservedport, plusIDA7DWORDlabel; notoriginalPDB/allocator typeproof',
        'in_bounds_payload_max_bytes_excluding_NUL':27,'native_enforced_maximum_proven':False,
        'lifetime':'Independentcopiedglobalbytes; nextordinaryselection overwritesprefixthroughNUL, notalltailbytes. Recordpointer neednotberetained. No clear/directfreefound in2literalrefs; aliases/privatebulkclears unknown.',
        'return_consumer':{'entry':'004A7550','conditions':'64AFBCor64AFBD→4A769A;64AFBE==0',
          'alias_pointer_argument':w(0x4A76A3),'opcode_argument':w(0x4A76A8),'format':data,'send':w(0x4A76B2),'postsendflag':w(0x4A76BA),
          'qualification':'Cachedalias C9 source argument proven. No ownerconstruct/reset/readiness inthisbranch, no sendercompletion/epoch qualification.'}},
      'neighbor_port':{'address':mapping(pe,0x64AD70),'width':2,'role':'Current reconnectportlow16, notstring length orpointer',
        'writes':['469B7A/46F176 literal2000','469C46 CX afteroldcurrentport handling','46EEC5 parsedportAX'],
        'loads':'SomeuseMOVSX signedword; bitstorage16, APIwholeportinterpretation notexpanded',
        'padding':'64AD72..73 are2alignmentbytes beforehost64AD74'},
      'neighbor_clear':{'start':w(0x47C9E6),'count':w(0x47C9E1),'store':w(0x47C9EB),'bytes':40,
        'range_half_open':['0064AD2C','0064AD54'],'clears_alias_or_port_or_host':False},
      'snapshot_difference':{'fixed_PE_inline_buffers_zero':all(pe.get_data(v-0x400000,n)==bytes(n)for v,n in [(0x64AD54,28),(0x64AD74,256),(0x64B7E4,256)]),
        'different_ASM_recovered_host_literal':'560604 a21020018214 is210.200.182.14,NUL',
        'meaning':'OriginalpackedBIN hashesidentifyinput only, notunpackedruntimevalue. FixedPEzero andASMdifferentimage literal cannotsetModernendpoint/characteridentity.'}}
    write('reconnect-source-index/host-alias-port-contract.json',source_contract)
    write('reconnect-source-index/private-external-boundaries.json',{'boundaries':boundaries,'prior_API_inference_index':old['priority-api-resolution.json'],
      'inference_limits':[{'name':x['current_export']['name'],'historical_dll_sha256':x['historical_dll_sha256'],'confidence':x['confidence']}for x in api_inferences],
      'provider_boundary':'481836 indirectCALL EAX output64B950; provideridentity/body/currentbytes/codepage notqualified',
      'selection_source_boundary':'494120selectionindex andupstreamrosterpopulation notexpanded; selectedrecord arithmetic/rawcopy provenonly'})
    cases=[]
    for capacity,name in [(256,'host'),(28,'alias')]:
        for payload_length in [capacity-1,capacity]:
            consumed=payload_length+1;cases.append({'case':f'{name}_payload{payload_length}',
              'payload_bytes':payload_length,'copied_bytes_including_NUL':consumed,'region_bytes':capacity,
              'in_bounds_copy':consumed<=capacity,'overflow_bytes':max(0,consumed-capacity),
              'scope':'Arithmeticcode-derived capacity model, notnativeguard oractualinput','match':True})
    payload=b'\xA4\x40\0\xEE\xEE';copied=payload[:payload.index(0)+1];assert copied==b'\xA4\x40\0'
    cases.append({'case':'opaque_highbytes_firstNUL','input_hex':payload.hex(),'copy_hex':copied.hex(),
      'encoding_identity_not_inferred':True,'match':True})
    assert 0x64AD2C+10*4==0x64AD54
    cases.append({'case':'table_clear_boundary','start':'0064AD2C','10DWORDs_bytes':40,'end_exclusive':'0064AD54',
      'alias_untouched_by_this_clear':True,'match':True})
    write('reconnect-source-index/new-code-derived-models.json',{'new_static_models':6,'cases':cases,'native_or_oldPASS_or_producer_runs':0})
    lines=ASM.read_text(encoding='utf-8').splitlines();nav=[]
    for a,b,label in [(273646,273665,'Additionalstartuphostreadalreadyinexistingraw'),(309118,309132,'selectedrecordpointer'),
      (559666,559690,'inlineregionboundaries'),(560600,560609,'differentASMinitializedhost')]:
        nav.append({'label':label,'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))})
    write('reconnect-source-index/ASM-navigation.json',{'source':sources[2],'blocks':nav,'ASMlabels_notfixedPEstate':True})
    write('reconnect-source-index/new-evidence-verification.json',{'sources':sources,'new_code_ranges':0,'new_data_ranges':1,
      'inherited_raw_ranges':len(inherited),'instruction_assertions':assertions,'assertion_count':len(assertions),
      'all_reused_raw_match_fixed_PE':True,'literal_patterns':4,'confirmed_literal_operands':25,'unmatched_candidates':0,
      'prior_primary_pins':preserved,'additional_existing_evidence_pins':old,'new_static_models':6,
      'historical_pre_batch_seal_metadata':{'manifest':record(ROOT/'artifact-manifest.json'),'verification':record(ROOT/'delivery-verification.json'),
        'meaning':'Historicalaggregates, notpostpublishcorepins'}})
    reusable=[record(p)for p in OUT.rglob('*')if p.is_file()]
    write('reconnect-source-index-contract.json',{'schema':'lito.reconnect.host-character-alias.finite-index.batch13.v1','updated_at':stamp(),
      'status':'EXISTING_SOURCE_INDEXED_FINITE_GAPS_QUALIFIED','sources':sources,'prior_primary_pins':preserved,'existing_evidence':old,
      'inherited_raw':inherited,'reusable_outputs':sorted(reusable,key=lambda x:x['path']),'source_contract':source_contract,
      'scope':'64AD74/64AD54/64AD70/64B7E4 directreads/writes/copy/clear andcached4A7550 C9arguments only. No upstreamrosterprotocol/wholeprogram/privatebody expansion.',
      'newly_qualified_gaps':['25alignedaddress operands withno directhost/nameclear; aliasabsence notwholeprogramproof',
        'Selectedrecordsource62FAA0+index*B4 indexedfromexistingraw','481DC7candidate nullscan fragment cannotqualify successfulCOMhostsource',
        'Byte/NUL/capacity/clear limits madeexplicit; nativeenforcedmax/codepage/legalcharlimit unknown'],
      'Modern_usable':['SeparatecopiedsavedStartupHostbytes,savedCharacterAliasbytes andportstate; inline regionsnotpointers',
        'Retainraw8bit/NUL contract andsource/connectionepoch;255host/27alias areinboundssafe bytebudgets, notprovennativevalidation',
        'CachedC9 mayuseoldalias withoutordinaryownerpath; requireindependentowner/class/order/reset/epochproof'],
      'Modern_not_inferable':['No defaultendpoint orcharacterfromzeroPE/ASMliteral','No CP950/UTF8/Unicodeconversion/legalnamecharlimit fromcopy',
        'No provenneverclear/perpetuallifetime','No validCOMsourceorproviderprivatebody','No guaranteedC9send,owner/player/GUIready'],
      'server_handoff':{'classification':'KNOWN_CONTRACT_INDEX_AND_BOUNDED_SOURCE_QUALIFICATION','new_wire_differences':0,'new_opcodes':0,
        'wire_corrections':0,'known_C_C9':'formatcs,aliasargument64AD54; sourceclarificationonly','packet_delta_unchanged':True},
      'runtime_acceptance':{'original_handler_or_worker_executed':False,'host_or_character_live_value_qualified':False,
        'owner_ready':False,'player_ready':False,'GUI_accepted':False,'overall_unpacking_accepted':False},
      'constraints':{'writes_only_own_directory':True,'native_observer_process_input_injection_screenshot':0,'Java_SQL_oldPASS_producer':0,
        'new_archive_or_capture':0,'other_group_changes':0,'blocked_server_work_or_S0_policy':False}})
    print(json.dumps({'report':record(ROOT/'reconnect-source-index-contract.json'),'new_code_ranges':0,'new_data_ranges':1,
      'inherited_raw_ranges':len(inherited),'instruction_assertions':len(assertions),'aligned_address_operands':25,'new_static_models':6},ensure_ascii=False))

if __name__=='__main__':main()
