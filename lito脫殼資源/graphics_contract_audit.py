"""Finite graphics contract audit. Reuses four checked vectors and adds shared fallback91.
No native process, game, DLL, or Modern decoder is executed. Single-member extraction is separately authorized.
"""
from audit import *
from extract_member import load_catalogue

OUT=ROOT/'graphics-parity'
MODERN=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'

def load(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))
def save_bytes(name,data):
    p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);return record(p)
def signed(v):return v if v<128 else v-256
def convert(v):return (v+(v&0x7FE0))&0xFFFF

def parse_spans(raw):
    # Explicit safety bounds are audit-tool policy, not claims about native validation.
    dx,dy,hw,h=raw[:4];assert 0<h<128
    pos=4;rows=[];width=hw
    for y in range(h):
        row_offset=pos;n=raw[pos];pos+=1;x=0;spans=[]
        for span in range(n):
            skip,count=raw[pos:pos+2];pos+=2;x+=skip>>1
            start=pos;values=list(struct.unpack_from('<'+'H'*count,raw,pos));pos+=2*count
            spans.append({'span':span,'skip_bytes':skip,'skip_words':skip>>1,'x':x,'count':count,'payload_offset':start,'words':values})
            x+=count;width=max(width,x)
        rows.append({'y':y,'encoded_offset':row_offset,'span_count':n,'spans':spans,'row_end_offset':pos})
    assert pos==len(raw)
    return {'offset_raw':[dx,dy],'offset_signed':[signed(dx),signed(dy)],'header_width':hw,'width_for_vector':width,'height':h,'bytes_consumed':pos,'rows':rows}

def stream_blit(raw,pitch,origin,canvas):
    # Separate streaming model of 4BE000's flag=0 branch, assuming DF=0 and safe in-bounds input.
    result=bytearray(canvas);pos=4
    row_dest=origin[1]*pitch+origin[0]*2+2*(signed(raw[1])*(pitch>>1)+signed(raw[0]))
    writes=[]
    for y in range(signed(raw[3])):
        spans=raw[pos];pos+=1;dest=row_dest
        for _ in range(spans):
            skip,count=raw[pos:pos+2];pos+=2;dest+=2*(skip>>1)
            n=count*2;assert 0<=dest<=len(result)-n
            result[dest:dest+n]=raw[pos:pos+n]
            writes.append({'row':y,'destination_byte_offset':dest,'source_byte_offset':pos,'bytes':n})
            pos+=n;dest+=n
        row_dest+=2*(pitch>>1)
    assert pos==len(raw)
    return bytes(result),writes

