"""Bounded skill-pane layout evidence. No native/Modern execution or resource extraction."""
from audit import *

OUT=ROOT/'skill-pane-layout'
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))
def truncdiv(a,b):return abs(a)//abs(b)*(1 if a*b>=0 else -1)

def placements(origin,count,start,step=(34,34)):
    # Mirrors 4CD290 normal-input iteration; rejects unsafe negative start in this audit model.
    assert start>=0 and count>=0
    x,y=origin;ox,oy=origin;w,h=step;rows=[];i=start
    while i<count:
        if y+h>oy+6*h:break
        rows.append({'index':i,'caller_origin':[x,y]})
        x+=w
        if x+w>ox+4*w:x=ox;y+=h
        i+=1
    return rows

def hit(point,rect,count,start,step=(34,34)):
    # Includes upstream half-open rectangle gate; 4CD480 alone does not contain this gate.
    x,y=point;l,t,r,b=rect
    if not(l<=x<r and t<=y<b):return {'gate':'OUTSIDE_UPSTREAM_RECT','index':None}
    row=truncdiv(y-t,step[1]);col=truncdiv(x-l,step[0])
    if row>8 or col>=4:return {'gate':'4CD480_UPPER_BOUND_REJECT','index':-1,'row':row,'column':col}
    index=start+row*4+col
    return {'gate':'IN_RECT','index':index if index<count else -1,'row':row,'column':col}

