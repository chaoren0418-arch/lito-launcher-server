"""Batch9 finite IMG composition; fixed bytes and synthetic models only."""
from audit import *
from extract_resources import decode_img

OUT=ROOT/'skill-pane-layer-composition'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
BATCH8='8B4AFC6C4C884099AD092459B1608B2586E4A4EE6DB22F1FBBA642EE4DC6A7CE'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def cv(v):return (v+(v&0x7FE0))&65535
def div31(n):
    q=((n*-2078209981>>32)+n)>>4
    return q+((q&0xFFFFFFFF)>>31)
def channels(v,flag):return [v&31,(v>>(6 if flag else 5))&31,(v>>(11 if flag else 10))&31]
def pack(c,flag):return c[0]|(c[1]<<(6 if flag else 5))|(c[2]<<(11 if flag else 10))
def alpha_pixel(d,s,a,flag):return pack([div31(dc*(31-a)+sc*a)for dc,sc in zip(channels(d,flag),channels(s,flag))],flag)

def simulate(kind,source,pitch,xy,flag=0,alpha=15,key=0,rect=None,seeded_screen=False):
    """Synthetic descriptor with padding; no native code execution or safety claim."""
    width=3;height=2;surface_height=6;base=256;stride=2*(pitch>>1)
    original=bytearray(b'\x5A\xA5'*1024);dest=bytearray(original);writes=[];reads=[]
    if rect is None:rect=[0,0,width,height]
    rw=width if kind!='screen_rect'else rect[2]-rect[0]
    rh=height if kind!='screen_rect'else rect[3]-rect[1]
    end=(base+(xy[1]+rh)*stride)&0xFFFFFFFF;limit=(base+surface_height*stride)&0xFFFFFFFF
    accepted=end<=limit
    if accepted:
        for row in range(max(rh,0)):
            for col in range(max(rw,0)):
                sx=col+(rect[0]if kind=='screen_rect'else 0);sy=row+(rect[1]if kind=='screen_rect'else 0)
                idx=sy*width+sx;s=source[idx];offset=base+(xy[1]+row)*stride+(xy[0]+col)*2
                if kind=='alpha'and(xy[0]+col<0 or xy[1]+row<0):continue
                if kind!='opaque'and s==key:continue
                assert 0<=offset<=len(dest)-2,'Audit buffer only; native code has no such assertion.'
                d=int.from_bytes(dest[offset:offset+2],'little')
                if kind=='alpha':o=alpha_pixel(d,s,alpha,flag);reads.append({'offset':offset,'word':f'{d:04X}'})
                elif kind=='screen_rect':
                    assert seeded_screen
                    # Explicit synthetic LUT q=D. This verifies consumers/coverage only, not native screen table rounding.
                    o=pack(channels(d,flag),flag);reads.append({'offset':offset,'word':f'{d:04X}'})
                else:o=s
                dest[offset:offset+2]=o.to_bytes(2,'little');writes.append({'offset':offset,'source_index':idx,'source_word':f'{s:04X}','output_word':f'{o:04X}'})
    return {'kind':kind,'source_words':[f'{v:04X}'for v in source],'descriptor':{'buffer_base':base,'height':surface_height,'audit_width_metadata_unused_by_native_guards':8,'pitch':pitch,'effective_stride':stride},
      'caller_xy':xy,'source_rect':rect if kind=='screen_rect'else None,'format':'RGB565'if flag else'RGB555','alpha':alpha if kind=='alpha'else None,
      'key':f'{key:04X}','bottom_guard_end_unsigned32':end,'bottom_guard_limit_unsigned32':limit,'bottom_guard_accepted':accepted,
      'writes':writes,'destination_reads':reads,'output_bytes':bytes(dest),'screen_table_policy':'synthetic q=D, not initialized native table'if seeded_screen else None}

