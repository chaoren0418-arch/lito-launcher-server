"""Finite, static initialization audit. Never executes client code or extracts archives."""
from audit import *

OUT = ROOT/'skill-pane-render-initialization'
FIXED = '4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'

def load(n): return json.loads((ROOT/n).read_text(encoding='utf-8'))
def udiv3(n): return ((n*0xAAAAAAAB)>>32)>>1
def signed8(n): return n if n<128 else n-256
def words(values): return struct.pack('<'+'H'*len(values),*values)

def make_mark_lut(flag, reciprocal=True):
    div=udiv3 if reciprocal else lambda n:n//3
    low=[];high=[]
    for j in range(256):
        # 41A2D8: ECX=&green_low, EDX=&blue; callback 4EC040.
        b=min(div(2*(j&31)),31)
        g=min(div(2*(j>>5)+7),63 if flag else 31)
        low.append((g<<5)+b)
    for j in range(256):
        # 41A362: ECX=&red, EDX=&green_high; callback 4EC070.
        r=((j>>3) if flag else (j>>2))&31
        gh=(j&(7 if flag else 3))<<3
        r=min(div(2*r+31),31)
        gh=div(2*gh+(56 if flag else 24))
        gl=low[j]>>5  # Builder's same-index low-table saturation check, not current pixel low byte.
        if gl+gh>(63 if flag else 31): gh=(63 if flag else 31)-gl
        high.append((((r<<(6 if flag else 5))+gh)<<5)&65535)
    return low+high

