"""未匯入、未接線。Second-stage bounded evidence enrichment, no DB/TCP/server.
Use --refresh-packets to compile only owned helpers and exercise pure builders.
Read the separately captured SELECT; never rerun SQL from this program.
"""
import sys
sys.dont_write_bytecode=True
import json,re,struct,subprocess,zipfile,hashlib,csv,io
from pathlib import Path
from collections import Counter,deque
import extract_validate as b
P=b.OUT; R=b.ROOT; S=b.SERVER; A=b.ANALYSIS; J=b.JAR
FIRST_SOURCE_COUNT=524; FIRST_PROVENANCE_COUNT=12119
def hashfile(p):
    h=hashlib.sha256()
    with Path(p).open('rb') as f:
        for chunk in iter(lambda:f.read(4*1024*1024),b''):h.update(chunk)
    return h.hexdigest()
def reg(p,kind,version='phase2/2026-10-14',member=None):
    p=Path(p).resolve()
    for row in b.sources:
        if row['path']==str(p) and row['member']==member:return row['source_id']
    raw=zipfile.ZipFile(p).read(member) if member else None
    row=dict(source_id=len(b.sources)+1,path=str(p),member=member,sha256=b.sha(raw) if raw is not None else hashfile(p),bytes=len(raw) if raw is not None else p.stat().st_size,kind=kind,version=version,stage=2)
    b.sources.append(row);return row['source_id']
def trace(table,key,fields,sid,loc,method,grade='static/resource confirmed; no live retest'):
    b.prov(table,key,fields,sid,loc,method,grade)
def load(name):return b.readjson(P/name)
def write(name,v):b.writejson(name,v)
def src(name):return S/'src/main/java/local/lito381/server'/name
def lines(p):return Path(p).read_bytes().decode('utf-8').split('\n')
def lineof(p,text):return next(i+1 for i,l in enumerate(lines(p)) if text in l)

class PE:
    def __init__(self,p):
        self.p=p;self.raw=p.read_bytes();pe=struct.unpack_from('<I',self.raw,60)[0];self.n=struct.unpack_from('<H',self.raw,pe+6)[0];opt=pe+24;self.base=struct.unpack_from('<I',self.raw,opt+28)[0];self.sec=opt+struct.unpack_from('<H',self.raw,pe+20)[0]
    def offset(self,va):
        rva=va-self.base
        for i in range(self.n):
            size,start,rawsize,raw=struct.unpack_from('<IIII',self.raw,self.sec+i*40+8)
            if start<=rva<start+min(size,rawsize):return raw+rva-start
        raise ValueError(hex(va))
    def read(self,va,n):return self.raw[self.offset(va):self.offset(va)+n]

def jar_methods(classname):
    javap=r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\javap.exe'
    r=subprocess.run([javap,'-J-Dfile.encoding=UTF-8','-p','-c','-classpath',str(J),'local.lito381.server.'+classname],check=True,capture_output=True,encoding='utf-8')
    parts=re.split(r'(?=^  (?:static|private|public|protected) .+\)(?: throws [^;]+)?;$)',r.stdout,flags=re.M)
    out={}
    for part in parts:
        m=re.match(r'  .+? ([\w$]+)\(',part)
        if m:out[m[1]]=part
    return out

