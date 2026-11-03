"""Finite static audit of class4 gaps and S1E readiness, no process operations."""
from audit import *
from seal_delta import seal
from collections import defaultdict

def load(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
    baseline=load('baseline.json')['sources'];assert record(PE)['sha256']==baseline[1]['sha256']
    pe=pefile.PE(str(PE));packed=pefile.PE(str(BIN));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    folder=ROOT/'skill-gates';folder.mkdir(exist_ok=True)
    for name in ['VA-00634A30.bin','VA-00492D4A.bin','VA-00492EE0.bin']:
        stale=folder/name
        if stale.exists():assert stale.resolve().is_relative_to(ROOT);stale.unlink()
    specs=[(0x43F6C0,0x220,'preconditions and class-order producers'),(0x492ACF,0x15D,'class/order publication and existing-owner reset gates'),(0x492D3A,0x94,'new owner/pane construction and publication; known instruction boundary'),(0x492EDA,0x2D,'selection format/send after owner construction; known instruction boundary'),(0x4AEE70,0xB5,'S1E complete bounded handler'),(0x4C9220,0x40,'S1E owner duplicate/capacity gate'),(0x4CD050,0xCA,'class-order matching replacement throughret'),(0x4C9190,0x42,'owner constructor including IDA tail'),(0x4CCED0,0xE2,'placeholder constructor including IDA tail'),(0x450090,0xAA,'capacity/table constructor including IDA tail'),(0x4CD1C0,0xA6,'full placeholder rebuild'),(0x4CD120,0xA0,'single-index placeholder replacement'),(0x4AEF30,0x7A,'S1F bitmap removal'),(0x4AA8B0,5,'SDA unconditional reset jump'),(0x46A260,0x35,'SDA owner-reset call context'),(0x4B29DB,0x38,'primary mode/phase dispatch gate'),(0x4B313F,0x1E,'fallback dispatch selector'),(0x4B2AD3,0x18,'S1E/S1F shared cases'),(0x4B3260,8,'primary S1E/S1F raw jump entries'),(0x4B35B4,8,'fallback S1E/S1F raw jump entries'),(0x4B348E,2,'primary S1E/S1F opcode map bytes'),(0x4B36D2,2,'fallback S1E/S1F opcode map bytes'),(0x834548,0xA0,'class4 forty DWORD snapshot'),(0x635B80,8,'current-order default pointer snapshot'),(0x75F694,1,'class byte snapshot'),(0x634580,14,'unknown spell literal from actual4AEEEC push')]
    ranges=[]
    for va,n,label in specs:
        raw=pe.get_data(va-0x400000,n);path=folder/f'VA-{va:08X}.bin';path.write_bytes(raw)
        ranges.append({**mapping(pe,va),'purpose':label,'bytes':len(raw),'output':record(path),'instructions':[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]if va<0x500000 and va not in [0x4B3260,0x4B35B4,0x4B348E,0x4B36D2] else [],'bounded_static_not_execution':True})
    write('skill-gates/ranges.json',{'source_PE':baseline[1],'generator':record(Path(__file__)),'ranges':ranges})
    # Known literal tables and loops are copied from the pinned raw producer instructions.
    producers={}
    for start,count,src,loop in [(0,16,0x508648,0x43F789),(16,4,0x508708,0x43F7A5),(24,4,0x508710,0x43F7C1),(32,4,0x508718,0x43F7DD)]:
        for i in range(count):producers[start+i]={'source_word_VA':src+2*i,'producer_loop_VA':f'{loop:08X}','kind':'signedWORD-to-DWORD loop'}
    for slot,src in [(20,0x508720),(28,0x508722),(36,0x508724),(31,0x50867A)]:producers[slot]={'source_word_VA':src,'producer_block_VA':'0043F7E9..0043F821','kind':'signedWORD-to-DWORD explicit store'}
    overrides={8:392,9:393,10:394,13:755,14:651}
    rows=[]
    for slot in range(40):
        va=0x834548+4*slot;raw=pe.get_data(va-0x400000,4)
        item={'slot':slot,**mapping(pe,va),'snapshot_bytes':raw.hex(),'snapshot_u32':struct.unpack('<I',raw)[0],'snapshot_bytes_sha256':sha(raw),'producer':producers.get(slot),'postselect_override':overrides.get(slot),'conditional_native_value':None,'runtime_value_proven':False}
        if slot in producers:
            src=producers[slot]['source_word_VA'];word=pe.get_data(src-0x400000,2);value=struct.unpack('<h',word)[0]
            item['producer']={**producers[slot],'source':mapping(pe,src),'source_bytes':word.hex(),'source_bytes_sha256':sha(word),'source_signed16':value}
            item['conditional_native_value']=overrides.get(slot,value)
        else:item['value_qualification']='Fixed OEP snapshot and ASM storage declaration zero; no explicit producer in bounded class4 init/selection. Post-init/runtime inherited value unknown; not synthesized as zero.'
        rows.append(item)
    holes=[x['slot']for x in rows if x['producer']is None];assert holes==[21,22,23,29,30,37,38,39]
    data=PE.read_bytes();literal_hits=[]
    for slot in holes:
        needle=struct.pack('<I',0x834548+4*slot);at=0;positions=[]
        while True:
            found=data.find(needle,at)
            if found<0:break
            positions.append(f'{found:08X}');at=found+1
        literal_hits.append({'slot':slot,'exact_address_literal_file_offsets':positions,'negative_scope':'Whole fixed PE exact u32 literal scan only; pointer arithmetic/aliases/private code are not excluded.'})
    lines=ASM.read_text(encoding='utf-8',errors='replace').splitlines();excerpts=[]
    for first,last,label in [(135655,135825,'class4 table producer and neighboring loop bounds'),(565024,565050,'static zero declarations and slot gaps'),(309248,309333,'class publication, common overrides and reuse reset'),(364990,365098,'S1E metadata fallback and owner call'),(420783,420834,'index-only duplicate and count gate'),(428394,428518,'matching all class-order slots, no scene/pointer guard'),(428628,428728,'rebuild creates -1/-1 placeholders independent of class-order values'),(372377,372391,'primary mode/phase dispatch'),(374425,374435,'fallback dispatch')]:
        p=folder/f'ASM-{first}-{last}.txt';p.write_text('\n'.join(f'{j+1}: {lines[j]}'for j in range(first-1,last))+'\n',encoding='utf-8');excerpts.append({'source':baseline[2],'line_start':first,'line_end':last,'purpose':label,'output':record(p)})
    exact_refs=[]
    pattern=re.compile(r'83459C|8345A0|8345A4|8345BC|8345C0|8345DC|8345E0|8345E4|834548|8345E8|635B80',re.I)
    for j,line in enumerate(lines):
        if pattern.search(line):exact_refs.append({'line':j+1,'text':line})
    write('skill-gates/asm-evidence.json',{'source_ASM':baseline[2],'excerpts':excerpts,'literal_references':exact_refs,'different_ASM_input_PE':True,'raw_fixed_PE_authoritative':True})
    original_map=mapping(packed,0x834548);section=next((s for s in packed.sections if s.VirtualAddress<=0x434548<s.VirtualAddress+max(s.Misc_VirtualSize,s.SizeOfRawData)),None)
    storage={'fixed_snapshot':mapping(pe,0x834548),'original_BIN':original_map,'original_section':{'name':section.Name.rstrip(b'\0').decode('ascii','replace'),'virtual_size':section.Misc_VirtualSize,'raw_size':section.SizeOfRawData,'characteristics_hex':f'{section.Characteristics:08X}','zero_fill_tail_candidate':0x434548-section.VirtualAddress>=section.SizeOfRawData}if section else None,'interpretation':'Raw-backed expanded OEP snapshot has zero bytes. If original packed PE address is in an unbacked virtual tail, loader-zero is an initial storage candidate; protected unpacking/indirect writes before skill init remain unclosed. No runtime zeros are asserted.'}
    meta=load('skill-initialization/LTSpellDesc-c-indexed.json');metadata_ids={x['index']for x in meta['rows']};matches=[]
    for ident in sorted(metadata_ids):
        raw=pe.get_data(0x508648-0x400000+ident*2,2);icon=struct.unpack('<h',raw)[0];targets=[x['slot']for x in rows if x['conditional_native_value']==icon]
        matches.append({'skill_index':ident,'icon_word_signed':icon,'known_class4_matching_slots':targets,'unclosed_slots_may_match_only_if_actual_value_equals_icon':holes})
    class_report={'updated_at':stamp(),'source_BIN':baseline[0],'source_PE':baseline[1],'source_ASM':baseline[2],'evidence_ranges':record(ROOT/'skill-gates/ranges.json'),'storage':storage,'slots':rows,'closed_explicit_producer_slots':32,'unclosed_runtime_value_slots':holes,'exact_literal_scan':literal_hits,'producer_preconditions':['43F6C0 opens list.til; pointer nonzero and count>0','43DE10 returns AL nonzero, then class array write block is reached','Class4 copy loops stop before holes; adjacent default80-word loop ends834548 exclusive; class2 starts8345E8 exclusive of class4 range','4928C0 reaches class4 branch after selected record validation; stores class75F694, order635B80=834548, then common overrides8/9/10/13/14','Full43F6C0 success is stronger than table-block reachability; later icon loading failure does not undo already written tables'],'minimal_unknown':'Actual eight DWORD values after native table block/selection in the same owner epoch, and any alias/private/protected writer that establishes or overrides them. Storage zeros alone do not close runtime producer provenance.','conditional_metadata_to_known_class4_slots':matches,'resource_rows_do_not_prove_filled_owner':True,'patched_or_filled_skills':False}
    write('class4-slot-provenance.json',class_report)
    legacy=Path('D:/lito-dev-runtime/20261003-supervision/launcher/observability-20261003-225704/ui-native-hookup-20261004-051000')
    timeline=json.loads((legacy/'t69_skill_evidence/V9-S1E-timeline.json').read_text(encoding='utf-8'))
    dispatch=[]
    for label,map_va,jump_va in [('mode4_phase>=3',0x4B3470,0x4B3208),('fallback',0x4B36B4,0x4B3570)]:
        idx=pe.get_data(map_va-0x400000+0x1E,1)[0];entry=pe.get_data(jump_va-0x400000+idx*4,4);target=struct.unpack('<I',entry)[0];assert target==0x4B2AD3
        dispatch.append({'branch':label,'opcode_map':mapping(pe,map_va+0x1E),'map_byte':f'{idx:02X}','jump_entry':mapping(pe,jump_va+idx*4),'jump_bytes':entry.hex(),'target_VA':f'{target:08X}'})
    gates={'updated_at':stamp(),'source_PE':baseline[1],'source_ASM':baseline[2],'ranges':record(ROOT/'skill-gates/ranges.json'),'class4_provenance':record(ROOT/'class4-slot-provenance.json'),'dispatch':dispatch,
      'finding':'Both audited dispatch branches admit S1E, with no explicit first-lobby/owner-readiness gate or queue in this hierarchy. Safe population depends on a live consistent owner/table and current selected class/order. Metadata absence is fallback, not a local drop. Actual upstream delivery timing is not established.',
      'stages':[
        {'stage':'dispatch','VA':'004B29DB /004B313F ->004B2AD3','condition':'mode64BBD4==4 and phase64AF98>=3 choosesprimary; other cases choosefallback; both raw maps route1E to samehandler','effect':'No lobby/owner/class gating for this opcode in either map'},
        {'stage':'bitmap','VA':'004AEE70..004AEEB5','condition':'unsigned byte countN>0, each set bit amongN bytes LSB-first','effect':'index=group*8+bit; unset bits/count0 do not clear skills'},
        {'stage':'metadata','VA':'004AEEC7..004AEEF3','condition':'647E24 pointer nonzero, index<647E20 count, indexed stringpointer nonzero','effect':'Use signed WORD508648[index] and metadata text; otherwise still calls nextstage with icon91 and unknownspell. Metadata147rows are not147populated skill rows.'},
        {'stage':'owner','VA':'004AEEF3..004C9235','condition':'834888 must point to live owner; handler does not testit;4C9220 immediately reads[ECX+2C] and later[ECX+58]','effect':'An early S1E is not buffered/delayed here. Invalid owner can dereference invalid memory; no specific historical crash asserted.'},
        {'stage':'dedup/capacity','VA':'004C9235..004C925B','condition':'No existing row hassame index and owner count<256','effect':'Same index is ignored even ifnewicon/text differs; placeholders index-1 do not match nonnegativeindex'},
        {'stage':'class-order replacement','VA':'004CD050..004CD107','condition':'75F694 determines count:class2=99,class0=56,class4=40,default80;635B80 pointer and owner+58 table mustbe valid/consistent; signed icon equalsorder[slot]','effect':'Replace every matching slot with4C8DE0(index,icon,text), destructoroldrow andnewrowpointer publish. No append, no scene/metadata/owner validity guard, no earlybreak; no match means no replacement.'}],
      'normal_selection_order':['492AD4 storesselectedclass;492B9F publishesclass4order;492AF4 commonoverride block','New path492D80->4C9190->4CCED0->450090 initializescapacity256/table and class-count placeholders, then publishesowner492D91/pane492DC6','Existing path492B61->4CD1C0 reconstructs placeholders; prerequisites648EBC/834888/834958 all nonzero','C_C9 producer492F02 is later in ordinary4928C0 callback than these initialization paths. Completion/order in actual session or modified bridge remains unproved'],
      'before_after_lobby':[{'position':'Before native selectedclass/owner initialization','static_result':'Dispatch admits1E; validity ofglobalowner/class/order is not protected here; no pending bitmap/replay buffer found in these bodies'},{'position':'After successful selection but before first lobby view','static_result':'Can populate ifowner/table current, metadata/icon resolved orfallbackmatch, class-order match andnotduplicate; viewcreation is not a handler gate'},{'position':'In/after lobby','static_result':'Same dependencies; owner/table maypersist while S1F/SDA clear placeholders. Lobby label alone doesnot provepopulation or preservation'}],
      'S1F_SDA_distinction':{'S1F':'4AEF30->4CD120 finds matchingindex and replaces (-1,-1,emptytext) placeholder, retaining count/table','SDA':'4AA8B0 unconditionaljump46A260->4CD1C0 destroys/reconstructs class-count (-1,-1,emptytext) placeholders;4CD1C0 doesnot read635B80 toinitializeicons','rebuild_and_constructor':'Placeholder count usesclassbyte; class-order values areconsulted onlylaterin4CD050','important':'Same owner/table/count is notproof skills stayed populated; reset createsplaceholders. No server resend or callbackexecution proven.'},
      'archived_dynamic_evidence':{'V9_timeline':record(legacy/'t69_skill_evidence/V9-S1E-timeline.json'),'pid':timeline['pid'],'filetime':timeline['filetime'],'S1E':timeline['S1E'],'interpretation':timeline['interpretation'],'payload_available':False,'selected_class_observed':False,'skill_owner_epoch_observed':False,'version_accepted_flag':'Archive flagfalse is not a native decoder-return observation; no newruntimeacceptanceclaim','T73':'Existing142frames containnoS1E andno typedskillowner; cannot close first-lobby gate execution'},
      'minimal_dynamic_requirements':['SamePID/FILETIME/owner epoch:8class4DWORDs after43F6 table block and4928 classselection','For actualS1E:payloadN/bitmap/index, mode/phase,75F694,635B80,647E20/24 pointerandindexedmetadata,834888 owner/count/table/rows atdispatch andafter4CD050','Orderrelative toownerpublication/resetS1F/SDA; actual receivedframe mustnotbe equatedwithdecodercompletion','Upstream receive pump/queue may affect when a frame reaches dispatcher; no first-lobby execution timing closure here','If rowsfilled butGUIempty, separately observebinding/render; resourceexistencealone doesnot answerit'],'static_task_complete':True,'runtime_gates_closed':False,'GUI_accepted':False,'native_started':False,'process_opened':False,'resource_extraction_rerun':False,'old_tests_rerun':False}
    write('S1E-initialization-gates.json',gates)
    print(json.dumps({'holes':holes,'storage':storage,'metadata_fallback_icon':91,'fallback91_known_class4_matches':[x['slot']for x in rows if x['conditional_native_value']==91],'dispatch':dispatch,'ranges':len(ranges)},ensure_ascii=True))

if __name__=='__main__':main()