def scroll(start,count,action,position=None):
    if action=='up4':return start-4 if start-4>=0 else start
    if action=='down4':return start+4 if start+4<count else start
    if action=='page_up36':return truncdiv(max(0,start-36),4)*4 if start>0 else start
    if action=='page_down36':return truncdiv(min(start+36,count-1),4)*4 if start<count-1 else start
    if action=='callback_row':return position*4
    if action=='slider':
        p=max(36,min(position,231))
        return 0 if count<=1 else (truncdiv(count-1,4)*(p-36)//195)*4
    raise ValueError(action)

def main():
    OUT.mkdir(exist_ok=True)
    assert record(PE)['sha256']=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
    frozen={'class4-slot-provenance.json':'845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8',
      'S1E-initialization-gates.json':'841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC',
      'skill-gates/ranges.json':'885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C',
      'sealed-batch3/accepted-20261005/snapshot-manifest.json':'140302D62027784D900DC628B550255F44E50F8D1275AA17B5321272C63C1DF0',
      'S1E-receive-selection-boundary.json':'3B5357B9D193EC9A93F93611FE5C33AC8761F7BB9B6CBA584FD87D517114440E',
      'native-icon-draw-contract.json':'9BA690374734FECFE86CF95651E864515FFFDC78D71FE58C556974625AD95803',
      'server-packet-delta.json':'B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'}
    preserved=[{**record(ROOT/n),'expected_sha256':s,'match':record(ROOT/n)['sha256']==s}for n,s in frozen.items()];assert all(p['match']for p in preserved)
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    specs=[('world_cleanup_skill_pane',0x47E571,0x16),('pane_deleting_destructor',0x4CC7B0,0x21),('pane_destructor',0x4CC7E0,0x69),
      ('base_pane_destructor',0x4EB430,0x4E),('skill_list_deleting_destructor',0x4C91E0,0x21),('skill_list_destructor_thunk',0x4C9210,5),
      ('skill_list_destructor',0x4CD000,0x4E),('row_clear',0x450230,0x30),('list_base_destructor',0x450170,0x5A),
      ('admission_layout_constructor_calls',0x492D3A,0xA8),('skill_list_constructor',0x4C9190,0x44),('list_constructor',0x4CCED0,0xFC),
      ('base_list_constructor',0x450090,0xA5),('rectangle_constructor',0x4C2FC0,0x30),('pane_owner_binding',0x4EB3E0,0x1F),
      ('pane_constructor_prefix',0x4CC3B0,0x69),('child_container_constructor',0x41B310,0x5C),('pane_initial_right_alignment',0x4CC742,0x60),
      ('pane_grid_activation',0x4CC870,0x1E),('world_visible_pane_registry_draw',0x48A9E4,0x27),('pane_render',0x4CCB20,0x21E),
      ('skill_list_render_caller',0x4CBDB0,0x13),('skill_grid_render',0x4CD290,0x1E8),('grid_row_render',0x4C8F90,0xE2),
      ('text_row_render_tail',0x4C8F0C,0x7B),('row_selection_init',0x450BC0,0x19),('clear_selection_marks',0x4509A0,0x20),
      ('selection_mark_update',0x4CD6D0,0x5E),('grid_hit_index',0x4CD480,0x4D),('input_rectangle_gate',0x4CD730,0x31),
      ('hover_grid_index',0x4CD85B,0x77),('pane_drag_translation',0x4CC330,0x75),('scroll_up4',0x4CC890,0x57),('scroll_down4',0x4CC8F0,0x5A),
      ('page_up36',0x4CC950,0x7D),('page_down36',0x4CC9D0,0x81),('scroll_slider',0x4CCA60,0xB8),
      ('scroll_row_callback',0x4CC310,0x16),('scroll_direct_setter',0x4CD270,0xD),('marked_rectangle_LUT',0x4B9DD0,0xA4),
      ('timed_alternate_surface_wrapper',0x4B9A90,0x53),('row_vtable',0x4FA4EC,0x10),('skill_list_vtable',0x4FA50C,0x5C),
      ('pane_and_base_list_vtables',0x4FA568,0x64),('unresolved_timed_blitter_pointer',0x8300E4,4),('mark_LUT_snapshot',0x83673C,0x400)]
    ranges=[];ins_by_va={};range_by_va={}
    for label,va,n in specs:
        kind='data'if 'vtable'in label or label.endswith('pointer')or label.endswith('snapshot')else'code'
        raw=pe.get_data(va-0x400000,n);ins=[]if kind=='data'else list(cs.disasm(raw,va))
        assert len(raw)==n and (kind=='data' or sum(i.size for i in ins)==n),(label,n,sum(i.size for i in ins))
        p=OUT/f'ranges/{va:08X}.bin';p.parent.mkdir(exist_ok=True);p.write_bytes(raw)
        row={**mapping(pe,va),'label':label,'kind':kind,'bytes':n,'output':record(p),'decoded_bytes':sum(i.size for i in ins),
          'instructions':[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in ins]}
        ranges.append(row)
        for i in ins:ins_by_va[i.address]=i;range_by_va[i.address]=row
    write('skill-pane-layout/ranges.json',{'fixed_PE':record(PE),'ranges':ranges,'raw_bytes_authoritative':True,'all_code_ranges_fully_decoded':True})
    expected=[(0x47E57F,'call','dword ptr [edx]'),(0x4CC7B6,'call','0x4cc7e0'),(0x4CC834,'call','0x4eb430'),(0x4EB467,'call','dword ptr [eax]'),
      (0x4C91E6,'call','0x4c9210'),(0x4C9210,'jmp','0x4cd000'),(0x4CD02C,'call','0x450230'),(0x4CD03A,'call','0x450170'),
      (0x450256,'mov','dword ptr [esi + 0x2c], ebx'),(0x450259,'mov','dword ptr [esi + 0x28], ebx'),
      (0x492D5D,'mov','ecx, 0x25'),(0x492D64,'mov','edx, 0x2b'),(0x492D69,'mov','edi, 0xae'),(0x492D70,'mov','ecx, 0xf8'),
      (0x492D80,'call','0x4c9190'),(0x492DB7,'call','0x4cc3b0'),(0x492DDF,'call','dword ptr [eax + 4]'),
      (0x4C9194,'push','0x22'),(0x4C9196,'push','0xc4'),(0x4CCFA5,'mov','dword ptr [esi + 0x54], ebx'),
      (0x4CC87D,'mov','eax, 0x22'),(0x4CC882,'mov','dword ptr [edx + 0x24], eax'),(0x4CC888,'mov','dword ptr [ecx + 0x20], eax'),
      (0x4CC750,'mov','edi, 0x280'),(0x4CC757,'movsx','ecx, word ptr [eax]'),(0x4CC75D,'sub','edi, ecx'),
      (0x48AA00,'call','dword ptr [edx + 8]'),(0x4CCCB1,'call','dword ptr [edx + 0x24]'),(0x4CBDBE,'call','0x4cd290'),
      (0x4CD2CC,'mov','eax, dword ptr [esi + 0x28]'),(0x4CD2EA,'lea','edx, [edx + edx*2]'),(0x4CD2ED,'lea','edx, [ecx + edx*2]'),
      (0x4CD2F7,'jg','0x4cd3dd'),(0x4CD303,'cmp','dword ptr [ecx + 4], -1'),(0x4CD30D,'call','dword ptr [edx + 8]'),
      (0x4CD33A,'call','0x4b9dd0'),(0x4CD3BA,'lea','edx, [edx + ecx*4]'),(0x4CD3C1,'jle','0x4cd3ce'),
      (0x4C8FB9,'cmp','eax, dword ptr [0x834878]'),(0x4C901D,'call','0x4b9a90'),(0x4C9068,'call','0x4b9980'),
      (0x4B9ACC,'call','dword ptr [0x8300e4]'),(0x4B9E34,'mov','bx, word ptr [edx + ebx*2 + 0x200]'),
      (0x4B9E3C,'add','bx, word ptr [edx + eax*2]'),(0x4CD715,'mov','byte ptr [edx + 0xc], 1'),
      (0x4CD492,'cmp','esi, 8'),(0x4CD4B3,'cmp','edx, 4'),(0x4CD4B8,'mov','eax, dword ptr [ecx + 0x28]'),
      (0x4CD4BB,'lea','eax, [eax + esi*4]'),(0x4CC31C,'shl','eax, 2'),(0x4CD276,'mov','dword ptr [ecx + 0x28], eax'),
      (0x4CC961,'mov','dword ptr [eax + 0x28], ecx'),(0x4CC95E,'add','ecx, -0x24'),(0x4CC9E2,'add','ecx, 0x24')]
    assertions=[]
    def witness(va):
        i=ins_by_va[va];r=range_by_va[va]
        return {'fixed_PE_sha256':record(PE)['sha256'],'va':f'{va:08X}','rva':f'{va-0x400000:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str,'range':r['output']}
    for va,mn,op in expected:
        i=ins_by_va[va];assert (i.mnemonic,i.op_str)==(mn,op),(f'{va:08X}',i.mnemonic,i.op_str)
        assertions.append({**witness(va),'match':True})
    data_assertions=[]
    for va,value in [(0x4FA568,0x4CC7B0),(0x4FA56C,0x4CC870),(0x4FA570,0x4CCB20),(0x4FA50C,0x4C91E0),(0x4FA530,0x4CBDB0),(0x4FA5C0,0x4CD290),(0x4FA4F0,0x4C8EA0),(0x4FA4F4,0x4C8F90),(0x8300E4,0)]:
        b=pe.get_data(va-0x400000,4);assert int.from_bytes(b,'little')==value
        data_assertions.append({'fixed_PE_sha256':record(PE)['sha256'],'va':f'{va:08X}','rva':f'{va-0x400000:08X}','raw_hex':b.hex(),'value':f'{value:08X}','match':True})
    assert pe.get_data(0x43673C,0x400)==bytes(0x400)
    data_assertions.append({'fixed_PE_sha256':record(PE)['sha256'],'va':'0083673C','rva':'0043673C','bytes':1024,'all_zero':True,'match':True,'meaning':'fixed snapshot only, not live LUT state'})
    cases=[]
    for count,start,origin in [(80,0,[37,43]),(10,4,[37,43]),(80,79,[101,60]),(80,80,[37,43])]:
        rows=placements(origin,count,start)
        closed=[{'index':i,'caller_origin':[origin[0]+((i-start)%4)*34,origin[1]+((i-start)//4)*34]}for i in range(start,min(count,start+24))]
        assert rows==closed
        cases.append({'count':count,'scroll_start':start,'already_resolved_origin':origin,'placements':rows,'loop_vs_closed_form_equal':True,'parameter_status':'code-derived inputs, not observed native pane state'})
    rect=[37,43,174,248];hits=[]
    for p in [[37,43],[172,246],[173,43],[37,247],[37,248]]:
        h=hit(p,rect,80,0);h.update(point=p,rect=rect,count=80,scroll_start=0);hits.append(h)
    assert [x['index']for x in hits]==[0,23,-1,24,None]
    actual_fallback=load('graphics-parity/vectors/shared-91.tbt/vector.json')
    assert actual_fallback['offset_signed']==[3,5]
    offset_case={'already_resolved_grid_origin':[37,43],'asset':'shared91.tbt','source_vector':record(ROOT/'graphics-parity/vectors/shared-91.tbt/vector.json'),
      'normal_blitter_header_offset':[3,5],'normal_blitter_effective_origin':[40,48],'inherited_blitter_contract':record(ROOT/'native-icon-draw-contract.json'),
      'applied_in_grid_loop':False,'applied_in_normal_4BE000':True,'alternate_8300E4_offset_application':'UNKNOWN_BODY','original_GUI_oracle':False}
    scroll_cases=[]
    for s,n,act,p,expect in [(0,80,'up4',None,0),(0,80,'down4',None,4),(0,80,'page_down36',None,36),(76,80,'page_down36',None,76),
                            (4,80,'page_up36',None,0),(0,80,'slider',231,76),(0,80,'callback_row',21,84)]:
        result=scroll(s,n,act,p);assert result==expect
        scroll_cases.append({'start':s,'count':n,'action':act,'parameter':p,'expected_start':result,'start_valid_for_draw':0<=result<n,'code_derived':True})
    write('skill-pane-layout/placement-cases.json',{'oracle':'CODE_DERIVED_ONLY, not native execution or Modern output','coordinate_basis':'Placement origin is already resolved after pane/frame/drag/global translation. Hit cases use owner/input coordinates and upstream rect gate; global translation assumed0 for these cases, not observed runtime state.','placement_cases':cases,'hit_cases':hits,'normal_bitmap_offset_case':offset_case,
      'scroll_cases':scroll_cases,'unsafe_negative_start':{'read_site':'004CD300','guard_missing':'No start>=0 check before table[start]; model does not execute this unsafe case'},
      'checks':'4 loop-vs-formula cases,5 hit boundaries,7 scroll transitions,1 inherited actual-source-offset calculation. No previous resource/packet test replay.'})
    modern=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'/'world_skill_panel.rs';lines=modern.read_text(encoding='utf-8').splitlines()
    write('skill-pane-layout/modern-comparison.json',{'source':record(modern),'excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {lines[i]}'for i in range(a-1,b))}for a,b in [(11,28),(174,195)]],
      'differences':[
        {'field':'grid geometry','native':'relative pane frame origin(37,43),34x34 step,4 columns/6 visible rows','Modern':'GRID_LEFT27,GRID_TOP34,CELL35x35;5/4 scale'},
        {'field':'row membership/scroll','native':'slot indices S..min(N,S+24)-1, placeholders consume slots but ID=-1 skips row draw; +/-4 and page +/-36 controls','Modern':'8 fixed supported skill IDs,2 rows,learned filtering; no native slot-order/scroll geometry in this module'},
        {'field':'clip','native':'row-bottom check before draw; no per-cell pixel clip in grid loop, normal blit contract has none','Modern':'42x42 explicit cell clip plus surface bounds; this is a Modern presentation policy'},
        {'field':'state rendering','native':'marked row+0C invokes separate LUT rectangle; row+8 uses time-dependent alternate target8300E4 or normalcopy; hover+4C positions text separately','Modern':'choice.is_none =>dim42px rectangle, selected ID=>border42px; unsupported/unavailable UI policy is not proved equivalent to native mark/timer behavior'},
        {'field':'scale and bitmap offset','native':'unscaled caller x/y plus signed bitmap offset in normal4B9980→4BE000; alternatebody unknown','Modern':'both coordinates and stored u8 bitmap offsets are scaled5/4; no native5/4 transform in captured path'}],
      'Modern_modified_or_executed':False,'used_as_original_oracle':False})
    asm_lines=ASM.read_text(encoding='utf-8').splitlines()
    write('skill-pane-layout/asm-excerpts.json',{'source':record(ASM),'warning':'Different recovered PE fingerprint; fixed PE bytes authoritative.',
      'excerpts':[{'first_line':a,'last_line':b,'text':'\n'.join(f'{i+1}: {asm_lines[i]}'for i in range(a-1,b))}for a,b in [(269471,269480),(427132,427213),(420737,420783),(309479,309541),(293790,293809),(426005,426018),(428774,428785)]]})
    claims=[]
    def claim(name,text,vas,callers):claims.append({'name':name,'conclusion':text,'fixed_PE_sha256':record(PE)['sha256'],'instruction_evidence':[witness(v)for v in vas],'caller_evidence':callers})
    claim('cleanup_chain_is_not_layout','47E4B0 destroys834884 through virtual0;4CC7B0/4CC7E0→4EB430 destroysowner virtual0→4C91E0→4C9210→4CD000→450230/450170.450230 clearscount/start after deleting rows. These are release/reset paths, not layout calculations.',[0x47E57F,0x4CC7B6,0x4CC834,0x4EB467,0x4C91E6,0x4C9210,0x4CD02C,0x4CD03A,0x450256,0x450259],['004FA568 virtual0=4CC7B0','004FA50C virtual0=4C91E0'])
    claim('initial_geometry_and_step','4928C0 constructs owner with rect(37,43,174,248), publishes834888, builds pane834884 with owner, then calls pane virtual+4.4CC870 setsowner+54=1 andboth+20/+24=34.4C9190 earlier passes196x34 defaults;196 is overwritten in admitted activation path.',[0x492D5D,0x492D64,0x492D69,0x492D70,0x492D80,0x492DB7,0x492DDF,0x4C9196,0x4CC87D,0x4CC882,0x4CC888],['492D3A admissioncaller','4C9190→4CCED0→450090→4C2FC0','pane vtable+4 at4FA56C'])
    claim('origin_translation','Pane child container starts origin(0,0).Constructor takesB=640-signedWORD(frame provider834BD4+E98 payload[0]), addsB to containerx andownerleft/right.Pane drag callback4CC330 adds(dx,dy) tocontainerorigin andallownerrectedges.Draw then addsglobals75A998/75A99C once. Actualframewidth/liveglobals/dragstate are not observed; noarchiveextracted.',[0x4CC750,0x4CC757,0x4CC75D,0x4CC764,0x4CC76F,0x4CC77A,0x4CD2C2,0x4CD2CA,0x4CC348,0x4CC35B,0x4CC36D,0x4CC392],['4CC3B0 constructorprefix +late4CC742','41B310 writescontainer+14/+18=0','4CC330 callback'])
    claim('actual_render_chain','WORLD488860 range48A9F4 walks4 registry pointers836B54..836B60 in reverse andcallsnonnullpane virtual+8. Ifslot is834884 withknownvtable, target4CCB20. Itdrawsframe/children thenowner virtual+24=4CBDB0→4CD290;generic list vtable also maps+24 to4CD290. Actualregistry membership/frame execution is unobserved.',[0x48AA00,0x4CCCB1,0x4CBDBE],['pane vtable4FA570=4CCB20','skill list vtable4FA530=4CBDB0','common list vtable4FA5C0=4CD290'])
    claim('grid_placement_and_visibility','Fornormalpositive34pxsteps,nonnegativeS,andstablefields:O=(ownerleft+Gx,ownertop+Gy),k=i-S,x=Ox+34*(k%4),y=Oy+34*(k//4).4CD290 allowsy+height<=Oy+6*height, wrapswhen nextx+width>Ox+4*width;4columns,6rows,max24slots. Initialiteration i=S, so nonmultiple4 S is placed at firstcolumn. ID=-1 skipsvirtualrowdrawbutstillconsumesposition. IfN<=0orS>=N no rows;negativeS/nullslot lacklower/nullguards.',[0x4CD2CC,0x4CD2DC,0x4CD2EA,0x4CD2ED,0x4CD2F7,0x4CD303,0x4CD307,0x4CD30D,0x4CD3BA,0x4CD3C1,0x4CD3D1],['4CCB20→4CBDB0→4CD290'])
    claim('clip_vs_iteration','Verticalextentcheck stopscallingremainingrows. Itdoesnotclipindividualbitmap extents.No headeroffset or5/4scale in gridloop.Normalgridrowcalls4B9980→4BE000;acceptedbatch5 appliesi8bitmapdx/dy separatelyonce. Timedalternatebody8300E4 isunknown, so its offset/clip semantics remainunproved. Pane IMG blits/external virtualsurface methodsareoutsideclosed normalwordcopy behavior.',[0x4CD2F7,0x4CD30D,0x4C9068,0x4C901D,0x4B9ACC],['priornative-icon-draw-contract.json unchanged','normal4C8F90 branch;differentfromtextrow4C8EA0'])
    claim('selection_mark_and_time_render','Gridusesrowvirtual+8=4C8F90,notvirtual+4=4C8EA0.4C8F90chooses4B9A90when time-return<834878 AND834870!=0,else4B9980.4B9A90indirect8300E4 is0infixedPE,livebodyunknown.Markrow+0C isselectionstate:4CD6D0 sets/clearsit,4509A0clearsall.4CD290drawsrowfirst,thenifmarknonzeroinvokes4B9DD0 at(x-1,y-1),width=stepx,height=stepy;thisappliesseparate16bitLUT towholecellrectangle,notRGBAalpha/nordisabledspellproof.Hover+4Ccomputesseparate tooltip position/text. LUTsnapshotall0,liveLUTunproved.',[0x4CD30D,0x4C8FB9,0x4C8FC1,0x4C901D,0x4C9068,0x4CD319,0x4CD33A,0x4B9E34,0x4B9E3C,0x4CD715,0x4CD342],['row vtable4FA4F4=4C8F90 versus4FA4F0=4C8EA0','4CD6D0 selectionupdate','4CD290 tooltip uses+4C; nochoice.is_nonecomparison'])
    claim('scroll_rules','Owner+28 isstartslot,notpixeloffset.4CC890/8F0 moveby4 onlyifcandidate>=0/<count.Page4CC950/9D0move36slots,clamp0/count-1thenaligntrunc(start/4)*4.Callback4CC310directlysetsrow*4via4CD270 withno countclamp.Slider4CCA60clampsp36..231 andnormalcount>1computes4*trunc(trunc((count-1)/4)*(p-36)/195).Scrollcontrolsneednotfilllast24slots;start76,count80shows4slots.',[0x4CC89A,0x4CC8FD,0x4CC95E,0x4CC9E2,0x4CC31C,0x4CD276,0x4CCA69,0x4CCA77,0x4CCA9F],['panevtable+0C/+10/+14/+18/+1C','callback4CC310 installedbyconstructor4CC59B','scrollfieldstart+28'])
    claim('hit_and_visible_boundary','Upstreaminputchecksownerhalfopenrect.4CD480returnsS+4*trunc((y-top)/stepy)+trunc((x-left)/stepx),rejectsrow>8,column>=4,index>=N;no lowerchecksinsidehelper. Withinitialrectheight205andstep34,y=top+204 remainsinsidebut mapsrow6/indexS+24 while renderer onlydrawsrows0..5. Rightmost1px atx=left+136 mapscol4andisrejected. Thesearecode-derivededgebehaviors,notobservedclickresults.',[0x4CD73D,0x4CD758,0x4CD492,0x4CD4B3,0x4CD4B8,0x4CD4BB,0x4CD4C0,0x4CD8AF],['4CD730 inputrectanglegate','4CD6A0→4CD480 forselection','4CD85B hovercomputescolumn/rowusingconstant34'])
    claim('stored_count_and_placeholders','Commonlistconstructorsetscapacity256;4CCED0 initiallyaddsplaceholderrows:classbyte75F694==2 =>99,==0=>56,==4=>40,other=>80. Theseareinitiallogicalslotcounts,notGUIlearnedspellcounts orvisiblecolumns. PlaceholdersID=-1arestillpositions;livecountcanchangeviaexistingrowoperations. Thisbatchdoesnotreplayclass/orderproducer orpacketreadinesschecks.',[0x4CCF38,0x4CCF3F,0x4CCF4A,0x4CCF56,0x4CCF83,0x4CCF95],['4C9190→4CCED0','450090capacity256','priorbatch3S1E/resourcecontracts unchanged'])
    claim('text_is_separate_from_grid_pixels','4C8EA0 virtual+4 text-row tail draws text atcaller(x+42,y+3), withpossible secondline14pxbelow. Nativegridusesvirtual+8 and doesnotcallthistexttail. Itsseparatehover tooltip iscomputedwhenvisible i==owner+4C:width=6*chars(row+14 orstrlen),x=min(gridX,Gx+630-width),y=gridY+32 unlessgridY+44>Gy+400 then gridY-12. Thispositions text; itdoesnotclipthebitmap cell.',[0x4C8F12,0x4C8F19,0x4C8F42,0x4CD342,0x4CD36D,0x4CD378,0x4CD396,0x4CD3A5,0x4CD3A9],['4FA4F0=4C8EA0 versusgrid4FA4F4=4C8F90','4CD290 tooltipbranch afterrowdraw'])
    write('skill-pane-layout/new-evidence-verification.json',{'fixed_PE':record(PE),'raw_ranges':len(ranges),'instruction_assertions':assertions,'data_assertions':data_assertions,
      'range_bytes_and_hashes_match':all(Path(r['output']['path']).read_bytes()==pe.get_data(int(r['rva'],16),r['bytes'])and record(Path(r['output']['path']))==r['output']for r in ranges),
      'preserved_prior_primary_pins':preserved,'code_derived_placement_cases':4,'hit_boundary_cases':5,'scroll_cases':7,'inherited_actual_offset_cases':1,
      'old_PASS_resource_packet_tests_replayed':False,'new_archive_members_extracted':0,'Modern_modified_or_executed':False,'native_launched':False,'GUI_accepted':False})
    report={'schema':1,'updated_at':stamp(),'status':'FINITE_SKILL_PANE_LAYOUT_STATIC_CONTRACT_COMPLETE_WITH_EXPLICIT_DYNAMIC_TARGET_UNKNOWNS',
      'fixed_PE':record(PE),'supporting_ASM':record(ASM),'source_ranges':record(OUT/'ranges.json'),'instruction_and_pin_verification':record(OUT/'new-evidence-verification.json'),
      'claims':claims,'placement_cases':record(OUT/'placement-cases.json'),'modern_comparison':record(OUT/'modern-comparison.json'),
      'previous_draw_contract':record(ROOT/'native-icon-draw-contract.json'),
      'blocked_addresses':[{'va':'008300E4','rva':'004300E4','fixed_raw_hex':'00000000','caller_va':'004B9ACC','unknown':'Live timed-alternate blitter target/body, alpha/color/bitmap-offset/clip semantics'},
        {'va':'0083673C','rva':'0043673C','fixed_snapshot_all_zero_bytes':1024,'caller_va':'004CD33A','unknown':'Live512-word LUT values; exactmarkcolor/darkness,notselectionflagmechanics'},
        {'va':'004CC757','rva':'000CC757','raw_hex':'0fbf08','unknown':'Actualframeprovider834BD4+E98 payload signedWORDwidth; no extraarchiveextract authorized/needed forsymbolicplacementcontract'},
        {'va':'004B9997','rva':'000B9997','prior_batch5_evidence':record(ROOT/'graphics-parity/ranges.json'),'unknown':'Externalsurfacevirtual+64/+80andupstream/externalclipimplementation'},
        {'va':'0048A9F4','rva':'0008A9F4','unknown':'Live registrymembership,globaltranslation,paneactivation state andactualframecallstack; no native runtime evidence'}],
      'scope_limits':['Closed34pxgrid4columns/6rowsandconditionalcallerchain;Modern5/4scale,35pxstep,27/34insets,42clipnotnativeequivalencetraces.',
        'Somecapturedroutinesreadmutablecount/start/stepsduringloop;closedformcasesassumestablefields,noreentryandnormalpositiveinput. No allthread/no-reentryclaim.',
        'Hithelper comparisons use owner/inputcoordinates whiledrawadds75A998/75A99C. Cases explicitlyfixtranslation0; fullOS-input normalization chain is not closed bythis finite layout audit.',
        'Gridrowtimebranchusesalternateunresolvedtarget,soacceptedbatch5normalcopycontractdoesnotcloseeverygridframealpha/colorpath.',
        'No native execution/screenshots,Modern implementation edits,new archives,packetwirechanges oroverallunpacking/productacceptance.'],
      'finite_static_layout_contract_complete':True,'native_GUI_layout_parity_accepted':False,'overall_complete':False,'new_wire_differences':0,'new_opcodes':0}
    write('skill-pane-layout-contract.json',report)
    print(json.dumps({'report':record(ROOT/'skill-pane-layout-contract.json'),'raw_ranges':len(ranges),'instruction_assertions':len(assertions),'data_assertions':len(data_assertions),'placement_cases':4,'hit_cases':5,'scroll_cases':7,'new_archive_members':0},ensure_ascii=False))

if __name__=='__main__':main()