RESOURCE_BOUNDARIES=[]
def resource(stem,name,allow_retained_payload=False,allow_current_extract=False):
    # Archive table and payload rechecked through actual readable paths.
    manifest=b.readjson(A/'resources/archive-manifest.json');a=next(x for x in manifest['archives'] if x['stem']==stem)
    native=stem.startswith('LT');root=R/'0-客戶端'/('LiTo' if native else '')
    idx=root/(stem+'.idx');pak=root/(stem+'.pak')
    actual_idx=hashfile(idx);actual_pak=hashfile(pak)
    idx_exact=actual_idx==a['index_source']['sha256'].lower();pak_exact=actual_pak==a['pak_source']['sha256'].lower()
    current_table=None;selected_descriptor=None
    if not idx_exact or not pak_exact:
        # Preserve old-vs-current container drift. Qualify only the selected
        # member by current decoded index + exact old raw payload + re-decode.
        ibcurrent=idx.read_bytes();oldbytes=a['index_source']['bytes'];oldn=(oldbytes-4)//28
        assert len(ibcurrent)>oldbytes and (len(ibcurrent)-4)%28==0
        codec=A/'codec/l1_decode.exe';local=P/'_codec';local.mkdir(exist_ok=True);exe=local/'l1_decode.exe';exe.write_bytes(codec.read_bytes());ip=P/'_index.raw';op=P/'_index.decoded';ip.write_bytes(ibcurrent)
        try:
            subprocess.run([str(exe),'idx',str(ip),str(op)],check=True,capture_output=True);fullidx=op.read_bytes()
        finally:ip.unlink(missing_ok=True);op.unlink(missing_ok=True);exe.unlink(missing_ok=True);local.rmdir()
        fn=struct.unpack_from('<I',fullidx)[0];assert len(fullidx)==4+28*fn
        oldidx=A/'resources/indices'/Path(a['decoded_index']['path']).name;oldib=oldidx.read_bytes();assert b.sha(oldib)==a['decoded_index']['sha256'].lower()
        oldentries=Counter(struct.unpack_from('<I20sI',oldib,4+28*i) for i in range(oldn));newentries=Counter(struct.unpack_from('<I20sI',fullidx,4+28*i) for i in range(fn))
        fullmatches=[struct.unpack_from('<I20sI',fullidx,4+28*i) for i in range(fn) if struct.unpack_from('<I20sI',fullidx,4+28*i)[1].split(b'\0')[0].decode('ascii').casefold()==name.casefold()]
        duplicate_count=len(fullmatches)
        if duplicate_count>1:
            assert allow_retained_payload,'duplicate resource requires explicit retained-payload identity'
            oldpayload=next((A/f'resources/raw/{stem}').glob('*-'+name)).read_bytes();qual=[]
            with pak.open('rb') as f:
                for desc in fullmatches:
                    off,_,size=desc;f.seek(off)
                    if size==len(oldpayload) and f.read(size)==oldpayload:qual.append(desc)
            assert len(qual)==1;selected_descriptor=qual[0];fullmatches=qual
        assert len(fullmatches)==1
        boundary=dict(archive=stem,member=name,old_index_sha256=a['index_source']['sha256'].lower(),current_index_sha256=actual_idx,old_pak_sha256=a['pak_source']['sha256'].lower(),current_pak_sha256=actual_pak,changed_or_removed_old_index_records=sum((oldentries-newentries).values()),new_or_changed_index_records=sum((newentries-oldentries).values()),added_pak_bytes=pak.stat().st_size-a['pak_source']['bytes'],same_name_duplicates=duplicate_count>1,same_name_member_count=duplicate_count,active_runtime_winner_confirmed=False if duplicate_count>1 else None,qualification='selected member only; explicit retained raw payload identity when duplicate; independent decode; old whole-container equality NOT claimed')
        RESOURCE_BOUNDARIES.append(boundary)
        current_table=fullidx
    idxsid=reg(idx,'native-resource',stem+' selected-client archive');paksid=reg(pak,'native-resource',stem+' selected-client archive')
    decodedidx=A/'resources/indices'/Path(a['decoded_index']['path']).name;ib=decodedidx.read_bytes();assert b.sha(ib)==a['decoded_index']['sha256'].lower();reg(decodedidx,'resource-derived','existing-pinned-index')
    if current_table is not None:ib=current_table
    n=struct.unpack_from('<I',ib)[0];assert len(ib)==4+28*n
    entries=[]
    for row in range(n):
        off,raw,size=struct.unpack_from('<I20sI',ib,4+28*row)
        if raw.split(b'\0')[0].decode('ascii').casefold()==name.casefold() and (selected_descriptor is None or (off,raw,size)==selected_descriptor):entries.append((row,off,size))
    assert len(entries)==1;(row,off,size)=entries[0]
    with pak.open('rb') as f:f.seek(off);raw=f.read(size)
    assert len(raw)==size
    matches=list((A/f'resources/raw/{stem}').glob('*-'+name))
    if not matches:
        assert allow_current_extract,'missing retained member requires explicit current unique extraction'
        codec=A/'codec/l1_decode.exe';assert hashfile(codec)==manifest['codec_executable']['sha256'].lower();local=P/'_codec';local.mkdir(exist_ok=True);exe=local/'l1_decode.exe';exe.write_bytes(codec.read_bytes());ip=P/'_payload.raw';op=P/'_payload.decoded';ip.write_bytes(raw)
        try:
            subprocess.run([str(exe),'payload',str(ip),str(op)],check=True,capture_output=True);decodedraw=op.read_bytes()
        finally:ip.unlink(missing_ok=True);op.unlink(missing_ok=True);exe.unlink(missing_ok=True);local.rmdir()
        return decodedraw,paksid,dict(index_source_id=idxsid,archive_source_id=paksid,member=name,row_0based=row,offset=off,bytes=size,raw_payload_sha256=b.sha(raw),decoded_sha256=b.sha(decodedraw),qualification='current unique indexed member independently decoded; no prior retained snapshot available')
    oldraw=matches[0];assert raw==oldraw.read_bytes()
    decoded=next((A/f'resources/decoded/{stem}').glob('*-'+name)) if a['codec'].startswith('L1') else oldraw
    if a['codec'].startswith('L1'):
        codec=A/'codec/l1_decode.exe';assert hashfile(codec)==manifest['codec_executable']['sha256'].lower()
        local=P/'_codec';local.mkdir(exist_ok=True);exe=local/'l1_decode.exe';exe.write_bytes(codec.read_bytes());ip=P/'_payload.raw';op=P/'_payload.decoded';ip.write_bytes(raw)
        try:
            subprocess.run([str(exe),'payload',str(ip),str(op)],check=True,capture_output=True);assert op.read_bytes()==decoded.read_bytes()
        finally:ip.unlink(missing_ok=True);op.unlink(missing_ok=True);exe.unlink(missing_ok=True);local.rmdir()
    sid=reg(decoded,'native-resource',stem+' decrypted selected-client resource')
    return decoded.read_bytes(),sid,dict(index_source_id=idxsid,archive_source_id=paksid,member=name,row_0based=row,offset=off,bytes=size,raw_payload_sha256=b.sha(raw),decoded_sha256=hashfile(decoded))

