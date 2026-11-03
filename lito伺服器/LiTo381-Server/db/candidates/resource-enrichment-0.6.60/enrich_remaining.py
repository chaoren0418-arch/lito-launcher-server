"""未匯入、未接線。Remaining finite local domains; no DB/TCP/UI/process launch.
Requires completed stage2 extraction. Pure filesystem/JSON/resource analysis.
"""
import sys
sys.dont_write_bytecode=True
import re,json,struct
from collections import Counter,deque
from pathlib import Path
import extract_validate as b
import enrich_stage2 as e
P=e.P;R=e.R;S=e.S;A=e.A;J=e.J
def main():
    b.sources=b.readjson(P/'sources.json');b.provenance=b.readjson(P/'provenance.json')
    # Deterministic rerun removes only this stage's previous generated entries.
    old=b.readjson(P/'validation.json').get('stage2_boundary')
    if old:b.sources=b.sources[:old['source_prefix']];b.provenance=b.provenance[:old['provenance_prefix']]
    boundary={'source_prefix':len(b.sources),'provenance_prefix':len(b.provenance)}
    b.writejson('remaining-stage-boundary.json',boundary)
    data=b.readjson(P/'candidate-data.json');checks=b.readjson(P/'validation.json');tool=e.reg(Path(__file__),'server-policy','remaining-domain-extraction/2026-10-14')
    e.reg(P/'consolidate_sql.py','server-policy','candidate-schema-consolidation')
    snapshot=b.readjson(P/'configuration-reference-select.json');snapid=e.reg(P/'configuration-reference-select.json','shared-db','public-config-SELECT-2026-10-14')
    mapids={r['map_id'] for r in data['maps']};shared=b.readjson(P/'static-reference-select.json')
    # All Layer8 records are environmental visual resources, NEVER NPC spawns.
    rawfields=['map_id','sector','sprite_id','raw_x','raw_y','extended_value','record_offset']
    policyfields=['effect_record_id','source_id','x','y','actor_role','verification','enabled']
    groups=[dict(group_id=i+1,target_table='lito_cand_map_effect',group_name='@raw_fields' if field in rawfields else '@policy_unknown',field_name=field,interpretation='exact raw record/path/byte offset' if field in rawfields else 'world/actor fields NULL; review disabled; interpretation bound') for i,field in enumerate(rawfields+policyfields)]
    data['lineage_groups']=groups
    masks={};effects=[];tails=[];lt_raw,ltsid,ltlink=e.resource('LTtext','LTlist.spr');lt=e.sprblocks(lt_raw)
    for sector in b.readjson(P/'terrain-index.json'):
        path=Path(b.sources[sector['source_id']-1]['path']);raw=path.read_bytes();attrs,_,_=b.parse_sector(path);mid=sector['map_id'];key=(int(path.stem[:4],16),int(path.stem[4:],16));masks.setdefault(mid,{})[key]=bytes((a&1)|((c&1)<<1) for a,c in attrs)
        o=32768;n=struct.unpack_from('<H',raw,o)[0];o+=2+n*6+16384;n=struct.unpack_from('<i',raw,o)[0];o+=4
        for _ in range(n):count=struct.unpack_from('<H',raw,o+2)[0];o+=4+count*7
        for stride in [5,4]:
            if o==len(raw):break
            n=struct.unpack_from('<i',raw,o)[0];assert 0<=n<=100000;o+=4+n*stride
        if o==len(raw):continue
        n=struct.unpack_from('<H',raw,o)[0];o+=2
        for _ in range(n):count=raw[o];o+=1+count+8;o+=1 if o<len(raw) and raw[o] in (100,101,102) else 0
        if o==len(raw):continue
        packed=struct.unpack_from('<H',raw,o)[0];o+=2;count=packed&0x7fff;extended=bool(packed&0x8000)
        for i in range(count):
            offset=o;gfx,x,y=struct.unpack_from('<HHH',raw,o);extra=struct.unpack_from('<i',raw,o+6)[0] if extended else None;o+=10 if extended else 6
            assert o<=len(raw)
            row=dict(effect_record_id=len(effects)+1,map_id=mid,sector=path.stem,sprite_id=gfx,raw_x=x,raw_y=y,x=None,y=None,extended_value=extra,actor_role=None,record_offset=offset,source_id=sector['source_id'],verification='Layer8 raw visual record; LTlist-local namespace; not server NPC/HP/spawn; world transform not independently proven',enabled=0)
            effects.append(row);e.trace('lito_cand_map_effect',row['effect_record_id'],['@raw_fields'],sector['source_id'],f'Layer8 byte={offset} record={i}; stride={10 if extended else 6}','fields expanded via lito_cand_lineage_group; ushort sprite/x/y and raw i32; source file/path fields; no NPC inference')
            e.trace('lito_cand_map_effect',row['effect_record_id'],['@policy_unknown'],tool,'Layer8 interpretation policy','fields expanded via lito_cand_lineage_group; x/y/actor_role remain NULL, enabled0; not recovered actors','offline policy/unknown')
        if o<len(raw):tails.append(dict(map_id=mid,sector=path.stem,source_id=sector['source_id'],unknown_tail_offset=o,unknown_tail_bytes=len(raw)-o))
    data['map_effects']=effects
    # Static cardinal connected components for the18 portal source/dest maps.
    def edge(mid,x,y,attribute):
        values=masks.get(mid,{}).get((0x7fff+(x-32704)//64,0x7fff+(y-32704)//64))
        return True if values is None else bool(values[((y-32704)%64)*64+(x-32704)%64]&(1<<attribute))
    def standing(mid,x,y):return not all([edge(mid,x,y,0),edge(mid,x,y,1),edge(mid,x,y+1,0),edge(mid,x-1,y,1)])
    wanted={}
    for r in data['portals']:
        wanted.setdefault(r['source_map_id'],set()).add((r['source_x']<<16)|r['source_y']);wanted.setdefault(r['target_map_id'],set()).add((r['target_x']<<16)|r['target_y'])
    endpoint_components={};component_metrics=[]
    for mid in sorted(wanted):
        cells=set()
        for (bx,by),values in masks[mid].items():
            ox,oy=32704+(bx-0x7fff)*64,32704+(by-0x7fff)*64
            for i in range(4096):
                x,y=ox+(i%64),oy+i//64
                if standing(mid,x,y):cells.add((x<<16)|y)
        seeds=sorted(cells);unseen=set(cells);components=[];labels={}
        for seed in seeds:
            if seed not in unseen:continue
            cid=len(components)+1;q=deque([seed]);unseen.remove(seed);size=0
            while q:
                node=q.popleft();size+=1;x,y=node>>16,node&65535
                if node in wanted[mid]:labels[node]=cid
                for nx,ny,blocked in [(x,y-1,edge(mid,x,y,0)),(x+1,y,edge(mid,x,y,1)),(x,y+1,edge(mid,x,y+1,0)),(x-1,y,edge(mid,x-1,y,1))]:
                    nn=(nx<<16)|ny
                    if not blocked and nn in unseen:unseen.remove(nn);q.append(nn)
            components.append(size)
        endpoint_components[mid]=labels;component_metrics.append(dict(map_id=mid,standing_cell_count=len(cells),cardinal_component_count=len(components),component_sizes=components))
    portal_analysis=[]
    for r in data['portals']:
        sc=endpoint_components[r['source_map_id']].get((r['source_x']<<16)|r['source_y']);tc=endpoint_components[r['target_map_id']].get((r['target_x']<<16)|r['target_y'])
        portal_analysis.append(dict(source_map=r['source_map_id'],source_x=r['source_x'],source_y=r['source_y'],target_map=r['target_map_id'],source_component=sc,target_component=tc,same_static_component=(sc==tc) if r['source_map_id']==r['target_map_id'] and sc is not None and tc is not None else None,original_dynamic_admission_verified=False))
    b.writejson('terrain-connectivity.json',dict(maps=component_metrics,portals=portal_analysis,unknown_layer8_tails=tails,limitations='cardinal bit0 resource graph; portal can intentionally bridge disconnected components; no original dynamic blockers, diagonal timing, room admission or live gameplay proof'))
    # Current seven public config tables: exact dated reference, not executable
    # copied settings. Original player/source databases remain untouched.
    keys={'lito_battle_rule':['mode_id','map_id'],'lito_battle_object':['mode_id','map_id','role'],'lito_battle_spawn':['mode_id','map_id','wave','sort_order'],'lito_battle_player_spawn':['mode_id','map_id','spawn_index'],'lito_tournament_shop':['item_id'],'commands':['name'],'lito_source_server':['source_id']}
    seedtext=(S/'db/380_lito_seed_example.sql').read_text(encoding='utf-8-sig');seed={}
    for m in re.finditer(r'INSERT INTO `380_lito`\.`([^`]+)`\s*\((.*?)\) VALUES\s*(.*?);',seedtext,re.S):
        table=m[1];fields=re.findall(r'`([^`]+)`',m[2]);rows=[]
        for rm in re.finditer(r'\(([^()]*)\)',m[3]):
            toks=re.findall(r"'(?:''|[^'])*'|NULL|-?\d+",rm[1]);vals=[None if t=='NULL' else t[1:-1].replace("''","'") if t.startswith("'") else int(t) for t in toks];assert len(vals)==len(fields);rows.append(dict(zip(fields,vals)))
        seed[table]=rows
    refs=[];comparisons={};config_validation=[]
    category_by_mode={0:'petRace',1:'deathmatch',2:'questbattle',3:'siegewar',4:'timeattack',5:'honormatch'}
    mapcats={r['map_id']:r['category'] for r in data['maps']}
    for table,rows in snapshot['lito_configuration'].items():
        b.unique(rows,keys[table])
        if table in seed:
            index={tuple(r[k] for k in keys[table]):r for r in seed[table]};missing=[];changed=[]
            for row in rows:
                key=tuple(row[k] for k in keys[table]);expected=index.get(key)
                if expected is None or any(row.get(k)!=v for k,v in expected.items()):changed.append(list(key))
            actualkeys={tuple(r[k] for k in keys[table]) for r in rows};missing=[list(k) for k in index if k not in actualkeys]
            comparisons[table]=dict(seed_count=len(seed[table]),current_count=len(rows),changed_or_extra_keys=changed,missing_seed_keys=missing,exact_in_seed_columns=not missing and not changed)
        for row in rows:
            key='|'.join(str(row[k]) for k in keys[table]);origin='operator-config; project defaults/derived compatibility, never wholly native'
            valid={}
            if 'map_id' in row:valid['native_map_registered']=row['map_id'] in mapids if row['map_id'] else None
            if row.get('x') is not None and row.get('y') is not None:valid['standing_resource_geometry']=standing(row['map_id'],row['x'],row['y'])
            if table=='lito_battle_rule':valid['mode_category_matches']=mapcats.get(row['map_id'])==category_by_mode.get(row['mode_id'])
            if table=='lito_battle_spawn':valid['shared_npc_id_exists']=row['npc_id'] in shared['npc_ids'];valid['map_specific_spawn_recovered']=False
            if table=='lito_battle_object':valid['original_hp_recovered']=False;valid['hp_origin']='current project setting; no native value recovered'
            if table=='lito_battle_player_spawn':valid['original_spawn_coordinate_recovered']=False;valid['note_qualification']='native default note means compatible derived/runtime-adjusted deployment, not original server coords'
            if table=='lito_source_server':valid['fixed_source_namespace']=1<=row['source_id']<=8
            if table=='lito_tournament_shop':valid['item_in_shared_namespace']=row['item_id'] in set(shared['weapon_ids']+shared['armor_ids']+shared['etcitem_ids']);valid['price_origin']='retained2003-series project setting, not bin table'
            reference=dict(reference_id=len(refs)+1,target_table=table,target_key=key,observed_at_utc=snapshot['observed_at_utc'],payload=row,origin_kind=origin,validation=valid,source_id=snapid,enabled=0);refs.append(reference)
            e.trace('lito_cand_config_reference',reference['reference_id'],[k for k in reference if k!='enabled'],snapid,table+' PK='+key,'dated public SELECT reference only; never auto-applied; player fields not exported')
            for field in row:e.trace(table,key,[field],snapid,'SELECT '+table+' PK='+key,'current observed setting; per-field native/derived/history qualifications retained from phase1 and domain evidence','current-SELECT-confirmed; original authority varies')
            config_validation.append(dict(table=table,key=key,checks=valid))
    data['config_references']=refs;b.writejson('configuration-comparison.json',dict(observed_at_utc=snapshot['observed_at_utc'],seed_comparisons=comparisons,table_counts={k:len(v) for k,v in snapshot['lito_configuration'].items()},validation=config_validation,settlement_count_only=snapshot['settlement_count'],player_contents_exported=False))
    # Native resource text/images: only known inventory identities, not all
    # strings in the173-declared LTdesc interpreted as usable items.
    descraw,descsid,desclink=e.resource('LTtext','LTdesc-c.tbl');desclines=descraw.decode('cp950').split('\n');desc=[(i+1,l.strip()) for i,l in enumerate(desclines[1:],1) if l.strip() and not l.lstrip().startswith(';')]
    item_indices=[5,6,7,8,9]+list(range(13,91))+[96,97];items=[]
    for idx in item_indices:
        line,name=desc[idx];hit=e.asset('LTicon',f'{idx}.lbt',native=True);assert hit is not None
        classification='retained-rental/native-renderer-identity' if 13<=idx<=90 else 'native-currency-renderer-identity' if idx==5 else 'native-emblem-renderer-identity' if idx in [6,7,8,9] else 'native-map-or-teleport-service-item-identity'
        row=dict(resource_index=idx,wire_name_id=20000+idx,native_description=name,icon_asset=hit,mapped_shared_item_id=None,classification=classification,shared_name_text=None,source_id=descsid,enabled=0);items.append(row)
        e.trace('lito_cand_item_resource',idx,['resource_index','native_description','wire_name_id','source_id'],descsid,f"LTdesc physical line={line}; entry={idx}; {desclink['member']}",'known identity range corroborated by deployed rental/item consumers; 20000 namespace is not shared item ID')
        e.trace('lito_cand_item_resource',idx,['icon_asset'],hit['archive_source_id'],f"{hit['member']} row={hit['row_0based']} offset={hit['offset']}; payload={hit['payload_sha256']}",'current LTicon .lbt member; not shared Sprite .tbt')
    data['item_resources']=items
    # Shared map-item name strings: exact indexed text resource, no external
    # user/player text or shared base combat values copied.
    textraw,textsid,textlink=e.resource('Text','desc-c.tbl',allow_current_extract=True);textlines=textraw.decode('cp950').split('\n');header=textlines[0].strip();shared_names=[]
    for item in data['map_items']:
        key=int(item['name_id'][1:]);observed=textlines[key+1].rstrip('\r') if key+1<len(textlines) else None
        shared_names.append(dict(item_id=item['item_id'],name_id=item['name_id'],resource_text=observed,physical_line=key+2,source_id=textsid,interpretation='current unique desc-c.tbl indexed reference; original item-page mapping remains unproven'))
        item['resource_text']=observed;item['text_source_id']=textsid;item['text_locator']=f"desc-c.tbl line={key+2} row0={textlink['row_0based']}";item['shared_icon_asset']=e.icon(item['icon'])
        e.trace('lito_cand_map_item',item['item_id'],['resource_text','text_source_id','text_locator'],textsid,item['text_locator']+'; decoded SHA='+textlink['decoded_sha256'],'explicit current desc table index; named legacy18-map resources distinct from native2town pages; no page intent inferred')
        if item['shared_icon_asset']:
            hit=item['shared_icon_asset'];e.trace('lito_cand_map_item',item['item_id'],['shared_icon_asset'],hit['archive_source_id'],f"{hit['member']} row={hit['row_0based']} offset={hit['offset']}; raw SHA={hit['payload_sha256']}",'shared Sprite .tbt asset; independent from native LTicon .lbt')
    # Other-server context: inspect public table names and identity hits only.
    # Never export account/character/back-up contents or import another ruleset.
    reference_checks=[]
    for rel,kind in [('1-伺服器/db/380_lito.sql','retained-server'),('1-伺服器/db/380_db.sql','shared-db'),('1-伺服器/docs/icon狀態圖示.md','retained-server')]:
        path=R/rel;sid=e.reg(path,kind,'other-server/current shared reference; not native LiTo original');text=path.read_text(encoding='utf-8-sig');tables=re.findall(r'CREATE TABLE\s+`([^`]+)`',text)
        current=None;hits=Counter()
        for line in text.split('\n'):
            match=re.match(r'INSERT INTO\s+`([^`]+)`',line)
            if match:current=match[1]
            if current in ['weapon','armor','etcitem','npc','shop','spawnlist']:
                for token in ['49358','71286','71287']:
                    if re.search(r'(?<!\d)'+token+r'(?!\d)',line):hits[token]+=1
            if line.rstrip().endswith(';'):current=None
        reference_checks.append(dict(path=str(path),source_id=sid,table_names=tables,scoped_identity_hit_lines=dict(hits),qualification='shared/other-version reference only; native same-version provenance/crosswalk not demonstrated'))
    b.writejson('other-server-reference-audit.json',dict(references=reference_checks,players_or_account_values_exported=False,native_original_values_recovered=0,icon_warning='other server uses multiple effectlist/PacketBox routes; its packet meanings cannot be transferred to LiTo opcodeFA or hc receiver'))
    b.writejson('item-resource-evidence.json',dict(native_item_count=len(items),description_links=desclink,description_header=descraw.decode('cp950').splitlines()[0],unclassified_description_entries=[dict(resource_index=i,physical_line=ln,text=name) for i,(ln,name) in enumerate(desc) if i not in set(item_indices)|{r['resource_index'] for r in data['npc_identities']}],shared_map_item_names=shared_names,shared_text_links=textlink,shared_text_header=header,raw_icon_namespace='LTicon .lbt + wire20000; shared icons are Sprite .tbt',original_use_type_and_quantities_not_inferred=True))
    # NPC services: deployed identity→HTML projection, actions independently
    # read from raw localized HTML. No original actor/model/coords guessed.
    npcclass=e.reg(J,'server-policy','deployed-0.6.60','local/lito381/server/NativeLitoRentalTownNpcCatalog.class');npcdefs=b.readjson(P/'pure-presentation-tw13081901-v1.json')['npc_resource_services'];services=[];html_evidence=[];missing_html=[]
    for npc in npcdefs:
        ni=npc['resourceIndex'];assert npc['name']==desc[ni][1]
        choices=[(npc['startHtml'],'start'),(npc['endHtml'],'end')] if npc['endHtml'] else [(npc['startHtml'],'start')]
        for htmlkey,kind in choices:
            member=htmlkey+'-c.html';hits=list((A/'resources/decoded/LTtext').glob('*-'+member))
            if not hits:
                missing_html.append(dict(resource_index=ni,requested_member=member,reason='no exact localized member; no locale/key fallback assumed'))
                raw=None;sid=npcclass;actions=[None];loc='NPCS resourceIndex='+str(ni)
            else:
                raw,sid,link=e.resource('LTtext',member);text=raw.decode('cp950');actions=re.findall(r'\baction\s*=\s*"([^"]+)"',text,re.I) or [None];actions=list(dict.fromkeys(actions));loc=f"{member} row={link['row_0based']} offset={link['offset']}"
                html_evidence.append(dict(npc_resource_index=ni,html_key=htmlkey,kind=kind,member=member,links=link,actions=actions,source_id=sid))
            for action in actions:
                status='raw HTML action; current projection only; original transaction/endpoint/actor unavailable' if raw is not None else 'exact resource missing; projection withheld'
                row=dict(service_id=len(services)+1,npc_resource_index=ni,role=str(npc['role']),html_member=member if raw is not None else None,action_text=action,native_description=desc[ni][1],service_status=status,actor_gfx=None,x=None,y=None,source_id=sid,enabled=0);services.append(row)
                e.trace('lito_cand_npc_service',row['service_id'],[k for k in row if k not in ('enabled','actor_gfx','x','y')],sid,loc,'deployed NPC identity/HTML projection + exact native action text; no original actor packet recovered')
    # Shared service dialogs have no unique NPC endpoint assignment in the
    # surviving source; parse them as evidence, don't create actor coordinates.
    for member in ['NoRent-c.html','TeleportMe-c.html','TeleportMe_C-c.html','TeleportMe_L-c.html']:
        raw,sid,link=e.resource('LTtext',member);actions=re.findall(r'\baction\s*=\s*"([^"]+)"',raw.decode('cp950'),re.I);html_evidence.append(dict(npc_resource_index=None,member=member,actions=actions,links=link,source_id=sid,assignment='shared service HTML, endpoint/actor assignment unavailable'))
    data['npc_services']=services;b.writejson('npc-service-evidence.json',dict(deployed_catalog_records=len(npcdefs),services=html_evidence,missing_exact_html=missing_html,actor_and_coordinates_recovered=0,original_hp_ai_drop_damage_recovered=0,checked_sources=['native LTdesc/LTicon/LTtext HTML','all495 S32 Layer8: visual effects only','deployed NativeLitoRentalTownNpcCatalog','current LiTo object/spawn settings','retained2010 rental references; other-version actor matches are compatibility only'],next_required_evidence='original same-version S_PUT_OBJECT/spawn/config capture or original server tables; visual/client identity alone cannot prove server actor/HP/AI/drop'))
    # All176 original byte-dispatch rows, with conservative route labels.
    pe_path=R/'3-lito開發/lito登入器/舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';pe=e.PE(pe_path);pesid=e.reg(pe_path,'native-code','selected-unpacked-PE');table=pe.read(0x4CBD00,176);handlers=list(struct.unpack('<32I',pe.read(0x4CBC80,128)));assert handlers[1]==0x4CBBD4 and handlers[2]==0x4CB755 and all(v<32 for v in table)
    requests=[]
    for index,cl in enumerate(table):
        shape='ccc' if cl in [1,31] else None;route='native selector-only branch if dispatcher reached; availability separate' if cl in [1,31] else 'native object/direct-self dispatch via sub_4CA6E0; mode conditional' if cl==2 else 'specialized native branch; raw class/handler preserved; target semantics not guessed'
        row=dict(skill_id=index+1,raw_selector_index=index,dispatch_class=cl,handler_va=f'0x{handlers[cl]:08X}',packet_shape=shape,target_route=route,direct_cast_conditional=True if cl==2 else None,availability_verified=None,source_id=pesid,resource_version='selected-native-PE; before DLL extension/repair',enabled=0);requests.append(row)
        e.trace('lito_cand_skill_request',row['skill_id'],[k for k in row if k!='enabled'],pesid,f'byte-table VA0x{0x4CBD00+index:08X}; file_offset={pe.offset(0x4CBD00+index)}; jump-table VA0x{0x4CBC80+cl*4:08X}','176-bound native switch, class32 jump table; specialized classes remain unclassified; no UI availability/playability claim')
        target=next((r for r in data['skills'] if r['skill_id']==index+1),None)
        if target:
            target['metadata']['original_native_dispatch_class']=cl;target['metadata']['original_native_handler_va']=row['handler_va']
            if target['target_selection'] is None and target.get('usable_skill')!=0 and cl in [1,31]:target['target_selection']='native-selector-only';e.trace('lito_cand_skill',f"{target['skill_id']}|{target['binding']}",['target_selection','metadata'],pesid,f'byte-table VA0x{0x4CBD00+index:08X}; handler={row["handler_va"]}','CCC producer only; original availability separate; reserved slots excluded')
    data['skill_requests']=requests;b.writejson('skill-request-evidence.json',dict(native_rows=176,source_id=pesid,dispatcher_va='0x004CB5F0',switch_max_zero_based=175,byte_table_va='0x004CBD00',jump_table_va='0x004CBC80',handler_count=32,source_asm_lines={'switch':425123,'selector_handler':425828,'default_handler':425875},world_tree_131_note='original class3 differs from extension one-byte repair toclass1; no on-disk rewrite; preserve original and current-source override separately',six_picker_only_ids_remain=[192,202,203,207,208,213],all_candidates_disabled=True))
    # Fill provenance for every added review field. This is metadata coverage,
    # not a claim that NULL unknowns have been recovered.
    domains=[('config_references','lito_cand_config_reference',['reference_id']),('item_resources','lito_cand_item_resource',['resource_index']),('npc_services','lito_cand_npc_service',['service_id']),('skill_requests','lito_cand_skill_request',['skill_id']),('lineage_groups','lito_cand_lineage_group',['group_id']),('map_items','lito_cand_map_item',['item_id'])]
    for domain,table,keys in domains:
        b.unique(data[domain],keys)
        for row in data[domain]:
            key='|'.join(str(row[k]) for k in keys);covered={p['field_name'] for p in b.provenance if p['table_name']==table and p['primary_key']==key}
            for field in sorted(row.keys()-covered):e.trace(table,key,[field],tool,'remaining-stage '+field,'unknown stays NULL; candidate disabled; no original actor/numeric rule invented','offline policy/unknown')
    checks['remaining']=dict(public_config_rows=len(refs),config_tables=len(snapshot['lito_configuration']),five_seed_tables_current_equal=all(v['exact_in_seed_columns'] for v in comparisons.values()),layer8_environmental_records=len(effects),layer8_records_not_npc_spawns=True,portal_maps_with_cardinal_components=len(component_metrics),portal_endpoints_without_standing_component=sum(p['source_component'] is None or p['target_component'] is None for p in portal_analysis),native_item_identities=len(items),npc_service_records=len(services),native_dispatch_rows=len(requests),unclassified_native_description_count=len(desc)-len(items)-len(data['npc_identities']),original_actor_hp_ai_drop_damage_recovered=0,unknowns_not_counted_as_restored=True,sql_executed=False)
    # Close each original pending domain with actual extracted result and the
    # precise remaining evidence boundary; finite investigation is complete.
    conclusions=[
      dict(item='地圖玩法支援',local_result='47 raw rows/62 terrains/40current rules/176 native producer classes independently indexed',status='已提取與離線驗證；玩法驗收待外部證據',unrecovered='all47 gameplay_confirmed NULL; no new map enabled',required='same-version live scene/room producer and acceptance capture'),
      dict(item='傳送正式接線與可達性',local_result=f'186raw routes + {len(component_metrics)} maps static components; map525 exact record and4closed edges confirmed',status='已提取與靜態驗證；動態准入未知',unrecovered='original dynamic override/room admission for blocked destination; raw tag100..102 opaque',required='original runtime portal/dynamic-terrain evidence; do not relocate target'),
      dict(item='技能flight/hit與圖示',local_result='7nonanimation declarations explained;5flight/resource chains;12phase rows;13buff rows;176original dispatch rows',status='已資料化；2legacy buff semantic conflicts require review',unrecovered='specialized native target semantics, exact collision/playback timing/current DLL equivalence;6picker-only IDs preserve bound',required='qualified native effect-controller/target callbacks or existing same-version capture; no guessed gfx'),
      dict(item='原生map-item頁面與文字圖示',local_result='18item catalog/name rows,2native page image bindings,85known native inventory identities/icons',status='已提取；pages3..18原始binding未恢復',unrecovered='original18item page intent and native service item useType;73descriptions not classified as items',required='same-version original item-use producer/table and independent page table/artwork binding'),
      dict(item='NPC原始actor/座標、HP/AI/掉落/傷害',local_result=f'13native identities/{len(services)}service action rows;{len(effects)}Layer8 visual records;26objects/25waves/380player-spawn refs',status='可恢復身分／資源完成；原始服務端規則待外部證據',unrecovered='original actor, coordinates, HP, exact map-specific waves/AI/drop/damage; configuredHP3000 not original evidence',required='original same-version server tables or authoritative S_PUT_OBJECT/spawn/combat capture; other-version templates cannot prove original values'),
      dict(item='租賃NPC／商品／貨幣／服務',local_result='78native name/LBT/retained references +25unique-name template comparisons; currency native5/20005 vs retained49358 separately traced',status='參考資料完成；0proven shared ID mappings',unrecovered='shared item/currency crosswalk, original hotel keys/maps/endpoints/prices/transaction lifecycle',required='original crosswalk or same-version actor/shop transaction source; no same-name automatic pairing'),
    ]
    b.writejson('pending.json',conclusions);b.writejson('candidate-data.json',data);b.writejson('sources.json',b.sources);b.writejson('provenance.json',b.provenance);checks['counts']={k:len(v) for k,v in data.items() if isinstance(v,list)}|{'sources':len(b.sources),'provenance':len(b.provenance)};b.writejson('validation.json',checks)
    b.writejson('remaining-resource-boundaries.json',e.RESOURCE_BOUNDARIES)
    b.writejson('domain-completion.json',dict(all_planned_local_domains_investigated=True,scope='finite extraction + candidate design + offline validation + explicit unrecovered evidence; not full original game restoration',domains=conclusions,counts=checks['counts'],status='待最終整體驗證與交付',sql_executed=False))
    print(json.dumps(checks['remaining'],ensure_ascii=False))
if __name__=='__main__':main()
