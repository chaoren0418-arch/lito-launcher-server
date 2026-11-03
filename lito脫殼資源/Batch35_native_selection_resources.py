"""Finite Batch35 two-file source addendum and Native-only existing resource audit."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,csv,struct,difflib
from pathlib import Path
from datetime import datetime
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'Native-selection-resource-gap'
MODERN=ROOT.parent/'lito重建版'/'LiTo381-Modern'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
    p=Path(p);b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def load(p):return json.loads(Path(p).read_text('utf-8-sig'))
def save(n,j):
    p=OUT/n;assert p.resolve().is_relative_to(ROOT);OUT.mkdir(exist_ok=True);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(p):
    p=Path(p);before=pin(p);b=p.read_bytes();after=pin(p)
    return b,{'before':before,'content_sha256':sha(b),'after':after,'stable':before==after and before['sha256']==sha(b),'read_at':datetime.now().astimezone().isoformat()}
def collect():
    previous=load(ROOT/'Modern-character-ui-reachability-contract.json')
    report=MODERN/'logs/continuation-20261003/r526-production-source-snapshot.json'
    b,r=read(report);assert r['stable'] and r['before']['sha256']=='514D978CAABECF04F20BDB03F941274E90274803FBEDFD32026E1C529C6FC82C'
    frozen=json.loads(b);selected=[q for q in frozen['source_pins'] if Path(q['path']).name in ['native_city_runtime.rs','native_source_send.rs']];assert len(selected)==2
    source_reads=[];pairs=[]
    for q in selected:
        name=Path(q['path']).name;fb,fr=read(q['snapshot']);lb,lr=read(q['path']);assert fr['stable'] and fr['before']['bytes']==q['bytes'] and fr['before']['sha256']==q['sha256']
        fl=fb.decode('utf-8-sig').splitlines();ll=lb.decode('utf-8-sig').splitlines();old=load(ROOT/'Modern-character-ui-reachability'/(name+'.snapshot.json'));ol={x['line']:x['text'] for x in old['implementation_lines']};new_rows={};live_rows={}
        for finding in previous['findings']:
            for a in finding['anchors']:
                if a['source']['path']!=q['path']:continue
                oldrow=ol[a['start_line']];exact=[i+1 for i,x in enumerate(fl) if x==oldrow]
                assert len(exact)==1,(name,a['function'],exact)
                start=exact[0];end=start+a['end_line']-a['start_line'];oldspan=[ol[i] for i in range(a['start_line'],a['end_line']+1)];newspan=fl[start-1:end]
                pairs.append({'finding':finding['id'],'function':a['function'],'historical_anchor':a,'frozen_source_pin':q,'frozen_start_line':start,'frozen_end_line':end,'required_start_token':a['required_start_token'],'start_text_unchanged':True,'full_anchored_span_unchanged':oldspan==newspan,'old_lines':oldspan,'frozen_lines':newspan,'qualified':'Selected existing character call-chain only; no ready/backend/compiled/GUI qualification.'})
                for i in range(start,end+1):new_rows[i]=fl[i-1]
                # Live line metadata limited to corresponding selected text, no R527 expansion.
                lh=[i+1 for i,x in enumerate(ll) if x==oldrow]
                for k in lh:
                    for i in range(k,min(len(ll),k+end-start)+1):live_rows[i]=ll[i-1]
        save(name+'.frozen-slices.json',{'read':fr,'R526_reference':q,'lines':[{'line':i,'text':t} for i,t in sorted(new_rows.items())]})
        save(name+'.live-slices.json',{'read':lr,'matches_frozen_at_read':lr['before']['bytes']==q['bytes'] and lr['before']['sha256']==q['sha256'],'lines':[{'line':i,'text':t} for i,t in sorted(live_rows.items())],'adoption':'Read-time observation only; mutable R527 source can subsequently change.'})
        source_reads.append({'name':name,'frozen_read':fr,'live_read':lr,'live_matches_frozen':lr['before']['bytes']==q['bytes'] and lr['before']['sha256']==q['sha256']})
    save('R526-selection-compatibility-addendum.json',{'Batch34_primary':pin(ROOT/'Modern-character-ui-reachability-contract.json'),'R526_frozen_manifest_read':r,'selected_source_references':selected,'reads':source_reads,'anchor_comparisons':pairs,'conclusion':'The selected Batch34 character selection anchors remain in the pinned R526 two-file source. Scope is existing roster publication, bounded8 controls, opaque selection/submit checks and exact raw-name C9 construction. No whole R526/R527 compile, production backend wiring or GUI acceptance inferred; Batch34 historical originals unchanged.','all_selected_spans_unchanged':all(x['full_anchored_span_unchanged'] for x in pairs),'R527_uninterrupted':True})
    mb,mr=read(ROOT/'resources/archive-manifest.json');man=json.loads(mb)
    mapping_ref=next(x for x in man['selection_sources'] if 'resource-mapping' in x['path']);b,mapread=read(mapping_ref['path']);mapping=json.loads(b);assert mapread['before']==mapping_ref and mapread['stable']
    b,studyread=read(mapping['source']['path']);study=json.loads(b);assert studyread['before']==mapping['source'] and studyread['stable']
    # Read only existing fixed PE data/instructions, never disassemble or open a new callee body.
    pe_path=Path(study['analysis_PE']['path']);pb,per=read(pe_path);assert per['before']==study['analysis_PE'] and per['stable']
    nt=struct.unpack_from('<I',pb,0x3c)[0];count=struct.unpack_from('<H',pb,nt+6)[0];opts=struct.unpack_from('<H',pb,nt+20)[0];imagebase=struct.unpack_from('<I',pb,nt+24+28)[0];sections=[]
    for i in range(count):
        off=nt+24+opts+i*40;vs,rv,rs,fp=struct.unpack_from('<4I',pb,off+8);sections.append((rv,rs,fp))
    def sliceva(va,n):
        rv=va-imagebase
        for start,size,fp in sections:
            if start<=rv and rv+n<=start+size:
                offset=fp+rv-start;return pb[offset:offset+n],{'va':f'{va:08X}','rva':f'{rv:08X}','file_offset':offset,'bytes':n,'PE':per['before']}
        raise AssertionError('non raw-backed range')
    tables=[];lookup_ids=set()
    for t in mapping['animation_tables']:
        b,tr=read(t['raw']['path']);assert tr['before']==t['raw'] and tr['stable'];raw,origin=sliceva(int(t['table_va'],16),96)
        unpacked=[dict(zip(['base_png','frame_limit','delay','draw_mode','dx','dy'],struct.unpack_from('<4I2i',b,i*24))) for i in range(4)]
        assert raw==b and unpacked==t['states']
        for state in t['states']:lookup_ids.update(range(state['base_png'],state['base_png']+state['frame_limit']+1))
        tables.append({**t,'raw_read':tr,'fixed_PE_origin':origin,'matches_fixed_PE':True,'decoded_states_match_metadata':True,'semantic_limit':'Numeric class byte/EDX-zero-vs-nonzero only; no class/sex naming inferred. State ordering/frame_limit inclusive/exclusive/selected startup and expiry remain unqualified.'})
    descriptor=next(x for x in study['tables'] if x['va']=='0x630df8');b,dr=read(descriptor['raw']['path']);raw,origin=sliceva(0x630DF8,540);assert b==raw and dr['before']==descriptor['raw']
    expected_rows=[list(struct.unpack_from('<15I',b,i*60)) for i in range(9)];assert expected_rows==descriptor['rows']
    save('Native-animation-and-slot-data.json',{'mapping_read':mapread,'mapping_reference':mapping_ref,'source_study_read':studyread,'fixed_PE_read':per,'descriptor_table':{**descriptor,'read':dr,'fixed_PE_origin':origin,'matches_fixed_PE':True},'slots':mapping['slots'],'animation_tables':tables,'numeric_png_lookup_envelope':sorted(lookup_ids),'lookup_limit':'Conservative base..base+frame_limit envelope, not an assertion every endpoint is a required frame; no filename-derived class/sex. Existing raw tables reused, no new raw extraction.','variant_rule_historical_metadata':mapping['variant_rule'],'historical_draw_chain':mapping['draw_chain'],'lifecycle_open':mapping['animation_lifecycle_open'],'new_native_body_expansion':False})
    wanted={'79.img','80.img','6314.img','7482.img','6303.img','6304.img','6201.img','6202.img'}|{str(i)+'.png' for i in lookup_ids}
    indexes=[]
    for ar in man['archives']:
        if ar['namespace']!='native':continue
        b,ir=read(ar['index_table']['path']);rows=list(csv.DictReader(b.decode('utf-8-sig').splitlines()));matches=[x for x in rows if x['name'].lower() in wanted]
        assert ir['stable'] and ir['before']==ar['index_table']
        indexes.append({'archive':ar['stem'],'index_read':ir,'rows_in_complete_existing_index':len(rows),'target_rows':matches,'target_lookup_policy':'Exact79/80 IMG and conservative numeric PNG envelope plus six existing action/background IMG names only; SPR/TIL/LBT names do not imply equivalent images.'})
    assets=[]
    for x in man['selected_members']:
        if x['namespace']!='native' or x['archive']!='LTsurf' or x['name'] not in {'6314.img','7482.img','6303.img','6304.img','6201.img','6202.img'}:continue
        b,ar=read(x['decoded']['path']);header=list(struct.unpack('<4H',b[:8]));assert ar['before']==x['decoded'] and ar['stable']
        prv=x.get('preview');pr=None
        if prv:
            png,pr=read(prv['output']['path']);assert pr['before']==prv['output'] and pr['stable'];w,h=struct.unpack('>2I',png[16:24]);assert (w,h)==(header[0],header[1])
        assets.append({'member':x,'existing_raw_read':ar,'IMG_header_words':header,'dimensions':{'width':header[0],'height':header[1]},'existing_preview_read':pr,'no_new_decode':True,'mapping_status':'Native Enter/Back action pair already source bound' if header[0]==51 else 'Existing Native640x480 resource; associated branch below, selected-scene assignment unresolved'})
    loose=Path(r'D:\lineage開發\0-客戶端\LiTo\LTSurf');before=sorted(x.name for x in loose.iterdir() if x.is_file());after=sorted(x.name for x in loose.iterdir() if x.is_file())
    save('Native-existing-resource-inventory.json',{'archive_manifest_read':mr,'native_index_lookups':indexes,'existing_assets':assets,'loose_native_LTSurf_exact_lookup':{'directory':str(loose),'before_names':before,'after_names':after,'stable_directory_names':before==after,'target_existing_names':[x for x in before if x.lower() in wanted],'qualification':'Nonrecursive Native loose LTSurf only, not all shared/client/Classic assets. Only observed8044.img; no body or art read.'},'missing_exact_IMG_names':['79.img','80.img'],'missing_numeric_PNG_lookup_names':[str(i)+'.png' for i in sorted(lookup_ids)],'absence_scope':'Five complete existing Native indexes and exact nonrecursive Native LTSurf directory only; lookup miss cannot distinguish external/private PNG bank from missing shipment. No all-assets absence claim, no new extraction justified without resolved indexed offset.'})
    background(study,sliceva)
    print(json.dumps({'source_pairs':len(pairs),'all_spans_equal':all(x['full_anchored_span_unchanged'] for x in pairs),'source_reads':[(x['name'],x['live_matches_frozen']) for x in source_reads],'native_assets':len(assets),'animation_tables':len(tables),'png_lookup_envelope':len(lookup_ids),'png_bounds':[min(lookup_ids),max(lookup_ids)],'indexed_matches':[(x['archive'],len(x['target_rows'])) for x in indexes]},ensure_ascii=False))
def background(study,sliceva):
    bgstudy=next(x for x in study['fragments'] if Path(x['path']).name=='496EB0-497010.disasm');b,br=read(bgstudy['path']);assert br['before']=={k:bgstudy[k] for k in ['path','bytes','sha256']};ls=b.decode('utf-8-sig').splitlines();first=next(i for i,s in enumerate(ls) if s.startswith('00496F05'));last=next(i for i,s in enumerate(ls) if s.startswith('00496F2D'));chosen=ls[first:last];assert '0x1d3a' in chosen[3] and '0x18aa' in chosen[5]
    raw,origin=sliceva(0x496F05,0x496F2D-0x496F05);claims=[]
    for line in chosen:
        addr,hx,text=line.split(' ',2);ib=bytes.fromhex(hx);v=int(addr,16);assert raw[v-0x496F05:v-0x496F05+len(ib)]==ib;claims.append({'va':addr,'bytes':hx,'instruction':text})
    save('Native-background-branch-reference.json',{'existing_fragment_read':br,'fixed_PE_origin':origin,'fixed_bytes_sha256':sha(raw),'existing_fragment_lines':claims,'fixed_bytes_match':True,'branch':'496F05 reads64AFA0;nonzero pushes7482(1D3A),zero pushes6314(18AA);passes kind7 and631EC8/631ECC descriptors to495520. Stop at call; no constructor/callback/private body expansion.','qualification':'Both native IMG files exist and match this numeric branch. This caller belongs to the historical character UI study; whether this phase is the selected directory background or another character subscene is not qualified by this finite branch alone. Do not bind it to Directory solely from filename or dimensions.'})
def background_only():
    own=load(OUT/'Native-animation-and-slot-data.json');b,meta=read(own['source_study_read']['before']['path']);assert meta['before']==own['source_study_read']['before'];study=json.loads(b)
    pb,per=read(own['fixed_PE_read']['before']['path']);assert per['before']==own['fixed_PE_read']['before'];nt=struct.unpack_from('<I',pb,0x3c)[0];count=struct.unpack_from('<H',pb,nt+6)[0];opts=struct.unpack_from('<H',pb,nt+20)[0];base=struct.unpack_from('<I',pb,nt+52)[0]
    def sliceva(va,n):
        rv=va-base
        for i in range(count):
            off=nt+24+opts+i*40;vs,start,size,fp=struct.unpack_from('<4I',pb,off+8)
            if start<=rv and rv+n<=start+size:
                offset=fp+rv-start;return pb[offset:offset+n],{'va':f'{va:08X}','rva':f'{rv:08X}','file_offset':offset,'bytes':n,'PE':per['before']}
        raise AssertionError('nonraw')
    background(study,sliceva)
    c=load(OUT/'R526-selection-compatibility-addendum.json');a=load(OUT/'Native-existing-resource-inventory.json');print(json.dumps({'source_pairs':len(c['anchor_comparisons']),'all_spans_equal':c['all_selected_spans_unchanged'],'source_reads':[(x['name'],x['live_matches_frozen']) for x in c['reads']],'native_assets':len(a['existing_assets']),'animation_tables':len(own['animation_tables']),'png_lookup_envelope':len(own['numeric_png_lookup_envelope']),'background_reference_complete':True},ensure_ascii=False))
if __name__=='__main__':
    if '--background-only' in sys.argv:background_only()
    else:collect()
