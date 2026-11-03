"""Finite direct 64AFA0 writers, background owner prefix, and one-hop callers.

Stops at every called private body. No asset normalization or old PASS replay.
"""
import sys
sys.dont_write_bytecode = True
from audit import ROOT, PE, ASM, record, write, stamp, sha, mapping
from seal_delta import seal
import json
import struct
import pefile
import capstone


def load(name):
    return json.loads((ROOT/name).read_text('utf-8'))


def main():
    destination = ROOT/'Native-background-scene'
    assert not (ROOT/'Native-background-scene-contract.json').exists(), 'Do not repeat a sealed finite branch'
    pins = {n:record(ROOT/n) for n in [
        'Native-provider-writer-contract.json', 'resources/modern-rgba/manifest.json',
        'server-packet-delta.json', 'CD1-mode-step/scope-write-correction.json',
        'Native-selection-resource-gap/Native-background-branch-reference.json',
        'CD2-selected-record-source/producer-copy-clear-and-field-contract.json',
        'CD2-selected-record-source/004A7450-52.bin',
        'receive-boundary/VA-00631EC8.bin',
    ]}
    assert pins['resources/modern-rgba/manifest.json']['sha256']=='D25BED57BE58F0AB3EB17F6A716F8202329B4E02BEE98F63B50D84ADA8218540'
    pe_before = record(PE)
    assert pe_before['sha256']=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
    data = PE.read_bytes()
    pe = pefile.PE(data=data)
    md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_32)
    checks = []

    def check(label, value):
        assert value, label
        checks.append({'check': label, 'pass': True})

    def capture(label, va, size, code=True):
        origin = mapping(pe, va)
        check(label+' raw-backed', origin['raw_backed'])
        off=int(origin['file_offset'],16)
        raw=data[off:off+size]
        check(label+' exact length',len(raw)==size)
        instructions=[]
        if code:
            instructions=[{'va':f'{i.address:08X}','rva':f'{i.address-0x400000:08X}',
                           'bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in md.disasm(raw,va)]
            check(label+' complete aligned decode',sum(len(bytes.fromhex(i['bytes'])) for i in instructions)==size)
        return {'label':label,'origin':{**origin,'bytes':size,'PE':pe_before},
                'raw_hex':raw.hex(),'sha256':sha(raw),'instructions':instructions}

    # Do not read callees. Branch40 is an existing sealed fragment referenced,
    # while adjacent owner/control statements and caller suffix are new evidence.
    ranges=[capture('direct_string_comparisons_and_two_flag_writers',0x4814DA,0x95),
            capture('background_entry_owner_prefix',0x496EB0,0x55),
            capture('background_return_and_direct_setup_prefix',0x496F2D,0x3B),
            capture('S03_one_hop_caller_suffix',0x4A74A2,0x42),
            capture('second_one_hop_caller',0x4AACF0,0x18)]
    data_ranges=[capture('two_compared_literals',0x62D228,24,False),
                 capture('disk_selector_byte',0x64AFA0,1,False)]
    old_branch=load('Native-selection-resource-gap/Native-background-branch-reference.json')
    branch_raw=bytes.fromhex(''.join(x['bytes'] for x in old_branch['existing_fragment_lines']))
    check('inherited branch40 matches current source without regenerating output', data[0x96F05:0x96F05+40]==branch_raw)
    caller_prefix=(ROOT/'CD2-selected-record-source/004A7450-52.bin').read_bytes()
    check('inherited S03 prefix82 matches source',data[0xA7450:0xA7450+82]==caller_prefix)
    descriptor=(ROOT/'receive-boundary/VA-00631EC8.bin').read_bytes()
    check('inherited descriptor pair matches current source',pe.get_data(0x231EC8,8)==descriptor)
    check('descriptor pair exact count and pointer',struct.unpack('<II',descriptor)==(8,0x630DF8))
    check('literal bytes qualified',bytes.fromhex(data_ranges[0]['raw_hex'])==b'lteventcsvr\0ltcharsvr\0\0\0')
    check('disk selector zero only',data_ranges[1]['raw_hex']=='00')
    all_instructions={i['va']:i for r in ranges for i in r['instructions']}
    expected={
        '004814DA':('mov','edi, 0x62d234'),
        '00481519':('mov','byte ptr [0x64afa0], 1'),
        '00481528':('mov','edi, 0x62d228'),
        '00481563':('mov','byte ptr [0x64afa0], 1'),
        '00496EB0':('mov','ecx, dword ptr [0x75f6e8]'),
        '00496EB8':('cmp','dword ptr [ecx + 0xc], 5'),
        '00496EBC':('jne','0x496f05'),
        '00496EF1':('mov','edx, 5'),
        '00496EF6':('mov','ecx, 0x631e38'),
        '00496EFB':('call','0x495520'),
        '00496F00':('mov','dword ptr [0x75f6e8], eax'),
        '00496F2D':('mov','edi, eax'),
        '00496F2F':('mov','eax, dword ptr [0x75f750]'),
        '00496F3D':('call','0x41b410'),
        '00496F5A':('cmp','esi, 4'),
        '00496F63':('call','0x495230'),
        '004A74A2':('mov','eax, dword ptr [0x63d54c]'),
        '004A74B1':('cmp','eax, 0x785b'),
        '004A74C0':('jge','0x4a74db'),
        '004A74C2':('mov','al, byte ptr [0x64afa3]'),
        '004A74C9':('jne','0x4a74db'),
        '004A74CB':('call','0x496eb0'),
        '004A74D0':('mov','byte ptr [0x64afa3], 1'),
        '004AACF0':('mov','al, byte ptr [0x64afbc]'),
        '004AACF9':('mov','al, byte ptr [0x64afbd]'),
        '004AAD02':('jmp','0x496eb0'),
    }
    for va,(mnemonic,operands) in expected.items():
        check(va+' direct operand',all_instructions[va]['mnemonic']==mnemonic and all_instructions[va]['operands']==operands)
    # ASM is navigation only; its input PE differs from this fixed source.
    asm_before=record(ASM)
    check('ASM navigation source pin',asm_before['sha256']=='2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A')
    lines=ASM.read_text('utf-8',errors='replace').splitlines()
    nav_ranges=[(275425,275518),(316824,316881),(349728,349790),(356635,356650),(559775,559789)]
    navigation={'source':asm_before,'authority':'Navigation/xrefs only, current fixed PE raw bytes authoritative; no private called body followed',
                'contexts':[{'start_line':a,'end_line':b,'lines':lines[a-1:b]} for a,b in nav_ranges],
                'search':{'literal_64AFA0_statement_occurrences':sum('byte_64AFA0' in line for line in lines),
                          'direct_named_store_statements': [{'line':i+1,'text':line} for i,line in enumerate(lines) if line.strip().startswith('mov     byte_64AFA0,')]},
                'absence_limit':'Two named immediate stores found in ASM; not exhaustive raw-code/indirect-store or runtime reset absence proof'}
    check('two ASM named writers only in finite search',len(navigation['search']['direct_named_store_statements'])==2)
    write('Native-background-scene/fixed-source-evidence.json',{
        'source_PE':pe_before,'new_code_ranges':ranges,'new_data_ranges':data_ranges,
        'inherited_background_branch':pins['Native-selection-resource-gap/Native-background-branch-reference.json'],
        'inherited_S03_prefix':pins['CD2-selected-record-source/004A7450-52.bin'],
        'inherited_descriptor_pair':pins['receive-boundary/VA-00631EC8.bin'],
        'limits':'Linear aligned bytes and explicit branch/call operands only. Unknown callees/virtual methods not inspected; source predicates conditional on calls returning normally.'})
    write('Native-background-scene/ASM-navigation.json',navigation)
    facts={
        'flag':{'VA':'0064AFA0','RVA':'0024AFA0','disk_value':0,'live_value_known':False,
                'writers':[{'VA':'00481519','RVA':'00081519','value':1,'comparison_literal':'ltcharsvr','literal_VA':'0062D234','condition':'Reaches byte-for-byte NUL-terminated comparison at4814DA with readable pre-existing ECX buffer and equality EAX0; companion64AF9E set1 at481512.'},
                           {'VA':'00481563','RVA':'00081563','value':1,'comparison_literal':'lteventcsvr','literal_VA':'0062D228','condition':'Reaches comparison with pointer from[EBP-1C] at481525; equality EAX0; companion64AF9F set1 at48155C.'}],
                'input_origin':'Not followed before this direct writer island; not assigned a command-line, server, language or sex policy',
                'reset_writer':'No other named direct store in the searched ASM; indirect/raw alternate initialization and actual runtime reset remain unknown'},
        'background_call':{'VA':'00496F28','RVA':'00096F28','target':'00495520',
                'selector_load':'00496F05','selector_test':'zero/nonzero only',
                'id_when_zero':6314,'id_when_nonzero':7482,
                'arguments':{'ECX':'[00631ECC], fixed data value00630DF8',
                             'EDX':'[00631EC8], fixed data value8',
                             'stack_push_order':['selected_image_id','7']},
                'result':'Normal return EAX moved into EDI at496F2D; if75F750 nonzero, direct loop indexes0..3 via41B410 and calls virtual+38/+3C. Then ECX=EDI/DL1 call495230 at496F63.',
                'calling_convention_or_constructor_semantics':'No new495520 body inspected; 7 is source argument, descriptor count8 is fixed data, neither proves scene enum or character capacity',
                'draw_position':None,'position_limit':'No direct x/y for the backdrop at this callsite. Existing child descriptor coordinates do not prove backdrop origin/crop/transform.'},
        'owner_prefix':{'global':'0075F6E8','load_VA':'00496EB0','tested_field_offset':'0C','compared_value':5,
                'condition':'The first cmp dereferences the loaded owner with no preceding null guard in this prefix. Value!=5 branches directly to496F05. Value5 calls opaque helpers, then495520 at496EFB withECX631E38/EDX5/push310/push0 and writes normal return EAX to75F6E8 at496F00.',
                'named_scene_owner':None,'limit':'This is source object/global and numeric field discrimination only; type5/name/creation-success and live initialized ownership not proved'},
        'one_hop_callers':[{'entry_VA':'004A7450','entry_RVA':'000A7450','inherited_handler':'S03 with signed countBYTE; established by existing CD2 producer contract, dispatch not reopened',
                'call_VA':'004A74CB','call_RVA':'000A74CB','source_predicate':'Decoded signed count is0; reached count-zero branch and49A510(ECX1) returned normally; signed[63D54C]<0x785B(30811) and[64AFA3]==0.',
                'other_path':'count!=0 bypasses setup; count0 with threshold>=0x785B or64AFA3 nonzero calls497010 at4A74DB; body not inspected.',
                'following_store':'64AFA3=1 at4A74D0 if496EB0 returns normally',
                'callback_stores':['4A74A7:62C9BC=4962B0','4A74B6:62C9B8=496200'],
                'position_or_named_scene':None},
               {'entry_VA':'004AACF0','entry_RVA':'000AACF0','transfer_VA':'004AAD02','transfer_RVA':'000AAD02',
                'source_predicate':'Both64AFBC and64AFBD are0 at their direct reads; otherwise return4AAD07.',
                'transfer':'tailjmp496EB0, not a count/scene-name contract','opcode_or_flag_meaning':None}],
        'state_values':{'selector':[0,'nonzero'],'owner_field_0C_test':5,'factory_stack_constant':7,
                'descriptor_count':8,'existing_S03_count_gate':0,
                'state_helper_call_argument':'49A510 receivesECX1 on inherited S03 count0 path; helper semantics and state storage not inspected',
                'named_scene_enum':None,'Directory_assigned':False,'sex_assigned':False},
        'minimum_missing_evidence':[
            'An accepted mapping from the numeric owner/factory/caller context to a named UI scene; S03 count0 setup association alone cannot justify naming this general Directory.',
            'An accepted existing495520(kind7,imageID) result-to-backdrop coordinates/transform/clip/draw-mode contract, or already-qualified saved instance record. No x/y arguments are present here.',
            'An accepted initialized owner/flag lifecycle record for the relevant version/epoch before treating static branch conditions as live state.'],
        'stop':'STOP at495520 constructor semantics / unknown owner and helper names. No follow-up private layer, no new_native_capture.42E550/42E6D0/42EA50 remain stopped.',
        'receiver':'Modern UI组＋監製；本組只交付這些數值路徑與未知，不授背景default scene binding',
        'wire_differences':[],'server_notification_requested':False,
    }
    write('Native-background-scene-contract.json',{
        'updated_at':stamp(),'generator':record(__file__),'source_PE':pe_before,
        'facts':facts,'source_evidence':record(destination/'fixed-source-evidence.json'),
        'ASM_navigation':record(destination/'ASM-navigation.json'),
        'reused_contracts':pins,'new_private_callee_bodies':0,'Native_capture_or_runtime':0,
        'Modern_edits_builds_tests':0,'scene_mapping_complete':False,'draw_position_complete':False,
        'private_backing_branches_stopped':True,'finite_source_contract_complete':True})
    (destination/'handoff.md').write_text('''# Native 背景：直接 writer 與一層 caller

已核固定PE，未追任何被呼叫private body。來源SHA、VA/RVA、rawhex與逐指令位置在fixed-source-evidence.json；結論在上一層Native-background-scene-contract.json。

- 64AFA0的兩個具名直接writer：481519／RVA81519與481563／RVA81563寫1。直接字串比對literal分別ltcharsvr@62D234與lteventcsvr@62D228；只有抵達且比對相等才写1。不是Directory／sex語義。固定disk byte0不是live initialized值，未證明其他間接writer不存在。
- 496F28／RVA96F28 call495520：64AFA0零選6314，非零選7482；先push圖片ID，再push7，ECX=[631ECC]／EDX=[631EC8]。既有fixed descriptor pair為630DF8／8；8不是角色容量。直接call無背景x/y參數。
- 496EB0的owner來源75F6E8，先讀[owner+0C]與5比對；無前置null guard。!=5直接選圖；==5先opaque helper，再用631E38／EDX5／push310／push0進495520，return EAX寫回75F6E8。這些數值尚沒有已授權的具名scene/type語義。
- 一層既有S03 handler4A7450：count0且signed[63D54C]<785B、64AFA3=0，於4A74CB呼叫496EB0；return後64AFA3=1。另一caller4AACF0只有64AFBC/64AFBD均0時於4AAD02 tailjmp496EB0。兩個caller的callee與上層dispatch不展開。

可接收的是conditional source predicates；仍不可指定Directory或draw(0,0)。精確停止：缺已接受的numeric owner/caller→scene名稱、495520結果→背景draw座標/transform/mode，以及同版本實際initialized owner/flag生命周期。既有四個portrait child的x/y不是背景位置。Modern／監製接收契約後須保留scene_binding與draw_position未知。

本次不改D25BED57 RGBA manifest，不重新產圖／抽取；0wire，不通知server；42E550/42E6D0/42EA50仍停止。既有外部pyc清理自動審查blocked by policy的事件保持open，未碰或重試。
''',encoding='utf-8')
    check('fixed PE stable',record(PE)==pe_before)
    check('navigation source stable',record(ASM)==asm_before)
    for name,pin in pins.items():
        check('retained pin '+name,record(ROOT/name)==pin)
    check('scene and draw unknown',not facts['state_values']['Directory_assigned'] and facts['background_call']['draw_position'] is None)
    write('Native-background-scene/finite-verification.json',{
        'updated_at':stamp(),'checks':checks,'all_pass':True,'checks_count':len(checks),
        'new_code_ranges':len(ranges),'new_code_bytes':sum(r['origin']['bytes'] for r in ranges),
        'new_instruction_rows':sum(len(r['instructions']) for r in ranges),
        'new_data_ranges':len(data_ranges),'new_data_bytes':sum(r['origin']['bytes'] for r in data_ranges),
        'old_tests_replayed':False,'unknown_callee_bodies_opened':0,'scope':'Targeted source delta only; no runnable/GUI/scene acceptance'})
    new_names=['Native-background-scene-contract.json','Native-background-scene/fixed-source-evidence.json',
               'Native-background-scene/ASM-navigation.json','Native-background-scene/handoff.md',
               'Native-background-scene/finite-verification.json','background_scene_boundary.py']
    phase='BACKGROUND_DIRECT_WRITERS_ONE_HOP_CALLERS_SEALED_SCENE_AND_DRAW_UNKNOWN_OVERALL_OPEN'
    next_step='背景directwriter/one-hop caller已核，見Native-background-scene-contract.json；STOP在具名scene／背景draw契約及live owner，禁止自行Directory/sex/default binding或逐層private追蹤。'
    h=load('handoff.json')
    for key in ('modern','launcher'):
        by={x['path']:x for x in h[key]}
        for name in new_names:by[str(ROOT/name)]=record(ROOT/name)
        h[key]=list(by.values())
    h.update(updated_at=stamp(),phase=phase,Native_background_scene_contract=record(ROOT/new_names[0]),
             Native_background_named_scene_proved=False,Native_background_draw_position_proved=False,overall_complete=False)
    h['next_steps']=[next_step]+h['next_steps']
    write('handoff.json',h)
    complete=load('completeness-audit.json')
    complete.update(updated_at=stamp(),Native_background_direct_source=record(ROOT/new_names[0]),
                    Native_background_named_scene_proved=False,Native_background_draw_position_proved=False)
    write('completeness-audit.json',complete)
    # Refresh changed shared completeness reference without altering historical refs.
    h=load('handoff.json')
    for key in ('modern','launcher','shared'):
        h[key]=[record(ROOT/'completeness-audit.json') if x['path']==str(ROOT/'completeness-audit.json') else x for x in h[key]]
    write('handoff.json',h)
    entry=ROOT/'閱讀入口.md'
    entry.write_text(entry.read_text('utf-8')+'\n背景directwriter有限續查：Native-background-scene-contract.json／Native-background-scene/handoff.md。481519/481563→64AFA0=1；496F28選6314/7482；一層S03 count0與4AACF0caller前提已核。scene名稱與背景x/y仍unknown，STOP於495520語義，不授Directory/sex，privatebacking保持停止。\n',encoding='utf-8')
    progress=load('監製進度.json')
    progress.update(updated_at=stamp(),phase=phase,current_task='背景64AFA0兩個直接writer與選圖/owner prefix、一層既有caller有限核對完成；具名scene及draw位置仍不足，已精確停止。',
                    Native_background_direct_source=record(ROOT/new_names[0]),overall_complete=False)
    progress['next_steps']=[next_step]+progress['next_steps']
    write('監製進度.json',progress)
    count=seal(new_names+['handoff.json','completeness-audit.json','閱讀入口.md'],ROOT/'background_scene_boundary.py')
    print(json.dumps({'contract':record(ROOT/new_names[0]),'finite_checks':len(checks),'changed_files_verified':count,
                      'new_code_bytes':sum(r['origin']['bytes'] for r in ranges),
                      'new_instruction_rows':sum(len(r['instructions']) for r in ranges),'scene':None,'draw_position':None},ensure_ascii=True))


if __name__=='__main__':
    main()
