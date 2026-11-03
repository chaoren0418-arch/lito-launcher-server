"""Batch21 finite map table producer/caller/UI byte consumer islands. No runtime."""
from audit import *
OUT=ROOT/'map-table-source'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='3ADB8FD6D3F48E6BB444C00BF3808A1F1A758E82C2EB2E9CEDD4F45CFAB38367'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]]
    assert [r['sha256']for r in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('SCC-catalog-lifecycle-contract.json');pin=record(ROOT/'SCC-catalog-lifecycle-contract.json');assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==22
    for r in prior:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,q):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(r['raw_hex']))for r in rows)==len(raw),(hex(va),'incomplete alignment')
        for r in rows:at=int(r['va'],16);ins[at]=r;owners[at]=q
        return rows
    def reuse(index,base,at,n):
        row=next(r for r in load(index)['ranges']if int(r['va'],16)==base);q=row['output'];raw=Path(q['path']).read_bytes();off=at-base
        assert record(Path(q['path']))==q and raw==pe.get_data(base-0x400000,len(raw))and 0<=off<=off+n<=len(raw)
        inherited.append({**mapping(pe,at),'base_va':f'{base:08X}','range_offset':off,'index':record(ROOT/index),'range':q,'used_bytes':n,'kind':'code','raw_matches_fixed_PE':True,'old_tests_replayed':False})
        decode(raw[off:off+n],at,q)
    reuse('SCC-catalog/ranges.json',0x4A7A30,0x4A7AB5,43)
    reuse('SCC-catalog/ranges.json',0x463D70,0x463D70,13)
    reuse('SCC-catalog/ranges.json',0x463C10,0x463C47,12)
    specs=[(0x46F870,10,'code','initializer_entry_call_jump'),(0x46F880,10,'code','global_table_ctor_wrapper'),(0x46F890,12,'code','cleanup_registration_helper_boundary'),
      (0x46F8A0,10,'code','global_table_cleanup_wrapper'),(0x46F8D0,22,'code','table_ctor_complete_only_embedded_state_and_recordpointer_zero'),
      (0x4D3DB0,13,'code','embedded_line_state_ctor_complete'),(0x46F8B0,13,'code','record_ctor_complete_zero_pointer_and_byte8'),(0x46F8C0,14,'code','record_dtor_complete_allocator_boundary'),
      (0x46F8F0,106,'code','table_dtor_complete_to_callbacks_and_embedded_cleanup_boundaries'),(0x4D3DC0,51,'code','embedded_cleanup_complete_allocator_boundaries'),
      (0x46F9C0,73,'code','map_loader_prefix_to_provider_and_failure_return_only'),(0x4D4450,43,'code','provider_name_prefix_STOP_first_external_formatter'),
      (0x4D43D0,45,'code','locale_suffix_selector_complete_runtime_input_unqualified'),(0x4D4870,14,'code','startup_loader_prefix_STOP_first_private_call'),
      (0x4D48D6,28,'code','map_load_direct_caller_result_checks_ONLY'),(0x481F7D,9,'code','startup_load_call_and_test_ONLY'),
      (0x46FA6F,31,'code','conditional_postprovider_pointer_count_stores_ISLAND_not_qualified_builder'),
      (0x46B926,171,'code','table_title_selector_UI_island_STOP_direct465110_callee'),(0x4658B0,108,'code','selected_catalog_row_BYTE_to_preview_chain_complete'),
      (0x4687D0,13,'code','preview0F_BYTE_setter_complete'),(0x468020,52,'code','preview_render_prefix_STOP_private_errorpath'),
      (0x4681AF,71,'code','conditional_preview_signed_BYTE_table_title_argument_island'),(0x4681F6,16,'code','negative_preview_BYTE_version_branch_STOP'),
      (0x4683F6,2,'code','prepared_preview_text_first_indirect_formatter_STOP'),
      (0x502070,4,'data','fixed_init_entry_pointer_NOT_runtime_schedule'),(0x64AFD0,20,'data','disk_table_object_zero_bytes_NOT_runtime_state'),
      (0x4D4400,20,'data','locale_suffix_jump_table'),(0x4D4414,19,'data','locale_suffix_slot_map'),(0x508900,10,'data','provider_name_format_s_dash_c_tbl'),
      (0x636590,9,'data','maptable_base_name_LTmapstr'),(0x4F7314,4,'data','disk_preview_formatter_pointer_NOT_runtime_IAT')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x46F870,'call','0x46f880'),(0x46F880,'mov','ecx, 0x64afd0'),(0x46F895,'call','0x4f10e0'),(0x46F8A5,'jmp','0x46f8f0'),
      (0x46F8D6,'call','0x4d3db0'),(0x46F8DB,'mov','dword ptr [esi + 0x10], 0'),(0x4D3DB4,'mov','dword ptr [eax], ecx'),
      (0x4D3DB6,'mov','dword ptr [eax + 8], ecx'),(0x4D3DB9,'mov','dword ptr [eax + 4], ecx'),(0x46F8B2,'mov','dword ptr [eax], 0'),
      (0x46F8B8,'mov','byte ptr [eax + 8], 0'),(0x46F8C7,'call','0x4f103c'),(0x46F90F,'mov','eax, dword ptr [edi + 0x10]'),
      (0x46F91D,'mov','ecx, dword ptr [eax - 4]'),(0x46F92A,'push','0x24'),(0x46F92D,'call','0x4f1362'),(0x46F933,'call','0x4f103c'),
      (0x46F946,'call','0x4d3dc0'),(0x4D3DD3,'mov','dword ptr [esi + 8], 0'),(0x4D3DEA,'mov','dword ptr [esi + 4], 0'),
      (0x46F9E9,'lea','ebx, [esi + 4]'),(0x46F9EE,'call','0x4d4450'),(0x46F9F5,'jne','0x46fa09'),
      (0x4D445C,'call','0x4d43d0'),(0x4D446A,'movsx','eax, al'),(0x4D446F,'push','0x508900'),(0x4D4476,'call','0x773f5200'),
      (0x4D43D0,'mov','eax, dword ptr [0x636584]'),(0x4D43E7,'jmp','dword ptr [ecx*4 + 0x4d4400]'),(0x4D4879,'call','0x5040e1d'),
      (0x4D48D6,'push','0x636590'),(0x4D48DB,'mov','ecx, 0x64afd0'),(0x4D48E0,'call','0x46f9c0'),(0x481F7D,'call','0x4d4870'),
      (0x46FA6F,'mov','dword ptr [esi + 0x10], eax'),(0x46FA75,'mov','ecx, 7'),(0x46FA7C,'rep stosd','dword ptr es:[edi], eax'),
      (0x46FA7E,'mov','eax, dword ptr [esi + 4]'),(0x46FA8C,'mov','dword ptr [esi], eax'),
      (0x46B926,'mov','eax, dword ptr [0x64afd0]'),(0x46B935,'mov','eax, dword ptr [0x64afe0]'),(0x46B93C,'mov','ecx, dword ptr [esi + eax + 4]'),
      (0x46B942,'mov','dx, word ptr [0x64acca]'),(0x46B955,'mov','ecx, dword ptr [esi + eax + 0x20]'),(0x46B993,'mov','eax, dword ptr [esi + eax]'),
      (0x46B996,'add','ecx, 0xfffffe0c'),(0x46B9A7,'sub','edx, 0x1f4'),(0x46B9B5,'call','0x465110'),(0x46B9C6,'add','esi, 0x24'),
      (0x4658B8,'jne','0x46591a'),(0x465908,'mov','al, byte ptr [edx + 0x74]'),(0x46590C,'call','0x4687d0'),
      (0x4687D3,'mov','al, byte ptr [ebp + 8]'),(0x4687D6,'mov','byte ptr [ecx + 0xf], al'),(0x46804F,'call','0x5ad0986'),
      (0x4681AF,'mov','al, byte ptr [ebx + 0xf]'),(0x4681B8,'cmp','al, 0xff'),(0x4681BA,'jle','0x4681f6'),
      (0x4681D5,'mov','edx, dword ptr [0x64afe0]'),(0x4681DB,'movsx','eax, al'),(0x4681DE,'lea','eax, [eax + eax*8]'),
      (0x4681E1,'mov','eax, dword ptr [edx + eax*4]'),(0x4681F6,'cmp','dword ptr [0x63d54c], 0x7794'),(0x4683F6,'call','edi'),
      (0x4A7AC4,'movsx','esi, cx'),(0x4A7AD0,'cmp','dword ptr [ecx], esi'),(0x4A7AD5,'add','ecx, 0x24')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at));assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    assert pe.get_data(0x236590,9)==b'LTmapstr\0'and pe.get_data(0x108900,10)==b'%s-%c.tbl\0'
    index=load('resources/archive-manifest.json');rows=index['selected_members']
    candidates=[]
    for name in ['LTmapstr-c.tbl','LTmapstr-h.tbl','LTmapstr-h.txt','LTmapstr-j.tbl','LTmapstr-k.tbl']:
        matches=[r for r in rows if r.get('namespace')=='native'and r.get('archive')=='LTtext'and r.get('name')==name];assert len(matches)==1
        r=matches[0]
        for k in ['raw','decoded']:assert record(Path(r[k]['path']))==r[k]
        data=Path(r['decoded']['path']).read_bytes();lines=data.splitlines();nonempty=[(i+1,b)for i,b in enumerate(lines)if b.strip()]
        candidates.append({'name':name,'archive':'LTtext','row':r['row'],'offset':r['offset'],'bytes':r['bytes'],'source_index_sha256':r['source_index_sha256'],
          'source_pak_sha256':r['source_pak_sha256'],'raw':r['raw'],'decoded':r['decoded'],'encoding_reference':r['encoding'],
          'first_nonempty_line_hex':nonempty[0][1].hex(),'first_nonempty_line_ascii':nonempty[0][1].decode('ascii','replace'),
          'first_6_nonempty_lines':[{'physical_line_1based':i,'raw_hex':b.hex()}for i,b in nonempty[:6]],
          'qualification':'Existingsealeddecodedcandidate only; nofileextraction/codecredecode/providerload. Physicalfileheader/lineordinalnotlivecount/recordindex/keybinding.'})
    sources_table={'object_VA':'0064AFD0','record_pointer_VA':'0064AFE0','record_stride':36,
      'object_fields':{'+0':'countusedbyCClookup/UIislands; constructorDOESNOT explicitlysetthisfield', '+4..+C':'embeddedline/state triple initializedzero by4D3DB0; actualloaderinputunknown',
        '+10':'recordarraypointer; ctorzero; conditionalpostprovider store EAX then countcopyfrom+4 at46FA8C', '+14..+2C':'sevenDWORDs zeroedbyconditionalpostproviderstoreisland; semanticsnotqualified'},
      'record_fields':{'+0 DWORD':'initialized0 by46F8B0; recordcleanupifnonnull passesitto4F103C. ConditionalUIconsumerreadspointerforlabel/formatargs; title/stringownershipandencodingunknown',
        '+4 DWORD':'conditionalUIselectorcomparetozeroextendedword64ACCA; unqualifiedcategory/selector, notmapresourceID', '+8 BYTE':'recordconstructorzero; meaning/otherWORLDusesnotfollowed',
        '+0C..+1F':'unqualified inthisfiniteboundary', '+20 DWORD':'CCnewfullrow comparesagainstsignextendedreceivedmapWORD; UIselectoruseskey-500. Producerparse/valueconversion/actualkeymappingunknown'},
      'construct':'502070 diskptr46F870→46F880 ECX64AFD0→46F8D0→4D3DB0(64AFD4),thenrecordptr+10zero. Diskobject20byteszeroNOTactualruntimecount.',
      'registration_cleanup':'46F890 passes46F8A0 to4F10E0 (body/lifecycleunknown);46F8A0 ECX64AFD0→46F8F0. Ifrecordptrnonnull useshiddenheader[ptr-4]count,stride36,callback46F8C0→4F1362 then4F103C; embeddedcleanup4D3DC0 allocatorcalls. Noqualifieddestructionorproviderbody.',
      'cleanup_NOT_reset':'46F8F0 doesnotexplicitlyzeroobject+0 countor+10 pointer aftercalls; cannotusepostdestroytable asvalidempty/currentgeneration. Hiddenheadercountisnotnecessarilypubliccount; runtimeownershipunknown.',
      'loader_last_callers':'startup481F7D→4D4870; mapdirectcaller4D48D6 pushLTmapstr/ECX64AFD0→4D48E0 call46F9C0;46F9EE call4D4450 withembeddedobject64AFD4. Onlyattemptstaticcaller.',
      'first_boundaries':[{'VA':'004D4879','raw_destination':'05040E1D','kind':'private','unknown':'producerof636584/locale, return/effectnotread; ASMlabel05A40CBFdiffersraw'},
        {'VA':'004D4476','raw_destination':'773F5200','kind':'external','unknown':'formatting/name/return/bounds/providerload unknown; STOPatcall, no4D3E40/provider/parsercontinuation'},
        {'VA':'0046F895','raw_destination':'004F10E0','kind':'unexpanded registrationhelper','unknown':'callbackregistration/invocationorder notqualified'},
        {'VA':'0046F92D','raw_destination':'004F1362','kind':'unexpanded arraycallbackhelper','unknown':'cleanupinvocation/memory/hiddenheader validity unknown'}],
      'conditional_store_island':'46FA6F..46FA8D directstoresonly: +10=EAX,sevenDWORDclear,+0=embedded+4. Theseareafterunknownprovider/allocationpaths, NOT aqualifiedproducer/parser/tablegeneration; nofollow-uprecordparser orfullbuildercompletionclaimed.',
      'versions':'CC9EA8 lookupgate isunchangedBatch20. Qualifiedconstructor/loadcaller/prefix hasno63D54C gate. Runtime636584locale isseparatefrom63D54C and64B6DC; noactualversion/locale/epoch assumed.'}
    locale={'selector_VA':'00636584','raw_helper':'004D43D0 complete45, noexternalcalls','values':{'932':'j','936':'h','949':'k','950':'c','other':'e'},
      'name_arguments':'4D4450 signedextendsAL, preparesexternal773F5200 args stackbuf256,%s-%c.tbl,inputbasename,selectorchar; fixedbaseLTmapstr.',
      'actual_name_or_loaded_file':'unknownatfirstexternal/privateinput. Fourlocale.tbl+oneh.txt areexistingcandidatesonly; h.txt doesnotmatchformat.tbl. Noe.tbl bindingfromthisfinitecandidateinventory.',
      'not_codepage_authority':'Knownconstantslooklikecodes,but actualprivateprovider/API/currentsetting/localeupdate/epochnotqualified; do notinferGetACP/nameAPI or64B6DCequivalence.'}
    consumers={'selected_row':'4658B0 complete108 gatedUI68==0; selectedindexmustnonnegative. Calls46A540(lowWORDrow+4), thenattributeshelper, re-readselection/array, thenrow+74 AL→4687D0→preview64A98C+0F BYTE. NOreversekeylookup/mapresourceassignment. Callback/reentry andactualobjectunknown.',
      'preview_text_island':'4681AF..4681F5 readsBYTE+0F; CMP AL,FF followedSIGNEDJLE means80..FF goto4681F6 (version7794comparisonthenSTOP), 00..7F fallthrough conditionalrecordpointer+36*signedbyte at+0→preparedformatterargs. Noexplicittablecount/boundscheck inthisisland.',
      'preview_boundary':'Preparedargsjumpto4683F6 callEDI; loadedfrom4F7314,diskvalue773F5200 only, runtimeIAT/provider/module/formatterreturnsunknown. renderentry468020 runtime834BD4 lookup; errorpath46804F private05AD0986 STOP. Otherbranches/virtualrender/GUI notfollowed.',
      'separate_UIselector_island':'46B926..46B9D0 iteratesrecordstride36/count, compares+4 tozeroextendedWORD64ACCA, conditional+20 filters; takes+0 pointerandkey-500 into465110 UI64A968. STOPdirectcallee465110; mode/trigger/UIresource/runtimeunknown, nootherroomsettingsactionqualified.',
      'BYTE_interpretation':'NewCClookupindex>=128truncatedBYTE mayenterpreviewnegativecodebranch; >=256collideslow8. Conditionalstaticpredicateonly; noactualNativeversions/index/boundsoverrun claim.',
      'map_resourceID':'Noqualifiedimage/mapresourceconsumerfromtheseboundedchains. Labelpointer/UIcode/index/BYTE/rawWORD remainseparate; startupandrenderunknowns blockactualbinding.'}
    asset={'archive_manifest':record(ROOT/'resources/archive-manifest.json'),'candidates':candidates,'extraction_or_archive_replay':False,
      'binding':'Noactualproviderselection/tablematerialization/recordordinalbinding. Filefirstlinee.g.67 iscandidateheader, NOT qualified64AFD0count. Existingdecodedbinary remainsauthoritative; nopersonalcodepage/redecode.'}
    verification={'new_code_ranges':sum(r['kind']=='code'for r in ranges),'new_data_ranges':sum(r['kind']=='data'for r in ranges),'inherited_raw_ranges':3,
      'instruction_assertions':len(assertions),'prior_primary_pins':22,'existing_candidate_resources':5,'candidate_raw_decoded_pins':10,'all_match':True,
      'server_packet_unchanged':packet,'old_Batch20_PASS_replayed':False,'new_capture_frames':0}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},
      'table-object-record-source-boundary.json':sources_table,'locale-name-provider-boundary.json':locale,'row-BYTE-UI-consumer-boundary.json':consumers,
      'existing-map-resource-candidates.json':asset,'new-evidence-verification.json':verification}
    for n,o in outputs.items():write('map-table-source/'+n,o)
    primary={'schema':'lito.native-map-table-source-boundary.v1','updated_at':stamp(),'status':'FINITE_STATIC_MAP_TABLE_CALLER_AND_UI_BYTE_CONSUMER_SEALED_PROVIDER_RUNTIME_RESOURCE_BINDING_OPEN',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':{'Batch20':pin,'archive_manifest':asset['archive_manifest']},
      'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'table_source':sources_table,'locale_name':locale,'UI_consumers':consumers,'resource_candidates':asset,'verification':verification,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],'known_corroboration':'CCmaplookuprecord/sourcecaller/UIBYTEconsumer finite static islands andexistingresourcecandidatepins only',
        'Java_or_Modern_extra_wire_requested':False,'actual_map_resourceID_or_WORLDbinding':False},
      'scope':{'writes':'ownresourcefolderonly','Native_GUI_capture_SQL_provider_Java':False,'whole_UI_WORLD_or_private_body_expanded':False,
        'runtime_table_provider_name_locale_or_resource_qualified':False,'joined_physical_source_ownexit_authority':False,'overall_complete':False}}
    write('map-table-source-contract.json',primary);print(json.dumps({'primary':record(ROOT/'map-table-source-contract.json'),'verification':verification},ensure_ascii=False))
if __name__=='__main__':main()
