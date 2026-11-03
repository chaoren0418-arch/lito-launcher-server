"""Independently serialize pinned map534 geometry identity from raw attributes.
No runtime, resource extraction or old Modern test execution.
"""
import collections, hashlib, json, struct, sys
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
OUT=Path(__file__).resolve().parent;OWNER=OUT.parent
PREV=OWNER/'cache-admission-20261008'
def require(ok,why):
 if not ok:raise ValueError(why)
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def pin(p):return dict(path=str(p),bytes=p.stat().st_size,sha256=sha(p))
def put(name,d):(OUT/name).write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def origin(s):return 32704+(s['x']-32767)*64,32704+(s['y']-32767)*64
def main():
 sources=read(PREV/'input-sources.json');sectors=read(PREV/'sector-parse.json')
 require(sha(PREV/'sector-parse.json')=='4434069296CFE20752ADA0CB97A6F38E2BC1E62AB5423E45185C5B7A4E6C3992','parsed layer offsets drift')
 consumer=[]
 for p in sources['decode_and_consumer_sources']:
  if Path(p['path']).name in ('map.rs','world_map.rs'):
   require(sha(Path(p['path']))==p['sha256'],'geometry source drift');consumer.append(p)
 resources=OWNER.parent/'lito重建版/LiTo381-Modern/src/modern_lito/resources.rs';consumer.append(pin(resources))
 cells={};cell_sector={};portal_indices={};raw=[]
 for s in sectors:
  p=Path(s['source']['path']);require(sha(p)==s['source']['sha256'] and p.stat().st_size==s['source']['bytes'],'sector pin drift')
  data=p.read_bytes();raw.append(data);start,end=s['layer_byte_ranges'][2]
  require(end-start==16384,'attribute extent');attrs=list(struct.iter_unpack('<hh',data[start:end]));portals=set()
  if len(s['layer_byte_ranges'])>=7:
   pos,limit=s['layer_byte_ranges'][6];n=struct.unpack_from('<H',data,pos)[0];pos+=2
   for _ in range(n):
    size=data[pos];pos+=1+size;x,y=data[pos:pos+2];pos+=8
    require(x<128 and y<64,'portal coordinate');portals.add(y*64+x//2)
    if pos<len(data) and 100<=data[pos]<=102:pos+=1
   require(pos==limit,'portal exact layer extent')
  key=(s['x'],s['y']);portal_indices[key]=portals;ox,oy=origin(s)
  for i,attr in enumerate(attrs):
   cell=(ox+i%64,oy+i//64);require(cell not in cells,'duplicate world cell')
   cells[cell]=None if i in portals else attr;cell_sector[cell]=key
 def cardinal(a,b):
  aa=cells.get(a);bb=cells.get(b)
  if aa is None or bb is None:return False
  dx=b[0]-a[0];dy=b[1]-a[1]
  if (dx,dy)==(0,-1):return aa[0]&1==0
  if (dx,dy)==(1,0):return aa[1]&1==0
  if (dx,dy)==(0,1):return bb[0]&1==0
  if (dx,dy)==(-1,0):return bb[1]&1==0
  return False
 def step(a,b):
  dx=b[0]-a[0];dy=b[1]-a[1]
  if dx==0 or dy==0:return cardinal(a,b)
  viax=(b[0],a[1]);viay=(a[0],b[1])
  return (cardinal(a,viax) and cardinal(viax,b)) or (cardinal(a,viay) and cardinal(viay,b))
 directions=[(0,-1),(1,-1),(1,0),(1,1),(0,1),(-1,1),(-1,0),(-1,-1)]
 canonical=bytearray(b'LITO-WORLD-MAP\0'+struct.pack('>HIH',1,534,len(sectors)))
 details=[];all_counts=collections.Counter();seam_open=0;outer_closed=0;standing=0
 for s in sectors:
  ox,oy=origin(s);maskbytes=bytearray();counts=collections.Counter()
  for y in range(64):
   for x in range(64):
    a=(ox+x,oy+y);mask=0
    for heading,(dx,dy) in enumerate(directions):
     b=(a[0]+dx,a[1]+dy);opened=step(a,b)
     if opened:mask|=1<<heading
     if b not in cells:require(not opened,'external step unexpectedly open');outer_closed+=1
     elif cell_sector[b]!=cell_sector[a] and opened:seam_open+=1
    maskbytes.append(mask);counts[mask]+=1;standing+=bool(mask&0x55)
  all_counts.update(counts);header_at=len(canonical)
  canonical.extend(struct.pack('>HHiiHHI',s['x'],s['y'],ox,oy,64,64,s['source']['bytes']))
  canonical.extend(bytes.fromhex(s['source']['sha256']));mask_at=len(canonical);canonical.extend(maskbytes)
  details.append(dict(x=s['x'],y=s['y'],world_origin=[ox,oy],source=s['source'],attribute_source_byte_range=s['layer_byte_ranges'][2],portal_source_byte_range=s['layer_byte_ranges'][6] if len(s['layer_byte_ranges'])>=7 else None,portal_blocked_local_indices=sorted(portal_indices[(s['x'],s['y'])]),canonical_sector_header_offset=header_at,canonical_direction_range=[mask_at,len(canonical)],direction_sha256=hashlib.sha256(maskbytes).hexdigest().upper(),mask_counts=dict(sorted(counts.items()))))
 require(len(canonical)==23+4148*16,'canonical length')
 digest=hashlib.sha256(canonical).hexdigest().upper();require(digest==sources['canonical_sha256_declared'],'canonical mismatch')
 (OUT/'canonical.bin').write_bytes(canonical)
 result=dict(mode_qualification='Mode1 is manifest route identity; canonical geometry serialization encodes map534, not mode.',map_id=534,sectors=16,world_cells=len(cells),standing_cells=standing,portal_blocked_cells=sum(len(p) for p in portal_indices.values()),open_cross_sector_direction_edges=seam_open,closed_outside_direction_edges=outer_closed,canonical_bytes=len(canonical),canonical_sha256=digest,declared_sha256=sources['canonical_sha256_declared'],matches=True,direction_mask_counts=dict(sorted(all_counts.items())))
 put('inputs.json',dict(input_manifest=pin(PREV/'input-sources.json'),layer_parse=pin(PREV/'sector-parse.json'),source_sectors=[s['source'] for s in sectors],consumer_source_pins=consumer,va_rva='N/A: raw sectors and Rust geometry/serialization sources; file offsets retained.'))
 put('sector-directions.json',details);put('result.json',result)
 print(json.dumps(result,sort_keys=True));print('PASS: independently recomputed canonical map geometry hash matches exact pinned declaration; static source-policy identity only')
if __name__=='__main__':main()
