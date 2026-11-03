"""未匯入、未接線。Independent offline SQL/JSON/source verification; no DB API.
--seal writes a small package manifest after verification. Default checks seal.
"""
import argparse, hashlib, json, re, zipfile, struct
from pathlib import Path
P=Path(__file__).resolve().parent
def digest(b):return hashlib.sha256(b).hexdigest()
def doc(name):
    v=json.loads((P/name).read_text(encoding='utf-8-sig'));return v['rows'] if isinstance(v,dict) and v.get('package_status') and 'rows' in v else v
def values(text):
    # Independent MySQL 5.5 string lexer, default backslash escaping and doubled
    # quotes. Rejects malformed string or trailing tokens without a SQL server.
    result=[];i=0
    while i<len(text):
        while i<len(text) and text[i].isspace():i+=1
        if text[i]=="'":
            i+=1;s='';closed=False
            while i<len(text):
                c=text[i];i+=1
                if c=="'":
                    if i<len(text) and text[i]=="'":s+="'";i+=1
                    else:closed=True;break
                elif c=='\\':
                    assert i<len(text);c=text[i];i+=1;s+=dict(n='\n',r='\r',t='\t').get(c,c)
                else:s+=c
            assert closed;result.append(s)
        else:
            start=i
            while i<len(text) and text[i]!=',':i+=1
            token=text[start:i].strip();assert token=='NULL' or re.fullmatch(r'-?\d+',token),token
            result.append(None if token=='NULL' else int(token))
        while i<len(text) and text[i].isspace():i+=1
        if i<len(text):assert text[i]==',';i+=1
    return result