INDEX_CACHE={}
def asset(stem,name,native=False):
    root=R/'0-客戶端'/('LiTo' if native else '');idx=root/(stem+'.idx');pak=root/(stem+'.pak')
    if not idx.exists():return None
    if str(idx) not in INDEX_CACHE:
        ib=idx.read_bytes();n=struct.unpack_from('<I',ib)[0];assert len(ib)==4+28*n
        byname={}
        for row in range(n):
            off,raw,size=struct.unpack_from('<I20sI',ib,4+28*row);nm=raw.split(b'\0')[0].decode('ascii').casefold();byname.setdefault(nm,[]).append((row,off,size))
        INDEX_CACHE[str(idx)]=byname
    hits=INDEX_CACHE[str(idx)].get(name.casefold(),[])
    if not hits:return None
    assert len(hits)==1;row,off,size=hits[0];assert off+size<=pak.stat().st_size
    with pak.open('rb') as f:f.seek(off);raw=f.read(size)
    return dict(namespace='native-LiTo' if native else 'shared-client',archive=stem,member=name,index_source_id=reg(idx,'native-resource',stem+' index'),archive_source_id=reg(pak,'native-resource',stem+' archive'),row_0based=row,offset=off,bytes=size,payload_sha256=b.sha(raw),header_hex=raw[:12].hex())
def icon(iconid):
    name=f'{iconid}.tbt';shard=sum(name.encode('ascii'))%16
    hit=asset(f'Sprite{shard:02}',name)
    if hit is not None:hit['routing']='ASCII filename sum %16 shard';return hit
    hit=asset('Sprite',name)
    if hit is not None:hit['routing']='base fallback after shard member absence'
    return hit

def sprblocks(raw):
    text=raw.decode('latin1');ls=text.split('\n');result={};off=0
    for n,l in enumerate(ls):
        m=re.match(r'^#(\d+)\b',l)
        if m:
            result[int(m[1])]=dict(line=n+1,offset=off,text=l.rstrip('\r'),parts=[l.rstrip('\r')])
        elif result:result[next(reversed(result))]['parts'].append(l.rstrip('\r'))
        off+=len(l.encode('latin1'))+1
    for row in result.values():row['text']='\n'.join(row.pop('parts')).rstrip();row['sha256']=b.sha(row['text'].encode('latin1'))
    return result