def vector(namespace,name,path,provenance,prior_details):
    raw=path.read_bytes();d=parse_spans(raw);w,h=d['width_for_vector'],d['height']
    words=[0]*(w*h);coverage=bytearray(w*h);mutated=bytearray(raw)
    for row in d['rows']:
        for span in row['spans']:
            for k,v in enumerate(span['words']):
                at=row['y']*w+span['x']+k;words[at]=v;coverage[at]=1
                struct.pack_into('<H',mutated,span['payload_offset']+2*k,convert(v))
    if prior_details:
        assert sha(coverage)==prior_details['coverage_sha256']
        assert sum(coverage)==prior_details['encoded_pixels']
        assert sum(c and v==0 for c,v in zip(coverage,words))==prior_details['zero_pixels']
    prefix=f'vectors/{namespace}-{name}'
    files={'raw':save_bytes(prefix+'/raw.bin',raw),'inplace_565_payload':save_bytes(prefix+'/converted-565.bin',mutated),
           'coverage_u8':save_bytes(prefix+'/coverage.u8',coverage),
           'dense_source_words_le16':save_bytes(prefix+'/source-words.le16',struct.pack('<'+'H'*len(words),*words)),
           'dense_converted_words_le16':save_bytes(prefix+'/converted-words.le16',struct.pack('<'+'H'*len(words),*(convert(v)for v in words)))}
    pitch=128;origin=[8,8];canvas_width=64;canvas_height=64;sentinel=0x5AA5
    initial=struct.pack('<H',sentinel)*(canvas_width*canvas_height)
    checks=[]
    for flag,payload in [(0,raw),(1,bytes(mutated))]:
        expected=bytearray(initial)
        for row in d['rows']:
            for span in row['spans']:
                for k,v in enumerate(span['words']):
                    px=origin[0]+d['offset_signed'][0]+span['x']+k
                    py=origin[1]+d['offset_signed'][1]+row['y']
                    assert 0<=px<canvas_width and 0<=py<canvas_height
                    struct.pack_into('<H',expected,py*pitch+px*2,convert(v)if flag else v)
        actual,writes=stream_blit(payload,pitch,origin,initial)
        assert actual==expected
        files[f'destination_flag_{flag}']=save_bytes(prefix+f'/destination-flag-{flag}.le16',actual)
        checks.append({'conversion_flag':flag,'independent_stream_vs_span_model_equal':True,'writes':writes})
    zero_examples=[{'x':i%w,'y':i//w,'native_written_word':0,'reference_rgba_alpha_policy':0}for i,(c,v)in enumerate(zip(coverage,words))if c and v==0][:8]
    holes=[{'x':i%w,'y':i//w,'native_destination_unchanged':True}for i,c in enumerate(coverage)if not c][:8]
    body={**d,'namespace':namespace,'member':name,'source':record(path),'provenance':provenance,
          'prior_layout_details_compared':prior_details,'coverage_words':sum(coverage),
          'covered_zero_words':sum(c and v==0 for c,v in zip(coverage,words)),
          'uncovered_words':len(coverage)-sum(coverage),'zero_examples':zero_examples,'uncovered_examples':holes,
          'raw_offset_applied_only_by_draw':True,'files':files,
          'destination_case':{'width':canvas_width,'height':canvas_height,'pitch_bytes':pitch,'caller_origin':origin,'sentinel_word_hex':f'{sentinel:04X}','mask_flag':0,'DF':0,'in_bounds':True},
          'new_case_checks':checks,'oracle_status':'CODE_DERIVED_EXPECTATIONS_ONLY; no original runtime pixels or Modern output used'}
    write('graphics-parity/'+prefix+'/vector.json',body)
    return record(OUT/prefix/'vector.json')

def main():
    OUT.mkdir(exist_ok=True)
    baseline=load('baseline.json')['sources'];assert all(record(Path(x['path']))['sha256']==x['expected_sha256']for x in baseline)
    frozen={'class4-slot-provenance.json':'845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8',
            'S1E-initialization-gates.json':'841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC',
            'skill-gates/ranges.json':'885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C',
            'S1E-receive-selection-boundary.json':'3B5357B9D193EC9A93F93611FE5C33AC8761F7BB9B6CBA584FD87D517114440E',
            'sealed-batch3/accepted-20261005/snapshot-manifest.json':'140302D62027784D900DC628B550255F44E50F8D1275AA17B5321272C63C1DF0'}
    assert all(record(ROOT/n)['sha256']==s for n,s in frozen.items())
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    specs=[('format_flag_setup',0x4231F0,0x155),('inplace_word_conversion',0x43DA40,0x67),
           ('shared_tbt_load_loop',0x43F91E,0xD2),('lticon_lbt_load_loop',0x43FA93,0x6D),
           ('shared_filename_shard',0x43FFC0,0x41),('skill_row_constructor',0x4C8DE0,0x5A),
           ('skill_row_render_prefix',0x4C8EA0,0x6C),('lticon_draw_caller',0x493640,0x39),
           ('surface_draw_wrapper',0x4B9980,0x51),('span_word_blitter',0x4BE000,0x107),
           ('formats',0x508880,0x20),('skill_row_vtable',0x4FA4EC,0x10)]
    ranges=[];instructions={}
    for label,va,n in specs:
        raw=pe.get_data(va-0x400000,n);kind='data' if label in ['formats','skill_row_vtable']else'code'
        ins=[] if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and (kind=='data' or sum(i.size for i in ins)==n), (label,n,sum(i.size for i in ins))
        row={**mapping(pe,va),'label':label,'kind':kind,'bytes':n,'output':save_bytes(f'ranges/{va:08X}.bin',raw),
             'decoded_bytes':sum(i.size for i in ins),'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in ins]}
        ranges.append(row);instructions.update({i.address:i for i in ins})
    assertions=[(0x42322F,'mov','ebx, 1'),(0x4232C3,'cmp','eax, 0x7c00'),(0x4232DF,'mov','byte ptr [0x641298], 0'),
                (0x423310,'cmp','eax, 0xf800'),(0x423317,'cmp','dword ptr [ebp - 0x10], 0x7e0'),(0x42332C,'mov','byte ptr [0x641298], bl'),
                (0x43DA53,'jle','0x43daa3'),(0x43DA7F,'and','ebx, 0x7fe0'),(0x43DA85,'add','ebx, edx'),(0x43DA88,'mov','word ptr [eax - 2], bx'),
                (0x43F9E2,'call','0x43da40'),(0x43FAE8,'call','0x43da40'),(0x4C8E10,'mov','dword ptr [esi + 0x6c], ecx'),
                (0x4C8EB8,'mov','edi, dword ptr [0x648ec8]'),(0x4C8F07,'call','0x4b9980'),(0x493650,'mov','eax, dword ptr [0x64a964]'),
                (0x493674,'call','0x4b9980'),(0x4B9997,'call','dword ptr [eax + 0x64]'),(0x4B99AA,'push','0'),(0x4B99BB,'call','0x4be000'),
                (0x4BE006,'movsx','eax, byte ptr [ecx + 1]'),(0x4BE012,'movsx','edi, byte ptr [ecx]'),(0x4BE020,'movsx','edx, byte ptr [ecx + 3]'),
                (0x4BE010,'sar','esi, 1'),(0x4BE054,'shr','ecx, 1'),(0x4BE070,'rep movsd','dword ptr es:[edi], dword ptr [esi]'),
                (0x4BE07A,'rep movsb','byte ptr es:[edi], byte ptr [esi]'),(0x4BE081,'je','0x4be0d2'),(0x4BE0EF,'add','ebx, edx')]
    checked=[]
    for va,mn,op in assertions:
        i=instructions[va];assert (i.mnemonic,i.op_str)==(mn,op)
        checked.append({'va':f'{va:08X}','raw_hex':i.bytes.hex(),'mnemonic':mn,'operands':op,'match':True})
    assert struct.unpack('<I',pe.get_data(0xFA4F0,4))[0]==0x4C8EA0
    assert pe.get_data(0x108880,7)==b'%d.lbt\0' and pe.get_data(0x108894,7)==b'%d.tbt\0'
    write('graphics-parity/ranges.json',{'source':record(PE),'ranges':ranges,'aligned_complete_decode':True})
    lt=load('resources/LTicon-decode-manifest.json');shared=load('skill-initialization/shared-icon-resources.json');archives=load('resources/archive-manifest.json')
    ltarchive=next(a for a in archives['archives']if a['stem']=='LTicon')
    vectors=[]
    for member in ['106.lbt','122.lbt','91.lbt']:
        r=next(r for r in lt['members']if r['member']==member)
        p=Path(r['source']['path']);assert record(p)==r['source']
        existing=OUT/f'vectors/LTicon-{member}/vector.json';v=json.loads(existing.read_text(encoding='utf-8'));assert v['source']==record(p)
        assert all(record(Path(x['path']))==x for x in v['files'].values());vectors.append(record(existing))
    r=next(r for r in shared['outputs']if r['name']=='383.tbt');p=Path(r['decoded']['path']);assert record(p)==r['decoded']
    existing=OUT/'vectors/shared-383.tbt/vector.json';v=json.loads(existing.read_text(encoding='utf-8'));assert v['source']==record(p)
    assert all(record(Path(x['path']))==x for x in v['files'].values());vectors.append(record(existing))
    extracted=load('graphics-parity/fallback91-extraction.json');p=Path(extracted['decoded']['path']);assert record(p)==extracted['decoded']
    vectors.append(vector('shared','91.tbt',p,{k:extracted[k]for k in ['archive','row','offset','bytes','source_index','source_pak','authorization','source_checks']},None))
    cat,tables=load_catalogue();filename='91.tbt';shard=sum(filename.encode('ascii'))%16;shardstem=f'Sprite{shard:02}'
    candidates=[]
    for stem in [shardstem,'Sprite']:
        for row in tables.get(stem,[]):
            if row['name']==filename:
                candidates.append({'archive':stem,**row,'source_index':cat[stem]['index_source'],'decoded_index':cat[stem]['decoded_index'],'source_pak':cat[stem]['pak_source'],'pin_status':'inherited previously verified container/index pins; no full archive rehash or member read this batch'})
    fallback={'logical_index':91,'namespace':'shared Sprite TBT','filename':filename,'cache_pointer_va':'00648EC8','count_va':'00648ECC',
             'hash_shard':shardstem,'shard_candidate_found':any(c['archive']==shardstem for c in candidates),'indexed_candidates':candidates,
             'prior_S1E_evidence':record(ROOT/'S1E-initialization-gates.json'),'row_vtable_render_va':'004C8EA0',
             'known_LTicon_91_is_not_a_substitute':True,'previously_extracted_shared_91_sample_found':False,'raw_decoded_vector_available':True,
             'single_member_supplemental_authorization':extracted['authorization'],'extraction':record(OUT/'fallback91-extraction.json'),'actual_vector':vectors[-1],
             'source_payload':extracted['decoded'],'route':'Sprite12 member absence -> base Sprite row90223; original asset bytes, no pixel fabrication'}
    assert candidates and not fallback['shard_candidate_found']
    write('graphics-parity/fallback-91-namespace.json',fallback)
    modern_sources=[]
    for name,sections in [('inventory_icons.rs',[(280,419)]),('world_skill_panel.rs',[(5,22),(89,99),(174,194)]),('world_inventory_panel.rs',[(413,450)])]:
        path=MODERN/name;lines=path.read_text(encoding='utf-8').splitlines()
        modern_sources.append({'source':record(path),'excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}for a,b in sections]})
    write('graphics-parity/modern-comparison.json',{'sources':modern_sources,'Modern_executed':False,'used_as_original_oracle':False,
        'differences':[
          {'property':'covered zero','native':'4BE070/7A copies 0000 like any other covered word; uncovered spans leave destination unchanged','Modern':'reference_rgba drops covered RGB555 zero via alpha0; world_skill_panel uses this view'},
          {'property':'offset','native':'header[0]/[1] sign-extended i8, no scale in bounded 4B9980→4BE000 path','Modern':'stored u8 and converted to i32; grid scales offsets by5/4'},
          {'property':'format','native':'conditional in-place (v+(v&7FE0))&FFFF; flag0 keeps555, flag1 selected onF800/07E0/001F tuple','Modern':'raw555 retained; reference RGBA bit replication is a PakViewer presentation policy'},
          {'property':'row stride/width','native':'compressed row spans variable-length; draw ignores header[2], destination row stride2*(pitch_bytes>>1)','Modern':'dense width=max(header_width,row_end), independent width-based RGBA buffer'},
          {'property':'validation','native':'bounded routines have no EOF/oddskip/highbit/headerwidth checks; height signed; malformed/negative behavior unvalidated','Modern':'reject oddskip/highbit/trailing/zero dimensions and imposes limits; these are valid safety policies but not native validation evidence'},
          {'property':'clip/alpha/scale','native':'no explicit clip rectangle, alpha blend, or scale in selected path; virtual surface/upstream behavior unresolved','Modern':'nearest sampling at5/4,42x42 grid clip plus surface bounds,RGBA alpha blend in world_inventory_panel::blit'}]})
    rawchecks=[{**r['output'],'match':record(Path(r['output']['path']))==r['output'] and Path(r['output']['path']).read_bytes()==pe.get_data(int(r['rva'],16),r['bytes'])}for r in ranges]
    assert all(x['match']for x in rawchecks)
    write('graphics-parity/new-evidence-verification.json',{'updated_at':stamp(),'fixed_sources_match':True,'raw_range_checks':rawchecks,'instruction_assertions':checked,
         'vtable_and_format_data_match':True,'accepted_batch3_batch4_primary_pins_unchanged':[{**record(ROOT/n),'expected_sha256':s,'match':True}for n,s in frozen.items()],
         'selected_existing_samples':4,'selected_total_actual_samples':5,'new_archive_members_extracted':1,'stream_and_span_models_equal_cases':10,'prior_four_coverage_layouts_match':True,
         'prior_four_sample_eight_cases_reused_without_decode_replay':True,'new_fallback91_cases_checked':2,'fallback91_archive_index_payload_checks':extracted['source_checks'],
         'Modern_decoder_or_tests_executed':False,'original_game_launched':False,'GUI_accepted':False,'native_runtime_output_oracle_available':False,'fallback_91_vector_missing':False})
    contract={'schema':1,'updated_at':stamp(),'fixed_PE':record(PE),'supporting_ASM':record(ASM),'ASM_warning':'Recovered ASM input differs from fixed PE; raw fixed-PE bytes govern. No private API identity promoted from labels.',
         'status':'FINITE_STATIC_DRAW_CONTRACT_AND_FIVE_ACTUAL_SAMPLE_VECTORS_COMPLETE','source_ranges':record(OUT/'ranges.json'),
         'namespace_contract':{'LTicon':{'filename':'%d.lbt','cache_va':'0064A964','load_range_va':'0043FA93','draw_call_va':'00493674'},
                               'shared':{'filename':'%d.tbt','cache_va':'00648EC8','load_range_va':'0043F91E','row_render_va':'004C8EA0','draw_call_va':'004C8F07'},
                               'fallback91':record(OUT/'fallback-91-namespace.json')},
         'decode_contract':{'header':'four bytes: dx i8,dy i8,width byte,height i8 at native draw. All selected real samples have height1..127.',
                            'payload':'Each row: u8 span_count; each span: u8 skip_bytes, u8 word_count, word_count LE16 values. Advance x by skip_bytes>>1 then word_count.',
                            'coverage':'Each span copies exactly word_count LE16 words; all covered zero/highbit values copied by raw blitter; uncovered areas preserve destination. Coverage is not native RGBA alpha.',
                            'format_flag':'4231F0 sees7C00/03E0/001F =>641298=0; F800/07E0/001F=>641298=1. Virtual query identity and live result remain unobserved.',
                            'conversion':'43DA40 rewrites each encoded word(v+(v&7FE0))&FFFF in place when loaders see641298!=0 and cache pointer!=0. ValidRGB555→RGB565 with green6 low bit0, not green replication.',
                            'preserve_raw':'Keep original source bytes and raw555 separate from converted mutable cache; do not apply conversion twice.'},
         'draw_contract':{'wrapper':'4B9980 acquires surface through virtual+64, draws only when returnEAX==0, then virtual+80. Exact external implementation unclosed.',
                          'globals':'63485C destination pointer;634848 pitch_bytes. Argument+08 pointer835B3C is unused in this wrapper, not proven clip data.',
                          'caller_origin':'base + y*pitch_bytes + x*2; x=[ebp+0C], y=[ebp+10]',
                          'header_origin':'add2*(dy*(pitch_bytes>>1)+dx); pitch SAR signed. Native row increment2*(pitch_bytes>>1), equals pitch for positive even pitch.',
                          'blit':'4BE000 flag0 performs REP MOVSD/MOVSB copying every encoded word. Headerwidth ignored. No alpha/colorkey/clip/scale check in captured wrapper+blitter.',
                          'optional_mask':'4BE07F tests caller flag;4B9980 pushes0. Other branch writes01 to6476C4 mask, not alpha; selected path does not execute it.',
                          'preconditions':'Code-derived vectors assume DF=0, positive even pitch, height1..127, complete payload and in-bounds destination. Native routines do not enforce these audit safety conditions.'},
         'reusable_vectors':vectors,'vector_semantics':'Raw payload, dense LE16 source/conditional565,coverage.u8,code-derived sentinel destination buffers and row/span/source/destination offsets. Binary LE16 outputs are not screenshots or RGBA-native oracles.',
         'modern_policy_comparison':record(OUT/'modern-comparison.json'),'new_checks':record(OUT/'new-evidence-verification.json'),
         'unclosed':['No observed native surface output, live pixel-format flag, pitch, placement or actual call stack; RGBA channel expansion and complete display parity not accepted.',
                    'External virtual surface methods, upstream clipping and private/error paths unclosed; captured no-clip path does not prove all native drawing lacks clipping.',
                    'Negative height, highbit pixels, odd skip, odd/negative pitch, malformed payload and DF!=0 are not validated behavioral cases.',
                    'No Modern source change or runtime comparison; overall unpacking/product/native first lobby/GUI remain unaccepted.'],
         'finite_static_contract_complete':True,'overall_complete':False,'native_graphics_runtime_parity_accepted':False,'all_requested_actual_samples_available':True,'new_opcodes':0,'wire_corrections':0}
    write('native-icon-draw-contract.json',contract)
    print(json.dumps({'contract':record(ROOT/'native-icon-draw-contract.json'),'raw_ranges':len(ranges),'instruction_assertions':len(checked),'actual_samples':len(vectors),'stream_span_cases':10,'new_cases':2,'prior_cases_reused':8,'fallback91_sample_gap':False},ensure_ascii=False))

if __name__=='__main__':main()
