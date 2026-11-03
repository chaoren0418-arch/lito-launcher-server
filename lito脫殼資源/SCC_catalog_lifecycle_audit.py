"""Batch20 finite catalog object mode/lifecycle and map projection. No runtime calls."""
from audit import *
OUT=ROOT/'SCC-catalog-lifecycle'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='E376B067F4E8BACD7F0DC01F2A615BE0E2B0B71C2FA3DC59E001EC0330D3F9B5'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]]
    assert [r['sha256']for r in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('SCC-catalog-contract.json');pin=record(ROOT/'SCC-catalog-contract.json');assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==21
    for r in prior:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,q):
        rows=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
        assert sum(len(bytes.fromhex(r['raw_hex']))for r in rows)==len(raw),(hex(va),'unaligned bounded input')
        for r in rows:at=int(r['va'],16);ins[at]=r;owners[at]=q
        return rows
    def reuse(index,va,n,kind='code'):
        data=load(index);row=next(r for r in data['ranges']if int(r['va'],16)==va);q=row['output'];raw=Path(q['path']).read_bytes()
        assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))and n<=len(raw)
        inherited.append({**mapping(pe,va),'index':record(ROOT/index),'range':q,'used_bytes':n,'kind':kind,'raw_matches_fixed_PE':True,'old_tests_replayed':False})
        if kind=='code':decode(raw[:n],va,q)
    for va,n,kind in [(0x4A7A30,1143,'code'),(0x465940,173,'code'),(0x463C10,120,'code'),(0x450260,35,'code'),(0x4A7EE0,12,'code'),
      (0x4A7EF0,80,'code'),(0x463D70,13,'code'),(0x465C50,55,'code'),(0x465C90,55,'code'),(0x465CD0,61,'code'),(0x4A7F40,24,'data'),
      (0x6343DC,8,'data'),(0x6343E4,8,'data')]:reuse('SCC-catalog/ranges.json',va,n,kind)
    reuse('skill-pane-layout/ranges.json',0x450230,48)
    reuse('SCE-navigation/ranges.json',0x46A520,30)
    reuse('skill-pane-frame-coordinate/ranges.json',0x469CA0,24)
    reuse('SCE-navigation/ranges.json',0x469CB8,17)
    reuse('SD0-receive/ranges.json',0x469CC9,506)
    reuse('SCE-navigation/ranges.json',0x46A244,20,'data')
    specs=[(0x465610,112,'code','complete_catalog_UI_constructor_including_tail'),(0x450090,165,'code','complete_list_base_constructor_including_allocator_return_tail'),
      (0x46BA10,98,'code','catalog_reuse_gate_to_first_catalog_clear_call_only'),(0x46BC88,95,'code','catalog_creation_all_args_and_global_publication'),
      (0x465680,33,'code','complete_catalog_scalar_deleting_destructor'),(0x4656B0,78,'code','complete_catalog_destructor_to_base_boundary'),
      (0x465D10,18,'code','catalog_filter_mask_setter_UI6C_NOT_UI68'),(0x452C20,120,'code','complete_direct_list_request_caller_to_tail_jump'),
      (0x451514,51,'code','aligned_direct_command_caller_predicate_to_return_only'),(0x4A7850,252,'code','C9_result_header_case0_to_lifecycle_call_return_only'),
      (0x4B309F,12,'code','C9_shared_dispatch_call'),(0x4F8CB0,92,'data','catalog_disk_vtable_NOT_runtime_identity'),
      (0x4B3539,1,'data','C9_primary_map'),(0x4B33CC,4,'data','C9_primary_jump'),(0x4B377D,1,'data','C9_fallback_map'),(0x4B361C,4,'data','C9_fallback_jump'),
      (0x4A7998,20,'data','C9_result_branch_targets'),(0x4A79AC,11,'data','C9_result0_slot_map'),(0x502778,3,'data','existing_cc_header_format')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x465649,'mov','ecx, dword ptr [ebp + 0x20]'),(0x46565B,'mov','dword ptr [esi + 0x68], ecx'),(0x465641,'call','0x450090'),
      (0x4500CD,'call','0x4c2fc0'),(0x4500F4,'mov','dword ptr [esi + 0x2c], eax'),(0x450110,'mov','dword ptr [esi + 0x5c], 0x100'),
      (0x4500E0,'push','0x400'),(0x450117,'call','0x4f118c'),(0x450122,'mov','dword ptr [esi + 0x58], eax'),
      (0x46BA31,'test','eax, eax'),(0x46BA47,'je','0x46bc88'),(0x46BA54,'je','0x46bc88'),(0x46BA61,'je','0x46bc88'),
      (0x46BA67,'mov','ecx, dword ptr [0x64a958]'),(0x46BA6D,'call','0x450230'),(0x46BCA5,'push','0'),(0x46BCA0,'push','0xffff'),
      (0x46BCD1,'call','0x465610'),(0x46BCE2,'mov','dword ptr [0x64a958], eax'),(0x4656DC,'call','0x450230'),(0x4656EA,'call','0x450170'),
      (0x465686,'call','0x4656b0'),(0x465692,'call','0x4f103c'),(0x465D16,'mov','dword ptr [ecx + 0x6c], eax'),
      (0x45024C,'call','dword ptr [edx]'),(0x450256,'mov','dword ptr [esi + 0x2c], ebx'),(0x450259,'mov','dword ptr [esi + 0x28], ebx'),
      (0x46A520,'mov','ecx, dword ptr [0x64a958]'),(0x46A526,'call','0x450230'),(0x46A52B,'push','0xce'),(0x46A535,'call','0x4c6ae0'),
      (0x469CAE,'jne','0x469cb8'),(0x469CB1,'mov','dword ptr [0x64af98], eax'),(0x469E9D,'call','0x46a520'),
      (0x452C8C,'call','0x46a520'),(0x451529,'cmp','dword ptr [0x64af98], 1'),(0x45153C,'call','0x452c20'),
      (0x4B30A1,'call','0x4a7850'),(0x4A7878,'movsx','ecx, byte ptr [ebp - 1]'),(0x4A7882,'ja','0x4a7993'),
      (0x4A7890,'jmp','dword ptr [edx*4 + 0x4a7998]'),(0x4A7941,'call','0x46ba10'),
      (0x4A7AC4,'movsx','esi, cx'),(0x4A7AD0,'cmp','dword ptr [ecx], esi'),(0x4A7ADE,'mov','edi, eax'),(0x4A7AFE,'push','edi'),
      (0x4659A6,'mov','edx, dword ptr [ebp + 0x20]'),(0x4659AD,'push','edx'),(0x4659AA,'mov','ecx, dword ptr [ebp + 0x1c]'),(0x4659B1,'push','ecx'),
      (0x463C47,'mov','dl, byte ptr [ebp + 0x20]'),(0x463C50,'mov','byte ptr [esi + 0x74], dl'),
      (0x4A7B45,'sub','cl, dl'),(0x4A7B4F,'dec','cl'),(0x4A7BBF,'add','dl, 0xc'),(0x4A7BE7,'mov','dl, 0xfe'),(0x4A7BEA,'sub','dl, bl'),
      (0x4A7E40,'add','al, 0xc'),(0x4A7E59,'sub','dl, bl'),(0x4A7E6B,'call','0x465c50'),(0x465C73,'call','0x463d70'),
      (0x4A7E84,'and','esi, 0xff'),(0x4A7E8A,'call','0x4a7ef0'),(0x4A7E95,'or','esi, eax'),(0x4A7E9C,'call','0x465c90')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at))
        assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    for va,b,jva in [(0x4B3539,0x71,0x4B33CC),(0x4B377D,0x2B,0x4B361C)]:assert pe.get_data(va-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==0x4B309F
    assert pe.get_data(0xA79AC,1)==b'\0'and int.from_bytes(pe.get_data(0xA7998,4),'little')==0x4A7897 and pe.get_data(0x102778,3)==b'cc\0'
    constructor={'entry':w(0x46565B),'argument':'7th u32 argument at EBP+20 copied to UI+68; 8th EBP+24 copied to UI+6C filter mask, different field',
      'creation':'46BC88..46BCE6 allocates0x70 and conditionally calls465610 with args [21h,38h,17Ch,150h,0Ch,0Ch,0,FFFFh]; 46BCA5 push0 is seventh arg. NULL skips constructor, publishesNULL at64A958.',
      'base':'450090 complete165 initializes count+2C/selection+28=0, capacity+5C=256, allocates1024 bytes pointerarray+58; allocator/4C2FC0 execution and success remainunknown.',
      'mode':'New object alongthis static caller hasUI+68=0 AFTERconstructorreturns; noqualifiedsetter/nonzero pathfor64A958 found in these finite directmethods/callers. Notproofofglobalimmutability/runtimeobjectidentity.',
      'different_field':'465D10 updatesUI+6C filtermask then4509A0. It doesnot setUI+68 orclearcatalog. Nofilter/action expansion.',
      'mode_writes_audited_scope':['465610 complete112','450090 complete165','465680 complete33','4656B0 complete78','465D10 complete18','46BA10prefix98','46BC88creation95','452C20complete120','C9case0prefix252','Batch19 CC/directrowhelpers fixedraw'],
      'missing_input':['actual64A958 object/lifetime/generation','runtimeorvirtualaliases/heapmutation','successofbaseprovider/allocations','othercallerorprivatewrites to+68 outsidefinitewitnessscope'],
      'vtable':'disk4F8CB0 has23 slots, first465680; constructorwrites thispointer. Noactualruntimevtable/dispatchproof; inheritedmethodbodies notexpanded.'}
    lifecycle={'clear':'450230 iterates count+2C andarray+58, skipsNULL otherwise virtualslot0(1); afterloop setscount+2C=0 andselection+28=0. DoesnotzeroUI+68, changearray+58/capacity+5C, publish64A958, orclearseparate64A98C previewWORD. Callbackmayreenter/mutatecount; completionrequirescallbackreturn.',
      'paths':[{'path':'46A520→450230→C_CEwriter','trigger':'direct46A520 call: read64A958 noNULLguard, clearthenattemptC_CE206 withc format. Actualsend/network/writecommitunknown.',
        'direct_callers':['469E9D scene1 nonzero62C21C branch, afteropaqueUI/privatepresentationcallbacks','452C8C directhelper, afterotherknownwriter/previewcalls','Batch14 sealedCEfailure calls only priorreference; notrereviewed'],
        'source_callers':['Batch16 ownD0→469CA0(1) priorstaticreference; selfalias/currentcontextunknown','451514..451546 predicate externalcomparatorcallEDI==0 andphase64AF98==1→452C20; commandproducer/token/externalfunction actualinputunknown'],
        'bypass':'469CA0 62C21C==0 onlywritesphase64AF98 andreturns, no46A520/clear. Neednonzero andscene1dispatch plusallprecedingcallsreturn; cannotinferclearfromphase1 alone.'},
        {'path':'C9 result0→4A7941→46BA10','trigger':'bothdispatchmaps→4B309F→4A7850, firstheaderc decodedsigned0 selects4A7897; resultheaderparse/cleanupcallbacks mustreturn. NoC9wire/schema extension oractualNativeentryacceptance.',
        'reuse_gate':'64A990!=0 AND64A98C!=0 AND64A930!=0 then46BA67 read64A958,46BA6D call450230; 64A958 itselfisnotNULLchecked. OtherUIbody notexpanded.',
        'create_gate':'anyofthose3isNULL→46BC88 allocation/create →publish64A958 at46BCE2. Seventharg0; old64A958 disposal/ownershipandotherUIinitializercontinuation notqualified.',
        'boundary':'Onlyprefixfirstcatalogclear orcatalogcreation/globalpublication, notentire46BA10 initializer orlaterotherUI/WORLD. Lastprovedcalls4A7941/46BA6D/46BCD1; actualobjects, branchstate, send/epoch/order missing.'},
        {'path':'diskvtablefirst465680→4656B0→450230→450170','trigger':'scalar destructor arglowbit1 additionallycalls4F103C; actualinvocation/destruction/lifetime notproved. Base450170body notexpanded.',
        'boundary':'destructorcleardoesnotqualifya usablepostdestroycatalog orresetresponse.'}],
      'row_outcomes':{'unchangedexistingID':'UI68==0 andmatchingfullDWORDrow present→ignoreCC0; nofieldreplace',
        'prior_clear_completed':'count0 meansnoexistingrowinactivescan; nextCC0 attemptsappend ifcapacityandallocationpermit. SameIDmaybeadded again, notreplacement.',
        'new_constructor_completed':'count0,capacity256,arrayallocattempt,UI68=0; initialCC0 attemptsappend; NULL/objectexecutionunknown.',
        'CF_delete_completed':'priorBatch18/19 fallbackdelete removesmatchingDWORDrows; a laterCC0 canattemptappend. CFprimaryprivatepath stillunknown; noCFprerequisiteproposal.',
        'UI68_nonzero':'skipsdedup andattemptsduplicateappendwhilecount<256/capacity; noqualifiedproducerforcatalognonzero inthisfinitechain',
        'no_clear_marker':'CC0payload hasnoclear/rebuild/listcomplete/resetbit; receivingalone doesnotinvoke450230 orswitchUI68'},
      'reset_implication':'Nativefullrowresetacceptance maydependonpriorcleared/new/absentrow state. SuppliedJavareset isknownwire; noautomaticrequestforCF/partial/CCextra, noGUIrootcauseorModernreplacementauthority.'}
    mapflow={'row_layout':'receivedmapWORD andUIrow+74 BYTE aredifferentrepresentations. Rowadd4659A6 loadsarg7 DWORD EBP+20→4659AD pushesEDX toconstructorarg7; constructor463C47 readsONLYbyte EBP+20→463C50 DLstore. Upperarg24bitsdiscarded. Adjacent4659AA/4659B1 forwardarg6 attributes, notmap.',
      'full0_new_version':{'condition':'signed63D54C>=9EA8; mapWORDnotFFFF/FF9C','projection':'signextend16 map toESI; scan64AFD0 signedcount/64AFE0 stride36 keyDWORD+20; EDIstarts0, firstmatchingindex→EDI; absent/count<=0 remains0. PushfullEDI thenrow+74 keepsEDI&FF, indices256apartcollide.',
        'special':'FFFF/FF9C: low8(map)-u8mode-1 modulo256 (4A7B3F/45/4F), NOT FE-modeforFF9C. UpperECX argbitsnotinitializedasnewwire; byteconstructordiscardsthem.',
        'flags':'4A7EE0 attrsbit2→2 else1 OR4A7EF0(mode). Validmode0..5→2000/200/400/1000/800/100hex; invalidsignednegative/>5 uninitializedstackreturn remainsunknown.'},
      'full0_old_version':{'condition':'signed63D54C<9EA8','ordinary':'low8(map)+0C modulo256','special':'FFFF/FF9C andsignedversion>=7794→FE-u8mode modulo256; below7794→FF; rawmap specialcodescollapseonthisoldbranch'},
      'partial5':{'condition':'subtype5 dhc independentof9EA8 ordinarylookup threshold','ordinary':'low8(map)+0C modulo256 (ADD AL,0C); no64AFD0/64AFE0 lookup inthisbranch','special':'FFFF/FF9C andsignedversion>=7794→FE-u8mode modulo256, otherwiseFF',
        'store':'465C50 scansallmatchingfullDWORDIDs, calls463D70 byteargto+74. Absentnoappend; upperstack/registerbitsdiscarded.',
        'flags':'465CD0 firstmatchingflags+78 (absentreturnsinputroomID) →ESI&FF ORmodehelper→465C90 writesallmatchingrows. Preservesonlylow8, replacesallhigherbits; duplicate rowsreceivefirstrowlowflags. Absentflagssetterhasnomatchingrow.'},
      'illustrative_arithmetic_not_runtime':[{'case':'ordinaryWORD0101 olderfull0/partial5','BYTE_hex':'0D','reason':'(01+0C)&FF'},
        {'case':'newfull0 lookupindex256','BYTE_hex':'00','reason':'256&FF; mapgeneration/resourceunknown'},
        {'case':'FFFF mode1 newfull0/oldfull0/partial5','BYTE_hex':['FD','FD','FD']},
        {'case':'FF9C mode1 newfull0 vsoldfull0/partial5','BYTE_hex':['9A','FD','FD'],'reason':'9C-1-1 versusFE-1'},
        {'case':'sealednewfull0 WORD0000 mode1','BYTE':'lookupindex&FF ordefault0','flags_hex':'00000201','qualification':'hypotheticalvalidattrs0/mode1 staticprojectiononly; existingrowignore/storage/runtimeunproved. Partial5 map0wouldproduce0C.'}],
      'not_qualified':['actual64AFD0/64AFE0 tablecontents/generation','mapresourceID orWORLD binding','selectedpreview/UIrender/use ofrowBYTE','Nativeversion/rowwriteexecuted','stableinvalidmodeflags','upperregisterpaddingaspacketfields']}
    verification={'new_code_ranges':11,'new_data_ranges':8,'inherited_raw_ranges':len(inherited),'instruction_assertions':len(assertions),'prior_primary_pins':21,
      'complete_catalog_constructor_bytes':112,'complete_base_constructor_bytes':165,'creation_slice_bytes':95,'lifecycle_prefix_bytes':98,'all_match':True,
      'server_packet_unchanged':packet,'new_capture_frames':0,'old_tests_replayed':False}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},
      'UI68-constructor-and-mode-source.json':constructor,'catalog-clear-rebuild-lifecycle.json':lifecycle,'map-WORD-BYTE-modeflags-dataflow.json':mapflow,
      'new-evidence-verification.json':verification}
    for n,o in outputs.items():write('SCC-catalog-lifecycle/'+n,o)
    primary={'schema':'lito.native-SCC-catalog-lifecycle.v1','updated_at':stamp(),'status':'FINITE_STATIC_UI68_CONSTRUCTOR_CLEAR_AND_MAP_PROJECTION_SEALED_RUNTIME_GUARD_RETAINED',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':{'Batch19':pin,'dispatch_navigation':record(LEGACY/'analysis/NATIVE-S-DISPATCH.json')},
      'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'constructor_mode':constructor,'catalog_lifecycle':lifecycle,'map_projection':mapflow,'verification':verification,
      'delta':{'new_wire':[],'new_opcodes':[],'wire_corrections':[],'known_internal_boundary':'UI68arg7zero constructor, guardedclear/newobject/C9result0/scene1/directcaller, preciseword/index/byteandmodeflagprojection',
        'Java_additional_CF_CC_partial_requested':False,'Native_GUI_root_cause_claimed':False},
      'scope':{'writes':'ownresourcefolderonly','Native_or_GUI_input':False,'SQL_provider_newcapture':False,'Java_taken_over':False,
        'all_UI_or_WORLD_or_private_protection_scanned':False,'actual_dynamic_object_virtual_lifetime_authority':False,'joined_source_physical_ownexit_authority':False,'overall_complete':False}}
    write('SCC-catalog-lifecycle-contract.json',primary);print(json.dumps({'primary':record(ROOT/'SCC-catalog-lifecycle-contract.json'),'verification':verification},ensure_ascii=False))
if __name__=='__main__':main()
