"""Batch8 bounded frame/coordinate source evidence; never executes client code."""
from audit import *
from extract_resources import decode_img

OUT=ROOT/'skill-pane-frame-coordinate'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);fixed=record(PE);assert fixed['sha256']==FIXED
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    lines=ASM.read_text(encoding='utf-8').splitlines()
    # Inventory only named direct stores. No arbitrary alias/private-code completeness claim.
    label=None;function=None;groups={};writerrefs=[]
    labelpat=re.compile(r'^(?:[A-Za-z_]\w*?_|locret_|loc_)([0-9A-F]{6,8})(?: proc|:)')
    for j,s in enumerate(lines):
        m=labelpat.match(s)
        if m:
            label={'va':m.group(1),'line':j+1,'name':s.split()[0]}
            if ' proc 'in s:function=label
        m=re.match(r'^mov     dword_(75A998|75A99C), (\w+)$',s)
        if m:
            item={'line':j+1,'text':s,'global_va':m.group(1),'register':m.group(2),'function':function,'nearest_label':label,
              'ASM_context':'\n'.join(lines[max(0,j-6):j+3])}
            writerrefs.append(item);groups.setdefault(int(label['va'],16),[]).append(item)
    ranges=[];instructions={};owners={}
    def capture(label,va,n,kind='code'):
        raw=pe.get_data(va-0x400000,n);decoded=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and (kind=='data'or sum(i.size for i in decoded)==n),(label,hex(va),n,sum(i.size for i in decoded))
        p=OUT/f'ranges/{va:08X}-{n:X}.bin';p.parent.mkdir(exist_ok=True);p.write_bytes(raw)
        row={**mapping(pe,va),'label':label,'kind':kind,'bytes':n,'output':record(p),'decoded_bytes':sum(i.size for i in decoded),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in decoded]}
        ranges.append(row)
        for i in decoded:instructions[i.address]=i;owners[i.address]=row['output']
        return row
    def w(va):
        i=instructions[va];return {'fixed_PE_sha256':FIXED,**mapping(pe,va),'raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str,'range':owners[va]}
    writerblocks=[]
    for va,refs in groups.items():
        candidate=list(cs.disasm(pe.get_data(va-0x400000,0x800),va));matched=[]
        for i in candidate:
            if i.mnemonic=='mov'and re.match(r'dword ptr \[0x75a99[8c]\],',i.op_str):
                ref=refs[len(matched)];op=f"dword ptr [0x{ref['global_va'].lower()}], {ref['register']}"
                assert i.op_str==op,(hex(va),i.op_str,op)
                matched.append(i)
                if len(matched)==len(refs):break
        assert len(matched)==len(refs),(hex(va),len(matched),len(refs))
        row=capture('translation_named_direct_writer_block',va,matched[-1].address+matched[-1].size-va)
        for ref,i in zip(refs,matched):ref['raw_witness']=w(i.address)
        writerblocks.append({'start':f'{va:08X}','function':refs[0]['function'],'writes':[w(i.address)for i in matched],'range':row['output']})
    specs=[
      ('IMG_table_initializer_and_direct_thunk',0x4D4930,0x9A),('IMG_atexit_registration',0x4D49D0,0xC),('IMG_table_destroy',0x4D49E0,0x56),
      ('IMG_entry_constructor',0x4D4D40,0x1C),('IMG_entry_loader',0x4D4D90,0x19E),('IMG_header_preserving_format_converter',0x4BB450,0x8A),
      ('pane_sizing_and_drag_region_constructor_tail',0x4CC611,0x191),('pane_input_handler',0x4CCD40,0x18F),
      ('IMG_baseSprite_handle_binding',0x42ECD3,0x1B),('IMG_base_handle_clone',0x42ED3B,0x1D),('IMG_native_LTsurf_binding',0x42EEEE,0x1B),
      ('IMG_native_overlay_before_shared',0x42EB0C,0x2B),('IMG_shard_handle_binding',0x42EDE8,0x16),
      ('registry_find_index',0x4EBB50,0x1B),('registry_raise_existing',0x4EBB70,0x52),('registry_toggle_or_clear',0x4EBBD0,0xD5),
      ('registry_direct_toggle_thunk',0x4EBEB0,5),('registry_direct_toggle_caller',0x4EBEC0,0x14),
      ('registry_teardown_zero',0x4EC8E4,0x2B),('input_driver_argument_prefix',0x46D8A0,0x37),('registry_input_dispatch',0x46DF76,0x2E),
      ('translation_zero_writer_entry_469CA0',0x469CA0,0x18),('translation_zero_writer_entry_4C22A0',0x4C22A0,0x31)]
    for name,va,n in specs:capture(name,va,n)
    # Data objects/pointers refer to this fixed snapshot only.
    for name,va,n in [('IMG_entry_vtable',0x4FA734,4),('IMG_CRT_initializer_slot',0x502194,4),('IMG_language_jump_table',0x4D4F30,20),
      ('IMG_format_strings',0x6365B0,0x28),('native_archive_name',0x505D94,11),('Sprite_archive_name',0x5055CC,7),
      ('IMG_table_pointer_snapshot',0x834BD4,4),('translation_current_saved_snapshot',0x75A998,16),('pane_registry_snapshot',0x836B54,16)]:capture(name,va,n,'data')
    expected=[(0x4D4940,'mov','ecx, 0x834bd4'),(0x4D496D,'push','0x7b08'),(0x4D497A,'mov','dword ptr [edi], eax'),
      (0x4D4996,'push','esi'),(0x4D4999,'call','0x4d4d40'),(0x4D49A5,'cmp','esi, 0x1ec2'),(0x4D49B2,'mov','dword ptr [ecx + esi*4 - 4], eax'),
      (0x4CC64A,'mov','ecx, dword ptr [eax + 0xe98]'),(0x4CC686,'movsx','edx, word ptr [ecx + 2]'),(0x4CC6D5,'movsx','ebx, word ptr [ebx]'),
      (0x4CC727,'mov','edi, dword ptr [eax + 0xe98]'),(0x4CC757,'movsx','ecx, word ptr [eax]'),(0x4CC75D,'sub','edi, ecx'),
      (0x4D4D99,'movsx','eax, byte ptr [0x64b6dc]'),(0x4D4DB4,'jmp','dword ptr [eax*4 + 0x4d4f30]'),
      (0x4D4E30,'mov','dword ptr [esi + 4], eax'),(0x4D4F0C,'mov','dword ptr [esi + 4], eax'),
      (0x42EEFC,'mov','dword ptr [0x6479a0], eax'),(0x42EB0C,'cmp','esi, dword ptr [0x64798c]'),
      (0x42EB1B,'mov','ecx, dword ptr [0x6479a0]'),(0x42EB23,'call','0x42ea50'),
      (0x4CCD5D,'mov','esi, dword ptr [eax + 0xe98]'),(0x4CCE86,'call','dword ptr [eax + 0x20]'),
      (0x4D4D48,'mov','dword ptr [eax], 0x4fa734'),(0x4D4D4E,'mov','dword ptr [eax + 4], 0'),(0x4D4D55,'mov','dword ptr [eax + 8], ecx'),
      (0x4CCE29,'call','0x4ebb70'),(0x4CCE3A,'call','0x4ebeb0'),
      (0x4CC660,'call','0x5ad0986'),(0x4CC6A2,'call','0x5ad0986'),(0x4CC73B,'call','0x5ad0986')]
    assertions=[]
    for va,mn,op in expected:
        i=instructions[va];assert(i.mnemonic,i.op_str)==(mn,op),(hex(va),i.mnemonic,i.op_str)
        assertions.append({**w(va),'match':True})
    assertions += [{**r['raw_witness'],'match':True,'ASM_expected':r['text']}for r in writerrefs]
    data=[]
    for r in ranges:
        if r['kind']=='data':
            b=Path(r['output']['path']).read_bytes();data.append({**mapping(pe,int(r['va'],16)),'label':r['label'],'bytes':len(b),'raw_hex':b.hex(),'all_zero':not any(b),'range':r['output']})
    assert int.from_bytes(pe.get_data(0xFA734,4),'little')==0x4D4D90
    assert int.from_bytes(pe.get_data(0x102194,4),'little')==0x4D4930
    assert pe.get_data(0x434BD4,4)==bytes(4)and pe.get_data(0x35A998,16)==bytes(16)
    assert pe.get_data(0x436B54,16)==bytes(16)
    assert pe.get_data(0x1055CC,7)==b'Sprite\0'
    assert pe.get_data(0x105D94,11)==b'LiTo\\LTSurf'
    assert pe.get_data(0x2365B0,0x28)==b'%d.img\0\0%de.img\0%dj.img\0%dh.img\0%dc.img\0'
    assert struct.unpack('<5I',pe.get_data(0xD4F30,20))==(0x4D4E40,0x4D4EA6,0x4D4DBB,0x4D4DF3,0x4D4DE8)
    private_error=mapping(pe,0x5AD0986);assert not private_error['raw_backed']
    write('skill-pane-frame-coordinate/ranges.json',{'fixed_PE':fixed,'ranges':ranges,'all_code_fully_decoded':True,'raw_bytes_authoritative':True})
    write('skill-pane-frame-coordinate/translation-direct-writers.json',{'ASM':record(ASM),'references':writerrefs,'blocks':writerblocks,'named_store_count':len(writerrefs),'block_count':len(writerblocks),
      'scope':'Visible named mov writers only, exact local raw blocks. Does not prove arbitrary alias stores/private code absent.',
      'classification':{'reset_paths':['469CA0','46B670','46DFB0 prefix','47F340 branches','482C60','496A00','4A9770'],
      'temporary_restore':'46DFB0 reads75A998/75A99C into savedlocals, writes0 atentry, then restores those savedglobals after draw. These arepreviousG,notincomingcoordinatearguments; nopermanentorigin0 imposed.',
      'branch_local_value_boundary':'4C22A0 directstore source isESI; itslocalwriterblock isqualified butfullupstreamregister/controlflow notclosed bythis finiteinventory.',
        'saved_origin_copies':['47E360','483400','4E2150 at4E2B7D and4E2E93 blocks'],
        'movement_deltas':'4E2150 reads currenttranslation and adds signed-indexed direction tuple from834C68/834CA8/834CE8/834D28/834D68, selected byobjectkind andflags. Boundary at source834EE4+11 signed direction; upstream direction/table writers not expanded.'}})
    # Reuse accepted batch6 raw evidence without rerunning layout/icon/packet tests.
    old=load('skill-pane-layout/ranges.json');inherited={r['label']:r for r in old['ranges']}
    hash_source=next(r for r in load('graphics-parity/ranges.json')['ranges']if r['label']=='shared_filename_shard')
    frame_draw=inherited['pane_render'];draw_refs=[i for i in frame_draw['instructions']if any(x in i['operands']for x in ['0xe94','0xe98','0xe9c','0x75a99'])or i['va']in ['004CCBA3','004CCC2C','004CCC96']]
    coordinate_refs={label:inherited[label]for label in ['skill_grid_render','pane_drag_translation','input_rectangle_gate','grid_hit_index','world_visible_pane_registry_draw']}
    extraction=load('skill-pane-frame-coordinate/frame934-extraction.json');rawpin=extraction['decoded'];raw=Path(rawpin['path']).read_bytes();assert record(Path(rawpin['path']))==rawpin
    width,height,flag,key=struct.unpack_from('<hhHH',raw);assert(width,height,flag,key)==(207,289,1,0x324C)and len(raw)==8+width*height*2
    image,details=decode_img(raw,True);pv=OUT/'frame934-source-preview.png';image.save(pv)
    header={'resource':rawpin,'width_signed':width,'height_signed':height,'flag':flag,'key_word':f'{key:04X}','exact_dense_pixel_bytes':len(raw)-8,
      'header_native_reads':[w(0x4CC686),w(0x4CC6D5),w(0x4CC757)],'preview':record(pv),'preview_policy':details,'native_GUI_oracle':False}
    write('skill-pane-frame-coordinate/frame934-header.json',header)
    modern=ROOT.parent/'lito重建版'/'LiTo381-Modern/src/modern_lito/world_skill_panel.rs';ms=modern.read_text(encoding='utf-8').splitlines()
    modernrefs=[{'line':i+1,'text':s}for i,s in enumerate(ms)if any(x in s for x in ['const FRAME_','const GRID_','const CELL_','const NUM:','const DEN:','const WIDTH:','const LEFT:','const TOP:','2057.img','SpriteShard(9)'])]
    comparison={'source':record(modern),'references':modernrefs,'native_main_frame_size_and_hit_resource':'index934→934.img;mainframe4CCC3B→4CCC96','native_additional_draw_resource':'index933→933.img atE94; no payload extracted inthisbatch',
      'Modern_frame':'sharedSprite09/2057.img;193x282','finding':'Modern2057 frame binding differs fromnative934 mainframe/sizing/hit and933 additionaldraw roles.934 ismainframe,butdoesnot replacealladditionalpane layers.',
      'native_coordinate_condition':'If extractednativeLTsurf934 is successfullyloaded anddrag/G=0: childorigin=(433,0),ownerrect=(470,43,607,248),gridorigin=(470,43). Additional933 draworigin=(441,10).',
      'Modern_policy':'800x600,5/4scale,frame193x282,inset27/34,step35;native codeuses640 rightalignment with934 width plus separate933 inset8/10 and34pxgrid.'}
    write('skill-pane-frame-coordinate/modern-comparison.json',comparison)
    # Symbolic coordinate examples only, no native input execution or old placement replay.
    cases=[]
    for drag,g in [([0,0],[0,0]),([5,-3],[12,7])]:
        b=640-width;f=[b+drag[0],drag[1]];owner=[f[0]+37,f[1]+43,f[0]+174,f[1]+248]
        cases.append({'qualified_resource':rawpin,'drag_delta':drag,'draw_translation':g,'childorigin':f,'owner_input_rectangle':owner,
          'pane934_input_bounds':[f[0],f[1],f[0]+width,f[1]+height],'main934_draw_origin':[f[0]+g[0],f[1]+g[1]],'grid_draw_origin':[owner[0]+g[0],owner[1]+g[1]],
          'additional933_draw_origin':[f[0]+g[0]+8,f[1]+g[1]+10],'input_point_if_upstream_removes_G':'screenpoint-G; conditional mapping only, upstream fullOS normalizationnotclosed',
          'native_GUI_oracle':False})
    write('skill-pane-frame-coordinate/coordinate-cases.json',{'status':'CONDITIONAL_CODE_DERIVED_ONLY','cases':cases})
    asmrefs=[{'line':i+1,'text':s}for i,s in enumerate(lines)if re.match(r'^mov     dword_(834BD4|75A998|75A99C|836B54|836B58|836B5C|836B60),',s)or re.match(r'^mov     \w+, offset dword_834BD4',s)]
    write('skill-pane-frame-coordinate/asm-reference-inventory.json',{'source':record(ASM),'role':'supportinglabels only','named_write_or_address_binding_references':asmrefs,
      'selected_excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}for a,b in [(444583,444723),(445182,445449),(427063,427125),(427906,428163),(494256,494449),(233797,233842),(234649,234673)]]})
    frozen={n:sha for n,sha in [('class4-slot-provenance.json','845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8'),('S1E-initialization-gates.json','841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC'),
      ('skill-gates/ranges.json','885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C'),('sealed-batch3/accepted-20261005/snapshot-manifest.json','140302D62027784D900DC628B550255F44E50F8D1275AA17B5321272C63C1DF0'),
      ('S1E-receive-selection-boundary.json','3B5357B9D193EC9A93F93611FE5C33AC8761F7BB9B6CBA584FD87D517114440E'),('native-icon-draw-contract.json','9BA690374734FECFE86CF95651E864515FFFDC78D71FE58C556974625AD95803'),
      ('skill-pane-layout-contract.json','45EBDED431F925A0F38066702E67F0AD79E9139939D793E201A96EEBD15E681F'),('skill-pane-render-initialization-contract.json','00262DDFF19C9392CFAEDDC53F38DE46725D99D0011A48F3FEF046C02754C1DE'),
      ('server-packet-delta.json','B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C')]}
    preserved=[{**record(ROOT/n),'expected_sha256':s,'match':record(ROOT/n)['sha256']==s}for n,s in frozen.items()];assert all(x['match']for x in preserved)
    verify={'fixed_PE':fixed,'raw_ranges':len(ranges),'code_ranges':sum(r['kind']=='code'for r in ranges),'data_ranges':len(data),'instruction_assertions':assertions,'data_snapshots':data,
      'range_bytes_and_pins_match':all(Path(r['output']['path']).read_bytes()==pe.get_data(int(r['rva'],16),r['bytes'])and record(Path(r['output']['path']))==r['output']for r in ranges),
      'prior_pins_preserved':preserved,'single_authorized_member_pin':rawpin,'new_wire_differences':0,'native_GUI_accepted':False};assert verify['range_bytes_and_pins_match']
    write('skill-pane-frame-coordinate/new-evidence-verification.json',verify)
    report={'schema':'lito.finite-skill-pane-frame-coordinate.v1','updated_at':stamp(),'status':'FINITE_STATIC_FRAME_COORDINATE_CONTRACT_COMPLETE_WITH_RUNTIME_BOUNDARIES',
      'source_PE':fixed,'source_ASM':record(ASM),'authority':'FixedPE rawbytes govern; ASM recoveredheap/global values are fromanother input and arenot livefixedPEdata.',
      'scope':'Directframe-table initializer/writes,necessaryIMG loader/namespace overlay,visibletranslation directwriterlocalblocks,andpane input/registry one-layer callers. No fullprogramreverse.',
      'evidence':[record(OUT/n)for n in ['ranges.json','translation-direct-writers.json','frame934-extraction.json','frame934-header.json','modern-comparison.json','coordinate-cases.json','asm-reference-inventory.json','new-evidence-verification.json']],
      'prior_primary_pins':preserved,
      'frame_table_834BD4':{'snapshot':'00000000','CRT_initializer_slot':'00502194→004D4930','direct_initializer_thunk':w(0x4D4940),'table_allocation_bytes':0x7B08,'entry_count':0x1EC2,
        'table_publication':w(0x4D497A),'entry_constructor_call':w(0x4D4999),'entry_publication':w(0x4D49B2),
        'entry_type':'12byteobject: +0 vtable4FA734 whosevirtual0=4D4D90 lazyloader; +4 cachedIMGpayloadpointer initially0; +8 numericID. Constructorstores loopID0..7873.',
        'entry_field_witnesses':[w(0x4D4D48),w(0x4D4D4E),w(0x4D4D55)],
        'member_E98':{'byte_offset':0xE98,'array_index':934,'resource_id':934,'resource_name_default':'934.img','usage':'Mainpane frame,size/rightalignment,drag geometry andpane inputbounds','witnesses':[w(0x4CC64A),w(0x4CC727),w(0x4CCD5D)],'main_draw_inherited':[i for i in draw_refs if i['va']in ['004CCC3B','004CCC96']],'inherited_range':frame_draw['output']},
        'member_E94_inherited':{'array_index':933,'resource_name_default':'933.img','usage':'Additionalpane draw atchildorigin+G+(8,10); conditional6328C0 branch','inherited_range':frame_draw['output'],'inherited_instructions':draw_refs,'payload_not_extracted':True},
        'member_E9C_scope_boundary':{'array_index':935,'resource_name_default':'935.img','last_visible':'004CCCBD reads table+E9C in inherited pane render','inherited_range':frame_draw['output'],'payload_not_extracted':True,'further_layer_role':'Outside this finite main-frame coordinate contract; no dimensions or rendering equivalence inferred.'},
        'lazy_load':'Caller checksobject+4==0 then invokesvirtual0. Loaderconstructsnamefromobject+8; storesallocated payload inobject+4; flag641298 nonzero calls4BB450. Converter leaves width/height unchanged.',
        'language_names':{'flag64B6DC':{'1':'934e.img','2':'934.img','3':'934c.img','4':'934j.img','5':'934h.img','default':'934.img'},
          'fallback':'Try variant hashedSpriteNN andbase handle; ifpayload remainsNULL, formatdefault934.img thenhashedSpriteNN/base. Alreadycachedpayload bypassesreloading.',
          'boundary':'Actual64B6DC/cachedentry/livefilesystem I/O unobserved. Existingsealedindex candidates containonlydefault934.img inLTsurf andSprite.'},
        'namespace_priority':'hash(name) selects6479E8 SpriteNN IMG-clone. Fallback64798C iscloneofbaseSprite.42EA50 recognizes64798C andtries6479A0 nativeLiTo\\LTSurf beforebaseSprite. OneLTsurf934memberextracted; sharedSprite934indexedonly.',
        'filename_shard_inherited':{'inherited_range':hash_source['output'],'formula':'sum of ASCII filename bytes & 15','default934_ascii_sum':sum(b'934.img'),'default934_shard':sum(b'934.img')&15,'default934_shard_name':'Sprite11','member_absent_from_existing_sealed_shard_indices':True,'language_variants_hash_separately':True},
        'namespace_witnesses':[w(0x42EEFC),w(0x42EB0C),w(0x42EB1B),w(0x42EB23)],
        'loader_internal_boundary':'42EA50 payloadI/O/cipher/API implementation notexpanded beyond neededoverlaybranch; postload livepointer andsuccess unobserved.',
        'destruction':'4D49E0 destroys/freesentries andtablethenzeroes834BD4;4D49D0 registersitscallback. No runtimeCRTreachabilityclaim.'},
      'qualified_single_resource':{'extraction':record(OUT/'frame934-extraction.json'),'header':header,'meaning':'Exactcandidatebytes207x289; ifthisnativeLTsurfmemberloads, alignmentB=433. Not an observedliveframewidth andnot everypane drawresource.'},
      'translation_75A998_75A99C':{'snapshot':[0,0],'direct_writer_inventory':record(OUT/'translation-direct-writers.json'),'named_store_count':len(writerrefs),'writer_blocks':len(writerblocks),
        'semantics':'Mutable global draw translation. Visible paths reset0, temporarily zero/restore saved previous globals, copy75A9A0/75A9A4, or add movement direction tuples. Live values cannot be assumed0. 4C22A0 source ESI remains conditional on its upstream control flow.',
        'draw':'4CD290 addsG toownerleft/top;main934 drawat4CCC96 useschildorigin+G;additional933draw addsG tochildcontainerorigin then(8,10). Dragcallback4CC330 changeschildorigin+ownerrectangle,doesnotwriteG.',
        'input':'4CCD40 usesincomingarg8/argC directlyagainstchildorigin..origin+934width/height, thenforwardsunchangedcoordinates tochildcontrols41B420 orownervirtual+20=4CD730. Neithercompletepanehandler noracceptedlistgate/hithelper subtractG.',
        'one_layer_dispatch':'46D8A0 acceptsmessageECX,wparamEDX,x=stackarg0,y=stackarg4;46DF76 registryloop forwardsmessage,wparam,x,y tovirtual+2C=4CCD40. FullOSnormalization/stabilitythroughotherbranchesnotclosed.',
        'consistency_condition':'ForG!=0,screen/inputmappingrequires upstream normalization orrender/input epoch policy. Thisreport providesonlyconditional screen=owner_input+G equation; doesnotassert currentOSinputalreadydoesit.',
        'inherited_ranges':[{label:r['output']}for label,r in coordinate_refs.items()]},
      'pane_registry_836B54_60':{'capacity':4,'ordering':'slot0 front;render visits3→0, input46DF76 visits0→3 stoppingonhandled.',
        'writers':'4EBBD0 withNULL callsvirtual+24 onallnonnullandzerosallslots;withexistingptr callsits+24,compactsandzeroslast;withnewptr callsoldlast+24,shift3←2←1←0,slot0=newptr,thennewvirtual+20.4EBB70 onlyraisesalreadyregisteredptrtofront.4EC8E4 teardownzerosfour.',
        'direct_callers':'4EBEB0 tailjmp,4EBEC0 directcall,andpane4CCD40 calls4EBB70 forin-bounds201/204/206;206 then4EBEB0toggle. Othernamedcallersinventoryonly,not recursivelyexpanded.',
        'pane_direct_call_witnesses':[w(0x4CCE29),w(0x4CCE3A)],
        'membership_boundary':'Live834884membershipnotobserved; construction/publication alone isnotregistrationproof.'},
      'Modern_differences':comparison,
      'unknowns':[{'last_visible':'004D4972 allocationcall→004D497A tablepublication;004CC727 dereference+E98;004CCD5D same','missing':'Initializedlive834BD4 table,entry934+4 payload,cachedIDstate,allocator/loadsuccess. Fixedsnapshotpointer0; no liveaddress/rangeinvented.'},
        {'last_visible':'004CCBA3 /004CCC2C usesentry933+4','missing':'933actualpayloadbytes/width/livevariant; only934memberextractionauthorizedandperformed. Name/typeclosed,indexlookupcanbehandedoff.'},
        {'last_visible':'004CC660/004CC6A2/004CC73B→private05AD0986 onresource-errorbranches','call_witnesses':[w(0x4CC660),w(0x4CC6A2),w(0x4CC73B)],'destination':private_error,'missing':'Private error helper body outside fixed PE; no attempt to fill it. Normal binding facts are conditional on successful resources.'},
        {'last_visible':'004E2150 directwritesfrom834EE4+11 anddirectiontuples;visiblecopyfrom75A9A0/A4','missing':'Dynamicactor/direction/savedoriginvaluesandtheirupstreamwriters;boundedbranchstopsatsourceoperands.'},
        {'last_visible':'0046DF76 registryvirtual+2C→004CCD40→owner+20/4CD730','missing':'FullOS/inputnormalization,realGepoch,coordinateevents/callstack;GUI/inputnotaccepted.'}],
      'new_wire_differences':0,'new_opcodes':0,'new_archive_members_extracted':1,'native_GUI_accepted':False,'overall_unpacking_accepted':False,
      'constraints':{'owned_directory_only':True,'native_observer_launch':False,'native_input_or_injection':False,'other_group_edits':False,'old_PASS_icon_packet_replay':False,'server_S0_policy_work':False}}
    write('skill-pane-frame-coordinate-contract.json',report)
    print(json.dumps({'report':record(ROOT/'skill-pane-frame-coordinate-contract.json'),'ranges':len(ranges),'assertions':len(assertions),'writer_blocks':len(writerblocks),'named_writes':len(writerrefs),'header':[width,height]},ensure_ascii=False))

if __name__=='__main__':main()
