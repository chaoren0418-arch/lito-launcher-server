"""Read the user-selected TW animation definitions and exact shared Sprite assets; no extraction or SQL writes."""
from pathlib import Path
import hashlib,json,re,struct,zipfile
R=Path(__file__).resolve().parent;D=R/'build/control-inventory';root=Path('D:/lineage開發/0-客戶端')
txt=Path('D:/lineage開發/2-登入器/打包建置/編碼器/TW13081901.txt')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda data:hashlib.sha256(data).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p.read_bytes()))
b=read(R/'resurrection-ingress/build/build.json');assert sha(Path(b['runtime']['path']).read_bytes())==b['runtime']['sha256']
with zipfile.ZipFile(b['runtime']['path'])as z:
 data=z.read('battle/spr-action-380.csv');server_ids={int(line.split(',')[0])for line in data.decode('utf-8-sig').splitlines()if line and line[0].isdigit()}
settings=read(D/'display-consumer-audit.json')['selected_source_settings'];ids={int(settings['KiringkuEffectSpr']),int(settings['DmgSprPhysicalMiss'])}
for k,v in settings.items():
 if k.startswith('DmgSpr')and 'Miss'not in k and 'Crit'not in k:ids.update(range(int(v),int(v)+10))
raw=txt.read_bytes();text=raw.decode('latin1');blocks={};matches=list(re.finditer(r'^#(\d+)\b',text,re.M))
for i,m in enumerate(matches):
 identity=int(m[1])
 if identity in ids:
  assert identity not in blocks
  block=text[m.start():matches[i+1].start()if i+1<len(matches)else len(text)]
  blocks[identity]=(m.start(),text.count('\n',0,m.start())+1,block)
indices={};pins=[]
for p in sorted(root.glob('Sprite*.idx')):
 content=p.read_bytes();count=struct.unpack_from('<I',content)[0];assert len(content)==4+28*count,p
 byname={}
 for row in range(count):
  offset,name,size=struct.unpack_from('<I20sI',content,4+28*row);name=name.split(b'\0')[0].decode('ascii').casefold();byname.setdefault(name,[]).append((row,offset,size))
 indices[p.stem]=byname;pins.append(pin(p))
rows=[];missing=[]
for identity in sorted(ids):
 assert identity in blocks,identity
 offset,line,block=blocks[identity]
 action=re.search(r'(?<!\d)0\.[^(\n]*\((\d+)\s+(\d+),([^)]*)\)',block);assert action,(identity,block)
 direction=int(action[1]);assert direction in (0,1)
 tokens=re.findall(r'(\d+)\.(\d+):(\d+)',action[3]);assert len(tokens)==int(action[2]),identity
 sheets={int(token[0])+d for token in tokens for d in range(8 if direction==1 else 1)};assets=[]
 for sheet in sorted(sheets):
  name=f'{identity}-{sheet}.spr';shard='Sprite'+str(sum(name.encode('ascii'))%16).zfill(2)
  hits=indices.get(shard,{}).get(name,[]);stem=shard
  if not hits:hits=indices.get('Sprite',{}).get(name,[]);stem='Sprite'
  if not hits:missing.append(name);continue
  assert len(hits)==1,(name,hits)
  row,start,size=hits[0];pak=root/(stem+'.pak');assert 0<size and start+size<=pak.stat().st_size
  with pak.open('rb')as f:f.seek(start);payload=f.read(size)
  assert len(payload)==size
  assets.append(dict(member=name,index=stem+'.idx',row_0based=row,archive=str(pak),archive_size=pak.stat().st_size,offset=start,bytes=size,sha256=sha(payload),header_hex=payload[:12].hex()))
 rows.append(dict(effect_id=identity,definition_line=line,definition_offset=offset,definition_sha256=sha(block.encode('latin1')),definition_inline=bool(re.search(r'^#[^\r\n]*0\.\(',block)),action0_frame_count=len(tokens),action0_duration_units=sum(int(t[2])for t in tokens),in_server_action_index=identity in server_ids,assets=assets))
v=dict(status='verified-definition-and-indexed-payloads'if not missing else 'missing-indexed-payloads',runtime=b['runtime'],definition=pin(txt),server_index=dict(entry='battle/spr-action-380.csv',sha256=sha(data)),indices=pins,driver=pin(Path(__file__)),effects=rows,missing_assets=missing,server_index_missing_effects=sorted(ids-server_ids),scope='Definitions, exact index routing, bounded payload presence and hashes only; no native render/UI or new consumer has been executed. Existing server CSV is SQL-derived; absent rows do not prove a TW text parser bug.',formal_deployment_changed=False,source_database_mutations=False,full_goal_achieved=False)
(D/'display-resource-audit.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(dict(status=v['status'],effects=len(rows),assets=sum(len(r['assets'])for r in rows),missing=missing)))
