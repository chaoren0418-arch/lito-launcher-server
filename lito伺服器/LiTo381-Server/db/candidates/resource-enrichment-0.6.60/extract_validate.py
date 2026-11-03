"""未匯入、未接線。Read-only inputs, offline outputs ONLY in this directory.
Python 3.12 stdlib. No DB connection, no game/server launch. --refresh-jar runs
only ReadOnlyCatalog via Java 17. The separately captured SELECT is never rerun.
"""
import argparse, csv, hashlib, io, json, re, struct, subprocess, zipfile
from pathlib import Path
from collections import Counter

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[5]
SERVER = OUT.parents[2]
ANALYSIS = ROOT / '3-lito開發/lito脫殼資源'
from resolve_baseline import resolve
JAR = resolve()
VERSION = 'LiTo-3.81C-selected/0.6.60'
PIN_JAR = 'bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43'
PIN_BIN = 'ce6ee68ada8fd37e6c096b1a27e46cb6888f1179f85defb5007ac60287360292'
sources, provenance, warnings = [], [], []
def sha(b): return hashlib.sha256(b).hexdigest()
def readjson(p):
    v=json.loads(p.read_text(encoding='utf-8-sig'));return v['rows'] if isinstance(v,dict) and v.get('package_status') and 'rows' in v else v