def phase2(refresh):
    assert hashfile(J)==b.PIN_JAR and hashfile(R/'0-客戶端/LiTo.bin')==b.PIN_BIN
    from resolve_baseline import repair
    b.sources=repair(load('sources.json')[:FIRST_SOURCE_COUNT]);b.provenance=load('provenance.json')[:FIRST_PROVENANCE_COUNT]
    # Owned extraction tools evolve; re-run output uses their current bytes,
    # while the first manifest keeps the earlier tool hash for traceability.
    for row in b.sources:
        if Path(row['path']).is_relative_to(P) and Path(row['path']).suffix in ['.py','.java']:
            row['previous_tool_sha256']=row.get('previous_tool_sha256',row['sha256']);row['sha256']=hashfile(row['path']);row['bytes']=Path(row['path']).stat().st_size
    reg(P/'resolve_baseline.py','server-policy','exact-pinned-input resolver')
    data=load('candidate-data.json');checks=load('validation.json')
    # Idempotent reset: original presentation records retain all raw metadata.
    for r in data['skills']:
        r['cast_action']=r['metadata'].get('actionId');r['cast_effect']=r['metadata'].get('castGfx');r['flight_effect']=None;r['hit_effect']=r['metadata'].get('impactGfx');r['buff_icon']=3080 if r['skill_id']==181 else None
        if r.get('target_selection')=='native-selector-only':r['target_selection']=r['metadata'].get('targeting') or r['metadata'].get('requestShape')
        r['metadata'].pop('original_native_dispatch_class',None);r['metadata'].pop('original_native_handler_va',None)
    toolid=reg(Path(__file__),'server-policy','stage2-extraction-tool')
    helperid=reg(P/'ReadOnlyStage2.java','resource-derived','pure reflection / SELECT helper')
    if refresh:
        jdir=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin')
        subprocess.run([str(jdir/'javac.exe'),'-encoding','UTF-8','-cp',str(P),str(P/'ReadOnlyStage2.java')],check=True)
        for profile in ['lito-legacy','tw13081901-v1']:
            args=[str(jdir/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile='+profile]
            if profile=='tw13081901-v1':args+=['-Dlito.commonCatalog='+str(R/'3-lito開發/lito登入器/LiTo381-Launcher/app/common_catalog.bin')]
            r=subprocess.run(args+['-cp',str(P)+';'+str(J),'ReadOnlyStage2','--presentation'],check=True,capture_output=True,encoding='utf-8');write('pure-presentation-'+profile+'.json',json.loads(r.stdout))
    pure={profile:load('pure-presentation-'+profile+'.json') for profile in ['lito-legacy','tw13081901-v1']}
    duration={r['skillId']:r for r in pure['tw13081901-v1']['presentation_duration_records']}
    assert all(not v['network_opened'] and not v['database_used'] for v in pure.values())
    pureids={profile:reg(P/('pure-presentation-'+profile+'.json'),'resource-derived','pure-functions/'+profile) for profile in pure}
    protoid=reg(J,'server-policy','deployed-0.6.60','local/lito381/server/Protocol.class');sessionid=reg(J,'server-policy','deployed-0.6.60','local/lito381/server/ClientSession.class');profileid=reg(J,'server-policy','deployed-0.6.60','local/lito381/server/SharedClientResources.class')
    srcprotoid=reg(src('Protocol.java'),'server-policy','working-source; differs from deployed bytecode');srcsessionid=reg(src('ClientSession.java'),'server-policy','working-source; activation not inferred')
    methods=jar_methods('Protocol');profile_methods=jar_methods('SharedClientResources');session_methods=jar_methods('ClientSession')
    selected_methods={k:methods[k] for k in ['attackSpell','rangedPhysicalAttack','effectObject','mageShield','showMap']}
    assert 'SharedClientResources.effect' in selected_methods['attackSpell'] and 'SharedClientResources.effect' in selected_methods['effectObject']
    assert 'sipush        30285' in profile_methods['effect']
    required=['useAttackSpell','useTurnUndeadSpell','useInvisibilitySpell','useEarthBlessSpell','useShieldSpell']
    assert all(k in session_methods for k in required)
    assert 'Protocol.attackSpell' in session_methods['useAttackSpell']
    assert 'Protocol.effectObject' in session_methods['useTurnUndeadSpell'] and 'Protocol.effectObject' in session_methods['useInvisibilitySpell']
    assert 'Protocol.mageShield' in session_methods['useShieldSpell']
    assert 'coexistGraphic' in session_methods['useEarthBlessSpell']
    # Compact executable evidence: exact selected bytecode + relevant call sites.
    write('stage2-jar-consumers.json',dict(profile_effect=profile_methods['effect'],protocol=selected_methods,client_consumers={k:[l.strip() for l in session_methods[k].split('\n') if any(t in l for t in ['effectGfx','castGfx','Protocol.effectObject','Protocol.attackSpell','Protocol.mageShield','coexistGraphic','icon=omitted','reserved'])] for k in required},jar_sha256=b.PIN_JAR,source_warning='working Protocol.java lacks SharedClientResources.effect calls present in deployed class; use bytecode/pure fixtures'))
    twp=R/'0-客戶端/TW13081901.txt';twsid=reg(twp,'native-resource','shared-TW13081901');tw=sprblocks(twp.read_bytes())
    oldraw,oldsid,oldlink=resource('Text','list.spr');old=sprblocks(oldraw)
    lt_raw,lt_sid,ltlink=resource('LTtext','LTlist.spr');lt=sprblocks(lt_raw)
    spr_evidence=[];phases=[]
    def addphase(skill,binding,phase,gfx,profile,role,sid,loc,method):
        row=dict(presentation_id=len(phases)+1,skill_id=skill,binding=binding,phase=phase,profile=profile,effect_id=gfx,role=role,resource_version='TW13081901' if profile=='tw13081901-v1' else 'retained-Text/list.spr; embedded native binding not asserted',source_id=sid,enabled=0)
        phases.append(row);trace('lito_cand_skill_phase',row['presentation_id'],[k for k in row if k!='enabled'],sid,loc,method)
    resolutions=[]
    for gfx in [754,5906,5907,5909,5910,2941,527]:
        assert gfx in tw
        decl=tw[gfx];olddecl=old.get(gfx)
        reason='reserved dummy; slot rejected before cast' if gfx in [5906,5907,5909,5910] else 'coexist metadata; no packet consumer' if gfx==527 else 'declared sprite without animation action in TW; packet object-effect consumer exists'
        assert not re.search(r'\d+\.(?:fly|fire)\(',decl['text'])
        resolutions.append(dict(effect_id=gfx,tw_declared=True,tw_animation_present=False,classification=reason,tw_source_id=twsid,tw_line=decl['line'],tw_block_sha256=decl['sha256'],tw_declaration=decl['text'],retained_text_source_id=oldsid,retained_line=olddecl['line'] if olddecl else None,retained_animation_present=bool(olddecl and re.search(r'\d+\.(?:fly|fire)\(',olddecl['text'])),not_fixed_by_fallback=True))
    for r in data['skills']:
        key=f"{r['skill_id']}|{r['binding']}";r['usable_skill']=0 if r['binding']=='RESERVED_SKILL_SLOTS' else None
        r['presentation_status']='reserved-not-emitted' if r['usable_skill']==0 else 'picker-only' if r['binding']=='CLASS_EXTENSION_PICKER' else 'metadata-recovered; gameplay-availability-unverified'
        if r['usable_skill']==0:
            r['cast_action']=r['cast_effect']=None
            trace('lito_cand_skill',key,['cast_action','cast_effect','usable_skill','presentation_status'],sessionid,'useSpell reserved-slot rejection branches','retained slot values are raw metadata only; no emitted casting action or effect; slot122 excluded')
            continue
        if r['binding']=='ATTACK_SPELLS':
            gfx=r['metadata']['effectGfx'];decl=tw[gfx]
            if '0.fly(' in decl['text']:
                r['flight_effect']=gfx;trace('lito_cand_skill',key,['flight_effect'],twsid,f"line={decl['line']} SPR#{gfx}; 0.fly",'deployed useAttackSpell -> Protocol.attackSpell, shared profile keeps original ID; explicit SPR fly action')
                addphase(r['skill_id'],r['binding'],'flight',gfx,'tw13081901-v1','explicit SPR 0.fly + S_ATTACK mode0',twsid,f"line={decl['line']}",'packet route and original resource action agree')
                impacts=re.findall(r'109\.effect\(\s*(\d+)\s+(-?\d+)\s*\)',decl['text'])
                if impacts and int(impacts[0][1])>0:
                    effectmode,impact=map(int,impacts[0]);assert impact in tw
                    r['hit_effect']=impact;trace('lito_cand_skill',key,['hit_effect'],twsid,f"SPR#{gfx} line={decl['line']}; 109.effect({effectmode} {impact}); SPR#{impact} line={tw[impact]['line']}",'explicit SPR chained-effect relation; native effect-controller collision/timing not rerun')
                    addphase(r['skill_id'],r['binding'],'resource-chained-impact',impact,'tw13081901-v1',f'SPR109 mode{effectmode}; emitted flight parent{gfx}',twsid,f"line={tw[impact]['line']}",'resource chain; exact effect timing not inferred')
                spr_evidence.append(dict(skill_id=r['skill_id'],sprite_id=gfx,source_id=twsid,line=decl['line'],block_sha256=decl['sha256'],declaration=decl['text']))
        if r['skill_id'] in [18,97]:
            gfx=r['metadata']['effectGfx'];phase='success-target-object' if r['skill_id']==18 else 'caster-object'
            if r['skill_id']==97:r['cast_effect']=gfx;trace('lito_cand_skill',key,['cast_effect'],sessionid,'useInvisibilitySpell Protocol.effectObject','caster object effect after cast; resource lacks animation; not a projectile')
            addphase(r['skill_id'],r['binding'],phase,gfx,'tw13081901-v1','S_EFFECT 0x37 object effect; visual animation absent in TW',sessionid,'useTurnUndeadSpell' if r['skill_id']==18 else 'useInvisibilitySpell','observed deployed bytecode call; no visible playback claim')
    # Physical arrows have no skill ID namespace: explicit SPR66 fly, S_ATTACK 0x7f.
    assert '0.fly(' in tw[66]['text'];arrow=dict(projectile_key='physical-arrow',skill_id=None,projectile_gfx=66,packet_opcode=0x23,flight_type=0x7f,resource_version='TW13081901',impact_gfx=None,enabled=0)
    trace('lito_cand_projectile','physical-arrow',[k for k in arrow if k!='enabled'],twsid,f"SPR#66 line={tw[66]['line']}",'explicit 0.fly; 109.effect(3 -1) does not identify impact; pure rangedPhysicalAttack proves flight_type127')
    data['skill_phases']=phases;data['projectiles']=[arrow]
    write('stage2-sprite-evidence.json',dict(unmatched_resolutions=resolutions,flight_declarations=spr_evidence,physical_arrow_declaration=tw[66],namespace_warning='LTlist.spr #66 is shining effect; shared SPR#66 is arrow. Same integers are distinct namespaces.',retained_resource_links=[oldlink,ltlink]))
    # Buff maps: explicit extension pairs and native receiver branches.
    pe_path=R/'3-lito開發/lito登入器/舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';pe=PE(pe_path);pesid=reg(pe_path,'native-code','selected-unpacked-PE; VA mapping')
    assert hashfile(pe_path)=='4271d92fb2b9a2fa8d118dde0767f6d8a67a896227bcc24127438fcb5c2ab6e9'
    asm_path=R/'3-lito開發/lito登入器/舊版分析與驗收/ida-client-oep-v2/full-disassembly.asm';asmsid=reg(asm_path,'native-code','selected-unpacked-ASM');asm=lines(asm_path)
    native_rows=list(struct.iter_unpack('<IIIII',pe.read(0x6328D8,77*20)))
    inc=R/'3-lito開發/lito登入器/LiTo381-Launcher/native/dragon_status.inc';incsid=reg(inc,'client-extension','0.6.50 retained source / current file');inct=inc.read_text(encoding='utf-8')
    skills=[int(x) for x in re.search(r'kIllusionSkills\[\]=\{([^}]+)',inct)[1].split(',')];icons=[int(x) for x in re.search(r'kIllusionIcons\[\]=\{([^}]+)',inct)[1].split(',')];assert len(skills)==len(icons)==9
    xmlp=R/'3-lito開發/lito登入器/LiTo381-Launcher/tools/class_compat/shared-effectlist2.decoded.xml';xmlsid=reg(xmlp,'native-resource','shared-effectlist2-TW13081901');xml=[]
    for n,l in enumerate(xmlp.read_text(encoding='utf-8').split('\n'),1):
        if '<effect ' in l:attrs=dict(re.findall(r'(\w+)="([^"]*)"',l));attrs['line']=n;xml.append(attrs)
    issid=reg(J,'server-policy','deployed-0.6.60','local/lito381/server/IllusionStatusIcons.class');ismethods=jar_methods('IllusionStatusIcons');assert 'Protocol.mageShield' in ismethods['sync']
    runtimeid=reg(S/'docs/maintenance/illusion-presentation-0.6.50.json','runtime-adjusted','retained-0.6.50-fixture')
    buffs=[];conflicts=[]
    pairs=[(181,3080,77)]+[(skill,ico,78+i) for i,(skill,ico) in enumerate(zip(skills,icons))]
    for skill,ico,slot in pairs:
        dd=duration[skill];matches=[e for e in xml if e.get('graphic')==str(dd['effectGfx']) and e.get('icon')==str(ico)];assert len(matches)==1
        e=matches[0];ico_asset=icon(ico);assert ico_asset is not None
        row=dict(skill_id=skill,icon_id=ico,skill_panel_icon=None,status_slot=slot,packet_opcode=0x6d,packet_selector=skill,server_duration_seconds=dd['durationSeconds'],resource_duration_seconds=int(e['duration']),off_icon_id=int(e['offIcon']) if 'offIcon' in e else None,clear_duration_seconds=0,mapping_kind='extension; explicit XML graphic+icon+name',verification='deployed server consumer + extension source + retained native fixture; current DLL equality not asserted',resource_version='shared effectlist2/TW13081901 + LiTo extension',asset=ico_asset,enabled=0)
        # Separate panel icon from status bitmap, including3079/3080.
        panel=R/'3-lito開發/lito登入器/LiTo381-Launcher/native/class_skills.inc';pt=panel.read_text(encoding='utf-8');pis=[int(v) for v in re.search(r'g_sharedNewSkillIcons\[40\]=\{([^}]+)',pt)[1].split(',')]
        row['skill_panel_icon']=pis[skill-181] if 181<=skill<=220 else None
        row['resource_named_icon']=ico;row['semantic_conflict']=False
        buffs.append(row);key=skill
        trace('lito_cand_buff',key,['icon_id','status_slot','packet_selector','packet_opcode','clear_duration_seconds','mapping_kind','skill_panel_icon'],incsid,f'ExtendedStatusSlot / kIllusionIcons; slot={slot}; hook VA0x004B2E61','explicit extension identity; zero seconds removes timer; slot and icon are distinct; panel icon separately sourced')
        trace('lito_cand_buff',key,['resource_duration_seconds','off_icon_id','resource_version'],xmlsid,f"effect id={e['id']} line={e['line']}",'match distinct effect graphic + status bitmap; preserve XML duration separately from authoritative lease')
        trace('lito_cand_buff',key,['server_duration_seconds'],reg(J,'server-policy','0.6.60','local/lito381/server/BattleSpellCatalog.class'),dd['binding'],'presentation duration read from pinned deployed record; not copied combat formula')
        trace('lito_cand_buff',key,['verification'],runtimeid,'native_test.nine_buff_icons=true; manual_test_pending=true','retained fixture confirms nine add/render/clear; not rerun or manual acceptance')
        target=next(r for r in data['skills'] if r['skill_id']==skill);target['buff_icon']=ico;trace('lito_cand_skill',f"{skill}|{target['binding']}",['buff_icon'],incsid,f'ExtendedStatusSlot slot={slot}','explicit extension + XML corroboration; current consumer traced')
    # Actual legacy selector mapping. Keep semantic conflicts visible instead
    # of silently equating a selector with an XML icon.
    for skill in [3,99,151]:
        dd=duration[skill];selector=dd['iconType'];slot={3:5,4:6,5:7,8:8}.get(selector,4);actual_icon=native_rows[slot][0]
        expected=[e for e in xml if e.get('graphic')==str(dd['effectGfx']) and not 'INDEPENDENT' in e.get('startMsgName','')]
        resource_icon=int(expected[0]['icon']) if len(expected)==1 else 485 if skill==3 else None
        conflict=resource_icon is not None and resource_icon!=actual_icon
        if conflict:conflicts.append(dict(skill_id=skill,packet_selector=selector,native_slot=slot,consumed_icon=actual_icon,resource_named_icon=resource_icon))
        row=dict(skill_id=skill,icon_id=actual_icon,skill_panel_icon=None,status_slot=slot,packet_opcode=0x6d,packet_selector=selector,server_duration_seconds=dd['durationSeconds'],resource_duration_seconds=None,off_icon_id=None,clear_duration_seconds=0,mapping_kind='native hc receiver; semantic conflict requires review' if conflict else 'native hc receiver',verification='deployed mageShield producer + selected PE/ASM receiver; actual mapped icon, not corrected intended icon',resource_version='selected-native-PE status rows + shared-TW icons',asset=icon(actual_icon),resource_named_icon=resource_icon,semantic_conflict=conflict,enabled=0);buffs.append(row)
        trace('lito_cand_buff',skill,['icon_id','status_slot','packet_selector','packet_opcode','clear_duration_seconds'],pesid,f'VA0x{0x6328D8+slot*20:08X}; file_offset={pe.offset(0x6328D8+slot*20)}; sub_4AFF30','type3->slot5,type4->6,type5->7,type8->8, other->4; actual consumed bitmap; no semantic substitution')
        if not conflict:
            target=next(r for r in data['skills'] if r['skill_id']==skill);target['buff_icon']=actual_icon
    data['buffs']=buffs;write('stage2-buff-evidence.json',dict(native_receiver=dict(virtual_address='0x004AFF30',asm_start_line=lineof(asm_path,'sub_4AFF30 proc'),native_table_va='0x006328D8',source_id=pesid),extension_source_sha256=hashfile(inc),extension_matches_retained_0_6_50_pin=hashfile(inc)==load('configuration-reference-select.json').get('unused','6e4bbf450fe7c7552665a1c10f2cfb69b706f44d1da949d5d4391e8b9f72b8b8'),semantic_conflicts=conflicts,illusion_sync_consumer=[l.strip() for l in ismethods['sync'].split('\n') if any(t in l for t in ['mageShield','32767','999999999','patience','send:','lconst_0'])],duration_note='XML DragonSkin1800 differs from deployed lease1200; packet authoritative remaining seconds; clear0; no XML-duration override'))
    # Native map page bounds: exactly two declared DWORD image entries followed
    # by the literal %d.alp, not an18-entry map image table.
    images=list(struct.unpack('<II',pe.read(0x62F7BC,8)));assert images==[0x1a2c,0x1a2d] and pe.read(0x62F7C4,7)==b'%d.alp\0'
    page_links=[]
    for i,image in enumerate(images):
        hit=asset('LTsurf',f'{image}.img',native=True);assert hit is not None;page_links.append(dict(wire_page=i+1,zero_based_index=i,image_resource_id=image,display_map_id=[500,560][i],asset=hit,pe_source_id=pesid,table_virtual_address=f'0x{0x62F7BC+i*4:08X}',enabled=0))
    for row in data['map_items']:
        matched=next((x for x in page_links if x['wire_page']==row['page']),None);row['native_page_verified']=matched is not None;row['native_image_resource']=matched['image_resource_id'] if matched else None;row['native_display_map_id']=matched['display_map_id'] if matched else None;row['original_item_page_binding_confirmed']=None
        row['verification']='deployed item->page; native page1/2 image bound; original item intent unknown' if matched else 'deployed inherited18-page mapping; native image table has no declared entry; withheld'
        trace('lito_cand_map_item',row['item_id'],['native_page_verified','native_image_resource','native_display_map_id','original_item_page_binding_confirmed','verification'],pesid,'VA0x0062F7BC/0x0062F7C4; S_SHOW_MAP VA0x004AFFB0; page-1','exactly two declared native page images; rest are not native bindings; original item mapping still only deployed catalog projection')
    data['map_pages']=page_links;write('stage2-map-page-evidence.json',dict(receiver=dict(virtual_address='0x004AFFB0',line=lineof(asm_path,'sub_4AFFB0 proc'),grammar='dc',inventory_object_validated=True,page_index='wire_page - 1'),image_table=dict(virtual_address='0x0062F7BC',file_offset=pe.offset(0x62F7BC),values=images,next_bytes_hex=pe.read(0x62F7C4,7).hex(),asm_line=lineof(asm_path,'dword_62F7BC dd')),pages=page_links,withheld_pages=list(range(3,19)),warning='page is renderer index; never map ID. Inherited common18-item mapping is not a native18-page table. Original item intent unproven.'))
    # Portal anomaly: preserve exact bytes and static geometry, never relocate.
    anomaly=next(r for r in data['portals'] if (r['source_map_id'],r['source_x'],r['source_y'])==(525,32748,32999));sourcepath=Path(b.sources[anomaly['source_id']-1]['path']);raw=sourcepath.read_bytes();rstart=anomaly['record_offset'];recordbytes=raw[rstart:rstart+1+len(bytes.fromhex(anomaly['name_hex']))+8]
    targetpath=R/'0-客戶端/LiTo/LTMap/525/80008000.s32';attrs,_,aoff=b.parse_sector(targetpath)
    localx,localy=30,25;idx=localy*64+localx
    edges={'north':attrs[idx][0],'east':attrs[idx][1],'south':attrs[(localy+1)*64+localx][0],'west':attrs[localy*64+localx-1][1]}
    assert all(v&1 for v in edges.values()) and (anomaly['target_x'],anomaly['target_y'])==(32798,32793)
    tsid=reg(targetpath,'native-resource','LiTo-S32 Layer3');write('stage2-portal-525-evidence.json',dict(source_source_id=anomaly['source_id'],source_record_offset=rstart,raw_record_hex=recordbytes.hex(),name_hex=anomaly['name_hex'],decoded_destination={'x':32798,'y':32793,'raw_x_signed_half_delta':60,'raw_y_signed_delta':25},target_source_id=tsid,target_sector='80008000',local_cell={'x':localx,'y':localy},layer3_attribute_offset=aoff+idx*4,cardinal_attribute_words=edges,all_four_bit0_closed=True,coordinate_changed=False,conclusion='resource bytes and packed coordinate conversion confirmed; static geometry blocks target; no evidence of parser fault; original dynamic override or historical use unknown',native_layer7_parser={'va':'0x00484C49','asm_line':lineof(asm_path,'loc_484C49:')},standing_test_limit='standing is server/resource-derived proxy, not original portal admission policy'))
    # Rental metadata and public template comparison; never assign by name.
    selected=load('configuration-reference-select.json');selectsid=reg(P/'configuration-reference-select.json','shared-db','SELECT-snapshot-2026-10-14');assert selected['mysql_version']=='5.5.22'
    descraw,descsid,desclink=resource('LTtext','LTdesc-c.tbl');desc=[l for l in descraw.decode('cp950').splitlines() if l.strip() and not l.lstrip().startswith(';')][1:]
    assert len(desc)>95
    similarity={r['resource_index']:r for r in selected['rental_template_similarity']};rentmaps=[]
    for row in data['rental_offers']:
        ri=row['resource_index'];assert desc[ri].strip()==row['name'].strip();hit=asset('LTicon',f'{ri}.lbt',native=True)
        assert hit is not None
        sim=similarity[ri];row['mapped_shared_item_id']=None;row['mapping_proven']=False
        rentmaps.append(dict(resource_index=ri,retained_source_item_id=row['source_item_id'],native_description=desc[ri],native_wire_name_id=20000+ri,icon_asset=hit,same_name_candidates=sim['same_name_candidates'],mapped_shared_item_id=None,mapping_proven=False,enabled=0))
        trace('lito_cand_rental_offer',ri,['mapped_shared_item_id','mapping_proven'],selectsid,'rental_template_similarity resource_index='+str(ri),'25 unique same-name candidates are similarity evidence only; differinggfx/enchant and missing original crosswalk prevent mapping')
    data['rental_mappings']=rentmaps;data['rental_currency']=[dict(currency_key='retained-light-gem',retained_item_id=49358,native_resource_index=5,native_wire_name_id=20005,native_description=desc[5],mapped_shared_item_id=None,same_name_shared_candidates=selected['currency_name_candidates'],official_crosswalk_confirmed=False,enabled=0)]
    write('stage2-rental-evidence.json',dict(metadata_links=desclink,description_header_declared_count=int(descraw.decode('cp950').splitlines()[0]),observed_nonblank_noncomment_descriptions=len(desc),header_is_not_index_bound=True,native_resource_range='LTdesc13..90 match all78 retained names; paired icon namespace20000+index is a renderer identity, not shared item ID',same_name_item_count=sum(bool(r['same_name_candidates']) for r in rentmaps),same_name_candidate_count=sum(len(r['same_name_candidates']) for r in rentmaps),proven_shared_mappings=0,currency=dict(native_description=desc[5],native_index=5,retained_id=49358,shared_same_name_candidates=selected['currency_name_candidates']),template_comparison='type/groundgfx/weight/enchant/damage equality flags and SHA only; no full shared equipment template copied'))
    # SQL field coverage for additions and corrected values.
    keys={'skill_phases':['presentation_id'],'projectiles':['projectile_key'],'buffs':['skill_id'],'map_pages':['wire_page'],'rental_mappings':['resource_index'],'rental_currency':['currency_key']}
    tables={'skill_phases':'lito_cand_skill_phase','projectiles':'lito_cand_projectile','buffs':'lito_cand_buff','map_pages':'lito_cand_map_page','rental_mappings':'lito_cand_rental_mapping','rental_currency':'lito_cand_rental_currency'}
    for domain,fields in keys.items():
        for row in data[domain]:
            key='|'.join(str(row[k]) for k in fields);covered={r['field_name'] for r in b.provenance if r['table_name']==tables[domain] and r['primary_key']==key}
            for field in sorted(row.keys()-covered):trace(tables[domain],key,[field],toolid,'stage2 derivation '+field,'unknown remains NULL; review disabled; see specific domain evidence','offline derivation/policy only')
    for domain,table,keyfields in [('skills','lito_cand_skill',['skill_id','binding']),('map_items','lito_cand_map_item',['item_id']),('rental_offers','lito_cand_rental_offer',['resource_index'])]:
        for row in data[domain]:
            key='|'.join(str(row[k]) for k in keyfields);covered={r['field_name'] for r in b.provenance if r['table_name']==table and r['primary_key']==key}
            for field in sorted(row.keys()-covered):trace(table,key,[field],toolid,'stage2 '+field,'explicit policy/unknown or new evidence-derived metadata','offline policy/derived')
    if checks.get('unknown_effect_membership'):checks['phase1_unknown_effect_membership']=checks['unknown_effect_membership']
    checks['unknown_effect_membership']=[];checks['nonanimating_resource_declarations']=resolutions
    checks['stage2']=dict(date='2026-10-14 Asia/Taipei',unmatched_sprite_declarations_resolved=7,unmatched_unexplained=0,reserved_slots_excluded=sum(r['usable_skill']==0 for r in data['skills']),skill122_usable=False,pure_packets_checked=sum(len(r['pure_packet_fixtures']) for r in pure.values()),pure_packet_profiles=2,network_or_game_started=False,flight_mappings=sum(r['flight_effect'] is not None for r in data['skills']),new_buffs=9,buff_rows=len(buffs),legacy_buff_semantic_conflicts=len(conflicts),map_pages_verified=2,inherited_pages_withheld=16,portal525_bytes_geometry_confirmed=True,portal525_dynamic_override_recovered=False,rental_name_matches=25,rental_proven_shared_mappings=0,sql_executed=False)
    checks['stage2_boundary']=dict(source_prefix=len(b.sources),provenance_prefix=len(b.provenance))
    write('candidate-data.json',data);write('sources.json',b.sources);write('provenance.json',b.provenance);checks['counts']={k:len(v) for k,v in data.items() if isinstance(v,list)}|{'sources':len(b.sources),'provenance':len(b.provenance)};write('validation.json',checks)
    write('stage2-resource-boundaries.json',RESOURCE_BOUNDARIES)
    write('stage2-changes.json',dict(phase1_manifest_sha256=hashfile(P/'phase1-manifest.json'),additions=checks['stage2'],counts=checks['counts'],source_files_stay_read_only=True,sql_executed=False))
    print(json.dumps(checks['stage2'],ensure_ascii=False))

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--refresh-packets',action='store_true');phase2(p.parse_args().refresh_packets)