def main():
    OUT.mkdir(exist_ok=True);fixed=record(PE);assert fixed['sha256']==FIXED
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    b8=load('skill-pane-frame-coordinate-contract.json');assert record(ROOT/'skill-pane-frame-coordinate-contract.json')['sha256']==BATCH8
    preserved=b8['prior_primary_pins']+[{**record(ROOT/'skill-pane-frame-coordinate-contract.json'),'expected_sha256':BATCH8,'match':True}]
    assert all(record(Path(p['path']))['sha256']==p['expected_sha256']for p in preserved)
    ranges=[];ins={};owners={};inherited=[]
    def install(row):
        for x in row['instructions']:
            va=int(x['va'],16);ins[va]=x;owners[va]=row['output']
    def capture(label,va,n,kind='code'):
        assert mapping(pe,va)['raw_backed'];raw=pe.get_data(va-0x400000,n)
        decoded=[]if kind=='data'else list(cs.disasm(raw,va));assert len(raw)==n and(kind=='data'or sum(x.size for x in decoded)==n),(label,hex(va),n)
        path=OUT/f'ranges/{va:08X}-{n:X}.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(raw)
        row={**mapping(pe,va),'label':label,'kind':kind,'bytes':n,'output':record(path),'decoded_bytes':sum(x.size for x in decoded),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in decoded]}
        ranges.append(row);install(row);return row
    for file,label in [('skill-pane-layout/ranges.json','pane_render'),('skill-pane-render-initialization/ranges.json','dispatch_and_timed_table_initializer'),
      ('skill-pane-render-initialization/ranges.json','dispatch_initializer_direct_caller_gate'),('skill-pane-frame-coordinate/ranges.json','IMG_header_preserving_format_converter'),('skill-pane-frame-coordinate/ranges.json','IMG_entry_loader')]:
        row=next(x for x in load(file)['ranges']if x['label']==label);assert record(Path(row['output']['path']))==row['output'];install(row)
        inherited.append({'index':record(ROOT/file),'label':label,'range':row['output'],'bytes_reused_without_old_case_replay':True})
    specs=[('933_opaque_dense_copy',0x4B9F60,0x151),('934_keyed_dense_copy',0x4BA220,0x146),
      ('933_alpha_RGB555_entry',0x4BD7D0,0x31),('933_alpha_RGB565_entry',0x4BD810,0x31),
      ('933_alpha_RGB555_body',0x4BD1B0,0x20B),('933_alpha_RGB565_body',0x4BD3C0,0x201),
      ('935_rect_RGB555_entry',0x4BD850,0x27),('935_rect_RGB565_entry',0x4BD880,0x27),
      ('935_rect_RGB565_body',0x4BE870,0x210),('935_rect_RGB555_body',0x4BEA80,0x21A),('screen_initializer_conversion_thunk',0x4F1592,6)]
    for x in specs:capture(*x)
    for label,va,n in [('screen_FPU_constants',0x4FA328,16),('alpha_dispatch_snapshot',0x83154C,4),('rect_dispatch_snapshot',0x831558,4),
      ('933_trigger_snapshot',0x6328C0,4),('format_flag_snapshot',0x641298,1)]:capture(label,va,n,'data')
    def w(va):return {'fixed_PE_sha256':FIXED,**mapping(pe,va),**{k:v for k,v in ins[va].items()if k!='va'},'range':owners[va]}
    expected=[
      (0x4CCB30,'cmp','esi, 0x1f'),(0x4CCBA3,'call','0x4b9f60'),(0x4CCBB2,'je','0x4ccc32'),(0x4CCC21,'and','esi, 0xffff'),(0x4CCC2C,'call','dword ptr [0x83154c]'),
      (0x4CCC3B,'mov','ebx, dword ptr [eax + 0xe98]'),(0x4CCC96,'call','0x4ba220'),(0x4CCCA4,'call','0x41b690'),(0x4CCCB1,'call','dword ptr [edx + 0x24]'),
      (0x4CCCBD,'mov','esi, dword ptr [eax + 0xe9c]'),(0x4CCCF8,'lea','ebx, [eax + ecx + 2]'),(0x4CCCE8,'lea','edi, [ecx + edx + 0xa]'),
      (0x4CCD15,'mov','dword ptr [ebp - 0x1c], eax'),(0x4CCD18,'mov','dword ptr [ebp - 0x18], eax'),(0x4CCD1B,'movsx','edx, word ptr [ecx]'),(0x4CCD24,'movsx','eax, word ptr [ecx + 2]'),(0x4CCD31,'call','dword ptr [0x831558]'),
      (0x4B9FFE,'sar','edx, 1'),(0x4BA01B,'movsx','ecx, word ptr [eax + 2]'),(0x4BA02D,'jbe','0x4ba041'),(0x4BA05F,'movsx','ecx, word ptr [eax]'),(0x4BA06E,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),(0x4BA078,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),
      (0x4BA2BE,'sar','ebx, 1'),(0x4BA2E7,'jbe','0x4ba2fb'),(0x4BA321,'cmp','si, word ptr [eax + 6]'),(0x4BA325,'je','0x4ba32a'),(0x4BA327,'mov','word ptr [ecx], si'),
      (0x4BD7D7,'shl','eax, 0xb'),(0x4BD7DA,'lea','esi, [eax + 0x7dc09c]'),(0x4BD7F7,'call','0x4bd1b0'),(0x4BD837,'call','0x4bd3c0'),(0x4BD86E,'call','0x4bea80'),(0x4BD89E,'call','0x4be870'),
      (0x4BEB35,'sar','eax, 1'),(0x4BEB78,'jbe','0x4beb8e'),(0x4BEBDF,'cmp','cx, word ptr [ebx + 6]'),(0x4BEC43,'mov','word ptr [edx], si'),
      (0x4C1F22,'mov','dword ptr [0x83154c], 0x4bd810'),(0x4C1F36,'mov','dword ptr [0x831558], 0x4bd880'),(0x4C2034,'mov','dword ptr [0x83154c], 0x4bd7d0'),(0x4C2048,'mov','dword ptr [0x831558], 0x4bd850'),
      (0x4C1972,'call','0x4f1592'),(0x4C1980,'mov','word ptr [esi + 0x81089c], ax'),(0x4C1995,'mov','word ptr [esi + 0x81189c], dx'),(0x4C19A8,'mov','word ptr [esi + 0x81109c], dx'),
      (0x4C1A3D,'mov','word ptr [ebx + 0x7db89c], dx'),(0x4C1A49,'mov','word ptr [ebx + 0x7fb89c], ax'),(0x4C1A5F,'mov','word ptr [ebx + 0x7eb89c], ax'),
      (0x4BB469,'mov','ax, word ptr [esi + 4]'),(0x4BB46F,'je','0x4bb483'),(0x4BB47F,'mov','word ptr [esi + 6], ax'),(0x4D4F20,'call','0x4bb450'),(0x4F1593,'jmp','0x7741ac20')]
    assertions=[]
    for va,mn,op in expected:
        assert(ins[va]['mnemonic'],ins[va]['operands'])==(mn,op),(hex(va),ins[va]);assertions.append({**w(va),'match':True})
    # Pin every key compare, channel extraction, lower-bound branch, write, and virtual surface call in the new bodies.
    additional=[]
    for row in ranges:
        if row['label'].endswith('_body'):
            additional += [x for x in row['instructions']if(x['mnemonic']in ['shr','and','jl','jle','jbe']or x['mnemonic']=='call'or(x['mnemonic']=='mov'and x['operands'].startswith('word ptr [edx]'))or(x['mnemonic']=='cmp'and '+ 6]'in x['operands']))]
    seen={x['va']for x in assertions}
    for x in additional:
        if x['va']not in seen:assertions.append({**w(int(x['va'],16)),'match':True,'assertion_kind':'decoded operand witness, not independent hardcoded expectation'});seen.add(x['va'])
    for x in assertions:
        raw=bytes.fromhex(x['raw_hex']);va=int(x['va'],16);decoded=list(cs.disasm(pe.get_data(va-0x400000,len(raw)),va))
        assert pe.get_data(va-0x400000,len(raw))==raw and len(decoded)==1 and(decoded[0].mnemonic,decoded[0].op_str)==(x['mnemonic'],x['operands'])
    assert struct.unpack('<2d',pe.get_data(0xFA328,16))==(1/31,31.0)
    assert pe.get_data(0x43154C,4)==bytes(4)and pe.get_data(0x431558,4)==bytes(4)
    # Explicit key/negative guards: exact addresses come from fully captured functions.
    guard_refs={}
    for label in ['933_alpha_RGB555_body','933_alpha_RGB565_body','935_rect_RGB555_body','935_rect_RGB565_body']:
        row=next(x for x in ranges if x['label']==label)
        guard_refs[label]=[{**w(int(x['va'],16))}for x in row['instructions']if x['mnemonic']in ['jl','jbe']or(x['mnemonic']=='cmp'and '+ 6]'in x['operands'])]
    assets=[];extraction=load('skill-pane-layer-composition/layer-extraction.json')
    for member,dimensions in zip(extraction['members'],[(191,274,0,0x4E73),(203,261,1,0)]):
        pin=member['decoded'];assert record(Path(pin['path']))==pin;raw=Path(pin['path']).read_bytes();width,height,flag,key=struct.unpack_from('<hhHH',raw)
        assert(width,height,flag,key)==dimensions and len(raw)==8+width*height*2
        px=list(struct.unpack('<'+'H'*(width*height),raw[8:]));assert all(v<32768 for v in px)
        converted=[cv(v)for v in px];converted_key=cv(key)if flag else key
        image,details=decode_img(raw,True);preview=OUT/(member['name']+'.source-preview.png');image.save(preview)
        assets.append({'member':member,'width':width,'height':height,'flag':flag,'key_RGB555':f'{key:04X}','key_after_565_loader':f'{converted_key:04X}',
          'dense_exact_size':True,'source_key_pixel_count':px.count(key),'565_key_comparison_match_count':converted.count(converted_key),'source_high_bit_words':0,
          'preview':record(preview),'preview_policy':details,'preview_is_GUI_oracle':False,
          '565_behavior':'flag0 leaves header key unchanged although all pixels convert; draw routines still compare that key'if not flag else'flag1 converts header key and all pixels'})
    write('skill-pane-layer-composition/member-headers.json',{'members':assets,'934_header_inherited':record(ROOT/'skill-pane-frame-coordinate/frame934-header.json'),'934_payload_replayed':False})
    geometry=[]
    for resource,width,height,offset in [(933,191,274,[8,10]),(934,207,289,[0,0]),(935,203,261,[2,10])]:
        geometry.append({'resource_id':resource,'qualified_default_candidate_dimensions':[width,height],'origin_offset_from_child_plus_G':offset,
          'relative_halfopen_extent':[offset[0],offset[1],offset[0]+width,offset[1]+height],
          'conditional_extent_if_child433_0_G0':[433+offset[0],offset[1],433+offset[0]+width,offset[1]+height],
          'runtime_default_candidate_load_and_epoch_unobserved':True})
    write('skill-pane-layer-composition/layer-geometry.json',{'geometry':geometry,'934_dimensions_reused_from_accepted_batch8':True,'native_GUI_oracle':False})
    tables=[]
    for flag in (0,1):
        for color,shift in zip(['blue','green','red'],[0,6 if flag else 5,11 if flag else 10]):
            values=[div31(d*(31-a)+s*a)<<shift for a in range(32)for d in range(32)for s in range(32)]
            assert values==[((d*(31-a)+s*a)//31)<<shift for a in range(32)for d in range(32)for s in range(32)]
            path=OUT/f'RGB{565 if flag else 555}/933-alpha-{color}.LE16.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(struct.pack('<'+'H'*len(values),*values))
            tables.append({'format':'RGB565'if flag else'RGB555','color':color,'channel_shift':shift,'alpha_count':32,'entries_per_alpha':1024,'index':'alpha*1024+destination5*32+source5','output':record(path)})
    write('skill-pane-layer-composition/933-alpha-tables.json',{'formula':'q=floor((D5*(31-a)+S5*a)/31), a/D5/S5=0..31','native_integer_reciprocal_model_matches_all_entries':True,
      'source_initializer_inherited':inherited[1],'initialization_requires_4F1592_returns_for_prior_screen_table_step':True,'conditional_live_tables':True,'tables':tables})
    # Screen LUT expressions stop at external conversion. No guessed native table bytes are produced.
    constants=struct.unpack('<2d',pe.get_data(0xFA328,16));expressions=[]
    for d,s in [(0,0),(1,1),(10,20),(16,16),(31,5),(31,31)]:
        maximum=max(d,s);minimum=min(d,s);x=((constants[1]-maximum)*constants[0])*minimum+maximum
        expressions.append({'destination5':d,'source5':s,'expression_input_to_4F1592':x,'ideal_rational_numerator':31*(d+s)-d*s,'ideal_rational_denominator':31,'native_integer_result':None})
    write('skill-pane-layer-composition/935-screen-expression-boundary.json',{'known_FPU_order':'(31-max(D,S))*double_at4FA328*min(D,S)+max(D,S)',
      'constants_raw':pe.get_data(0xFA328,16).hex(),'constant_1_over_31':constants[0],'constant_31':constants[1],
      'last_visible_call':w(0x4C1972),'thunk_jump':w(0x4F1593),'missing_destination':mapping(pe,0x7741AC20),
      'missing':'Floating-to-integer implementation/rounding/FPU precision state outside fixed PE. No native935 screen LUT numeric closure claimed. Do not use floor/round from preview.',
      'expression_examples_only':expressions,'native_table_outputs_generated':False})
    cases=[];source=[0,0x7C00,0x83E0,0x001F,0x4E73,0xFFFF]
    specs_cases=[('opaque_even','opaque',18,[1,1],0,31,0,None,False),('opaque_odd_pitch','opaque',19,[1,1],0,31,0,None,False),
      ('keyed_flag0','keyed',18,[1,1],0,15,0,None,False),('alpha555','alpha',18,[1,1],0,15,0,None,False),
      ('alpha565','alpha',19,[1,1],1,15,0,None,False),('alpha_negative_lower_guard','alpha',18,[-1,-1],0,15,0,None,False),
      ('opaque_negative_origin','opaque',18,[-1,-1],0,31,0,None,False),('right_edge_no_clip','keyed',18,[8,1],0,15,0,None,False),
      ('bottom_all_or_nothing','keyed',18,[1,5],0,15,0,None,False),('screen_full_rect_seeded','screen_rect',19,[1,1],1,15,0,[0,0,3,2],True),
      ('screen_subrect_seeded','screen_rect',18,[1,1],0,15,0,[1,0,3,2],True),('screen_negative_no_lower_guard_seeded','screen_rect',18,[-1,-1],1,15,0,[0,0,3,2],True)]
    for name,kind,pitch,xy,flag,alpha,key,rect,seed in specs_cases:
        case=simulate(kind,source,pitch,xy,flag,alpha,key,rect,seed);raw=case.pop('output_bytes');path=OUT/f'cases/{name}.bin';path.parent.mkdir(exist_ok=True);path.write_bytes(raw)
        cases.append({'name':name,**case,'output':record(path),'native_GUI_oracle':False})
    assert cases[0]['output']['sha256']==cases[1]['output']['sha256']
    assert len(cases[0]['writes'])==6 and len(cases[2]['writes'])==5 and not cases[8]['writes']
    assert len(cases[5]['writes'])<len(cases[6]['writes'])
    order_cases=[]
    for trigger in [0,15,31,0x10000,0x1001F,32]:
        first=[]if trigger==0 else[{'id':933,'target':'4B9F60'if trigger==31 else'83154C','alpha':None if trigger==31 else trigger&65535}]
        order_cases.append({'6328C0':trigger,'draw_sequence':first+[{'id':934,'target':'4BA220'},{'role':'child_controls','target':'41B690'},{'role':'skill_grid','target':'owner virtual+24'},{'id':935,'target':'831558'}],
          'all_payloads_present_and_initialization_success_assumed':True,'alpha_valid_initialized_range':trigger==0 or trigger==31 or (trigger&65535)<32})
    write('skill-pane-layer-composition/code-derived-cases.json',{'status':'SYNTHETIC_STATIC_MODELS_ONLY','cases':cases,'renderer_order_cases':order_cases,
      'header_flag_not_read_by_draw_bodies':True,'screen_cases_require_explicit_synthetic_LUT':True,'old_PASS_icon_packet_or934_case_replay':False})
    surface_calls=[];private_calls=[]
    for row in ranges:
        for x in row['instructions']:
            if x['mnemonic']=='call':
                if 'dword ptr ['in x['operands']:surface_calls.append({'body':row['label'],**w(int(x['va'],16)),'implementation_not_observed':True})
                elif x['operands'].startswith('0x'):
                    dest=int(x['operands'],16)
                    if not mapping(pe,dest)['raw_backed']:private_calls.append({'body':row['label'],**w(int(x['va'],16)),'destination':mapping(pe,dest),'not_expanded':True})
    for va in [0x4CCB57,0x4CCBD1,0x4CCC50,0x4CCCD2]:
        dest=int(ins[va]['operands'],16);assert not mapping(pe,dest)['raw_backed']
        private_calls.append({'body':'inherited4CCB20 resource error branch',**w(va),'destination':mapping(pe,dest),'not_expanded':True})
    lines=ASM.read_text(encoding='utf-8').splitlines()
    write('skill-pane-layer-composition/asm-navigation.json',{'source':record(ASM),'authority':'navigation only; heap addresses/target calls can differ from fixed PE',
      'excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}for a,b in [(427623,427897),(388999,389195),(389401,389604),(395945,396466),(396749,396874),(398937,399466),(405177,405428),(405672,405742)]]})
    write('skill-pane-layer-composition/ranges.json',{'fixed_PE':fixed,'ranges':ranges,'inherited_ranges':inherited,'all_code_fully_decoded':True,'all_new_ranges_raw_backed':True})
    verified={'range_bytes_and_pins_match':all(record(Path(r['output']['path']))==r['output']and Path(r['output']['path']).read_bytes()==pe.get_data(int(r['rva'],16),r['bytes'])for r in ranges),
      'raw_ranges':len(ranges),'code_ranges':sum(r['kind']=='code'for r in ranges),'data_ranges':sum(r['kind']=='data'for r in ranges),
      'hardcoded_instruction_assertions':len(expected),'all_instruction_witnesses':assertions,'prior_primary_pins':preserved,
      'all_witness_rawbytes_and_decoded_operands_match_fixed_PE':True,
      'new_exact_member_pins':[a['member']['decoded']for a in assets],'new_wire_differences':0,'native_GUI_accepted':False}
    assert verified['range_bytes_and_pins_match'];write('skill-pane-layer-composition/new-evidence-verification.json',verified)
    report={'schema':'lito.finite-skill-pane-layer-composition.v1','updated_at':stamp(),'status':'FINITE_STATIC_LAYER_COMPOSITION_WITH_PRECISE_EXTERNAL_TABLE_BOUNDARY',
      'fixed_PE':fixed,'ASM':record(ASM),'scope':'Only existing4CCB20 renderer, actual direct entries and one necessary body per thin entry. Reuse accepted initializer/loader/converter raw; no fullprogram expansion.',
      'prior_primary_pins':preserved,'inherited_evidence':inherited,
      'reusable_outputs':[record(OUT/n)for n in ['ranges.json','layer-extraction.json','member-headers.json','layer-geometry.json','933-alpha-tables.json','935-screen-expression-boundary.json','code-derived-cases.json','asm-navigation.json','new-evidence-verification.json']]+[t['output']for t in tables]+[a['preview']for a in assets]+[a['member']['decoded']for a in assets],
      'renderer_order':[
        {'order':1,'id':933,'condition':'Full DWORD6328C0==0 skip; ==31 opaque4B9F60; otherwise83154C with alpha=DWORD&FFFF. Payload NULL skips layer. No signed/range validation.',
          'origin':'child origin+G+(8,10)','witnesses':[w(v)for v in [0x4CCB30,0x4CCB42,0x4CCB6D,0x4CCB7D,0x4CCBA3,0x4CCBB2,0x4CCC21,0x4CCC2C]]},
        {'order':2,'id':934,'condition':'Payload nonNULL; independent of6328C0','origin':'child origin+G','target':'4BA220','witnesses':[w(0x4CCC3B),w(0x4CCC96)],'resource_header_inherited':record(ROOT/'skill-pane-frame-coordinate/frame934-header.json')},
        {'order':3,'role':'child controls','target':'41B690','witness':w(0x4CCCA4),'implementation':'Accepted batch6; no extra recursion'},
        {'order':4,'role':'skill grid','target':'owner virtual+24','witness':w(0x4CCCB1),'implementation':'Accepted batch6/7; no icon replay'},
        {'order':5,'id':935,'condition':'Payload nonNULL; no6328C0 check','origin':'child origin+G+(2,10)','source_rect':'[0,0,signed IMGwidth,signed IMGheight] halfopen','target':'831558',
          'witnesses':[w(v)for v in [0x4CCCBD,0x4CCCF8,0x4CCCE8,0x4CCD15,0x4CCD18,0x4CCD1B,0x4CCD24,0x4CCD31]]}],
      'entry_ABIs':{'4B9F60_4BA220':'cdecl stack IMGpayload,surface,x,y; caller adds16 bytes','83154C':'ECX=IMGpayload,EDX=surface; stack x,y,alpha; target ret12',
        '831558':'ECX=IMGpayload,EDX=&sourceRect; stack surface,x,y; target ret12; body receives ECX=IMG,EDX=surface and stack rect,x,y,redTable,greenTable,blueTable'},
      'format_dispatch':{'initializer_inherited':inherited[1],'direct_caller_inherited':inherited[2],
        'nonzero_641298':{'83154C':'4BD810→4BD3C0 RGB565','831558':'4BD880→4BE870 RGB565'},'zero_641298':{'83154C':'4BD7D0→4BD1B0 RGB555','831558':'4BD850→4BEA80 RGB555'},
        'writes':[w(v)for v in [0x4C1F22,0x4C1F36,0x4C2034,0x4C2048]],'snapshot_dispatch_pointers_zero':True,'live_initialized_dispatch_unobserved':True},
      'dense_IMG_header':{'layout':'+0 signed16 width,+2 signed16 height,+4 WORDflag,+6 WORDkey,+8 dense LE16 pixels, no RLE',
        'draw_flag_semantics':'All six draw bodies ignore IMG+4. Keyed/blend bodies compare +6 unconditionally; opaque body does not compare key.',
        'dimensions':'Positive signed width/height required for qualified members; no safe general validation. Opaque width negative can become huge unsigned rep count.',
        'pixel_formats':'Opaque/keyed copy WORD verbatim, including bit15 and zero unless equal key. Alpha/rect use fivebit B/G/R;565 green=(word>>6)&31, discards greenbit5 and outputs it0;555 ignores bit15.',
        'loader_converter_inherited':inherited[3],'loader_inherited':inherited[4],
        'converter':'641298 nonzero converts all pixels v+(v&7FE0); key converts only if flag!=0; width/height/flag unchanged. This asset-specific loader step precedes draw, not a draw-time format check.',
        'format_epoch_boundary':'Loader flag, initializer flag and surface pixel format must agree; cached IMG may bypass conversion on later format changes. No live epoch acceptance.'},
      'opaque933':'4B9F60 copies exactly width*2 bytes per positive row with rep movsd then rep movsb. Key/flag ignored, no destination read, no alpha. Effective stride=2*(signed pitch>>1).',
      'keyed934':'4BA220 skips S==headerkey regardless flag; every other WORD writes S without destination read. No alpha/scale/coverage mask.',
      'alpha933':{'table_slice':'alpha<<11 bytes; red7FC09C,green7EC09C,blue7DC09C + slice. Native initialized alpha0..31 only; caller masksFFFF but does not clamp.',
        'q':'floor((D5*(31-a)+S5*a)/31), exact integer reciprocal model; 32*1024 entries per color. RGB555 shifts0/5/10, RGB5650/6/11.',
        'key':'S==key skips without destination read; otherwise destination-dependent table blend. Headerflag ignored.',
        'lower_bounds':'Each row skips if destY+row<0; each pixel skips if destX+col<0. No right check. Opaque fullDWORD31 branch has different key/lower-bound behavior from alpha31 table branch.',
        'outputs':record(OUT/'933-alpha-tables.json'),'guard_witnesses':guard_refs['933_alpha_RGB555_body']+guard_refs['933_alpha_RGB565_body']},
      'rect935':{'table_addresses':{'blue':'81089C','green':'81109C','red':'81189C'},'consumer':'Keyed destination-dependent lookup, index=32*D5+S5 per channel. Full source rectangle from renderer. No lower x/y guard or right/sourceRect clamp.',
        'known_initializer_expression':'max(D5,S5)+(31-max(D5,S5))*double(1/31)*min(D5,S5), exact observed FPU order/constants; ideal mathematical screen expression D+S-D*S/31.',
        'integer_conversion':'4C1972→4F1592 thunk→7741AC20 outside fixed PE; actual integer rounding/output unknown. No guessed native935 LE16 LUT emitted.',
        'boundary':record(OUT/'935-screen-expression-boundary.json'),'guard_witnesses':guard_refs['935_rect_RGB555_body']+guard_refs['935_rect_RGB565_body']},
      'pitch_and_bounds':{'descriptor':'0x6C-byte zeroed local filled via surface virtual+64; fields read as height at+8,pitch at+10,buffer at+24. Implementation stays outside this contract.',
        'effective_stride':'2*(signed pitch>>1); odd positive pitch rounded down to even, consistently for all six bodies. Destination start=buffer+y*stride+2*x.',
        'end_pointer_global':'Each body publishes buffer+surfaceHeight*stride to831560 for the bottom pointer comparison; no live descriptor/global epoch observed.',
        'bottom_guard':'unsigned32 pointer comparison: buffer+(y+drawHeight)*stride <= buffer+surfaceHeight*stride. Reject whole draw when greater; no partial bottom clip. Computation occurs before x.',
        'rect_guard':'drawHeight=rect.bottom-rect.top; source begins IMG+8+2*(rect.top*IMGwidth+rect.left), destination begins buffer+y*stride+2*x.',
        'not_full_clipping':'No right-bound check; opaque/keyed/rect do not guard negative x/y. Native arithmetic is32bit and may wrap; code-derived examples use a padded audit buffer, not proof arbitrary coordinates are safe.'},
      'qualified_assets':assets,'namespace_and_variants':{'binding_inherited':record(ROOT/'skill-pane-frame-coordinate-contract.json'),
        'default_shards':{'933.img':sum(b'933.img')&15,'935.img':sum(b'935.img')&15},'index_candidates':extraction['index_candidates_only'],
        'load_order':'Try language variant hashed shard/base overlay; variant miss triesdefault. Defaulthashed shard absent in sealed indices; nativeLTsurf before sharedSprite.933c/933h exist sharedSprite, may win beforedefault.',
        '933_key_effect':'Exactnative933flag0,key4E73 occurs92 times.555alpha skips92;565loader leaveskey4E73 while all pixelsconvert, so this candidate has0 comparisons equalkey. Opaque31 copiesall92key pixels inbothformats.',
        '935_key_effect':'Exactnative935flag1,key0 has50008 zero words; key remains0 under565, so both keyed rect formats skipthem.',
        'scope':'Each exactdefault933/935 native member extracted once. No shared/variant payloads and no934 payload/model replay.'},
      'qualified_layer_geometry':record(OUT/'layer-geometry.json'),
      'surface_boundary':{'calls':surface_calls,'missing':'surface object/global6476D0 live vtable, virtual+64/+80/+6C implementations and valid returned descriptor/lock success. No native runtime or surface behavior claim.',
        'error_flow':'HRESULT-like signed negative paths compare887601C2, restore via+6C/retry+64, then privatecall/retry. Final retry return is not checked before descriptor use; success cannot be inferred.'},
      'private_error_calls':private_calls,'unresolved_FPU_conversion':{'last_visible':w(0x4F1593),'destination':mapping(pe,0x7741AC20),'no_external_body_filled':True},
      'new_wire_differences':0,'new_opcodes':0,'new_archive_members_extracted':2,'native_GUI_accepted':False,'overall_unpacking_accepted':False,
      'constraints':{'owned_directory_only':True,'native_launch_or_observer':False,'live_process_input_injection_screenshot':False,'other_group_edits':False,'S0_policy_or_history_takeover':False,'branch_expansion_stopped':True}}
    write('skill-pane-layer-composition-contract.json',report)
    print(json.dumps({'report':record(ROOT/'skill-pane-layer-composition-contract.json'),'new_ranges':len(ranges),'hardcoded_assertions':len(expected),'instruction_witnesses':len(assertions),
      'members':[(a['width'],a['height'],a['flag'],a['key_RGB555'])for a in assets],'cases':len(cases),'external_screen_rounding_closed':False},ensure_ascii=False))

if __name__=='__main__':main()