def writejson(name, value):
    if isinstance(value,list):value={'package_status':'未匯入、未接線','rows':value}
    else:value={'package_status':'未匯入、未接線',**value}
    (OUT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def source(path, kind, version=VERSION, member=None):
    path=Path(path).resolve()
    key=(str(path),member)
    for s in sources:
        if (s['path'],s['member'])==key:return s['source_id']
    b=zipfile.ZipFile(path).read(member) if member else path.read_bytes()
    s=dict(source_id=len(sources)+1,path=str(path),member=member,sha256=sha(b),bytes=len(b),kind=kind,version=version)
    sources.append(s);return s['source_id']
def prov(table,key,fields, sid,locator,method,grade,kind=None):
    for field in fields:
        provenance.append(dict(provenance_id=len(provenance)+1,table_name=table,primary_key=str(key),field_name=field,source_id=sid,source_kind=kind or sources[sid-1]['kind'],locator=str(locator),method=method,verification=grade))
def unique(rows, fields):
    keys=[tuple(r[f] for f in fields) for r in rows]
    assert len(keys)==len(set(keys)), ('duplicate key',fields)
def sqlval(v):
    if v is None:return 'NULL'
    if isinstance(v,bool):return str(int(v))
    if isinstance(v,(int,float)):return str(v)
    if isinstance(v,(dict,list)):v=json.dumps(v,ensure_ascii=False,separators=(',',':'))
    assert '\x00' not in v
    # MySQL 5.5 default mode: protect both backslash and quote.
    return "'"+v.replace('\\','\\\\').replace("'","''").replace('\r','\\r').replace('\n','\\n')+"'"
def insert(table,rows,fields):
    return '\n'.join('INSERT INTO `'+table+'` ('+','.join('`'+f+'`' for f in fields)+') VALUES ('+','.join(sqlval(r.get(f)) for f in fields)+');' for r in rows)+'\n'

def archive_text(name):
    manifest=readjson(ANALYSIS/'resources/archive-manifest.json')
    a=next(a for a in manifest['archives'] if a['stem']=='LTtext')
    # Rebuild real paths; never trust a damaged path string in old reports.
    for key,rel in [('index_source','0-客戶端/LiTo/LTtext.idx'),('pak_source','0-客戶端/LiTo/LTtext.pak')]:
        p=ROOT/rel; assert sha(p.read_bytes())==a[key]['sha256'].lower();source(p,'native-resource')
    idx=ANALYSIS/'resources/indices/LTtext.decoded.idx'
    # decoded_index basename is the existing pinned snapshot, not source authority.
    if not idx.exists():idx=ANALYSIS/'resources/indices'/Path(a['decoded_index']['path']).name
    b=idx.read_bytes();assert sha(b)==a['decoded_index']['sha256'].lower();source(idx,'resource-derived')
    n=struct.unpack_from('<I',b)[0];assert len(b)==4+28*n
    matches=[]
    for row in range(n):
        off,raw,size=struct.unpack_from('<I20sI',b,4+28*row)
        if raw.split(b'\0')[0].decode('ascii').casefold()==name.casefold():matches.append((row,off,size))
    assert len(matches)==1,(name,matches)
    row,off,size=matches[0];pak=ROOT/'0-客戶端/LiTo/LTtext.pak'
    with pak.open('rb') as f:f.seek(off);raw=f.read(size)
    assert len(raw)==size
    existing=next((ANALYSIS/'resources/decoded/LTtext').glob(f'row{row:05}-*'))
    codec=ANALYSIS/'codec/l1_decode.exe';assert sha(codec.read_bytes())==manifest['codec_executable']['sha256'].lower();source(codec,'resource-derived','existing-pinned-codec')
    tempraw=OUT/'_payload.raw';tempdecoded=OUT/'_payload.decoded'
    # Existing decoder confines output under executable's grandparent. Use an
    # identical temporary tool copy here so that every write stays in OUT.
    toolfolder=OUT/'_codec';toolfolder.mkdir(exist_ok=True);localcodec=toolfolder/'l1_decode.exe';localcodec.write_bytes(codec.read_bytes())
    tempraw.write_bytes(raw)
    try:
        subprocess.run([str(localcodec),'payload',str(tempraw),str(tempdecoded)],check=True,capture_output=True)
        decoded=tempdecoded.read_bytes();assert decoded==existing.read_bytes()
    finally:
        tempraw.unlink(missing_ok=True);tempdecoded.unlink(missing_ok=True);localcodec.unlink(missing_ok=True);toolfolder.rmdir()
    sid=source(existing,'native-resource')
    return decoded,sid,f'LTtext.idx row0={row}; pak offset={off}; bytes={size}; raw-sha256={sha(raw)}',row

def parse_sector(path):
    b=path.read_bytes();o=32768 # 8192 floor refs * 4
    def take(n):
        nonlocal o
        assert o+n<=len(b),('truncated',path,o,n)
        v=b[o:o+n];o+=n;return v
    def count(fmt):return struct.unpack(fmt,take(struct.calcsize(fmt)))[0]
    n=count('<H');take(n*6)
    aoff=o;attrs=list(struct.iter_unpack('<hh',take(16384)))
    n=count('<i');assert 0<=n<=100000
    for _ in range(n):take(2);k=count('<H');take(k*7)
    if o==len(b):return attrs,[],aoff
    n=count('<i');assert 0<=n<=100000;take(n*5)
    if o==len(b):return attrs,[],aoff
    n=count('<i');assert 0<=n<=100000;take(n*4)
    if o==len(b):return attrs,[],aoff
    n=count('<H');routes=[]
    sx,sy=int(path.stem[:4],16),int(path.stem[4:],16)
    ox,oy=32704+(sx-0x7fff)*64,32704+(sy-0x7fff)*64
    for i in range(n):
        roff=o;k=count('<B');name=take(k);x,y,tm,packed=struct.unpack('<BBHI',take(8));assert x<128 and y<64
        tag=count('<B') if o<len(b) and b[o] in (100,101,102) else None
        # Native destination stores signed half-tile X and signed Y deltas
        # from 32768. Preserve raw dword and corroborate every published row.
        dx,dy=struct.unpack('<hh',struct.pack('<I',packed))
        routes.append(dict(source_map_id=int(path.parent.name),source_x=ox+x//2,source_y=oy+y,target_map_id=tm,target_x=32768+dx//2,target_y=32768+dy,raw_destination_packed=packed,raw_target_x_delta=dx,raw_target_y_delta=dy,raw_tag=tag,name_hex=name.hex(),raw_local_x=x,raw_local_y=y,record_offset=roff,record_index=i,enabled=0))
    return attrs,routes,aoff

def main(refresh):
    assert OUT.name=='resource-enrichment-0.6.60'
    assert sha(JAR.read_bytes())==PIN_JAR
    assert sha((ROOT/'0-客戶端/LiTo.bin').read_bytes())==PIN_BIN
    source(JAR,'server-policy','deployed-0.6.60');source(ROOT/'0-客戶端/LiTo.bin','native-code','LiTo-3.81C-selected')
    audit=readjson(SERVER/'docs/maintenance/data-sources-0.6.60.json')
    auditid=source(SERVER/'docs/maintenance/data-sources-0.6.60.json','server-policy','audit-2026-10-13')
    for name in ['DATA-SOURCES.zh-TW.md'] :source(SERVER/'docs'/name,'server-policy')
    source(SERVER/'SQL-PERSISTENCE-GATES.zh-TW.md','server-policy')
    if refresh:
        java=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/java.exe')
        javac=java.with_name('javac.exe')
        subprocess.run([str(javac),'-encoding','UTF-8',str(OUT/'ReadOnlyCatalog.java')],check=True)
        r=subprocess.run([str(java),'-Dfile.encoding=UTF-8','-cp',str(OUT)+';'+str(JAR),'ReadOnlyCatalog'],capture_output=True,check=True,encoding='utf-8')
        (OUT/'deployed-catalog.json').write_text(r.stdout,encoding='utf-8')
    deployed=readjson(OUT/'deployed-catalog.json');static=readjson(OUT/'static-reference-select.json')
    writejson('static-reference-select.json',static)
    assert static['mysql_version']=='5.5.22'
    staticid=source(OUT/'static-reference-select.json','shared-db','SELECT-snapshot-2026-10-13')
    source(OUT/'ReadOnlyCatalog.java','resource-derived','candidate-tool-v1')
    # Existing SQL rows: provenance only, no candidate copies of their combat values.
    seedp=SERVER/'db/380_lito_seed_example.sql';seedsid=source(seedp,'server-policy','0.6.60-seed')
    seedtext=seedp.read_text(encoding='utf-8-sig');seedtables={}
    pkfields={'lito_battle_rule':['mode_id','map_id'],'lito_battle_object':['mode_id','map_id','role'],'lito_battle_spawn':['mode_id','map_id','wave','sort_order'],'lito_battle_player_spawn':['mode_id','map_id','spawn_index'],'lito_tournament_shop':['item_id']}
    for m in re.finditer(r'INSERT INTO `380_lito`\.`([^`]+)`\s*\((.*?)\) VALUES\s*(.*?);',seedtext,re.S):
        table=m[1];fields=re.findall(r'`([^`]+)`',m[2]);rows=[]
        for rm in re.finditer(r'\(([^()]*)\)',m[3]):
            toks=re.findall(r"'(?:''|[^'])*'|NULL|-?\d+",rm[1]);assert len(toks)==len(fields)
            vals=[None if t=='NULL' else t[1:-1].replace("''","'") if t.startswith("'") else int(t) for t in toks];r=dict(zip(fields,vals));rows.append(r)
            keys=pkfields[table];assert all(k in r for k in keys),(table,fields)
            key='|'.join(str(r[k]) for k in keys);line=seedtext[:m.start(3)+rm.start()].count('\n')+1
            for f in fields:
                kind='server-policy';method='seed value; prior SELECT equality is a dated snapshot, not native authority'
                if table=='lito_battle_player_spawn' and f in ('x','y','note'):kind='resource-derived';method='mixed terrain-derived / runtime-adjusted compatible deployment; exact per-row native origin unavailable'
                if table=='lito_tournament_shop' and f in ('buy_price','sell_price'):kind='retained-history';method='retained 2003-series price summarized by TournamentShopCatalog; not native bin'
                if table=='lito_battle_spawn' and f=='npc_id':kind='shared-db';method='public shared NPC template ID; rule remains project wave policy'
                prov(table,key,[f],seedsid,'line '+str(line),method,'seed-confirmed; prior-SELECT-equal',kind)
        seedtables[table]=rows
    assert {k:len(v) for k,v in seedtables.items()}=={'lito_tournament_shop':16,'lito_battle_rule':40,'lito_battle_object':26,'lito_battle_spawn':25,'lito_battle_player_spawn':380}
    # Map rows preserve resource slot including inactive/commented entries.
    raw,sid,loc,_=archive_text('LTmapstr-c.tbl');text=raw.decode('cp950');maps=[];slot=-1
    for line_no,line in enumerate(text.splitlines(),1):
        if '#' not in line:continue
        slot+=1
        if line.lstrip().startswith(';'):continue
        cols=line.split('#')
        if not cols[0].strip().isdigit():continue
        mid=int(cols[0]);r=dict(map_id=mid,raw_slot=slot,category=cols[1].strip(),name=cols[2].strip(),resource_version=VERSION,resource_exists=(ROOT/f'0-客戶端/LiTo/LTMap/{mid}').is_dir(),rule_in_20261013_snapshot=any(x['map_id']==mid for x in seedtables['lito_battle_rule']),gameplay_confirmed=None,enabled=0)
        maps.append(r);prov('lito_cand_map',mid,['map_id','raw_slot','category','name','resource_version'],sid,f'{loc}; line={line_no}', 'Big5 cp950; nonblank hash rows consume slot including comments/placeholders','resource-byte-confirmed; runtime-binding-unknown')
        prov('lito_cand_map',mid,['rule_in_20261013_snapshot'],auditid,'database_snapshot + seed_comparison','dated snapshot; rule existence is not gameplay support','prior-SELECT-only')
    jarrows=deployed['NativeLitoMapTableCatalog']['ACTIVE'];assert len(maps)==47
    assert [(r['raw_slot'],r['map_id'],r['category'],r['name']) for r in maps]==[(r['slot'],r['mapId'],r['category'],r['name']) for r in jarrows]
    unique(maps,['map_id']);unique(maps,['raw_slot']);mapids={m['map_id'] for m in maps}
    terrains,portals,sector_summary={},[],[]
    rootmap=ROOT/'0-客戶端/LiTo/LTMap'
    for p in sorted(rootmap.glob('*/*.s32'),key=lambda p:(int(p.parent.name),p.name)):
        mid=int(p.parent.name);sid=source(p,'native-resource');attrs,rs,aoff=parse_sector(p);key=(int(p.stem[:4],16),int(p.stem[4:],16));terrains.setdefault(mid,{})[key]=attrs
        sector_summary.append(dict(map_id=mid,sector=p.stem,source_id=sid,layer3_offset=aoff,portal_count=len(rs)))
        for r in rs:
            r['resource_version']=VERSION;r['source_id']=sid;portals.append(r)
            prov('lito_cand_portal',f"{mid}|{r['source_x']}|{r['source_y']}",[k for k in r if k not in ('enabled','source_id')],sid,f"Layer7 byte={r['record_offset']} record={r['record_index']}",'source local x//2 + sector origin; destination signed16 x//2/y +32768; all published routes corroborated; tag opaque','resource-byte-confirmed; gameplay-unverified')
    def edge(mid,x,y,attr):
        bx=0x7fff+(x-32704)//64;by=0x7fff+(y-32704)//64
        a=terrains.get(mid,{}).get((bx,by))
        return True if a is None else bool(a[((y-32704)%64)*64+(x-32704)%64][attr]&1)
    def standing(mid,x,y):return not all((edge(mid,x,y,0),edge(mid,x,y,1),edge(mid,x,y+1,0),edge(mid,x-1,y,1)))
    def reachable_neighbor(mid,x,y):
        return any(not block and standing(mid,nx,ny) for block,nx,ny in [(edge(mid,x,y,0),x,y-1),(edge(mid,x,y,1),x+1,y),(edge(mid,x,y+1,0),x,y+1),(edge(mid,x-1,y,1),x-1,y)])
    for r in portals:
        assert all(0<=r[k]<=65535 for k in ['source_x','source_y','target_x','target_y','source_map_id','target_map_id'])
        r['target_resource_exists']=r['target_map_id'] in terrains
        r['source_standing']=standing(r['source_map_id'],r['source_x'],r['source_y'])
        r['target_standing']=standing(r['target_map_id'],r['target_x'],r['target_y'])
        r['source_has_cardinal_neighbor']=reachable_neighbor(r['source_map_id'],r['source_x'],r['source_y'])
        r['target_has_cardinal_neighbor']=reachable_neighbor(r['target_map_id'],r['target_x'],r['target_y'])
        assert r['raw_tag'] in (None,100,101,102)
        if not all(r[k] for k in ['target_resource_exists','source_standing','target_standing','source_has_cardinal_neighbor','target_has_cardinal_neighbor']):warnings.append({'type':'portal-terrain-review','key':[r['source_map_id'],r['source_x'],r['source_y']]})
    unique(portals,['source_map_id','source_x','source_y'])
    # Compare recovered S32 portal rows with working source, never equate it to deployed activation.
    portalp=SERVER/'src/main/java/local/lito381/server/ArenaPortalCatalog.java';portaltext=portalp.read_text(encoding='utf-8');source(portalp,'server-policy','working-source')
    projected=[]
    for m in re.finditer(r'routes\((\d+), "([^"]+)"\)',portaltext):
        for token in m[2].split():
            x,y,tm,tx,ty,tag=map(int,token.split(','));projected.append((int(m[1]),x,y,tm,tx,ty,None if tag==-1 else tag))
    nativekeys={(r['source_map_id'],r['source_x'],r['source_y'],r['target_map_id'],r['target_x'],r['target_y'],r['raw_tag']) for r in portals}
    assert set(projected)<=nativekeys
    # Skill presentation: pinned deployed metadata, selected TW13081901 animation resource.
    jar=zipfile.ZipFile(JAR);classid=source(JAR,'server-policy','deployed-0.6.60','local/lito381/server/BattleSpellCatalog.class')
    animsid=source(JAR,'generated-resource','TW13081901 + LiTo LTlist.spr','battle/spr-action-tw13081901.csv')
    animationids={int(line.split(',')[0]) for line in jar.read('battle/spr-action-tw13081901.csv').decode('utf-8').splitlines() if line and not line.startswith('#')}
    spells=[]
    for rawr in sorted(deployed['spell_records'],key=lambda r:(r['skillId'],r['binding'])):
        r=dict(skill_id=rawr['skillId'],binding=rawr['binding'],resource_version='TW13081901 + native LiTo namespace (mixed)',cast_action=rawr.get('actionId'),cast_effect=rawr.get('castGfx'),flight_effect=None,hit_effect=rawr.get('impactGfx'),buff_icon=None,target_selection=rawr.get('targeting') or rawr.get('requestShape'),enabled=0,metadata=rawr)
        # EffectGfx alone does NOT specify flight vs hit; preserve raw metadata.
        gfx={k:v for k,v in rawr.items() if (k.endswith('Gfx') or k in ('coexistGraphic',)) and isinstance(v,int) and v>0}
        r['effect_resource_checks']={k:{'id':v,'in_tw_animation_catalog':v in animationids} for k,v in gfx.items()}
        r['shared_skill_exists']=r['skill_id'] in static['skills_ids']
        if not r['shared_skill_exists']:warnings.append(dict(type='reserved-skill-missing-shared-template',skill_id=r['skill_id'],binding=r['binding']))
        spells.append(r)
        key=f"{r['skill_id']}|{r['binding']}"
        prov('lito_cand_skill',key,['skill_id','binding','cast_action','cast_effect','hit_effect','metadata'],classid,'static field '+r['binding']+' record '+rawr['record_type'],'offline reflection; effectGfx retained without inferring phase; 0 remains explicit metadata','deployed-class-value-confirmed; native-authority-mixed')
        prov('lito_cand_skill',key,['shared_skill_exists'],staticid,'skills_ids','SELECT ID membership; no combat values copied','current-SELECT-confirmed')
        prov('lito_cand_skill',key,['effect_resource_checks'],animsid,'sprite-id column0','effect ID existence in selected deployed animation table; no visual/semantic test','animation-catalog-membership-only')
    unique(spells,['skill_id','binding'])
    # Explicit status icon evidence for Dragon Skin only. Legacy iconType is not bitmap ID.
    xmlp=ROOT/'3-lito開發/lito登入器/LiTo381-Launcher/tools/class_compat/shared-effectlist2.decoded.xml';xmlsid=source(xmlp,'native-resource','shared-TW13081901-effectlist2')
    xml=xmlp.read_text(encoding='utf-8-sig');m=re.search(r'<effect id="107"[^>]+>',xml);assert m and 'icon="3080"' in m[0] and 'DRAGON_SKIN_S' in m[0]
    # Corroborate the decoded shared XML's encrypted payload against current
    # Tile.pak, retaining its separate shared namespace and original row.
    tileidx=ROOT/'0-客戶端/Tile.idx';tb=tileidx.read_bytes();tn=struct.unpack_from('<I',tb)[0];assert len(tb)==4+28*tn
    hits=[(i,struct.unpack_from('<I20sI',tb,4+28*i)) for i in range(tn) if b'effectlist2.xml' in tb[4+28*i:4+28*(i+1)]];assert len(hits)==1
    tile_row,(tile_off,_,tile_len)=hits[0];tilepak=ROOT/'0-客戶端/Tile.pak'
    with tilepak.open('rb') as f:f.seek(tile_off);tile_raw=f.read(tile_len)
    reference=readjson(ROOT/'3-lito開發/lito登入器/LiTo381-Launcher/tools/class_compat/dragon-presentation-reference.json')
    assert sha(tile_raw)==reference['raw_entry_sha256'] and sha(xmlp.read_bytes())==reference['decoded_sha256']
    tileidxsid=source(tileidx,'native-resource','shared-TW13081901');tilepaksid=source(tilepak,'native-resource','shared-TW13081901')
    writejson('resource-links.json',[dict(decoded_source_id=xmlsid,archive_source_id=tilepaksid,index_source_id=tileidxsid,archive_row_0based=tile_row,archive_member='effectlist2.xml',offset=tile_off,bytes=tile_len,raw_payload_sha256=sha(tile_raw),decoded_sha256=sha(xmlp.read_bytes()),verification='current archive raw payload matches pinned retained decode; no rendering test')])
    reportp=SERVER/'docs/maintenance/dragon-presentation-0.6.48.json';reportsid=source(reportp,'runtime-adjusted','0.6.48-retained-test')
    for r in spells:
        if r['skill_id']==181:
            r['buff_icon']=3080;prov('lito_cand_skill',f"181|{r['binding']}",['buff_icon'],xmlsid,'effect id107; line '+str(xml[:m.start()].count('\n')+1),'explicit Dragon Skin name mapping; icon3080; not skill icon3079','resource-confirmed')
            prov('lito_cand_skill',f"181|{r['binding']}",['buff_icon'],reportsid,'native_status_icon + manual_validation','retained user and fixture confirmation; not rerun in 0.6.60','retained-runtime-confirmation')
    # Class picker extension evidence, no binary patching.
    cpp=ROOT/'3-lito開發/lito登入器/LiTo381-Launcher/native/class_skills.inc';cppsid=source(cpp,'client-extension','working-source/0.6.51-contract')
    block=cpp.read_text(encoding='utf-8').split('int __cdecl DragonSkillTargetRange',1)[1].split('__declspec',1)[0]
    picker={}
    for cm in re.finditer(r'((?:\s*case \d+:)+)\s*return (\d+);',block):
        for n in re.findall(r'case (\d+):',cm[1]):picker[int(n)]=int(cm[2])
    assert len(picker)==21
    represented={r['skill_id'] for r in spells}
    for skill in sorted(set(picker)-represented):
        # Picker-only evidence does not recover graphics or combat values.
        spells.append(dict(skill_id=skill,binding='CLASS_EXTENSION_PICKER',resource_version='native-LiTo-extension/0.6.51-source',cast_action=None,cast_effect=None,flight_effect=None,hit_effect=None,buff_icon=None,target_selection=None,enabled=0,metadata={'extension_picker_range':picker[skill]},effect_resource_checks={},shared_skill_exists=skill in static['skills_ids']))
    for r in spells:
        if r['skill_id'] in picker:
            r['target_selection']='extension-object-picker';r['metadata']['extension_picker_range']=picker[r['skill_id']]
            prov('lito_cand_skill',f"{r['skill_id']}|{r['binding']}",['target_selection'],cppsid,'DragonSkillTargetRange case '+str(r['skill_id']),'launcher extension; 21 IDs match retained 0.6.51 test count; deployed DLL byte equivalence not asserted','source-confirmed; retained-fixture-only')
    # Native zones, names and RGB, no inferred combat/safety rule.
    zb,zsid,zloc,_=archive_text('LTzone-c.tbl');zt=zb.decode('cp950');zones=[];colors=[]
    for ln,line in enumerate(zt.splitlines(),1):
        cm=re.match(r'\s*(\d+) (\d+) (\d+)\s*;',line)
        if cm:colors.append(dict(color_index=len(colors),red=int(cm[1]),green=int(cm[2]),blue=int(cm[3])))
        zm=re.match(r'\s*"([^"]+)"\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)',line)
        if zm:
            color=colors[int(zm[2])];r=dict(zone_key=zm[1],color_index=int(zm[2]),map_id=int(zm[3]),min_x=int(zm[4]),min_y=int(zm[5]),max_x=int(zm[6]),max_y=int(zm[7]),red=color['red'],green=color['green'],blue=color['blue'],enabled=0);zones.append(r)
            prov('lito_cand_zone',r['zone_key'],[k for k in r if k!='enabled'],zsid,f'{zloc}; line={ln}','explicit text + referenced RGB; zero rectangle means resource whole-map convention only','resource-byte-confirmed')
    assert len(zones)==2 and len(colors)==2 and all(z['map_id'] in mapids for z in zones)
    # Map item mapping retained as a source projection pending native packet/page cross-check.
    itemclass=source(JAR,'server-policy','deployed-0.6.60','local/lito381/server/NativeMapItemCatalog.class');mapitems=[]
    for r0 in deployed['NativeMapItemCatalog']['ITEMS']:
        r=dict(item_id=r0['catalogId'],page=r0['page'],icon=r0['icon'],ground_gfx=r0['groundGfx'],name_id=r0['nameId'],shared_item_exists=r0['catalogId'] in static['etcitem_ids'],verification='deployed-projection; native-page-binding-pending',enabled=0);mapitems.append(r)
        prov('lito_cand_map_item',r['item_id'],[k for k in r if k not in ('enabled','verification','shared_item_exists')],itemclass,'ITEMS catalogId='+str(r['item_id']),'deployed mapping only; do not infer map ID from page','deployed-value-confirmed; native-binding-pending')
    assert len(mapitems)==18 and {r['page'] for r in mapitems}==set(range(1,19));unique(mapitems,['item_id'])
    # Rental prices only; never export retained equipment combat templates.
    rentalmember='rental/retained-lito-rental-merchant-2010.tsv';rentalsid=source(JAR,'retained-server','2010-L1J-compatibility',rentalmember);rentals=[];section=None;headers=None
    for ln,line in enumerate(jar.read(rentalmember).decode('utf-8-sig').splitlines(),1):
        if not line:continue
        if line.startswith('['):section=line.strip('[]');headers=None;continue
        if headers is None:headers=line.split('\t');continue
        r0=dict(zip(headers,line.split('\t')))
        itemid=int(r0['source_item_id'])
        r=dict(resource_index=int(r0['resource_index']),source_item_id=itemid,name=r0['selected_name'],maximum_seconds=int(r0['maximum_seconds']),light_gem_price=int(r0['light_gem_price']),currency_source_item_id=49358,source_npc_id=71286 if section=='weapon' else 71287,source_group=section,official_exact=False,shared_item_exists=itemid in set(static.get('weapon_ids',[])+static.get('armor_ids',[])+static['etcitem_ids']),enabled=0);rentals.append(r)
        prov('lito_cand_rental_offer',r['resource_index'],[k for k in r if k not in ('enabled','official_exact')],rentalsid,'TSV line '+str(ln),'retained 2010 prices/duration only; four merchant grouping is project policy','retained-resource-confirmed; original-official-unknown')
    unique(rentals,['resource_index']);assert len(rentals)==78
    # Resource identities: expose native description index, never actor graphics/coordinates.
    db,dsid,dloc,_=archive_text('LTdesc-c.tbl');npcs=[];description_rows=[]
    for ln,line in enumerate(db.decode('cp950').splitlines(),1):
        if line.strip() and not line.lstrip().startswith(';'):description_rows.append((ln,line))
    # Header count is not a row; native resource index is sequential from 0 after header.
    assert description_rows[0][1].strip().isdigit();description_rows=description_rows[1:]
    for idx in [0,1,2,3,4,10,11,12,91,92,93,94,95]:
        ln,name=description_rows[idx];r=dict(resource_index=idx,wire_name_id=20000+idx,description=name,actor_gfx=None,x=None,y=None,enabled=0);npcs.append(r)
        prov('lito_cand_npc_identity',idx,['resource_index','description','wire_name_id'],dsid,f'{dloc}; line={ln}','LTdesc sequential resource identity + protocol wire base20000; not SPR actor ID','resource-byte-confirmed; actor/position-unknown')
    # Validation summary and explicit remaining work.
    existing_coords=[]
    for table in ['lito_battle_object','lito_battle_player_spawn']:
        for r in seedtables[table]:
            if not standing(r['map_id'],r['x'],r['y']):existing_coords.append(dict(table=table,map_id=r['map_id'],x=r['x'],y=r['y']))
    shared_refs={'spawn_missing_npc':sorted({r['npc_id'] for r in seedtables['lito_battle_spawn']}-set(static['npc_ids'])),'map_item_missing_etcitem': [r['item_id'] for r in mapitems if not r['shared_item_exists']],'rental_item_missing_shared_ids':[r['source_item_id'] for r in rentals if not r['shared_item_exists']],'rental_currency_49358_in_shared_etcitem':49358 in static['etcitem_ids']}
    onlyterrain=sorted(set(terrains)-mapids)
    missingrule=[r['map_id'] for r in maps if not r['rule_in_20261013_snapshot']]
    checks=dict(status='passed-with-explicit-boundaries',mysql_version=static['mysql_version'],source_pin_jar=True,source_pin_bin=True,map_47_matches_deployed_jar=True,unique_keys=True,portal_coordinates_uint16=True,portal_tags_opaque=True,portal_catalog_subset_matches_native=True,portal_projected_rows=len(projected),native_portal_rows=len(portals),portal_terrain_warnings=sum(w['type']=='portal-terrain-review' for w in warnings),warnings=warnings,terrain_map_count=len(terrains),sector_count=len(sector_summary),resource_only_maps=onlyterrain,maps_without_rule_snapshot=missingrule,skill_records=len(spells),distinct_skill_ids=len({r['skill_id'] for r in spells}),skill_missing_shared_ids=sorted({r['skill_id'] for r in spells if not r['shared_skill_exists']}),unknown_effect_membership=[dict(skill_id=r['skill_id'],binding=r['binding'],field=k,id=v['id']) for r in spells for k,v in r['effect_resource_checks'].items() if not v['in_tw_animation_catalog']],class_picker_ids=len(picker),existing_seed_unstandable_coordinates=existing_coords,shared_reference_checks=shared_refs,sql_executed=False,sql_parser_server_validated=False,gameplay_retested=False,connectivity_scope='Layer3 cardinal neighbor and standing checks only; no original dynamic collision, whole-map connected path or instance admission proof',database_snapshot_scope='current SELECT static IDs/version; LiTo row values remain prior 2026-10-13 audit snapshot')
    data=dict(status='未匯入、未接線',maps=maps,portals=portals,skills=spells,zones=zones,map_items=mapitems,rental_offers=rentals,npc_identities=npcs)
    # Full field coverage for review tables: policy/unknowns and computed checks
    # are explicit, not falsely attached to native-resource authority.
    toolsid=source(OUT/'extract_validate.py','server-policy','candidate-extraction-v1')
    for table,rows,keyfields in [('lito_cand_map',maps,['map_id']),('lito_cand_portal',portals,['source_map_id','source_x','source_y']),('lito_cand_skill',spells,['skill_id','binding']),('lito_cand_zone',zones,['zone_key']),('lito_cand_map_item',mapitems,['item_id']),('lito_cand_rental_offer',rentals,['resource_index']),('lito_cand_npc_identity',npcs,['resource_index'])]:
        for r in rows:
            key='|'.join(str(r[f]) for f in keyfields)
            covered={p['field_name'] for p in provenance if p['table_name']==table and p['primary_key']==key}
            for f in sorted(r.keys()-covered):
                method='candidate policy: disabled for review' if f=='enabled' else 'unknown remains NULL; no original evidence' if r[f] is None else 'offline computed field; see extraction tool + terrain-index + static-reference-select'
                prov(table,key,[f],toolsid,'candidate derivation field '+f,method,'offline-policy-or-derived-only')
    # Preserve native static contract limits and unresolved historical paths.
    contract_refs=[]
    for name in ['map-table-source-contract.json','map-candidate-index-contract.json','skill-pane-layout-contract.json']:
        p=ANALYSIS/name;csid=source(p,'native-code','retained-static-contract');c=readjson(p)
        refs=c.get('sources',[])
        if 'fixed_PE' in c:refs.append(c['fixed_PE'])
        for ref in refs:
            rp=Path(ref['path']);exists=rp.is_file()
            contract_refs.append(dict(contract_source_id=csid,reported_path=ref['path'],actual_file_exists=exists,sha256_matches=(sha(rp.read_bytes())==ref['sha256'].lower()) if exists else None,limitation='static address is PE virtual address, not LiTo.bin disk offset; no runtime resource binding inferred'))
    writejson('native-static-locators.json',[
      dict(topic='map-table loader',contract='map-table-source-contract.json',virtual_addresses=['0x004D48D6','0x004D48E0','0x0046F9C0','0x004D4450'],qualification='retained contract; provider/runtime resource binding remains open; address not disk offset'),
      dict(topic='Layer7 opaque suffix',reference_source='modern_lito/map.rs parse_native_s32 comment and existing ArenaPortalCatalog',virtual_addresses=['0x00483A80','0x00484C49','0x00484C91'],qualification='existing parser reference; this run validates resources, does not redo disassembly or infer permission'),
      dict(topic='directional Layer3',reference_source='ArenaPassabilityCatalog SOURCE',virtual_addresses=['0x0048B640'],qualification='existing native algorithm mapping; this run validates standing/cardinal neighbor only'),
    ])
    writejson('evidence-boundaries.json',dict(referenced_files=contract_refs,source_kind_legend={'native-code':'原生程式／既有靜態分析','native-resource':'原生或共用客戶端資源；版本另列','resource-derived':'資源推導','runtime-adjusted':'保留執行校正與使用者確認','shared-db':'共用天堂 SELECT 定義','retained-history':'保留歷史說明','retained-server':'保留其他伺服器','server-policy':'專案／候選預設','client-extension':'登入器修補','generated-resource':'生成且隨版固定的資源'}))
    writejson('candidate-data.json',data);writejson('sources.json',sources);writejson('provenance.json',provenance);writejson('terrain-index.json',sector_summary);writejson('validation.json',checks)
    writejson('pending.json',[
      dict(item='地圖玩法支援',reason='47 resource rows / 40 dated rules; 2 towns and 5 siege partner/event rows are not automatic missing rules',map_ids=missingrule),
      dict(item='傳送正式接線',reason='tag100..102 opaque; dynamic collision, instance admission and whole-map reachability need separate tests'),
      dict(item='技能flight/hit與圖示',reason='effectGfx phase unresolved stays NULL; legacy iconType is protocol selector, not bitmap; only Dragon Skin icon3080 explicitly mapped'),
      dict(item='原生map-item頁面',reason='18 deployed mappings + static item IDs; source-page packet and artwork binding not independently recovered'),
      dict(item='NPC原始actor/座標、HP/AI/掉落/伤害',reason='not recovered; 26 objects HP3000 are project seed values; 25 waves map0/null positions are shared project defaults'),
      dict(item='租賃交易',reason='78 retained2010 prices; currency49358 belongs retained item namespace; current shared IDs/merchant actors + transaction lifecycle need explicit design'),
    ])
    # MySQL 5.5-compatible isolated review tables, no USE/CREATE DATABASE/FK to live tables.
    defs={
      'lito_cand_source':'source_id INT UNSIGNED PRIMARY KEY, path TEXT NOT NULL, member VARCHAR(255) NULL, sha256 CHAR(64) NOT NULL, bytes BIGINT UNSIGNED NOT NULL, kind VARCHAR(32) NOT NULL, version VARCHAR(96) NOT NULL',
      'lito_cand_provenance':'provenance_id INT UNSIGNED PRIMARY KEY, table_name VARCHAR(64) NOT NULL, primary_key VARCHAR(191) NOT NULL, field_name VARCHAR(64) NOT NULL, source_id INT UNSIGNED NOT NULL, source_kind VARCHAR(32) NOT NULL, locator TEXT NOT NULL, method TEXT NOT NULL, verification VARCHAR(96) NOT NULL',
      'lito_cand_map':'map_id SMALLINT UNSIGNED PRIMARY KEY, raw_slot SMALLINT UNSIGNED NOT NULL, category VARCHAR(32) NOT NULL, name VARCHAR(96) NOT NULL, resource_version VARCHAR(96) NOT NULL, resource_exists TINYINT NOT NULL, rule_in_20261013_snapshot TINYINT NOT NULL, gameplay_confirmed TINYINT NULL, enabled TINYINT NOT NULL DEFAULT 0',
      'lito_cand_portal':'source_map_id SMALLINT UNSIGNED NOT NULL, source_x SMALLINT UNSIGNED NOT NULL, source_y SMALLINT UNSIGNED NOT NULL, target_map_id SMALLINT UNSIGNED NOT NULL, target_x SMALLINT UNSIGNED NOT NULL, target_y SMALLINT UNSIGNED NOT NULL, raw_tag SMALLINT NULL, resource_version VARCHAR(96) NOT NULL, enabled TINYINT NOT NULL DEFAULT 0, PRIMARY KEY(source_map_id,source_x,source_y)',
      'lito_cand_skill':'skill_id SMALLINT UNSIGNED NOT NULL, binding VARCHAR(96) NOT NULL, resource_version VARCHAR(96) NOT NULL, cast_action SMALLINT NULL, cast_effect SMALLINT NULL, flight_effect SMALLINT NULL, hit_effect SMALLINT NULL, buff_icon SMALLINT NULL, target_selection VARCHAR(96) NULL, metadata LONGTEXT NOT NULL, enabled TINYINT NOT NULL DEFAULT 0, PRIMARY KEY(skill_id,binding)',
      'lito_cand_zone':'zone_key VARCHAR(32) PRIMARY KEY, color_index SMALLINT NOT NULL, map_id SMALLINT UNSIGNED NOT NULL, min_x SMALLINT UNSIGNED NOT NULL, min_y SMALLINT UNSIGNED NOT NULL, max_x SMALLINT UNSIGNED NOT NULL, max_y SMALLINT UNSIGNED NOT NULL, red TINYINT UNSIGNED NOT NULL, green TINYINT UNSIGNED NOT NULL, blue TINYINT UNSIGNED NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
      'lito_cand_map_item':'item_id INT UNSIGNED PRIMARY KEY, page SMALLINT UNSIGNED NOT NULL, icon SMALLINT UNSIGNED NOT NULL, ground_gfx SMALLINT UNSIGNED NOT NULL, name_id VARCHAR(32) NOT NULL, shared_item_exists TINYINT NOT NULL, verification VARCHAR(96) NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
      'lito_cand_rental_offer':'resource_index SMALLINT UNSIGNED PRIMARY KEY, source_item_id INT UNSIGNED NOT NULL, name VARCHAR(96) NOT NULL, maximum_seconds INT UNSIGNED NOT NULL, light_gem_price BIGINT UNSIGNED NOT NULL, currency_source_item_id INT UNSIGNED NOT NULL, source_npc_id INT UNSIGNED NOT NULL, source_group VARCHAR(32) NOT NULL, official_exact TINYINT NOT NULL DEFAULT 0, shared_item_exists TINYINT NOT NULL, enabled TINYINT NOT NULL DEFAULT 0',
      'lito_cand_npc_identity':'resource_index SMALLINT UNSIGNED PRIMARY KEY, wire_name_id SMALLINT UNSIGNED NOT NULL, description VARCHAR(191) NOT NULL, actor_gfx SMALLINT NULL, x SMALLINT NULL, y SMALLINT NULL, enabled TINYINT NOT NULL DEFAULT 0',
    }
    header='-- 未匯入、未接線；供審查，勿對正式庫執行。MySQL 5.5.22。\n-- 僅新增 lito_cand_* 隔離候選表；所有 enabled=0。無覆寫正式表。\n-- 未指定資料庫；日後須先決定管理流程、權限、載入驗證與遷移。\nSET NAMES utf8;\n'
    schema=header+'\n'.join('CREATE TABLE `'+k+'` ('+v+') ENGINE=InnoDB DEFAULT CHARSET=utf8;' for k,v in defs.items())+'\n'
    rows_by_table={'lito_cand_source':sources,'lito_cand_provenance':provenance,'lito_cand_map':maps,'lito_cand_portal':portals,'lito_cand_skill':spells,'lito_cand_zone':zones,'lito_cand_map_item':mapitems,'lito_cand_rental_offer':rentals,'lito_cand_npc_identity':npcs}
    fields={k:re.findall(r'(?:^|, )([a-z_]+) (?:INT|SMALLINT|TINYINT|BIGINT|VARCHAR|CHAR|TEXT|LONGTEXT)',v) for k,v in defs.items()}
    sql=header+'START TRANSACTION;\n'+''.join(insert(k,v,fields[k]) for k,v in rows_by_table.items())+'COMMIT;\n'
    assert not re.search(r'\b(DROP|TRUNCATE|DELETE|REPLACE|UPDATE|ALTER|CREATE DATABASE)\b',schema+sql,re.I)
    assert all(r.get('enabled',0)==0 for rows in rows_by_table.values() for r in rows)
    assert all(ord(c)<=0xffff for c in schema+sql),'utf8 3-byte incompatibility'
    assert '\ufffd' not in schema+sql
    (OUT/'01-candidate-schema.sql').write_text(schema,encoding='utf-8');(OUT/'02-candidate-data.sql').write_text(sql,encoding='utf-8')
    # Individual trace keys duplicate only when distinct evidence contributes to one field.
    unique(provenance,['table_name','primary_key','field_name','source_id','locator'])
    assert all(1<=p['source_id']<=len(sources) for p in provenance)
    checks['counts']={k:len(v) for k,v in data.items() if isinstance(v,list)}|{'sources':len(sources),'provenance':len(provenance)}
    checks['sql_offline_checks']={'no_destructive_or_live_table_statements':True,'utf8_three_byte_compatible':True,'replacement_characters_absent':True,'source_references_valid':True,'all_candidates_disabled':True,'DDL_DML_execution':False,'literal_roundtrip':'generated escaped values; independent lexer check in verify_package.py'}
    writejson('validation.json',checks)
    # Original inputs are rehashed at end, excluding owned candidate files that are regenerated.
    for s in sources:
        p=Path(s['path']);b=zipfile.ZipFile(p).read(s['member']) if s['member'] else p.read_bytes();assert sha(b)==s['sha256'],('source drift',p)
    print(json.dumps(checks['counts'],ensure_ascii=False));print('offline extraction passed; SQL NOT executed; live program NOT wired')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--refresh-jar',action='store_true');args=p.parse_args();main(args.refresh_jar)
