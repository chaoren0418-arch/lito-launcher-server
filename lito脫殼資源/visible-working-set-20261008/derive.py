"""Independent static projection of already-qualified raw resource references.
No pixels, Modern binary, native process or preview execution.
"""
import collections, csv, hashlib, json, struct, sys
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
OUT=Path(__file__).resolve().parent
OWNER=OUT.parent
PREV=OWNER/'cache-admission-20261008'
MODERN=OWNER.parent/'lito重建版'/'LiTo381-Modern'
def require(v,why):
 if not v: raise ValueError(why)
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def pin(p): return dict(path=str(p),bytes=p.stat().st_size,sha256=sha(p))
def put(name,d): (OUT/name).write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def scan(hx,y): return 2*y-(hx&~1),((hx+1)&~1)+2*y
def origin(sector): return 32704+(sector['x']-0x7fff)*64,32704+(sector['y']-0x7fff)*64

def project(refs,sectors,camera):
 cx,cy,w,h=camera
 ax,ay=origin(sectors[0]); ar,ac=scan(2*ax-32768,ay)
 lo=ar+(-768+cy-24)//6-2; hi=ar+(h-768+cy+24)//6+2
 needed=set(); visible=0; candidates=0; rows=set(); keys=collections.Counter()
 for si,layer,offset,x,y,depth,tid,slot in refs:
  ox,oy=origin(sectors[si]); row,col=scan(2*ox-32768+x,oy+y)
  if lo<=row<=hi:
   candidates+=1
   if layer!=1: rows.add(row)
   px=-cx+12*(col-ac); py=756-cy+6*(row-ar)
   if px+24>0 and py+24>0 and px<w and py<h:
    needed.add((tid,slot)); visible+=1; keys[(tid,slot)]+=1
 return dict(camera=camera,scan_row_band=[lo,hi],candidate_fragments=candidates,visible_fragments=visible,visited_index_rows=len(rows),unique_keys=len(needed),frame_decoded_bytes=len(needed)*2304,capacity_16MiB_fits=len(needed)*2304<=16<<20),needed,keys

def simulate(frames,keysets,capacity):
 max_slots=capacity//2304; resident={}; stamp=0; total_hits=total_misses=total_evictions=0; output=[]; peak=0
 for name in ['initial','initial','seam_before','seam_after','initial']:
  needed=keysets[name]; require(len(needed)<=max_slots,'frame exceeds simulated capacity')
  missing=sorted(needed-resident.keys()); remove=max(0,len(resident)+len(missing)-max_slots)
  victims=sorted((s,k) for k,s in resident.items() if k not in needed)[:remove]
  require(len(victims)==remove,'protected frame eviction failure')
  for _,key in victims: del resident[key]
  hits=len(needed)-len(missing)
  for key in missing:
   require((len(resident)+1)*2304<=capacity,'transient cap exceeded')
   stamp+=1; resident[key]=stamp; peak=max(peak,len(resident)*2304)
  for key in sorted(needed): stamp+=1; resident[key]=stamp
  total_hits+=hits; total_misses+=len(missing); total_evictions+=remove
  output.append(dict(frame=name,needed_keys=len(needed),hits_delta=hits,misses_delta=len(missing),evictions_delta=remove,resident_slots=len(resident),resident_decoded_bytes=len(resident)*2304,cumulative_hits=total_hits,cumulative_misses=total_misses,cumulative_evictions=total_evictions))
 return dict(capacity_bytes=capacity,max_slots=max_slots,peak_resident_decoded_bytes=peak,frames=output)

def main():
 contract=read(OWNER/'cache-admission-20261008-contract.json')
 refs_pin=next(x for x in contract['evidence'] if Path(x['path']).name=='references.csv')
 require(sha(Path(refs_pin['path']))==refs_pin['sha256'],'qualified reference drift')
 source_manifest=read(PREV/'input-sources.json'); relevant=[]
 for p in source_manifest['decode_and_consumer_sources']:
  if Path(p['path']).name in ('native_city_resources.rs','world_depth.rs','native_city_tile_cache.rs'):
   require(sha(Path(p['path']))==p['sha256'],'projection/cache source drift'); relevant.append(p)
 sectors=read(PREV/'sector-parse.json')
 with (PREV/'references.csv').open(encoding='utf-8',newline='') as f:
  rows=csv.reader(f); next(rows); refs=[tuple(map(int,r)) for r in rows]
 require(len(refs)==347528,'complete reference input')
 cameras=dict(initial=[5744,468,800,600],seam_before=[3752,1092,800,600],seam_after=[3944,996,800,600],minimum=[0,-2304,800,600],maximum=[11488,3240,800,600])
 frames={}; keysets={}
 for name,camera in cameras.items():
  result,needed,counts=project(refs,sectors,camera); frames[name]=result; keysets[name]=needed
  with (OUT/f'{name}-keys.csv').open('w',encoding='utf-8',newline='') as f:
   rows=csv.writer(f);rows.writerow(['tile_id','slot','visible_reference_count'])
   for tid,slot in sorted(needed): rows.writerow([tid,slot,counts[(tid,slot)]])
  result['keys_file']=pin(OUT/f'{name}-keys.csv')
  result['keyset_sha256_le_u16_u8']=hashlib.sha256(b''.join(struct.pack('<HB',tid,slot) for tid,slot in sorted(needed))).hexdigest().upper()
 max_keys=max(len(v) for v in keysets.values()); limited_cap=(max_keys+8)*2304
 sims=[simulate(frames,keysets,16<<20),simulate(frames,keysets,limited_cap)]
 initial_bytes=frames['initial']['frame_decoded_bytes']
 rejection=dict(capacity_bytes=initial_bytes-2304,initial_required_bytes=initial_bytes,expected='Reject visible pixel budget before eviction or decode; resident/cache counters unchanged.',valid_capacity_bounds=2304<=initial_bytes-2304<=16<<20)
 ref=MODERN/'target/r483-city-visible-rows/reference/reference-534-initial.png'
 require(sha(ref)=='D7B9C260F35297EE7E9505D2170667C6CE4EE8F8FA77E9C96EFC3AB81E6B7CB5','historical reference drift')
 put('inputs.json',dict(qualified_reference=refs_pin,preceding_contract=pin(OWNER/'cache-admission-20261008-contract.json'),sector_parse=pin(PREV/'sector-parse.json'),projection_cache_source_pins=relevant,historical_static_reference=pin(ref),camera_origin='Historical static regression camera coordinates; no runtime scene/owner proof.',va_rva='N/A: CSV/raw sectors and Rust projection/cache text; no PE mapping claim.'))
 put('frame-demands.json',frames);put('cache-sequence-model.json',sims);put('rejection-witness.json',rejection)
 print(json.dumps(dict(frames=frames,limited_capacity_bytes=limited_cap,default_sequence=sims[0]['frames'],limited_sequence=sims[1]['frames'],rejection=rejection),sort_keys=True))
 print('PASS static projected keysets and bounded cache sequence model; no rasterization, pixel equality, native/runtime or consumer execution claim')
if __name__=='__main__': main()
