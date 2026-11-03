"""Finite extension to T74: initialization writes and resource record provenance only."""
from audit import *
from extract_member import load_catalogue,extract
from decode_icons import decode_lbt
def main():
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    prior=Path('D:/lito-dev-runtime/20261003-supervision/launcher/observability-20261003-225704/ui-native-hookup-20261004-051000/t69_skill_evidence/conditional-class-orders.json')
    old=json.loads(prior.read_text(encoding='utf-8'));assert struct.unpack('<179h',pe.get_data(0x108648,358))==tuple(old['signed_icon_prefix'])
    specs=[(0x43F6C0,0x455,'shared icon initializer; class array population and list.ico count/table'),(0x43F920,0xC0,'per-icon %d.tbt shard-name cache population'),(0x4426E0,0x11,'zero iconcount/table initializer'),(0x440910,0x156,'metadata indexed table loader'),(0x492ACA,0xD8,'class byte and selected class-order pointers'),(0x4CD050,0x70,'index/icon replacement gate context; T74 lifecycle retained'),(0x4CD1C0,0x100,'order pointer -> placeholder reset context'),(0x4AEE70,0xC0,'S1E dispatch context; not actual receivedpacket'),(0x635B80,0x14,'class selected order pointer'),(0x75F694,4,'selected class byte, onlyfirstbyte belongs toclass'),(0x648EC8,8,'icon cache tablepointer + count'),(0x647E20,8,'skill metadata count32 + tablepointer32'),(0x508648,358,'source referenced signed icon179word prefix')]
    slices=[]
    for va,n,purpose in specs:
        raw=pe.get_data(va-0x400000,n);p=ROOT/'skill-initialization'/f'VA-{va:08X}.bin';p.parent.mkdir(exist_ok=True);p.write_bytes(raw)
        slices.append({**mapping(pe,va),'purpose':purpose,'bytes':n,'output':record(p),'instructions':[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]if va<0x500000 else []})
    write('skill-initialization/ranges.json',{'source':record(PE),'slices':slices,'scope':'bounded extension to prior T74; not full CFG or execution proof'})
    catalog,tables=load_catalogue();lookup={stem:{r['name'].lower():r for r in rows}for stem,rows in tables.items()}
    # Only the source-established filename hash route, member absence ->basefallback.
    requests={'list.ico'}|{f'{value}.tbt'for value in old['signed_icon_prefix']if value>=0}|{f'{value}.tbt'for rows in old['class_orders'].values()for value in rows if value is not None and value>=0}
    selections=[];missing=[]
    for name in sorted(requests):
        shard=f'Sprite{sum(name.encode())%16:02}';row=lookup[shard].get(name);stem=shard;route='hashmod16 shard'
        if row is None:stem='Sprite';row=lookup[stem].get(name);route='base member fallback after shard member absence'
        if row is None:missing.append({'name':name,'shard':shard,'base_checked':True});continue
        selections.append((name,stem,row,route))
    selected_stems={stem for name,stem,row,route in selections}
    pins=[]
    for stem in sorted(selected_stems):
        a=catalog[stem];now=record(Path(a['pak_source']['path']));assert now['sha256']==a['pak_source']['sha256'];pins.append(now)
    outputs=[]
    for name,stem,row,route in selections:
        result=extract(catalog[stem],row,'skill-source');result['route']=route
        if name=='list.ico':result['declared_icon_range']=int(Path(result['decoded']['path']).read_bytes().strip())
        else:
            try:
                im,details=decode_lbt(Path(result['decoded']['path']).read_bytes());p=Path(result['decoded']['path']).with_name(name+'.png');im.save(p);result['layout_validation']={'status':'L1 TBT rowspan RGB555 verified','preview':record(p),'details':details,'native_render_parity':False}
            except ValueError as e:result['layout_validation']={'status':'unqualified variant preserved raw','reason':str(e)}
        outputs.append(result)
    write('skill-initialization/shared-icon-resources.json',{'updated_at':stamp(),'generator':record(Path(__file__)),'routing_source':'43F6C0->list.ico / %d.tbt;43FFC0 sumsASCII filename mod16; shard thenbaseonlyifmemberabsent. NativeLTicon is separate.','prior_class_order_evidence':record(prior),'only_source_observed179wordprefix_and_closed_classslots':True,'prefix_is_complete_table_bound':False,'container_verifications':pins,'requested':len(requests),'outputs':outputs,'missing':missing,'unqualified':[x['name']for x in outputs if x.get('layout_validation',{}).get('status')=='unqualified variant preserved raw']})
    m=json.loads((ROOT/'resources/archive-manifest.json').read_text(encoding='utf-8'));metadata=next(x for x in m['selected_members']if x['name']=='LTSpellDesc-c.tbl');raw=Path(metadata['decoded']['path']).read_bytes();lines=[x for x in raw.splitlines()if x.strip()];count=int(lines[0]);parsed=[]
    for line in lines[1:]:
        match=re.fullmatch(rb'([0-9]+) (.*)',line)
        if not match:raise ValueError('Metadata grammar unqualified')
        ident=int(match[1]);assert 0<=ident<count
        parsed.append({'index':ident,'text_bytes_hex':match[2].hex(),'cp950_text':match[2].decode('cp950')})
    write('skill-initialization/LTSpellDesc-c-indexed.json',{'source':metadata,'declared_count':count,'nonempty_rows':len(parsed),'rows':parsed,'null_indices':sorted(set(range(count))-{x['index']for x in parsed}),'meaning':'Native440910 allocates count4byte pointers thenzeroes; onlypresentindexedrows getstringallocation. Missingrows remainNULL; no synthesizedmetadata. Duplicate row replacement semantics not triggered by thisfile.'})
    bindings={
      'updated_at':stamp(),'source_PE':record(PE),'prior_T74':record(ROOT.parent/'lito登入器'/'Native-T74-Skill-Lifecycle-Handoff.json'),'prior_T69_class_orders':record(prior),
      'class_initialization':{'source':'4928C0 selected roster EBX+2D (u8)->75F694 at492AD4; sets635B80 ordertablepointer and635B84 secondarypointer','classes':[{'class':2,'order':'008345E8','secondary':'00635D08'},{'class':0,'order':'00834774','secondary':'00635E94'},{'class':4,'order':'00834548','secondary':'00635F74'},{'class':'default (includes1)','order':'00834408','secondary':'00635B88'}],'postselect_overrides':'392/393/394/755/651 assignedselectedorder[8]/[9]/[10]/[13]/[14]; class1 separately applies extendedoverrides from priorT69. Rawbytes takeprecedence; priorclass4unclosedslots retained.','requires':'43F6C0 sharedresource/tableinit before4928C0 classselection; nofixtureclasssetter substituted','disk_values_not_execution':'Static OEPsnapshot mayholdzero classarrays; not proof runtime arrays aremissing'},
      'shared_icon_initialization':{'initializer':'43F6C0 fills class arrays from signedWORD data tables; defaultprefix508648..5086E8 copies80words to834408','cache_count':'list.ico intconversion ->648ECC at43F8D6 (actual instruction inranges)','cache_pointer':'malloc(count*4) ->648EC8 at43F8F4 (actual instruction inranges); zero slots then periconrecord loads','null_failure':'loaderfail/nulltable/absenticon paths remain native; no resource substitution','CRT_initializer':'4426E0 zeros648EC8/648ECC; constructor-registration linkage is unclosed;43F6C0 populates','resource_namespace':'shared SpriteNN/Sprite %d.tbt; separateNativeLTicon %d.lbt cannot be substitutedby matching number'},
      'skill_metadata_initialization':{'initializer':'47C8E1 EDX647E20;47C8E6 ECXnameLTSpellDesc;47C8F0 call440910;47C8F5 storesreturnedtable647E24','widths':'647E20=count32,647E24=tablepointer32 (loader writes count viaEDX andreturns tableinEAX); prior shorthand mayinvert theirroles','namespace':'440910 usesmanager647994 openformatter508900 after4D43D0 locale; exactextractedLTtext/LTSpellDesc-c.tbl forcodepage950/c locale','resource':metadata,'declared_count':count,'presentrows':len(parsed),'missing_null_indices':sorted(set(range(count))-{x['index']for x in parsed}),'loader_bounds':'count>0&&count<0x8000;tablecount*4 zeroed;validindexedrowduplicatestrdup;outofrangenegativeindicesignored;absentopenreturnsEAX0 without pointerpublish','callsite_context':'worker47C8A0 raw code; native executionunproved; usesmetadata beforelaterhostsave butdoesnot proveGUIcorrect'},
      'T74_consumers_retained':'S1E4AEE70 alwaysattempts setbits: validmetadata selects508648 icon/text; missingmetadata fallsback toicon91/unknownspell andstill calls4C9220. Liveowner + indexdedup/countgate + selectedclassordericonmatch controlsreplacementin4CD050;4CD1C0 rebuilds -1/-1 placeholders anddoesnot read635B80.',
      'T73_actual_scope':'142frames noS1E, notypedskillownersnapshot; noabsenceofresourcesorunpackingrootcause established',
      'remaining':['class4 eightunclosedstatic slots requireactualsource/runtimeallocation','Originalsameepoch class/order/metadata/cache success notobserved','S1E delivery/owner reset/callback/presentation paths stilldistinguished','CapturedDLLhash andprivatethunk bodyremainunavailable']}
    write('launcher-skill-initialization.json',bindings)
    print('Skillinit bindings ready;',len(outputs),'shared iconmembers;',len(parsed),'nonempty LTSpellDesc rows fromdeclared',count,'; missing',len(missing))
if __name__=='__main__':main()