def verify(seal):
    d=doc('candidate-data.json');s=doc('sources.json');pv=doc('provenance.json');v=doc('validation.json')
    mapping={'lito_cand_source':s,'lito_cand_provenance':pv,'lito_cand_map':d['maps'],'lito_cand_portal':d['portals'],'lito_cand_skill':d['skills'],'lito_cand_zone':d['zones'],'lito_cand_map_item':d['map_items'],'lito_cand_rental_offer':d['rental_offers'],'lito_cand_npc_identity':d['npc_identities']}
    definitions={};schema=(P/'01-candidate-schema.sql').read_text(encoding='utf-8')
    for m in re.finditer(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE=InnoDB DEFAULT CHARSET=utf8;',schema):
        fields={}
        for f in re.finditer(r'(?:^|, )([a-z_]+) (INT|SMALLINT|TINYINT|BIGINT|VARCHAR|CHAR|TEXT|LONGTEXT)(?:\((\d+)\))?( UNSIGNED)?([^,]*)',m[2]):fields[f[1]]=(f[2],int(f[3]) if f[3] else None,bool(f[4]),'NOT NULL' in f[5] or 'PRIMARY KEY' in f[5])
        definitions[m[1]]=fields
    schema_map=doc('schema-map.json') if (P/'schema-map.json').exists() else None
    if schema_map:
        for table,meta in schema_map['tables'].items():mapping[table]=d[meta['domain']]
    assert set(definitions)==set(mapping)
    cursors={k:0 for k in mapping};literal_count=0
    data=(P/'02-candidate-data.sql').read_text(encoding='utf-8')
    for line in data.splitlines():
        if not line.startswith('INSERT'):continue
        m=re.fullmatch(r'INSERT INTO `([^`]+)` \((.*?)\) VALUES \((.*)\);',line);assert m,line[:100]
        table=m[1];assert table in mapping
        fields=re.findall(r'`([^`]+)`',m[2]);vals=values(m[3]);assert len(fields)==len(vals)
        assert fields==list(definitions[table]);r=mapping[table][cursors[table]];cursors[table]+=1
        for key,value in zip(fields,vals):
            expected=r.get(key)
            if isinstance(expected,(dict,list)):expected=json.dumps(expected,ensure_ascii=False,separators=(',',':'))
            assert value==expected,(table,key,value,expected)
            typ,length,unsigned,required=definitions[table][key]
            if value is None:assert not required,(table,key);continue
            if typ in ['VARCHAR','CHAR']:assert len(value)<=length,(table,key,len(value),length)
            if typ in ['TINYINT','SMALLINT','INT','BIGINT']:
                bits={'TINYINT':8,'SMALLINT':16,'INT':32,'BIGINT':64}[typ]
                lo,hi=(0,2**bits-1) if unsigned else (-2**(bits-1),2**(bits-1)-1)
                assert lo<=value<=hi,(table,key,value)
            if key=='enabled':assert value==0
            literal_count+=1
    assert cursors=={k:len(vv) for k,vv in mapping.items()}
    # Namespace checks deliberately use the native terrain inventory, since
    # resource-only source/destination maps are outside the 47 active rows.
    terrain={r['map_id'] for r in doc('terrain-index.json')}
    assert all(r['source_map_id'] in terrain and r['target_map_id'] in terrain for r in d['portals'])
    assert all(r['raw_tag'] in [None,100,101,102] for r in d['portals'])
    assert all(r['gameplay_confirmed'] is None for r in d['maps'])
    if 'skill_phases' in d:
        flight={(r['skill_id'],r['effect_id']) for r in d['skill_phases'] if r['phase']=='flight'}
        assert all(r['flight_effect'] is None or (r['skill_id'],r['flight_effect']) in flight for r in d['skills'])
        assert all(r.get('usable_skill')==0 and r['cast_effect'] is None for r in d['skills'] if r['binding']=='RESERVED_SKILL_SLOTS')
        assert next(r for r in d['skills'] if r['skill_id']==122)['usable_skill']==0
        assert all(r['mapped_shared_item_id'] is None and not r['mapping_proven'] for r in d['rental_mappings'])
        assert len(d['map_pages'])==2 and {r['wire_page'] for r in d['map_pages']}=={1,2}
        assert all(r['buff_icon'] is None for r in d['skills'] if r['skill_id'] in [3,151])
    else:assert all(r['flight_effect'] is None for r in d['skills'])
    pkeys={'lito_cand_map':['map_id'],'lito_cand_portal':['source_map_id','source_x','source_y'],'lito_cand_skill':['skill_id','binding'],'lito_cand_zone':['zone_key'],'lito_cand_map_item':['item_id'],'lito_cand_rental_offer':['resource_index'],'lito_cand_npc_identity':['resource_index']}
    if schema_map:
        for table,meta in schema_map['tables'].items():pkeys[table]=meta['primary_key']
    coverage={(r['table_name'],r['primary_key'],r['field_name']) for r in pv}
    for group in d.get('lineage_groups',[]):
        inherited=[(r['table_name'],r['primary_key'],group['field_name']) for r in pv if r['table_name']==group['target_table'] and r['field_name']==group['group_name']]
        coverage.update(inherited)
    for table,keys in pkeys.items():
        seen=set()
        for row in mapping[table]:
            key='|'.join(str(row[k]) for k in keys);assert key not in seen;seen.add(key)
            assert all((table,key,f) in coverage for f in definitions[table]),(table,key,'provenance coverage')
    assert len({(r['table_name'],r['primary_key'],r['field_name'],r['source_id'],r['locator']) for r in pv})==len(pv)
    for src in s:
        path=Path(src['path'])
        if src['member']:
            raw=zipfile.ZipFile(path).read(src['member']);actual=digest(raw);size=len(raw)
        else:
            h=hashlib.sha256()
            with path.open('rb') as f:
                for chunk in iter(lambda:f.read(4*1024*1024),b''):h.update(chunk)
            actual=h.hexdigest();size=path.stat().st_size
        assert size==src['bytes'] and actual==src['sha256'],path
    for text in [schema,data]:
        assert '\ufffd' not in text and all(ord(c)<65536 for c in text)
        assert not re.search(r'^\s*(DROP|TRUNCATE|DELETE|REPLACE|UPDATE|ALTER|USE|CREATE DATABASE)\b',text,re.M|re.I)
        assert '-- 未匯入、未接線' in text
    assert v['mysql_version']=='5.5.22' and not v['sql_executed'] and not v['gameplay_retested']
    packet_count=0
    for profile in ['lito-legacy','tw13081901-v1']:
        name='pure-presentation-'+profile+'.json'
        if not (P/name).exists():continue
        fixtures=doc(name);assert not fixtures['network_opened'] and not fixtures['database_used']
        for r in fixtures['pure_packet_fixtures']:
            raw=bytes.fromhex(r['hex']);builder=r['builder'];packet_count+=1
            if builder in ['attackSpell','rangedPhysicalAttack']:
                assert raw[0]==0x23 and len(raw)==29
                gfx=struct.unpack_from('<H',raw,16)[0]
                expected=r['input_gfx']
                if builder=='attackSpell' and profile=='lito-legacy':expected={11736:10,11738:1809,11746:1815,11748:1815}.get(expected,expected)
                assert gfx==expected and raw[18]==(127 if builder=='rangedPhysicalAttack' else 0)
            elif builder=='effectObject':
                assert raw[0]==0x37 and len(raw)==7
                expected=r['input_gfx'] if profile=='tw13081901-v1' else {11722:744,11723:829,11724:830,11725:831}.get(r['input_gfx'],r['input_gfx'])
                assert struct.unpack_from('<H',raw,5)[0]==expected
            elif builder=='mageShield':assert len(raw)==4 and raw[0]==0x6d and struct.unpack_from('<H',raw,1)[0]==r['duration'] and raw[3]==r['selector']
            elif builder=='showMap':assert len(raw)==6 and raw[0]==0x6e and raw[5]==r['page']
            else:raise AssertionError(builder)
    # Every structured asset/source reference resolves to an indexed source.
    def refs(obj):
        if isinstance(obj,dict):
            for key,val in obj.items():
                if key in ['source_id','archive_source_id','index_source_id','text_source_id','pe_source_id'] and val is not None:assert isinstance(val,int) and 1<=val<=len(s),(key,val)
                refs(val)
        elif isinstance(obj,list):
            for val in obj:refs(val)
    refs(d)
    if 'map_effects' in d:
        assert len({(r['map_id'],r['sector'],r['record_offset']) for r in d['map_effects']})==len(d['map_effects'])
        assert all(r['actor_role'] is None and r['x'] is None and r['y'] is None for r in d['map_effects'])
        assert all(r['actor_gfx'] is None and r['x'] is None and r['y'] is None for r in d['npc_services'])
        assert len(d['skill_requests'])==176 and {r['skill_id'] for r in d['skill_requests']}==set(range(1,177))
        assert all(0<=r['dispatch_class']<32 and r['availability_verified'] is None for r in d['skill_requests'])
    result=dict(package_status='未匯入、未接線',status='PASS (offline only)',sql_insert_rows=sum(cursors.values()),sql_literal_values_checked=literal_count,sql_roundtrip=True,integer_and_varchar_ranges=True,all_sql_field_provenance_covered=True,unique_keys=True,native_terrain_references=True,all_source_hashes_match=True,pure_packet_fixtures_independently_decoded=packet_count,all_asset_source_references_resolve=True,unknown_actor_fields_not_invented=True,sql_executed=False,production_wired=False)
    if seal:
        (P/'verification-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        files=[dict(file=f.relative_to(P).as_posix(),bytes=f.stat().st_size,sha256=digest(f.read_bytes())) for f in sorted(P.rglob('*')) if f.is_file() and f.name!='manifest.json']
        manifest=dict(status='未匯入、未接線',baseline='0.6.60',verification_date='2026-10-14 Asia/Taipei',sources_inventory_sha256=digest((P/'sources.json').read_bytes()),source_count=len(s),counts=v['counts'],checks=result,files=files)
        (P/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    else:
        manifest=doc('manifest.json')
        assert set(f['file'] for f in manifest['files'])=={f.relative_to(P).as_posix() for f in P.rglob('*') if f.is_file() and f.name!='manifest.json'}
        for f in manifest['files']:
            b=(P/f['file']).read_bytes();assert len(b)==f['bytes'] and digest(b)==f['sha256'],f['file']
    print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--seal',action='store_true');verify(p.parse_args().seal)
