from pathlib import Path
import json,hashlib,zipfile,struct
R=Path(__file__).resolve().parent;C=R.parents[1]/'db/candidates/resource-enrichment-0.6.60'
def read(n):
 v=json.loads((C/n).read_text(encoding='utf-8-sig'));return v.get('rows',v) if isinstance(v,dict) else v
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(8*1024*1024),b''):h.update(b)
 return h.hexdigest()
sources=read('sources.json');cache={};diff=[]
for s in sources:
 key=(s['path'],s['member']);p=Path(s['path'])
 if key not in cache:
  if s['member']:
   with zipfile.ZipFile(p) as z:b=z.read(s['member'])
   cache[key]=(len(b),hashlib.sha256(b).hexdigest())
  else:cache[key]=(p.stat().st_size,sha(p))
 size,h=cache[key]
 if size!=s['bytes'] or h!=s['sha256']:diff.append(dict(source_id=s['source_id'],path=str(p),expected=s['sha256'],actual=h,bytes=size))
 print('verified',s['source_id'],flush=True) if len(cache)%100==0 else None
assets=[];d=read('candidate-data.json')
def walk(x):
 if isinstance(x,dict):
  if 'payload_sha256' in x and 'archive' in x and 'member' in x:assets.append(x)
  for v in x.values():walk(v)
 elif isinstance(x,list):
  for v in x:walk(v)
walk(d);checks=[];indices={}
for a in assets:
 root=Path(r'D:/lineage開發/0-客戶端')/('LiTo' if a['namespace']=='native-LiTo' else '')
 idx=root/(a['archive']+'.idx');pak=root/(a['archive']+'.pak')
 if str(idx) not in indices:
  b=idx.read_bytes();count=struct.unpack_from('<I',b)[0];assert len(b)==4+28*count
  entries={}
  for i in range(count):
   off,nm,size=struct.unpack_from('<I20sI',b,4+28*i);entries.setdefault(nm.split(b'\0')[0].decode('ascii').lower(),[]).append((off,size))
  indices[str(idx)]=entries
 hits=indices[str(idx)].get(a['member'].lower(),[]);ok=False
 if len(hits)==1:
  off,size=hits[0]
  with pak.open('rb') as f:f.seek(off);data=f.read(size)
  ok=hashlib.sha256(data).hexdigest()==a['payload_sha256']
 checks.append(dict(archive=a['archive'],member=a['member'],unique=len(hits)==1,payload_matches=ok))
report=dict(source_count=len(sources),changed_sources=diff,assets_checked=len(checks),failed_assets=[a for a in checks if not a['payload_matches']],qualification='Historic package preserved; selected currently indexed assets must independently match payload hash')
(R/'source-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False),flush=True)