def timed_tables(flag):
    # 4C18E0's relevant 32x32 loop projection: a=var_4, b=EDI, ESI=64*a+2*b.
    tables=[[],[],[]]
    a=0
    while a<32:
        b=0
        while b<32:
            v=a+(b//2)
            for table,shift in zip(tables,[0,6 if flag else 5,11 if flag else 10]):
                table.append((v<<shift)&65535)
            b+=1
        a+=1
    assert tables==[[((a+b//2)<<s)&65535 for a in range(32)for b in range(32)]for s in [0,6 if flag else 5,11 if flag else 10]]
    return tables

def timed_pixel(v,color,flag,tables):
    shifts=[0,6 if flag else 5,11 if flag else 10]
    return tables[0][(color&31)*32+(v&31)] | tables[1][((color>>shifts[1])&31)*32+((v>>shifts[1])&31)] | tables[2][((color>>shifts[2])&31)*32+((v>>shifts[2])&31)]

def stream_draw(raw,flag,pitch,caller_xy,base,normal=False):
    # Bounded synthetic inputs only. Native routine lacks these audit-model bounds assertions.
    buf=bytearray(b'\xA5'*256);writes=[];p=4
    dx,dy,h=signed8(raw[0]),signed8(raw[1]),signed8(raw[3])
    stride=(pitch//2)*2 if normal else pitch
    start=base+caller_xy[1]*pitch+caller_xy[0]*2+dy*stride+dx*2
    tables=timed_tables(flag)
    for y in range(max(h,0)):
        dest=start+y*stride;n=raw[p];p+=1
        for _ in range(n):
            skip,count=raw[p:p+2];p+=2
            dest+=(skip//2)*2 if normal else skip
            for _ in range(count):
                v=int.from_bytes(raw[p:p+2],'little');p+=2
                out=v if normal else timed_pixel(v,0,flag,tables)
                assert 0<=dest<=len(buf)-2
                buf[dest:dest+2]=out.to_bytes(2,'little')
                writes.append({'byte_offset':dest,'input_word':f'{v:04X}','output_word':f'{out:04X}'})
                dest+=2
    assert p==len(raw)
    return bytes(buf),writes,start

def main():
    OUT.mkdir(exist_ok=True)
    fixed=record(PE);assert fixed['sha256']==FIXED
    frozen={
      'class4-slot-provenance.json':'845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8',
      'S1E-initialization-gates.json':'841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC',
      'skill-gates/ranges.json':'885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C',
      'sealed-batch3/accepted-20261005/snapshot-manifest.json':'140302D62027784D900DC628B550255F44E50F8D1275AA17B5321272C63C1DF0',
      'S1E-receive-selection-boundary.json':'3B5357B9D193EC9A93F93611FE5C33AC8761F7BB9B6CBA584FD87D517114440E',
      'native-icon-draw-contract.json':'9BA690374734FECFE86CF95651E864515FFFDC78D71FE58C556974625AD95803',
      'skill-pane-layout-contract.json':'45EBDED431F925A0F38066702E67F0AD79E9139939D793E201A96EEBD15E681F',
      'server-packet-delta.json':'B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'}
    preserved=[{**record(ROOT/n),'expected_sha256':s,'match':record(ROOT/n)['sha256']==s}for n,s in frozen.items()];assert all(p['match']for p in preserved)
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    specs=[('timed_blitter_RGB565',0x4C1060,0x106,'code'),('timed_blitter_RGB555',0x4C1170,0x109,'code'),
      ('dispatch_and_timed_table_initializer',0x4C18E0,0x7EE,'code'),('dispatch_initializer_direct_caller_gate',0x482088,0x15,'code'),
      ('mark_LUT_builder',0x41A2B0,0x12D,'code'),('mark_low_callback',0x4EC040,0x25,'code'),('mark_high_callback',0x4EC070,0x43,'code'),
      ('mark_LUT_initializer_prefix',0x4EC290,0x30,'code'),('mark_initializer_direct_call',0x46D1DC,5,'code'),
      ('timed_surface_wrapper',0x4B9A90,0x53,'code'),('other_named_timed_pointer_consumer',0x488E41,0x16,'code'),
      ('pointer_snapshot',0x8300E4,4,'data'),('mark_LUT_snapshot',0x83673C,0x400,'data'),
      ('timed_blue_table_snapshot',0x7A789C,0x800,'data'),('timed_green_table_snapshot',0x7A809C,0x800,'data'),('timed_red_table_snapshot',0x7A889C,0x800,'data'),
      ('other_consumer_color_snapshot',0x503524,4,'data'),('format_flag_snapshot',0x641298,1,'data')]
    ranges=[];ins={};source={}
    for label,va,n,kind in specs:
        raw=pe.get_data(va-0x400000,n);decoded=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and (kind=='data'or sum(i.size for i in decoded)==n),(label,n,sum(i.size for i in decoded))
        out=OUT/f'ranges/{va:08X}.bin';out.parent.mkdir(exist_ok=True);out.write_bytes(raw)
        row={**mapping(pe,va),'label':label,'kind':kind,'bytes':n,'output':record(out),'decoded_bytes':sum(i.size for i in decoded),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in decoded]}
        ranges.append(row)
        for i in decoded:ins[i.address]=i;source[i.address]=row['output']
    write('skill-pane-render-initialization/ranges.json',{'fixed_PE':fixed,'ranges':ranges,'all_code_ranges_fully_decoded':True,'all_ranges_raw_backed':all(r['raw_backed']for r in ranges)})
    def w(va):
        i=ins[va]
        return {'fixed_PE_sha256':FIXED,**mapping(pe,va),'raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str,'source_range':source[va]}
    expected=[(0x482088,'call','0x4231d0'),(0x48208F,'jne','0x482098'),(0x482098,'call','0x4c18e0'),
      (0x4C18E6,'mov','al, byte ptr [0x641298]'),(0x4C18F8,'mov','dword ptr [ebp - 8], 6'),(0x4C1901,'mov','dword ptr [ebp - 0xc], 0xa'),
      (0x4C1908,'mov','dword ptr [ebp - 8], 5'),(0x4C1AB7,'sar','eax, 1'),(0x4C1AB9,'add','eax, ecx'),
      (0x4C1A9A,'add','esi, 2'),(0x4C1ABF,'mov','word ptr [esi + 0x7a789a], ax'),(0x4C1ACB,'mov','word ptr [esi + 0x7a889a], dx'),(0x4C1AE2,'mov','word ptr [esi + 0x7a809a], dx'),
      (0x4C1B70,'cmp','edi, 0x20'),(0x4C1B8C,'add','edx, 0x40'),(0x4C1B8F,'cmp','eax, -0x20'),
      (0x4C1E9A,'mov','al, byte ptr [0x641298]'),(0x4C1EA4,'je','0x4c1fbc'),
      (0x4C1EF0,'mov','dword ptr [0x8300e4], 0x4c1060'),(0x4C2002,'mov','dword ptr [0x8300e4], 0x4c1170'),
      (0x4C1066,'movsx','eax, byte ptr [ecx + 1]'),(0x4C1071,'movsx','esi, byte ptr [ecx]'),(0x4C1074,'movsx','edx, byte ptr [ecx + 3]'),
      (0x4C10AF,'add','eax, ecx'),(0x4C10F0,'shr','esi, 6'),(0x4C1100,'mov','si, word ptr [esi*2 + 0x7a809c]'),(0x4C1120,'mov','word ptr [eax], si'),
      (0x4C114E,'add','eax, edx'),(0x4C11BF,'add','eax, ecx'),(0x4C1214,'and','ebx, 0x1f'),(0x4C1239,'mov','word ptr [eax], di'),
      (0x4B9AB6,'push','ecx'),(0x4B9AB7,'push','eax'),(0x4B9ACC,'call','dword ptr [0x8300e4]'),
      (0x488E41,'test','ah, 2'),(0x488E46,'mov','eax, dword ptr [0x503524]'),(0x488E4F,'call','dword ptr [0x8300e4]'),
      (0x46D1DC,'call','0x4ec290'),(0x4EC2AC,'push','0x4ec070'),(0x4EC2B1,'mov','edx, 0x4ec040'),(0x4EC2B6,'mov','ecx, 0x83673c'),(0x4EC2BB,'call','0x41a2b0'),
      (0x41A2D8,'call','esi'),(0x41A312,'mov','word ptr [ebx], ax'),(0x41A318,'cmp','edi, 0x100'),
      (0x41A362,'call','dword ptr [ebp + 8]'),(0x41A3C0,'mov','word ptr [esi + 0x1fe], ax'),(0x41A3C8,'cmp','edi, 0x100'),
      (0x4EC045,'lea','edx, [eax + eax + 7]'),(0x4EC075,'lea','edx, [eax + eax + 0x1f]'),(0x4EC08F,'lea','ecx, [eax + eax + 0x38]'),(0x4EC0A7,'lea','ecx, [edx + edx + 0x18]')]
    assertions=[]
    for va,mn,op in expected:
        i=ins[va];assert(i.mnemonic,i.op_str)==(mn,op),(hex(va),i.mnemonic,i.op_str)
        assertions.append({**w(va),'match':True})
    # Include all visible named symbol references and exact bounded ASM helper context.
    lines=ASM.read_text(encoding='utf-8').splitlines()
    refs=[{'line':i+1,'text':s}for i,s in enumerate(lines)if '8300E4'in s or '83673C'in s]
    excerpts=[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}for a,b in [(55511,55688),(404019,404295),(405163,405428),(405672,405744),(495028,495088),(495313,495328),(276757,276770),(232847,232861),(290639,290649)]]
    write('skill-pane-render-initialization/asm-excerpts.json',{'source':record(ASM),'role':'supporting names only; fixed PE raw bytes govern','references':refs,'excerpts':excerpts})
    inherited=load('skill-pane-layout/ranges.json')
    inherited_row=next(r for r in inherited['ranges']if r['label']=='grid_row_render')
    row_ins=inherited_row['instructions']
    row_color=next(i for i in row_ins if i['va']=='004C901D')
    preceding=[i for i in row_ins if 0x4C8FF0<=int(i['va'],16)<=0x4C901D]
    assert any(i['mnemonic']=='push' and i['operands']=='0'for i in preceding)
    artifacts=[];vectorcases=[]
    samples=[0,1,0x1F,0x3E0,0x7C00,0x7FFF,0x8000,0xFFFF,0x1234]
    # Synthetic stream, no previous icon/packet decoding is rerun.
    synthetic=bytes([255,1,7,2,1,3,3])+words([0,0x7FFF,0x8000])+bytes([1,2,2])+words([0x421,0x3E0])
    (OUT/'synthetic-rle.bin').write_bytes(synthetic);artifacts.append(record(OUT/'synthetic-rle.bin'))
    for flag in [0,1]:
        lut=make_mark_lut(flag);assert lut==make_mark_lut(flag,False)
        folder=OUT/('RGB565'if flag else 'RGB555');folder.mkdir(exist_ok=True)
        p=folder/'selection-83673C.le16';p.write_bytes(words(lut));artifacts.append(record(p))
        tables=timed_tables(flag)
        for name,va,table in zip(['blue','green','red'],[0x7A789C,0x7A809C,0x7A889C],tables):
            p=folder/f'timed-{name}-{va:08X}.le16';p.write_bytes(words(table));artifacts.append(record(p))
        pixels=[{'input':f'{v:04X}','timed_color_zero':f'{timed_pixel(v,0,flag,tables):04X}','selected_word':f'{(lut[v&255]+lut[256+(v>>8)])&65535:04X}'}for v in samples]
        # Independent scalar channel equation for color=0, including 565 green low-bit loss.
        for v in samples:
            shifts=[0,6 if flag else 5,11 if flag else 10]
            scalar=sum((((v>>s)&31)//2)<<s for s in shifts)
            assert scalar==timed_pixel(v,0,flag,tables)
        assert pixels[0]['selected_word']==('5280'if flag else '2940')
        cases=[]
        for pitch in [16,17]:
            rendered,writes,start=stream_draw(synthetic,flag,pitch,[2,1],32)
            p=folder/f'timed-synthetic-pitch{pitch}.bin';p.write_bytes(rendered);artifacts.append(record(p))
            normal,normalwrites,normalstart=stream_draw(synthetic,flag,pitch,[2,1],32,True)
            # Normal comparison is the inherited batch5 equation applied to new synthetic bytes.
            assert [x['byte_offset']for x in writes]==[start+3,start+5,start+7,start+pitch+2,start+pitch+4]
            assert writes[0]['output_word']=='0000' and writes[2]['output_word']==('4000'if flag else '0000')
            cases.append({'pitch_bytes':pitch,'caller_xy':[2,1],'destination_base':32,'header_signed_dxdy':[-1,1],'header_width_unused':7,
              'timed_start_byte':start,'timed_writes':writes,'normal_start_byte':normalstart,'normal_writes':normalwrites,'timed_destination':record(p),
              'normal_equation_reference':record(ROOT/'native-icon-draw-contract.json'),'new_synthetic_case':True,'native_execution_oracle':False})
        vectorcases.append({'format_flag':flag,'format':'RGB565'if flag else 'RGB555','selection_LUT_words':512,'timed_table_words_each':1024,
          'reciprocal_mul_vs_integer_divide_all_LUT_entries_equal':True,'timed_projected_loop_vs_index_formula_all_entries_equal':True,'pixel_cases':pixels,'placement_cases':cases})
    write('skill-pane-render-initialization/code-derived-cases.json',{'status':'CODE_DERIVED_ONLY','artifacts':artifacts,'cases':vectorcases,'no_native_execution':True,'no_old_icon_or_packet_test_replay':True})
    data=[{'fixed_PE_sha256':FIXED,**mapping(pe,va),'bytes':n,'raw_sha256':sha(pe.get_data(va-0x400000,n)),
           'all_zero':not any(pe.get_data(va-0x400000,n)),'raw_hex_if_small':pe.get_data(va-0x400000,n).hex()if n<=4 else None,'snapshot_only':True}for label,va,n,k in specs if k=='data']
    verify={'fixed_PE':fixed,'range_count':len(ranges),'code_range_count':sum(r['kind']=='code'for r in ranges),'data_range_count':len(data),
      'range_bytes_and_hashes_match':all((Path(r['output']['path']).read_bytes()==pe.get_data(int(r['rva'],16),r['bytes'])and record(Path(r['output']['path']))==r['output'])for r in ranges),
      'all_code_ranges_fully_decoded':True,'instruction_assertions':assertions,'data_snapshots':data,'preserved_prior_primary_pins':preserved,
      'no_native_execution':True,'no_new_archive_reads_or_extraction':True,'new_wire_differences':0,'new_opcodes':0}
    assert verify['range_bytes_and_hashes_match']
    write('skill-pane-render-initialization/new-evidence-verification.json',verify)
    report={'schema':'lito.finite-skill-pane-render-initialization.v1','updated_at':stamp(),'status':'FINITE_STATIC_INITIALIZATION_CONTRACT_COMPLETE',
      'scope':'8300E4 and 83673C visible initialization/write sources, necessary table dependencies and direct callers only',
      'sources':{'fixed_PE':fixed,'BIN_inherited':load('skill-pane-layout-contract.json').get('sources',{}),'ASM':record(ASM)},
      'prior_contracts':preserved,'authority':'Fixed-PE raw bytes; ASM labels support navigation. Different recovered ASM input is not substituted for this PE.',
      'evidence':{'ranges':record(OUT/'ranges.json'),'verification':record(OUT/'new-evidence-verification.json'),'ASM_context':record(OUT/'asm-excerpts.json'),'cases':record(OUT/'code-derived-cases.json')},
      'timed_pointer_8300E4':{
        'snapshot':next(d for d in data if d['va']=='008300E4'),
        'direct_initializer':'004C18E0','direct_caller':w(0x482098),'caller_gate':[w(0x482088),w(0x48208F)],
        'dispatch_condition_witnesses':[w(0x4C1E9A),w(0x4C1EA4)],
        'writes':[{'condition':'byte[641298] != 0','target':'004C1060','format':'RGB565','witness':w(0x4C1EF0)},
                  {'condition':'byte[641298] == 0','target':'004C1170','format':'RGB555','witness':w(0x4C2002)}],
        'needed_table_writes':[w(0x4C1ABF),w(0x4C1ACB),w(0x4C1AE2)],
        'table_loop_witnesses':[w(0x4C1A9A),w(0x4C1AB7),w(0x4C1AB9),w(0x4C1B70),w(0x4C1B8C),w(0x4C1B8F)],
        'table_reconstruction':'For a,b in 0..31: index=32*a+b; q=a+floor(b/2). LE16 tables at7A789C=q,7A809C=q<<(flag?6:5),7A889C=q<<(flag?11:10). Each1024words. No saturation at these stores.',
        'initializer_condition':'Uses format flag at entry for shifts and rereads it before dispatch publication; reconstruction assumes flag remains stable during initialization and drawing.',
        'format_flag_source':'Inherit batch5:4231F0 recognizes masks7C00/03E0/001F→0 andF800/07E0/001F→1. No format query or old tests replayed.',
        'direct_named_consumers':[{'call':w(0x4B9ACC),'wrapper':'004B9A90','skill_row_call_inherited':row_color,'inherited_row_range':inherited_row['output'],
          'color_argument_evidence_inherited':preceding,'skill_color_argument':0},
          {'call':w(0x488E4F),'branch':w(0x488E41),'color_read':w(0x488E46),'color_global':'00503524','live_color_unknown':True,'boundary':'Direct consumer only; upstream sprite/attribute semantics and color writer not expanded.'}],
        'read_write_contract':{'inputs':'ECX=RLE payload, EDX=destination caller-origin address, stack arg0=pitch bytes,arg4=color WORD',
          'offset':'signed i8 dx,dy; dest=caller_dest+dy*pitch+2*dx; offset applied once',
          'height':'signed i8 header[3], <=0 skips; header width unused',
          'rows':'u8 span count; each span advances by unsigned skip byte exactly, then copies/transforms count u16 source words; next row start += pitch exactly',
          'pixels':'Extract5-bit B,G,R at shifts(0,5,10) for555 or(0,6,11) for565. Each table index=color_channel*32+source_channel, OR three LUT words; write every covered word including zero. Does not read destination pixel.',
          'skill_color_zero':'B=floor(B5/2); R=floor(R5/2); G555=floor(G5/2). For565, G6out=2*floor(G6in/4), discarding original green low bit. RGB555 ignores source bit15;RGB565 bit15 belongs to red.',
          'mask':'No 6476C4 or other coverage-mask writes in either complete body; color arg is not the normal mask flag.',
          'bounds':'No explicit alpha/key/clip/scale in these two complete routines; valid source/destination spans assumed. Surface virtual implementations remain unknown.',
          'normal_differences':'Batch5 normal4BE000 copies covered words verbatim, uses2*floor(skip/2) and2*floor(pitch/2); timed uses exact skip and pitch. Signed offsets agree on even pitch; odd skip/pitch byte placements differ. No resource validity claim for synthetic odd cases.'}},
      'selection_LUT_83673C':{
        'snapshot':next(d for d in data if d['va']=='0083673C'),'direct_initializer':'004EC290','direct_caller':w(0x46D1DC),
        'table_binding':w(0x4EC2B6),'builder_call':w(0x4EC2BB),'callback_bindings':[w(0x4EC2AC),w(0x4EC2B1)],
        'low_callback':'004EC040','high_callback':'004EC070','builder':'0041A2B0',
        'writes':[w(0x41A312),w(0x41A3C0)],'extent':'512 WORDs=1024bytes, table[0..255] at83673C andtable[256..511] at83693C',
        'formula_low':'For byte j: B=floor(2*(j&31)/3), GL=floor((2*(j>>5)+7)/3); clampB31 andGL31/63; low[j]=B+(GL<<5).',
        'formula_high':'R=floor((2*((j>>(flag?3:2))&31)+31)/3); GH=floor((2*((j&(flag?7:3))<<3)+(flag?56:24))/3); Rclamp31. Builder comparesGH+(low[j]>>5) against31/63 and reducesGH if needed. high[j]=(R<<(flag?11:10))+(GH<<5), modulo65536.',
        'clamp_index_detail':'High-loop saturation reads low[j] at same loop index j, not the low byte of a future input pixel. This callback set never reaches saturation forj0..255 in eitherformat; reconstruct builder behavior explicitly.',
        'consumer_inherited':'Batch6 grid4CD290 first draws icon then mark!=0 calls4B9DD0 for(x-1,y-1),34x34; newword=(low[v&255]+high[v>>8])&FFFF. Applies to every destination word in rectangle, includingbackground/uncoveredpixels.',
        'interpretation':'Channel-specific color remap. Black maps2940 in555 or5280 in565; not grayscale, fixedborder, alpha or availability proof. State is row+0C selectionmark; Modern choice_none dim is not justified by this initializer.',
        'format_condition':'Requires641298 stable throughout both builderloops and callbackreads; no concurrency/epoch guarantee established.',
        'initialization_order_boundary':'Visible direct caller46D1DC invokes4EC290; report does not prove this live session reached it or its order relative to firstpane draw. Existing fixed snapshot stillzero; no new live/native observation.'},
      'reusable_outputs':artifacts,'unknowns':[
        {'boundary':'00482098→004C18E0 and0046D1DC→004EC290','missing_evidence':'Actual session reachability/order, stable641298 epoch and postinitialization live values; not missing static target bodies.'},
        {'boundary':'004B9AA7 virtualsurface+64;004B9AD7 virtualsurface+80','missing_evidence':'External surface method implementations, runtime success, globalbase/pitch and fullsurface clipping; no claim of globalno-clip.'},
        {'boundary':'00488E46 reads00503524','missing_evidence':'Live color and upstream attribute meaning; stops at directconsumer, doesnot tracecolorwriters.'},
        {'boundary':'Batch6 remainingframewidth/globaltranslation/paneregistry/inputcallstack','missing_evidence':'NativeGUI/input parity remainsunaccepted; initializationreport doesnot fillthese.'},
        {'boundary':'ASM named references only','missing_evidence':'No proof that arbitrary computed/aliased writes to these globals are absent; no fullprogramreverse attempted.'}],
      'static_body_gaps':[],'private_missing_range_required_for_this_contract':None,
      'new_wire_differences':0,'new_opcodes':0,'overall_unpacking_accepted':False,'native_GUI_parity_accepted':False,
      'work_constraints':{'writes_only_owned_resource_directory':True,'native_or_observer_started':False,'new_archive_reads_or_extraction':False,'Modern_Java_SQL_changed':False,'old_PASS_icon_packet_replay':False}}
    # Keep source metadata compact; inherited report pin is the authority for prior conclusions.
    report['sources']['BIN_inherited']=record(BIN)
    assert report['sources']['BIN_inherited']['sha256']=='CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'
    write('skill-pane-render-initialization-contract.json',report)
    print(json.dumps({'report':record(ROOT/'skill-pane-render-initialization-contract.json'),'ranges':len(ranges),'instruction_assertions':len(assertions),'data_snapshots':len(data),'new_wire_differences':0},ensure_ascii=False))

if __name__=='__main__':main()
